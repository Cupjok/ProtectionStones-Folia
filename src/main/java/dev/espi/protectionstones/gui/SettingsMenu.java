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

import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import dev.espi.protectionstones.PSL;
import dev.espi.protectionstones.PSRegion;
import dev.espi.protectionstones.ProtectionStones;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * The less-used region options: priority, parent region, hiding the protection block, merging, and
 * showing the region borders.
 */
final class SettingsMenu {

    private SettingsMenu() {}

    static void open(Player p, World world, String id) {
        RegionMenu.withRegion(p, world, id, r -> {
            List<DialogBody> body = Dialogs.bodies(GuiText.SETTINGS_BODY.msg());
            List<ActionButton> buttons = new ArrayList<>();
            boolean manage = Regions.canManage(p, r);

            if (p.hasPermission("protectionstones.view")) {
                buttons.add(Dialogs.button(GuiText.REGION_BTN_VIEW.msg(), GuiText.REGION_BTN_VIEW_TIP.msg(),
                        pl -> RegionMenu.withRegion(pl, world, id, region -> {
                            if (Regions.requireInside(pl, region)) Regions.runCommand(pl, "view");
                        })));
            }

            if (manage && p.hasPermission("protectionstones.priority")) {
                buttons.add(Dialogs.button(
                        GuiText.SETTINGS_BTN_PRIORITY.msg("%priority%", r.getWGRegion().getPriority()),
                        GuiText.SETTINGS_BTN_PRIORITY_TIP.msg(),
                        pl -> openPriorityPrompt(pl, world, id)));
            }

            if (manage && p.hasPermission("protectionstones.setparent")) {
                buttons.add(Dialogs.button(GuiText.SETTINGS_BTN_PARENT.msg(), GuiText.SETTINGS_BTN_PARENT_TIP.msg(),
                        pl -> openParentPicker(pl, world, id)));
            }

            if (manage) {
                if (r.isHidden() && p.hasPermission("protectionstones.unhide")) {
                    buttons.add(Dialogs.button(GuiText.SETTINGS_BTN_UNHIDE.msg(), null,
                            pl -> setHidden(pl, world, id, false)));
                } else if (!r.isHidden() && p.hasPermission("protectionstones.hide")) {
                    buttons.add(Dialogs.button(GuiText.SETTINGS_BTN_HIDE.msg(), null,
                            pl -> setHidden(pl, world, id, true)));
                }
            }

            if (manage && p.hasPermission("protectionstones.merge")
                    && ProtectionStones.getInstance().getConfigOptions().allowMergingRegions) {
                buttons.add(Dialogs.button(GuiText.SETTINGS_BTN_MERGE.msg(), null,
                        pl -> openMergePicker(pl, world, id)));
            }

            Dialogs.show(p, Dialogs.menu(GuiText.SETTINGS_TITLE.msg(), body, buttons,
                    Dialogs.backButton(pl -> RegionMenu.reopen(pl, world, id))));
        });
    }

    /* ~~~~~~~~~~ priority ~~~~~~~~~~ */

    private static void openPriorityPrompt(Player p, World world, String id) {
        RegionMenu.withRegion(p, world, id, r -> Dialogs.show(p, Dialogs.textPrompt(
                GuiText.SETTINGS_PRIORITY_TITLE.msg(),
                Dialogs.bodies(GuiText.SETTINGS_PRIORITY_BODY.msg()),
                GuiText.SETTINGS_PRIORITY_FIELD.msg(),
                String.valueOf(r.getWGRegion().getPriority()),
                10,
                GuiText.SETTINGS_PRIORITY_SUBMIT.msg(),
                (pl, value) -> RegionMenu.withRegion(pl, world, id, region -> {
                    if (!Regions.requirePermission(pl, "protectionstones.priority", PSL.NO_PERMISSION_PRIORITY.msg())) return;
                    if (!Regions.requireManage(pl, region)) return;
                    try {
                        region.getWGRegion().setPriority(Integer.parseInt(value));
                        PSL.msg(pl, PSL.PRIORITY_SET.msg());
                    } catch (NumberFormatException e) {
                        PSL.msg(pl, PSL.PRIORITY_ERROR.msg());
                    }
                    open(pl, world, id);
                }),
                pl -> open(pl, world, id))));
    }

    /* ~~~~~~~~~~ parent ~~~~~~~~~~ */

