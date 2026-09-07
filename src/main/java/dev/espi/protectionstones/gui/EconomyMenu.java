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

import dev.espi.protectionstones.PSRegion;
import dev.espi.protectionstones.ProtectionStones;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Selling, buying and renting a region.
 *
 * <p>Every action here is handed to the existing {@code /ps buy}, {@code /ps sell} and
 * {@code /ps rent} commands, which all act on the region the player is standing in — so the menu
 * checks that first rather than acting on the wrong region.
 */
final class EconomyMenu {

    private EconomyMenu() {}

    static void open(Player p, World world, String id) {
        RegionMenu.withRegion(p, world, id, r -> {
            if (!ProtectionStones.getInstance().isVaultSupportEnabled()) {
                Dialogs.show(p, Dialogs.notice(GuiText.ECON_TITLE.msg(),
                        Dialogs.bodies(GuiText.ERR_NO_ECONOMY.msg()),
                        Dialogs.backButton(pl -> RegionMenu.reopen(pl, world, id))));
                return;
            }

            List<DialogBody> body = Dialogs.bodies(GuiText.ECON_BODY.msg());
            List<ActionButton> buttons = new ArrayList<>();

            boolean owner = r.isOwner(p.getUniqueId());
            boolean renting = r.getRentStage() != PSRegion.RentStage.NOT_RENTING;

            if (p.hasPermission("protectionstones.buysell")) {
                if (r.forSale()) {
                    if (owner) {
                        buttons.add(Dialogs.button(GuiText.ECON_BTN_STOP_SELL.msg(), null,
                                pl -> run(pl, world, id, "sell stop")));
                    } else {
                        buttons.add(Dialogs.button(GuiText.ECON_BTN_BUY.msg("%price%", RegionMenu.money(r.getPrice())), null,
                                pl -> run(pl, world, id, "buy")));
                    }
                } else if (owner && !renting) {
                    buttons.add(Dialogs.button(GuiText.ECON_BTN_SELL.msg(), null, pl -> openSellPrompt(pl, world, id)));
                }
            }

            if (p.hasPermission("protectionstones.rent")) {
                switch (r.getRentStage()) {
                    case NOT_RENTING:
                        if (owner && !r.forSale()) {
                            buttons.add(Dialogs.button(GuiText.ECON_BTN_RENT_OUT.msg(), null,
                                    pl -> openRentPricePrompt(pl, world, id)));
                        }
                        break;
                    case LOOKING_FOR_TENANT:
                        if (owner) {
                            buttons.add(Dialogs.button(GuiText.ECON_BTN_STOP_RENT_OUT.msg(), null,
                                    pl -> run(pl, world, id, "rent stoplease")));
                        } else {
                            buttons.add(Dialogs.button(GuiText.ECON_BTN_RENT.msg(
                                    "%price%", RegionMenu.money(r.getPrice()),
                                    "%period%", String.valueOf(r.getRentPeriod())), null,
                                    pl -> run(pl, world, id, "rent rent")));
                        }
                        break;
                    case RENTING:
                        if (p.getUniqueId().equals(r.getTenant())) {
                            buttons.add(Dialogs.button(GuiText.ECON_BTN_STOP_RENTING.msg(), null,
                                    pl -> run(pl, world, id, "rent stoprenting")));
                        } else if (owner) {
                            buttons.add(Dialogs.button(GuiText.ECON_BTN_STOP_RENT_OUT.msg(), null,
                                    pl -> run(pl, world, id, "rent stoplease")));
                        }
                        break;
                }
            }

            if (ProtectionStones.getInstance().getConfigOptions().taxEnabled && p.hasPermission("protectionstones.tax")) {
                buttons.add(Dialogs.button(GuiText.ECON_BTN_TAX.msg(), null, pl -> TaxMenu.open(pl, 0)));

                if (owner) {
                    buttons.add(Dialogs.button(GuiText.ECON_BTN_AUTOPAY.msg(), GuiText.ECON_BTN_AUTOPAY_TIP.msg(), pl -> {
                        Regions.runCommand(pl, "tax autopay " + id);
                        open(pl, world, id);
                    }));
                }
            }

            Dialogs.show(p, Dialogs.menu(GuiText.ECON_TITLE.msg(), body, buttons,
                    Dialogs.backButton(pl -> RegionMenu.reopen(pl, world, id))));
        });
    }

    private static void openSellPrompt(Player p, World world, String id) {
        Dialogs.show(p, Dialogs.textPrompt(
                GuiText.ECON_SELL_TITLE.msg(),
                Dialogs.bodies(GuiText.ECON_SELL_BODY.msg()),
                GuiText.ECON_SELL_FIELD.msg(),
                "100",
                16,
                GuiText.ECON_SELL_SUBMIT.msg(),
                (pl, price) -> run(pl, world, id, "sell " + sanitize(price)),
                pl -> open(pl, world, id)));
    }

    private static void openRentPricePrompt(Player p, World world, String id) {
        Dialogs.show(p, Dialogs.textPrompt(
                GuiText.ECON_RENT_TITLE.msg(),
                Dialogs.bodies(GuiText.ECON_RENT_BODY.msg()),
                GuiText.ECON_RENT_PRICE_FIELD.msg(),
                "100",
                16,
                GuiText.ECON_RENT_SUBMIT.msg(),
                (pl, price) -> openRentPeriodPrompt(pl, world, id, sanitize(price)),
                pl -> open(pl, world, id)));
    }

    private static void openRentPeriodPrompt(Player p, World world, String id, String price) {
        Dialogs.show(p, Dialogs.textPrompt(
                GuiText.ECON_RENT_TITLE.msg(),
                Dialogs.bodies(GuiText.ECON_RENT_BODY.msg()),
                GuiText.ECON_RENT_PERIOD_FIELD.msg(),
                "1d",
                16,
                GuiText.ECON_RENT_CONFIRM.msg(),
                (pl, period) -> run(pl, world, id, "rent lease " + price + " " + sanitize(period)),
                pl -> open(pl, world, id)));
    }

    /** Runs a {@code /ps} subcommand against this region, after checking the player is standing in it. */
    private static void run(Player p, World world, String id, String subcommand) {
        RegionMenu.withRegion(p, world, id, r -> {
            if (!Regions.requireInside(p, r)) return;
            Regions.runCommand(p, subcommand);
            open(p, world, id);
        });
    }

    /**
     * Free text goes into a command line, so anything that could split it into extra arguments or
     * escape the subcommand is dropped. The commands themselves validate what's left.
     */
    private static String sanitize(String input) {
        // an empty result still reaches the command, which answers with its own help message
        return input.replaceAll("[^A-Za-z0-9.]", "");
    }
}
