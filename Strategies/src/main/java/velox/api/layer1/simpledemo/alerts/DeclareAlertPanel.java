package velox.api.layer1.simpledemo.alerts;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import velox.api.layer1.messages.Layer1ApiSoundAlertDeclarationMessage;
import velox.gui.StrategyPanel;

public class DeclareAlertPanel extends StrategyPanel {
    
    interface DeclareAlertPanelCallback {
        
        void sendDeclarationMessage(Layer1ApiSoundAlertDeclarationMessage declarationMessage);
    }
    
    private static final String SOURCE_GLOBAL = "<NONE> (Global alert)";
    private JTextField textFieldDescription;
    private JComboBox<String> comboBoxMatchedAlias;
    
    public DeclareAlertPanel(DeclareAlertPanelCallback callback) {
        super("Declare alert");
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
        
        JCheckBox chckbxPopup = new JCheckBox("Popup");
        panel.add(chckbxPopup);
        
        JCheckBox chckbxSound = new JCheckBox("Sound");
        panel.add(chckbxSound);
        
        JCheckBox chckbxIsRepeated = new JCheckBox("Is repeated alert");
        GridBagConstraints gbc_chckbxIsRepeated = new GridBagConstraints();
        gbc_chckbxIsRepeated.anchor = GridBagConstraints.WEST;
        gbc_chckbxIsRepeated.insets = new Insets(0, 0, 5, 0);
        gbc_chckbxIsRepeated.gridx = 1;
        gbc_chckbxIsRepeated.gridy = 3;
        add(chckbxIsRepeated, gbc_chckbxIsRepeated);
    
        JButton btnDeclareAlert = new JButton("Declare alert");
        btnDeclareAlert.addActionListener(e -> {
            String selectedAlias = getAlias();
            
            Layer1ApiSoundAlertDeclarationMessage declarationMessage = Layer1ApiSoundAlertDeclarationMessage
                .builder()
                .setIsAdd(true)
                .setTriggerDescription(textFieldDescription.getText())
                .setPopup(chckbxPopup.isSelected())
                .setRepeated(chckbxIsRepeated.isSelected())
                .setSource(Layer1ApiAlertDemo.class)
                .setAliasMatcher((alias) -> selectedAlias == null || selectedAlias.equals(alias))
                .build();
            callback.sendDeclarationMessage(declarationMessage);
        });
        GridBagConstraints gbc_btnNewButton = new GridBagConstraints();
        gbc_btnNewButton.fill = GridBagConstraints.HORIZONTAL;
        gbc_btnNewButton.gridx = 1;
        gbc_btnNewButton.gridy = 4;
        add(btnDeclareAlert, gbc_btnNewButton);
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
    
}
