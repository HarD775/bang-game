//
// $Id$

package com.threerings.bang.avatar.server;

import com.threerings.bang.avatar.client.AvatarService;
import com.threerings.bang.avatar.data.AvatarMarshaller;
import com.threerings.bang.avatar.data.LookConfig;
import com.threerings.bang.data.Handle;
import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.data.InvocationMarshaller;
import com.threerings.presents.server.InvocationDispatcher;
import com.threerings.presents.server.InvocationException;

/**
 * Dispatches requests to the {@link AvatarProvider}.
 */
public class AvatarDispatcher extends InvocationDispatcher
{
    /**
     * Creates a dispatcher that may be registered to dispatch invocation
     * service requests for the specified provider.
     */
    public AvatarDispatcher (AvatarProvider provider)
    {
        this.provider = provider;
    }

    // documentation inherited
    public InvocationMarshaller createMarshaller ()
    {
        return new AvatarMarshaller();
    }

    // documentation inherited
    public void dispatchRequest (
        ClientObject source, int methodId, Object[] args)
        throws InvocationException
    {
        switch (methodId) {
        case AvatarMarshaller.CREATE_AVATAR:
            ((AvatarProvider)provider).createAvatar(
                source,
                (Handle)args[0], ((Boolean)args[1]).booleanValue(), (LookConfig)args[2], ((Integer)args[3]).intValue(), (InvocationService.ConfirmListener)args[4]
            );
            return;

        case AvatarMarshaller.SELECT_LOOK:
            ((AvatarProvider)provider).selectLook(
                source,
                (String)args[0]
            );
            return;

        default:
            super.dispatchRequest(source, methodId, args);
            return;
        }
    }
}
