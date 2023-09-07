package velox.api.layer1.simpledemo.truestrengthindex;

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
import velox.api.layer1.messages.UserMessageLayersChainCreatedTargeted;
import velox.api.layer1.messages.indicators.IndicatorLineStyle;
import velox.api.layer1.messages.indicators.Layer1ApiDataInterfaceRequestMessage;
import velox.api.layer1.messages.indicators.Layer1ApiUserMessageModifyIndicator;
import velox.api.layer1.messages.indicators.SettingsAccess;
import velox.api.layer1.settings.Layer1ConfigSettingsInterface;
import velox.api.layer1.simpledemo.averagepositionprice.Layer1ApiAveragePositionPriceDemo;
import velox.gui.StrategyPanel;

/**
 * An example of usage of custom events
 * This example show meaningless custom events, just to demonstrate how this works.
 * <p>
 * In this example, we will draw line, that is a modified last trade price.
 * Value at i-th trade is calculated as average of value at (i-1)th trade and price of i-tr trade
 * We can't easily calculate this value fast using standard trade events, so we will need to
 * create our own event generator, and then use our generated events to quickly calculate values on screen.
 */
@Layer1Attachable
@Layer1StrategyName("True Strength Index")
@Layer1ApiVersion(Layer1ApiVersionValue.VERSION2)
public class Layer1TrueStrengthIndex implements Layer1ApiFinishable,
        Layer1ApiAdminAdapter,
        Layer1CustomPanelsGetter,
        Layer1ConfigSettingsInterface {
    private final TrueStrengthIndexOnlineCalculator trueStrengthIndexOnlineCalculator;
    private final TrueStrengthIndexColorIndicator trueStrengthIndexColorIndicator;
    private final TrueStrengthIndexRepo trueStrengthIndexRepo;
    private final Layer1ApiProvider provider;

    public Layer1TrueStrengthIndex(Layer1ApiProvider provider) {
        this.trueStrengthIndexRepo = new TrueStrengthIndexRepo();
        this.trueStrengthIndexOnlineCalculator = new TrueStrengthIndexOnlineCalculator(trueStrengthIndexRepo);
        this.trueStrengthIndexColorIndicator = new TrueStrengthIndexColorIndicator(trueStrengthIndexRepo);
        this.provider = provider;

        ListenableHelper.addListeners(provider, this);
    }

    @Override
    public StrategyPanel[] getCustomGuiFor(String alias, String indicatorName) {
        return trueStrengthIndexColorIndicator.getCustomGuiFor(alias);
    }

    @Override
    public void onUserMessage(Object data) {
        if (data.getClass() == UserMessageLayersChainCreatedTargeted.class) {
            UserMessageLayersChainCreatedTargeted message = (UserMessageLayersChainCreatedTargeted) data;
            if (message.targetClass == getClass()) {
                provider.sendUserMessage(new Layer1ApiDataInterfaceRequestMessage(
                        trueStrengthIndexOnlineCalculator::setDataStructureInterface));
                addIndicator(TrueStrengthIndexConstants.MAIN_INDEX.getIndicatorName());
            }
        }
    }

    @Override
    public void finish() {
        provider.sendUserMessage(new Layer1ApiUserMessageModifyIndicator(
                Layer1ApiAveragePositionPriceDemo.class,
                TrueStrengthIndexConstants.MAIN_INDEX.getIndicatorName(),
                false));
    }

    @Override
    public void acceptSettingsInterface(SettingsAccess settingsAccess) {
        this.trueStrengthIndexColorIndicator.setSettingsAccess(settingsAccess);
    }

    public void addIndicator(String indicatorName) {
        Layer1ApiUserMessageModifyIndicator message = null;
        TrueStrengthIndexConstants trueStrengthIndexConstant =
                TrueStrengthIndexConstants.fromIndicatorName(indicatorName);

        if (trueStrengthIndexConstant == TrueStrengthIndexConstants.MAIN_INDEX) {
            message = getIndicatorMessage(indicatorName);
        } else {
            Log.warn("Unknwon name for marker indicator: " + indicatorName);
        }

        if (message != null) {
            synchronized (trueStrengthIndexRepo) {
                trueStrengthIndexRepo.putIndicatorShortNameByFullName(message.fullName, message.userName);
            }
            provider.sendUserMessage(message);
        }
    }

    private Layer1ApiUserMessageModifyIndicator getIndicatorMessage(String indicatorName) {
        return Layer1ApiUserMessageModifyIndicator.builder(Layer1TrueStrengthIndex.class, indicatorName)
                .setIsAdd(true)
                .setGraphType(Layer1ApiUserMessageModifyIndicator.GraphType.BOTTOM)
                .setOnlineCalculatable(trueStrengthIndexOnlineCalculator)
                .setColorInterface(trueStrengthIndexColorIndicator)
                .setIndicatorColorScheme(trueStrengthIndexColorIndicator.createIndicatorColorScheme())
                .setIndicatorLineStyle(IndicatorLineStyle.DEFAULT)
                .build();
    }
}
