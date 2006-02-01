//
// $Id$

package com.threerings.bang.game.client;

import com.jmex.bui.BButton;
import com.jmex.bui.BContainer;
import com.jmex.bui.BDecoratedWindow;
import com.jmex.bui.BLabel;
import com.jmex.bui.event.ActionEvent;
import com.jmex.bui.event.ActionListener;
import com.jmex.bui.layout.BorderLayout;
import com.jmex.bui.layout.GroupLayout;
import com.jmex.bui.util.Dimension;
import com.samskivert.util.ArrayIntSet;

import com.threerings.util.MessageBundle;

import com.threerings.bang.game.data.BangConfig;
import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.GameCodes;

import com.threerings.bang.ranch.client.UnitIcon;
import com.threerings.bang.ranch.client.UnitPalette;

import com.threerings.bang.data.CardItem;
import com.threerings.bang.util.BangContext;

/**
 * Displays an interface for selecting a big shot and a starting hand of
 * cards from a player's inventory.
 */
public class SelectionView extends BDecoratedWindow
    implements ActionListener
{
    /**
     * Creates a nice header to display on pre-game dialogs.
     */
    public static BContainer createRoundHeader (
        BangContext ctx, BangConfig config, BangObject bangobj)
    {
        BContainer header = new BContainer(new BorderLayout(10, 0));
        header.setStyleClass("dialog_title");
        MessageBundle msgs =
            ctx.getMessageManager().getBundle(GameCodes.GAME_MSGS);
        String title = bangobj.boardName + ": " +
            msgs.get("m.scenario_" + bangobj.scenarioId);
        header.add(new BLabel(title), BorderLayout.WEST);
        String rmsg = msgs.get("m.round", "" + (bangobj.roundId + 1),
                               "" + config.getRounds());
        header.add(new BLabel(rmsg), BorderLayout.EAST);
        return header;
    }

    public SelectionView (BangContext ctx, BangConfig config,
                          BangObject bangobj, int pidx)
    {
        super(ctx.getStyleSheet(), null);

        _ctx = ctx;
        _msgs = _ctx.getMessageManager().getBundle(GameCodes.GAME_MSGS);
        _bangobj = bangobj;
        _pidx = pidx;

        setLayoutManager(GroupLayout.makeVStretch());
        add(createRoundHeader(ctx, config, bangobj), GroupLayout.FIXED);
        add(new BLabel(_msgs.get("m.select_bigshot")), GroupLayout.FIXED);

        // create the big shot selection display
        _units = new UnitPalette(ctx, null, 4, 1);
        _units.setUser(_ctx.getUserObject());
        add(_units);

        // create the card selection display
        add(new BLabel(_msgs.get("m.select_cards")), GroupLayout.FIXED);
        add(_cards = new CardPalette(ctx, bangobj));

        BContainer footer = new BContainer(new BorderLayout(10, 0));
        _ready = new BButton(_msgs.get("m.ready"));
        _ready.addListener(this);
        footer.add(_ready, BorderLayout.EAST);
        add(footer, GroupLayout.FIXED);
    }

    // documentation inherited from interface ActionListener
    public void actionPerformed (ActionEvent e)
    {
        UnitIcon icon = _units.getSelectedUnit();
        if (icon == null) {
            return;
        }

        // don't allow double clickage
        _ready.setEnabled(false);

        // determine which cards are selected
        ArrayIntSet cardIds = new ArrayIntSet();
        for (int ii = 0; ii < GameCodes.MAX_CARDS; ii++) {
            CardItem item = _cards.getSelectedCard(ii);
            if (item != null) {
                cardIds.add(item.getItemId());
            }
        }

        int bigShotId = icon.getItemId();
        _bangobj.service.selectStarters(
            _ctx.getClient(), bigShotId, cardIds.toIntArray());
    }

    @Override // documentation inherited
    public Dimension getPreferredSize (int whint, int hhint)
    {
        // make sure we fit comfortably in the available height
        int maxheight = _ctx.getDisplay().getHeight() - 250;
        hhint = (hhint == -1) ? maxheight : hhint;
        Dimension d = super.getPreferredSize(whint, hhint);
        d.height = Math.min(d.height, maxheight);
        return d;
    }

    @Override // documentation inherited
    protected void wasRemoved ()
    {
        super.wasRemoved();
        _units.shutdown();
    }

    protected BangContext _ctx;
    protected MessageBundle _msgs;
    protected BangObject _bangobj;
    protected int _pidx;

    protected UnitPalette _units;
    protected CardPalette _cards;
    protected BButton _ready;
}
