//
// $Id$

package com.threerings.bang.avatar.util;

import java.io.IOException;
import java.util.HashSet;

import com.samskivert.util.ArrayIntSet;
import com.samskivert.util.IntListUtil;
import com.samskivert.util.StringUtil;

import com.threerings.media.image.ColorPository;
import com.threerings.media.image.Colorization;
import com.threerings.resource.ResourceManager;
import com.threerings.util.CompiledConfig;

import com.threerings.cast.CharacterComponent;
import com.threerings.cast.CharacterDescriptor;
import com.threerings.cast.ComponentClass;
import com.threerings.cast.ComponentRepository;
import com.threerings.cast.NoSuchComponentException;

import com.threerings.bang.data.Article;
import com.threerings.bang.data.PlayerObject;

import com.threerings.bang.avatar.data.AvatarCodes;
import com.threerings.bang.avatar.data.Look;
import com.threerings.bang.avatar.data.LookConfig;

import static com.threerings.bang.Log.log;

/**
 * Used to calculate various things about avatars, decode avatar fingerprints
 * and whatnot.
 */
public class AvatarLogic
{
    /** Defines a particular aspect of an avatar's look. An aspect will
     * configure one or more character components in the avatar's look. */
    public static class Aspect
    {
        /** A string identifier for this aspect. Translated for display on the
         * client. */
        public String name;

        /** The names of the component classes configured by this aspect. */
        public String[] classes;

        /** Indicates whether or not this aspect can be omitted. */
        public boolean optional;

        /** Indicates that this aspect is only for male avatars. */
        public boolean maleOnly;

        public Aspect (String name, String[] classes,
                       boolean optional, boolean maleOnly)
        {
            this.name = name;
            this.classes = classes;
            this.optional = optional;
            this.maleOnly = maleOnly;
        }

        public String toString ()
        {
            return StringUtil.fieldsToString(this);
        }
    }

    /** Defines the various aspects of an avatar's look. */
    public static final Aspect[] ASPECTS = {
        new Aspect("head", new String[] { "head" }, false, false),
        new Aspect("hair", new String[] {
            "hair_front", "hair_middle", "hair_back" }, false, false),
        new Aspect("eyebrows", new String[] { "eyebrows" }, false, false),
        new Aspect("eyes", new String[] { "eyes" }, false, false),
        new Aspect("nose", new String[] { "nose" }, false, false),
        new Aspect("mouth", new String[] { "mouth" }, false, false),
        new Aspect("mustache", new String[] { "mustache" }, true, true),
        new Aspect("beard", new String[] { "beard", "beard_back" }, true, true),
    };

    /** Defines the various article slots available to an avatar. */
    public static final Aspect[] SLOTS = {
        new Aspect("hat", new String[] {
            "hat", "hat_back", "hat_band" }, true, false),
        new Aspect("clothing", new String[] {
            "clothing_back", "clothing_front", "clothing_props" },
                   false, false),
        new Aspect("glasses", new String[] { "glasses" }, true, false),
        new Aspect("jewelry", new String[] { "jewelry" }, true, false),
        new Aspect("makeup", new String[] { "makeup" }, true, false),
        new Aspect("familiar", new String[] { "familiar" }, true, false),
    };

    /** The colorization class for skin colors. */
    public static final String SKIN = "skin";

    /** The colorization class for hair colors. */
    public static final String HAIR = "hair";

    /** The colorization class for eye colors. */
    public static final String EYES = "iris_t";

    /** The width of our avatar source images. */
    public static final int FRAMED_WIDTH = 468;

    /** The width of our avatar source images. */
    public static final int WIDTH = 540;

    /** The height of our avatar source images. */
    public static final int HEIGHT = 600;

    /**
     * Returns the index in the {@link #SLOTS} array of the specified slot.
     */
    public static int getSlotIndex (String slot)
    {
        for (int ii = 0; ii < SLOTS.length; ii++) {
            if (SLOTS[ii].name.equals(slot)) {
                return ii;
            }
        }
        return -1;
    }

