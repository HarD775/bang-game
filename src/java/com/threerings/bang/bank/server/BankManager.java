//
// $Id$

package com.threerings.bang.bank.server;

import com.threerings.presents.data.ClientObject;
import com.threerings.presents.server.InvocationException;
import com.threerings.presents.util.ResultAdapter;

import com.threerings.crowd.server.PlaceManager;

import com.threerings.coin.data.CoinExOfferInfo;
import com.threerings.coin.server.CoinExOffer;

import com.threerings.bang.data.ConsolidatedOffer;
import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.server.BangCoinExchangeManager;
import com.threerings.bang.server.BangServer;

import com.threerings.bang.bank.client.BankService;
import com.threerings.bang.bank.data.BankCodes;
import com.threerings.bang.bank.data.BankMarshaller;
import com.threerings.bang.bank.data.BankObject;

import static com.threerings.bang.Log.log;

/**
 * Handles the server-side operation of the Bank.
 */
public class BankManager extends PlaceManager
    implements BankProvider, BankCodes, BangCoinExchangeManager.OfferPublisher
{
    // documentation inherited from interface BankProvider
    public void getMyOffers (ClientObject caller, BankService.OfferListener ol)
        throws InvocationException
    {
        PlayerObject user = (PlayerObject)caller;
        CoinExOfferInfo[][] offers = BangServer.coinexmgr.getPlayerOffers(user);
        ol.gotOffers(offers[0], offers[1]);
    }

    // documentation inherited from interface BankProvider
    public void postOffer (ClientObject caller, int coins, int pricePerCoin,
                           boolean buying, boolean immediate,
                           BankService.ResultListener listener)
        throws InvocationException
    {
        PlayerObject player = (PlayerObject)caller;
        CoinExOffer offer = new CoinExOffer();
        offer.accountName = player.username.toString();
        offer.gameName = offer.accountName;
        offer.buy = buying;
        offer.volume = (short)Math.min(coins, Short.MAX_VALUE);
        offer.price = (short)Math.min(pricePerCoin, Short.MAX_VALUE);
        BangServer.coinexmgr.postOffer(
            caller, offer, immediate, new ResultAdapter(listener));
    }

    // documentation inherited from interface BankProvider
    public void cancelOffer (ClientObject caller, int offerId,
                             BankService.ConfirmListener cl)
        throws InvocationException
    {
        PlayerObject player = (PlayerObject)caller;
        if (BangServer.coinexmgr.cancelOffer(
                player.username.toString(), offerId)) {
            cl.requestProcessed();
        } else {
            cl.requestFailed(NO_SUCH_OFFER);
        }
    }

    // documentation inherited from interface OfferPublisher
    public void updateOffers (ConsolidatedOffer[] buys,
                              ConsolidatedOffer[] sells, int lastPrice)
    {
        if (buys != null) {
            _bankobj.setBuyOffers(buys);
        }
        if (sells != null) {
            _bankobj.setSellOffers(sells);
        }
        if (lastPrice != -1) {
            _bankobj.setLastTrade(lastPrice);
        }
    }

    @Override // documentation inherited
    protected Class getPlaceObjectClass ()
    {
        return BankObject.class;
    }

    @Override // documentation inherited
    protected long idleUnloadPeriod ()
    {
        // we don't want to unload
        return 0L;
    }

    @Override // documentation inherited
    protected void didStartup ()
    {
        super.didStartup();

        // register our invocation service
        _bankobj = (BankObject)_plobj;
        _bankobj.setService((BankMarshaller)BangServer.invmgr.registerDispatcher(
                                new BankDispatcher(this), false));

        // register with the coin exchange manager
        BangServer.coinexmgr.registerPublisher(this);
    }

    protected BankObject _bankobj;
}
