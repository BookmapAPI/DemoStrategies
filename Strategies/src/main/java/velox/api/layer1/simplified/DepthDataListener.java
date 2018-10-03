package velox.api.layer1.simplified;

public interface DepthDataListener {
    void onDepth(boolean isBid, int price, int size);
}
