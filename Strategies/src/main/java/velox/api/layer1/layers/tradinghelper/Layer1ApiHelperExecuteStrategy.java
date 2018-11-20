package velox.api.layer1.layers.tradinghelper;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.Map;

import javax.swing.JLabel;
import javax.swing.JSpinner;
import javax.swing.event.ChangeListener;

import velox.api.layer1.Layer1ApiProvider;
import velox.api.layer1.annotations.Layer1ApiVersion;
import velox.api.layer1.annotations.Layer1ApiVersionValue;
import velox.api.layer1.annotations.Layer1Attachable;
import velox.api.layer1.annotations.Layer1StrategyName;
import velox.api.layer1.annotations.Layer1TradingStrategy;
import velox.api.layer1.config.beans.HelperExecuteStrategySettings;
import velox.api.layer1.data.OrderMoveParameters;
import velox.api.layer1.layers.utils.OrderBook;
import velox.api.layer1.providers.data.Combination;
import velox.gui.StrategyPanel;

@Layer1Attachable
@Layer1TradingStrategy
@Layer1StrategyName("Execute")
@Layer1ApiVersion(Layer1ApiVersionValue.VERSION2)
public class Layer1ApiHelperExecuteStrategy extends Layer1ApiHelperStrategyAbstract<HelperExecuteStrategySettings> {
    private static final int LEVELS_NUMBER_MIN = 1;
    private static final int LEVELS_NUMBER_MAX = 50;
    private static final int TICKS_NUMBER_MIN = 1;
    private static final int TICKS_NUMBER_MAX = 20;
    private static final int PERCENT_MIN = 1;
    private static final int PERCENT_MAX = 400;
    
    private JSpinner spinnerLevelsNumber;
    private ChangeListener spinnerLevelsNumberListener;
    private JSpinner spinnerTicksNumber;
    private ChangeListener spinnerTicksNumberListener;
    private JSpinner spinnerPercent;
    private ChangeListener spinnerPercentListener;
    
    public Layer1ApiHelperExecuteStrategy(Layer1ApiProvider provider) {
        super(provider, "Execute", "velox.strategy.Execute", HelperExecuteStrategySettings.class);
    }
    
    @Override
    public StrategyPanel[] getCustomGuiFor(String alias, String indicatorName) {
        synchronized (locker) {
            HelperExecuteStrategySettings settings = getSettingsFor(alias);
            
            StrategyPanel panel = new StrategyPanel("Description");
            panel.setLayout(new FlowLayout());
            
            JLabel lbl1 = new JLabel("If the total size of");
            JLabel lbl2 = new JLabel("price levels at my order's side");
            JLabel lbl3 = new JLabel("is more than");
            JLabel lbl3_1 = new JLabel("% of the opposite side");
            JLabel lbl4 = new JLabel("move my order");
            JLabel lbl5 = new JLabel("ticks before the best price (best Bid for a Buy order,");
            JLabel lbl6 = new JLabel("best Ask for a Sell order)");
            
            spinnerLevelsNumber = Utils.createSpinner(settings.getLevelsNumber(), LEVELS_NUMBER_MIN, LEVELS_NUMBER_MAX, 1);
            spinnerTicksNumber = Utils.createSpinner(settings.getTickNumber(), TICKS_NUMBER_MIN, TICKS_NUMBER_MAX, 1);
            spinnerPercent = Utils.createSpinner(settings.getPercent(), PERCENT_MIN, PERCENT_MAX, 1);
            
            reloadGui(alias);
            
            setSpinnerOnlyNumbers(spinnerLevelsNumber);
            setSpinnerOnlyNumbers(spinnerTicksNumber);
            
            panel.add(lbl1);
            panel.add(spinnerLevelsNumber);
            panel.add(lbl2);
            panel.add(lbl3);
            panel.add(spinnerPercent);
            panel.add(lbl3_1);
            panel.add(lbl4);
            panel.add(spinnerTicksNumber);
            panel.add(lbl5);
            panel.add(lbl6);
            
            panel.setMinimumSize(new Dimension(panel.getMinimumSize().width, 155));
            panel.setPreferredSize(new Dimension(panel.getPreferredSize().width, 155));
            
            lastPanels = new StrategyPanel[] {panel, getSettingsPanel(alias, getSettingsFor(alias).isEnabled())};
            return lastPanels;
        }
    }
    
