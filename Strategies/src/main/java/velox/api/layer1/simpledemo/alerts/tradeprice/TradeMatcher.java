package velox.api.layer1.simpledemo.alerts.tradeprice;

/**
 * Incorporates the trade matching logic (<em>when</em> to invoke a callback)
 * with the custom callback logic (<em>what</em> to do in a callback)
 */
public class TradeMatcher {
    
    public interface OnMatchCallback {
        void accept(String alias, double price, double size);
    }
    
    public interface TradePredicate {
        boolean test(String alias, double price, double size);
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
     * @param alias instrument
     * @param realPrice Bookmap stores price as a number of pips, e.g. for pips = 5
     *                  and real price = 100, the price obtained from onTrade or onDepth
     *                  will be: <br>
     *                  <b>realPrice / pips = price</b>, or<br>
     *                  <b>100 / 5 = 20</b>
     * @param realSize Bookmap stores size as a number of size increments, e.g. for
     *                 sizeMultiplier = 0.5 and real size = 100, the size obtained
     *                 from onTrade or onDepth will be: <br>
     *                 <b>realSize * (1 / sizeMultiplier) = size</b>, or <br>
     *                 <b>100 * (1 / 0.5) = 200</b>
     */
    public void tryMatch(String alias, double realPrice, double realSize) {
        if (tradePredicate.test(alias, realPrice, realSize)) {
            onMatch.accept(alias, realPrice, realSize);
        }
    }
}
