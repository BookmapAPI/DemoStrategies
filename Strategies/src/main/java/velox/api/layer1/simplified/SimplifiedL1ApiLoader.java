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
import java.util.concurrent.TimeUnit;
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
import velox.api.layer1.data.StatusInfo;
import velox.api.layer1.data.TradeInfo;
import velox.api.layer1.layers.Layer1ApiRelay;
import velox.api.layer1.layers.strategies.interfaces.CalculatedResultListener;
import velox.api.layer1.layers.strategies.interfaces.CustomEventAggregatble;
import velox.api.layer1.layers.strategies.interfaces.CustomGeneratedEvent;
import velox.api.layer1.layers.strategies.interfaces.CustomGeneratedEventAliased;
import velox.api.layer1.layers.strategies.interfaces.InvalidateInterface;
import velox.api.layer1.layers.strategies.interfaces.Layer1IndicatorColorInterface;
import velox.api.layer1.layers.strategies.interfaces.OnlineCalculatable;
import velox.api.layer1.layers.strategies.interfaces.OnlineValueCalculatorAdapter;
import velox.api.layer1.layers.utils.OrderBook;
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

public class SimplifiedL1ApiLoader<T extends CustomModule> extends Layer1ApiRelay implements
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
        protected final double initialValue;
        protected final InstanceWrapper wrapper;

        public IndicatorImplementation(String alias, String name, GraphType graphType, double initialValue, InstanceWrapper wrapper) {
            super();
            this.alias = alias;
            this.name = name;
            this.graphType = graphType;
            this.initialValue = initialValue;
            this.wrapper = wrapper;
        }

        public void register() {
            Layer1ApiUserMessageModifyIndicator message = getUserMessageModify(name, graphType, alias, true, this);
            provider.sendUserMessage(message);
        }
        
        @Override
        public void remove() {
            Layer1ApiUserMessageModifyIndicator message = getUserMessageModify(name, graphType, alias, false, this);
            provider.sendUserMessage(message);
        }
    }

    private class IndicatorBasicImplementation extends IndicatorImplementation {
        
        private List<Pair<Long, Double>> points = new ArrayList<>();

        public IndicatorBasicImplementation(String alias, String name, GraphType graphType, double initialValue, InstanceWrapper wrapper) {
            super(alias, name, graphType, initialValue, wrapper);
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

                long time = mode == Mode.MIXED && !wrapper.isRealtime ?  wrapper.generatorTime : getCurrentTime();
                int lastIndex = points.size() - 1;
                Pair<Long, Double> lastPoint = lastIndex > 0 ? points.get(lastIndex) : null;
                long lastListItemTime = lastPoint == null ? 0 : lastPoint.getKey();
                
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

        public IndicatorGeneratorImplementation(String alias, String name, GraphType graphType, InstanceWrapper wrapper, int generatorIndicatorId, double initialValue) {
            super(alias, name, graphType, initialValue, wrapper);
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
                    new CustomEvent(wrapper.getGeneratorTime(), generatorIndicatorId, value), alias));
        }
    }
    
    private class InstanceWrapper implements Api {
        private final CustomModule instance;
        private final String alias;
        
        private List<Indicator> indicators = new ArrayList<>();
        
        private final List<DepthDataListener> depthDataListeners = new ArrayList<>();
        private final List<TradeDataListener> tradeDataListeners = new ArrayList<>();
        private final List<HistoricalModeListener> historicalModeListeners = new ArrayList<>();
        
        private boolean initializing;
        
        private long generatorTime = 0;
        private boolean isRealtime = false;
        
        private int generatorIndicatorId = 0;
        
        private Layer1ApiUserMessageAddStrategyUpdateGenerator generatorMessage;
        
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
            instance.initialize(instruments.get(alias), this);
            initializing = false;
            
            addListener(instance);
            
            // Store data up to current time when adding generator
            if (mode == Mode.GENERATORS || mode == Mode.MIXED) {
                generatorMessage = getGeneratorMessage(true, alias, this);
                provider.sendUserMessage(generatorMessage);
            } else {
                OrderBook orderBook = orderBooks.get(alias);
                for (Entry<Integer, Long> entry : orderBook.getBidMap().entrySet()) {
                    onDepth(true, entry.getKey(), entry.getValue().intValue(), false);
                }
                for (Entry<Integer, Long> entry : orderBook.getAskMap().entrySet()) {
                    onDepth(false, entry.getKey(), entry.getValue().intValue(), false);
                }
            }
        }

        public void stop() {
            instance.stop();
            indicators.forEach(Indicator::remove);
            Layer1ApiUserMessageAddStrategyUpdateGenerator generatorMessage = getGeneratorMessage(false, alias, this);
            provider.sendUserMessage(generatorMessage);
        }

        public void addListener(Object simplifiedListener) {
            if (simplifiedListener instanceof DepthDataListener) {
                depthDataListeners.add((DepthDataListener) simplifiedListener);
            }
            if (simplifiedListener instanceof TradeDataListener) {
                tradeDataListeners.add((TradeDataListener) simplifiedListener);
            }
            if (simplifiedListener instanceof HistoricalModeListener) {
            	historicalModeListeners.add((HistoricalModeListener)simplifiedListener);
            }
        }

        @Override
        public Indicator registerIndicator(String name, GraphType graphType, double initialValue) {
            
            if (!initializing) {
                throw new IllegalStateException("Registering indicators is only allowed inside CustomModule#initialize");
            }

            IndicatorImplementation indicatorImplementation = mode == Mode.GENERATORS
                    ? new IndicatorGeneratorImplementation(alias, name, graphType, this, generatorIndicatorId++, initialValue)
                    : new IndicatorBasicImplementation(alias, name, graphType, initialValue, this);
            indicatorImplementation.register();
            indicators.add(indicatorImplementation);
            return indicatorImplementation;
        }

        public void onDepth(boolean isBid, int price, int size, boolean fromGenerator) {
            if (fromGenerator ? mode == Mode.MIXED || mode == Mode.GENERATORS : mode == Mode.LIVE || mode == Mode.MIXED) {
                for (DepthDataListener listener : depthDataListeners) {
                    listener.onDepth(isBid, price, size);
                }
            }
        }

        public void onTrade(double price, int size, TradeInfo tradeInfo, boolean fromGenerator) {
        	if (fromGenerator ? mode == Mode.MIXED || mode == Mode.GENERATORS : mode == Mode.LIVE || mode == Mode.MIXED) {
                for (TradeDataListener listener : tradeDataListeners) {
                    listener.onTrade(price, size, tradeInfo);
                }
            }
        }

        public void setGeneratorTime(long time) {
            this.generatorTime = time;
        }
        
        public long getGeneratorTime() {
            return generatorTime;
        }

		public void onRealtimeStart() {
			isRealtime = true;
			for (HistoricalModeListener listener : historicalModeListeners) {
				listener.onRealtimeStart();
			}
		}
    }
    
    private final Layer1ApiRequestCurrentTimeEvents requestCurrentTimeEventsMessage = new Layer1ApiRequestCurrentTimeEvents(true, 0,
            TimeUnit.MILLISECONDS.toNanos(100));
    
    private DataStructureInterface dataStructureInterface;
    private Class<T> simpleStrategyClass;
    private Map<String, InstanceWrapper> instanceWrappers = new ConcurrentHashMap<>();
    
    private final Mode mode;
    
    private Map<String, InstrumentInfo> instruments = new ConcurrentHashMap<>();
    private Map<String, OrderBook> orderBooks = new ConcurrentHashMap<>();
    
    private HashSet<String> enabledAliases = new HashSet<>();
    
    public SimplifiedL1ApiLoader(Layer1ApiProvider provider, Class<T> clazz) {
        super(provider);
        this.simpleStrategyClass = clazz;
        
        mode = HistoricalModeListener.class.isAssignableFrom(clazz) ? Mode.MIXED
        		: HistoricalDataListener.class.isAssignableFrom(clazz) ? Mode.GENERATORS
    			: Mode.LIVE;
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
        // TODO Save color to settings
        
    }

    @Override
    public Color getColor(String alias, String name) {
        // Return proper color
        return Color.RED;
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
        return new StrategyPanel[] {new StrategyPanel("No settings yet")};
    }

    @Override
    public void onStrategyCheckboxEnabled(String alias, boolean isEnabled) {
        Log.warn("Enabled for " + alias + " " + isEnabled);
        if (isEnabled) {
            startForInstrument(alias);
            enabledAliases.add(alias);
        } else {
            stopForInstrument(alias);
            enabledAliases.remove(alias);
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
        }
    }
    
    private Layer1ApiUserMessageModifyIndicator getUserMessageModify(String userReadableIndicatorName, GraphType graphType,
            String indicatorAlias, boolean isAdd, OnlineCalculatable onlineCalculatable) {
        
        Layer1ApiUserMessageModifyIndicator message = Layer1ApiUserMessageModifyIndicator
                .builder(simpleStrategyClass, userReadableIndicatorName)
                .extendFullName(indicatorAlias)
                .setIsAdd(isAdd)
                .setIndicatorColorScheme(new IndicatorColorScheme() {
                    private final String defaultColor = "default-" + indicatorAlias;
                    
                    @Override
                    public ColorDescription[] getColors() {
                        return new ColorDescription[] {
                                new ColorDescription(simpleStrategyClass, defaultColor, Color.RED, false)
                        };
                    }
                    
                    @Override
                    public ColorIntervalResponse getColorIntervalsList(double valueFrom, double valueTo) {
                        return new ColorIntervalResponse(new String[] {defaultColor}, new double[] {});
                    }
                    
                    @Override
                    public String getColorFor(Double value) {
                        return defaultColor;
                    }
                })
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
            
            private Map<String, Double> aliasToCurrentValue = new HashMap<>();
            
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
            	if (mode == Mode.GENERATORS || !isRealtime) {
	            	if (targetAlias.equals(alias)) {
	            		listener.onTrade(price, size, tradeInfo, true);
	            	}
            	}
            }
            
            @Override
            public void onMarketMode(String alias, MarketMode marketMode) {
            }
            
            @Override
            public void onDepth(String alias, boolean isBid, int price, int size) {
            	if (mode == Mode.GENERATORS || !isRealtime) {
	            	if (targetAlias.equals(alias)) {
	            		listener.onDepth(isBid, price, size, true);
	            	}
            	}
            }
            
            @Override
            public void onInstrumentAdded(String alias, InstrumentInfo instrumentInfo) {
            }
            
            @Override
            public void onInstrumentRemoved(String alias) {
                aliasToCurrentValue.remove(alias);
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
                listener.setGeneratorTime(time);
                
                if (!isRealtime && time >= getCurrentTime()) {
                    // This does not work yet (probably will work in live)
                    isRealtime = true;
                    listener.onRealtimeStart();
                }
            }
        }, new GeneratedEventInfo[] {new GeneratedEventInfo(CustomEvent.class, CustomAggregationEvent.class, CUSTOM_TRADE_EVENTS_AGGREGATOR)});
    }
    
    @Override
    public void onInstrumentAdded(String alias, InstrumentInfo instrumentInfo) {
        super.onInstrumentAdded(alias, instrumentInfo);
        
        addInstrument(alias, instrumentInfo, new OrderBook());
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
        orderBooks.remove(alias);
    }
    
    @Override
    public void onDepth(String alias, boolean isBid, int price, int size) {
        super.onDepth(alias, isBid, price, size);
        
        OrderBook orderBook = orderBooks.get(alias);
        if (orderBook != null) {
            orderBook.onUpdate(isBid, price, size);
        }
        
        InstanceWrapper instanceWrapper = instanceWrappers.get(alias);
        if (instanceWrapper != null) {
            instanceWrapper.onDepth(isBid, price, size, false);
        }
    }
    
    @Override
    public void onTrade(String alias, double price, int size, TradeInfo tradeInfo) {

        InstanceWrapper instanceWrapper = instanceWrappers.get(alias);
        if (instanceWrapper != null) {
            instanceWrapper.onTrade(price, size, tradeInfo, false);
        }
    }
}
