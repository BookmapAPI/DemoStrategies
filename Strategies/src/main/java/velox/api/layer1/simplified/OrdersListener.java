package velox.api.layer1.simplified;

import velox.api.layer1.data.ExecutionInfo;
import velox.api.layer1.data.OrderInfoUpdate;

/**
 * Provides order updates. Please read warnings before using it. <br>
 * <b>Warning 1: Order prices are provided as raw price value (without dividing
 * by min tick (pips)), so you will have to do multiplication yourself if you
 * want to show those on main chart.</b> <br>
 */
public interface OrdersListener {
    void onOrderUpdated(OrderInfoUpdate orderInfoUpdate);

    void onOrderExecuted(ExecutionInfo executionInfo);
}
