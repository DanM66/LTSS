package lcm.ltss;

import java.util.Random;

import weka.classifiers.AbstractClassifier;
import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.core.Instances;
import weka.core.Utils;
import weka.core.converters.ConverterUtils.DataSource;

/**
 * Performs a single run of cross-validation and adds the prediction on the test
 * set to the dataset.
 * 
 * Command-line parameters:
 * <ul>
 * <li>-t filename - the dataset to use</li>
 * <li>-o filename - the output file to store dataset with the predictions
 * in</li>
 * <li>-x int - the number of folds to use</li>
 * <li>-s int - the seed for the random number generator</li>
 * <li>-c int - the class index, "first" and "last" are accepted as well; "last"
 * is used by default</li>
 * <li>-W classifier - classname and options, enclosed by double quotes; the
 * classifier to cross-validate</li>
 * </ul>
 * 
 * Example command-line:
 * 
 * <pre>
 * java CrossValidationAddPrediction -t anneal.arff -c last -o predictions.arff -x 10 -s 1 -W "weka.classifiers.trees.J48 -C 0.25"
 * </pre>
 */
public class CrossValidation
{

    private Evaluation mEval = null;
    private AbstractClassifier mCls = null;
    private Instances mData = null;
    private int mFolds = 0;
    private int mSeed = 0;

    public Evaluation getEval() throws Exception
    {
        if (mEval == null)
        {
            mEval = crossValidation(mData, mCls, mSeed, mFolds);
        }
        return mEval;
    }

    public AbstractClassifier getClassifier()
    {
        return mCls;
    }

    public Instances getData()
    {
        return mData;
    }

    public int getFolds()
    {
        return mFolds;
    }

    public int getSeed()
    {
        return mSeed;
    }

    CrossValidation(Instances data, Classifier cls, int seed, int nFolds) throws Exception
    {
        if (nFolds <= 1)
            return;
        mCls = (AbstractClassifier) cls;
        mData = data;
        mSeed = seed;
        mFolds = nFolds;
        mEval = null; // last minute construction. Only evaluate if results required.
    }

    private Evaluation crossValidation(Instances data, Classifier cls, int seed, int folds) throws Exception
    {
        Random rand = new Random(seed);
        Instances randData = new Instances(data);
        randData.randomize(rand);
        if (randData.classAttribute().isNominal())
            randData.stratify(folds);

        Evaluation eval = new Evaluation(randData);
        for (int n = 0; n < folds; n++)
        {
            Instances train = randData.trainCV(folds, n);
            Instances test = randData.testCV(folds, n);
            // Classifier clsCopy = AbstractClassifier.makeCopy(cls);
            cls.buildClassifier(train);
            eval.evaluateModel(cls, test);
        }
        return eval;
    }

    public String getResultDetail() throws Exception
    {

        // output evaluation
        StringBuffer strBuff = new StringBuffer();
        strBuff.append("\n");
        strBuff.append("=== Setup ===\n");
        strBuff.append("Classifier: " + mCls.getClass().getName() + " " + Utils.joinOptions(mCls.getOptions()) + "\n");
        strBuff.append("Dataset: " + mData.relationName() + "\n");
        strBuff.append("Folds: " + mFolds + "\n");
        strBuff.append("Seed: " + mSeed + "\n");
        strBuff.append("\n");
        strBuff.append(getEval().toSummaryString("=== " + mFolds + "-fold Cross-validation ===", false) + "\n");
        strBuff.append(getEval().toClassDetailsString() + "\n");
        strBuff.append(getEval().toMatrixString() + "\n");

        return strBuff.toString();

    }

    public String getClassifierDetails()
    {
        return mCls.getClass().getName() + " " + Utils.joinOptions(mCls.getOptions()) + "\n" + "Dataset: "
                + mData.relationName() + "\n" + "Folds: " + mFolds + "\n" + "Seed: " + mSeed + "\n";
    }

    public String getCVSummary() throws Exception
    {
        return getEval().toSummaryString("=== " + mFolds + "-fold Cross-validation ===", false);
    }

    public String getClassificationDetails() throws Exception
    {
        return getEval().toClassDetailsString();
    }

    public String getMatrix() throws Exception
    {
        return getEval().toMatrixString();
    }

    public double getKappa() throws Exception
    {
        return getEval().kappa();
    }

    public double getPCTCorrect() throws Exception
    {
        return getEval().pctCorrect();
    }

    public static void main(String[] args) throws Exception
    {
        Instances data = DataSource.read(Utils.getOption("t", args));
        String clsIndex = Utils.getOption("c", args);
        if (clsIndex.length() == 0)
            clsIndex = "last";
        if (clsIndex.equals("first"))
            data.setClassIndex(0);
        else if (clsIndex.equals("last"))
            data.setClassIndex(data.numAttributes() - 1);
        else
            data.setClassIndex(Integer.parseInt(clsIndex) - 1);

        // classifier
        String[] tmpOptions;
        String classname;
        tmpOptions = Utils.splitOptions(Utils.getOption("W", args));
        classname = tmpOptions[0];
        tmpOptions[0] = "";
        AbstractClassifier cls = (AbstractClassifier) Utils.forName(Classifier.class, classname, tmpOptions);

        // other options
        int seed = Integer.parseInt(Utils.getOption("s", args));
        int folds = Integer.parseInt(Utils.getOption("x", args));

        System.out.println(new CrossValidation(data, cls, seed, folds).getResultDetail());
    }
}