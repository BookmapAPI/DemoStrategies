package velox.api.layer1.simpledemo.tradingmessages;

import java.awt.FlowLayout;
import java.awt.Font;
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
import javax.swing.SpinnerNumberModel;
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
    private static final String GET_TCP_STATE_TITLE = "Get TCP State";
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
    private final JComboBox<String> comboBoxAliasesGetState;
    private final Consumer<Object> messageConsumer;
    private final JComponent workingPanel;
    private final JComponent disabledPanel;
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
        comboBoxAliasesGetState = newComboBox();
        this.messageConsumer = messageConsumer;
        this.accountsPerAliasProvider = accountsPerAliasProvider;
        workingPanel = getMainPanel();
        disabledPanel = getDisabledPanel();
        this.add(disabledPanel);
        this.add(workingPanel);
        
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
            accounts.add(0, new AccountInfoBuilder(null, "None").build());
            
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
        updateComboBox(comboBoxAliasesGetState, aliases);
    }
    
    public void updateIfNeeded() {
        workingPanel.setVisible(isWorking.get());
        disabledPanel.setVisible(!isWorking.get());
    }
    
    private JPanel getMainPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        panel.add(buildChangeAccountBlock());
        panel.add(buildSetOrderSizeBlock());
        panel.add(buildSetBracketsBlock());
        panel.add(buildSetOcoBlock());
        panel.add(buildSetTrailingStopBlock());
        panel.add(buildSetStopOrderTypeBlock());
        panel.add(buildSetStopOrderOffsetBlock());
        panel.add(buildSetStopOrderDurationBlock());
        panel.add(buildSetOrderDurationBlock());
        panel.add(buildGetTcpStateBlock());

        JScrollPane scrollPane = new JScrollPane(panel);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());

        JPanel wrapper = new JPanel();
        wrapper.setLayout(new BoxLayout(wrapper, BoxLayout.Y_AXIS));
        wrapper.add(scrollPane);
        wrapper.setVisible(isWorking.get());
        return wrapper;
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

            String accountId;
            try {
                accountId = ((AccountInfo) comboBoxAccounts.getSelectedItem()).id;
            } catch (ClassCastException exception) {
                JOptionPane.showMessageDialog(null, "Selected Account cannot be empty!", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

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

        block.add(getBracketsBtn);
        block.add(getOcoBtn);
        block.add(getTrailingStopBtn);
        block.add(getStopOrderTypeBtn);
        block.add(getStopOrderOffsetBtn);
        block.add(getStopOrderDurationBtn);
        block.add(getOrderDurationBtn);
        block.add(getOrderSizeBtn);
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
}
