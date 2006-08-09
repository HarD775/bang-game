//
// $Id$

package com.threerings.bang.client;

import com.threerings.util.Name;

import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;

import com.threerings.bang.data.Handle;

/**
 * A general purpose bootstrap invocation service.
 */
public interface PlayerService extends InvocationService
{
    /**
     * Issues a request to create this player's (free) first Big Shot.
     */
    public void pickFirstBigShot (
        Client client, String type, Name name, ConfirmListener cl);

    /**
     * Invite the specified user to be our pardner.
     */
    public void invitePardner (
        Client client, Handle handle, String message, ConfirmListener listener);

    /**
     * Respond to another cowpoke's invitation to be pardners.
     */
    public void respondToPardnerInvite (
        Client client, Handle inviter, boolean resp, ConfirmListener listener);

    /**
     * Remove one of our pardners from our pardner list.
     */
    public void removePardner (
        Client client, Handle pardner, ConfirmListener listener);

    /**
     * Requests to play the specified tutorial. On success the game will start
     * and the client will enter the game. On failure the supplied listener
     * will be notified.
     */
    public void playTutorial (
        Client client, String tutid, InvocationListener listener);

    /**
     * Request to play a practice session with the specified unit.
     */
    public void playPractice (
        Client client, String unit, InvocationListener listener);

    /**
     * Requests to play a single round match against the computer.
     */
    public void playComputer (
        Client client, int players, String[] scenario, String board,
        boolean autoplay, InvocationListener listener);

    /**
     * Requests to view another player's wanted poster.
     */
    public void getPosterInfo(
        Client client, Handle handle, ResultListener listener);

    /**
     * Requests to update the configurable attributes of our wanted posted
     */
    public void updatePosterInfo(
        Client client, int playerId, String Statement,
        int[] badgeIds, ConfirmListener listener);
}
