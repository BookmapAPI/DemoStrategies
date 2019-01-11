package velox.api.layer1.simpledemo.datarecording;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;

import javax.swing.JCheckBox;

import velox.api.layer1.Layer1ApiDataAdapter;
import velox.api.layer1.Layer1ApiFinishable;
import velox.api.layer1.Layer1ApiInstrumentAdapter;
import velox.api.layer1.Layer1ApiProvider;
import velox.api.layer1.Layer1ApiTradingAdapter;
import velox.api.layer1.Layer1CustomPanelsGetter;
import velox.api.layer1.annotations.Layer1ApiVersion;
import velox.api.layer1.annotations.Layer1ApiVersionValue;
import velox.api.layer1.annotations.Layer1Attachable;
import velox.api.layer1.annotations.Layer1StrategyName;
import velox.api.layer1.common.ListenableHelper;
import velox.api.layer1.data.ExecutionInfo;
import velox.api.layer1.data.InstrumentInfo;
import velox.api.layer1.data.OrderInfoUpdate;
import velox.api.layer1.data.TradeInfo;
import velox.gui.StrategyPanel;

/**
 * Simple demo for recording feed. It takes all data passing through the
 * strategy and writes it to text file in working
 * directory("C:\Bookmap\Config").
 * In replay mode you should not use rewind functionality with this strategy.
 */

@Layer1Attachable
@Layer1StrategyName("FeedRecorder demo")
@Layer1ApiVersion(Layer1ApiVersionValue.VERSION2)
public class FeedRecorder implements Layer1CustomPanelsGetter, Layer1ApiDataAdapter, Layer1ApiFinishable,
        Layer1ApiInstrumentAdapter, Layer1ApiTradingAdapter {

    private SpecificFormatRecorder recorder;
    private Map<String, InstrumentInfo> instruments = new TreeMap<>();
    private Map<String, Integer> instrumentIds = new TreeMap<>();
    private JCheckBox recordTrades = new JCheckBox("Record trades", true);
    private JCheckBox recordOrders = new JCheckBox("Record orders", true);

    public FeedRecorder(Layer1ApiProvider provider) throws IOException {
        File depthRecordsFile = new File(System.getProperty("user.dir"), "FeedRecorder_demo_depth-" + System.currentTimeMillis() + ".txt");
        File ordersRecordsFile = new File(System.getProperty("user.dir"), "FeedRecorder_demo_orders-"  + System.currentTimeMillis() + ".txt");
        recorder = new SpecificFormatRecorder(System.currentTimeMillis(), depthRecordsFile, ordersRecordsFile,
                provider.getSource());
        // register listener to get data
        ListenableHelper.addListeners(provider, this);
    }

    @Override
    public StrategyPanel[] getCustomGuiFor(String alias, String indicatorName) {
        StrategyPanel settingsPanel = new StrategyPanel("FeedRecorder demo");
        settingsPanel.add(recordTrades);
        settingsPanel.add(recordOrders);
        return new StrategyPanel[] { settingsPanel };
    }

    @Override
    public void onTrade(String alias, double price, int size, TradeInfo tradeInfo) {
        if (recordTrades.isEnabled()) {
            try {
                recorder.onTrade(instrumentIds.get(alias), instruments.get(alias).pips * price, size,
                        tradeInfo.isBidAggressor ? 1 : -1, 0);
            } catch (IOException e) {
                throwRuntimeException(e);
            }
        }
    }

    @Override
    public void onDepth(String alias, boolean isBid, int price, int size) {
        try {
            recorder.onDepth(instrumentIds.get(alias), isBid, instruments.get(alias).pips * price, size);
        } catch (IOException e) {
            throwRuntimeException(e);
        }
    }

    @Override
    public void finish() {
        // We could close recorder here if it would be something more complex
    }

    @Override
    public void onInstrumentAdded(String alias, InstrumentInfo instrumentInfo) {
        instruments.put(alias, instrumentInfo);
        int id = (int)(Math.random() * Integer.MAX_VALUE);
        instrumentIds.put(alias, id);
        try {
            recorder.onInstrumentAdded(id, instrumentInfo);
        } catch (IOException e) {
            throwRuntimeException(e);
        }
    }

    private void throwRuntimeException(Throwable e) {
        throw new RuntimeException(e);
    }

    @Override
    public void onInstrumentRemoved(String alias) {
        instruments.remove(alias);
        instrumentIds.remove(alias);
    }

    @Override
    public void onOrderUpdated(OrderInfoUpdate orderInfoUpdate) {
        if (recordOrders.isEnabled()) {
            try {
                recorder.onOrderData(orderInfoUpdate.toString());
            } catch (IOException e) {
                throwRuntimeException(e);
            }
        }
    }

    @Override
    public void onOrderExecuted(ExecutionInfo executionInfo) {
        if (recordOrders.isEnabled()) {
            try {
                recorder.onOrderData(executionInfo.toString());
            } catch (IOException e) {
                throwRuntimeException(e);
            }
        }
    }
}
