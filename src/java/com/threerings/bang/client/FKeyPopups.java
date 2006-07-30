//
// $Id$

package com.threerings.bang.client;

import java.awt.image.BufferedImage;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;
import javax.imageio.ImageIO;

import com.jme.input.KeyInput;

import com.jmex.bui.BButton;
import com.jmex.bui.BContainer;
import com.jmex.bui.BDecoratedWindow;
import com.jmex.bui.BLabel;
import com.jmex.bui.BScrollPane;
import com.jmex.bui.BTextField;
import com.jmex.bui.event.ActionEvent;
import com.jmex.bui.event.ActionListener;
import com.jmex.bui.event.InputEvent;
import com.jmex.bui.layout.GroupLayout;
import com.jmex.bui.util.Dimension;

import com.samskivert.util.ResultListener;
import com.samskivert.util.StringUtil;
import com.threerings.util.MessageBundle;

import com.threerings.bang.avatar.client.AvatarView;
import com.threerings.bang.avatar.data.Look;

import com.threerings.bang.admin.client.RuntimeConfigView;
import com.threerings.bang.admin.client.ServerStatusView;
import com.threerings.bang.client.PickTutorialView;
import com.threerings.bang.client.bui.EnablingValidator;
import com.threerings.bang.data.BangCodes;
import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.util.BangContext;

import static com.threerings.bang.Log.log;

/**
 * Handles popping up various windows when the user presses a function key or
 * some other globally mapped keys.
 */
