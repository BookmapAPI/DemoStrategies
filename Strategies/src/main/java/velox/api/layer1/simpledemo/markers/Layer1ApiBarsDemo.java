package velox.api.layer1.simpledemo.markers;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;

import velox.api.layer1.Layer1ApiAdminAdapter;
import velox.api.layer1.Layer1ApiFinishable;
import velox.api.layer1.Layer1ApiInstrumentListener;
import velox.api.layer1.Layer1ApiProvider;
import velox.api.layer1.annotations.Layer1ApiVersion;
import velox.api.layer1.annotations.Layer1ApiVersionValue;
import velox.api.layer1.annotations.Layer1Attachable;
import velox.api.layer1.annotations.Layer1StrategyName;
import velox.api.layer1.common.ListenableHelper;
import velox.api.layer1.common.Log;
import velox.api.layer1.data.BalanceInfo;
import velox.api.layer1.data.ExecutionInfo;
import velox.api.layer1.data.InstrumentInfo;
import velox.api.layer1.data.MarketMode;
import velox.api.layer1.data.OrderInfoUpdate;
import velox.api.layer1.data.StatusInfo;
import velox.api.layer1.data.TradeInfo;
import velox.api.layer1.layers.strategies.interfaces.CalculatedResultListener;
import velox.api.layer1.layers.strategies.interfaces.CustomEventAggregatble;
import velox.api.layer1.layers.strategies.interfaces.CustomGeneratedEvent;
import velox.api.layer1.layers.strategies.interfaces.CustomGeneratedEventAliased;
import velox.api.layer1.layers.strategies.interfaces.InvalidateInterface;
import velox.api.layer1.layers.strategies.interfaces.OnlineCalculatable;
import velox.api.layer1.layers.strategies.interfaces.OnlineValueCalculatorAdapter;
import velox.api.layer1.messages.GeneratedEventInfo;
import velox.api.layer1.messages.Layer1ApiUserMessageAddStrategyUpdateGenerator;
import velox.api.layer1.messages.UserMessageLayersChainCreatedTargeted;
import velox.api.layer1.messages.indicators.DataStructureInterface;
import velox.api.layer1.messages.indicators.DataStructureInterface.TreeResponseInterval;
import velox.api.layer1.messages.indicators.IndicatorColorScheme;
import velox.api.layer1.messages.indicators.IndicatorLineStyle;
import velox.api.layer1.messages.indicators.Layer1ApiDataInterfaceRequestMessage;
import velox.api.layer1.messages.indicators.Layer1ApiUserMessageModifyIndicator;
import velox.api.layer1.messages.indicators.Layer1ApiUserMessageModifyIndicator.GraphType;
import velox.api.layer1.messages.indicators.StrategyUpdateGenerator;

