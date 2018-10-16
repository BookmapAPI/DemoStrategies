package velox.api.layer1.simplified;

import java.util.ArrayList;
import java.util.List;

/**
 * Group of indicators that have same value ranges.
 */
public class AxisGroup {

    private List<Indicator> indicators = new ArrayList<>();
    private AxisRules axisRules = null;

    /**
     * Set rules for selecting indicators range. This will be applied to all
     * indicators in a group.
     *
     * @param axisRules
     *            object describing the rules
     */
    public void setAxisRules(AxisRules axisRules) {
        this.axisRules = axisRules;
    }

    /**
     * Add indicator to a group
     *
     * @param indicator
     *            indicator to add
     */
    public void add(Indicator indicator) {
        indicators.add(indicator);
        ((SimplifiedL1ApiLoader<?>.IndicatorImplementation)indicator).setAxisGroup(this);
    }
    
    public List<Indicator> getIndicators() {
        return indicators;
    }
    
    public AxisRules getAxisRules() {
        return axisRules;
    }
}
