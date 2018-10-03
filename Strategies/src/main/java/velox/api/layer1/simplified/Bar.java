package velox.api.layer1.simplified;

import java.util.concurrent.TimeUnit;

public class Bar {
    
    public static final long INTERVAL_1_SECONDS = TimeUnit.SECONDS.toNanos(1);
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

    private double open;
    private double close;
    private double high;
    private double low;

    private long volumeBuy;
    private long volumeSell;

    private double volumePremultipliedPriceBuy;
    private double volumePremultipliedPriceSell;

    public Bar(Bar other) {
        this.open = other.open;
        this.close = other.close;
        this.high = other.high;
        this.low = other.low;
        this.volumeBuy = other.volumeBuy;
        this.volumeSell = other.volumeSell;
        this.volumePremultipliedPriceBuy = other.volumePremultipliedPriceBuy;
        this.volumePremultipliedPriceSell = other.volumePremultipliedPriceSell;
    }

    public Bar(double openPrice) {
        this.open = this.close = this.high = this.low = openPrice;
    }
    
    public Bar() {
        this(Double.NaN);
    }
    
    /** Update bar based on the new trade */
    public void addTrade(boolean isBuy, long volume, double price) {
        if (Double.isNaN(this.open)) {
            open = high = low = price;
        }
        high = Math.max(price, high);
        low = Math.max(price, low);
        close = price;

        if (isBuy) {
            volumeBuy += volume;
            volumePremultipliedPriceBuy += volume * price;
        } else {
            volumeSell += volume;
            volumePremultipliedPriceSell += volume * price;
        }
    }
    
    /** Set open/close prices to previous close price, clear other fields */
    public void startNext() {
        close = high = low = open;
        volumeBuy = volumeSell = 0;
        volumePremultipliedPriceBuy = volumePremultipliedPriceSell = 0;
    }

    public double getOpen() {
        return open;
    }

    public void setOpen(double open) {
        this.open = open;
    }

    public double getClose() {
        return close;
    }

    public void setClose(double close) {
        this.close = close;
    }

    public double getHigh() {
        return high;
    }

    public void setHigh(double high) {
        this.high = high;
    }

    public double getLow() {
        return low;
    }

    public void setLow(double low) {
        this.low = low;
    }

    public long getVolumeBuy() {
        return volumeBuy;
    }

    public void setVolumeBuy(long volumeBuy) {
        this.volumeBuy = volumeBuy;
    }

    public long getVolumeSell() {
        return volumeSell;
    }

    public void setVolumeSell(long volumeSell) {
        this.volumeSell = volumeSell;
    }

    public long getVolumeTotal() {
        return volumeBuy + volumeSell;
    }

    public double getVwapBuy() {
        return volumePremultipliedPriceBuy / volumeBuy;
    }

    public double getVwapSell() {
        return volumePremultipliedPriceSell / volumeSell;
    }

    public double getVwapTotal() {
        return (volumePremultipliedPriceBuy + volumePremultipliedPriceSell) / (volumeBuy + volumeSell);
    }
}
