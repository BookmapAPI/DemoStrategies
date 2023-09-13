package velox.api.layer1.simpledemo.truestrengthindex;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.awt.*;
import java.util.Arrays;

public enum TsiConstants {
    MAIN_INDEX("Strength index line", Color.RED),
    CIRCLE_INDEX("Strength index circle", Color.GREEN);
    static public final String INDICATOR_NAME = "True Strength index";
    static public final String SHORT_NAME = "TSI";
    static public final Pair<Integer, Integer> TSI_PARAMS = new ImmutablePair<>(13, 25);
    private final String lineName;
    private final Color defaultColor;

    TsiConstants(String lineName, Color color) {
        this.lineName = lineName;
        this.defaultColor = color;
    }

    public static TsiConstants fromIndicatorName(String name) {
        return Arrays.stream(TsiConstants.values())
                .filter(indexConstant -> INDICATOR_NAME.equals(name))
                .findFirst()
                .orElse(null);
    }

    public static TsiConstants fromLineName(String name) {
        return Arrays.stream(TsiConstants.values())
                .filter(constant -> constant.lineName.equals(name))
                .findFirst()
                .orElse(null);
    }

    public String getLineName() {
        return lineName;
    }

    public Color getDefaultColor() {
        return defaultColor;
    }
}