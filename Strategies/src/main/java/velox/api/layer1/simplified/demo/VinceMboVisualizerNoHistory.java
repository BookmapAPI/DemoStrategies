package velox.api.layer1.simplified.demo;

import velox.api.layer1.annotations.Layer1ApiVersion;
import velox.api.layer1.annotations.Layer1ApiVersionValue;
import velox.api.layer1.annotations.Layer1SimpleAttachable;
import velox.api.layer1.annotations.Layer1StrategyName;
import velox.api.layer1.data.InstrumentInfo;
import velox.api.layer1.layers.utils.OrderBook;
import velox.api.layer1.layers.utils.mbo.Order;
import velox.api.layer1.layers.utils.mbo.OrderBookMbo;
import velox.api.layer1.messages.indicators.IndicatorLineStyle;
import velox.api.layer1.messages.indicators.Layer1ApiUserMessageModifyIndicator;
import velox.api.layer1.simplified.*;
import velox.api.layer1.simplified.demo.model.OrderAndCount;
import velox.gui.StrategyPanel;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

/**
 * Visualizes MBO data
 */
@Layer1SimpleAttachable
@Layer1StrategyName("Vince Mbo visualizer: no history")
@Layer1ApiVersion(Layer1ApiVersionValue.VERSION2)
public class VinceMboVisualizerNoHistory
        implements CustomModule, CustomSettingsPanelProvider, MarketByOrderDepthDataListener {

    private OrderBookMbo orderBookMbo = new OrderBookMbo();
    private OrderBook orderBook = new OrderBook();
    private TreeMap<String, OrderAndCount> orderUpdateMap = new TreeMap<>();
    private JLabel displayLabel;
    private AtomicInteger lastDelta = new AtomicInteger(0);
    private final long widgetRangeLifeSpan = 1_000 * 1_000_000_000l; // 1000sec so sample length is 10 sec

    private AtomicBoolean updateIsScheduled = new AtomicBoolean();

    public enum OrderActionType {
        NEW,
        UPDATED,
        CANCELLED
    }

    private Indicator distanceIndicator;

    public VinceMboVisualizerNoHistory() {
        SwingUtilities.invokeLater(() -> {
            displayLabel = new JLabel();
        });
    }

    @Override
    public void initialize(String alias, InstrumentInfo info, Api api, InitialState initialState) {

//        WidgetRules widgetRules = new WidgetRules(-1000, 1000, widgetRangeLifeSpan);
        distanceIndicator = api.registerIndicatorModifiable("Distance", Layer1ApiUserMessageModifyIndicator.GraphType.BOTTOM, 0);
//        distanceIndicator.setWidgetRules(widgetRules);
        distanceIndicator.setColor(Color.YELLOW);
        AxisRules axisRules = new AxisRules();

//        distanceIndicator.setWidgetRules(widgetRules);

//        WidgetGroup widgetGroup = new WidgetGroup();
//        widgetGroup.setWidgetRules(widgetRules);
//        widgetGroup.addIndicator(distanceIndicator);


    }

    @Override
    public void stop() {
    }

    @Override
    public void send(String orderId, boolean isBid, int price, int size) {
        orderBookMbo.send(orderId, isBid, price, size);
        long newLevelSize = 0;
        synchronized (orderBook) {
            long levelSize = orderBook.getSizeFor(isBid, price, 0);
            levelSize += size;
            newLevelSize = levelSize;
            orderBook.onUpdate(isBid, price, levelSize);
        }

        scheduleUpdateIfNecessary(orderId, isBid, price, size, newLevelSize, OrderActionType.NEW);
    }

    @Override
    public void replace(String orderId, int price, int size) {
        Order oldOrder = orderBookMbo.getOrder(orderId);
        boolean isBid = oldOrder.isBid();
        int oldPrice = oldOrder.getPrice();
        int oldSize = oldOrder.getSize();
        long myNewLevelSize = 0;

        orderBookMbo.replace(orderId, price, size);

        synchronized (orderBook) {
            long oldLevelSize = orderBook.getSizeFor(isBid, oldPrice, 0);
            oldLevelSize -= oldSize;

            orderBook.onUpdate(isBid, oldPrice, oldLevelSize);

            long newLevelSize = orderBook.getSizeFor(isBid, price, 0);
            newLevelSize += size;
            myNewLevelSize = newLevelSize;
            orderBook.onUpdate(isBid, price, newLevelSize);

            if (this.orderUpdateMap.containsKey(orderId)) {
                this.orderUpdateMap.put(orderId, new OrderAndCount(orderBookMbo.getOrder(orderId), this.orderUpdateMap.get(orderId).getCount() + 1L));
            } else {
                this.orderUpdateMap.put(orderId, new OrderAndCount(orderBookMbo.getOrder(orderId), 1L));
            }
        }
        scheduleUpdateIfNecessary(orderId, isBid, price, size, myNewLevelSize, OrderActionType.UPDATED);
    }

    @Override
    public void cancel(String orderId) {
        Order oldOrder = orderBookMbo.getOrder(orderId);
        boolean isBid = oldOrder.isBid();
        int price = oldOrder.getPrice();
        int size = oldOrder.getSize();
        long newLevelSize = 0;

        orderBookMbo.cancel(orderId);

        synchronized (orderBook) {
            long levelSize = orderBook.getSizeFor(isBid, price, 0);
            levelSize -= size;
            newLevelSize = levelSize;
            orderBook.onUpdate(isBid, price, levelSize);

            this.orderUpdateMap.remove(orderId);
        }
        scheduleUpdateIfNecessary(orderId, isBid, price, size, newLevelSize, OrderActionType.CANCELLED);
    }

    private void scheduleUpdateIfNecessary(String orderId, boolean isBid, int price, int size, long newLevelSize, OrderActionType updateType) {
        boolean shouldSchedule = !updateIsScheduled.getAndSet(true);

        if (shouldSchedule) {
            SwingUtilities.invokeLater(() -> {
                updateIsScheduled.set(false);

                StringBuilder builder = new StringBuilder();
                builder.append("<html>");


                synchronized (orderBook) {


//                        builder.append("OrderId: ").append(orderId).append(" isBid: ").append(isBid).append(" price: ").append(price).append(" size ").append(size).append(" newLevelSize: ").append(newLevelSize).append(size).append(" updateType: ").append(updateType).append("<br/>");
//                        builder.append(" price: ").append(price).append(" size ").append(size).append(" newLevelSize: ").append(newLevelSize).append(" updateType: ").append(updateType);

                    Iterator<Entry<String, OrderAndCount>> orderUpdateMapIterator = this.orderUpdateMap.entrySet().stream().filter(entry -> entry.getValue().getOrder().getSize() > 50).filter(entry -> entry.getValue().getCount() > 1).iterator();
                    Optional<Entry<String, OrderAndCount>> largestBidBotOrder = this.orderUpdateMap.entrySet().stream().filter(entry -> entry.getValue().getOrder().getSize() > 50).filter(entry -> entry.getValue().getCount() > 1).filter(entry -> entry.getValue().getOrder().isBid()).max((obj1, obj2) -> obj1.getValue().getOrder().getSize() - obj2.getValue().getOrder().getSize());
                    Optional<Entry<String, OrderAndCount>> largestAskBotOrder = this.orderUpdateMap.entrySet().stream().filter(entry -> entry.getValue().getOrder().getSize() > 50).filter(entry -> entry.getValue().getCount() > 1).filter(entry -> !entry.getValue().getOrder().isBid()).max((obj1, obj2) -> obj1.getValue().getOrder().getSize() - obj2.getValue().getOrder().getSize());
//                    while (orderUpdateMapIterator.hasNext()) {
//                        Entry<String, OrderAndCount> next = orderUpdateMapIterator.next();
//                        Order order = orderBookMbo.getOrder(next.getKey());
//                        if (order != null) {
//                            builder.append("OrderId: ").append(order.getOrderId()).append(" size ").append(order.getSize()).append(" count: ").append(next.getValue().getCount()).append("<br/>");
//                        }
//                    }


                    int askDistance = 0;
                    int bidDistance = 0;
                    if (largestAskBotOrder.isPresent()) {
                        Order order = largestAskBotOrder.get().getValue().getOrder();
                        askDistance = order.getPrice() - orderBook.getBestAskPriceOrNone();

                        double actualPrice = order.getPrice() / 4.0;
                        builder.append("Ask Order: ").append(actualPrice).append(" size ").append(order.getSize()).append(" count: ").append(largestAskBotOrder.get().getValue().getCount()).append(" Distance: ").append(askDistance).append("<br/>");
                    }


                    if (largestBidBotOrder.isPresent()) {
                        Order order = largestBidBotOrder.get().getValue().getOrder();
                        bidDistance = orderBook.getBestBidPriceOrNone() - order.getPrice();
                        double actualPrice = order.getPrice() / 4.0;

                        builder.append("Bid Order: ").append(actualPrice).append(" size ").append(order.getSize()).append(" count: ").append(largestBidBotOrder.get().getValue().getCount()).append(" Distance: ").append(bidDistance).append("<br/>");
                    }
                    ;


                    //TODO: Move to its own method
                    //Update Indicator:
//                    if (askDistance.get() > bidDistance.get()) {
////                        distanceIndicator.setColor(Color.GREEN);
//                        distanceIndicator.addPoint(askDistance.get() - bidDistance.get());
//                    } else if (bidDistance.get() > askDistance.get()) {
////                        distanceIndicator.setColor(Color.RED);
//                        distanceIndicator.addPoint(bidDistance.get() - askDistance.get());
//                    } else {
////                        distanceIndicator.setColor(Color.GRAY);
//                        distanceIndicator.addPoint(0);
//                    }


                    int newDelta = largestAskBotOrder.isPresent() && largestBidBotOrder.isPresent() ? askDistance - bidDistance : 0;
                    builder.append("Distance ").append(newDelta).append("<br/>");

                    int oldDelta = lastDelta.getAndSet(newDelta);
//                    //Set new delta and flip color if necessary
//                    if (oldDelta >= 0 && newDelta < 0) {
//                        distanceIndicator.setColor(Color.RED);
//                    } else if (oldDelta < 0 && newDelta >= 0) {
//                        distanceIndicator.setColor(Color.GREEN);
//                    }

                    distanceIndicator.addPoint(newDelta);


                    builder.append("</html>");
                    displayLabel.setText(builder.toString());

                }
            });
        }
    }


    private void scheduleUpdateIfNecessaryLegacy() {
        boolean shouldSchedule = !updateIsScheduled.getAndSet(true);

        if (shouldSchedule) {
            SwingUtilities.invokeLater(() -> {
                updateIsScheduled.set(false);

                StringBuilder builder = new StringBuilder();
                builder.append("<html>");

                synchronized (orderBook) {
                    Iterator<Entry<Integer, Long>> askItterator = orderBook.getAskMap().entrySet().iterator();
                    Iterator<Entry<Integer, Long>> bidItterator = orderBook.getBidMap().entrySet().iterator();

                    List<String> askRows = new ArrayList<>();
                    for (int i = 0; i < 30 && askItterator.hasNext(); ++i) {
                        Entry<Integer, Long> nextAskEntry = askItterator.next();
                        askRows.add(" Vince 2 ASK Distance: " + i + " Price(int): " + nextAskEntry.getKey() + " Size: "
                                + nextAskEntry.getValue() + "<br/>");
                    }
                    Collections.reverse(askRows);
                    askRows.forEach(builder::append);

                    for (int i = 0; i < 30 && bidItterator.hasNext(); ++i) {
                        Entry<Integer, Long> nextBidEntry = bidItterator.next();
                        builder.append("Vince 2 BID Distance: " + i + " Price(int): " + nextBidEntry.getKey() + " Size: "
                                + nextBidEntry.getValue() + "<br/>");
                    }
                }

                builder.append("</html>");
                displayLabel.setText(builder.toString());
            });
        }
    }

    @Override
    public StrategyPanel[] getCustomSettingsPanels() {

        displayLabel = new JLabel();
        scheduleUpdateIfNecessary("INITIAL", false, -1, -1, 100, OrderActionType.NEW);

        StrategyPanel ordersPanel = new StrategyPanel("Vince Order book");
        ordersPanel.add(displayLabel);
        return new StrategyPanel[]{ordersPanel};
    }
}
