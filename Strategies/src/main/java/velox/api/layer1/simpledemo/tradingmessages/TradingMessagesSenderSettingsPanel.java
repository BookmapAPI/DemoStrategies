package velox.api.layer1.simpledemo.tradingmessages;

 import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Function;

import velox.api.layer1.common.Log;
import velox.api.layer1.data.AccountInfo;
import velox.api.layer1.data.AccountInfoBuilder;
import velox.api.layer1.data.InstrumentCoreInfo;
import velox.api.layer1.data.OrderDuration;
import velox.api.layer1.data.OrderType;
import velox.api.layer1.messages.*;
import velox.gui.StrategyPanel;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.Scrollable;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;

public class TradingMessagesSenderSettingsPanel extends StrategyPanel {
    public static TradingMessagesSenderSettingsPanel newPanel(final String name, final Consumer<Object> messageConsumer, final Function<String, Set<AccountInfo>> accountsPerAliasProvider, final AtomicBoolean isWorking) {
        return new TradingMessagesSenderSettingsPanel(name, messageConsumer, accountsPerAliasProvider, isWorking);
    }
    
    private static final String CHANGE_ACCOUNT_TITLE = "Change Selected Account";
    private static final String SET_ORDER_SIZE_TITLE = "Send Set Order Size Message";
    private static final String SET_BRACKETS_TITLE = "Set Brackets";
    private static final String SET_OCO_TITLE = "Set OCO";
    private static final String SET_TRAILING_STOP_TITLE = "Set Trailing Stop";
    private static final String SET_STOP_ORDER_TYPE_TITLE = "Set Stop Order Type";
    private static final String SET_STOP_ORDER_OFFSET_TITLE = "Set Stop Order Offset";
    private static final String SET_STOP_ORDER_DURATION_TITLE = "Set Stop Order Duration";
    private static final String SET_ORDER_DURATION_TITLE = "Set Order Duration";
    private static final String CROSS_TRADING_TITLE = "Cross-Instrument Trading";
    private static final String CROSS_TRADING_ROUNDING_TITLE = "Rounding";
    private static final String CANCEL_ALL_ORDERS_TITLE = "Cancel All Orders";
    private static final String CANCEL_BIDS_TITLE = "Cancel Bids";
    private static final String CANCEL_ASKS_TITLE = "Cancel Asks";
    private static final String FLATTEN_POSITION_TITLE = "Flatten Position";
    private static final String REVERSE_POSITION_TITLE = "Reverse Position";
    private static final String GET_TCP_STATE_TITLE = "Get TCP State";
    private static final String BROADCAST_LOG_TITLE = "TCP Broadcasts";
    private static final String CARD_WORKING = "working";
    private static final String CARD_DISABLED = "disabled";
    /** Sentinel entry in the target combo representing "stop cross trading" (maps to a {@code null} target). */
    private static final InstrumentCoreInfo STOP_CROSS_TRADING =
            new InstrumentCoreInfo("None (stop cross trading)", "", "");
    private final boolean initialized;
    private final JComboBox<String> comboBoxAliasesChangeAccount;
    private final JComboBox<String> comboBoxAliasesSetOrderSize;
    private final JComboBox<AccountInfo> comboBoxAccounts;
    private final JComboBox<String> comboBoxAliasesBrackets;
    private final JComboBox<String> comboBoxAliasesOco;
    private final JComboBox<String> comboBoxAliasesTrailingStop;
    private final JComboBox<String> comboBoxAliasesStopOrderType;
    private final JComboBox<String> comboBoxAliasesStopOrderOffset;
    private final JComboBox<String> comboBoxAliasesStopOrderDuration;
    private final JComboBox<String> comboBoxAliasesOrderDuration;
    private final JComboBox<String> comboBoxAliasesCrossTrading;
    private final JComboBox<InstrumentCoreInfo> comboBoxCrossTradingTargets;
    private final JComboBox<String> comboBoxAliasesCrossRounding;
    private final JComboBox<String> comboBoxCrossRoundingMode;
    private final JComboBox<String> comboBoxAliasesCancelAllOrders;
    private final JComboBox<String> comboBoxAliasesCancelBids;
    private final JComboBox<String> comboBoxAliasesCancelAsks;
    private final JComboBox<String> comboBoxAliasesFlattenPosition;
    private final JComboBox<String> comboBoxAliasesReversePosition;
    private final JComboBox<String> comboBoxAliasesGetState;
    private final JTextArea broadcastLogArea;
    private final Consumer<Object> messageConsumer;
    private final CardLayout cardLayout;
    private final JPanel cardPanel;
    private final AtomicBoolean isWorking;
    
    private final Function<String, Set<AccountInfo>> accountsPerAliasProvider;
    
