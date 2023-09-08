package velox.api.layer1.simpledemo.truestrengthindex;

import velox.api.layer1.common.Log;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class TrueStrengthIndex {

    private final LinkedList<Double> ema = new LinkedList<>();
    private final LinkedList<Double> doubleEma = new LinkedList<>();
    private final LinkedList<Double> absEma = new LinkedList<>();
    private final LinkedList<Double> absDoubleEma = new LinkedList<>();
    private Double lastPrice = Double.NaN;
    private int shortPeriod = 13;
    private int longPeriod = 25;
    private double alpha = 2. / (shortPeriod + 1);
    private double beta = 2. / (longPeriod + 1);
    private LinkedList<Double> tsi = new LinkedList<>();
    private volatile Boolean flag = false;

    synchronized public List<Double> addNewTsiValues(List<Double> closePrices) {
        lastPrice = closePrices.get(closePrices.size() - 1);
        tsi = new LinkedList<>();

        List<Double> diff = new ArrayList<>();

        for (int i = 1; i < closePrices.size(); i++) {
            diff.add(closePrices.get(i) - closePrices.get(i - 1));
        }

        countTsiValues(diff);

        flag = true;
        notifyAll();
        return tsi;
    }

    synchronized public Double addTwoTsiValues(Double firstPrice, Double secondPrice) {
        lastPrice = secondPrice;
        tsi.clear();
        ema.clear();
        doubleEma.clear();
        absEma.clear();
        absDoubleEma.clear();

        double diff = secondPrice - firstPrice;
        ema.add(diff);
        doubleEma.add(diff);
        absEma.add(Math.abs(diff));
        absDoubleEma.add(Math.abs(diff));
        tsi.add((diff / Math.abs(diff)) * 100.0);

        flag = true;
        notifyAll();
        return tsi.getLast();
    }
    synchronized public Double addTsiValue(Double newPrice) {
        while(flag.equals(false)) {
            try {
                wait();
            } catch (InterruptedException e) {
                Log.warn("Layer1ApiTrueStrengthIndex: InterruptedException: " + e);
            }
        }
        countTsiValue(newPrice - lastPrice);
        lastPrice = newPrice;
        return tsi.getLast();
    }

    private void countTsiValues(List<Double> values) {
        ema.add(values.get(0));
        doubleEma.add(values.get(0));
        absEma.add(values.get(0));
        doubleEma.add(values.get(0));
        for (int i = 1; i < values.size(); i++) {
            countTsiValue(values.get(i));
        }
    }

    private void countTsiValue(Double value) {
        addDoubleEmaValue(value, ema, doubleEma);
        addDoubleEmaValue(Math.abs(value), absEma, absDoubleEma);

        tsi.add((doubleEma.getLast() / absDoubleEma.getLast()) * 100.0);
    }
    private void addDoubleEmaValue(Double value, LinkedList<Double> ema, LinkedList<Double> doubleEma) {
        ema.add(ema.getLast() * (1 - alpha) + alpha * value);
        doubleEma.add(doubleEma.getLast() * (1 - beta) + alpha * ema.getLast());
    }
}
