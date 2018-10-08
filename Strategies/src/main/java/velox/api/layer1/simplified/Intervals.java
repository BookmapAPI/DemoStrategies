package velox.api.layer1.simplified;

import java.util.concurrent.TimeUnit;

/**
 * Just a bunch of common intervals. You might prefer to use {@link TimeUnit}
 * directly instead
 */
public class Intervals {

    public static final long INTERVAL_50_MILLISECONDS = TimeUnit.MILLISECONDS.toNanos(50);
    public static final long INTERVAL_100_MILLISECONDS = TimeUnit.MILLISECONDS.toNanos(100);
    public static final long INTERVAL_200_MILLISECONDS = TimeUnit.MILLISECONDS.toNanos(200);
    public static final long INTERVAL_250_MILLISECONDS = TimeUnit.MILLISECONDS.toNanos(250);
    public static final long INTERVAL_500_MILLISECONDS = TimeUnit.MILLISECONDS.toNanos(500);
    public static final long INTERVAL_750_MILLISECONDS = TimeUnit.MILLISECONDS.toNanos(750);

    public static final long INTERVAL_1_SECOND = TimeUnit.SECONDS.toNanos(1);
    public static final long INTERVAL_2_SECONDS = TimeUnit.SECONDS.toNanos(2);
    public static final long INTERVAL_5_SECONDS = TimeUnit.SECONDS.toNanos(5);
    public static final long INTERVAL_10_SECONDS = TimeUnit.SECONDS.toNanos(10);
    public static final long INTERVAL_15_SECONDS = TimeUnit.SECONDS.toNanos(15);
    public static final long INTERVAL_20_SECONDS = TimeUnit.SECONDS.toNanos(20);
    public static final long INTERVAL_30_SECONDS = TimeUnit.SECONDS.toNanos(30);

    public static final long INTERVAL_1_MINUTE = TimeUnit.MINUTES.toNanos(1);
    public static final long INTERVAL_2_MINUTES = TimeUnit.MINUTES.toNanos(2);
    public static final long INTERVAL_5_MINUTES = TimeUnit.MINUTES.toNanos(5);
    public static final long INTERVAL_10_MINUTES = TimeUnit.MINUTES.toNanos(10);
    public static final long INTERVAL_15_MINUTES = TimeUnit.MINUTES.toNanos(15);
    public static final long INTERVAL_20_MINUTES = TimeUnit.MINUTES.toNanos(20);
    public static final long INTERVAL_30_MINUTES = TimeUnit.MINUTES.toNanos(30);
    
    /** Smallest interval allowed */
    public static final long MIN_INTERVAL = INTERVAL_50_MILLISECONDS;
}
