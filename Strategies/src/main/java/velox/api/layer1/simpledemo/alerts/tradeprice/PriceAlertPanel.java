package velox.api.layer1.simpledemo.alerts.tradeprice;

import java.awt.Dimension;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JSpinner;
import velox.gui.StrategyPanel;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.Insets;

/**
 * GUI for managing the alerts of {@link CustomPriceAlertDemo}.
 *
 */
public class PriceAlertPanel extends StrategyPanel {
    
    public interface PriceAlertPanelCallback {
        void onCreateAlert(CustomDeclarationSettings declarationSettings);
        void onUpdateAlert(CustomDeclarationSettings declarationSettings, String declarationId);
    }
    
    private JComboBox<String> comparisonSymbolCombBox;
    private JSpinner priceSpinner;
    private JCheckBox withPopupChckbx;
    
    public PriceAlertPanel(PriceAlertPanelCallback callback,
                           CustomDeclarationSettings storedDeclarationSettings,
                           String declarationId) {
        super("Price alert GUI");
        initUI();
        
        if (storedDeclarationSettings != null) {
            // Pre-populate fields for alert update
            comparisonSymbolCombBox.setSelectedItem(storedDeclarationSettings.comparisonSymbol);
            priceSpinner.setValue(storedDeclarationSettings.selectedPrice);
            withPopupChckbx.setSelected(storedDeclarationSettings.isPopupPossible);
        }
        
        JButton updateAlertBtn = new JButton("Update alert");
        updateAlertBtn.addActionListener(e -> {
            callback.onUpdateAlert(getCurrentDeclarationSettings(), declarationId);
        });
        JButton createAlertBtn = new JButton("Create alert");
        createAlertBtn.addActionListener(e -> {
            callback.onCreateAlert(getCurrentDeclarationSettings());
        });
        GridBagConstraints gbc_Btn = new GridBagConstraints();
        gbc_Btn.gridwidth = 3;
        gbc_Btn.insets = new Insets(0, 0, 0, 5);
        gbc_Btn.gridx = 1;
        gbc_Btn.gridy = 2;
        
        if (storedDeclarationSettings == null) {
            add(createAlertBtn, gbc_Btn);
        } else {
            add(updateAlertBtn, gbc_Btn);
        }
    }
    
    private CustomDeclarationSettings getCurrentDeclarationSettings() {
        String comparisonSymbol = (String) comparisonSymbolCombBox.getSelectedItem();
        int selectedPrice = (Integer) priceSpinner.getValue();
        boolean withPopup = withPopupChckbx.isSelected();
        return new CustomDeclarationSettings(comparisonSymbol, selectedPrice, withPopup);
    }
    
    private void initUI() {
        GridBagLayout gridBagLayout = new GridBagLayout();
        gridBagLayout.columnWidths = new int[]{0, 0, 0, 0, 0, 0};
        gridBagLayout.rowHeights = new int[]{0, 0, 0, 0};
        gridBagLayout.columnWeights = new double[]{1.0, 0.0, 0.0, 0.0, 1.0, Double.MIN_VALUE};
        gridBagLayout.rowWeights = new double[]{0.0, 0.0, 0.0, Double.MIN_VALUE};
        setLayout(gridBagLayout);
    
        JLabel triggerDescription = new JLabel("Alert when trade price is ");
        GridBagConstraints gbc_triggerDescription = new GridBagConstraints();
        gbc_triggerDescription.anchor = GridBagConstraints.WEST;
        gbc_triggerDescription.insets = new Insets(0, 0, 5, 5);
        gbc_triggerDescription.gridx = 1;
        gbc_triggerDescription.gridy = 0;
        add(triggerDescription, gbc_triggerDescription);
    
        comparisonSymbolCombBox = new JComboBox<>(new String[]{"<", "=", ">"});
        comparisonSymbolCombBox.setEditable(false);
        comparisonSymbolCombBox.setSelectedIndex(0);
        GridBagConstraints gbc_comparisonSymbolCombBox = new GridBagConstraints();
        gbc_comparisonSymbolCombBox.insets = new Insets(0, 0, 5, 5);
        gbc_comparisonSymbolCombBox.gridx = 2;
        gbc_comparisonSymbolCombBox.gridy = 0;
        add(comparisonSymbolCombBox, gbc_comparisonSymbolCombBox);
    
        priceSpinner = new JSpinner();
        priceSpinner.setMinimumSize(new Dimension(100, priceSpinner.getMinimumSize().height));
        priceSpinner.setPreferredSize(new Dimension(100, priceSpinner.getPreferredSize().height));
        GridBagConstraints gbc_priceSpinner = new GridBagConstraints();
        gbc_priceSpinner.insets = new Insets(0, 0, 5, 5);
        gbc_priceSpinner.gridx = 3;
        gbc_priceSpinner.gridy = 0;
        add(priceSpinner, gbc_priceSpinner);
    
        JLabel withPopupLbl = new JLabel("With popup: ");
        GridBagConstraints gbc_withPopupLbl = new GridBagConstraints();
        gbc_withPopupLbl.anchor = GridBagConstraints.WEST;
        gbc_withPopupLbl.insets = new Insets(0, 0, 5, 5);
        gbc_withPopupLbl.gridx = 1;
        gbc_withPopupLbl.gridy = 1;
        add(withPopupLbl, gbc_withPopupLbl);
        
        withPopupChckbx = new JCheckBox();
        withPopupChckbx.setSelected(true);
        GridBagConstraints gbc_withPopupChckbx = new GridBagConstraints();
        gbc_withPopupChckbx.insets = new Insets(0, 0, 5, 5);
        gbc_withPopupChckbx.gridx = 2;
        gbc_withPopupChckbx.gridy = 1;
        add(withPopupChckbx, gbc_withPopupChckbx);
    }
}
