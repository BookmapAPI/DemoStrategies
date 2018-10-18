package velox.api.layer1.simplified;

public interface IntervalListener {

    /**
     * Return desired interval width in nanoseconds. Should always be larger than
     * {@link Intervals#MIN_INTERVAL}. You can use other constants in
     * {@link Intervals} class for common intervals, but you are not required to.
     */
    long getInterval();

    /**
     * Called with frequency set by {@link #getInterval()}. Useful as replacement
     * for
     * {@link BarDataListener#onBar(velox.api.layer1.layers.utils.OrderBook, Bar)}
     * when you don't really need the bar itself and just want a timer functionality
     * (keep in mind, that indicator time is not bound to computer clock, so using
     * general-purpose timers won't work in many cases)
     */
    void onInterval();

}