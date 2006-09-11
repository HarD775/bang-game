//
// $Id$

package com.threerings.bang.chat.client;

import java.util.HashMap;

import com.jmex.bui.BButton;
import com.jmex.bui.BContainer;
import com.jmex.bui.BTextField;
import com.jmex.bui.event.ActionEvent;
import com.jmex.bui.event.ActionListener;
import com.jmex.bui.icon.BIcon;
import com.jmex.bui.icon.ImageIcon;
import com.jmex.bui.layout.GroupLayout;
import com.jmex.bui.util.Dimension;

import com.samskivert.util.ResultListener;
import com.threerings.util.Name;

import com.threerings.crowd.chat.client.ChatDisplay;
import com.threerings.crowd.chat.data.ChatCodes;
import com.threerings.crowd.chat.data.ChatMessage;
import com.threerings.crowd.chat.data.SystemMessage;

import com.threerings.bang.avatar.data.Look;
import com.threerings.bang.chat.data.PlayerMessage;

import com.threerings.bang.client.bui.EnablingValidator;
import com.threerings.bang.client.bui.TabbedPane;
import com.threerings.bang.data.BangCodes;
import com.threerings.bang.data.Handle;
import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.util.BangContext;

import static com.threerings.bang.Log.log;

/**
 * The common superclass for displaying user-to-user tells in a tabbed
 * view that shows the users' avatars next to the text.
 */
