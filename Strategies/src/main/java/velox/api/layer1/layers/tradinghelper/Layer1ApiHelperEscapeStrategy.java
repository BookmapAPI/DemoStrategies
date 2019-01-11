package velox.api.layer1.layers.tradinghelper;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Map;

import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.event.ChangeListener;

import velox.api.layer1.Layer1ApiProvider;
import velox.api.layer1.annotations.Layer1ApiVersion;
import velox.api.layer1.annotations.Layer1ApiVersionValue;
import velox.api.layer1.annotations.Layer1Attachable;
import velox.api.layer1.annotations.Layer1StrategyName;
import velox.api.layer1.annotations.Layer1TradingStrategy;
import velox.api.layer1.common.Log;
import velox.api.layer1.config.beans.HelperEscapeStrategySettings;
import velox.api.layer1.config.beans.HelperEscapeStrategySettings.Mode;
import velox.api.layer1.data.OrderCancelParameters;
import velox.api.layer1.data.OrderMoveParameters;
import velox.api.layer1.layers.utils.OrderBook;
import velox.api.layer1.providers.data.Combination;
import velox.gui.StrategyPanel;

@Layer1Attachable
@Layer1TradingStrategy
@Layer1StrategyName("Escape")
@Layer1ApiVersion(Layer1ApiVersionValue.VERSION2)
public class Layer1ApiHelperEscapeStrategy extends Layer1ApiHelperStrategyAbstract<HelperEscapeStrategySettings> {
    private static final int MOVE_DISTANCE_MIN = 1;
    private static final int MOVE_DISTANCE_MAX = 20;
    private static final int AFFECTED_LEVELS_NUMBER_MIN = 1;
    private static final int AFFECTED_LEVELS_NUMBER_MAX = 20;
    private static final int CONSIDERED_LEVELS_NUMBER_MIN = 1;
    private static final int CONSIDERED_LEVELS_NUMBER_MAX = 20;
    private static final int PERCENT_MIN = 1;
    private static final int PERCENT_MAX = 400;
    
    private JSpinner spinnerAffectedLevelsNumber;
    private ChangeListener spinnerAffectedLevelsNumberListener;
    private JSpinner spinnerConsideredLevelsNumber;
    private ChangeListener spinnerPercentListener;
    private JSpinner spinnerPercent;
    private ChangeListener spinnerConsideredLevelsNumberListener;
    private JSpinner spinnerMoveDistance;
    private ChangeListener spinnerMoveDistanceListener;
    private JRadioButton rbCancelOrder;
    private ActionListener rbCancelOrderListener;
    private JRadioButton rbMoveOrder;
    private ActionListener rbMoveOrderListener;
    
    private Mode currentMode;
    
    public Layer1ApiHelperEscapeStrategy(Layer1ApiProvider provider) {
        super(provider, "Escape", "velox.strategy.Escape", HelperEscapeStrategySettings.class);
    }
    
