package velox.api.layer1.simplified.demo;

import java.awt.Color;

import velox.api.layer1.annotations.Layer1ApiVersion;
import velox.api.layer1.annotations.Layer1ApiVersionValue;
import velox.api.layer1.annotations.Layer1SimpleAttachable;
import velox.api.layer1.annotations.Layer1StrategyName;
import velox.api.layer1.common.Log;
import velox.api.layer1.data.InstrumentInfo;
import velox.api.layer1.data.TradeInfo;
import velox.api.layer1.messages.indicators.Layer1ApiUserMessageModifyIndicator.GraphType;
import velox.api.layer1.simplified.Api;
import velox.api.layer1.simplified.CustomModule;
import velox.api.layer1.simplified.IndicatorModifiable;
import velox.api.layer1.simplified.InitialState;
import velox.api.layer1.simplified.TimeListener;
import velox.api.layer1.simplified.TradeDataListener;

@Layer1SimpleAttachable
@Layer1StrategyName("Indicator Modifiable Demo")
@Layer1ApiVersion(Layer1ApiVersionValue.VERSION2)
public class IndicatorModifiableDemo implements CustomModule, TradeDataListener, TimeListener
{
    private IndicatorModifiable lastTradeIndicator;
    private int pointNumber;
    private long timestamp;
    
    private long t10;
    private double point10;
    private long t25;
    private double point25;

    @Override
    public void initialize(String alias, InstrumentInfo info, Api api, InitialState initialState) {
        lastTradeIndicator = api.registerIndicatorModifiable("Indicator Modifiable GENERATOR",
                GraphType.PRIMARY);
        lastTradeIndicator.setColor(Color.ORANGE);
    }
    
    @Override
    public void stop() {
    }

    @Override
    public void onTrade(double price, int size, TradeInfo tradeInfo) {
        lastTradeIndicator.addPoint(price);
        pointNumber++;
        
        if (pointNumber <= 30 && pointNumber%5 == 0) {
            Log.info(pointNumber + " " + price);
        }
        
        if (pointNumber == 10) {
            t10 = timestamp;
            point10 = price;
        }
        if (pointNumber == 25) {
            t25 = timestamp;
            point25 = price;
        }
        if (pointNumber == 30) {
            lastTradeIndicator.clear(t10, t25);
            lastTradeIndicator.addPoint(t10, point10 + 50);
            lastTradeIndicator.addPoint(t25, point25 + 50);
        }
    }

    @Override
    public void onTimestamp(long t) {
        this.timestamp = t;
    }
}
