package velox.api.layer1.simpledemo.markers;

import velox.api.layer1.*;
import velox.api.layer1.annotations.Layer1ApiVersion;
import velox.api.layer1.annotations.Layer1ApiVersionValue;
import velox.api.layer1.annotations.Layer1Attachable;
import velox.api.layer1.annotations.Layer1StrategyName;
import velox.api.layer1.common.ListenableHelper;
import velox.api.layer1.common.Log;
import velox.api.layer1.data.InstrumentInfo;
import velox.api.layer1.layers.strategies.interfaces.InvalidateInterface;
import velox.api.layer1.messages.UserMessageLayersChainCreatedTargeted;
import velox.api.layer1.messages.indicators.IndicatorLineStyle;
import velox.api.layer1.messages.indicators.Layer1ApiDataInterfaceRequestMessage;
import velox.api.layer1.messages.indicators.Layer1ApiUserMessageModifyIndicator;
import velox.api.layer1.messages.indicators.Layer1ApiUserMessageModifyIndicator.GraphType;
import velox.api.layer1.messages.indicators.SettingsAccess;
import velox.api.layer1.settings.Layer1ConfigSettingsInterface;
import velox.gui.StrategyPanel;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Layer1Attachable
@Layer1StrategyName("Markers demo")
@Layer1ApiVersion(Layer1ApiVersionValue.VERSION2)
public class Layer1ApiMarkersDemo implements
        Layer1ApiFinishable,
        Layer1ApiAdminAdapter,
        Layer1ApiInstrumentListener,
        Layer1CustomPanelsGetter,
        Layer1ConfigSettingsInterface {

    public static final String INDICATOR_NAME_TRADE = "Trade markers";
    public static final String INDICATOR_NAME_CIRCLES = "Order markers";

    private final MarkersIndicatorColor markersIndicatorColor;

    private final MarkersOnlineCalculator markersOnlineCalculator;

    private final Layer1ApiProvider provider;

    private final Map<String, String> indicatorsFullNameToUserName = new HashMap<>();
    private final Map<String, String> indicatorsUserNameToFullName = new HashMap<>();

    private final Map<String, InvalidateInterface> invalidateInterfaceMap = new ConcurrentHashMap<>();

    public Layer1ApiMarkersDemo(Layer1ApiProvider provider) {
        this.provider = provider;

        ListenableHelper.addListeners(provider, this);

        markersIndicatorColor = new MarkersIndicatorColor(this);
        markersOnlineCalculator = new MarkersOnlineCalculator(markersIndicatorColor, this);
    }

    @Override
    public void finish() {
        synchronized (indicatorsFullNameToUserName) {
            for (String userName : indicatorsFullNameToUserName.values()) {
                provider.sendUserMessage(new Layer1ApiUserMessageModifyIndicator(Layer1ApiMarkersDemo.class, userName, false));
            }
        }
        invalidateInterfaceMap.clear();
    }

    @Override
    public void onUserMessage(Object data) {
        if (data.getClass() == UserMessageLayersChainCreatedTargeted.class) {
            UserMessageLayersChainCreatedTargeted message = (UserMessageLayersChainCreatedTargeted) data;
            if (message.targetClass == getClass()) {
                provider.sendUserMessage(new Layer1ApiDataInterfaceRequestMessage(
                        dataStructureInterface -> {
                            markersOnlineCalculator.setDataStructureInterface(dataStructureInterface);
                            invalidateInterfaceMap.values().forEach(InvalidateInterface::invalidate);
                        }));
                addIndicator(INDICATOR_NAME_TRADE);
                addIndicator(INDICATOR_NAME_CIRCLES);
            }
        }
    }

    @Override
    public void onInstrumentAdded(String alias, InstrumentInfo instrumentInfo) {
        markersOnlineCalculator.putPipsByAlias(alias, instrumentInfo.pips);
        markersIndicatorColor.reloadOrderIcon(alias);
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
    public StrategyPanel[] getCustomGuiFor(String alias, String indicatorName) {
        return markersIndicatorColor.getCustomGuiFor(alias);
    }

    @Override
    public void acceptSettingsInterface(SettingsAccess settingsAccess) {
        markersIndicatorColor.setSettingsAccess(settingsAccess);
    }

    public void putInvalidateInterface(String userName, InvalidateInterface invalidateInterface) {
        invalidateInterfaceMap.put(userName, invalidateInterface);
    }

    public InvalidateInterface getInvalidateInterface(String userName) {
        return invalidateInterfaceMap.get(userName);
    }

    public String getFullNameByIndicator(String indicatorName) {
        return indicatorsFullNameToUserName.get(indicatorName);
    }

    public void addIndicator(String userName) {
        Layer1ApiUserMessageModifyIndicator message = null;
        switch (userName) {
            case INDICATOR_NAME_TRADE:
                message = getUserMessageAdd(userName, IndicatorLineStyle.SHORT_DASHES_WIDE_LEFT_NARROW_RIGHT, true);
                break;
            case INDICATOR_NAME_CIRCLES:
                message = getUserMessageAdd(userName, IndicatorLineStyle.NONE, true);
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

    private Layer1ApiUserMessageModifyIndicator getUserMessageAdd(String userName,
                                                                  IndicatorLineStyle lineStyle, boolean isAddWidget) {
        return Layer1ApiUserMessageModifyIndicator.builder(Layer1ApiMarkersDemo.class, userName)
                .setIsAdd(true)
                .setGraphType(GraphType.PRIMARY)
                .setColorInterface(markersIndicatorColor)
                .setOnlineCalculatable(markersOnlineCalculator)
                .setIndicatorColorScheme(markersIndicatorColor.createDefaultIndicatorColorScheme())
                .setIndicatorLineStyle(lineStyle)
                .setDefaultTooltipTextColor(Color.black)
                .setDefaultTooltipBackgrondColor(Color.white)
                .setIsSupportWidget(isAddWidget)
                .setIsShowColorSettings(false)
                .build();
    }
}
