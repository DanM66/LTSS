package lcm.ltss;
/**
 * Knuth implementation of running stat.
 * @author User
 *
 */
class RunningStat
{

    private int n;
    private double mOldM, mNewM, mOldS, mNewS;

    public void clear()
    {
        n = 0;
    }

    public void push(double x)
    {
        n++;

        // See Knuth TAOCP vol 2, 3rd edition, page 232
        if (n == 1)
        {
            mOldM = mNewM = x;
            mOldS = 0.0;
        }
        else
        {
            mNewM = mOldM + (x - mOldM) / n;
            mNewS = mOldS + (x - mOldM) * (x - mNewM);

            // set up for next iteration
            mOldM = mNewM;
            mOldS = mNewS;
        }
    }

    public int numDataValues()
    {
        return n;
    }

    public double mean()
    {
        return (n > 0) ? mNewM : 0.0;
    }

    public double sampleVariance()
    {
        return ((n > 1) ? mNewS / (n - 1) : 0.0);
    }
    
    public double variance()
    {
        return ((n > 1) ? mNewS / n : 0.0);
    }

    public double standardDeviationPopulation()
    {
        return Math.sqrt(variance());
    }
    
    public double standardDeviationSample()
    {
        return Math.sqrt(sampleVariance());
    }

}