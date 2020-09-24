package lcm.ltss;

import java.sql.SQLException;

public class Tile
{
    int mRid;

    Number[][][] mTile = null;
    TileWriter mTw = null;

    public Tile()
    {
    }

    public Tile(int rid, int nbands)
    {
        this.mRid = rid;
        mTile = new Number[nbands][][];
    }

    public Tile(int rid, int nbands, int x, int y)
    {
        this.mRid = rid;
        mTile = new Number[nbands][x][y];
    }
    
    public int getNumBands()
    {
        return mTile.length;
    }
    
    public int getWidth()
    {
        return mTile[0].length;    
    }
    
    public int getHeight()
    {
        return mTile[0][0].length;    
    }

    /**
     * Gets the tile as a 3d Number array nbands, x, y
     * 
     * @return
     */
    public Number[][][] getTile()
    {
        return mTile;
    }

    /**
     * Gets the nband pixel at a given location within a raster tile. Note that
     * indices start at zero. Returns null if any band has a null value.
     * 
     * @param x
     *            index
     * @param y
     *            index
     * @return
     */
    public Number[] getCell(int x, int y)
    {
        if (mTile == null)
            return null;

        int nbands = mTile.length;

        Number[] result = new Number[nbands];

        for (int ii = 0; ii < nbands; ++ii)
        {
            result[ii] = mTile[ii][x][y];
        }

        return result;
    }
    
 
    /**
     * Sets the value of each band of a given pixel within a tile
     * 
     * @param n
     * @param x
     * @param y
     */
    public void setPixel(Number[] n, int x, int y)
    {
        for (int ii = 0; ii < n.length; ++ii)
        {
            mTile[ii][x][y] = n[ii];
        }
    }
    
    public int getId()
    {
        return this.mRid;
    }

    /**
     * Sets a given band of a tile
     * 
     * @param band
     * @param bandnum
     */
    public void setBand(Number[][] band, int bandnum)
    {
        mTile[bandnum] = band;
    }
    

    public String toString()
    {
        if (mTile == null)
            return null;
        
        String newline = System.lineSeparator();

        StringBuffer strBuff = new StringBuffer();

        for (int ii = 0; ii < mTile.length; ++ii)
        {
            strBuff.append("band = " + (ii+1));
            strBuff.append(newline);

            for (int jj = 0; jj < mTile[ii].length; ++jj)
            {
                for (int kk = 0; kk < mTile[ii][jj].length; ++kk)
                {
                    Number n = mTile[ii][jj][kk];
                    n = (n == null) ? n : n.shortValue();
                    strBuff.append("" + n + " ");
                }
                strBuff.append(newline);
            }
        }

        return strBuff.toString();
    }

    public static void main(String[] args) throws SQLException
    {
        Tile t = new Tile(1, 7, 3, 3);
        
       System.out.println(t.toString());

    }

}
