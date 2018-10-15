package velox.api.layer1.simplified;

/**
 * Some initialization data about events that happened shortly before the
 * initialization moment (note, that for historical-enabled indicators many
 * fields will be empty)
 */
public class InitialState {
    boolean isLastTradeBid;
    int lastTradeSize = 0;
    double lastTradePrice = Double.NaN;

    public boolean isLastTradeBid() {
        return isLastTradeBid;
    }

    public int getLastTradeSize() {
        return lastTradeSize;
    }

    public double getLastTradePrice() {
        return lastTradePrice;
    }
}
