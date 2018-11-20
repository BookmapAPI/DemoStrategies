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
import velox.api.layer1.simplified.HistoricalDataListener;
import velox.api.layer1.simplified.Indicator;
import velox.api.layer1.simplified.InitialState;
import velox.api.layer1.simplified.Intervals;

@Layer1SimpleAttachable
@Layer1StrategyName("VWAP from bars")
@Layer1ApiVersion(Layer1ApiVersionValue.VERSION2)
public class BarDemoWithHistoryData implements
    CustomModule, BarDataListener, HistoricalDataListener {

    private Indicator openIndicator;
    private Indicator highIndicator;
    private Indicator lowIndicator;
    private Indicator closeIndicator;
    private Indicator vwapBuyIndicator;
    private Indicator vwapSellIndicator;
    private Indicator vwapTotalIndicator;
    private Indicator volumeBuyIndicator;
    private Indicator volumeSellIndicator;

    @Override
    public void initialize(String alias, InstrumentInfo info, Api api, InitialState initialState) {
//        openIndicator = api.registerIndicator("Open", GraphType.PRIMARY, Color.MAGENTA);
//        highIndicator = api.registerIndicator("High", GraphType.PRIMARY, Color.MAGENTA);
//        lowIndicator = api.registerIndicator("Low", GraphType.PRIMARY, Color.MAGENTA);
        closeIndicator = api.registerIndicator("Close", GraphType.PRIMARY);
        closeIndicator.setColor(Color.MAGENTA);
//        vwapBuyIndicator = api.registerIndicator("VWAP buy", GraphType.PRIMARY, Color.MAGENTA);
//        vwapSellIndicator = api.registerIndicator("VWAP sell", GraphType.PRIMARY, Color.MAGENTA);
//        vwapTotalIndicator = api.registerIndicator("VWAP combined", GraphType.PRIMARY, Color.MAGENTA);
//        volumeBuyIndicator = api.registerIndicator("Volume buy", GraphType.BOTTOM, Color.GREEN);
//        volumeSellIndicator = api.registerIndicator("Volume sell", GraphType.BOTTOM, Color.GREEN);
    }
    
    @Override
    public void stop() {
    }

    @Override
    public void onBar(OrderBook orderBook, Bar bar) {
//        openIndicator.addPoint(bar.getOpen());
//        highIndicator.addPoint(bar.getHigh());
//        lowIndicator.addPoint(bar.getLow());
        closeIndicator.addPoint(bar.getClose());
//        vwapBuyIndicator.addPoint(bar.getVwapBuy());
//        vwapSellIndicator.addPoint(bar.getVwapSell());
//        vwapTotalIndicator.addPoint(bar.getVwapTotal());
//        volumeBuyIndicator.addPoint(bar.getVwapBuy());
//        volumeSellIndicator.addPoint(bar.getVwapSell());
    }

    @Override
    public long getInterval() {
        return Intervals.INTERVAL_15_SECONDS;
    }
}
