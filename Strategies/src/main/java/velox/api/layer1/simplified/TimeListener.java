package velox.api.layer1.simplified;

public interface TimeListener {
    void onTimestamp(long t);

    /**
     * Even if there are no events you will still be notified with this frequency.
     * -1 if you don't want it.
     */
    long getTimeNotificationInterval();
}