    /**
     * Returns the appropriate index for this color class (which depends on
     * whether it is primary, secondary or tertiary).
     */
    public static int getColorIndex (String cclass)
    {
        if (cclass.endsWith("_p")) {
            return 0;
        } else if (cclass.endsWith("_s")) {
            return 1;
        } else if (cclass.endsWith("_t")) {
            return 2;
        } else  {
            log.warning("Requested color index for non-indexed color " +
                        "[cclass=" + cclass + "].");
            return 0;
        }
    }

    /**
     * Creates a colorization mask with the specified colorization id in the
     * appropriate position for the specified colorization class.
     */
    public static int composeZation (String cclass, int colorId)
    {
        switch (getColorIndex(cclass)) {
        default:
        case 0: return composeZations(colorId, 0, 0);
        case 1: return composeZations(0, colorId, 0);
        case 2: return composeZations(0, 0, colorId);
        }
    }

    /**
     * Creates a colorization mask with the specified three colorization ids
     * (which may be zero if the component in question does not require
     * secondary or tertiary colorizations). This value can then be provided to
     * {@link #createArticle}.
     */
    public static int composeZations (int primary, int secondary, int tertiary)
    {
        return (primary << 16) | (secondary << 21) | (tertiary << 26);
    }

    /** Decodes the global skin colorization. */
    public static int decodeSkin (int value)
    {
        return value & 0x1F;
    }

    /** Decodes the global hair colorization. */
    public static int decodeHair (int value)
    {
        return (value >> 5) & 0x1F;
    }

    /** Decodes the primary colorization. */
    public static int decodePrimary (int value)
    {
        return (value >> 16) & 0x1F;
    }

    /** Decodes the secondary colorization. */
    public static int decodeSecondary (int value)
    {
        return (value >> 21) & 0x1F;
    }

    /** Decodes the tertiary colorization. */
    public static int decodeTertiary (int value)
    {
        return (value >> 26) & 0x1F;
    }

    /**
     * Creates a logic instance which will make use of the supplied sources to
     * obtain avatar related information.
     */
    public AvatarLogic (ResourceManager rsrcmgr, ComponentRepository crepo)
        throws IOException
    {
        _crepo = crepo;
        _pository = ColorPository.loadColorPository(rsrcmgr);
        _aspcat = (AspectCatalog)CompiledConfig.loadConfig(
            rsrcmgr.getResource(AspectCatalog.CONFIG_PATH));
        _artcat = (ArticleCatalog)CompiledConfig.loadConfig(
            rsrcmgr.getResource(ArticleCatalog.CONFIG_PATH));
    }

    /**
     * Returns the repository which defines our various recolorizations.
     */
    public ColorPository getColorPository ()
    {
        return _pository;
    }

    /**
     * Returns the catalog that defines the various avatar aspects.
     */
    public AspectCatalog getAspectCatalog ()
    {
        return _aspcat;
    }

    /**
     * Returns the catalog that defines the various avatar articles.
     */
    public ArticleCatalog getArticleCatalog ()
    {
        return _artcat;
    }

    /**
     * Decodes an avatar fingerprint into a {@link CharacterDescriptor} that
     * can be passed to the character manager.
     */
    public CharacterDescriptor decodeAvatar (int[] avatar)
    {
        // decode the skin and hair colorizations
        _globals[0] = _pository.getColorization(SKIN, decodeSkin(avatar[0]));
        _globals[1] = _pository.getColorization(HAIR, decodeHair(avatar[0]));

        // compact the array to remove unused entries
        avatar = IntListUtil.compact(avatar);

        // the subsequent elements are article colorizations and component ids
        // composed into a single integer
        int clength = avatar.length-1;
        int[] componentIds = new int[clength];
        Colorization[][] zations = new Colorization[clength][];
        for (int ii = 0; ii < clength; ii++) {
            int pvalue = avatar[ii+1];
            componentIds[ii] = (pvalue & 0xFFFF);
            zations[ii] = decodeColorizations(pvalue);
        }

        return new CharacterDescriptor(componentIds, zations);
    }

