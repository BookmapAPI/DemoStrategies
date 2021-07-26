package velox.api.layer1.simpledemo.alerts.manual;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.util.Optional;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import velox.api.layer1.messages.Layer1ApiSoundAlertDeclarationMessage;
import velox.api.layer1.messages.Layer1ApiSoundAlertDeclarationMessage.Builder;
import velox.gui.StrategyPanel;

public class DeclareOrUpdateAlertPanel extends StrategyPanel {
    
    interface DeclareAlertPanelCallback {
        
        void sendDeclarationMessage(Layer1ApiSoundAlertDeclarationMessage declarationMessage);
    }
    
    private interface PanelState {
        String getTriggerDescription();
        
        String getMatchedAlias();
        
        JPanel getBottomButtonsPanel();
        
        default boolean popup() {
            return false;
        }
        default boolean sound() {
            return false;
        }
        default boolean repeated() {
            return false;
        }
    }
    
    private final DeclareAlertPanelCallback callback;
    private final PanelState declareAlertState = new DeclareAlertState();
    private static final String SOURCE_GLOBAL = "<NONE> (Global alert)";
    private JTextField textFieldDescription;
    private JComboBox<String> comboBoxMatchedAlias;
    private final JCheckBox chckbxPopup;
    private final JCheckBox chckbxSound;
    private final JCheckBox chckbxIsRepeated;
    private JPanel bottomBtnsPanel;
    
    public DeclareOrUpdateAlertPanel(DeclareAlertPanelCallback callback) {
        super("Declare/Update alert");
        this.callback = callback;
        GridBagLayout gridBagLayout = new GridBagLayout();
        gridBagLayout.columnWidths = new int[]{150, 0, 0};
        gridBagLayout.rowHeights = new int[]{0, 0, 0, 0, 0, 0};
        gridBagLayout.columnWeights = new double[]{0.0, 1.0, Double.MIN_VALUE};
        gridBagLayout.rowWeights = new double[]{0.0, 0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE};
        setLayout(gridBagLayout);
        
        JLabel lblTriggerDescription = new JLabel("Trigger description:");
        GridBagConstraints gbc_lblTriggerDescription = new GridBagConstraints();
        gbc_lblTriggerDescription.anchor = GridBagConstraints.EAST;
        gbc_lblTriggerDescription.insets = new Insets(0, 0, 5, 5);
        gbc_lblTriggerDescription.gridx = 0;
        gbc_lblTriggerDescription.gridy = 0;
        add(lblTriggerDescription, gbc_lblTriggerDescription);
        
        textFieldDescription = new JTextField();
        GridBagConstraints gbc_textFieldDescription = new GridBagConstraints();
        gbc_textFieldDescription.insets = new Insets(0, 0, 5, 0);
        gbc_textFieldDescription.fill = GridBagConstraints.HORIZONTAL;
        gbc_textFieldDescription.gridx = 1;
        gbc_textFieldDescription.gridy = 0;
        add(textFieldDescription, gbc_textFieldDescription);
        textFieldDescription.setColumns(10);
        
        
        JLabel lbAliasMatcher = new JLabel("Matched alias:");
        GridBagConstraints gbc_lbAliasMatcher = new GridBagConstraints();
        gbc_lbAliasMatcher.anchor = GridBagConstraints.EAST;
        gbc_lbAliasMatcher.insets = new Insets(0, 0, 5, 5);
        gbc_lbAliasMatcher.gridx = 0;
        gbc_lbAliasMatcher.gridy = 1;
        add(lbAliasMatcher, gbc_lbAliasMatcher);
    
        comboBoxMatchedAlias = new JComboBox<>();
        comboBoxMatchedAlias.addItem(SOURCE_GLOBAL);
        GridBagConstraints gbc_comboBox = new GridBagConstraints();
        gbc_comboBox.insets = new Insets(0, 0, 5, 0);
        gbc_comboBox.fill = GridBagConstraints.HORIZONTAL;
        gbc_comboBox.gridx = 1;
        gbc_comboBox.gridy = 1;
        add(comboBoxMatchedAlias, gbc_comboBox);
        
        JLabel lblNewLabel = new JLabel("Notifications:");
        GridBagConstraints gbc_lblNewLabel = new GridBagConstraints();
        gbc_lblNewLabel.anchor = GridBagConstraints.EAST;
        gbc_lblNewLabel.insets = new Insets(0, 0, 5, 5);
        gbc_lblNewLabel.gridx = 0;
        gbc_lblNewLabel.gridy = 2;
        add(lblNewLabel, gbc_lblNewLabel);
        
        JPanel panel = new JPanel();
        GridBagConstraints gbc_panel = new GridBagConstraints();
        gbc_panel.insets = new Insets(0, 0, 5, 0);
        gbc_panel.fill = GridBagConstraints.BOTH;
        gbc_panel.gridx = 1;
        gbc_panel.gridy = 2;
        add(panel, gbc_panel);
        panel.setLayout(new GridLayout(0, 2, 0, 0));
        
        chckbxPopup = new JCheckBox("Popup");
        panel.add(chckbxPopup);
        
        chckbxSound = new JCheckBox("Sound");
        panel.add(chckbxSound);
        
        chckbxIsRepeated = new JCheckBox("Is repeated alert");
        GridBagConstraints gbc_chckbxIsRepeated = new GridBagConstraints();
        gbc_chckbxIsRepeated.anchor = GridBagConstraints.WEST;
        gbc_chckbxIsRepeated.insets = new Insets(0, 0, 5, 0);
        gbc_chckbxIsRepeated.gridx = 1;
        gbc_chckbxIsRepeated.gridy = 3;
        add(chckbxIsRepeated, gbc_chckbxIsRepeated);
    
        bottomBtnsPanel = new JPanel(new BorderLayout());
        GridBagConstraints gbc_bottomBtnsPanel = new GridBagConstraints();
        gbc_bottomBtnsPanel.fill = GridBagConstraints.BOTH;
        gbc_bottomBtnsPanel.gridx = 1;
        gbc_bottomBtnsPanel.gridy = 4;
        add(bottomBtnsPanel, gbc_bottomBtnsPanel);
        
        setState(declareAlertState);
    }
    
