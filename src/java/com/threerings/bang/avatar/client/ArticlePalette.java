//
// $Id$

package com.threerings.bang.avatar.client;

import java.util.Iterator;

import com.threerings.bang.client.ItemIcon;
import com.threerings.bang.client.bui.IconPalette;
import com.threerings.bang.data.Article;
import com.threerings.bang.data.Item;
import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.util.BangContext;

import com.threerings.bang.avatar.data.Look;
import com.threerings.bang.avatar.util.AvatarLogic;

/**
 * Displays a selectable list of avatar articles.
 */
public class ArticlePalette extends IconPalette
{
    public ArticlePalette (BangContext ctx, Inspector inspector,
                           PickLookView view)
    {
        super(inspector, 4, 3, ItemIcon.ICON_SIZE, 1);
        _ctx = ctx;
        _view = view;
    }

    /**
     * Configures this palette to display articles that fit into the specified
     * slot. The player's inventory will be rescanned and the palette
     * repopulated.
     */
    public void setSlot (String slot)
    {
        clear();

        // disallow a non-selection if we're not an optional slot
        int slidx = AvatarLogic.getSlotIndex(slot);
        setAllowsEmptySelection(AvatarLogic.SLOTS[slidx].optional);

        // look up the selected look and find out what the player is wearing in
        // this slot
        PlayerObject player = _ctx.getUserObject();
        int articleId = -1;
        Look look = _view.getSelection();
        if (look != null && look.articles.length > slidx) {
            articleId = look.articles[slidx];
        }

        for (Iterator iter = player.inventory.iterator(); iter.hasNext(); ) {
            Item item = (Item)iter.next();
            if (!(item instanceof Article) ||
                !((Article)item).getSlot().equals(slot)) {
                continue;
            }
            ItemIcon icon = new ItemIcon(_ctx, item);
            addIcon(icon);

            // if this is the currently worn article, select it
            if (item.getItemId() == articleId) {
                icon.setSelected(true);
            }
        }
    }

    protected BangContext _ctx;
    protected PickLookView _view;
}
