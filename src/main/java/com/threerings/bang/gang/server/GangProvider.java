//
// $Id$

package com.threerings.bang.gang.server;

import com.threerings.bang.data.Handle;
import com.threerings.bang.gang.client.GangService;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.server.InvocationException;
import com.threerings.presents.server.InvocationProvider;

/**
 * Defines the server-side of the {@link GangService}.
 */
public interface GangProvider extends InvocationProvider
{
    /**
     * Handles a {@link GangService#getGangInfo} request.
     */
    void getGangInfo (ClientObject caller, Handle arg1, InvocationService.ResultListener arg2)
        throws InvocationException;

    /**
     * Handles a {@link GangService#inviteMember} request.
     */
    void inviteMember (ClientObject caller, Handle arg1, String arg2, InvocationService.ConfirmListener arg3)
        throws InvocationException;
}