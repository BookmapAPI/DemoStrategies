package velox.api.layer1.simpledemo.alerts.tradeprice;


import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import velox.api.layer1.Layer1ApiAdminAdapter;
import velox.api.layer1.Layer1ApiDataAdapter;
import velox.api.layer1.Layer1ApiFinishable;
import velox.api.layer1.Layer1ApiProvider;
import velox.api.layer1.annotations.Layer1ApiVersion;
import velox.api.layer1.annotations.Layer1ApiVersionValue;
import velox.api.layer1.annotations.Layer1Attachable;
import velox.api.layer1.annotations.Layer1StrategyName;
import velox.api.layer1.common.ListenableHelper;
import velox.api.layer1.data.TradeInfo;
import velox.api.layer1.messages.Layer1ApiAlertGuiMessage;
import velox.api.layer1.messages.Layer1ApiAlertSettingsMessage;
import velox.api.layer1.messages.Layer1ApiSoundAlertDeclarationMessage;
import velox.api.layer1.messages.Layer1ApiSoundAlertDeclarationMessage.Builder;
import velox.api.layer1.messages.Layer1ApiSoundAlertMessage;
import velox.api.layer1.simpledemo.alerts.tradeprice.PriceAlertPanel.PriceAlertPanelCallback;
import velox.api.layer1.simpledemo.alerts.tradeprice.TradeMatcher.OnMatchCallback;
import velox.api.layer1.simpledemo.alerts.tradeprice.TradeMatcher.TradePredicate;
import velox.gui.StrategyPanel;

/**
 * <p>This is an example addon, serving as a more elaborate illustration on how you can
 * use your custom GUI panels to manage alerts</p>
 * <p>If you are only stating with the Bookmap notification system API, it is
 * recommended to check the {@link Layer1ApiSoundAlertMessage} javadoc, and then
 * return to this example.</p>
 *
 * <p>With this addon you can setup an alert for trades of price greater-than / equal-to / less-than
 * some custom target price</p>
 */
