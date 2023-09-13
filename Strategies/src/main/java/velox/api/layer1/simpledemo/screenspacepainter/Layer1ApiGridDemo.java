package velox.api.layer1.simpledemo.screenspacepainter;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import velox.api.layer1.Layer1ApiAdminAdapter;
import velox.api.layer1.Layer1ApiDataAdapter;
import velox.api.layer1.Layer1ApiFinishable;
import velox.api.layer1.Layer1ApiInstrumentAdapter;
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

/**
 * Emulates heavy computations with output in price/time coordinates
 */
@Layer1Attachable
@Layer1StrategyName("SSP grid")
@Layer1ApiVersion(Layer1ApiVersionValue.VERSION2)
public class Layer1ApiGridDemo implements
    Layer1ApiFinishable,
    Layer1ApiAdminAdapter,
    Layer1ApiDataAdapter,
    Layer1ApiInstrumentAdapter,
    ScreenSpacePainterFactory
{
    
    private static final int PRICE_GRID_SIZE = 10;
    private static final long TIME_GRID_SIZE = TimeUnit.SECONDS.toNanos(5);
    /** Just a rectangle 1x1 px. Allows drawing rectangles of any size (until API will start providing shapes functionality) */
    private static final PreparedImage GRID_PATTERN;
    static {
        
        BufferedImage gridPatternImage = new BufferedImage(1, 1,
                BufferedImage.TYPE_INT_ARGB);
        Graphics graphics = gridPatternImage.getGraphics();
        graphics.setColor(Color.CYAN);
        graphics.fillRect(0, 0, 1, 1);
        graphics.dispose();
        
        GRID_PATTERN = new PreparedImage(gridPatternImage);
    }
    
    class GridPainter implements ScreenSpacePainterAdapter {

        private final ScreenSpaceCanvas canvas;

        int heatmapFullPixelsWidth;
        int heatmapPixelsHeight;
        long heatmapTimeLeft;
        long heatmapActiveTimeWidth;
        long heatmapPriceHeight;
        double heatmapPriceBottom;
        
        
        List<CanvasIcon> gridLineIcons = new ArrayList<>();
        
        public GridPainter(ScreenSpaceCanvas canvas) {
            this.canvas = canvas;
        }

        @Override
        public void onHeatmapFullPixelsWidth(int heatmapFullPixelsWidth) {
            this.heatmapFullPixelsWidth = heatmapFullPixelsWidth;
        }
        
        @Override
        public void onHeatmapPixelsHeight(int heatmapPixelsHeight) {
            this.heatmapPixelsHeight = heatmapPixelsHeight;
        }
        
        @Override
        public void onHeatmapPriceHeight(long heatmapPriceHeight) {
            this.heatmapPriceHeight = heatmapPriceHeight;
        };
        
        @Override
        public void onHeatmapActiveTimeWidth(long heatmapActiveTimeWidth) {
            this.heatmapActiveTimeWidth = heatmapActiveTimeWidth;
        };
        
        @Override
        public void onHeatmapPriceBottom(long heatmapPriceBottom) {
            this.heatmapPriceBottom = heatmapPriceBottom;
        }
        
        @Override
        public void onHeatmapTimeLeft(long heatmapTimeLeft) {
            this.heatmapTimeLeft = heatmapTimeLeft;
        }

        @Override
        public void onMoveEnd() {
            update();
        }

        private synchronized void update() {
            
            for (CanvasIcon icon : gridLineIcons) {
                canvas.removeShape(icon);
            }
            gridLineIcons.clear();
            
            for (double y = heatmapPriceBottom + PRICE_GRID_SIZE - heatmapPriceBottom % PRICE_GRID_SIZE;
                    y < heatmapPriceHeight + heatmapPriceBottom;
                    y += PRICE_GRID_SIZE) {
                CompositeHorizontalCoordinate x1 = new CompositeHorizontalCoordinate(CompositeCoordinateBase.PIXEL_ZERO, 0, 0);
                CompositeVerticalCoordinate y1 = new CompositeVerticalCoordinate(CompositeCoordinateBase.DATA_ZERO, -1, y);
                CompositeHorizontalCoordinate x2 = new CompositeHorizontalCoordinate(CompositeCoordinateBase.PIXEL_ZERO, heatmapFullPixelsWidth, 0);
                CompositeVerticalCoordinate y2 = new CompositeVerticalCoordinate(CompositeCoordinateBase.DATA_ZERO, 1, y);
                
                CanvasIcon icon = new CanvasIcon(GRID_PATTERN, x1, y1, x2, y2);
                gridLineIcons.add(icon);
                canvas.addShape(icon);
            }
            
            for (long x = heatmapTimeLeft + TIME_GRID_SIZE - heatmapTimeLeft % TIME_GRID_SIZE;
                    x < heatmapActiveTimeWidth + heatmapTimeLeft;
                    x += TIME_GRID_SIZE) {
                CompositeHorizontalCoordinate x1 = new CompositeHorizontalCoordinate(CompositeCoordinateBase.DATA_ZERO, -1, x);
                CompositeVerticalCoordinate y1 = new CompositeVerticalCoordinate(CompositeCoordinateBase.PIXEL_ZERO, 0, 0);
                CompositeHorizontalCoordinate x2 = new CompositeHorizontalCoordinate(CompositeCoordinateBase.DATA_ZERO, 1, x);
                CompositeVerticalCoordinate y2 = new CompositeVerticalCoordinate(CompositeCoordinateBase.PIXEL_ZERO, heatmapPixelsHeight, 0);
                
                CanvasIcon icon = new CanvasIcon(GRID_PATTERN, x1, y1, x2, y2);
                gridLineIcons.add(icon);
                canvas.addShape(icon);
            }
        }
        
        @Override
        public void dispose() {
            canvas.dispose();
        }
    }
    
 
    private static final String INDICATOR_NAME = "Grid demo";
    
    private Layer1ApiProvider provider;

    private Map<String, String> indicatorsFullNameToUserName = new HashMap<>();
    private Map<String, String> indicatorsUserNameToFullName = new HashMap<>();

    public Layer1ApiGridDemo(Layer1ApiProvider provider) {
        this.provider = provider;
        
        ListenableHelper.addListeners(provider, this);
    }
    
    @Override
    public void finish() {
        synchronized (indicatorsFullNameToUserName) {
            for (String userName : indicatorsFullNameToUserName.values()) {
                Layer1ApiUserMessageModifyScreenSpacePainter message = Layer1ApiUserMessageModifyScreenSpacePainter
                        .builder(Layer1ApiGridDemo.class, userName).setIsAdd(false).build();
                provider.sendUserMessage(message);
            }
        }
    }
    
    private Layer1ApiUserMessageModifyScreenSpacePainter getUserMessageAdd(String userName) {
        return Layer1ApiUserMessageModifyScreenSpacePainter.builder(Layer1ApiGridDemo.class, userName)
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
        ScreenSpaceCanvas canvas = screenSpaceCanvasFactory.createCanvas(ScreenSpaceCanvasType.HEATMAP);

        GridPainter gridPainter = new GridPainter(canvas);
        return gridPainter;
    }
}
