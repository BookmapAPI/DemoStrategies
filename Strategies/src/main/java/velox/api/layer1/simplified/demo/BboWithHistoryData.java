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

/**
 * Follows BBO.Shows BBO sizes in bottom panel
 */
@Layer1SimpleAttachable
@Layer1StrategyName("BBO: with history")
@Layer1ApiVersion(Layer1ApiVersionValue.VERSION1)
public class BboWithHistoryData implements
    CustomModule, BboListener, HistoricalDataListener {

    private Indicator bidPriceIndicator;
    private Indicator askPriceIndicator;
    private Indicator bidSizeIndicator;
    private Indicator askSizeIndicator;

    @Override
    public void initialize(String alias, InstrumentInfo info, Api api) {
        bidPriceIndicator = api.registerIndicator("Bid", GraphType.PRIMARY, Color.RED);
        askPriceIndicator = api.registerIndicator("Ask", GraphType.PRIMARY, Color.GREEN);

        bidSizeIndicator = api.registerIndicator("Bid size", GraphType.BOTTOM, Color.RED);
        askSizeIndicator = api.registerIndicator("Ask size", GraphType.BOTTOM, Color.GREEN);
    }

    @Override
    public void onBbo(int bidPrice, int bidSize, int askPrice, int askSize) {
        bidPriceIndicator.addPoint(bidPrice);
        askPriceIndicator.addPoint(askPrice);
        bidSizeIndicator.addPoint(bidSize);
        askSizeIndicator.addPoint(askSize);
    }
}