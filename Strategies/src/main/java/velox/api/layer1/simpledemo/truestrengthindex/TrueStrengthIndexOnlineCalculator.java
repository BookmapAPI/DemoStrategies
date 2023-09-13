package velox.api.layer1.simpledemo.truestrengthindex;

import velox.api.layer1.layers.strategies.interfaces.*;
import velox.api.layer1.messages.indicators.DataStructureInterface;

import java.util.List;
import java.util.function.Consumer;

public class TrueStrengthIndexOnlineCalculator implements OnlineCalculatable {
    private final TrueStrengthIndexRepo indexRepo;
    private DataStructureInterface dataStructureInterface;

    public TrueStrengthIndexOnlineCalculator(TrueStrengthIndexRepo trueStrengthIndexRepo) {
        this.indexRepo = trueStrengthIndexRepo;
    }

    @Override
    public void calculateValuesInRange(String indicatorName,
                                       String alias,
                                       long t0,
                                       long intervalWidth,
                                       int intervalsNumber,
                                       CalculatedResultListener listener) {
        if (dataStructureInterface == null) {
            listener.setCompleted();
            return;
        }

        String userName = indexRepo.getIndicatorNameByFullName(indicatorName);
        TsiConstants indicator = TsiConstants.fromIndicatorName(userName);
        if (indicator != null) {
            calculateMainIndexValuesInRange(alias, t0, intervalWidth, intervalsNumber, listener);
        } else {
            throw new IllegalArgumentException("Unknown indicator name " + indicatorName);
        }
    }

    @Override
    public OnlineValueCalculatorAdapter createOnlineValueCalculator(String indicatorName,
                                                                    String indicatorAlias,
                                                                    long time,
                                                                    Consumer<Object> listener,
                                                                    InvalidateInterface invalidateInterface) {
        String userName = indexRepo.getIndicatorNameByFullName(indicatorName);
        indexRepo.putInvalidateInterface(userName, invalidateInterface);

        if (dataStructureInterface == null) {
            return new OnlineValueCalculatorAdapter() {
            };
        }

        TsiConstants indicator = TsiConstants.fromIndicatorName(userName);
        if (indicator != null) {
            return getTradeOnlineValueCalculatorAdapter(indicatorAlias, listener);
        } else {
            throw new IllegalArgumentException("Unknown indicator name " + indicatorName);
        }
    }

    protected void setDataStructureInterface(DataStructureInterface dataStructureInterface) {
        this.dataStructureInterface = dataStructureInterface;
    }

    private void calculateMainIndexValuesInRange(String alias,
                                                 long t0,
                                                 long intervalWidth,
                                                 int intervalsNumber,
                                                 CalculatedResultListener listener) {
        TrueStrengthIndex trueStrengthIndex = indexRepo.getTrueStrengthIndex(alias);
        Double pips = indexRepo.getPips(alias);
        List<DataStructureInterface.TreeResponseInterval> intervalResponse =
                getIntervalResponse(alias, t0, intervalWidth, intervalsNumber);

        for (int i = 1; i <= intervalsNumber; ++i) {
            PeriodEvent value = getEvent(intervalResponse.get(i));

            if (value != null) {
                value = new PeriodEvent(value);
                value.applyPips(pips);

                value.addTsiIfAbsent(trueStrengthIndex);
                listener.provideResponse(value.getTsi());
            } else {
                listener.provideResponse(Double.NaN);
            }
        }
        listener.setCompleted();
    }

    private OnlineValueCalculatorAdapter getTradeOnlineValueCalculatorAdapter(String alias,
                                                                              Consumer<Object> listener) {
        Double pips = indexRepo.getPips(alias);
        TrueStrengthIndex trueStrengthIndex = indexRepo.getTrueStrengthIndex(alias);
        return new OnlineValueCalculatorAdapter() {
            @Override
            public void onUserMessage(Object data) {
                if (!(data instanceof CustomGeneratedEventAliased)) {
                    return;
                }
                CustomGeneratedEventAliased aliasedEvent = (CustomGeneratedEventAliased) data;
                if (alias.equals(aliasedEvent.alias) && aliasedEvent.event instanceof PeriodEvent) {
                    PeriodEvent event = (PeriodEvent) aliasedEvent.event;

                    event = new PeriodEvent(event);
                    event.applyPips(pips);
                    event.addTsiIfAbsent(trueStrengthIndex);
                    listener.accept(event.getTsi());
                }
            }
        };
    }

    private List<DataStructureInterface.TreeResponseInterval> getIntervalResponse(String alias,
                                                                                  long t0,
                                                                                  long intervalWidth,
                                                                                  int intervalsNumber) {
        return dataStructureInterface.get(Layer1ApiTrueStrengthIndex.class,
                TsiConstants.SHORT_NAME,
                t0,
                intervalWidth,
                intervalsNumber,
                alias,
                new Class<?>[]{PeriodEvent.class});
    }

    private PeriodEvent getEvent(DataStructureInterface.TreeResponseInterval treeResponseInterval) {
        Object result = treeResponseInterval.events.get(PeriodEvent.class.toString());
        if (result != null) {
            return (PeriodEvent) result;
        } else {
            return null;
        }
    }
}
