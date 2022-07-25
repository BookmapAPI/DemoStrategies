package velox.api.layer1.simpledemo.actions;

import velox.api.layer1.Layer1ApiFinishable;
import velox.api.layer1.Layer1ApiProvider;
import velox.api.layer1.actions.Layer1ActionContainer;
import velox.api.layer1.actions.Layer1ActionMapper;
import velox.api.layer1.actions.Layer1ExternalAction;
import velox.api.layer1.actions.annotations.Layer1ActionMetadata;
import velox.api.layer1.annotations.Layer1ApiVersion;
import velox.api.layer1.annotations.Layer1ApiVersionValue;
import velox.api.layer1.annotations.Layer1Injectable;
import velox.api.layer1.annotations.Layer1StrategyName;
import velox.api.layer1.data.InstrumentInfo;
import velox.api.layer1.layers.Layer1ApiInjectorRelay;
import velox.api.layer1.messages.Layer1ApiSoundAlertMessage;

import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import java.awt.FlowLayout;
import java.awt.event.ItemEvent;
import java.awt.event.KeyEvent;
import java.util.HashSet;
import java.util.Set;

/**
 * Simple strategy that declares actions and executes them in its external window.
 * It declares custom action that sends message alerts when focus is on the external window.
 *
 * To be able to perform actions in external window, such windows must have
 * {@link Layer1ExternalAction#EXTERNAL_WINDOW_ALIAS_PROPERTY} client property.
 */
@Layer1Injectable
@Layer1StrategyName(ActionStrategy2.STRATEGY_NAME)
@Layer1ApiVersion(Layer1ApiVersionValue.VERSION2)
public class ActionStrategy2 extends Layer1ApiInjectorRelay implements Layer1ApiFinishable, Layer1ActionMapper {
    static final String STRATEGY_NAME = "Action Strategy 2";
    private static final String NONE_ITEM = "---";
    
    private static JFrame frame;
    private static JComboBox<String> combo;
    private Layer1ApiProvider provider;
    private JLabel aliasLabel;

    public ActionStrategy2(Layer1ApiProvider provider) {
        super(provider);
        this.provider = provider;
    }

    @Override
    public void onInstrumentAdded(String alias, InstrumentInfo instrumentInfo) {
        super.onInstrumentAdded(alias, instrumentInfo);
        SwingUtilities.invokeLater(() -> {
            createFrame();
            combo.addItem(alias);
        });
    }

    @Override
    public void onInstrumentRemoved(String alias) {
        super.onInstrumentRemoved(alias);
        SwingUtilities.invokeLater(() -> {
            combo.removeItem(alias);
        });
    }

    private void createFrame() {
        if (frame == null) {
            frame = new JFrame() {
                @Override
                public void dispose() {
                    super.dispose();
                    frame = null;
                }
            };

            frame.setSize(600, 400);
            combo = new JComboBox<>(new String[]{NONE_ITEM});
            combo.addItemListener(e -> {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    boolean itemSelected = !NONE_ITEM.equals(combo.getSelectedItem());
                    
                    Object alias = itemSelected ? combo.getSelectedItem() : "" ;
                    frame.getRootPane().putClientProperty(Layer1ExternalAction.EXTERNAL_WINDOW_ALIAS_PROPERTY, alias);

                    frame.setTitle(STRATEGY_NAME + (itemSelected ? "#" + alias : ""));
                    aliasLabel.setText(itemSelected ? (String) alias : NONE_ITEM);
                }
            });
            JLabel prefixLabel = new JLabel("Window belongs to alias: ");
            aliasLabel = new JLabel(NONE_ITEM);
            frame.setLayout(new FlowLayout());
            frame.setTitle(STRATEGY_NAME);
            frame.getRootPane().putClientProperty(Layer1ExternalAction.EXTERNAL_WINDOW_ALIAS_PROPERTY, "");
            frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            
            frame.add(prefixLabel);
            frame.add(combo);
            frame.add(aliasLabel);
            frame.setVisible(true);
        }
    }

    @Override
    public void finish() {
        if (frame != null) {
            frame.dispose();
        }
    }

    /** This method will register action when the strategy is loaded */
    @Override
    public Layer1ActionContainer getActionContainer() {
        Set<Layer1ExternalAction> actions = new HashSet<>();
        actions.add(new TestExternalWindowAction());
        return new Layer1ActionContainer(actions);
    }
    
    @Layer1ActionMetadata(id = "Alert from external window")
    public class TestExternalWindowAction implements Layer1ExternalAction {

        @Override
        public boolean performAction(String actionId, KeyEvent e) {
            boolean isMyWindow = e.getComponent() == frame
                    || SwingUtilities.getWindowAncestor(e.getComponent()) == frame;
            
            if (isMyWindow) {
                String alias = (String) frame.getRootPane()
                        .getClientProperty(Layer1ExternalAction.EXTERNAL_WINDOW_ALIAS_PROPERTY);
                
                String text = "".equals(alias) ? "Please choose the alias" : "Current alias: " + alias;
                
                provider.sendUserMessage(
                        Layer1ApiSoundAlertMessage.builder()
                                .setTextInfo("Alert from external window.\n\n" + text)
                                .setSource(ActionStrategy2.class)
                                .setMetadata(this.getClass().getName())
                                .setShowPopup(true)
                                .build()
                );
            }
            return isMyWindow;
        }

        @Override
        public void onShortcutChanged(String actionId, Set<String> shortcuts) {}
    }
}
