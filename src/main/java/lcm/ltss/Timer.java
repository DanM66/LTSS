package lcm.ltss;

public class Timer
{
    public long startTime = -1;
    public long stopTime = -1;
    
    public String start(String str)
    {
        start();
        return str + ": Timer started";
    }
    
    
    public long start()
    {
        startTime = System.currentTimeMillis();
        return startTime;
    }
    
    public long stop()
    {
        stopTime = System.currentTimeMillis();
        return stopTime;
    }
    
    public long getElapsed()
    {
        return stop() - startTime; 
    }
    
    public String getElapsedMilliseconds(String str)
    {
        long elapsed = getElapsed();
         
        return str + ": elapsed time " + elapsed + " milliseconds";
    }
    
    public String getElapsedSeconds(String str)
    {
        long elapsed = getElapsed();
         
        return str + ": elapsed time " + (int) (elapsed/1000) + " seconds";
    }
    
}