//
// $Id$

package com.threerings.bang.server;

import java.util.HashMap;

import com.samskivert.util.HashIntMap;

import com.threerings.bang.data.Article;
import com.threerings.bang.data.Badge;
import com.threerings.bang.data.BigShotItem;
import com.threerings.bang.data.CardItem;
import com.threerings.bang.data.Item;
import com.threerings.bang.data.Purse;

import static com.threerings.bang.Log.log;

/**
 * The item factory is responsible for assigning item codes to item
 * classes and instantiating said classes when requested.
 */
public class ItemFactory
{
    /**
     * Returns the type code that is assigned to this derived class of
     * {@link Item}. -1 is returned if the specified class has not been
     * registered with the item factory. A log message is also generated.
     */
    public static int getType (Class itemClass)
    {
        // make sure the item classes are registered
        if (_classToType == null) {
            registerItemClasses();
        }

        // now do the lookup
        Integer type = _classToType.get(itemClass);
        if (type == null) {
            log.warning("No type for item class! " +
                        "[class=" + itemClass.getName() + "].");
            return -1;
        }

        return type.intValue();
    }

    /**
     * Returns the derived class of {@link Item} that is registered for
     * the specified type code. null is returned of no item class is
     * registered for the specified type.
     */
    public static Class getClass (int type)
    {
        // make sure the item classes are registered
        if (_typeToClass == null) {
            registerItemClasses();
        }

        // now do the lookup
        return _typeToClass.get(type);
    }

    /**
     * Registers the item class with the item factory. This should be
     * called below in the canonical list of item registrations.
     */
    protected static void registerItemClass (Class typeClass)
    {
        int type = ++_nextType;
        _typeToClass.put(type, typeClass);
        log.fine("Registering [class=" + typeClass + ", type=" + type + "].");
        _classToType.put(typeClass, type);
    }

    /**
     * New item types must be inserted here to register a type code for
     * the item class.
     *
     * <p><em>NOTE:</em> old item classes must not be removed directly,
     * but must be replaced with a line that increments the type code to
     * preserve the assigned values of the surrounding types.
     */
    protected static void registerItemClasses ()
    {
        // create our tables
        _typeToClass = new HashIntMap<Class>();
        _classToType = new HashMap<Class,Integer>();

        // register the item classes (DO NOT CHANGE ORDER, SEE NOTE ABOVE)
        registerItemClass(BigShotItem.class);
        registerItemClass(CardItem.class);
        registerItemClass(Badge.class);
        registerItemClass(Purse.class);
        registerItemClass(Article.class);
        // end of registeration (DO NOT CHANGE ORDER, SEE NOTE ABOVE)
    }

    /** The table mapping item types to classes. */
    protected static HashIntMap<Class> _typeToClass;

    /** The table mapping item classes to types. */
    protected static HashMap<Class,Integer> _classToType;

    /** A counter used in assigning types to classes. */
    protected static int _nextType = 0;
}
