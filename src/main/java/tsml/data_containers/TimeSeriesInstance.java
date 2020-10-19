package tsml.data_containers;

import org.junit.Assert;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Data structure able to store a time series instance. it can be standard
 * (univariate, no missing, equally sampled series) or complex (multivariate,
 * unequal length, unequally spaced, univariate or multivariate time series).
 *
 * Should Instances be immutable after creation? Meta data is calculated on
 * creation, mutability can break this
 */

public class TimeSeriesInstance extends AbstractList<TimeSeries> implements Notifier<TimeSeriesInstanceListener> {
    
    /* Data */
    private List<TimeSeries> dimensions = new ArrayList<>();
    // store a listener per dimension in this instance. These listen to the dimensions and update this instance.
    private List<TimeSeriesListener> dimensionsListeners = new ArrayList<>();
    // store the listeners to this instance, i.e. alert external listeners for changes in this instance
    private final Set<TimeSeriesInstanceListener> listeners = new HashSet<>(1, 1);
    private int classLabelIndex = -1; // init to invalid index
    private double regressionTarget = Double.NaN; // init to invalid target
    private LabelEncoder labelEncoder;
    private String classLabel;
    /* Meta Information */
    private boolean hasMissing;
    private boolean computeHasMissing = true;
    private int minLength = -1;
    private boolean computeMinLength = true;
    private int maxLength = -1;
    private boolean computeMaxLength = true;
    private boolean hasTimeStamps;
    private boolean computeHasTimeStamps = true;
    private boolean isEquallySpaced;
    private boolean computeIsEquallySpaced = true;
    private boolean isEqualLength;
    private boolean computeIsEqualLength = true;
    
    public boolean hasRegressionTarget() {
        return !Double.isNaN(regressionTarget);
    }

    public LabelEncoder getLabelEncoder() {
        return labelEncoder;
    }

    public void setLabelEncoder(final LabelEncoder labelEncoder) {
        if(hasClassLabel() && !labelEncoder.getClasses().contains(classLabel)) {
            throw new IllegalArgumentException("label encoder does not contain class label " + classLabel);
        }
        this.labelEncoder = labelEncoder;
        // unset regression target
        regressionTarget = Double.NaN;
        listeners.forEach(TimeSeriesInstanceListener::onClassChange);
    }

    public List<String> getClasses() {
        return labelEncoder.getClasses();
    }
    
    public void setClasses(List<String> classes) {
        setLabelEncoder(new LabelEncoder(classes));
    }

    public String getClassLabel() {
        return classLabel;
    }

    public void setClassLabel(final String classLabel) {
        this.classLabel = classLabel;
        if(classLabel == null) {
            classLabelIndex = -1;
        } else {
            classLabelIndex = labelEncoder.transform(classLabel);
        }
        // unset regression target
        regressionTarget = Double.NaN;
        listeners.forEach(TimeSeriesInstanceListener::onClassChange);
    }

    public int getClassLabelIndex() {
        return classLabelIndex;
    }

    public void setClassLabelIndex(final int classLabelIndex) {
        this.classLabelIndex = classLabelIndex;
        classLabel = labelEncoder.inverseTransform(classLabelIndex);
        // unset regression target
        removeRegressionTarget();
        listeners.forEach(TimeSeriesInstanceListener::onClassChange);
    }
    
    public void setClassLabelIndex(int index, LabelEncoder labelEncoder) {
        removeClassLabel();
        setLabelEncoder(labelEncoder);
        setClassLabelIndex(index);
    }
    
    public void setClassLabelIndex(int index, String[] classes) {
        setClassLabelIndex(index, Arrays.asList(classes));
    }
    
    public void setClassLabelIndex(int index, List<String> classes) {
        setClassLabelIndex(index, new LabelEncoder(classes));
    }
    
    public void setClassLabel(String label, LabelEncoder labelEncoder) {
        setClassLabelIndex(labelEncoder.transform(label), labelEncoder);
    }
    
    public void setClassLabel(String label, String[] classes) {
        setClassLabel(label, Arrays.asList(classes));
    }
    
