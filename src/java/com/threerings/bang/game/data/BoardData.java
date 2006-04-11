//
// $Id$

package com.threerings.bang.game.data;

import java.awt.Rectangle;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import java.util.ArrayList;
import java.util.logging.Level;

import com.samskivert.util.StringUtil;

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;
import com.threerings.io.SimpleStreamableObject;

import com.threerings.bang.game.data.piece.Marker;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.Prop;
import com.threerings.bang.game.data.piece.Track;
import com.threerings.bang.game.data.piece.Viewpoint;

import static com.threerings.bang.Log.*;

/**
 * Contains the data ({@link BangBoard} and props, markers, etc.) associated
 * with a board stored on the client or server.
 */
public class BoardData
{
    /** The serialized board data. */
    public byte[] data;
    
    /** The last iteration of the bang board. */
    public static class OldBangBoard extends SimpleStreamableObject
    {
        public int width, height;
        public byte[] heightfield;
        public byte[] terrain;
        public byte[] shadows;
        public float shadowIntensity;
        public byte waterLevel;
        public int waterColor;
        public float waterAmplitude;
        public float[] lightAzimuths, lightElevations;
        public int[] lightDiffuseColors, lightAmbientColors;
        public int skyHorizonColor, skyOverheadColor;
        public float skyFalloff;
        public float windDirection, windSpeed;
    }
    
    /**
     * Serializes the supplied board and piece information and stuffs it
     * into the {@link #data} member.
     */
    public void setData (BangBoard board, Piece[] pieces)
    {
        try {
            // serialize the board and pieces
            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(bout);
            oos.writeObject(board);
            oos.writeInt(pieces.length);
            for (int ii = 0; ii < pieces.length; ii++) {
                writePiece(oos, pieces[ii]);
            }
            oos.flush();

            // store the various bits into our record
            data = bout.toByteArray();
            _board = board;
            _pieces = pieces;

        } catch (IOException ioe) {
            log.log(Level.WARNING, "Failed to encode board " + this, ioe);
        }
    }
    
    /**
     * Decodes and returns the board in this record.
     */
    public BangBoard getBoard ()
    {
        if (_board == null) {
            decodeData();
        }
        return _board;
    }

    /**
     * Decodes and returns the pieces in this record.
     */
    public Piece[] getPieces ()
    {
        if (_pieces == null) {
            decodeData();
        }
        return _pieces;
    }
    
    /**
     * Computes and returns the MD5 hash of the board data.
     */
    public byte[] getDataHash ()
    {
        try {
            return MessageDigest.getInstance("MD5").digest(data);
            
        } catch (NoSuchAlgorithmException nsae) {
            throw new RuntimeException("MD5 codec not available");
        }
    }
    
    /** Returns a string representation of this instance. */
    public String toString ()
    {
        return StringUtil.fieldsToString(this);
    }

    /** Helper function for {@link #toString}. */
    public String dataToString ()
    {
        return data.length + " bytes";
    }
    
    /** Helper for the two load board methods. */
    protected void decodeData ()
    {
        try {
            ObjectInputStream oin = new ObjectInputStream(
                new ByteArrayInputStream(data));
            /*
            oin.addTranslation("com.threerings.bang.game.data.BangBoard",
                "com.threerings.bang.game.data.BoardData$OldBangBoard");
            OldBangBoard obb = (OldBangBoard)oin.readObject();
            _board = new BangBoard(obb.width, obb.height);
            System.arraycopy(obb.heightfield, 0, _board.getHeightfield(), 0,
                obb.heightfield.length);
            System.arraycopy(obb.terrain, 0, _board.getTerrain(), 0,
                obb.terrain.length);
            System.arraycopy(obb.shadows, 0, _board.getShadows(), 0,
                obb.shadows.length);
            _board.setShadowIntensity(obb.shadowIntensity);
            _board.setWaterParams(obb.waterLevel, obb.waterColor,
                obb.waterAmplitude);
            for (int i = 0; i < BangBoard.NUM_LIGHTS; i++) {
                _board.setLightParams(i, obb.lightAzimuths[i],
                    obb.lightElevations[i], obb.lightDiffuseColors[i],
                    obb.lightAmbientColors[i]);
            }
            _board.setSkyParams(obb.skyHorizonColor, obb.skyOverheadColor,
                obb.skyFalloff);
            _board.setWindParams(obb.windDirection, obb.windSpeed);
            */

            // load up our board
            _board = (BangBoard)oin.readObject();

            // load and sanity check our pieces
            int pcount = oin.readInt();
            Rectangle rect = new Rectangle(
                0, 0, _board.getWidth(), _board.getHeight());
            ArrayList<Piece> plist = new ArrayList<Piece>();
            for (int ii = 0; ii < pcount; ii++) {
                Piece p = readPiece(oin);
                if (!rect.contains(p.x, p.y)) {
                    log.warning("Rececting OOB piece " + p + ".");
                } else {
                    plist.add(p);
                }
            }
            _pieces = plist.toArray(new Piece[plist.size()]);

        } catch (Exception e) {
            log.log(Level.WARNING, "Failed to decode board " + this, e);
        }
    }
    
    /** Helper method. */
    protected void writePiece (ObjectOutputStream oout, Piece piece)
        throws IOException
    {
        if (piece instanceof Prop) {
            oout.writeUTF(((Prop)piece).getType());
        } else if (piece instanceof Marker) {
            oout.writeUTF("__marker__");
            oout.writeInt(((Marker)piece).getType());
        } else if (piece instanceof Track) {
            oout.writeUTF("__track__");
            oout.writeByte(((Track)piece).type);
        } else if (piece instanceof Viewpoint) {
            oout.writeUTF("__viewpoint__");
        } else {
            throw new IOException("Unknown piece type " +
                                  "[type=" + piece.getClass().getName() +
                                  ", piece=" + piece + "].");
        }
        piece.persistTo(oout);
    }

    /** Helper method. */
    protected Piece readPiece (ObjectInputStream oin)
        throws IOException
    {
        String type = oin.readUTF();
        Piece piece;
        if (type.equals("__marker__")) {
            piece = new Marker(oin.readInt());
        } else if (type.equals("__track__")) {
            piece = new Track(oin.readByte());
        } else if (type.equals("__viewpoint__")) {
            piece = new Viewpoint();
        } else {
            piece = Prop.getProp(type);
        }
        piece.unpersistFrom(oin);
        return piece;
    }
    
    protected transient BangBoard _board;
    protected transient Piece[] _pieces;
}
