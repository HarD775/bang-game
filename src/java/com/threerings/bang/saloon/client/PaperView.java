//
// $Id$

package com.threerings.bang.saloon.client;

import java.io.StringReader;
import java.io.StringWriter;
import java.net.URL;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.logging.Level;
import javax.swing.text.html.HTMLDocument;

import com.jmex.bui.BButton;
import com.jmex.bui.BContainer;
import com.jmex.bui.BLabel;
import com.jmex.bui.Spacer;
import com.jmex.bui.event.ActionEvent;
import com.jmex.bui.event.ActionListener;
import com.jmex.bui.icon.ImageIcon;
import com.jmex.bui.layout.GroupLayout;
import com.jmex.bui.layout.TableLayout;
import com.jmex.bui.text.HTMLView;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;

import com.samskivert.util.CollectionUtil;
import com.samskivert.util.ResultListener;
import com.samskivert.velocity.VelocityUtil;

import com.threerings.bang.client.BangUI;
import com.threerings.bang.util.BangContext;
import com.threerings.bang.util.CachedDocument;
import com.threerings.bang.util.DeploymentConfig;

import com.threerings.bang.saloon.data.SaloonCodes;
import com.threerings.bang.saloon.data.SaloonObject;
import com.threerings.bang.saloon.data.TopRankedList;

import static com.threerings.bang.Log.log;

/**
 * Displays the daily paper.
 */
public class PaperView extends BContainer
{
    public PaperView (BangContext ctx)
    {
        super(GroupLayout.makeVStretch());
        ((GroupLayout)getLayoutManager()).setGap(0);
        setStyleClass("news_view");
        _ctx = ctx;
        String townId = ctx.getUserObject().townId;

        BLabel lbl;
        add(lbl = new BLabel("", "news_nameplate"), GroupLayout.FIXED);
        String npath = "ui/saloon/" + townId + "/nameplate.png";
        lbl.setIcon(new ImageIcon(ctx.loadImage(npath)));

        BContainer row = GroupLayout.makeHBox(GroupLayout.RIGHT);
        TableLayout tlay = new TableLayout(6, 0, 0);
        tlay.setHorizontalAlignment(TableLayout.STRETCH);
        BContainer masthead = new BContainer(tlay);
        masthead.setStyleClass("news_masthead");
        // TODO: add proper masthead buttons, etc.
        masthead.add(new BLabel("No. 23", "news_mastlabel"));
        masthead.add(new BLabel("News", "news_mastlabel"));
        masthead.add(new BLabel("Top Scores", "news_mastlabel"));
        masthead.add(new BLabel("Events", "news_mastlabel"));
        masthead.add(new BLabel("Highlights", "news_mastlabel"));
        masthead.add(new BLabel(_dfmt.format(new Date()), "news_mastlabel"));
        row.add(masthead);
        add(row, GroupLayout.FIXED);

        add(_contents = new HTMLView());
        _contents.setStyleClass("news_contents");

        GroupLayout hlay = GroupLayout.makeHoriz(GroupLayout.RIGHT);
        hlay.setGap(40);
        BContainer bcont = new BContainer(hlay);
        bcont.setStyleClass("news_buttons");
        bcont.add(_back = new BButton("", _listener, "back"));
        _back.setStyleClass("back_button");
        _back.setEnabled(false);
        bcont.add(_forward = new BButton("", _listener, "forward"));
        _forward.setStyleClass("fwd_button");
        _forward.setEnabled(true);
        add(bcont, GroupLayout.FIXED);

        // read in the main news page
        refreshNews(false);
    }

    public void init (SaloonObject salobj)
    {
        _salobj = salobj;
    }

    protected void displayPage (int pageNo)
    {
        if (_pageNo == pageNo) {
            return;
        }

        switch (_pageNo = pageNo) {
        case 0:
            setContents(_news.getDocument());
            break;

        case 1:
            setContents(createTopScoresHTML());
            break;
        }

        _back.setEnabled(_pageNo > 0);
        _forward.setEnabled(_pageNo < 1);
    }

    protected void setContents (String contents)
    {
        HTMLDocument doc = new HTMLDocument(BangUI.css);
        doc.setBase(DeploymentConfig.getDocBaseURL());
        try {
            _contents.getEditorKit().read(new StringReader(contents), doc, 0);
            _contents.setContents(doc);
        } catch (Throwable t) {
            log.log(Level.WARNING, "Failed to parse HTML " +
                    "[contents=" + contents + "].", t);
        }
    }

    protected void refreshNews (boolean force)
    {
        if (_news == null) {
            URL base = DeploymentConfig.getDocBaseURL();
            try {
                URL news = new URL(base, NEWS_URL);
                _news = new CachedDocument(news, NEWS_REFRESH_INTERVAL);
            } catch (Exception e) {
                log.log(Level.WARNING, "Failed to create news URL " +
                        "[base=" + base + ", path=" + NEWS_URL + "].", e);
                return;
            }
        }
        if (!_news.refreshDocument(force, _newsup)) {
            setContents(_news.getDocument());
        }
    }

    protected String createTopScoresHTML ()
    {
        // sort our lists in the order we want them displayed in the template
        ArrayList<TopRankedList> lists = new ArrayList<TopRankedList>();
        CollectionUtil.addAll(lists, _salobj.topRanked.iterator());

        try {
            if (_vformatter == null) {
                _vformatter = VelocityUtil.createEngine();
            }
            VelocityContext ctx = new VelocityContext();
            ctx.put("i18n", _ctx.getMessageManager().getBundle(
                        SaloonCodes.SALOON_MSGS));
            ctx.put("lists", lists);
            StringWriter sw = new StringWriter();
            _vformatter.mergeTemplate(
                "rsrc/ui/saloon/top_scores.tmpl", "UTF-8", ctx, sw);
            return sw.toString();

        } catch (Exception e) {
            log.log(Level.WARNING, "Failed to format top scores.", e);
            return _ctx.xlate(SaloonCodes.SALOON_MSGS,
                              SaloonCodes.INTERNAL_ERROR);
        }
    }

    protected ResultListener _newsup = new ResultListener() {
        public void requestCompleted (Object result) {
            updateNews((String)result);
        }
        public void requestFailed (Exception cause) {
            log.log(Level.WARNING, "Failed to load the news.", cause);
            updateNews("m.news_load_failed");
        }
        protected void updateNews (final String text) {
            _ctx.getApp().postRunnable(new Runnable() {
                public void run () {
                    if (text.startsWith("m.")) {
                        setContents(_ctx.xlate(SaloonCodes.SALOON_MSGS, text));
                    } else {
                        setContents(text);
                    }
                }
            });
        }
    };

    protected ActionListener _listener = new ActionListener() {
        public void actionPerformed (ActionEvent event) {
            if (event.getAction().equals("forward")) {
                displayPage(_pageNo+1);
            } else if (event.getAction().equals("back")) {
                displayPage(_pageNo-1);
            }
        }
    };

    protected BangContext _ctx;
    protected SaloonObject _salobj;
    protected BButton _forward, _back;
    protected HTMLView _contents;

    protected int _pageNo;
    protected VelocityEngine _vformatter;
    protected DateFormat _dfmt = DateFormat.getDateInstance(DateFormat.SHORT);

    protected static CachedDocument _news;

    protected static final long NEWS_REFRESH_INTERVAL = 60 * 60 * 1000L;
    protected static final String NEWS_URL = "news_incl.html";
}
