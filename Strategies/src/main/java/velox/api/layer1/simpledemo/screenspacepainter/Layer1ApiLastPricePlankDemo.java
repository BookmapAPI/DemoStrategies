package velox.api.layer1.simpledemo.screenspacepainter;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

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
import velox.api.layer1.data.InstrumentInfo;
import velox.api.layer1.data.TradeInfo;
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

@Layer1Attachable
@Layer1StrategyName("SSP last price")
@Layer1ApiVersion(Layer1ApiVersionValue.VERSION2)
public class Layer1ApiLastPricePlankDemo implements
    Layer1ApiFinishable,
    Layer1ApiAdminAdapter,
    Layer1ApiDataAdapter,
    Layer1ApiInstrumentAdapter,
    ScreenSpacePainterFactory
{
    class LastPricePainter implements ScreenSpacePainterAdapter {
        
        private static final int PLANK_HEIGHT = 20;
        
        private final ScreenSpaceCanvas canvas;
        private final String alias;

        int heatmapPixelsHeight;
        int rightOfTimelineWidth;
        
        boolean needToUpdatePlankImage = true;
        
        CanvasIcon plankIcon;
        
        double lastPrice;
        
        public LastPricePainter(ScreenSpaceCanvas canvas, String alias) {
            this.canvas = canvas;
            this.alias = alias;
            this.lastPrice = lastPrices.getOrDefault(alias, Double.NaN);
        }
        
        private void onLastPrice(double lastPrice) {
            this.lastPrice = lastPrice;
            
            // If initialized
            if (heatmapPixelsHeight > 0) {
                update();
            }
        }
        
        @Override
        public void onHeatmapPixelsHeight(int heatmapPixelsHeight) {
            this.heatmapPixelsHeight = heatmapPixelsHeight;
        }
        
        @Override
        public void onRightOfTimelineWidth(int rightOfTimelineWidth) {
            this.rightOfTimelineWidth = rightOfTimelineWidth;
            needToUpdatePlankImage = true;
        }

        @Override
        public void onMoveEnd() {
            update();
        }

        private synchronized void update() {
            // Return as we don't have the necessary info for calculation yet.
            if (!instrumentInfos.containsKey(alias)) {
                return;
            }
            
            CompositeHorizontalCoordinate x1 = new CompositeHorizontalCoordinate(CompositeCoordinateBase.PIXEL_ZERO, 0, 0);
            CompositeVerticalCoordinate y1 = new CompositeVerticalCoordinate(CompositeCoordinateBase.DATA_ZERO, - PLANK_HEIGHT / 2, lastPrice);
            CompositeHorizontalCoordinate x2 = new CompositeHorizontalCoordinate(CompositeCoordinateBase.PIXEL_ZERO, rightOfTimelineWidth, 0);
            CompositeVerticalCoordinate y2 = new CompositeVerticalCoordinate(CompositeCoordinateBase.DATA_ZERO, PLANK_HEIGHT - PLANK_HEIGHT / 2, lastPrice);
            
            
            if (needToUpdatePlankImage) {
                PreparedImage icon = generatLastPriceIcon(rightOfTimelineWidth, PLANK_HEIGHT, alias, lastPrice);
                if (plankIcon == null) {
                    plankIcon = new CanvasIcon(icon, x1, y1, x2, y2);
                    canvas.addShape(plankIcon);
                } else {
                    plankIcon.setImage(icon);
                    plankIcon.setX2(x2);
                }
            }
            
            plankIcon.setY1(y1);
            plankIcon.setY2(y2);
        }

        private PreparedImage generatLastPriceIcon(int width, int height, String alias, double price) {
            BufferedImage icon = new BufferedImage(width, height,
                    BufferedImage.TYPE_INT_ARGB);
            Graphics2D graphics = (Graphics2D) icon.getGraphics();
            graphics.setBackground(Color.GRAY);
            graphics.clearRect(0, 0, icon.getWidth(), icon.getHeight());
            
            graphics.setColor(Color.WHITE);
            graphics.setFont(graphics.getFont().deriveFont(22f));
            
            double pips = instrumentInfos.get(alias).pips;
            String priceString = provider.formatPrice(alias, price * pips);
            
            graphics.drawString(priceString, 0, height);

            graphics.dispose();
            
            PreparedImage preparedImage = new PreparedImage(icon);
            return preparedImage;
        }
        
        @Override
        public void dispose() {
            canvas.dispose();
        }
        
        
    }
    
 
    private static final String INDICATOR_NAME = "Last price plank";
    
    private Layer1ApiProvider provider;

    private Map<String, String> indicatorsFullNameToUserName = new HashMap<>();
    private Map<String, String> indicatorsUserNameToFullName = new HashMap<>();
    
    private Map<String, LastPricePainter> painters = Collections.synchronizedMap(new HashMap<>());

    private Map<String, InstrumentInfo> instrumentInfos = Collections.synchronizedMap(new HashMap<>());
    private Map<String, Double> lastPrices = Collections.synchronizedMap(new HashMap<>());

    public Layer1ApiLastPricePlankDemo(Layer1ApiProvider provider) {
        this.provider = provider;
        
        ListenableHelper.addListeners(provider, this);
    }
    
    @Override
    public void finish() {
        synchronized (indicatorsFullNameToUserName) {
            for (String userName : indicatorsFullNameToUserName.values()) {
                Layer1ApiUserMessageModifyScreenSpacePainter message = Layer1ApiUserMessageModifyScreenSpacePainter
                        .builder(Layer1ApiLastPricePlankDemo.class, userName).setIsAdd(false).build();
                provider.sendUserMessage(message);
            }
        }
    }
    
    private Layer1ApiUserMessageModifyScreenSpacePainter getUserMessageAdd(String userName) {
        return Layer1ApiUserMessageModifyScreenSpacePainter.builder(Layer1ApiLastPricePlankDemo.class, userName)
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
        ScreenSpaceCanvas canvas = screenSpaceCanvasFactory.createCanvas(ScreenSpaceCanvasType.RIGHT_OF_TIMELINE);

        LastPricePainter lastPricePainter = new LastPricePainter(canvas, indicatorAlias);
        painters.put(indicatorAlias, lastPricePainter);
        return lastPricePainter;
    }
    
    @Override
    public void onInstrumentAdded(String alias, InstrumentInfo instrumentInfo) {
        instrumentInfos.put(alias, instrumentInfo);
        
        if (painters.containsKey(alias)) {
            painters.get(alias).update();
        }
    }
    
    @Override
    public void onTrade(String alias, double price, int size, TradeInfo tradeInfo) {
        LastPricePainter painter = painters.get(alias);
        if (painter != null) {
            painter.onLastPrice(price);
        }
        lastPrices.put(alias, price);
    }
}
