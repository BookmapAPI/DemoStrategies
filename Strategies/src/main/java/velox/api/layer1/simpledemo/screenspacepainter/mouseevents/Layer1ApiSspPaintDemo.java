package velox.api.layer1.simpledemo.screenspacepainter.mouseevents;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JRadioButton;
import javax.swing.JSeparator;
import javax.swing.SwingUtilities;
import velox.api.layer1.Layer1ApiAdminAdapter;
import velox.api.layer1.Layer1ApiFinishable;
import velox.api.layer1.Layer1ApiProvider;
import velox.api.layer1.Layer1CustomPanelsGetter;
import velox.api.layer1.annotations.Layer1ApiVersion;
import velox.api.layer1.annotations.Layer1ApiVersionValue;
import velox.api.layer1.annotations.Layer1Attachable;
import velox.api.layer1.annotations.Layer1StrategyName;
import velox.api.layer1.common.ListenableHelper;
import velox.api.layer1.layers.strategies.interfaces.CanvasMouseEvent;
import velox.api.layer1.layers.strategies.interfaces.CanvasMouseEvent.CoordinateRequestType;
import velox.api.layer1.layers.strategies.interfaces.CanvasMouseListener;
import velox.api.layer1.layers.strategies.interfaces.MouseModuleScore;
import velox.api.layer1.layers.strategies.interfaces.ScreenSpaceCanvas;
import velox.api.layer1.layers.strategies.interfaces.ScreenSpaceCanvas.CanvasIcon;
import velox.api.layer1.layers.strategies.interfaces.ScreenSpaceCanvas.CompositeCoordinateBase;
import velox.api.layer1.layers.strategies.interfaces.ScreenSpaceCanvas.CompositeHorizontalCoordinate;
import velox.api.layer1.layers.strategies.interfaces.ScreenSpaceCanvas.CompositeVerticalCoordinate;
import velox.api.layer1.layers.strategies.interfaces.ScreenSpaceCanvas.HorizontalCoordinate;
import velox.api.layer1.layers.strategies.interfaces.ScreenSpaceCanvas.PreparedImage;
import velox.api.layer1.layers.strategies.interfaces.ScreenSpaceCanvas.RelativePixelHorizontalCoordinate;
import velox.api.layer1.layers.strategies.interfaces.ScreenSpaceCanvas.RelativePixelVerticalCoordinate;
import velox.api.layer1.layers.strategies.interfaces.ScreenSpaceCanvas.VerticalCoordinate;
import velox.api.layer1.layers.strategies.interfaces.ScreenSpaceCanvasFactory.ScreenSpaceCanvasType;
import velox.api.layer1.layers.strategies.interfaces.ScreenSpacePainterAdapter;
import velox.api.layer1.messages.UserMessageLayersChainCreatedTargeted;
import velox.api.layer1.messages.indicators.Layer1ApiUserMessageModifyScreenSpacePainter;
import velox.gui.StrategyPanel;
import velox.gui.utils.GuiUtils;

/**
 * Paint-like pencil painting example.
 * Note that you can paint in DATA or PIXEL coordinates - in the first case
 * your image is fixed in chart coordinates (time and price), while in the
 * latter case it stays fixed relative to the screen boundaries.
 */
