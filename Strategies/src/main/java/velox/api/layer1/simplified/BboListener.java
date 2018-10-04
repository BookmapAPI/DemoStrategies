package velox.api.layer1.simplified;

public interface BboListener {
    void onBbo(int bidPrice, int bidSize, int askPrice, int askSize);
}
