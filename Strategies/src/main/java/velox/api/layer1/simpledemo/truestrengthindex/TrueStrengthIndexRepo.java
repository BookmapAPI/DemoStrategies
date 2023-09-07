package velox.api.layer1.simpledemo.truestrengthindex;

import velox.api.layer1.layers.strategies.interfaces.InvalidateInterface;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TrueStrengthIndexRepo {
    private final Map<String, InvalidateInterface> invalidateInterfaceMap = new ConcurrentHashMap<>();
    private final Map<String, String> indicatorsFullNameToShortName = new HashMap<>();

    public void putInvalidateInterface(String shortName, InvalidateInterface invalidateInterface) {
        invalidateInterfaceMap.put(shortName, invalidateInterface);
    }

    public InvalidateInterface getInvalidateInterface(String shortName) {
        return invalidateInterfaceMap.get(shortName);
    }

    public void putIndicatorShortNameByFullName(String fullName, String shortName) {
        indicatorsFullNameToShortName.put(fullName, shortName);
    }

    public String getIndicatorShortNameByFullName(String fullName) {
        return indicatorsFullNameToShortName.get(fullName);
    }
}
