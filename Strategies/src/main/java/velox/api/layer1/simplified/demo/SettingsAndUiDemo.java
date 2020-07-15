package velox.api.layer1.simplified.demo;

import java.awt.BorderLayout;
import java.awt.Color;

import javax.swing.JButton;
import javax.swing.JComboBox;
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
import velox.api.layer1.simplified.LineStyle;
import velox.api.layer1.simplified.TradeDataListener;
import velox.gui.StrategyPanel;
import velox.gui.colors.ColorsConfigItem;
import velox.gui.utils.GuiUtils;

@Layer1SimpleAttachable
@Layer1StrategyName("Settings/UI demo")
@Layer1ApiVersion(Layer1ApiVersionValue.VERSION2)
public class SettingsAndUiDemo implements
    CustomModule,
    TradeDataListener,
    CustomSettingsPanelProvider
{
    @StrategySettingsVersion(currentVersion = 1, compatibleVersions = {})
    public static class Settings {
        int lastTradeOffset = 0;
        Color lastTradeColor = DEFAULT_LAST_TRADE_COLOR;
        int lineWidth = 3;
        LineStyle lineStyle = LineStyle.SOLID;
    }
    
    private static final Color DEFAULT_LAST_TRADE_COLOR = Color.GREEN;
    
    private Api api;
    
    private Indicator lastTradeIndicator;
    private Settings settings;
    
    
    
    @Override
    public void initialize(String alias, InstrumentInfo info, Api api, InitialState initialState) {
        this.api = api;
        
        lastTradeIndicator = api.registerIndicator("Last trade, no history",
                GraphType.PRIMARY);
        
        settings = api.getSettings(Settings.class);
        lastTradeIndicator.setColor(settings.lastTradeColor);
        lastTradeIndicator.setLineStyle(settings.lineStyle);
        lastTradeIndicator.setWidth(settings.lineWidth);

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
        return getCustomSettingsPanels(settings, api, lastTradeIndicator);
    }
    
    /**
     * Generate custom settings UI. Since we want to generate both disabled and
     * enabled UI we move the code to separate method. If we would not care about
     * having disabled UI (to be shown while module is not loaded) we could just
     * implement {@link #getCustomSettingsPanels()}
     *
     * @param settings           settings to show in UI and change
     * @param api                API object to interact with bookmap. If it's null -
     *                           UI is generated as disabled (because module is not
     *                           loaded)
     * @param lastTradeIndicator indicator to manage. It will be null if UI is
     *                           disabled
     * @return generated UI
     */
    private static StrategyPanel[] getCustomSettingsPanels(Settings settings, Api api, Indicator lastTradeIndicator) {
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
        
        StrategyPanel p2 = new StrategyPanel("Line color");
        
        // Label parameter can be omitted if you want to use separate component for that
        ColorsConfigItem colorConfig = new ColorsConfigItem(
                settings.lastTradeColor, DEFAULT_LAST_TRADE_COLOR, "Last trade", color -> {
            settings.lastTradeColor = color;
            
            // ColorsConfigItem currently can trigger callback during construction
            if (api != null) {
                api.setSettings(settings);
                
                // Note, that there is no need to reload indicator to apply the color - it's applied immediately
                lastTradeIndicator.setColor(color);
            }
        });
        
        p2.setLayout(new BorderLayout());
        p2.add(colorConfig, BorderLayout.CENTER);
        
        StrategyPanel p3 = new StrategyPanel("Line width");
        // This spinner will affect new values, but not computed ones
        JSpinner lineWidthSpinner = new JSpinner(new SpinnerNumberModel(
                settings.lineWidth, 1, 10, 1));
        lineWidthSpinner.addChangeListener(e -> {
            settings.lineWidth = (Integer)lineWidthSpinner.getValue();
            api.setSettings(settings);
            lastTradeIndicator.setWidth(settings.lineWidth);
        });
        
        p3.setLayout(new BorderLayout());
        p3.add(lineWidthSpinner, BorderLayout.CENTER);
        
        
        StrategyPanel p4 = new StrategyPanel("Line style");
        // This spinner will affect new values, but not computed ones
        JComboBox<LineStyle> lineStyleComboBox = new JComboBox<>(LineStyle.values());
        lineStyleComboBox.setSelectedItem(settings.lineStyle);
        lineStyleComboBox.addActionListener(e -> {
            settings.lineStyle = (LineStyle) lineStyleComboBox.getSelectedItem();
            api.setSettings(settings);
            lastTradeIndicator.setLineStyle(settings.lineStyle);
        });
        
        p4.setLayout(new BorderLayout());
        p4.add(lineStyleComboBox, BorderLayout.CENTER);
        
        StrategyPanel[] strategyPanels = new StrategyPanel[] {p1, p2, p3, p4};
        
        // If module is not loaded - disable all components
        if (api == null) {
            for (StrategyPanel strategyPanel : strategyPanels) {
                GuiUtils.setPanelEnabled(strategyPanel, false);
            }
        }

        return strategyPanels;
    }
    
    /**
     * Optional method generating UI for unloaded module. See javadoc for
     * {@link CustomSettingsPanelProvider}
     */
    public static StrategyPanel[] getCustomDisabledSettingsPanels() {
        // Generating disabled UI
        return getCustomSettingsPanels(new Settings(), null ,null);
        
    }
}
