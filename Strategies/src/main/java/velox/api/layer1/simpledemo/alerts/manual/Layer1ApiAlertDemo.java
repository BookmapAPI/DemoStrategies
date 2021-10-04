package velox.api.layer1.simpledemo.alerts.manual;

import java.util.HashSet;
import java.util.Set;

import java.util.concurrent.ConcurrentHashMap;
import javax.swing.SwingUtilities;
import velox.api.layer1.Layer1ApiAdminAdapter;
import velox.api.layer1.Layer1ApiFinishable;
import velox.api.layer1.Layer1ApiInstrumentAdapter;
import velox.api.layer1.Layer1ApiProvider;
import velox.api.layer1.Layer1CustomPanelsGetter;
import velox.api.layer1.annotations.Layer1ApiVersion;
import velox.api.layer1.annotations.Layer1ApiVersionValue;
import velox.api.layer1.annotations.Layer1Attachable;
import velox.api.layer1.annotations.Layer1StrategyName;
import velox.api.layer1.common.ListenableHelper;
import velox.api.layer1.data.InstrumentInfo;
import velox.api.layer1.messages.Layer1ApiAlertGuiMessage;
import velox.api.layer1.messages.Layer1ApiAlertSettingsMessage;
import velox.api.layer1.messages.Layer1ApiSoundAlertMessage;
import velox.api.layer1.messages.Layer1ApiSoundAlertDeclarationMessage;
import velox.api.layer1.messages.UserMessageLayersChainCreatedTargeted;
import velox.api.layer1.simpledemo.alerts.manual.DeclareOrUpdateAlertPanel.DeclareAlertPanelCallback;
import velox.api.layer1.simpledemo.alerts.manual.SendAlertPanel.SendAlertPanelCallback;
import velox.gui.StrategyPanel;

/**
 * This demo shows you how to send sound alerts. In this demo alerts are sent
 * from UI manually (which is quite pointless), but you can send those based on
 * market events
 */
