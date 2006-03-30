//
// $Id$

package com.threerings.bang.data;

import com.threerings.presents.data.InvocationCodes;
import com.threerings.util.MessageBundle;

/**
 * Codes and constants.
 */
public interface BangCodes extends InvocationCodes
{
    /** A city code. */
    public static final String FRONTIER_TOWN = "frontier_town";

    /** A city code. */
    public static final String INDIAN_VILLAGE = "indian_village";

    /** A city code. */
    public static final String BOOM_TOWN = "boom_town";

    /** A city code. */
    public static final String GHOST_TOWN = "ghost_town";

    /** A city code. */
    public static final String CITY_OF_GOLD = "city_of_gold";

    /** Enumerates our various town ids in order of accessibility. */
    public static final String[] TOWN_IDS = {
        FRONTIER_TOWN, INDIAN_VILLAGE, BOOM_TOWN, GHOST_TOWN, CITY_OF_GOLD
    };

    /** The message bundle identifier for our translation messages. */
    public static final String BANG_MSGS = "bang";

    /** The message bundle identifier for chat-related translation messages. */
    public static final String CHAT_MSGS = "chat";

    /** The message bundle identifier for our translation messages. */
    public static final String STATS_MSGS = "stats";

    /** The message bundle identifier for our translation messages. */
    public static final String BADGE_MSGS = "badge";

    /** The message bundle identifier for our translation messages. */
    public static final String UNITS_MSGS = "units";

    /** The message bundle identifier for our translation messages. */
    public static final String GOODS_MSGS = "goods";

    /** The message bundle identifier for our translation messages. */
    public static final String CARDS_MSGS = "cards";

    /** The number of offers of each type we publish in the coin exchange. */
    public static final int COINEX_OFFERS_SHOWN = 5;

    /** The maximum number of pardners you can have. */
    public static final int MAX_PARDNERS = 75;

    /** The layer for popups that should never be auto cleared and should be
     * hovered above everything (like the bug report popup). */
    public static final int NEVER_CLEAR_LAYER = 10;

    /** Enumerates the identifiers for our tutorials and the order in which
     * they should be displayed and completed. */
    public static final String[] TUTORIALS = {
        "controls",
        "bonuses_cards",
//         "bigshots",
//         "cattle_rustling",
//         "claim_jumping",
//         "gold_rush",
    };

    /** An error code reported when a financial transaction cannot complete. */
    public static final String INSUFFICIENT_FUNDS =
        MessageBundle.qualify(BANG_MSGS, "e.insufficient_funds");
}
