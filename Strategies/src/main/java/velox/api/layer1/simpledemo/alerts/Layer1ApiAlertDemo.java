package velox.api.layer1.simpledemo.alerts;

import java.util.HashSet;
import java.util.Set;

import velox.api.layer1.Layer1ApiFinishable;
import velox.api.layer1.Layer1ApiInstrumentAdapter;
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
import velox.api.layer1.messages.Layer1ApiSoundAlertMessage.SoundAlertStatus;
import velox.api.layer1.messages.Layer1ApiSoundAlertMessage.SoundAlertStatusListener;
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
        Layer1ApiInstrumentAdapter
{

	private final Layer1ApiProvider provider;

	private SendAlertPanel sendAlertPanel;
	
	private Set<String> instruments = new HashSet<>();

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
	            instruments.forEach(sendAlertPanel::addAlias);
            }
	    }

        return new StrategyPanel[] { sendAlertPanel };
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

	}

    @Override
    public void sendSimpleAlert() {
        Layer1ApiSoundAlertMessage data = Layer1ApiSoundAlertMessage.builder()
                .setAlias(sendAlertPanel.getAlias())
                .setTextInfo("Text+sound alert")
                .setSound(SoundSynthHelper.synthesize("Text+sound alert"))
                .setStatusListener(new SoundAlertStatusListener() {
                    @Override
                    public void onSoundAlertStatus(String alertId, SoundAlertStatus status) {
                        Log.info("onSoundAlertStatus: " + alertId + " " + status);
                    }
                })
                .setSource(Layer1ApiAlertDemo.class)
                .setShowPopup(true)
                .build();
        provider.sendUserMessage(data);
    }

    @Override
    public void sendTextOnlyAlert() {
        Layer1ApiSoundAlertMessage data = Layer1ApiSoundAlertMessage.builder()
                .setAlias(sendAlertPanel.getAlias())
                .setTextInfo("Text only alert")
                .setStatusListener(new SoundAlertStatusListener() {
                    @Override
                    public void onSoundAlertStatus(String alertId, SoundAlertStatus status) {
                        Log.info("onSoundAlertStatus: " + alertId + " " + status);
                    }
                })
                .setSource(Layer1ApiAlertDemo.class)
                .setShowPopup(true)
                .build();
        
        provider.sendUserMessage(data);
    }

    @Override
    public void sendSoundOnlyAlert() {
        Layer1ApiSoundAlertMessage data = Layer1ApiSoundAlertMessage.builder()
                .setAlias(sendAlertPanel.getAlias())
                .setSound(SoundSynthHelper.synthesize("Sound only alert"))
                .setStatusListener(new SoundAlertStatusListener() {
                    @Override
                    public void onSoundAlertStatus(String alertId, SoundAlertStatus status) {
                        Log.info("onSoundAlertStatus: " + alertId + " " + status);
                    }
                })
                .setSource(Layer1ApiAlertDemo.class)
                .build();
        provider.sendUserMessage(data);
    }
}
