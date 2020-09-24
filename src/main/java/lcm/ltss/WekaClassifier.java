package lcm.ltss;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import weka.classifiers.AbstractClassifier;
import weka.classifiers.Classifier;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Utils;
import weka.core.converters.ConverterUtils.DataSource;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Remove;

/**
 * Convenience wrapper for Weka classifier
 * @author danm
 *
 */
public class WekaClassifier 
{
    private AbstractClassifier mClassifier = null;
    private Instances mTrainingData = null;
    private String mClassifierName = null;
    private String mOptions = null;
    private String[] mOptionsArray = null;
    private String mArff = null;
    
    private String[] splitOptions(String options)
    {
    	List<String> list = new ArrayList<String>();
    	Matcher m = Pattern.compile("([^\"]\\S*|\".+?\")\\s*").matcher(options);
        while (m.find())
            list.add(m.group(1)); 
        String[] tmpOptions = new String[list.size()];
        int ii = 0;
        for (Iterator<String> iterator = list.iterator(); iterator.hasNext(); ii++)
        {
        	tmpOptions[ii] = iterator.next().replace("\"","");
        }
        return tmpOptions;
    	
    }
    
    public WekaClassifier(String arffName, String options, String filter) throws Exception
    {
        mOptions = options;
        mArff = arffName;
        mOptionsArray = splitOptions(options);
        mClassifierName = mOptionsArray[0];
        mTrainingData = filterAttributes(DataSource.read(arffName), filter);
        mTrainingData.setClassIndex(mTrainingData.numAttributes() - 1);

    }
    
    public void train() throws Exception
    {
       
        String[] options = mOptionsArray;
        options[0] = "";
        AbstractClassifier rf = (AbstractClassifier) Utils.forName(Classifier.class, mClassifierName, options);
        rf.buildClassifier(mTrainingData);
        mClassifier = rf;
    }
    
    public Classifier getClassifier()
    {
        /** Most classifiers are not threadsafe (thankfully RF is).  We could create a copy for each
         *  classification but it takes longer than running a single thread.  */
        /*try
        {
            return AbstractClassifier.makeCopy(mClassifier);
        }
        catch (Exception e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }*/
        return mClassifier;
    }
    
    public Instances getTrainingData()
    {
        return mTrainingData;
    }
    
    /**
     * We stack a raster with multiple bands including to be used in a
     * classification. At this point the raster has already been queried to
     * create a training data set (arff file). In the classification we may wish
     * to ignore some bands. A filter is specified of the format 1,2,3-5,7. This
     * specifies the bands to be ignored and the training set is filtered by
     * removing these.
     * 
     * @param inst
     * @param filter
     * @return
     * @throws Exception
     */
    private Instances filterAttributes(Instances inst, String filter)
            throws Exception
    {
        if (filter == null || "".equals(filter) || "none".equals(filter))
            return inst;

        Remove remove = new Remove();

        remove.setOptions(new String[] { "-R", filter });
        remove.setInputFormat(inst);

        return Filter.useFilter(inst, remove);
    }
    
    public double[] getDistribution(Number[] v) throws Exception
    {
        Instance instance = new DenseInstance(v.length+1);
        instance.setDataset(mTrainingData);
        
        for (int ii = 0; ii < v.length; ++ii)
        {
            instance.setValue(ii, v[ii].doubleValue());
        }
        
        return getClassifier().distributionForInstance(instance);
    }

    public short[] getClasses()
    {
        ArrayList<Object> al = (Collections.list(mTrainingData.attribute("class").enumerateValues()));
        short[] classes = new short[al.size()];
        for (int ii = 0; ii < al.size(); ++ii)
        {
            classes[ii] = Short.parseShort(al.get(ii).toString());
        }
        return classes;
    }
    
    public int getSeed()
    {
        String[] split = mOptions.split(" ");
        
        for (int ii = 0; ii < split.length; ++ii)
        {
            if ("-S".equals(split[ii]))
            {
                return Integer.parseInt(split[ii+1]);
            }
        }
        
        return 0;
    }

    public String getOptions()
    {
        return mOptions;
    }

    public String getName()
    {
        return mClassifierName;
    }
    
    public String getArff()
    {
        return mArff;
    }
    
    public String getDescription()
    {
        return mClassifier.toString();
    }
    
   
 
}
