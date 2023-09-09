package velox.api.layer1.simpledemo.truestrengthindex;

import velox.api.layer1.data.TradeInfo;
import velox.api.layer1.datastructure.events.TradeAggregationEvent;
import velox.api.layer1.layers.strategies.interfaces.CalculatedResultListener;
import velox.api.layer1.layers.strategies.interfaces.InvalidateInterface;
import velox.api.layer1.layers.strategies.interfaces.OnlineCalculatable;
import velox.api.layer1.layers.strategies.interfaces.OnlineValueCalculatorAdapter;
import velox.api.layer1.messages.indicators.DataStructureInterface;

import java.util.ArrayList;
import java.util.function.Consumer;

public class TrueStrengthIndexOnlineCalculator implements OnlineCalculatable {
    private final TrueStrengthIndexRepo trueStrengthIndexRepo;
    private DataStructureInterface dataStructureInterface;

    public TrueStrengthIndexOnlineCalculator(TrueStrengthIndexRepo trueStrengthIndexRepo) {
        this.trueStrengthIndexRepo = trueStrengthIndexRepo;
    }

    @Override
    public void calculateValuesInRange(String indicatorName, String alias, long t0, long intervalWidth,
                                       int intervalsNumber, CalculatedResultListener listener) {
        if (dataStructureInterface == null) {
            listener.setCompleted();
            return;
        }

        String userName = trueStrengthIndexRepo.getIndicatorNameByFullName(indicatorName);
        TrueStrengthIndexDemoConstants indicator = TrueStrengthIndexDemoConstants.fromIndicatorName(userName);
        if (indicator == TrueStrengthIndexDemoConstants.MAIN_INDEX) {
            calculateMainIndexValuesInRange(alias, t0, intervalWidth, intervalsNumber, listener);
        } else {
            throw new IllegalArgumentException("Unknown indicator name " + indicatorName);
        }
    }

    @Override
    public OnlineValueCalculatorAdapter createOnlineValueCalculator(String indicatorName,
                                                                    String indicatorAlias,
                                                                    long time,
                                                                    Consumer<Object> listener,
                                                                    InvalidateInterface invalidateInterface) {
        String userName = trueStrengthIndexRepo.getIndicatorNameByFullName(indicatorName);
        trueStrengthIndexRepo.putInvalidateInterface(userName, invalidateInterface);

        if (dataStructureInterface == null) {
            return new OnlineValueCalculatorAdapter() {};
        }

        TrueStrengthIndexDemoConstants indicator = TrueStrengthIndexDemoConstants.fromIndicatorName(userName);
        if (indicator == TrueStrengthIndexDemoConstants.MAIN_INDEX) {
            return getTradeOnlineValueCalculatorAdapter(indicatorAlias, listener);
        } else {
            throw new IllegalArgumentException("Unknown indicator name " + indicatorName);
        }
    }

    protected void setDataStructureInterface(DataStructureInterface dataStructureInterface) {
        this.dataStructureInterface = dataStructureInterface;
    }

    private OnlineValueCalculatorAdapter getTradeOnlineValueCalculatorAdapter(String indicatorAlias,
                                                                              Consumer<Object> listener) {

        return new OnlineValueCalculatorAdapter() {
            @Override
            public void onTrade(String alias, double price, int size, TradeInfo tradeInfo) {
                if (alias.equals(indicatorAlias)) {
                    TrueStrengthIndex t = trueStrengthIndexRepo.getTrueStrengthIndex(alias);
                    listener.accept(t.addTsiValue(price));
                }
            }
        };
    }

    private void calculateMainIndexValuesInRange(String alias,
                                                 long t0,
                                                 long intervalWidth,
                                                 int intervalsNumber,
                                                 CalculatedResultListener listener) {
        ArrayList<DataStructureInterface.TreeResponseInterval> intervalResponse =
                dataStructureInterface.get(t0, intervalWidth, intervalsNumber, alias,
                        new DataStructureInterface.StandardEvents[]{DataStructureInterface.StandardEvents.TRADE});

        TradeAggregationEvent trades = ((TradeAggregationEvent) intervalResponse.get(0).events
                .get(DataStructureInterface.StandardEvents.TRADE.toString()));

        TrueStrengthIndex trueStrengthIndex = trueStrengthIndexRepo.getTrueStrengthIndex(alias);
        double lastPrice = trades.lastPrice;
        double firstPrice = lastPrice;
        boolean isFirst = true;
        double tsi;
        for (int i = 1; i <= intervalsNumber; ++i) {
            trades = (TradeAggregationEvent) intervalResponse.get(i).events
                    .get(DataStructureInterface.StandardEvents.TRADE.toString());

            if (!Double.isNaN(trades.lastPrice)) {
                lastPrice = trades.lastPrice;
            }

            if (isFirst) {
                tsi = trueStrengthIndex.addTwoTsiValues(firstPrice, lastPrice);
                isFirst = false;
            } else {
                tsi = trueStrengthIndex.addTsiValue(lastPrice);
            }
            listener.provideResponse(tsi);
        }
        listener.setCompleted();
    }
}
