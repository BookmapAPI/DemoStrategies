package velox.api.layer1.simpledemo.alerts.tradeprice;

/**
 * Incorporates the trade matching logic (<em>when</em> to invoke a callback)
 * with the custom callback logic (<em>what</em> to do in a callback)
 */
public class TradeMatcher {
    
    public interface OnMatchCallback {
        void accept(String alias, double price, int size);
    }
    
    public interface TradePredicate {
        boolean test(String alias, double price, int size);
    }
    
    private final TradePredicate tradePredicate;
    private final OnMatchCallback onMatch;
    
    
    public TradeMatcher(TradePredicate tradePredicate, OnMatchCallback onMatch) {
        this.tradePredicate = tradePredicate;
        this.onMatch = onMatch;
    }
    
    /**
     * Invokes the {@link OnMatchCallback} if {@link TradePredicate} returns true
     * for these arguments
     * @param alias
     * @param price
     * @param size
     */
    public void tryMatch(String alias, double price, int size) {
        if (tradePredicate.test(alias, price, size)) {
            onMatch.accept(alias, price, size);
        }
    }
}
