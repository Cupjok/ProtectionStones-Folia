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

import org.bukkit.configuration.file.YamlConfiguration;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Every piece of text the dialog GUI shows, backed by {@code gui.yml}.
 *
 * <p>This deliberately lives in its own file rather than in {@link dev.espi.protectionstones.PSL}
 * so that pulling in upstream changes never conflicts over the GUI's strings.
 */
public enum GuiText {

    /* ~~~~~~~~~~ shared ~~~~~~~~~~ */

    BUTTON_BACK("button.back", "&7« Back"),
    BUTTON_CLOSE("button.close", "&7Close"),
    BUTTON_CANCEL("button.cancel", "&7Cancel"),
    BUTTON_NEXT_PAGE("button.next_page", "&bNext »"),
    BUTTON_PREV_PAGE("button.previous_page", "&b« Previous"),
    BUTTON_NOTHING_HERE("button.nothing_here", "&8(nothing here)"),
    PAGE_FOOTER("page_footer", "&7Page &f%page%&7 of &f%pages%"),

    ERR_DISABLED("error.disabled", "&cThe region menu is disabled on this server."),
    ERR_UNSUPPORTED("error.unsupported", "&cYour client does not support menus. Use Minecraft 1.21.6+ (or Bedrock via Geyser), or type &f/ps help&c."),
    ERR_NOT_IN_REGION("error.not_in_region", "&cYou are not standing in a region."),
    ERR_NO_ACCESS("error.no_access", "&cYou don't have access to this region."),
    ERR_NO_PERMISSION("error.no_permission", "&cYou don't have permission to do that."),
    ERR_MUST_STAND_IN_REGION("error.must_stand_in_region", "&cYou need to be standing inside &f%region%&c to do that. Teleport there first."),
    ERR_REGION_GONE("error.region_gone", "&cThat region no longer exists."),
    ERR_NO_ECONOMY("error.no_economy", "&cThis server has no economy plugin, so buying and renting are unavailable."),

    /* ~~~~~~~~~~ main menu ~~~~~~~~~~ */

    MAIN_TITLE("main.title", "ProtectionStones"),
    MAIN_BODY("main.body", "&7Manage your protected land without typing commands."),
    MAIN_BODY_COUNT("main.body_count", "&7You own &f%count%&7 region(s)."),
    MAIN_BTN_HERE("main.button.here", "&aRegion You're In"),
    MAIN_BTN_HERE_TIP("main.button.here_tooltip", "&7Manage the region you are standing in."),
    MAIN_BTN_MY_REGIONS("main.button.my_regions", "&aMy Regions"),
    MAIN_BTN_MY_REGIONS_TIP("main.button.my_regions_tooltip", "&7Browse every region you own or belong to."),
    MAIN_BTN_GET("main.button.get", "&aGet a Protection Block"),
    MAIN_BTN_GET_TIP("main.button.get_tooltip", "&7Buy or claim a protection block."),
    MAIN_BTN_TOGGLE_ON("main.button.toggle_on", "&aPlacement: &2ON"),
    MAIN_BTN_TOGGLE_OFF("main.button.toggle_off", "&aPlacement: &cOFF"),
    MAIN_BTN_TOGGLE_TIP("main.button.toggle_tooltip", "&7Whether placing a protection block creates a region."),
    MAIN_BTN_TAX("main.button.tax", "&aTaxes"),
    MAIN_BTN_TAX_TIP("main.button.tax_tooltip", "&7See and pay what your regions owe."),
    MAIN_BTN_HELP("main.button.help", "&aHelp"),

    /* ~~~~~~~~~~ region list ~~~~~~~~~~ */

    LIST_TITLE("list.title", "My Regions"),
    LIST_BODY("list.body", "&7Pick a region to manage it."),
    LIST_EMPTY("list.empty", "&7You don't have any regions yet. Place a protection block to claim one."),
    LIST_ENTRY("list.entry", "&a%name%"),
    LIST_ENTRY_TIP("list.entry_tooltip", "&7%world% &8• &7%x%, %y%, %z%"),

    /* ~~~~~~~~~~ region menu ~~~~~~~~~~ */

