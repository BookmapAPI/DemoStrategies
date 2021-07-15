package velox.api.layer1.simpledemo.alerts.tradeprice;


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
import velox.api.layer1.messages.Layer1ApiAlertSettingsMessage;
import velox.api.layer1.messages.Layer1ApiSoundAlertDeclarationMessage;
import velox.api.layer1.messages.Layer1ApiSoundAlertDeclarationMessage.Builder;
import velox.api.layer1.messages.Layer1ApiSoundAlertMessage;

@Layer1Attachable
@Layer1StrategyName("Price alert demo")
@Layer1ApiVersion(Layer1ApiVersionValue.VERSION2)
public class SimplePriceAlertDemo implements
    Layer1ApiAdminAdapter,
    Layer1ApiDataAdapter,
    Layer1ApiFinishable {
    
    private Layer1ApiProvider provider;
    private Layer1ApiSoundAlertDeclarationMessage declarationMessage;
    private Layer1ApiAlertSettingsMessage settingsMessage;
    
    public SimplePriceAlertDemo(Layer1ApiProvider provider) {
        this.provider = provider;
    
        ListenableHelper.addListeners(provider, this);
    
        /*
         * The declaration message helps Bookmap to create controls for this alert,
         * for more info check out Layer1ApiSoundAlertDeclarationMessage javadoc
         */
        declarationMessage = Layer1ApiSoundAlertDeclarationMessage.builder()
            .setTriggerDescription("Trade price > 10")
            .setSource(SimplePriceAlertDemo.class)
            .setPopupAllowed(true)
            .setAliasMatcher(Layer1ApiSoundAlertDeclarationMessage.ALIAS_MATCH_ALL)
            .build();
        provider.sendUserMessage(declarationMessage);
    
        /*
         * The settings message determines whether an alert has sound notification,
         * alert popup. For more info check out Layer1ApiAlertSettingsMessage javadoc
         */
        settingsMessage = Layer1ApiAlertSettingsMessage
            .builder()
            .setDeclarationId(declarationMessage.id)
            .setPopup(true)
            .setSource(SimplePriceAlertDemo.class)
            .build();
        provider.sendUserMessage(settingsMessage);
    }
    
    @Override
    public void onTrade(String alias, double price, int size, TradeInfo tradeInfo) {
        if (price > 10) {
            System.out.printf("Trade of price > 10 occurred, actual price={%.2f}, size={%d}%n", price, size);
            
            /*
             * The actual alert is sent here. Note that it is connected to the
             * declaration message via alertDeclarationId, and the state of the
             * popup (on/off) is taken from the settingsMessage. And, the alert
             * message is sent AFTER the declaration and settings messages were sent
             * (from the constructor).
             *
             * If the declarationMessage is null - it means that the alert was
             * removed by a user - take a look at #onUserMessage(Object), where
             * this field is nullified if the arrived declaration message has flag
             * isAdd = false
             */
            if (declarationMessage != null) {
                Layer1ApiSoundAlertMessage soundAlertMessage = Layer1ApiSoundAlertMessage.builder()
                    .setAlias(alias)
                    .setTextInfo(String.format("Trade actual price={%.2f}, size={%d}%n", price, size))
                    .setAdditionalInfo("Trade of price > 10")
                    .setShowPopup(settingsMessage.popup)
                    .setAlertDeclarationId(declarationMessage.id)
                    .setSource(SimplePriceAlertDemo.class)
                    .build();
                provider.sendUserMessage(soundAlertMessage);
            }

        }
    }
    
    @Override
    public void onUserMessage(Object data) {
        /*
         * We need to listen for Layer1ApiSoundAlertDeclarationMessage - as the alert
         * might be removed by the user
         * And for the Layer1ApiAlertSettingsMessage - as settings can be changed
         * by the user
         */
        if (data instanceof Layer1ApiSoundAlertDeclarationMessage) {
            Layer1ApiSoundAlertDeclarationMessage obtainedDeclarationMessage = (Layer1ApiSoundAlertDeclarationMessage) data;
            if (obtainedDeclarationMessage.source == SimplePriceAlertDemo.class && !obtainedDeclarationMessage.isAdd) {
                declarationMessage = null;
            }
        } else if (data instanceof Layer1ApiAlertSettingsMessage) {
            Layer1ApiAlertSettingsMessage obtainedSettingsMessage = (Layer1ApiAlertSettingsMessage) data;
            if (obtainedSettingsMessage.source == SimplePriceAlertDemo.class) {
                settingsMessage = (Layer1ApiAlertSettingsMessage) data;
            }
        }
    }
    
    @Override
    public void finish() {
        /*
         * Although Bookmap will try its best to remove any stale alerts
         * when your addon is unloaded, it is a good practice to take care of the
         * resources you used.
         * Also, it is a suitable place to show how you can remove the alert declaration
         */
        if (declarationMessage != null) {
            Layer1ApiSoundAlertDeclarationMessage removeDeclarationMessage = new Builder(declarationMessage)
                .setIsAdd(false)
                .build();
            provider.sendUserMessage(removeDeclarationMessage);
        }
    }
}
