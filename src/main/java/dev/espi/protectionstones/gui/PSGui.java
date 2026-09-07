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
import org.bukkit.block.Block;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

import java.io.File;
import java.io.IOException;

/**
 * Entry point for the dialog-based region menu.
 *
 * <p>The menu is aimed at players who cannot comfortably type commands (Bedrock and touch clients in
 * particular — Geyser renders these dialogs as native Bedrock forms). Everything it offers is a
 * front-end for functionality that already exists as a {@code /ps} subcommand.
 *
 * <p>All of it degrades gracefully: on a server without the Paper dialog API, or for a client older
 * than 1.21.6, {@link #isSupported()} is false and players are pointed back at the chat commands.
 */
public final class PSGui {

    private PSGui() {}

    public static final String PERMISSION = "protectionstones.gui";

    private static Boolean supported;

    private static boolean enabled = true;
    private static boolean rightClickOpensMenu = true;
    private static boolean rightClickRequiresEmptyHand = false;
    private static int entriesPerPage = 8;

    /* ~~~~~~~~~~ availability ~~~~~~~~~~ */

    /**
     * @return whether the server provides the Paper dialog API this menu is built on
     */
    public static boolean isSupported() {
        if (supported == null) {
            try {
                Class.forName("io.papermc.paper.dialog.Dialog");
                supported = true;
            } catch (ClassNotFoundException | LinkageError e) {
                supported = false;
            }
        }
        return supported;
    }

    public static boolean isEnabled() {
        return enabled && isSupported();
    }

    static int getEntriesPerPage() {
        return entriesPerPage;
    }

    /* ~~~~~~~~~~ config ~~~~~~~~~~ */

    /**
     * Loads (creating if needed) {@code plugins/ProtectionStones/gui.yml}. Called on enable and on
     * {@code /ps reload}.
     */
    public static void reload() {
        File file = new File(ProtectionStones.getInstance().getDataFolder(), "gui.yml");
        YamlConfiguration yml = new YamlConfiguration();

        if (file.exists()) {
            try {
                yml.load(file);
            } catch (Exception e) { // a broken hand-edit shouldn't silently reset everyone's translations
                ProtectionStones.getPluginLogger().warning("Unable to read gui.yml, using defaults: " + e.getMessage());
                return;
            }
        }

        boolean changed = false;
        if (!yml.isSet("settings.enabled")) {
            yml.set("settings.enabled", true);
            changed = true;
        }
        if (!yml.isSet("settings.right_click_opens_menu")) {
            yml.set("settings.right_click_opens_menu", true);
            changed = true;
        }
        if (!yml.isSet("settings.right_click_requires_empty_hand")) {
            yml.set("settings.right_click_requires_empty_hand", false);
            changed = true;
        }
        if (!yml.isSet("settings.entries_per_page")) {
            yml.set("settings.entries_per_page", 8);
            changed = true;
        }

        enabled = yml.getBoolean("settings.enabled", true);
        rightClickOpensMenu = yml.getBoolean("settings.right_click_opens_menu", true);
        rightClickRequiresEmptyHand = yml.getBoolean("settings.right_click_requires_empty_hand", false);
        entriesPerPage = Math.max(1, Math.min(20, yml.getInt("settings.entries_per_page", 8)));

        changed |= GuiText.apply(yml);

        if (changed) {
            try {
                yml.options().setHeader(java.util.Arrays.asList(
                        "ProtectionStones dialog menu.",
                        "",
                        "The menu needs a Paper-based server on 1.21.6+ and a client on 1.21.6+.",
                        "Bedrock players connected through Geyser see these as native Bedrock forms.",
                        "Colour codes use & (for example &a), and &#aabbcc for hex."));
                yml.save(file);
            } catch (IOException e) {
                ProtectionStones.getPluginLogger().warning("Unable to save gui.yml: " + e.getMessage());
            }
        }
    }

    /* ~~~~~~~~~~ opening ~~~~~~~~~~ */

    /**
     * Opens the root menu, or explains why it can't be opened.
     *
     * @return true, so this can be returned straight out of a command handler
     */
    public static boolean open(Player p) {
        if (!isSupported()) return PSL.msg(p, GuiText.ERR_UNSUPPORTED.msg());
        if (!enabled) return PSL.msg(p, GuiText.ERR_DISABLED.msg());
        MainMenu.open(p);
        return true;
    }

    /**
     * Opens the management menu for a specific region (falling back to the read-only info screen for
     * players who are neither an owner nor a member).
     */
    public static boolean openRegion(Player p, PSRegion r) {
        if (!isSupported()) return PSL.msg(p, GuiText.ERR_UNSUPPORTED.msg());
        if (!enabled) return PSL.msg(p, GuiText.ERR_DISABLED.msg());
        RegionMenu.open(p, r);
        return true;
    }

    /**
     * Handles a right click on a protection block. Kept here rather than in the plugin's listener so
     * that the listener only needs a single call.
     *
     * @return whether the menu was opened
     */
    public static boolean handleRightClick(PlayerInteractEvent e) {
        if (!isEnabled() || !rightClickOpensMenu) return false;
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK) return false;
        if (e.getHand() != EquipmentSlot.HAND) return false; // fires once per hand
        if (e.getPlayer().isSneaking()) return false; // sneak-right-click is the break gesture

        Block b = e.getClickedBlock();
        if (b == null || !ProtectionStones.isProtectBlock(b)) return false;

        // don't hijack placing a block against the protection block
        if (e.isBlockInHand()) return false;
        if (rightClickRequiresEmptyHand && e.getItem() != null) return false;

        Player p = e.getPlayer();
        if (!p.hasPermission(PERMISSION)) return false;

        PSRegion r = PSRegion.fromLocationGroup(b.getLocation());
        if (r == null) return false;

        RegionMenu.open(p, r);
        return true;
    }
}
