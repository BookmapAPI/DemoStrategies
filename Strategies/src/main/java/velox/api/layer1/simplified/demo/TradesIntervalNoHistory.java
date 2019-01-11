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
import velox.api.layer1.simplified.IntervalListener;
import velox.api.layer1.simplified.Intervals;
import velox.api.layer1.simplified.TimeListener;
import velox.api.layer1.simplified.TradeDataListener;

/**
 * Displays time since last trade
 */
@Layer1SimpleAttachable
@Layer1StrategyName("Trade interval: no history")
@Layer1ApiVersion(Layer1ApiVersionValue.VERSION2)
public class TradesIntervalNoHistory implements
        CustomModule,
        TradeDataListener,
        TimeListener,
        IntervalListener {

    private Indicator lastTradeInterval;
    private Indicator lastTradeIntervalSmooth;

    private long currentTime = 0;
    private long lastTradeTime = -1;

    @Override
    public void initialize(String alias, InstrumentInfo info, Api api, InitialState initialState) {
        lastTradeInterval = api.registerIndicator("Last trade interval", GraphType.BOTTOM);
        lastTradeIntervalSmooth = api.registerIndicator("Last trade interval (smooth)", GraphType.BOTTOM);
        
        lastTradeInterval.setColor(Color.RED);
        lastTradeIntervalSmooth.setColor(Color.GREEN);
    }
    
    @Override
    public void stop() {
    }

    @Override
    public void onTrade(double price, int size, TradeInfo tradeInfo) {

        boolean isFirstTrade = lastTradeTime == -1;

        if (!isFirstTrade) {
            publishInterval(lastTradeInterval);
        }
        lastTradeTime = currentTime;
    }

    @Override
    public void onTimestamp(long t) {
        currentTime = t;

        if (lastTradeTime != -1) {
            publishInterval(lastTradeIntervalSmooth);
        }
    }
    
    @Override
    public void onInterval() {
        // We don't need the call itself, but it is guaranteed to be called with requested frequency
    }
    
    @Override
    public long getInterval() {
        return Intervals.INTERVAL_50_MILLISECONDS;
    }

    private void publishInterval(Indicator indicator) {
        long interval = currentTime - lastTradeTime;
        double intervalInSeconds = interval / (double) Intervals.INTERVAL_1_SECOND;
        indicator.addPoint(intervalInSeconds);
    }
}
