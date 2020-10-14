package tsml.data_containers;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;
import java.util.stream.Stream;

/**
 * Class to store a time series. The series can have different indices (time stamps) and store missing values (NaN).
 *
 * The model for the indexes is the first is always zero the other indexes are in units of md.increment
 * Hopefully most of this can be encapsulated, so if the data has equal increments then indices is null and the user

 * */
public class TimeSeries
        extends AbstractList<Double> 
                {

    public static double DEFAULT_VALUE = Double.NaN;

    // the list of channels / dimensions
    private List<Double> series;
    private List<Double> indices;
    // todo is this metadata stuff redundant now? Should be able to mirror the setup in TimeSeriesInstance for metadata handling
    private MetaData md;


    public TimeSeries(double[] series){
        setSeries(series);
    }
    
    public TimeSeries(List<Double> series) {
        setSeries(series);
    }
    
    /** 
     * @param ind
     */
    public void setIndices(double[] indices){
        setIndices(DoubleStream.of(indices).boxed().collect(Collectors.toList()));
    }
    
    public void setIndices(List<Double> indices) {
        this.indices = indices;
    }
    
    public void setSeries(double[] series) {
        setSeries(DoubleStream.of(series).boxed().collect(Collectors.toList()));
    }
    
    public void setSeries(List<Double> series) {
        this.series = series;
    }

    @Override public void add(final int i, final Double value) {
        if(indices != null) {
            // todo handle indices
        }
        series.add(i, value);
    }

    @Override public Double remove(final int i) {
        if(indices != null) {
            indices.remove(i);
        }
        return series.remove(i);
    }

    @Override public Double set(final int i, final Double value) {
        return series.set(i, value);
    }

                    /** 
     * @return int
     */
    public int getSeriesLength(){
        return series.size();
    }
    
    /** 
     * @param i
     * @return boolean
     */
    public boolean hasValidValueAt(int i){
        //test whether its out of range, or NaN
        boolean output = i < series.size() &&
                         Double.isFinite(series.get(i));
        return output;
    }

    
    /** 
     * @param i
     * @return double
     */
    public Double get(int i){
        return series.get(i);
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
        return series.subList(start, end);
    }

    
    /** 
     * @param start
     * @param end
     * @return double[]
     */
    public double[] getSlidingWindowArray(int start, int end){
        return series.subList(start, end).stream().mapToDouble(Double::doubleValue).toArray();
    }

    
    /** 
     * @return List<Double>
     */
    public List<Double> getSeries(){ return series;}
    
    /** 
     * @return List<Double>
     */
    public List<Double> getIndices(){ return indices;}

    private class MetaData{
        String name;
        Date startDate;
        double increment;  //Base unit to be ....... 1 day?

    }

    
    /** 
     * @return String
     */
    @Override
    public String toString(){
        StringBuilder sb = new StringBuilder();

        for(double val : series){
            sb.append(val).append(',');
        }

        return sb.toString();
    }


    public int size() {
        return series.size();
    }

    /** 
     * @return double[]
     */

	public double[] toArrayPrimitive() {
		return getSeries().stream().mapToDouble(Double::doubleValue).toArray();
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
        return this.series.hashCode();
    }

    @Override public boolean equals(final Object o) {
        if(this == o) {
            return true;
        }
        if(!(o instanceof TimeSeries)) {
            return false;
        }
        final TimeSeries series1 = (TimeSeries) o;
        return series.equals(series1.series);
    }

                    /** 
     * @param args
     */
    public static void main(String[] args) {
        TimeSeries ts = new TimeSeries(new double[]{1,2,3,4}) ;
    }


}
