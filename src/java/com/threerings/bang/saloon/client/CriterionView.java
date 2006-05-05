//
// $Id$

package com.threerings.bang.saloon.client;

import com.jmex.bui.BButton;
import com.jmex.bui.BCheckBox;
import com.jmex.bui.BComboBox;
import com.jmex.bui.BContainer;
import com.jmex.bui.BLabel;
import com.jmex.bui.event.ActionEvent;
import com.jmex.bui.event.ActionListener;
import com.jmex.bui.layout.BorderLayout;
import com.jmex.bui.layout.GroupLayout;
import com.jmex.bui.layout.TableLayout;

import com.threerings.util.MessageBundle;

import com.threerings.bang.game.data.GameCodes;
import com.threerings.bang.util.BangContext;

import com.threerings.bang.saloon.data.Criterion;
import com.threerings.bang.saloon.data.SaloonCodes;

/**
 * Displays the set of configurable game criterion.
 */
public class CriterionView extends BContainer
{
    public CriterionView (BangContext ctx, SaloonController ctrl)
    {
        super(new BorderLayout(5, 5));

        _ctx = ctx;
        _ctrl = ctrl;
        MessageBundle msgs = _ctx.getMessageManager().getBundle(
            SaloonCodes.SALOON_MSGS);

        TableLayout tlay = new TableLayout(2, 5, 25);
        tlay.setHorizontalAlignment(TableLayout.CENTER);
        tlay.setVerticalAlignment(TableLayout.CENTER);
        tlay.setEqualRows(true);
        BContainer table = new BContainer(tlay);
        add(table, BorderLayout.CENTER);

        table.add(new BLabel(msgs.get("m.rounds"), "match_label"));
        BContainer row = new BContainer(GroupLayout.makeHStretch());
        for (int ii = 0; ii < _rounds.length; ii++) {
            row.add(_rounds[ii] = new BCheckBox("" + (ii+1)));
            _rounds[ii].setSelected(ii == 0);
        }
        table.add(row);

        table.add(new BLabel(msgs.get("m.players"), "match_label"));
        row = new BContainer(GroupLayout.makeHStretch());
        for (int ii = 0; ii < _players.length; ii++) {
            row.add(_players[ii] = new BCheckBox("" + (ii+2)));
            _players[ii].setSelected(true);
        }
        table.add(row);

        table.add(new BLabel(msgs.get("m.rankedness"), "match_label"));
        table.add(_ranked = new BComboBox(xlate(msgs, RANKED)));
        _ranked.selectItem(0);

        table.add(new BLabel(msgs.get("m.range"), "match_label"));
        table.add(_range = new BComboBox(xlate(msgs, RANGE)));
        _range.selectItem(0);

        table.add(new BLabel(msgs.get("m.opponents"), "match_label"));
        row = new BContainer(GroupLayout.makeHStretch());
        for (int ii = 0; ii < _aiopps.length; ii++) {
            row.add(_aiopps[ii] = new BCheckBox("" + ii));
            _aiopps[ii].setSelected(ii < 2);
        }
        table.add(row);

        row = GroupLayout.makeHBox(GroupLayout.CENTER);
        row.add(_go = new BButton(msgs.get("m.go"), _golist, "match"));
        _go.setStyleClass("big_button");
        add(row, BorderLayout.SOUTH);

        // TODO: preserve our settings in persistent preferences
    }

    public void reenable ()
    {
        _go.setEnabled(true);
    }

    @Override // documentation inherited
    public void wasAdded ()
    {
        super.wasAdded();
        reenable();
    }

    protected String[] xlate (MessageBundle msgs, String[] umsgs)
    {
        String[] tmsgs = new String[umsgs.length];
        for (int ii = 0; ii < tmsgs.length; ii++) {
            tmsgs[ii] = msgs.get("m." + umsgs[ii]);
        }
        return tmsgs;
    }

    protected ActionListener _golist = new ActionListener() {
        public void actionPerformed (ActionEvent event) {
            // create a criterion instance from our UI configuration
            Criterion criterion = new Criterion();
            criterion.rounds = Criterion.compose(
                _rounds[0].isSelected(), _rounds[1].isSelected(),
                _rounds[2].isSelected());
            criterion.players = Criterion.compose(
                _players[0].isSelected(), _players[1].isSelected(),
                _players[2].isSelected());
            int rsel = _ranked.getSelectedIndex();
            criterion.ranked = Criterion.compose(
                (rsel == 0 || rsel == 2), (rsel == 1 || rsel == 2), false);
            criterion.range = _range.getSelectedIndex();
            criterion.allowAIs = Criterion.compose(
                _aiopps[0].isSelected(), _aiopps[1].isSelected(),
                _aiopps[2].isSelected());

            // pass the buck onto the controller to do the rest
            _go.setEnabled(false);
            _ctrl.findMatch(criterion);
        }
    };

    protected BangContext _ctx;
    protected SaloonController _ctrl;

    protected BCheckBox[] _rounds = new BCheckBox[GameCodes.MAX_ROUNDS];
    protected BCheckBox[] _players = new BCheckBox[GameCodes.MAX_PLAYERS-1];
    protected BCheckBox[] _aiopps = new BCheckBox[GameCodes.MAX_PLAYERS-1];
    protected BComboBox _ranked, _range;
    protected BButton _go;

    protected static final String[] RANKED = { "ranked", "unranked", "both" };
    protected static final String[] RANGE = { "tight", "loose", "open" };
    protected static final String[] ALLOW_AIS = { "allow", "disallow", "both" };
}
