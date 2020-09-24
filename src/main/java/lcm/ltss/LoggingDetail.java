package lcm.ltss;

public class LoggingDetail
{
    private String id = null;
    private String ipAddress = null;
    private String nThreads = null;
    private String classifierDescription = null;
    private String date = null;
    private int classificationTime = -1;
    private int trainingTime = -1;
    private int crossValidationTime = -1;
    private String crossValidationDetail = null;
    private String[] args;
    private Logger[] logger = null;
    private String crossValidationSummary=null;
    private String confusionMatrix = null;
    private double crossValidationKappa = 0.0;
    private double crossValidaitonPercentCorrect = 0.0;;
    
    
    public int getCrossValidationTime()
    {
        return crossValidationTime;
    }

    public void setCrossValidationTime(int crossValidationTime)
    {
        this.crossValidationTime = crossValidationTime;
    }

    
    public double getCrossValidaitonPercentCorrect()
    {
        return crossValidaitonPercentCorrect;
    }

    public void setCrossValidaitonPercentCorrect(double crossValidaitonPercentCorrect)
    {
        this.crossValidaitonPercentCorrect = crossValidaitonPercentCorrect;
    }

    public double getCrossValidationKappa()
    {
        return crossValidationKappa;
    }

    public void setCrossValidationKappa(double crossValidationKappa)
    {
        this.crossValidationKappa = crossValidationKappa;
    }

    public String getConfusionMatrix()
    {
        return confusionMatrix;
    }

    public void setConfusionMatrix(String confusionMatrix)
    {
        this.confusionMatrix = confusionMatrix;
    }

    public String getCrossValidationDetail()
    {
        return crossValidationDetail;
    }

    public void setCrossValidationDetail(String crossValidationDetail)
    {
        this.crossValidationDetail = crossValidationDetail;
    }

    public String getCrossValidationSummary()
    {
        return crossValidationSummary;
    }

    public void setCrossValidationSummary(String str)
    {
        this.crossValidationSummary = str;
    }
    
    public void setCrossValidationDetails(String str)
    {
        this.crossValidationDetail = str;
    }
    public int getTrainingTime()
    {
        return trainingTime;
    }

    public void setTrainingTime(int trainingTime)
    {
        this.trainingTime = trainingTime;
    }
    
    public String getId()
    {
        return id;
    }
    
    public void setArgs(String[] args)
    {
        this.args = args;
    }
    
    public String getArgsString()
    {
        String newLine = System.lineSeparator();
        StringBuffer strBuff = new StringBuffer();
        strBuff.append(args[0]);
        for (int ii = 1; ii < this.args.length; ++ii)
        {
            strBuff.append(newLine);
            strBuff.append(args[ii]); 
        }
        
        return strBuff.toString();
    }


    public void setId(String id)
    {
        this.id = id;
    }

    public String getIpAddress()
    {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress)
    {
        this.ipAddress = ipAddress;
    }

    public String getThreads()
    {
        return nThreads;
    }

    public void setThreads(String nThreads)
    {
        this.nThreads = nThreads;
    }

    public String getClassifierDescription()
    {
        return classifierDescription;
    }

    public void setClassifierDescription(String classifierDescription)
    {
        this.classifierDescription = classifierDescription;
    }

    public String getDate()
    {
        return date;
    }

    public void setDate(String date)
    {
        this.date = date;
    }

    public int getClassificationTime()
    {
        return classificationTime;
    }

    public void setClassificationTime(int classificationTime)
    {
        this.classificationTime = classificationTime;
    }
    
    public void setLoggers(Logger[] l)
    {
        this.logger = l;
    }
    
    public void writeLog() throws Exception
    {
        for (int ii = 0; ii < this.logger.length; ++ii)
        {
           this.logger[ii].log(this);
        }
    }
    
    @Override
    public String toString()
    {
     // TODO create a proper to String
        return "Logger has logged";
    }

}
