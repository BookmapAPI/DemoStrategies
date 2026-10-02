package velox.api.layer1.simpledemo.dynamichistory;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;

import velox.api.layer1.Layer1ApiFinishable;
import velox.api.layer1.Layer1ApiInstrumentAdapter;
import velox.api.layer1.Layer1ApiProvider;
import velox.api.layer1.Layer1CustomPanelsGetter;
import velox.api.layer1.annotations.Layer1ApiVersion;
import velox.api.layer1.annotations.Layer1ApiVersionValue;
import velox.api.layer1.annotations.Layer1Attachable;
import velox.api.layer1.annotations.Layer1StrategyName;
import velox.api.layer1.common.ListenableHelper;
import velox.api.layer1.common.Log;
import velox.api.layer1.data.InstrumentInfo;
import velox.api.layer1.datastructure.events.PriceSizePair;
import velox.api.layer1.datastructure.events.dynamichistory.HistoricalDepthAggregationEvent;
import velox.api.layer1.datastructure.events.dynamichistory.HistoricalLightweightMainAggregationEvent;
import velox.api.layer1.dynamichistory.DynamicHistoryAccessInterface;
import velox.api.layer1.dynamichistory.DynamicHistoryLoadingResult;
import velox.api.layer1.dynamichistory.Layer1ApiDynamicHistoryListener;
import velox.api.layer1.dynamichistory.events.DynamicHistoryEvent;
import velox.api.layer1.dynamichistory.metadata.DefaultTreeIds;
import velox.api.layer1.dynamichistory.metadata.FirstEventTime;
import velox.api.layer1.dynamichistory.metadata.MetadataType;
import velox.api.layer1.dynamichistory.updates.HeavyweightHistoricalUpdate;
import velox.api.layer1.dynamichistory.updates.HistoricalUpdate;
import velox.api.layer1.dynamichistory.updates.LightweightHistoricalUpdate;
import velox.gui.BookmapScrollPane;
import velox.gui.StrategyPanel;

import com.bookmap.api.Aggregation;
import com.bookmap.api.treestructure.DataSource;
import com.bookmap.api.treestructure.TreeId;

/**
 * Reads the zoomed-out history that Bookmap's own chart renders: the aggregated order book behind the
 * heatmap ({@link DefaultTreeIds#DEPTH}) and the aggregated trades, last trade and BBO behind the trade dots
 * ({@link DefaultTreeIds#LIGHTWEIGHT_MAIN}). Select the add-on in the strategies dialog, pick a granularity
 * and a block count, and press Load.
 *
 * <p>Implement {@link Layer1ApiDynamicHistoryListener} to receive a {@link DynamicHistoryAccessInterface}.
 * Returning an empty list from {@link #getDynamicHistoryInfos()} means "I only read Bookmap's trees, I publish
 * none of my own" - that list is for publishing history of your own.
 *
 * <p>Things worth copying from here:
 *
 * <p> The zoomout response is guaranteed to cover the requested time interval, but the actual blocks may
 * be smaller and more numerous because zoomout returns blocks of precomputed size.
 *
 * <p><b>Values are raw, not display units.</b> Prices are level indices, so multiply by
 * {@link InstrumentInfo#pips}; sizes are in size increments, so divide by {@link InstrumentInfo#sizeMultiplier}.
 * {@link #describeDepth} and {@link #describeLightweightMain} show the conversion. Public data classes should have
 * javadoc with more details about units.
 *
 * <p>{@link TreeId} and {@link DataSource} are not part of the published {@code api-core} artifact; building
 * outside the Bookmap project needs the extra {@code compileOnly} dependency described in
 * {@code Strategies/build.gradle}.
 */
