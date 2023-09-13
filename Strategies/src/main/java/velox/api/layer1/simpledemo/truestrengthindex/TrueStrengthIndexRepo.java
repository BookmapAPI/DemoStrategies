package velox.api.layer1.simpledemo.truestrengthindex;

import velox.api.layer1.layers.strategies.interfaces.InvalidateInterface;
import velox.api.layer1.messages.indicators.SettingsAccess;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class TrueStrengthIndexRepo {
    private final Map<String, TrueStrengthIndexSettings> settingsMap = new HashMap<>();
    private final Map<String, TrueStrengthIndex> trueStrengthIndexByAlias = new HashMap<>();
    private final Map<String, String> indicatorsFullNameToUserName = new HashMap<>();
    private final Map<String, Double> pipsMap = new ConcurrentHashMap<>();
    private final Map<String, InvalidateInterface> invalidateInterfaceMap = new ConcurrentHashMap<>();
    private final Object locker = new Object();
    private SettingsAccess settingsAccess;

    protected void putPips(String alias, Double pips) {
        pipsMap.put(alias, pips);
    }

    protected Double getPips(String alias) {
        return pipsMap.get(alias);
    }

    protected void putTrueStrengthIndex(String alias, TrueStrengthIndex trueStrengthIndex) {
        trueStrengthIndexByAlias.put(alias, trueStrengthIndex);
    }

    protected TrueStrengthIndex getTrueStrengthIndex(String alias) {
        return trueStrengthIndexByAlias.get(alias);
    }

    protected void putInvalidateInterface(String userName, InvalidateInterface invalidateInterface) {
        invalidateInterfaceMap.put(userName, invalidateInterface);
    }

    protected InvalidateInterface getInvalidateInterface(String userName) {
        return invalidateInterfaceMap.get(userName);
    }

    protected void clearInvalidateInterfaceMap() {
        invalidateInterfaceMap.clear();
    }

    protected void executeForEachValueOfInvalidateInterfaceMap(Consumer<? super InvalidateInterface> consumer) {
        invalidateInterfaceMap.values().forEach(consumer);
    }

    protected String getIndicatorNameByFullName(String indicatorName) {
        return indicatorsFullNameToUserName.get(indicatorName);
    }

    protected void putIndicatorNameByFullName(String fullName, String indicatorName) {
        synchronized (indicatorsFullNameToUserName) {
            indicatorsFullNameToUserName.put(fullName, indicatorName);
        }
    }

    protected void executeForEachValueOfIndicatorsFullNameToUserName(Consumer<? super String> consumer) {
        synchronized (indicatorsFullNameToUserName) {
            indicatorsFullNameToUserName.values().forEach(consumer);
        }
    }

    protected TrueStrengthIndexSettings getSettingsFor(String alias) {
        synchronized (locker) {
            TrueStrengthIndexSettings settings = settingsMap.get(alias);
            if (settings == null) {
                settings = (TrueStrengthIndexSettings) settingsAccess.getSettings(alias,
                        TsiConstants.INDICATOR_NAME,
                        TrueStrengthIndexSettings.class);
                settingsMap.put(alias, settings);
            }
            return settings;
        }
    }

    protected void settingsChanged(String settingsAlias,
                                   TrueStrengthIndexSettings settingsObject) {
        synchronized (locker) {
            settingsAccess.setSettings(settingsAlias,
                    TsiConstants.INDICATOR_NAME,
                    settingsObject,
                    TrueStrengthIndexSettings.class);
        }
    }

    protected void setSettingsAccess(SettingsAccess settingsAccess) {
        this.settingsAccess = settingsAccess;
    }
}
