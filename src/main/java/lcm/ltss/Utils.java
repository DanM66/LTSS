package lcm.ltss;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class Utils
{
    
 
    /**
     * Calculates mean iteratively, avoiding overflow.
     * 
     * @param input
     * @return
     */
    public static double mean(double[] input)
    {
        double avg = 0;
        int t = 1;
        for (double x : input)
        {
            avg += (x - avg) / t;
            ++t;
        }
        return avg;
    }

    
    /**
     * Get the max double value
     * 
     * @param input
     * @return
     */
    public static double max(double[] input)
    {
        double max = -Double.MAX_VALUE;

        for (double x : input)
        {
            if (x > max)
                max = x;
        }

        return max;

    }

    /**
     * Get the max double value
     * 
     * @param input
     * @return
     */
    public static int max(int[] input)
    {
        int max = -Integer.MAX_VALUE;

        for (int x : input)
        {
            if (x > max)
                max = x;
        }

        return max;
    }

    public static int mode(int[] input)
    {

        int[] count = new int[max(input) + 1];

        // count the occurrences
        for (int ii : input)
        {
            count[ii]++;
        }

        // go backwards and find the count with the most occurrences
        int index = count.length - 1;
        for (int ii = count.length - 2; ii >= 0; ii--)
        {
            if (count[ii] >= count[index])
                index = ii;
        }

        return index;
    }

    /**
     * Creates a raster using a template with nbands. Uses input raster as a
     * template.
     * 
     * @param outRaster
     * @param inRaster
     * @param schema
     * @param nBands
     * @param con
     * @throws SQLException
     */
    public static void createRasterFromTemplate(String outRaster, String inRaster, int nBands) throws SQLException
    {
        Connection con = null;
        Statement st = null;

        try
        {
            con = ConnectionPool.getConnection();

            String queryString = "create table " + outRaster
                    + " as select r.rid as rid, st_makeemptyraster(rast) as rast from " + inRaster
                    + " r order by r.rid";

            st = con.createStatement();

            st.execute(queryString);

            queryString = "update " + outRaster + " set rast = st_addband(rast,'8BUI'::text,0)";

            for (int ii = 0; ii < nBands; ++ii)
            {
                st.execute(queryString);
            }

            String table = outRaster;

            String parts[] = outRaster.split("[.]");
            String schema = "public";
            if (parts.length == 2)
            {
                schema = parts[0];
                table = parts[1];
            }

            queryString = "select addrasterconstraints('" + schema + "'::name, '" + table + "'::name, 'rast'::name)";
            st.execute(queryString);

            
        }
        finally
        {
            if (st != null) st.close();
            if (con != null) con.close();
        }
    }
    
    /**
     * Check if a table exists.
     * 
     * @param tableName
     * @param con
     * @return
     * @throws SQLException
     */
    public static boolean relationExists(String tableName) throws SQLException
    {
        boolean tableExists = false;
        String schema = "public";
        String table = tableName;

        String parts[] = tableName.split("[.]");
        if (parts.length == 2)
        {
            schema = parts[0];
            table = parts[1];
        }

        Connection con = null;
        Statement st = null;
        ResultSet rs = null;
       
        try
        {
            con = ConnectionPool.getConnection();
            String queryString = "select table_schema, table_name from information_schema.tables where table_name = '"
                    + table + "' and table_schema = '" + schema + "';";
            st = con.createStatement();
            rs = st.executeQuery(queryString);
            while (rs.next())
            {
                tableExists = true;
            }
           
        }
        finally
        {
            if (rs != null) rs.close();
            if (st != null) st.close();
            if (con != null) con.close();
        }
        return tableExists;
    }


    /**
     * Drop a table or view
     * 
     * @param tableName
     * @param con
     * @throws SQLException
     */
    public static void drop(String tableName) throws SQLException
    {
        Connection con = null;
        Statement st = null;
        ResultSet rs = null;
        try
        {
            con = ConnectionPool.getConnection();
            st = con.createStatement();
            rs = st.executeQuery("select table_type from information_schema.tables where table_name = '" + tableName + "';");

            String type = "table";
            while (rs.next())
            {
                String str = rs.getString(1).toLowerCase();

                if ("view".equals(str))
                    type = "view";
            }
            st.execute("drop " + type + " " + tableName);
        }
        finally
        {
            if (rs!=null) rs.close();
            if (st!=null) st.close();
            if (con!=null) con.close();
        }
    }

}
