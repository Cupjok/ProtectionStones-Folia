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
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Browse the regions a player owns or belongs to, and drill into one.
 */
final class RegionListMenu {

    private RegionListMenu() {}

    static void open(Player p, int page) {
        open(p, page, MainMenu::open);
    }

    private static void open(Player p, int page, Consumer<Player> onBack) {
        Regions.collectPlayerRegions(p, regions ->
                render(p, page, GuiText.LIST_TITLE.msg(), GuiText.LIST_BODY.msg(), regions,
                        r -> RegionMenu.open(p, r), onBack, null));
    }

    /**
     * Draws a pick-a-region screen. Also used for the parent and merge pickers, which supply their own
     * candidate list rather than "everything the player owns".
     *
     * @param extraButtons rendered above the regions (for example "remove parent"); may be null
     */
    static void render(Player p, int page, String title, String bodyText, List<PSRegion> regions,
                       Consumer<PSRegion> onPick, Consumer<Player> onBack, List<ActionButton> extraButtons) {
        int pages = Paging.pageCount(regions.size());
        List<PSRegion> slice = Paging.slice(regions, page);

        List<DialogBody> body = Dialogs.bodies(
                bodyText,
                regions.isEmpty() ? GuiText.LIST_EMPTY.msg() : Paging.footer(page, pages));

        List<ActionButton> buttons = new ArrayList<>();
        if (extraButtons != null) buttons.addAll(extraButtons);

        for (PSRegion r : slice) {
            final World world = r.getWorld();
            final String id = r.getId();
            Location anchor = Regions.anchor(r);

            buttons.add(Dialogs.button(
                    GuiText.LIST_ENTRY.msg("%name%", Regions.label(r)),
                    GuiText.LIST_ENTRY_TIP.msg(
                            "%world%", world.getName(),
                            "%x%", anchor.getBlockX(),
                            "%y%", anchor.getBlockY(),
                            "%z%", anchor.getBlockZ()),
                    pl -> {
                        PSRegion resolved = Regions.resolve(world, id);
                        if (resolved == null) {
                            PSL.msg(pl, GuiText.ERR_REGION_GONE.msg());
                            return;
                        }
                        onPick.accept(resolved);
                    }));
        }

        Paging.addNav(buttons, page, pages, next ->
                render(p, next, title, bodyText, regions, onPick, onBack, extraButtons));

        Dialogs.show(p, Dialogs.menu(title, body, buttons, Dialogs.backButton(onBack)));
    }
}
