package velox.api.layer1.simplified.demo;

import java.awt.Color;

import velox.api.layer1.annotations.Layer1ApiVersion;
import velox.api.layer1.annotations.Layer1ApiVersionValue;
import velox.api.layer1.annotations.Layer1SimpleAttachable;
import velox.api.layer1.annotations.Layer1StrategyName;
import velox.api.layer1.data.InstrumentInfo;
import velox.api.layer1.messages.indicators.Layer1ApiUserMessageModifyIndicator.GraphType;
import velox.api.layer1.simplified.Api;
import velox.api.layer1.simplified.BboListener;
import velox.api.layer1.simplified.CustomModule;
import velox.api.layer1.simplified.HistoricalDataListener;
import velox.api.layer1.simplified.Indicator;
import velox.api.layer1.simplified.InitialState;

/**
 * Follows BBO.Shows BBO sizes in bottom panel
 */
@Layer1SimpleAttachable
@Layer1StrategyName("BBO: with history")
@Layer1ApiVersion(Layer1ApiVersionValue.VERSION2)
public class BboWithHistoryData implements
    CustomModule, BboListener, HistoricalDataListener {

    private Indicator bidPriceIndicator;
    private Indicator askPriceIndicator;
    private Indicator bidSizeIndicator;
    private Indicator askSizeIndicator;

    @Override
    public void initialize(String alias, InstrumentInfo info, Api api, InitialState initialState) {
        bidPriceIndicator = api.registerIndicator("Bid", GraphType.PRIMARY);
        askPriceIndicator = api.registerIndicator("Ask", GraphType.PRIMARY);

        bidSizeIndicator = api.registerIndicator("Bid size", GraphType.BOTTOM);
        askSizeIndicator = api.registerIndicator("Ask size", GraphType.BOTTOM);
        
        bidPriceIndicator.setColor(Color.RED);
        bidSizeIndicator.setColor(Color.RED);
        askPriceIndicator.setColor(Color.GREEN);
        askSizeIndicator.setColor(Color.GREEN);
    }
    
    @Override
    public void stop() {
    }

    @Override
    public void onBbo(int bidPrice, int bidSize, int askPrice, int askSize) {
        bidPriceIndicator.addPoint(bidPrice);
        askPriceIndicator.addPoint(askPrice);
        bidSizeIndicator.addPoint(bidSize);
        askSizeIndicator.addPoint(askSize);
    }
}