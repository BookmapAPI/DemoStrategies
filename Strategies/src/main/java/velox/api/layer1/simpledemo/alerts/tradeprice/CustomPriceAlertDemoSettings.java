package velox.api.layer1.simpledemo.alerts.tradeprice;

import velox.api.layer1.messages.indicators.SettingsAccess;
import velox.api.layer1.settings.StrategySettingsVersion;

import java.util.List;

/**
 * Container for the addon settings. Note the {@link StrategySettingsVersion} annotation -
 * it is required for the classes used in {@link SettingsAccess#getSettings(String, String, Class)} or
 * {@link SettingsAccess#getSettings(String, String, Class)}
 */
@StrategySettingsVersion(currentVersion = 1, compatibleVersions = {})
public class CustomPriceAlertDemoSettings {
    private List<CustomDeclarationSettings> declarationSettings;

    /** Instances of this class are serialized/deserialized by the Bookmap,
     * thus we heed to provide a default constructor */
    public CustomPriceAlertDemoSettings() {
    }

    public CustomPriceAlertDemoSettings(List<CustomDeclarationSettings> declarationSettings) {
        this.declarationSettings = declarationSettings;
    }

    public void setDeclarationSettings(List<CustomDeclarationSettings> declarationSettings) {
        this.declarationSettings = declarationSettings;
    }

    public List<CustomDeclarationSettings> getDeclarationSettings() {
        return declarationSettings;
    }
}
