package velox.api.layer1.simpledemo.markers.bars;

import velox.api.layer1.layers.strategies.interfaces.*;
import velox.api.layer1.messages.indicators.DataStructureInterface;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;

public class BarsOnlineCalculator implements OnlineCalculatable {

    private static final String INDICATOR_NAME_BARS_BOTTOM = "Bars: bottom panel";

    private static final String TREE_NAME = "Bars";

    private static final int MAX_BODY_WIDTH = 30;
    private static final int MIN_BODY_WIDTH = 1;
    private static final long CANDLE_INTERVAL_NS = TimeUnit.SECONDS.toNanos(30);

    private final Map<String, Double> pipsMap = new ConcurrentHashMap<>();

    private DataStructureInterface dataStructureInterface;

    private final Layer1ApiBarsDemo l;

    public BarsOnlineCalculator(Layer1ApiBarsDemo l) {
        this.l = l;
    }

    @Override
    public void calculateValuesInRange(String indicatorName, String indicatorAlias, long t0, long intervalWidth,
                                       int intervalsNumber, CalculatedResultListener listener) {
        String userName = l.getUserNameFromIndicator(indicatorName);
        boolean isBottomChart = userName.equals(INDICATOR_NAME_BARS_BOTTOM);

        Double pips = pipsMap.get(indicatorAlias);

        List<DataStructureInterface.TreeResponseInterval> result =
                dataStructureInterface.get(Layer1ApiBarsDemo.class, TREE_NAME, t0, intervalWidth, intervalsNumber,
                        indicatorAlias, l.getInterestingCustomEvents());

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
    public OnlineValueCalculatorAdapter createOnlineValueCalculator(String indicatorName,
                                                                    String indicatorAlias,
                                                                    long time,
                                                                    Consumer<Object> listener,
                                                                    InvalidateInterface invalidateInterface) {
        String userName = l.getUserNameFromIndicator(indicatorName);
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
                        BarEvent event = (BarEvent) aliasedEvent.event;
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

    public void putPips(String alias, double pips) {
        pipsMap.put(alias, pips);
    }

    private int getBodyWidth(long intervalWidth) {
        long bodyWidth = CANDLE_INTERVAL_NS / intervalWidth;
        bodyWidth = Math.max(bodyWidth, MIN_BODY_WIDTH);
        bodyWidth = Math.min(bodyWidth, MAX_BODY_WIDTH);
        return (int) bodyWidth;

    }

    public void setDataStructureInterface(DataStructureInterface dataStructureInterface) {
        this.dataStructureInterface = dataStructureInterface;
    }

    public BarEvent getBarEvent(DataStructureInterface.TreeResponseInterval treeResponseInterval) {
        Object result = treeResponseInterval.events.get(BarEvent.class.toString());
        if (result != null) {
            return (BarEvent) result;
        } else {
            return null;
        }
    }

    static class BarEvent implements CustomGeneratedEvent, OnlineCalculatable.DataCoordinateMarker {
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
            return "[" + time + ": " + open + "/" + low + "/" + high + "/" + close + "]";
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
        public OnlineCalculatable.Marker makeMarker(Function<Double, Integer> yDataCoordinateToPixelFunction) {

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
            return new OnlineCalculatable.Marker(close, iconOffsetX, iconOffsetY, bufferedImage);
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
}
