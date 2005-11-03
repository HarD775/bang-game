//
// $Id$

package com.threerings.bang.avatar.server.persist;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;

import com.samskivert.io.PersistenceException;
import com.samskivert.jdbc.ConnectionProvider;
import com.samskivert.jdbc.DatabaseLiaison;
import com.samskivert.jdbc.JDBCUtil;
import com.samskivert.jdbc.SimpleRepository;

import com.threerings.bang.avatar.data.Look;

import static com.threerings.bang.Log.log;

/**
 * Stores a set of "looks" (avatar fingerprints) for players.
 */
public class LookRepository extends SimpleRepository
{
    /**
     * The database identifier used when establishing a database connection.
     * This value being <code>lookdb</code>.
     */
    public static final String LOOK_DB_IDENT = "lookdb";

    /**
     * Constructs a new look repository with the specified connection provider.
     *
     * @param conprov the connection provider via which we will obtain our
     * database connection.
     */
    public LookRepository (ConnectionProvider conprov)
        throws PersistenceException
    {
        super(conprov, LOOK_DB_IDENT);
    }

    /**
     * Loads up all of the different "looks" registered for this player.
     */
    public ArrayList<Look> loadLooks (int playerId)
        throws PersistenceException
    {
        final ArrayList<Look> looks = new ArrayList<Look>();
        final String query = "select NAME, ASPECTS, ARTICLES from LOOKS " +
            "where PLAYER_ID = " + playerId;
        execute(new Operation() {
            public Object invoke (Connection conn, DatabaseLiaison liaison)
                throws SQLException, PersistenceException
            {
                Statement stmt = conn.createStatement();
                try {
                    ResultSet rs = stmt.executeQuery(query);
                    while (rs.next()) {
                        looks.add(decodeLook(rs));
                    }
                } finally {
                    JDBCUtil.close(stmt);
                }
                return null;
            }
        });
        return looks;
    }

    /**
     * Inserts a new look into the database for the specified player.
     */
    public void insertLook (final int playerId, final Look look)
        throws PersistenceException
    {
        execute(new Operation() {
            public Object invoke (Connection conn, DatabaseLiaison liaison)
                throws SQLException, PersistenceException
            {
                String ssql = "insert into LOOKS " +
                    "(PLAYER_ID, NAME, ASPECTS, ARTICLES) values (?, ?, ?, ?)";
                PreparedStatement stmt = conn.prepareStatement(ssql);
                try {
                    stmt.setInt(1, playerId);
                    stmt.setString(2, look.name);
                    stmt.setBytes(3, toByteArray(look.aspects));
                    stmt.setBytes(4, toByteArray(look.articles));
                    stmt.executeUpdate();
                } finally {
                    JDBCUtil.close(stmt);
                }
                return null;
            }
        });
    }

    /**
     * Writes updates to a look out to the database. <em>Note:</em> only {@link
     * Look#articles} are updated, all other aspects of a look are immutable.
     */
    public void updateLook (final int playerId, final Look look)
        throws PersistenceException
    {
        execute(new Operation() {
            public Object invoke (Connection conn, DatabaseLiaison liaison)
                throws SQLException, PersistenceException
            {
                String ssql = "update LOOKS set ARTICLES = ? " +
                    "where PLAYER_ID = ? and NAME = ?";
                PreparedStatement stmt = conn.prepareStatement(ssql);
                try {
                    stmt.setBytes(1, toByteArray(look.articles));
                    stmt.setInt(2, playerId);
                    stmt.setString(3, look.name);
                    JDBCUtil.warnedUpdate(stmt, 1);
                } finally {
                    JDBCUtil.close(stmt);
                }
                return null;
            }
        });
    }

    /**
     * Deletes a particular look owned by a particular player.
     */
    public void deleteLook (int playerId, String name)
        throws PersistenceException
    {
        int mods = update("delete from LOOKS where PLAYER_ID = " + playerId +
                          " and LOOK = " + JDBCUtil.escape(name));
        if (mods != 1) {
            log.warning("Unable to delete look [pid=" + playerId +
                        ", name=" + name + ", mods=" + mods + "].");
        }
    }

    /** Helper function for {@link #loadLooks}. */
    protected Look decodeLook (ResultSet rs)
        throws SQLException
    {
        Look look = new Look();
        look.name = rs.getString(1);
        look.aspects = fromByteArray(rs.getBytes(2));
        look.articles = fromByteArray(rs.getBytes(3));
        return look;
    }

    /** Helper function for {@link #insertLook}. */
    protected byte[] toByteArray (int[] array)
    {
        ByteBuffer adata = ByteBuffer.allocate(array.length*4);
        adata.asIntBuffer().put(array);
        return adata.array();
    }

    /** Helper function for {@link #decodeLook}. */
    protected int[] fromByteArray (byte[] array)
    {
        IntBuffer ints = ByteBuffer.wrap(array).asIntBuffer();
        int[] iarray = new int[ints.remaining()];
        ints.get(iarray);
        return iarray;
    }
}
