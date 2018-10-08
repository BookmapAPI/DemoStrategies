package velox.api.layer1.simplified;

/**
 * Indicator representing a line.
 */
public interface Indicator {
    /**
     * Set new line Y coordinate in indicator coordinates space. {@link Double#NaN}
     * to stop drawing (can be imagined as switching to invisible color)
     *
     * @param value
     *            new value
     */
    void addPoint(double value);
}
