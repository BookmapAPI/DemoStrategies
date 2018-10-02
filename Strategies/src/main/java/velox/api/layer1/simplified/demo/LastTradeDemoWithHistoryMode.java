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
import velox.api.layer1.simplified.TradeDataListener;

@Layer1SimpleAttachable
@Layer1StrategyName("Last trade: history+mode")
@Layer1ApiVersion(Layer1ApiVersionValue.VERSION1)
public class LastTradeDemoWithHistoryMode implements
    CustomModule, TradeDataListener, HistoricalModeListener {

    /** Last trade price */
    private Indicator lastTradeIndicator;
    /** 1 for historical data, 0 for live */
    private Indicator isHistoricalIndicator;

    @Override
    public void initialize(InstrumentInfo info, Api api) {
        lastTradeIndicator = api.registerIndicator("Last trade, with historical",
                GraphType.PRIMARY, Color.ORANGE);
        isHistoricalIndicator = api.registerIndicator("1 if historical",
                GraphType.BOTTOM, Color.YELLOW, 1);
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
