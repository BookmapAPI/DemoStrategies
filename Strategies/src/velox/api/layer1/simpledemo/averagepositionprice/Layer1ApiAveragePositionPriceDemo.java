package velox.api.layer1.simpledemo.averagepositionprice;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import velox.api.layer1.Layer1ApiAdminAdapter;
import velox.api.layer1.Layer1ApiFinishable;
import velox.api.layer1.Layer1ApiInstrumentListener;
import velox.api.layer1.Layer1ApiProvider;
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
import velox.api.layer1.layers.strategies.interfaces.OnlineCalculatable;
import velox.api.layer1.layers.strategies.interfaces.OnlineValueCalculatorAdapter;
import velox.api.layer1.messages.UserMessageLayersChainCreatedTargeted;
import velox.api.layer1.messages.indicators.DataStructureInterface;
import velox.api.layer1.messages.indicators.IndicatorColorScheme;
import velox.api.layer1.messages.indicators.Layer1ApiDataInterfaceRequestMessage;
import velox.api.layer1.messages.indicators.Layer1ApiUserMessageModifyIndicator;
import velox.api.layer1.messages.indicators.DataStructureInterface.StandardEvents;
import velox.api.layer1.messages.indicators.DataStructureInterface.TreeResponseInterval;
import velox.api.layer1.messages.indicators.Layer1ApiUserMessageModifyIndicator.GraphType;
import velox.api.layer1.simpledemo.markers.Layer1ApiMarkersDemo;

