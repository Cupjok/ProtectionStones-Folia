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

import com.sk89q.worldguard.protection.flags.BooleanFlag;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.flags.StateFlag;
import dev.espi.protectionstones.PSL;
import dev.espi.protectionstones.PSRegion;
import dev.espi.protectionstones.commands.ArgFlag;
import dev.espi.protectionstones.utils.WGUtils;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * The flag editor: what other players may do inside the region.
 *
 * <p>The chat version of this ({@code /ps flag}) renders as clickable columns of text, which is
 * unusable on a phone. Here each flag is a single button that cycles allow → deny → default, and text
 * flags open a text field. Setting the value goes through {@link ArgFlag#setFlag} so WorldGuard's own
 * parsing, the pvp exploit guard and the config defaults all still apply.
 */
final class FlagMenu {

    private FlagMenu() {}

    static void open(Player p, World world, String id, int page) {
        RegionMenu.withRegion(p, world, id, r -> {
            if (!Regions.requirePermission(p, "protectionstones.flags", PSL.NO_PERMISSION_FLAGS.msg())) return;
            if (!Regions.requireManage(p, r)) return;
            if (r.getTypeOptions() == null) {
                PSL.msg(p, GuiText.ERR_REGION_GONE.msg());
                return;
            }

            List<String> allowed = new ArrayList<>(r.getTypeOptions().allowedFlags.keySet());
            int pages = Paging.pageCount(allowed.size());
            List<String> slice = Paging.slice(allowed, page);

            List<DialogBody> body = Dialogs.bodies(
                    GuiText.FLAGS_BODY.msg(),
                    allowed.isEmpty() ? GuiText.FLAGS_EMPTY.msg() : Paging.footer(page, pages));

            List<ActionButton> buttons = new ArrayList<>();
            for (String flagName : slice) {
                Flag<?> flag = Flags.fuzzyMatchFlag(WGUtils.getFlagRegistry(), flagName);
                if (flag == null) continue;

                Object value = r.getWGRegion().getFlag(flag);
                String group = groupOf(r, flag);

                buttons.add(Dialogs.button(
                        GuiText.FLAGS_ENTRY.msg("%flag%", flagName, "%value%", display(flag, value)),
                        GuiText.FLAGS_ENTRY_TIP.msg("%group%", group),
                        pl -> onClick(pl, world, id, page, flagName)));
            }

            Paging.addNav(buttons, page, pages, next -> open(p, world, id, next));

            Dialogs.show(p, Dialogs.menu(GuiText.FLAGS_TITLE.msg(), body, buttons,
                    Dialogs.backButton(pl -> RegionMenu.reopen(pl, world, id)), 1));
        });
    }

    private static void onClick(Player p, World world, String id, int page, String flagName) {
        RegionMenu.withRegion(p, world, id, r -> {
            if (!Regions.requireManage(p, r)) return;

            // same per-flag permission the command enforces
            if (!p.hasPermission("protectionstones.flags.edit." + flagName)) {
                PSL.msg(p, PSL.NO_PERMISSION_PER_FLAG.msg());
                return;
            }

            Flag<?> flag = Flags.fuzzyMatchFlag(WGUtils.getFlagRegistry(), flagName);
            if (flag == null) return;

            Object value = r.getWGRegion().getFlag(flag);
            String group = groupOf(r, flag);

            if (flag instanceof StateFlag) {
                ArgFlag.setFlag(r, p, flagName, next(value == StateFlag.State.ALLOW, value == StateFlag.State.DENY, "allow", "deny"), group);
                open(p, world, id, page);
            } else if (flag instanceof BooleanFlag) {
                ArgFlag.setFlag(r, p, flagName, next(Boolean.TRUE.equals(value), Boolean.FALSE.equals(value), "true", "false"), group);
                open(p, world, id, page);
            } else {
                openTextPrompt(p, world, id, page, flagName, value == null ? "" : value.toString().replace("§", "&"), group);
            }
        });
    }

    /** allow/true → deny/false → default → allow/true */
    private static String next(boolean isFirst, boolean isSecond, String first, String second) {
        if (isFirst) return second;
        if (isSecond) return "none";
        return first;
    }

    private static void openTextPrompt(Player p, World world, String id, int page, String flagName, String current, String group) {
        Dialogs.show(p, Dialogs.textPrompt(
                GuiText.FLAGS_TEXT_TITLE.msg("%flag%", flagName),
                Dialogs.bodies(GuiText.FLAGS_TEXT_BODY.msg()),
                GuiText.FLAGS_TEXT_FIELD.msg(),
                current,
                256,
                GuiText.FLAGS_TEXT_SUBMIT.msg(),
                (pl, value) -> RegionMenu.withRegion(pl, world, id, region -> {
                    if (!Regions.requireManage(pl, region)) return;
                    if (!pl.hasPermission("protectionstones.flags.edit." + flagName)) {
                        PSL.msg(pl, PSL.NO_PERMISSION_PER_FLAG.msg());
                        return;
                    }
                    ArgFlag.setFlag(region, pl, flagName, value.isEmpty() ? "none" : value, group);
                    open(pl, world, id, page);
                }),
                pl -> open(pl, world, id, page)));
    }

    private static String groupOf(PSRegion r, Flag<?> flag) {
        if (flag.getRegionGroupFlag() == null) return "all";
        Object group = r.getWGRegion().getFlag(flag.getRegionGroupFlag());
        return group == null ? "all" : group.toString().toLowerCase().replace("_", "");
    }

    private static String display(Flag<?> flag, Object value) {
        if (value == null) return GuiText.FLAGS_VALUE_UNSET.msg();
        if (flag instanceof StateFlag) {
            return value == StateFlag.State.ALLOW ? GuiText.FLAGS_VALUE_ALLOW.msg() : GuiText.FLAGS_VALUE_DENY.msg();
        }
        if (flag instanceof BooleanFlag) {
            return Boolean.TRUE.equals(value) ? GuiText.FLAGS_VALUE_TRUE.msg() : GuiText.FLAGS_VALUE_FALSE.msg();
        }
        String s = value.toString().replace("§", "&");
        return s.length() > 24 ? s.substring(0, 24) + "…" : s;
    }
}
