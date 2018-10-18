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
import velox.api.layer1.simplified.CustomModule;
import velox.api.layer1.simplified.Indicator;
import velox.api.layer1.simplified.InitialState;
import velox.api.layer1.simplified.TradeDataListener;

@Layer1SimpleAttachable
@Layer1StrategyName("2_Last trade: live")
@Layer1ApiVersion(Layer1ApiVersionValue.VERSION1)
public class LastTradeDemoNoHistory2 implements CustomModule, TradeDataListener
{
    private Indicator lastTradeIndicator;

    @Parameter(name = "Price shift", step = 1.0)
    Double priceShift = 2.0;
    
    @Parameter(name = "Sample text", step = 0.0)
    String any = "Any";
    
    @Override
    public void initialize(String alias, InstrumentInfo info, Api api, InitialState initialState) {
        lastTradeIndicator = api.registerIndicator("Last trade, no history",
                GraphType.PRIMARY);
        lastTradeIndicator.setColor(Color.GREEN);
    }
    
    @Override
    public void stop() {
    }

    @Override
    public void onTrade(double price, int size, TradeInfo tradeInfo) {
        lastTradeIndicator.addPoint(price);
    }

    
}