    private TradingMessagesSenderSettingsPanel(final String name, final Consumer<Object> messageConsumer, final Function<String, Set<AccountInfo>> accountsPerAliasProvider, final AtomicBoolean isWorking) {
        super(name);
        setName(name);
        setBorder(BorderFactory.createEmptyBorder());
        this.isWorking = isWorking;
        initialized = true;
        comboBoxAliasesChangeAccount = newComboBox();
        comboBoxAliasesSetOrderSize = newComboBox();
        comboBoxAccounts = newComboBox();
        comboBoxAliasesBrackets = newComboBox();
        comboBoxAliasesOco = newComboBox();
        comboBoxAliasesTrailingStop = newComboBox();
        comboBoxAliasesStopOrderType = newComboBox();
        comboBoxAliasesStopOrderOffset = newComboBox();
        comboBoxAliasesStopOrderDuration = newComboBox();
        comboBoxAliasesOrderDuration = newComboBox();
        comboBoxAliasesCrossTrading = newComboBox();
        comboBoxCrossTradingTargets = newComboBox();
        comboBoxAliasesCrossRounding = newComboBox();
        comboBoxCrossRoundingMode = new JComboBox<>(new String[]{"To", "Away"});
        comboBoxAliasesCancelAllOrders = newComboBox();
        comboBoxAliasesCancelBids = newComboBox();
        comboBoxAliasesCancelAsks = newComboBox();
        comboBoxAliasesFlattenPosition = newComboBox();
        comboBoxAliasesReversePosition = newComboBox();
        comboBoxAliasesGetState = newComboBox();
        this.messageConsumer = messageConsumer;
        this.accountsPerAliasProvider = accountsPerAliasProvider;
        broadcastLogArea = new JTextArea(6, 20);
        broadcastLogArea.setEditable(false);
        broadcastLogArea.setLineWrap(true);
        broadcastLogArea.setWrapStyleWord(true);
        setLayout(new BorderLayout());
        JComponent workingPanel = getMainPanel();
        JComponent disabledPanel = getDisabledPanel();
        cardLayout = new CardLayout();
        cardPanel = new JPanel(cardLayout);
        cardPanel.add(disabledPanel, CARD_DISABLED);
        cardPanel.add(workingPanel, CARD_WORKING);
        cardPanel.setPreferredSize(new Dimension(360, 600));
        this.add(cardPanel, BorderLayout.CENTER);
        updateIfNeeded();

        comboBoxAliasesChangeAccount.addActionListener(e -> updateAccounts());


        comboBoxAccounts.setRenderer(new DefaultListCellRenderer() {
            @Override
            public java.awt.Component getListCellRendererComponent(javax.swing.JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                if (value instanceof AccountInfo accountInfo) {
                    return super.getListCellRendererComponent(list, accountInfo.summary, index, isSelected, cellHasFocus);
                }
                return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            }
        });
    }
    
    @Override
    public void setName(final String name) {
        if (!initialized) {
            super.setName(name);
        }
    }
    
    @Override
    public void setBorder(final Border border) {
        if (!initialized) {
            super.setBorder(border);
        }
    }
    
    public void updateAccounts() {
        String alias = (String) comboBoxAliasesChangeAccount.getSelectedItem();
        if (alias != null && !alias.isEmpty()) {
            List<AccountInfo> accounts = new ArrayList<>(accountsPerAliasProvider.apply(alias));
            accounts.sort(Comparator.comparing(account -> account.summary));
            accounts.add(0, new AccountInfoBuilder("", "None").build());

            Log.info("TradingMessagesSenderSettingsPanel", "Updating accounts for alias: " + alias + ", accounts: " + accounts);
            updateComboBox(comboBoxAccounts, accounts);
        }
    }
    
    public void updateInstruments(List<String> aliases) {
        updateComboBox(comboBoxAliasesChangeAccount, aliases);
        updateComboBox(comboBoxAliasesSetOrderSize, aliases);
        updateComboBox(comboBoxAliasesBrackets, aliases);
        updateComboBox(comboBoxAliasesOco, aliases);
        updateComboBox(comboBoxAliasesTrailingStop, aliases);
        updateComboBox(comboBoxAliasesStopOrderType, aliases);
        updateComboBox(comboBoxAliasesStopOrderOffset, aliases);
        updateComboBox(comboBoxAliasesStopOrderDuration, aliases);
        updateComboBox(comboBoxAliasesOrderDuration, aliases);
        updateComboBox(comboBoxAliasesCrossTrading, aliases);
        updateComboBox(comboBoxAliasesCrossRounding, aliases);
        updateComboBox(comboBoxAliasesCancelAllOrders, aliases);
        updateComboBox(comboBoxAliasesCancelBids, aliases);
        updateComboBox(comboBoxAliasesCancelAsks, aliases);
        updateComboBox(comboBoxAliasesFlattenPosition, aliases);
        updateComboBox(comboBoxAliasesReversePosition, aliases);
        updateComboBox(comboBoxAliasesGetState, aliases);
    }
    
    public void updateIfNeeded() {
        cardLayout.show(cardPanel, isWorking.get() ? CARD_WORKING : CARD_DISABLED);
    }

    /** Appends a Trading Control Panel broadcast to the on-screen log. */
    public void appendBroadcast(final String text) {
        SwingUtilities.invokeLater(() -> {
            broadcastLogArea.append(text + "\n");
            broadcastLogArea.setCaretPosition(broadcastLogArea.getDocument().getLength());
        });
    }
    
