//
// $Id$

package com.threerings.bang.bank.client;

import com.jmex.bui.BButton;
import com.jmex.bui.BContainer;
import com.jmex.bui.BLabel;
import com.jmex.bui.BScrollPane;
import com.jmex.bui.BTextField;
import com.jmex.bui.Spacer;
import com.jmex.bui.event.ActionEvent;
import com.jmex.bui.event.ActionListener;
import com.jmex.bui.event.TextEvent;
import com.jmex.bui.event.TextListener;
import com.jmex.bui.layout.AbsoluteLayout;
import com.jmex.bui.layout.GroupLayout;
import com.jmex.bui.layout.TableLayout;
import com.jmex.bui.util.Point;
import com.jmex.bui.util.Rectangle;

import com.threerings.util.MessageBundle;

import com.threerings.presents.dobj.AttributeChangeListener;
import com.threerings.presents.dobj.AttributeChangedEvent;

import com.threerings.coin.data.CoinExOfferInfo;

import com.threerings.bang.client.BangUI;
import com.threerings.bang.data.BangCodes;
import com.threerings.bang.data.ConsolidatedOffer;
import com.threerings.bang.util.BangContext;

import com.threerings.bang.bank.data.BankCodes;
import com.threerings.bang.bank.data.BankObject;

import static com.threerings.bang.Log.log;

/**
 * Displays an interface for posting a buy or sell offer and for viewing one's
 * outstanding offers.
 */
