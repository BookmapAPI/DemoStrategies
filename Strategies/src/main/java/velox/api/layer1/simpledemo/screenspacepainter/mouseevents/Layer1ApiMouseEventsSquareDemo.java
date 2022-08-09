package velox.api.layer1.simpledemo.screenspacepainter.mouseevents;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JMenuItem;
import javax.swing.SwingUtilities;
import velox.api.layer1.Layer1ApiFinishable;
import velox.api.layer1.Layer1ApiProvider;
import velox.api.layer1.annotations.Layer1ApiVersion;
import velox.api.layer1.annotations.Layer1ApiVersionValue;
import velox.api.layer1.annotations.Layer1Attachable;
import velox.api.layer1.annotations.Layer1StrategyName;
import velox.api.layer1.common.Log;
import velox.api.layer1.layers.Layer1ApiRelay;
import velox.api.layer1.layers.strategies.interfaces.CanvasContextMenuProvider;
import velox.api.layer1.layers.strategies.interfaces.CanvasMouseEvent;
import velox.api.layer1.layers.strategies.interfaces.CanvasMouseEvent.CoordinateRequestType;
import velox.api.layer1.layers.strategies.interfaces.CanvasMouseListener;
import velox.api.layer1.layers.strategies.interfaces.ScreenSpaceCanvas;
import velox.api.layer1.layers.strategies.interfaces.ScreenSpaceCanvas.CanvasIcon;
import velox.api.layer1.layers.strategies.interfaces.ScreenSpaceCanvas.CompositeCoordinateBase;
import velox.api.layer1.layers.strategies.interfaces.ScreenSpaceCanvas.CompositeHorizontalCoordinate;
import velox.api.layer1.layers.strategies.interfaces.ScreenSpaceCanvas.CompositeVerticalCoordinate;
import velox.api.layer1.layers.strategies.interfaces.ScreenSpaceCanvas.PreparedImage;
import velox.api.layer1.layers.strategies.interfaces.ScreenSpaceCanvas.RelativeHorizontalCoordinate;
import velox.api.layer1.layers.strategies.interfaces.ScreenSpaceCanvas.RelativeVerticalCoordinate;
import velox.api.layer1.layers.strategies.interfaces.ScreenSpaceCanvasFactory.ScreenSpaceCanvasType;
import velox.api.layer1.layers.strategies.interfaces.ScreenSpacePainterAdapter;
import velox.api.layer1.messages.UserMessageLayersChainCreatedTargeted;
import velox.api.layer1.messages.indicators.Layer1ApiUserMessageModifyScreenSpacePainter;

/**
 * This demo adds a blue rectangle to the center of the heatmap, which turns green
 * on mouse hover. Also, adds custom options to Bookmap right click menu
 */
@Layer1Attachable
@Layer1StrategyName("SSP mouse events on square")
@Layer1ApiVersion(Layer1ApiVersionValue.VERSION2)
public class Layer1ApiMouseEventsSquareDemo extends Layer1ApiRelay implements Layer1ApiFinishable {
    
    
    public Layer1ApiMouseEventsSquareDemo(Layer1ApiProvider provider) {
        super(provider);
    }
    
    @Override
    public void onUserMessage(Object data) {
        if (data instanceof UserMessageLayersChainCreatedTargeted) {
            UserMessageLayersChainCreatedTargeted message = (UserMessageLayersChainCreatedTargeted) data;
            if (message.targetClass == this.getClass()) {
                Layer1ApiUserMessageModifyScreenSpacePainter addSspMessage = Layer1ApiUserMessageModifyScreenSpacePainter
                    .builder(this.getClass(), "SSP square painter mouse events")
                    .setScreenSpacePainterFactory((indicatorName, indicatorAlias, screenSpaceCanvasFactory) ->
                        new SquarePainter(screenSpaceCanvasFactory.createCanvas(ScreenSpaceCanvasType.HEATMAP)))
                    .setIsAdd(true)
                    .build();
    
                SwingUtilities.invokeLater(() -> {
                    provider.sendUserMessage(addSspMessage);
                });
            }
        }
        super.onUserMessage(data);
    }
    
    @Override
    public void finish() {
    }
    
    static class SquarePainter implements ScreenSpacePainterAdapter, CanvasMouseListener, CanvasContextMenuProvider {
    
        private static final int SQUARE_SIDE_PX = 100;
        
        int canvasWidth;
        int heatmapHeight;
        boolean shouldUpdateImage = false;
        boolean focusGained = false;
        CanvasIcon squareShape;
        private CompositeHorizontalCoordinate x1;
        private CompositeVerticalCoordinate y1;
        private CompositeHorizontalCoordinate x2;
        private CompositeVerticalCoordinate y2;
        
        ScreenSpaceCanvas canvas;
    
