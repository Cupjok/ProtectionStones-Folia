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

import dev.espi.protectionstones.PSProtectBlock;
import dev.espi.protectionstones.ProtectionStones;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import org.bukkit.entity.Player;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * The protection blocks a player may obtain — the menu equivalent of {@code /ps get}.
 */
final class BlockMenu {

    private BlockMenu() {}

    static void open(Player p, int page) {
        List<PSProtectBlock> available = new ArrayList<>();
        for (PSProtectBlock b : ProtectionStones.getInstance().getConfiguredBlocks()) {
            // same visibility rules as the chat version of /ps get
            if (!b.permission.isEmpty() && !p.hasPermission(b.permission)) continue;
            if (b.preventPsGet && !p.hasPermission("protectionstones.admin")) continue;
            available.add(b);
        }

        int pages = Paging.pageCount(available.size());
        List<PSProtectBlock> slice = Paging.slice(available, page);

        List<DialogBody> body = Dialogs.bodies(
                GuiText.GET_BODY.msg(),
                available.isEmpty() ? GuiText.GET_EMPTY.msg() : Paging.footer(page, pages));

        DecimalFormat priceFormat = new DecimalFormat("#.##");
        List<ActionButton> buttons = new ArrayList<>();
        for (PSProtectBlock b : slice) {
            final String alias = b.alias;
            buttons.add(Dialogs.button(
                    GuiText.GET_ENTRY.msg("%alias%", alias),
                    GuiText.GET_ENTRY_TIP.msg(
                            "%price%", priceFormat.format(b.price),
                            "%size%", size(b)),
                    pl -> {
                        Regions.runCommand(pl, "get " + alias);
                        open(pl, page);
                    }));
        }

        Paging.addNav(buttons, page, pages, next -> open(p, next));

        Dialogs.show(p, Dialogs.menu(GuiText.GET_TITLE.msg(), body, buttons, Dialogs.backButton(MainMenu::open)));
    }

    private static String size(PSProtectBlock b) {
        if (b.chunkRadius >= 0) return (b.chunkRadius * 2 + 1) + "x" + (b.chunkRadius * 2 + 1) + " chunks";
        return (b.xRadius * 2 + 1) + " x " + (b.zRadius * 2 + 1);
    }
}