public class FullTransact extends BContainer
    implements ActionListener, BankCodes
{
    public FullTransact (BangContext ctx, BLabel status, boolean buying)
    {
        super(new AbsoluteLayout());
        _ctx = ctx;
        _status = status;
        _buying = buying;
        _msgs = ctx.getMessageManager().getBundle(BANK_MSGS);

        String msg = buying ? "m.buy" : "m.sell";
        add(new BLabel(_msgs.get(msg + "_offers"), "bank_title"),
            new Point(0, 385));

        // add slots for the top four offers
        BContainer offers = new BContainer(new TableLayout(4, 5, 15));
        _offers = new OfferLabel[BangCodes.COINEX_OFFERS_SHOWN];
        for (int ii = 0; ii < _offers.length; ii++) {
            _offers[ii] = new OfferLabel(offers);
        }
        _offers[0].setNoOffers();
        add(offers, new Rectangle(12, 245, 310, 135));

        add(new BLabel(_msgs.get(msg + "_post_offer"), "bank_post_title"),
            new Point(0, 212));

        BContainer moffer = GroupLayout.makeHBox(GroupLayout.LEFT);
        moffer.add(new BLabel(BangUI.coinIcon));
        moffer.add(_coins = new BTextField(""));
        _coins.addListener(_textlist);
        _coins.setPreferredWidth(30);
        moffer.add(new BLabel(_msgs.get("m.for")));
        moffer.add(new BLabel(BangUI.scripIcon));
        moffer.add(_scrip = new BTextField(""));
        _scrip.setPreferredWidth(40);
        _scrip.addListener(_textlist);
        moffer.add(new BLabel(_msgs.get("m.each")));
        moffer.add(new Spacer(15, 1));
        moffer.add(_post = new BButton(_msgs.get("m.post"), this, "post"));
        _post.setEnabled(false);
        add(moffer, new Point(0, 178));

        add(new BLabel(_msgs.get("m.your_offers"), "bank_title"),
            new Point(0, 139));
        _myoffers = new BContainer(new TableLayout(5, 3, 15));
        add(new BScrollPane(_myoffers), new Rectangle(12, 3, 310, 132));
    }

    public void init (BankObject bankobj)
    {
        _bankobj = bankobj;
        _bankobj.addListener(_updater);
        updateOffers();
    }

    public void notePostedOffers (CoinExOfferInfo[] offers)
    {
        for (int ii = 0; ii < offers.length; ii++) {
            notePostedOffer(offers[ii]);
        }
    }

    // documentation inherited from interface ActionListener
    public void actionPerformed (ActionEvent event)
    {
        if ("post".equals(event.getAction())) {
            // sanity check
            if (_ccount <= 0 || _price <= 0) {
                return;
            }

            // if we don't have sufficient funds, complain and stop
            if (_buying && _ccount * _price > _ctx.getUserObject().scrip) {
                _status.setText(_msgs.get("m.insufficient_scrip"));
                return;
            } else if (!_buying && _ccount > _ctx.getUserObject().coins) {
                _status.setText(_msgs.get("m.insufficient_coins"));
                return;
            }

            BankService.ResultListener cl = new BankService.ResultListener() {
                public void requestProcessed (Object result) {
                    _status.setText(_msgs.get("m.offer_posted"));
                    _coins.setText("");
                    _scrip.setText("");
                    notePostedOffer((CoinExOfferInfo)result);
                }
                public void requestFailed (String reason) {
                    _status.setText(_msgs.xlate(reason));
                }
            };
            _bankobj.service.postOffer(
                _ctx.getClient(), _ccount, _price, _buying, false, cl);

        } else if ("rescind".equals(event.getAction())) {
            BButton rb = (BButton)event.getSource();
            final CoinExOfferInfo offer =
                (CoinExOfferInfo)rb.getProperty("offer");
            BankService.ConfirmListener cl = new BankService.ConfirmListener() {
                public void requestProcessed () {
                    clearPostedOffer(offer);
                    _status.setText(_msgs.get("m.offer_canceled"));
                }
                public void requestFailed (String reason) {
                    _status.setText(_msgs.xlate(reason));
                }
            };
            _bankobj.service.cancelOffer(_ctx.getClient(), offer.offerId, cl);
        }
    }

    protected void updateOffers ()
    {
        ConsolidatedOffer[] offers = _buying ?
            _bankobj.buyOffers : _bankobj.sellOffers;
        for (int ii = 0; ii < _offers.length; ii++) {
            if (offers.length > ii) {
                _offers[ii].setOffer(offers[ii].volume, offers[ii].price);
            } else {
                if (ii == 0) {
                    _offers[ii].setNoOffers();
                } else {
                    _offers[ii].clearOffer();
                }
            }
        }
    }

    protected void notePostedOffer (CoinExOfferInfo offer)
    {
        OfferLabel mine = new OfferLabel(_myoffers);
        mine.setOffer(offer.volume, offer.price);
        BButton rescind = new BButton(_msgs.get("m.rescind"), this, "rescind");
        rescind.setStyleClass("alt_button");
        rescind.setProperty("offer", offer);
        _myoffers.add(rescind);
    }

    protected void clearPostedOffer (CoinExOfferInfo offer)
    {
        for (int ii = 0; ii < _myoffers.getComponentCount(); ii += 5) {
            BButton rb = (BButton)_myoffers.getComponent(ii+4);
            CoinExOfferInfo rbinfo = (CoinExOfferInfo)rb.getProperty("offer");
            if (rbinfo.offerId == offer.offerId) {
                _myoffers.remove(_myoffers.getComponent(ii));
                _myoffers.remove(_myoffers.getComponent(ii));
                _myoffers.remove(_myoffers.getComponent(ii));
                _myoffers.remove(_myoffers.getComponent(ii));
                _myoffers.remove(rb);
                break;
            }
        }
    }

    protected void orderUpdated ()
    {
        _post.setEnabled(false);
        try {
            _ccount = Integer.parseInt(_coins.getText());
            _price = Integer.parseInt(_scrip.getText());
            if (_ccount <= 0 || _price <= 0) {
                return;
            }
            _post.setEnabled(true);
        } catch (Exception e) {
            // leave the button disabled
        }
    }

    protected TextListener _textlist = new TextListener() {
        public void textChanged (TextEvent event) {
            orderUpdated();
        }
    };

    protected class OfferLabel
    {
        public OfferLabel (BContainer table) {
            table.add(_clabel = new BLabel(""));
            table.add(_coins = new BLabel("", "right_label"));
            table.add(_slabel = new BLabel(""));
            table.add(_scrip = new BLabel("", "right_label"));
        }

        public void setOffer (int coins, int scrip)
        {
            _clabel.setIcon(BangUI.coinIcon);
            _coins.setText(_msgs.get("m.offer_coins", "" + coins));
            _slabel.setIcon(BangUI.scripIcon);
            _scrip.setText(_msgs.get("m.offer_scrip", "" + scrip));
        }

        public void setNoOffers ()
        {
            clearOffer();
            _coins.setText(_msgs.get("m.no_offers_tbl"));
        }

        public void clearOffer ()
        {
            _clabel.setIcon(null);
            _coins.setText("");
            _slabel.setIcon(null);
            _scrip.setText("");
        }

        protected BLabel _clabel, _coins;
        protected BLabel _slabel, _scrip;
    }

    protected AttributeChangeListener _updater = new AttributeChangeListener() {
        public void attributeChanged (AttributeChangedEvent event) {
            String name = event.getName();
            if (_buying && name.equals(BankObject.BUY_OFFERS) ||
                !_buying && name.equals(BankObject.SELL_OFFERS)) {
                updateOffers();
            }
        }
    };

    protected BangContext _ctx;
    protected MessageBundle _msgs;
    protected BankObject _bankobj;
    protected boolean _buying;

    protected BLabel _status;
    protected BTextField _coins, _scrip;
    protected int _ccount, _price;
    protected BButton _post;

    protected BContainer _myoffers;
    protected OfferLabel[] _offers;
}