    @Override
    public StrategyPanel[] getCustomGuiFor(String alias, String indicatorName) {
        synchronized (locker) {
            HelperEscapeStrategySettings settings = getSettingsFor(alias);
            
            StrategyPanel panel1 = new StrategyPanel("Condition");
            panel1.setLayout(new FlowLayout());
            
            JLabel lbl1 = new JLabel("If my limit order is within");
            JLabel lbl2 = new JLabel("price levels of the best");
            JLabel lbl7 = new JLabel("price");
            JLabel lbl3 = new JLabel("(best Bid for a Buy order, best Ask for a Sell order)");
            JLabel lbl4 = new JLabel("AND the total size of");
            JLabel lbl5 = new JLabel("price levels at my");
            JLabel lbl6 = new JLabel("order's side is less than");
            JLabel lbl8 = new JLabel("% of the");
            JLabel lbl9 = new JLabel("opposite side, then:");
            
            spinnerAffectedLevelsNumber = Utils.createSpinner(settings.getAffectedLevelsNumber(), AFFECTED_LEVELS_NUMBER_MIN, AFFECTED_LEVELS_NUMBER_MAX, 1);
            spinnerConsideredLevelsNumber = Utils.createSpinner(settings.getConsideredLevelsNumber(), CONSIDERED_LEVELS_NUMBER_MIN, CONSIDERED_LEVELS_NUMBER_MAX, 1);
            spinnerPercent = Utils.createSpinner(settings.getPercent(), PERCENT_MIN, PERCENT_MAX, 1);
            
            panel1.add(lbl1);
            panel1.add(spinnerAffectedLevelsNumber);
            panel1.add(lbl2);
            panel1.add(lbl7);
            panel1.add(lbl3);
            panel1.add(lbl4);
            panel1.add(spinnerConsideredLevelsNumber);
            panel1.add(lbl5);
            panel1.add(lbl6);
            panel1.add(spinnerPercent);
            panel1.add(lbl8);
            panel1.add(lbl9);
            
            panel1.setMinimumSize(new Dimension(panel1.getMinimumSize().width, 160));
            panel1.setPreferredSize(new Dimension(panel1.getPreferredSize().width, 160));
            
            StrategyPanel panel2 = new StrategyPanel("Action");
            panel2.setLayout(new GridBagLayout());
            
            ButtonGroup bg = new ButtonGroup();
            GridBagConstraints gbConst;
            
            JLabel lbl22 = new JLabel("ticks");
            spinnerMoveDistance = Utils.createSpinner(settings.getMoveDistance(), MOVE_DISTANCE_MIN, MOVE_DISTANCE_MAX, 1);
            rbCancelOrder = new JRadioButton("Cancel the order");
            rbMoveOrder = new JRadioButton("Move order away from the market by");
            bg.add(rbCancelOrder);
            bg.add(rbMoveOrder);
            
            reloadGui(alias);
            
            currentMode = settings.getMode();
            
            gbConst = new GridBagConstraints();
            gbConst.gridx = 0;
            gbConst.gridy = 0;
            gbConst.gridwidth = 1;
            gbConst.weightx = 0;
            gbConst.fill = GridBagConstraints.HORIZONTAL;
            panel2.add(rbMoveOrder, gbConst);

            gbConst = new GridBagConstraints();
            gbConst.gridx = 1;
            gbConst.gridy = 0;
            gbConst.gridwidth = 1;
            gbConst.weightx = 1;
            gbConst.insets = new Insets(0, 5, 0, 5);
            gbConst.anchor = GridBagConstraints.WEST;
            gbConst.fill = GridBagConstraints.HORIZONTAL;
            panel2.add(spinnerMoveDistance, gbConst);
            
            gbConst = new GridBagConstraints();
            gbConst.gridx = 2;
            gbConst.gridy = 0;
            gbConst.gridwidth = 1;
            gbConst.weightx = 0;
            gbConst.anchor = GridBagConstraints.WEST;
            gbConst.fill = GridBagConstraints.HORIZONTAL;
            panel2.add(lbl22, gbConst);
            
            gbConst = new GridBagConstraints();
            gbConst.gridx = 0;
            gbConst.gridy = 1;
            gbConst.gridwidth = 3;
            gbConst.weightx = 1;
            gbConst.fill = GridBagConstraints.HORIZONTAL;
            panel2.add(rbCancelOrder, gbConst);
            
            lastPanels = new StrategyPanel[] {panel1, panel2, getSettingsPanel(alias, getSettingsFor(alias).isEnabled())};
            return lastPanels;
        }
    }
    
