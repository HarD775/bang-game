//
// $Id$

package com.threerings.bang.game.data;

import java.util.ArrayList;
import java.util.Iterator;

import com.samskivert.util.ArrayIntSet;
import com.samskivert.util.StringUtil;

import com.threerings.presents.dobj.DSet;
import com.threerings.parlor.game.data.GameObject;

import com.threerings.bang.data.Stat;
import com.threerings.bang.data.StatSet;

import com.threerings.bang.game.data.card.Card;
import com.threerings.bang.game.data.effect.Effect;
import com.threerings.bang.game.data.piece.Bonus;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.Unit;
import com.threerings.bang.game.util.PieceUtil;

import static com.threerings.bang.Log.log;

/**
 * Contains all distributed information for the game.
 */
public class BangObject extends GameObject
{
    /** Used to track runtime metrics for the overall game. */
    public static class GameData
    {
        /** The number of live players remaining in the game. */
        public int livePlayers;

        /** The total power of all players on the board. */
        public int totalPower;

        /** The average power of the live players. */
        public double averagePower;

        /** The number of unclaimed bonuses on the board. */
        public int bonuses;

        /** Clears our accumulator stats in preparation for a recompute. */
        public void clear () {
            livePlayers = 0;
            totalPower = 0;
            bonuses = 0;
        }

        /** Generates a string representation of this instance. */
        public String toString () {
            return StringUtil.fieldsToString(this);
        }
    }

    /** Used to track runtime metrics for each player. */
    public static class PlayerData
    {
        /** The number of still-alive units controlled by this player. */
        public int liveUnits;

        /** The total power (un-damage) controlled by this player. */
        public int power;

        /** This player's power divided by the average power. */
        public double powerFactor;

        /** Clears our accumulator stats in preparation for a recompute. */
        public void clear () {
            liveUnits = 0;
            power = 0;
        }

        /** Generates a string representation of this instance. */
        public String toString () {
            return StringUtil.fieldsToString(this);
        }
    }

    // AUTO-GENERATED: FIELDS START
    /** The field name of the <code>stats</code> field. */
    public static final String STATS = "stats";

    /** The field name of the <code>service</code> field. */
    public static final String SERVICE = "service";

    /** The field name of the <code>townId</code> field. */
    public static final String TOWN_ID = "townId";

    /** The field name of the <code>scenarioId</code> field. */
    public static final String SCENARIO_ID = "scenarioId";

    /** The field name of the <code>boardName</code> field. */
    public static final String BOARD_NAME = "boardName";

    /** The field name of the <code>board</code> field. */
    public static final String BOARD = "board";

    /** The field name of the <code>bigShots</code> field. */
    public static final String BIG_SHOTS = "bigShots";

    /** The field name of the <code>tick</code> field. */
    public static final String TICK = "tick";

    /** The field name of the <code>lastTick</code> field. */
    public static final String LAST_TICK = "lastTick";

    /** The field name of the <code>duration</code> field. */
    public static final String DURATION = "duration";

    /** The field name of the <code>pieces</code> field. */
    public static final String PIECES = "pieces";

    /** The field name of the <code>cards</code> field. */
    public static final String CARDS = "cards";

    /** The field name of the <code>effect</code> field. */
    public static final String EFFECT = "effect";

    /** The field name of the <code>actionId</code> field. */
    public static final String ACTION_ID = "actionId";

    /** The field name of the <code>points</code> field. */
    public static final String POINTS = "points";

    /** The field name of the <code>perRoundEarnings</code> field. */
    public static final String PER_ROUND_EARNINGS = "perRoundEarnings";

    /** The field name of the <code>awards</code> field. */
    public static final String AWARDS = "awards";
    // AUTO-GENERATED: FIELDS END

    /** A {@link #state} constant indicating the pre-game selection phase. */
    public static final int SELECT_PHASE = 4;

    /** A {@link #state} constant indicating the pre-game buying phase. */
    public static final int BUYING_PHASE = 5;

    /** A {@link #state} constant indicating the post-round phase. */
    public static final int POST_ROUND = 6;

    /** Contains statistics on the game, updated every time any change is
     * made to pertinent game state. */
    public transient GameData gdata = new GameData();

    /** Contains statistics on each player, updated every time any change
     * is made to pertinent game state. */
    public transient PlayerData[] pdata;

    /** Used to assign ids to pieces added during the game. */
    public transient int maxPieceId;

    /** This value is set at the end of every round, to inform the players
     * of various interesting statistics. */
    public StatSet[] stats;

