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
import velox.api.layer1.simplified.HistoricalDataListener;
import velox.api.layer1.simplified.Indicator;
import velox.api.layer1.simplified.InitialState;
import velox.api.layer1.simplified.TradeDataListener;
import velox.api.layer1.simplified.WidgetGroup;
import velox.api.layer1.simplified.WidgetRules;

@Layer1SimpleAttachable
@Layer1StrategyName("Widget Range Demo")
@Layer1ApiVersion(Layer1ApiVersionValue.VERSION1)
public class WidgetRangeDemo implements CustomModule, TradeDataListener, HistoricalDataListener
{
    private Indicator indicatorFirst;
    private Indicator indicatorSecond;
    
    private final long widgetRangeLifeSpan = 1_000 * 1_000_000_000l;// 1000sec so sample length is 10 sec

    @Override
    public void initialize(String alias, InstrumentInfo info, Api api, InitialState initialState) {
        
        WidgetRules widgetRules = new WidgetRules(3_000, 10_000, widgetRangeLifeSpan);
        
        indicatorFirst = api.registerIndicator("indicatorFirst", GraphType.PRIMARY, Double.NaN);
        indicatorFirst.setColor(Color.BLUE);
        indicatorFirst.setWidgetRules(widgetRules);
        
        indicatorSecond = api.registerIndicator("indicatorSecond", GraphType.PRIMARY, Double.NaN);
        indicatorSecond.setColor(Color.YELLOW);

        WidgetGroup widgetGroup = new WidgetGroup();
        widgetGroup.setWidgetRules(new WidgetRules(6_000, 9_500));
        widgetGroup.addIndicator(indicatorFirst);
        widgetGroup.addIndicator(indicatorSecond);
    }

    @Override
    public void onTrade(double price, int size, TradeInfo tradeInfo) {
        indicatorFirst.addPoint(price - 100);
        indicatorSecond.addPoint(price + 5_000);
    }

    @Override
    public void stop() {
    }

}
