package velox.api.layer1.simpledemo.alerts;

import velox.gui.StrategyPanel;
import java.awt.GridBagLayout;
import javax.swing.JButton;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.time.Duration;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

class SendAlertPanel extends StrategyPanel {
    private static final String SOURCE_GLOBAL = "<NONE> (Global alert)";
    private JComboBox<String> comboBoxAliases;
    private final JSpinner prioritySpinner = new JSpinner();
    private final JSpinner repeatsSpinner = new JSpinner();
    private final JSpinner delaySpinner = new JSpinner();

    static interface SendAlertPanelCallback {
        void sendSimpleAlert(long repeats, Duration repeatDelay, int priority);
        void sendTextOnlyAlert(long repeats, Duration repeatDelay, int priority);
        void sendSoundOnlyAlert(long repeats, Duration repeatDelay, int priority);
    }

    public SendAlertPanel(SendAlertPanelCallback callback) {
        super("Send alert");
        GridBagLayout gridBagLayout = new GridBagLayout();
        gridBagLayout.columnWidths = new int[]{0, 0, 0};
        gridBagLayout.rowHeights = new int[]{0, 0, 0, 0, 0, 0, 0, 0};
        gridBagLayout.columnWeights = new double[]{0.0, 1.0, Double.MIN_VALUE};
        gridBagLayout.rowWeights = new double[]{0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE};
        setLayout(gridBagLayout);
        
        JButton btnSimpleAlert = new JButton("Send simple alert");
        btnSimpleAlert.addActionListener(e -> callback.sendSimpleAlert((Long) repeatsSpinner.getValue(),
            Duration.ofMillis((Long) delaySpinner.getValue()),
            (Integer) prioritySpinner.getValue()));
        GridBagConstraints gbc_btnSimpleAlert = new GridBagConstraints();
        gbc_btnSimpleAlert.fill = GridBagConstraints.HORIZONTAL;
        gbc_btnSimpleAlert.insets = new Insets(0, 0, 5, 5);
        gbc_btnSimpleAlert.gridx = 0;
        gbc_btnSimpleAlert.gridy = 0;
        add(btnSimpleAlert, gbc_btnSimpleAlert);
        
        JButton btnTextAlert = new JButton("Send text-only alert");
        btnTextAlert.addActionListener(e -> callback.sendTextOnlyAlert((Long) repeatsSpinner.getValue(),
            Duration.ofMillis((Long) delaySpinner.getValue()), 
            (Integer) prioritySpinner.getValue()));
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
        btnSoundOnlyAlert.addActionListener(e -> callback.sendSoundOnlyAlert((Long) repeatsSpinner.getValue(),
            Duration.ofMillis((Long) delaySpinner.getValue()),
            (Integer) prioritySpinner.getValue()));
        gbc_btnSoundOnlyAlert.gridx = 0;
        gbc_btnSoundOnlyAlert.gridy = 2;
        add(btnSoundOnlyAlert, gbc_btnSoundOnlyAlert);
        
        JLabel lblSource = new JLabel("Source instrument:");
        GridBagConstraints gbc_lblSource = new GridBagConstraints();
        gbc_lblSource.insets = new Insets(0, 0, 5, 5);
        gbc_lblSource.anchor = GridBagConstraints.EAST;
        gbc_lblSource.gridx = 0;
        gbc_lblSource.gridy = 3;
        add(lblSource, gbc_lblSource);
        
        comboBoxAliases = new JComboBox<>();
        comboBoxAliases.addItem(SOURCE_GLOBAL);
        GridBagConstraints gbc_comboBoxSource = new GridBagConstraints();
        gbc_comboBoxSource.insets = new Insets(0, 0, 5, 0);
        gbc_comboBoxSource.fill = GridBagConstraints.HORIZONTAL;
        gbc_comboBoxSource.gridx = 1;
        gbc_comboBoxSource.gridy = 3;
        add(comboBoxAliases, gbc_comboBoxSource);
        
        JLabel repeatsLabel = new JLabel("Alert repeats:");
        repeatsLabel.setToolTipText("Set next alert repeats count");
        repeatsLabel.setHorizontalAlignment(SwingConstants.CENTER);
        GridBagConstraints gbc_repeatsLabel = new GridBagConstraints();
        gbc_repeatsLabel.anchor = GridBagConstraints.EAST;
        gbc_repeatsLabel.insets = new Insets(0, 0, 5, 5);
        gbc_repeatsLabel.gridx = 0;
        gbc_repeatsLabel.gridy = 4;
        add(repeatsLabel, gbc_repeatsLabel);

        repeatsSpinner.setModel(new SpinnerNumberModel(Long.valueOf(1), Long.valueOf(1), Long.valueOf(Long.MAX_VALUE), Long.valueOf(1)));
        GridBagConstraints gbc_repeatsSpinner = new GridBagConstraints();
        gbc_repeatsSpinner.fill = GridBagConstraints.HORIZONTAL;
        gbc_repeatsSpinner.insets = new Insets(0, 0, 5, 0);
        gbc_repeatsSpinner.gridx = 1;
        gbc_repeatsSpinner.gridy = 4;
        add(repeatsSpinner, gbc_repeatsSpinner);
        
        JLabel repeatDelayLabel = new JLabel("Repeat Delay, ms:");
        GridBagConstraints gbc_repeatDelayLabel = new GridBagConstraints();
        gbc_repeatDelayLabel.anchor = GridBagConstraints.EAST;
        gbc_repeatDelayLabel.insets = new Insets(0, 0, 5, 5);
        gbc_repeatDelayLabel.gridx = 0;
        gbc_repeatDelayLabel.gridy = 5;
        add(repeatDelayLabel, gbc_repeatDelayLabel);
        repeatDelayLabel.setHorizontalAlignment(SwingConstants.CENTER);
        repeatDelayLabel.setToolTipText("Set next alert repeats count");

        delaySpinner.setModel(new SpinnerNumberModel(Long.valueOf(0), Long.valueOf(0), Long.valueOf(Long.MAX_VALUE), Long.valueOf(1)));
        GridBagConstraints gbc_delaySpinner = new GridBagConstraints();
        gbc_delaySpinner.fill = GridBagConstraints.HORIZONTAL;
        gbc_delaySpinner.insets = new Insets(0, 0, 5, 0);
        gbc_delaySpinner.gridx = 1;
        gbc_delaySpinner.gridy = 5;
        add(delaySpinner, gbc_delaySpinner);
        
        JLabel priorityLabel = new JLabel("Priority:");
        GridBagConstraints gbc_priorityLabel = new GridBagConstraints();
        gbc_priorityLabel.anchor = GridBagConstraints.EAST;
        gbc_priorityLabel.insets = new Insets(0, 0, 0, 5);
        gbc_priorityLabel.gridx = 0;
        gbc_priorityLabel.gridy = 6;
        add(priorityLabel, gbc_priorityLabel);
        priorityLabel.setToolTipText("Set next alert priority");
        
        GridBagConstraints gbc_prioritySpinner = new GridBagConstraints();
        gbc_prioritySpinner.fill = GridBagConstraints.HORIZONTAL;
        gbc_prioritySpinner.gridx = 1;
        gbc_prioritySpinner.gridy = 6;
        add(prioritySpinner, gbc_prioritySpinner);
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
