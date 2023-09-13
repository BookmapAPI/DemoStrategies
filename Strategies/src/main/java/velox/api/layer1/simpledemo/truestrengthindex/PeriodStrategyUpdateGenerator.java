package velox.api.layer1.simpledemo.truestrengthindex;

import velox.api.layer1.data.*;
import velox.api.layer1.layers.strategies.interfaces.CustomGeneratedEventAliased;
import velox.api.layer1.messages.indicators.StrategyUpdateGenerator;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

class PeriodStrategyUpdateGenerator implements StrategyUpdateGenerator {
    private static final long PERIOD_INTERVAL_NS = TimeUnit.SECONDS.toNanos(1);
    private final Map<String, PeriodEvent> aliasToLastPeriod = new HashMap<>();
    private Consumer<CustomGeneratedEventAliased> consumer;
    private long time = 0;

    @Override
    public Consumer<CustomGeneratedEventAliased> getGeneratedEventsConsumer() {
        return consumer;
    }

    @Override
    public void setGeneratedEventsConsumer(Consumer<CustomGeneratedEventAliased> consumer) {
        this.consumer = consumer;
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
        PeriodEvent period = aliasToLastPeriod.get(alias);

        long periodStartTime = getPeriodStartTime(time);

        if (period == null) {
            period = new PeriodEvent(periodStartTime);
            aliasToLastPeriod.put(alias, period);
        }

        if (periodStartTime != period.getTime()) {
            period.setTime(time);
            consumer.accept(new CustomGeneratedEventAliased(period, alias));
            period = new PeriodEvent(periodStartTime, period.getClose());
            aliasToLastPeriod.put(alias, period);
        }

        if (size != 0) {
            period.update(price);
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
        aliasToLastPeriod.remove(alias);
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

        long periodStartTime = getPeriodStartTime(time);
        for (Map.Entry<String, PeriodEvent> entry : aliasToLastPeriod.entrySet()) {
            String alias = entry.getKey();
            PeriodEvent period = entry.getValue();

            if (periodStartTime != period.getTime()) {
                period.setTime(time);
                consumer.accept(new CustomGeneratedEventAliased(period, alias));
                period = new PeriodEvent(periodStartTime, period.getClose());
                entry.setValue(period);
            }
        }
    }

    private long getPeriodStartTime(long time) {
        return time - time % PERIOD_INTERVAL_NS;
    }
}