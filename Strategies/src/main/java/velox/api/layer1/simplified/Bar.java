package velox.api.layer1.simplified;

/** Single OHLC bar */
public class Bar {

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
        initPrice(openPrice);
    }

    public Bar() {
        this(Double.NaN);
    }

    private void initPrice(double openPrice) {
        open = close = high = low = openPrice;
    }

    /** Update bar based on the new trade */
    public void addTrade(boolean isBuy, long volume, double price) {
        if (Double.isNaN(this.open)) {
            open = high = low = price;
        }
        high = Math.max(price, high);
        low = Math.min(price, low);
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
        initPrice(close);
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

    public double getVwap() {
        return (volumePremultipliedPriceBuy + volumePremultipliedPriceSell) / (volumeBuy + volumeSell);
    }
}
