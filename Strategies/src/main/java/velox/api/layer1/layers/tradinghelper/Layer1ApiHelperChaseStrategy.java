package velox.api.layer1.layers.tradinghelper;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeListener;

import velox.api.layer1.Layer1ApiProvider;
import velox.api.layer1.annotations.Layer1ApiVersion;
import velox.api.layer1.annotations.Layer1ApiVersionValue;
import velox.api.layer1.annotations.Layer1Attachable;
import velox.api.layer1.annotations.Layer1StrategyName;
import velox.api.layer1.annotations.Layer1TradingStrategy;
import velox.api.layer1.config.beans.HelperChaseStrategySettings;
import velox.api.layer1.data.InstrumentInfo;
import velox.api.layer1.data.OrderMoveParameters;
import velox.api.layer1.data.TradeInfo;
import velox.api.layer1.layers.utils.OrderBook;
import velox.api.layer1.messages.UserMessageRewindBase;
import velox.api.layer1.messages.indicators.SettingsAccess;
import velox.api.layer1.providers.data.Combination;
import velox.gui.StrategyPanel;

@Layer1Attachable
@Layer1TradingStrategy
@Layer1StrategyName("Chase")
@Layer1ApiVersion(Layer1ApiVersionValue.VERSION2)
public class Layer1ApiHelperChaseStrategy extends Layer1ApiHelperStrategyAbstract<HelperChaseStrategySettings> {
    private static final int DISTANCE_MIN = 0;
    private static final int DISTANCE_MAX = 99;
    
    private JSpinner spinnerDistance;
    private ChangeListener spinnerDistanceListener;
    
    private JRadioButton rbChaseBest;
    private JRadioButton rbChaseLast;
    
    private Map<String, HelperChaseStrategySettings> settingsMap = new HashMap<>();
    
    private Map<String, Double> lastPriceMap = new HashMap<>();
    
    public Layer1ApiHelperChaseStrategy(Layer1ApiProvider provider) {
        super(provider, "Chase", "velox.strategy.Chase", HelperChaseStrategySettings.class);
    }
    
    @Override
    public StrategyPanel[] getCustomGuiFor(String alias, String indicatorName) {
        synchronized (locker) {
            HelperChaseStrategySettings settings = getSettingsFor(alias);
            
            StrategyPanel panel = new StrategyPanel("Description");
            
            panel.setLayout(new FlowLayout());
            JLabel lbl1 = new JLabel("Keep my limit order at a distance not larger than");
            spinnerDistance = new JSpinner(new SpinnerNumberModel(
                    Utils.toRange(settings.getDistance(), DISTANCE_MIN, DISTANCE_MAX), DISTANCE_MIN, DISTANCE_MAX, 1));
            JLabel lbl2 = new JLabel("levels from");
            ButtonGroup buttonGroup = new ButtonGroup();
            rbChaseBest = new JRadioButton("best price");
            rbChaseBest.setToolTipText("(best Bid for a Buy order,best Ask for a Sell order)");
            rbChaseLast = new JRadioButton("last price");
            buttonGroup.add(rbChaseBest);
            buttonGroup.add(rbChaseLast);

            reloadGui(alias);
            
            setSpinnerOnlyNumbers(spinnerDistance);
            
            panel.add(lbl1);
            panel.add(spinnerDistance);
            panel.add(lbl2);
            panel.add(rbChaseBest);
            panel.add(rbChaseLast);
            
            panel.setMinimumSize(new Dimension(panel.getMinimumSize().width, 100));
            panel.setPreferredSize(new Dimension(panel.getPreferredSize().width, 100));
            
            lastPanels = new StrategyPanel[] {panel, getSettingsPanel(alias, getSettingsFor(alias).isEnabled())};
            return lastPanels;
        }
    }
    
