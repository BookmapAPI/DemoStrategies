package velox.api.layer1.simpledemo.alerts;

import velox.gui.StrategyPanel;
import java.awt.GridBagLayout;
import javax.swing.JButton;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;

class SendAlertPanel extends StrategyPanel {
    private static final String SOURCE_GLOBAL = "<NONE> (Global alert)";
    private JComboBox<String> comboBoxAliases;
    
    static interface SendAlertPanelCallback {
        void sendSimpleAlert(long repeats, int priority);
        void sendTextOnlyAlert(long repeats, int priority);
        void sendSoundOnlyAlert(long repeats, int priority);
    }

    public SendAlertPanel(SendAlertPanelCallback callback) {
        super("Send alert");
        GridBagLayout gridBagLayout = new GridBagLayout();
        gridBagLayout.columnWidths = new int[]{0, 0, 0};
        gridBagLayout.rowHeights = new int[]{0, 0, 0, 0, 0};
        gridBagLayout.columnWeights = new double[]{0.0, 1.0, Double.MIN_VALUE};
        gridBagLayout.rowWeights = new double[]{0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE};
        setLayout(gridBagLayout);
        
        JButton btnSimpleAlert = new JButton("Send simple alert");
        btnSimpleAlert.addActionListener(e -> callback.sendSimpleAlert(1, 0));
        GridBagConstraints gbc_btnSimpleAlert = new GridBagConstraints();
        gbc_btnSimpleAlert.fill = GridBagConstraints.HORIZONTAL;
        gbc_btnSimpleAlert.insets = new Insets(0, 0, 5, 5);
        gbc_btnSimpleAlert.gridx = 0;
        gbc_btnSimpleAlert.gridy = 0;
        add(btnSimpleAlert, gbc_btnSimpleAlert);
        
        JButton btnTextAlert = new JButton("Send text-only alert");
        btnTextAlert.addActionListener(e -> callback.sendTextOnlyAlert(1, 0));
        GridBagConstraints gbc_btnTextAlert = new GridBagConstraints();
        gbc_btnTextAlert.fill = GridBagConstraints.HORIZONTAL;
        gbc_btnTextAlert.insets = new Insets(0, 0, 5, 5);
        gbc_btnTextAlert.gridx = 0;
        gbc_btnTextAlert.gridy = 1;
        add(btnTextAlert, gbc_btnTextAlert);
        
        JButton btnSoundOnlyAlert = new JButton("Sound only alert");
        GridBagConstraints gbc_btnSoundOnlyAlert = new GridBagConstraints();
        gbc_btnSoundOnlyAlert.fill = GridBagConstraints.HORIZONTAL;
        gbc_btnSoundOnlyAlert.insets = new Insets(0, 0, 5, 5);
        btnSoundOnlyAlert.addActionListener(e -> callback.sendSoundOnlyAlert(1, 0));
        gbc_btnSoundOnlyAlert.gridx = 0;
        gbc_btnSoundOnlyAlert.gridy = 2;
        add(btnSoundOnlyAlert, gbc_btnSoundOnlyAlert);
        
        JLabel lblSource = new JLabel("Source instrument:");
        GridBagConstraints gbc_lblSource = new GridBagConstraints();
        gbc_lblSource.insets = new Insets(0, 0, 0, 5);
        gbc_lblSource.anchor = GridBagConstraints.EAST;
        gbc_lblSource.gridx = 0;
        gbc_lblSource.gridy = 3;
        add(lblSource, gbc_lblSource);
        
        comboBoxAliases = new JComboBox<>();
        comboBoxAliases.addItem(SOURCE_GLOBAL);
        GridBagConstraints gbc_comboBoxSource = new GridBagConstraints();
        gbc_comboBoxSource.fill = GridBagConstraints.HORIZONTAL;
        gbc_comboBoxSource.gridx = 1;
        gbc_comboBoxSource.gridy = 3;
        add(comboBoxAliases, gbc_comboBoxSource);
    }

    public void addAlias(String alias) {
        SwingUtilities.invokeLater(() -> comboBoxAliases.addItem(alias));
    }

    public void removeAlias(String alias) {
        SwingUtilities.invokeLater(() -> comboBoxAliases.removeItem(alias));
    }
    
    public String getAlias() {
        String source = (String) comboBoxAliases.getSelectedItem();
        if (source.equals(SOURCE_GLOBAL)) {
            source = null;
        }
        return source;
    }

}
