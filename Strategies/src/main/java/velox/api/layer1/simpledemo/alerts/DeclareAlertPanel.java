package velox.api.layer1.simpledemo.alerts;

import javax.swing.JPanel;
import java.awt.GridBagLayout;
import javax.swing.JLabel;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import javax.swing.JButton;
import javax.swing.JTextField;
import java.awt.GridLayout;
import javax.swing.JCheckBox;
import velox.api.layer1.messages.Layer1ApiSoundAlertMessageDeclaration;
import velox.gui.StrategyPanel;

public class DeclareAlertPanel extends StrategyPanel {
    
    interface DeclareAlertPanelCallback {
        void sendDeclarationMessage(Layer1ApiSoundAlertMessageDeclaration declarationMessage);
    }
    
    private JTextField textFieldDeclarationName;
    private JTextField textFieldDescription;
    
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
            Layer1ApiSoundAlertMessageDeclaration declarationMessage = Layer1ApiSoundAlertMessageDeclaration
                .builder()
                .setIsAdd(true)
                .setName(textFieldDeclarationName.getText())
                .setDescription(textFieldDescription.getText())
                .setPopup(chckbxPopup.isSelected())
                .setRepeated(chckbxIsRepeated.isSelected())
                .setSource(Layer1ApiAlertDemo.class)
//                .setAliasMatcher(() -> {})
                .build();
            callback.sendDeclarationMessage(declarationMessage);
        });
        GridBagConstraints gbc_btnNewButton = new GridBagConstraints();
        gbc_btnNewButton.fill = GridBagConstraints.HORIZONTAL;
        gbc_btnNewButton.insets = new Insets(0, 0, 5, 0);
        gbc_btnNewButton.gridx = 1;
        gbc_btnNewButton.gridy = 4;
        add(btnDeclareAlert, gbc_btnNewButton);
    }
}
