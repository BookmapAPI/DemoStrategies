package velox.api.layer1.simplified;

import velox.api.layer1.layers.utils.OrderBook;

/** Get bars and order book snapshot with fixed interval */
public interface BarDataListener {
    /** Called when bar is ready */
    void onBar(OrderBook orderBook, Bar bar);

    /**
     * Return desired bar width in nanoseconds. Should always be larger than
     * {@link Intervals#MIN_INTERVAL}. You can user other constants in
     * {@link Intervals} class for common intervals, but you are not required to.
     */
    long getBarInterval();
}
