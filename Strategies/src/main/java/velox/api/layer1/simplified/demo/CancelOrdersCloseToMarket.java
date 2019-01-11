package velox.api.layer1.simplified.demo;

import java.util.HashMap;
import java.util.Map;

import velox.api.layer1.annotations.Layer1ApiVersion;
import velox.api.layer1.annotations.Layer1ApiVersionValue;
import velox.api.layer1.annotations.Layer1SimpleAttachable;
import velox.api.layer1.annotations.Layer1StrategyName;
import velox.api.layer1.annotations.Layer1TradingStrategy;
import velox.api.layer1.data.ExecutionInfo;
import velox.api.layer1.data.InstrumentInfo;
import velox.api.layer1.data.OrderCancelParameters;
import velox.api.layer1.data.OrderInfo;
import velox.api.layer1.data.OrderInfoUpdate;
import velox.api.layer1.simplified.Api;
import velox.api.layer1.simplified.BboListener;
import velox.api.layer1.simplified.CustomModule;
import velox.api.layer1.simplified.InitialState;
import velox.api.layer1.simplified.OrdersListener;

/**
 * Cancels limit orders that are too close to BBO on BBO change
 */
@Layer1SimpleAttachable
@Layer1TradingStrategy
@Layer1StrategyName("Cancel orders near BBO")
@Layer1ApiVersion(Layer1ApiVersionValue.VERSION2)
public class CancelOrdersCloseToMarket implements
    CustomModule, OrdersListener, BboListener{
    
    private static final int CANCEL_DISTANCE = 3;

    private Api api;
    
    private Map<String, OrderInfo> activeLimitOrders = new HashMap<>();
    private double pips;
    
    @Override
    public void initialize(String alias, InstrumentInfo info, Api api, InitialState initialState) {
        this.pips = info.pips;
        this.api = api;
    }
    
    @Override
    public void stop() {
    }
    
    @Override
    public void onBbo(int bidPrice, int bidSize, int askPrice, int askSize) {
        for (OrderInfo order : activeLimitOrders.values()) {
            // Since BBO is providing level number and order contains raw price, let's convert it to level number
            double orderLevel = order.limitPrice / pips;
            boolean shouldCancel = order.isBuy
                    ? bidPrice - orderLevel  <= CANCEL_DISTANCE
                    : orderLevel - askPrice <= CANCEL_DISTANCE;
            if (shouldCancel) {
                api.updateOrder(new OrderCancelParameters(order.orderId));
            }
        }
    }
    
    @Override
    public void onOrderUpdated(OrderInfoUpdate orderInfoUpdate) {
        boolean active = orderInfoUpdate.status.isActive();
        if (active) {
            activeLimitOrders.put(orderInfoUpdate.orderId, orderInfoUpdate);
        } else {
            activeLimitOrders.remove(orderInfoUpdate.orderId);
        }
    }
    
    @Override
    public void onOrderExecuted(ExecutionInfo executionInfo) {
        // Not needed for this strategy
    }
}
