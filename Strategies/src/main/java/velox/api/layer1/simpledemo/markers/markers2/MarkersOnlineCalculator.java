package velox.api.layer1.simpledemo.markers.markers2;

import velox.api.layer1.data.TradeInfo;
import velox.api.layer1.datastructure.events.TradeAggregationEvent;
import velox.api.layer1.layers.strategies.interfaces.CalculatedResultListener;
import velox.api.layer1.layers.strategies.interfaces.InvalidateInterface;
import velox.api.layer1.layers.strategies.interfaces.OnlineCalculatable;
import velox.api.layer1.layers.strategies.interfaces.OnlineValueCalculatorAdapter;
import velox.api.layer1.messages.indicators.DataStructureInterface;

import java.util.ArrayList;
import java.util.function.Consumer;

public class MarkersOnlineCalculator implements OnlineCalculatable {
    private final MarkersRepo markersRepo;

    private final MarkersIndicatorColor markersIndicatorColor;
    private DataStructureInterface dataStructureInterface;

    public MarkersOnlineCalculator(MarkersRepo markersRepo, MarkersIndicatorColor markersIndicatorColor) {
        this.markersRepo = markersRepo;
        this.markersIndicatorColor = markersIndicatorColor;
    }

    @Override
    public void calculateValuesInRange(String indicatorName, String alias, long t0, long intervalWidth,
                                       int intervalsNumber, CalculatedResultListener listener) {
        if (dataStructureInterface == null) {
            listener.setCompleted();
            return;
        }

        String userName = markersRepo.getIndicatorNameByFullName(indicatorName);
        MarkersDemoConstants indicator = MarkersDemoConstants.fromIndicatorName(userName);
        if (indicator == MarkersDemoConstants.MAIN_INDEX) {
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
        String userName = markersRepo.getIndicatorNameByFullName(indicatorName);
        markersRepo.putInvalidateInterface(userName, invalidateInterface);

        if (dataStructureInterface == null) {
            return new OnlineValueCalculatorAdapter() {
            };
        }

        MarkersDemoConstants indicator = MarkersDemoConstants.fromIndicatorName(userName);
        if (indicator == MarkersDemoConstants.MAIN_INDEX) {
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
                    listener.accept(markersIndicatorColor.createNewTradeMarker(price));
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

        double lastPrice = ((TradeAggregationEvent) intervalResponse.get(0).events
                .get(DataStructureInterface.StandardEvents.TRADE.toString())).lastPrice;

        for (int i = 1; i <= intervalsNumber; ++i) {
            TradeAggregationEvent trades = (TradeAggregationEvent) intervalResponse.get(i).events
                    .get(DataStructureInterface.StandardEvents.TRADE.toString());

            if (!Double.isNaN(trades.lastPrice)) {
                lastPrice = trades.lastPrice;
            }

            if (trades.askAggressorMap.isEmpty() && trades.bidAggressorMap.isEmpty()) {
                listener.provideResponse(lastPrice);
            } else {
                listener.provideResponse(markersIndicatorColor.createNewTradeMarker(lastPrice));
            }
        }

        listener.setCompleted();
    }
}
