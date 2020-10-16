package tsml.data_containers;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Data structure able to handle unequal length, unequally spaced, univariate or
 * multivariate time series.
 */
public class TimeSeriesInstances extends AbstractList<TimeSeriesInstance> {

    /* Meta Information */
    private String description;
    private String problemName;
    private boolean isEquallySpaced;
    private boolean computeIsEquallySpaced = true;
    private boolean hasMissing;
    private boolean computeHasMissing = true;
    private boolean isMultivariate;
    private boolean computeIsMultivariate = true;
    private boolean hasTimeStamps;
    private boolean computeHasTimeStamps = true;
    private int minLength;
    private boolean computeMinLength = true;
    private int maxLength;
    private boolean computeMaxLength = true;
    private int maxNumDimensions;
    private boolean computeMaxNumDimensions = true;
    private int minNumDimensions;
    private boolean computeMinNumDimensions = true;

    public int getMinNumDimensions() {
        if(computeMinNumDimensions) {
            minNumDimensions = stream().mapToInt(TimeSeriesInstance::getNumDimensions).min().orElse(-1);
            computeMinNumDimensions = false;
        }
        return minNumDimensions;
    }

    @Override public void add(final int i, final TimeSeriesInstance instance) {
        instances.add(i, instance);
    }

    @Override public TimeSeriesInstance remove(final int i) {
        return instances.remove(i);
    }

    @Override public TimeSeriesInstance set(final int i, final TimeSeriesInstance instance) {
        return instances.set(i, instance);
    }

    @Override public int size() {
        return numInstances();
    }

    /** 
     * @return String
     */
    public String getProblemName() {
		return problemName;
	}

    /** 
     * @return boolean
     */
    public boolean hasTimeStamps() {
		if(computeHasTimeStamps) {
		    hasTimeStamps = instances.stream().allMatch(TimeSeriesInstance::hasTimeStamps);
		    computeHasTimeStamps = false;
        }
        return hasTimeStamps;
	}
    
    /** 
     * @return boolean
     */
    public boolean hasMissing() {
        if(computeHasMissing) {
            hasMissing = stream().allMatch(TimeSeriesInstance::hasMissing);
            computeHasMissing = false;
        }
        return hasMissing;
    }

    
    /** 
     * @return boolean
     */
    public boolean isEquallySpaced() {
        if(computeIsEquallySpaced) {
            isEquallySpaced = stream().allMatch(TimeSeriesInstance::isEquallySpaced);
            computeIsEquallySpaced = false;
        }
        return isEquallySpaced;
    }

    
    /** 
     * @return boolean
     */
    public boolean isMultivariate(){
        if(computeIsMultivariate) {
            isMultivariate = stream().allMatch(TimeSeriesInstance::isMultivariate);
            computeIsMultivariate = false;
        }
        return isMultivariate;
    }

    
    /** 
     * @return boolean
     */
    public boolean isEqualLength() {
        return getMaxLength() == getMinLength();
    }

    
    /** 
     * @return int
     */
    public int getMinLength() {
        if(computeMinLength) {
            minLength = stream().mapToInt(TimeSeriesInstance::getMinLength).min().orElse(-1);
            computeMinLength = false;
        }
        return minLength;
    }

    
    /** 
     * @return int
     */
    public int getMaxLength() {
        if(computeMaxLength) {
            maxLength = stream().mapToInt(TimeSeriesInstance::getMaxLength).max().orElse(-1);
            computeMaxLength = false;
        }
        return maxLength;
    }

    
    /** 
     * @return int
     */
    public int numClasses(){
        return classLabels.length;
    }

	
    /** 
     * @return int
     */
    public int getMaxNumChannels() { // todo channels or dimensions?
        if(computeMaxNumDimensions) {
            maxNumDimensions = stream().mapToInt(TimeSeriesInstance::getNumDimensions).max().orElse(-1);
            computeMaxNumDimensions = false;
        }
		return maxNumDimensions;
	}
	    
    /** 
     * @param problemName
     */
    public void setProblemName(String problemName) {
        this.problemName = problemName;
    }
    
    /** 
     * @return String
     */
    public String getDescription() {
        return description;
    }

    
    /** 
     * @param description
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /* End Meta Information */

