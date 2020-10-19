package tsml.data_containers;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;

/**
 * Class to store a time series. The series can have different indices (time stamps) and store missing values (NaN).
 *
 * The model for the indexes is the first is always zero the other indexes are in units of md.increment
 * Hopefully most of this can be encapsulated, so if the data has equal increments then indices is null and the user

 * */
public class TimeSeries
        extends AbstractList<Double> implements Notifier<TimeSeriesListener> {

    // The default value if an entry in the time series is missing, i.e. [1, NaN, 3]
    public static double DEFAULT_VALUE = Double.NaN;

    // todo is this metadata stuff redundant now? Should be able to mirror the setup in TimeSeriesInstance for metadata handling
    private MetaData md;
    private class MetaData{
        String name;
        Date startDate;
        double increment;  //Base unit to be ....... 1 day?

    }
    // the list of channels / dimensions
    private List<Double> values = new ArrayList<>();
    // todo iron out how the timestamps stuff is working exactly
    private List<Double> timeStamps = null;
    // the list of event listeners. These are fired on modification of this time series to manage metadata changes in containing classes (e.g. TimeSeriesInstance)
    // initialising to hold a single listener, as this is the norm for TimeSeriesInstance
    private final Set<TimeSeriesListener> listeners = new HashSet<>(1, 1f);
    // metadata bits
    private boolean hasMissing;
    private boolean computeHasMissing = true;
    private boolean isEquallySpaced;
    private boolean computeIsEquallySpaced = true;

    /**
     * Construct a default empty time series
     */
    public TimeSeries() {
        this(new ArrayList<>());
    }

    /**
     * Construct a time series populated with values
     * @param values
     */
    public TimeSeries(double[] values){
        this(values, null);
    }

    /**
     * Construct a populated time series
     * @param values
     * @param timeStamps
     */
    public TimeSeries(double[] values, double[] timeStamps) {
        setValues(values);
        setTimeStamps(timeStamps);
    }

    /**
     * Construct a populated time series
     * @param values
     */
    public TimeSeries(List<Double> values) {
        this(values, null);
    }

    /**
     * Construct a populated time series
     * @param values
     * @param timeStamps
     */
    public TimeSeries(List<Double> values, List<Double> timeStamps) {
        setValues(values);
        setTimeStamps(timeStamps);
    }

    /**
     * Are the time stamps equally spaced?
     * @return
     */
    public boolean isEquallySpaced() {
        if(computeIsEquallySpaced) {
            // less than or equal to 2 time stamps are guaranteed to be equally spaced
            if(size() <= 2) {
                isEquallySpaced = true;
            } else {
                // otherwise work out the spacing
                Double previous = get(0);
                Double current = get(1);
                final double spacing = current - previous;
                // check if the spacing of each are equal
                isEquallySpaced = true;
                for(int i = 1; i < size() && isEquallySpaced; i++) {
                    previous = current;
                    current = get(i);
                    isEquallySpaced = current - previous == spacing;
                }
            }
            computeIsEquallySpaced = false;
        }
        return isEquallySpaced;
    }

    /**
     * 
     * @return
     */
    public boolean hasTimeStamps() {
        return timeStamps != null;
    }

    /**
     * Are there any missing values?
     * @return
     */
    public boolean hasMissing() {
        if(computeHasMissing) {
            hasMissing = contains(DEFAULT_VALUE);
            computeHasMissing = false;
        }
        return hasMissing;
    }

    /**
     * Add a listener to get notified about modifications.
     * @param listener
     * @return
     */
    public boolean addListener(TimeSeriesListener listener) {
        return listeners.add(listener);
    }

    /**
     * Remove a listener to stop getting notified about modifications.
     * @param listener
     * @return
     */
    public boolean removeListener(TimeSeriesListener listener) {
        return listeners.remove(listener);
    }
    
    /** 
     * @param timeStamps
     */
    public void setTimeStamps(double[] timeStamps){
        List<Double> list = null;
        // if time stamps are null then don't both converting to list
        if(timeStamps != null) {
            list = DoubleStream.of(timeStamps).boxed().collect(Collectors.toList());
        }
        setTimeStamps(list);
    }
    
    public void setTimeStamps(List<Double> timeStamps) {
        if(timeStamps != null) {
            // check the time stamps are in ascending order
            // the first timestamp should never be smaller as there's no timestamp before the first, so init previous to neg inf
            Double previous = Double.NEGATIVE_INFINITY;
            for(Double timeStamp : timeStamps) {
                // check the time stamp is no smaller than the previous
                if(previous > timeStamp) {
                    throw new IllegalArgumentException("time stamps not in ascending order: " + previous + " > " + timeStamp);
                }
                // current time stamp becomes the previous
                previous = timeStamp;
            }
            this.timeStamps = timeStamps;
            listeners.forEach(TimeSeriesListener::onValueChange);
        } else {
            // disable time stamps
            timeStamps = null;
        }
    }
    
    public void setValues(double[] values) {
        setValues(DoubleStream.of(values).boxed().collect(Collectors.toList()));
    }

    /**
     * Set the values of this time series to a new list of values.
     * @param newSeries the values to copy. These are copied out of the list so modifications can be managed.
     */
    public void setValues(List<Double> newSeries) {
        clear();
        addAll(newSeries);
    }

    /**
     * Clear all values and time stamps
     */
    @Override public void clear() {
        super.clear();
        // clear the values
        this.values = new ArrayList<>();
        // remove the timestamps
        this.timeStamps = null;
        // need to compute metadata
        setComputeMetadataValues();
        // alert listeners that changes have happened. Must do this after setting compute metadata above as these may rely on the meta data being recomputed
        listeners.forEach(TimeSeriesListener::onValueChange);
        listeners.forEach(TimeSeriesListener::onTimeStampChange);
    }

    /**
     * Add a single value to the time series at a given index.
     * @param i
     * @param value
     */
    @Override public void add(final int i, final Double value) {
        // todo handle timestamps when timestamp structure has been ironed out
        // must reassess data for missing values as more data has been added
        computeHasMissing = true;
        values.add(i, value);
        // recompute metadata
        setComputeMetadataValues();
        // alert listeners that changes have happened. Must do this after setting compute metadata above as these may rely on the meta data being recomputed
        listeners.forEach(TimeSeriesListener::onValueChange);
    }

    /**
     * Remove a single value from this time series given an index.
     * @param i
     * @return
     */
    @Override public Double remove(final int i) {
        // todo handle timestamps when timestamp structure has been ironed out
//        timeStamps.remove(i);
        // may have removed missing values so must recompute
        computeHasMissing = true;
        final Double removed = values.remove(i);
        // recompute the metadata
        setComputeMetadataValues();
        // alert listeners that changes have happened. Must do this after setting compute metadata above as these may rely on the meta data being recomputed
        listeners.forEach(TimeSeriesListener::onValueChange);
        return removed;
    }

    /**
     * Set a specific value of the time series to a given value, given an index.
     * @param i the index at which to replace a value
     * @param value the new value
     * @return the previous value
     */
    @Override public Double set(final int i, final Double value) {
        // todo setter with timestamp
        // may be setting missing values / unsetting / already have missing values so recompute hasMissing
        computeHasMissing = true;
        final Double previous = values.set(i, value);
        // recompute metadata
        setComputeMetadataValues();
        // alert listeners that changes have happened. Must do this after setting compute metadata above as these may rely on the meta data being recomputed
        listeners.forEach(TimeSeriesListener::onValueChange);
        return previous;
    }

    /**
     * Set compute all metadata
     */
    private void setComputeMetadata() {
        setComputeMetadataTimeStamps();
        setComputeMetadataValues();
    }

    /**
     * Set compute only value related metadata
     */
    private void setComputeMetadataValues() {
        computeHasMissing = true;
    }

    /**
     * Set compute only time stamp related metadata
     */
    private void setComputeMetadataTimeStamps() {
        computeIsEquallySpaced = true;
    }

    /** 
     * 
     * @return int
     */
    public int getSeriesLength(){
        return values.size();
    }
    
    /**
     * Test whether a value is valid, i.e. not missing. 
     * @param i
     * @return boolean
     */
    public boolean hasValidValueAt(int i){
        //test whether its out of range, or NaN
        boolean output = i < values.size() &&
                         Double.isFinite(values.get(i));
        return output;
    }

    
    /** 
     * @param i
     * @return double
     */
    public Double get(int i){
        return values.get(i);
    }

    
    /** 
     * @param i
     * @return double
     */
    public Double getOrDefault(int i){
        return hasValidValueAt(i) ? get(i) : DEFAULT_VALUE;
    }
    
    /** 
     * @param start
     * @param end
     * @return List<Double>
     */
    public List<Double> getSlidingWindow(int start, int end){
        return values.subList(start, end);
    }

    
    /** 
     * @param start
     * @param end
     * @return double[]
     */
    public double[] getSlidingWindowArray(int start, int end){
        return values.subList(start, end).stream().mapToDouble(Double::doubleValue).toArray();
    }

    
    /** 
     * @return List<Double>
     */
    public List<Double> getValues(){ return values;}
    
    /** 
     * @return List<Double>
     */
    public List<Double> getTimeStamps(){ return timeStamps;}
    
    /** 
     * @return String
     */
    @Override
    public String toString(){
        
        if(isEmpty()) {
            return "";
        }
        
        StringBuilder sb = new StringBuilder();

        for(int i = 0, seriesSize = values.size(); i < seriesSize - 1; i++) {
            final double val = values.get(i);
            sb.append(val).append(',');
        }
        sb.append(values.get(values.size() - 1));

        return sb.toString();
    }


    public int size() {
        return values.size();
    }

    /** 
     * @return double[]
     */

	public double[] toArrayPrimitive() {
		return getValues().stream().mapToDouble(Double::doubleValue).toArray();
    }

    
    /** 
     * this is useful if you want to delete a column/truncate the array, but without modifying the original dataset.
     * @param indexesToRemove
     * @return List<Double>
     */
    public List<Double> toListWithoutIndexes(List<Integer> indexesToRemove){
        //if the current index isn't in the removal list, then copy across.
        List<Double> out = new ArrayList<>(this.getSeriesLength() - indexesToRemove.size());
        for(int i=0; i<this.getSeriesLength(); ++i){
            if(!indexesToRemove.contains(i))
                out.add(getOrDefault(i));
        }

        return out;
    }

    
    /** 
     * @param indexesToKeep
     * @return double[]
     */
    public double[] toListWithoutIndexes(int[] indexesToKeep){
        return toArrayWithoutIndexes(Arrays.stream(indexesToKeep).boxed().collect(Collectors.toList()));
    }

    
    /** 
     * @param indexesToRemove
     * @return double[]
     */
    public double[] toArrayWithoutIndexes(List<Integer> indexesToRemove){
        return toListWithoutIndexes(indexesToRemove).stream().mapToDouble(Double::doubleValue).toArray();
    }
    
    
    /** 
     * this is useful if you want to slice a column/truncate the array, but without modifying the original dataset.
     * @param indexesToKeep
     * @return List<Double>
     */
    public List<Double> toListWithIndexes(List<Integer> indexesToKeep){
        //if the current index isn't in the removal list, then copy across.
        List<Double> out = new ArrayList<>(indexesToKeep.size());
        for(int i=0; i<this.getSeriesLength(); ++i){
            if(indexesToKeep.contains(i))
                out.add(getOrDefault(i));
        }

        return out;
    }

    
    /** 
     * @param indexesToKeep
     * @return double[]
     */
    public double[] toArrayWithIndexes(int[] indexesToKeep){
        return toArrayWithIndexes(Arrays.stream(indexesToKeep).boxed().collect(Collectors.toList()));
    }

    
    /** 
     * @param indexesToKeep
     * @return double[]
     */
    public double[] toArrayWithIndexes(List<Integer> indexesToKeep){
        return toListWithIndexes(indexesToKeep).stream().mapToDouble(Double::doubleValue).toArray();
    }

    
    /** 
     * @return int
     */
    @Override
    public int hashCode(){
        return this.values.hashCode();
    }

    @Override public boolean equals(final Object o) {
        if(this == o) {
            return true;
        }
        if(!(o instanceof TimeSeries)) {
            return false;
        }
        final TimeSeries series1 = (TimeSeries) o;
        // todo consider time stamps
        return values.equals(series1.values);
    }

                    /** 
     * @param args
     */
    public static void main(String[] args) {
        TimeSeries ts = new TimeSeries(new double[]{1,2,3,4});
        ts.addListener(new TimeSeriesListener() {
            @Override public void onValueChange() {
                super.onValueChange();
                System.out.println("value(s) mutated");
            }

            @Override public void onTimeStampChange() {
                super.onTimeStampChange();
                System.out.println("timestamp(s) mutated");
            }
        });
        System.out.println(ts);
        ts.add(5d);
        System.out.println(ts);
        ts.add(0, 6d);
        System.out.println(ts);
        ts.set(3, -7d);
        System.out.println(ts);
        ts.remove(2);
        System.out.println(ts);
        ts.setTimeStamps(new double[] {1,2,3,4});
        System.out.println(ts);
    }


}
