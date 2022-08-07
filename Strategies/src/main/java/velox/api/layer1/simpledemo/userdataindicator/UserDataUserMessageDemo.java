package velox.api.layer1.simpledemo.userdataindicator;

import java.awt.Color;
import java.math.BigInteger;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import velox.api.layer1.Layer1ApiAdminAdapter;
import velox.api.layer1.Layer1ApiFinishable;
import velox.api.layer1.Layer1ApiInstrumentAdapter;
import velox.api.layer1.Layer1ApiProvider;
import velox.api.layer1.annotations.Layer1ApiVersion;
import velox.api.layer1.annotations.Layer1ApiVersionValue;
import velox.api.layer1.annotations.Layer1Attachable;
import velox.api.layer1.annotations.Layer1StrategyName;
import velox.api.layer1.common.ListenableHelper;
import velox.api.layer1.data.BalanceInfo;
import velox.api.layer1.data.ExecutionInfo;
import velox.api.layer1.data.InstrumentInfo;
import velox.api.layer1.data.MarketMode;
import velox.api.layer1.data.OrderInfoUpdate;
import velox.api.layer1.data.StatusInfo;
import velox.api.layer1.data.TradeInfo;
import velox.api.layer1.layers.strategies.interfaces.CalculatedResultListener;
import velox.api.layer1.layers.strategies.interfaces.CustomEventAggregatble;
import velox.api.layer1.layers.strategies.interfaces.CustomGeneratedEvent;
import velox.api.layer1.layers.strategies.interfaces.CustomGeneratedEventAliased;
import velox.api.layer1.layers.strategies.interfaces.InvalidateInterface;
import velox.api.layer1.layers.strategies.interfaces.Layer1IndicatorColorInterface;
import velox.api.layer1.layers.strategies.interfaces.OnlineCalculatable;
import velox.api.layer1.layers.strategies.interfaces.OnlineValueCalculatorAdapter;
import velox.api.layer1.messages.GeneratedEventInfo;
import velox.api.layer1.messages.Layer1ApiHistoricalDataLoadedMessage;
import velox.api.layer1.messages.Layer1ApiRequestCurrentTimeEvents;
import velox.api.layer1.messages.Layer1ApiUserMessageAddStrategyUpdateGenerator;
import velox.api.layer1.messages.UserMessageLayersChainCreatedTargeted;

import velox.api.layer1.messages.indicators.DataStructureInterface;
import velox.api.layer1.messages.indicators.DataStructureInterface.TreeResponseInterval;
import velox.api.layer1.messages.indicators.StrategyUpdateGenerator;
import velox.api.layer1.messages.indicators.StrategyUpdateGeneratorFilter;
import velox.api.layer1.messages.indicators.IndicatorColorScheme;
import velox.api.layer1.messages.indicators.IndicatorLineStyle;
import velox.api.layer1.messages.indicators.Layer1ApiDataInterfaceRequestMessage;
import velox.api.layer1.messages.indicators.Layer1ApiUserMessageModifyIndicator;
import velox.api.layer1.messages.indicators.Layer1ApiUserMessageModifyIndicator.GraphType;
import velox.api.layer1.messages.indicators.WidgetDisplayInfo;
import velox.api.layer1.messages.indicators.WidgetDisplayInfo.Type;
import velox.api.layer1.reading.UserDataUserMessage;
import velox.colors.ColorsChangedListener;

/**
 * An example of usage of UserDataUserMessage.<br>
 * These messages are used to store arbitrary binary data (byte array) in tree structures/feeds/historical data. <br><br>
 *
 * In this example, DemoExternalRealtimeProvider (from <b><a href="https://github.com/BookmapAPI/Layer0ApiDemo">Layer0ApiDemo</a></b>)
 * sends us messages with the tag "RandomData" which contains an integer represented as a byte array. There are two types of incoming
 * messages - global (alias = null) and aliased (alias != null).<br>
 * Aliased messages will be displayed only for instruments with specific alias (as a yellow line, "Aliased Random Data").<br>
 * Global messages will be displayed for each instrument (on the same price level, as a blue line, "Global Random Data").<br><br>
 *
 * This demo will also display both global and aliased data in replay mode.<br>
 * As for historical data - <b>only aliased data</b> will be displayed in it (resubscribe to the same instrument to see
 * previous subscribed time as historical data).
 */