    /** The invocation service via which the client communicates with the
     * server. */
    public BangMarshaller service;

    /** The id of the town in which this game is being played. */
    public String townId;

    /** The identifier for the current scenario. */
    public String scenarioId;

    /** The name of the current board. */
    public String boardName;

    /** Contains the representation of the game board. */
    public BangBoard board;

    /** The big shots selected for use by each player. */
    public Unit[] bigShots;

    /** The current board tick count. */
    public short tick;

    /** The tick after which the game will end. This may not be {@link
     * #duration} - 1 because some scenarios may opt to end the game early. */
    public short lastTick;

    /** The maximum number of ticks that will be allowed to elapse before the
     * game is ended. Some scenarios may choose to end the game early (see
     * {@link #lastTick}). */
    public short duration;

    /** Contains information on all pieces on the board. */
    public PieceDSet pieces;

    /** Contains information on all available cards. */
    public DSet cards = new DSet();

    /** A field we use to broadcast applied effects. */
    public Effect effect;

    /** The currently executing action (only used in the tutorial). */
    public int actionId;

    /** Total points earned by each player. */
    public int[] points;

    /** Points earned per player per round, this is only broadcast to the
     * client at the end of the game. */
    public int[][] perRoundEarnings;

    /** Used to report cash and badges awarded at the end of the game. */
    public Award[] awards;

    /** Returns the {@link #pieces} set as an array to allow for
     * simultaneous iteration and removal. */
    public Piece[] getPieceArray ()
    {
        return (Piece[])pieces.toArray(new Piece[pieces.size()]);
    }

    /**
     * Adds a piece directly to the piece set without broadcasting any
     * distributed object events. This is used by entities that are known
     * to run on both the client and server. The board's piece shadow is
     * also updated by this call.
     */
    public void addPieceDirect (Piece piece)
    {
        pieces.addDirect(piece);
        board.updateShadow(null, piece);
    }

    /**
     * Removes a piece directly from the piece set without broadcasting
     * any distributed object events. This is used by entities that are
     * known to run on both the client and server. The board's piece
     * shadow is also updated by this call.
     */
    public void removePieceDirect (Piece piece)
    {
        pieces.removeDirect(piece);
        board.updateShadow(piece, null);
    }
    
    /**
     * Returns a list of pieces that overlap the specified piece given its
     * (hypothetical) current coordinates. If no pieces overlap, null will
     * be returned.
     */
    public ArrayList<Piece> getOverlappers (Piece piece)
    {
        return PieceUtil.getOverlappers(pieces.iterator(), piece);
    }

    /**
     * Returns true if the specified player has live pieces, false if they
     * are totally knocked out.
     */
    public boolean hasLiveUnits (int pidx)
    {
        return countLiveUnits(pidx) > 0;
    }

    /**
     * Returns the number of live units remaining for the specified
     * player.
     */
    public int countLiveUnits (int pidx)
    {
        int pcount = 0;
        if (pieces != null) {
            for (Iterator iter = pieces.iterator(); iter.hasNext(); ) {
                Piece p = (Piece)iter.next();
                if (p.owner == pidx && p instanceof Unit && p.isAlive()) {
                    pcount++;
                }
            }
        }
        return pcount;
    }

    /**
     * Returns the number of dead units on the board.
     */
    public int countDeadUnits ()
    {
        int pcount = 0;
        for (Iterator iter = pieces.iterator(); iter.hasNext(); ) {
            Piece p = (Piece)iter.next();
            if (p.owner >= 0 && p instanceof Unit && !p.isAlive()) {
                pcount++;
            }
        }
        return pcount;
    }

    /**
     * Returns the number of playable cards owned by the specified player.
     */
    public int countPlayerCards (int pidx)
    {
        int ccount = 0;
        for (Iterator iter = cards.iterator(); iter.hasNext(); ) {
            Card card = (Card)iter.next();
            if (card.owner == pidx) {
                ccount++;
            }
        }
        return ccount;
    }

    /**
     * Returns the player piece at the specified coordinates or null if no
     * owned piece exists at those coordinates.
     */
    public Piece getPlayerPiece (int tx, int ty)
    {
        for (Iterator iter = pieces.iterator(); iter.hasNext(); ) {
            Piece p = (Piece)iter.next();
            if (p.owner >= 0 && p.x == tx && p.y == ty) {
                return p;
            }
        }
        return null;
    }

