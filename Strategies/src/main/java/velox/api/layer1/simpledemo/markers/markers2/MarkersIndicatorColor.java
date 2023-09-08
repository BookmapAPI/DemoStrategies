package velox.api.layer1.simpledemo.markers.markers2;

import velox.api.layer1.common.Log;
import velox.api.layer1.layers.strategies.interfaces.InvalidateInterface;
import velox.api.layer1.layers.strategies.interfaces.Layer1IndicatorColorInterface;
import velox.api.layer1.layers.strategies.interfaces.OnlineCalculatable;
import velox.api.layer1.messages.indicators.IndicatorColorInterface;
import velox.api.layer1.messages.indicators.IndicatorColorScheme;
import velox.api.layer1.messages.indicators.SettingsAccess;
import velox.colors.ColorsChangedListener;
import velox.gui.StrategyPanel;
import velox.gui.colors.ColorsConfigItem;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

public class MarkersIndicatorColor implements Layer1IndicatorColorInterface {

    private final BufferedImage tradeIcon = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);

    private final Map<String, MarkersDemoSettings> settingsMap = new HashMap<>();

    private final Object locker = new Object();

    private final MarkersRepo markersRepo;

    private SettingsAccess settingsAccess;


    public MarkersIndicatorColor(MarkersRepo markersRepo) {
        this.markersRepo = markersRepo;

        // Prepare trade marker
        Graphics graphics = tradeIcon.getGraphics();
        graphics.setColor(Color.BLUE);
        graphics.drawLine(0, 0, 15, 15);
        graphics.drawLine(15, 0, 0, 15);
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
            MarkersDemoConstants line = MarkersDemoConstants.fromLineName(name);
            if (line == MarkersDemoConstants.MAIN_INDEX) {
                color = line.getDefaultColor();
            } else {
                Log.warn("Layer1ApiMarkersDemo: unknown color name " + name);
                color = Color.WHITE;
            }
        }

        return color;
    }

    @Override
    public void addColorChangeListener(ColorsChangedListener listener) {
        // every one of our colors is modified only from one place
    }

    protected StrategyPanel[] getCustomGuiFor(String alias) {
        StrategyPanel panel = new StrategyPanel("Colors", new GridBagLayout());
//        panel.setLayout(new GridBagLayout());

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

    protected IndicatorColorScheme createDefaultIndicatorColorScheme() {
        return new IndicatorColorScheme() {
            @Override
            public ColorDescription[] getColors() {
                return new ColorDescription[]{
                        new ColorDescription(Layer1ApiMarkersDemo2.class,
                                MarkersDemoConstants.MAIN_INDEX.getLineName(),
                                MarkersDemoConstants.MAIN_INDEX.getDefaultColor(),
                                false),
                };
            }

            @Override
            public String getColorFor(Double value) {
                return MarkersDemoConstants.MAIN_INDEX.getLineName();
            }

            @Override
            public ColorIntervalResponse getColorIntervalsList(double valueFrom, double valueTo) {
                return new ColorIntervalResponse(new String[]{MarkersDemoConstants.MAIN_INDEX.getLineName()},
                        new double[]{});
            }
        };
    }

    protected OnlineCalculatable.Marker createNewTradeMarker(double price) {
        return new OnlineCalculatable.Marker(price,
                -tradeIcon.getHeight() / 2,
                -tradeIcon.getWidth() / 2,
                tradeIcon);
    }

    protected void setSettingsAccess(SettingsAccess settingsAccess) {
        this.settingsAccess = settingsAccess;
    }

    protected void settingsChanged(String settingsAlias, MarkersDemoSettings settingsObject) {
        synchronized (locker) {
            settingsAccess.setSettings(settingsAlias,
                    MarkersDemoConstants.MAIN_INDEX.getIndicatorName(), settingsObject, MarkersDemoSettings.class);
        }
    }

    private MarkersDemoSettings getSettingsFor(String alias) {
        synchronized (locker) {
            MarkersDemoSettings settings = settingsMap.get(alias);
            if (settings == null) {
                settings = (MarkersDemoSettings) settingsAccess.getSettings(alias,
                        MarkersDemoConstants.MAIN_INDEX.getIndicatorName(), MarkersDemoSettings.class);
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
                    markersRepo.getInvalidateInterface(MarkersDemoConstants.MAIN_INDEX.getIndicatorName());
            if (invalidaInterface != null) {
                invalidaInterface.invalidate();
            }
        };

        return new ColorsConfigItem(MarkersDemoConstants.MAIN_INDEX.getLineName(),
                MarkersDemoConstants.MAIN_INDEX.getLineName(),
                true,
                MarkersDemoConstants.MAIN_INDEX.getDefaultColor(),
                indicatorColorInterface,
                colorsChangedListener);
    }
}
