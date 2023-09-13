package velox.api.layer1.simpledemo.truestrengthindex;

import velox.api.layer1.common.Log;
import velox.api.layer1.layers.strategies.interfaces.InvalidateInterface;
import velox.api.layer1.layers.strategies.interfaces.Layer1IndicatorColorInterface;
import velox.api.layer1.messages.indicators.IndicatorColorInterface;
import velox.api.layer1.messages.indicators.IndicatorColorScheme;
import velox.colors.ColorsChangedListener;
import velox.gui.StrategyPanel;
import velox.gui.colors.ColorsConfigItem;

import java.awt.*;

public class TrueStrengthIndexGraphics implements Layer1IndicatorColorInterface {

    final TrueStrengthIndexRepo trueStrengthIndexRepo;


    public TrueStrengthIndexGraphics(TrueStrengthIndexRepo trueStrengthIndexRepo) {
        this.trueStrengthIndexRepo = trueStrengthIndexRepo;
    }

    @Override
    public void setColor(String alias, String name, Color color) {
        TrueStrengthIndexSettings settings = trueStrengthIndexRepo.getSettingsFor(alias);
        settings.setColor(name, color);
        trueStrengthIndexRepo.settingsChanged(alias, settings);
    }

    @Override
    public Color getColor(String alias, String name) {
        Color color = trueStrengthIndexRepo.getSettingsFor(alias).getColor(name);
        if (color == null) {
            TsiConstants line = TsiConstants.fromLineName(name);
            if (line != null) {
                color = line.getDefaultColor();
            } else {
                Log.warn("Layer1ApiTrueStrengthIndex: unknown color name " + name);
                color = Color.WHITE;
            }
        }

        return color;
    }

    @Override
    public void addColorChangeListener(ColorsChangedListener listener) {
    }

    protected StrategyPanel[] getCustomGuiFor(String alias) {
        StrategyPanel panel = new StrategyPanel("Colors", new GridBagLayout());

        IndicatorColorInterface indicatorColorInterface = createNewIndicatorColorInterfaceInst(alias);
        ColorsConfigItem configItemMain = createNewColorsConfigItem(indicatorColorInterface, TsiConstants.MAIN_INDEX);
        ColorsConfigItem configItemBack = createNewColorsConfigItem(indicatorColorInterface, TsiConstants.CIRCLE_INDEX);

        panel.add(configItemMain, createDefaultGridBagConstraints(0));
        panel.add(configItemBack, createDefaultGridBagConstraints(1));

        panel.requestReload();

        return new StrategyPanel[]{panel};
    }

    protected IndicatorColorScheme createDefaultIndicatorColorScheme() {
        return new IndicatorColorScheme() {
            @Override
            public ColorDescription[] getColors() {
                return new ColorDescription[]{
                        new ColorDescription(Layer1ApiTrueStrengthIndex.class,
                                TsiConstants.MAIN_INDEX.getLineName(),
                                TsiConstants.MAIN_INDEX.getDefaultColor(),
                                false),
                        new ColorDescription(Layer1ApiTrueStrengthIndex.class,
                                TsiConstants.CIRCLE_INDEX.getLineName(),
                                TsiConstants.CIRCLE_INDEX.getDefaultColor(),
                                false)
                };
            }

            @Override
            public String getColorFor(Double value) {
                return TsiConstants.CIRCLE_INDEX.getLineName();
            }

            @Override
            public ColorIntervalResponse getColorIntervalsList(double valueFrom, double valueTo) {
                return new ColorIntervalResponse(new String[]{TsiConstants.MAIN_INDEX.getLineName()},
                        new double[]{});
            }
        };
    }

    private IndicatorColorInterface createNewIndicatorColorInterfaceInst(String alias) {
        return new IndicatorColorInterface() {
            @Override
            public void set(String name, Color color) {
                setColor(alias, name, color);
            }

            @Override
            public Color getOrDefault(String name, Color defaultValue) {
                Color color = trueStrengthIndexRepo.getSettingsFor(alias).getColor(name);
                return color == null ? defaultValue : color;
            }

            @Override
            public void addColorChangeListener(ColorsChangedListener listener) {
            }
        };
    }

    private ColorsConfigItem createNewColorsConfigItem(IndicatorColorInterface indicatorColorInterface, TsiConstants c) {
        ColorsChangedListener colorsChangedListener = () -> {
            InvalidateInterface invalidateInterface =
                    trueStrengthIndexRepo.getInvalidateInterface(TsiConstants.INDICATOR_NAME);

            if (invalidateInterface != null) {
                invalidateInterface.invalidate();
            }
        };

        return new ColorsConfigItem(c.getLineName(),
                c.getLineName(),
                true,
                c.getDefaultColor(),
                indicatorColorInterface,
                colorsChangedListener);
    }

    private GridBagConstraints createDefaultGridBagConstraints(int number) {
        GridBagConstraints gbConst;
        gbConst = new GridBagConstraints();
        gbConst.gridx = 0;
        gbConst.gridy = number;
        gbConst.weightx = 1;
        gbConst.insets = new Insets(5, 5, 5, 5);
        gbConst.fill = GridBagConstraints.HORIZONTAL;
        return gbConst;
    }
}
