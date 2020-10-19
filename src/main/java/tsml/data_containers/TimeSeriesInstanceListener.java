package tsml.data_containers;

public class TimeSeriesInstanceListener {

    /**
     * The class label or regression target has been changed.
     */
    public void onClassChange() {
        
    }
    
    /**
     * One or more values in one or more dimensions of a time series instance has changed.
     */
    public void onValueChange() {
        
    }

    /**
     * One or more time stamps in one or more dimensions of a time series instance has changed.
     */
    public void onTimeStampChange() {
        
    }

    /**
     * The dimensions have changed, i.e. been added / removed
     */
    public void onDimensionChange() {
        
    }
}
