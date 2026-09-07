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
import dev.espi.protectionstones.PSRegion;
import dev.espi.protectionstones.ProtectionStones;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * The root of the menu: {@code /ps gui}.
 */
final class MainMenu {

    private MainMenu() {}

    static void open(Player p) {
        // resolved on the player's own thread before hopping to the global domain for the region list
        final PSRegion here = PSRegion.fromLocationGroup(p.getLocation());

        Regions.collectPlayerRegions(p, regions -> {
            List<DialogBody> body = Dialogs.bodies(
                    GuiText.MAIN_BODY.msg(),
                    GuiText.MAIN_BODY_COUNT.msg("%count%", regions.size()));

            List<ActionButton> buttons = new ArrayList<>();

            if (here != null && Regions.canUse(p, here)) {
                final String id = here.getId();
                buttons.add(Dialogs.button(GuiText.MAIN_BTN_HERE.msg(), GuiText.MAIN_BTN_HERE_TIP.msg(), pl -> {
                    PSRegion r = Regions.resolve(pl.getWorld(), id);
                    if (r == null) {
                        PSL.msg(pl, GuiText.ERR_REGION_GONE.msg());
                        return;
                    }
                    RegionMenu.open(pl, r);
                }));
            }

            buttons.add(Dialogs.button(GuiText.MAIN_BTN_MY_REGIONS.msg(), GuiText.MAIN_BTN_MY_REGIONS_TIP.msg(),
                    pl -> RegionListMenu.open(pl, 0)));

            if (p.hasPermission("protectionstones.get")) {
                buttons.add(Dialogs.button(GuiText.MAIN_BTN_GET.msg(), GuiText.MAIN_BTN_GET_TIP.msg(),
                        pl -> BlockMenu.open(pl, 0)));
            }

            if (p.hasPermission("protectionstones.toggle")) {
                boolean placementOn = !ProtectionStones.toggleList.contains(p.getUniqueId());
                String label = placementOn ? GuiText.MAIN_BTN_TOGGLE_ON.msg() : GuiText.MAIN_BTN_TOGGLE_OFF.msg();
                buttons.add(Dialogs.button(label, GuiText.MAIN_BTN_TOGGLE_TIP.msg(), pl -> {
                    Regions.runCommand(pl, "toggle");
                    open(pl);
                }));
            }

            if (ProtectionStones.getInstance().getConfigOptions().taxEnabled && p.hasPermission("protectionstones.tax")) {
                buttons.add(Dialogs.button(GuiText.MAIN_BTN_TAX.msg(), GuiText.MAIN_BTN_TAX_TIP.msg(),
                        pl -> TaxMenu.open(pl, 0)));
            }

            buttons.add(Dialogs.button(GuiText.MAIN_BTN_HELP.msg(), MainMenu::openHelp));

            Dialogs.show(p, Dialogs.menu(GuiText.MAIN_TITLE.msg(), body, buttons, Dialogs.closeButton()));
        });
    }

    private static void openHelp(Player p) {
        List<DialogBody> body = Dialogs.bodies(
                GuiText.HELP_BODY_1.msg(),
                GuiText.HELP_BODY_2.msg(),
                GuiText.HELP_BODY_3.msg(),
                GuiText.HELP_BODY_4.msg());

        Dialogs.show(p, Dialogs.notice(GuiText.HELP_TITLE.msg(), body, Dialogs.backButton(MainMenu::open)));
    }
}