@Layer1Attachable
@Layer1StrategyName("Average Price")
public class Layer1ApiAveragePositionPriceDemo implements Layer1ApiFinishable,
    Layer1ApiAdminAdapter,
    Layer1ApiInstrumentListener, OnlineCalculatable {
    
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
    private static final String FULL_INDICATOR_NAME = "demo." + INDICATOR_NAME;
    
    private Layer1ApiProvider provider;
    
    private Map<String, Double> pipsMap = new ConcurrentHashMap<>();
    
    private Map<String, Map<String, Boolean>> aliasToBuyInfo = new ConcurrentHashMap<>();
    
    private DataStructureInterface dataStructureInterface;
    
    public Layer1ApiAveragePositionPriceDemo(Layer1ApiProvider provider) {
        this.provider = provider;
        
        ListenableHelper.addListeners(provider, this);
    }
    
    @Override
    public void onUserMessage(Object data) {
        if (data.getClass() == UserMessageLayersChainCreatedTargeted.class) {
            UserMessageLayersChainCreatedTargeted message = (UserMessageLayersChainCreatedTargeted) data;
            if (message.targetClass == getClass()) {
                provider.sendUserMessage(new Layer1ApiDataInterfaceRequestMessage(dataStructureInterface -> this.dataStructureInterface = dataStructureInterface));
                provider.sendUserMessage(getUserMessageAdd());
            }
        }
    }
    
    private OrderUpdatesExecutionsAggregationEvent getOrderEvent(TreeResponseInterval interval) {
        return (OrderUpdatesExecutionsAggregationEvent) interval.events.get(StandardEvents.ORDER.toString());
    }
    
    private void updateState(CurrentState state, ExecutionInfo executionInfo, Map<String, Boolean> orderIdToIsBuy) {
        Boolean isBuy = orderIdToIsBuy.get(executionInfo.orderId);
        if (isBuy != null) {
            
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
    
    private void updateState(CurrentState state, OrderUpdatesExecutionsAggregationEvent aggregationEvent, Map<String, Boolean> orderIdToIsBuy) {
        for (Object object: aggregationEvent.orderUpdates) {
            if (object instanceof OrderUpdatedEvent) {
                OrderUpdatedEvent event = (OrderUpdatedEvent) object;
                orderIdToIsBuy.put(event.orderInfoUpdate.orderId, event.orderInfoUpdate.isBuy);
            } else if (object instanceof OrderExecutedEvent) {
                OrderExecutedEvent event = (OrderExecutedEvent) object;
                updateState(state, event.executionInfo, orderIdToIsBuy);
            } else {
                throw new IllegalArgumentException("Unknown event: " + object);
            }
        }
        
        
    }
    
    @Override
    public void calculateValuesInRange(String indicatorName, String alias, long t0, long intervalWidth,
            int intervalsNumber, CalculatedResultListener listener) {
        
        ArrayList<TreeResponseInterval> intervalResponse = dataStructureInterface.get(t0, intervalWidth,
                intervalsNumber, alias, new StandardEvents[] { StandardEvents.ORDER });
        
        Double pips = pipsMap.get(alias);
        if (pips == null) {
            throw new IllegalStateException("Unknown pips for alias " + alias);
        }
        
        Map<String, Boolean> orderIdToIsBuy = new HashMap<>();
        aliasToBuyInfo.put(alias, orderIdToIsBuy);
        
        CurrentState state = new CurrentState(pips);
        updateState(state, getOrderEvent(intervalResponse.get(0)), orderIdToIsBuy);
        
        for (int i = 1; i <= intervalsNumber; ++i) {
            updateState(state, getOrderEvent(intervalResponse.get(i)), orderIdToIsBuy);
            
            listener.provideResponse(state.getLineY());
        }
        
        listener.setCompleted();
        
    }

    @Override
    public OnlineValueCalculatorAdapter createOnlineValueCalculator(String indicatorName, String alias, long time,
            Consumer<Object> listener, InvalidateInterface invalidateInterface) {
        
        TreeResponseInterval treeResponse = dataStructureInterface.get(time, alias, new StandardEvents[] {StandardEvents.ORDER});
        
        Double pips = pipsMap.get(alias);
        if (pips == null) {
            throw new IllegalStateException("Unknown pips for alias " + alias);
        }
        
        if (!aliasToBuyInfo.containsKey(alias)) {
            aliasToBuyInfo.put(alias, new HashMap<>());
        }
        Map<String, Boolean> orderIdToIsBuy = aliasToBuyInfo.get(alias);
        
        CurrentState state = new CurrentState(pips);
        updateState(state, getOrderEvent(treeResponse), orderIdToIsBuy);
        
        return new OnlineValueCalculatorAdapter() {
            @Override
            public void onOrderUpdated(OrderInfoUpdate orderInfoUpdate) {
                orderIdToIsBuy.put(orderInfoUpdate.orderId, orderInfoUpdate.isBuy);
            }
            
            @Override
            public void onOrderExecuted(ExecutionInfo executionInfo) {
                updateState(state, executionInfo, orderIdToIsBuy);
                listener.accept(state.getLineY());
            }
        };
    }

    @Override
    public void onInstrumentAdded(String alias, InstrumentInfo instrumentInfo) {
        pipsMap.put(alias, instrumentInfo.pips);
        provider.sendUserMessage(getUserMessageAdd());
    }

    @Override
    public void onInstrumentRemoved(String alias) {
        pipsMap.remove(alias);
    }
    
    @Override
    public void onInstrumentNotFound(String symbol, String exchange, String type) {
    }

    @Override
    public void onInstrumentAlreadySubscribed(String symbol, String exchange, String type) {
    }

    @Override
    public void finish() {
        provider.sendUserMessage(new Layer1ApiUserMessageModifyIndicator(FULL_INDICATOR_NAME, INDICATOR_NAME, false));
    }
    
    private Layer1ApiUserMessageModifyIndicator getUserMessageAdd() {
        return new Layer1ApiUserMessageModifyIndicator(FULL_INDICATOR_NAME, INDICATOR_NAME, true,
                new IndicatorColorScheme() {
                    @Override
                    public ColorDescription[] getColors() {
                        return new ColorDescription[] {
                                new ColorDescription(Layer1ApiMarkersDemo.class, INDICATOR_NAME, Color.blue, true),
                        };
                    }
                    
                    @Override
                    public String getColorFor(Double value) {
                        return INDICATOR_NAME;
                    }

                    @Override
                    public ColorIntervalResponse getColorIntervalsList(double valueFrom, double valueTo) {
                        return new ColorIntervalResponse(new String[] {INDICATOR_NAME}, new double[] {});
                    }
                }, null, Color.white, Color.black, null,
                null, null, null, null, GraphType.PRIMARY, false, false, null, this, null);
    }
}
