//
// $Id$

package com.threerings.bang.game.data.piece;

import com.threerings.util.StreamableArrayList;
import com.threerings.io.SimpleStreamableObject;

import com.threerings.bang.game.client.sprite.MarkerSprite;
import com.threerings.bang.game.client.sprite.PieceSprite;
import com.threerings.bang.game.client.sprite.TotemBaseSprite;
import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.effect.TotemEffect;

import static com.threerings.bang.Log.log;

/**
 * A totem base can have totem pieces added to it and toped off by a 
 * totem crown.
 */
public class TotemBase extends Prop
{
    /**
     * Returns true if a totem piece can be added to the base.
     */
    public boolean canAddPiece ()
    {
        // the only time you can't is if a crown is on top
        return !TotemEffect.TOTEM_CROWN_BONUS.equals(getTopPiece());
    }

    /**
     * Add a totem piece to the base.
     */
    public void addPiece (String type, int owner)
    {
        int idx = _pieces.size() - 1;
        if (idx > -1) {
            _pieces.get(idx).damage = damage;
        }
        PieceData data = new PieceData(type, owner);
        _pieces.add(data);
        damage = data.type.damage();
    }

    @Override // documentation inherited
    public void wasDamaged (int newDamage)
    {
        super.wasDamaged(newDamage);
        _destroyed = null;
    }
    
    @Override // documentation inherited
    public void wasKilled (short tick)
    {
        int idx = _pieces.size() - 1;
        _destroyed = _pieces.remove(idx--);
        if (idx > -1) {
            damage = _pieces.get(idx).damage;
        } else {
            damage = 0;
        }
    }

    /** 
     * Returns the height of the totem.
     */
    public int getTotemHeight ()
    {
        return _pieces.size();
    }

    /**
     * Returns the number of pieces on the totem.
     */
    public int numPieces ()
    {
        return _pieces.size();
    }

    /**
     * Returns the type of piece on the top.
     */
    public String getTopPiece ()
    {
        if (_pieces.size() > 0) {
            return getType(_pieces.size() - 1).bonus();
        }
        return null;
    }

    /**
     * Returns the owner id of the piece on top.
     */
    public int getTopOwner ()
    {
        return getOwner(_pieces.size() - 1);
    }

    /**
     * Returns the owner of the piece at index idx.
     */
    public int getOwner (int idx)
    {
        return (idx < 0 || idx >= _pieces.size()) ? 
            -1 : _pieces.get(idx).owner;
    }
    
    /**
     * Returns the owner of the last piece destroyed.
     */
    public int getDestroyedOwner ()
    {
        return (_destroyed == null) ? -1 : _destroyed.owner;
    }

    /**
     * Returns the type of the piece at index idx.
     */
    public TotemBonus.Type getType (int idx)
    {
        if (idx < 0 || idx >= _pieces.size()) {
            log.warning("Requested type of OOB totem " +
                        "[idx=" + idx + ", have=" + _pieces.size() + "].");
            Thread.dumpStack();
            return TotemBonus.Type.TOTEM_SMALL;
        } else {
            return _pieces.get(idx).type;
        }
    }

    /**
     * Returns the type of the last piece destroyed.
     */
    public TotemBonus.Type getDestroyedType ()
    {
        return (_destroyed == null) ? null : _destroyed.type;
    }

    @Override // documentation inherited
    public boolean isTargetable ()
    {
        return _pieces.size() > 0 && canAddPiece();
    }

    @Override // documentation inherited
    public boolean willBeTargetable ()
    {
        return true;
    }

    @Override // documentation inherited
    public boolean isSameTeam (BangObject bangobj, Piece target)
    {
        return getTopOwner() == target.owner;
    }

    @Override // documentation inherited
    public int getTicksPerMove ()
    {
        return Integer.MAX_VALUE;
    }

    @Override // documentation inherited
    public PieceSprite createSprite ()
    {
        return new TotemBaseSprite();
    }

    @Override // documentation inherited
    public Object clone ()
    {
        TotemBase base = (TotemBase)super.clone();
        @SuppressWarnings("unchecked") StreamableArrayList<PieceData> npieces =
            (StreamableArrayList<PieceData>)base._pieces.clone();
        base._pieces = npieces;
        return base;
    }

    protected class PieceData extends SimpleStreamableObject
    {
        public int owner;
        public TotemBonus.Type type;
        public int damage;

        public PieceData ()
        {
        }

        public PieceData (String type, int owner)
        {
            this.type = TotemBonus.TOTEM_LOOKUP.get(type);
            this.owner = owner;
        }
    }
    
    protected transient StreamableArrayList<PieceData> _pieces = 
        new StreamableArrayList<PieceData>();
    protected transient PieceData _destroyed;
}
