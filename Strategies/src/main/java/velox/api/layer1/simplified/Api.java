package velox.api.layer1.simplified;

import java.awt.Color;

import velox.api.layer1.messages.indicators.Layer1ApiUserMessageModifyIndicator.GraphType;

public interface Api {
    default Indicator registerIndicator(String name, GraphType graphType, Color defaultColor) {
        return registerIndicator(name, graphType, defaultColor, Double.NaN);
    }

    Indicator registerIndicator(String name, GraphType graphType, Color defaultColor, double initialValue);
}
