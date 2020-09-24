package lcm.ltss;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;

public class DbTileWriter implements TileWriter
{

    public DbTileWriter() throws SQLException
    {

        String outRaster = ClassificationConfig.getInstance().generateOutName();
        String inRaster = ClassificationConfig.getInstance().getRaster();
        int nBands = ClassificationConfig.getInstance().getNumOutBands();

        if (Utils.relationExists(outRaster))
        {
           Utils.drop(outRaster);
        }
        Utils.createRasterFromTemplate(outRaster, inRaster, nBands);

    }

    public void writeTile(Tile t) throws SQLException
    {
        int band = 0;
        
        Number[][][] d = t.getTile();
        //The outile has a probability for each class and the most likely class
        int start = ClassificationConfig.getInstance().isDetail() ? 0: t.getNumBands() - 2;
        int stop = d.length;

        
        for (int ii = start; ii < stop; ++ii)
        {
            ++band;
            
            StringBuffer strBuff = new StringBuffer();
            strBuff.append("[ " + Arrays.toString(d[ii][0]));
            for (int jj = 1; jj < d[0].length; ++jj)
            {
                strBuff.append("," + Arrays.toString(d[ii][jj]));
            }
            strBuff.append("]");
            
            String sqlStr = "update " + ClassificationConfig.getInstance().generateOutName()
                    + " set rast = st_setvalues(rast, "
                    + band + ", 1, 1, ARRAY" + strBuff.toString().replaceAll("null", "NULL")
                    + "::int[][]) where rid = " + t.getId();
            
            Connection con = null;
            Statement st = null;
            
            try
            {
                con = ConnectionPool.getConnection();
                st = con.createStatement();
                st.execute(sqlStr);
            }
            finally
            {
                if (st!=null) st.close();
                if (con!=null) con.close();
            }
        }
        //System.out.println("Tile written:" + t.getId());
    }
}
