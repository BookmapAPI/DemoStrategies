package velox.api.layer1.simpledemo.alerts.tradeprice;

/**
 * Helper class to restore the settings used for alert creation
 */
public class CustomDeclarationSettings {
    
    String comparisonSymbol;
    int selectedPrice;
    boolean withPopup;
    
    public CustomDeclarationSettings(String comparisonSymbol, int selectedPrice, boolean withPopup) {
        this.comparisonSymbol = comparisonSymbol;
        this.selectedPrice = selectedPrice;
        this.withPopup = withPopup;
    }
}