    /**
     * Decodes and returns the colorizations encoded into the supplied encoded
     * component.
     */
    public Colorization[] decodeColorizations (int fqComponentId)
    {
        // look up the component in the repository
        int componentId = (fqComponentId & 0xFFFF);
        CharacterComponent ccomp = null;
        try {
            ccomp = _crepo.getComponent(componentId);
        } catch (NoSuchComponentException nsce) {
            log.warning("Avatar contains non-existent component " +
                        "[compId=" + componentId + "].");
            return null;
        }

        // decode the colorization color id values
        _colors[0] = decodePrimary(fqComponentId);
        _colors[1] = decodeSecondary(fqComponentId);
        _colors[2] = decodeTertiary(fqComponentId);

        // look up the actual colorizations from those
        String[] colors = ccomp.componentClass.colors;
        Colorization[] zations = new Colorization[colors.length];
        for (int cc = 0; cc < colors.length; cc++) {
            if (colors[cc].equals(SKIN)) {
                zations[cc] = _globals[0];
            } else if (colors[cc].equals(HAIR)) {
                zations[cc] = _globals[1];
            } else {
                zations[cc] = _pository.getColorization(
                    colors[cc], _colors[getColorIndex(colors[cc])]);
            }
        }

//         log.info("Decoded colors for " + ccomp.name + " into " +
//                  StringUtil.toString(zations) + " using " +
//                  StringUtil.toString(colors) + " and " +
//                  StringUtil.toString(_colors));

        return zations;
    }

    /**
     * Creates a new {@link Look} with the specified configuration.
     *
     * @return the newly created look or null if the look configuration was
     * invalid for some reason (in which case an error will have been logged).
     *
     * @param user the user for whom we are creating the look.
     * @param cost a two element array into which the scrip and coin cost of
     * the look will be filled in (in that order).
     */
    public Look createLook (PlayerObject user, LookConfig config, int[] cost)
    {
        String gender = user.isMale ? "male/" : "female/";
        int scrip = AvatarCodes.BASE_LOOK_SCRIP_COST,
            coins = AvatarCodes.BASE_LOOK_COIN_COST;
        ArrayIntSet compids = new ArrayIntSet();
        for (int ii = 0; ii < config.aspects.length; ii++) {
            AvatarLogic.Aspect aclass = AvatarLogic.ASPECTS[ii];
            String acname = gender + aclass.name;
            if (config.aspects[ii] == null) {
                if (aclass.optional) {
                    continue;
                }
                log.warning("Requested to purchase look that is missing a " +
                            "non-optional aspect [who=" + user.who() +
                            ", class=" + acname + "].");
                return null;
            }

            AspectCatalog.Aspect aspect =
                _aspcat.getAspect(acname, config.aspects[ii]);
            if (aspect == null) {
                log.warning("Requested to purchase look with unknown aspect " +
                            "[who=" + user.who() + ", class=" + acname +
                            ", choice=" + config.aspects[ii] + "].");
                return null;
            }

            // add the cost to the total cost
            scrip += aspect.scrip;
            coins += aspect.coins;

            // look up the aspect's components
            for (int cc = 0; cc < aclass.classes.length; cc++) {
                String cclass = gender + aclass.classes[cc];
                try {
                    CharacterComponent ccomp = _crepo.getComponent(
                        cclass, aspect.name);
                    int compmask = ccomp.componentId;
                    if (config.colors[ii] != 0) {
                        // TODO: additional costs for some colors?
                        compmask |= config.colors[ii];
                    }
                    compids.add(compmask);
                } catch (NoSuchComponentException nsce) {
                    // no problem, some of these are optional
                }
            }
        }

        Look look = new Look();
        look.name = config.name;
        look.aspects = new int[compids.size()+1];
        // TODO: additional costs for some hair and skin colorizations?
        look.aspects[0] = (config.hair << 5) | config.skin;
        compids.toIntArray(look.aspects, 1);

        cost[0] = scrip;
        cost[1] = coins;
        return look;
    }

