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
    
        declarationMessage = Layer1ApiSoundAlertDeclarationMessage.builder()
            .setTriggerDescription("Trade price > 10")
            .setSource(SimplePriceAlertDemo.class)
            .setPopupAllowed(true)
            .setAliasMatcher(Layer1ApiSoundAlertDeclarationMessage.ALIAS_MATCH_ALL)
            .build();
        provider.sendUserMessage(declarationMessage);
    
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
    
    @Override
    public void onUserMessage(Object data) {
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
    }
}
