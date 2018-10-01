package velox.api.layer1.config.beans;

import velox.api.layer1.settings.StrategySettingsVersion;

@StrategySettingsVersion(currentVersion = 1, compatibleVersions = {})
public class HelperChaseStrategySettings extends HelperStrategySettings {
    public static enum ChaseMode {
        LAST_PRICE,
        BEST_PRICE
    };
    
    private static final int DISTANCE_DEFAULT = 3;
    
    private static final ChaseMode MODE_DEFAULT = ChaseMode.BEST_PRICE;
    
    private int distance = DISTANCE_DEFAULT;
    
    private ChaseMode mode = MODE_DEFAULT;

    public HelperChaseStrategySettings() {
        super();
    }
    
    public HelperChaseStrategySettings(HelperChaseStrategySettings settings) {
        super(settings);
        this.distance = settings.distance;
        this.mode = settings.mode;
    }
    
    public int getDistance() {
        return distance;
    }

    public void setDistance(int distance) {
        this.distance = distance;
    }

    public ChaseMode getMode() {
        return mode;
    }

    public void setMode(ChaseMode mode) {
        this.mode = mode;
    }
}
