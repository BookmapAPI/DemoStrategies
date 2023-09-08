package velox.api.layer1.simpledemo.markers.markers2;

import velox.api.layer1.layers.strategies.interfaces.InvalidateInterface;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class MarkersRepo {
    private final Map<String, String> indicatorsFullNameToUserName = new HashMap<>();
    private final Map<String, InvalidateInterface> invalidateInterfaceMap = new ConcurrentHashMap<>();

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
}
