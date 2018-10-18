package velox.api.layer1.simplified;

import velox.api.layer1.messages.indicators.IndicatorLineStyle;

public enum LineStyle {
    SOLID("Solid", IndicatorLineStyle.DEFAULT),
    SHORT_DASH("Short dash", new IndicatorLineStyle(
            (short) 0xFF00, (short) 1, 0, (short) 0xF0F0, (short) 1, 0)),
    LONG_DASH("Long dash", new IndicatorLineStyle(
            (short) 0xFF00, (short) 2, 0, (short) 0xFF00, (short) 1, 0)),
    DOT("Dot", new IndicatorLineStyle(
            (short) 0x5555, (short) 2, 0, (short) 0x5555, (short) 1, 0)),
    DASH_DOT("Dot-dash", new IndicatorLineStyle(
            (short) 0xFF55, (short) 2, 0, (short) 0xFF55, (short) 1, 0));
    
    private final String stringRepresentation;
    private final IndicatorLineStyle baseStyle;
    
    private LineStyle(String stringRepresentation, IndicatorLineStyle baseStyle) {
        this.stringRepresentation = stringRepresentation;
        this.baseStyle = baseStyle;
    }
    
    public IndicatorLineStyle toIndicatorStyle(int width) {
        return new IndicatorLineStyle(baseStyle.mainLineStyleMask,
                baseStyle.mainLineStyleMultiplier,
                width,
                baseStyle.rightLineStyleMask,
                baseStyle.rightLineStyleMultiplier,
                width);
    }

    @Override
    public String toString() {
        return stringRepresentation;
    }
}