    public void setClassLabel(String label, List<String> classes) {
        setClassLabel(label, new LabelEncoder(classes));
    }
    
    public boolean hasClassLabel() {
        return classLabel != null;
    }

    public void removeRegressionTarget() {
        setRegressionTarget(Double.NaN);
    }
    
    public void removeClassLabel() {
        setClassLabel(null);
    }
    
    /**
     * Set the regression target value
     * @param regressionTarget
     */
    public void setRegressionTarget(final double regressionTarget) {
        this.regressionTarget = regressionTarget;
        // unset classification variables
        classLabel = null;
        classLabelIndex = -1;
        labelEncoder = null;
        // alert listeners
        listeners.forEach(TimeSeriesInstanceListener::onClassChange);
    }

    public void setDimensionsRaw(List<? extends List<Double>> dimensions) {
        setDimensions(stream().map(TimeSeries::new).collect(Collectors.toList()));
    }
    
    public void setDimensionsRaw(double[][] dimensions) {
        setDimensions(Arrays.stream(dimensions).map(TimeSeries::new).collect(Collectors.toList()));
    }
    
    public void setDimensions(List<? extends TimeSeries> newDimensions) {
        clear(); // clear out current data
        addAll(newDimensions); // add all the new dimensions
    }
    
    public void setDimensions(TimeSeries[] dimensions) {
        setDimensions(Arrays.asList(dimensions));
    }
    
    public boolean isEquallySpaced() {
        if(computeIsEquallySpaced) {
            isEquallySpaced = stream().allMatch(TimeSeries::isEquallySpaced);
            computeIsEquallySpaced = false;
        }
        return isEquallySpaced;
    }
    
    public boolean hasTimeStamps() {
        if(computeHasTimeStamps) {
            hasTimeStamps = stream().allMatch(TimeSeries::hasTimeStamps);
            computeHasTimeStamps = false;
        }
        return hasTimeStamps;
    }

    /** 
     * @return boolean
     */
    public boolean isEqualLength(){
        if(computeIsEqualLength) {
            // if there's no dimensions then all dimensions are equal length
            if(dimensions.isEmpty()) {
                isEqualLength = false;
            } else {
                // otherwise there's at least one dimension
                final int length = dimensions.get(0).getSeriesLength();
                // compare to other dimensions
                isEqualLength = true; // assume condition is true
                for(int i = 1; i < dimensions.size() && isEqualLength; i++) {
                    final int otherLength = dimensions.get(i).getSeriesLength();
                    if(otherLength != length) {
                        // fail the condition if lengths not equal
                        isEqualLength = false;
                    }
                }
            }
            computeIsEqualLength = false;
        }
        return isEqualLength;
    }

    /** 
     * @return int
     */
    public int getMinLength() {
        if(computeMinLength) {
            minLength = dimensions.stream().map(TimeSeries::getSeriesLength).min(Integer::compareTo).orElse(0);
            computeMinLength = false;
        }
        return minLength;
    }

    
    /** 
     * @return int
     */
    public int getMaxLength() {
        if(computeMaxLength) {
            maxLength = dimensions.stream().map(TimeSeries::getSeriesLength).max(Integer::compareTo).orElse(0);
            computeMaxLength = false;
        }
        return maxLength;
    }
    
    public boolean isUnivariate() {
        return dimensions.size() == 1;
    }
    
    public boolean isMultivariate() {
        return dimensions.size() > 1;
    }

    public boolean hasMissing() {
        if(computeHasMissing) {
            // if any of the series have a NaN value, across all dimensions then this is true.
            hasMissing = dimensions.stream().anyMatch(TimeSeries::hasMissing);
            computeHasMissing = false;
        }
        return hasMissing;
    }

    /* End Meta Information */
    
    public TimeSeriesInstance() {
        
    }
    
    // instead of constructors we're using several static methods to produce TimeSeriesInstance objects. These are required as generics cause ctor clashes, i.e TimeSeriesInstance(List<TimeSeries> list) and TimeSeriesInstance(List<List<Double>> list) clash due to runtime erasure.

