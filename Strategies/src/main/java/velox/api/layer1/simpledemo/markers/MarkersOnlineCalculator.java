package velox.api.layer1.simpledemo.markers;

import velox.api.layer1.common.Log;
import velox.api.layer1.data.*;
import velox.api.layer1.datastructure.events.OrderExecutedEvent;
import velox.api.layer1.datastructure.events.OrderUpdatedEvent;
import velox.api.layer1.datastructure.events.OrderUpdatesExecutionsAggregationEvent;
import velox.api.layer1.datastructure.events.TradeAggregationEvent;
import velox.api.layer1.layers.strategies.interfaces.CalculatedResultListener;
import velox.api.layer1.layers.strategies.interfaces.InvalidateInterface;
import velox.api.layer1.layers.strategies.interfaces.OnlineCalculatable;
import velox.api.layer1.layers.strategies.interfaces.OnlineValueCalculatorAdapter;
import velox.api.layer1.messages.indicators.DataStructureInterface;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class MarkersOnlineCalculator implements OnlineCalculatable {
    private final Map<String, Double> pipsMap = new ConcurrentHashMap<>();
    private final MarkersIndicatorColor markersIndicatorColor;
    private final BufferedImage tradeIcon = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
    private final Layer1ApiMarkersDemo2 layer1ApiMarkersDemo2;
    private DataStructureInterface dataStructureInterface;

    public MarkersOnlineCalculator(MarkersIndicatorColor markersIndicatorColor,
                                   Layer1ApiMarkersDemo2 layer1ApiMarkersDemo2) {
        this.markersIndicatorColor = markersIndicatorColor;
        this.layer1ApiMarkersDemo2 = layer1ApiMarkersDemo2;

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

        String userName = layer1ApiMarkersDemo2.getFullNameByIndicator(indicatorName);

        switch (userName) {
            case Layer1ApiMarkersDemo2.INDICATOR_NAME_TRADE: {
                ArrayList<DataStructureInterface.TreeResponseInterval> intervalResponse =
                        dataStructureInterface.get(t0, intervalWidth, intervalsNumber, alias,
                        new DataStructureInterface.StandardEvents[] {DataStructureInterface.StandardEvents.TRADE});

                double lastPrice = ((TradeAggregationEvent) intervalResponse.get(0).events
                        .get(DataStructureInterface.StandardEvents.TRADE.toString())).lastPrice;

                for (int i = 1; i <= intervalsNumber; ++i) {
                    TradeAggregationEvent trades = (TradeAggregationEvent)intervalResponse.get(i).events
                            .get(DataStructureInterface.StandardEvents.TRADE.toString());

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

                listener.setCompleted();
                break;
            } case Layer1ApiMarkersDemo2.INDICATOR_NAME_CIRCLES: {
                ArrayList<DataStructureInterface.TreeResponseInterval> intervalResponse = dataStructureInterface.get(t0, intervalWidth, intervalsNumber, alias,
                        new DataStructureInterface.StandardEvents[] {DataStructureInterface.StandardEvents.ORDER});
                for (int i = 1; i <= intervalsNumber; ++i) {
                    OrderUpdatesExecutionsAggregationEvent orders = (OrderUpdatesExecutionsAggregationEvent) intervalResponse.get(i).events.get(DataStructureInterface.StandardEvents.ORDER.toString());

                    ArrayList<OnlineCalculatable.Marker> result = new ArrayList<>();

                    BufferedImage orderIcon = markersIndicatorColor.getOrderIconByAlias(alias);

                    for (Object object : orders.orderUpdates) {
                        if (object instanceof OrderExecutedEvent) {
                            OrderExecutedEvent orderExecutedEvent = (OrderExecutedEvent) object;
                            result.add(new OnlineCalculatable.Marker(orderExecutedEvent.executionInfo.price / pipsMap.getOrDefault(orderExecutedEvent.alias, 1.),
                                    -orderIcon.getHeight() / 2, -orderIcon.getWidth() / 2, orderIcon));
                        } else if (object instanceof OrderUpdatedEvent) {
                            OrderUpdatedEvent orderUpdatedEvent = (OrderUpdatedEvent) object;
                            if (orderUpdatedEvent.orderInfoUpdate.status == OrderStatus.CANCELLED) {
                                result.add(new OnlineCalculatable.Marker(getActivePrice(orderUpdatedEvent.orderInfoUpdate) / pipsMap.getOrDefault(orderUpdatedEvent.alias, 1.),
                                        -orderIcon.getHeight() / 2, -orderIcon.getWidth() / 2, orderIcon));
                            }
                        }
                    }

                    listener.provideResponse(result);
                }

                listener.setCompleted();
                break;
            } default:
                throw new IllegalArgumentException("Unknown indicator name " + indicatorName);
        }

    }

    @Override
    public OnlineValueCalculatorAdapter createOnlineValueCalculator(String indicatorName,
                                                                    String indicatorAlias,
                                                                    long time,
                                                                    Consumer<Object> listener,
                                                                    InvalidateInterface invalidateInterface) {
        String userName = layer1ApiMarkersDemo2.getFullNameByIndicator(indicatorName);
        layer1ApiMarkersDemo2.putInvalidateInterface(userName, invalidateInterface);

        if (dataStructureInterface == null) {
            return new OnlineValueCalculatorAdapter() {};
        }

        BufferedImage orderIcon = markersIndicatorColor.getOrderIconByAlias(indicatorAlias);

        switch (userName) {
            case Layer1ApiMarkersDemo2.INDICATOR_NAME_TRADE:
                return getCirclesTradeOnlineValueCalculatorAdapter(indicatorAlias, listener);
            case Layer1ApiMarkersDemo2.INDICATOR_NAME_CIRCLES:
                return getCirclesTradeOnlineValueCalculatorAdapter(indicatorAlias, listener, orderIcon);
            default:
                throw new IllegalArgumentException("Unknown indicator name " + indicatorName);
        }
    }

    private OnlineValueCalculatorAdapter getCirclesTradeOnlineValueCalculatorAdapter(String indicatorAlias,
                                                                                     Consumer<Object> listener,
                                                                                     BufferedImage orderIcon) {
        return new OnlineValueCalculatorAdapter() {
            private final Map<String, String> orderIdToAlias = new HashMap<>();

            @Override
            public void onOrderExecuted(ExecutionInfo executionInfo) {
                String alias = orderIdToAlias.get(executionInfo.orderId);
                if (alias != null) {
                    if (alias.equals(indicatorAlias)) {
                        Double pips = pipsMap.get(alias);
                        if (pips != null) {
                            listener.accept(new Marker(executionInfo.price / pips, -orderIcon.getHeight() / 2, -orderIcon.getWidth() / 2, orderIcon));
                        } else {
                            Log.info("Unknown pips for instrument " + alias);
                        }

                    }
                } else {
                    Log.warn("Markers demo: Unknown alias for execution with order id " + executionInfo.orderId);
                }
            }

            @Override
            public void onOrderUpdated(OrderInfoUpdate orderInfoUpdate) {
                if (orderInfoUpdate.instrumentAlias.equals(indicatorAlias)) {
                    if (orderInfoUpdate.status == OrderStatus.CANCELLED ||
                            orderInfoUpdate.status == OrderStatus.DISCONNECTED) {
                        Double pips = pipsMap.get(orderInfoUpdate.instrumentAlias);
                        if (pips != null) {
                            listener.accept(new Marker(getActivePrice(orderInfoUpdate) / pips, -orderIcon.getHeight() / 2, -orderIcon.getWidth() / 2, orderIcon));
                        } else {
                            Log.info("Unknown pips for instrument " + orderInfoUpdate.instrumentAlias);
                        }
                    }
                }
                orderIdToAlias.put(orderInfoUpdate.orderId, orderInfoUpdate.instrumentAlias);
            }
        };
    }

    private OnlineValueCalculatorAdapter getCirclesTradeOnlineValueCalculatorAdapter(String indicatorAlias,
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

    public void putPipsByAlias(String alias, double pips) {
        pipsMap.put(alias, pips);
    }

    public void setDataStructureInterface(DataStructureInterface dataStructureInterface) {
        this.dataStructureInterface = dataStructureInterface;
    }

    private double getActivePrice(OrderInfoUpdate orderInfoUpdate) {
        return (orderInfoUpdate.type == OrderType.STP || orderInfoUpdate.type == OrderType.STP_LMT)
                ? orderInfoUpdate.stopPrice : orderInfoUpdate.limitPrice;
    }
}
