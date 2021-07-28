package velox.api.layer1.simpledemo.alerts.simplegui;

import javax.swing.JLabel;
import velox.api.layer1.Layer1ApiFinishable;
import velox.api.layer1.Layer1ApiProvider;
import velox.api.layer1.annotations.Layer1ApiVersion;
import velox.api.layer1.annotations.Layer1ApiVersionValue;
import velox.api.layer1.annotations.Layer1Attachable;
import velox.api.layer1.annotations.Layer1StrategyName;
import velox.api.layer1.messages.Layer1ApiAlertGuiMessage;
import velox.api.layer1.messages.Layer1ApiAlertGuiMessage.Builder;
import velox.gui.StrategyPanel;

/**
 * A "Hello world" example of custom GUI panels for alerts configuration.
 * <p>
 * Its idea is to show how you can incorporate custom GUI into Bookmap. No alerts
 * are sent/configured here.
 * </p>
 * <p>
 * To test it, load this addon into Bookmap and open <em>File -&gt; Alerts -&gt; Configure alerts
 * -&gt; Add alert...</em>. You should see this addon in the list, and be able to open
 * the GUI panel (which only has a text message)
 * </p>
 */
@Layer1Attachable
@Layer1StrategyName("Simple alert GUI demo")
@Layer1ApiVersion(Layer1ApiVersionValue.VERSION2)
public class SimpleAlertGuiDemo implements Layer1ApiFinishable {
    
    private final Layer1ApiAlertGuiMessage guiMessage;
    private final Layer1ApiProvider provider;
    
    public SimpleAlertGuiDemo(Layer1ApiProvider provider) {
        this.provider = provider;
        
        /*
         * The key property in Layer1ApiAlertGuiMessage is the guiPanelsProvider -
         * here it returns an array with our custom panel (SimpleGuiPanel).
         * Bookmap will call this function and add the panel to the UI
         */
        guiMessage = Layer1ApiAlertGuiMessage
            .builder()
            .setSource(SimpleAlertGuiDemo.class)
            .setGuiPanelsProvider(declarationMessage -> new StrategyPanel[]{new SimpleGuiPanel()})
            .build();
        provider.sendUserMessage(guiMessage);
    }
    
    @Override
    public void finish() {
        /*
         * You can remove the GUI at any point
         */
        if (guiMessage != null) {
            Layer1ApiAlertGuiMessage removeGuiMessage = new Builder(guiMessage)
                .setIsAdd(false)
                .build();
            provider.sendUserMessage(removeGuiMessage);
        }
    }
    
    private static class SimpleGuiPanel extends StrategyPanel {
    
        public SimpleGuiPanel() {
            super("Simple alert GUI demo");
            add(new JLabel("Hello from Simple alert GUI demo!"));
        }
    }
}
