package velox.api.layer1.simplified;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

/**
 * This class describes axis range selection rules.
 */
public class AxisRules {

    /**
     * If not {@link Double#NaN NaN}, this will be forced as lower boundary of the
     * indicator values range
     */
    private double forcedMin = Double.NaN;
    /**
     * If not {@link Double#NaN NaN}, this will be forced as upper boundary of the
     * indicator values range
     */
    private double forcedMax = Double.NaN;

    /**
     * If not {@link Double#NaN NaN}, min and max points will always be visible, but
     * if those are exceeded by values range will be extended.
     */
    private double includedMin = Double.NaN;
    /**
     * If not {@link Double#NaN NaN}, min and max points will always be visible, but
     * if those are exceeded by values range will be extended.
     */
    private double includedMax = Double.NaN;

    /**
     * Range can be expanded to avoid frequent adjustments. 0 means no expansion, 1
     * means increasing range size 2x, etc
     */
    private double margin = 0;
    
    /** Symmetrical around 0 */
    private boolean symmetrical = false;

    public double getForcedMin() {
        return forcedMin;
    }

    public void setForcedMin(double forcedMin) {
        this.forcedMin = forcedMin;
    }

    public double getForcedMax() {
        return forcedMax;
    }

    public void setForcedMax(double forcedMax) {
        this.forcedMax = forcedMax;
    }

    public double getIncludedMin() {
        return includedMin;
    }

    public void setIncludedMin(double includedMin) {
        this.includedMin = includedMin;
    }

    public double getIncludedMax() {
        return includedMax;
    }

    public void setIncludedMax(double includedMax) {
        this.includedMax = includedMax;
    }

    public double getMargin() {
        return margin;
    }
    
    public void setMargin(double margin) {
        this.margin = margin;
    }
    
    public boolean isSymmetrical() {
        return symmetrical;
    }
    
    public void setSymmetrical(boolean symmetrical) {
        this.symmetrical = symmetrical;
    }

    public Pair<Double, Double> apply(double min, double max) {
        boolean minLocked = !Double.isNaN(forcedMin);
        boolean maxLocked = !Double.isNaN(forcedMax);

        if (minLocked) {
            min = forcedMin;
        }
        if (maxLocked) {
            max = forcedMax;
        }

        if (!minLocked && !Double.isNaN(includedMin)) {
            min = Math.min(min, includedMin);
        }
        
        if (!maxLocked && !Double.isNaN(includedMax)) {
            max = Math.max(max, includedMax);
        }
        
        if (!minLocked && !maxLocked && isSymmetrical()) {
            min = Math.min(min, -max);
            max = Math.max(max, -min);
        }
        
        double expansionValue = (includedMax - includedMin) * margin;
        if (expansionValue > 0) {
            if (!minLocked && !maxLocked) {
                min -= expansionValue / 2;
                max += expansionValue / 2;
            }
            if (minLocked && !maxLocked) {
                max += expansionValue;
            }
            if (maxLocked && !minLocked) {
                min -= expansionValue;
            }
        }
        
        return new ImmutablePair<Double, Double>(min, max);
    }
    
    
}
