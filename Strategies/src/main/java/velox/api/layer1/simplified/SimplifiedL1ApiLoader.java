package velox.api.layer1.simplified;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import velox.api.layer1.Layer1ApiFinishable;
import velox.api.layer1.Layer1ApiInstrumentSpecificEnabledStateProvider;
import velox.api.layer1.Layer1ApiProvider;
import velox.api.layer1.Layer1CustomPanelsGetter;
import velox.api.layer1.common.Log;
import velox.api.layer1.data.BalanceInfo;
import velox.api.layer1.data.ExecutionInfo;
import velox.api.layer1.data.InstrumentInfo;
import velox.api.layer1.data.MarketMode;
import velox.api.layer1.data.OrderInfoUpdate;
import velox.api.layer1.data.OrderSendParameters;
import velox.api.layer1.data.OrderUpdateParameters;
import velox.api.layer1.data.StatusInfo;
import velox.api.layer1.data.TradeInfo;
import velox.api.layer1.layers.Layer1ApiInjectorRelay;
import velox.api.layer1.layers.strategies.interfaces.CalculatedResultListener;
import velox.api.layer1.layers.strategies.interfaces.CustomEventAggregatble;
import velox.api.layer1.layers.strategies.interfaces.CustomGeneratedEvent;
import velox.api.layer1.layers.strategies.interfaces.CustomGeneratedEventAliased;
import velox.api.layer1.layers.strategies.interfaces.InvalidateInterface;
import velox.api.layer1.layers.strategies.interfaces.Layer1IndicatorColorInterface;
import velox.api.layer1.layers.strategies.interfaces.OnlineCalculatable;
import velox.api.layer1.layers.strategies.interfaces.OnlineValueCalculatorAdapter;
import velox.api.layer1.layers.utils.OrderBook;
import velox.api.layer1.messages.CurrentTimeUserMessage;
import velox.api.layer1.messages.GeneratedEventInfo;
import velox.api.layer1.messages.Layer1ApiRequestCurrentTimeEvents;
import velox.api.layer1.messages.Layer1ApiUserMessageAddStrategyUpdateGenerator;
import velox.api.layer1.messages.UserMessageLayersChainCreatedTargeted;
import velox.api.layer1.messages.UserMessageRewindBase;
import velox.api.layer1.messages.indicators.AliasFilter;
import velox.api.layer1.messages.indicators.DataStructureInterface;
import velox.api.layer1.messages.indicators.DataStructureInterface.TreeResponseInterval;
import velox.api.layer1.messages.indicators.IndicatorColorScheme;
import velox.api.layer1.messages.indicators.Layer1ApiDataInterfaceRequestMessage;
import velox.api.layer1.messages.indicators.Layer1ApiUserMessageModifyIndicator;
import velox.api.layer1.messages.indicators.Layer1ApiUserMessageModifyIndicator.GraphType;
import velox.api.layer1.messages.indicators.SettingsAccess;
import velox.api.layer1.messages.indicators.StrategyUpdateGenerator;
import velox.api.layer1.messages.indicators.WidgetDisplayInfo;
import velox.api.layer1.messages.indicators.WidgetDisplayInfo.Type;
import velox.api.layer1.settings.Layer1ConfigSettingsInterface;
import velox.colors.ColorsChangedListener;
import velox.gui.StrategyPanel;

