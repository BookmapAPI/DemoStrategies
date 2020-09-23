package velox.api.layer1.simplified.demo;

import java.awt.Color;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.function.BiFunction;

import velox.api.layer1.annotations.Layer1ApiVersion;
import velox.api.layer1.annotations.Layer1ApiVersionValue;
import velox.api.layer1.annotations.Layer1SimpleAttachable;
import velox.api.layer1.annotations.Layer1StrategyName;
import velox.api.layer1.data.InstrumentInfo;
import velox.api.layer1.data.TradeInfo;
import velox.api.layer1.messages.indicators.Layer1ApiUserMessageModifyIndicator.GraphType;
import velox.api.layer1.simplified.Api;
import velox.api.layer1.simplified.AxisGroup;
import velox.api.layer1.simplified.CustomModule;
import velox.api.layer1.simplified.DepthDataListener;
import velox.api.layer1.simplified.Indicator;
import velox.api.layer1.simplified.InitialState;
import velox.api.layer1.simplified.IntervalListener;
import velox.api.layer1.simplified.Intervals;
import velox.api.layer1.simplified.MarketByOrderDepthDataListener;
import velox.api.layer1.simplified.Parameter;
import velox.api.layer1.simplified.TradeDataListener;

/**
 * Illustrates how you can create "spiky" indicators with custom aggregation
 * functions. Note that functionality is experimental, so it is very likely to
 * change and/or contain bugs.
 */
@Layer1SimpleAttachable
@Layer1StrategyName("Message rate: live")
@Layer1ApiVersion(Layer1ApiVersionValue.VERSION2)
public class MessageRateIndicatorNoHistory implements CustomModule, TradeDataListener, IntervalListener, DepthDataListener, MarketByOrderDepthDataListener
{
    private static final long INTERVAL_DURATION = Intervals.INTERVAL_50_MILLISECONDS;
    private static final long INTERVALS_IN_SECOND = Intervals.INTERVAL_1_SECOND / INTERVAL_DURATION;

    @Parameter(name = "Peak rate instead of average")
    private Boolean showPeakRate = false;

    protected Indicator tradeRateIndicator;
    protected Indicator depthRateIndicator;
    protected Indicator mboRateIndicator;
    protected Indicator totalRateIndicator;
    
    private long tradesInLastInterval = 0;
    private long depthInLastInterval = 0;
    private long mboInLastInterval = 0;

    @Override
    public void initialize(String alias, InstrumentInfo info, Api api, InitialState initialState) {
        initialize(alias, info, api, initialState, showPeakRate);
    }
    
    public void initialize(String alias, InstrumentInfo info, Api api, InitialState initialState, boolean showPeakRate) {
        BiFunction<Double, Double, Double> aggregationFunction = showPeakRate
                ? Math::max
                : (a, b) -> (a + b) / 2;
                
        // Accessing experimental method (not part of public API)
        try {
            Method registerIndicatorModifiableMethod = api.getClass()
                    .getDeclaredMethod("registerIndicatorModifiable", String.class, GraphType.class, double.class, boolean.class, BiFunction.class);
            registerIndicatorModifiableMethod.setAccessible(true);
                
            tradeRateIndicator = (Indicator) registerIndicatorModifiableMethod.invoke(
                    api, "Trade events/sec", GraphType.BOTTOM, Double.NaN, true, aggregationFunction);
            depthRateIndicator = (Indicator) registerIndicatorModifiableMethod.invoke(
                    api, "Depth events/sec", GraphType.BOTTOM, Double.NaN, true, aggregationFunction);
            mboRateIndicator = (Indicator) registerIndicatorModifiableMethod.invoke(
                    api, "Mbo events rate/sec", GraphType.BOTTOM, Double.NaN, true, aggregationFunction);
            totalRateIndicator = (Indicator) registerIndicatorModifiableMethod.invoke(
                    api, "Total events/sec", GraphType.BOTTOM, Double.NaN, true, aggregationFunction);
        } catch (NoSuchMethodException | SecurityException | IllegalAccessException
                | IllegalArgumentException | InvocationTargetException e) {
            throw new RuntimeException("Failed to invoke experimental method", e);
        }
        
        tradeRateIndicator.setColor(Color.BLUE);
        depthRateIndicator.setColor(Color.YELLOW);
        mboRateIndicator.setColor(Color.ORANGE);
        totalRateIndicator.setColor(Color.RED);
        
        AxisGroup axisGroup = new AxisGroup();
        axisGroup.add(tradeRateIndicator);
        axisGroup.add(depthRateIndicator);
        axisGroup.add(mboRateIndicator);
        axisGroup.add(totalRateIndicator);
    }
    
    @Override
    public void stop() {
    }

    @Override
    public void onTrade(double price, int size, TradeInfo tradeInfo) {
        tradesInLastInterval++;
    }
    
    @Override
    public void onDepth(boolean isBid, int price, int size) {
        depthInLastInterval++;
    }
    
    @Override
    public void send(String orderId, boolean isBid, int price, int size) {
        mboInLastInterval++;
    }
    
    @Override
    public void replace(String orderId, int price, int size) {
        mboInLastInterval++;
    }
    
    @Override
    public void cancel(String orderId) {
        mboInLastInterval++;
    }

    @Override
    public long getInterval() {
        return INTERVAL_DURATION;
    }

    @Override
    public void onInterval() {
        long tradesPerSecond = tradesInLastInterval * INTERVALS_IN_SECOND;
        long depthPerSecond = depthInLastInterval * INTERVALS_IN_SECOND;
        long mboPerSecond = mboInLastInterval * INTERVALS_IN_SECOND;
        long totalPerSecond = tradesPerSecond + depthPerSecond + mboPerSecond;
        
        tradeRateIndicator.addPoint(tradesPerSecond);
        depthRateIndicator.addPoint(depthPerSecond);
        mboRateIndicator.addPoint(mboPerSecond);
        totalRateIndicator.addPoint(totalPerSecond);
        
        tradesInLastInterval = 0;
        depthInLastInterval = 0;
        mboInLastInterval = 0;
    }
}
