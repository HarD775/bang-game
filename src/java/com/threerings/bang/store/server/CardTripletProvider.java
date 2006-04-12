//
// $Id$

package com.threerings.bang.store.server;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import com.samskivert.io.PersistenceException;

import com.threerings.presents.server.InvocationException;

import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.data.CardItem;
import com.threerings.bang.game.data.card.Card;
import com.threerings.bang.server.BangServer;

import com.threerings.bang.store.data.CardPackGood;
import com.threerings.bang.store.data.CardTripletGood;
import com.threerings.bang.store.data.Good;

/**
 * Creates and delivers cards to a player when they buy a pack of cards in
 * the General Store.
 */
public class CardTripletProvider extends Provider
{
    public CardTripletProvider (PlayerObject user, Good good)
        throws InvocationException
    {
        super(user, good, null);

        _card = ((CardTripletGood)good).getCardType();

        // determine whether or not the user has this type of card
        for (Iterator iter = user.inventory.iterator(); iter.hasNext(); ) {
            Object item = iter.next();
            if (item instanceof CardItem &&
                ((CardItem)item).getType().equals(_card)) {
                _original = (CardItem)item;
                break;
            }
        }

        // create a new inventory item if needed or clone their current
        if (_original == null) {
            _item = new CardItem(user.playerId, _card);
        } else {
            _item = (CardItem)_original.clone();
        }
        for (int ii = 0; ii < 3; ii++) {
            _item.addCard();
        }
    }

    @Override // documentation inherited
    protected void persistentAction ()
        throws PersistenceException
    {
        // insert or update the card item
        if (_item.getItemId() == 0) {
            BangServer.itemrepo.insertItem(_item);
        } else {
            BangServer.itemrepo.updateItem(_item);
        }
    }

    @Override // documentation inherited
    protected void rollbackPersistentAction ()
        throws PersistenceException
    {
        // restore the original item
        if (_original != null) {
            BangServer.itemrepo.updateItem(_original);
        }

        // delete the original item if it was stored
        if (_item.getItemId() != 0) {
            BangServer.itemrepo.deleteItem(_item, "cardtrip_provider_rollback");
        }
    }

    @Override // documentation inherited
    protected void actionCompleted ()
    {
        // broadcast the update to the player's inventory
        if (_user.inventory.contains(_item)) {
            _user.updateInventory(_item);
        } else {
            _user.addToInventory(_item);
        }

        // send a custom message to their user object detailing the cards they
        // just received so that they can be nicely displayed
        _user.postMessage(CardPackGood.PURCHASED_CARDS,
                          new Object[] { new String[] { _card }});

        super.actionCompleted();
    }

    /** The card type that will be delivered. */
    protected String _card;

    /** The inventory item to be added or updated as a result of this
     * purchase. */
    protected CardItem _item;

    /** The inventory item that was be updated as a result of this purchase in
     * its pre-updated form. */
    protected CardItem _original;
}
