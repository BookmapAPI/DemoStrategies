package velox.api.layer1.simpledemo.markers;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

import velox.api.layer1.settings.StrategySettingsVersion;

@StrategySettingsVersion(currentVersion = 1, compatibleVersions = {})
public class MarkersDemoSettings {
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