@Layer1Attachable
@Layer1StrategyName("UserDataUserMessage Demo")
@Layer1ApiVersion(Layer1ApiVersionValue.VERSION2)
public class UserDataUserMessageDemo implements Layer1ApiAdminAdapter, Layer1ApiInstrumentAdapter, Layer1ApiFinishable {

    public static class CustomEvent implements CustomGeneratedEvent {
        private static final long serialVersionUID = 1L;
        private final long time;

        public final boolean isGlobal;
        public final int indicatorValue;

        public CustomEvent(long time, boolean isGlobal, int indicatorValue) {
            this.time = time;
            this.isGlobal = isGlobal;
            this.indicatorValue = indicatorValue;
        }

        @Override
        public long getTime() {
            return time;
        }

        @Override
        public Object clone() {
            return new CustomEvent(time, isGlobal, indicatorValue);
        }

        @Override
        public String toString() {
            return "CustomEvent [time=" + time + ", indicatorValue=" + indicatorValue + "]";
        }
    }

    public static class CustomAggregationEvent implements CustomGeneratedEvent {
        private static final long serialVersionUID = 1L;
        private final long time;

        public Map<Boolean, Integer> indicatorValues = new HashMap<>();

        public CustomAggregationEvent(long time, Map<Boolean, Integer> indicatorValues) {
            this.time = time;
            this.indicatorValues.putAll(indicatorValues);
        }

        @Override
        public long getTime() {
            return time;
        }

        @Override
        public Object clone() {
            return new CustomAggregationEvent(time, indicatorValues);
        }
    }

    public static final CustomEventAggregatble CUSTOM_EVENTS_AGGREGATOR = new CustomEventAggregatble() {
        @Override
        public CustomGeneratedEvent getInitialValue(long t) {
            return new CustomAggregationEvent(t, Collections.emptyMap());
        }

        @Override
        public void aggregateAggregationWithValue(CustomGeneratedEvent aggregation, CustomGeneratedEvent value) {
            CustomAggregationEvent aggregationEvent = (CustomAggregationEvent) aggregation;
            CustomEvent valueEvent = (CustomEvent) value;
            aggregationEvent.indicatorValues.put(valueEvent.isGlobal, valueEvent.indicatorValue);
        }

        @Override
        public void aggregateAggregationWithAggregation(CustomGeneratedEvent aggregation1,
                CustomGeneratedEvent aggregation2) {
            CustomAggregationEvent aggregationEvent1 = (CustomAggregationEvent) aggregation1;
            CustomAggregationEvent aggregationEvent2 = (CustomAggregationEvent) aggregation2;
            aggregationEvent1.indicatorValues.putAll(aggregationEvent2.indicatorValues);
        }
    };

    private class Indicator implements OnlineCalculatable {

        private final AtomicReference<InvalidateInterface> invalidateInterface = new AtomicReference<>();
        public final boolean isGlobal;
        public final String indicatorName;
        public final Color indicatorColor;

        public Indicator(boolean isGlobal) {
            this.isGlobal = isGlobal;
            this.indicatorName = isGlobal ? "GlobalRandomData" : "AliasedRandomData";
            this.indicatorColor = isGlobal ? Color.BLUE : Color.YELLOW;
        }

        @Override
        public void calculateValuesInRange(String indicatorName, String indicatorAlias, long t0, long intervalWidth,
                int intervalsNumber, CalculatedResultListener listener) {
            if (dataStructureInterface == null) {
                listener.setCompleted();
                return;
            }

            List<TreeResponseInterval> result = dataStructureInterface.get(UserDataUserMessageDemo.class,
                    getGeneratorName(indicatorAlias), t0, intervalWidth, intervalsNumber, indicatorAlias,
                    new Class<?>[] { CustomEvent.class });

            if (result.size() == 0) {
                listener.setCompleted();
                return;
            }
            
            Integer lastValue = 0;
            for (int i = 0; i <= intervalsNumber; i++) {
                Integer value = getValueFromEvent(result.get(i));
                if (value != null) {
                    listener.provideResponse(value);
                    lastValue = value;
                } else {
                    listener.provideResponse(lastValue);
                }
            }

            listener.setCompleted();
        }

