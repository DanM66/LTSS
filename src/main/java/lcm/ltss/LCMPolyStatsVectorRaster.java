package lcm.ltss;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * This application does a vector raster intersection and accumulates
 * classification information. It is much slower than the raster*raster version
 * and there are some minor problems with how postgis intersects skinny polygons
 * with a raster. The problem is extremely rare (~1/million).
 * 
 * @author danm
 *
 */
public class LCMPolyStatsVectorRaster
{

    private static final LCMPolyStatsVectorRaster lr = new LCMPolyStatsVectorRaster();

    private static LCMPolyStatsVectorRaster getInstance()
    {
        return lr;
    }

    private static final class MyWorker implements Runnable
    {
        String sql = null;
        String outTable = null;
        int gid;

        MyWorker(String sqlStr, String outTable, int gid)
        {
            this.sql = sqlStr;
            this.gid = gid;
            this.outTable = outTable;
        }

        private String histogram(Number[][] n)
        {
            int count[] = new int[21];

            for (int ii = 0; ii < n.length; ++ii)
            {
                for (int jj = 0; jj < n[ii].length; ++jj)
                {
                    if (n[ii][jj] != null)
                    {
                        int index = (n[ii][jj].intValue());
                        count[index - 1]++;
                    }
                }
            }

            StringBuffer strBuff = new StringBuffer();

            boolean b = false;
            int max = -1;
            int mode = -1;
            int sigma = 0;

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
                    max = nc > max ? nc : max;
                    if (b)
                    {
                        strBuff.append(",");
                    }
                    b = true;
                    strBuff.append((ii + 1) + ":" + count[ii]);

                }
            }

            return "'" + strBuff.toString() + "', " + mode + ", " + String.format("%.2f", (max * 1.0) / (sigma * 1.0))
                    + "";
        }

        private String confidence(Number[][] n)
        {
            RunningStat rs = new RunningStat();

            for (int ii = 0; ii < n.length; ++ii)
            {
                for (int jj = 0; jj < n[ii].length; ++jj)
                {
                    if (n[ii][jj] != null)
                    {
                        rs.push(n[ii][jj].doubleValue());
                    }
                }
            }

            return String.format("%.2f", rs.mean()) + ", " + String.format("%.2f", rs.standardDeviationPopulation()) + ","
                    + rs.numDataValues();
        }

        public void run()
        {
            Connection con = null;
            Statement st = null;
            Statement st2 = null;
            ResultSet rs = null;
            String insert = "insert into %s(gid, _hist, _mode, _purity, _conf, _stdev, _n) select %s, %s, %s;";
            // insert = String.format(insert, this.outTable, "" + this.gid);

            try
            {
                con = ConnectionPool.getConnection();
                st = con.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
                st2 = con.createStatement();
                rs = st.executeQuery(sql);
                // System.out.println(sql);
                String histo = null;
                String stat = null;

                // Note this will only work with a raster union.
                while (rs.next())
                {
                    int band = rs.getInt(1);
                    // System.out.println("band = " + band);
                    Number[][] b = (Number[][]) (rs.getArray(2)).getArray();

                    switch (band)
                    {
                    case 1:
                        histo = histogram(b);
                        break;
                    case 2:
                        stat = confidence(b);
                        break;
                    }
                }
                insert = String.format(insert, this.outTable, "" + this.gid, histo, stat);
                System.out.println(insert);
                if (histo != null && stat != null) st.execute(insert);

            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
            finally
            {
                try
                {
                    con.close();
                    st.close();
                    st2.close();
                    rs.close();
                }
                catch (Exception e)
                {

                }
            }
        }

    }

    public static void main(String[] args)
    {
        getInstance().run(args);
    }

    public void run(String[] args)
    {
        String raster = "c_osgrid2019sp_1lcm2015_99_v3osgrid2019sp_10000randomforest";
        String vector = "spatial_fw";
        String out = raster + "_vec";
        double buffer = 0;
        int nThreads = 50;
        boolean debug = false;

        for (int ii = 0; ii < args.length; ++ii)
        {
            if ("-vector".equals(args[ii])) vector = args[ii + 1];

            if ("-raster".equals(args[ii])) raster = args[ii + 1];

            if ("-nthreads".equals(args[ii])) nThreads = Integer.parseInt(args[ii + 1]);

            if ("-out".equals(args[ii])) out = args[ii + 1];

            if ("-buffer".equals(args[ii])) buffer = Double.parseDouble(args[ii + 1]);

            if ("-debug".equals(args[ii])) debug = true;

        }

        String sql = null;
        Connection con = null;
        Statement st = null;
        // Statement st2 = null;
        ResultSet rs = null;
        ExecutorService executor = Executors.newFixedThreadPool(nThreads);

        try
        {

            con = ConnectionPool.getConnection("jdbc:postgresql://la-lcm:5432/LCM2019", "postgres", "FireTrail1066");
            st = con.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);

            sql = "select distinct b.gid from " + raster + " a, " + vector + " b where st_intersects(a.geom, b.geom);";
            rs = st.executeQuery(sql);

            while (rs.next())
            {
                int gid = rs.getInt(1);
                String s1 = "select (st_dumpvalues(st_union(st_clip(a.rast, st_buffer(b.geom,%s))))).* from %s a, %s b where st_intersects(a.rast, b.geom) and b.gid = %s";
                s1 = String.format(s1, "" + buffer, raster, vector, "" + gid);
                Runnable worker = new MyWorker(s1, out, gid);
           
                if (debug)
                    worker.run();
                else
                    executor.execute(worker);
        
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
                con.close();
                st.close();
            }
            catch (Exception e)
            {
            }
        }
        executor.shutdown();
        while (!executor.isTerminated())
        {
        }
    }

}
