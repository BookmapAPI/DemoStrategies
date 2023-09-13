package velox.api.layer1.simpledemo.truestrengthindex;

import velox.api.layer1.layers.strategies.interfaces.CustomEventAggregatble;
import velox.api.layer1.layers.strategies.interfaces.CustomGeneratedEvent;
import velox.api.layer1.layers.strategies.interfaces.OnlineCalculatable;

import java.awt.*;
import java.awt.image.BufferedImage;

public class PeriodEvent implements CustomGeneratedEvent {
    private static final int IMAGE_HEIGHT = 4;
    private static final int MIN_IMAGE_WIGHT = 4;
    public static final CustomEventAggregatble AGGREGATOR = new CustomEventAggregatble() {
        @Override
        public CustomGeneratedEvent getInitialValue(long t) {
            return new PeriodEvent(t);
        }

        @Override
        public void aggregateAggregationWithValue(CustomGeneratedEvent aggregation, CustomGeneratedEvent value) {
            PeriodEvent aggregationEvent = (PeriodEvent) aggregation;
            PeriodEvent valueEvent = (PeriodEvent) value;
            aggregationEvent.update(valueEvent);
        }

        @Override
        public void aggregateAggregationWithAggregation(CustomGeneratedEvent aggregation1,
                                                        CustomGeneratedEvent aggregation2) {
            PeriodEvent aggregationEvent1 = (PeriodEvent) aggregation1;
            PeriodEvent aggregationEvent2 = (PeriodEvent) aggregation2;
            aggregationEvent1.update(aggregationEvent2);
        }
    };
    private static final long serialVersionUID = 1L;
    private Double tsi = Double.NaN;
    private long time;
    private double open;
    private double close;
    private int bodyWidthPx;

    public PeriodEvent(long time) {
        this(time, Double.NaN);
    }

    public PeriodEvent(long time, double open) {
        this(time, open, open);
    }

    public PeriodEvent(long time, double open, double close) {
        super();
        this.time = time;
        this.open = open;
        this.close = close;
    }
    public PeriodEvent(long time, double open, double close, double tsi, int bodyWidthPx) {
        super();
        this.time = time;
        this.open = open;
        this.close = close;
        this.tsi = tsi;
        this.bodyWidthPx = bodyWidthPx;
    }

    public PeriodEvent(PeriodEvent other) {
        this(other.time, other.open, other.close, other.tsi, other.bodyWidthPx);
    }

    @Override
    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    @Override
    public Object clone() {
        return new PeriodEvent(time, open, close, tsi, bodyWidthPx);
    }

    @Override
    public String toString() {
        return "[" + time + ": " + open + "/" + close + "]";
    }

    public double getPrice() {
        return open - close;
    }

    public double getClose() {
        return close;
    }

    public void addTsiIfAbsent(TrueStrengthIndex trueStrengthIndex) {
        if (Double.isNaN(tsi)) {
            tsi = trueStrengthIndex.getTsi(getPrice());
        }
    }

    public void update(double price) {
        if (Double.isNaN(price)) {
            return;
        }
        if (Double.isNaN(open)) {
            open = price;
        }
        close = price;
    }

    public void update(PeriodEvent nextPeriod) {
        update(nextPeriod.open);
        update(nextPeriod.close);
    }

    public void applyPips(double pips) {
        open *= pips;
        close *= pips;
    }

    public void setBodyWidthPx(long bodyWidthPx) {
        this.bodyWidthPx = getBodyWidth(bodyWidthPx);
    }

    public OnlineCalculatable.Marker makeMarker(Color color) {
        BufferedImage icon = new BufferedImage(bodyWidthPx, IMAGE_HEIGHT, BufferedImage.TYPE_INT_ARGB);
        Graphics graphics = icon.getGraphics();

        graphics.setColor(color);
        graphics.fillRect(0, 0, bodyWidthPx - 1, IMAGE_HEIGHT - 1);

        return new OnlineCalculatable.Marker(tsi,
                - icon.getWidth() / 2,
                - icon.getHeight() / 2,
                icon);
    }

    private int getBodyWidth(long intervalWidth) {
        long bodyWidth = TsiConstants.PERIOD_INTERVAL_NS / intervalWidth;
        bodyWidth = Math.max(bodyWidth, MIN_IMAGE_WIGHT);
        return (int) bodyWidth;
    }
}