    REGION_TITLE("region.title", "%name%"),
    REGION_BODY_ID("region.body.id", "&7ID: &f%id%"),
    REGION_BODY_TYPE("region.body.type", "&7Type: &f%type%"),
    REGION_BODY_WORLD("region.body.world", "&7World: &f%world%"),
    REGION_BODY_OWNERS("region.body.owners", "&7Owners: &f%players%"),
    REGION_BODY_MEMBERS("region.body.members", "&7Members: &f%players%"),
    REGION_BODY_PARENT("region.body.parent", "&7Parent: &f%parent%"),
    REGION_BODY_PRIORITY("region.body.priority", "&7Priority: &f%priority%"),
    REGION_BODY_BOUNDS("region.body.bounds", "&7Bounds: &f%minx%, %minz% &7to &f%maxx%, %maxz%"),
    REGION_BODY_HIDDEN("region.body.hidden", "&7The protection block is &fhidden&7."),
    REGION_BODY_FOR_SALE("region.body.for_sale", "&6For sale: &f$%price%&6 (seller: &f%seller%&6)"),
    REGION_BODY_FOR_RENT("region.body.for_rent", "&6For rent: &f$%price%&6 per &f%period%"),
    REGION_BODY_RENTED("region.body.rented", "&6Rented by &f%tenant%&6 for &f$%price%&6 per &f%period%"),
    REGION_BODY_TAX("region.body.tax", "&7Tax: &f$%amount%&7 owed"),
    REGION_BODY_NONE("region.body.none", "none"),

    REGION_BTN_TELEPORT("region.button.teleport", "&aTeleport"),
    REGION_BTN_SETHOME("region.button.sethome", "&aSet Home Here"),
    REGION_BTN_SETHOME_TIP("region.button.sethome_tooltip", "&7Teleports will land where you are standing now."),
    REGION_BTN_RENAME("region.button.rename", "&aRename"),
    REGION_BTN_MEMBERS("region.button.members", "&aMembers"),
    REGION_BTN_OWNERS("region.button.owners", "&aOwners"),
    REGION_BTN_FLAGS("region.button.flags", "&aFlags"),
    REGION_BTN_FLAGS_TIP("region.button.flags_tooltip", "&7Control what other players may do here."),
    REGION_BTN_ECONOMY("region.button.economy", "&aBuy / Sell / Rent"),
    REGION_BTN_SETTINGS("region.button.settings", "&aMore Settings"),
    REGION_BTN_UNCLAIM("region.button.unclaim", "&cUnclaim"),
    REGION_BTN_VIEW("region.button.view", "&aShow Borders"),
    REGION_BTN_VIEW_TIP("region.button.view_tooltip", "&7Draws particles around the region edge."),

    REGION_INFO_TITLE("region.info.title", "Region Info"),
    REGION_INFO_BODY("region.info.body", "&7This land belongs to someone else."),

    /* ~~~~~~~~~~ rename ~~~~~~~~~~ */

    RENAME_TITLE("rename.title", "Rename Region"),
    RENAME_BODY("rename.body", "&7Give the region a name you can use with &f/ps home&7."),
    RENAME_FIELD("rename.field", "&7Name"),
    RENAME_SUBMIT("rename.submit", "&aSave"),
    RENAME_CLEAR("rename.clear", "&7Leave the field empty to remove the current name."),

    /* ~~~~~~~~~~ members / owners ~~~~~~~~~~ */

    PLAYERS_TITLE_MEMBERS("players.title_members", "Members"),
    PLAYERS_TITLE_OWNERS("players.title_owners", "Owners"),
    PLAYERS_BODY_MEMBERS("players.body_members", "&7Members can build here. Tap one to remove them."),
    PLAYERS_BODY_OWNERS("players.body_owners", "&7Owners can build here and change settings. Tap one to remove them."),
    PLAYERS_EMPTY("players.empty", "&7Nobody yet."),
    PLAYERS_ENTRY("players.entry", "&f%player%"),
    PLAYERS_ENTRY_TIP("players.entry_tooltip", "&cTap to remove %player%."),
    PLAYERS_BTN_ADD("players.button.add", "&a+ Add Player"),
    PLAYERS_ADD_TITLE("players.add.title", "Add Player"),
    PLAYERS_ADD_BODY("players.add.body", "&7Type the player's name."),
    PLAYERS_ADD_FIELD("players.add.field", "&7Player name"),
    PLAYERS_ADD_SUBMIT("players.add.submit", "&aAdd"),
    PLAYERS_REMOVE_TITLE("players.remove.title", "Remove Player"),
    PLAYERS_REMOVE_BODY("players.remove.body", "&7Remove &f%player%&7 from &f%region%&7?"),
    PLAYERS_REMOVE_YES("players.remove.yes", "&cRemove"),

    /* ~~~~~~~~~~ flags ~~~~~~~~~~ */

