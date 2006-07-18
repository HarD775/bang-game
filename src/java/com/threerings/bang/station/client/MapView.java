//
// $Id$

package com.threerings.bang.station.client;

import com.jmex.bui.BButton;
import com.jmex.bui.BComponent;
import com.jmex.bui.BContainer;
import com.jmex.bui.BLabel;
import com.jmex.bui.layout.AbsoluteLayout;
import com.jmex.bui.util.Rectangle;

import com.threerings.util.MessageBundle;

import com.threerings.presents.dobj.EntryAddedEvent;
import com.threerings.presents.dobj.SetAdapter;

import com.threerings.bang.data.BangCodes;
import com.threerings.bang.data.Item;
import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.data.TrainTicket;
import com.threerings.bang.util.BangContext;

import com.threerings.bang.station.data.StationCodes;

/**
 * Displays the map of the different towns and manages the buttons that will
 * take the player between them.
 */
public class MapView extends BContainer
{
    public MapView (BangContext ctx, StationController ctrl)
    {
        super(new AbsoluteLayout());
        setStyleClass("station_map");
        _ctx = ctx;
        MessageBundle msgs = ctx.getMessageManager().getBundle(
            StationCodes.STATION_MSGS);

        // add buttons or labels for each town
        _tbuts = new BComponent[TBUT_RECTS.length];
        _towns = new PairedButton[_tbuts.length];
        for (int ii = 0; ii < _tbuts.length; ii++) {
            String townId = BangCodes.TOWN_IDS[ii];
            String gocmd = StationController.TAKE_TRAIN + townId;

            _towns[ii] = new PairedButton(ctrl, gocmd, "map_" + townId, null);
            add(_towns[ii], TOWN_RECTS[ii]);
            _towns[ii].setEnabled(false);

            if (townId.equals(ctx.getUserObject().townId)) {
                _tbuts[ii] = new BLabel("", "map_here");
            } else {
                _tbuts[ii] = new PairedButton(
                    ctrl, gocmd, "map_take", _towns[ii]);
                _towns[ii].setPair((PairedButton)_tbuts[ii]);
                // frontier town is always enabled (if we're not in frontier
                // town), the other towns all start disabled and we'll enable
                // them if the player has a ticket
                boolean enabled = (ii == 0);
                _tbuts[ii].setEnabled(enabled);
                _towns[ii].setEnabled(enabled);
            }
            add(_tbuts[ii], TBUT_RECTS[ii]);
        }

        enableTownButtons();
    }

    @Override // documentation inherited
    protected void wasAdded ()
    {
        super.wasAdded();
        _ctx.getUserObject().addListener(_enabler);
    }

    @Override // documentation inherited
    protected void wasRemoved ()
    {
        super.wasRemoved();
        _ctx.getUserObject().removeListener(_enabler);
    }

    protected void enableTownButtons ()
    {
        for (int ii = 1; ii < _tbuts.length; ii++) {
            if (!(_tbuts[ii] instanceof BButton)) {
                continue;
            }
            boolean enabled = _ctx.getUserObject().holdsTicket(
                BangCodes.TOWN_IDS[ii]);
            _tbuts[ii].setEnabled(enabled);
            _towns[ii].setEnabled(enabled);
        }
    }

    protected class PairedButton extends BButton
    {
        public PairedButton (StationController ctrl, String command,
                             String styleClass, PairedButton pair) {
            super("", ctrl, command);
            setStyleClass(styleClass);
            _pair = pair;
        }

        public void setPair (PairedButton pair) {
            _pair = pair;
        }

        public int getState () {
            int pstate = (_pair == null) ? DEFAULT : _pair.getSelfState();
            return (pstate == HOVER) ? pstate : super.getState();
        }

        protected int getSelfState () {
            return super.getState();
        }

        protected PairedButton _pair;
    }

    /** Listens for additions to the player's inventory and reenables our town
     * buttons if they buy a ticket. */
    protected SetAdapter _enabler = new SetAdapter() {
        public void entryAdded (EntryAddedEvent event) {
            if (event.getName().equals(PlayerObject.INVENTORY)) {
                enableTownButtons();
            }
        }
    };

    protected BangContext _ctx;
    protected PairedButton[] _towns;
    protected BComponent[] _tbuts;

    protected static final Rectangle[] TOWN_RECTS = {
        new Rectangle(37, 149, 142, 119),
        new Rectangle(236, 396, 183, 71),
    };

    protected static final Rectangle[] TBUT_RECTS = {
        new Rectangle(75, 132, 88, 19),
        new Rectangle(276, 354, 88, 19),
    };
}