public abstract class TabbedChatView extends BContainer
    implements ActionListener, ChatDisplay
{
    public TabbedChatView (BangContext ctx, Dimension tabSize)
    {
        super();
        _ctx = ctx;
        _ctx.getChatDirector().addChatDisplay(this);
        _tabSize = tabSize;

        GroupLayout layout = GroupLayout.makeVert(
            GroupLayout.NONE, GroupLayout.TOP, GroupLayout.STRETCH); 
        layout.setGap(0);
        setLayoutManager(layout);

        add(_pane = new TabbedPane(true));
        BContainer tcont = new BContainer(GroupLayout.makeHoriz(
            GroupLayout.STRETCH, GroupLayout.CENTER, GroupLayout.NONE));
        tcont.add(_text = new BTextField());
        _text.addListener(this);
        tcont.add(_send = new BButton(new ImageIcon(
            _ctx.loadImage("ui/chat/bubble_icon.png")), this, "send"),
            GroupLayout.FIXED);
        add(tcont);

        // disable send until some text is entered
        new EnablingValidator(_text, _send);

        _alert = new ImageIcon(_ctx.loadImage("ui/chat/alert_icon.png"));
    }

    @Override // documentation inherited
    public void wasRemoved ()
    {
        super.wasRemoved();
        clear();
    }

    // from interface ChatDisplay
    public void clear ()
    {
        _pane.removeAllTabs();
        _users.clear();
        _text.setText("");
    }

    // from interface ChatDisplay
    public void displayMessage (ChatMessage msg)
    {
        // we handle player-to-player chat
        if (msg instanceof PlayerMessage &&
            ChatCodes.USER_CHAT_TYPE.equals(msg.localtype)) {
            PlayerMessage pmsg = (PlayerMessage)msg;
            Handle handle = (Handle)pmsg.speaker;
            UserTab tab = openUserTab(handle, pmsg.avatar, false);
            if (tab == null) {
                return;
            }
            if (tab != _pane.getSelectedTab()) {
                _pane.getTabButton(tab).setIcon(_alert);
            }
            tab.appendReceived(pmsg);
        }

// TODO: right now this shows up in the main UI which is weird but we need to
// differentiate between feedback as a result of our tell and general chat
// feedback messages to do the right thing...
//         } else if (msg instanceof SystemMessage &&
//                    ((SystemMessage)msg).attentionLevel ==
//                    SystemMessage.FEEDBACK) {
//             // we also have to handle feedback messages because that's how tell
//             // failures are reported
//             ((PardnerTab)_tabs.getSelectedTab()).appendSystem(msg, "feedback");
        
    }

    // from interface ActionListener
    public void actionPerformed (ActionEvent ae)
    {
        Object src = ae.getSource();
        if (src == _send || (src == _text && _send.isEnabled())) {
            String msg = _text.getText().trim();
            _text.setText("");
            if (msg.startsWith("/")) {
                String error = _ctx.getChatDirector().requestChat(
                    null, msg, true);
                if (!ChatCodes.SUCCESS.equals(error)) {
                    SystemMessage sysmsg = new SystemMessage(
                        _ctx.xlate(BangCodes.CHAT_MSGS, error),
                        null, SystemMessage.FEEDBACK);
                    ((UserTab)_pane.getSelectedTab()).appendSystem(sysmsg);
                }
    
            } else {
                ((UserTab)_pane.getSelectedTab()).requestTell(msg);
            }
        }
    }

    /**
     * Ensure that a given user tab exists, possibly creating it.
     */
    public UserTab openUserTab (Handle handle, int[] avatar, boolean focus)
    {
        UserTab tab = _users.get(handle);
        if (tab == null) {
            tab = new UserTab(_ctx, handle, avatar);
            _pane.addTab(handle.toString(), tab);
            _users.put(handle, tab);
        }
        // this has to be called when the tab is already added
        if (!isAdded()) {
            if (!displayTabs(focus)) {
                return null;
            }
        }
        return tab;
    }

    /**
     * Displays the chat interface when needed, if it's not already shown.
     */
    protected abstract boolean displayTabs(boolean grabFocus);
    
    /** Lets subclasses react to the last tab closing */
    protected abstract void lastTabClosed ();

    /**
     * Handles the chat display for single user.
     */
    protected class UserTab extends ComicChatView
    {
        public UserTab (BangContext ctx, Handle user, int[] avatar)
        {
            super(ctx, _tabSize, false);
            _user = user;
            _avatar = avatar;
        }

        /**
         * Attempts to send a tell to this tab's user.
         */
        public void requestTell (final String msg)
        {
            _ctx.getChatDirector().requestTell(
                _user, msg, new ResultListener<Name>() {
                    public void requestCompleted (Name result) {
                        appendSent(msg);
                    }
                    public void requestFailed (Exception cause) {
                        // will be reported in a feedback message
                    }
                });
        }

        /**
         * Mutes this tab's user.
         */
        public void mute ()
        {
            close();
            _ctx.getMuteDirector().setMuted(_user, true);
        }

        /**
         * Closes this tab and hides the pop-up if it was the last tab open.
         */
        public void close ()
        {
            _pane.removeTab(this);
            _users.remove(_user);
            lastTabClosed();
        }

        @Override // documentation inherited
        protected void wasAdded ()
        {
            super.wasAdded();

            // clear the alert icon, if present
            BButton btn = _pane.getTabButton(this);
            if (btn != null) {
                btn.setIcon(null);
            }
        }

        @Override // documentation inherited
        protected int[] getSpeakerAvatar (Handle speaker)
        {
            if (speaker.equals(_ctx.getUserObject().handle)) {
                PlayerObject player = _ctx.getUserObject();
                Look look = player.getLook(Look.Pose.DEFAULT);
                return (look == null) ? null : look.getAvatar(player);

            } else if (speaker.equals(_user)) {
                return _avatar;

            } else {
                // this should never happen
                log.warning("Unknown speaker [speaker=" + speaker +
                            ", user=" + _user + "].");
                return null;
            }
        }

        protected Handle _user;
        protected int[] _avatar;
    }

    protected BangContext _ctx;
    protected BTextField _text;
    protected BButton _send;
    protected TabbedPane _pane;
    protected Dimension _tabSize;

    protected HashMap<Handle,UserTab> _users =
        new HashMap<Handle,UserTab>();

    protected BIcon _alert;
}
