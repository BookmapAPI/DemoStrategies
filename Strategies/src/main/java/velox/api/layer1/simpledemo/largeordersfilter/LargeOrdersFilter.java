package velox.api.layer1.simpledemo.largeordersfilter;

import velox.api.layer1.Layer1ApiFinishable;
import velox.api.layer1.Layer1ApiProvider;
import velox.api.layer1.annotations.Layer1ApiVersion;
import velox.api.layer1.annotations.Layer1ApiVersionValue;
import velox.api.layer1.annotations.Layer1Injectable;
import velox.api.layer1.annotations.Layer1StrategyDateLicensed;
import velox.api.layer1.annotations.Layer1StrategyName;
import velox.api.layer1.data.OrderInfo;
import velox.api.layer1.data.OrderInfoBuilder;
import velox.api.layer1.data.OrderInfoUpdate;
import velox.api.layer1.data.OrderSendParameters;
import velox.api.layer1.data.OrderStatus;
import velox.api.layer1.data.OrderType;
import velox.api.layer1.data.SimpleOrderSendParameters;
import velox.api.layer1.layers.Layer1ApiInjectorRelay;

/**
 * Block all orders larger then 10.
 */
// This order is not necessarily a trading strategy since it does not generate new orders.
// Still, to get additional protection you might want to add @Layer1TradingStrategy
@Layer1Injectable
@Layer1StrategyName("Large orders filter")
@Layer1StrategyDateLicensed("BmDemo-LargeOrdersFilter")
@Layer1ApiVersion(Layer1ApiVersionValue.VERSION2)
public class LargeOrdersFilter extends Layer1ApiInjectorRelay implements Layer1ApiFinishable {

    public LargeOrdersFilter(Layer1ApiProvider provider) {
        super(provider);
    }

    @Override
    public void sendOrder(OrderSendParameters orderSendParameters) {

        if (orderSendParameters instanceof SimpleOrderSendParameters) {
            SimpleOrderSendParameters simpleOrderSendParameters = (SimpleOrderSendParameters) orderSendParameters;
            if (simpleOrderSendParameters.size <= 10) {
                super.sendOrder(orderSendParameters);
            } else {
                String fakeOrderId = "Fake-" + Math.random();
                long currentTime = provider.getCurrentTime();
                
                // We need to simulate the following sequence:
                // 1) Order is sent
                // 2) Order is rejected
                // If we just send "REJECTED" update Bookmap will handle it as historical order and won't display it on heatmap
                OrderInfo orderInfo1 = new OrderInfo(
                        simpleOrderSendParameters.alias,
                        fakeOrderId,
                        simpleOrderSendParameters.isBuy,
                        OrderType.getTypeFromPrices(simpleOrderSendParameters.stopPrice,
                                simpleOrderSendParameters.limitPrice),
                        simpleOrderSendParameters.clientId,
                        simpleOrderSendParameters.doNotIncrease,
                        0, simpleOrderSendParameters.size,
                        Double.NaN,
                        simpleOrderSendParameters.duration,
                        OrderStatus.PENDING_SUBMIT,
                        simpleOrderSendParameters.limitPrice,
                        simpleOrderSendParameters.stopPrice,
                        false, currentTime);
                OrderInfoUpdate orderInfoUpdate1 = new OrderInfoUpdate(orderInfo1);

                OrderInfoBuilder orderInfoUpdate2Template = orderInfoUpdate1.toBuilder();
                orderInfoUpdate2Template.setStatus(OrderStatus.REJECTED);
                OrderInfoUpdate orderInfoUpdate2 = orderInfoUpdate2Template.build();
                
                inject(() -> {
                    super.onOrderUpdated(orderInfoUpdate1);
                    super.onOrderUpdated(orderInfoUpdate2);
                });

            }
        } else {
            super.sendOrder(orderSendParameters);
        }

    }

    @Override
    public void finish() {
        // Our strategy is too simple to do anything here
    }
}