@Layer1Attachable
@Layer1StrategyName("Custom price alert demo")
@Layer1ApiVersion(Layer1ApiVersionValue.VERSION2)
public class CustomPriceAlertDemo implements
    Layer1ApiAdminAdapter,
    Layer1ApiDataAdapter,
    Layer1ApiFinishable,
    PriceAlertPanelCallback {
    
    private final Layer1ApiProvider provider;
    private final Map<String, TradeMatcher> declarationIdToTradeMatcher = new ConcurrentHashMap<>();
    private final Map<String, CustomDeclarationSettings> declarationIdToDeclarationSettings = new ConcurrentHashMap<>();
    private final Map<String, Layer1ApiSoundAlertDeclarationMessage> declarationIdToDeclarationMessage = new ConcurrentHashMap<>();
    private final Map<String, Layer1ApiAlertSettingsMessage> declarationIdToAlertSettingsMessage = new ConcurrentHashMap<>();
    private final Layer1ApiAlertGuiMessage guiMessage;
    
    private final Object alertCreationLock = new Object();
    
    
    public CustomPriceAlertDemo(Layer1ApiProvider provider) {
        this.provider = provider;
    
        ListenableHelper.addListeners(provider, this);
        
        guiMessage = Layer1ApiAlertGuiMessage.builder()
            .setSource(CustomPriceAlertDemo.class)
            .setGuiPanelsProvider(declarationMessage -> {
                /* declarationMessage != null means that a user wants to update
                 * this declaration, otherwise they want to create a new one
                 */
                String declarationId = declarationMessage != null ? declarationMessage.id : null;
                CustomDeclarationSettings storedDeclarationSettings = declarationId != null
                    ? declarationIdToDeclarationSettings.get(declarationId)
                    : null;
                return new StrategyPanel[]{new PriceAlertPanel(this, storedDeclarationSettings, declarationId)};
            })
            .build();
        provider.sendUserMessage(guiMessage);
    }
    
    @Override
    public void onTrade(String alias, double price, int size, TradeInfo tradeInfo) {
        declarationIdToTradeMatcher.values().forEach(tradeMatcher -> tradeMatcher.tryMatch(alias, price, size));
    }
    
    @Override
    public void onUserMessage(Object data) {
        synchronized (alertCreationLock) {
            if (data instanceof Layer1ApiSoundAlertDeclarationMessage) {
                Layer1ApiSoundAlertDeclarationMessage declarationMessage = (Layer1ApiSoundAlertDeclarationMessage) data;
                if (declarationMessage.source == CustomPriceAlertDemo.class && !declarationMessage.isAdd) {
                    declarationIdToDeclarationMessage.remove(declarationMessage.id);
                    declarationIdToDeclarationSettings.remove(declarationMessage.id);
                    declarationIdToAlertSettingsMessage.remove(declarationMessage.id);
                    declarationIdToTradeMatcher.remove(declarationMessage.id);
                }
            } else if (data instanceof Layer1ApiAlertSettingsMessage) {
                Layer1ApiAlertSettingsMessage settingsMessage = (Layer1ApiAlertSettingsMessage) data;
                if (settingsMessage.source == CustomPriceAlertDemo.class) {
                    declarationIdToAlertSettingsMessage.put(settingsMessage.declarationId, settingsMessage);
                }
            }
        }
    }
    
    @Override
    public void finish() {
        /*
        * Although Bookmap will try its best to remove any stale alerts
        * when your addon is unloaded, it is a good practice to take care of the
        * resources you used.
        * Also, it is a suitable place to show how you can remove an alert declaration,
        * or GUI panels from your addon
        */
        synchronized (alertCreationLock) {
            declarationIdToDeclarationMessage.values()
                .stream()
                .map(Layer1ApiSoundAlertDeclarationMessage.Builder::new)
                .map(builder -> builder.setIsAdd(false))
                .map(Layer1ApiSoundAlertDeclarationMessage.Builder::build)
                .forEach(provider::sendUserMessage);
    
            Layer1ApiAlertGuiMessage removeGuiMessage = new Layer1ApiAlertGuiMessage.Builder(guiMessage)
                .setIsAdd(false)
                .build();
            provider.sendUserMessage(removeGuiMessage);
            
            declarationIdToAlertSettingsMessage.clear();
            declarationIdToDeclarationMessage.clear();
            declarationIdToTradeMatcher.clear();
            declarationIdToAlertSettingsMessage.clear();
        }
    }
    
    @Override
    public void onCreateAlert(CustomDeclarationSettings declarationSettings) {
         /*
         * When an alert is _created_, we compose a new declaration and settings
         * message for it, and send it to Bookmap.
         */
        Layer1ApiSoundAlertDeclarationMessage declarationMessage = Layer1ApiSoundAlertDeclarationMessage
            .builder()
            .setAliasMatcher(alias -> true)
            .setSource(CustomPriceAlertDemo.class)
            .setPopupAllowed(declarationSettings.withPopup)
            .setTriggerDescription(getTriggerDescription(declarationSettings))
            .build();
        createOrUpdateAlertImpl(declarationMessage, declarationSettings);
    }
    
    @Override
    public void onUpdateAlert(CustomDeclarationSettings declarationSettings, String declarationId) {
         /*
         * When an alert is _updated_, we edit the previous declaration. Key
         * point is that the declaration we send to Bookmap has the same id
         * as the one we update.
         */
        Layer1ApiSoundAlertDeclarationMessage existingDeclarationMessage = declarationIdToDeclarationMessage.get(declarationId);
        Layer1ApiSoundAlertDeclarationMessage updatedDeclaration = new Builder(existingDeclarationMessage)
            .setPopupAllowed(declarationSettings.withPopup)
            .setTriggerDescription(getTriggerDescription(declarationSettings))
            .build();
        createOrUpdateAlertImpl(updatedDeclaration, declarationSettings);
    }
    
    private void createOrUpdateAlertImpl(Layer1ApiSoundAlertDeclarationMessage declarationMessage,
                                         CustomDeclarationSettings declarationSettings) {
        synchronized (alertCreationLock) {
            String comparisonSymbol = declarationSettings.comparisonSymbol;
            int selectedPrice = declarationSettings.selectedPrice;
            
            declarationIdToDeclarationMessage.put(declarationMessage.id, declarationMessage);
            declarationIdToDeclarationSettings.put(declarationMessage.id, declarationSettings);
        
            Layer1ApiAlertSettingsMessage settingsMessage = Layer1ApiAlertSettingsMessage.builder()
                .setSource(CustomPriceAlertDemo.class)
                .setDeclarationId(declarationMessage.id)
                .setPopup(declarationMessage.isPopupAllowed)
                .build();
            declarationIdToAlertSettingsMessage.put(declarationMessage.id, settingsMessage);
        
            TradePredicate tradePredicate;
            switch (comparisonSymbol) {
                case "<": tradePredicate = (alias, price, size) -> price < selectedPrice; break;
                case "=": tradePredicate = (alias, price, size) -> price == selectedPrice; break;
                case ">": tradePredicate = (alias, price, size) -> price > selectedPrice; break;
                default: throw new IllegalArgumentException("Unknown comparison symbol: " + comparisonSymbol);
            }
        
            OnMatchCallback onMatchCallback = (alias, price, size) -> {
                Layer1ApiSoundAlertMessage soundAlertMessage = Layer1ApiSoundAlertMessage.builder()
                    .setAlias(alias)
                    .setAlertDeclarationId(declarationMessage.id)
                    .setTextInfo(String.format("Trade actual price={%.2f}, size={%d}%n", price, size))
                    .setAdditionalInfo(getTriggerDescription(declarationSettings))
                    .setSource(CustomPriceAlertDemo.class)
                    .setShowPopup(declarationIdToAlertSettingsMessage.get(declarationMessage.id).popup)
                    .build();
                provider.sendUserMessage(soundAlertMessage);
            };
        
            TradeMatcher tradeMatcher = new TradeMatcher(tradePredicate, onMatchCallback);
            declarationIdToTradeMatcher.put(declarationMessage.id, tradeMatcher);
            provider.sendUserMessage(declarationMessage);
            provider.sendUserMessage(settingsMessage);
        }
    }
    
    private String getTriggerDescription(CustomDeclarationSettings declarationSettings) {
        return String.format("Trade with price %s %d",
            declarationSettings.comparisonSymbol,
            declarationSettings.selectedPrice);
    }
}
