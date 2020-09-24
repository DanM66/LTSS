package lcm.ltss;

import java.sql.Array;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DecimalFormat;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Extracts pixels from a raster where n is the number of bands and the nth band
 * is non-zero. The input raster for this n-1 bands of information; the nth band
 * is the training layer.
 * 
 * @author danm
 *
 */
public class ExtractTrainingPixels
{

    static int processcount = 0;
    static int nrid;

    /**
     * 
     * @param args
     */
    public static void main(String[] args)
    {

        try
        {
            (new ExtractTrainingPixels()).run(args);
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }
    }

    /**
     * 
     * @param args
     * @throws SQLException
     */
    public void run(String[] args) throws SQLException
    {
        String user = null, url = null, password = null;
        String theRaster = null, output = null;
        int nThreads = 50, nodata = -9999;

        for (int ii = 0; ii < args.length; ++ii)
        {
            System.out.println(args[ii]);

            if ("-db".equals(args[ii]))
                url = args[ii + 1];

            if ("-user".equals(args[ii]))
                user = args[ii + 1];

            if ("-password".equals(args[ii]))
                password = args[ii + 1];

            if ("-r".equals(args[ii]))
                theRaster = args[ii + 1];

            if ("-o".equals(args[ii]))
                output = args[ii + 1];

            if ("-threads".equals(args[ii]))
                nThreads = Integer.parseInt(args[ii + 1]);

            if ("-nodata".equals(args[ii]))
                nodata = Integer.parseInt(args[ii + 1]);

        }

        if (output == null)
            output = theRaster + "_train";

        Connection con = null;
        String sqlString = null;

        Statement st1 = null;

        ResultSet rs1 = null;
        try
        {
            // Initialise the connection pool and get a connection
            con = ConnectionPool.getConnection(url, user, password);

            st1 = con.createStatement();

            sqlString = "select count(*) from " + theRaster;
            rs1 = st1.executeQuery(sqlString);

            rs1.next(); // There is only one result.
            nrid = rs1.getInt(1);
            rs1.close();

            sqlString = "drop table if exists " + output + "; create table " + output + "(  rec text, class integer);";
            st1.execute(sqlString);

            System.out.println(sqlString);

            sqlString = "select r.rid, st_numbands(r.rast), st_width(r.rast), st_height(r.rast) from " + theRaster
                    + " r order by r.rid";
            rs1 = st1.executeQuery(sqlString);

            ExecutorService executor = Executors.newFixedThreadPool(nThreads);
            while (rs1.next())
            {
                int rid = rs1.getInt(1);
                int nbands = rs1.getInt(2);
                int width = rs1.getInt(3);
                int height = rs1.getInt(4);

                Runnable worker = new TileProcessor(theRaster, output, rid, nbands, width, height, nodata);
                executor.execute(worker);

            }
            executor.shutdown();
            while (!executor.isTerminated())
            {

            }

        }
        catch (SQLException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        finally
        {
            if (st1 != null)
                st1.close();
            if (rs1 != null)
                rs1.close();
            if (con != null)
                con.close();
        }
    }

    /**
     * 
     * @param d
     * @param table
     * @return the SqlString
     */
    private static String getSqlString(double[] d, String table)
    {
        int _class = (int) d[d.length - 1];
        StringBuffer strBuff = new StringBuffer();
        strBuff.append("" + (int) d[0]);
        for (int ii = 1; ii < d.length; ++ii)
            strBuff.append("," + ((int) d[ii]));
        String str = "('" + strBuff + "'," + _class + ")";

        return str;
    }

    static class TileProcessor implements Runnable
    {
        // Connection mCon;
        int mNbands, mWidth, mHeight;
        int mRid;
        String mRaster;
        String mOutput;
        int mNoData;

        DecimalFormat df = new DecimalFormat();

        TileProcessor(String raster, String output, int rid, int nbands, int width, int height, int nodata)
        {
            mNbands = nbands;
            mWidth = width;
            mHeight = height;
            mRid = rid;
            mRaster = raster;
            mOutput = output;
            mNoData = nodata;

            df.setMaximumFractionDigits(2);
            df.setMinimumFractionDigits(2);

        }

        public void run()
        {
            Connection con = null;
            Statement st = null;
            ResultSet rs = null;
            String sqlString = null;

            Double[][][] rast = new Double[mNbands][mWidth][mHeight];

            try
            {
                con = ConnectionPool.getConnection();
                st = con.createStatement();
                sqlString = "select (st_dumpvalues(rast)).* from " + mRaster + " where rid = " + mRid;

                rs = st.executeQuery(sqlString);

                int count = 0;
                while (rs.next())
                {
                    Array ddd = rs.getArray(2);
                    Double[][] values = (Double[][]) ddd.getArray();

                    rast[count] = values;
                    count++;
                }

                double[] pixel = new double[mNbands];

                StringBuffer str = new StringBuffer("insert into " + mOutput + "(rec, class) values");

                int data = 0;

                boolean first = true;
                for (int ii = 0; ii < mWidth; ++ii)
                {
                    for (int jj = 0; jj < mHeight; ++jj)
                    {

                        for (int kk = 0; kk < mNbands; ++kk)
                        {
                            Double v = rast[kk][jj][ii];

                            try
                            {
                                pixel[kk] = v.doubleValue();
                            }
                            catch (Exception e)
                            {
                                pixel[kk] = 0.0;
                            }

                        }

                        double eps = 1e-8;
                        if (pixel[mNbands - 1] > eps && pixel[0] > (mNoData + eps))
                        {
                            data++;

                            if (first)
                            {
                                str.append(getSqlString(pixel, mOutput));
                                first = false;
                            }
                            else
                            {
                                str.append("," + getSqlString(pixel, mOutput));
                            }

                        }
                    }
                }

                if (data > 0)
                {

                    str.append(";");

                    try
                    {
                        st.executeUpdate(str.toString());

                        // mCon.commit();
                    }
                    catch (SQLException e)
                    {
                        System.out.println(mRid + ":" + str.toString());
                        throw e;
                    }

                }

            }
            catch (SQLException e1)
            {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }
            finally
            {
                try
                {
                    if (rs != null)
                        rs.close();
                    if (st != null)
                        st.close();
                    if (con != null)
                        con.close();
                }
                catch (SQLException e)
                {
                    e.printStackTrace();
                }
            }
            System.out.print("Build training " + df.format((100.0 * ++processcount / nrid)) + "% complete\r");
        }

    }
}
