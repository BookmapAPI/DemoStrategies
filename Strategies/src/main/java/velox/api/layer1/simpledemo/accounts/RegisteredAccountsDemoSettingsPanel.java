package velox.api.layer1.simpledemo.accounts;

import velox.api.layer1.messages.TradingAccountsInfoMessage;
import velox.gui.StrategyPanel;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Settings panel that displays a table of registered trading accounts built from two collections:
 * <ul>
 *   <li>alias-keyed accounts (where account source comes from an instrument alias)</li>
 *   <li>provider-keyed accounts (where account source comes from a provider programmatic name)</li>
 * </ul>
 * Columns: Account ID | Source (alias / provider) | State (Added / Removed)
 */
public class RegisteredAccountsDemoSettingsPanel extends StrategyPanel {

    private static final String[] COLUMN_NAMES = {
            "Account ID",
            "Source",
            "State"
    };

    private final Collection<TradingAccountsInfoMessage> aliasAccounts;
    private final Collection<TradingAccountsInfoMessage> providerAccounts;
    private final AtomicBoolean isWorking;

    private static final String CARD_WORKING = "working";
    private static final String CARD_DISABLED = "disabled";

    private final DefaultTableModel tableModel;
    private final CardLayout cardLayout;
    private final JPanel cardPanel;

    /**
     *
     * @param name              panel title
     * @param aliasAccounts  collection of alias-keyed account messages
     * @param providerAccounts collection of provider-keyed account messages
     * @param isWorking         flag indicating whether the strategy is active
     * @return a new panel instance
     */
    public static RegisteredAccountsDemoSettingsPanel newPanel(
            String name,
            Collection<TradingAccountsInfoMessage> aliasAccounts,
            Collection<TradingAccountsInfoMessage> providerAccounts,
            AtomicBoolean isWorking) {
        return new RegisteredAccountsDemoSettingsPanel(name, aliasAccounts, providerAccounts, isWorking);
    }

    private RegisteredAccountsDemoSettingsPanel(
            String name,
            Collection<TradingAccountsInfoMessage> aliasAccounts,
            Collection<TradingAccountsInfoMessage> providerAccounts,
            AtomicBoolean isWorking) {
        super(name);
        this.aliasAccounts = aliasAccounts;
        this.providerAccounts = providerAccounts;
        this.isWorking = isWorking;

        setLayout(new BorderLayout());

        tableModel = new DefaultTableModel(COLUMN_NAMES, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        JTable table = new JTable(tableModel);
        table.setFillsViewportHeight(true);

        cardLayout = new CardLayout();
        cardPanel = new JPanel(cardLayout);
        cardPanel.add(new JScrollPane(table), CARD_WORKING);
        cardPanel.add(createDisabledPanel(), CARD_DISABLED);
        cardPanel.setPreferredSize(new Dimension(500, 600));
        cardPanel.setMinimumSize(new Dimension(500, 600));

        add(cardPanel, BorderLayout.CENTER);

        updateIfNeeded();
    }

    private JComponent createDisabledPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel label = new JLabel(String.format("<html>Enable the '%s' checkbox to activate<br>this add-on.</html>", RegisteredAccountsDemo.NAME));
        panel.add(label);
        return panel;
    }

    /**
     * Refreshes visibility and table contents based on the current state of the maps and
     * the {@code isWorking} flag.
     */
    public void updateIfNeeded() {
        boolean active = isWorking.get();
        cardLayout.show(cardPanel, active ? CARD_WORKING : CARD_DISABLED);

        if (active) {
            rebuildTable();
        }
    }

    private void rebuildTable() {
        tableModel.setRowCount(0);

        List<TradingAccountsInfoMessage> accounts = new ArrayList<>(aliasAccounts);
        accounts.addAll(providerAccounts);

        for (TradingAccountsInfoMessage msg : accounts) {
            String accountId = msg.accountInfo.id;
            String source = msg.alias != null ? msg.alias : msg.accountInfo.providerProgrammaticName;
            String state = msg.isAdd ? "Added" : "Removed";

            tableModel.addRow(new Object[]{accountId, source, state});
        }

        tableModel.fireTableDataChanged();
        revalidate();
        repaint();
    }
}
