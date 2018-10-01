package velox.api.layer1.config.beans;

public class HelperStrategySettings {
    private boolean isEnabled = false;
    
    public HelperStrategySettings() {
    }
    
    public HelperStrategySettings(HelperStrategySettings settings) {
        this.isEnabled = settings.isEnabled;
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    public void setEnabled(boolean isEnabled) {
        this.isEnabled = isEnabled;
    }
    
}
