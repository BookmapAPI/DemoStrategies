package velox.api.layer1.simpledemo.truestrengthindex;

import velox.api.layer1.layers.strategies.interfaces.CustomEventAggregatble;
import velox.api.layer1.layers.strategies.interfaces.CustomGeneratedEvent;

public class PeriodEvent implements CustomGeneratedEvent {
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

    public PeriodEvent(long time, double open, double close, double tsi) {
        super();
        this.time = time;
        this.open = open;
        this.close = close;
        this.tsi = tsi;
    }

    public PeriodEvent(PeriodEvent other) {
        this(other.time, other.open, other.close, other.tsi);
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
        return new PeriodEvent(time, open, close, tsi);
    }

    @Override
    public String toString() {
        return "[" + time + ": " + open + "/" + close + "]";
    }

    public double getPrice() {
        return open - close;
    }

    public Double getTsi() {
        return tsi;
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
}
