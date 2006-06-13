//
// $Id$

package com.threerings.bang.avatar.client;

import java.util.HashSet;

import com.jmex.bui.BButton;
import com.jmex.bui.BComboBox;
import com.jmex.bui.BContainer;
import com.jmex.bui.BDecoratedWindow;
import com.jmex.bui.BLabel;
import com.jmex.bui.BTextArea;
import com.jmex.bui.BTextField;
import com.jmex.bui.Spacer;
import com.jmex.bui.event.ActionEvent;
import com.jmex.bui.event.ActionListener;
import com.jmex.bui.event.TextEvent;
import com.jmex.bui.event.TextListener;
import com.jmex.bui.icon.ImageIcon;
import com.jmex.bui.layout.GroupLayout;
import com.jmex.bui.text.LengthLimitedDocument;

import com.samskivert.util.RandomUtil;
import com.threerings.util.MessageBundle;

import com.threerings.bang.client.BangClient;
import com.threerings.bang.client.bui.StatusLabel;
import com.threerings.bang.data.Handle;
import com.threerings.bang.util.BangContext;
import com.threerings.bang.util.NameFactory;

import com.threerings.bang.avatar.data.AvatarCodes;

/**
 * Displays an interface via which the player can create their avatar: name,
 * sex and default look.
 */
