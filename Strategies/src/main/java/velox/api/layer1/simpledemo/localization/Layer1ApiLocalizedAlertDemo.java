package velox.api.layer1.simpledemo.localization;

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
import velox.api.layer1.annotations.Layer1DuplicateInstrumentsAware;
import velox.api.layer1.annotations.Layer1LocalizationBundle;
import velox.api.layer1.annotations.Layer1StrategyName;
import velox.api.layer1.common.ListenableHelper;
import velox.api.layer1.data.InstrumentInfo;
import velox.api.layer1.localization.Layer1LocalizationInterface;
import velox.api.layer1.localization.LocalizedBundleProvider;
import velox.api.layer1.messages.Layer1ApiAlertGuiMessage;
import velox.api.layer1.messages.Layer1ApiAlertSettingsMessage;
import velox.api.layer1.messages.Layer1ApiSoundAlertDeclarationMessage;
import velox.api.layer1.messages.Layer1ApiSoundAlertMessage;
import velox.api.layer1.messages.UserMessageLayersChainCreatedTargeted;
import velox.api.layer1.simpledemo.localization.LocalizedDeclareOrUpdateAlertPanel.DeclareAlertPanelCallback;
import velox.api.layer1.simpledemo.localization.LocalizedSendAlertPanel.SendAlertPanelCallback;
import velox.gui.StrategyPanel;
import velox.gui.utils.localization.LocalizedBundle;

/**
 * This demo is identical to {@link velox.api.layer1.simpledemo.alerts.manual.Layer1ApiAlertDemo}, but with localization support.
 * See also {@link LocalizedSendAlertPanel} and {@link LocalizedDeclareOrUpdateAlertPanel} for more details.
 * Explore this demo to understand how to localize your addon.
 */
@Layer1Attachable
@Layer1LocalizationBundle(Layer1ApiLocalizedAlertDemo.LOCALIZED_BUNDLE_NAME)
@Layer1StrategyName(value = "Alert localized demo", localizationKey = "Layer1ApiLocalizedAlertDemo.Name")
@Layer1ApiVersion(Layer1ApiVersionValue.VERSION2)
@Layer1DuplicateInstrumentsAware()
public class Layer1ApiLocalizedAlertDemo implements
    Layer1CustomPanelsGetter,
    Layer1ApiFinishable,
    SendAlertPanelCallback,
    DeclareAlertPanelCallback,
    Layer1ApiInstrumentAdapter,
    Layer1ApiAdminAdapter,
    Layer1LocalizationInterface {
    
    public static final String LOCALIZED_BUNDLE_NAME = "resources.locale.LocalizedAlertDemoBundle";

    private final Layer1ApiProvider provider;
    
    private final Set<String> instruments = new HashSet<>();
    private final ConcurrentHashMap<String, Layer1ApiSoundAlertDeclarationMessage> registeredDeclarations = new ConcurrentHashMap<>();
    
    private LocalizedSendAlertPanel sendAlertPanel;
    private LocalizedDeclareOrUpdateAlertPanel declareOrUpdateAlertPanel;
    
    private Layer1ApiAlertGuiMessage guiDeclarationMessage;
    
    private LocalizedBundle localizedBundle;

    public Layer1ApiLocalizedAlertDemo(Layer1ApiProvider provider) {
        super();
        this.provider = provider;
        
        ListenableHelper.addListeners(provider, this);
    }
    
    @Override
    public void acceptLocalizedBundleProvider(LocalizedBundleProvider localizedBundleProvider) {
        localizedBundle = localizedBundleProvider.getBundle(LOCALIZED_BUNDLE_NAME);
        
        sendAlertPanel = new LocalizedSendAlertPanel(this, localizedBundle);
        sendAlertPanel.setEnabled(false);
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
                    declareOrUpdateAlertPanel = new LocalizedDeclareOrUpdateAlertPanel(this, localizedBundle);
                    instruments.forEach(declareOrUpdateAlertPanel::addAlias);
                }
                sendAlertPanel.setEnabled(isEnabled);
            });
            
            if (isEnabled) {
                if (guiDeclarationMessage == null) {
                    guiDeclarationMessage = Layer1ApiAlertGuiMessage.builder()
                        .setSource(Layer1ApiLocalizedAlertDemo.class)
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
            if (message.source == Layer1ApiLocalizedAlertDemo.class && !message.isAdd) {
                synchronized (instruments) {
                    registeredDeclarations.remove(message.id);
                    sendAlertPanel.removeAlertDeclaration(message);
                }
            }
        } else if (data instanceof Layer1ApiAlertSettingsMessage) {
            Layer1ApiAlertSettingsMessage message = (Layer1ApiAlertSettingsMessage) data;
            if (message.source == Layer1ApiLocalizedAlertDemo.class) {
                synchronized (instruments) {
                    sendAlertPanel.updateAlertSettings(message);
                }
            }
        } else if (data instanceof UserMessageLayersChainCreatedTargeted) {
            UserMessageLayersChainCreatedTargeted message = (UserMessageLayersChainCreatedTargeted) data;
            if (message.targetClass == Layer1ApiLocalizedAlertDemo.class) {
                addonStateChanged(true);
            }
        }
    }
}