    private Layer1ApiSoundAlertDeclarationMessage updateMessageBuilder(Layer1ApiSoundAlertDeclarationMessage.Builder builder) {
        String selectedAlias = getAlias();
        return builder
            .setIsAdd(true)
            .setTriggerDescription(textFieldDescription.getText())
            .setPopupAllowed(chckbxPopup.isSelected())
            .setSoundAllowed(chckbxSound.isSelected())
            .setRepeated(chckbxIsRepeated.isSelected())
            .setSource(Layer1ApiAlertDemo.class)
            .setAliasMatcher((alias) -> selectedAlias == null || selectedAlias.equals(alias))
            .build();
    }
    
    public void addAlias(String alias) {
        SwingUtilities.invokeLater(() -> comboBoxMatchedAlias.addItem(alias));
    }
    
    public void removeAlias(String alias) {
        SwingUtilities.invokeLater(() -> comboBoxMatchedAlias.removeItem(alias));
    }
    
    private String getAlias() {
        String source = (String) comboBoxMatchedAlias.getSelectedItem();
        if (source.equals(SOURCE_GLOBAL)) {
            source = null;
        }
        return source;
    }
    
    public void setConfiguredDeclaration(Layer1ApiSoundAlertDeclarationMessage declaration) {
        PanelState newState = declaration == null ? declareAlertState : new UpdateAlertState(declaration);
        setState(newState);
    }
    
    private void setState(PanelState state) {
        textFieldDescription.setText(state.getTriggerDescription());
        
        String selectedItem = Optional.ofNullable(state.getMatchedAlias()).orElse(SOURCE_GLOBAL);
        comboBoxMatchedAlias.setSelectedItem(selectedItem);
        
        chckbxPopup.setSelected(state.popup());
        chckbxSound.setSelected(state.sound());
        chckbxIsRepeated.setSelected(state.repeated());
        
        bottomBtnsPanel.removeAll();
        bottomBtnsPanel.add(state.getBottomButtonsPanel(), BorderLayout.CENTER);
        
        revalidate();
        repaint();
    }
    
