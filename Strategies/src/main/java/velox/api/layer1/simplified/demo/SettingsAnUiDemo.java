package velox.api.layer1.simplified.demo;

import java.awt.BorderLayout;
import java.awt.Color;

import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import velox.api.layer1.annotations.Layer1ApiVersion;
import velox.api.layer1.annotations.Layer1ApiVersionValue;
import velox.api.layer1.annotations.Layer1SimpleAttachable;
import velox.api.layer1.annotations.Layer1StrategyName;
import velox.api.layer1.data.InstrumentInfo;
import velox.api.layer1.data.TradeInfo;
import velox.api.layer1.messages.indicators.Layer1ApiUserMessageModifyIndicator.GraphType;
import velox.api.layer1.settings.StrategySettingsVersion;
import velox.api.layer1.simplified.Api;
import velox.api.layer1.simplified.CustomModule;
import velox.api.layer1.simplified.CustomSettingsPanelProvider;
import velox.api.layer1.simplified.Indicator;
import velox.api.layer1.simplified.TradeDataListener;
import velox.gui.StrategyPanel;

@Layer1SimpleAttachable
@Layer1StrategyName("Settings/UI demo")
@Layer1ApiVersion(Layer1ApiVersionValue.VERSION1)
public class SettingsAnUiDemo implements
    CustomModule,
    TradeDataListener,
    CustomSettingsPanelProvider
{
    @StrategySettingsVersion(currentVersion = 1, compatibleVersions = {})
    public static class Settings {
        int lastTradeOffset = 0;
    }
    
    private Api api;
    
    private Indicator lastTradeIndicator;
    private Settings settings;
    
    
    
    @Override
    public void initialize(String alias, InstrumentInfo info, Api api) {
        this.api = api;
        
        lastTradeIndicator = api.registerIndicator("Last trade, no history",
                GraphType.PRIMARY, Color.GREEN);
        settings = api.getSettings(Settings.class);
    }
    
    @Override
    public void stop() {
    }

    @Override
    public void onTrade(double price, int size, TradeInfo tradeInfo) {
        lastTradeIndicator.addPoint(price + settings.lastTradeOffset);
    }

    @Override
    public StrategyPanel[] getCustomSettingsPanels() {
        StrategyPanel p1 = new StrategyPanel("Last trade offset");
        
        JSpinner spinner = new JSpinner(new SpinnerNumberModel(
                settings.lastTradeOffset, -100, 100, 1));
        spinner.addChangeListener(e -> {
            settings.lastTradeOffset = (Integer)spinner.getValue();
            api.setSettings(settings);
        });
        
        p1.setLayout(new BorderLayout());
        p1.add(spinner, BorderLayout.CENTER);
        
        return new StrategyPanel[] {p1};
    }
}
