package velox.api.layer1.simplified;

import velox.api.layer1.layers.utils.OrderBook;

/** Get bars and order book snapshot with fixed interval */
public interface BarDataListener extends IntervalListener {
    /**
     * Called when bar is ready. Bar width is specified via
     * {@link IntervalListener#getInterval()}
     */
    void onBar(OrderBook orderBook, Bar bar);
    
    /**
     * {@link BarDataListener} provides default empty implementation for
     * {@link IntervalListener#onInterval()} since you get
     * {@link #onBar(OrderBook, Bar)} called at the same time. But you still can override and use
     * this method.
     */
    @Override
    default void onInterval() {
    }
}
