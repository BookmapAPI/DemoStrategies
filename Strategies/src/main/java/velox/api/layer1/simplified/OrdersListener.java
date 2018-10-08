package velox.api.layer1.simplified;

import velox.api.layer1.data.ExecutionInfo;
import velox.api.layer1.data.OrderInfoUpdate;

/**
 * Provides order updates.<br>
 * <b>Warning 1: Order prices are provided as raw price value (without dividing
 * by min tick (pips)), so you will have to do multiplication yourself if you
 * want to show those on main chart.</b> <br>
 */
public interface OrdersListener {
    /**
     * Called each time order is changed (placed/cancelled/updated/filled/partially
     * filled. Please also see {@link OrdersListener} description.
     */
    void onOrderUpdated(OrderInfoUpdate orderInfoUpdate);

    /**
     * Called when execution (fill/partial fill) happens. Please also see
     * {@link OrdersListener} description. Order with
     * {@link ExecutionInfo#executionId} is supposed to exist (you should get at
     * least one {@link #onOrderUpdated(OrderInfoUpdate)} for that order first).
     * Note, that there will be a separate {@link #onOrderUpdated(OrderInfoUpdate)}
     * call reflecting filled/unfilled size change.
     */
    void onOrderExecuted(ExecutionInfo executionInfo);
}
