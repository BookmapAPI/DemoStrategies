package velox.api.layer1.simplified;

public interface DepthDataListener extends SimplifiedListener {
    void onDepth(boolean isBid, int price, int size);
}
