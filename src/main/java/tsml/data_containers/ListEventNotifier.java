package tsml.data_containers;

public interface ListEventNotifier<A> {
    boolean addListEventListener(ListEventListener<A> listener);
    
    boolean removeListEventListener(ListEventListener<A> listener);
}
