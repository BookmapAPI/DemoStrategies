package velox.api.layer1.simplified.demo;

import java.awt.BorderLayout;
import java.awt.Color;

import javax.swing.JButton;
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
import velox.api.layer1.simplified.InitialState;
import velox.api.layer1.simplified.TradeDataListener;
import velox.gui.StrategyPanel;

@Layer1SimpleAttachable
@Layer1StrategyName("Settings/UI demo")
@Layer1ApiVersion(Layer1ApiVersionValue.VERSION1)
public class SettingsAndUiDemo implements
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
    public void initialize(String alias, InstrumentInfo info, Api api, InitialState initialState) {
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
        
        // This spinner will affect new values, but not computed ones
        JSpinner offsetSpinner = new JSpinner(new SpinnerNumberModel(
                settings.lastTradeOffset, -100, 100, 1));
        offsetSpinner.addChangeListener(e -> {
            settings.lastTradeOffset = (Integer)offsetSpinner.getValue();
            api.setSettings(settings);
        });
        
        
        // Note, that this will also trigger reloading of this panel, meaning focus will
        // be lost
        JButton reloadButton = new JButton("Apply and reload");
        reloadButton.addActionListener(e -> api.reload());
        
        p1.setLayout(new BorderLayout());
        p1.add(offsetSpinner, BorderLayout.CENTER);
        p1.add(reloadButton, BorderLayout.EAST);
        
        return new StrategyPanel[] {p1};
    }
}
