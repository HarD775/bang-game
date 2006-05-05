//
// $Id$

package com.threerings.bang.game.data;

import com.threerings.presents.data.InvocationCodes;

import com.threerings.bang.game.client.BangService;

/**
 * Codes and constants used by the actual game.
 */
public interface GameCodes extends InvocationCodes
{
    /** The message bundle identifier for our translation messages. */
    public static final String GAME_MSGS = "game";

    /** A response code for {@link BangService#move}. */
    public static final Integer EXECUTED_ORDER = 0;

    /** A response code for {@link BangService#move}. */
    public static final Integer QUEUED_ORDER = 1;

    /** An error response code for {@link BangService#move}. */
    public static final String MOVER_NO_LONGER_VALID = "m.mover_invalid";

    /** An error response code for {@link BangService#move}. */
    public static final String MOVE_BLOCKED = "m.move_blocked";

    /** An error response code for {@link BangService#move}. */
    public static final String TARGET_NO_LONGER_VALID = "m.target_invalid";

    /** An error response code for {@link BangService#move}. */
    public static final String TARGET_TOO_FAR = "m.target_too_far";

    /** An error response code for {@link BangService#playCard}. */
    public static final String CARD_UNPLAYABLE = "m.card_unplayable";

    /** An feedback message for {@link BangService#cancelOrder}. */
    public static final String ORDER_CLEARED = "m.order_cleared";

    /** The highest number of players we will allow in a game. */
    public static final int MAX_PLAYERS = 4;

    /** The highest number of rounds we will allow in a game. */
    public static final int MAX_ROUNDS = 3;

    /** Defines the minimum team size (not counting one's big shot). */
    public static final int MIN_TEAM_SIZE = 2;

    /** Defines the maximum team size (not counting one's big shot). */
    public static final int MAX_TEAM_SIZE = 5;

    /** The maximum number of cards a player can hold in a game. */
    public static final int MAX_CARDS = 3;
}
