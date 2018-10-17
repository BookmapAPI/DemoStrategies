package velox.api.layer1.simplified;

import velox.api.layer1.data.TradeInfo;

/**
 * Some initialization data about events that happened shortly before the
 * initialization moment (note, that for historical-enabled indicators many
 * fields will be empty)
 */
public class InitialState {
    /** Size of a last trade */
    int lastTradeSize = 0;
    /** Price where the last trade occurred */
    double lastTradePrice = Double.NaN;
    /**
     * Additional information about trade (including aggressor). Null if no trades
     * happened yet.
     */
    TradeInfo tradeInfo;

    public int getLastTradeSize() {
        return lastTradeSize;
    }

    public double getLastTradePrice() {
        return lastTradePrice;
    }
    
    public TradeInfo getTradeInfo() {
        return tradeInfo;
    }
}
