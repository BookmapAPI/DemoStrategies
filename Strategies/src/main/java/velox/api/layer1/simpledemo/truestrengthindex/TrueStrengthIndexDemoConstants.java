package velox.api.layer1.simpledemo.truestrengthindex;

import java.awt.*;
import java.util.Arrays;

public enum TrueStrengthIndexDemoConstants {
    MAIN_INDEX("True Strength index", "Strength index line", Color.RED);
    private final String indicatorName;
    private final Color defaultColor;
    private final String lineName;

    TrueStrengthIndexDemoConstants(String indexName, String lineName, Color color) {
        this.indicatorName = indexName;
        this.lineName = lineName;
        this.defaultColor = color;
    }

    public static TrueStrengthIndexDemoConstants fromIndicatorName(String name) {
        return Arrays.stream(TrueStrengthIndexDemoConstants.values())
                .filter(indexColor -> indexColor.indicatorName.equals(name))
                .findFirst()
                .orElse(null);
    }

    public static TrueStrengthIndexDemoConstants fromLineName(String name) {
        return Arrays.stream(TrueStrengthIndexDemoConstants.values())
                .filter(indexColor -> indexColor.lineName.equals(name))
                .findFirst()
                .orElse(null);
    }


    public String getLineName() {
        return lineName;
    }

    public Color getDefaultColor() {
        return defaultColor;
    }

    public String getIndicatorName() {
        return indicatorName;
    }
}