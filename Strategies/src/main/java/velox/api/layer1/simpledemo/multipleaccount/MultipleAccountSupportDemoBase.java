package velox.api.layer1.simpledemo.multipleaccount;

import velox.api.layer1.Layer1ApiAdminAdapter;
import velox.api.layer1.Layer1ApiFinishable;
import velox.api.layer1.Layer1ApiInstrumentListener;
import velox.api.layer1.Layer1ApiProvider;
import velox.api.layer1.Layer1ApiTradingListener;
import velox.api.layer1.common.ListenableHelper;
import velox.api.layer1.common.Log;
import velox.api.layer1.common.helper.AccountListManager;
import velox.api.layer1.data.AccountInfo;
import velox.api.layer1.data.AccountInfoBuilder;
import velox.api.layer1.data.BalanceInfo;
import velox.api.layer1.data.ExecutionInfo;
import velox.api.layer1.data.InstrumentInfo;
import velox.api.layer1.data.Layer1ApiProviderSupportedFeatures;
import velox.api.layer1.data.OrderInfo;
import velox.api.layer1.data.OrderInfoUpdate;
import velox.api.layer1.data.SimpleOrderSendParametersBuilder;
import velox.api.layer1.data.StatusInfo;
import velox.api.layer1.layers.strategies.interfaces.CalculatedResultListener;
import velox.api.layer1.layers.strategies.interfaces.CustomGeneratedEvent;
import velox.api.layer1.layers.strategies.interfaces.CustomGeneratedEventAliased;
import velox.api.layer1.layers.strategies.interfaces.InvalidateInterface;
import velox.api.layer1.layers.strategies.interfaces.OnlineCalculatable;
import velox.api.layer1.layers.strategies.interfaces.OnlineValueCalculatorAdapter;
import velox.api.layer1.messages.GeneratedEventInfo;
import velox.api.layer1.messages.Layer1ApiUserMessageAddStrategyUpdateGenerator;
import velox.api.layer1.messages.TradingAccountsInfoMessage;
import velox.api.layer1.messages.UserMessageLayersChainCreatedTargeted;
import velox.api.layer1.messages.indicators.IndicatorColorScheme;
import velox.api.layer1.messages.indicators.IndicatorLineStyle;
import velox.api.layer1.messages.indicators.Layer1ApiUserMessageModifyIndicator;
import velox.api.layer1.messages.indicators.StrategyUpdateGenerator;
import velox.api.layer1.providers.helper.TargetedRequestHelper;
import velox.gui.BookmapScrollPane;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.table.DefaultTableModel;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;


/**
 * Base class for strategies to demonstrate multiple account support.
 * See {@link NoMultipleAccountSupportDemo} for a strategy without multiple account support.
 * See {@link MultipleAccountSupportDemo} for a strategy with multiple account support.
 */
