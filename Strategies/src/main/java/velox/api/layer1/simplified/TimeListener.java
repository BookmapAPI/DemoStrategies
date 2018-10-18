package velox.api.layer1.simplified;

/** Get event timestamps */
public interface TimeListener {
    /**
     * Time of the next event(s)
     *
     * @param t
     *            time in nanoseconds
     */
    void onTimestamp(long t);
}
