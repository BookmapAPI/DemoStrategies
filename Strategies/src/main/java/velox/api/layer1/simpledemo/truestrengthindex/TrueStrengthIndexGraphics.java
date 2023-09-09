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
            TrueStrengthIndexDemoConstants line = TrueStrengthIndexDemoConstants.fromLineName(name);
            if (line == TrueStrengthIndexDemoConstants.MAIN_INDEX) {
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
        // every one of our colors is modified only from one place
    }

    protected StrategyPanel[] getCustomGuiFor(String alias) {
        StrategyPanel panel = new StrategyPanel("Colors", new GridBagLayout());

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
                        new ColorDescription(Layer1ApiTrueStrengthIndex.class,
                                TrueStrengthIndexDemoConstants.MAIN_INDEX.getLineName(),
                                TrueStrengthIndexDemoConstants.MAIN_INDEX.getDefaultColor(),
                                false),
                };
            }

            @Override
            public String getColorFor(Double value) {
                return TrueStrengthIndexDemoConstants.MAIN_INDEX.getLineName();
            }

            @Override
            public ColorIntervalResponse getColorIntervalsList(double valueFrom, double valueTo) {
                return new ColorIntervalResponse(new String[]{TrueStrengthIndexDemoConstants.MAIN_INDEX.getLineName()},
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

    private ColorsConfigItem createNewColorsLinesConfigItem(IndicatorColorInterface indicatorColorInterface) {
        ColorsChangedListener colorsChangedListener = () -> {
            InvalidateInterface invalidaInterface =
                    trueStrengthIndexRepo.getInvalidateInterface(TrueStrengthIndexDemoConstants.MAIN_INDEX.getIndicatorName());
            if (invalidaInterface != null) {
                invalidaInterface.invalidate();
            }
        };

        return new ColorsConfigItem(TrueStrengthIndexDemoConstants.MAIN_INDEX.getLineName(),
                TrueStrengthIndexDemoConstants.MAIN_INDEX.getLineName(),
                true,
                TrueStrengthIndexDemoConstants.MAIN_INDEX.getDefaultColor(),
                indicatorColorInterface,
                colorsChangedListener);
    }
}
