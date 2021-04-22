package velox.api.layer1.simpledemo.alerts;

import java.util.HashSet;
import java.util.Set;

import java.util.concurrent.atomic.AtomicBoolean;
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
import velox.api.layer1.messages.Layer1ApiSoundAlertMessage;
import velox.api.layer1.messages.Layer1ApiSoundAlertDeclarationMessage;
import velox.api.layer1.simpledemo.alerts.DeclareAlertPanel.DeclareAlertPanelCallback;
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
    private DeclareAlertPanel declareAlertPanel;

    private Set<String> instruments = new HashSet<>();
    
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
            if (declareAlertPanel == null) {
                declareAlertPanel = new DeclareAlertPanel(this);
                declareAlertPanel.setEnabled(false);
                instruments.forEach(declareAlertPanel::addAlias);
            }
        }


        return new StrategyPanel[]{declareAlertPanel, sendAlertPanel};
    }

    @Override
    public void onInstrumentAdded(String alias, InstrumentInfo instrumentInfo) {
        synchronized (instruments) {
            instruments.add(alias);
            if (sendAlertPanel != null) {
                sendAlertPanel.addAlias(alias);
            }
            if (declareAlertPanel != null) {
                declareAlertPanel.addAlias(alias);
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
            if (declareAlertPanel != null) {
                declareAlertPanel.removeAlias(alias);
            }
        }
    }

    @Override
    public void finish() {
        synchronized (instruments) {
            if (sendAlertPanel != null) {
                sendAlertPanel.setEnabled(false);
            }
            if (declareAlertPanel != null) {
                declareAlertPanel.setEnabled(false);
            }
        }
    }

    @Override
    public void onStrategyCheckboxEnabled(String alias, boolean isEnabled) {
        synchronized (instruments) {
            this.isEnabled.set(isEnabled);
            sendAlertPanel.setEnabled(isEnabled);
            declareAlertPanel.setEnabled(isEnabled);
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
            
            sendAlertPanel.removeAlertDeclaration(message);
            if (message.isAdd) {
                sendAlertPanel.addAlertDeclaration(message);
            }
        }
    }
}
