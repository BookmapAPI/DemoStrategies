package velox.api.layer1.simpledemo.markers.bars;

import velox.api.layer1.data.*;
import velox.api.layer1.layers.strategies.interfaces.CustomGeneratedEventAliased;
import velox.api.layer1.messages.indicators.StrategyUpdateGenerator;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

class BarsStrategyUpdateGenerator implements StrategyUpdateGenerator {
    private static final long CANDLE_INTERVAL_NS = TimeUnit.SECONDS.toNanos(30);
    private Consumer<CustomGeneratedEventAliased> consumer;

    private long time = 0;

    private Map<String, BarsOnlineCalculator.BarEvent> aliasToLastBar = new HashMap<>();

    @Override
    public void setGeneratedEventsConsumer(Consumer<CustomGeneratedEventAliased> consumer) {
        this.consumer = consumer;
    }

    @Override
    public Consumer<CustomGeneratedEventAliased> getGeneratedEventsConsumer() {
        return consumer;
    }

    @Override
    public void onStatus(StatusInfo statusInfo) {
    }

    @Override
    public void onOrderUpdated(OrderInfoUpdate orderInfoUpdate) {
    }

    @Override
    public void onOrderExecuted(ExecutionInfo executionInfo) {
    }

    @Override
    public void onBalance(BalanceInfo balanceInfo) {
    }

    @Override
    public void onTrade(String alias, double price, int size, TradeInfo tradeInfo) {
        BarsOnlineCalculator.BarEvent bar = aliasToLastBar.get(alias);

        long barStartTime = getBarStartTime(time);

        if (bar == null) {
            bar = new BarsOnlineCalculator.BarEvent(barStartTime);
            aliasToLastBar.put(alias, bar);
        }

        if (barStartTime != bar.getTime()) {
            bar.setTime(time);
            consumer.accept(new CustomGeneratedEventAliased(bar, alias));
            bar = new BarsOnlineCalculator.BarEvent(barStartTime, bar.close);
            aliasToLastBar.put(alias, bar);
        }

        if (size != 0) {
            bar.update(price);
        }
    }

    @Override
    public void onMarketMode(String alias, MarketMode marketMode) {
    }

    @Override
    public void onDepth(String alias, boolean isBid, int price, int size) {
    }

    @Override
    public void onInstrumentAdded(String alias, InstrumentInfo instrumentInfo) {
    }

    @Override
    public void onInstrumentRemoved(String alias) {
        aliasToLastBar.remove(alias);
    }

    @Override
    public void onInstrumentNotFound(String symbol, String exchange, String type) {
    }

    @Override
    public void onInstrumentAlreadySubscribed(String symbol, String exchange, String type) {
    }

    @Override
    public void onUserMessage(Object data) {
    }

    @Override
    public void setTime(long time) {
        this.time = time;

        /*
         * Publish finished bars. Bookmap call this method periodically even if nothing
         * is happening at around 50ms intervals (avoid relying on exact value as it
         * might be changed in the future).
         */
        long barStartTime = getBarStartTime(time);
        for (Map.Entry<String, BarsOnlineCalculator.BarEvent> entry : aliasToLastBar.entrySet()) {
            String alias = entry.getKey();
            BarsOnlineCalculator.BarEvent bar = entry.getValue();

            if (barStartTime != bar.getTime()) {
                bar.setTime(time);
                consumer.accept(new CustomGeneratedEventAliased(bar, alias));
                bar = new BarsOnlineCalculator.BarEvent(barStartTime, bar.close);
                entry.setValue(bar);
            }
        }
    }

    private long getBarStartTime(long time) {
        return time - time % CANDLE_INTERVAL_NS;
    }
}