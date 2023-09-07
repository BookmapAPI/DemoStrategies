package velox.api.layer1.simpledemo.markers.bars;

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
import velox.api.layer1.data.InstrumentInfo;
import velox.api.layer1.layers.strategies.interfaces.CustomEventAggregatble;
import velox.api.layer1.layers.strategies.interfaces.CustomGeneratedEvent;
import velox.api.layer1.messages.GeneratedEventInfo;
import velox.api.layer1.messages.Layer1ApiUserMessageAddStrategyUpdateGenerator;
import velox.api.layer1.messages.UserMessageLayersChainCreatedTargeted;
import velox.api.layer1.messages.indicators.IndicatorColorScheme;
import velox.api.layer1.messages.indicators.IndicatorLineStyle;
import velox.api.layer1.messages.indicators.Layer1ApiDataInterfaceRequestMessage;
import velox.api.layer1.messages.indicators.Layer1ApiUserMessageModifyIndicator;
import velox.api.layer1.messages.indicators.Layer1ApiUserMessageModifyIndicator.GraphType;
import velox.api.layer1.simpledemo.markers.bars.BarsOnlineCalculator.BarEvent;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Layer1Attachable
@Layer1StrategyName("Bars demo")
@Layer1ApiVersion(Layer1ApiVersionValue.VERSION2)
public class Layer1ApiBarsDemo implements
    Layer1ApiFinishable,
    Layer1ApiAdminAdapter,
    Layer1ApiInstrumentListener{

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
    
    private Layer1ApiProvider provider;
    
    private Map<String, String> indicatorsFullNameToUserName = new HashMap<>();
    private Map<String, String> indicatorsUserNameToFullName = new HashMap<>();
    
    private BufferedImage tradeIcon = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);

    private BarsOnlineCalculator barsOnlineCalculator = new BarsOnlineCalculator(this);

    public Layer1ApiBarsDemo(Layer1ApiProvider provider) {
        this.provider = provider;
        
        ListenableHelper.addListeners(provider, this);
        
        // Prepare trade marker
        Graphics graphics = tradeIcon.getGraphics();
        graphics.setColor(Color.BLUE);
        graphics.drawLine(0, 0, 15, 15);
        graphics.drawLine(15, 0, 0, 15);
    }

    public String getUserNameFromIndicator(String indicatorName) {
        return indicatorsFullNameToUserName.get(indicatorName);
    }

    public Class<?>[] getInterestingCustomEvents () {
        return INTERESTING_CUSTOM_EVENTS;
    }
    
    @Override
    public void finish() {
        synchronized (indicatorsFullNameToUserName) {
            for (String userName : indicatorsFullNameToUserName.values()) {
                provider.sendUserMessage(new Layer1ApiUserMessageModifyIndicator(Layer1ApiBarsDemo.class, userName, false));
            }
        }
        
        provider.sendUserMessage(getGeneratorMessage(false));
    }
    
    private Layer1ApiUserMessageModifyIndicator getUserMessageAdd(String userName, GraphType graphType) {
        return Layer1ApiUserMessageModifyIndicator.builder(Layer1ApiBarsDemo.class, userName)
                .setIsAdd(true)
                .setGraphType(graphType)
                .setOnlineCalculatable(barsOnlineCalculator)
                .setIndicatorColorScheme(createDefaultIndicatorColorScheme())
                .setIndicatorLineStyle(IndicatorLineStyle.NONE)
                .build();
    }
    
    @Override
    public void onUserMessage(Object data) {
        if (data.getClass() == UserMessageLayersChainCreatedTargeted.class) {
            UserMessageLayersChainCreatedTargeted message = (UserMessageLayersChainCreatedTargeted) data;
            if (message.targetClass == getClass()) {
                provider.sendUserMessage(new Layer1ApiDataInterfaceRequestMessage(
                        barsOnlineCalculator::setDataStructureInterface));
                addIndicator(INDICATOR_NAME_BARS_MAIN);
                addIndicator(INDICATOR_NAME_BARS_BOTTOM);
                provider.sendUserMessage(getGeneratorMessage(true));
            }
        }
    }
    
    @Override
    public void onInstrumentAdded(String alias, InstrumentInfo instrumentInfo) {
        barsOnlineCalculator.putPips(alias, instrumentInfo.pips);
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
        return new Layer1ApiUserMessageAddStrategyUpdateGenerator(Layer1ApiBarsDemo.class,
                TREE_NAME,
                isAdd,
                true,
                new BarsStrategyUpdateGenerator(),
                new GeneratedEventInfo[] {new GeneratedEventInfo(BarEvent.class, BarEvent.class, BAR_EVENTS_AGGREGATOR)});
    }

    private IndicatorColorScheme createDefaultIndicatorColorScheme() {
        return new IndicatorColorScheme() {
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
        };
    }
}
