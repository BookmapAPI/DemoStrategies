package velox.api.layer1.simpledemo.alerts.tradeprice;


import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;
import velox.api.layer1.Layer1ApiAdminAdapter;
import velox.api.layer1.Layer1ApiDataAdapter;
import velox.api.layer1.Layer1ApiFinishable;
import velox.api.layer1.Layer1ApiInstrumentAdapter;
import velox.api.layer1.Layer1ApiProvider;
import velox.api.layer1.annotations.Layer1ApiVersion;
import velox.api.layer1.annotations.Layer1ApiVersionValue;
import velox.api.layer1.annotations.Layer1Attachable;
import velox.api.layer1.annotations.Layer1StrategyName;
import velox.api.layer1.common.ListenableHelper;
import velox.api.layer1.data.InstrumentInfo;
import velox.api.layer1.data.TradeInfo;
import velox.api.layer1.messages.Layer1ApiAlertGuiMessage;
import velox.api.layer1.messages.Layer1ApiAlertSettingsMessage;
import velox.api.layer1.messages.Layer1ApiSoundAlertDeclarationMessage;
import velox.api.layer1.messages.Layer1ApiSoundAlertDeclarationMessage.Builder;
import velox.api.layer1.messages.Layer1ApiSoundAlertMessage;
import velox.api.layer1.messages.UserMessageLayersChainCreatedTargeted;
import velox.api.layer1.messages.indicators.SettingsAccess;
import velox.api.layer1.settings.Layer1ConfigSettingsInterface;
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
    Layer1ApiInstrumentAdapter,
    Layer1ConfigSettingsInterface,
    Layer1ApiFinishable,
    PriceAlertPanelCallback {

    public static final String ADDON_NAME = "Custom price alert demo";
    private final Layer1ApiProvider provider;
    private SettingsAccess settingsAccess;
    private final Map<String, TradeMatcher> declarationIdToTradeMatcher = new ConcurrentHashMap<>();
    private final Map<String, CustomDeclarationSettings> declarationIdToDeclarationSettings = new ConcurrentHashMap<>();
    private final Map<String, Layer1ApiSoundAlertDeclarationMessage> declarationIdToDeclarationMessage = new ConcurrentHashMap<>();
    private final Map<String, InstrumentInfo> aliasToInstrumentInfo = new ConcurrentHashMap<>();
    private Layer1ApiAlertGuiMessage guiMessage;
    
    private final Object alertCreationLock = new Object();
    private final AtomicBoolean isActive = new AtomicBoolean(false);

    
    public CustomPriceAlertDemo(Layer1ApiProvider provider) {
        this.provider = provider;
        ListenableHelper.addListeners(provider, this);
    }
    
    @Override
    public void onTrade(String alias, double price, int size, TradeInfo tradeInfo) {
        /*
         * Here, the price and size are not the "real" values - Bookmap passes them
         * as a number of increments. Thus, we need to transform it using
         * the pips and sizeMultiplier for a given instrument.
         */
        InstrumentInfo instrumentInfo = aliasToInstrumentInfo.get(alias);
        declarationIdToTradeMatcher.values().forEach(tradeMatcher -> {
            tradeMatcher.tryMatch(alias, price * instrumentInfo.pips, size / instrumentInfo.sizeMultiplier);
        });
    }
    
    @Override
    public void onUserMessage(Object data) {
        if (data instanceof Layer1ApiSoundAlertDeclarationMessage) {
            Layer1ApiSoundAlertDeclarationMessage declarationMessage = (Layer1ApiSoundAlertDeclarationMessage) data;
            if (declarationMessage.source == CustomPriceAlertDemo.class && !declarationMessage.isAdd) {
                synchronized (alertCreationLock) {
                    declarationIdToDeclarationMessage.remove(declarationMessage.id);
                    declarationIdToDeclarationSettings.remove(declarationMessage.id);
                    declarationIdToTradeMatcher.remove(declarationMessage.id);
                    /*
                     * During addon unload we will obtain declaration messages with
                     * isAdd = false (sent from #finish()), but we don't want to store
                     * those in settings
                     */
                    if (isActive.get()) {
                        settingsChanged();
                    }
                }
            }
        } else if (data instanceof Layer1ApiAlertSettingsMessage) {
            Layer1ApiAlertSettingsMessage settingsMessage = (Layer1ApiAlertSettingsMessage) data;
            if (settingsMessage.source == CustomPriceAlertDemo.class) {
                synchronized (alertCreationLock) {
                    CustomDeclarationSettings customDeclarationSettings = declarationIdToDeclarationSettings.get(settingsMessage.declarationId);
                    customDeclarationSettings.isPopupActive = settingsMessage.popup;
                    settingsChanged();
                }
            }
        } else if (data instanceof UserMessageLayersChainCreatedTargeted) {
            UserMessageLayersChainCreatedTargeted message = (UserMessageLayersChainCreatedTargeted) data;
            if (message.targetClass == CustomPriceAlertDemo.class) {
                synchronized (alertCreationLock) {
                    isActive.set(true);

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
            isActive.set(false);
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
            
            declarationIdToDeclarationMessage.clear();
            declarationIdToTradeMatcher.clear();
            aliasToInstrumentInfo.clear();
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
            .setPopupAllowed(declarationSettings.isPopupPossible)
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
            .setPopupAllowed(declarationSettings.isPopupPossible)
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
            settingsChanged();

            Predicate<Double> pricePredicate;
            switch (comparisonSymbol) {
                case "<": pricePredicate = price -> price < selectedPrice; break;
                case "=": pricePredicate = price -> price == selectedPrice; break;
                case ">": pricePredicate = price -> price > selectedPrice; break;
                default: throw new IllegalArgumentException("Unknown comparison symbol: " + comparisonSymbol);
            }

            /*
             * Here we first notify Bookmap about future alerts, and only then
             * put the newly created TradeMatcher instance into declarationIdToTradeMatcher Map.
             * This ensures that no alert will be sent before it is registered
             */
            provider.sendUserMessage(declarationMessage);
            Layer1ApiAlertSettingsMessage initialSettingsMessage = Layer1ApiAlertSettingsMessage.builder()
                .setSource(CustomPriceAlertDemo.class)
                .setDeclarationId(declarationMessage.id)
                .setPopup(declarationSettings.isPopupActive)
                .build();
            provider.sendUserMessage(initialSettingsMessage);
            
            // We are not interested in trades with size == 0, as in that case the size < size granularity
            TradePredicate tradePredicate = (alias, price, size) -> size != 0 && pricePredicate.test(price);

            OnMatchCallback onMatchCallback = (alias, price, size) -> {
                CustomDeclarationSettings currentSettings = declarationIdToDeclarationSettings.get(declarationMessage.id);
                Layer1ApiSoundAlertMessage soundAlertMessage = Layer1ApiSoundAlertMessage.builder()
                    .setAlias(alias)
                    .setAlertDeclarationId(declarationMessage.id)
                    .setTextInfo(String.format("Trade actual price={%.2f}, size={%.2f}%n", price, size))
                    .setAdditionalInfo(declarationMessage.triggerDescription)
                    .setSource(CustomPriceAlertDemo.class)
                    .setShowPopup(currentSettings.isPopupActive)
                    .build();
                provider.sendUserMessage(soundAlertMessage);
            };
        
            TradeMatcher tradeMatcher = new TradeMatcher(tradePredicate, onMatchCallback);
            declarationIdToTradeMatcher.put(declarationMessage.id, tradeMatcher);
        }
    }
    
    private String getTriggerDescription(CustomDeclarationSettings declarationSettings) {
        return String.format("Trade with price %s %d",
            declarationSettings.comparisonSymbol,
            declarationSettings.selectedPrice);
    }
    
    @Override
    public void onInstrumentAdded(String alias, InstrumentInfo instrumentInfo) {
        aliasToInstrumentInfo.put(alias, instrumentInfo);
    }
    
    @Override
    public void onInstrumentRemoved(String alias) {
        aliasToInstrumentInfo.remove(alias);
    }

    @Override
    public void acceptSettingsInterface(SettingsAccess settingsAccess) {
        synchronized (alertCreationLock) {
            this.settingsAccess = settingsAccess;
            restoreSettings();
        }
    }

    /**
     * {@link CustomPriceAlertDemoSettings} is used to create/update alerts. Here we get the saved version
     * from Bookmap and restore the alerts
     */
    private void restoreSettings() {
        CustomPriceAlertDemoSettings settings = (CustomPriceAlertDemoSettings) settingsAccess.getSettings("", ADDON_NAME, CustomPriceAlertDemoSettings.class);
        Optional.ofNullable(settings)
                .map(CustomPriceAlertDemoSettings::getDeclarationSettings)
                .ifPresent(declarationSettings -> declarationSettings.forEach(this::onCreateAlert));
    }


    /**
     * On each change of the alerts created by this addon we update the settings
     */
    private void settingsChanged() {
        List<CustomDeclarationSettings> customDeclarationSettings = new ArrayList<>(declarationIdToDeclarationSettings.values());
        settingsAccess.setSettings("", ADDON_NAME, new CustomPriceAlertDemoSettings(customDeclarationSettings), CustomPriceAlertDemoSettings.class);
    }
}
