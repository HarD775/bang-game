//
// $Id$

package com.threerings.bang.editor;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ItemListener;
import java.awt.event.ItemEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;

import com.samskivert.swing.VGroupLayout;
import com.samskivert.swing.HGroupLayout;

import com.threerings.util.MessageBundle;

import com.threerings.bang.data.BangCodes;
import com.threerings.bang.game.data.scenario.ScenarioInfo;
import com.threerings.bang.server.persist.BoardRecord;
import com.threerings.bang.util.BasicContext;

/**
 * Displays and allows editing of board metadata.
 */
public class BoardInfo extends JPanel
    implements ItemListener
{
    public BoardInfo (BasicContext ctx, EditorPanel panel)
    {
        _panel = panel;
        setLayout(new VGroupLayout(VGroupLayout.NONE, VGroupLayout.STRETCH,
                                   5, VGroupLayout.TOP));
        _ctx = ctx;
        _msgs = ctx.getMessageManager().getBundle("editor");
        MessageBundle gmsgs = ctx.getMessageManager().getBundle("game");

        add(new JLabel(_msgs.get("m.board_name")));
        add(_name = new JTextField());

        add(_pcount = new JLabel());
        updatePlayers(0);

        add(new JLabel(_msgs.get("m.scenarios")));
        JPanel spanel = new JPanel(
            new VGroupLayout(VGroupLayout.NONE, VGroupLayout.STRETCH,
                             2, VGroupLayout.TOP));
        ArrayList<ScenarioInfo> scens = ScenarioInfo.getScenarios(
            BangCodes.TOWN_IDS[BangCodes.TOWN_IDS.length-1], true);
        JCheckBox box;

        // prop visibility combo box
        _props = new JComboBox();
        _props.addItem(new ScenarioLabel(null));

        for (ScenarioInfo info : scens) {
            spanel.add(box = new JCheckBox(gmsgs.get(info.getName())));
            _sboxes.put(info.getIdent(), box);
            _props.addItem(new ScenarioLabel(info.getIdent()));
        }

        add(new JScrollPane(spanel) {
            public Dimension getPreferredSize () {
                Dimension d = super.getPreferredSize();
                d.height = Math.min(d.height, 200);
                return d;
            }
        });

        // create the prop visibility panel
        JPanel ppanel = new JPanel(new HGroupLayout(HGroupLayout.STRETCH));
        ppanel.add(_plabel = new JLabel(_msgs.get("m.props")), 
                HGroupLayout.FIXED);
        ppanel.add(_props);
        _props.addItemListener(this);
        add(ppanel);
    }

    /**
     * Updates the "number of players" count displayed for this board.
     */
    public void updatePlayers (int players)
    {
        _players = players;
        _pcount.setText(_msgs.get("m.players", "" + players));
    }

    /**
     * Reads the supplied board's metadata and configures the UI
     * appropriately.
     */
    public void fromBoard (BoardRecord board)
    {
        _name.setText(board.name);
        _props.setSelectedIndex(0);
        updatePlayers(board.players);

        // first turn off all the scenario check boxes
        for (String scid : _sboxes.keySet()) {
            _sboxes.get(scid).setSelected(false);
        }
        // then turn on the ones that are valid for this board
        String[] scids = board.getScenarios();
        for (int ii = 0; ii < scids.length; ii++) {
            JCheckBox box = _sboxes.get(scids[ii]);
            if (box != null) {
                box.setSelected(true);
            }
        }
    }

    /**
     * Configures the supplied board's metadata with the values from the
     * user interface.
     */
    public void toBoard (BoardRecord board)
    {
        board.name = _name.getText();
        board.players = _players;
        ArrayList<String> scenids = getSelectedScenarios();
        board.setScenarios(scenids.toArray(new String[scenids.size()]));
    }

    /**
     * Get an ArrayList of selected scenario ids.
     */
    public ArrayList<String> getSelectedScenarios ()
    {
        ArrayList<String> scenids = new ArrayList<String>();
        for (String scid : _sboxes.keySet()) {
            if (_sboxes.get(scid).isSelected()) {
                scenids.add(scid);
            }
        }
        return scenids;
    }

    /**
     * Clears the user interface.
     */
    public void clear ()
    {
        _name.setText("");
        for (Iterator<JCheckBox> it = _sboxes.values().iterator();
                it.hasNext(); ) {
            it.next().setSelected(false);
        }
    }

    /**
     * Get the currently selected prop id.
     */
    public String getPropId ()
    {
        return ((ScenarioLabel)_props.getSelectedItem()).id;
    }

    // inherited from iterface ItemListener
    public void itemStateChanged (ItemEvent ie)
    {
        ScenarioLabel sl = (ScenarioLabel)ie.getItem();
        _plabel.setForeground(sl.getColor());
        _props.setForeground(sl.getColor());
        ((EditorController)_panel.getController()).setViewingProps(sl.id);
    }
    
    protected class ScenarioLabel {
        String id;

        public ScenarioLabel (String scenarioId)
        {
            id = scenarioId;
            _name = (id == null ? _msgs.get("m.all") :
                    _ctx.xlate("game", "m.scenario_" + id));
        }

        public String toString ()
        {
            return _name;
        }

        public Color getColor ()
        {
            return (id == null) ? Color.BLACK : Color.RED; 
        }

        String _name;
    }

    protected BasicContext _ctx;
    protected MessageBundle _msgs;

    protected JTextField _name;
    protected JLabel _pcount, _plabel;
    protected JComboBox _props;
    protected int _players;

    protected EditorPanel _panel;

    protected HashMap<String,JCheckBox> _sboxes =
        new HashMap<String,JCheckBox>();
}
