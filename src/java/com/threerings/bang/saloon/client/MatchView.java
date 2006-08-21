//
// $Id$

package com.threerings.bang.saloon.client;

import com.jmex.bui.BButton;
import com.jmex.bui.BContainer;
import com.jmex.bui.BLabel;
import com.jmex.bui.Spacer;
import com.jmex.bui.event.ActionEvent;
import com.jmex.bui.event.ActionListener;
import com.jmex.bui.layout.BorderLayout;
import com.jmex.bui.layout.GroupLayout;
import com.jmex.bui.icon.ImageIcon;
import com.jmex.bui.util.Dimension;

import com.threerings.crowd.chat.data.SystemMessage;
import com.threerings.jme.chat.ChatView;
import com.threerings.util.MessageBundle;

import com.threerings.presents.dobj.AttributeChangeListener;
import com.threerings.presents.dobj.AttributeChangedEvent;
import com.threerings.presents.dobj.DObject;
import com.threerings.presents.dobj.ElementUpdateListener;
import com.threerings.presents.dobj.ElementUpdatedEvent;
import com.threerings.presents.dobj.ObjectAccessException;
import com.threerings.presents.dobj.Subscriber;
import com.threerings.presents.util.SafeSubscriber;

import com.threerings.bang.util.BangContext;

import com.threerings.bang.saloon.data.MatchObject;
import com.threerings.bang.saloon.data.SaloonCodes;
import com.threerings.bang.saloon.data.SaloonObject;

import static com.threerings.bang.Log.log;

/**
 * Displays a pending matched game and handles the process of entering the game
 * when all is ready to roll.
 */
