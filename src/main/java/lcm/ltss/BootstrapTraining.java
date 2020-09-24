package lcm.ltss;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DecimalFormat;
import java.util.Random;

/**
 * 
 * @author danm
 *
 */
public class BootstrapTraining
{

    static String url = null;
    static String user = null;
    static String password = null;
    static int nclasses = -1;
    static DecimalFormat df = null;
    static int[] classIds;

    public BootstrapTraining()
    {
        df = new DecimalFormat();
        df.setMaximumFractionDigits(2);
        df.setMinimumFractionDigits(2);
    }

    public static void main(String[] args) throws FileNotFoundException, UnsupportedEncodingException, SQLException
    {
        (new BootstrapTraining()).run(args);
    }

    private void run(String[] args) throws SQLException, FileNotFoundException, UnsupportedEncodingException
    {

        String tableName = "training_data";
        int n = 10000;
        int seed = -999;
        boolean verbose = false;
        String classList = null;

        for (int ii = 0; ii < args.length; ++ii)
        {
            if ("-t".equals(args[ii]))
                tableName = args[ii + 1];

            if ("-n".equals(args[ii]))
                n = Integer.parseInt(args[ii + 1]);

            if ("-s".equals(args[ii]))
                seed = Integer.parseInt(args[ii + 1]);

            if ("-nc".equals(args[ii]))
                nclasses = Integer.parseInt(args[ii + 1]);

            if ("-user".equals(args[ii]))
                user = args[ii + 1];

            if ("-url".equals(args[ii]))
                url = args[ii + 1];

            if ("-password".equals(args[ii]))
                password = args[ii + 1];

            if ("-v".equals(args[ii]))
                verbose = true;

            if ("-classlist".equals(args[ii]))
                classList = args[ii + 1];
        }

        classIds = new int[nclasses];
        for (int ii = 0; ii < nclasses; ++ii)
            classIds[ii] = ii + 1;

        // hacky hack hack
        if (classList != null)
        {
            String[] strarray = classList.split(",");
            if (strarray.length != nclasses)
            {
                System.out.println("Error, check inputs");
                return;
            }

            for (int ii = 0; ii < nclasses; ++ii)
                classIds[ii] = Integer.parseInt(strarray[ii]);
        }

        Connection con = null;
        Statement st = null;
        ResultSet rs = null;
        PrintWriter pw = null;
        try
        {
            con = ConnectionPool.getConnection(url, user, password);
                                                                        

            String str = "create temp sequence temp_training_seq";
            st = con.createStatement();
            st.execute(str);

            str = "CREATE temp TABLE temp_training("
                    + "id bigint NOT NULL DEFAULT nextval('temp_training_seq'::regclass)," + "rec text,"
                    + "CONSTRAINT temp_training_pkey PRIMARY KEY (id));create index on temp_training (id)";
            st.execute(str);

            pw = new PrintWriter(tableName + "_" + n + ".arff", "UTF-8");
            pw.println("@RELATION landcover\n");

            boolean attributes = false;

            for (int ii = 0; ii < nclasses; ++ii)
            {

                str = "insert into temp_training(rec) select rec from " + tableName + " where class = " + classIds[ii];
                st.execute(str);

                str = "select count(*) from temp_training";
                rs = st.executeQuery(str);

                int count = 0;
                while (rs.next())
                {
                    count = rs.getInt(1);
                }
               
                rs.close();

                if (count > 0)
                {
                    Random r = new Random();
                    if (seed != -999)
                        r.setSeed(seed);

                    for (int jj = 0; jj < n; ++jj)
                    {
                        str = "select rec from temp_training where id = " + (r.nextInt(count) + 1);
                        rs = st.executeQuery(str);
                        while (rs.next())
                        {
                            String s = rs.getString(1);
                            if (!attributes)
                            {
                                for (int kk = 0; kk < s.split(",").length - 1; ++kk)
                                {
                                    pw.print("@ATTRIBUTE band" + (kk + 1) + " REAL" + "\n");
                                }

                                pw.print("@ATTRIBUTE class {");
                                pw.print("" + classIds[0]);
                                for (int kk = 1; kk < nclasses; ++kk)
                                {
                                    pw.print("," + classIds[kk]);
                                }

                                pw.print("}" + "\n" + "\n");

                                pw.println("@DATA" + "\n");
                                attributes = true;
                            }
                            if (verbose)
                                System.out.println(s);
                            pw.print(s + "\n");
                        }
                    }
                }

                str = "delete from temp_training where true";
                st.execute(str);

                str = "alter sequence temp_training_seq restart with 1";
                st.execute(str);

                System.out.print("Bootstrap sample " + df.format(((ii + 1) * 100) / nclasses) + "% complete\r");

            }
           
        }
        finally
        {
            if (pw != null) pw.close();
            if (rs != null) rs.close();
            if (st != null) st.close();
            if (con != null) con.close();
        }
    }

}