    /**
     * Creates an inventory article from an article catalog entry and a
     * colorization mask.
     */
    public Article createArticle (
        int playerId, ArticleCatalog.Article article, int zations)
    {
        // sanity check the slot name
        Aspect slot = null;
        for (int ii = 0; ii < SLOTS.length; ii++) {
            if (SLOTS[ii].name.equals(article.slot)) {
                slot = SLOTS[ii];
                break;
            }
        }
        if (slot == null) {
            log.warning("Requested to create article for unknown slot " +
                        "[pid=" + playerId + ", article=" + article + "].");
            return null;
        }
        return new Article(playerId, article.slot, article.name,
                           getComponentIds(article, zations));
    }

    /**
     * Creates the default clothing article for the specified gender, choosing
     * random colorizations.
     */
    public Article createDefaultClothing (PlayerObject user, boolean forMale)
    {
        int primary = ColorConstraints.pickRandomColor(
            _pository, "clothes_p", user).colorId;
        int secondary = ColorConstraints.pickRandomColor(
            _pository, "clothes_s", user).colorId;
        int tertiary = ColorConstraints.pickRandomColor(
            _pository, "clothes_t", user).colorId;
        return createDefaultClothing(
            user, forMale, composeZations(primary, secondary, tertiary));
    }

    /**
     * Creates the default clothing article for the specified gender, with the
     * specified colorizations (which should have come from {@link
     * #composeZation}.
     */
    public Article createDefaultClothing (
        PlayerObject user, boolean forMale, int zations)
    {
        // look up the starter article
        String prefix = forMale ? "male" : "female";
        ArticleCatalog.Article article = null;
        for (ArticleCatalog.Article art : _artcat.getArticles()) {
            if (art.name.startsWith(prefix) && art.starter) {
                article = art;
            }
        }
        if (article == null) {
            log.warning("Missing starter clothing article " +
                        "[gender=" + prefix + "].");
            return null;
        }
        return createArticle(user.playerId, article, zations);
    }

    /**
     * Returns the colorization classes used by the specified article.
     */
    public String[] getColorizationClasses (ArticleCatalog.Article article)
    {
        // if a specific set of colorizations have not been specified for an
        // article, we generate the list by computing the union of the classes
        // used by each of the individual components in the article; then we
        // cache it because we're cool like that
        if (article.colors == null) {
            HashSet<String> classes = new HashSet<String>();
            for (ArticleCatalog.Component comp : article.components) {
                ComponentClass cclass = _crepo.getComponentClass(comp.cclass);
                if (cclass == null) {
                    log.warning("Missing component classs for article " +
                                "[article=" + article +
                                ", cclass=" + comp.cclass + "].");
                    continue;
                }
                for (int ii = 0; ii < cclass.colors.length; ii++) {
                    classes.add(cclass.colors[ii]);
                }
            }
            article.colors = classes.toArray(new String[classes.size()]);
        }
        return article.colors;
    }

    /**
     * Looks up the appropriate component ids for the specified article,
     * combines them with the supplied colorizations and returns an array
     * suitable for using in an {@link Article} instance.
     */
    protected int[] getComponentIds (
        ArticleCatalog.Article article, int zations)
    {
        int[] componentIds = new int[article.components.size()];
        int idx = 0;
        for (ArticleCatalog.Component comp : article.components) {
            try {
                CharacterComponent ccomp =
                    _crepo.getComponent(comp.cclass, comp.name);
                // the zations are already shifted 16 bits left
                componentIds[idx++] = ccomp.componentId | zations;
            } catch (NoSuchComponentException nsce) {
                log.warning("Article references unknown component " +
                            "[article=" + article.name +
                            ", cclass=" + comp.cclass +
                            ", name=" + comp.name + "].");
            }
        }
        return componentIds;
    }

    protected ComponentRepository _crepo;
    protected ColorPository _pository;
    protected AspectCatalog _aspcat;
    protected ArticleCatalog _artcat;

    /** Used by {@link #decodeAvatar}. */
    protected Colorization[] _globals = new Colorization[2];

    /** Used by {@link #decodeAvatar}. */
    protected int[] _colors = new int[3];
}