    /**
     * Build instance from time series dimensions
     * @param dimensions
     * @return
     */
    public static TimeSeriesInstance fromTimeSeries(List<? extends TimeSeries> dimensions) {
        TimeSeriesInstance instance = new TimeSeriesInstance();
        instance.setDimensions(dimensions);
        return instance;
    }

    /**
     * Build instance from raw data
     * @param dimensions
     * @return
     */
    public static TimeSeriesInstance fromData(List<? extends List<Double>> dimensions) {
        TimeSeriesInstance instance = new TimeSeriesInstance();
        instance.setDimensionsRaw(dimensions);
        return instance;
    }

    /**
     * Build instance from raw data
     * @param dimensions
     * @return
     */
    public static TimeSeriesInstance fromData(double[][] dimensions) {
        TimeSeriesInstance instance = new TimeSeriesInstance();
        instance.setDimensionsRaw(dimensions);
        return instance;
    }

    /**
     * Build instance from time series dimensions with a regression target
     * @param dimensions
     * @param targetValue
     * @return
     */
    public static TimeSeriesInstance fromRegressedTimeSeries(List<? extends TimeSeries> dimensions, double targetValue) {
        TimeSeriesInstance instance = new TimeSeriesInstance();
        instance.setDimensions(dimensions);
        instance.setRegressionTarget(targetValue);
        return instance;
    }

    /**
     * Build instance from raw data with a regression target
     * @param dimensions
     * @param targetValue
     * @return
     */
    public static TimeSeriesInstance fromRegressedData(List<? extends List<Double>> dimensions, double targetValue) {
        TimeSeriesInstance instance = new TimeSeriesInstance();
        instance.setDimensionsRaw(dimensions);
        instance.setRegressionTarget(targetValue);
        return instance;
    }

    /**
     * Build instance from raw data with a regression target
     * @param dimensions
     * @param targetValue
     * @return
     */
    public static TimeSeriesInstance fromRegressedData(double[][] dimensions, double targetValue) {
        TimeSeriesInstance instance = new TimeSeriesInstance();
        instance.setDimensionsRaw(dimensions);
        instance.setRegressionTarget(targetValue);
        return instance;
    }

    /**
     * Build instance from time series dimensions with a classification label
     * @param dimensions
     * @param labelEncoder
     * @param classLabelIndex
     * @return
     */
    public static TimeSeriesInstance fromClassifiedTimeSeries(List<? extends TimeSeries> dimensions, LabelEncoder labelEncoder, int classLabelIndex) {
        TimeSeriesInstance instance = new TimeSeriesInstance();
        instance.setDimensions(dimensions);
        instance.setLabelEncoder(labelEncoder);
        instance.setClassLabelIndex(classLabelIndex);
        return instance;
    }
    public static TimeSeriesInstance fromClassifiedTimeSeries(List<? extends TimeSeries> dimensions, List<String> classes, int classLabelIndex) {
        return fromClassifiedTimeSeries(dimensions, new LabelEncoder(classes), classLabelIndex);
    }
    public static TimeSeriesInstance fromClassifiedTimeSeries(List<? extends TimeSeries> dimensions, List<String> classes, String classLabel) {
        return fromClassifiedTimeSeries(dimensions, new LabelEncoder(classes), classLabel);
    }
    public static TimeSeriesInstance fromClassifiedTimeSeries(List<? extends TimeSeries> dimensions, LabelEncoder labelEncoder, String classLabel) {
        return fromClassifiedTimeSeries(dimensions, labelEncoder, labelEncoder.transform(classLabel));
    }

    public static TimeSeriesInstance fromClassifiedData(List<? extends List<Double>> dimensions, List<String> classes, int classLabelIndex) {
        return fromClassifiedTimeSeries(dimensions.stream().map(TimeSeries::new).collect(Collectors.toList()), classes, classLabelIndex);
    }
    public static TimeSeriesInstance fromClassifiedData(List<? extends List<Double>> dimensions, LabelEncoder labelEncoder, int classLabelIndex) {
        return fromClassifiedTimeSeries(dimensions.stream().map(TimeSeries::new).collect(Collectors.toList()), labelEncoder, classLabelIndex);
    }
    public static TimeSeriesInstance fromClassifiedData(List<? extends List<Double>> dimensions, List<String> classes, String classLabel) {
        return fromClassifiedTimeSeries(dimensions.stream().map(TimeSeries::new).collect(Collectors.toList()), classes, classLabel);
    }
    public static TimeSeriesInstance fromClassifiedData(List<? extends List<Double>> dimensions, LabelEncoder labelEncoder, String classLabel) {
        return fromClassifiedTimeSeries(dimensions.stream().map(TimeSeries::new).collect(Collectors.toList()), labelEncoder, classLabel);
    }