public class CreateAvatarView extends BDecoratedWindow
    implements ActionListener, BangClient.NonClearablePopup
{
    public CreateAvatarView (BangContext ctx)
    {
        super(ctx.getStyleSheet(), null);
        setStyleClass("dialog_window");
        setLayoutManager(GroupLayout.makeVert(GroupLayout.CENTER));
        ((GroupLayout)getLayoutManager()).setGap(15);

        _ctx = ctx;
        _msgs = _ctx.getMessageManager().getBundle(AvatarCodes.AVATAR_MSGS);

        add(new BLabel(_msgs.get("m.create_title"), "window_title"));
        add(new BLabel(_msgs.get("m.create_intro"), "dialog_text"));

        GroupLayout glay = GroupLayout.makeVert(
            GroupLayout.NONE, GroupLayout.TOP, GroupLayout.STRETCH);
        glay.setGap(15);
        BContainer inner = new BContainer(glay);
        inner.setStyleClass("fa_inner_box");
        add(inner);
        _status = new StatusLabel(ctx);
        _status.setStyleClass("dialog_text");
        add(_status);
        add(_done = new BButton(_msgs.get("m.done"), this, "done"));
        _done.setEnabled(false);

        // this all goes in the inner box
        BContainer row = new BContainer(GroupLayout.makeHStretch());
        BContainer col = GroupLayout.makeHBox(GroupLayout.LEFT);
        col.add(new Spacer(20, 1));
        col.add(new BLabel(_msgs.get("m.persuasion"), "dialog_label"));
        String[] gensel = new String[] {
            _msgs.get("m.male"), _msgs.get("m.female") };
        col.add(_gender = new BComboBox(gensel));
        _gender.addListener(_sexer);
        row.add(col);

        col = GroupLayout.makeHBox(GroupLayout.RIGHT);
        col.add(new BLabel(_msgs.get("m.handle"), "dialog_label"));
        col.add(_handle = new BTextField(""));
        _handle.setPreferredWidth(150);
        _handle.setDocument(new HandleDocument());
        _handle.addListener(new TextListener() {
            public void textChanged (TextEvent event) {
                handleUpdated(_handle.getText().trim());
            }
        });

        ImageIcon dicon = new ImageIcon(ctx.loadImage("ui/icons/dice.png"));
        BButton btn;
        col.add(btn = new BButton(dicon, this, "random"), GroupLayout.FIXED);
        btn.setStyleClass("arrow_button");
        row.add(col);
        inner.add(row);
        inner.add(_look = new FirstLookView(ctx, _status));
    }

    // documentation inherited from interface ActionListener
    public void actionPerformed (ActionEvent event)
    {
        String cmd = event.getAction();
        if (cmd.equals("random")) {
            pickRandomHandle();
            maybeClearStatus();
        } else if (cmd.equals("done")) {
            createAvatar();
        }
    }

    @Override // documentation inherited
    protected void wasAdded ()
    {
        super.wasAdded();
        // start with a random gender which will trigger name list and avatar
        // display configuration
        _gender.selectItem(RandomUtil.getInt(2));
    }

    protected void pickRandomHandle ()
    {
        boolean isMale = (_gender.getSelectedIndex() == 0);
        HashSet<String> first, second;
        if (RandomUtil.getInt(100) >= 50) {
            first = NameFactory.getCreator().getHandlePrefixes(isMale);
            second = NameFactory.getCreator().getHandleRoots(isMale);
        } else {
            first = NameFactory.getCreator().getHandleRoots(isMale);
            second = NameFactory.getCreator().getHandleSuffixes(isMale);
        }
        String fname = (String)
            RandomUtil.pickRandom(first.iterator(), first.size());
        String sname = (String)
            RandomUtil.pickRandom(second.iterator(), second.size());
        _handle.setText(fname + " " + sname);
    }

    protected void handleUpdated (String text)
    {
        boolean valid = NameFactory.getValidator().isValidHandle(
            new Handle(text));
        _done.setEnabled(valid);
        String status = "m.create_defstatus";
        if (!valid && text.length() >=
            NameFactory.getValidator().getMinHandleLength()) {
            status = "m.invalid_handle";
        }
        _status.setStatus(_msgs.get(status), false);
    }

    protected void createAvatar ()
    {
        AvatarService asvc = (AvatarService)
            _ctx.getClient().requireService(AvatarService.class);
        AvatarService.ConfirmListener cl = new AvatarService.ConfirmListener() {
            public void requestProcessed () {
                // move to the next phase of the intro
                _ctx.getBangClient().clearPopup(CreateAvatarView.this, true);
                _ctx.getBangClient().checkShowIntro();
            }
            public void requestFailed (String reason) {
                _status.setStatus(_msgs.xlate(reason), true);
                _failed = true;
                _handle.setEnabled(true);
                _done.setEnabled(true);
            }
        };
        _handle.setEnabled(false);
        _done.setEnabled(false);

        Handle handle = new Handle(_handle.getText());
        boolean isMale = (_gender.getSelectedIndex() == 0);
        asvc.createAvatar(
            _ctx.getClient(), handle, isMale, _look.getLookConfig(),
            _look.getDefaultArticleColorizations(), cl);
    }

    protected void maybeClearStatus ()
    {
        if (_failed) {
            _status.setStatus(_msgs.get("m.create_defstatus"), false);
        }
    }

    protected static class HandleDocument extends LengthLimitedDocument
    {
        public HandleDocument () {
            super(NameFactory.getValidator().getMaxHandleLength());
        }

        public boolean replace (int offset, int length, String text) {
            StringBuffer buf = new StringBuffer();
            for (int ii = 0, ll = text.length(); ii < ll; ii++) {
                char c = text.charAt(ii);
                // filter out non-letters and whitespace
                if (!Character.isLetter(c) && !Character.isWhitespace(c)) {
                    continue;
                }
                // if they're just starting to type from a blank slate,
                // capitalize the first letter they type; doing it at any other
                // time has the potential to result in unintended behavior
                if (getLength() == 0 && ii == 0 && Character.isLowerCase(c)) {
                    buf.append(Character.toUpperCase(c));
                } else {
                    buf.append(c);
                }
            }
            text = buf.toString();

            // if we've reduced this to a NOOP, we have to indicate that we've
            // rejected the edit
            if (length == 0 && text.length() == 0) {
                return false;
            }

            return super.replace(offset, length, text);
        }

        protected boolean validateEdit (String oldText, String newText) {
            // disallow consecutive spaces
            return super.validateEdit(oldText, newText) &&
                !newText.matches(".*\\s\\s.*");
        }
    };

    protected ActionListener _sexer = new ActionListener() {
        public void actionPerformed (ActionEvent event) {
            _look.setGender(_gender.getSelectedIndex() == 0);
            pickRandomHandle();
            maybeClearStatus();
        }
    };

    protected BangContext _ctx;
    protected MessageBundle _msgs;
    protected StatusLabel _status;
    protected boolean _failed;

    protected BComboBox _gender;
    protected BTextField _handle;
    protected FirstLookView _look;
    protected BButton _done;

    protected static final int PREF_WIDTH = 640;
}
