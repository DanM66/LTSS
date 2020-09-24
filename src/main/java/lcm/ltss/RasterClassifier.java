package lcm.ltss;

import java.net.InetAddress;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Designed to work with an ensemble of classifiers.
 * 
 * @author danm
 *
 */
public class RasterClassifier
{
    private static final RasterClassifier rc = new RasterClassifier();
    private static int mCount = 0;
    private static int mNRows = 0;
    
    public static RasterClassifier getInstance()
    {
        return rc;
    }

    private RasterClassifier() {}

    private static final class TileClassifier implements Runnable
    {
        private int mTileId;
        private WekaClassifier mWk = null;
        private boolean mDetail = false;
        private String mRaster = null;
        private TileWriter mTw = null;
        private Set<Integer> mFilter = null;

        private DecimalFormat df = null;

        /**
         * 
         * @param t
         * @throws SQLException
         */
        TileClassifier(int tileId, String raster, WekaClassifier wk, TileWriter tw, Set<Integer> filter, boolean detail)
                throws SQLException
        {
            mTileId = tileId;
            mWk = wk;
            mDetail = detail;
            mRaster = raster;
            mTw = tw;
            mFilter = filter;
            
            df = new DecimalFormat();
            df.setMaximumFractionDigits(2);
            df.setMinimumFractionDigits(2);
        }

        public Tile classify() throws Exception
        {
            int nbands = 2;
            if (mDetail)
            {
                nbands = mWk.getClasses().length + 1;
            }

            Tile inTile = new RasterTile(mTileId, mRaster, mFilter, mWk);

            Tile outTile = new Tile(mTileId, nbands, inTile.getWidth(), inTile.getHeight());

            for (int ii = 0; ii < inTile.getWidth(); ++ii)
            {
                for (int jj = 0; jj < inTile.getHeight(); ++jj)
                {
                    Number[] n = inTile.getCell(ii, jj);
                    if (checkNullValues(n))
                        continue;
                    setPixel(mWk.getDistribution(n), mWk.getClasses(), outTile, ii, jj);
                }
            }

            return outTile;
        }

        /**
         * Checks for null values in a number array, returns true if any are found.
         * 
         * @param n
         * @return
         */
        private boolean checkNullValues(Number[] n)
        {
            for (Number x : n)
            {
                if (x == null)
                    return true;
            }

            return false;
        }

        /**
         * Sets the pixels for each band at location x y
         * 
         * @param v
         * @param t
         * @param x
         * @param y
         */
        private void setPixel(double[] v, short[] classes, Tile t, int x, int y)
        {

            int _class = -999;
            double maxValue = -1.0;
            for (int ii = 0; ii < v.length; ++ii)
            {
                if (v[ii] > maxValue)
                {
                    _class = ii;
                    maxValue = v[ii];
                }
            }

            Number[] n = new Number[(t.getCell(x, y)).length];

            n[0] = classes[_class];

            if (n.length > 2)
            {
                for (int ii = 0; ii < v.length; ++ii)
                {
                    n[ii + 1] = ((Number) (v[ii] * 100)).shortValue();
                }
            }
            else
            {
                n[1] = ((Number) (maxValue * 100)).shortValue();
            }

            t.setPixel(n, x, y);
        }

        /**
         * 
         */
        public void run()
        {
            try
            {
                Tile t = classify();
                if (mTw != null)
                {
                    mTw.writeTile(t);
                   
                }
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
            System.out.print("Classification " + df.format((100.0 * ++mCount / mNRows)) + "% complete\r");
        }

    }

