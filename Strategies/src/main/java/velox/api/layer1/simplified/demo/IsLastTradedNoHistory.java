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
import velox.api.layer1.simplified.Indicator;
import velox.api.layer1.simplified.InitialState;
import velox.api.layer1.simplified.MultiInstrumentListener;
import velox.api.layer1.simplified.TradeDataListener;

/**
 * Only draws the line if this instrument traded last
 */
@Layer1SimpleAttachable
@Layer1StrategyName("Is last trade: live")
@Layer1ApiVersion(Layer1ApiVersionValue.VERSION2)
public class IsLastTradedNoHistory implements
    CustomModule, TradeDataListener, MultiInstrumentListener
{
    private String outputInstrumetnAlias;
    private String currentInstrumentAlias;
    private Indicator isLastTraded;

    @Override
    public void initialize(String alias, InstrumentInfo info, Api api, InitialState initialState) {
        isLastTraded = api.registerIndicator("Is last traded?",
                GraphType.BOTTOM);
        isLastTraded.setColor(Color.BLUE);
        outputInstrumetnAlias = alias;
    }
    
    @Override
    public void stop() {}

    @Override
    public void onTrade(double price, int size, TradeInfo tradeInfo) {
        if (currentInstrumentAlias.equals(outputInstrumetnAlias)) {
            isLastTraded.addPoint(1);
        } else {
            isLastTraded.addPoint(Double.NaN);
        }
    }

    @Override
    public void onCurrentInstrument(String alias) {
         currentInstrumentAlias = alias;
    }
    
    @Override
    public void onInstrumentAdded(InstrumentInfo info) {
        Log.info("[IsLastTradedNoHistory] indicator for " + outputInstrumetnAlias + " received onAdded for "
                + currentInstrumentAlias + " info " + info);
    }
}
