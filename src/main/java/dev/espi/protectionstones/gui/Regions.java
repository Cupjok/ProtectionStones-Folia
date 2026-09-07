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

import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import dev.espi.protectionstones.PSL;
import dev.espi.protectionstones.PSRegion;
import dev.espi.protectionstones.ProtectionStones;
import dev.espi.protectionstones.utils.UUIDCache;
import dev.espi.protectionstones.utils.WGUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.StringJoiner;
import java.util.UUID;

/**
 * Region lookups and permission checks shared by the menu screens.
 *
 * <p>Menus hold on to a world plus a region id rather than a {@link PSRegion}, because a player can
 * leave a dialog open across a reload, an unclaim, or a merge. Every click re-resolves the region and
 * re-checks access, mirroring what the equivalent {@code /ps} command would have done.
 */
final class Regions {

    private Regions() {}

    /**
     * @return the region, or null if it has been deleted since the menu was drawn
     */
    static PSRegion resolve(World w, String id) {
        if (w == null || id == null) return null;
        try {
            ProtectedRegion pr = WGUtils.getRegionManagerWithWorld(w).getRegion(id);
            return pr == null ? null : PSRegion.fromWGRegion(w, pr);
        } catch (Exception e) {
            return null;
        }
    }

    /** The name players see: the region's alias if it has one, otherwise its id. */
    static String label(PSRegion r) {
        return r.getName() != null ? r.getName() : r.getId();
    }

    /** Owner-level access — the bar for changing anything about the region. */
    static boolean canManage(Player p, PSRegion r) {
        return !WGUtils.hasNoAccess(r.getWGRegion(), p, WorldGuardPlugin.inst().wrapPlayer(p), false);
    }

    /** Owner or member — the bar for seeing the region's details. */
    static boolean canUse(Player p, PSRegion r) {
        return !WGUtils.hasNoAccess(r.getWGRegion(), p, WorldGuardPlugin.inst().wrapPlayer(p), true);
    }

    static boolean isInside(Player p, PSRegion r) {
        return p.getWorld().equals(r.getWorld()) && r.getWGRegion().contains(
                p.getLocation().getBlockX(), p.getLocation().getBlockY(), p.getLocation().getBlockZ());
    }

    /**
     * Some actions (unclaim, merge, buy, rent) run through the existing commands, which always act on
     * the region the player is standing in. Tells the player to go there rather than silently acting
     * on the wrong region.
     */
    static boolean requireInside(Player p, PSRegion r) {
        if (isInside(p, r)) return true;
        PSL.msg(p, GuiText.ERR_MUST_STAND_IN_REGION.msg("%region%", label(r)));
        return false;
    }

    static boolean requireManage(Player p, PSRegion r) {
        if (canManage(p, r)) return true;
        PSL.msg(p, PSL.NO_ACCESS.msg());
        return false;
    }

    static boolean requirePermission(Player p, String permission, String denyMessage) {
        if (p.hasPermission(permission)) return true;
        PSL.msg(p, denyMessage);
        return false;
    }

    static String names(Collection<UUID> uuids) {
        if (uuids == null || uuids.isEmpty()) return GuiText.REGION_BODY_NONE.msg();
        StringJoiner j = new StringJoiner(", ");
        for (UUID uuid : uuids) {
            String name = UUIDCache.getNameFromUUID(uuid);
            j.add(name == null ? uuid.toString().substring(0, 8) : name);
        }
        return j.toString();
    }

    /** A location that stands for the region, for display and for teleport-free scheduling. */
    static Location anchor(PSRegion r) {
        Location home = r.getHome();
        if (home != null) return home;
        Block b = r.getProtectBlock();
        if (b != null) return b.getLocation();
        return r.getWorld().getSpawnLocation();
    }

    /**
     * Collects the regions a player owns or belongs to. Region manager access has to happen on the
     * global domain under Folia, so the result is handed back through a callback (which then runs on
     * that domain — {@link Dialogs#show} hops back to the player's thread on its own).
     */
    static void collectPlayerRegions(Player p, java.util.function.Consumer<java.util.List<PSRegion>> then) {
        // snapshot the player's state here, on their own thread, rather than reading it from the
        // global domain — the same pattern the list commands use
        final UUID uuid = p.getUniqueId();
        final World world = p.getWorld();

        dev.espi.protectionstones.compat.FoliaScheduler
                .callGlobal(() -> dev.espi.protectionstones.PSPlayer.fromUUID(uuid).getPSRegionsCrossWorld(world, true))
                .thenAccept(then);
    }

    /**
     * Runs a {@code /ps} subcommand as the player, so the menu reuses the command's own checks,
     * messages and events rather than duplicating them.
     */
    static void runCommand(Player p, String subcommand) {
        Bukkit.dispatchCommand(p, ProtectionStones.getInstance().getConfigOptions().base_command + " " + subcommand);
    }
}
