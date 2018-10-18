package velox.api.layer1.simplified;

/** Get best bid/offer updates each time one of those changes. */
public interface BboListener {
    /** Called on each BBO change providing new price/size value */
    void onBbo(int bidPrice, int bidSize, int askPrice, int askSize);
}
