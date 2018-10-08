package velox.api.layer1.simplified;

import velox.api.layer1.data.StatusInfo;

/**
 * Provides position info. Currently only works in realtime
 * ({@link HistoricalDataListener} should <b>not</b> be implemented if you want
 * this to work)
 */
public interface PositionListener {
    /**
     * Called when instrument status information changes (PnL, number of open
     * orders, position, etc).
     *
     * @param statusInfo
     *            status information
     */
    void onPositionUpdate(StatusInfo statusInfo);
}
