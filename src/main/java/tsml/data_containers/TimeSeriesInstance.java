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
    private List<TimeSeries> dimensions = new ArrayList<>(); // todo ctors copy
    // store a listener per dimension in this instance
    private final List<ListEventListener<Double>> dimensionsListeners = new ArrayList<>();
    private final List<ListEventListener<TimeSeries>> listEventListeners = new ArrayList<>();
    private int classLabelIndex;
    private double targetValue;
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
        return getMinLength() == getMaxLength();
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

    // this ctor can be made way more sophisticated.
    public TimeSeriesInstance(List<List<Double>> series, Double value) {
        this(series);

        //could be an index, or it could be regression target
        classLabelIndex = value.intValue();
        targetValue = value;
    }

    // this ctor can be made way more sophisticated.
    public TimeSeriesInstance(List<List<Double>> series, int label) {
        this(series);

        classLabelIndex = label;
    }

    //do the ctor this way round to avoid erasure problems :(
    public TimeSeriesInstance(int labelIndex, List<TimeSeries> series) {
        dimensions = new ArrayList<>(series);
        classLabelIndex = labelIndex; 
    }

    public TimeSeriesInstance(List<List<Double>> series) {
        // process the input list to produce TimeSeries Objects.
        // this allows us to pad if need be, or if we want to squarify the data etc.
        dimensions = new ArrayList<>();

        for (List<Double> ts : series) {
            // convert List<Double> to double[]
            dimensions.add(new TimeSeries(ts.stream().mapToDouble(Double::doubleValue).toArray()));
        }
    }

    public TimeSeriesInstance(double[][] data) {
        dimensions = new ArrayList<>();

        for(double[] in : data){
            dimensions.add(new TimeSeries(in));
        }
	}

    public TimeSeriesInstance(double[][] data, int labelIndex) {
        dimensions = new ArrayList<>();

        for(double[] in : data){
            dimensions.add(new TimeSeries(in));
        }

        classLabelIndex = labelIndex;
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
        return dimensions.get(dim).getSeries();
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
            out.add(dimensions.get(dim).getSeries());

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