public class MultipleAccountSupportDemoBase implements
        Layer1ApiTradingListener,
        Layer1ApiFinishable,
        Layer1ApiAdminAdapter,
        Layer1ApiInstrumentListener,
        OnlineCalculatable {
    
    private static final int MARKET_ORDER_SIZE = 1;
    
    private final AccountListManager accountListManager = new AccountListManager();
    
    private record AccountItem(AccountInfo accountInfo) {
        @Override
        public String toString() {
            String text = accountInfo.summary;
            if (accountInfo.isPrimary) {
                text += " (Primary)";
            }
            return text;
        }
    }
    
    /**
     * Order ID -> order info
     */
    private final HashMap<String, OrderInfo> orders = new HashMap<>();
    
    private final JFrame strategyPanel = new JFrame(this.getClass().getSimpleName() + ": Trading Demo Dialog");
    
    private final JComboBox<String> comboBoxAliases = new JComboBox<>();
    private final JComboBox<AccountItem> comboBoxAccounts = new JComboBox<>();
    private final JButton buttonSendMarketBuyOrder = new JButton("Market Buy (size = " + MARKET_ORDER_SIZE + ")");
    private final JButton buttonSendMarketSellOrder = new JButton("Market Sell (size = " + MARKET_ORDER_SIZE + ")");
    private final JButton buttonClearOrdersTable = new JButton("Clear orders table");
    
    private final DefaultTableModel ordersTableModel = new DefaultTableModel(new Object[][] {}, new String[] {
            "Order ID", "Instrument", "Account", "Buy/Sell", "Size (filled/total)", "Status", "Type"
    }) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    
    private static class SampleEvent implements CustomGeneratedEvent {
        
        @Override
        public long getTime() {
            return 0;
        }
        
        @Override
        public Object clone() {
            return null;
        }
    }
    
    private final String providerName = this.getClass().getName() + "Sample";
    
    private final String treeName = providerName + "_Tree";
    private final String indicatorName = providerName + "_Indicator";
    
    private final Layer1ApiProvider provider;
    
    private final Map<String, String> indicatorsFullNameToUserName = new HashMap<>();
    
    public MultipleAccountSupportDemoBase(Layer1ApiProvider provider) {
        this.provider = provider;
        
        ListenableHelper.addListeners(provider, this);
        
        setupGui();
    }
    
    private void setupGui() {
        // Layout:
        GridBagLayout gridBagLayout = new GridBagLayout();
        gridBagLayout.columnWidths = new int[] {150, 0};
        gridBagLayout.rowHeights = new int[] {50, 50, 50, 50, 30, 250, 50};
        gridBagLayout.columnWeights = new double[] {Double.MIN_VALUE, 1.0};
        gridBagLayout.rowWeights = new double[] {0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0};
        strategyPanel.setLayout(gridBagLayout);
        
        strategyPanel.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        
        // Instruments combo box:
        JLabel labelOrderInstrument = new JLabel("Instrument for order:");
        labelOrderInstrument.setToolTipText("Instrument name to be used to send market order");
        labelOrderInstrument.setHorizontalAlignment(SwingConstants.CENTER);
        GridBagConstraints gbcLabelOrderInstrument = new GridBagConstraints();
        gbcLabelOrderInstrument.anchor = GridBagConstraints.EAST;
        gbcLabelOrderInstrument.insets = new Insets(5, 5, 5, 5);
        gbcLabelOrderInstrument.gridx = 0;
        gbcLabelOrderInstrument.gridy = 0;
        strategyPanel.add(labelOrderInstrument, gbcLabelOrderInstrument);
        
        GridBagConstraints gbcComboBoxAliases = new GridBagConstraints();
        gbcComboBoxAliases.insets = new Insets(5, 0, 5, 0);
        gbcComboBoxAliases.fill = GridBagConstraints.HORIZONTAL;
        gbcComboBoxAliases.gridx = 1;
        gbcComboBoxAliases.gridy = 0;
        strategyPanel.add(comboBoxAliases, gbcComboBoxAliases);
        
        comboBoxAliases.addItem(null);
        
        comboBoxAliases.addActionListener(e -> {
            String selectedAlias = (String) comboBoxAliases.getSelectedItem();
            boolean hasSelectedAlias = selectedAlias != null;
            comboBoxAccounts.setEnabled(hasSelectedAlias);
            
            if (hasSelectedAlias) {
                Set<AccountInfo> accountsByAlias = accountListManager.getAccountsByAlias(selectedAlias);
                comboBoxAccounts.removeAllItems();
                if (accountsByAlias.isEmpty()) {
                    // if provider does not support multiple accounts, add a default account with id null
                    comboBoxAccounts.addItem(new AccountItem(
                            new AccountInfoBuilder(null, "Default account (accountId=null)", false)
                                    .build()
                    ));
                } else {
                    accountsByAlias.forEach(account -> comboBoxAccounts.addItem(new AccountItem(account)));
                }
            }
            
            comboBoxAccounts.setSelectedItem(null);
        });
        
        comboBoxAliases.setSelectedItem(null);
        
        // Accounts combo box:
        JLabel labelOrderAccount = new JLabel("Account for order:");
        labelOrderAccount.setToolTipText("Account to be used to send market order");
        labelOrderAccount.setHorizontalAlignment(SwingConstants.CENTER);
        GridBagConstraints gbcLabelOrderAccount = new GridBagConstraints();
        gbcLabelOrderAccount.anchor = GridBagConstraints.EAST;
        gbcLabelOrderAccount.insets = new Insets(0, 5, 5, 5);
        gbcLabelOrderAccount.gridx = 0;
        gbcLabelOrderAccount.gridy = 1;
        strategyPanel.add(labelOrderAccount, gbcLabelOrderAccount);
        
        comboBoxAccounts.setEnabled(false);
        comboBoxAccounts.addActionListener(e -> {
            boolean hasSelectedAccount = comboBoxAccounts.getSelectedItem() != null;
            buttonSendMarketBuyOrder.setEnabled(hasSelectedAccount);
            buttonSendMarketSellOrder.setEnabled(hasSelectedAccount);
        });
        
        GridBagConstraints gbcComboBoxAccounts = new GridBagConstraints();
        gbcComboBoxAccounts.insets = new Insets(0, 0, 5, 0);
        gbcComboBoxAccounts.fill = GridBagConstraints.HORIZONTAL;
        gbcComboBoxAccounts.gridx = 1;
        gbcComboBoxAccounts.gridy = 1;
        strategyPanel.add(comboBoxAccounts, gbcComboBoxAccounts);
        
        // Send order buttons:
        Consumer<Boolean> sendMarketOrder = isBuy -> {
            String selectedAlias = (String) comboBoxAliases.getSelectedItem();
            Object selectedAccountItem = comboBoxAccounts.getSelectedItem();
            if (selectedAlias != null && selectedAccountItem != null) {
                String accountId = ((AccountItem) selectedAccountItem).accountInfo.id;
                provider.sendOrder(new SimpleOrderSendParametersBuilder(selectedAlias, isBuy, MARKET_ORDER_SIZE)
                        .setAccountId(accountId)
                        .build());
            }
        };
        
        buttonSendMarketBuyOrder.addActionListener(e -> sendMarketOrder.accept(true));
        buttonSendMarketBuyOrder.setEnabled(false);
        
        GridBagConstraints gbcButtonSendMarketBuyOrder = new GridBagConstraints();
        gbcButtonSendMarketBuyOrder.gridwidth = 2;
        gbcButtonSendMarketBuyOrder.fill = GridBagConstraints.HORIZONTAL;
        gbcButtonSendMarketBuyOrder.insets = new Insets(0, 5, 5, 5);
        gbcButtonSendMarketBuyOrder.gridx = 0;
        gbcButtonSendMarketBuyOrder.gridy = 2;
        strategyPanel.add(buttonSendMarketBuyOrder, gbcButtonSendMarketBuyOrder);
        
        buttonSendMarketSellOrder.addActionListener(e -> sendMarketOrder.accept(false));
        buttonSendMarketSellOrder.setEnabled(false);
        
        GridBagConstraints gbcButtonSendMarketSellOrder = new GridBagConstraints();
        gbcButtonSendMarketSellOrder.gridwidth = 2;
        gbcButtonSendMarketSellOrder.fill = GridBagConstraints.HORIZONTAL;
        gbcButtonSendMarketSellOrder.insets = new Insets(0, 5, 5, 5);
        gbcButtonSendMarketSellOrder.gridx = 0;
        gbcButtonSendMarketSellOrder.gridy = 3;
        strategyPanel.add(buttonSendMarketSellOrder, gbcButtonSendMarketSellOrder);
        
        // Orders table:
        JLabel labelOrders = new JLabel("Orders:");
        labelOrders.setHorizontalAlignment(SwingConstants.LEFT);
        GridBagConstraints gbcLabelOrders = new GridBagConstraints();
        gbcLabelOrders.gridwidth = 1;
        gbcLabelOrders.insets = new Insets(15, 5, 0, 5);
        gbcLabelOrders.gridx = 0;
        gbcLabelOrders.gridy = 4;
        strategyPanel.add(labelOrders, gbcLabelOrders);
        
        JTable tableOrders = new JTable();
        tableOrders.setModel(ordersTableModel);
        JScrollPane tableOrdersScrollPane = new BookmapScrollPane(tableOrders);
        tableOrdersScrollPane.createHorizontalScrollBar();
        
        GridBagConstraints gbcTableOrdersScrollPane = new GridBagConstraints();
        gbcTableOrdersScrollPane.gridwidth = 2;
        gbcTableOrdersScrollPane.fill = GridBagConstraints.BOTH;
        gbcTableOrdersScrollPane.gridx = 0;
        gbcTableOrdersScrollPane.gridy = 5;
        strategyPanel.add(tableOrdersScrollPane, gbcTableOrdersScrollPane);
        
        // Clear orders table button:
        buttonClearOrdersTable.addActionListener(e -> {
            synchronized (orders) {
                orders.clear();
                updateOrdersTable();
            }
        });
        
        GridBagConstraints gbcButtonClearOrdersTable = new GridBagConstraints();
        gbcButtonClearOrdersTable.gridwidth = 2;
        gbcButtonClearOrdersTable.fill = GridBagConstraints.HORIZONTAL;
        gbcButtonClearOrdersTable.insets = new Insets(5, 5, 5, 5);
        gbcButtonClearOrdersTable.gridx = 0;
        gbcButtonClearOrdersTable.gridy = 6;
        strategyPanel.add(buttonClearOrdersTable, gbcButtonClearOrdersTable);
        
        strategyPanel.setSize(600, 400);
    }
    
    private void updateOrdersTable() {
        SwingUtilities.invokeLater(() -> {
            ordersTableModel.setRowCount(0);
            synchronized (orders) {
                for (Map.Entry<String, OrderInfo> entry : orders.entrySet()) {
                    OrderInfo orderInfo = entry.getValue();
                    
                    AccountInfo account = accountListManager.getAccountById(orderInfo.accountId);
                    String accountName = account == null ? "" : account.summary;
                    // Columns: "Order ID", "Instrument", "Account", "Buy/Sell", "Size (filled/total)", "Status", "Type"
                    ordersTableModel.addRow(new Object[] {
                            entry.getKey(),
                            orderInfo.instrumentAlias,
                            accountName,
                            orderInfo.isBuy ? "Buy" : "Sell",
                            orderInfo.filled + "/" + (orderInfo.unfilled + orderInfo.filled),
                            orderInfo.status,
                            orderInfo.type
                    });
                }
            }
        });
    }
    
    @Override
    public void finish() {
        synchronized (indicatorsFullNameToUserName) {
            for (String userName : indicatorsFullNameToUserName.values()) {
                provider.sendUserMessage(new Layer1ApiUserMessageModifyIndicator(MultipleAccountSupportDemoBase.class, userName, false));
            }
        }
        
        SwingUtilities.invokeLater(strategyPanel::dispose);
        
        provider.sendUserMessage(getGeneratorMessage(false));
    }
    
    private Layer1ApiUserMessageModifyIndicator getIndicatorUserMessageAdd() {
        return Layer1ApiUserMessageModifyIndicator.builder(MultipleAccountSupportDemoBase.class, indicatorName)
                .setIsAdd(true)
                .setGraphType(Layer1ApiUserMessageModifyIndicator.GraphType.BOTTOM)
                .setOnlineCalculatable(this)
                .setIndicatorColorScheme(new IndicatorColorScheme() {
                    @Override
                    public ColorDescription[] getColors() {
                        return new ColorDescription[] {
                                new ColorDescription(MultipleAccountSupportDemoBase.class, "Some color", Color.RED, false),
                        };
                    }
                    
                    @Override
                    public String getColorFor(Double value) {
                        return "Some color";
                    }
                    
                    @Override
                    public ColorIntervalResponse getColorIntervalsList(double valueFrom, double valueTo) {
                        return new ColorIntervalResponse(new String[] {"Some color"}, new double[] {});
                    }
                })
                .setIndicatorLineStyle(IndicatorLineStyle.NONE)
                .build();
    }
    
    @Override
    public void onUserMessage(Object data) {
        boolean accountListChanged = accountListManager.onUserMessage(data);
        
        if (accountListChanged) {
            Log.info("Account list changed (accounts count = " + accountListManager.getAccounts().size() + ")");
        }
        
        if (data instanceof TradingAccountsInfoMessage message) {
            Log.info("TradingAccountsInfoMessage: " + message);
        }
        
        if (data.getClass() == UserMessageLayersChainCreatedTargeted.class) {
            SwingUtilities.invokeLater(() -> strategyPanel.setVisible(true));
            
            UserMessageLayersChainCreatedTargeted message = (UserMessageLayersChainCreatedTargeted) data;
            if (message.targetClass == getClass()) {
                
                // Get DataStructureInterface for the indicator here if needed ...
                
                Layer1ApiUserMessageModifyIndicator indicatorUserMessageAdd = getIndicatorUserMessageAdd();
                indicatorsFullNameToUserName.put(indicatorUserMessageAdd.fullName, indicatorUserMessageAdd.userName);
                provider.sendUserMessage(indicatorUserMessageAdd);
                
                provider.sendUserMessage(getGeneratorMessage(true));
            }
        }
    }
    
    @Override
    public void onOrderUpdated(OrderInfoUpdate orderInfoUpdate) {
        accountListManager.onOrderUpdated(orderInfoUpdate);
        
        // Multi-account data should be available here if Strategy is annotated with @Layer1MultiAccountTradingSupported.
        boolean isSelectedAccount = accountListManager.isAccountSelectedForAlias(orderInfoUpdate.accountId, orderInfoUpdate.instrumentAlias);
        Log.info("[isSelectedAccount=" + isSelectedAccount + "] MultipleAccountSupportDemoBase#onOrderUpdated: " + orderInfoUpdate);
        
        // Save order info for GUI:
        synchronized (orders) {
            orders.put(orderInfoUpdate.orderId, orderInfoUpdate);
            updateOrdersTable();
        }
    }
    
    @Override
    public void onOrderExecuted(ExecutionInfo executionInfo) {
        // Multi-account data should be available here if Strategy is annotated with @Layer1MultiAccountTradingSupported.
        boolean isSelectedAccount = accountListManager.isAccountSelectedForOrderIdStrict(executionInfo.orderId);
        Log.info("[isSelectedAccount=" + isSelectedAccount + "] MultipleAccountSupportDemoBase#onOrderExecuted: " + executionInfo);
    }
    
    @Override
    public void onStatus(StatusInfo statusInfo) {
        boolean isSelectedAccount = accountListManager.isAccountSelectedForAlias(statusInfo.accountId, statusInfo.instrumentAlias);
        Log.info("[isSelectedAccount=" + isSelectedAccount + "] MultipleAccountSupportDemoBase#onStatus: " + statusInfo);
    }
    
    @Override
    public void onBalance(BalanceInfo balanceInfo) {}
    
    @Override
    public void onInstrumentAdded(String alias, InstrumentInfo instrumentInfo) {
        // Add alias to the instruments combo box for trading:
        Layer1ApiProviderSupportedFeatures supportedFeaturesForAlias = TargetedRequestHelper.getSupportedFeaturesForAlias(provider, alias);
        if (supportedFeaturesForAlias != null && supportedFeaturesForAlias.trading) {
            SwingUtilities.invokeLater(() -> comboBoxAliases.addItem(alias));
        }
    }
    
    @Override
    public void onInstrumentRemoved(String alias) {
        // Remove alias from the instruments combo box for trading:
        SwingUtilities.invokeLater(() -> comboBoxAliases.removeItem(alias));
    }
    
    @Override
    public void calculateValuesInRange(String indicatorName, String indicatorAlias, long t0, long intervalWidth, int intervalsNumber,
                                       CalculatedResultListener listener) {
        
        listener.setCompleted();
    }
    
    @Override
    public OnlineValueCalculatorAdapter createOnlineValueCalculator(String indicatorName, String indicatorAlias, long time,
                                                                    Consumer<Object> listener, InvalidateInterface invalidateInterface) {
        return new OnlineValueCalculatorAdapter() {
            
            @Override
            public void onStatus(StatusInfo statusInfo) {
                // Multi-account data should be available here if Strategy is annotated with @Layer1MultiAccountTradingSupported.
                Boolean isSelectedAccount = accountListManager.isAccountSelectedForAlias(statusInfo.accountId, statusInfo.instrumentAlias);
                Log.info("[isSelectedAccount=" + isSelectedAccount + "] OnlineValueCalculatorAdapter#onStatus: " + statusInfo);
            }
            
            @Override
            public void onOrderUpdated(OrderInfoUpdate orderInfoUpdate) {
                // Multi-account data should be available here if Strategy is annotated with @Layer1MultiAccountTradingSupported.
                Boolean isSelectedAccount = accountListManager.isAccountSelectedForAlias(orderInfoUpdate.accountId, orderInfoUpdate.instrumentAlias);
                Log.info("[isSelectedAccount=" + isSelectedAccount + "] OnlineValueCalculatorAdapter#onOrderUpdated: " + orderInfoUpdate);
            }
            
            @Override
            public void onOrderExecuted(ExecutionInfo executionInfo) {
                // Multi-account data should be available here if Strategy is annotated with @Layer1MultiAccountTradingSupported.
                Boolean isSelectedAccount = accountListManager.isAccountSelectedForOrderIdStrict(executionInfo.orderId);
                Log.info("[isSelectedAccount=" + isSelectedAccount + "] OnlineValueCalculatorAdapter#onOrderExecuted: " + executionInfo);
            }
            
            @Override
            public void onBalance(BalanceInfo balanceInfo) {}
            
            @Override
            public void onUserMessage(Object data) {
                if (data instanceof TradingAccountsInfoMessage message) {
                    // TradingAccountsListMessage should be received here.
                    Log.info("OnlineValueCalculatorAdapter#onUserMessage: " + message);
                }
            }
            
        };
    }
    
    private Layer1ApiUserMessageAddStrategyUpdateGenerator getGeneratorMessage(boolean isAdd) {
        return new Layer1ApiUserMessageAddStrategyUpdateGenerator(MultipleAccountSupportDemoBase.class, treeName, isAdd, true, new StrategyUpdateGenerator() {
            private Consumer<CustomGeneratedEventAliased> consumer;
            
            @Override
            public void setGeneratedEventsConsumer(Consumer<CustomGeneratedEventAliased> consumer) {
                this.consumer = consumer;
            }
            
            @Override
            public Consumer<CustomGeneratedEventAliased> getGeneratedEventsConsumer() {
                return consumer;
            }
            
            @Override
            public void onStatus(StatusInfo statusInfo) {
                // Multi-account data should be available here if Strategy is annotated with @Layer1MultiAccountTradingSupported.
                Boolean isSelectedAccount = accountListManager.isAccountSelectedForAlias(statusInfo.accountId, statusInfo.instrumentAlias);
                Log.info("[isSelectedAccount=" + isSelectedAccount + "] Layer1ApiUserMessageAddStrategyUpdateGenerator#onStatus: " + statusInfo);
            }
            
            @Override
            public void onOrderUpdated(OrderInfoUpdate orderInfoUpdate) {
                // Multi-account data should be available here if Strategy is annotated with @Layer1MultiAccountTradingSupported.
                Boolean isSelectedAccount = accountListManager.isAccountSelectedForAlias(orderInfoUpdate.accountId, orderInfoUpdate.instrumentAlias);
                Log.info("[isSelectedAccount=" + isSelectedAccount + "] Layer1ApiUserMessageAddStrategyUpdateGenerator#onOrderUpdated: " + orderInfoUpdate);
            }
            
            @Override
            public void onOrderExecuted(ExecutionInfo executionInfo) {
                // Multi-account data should be available here if Strategy is annotated with @Layer1MultiAccountTradingSupported.
                Boolean isSelectedAccount = accountListManager.isAccountSelectedForOrderIdStrict(executionInfo.orderId);
                Log.info("[isSelectedAccount=" + isSelectedAccount + "] Layer1ApiUserMessageAddStrategyUpdateGenerator#onOrderExecuted: " + executionInfo);
            }
            
            @Override
            public void onBalance(BalanceInfo balanceInfo) {
                // BalanceInfo is always received here for all accounts.
                Log.info("[accountId=" + balanceInfo.accountId + "] Layer1ApiUserMessageAddStrategyUpdateGenerator#onBalance: " + balanceInfo);
            }
            
            @Override
            public void onUserMessage(Object data) {
                if (data instanceof TradingAccountsInfoMessage message) {
                    // TradingAccountsListMessage should be received here.
                    Log.info("Layer1ApiUserMessageAddStrategyUpdateGenerator#onUserMessage: " + message);
                }
            }
            
            @Override
            public void setTime(long time) {}
        }, new GeneratedEventInfo[] {new GeneratedEventInfo(SampleEvent.class)});
    }
    
    @Override
    public void onInstrumentNotFound(String symbol, String exchange, String type) {}
    
    @Override
    public void onInstrumentAlreadySubscribed(String symbol, String exchange, String type) {}
}
