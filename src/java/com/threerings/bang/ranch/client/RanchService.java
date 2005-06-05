//
// $Id$

package com.threerings.bang.ranch.client;

import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;

/**
 * Provides ranch-related functionality.
 */
public interface RanchService extends InvocationService
{
    /**
     * Requests that a big shot of the specified type be recruited onto
     * the player's ranch.
     */
    public void recruitBigShot (
        Client client, String type, ResultListener listener);
}
