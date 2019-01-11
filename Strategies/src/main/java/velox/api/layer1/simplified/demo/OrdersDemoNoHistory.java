package velox.api.layer1.simplified.demo;

import java.awt.Color;

import velox.api.layer1.annotations.Layer1ApiVersion;
import velox.api.layer1.annotations.Layer1ApiVersionValue;
import velox.api.layer1.annotations.Layer1SimpleAttachable;
import velox.api.layer1.annotations.Layer1StrategyName;
import velox.api.layer1.data.BalanceInfo;
import velox.api.layer1.data.ExecutionInfo;
import velox.api.layer1.data.InstrumentInfo;
import velox.api.layer1.data.OrderInfoUpdate;
import velox.api.layer1.data.StatusInfo;
import velox.api.layer1.messages.indicators.Layer1ApiUserMessageModifyIndicator.GraphType;
import velox.api.layer1.simplified.Api;
import velox.api.layer1.simplified.BalanceListener;
import velox.api.layer1.simplified.CustomModule;
import velox.api.layer1.simplified.Indicator;
import velox.api.layer1.simplified.InitialState;
import velox.api.layer1.simplified.OrdersListener;
import velox.api.layer1.simplified.PositionListener;

@Layer1SimpleAttachable
@Layer1StrategyName("Basic orders/balance info")
@Layer1ApiVersion(Layer1ApiVersionValue.VERSION2)
public class OrdersDemoNoHistory implements
    CustomModule, OrdersListener, BalanceListener, PositionListener {
    
    private Indicator lastExecutionPrice;
    private Indicator lastOrderLimitPrice;
    private Indicator balance;
    private Indicator position;
    
    private double pips;
    
    @Override
    public void initialize(String alias, InstrumentInfo info, Api api, InitialState initialState) {
        pips = info.pips;
        
        lastExecutionPrice = api.registerIndicator("Last execution price", GraphType.PRIMARY);
        lastOrderLimitPrice = api.registerIndicator("Last order limit price", GraphType.PRIMARY);
        balance = api.registerIndicator("Balance", GraphType.BOTTOM);
        position = api.registerIndicator("Position", GraphType.BOTTOM);
        
        lastExecutionPrice.setColor(Color.ORANGE);
        lastOrderLimitPrice.setColor(Color.BLUE);
        balance.setColor(Color.CYAN);
        position.setColor(Color.WHITE);
    }
    
    @Override
    public void stop() {
    }

    @Override
    public void onPositionUpdate(StatusInfo statusInfo) {
        position.addPoint(statusInfo.position);
    }

    @Override
    public void onBalance(BalanceInfo balanceInfo) {
        balance.addPoint(balanceInfo.balancesInCurrency.get(0).balance);
    }

    @Override
    public void onOrderUpdated(OrderInfoUpdate orderInfoUpdate) {
        lastOrderLimitPrice.addPoint(orderInfoUpdate.limitPrice / pips);
    }

    @Override
    public void onOrderExecuted(ExecutionInfo executionInfo) {
        lastExecutionPrice.addPoint(executionInfo.price / pips);
    }
}