    FLAGS_TITLE("flags.title", "Flags"),
    FLAGS_BODY("flags.body", "&7Tap a flag to cycle its value."),
    FLAGS_EMPTY("flags.empty", "&7No flags are configured for this region type."),
    FLAGS_ENTRY("flags.entry", "&f%flag%&7: %value%"),
    FLAGS_ENTRY_TIP("flags.entry_tooltip", "&7Applies to: &f%group%&7. Tap to change."),
    FLAGS_VALUE_ALLOW("flags.value.allow", "&aallow"),
    FLAGS_VALUE_DENY("flags.value.deny", "&cdeny"),
    FLAGS_VALUE_TRUE("flags.value.true", "&atrue"),
    FLAGS_VALUE_FALSE("flags.value.false", "&cfalse"),
    FLAGS_VALUE_UNSET("flags.value.unset", "&8default"),
    FLAGS_TEXT_TITLE("flags.text.title", "Set %flag%"),
    FLAGS_TEXT_BODY("flags.text.body", "&7Leave empty to reset this flag to the default."),
    FLAGS_TEXT_FIELD("flags.text.field", "&7Value"),
    FLAGS_TEXT_SUBMIT("flags.text.submit", "&aSave"),

    /* ~~~~~~~~~~ economy ~~~~~~~~~~ */

    ECON_TITLE("economy.title", "Buy / Sell / Rent"),
    ECON_BODY("economy.body", "&7Sell this region outright, or rent it out for income."),
    ECON_BTN_SELL("economy.button.sell", "&aPut Up For Sale"),
    ECON_BTN_STOP_SELL("economy.button.stop_sell", "&cStop Selling"),
    ECON_BTN_BUY("economy.button.buy", "&aBuy For $%price%"),
    ECON_BTN_RENT_OUT("economy.button.rent_out", "&aOffer For Rent"),
    ECON_BTN_STOP_RENT_OUT("economy.button.stop_rent_out", "&cStop Renting Out"),
    ECON_BTN_RENT("economy.button.rent", "&aRent For $%price% / %period%"),
    ECON_BTN_STOP_RENTING("economy.button.stop_renting", "&cStop Renting"),
    ECON_BTN_TAX("economy.button.tax", "&aTaxes"),
    ECON_BTN_AUTOPAY("economy.button.autopay", "&aToggle Tax Autopay"),
    ECON_BTN_AUTOPAY_TIP("economy.button.autopay_tooltip", "&7Pay this region's tax from your balance automatically."),
    ECON_SELL_TITLE("economy.sell.title", "Sell Region"),
    ECON_SELL_BODY("economy.sell.body", "&7How much should this region cost?"),
    ECON_SELL_FIELD("economy.sell.field", "&7Price"),
    ECON_SELL_SUBMIT("economy.sell.submit", "&aList For Sale"),
    ECON_RENT_TITLE("economy.rent.title", "Rent Out Region"),
    ECON_RENT_BODY("economy.rent.body", "&7Set the rent price, then the payment period (for example &f1d&7, &f12h&7, &f30m&7)."),
    ECON_RENT_PRICE_FIELD("economy.rent.price_field", "&7Rent price"),
    ECON_RENT_PERIOD_FIELD("economy.rent.period_field", "&7Period (e.g. 1d)"),
    ECON_RENT_SUBMIT("economy.rent.submit", "&aContinue"),
    ECON_RENT_CONFIRM("economy.rent.confirm", "&aOffer For Rent"),

    /* ~~~~~~~~~~ settings ~~~~~~~~~~ */

    SETTINGS_TITLE("settings.title", "Region Settings"),
    SETTINGS_BODY("settings.body", "&7Less common options for this region."),
    SETTINGS_BTN_PRIORITY("settings.button.priority", "&aPriority: &f%priority%"),
    SETTINGS_BTN_PRIORITY_TIP("settings.button.priority_tooltip", "&7Higher priority wins where regions overlap."),
    SETTINGS_BTN_PARENT("settings.button.parent", "&aParent Region"),
    SETTINGS_BTN_PARENT_TIP("settings.button.parent_tooltip", "&7Inherit flags and members from another region you own."),
    SETTINGS_BTN_HIDE("settings.button.hide", "&aHide Protection Block"),
    SETTINGS_BTN_UNHIDE("settings.button.unhide", "&aUnhide Protection Block"),
    SETTINGS_BTN_MERGE("settings.button.merge", "&aMerge Into Another Region"),
    SETTINGS_PRIORITY_TITLE("settings.priority.title", "Set Priority"),
    SETTINGS_PRIORITY_BODY("settings.priority.body", "&7Enter a whole number."),
    SETTINGS_PRIORITY_FIELD("settings.priority.field", "&7Priority"),
    SETTINGS_PRIORITY_SUBMIT("settings.priority.submit", "&aSave"),
    SETTINGS_PARENT_TITLE("settings.parent.title", "Choose Parent"),
    SETTINGS_PARENT_BODY("settings.parent.body", "&7Pick one of your other regions to inherit from."),
    SETTINGS_PARENT_NONE("settings.parent.none", "&cRemove Parent"),
    SETTINGS_MERGE_TITLE("settings.merge.title", "Merge Region"),
    SETTINGS_MERGE_BODY("settings.merge.body", "&7Pick the region to merge &f%region%&7 into. &cThis cannot be undone."),
    SETTINGS_MERGE_EMPTY("settings.merge.empty", "&7There is no neighbouring region you can merge into."),

