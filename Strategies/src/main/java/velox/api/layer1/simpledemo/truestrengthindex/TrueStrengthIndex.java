package velox.api.layer1.simpledemo.truestrengthindex;

import velox.api.layer1.common.Log;

import java.util.LinkedList;

public class TrueStrengthIndex {

    private final LinkedList<Double> ema = new LinkedList<>();
    private final LinkedList<Double> doubleEma = new LinkedList<>();
    private final LinkedList<Double> absEma = new LinkedList<>();
    private final LinkedList<Double> absDoubleEma = new LinkedList<>();
    private final LinkedList<Double> tsi = new LinkedList<>();
    private volatile Boolean isInitialized = false;
    private int shortPeriod;
    private int longPeriod;
    private double alpha;
    private double beta;

    public TrueStrengthIndex(Integer shortPeriod, Integer longPeriod) {
        if (shortPeriod < longPeriod) {
            this.shortPeriod = shortPeriod;
            this.longPeriod = longPeriod;
        } else {
            this.shortPeriod = longPeriod;
            this.longPeriod = shortPeriod;
        }

        alpha = getConst(shortPeriod);
        beta = getConst(longPeriod);
    }

    public Double getTsi(Double newPrice) {
        double tsi;
        if (!this.isInitialized) {
            tsi = initialize(newPrice);
        } else {
            tsi = addTsi(newPrice);
        }
        return tsi;
    }

    public boolean getRoc() {
        if (tsi.size() >= 3) {
            int n = tsi.size() - 1;
            double roc1 = calculateRoc(tsi.getLast(), tsi.get(n - 1));
            double roc2 = calculateRoc(tsi.get(n - 1), tsi.get(n - 2));

            return roc1 >= roc2;
        }
        return false;
    }

    public void setShortPeriod(int shortPeriod) {
        this.shortPeriod = shortPeriod;
        this.alpha = getConst(shortPeriod);
    }

    public void setLongPeriod(int longPeriod) {
        this.longPeriod = longPeriod;
        beta = getConst(longPeriod);
    }

    synchronized private Double initialize(Double newPrice) {
        tsi.clear();
        ema.clear();
        doubleEma.clear();
        absEma.clear();
        absDoubleEma.clear();

        ema.add(newPrice);
        doubleEma.add(newPrice);
        absEma.add(Math.abs(newPrice));
        absDoubleEma.add(Math.abs(newPrice));
        tsi.add((newPrice / Math.abs(newPrice)) * 100.0);

        isInitialized = true;
        notifyAll();
        return tsi.getLast();
    }

    synchronized private Double addTsi(Double newPrice) {
        while (!isInitialized) {
            try {
                wait();
            } catch (InterruptedException e) {
                Log.warn("Layer1ApiTrueStrengthIndex: InterruptedException: " + e);
            }
        }
        addTsiValue(newPrice);
        return tsi.getLast();
    }

    private void addTsiValue(Double value) {
        addDoubleEmaValue(value, ema, doubleEma);
        addDoubleEmaValue(Math.abs(value), absEma, absDoubleEma);

        tsi.add((doubleEma.getLast() / absDoubleEma.getLast()) * 100.0);
    }

    private void addDoubleEmaValue(Double value, LinkedList<Double> ema, LinkedList<Double> doubleEma) {
        ema.add(ema.getLast() * (1 - alpha) + alpha * value);
        doubleEma.add(doubleEma.getLast() * (1 - beta) + alpha * ema.getLast());
    }

    private double getConst(int period) {
        return 2. / (period + 1);
    }

    private double calculateRoc(double currentValue, double previousValue) {
        return ((currentValue - previousValue) / previousValue) * 100.0;
    }
}