        public SquarePainter(ScreenSpaceCanvas canvas) {
            this.canvas = canvas;
            canvas.addMouseListener(this);
            canvas.addContextMenuProvider(this);
        }
    
        @Override
        public void onHeatmapActivePixelsWidth(int heatmapActivePixelsWidth) {
            canvasWidth = heatmapActivePixelsWidth;
            shouldUpdateImage = true;
        }
    
        @Override
        public void onHeatmapPixelsHeight(int heatmapPixelsHeight) {
            heatmapHeight = heatmapPixelsHeight;
            shouldUpdateImage = true;
        }
        
        @Override
        public void onMoveEnd() {
            if (shouldUpdateImage) {
                update();
            }
        }
        
        private void update() {
            x1 = new CompositeHorizontalCoordinate(CompositeCoordinateBase.PIXEL_ZERO,
                canvasWidth / 2 - SQUARE_SIDE_PX / 2, 0);
            y1 = new CompositeVerticalCoordinate(CompositeCoordinateBase.PIXEL_ZERO,
                heatmapHeight / 2 - SQUARE_SIDE_PX / 2, 0);
    
            x2 = new CompositeHorizontalCoordinate(CompositeCoordinateBase.PIXEL_ZERO,
                canvasWidth / 2 + SQUARE_SIDE_PX / 2, 0);
            y2 = new CompositeVerticalCoordinate(CompositeCoordinateBase.PIXEL_ZERO,
                heatmapHeight / 2 + SQUARE_SIDE_PX / 2, 0);
    
            if (squareShape == null) {
                squareShape = new CanvasIcon(getSquareImage(Color.BLUE),  x1, y1, x2, y2);
                canvas.addShape( squareShape);
            } else {
                squareShape.setImage(getSquareImage(focusGained ? Color.GREEN : Color.BLUE));
                squareShape.setX1(x1);
                squareShape.setX2(x2);
                squareShape.setY1(y1);
                squareShape.setY2(y2);
            }
        }
        
        private PreparedImage getSquareImage(Color color) {
            BufferedImage texture = new BufferedImage(SQUARE_SIDE_PX, SQUARE_SIDE_PX, BufferedImage.TYPE_INT_ARGB);
            Graphics2D graph = texture.createGraphics();
            graph.setColor(color);
            graph.fillRect(0, 0, 100, 100);
            graph.dispose();
            
            return new PreparedImage(texture);
        }
    
        /**
         * Request focus when there is a hover event over the square. Note that if
         * we don't ignore drag event - by only requesting focus if it is a right-click
         * or no-click (hover) event - it is impossible to drag the chart from the square.
         */
        @Override
        public int getEventScore(CanvasMouseEvent e) {
            CompositeHorizontalCoordinate eventX = e.getX(RelativeHorizontalCoordinate.HORIZONTAL_PIXEL_ZERO,
                CoordinateRequestType.PIXELS);
            CompositeVerticalCoordinate eventY = e.getY(RelativeVerticalCoordinate.VERTICAL_PIXEL_ZERO, CoordinateRequestType.PIXELS);
            /*
            * Request score if the square is drawn, and it is not a drag event, and event intersects the square
            * */
            return squareShape != null
                && (SwingUtilities.isRightMouseButton(e.sourceEvent) || e.sourceEvent.getClickCount() == 0) // ignore drag events
                && x1.pixelsX <= eventX.pixelsX && eventX.pixelsX <= x2.pixelsX
                && y1.pixelsY <= eventY.pixelsY && eventY.pixelsY <= y2.pixelsY
                    ? CanvasMouseListener.MAX_SCORE
                    : 0;
        }
    
        @Override
        public void onFocusGained() {
            focusGained = true;
            update();
        }
    
        @Override
        public void onFocusLost() {
            focusGained = false;
            update();
        }
    
        /**
         * Add context menu options. One of them is always added, another one
         * only when the listener gets focus on the square.
         */
        @Override
        public List<JMenuItem> getMenuItems(CanvasMouseEvent canvasMouseEvent) {
            List<JMenuItem> items = new ArrayList<>();
            JMenuItem chartItem = new JMenuItem("From Layer1ApiMouseEventsSquareDemo - on chart");
            chartItem.addActionListener(e -> {
                Log.info(chartItem.getText() + " obtained event: " + e);
            });
            items.add(chartItem);
            
            if (focusGained) {
                JMenuItem squareItem = new JMenuItem("From Layer1ApiMouseEventsSquareDemo - on square");
                squareItem.addActionListener(e -> {
                    Log.info(squareItem.getText() + " obtained event: " + e);
                });
                items.add(squareItem);
            }
            
            return items;
        }
    
        @Override
        public void dispose() {
            canvas.dispose();
        }
    }
}