    public void run(String[] args) throws Exception
    {

        ClassificationConfig c = ClassificationConfig.getInstance();
        boolean debug = false;

        // Defaults
        c.setLogTableName("classification_details");

        for (int ii = 0; ii < args.length; ++ii)
        {
            if ("-v".equals(args[ii]))
                c.setVerbose(true);

            if ("-cv".equals(args[ii]))
                c.setFolds(args[ii + 1]);

            if ("-d".equals(args[ii]))
                c.setDetail(true);

            if ("-W".equals(args[ii]))
                c.setWekas(args[ii + 1]);

            if ("-arff".equals(args[ii]))
                c.setArff(args[ii + 1]);

            if ("-db".equals(args[ii]))
                c.setUrl(args[ii + 1]);

            if ("-user".equals(args[ii]))
                c.setUser(args[ii + 1]);

            if ("-r".equals(args[ii]))
                c.setRaster(args[ii + 1]);

            if ("-m".equals(args[ii]))
                c.setMask(args[ii + 1]);

            if ("-password".equals(args[ii]))
                c.setPassword(args[ii + 1]);

            if ("-o".equals(args[ii]))
                c.setOutput(args[ii + 1]);

            if ("-filter".equals(args[ii]))
                c.setFilter(args[ii + 1]);

            if ("-debug".equals(args[ii]))
                debug = true;

            if ("-threads".equals(args[ii]))
                c.setThreads(args[ii + 1]);

            if ("-log".equals(args[ii]))
                c.setLoggers(args[ii + 1]);

            if ("-logtable".equals(args[ii]))
                c.setLogTableName(args[ii + 1]);

            if ("-id".equals(args[ii]))
                c.setId(args[ii + 1]);
        }

        if (c.isVerbose())
        {
            for (String str : args)
                System.out.println(str);
        }

        String[] classifiers = c.getWekaOptions().split(":");
        // WekaClassifier[] wk = new WekaClassifier[classifiers.length];

        for (int ii = 0; ii < classifiers.length; ++ii)
        {
            LoggingDetail log = new LoggingDetail();
            log.setLoggers(LoggingFactory.getLoggers(c.getLoggers()));

            log.setIpAddress(InetAddress.getLocalHost().toString());
            log.setThreads("" + c.getThreads());
            DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
            Calendar cal = Calendar.getInstance();
            String date = dateFormat.format(cal.getTime());
            log.setDate(date);
            log.setArgs(args);

            {     
              
                WekaClassifier wc = new WekaClassifier(c.getArff(), classifiers[ii], c.getFilter());
                c.setCurrentWeka(wc);
                String outName = c.generateOutName();
                log.setId(outName); // Have to set here because of count hack
                System.out.println(outName);
                // str = t.getElapsedSeconds("Finished training" + classifiers[ii]);
                Timer t = new Timer();
                String str = t.start("Training classifier: " + classifiers[ii]);
                System.out.println(str);
                wc.train();
                int trainingTime = (int) (t.getElapsed() / 1000);
                System.out.println(
                        "Finished training classifier " + classifiers[ii] + " in " + trainingTime + " seconds.");
                log.setTrainingTime(trainingTime);
                
                log.setClassifierDescription(c.getCurrentWeka().getDescription());
            }

            Connection con = null;
            Statement st = null;
            ResultSet rs = null;

            try
            {
                con = ConnectionPool.getConnection(c.getUrl(), c.getUser(), c.getPassword());

                String queryString = "select distinct rid from " + c.getRaster();
                
                if (c.getMask() != null)
                {
                	queryString += "," + c.getMask() + " where st_intersects(rast, geom) ";
                }
                queryString +=       " order by rid asc";

                st = con.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
                rs = st.executeQuery(queryString);
              
                //Determine the number of rows
                rs.last();
                mNRows = rs.getRow();
                rs.beforeFirst();
                System.out.println("nrows = " + mNRows);

                Timer t = new Timer();

                System.out.println(t.start("classifying"));
                TileWriter tw = WriterFactory.getWriter(c.getOutput());
                ExecutorService executor = Executors.newFixedThreadPool(c.getThreads());
                
                
                //for (int jj = 0; rs.next(); ++jj)
                while (rs.next())
                {
                    int tileId = rs.getInt(1);
                    Runnable worker = new TileClassifier(tileId, c.getRaster(), c.getCurrentWeka(), tw,
                            c.getFilterSet(), c.isDetail());

                    if (debug)
                        worker.run();
                    else
                        executor.execute(worker);
                    
                    //double pCentComplete = (jj*1.0/count*1.0);
                    //System.out.println("pcent complete = " + pCentComplete);

                }
                executor.shutdown();
                while (!executor.isTerminated())
                {
                }

                int classificationTime = (int) t.getElapsed() / 1000;
                log.setClassificationTime(classificationTime);
                System.out.println(
                        "Finished classifying with " + classifiers[ii] + " in " + classificationTime + " seconds.");

            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
            finally
            {
                if (rs != null)
                    rs.close();
                if (st != null)
                    st.close();
                if (con != null)
                    con.close();

            }

            if (c.getFolds() > 1)
            {

                Timer t = new Timer();
                System.out.println(t.start("CrossValidation with: " + classifiers[ii]));
                System.out.println("Performing Crossvalidation with " + c.getFolds() + " folds");
                CrossValidation cv = new CrossValidation(c.getCurrentWeka().getTrainingData(),
                        c.getCurrentWeka().getClassifier(), c.getCurrentWeka().getSeed(), c.getFolds());
                log.setCrossValidationDetails(cv.getClassificationDetails());
                log.setCrossValidationSummary(cv.getCVSummary());
                log.setConfusionMatrix(cv.getMatrix());
                log.setCrossValidationKappa(cv.getKappa());
                log.setCrossValidaitonPercentCorrect(cv.getPCTCorrect());
                int crossValidationTime = (int) (t.getElapsed() / 1000);
                log.setCrossValidationTime(crossValidationTime);
                System.out.println(
                        "Finished crossvalidation of " + classifiers[ii] + " in " + crossValidationTime + " seconds.");

            }

            log.writeLog();
        }

    }

    public static void main(final String[] args) throws Exception
    {
        RasterClassifier.getInstance().run(args);
    }

}