        private Integer getValueFromEvent(TreeResponseInterval treeResponseInterval) {
            Object object = treeResponseInterval.events.get(CustomEvent.class.toString());
            if (object != null) {
                return ((CustomAggregationEvent) object).indicatorValues.get(isGlobal);
            }
            return null;
        }

        @Override
        public OnlineValueCalculatorAdapter createOnlineValueCalculator(String indicatorName, String indicatorAlias,
                long time, Consumer<Object> listener, InvalidateInterface invalidateInterface) {
            this.invalidateInterface.set(invalidateInterface);
            return new OnlineValueCalculatorAdapter() {
                @Override
                public void onUserMessage(Object data) {
                    if (data instanceof CustomGeneratedEventAliased) {
                        CustomGeneratedEventAliased aliasedEvent = (CustomGeneratedEventAliased) data;
                        if (aliasedEvent.event instanceof CustomEvent) {
                            CustomEvent event = (CustomEvent) aliasedEvent.event;
                            if (isGlobal == event.isGlobal && indicatorAlias.equals(aliasedEvent.alias)) {
                                listener.accept(event.indicatorValue);
                            }
                        }
                    }
                }
            };
        }

        public void invalidate() {
            InvalidateInterface invalidateInterfaceValue = invalidateInterface.get();
            if (invalidateInterfaceValue != null) {
                invalidateInterfaceValue.invalidate();
            }
        }
    }

    private Layer1ApiUserMessageModifyIndicator getIndicatorMessage(boolean isGlobal, boolean isAdd) {
        Indicator indicator = new Indicator(isGlobal);
        if (isAdd) {
            indicators.add(indicator);
        } else {
            indicators.remove(indicator);
        }
        Layer1ApiUserMessageModifyIndicator message = Layer1ApiUserMessageModifyIndicator
                .builder(UserDataUserMessageDemo.class, indicator.indicatorName).setIsAdd(isAdd)
                .setIndicatorColorScheme(new IndicatorColorScheme() {
                    private final String defaultColorName = indicator.indicatorName;

                    @Override
                    public ColorDescription[] getColors() {
                        return new ColorDescription[] { new ColorDescription(UserDataUserMessageDemo.class,
                                defaultColorName, indicator.indicatorColor, false).setDisplayedInIndicatorPopup(true) };
                    }

                    @Override
                    public ColorIntervalResponse getColorIntervalsList(double valueFrom, double valueTo) {
                        return new ColorIntervalResponse(new String[] { defaultColorName }, new double[] {});
                    }

                    @Override
                    public String getColorFor(Double value) {
                        return defaultColorName;
                    }
                }).setColorInterface(new Layer1IndicatorColorInterface() {
                    @Override
                    public void setColor(String alias, String name, Color color) {
                    }

                    @Override
                    public Color getColor(String alias, String name) {
                        return indicator.indicatorColor;
                    }

                    @Override
                    public void addColorChangeListener(ColorsChangedListener listener) {
                        // we do not own any colors
                    }
                })
                .setIndicatorLineStyle(IndicatorLineStyle.DEFAULT)
                .setGraphType(GraphType.PRIMARY)
                .setOnlineCalculatable(indicator)
                .setWidgetDisplayInfo(new WidgetDisplayInfo(Type.DEFAULT, 0))
                .setGraphLayerRenderPriority(Layer1ApiUserMessageModifyIndicator.LayerRenderPriority.ABOVE_BBO)
                .build();
        return message;
    }

    static class CustomStrategyUpdateGenerator implements StrategyUpdateGenerator, StrategyUpdateGeneratorFilter {
        private long time;
        private final String generatorAlias;
        private final Set<String> generatorAliases;
        private final Set<StrategyUpdateGeneratorEventType> generatorUpdateTypes;

        public CustomStrategyUpdateGenerator(String generatorAlias) {
            this.generatorAlias = generatorAlias;
            this.generatorAliases = new HashSet<>();
            generatorAliases.add(generatorAlias);
            this.generatorUpdateTypes = new HashSet<>();
            generatorUpdateTypes.add(StrategyUpdateGeneratorEventType.USER_DATA);
        }

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
        }

        @Override
        public void onInstrumentNotFound(String symbol, String exchange, String type) {
        }

        @Override
        public void onInstrumentAlreadySubscribed(String symbol, String exchange, String type) {
        }