@Layer1Attachable
@Layer1StrategyName("Bars demo")
@Layer1ApiVersion(Layer1ApiVersionValue.VERSION2)
public class Layer1ApiBarsDemo implements
    Layer1ApiFinishable,
    Layer1ApiAdminAdapter,
    Layer1ApiInstrumentListener,
    OnlineCalculatable {
 
    private static class BarEvent implements CustomGeneratedEvent, DataCoordinateMarker {
        private static final long serialVersionUID = 1L;
        /**
         * While bar is being accumulated we store open time here, then we change it to
         * actual event time.
         */
        private long time;

        double open;
        double low;
        double high;
        double close;
        
        transient int bodyWidthPx;
        
        public BarEvent(long time) {
            this(time, Double.NaN);
        }
        
        public BarEvent(long time, double open) {
            this(time, open, -1);
        }
        
        public BarEvent(long time, double open, int bodyWidthPx) {
            this(time, open, open, open, open, bodyWidthPx);
        }
        
        public BarEvent(long time, double open, double low, double high, double close, int bodyWidthPx) {
            super();
            this.time = time;
            this.open = open;
            this.low = low;
            this.high = high;
            this.close = close;
            this.bodyWidthPx = bodyWidthPx;
        }
        
        public BarEvent(BarEvent other) {
            this(other.time, other.open, other.low, other.high, other.close, other.bodyWidthPx);
        }

        public void setTime(long time) {
            this.time = time;
        }
    
        @Override
        public long getTime() {
            return time;
        }
        
        @Override
        public Object clone() {
            return new BarEvent(time, open, low, high, close, bodyWidthPx);
        }

        @Override
        public String toString() {
            return "[" + time + ": "+ open + "/" + low + "/" + high + "/" + close + "]";
        }

        @Override
        public double getMinY() {
            return open;
        }

        @Override
        public double getMaxY() {
            return high;
        }

        @Override
        public double getValueY() {
            return low;
        }
        
        public void update(double price) {
            if (Double.isNaN(price)) {
                return;
            }
            
            // If bar was not initialized yet
            if (Double.isNaN(open)) {
                open = price;
                low = price;
                high = price;
            } else {
                low = Math.min(low, price);
                high = Math.max(high, price);
            }
            close = price;
        }
        
        public void update(BarEvent nextBar) {
            // Inefficient, but simple
            update(nextBar.open);
            update(nextBar.low);
            update(nextBar.high);
            update(nextBar.close);
        }
        
        public void setBodyWidthPx(int bodyWidthPx) {
            this.bodyWidthPx = bodyWidthPx;
        }

        @Override
        public Marker makeMarker(Function<Double, Integer> yDataCoordinateToPixelFunction) {
            
            /*
             * Note, that caching and reusing markers would improve efficiency, but for
             * simplicity we won't do that here. If you do decide to cache the icons - be
             * mindful of the cache size.
             */
            
            int top = yDataCoordinateToPixelFunction.apply(high);
            int bottom = yDataCoordinateToPixelFunction.apply(low);
            int openPx = yDataCoordinateToPixelFunction.apply(open);
            int closePx = yDataCoordinateToPixelFunction.apply(close);
            
            int bodyLow = Math.min(openPx, closePx);
            int bodyHigh = Math.max(openPx, closePx);
            
            int imageHeight = top - bottom + 1;
            BufferedImage bufferedImage = new BufferedImage(bodyWidthPx, imageHeight, BufferedImage.TYPE_INT_ARGB);
            int imageCenterX = bufferedImage.getWidth() / 2;
            
            Graphics2D graphics = bufferedImage.createGraphics();
            // Clear background
            graphics.setBackground(new Color(0, 0, 0, 0));
            graphics.clearRect(0, 0, bufferedImage.getWidth(), bufferedImage.getHeight());
            
            /*
             * Draw "shadow", also known as "wick". Here we'll take advantage of the fact
             * we'll later draw a non-transparent body over it. If body would be
             * semi-transparent you'd have to take that into account and leave (or make) a
             * gap in the shadow.
             */
            graphics.setColor(Color.WHITE);
            graphics.drawLine(imageCenterX, 0, imageCenterX, imageHeight);
            
            
            /*
             * Draw body. Keep in mind that BufferedImage coordinate system starts from the
             * left top corner and Y axis points downwards
             */
            graphics.setColor(open < close ? Color.GREEN : Color.RED);
            graphics.fillRect(0, top - bodyHigh, bodyWidthPx, bodyHigh - bodyLow + 1);
            
            graphics.dispose();

            /*
             * This one is a little tricky. We have a reference point which we'll pass as
             * markerY. Now we need to compute offsets so that icon is where we want it to
             * be. Since we took close as a reference point, we want to offset the icon so
             * that close is at the markerY. Zero offset would align bottom of the icon with
             * a value, so we do this:
             */
            int iconOffsetY = bottom - closePx;
            /*
             * This one is simple, we just want to center the bar vertically over where it
             * should be.
             */
            int iconOffsetX = -imageCenterX;
            return new Marker(close, iconOffsetX, iconOffsetY, bufferedImage);
        }

        /**
         * We initially compute everything in level number, like onDepth calls are
         * (prices divided by pips), but if we are going to render it on the bottom
         * panel we want to convert into price
         */
        public void applyPips(double pips) {
            open *= pips;
            low *= pips;
            high *= pips;
            close *= pips;
        }
    }

    public static final CustomEventAggregatble BAR_EVENTS_AGGREGATOR = new CustomEventAggregatble() {
        @Override
        public CustomGeneratedEvent getInitialValue(long t) {
            return new BarEvent(t);
        }

        @Override
        public void aggregateAggregationWithValue(CustomGeneratedEvent aggregation, CustomGeneratedEvent value) {
            BarEvent aggregationEvent = (BarEvent) aggregation;
            BarEvent valueEvent = (BarEvent) value;
            aggregationEvent.update(valueEvent);
        }
        
        @Override
        public void aggregateAggregationWithAggregation(CustomGeneratedEvent aggregation1,
                CustomGeneratedEvent aggregation2) {
            BarEvent aggregationEvent1 = (BarEvent) aggregation1;
            BarEvent aggregationEvent2 = (BarEvent) aggregation2;
            aggregationEvent1.update(aggregationEvent2);
        }
    };
    
    private static final String INDICATOR_NAME_BARS_MAIN = "Bars: main chart";
    private static final String INDICATOR_NAME_BARS_BOTTOM = "Bars: bottom panel";
    private static final String INDICATOR_LINE_COLOR_NAME = "Trade markers line";
    private static final Color INDICATOR_LINE_DEFAULT_COLOR = Color.RED;
    
    private static final String TREE_NAME = "Bars";
    private static final Class<?>[] INTERESTING_CUSTOM_EVENTS = new Class<?>[] { BarEvent.class };

    private static final int MAX_BODY_WIDTH = 30;
    private static final int MIN_BODY_WIDTH = 1;
    private static final long CANDLE_INTERVAL_NS = TimeUnit.SECONDS.toNanos(30);
    
    private Layer1ApiProvider provider;
    
    private Map<String, String> indicatorsFullNameToUserName = new HashMap<>();
    private Map<String, String> indicatorsUserNameToFullName = new HashMap<>();

    private Map<String, Double> pipsMap = new ConcurrentHashMap<>();
    
    private DataStructureInterface dataStructureInterface;
    
    private BufferedImage tradeIcon = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
    private Map<String, BufferedImage> orderIcons = Collections.synchronizedMap(new HashMap<>());
    
    private Object locker = new Object();

    public Layer1ApiBarsDemo(Layer1ApiProvider provider) {
        this.provider = provider;
        
        ListenableHelper.addListeners(provider, this);
        
        // Prepare trade marker
        Graphics graphics = tradeIcon.getGraphics();
        graphics.setColor(Color.BLUE);
        graphics.drawLine(0, 0, 15, 15);
        graphics.drawLine(15, 0, 0, 15);
    }
    
    @Override
    public void finish() {
        synchronized (indicatorsFullNameToUserName) {
            for (String userName: indicatorsFullNameToUserName.values()) {
                provider.sendUserMessage(new Layer1ApiUserMessageModifyIndicator(Layer1ApiBarsDemo.class, userName, false));
            }
        }
        
        provider.sendUserMessage(getGeneratorMessage(false));
    }
    
    private Layer1ApiUserMessageModifyIndicator getUserMessageAdd(String userName, GraphType graphType) {
        return Layer1ApiUserMessageModifyIndicator.builder(Layer1ApiBarsDemo.class, userName)
                .setIsAdd(true)
                .setGraphType(graphType)
                .setOnlineCalculatable(this)
                .setIndicatorColorScheme(new IndicatorColorScheme() {
                    @Override
                    public ColorDescription[] getColors() {
                        return new ColorDescription[] {
                                new ColorDescription(Layer1ApiBarsDemo.class, INDICATOR_LINE_COLOR_NAME, INDICATOR_LINE_DEFAULT_COLOR, false),
                        };
                    }
                    
                    @Override
                    public String getColorFor(Double value) {
                        return INDICATOR_LINE_COLOR_NAME;
                    }

                    @Override
                    public ColorIntervalResponse getColorIntervalsList(double valueFrom, double valueTo) {
                        return new ColorIntervalResponse(new String[] {INDICATOR_LINE_COLOR_NAME}, new double[] {});
                    }
                })
                .setIndicatorLineStyle(IndicatorLineStyle.NONE)
                .build();
    }
    
    @Override
    public void onUserMessage(Object data) {
        if (data.getClass() == UserMessageLayersChainCreatedTargeted.class) {
            UserMessageLayersChainCreatedTargeted message = (UserMessageLayersChainCreatedTargeted) data;
            if (message.targetClass == getClass()) {
                provider.sendUserMessage(new Layer1ApiDataInterfaceRequestMessage(dataStructureInterface -> this.dataStructureInterface = dataStructureInterface));
                addIndicator(INDICATOR_NAME_BARS_MAIN);
                addIndicator(INDICATOR_NAME_BARS_BOTTOM);
                provider.sendUserMessage(getGeneratorMessage(true));
            }
        }
    }
    
    @Override
    public void onInstrumentAdded(String alias, InstrumentInfo instrumentInfo) {
        pipsMap.put(alias, instrumentInfo.pips);
    }

    @Override
    public void onInstrumentRemoved(String alias) {
    }
    
    @Override
    public void onInstrumentNotFound(String symbol, String exchange, String type) {
    }

    @Override
    public void onInstrumentAlreadySubscribed(String symbol, String exchange, String type) {
    }
    
    @Override
    public void calculateValuesInRange(String indicatorName, String indicatorAlias, long t0, long intervalWidth, int intervalsNumber,
            CalculatedResultListener listener) {
        String userName = indicatorsFullNameToUserName.get(indicatorName);
        boolean isBottomChart = userName.equals(INDICATOR_NAME_BARS_BOTTOM);
        
        Double pips = pipsMap.get(indicatorAlias);
        
        List<TreeResponseInterval> result = dataStructureInterface.get(Layer1ApiBarsDemo.class, TREE_NAME, t0,
                intervalWidth, intervalsNumber, indicatorAlias, INTERESTING_CUSTOM_EVENTS);
        
        int bodyWidth = getBodyWidth(intervalWidth);
        
        for (int i = 1; i <= intervalsNumber; i++) {
            
            BarEvent value = getBarEvent(result.get(i));
            if (value != null) {
                /*
                 * IMPORTANT: don't edit value returned by interface directly. It might be
                 * cached by bookmap for performance reasons, so you'll often end up with the
                 * modified value next time you request it, but it isn't going to happen every
                 * time, so the behavior wont be predictable.
                 */
                value = new BarEvent(value);
                
                value.setBodyWidthPx(bodyWidth);
                if (isBottomChart) {
                    value.applyPips(pips);
                }
                listener.provideResponse(value);
            } else {
                listener.provideResponse(Double.NaN);
            }
        }
        
        listener.setCompleted();
    }
    
    @Override
    public OnlineValueCalculatorAdapter createOnlineValueCalculator(String indicatorName, String indicatorAlias, long time,
            Consumer<Object> listener, InvalidateInterface invalidateInterface) {
        String userName = indicatorsFullNameToUserName.get(indicatorName);
        boolean isBottomChart = userName.equals(INDICATOR_NAME_BARS_BOTTOM);

        Double pips = pipsMap.get(indicatorAlias);
        
        return new OnlineValueCalculatorAdapter() {
            
            int bodyWidth = MAX_BODY_WIDTH;
            
            @Override
            public void onIntervalWidth(long intervalWidth) {
                this.bodyWidth = getBodyWidth(intervalWidth);
            }
            
            @Override
            public void onUserMessage(Object data) {
                if (data instanceof CustomGeneratedEventAliased) {
                    CustomGeneratedEventAliased aliasedEvent = (CustomGeneratedEventAliased) data;
                    if (indicatorAlias.equals(aliasedEvent.alias) && aliasedEvent.event instanceof BarEvent) {
                        BarEvent event = (BarEvent)aliasedEvent.event;
                        /*
                         * Same idea as in calculateValuesInRange - we don't want to mess up the
                         * message, but here it's for a different reason. We have a chance of changing
                         * it before or after it's stored inside bookmap, also resulting in undefined
                         * behavior.
                         */
                        event = new BarEvent(event);
                        event.setBodyWidthPx(bodyWidth);
                        if (isBottomChart) {
                            event.applyPips(pips);
                        }
                        listener.accept(event);
                    }
                }
            }
        };
    }
    
    private int getBodyWidth(long intervalWidth) {
        long bodyWidth = CANDLE_INTERVAL_NS / intervalWidth;
        bodyWidth = Math.max(bodyWidth, MIN_BODY_WIDTH);
        bodyWidth = Math.min(bodyWidth, MAX_BODY_WIDTH);
        return (int) bodyWidth;
        
    }
    
    private BarEvent getBarEvent(TreeResponseInterval treeResponseInterval) {
        Object result = treeResponseInterval.events.get(BarEvent.class.toString());
        if (result != null) {
            return (BarEvent) result;
        } else {
            return null;
        }
    }

    public void addIndicator(String userName) {
        Layer1ApiUserMessageModifyIndicator message = null;
        switch (userName) {
        case INDICATOR_NAME_BARS_MAIN:
            message = getUserMessageAdd(userName, GraphType.PRIMARY);
            break;
        case INDICATOR_NAME_BARS_BOTTOM:
            message = getUserMessageAdd(userName, GraphType.BOTTOM);
            break;
        default:
            Log.warn("Unknwon name for marker indicator: " + userName);
            break;
        }
        
        if (message != null) {
            synchronized (indicatorsFullNameToUserName) {
                indicatorsFullNameToUserName.put(message.fullName, message.userName);
                indicatorsUserNameToFullName.put(message.userName, message.fullName);
            }
            provider.sendUserMessage(message);
        }
    }
    
    private Layer1ApiUserMessageAddStrategyUpdateGenerator getGeneratorMessage(boolean isAdd) {
        return new Layer1ApiUserMessageAddStrategyUpdateGenerator(Layer1ApiBarsDemo.class, TREE_NAME, isAdd, true, new StrategyUpdateGenerator() {
            private Consumer<CustomGeneratedEventAliased> consumer;
            
            private long time = 0;

            private Map<String, BarEvent> aliasToLastBar = new HashMap<>();
            
            @Override
            public void setGeneratedEventsConsumer(Consumer<CustomGeneratedEventAliased> consumer) {
                this.consumer = consumer;
            }
            
            @Override
            public Consumer<CustomGeneratedEventAliased> getGeneratedEventsConsumer() {
                return consumer;
            }
            
            @Override
            public void onStatus(StatusInfo statusInfo) {
            }
            
            @Override
            public void onOrderUpdated(OrderInfoUpdate orderInfoUpdate) {
            }
            
            @Override
            public void onOrderExecuted(ExecutionInfo executionInfo) {
            }
            
            @Override
            public void onBalance(BalanceInfo balanceInfo) {
            }
            
            @Override
            public void onTrade(String alias, double price, int size, TradeInfo tradeInfo) {
                BarEvent bar = aliasToLastBar.get(alias);

                long barStartTime = getBarStartTime(time);

                if (bar == null) {
                    bar = new BarEvent(barStartTime);
                    aliasToLastBar.put(alias, bar);
                }
                
                if (barStartTime != bar.time) {
                    bar.setTime(time);
                    consumer.accept(new CustomGeneratedEventAliased(bar, alias));
                    bar = new BarEvent(barStartTime, bar.close);
                    aliasToLastBar.put(alias, bar);
                }
                
                if (size != 0) {
                    bar.update(price);
                }
            }
            
            @Override
            public void onMarketMode(String alias, MarketMode marketMode) {
            }
            
            @Override
            public void onDepth(String alias, boolean isBid, int price, int size) {
            }
            
            @Override
            public void onInstrumentAdded(String alias, InstrumentInfo instrumentInfo) {
            }
            
            @Override
            public void onInstrumentRemoved(String alias) {
                aliasToLastBar.remove(alias);
            }

            @Override
            public void onInstrumentNotFound(String symbol, String exchange, String type) {
            }

            @Override
            public void onInstrumentAlreadySubscribed(String symbol, String exchange, String type) {
            }

            @Override
            public void onUserMessage(Object data) {
            }
            
            @Override
            public void setTime(long time) {
                this.time = time;
                
                /*
                 * Publish finished bars. Bookmap call this method periodically even if nothing
                 * is happening at around 50ms intervals (avoid relying on exact value as it
                 * might be changed in the future).
                 */
                long barStartTime = getBarStartTime(time);
                for (Entry<String, BarEvent> entry : aliasToLastBar.entrySet()) {
                    String alias = entry.getKey();
                    BarEvent bar = entry.getValue();
                    
                    if (barStartTime != bar.time) {
                        bar.setTime(time);
                        consumer.accept(new CustomGeneratedEventAliased(bar, alias));
                        bar = new BarEvent(barStartTime, bar.close);
                        entry.setValue(bar);
                    }
                }
            }
        }, new GeneratedEventInfo[] {new GeneratedEventInfo(BarEvent.class, BarEvent.class, BAR_EVENTS_AGGREGATOR)});
    }
    
    private long getBarStartTime(long time) {
        return time - time % CANDLE_INTERVAL_NS;
    }
}
