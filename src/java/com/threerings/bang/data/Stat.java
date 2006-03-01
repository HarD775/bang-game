//
// $Id$

package com.threerings.bang.data;

import java.io.IOException;
import java.util.EnumSet;

import com.samskivert.util.HashIntMap;

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;

import com.threerings.presents.dobj.DSet;

import com.threerings.bang.util.BangUtil;

import static com.threerings.bang.Log.log;

/**
 * A base class for persistent statistics tracked on a per-player basis
 * (some for a single game, others for all time).
 */
public abstract class Stat
    implements DSet.Entry, Cloneable
{
    /**
     * Defines the various per-player tracked statistics.
     */
    public static enum Type
    {
        // general statistics
        GAMES_PLAYED(new IntStat()),
        GAMES_WON(new IntStat()),
        GAME_TIME(new IntStat()),
        CONSEC_WINS(new IntStat()),
        CONSEC_LOSSES(new IntStat()),
        LATE_NIGHTS(new IntStat(), true, true),
        TUTORIALS_COMPLETED(new ByteStringSetStat(), true, true),

        // transient (per-session) statistics
        SESSION_GAMES_PLAYED(new IntStat(), false, true),

        // stats accumulated during a game
        DAMAGE_DEALT(new IntStat()),

        // stats accumulated during a game and persisted
        UNITS_KILLED(new IntStat()),
        UNITS_LOST(new IntStat()),
        BONUSES_COLLECTED(new IntStat()),
        CARDS_PLAYED(new IntStat()),
        POINTS_EARNED(new IntStat()),
        CASH_EARNED(new IntStat()),
        DISTANCE_MOVED(new IntStat()),
        SHOTS_FIRED(new IntStat()),

        CATTLE_RUSTLED(new IntStat()),
        NUGGETS_CLAIMED(new IntStat()),

        // stats derived from in-game statistics
        HIGHEST_POINTS(new IntStat()),
        MOST_KILLS(new IntStat()),

        // stats accumulated outside a game
        CHAT_SENT(new IntStat()),
        CHAT_RECEIVED(new IntStat()),
        GAMES_HOSTED(new IntStat()),

        UNUSED(new IntStat());

        /** Returns a new blank stat instance of the specified type. */
        public Stat newStat ()
        {
            return (Stat)_prototype.clone();
        }

        /** Returns the translation key used by this stat. */
        public String key () {
            return "m.stat_" + name().toLowerCase();
        }

        /** Returns the unique code for this stat which is a function of
         * its name. */
        public int code () {
            return _code;
        }

        /** Returns true if this stat is persisted between sessions. */
        public boolean isPersistent ()
        {
            return _persist;
        }

        /** Returns true if this stat is not shown in the stats display. */
        public boolean isHidden ()
        {
            return _hidden;
        }

        // most stats are persistent and not hidden
        Type (Stat prototype) {
            this(prototype, true, false);
        }

        Type (Stat prototype, boolean persist, boolean hidden) {
            _persist = persist;
            _hidden = hidden;

            // configure our prototype
            _prototype = prototype;
            _prototype._type = this;

            // compute our unique code
            _code = BangUtil.crc32(name());

            if (_codeToType.containsKey(_code)) {
                log.warning("Stat type collision! " + this + " and " +
                            _codeToType.get(_code) + " both map to '" +
                            _code + "'.");
            } else {
                _codeToType.put(_code, this);
            }
        }

        protected Stat _prototype;
        protected int _code;
        protected boolean _persist, _hidden;
    };

    /** Provides auxilliary information to statistics during the persisting
     * process. */
    public static interface AuxDataSource
    {
        /** Maps the specified string to a unique integer value. */
        public int getStringCode (Type type, String value);

        /** Maps the specified unique code back to its string value. */
        public String getCodeString (Type type, int code);
    }

    /**
     * Maps a {@link Type}'s code code back to a {@link Type} instance.
     */
    public static Type getType (int code)
    {
        return (Type)_codeToType.get(code);
    }

    /**
     * Returns the type of this statistic.
     */
    public Type getType ()
    {
        return _type;
    }

    /**
     * Returns the integer code to which this statistic's name maps.
     */
    public int getCode ()
    {
        return _type.code();
    }

    /**
     * Returns true if the supplied statistic has been modified since it
     * was loaded from the repository.
     */
    public boolean isModified ()
    {
        return _modified;
    }

    /** Writes our custom streamable fields. */
    public void writeObject (ObjectOutputStream out)
        throws IOException
    {
        out.writeInt(_type.code());
        out.defaultWriteObject();
    }

    /** Reads our custom streamable fields. */
    public void readObject (ObjectInputStream in)
        throws IOException, ClassNotFoundException
    {
        _type = getType(in.readInt());
        in.defaultReadObject();
    }

    /**
     * Serializes this instance for storage in the item database. Derived
     * classes must override this method to implement persistence.
     */
    public abstract void persistTo (ObjectOutputStream out, AuxDataSource aux)
        throws IOException;

    /**
     * Unserializes this item from data obtained from the item database.
     * Derived classes must override this method to implement persistence.
     */
    public abstract void unpersistFrom (ObjectInputStream in, AuxDataSource aux)
        throws IOException, ClassNotFoundException;

    /**
     * Generates a string representation of this instance.
     */
    public String toString ()
    {
        StringBuffer buf = new StringBuffer(_type.name().toLowerCase());
        buf.append("=");
        buf.append(valueToString());
        return buf.toString();
    }

    /**
     * Derived statistics must override this method and render their value
     * to a string. Used by {@link #toString} and to display the value in
     * game.
     */
    public abstract String valueToString ();

    // documentation inherited from DSet.Entry
    public Comparable getKey ()
    {
        return _type.name();
    }

    // documentation inherited from Cloneable
    public Object clone ()
    {
        try {
            return super.clone();
        } catch (Exception e) {
            throw new RuntimeException("Clone failed", e);
        }
    }

    /** The type of the statistic in question. */
    protected transient Type _type;

    /** Indicates whether or not this statistic has been modified since it
     * was loaded from the database. */
    protected transient boolean _modified;

    /** The table mapping stat codes to enumerated types. */
    protected static HashIntMap _codeToType = new HashIntMap();

    /** Trigger the loading of the enum when we load this class. */
    protected static Type _trigger = Type.UNUSED;
}
