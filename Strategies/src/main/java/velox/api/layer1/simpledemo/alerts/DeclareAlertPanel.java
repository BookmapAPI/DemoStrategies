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
import velox.api.layer1.messages.Layer1ApiSoundAlertMessageDeclaration;
import velox.gui.StrategyPanel;

public class DeclareAlertPanel extends StrategyPanel {
    
    interface DeclareAlertPanelCallback {
        
        void sendDeclarationMessage(Layer1ApiSoundAlertMessageDeclaration declarationMessage);
    }
    
    private static final String SOURCE_GLOBAL = "<NONE> (Global alert)";
    private JTextField textFieldDeclarationName;
    private JTextField textFieldDescription;
    private JComboBox<String> comboBoxMatchedAlias;
    
    public DeclareAlertPanel(DeclareAlertPanelCallback callback) {
        super("Declare alert");
        GridBagLayout gridBagLayout = new GridBagLayout();
        gridBagLayout.columnWidths = new int[]{150, 0, 0};
        gridBagLayout.rowHeights = new int[]{0, 0, 0, 0, 0, 0, 0};
        gridBagLayout.columnWeights = new double[]{0.0, 1.0, Double.MIN_VALUE};
        gridBagLayout.rowWeights = new double[]{0.0, 0.0, 0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE};
        setLayout(gridBagLayout);
        
        JLabel lblDeclarationName = new JLabel("Declaration name:");
        GridBagConstraints gbc_lblDeclarationName = new GridBagConstraints();
        gbc_lblDeclarationName.anchor = GridBagConstraints.EAST;
        gbc_lblDeclarationName.insets = new Insets(0, 0, 5, 5);
        gbc_lblDeclarationName.gridx = 0;
        gbc_lblDeclarationName.gridy = 0;
        add(lblDeclarationName, gbc_lblDeclarationName);
        
        textFieldDeclarationName = new JTextField();
        GridBagConstraints gbc_textFieldDeclarationName = new GridBagConstraints();
        gbc_textFieldDeclarationName.insets = new Insets(0, 0, 5, 0);
        gbc_textFieldDeclarationName.fill = GridBagConstraints.HORIZONTAL;
        gbc_textFieldDeclarationName.gridx = 1;
        gbc_textFieldDeclarationName.gridy = 0;
        add(textFieldDeclarationName, gbc_textFieldDeclarationName);
        textFieldDeclarationName.setColumns(10);
        
        JLabel lblDescription = new JLabel("Description:");
        GridBagConstraints gbc_lblDescription = new GridBagConstraints();
        gbc_lblDescription.anchor = GridBagConstraints.EAST;
        gbc_lblDescription.insets = new Insets(0, 0, 5, 5);
        gbc_lblDescription.gridx = 0;
        gbc_lblDescription.gridy = 1;
        add(lblDescription, gbc_lblDescription);
        
        textFieldDescription = new JTextField();
        GridBagConstraints gbc_textFieldDescription = new GridBagConstraints();
        gbc_textFieldDescription.insets = new Insets(0, 0, 5, 0);
        gbc_textFieldDescription.fill = GridBagConstraints.HORIZONTAL;
        gbc_textFieldDescription.gridx = 1;
        gbc_textFieldDescription.gridy = 1;
        add(textFieldDescription, gbc_textFieldDescription);
        textFieldDescription.setColumns(10);
        
        
        JLabel lbAliasMatcher = new JLabel("Matched alias:");
        GridBagConstraints gbc_lbAliasMatcher = new GridBagConstraints();
        gbc_lbAliasMatcher.anchor = GridBagConstraints.EAST;
        gbc_lbAliasMatcher.insets = new Insets(0, 0, 5, 5);
        gbc_lbAliasMatcher.gridx = 0;
        gbc_lbAliasMatcher.gridy = 2;
        add(lbAliasMatcher, gbc_lbAliasMatcher);
    
        comboBoxMatchedAlias = new JComboBox<>();
        comboBoxMatchedAlias.addItem(SOURCE_GLOBAL);
        GridBagConstraints gbc_comboBox = new GridBagConstraints();
        gbc_comboBox.insets = new Insets(0, 0, 5, 0);
        gbc_comboBox.fill = GridBagConstraints.HORIZONTAL;
        gbc_comboBox.gridx = 1;
        gbc_comboBox.gridy = 2;
        add(comboBoxMatchedAlias, gbc_comboBox);
        
        JLabel lblNewLabel = new JLabel("Notifications:");
        GridBagConstraints gbc_lblNewLabel = new GridBagConstraints();
        gbc_lblNewLabel.anchor = GridBagConstraints.EAST;
        gbc_lblNewLabel.insets = new Insets(0, 0, 5, 5);
        gbc_lblNewLabel.gridx = 0;
        gbc_lblNewLabel.gridy = 3;
        add(lblNewLabel, gbc_lblNewLabel);
        
        JPanel panel = new JPanel();
        GridBagConstraints gbc_panel = new GridBagConstraints();
        gbc_panel.insets = new Insets(0, 0, 5, 0);
        gbc_panel.fill = GridBagConstraints.BOTH;
        gbc_panel.gridx = 1;
        gbc_panel.gridy = 3;
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
        gbc_chckbxIsRepeated.gridy = 4;
        add(chckbxIsRepeated, gbc_chckbxIsRepeated);
    
        JButton btnDeclareAlert = new JButton("Declare alert");
        btnDeclareAlert.addActionListener(e -> {
            String selectedAlias = getAlias();
            
            Layer1ApiSoundAlertMessageDeclaration declarationMessage = Layer1ApiSoundAlertMessageDeclaration
                .builder()
                .setIsAdd(true)
                .setName(textFieldDeclarationName.getText())
                .setDescription(textFieldDescription.getText())
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
        gbc_btnNewButton.gridy = 5;
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