    private List<TimeSeriesInstance> instances;

    // mapping for class labels. so ["apple","orange"] => [0,1]
    // this could be optional for example regression problems.
    private String[] classLabels;

    private int[] classCounts;
    

    public TimeSeriesInstances() {
        instances = new ArrayList<>();
    }

    public TimeSeriesInstances(final String[] classLabels) {
        this();
        setClassLabels(classLabels);
    }

    public TimeSeriesInstances(final List<List<List<Double>>> raw_data) {
        this();

        for (final List<List<Double>> series : raw_data) {
            instances.add(new TimeSeriesInstance(series));
        }

    }

    
    public TimeSeriesInstances(final List<List<List<Double>>> raw_data, final List<Double> label_indexes) {
        this();

        int index = 0;
        for (final List<List<Double>> series : raw_data) {
            //using the add function means all stats should be correctly counted.
            instances.add(new TimeSeriesInstance(series, label_indexes.get(index++)));
        }

    }

    public TimeSeriesInstances(final double[][][] raw_data) {
        this();

        for (final double[][] series : raw_data) {
            //using the add function means all stats should be correctly counted.
            instances.add(new TimeSeriesInstance(series));
        }

    }

    public TimeSeriesInstances(final double[][][] raw_data, int[] label_indexes) {
        this();

        int index = 0;
        for (double[][] series : raw_data) {
            //using the add function means all stats should be correctly counted.
            instances.add(new TimeSeriesInstance(series, label_indexes[index++]));
        }

    }

    public TimeSeriesInstances(final double[][][] raw_data, int[] label_indexes, String[] labels) {
        this(raw_data, label_indexes);
        classLabels = labels;
    }

    public TimeSeriesInstances(List<TimeSeriesInstance> data, String[] labels) {
        this();
        
        for(TimeSeriesInstance d : data)
            instances.add(d);

        classLabels = labels;

	}

    private void calculateClassCounts() {
        classCounts = new int[classLabels.length];
        for(TimeSeriesInstance inst : instances){
            classCounts[inst.getLabelIndex()]++;
        }
    }

    
    /** 
     * @param labels
     */
    public void setClassLabels(String[] labels) {
        classLabels = labels;

        calculateClassCounts();
    }

    
    /** 
     * @return String[]
     */
    public String[] getClassLabels() {
        return classLabels;
    }

    
    /** 
     * @return String
     */
    public String getClassLabelsFormatted(){
        String output = " ";
        for(String s : classLabels)
            output += s + " ";
        return output;
    }

    
    /** 
     * @return int[]
     */
    public int[] getClassCounts(){
        return classCounts;
    }

    
    /** 
     * @param new_series
     */
    public boolean add(final TimeSeriesInstance new_series) {
        final boolean result = instances.add(new_series);

        //guard for if we're going to force update classCounts after.
        final int labelIndex = new_series.getLabelIndex();
        if(classCounts != null && labelIndex < classCounts.length)
            classCounts[labelIndex]++;
        
        return result;
    }

    
    /** 
     * @return String
     */
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();


        sb.append("Labels: [").append(classLabels[0]);
        for (int i = 1; i < classLabels.length; i++) {
            sb.append(',');
            sb.append(classLabels[i]);
        }
        sb.append(']').append(System.lineSeparator());

        for (final TimeSeriesInstance series : instances) {
            sb.append(series.toString());
            sb.append(System.lineSeparator());
        }

