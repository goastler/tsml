package tsml.data_containers;

/**
 * Implementation to notify of actions performed on the list. Only the 4 mutability actions are notified for as all other actions work above these.
 *
 * Each action has a before and after with relevant parameters according to the operation's modification before and after states.
 *
 * @param <A>
 */
public class ListEventListener<A> {
    /**
     * Fired when add() called
     * @param i
     * @param newItem
     */
    public void onAdd(int i, A newItem) {
        onMutate();
    }

    /**
     * Fired when remove() called
     * @param i
     * @param removedItem
     */
    public void onRemove(int i, A removedItem) {
        onMutate();
    }

    /**
     * Fired when set() called
     * @param i
     * @param previousItem
     */
    public void onSet(int i, A previousItem, A nextItem) {
        onMutate();
    }

    /**
     * Fired when any of set, add or remove called
     */
    public void onMutate() {
        
    }
}
