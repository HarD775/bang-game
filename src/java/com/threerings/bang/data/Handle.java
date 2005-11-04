//
// $Id$

package com.threerings.bang.data;

import java.util.regex.Pattern;

import com.threerings.util.Name;

/**
 * Contains a player's name.
 */
public class Handle extends Name
{
    /** Creates a blank instance for unserialization. */
    public Handle ()
    {
    }

    /**
     * Creates a handle instance with the supplied name.
     */
    public Handle (String name)
    {
        super(name);
    }

    @Override // documentation inherited
    protected String normalize (String name)
    {
        name = name.toLowerCase();
        name = _compactor.matcher(name).replaceAll("");
        return name;
    }

    /** Used to strip spaces from names. */
    protected static Pattern _compactor = Pattern.compile("\\s");
}