        return sb.toString();
    }
    
    /** 
     * @return double[][][]
     */
    public double[][][] toValueArray() {
        final double[][][] output = new double[instances.size()][][];
        for (int i = 0; i < output.length; ++i) {
            // clone the data so the underlying representation can't be modified
            output[i] = instances.get(i).toValueArray();
        }
        return output;
    }

    
    /** 
     * @return int[]
     */
    public int[] getClassIndexes(){
        int[] out = new int[numInstances()];
        int index=0;
        for(TimeSeriesInstance inst : instances){
            out[index++] = inst.getLabelIndex();
        }
        return out;
    }

    
    /** 
     * @param index
     * @return double[]
     */
    //assumes equal numbers of channels
    public double[] getVSliceArray(int index){
        double[] out = new double[numInstances() * instances.get(0).getNumDimensions()];
        int i=0;
        for(TimeSeriesInstance inst : instances){
            for(TimeSeries ts : inst)
                // if the index isn't always valid, populate with NaN values.
                out[i++] = ts.hasValidValueAt(index) ? ts.get(index) : Double.NaN;
        }

        return out;
    }

    
    /** 
     * @param indexesToKeep
     * @return List<List<List<Double>>>
     */
    public List<List<List<Double>>> getVSliceList(int[] indexesToKeep){
        return getVSliceList(Arrays.stream(indexesToKeep).boxed().collect(Collectors.toList()));
    }

    
    /** 
     * @param indexesToKeep
     * @return List<List<List<Double>>>
     */
    public List<List<List<Double>>> getVSliceList(List<Integer> indexesToKeep){
        List<List<List<Double>>> out = new ArrayList<>(numInstances());
        for(TimeSeriesInstance inst : instances){
            out.add(inst.getVSliceList(indexesToKeep));
        }

        return out;
    }

    
    /** 
     * @param indexesToKeep
     * @return double[][][]
     */
    public double[][][] getVSliceArray(int[] indexesToKeep){
        return getVSliceArray(Arrays.stream(indexesToKeep).boxed().collect(Collectors.toList()));
    }

    
    /** 
     * @param indexesToKeep
     * @return double[][][]
     */
    public double[][][] getVSliceArray(List<Integer> indexesToKeep){
        double[][][] out = new double[numInstances()][][];
        int i=0;
        for(TimeSeriesInstance inst : instances){
            out[i++] = inst.getVSliceArray(indexesToKeep);
        }

        return out;
    }

    
    /** 
     * @param dim
     * @return double[][]
     */
    //assumes equal numbers of channels
    public double[][] getHSliceArray(int dim){
        double[][] out = new double[numInstances()][];
        int i=0;
        for(TimeSeriesInstance inst : instances){
            // if the index isn't always valid, populate with NaN values.
            out[i++] = inst.getSingleHSliceArray(dim);
        }
        return out;
    }
    
    
    /** 
     * @param indexesToKeep
     * @return List<List<List<Double>>>
     */
    public List<List<List<Double>>> getHSliceList(int[] indexesToKeep){
        return getVSliceList(Arrays.stream(indexesToKeep).boxed().collect(Collectors.toList()));
    }
    
    
    /** 
     * @param indexesToKeep
     * @return List<List<List<Double>>>
     */
    public List<List<List<Double>>> getHSliceList(List<Integer> indexesToKeep){
        List<List<List<Double>>> out = new ArrayList<>(numInstances());
        for(TimeSeriesInstance inst : instances){
            out.add(inst.getHSliceList(indexesToKeep));
        }

        return out;
    }
    
    
    /** 
     * @param indexesToKeep
     * @return double[][][]
     */
    public double[][][] getHSliceArray(int[] indexesToKeep){
        return getHSliceArray(Arrays.stream(indexesToKeep).boxed().collect(Collectors.toList()));
    }

    
    /** 
     * @param indexesToKeep
     * @return double[][][]
     */
    public double[][][] getHSliceArray(List<Integer> indexesToKeep){
        double[][][] out = new double[numInstances()][][];
        int i=0;
        for(TimeSeriesInstance inst : instances){
            out[i++] = inst.getHSliceArray(indexesToKeep);
        }

        return out;
    }

    
    /** 
     * @param i
     * @return TimeSeriesInstance
     */
    public TimeSeriesInstance get(final int i) {
        return instances.get(i);
    }
	
    /** 
     * @return int
     */
    public int numInstances() {
		return instances.size();
    }

    
    /** 
     * @return int
     */
    @Override
    public int hashCode(){
        return this.instances.hashCode();
    }


    public Map<Integer, Integer> getHistogramOfLengths(){
        Map<Integer, Integer> out = new TreeMap<>();
        for(TimeSeriesInstance inst : instances){
            for(TimeSeries ts : inst){
                out.merge(ts.getSeriesLength(), 1, Integer::sum);
            }
        }

        return out;
    }

    
}
