package velox.api.layer1.simpledemo.markers.markers2;

import velox.api.layer1.Layer1ApiAdminAdapter;
import velox.api.layer1.Layer1ApiFinishable;
import velox.api.layer1.Layer1ApiProvider;
import velox.api.layer1.Layer1CustomPanelsGetter;
import velox.api.layer1.annotations.Layer1ApiVersion;
import velox.api.layer1.annotations.Layer1ApiVersionValue;
import velox.api.layer1.annotations.Layer1Attachable;
import velox.api.layer1.annotations.Layer1StrategyName;
import velox.api.layer1.common.ListenableHelper;
import velox.api.layer1.common.Log;
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

@Layer1Attachable
@Layer1StrategyName("Markers demo 2")
@Layer1ApiVersion(Layer1ApiVersionValue.VERSION2)
public class Layer1ApiMarkersDemo2 implements
        Layer1ApiFinishable,
        Layer1ApiAdminAdapter,
        Layer1CustomPanelsGetter,
        Layer1ConfigSettingsInterface {

    private final MarkersRepo markersRepo = new MarkersRepo();
    private final MarkersIndicatorColor markersIndicatorColor;

    private final MarkersOnlineCalculator markersOnlineCalculator;

    private final Layer1ApiProvider provider;

    public Layer1ApiMarkersDemo2(Layer1ApiProvider provider) {
        this.provider = provider;

        ListenableHelper.addListeners(provider, this);

        markersIndicatorColor = new MarkersIndicatorColor(markersRepo);
        markersOnlineCalculator = new MarkersOnlineCalculator(markersRepo, markersIndicatorColor);
    }

    @Override
    public void finish() {
        markersRepo.executeForEachValueOfIndicatorsFullNameToUserName(this::getUserMessageRemove);
        markersRepo.clearInvalidateInterfaceMap();
    }

    @Override
    public void onUserMessage(Object data) {
        if (data.getClass() == UserMessageLayersChainCreatedTargeted.class) {
            UserMessageLayersChainCreatedTargeted message = (UserMessageLayersChainCreatedTargeted) data;
            if (message.targetClass == getClass()) {
                provider.sendUserMessage(new Layer1ApiDataInterfaceRequestMessage(
                        dataStructureInterface -> {
                            markersOnlineCalculator.setDataStructureInterface(dataStructureInterface);
                            markersRepo.executeForEachValueOfInvalidateInterfaceMap(InvalidateInterface::invalidate);
                        }));
                addIndicator(MarkersDemoConstants.MAIN_INDEX.getIndicatorName());
            }
        }
    }

    @Override
    public StrategyPanel[] getCustomGuiFor(String alias, String indicatorName) {
        return markersIndicatorColor.getCustomGuiFor(alias);
    }

    @Override
    public void acceptSettingsInterface(SettingsAccess settingsAccess) {
        markersIndicatorColor.setSettingsAccess(settingsAccess);
    }

    private void addIndicator(String userName) {
        Layer1ApiUserMessageModifyIndicator message = null;

        MarkersDemoConstants indicator = MarkersDemoConstants.fromIndicatorName(userName);
        if (indicator == MarkersDemoConstants.MAIN_INDEX) {
            message = getUserMessageAdd(userName, IndicatorLineStyle.DEFAULT, true);
        } else {
            Log.warn("Unknwon name for marker indicator: " + userName);
        }

        if (message != null) {
            markersRepo.putIndicatorNameByFullName(message.fullName, message.userName);
            provider.sendUserMessage(message);
        }
    }

    private Layer1ApiUserMessageModifyIndicator getUserMessageRemove(String userName) {
        return new Layer1ApiUserMessageModifyIndicator(Layer1ApiMarkersDemo2.class, userName, false);
    }

    private Layer1ApiUserMessageModifyIndicator getUserMessageAdd(String userName,
                                                                  IndicatorLineStyle lineStyle,
                                                                  boolean isAddWidget) {
        return Layer1ApiUserMessageModifyIndicator.builder(Layer1ApiMarkersDemo2.class, userName)
                .setIsAdd(true)
                .setGraphType(GraphType.BOTTOM)
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
