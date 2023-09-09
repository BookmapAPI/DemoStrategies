package velox.api.layer1.simpledemo.truestrengthindex;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.awt.*;
import java.util.Arrays;

public enum TrueStrengthIndexDemoConstants {
    MAIN_INDEX("True Strength index",
            "Strength index line",
            Color.RED,
            new ImmutablePair<>(13,  25));
    private final String indicatorName;
    private final Color defaultColor;
    private final String lineName;
    private final Pair<Integer, Integer> params;

    TrueStrengthIndexDemoConstants(String indexName,
                                   String lineName,
                                   Color color,
                                   Pair<Integer, Integer> params) {
        this.indicatorName = indexName;
        this.lineName = lineName;
        this.defaultColor = color;
        this.params = params;
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

    public Pair<Integer, Integer> getParams() {
        return params;
    }

}