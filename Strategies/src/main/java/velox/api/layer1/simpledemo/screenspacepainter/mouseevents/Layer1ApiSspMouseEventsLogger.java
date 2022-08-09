package velox.api.layer1.simpledemo.screenspacepainter.mouseevents;

import java.util.HashMap;
import java.util.Map;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import velox.api.layer1.Layer1ApiFinishable;
import velox.api.layer1.Layer1ApiProvider;
import velox.api.layer1.Layer1CustomPanelsGetter;
import velox.api.layer1.annotations.Layer1ApiVersion;
import velox.api.layer1.annotations.Layer1ApiVersionValue;
import velox.api.layer1.annotations.Layer1Attachable;
import velox.api.layer1.annotations.Layer1StrategyName;
import velox.api.layer1.common.Log;
import velox.api.layer1.common.Utils;
import velox.api.layer1.layers.Layer1ApiRelay;
import velox.api.layer1.layers.strategies.interfaces.CanvasMouseEvent;
import velox.api.layer1.layers.strategies.interfaces.CanvasMouseEvent.CoordinateRequestType;
import velox.api.layer1.layers.strategies.interfaces.CanvasMouseListener;
import velox.api.layer1.layers.strategies.interfaces.ScreenSpaceCanvas;
import velox.api.layer1.layers.strategies.interfaces.ScreenSpaceCanvas.RelativeHorizontalCoordinate;
import velox.api.layer1.layers.strategies.interfaces.ScreenSpaceCanvas.RelativeVerticalCoordinate;
import velox.api.layer1.layers.strategies.interfaces.ScreenSpaceCanvasFactory.ScreenSpaceCanvasType;
import velox.api.layer1.layers.strategies.interfaces.ScreenSpacePainterAdapter;
import velox.api.layer1.messages.UserMessageLayersChainCreatedTargeted;
import velox.api.layer1.messages.indicators.Layer1ApiUserMessageModifyScreenSpacePainter;
import velox.gui.StrategyPanel;
import velox.gui.utils.GuiUtils;

/**
 * Simply logs mouse events. On the UI you can enable types of mouse events you want to log, and the active
 * canvases types.
 */
@Layer1Attachable
@Layer1StrategyName("SSP mouse events logger")
@Layer1ApiVersion(Layer1ApiVersionValue.VERSION2)
public class Layer1ApiSspMouseEventsLogger extends Layer1ApiRelay implements Layer1ApiFinishable, Layer1CustomPanelsGetter {
    
    private StrategyPanel strategyPanel;
    
    private volatile boolean isEnabled = false;
    
    public Layer1ApiSspMouseEventsLogger(Layer1ApiProvider provider) {
        super(provider);
    }
    
    enum MouseEventType {
        CLICKED, PRESSED, ENTERED, DRAGGED, FOCUS_GAINED, FOCUS_LOST, MOUSE_RELEASED, MOUSE_EXITED, MOUSE_MOVED, MOUSE_WHEEL_MOVED
    }
    
    Map<MouseEventType, JCheckBox> checkboxesPerType = new HashMap<>();
    
