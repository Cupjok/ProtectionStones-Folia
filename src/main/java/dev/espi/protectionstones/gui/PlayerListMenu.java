/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package dev.espi.protectionstones.gui;

import dev.espi.protectionstones.PSL;
import dev.espi.protectionstones.PSPlayer;
import dev.espi.protectionstones.PSRegion;
import dev.espi.protectionstones.commands.ArgAddRemove;
import dev.espi.protectionstones.compat.FoliaScheduler;
import dev.espi.protectionstones.utils.UUIDCache;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Add and remove the members (or owners) of a region — the reason most players open this menu at all.
 *
 * <p>Mirrors the checks {@code /ps add}, {@code /ps remove}, {@code /ps addowner} and
 * {@code /ps removeowner} make, including the region limit check when granting ownership.
 */
final class PlayerListMenu {

    private PlayerListMenu() {}

    static void open(Player p, World world, String id, boolean owners) {
        RegionMenu.withRegion(p, world, id, r -> {
            if (!hasPermission(p, owners)) {
                PSL.msg(p, denial(owners));
                return;
            }

            List<UUID> people = owners ? r.getOwners() : r.getMembers();
            boolean manage = Regions.canManage(p, r);

            List<DialogBody> body = Dialogs.bodies(
                    owners ? GuiText.PLAYERS_BODY_OWNERS.msg() : GuiText.PLAYERS_BODY_MEMBERS.msg(),
                    people.isEmpty() ? GuiText.PLAYERS_EMPTY.msg() : null);

            List<ActionButton> buttons = new ArrayList<>();

            if (manage) {
                buttons.add(Dialogs.button(GuiText.PLAYERS_BTN_ADD.msg(), null, pl -> openAddPrompt(pl, world, id, owners)));
            }

            for (UUID uuid : people) {
                String name = UUIDCache.getNameFromUUID(uuid);
                if (name == null) name = uuid.toString().substring(0, 8);
                final String shownName = name;
                final UUID target = uuid;

                if (manage) {
                    buttons.add(Dialogs.button(
                            GuiText.PLAYERS_ENTRY.msg("%player%", shownName),
                            GuiText.PLAYERS_ENTRY_TIP.msg("%player%", shownName),
                            pl -> confirmRemove(pl, world, id, owners, target, shownName)));
                } else {
                    // no permission to change anything: the names are just information
                    buttons.add(Dialogs.button(GuiText.PLAYERS_ENTRY.msg("%player%", shownName), null, pl -> {}));
                }
            }

            Dialogs.show(p, Dialogs.menu(
                    owners ? GuiText.PLAYERS_TITLE_OWNERS.msg() : GuiText.PLAYERS_TITLE_MEMBERS.msg(),
                    body, buttons,
                    Dialogs.backButton(pl -> RegionMenu.reopen(pl, world, id))));
        });
    }

    private static void openAddPrompt(Player p, World world, String id, boolean owners) {
        Dialogs.show(p, Dialogs.textPrompt(
                GuiText.PLAYERS_ADD_TITLE.msg(),
                Dialogs.bodies(GuiText.PLAYERS_ADD_BODY.msg()),
                GuiText.PLAYERS_ADD_FIELD.msg(),
                "",
                16,
                GuiText.PLAYERS_ADD_SUBMIT.msg(),
                (pl, name) -> add(pl, world, id, owners, name),
                pl -> open(pl, world, id, owners)));
    }

    private static void add(Player p, World world, String id, boolean owners, String name) {
        RegionMenu.withRegion(p, world, id, r -> {
            if (!hasPermission(p, owners)) {
                PSL.msg(p, denial(owners));
                return;
            }
            if (!Regions.requireManage(p, r)) return;

            if (name.isEmpty()) {
                PSL.msg(p, PSL.COMMAND_REQUIRES_PLAYER_NAME.msg());
                open(p, world, id, owners);
                return;
            }
            if (!UUIDCache.containsName(name)) {
                PSL.msg(p, PSL.PLAYER_NOT_FOUND.msg());
                open(p, world, id, owners);
                return;
            }

            UUID uuid = UUIDCache.getUUIDFromName(name);
            String cachedName = UUIDCache.getNameFromUUID(uuid);

            FoliaScheduler.callGlobal(() -> {
                PSRegion region = Regions.resolve(world, id);
                if (region == null) return GuiText.ERR_REGION_GONE.msg();

                if (owners) {
                    // granting ownership counts against the other player's region limit
                    String err = new ArgAddRemove().determinePlayerSurpassedLimit(
                            Collections.singletonList(region), PSPlayer.fromUUID(uuid));
                    if (err != null) return err;
                    region.addOwner(uuid);
                } else {
                    region.addMember(uuid);
                }
                return PSL.ADDED_TO_REGION.msg().replace("%player%", cachedName);
            }).thenAccept(message -> FoliaScheduler.runEntity(p, () -> {
                PSL.msg(p, (String) message);
                FoliaScheduler.runAsync(() -> UUIDCache.storeWGProfile(uuid, cachedName));
                open(p, world, id, owners);
            }));
        });
    }

    private static void confirmRemove(Player p, World world, String id, boolean owners, UUID target, String name) {
        RegionMenu.withRegion(p, world, id, r -> Dialogs.show(p, Dialogs.confirm(
                GuiText.PLAYERS_REMOVE_TITLE.msg(),
                Dialogs.bodies(GuiText.PLAYERS_REMOVE_BODY.msg("%player%", name, "%region%", Regions.label(r))),
                GuiText.PLAYERS_REMOVE_YES.msg(),
                pl -> remove(pl, world, id, owners, target, name),
                GuiText.BUTTON_CANCEL.msg(),
                pl -> open(pl, world, id, owners))));
    }

    private static void remove(Player p, World world, String id, boolean owners, UUID target, String name) {
        RegionMenu.withRegion(p, world, id, r -> {
            if (!hasPermission(p, owners)) {
                PSL.msg(p, denial(owners));
                return;
            }
            if (!Regions.requireManage(p, r)) return;

            if (owners && target.equals(p.getUniqueId()) && r.getOwners().size() == 1) {
                PSL.msg(p, PSL.CANNOT_REMOVE_YOURSELF_LAST_OWNER.msg());
                open(p, world, id, owners);
                return;
            }

            if (owners) {
                r.removeOwner(target);
            } else {
                r.removeMember(target);
            }
            PSL.msg(p, PSL.REMOVED_FROM_REGION.msg().replace("%player%", name));
            open(p, world, id, owners);
        });
    }

    private static boolean hasPermission(Player p, boolean owners) {
        return p.hasPermission(owners ? "protectionstones.owners" : "protectionstones.members");
    }

    private static String denial(boolean owners) {
        return owners ? PSL.NO_PERMISSION_OWNERS.msg() : PSL.NO_PERMISSION_MEMBERS.msg();
    }
}
