package velox.api.layer1.simplified;

/**
 * In addition to historical data, you will also be notified on transition
 * between historical data and live data. This adds more computations compared
 * to just getting historical data, since now indicator has to be recalculated
 * on each rewind in replay.
 */
public interface HistoricalModeListener extends HistoricalDataListener {
    void onRealtimeStart();
}
