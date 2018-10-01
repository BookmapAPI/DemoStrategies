package velox.api.layer1.layers.tradinghelper;

import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

public class Utils {
    public static int toRange(int a, int lower, int upper) {
        if (a < lower) {
            a = lower;
        }
        if (a > upper) {
            a = upper;
        }
        return a;
    }
    
    public static JSpinner createSpinner(int desiredValue, int minValue, int maxValue, int stepSize) {
        return new JSpinner(new SpinnerNumberModel(toRange(desiredValue, minValue, maxValue),
                minValue, maxValue, stepSize));
    }
}
