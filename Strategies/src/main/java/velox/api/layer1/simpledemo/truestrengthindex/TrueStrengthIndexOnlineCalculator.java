package velox.api.layer1.simpledemo.truestrengthindex;

import velox.api.layer1.layers.strategies.interfaces.*;
import velox.api.layer1.messages.indicators.DataStructureInterface;

import java.awt.*;
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
        Double pips = indexRepo.getPips(alias);
        List<DataStructureInterface.TreeResponseInterval> intervalResponse =
                getIntervalResponse(alias, t0, intervalWidth, intervalsNumber);

        for (int i = 1; i <= intervalsNumber; ++i) {
            PeriodEvent value = getEvent(intervalResponse.get(i));

            if (value != null) {
                Marker marker = makeMarker(value, pips, intervalWidth, alias);
                listener.provideResponse(marker);
            } else {
                listener.provideResponse(Double.NaN);
            }
        }
        listener.setCompleted();
    }

    private OnlineValueCalculatorAdapter getTradeOnlineValueCalculatorAdapter(String alias,
                                                                              Consumer<Object> listener) {
        Double pips = indexRepo.getPips(alias);
        return new OnlineValueCalculatorAdapter() {
            long intervalWidth = 30;
            @Override
            public void onIntervalWidth(long intervalWidth) {
                this.intervalWidth = intervalWidth;
            }
            @Override
            public void onUserMessage(Object data) {
                if (!(data instanceof CustomGeneratedEventAliased)) {
                    return;
                }
                CustomGeneratedEventAliased aliasedEvent = (CustomGeneratedEventAliased) data;
                if (alias.equals(aliasedEvent.alias) && aliasedEvent.event instanceof PeriodEvent) {
                    PeriodEvent event = (PeriodEvent) aliasedEvent.event ;

                    Marker marker = makeMarker(event, pips, intervalWidth, alias);
                    listener.accept(marker);
                }
            }
        };
    }

    private OnlineCalculatable.Marker makeMarker(PeriodEvent event,
                                                 double pips,
                                                 long intervalWidth,
                                                 String alias) {
        TrueStrengthIndex trueStrengthIndex = indexRepo.getTrueStrengthIndex(alias);

        event = new PeriodEvent(event);
        event.applyPips(pips);
        event.setBodyWidthPx(intervalWidth);
        event.addTsiIfAbsent(trueStrengthIndex);

        String name = getColorName(trueStrengthIndex);
        Color color = indexRepo.getSettingsFor(alias).getColor(name);

        return event.makeMarker(color);
    }

    private String getColorName(TrueStrengthIndex trueStrengthIndex) {
        return trueStrengthIndex.getRoc() ? TsiConstants.CIRCLE_INDEX.getLineName() : TsiConstants.MAIN_INDEX.getLineName();
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
