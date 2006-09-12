//
// $Id$

package com.threerings.bang.store.data;

import com.threerings.util.MessageBundle;

import com.threerings.bang.data.BangCodes;
import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.game.data.card.Card;

/**
 * Represents a three pack of a known card type that is for sale.
 */
public class CardTripletGood extends Good
{
    /**
     * Creates a good representing the specified card.
     */
    public CardTripletGood (String cardType, int scripCost, int coinCost)
    {
        super("card_trip_" + cardType, scripCost, coinCost);
        _cardType = cardType;
    }

    /** A constructor only used during serialization. */
    public CardTripletGood ()
    {
    }

    /**
     * Returns the type of card created by this good.
     */
    public String getCardType ()
    {
        return _cardType;
    }

    /**
     * Sets the quantity of this good.  This is used on a client side for
     * displaying how many of this good is owned by the player.
     */
    public void setQuantity (int quantity)
    {
        _quantity = quantity;
    }

    @Override // documentation inherited
    public String getIconPath ()
    {
        return Card.newCard(_cardType).getIconPath("card_pack");
    }

    @Override // documentation inherited
    public boolean isAvailable (PlayerObject user)
    {
        // anyone can buy cards (for now)
        return true;
    }

    @Override // documentation inherited
    public String getName ()
    {
        return MessageBundle.qualify(BangCodes.CARDS_MSGS, "m." + _cardType);
    }

    @Override // documentation inherited
    public String getTip ()
    {
        String msg = MessageBundle.qualify(
            BangCodes.CARDS_MSGS, "m." + _cardType);
        msg = MessageBundle.compose("m.card_trip_tip", msg);
        msg = MessageBundle.qualify(BangCodes.GOODS_MSGS, msg);
        msg = MessageBundle.compose(
            "m.card_tip_cont", msg, "m." + _cardType + "_tip");
        return MessageBundle.qualify(BangCodes.CARDS_MSGS, msg);
    }

    @Override // documentation inherited
    public String getToolTip ()
    {
        String msg = getTip();
        msg = MessageBundle.compose(
                "m.card_tool_tip", msg, MessageBundle.taint("" + _quantity));
        return MessageBundle.qualify(BangCodes.CARDS_MSGS, msg);
        
    }

    protected String _cardType;

    protected transient int _quantity;
}
