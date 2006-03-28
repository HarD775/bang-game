//
// $Id$

package com.threerings.bang.data;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import com.threerings.presents.dobj.DSet;
import com.threerings.util.Name;

/**
 * An entry in the list of pardners.
 */
public class PardnerEntry
    implements DSet.Entry, Comparable
{
    /** The pardner is not logged in. */
    public static final byte OFFLINE = 0;
    
    /** The pardner is somewhere other than the saloon or a game. */
    public static final byte ONLINE = 1;
    
    /** The pardner is in the saloon interface. */
    public static final byte IN_SALOON = 2;
    
    /** The pardner is in a game. */
    public static final byte IN_GAME = 3;
    
    /** The pardner's handle. */
    public Name handle;
    
    /** The pardner's avatar. */
    public int[] avatar;
    
    /** The pardner's status ({@link #OFFLINE}, {@link #IN_TOWN}, etc). */
    public byte status;
    
    /**
     * No-arg constructor for deserialization.
     */
    public PardnerEntry ()
    {
    }
    
    /**
     * Constructor for online pardners.
     */
    public PardnerEntry (Name handle)
    {
        this.handle = (handle == null) ? Name.BLANK : handle;
    }
    
    /**
     * Constructor for offline pardners.
     */
    public PardnerEntry (Name handle, Date lastSession)
    {
        this(handle);
        setLastSession(lastSession);
    }
    
    /**
     * Determines whether this pardner is online.
     */
    public boolean isOnline ()
    {
        return status != OFFLINE;
    }
    
    /**
     * Determines whether this pardner is available for chat (i.e., online and
     * not in a game).
     */
    public boolean isAvailable ()
    {
        return status != OFFLINE && status != IN_GAME;
    }
    
    /**
     * Retrieves the date at which the pardner was last online.
     */
    public Date getLastSession ()
    {
        return new Date(_lastSessionEpoch +
            _lastSession * MILLISECONDS_PER_DAY);
    }
    
    /**
     * Sets the date at which the pardner was last online.
     */
    public void setLastSession (Date lastSession)
    {
        _lastSession = (short)((lastSession.getTime() - _lastSessionEpoch) /
            MILLISECONDS_PER_DAY);
    }
    
    // documentation inherited from interface DSet.Entry
    public Comparable getKey ()
    {
        return handle;
    }
    
    // documentation inherited from interface Comparable
    public int compareTo (Object other)
    {
        // sort online pardners above offline ones and available ones above
        // unavailable ones
        PardnerEntry oentry = (PardnerEntry)other;
        if (isOnline() != oentry.isOnline()) {
            return isOnline() ? -1 : +1;
        
        } else if (isAvailable() != oentry.isAvailable()) {
            return isAvailable() ? -1 : +1;
            
        } else {
            return handle.compareTo(oentry.handle);
        }
    }
    
    /** For offline pardners, the date when the pardner last logged on, stored
     * as the number of days since midnight, 1/1/05 GMT. */
    protected short _lastSession;
    
    /** The time from which last session times are measured. */
    protected static long _lastSessionEpoch;
    static {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        cal.set(2005, 0, 1, 0, 0, 0);
        _lastSessionEpoch = cal.getTimeInMillis();
    }
    
    /** The number of milliseconds in a day. */
    protected static final long MILLISECONDS_PER_DAY = 24L * 60 * 60 * 1000;
}
