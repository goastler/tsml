package tsml.data_containers;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Data structure able to handle unequal length, unequally spaced, univariate or
 * multivariate time series.
 */
public class TimeSeriesInstances extends AbstractList<TimeSeriesInstance> {
    
    private List<TimeSeriesInstance> instances = new ArrayList<>();
    // a listener per instance in this object. These listen for changes in their corresponding instance, then use these changes to recompute metadata / stats
    private List<TimeSeriesInstanceListener> instanceListeners = new ArrayList<>();
    // mapping for class labels. so ["apple","orange"] => [0,1]
    private LabelEncoder labelEncoder;
    /* Meta Information */
    private List<Integer> classCounts;
    private boolean computeClassCounts = true;
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
    
    public int getNumClasses() {
        return getClassesList().size();
    }
    
    public int[] getClassCountsArray() {
        return getClassCountsList().stream().mapToInt(i -> i).toArray();
    }
    
    public List<Integer> getClassCountsList() {
        if(computeClassCounts) {
            classCounts = new ArrayList<>(getNumClasses());
            for(TimeSeriesInstance instance : instances) {
                int labelIndex = instance.getClassLabelIndex();
                classCounts.set(labelIndex, classCounts.get(labelIndex) + 1);
            }
        }
        return classCounts;
    }

    public int getMinNumDimensions() {
        if(computeMinNumDimensions) {
            minNumDimensions = stream().mapToInt(TimeSeriesInstance::getNumDimensions).min().orElse(-1);
            computeMinNumDimensions = false;
        }
        return minNumDimensions;
    }

    private TimeSeriesInstanceListener buildListener(int i, TimeSeriesInstance instance) {
        return new TimeSeriesInstanceListener() {

            @Override public void onClassChange() {
                super.onClassChange();
                setComputeMetadataInstances();
            }

            @Override public void onValueChange() {
                super.onValueChange();
                setComputeMetadataValues();
            }

            @Override public void onDimensionChange() {
                super.onDimensionChange();
                setComputeMetadataDimensions();
            }

            @Override public void onTimeStampChange() {
                super.onTimeStampChange();
                setComputeMetadataTimeStamps();
            }
        };
    }
    
    private void setComputeMetadata() {
        setComputeMetadataInstances();
        setComputeMetadataTimeStamps();
        setComputeMetadataValues();
        setComputeMetadataDimensions();
    }
    
    private void setComputeMetadataTimeStamps() {
        computeHasTimeStamps = true;
        computeIsEquallySpaced = true;
    }
    
    private void setComputeMetadataDimensions() {
        computeIsMultivariate = true;
        computeMaxNumDimensions = true;
        computeMinNumDimensions = true;
    }
    
    private void setComputeMetadataValues() {
        computeHasMissing = true;
        computeMaxLength = true;
        computeMinLength = true;
    }
    
    private void setComputeMetadataInstances() {
        computeClassCounts = true;
    }
    
    @Override public void clear() {
        super.clear();
        // remove the listeners
        for(int i = 0; i < instances.size(); i++) {
            TimeSeriesInstance instance = instances.get(i);
            TimeSeriesInstanceListener listener = instanceListeners.get(i);
            instance.removeListener(listener);
        }
        // initialise the dimensions and listeners for each dimension to empty
        instances = new ArrayList<>();
        instanceListeners = new ArrayList<>();
        // set metadata to be recomputed
        setComputeMetadata();
        // don't clear the class labels, these are managed through the setClassLabels function (and should persist beyond clear operations)
    }
    
    private void checkLabelEncoderMatch(TimeSeriesInstance instance) {
        // must check that instance's class labels is the same as our set of class labels
        // the problem here is the instance being added may have a different set of class labels than this object does. Therefore, must check the equality. If they're not equal then reject the instance as recomputing the class label indices is a) inefficient and b) may lead to unexpected behaviour, as a class may more indices and the user may not realise. Use the explicit setClassLabels method to do this.
        if(!labelEncoder.equals(instance.getLabelEncoder())) {
            throw new IllegalArgumentException("classes do not match");
        }
    }
    
