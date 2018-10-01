package velox.api.layer1.config.beans;

import velox.api.layer1.settings.StrategySettingsVersion;

@StrategySettingsVersion(currentVersion = 1, compatibleVersions = {})
public class HelperExecuteStrategySettings extends HelperStrategySettings {
    private static final int LEVELS_NUMBER_DEFAULT = 3;
    private static final int TICKS_NUMBER_DEFAULT = 1;
    private static final int PERCENT_DEFAULT = 200;
    
    private int levelsNumber = LEVELS_NUMBER_DEFAULT;
    private int tickNumber = TICKS_NUMBER_DEFAULT;
    private int percent = PERCENT_DEFAULT;
    
    public HelperExecuteStrategySettings() {
        super();
    }
    
    public HelperExecuteStrategySettings(HelperExecuteStrategySettings settings) {
        super(settings);
        this.levelsNumber = settings.levelsNumber;
        this.tickNumber = settings.tickNumber;
        this.percent = settings.percent;
    }

    public int getLevelsNumber() {
        return levelsNumber;
    }

    public void setLevelsNumber(int levelsNumber) {
        this.levelsNumber = levelsNumber;
    }

    public int getTickNumber() {
        return tickNumber;
    }

    public void setTickNumber(int tickNumber) {
        this.tickNumber = tickNumber;
    }

    public int getPercent() {
        return percent;
    }

    public void setPercent(int percent) {
        this.percent = percent;
    }
}