    public static TimeSeriesInstance fromClassifiedData(double[][] dimensions, List<String> classes, int classLabelIndex) {
        return fromClassifiedTimeSeries(Arrays.stream(dimensions).map(TimeSeries::new).collect(Collectors.toList()), classes, classLabelIndex);
    }
    public static TimeSeriesInstance fromClassifiedData(double[][] dimensions, LabelEncoder labelEncoder, int classLabelIndex) {
        return fromClassifiedTimeSeries(Arrays.stream(dimensions).map(TimeSeries::new).collect(Collectors.toList()), labelEncoder, classLabelIndex);
    }
    public static TimeSeriesInstance fromClassifiedData(double[][] dimensions, List<String> classes, String classLabel) {
        return fromClassifiedTimeSeries(Arrays.stream(dimensions).map(TimeSeries::new).collect(Collectors.toList()), classes, classLabel);
    }
    public static TimeSeriesInstance fromClassifiedData(double[][] dimensions, LabelEncoder labelEncoder, String classLabel) {
        return fromClassifiedTimeSeries(Arrays.stream(dimensions).map(TimeSeries::new).collect(Collectors.toList()), labelEncoder, classLabel);
    }
    
    @Override public int size() {
        return getNumDimensions();
    }

    /** 
     * @return int
     */
    public int getNumDimensions() {
        return dimensions.size();
    }
    
    
    /** 
     * @param index
     * @return List<Double>
     */
    public List<Double> getVSliceList(int index){
        List<Double> out = new ArrayList<>(getNumDimensions());
        for(TimeSeries ts : dimensions){
            out.add(ts.get(index));
        }

        return out;
    }

    
    /** 
     * @param index
     * @return double[]
     */
    public double[] getVSliceArray(int index){
        double[] out = new double[getNumDimensions()];
        int i=0;
        for(TimeSeries ts : dimensions){
            out[i++] = ts.get(index);
        }

        return out;
    }

    
    /** 
     * @param indexesToKeep
     * @return List<List<Double>>
     */
    public List<List<Double>> getVSliceList(int[] indexesToKeep){
        return getVSliceList(Arrays.stream(indexesToKeep).boxed().collect(Collectors.toList()));
    }

    
    /** 
     * @param indexesToKeep
     * @return List<List<Double>>
     */
    public List<List<Double>> getVSliceList(List<Integer> indexesToKeep){
        List<List<Double>> out = new ArrayList<>(getNumDimensions());
        for(TimeSeries ts : dimensions){
            out.add(ts.toListWithIndexes(indexesToKeep));
        }

        return out;
    }

 
    
    /** 
     * @param indexesToKeep
     * @return double[][]
     */
    public double[][] getVSliceArray(int[] indexesToKeep){
        return getVSliceArray(Arrays.stream(indexesToKeep).boxed().collect(Collectors.toList()));
    }

    
    /** 
     * @param indexesToKeep
     * @return double[][]
     */
    public double[][] getVSliceArray(List<Integer> indexesToKeep){
        double[][] out = new double[getNumDimensions()][];
        int i=0;
        for(TimeSeries ts : dimensions){
            out[i++] = ts.toArrayWithIndexes(indexesToKeep);
        }

        return out;
    }
    
    
    /** 
     * @param dim
     * @return List<Double>
     */
    public List<Double> getHSliceList(int dim){
        return dimensions.get(dim).getValues();
    }

    
    /** 
     * @param dim
     * @return double[]
     */
    public double[] getHSliceArray(int dim){
        return dimensions.get(dim).toArrayPrimitive();
    }

    
    /** 
     * @param dimensionsToKeep
     * @return List<List<Double>>
     */
    public List<List<Double>> getHSliceList(int[] dimensionsToKeep){
        return getHSliceList(Arrays.stream(dimensionsToKeep).boxed().collect(Collectors.toList()));
    }
    
