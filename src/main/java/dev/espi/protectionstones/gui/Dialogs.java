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

import dev.espi.protectionstones.compat.FoliaScheduler;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.dialog.DialogResponseView;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Thin wrapper around the Paper dialog API, so the menu classes read as a list of buttons instead of
 * a pile of builders. Everything in this package assumes {@link PSGui#isSupported()} is true.
 */
final class Dialogs {

    private Dialogs() {}

    static final int BUTTON_WIDTH = 150;
    static final int WIDE_BUTTON_WIDTH = 310;
    static final int BODY_WIDTH = 380;

    // menus are rebuilt on every open, but a player may click several buttons on the same screen
    // (afterAction NONE), so the callbacks have to survive more than one use
    private static final ClickCallback.Options CALLBACK_OPTIONS = ClickCallback.Options.builder()
            .uses(ClickCallback.UNLIMITED_USES)
            .lifetime(Duration.ofMinutes(30))
            .build();

    /* ~~~~~~~~~~ text ~~~~~~~~~~ */

    static Component comp(String legacy) {
        return LegacyComponentSerializer.legacySection().deserialize(legacy);
    }

    static DialogBody body(String legacy) {
        return DialogBody.plainMessage(comp(legacy), BODY_WIDTH);
    }

    static List<DialogBody> bodies(String... legacyLines) {
        List<DialogBody> l = new ArrayList<>();
        for (String line : legacyLines) {
            if (line != null && !line.isEmpty()) l.add(body(line));
        }
        return l;
    }

    /* ~~~~~~~~~~ actions ~~~~~~~~~~ */

    /**
     * Wraps a click handler so it always runs on the clicking player's own scheduler thread, which is
     * what the rest of the plugin (command handlers) assumes.
     */
    static DialogAction action(BiConsumer<Player, DialogResponseView> handler) {
        return DialogAction.customClick((view, audience) -> {
            if (!(audience instanceof Player p)) return;
            FoliaScheduler.runEntity(p, () -> handler.accept(p, view));
        }, CALLBACK_OPTIONS);
    }

    static DialogAction action(Consumer<Player> handler) {
        return action((p, view) -> handler.accept(p));
    }

    /* ~~~~~~~~~~ buttons ~~~~~~~~~~ */

    static ActionButton button(String label, String tooltip, int width, DialogAction action) {
        ActionButton.Builder b = ActionButton.builder(comp(label)).width(width).action(action);
        if (tooltip != null && !tooltip.isEmpty()) b.tooltip(comp(tooltip));
        return b.build();
    }

    static ActionButton button(String label, String tooltip, Consumer<Player> onClick) {
        return button(label, tooltip, BUTTON_WIDTH, action(onClick));
    }

    static ActionButton button(String label, Consumer<Player> onClick) {
        return button(label, null, BUTTON_WIDTH, action(onClick));
    }

    static ActionButton wideButton(String label, String tooltip, Consumer<Player> onClick) {
        return button(label, tooltip, WIDE_BUTTON_WIDTH, action(onClick));
    }

    /** A button that closes the dialog and does nothing else. */
    static ActionButton closeButton() {
        return button(GuiText.BUTTON_CLOSE.msg(), null, BUTTON_WIDTH, action(p -> {}));
    }

    static ActionButton backButton(Consumer<Player> onClick) {
        return button(GuiText.BUTTON_BACK.msg(), null, BUTTON_WIDTH, action(onClick));
    }

    /* ~~~~~~~~~~ dialogs ~~~~~~~~~~ */

    /**
     * A menu of buttons. {@code exit} is rendered separately at the bottom (back/close).
     */
    static Dialog menu(String title, List<DialogBody> body, List<ActionButton> buttons, ActionButton exit, int columns) {
        List<ActionButton> btns = buttons.isEmpty()
                ? Collections.singletonList(button(GuiText.BUTTON_NOTHING_HERE.msg(), null, WIDE_BUTTON_WIDTH, action(p -> {})))
                : buttons;

        return Dialog.create(f -> f.empty()
                .base(DialogBase.builder(comp(title))
                        .canCloseWithEscape(true)
                        .pause(false)
                        .afterAction(DialogBase.DialogAfterAction.CLOSE)
                        .body(body)
                        .build())
                .type(DialogType.multiAction(btns)
                        .columns(columns)
                        .exitAction(exit)
                        .build()));
    }

    static Dialog menu(String title, List<DialogBody> body, List<ActionButton> buttons, ActionButton exit) {
        return menu(title, body, buttons, exit, 2);
    }

    /** An information screen with a single dismiss button. */
    static Dialog notice(String title, List<DialogBody> body, ActionButton dismiss) {
        return Dialog.create(f -> f.empty()
                .base(DialogBase.builder(comp(title))
                        .canCloseWithEscape(true)
                        .pause(false)
                        .afterAction(DialogBase.DialogAfterAction.CLOSE)
                        .body(body)
                        .build())
                .type(DialogType.notice(dismiss)));
    }

    /** A yes/no screen. */
    static Dialog confirm(String title, List<DialogBody> body, String yesLabel, Consumer<Player> onYes, String noLabel, Consumer<Player> onNo) {
        return Dialog.create(f -> f.empty()
                .base(DialogBase.builder(comp(title))
                        .canCloseWithEscape(true)
                        .pause(false)
                        .afterAction(DialogBase.DialogAfterAction.CLOSE)
                        .body(body)
                        .build())
                .type(DialogType.confirmation(
                        button(yesLabel, null, BUTTON_WIDTH, action(onYes)),
                        button(noLabel, null, BUTTON_WIDTH, action(onNo)))));
    }

    /**
     * A screen with one text field. The submitted value (never null, possibly empty) is passed to
     * {@code onSubmit}.
     */
    static Dialog textPrompt(String title, List<DialogBody> body, String fieldLabel, String initial, int maxLength,
                             String submitLabel, BiConsumer<Player, String> onSubmit, Consumer<Player> onCancel) {
        DialogInput input = DialogInput.text("ps_value", comp(fieldLabel))
                .width(BODY_WIDTH)
                .initial(initial == null ? "" : initial)
                .maxLength(maxLength)
                .build();

        ActionButton submit = button(submitLabel, null, BUTTON_WIDTH, action((p, view) -> {
            String value = view.getText("ps_value");
            onSubmit.accept(p, value == null ? "" : value.trim());
        }));

        return Dialog.create(f -> f.empty()
                .base(DialogBase.builder(comp(title))
                        .canCloseWithEscape(true)
                        .pause(false)
                        .afterAction(DialogBase.DialogAfterAction.CLOSE)
                        .body(body)
                        .inputs(Collections.singletonList(input))
                        .build())
                .type(DialogType.multiAction(Collections.singletonList(submit))
                        .columns(1)
                        .exitAction(backButton(onCancel))
                        .build()));
    }

    /* ~~~~~~~~~~ display ~~~~~~~~~~ */

    static void show(Player p, Dialog d) {
        FoliaScheduler.runEntity(p, () -> p.showDialog(d));
    }
}
