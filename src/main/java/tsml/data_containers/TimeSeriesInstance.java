package tsml.data_containers;

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

public class TimeSeriesInstance extends AbstractList<TimeSeries> implements ListEventNotifier<TimeSeries> {
    
    /* Data */
    private List<TimeSeries> dimensions = new ArrayList<>();
    // store a listener per dimension in this instance
    private final List<ListEventListener<Double>> dimensionsListeners = new ArrayList<>();
    // todo what's the diff between these?
    private final List<ListEventListener<TimeSeries>> listEventListeners = new ArrayList<>();
    private int classLabelIndex = -1; // init to invalid index
    private double targetValue = Double.POSITIVE_INFINITY;
    private List<String> classLabels;
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

    public List<String> getClassLabels() {
        return classLabels;
    }

    public void setClassLabels(final List<String> classLabels) {
        this.classLabels = classLabels;
    }
    
    public String getClassLabel() {
        if(classLabelIndex < 0) {
            // return no class label if class index invalid
            return null;
        }
        return classLabels.get(classLabelIndex);
    }

    /**
     * Set the class label index
     * @param classLabelIndex
     */
    public void setClassLabelIndex(final int classLabelIndex) {
        if(classLabelIndex < 0) {
            throw new IllegalArgumentException("class label index should be >=0, received: " + classLabelIndex);
        }
        this.classLabelIndex = classLabelIndex;
    }

    /**
     * Set the regression target value
     * @param targetValue
     */
    public void setTargetValue(final double targetValue) {
        this.targetValue = targetValue;
    }
    
    public void setDimensionsRaw(List<List<Double>> dimensions) {
        setDimensions(stream().map(TimeSeries::new).collect(Collectors.toList()));
    }
    
    public void setDimensionsRaw(double[][] dimensions) {
        setDimensions(Arrays.stream(dimensions).map(TimeSeries::new).collect(Collectors.toList()));
    }
    
    public void setDimensions(List<TimeSeries> newDimensions) {
        this.dimensions = new ArrayList<>(); // clear the dimensions
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
    
    public static TimeSeriesInstance fromLabelledRawData(List<List<Double>> dimensions, int classLabelIndex, List<String> classLabels) {
        TimeSeriesInstance inst = new TimeSeriesInstance();
        inst.setDimensionsRaw(dimensions);
        inst.setClassLabels(classLabels);
        inst.setClassLabelIndex(classLabelIndex);
        return inst;
    }

    public static TimeSeriesInstance fromLabelledRawData(double[][] dimensions, int classLabelIndex, List<String> classLabels) {
        TimeSeriesInstance inst = new TimeSeriesInstance();
        inst.setDimensionsRaw(dimensions);
        inst.setClassLabels(classLabels);
        inst.setClassLabelIndex(classLabelIndex);
        return inst;
    }
    
    public static TimeSeriesInstance fromLabelledDimensions(List<TimeSeries> dimensions, List<String> classLabels, int classLabelIndex) {
        TimeSeriesInstance inst = new TimeSeriesInstance();
        inst.setDimensions(dimensions);
        inst.setClassLabelIndex(classLabelIndex);
        inst.setClassLabels(classLabels);
        return inst;
    }
    
    public TimeSeriesInstance(List<List<Double>> series, double targetValue) {
        setDimensionsRaw(series);
        setTargetValue(targetValue);
    }
    
    public TimeSeriesInstance(double[][] series, double targetValue) {
        setDimensionsRaw(series);
        setTargetValue(targetValue);
    }
    
    public TimeSeriesInstance(TimeSeries[] dimensions, double targetValue) {
        setDimensions(dimensions);
        setTargetValue(targetValue);
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
     * @return int
     */
    public int getLabelIndex(){
        return classLabelIndex;
    }

    
    /** 
     * @param index
     * @return List<Double>
     */
    public List<Double> getSingleVSliceList(int index){
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
    public double[] getSingleVSliceArray(int index){
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
    public List<Double> getSingleHSliceList(int dim){
        return dimensions.get(dim).getValues();
    }

    
    /** 
     * @param dim
     * @return double[]
     */
    public double[] getSingleHSliceArray(int dim){
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

    @Override public TimeSeries remove(final int i) {
        final TimeSeries removed = dimensions.remove(i);
        final ListEventListener<Double> listener = dimensionsListeners.remove(i);
        removed.removeListEventListener(listener);
        return removed;
    }

    @Override public void add(final int i, final TimeSeries dimension) {
        dimensions.add(i, dimension);
        dimensionsListeners.add(i, buildListener(i, dimension));
    }

    @Override public TimeSeries set(final int i, final TimeSeries dimension) {
        final TimeSeries previous = dimensions.set(i, dimension);
        final ListEventListener<Double> previousListener = dimensionsListeners.set(i, buildListener(i, dimension));
        previous.removeListEventListener(previousListener);
        return previous;
    }
    
    private ListEventListener<Double> buildListener(int dimensionIndex, TimeSeries dimension) {
        // use the dimension index and dimension in the listener if so required
        return new ListEventListener<Double>() {
            @Override public void onMutate() {
                super.onMutate();
                // invalidate any data-dependent metadata since the underlying data has been mutated 
                computeHasMissing = false;
                computeMinLength = false;
                computeMaxLength = false;
            }
        };
    }

    @Override public boolean addListEventListener(final ListEventListener<TimeSeries> listener) {
        return listEventListeners.add(listener);
    }

    @Override public boolean removeListEventListener(final ListEventListener<TimeSeries> listener) {
        return listEventListeners.remove(listener);
    }
}