    @Override
    protected void reloadGui(String alias) {
        HelperEscapeStrategySettings settings = getSettingsFor(alias);
        
        rbMoveOrder.removeActionListener(rbMoveOrderListener);
        rbMoveOrderListener = e -> {
            setRbSelections();
            settings.setMode(Mode.MOVE);
            if (Mode.MOVE != currentMode) {
                currentMode = settings.getMode();
                lastRequestMap.forEach((currentAlias, map) -> map.clear());
            }
            settingsChanged(alias, settings);
        };
        rbMoveOrder.addActionListener(rbMoveOrderListener);
        rbMoveOrder.setSelected(settings.getMode() == Mode.MOVE);
        
        rbCancelOrder.removeActionListener(rbCancelOrderListener);
        rbCancelOrderListener = e -> {
            setRbSelections();
            settings.setMode(Mode.CANCEL);
            if (Mode.CANCEL != currentMode) {
                currentMode = settings.getMode();
                lastRequestMap.forEach((currentAlias, map) -> map.clear());
            }
            settingsChanged(alias, settings);
        };
        rbCancelOrder.addActionListener(rbCancelOrderListener);
        rbCancelOrder.setSelected(settings.getMode() == Mode.CANCEL);
        
        spinnerMoveDistance.removeChangeListener(spinnerMoveDistanceListener);
        spinnerMoveDistanceListener = e -> {
            settings.setMoveDistance((Integer) spinnerMoveDistance.getValue());
            settingsChanged(alias, settings);
        };
        spinnerMoveDistance.addChangeListener(spinnerMoveDistanceListener);
        spinnerMoveDistance.setValue(Utils.toRange(settings.getMoveDistance(), MOVE_DISTANCE_MIN, MOVE_DISTANCE_MAX));
        
        spinnerAffectedLevelsNumber.removeChangeListener(spinnerAffectedLevelsNumberListener);
        spinnerAffectedLevelsNumberListener = e -> {
            settings.setAffectedLevelsNumber((Integer) spinnerAffectedLevelsNumber.getValue());
            settingsChanged(alias, settings);
        };
        spinnerAffectedLevelsNumber.addChangeListener(spinnerAffectedLevelsNumberListener);
        spinnerAffectedLevelsNumber.setValue(Utils.toRange(settings.getAffectedLevelsNumber(), AFFECTED_LEVELS_NUMBER_MIN, AFFECTED_LEVELS_NUMBER_MAX));
        
        spinnerConsideredLevelsNumber.removeChangeListener(spinnerConsideredLevelsNumberListener);
        spinnerConsideredLevelsNumberListener = e -> {
            settings.setConsideredLevelsNumber((Integer) spinnerConsideredLevelsNumber.getValue());
            settingsChanged(alias, settings);
        };
        spinnerConsideredLevelsNumber.addChangeListener(spinnerConsideredLevelsNumberListener);
        spinnerConsideredLevelsNumber.setValue(Utils.toRange(settings.getConsideredLevelsNumber(), CONSIDERED_LEVELS_NUMBER_MIN, CONSIDERED_LEVELS_NUMBER_MAX));
        
        spinnerPercent.removeChangeListener(spinnerPercentListener);
        spinnerPercentListener = e -> {
            settings.setPercent((Integer) spinnerPercent.getValue());
            settingsChanged(alias, settings);
        };
        spinnerPercent.addChangeListener(spinnerPercentListener);
        spinnerPercent.setValue(Utils.toRange(settings.getPercent(), PERCENT_MIN, PERCENT_MAX));
        
        setRbSelections();
        
        super.reloadGui(alias);
    }
    
    private void setRbSelections() {
        spinnerMoveDistance.setEnabled(rbMoveOrder.isSelected());
    }
    
    @Override
    protected void doActionForAlias(String alias) {
        if (!getSettingsFor(alias).isEnabled() || !isWorking) {
            return;
        }
        
        synchronized (locker) {
            OrderBook orderBook = orderBookMap.get(alias);
            
            if (orderBook == null) {
                return;
            }
            
            int levelsNumber = getSettingsFor(alias).getConsideredLevelsNumber();
            int affectedLevelsNumber = getSettingsFor(alias).getAffectedLevelsNumber();
            int percent = getSettingsFor(alias).getPercent();
            
            int volumeBid = getTopLevelsSum(orderBook.getBidMap(), levelsNumber, true);
            int volumeAsk = getTopLevelsSum(orderBook.getAskMap(), levelsNumber, false);
            
            if (!orderBook.getBidMap().isEmpty() && !orderBook.getAskMap().isEmpty()) {
                int firstBidLevel = orderBook.getBidMap().firstKey();
                int firstAskLevel = orderBook.getAskMap().firstKey();
                
                if (isConditionSatisfiedLess(volumeBid, volumeAsk, percent)) {
                    executeAction(alias, true, firstBidLevel - affectedLevelsNumber + 1, firstAskLevel);
                }
                
                if (isConditionSatisfiedLess(volumeAsk, volumeBid, percent)) {
                    executeAction(alias, false, firstAskLevel + affectedLevelsNumber - 1, firstBidLevel);
                }
            }
        }
    }
    
