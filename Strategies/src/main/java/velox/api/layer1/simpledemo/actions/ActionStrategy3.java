package velox.api.layer1.simpledemo.actions;

import velox.api.layer1.Layer1ApiFinishable;
import velox.api.layer1.Layer1ApiInstrumentSpecificEnabledStateProvider;
import velox.api.layer1.Layer1ApiProvider;
import velox.api.layer1.actions.Layer1ActionContainer;
import velox.api.layer1.actions.Layer1ActionMapper;
import velox.api.layer1.actions.Layer1ExternalAction;
import velox.api.layer1.actions.annotations.Layer1ActionMetadata;
import velox.api.layer1.annotations.Layer1ApiVersion;
import velox.api.layer1.annotations.Layer1ApiVersionValue;
import velox.api.layer1.annotations.Layer1Attachable;
import velox.api.layer1.annotations.Layer1Injectable;
import velox.api.layer1.annotations.Layer1StrategyName;
import velox.api.layer1.common.ListenableHelper;

import javax.swing.JFrame;
import javax.swing.JLabel;
import java.awt.Color;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Strategy that declares custom actions and executes them in its external window.
 *
 * Unlike {@link ActionStrategy2} this strategy creates window per alias.
 *
 * To be able to perform actions in external window, such windows must have
 * {@link Layer1ExternalAction#EXTERNAL_WINDOW_ALIAS_PROPERTY} client property.
 *
 * Additionally, this strategy requests Bookmap to execute built-in actions in the strategy window
 * by declaring {@link Layer1ExternalAction#ALLOW_EXECUTING_BUILTIN_ACTIONS} client property which is set to true.
 */
@Layer1Attachable
@Layer1Injectable
@Layer1StrategyName("Action Strategy 3")
@Layer1ApiVersion(Layer1ApiVersionValue.VERSION2)
public class ActionStrategy3 implements Layer1ApiFinishable, Layer1ApiInstrumentSpecificEnabledStateProvider, Layer1ActionMapper {
    static final String STRATEGY_NAME = "Action Strategy 3";
    private Layer1ApiProvider provider;
    private Map<String, JFrame> aliasToFrame = new ConcurrentHashMap<>();

    public ActionStrategy3(Layer1ApiProvider provider) {
        this.provider = provider;
        ListenableHelper.addListeners(provider, this);
    }
    
    @Override
    public void finish() {
        aliasToFrame.values().forEach(Window::dispose);
        aliasToFrame.clear();
    }

    /** This method will register action when the strategy is loaded */
    @Override
    public Layer1ActionContainer getActionContainer() {
        Set<Layer1ExternalAction> actions = new HashSet<>();
        actions.add(new ColorAction());
        return new Layer1ActionContainer(actions);
    }

    @Override
    public void onStrategyCheckboxEnabled(String alias, boolean isEnabled) {
        if (isEnabled) {
            JFrame frame = createFrame(alias);
            aliasToFrame.put(alias, frame);
            frame.setVisible(true);
        } else {
            JFrame frame = aliasToFrame.remove(alias);
            if (frame != null) {
                frame.dispose();
            }
        }
    }
    
    private JFrame createFrame(String alias) {
        JFrame frame = new JFrame() {
            @Override
            public void dispose() {
                super.dispose();
                String alias = (String) this.getRootPane()
                        .getClientProperty(Layer1ExternalAction.EXTERNAL_WINDOW_ALIAS_PROPERTY);
                aliasToFrame.remove(alias);
            }
        };
        frame.setSize(600, 400);
        frame.setTitle(STRATEGY_NAME + "#" + alias);
        frame.getRootPane().putClientProperty(Layer1ExternalAction.EXTERNAL_WINDOW_ALIAS_PROPERTY, alias);
        frame.getRootPane().putClientProperty(Layer1ExternalAction.ALLOW_EXECUTING_BUILTIN_ACTIONS, true);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        JLabel label1 = new JLabel("Window belongs to alias: " + alias);
        frame.add(label1);
        return frame;
    }

    @Override
    public boolean isStrategyEnabled(String alias) {
        return aliasToFrame.containsKey(alias);
    }


    @Layer1ActionMetadata(id = ColorAction.RED_ACTION_ID, name = "Red Color")
    @Layer1ActionMetadata(id = ColorAction.BLUE_ACTION_ID, name = "Blue Color")
    public class ColorAction implements Layer1ExternalAction {
        private static final String RED_ACTION_ID = "action_strategy3.red_color";
        private static final String BLUE_ACTION_ID = "action_strategy3.blue_color";

        @Override
        public boolean performAction(String actionId, KeyEvent e) {
            boolean isMyWindow = aliasToFrame.values().stream().anyMatch(frame -> e.getComponent() == frame);
            
            if (isMyWindow) {
                JFrame frame = (JFrame) e.getComponent();
                JLabel label = (JLabel) frame.getRootPane().getContentPane().getComponent(0);

                Color color = RED_ACTION_ID.equals(actionId) ? Color.RED : Color.BLUE;
                label.setForeground(color);
            }
            return isMyWindow;
        }

        @Override
        public void onShortcutChanged(String actionId, Set<String> shortcuts) {
            System.out.println("ActionStrategy3#onShortcutChanged: " + shortcuts);
        }
    }
}
