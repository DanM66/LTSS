package lcm.ltss;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class DbLogger implements Logger
{
   
    
    private void executeSql(String sql) throws SQLException
    {
        Connection con = null;
        Statement st = null;
        
        try
        {
            con = ConnectionPool.getConnection();
            st = con.createStatement();
            st.execute(sql);
        }
        finally
        {
            if (st != null) st.close();
            if (con != null) con.close();
        }
        
    }
    
    private String updateString(String tableName, String str, String value, String id)
    {
        return "update " + tableName + " set " + str + " = '" + value
                + "' where id = '" + id + "';";
    }
    
    private String updateNumber(String tableName, String str, Number value, String id)
    {
        return "update " + tableName + " set " + str + " = " + value
                + " where id = '" + id + "';";
    }
   

    public void log(LoggingDetail o) throws Exception
    {
        
        ClassificationConfig c = ClassificationConfig.getInstance();
        String tableName = c.getLogTableName();
        if (!Utils.relationExists(tableName))
        {
            System.err.println("Cannot log.  Table " + tableName + "does not exist.");
            return;
        }
        
        StringBuffer strBuff = new StringBuffer();
        String sql = null;
        //Delete row
        sql = "delete from " + tableName + " where id = '" + o.getId() + "';";
        strBuff.append(sql);
        
        //Add
        String id = o.getId();
        sql = "insert into " + tableName + "(id) values ('" + id + "');";
        strBuff.append(sql);
        
        sql = updateString(tableName, "ip_address", o.getIpAddress(), id);
        strBuff.append(sql);
        
        sql = updateString(tableName, "args", o.getArgsString(), id);
        strBuff.append(sql);
        
        sql = updateString(tableName, "_date", o.getDate(), id);
        strBuff.append(sql);
        
        sql = updateString(tableName, "description", o.getClassifierDescription(), id);
        strBuff.append(sql);
        
        sql = updateNumber(tableName, "t_time", o.getTrainingTime(), id);
        strBuff.append(sql);
        
        sql = updateNumber(tableName, "c_time", o.getClassificationTime(), id);
        strBuff.append(sql);
        
        sql = updateString(tableName, "cv_summary", o.getCrossValidationSummary(), id);
        strBuff.append(sql);
        
        sql = updateString(tableName, "cv_matrix", o.getConfusionMatrix(), id);
        strBuff.append(sql);
        
        sql = updateNumber(tableName, "kappa", o.getCrossValidationKappa(), id);
        strBuff.append(sql);
        
        sql = updateNumber(tableName, "pc_correct", o.getCrossValidaitonPercentCorrect(), id);
        strBuff.append(sql);
        
        sql = updateNumber(tableName, "cv_time", o.getCrossValidationTime(), id);
        strBuff.append(sql);
        
        executeSql(strBuff.toString());
        
    }

}
