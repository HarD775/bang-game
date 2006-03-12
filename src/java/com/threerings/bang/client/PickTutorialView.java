//
// $Id$

package com.threerings.bang.client;

import com.jmex.bui.BButton;
import com.jmex.bui.BContainer;
import com.jmex.bui.BDecoratedWindow;
import com.jmex.bui.BLabel;
import com.jmex.bui.event.ActionEvent;
import com.jmex.bui.event.ActionListener;
import com.jmex.bui.icon.ImageIcon;
import com.jmex.bui.layout.GroupLayout;
import com.jmex.bui.layout.TableLayout;
import com.jmex.bui.util.Dimension;

import com.threerings.util.MessageBundle;
import com.threerings.util.Name;

import com.threerings.presents.client.InvocationService.ConfirmListener;
import com.threerings.parlor.game.data.GameAI;

import com.threerings.bang.game.data.BangConfig;
import com.threerings.bang.game.data.TutorialConfig;
import com.threerings.bang.game.util.TutorialUtil;

import com.threerings.bang.data.BangCodes;
import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.data.Stat;
import com.threerings.bang.util.BangContext;

import com.threerings.bang.client.util.ReportingListener;

import static com.threerings.bang.Log.log;

/**
 * Displays a list of completed and uncompleted tutorials and allows the user
 * to play and replay them.
 */
public class PickTutorialView extends BDecoratedWindow
    implements ActionListener
{
    /** The various modes in which we pop up the tutorial view. */
    public static enum Mode { FIRST_TIME, FKEY, COMPLETED };

    /**
     * Creates the pick tutorial view.
     *
     * @param completed the identifier for the just completed tutorial, or null
     * if we're displaying this view from in town.
     */
    public PickTutorialView (BangContext ctx, Mode mode)
    {
        super(ctx.getStyleSheet(), null);
        setStyleClass("dialog_window");
        setLayoutManager(GroupLayout.makeVert(
                             GroupLayout.NONE, GroupLayout.CENTER,
                             GroupLayout.NONE));
        ((GroupLayout)getLayoutManager()).setGap(25);

        _ctx = ctx;
        _msgs = _ctx.getMessageManager().getBundle(BangCodes.BANG_MSGS);
        PlayerObject self = _ctx.getUserObject();

        String tmsg, hmsg;
        switch (mode) {
        default:
        case FKEY:
            tmsg = "m.tut_title";
            hmsg = "m.tut_intro";
            break;
        case FIRST_TIME:
            tmsg = "m.tut_first_title";
            hmsg = "m.tut_first_intro";
            break;
        case COMPLETED:
            tmsg = "m.tut_completed_title";
            hmsg = "m.tut_completed_intro";
            break;
        }
        add(new BLabel(_msgs.get(tmsg), "window_title"));
        add(new BLabel(_msgs.get(hmsg), "dialog_text"));

        ImageIcon comp = new ImageIcon(
            ctx.loadImage("ui/tutorials/complete.png"));
        ImageIcon incomp = new ImageIcon(
            ctx.loadImage("ui/tutorials/incomplete.png"));

        BContainer table = new BContainer(new TableLayout(2, 5, 15));
        add(table);

        int unplayed = 0;
        for (int ii = 0; ii < BangCodes.TUTORIALS.length; ii++) {
            String tid = BangCodes.TUTORIALS[ii];
            ImageIcon icon;
            String btext;
            if (self.stats.containsValue(Stat.Type.TUTORIALS_COMPLETED, tid)) {
                icon = comp;
                btext = "m.tut_replay";
            } else {
                icon = incomp;
                btext = "m.tut_play";
                unplayed++;
            }

            BLabel tlabel = new BLabel(
                _msgs.get("m.tut_" + tid), "tutorial_text");
            tlabel.setIcon(icon);
            table.add(tlabel);

            BButton play = new BButton(_msgs.get(btext), this, tid);
            play.setStyleClass("alt_button");
            table.add(play);

            if (unplayed > 1) {
                tlabel.setEnabled(false);
                play.setEnabled(false);
            }
        }

        add(new BLabel(_msgs.get("m.tut_access_key"), "dialog_tip"));

        if (mode != Mode.COMPLETED) {
            add(new BButton(_msgs.get("m.dismiss"), this, "dismiss"));
        } else {
            add(new BButton(_msgs.get("m.to_town"), this, "to_town"));
        }
    }

    // documentation inherited from interface ActionListener
    public void actionPerformed (ActionEvent event)
    {
        String action = event.getAction();
        if (action.equals("dismiss")) {
            BangPrefs.setDeclinedTutorials(_ctx.getUserObject());;
            _ctx.getBangClient().clearPopup(this, true);
            _ctx.getBangClient().checkShowIntro();

        } else if (action.equals("to_town")) {
            BangPrefs.setDeclinedTutorials(_ctx.getUserObject());;
            _ctx.getBangClient().clearPopup(this, true);
            _ctx.getLocationDirector().leavePlace();
            _ctx.getBangClient().showTownView();

        } else {
            PlayerService psvc = (PlayerService)
                _ctx.getClient().requireService(PlayerService.class);
            ReportingListener rl = new ReportingListener(
                _ctx, BangCodes.BANG_MSGS, "m.start_tut_failed");
            psvc.playTutorial(_ctx.getClient(), action, rl);
        }
    }

    @Override // documentation inherited
    protected Dimension computePreferredSize (int whint, int hhint)
    {
        return super.computePreferredSize(400, -1);
    }

    protected BangContext _ctx;
    protected MessageBundle _msgs;
}