    /**
     * Returns the average number of live units per player.
     */
    public int getAverageUnitCount ()
    {
        int[] pcount = getUnitCount();
        float tunits = 0, tcount = 0;
        for (int ii = 0; ii < pcount.length; ii++) {
            if (pcount[ii] > 0) {
                tunits += pcount[ii];
                tcount++;
            }
        }
        return (int)Math.round(tunits / tcount);
    }

    /**
     * Returns the average number of live units among the specified set
     * of players.
     */
    public int getAverageUnitCount (ArrayIntSet players)
    {
        int[] pcount = getUnitCount();
        float tunits = 0, tcount = 0;
        for (int ii = 0; ii < pcount.length; ii++) {
            if (pcount[ii] > 0 && players.contains(ii)) {
                tunits += pcount[ii];
                tcount++;
            }
        }
        return (int)Math.round(tunits / tcount);
    }

    /**
     * Returns the count of units per player.
     */
    public int[] getUnitCount ()
    {
        int[] pcount = new int[players.length];
        for (Iterator iter = pieces.iterator(); iter.hasNext(); ) {
            Piece p = (Piece)iter.next();
            if (p instanceof Unit && p.isAlive() && p.owner >= 0) {
                pcount[p.owner]++;
            }
        }
        return pcount;
    }

    /**
     * Returns the average power of the specified set of players
     * (referenced by index).
     */
    public double getAveragePower (ArrayIntSet players)
    {
        double tpower = 0;
        for (int ii = 0; ii < players.size(); ii++) {
            tpower += pdata[players.get(ii)].power;
        }
        return tpower/players.size();
    }

    /**
     * Returns the average damage level of all live units owned by the
     * specified players.
     */
    public int getAverageUnitDamage (ArrayIntSet players)
    {
        int pcount = 0, tdamage = 0;
        for (Iterator iter = pieces.iterator(); iter.hasNext(); ) {
            Piece p = (Piece)iter.next();
            if (p instanceof Unit && p.isAlive() && players.contains(p.owner)) {
                pcount++;
                tdamage += p.damage;
            }
        }
        return tdamage / pcount;
    }

    /**
     * Updates the {@link #gdata} and {@link #pdata} information.
     */
    public void updateData ()
    {
        // don't do any computation on the client
        if (pdata == null) {
            return;
        }

        // first clear out the old stats
        gdata.clear();
        for (int ii = 0; ii < pdata.length; ii++) {
            pdata[ii].clear();
        }

        Piece[] pieces = getPieceArray();
        int pcount = players.length;
        for (int ii = 0; ii < pieces.length; ii++) {
            Piece p = pieces[ii];
            if (p instanceof Bonus) {
                gdata.bonuses++;
            } else if (p.isAlive() && p.owner >= 0) {
                pdata[p.owner].liveUnits++;
                int pp = (100 - p.damage);
                pdata[p.owner].power += pp;
                gdata.totalPower += pp;
//                 if (p.ticksUntilMovable(prevTick) == 0) {
//                     nonactors[p.owner]++;
//                 }
            }
        }

        for (int ii = 0; ii < pdata.length; ii++) {
            if (pdata[ii].liveUnits > 0) {
                gdata.livePlayers++;
            }
        }

        gdata.averagePower = (double)gdata.totalPower / gdata.livePlayers;
        for (int ii = 0; ii < pdata.length; ii++) {
            pdata[ii].powerFactor =
                (double)pdata[ii].power / gdata.averagePower;
        }

//         log.info("Updated stats " + gdata + ": " +
//                  StringUtil.toString(pdata));
    }

    /**
     * Grants the specified number of points to the specified player, updating
     * their {@link #points} and updating the appropriate earned points
     * statistic.
     */
    public void grantPoints (int pidx, int amount)
    {
        setPointsAt(points[pidx] + amount, pidx);
        perRoundEarnings[roundId-1][pidx] += amount;
        stats[pidx].incrementStat(Stat.Type.POINTS_EARNED, amount);
    }

    /**
     * Returns an adjusted points array where players that have resigned from
     * the game are adjusted to zero.
     */
    public int[] getFilteredPoints ()
    {
        int[] apoints = (int[])points.clone();
        for (int ii = 0; ii < apoints.length; ii++) {
            if (!isActivePlayer(ii)) {
                apoints[ii] = 0;
            }
        }
        return apoints;
    }

    @Override // documentation inherited
    public boolean isInPlay ()
    {
        return (state == BUYING_PHASE || state == SELECT_PHASE ||
                state == IN_PLAY || state == POST_ROUND);
    }

