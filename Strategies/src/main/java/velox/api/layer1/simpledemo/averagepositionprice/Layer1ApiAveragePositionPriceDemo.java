package velox.api.layer1.simpledemo.averagepositionprice;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import velox.api.layer1.Layer1ApiAdminAdapter;
import velox.api.layer1.Layer1ApiFinishable;
import velox.api.layer1.Layer1ApiInstrumentListener;
import velox.api.layer1.Layer1ApiProvider;
import velox.api.layer1.Layer1CustomPanelsGetter;
import velox.api.layer1.annotations.Layer1ApiVersion;
import velox.api.layer1.annotations.Layer1ApiVersionValue;
import velox.api.layer1.annotations.Layer1Attachable;
import velox.api.layer1.annotations.Layer1StrategyName;
import velox.api.layer1.common.ListenableHelper;
import velox.api.layer1.common.Log;
import velox.api.layer1.data.ExecutionInfo;
import velox.api.layer1.data.InstrumentInfo;
import velox.api.layer1.data.OrderInfoUpdate;
import velox.api.layer1.datastructure.events.OrderExecutedEvent;
import velox.api.layer1.datastructure.events.OrderUpdatedEvent;
import velox.api.layer1.datastructure.events.OrderUpdatesExecutionsAggregationEvent;
import velox.api.layer1.layers.strategies.interfaces.CalculatedResultListener;
import velox.api.layer1.layers.strategies.interfaces.InvalidateInterface;
import velox.api.layer1.layers.strategies.interfaces.Layer1IndicatorColorInterface;
import velox.api.layer1.layers.strategies.interfaces.OnlineCalculatable;
import velox.api.layer1.layers.strategies.interfaces.OnlineValueCalculatorAdapter;
import velox.api.layer1.messages.UserMessageLayersChainCreatedTargeted;
import velox.api.layer1.messages.indicators.DataStructureInterface;
import velox.api.layer1.messages.indicators.IndicatorColorInterface;
import velox.api.layer1.messages.indicators.IndicatorColorScheme;
import velox.api.layer1.messages.indicators.IndicatorDisplayLogic;
import velox.api.layer1.messages.indicators.Layer1ApiDataInterfaceRequestMessage;
import velox.api.layer1.messages.indicators.Layer1ApiUserMessageModifyIndicator;
import velox.api.layer1.messages.indicators.SettingsAccess;
import velox.api.layer1.messages.indicators.ValuesFormatter;
import velox.api.layer1.settings.Layer1ConfigSettingsInterface;
import velox.api.layer1.messages.indicators.DataStructureInterface.StandardEvents;
import velox.api.layer1.messages.indicators.DataStructureInterface.TreeResponseInterval;
import velox.api.layer1.messages.indicators.Layer1ApiUserMessageModifyIndicator.GraphType;
import velox.colors.ColorsChangedListener;
import velox.gui.StrategyPanel;
import velox.gui.colors.ColorsConfigItem;

