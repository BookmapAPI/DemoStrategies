package velox.api.layer1.simpledemo.alerts;

import java.util.HashSet;
import java.util.Set;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.swing.SwingUtilities;
import velox.api.layer1.Layer1ApiAdminAdapter;
import velox.api.layer1.Layer1ApiFinishable;
import velox.api.layer1.Layer1ApiInstrumentAdapter;
import velox.api.layer1.Layer1ApiInstrumentSpecificEnabledStateProvider;
import velox.api.layer1.Layer1ApiProvider;
import velox.api.layer1.Layer1CustomPanelsGetter;
import velox.api.layer1.annotations.Layer1ApiVersion;
import velox.api.layer1.annotations.Layer1ApiVersionValue;
import velox.api.layer1.annotations.Layer1Attachable;
import velox.api.layer1.annotations.Layer1StrategyName;
import velox.api.layer1.common.ListenableHelper;
import velox.api.layer1.data.InstrumentInfo;
import velox.api.layer1.messages.Layer1ApiAlertGuiMessage;
import velox.api.layer1.messages.Layer1ApiSoundAlertMessage;
import velox.api.layer1.messages.Layer1ApiSoundAlertDeclarationMessage;
import velox.api.layer1.simpledemo.alerts.DeclareOrUpdateAlertPanel.DeclareAlertPanelCallback;
import velox.api.layer1.simpledemo.alerts.SendAlertPanel.SendAlertPanelCallback;
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
    Layer1ApiAdminAdapter,
    Layer1ApiInstrumentSpecificEnabledStateProvider {

    private final Layer1ApiProvider provider;

    private SendAlertPanel sendAlertPanel;
    private DeclareOrUpdateAlertPanel declareOrUpdateAlertPanel;

    private Set<String> instruments = new HashSet<>();
    private ConcurrentHashMap<String, Layer1ApiSoundAlertDeclarationMessage> registeredDeclarations = new ConcurrentHashMap<>();
    private Layer1ApiAlertGuiMessage guiDeclarationMessage;
    
    private AtomicBoolean isEnabled = new AtomicBoolean(false);

    public Layer1ApiAlertDemo(Layer1ApiProvider provider) {
        super();
        this.provider = provider;

        ListenableHelper.addListeners(provider, this);
    }

    @Override
    public StrategyPanel[] getCustomGuiFor(String alias, String indicatorName) {
        synchronized (instruments) {
            if (sendAlertPanel == null) {
                sendAlertPanel = new SendAlertPanel(this);
                sendAlertPanel.setEnabled(false);
                instruments.forEach(sendAlertPanel::addAlias);
            }
        }

        return new StrategyPanel[]{sendAlertPanel};
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
        synchronized (instruments) {
            if (sendAlertPanel != null) {
                sendAlertPanel.setEnabled(false);
            }
            if (declareOrUpdateAlertPanel != null) {
                declareOrUpdateAlertPanel.setEnabled(false);
            }
        }
    }

    @Override
    public void onStrategyCheckboxEnabled(String alias, boolean isEnabled) {
        synchronized (instruments) {
            this.isEnabled.set(isEnabled);
            SwingUtilities.invokeLater(() -> {
                sendAlertPanel.setEnabled(isEnabled);
                declareOrUpdateAlertPanel.setEnabled(isEnabled);
            });
        
            if (isEnabled && declareOrUpdateAlertPanel == null) {
                declareOrUpdateAlertPanel = new DeclareOrUpdateAlertPanel(this);
                instruments.forEach(declareOrUpdateAlertPanel::addAlias);
            }
        }
    
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
                .map(message -> message.getClonedBuilder().setIsAdd(false).build())
                .forEach(provider::sendUserMessage);
        }
    }
    
    @Override
    public boolean isStrategyEnabled(String alias) {
        return isEnabled.get();
    }
    
    @Override
    public void sendCustomAlert(Layer1ApiSoundAlertMessage message) {
        provider.sendUserMessage(message);
    }
    
    @Override
    public void sendDeclarationMessage(Layer1ApiSoundAlertDeclarationMessage declarationMessage) {
        provider.sendUserMessage(declarationMessage);
    }
    
    @Override
    public void onUserMessage(Object data) {
        if (data instanceof Layer1ApiSoundAlertDeclarationMessage) {
            Layer1ApiSoundAlertDeclarationMessage message = (Layer1ApiSoundAlertDeclarationMessage) data;
            
            if (message.isAdd) {
                Layer1ApiSoundAlertDeclarationMessage previousMessage = registeredDeclarations.put(message.declarationId, message);
                if (previousMessage == null) {
                    sendAlertPanel.addAlertDeclaration(message);
                }
            } else {
                registeredDeclarations.remove(message.declarationId);
                sendAlertPanel.removeAlertDeclaration(message);
            }
        }
    }
}