    @Override public void add(final int i, final TimeSeriesInstance instance) {
        checkLabelEncoderMatch(instance);
        // build a listener to listen for changes to the new instance
        TimeSeriesInstanceListener listener = buildListener(i, instance);
        instanceListeners.add(i, listener);
        // store that instance in a position corresponding to the position of the new instance
        instances.add(i, instance);
        // set the listener to listen to changes in the instance
        instance.addListener(listener);
        // set metadata to be recomputed
        setComputeMetadata();
    }

    @Override public TimeSeriesInstance remove(final int i) {
        // remove the instance
        TimeSeriesInstance removed = instances.remove(i);
        // remove the corresponding listener
        TimeSeriesInstanceListener listener = instanceListeners.remove(i);
        // disable listening to the instance
        removed.removeListener(listener);
        // set metadata to be recomputed
        setComputeMetadata();
        return removed;
    }

    @Override public TimeSeriesInstance set(final int i, final TimeSeriesInstance instance) {
        checkLabelEncoderMatch(instance);
        // get the current listener
        TimeSeriesInstanceListener currentListener = instanceListeners.get(i);
        // get the current instance
        TimeSeriesInstance currentInstance = instances.get(i);
        // disable listening to the current instance
        currentInstance.removeListener(currentListener);
        // build a new listener
        TimeSeriesInstanceListener listener = buildListener(i, instance);
        // enable listening to the new instance
        instance.addListener(listener);
        // replace the listener and instance
        instanceListeners.set(i, listener);
        TimeSeriesInstance previous = instances.set(i, instance);
        // set metadata to be recomputed
        setComputeMetadata();
        return previous;
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
    public int getMaxNumDimensions() {
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
    
    public void setInstancesLabelledRawData(List<? extends List<? extends List<Double>>> data, List<?> labels) {
        // check data shape
        if(labels.size() != data.size()) {
            throw new IllegalArgumentException("expected a label per instance/row of data");
        }
        // find the unique class labels
        // sort the unique labels as they are in seen-first order, i.e. [a,c,b] --> [a,b,c]
        List<String> uniqueLabels =
                labels.stream().map(String::valueOf).distinct().sorted().collect(Collectors.toList());
        // map the class labels to their indices
        Map<String, Integer> labelIndexMap = new HashMap<>();
        for(int i = 0; i < uniqueLabels.size(); i++) {
            String label = uniqueLabels.get(i);
            labelIndexMap.put(label, i);
        }
        // setup new instances obj
        setClasses(uniqueLabels);
        // convert labels to indices
        List<Integer> labelIndices =
                labels.stream().map(label -> labelIndexMap.get(label.toString())).collect(Collectors.toList());
        // set data given ts data and label indices
        setInstancesIndexLabelledRawData(data, labelIndices, uniqueLabels);
    }

    /**
     * Set the label for each instance in this object.
     * @param labels
     */
    public void setLabels(List<String> labels) {
        List<Integer> indices = labels.stream().map(classLabelIndexMap::get).collect(Collectors.toList());
        for(int i = 0; i < labels.size(); i++) {
            Integer index = indices.get(i);
            if(index == null) {
                throw new IllegalArgumentException("class label " + labels.get(i) + " not in class labels set");
            }
        }
    }

    /**
     * Set the class label index for each instance in this object.
     * @param labels
     */
    public void setClassLabelIndices(List<Integer> labels) {
        if(labels.size() != size()) {
            throw new IllegalArgumentException("expected one label per instance");
        }
        for(int i = 0; i < size(); i++) {
            TimeSeriesInstance instance = get(i);
            int index = labels.get(i);
            instance.setClassLabelIndex(index);
        }
    }
    
    private void setInstancesIndexLabelledRawData(List<? extends List<? extends List<Double>>> data, List<Integer> labelIndices, List<String> uniqueLabels) {
        
    }
    
    public void setInstancesRaw(List<? extends List<? extends List<Double>>> instancesData) {
        setInstances(instancesData.stream().map(TimeSeriesInstance::fromData).collect(Collectors.toList()));
    }
    
    public void setInstances(List<? extends TimeSeriesInstance> instances) {
        clear();
        addAll(instances);
    }
    
    public TimeSeriesInstances() {
        
    }
    
    public static TimeSeriesInstances fromClassLabels(String[] labels) {
        return fromClassLabels(Arrays.asList(labels));
    }
    
    public static TimeSeriesInstances fromClassLabels(List<String> labels) {
        TimeSeriesInstances instances = new TimeSeriesInstances();
        instances.setClasses(labels);
        return instances;
    }
    
    public static TimeSeriesInstances fromData(List<? extends List<? extends List<Double>>> data) {
        TimeSeriesInstances instances = new TimeSeriesInstances();
        instances.setInstancesRaw(data);
        return instances;
    }
    
    public static TimeSeriesInstances fromClassifiedData(List<? extends List<? extends List<Double>>> data, List<?> labels) {
        
        return instances;
    }
    
    public static TimeSeriesInstances fromData(double[][][] data) {
        
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
            instances.add(new TimeSeriesInstance(series, label_indexes[index++], null));
        }

    }

    public TimeSeriesInstances(final double[][][] raw_data, int[] label_indexes, String[] labels) {
        this(raw_data, label_indexes);
        classes = labels;
    }

    public TimeSeriesInstances(List<TimeSeriesInstance> data, String[] labels) {
        this();
        
        for(TimeSeriesInstance d : data)
            instances.add(d);

        classes = labels;

	}

    
    /** 
     * @param labels
     */
    public void setClasses(String[] labels) {
        setClasses(new ArrayList<>(Arrays.asList(labels)));
    }

    /**
     * ***CAUTION*** Set the class labels, re-encoding indices of labels as required and potentially causing BREAKING changes - proceed with care!
     * 
     * For example, suppose this object is already populated with TimeSeriesInstance objects, each with a single class. Suppose the class labels list for this object is ["dog", "cat"]. If this function were used to set the class labels to ["dog", "rabbit", "cat"] then any "cat" labelled TimeSeriesInstance objects must have their indices adjusted to account for "cat" moving from index 1 to index 2 - thus re-encoding the class label indices. This can be unexpected for users so BE CAREFUL.
     *
     * @param classLabels
     */
    public void setClasses(List<String> classLabels) {
        LabelEncoder labelEncoder = new LabelEncoder(classLabels);
        setLabelEncoder(labelEncoder);
    }

    /**
     * ***CAUTION*** Set the class labels, re-encoding indices of labels as required and potentially causing BREAKING changes - proceed with care!
     *
     * For example, suppose this object is already populated with TimeSeriesInstance objects, each with a single class. Suppose the class labels list for this object is ["dog", "cat"]. If this function were used to set the class labels to ["dog", "rabbit", "cat"] then any "cat" labelled TimeSeriesInstance objects must have their indices adjusted to account for "cat" moving from index 1 to index 2 - thus re-encoding the class label indices. This can be unexpected for users so BE CAREFUL.
     *
     * @param labelEncoder
     */
    public void setLabelEncoder(LabelEncoder labelEncoder) {
        // go through the instances adjusting any labels which have changed
        for(TimeSeriesInstance instance : this) {
            String label = instance.getClassLabel();
            if(label == null) {
                // instance is unclassified so don't do anything other than pass on the new labels
                instance.setLabelEncoder(labelEncoder);
            } else {
                // valid label and label index, so set them
                instance.setClassLabel(label, labelEncoder);
            }
        }
    }
    
    /** 
     * @return String[]
     */
    public List<String> getClassesList() {
        return labelEncoder.getClasses();
    }
    
    public String[] getClassesArray() {
        return getClassesList().toArray(new String[0]);
    }
    
    /** 
     * @return String
     */
    public String getClassLabelsFormatted(){
        String output = " ";
        for(String s : getClassesList())
            output += s + " ";
        return output;
    }
    
    /** 
     * @return String
     */
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();


        sb.append("Labels: [").append(classes[0]);
        for (int i = 1; i < classes.length; i++) {
            sb.append(',');
            sb.append(classes[i]);
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
    public int[] getClassLabelIndexes(){
        int[] out = new int[numInstances()];
        int index=0;
        for(TimeSeriesInstance inst : instances){
            out[index++] = inst.getClassLabelIndex();
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
            out[i++] = inst.getHSliceArray(dim);
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
