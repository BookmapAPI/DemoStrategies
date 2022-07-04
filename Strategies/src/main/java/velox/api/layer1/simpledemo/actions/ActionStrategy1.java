package velox.api.layer1.simpledemo.actions;

import velox.api.layer1.Layer1ApiFinishable;
import velox.api.layer1.Layer1ApiProvider;
import velox.api.layer1.actions.Layer1ActionContainer;
import velox.api.layer1.actions.Layer1ExternalAction;
import velox.api.layer1.actions.annotations.Layer1ActionMapper;
import velox.api.layer1.actions.annotations.Layer1ActionMetadata;
import velox.api.layer1.annotations.Layer1ApiVersion;
import velox.api.layer1.annotations.Layer1ApiVersionValue;
import velox.api.layer1.annotations.Layer1Injectable;
import velox.api.layer1.annotations.Layer1StrategyName;
import velox.api.layer1.layers.Layer1ApiInjectorRelay;
import velox.api.layer1.messages.Layer1ApiSoundAlertMessage;

import java.awt.event.KeyEvent;
import java.util.HashSet;
import java.util.Set;

/**
 * Simple strategy that declares 3 actions and does not have any custom external windows.
 * It declares 3 custom actions that send message alerts.
 */
@Layer1Injectable
@Layer1StrategyName("Action Strategy 1")
@Layer1ApiVersion(Layer1ApiVersionValue.VERSION2)
public class ActionStrategy1 extends Layer1ApiInjectorRelay implements Layer1ApiFinishable {
    private Layer1ApiProvider provider;

    public ActionStrategy1(Layer1ApiProvider provider) {
        super(provider);
        this.provider = provider;
    }

    @Override
    public void finish() {}

    /** This method will register action when the strategy is loaded */
    @Layer1ActionMapper
    public Layer1ActionContainer registerActions() {
        Set<Layer1ExternalAction> actions = new HashSet<>();
        actions.add(new HelloWorldAction());
        actions.add(new ByeWorldAction());
        return new Layer1ActionContainer(actions);
    }
    
    @Layer1ActionMetadata(id = "action_strategy1.say_hi", name = "Hello World", groups = {"Alert Custom"})
    public class HelloWorldAction implements Layer1ExternalAction {

        @Override
        public boolean performAction(String actionId, KeyEvent e) {
            provider.sendUserMessage(
                    Layer1ApiSoundAlertMessage.builder()
                            .setTextInfo("Hi everyone! :)")
                            .setSource(ActionStrategy1.class)
                            .setMetadata(this.getClass().getName())
                            .setShowPopup(true)
                            .build()
            );
            return true;
        }

        @Override
        public void onShortcutChanged(String actionId, Set<String> shortcuts) {}
    }
    
    @Layer1ActionMetadata(id = ByeWorldAction.BYE_ID1, name ="Bye Bye", groups = {"Alert Custom"})
    @Layer1ActionMetadata(id = ByeWorldAction.BYE_ID2, name ="See you later", groups = {"Alert Custom"})
    public class ByeWorldAction implements Layer1ExternalAction {
        private static final String BYE_ID1 = "action_strategy1.say_bye";
        private static final String BYE_ID2 = "action_strategy1.say_bye2";

        @Override
        public boolean performAction(String actionId, KeyEvent e) {
            String description = BYE_ID1.equals(actionId) ? "Bye Bye!" : "See you later!";

            provider.sendUserMessage(
                    Layer1ApiSoundAlertMessage.builder()
                            .setTextInfo(description)
                            .setSource(ActionStrategy1.class)
                            .setMetadata(this.getClass().getName())
                            .setShowPopup(true)
                            .build()
            );
            
            return true;
        }

        @Override
        public void onShortcutChanged(String actionId, Set<String> shortcuts) {}
    }
}
