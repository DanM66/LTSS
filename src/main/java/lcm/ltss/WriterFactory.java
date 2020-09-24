package lcm.ltss;

public final class WriterFactory
{
    public static TileWriter getWriter(String w) throws Exception
    {
     
        
        if ("dbwriter".equalsIgnoreCase(w))
        {        
            return new DbTileWriter();
        }
        
        if ("txt".equals(w))
        {
            return new TextTileWriter();
        }
        
        if (w == null)
            return null;
        
        throw new WriterFactoryException("Writer type not found");
    }
}

@SuppressWarnings("serial")
class WriterFactoryException extends Exception
{
    WriterFactoryException(String str)
    {
        super(str);
    }
}