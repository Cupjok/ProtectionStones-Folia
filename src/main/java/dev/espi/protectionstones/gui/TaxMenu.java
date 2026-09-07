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

import dev.espi.protectionstones.PSPlayer;
import dev.espi.protectionstones.PSRegion;
import dev.espi.protectionstones.ProtectionStones;
import dev.espi.protectionstones.compat.FoliaScheduler;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * What the player's regions owe in tax, and a one-tap way to pay it.
 */
final class TaxMenu {

    private TaxMenu() {}

    static void open(Player p, int page) {
        if (!ProtectionStones.getInstance().getConfigOptions().taxEnabled) {
            Dialogs.show(p, Dialogs.notice(GuiText.TAX_TITLE.msg(),
                    Dialogs.bodies(GuiText.TAX_DISABLED.msg()),
                    Dialogs.backButton(MainMenu::open)));
            return;
        }

        FoliaScheduler.callGlobal(() -> {
            Map<String, Double> owed = new LinkedHashMap<>();
            Map<String, String> labels = new LinkedHashMap<>();

            for (PSRegion r : PSPlayer.fromPlayer(p).getTaxEligibleRegions()) {
                r.updateTaxPayments();

                double total = 0;
                List<PSRegion.TaxPayment> due = r.getTaxPaymentsDue();
                if (due != null) for (PSRegion.TaxPayment t : due) total += t.getAmount();

                if (total > 0) {
                    owed.put(r.getId(), total);
                    labels.put(r.getId(), Regions.label(r));
                }
            }
            return new Object[] { owed, labels };
        }).thenAccept(result -> render(p, page, result));
    }

    @SuppressWarnings("unchecked")
    private static void render(Player p, int page, Object result) {
        Object[] data = (Object[]) result;
        Map<String, Double> owed = (Map<String, Double>) data[0];
        Map<String, String> labels = (Map<String, String>) data[1];

        List<String> ids = new ArrayList<>(owed.keySet());
        int pages = Paging.pageCount(ids.size());
        List<String> slice = Paging.slice(ids, page);

        List<DialogBody> body = Dialogs.bodies(
                GuiText.TAX_BODY.msg(),
                ids.isEmpty() ? GuiText.TAX_EMPTY.msg() : Paging.footer(page, pages));

        List<ActionButton> buttons = new ArrayList<>();
        for (String id : slice) {
            String amount = String.format("%.2f", owed.get(id));
            buttons.add(Dialogs.wideButton(
                    GuiText.TAX_ENTRY.msg("%amount%", amount, "%region%", labels.get(id)),
                    GuiText.TAX_ENTRY_TIP.msg(),
                    pl -> {
                        Regions.runCommand(pl, "tax pay " + amount + " " + id);
                        // /ps tax pay settles on the global domain, so give it a moment before
                        // redrawing — otherwise the menu reopens still showing the old balance
                        FoliaScheduler.runEntityLater(pl, () -> open(pl, page), null, 20L);
                    }));
        }

        Paging.addNav(buttons, page, pages, next -> open(p, next));

        Dialogs.show(p, Dialogs.menu(GuiText.TAX_TITLE.msg(), body, buttons, Dialogs.backButton(MainMenu::open), 1));
    }
}
