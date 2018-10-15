package velox.api.layer1.simplified;

import java.awt.Color;

import velox.api.layer1.data.OrderSendParameters;
import velox.api.layer1.data.OrderUpdateParameters;
import velox.api.layer1.settings.StrategySettingsVersion;
import velox.api.layer1.messages.indicators.Layer1ApiUserMessageModifyIndicator.GraphType;

/**
 * Allows communicating back to Bookmap.
 */
public interface Api {
    /**
     * Similar to {@link #registerIndicator(String, GraphType, Color, double)},
     * assumes initialValue = NaN (no line until first update)
     *
     * @param name
     *            user-friendly name for an indicator. <b>Must be unique within
     *            alias.</b>
     * @param graphType
     *            where to draw the indicator (bottom panel or main chart)
     * @param color
     *            color to use for indicator
     * @return indicator object that can be used to manipulate the line
     */
    default Indicator registerIndicator(String name, GraphType graphType, Color color) {
        return registerIndicator(name, graphType, color, Double.NaN);
    }

    /**
     * Register an indicator (line).
     *
     * @param name
     *            user-friendly name for an indicator. <b>Must be unique within
     *            alias.</b>
     * @param graphType
     *            where to draw the indicator (bottom panel or main chart)
     * @param color
     *            color to use for indicator
     * @param initialValue
     *            initial value of the indicator. NaN means "no visible line".
     * @return indicator object that can be used to manipulate the line
     */
    Indicator registerIndicator(String name, GraphType graphType, Color color, double initialValue);
    

    /**
     * Submit order with specified parameters
     *
     * @param orderSendParameters
     *            parameters
     */
    void sendOrder(OrderSendParameters orderSendParameters);

    /**
     * Update order according to parameters
     *
     * @param orderUpdateParameters
     *            parameters
     */
    void updateOrder(OrderUpdateParameters orderUpdateParameters);
    
    /**
     * Store settings.
     *
     * @param settingsObject
     *            your settings object. Class must be annotated with
     *            {@link StrategySettingsVersion}
     */
    public <T> void setSettings(T settingsObject);
    
    /**
     * Retrieve earlier stored settings
     *
     * @param settingsClass
     *            class of your settings object. Must be annotated with
     *            {@link StrategySettingsVersion}
     * @return settings object. If there is no compatible saved object for this
     *         request, new object will be created with default constructor<br>
     *         <b>Changing returned object will not change settings. You need to
     *         call {@link #setSettings(Object)} for this</b>
     */
    public <T> T getSettings(Class<? extends T> settingsClass);
    
    /**
     * Request reloading the module. This will lead to your module being unloaded
     * for current alias, then loaded back. Typical use case would be to apply
     * settings changes.
     */
    public void reload();
}