    /* ~~~~~~~~~~ get block ~~~~~~~~~~ */

    GET_TITLE("get.title", "Protection Blocks"),
    GET_BODY("get.body", "&7Place one of these to claim land."),
    GET_ENTRY("get.entry", "&a%alias%"),
    GET_ENTRY_TIP("get.entry_tooltip", "&7Price: &f$%price%&7\n&7Size: &f%size%"),
    GET_EMPTY("get.empty", "&7No protection blocks are available to you."),

    /* ~~~~~~~~~~ taxes ~~~~~~~~~~ */

    TAX_TITLE("tax.title", "Taxes"),
    TAX_BODY("tax.body", "&7Regions with tax owing."),
    TAX_EMPTY("tax.empty", "&7Nothing is owed right now."),
    TAX_ENTRY("tax.entry", "&aPay $%amount% — %region%"),
    TAX_ENTRY_TIP("tax.entry_tooltip", "&7Tap to pay this region's outstanding tax."),
    TAX_DISABLED("tax.disabled", "&7Taxes are disabled on this server."),

    /* ~~~~~~~~~~ unclaim ~~~~~~~~~~ */

    UNCLAIM_TITLE("unclaim.title", "Unclaim Region"),
    UNCLAIM_BODY("unclaim.body", "&cThis removes &f%region%&c and its protection. Everything you built stays, but anyone can change it."),
    UNCLAIM_YES("unclaim.yes", "&cUnclaim"),

    /* ~~~~~~~~~~ help ~~~~~~~~~~ */

    HELP_TITLE("help.title", "How Protection Works"),
    HELP_BODY_1("help.body_1", "&7Place a protection block to claim the land around it."),
    HELP_BODY_2("help.body_2", "&7Right-click your protection block to open this menu again."),
    HELP_BODY_3("help.body_3", "&7Add friends under &fMembers&7 so they can build with you."),
    HELP_BODY_4("help.body_4", "&7Break the block, or use &fUnclaim&7, to give the land back."),

    /* ~~~~~~~~~~ /ps help entry ~~~~~~~~~~ */

    COMMAND_HELP("command.help", "&b> &7/ps gui"),
    COMMAND_HELP_DESC("command.help_description", "&7Open the region menu.");

    // in a holder class because enum constants are constructed before the enum's own static fields,
    // and the constructor already needs the pattern
    private static final class Patterns {
        static final Pattern HEX = Pattern.compile("(?<!\\\\)(&#[a-fA-F0-9]{6})");
    }

    private final String path, defaultMessage;
    private String message;

    GuiText(String path, String defaultMessage) {
        this.path = path;
        this.defaultMessage = defaultMessage;
        this.message = colour(defaultMessage);
    }

    public String msg() {
        return message;
    }

    /**
     * @param replacements alternating placeholder/value pairs, e.g. {@code msg("%name%", "home")}
     */
    public String msg(Object... replacements) {
        String s = message;
        for (int i = 0; i + 1 < replacements.length; i += 2) {
            s = s.replace(String.valueOf(replacements[i]), String.valueOf(replacements[i + 1]));
        }
        return s;
    }

    /**
     * Reads every message out of the given config, filling in (and reporting) any that are missing.
     *
     * @return whether the config was modified and needs saving
     */
    static boolean apply(YamlConfiguration yml) {
        boolean changed = false;
        for (GuiText t : values()) {
            String key = "messages." + t.path;
            String value = yml.getString(key);
            if (value == null) {
                yml.set(key, t.defaultMessage);
                value = t.defaultMessage;
                changed = true;
            }
            t.message = colour(value);
        }
        return changed;
    }

    // same colour handling as PSL, so translators can use &a and &#aabbcc
    private static String colour(String msg) {
        Matcher matcher = Patterns.HEX.matcher(msg);
        while (matcher.find()) {
            String color = msg.substring(matcher.start() + 1, matcher.end());
            msg = msg.replace(msg.substring(matcher.start(), matcher.end()), "" + net.md_5.bungee.api.ChatColor.of(color));
        }
        return msg.replace('&', '§');
    }
}