    private static void openParentPicker(Player p, World world, String id) {
        Regions.collectPlayerRegions(p, all -> {
            List<PSRegion> candidates = new ArrayList<>();
            for (PSRegion candidate : all) {
                if (candidate.getId().equals(id) && candidate.getWorld().equals(world)) continue;
                if (!candidate.getWorld().equals(world)) continue; // WorldGuard parents are per-world
                candidates.add(candidate);
            }

            List<ActionButton> extra = new ArrayList<>();
            PSRegion current = Regions.resolve(world, id);
            if (current != null && current.getParent() != null) {
                extra.add(Dialogs.button(GuiText.SETTINGS_PARENT_NONE.msg(), null,
                        pl -> setParent(pl, world, id, null)));
            }

            RegionListMenu.render(p, 0,
                    GuiText.SETTINGS_PARENT_TITLE.msg(),
                    GuiText.SETTINGS_PARENT_BODY.msg(),
                    candidates,
                    parent -> setParent(p, world, id, parent),
                    pl -> open(pl, world, id),
                    extra);
        });
    }

    private static void setParent(Player p, World world, String id, PSRegion parent) {
        RegionMenu.withRegion(p, world, id, r -> {
            if (!Regions.requirePermission(p, "protectionstones.setparent", PSL.NO_PERMISSION_SETPARENT.msg())) return;
            if (!Regions.requireManage(p, r)) return;
            if (parent != null && !p.hasPermission("protectionstones.setparent.others") && !parent.isOwner(p.getUniqueId())) {
                PSL.msg(p, PSL.NO_PERMISSION_SETPARENT_OTHERS.msg());
                return;
            }

            try {
                r.setParent(parent);
                if (parent == null) {
                    PSL.msg(p, PSL.SETPARENT_SUCCESS_REMOVE.msg().replace("%id%", Regions.label(r)));
                } else {
                    PSL.msg(p, PSL.SETPARENT_SUCCESS.msg()
                            .replace("%id%", Regions.label(r))
                            .replace("%parent%", Regions.label(parent)));
                }
            } catch (ProtectedRegion.CircularInheritanceException e) {
                PSL.msg(p, PSL.SETPARENT_CIRCULAR_INHERITANCE.msg());
            }
            open(p, world, id);
        });
    }

    /* ~~~~~~~~~~ hide ~~~~~~~~~~ */

    private static void setHidden(Player p, World world, String id, boolean hide) {
        RegionMenu.withRegion(p, world, id, r -> {
            String permission = hide ? "protectionstones.hide" : "protectionstones.unhide";
            String denial = hide ? PSL.NO_PERMISSION_HIDE.msg() : PSL.NO_PERMISSION_UNHIDE.msg();
            if (!Regions.requirePermission(p, permission, denial)) return;
            if (!Regions.requireManage(p, r)) return;

            if (hide) {
                if (r.isHidden()) {
                    PSL.msg(p, PSL.ALREADY_HIDDEN.msg());
                } else {
                    r.hide();
                }
            } else {
                if (!r.isHidden()) {
                    PSL.msg(p, PSL.ALREADY_NOT_HIDDEN.msg());
                } else {
                    r.unhide();
                }
            }
            open(p, world, id);
        });
    }

    /* ~~~~~~~~~~ merge ~~~~~~~~~~ */

    private static void openMergePicker(Player p, World world, String id) {
        RegionMenu.withRegion(p, world, id, r -> {
            if (!Regions.requireInside(p, r)) return; // /ps merge acts on the region you stand in

            List<PSRegion> candidates = r.getMergeableRegions(p);
            if (candidates.isEmpty()) {
                Dialogs.show(p, Dialogs.notice(GuiText.SETTINGS_MERGE_TITLE.msg(),
                        Dialogs.bodies(GuiText.SETTINGS_MERGE_EMPTY.msg()),
                        Dialogs.backButton(pl -> open(pl, world, id))));
                return;
            }

            RegionListMenu.render(p, 0,
                    GuiText.SETTINGS_MERGE_TITLE.msg(),
                    GuiText.SETTINGS_MERGE_BODY.msg("%region%", Regions.label(r)),
                    candidates,
                    into -> Dialogs.show(p, Dialogs.confirm(
                            GuiText.SETTINGS_MERGE_TITLE.msg(),
                            Dialogs.bodies(GuiText.SETTINGS_MERGE_BODY.msg("%region%", Regions.label(r))),
                            GuiText.SETTINGS_BTN_MERGE.msg(),
                            pl -> Regions.runCommand(pl, "merge " + into.getId()),
                            GuiText.BUTTON_CANCEL.msg(),
                            pl -> open(pl, world, id))),
                    pl -> open(pl, world, id),
                    null);
        });
    }
}
