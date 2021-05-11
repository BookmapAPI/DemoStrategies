package velox.api.layer1.simpledemo.alerts;

import java.awt.Image;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.HashSet;
import java.util.Set;

import java.util.concurrent.atomic.AtomicBoolean;
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
import velox.api.layer1.common.Log;
import velox.api.layer1.data.InstrumentInfo;
import velox.api.layer1.layers.utils.SoundSynthHelper;
import velox.api.layer1.messages.Layer1ApiSoundAlertMessage;
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
    Layer1ApiInstrumentAdapter,
    Layer1ApiInstrumentSpecificEnabledStateProvider {

    private final Layer1ApiProvider provider;

    private SendAlertPanel sendAlertPanel;

    private Set<String> instruments = new HashSet<>();
    
    private AtomicBoolean isEnabled = new AtomicBoolean(false);

    public Layer1ApiAlertDemo(Layer1ApiProvider provider) {
        super();
        this.provider = provider;

        ListenableHelper.addListeners(provider, this);
    }

    @Override
    public StrategyPanel[] getCustomGuiFor(String alias, String indicatorName) {

        if (sendAlertPanel == null) {
            synchronized (instruments) {
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
        }
    }

    @Override
    public void onInstrumentRemoved(String alias) {
        synchronized (instruments) {
            instruments.remove(alias);
            if (sendAlertPanel != null) {
                sendAlertPanel.removeAlias(alias);
            }
        }
    }

    @Override
    public void finish() {
        synchronized (instruments) {
            if (sendAlertPanel != null) {
                sendAlertPanel.setEnabled(false);
            }
        }
    }

    @Override
    public void onStrategyCheckboxEnabled(String alias, boolean isEnabled) {
        synchronized (instruments) {
            this.isEnabled.set(isEnabled);
            sendAlertPanel.setEnabled(isEnabled);
        }
    }
    
    @Override
    public boolean isStrategyEnabled(String alias) {
        return isEnabled.get();
    }
    
    @Override
    public void sendSimpleAlert(long repeats, Duration repeatDelay, int priority) {
        sendAlert("Text+sound alert", null, true, true, repeats, repeatDelay, priority, null);
    }

    @Override
    public void sendTextOnlyAlert(long repeats, Duration repeatDelay, int priority) {
        sendAlert("Text only alert", null, false, true, repeats, repeatDelay, priority, null);
    }

    @Override
    public void sendSoundOnlyAlert(long repeats, Duration repeatDelay, int priority) {
        sendAlert("Sound only alert", null, true, false, repeats, repeatDelay, priority, null);
    }
    
    @Override
    public void sendTextAndAdditionalInfoAlert(String message, String additionalInfo, Image selectedIcon) {
        sendAlert(message, additionalInfo, false, true, 1, null, 0, selectedIcon);
    }
    
    @Override
    public void sendNoSoundNoPopupAlert() {
        sendAlert(null, null, false, false, 1, null, 0, null);
    }
    
    @Override
    public void sendZeroDurationSoundAlert() {
        byte[] soundData;
        try (InputStream soundFileStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("silence.wav")) {
            soundData = new byte[soundFileStream.available()];
            soundFileStream.read(soundData);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    
        Layer1ApiSoundAlertMessage data = Layer1ApiSoundAlertMessage.builder()
            .setAlias(sendAlertPanel.getAlias())
            .setTextInfo(null)
            .setAdditionalInfo(null)
            .setSound(soundData)
            .setStatusListener((alertId, status) -> Log.info("onSoundAlertStatus: " + alertId + " " + status))
            .setSource(Layer1ApiAlertDemo.class)
            .setShowPopup(false)
            .setRepeatCount(1)
            .setRepeatDelay(null)
            .setPriority(0)
            .setSeverityIcon(null)
            .build();
    
        provider.sendUserMessage(data);
    }
    
    private void sendAlert(String message, String additionalInfo, boolean playSound, boolean showPopup, long repeats, Duration repeatDelay, int priority, Image icon) {
        Layer1ApiSoundAlertMessage data = Layer1ApiSoundAlertMessage.builder()
            .setAlias(sendAlertPanel.getAlias())
            .setTextInfo(message)
            .setAdditionalInfo(additionalInfo)
            .setSound(playSound ? SoundSynthHelper.synthesize(message) : null)
            .setStatusListener((alertId, status) -> Log.info("onSoundAlertStatus: " + alertId + " " + status))
            .setSource(Layer1ApiAlertDemo.class)
            .setShowPopup(showPopup)
            .setRepeatCount(repeats)
            .setRepeatDelay(repeatDelay)
            .setPriority(priority)
            .setSeverityIcon(icon)
            .build();

        provider.sendUserMessage(data);
    }
}
