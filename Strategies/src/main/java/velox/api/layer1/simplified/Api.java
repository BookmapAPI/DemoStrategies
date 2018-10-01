package velox.api.layer1.simplified;

import velox.api.layer1.messages.indicators.Layer1ApiUserMessageModifyIndicator.GraphType;

public interface Api {
    default Indicator registerIndicator(String name, GraphType graphType) {
        return registerIndicator(name, graphType, Double.NaN);
    }

    Indicator registerIndicator(String name, GraphType graphType, double initialValue);
}
