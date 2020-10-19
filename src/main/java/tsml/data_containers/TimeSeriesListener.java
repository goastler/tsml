package tsml.data_containers;

public class TimeSeriesListener {
    
    // todo these operations could be broken up into more exact time series operations, e.g. removal of a value, as certain metadata / stats may not require recomputation for these operations

    /**
     * Notify that the values in a time series has changed (i.e. added / removed / set to something different).
     */
    public void onValueChange() {
        
    }

    /**
     * Notify that the time stamps in a time series has changed (i.e. added / removed / set to something different).
     */
    public void onTimeStampChange() {
        
    }
}
