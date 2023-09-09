package velox.api.layer1.simpledemo.truestrengthindex;

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

@Layer1Attachable
@Layer1StrategyName("True Strength Index")
@Layer1ApiVersion(Layer1ApiVersionValue.VERSION2)
public class Layer1ApiTrueStrengthIndex implements
        Layer1ApiFinishable,
        Layer1ApiAdminAdapter,
        Layer1CustomPanelsGetter,
        Layer1ApiInstrumentListener,
        Layer1ConfigSettingsInterface {

    private final TrueStrengthIndexRepo indexRepo = new TrueStrengthIndexRepo();
    private final TrueStrengthIndexGraphics indexGraphics;

    private final TrueStrengthIndexOnlineCalculator indexOnlineCalculator;

    private final Layer1ApiProvider provider;

    public Layer1ApiTrueStrengthIndex(Layer1ApiProvider provider) {
        this.provider = provider;

        ListenableHelper.addListeners(provider, this);

        indexGraphics = new TrueStrengthIndexGraphics(indexRepo);
        indexOnlineCalculator = new TrueStrengthIndexOnlineCalculator(indexRepo);
    }

    @Override
    public void finish() {
        indexRepo.executeForEachValueOfIndicatorsFullNameToUserName(this::getUserMessageRemove);
        indexRepo.clearInvalidateInterfaceMap();
    }

    @Override
    public void onUserMessage(Object data) {
        if (data.getClass() == UserMessageLayersChainCreatedTargeted.class) {
            UserMessageLayersChainCreatedTargeted message = (UserMessageLayersChainCreatedTargeted) data;
            if (message.targetClass == getClass()) {
                provider.sendUserMessage(new Layer1ApiDataInterfaceRequestMessage(
                        dataStructureInterface -> {
                            indexOnlineCalculator.setDataStructureInterface(dataStructureInterface);
                            indexRepo.executeForEachValueOfInvalidateInterfaceMap(InvalidateInterface::invalidate);
                        }));
                addIndicator(TrueStrengthIndexDemoConstants.MAIN_INDEX.getIndicatorName());
            }
        }
    }

    @Override
    public StrategyPanel[] getCustomGuiFor(String alias, String indicatorName) {
        return indexGraphics.getCustomGuiFor(alias);
    }

    @Override
    public void acceptSettingsInterface(SettingsAccess settingsAccess) {
        indexRepo.setSettingsAccess(settingsAccess);
    }

    @Override
    public void onInstrumentAdded(String alias, InstrumentInfo instrumentInfo) {
        Integer shortPeriod = TrueStrengthIndexDemoConstants.MAIN_INDEX.getParams().getLeft();
        Integer longPeriod = TrueStrengthIndexDemoConstants.MAIN_INDEX.getParams().getRight();
        indexRepo.putTrueStrengthIndex(alias, new TrueStrengthIndex(shortPeriod, longPeriod));
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

    private void addIndicator(String userName) {
        Layer1ApiUserMessageModifyIndicator message = null;

        TrueStrengthIndexDemoConstants indicator = TrueStrengthIndexDemoConstants.fromIndicatorName(userName);
        if (indicator == TrueStrengthIndexDemoConstants.MAIN_INDEX) {
            message = getUserMessageAdd(userName);
        } else {
            Log.warn("Layer1ApiTrueStrengthIndex: Unknown name for true strength index indicator: " + userName);
        }

        if (message != null) {
            indexRepo.putIndicatorNameByFullName(message.fullName, message.userName);
            provider.sendUserMessage(message);
        }
    }

    private Layer1ApiUserMessageModifyIndicator getUserMessageRemove(String userName) {
        return new Layer1ApiUserMessageModifyIndicator(Layer1ApiTrueStrengthIndex.class, userName, false);
    }

    private Layer1ApiUserMessageModifyIndicator getUserMessageAdd(String userName) {
        return Layer1ApiUserMessageModifyIndicator.builder(Layer1ApiTrueStrengthIndex.class, userName)
                .setIsAdd(true)
                .setGraphType(GraphType.BOTTOM)
                .setColorInterface(indexGraphics)
                .setOnlineCalculatable(indexOnlineCalculator)
                .setIndicatorColorScheme(indexGraphics.createDefaultIndicatorColorScheme())
                .setIndicatorLineStyle(IndicatorLineStyle.DEFAULT)
                .setDefaultTooltipTextColor(Color.black)
                .setDefaultTooltipBackgrondColor(Color.white)
                .setIsSupportWidget(true)
                .setIsShowColorSettings(false)
                .build();
    }
}
