//
// $Id$

package com.threerings.bang.avatar.server;

import com.threerings.bang.avatar.client.AvatarService;
import com.threerings.bang.avatar.data.LookConfig;
import com.threerings.bang.data.Handle;
import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.server.InvocationException;
import com.threerings.presents.server.InvocationProvider;

/**
 * Defines the server-side of the {@link AvatarService}.
 */
public interface AvatarProvider extends InvocationProvider
{
    /**
     * Handles a {@link AvatarService#createAvatar} request.
     */
    public void createAvatar (ClientObject caller, Handle arg1, boolean arg2, LookConfig arg3, InvocationService.ConfirmListener arg4)
        throws InvocationException;

    /**
     * Handles a {@link AvatarService#selectLook} request.
     */
    public void selectLook (ClientObject caller, String arg1);
}
