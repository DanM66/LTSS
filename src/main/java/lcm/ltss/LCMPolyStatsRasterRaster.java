package lcm.ltss;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * This application takes a three band raster to produce polygon statistics. It
 * is several thousand times faster than a polygon intersection. Band 1 is the
 * most likely land cover class. Band 2 is the probability associated with this
 * class. Band 3 is from a rasterized vector spatial framework. It's pixel value
 * is the unique parcel identifier. This application will accumulate
 * classification information per parcel id. These data are stored in a user
 * specified table with parcel id as the key.
 * 
 * 
 * @author danm
 *
 */
public class LCMPolyStatsRasterRaster
{

    private static final LCMPolyStatsRasterRaster pr = new LCMPolyStatsRasterRaster();

    // Thread-safe version of HashMap.
    private ConcurrentHashMap<Number, Detail> map = new ConcurrentHashMap<Number, Detail>();

    private static LCMPolyStatsRasterRaster getInstance()
    {
        return pr;
    }

    private static final class MyWorker implements Runnable
    {
        String sql = null;
        Number[][][] rast = null;
        final int nBands = 3;
        int nClasses = 21;

        private void push(Number lc, Number conf, Number id)
        {
            try
            {
                int landcover = lc.intValue();
                int confidence = conf.intValue();
                id.intValue();

                if (!getInstance().map.containsKey(id)) getInstance().map.put(id, new Detail(id, nClasses));
                Detail d = getInstance().map.get(id);
                d.push(landcover, confidence);

            }
            catch (Exception e)
            {
                System.out.println("" + lc + "," + conf + "," + id);
            }

        }

        private void update(Number[][][] n)
        {
            for (int ii = 0; ii < n[0].length; ++ii)
            {
                for (int jj = 0; jj < n[0][0].length; ++jj)
                {
                    if (n[2][ii][jj] == null) continue;
                    push(n[0][ii][jj], n[1][ii][jj], n[2][ii][jj]);
                }
            }
        }

        MyWorker(String sql, int nclass)
        {
            this.sql = sql;
            rast = new Number[nBands][][];
            this.nClasses = nclass;

        }
        
       

        public void run()
        {
            Connection con = null;
            Statement st = null;
            ResultSet rs = null;
            try
            {
                con = ConnectionPool.getConnection();
                st = con.createStatement();
                rs = st.executeQuery(this.sql);

                while (rs.next())
                {
                    int band = rs.getInt(1);
                    Number[][] b = (Number[][]) (rs.getArray(2)).getArray();
                    rast[band - 1] = b;
                }

                update(rast);

            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
            finally
            {
                try
                {
                    rs.close();
                    st.close();
                    con.close();
                }
                catch (Exception e)
                {

                }
            }
            System.out.println(sql);
        }

    }

    private void run(String[] args)
    {
        Connection con = null;
        Statement st = null;
        ResultSet rs = null;
        int nThreads = 50;
        int nClasses = 21;
        String out = null;
        String raster = null;
        String spatial_fw = "spatial_fw";
        boolean debug = false;
        int commit = 20000;

        for (int ii = 0; ii < args.length; ++ii)
        {
            if ("-out".equals(args[ii])) out = args[ii + 1];

            if ("-spf".equals(args[ii])) spatial_fw = args[ii + 1];

            if ("-raster".equals(args[ii])) raster = args[ii + 1];

            if ("-nthreads".equals(args[ii])) nThreads = Integer.parseInt(args[ii + 1]);
            
            if ("-nclass".equals(args[ii])) nClasses = Integer.parseInt(args[ii + 1]);
            
            if ("-commit".equals(args[ii])) commit = Integer.parseInt(args[ii + 1]);

            if ("-debug".equals(args[ii])) debug = true;
        }

        if (raster == null)
        {
            System.err.println("No raster input specified");
            return;
        }

        if (out == null)
        {
            out = raster + "_vec";
        }

        ExecutorService executor = Executors.newFixedThreadPool(nThreads);

        try
        {
            con = ConnectionPool.getConnection("jdbc:postgresql://la-lcm:5432/LCM2019", "postgres", "FireTrail1066");
            st = con.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);

            String sql = "drop table if exists " + out + " cascade;";
            st.execute(sql);

            sql = "create table " + out
                    + "(gid integer, _hist text, _mode integer, _purity double precision, _conf double precision, _stdev double precision, _n integer);";

            st.execute(sql);

            sql = "select rid from " + raster + ";";

            rs = st.executeQuery(sql);
            sql = "select (st_dumpvalues(rast)).* from " + raster + " where rid = %s";
            while (rs.next())
            {
                int rid = rs.getInt(1);
                String str = String.format(sql, rid);
                Runnable worker = new MyWorker(str, nClasses);

                if (debug)
                    worker.run();
                else
                    executor.execute(worker);

            }
            executor.shutdown();
            while (!executor.isTerminated())
            {
            }

            int count = 0;
            StringBuilder strBuild = new StringBuilder();
            
           
            for (Number key : getInstance().map.keySet())
            {
                count++;
                // insert
                sql = "insert into " + out + "(gid, _hist, _mode, _purity, _conf, _stdev, _n) select "
                        + map.get(key).toString() + ";";
                System.out.println(sql);

                strBuild.append(sql);

                if (count == commit)
                {
                    st.execute(strBuild.toString());
                    strBuild.setLength(0);
                    count = 0;
                }
            }

            if (count > 0)
            {
                st.execute(strBuild.toString());
                //System.out.println("I'm done :), count = " + count);
            }

            // Add an index
            sql = "create index idx_" + out + "_gid on " + out + "(gid);";
            st.execute(sql);

            // create view
            if (spatial_fw != null)
            {
                String view = out + "_view";
                sql = "drop view if exists " + view;
                st.execute(sql);
                sql = "create view " + view + " as select a.*, b.geom from " + out + " a, " + spatial_fw
                        + " b where a.gid = b.gid;";
                st.execute(sql);
            }

        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        finally
        {
            try
            {
                rs.close();
                st.close();
                con.close();
            }
            catch (Exception e)
            {
            }
        }

    }

    public static void main(String[] args)
    {
        getInstance().run(args);
    }

}

class Detail
{
    Number id = null;
    int max = 0, mode = -1;
    String hist = null;
    double purity;
    int[] count = null;

    Detail(Number n, int nclasses)
    {
        this.id = n;
        count = new int[nclasses];
    };

    RunningStat rs = new RunningStat();

    void push(int lc, int p)
    {
        rs.push(p);
        count[lc - 1]++;
    }

    private void setDetails()
    {

        int sigma = 0;
        boolean f = false;
        StringBuffer strBuff = new StringBuffer();

        for (int ii = 0; ii < count.length; ++ii)
        {
            int nc = count[ii];
            sigma += nc;
            if (nc > 0)
            {
                if (nc > max)
                {
                    max = nc;
                    mode = ii + 1;
                }

                if (f)
                {
                    strBuff.append(",");
                }
                f = true;
                strBuff.append((ii + 1) + ":" + count[ii]);

            }
        }
        hist = strBuff.toString();
        purity = max * 1.0 / sigma;
    }

    public String toString()
    {
        setDetails();
        String str = "" + id.intValue() + ", '" + hist + "', " + mode + "," + String.format("%.2f", purity) + ", "
                + String.format("%.2f", rs.mean()) + ", " + String.format("%.2f", rs.standardDeviationPopulation()) + ", "
                + rs.numDataValues();
        return str;
    }
}
