package lcm.ltss;

import java.sql.Connection;
import java.sql.SQLException;

import org.apache.commons.dbcp2.BasicDataSource;

/**
 * Singleton.  We only want one connection manager.
 * 
 * Connection pooling is not implemented by the database, so we'll do it manually
 * to speed up computation.  This should be safe to use if automagic connection
 * pooling is implemented by db server.
 * 
 * @author danm
 *
 */
public class ConnectionPool
{
    private static final ConnectionPool cp = new ConnectionPool();
    private static  BasicDataSource connectionPool;
    private static boolean initialised = false;

    private ConnectionPool()
    {
        connectionPool = new BasicDataSource();
        connectionPool.setInitialSize(20);
    }

    public static void init(String driver, String url, String user, String password)
    {
        connectionPool.setDriverClassName(driver);
        connectionPool.setUrl(url);
        connectionPool.setUsername(user);
        connectionPool.setPassword(password);
       
        initialised = true;
    }

    public static ConnectionPool getInstance()
    {
        if (!initialised) return null;

        return cp;
    }
    
    public static Connection getConnection(String url, String user, String password) throws SQLException
    {
        return getConnection("org.postgresql.Driver", url, user, password);
    }
    
    public static Connection getConnection(String driver, String url, String user, String password) throws SQLException
    {
        if (!initialised) init(driver, url, user, password);
        return getConnection();
    }
    
    public static Connection getConnection() throws SQLException
    {
        return connectionPool.getConnection();
    }

}
