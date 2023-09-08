package velox.api.layer1.simpledemo.markers.markers2;

import java.awt.*;
import java.util.Arrays;

public enum MarkersDemoConstants {
    MAIN_INDEX("Trade markers", "Trade markers line", Color.RED);
    private final String indicatorName;
    private final Color defaultColor;
    private final String lineName;

    MarkersDemoConstants(String indexName, String lineName, Color color) {
        this.indicatorName = indexName;
        this.lineName = lineName;
        this.defaultColor = color;
    }

    public static MarkersDemoConstants fromIndicatorName(String name) {
        return Arrays.stream(MarkersDemoConstants.values())
                .filter(indexColor -> indexColor.indicatorName.equals(name))
                .findFirst()
                .orElse(null);
    }

    public static MarkersDemoConstants fromLineName(String name) {
        return Arrays.stream(MarkersDemoConstants.values())
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