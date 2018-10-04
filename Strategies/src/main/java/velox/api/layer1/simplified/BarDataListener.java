package velox.api.layer1.simplified;

import velox.api.layer1.layers.utils.OrderBook;

public interface BarDataListener {
    void onBar(OrderBook orderBook, Bar bar);
    long getBarInterval();
}
