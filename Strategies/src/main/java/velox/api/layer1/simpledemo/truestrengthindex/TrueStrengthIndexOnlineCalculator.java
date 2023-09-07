package velox.api.layer1.simpledemo.truestrengthindex;

import velox.api.layer1.data.TradeInfo;
import velox.api.layer1.datastructure.events.TradeAggregationEvent;
import velox.api.layer1.layers.strategies.interfaces.CalculatedResultListener;
import velox.api.layer1.layers.strategies.interfaces.InvalidateInterface;
import velox.api.layer1.layers.strategies.interfaces.OnlineCalculatable;
import velox.api.layer1.layers.strategies.interfaces.OnlineValueCalculatorAdapter;
import velox.api.layer1.messages.indicators.DataStructureInterface;
import velox.api.layer1.messages.indicators.DataStructureInterface.TreeResponseInterval;
import velox.api.layer1.messages.indicators.DataStructureInterface.StandardEvents;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class TrueStrengthIndexOnlineCalculator implements OnlineCalculatable {
    private final BufferedImage tradeIcon = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
    private final TrueStrengthIndexRepo trueStrengthIndexRepo;
    private DataStructureInterface dataStructureInterface;

    public TrueStrengthIndexOnlineCalculator(TrueStrengthIndexRepo trueStrengthIndexRepo) {
        this.trueStrengthIndexRepo = trueStrengthIndexRepo;

        // Prepare trade marker
        Graphics graphics = tradeIcon.getGraphics();
        graphics.setColor(Color.BLUE);
        graphics.drawLine(0, 0, 15, 15);
        graphics.drawLine(15, 0, 0, 15);
    }

    @Override
    public void calculateValuesInRange(String indicatorName, String alias, long t0, long intervalWidth,
                                       int intervalsNumber, CalculatedResultListener listener) {
        if (dataStructureInterface == null) {
            listener.setCompleted();
            return;
        }

        String shortName = trueStrengthIndexRepo.getIndicatorShortNameByFullName(indicatorName);
        TrueStrengthIndexConstants trueStrengthIndexConstant = TrueStrengthIndexConstants.fromIndicatorName(shortName);

        if (trueStrengthIndexConstant == null) {
            throw new IllegalArgumentException("Unknown indicator name " + indicatorName);
        } else if (trueStrengthIndexConstant == TrueStrengthIndexConstants.MAIN_INDEX) {
            calculateMainIndexValuesInRange(alias, t0, intervalWidth, intervalsNumber, listener);
        }
    }

    @Override
    public OnlineValueCalculatorAdapter createOnlineValueCalculator(String indicatorName,
                                                                    String indicatorAlias,
                                                                    long time,
                                                                    Consumer<Object> listener,
                                                                    InvalidateInterface invalidateInterface) {
        String shortName = trueStrengthIndexRepo.getIndicatorShortNameByFullName(indicatorName);
        trueStrengthIndexRepo.putInvalidateInterface(shortName, invalidateInterface);

        if (dataStructureInterface == null) {
            return new OnlineValueCalculatorAdapter() {
            };
        }

        TrueStrengthIndexConstants trueStrengthIndexConstant = TrueStrengthIndexConstants.fromIndicatorName(shortName);

        if (trueStrengthIndexConstant == TrueStrengthIndexConstants.MAIN_INDEX) {
            return getMainOnlineValueCalculatorAdapter(indicatorAlias, listener);
        } else {
            throw new IllegalArgumentException("Unknown indicator name " + indicatorName);
        }
    }

    public void setDataStructureInterface(DataStructureInterface dataStructureInterface) {
        this.dataStructureInterface = dataStructureInterface;
    }

    private void calculateMainIndexValuesInRange(String alias, long t0, long intervalWidth,
                                                 int intervalsNumber, CalculatedResultListener listener) {
        ArrayList<TreeResponseInterval> intervalResponse = dataStructureInterface.get(t0,
                intervalWidth,
                intervalsNumber,
                alias,
                new StandardEvents[]{StandardEvents.TRADE});

        double lastPrice = ((TradeAggregationEvent) intervalResponse.get(0).events
                .get(StandardEvents.TRADE.toString())).lastPrice;

        for (int i = 1; i <= intervalsNumber; ++i) {
            TradeAggregationEvent trades = (TradeAggregationEvent) intervalResponse.get(i).events
                    .get(StandardEvents.TRADE.toString());

            if (!Double.isNaN(trades.lastPrice)) {
                lastPrice = trades.lastPrice;
            }

            if (trades.askAggressorMap.isEmpty() && trades.bidAggressorMap.isEmpty()) {
                listener.provideResponse(lastPrice);
            } else {
                listener.provideResponse(new OnlineCalculatable.Marker(lastPrice,
                        -tradeIcon.getHeight() / 2, -tradeIcon.getWidth() / 2, tradeIcon));
            }
        }
    }

    private OnlineValueCalculatorAdapter getMainOnlineValueCalculatorAdapter(String indicatorAlias,
                                                                             Consumer<Object> listener) {
        return new OnlineValueCalculatorAdapter() {
            @Override
            public void onTrade(String alias, double price, int size, TradeInfo tradeInfo) {
                if (alias.equals(indicatorAlias)) {
                    listener.accept(new Marker(price,
                            -tradeIcon.getHeight() / 2,
                            -tradeIcon.getWidth() / 2,
                            tradeIcon));
                }
            }
        };
    }
}