public class MatchView extends BContainer
    implements Subscriber<MatchObject>
{
    public MatchView (BangContext ctx, SaloonController ctrl, int matchOid)
    {
        super(GroupLayout.makeVStretch());
        setStyleClass("match_view");

        _ctx = ctx;
        _ctrl = ctrl;
        _msgs = _ctx.getMessageManager().getBundle(SaloonCodes.SALOON_MSGS);
        _msub = new SafeSubscriber<MatchObject>(matchOid, this);
        _msub.subscribe(_ctx.getDObjectManager());

        // this will contain the players and game info
        BContainer main = new BContainer(GroupLayout.makeHStretch());
        main.add(_left = GroupLayout.makeVBox(GroupLayout.CENTER));
        ((GroupLayout)_left.getLayoutManager()).setGap(0);
        main.add(_info = GroupLayout.makeVBox(GroupLayout.CENTER),
                 GroupLayout.FIXED);
        main.add(_right = GroupLayout.makeVBox(GroupLayout.CENTER));
        ((GroupLayout)_right.getLayoutManager()).setGap(0);
        main.setPreferredSize(new Dimension(395, 203));
        add(main, GroupLayout.FIXED);

        // this will contain our current criterion
        _info.add(_rounds = new BLabel("", "match_label"));
        _info.add(_players = new BLabel("", "match_label"));
        _info.add(_ranked = new BLabel("", "match_label"));
        _info.add(_range = new BLabel("", "match_label"));
        _info.add(_opponents = new BLabel("", "match_label"));
        _info.add(_starting = new BLabel("", "starting_label"));

        // add our leave button
        BContainer row = GroupLayout.makeHBox(GroupLayout.CENTER);
        row.add(_bye = new BButton(_msgs.get("m.leave"), new ActionListener() {
            public void actionPerformed (ActionEvent event) {
                _bye.setEnabled(false);
                _ctrl.leaveMatch(_mobj.getOid());
            }
        }, "leave"));
        add(row, GroupLayout.FIXED);

        // add a label that will overlay the "Back Parlors" text (it also has
        // custom spacing that positions everything properly)
        ImageIcon icon = new ImageIcon(
            _ctx.loadImage("ui/saloon/matched_game_chat.png"));
        add(new BLabel(icon, "match_chat_header"), GroupLayout.FIXED);

        // this will eventually be the chat view
        _chat = new ChatView(_ctx, _ctx.getChatDirector()) {
            /* ChatView() */ {
                _text.setStyleClass("match_chat_text");
            }
            protected boolean handlesType (String localType) {
                return "match_chat".equals(localType);
            }
        };
        ((BorderLayout)_chat.getLayoutManager()).setGaps(2, 3);
        _chat.setEnabled(false);
        icon = new ImageIcon(_ctx.loadImage("ui/chat/bubble_icon.png"));
        _chat.setChatButton(new BButton(icon, ""));
        add(_chat);
    }

    // documentation inherited from interface Subscriber
    public void objectAvailable (MatchObject object)
    {
        _mobj = object;
        _mobj.addListener(_elup);
        _mobj.addListener(_atch);

        // create our player slots
        _slots = new PlayerSlot[_mobj.playerOids.length];
        for (int ii = 0; ii < _slots.length; ii++) {
            if (ii % 2 == 0) {
                _left.add(_slots[ii] = new PlayerSlot(_ctx));
            } else {
                _right.add(_slots[ii] = new PlayerSlot(_ctx));
            }
        }

        updateDisplay();
        updateCriterion();
        updateStarting();

        _ctx.getChatDirector().addAuxiliarySource(_mobj, "match_chat");
        _chat.setSpeakService(_mobj.speakService);
        _chat.setEnabled(true);
        _chat.requestFocus();
        _chat.displayMessage(new SystemMessage(_msgs.get("m.chat_here"),
                                               null, SystemMessage.INFO));
    }

    // documentation inherited from interface Subscriber
    public void requestFailed (int oid, ObjectAccessException cause)
    {
        log.warning("Failed to subscribe to match object " +
                    "[oid=" + oid + ", cause=" + cause + "].");
        _ctrl.leaveMatch(-1);
    }

    @Override // documentation inherited
    protected void wasRemoved ()
    {
        super.wasRemoved();
        if (_mobj != null) {
            _ctx.getChatDirector().removeAuxiliarySource(_mobj);
        }
        _msub.unsubscribe(_ctx.getDObjectManager());
        _chat.clearSpeakService();
    }

    protected void updateDisplay ()
    {
        for (int ii = 0; ii < _mobj.playerOids.length; ii++) {
            _slots[ii].setPlayerOid(_mobj.playerOids[ii]);
        }
    }

    protected void updateCriterion ()
    {
        String value = _mobj.criterion.getPlayerString();
        _players.setText(_msgs.get("m.cr_players", value));
        value = _mobj.criterion.getRoundString();
        _rounds.setText(_msgs.get("m.cr_rounds", value));
        _ranked.setText(_msgs.get(_mobj.criterion.getDesiredRankedness() ?
                                  "m.ranked" : "m.unranked"));
        value = _mobj.criterion.getAIString();
        _opponents.setText(_msgs.get("m.cr_aiopps", value));
        value = "m." + CriterionView.RANGE[_mobj.criterion.range];
        _range.setText(_msgs.get(value));
    }

    protected void updateStarting ()
    {
        _starting.setText(_mobj.starting ? _msgs.get("m.starting") : "");
    }

    protected AttributeChangeListener _atch = new AttributeChangeListener() {
        public void attributeChanged (AttributeChangedEvent event) {
            if (event.getName().equals(MatchObject.STARTING)) {
                updateStarting();
            } else {
                updateCriterion();
            }
        }
    };

    protected ElementUpdateListener _elup = new ElementUpdateListener() {
        public void elementUpdated (ElementUpdatedEvent event) {
            updateDisplay();
        }
    };

    protected BangContext _ctx;
    protected SaloonController _ctrl;
    protected MessageBundle _msgs;
    protected SafeSubscriber<MatchObject> _msub;
    protected MatchObject _mobj;

    protected BLabel _players, _rounds, _ranked, _range, _opponents;
    protected BLabel _starting;
    protected BButton _bye;

    protected BContainer _left, _right, _info;
    protected PlayerSlot[] _slots;

    protected ChatView _chat;
}