    /** 
     * TODO: not a clone. may need to be careful...
     * @param dimensionsToKeep
     * @return List<List<Double>>
     */
    public List<List<Double>> getHSliceList(List<Integer> dimensionsToKeep){
        List<List<Double>> out = new ArrayList<>(dimensionsToKeep.size());
        for(Integer dim : dimensionsToKeep)
            out.add(dimensions.get(dim).getValues());

        return out;
    }

    
    /** 
     * @param dimensionsToKeep
     * @return double[][]
     */
    public double[][] getHSliceArray(int[] dimensionsToKeep){
        return getHSliceArray(Arrays.stream(dimensionsToKeep).boxed().collect(Collectors.toList()));
    }

    
    /** 
     * @param dimensionsToKeep
     * @return double[][]
     */
    public double[][] getHSliceArray(List<Integer> dimensionsToKeep){
        double[][] out = new double[dimensionsToKeep.size()][];
        int i=0;
        for(Integer dim : dimensionsToKeep){
            out[i++] = dimensions.get(dim).toArrayPrimitive();
        }

        return out;
    }

    
    /** 
     * @return String
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append("Num Dimensions: ").append(getNumDimensions()).append(" Class Label Index: ").append(classLabelIndex);
        for (TimeSeries ts : dimensions) {
            sb.append(System.lineSeparator());
            sb.append(ts.toString());
        }

        return sb.toString();
    }
    
    /** 
     * @return double[][]
     */
    public double[][] toValueArray(){
        double[][] output = new double[this.dimensions.size()][];
        for (int i=0; i<output.length; ++i){
             //clone the data so the underlying representation can't be modified
            output[i] = dimensions.get(i).toArrayPrimitive();
        }
        return output;
    }

    
    /** 
     * @return double[][]
     */
    public double[][] toTransposedArray(){
        return this.getVSliceArray(IntStream.range(0, maxLength).toArray());
    }


    
    /** 
     * @return int
     */
    @Override
    public int hashCode(){
        return this.dimensions.hashCode();
    }

	
    /** 
     * @param i
     * @return TimeSeries
     */
    public TimeSeries get(int i) {
        return this.dimensions.get(i);
	}

    private void setComputeMetadataForValues() {
        computeIsEqualLength = true;
        computeMinLength = true;
        computeMaxLength = true;
        computeHasMissing = true;
    }
    
    private void setComputeMetadataForTimeStamps() {
        computeIsEqualLength = true;
        computeHasTimeStamps = true;
        computeIsEquallySpaced = true;
    }
    
    private void setComputeMetadata() {
        setComputeMetadataForTimeStamps();
        setComputeMetadataForValues();
    }
	
    @Override public void clear() {
        super.clear();
        // remove the listeners
        for(int i = 0; i < dimensions.size(); i++) {
            TimeSeries dimension = dimensions.get(i);
            TimeSeriesListener listener = dimensionsListeners.get(i);
            dimension.removeListener(listener);
        }
        // initialise the dimensions and listeners for each dimension to empty
        dimensions = new ArrayList<>();
        dimensionsListeners = new ArrayList<>();
        // compute metadata
        setComputeMetadata();
        // alert listeners that changes have happened. Must do this after setting compute metadata above as these may rely on the meta data being recomputed
        listeners.forEach(TimeSeriesInstanceListener::onDimensionChange);
    }
	
    @Override public TimeSeries remove(final int i) {
        // remove the dimension
        final TimeSeries removed = dimensions.remove(i);
        // get the listener associated with the dimension
        final TimeSeriesListener listener = dimensionsListeners.remove(i);
        // disable the listening to the dimension
        removed.removeListener(listener);
        // compute metadata
        setComputeMetadata();
        // alert listeners that changes have happened. Must do this after setting compute metadata above as these may rely on the meta data being recomputed
        listeners.forEach(TimeSeriesInstanceListener::onDimensionChange);
        return removed;
    }

