//
// $Id$

package com.threerings.bang.data;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Properties;

import com.samskivert.util.StringUtil;
import com.threerings.util.MessageBundle;

import com.threerings.bang.util.BangUtil;

import static com.threerings.bang.Log.log;

/**
 * Loads and manages unit configuration information.
 */
public class UnitConfig
{
    /** Defines a unit's modality. */
    public enum Mode {
        GROUND, AIR, RANGE
    };

    /** Defines a unit's make. */
    public enum Make {
        HUMAN, STEAM, SPIRIT
    };

   /** Defines a unit's rank. */
    public enum Rank {
        NORMAL, SPECIAL, BIGSHOT
    }

    /** The total number of modes. */
    public static final int MODE_COUNT = EnumSet.allOf(Mode.class).size();

    /** The total number of makes. */
    public static final int MAKE_COUNT = EnumSet.allOf(Make.class).size();

    /** The total number of terrain categories. */
    public static final int TERRAIN_CATEGORY_COUNT =
        EnumSet.allOf(TerrainConfig.Category.class).size();

    /** The name of this unit type (ie. <code>frontier_town/gunslinger</code>,
     * etc.). */
    public String type;

    /** The modality of this unit: {@link Mode#GROUND}, {@link Mode#AIR} or
     * {@link Mode#RANGE}. */
    public Mode mode;

    /** The make of this unit: {@link Make#HUMAN}, {@link Make#STEAM} or {@link
     * Make#SPIRIT}. */
    public Make make;

    /** The rank of this unit: {@link Rank#NORMAL}, {@link Rank#SPECIAL} or
     * {@link Rank#BIGSHOT}. */
    public Rank rank;

    /** The distance this unit can see. */
    public int sightDistance;

    /** The distance this unit can move. */
    public int moveDistance;

    /** The minimum distance this unit can shoot. */
    public int minFireDistance;

    /** The maximum distance this unit can shoot. */
    public int maxFireDistance;

    /** The percentage at which this unit deals a returning fire shot to
     * the shooting unit when fired upon. */
    public int returnFire;

    /** This unit's base damage. */
    public int damage;

    /** The number of ticks into the past to record as this piece's first
     * move, thereby making them available for play sooner. */
    public int initiative;

    /** The type of unit this unit duplicates into. */
    public String dupeType;

    /** The cost of this unit in scrip (or a pass for this unit). */
    public int scripCost;

    /** The cost of this unit in coins (or a pass for this unit). */
    public int coinCost;

    /** Our damage adjustments versus other modes and makes. */
    public int[] damageAdjust = new int[MODE_COUNT + MAKE_COUNT];

    /** Our defense adjustments versus other modes and makes. */
    public int[] defenseAdjust = new int[MODE_COUNT + MAKE_COUNT];

    /** The cost of movement over each category of terrain. */
    public int[] movementAdjust = new int[TERRAIN_CATEGORY_COUNT];

    /** The code for the badge required to use this unit in a game or zero if
     * there is no badge requirement. */
    public int badgeCode;

    /** A custom class for this unit, if one was specified. */
    public String unitClass;

    /** Returns a translatable name for the specified unit type. */
    public static String getName (String type)
    {
        int slidx = type.lastIndexOf("/");
        return MessageBundle.qualify(
            BangCodes.UNITS_MSGS, "m." + type.substring(slidx+1));
    }

    /** Returns a translatable tooltip for the specified unit type. */
    public static String getTip (String type)
    {
        return getName(type) + "_descrip";
    }

    /**
     * Returns the unit configuration for the specified unit type.
     */
    public static UnitConfig getConfig (String type)
    {
        ensureUnitsRegistered();
        UnitConfig config = _types.get(type);
        if (config == null) {
            log.warning("Requested unknown unit config '" + type + "'!");
            Thread.dumpStack();
        }
        return config;
    }

    /**
     * Returns an array of configurations for all unit types accessible in
     * the specified town.
     */
    public static UnitConfig[] getTownUnits (String townId)
    {
        ensureUnitsRegistered();
        return _townMap.get(townId);
    }

    /**
     * Returns a filtered array of configurations for all unit types
     * accessible in the specified town of the specified rank.
     */
    public static UnitConfig[] getTownUnits (String townId, Rank rank)
    {
        return getTownUnits(townId, EnumSet.of(rank));
    }

    /**
     * Returns a filtered array of configurations for all unit types
     * accessible in the specified town of the specified rank.
     */
    public static UnitConfig[] getTownUnits (String townId, EnumSet<Rank> ranks)
    {
        UnitConfig[] units = getTownUnits(townId);
        ArrayList<UnitConfig> list = new ArrayList<UnitConfig>();
        for (int ii = 0; ii < units.length; ii++) {
            if (ranks.contains(units[ii].rank)) {
                list.add(units[ii]);
            }
        }
        return list.toArray(new UnitConfig[list.size()]);
    }

    public static void main (String[] args)
    {
        for (UnitConfig config : _types.values()) {
            System.err.println("" + config);
        }
    }

    /**
     * Computes and returns the damage adjustment to be used when a unit
     * of this type attacks a unit of the specified type.
     */
    public int getDamageAdjust (UnitConfig target)
    {
        return damageAdjust[target.mode.ordinal()] +
            damageAdjust[MODE_COUNT + target.make.ordinal()];
    }

    /**
     * Computes and returns the defense adjustment to be used when a unit
     * of this type is attacked by a unit of the specified type.
     */
    public int getDefenseAdjust (UnitConfig attacker)
    {
        return defenseAdjust[attacker.mode.ordinal()] +
            defenseAdjust[MODE_COUNT + attacker.make.ordinal()];
    }

    /** Returns a translatable name for this unit. */
    public String getName ()
    {
        return getName(type);
    }

