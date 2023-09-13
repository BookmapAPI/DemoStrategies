package velox.api.layer1.simpledemo.truestrengthindex;

import velox.api.layer1.settings.StrategySettingsVersion;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

@StrategySettingsVersion(currentVersion = 1, compatibleVersions = {})
public class TrueStrengthIndexSettings {
    private Map<String, Color> colors;

    public Color getColor(String name) {
        if (colors == null) {
            colors = new HashMap<>();
        }
        return colors.get(name);
    }

    public void setColor(String name, Color color) {
        if (colors == null) {
            colors = new HashMap<>();
        }
        colors.put(name, color);
    }
}