@Layer1Attachable
@Layer1StrategyName("Zoom-out history demo")
@Layer1ApiVersion(Layer1ApiVersionValue.VERSION2)
public class Layer1ZoomOutHistoryDemo
        implements Layer1ApiFinishable,
                Layer1ApiInstrumentAdapter,
                Layer1ApiDynamicHistoryListener,
                Layer1CustomPanelsGetter {

    private static final String NAME = "Zoom-out history demo";

    private static final long NANOS_IN_SECOND = 1_000_000_000L;

    /** Bookmap's own trees: the heatmap order book, and the trades/last trade/BBO behind the dots. */
    private static final List<TreeId> TREES = List.of(DefaultTreeIds.DEPTH, DefaultTreeIds.LIGHTWEIGHT_MAIN);
    
    private final Executor executor = Executors.newSingleThreadExecutor();

    private final Layer1ApiProvider provider;
    private final Map<String, InstrumentInfo> instruments = new ConcurrentHashMap<>();

    private volatile DynamicHistoryAccessInterface dynamicHistoryAccess;

    /** The request currently in flight, cancelled when a new one starts or the add-on stops. */
    private volatile DynamicHistoryLoadingResult runningRequest;

    public Layer1ZoomOutHistoryDemo(Layer1ApiProvider provider) {
        this.provider = provider;
        ListenableHelper.addListeners(provider, this);
    }

    //This add-on don't have any trees
    @Override
    public List<DynamicHistoryInfo<? extends Aggregation, ? extends DynamicHistoryEvent>> getDynamicHistoryInfos() {
        return List.of();
    }

    @Override
    public void acceptDynamicHistoryAccessInterface(DynamicHistoryAccessInterface dynamicHistoryAccessInterface) {
        this.dynamicHistoryAccess = dynamicHistoryAccessInterface;
    }

    @Override
    public void onInstrumentAdded(String alias, InstrumentInfo instrumentInfo) {
        instruments.put(alias, instrumentInfo);
    }

    @Override
    public void onInstrumentRemoved(String alias) {
        instruments.remove(alias);
    }

    @Override
    public StrategyPanel[] getCustomGuiFor(String alias, String indicatorName) {
        return new StrategyPanel[] {new QueryPanel(alias)};
    }

    @Override
    public void finish() {
        cancelRunningRequest();
    }

    private void cancelRunningRequest() {
        DynamicHistoryLoadingResult previous = runningRequest;
        runningRequest = null;
        if (previous != null) {
            previous.cancel();
        }
    }

    // ---------------------------------------------------------------------------------------------------
    // Requests
    // ---------------------------------------------------------------------------------------------------

    /**
     * Asks how far back this instrument's history goes, then requests the most recent blocks from there.
     *
     * @param blockWidthNanos width of one block; the tree stores fixed resolutions, so the actual blocks are
     *     the nearest level Bookmap has and are not exactly this wide
     * @param blockCount number of blocks to request; by taking into account blockWidthNanos,
     *     you will resolve more blocks that you was asked
     */
    private void load(String alias, long blockWidthNanos, int blockCount, Consumer<String> log) {
        DynamicHistoryAccessInterface access = dynamicHistoryAccess;
        InstrumentInfo instrumentInfo = instruments.get(alias);
        if (access == null || instrumentInfo == null) {
            log.accept(access == null ? "Dynamic history access is not available yet." : "Unknown alias " + alias);
            return;
        }

        access.getMetadata(DataSource.BOOKMAP_ORIGINAL_DATA_SOURCE, instrumentInfo, MetadataType.firstEventTimeType())
                .thenAcceptAsync(firstEventTime -> {
                    if (!(firstEventTime instanceof FirstEventTime.Found(long firstEventTimeNs))) {
                        log.accept("No zoom-out history for this instrument.");
                        return;
                    }
                    log.accept("History starts at " + formatTime(firstEventTimeNs));
                    // Clamping to the start of history keeps the request inside the range that can answer it.
                    long t1 = Math.max(
                            firstEventTimeNs, provider.getCurrentTime() - blockWidthNanos * blockCount);
                    request(access, instrumentInfo, t1, blockWidthNanos, blockCount, log);
                }, executor)
                .exceptionally(e -> {
                    // The stage completes exceptionally when the server is unavailable; that is normal here.
                    Log.warn(NAME, "could not fetch the first event time for " + alias, e);
                    log.accept("Could not fetch the first event time: " + e);
                    return null;
                });
    }

    private void request(
            DynamicHistoryAccessInterface access,
            InstrumentInfo instrumentInfo,
            long t1,
            long blockWidthNanos,
            int blockCount,
            Consumer<String> log) {
        cancelRunningRequest();
        log.accept("Requesting " + blockCount + " block(s) from " + formatTime(t1));

        DynamicHistoryLoadingResult result = access.get(
                t1,
                blockWidthNanos,
                blockCount,
                DataSource.BOOKMAP_ORIGINAL_DATA_SOURCE,
                instrumentInfo,
                TREES,
                (treeId, update) -> log.accept(describeUpdate(treeId, update, instrumentInfo)));
        runningRequest = result;

        // Whatever was cached is available immediately; the rest arrives through the consumer above.
        for (TreeId treeId : TREES) {
            Optional<HistoricalUpdate<? extends DynamicHistoryEvent>> cached = result.getDataFromCache(treeId);
            cached.ifPresent(update -> log.accept("from cache, " + describeUpdate(treeId, update, instrumentInfo)));
        }
    }

    // ---------------------------------------------------------------------------------------------------
    // Decoding
    // ---------------------------------------------------------------------------------------------------

    private static String describeUpdate(TreeId treeId, HistoricalUpdate<?> update, InstrumentInfo instrumentInfo) {
        StringBuilder builder = new StringBuilder();
        builder.append(treeId.getName())
                .append(": ")
                .append(update.events.size())
                .append(" event(s), ")
                .append(describeStage(update))
                .append("\n    ")
                .append(formatTime(update.timeFrom))
                .append(" .. ")
                .append(formatTime(update.timeTo));
        if (!update.events.isEmpty()) {
            builder.append("\n    newest block: ")
                    .append(describe(update.events.get(update.events.size() - 1), instrumentInfo));
        }
        return builder.toString();
    }

    /**
     * A lightweight load arrives in one update that says outright whether it is the last; a heavyweight one
     * arrives in several, each refining the resolution, and reports how far along it is instead.
     */
    private static String describeStage(HistoricalUpdate<?> update) {
        if (update instanceof LightweightHistoricalUpdate<?> lightweight) {
            return lightweight.isFinalUpdate ? "final" : "more coming";
        } else if (update instanceof HeavyweightHistoricalUpdate<?> heavyweight) {
            var progress = heavyweight.progress;
            return "level " + progress.level() + "/" + progress.targetLevel()
                    + (progress.isLastUpdate() ? " (final)" : "");
        } else {
            return "unknown update type " + update.getClass().getSimpleName();
        }
    }

    private static String describe(DynamicHistoryEvent event, InstrumentInfo instrumentInfo) {
        if (event instanceof HistoricalDepthAggregationEvent depth) {
            return describeDepth(depth, instrumentInfo);
        } else if (event instanceof HistoricalLightweightMainAggregationEvent main) {
            return describeLightweightMain(main, instrumentInfo);
        } else {
            return "unexpected event type " + event.getClass().getSimpleName();
        }
    }

    /** Levels are indices and sizes are in size increments, so both need converting before they mean anything. */
    private static String describeDepth(HistoricalDepthAggregationEvent depth, InstrumentInfo instrumentInfo) {
        return depth.bids.size() + " bid level(s), " + depth.asks.size() + " ask level(s)"
                + describeBestLevel("bid", depth.bids, Comparator.comparingInt(PriceSizePair::price), instrumentInfo)
                + describeBestLevel(
                        "ask",
                        depth.asks,
                        Comparator.comparingInt(PriceSizePair::price).reversed(),
                        instrumentInfo);
    }

    /**
     * A depth event carries the levels that changed in the block, unordered, so the extreme level here is the
     * best <i>touched</i> one rather than necessarily the book's best.
     */
    private static String describeBestLevel(
            String side,
            List<PriceSizePair> levels,
            Comparator<PriceSizePair> comparator,
            InstrumentInfo instrumentInfo) {
        return levels.stream()
                .max(comparator)
                .map(best -> "\n      best " + side + " " + best.price() * instrumentInfo.pips + " x "
                        + best.size() / instrumentInfo.sizeMultiplier)
                .orElse("");
    }

    private static String describeLightweightMain(
            HistoricalLightweightMainAggregationEvent main, InstrumentInfo instrumentInfo) {
        StringBuilder builder = new StringBuilder();

        var trades = main.trades;
        long totalSize = trades.getTotalSize();
        builder.append("traded ")
                .append(totalSize / instrumentInfo.sizeMultiplier)
                .append(" (bid aggressor ")
                .append(trades.bidSize() / instrumentInfo.sizeMultiplier)
                .append(", ask aggressor ")
                .append(trades.askSize() / instrumentInfo.sizeMultiplier)
                .append(')');
        if (totalSize > 0) {
            // getAveragePrice divides by the total size, so it is only meaningful when something traded.
            builder.append(", vwap ").append(trades.getAveragePrice() * instrumentInfo.pips);
        }

        var lastTrade = main.lastTrade;
        if (lastTrade.lastPriceOrNull() != null) {
            builder.append("\n      last trade ")
                    .append(lastTrade.lastPriceOrNull() * instrumentInfo.pips)
                    .append(" x ")
                    .append(lastTrade.lastTradeSizeOrNull() / instrumentInfo.sizeMultiplier)
                    .append(lastTrade.isBidAggressor() ? " (bid aggressor)" : " (ask aggressor)");
        }

        var bbo = main.bbo;
        builder.append("\n      bbo ")
                .append(bbo.bestBidOrNull() == null ? "-" : bbo.bestBidOrNull() * instrumentInfo.pips)
                .append(" / ")
                .append(bbo.bestAskOrNull() == null ? "-" : bbo.bestAskOrNull() * instrumentInfo.pips);

        return builder.toString();
    }

    private static String formatTime(long timeNanos) {
        return ZonedDateTime.ofInstant(
                        Instant.ofEpochSecond(
                                timeNanos / NANOS_IN_SECOND, Math.floorMod(timeNanos, NANOS_IN_SECOND)),
                        ZoneOffset.UTC)
                .toString();
    }

    // ---------------------------------------------------------------------------------------------------
    // GUI
    // ---------------------------------------------------------------------------------------------------

    /**
     * The add-ons dialog is a fixed-width single column with no horizontal scrolling, so controls go one per
     * row and the output wraps instead of asking for width. Height, unlike width, is free.
     */
    private class QueryPanel extends StrategyPanel {

        private static final long serialVersionUID = 1L;

        private final JTextArea output = new JTextArea();

        QueryPanel(String alias) {
            super("Bookmap zoom-out history", new GridBagLayout());

            JSpinner granularity = new JSpinner(new SpinnerNumberModel(1, 1, 86_400, 1));
            JSpinner blocks = new JSpinner(new SpinnerNumberModel(600, 1, 100_000, 100));
            JButton loadButton = new JButton("Load recent");

            GridBagConstraints constraints = new GridBagConstraints();
            constraints.insets = new Insets(2, 2, 2, 2);
            constraints.anchor = GridBagConstraints.WEST;

            addRow(constraints, 0, new JLabel("Granularity, s:"), granularity);
            addRow(constraints, 1, new JLabel("Blocks per request:"), blocks);

            constraints.gridx = 0;
            constraints.gridy = 2;
            constraints.gridwidth = 2;
            add(loadButton, constraints);

            output.setEditable(false);
            output.setLineWrap(true);
            output.setWrapStyleWord(true);
            BookmapScrollPane scrollPane = new BookmapScrollPane(output);
            // Sizing the scroll pane rather than the panel: the dialog stretches the panel to its own width
            // anyway, so a width set there would only mislead whoever copies this demo.
            scrollPane.setPreferredSize(new Dimension(440, 300));

            constraints.gridy = 3;
            constraints.fill = GridBagConstraints.BOTH;
            constraints.weightx = 1;
            add(scrollPane, constraints);

            loadButton.addActionListener(event -> {
                output.setText("");
                // get() returns promptly and delivers asynchronously, so calling it from the EDT is fine.
                load(
                        alias,
                        ((Number) granularity.getValue()).longValue() * NANOS_IN_SECOND,
                        ((Number) blocks.getValue()).intValue(),
                        this::show);
            });
        }

        private void addRow(GridBagConstraints constraints, int gridy, JLabel label, JSpinner spinner) {
            constraints.gridy = gridy;
            constraints.gridx = 0;
            add(label, constraints);
            constraints.gridx = 1;
            add(spinner, constraints);
        }

        /** Reports arrive on Bookmap threads, the text area may only be touched on the EDT. */
        private void show(String line) {
            SwingUtilities.invokeLater(() -> {
                output.append(line + "\n\n");
            });
        }
    }
}
