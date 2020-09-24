package lcm.ltss;

public class TextTileWriter implements TileWriter
{
    public TextTileWriter()
    {
        
    }

    public void writeTile(Tile t)
    {
        System.out.println(t.toString());
    }

}