    private class DeclareAlertState implements PanelState {
        
        @Override
        public String getTriggerDescription() {
            return "";
        }
    
        @Override
        public String getMatchedAlias() {
            return null;
        }
    
        @Override
        public JPanel getBottomButtonsPanel() {
            JPanel bottomBtnsPanel = new JPanel();
            bottomBtnsPanel.setLayout(new BorderLayout());
    
            JButton btnDeclareAlert = new JButton("Declare new alert");
            bottomBtnsPanel.add(btnDeclareAlert, BorderLayout.CENTER);
            btnDeclareAlert.addActionListener(e -> {
                Layer1ApiSoundAlertDeclarationMessage declarationMessage = updateMessageBuilder(Layer1ApiSoundAlertDeclarationMessage.builder());
                callback.sendDeclarationMessage(declarationMessage);
            });
            
            return bottomBtnsPanel;
        }
    }
    
    private class UpdateAlertState implements PanelState {
    
        Layer1ApiSoundAlertDeclarationMessage declaration;
        
        public UpdateAlertState(Layer1ApiSoundAlertDeclarationMessage declaration) {
            this.declaration = declaration;
        }
    
        @Override
        public String getTriggerDescription() {
            return declaration.triggerDescription;
        }
    
        @Override
        public String getMatchedAlias() {
            for (int i = 0; i < comboBoxMatchedAlias.getItemCount(); i++) {
                String alias = comboBoxMatchedAlias.getItemAt(i);
                if (declaration.aliasMatcher.test(alias)) {
                    return alias;
                }
            }
            
            return null;
        }
    
        @Override
        public JPanel getBottomButtonsPanel() {
            JPanel bottomBtnsPanel = new JPanel();
            GridBagLayout gbl_bottomBtnsPanel = new GridBagLayout();
            gbl_bottomBtnsPanel.columnWidths = new int[]{0, 104, 0};
            gbl_bottomBtnsPanel.rowHeights = new int[]{21, 0};
            gbl_bottomBtnsPanel.columnWeights = new double[]{1.0, 1.0, Double.MIN_VALUE};
            gbl_bottomBtnsPanel.rowWeights = new double[]{0.0, Double.MIN_VALUE};
            bottomBtnsPanel.setLayout(gbl_bottomBtnsPanel);
    
            JButton btnReset = new JButton("Reset");
            btnReset.addActionListener(e -> setState(this));
            GridBagConstraints gbc_btnReset = new GridBagConstraints();
            gbc_btnReset.insets = new Insets(0, 0, 0, 5);
            gbc_btnReset.gridx = 0;
            gbc_btnReset.gridy = 0;
            bottomBtnsPanel.add(btnReset, gbc_btnReset);
    
            JButton btnUpdateDeclaration = new JButton("Update declaration");
            GridBagConstraints gbc_btnDeclareAlert = new GridBagConstraints();
            gbc_btnDeclareAlert.fill = GridBagConstraints.VERTICAL;
            gbc_btnDeclareAlert.gridx = 1;
            gbc_btnDeclareAlert.gridy = 0;
            bottomBtnsPanel.add(btnUpdateDeclaration, gbc_btnDeclareAlert);
            btnUpdateDeclaration.addActionListener(e -> {
                Layer1ApiSoundAlertDeclarationMessage declarationMessage = updateMessageBuilder(new Builder(declaration));
                callback.sendDeclarationMessage(declarationMessage);
            });
            
            return bottomBtnsPanel;
        }
    
        @Override
        public boolean popup() {
            return declaration.isPopupAllowed;
        }
    
        @Override
        public boolean sound() {
            return declaration.isSoundAllowed;
        }
    
        @Override
        public boolean repeated() {
            return declaration.isRepeated;
        }
    }
}
