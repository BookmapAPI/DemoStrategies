package velox.api.layer1.simpledemo.screenspacepainter;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

import velox.api.layer1.Layer1ApiAdminAdapter;
import velox.api.layer1.Layer1ApiFinishable;
import velox.api.layer1.Layer1ApiProvider;
import velox.api.layer1.annotations.Layer1ApiVersion;
import velox.api.layer1.annotations.Layer1ApiVersionValue;
import velox.api.layer1.annotations.Layer1Attachable;
import velox.api.layer1.annotations.Layer1StrategyName;
import velox.api.layer1.common.ListenableHelper;
import velox.api.layer1.layers.strategies.interfaces.ScreenSpaceCanvas;
import velox.api.layer1.layers.strategies.interfaces.ScreenSpaceCanvas.CanvasIcon;
import velox.api.layer1.layers.strategies.interfaces.ScreenSpaceCanvas.CompositeCoordinateBase;
import velox.api.layer1.layers.strategies.interfaces.ScreenSpaceCanvas.CompositeHorizontalCoordinate;
import velox.api.layer1.layers.strategies.interfaces.ScreenSpaceCanvas.CompositeVerticalCoordinate;
import velox.api.layer1.layers.strategies.interfaces.ScreenSpaceCanvas.PreparedImage;
import velox.api.layer1.layers.strategies.interfaces.ScreenSpaceCanvasFactory;
import velox.api.layer1.layers.strategies.interfaces.ScreenSpaceCanvasFactory.ScreenSpaceCanvasType;
import velox.api.layer1.layers.strategies.interfaces.ScreenSpacePainter;
import velox.api.layer1.layers.strategies.interfaces.ScreenSpacePainterAdapter;
import velox.api.layer1.layers.strategies.interfaces.ScreenSpacePainterFactory;
import velox.api.layer1.messages.UserMessageLayersChainCreatedTargeted;
import velox.api.layer1.messages.indicators.Layer1ApiUserMessageModifyScreenSpacePainter;

