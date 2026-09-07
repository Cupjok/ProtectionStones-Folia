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

import io.papermc.paper.registry.data.dialog.ActionButton;

import java.util.Collections;
import java.util.List;
import java.util.function.IntConsumer;

/**
 * Page slicing for the list screens. A dialog only fits a handful of buttons comfortably on a phone,
 * so long lists are cut into pages with previous/next buttons at the bottom.
 */
final class Paging {

    private Paging() {}

    static int pageCount(int totalItems) {
        int per = PSGui.getEntriesPerPage();
        return Math.max(1, (totalItems + per - 1) / per);
    }

    /** Clamps {@code page} into range and returns that page's slice. */
    static <T> List<T> slice(List<T> all, int page) {
        int per = PSGui.getEntriesPerPage();
        int from = clamp(page, pageCount(all.size())) * per;
        if (from >= all.size()) return Collections.emptyList();
        return all.subList(from, Math.min(all.size(), from + per));
    }

    static int clamp(int page, int pages) {
        return Math.max(0, Math.min(page, pages - 1));
    }

    /** Appends previous/next buttons, if there is more than one page. */
    static void addNav(List<ActionButton> buttons, int page, int pages, IntConsumer goTo) {
        if (pages <= 1) return;
        int current = clamp(page, pages);
        if (current > 0) {
            buttons.add(Dialogs.button(GuiText.BUTTON_PREV_PAGE.msg(), pl -> goTo.accept(current - 1)));
        }
        if (current < pages - 1) {
            buttons.add(Dialogs.button(GuiText.BUTTON_NEXT_PAGE.msg(), pl -> goTo.accept(current + 1)));
        }
    }

    static String footer(int page, int pages) {
        return GuiText.PAGE_FOOTER.msg("%page%", clamp(page, pages) + 1, "%pages%", pages);
    }
}
