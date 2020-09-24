package lcm.ltss;

/**
 * 
 * @author danm
 *
 */
public final class LoggingFactory
{
    
    public static Logger[] getLoggers(String[] loggers) throws Exception
    {
        Logger[] l = new Logger[loggers.length];
        
        for (int ii = 0; ii < loggers.length; ++ii)
        {
            l[ii] = getLogger(loggers[ii]);
        }
        
        return l;
    }
    
    private static Logger getLogger(String w) throws Exception
    {
     
        if ("dblog".equalsIgnoreCase(w))
        {        
            return new DbLogger();
        }
        
        if ("txtlog".equals(w))
        {
            return new TextLogger();
        }
        
        if (w == null)
            return null;
        
        throw new WriterFactoryException("Writer type not found");
    }
}

@SuppressWarnings("serial")
class LoggingFactoryException extends Exception
{
    LoggingFactoryException(String str)
    {
        super(str);
    }
}