    private JComponent getMainPanel() {
        JPanel panel = new ScrollableWidthPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        panel.add(buildBroadcastLogBlock());
        panel.add(buildChangeAccountBlock());
        panel.add(buildSetOrderSizeBlock());
        panel.add(buildSetBracketsBlock());
        panel.add(buildSetOcoBlock());
        panel.add(buildSetTrailingStopBlock());
        panel.add(buildSetStopOrderTypeBlock());
        panel.add(buildSetStopOrderOffsetBlock());
        panel.add(buildSetStopOrderDurationBlock());
        panel.add(buildSetOrderDurationBlock());
        panel.add(buildCrossTradingBlock());
        panel.add(buildCrossTradingRoundingBlock());
        panel.add(buildCancelAllOrdersBlock());
        panel.add(buildCancelBidsBlock());
        panel.add(buildCancelAsksBlock());
        panel.add(buildFlattenPositionBlock());
        panel.add(buildReversePositionBlock());
        panel.add(buildGetTcpStateBlock());

        JScrollPane scrollPane = new JScrollPane(panel);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        return scrollPane;
    }

    private JPanel buildBroadcastLogBlock() {
        JPanel block = new JPanel();
        block.setLayout(new BoxLayout(block, BoxLayout.Y_AXIS));
        block.setBorder(BorderFactory.createTitledBorder(BROADCAST_LOG_TITLE));

        JScrollPane logScrollPane = new JScrollPane(broadcastLogArea);
        logScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        logScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        JButton clearButton = new JButton("Clear");
        clearButton.addActionListener(e -> broadcastLogArea.setText(""));

        block.add(new JLabel("TCP value change broadcasts:"));
        block.add(logScrollPane);
        block.add(clearButton);
        return block;
    }

