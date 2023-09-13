package velox.api.layer1.simpledemo.truestrengthindex;

import velox.api.layer1.*;
import velox.api.layer1.annotations.Layer1ApiVersion;
import velox.api.layer1.annotations.Layer1ApiVersionValue;
import velox.api.layer1.annotations.Layer1Attachable;
import velox.api.layer1.annotations.Layer1StrategyName;
import velox.api.layer1.common.ListenableHelper;
import velox.api.layer1.data.InstrumentInfo;
import velox.api.layer1.layers.strategies.interfaces.InvalidateInterface;
import velox.api.layer1.messages.GeneratedEventInfo;
import velox.api.layer1.messages.Layer1ApiUserMessageAddStrategyUpdateGenerator;
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

        provider.sendUserMessage(getGeneratorMessage(false));
    }

    @Override
    public void onUserMessage(Object data) {
        if (data.getClass() == UserMessageLayersChainCreatedTargeted.class) {
            UserMessageLayersChainCreatedTargeted message = (UserMessageLayersChainCreatedTargeted) data;
            if (message.targetClass == getClass()) {
                provider.sendUserMessage(getInitializationMassage());
                provider.sendUserMessage(getAddIndicatorMassage());
                provider.sendUserMessage(getGeneratorMessage(true));
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
        indexRepo.putPips(alias, instrumentInfo.pips);

        Integer shortPeriod = TsiConstants.TSI_PARAMS.getLeft();
        Integer longPeriod = TsiConstants.TSI_PARAMS.getRight();
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

    private Layer1ApiDataInterfaceRequestMessage getInitializationMassage() {
        return new Layer1ApiDataInterfaceRequestMessage(dataStructureInterface -> {
            indexOnlineCalculator.setDataStructureInterface(dataStructureInterface);
            indexRepo.executeForEachValueOfInvalidateInterfaceMap(InvalidateInterface::invalidate);
        });
    }

    private Layer1ApiUserMessageModifyIndicator getAddIndicatorMassage() {
        Layer1ApiUserMessageModifyIndicator message = createAddIndicatorMassage(TsiConstants.INDICATOR_NAME);

        indexRepo.putIndicatorNameByFullName(message.fullName, message.userName);

        return message;
    }

    private Layer1ApiUserMessageAddStrategyUpdateGenerator getGeneratorMessage(boolean isAdd) {
        return new Layer1ApiUserMessageAddStrategyUpdateGenerator(
                Layer1ApiTrueStrengthIndex.class,
                TsiConstants.SHORT_NAME,
                isAdd,
                true,
                true,
                new PeriodStrategyUpdateGenerator(),
                new GeneratedEventInfo[]{
                        new GeneratedEventInfo(PeriodEvent.class, PeriodEvent.class, PeriodEvent.AGGREGATOR)});
    }

    private Layer1ApiUserMessageModifyIndicator getUserMessageRemove(String userName) {
        return new Layer1ApiUserMessageModifyIndicator(Layer1ApiTrueStrengthIndex.class, userName, false);
    }

    private Layer1ApiUserMessageModifyIndicator createAddIndicatorMassage(String userName) {
        return Layer1ApiUserMessageModifyIndicator.builder(Layer1ApiTrueStrengthIndex.class, userName)
                .setIsAdd(true)
                .setGraphType(GraphType.BOTTOM)
                .setColorInterface(indexGraphics)
                .setOnlineCalculatable(indexOnlineCalculator)
                .setIndicatorColorScheme(indexGraphics.createDefaultIndicatorColorScheme())
                .setIndicatorLineStyle(IndicatorLineStyle.NONE)
                .setDefaultTooltipTextColor(Color.black)
                .setDefaultTooltipBackgrondColor(Color.white)
                .setIsSupportWidget(true)
                .setIsShowColorSettings(false)
                .build();
    }
}
