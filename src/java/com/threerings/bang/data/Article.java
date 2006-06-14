//
// $Id$

package com.threerings.bang.data;

import com.jmex.bui.BImage;
import com.jmex.bui.icon.ImageIcon;

import com.samskivert.util.StringUtil;
import com.threerings.util.MessageBundle;

import com.threerings.media.image.Colorization;
import com.threerings.media.image.ImageUtil;

import com.threerings.bang.avatar.data.AvatarCodes;
import com.threerings.bang.avatar.util.ArticleCatalog;
import com.threerings.bang.avatar.util.AvatarLogic;

import com.threerings.bang.data.BangCodes;
import com.threerings.bang.util.BasicContext;

import static com.threerings.bang.Log.log;

/**
 * Represents an article of clothing or an accessory.
 */
public class Article extends Item
{
    /**
     * Returns the path to the icon for an article of the specified type.
     */
    public static String getIconPath (String type)
    {
        int sidx = type.lastIndexOf("/");
        String townId;
        if (sidx == -1) {
            // legacy articles
            townId = BangCodes.FRONTIER_TOWN;
        } else {
            townId = type.substring(0, sidx);
            type = type.substring(sidx+1);
        }
        return "goods/" + townId + "/articles/" + type + ".png";
    }

    /**
     * Returns a translatable name for an article of the specified type.
     */
    public static String getName (String type)
    {
        type = type.substring(type.lastIndexOf("/")+1);
        return MessageBundle.qualify(AvatarCodes.ARTICLE_MSGS, "m." + type);
    }

    /** A blank constructor used during unserialization. */
    public Article ()
    {
    }

    /**
     * Creates a new article item with the specified slot and components.
     */
    public Article (int ownerId, String slot, String name, int[] components)
    {
        super(ownerId);
        _slot = slot;
        _name = name;
        _components = components;
    }

    /**
     * Returns the slot into which this article fits on an avatar.
     */
    public String getSlot ()
    {
        return _slot;
    }

    /**
     * Returns the component ids (and associated colorizations) for the various
     * avatar components that should be "applied" when wearing this article.
     */
    public int[] getComponents ()
    {
        return _components;
    }

    @Override // documentation inherited
    public String getName ()
    {
        return getName(_name);
    }

    @Override // documentation inherited
    public String getTooltip ()
    {
        return MessageBundle.qualify(BangCodes.GOODS_MSGS, "m.article_tip");
    }

    @Override // documentation inherited
    public String getIconPath ()
    {
        return getIconPath(_name);
    }

    @Override // documentation inherited
    public ImageIcon createIcon (BasicContext ctx, String iconPath)
    {
        AvatarLogic al = ctx.getAvatarLogic();
        ArticleCatalog.Article aca = al.getArticleCatalog().getArticle(_name);
        if (aca == null) {
            log.warning("Article no longer exists? " + this);
            return super.createIcon(ctx, iconPath);
        }

        Colorization[] zations = al.decodeColorizations(
            getComponents()[0], al.getColorizationClasses(aca));
        if (zations == null) {
            return super.createIcon(ctx, iconPath);
        }

        BImage image = new BImage(
            ImageUtil.recolorImage(
                ctx.getImageCache().getBufferedImage(iconPath), zations));
        return new ImageIcon(image);
    }

    @Override // documentation inherited
    protected void toString (StringBuilder buf)
    {
        super.toString(buf);
        buf.append(", slot=").append(_slot);
        buf.append(", name=").append(_name);
        buf.append(", components=");
        StringUtil.toString(buf, _components);
    }

    protected String _slot, _name;
    protected int[] _components;
}