    private JPanel buildChangeAccountBlock() {
        JPanel block = new JPanel();
        block.setLayout(new BoxLayout(block, BoxLayout.Y_AXIS));
        block.setBorder(BorderFactory.createTitledBorder(CHANGE_ACCOUNT_TITLE));

        JLabel comboBoxAliasesLabel = new JLabel("Select instrument:");
        JLabel accountLabel = new JLabel("Select Account:");

        JButton sendButton = new JButton("Send ChangeSelectedAccount Message");
        sendButton.addActionListener(e -> {
            String alias = getSelectedAlias(comboBoxAliasesChangeAccount);
            if (alias == null) return;

            AccountInfo selectedAccount = (AccountInfo) comboBoxAccounts.getSelectedItem();
            if (selectedAccount == null) {
                JOptionPane.showMessageDialog(null, "Selected Account cannot be empty!", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            // The "None" entry has an empty id and is sent as a null accountId, which disables trading.
            String accountId = selectedAccount.id.isEmpty() ? null : selectedAccount.id;

            String messageText = "Layer1ApiChangeSelectedAccountMessage with alias: " + alias + ", accountId: " + accountId;
            messageConsumer.accept(new Layer1ApiChangeSelectedAccountMessage(alias, accountId, getResponseListener(messageText)));
        });

        block.add(comboBoxAliasesLabel);
        block.add(comboBoxAliasesChangeAccount);
        block.add(accountLabel);
        block.add(comboBoxAccounts);
        block.add(sendButton);
        return block;
    }

    private JPanel buildSetOrderSizeBlock() {
        JPanel setOrderSizeBlock = new JPanel();
        setOrderSizeBlock.setLayout(new BoxLayout(setOrderSizeBlock, BoxLayout.Y_AXIS));
        setOrderSizeBlock.setBorder(BorderFactory.createTitledBorder(SET_ORDER_SIZE_TITLE));
        JLabel comboBoxSetOrderSizeAliasesLabel = new JLabel("Select instrument:");

        JLabel integerFieldLabel = new JLabel("Order size:");
        JLabel integerFieldLabelNote = new JLabel("(Note that the size multiplier will be applied)");
        integerFieldLabelNote.setFont(new Font("", Font.ITALIC, 12));
        JSpinner spinnerOrderSize = new JSpinner(new SpinnerNumberModel(1,
                Layer1ApiSetOrderSizeMessage.ORDER_SIZE_MIN_VALUE,
                Layer1ApiSetOrderSizeMessage.ORDER_SIZE_MAX_VALUE, 1));

        JButton sendSetOrderSizeMessage = new JButton("Send SetOrderSize Message");
        sendSetOrderSizeMessage.addActionListener(e -> {
            String alias = getSelectedAlias(comboBoxAliasesSetOrderSize);
            if (alias == null) return;
            int orderSize = (int) spinnerOrderSize.getValue();
            String messageText = "Layer1ApiSetOrderSizeMessage with alias: " + alias + ", orderSize: " + orderSize;
            messageConsumer.accept(new Layer1ApiSetOrderSizeMessage(alias, orderSize, getResponseListener(messageText)));
        });

        setOrderSizeBlock.add(comboBoxSetOrderSizeAliasesLabel);
        setOrderSizeBlock.add(comboBoxAliasesSetOrderSize);
        setOrderSizeBlock.add(integerFieldLabel);
        setOrderSizeBlock.add(integerFieldLabelNote);
        setOrderSizeBlock.add(spinnerOrderSize);
        setOrderSizeBlock.add(sendSetOrderSizeMessage);
        return setOrderSizeBlock;
    }

    private JPanel buildSetBracketsBlock() {
        JPanel block = new JPanel();
        block.setLayout(new BoxLayout(block, BoxLayout.Y_AXIS));
        block.setBorder(BorderFactory.createTitledBorder(SET_BRACKETS_TITLE));

        block.add(new JLabel("Select instrument:"));
        block.add(comboBoxAliasesBrackets);

        JCheckBox cbEnabled = new JCheckBox("Enable Brackets", true);
        // A 0 offset means the corresponding leg is not set, mirroring the message's "not set" sentinel,
        // so each leg can be toggled independently without a dedicated checkbox.
        JSpinner spinnerTp = new JSpinner(new SpinnerNumberModel(10, 0, 9999, 1));
        JSpinner spinnerSl = new JSpinner(new SpinnerNumberModel(10, 0, 9999, 1));

        block.add(cbEnabled);
        block.add(new JLabel("Take Profit offset (ticks, 0 = not set):"));
        block.add(spinnerTp);
        block.add(new JLabel("Stop Loss offset (ticks, 0 = not set):"));
        block.add(spinnerSl);

        JButton sendButton = new JButton("Send SetBrackets Message");
        sendButton.addActionListener(e -> {
            String alias = getSelectedAlias(comboBoxAliasesBrackets);
            if (alias == null) return;
            Layer1ApiSetBracketsMessage message;
            if (!cbEnabled.isSelected()) {
                // Disable brackets entirely.
                String messageText = "Layer1ApiSetBracketsMessage with alias: " + alias + ", enabled: false";
                message = new Layer1ApiSetBracketsMessage(alias, getResponseListener(messageText));
            } else {
                // 0 means the leg is not set; the spinners already allow 0 as their minimum.
                int tp = (int) spinnerTp.getValue();
                int sl = (int) spinnerSl.getValue();
                if (tp == 0 && sl == 0) {
                    JOptionPane.showMessageDialog(null,
                            "Set a non-zero Take Profit or Stop Loss offset when enabling brackets.",
                            "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                String messageText = "Layer1ApiSetBracketsMessage with alias: " + alias
                        + ", enabled: true, tp: " + tp + ", sl: " + sl;
                message = new Layer1ApiSetBracketsMessage(alias, tp, sl, getResponseListener(messageText));
            }
            messageConsumer.accept(message);
        });
        block.add(sendButton);
        return block;
    }

    private JPanel buildSetOcoBlock() {
        JPanel block = new JPanel();
        block.setLayout(new BoxLayout(block, BoxLayout.Y_AXIS));
        block.setBorder(BorderFactory.createTitledBorder(SET_OCO_TITLE));

        block.add(new JLabel("Select instrument:"));
        block.add(comboBoxAliasesOco);

        JCheckBox cbEnabled = new JCheckBox("Enable OCO", true);
        block.add(cbEnabled);

        JButton sendButton = new JButton("Send SetOco Message");
        sendButton.addActionListener(e -> {
            String alias = getSelectedAlias(comboBoxAliasesOco);
            if (alias == null) return;
            boolean enabled = cbEnabled.isSelected();
            String messageText = "Layer1ApiSetOcoMessage with alias: " + alias + ", enabled: " + enabled;
            messageConsumer.accept(new Layer1ApiSetOcoMessage(alias, enabled, getResponseListener(messageText)));
        });
        block.add(sendButton);
        return block;
    }

    private JPanel buildSetTrailingStopBlock() {
        JPanel block = new JPanel();
        block.setLayout(new BoxLayout(block, BoxLayout.Y_AXIS));
        block.setBorder(BorderFactory.createTitledBorder(SET_TRAILING_STOP_TITLE));

        block.add(new JLabel("Select instrument:"));
        block.add(comboBoxAliasesTrailingStop);

        JCheckBox cbEnabled = new JCheckBox("Enable Trailing Stop", true);
        JSpinner spinnerStep = new JSpinner(new SpinnerNumberModel(5, 1, 9999, 1));

        block.add(cbEnabled);
        block.add(new JLabel("Trailing step (ticks):"));
        block.add(spinnerStep);

        JButton sendButton = new JButton("Send SetTrailingStop Message");
        sendButton.addActionListener(e -> {
            String alias = getSelectedAlias(comboBoxAliasesTrailingStop);
            if (alias == null) return;
            Layer1ApiSetTrailingStopMessage message;
            if (cbEnabled.isSelected()) {
                int step = (int) spinnerStep.getValue();
                String messageText = "Layer1ApiSetTrailingStopMessage with alias: " + alias
                        + ", enabled: true, step: " + step;
                message = new Layer1ApiSetTrailingStopMessage(alias, step, getResponseListener(messageText));
            } else {
                String messageText = "Layer1ApiSetTrailingStopMessage with alias: " + alias + ", enabled: false";
                message = new Layer1ApiSetTrailingStopMessage(alias, getResponseListener(messageText));
            }
            messageConsumer.accept(message);
        });
        block.add(sendButton);
        return block;
    }

    private JPanel buildSetStopOrderTypeBlock() {
        JPanel block = new JPanel();
        block.setLayout(new BoxLayout(block, BoxLayout.Y_AXIS));
        block.setBorder(BorderFactory.createTitledBorder(SET_STOP_ORDER_TYPE_TITLE));

        block.add(new JLabel("Select instrument:"));
        block.add(comboBoxAliasesStopOrderType);

        OrderType[] stopTypes = {OrderType.MKT, OrderType.LMT};
        JComboBox<OrderType> typeCombo = new JComboBox<>(stopTypes);
        block.add(new JLabel("Stop Order Type:"));
        block.add(typeCombo);

        JButton sendButton = new JButton("Send SetStopOrderType Message");
        sendButton.addActionListener(e -> {
            String alias = getSelectedAlias(comboBoxAliasesStopOrderType);
            if (alias == null) return;
            OrderType type = (OrderType) typeCombo.getSelectedItem();
            String messageText = "Layer1ApiSetStopOrderTypeMessage with alias: " + alias + ", type: " + type;
            messageConsumer.accept(new Layer1ApiSetStopOrderTypeMessage(alias, type, getResponseListener(messageText)));
        });
        block.add(sendButton);
        return block;
    }

    private JPanel buildSetStopOrderOffsetBlock() {
        JPanel block = new JPanel();
        block.setLayout(new BoxLayout(block, BoxLayout.Y_AXIS));
        block.setBorder(BorderFactory.createTitledBorder(SET_STOP_ORDER_OFFSET_TITLE));

        block.add(new JLabel("Select instrument:"));
        block.add(comboBoxAliasesStopOrderOffset);

        // Only applies to stop-limit orders. Negative values are accepted only on instruments that support them.
        JSpinner spinnerOffset = new JSpinner(new SpinnerNumberModel(0,
                -Layer1ApiSetStopOrderOffsetMessage.STOP_ORDER_OFFSET_MAX_ABS_VALUE,
                Layer1ApiSetStopOrderOffsetMessage.STOP_ORDER_OFFSET_MAX_ABS_VALUE, 1));
        block.add(new JLabel("Stop Order offset (ticks):"));
        block.add(spinnerOffset);

        JButton sendButton = new JButton("Send SetStopOrderOffset Message");
        sendButton.addActionListener(e -> {
            String alias = getSelectedAlias(comboBoxAliasesStopOrderOffset);
            if (alias == null) return;
            int offset = (int) spinnerOffset.getValue();
            String messageText = "Layer1ApiSetStopOrderOffsetMessage with alias: " + alias + ", offset: " + offset;
            messageConsumer.accept(new Layer1ApiSetStopOrderOffsetMessage(alias, offset, getResponseListener(messageText)));
        });
        block.add(sendButton);
        return block;
    }

    private JPanel buildSetStopOrderDurationBlock() {
        JPanel block = new JPanel();
        block.setLayout(new BoxLayout(block, BoxLayout.Y_AXIS));
        block.setBorder(BorderFactory.createTitledBorder(SET_STOP_ORDER_DURATION_TITLE));

        block.add(new JLabel("Select instrument:"));
        block.add(comboBoxAliasesStopOrderDuration);

        // Hardcoded for simplicity. In production, these should be obtained from SupportedFeatures
        OrderDuration[] durations = {OrderDuration.GTC, OrderDuration.DAY, OrderDuration.FOK, OrderDuration.IOC};
        JComboBox<OrderDuration> durationCombo = new JComboBox<>(durations);
        block.add(new JLabel("Stop Order Duration:"));
        block.add(durationCombo);

        JButton sendButton = new JButton("Send SetStopOrderDuration Message");
        sendButton.addActionListener(e -> {
            String alias = getSelectedAlias(comboBoxAliasesStopOrderDuration);
            if (alias == null) return;
            OrderDuration duration = (OrderDuration) durationCombo.getSelectedItem();
            String messageText =
                    "Layer1ApiSetStopOrderDurationMessage with alias: " + alias + ", duration: " + duration;
            messageConsumer.accept(
                    new Layer1ApiSetStopOrderDurationMessage(alias, duration, getResponseListener(messageText)));
        });
        block.add(sendButton);
        return block;
    }

    private JPanel buildSetOrderDurationBlock() {
        JPanel block = new JPanel();
        block.setLayout(new BoxLayout(block, BoxLayout.Y_AXIS));
        block.setBorder(BorderFactory.createTitledBorder(SET_ORDER_DURATION_TITLE));

        block.add(new JLabel("Select instrument:"));
        block.add(comboBoxAliasesOrderDuration);

        // Hardcoded for simplicity. In production, these should be obtained from SupportedFeatures
        OrderDuration[] durations = {OrderDuration.GTC, OrderDuration.DAY, OrderDuration.FOK, OrderDuration.IOC};
        JComboBox<OrderDuration> durationCombo = new JComboBox<>(durations);
        block.add(new JLabel("Order Duration:"));
        block.add(durationCombo);

        JButton sendButton = new JButton("Send SetOrderDuration Message");
        sendButton.addActionListener(e -> {
            String alias = getSelectedAlias(comboBoxAliasesOrderDuration);
            if (alias == null) return;
            OrderDuration duration = (OrderDuration) durationCombo.getSelectedItem();
            String messageText = "Layer1ApiSetOrderDurationMessage with alias: " + alias + ", duration: " + duration;
            messageConsumer.accept(new Layer1ApiSetOrderDurationMessage(alias, duration, getResponseListener(messageText)));
        });
        block.add(sendButton);
        return block;
    }

    private JPanel buildCrossTradingBlock() {
        JPanel block = new JPanel();
        block.setLayout(new BoxLayout(block, BoxLayout.Y_AXIS));
        block.setBorder(BorderFactory.createTitledBorder(CROSS_TRADING_TITLE));

        block.add(new JLabel("Select instrument:"));
        block.add(comboBoxAliasesCrossTrading);

        comboBoxCrossTradingTargets.setRenderer(new DefaultListCellRenderer() {
            @Override
            public java.awt.Component getListCellRendererComponent(javax.swing.JList<?> list, Object value,
                    int index, boolean isSelected, boolean cellHasFocus) {
                Object text = value;
                if (value instanceof InstrumentCoreInfo info) {
                    text = (info == STOP_CROSS_TRADING) ? info.symbol
                            : info.symbol + " @ " + info.exchange + " (" + info.type + ")";
                }
                return super.getListCellRendererComponent(list, text, index, isSelected, cellHasFocus);
            }
        });

        // The set of valid targets depends on the source instrument, so fetch them from the TCP before choosing one.
        JButton refreshButton = new JButton("Refresh Possible Targets");
        refreshButton.addActionListener(e -> {
            String alias = getSelectedAlias(comboBoxAliasesCrossTrading);
            if (alias == null) return;
            messageConsumer.accept(new Layer1ApiGetCrossTradingTargetMessage(alias, state ->
                    SwingUtilities.invokeLater(() -> updateCrossTradingTargets(state))));
        });

        block.add(new JLabel("Target instrument:"));
        block.add(comboBoxCrossTradingTargets);
        block.add(refreshButton);

        JButton sendButton = new JButton("Send SetCrossTradingTarget Message");
        sendButton.addActionListener(e -> {
            String alias = getSelectedAlias(comboBoxAliasesCrossTrading);
            if (alias == null) return;
            InstrumentCoreInfo selected = (InstrumentCoreInfo) comboBoxCrossTradingTargets.getSelectedItem();
            if (selected == null) {
                JOptionPane.showMessageDialog(null,
                        "Refresh the possible targets and select one (or \"None\") first.",
                        "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            // The sentinel maps to a null target, which stops cross trading.
            InstrumentCoreInfo target = (selected == STOP_CROSS_TRADING) ? null : selected;
            String messageText = "Layer1ApiSetCrossTradingTargetMessage with alias: " + alias + ", target: " + target;
            messageConsumer.accept(
                    new Layer1ApiSetCrossTradingTargetMessage(alias, target, getResponseListener(messageText)));
        });
        block.add(sendButton);
        return block;
    }

    private void updateCrossTradingTargets(Layer1ApiGetCrossTradingTargetMessage.CrossTradingTargetState state) {
        List<InstrumentCoreInfo> items = new ArrayList<>();
        // The first entry stops cross trading; the rest are the targets reported by the TCP.
        items.add(STOP_CROSS_TRADING);
        items.addAll(state.possibleTargets);
        updateComboBox(comboBoxCrossTradingTargets, items);
        // Pre-select the current target, or the "stop" sentinel when cross trading is not active.
        SwingUtilities.invokeLater(() -> comboBoxCrossTradingTargets.setSelectedItem(
                state.target == null ? STOP_CROSS_TRADING : state.target));
    }

    private JPanel buildCrossTradingRoundingBlock() {
        JPanel block = new JPanel();
        block.setLayout(new BoxLayout(block, BoxLayout.Y_AXIS));
        block.setBorder(BorderFactory.createTitledBorder(CROSS_TRADING_ROUNDING_TITLE));

        block.add(new JLabel("Select instrument:"));
        block.add(comboBoxAliasesCrossRounding);

        block.add(new JLabel("Rounding rule:"));
        block.add(comboBoxCrossRoundingMode);

        JButton sendButton = new JButton("Send Rounding Message");
        sendButton.addActionListener(e -> {
            String alias = getSelectedAlias(comboBoxAliasesCrossRounding);
            if (alias == null) return;
            CrossTradingRoundingMode mode = "To".equals(comboBoxCrossRoundingMode.getSelectedItem())
                    ? CrossTradingRoundingMode.ROUND_TO_MARKET_PRICE
                    : CrossTradingRoundingMode.ROUND_OFF_MARKET_PRICE;
            String messageText = "Layer1ApiSetCrossTradingRoundingMessage with alias: " + alias + ", mode: " + mode;
            messageConsumer.accept(
                    new Layer1ApiSetCrossTradingRoundingMessage(alias, mode, getResponseListener(messageText)));
        });
        block.add(sendButton);
        return block;
    }

    private JPanel buildCancelAllOrdersBlock() {
        JPanel block = new JPanel();
        block.setLayout(new BoxLayout(block, BoxLayout.Y_AXIS));
        block.setBorder(BorderFactory.createTitledBorder(CANCEL_ALL_ORDERS_TITLE));

        block.add(new JLabel("Select instrument:"));
        block.add(comboBoxAliasesCancelAllOrders);

        JButton sendButton = new JButton("Send CancelAllOrders Message");
        sendButton.addActionListener(e -> {
            String alias = getSelectedAlias(comboBoxAliasesCancelAllOrders);
            if (alias == null) return;
            String messageText = "Layer1ApiCancelAllOrdersMessage with alias: " + alias;
            messageConsumer.accept(new Layer1ApiCancelAllOrdersMessage(alias, getResponseListener(messageText)));
        });
        block.add(sendButton);
        return block;
    }

    private JPanel buildCancelBidsBlock() {
        JPanel block = new JPanel();
        block.setLayout(new BoxLayout(block, BoxLayout.Y_AXIS));
        block.setBorder(BorderFactory.createTitledBorder(CANCEL_BIDS_TITLE));

        block.add(new JLabel("Select instrument:"));
        block.add(comboBoxAliasesCancelBids);

        JButton sendButton = new JButton("Send CancelBids Message");
        sendButton.addActionListener(e -> {
            String alias = getSelectedAlias(comboBoxAliasesCancelBids);
            if (alias == null) return;
            String messageText = "Layer1ApiCancelBidsMessage with alias: " + alias;
            messageConsumer.accept(new Layer1ApiCancelBidsMessage(alias, getResponseListener(messageText)));
        });
        block.add(sendButton);
        return block;
    }

    private JPanel buildCancelAsksBlock() {
        JPanel block = new JPanel();
        block.setLayout(new BoxLayout(block, BoxLayout.Y_AXIS));
        block.setBorder(BorderFactory.createTitledBorder(CANCEL_ASKS_TITLE));

        block.add(new JLabel("Select instrument:"));
        block.add(comboBoxAliasesCancelAsks);

        JButton sendButton = new JButton("Send CancelAsks Message");
        sendButton.addActionListener(e -> {
            String alias = getSelectedAlias(comboBoxAliasesCancelAsks);
            if (alias == null) return;
            String messageText = "Layer1ApiCancelAsksMessage with alias: " + alias;
            messageConsumer.accept(new Layer1ApiCancelAsksMessage(alias, getResponseListener(messageText)));
        });
        block.add(sendButton);
        return block;
    }

    private JPanel buildFlattenPositionBlock() {
        JPanel block = new JPanel();
        block.setLayout(new BoxLayout(block, BoxLayout.Y_AXIS));
        block.setBorder(BorderFactory.createTitledBorder(FLATTEN_POSITION_TITLE));

        block.add(new JLabel("Select instrument:"));
        block.add(comboBoxAliasesFlattenPosition);

        JButton sendButton = new JButton("Send FlattenPosition Message");
        sendButton.addActionListener(e -> {
            String alias = getSelectedAlias(comboBoxAliasesFlattenPosition);
            if (alias == null) return;
            String messageText = "Layer1ApiFlattenPositionMessage with alias: " + alias;
            messageConsumer.accept(new Layer1ApiFlattenPositionMessage(alias, getResponseListener(messageText)));
        });
        block.add(sendButton);
        return block;
    }

    private JPanel buildReversePositionBlock() {
        JPanel block = new JPanel();
        block.setLayout(new BoxLayout(block, BoxLayout.Y_AXIS));
        block.setBorder(BorderFactory.createTitledBorder(REVERSE_POSITION_TITLE));

        block.add(new JLabel("Select instrument:"));
        block.add(comboBoxAliasesReversePosition);

        JButton sendButton = new JButton("Send ReversePosition Message");
        sendButton.addActionListener(e -> {
            String alias = getSelectedAlias(comboBoxAliasesReversePosition);
            if (alias == null) return;
            String messageText = "Layer1ApiReversePositionMessage with alias: " + alias;
            messageConsumer.accept(new Layer1ApiReversePositionMessage(alias, getResponseListener(messageText)));
        });
        block.add(sendButton);
        return block;
    }

    private JPanel buildGetTcpStateBlock() {
        JPanel block = new JPanel();
        block.setLayout(new BoxLayout(block, BoxLayout.Y_AXIS));
        block.setBorder(BorderFactory.createTitledBorder(GET_TCP_STATE_TITLE));

        block.add(new JLabel("Select instrument:"));
        block.add(comboBoxAliasesGetState);

        JButton getBracketsBtn = new JButton("Get Brackets");
        getBracketsBtn.addActionListener(e -> {
            String alias = getSelectedAlias(comboBoxAliasesGetState);
            if (alias == null) return;
            messageConsumer.accept(new Layer1ApiGetBracketsMessage(alias, state ->
                    showMessageDialog("Brackets: " + state)));
        });

        JButton getOcoBtn = new JButton("Get OCO");
        getOcoBtn.addActionListener(e -> {
            String alias = getSelectedAlias(comboBoxAliasesGetState);
            if (alias == null) return;
            messageConsumer.accept(new Layer1ApiGetOcoMessage(alias, state ->
                    showMessageDialog("OCO: " + state)));
        });

        JButton getTrailingStopBtn = new JButton("Get Trailing Stop");
        getTrailingStopBtn.addActionListener(e -> {
            String alias = getSelectedAlias(comboBoxAliasesGetState);
            if (alias == null) return;
            messageConsumer.accept(new Layer1ApiGetTrailingStopMessage(alias, state ->
                    showMessageDialog("Trailing Stop: " + state)));
        });

        JButton getStopOrderTypeBtn = new JButton("Get Stop Order Type");
        getStopOrderTypeBtn.addActionListener(e -> {
            String alias = getSelectedAlias(comboBoxAliasesGetState);
            if (alias == null) return;
            messageConsumer.accept(new Layer1ApiGetStopOrderTypeMessage(alias, state ->
                    showMessageDialog("Stop Order Type: " + state)));
        });

        JButton getStopOrderOffsetBtn = new JButton("Get Stop Order Offset");
        getStopOrderOffsetBtn.addActionListener(e -> {
            String alias = getSelectedAlias(comboBoxAliasesGetState);
            if (alias == null) return;
            messageConsumer.accept(new Layer1ApiGetStopOrderOffsetMessage(alias, state ->
                    showMessageDialog("Stop Order Offset: " + state)));
        });

        JButton getStopOrderDurationBtn = new JButton("Get Stop Order Duration");
        getStopOrderDurationBtn.addActionListener(e -> {
            String alias = getSelectedAlias(comboBoxAliasesGetState);
            if (alias == null) return;
            messageConsumer.accept(new Layer1ApiGetStopOrderDurationMessage(alias, state ->
                    showMessageDialog("Stop Order Duration: " + state)));
        });

        JButton getOrderDurationBtn = new JButton("Get Order Duration");
        getOrderDurationBtn.addActionListener(e -> {
            String alias = getSelectedAlias(comboBoxAliasesGetState);
            if (alias == null) return;
            messageConsumer.accept(new Layer1ApiGetOrderDurationMessage(alias, state ->
                    showMessageDialog("Order Duration: " + state)));
        });

        JButton getOrderSizeBtn = new JButton("Get Order Size");
        getOrderSizeBtn.addActionListener(e -> {
            String alias = getSelectedAlias(comboBoxAliasesGetState);
            if (alias == null) return;
            messageConsumer.accept(new Layer1ApiGetOrderSizeMessage(alias, state ->
                    showMessageDialog("Order Size: " + state)));
        });

        JButton getCrossTradingTargetBtn = new JButton("Get Cross Trading Target");
        getCrossTradingTargetBtn.addActionListener(e -> {
            String alias = getSelectedAlias(comboBoxAliasesGetState);
            if (alias == null) return;
            messageConsumer.accept(new Layer1ApiGetCrossTradingTargetMessage(alias, state ->
                    showMessageDialog("Cross Trading Target: " + state)));
        });

        JButton getCrossTradingRoundingBtn = new JButton("Get Cross Trading Rounding");
        getCrossTradingRoundingBtn.addActionListener(e -> {
            String alias = getSelectedAlias(comboBoxAliasesGetState);
            if (alias == null) return;
            messageConsumer.accept(new Layer1ApiGetCrossTradingRoundingMessage(alias, state ->
                    showMessageDialog("Cross Trading Rounding: " + state)));
        });

        block.add(getBracketsBtn);
        block.add(getOcoBtn);
        block.add(getTrailingStopBtn);
        block.add(getStopOrderTypeBtn);
        block.add(getStopOrderOffsetBtn);
        block.add(getStopOrderDurationBtn);
        block.add(getOrderDurationBtn);
        block.add(getOrderSizeBtn);
        block.add(getCrossTradingTargetBtn);
        block.add(getCrossTradingRoundingBtn);
        return block;
    }

    private String getSelectedAlias(JComboBox<String> comboBox) {
        String alias = (String) comboBox.getSelectedItem();
        if (alias == null || alias.isEmpty()) {
            JOptionPane.showMessageDialog(null, "Alias cannot be empty!", "Error", JOptionPane.ERROR_MESSAGE);
            return null;
        }
        return alias;
    }
    
    public static <E> JComboBox<E> newComboBox() {
        final JComboBox<E> comboBox = new JComboBox<>();
        final Runnable update = () -> {
            @SuppressWarnings("unchecked") final E item = (E) comboBox.getSelectedItem();
            comboBox.setPrototypeDisplayValue(item);
        };
        comboBox.addItemListener(e -> update.run());
        return comboBox;
    }
    
    private <T> void updateComboBox(final JComboBox<T> comboBox, final List<T> items) {
        SwingUtilities.invokeLater(() -> {
            @SuppressWarnings("unchecked") final T item = (T) comboBox.getModel().getSelectedItem();
            final ComboBoxModel<T> model = new DefaultComboBoxModel<>(new Vector<>(items));
            comboBox.setModel(model);
            model.setSelectedItem(item);
        });
    }
    
    private JComponent getDisabledPanel() {
        StrategyPanel panel = new StrategyPanel("Strategy is disabled");
        panel.setLayout(new FlowLayout(FlowLayout.LEFT));
        JLabel label = new JLabel(String.format("<html>Enable the '%s' checkbox to activate<br>this add-on.</html>", TradingMessagesSender.NAME));
        panel.setVisible(!this.isWorking.get());
        panel.add(label);
        return panel;
    }
    
    private Layer1ApiTradingMessageWithCallback.TradingMessageResponseListener getResponseListener(String message) {
        return response -> showMessageDialog(String.format("Recieved response by message %s, the response was %s", message, response));
    }
    
    private void showMessageDialog(String message) {
        SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(null, message));
    }

    /**
     * A vertically-stacked panel whose width always follows the scroll pane viewport.
     * This makes the blocks wrap/shrink to the available width instead of extending past the
     * right edge of the add-on window (which would otherwise be clipped, since the horizontal
     * scrollbar is disabled).
     */
    private static class ScrollableWidthPanel extends JPanel implements Scrollable {
        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
            return 16;
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
            return orientation == SwingConstants.VERTICAL ? visibleRect.height : visibleRect.width;
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            // Never grow wider than the viewport, so nothing gets cut off on the right.
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            // Allow the content to be taller than the viewport so the vertical scrollbar can appear.
            return false;
        }
    }
}
