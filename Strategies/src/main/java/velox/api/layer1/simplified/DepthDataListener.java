package velox.api.layer1.simplified;

/**
 * Get incremental depth updates.
 */
public interface DepthDataListener {
    /**
     * Called on each incremental depth update
     *
     * @param isBid
     *            true if update describes changes to bid side of the order book
     * @param price
     *            price where the update happens (as level number)
     * @param size
     *            new size on the level (0 if level is removed)
     */
    void onDepth(boolean isBid, int price, int size);
}
