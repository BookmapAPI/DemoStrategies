package velox.api.layer1.simplified;

import velox.api.layer1.data.BalanceInfo;

/**
 * Provides balance info. Currently only works in realtime
 * ({@link HistoricalDataListener} should <b>not</b> be implemented)
 */
public interface BalanceListener {

    /**
     * Called when account balance information changes
     *
     * @param balanceInfo
     *            account balance information
     */
    void onBalance(BalanceInfo balanceInfo);
}