    @Override
    protected void reloadGui(String alias) {
        HelperExecuteStrategySettings settings = getSettingsFor(alias);
        
        spinnerLevelsNumber.removeChangeListener(spinnerLevelsNumberListener);
        spinnerLevelsNumberListener = e -> {
            settings.setLevelsNumber((Integer) spinnerLevelsNumber.getValue());
            settingsChanged(alias, settings);
        };
        spinnerLevelsNumber.addChangeListener(spinnerLevelsNumberListener);
        spinnerLevelsNumber.setValue(Utils.toRange(settings.getLevelsNumber(), LEVELS_NUMBER_MIN, LEVELS_NUMBER_MAX));
        
        spinnerTicksNumber.removeChangeListener(spinnerTicksNumberListener);
        spinnerTicksNumberListener = e -> {
            settings.setTickNumber((Integer) spinnerTicksNumber.getValue());
            settingsChanged(alias, settings);
        };
        spinnerTicksNumber.addChangeListener(spinnerTicksNumberListener);
        spinnerTicksNumber.setValue(Utils.toRange(settings.getTickNumber(), TICKS_NUMBER_MIN, TICKS_NUMBER_MAX));
        
        spinnerPercent.removeChangeListener(spinnerPercentListener);
        spinnerPercentListener = e -> {
            settings.setPercent((Integer) spinnerPercent.getValue());
            settingsChanged(alias, settings);
        };
        spinnerPercent.addChangeListener(spinnerPercentListener);
        spinnerPercent.setValue(Utils.toRange(settings.getPercent(), PERCENT_MIN, PERCENT_MAX));
        
        super.reloadGui(alias);
    }
    
    @Override
    protected void doActionForAlias(String alias) {
        if (!getSettingsFor(alias).isEnabled() || !isWorking) {
            return;
        }
        
        synchronized (locker) {
            OrderBook orderBook = orderBookMap.get(alias);
            Map<String, Combination<Integer, Boolean>> ordersMap = aliasToOrdersMap.get(alias);
            Double pips = pipsMap.get(alias);
            Map<OrderRequest, Long> requestTimesMap = lastRequestMap.get(alias);
            
            if (orderBook == null || ordersMap == null || pips == null || requestTimesMap == null) {
                return;
            }
            
            ArrayList<Combination<String, Double>> pendingMoves = new ArrayList<>();
            
            int levelsNumber = getSettingsFor(alias).getLevelsNumber();
            
            int volumeBid = getTopLevelsSum(orderBook.getBidMap(), levelsNumber, true);
            int volumeAsk = getTopLevelsSum(orderBook.getAskMap(), levelsNumber, false);
            int percent = getSettingsFor(alias).getPercent();
            int distance = getSettingsFor(alias).getTickNumber();
            
            if (!orderBook.getBidMap().isEmpty() && !orderBook.getAskMap().isEmpty()) {
                if (isConditionSatisfiedMore(volumeAsk, volumeBid, percent)) {
                    moveOrders(false, orderBook.getAskMap().firstKey() - distance,ordersMap, pips, requestTimesMap, levelsNumber, pendingMoves);
                }
                
                if (isConditionSatisfiedMore(volumeBid, volumeAsk, percent)){
                    moveOrders(true, orderBook.getBidMap().firstKey() + distance, ordersMap, pips, requestTimesMap, levelsNumber, pendingMoves);
                }
            }
            
            for (Combination<String, Double> pendingMove: pendingMoves) {
                provider.updateOrder(new OrderMoveParameters(pendingMove.first, Double.NaN, pendingMove.second));
            }
        }
    }
    
    private void moveOrders(boolean isBid, int targetLevel, Map<String, Combination<Integer, Boolean>> ordersMap,
            double pips, Map<OrderRequest, Long> requestTimesMap,
            int levelsNumber, ArrayList<Combination<String, Double>> pendingMoves) {
        ordersMap.forEach((orderId, info) -> {
            if (info.second == isBid) {
                OrderRequest orderRequest = new OrderRequest(orderId, targetLevel);
                Long t = requestTimesMap.getOrDefault(orderRequest, 0L);
                
                if (System.currentTimeMillis() - t >= REQUEST_DELAY_MS) {
                    pendingMoves.add(new Combination<String, Double>(orderId, targetLevel * pips));
                    requestTimesMap.put(orderRequest, System.currentTimeMillis());
                }
            }
        });
    }
}