@Layer1Attachable
@Layer1StrategyName("Alert demo")
@Layer1ApiVersion(Layer1ApiVersionValue.VERSION2)
public class Layer1ApiAlertDemo implements
    Layer1CustomPanelsGetter,
    Layer1ApiFinishable,
    SendAlertPanelCallback,
    DeclareAlertPanelCallback,
    Layer1ApiInstrumentAdapter,
    Layer1ApiAdminAdapter {

    private final Layer1ApiProvider provider;

    private SendAlertPanel sendAlertPanel;
    private DeclareOrUpdateAlertPanel declareOrUpdateAlertPanel;

    private Set<String> instruments = new HashSet<>();
    private ConcurrentHashMap<String, Layer1ApiSoundAlertDeclarationMessage> registeredDeclarations = new ConcurrentHashMap<>();
    private Layer1ApiAlertGuiMessage guiDeclarationMessage;

    public Layer1ApiAlertDemo(Layer1ApiProvider provider) {
        super();
        this.provider = provider;
        
        sendAlertPanel = new SendAlertPanel(this);
        sendAlertPanel.setEnabled(false);
        
        ListenableHelper.addListeners(provider, this);
    }

    @Override
    public StrategyPanel[] getCustomGuiFor(String alias, String indicatorName) {
        synchronized (instruments) {
            instruments.forEach(sendAlertPanel::addAlias);
        }

        return new StrategyPanel[]{ sendAlertPanel };
    }

    @Override
    public void onInstrumentAdded(String alias, InstrumentInfo instrumentInfo) {
        synchronized (instruments) {
            instruments.add(alias);
            if (sendAlertPanel != null) {
                sendAlertPanel.addAlias(alias);
            }
            if (declareOrUpdateAlertPanel != null) {
                declareOrUpdateAlertPanel.addAlias(alias);
            }
        }
    }

    @Override
    public void onInstrumentRemoved(String alias) {
        synchronized (instruments) {
            instruments.remove(alias);
            if (sendAlertPanel != null) {
                sendAlertPanel.removeAlias(alias);
            }
            if (declareOrUpdateAlertPanel != null) {
                declareOrUpdateAlertPanel.removeAlias(alias);
            }
        }
    }

    @Override
    public void finish() {
        addonStateChanged(false);
    }

    private void addonStateChanged(boolean isEnabled) {
        synchronized (instruments) {
            SwingUtilities.invokeLater(() -> {
                if (isEnabled && declareOrUpdateAlertPanel == null) {
                    declareOrUpdateAlertPanel = new DeclareOrUpdateAlertPanel(this);
                    instruments.forEach(declareOrUpdateAlertPanel::addAlias);
                }
                sendAlertPanel.setEnabled(isEnabled);
            });
            
            if (isEnabled) {
                if (guiDeclarationMessage == null) {
                    guiDeclarationMessage = Layer1ApiAlertGuiMessage.builder()
                        .setSource(Layer1ApiAlertDemo.class)
                        .setGuiPanelsProvider(declaration -> {
                            declareOrUpdateAlertPanel.setConfiguredDeclaration(declaration);
                            return new StrategyPanel[]{declareOrUpdateAlertPanel};
                        })
                        .build();
                }
                provider.sendUserMessage(guiDeclarationMessage);
            } else {
                if (guiDeclarationMessage != null) {
                    Layer1ApiAlertGuiMessage removeGuiMessage = new Layer1ApiAlertGuiMessage.Builder(guiDeclarationMessage)
                        .setIsAdd(false)
                        .build();
                    provider.sendUserMessage(removeGuiMessage);
                }
                
                registeredDeclarations.values().stream()
                    .map(message -> new Layer1ApiSoundAlertDeclarationMessage.Builder(message).setIsAdd(false).build())
                    .forEach(provider::sendUserMessage);
    
                declareOrUpdateAlertPanel = null;
            }
        }
    }
    
    @Override
    public void sendCustomAlert(Layer1ApiSoundAlertMessage message) {
        provider.sendUserMessage(message);
    }
    
    @Override
    public void sendDeclarationMessage(Layer1ApiSoundAlertDeclarationMessage message) {
        synchronized (instruments) {
            registeredDeclarations.put(message.id, message);
            sendAlertPanel.addAlertDeclaration(message);
            provider.sendUserMessage(message);

            // Make popup and sound active if they are allowed by the declaration message
            Layer1ApiAlertSettingsMessage settingsMessage = Layer1ApiAlertSettingsMessage
                    .builder()
                    .setSource(message.source)
                    .setDeclarationId(message.id)
                    .setPopup(message.isPopupAllowed)
                    .setSound(message.isSoundAllowed)
                    .build();
            provider.sendUserMessage(settingsMessage);
        }
    }
    
    @Override
    public void onUserMessage(Object data) {
        if (data instanceof Layer1ApiSoundAlertDeclarationMessage) {
            Layer1ApiSoundAlertDeclarationMessage message = (Layer1ApiSoundAlertDeclarationMessage) data;
            if (message.source == Layer1ApiAlertDemo.class && !message.isAdd) {
                synchronized (instruments) {
                    registeredDeclarations.remove(message.id);
                    sendAlertPanel.removeAlertDeclaration(message);
                }
            }
        } else if (data instanceof Layer1ApiAlertSettingsMessage) {
            Layer1ApiAlertSettingsMessage message = (Layer1ApiAlertSettingsMessage) data;
            if (message.source == Layer1ApiAlertDemo.class) {
                synchronized (instruments) {
                    sendAlertPanel.updateAlertSettings(message);
                }
            }
        } else if (data instanceof UserMessageLayersChainCreatedTargeted) {
            UserMessageLayersChainCreatedTargeted message = (UserMessageLayersChainCreatedTargeted) data;
            if (message.targetClass == Layer1ApiAlertDemo.class) {
                addonStateChanged(true);
            }
        }
    }
}
