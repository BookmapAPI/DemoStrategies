package velox.api.layer1.simpledemo.customevents;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import velox.api.layer1.Layer1ApiAdminAdapter;
import velox.api.layer1.Layer1ApiFinishable;
import velox.api.layer1.Layer1ApiProvider;
import velox.api.layer1.Layer1CustomPanelsGetter;
import velox.api.layer1.annotations.Layer1ApiVersion;
import velox.api.layer1.annotations.Layer1ApiVersionValue;
import velox.api.layer1.annotations.Layer1Attachable;
import velox.api.layer1.annotations.Layer1StrategyName;
import velox.api.layer1.common.ListenableHelper;
import velox.api.layer1.common.Log;
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
import velox.api.layer1.messages.Layer1ApiUserMessageAddStrategyUpdateGenerator;
import velox.api.layer1.messages.UserMessageLayersChainCreatedTargeted;
import velox.api.layer1.messages.indicators.DataStructureInterface;
import velox.api.layer1.messages.indicators.IndicatorColorScheme;
import velox.api.layer1.messages.indicators.Layer1ApiDataInterfaceRequestMessage;
import velox.api.layer1.messages.indicators.Layer1ApiUserMessageModifyIndicator;
import velox.api.layer1.messages.indicators.StrategyUpdateGenerator;
import velox.api.layer1.messages.indicators.DataStructureInterface.TreeResponseInterval;
import velox.api.layer1.messages.indicators.IndicatorColorInterface;
import velox.api.layer1.messages.indicators.Layer1ApiUserMessageModifyIndicator.GraphType;
import velox.api.layer1.messages.indicators.SettingsAccess;
import velox.api.layer1.settings.Layer1ConfigSettingsInterface;
import velox.colors.ColorsChangedListener;
import velox.gui.StrategyPanel;
import velox.gui.colors.ColorsConfigItem;

/**
 * An example of usage of custom events
 * This example show meaningless custom events, just to demonstrate how this works.
 *
 * In this example, we will draw line, that is a modified last trade price.
 * Value at i-th trade is calculated as average of value at (i-1)th trade and price of i-tr trade
 * We can't easily calculate this value fast using standard trade events, so we will need to
 * create our own event generator, and then use our generated events to quickly calculate values on screen.
 */