    /**
     * Move orders with prices in range [-inf, borderLevel] or [borderLevel, +inf] (defined by isBid) by offset in moveDirection
     * @param alias
     * @param isBid if true, execute condition for buy orders, otherwise for sell orders
     * @param borderLevel
     * @param oppositeStartLevel level where opposite trades start
     * @param offset
     * @param moveDirection
     */
    private void executeAction(String alias, boolean isBid, final int borderLevel, final int oppositeStartLevel) {
        synchronized (locker) {
            ArrayList<Combination<String, Integer>> affectedOrders = new ArrayList<>();
            
            Map<String, Combination<Integer, Boolean>> ordersMap = aliasToOrdersMap.get(alias);
            Double pips = pipsMap.get(alias);
            Map<OrderRequest, Long> requestTimesMap = lastRequestMap.get(alias);
            
            if (ordersMap == null || pips == null || requestTimesMap == null) {
                Log.warn("Helper strategy: unknown instrument: " + alias);
                return;
            }
            
            ordersMap.forEach((orderId, info) -> {
                int level = info.first;
                if (info.second == isBid && isPriceInRange(level, isBid, borderLevel)) {
                    affectedOrders.add(new Combination<String, Integer>(orderId, level));
                }
            });
            
            int moveDelta = getSettingsFor(alias).getMoveDistance();
            Mode mode = getSettingsFor(alias).getMode();
            
            if (isBid) {
                moveDelta *= -1;
            }
            
            for (Combination<String, Integer> pair: affectedOrders) {
                switch (mode) {
                case CANCEL: {
                    OrderRequest orderRequest = new OrderRequest(pair.first, pair.second);
                    Long t = requestTimesMap.getOrDefault(orderRequest, 0L);
                    
                    if (System.currentTimeMillis() - t >= REQUEST_DELAY_MS) {
                        provider.updateOrder(new OrderCancelParameters(pair.first));
                        requestTimesMap.put(orderRequest, System.currentTimeMillis());
                    }
                    
                    break;
                } case MOVE: {
                    //avoid multiple moves, move 1 time [minLevel, maxLevel]
                    int k = 1;
                    while (isPriceInRange(pair.second + moveDelta * k, isBid, borderLevel) &&
                            !isPriceinOppositeRange(pair.second + moveDelta * k, !isBid, oppositeStartLevel)) {
                        k++;
                    }
                    
                    OrderRequest orderRequest = new OrderRequest(pair.first, pair.second + k * moveDelta);
                    Long t = requestTimesMap.getOrDefault(orderRequest, 0L);
                    
                    if (System.currentTimeMillis() - t >= REQUEST_DELAY_MS) {
                        provider.updateOrder(new OrderMoveParameters(pair.first, Double.NaN, (pair.second + k * moveDelta) * pips));
                        requestTimesMap.put(orderRequest, System.currentTimeMillis());
                    }
                    
                    break;
                } default:
                    throw new IllegalArgumentException("Helper strategy: unknown mode: " + mode);
                }
                
            }
        }
    }

    private boolean isPriceInRange(int price, boolean isBid, int borderLevel) {
        if (isBid) {
            return price >= borderLevel;
        } else {
            return price <= borderLevel;
        }
    }
    
    private boolean isPriceinOppositeRange(int price, boolean isBid, int startLevel) {
        if (!isBid) {
            return price >= startLevel;
        } else {
            return price <= startLevel;
        }
    }
}