public class FKeyPopups
    implements GlobalKeyManager.Command
{
    /** Enumerates the various types of popups we know about. */
    public static enum Type {
        HELP(KeyInput.KEY_F1, 0, false, true),
        TUTORIALS(KeyInput.KEY_T, 0, false, true),
        REPORT_BUG(KeyInput.KEY_F2, 0, false, false),
        CLIENT_LOG(KeyInput.KEY_F3, InputEvent.SHIFT_DOWN_MASK, false, false),
        SERVER_STATUS(KeyInput.KEY_F4, 0, true, false),
        SERVER_CONFIG(KeyInput.KEY_F5, 0, true, false),
        CLIENT_CONFIG(KeyInput.KEY_F6, CTRL_SHIFT, false, false);

        public int keyCode () {
            return _keyCode;
        }

        public int modifiers () {
            return _modifiers;
        }

        public boolean requiresAdmin () {
            return _requiresAdmin;
        }

        public boolean checkCanDisplay () {
            return _checkCanDisplay;
        }

        Type (int keyCode, int modifiers, boolean requiresAdmin,
              boolean checkCanDisplay) {
            _keyCode = keyCode;
            _modifiers = modifiers;
            _requiresAdmin = requiresAdmin;
            _checkCanDisplay = checkCanDisplay;
        }

        protected int _keyCode, _modifiers;
        protected boolean _requiresAdmin, _checkCanDisplay;
    };

    /**
     * Creates the function key popups manager and registers its key bindings.
     */
    public FKeyPopups (BangContext ctx)
    {
        _ctx = ctx;
        for (Type type : Type.values()) {
            _ctx.getKeyManager().registerCommand(type.keyCode(), this);
        }
        _msgs = _ctx.getMessageManager().getBundle(BangCodes.BANG_MSGS);
    }

    /**
     * Shows the popup of the specifide type, clearing any existing popup
     * prior to doing so.
     */
    public void showPopup (Type type)
    {
        // don't auto-replace a never clear popup
        if (_popped != null && _popped.isAdded() &&
            _popped.getLayer() == BangCodes.NEVER_CLEAR_LAYER) {
            return;
        }

        // if this is the same as the current popup window, just dismiss it
        if (type == _poppedType && _popped != null && _popped.isAdded()) {
            clearPopup();
            return;
        }

        // make sure we can display an FKEY popup right now (but only if we
        // don't already have one popped up, in which case we'll replace it)
        if (type.checkCanDisplay() && (_popped == null || !_popped.isAdded()) &&
            !_ctx.getBangClient().canDisplayPopup(MainView.Type.FKEY)) {
            return;
        }

        // if this popup requires admin privileges, make sure we've got 'em
        boolean isAdmin = (_ctx.getUserObject() != null) &&
            _ctx.getUserObject().tokens.isAdmin();
        if (type.requiresAdmin() && !isAdmin) {
            return;
        }

        BDecoratedWindow popup = null;
        switch (type) {
        default:
        case HELP:
            popup = createHelp();
            break;
        case REPORT_BUG:
            popup = createReportBug();
            break;
        case TUTORIALS:
            popup = new PickTutorialView(_ctx, PickTutorialView.Mode.FKEY);
            break;
        case CLIENT_LOG:
            popup = createRecentLog();
            break;
        case SERVER_STATUS:
            popup = new ServerStatusView(_ctx);
            break;
        case SERVER_CONFIG:
            popup = new RuntimeConfigView(_ctx);
            break;
        case CLIENT_CONFIG:
            popup = new ConfigEditorView(_ctx);
            break;
        }

        if (popup != null) {
            clearPopup();
            _poppedType = type;
            _ctx.getBangClient().displayPopup(_popped = popup, true, 500);
        }
    }

    // documentation inherited from interface GlobalKeyManager.Command
    public void invoke (int keyCode, int modifiers)
    {
        // special hackery to handle Ctrl-Shift-F2 which submits an
        // auto-bug-report and exits the client
        if (keyCode == KeyInput.KEY_F2 && modifiers == CTRL_SHIFT) {
            if (!_autoBugged) { // avoid repeat pressage
                BangClient.submitBugReport(_ctx, "Autobug!", true);
            }
            return;
        }

        // other hackery to handle taking screen shots
        if (keyCode == KeyInput.KEY_F12) {
            String fname = "bang_screen_" + _sfmt.format(new Date());
            _ctx.getRenderer().takeScreenShot(fname);
            String msg = MessageBundle.tcompose(
                "m.screenshot_taken", fname + ".png");
            _ctx.getChatDirector().displayFeedback(BangCodes.BANG_MSGS, msg);
            return;
        }

        boolean isAdmin = (_ctx.getUserObject() != null) &&
            _ctx.getUserObject().tokens.isAdmin();

        // yet more hackery to handle dumping a copy of your current avatar
        // look to a file (only available to admins currently)
        if (keyCode == KeyInput.KEY_F11) {
            if (modifiers == CTRL_SHIFT && isAdmin) {
                createCurrentLookSnapshot();
            }
            return;
        }

        // otherwise pop up the dialog associated with they key they pressed
        for (Type type : Type.values()) {
            if (type.keyCode() == keyCode && type.modifiers() == modifiers) {
                showPopup(type);
                return;
            }
        }
    }

    protected void clearPopup ()
    {
        _poppedType = null;
        if (_popped != null) {
            _ctx.getBangClient().clearPopup(_popped, true);
            _popped = null;
        }
    }

    protected BDecoratedWindow createHelp ()
    {
        BDecoratedWindow help = createDialogWindow("m.key_help_title");
        String text = _msgs.get("m.key_help");
        if (_ctx.getUserObject().tokens.isAdmin()) {
            text += _msgs.get("m.key_help_admin");
        }
        help.add(new BLabel(text, "dialog_text_left"));
        help.add(makeDismiss(help), GroupLayout.FIXED);
        return help;
    }

    protected BDecoratedWindow createReportBug ()
    {
        final BDecoratedWindow bug = createDialogWindow("m.bug_title");
        ((GroupLayout)bug.getLayoutManager()).setOffAxisPolicy(
            GroupLayout.STRETCH);
        bug.setLayer(BangCodes.NEVER_CLEAR_LAYER);
        bug.add(new BLabel(_msgs.get("m.bug_intro"), "dialog_text_left"));
        final BTextField descrip = new BTextField("");
        bug.add(descrip, GroupLayout.FIXED);
        descrip.requestFocus();
        BContainer buttons = GroupLayout.makeHBox(GroupLayout.CENTER);
        bug.add(buttons, GroupLayout.FIXED);

        ActionListener buglist = new ActionListener() {
            public void actionPerformed (ActionEvent event) {
                if (event.getAction().equals("submit")) {
                    BangClient.submitBugReport(_ctx, descrip.getText(), false);
                }
                _ctx.getBangClient().clearPopup(bug, true);
            }
        };
        BButton submit =
            new BButton(_msgs.get("m.bug_submit"), buglist, "submit");
        buttons.add(submit);
        buttons.add(new BButton(_msgs.get("m.cancel"), buglist, "cancel"));
        // disable the submit button until a description is entered
        new EnablingValidator(descrip, submit);
        return bug;
    }

    protected BDecoratedWindow createRecentLog ()
    {
        BDecoratedWindow window = new BDecoratedWindow(
            _ctx.getStyleSheet(), _msgs.get("m.log_title"));
        ((GroupLayout)window.getLayoutManager()).setGap(15);
        StringBuffer buf = new StringBuffer();
        for (int ii = BangApp.recentLog.size()-1; ii >= 0; ii--) {
            String line = (String)BangApp.recentLog.get(ii);
            buf.append(line.replace("@", "@@"));
        }
        window.add(new BScrollPane(new BLabel(buf.toString(), "debug_log")));
        window.add(makeDismiss(window), GroupLayout.FIXED);
        window.setPreferredSize(new Dimension(1000, 700));
        return window;
    }

    protected BDecoratedWindow createDialogWindow (String title)
    {
        BDecoratedWindow window =
            new BDecoratedWindow(_ctx.getStyleSheet(), _msgs.get(title));
        ((GroupLayout)window.getLayoutManager()).setGap(15);
        window.setStyleClass("dialog_window");
        return window;
    }

    protected void createCurrentLookSnapshot ()
    {
        PlayerObject user = _ctx.getUserObject();
        Look look = user.getLook(Look.Pose.DEFAULT);
        final File target = new File(System.getProperty("user.home") +
                                     File.separator + "Desktop" +
                                     File.separator + look.name + ".png");
        AvatarView.getImage(_ctx, look.getAvatar(user),
                            new ResultListener<BufferedImage>() {
            public void requestCompleted (BufferedImage image) {
                try {
                    ImageIO.write(image, "PNG", target);
                    _ctx.getChatDirector().displayFeedback(
                        BangCodes.BANG_MSGS, "m.avatar_saved");
                } catch (Exception e) {
                    log.log(Level.WARNING, "Failed to write avatar image " +
                            "[target=" + target + "].", e);
                    _ctx.getChatDirector().displayFeedback(
                        BangCodes.BANG_MSGS, "m.avatar_save_failed");
                }
            }
            public void requestFailed (Exception cause) {
                // not called
            }
        });
    }

    protected BButton makeDismiss (final BDecoratedWindow popup)
    {
        return new BButton(_msgs.get("m.dismiss"), new ActionListener() {
            public void actionPerformed (ActionEvent event) {
                _ctx.getBangClient().clearPopup(popup, true);
            }
        }, "dismiss");
    }

    protected BangContext _ctx;
    protected MessageBundle _msgs;

    protected Type _poppedType = null;
    protected BDecoratedWindow _popped;
    protected boolean _autoBugged;

    protected static SimpleDateFormat _sfmt =
        new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");

    protected static final int CTRL_SHIFT =
        InputEvent.CTRL_DOWN_MASK|InputEvent.SHIFT_DOWN_MASK;
}