@Layer1Attachable
@Layer1StrategyName("SSP paint")
@Layer1ApiVersion(Layer1ApiVersionValue.VERSION2)
public class Layer1ApiSspPaintDemo implements
    Layer1ApiAdminAdapter,
    Layer1ApiFinishable,
    Layer1CustomPanelsGetter {
    
    private final Layer1ApiProvider provider;
    
    private final Set<ScreenSpaceCanvasType> activeCanvases = Collections.newSetFromMap(new ConcurrentHashMap<>());
    
    private StrategyPanel strategyPanel;
    private JCheckBox heatmapCanvasState;
    private JCheckBox rightOfTimelineCanvasState;
    private ButtonGroup basisCoordinate;
    private ButtonGroup coordinateRequestType;
    private JLabel warnMsgLabel;
    
    volatile boolean isEnabled = false;
    
    public Layer1ApiSspPaintDemo(Layer1ApiProvider provider) {
        this.provider = provider;
        ListenableHelper.addListeners(provider,this);
    }
    
    @Override
    public void onUserMessage(Object data) {
        if (data instanceof UserMessageLayersChainCreatedTargeted) {
            UserMessageLayersChainCreatedTargeted message = (UserMessageLayersChainCreatedTargeted) data;
            if (message.targetClass == this.getClass()) {
                isEnabled = true;
                if (strategyPanel != null) {
                    SwingUtilities.invokeLater(() -> {
                        GuiUtils.setPanelEnabled(strategyPanel, true);
                    });
                }
            }
        }
    }
    
    private void modifyScreenSpacePainter(ScreenSpaceCanvasType canvasType, boolean isAdd) {
        Layer1ApiUserMessageModifyScreenSpacePainter message = Layer1ApiUserMessageModifyScreenSpacePainter
            .builder(this.getClass(), "SSP paint--" + canvasType)
            .setScreenSpacePainterFactory((indicatorName, indicatorAlias, screenSpaceCanvasFactory) -> {
                return new PaintScreenSpacePainter(
                    screenSpaceCanvasFactory.createCanvas(canvasType),
                    canvasType == ScreenSpaceCanvasType.HEATMAP ? Color.GREEN : Color.BLUE);
            })
            .setIsAdd(isAdd)
            .build();
        
        if (isAdd) {
            activeCanvases.add(canvasType);
        } else {
            activeCanvases.remove(canvasType);
        }
        
        provider.sendUserMessage(message);
    }
    
    @Override
    public void finish() {
        for (ScreenSpaceCanvasType canvas : activeCanvases) {
            modifyScreenSpacePainter(canvas, false);
        }
        if (strategyPanel != null) {
            GuiUtils.setPanelEnabled(strategyPanel, false);
        }
        isEnabled = false;
    }
    
    
    @Override
    public StrategyPanel[] getCustomGuiFor(String s, String s1) {
        if (strategyPanel == null) {
            strategyPanel = new StrategyPanel("Paint with SSP");
            strategyPanel.setLayout(new BoxLayout(strategyPanel, BoxLayout.Y_AXIS));
            strategyPanel.add(new JLabel("Hold Ctrl and drag the mouse to paint"));
            heatmapCanvasState = new JCheckBox("Heatmap canvas painting");
            heatmapCanvasState.addActionListener(e -> {
                modifyScreenSpacePainter(ScreenSpaceCanvasType.HEATMAP, heatmapCanvasState.isSelected());
            });
            rightOfTimelineCanvasState = new JCheckBox("Right of timeline canvas painting");
            rightOfTimelineCanvasState.addActionListener(e -> {
                modifyScreenSpacePainter(ScreenSpaceCanvasType.RIGHT_OF_TIMELINE, rightOfTimelineCanvasState.isSelected());
            });
            strategyPanel.add(heatmapCanvasState);
            strategyPanel.add(rightOfTimelineCanvasState);
            
            strategyPanel.add(new JSeparator());
            
            strategyPanel.add(new JLabel("Resolve coordinates relative to:"));
            basisCoordinate = new ButtonGroup();
    

            JRadioButton pixelZeroCoordsBtn = new JRadioButton(CompositeCoordinateBase.PIXEL_ZERO.name());
            pixelZeroCoordsBtn.setSelected(true);
            pixelZeroCoordsBtn.setActionCommand(CompositeCoordinateBase.PIXEL_ZERO.name());
            strategyPanel.add(pixelZeroCoordsBtn);
            basisCoordinate.add(pixelZeroCoordsBtn);
            
            JRadioButton dataZeroBtn = new JRadioButton(CompositeCoordinateBase.DATA_ZERO.name());
            dataZeroBtn.setActionCommand(CompositeCoordinateBase.DATA_ZERO.name());
            basisCoordinate.add(dataZeroBtn);
            strategyPanel.add(dataZeroBtn);
    
            strategyPanel.add(new JSeparator());
            
            strategyPanel.add(new JLabel("Request coordinates in coordinate type:"));
            coordinateRequestType = new ButtonGroup();
            JRadioButton pixelRequestBtn = new JRadioButton(CoordinateRequestType.PIXELS.name());
            pixelRequestBtn.setSelected(true);
            pixelRequestBtn.setActionCommand(CoordinateRequestType.PIXELS.name());
            coordinateRequestType.add(pixelRequestBtn);
            strategyPanel.add(pixelRequestBtn);
            
            JRadioButton dataRequestBtn = new JRadioButton(CoordinateRequestType.DATA.name());
            dataRequestBtn.setActionCommand(CoordinateRequestType.DATA.name());
    

            coordinateRequestType.add(dataRequestBtn);
            strategyPanel.add(dataRequestBtn);
    
            strategyPanel.add(new JSeparator());
            warnMsgLabel = new JLabel();
            strategyPanel.add(warnMsgLabel);
    
            // Show warn message if request coords in PIXELS from DATA_ZERO
            ActionListener warnMessageListener = e -> {
                if (dataZeroBtn.isSelected() && pixelRequestBtn.isSelected()) {
                    warnMsgLabel.setText("<html>It is not recommended to request data from DATA_ZERO in PIXELS,"
                        + " almost certainly this isn't what you want to do.</html>");
                } else if (dataZeroBtn.isSelected()
                        && dataRequestBtn.isSelected()
                        && rightOfTimelineCanvasState.isSelected()) {
                    warnMsgLabel.setText("<html>It is not recommended to draw in RIGHT_OF_TIMELINE canvas"
                        + " using DATA_ZERO coordinate as base, this might lead to"
                        + " unexpected results, as the left edge of this canvas moves with the data.</html>");
                } else {
                    warnMsgLabel.setText("");
                }
            };
            rightOfTimelineCanvasState.addActionListener(warnMessageListener);
            pixelZeroCoordsBtn.addActionListener(warnMessageListener);
            dataZeroBtn.addActionListener(warnMessageListener);
            pixelRequestBtn.addActionListener(warnMessageListener);
            dataRequestBtn.addActionListener(warnMessageListener);
        }
        
        GuiUtils.setPanelEnabled(strategyPanel, isEnabled);
        return new StrategyPanel[]{strategyPanel};
    }
    
    class PaintScreenSpacePainter implements ScreenSpacePainterAdapter, CanvasMouseListener {
        
        private static final int PAINT_WIDTH = 4;
        
        ScreenSpaceCanvas canvas;
        
        PreparedImage pointImage;
    
    
        public PaintScreenSpacePainter(ScreenSpaceCanvas canvas, Color drawingColor) {
            this.canvas = canvas;
            canvas.addMouseListener(this);
        
            BufferedImage texture = new BufferedImage(PAINT_WIDTH, PAINT_WIDTH, BufferedImage.TYPE_INT_ARGB);
            Graphics2D graph = texture.createGraphics();
            graph.setColor(drawingColor);
            graph.fillRect(0, 0, PAINT_WIDTH * 2, PAINT_WIDTH * 2);
            graph.dispose();
            pointImage = new PreparedImage(texture);
        }
        
        private void paintPoint(CompositeHorizontalCoordinate x, CompositeVerticalCoordinate y) {
            CanvasIcon pointIcon = new CanvasIcon(pointImage,
                new RelativePixelHorizontalCoordinate(x, -PAINT_WIDTH),
                new RelativePixelVerticalCoordinate(y, -PAINT_WIDTH),
                new RelativePixelHorizontalCoordinate(x, PAINT_WIDTH),
                new RelativePixelVerticalCoordinate(y, PAINT_WIDTH));
            canvas.addShape(pointIcon);
        }
        

        @Override
        public int getEventScore(CanvasMouseEvent e) {
            return e.sourceEvent.isControlDown() && SwingUtilities.isLeftMouseButton(e.sourceEvent)
                ? MouseModuleScore.MAX.score
                : MouseModuleScore.NONE.score;
        }
    
        @Override
        public void mouseDragged(CanvasMouseEvent e) {
            // .getActionCommand() returns values of CompositeCoordinateBase enum
            CompositeCoordinateBase base = CompositeCoordinateBase.valueOf(basisCoordinate.getSelection().getActionCommand());
            HorizontalCoordinate basisX = new CompositeHorizontalCoordinate(base, 0, 0);
            VerticalCoordinate basisY = new CompositeVerticalCoordinate(base, 0, 0);

            CoordinateRequestType requestType = CoordinateRequestType.valueOf(
                coordinateRequestType.getSelection().getActionCommand());
            CompositeHorizontalCoordinate x = e.getX(basisX, requestType);
            CompositeVerticalCoordinate y = e.getY(basisY, requestType);
            
            // Draw the point in the same coordinate
            paintPoint(x, y);
        }
    
        @Override
        public void dispose() {
            canvas.dispose();
        }
    }
}
