package velox.api.layer1.simplified.demo;

import java.awt.Color;

import velox.api.layer1.annotations.Layer1ApiVersion;
import velox.api.layer1.annotations.Layer1ApiVersionValue;
import velox.api.layer1.annotations.Layer1SimpleAttachable;
import velox.api.layer1.annotations.Layer1StrategyName;
import velox.api.layer1.data.InstrumentInfo;
import velox.api.layer1.data.TradeInfo;
import velox.api.layer1.messages.indicators.Layer1ApiUserMessageModifyIndicator.GraphType;
import velox.api.layer1.simplified.Api;
import velox.api.layer1.simplified.AxisGroup;
import velox.api.layer1.simplified.AxisRules;
import velox.api.layer1.simplified.CustomModule;
import velox.api.layer1.simplified.Indicator;
import velox.api.layer1.simplified.InitialState;
import velox.api.layer1.simplified.Parameter;
import velox.api.layer1.simplified.TradeDataListener;
import velox.api.layer1.simplified.WidgetGroup;
import velox.api.layer1.simplified.WidgetRules;

@Layer1SimpleAttachable
@Layer1StrategyName("Last trade Twins (group)")
@Layer1ApiVersion(Layer1ApiVersionValue.VERSION1)
public class LastTradeDemoNoHistoryTwins implements CustomModule, TradeDataListener
{
    private Indicator lastTradeIndicator;
    private Indicator lastTradeIndicatorCopy;
    
    @Parameter(name = "Price shift", step = 1.0)
    Integer priceShift = 2;
    
    @Parameter(name = "Price shift 2", step = 1.0)
    Short priceShift0 = 2;
    
    @Parameter(name = "Second Color")
    Color secondColor = Color.ORANGE;
    
    @Parameter(name = "Enable price shift")
    Boolean isEnabledPriceShift = true;
    
    @Parameter(name = "First Color")
    Color firstColor = Color.YELLOW;
    
    @Parameter(name = "Text sample")
    String textSample = "hello world";
         
    @Override
    public void initialize(String alias, InstrumentInfo info, Api api, InitialState initialState) {
        WidgetGroup widgetGroup = new WidgetGroup();
        widgetGroup.setWidgetRules(new WidgetRules(4000d, 50000d));
        lastTradeIndicator = api.registerIndicator("Last trade, no history", GraphType.BOTTOM, Double.NaN);
        lastTradeIndicator.setColor(firstColor);
        
        AxisGroup axisGroup = new AxisGroup();
        AxisRules rules = new AxisRules();
        rules.setForcedMax(4400);
        rules.setForcedMin(4380);
        axisGroup.setAxisRules(rules);
        lastTradeIndicator.setAxisGroup(axisGroup);
        lastTradeIndicatorCopy = api.registerIndicator("Copy of Last trade, no history", GraphType.BOTTOM, Double.NaN);
        lastTradeIndicatorCopy.setColor(secondColor);
        lastTradeIndicatorCopy.setAxisGroup(axisGroup);
        
        
    }

    @Override
    public void onTrade(double price, int size, TradeInfo tradeInfo) {
        Double shift = isEnabledPriceShift == true? priceShift : 0.0;
        lastTradeIndicator.addPoint(price + shift);
        lastTradeIndicatorCopy.addPoint(price + shift - 1);
    }

    @Override
    public void stop() {
        // TODO Auto-generated method stub
    }
    
}