@Layer1Attachable
@Layer1StrategyName("Average Price")
@Layer1ApiVersion(Layer1ApiVersionValue.VERSION2)
public class Layer1ApiAveragePositionPriceDemo implements Layer1ApiFinishable,
    Layer1ApiAdminAdapter,
    Layer1ApiInstrumentListener, OnlineCalculatable,
    Layer1CustomPanelsGetter,
    Layer1ConfigSettingsInterface,
    Layer1IndicatorColorInterface {
    
    private static class CurrentState {
        private final double pips;
        public double avgPrice = 0;
        public int positon = 0;
        
        public CurrentState(double pips) {
            this.pips = pips;
        }
        
        public double getLineY() {
            return (positon == 0 || Double.isNaN(avgPrice)) ? Double.NaN : (avgPrice / pips);
        }
        
        @Override
        public String toString() {
            return "[" + positon + "@ " + avgPrice + "]";
        }
    }
    
    private static final String INDICATOR_NAME = "Average Price";
    private static final String LINE_COLOR_NAME = "Line color";
    private static final Color LINE_COLOR_DEFAULT = Color.BLUE;
    
    private Layer1ApiProvider provider;
    
    private Map<String, Double> pipsMap = new ConcurrentHashMap<>();
    
    private Map<String, Map<String, Boolean>> aliasToBuyInfo = new ConcurrentHashMap<>();
    private Map<String, Map<String, String>> aliasToOrderAliasInfo = new ConcurrentHashMap<>();
    
    private DataStructureInterface dataStructureInterface;
    
    private Map<String, AveragePositionPriceDemoSettings> settingsMap = new HashMap<>();
    
    private SettingsAccess settingsAccess;
    
    private Map<String, InvalidateInterface> invalidateInterfaceMap = new ConcurrentHashMap<>();
    
    private Object locker = new Object();
    
    public Layer1ApiAveragePositionPriceDemo(Layer1ApiProvider provider) {
        this.provider = provider;
        
        ListenableHelper.addListeners(provider, this);
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
                provider.sendUserMessage(getUserMessageAdd());
            }
        }
    }
    
    private OrderUpdatesExecutionsAggregationEvent getOrderEvent(TreeResponseInterval interval) {
        return (OrderUpdatesExecutionsAggregationEvent) interval.events.get(StandardEvents.ORDER.toString());
    }
    
    private void updateState(CurrentState state, ExecutionInfo executionInfo, Map<String, Boolean> orderIdToIsBuy) {
        Boolean isBuy = orderIdToIsBuy.get(executionInfo.orderId);
        
        if (isBuy != null && executionInfo.size != 0) {
            
            int positionDelta = executionInfo.size;
            if (!isBuy) {
                positionDelta = -positionDelta;
            }
            
            if (!isChangeSign(state.positon, state.positon + positionDelta)) {
                
                if (state.positon >= 0 && isBuy || state.positon <= 0 && !isBuy) {
                    double oldValue = state.positon * state.avgPrice;
                    double addValue = executionInfo.price * positionDelta;
                    
                    state.avgPrice = (oldValue + addValue) / (state.positon + positionDelta);
                    state.positon += positionDelta;
                } else {
                    state.positon += positionDelta;
                }
                
                
            } else {
                state.avgPrice = executionInfo.price;
                state.positon += positionDelta;
            }

        } else {
            Log.warn("Unknown execution's orderId " + executionInfo.orderId);
        }
    }
    
    private boolean isChangeSign(int a, int b) {
        return ((a > 0 && b < 0) || (a < 0 && b > 0));
    }
    
    private void updateState(String alias, CurrentState state, OrderUpdatesExecutionsAggregationEvent aggregationEvent, Map<String, Boolean> orderIdToIsBuy,
            Map<String, String> orderIdToAlias) {
        for (Object object: aggregationEvent.orderUpdates) {
            if (object instanceof OrderUpdatedEvent) {
                OrderUpdatedEvent event = (OrderUpdatedEvent) object;
                orderIdToIsBuy.put(event.orderInfoUpdate.orderId, event.orderInfoUpdate.isBuy);
                orderIdToAlias.put(event.orderInfoUpdate.orderId, event.orderInfoUpdate.instrumentAlias);
            } else if (object instanceof OrderExecutedEvent) {
                OrderExecutedEvent event = (OrderExecutedEvent) object;
                String orderAlias = orderIdToAlias.get(event.executionInfo.orderId);
                if (alias.equals(orderAlias)) {
                    updateState(state, event.executionInfo, orderIdToIsBuy);
                }
            } else {
                throw new IllegalArgumentException("Unknown event: " + object);
            }
        }
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
    
    @Override
    public void calculateValuesInRange(String indicatorName, String alias, long t0, long intervalWidth,
            int intervalsNumber, CalculatedResultListener listener) {
        if (dataStructureInterface == null) {
            listener.setCompleted();
            return;
        }
        
        ArrayList<TreeResponseInterval> intervalResponse = dataStructureInterface.get(t0, intervalWidth,
                intervalsNumber, alias, new StandardEvents[] { StandardEvents.ORDER });
        
        Double pips = pipsMap.get(alias);
        if (pips == null) {
            throw new IllegalStateException("Unknown pips for alias " + alias);
        }
        
        Map<String, Boolean> orderIdToIsBuy = new HashMap<>();
        Map<String, String> orderIdToAlias = new HashMap<>();
        
        CurrentState state = new CurrentState(pips);
        updateState(alias, state, getOrderEvent(intervalResponse.get(0)), orderIdToIsBuy, orderIdToAlias);
        
        for (int i = 1; i <= intervalsNumber; ++i) {
            updateState(alias, state, getOrderEvent(intervalResponse.get(i)), orderIdToIsBuy, orderIdToAlias);
            
            listener.provideResponse(state.getLineY());
        }
        
        aliasToBuyInfo.put(alias, orderIdToIsBuy);
        aliasToOrderAliasInfo.put(alias, orderIdToAlias);
        
        listener.setCompleted();
        
    }

    @Override
    public OnlineValueCalculatorAdapter createOnlineValueCalculator(String indicatorName, String alias, long time,
            Consumer<Object> listener, InvalidateInterface invalidateInterface) {
        invalidateInterfaceMap.put(INDICATOR_NAME, invalidateInterface);
        
        if (dataStructureInterface == null) {
            return new OnlineValueCalculatorAdapter() {};
        }
        
        TreeResponseInterval treeResponse = dataStructureInterface.get(time, alias, new StandardEvents[] {StandardEvents.ORDER});
        
        Double pips = pipsMap.get(alias);
        if (pips == null) {
            throw new IllegalStateException("Unknown pips for alias " + alias);
        }
        
        if (!aliasToBuyInfo.containsKey(alias)) {
            aliasToBuyInfo.put(alias, new HashMap<>());
        }
        
        if (!aliasToOrderAliasInfo.containsKey(alias)) {
            aliasToOrderAliasInfo.put(alias, new HashMap<>());
        }
        
        Map<String, Boolean> orderIdToIsBuy = aliasToBuyInfo.get(alias);
        Map<String, String> orderIdToAlias = aliasToOrderAliasInfo.get(alias);
        
        CurrentState state = new CurrentState(pips);
        updateState(alias, state, getOrderEvent(treeResponse), orderIdToIsBuy, orderIdToAlias);
        
        return new OnlineValueCalculatorAdapter() {
            @Override
            public void onOrderUpdated(OrderInfoUpdate orderInfoUpdate) {
                orderIdToIsBuy.put(orderInfoUpdate.orderId, orderInfoUpdate.isBuy);
                orderIdToAlias.put(orderInfoUpdate.orderId, orderInfoUpdate.instrumentAlias);
            }
            
            @Override
            public void onOrderExecuted(ExecutionInfo executionInfo) {
                String orderAlias = orderIdToAlias.get(executionInfo.orderId);
                if (alias.equals(orderAlias)) {
                    updateState(state, executionInfo, orderIdToIsBuy);
                    listener.accept(state.getLineY());
                }
            }
        };
    }

    @Override
    public void onInstrumentAdded(String alias, InstrumentInfo instrumentInfo) {
        pipsMap.put(alias, instrumentInfo.pips);
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
    public void finish() {
        provider.sendUserMessage(new Layer1ApiUserMessageModifyIndicator(Layer1ApiAveragePositionPriceDemo.class, INDICATOR_NAME, false));
    }
    
    private Layer1ApiUserMessageModifyIndicator getUserMessageAdd() {
        return new Layer1ApiUserMessageModifyIndicator(Layer1ApiAveragePositionPriceDemo.class, INDICATOR_NAME, true,
                new IndicatorColorScheme() {
                    @Override
                    public ColorDescription[] getColors() {
                        return new ColorDescription[] {
                                new ColorDescription(Layer1ApiAveragePositionPriceDemo.class, LINE_COLOR_NAME, LINE_COLOR_DEFAULT, false),
                        };
                    }
                    
                    @Override
                    public String getColorFor(Double value) {
                        return LINE_COLOR_NAME;
                    }

                    @Override
                    public ColorIntervalResponse getColorIntervalsList(double valueFrom, double valueTo) {
                        return new ColorIntervalResponse(new String[] {LINE_COLOR_NAME}, new double[] {});
                    }
                }, Layer1ApiAveragePositionPriceDemo.this, null, Color.white, Color.black, new IndicatorDisplayLogic().setValuesFormatter(new ValuesFormatter() {
                    @Override
                    public String formatWidget(double value) {
                        return Double.isNaN(value) ? "" : String.format("%.2f", value);
                    }
                    
                    @Override
                    public String formatTooltip(double value, double minValue, double maxValue, int pixelsCount) {
                        return String.format("%.2f", value);
                    }
                }),
                null, null, null, null, GraphType.PRIMARY, true, false, null, this, null);
    }
    
    private AveragePositionPriceDemoSettings getSettingsFor(String alias) {
        synchronized (locker) {
            AveragePositionPriceDemoSettings settings = settingsMap.get(alias);
            if (settings == null) {
                settings = (AveragePositionPriceDemoSettings) settingsAccess.getSettings(alias, INDICATOR_NAME, AveragePositionPriceDemoSettings.class);
                settingsMap.put(alias, settings);
            }
            return settings;
        }
    }
    
    protected void settingsChanged(String settingsAlias, AveragePositionPriceDemoSettings settingsObject) {
        synchronized (locker) {
            settingsAccess.setSettings(settingsAlias, INDICATOR_NAME, settingsObject, settingsObject.getClass());
        }
    }
    
    @Override
    public void setColor(String alias, String name, Color color) {
        AveragePositionPriceDemoSettings settings = getSettingsFor(alias);
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
                Log.warn("Layer1ApiAveragePositionPriceDemo: unknown color name " + name);
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
