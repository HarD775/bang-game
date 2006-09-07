//
// $Id$

package com.threerings.bang.data;

import com.samskivert.util.ArrayUtil;
import com.samskivert.util.StringUtil;

/**
 * Used to track a statistic comprised of a set of strings that map to numeric
 * counts.
 */
public abstract class StringMapStat extends Stat
{
    /**
     * Returns true if the specified key is contained in this map.
     */
    public boolean containsKey (String key)
    {
        return ArrayUtil.binarySearch(_keys, 0, _keys.length, key) >= 0;
    }

    /**
     * Returns the value to which the specified key maps or zero if it is not
     * contained in this map.
     */
    public int get (String key)
    {
        int iidx = ArrayUtil.binarySearch(_keys, 0, _keys.length, key);
        return (iidx < 0) ? 0 : _values[iidx];
    }

    /**
     * Adds the specified key value pair to this set, overwriting any existing
     * mapping.
     *
     * @return true if the stat's value was changed, false otherwise.
     */
    public boolean put (String key, int value)
    {
        int iidx = getOrCreateEntry(key);
        int ovalue = _values[iidx];
        _values[iidx] = Math.min(value, getMaxValue());
        if (_values[iidx] != ovalue) {
            _modified = true;
            return true;
        }
        return false;
    }

    /**
     * Adds the specified key value pair to this set, overwriting any existing
     * mapping.
     *
     * @return true if the stat's value was changed, false otherwise.
     */
    public boolean increment (String key, int amount)
    {
        int iidx = getOrCreateEntry(key);
        int ovalue = _values[iidx];
        _values[iidx] = Math.min(_values[iidx] + amount, getMaxValue());
        if (_values[iidx] != ovalue) {
            _modified = true;
            return true;
        }
        return false;
    }

    @Override // documentation inherited
    public String valueToString ()
    {
        StringBuffer buf = new StringBuffer("[");
        for (int ii = 0; ii < _keys.length; ii++) {
            if (ii > 0) {
                buf.append(", ");
            }
            buf.append(_keys[ii]).append("->").append(_values[ii]);
        }
        return buf.append("]").toString();
    }

    /**
     * Returns the index of the specified key, creating an entry if necessary.
     */
    protected int getOrCreateEntry (String key)
    {
        int iidx = ArrayUtil.binarySearch(_keys, 0, _keys.length, key);
        if (iidx < 0) {
            iidx = -iidx - 1;
            String[] keys = new String[_keys.length+1];
            System.arraycopy(_keys, 0, keys, 0, iidx);
            System.arraycopy(_keys, iidx, keys, iidx+1, (_keys.length-iidx));
            keys[iidx] = key;
            _keys = keys;
            int[] values = new int[_values.length+1];
            System.arraycopy(_values, 0, values, 0, iidx);
            System.arraycopy(
                _values, iidx, values, iidx+1, (_values.length-iidx));
            _values = values;
        }
        return iidx;
    }

    /**
     * Returns the maximum value to which we'll allow our strings to map (to
     * avoid overflowing our underlying data types and to avoid accumulating
     * forever when we don't really care).
     */
    protected abstract int getMaxValue ();

    /** Contains the string keys in this set. */
    protected String[] _keys = {};

    /** Contains the values in this set. */
    protected int[] _values = {};
}
