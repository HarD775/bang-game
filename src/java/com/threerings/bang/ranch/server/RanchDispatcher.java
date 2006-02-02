//
// $Id$

package com.threerings.bang.ranch.server;

import com.threerings.bang.ranch.client.RanchService;
import com.threerings.bang.ranch.data.RanchMarshaller;
import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.data.InvocationMarshaller;
import com.threerings.presents.server.InvocationDispatcher;
import com.threerings.presents.server.InvocationException;
import com.threerings.util.Name;

/**
 * Dispatches requests to the {@link RanchProvider}.
 */
public class RanchDispatcher extends InvocationDispatcher
{
    /**
     * Creates a dispatcher that may be registered to dispatch invocation
     * service requests for the specified provider.
     */
    public RanchDispatcher (RanchProvider provider)
    {
        this.provider = provider;
    }

    // documentation inherited
    public InvocationMarshaller createMarshaller ()
    {
        return new RanchMarshaller();
    }

    // documentation inherited
    public void dispatchRequest (
        ClientObject source, int methodId, Object[] args)
        throws InvocationException
    {
        switch (methodId) {
        case RanchMarshaller.RECRUIT_BIG_SHOT:
            ((RanchProvider)provider).recruitBigShot(
                source,
                (String)args[0], (Name)args[1], (InvocationService.ResultListener)args[2]
            );
            return;

        default:
            super.dispatchRequest(source, methodId, args);
        }
    }
}
