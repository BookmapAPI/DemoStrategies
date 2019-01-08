package velox.api.layer1.simplified.demo;

import java.awt.Color;

import velox.api.layer1.annotations.Layer1ApiVersion;
import velox.api.layer1.annotations.Layer1ApiVersionValue;
import velox.api.layer1.annotations.Layer1SimpleAttachable;
import velox.api.layer1.annotations.Layer1StrategyName;
import velox.api.layer1.data.InstrumentInfo;
import velox.api.layer1.data.TradeInfo;
import velox.api.layer1.messages.indicators.Layer1ApiUserMessageModifyIndicator.GraphType;
import velox.api.layer1.messages.indicators.WidgetDisplayInfo;
import velox.api.layer1.messages.indicators.WidgetDisplayInfo.Type;
import velox.api.layer1.simplified.Api;
import velox.api.layer1.simplified.CustomModule;
import velox.api.layer1.simplified.HistoricalDataListener;
import velox.api.layer1.simplified.Indicator;
import velox.api.layer1.simplified.InitialState;
import velox.api.layer1.simplified.Parameter;
import velox.api.layer1.simplified.TradeDataListener;
import velox.api.layer1.simplified.WidgetGroup;
import velox.api.layer1.simplified.WidgetRules;

@Layer1SimpleAttachable
@Layer1StrategyName("Last Trade Setting Widget Range")
@Layer1ApiVersion(Layer1ApiVersionValue.VERSION1)
public class LastTradeDemoSettingWidgetRange implements CustomModule, TradeDataListener, HistoricalDataListener
{
	private Indicator lastTradeIndicator;
	private Indicator lastTradeIndicatorCopy;
	boolean isUpward = false;

	@Parameter(name = "First Color")
	Color firstColor = Color.BLUE;
	
	Color secondColor = Color.YELLOW;

	double upper = Double.NaN;
	double lower = Double.NaN;
	final long widgetRangeLifeSpan = 1_000 * 1_000_000_000l;// 1000sec so sample length is 10 sec
	WidgetRules widgetRules = new WidgetRules(lower, upper, new WidgetDisplayInfo(Type.DEFAULT, 0), widgetRangeLifeSpan);
	WidgetGroup widgetGroup = new WidgetGroup();

	@Override
	public void initialize(String alias, InstrumentInfo info, Api api, InitialState initialState) {
		WidgetRules groupWidgetRules = new WidgetRules();
		groupWidgetRules.setMargin(2);
		widgetGroup.setWidgetRules(groupWidgetRules);
		
		lastTradeIndicator = api.registerIndicator("Last trade, no history", GraphType.PRIMARY, Double.NaN);
		lastTradeIndicator.setColor(firstColor);
		lastTradeIndicator.setWidgetRules(widgetRules);
		widgetGroup.addIndicator(lastTradeIndicator);
		
		lastTradeIndicatorCopy = api.registerIndicator("Copy", GraphType.PRIMARY, Double.NaN);
		lastTradeIndicatorCopy.setColor(secondColor);
		lastTradeIndicatorCopy.setWidgetRules(widgetRules);
		widgetGroup.addIndicator(lastTradeIndicatorCopy);
	}

	@Override
	public void onTrade(double price, int size, TradeInfo tradeInfo) {
		lastTradeIndicator.addPoint(price + 2);
		lastTradeIndicatorCopy.addPoint(price* 1.5);
	}

	@Override
	public void stop() {
	}


	

}
