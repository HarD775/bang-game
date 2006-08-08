//
// $Id$

package com.threerings.bang.game.client.sprite;

import java.util.List;
import java.awt.Point;

import com.jme.math.Vector3f;

import com.samskivert.util.ArrayUtil;

import com.threerings.bang.game.client.MoveShootHandler;
import com.threerings.bang.game.data.BangBoard;
import com.threerings.bang.game.data.piece.Piece;

import com.threerings.jme.sprite.Path;

/**
 * Sprite for the Buffalo Rider unit.
 */
public class BuffaloRiderSprite extends UnitSprite
{
    public BuffaloRiderSprite (String type)
    {
        super(type);
    }

    @Override // documentation inherited
    protected Path createPath (BangBoard board)
    {
        Path path = super.createPath(board);
        // something's booched so fire off the shot now
        if (path == null && _effectHandler != null) {
            ((MoveShootHandler)_effectHandler).fireShot();
            _effectHandler = null;
        }
        return path;
    }

    @Override // documentation inherited
    protected Path createPath (BangBoard board, List<Point> path, float speed)
    {
        if (_effectHandler != null) {
            Piece target = _tsprite.getPiece();
            path.add(new Point(target.x, target.y));
        }
        return super.createPath(board, path, speed);
    }

    @Override // documentation inherited
    protected Path createPath (
            Vector3f[] coords, float[] durations, String action)
    {
        if (_effectHandler != null) {
            MoveShootHandler handler = (MoveShootHandler)_effectHandler;
            _effectHandler = null;
            durations[durations.length - 1] = 0f;
            return new BuffaloRiderPath(
                    this, coords, durations, _moveType, action, handler);
        }
        return super.createPath(coords, durations, action);
    }
}
