package lcm.ltss;

import java.io.File;
import java.util.Set;
import java.util.TreeSet;

/**
 * Convenience singleton class for passing around parameters.
 * 
 * @author danm
 *
 */
public final class ClassificationConfig
{
    private boolean verbose = false;
    private boolean detail = false;
    private String wekaOptions = null;
    private WekaClassifier wekaCurrent = null;
    private String arff = null;
    private String url = null;
    private String user = null;
    private String raster = null;
    private String mask = null;
    private String password = null;
    private String output = null;
    private String filter = null;
    private int nThreads = 1;
    private int nFolds = 0;
    private String[] logger = null;
    private String id = "";
    private int count = 0;
    private String logTableName = null;
    
  
    public String getLogTableName()
    {
        return logTableName;
    }

    public void setLogTableName(String logTableName)
    {
        this.logTableName = logTableName;
    }

    private static final ClassificationConfig c = new ClassificationConfig();
    
    public String getId()
    {
        return id;
    }

    public void setId(String id)
    {
        this.id = id;
    }

    private String getFilterId()
    {
        if (filter == null || "".equals(filter) || "none".equals(filter))
            return "";

        String str = filter.replace("-", "to");
        str = str.replace(",", "_");
        return str;
    }

    public static final ClassificationConfig getInstance()
    {
        return c;
    }
    
    /**
     * 
     * @return
     */
    public String[] getLoggers()
    {
        return logger;
    }

    /**
     * We can register a number of loggers.  Each is separated by a ":"
     * @param logger
     */
    public void setLoggers(String logger)
    {
        this.logger = logger.split(":");
    }
    
    public void setThreads(String s)
    {
        this.nThreads = Integer.parseInt(s);
    }
    
    public void setFolds(String s)
    {
        this.nFolds = Integer.parseInt(s);
    }
    
    public int getFolds()
    {
        return this.nFolds;
    }
    
    public int getThreads()
    {
        return this.nThreads;
    }

    public boolean isVerbose()
    {
        return verbose;
    }

    public void setVerbose(boolean verbose)
    {
        this.verbose = verbose;
    }

    public boolean isDetail()
    {
        return detail;
    }

    public void setDetail(boolean detail)
    {
        this.detail = detail;
    }

    public String getWekaOptions()
    {
        return wekaOptions;
    }

    public void setCurrentWeka(WekaClassifier wekaCurrent)
    {
        //hacky
        ++count;
        this.wekaCurrent = wekaCurrent;
    }

    public WekaClassifier getCurrentWeka()
    {
        return wekaCurrent;
    }

    public void setWekas(String wekaOptions)
    {
        this.wekaOptions = wekaOptions;
    }

    public String getArff()
    {
        return arff;
    }

    public void setArff(String arff)
    {
        this.arff = arff;
    }

    public String getUrl()
    {
        return url;
    }

    public void setUrl(String url)
    {
        this.url = url;
    }

    public String getUser()
    {
        return user;
    }

    public void setUser(String user)
    {
        this.user = user;
    }

    public String getRaster()
    {
        return raster;
    }

    public void setRaster(String raster)
    {
        this.raster = raster;
    }

    public String getMask()
    {
        return mask;
    }

    public void setMask(String mask)
    {
        this.mask = mask;
    }

    public String getPassword()
    {
        return password;
    }

    public void setPassword(String password)
    {
        this.password = password;
    }

    public String getOutput()
    {
        return output;
    }

    public void setOutput(String output)
    {
        this.output = output;
    }

    public void setFilter(String filter)
    {
        this.filter = filter;
    }
    
    public String getFilter()
    {
        return this.filter;
    }

    public Set<Integer> getFilterSet()
    {
        if (filter == null || "".equals(filter) || "none".equals(filter))
            return null;

        TreeSet<Integer> s = new TreeSet<Integer>();

        String[] split1 = filter.split(",");

        for (int ii = 0; ii < split1.length; ++ii)
        {
            String[] split2 = split1[ii].split("-");

            if (split2.length != 2)
            {
                s.add(Integer.parseInt(split1[ii]));
                continue;
            }

            for (int jj = Integer.parseInt(split2[0]); jj <= Integer.parseInt(split2[1]); ++jj)
            {
                s.add(jj);
            }
        }
        return s;
    }

    public int getNumOutBands()
    {
        if (detail)
        {
            return this.getCurrentWeka().getClasses().length + 1;
        }

        return 2;

    }

    public String generateOutName()
    {
        WekaClassifier wk = this.getCurrentWeka();

        String[] tmp = wk.getOptions().split(" ");
        //String name = tmp[tmp.length - 1].replaceAll("\\s+", "");
        tmp = tmp[0].split("[.]");
        String name = tmp[tmp.length-1];
        String arff = (new File(wk.getArff()).getName()).split("[.]")[0];
        String filter = getFilterId();
        name = this.getRaster() + "_" + count + getId() + arff + name.replaceAll("[-]", "_") + (this.isDetail() ? "_d" : "") + filter;

        return name.toLowerCase();
    }

}
