//
// $Id$

package com.threerings.bang.game.data;

import com.threerings.bang.game.client.BangService;
import com.threerings.bang.game.data.BangBoard;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.InvocationMarshaller;
import com.threerings.presents.dobj.InvocationResponseEvent;

/**
 * Provides the implementation of the {@link BangService} interface
 * that marshalls the arguments and delivers the request to the provider
 * on the server. Also provides an implementation of the response listener
 * interfaces that marshall the response arguments and deliver them back
 * to the requesting client.
 */
public class BangMarshaller extends InvocationMarshaller
    implements BangService
{
    // documentation inherited
    public static class BoardMarshaller extends ListenerMarshaller
        implements BoardListener
    {
        /** The method id used to dispatch {@link #requestProcessed}
         * responses. */
        public static final int REQUEST_PROCESSED = 1;

        // documentation inherited from interface
        public void requestProcessed (BangBoard arg1, Piece[] arg2)
        {
            _invId = null;
            omgr.postEvent(new InvocationResponseEvent(
                               callerOid, requestId, REQUEST_PROCESSED,
                               new Object[] { arg1, arg2 }));
        }

        // documentation inherited
        public void dispatchResponse (int methodId, Object[] args)
        {
            switch (methodId) {
            case REQUEST_PROCESSED:
                ((BoardListener)listener).requestProcessed(
                    (BangBoard)args[0], (Piece[])args[1]);
                return;

            default:
                super.dispatchResponse(methodId, args);
                return;
            }
        }
    }

    /** The method id used to dispatch {@link #cancelOrder} requests. */
    public static final int CANCEL_ORDER = 1;

    // documentation inherited from interface
    public void cancelOrder (Client arg1, int arg2)
    {
        sendRequest(arg1, CANCEL_ORDER, new Object[] {
            Integer.valueOf(arg2)
        });
    }

    /** The method id used to dispatch {@link #getBoard} requests. */
    public static final int GET_BOARD = 2;

    // documentation inherited from interface
    public void getBoard (Client arg1, BangService.BoardListener arg2)
    {
        BangMarshaller.BoardMarshaller listener2 = new BangMarshaller.BoardMarshaller();
        listener2.listener = arg2;
        sendRequest(arg1, GET_BOARD, new Object[] {
            listener2
        });
    }

    /** The method id used to dispatch {@link #order} requests. */
    public static final int ORDER = 3;

    // documentation inherited from interface
    public void order (Client arg1, int arg2, short arg3, short arg4, int arg5, InvocationService.ResultListener arg6)
    {
        InvocationMarshaller.ResultMarshaller listener6 = new InvocationMarshaller.ResultMarshaller();
        listener6.listener = arg6;
        sendRequest(arg1, ORDER, new Object[] {
            Integer.valueOf(arg2), Short.valueOf(arg3), Short.valueOf(arg4), Integer.valueOf(arg5), listener6
        });
    }

    /** The method id used to dispatch {@link #playCard} requests. */
    public static final int PLAY_CARD = 4;

    // documentation inherited from interface
    public void playCard (Client arg1, int arg2, Object arg3, InvocationService.ConfirmListener arg4)
    {
        InvocationMarshaller.ConfirmMarshaller listener4 = new InvocationMarshaller.ConfirmMarshaller();
        listener4.listener = arg4;
        sendRequest(arg1, PLAY_CARD, new Object[] {
            Integer.valueOf(arg2), arg3, listener4
        });
    }

    /** The method id used to dispatch {@link #reportPerformance} requests. */
    public static final int REPORT_PERFORMANCE = 5;

    // documentation inherited from interface
    public void reportPerformance (Client arg1, String arg2, String arg3, int[] arg4)
    {
        sendRequest(arg1, REPORT_PERFORMANCE, new Object[] {
            arg2, arg3, arg4
        });
    }

    /** The method id used to dispatch {@link #selectStarters} requests. */
    public static final int SELECT_STARTERS = 6;

    // documentation inherited from interface
    public void selectStarters (Client arg1, int arg2, int[] arg3)
    {
        sendRequest(arg1, SELECT_STARTERS, new Object[] {
            Integer.valueOf(arg2), arg3
        });
    }

    /** The method id used to dispatch {@link #selectTeam} requests. */
    public static final int SELECT_TEAM = 7;

    // documentation inherited from interface
    public void selectTeam (Client arg1, String[] arg2)
    {
        sendRequest(arg1, SELECT_TEAM, new Object[] {
            arg2
        });
    }

}
