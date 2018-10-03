package velox.api.layer1.simplified;

import velox.api.layer1.data.TradeInfo;

public interface TradeDataListener {
    void onTrade(double price, int size, TradeInfo tradeInfo);
}
