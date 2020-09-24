package lcm.ltss;

import java.sql.Array;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Set;

public class RasterTile extends Tile
{
    Set<Integer> mFilter = null;
    WekaClassifier mWc = null;

    /**
     * Constructs a tile based on a postgis raster
     * 
     * @param c
     * @param rid
     * @param raster
     * @param nbands
     * @param width
     * @param height
     * @throws SQLException
     */
    RasterTile(int rid, String raster, Set<Integer> filter, WekaClassifier wc) throws SQLException
    {
        this.mRid = rid;
        this.mFilter = filter;
        this.mWc = wc;
        
        
        Connection c = null;

        try
        {
            c = ConnectionPool.getConnection();
            mTile = setTile(c, raster, rid);
        }
        finally
        {
            if (c != null)
                c.close();
        }

    }

    private Number[][][] setTile(Connection c, String rname, int rid) throws SQLException
    {

        Statement st = null;
        ResultSet rs = null;
        Number[][][] tile;
        try
        {
              
            tile = new Number[mWc.getTrainingData().numAttributes()-1][][];

            String queryString = "select (st_dumpvalues(rast)).* from " + rname + " where rid = " + rid;
            st = c.createStatement();
            rs = st.executeQuery(queryString);

            int count = 0;

            // If training observations are embedded as the last band in the raster we need to ignore it.
            while (rs.next() && count < mWc.getTrainingData().numAttributes()-1)
            {
                int band = rs.getInt(1);
       
                if (mFilter != null)
                {
                    if (mFilter.contains(band)) 
                        continue;
                }
                
                Array a = rs.getArray(2);
               
                tile[count++] = (Number[][]) a.getArray();
            }

        }
        finally
        {
            if (st != null)
                st.close();
            if (rs != null)
                rs.close();
        }

        return tile;
    }

    public static void main(String[] args) throws SQLException
    {

    }

}
