package tsml.data_containers;

public interface Notifier<A> {
    boolean addListener(A listener);
    
    boolean removeListener(A listener);
}