    @Override public void add(final int i, final TimeSeries dimension) {
        // add the dimension
        dimensions.add(i, dimension);
        // build a listener for the dimension to listen for changes
        TimeSeriesListener listener = buildDimensionListener(i, dimension);
        // enable listeneing to changes in the dimension
        dimension.addListener(listener);
        // associate listen with dimension, storing at the same index
        dimensionsListeners.add(i, listener);
        // compute metadata
        setComputeMetadata();
        // alert listeners that changes have happened. Must do this after setting compute metadata above as these may rely on the meta data being recomputed
        listeners.forEach(TimeSeriesInstanceListener::onDimensionChange);
    }

    @Override public TimeSeries set(final int i, final TimeSeries dimension) {
        // get the current listener
        TimeSeriesListener currentListener = dimensionsListeners.get(i);
        // get the current dimension
        TimeSeries currentDimension = dimensions.get(i);
        // disable listening to the current dimension
        currentDimension.removeListener(currentListener);
        // build a new listener
        TimeSeriesListener listener = buildDimensionListener(i, dimension);
        // enable listening to the new dimension
        dimension.addListener(listener);
        // replace the new dimension and associated listener
        dimensionsListeners.set(i, listener);
        // compute metadata
        setComputeMetadata();
        // alert listeners that changes have happened. Must do this after setting compute metadata above as these may rely on the meta data being recomputed
        listeners.forEach(TimeSeriesInstanceListener::onDimensionChange);
        return dimensions.set(i, dimension);
    }
    
    private TimeSeriesListener buildDimensionListener(int dimensionIndex, TimeSeries dimension) {
        // use the dimension index and dimension in the listener if so required
        return new TimeSeriesListener() {

            @Override public void onValueChange() {
                super.onValueChange();
                // recompute several stats / metadata as time series values have changed. Do this before alerting listeners which may depend on the metadata.
                setComputeMetadataForValues();
                // alert the listeners attached to this instance that the values inside the time series inside this instance have changed
                listeners.forEach(TimeSeriesInstanceListener::onValueChange);
            }

            @Override public void onTimeStampChange() {
                super.onTimeStampChange();
                // recompute several stats / metadata as time series time stamps have changed. Do this before alerting listeners which may depend on the metadata.
                setComputeMetadataForTimeStamps();
                // alert the listeners attached to this instance that the timestamps inside the time series inside this instance have changed
                listeners.forEach(TimeSeriesInstanceListener::onTimeStampChange);
            }

        };
    }

    @Override public boolean addListener(final TimeSeriesInstanceListener listener) {
        return listeners.add(listener);
    }

    @Override public boolean removeListener(final TimeSeriesInstanceListener listener) {
        return listeners.remove(listener);
    }

    public static void main(String[] args) {
        TimeSeriesInstance inst = new TimeSeriesInstance();
        inst.addListener(new TimeSeriesInstanceListener() {
            @Override public void onValueChange() {
                super.onValueChange();
                System.out.println("mutated values");
            }

            @Override public void onTimeStampChange() {
                super.onTimeStampChange();
                System.out.println("mutated time stamps");
            }
        });
        TimeSeries dimA = new TimeSeries(new double[] {1,2,3});
        TimeSeries dimB = new TimeSeries(new double[] {4,5,6});
        TimeSeries dimC = new TimeSeries(new double[] {7,8,9});
        TimeSeries dimD = new TimeSeries(new double[] {10,11,12});
        
        dimA.addListener(new TimeSeriesListener() {
            @Override public void onValueChange() {
                super.onValueChange();
                System.out.println("dimA mutated");
            }
        });
        
        inst.add(dimA);
        inst.add(dimB);
        inst.add(dimC);
        inst.set(2, dimD);
        System.out.println(inst);

        Assert.assertFalse(inst.hasMissing());
        
        dimA.set(1, Double.NaN);

        System.out.println(inst);
        
        Assert.assertTrue(inst.hasMissing());
    }
}