    /** Returns a translatable tooltip for this unit. */
    public String getTip ()
    {
        return getName(type) + "_descrip";
    }

    /**
     * Returns either "N" or "N - M" to indicate this unit's fire distance.
     */
    public String getDisplayFireDistance ()
    {
        return (minFireDistance == maxFireDistance) ?
            String.valueOf(minFireDistance) :
            minFireDistance + " - " + maxFireDistance;
    }

    /**
     * Returns true if this player has access to this unit for use in game.
     */
    public boolean hasAccess (PlayerObject user)
    {
        if (badgeCode != 0) {
            if (user.holdsBadge(Badge.getType(badgeCode))) {
                return true;
            } else if (user.holdsPass(type)) {
                return true;
            }
            return false;
        }
        return true;
    }

    /** Returns a string representation of this instance. */
    public String toString ()
    {
        return StringUtil.fieldsToString(this);
    }

    protected static void registerUnit (String type)
    {
        // load up the properties file for this unit
        Properties props = BangUtil.resourceToProperties(
            "rsrc/units/" + type + "/unit.properties");

        // fill in a config instance from the properties file
        UnitConfig config = new UnitConfig();
        config.type = type;
        config.unitClass = props.getProperty("class");
        config.badgeCode = BangUtil.getIntProperty(type, props, "badge", 0);

        String modestr =
            BangUtil.requireProperty(type, props, "mode").toUpperCase();
        try {
            config.mode = Enum.valueOf(Mode.class, modestr);
        } catch (Exception e) {
            log.warning("Invalid mode specified [type=" + type +
                        ", mode=" + modestr + "].");
        }
        String makestr =
            BangUtil.requireProperty(type, props, "make").toUpperCase();
        try {
            config.make = Enum.valueOf(Make.class, makestr);
        } catch (Exception e) {
            log.warning("Invalid make specified [type=" + type +
                        ", make=" + makestr + "].");
        }
        String rankstr =
            BangUtil.requireProperty(type, props, "rank").toUpperCase();
        try {
            config.rank = Enum.valueOf(Rank.class, rankstr);
        } catch (Exception e) {
            log.warning("Invalid rank specified [type=" + type +
                        ", rank=" + rankstr + "].");
        }

        config.sightDistance = BangUtil.getIntProperty(type, props, "sight", 5);
        config.moveDistance = BangUtil.getIntProperty(type, props, "move", 1);
        config.minFireDistance =
            BangUtil.getIntProperty(type, props, "min_fire", 1);
        config.maxFireDistance =
            BangUtil.getIntProperty(type, props, "max_fire", 1);
        config.returnFire =
            BangUtil.getIntProperty(type, props, "return_fire", 0);
        config.damage = BangUtil.getIntProperty(type, props, "damage", 25);

        config.initiative =
            BangUtil.getIntProperty(type, props, "initiative", 0);

        config.dupeType = props.getProperty("dupe_type", type);

        config.scripCost =
            BangUtil.getIntProperty(type, props, "scrip_cost", 999);
        config.coinCost = BangUtil.getIntProperty(type, props, "coin_cost", 99);

        int idx = 0;
        for (Mode mode : EnumSet.allOf(Mode.class)) {
            String key = mode.toString().toLowerCase();
            config.damageAdjust[mode.ordinal()] = BangUtil.getIntProperty(
                type, props, "damage." + key, 0);
            config.defenseAdjust[mode.ordinal()] = BangUtil.getIntProperty(
                type, props, "defense." + key, 0);
        }
        for (Make make : EnumSet.allOf(Make.class)) {
            String key = make.toString().toLowerCase();
            config.damageAdjust[MODE_COUNT + make.ordinal()] =
                BangUtil.getIntProperty(type, props, "damage." + key, 0);
            config.defenseAdjust[MODE_COUNT + make.ordinal()] =
                BangUtil.getIntProperty(type, props, "defense." + key, 0);
        }

        for (TerrainConfig.Category tcat :
            EnumSet.allOf(TerrainConfig.Category.class)) {
            String key = tcat.toString().toLowerCase();
            config.movementAdjust[tcat.ordinal()] =
                BangUtil.getIntProperty(type, props, "movement." + key, 0);
        }

        // map this config into the proper towns
        String towns = BangUtil.requireProperty(type, props, "towns");
        boolean andSoOn = false;
        for (int ii = 0; ii < BangCodes.TOWN_IDS.length; ii++) {
            String town = BangCodes.TOWN_IDS[ii];
            if (andSoOn || towns.indexOf(town) != -1) {
                mapTown(town, config);
                andSoOn = andSoOn || (towns.indexOf(town + "+") != -1);
            }
        }

        // map the type to the config
        _types.put(type, config);
    }

    protected static void mapTown (String town, UnitConfig config)
    {
        UnitConfig[] configs = _townMap.get(town);
        if (configs == null) {
            configs = new UnitConfig[0];
        }
        UnitConfig[] nconfigs = new UnitConfig[configs.length+1];
        System.arraycopy(configs, 0, nconfigs, 0, configs.length);
        nconfigs[configs.length] = config;
        _townMap.put(town, nconfigs);
    }

    protected static void ensureUnitsRegistered ()
    {
        if (_types.size() == 0) {
            // register our units
            String[] units = BangUtil.townResourceToStrings(
                "rsrc/units/TOWN/units.txt");
            for (int ii = 0; ii < units.length; ii++) {
                registerUnit(units[ii]);
            }
        }
    }

    /** A mapping from unit type to its configuration. */
    protected static HashMap<String,UnitConfig> _types =
        new HashMap<String,UnitConfig>();

    /** A mapping from town to all units accessible in that town. */
    protected static HashMap<String,UnitConfig[]> _townMap =
        new HashMap<String,UnitConfig[]>();
}