    @Override
    protected void reloadGui(String alias) {
        HelperChaseStrategySettings settings = getSettingsFor(alias);
        
        spinnerDistance.removeChangeListener(spinnerDistanceListener);
        spinnerDistance.setValue(Utils.toRange(settings.getDistance(), DISTANCE_MIN, DISTANCE_MAX));
        spinnerDistanceListener = e -> {
            settings.setDistance((Integer) spinnerDistance.getValue());
            settingsChanged(alias, settings);
        };
        spinnerDistance.addChangeListener(spinnerDistanceListener);
        
        rbChaseBest.addActionListener(e -> {
            settings.setMode(HelperChaseStrategySettings.ChaseMode.BEST_PRICE);
            settingsChanged(alias, settings);
        });
        rbChaseBest.setSelected(settings.getMode() == HelperChaseStrategySettings.ChaseMode.BEST_PRICE);
        
        rbChaseLast.addActionListener(e -> {
            settings.setMode(HelperChaseStrategySettings.ChaseMode.LAST_PRICE);
            settingsChanged(alias, settings);
        });
        rbChaseLast.setSelected(settings.getMode() == HelperChaseStrategySettings.ChaseMode.LAST_PRICE);
        
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
            
            int distance = getSettingsFor(alias).getDistance();
            
            if (!orderBook.getBidMap().isEmpty() && !orderBook.getAskMap().isEmpty()) {
                int chaseBidLevel = 0;
                int chaseAskLevel = 0;
                switch (getSettingsFor(alias).getMode()) {
                case BEST_PRICE:
                    chaseBidLevel = orderBook.getBidMap().firstKey();
                    chaseAskLevel = orderBook.getAskMap().firstKey();
                    break;
                case LAST_PRICE:
                    if (!lastPriceMap.containsKey(alias)) {
                        chaseBidLevel = orderBook.getBidMap().firstKey();
                        chaseAskLevel = orderBook.getAskMap().firstKey();
                    } else {
                        chaseBidLevel = chaseAskLevel = (int) Math.round(lastPriceMap.get(alias));
                    }
                    break;
                default:
                    throw new IllegalArgumentException("Unknown chase mode: " + getSettingsFor(alias).getMode());
                }
                
                ArrayList<Combination<String, Double>> pendingMoves = new ArrayList<>();
                final int chaseBid = chaseBidLevel;
                final int chaseAsk = chaseAskLevel;
                ordersMap.forEach((orderId, info) -> {
                    int level = info.first;
                    Integer targetLevel = null;
                    if (info.second) { //bid
                        if (chaseBid - level > distance) {
                            targetLevel = chaseBid - distance;
                        }
                    } else { //ask
                        if (level - chaseAsk > distance) {
                            targetLevel = chaseAsk + distance;
                        }
                    }
                    
                    if (targetLevel != null) {
                        OrderRequest orderRequest = new OrderRequest(orderId, targetLevel);
                        Long t = requestTimesMap.getOrDefault(orderRequest, 0L);
                        
                        if (System.currentTimeMillis() - t >= REQUEST_DELAY_MS) {
                            pendingMoves.add(new Combination<String, Double>(orderId, targetLevel * pips));
                            requestTimesMap.put(orderRequest, System.currentTimeMillis());
                        }
                    }
                });
                
                for (Combination<String, Double> pendingMove: pendingMoves) {
                    provider.updateOrder(new OrderMoveParameters(pendingMove.first, Double.NaN, pendingMove.second));
                }
            }
        }
    }
    
    @Override
    public void onTrade(String alias, double price, int size, TradeInfo tradeInfo) {
        super.onTrade(alias, price, size, tradeInfo);
        
        synchronized (locker) {
            if (size > 0) {
                lastPriceMap.put(alias, price);
                doActionForAlias(alias);
            }
        }
    }
    
    @Override
    public void onInstrumentAdded(String alias, InstrumentInfo instrumentInfo) {
        super.onInstrumentAdded(alias, instrumentInfo);
        synchronized (locker) {
            settingsMap.put(alias, (HelperChaseStrategySettings) settingsAccess.getSettings(alias, strategyName, HelperChaseStrategySettings.class));
        }
    }
    
    @Override
    public void acceptSettingsInterface(SettingsAccess settingsAccess) {
        super.acceptSettingsInterface(settingsAccess);
        synchronized (locker) {
            settingsMap.put(null, (HelperChaseStrategySettings) settingsAccess.getSettings(null, strategyName, HelperChaseStrategySettings.class));
        }
    }
    
    @Override
    public void onUserMessage(Object data) {
        super.onUserMessage(data);
        if (data instanceof UserMessageRewindBase) {
            synchronized (locker) {
                lastPriceMap.clear();
            }
        }
    }
}