public class SimplifiedL1ApiLoader<T extends CustomModule> extends Layer1ApiInjectorRelay implements
    Layer1ApiFinishable,
    Layer1CustomPanelsGetter,
    Layer1ConfigSettingsInterface,
    Layer1IndicatorColorInterface,
    Layer1ApiInstrumentSpecificEnabledStateProvider {
    
    private enum Mode {
        LIVE,
        GENERATORS,
        MIXED;
    }
    
    private static class CustomEvent implements CustomGeneratedEvent {
        private static final long serialVersionUID = 1L;
        private final long time;
        
        public final int indicatorId;
        public final Double indicatorValue;
        
        
        public CustomEvent(long time, int indicatorId, Double indicatorValue) {
            this.time = time;
            this.indicatorId = indicatorId;
            this.indicatorValue = indicatorValue;
        }
    
        @Override
        public long getTime() {
            return time;
        }
        
        @Override
        public Object clone() {
            return new CustomEvent(time, indicatorId, indicatorValue);
        }

        @Override
        public String toString() {
            return "CustomEvent [time=" + time + ", indicatorId=" + indicatorId + ", indicatorValue=" + indicatorValue
                    + "]";
        }
    }

    private static class CustomAggregationEvent implements CustomGeneratedEvent {
        private static final long serialVersionUID = 1L;
        private final long time;
        
        public Map<Integer, Double> indicators = new HashMap<>();
        
        
        public CustomAggregationEvent(long time, Map<Integer, Double> indicators) {
            this.time = time;
            this.indicators.putAll(indicators);
        }
    
        @Override
        public long getTime() {
            return time;
        }
        
        @Override
        public Object clone() {
            return new CustomAggregationEvent(time, indicators);
        }
    }

    public static final CustomEventAggregatble CUSTOM_TRADE_EVENTS_AGGREGATOR = new CustomEventAggregatble() {
        @Override
        public CustomGeneratedEvent getInitialValue(long t) {
            return new CustomAggregationEvent(t, Collections.emptyMap());
        }

        @Override
        public void aggregateAggregationWithValue(CustomGeneratedEvent aggregation, CustomGeneratedEvent value) {
            CustomAggregationEvent aggregationEvent = (CustomAggregationEvent) aggregation;
            CustomEvent valueEvent = (CustomEvent) value;
            aggregationEvent.indicators.put(valueEvent.indicatorId, valueEvent.indicatorValue);
        }
        
        @Override
        public void aggregateAggregationWithAggregation(CustomGeneratedEvent aggregation1,
                CustomGeneratedEvent aggregation2) {
            CustomAggregationEvent aggregationEvent1 = (CustomAggregationEvent) aggregation1;
            CustomAggregationEvent aggregationEvent2 = (CustomAggregationEvent) aggregation2;
            aggregationEvent1.indicators.putAll(aggregationEvent2.indicators);
        }
    };
    
    private abstract class IndicatorImplementation implements Indicator, OnlineCalculatable {
        
        protected final String alias;
        protected final String name;
        protected final GraphType graphType;
        protected final Color defaultColor;
        protected final double initialValue;
        protected final InstanceWrapper wrapper;

        protected AtomicReference<InvalidateInterface> invalidateInterface = new AtomicReference<>();

        public IndicatorImplementation(String alias, String name, GraphType graphType, Color defaultColor, double initialValue, InstanceWrapper wrapper) {
            super();
            this.alias = alias;
            this.name = name;
            this.graphType = graphType;
            this.defaultColor = defaultColor;
            this.initialValue = initialValue;
            this.wrapper = wrapper;
        }

        public void register() {
            Layer1ApiUserMessageModifyIndicator message = getUserMessageModify(name, graphType, defaultColor, alias, true, this);
            provider.sendUserMessage(message);
        }

        public void remove() {
            Layer1ApiUserMessageModifyIndicator message = getUserMessageModify(name, graphType, defaultColor, alias, false, this);
            provider.sendUserMessage(message);
        }

        public void invalidateOnlineCalculator() {
            InvalidateInterface interfaceToInvalidate = invalidateInterface.getAndSet(null);
            if (interfaceToInvalidate != null) {
                interfaceToInvalidate.invalidate();
            }
        }
    }

    private class IndicatorBasicImplementation extends IndicatorImplementation {
        
        private List<Pair<Long, Double>> points = new ArrayList<>();

        public IndicatorBasicImplementation(String alias, String name, GraphType graphType, Color defaultColor, double initialValue, InstanceWrapper wrapper) {
            super(alias, name, graphType, defaultColor, initialValue, wrapper);
        }

        @Override
        public void calculateValuesInRange(String indicatorName, String indicatorAlias, long t0, long intervalWidth,
                int intervalsNumber, CalculatedResultListener listener) {

            for (int i = 0; i < intervalsNumber; ++i) {
                long t = t0 + intervalWidth * i;
                
                double response = initialValue;
                synchronized (points) {
                    int index = Collections.binarySearch(points, new ImmutablePair<>(t, 0.));
                    // If exact time was not found (usually) - set index to previous item
                    if (index < 0) {
                        index = -index - 2;
                    }
                    // index == -1 means no points before that
                    if (index >= 0) {
                        Pair<Long, Double> pair = points.get(index);
                        response = pair.getValue();
                    }
                }
                listener.provideResponse(response);
            }
            listener.setCompleted();
        }

        @Override
        public OnlineValueCalculatorAdapter createOnlineValueCalculator(String indicatorName, String indicatorAlias,
                long time, Consumer<Object> listener, InvalidateInterface invalidateInterface) {
            this.invalidateInterface.set(invalidateInterface);
            return new OnlineValueCalculatorAdapter() {
                
                @Override
                public void onLeftTimeChanged(long leftTime) {
                    sendLastValue(listener);
                }
                
                @Override
                public void onUserMessage(Object data) {
                    // Relying on time messages
                    sendLastValue(listener);
                }

                private void sendLastValue(Consumer<Object> listener) {
                    Double value = initialValue;
                    synchronized (points) {
                        if (!points.isEmpty()) {
                            Pair<Long, Double> lastPoint = points.get(points.size() - 1);
                            value = lastPoint.getValue();
                        }
                    }
                    if (value != null) {
                        listener.accept(value);
                    }
                }
            };
        }

        @Override
        public void addPoint(double value) {
            synchronized (points) {
                
                long time = mode == Mode.MIXED && !wrapper.isRealtime ?  wrapper.getTime() : getCurrentTime();
                
                if (time == 0) {
                    throw new IllegalStateException("time == 0");
                }
                
                int lastIndex = points.size() - 1;
                Pair<Long, Double> lastPoint = lastIndex > 0 ? points.get(lastIndex) : null;
                long lastListItemTime = lastPoint == null ? -1 : lastPoint.getKey();
                
                ImmutablePair<Long, Double> newPoint = new ImmutablePair<Long, Double>(time, value);
                
                if (lastListItemTime == time) {
                    points.set(lastIndex, newPoint);
                } else {
                    points.add(newPoint);
                }
            }
        }
    }
    
    // It's questionable if we need separate implementation for generator-only mode
    private class IndicatorGeneratorImplementation extends IndicatorImplementation {

        private final int generatorIndicatorId;

        public IndicatorGeneratorImplementation(String alias, String name, GraphType graphType, Color defaultColor, InstanceWrapper wrapper, int generatorIndicatorId, double initialValue) {
            super(alias, name, graphType, defaultColor, initialValue, wrapper);
            this.generatorIndicatorId = generatorIndicatorId;
        }
        
        @Override
        public void calculateValuesInRange(String indicatorName, String indicatorAlias, long t0, long intervalWidth,
                int intervalsNumber, CalculatedResultListener listener) {
            
            List<TreeResponseInterval> result = dataStructureInterface.get(simpleStrategyClass, alias, t0,
                    intervalWidth, intervalsNumber, alias, new Class<?>[] {CustomEvent.class});
            
            Double currentValue = getValueFromEvent(result.get(0));
            if (currentValue == null) {
                currentValue = initialValue;
            }
            
            for (int i = 1; i <= intervalsNumber; i++) {
                
                Double newValue = getValueFromEvent(result.get(i));
                if (newValue != null) {
                    currentValue = newValue;
                }
                
                if (currentValue != null)
                    listener.provideResponse(currentValue);
                
            }
            
            listener.setCompleted();
        }

        private Double getValueFromEvent(TreeResponseInterval treeResponseInterval) {
            Object object = treeResponseInterval.events.get(CustomEvent.class.toString());
            if (object != null) {
                return ((CustomAggregationEvent) object).indicators.get(generatorIndicatorId);
            } else {
                return null;
            }
        }

        @Override
        public OnlineValueCalculatorAdapter createOnlineValueCalculator(String indicatorName, String indicatorAlias,
                long time, Consumer<Object> listener, InvalidateInterface invalidateInterface) {
            return new OnlineValueCalculatorAdapter() {
                @Override
                public void onUserMessage(Object data) {
                    
                    if (data.getClass() == CustomGeneratedEventAliased.class) {
                        CustomGeneratedEventAliased aliasedEvent = (CustomGeneratedEventAliased) data;
                        if (aliasedEvent.event instanceof CustomEvent) {
                            CustomEvent event = (CustomEvent)aliasedEvent.event;
                            if (event.indicatorId == generatorIndicatorId) {
                                listener.accept(event.indicatorValue);
                            }
                        }
                    }
                }
            };
        }

        @Override
        public void addPoint(double value) {
            wrapper.generatorMessage.generator.getGeneratedEventsConsumer().accept(new CustomGeneratedEventAliased(
                    new CustomEvent(wrapper.getTime(), generatorIndicatorId, value), alias));
        }
    }
    
    private class InstanceWrapper implements Api {
        private final CustomModule instance;
        private final String alias;
        
        private List<IndicatorImplementation> indicators = new ArrayList<>();
        
        private final List<TimeListener> timeListeners = new ArrayList<>();
        private final List<DepthDataListener> depthDataListeners = new ArrayList<>();
        private final List<SnapshotEndListener> snapshotEndListeners = new ArrayList<>();
        private final List<TradeDataListener> tradeDataListeners = new ArrayList<>();
        private final List<BarDataListener> barDataListeners = new ArrayList<>();
        private final List<BboListener> bboDataListeners = new ArrayList<>();
        private final List<OrdersListener> ordersListeners = new ArrayList<>();
        private final List<PositionListener> statusListeners = new ArrayList<>();
        private final List<BalanceListener> balanceListeners = new ArrayList<>();
        private final List<HistoricalModeListener> historicalModeListeners = new ArrayList<>();
        private final List<MultiInstrumentListener> multiInstrumentListeners = new ArrayList<>();
        
        private boolean initializing;
        
        /** Generator time while generator is active, last time message otherwise */
        private long time = 0;
        private boolean isRealtime = false;
        
        private int generatorIndicatorId = 0;

        private String currentAlias;
        
        private Layer1ApiUserMessageAddStrategyUpdateGenerator generatorMessage;
        
        private long barInterval = -1;
        private Map<String, Bar> currentBars = new HashMap<>();
        
        private long timeNotificationInterval = -1;
        
        private Map<String, OrderBook> submittedOrderBooks = new HashMap<>();
        
        private boolean sendingSnapshots = false;
        private HashSet<String> snapshotEndsSent = new HashSet<>();
        private HashMap<String, Long> firstDepthUpdateTimes = new HashMap<>();
        
        
        public InstanceWrapper(String alias) {
            this.alias = alias;
            try {
                instance = simpleStrategyClass.newInstance();
            } catch (InstantiationException | IllegalAccessException e) {
                throw new IllegalStateException("Failed to create instance", e);
            }
            
        }

        public void start() {
            initializing = true;
            instance.initialize(alias, instruments.get(alias), this);
            initializing = false;
            
            addListener(instance);
            
            // Store data up to current time when adding generator
            if (mode == Mode.GENERATORS || mode == Mode.MIXED) {
                generatorMessage = getGeneratorMessage(true, alias, this);
                provider.sendUserMessage(generatorMessage);
            } else {
                sendSnapshots();
            }
        }

        private void sendSnapshots() {
            sendingSnapshots = true;
            Map<String, InstrumentInfo> instrumentInfosToSend = multiInstrument
                    ? instruments
                    : Collections.singletonMap(alias, instruments.get(alias));
            for (Entry<String, InstrumentInfo> entry : instrumentInfosToSend.entrySet()) {
                onInstrumentAdded(entry.getKey(), entry.getValue(), false);
            }
            
            Map<String, OrderBook> orderBooksToSend = multiInstrument
                    ? orderBooks
                    : Collections.singletonMap(alias, orderBooks.get(alias));
            for (Entry<String, OrderBook> bookEntry : orderBooksToSend.entrySet()) {
                String alias = bookEntry.getKey();
                OrderBook orderBook = bookEntry.getValue();
                for (Entry<Integer, Long> entry : orderBook.getBidMap().entrySet()) {
                    onDepth(alias, true, entry.getKey(), entry.getValue().intValue(), false);
                }
                for (Entry<Integer, Long> entry : orderBook.getAskMap().entrySet()) {
                    onDepth(alias, false, entry.getKey(), entry.getValue().intValue(), false);
                }
            }

            // Orders filtering requires iterating anyway
            for (Entry<String, OrderInfoUpdate> entry : orders.entrySet()) {
                onOrderUpdated(entry.getValue(), false);
            }
            
            Map<String, StatusInfo> statusesToSend = multiInstrument ? statuses
                    : statuses.containsKey(alias) ? Collections.singletonMap(alias, statuses.get(alias))
                    : Collections.emptyMap();
            for (Entry<String, StatusInfo> entry : statusesToSend.entrySet()) {
                onStatus(entry.getValue(), false);
            }
            
            if (balance != null) {
                onBalance(balance, false);
            }
            sendingSnapshots = false;
        }

        public void stop() {
            instance.stop();
            indicators.forEach(IndicatorImplementation::remove);
            Layer1ApiUserMessageAddStrategyUpdateGenerator generatorMessage = getGeneratorMessage(false, alias, this);
            provider.sendUserMessage(generatorMessage);
        }

        public void addListener(Object simplifiedListener) {
            
            if (simplifiedListener instanceof TimeListener) {
                // Actually that's a dead code, but I'm not sure if we'll want to bring that back shortly
                timeNotificationInterval = -1;
                timeListeners.add((TimeListener) simplifiedListener);
            }
            
            if (simplifiedListener instanceof DepthDataListener) {
                depthDataListeners.add((DepthDataListener) simplifiedListener);
            }
            if (simplifiedListener instanceof SnapshotEndListener) {
                snapshotEndListeners.add((SnapshotEndListener) simplifiedListener);
            }
            if (simplifiedListener instanceof TradeDataListener) {
                tradeDataListeners.add((TradeDataListener) simplifiedListener);
            }
            if (simplifiedListener instanceof BarDataListener) {
                barInterval = ((BarDataListener) instance).getBarInterval();
                if (barInterval < Intervals.MIN_INTERVAL) {
                    throw new IllegalArgumentException("BarDataListener#getBarInterval < Intervals#MIN_INTERVAL (" + barInterval + ")");
                }
                barDataListeners.add((BarDataListener) instance);
            }
            if (simplifiedListener instanceof BboListener) {
                bboDataListeners.add((BboListener) instance);
            }
            
            if (simplifiedListener instanceof OrdersListener) {
                ordersListeners.add((OrdersListener) instance);
            }
            if (simplifiedListener instanceof PositionListener) {
                statusListeners.add((PositionListener) instance);
            }
            if (simplifiedListener instanceof BalanceListener) {
                balanceListeners.add((BalanceListener) instance);
            }

            if (simplifiedListener instanceof HistoricalModeListener) {
                historicalModeListeners.add((HistoricalModeListener)simplifiedListener);
            }
            if (simplifiedListener instanceof MultiInstrumentListener) {
                multiInstrumentListeners.add((MultiInstrumentListener)simplifiedListener);
            }
        }

        @Override
        public Indicator registerIndicator(String name, GraphType graphType, Color defaultColor, double initialValue) {
            
            if (!initializing) {
                throw new IllegalStateException("Registering indicators is only allowed inside CustomModule#initialize");
            }
            
            defaultColors.put(new ImmutablePair<String, String>(alias, name), defaultColor);

            IndicatorImplementation indicatorImplementation = mode == Mode.GENERATORS
                    ? new IndicatorGeneratorImplementation(alias, name, graphType, defaultColor, this, generatorIndicatorId++, initialValue)
                    : new IndicatorBasicImplementation(alias, name, graphType, defaultColor, initialValue, this);
            indicatorImplementation.register();
            indicators.add(indicatorImplementation);
            return indicatorImplementation;
        }

        public void onDepth(String alias, boolean isBid, int price, int size, boolean fromGenerator) {
            if (eventShouldBePublished(fromGenerator)) {
                if (multiInstrument || this.alias.equals(alias)) {
                    setCurrentAlias(alias);
                    
                    if (!bboDataListeners.isEmpty()) {
                        OrderBook submittedOrderBook = submittedOrderBooks.get(alias);
                        if (submittedOrderBook == null) {
                            submittedOrderBook = new OrderBook();
                            submittedOrderBooks.put(alias, submittedOrderBook);
                        }
                        submittedOrderBook.onUpdate(isBid, price, size);
                        int bestPriceOnSameSide = isBid
                                ? submittedOrderBook.getBestBidPriceOrNone()
                                : submittedOrderBook.getBestAskPriceOrNone();
                        if (price == bestPriceOnSameSide) {

                            Entry<Integer, Long> firstBidEntry = submittedOrderBook.getBidMap().firstEntry();
                            Entry<Integer, Long> firstAskEntry = submittedOrderBook.getAskMap().firstEntry();

                            if (firstBidEntry != null && firstAskEntry != null) {
                                for (BboListener listener : bboDataListeners) {
                                    listener.onBbo(firstBidEntry.getKey(), firstBidEntry.getValue().intValue(),
                                            firstAskEntry.getKey(), firstAskEntry.getValue().intValue());
                                }
                            }
                        }
                    }
                    
                    invokeCurrentTimeListeners(fromGenerator, true);
                    boolean snapshotEndSent = snapshotEndsSent.contains(alias);
                    long firstDepthUpdateTime = firstDepthUpdateTimes.getOrDefault(alias, -1L);
                    if (!snapshotEndSent && firstDepthUpdateTime == -1) {
                        firstDepthUpdateTime = time;
                        firstDepthUpdateTimes.put(alias, firstDepthUpdateTime);
                    }
                    if (!snapshotEndSent && !sendingSnapshots && time > firstDepthUpdateTime) {
                        invokeSnapshotEndListeners(alias);
                    }
                    for (DepthDataListener listener : depthDataListeners) {
                        listener.onDepth(isBid, price, size);
                    }
                }
            }
        }

        private void invokeSnapshotEndListeners(String alias) {
            for (SnapshotEndListener listener : snapshotEndListeners) {
                listener.onSnapshotEnd();
            }
            snapshotEndsSent.add(alias);
        }

        public void onTrade(String alias, double price, int size, TradeInfo tradeInfo, boolean fromGenerator) {
            if (eventShouldBePublished(fromGenerator)) {
                if (multiInstrument || this.alias.equals(alias)) {
                    setCurrentAlias(alias);
                    if (!snapshotEndsSent.contains(alias)) {
                        invokeSnapshotEndListeners(alias);
                    }
                    
                    Bar currentBar = currentBars.get(alias);
                    if (currentBar == null) {
                        currentBar = new Bar();
                        currentBars.put(alias, currentBar);
                    }
                    currentBar.addTrade(tradeInfo.isBidAggressor, size, price);
                    
                    invokeCurrentTimeListeners(fromGenerator, true);
                    for (TradeDataListener listener : tradeDataListeners) {
                        listener.onTrade(price, size, tradeInfo);
                    }
                }
            }
        }
        
        public void onOrderUpdated(OrderInfoUpdate orderInfoUpdate, boolean fromGenerator) {
            if (eventShouldBePublished(fromGenerator)) {
                if (multiInstrument || this.alias.equals(alias)) {
                    setCurrentAlias(alias);
                    
                    invokeCurrentTimeListeners(fromGenerator, true);
                    for (OrdersListener listener : ordersListeners) {
                        listener.onOrderUpdated(orderInfoUpdate);
                    }
                }
            }
        }
        
        public void onOrderExecuted(ExecutionInfo executionInfo, boolean fromGenerator) {
            if (eventShouldBePublished(fromGenerator)) {
                if (multiInstrument || this.alias.equals(alias)) {
                    setCurrentAlias(alias);
                    
                    invokeCurrentTimeListeners(fromGenerator, true);
                    for (OrdersListener listener : ordersListeners) {
                        listener.onOrderExecuted(executionInfo);
                    }
                }
            }
            
        }
        
        public void onStatus(StatusInfo statusInfo, boolean fromGenerator) {
            if (eventShouldBePublished(fromGenerator)) {
                if (multiInstrument || this.alias.equals(alias)) {
                    setCurrentAlias(alias);
                    
                    invokeCurrentTimeListeners(fromGenerator, true);
                    for (PositionListener listener : statusListeners) {
                        listener.onPositionUpdate(statusInfo);
                    }
                }
            }
        }

        public void onBalance(BalanceInfo balanceInfo, boolean fromGenerator) {
            if (eventShouldBePublished(fromGenerator)) {
                if (multiInstrument || this.alias.equals(alias)) {
                    setCurrentAlias(alias);
                    
                    invokeCurrentTimeListeners(fromGenerator, true);
                    for (BalanceListener listener : balanceListeners) {
                        listener.onBalance(balance);
                    }
                }
            }
        }
        
        private boolean eventShouldBePublished(boolean fromGenerator) {
            return fromGenerator ? mode == Mode.MIXED || mode == Mode.GENERATORS : mode == Mode.LIVE || mode == Mode.MIXED;
        }
        
        private void invokeCurrentTimeListeners(boolean fromGenerator, boolean setTime) {
            if (setTime && !fromGenerator) {
                setTime(getCurrentTime(), fromGenerator);
            }
            for (TimeListener listener : timeListeners) {
                listener.onTimestamp(time);
            }
        }

        private void setCurrentAlias(String currentAlias) {
            if (!currentAlias.equals(this.currentAlias)) {
                this.currentAlias = currentAlias;
                for (MultiInstrumentListener listener : multiInstrumentListeners) {
                    listener.onCurrentInstrument(currentAlias);
                }
            }
        }
        
        public void onInstrumentAdded(String alias, InstrumentInfo instrumentInfo, boolean fromGenerator) {
            if (eventShouldBePublished(fromGenerator)) {
                if (multiInstrument) {
                    setCurrentAlias(alias);
                    
                    invokeCurrentTimeListeners(fromGenerator, true);
                    for (MultiInstrumentListener listener : multiInstrumentListeners) {
                        listener.onInstrumentAdded(instrumentInfo);
                    }
                }
            }
        }

        public void setTime(long newTime, boolean fromGenerator) {
            boolean isValid = mode == Mode.LIVE ? !fromGenerator
                    : mode == Mode.GENERATORS ? fromGenerator
                    : /* mode == Mode.MIXED ? */ isRealtime != fromGenerator;
            if (!isValid) {
                return;
            }
            
            if (time != 0) {
                // Loop is only needed for generators since gaps can be large
                // Technically, also needed for realtime after sleep, but in that case there is no data there

                while (getNextKeyTime(barInterval)<= newTime
                        || getNextKeyTime(timeNotificationInterval) <= newTime) {

                    long barKeyTime = getNextKeyTime(barInterval);
                    long notificationKeyTime = getNextKeyTime(timeNotificationInterval);

                    if (barKeyTime <= notificationKeyTime) {
                        time = barKeyTime;
                        Map<String, OrderBook> orderBooksMap = multiInstrument
                                ? orderBooks
                                : Collections.singletonMap(alias, orderBooks.get(alias));
                        for (Entry<String, OrderBook> entry : orderBooksMap.entrySet()) {
                            String alias = entry.getKey();
                            OrderBook orderBook = entry.getValue();
                            
                            Bar bar = currentBars.get(alias);
                            if (bar != null) {
                                Bar nextBar = new Bar(bar);
                                nextBar.startNext();
                                currentBars.put(alias, nextBar);
                            } else {
                                bar = new Bar();
                            }
                            
                            invokeCurrentTimeListeners(fromGenerator, false);
                            for (BarDataListener listener : barDataListeners) {
                                listener.onBar(orderBook, bar);
                            }
                        }
                    } else {
                        time = notificationKeyTime;
                        invokeCurrentTimeListeners(fromGenerator, false);
                    }
                }
            }

            this.time = newTime;
        }

        private long getNextKeyTime(long interval) {
            if (interval <= 0) {
                return Long.MAX_VALUE;
            } else {
                long remainder = time % interval;
                if (remainder == 0) {
                    return time + interval;
                } else {
                    return time + interval - remainder;
                }
            }
        }
        
        public long getTime() {
            return time;
        }

        public void onRealtimeStart() {
            isRealtime = true;
            for (HistoricalModeListener listener : historicalModeListeners) {
                listener.onRealtimeStart();
            }
            invalidateOnlineCalculators();
        }
        
        public void invalidateOnlineCalculators() {
            for (IndicatorImplementation indicator : indicators) {
                indicator.invalidateOnlineCalculator();
            }
        }

        @Override
        public void sendOrder(OrderSendParameters orderSendParameters) {
            SimplifiedL1ApiLoader.this.sendOrder(orderSendParameters);
        }

        @Override
        public void updateOrder(OrderUpdateParameters orderUpdateParameters) {
            SimplifiedL1ApiLoader.this.updateOrder(orderUpdateParameters);
        }
    }
    
    private final Layer1ApiRequestCurrentTimeEvents requestCurrentTimeEventsMessage = new Layer1ApiRequestCurrentTimeEvents(true, 0,
            TimeUnit.MILLISECONDS.toNanos(50));
    
    private DataStructureInterface dataStructureInterface;
    private Class<T> simpleStrategyClass;
    private Map<String, InstanceWrapper> instanceWrappers = new ConcurrentHashMap<>();
    
    private final Mode mode;
    private final boolean multiInstrument;
    
    private Map<String, InstrumentInfo> instruments = new ConcurrentHashMap<>();
    /** Order books for all currently added instruments */
    private Map<String, OrderBook> orderBooks = new ConcurrentHashMap<>();
    /** All known orders, key is order ID */
    private Map<String, OrderInfoUpdate> orders = new ConcurrentHashMap<>();
    private Map<String, StatusInfo> statuses = new ConcurrentHashMap<>();
    private BalanceInfo balance;
    
    // TODO: replace with settings
    private HashSet<String> enabledAliases = new HashSet<>();
    
    private HashMap<Pair<String, String>, Color> defaultColors = new HashMap<>();
    // TODO: replace with settings
    private HashMap<Pair<String, String>, Color> colors = new HashMap<>();
    
    public SimplifiedL1ApiLoader(Layer1ApiProvider provider, Class<T> clazz) {
        super(provider);
        this.simpleStrategyClass = clazz;
        
        mode = HistoricalModeListener.class.isAssignableFrom(clazz) ? Mode.MIXED
                : HistoricalDataListener.class.isAssignableFrom(clazz) ? Mode.GENERATORS
                : Mode.LIVE;
        multiInstrument = MultiInstrumentListener.class.isAssignableFrom(clazz);
    }
    
    @Override
    public void finish() {
        // Unload indicators, clear invalidate interfaces
        instanceWrappers.keySet().forEach(alias -> stopForInstrument(alias));
        
        requestCurrentTimeEventsMessage.setAdd(false);
        provider.sendUserMessage(requestCurrentTimeEventsMessage);
    }

    @Override
    public void setColor(String alias, String name, Color color) {
        colors.put(new ImmutablePair<String, String>(alias, name), color);
    }

    @Override
    public Color getColor(String alias, String name) {
        return colors.computeIfAbsent(new ImmutablePair<String, String>(alias, name),
                k -> defaultColors.get(k));
    }

    @Override
    public void addColorChangeListener(ColorsChangedListener listener) {
        // If there are 2 places where color is modified, not needed otherwise
        
    }

    @Override
    public void acceptSettingsInterface(SettingsAccess settingsAccess) {
        // Not working with settings yet
    }

    @Override
    public StrategyPanel[] getCustomGuiFor(String alias, String indicatorName) {
        
        InstanceWrapper instanceWrapper = instanceWrappers.get(alias);
        if (instanceWrapper != null && instanceWrapper.instance instanceof CustomSettingsPanelProvider) {
            return ((CustomSettingsPanelProvider)instanceWrapper.instance).getCustomSettingsPanels();
        }
        
        return new StrategyPanel[] {new StrategyPanel("No settings yet")};
    }

    @Override
    public void onStrategyCheckboxEnabled(String alias, boolean isEnabled) {
        Log.warn("Enabled for " + alias + " " + isEnabled);
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Exception> exception = new AtomicReference<Exception>();
        inject(() -> {
            try {
                if (isEnabled) {
                    if (instruments.containsKey(alias)) {
                        startForInstrument(alias);
                    }
                    enabledAliases.add(alias);
                } else {
                    stopForInstrument(alias);
                    enabledAliases.remove(alias);
                }
            } catch (Exception e) {
                exception.set(e);
            } finally {
                latch.countDown();
            }
        });
        try {
            latch.await();
            if (exception.get() != null) {
                throw new RuntimeException("Error while changing checkbox state", exception.get());
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private void startForInstrument(String alias) {
        InstanceWrapper instanceWrapper = new InstanceWrapper(alias);
        instanceWrappers.put(alias, instanceWrapper);
        instanceWrapper.start();
    }
    
    private void stopForInstrument(String alias) {
        instanceWrappers.remove(alias).stop();
    }

    @Override
    public boolean isStrategyEnabled(String alias) {
        // TODO Auto-generated method stub
        return enabledAliases.contains(alias);
    }
    
    @Override
    public void onUserMessage(Object data) {
        super.onUserMessage(data);
        
        if (data.getClass() == UserMessageLayersChainCreatedTargeted.class) {
            UserMessageLayersChainCreatedTargeted message = (UserMessageLayersChainCreatedTargeted) data;
            if (message.targetClass == simpleStrategyClass) {
                provider.sendUserMessage(new Layer1ApiDataInterfaceRequestMessage(
                        dataStructureInterface -> this.dataStructureInterface = dataStructureInterface));
                requestCurrentTimeEventsMessage.setAdd(true);
                provider.sendUserMessage(requestCurrentTimeEventsMessage);
//                isChainNew = message.isNew;
//                isWorking = true;
//                provider.sendUserMessage(new AnnotatedLayer1ApiDataInterfaceRequestMessage(dataStructureInterface -> this.dataStructureInterface = dataStructureInterface));
//                requestCurrentTimeEventsMessage.setAdd(true);
//                provider.sendUserMessage(requestCurrentTimeEventsMessage);
                
//                if (isChainNew) {
//                    Set<String> sentAliases = new HashSet<>();
//                    indicatorsFullNameToAlias.values().forEach(alias -> {
//                        if (!sentAliases.contains(alias)) {
//                            sentAliases.add(alias);
//                            provider.sendUserMessage(new Layer1ApiShowBottomPanel(alias));
//                        }
//                    });
//                }
                
            }
        } else if (data instanceof UserMessageRewindBase) {
            if (mode == Mode.LIVE || mode == Mode.MIXED) {
                Map<String, InstrumentInfo> instrumentsCopy = new HashMap<>(instruments);
                instrumentsCopy.keySet().forEach(this::removeInstrument);
                UserMessageRewindBase rewindMessage = (UserMessageRewindBase) data;
                for (Entry<String, OrderBook> entry: rewindMessage.aliasToOrderBooksMap.entrySet()) {
                    String alias = entry.getKey();
                    OrderBook orderBook = new OrderBook(entry.getValue());
                    addInstrument(alias, instrumentsCopy.get(alias), orderBook);
                }
            }
        } else if (data instanceof CurrentTimeUserMessage) {
            for (InstanceWrapper instanceWrapper : instanceWrappers.values()) {
                instanceWrapper.setTime(getCurrentTime(), false);
            }
        }
    }
    
    private Layer1ApiUserMessageModifyIndicator getUserMessageModify(String userReadableIndicatorName, GraphType graphType,
            Color defaultColor, String indicatorAlias, boolean isAdd, OnlineCalculatable onlineCalculatable) {
        
        Layer1ApiUserMessageModifyIndicator message = Layer1ApiUserMessageModifyIndicator
                .builder(simpleStrategyClass, userReadableIndicatorName)
                .extendFullName(indicatorAlias)
                .setIsAdd(isAdd)
                .setIndicatorColorScheme(new IndicatorColorScheme() {
                    private final String defaultColorName = userReadableIndicatorName;
                    
                    @Override
                    public ColorDescription[] getColors() {
                        return new ColorDescription[] {
                                new ColorDescription(simpleStrategyClass, defaultColorName, defaultColor, false)
                        };
                    }
                    
                    @Override
                    public ColorIntervalResponse getColorIntervalsList(double valueFrom, double valueTo) {
                        return new ColorIntervalResponse(new String[] {defaultColorName}, new double[] {});
                    }
                    
                    @Override
                    public String getColorFor(Double value) {
                        return defaultColorName;
                    }
                })
                .setColorInterface(this)
                .setGraphType(graphType)
                .setIsSupportWidget(true)
                .setIsShowColorSettings(false)
                .setOnlineCalculatable(onlineCalculatable).setAliasFiler(new AliasFilter() {
                    @Override
                    public boolean isDisplayedForAlias(String alias) {
                        return alias.equals(indicatorAlias);
                    }
                })
                .setWidgetDisplayInfo(new WidgetDisplayInfo(Type.SYMMETRIC, 0))
                .setIsWidgetEnabledByDefault(true)
                .setIsEnableSettingsFromConfigPopup(true).build();
        
        return message;
    }
    
    private Layer1ApiUserMessageAddStrategyUpdateGenerator getGeneratorMessage(boolean isAdd, String targetAlias, InstanceWrapper listener) {
        return new Layer1ApiUserMessageAddStrategyUpdateGenerator(simpleStrategyClass, targetAlias, isAdd, true, new StrategyUpdateGenerator() {
            private boolean isRealtime = false;
            
            private Consumer<CustomGeneratedEventAliased> consumer;
            
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
                if (mode == Mode.GENERATORS || !isRealtime) {
                    listener.onStatus(statusInfo, true);;
                }
            }
            
            @Override
            public void onOrderUpdated(OrderInfoUpdate orderInfoUpdate) {
                if (mode == Mode.GENERATORS || !isRealtime) {
                    listener.onOrderUpdated(orderInfoUpdate, true);
                }
            }
            
            @Override
            public void onOrderExecuted(ExecutionInfo executionInfo) {
                if (mode == Mode.GENERATORS || !isRealtime) {
                    listener.onOrderExecuted(executionInfo, true);
                }
            }
            
            @Override
            public void onBalance(BalanceInfo balanceInfo) {
                if (mode == Mode.GENERATORS || !isRealtime) {
                    listener.onBalance(balanceInfo, true);
                }
            }
            
            @Override
            public void onTrade(String alias, double price, int size, TradeInfo tradeInfo) {
                if (mode == Mode.GENERATORS || !isRealtime) {
                    listener.onTrade(alias, price, size, tradeInfo, true);
                }
            }
            
            @Override
            public void onMarketMode(String alias, MarketMode marketMode) {
            }
            
            @Override
            public void onDepth(String alias, boolean isBid, int price, int size) {
                if (mode == Mode.GENERATORS || !isRealtime) {
                    listener.onDepth(alias, isBid, price, size, true);
                }
            }
            
            @Override
            public void onInstrumentAdded(String alias, InstrumentInfo instrumentInfo) {
                if (mode == Mode.GENERATORS || !isRealtime) {
                    listener.onInstrumentAdded(alias, instrumentInfo, true);
                }
            }
            
            @Override
            public void onInstrumentRemoved(String alias) {
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
                if (mode == Mode.MIXED && !isRealtime && time >= getCurrentTime()) {
                    // This does not work yet (probably will work in live)
                    isRealtime = true;
                    listener.onRealtimeStart();
                }
                if (!isRealtime) {
                    listener.setTime(time, true);
                }
            }
        }, new GeneratedEventInfo[] {new GeneratedEventInfo(CustomEvent.class, CustomAggregationEvent.class, CUSTOM_TRADE_EVENTS_AGGREGATOR)});
    }
    
    @Override
    public void onInstrumentAdded(String alias, InstrumentInfo instrumentInfo) {
        super.onInstrumentAdded(alias, instrumentInfo);
        
        addInstrument(alias, instrumentInfo, new OrderBook());
        
        if (multiInstrument) {
            for (InstanceWrapper instanceWrapper : instanceWrappers.values()) {
                instanceWrapper.onInstrumentAdded(alias, instrumentInfo, false);
            }
        }
    }

    private void addInstrument(String alias, InstrumentInfo instrumentInfo, OrderBook orderBook) {

        instruments.put(alias, instrumentInfo);
        orderBooks.put(alias, orderBook);
        
        if (enabledAliases.contains(alias)) {
            startForInstrument(alias);
        }
    }
    
    @Override
    public void onInstrumentRemoved(String alias) {
        super.onInstrumentRemoved(alias);
        removeInstrument(alias);
    }

    private void removeInstrument(String alias) {
        
        if (enabledAliases.contains(alias)) {
            stopForInstrument(alias);
        }
        instruments.remove(alias);
        orderBooks.remove(alias);
    }
    
    @Override
    public void onDepth(String alias, boolean isBid, int price, int size) {
        super.onDepth(alias, isBid, price, size);
        
        OrderBook orderBook = orderBooks.get(alias);
        if (orderBook != null) {
            orderBook.onUpdate(isBid, price, size);
        }
        
        if (multiInstrument) {
            for (InstanceWrapper instanceWrapper : instanceWrappers.values()) {
                instanceWrapper.onDepth(alias, isBid, price, size, false);
            }
        } else {
            InstanceWrapper instanceWrapper = instanceWrappers.get(alias);
            if (instanceWrapper != null) {
                instanceWrapper.onDepth(alias, isBid, price, size, false);
            }
        }
    }
    
    @Override
    public void onTrade(String alias, double price, int size, TradeInfo tradeInfo) {
        super.onTrade(alias, price, size, tradeInfo);
        
        if (multiInstrument) {
            for (InstanceWrapper instanceWrapper : instanceWrappers.values()) {
                instanceWrapper.onTrade(alias, price, size, tradeInfo, false);
            }
        } else {
            InstanceWrapper instanceWrapper = instanceWrappers.get(alias);
            if (instanceWrapper != null) {
                instanceWrapper.onTrade(alias, price, size, tradeInfo, false);
            }
        }
    }
    
    @Override
    public void onOrderUpdated(OrderInfoUpdate orderInfoUpdate) {
        super.onOrderUpdated(orderInfoUpdate);
        
        orders.put(orderInfoUpdate.orderId, orderInfoUpdate);
        if (multiInstrument) {
            for (InstanceWrapper instanceWrapper : instanceWrappers.values()) {
                instanceWrapper.onOrderUpdated(orderInfoUpdate, false);
            }
        } else {
            InstanceWrapper instanceWrapper = instanceWrappers.get(orderInfoUpdate.instrumentAlias);
            if (instanceWrapper != null) {
                instanceWrapper.onOrderUpdated(orderInfoUpdate, false);
            }
        }
    }
    
    @Override
    public void onOrderExecuted(ExecutionInfo executionInfo) {
        super.onOrderExecuted(executionInfo);
        
        if (multiInstrument) {
            for (InstanceWrapper instanceWrapper : instanceWrappers.values()) {
                instanceWrapper.onOrderExecuted(executionInfo, false);
            }
        } else {
            String alias = orders.get(executionInfo.orderId).instrumentAlias;
            InstanceWrapper instanceWrapper = instanceWrappers.get(alias);
            if (instanceWrapper != null) {
                instanceWrapper.onOrderExecuted(executionInfo, false);
            }
        }
    }
    
    @Override
    public void onStatus(StatusInfo statusInfo) {
        super.onStatus(statusInfo);
        
        statuses.put(statusInfo.instrumentAlias, statusInfo);
        
        if (multiInstrument) {
            for (InstanceWrapper instanceWrapper : instanceWrappers.values()) {
                instanceWrapper.onStatus(statusInfo, false);
            }
        } else {
            InstanceWrapper instanceWrapper = instanceWrappers.get(statusInfo.instrumentAlias);
            if (instanceWrapper != null) {
                instanceWrapper.onStatus(statusInfo, false);
            }
        }
    }
    
    @Override
    public void onBalance(BalanceInfo balanceInfo) {
        super.onBalance(balanceInfo);
        
        this.balance = balanceInfo;

        for (InstanceWrapper instanceWrapper : instanceWrappers.values()) {
            instanceWrapper.onBalance(balanceInfo, false);
        }
    }
}
