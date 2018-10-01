package velox.api.layer1.config.beans;

import velox.api.layer1.settings.StrategySettingsVersion;

@StrategySettingsVersion(currentVersion = 1, compatibleVersions = {})
public class HelperEscapeStrategySettings extends HelperStrategySettings {
    public static enum Mode {
        CANCEL,
        MOVE
    }
    
    private static final Mode DEFAULT_MODE = Mode.MOVE;
    private static final int DEFAULT_AFFECTED_LEVELS_NUMBER = 1;
    private static final int DEFAULT_CONSIDERED_LEVELS_NUMBER = 3;
    private static final int DEFAULT_MOVE_DISTANCE = 1;
    private static final int DEFAULT_PERCENT = 60;
    
    private Mode mode = DEFAULT_MODE;
    private int affectedLevelsNumber = DEFAULT_AFFECTED_LEVELS_NUMBER;
    private int consideredLevelsNumber = DEFAULT_CONSIDERED_LEVELS_NUMBER;
    private int moveDistance = DEFAULT_MOVE_DISTANCE;
    private int percent = DEFAULT_PERCENT;
    
    public HelperEscapeStrategySettings() {
        super();
    }
    
    public HelperEscapeStrategySettings(HelperEscapeStrategySettings settings) {
        super(settings);
        this.mode = settings.mode;
        this.affectedLevelsNumber = settings.affectedLevelsNumber;
        this.consideredLevelsNumber = settings.consideredLevelsNumber;
        this.moveDistance = settings.moveDistance;
        this.percent = settings.percent;
    }
    
    public Mode getMode() {
        return mode;
    }
    public void setMode(Mode mode) {
        this.mode = mode;
    }
    public int getAffectedLevelsNumber() {
        return affectedLevelsNumber;
    }
    public void setAffectedLevelsNumber(int affectedLevelsNumber) {
        this.affectedLevelsNumber = affectedLevelsNumber;
    }
    public int getConsideredLevelsNumber() {
        return consideredLevelsNumber;
    }
    public void setConsideredLevelsNumber(int consideredLevelsNumber) {
        this.consideredLevelsNumber = consideredLevelsNumber;
    }
    public int getMoveDistance() {
        return moveDistance;
    }
    public void setMoveDistance(int moveDistance) {
        this.moveDistance = moveDistance;
    }
    public int getPercent() {
        return percent;
    }
    public void setPercent(int percent) {
        this.percent = percent;
    }
    
}
