package velox.api.layer1.simpledemo.screenspacepainter;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import velox.api.layer1.Layer1ApiAdminAdapter;
import velox.api.layer1.Layer1ApiDataAdapter;
import velox.api.layer1.Layer1ApiFinishable;
import velox.api.layer1.Layer1ApiProvider;
import velox.api.layer1.annotations.Layer1ApiVersion;
import velox.api.layer1.annotations.Layer1ApiVersionValue;
import velox.api.layer1.annotations.Layer1Attachable;
import velox.api.layer1.annotations.Layer1StrategyName;
import velox.api.layer1.common.ListenableHelper;
import velox.api.layer1.data.InstrumentInfo;
import velox.api.layer1.data.TradeInfo;
import velox.api.layer1.layers.strategies.interfaces.ScreenSpaceCanvas;
import velox.api.layer1.layers.strategies.interfaces.ScreenSpaceCanvas.CanvasIcon;
import velox.api.layer1.layers.strategies.interfaces.ScreenSpaceCanvas.HorizontalCoordinate;
import velox.api.layer1.layers.strategies.interfaces.ScreenSpaceCanvas.PreparedImage;
import velox.api.layer1.layers.strategies.interfaces.ScreenSpaceCanvas.RelativeDataHorizontalCoordinate;
import velox.api.layer1.layers.strategies.interfaces.ScreenSpaceCanvas.RelativeDataVerticalCoordinate;
import velox.api.layer1.layers.strategies.interfaces.ScreenSpaceCanvas.RelativePixelHorizontalCoordinate;
import velox.api.layer1.layers.strategies.interfaces.ScreenSpaceCanvas.RelativePixelVerticalCoordinate;
import velox.api.layer1.layers.strategies.interfaces.ScreenSpaceCanvas.VerticalCoordinate;
import velox.api.layer1.layers.strategies.interfaces.ScreenSpaceCanvasFactory;
import velox.api.layer1.layers.strategies.interfaces.ScreenSpaceCanvasFactory.ScreenSpaceCanvasType;
import velox.api.layer1.layers.strategies.interfaces.ScreenSpacePainter;
import velox.api.layer1.layers.strategies.interfaces.ScreenSpacePainterAdapter;
import velox.api.layer1.layers.strategies.interfaces.ScreenSpacePainterFactory;
import velox.api.layer1.messages.UserMessageLayersChainCreatedTargeted;
import velox.api.layer1.messages.indicators.DataStructureInterface;
import velox.api.layer1.messages.indicators.Layer1ApiUserMessageModifyScreenSpacePainter;
import velox.api.layer1.simpledemo.markers.Layer1ApiMarkersDemo;

/**
 * <p>
 * This demo shows how to overlay some custom data, e.g. orders. This is a
 * relatively advanced demo, it might be best to start with one of the simpler
 * ones.
 * </p>
 * <p>
 * This demo shows how you could overlay some external database on top of data
 * visualized by bookmap.
 * </p>
 * <p>
 * Let's imagine there is an order placed every 1 minute.
 * </p>
 * <ol>
 * <li>It is placed somewhere near BBO (for simplicity - let's take some random
 * price; it's possible to get necessary data using
 * {@link DataStructureInterface}, but that's outside of the scope of this demo
 * - see {@link Layer1ApiMarkersDemo} if that's something you are interested in)
 * </li>
 * <li>In 5 seconds moved 10 levels down (keep in mind that to convert price to
 * levels it has to be divided by {@link InstrumentInfo#pips})</li>
 * <li>In 3 seconds it's cancelled.</li>
 * </ol>
 * <p>
 * Orders are placed with varying offsets relative to the price. After every 2
 * orders color randomly changes.
 * </p>
 * <p>
 * Important: indicator will only starts working after it gets at least one
 * trade though realtime data, so you need to let some data play (so it receives
 * at least 1 trade). This can be solved using {@link DataStructureInterface}
 * </p>
 */
