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
import velox.api.layer1.simplified.HistoricalModeListener;
import velox.api.layer1.simplified.Indicator;
import velox.api.layer1.simplified.InitialState;
import velox.api.layer1.simplified.TradeDataListener;

@Layer1SimpleAttachable
@Layer1StrategyName("Last trade: history+mode")
@Layer1ApiVersion(Layer1ApiVersionValue.VERSION2)
public class LastTradeDemoWithHistoryMode implements
    CustomModule, TradeDataListener, HistoricalModeListener {

    /** Last trade price */
    private Indicator lastTradeIndicator;
    /** 1 for historical data, 0 for live */
    private Indicator isHistoricalIndicator;

    @Override
    public void initialize(String alias, InstrumentInfo info, Api api, InitialState initialState) {
        lastTradeIndicator = api.registerIndicator("Last trade, with historical",
                GraphType.PRIMARY);
        isHistoricalIndicator = api.registerIndicator("1 if historical",
                GraphType.BOTTOM, 1);
        
        lastTradeIndicator.setColor(Color.ORANGE);
        isHistoricalIndicator.setColor(Color.YELLOW);
    }
    
    @Override
    public void stop() {
    }

    @Override
    public void onTrade(double price, int size, TradeInfo tradeInfo) {
        lastTradeIndicator.addPoint(price);
    }

    @Override
    public void onRealtimeStart() {
        isHistoricalIndicator.addPoint(0);
    }

}
