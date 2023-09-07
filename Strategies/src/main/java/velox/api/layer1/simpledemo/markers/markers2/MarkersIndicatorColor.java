package velox.api.layer1.simpledemo.markers.markers2;

import velox.api.layer1.common.Log;
import velox.api.layer1.layers.strategies.interfaces.InvalidateInterface;
import velox.api.layer1.layers.strategies.interfaces.Layer1IndicatorColorInterface;
import velox.api.layer1.messages.indicators.IndicatorColorInterface;
import velox.api.layer1.messages.indicators.IndicatorColorScheme;
import velox.api.layer1.messages.indicators.SettingsAccess;
import velox.colors.ColorsChangedListener;
import velox.gui.StrategyPanel;
import velox.gui.colors.Colors;
import velox.gui.colors.ColorsConfigItem;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class MarkersIndicatorColor implements Layer1IndicatorColorInterface {
    private static final String INDICATOR_LINE_COLOR_NAME = "Trade markers line";
    private static final Color INDICATOR_LINE_DEFAULT_COLOR = Color.RED;

    private final Map<String, MarkersDemoSettings> settingsMap = new HashMap<>();

    private final Map<String, BufferedImage> orderIcons = Collections.synchronizedMap(new HashMap<>());

    private final Object locker = new Object();

    private final Layer1ApiMarkersDemo2 layer1ApiMarkersDemo2;

    private SettingsAccess settingsAccess;


    public MarkersIndicatorColor(Layer1ApiMarkersDemo2 layer1ApiMarkersDemo2) {
        this.layer1ApiMarkersDemo2 = layer1ApiMarkersDemo2;
    }

    @Override
    public void setColor(String alias, String name, Color color) {
        MarkersDemoSettings settings = getSettingsFor(alias);
        settings.setColor(name, color);
        settingsChanged(alias, settings);
    }

    @Override
    public Color getColor(String alias, String name) {
        Color color = getSettingsFor(alias).getColor(name);
        if (color == null) {
            switch (name) {
                case INDICATOR_LINE_COLOR_NAME:
                    color = INDICATOR_LINE_DEFAULT_COLOR;
                    break;
                default:
                    Log.warn("Layer1ApiMarkersDemo: unknown color name " + name);
                    color = Color.WHITE;
                    break;
            }
        }

        return color;
    }

    @Override
    public void addColorChangeListener(ColorsChangedListener listener) {
        // every one of our colors is modified only from one place
    }

    public StrategyPanel[] getCustomGuiFor(String alias) {
        StrategyPanel panel = new StrategyPanel("Colors", new GridBagLayout());
        panel.setLayout(new GridBagLayout());

        IndicatorColorInterface indicatorColorInterface = createNewIndicatorColorInterfaceInst(alias);
        ColorsConfigItem configItemLines = createNewColorsLinesConfigItem(indicatorColorInterface);

        GridBagConstraints gbConst = new GridBagConstraints();
        gbConst.gridx = 0;
        gbConst.gridy = 0;
        gbConst.weightx = 1;
        gbConst.insets = new Insets(5, 5, 5, 5);
        gbConst.fill = GridBagConstraints.HORIZONTAL;
        panel.add(configItemLines, gbConst);

        return new StrategyPanel[]{panel};
    }

    public IndicatorColorScheme createDefaultIndicatorColorScheme() {
        return new IndicatorColorScheme() {
            @Override
            public ColorDescription[] getColors() {
                return new ColorDescription[]{
                        new ColorDescription(Layer1ApiMarkersDemo2.class,
                                INDICATOR_LINE_COLOR_NAME,
                                INDICATOR_LINE_DEFAULT_COLOR,
                                false),
                };
            }

            @Override
            public String getColorFor(Double value) {
                return INDICATOR_LINE_COLOR_NAME;
            }

            @Override
            public ColorIntervalResponse getColorIntervalsList(double valueFrom, double valueTo) {
                return new ColorIntervalResponse(new String[]{INDICATOR_LINE_COLOR_NAME}, new double[]{});
            }
        };
    }

    public void setSettingsAccess(SettingsAccess settingsAccess) {
        this.settingsAccess = settingsAccess;
    }

    public BufferedImage getOrderIconByAlias(String alias) {
        return orderIcons.get(alias);
    }

    protected void settingsChanged(String settingsAlias, MarkersDemoSettings settingsObject) {
        synchronized (locker) {
            settingsAccess.setSettings(settingsAlias,
                    Layer1ApiMarkersDemo2.INDICATOR_NAME_TRADE, settingsObject, settingsObject.getClass());
        }
    }

    private MarkersDemoSettings getSettingsFor(String alias) {
        synchronized (locker) {
            MarkersDemoSettings settings = settingsMap.get(alias);
            if (settings == null) {
                settings = (MarkersDemoSettings) settingsAccess.getSettings(alias,
                        Layer1ApiMarkersDemo2.INDICATOR_NAME_TRADE, MarkersDemoSettings.class);
                settingsMap.put(alias, settings);
            }
            return settings;
        }
    }

    private IndicatorColorInterface createNewIndicatorColorInterfaceInst(String alias) {
        return new IndicatorColorInterface() {
            @Override
            public void set(String name, Color color) {
                setColor(alias, name, color);
            }

            @Override
            public Color getOrDefault(String name, Color defaultValue) {
                Color color = getSettingsFor(alias).getColor(name);
                return color == null ? defaultValue : color;
            }

            @Override
            public void addColorChangeListener(ColorsChangedListener listener) {
            }
        };
    }

    private ColorsConfigItem createNewColorsLinesConfigItem(IndicatorColorInterface indicatorColorInterface) {
        ColorsChangedListener colorsChangedListener = () -> {
            InvalidateInterface invalidaInterface =
                    layer1ApiMarkersDemo2.getInvalidateInterface(Layer1ApiMarkersDemo2.INDICATOR_NAME_TRADE);
            if (invalidaInterface != null) {
                invalidaInterface.invalidate();
            }
        };

        return new ColorsConfigItem(INDICATOR_LINE_COLOR_NAME, INDICATOR_LINE_COLOR_NAME, true,
                INDICATOR_LINE_DEFAULT_COLOR, indicatorColorInterface, colorsChangedListener);
    }
}
