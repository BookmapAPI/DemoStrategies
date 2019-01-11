package velox.api.layer1.simplified.demo;

import velox.api.layer1.annotations.Layer1ApiVersion;
import velox.api.layer1.annotations.Layer1ApiVersionValue;
import velox.api.layer1.annotations.Layer1SimpleAttachable;
import velox.api.layer1.annotations.Layer1StrategyName;
import velox.api.layer1.simplified.HistoricalDataListener;

@Layer1SimpleAttachable
@Layer1StrategyName("Trade interval: with history")
@Layer1ApiVersion(Layer1ApiVersionValue.VERSION2)
public class TradesIntervalWithHistory extends TradesIntervalNoHistory
    implements HistoricalDataListener {

}
