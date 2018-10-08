package velox.api.layer1.simplified.demo;

import java.awt.Color;

import velox.api.layer1.annotations.Layer1ApiVersion;
import velox.api.layer1.annotations.Layer1ApiVersionValue;
import velox.api.layer1.annotations.Layer1SimpleAttachable;
import velox.api.layer1.annotations.Layer1StrategyName;
import velox.api.layer1.data.InstrumentInfo;
import velox.api.layer1.layers.utils.OrderBook;
import velox.api.layer1.messages.indicators.Layer1ApiUserMessageModifyIndicator.GraphType;
import velox.api.layer1.simplified.Api;
import velox.api.layer1.simplified.Bar;
import velox.api.layer1.simplified.BarDataListener;
import velox.api.layer1.simplified.CustomModule;
import velox.api.layer1.simplified.Indicator;
import velox.api.layer1.simplified.Intervals;

@Layer1SimpleAttachable
@Layer1StrategyName("Mid price from bars")
@Layer1ApiVersion(Layer1ApiVersionValue.VERSION1)
public class BarMidPriceNoHistory implements
    CustomModule, BarDataListener {

    private Indicator midPrice;

    @Override
    public void initialize(String alias, InstrumentInfo info, Api api) {
        midPrice = api.registerIndicator("Mid price", GraphType.PRIMARY, Color.CYAN);
    }
    
    @Override
    public void stop() {
    }

    @Override
    public void onBar(OrderBook ob, Bar bar) {
        midPrice.addPoint(ob.getMidPriceOrNan());
    }

    @Override
    public long getBarInterval() {
        return Intervals.INTERVAL_15_SECONDS;
    }
}