    private void addScreenSpacePainter(ScreenSpaceCanvasType canvasType) {
        Layer1ApiUserMessageModifyScreenSpacePainter message = Layer1ApiUserMessageModifyScreenSpacePainter
            .builder(this.getClass(), getUserName(canvasType))
            .setScreenSpacePainterFactory((indicatorName, indicatorAlias, screenSpaceCanvasFactory) -> {
                ScreenSpaceCanvas canvas = screenSpaceCanvasFactory.createCanvas(canvasType);
                canvas.addMouseListener(new CanvasMouseListener() {
                    
                    @Override
                    public void mouseClicked(CanvasMouseEvent e) {
                        if (checkboxesPerType.get(MouseEventType.CLICKED).isSelected()) {
                            Log.info("Mouse clicked in [" + indicatorName + "] event: " + canvasEvtToString(e));
                        }
                    }
                    
                    @Override
                    public void mousePressed(CanvasMouseEvent e) {
                        if (checkboxesPerType.get(MouseEventType.PRESSED).isSelected()) {
                            Log.info("Mouse pressed in [" + indicatorName + "] event: " + canvasEvtToString(e));
                        }
                        
                    }
                    
                    @Override
                    public void mouseEntered(CanvasMouseEvent e) {
                        if (checkboxesPerType.get(MouseEventType.ENTERED).isSelected()) {
                            Log.info("Mouse entered in [" + indicatorName + "] event: " + canvasEvtToString(e));
                        }
                    }
                    
                    @Override
                    public void mouseDragged(CanvasMouseEvent e) {
                        if (checkboxesPerType.get(MouseEventType.DRAGGED).isSelected()) {
                            Log.info("Mouse dragged in [" + indicatorName + "] event: " + canvasEvtToString(e));
                        }
                    }
                    
                    @Override
                    public void onFocusGained() {
                        if (checkboxesPerType.get(MouseEventType.FOCUS_GAINED).isSelected()) {
                            Log.info("Focus gained in [" + indicatorName + "]");
                        }
                    }
                    
                    @Override
                    public void onFocusLost() {
                        if (checkboxesPerType.get(MouseEventType.FOCUS_LOST).isSelected()) {
                            Log.info("Focus lost in [" + indicatorName + "]");
                        }
                    }
                    
                    @Override
                    public void mouseReleased(CanvasMouseEvent e) {
                        if (checkboxesPerType.get(MouseEventType.MOUSE_RELEASED).isSelected()) {
                            Log.info("Mouse released in [" + indicatorName + "] event: " + canvasEvtToString(e));
                        }
                    }
                    
                    @Override
                    public void mouseExited(CanvasMouseEvent e) {
                        if (checkboxesPerType.get(MouseEventType.MOUSE_EXITED).isSelected()) {
                            Log.info("Mouse exited in [" + indicatorName + "] event: " + canvasEvtToString(e));
                        }
                    }
                    
                    @Override
                    public void mouseMoved(CanvasMouseEvent e) {
                        if (checkboxesPerType.get(MouseEventType.MOUSE_MOVED).isSelected()) {
                            Log.info("Mouse moved in [" + indicatorName + "] event: " + canvasEvtToString(e));
                        }
                    }
                    
                    @Override
                    public void mouseWheelMoved(CanvasMouseEvent e) {
                        if (checkboxesPerType.get(MouseEventType.MOUSE_WHEEL_MOVED).isSelected()) {
                            Log.info("Mouse wheel moved in [" + indicatorName + "] event: " + canvasEvtToString(e));
                        }
                    }
                    
                    @Override
                    public int getEventScore(CanvasMouseEvent canvasMouseEvent) {
                        return CanvasMouseListener.MAX_SCORE;
                    }
                });
                return new ScreenSpacePainterAdapter() {
                    @Override
                    public void dispose() {
                        canvas.dispose();
                    }
                };
            })
            .setIsAdd(true)
            .build();
        
        SwingUtilities.invokeLater(() -> {
            provider.sendUserMessage(message);
        });
    }
    
    private String getUserName(ScreenSpaceCanvasType canvasType) {
        return "SSP mouse events log--" + canvasType;
    }
    
    @Override
    public void onUserMessage(Object data) {
        if (data instanceof UserMessageLayersChainCreatedTargeted) {
            UserMessageLayersChainCreatedTargeted message = (UserMessageLayersChainCreatedTargeted) data;
            if (message.targetClass == this.getClass()) {
                isEnabled = true;
                if (strategyPanel != null) {
                    SwingUtilities.invokeLater(() -> GuiUtils.setPanelEnabled(strategyPanel, true));
                }
            }
        }
        super.onUserMessage(data);
    }
    
    @Override
    public void finish() {
        isEnabled = false;
        if (strategyPanel != null) {
            GuiUtils.setPanelEnabled(strategyPanel, false);
        }
    }
    