@Layer1Attachable
@Layer1StrategyName("SSP data overlay")
@Layer1ApiVersion(Layer1ApiVersionValue.VERSION2)
public class Layer1OrdersOverlayDemo implements
    Layer1ApiFinishable,
    Layer1ApiAdminAdapter,
    Layer1ApiDataAdapter,
    ScreenSpacePainterFactory
{
 
    private static final String INDICATOR_NAME = "Data overlay demo";

    private static final int CACHE_MAX_SIZE = 100;
    
    private static final long FAKE_ORDERS_INTERVAL = TimeUnit.MINUTES.toNanos(1);
    
    private Layer1ApiProvider provider;

    private Map<String, String> indicatorsFullNameToUserName = new HashMap<>();
    private Map<String, String> indicatorsUserNameToFullName = new HashMap<>();
    
    /**
     * Some price for the instrument. Reference point for generating fake orders.
     * It's not needed in real use-case where data is retrieved from external
     * source, it's here only to generate reasonable demo data.
     */
    private Map<String, Integer> prices = new ConcurrentHashMap<>();

    public Layer1OrdersOverlayDemo(Layer1ApiProvider provider) {
        this.provider = provider;
        
        ListenableHelper.addListeners(provider, this);
    }
    
    @Override
    public void finish() {
        synchronized (indicatorsFullNameToUserName) {
            for (String userName : indicatorsFullNameToUserName.values()) {
                Layer1ApiUserMessageModifyScreenSpacePainter message = Layer1ApiUserMessageModifyScreenSpacePainter
                        .builder(Layer1OrdersOverlayDemo.class, userName).setIsAdd(false).build();
                provider.sendUserMessage(message);
            }
        }
    }
    
    private Layer1ApiUserMessageModifyScreenSpacePainter getUserMessageAdd(String userName) {
        return Layer1ApiUserMessageModifyScreenSpacePainter.builder(Layer1OrdersOverlayDemo.class, userName)
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

        return new ScreenSpacePainterAdapter() {

            long heatmapTimeLeft;
            long heatmapActiveTimeWidth;
            
            boolean needToUpdateOrders = true;

            // Caches, dropped every time size exceeds threshold.
            private HashMap<Color, PreparedImage> cancelationImagesCache = new HashMap<>();
            private HashMap<Color, PreparedImage> lineImagesCache = new HashMap<>();
            
            // List of icons we are currently showing - used to allow removal of shown icons.
            private List<CanvasIcon> shownIcons = new ArrayList<>();
            
            @Override
            public void onHeatmapTimeLeft(long heatmapTimeLeft) {
                needToUpdateOrders = true;
                this.heatmapTimeLeft = heatmapTimeLeft;
            }
            
            @Override
            public void onHeatmapActiveTimeWidth(long heatmapActiveTimeWidth) {
                needToUpdateOrders = true;
                this.heatmapActiveTimeWidth = heatmapActiveTimeWidth;
            }

            @Override
            public void onMoveEnd() {
                if (needToUpdateOrders) {
                    
                    for (CanvasIcon icon : shownIcons) {
                        heatmapCanvas.removeShape(icon);
                    }
                    shownIcons.clear();
                    
                    // Computing and drawing fake orders on the screen
                    long leftmostFakeOrderIndex = heatmapTimeLeft / FAKE_ORDERS_INTERVAL;
                    long rightmostFakeOrderIndex = (heatmapTimeLeft + heatmapActiveTimeWidth + FAKE_ORDERS_INTERVAL - 1) / FAKE_ORDERS_INTERVAL;
                    
                    for (long orderIndex = leftmostFakeOrderIndex; orderIndex <= rightmostFakeOrderIndex; ++orderIndex) {
                        shownIcons.addAll(generateIconsFor(orderIndex));
                    }
                    for (CanvasIcon icon : shownIcons) {
                        heatmapCanvas.addShape(icon);
                    }

                    needToUpdateOrders = false;
                }
            }

            private Collection<? extends CanvasIcon> generateIconsFor(long orderIndex) {
                
                Integer basePrice = prices.get(indicatorAlias);
                
                if (basePrice == null) {
                    return Collections.emptyList();
                }
                
                long placementTime = orderIndex * FAKE_ORDERS_INTERVAL;
                int placementPrice = basePrice + (int) (orderIndex % 10);
                
                long modificationTime = placementTime + TimeUnit.SECONDS.toNanos(5);
                int modificationPrice =  placementPrice - 10;
                
                long cancellationTime = modificationTime + TimeUnit.SECONDS.toNanos(3);
                
                Random colorRandom = new Random(orderIndex / 2);
                Color orderColor = new Color(0.5f + colorRandom.nextFloat() / 2, colorRandom.nextFloat(), colorRandom.nextFloat());
                
                ArrayList<CanvasIcon> icons = new ArrayList<>();
                
                icons.add(genearateLineIcon(placementTime, placementPrice, modificationTime, placementPrice, orderColor));
                icons.add(genearateLineIcon(modificationTime, placementPrice, modificationTime, modificationPrice, orderColor));
                icons.add(genearateLineIcon(modificationTime, modificationPrice, cancellationTime, modificationPrice, orderColor));
                icons.add(genearateCancelationIcon(cancellationTime, modificationPrice, orderColor));
                
                icons.removeIf(Objects::isNull);
                
                return icons;
            }

            private CanvasIcon genearateLineIcon(long t1, int p1, long t2,
                    int p2, Color color) {
                
                // Clipping and ignoring orders outside of displayed area.
                // We could also filter vertically in a similar way,
                // but that's not so important unless we have many orders
                // or can zoom in vertically a lot.
                t1 = Math.max(heatmapTimeLeft, t1);
                t2 = Math.min(heatmapTimeLeft + heatmapActiveTimeWidth, t2);
                if (t1 > t2) {
                    return null;
                }
                
                PreparedImage image = generateLineImage(color);
                
                // Compute coordinates of the line itself (with 0 width)
                HorizontalCoordinate x1 = new RelativeDataHorizontalCoordinate(
                        RelativeDataHorizontalCoordinate.HORIZONTAL_DATA_ZERO, t1);
                HorizontalCoordinate x2 = new RelativeDataHorizontalCoordinate(
                        RelativeDataHorizontalCoordinate.HORIZONTAL_DATA_ZERO, t2);
                VerticalCoordinate y1 = new RelativeDataVerticalCoordinate(
                        RelativeDataVerticalCoordinate.VERTICAL_DATA_ZERO, p1);
                VerticalCoordinate y2 = new RelativeDataVerticalCoordinate(
                        RelativeDataVerticalCoordinate.VERTICAL_DATA_ZERO, p2);
                
                // Expand line 1px to every side (so it's 2px wide)
                x1 = new RelativePixelHorizontalCoordinate(x1, -2);
                x2 = new RelativePixelHorizontalCoordinate(x2, 2);
                y1 = new RelativePixelVerticalCoordinate(y1, -2);
                y2 = new RelativePixelVerticalCoordinate(y2, 2);
                
                return new CanvasIcon(image, x1, y1, x2, y2);
            }
            
            private CanvasIcon genearateCancelationIcon(long t, int p, Color color) {
                
                // Ignoring orders outside of displayed area.
                // We could also filter vertically in a similar way,
                // but that's not so important unless we have many orders
                // or can zoom in vertically a lot.
                if (t < heatmapTimeLeft || heatmapTimeLeft + heatmapActiveTimeWidth < t) {
                    return null;
                }
                
                PreparedImage image = generateCancelationImage(color);
                
                HorizontalCoordinate centerX = new RelativeDataHorizontalCoordinate(
                        RelativeDataHorizontalCoordinate.HORIZONTAL_DATA_ZERO, t);
                VerticalCoordinate centerY = new RelativeDataVerticalCoordinate(
                        RelativeDataVerticalCoordinate.VERTICAL_DATA_ZERO, p);
                
                // Compute coordinates of the icon corners
                HorizontalCoordinate x1 = new RelativePixelHorizontalCoordinate(
                        centerX, -image.getReadOnlyImage().getWidth() / 2);
                HorizontalCoordinate x2 = new RelativePixelHorizontalCoordinate(
                        centerX, image.getReadOnlyImage().getWidth() / 2);
                VerticalCoordinate y1 = new RelativePixelVerticalCoordinate(
                        centerY, -image.getReadOnlyImage().getWidth() / 2);
                VerticalCoordinate y2 = new RelativePixelVerticalCoordinate(
                        centerY, image.getReadOnlyImage().getWidth() / 2);
                
                return new CanvasIcon(image, x1, y1, x2, y2);
            }

            private PreparedImage generateLineImage(Color color) {
                // Getting line image from cache or creating a new one and caching it
                PreparedImage preparedImage = lineImagesCache.get(color);
                if (preparedImage == null) {
                    if (lineImagesCache.size() >= CACHE_MAX_SIZE) {
                        lineImagesCache.clear();
                    }
                    BufferedImage bufferedImage = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
                    Graphics2D graphics = bufferedImage.createGraphics();
                    graphics.setColor(color);
                    graphics.fillRect(0, 0, bufferedImage.getWidth(), bufferedImage.getHeight());
                    graphics.dispose();
                    preparedImage = new PreparedImage(bufferedImage);
                    lineImagesCache.put(color, preparedImage);
                }
                return preparedImage;
            }
            
            private PreparedImage generateCancelationImage(Color color) {
                // Getting line image from cache or creating a new one and caching it
                PreparedImage preparedImage = cancelationImagesCache.get(color);
                if (preparedImage == null) {
                    if (cancelationImagesCache.size() >= CACHE_MAX_SIZE) {
                        cancelationImagesCache.clear();
                    }
                    BufferedImage bufferedImage = new BufferedImage(30, 30, BufferedImage.TYPE_INT_ARGB);
                    Graphics2D graphics = bufferedImage.createGraphics();
                    graphics.setStroke(new BasicStroke(4f));
                    graphics.setColor(color);
                    graphics.drawOval(2, 2, bufferedImage.getWidth() - 4, bufferedImage.getHeight() - 4);
                    graphics.setStroke(new BasicStroke(2f));
                    graphics.drawLine(5, 5, 25, 25);
                    graphics.drawLine(5, 25, 25, 5);
                    graphics.dispose();
                    preparedImage = new PreparedImage(bufferedImage);
                    cancelationImagesCache.put(color, preparedImage);
                }
                return preparedImage;
            }

            @Override
            public void dispose() {
                heatmapCanvas.dispose();
            }
        };
    }
    
    @Override
    public void onTrade(String alias, double price, int size, TradeInfo tradeInfo) {
        // Remembering first trade price rounded to nearest level.
        prices.putIfAbsent(alias, (int) Math.round(price));
    }
}