    // AUTO-GENERATED: METHODS START
    /**
     * Requests that the <code>stats</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setStats (StatSet[] value)
    {
        StatSet[] ovalue = this.stats;
        requestAttributeChange(
            STATS, value, ovalue);
        this.stats = (value == null) ? null : (StatSet[])value.clone();
    }

    /**
     * Requests that the <code>index</code>th element of
     * <code>stats</code> field be set to the specified value.
     * The local value will be updated immediately and an event will be
     * propagated through the system to notify all listeners that the
     * attribute did change. Proxied copies of this object (on clients)
     * will apply the value change when they received the attribute
     * changed notification.
     */
    public void setStatsAt (StatSet value, int index)
    {
        StatSet ovalue = this.stats[index];
        requestElementUpdate(
            STATS, index, value, ovalue);
        this.stats[index] = value;
    }

    /**
     * Requests that the <code>service</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setService (BangMarshaller value)
    {
        BangMarshaller ovalue = this.service;
        requestAttributeChange(
            SERVICE, value, ovalue);
        this.service = value;
    }

    /**
     * Requests that the <code>townId</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setTownId (String value)
    {
        String ovalue = this.townId;
        requestAttributeChange(
            TOWN_ID, value, ovalue);
        this.townId = value;
    }

    /**
     * Requests that the <code>scenarioId</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setScenarioId (String value)
    {
        String ovalue = this.scenarioId;
        requestAttributeChange(
            SCENARIO_ID, value, ovalue);
        this.scenarioId = value;
    }

    /**
     * Requests that the <code>boardName</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setBoardName (String value)
    {
        String ovalue = this.boardName;
        requestAttributeChange(
            BOARD_NAME, value, ovalue);
        this.boardName = value;
    }

    /**
     * Requests that the <code>board</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setBoard (BangBoard value)
    {
        BangBoard ovalue = this.board;
        requestAttributeChange(
            BOARD, value, ovalue);
        this.board = value;
    }

    /**
     * Requests that the <code>bigShots</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setBigShots (Unit[] value)
    {
        Unit[] ovalue = this.bigShots;
        requestAttributeChange(
            BIG_SHOTS, value, ovalue);
        this.bigShots = (value == null) ? null : (Unit[])value.clone();
    }

    /**
     * Requests that the <code>index</code>th element of
     * <code>bigShots</code> field be set to the specified value.
     * The local value will be updated immediately and an event will be
     * propagated through the system to notify all listeners that the
     * attribute did change. Proxied copies of this object (on clients)
     * will apply the value change when they received the attribute
     * changed notification.
     */
    public void setBigShotsAt (Unit value, int index)
    {
        Unit ovalue = this.bigShots[index];
        requestElementUpdate(
            BIG_SHOTS, index, value, ovalue);
        this.bigShots[index] = value;
    }

    /**
     * Requests that the <code>tick</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setTick (short value)
    {
        short ovalue = this.tick;
        requestAttributeChange(
            TICK, new Short(value), new Short(ovalue));
        this.tick = value;
    }

    /**
     * Requests that the <code>lastTick</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setLastTick (short value)
    {
        short ovalue = this.lastTick;
        requestAttributeChange(
            LAST_TICK, new Short(value), new Short(ovalue));
        this.lastTick = value;
    }

    /**
     * Requests that the <code>duration</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setDuration (short value)
    {
        short ovalue = this.duration;
        requestAttributeChange(
            DURATION, new Short(value), new Short(ovalue));
        this.duration = value;
    }

    /**
     * Requests that the specified entry be added to the
     * <code>pieces</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    public void addToPieces (DSet.Entry elem)
    {
        requestEntryAdd(PIECES, pieces, elem);
    }

    /**
     * Requests that the entry matching the supplied key be removed from
     * the <code>pieces</code> set. The set will not change until the
     * event is actually propagated through the system.
     */
    public void removeFromPieces (Comparable key)
    {
        requestEntryRemove(PIECES, pieces, key);
    }

    /**
     * Requests that the specified entry be updated in the
     * <code>pieces</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    public void updatePieces (DSet.Entry elem)
    {
        requestEntryUpdate(PIECES, pieces, elem);
    }

    /**
     * Requests that the <code>pieces</code> field be set to the
     * specified value. Generally one only adds, updates and removes
     * entries of a distributed set, but certain situations call for a
     * complete replacement of the set value. The local value will be
     * updated immediately and an event will be propagated through the
     * system to notify all listeners that the attribute did
     * change. Proxied copies of this object (on clients) will apply the
     * value change when they received the attribute changed notification.
     */
    public void setPieces (PieceDSet value)
    {
        requestAttributeChange(PIECES, value, this.pieces);
        this.pieces = (value == null) ? null : (PieceDSet)value.clone();
    }

