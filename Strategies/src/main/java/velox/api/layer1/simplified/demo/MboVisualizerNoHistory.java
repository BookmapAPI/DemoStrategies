package velox.api.layer1.simplified.demo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.JLabel;
import javax.swing.SwingUtilities;

import velox.api.layer1.annotations.Layer1ApiVersion;
import velox.api.layer1.annotations.Layer1ApiVersionValue;
import velox.api.layer1.annotations.Layer1SimpleAttachable;
import velox.api.layer1.annotations.Layer1StrategyName;
import velox.api.layer1.data.InstrumentInfo;
import velox.api.layer1.layers.utils.OrderBook;
import velox.api.layer1.layers.utils.mbo.Order;
import velox.api.layer1.layers.utils.mbo.OrderBookMbo;
import velox.api.layer1.simplified.Api;
import velox.api.layer1.simplified.CustomModule;
import velox.api.layer1.simplified.CustomSettingsPanelProvider;
import velox.api.layer1.simplified.InitialState;
import velox.api.layer1.simplified.MarketByOrderDepthDataListener;
import velox.gui.StrategyPanel;

/**
 * Visualizes MBO data
 */
@Layer1SimpleAttachable
@Layer1StrategyName("Mbo visualizer: no history")
@Layer1ApiVersion(Layer1ApiVersionValue.VERSION2)
public class MboVisualizerNoHistory
        implements CustomModule, CustomSettingsPanelProvider, MarketByOrderDepthDataListener {

    private OrderBookMbo orderBookMbo = new OrderBookMbo();
    private OrderBook orderBook = new OrderBook();

    private JLabel displayLabel;

    private AtomicBoolean updateIsScheduled = new AtomicBoolean();
    
    public MboVisualizerNoHistory() {
        SwingUtilities.invokeLater(() -> {
            displayLabel = new JLabel();
        });
    }

    @Override
    public void initialize(String alias, InstrumentInfo info, Api api, InitialState initialState) {
    }

    @Override
    public void stop() {
    }

    @Override
    public void send(String orderId, boolean isBid, int price, int size) {
        orderBookMbo.send(orderId, isBid, price, size);

        synchronized (orderBook) {
            long levelSize = orderBook.getSizeFor(isBid, price, 0);
            levelSize += size;
            orderBook.onUpdate(isBid, price, levelSize);
        }

        scheduleUpdateIfNecessary();
    }

    @Override
    public void replace(String orderId, int price, int size) {
        Order oldOrder = orderBookMbo.getOrder(orderId);
        boolean isBid = oldOrder.isBid();
        int oldPrice = oldOrder.getPrice();
        int oldSize = oldOrder.getSize();

        orderBookMbo.replace(orderId, price, size);

        synchronized (orderBook) {
            long oldLevelSize = orderBook.getSizeFor(isBid, oldPrice, 0);
            oldLevelSize -= oldSize;

            orderBook.onUpdate(isBid, oldPrice, oldLevelSize);

            long newLevelSize = orderBook.getSizeFor(isBid, price, 0);
            newLevelSize += size;
            orderBook.onUpdate(isBid, price, newLevelSize);
        }
        scheduleUpdateIfNecessary();
    }

    @Override
    public void cancel(String orderId) {
        Order oldOrder = orderBookMbo.getOrder(orderId);
        boolean isBid = oldOrder.isBid();
        int price = oldOrder.getPrice();
        int size = oldOrder.getSize();

        orderBookMbo.cancel(orderId);

        synchronized (orderBook) {
            long levelSize = orderBook.getSizeFor(isBid, price, 0);
            levelSize -= size;
            orderBook.onUpdate(isBid, price, levelSize);
        }
        scheduleUpdateIfNecessary();
    }

    private void scheduleUpdateIfNecessary() {
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
                    for (int i = 0; i < 10 && askItterator.hasNext(); ++i) {
                        Entry<Integer, Long> nextAskEntry = askItterator.next();
                        askRows.add("ASK Distance: " + i + " Price(int): " + nextAskEntry.getKey() + " Size: "
                                + nextAskEntry.getValue() + "<br/>");
                    }
                    Collections.reverse(askRows);
                    askRows.forEach(builder::append);
                    
                    for (int i = 0; i < 10 && bidItterator.hasNext(); ++i) {
                        Entry<Integer, Long> nextBidEntry = bidItterator.next();
                        builder.append("BID Distance: " + i + " Price(int): " + nextBidEntry.getKey() + " Size: "
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
        scheduleUpdateIfNecessary();

        StrategyPanel ordersPanel = new StrategyPanel("Order book");
        ordersPanel.add(displayLabel);
        return new StrategyPanel[] { ordersPanel };
    }
}
