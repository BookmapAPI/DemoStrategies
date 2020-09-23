package velox.api.layer1.simplified.demo;

import velox.api.layer1.annotations.Layer1ApiVersion;
import velox.api.layer1.annotations.Layer1ApiVersionValue;
import velox.api.layer1.annotations.Layer1SimpleAttachable;
import velox.api.layer1.annotations.Layer1StrategyName;
import velox.api.layer1.data.InstrumentInfo;
import velox.api.layer1.simplified.Api;
import velox.api.layer1.simplified.HistoricalModeAdapter;
import velox.api.layer1.simplified.InitialState;
import velox.api.layer1.simplified.Parameter;

@Layer1SimpleAttachable
@Layer1StrategyName("Message rate: history+mode")
@Layer1ApiVersion(Layer1ApiVersionValue.VERSION2)
public class MessageRateIndicatorWithHistoryMode extends MessageRateIndicatorWithHistoryData
    implements HistoricalModeAdapter
{
    @Parameter(name = "Peak rate instead of average")
    private Boolean showPeakRate = false;
    
    @Override
    public void initialize(String alias, InstrumentInfo info, Api api, InitialState initialState) {
        initialize(alias, info, api, initialState, showPeakRate);
    }
}
