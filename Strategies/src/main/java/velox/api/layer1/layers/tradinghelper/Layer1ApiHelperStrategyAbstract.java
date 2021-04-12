package velox.api.layer1.layers.tradinghelper;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.JCheckBox;
import javax.swing.JFormattedTextField;
import javax.swing.JSpinner;
import javax.swing.text.NumberFormatter;

import java.util.TreeMap;

import velox.api.layer1.Layer1ApiProvider;
import velox.api.layer1.Layer1ApiTradingAdapter;
import velox.api.layer1.Layer1ApiAdminAdapter;
import velox.api.layer1.Layer1ApiDataAdapter;
import velox.api.layer1.Layer1ApiFinishable;
import velox.api.layer1.Layer1ApiInstrumentAdapter;
import velox.api.layer1.Layer1CustomPanelsGetter;
import velox.api.layer1.Layer1Descripted;
import velox.api.layer1.Layer1InternallyControllingEnable;
import velox.api.layer1.annotations.Layer1TradingStrategy;
import velox.api.layer1.common.ListenableHelper;
import velox.api.layer1.common.Log;
import velox.api.layer1.config.beans.HelperStrategySettings;
import velox.api.layer1.data.InstrumentInfo;
import velox.api.layer1.data.OrderInfoUpdate;
import velox.api.layer1.data.OrderStatus;
import velox.api.layer1.data.OrderType;
import velox.api.layer1.layers.utils.OrderBook;
import velox.api.layer1.messages.UserMessageLayersChainCreatedTargeted;
import velox.api.layer1.messages.UserMessageRewindBase;
import velox.api.layer1.messages.indicators.Layer1ApiUserMessageModifyIndicator;
import velox.api.layer1.messages.indicators.SettingsAccess;
import velox.api.layer1.messages.indicators.Layer1ApiUserMessageModifyIndicator.GraphType;
import velox.api.layer1.providers.data.Combination;
import velox.api.layer1.settings.Layer1ConfigSettingsInterface;
import velox.gui.StrategyPanel;

@Layer1TradingStrategy
/**
 * @param <V> settings class
 */
