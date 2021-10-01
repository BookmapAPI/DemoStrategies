package velox.api.layer1.simpledemo.alerts.tradeprice;

/**
 * Helper class to restore the settings used for alert creation
 */
public class CustomDeclarationSettings {
    
    String comparisonSymbol;
    int selectedPrice;
    boolean isPopupPossible;
    boolean isPopupActive;

    /** Instances of this class are serialized/deserialized by the Bookmap,
     * thus we heed to provide a default constructor */
    public CustomDeclarationSettings() {
    }

    public CustomDeclarationSettings(String comparisonSymbol, int selectedPrice, boolean isPopupPossible) {
        this.comparisonSymbol = comparisonSymbol;
        this.selectedPrice = selectedPrice;
        // If popup is possible - also make it active
        this.isPopupPossible = isPopupPossible;
        this.isPopupActive = isPopupPossible;
    }
}