@Layer1Attachable
@Layer1StrategyName("Custom Events Demo")
@Layer1ApiVersion(Layer1ApiVersionValue.VERSION2)
public class Layer1CustomEventsDemo implements Layer1ApiFinishable, Layer1ApiAdminAdapter, OnlineCalculatable,
    Layer1CustomPanelsGetter, Layer1ConfigSettingsInterface, Layer1IndicatorColorInterface {
    private static class CustomTradePriceEvent implements CustomGeneratedEvent {
        private static final long serialVersionUID = 1L;
        private final long time;
        
        public double lastPrice;
        
        
        public CustomTradePriceEvent(long time, double lastPrice) {
            this.time = time;
            this.lastPrice = lastPrice;
        }
    
        @Override
        public long getTime() {
            return time;
        }
        
        @Override
        public Object clone() {
            return new CustomTradePriceEvent(time, lastPrice);
        }
        
        @Override
        public String toString() {
            return "[" + lastPrice + "(V)]";
        }
    }
    
    /**
     * Represents a result of some number of consecutive {@link CustomTradePriceEvent}
     * In our example it is the same as base event, cause we only need one value
     */
    private static class CustomTradePriceAggregationEvent implements CustomGeneratedEvent {
        private static final long serialVersionUID = 1L;
        private final long time;
        
        public double lastPrice;
        
        
        public CustomTradePriceAggregationEvent(long time, double lastPrice) {
            this.time = time;
            this.lastPrice = lastPrice;
        }
    
        @Override
        public long getTime() {
            return time;
        }
        
        @Override
        public Object clone() {
            return new CustomTradePriceAggregationEvent(time, lastPrice);
        }
        
        @Override
        public String toString() {
            return "[" + lastPrice + "(A)]";
        }
    }

    public static final CustomEventAggregatble CUSTOM_TRADE_EVENTS_AGGREGATOR = new CustomEventAggregatble() {
        @Override
        public CustomGeneratedEvent getInitialValue(long t) {
            return new CustomTradePriceAggregationEvent(t, Double.NaN);
        }

        @Override
        public void aggregateAggregationWithValue(CustomGeneratedEvent aggregation, CustomGeneratedEvent value) {
            CustomTradePriceAggregationEvent aggregationEvent = (CustomTradePriceAggregationEvent) aggregation;
            CustomTradePriceEvent valueEvent = (CustomTradePriceEvent) value;
            if (Double.isNaN(aggregationEvent.lastPrice) || !Double.isNaN(valueEvent.lastPrice)) {
                aggregationEvent.lastPrice = valueEvent.lastPrice;
            }
        }
        
        @Override
        public void aggregateAggregationWithAggregation(CustomGeneratedEvent aggregation1,
                CustomGeneratedEvent aggregation2) {
            CustomTradePriceAggregationEvent aggregationEvent1 = (CustomTradePriceAggregationEvent) aggregation1;
            CustomTradePriceAggregationEvent aggregationEvent2 = (CustomTradePriceAggregationEvent) aggregation2;
            if (Double.isNaN(aggregationEvent1.lastPrice) || !Double.isNaN(aggregationEvent2.lastPrice)) {
                aggregationEvent1.lastPrice = aggregationEvent2.lastPrice;
            }
            
        }
    };
    
    private static final String INDICATOR_NAME = "Custom Events Demo";
    private static final String TREE_NAME = "Custom Events Tree";
    private static final String LINE_COLOR_NAME = "Line color";
    private static final Color LINE_COLOR_DEFAULT = Color.RED;

    private static final Class<?>[] INTERESTING_CUSTOM_EVENTS = new Class<?>[] {CustomTradePriceEvent.class};
    
    private Layer1ApiProvider provider;
    
    private DataStructureInterface dataStructureInterface;
    
    private Map<String, CustomEventsDemoSettings> settingsMap = new HashMap<>();
    
    private SettingsAccess settingsAccess;
    
    private Map<String, InvalidateInterface> invalidateInterfaceMap = new ConcurrentHashMap<>();
    
    private Object locker = new Object();
    
    public Layer1CustomEventsDemo(Layer1ApiProvider provider) {
        this.provider = provider;
        
        ListenableHelper.addListeners(provider, this);
    }

    
    private double getValueFromEvent(TreeResponseInterval treeResponseInterval) {
        Object object = treeResponseInterval.events.get(CustomTradePriceEvent.class.toString());
        if (object != null) {
            return ((CustomTradePriceAggregationEvent) object).lastPrice;
        } else {
            return Double.NaN;
        }
        
    }
    
    @Override
    public void calculateValuesInRange(String indicatorName, String alias, long t0, long intervalWidth,
            int intervalsNumber, CalculatedResultListener listener) {
        
        if (dataStructureInterface == null) {
            listener.setCompleted();
            return;
        }
        
        List<TreeResponseInterval> result = dataStructureInterface.get(Layer1CustomEventsDemo.class, TREE_NAME, t0,
                intervalWidth, intervalsNumber, alias, INTERESTING_CUSTOM_EVENTS);
        
        TreeResponseInterval startValue = dataStructureInterface.get(Layer1CustomEventsDemo.class, TREE_NAME, t0, alias, INTERESTING_CUSTOM_EVENTS);
        double currentValue = getValueFromEvent(startValue);
        
        for (int i = 1; i <= intervalsNumber; i++) {
            
            double newValue = getValueFromEvent(result.get(i));
            if (!Double.isNaN(newValue)) {
                currentValue = newValue;
            }
            
            listener.provideResponse(currentValue);
            
        }
        
        listener.setCompleted();
    }

    @Override
    public OnlineValueCalculatorAdapter createOnlineValueCalculator(String indicatorName, String indicatorAlias, long time,
            Consumer<Object> listener, InvalidateInterface invalidateInterface) {
        invalidateInterfaceMap.put(INDICATOR_NAME, invalidateInterface);

        if (dataStructureInterface == null) {
            return new OnlineValueCalculatorAdapter() {};
        }
        
        TreeResponseInterval startEvents = dataStructureInterface.get(Layer1CustomEventsDemo.class, TREE_NAME, time, indicatorAlias, INTERESTING_CUSTOM_EVENTS);
        final double startValue = getValueFromEvent(startEvents);
        
        return new OnlineValueCalculatorAdapter() {
            private double lastValue = Double.isNaN(startValue) ? 0 : startValue;
            
            @Override
            public void onTrade(String alias, double price, int size, TradeInfo tradeInfo) {
                if (alias.equals(indicatorAlias)) {
                    lastValue = Double.isNaN(lastValue) ? price : (lastValue + price) / 2.;
                    listener.accept(lastValue);
                }
            }
        };
    }
    
    @Override
    public void finish() {
        provider.sendUserMessage(getGeneratorMessage(false));
        provider.sendUserMessage(getIndicatorMessage(false));
    }
    
    
    @Override
    public void onUserMessage(Object data) {
        if (data.getClass() == UserMessageLayersChainCreatedTargeted.class) {
            UserMessageLayersChainCreatedTargeted message = (UserMessageLayersChainCreatedTargeted) data;
            if (message.targetClass == getClass()) {
                provider.sendUserMessage(new Layer1ApiDataInterfaceRequestMessage(
                    dataStructureInterface -> {
                        this.dataStructureInterface = dataStructureInterface;
                        InvalidateInterface invalidateInterface = invalidateInterfaceMap.get(INDICATOR_NAME);
                        if (invalidateInterface != null) {
                            invalidateInterface.invalidate();
                        }
                    }));
                provider.sendUserMessage(getGeneratorMessage(true));
                provider.sendUserMessage(getIndicatorMessage(true));
            }
        }
    }
    

    private Layer1ApiUserMessageAddStrategyUpdateGenerator getGeneratorMessage(boolean isAdd) {
        return new Layer1ApiUserMessageAddStrategyUpdateGenerator(Layer1CustomEventsDemo.class, TREE_NAME, isAdd, true, new StrategyUpdateGenerator() {
            private Consumer<CustomGeneratedEventAliased> consumer;
            
            private long time = 0;
            
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
                Double lastValue = aliasToCurrentValue.get(alias);
                
                if (lastValue == null) {
                    lastValue = price;
                } else {
                    lastValue  = (lastValue + price) / 2.;
                }
                
                aliasToCurrentValue.put(alias, lastValue);
                
                this.consumer.accept(new CustomGeneratedEventAliased(new CustomTradePriceEvent(time, lastValue), alias));
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
                this.time = time;
            }
        }, new GeneratedEventInfo[] {new GeneratedEventInfo(CustomTradePriceEvent.class, CustomTradePriceAggregationEvent.class, CUSTOM_TRADE_EVENTS_AGGREGATOR)});
    }
    
    @Override
    public StrategyPanel[] getCustomGuiFor(String alias, String indicatorName) {
        StrategyPanel panel = new StrategyPanel("Colors", new GridBagLayout());
         
        panel.setLayout(new GridBagLayout());
        GridBagConstraints gbConst;
         
        IndicatorColorInterface indicatorColorInterface = new IndicatorColorInterface() {
            @Override
            public void set(String name, Color color) {
                setColor(alias, name, color);
            }
            
            @Override
            public Color getOrDefault(String name, Color defaultValue) {
                Color color = getSettingsFor(alias).getColor(name);
                return color == null ? defaultValue : color;
            }
            
            @Override
            public void addColorChangeListener(ColorsChangedListener listener) {
            }
        };
        
        ColorsConfigItem configItemLines = new ColorsConfigItem(LINE_COLOR_NAME, LINE_COLOR_NAME, true,
                LINE_COLOR_DEFAULT, indicatorColorInterface, new ColorsChangedListener() {
                    @Override
                    public void onColorsChanged() {
                        InvalidateInterface invalidaInterface = invalidateInterfaceMap.get(INDICATOR_NAME);
                        if (invalidaInterface != null) {
                            invalidaInterface.invalidate();
                        }
                    }
                });
        
        gbConst = new GridBagConstraints();
        gbConst.gridx = 0;
        gbConst.gridy = 0;
        gbConst.weightx = 1;
        gbConst.insets = new Insets(5, 5, 5, 5);
        gbConst.fill = GridBagConstraints.HORIZONTAL;
        panel.add(configItemLines, gbConst);
        
        return new StrategyPanel[] {panel};
    }
    
    private Layer1ApiUserMessageModifyIndicator getIndicatorMessage(boolean isAdd) {
        return new Layer1ApiUserMessageModifyIndicator(Layer1CustomEventsDemo.class, INDICATOR_NAME, isAdd,
                new IndicatorColorScheme() {
                    @Override
                    public ColorDescription[] getColors() {
                        return new ColorDescription[] {new ColorDescription(Layer1CustomEventsDemo.class, LINE_COLOR_NAME, LINE_COLOR_DEFAULT, false)};
                    }
                    
                    @Override
                    public ColorIntervalResponse getColorIntervalsList(double valueFrom, double valueTo) {
                        return new ColorIntervalResponse(new String[] {LINE_COLOR_NAME}, new double[]{});
                    }
                    
                    @Override
                    public String getColorFor(Double value) {
                        return LINE_COLOR_NAME;
                    }
                    
                }, Layer1CustomEventsDemo.this, null, null, null, null, null, null, null, null, GraphType.PRIMARY, true, true, false, this, null);
    }
    
    private CustomEventsDemoSettings getSettingsFor(String alias) {
        synchronized (locker) {
            CustomEventsDemoSettings settings = settingsMap.get(alias);
            if (settings == null) {
                settings = (CustomEventsDemoSettings) settingsAccess.getSettings(alias, INDICATOR_NAME, CustomEventsDemoSettings.class);
                settingsMap.put(alias, settings);
            }
            return settings;
        }
    }
    
    protected void settingsChanged(String settingsAlias, CustomEventsDemoSettings settingsObject) {
        synchronized (locker) {
            settingsAccess.setSettings(settingsAlias, INDICATOR_NAME, settingsObject, settingsObject.getClass());
        }
    }
    
    @Override
    public void setColor(String alias, String name, Color color) {
        CustomEventsDemoSettings settings = getSettingsFor(alias);
        settings.setColor(name, color);
        settingsChanged(alias, settings);
    }

    @Override
    public Color getColor(String alias, String name) {
        Color color = getSettingsFor(alias).getColor(name);
        if (color == null) {
            switch (name) {
            case LINE_COLOR_NAME:
                color = LINE_COLOR_DEFAULT;
                break;
            default:
                Log.warn("Layer1CustomEventsDemo: unknown color name " + name);
                color = Color.WHITE;
                break;
            }
        }
        
        return color;
    }

    @Override
    public void addColorChangeListener(ColorsChangedListener listener) {
        // every one of our colors is modified only from one place
    }
    
    @Override
    public void acceptSettingsInterface(SettingsAccess settingsAccess) {
        this.settingsAccess = settingsAccess;
    }
}
