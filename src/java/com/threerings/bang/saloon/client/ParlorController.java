//
// $Id$

package com.threerings.bang.saloon.client;

import com.threerings.presents.dobj.AttributeChangeListener;
import com.threerings.presents.dobj.AttributeChangedEvent;

import com.threerings.crowd.client.PlaceController;
import com.threerings.crowd.client.PlaceView;
import com.threerings.crowd.data.PlaceConfig;
import com.threerings.crowd.data.PlaceObject;
import com.threerings.crowd.util.CrowdContext;

import com.threerings.bang.util.BangContext;

import com.threerings.bang.saloon.data.ParlorObject;

/**
 * Handles the client side of a Back Parlor.
 */
public class ParlorController extends PlaceController
    implements AttributeChangeListener
{
    @Override // documentation inherited
    public void init (CrowdContext ctx, PlaceConfig config)
    {
        super.init(ctx, config);
        _ctx = (BangContext)ctx;
    }

    @Override // documentation inherited
    public void willEnterPlace (PlaceObject plobj)
    {
        super.willEnterPlace(plobj);
        _parobj = (ParlorObject)plobj;
        _parobj.addListener(this);
    }

    @Override // documentation inherited
    public void didLeavePlace (PlaceObject plobj)
    {
        super.didLeavePlace(plobj);
        if (_parobj != null) {
            _parobj.removeListener(this);
            _parobj = null;
        }
    }

    // documentation inherited from interface AttributeChangeListener
    public void attributeChanged (AttributeChangedEvent event)
    {
        if (event.getName().equals(ParlorObject.PLAYER_OIDS)) {
            if (_parobj.playerOids == null) {
                _view.clearMatchView();
            } else {
                _view.displayMatchView();
            }
        }
    }

    @Override // documentation inherited
    protected PlaceView createPlaceView (CrowdContext ctx)
    {
        return (_view = new ParlorView((BangContext)ctx, this));
    }

    protected BangContext _ctx;
    protected ParlorView _view;
    protected ParlorObject _parobj;
}