    private void changeSSPListenerState(ScreenSpaceCanvasType listenerCanvasType, boolean enabled) {
        if (enabled) {
            addScreenSpacePainter(listenerCanvasType);
        } else {
            removeScreenSpacePainter(listenerCanvasType);
        }
    }
    
    private void removeScreenSpacePainter(ScreenSpaceCanvasType listenerCanvasType) {
        Layer1ApiUserMessageModifyScreenSpacePainter removeMessage = Layer1ApiUserMessageModifyScreenSpacePainter
            .builder(this.getClass(), getUserName(listenerCanvasType))
            .setIsAdd(false)
            .build();
        Utils.invokeInEdtDirectlyOrLater(() -> {
            provider.sendUserMessage(removeMessage);
        });
    }
    
    
    @Override
    public StrategyPanel[] getCustomGuiFor(String s, String s1) {
        if (strategyPanel == null) {
            strategyPanel = new StrategyPanel("SSP mouse events logger");
            strategyPanel.setLayout(new BoxLayout(strategyPanel, BoxLayout.Y_AXIS));
            for (ScreenSpaceCanvasType canvasType : ScreenSpaceCanvasType.values()) {
                if (canvasType == ScreenSpaceCanvasType.FULL_WINDOW) { // Not supported
                    continue;
                }
                JCheckBox checkBox = new JCheckBox(canvasType.toString().toLowerCase() + " mouse listener");
                checkBox.addActionListener(e -> changeSSPListenerState(canvasType, checkBox.isSelected()));
                strategyPanel.add(checkBox);
            }
            
            strategyPanel.add(new JLabel("Events to log:"));
            for (MouseEventType eventType : MouseEventType.values()) {
                JCheckBox typeCheckbox = new JCheckBox(eventType.toString().toLowerCase());
                strategyPanel.add(typeCheckbox);
                checkboxesPerType.put(eventType, typeCheckbox);
            }
            GuiUtils.setPanelEnabled(strategyPanel, isEnabled);
        }
        
        return new StrategyPanel[]{strategyPanel};
    }
    
    /**
     * Request all available combinations of relativeTo and requestType
     */
    private String canvasEvtToString(CanvasMouseEvent e) {
        return "CanvasMouseEvent{"
            + "x[LEFT_EDGE][IN_PIXELS]=" + e.getX(RelativeHorizontalCoordinate.HORIZONTAL_PIXEL_ZERO, CoordinateRequestType.PIXELS)
            + ", x[DATA_ZERO][IN_PIXELS]=" + e.getX(RelativeHorizontalCoordinate.HORIZONTAL_DATA_ZERO, CoordinateRequestType.PIXELS)
            + ", x[LEFT_EDGE][IN_DATA]=" + e.getX(RelativeHorizontalCoordinate.HORIZONTAL_PIXEL_ZERO, CoordinateRequestType.DATA)
            + ", x[DATA_ZERO][IN_DATA]=" + e.getX(RelativeHorizontalCoordinate.HORIZONTAL_DATA_ZERO, CoordinateRequestType.DATA)
            + ", y[BOTTOM_EDGE][IN_PIXELS]=" + e.getY(RelativeVerticalCoordinate.VERTICAL_PIXEL_ZERO, CoordinateRequestType.PIXELS)
            + ", y[DATA_ZERO][IN_PIXELS]=" + e.getY(RelativeVerticalCoordinate.VERTICAL_DATA_ZERO, CoordinateRequestType.PIXELS)
            + ", y[BOTTOM_EDGE][IN_DATA]=" + e.getY(RelativeVerticalCoordinate.VERTICAL_PIXEL_ZERO, CoordinateRequestType.DATA)
            + ", y[DATA_ZERO][IN_DATA]=" + e.getY(RelativeVerticalCoordinate.VERTICAL_DATA_ZERO, CoordinateRequestType.DATA)
            + "}";
    }
}