/** This demo shows you locations corresponding to each canvas type visually */
@Layer1Attachable
@Layer1StrategyName("SSP canvas demo")
@Layer1ApiVersion(Layer1ApiVersionValue.VERSION2)
public class Layer1ApiCanvasColorsDemo implements
    Layer1ApiFinishable,
    Layer1ApiAdminAdapter,
    ScreenSpacePainterFactory
{
 
    private static final String INDICATOR_NAME = "Screen space canvas demo";
    
    private Layer1ApiProvider provider;

    private Map<String, String> indicatorsFullNameToUserName = new HashMap<>();
    private Map<String, String> indicatorsUserNameToFullName = new HashMap<>();

    public Layer1ApiCanvasColorsDemo(Layer1ApiProvider provider) {
        this.provider = provider;
        
        ListenableHelper.addListeners(provider, this);
    }
    
    @Override
    public void finish() {
        synchronized (indicatorsFullNameToUserName) {
            for (String userName : indicatorsFullNameToUserName.values()) {
                Layer1ApiUserMessageModifyScreenSpacePainter message = Layer1ApiUserMessageModifyScreenSpacePainter
                        .builder(Layer1ApiCanvasColorsDemo.class, userName).setIsAdd(false).build();
                provider.sendUserMessage(message);
            }
        }
    }
    
    private Layer1ApiUserMessageModifyScreenSpacePainter getUserMessageAdd(String userName) {
        return Layer1ApiUserMessageModifyScreenSpacePainter.builder(Layer1ApiCanvasColorsDemo.class, userName)
                .setIsAdd(true)
                .setScreenSpacePainterFactory(this)
                .build();
    }
    
    @Override
    public void onUserMessage(Object data) {
        if (data.getClass() == UserMessageLayersChainCreatedTargeted.class) {
            UserMessageLayersChainCreatedTargeted message = (UserMessageLayersChainCreatedTargeted) data;
            if (message.targetClass == getClass()) {
                addIndicator();
            }
        }
    }

    public void addIndicator() {
        Layer1ApiUserMessageModifyScreenSpacePainter message = getUserMessageAdd(INDICATOR_NAME);
        
        synchronized (indicatorsFullNameToUserName) {
            indicatorsFullNameToUserName.put(message.fullName, message.userName);
            indicatorsUserNameToFullName.put(message.userName, message.fullName);
        }
        provider.sendUserMessage(message);
    }

    @Override
    public ScreenSpacePainter createScreenSpacePainter(String indicatorName, String indicatorAlias,
            ScreenSpaceCanvasFactory screenSpaceCanvasFactory) {

        ScreenSpaceCanvas heatmapCanvas = screenSpaceCanvasFactory.createCanvas(ScreenSpaceCanvasType.HEATMAP);
        ScreenSpaceCanvas timelineCanvas = screenSpaceCanvasFactory.createCanvas(ScreenSpaceCanvasType.RIGHT_OF_TIMELINE);

        return new ScreenSpacePainterAdapter() {
            

            int heatmapFullPixelsWidth;
            int heatmapPixelsHeight;
            int rightOfTimelineWidth;
            
            boolean needToUpdateHeatmapImage = true;
            boolean needToUpdateRightOfTimelineImage = true;
            
            CanvasIcon heatmapIcon;
            CanvasIcon rightOfTimelineIcon;
            
            @Override
            public void onHeatmapFullPixelsWidth(int heatmapFullPixelsWidth) {
                needToUpdateHeatmapImage = true;
                this.heatmapFullPixelsWidth = heatmapFullPixelsWidth;
            }
            
            @Override
            public void onHeatmapPixelsHeight(int heatmapPixelsHeight) {
                needToUpdateHeatmapImage = true;
                needToUpdateRightOfTimelineImage = true;
                this.heatmapPixelsHeight = heatmapPixelsHeight;
            }

            @Override
            public void onMoveEnd() {
                if (needToUpdateHeatmapImage) {
                    PreparedImage icon = generateCrossedBoxIcon(
                            heatmapFullPixelsWidth, heatmapPixelsHeight, Color.ORANGE, 1, false);
                    
                    CompositeHorizontalCoordinate x2 = new CompositeHorizontalCoordinate(CompositeCoordinateBase.PIXEL_ZERO, heatmapFullPixelsWidth, 0);
                    CompositeVerticalCoordinate y2 = new CompositeVerticalCoordinate(CompositeCoordinateBase.PIXEL_ZERO, heatmapPixelsHeight, 0);
                    if (heatmapIcon == null) {
                        CompositeHorizontalCoordinate x1 = new CompositeHorizontalCoordinate(CompositeCoordinateBase.PIXEL_ZERO, 0, 0);
                        CompositeVerticalCoordinate y1 = new CompositeVerticalCoordinate(CompositeCoordinateBase.PIXEL_ZERO, 0, 0);
                        heatmapIcon = new CanvasIcon(icon, x1, y1, x2, y2);
                        heatmapCanvas.addShape(heatmapIcon);
                    } else {
                        heatmapIcon.setImage(icon);
                        heatmapIcon.setX2(x2);
                        heatmapIcon.setY2(y2);
                    }
                    needToUpdateHeatmapImage = false;
                }
                if (needToUpdateRightOfTimelineImage) {
                    PreparedImage icon = generateCrossedBoxIcon(
                            rightOfTimelineWidth, heatmapPixelsHeight, Color.GREEN.darker(), 3, true);
                    
                    CompositeHorizontalCoordinate x2 = new CompositeHorizontalCoordinate(CompositeCoordinateBase.PIXEL_ZERO, rightOfTimelineWidth, 0);
                    CompositeVerticalCoordinate y2 = new CompositeVerticalCoordinate(CompositeCoordinateBase.PIXEL_ZERO, heatmapPixelsHeight, 0);
                    if (rightOfTimelineIcon == null) {
                        CompositeHorizontalCoordinate x1 = new CompositeHorizontalCoordinate(CompositeCoordinateBase.PIXEL_ZERO, 0, 0);
                        CompositeVerticalCoordinate y1 = new CompositeVerticalCoordinate(CompositeCoordinateBase.PIXEL_ZERO, 0, 0);
                        rightOfTimelineIcon = new CanvasIcon(icon, x1, y1, x2, y2);
                        timelineCanvas.addShape(rightOfTimelineIcon);
                    } else {
                        rightOfTimelineIcon.setImage(icon);
                        rightOfTimelineIcon.setX2(x2);
                        rightOfTimelineIcon.setY2(y2);
                    }
                    needToUpdateRightOfTimelineImage = false;
                }
            }

            private PreparedImage generateCrossedBoxIcon(int width, int height, Color color, int lineWidth, boolean dashed) {
                BufferedImage icon = new BufferedImage(width, height,
                        BufferedImage.TYPE_INT_ARGB);
                Graphics2D graphics = (Graphics2D) icon.getGraphics();
                graphics.setColor(color);
                if (dashed) {
                    graphics.setStroke(new BasicStroke(lineWidth, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER,
                            10.0f, new float[] {20f, 20f}, 0.0f));
                } else {
                    graphics.setStroke(new BasicStroke(lineWidth));
                }
                
                // Coloring outermost pixels of heatmap creating a frame
                graphics.drawRect(0, 0, width - 1, height - 1);
                // And two diagonal lines
                graphics.drawLine(0, 0, width - 1, height - 1);
                graphics.drawLine(0, height - 1, width - 1, 0);
                graphics.dispose();

                PreparedImage preparedImage = new PreparedImage(icon);
                return preparedImage;
            }
            
            @Override
            public void onRightOfTimelineWidth(int rightOfTimelineWidth) {
                this.rightOfTimelineWidth = rightOfTimelineWidth;
                needToUpdateRightOfTimelineImage = true;
            }
            
            @Override
            public void dispose() {
                heatmapCanvas.dispose();
                timelineCanvas.dispose();
            }
        };
    }
}