    /**
     * Requests that the specified entry be added to the
     * <code>cards</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    public void addToCards (DSet.Entry elem)
    {
        requestEntryAdd(CARDS, cards, elem);
    }

    /**
     * Requests that the entry matching the supplied key be removed from
     * the <code>cards</code> set. The set will not change until the
     * event is actually propagated through the system.
     */
    public void removeFromCards (Comparable key)
    {
        requestEntryRemove(CARDS, cards, key);
    }

    /**
     * Requests that the specified entry be updated in the
     * <code>cards</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    public void updateCards (DSet.Entry elem)
    {
        requestEntryUpdate(CARDS, cards, elem);
    }

    /**
     * Requests that the <code>cards</code> field be set to the
     * specified value. Generally one only adds, updates and removes
     * entries of a distributed set, but certain situations call for a
     * complete replacement of the set value. The local value will be
     * updated immediately and an event will be propagated through the
     * system to notify all listeners that the attribute did
     * change. Proxied copies of this object (on clients) will apply the
     * value change when they received the attribute changed notification.
     */
    public void setCards (DSet value)
    {
        requestAttributeChange(CARDS, value, this.cards);
        this.cards = (value == null) ? null : (DSet)value.clone();
    }

    /**
     * Requests that the <code>effect</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setEffect (Effect value)
    {
        Effect ovalue = this.effect;
        requestAttributeChange(
            EFFECT, value, ovalue);
        this.effect = value;
    }

    /**
     * Requests that the <code>actionId</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setActionId (int value)
    {
        int ovalue = this.actionId;
        requestAttributeChange(
            ACTION_ID, new Integer(value), new Integer(ovalue));
        this.actionId = value;
    }

    /**
     * Requests that the <code>points</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setPoints (int[] value)
    {
        int[] ovalue = this.points;
        requestAttributeChange(
            POINTS, value, ovalue);
        this.points = (value == null) ? null : (int[])value.clone();
    }

    /**
     * Requests that the <code>index</code>th element of
     * <code>points</code> field be set to the specified value.
     * The local value will be updated immediately and an event will be
     * propagated through the system to notify all listeners that the
     * attribute did change. Proxied copies of this object (on clients)
     * will apply the value change when they received the attribute
     * changed notification.
     */
    public void setPointsAt (int value, int index)
    {
        int ovalue = this.points[index];
        requestElementUpdate(
            POINTS, index, new Integer(value), new Integer(ovalue));
        this.points[index] = value;
    }

    /**
     * Requests that the <code>perRoundEarnings</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setPerRoundEarnings (int[][] value)
    {
        int[][] ovalue = this.perRoundEarnings;
        requestAttributeChange(
            PER_ROUND_EARNINGS, value, ovalue);
        this.perRoundEarnings = (value == null) ? null : (int[][])value.clone();
    }

    /**
     * Requests that the <code>index</code>th element of
     * <code>perRoundEarnings</code> field be set to the specified value.
     * The local value will be updated immediately and an event will be
     * propagated through the system to notify all listeners that the
     * attribute did change. Proxied copies of this object (on clients)
     * will apply the value change when they received the attribute
     * changed notification.
     */
    public void setPerRoundEarningsAt (int[] value, int index)
    {
        int[] ovalue = this.perRoundEarnings[index];
        requestElementUpdate(
            PER_ROUND_EARNINGS, index, value, ovalue);
        this.perRoundEarnings[index] = value;
    }

    /**
     * Requests that the <code>awards</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setAwards (Award[] value)
    {
        Award[] ovalue = this.awards;
        requestAttributeChange(
            AWARDS, value, ovalue);
        this.awards = (value == null) ? null : (Award[])value.clone();
    }

    /**
     * Requests that the <code>index</code>th element of
     * <code>awards</code> field be set to the specified value.
     * The local value will be updated immediately and an event will be
     * propagated through the system to notify all listeners that the
     * attribute did change. Proxied copies of this object (on clients)
     * will apply the value change when they received the attribute
     * changed notification.
     */
    public void setAwardsAt (Award value, int index)
    {
        Award ovalue = this.awards[index];
        requestElementUpdate(
            AWARDS, index, value, ovalue);
        this.awards[index] = value;
    }
    // AUTO-GENERATED: METHODS END
}
