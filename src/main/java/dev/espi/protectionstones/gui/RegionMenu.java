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

import com.sk89q.worldedit.math.BlockVector3;
import dev.espi.protectionstones.PSL;
import dev.espi.protectionstones.PSRegion;
import dev.espi.protectionstones.ProtectionStones;
import dev.espi.protectionstones.commands.ArgTp;
import dev.espi.protectionstones.utils.UUIDCache;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * The management screen for a single region — the screen right-clicking a protection block lands on.
 *
 * <p>Players who are neither an owner nor a member get {@link #openInfo} instead, which shows who owns
 * the land (and lets them buy or rent it when it is on offer) but changes nothing.
 */
final class RegionMenu {

    private RegionMenu() {}

    static void open(Player p, PSRegion r) {
        if (!Regions.canUse(p, r)) {
            openInfo(p, r);
            return;
        }

        final World world = r.getWorld();
        final String id = r.getId();
        Consumer<Player> reopen = pl -> reopen(pl, world, id);

        List<ActionButton> buttons = new ArrayList<>();
        boolean manage = Regions.canManage(p, r);

        if (p.hasPermission("protectionstones.home") && r.getHome() != null
                && r.getTypeOptions() != null && !r.getTypeOptions().preventPsHome) {
            buttons.add(Dialogs.button(GuiText.REGION_BTN_TELEPORT.msg(), null,
                    pl -> withRegion(pl, world, id, region -> ArgTp.teleportPlayer(pl, region))));
        }

        if (manage && p.hasPermission("protectionstones.sethome")) {
            buttons.add(Dialogs.button(GuiText.REGION_BTN_SETHOME.msg(), GuiText.REGION_BTN_SETHOME_TIP.msg(),
                    pl -> withRegion(pl, world, id, region -> {
                        if (!Regions.requireManage(pl, region)) return;
                        Location l = pl.getLocation();
                        region.setHome(l.getBlockX(), l.getBlockY(), l.getBlockZ(), l.getYaw(), l.getPitch());
                        PSL.msg(pl, PSL.SETHOME_SET.msg().replace("%psid%", describe(region)));
                        reopen.accept(pl);
                    })));
        }

        if (manage && p.hasPermission("protectionstones.name")) {
            buttons.add(Dialogs.button(GuiText.REGION_BTN_RENAME.msg(), null,
                    pl -> openRename(pl, world, id)));
        }

        if (p.hasPermission("protectionstones.members")) {
            buttons.add(Dialogs.button(GuiText.REGION_BTN_MEMBERS.msg(), null,
                    pl -> PlayerListMenu.open(pl, world, id, false)));
        }

        if (p.hasPermission("protectionstones.owners")) {
            buttons.add(Dialogs.button(GuiText.REGION_BTN_OWNERS.msg(), null,
                    pl -> PlayerListMenu.open(pl, world, id, true)));
        }

        if (manage && p.hasPermission("protectionstones.flags")) {
            buttons.add(Dialogs.button(GuiText.REGION_BTN_FLAGS.msg(), GuiText.REGION_BTN_FLAGS_TIP.msg(),
                    pl -> FlagMenu.open(pl, world, id, 0)));
        }

        if (p.hasPermission("protectionstones.buysell") || p.hasPermission("protectionstones.rent")) {
            buttons.add(Dialogs.button(GuiText.REGION_BTN_ECONOMY.msg(), null,
                    pl -> EconomyMenu.open(pl, world, id)));
        }

        buttons.add(Dialogs.button(GuiText.REGION_BTN_SETTINGS.msg(), null,
                pl -> SettingsMenu.open(pl, world, id)));

        if (manage && p.hasPermission("protectionstones.unclaim")) {
            buttons.add(Dialogs.button(GuiText.REGION_BTN_UNCLAIM.msg(), null,
                    pl -> openUnclaim(pl, world, id)));
        }

        Dialogs.show(p, Dialogs.menu(
                GuiText.REGION_TITLE.msg("%name%", Regions.label(r)),
                describeBody(p, r, true),
                buttons,
                Dialogs.backButton(MainMenu::open)));
    }

    /** Read-only view for players who don't belong to the region. */
    static void openInfo(Player p, PSRegion r) {
        final World world = r.getWorld();
        final String id = r.getId();

        List<DialogBody> body = new ArrayList<>();
        body.add(Dialogs.body(GuiText.REGION_INFO_BODY.msg()));
        body.addAll(describeBody(p, r, false));

        List<ActionButton> buttons = new ArrayList<>();

        // the one thing an outsider can do here: take up an offer the owner has made
        if (r.forSale() && p.hasPermission("protectionstones.buysell")) {
            buttons.add(Dialogs.button(GuiText.ECON_BTN_BUY.msg("%price%", money(r.getPrice())), null,
                    pl -> withRegion(pl, world, id, region -> {
                        if (Regions.requireInside(pl, region)) Regions.runCommand(pl, "buy");
                    })));
        }
        if (r.getRentStage() == PSRegion.RentStage.LOOKING_FOR_TENANT && p.hasPermission("protectionstones.rent")) {
            buttons.add(Dialogs.button(
                    GuiText.ECON_BTN_RENT.msg("%price%", money(r.getPrice()), "%period%", String.valueOf(r.getRentPeriod())), null,
                    pl -> withRegion(pl, world, id, region -> {
                        if (Regions.requireInside(pl, region)) Regions.runCommand(pl, "rent rent");
                    })));
        }

        if (buttons.isEmpty()) {
            Dialogs.show(p, Dialogs.notice(GuiText.REGION_INFO_TITLE.msg(), body, Dialogs.closeButton()));
        } else {
            Dialogs.show(p, Dialogs.menu(GuiText.REGION_INFO_TITLE.msg(), body, buttons, Dialogs.closeButton()));
        }
    }

    /* ~~~~~~~~~~ body ~~~~~~~~~~ */

    private static List<DialogBody> describeBody(Player p, PSRegion r, boolean detailed) {
        List<String> lines = new ArrayList<>();

        lines.add(GuiText.REGION_BODY_ID.msg("%id%", r.getId()));
        if (r.getTypeOptions() != null) {
            lines.add(GuiText.REGION_BODY_TYPE.msg("%type%", r.getTypeOptions().alias));
        }
        lines.add(GuiText.REGION_BODY_WORLD.msg("%world%", r.getWorld().getName()));
        lines.add(GuiText.REGION_BODY_OWNERS.msg("%players%", Regions.names(r.getOwners())));

        if (detailed) {
            lines.add(GuiText.REGION_BODY_MEMBERS.msg("%players%", Regions.names(r.getMembers())));
            lines.add(GuiText.REGION_BODY_PRIORITY.msg("%priority%", r.getWGRegion().getPriority()));

            BlockVector3 min = r.getWGRegion().getMinimumPoint(), max = r.getWGRegion().getMaximumPoint();
            lines.add(GuiText.REGION_BODY_BOUNDS.msg(
                    "%minx%", min.getBlockX(), "%minz%", min.getBlockZ(),
                    "%maxx%", max.getBlockX(), "%maxz%", max.getBlockZ()));

            if (r.getParent() != null) {
                lines.add(GuiText.REGION_BODY_PARENT.msg("%parent%", Regions.label(r.getParent())));
            }
            if (r.isHidden()) {
                lines.add(GuiText.REGION_BODY_HIDDEN.msg());
            }
        }

        if (r.forSale()) {
            lines.add(GuiText.REGION_BODY_FOR_SALE.msg(
                    "%price%", money(r.getPrice()),
                    "%seller%", nameOf(r.getLandlord())));
        }
        if (r.getRentStage() == PSRegion.RentStage.LOOKING_FOR_TENANT) {
            lines.add(GuiText.REGION_BODY_FOR_RENT.msg(
                    "%price%", money(r.getPrice()),
                    "%period%", String.valueOf(r.getRentPeriod())));
        } else if (r.getRentStage() == PSRegion.RentStage.RENTING) {
            lines.add(GuiText.REGION_BODY_RENTED.msg(
                    "%tenant%", nameOf(r.getTenant()),
                    "%price%", money(r.getPrice()),
                    "%period%", String.valueOf(r.getRentPeriod())));
        }

        if (detailed && ProtectionStones.getInstance().getConfigOptions().taxEnabled) {
            double owed = 0;
            List<PSRegion.TaxPayment> due = r.getTaxPaymentsDue();
            if (due != null) for (PSRegion.TaxPayment t : due) owed += t.getAmount();
            if (owed > 0) lines.add(GuiText.REGION_BODY_TAX.msg("%amount%", money(owed)));
        }

        return Dialogs.bodies(lines.toArray(new String[0]));
    }

    /* ~~~~~~~~~~ sub-screens ~~~~~~~~~~ */

    private static void openRename(Player p, World world, String id) {
        withRegion(p, world, id, r -> {
            if (!Regions.requireManage(p, r)) return;

            Dialogs.show(p, Dialogs.textPrompt(
                    GuiText.RENAME_TITLE.msg(),
                    Dialogs.bodies(GuiText.RENAME_BODY.msg(), GuiText.RENAME_CLEAR.msg()),
                    GuiText.RENAME_FIELD.msg(),
                    r.getName() == null ? "" : r.getName(),
                    64,
                    GuiText.RENAME_SUBMIT.msg(),
                    (pl, value) -> withRegion(pl, world, id, region -> {
                        if (!Regions.requireManage(pl, region)) return;
                        if (value.isEmpty()) {
                            region.setName(null);
                            PSL.msg(pl, PSL.NAME_REMOVED.msg().replace("%id%", region.getId()));
                        } else if (!ProtectionStones.getInstance().getConfigOptions().allowDuplicateRegionNames
                                && ProtectionStones.isPSNameAlreadyUsed(value)) {
                            PSL.msg(pl, PSL.NAME_TAKEN.msg().replace("%name%", value));
                        } else {
                            region.setName(value);
                            PSL.msg(pl, PSL.NAME_SET_NAME.msg().replace("%id%", region.getId()).replace("%name%", value));
                        }
                        reopen(pl, world, id);
                    }),
                    pl -> reopen(pl, world, id)));
        });
    }

    private static void openUnclaim(Player p, World world, String id) {
        withRegion(p, world, id, r -> Dialogs.show(p, Dialogs.confirm(
                GuiText.UNCLAIM_TITLE.msg(),
                Dialogs.bodies(GuiText.UNCLAIM_BODY.msg("%region%", Regions.label(r))),
                GuiText.UNCLAIM_YES.msg(),
                pl -> withRegion(pl, world, id, region -> {
                    // hand off to /ps unclaim so rent checks, block refunds and events all still apply
                    if (Regions.isInside(pl, region)) {
                        Regions.runCommand(pl, "unclaim");
                    } else {
                        Regions.runCommand(pl, "unclaim " + region.getId());
                    }
                }),
                GuiText.BUTTON_CANCEL.msg(),
                pl -> reopen(pl, world, id))));
    }

    /* ~~~~~~~~~~ helpers ~~~~~~~~~~ */

    static void reopen(Player p, World world, String id) {
        withRegion(p, world, id, r -> open(p, r));
    }

    /**
     * Re-resolves the region before acting on it — a dialog can sit open while the region is unclaimed,
     * merged or renamed by someone else.
     */
    static void withRegion(Player p, World world, String id, Consumer<PSRegion> action) {
        PSRegion r = Regions.resolve(world, id);
        if (r == null) {
            PSL.msg(p, GuiText.ERR_REGION_GONE.msg());
            return;
        }
        action.accept(r);
    }

    static String money(Double d) {
        return d == null ? "0" : String.format("%.2f", d);
    }

    private static String nameOf(java.util.UUID uuid) {
        if (uuid == null) return GuiText.REGION_BODY_NONE.msg();
        String name = UUIDCache.getNameFromUUID(uuid);
        return name == null ? GuiText.REGION_BODY_NONE.msg() : name;
    }

    private static String describe(PSRegion r) {
        return r.getName() != null ? String.format("%s (%s)", r.getName(), r.getId()) : r.getId();
    }
}