public class Layer1ApiHelperStrategyAbstract<V extends HelperStrategySettings> implements Layer1ApiFinishable, Layer1CustomPanelsGetter,
    Layer1ApiTradingAdapter, Layer1ApiDataAdapter, Layer1ApiInstrumentAdapter, Layer1ApiAdminAdapter,
    Layer1ConfigSettingsInterface, Layer1Descripted, Layer1InternallyControllingEnable {
    
    /**
     * Time that has to pass between 2 requests (cancel / move to level) can be made for same level (to avoid cancel or move requests spam in real trading)
     */
    protected static final long REQUEST_DELAY_MS = 1000;
    
    protected final String userReadableStrategyName;
    protected final String strategyName;
    
    protected Object locker = new Object();
    
    private Map<String, V> settingsMap = new HashMap<>();
    
    protected StrategyPanel[] lastPanels;
    
    protected Map<String, OrderBook> orderBookMap = new HashMap<>(); //alias - order book
    protected Map<String, Map<String, Combination<Integer, Boolean>>> aliasToOrdersMap = new HashMap<>(); //alias - (orderId - <price, isBid>)
    protected Map<String, Double> pipsMap = new HashMap<>();
    protected Map<String, Map<OrderRequest, Long>> lastRequestMap = new HashMap<>(); //alias - mapping <(order id, level number) - last time request on that level was made>
    
    protected SettingsAccess settingsAccess;
    
    protected final Layer1ApiProvider provider;
    
    private final Class<?> settingsClass;
    
    protected volatile boolean isWorking = false;
    
    private InvalidateIsEnabledCallback invalidateIsEnabledCallback;
    
    public Layer1ApiHelperStrategyAbstract(Layer1ApiProvider provider, String userReadableStrategyName, String strategyName, Class<?> settingsClass) {
        this.provider = provider;
        this.settingsClass = settingsClass;
        
        ListenableHelper.addListeners(provider, this);
        
        this.userReadableStrategyName = userReadableStrategyName;
        this.strategyName = strategyName;
    }
    
    protected void settingsChanged(String settingsAlias, HelperStrategySettings settingsObject) {
        synchronized (locker) {
            V instrumentSettings = getSettingsFor(settingsAlias);
            
            settingsAccess.setSettings(settingsAlias, strategyName, settingsObject, settingsObject.getClass());
            
            for (String alias: orderBookMap.keySet()) {
                doActionForAlias(alias);
            }
        }
    }
    
    protected void doActionForAlias(String alias) {
    }
    
    @Override
    public StrategyPanel[] getCustomGuiFor(String alias, String indicatorName) {
        return new StrategyPanel[0];
    }

    @Override
    public void finish() {
        isWorking = false;
        onUserMessage(new Layer1ApiUserMessageModifyIndicator(Layer1ApiHelperStrategyAbstract.class, userReadableStrategyName, false));
        synchronized (locker) {
            orderBookMap.clear();
            aliasToOrdersMap.clear();
            lastRequestMap.clear();
        }
    }
    
    @Override
    public void onInstrumentAdded(String alias, InstrumentInfo instrumentInfo) {
        synchronized (locker) {
            aliasToOrdersMap.put(alias, new HashMap<>());
            orderBookMap.put(alias, new OrderBook());
            pipsMap.put(alias, instrumentInfo.pips);
            lastRequestMap.put(alias, new TreeMap<>());
        }
    }
    
    @Override
    public void onInstrumentRemoved(String alias) {
        synchronized (locker) {
            orderBookMap.remove(alias);
            lastRequestMap.remove(alias);
        }
    }
    
    @Override
    public void onDepth(String alias, boolean isBid, int price, int size) {
        synchronized (locker) {
            OrderBook orderBook = orderBookMap.get(alias);
            if (orderBook != null) {
                orderBook.onUpdate(isBid, price, size);
                doActionForAlias(alias);
            } else {
                Log.warn("Helper strategy: unknown instrument " + alias);
            }
        }
    }
    
    @Override
    public void onUserMessage(Object data) {
        if (data instanceof UserMessageLayersChainCreatedTargeted) {
            UserMessageLayersChainCreatedTargeted message = (UserMessageLayersChainCreatedTargeted) data;
            if (message.targetClass == getClass()) {
                isWorking = true;
                onUserMessage(new Layer1ApiUserMessageModifyIndicator(Layer1ApiHelperStrategyAbstract.class, userReadableStrategyName, true,
                        null, null, null, null, null, null, null, null, null, null, GraphType.NONE, false, null, null, null, null));
            }
        } else if (data instanceof UserMessageRewindBase) {
            UserMessageRewindBase message = (UserMessageRewindBase) data;
            
            synchronized (locker) {
                for (Entry<String, OrderBook> entry: message.aliasToOrderBooksMap.entrySet()) {
                    OrderBook orderBook = orderBookMap.get(entry.getKey());
                    
                    if (orderBook != null) {
                        orderBookMap.put(entry.getKey(), new OrderBook(entry.getValue()));
                    }
                }
                
                aliasToOrdersMap.forEach((alias, orderMap) -> orderMap.clear());
            }
        }
    }

    @Override
    public void onOrderUpdated(OrderInfoUpdate orderInfoUpdate) {
        synchronized (locker) {
            Map<String, Combination<Integer, Boolean>> ordersMap = aliasToOrdersMap.get(orderInfoUpdate.instrumentAlias);
            if (ordersMap == null) {
                ordersMap = new HashMap<>();
                aliasToOrdersMap.put(orderInfoUpdate.instrumentAlias, ordersMap);
            }
            if (orderInfoUpdate.type == OrderType.LMT) {
                if (orderInfoUpdate.unfilled == 0 || !(orderInfoUpdate.status == OrderStatus.WORKING)) {
                    ordersMap.remove(orderInfoUpdate.orderId);
                } else {
                    Double pips = pipsMap.get(orderInfoUpdate.instrumentAlias);
                    if (pips == null) {
                        Log.warn("Helper strategy: unknown instrument: " + orderInfoUpdate.instrumentAlias);
                    } else {
                        ordersMap.put(orderInfoUpdate.orderId, new Combination<Integer, Boolean>((int) Math.round(orderInfoUpdate.limitPrice / pips), orderInfoUpdate.isBuy));
                    }
                }
            }
            
            doActionForAlias(orderInfoUpdate.instrumentAlias);
        }
    }
    
    protected void setSpinnerOnlyNumbers(JSpinner spinner) {
        JFormattedTextField tfield = ((JSpinner.NumberEditor) spinner.getEditor()).getTextField();
        ((NumberFormatter) tfield.getFormatter()).setAllowsInvalid(false);
    }
    
    protected int getTopLevelsSum(TreeMap<Integer, Long> map, int levelsNumber, boolean isBid) {
        if (map.isEmpty()) {
            return 0;
        }
        
        int result = 0;
        
        int topPrice = map.firstKey();
        for (int i = 0; i < levelsNumber; i++) {
            int price = topPrice + i * (isBid ? -1 : 1);
            result += map.getOrDefault(price, 0L);
        }
                
        return result;
    }
    
    protected boolean isConditionSatisfiedLess(int volumeOur, int volumeTheir, double percent) {
        if (volumeOur == 0 && volumeTheir == 0) {
            return false;
        }
        
        percent /= 100.;
        
        return volumeOur < ((double) volumeTheir) * percent;
    }
    
    protected boolean isConditionSatisfiedMore(int volumeOur, int volumeTheir, double percent) {
        if (volumeOur == 0 && volumeTheir == 0) {
            return false;
        }
        
        percent /= 100.;
        
        return volumeOur > ((double) volumeTheir) * percent;
    }
    
    protected StrategyPanel getSettingsPanel(String alias, boolean isSelected) {
        StrategyPanel panel = new StrategyPanel("Enable");
        
        panel.setLayout(new GridBagLayout());
        
        JCheckBox cbUseInstrumentSettings = new JCheckBox("Enable for " + alias);
        cbUseInstrumentSettings.setSelected(isSelected);
        cbUseInstrumentSettings.addActionListener(e -> onCbUseInstrumentSettingsClicked(alias, cbUseInstrumentSettings.isSelected()));
        
        GridBagConstraints gbConst = new GridBagConstraints();
        gbConst.gridx = 0;
        gbConst.gridy = 0;
        gbConst.weightx = 1;
        gbConst.anchor = GridBagConstraints.WEST;
        gbConst.insets = new Insets(5, 5, 5, 5);
        panel.add(cbUseInstrumentSettings, gbConst);
        
        return panel;
    }
    
    protected void onCbUseInstrumentSettingsClicked(String alias, boolean isSelected) {
        V settings = getSettingsFor(alias);
        settings.setEnabled(isSelected);
        settingsAccess.setSettings(alias, strategyName, settings, settingsClass);
        
        doActionForAlias(alias);
        
        if (invalidateIsEnabledCallback != null) {
            invalidateIsEnabledCallback.invalidate();
        }
    }
    
    protected void reloadGui(String alias) {
        if (lastPanels == null) {
            return;
        }
        
        for (StrategyPanel panel: lastPanels) {
            panel.invalidate();
            panel.repaint();
        }
    }

    @Override
    public void acceptSettingsInterface(SettingsAccess settingsAccess) {
        this.settingsAccess = settingsAccess;
    }
    
    @SuppressWarnings("unchecked")
    protected V getSettingsFor(String alias) {
        synchronized (locker) {
            V settings = settingsMap.get(alias);
            if (settings == null) {
                settings = (V) settingsAccess.getSettings(alias, strategyName, settingsClass);
                settingsMap.put(alias, settings);
            }
            return settings;
        }
    }

    @Override
    public boolean isEnabledFor(String alias) {
        HelperStrategySettings settings = getSettingsFor(alias);
        
        return settings.isEnabled();
    }

    @Override
    public void setInvalidateIsEnabledCallback(InvalidateIsEnabledCallback invalidateIsEnabledCallback) {
        this.invalidateIsEnabledCallback = invalidateIsEnabledCallback;
    }
    
    @Override
    public String getDescription() {
        return "<a href='https://www.bookmap.com/api/v1/redirect/strategies' style=\"color: #FFFFFF\">Click here</a> to learn more on our website";
    }
}