        @Override
        public void onUserMessage(Object data) {
            if (data instanceof UserDataUserMessage) {
                UserDataUserMessage userMessage = (UserDataUserMessage) data;

                if (userMessage.tag.equals("RandomData")) {
                    int value = (new BigInteger(userMessage.data)).intValue();
                    String alias = userMessage.alias;

                    CustomGeneratedEventAliased generatedEvent;
                    if (alias == null) {
                        // We add global events to every generator tree, so it will be displayed for each instrument
                        generatedEvent = new CustomGeneratedEventAliased(
                                new CustomEvent(time, true, value), generatorAlias);
                    } else {
                        generatedEvent = new CustomGeneratedEventAliased(
                                new CustomEvent(time, false, value), alias);
                    }
                    consumer.accept(generatedEvent);
                }
            }
        }

        @Override
        public Set<StrategyUpdateGeneratorEventType> getGeneratorUpdateTypes() {
            return generatorUpdateTypes;
        }

        @Override
        public Set<String> getGeneratorAliases() {
            return generatorAliases;
        }

        @Override
        public void setTime(long time) {
            this.time = time;
        }

    }

    private String getGeneratorName(String alias) {
        return GENERATOR_NAME_PREFIX + alias;
    }

    private Layer1ApiUserMessageAddStrategyUpdateGenerator getGeneratorMessage(String alias, boolean isAdd) {
        return new Layer1ApiUserMessageAddStrategyUpdateGenerator(UserDataUserMessageDemo.class,
                getGeneratorName(alias), isAdd, true, true, new CustomStrategyUpdateGenerator(alias), new GeneratedEventInfo[] { new GeneratedEventInfo(CustomEvent.class, CustomAggregationEvent.class,
                CUSTOM_EVENTS_AGGREGATOR) });
    }

    private static final String GENERATOR_NAME_PREFIX = "RandomDataGenerator ";

    private final Layer1ApiProvider provider;
    private final List<Indicator> indicators = new CopyOnWriteArrayList<>();
    private final Layer1ApiRequestCurrentTimeEvents requestCurrentTimeEventsMessage = new Layer1ApiRequestCurrentTimeEvents(
            true, 0, TimeUnit.MILLISECONDS.toNanos(50));

    private DataStructureInterface dataStructureInterface;

    public UserDataUserMessageDemo(Layer1ApiProvider provider) {
        this.provider = provider;
        ListenableHelper.addListeners(provider, this);
    }

    @Override
    public void onInstrumentAdded(String alias, InstrumentInfo instrumentInfo) {
        new Thread(() -> {
            provider.sendUserMessage(getGeneratorMessage(alias,false));
            provider.sendUserMessage(getGeneratorMessage(alias, true));
        }).start();
    }

    @Override
    public void onInstrumentRemoved(String alias) {
        new Thread(() -> {
            provider.sendUserMessage(getGeneratorMessage(alias,false));
        }).start();
    }

    @Override
    public void finish() {
        provider.sendUserMessage(getIndicatorMessage(true, false));
        provider.sendUserMessage(getIndicatorMessage(false, false));
    }

    @Override
    public void onUserMessage(Object data) {
        if (data instanceof Layer1ApiHistoricalDataLoadedMessage) {
            Layer1ApiHistoricalDataLoadedMessage message = (Layer1ApiHistoricalDataLoadedMessage) data;
            new Thread(() -> {
                provider.sendUserMessage(getGeneratorMessage(message.alias,false));
                provider.sendUserMessage(getGeneratorMessage(message.alias, true));
            }).start();
        } else if (data instanceof UserMessageLayersChainCreatedTargeted) {
            UserMessageLayersChainCreatedTargeted message = (UserMessageLayersChainCreatedTargeted) data;
            if (message.targetClass == getClass()) {
                provider.sendUserMessage(new Layer1ApiDataInterfaceRequestMessage(
                        dataStructureInterface -> {
                            this.dataStructureInterface = dataStructureInterface;
                            indicators.forEach(Indicator::invalidate);
                        }));
                provider.sendUserMessage(requestCurrentTimeEventsMessage);
                provider.sendUserMessage(getIndicatorMessage(true, true));
                provider.sendUserMessage(getIndicatorMessage(false, true));
            }
        }
    }
}
