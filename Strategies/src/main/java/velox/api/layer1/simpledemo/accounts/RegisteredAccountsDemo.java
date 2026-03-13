package velox.api.layer1.simpledemo.accounts;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import velox.api.layer1.Layer1ApiAdminAdapter;
import velox.api.layer1.Layer1ApiFinishable;
import velox.api.layer1.Layer1ApiInstrumentListener;
import velox.api.layer1.Layer1ApiProvider;
import velox.api.layer1.Layer1CustomPanelsGetter;
import velox.api.layer1.annotations.Layer1ApiVersion;
import velox.api.layer1.annotations.Layer1ApiVersionValue;
import velox.api.layer1.annotations.Layer1Attachable;
import velox.api.layer1.annotations.Layer1StrategyName;
import velox.api.layer1.annotations.Layer1TradingStrategy;
import velox.api.layer1.common.ListenableHelper;
import velox.api.layer1.common.helper.AccountListManager;
import velox.api.layer1.common.helper.ProviderTypeHelper;
import velox.api.layer1.data.InstrumentInfo;
import velox.api.layer1.datastructure.events.TradingAccountInfoAddEvent;
import velox.api.layer1.datastructure.events.TradingAccountInfoAggregationEvent;
import velox.api.layer1.layers.strategies.interfaces.CalculatedResultListener;
import velox.api.layer1.layers.strategies.interfaces.CustomGeneratedEvent;
import velox.api.layer1.layers.strategies.interfaces.CustomGeneratedEventAliased;
import velox.api.layer1.layers.strategies.interfaces.InvalidateInterface;
import velox.api.layer1.layers.strategies.interfaces.OnlineCalculatable;
import velox.api.layer1.layers.strategies.interfaces.OnlineValueCalculatorAdapter;
import velox.api.layer1.messages.GeneratedEventInfo;
import velox.api.layer1.messages.Layer1ApiUserMessageAddStrategyUpdateGenerator;
import velox.api.layer1.messages.TradingAccountsInfoMessage;
import velox.api.layer1.messages.UserMessageLayersChainCreatedTargeted;
import velox.api.layer1.messages.indicators.AliasFilter;
import velox.api.layer1.messages.indicators.DataStructureInterface;
import velox.api.layer1.messages.indicators.IndicatorColorScheme;
import velox.api.layer1.messages.indicators.Layer1ApiDataInterfaceRequestMessage;
import velox.api.layer1.messages.indicators.Layer1ApiUserMessageModifyIndicator;
import velox.api.layer1.messages.indicators.Layer1ApiUserMessageModifyIndicator.GraphType;
import velox.api.layer1.messages.indicators.StrategyUpdateGenerator;
import velox.api.layer1.simpledemo.multipleaccount.MultipleAccountSupportDemoBase;
import velox.gui.StrategyPanel;

import javax.swing.SwingUtilities;

/**
 * A strategy that shows all known trading accounts and whether they are available.
 * <p>
 * For each subscribed instrument (alias) a separate "Accounts: {alias}" indicator is created
 * on the BOTTOM chart showing the number of accounts available for that instrument.
 * Additionally, a general "Available Accounts (All)" bottom-panel indicator shows
 * the total number of registered accounts  with alias null of the provider of the alias.
 * </p>
 * <p>
 * In the settings panel, a table of all ever registered accounts is displayed with their IDs, sources (alias or provider)
 * and current states (added or removed).
 * </p>
 */
@Layer1Attachable
@Layer1StrategyName(RegisteredAccountsDemo.NAME)
@Layer1ApiVersion(Layer1ApiVersionValue.VERSION2)
@Layer1TradingStrategy
public class RegisteredAccountsDemo implements
        Layer1ApiFinishable,
        Layer1ApiAdminAdapter,
        Layer1ApiInstrumentListener,
        OnlineCalculatable,
        Layer1CustomPanelsGetter {

    /**
     * @param accountBelongsTo alias or provider programmatic name if alias is not available
     */
    private record AccountInfoMessageKey(String accountId, String accountBelongsTo) {}

    final static String NAME = "Registered Accounts Demo";
    private static final String PER_ALIAS_COLOR = "Per-alias accounts count";
    private static final Color  PER_ALIAS_COLOR_DEFAULT = Color.CYAN;

    private static final String GENERAL_COLOR = "Total accounts count";
    private static final Color  GENERAL_COLOR_DEFAULT = Color.ORANGE;

    private static final String GENERAL_INDICATOR_NAME = "Available Accounts (All)";

    private final Layer1ApiProvider provider;
    private DataStructureInterface dataStructureInterface;

    private final AccountListManager accountListManager = new AccountListManager();

    /** fullName → userName for all registered indicators */
    private final Map<String, String> indicatorsFullNameToUserName = new HashMap<>();

    /** alias → per-alias indicator userName */
    private final Map<String, String> aliasIndicatorNames = new ConcurrentHashMap<>();

    private final Map<String, String> aliasesToProgrammaticName = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> programmaticNameToAvailableAccountsSet = new ConcurrentHashMap<>();

    private final Map<String, InvalidateInterface> invalidateInterfaces = new ConcurrentHashMap<>();

    /** alias account key → last received TradingAccountsInfoMessage */
    private final Map<AccountInfoMessageKey, TradingAccountsInfoMessage> aliasToLastTradingAccountsInfoMessage = new ConcurrentHashMap<>();

    /** provider account key → last received TradingAccountsInfoMessage */
    private final Map<AccountInfoMessageKey, TradingAccountsInfoMessage> providerToLastTradingAccountsInfoMessage = new ConcurrentHashMap<>();

    private final AtomicReference<RegisteredAccountsDemoSettingsPanel> settingsPanel = new AtomicReference<>();
    private final AtomicBoolean isWorking = new AtomicBoolean(false);

    public RegisteredAccountsDemo(Layer1ApiProvider provider) {
        this.provider = provider;
        ListenableHelper.addListeners(provider, this);
    }

    @Override
    public void finish() {
        synchronized (indicatorsFullNameToUserName) {
            for (String userName : indicatorsFullNameToUserName.values()) {
                provider.sendUserMessage(
                        new Layer1ApiUserMessageModifyIndicator(RegisteredAccountsDemo.class, userName, false));
            }
            indicatorsFullNameToUserName.clear();
        }
        this.isWorking.set(false);
        settingsPanel.set(null);
        provider.sendUserMessage(getGeneratorMessage(false));
        aliasIndicatorNames.clear();
        aliasesToProgrammaticName.clear();
        invalidateInterfaces.clear();
    }

    @Override
    public void onUserMessage(Object data) {
        if (data.getClass() == UserMessageLayersChainCreatedTargeted.class) {
            UserMessageLayersChainCreatedTargeted message = (UserMessageLayersChainCreatedTargeted) data;
            if (message.targetClass == getClass()) {
                        provider.sendUserMessage(new Layer1ApiDataInterfaceRequestMessage(
                                dataStructureInterface -> {
                                    this.dataStructureInterface = dataStructureInterface;
                                    invalidateInterfaces.values().forEach(InvalidateInterface::invalidate);
                                }));
                registerIndicator(createGeneralIndicatorMessage());

                this.isWorking.set(true);

                provider.sendUserMessage(getGeneratorMessage(true));
            }
        }
    }

    @Override
    public void onInstrumentAdded(String alias, InstrumentInfo instrumentInfo) {
        String providerProgrammaticName = ProviderTypeHelper.getProviderProgrammaticName(instrumentInfo.type);
        aliasesToProgrammaticName.put(alias, providerProgrammaticName);

        // Create a per-alias indicator on the bottom chart
        String indicatorUserName = "Accounts: " + alias;
        aliasIndicatorNames.put(alias, indicatorUserName);
        registerIndicator(createPerAliasIndicatorMessage(alias, indicatorUserName));
    }

    @Override
    public void onInstrumentRemoved(String alias) {
        aliasesToProgrammaticName.remove(alias);

        String indicatorUserName = aliasIndicatorNames.remove(alias);
        if (indicatorUserName != null) {
            provider.sendUserMessage(
                    new Layer1ApiUserMessageModifyIndicator(RegisteredAccountsDemo.class, indicatorUserName, false));
            synchronized (indicatorsFullNameToUserName) {
                indicatorsFullNameToUserName.values().remove(indicatorUserName);
            }
            invalidateInterfaces.remove(indicatorUserName);
        }
    }

    @Override
    public void onInstrumentNotFound(String symbol, String exchange, String type) { }

    @Override
    public void onInstrumentAlreadySubscribed(String symbol, String exchange, String type) { }

    @Override
    public StrategyPanel[] getCustomGuiFor(final String alias, final String indicatorName) {
        final RegisteredAccountsDemoSettingsPanel panel = settingsPanel
                .updateAndGet(s -> (s != null) ? s :
                        RegisteredAccountsDemoSettingsPanel.newPanel(
                                RegisteredAccountsDemo.NAME,
                                aliasToLastTradingAccountsInfoMessage.values(),
                                providerToLastTradingAccountsInfoMessage.values(),
                                isWorking));
        settingsPanel.get().updateIfNeeded();
        return new StrategyPanel[]{panel};
    }

    @Override
    public void calculateValuesInRange(String indicatorName, String indicatorAlias, long t0,
                                       long intervalWidth, int intervalsNumber,
                                       CalculatedResultListener listener) {
        if (dataStructureInterface == null) {
            listener.setCompleted();
            return;
        }

        String userName = indicatorsFullNameToUserName.get(indicatorName);

        ArrayList<DataStructureInterface.TreeResponseInterval> intervalResponse;
        if (GENERAL_INDICATOR_NAME.equals(userName)) {
            if (!aliasesToProgrammaticName.containsKey(indicatorAlias)) {
                listener.setCompleted();
                return;
            }
            intervalResponse = dataStructureInterface.getByProvider(t0, intervalWidth,
                    intervalsNumber, aliasesToProgrammaticName.get(indicatorAlias), new DataStructureInterface.StandardEvents[] { DataStructureInterface.StandardEvents.ACCOUNT });
        } else {
            intervalResponse = dataStructureInterface.get(t0, intervalWidth,
                    intervalsNumber, indicatorAlias, new DataStructureInterface.StandardEvents[] { DataStructureInterface.StandardEvents.ACCOUNT });
        }

        Map<String, TradingAccountInfoAddEvent> availableAccounts = getAccountEvent(intervalResponse.getFirst()).accountInfoMap.entrySet()
                .stream()
                .filter(e -> e.getValue().getClass() == TradingAccountInfoAddEvent.class)
                .collect(Collectors.toMap(Map.Entry::getKey, e -> (TradingAccountInfoAddEvent) e.getValue()));

        for (int i = 1; i <= intervalsNumber; ++i) {
            TradingAccountInfoAggregationEvent accountEvent = getAccountEvent(intervalResponse.get(i));

            accountEvent.accountInfoMap.forEach((id, event) -> {
                if (event.getClass() == TradingAccountInfoAddEvent.class) {
                    availableAccounts.put(id, (TradingAccountInfoAddEvent) event);
                } else {
                    availableAccounts.remove(id);
                }
            });
            listener.provideResponse(availableAccounts.size());
        }

        listener.setCompleted();
    }

    private TradingAccountInfoAggregationEvent getAccountEvent(DataStructureInterface.TreeResponseInterval interval) {
        return (TradingAccountInfoAggregationEvent) interval.events.get(DataStructureInterface.StandardEvents.ACCOUNT.toString());
    }

    @Override
    public OnlineValueCalculatorAdapter createOnlineValueCalculator(String indicatorName,
                                                                     String indicatorAlias,
                                                                     long time,
                                                                     Consumer<Object> listener,
                                                                     InvalidateInterface invalidateInterface) {
        if (dataStructureInterface == null) {
            return new OnlineValueCalculatorAdapter() {};
        }

        String userName = indicatorsFullNameToUserName.get(indicatorName);
        if (userName != null) {
            invalidateInterfaces.put(userName, invalidateInterface);
        }

        if (GENERAL_INDICATOR_NAME.equals(userName)) {
            return new OnlineValueCalculatorAdapter() {
                @Override
                public void onUserMessage(Object data) {
                    if (data instanceof TradingAccountsInfoMessage) {
                        String programmaticName = aliasesToProgrammaticName.get(indicatorAlias);
                        if (programmaticName != null) {
                            Set<String> accounts = programmaticNameToAvailableAccountsSet.get(programmaticName);
                            listener.accept(accounts == null ? 0 : accounts.size());
                        }
                    }
                }
            };
        }

        return new OnlineValueCalculatorAdapter() {
            @Override
            public void onUserMessage(Object data) {
                if (data instanceof TradingAccountsInfoMessage) {
                    listener.accept((double) accountListManager.getAccountsByAlias(indicatorAlias).size());
                }
            }
        };
    }

    /** Register an indicator and record the fullName→userName mapping. */
    private void registerIndicator(Layer1ApiUserMessageModifyIndicator msg) {
        synchronized (indicatorsFullNameToUserName) {
            indicatorsFullNameToUserName.put(msg.fullName, msg.userName);
        }
        provider.sendUserMessage(msg);
    }

    /** Per-alias indicator (BOTTOM chart – accounts count for one instrument). */
    private Layer1ApiUserMessageModifyIndicator createPerAliasIndicatorMessage(String indicatorAlias, String userName) {
        return new Layer1ApiUserMessageModifyIndicator(
                RegisteredAccountsDemo.class,
                userName,
                true,
                new IndicatorColorScheme() {
                    @Override
                    public ColorDescription[] getColors() {
                        return new ColorDescription[]{
                                new ColorDescription(RegisteredAccountsDemo.class,
                                        PER_ALIAS_COLOR, PER_ALIAS_COLOR_DEFAULT, false)
                        };
                    }

                    @Override
                    public String getColorFor(Double value) {
                        return PER_ALIAS_COLOR;
                    }

                    @Override
                    public ColorIntervalResponse getColorIntervalsList(double valueFrom, double valueTo) {
                        return new ColorIntervalResponse(new String[]{PER_ALIAS_COLOR}, new double[]{});
                    }
                },
                null,      // Layer1IndicatorColorInterface
                null,      // lineStyle (default solid)
                Color.white,
                Color.black,
                null,      // displayLogic
                null, null, null, null,
                GraphType.BOTTOM,
                true,      // isSupportWidget
                true,      // isShowColorSettings
                null,      // isEnableSettingsFromConfigPopup
                RegisteredAccountsDemo.this,  // onlineCalculatable
                new AliasFilter() {
                    @Override
                    public boolean isDisplayedForAlias(String alias) {
                        return Objects.equals(alias, indicatorAlias);
                    }
                }
        );
    }

    /** General indicator (BOTTOM chart – total accounts with alias null count per provider). */
    private Layer1ApiUserMessageModifyIndicator createGeneralIndicatorMessage() {
        return new Layer1ApiUserMessageModifyIndicator(
                RegisteredAccountsDemo.class,
                GENERAL_INDICATOR_NAME,
                true,
                new IndicatorColorScheme() {
                    @Override
                    public ColorDescription[] getColors() {
                        return new ColorDescription[]{
                                new ColorDescription(RegisteredAccountsDemo.class,
                                        GENERAL_COLOR, GENERAL_COLOR_DEFAULT, false)
                        };
                    }

                    @Override
                    public String getColorFor(Double value) {
                        return GENERAL_COLOR;
                    }

                    @Override
                    public ColorIntervalResponse getColorIntervalsList(double valueFrom, double valueTo) {
                        return new ColorIntervalResponse(new String[]{GENERAL_COLOR}, new double[]{});
                    }
                },
                null,      // Layer1IndicatorColorInterface
                null,      // lineStyle
                Color.white,
                Color.black,
                null,      // displayLogic
                null, null, null, null,
                GraphType.BOTTOM,
                true,      // isSupportWidget
                true,      // isShowColorSettings
                null,      // isEnableSettingsFromConfigPopup
                RegisteredAccountsDemo.this,  // onlineCalculatable
                null       // aliasFilter
        );
    }

    private static class MockEvent implements CustomGeneratedEvent {

        @Override
        public long getTime() {
            return 0;
        }

        @Override
        public Object clone() {
            return null;
        }
    }

    /**
     * Generator doesn't generate any events, it only listens to TradingAccountsInfoMessage
     */
    private Layer1ApiUserMessageAddStrategyUpdateGenerator getGeneratorMessage(boolean isAdd) {
        return new Layer1ApiUserMessageAddStrategyUpdateGenerator(MultipleAccountSupportDemoBase.class,
                RegisteredAccountsDemo.NAME,
                isAdd,
                true,
                new StrategyUpdateGenerator() {
            private Consumer<CustomGeneratedEventAliased> consumer;

            @Override
            public void setGeneratedEventsConsumer(Consumer<CustomGeneratedEventAliased> consumer) {
                this.consumer = consumer;
            }

            @Override
            public Consumer<CustomGeneratedEventAliased> getGeneratedEventsConsumer() {
                return consumer;
            }

            @Override
            public void onUserMessage(Object data) {
                accountListManager.onUserMessage(data);
                if (data instanceof TradingAccountsInfoMessage message) {
                    String accountBelongsTo = message.alias == null ? message.accountInfo.providerProgrammaticName : message.alias;
                    AccountInfoMessageKey accountInfoMessageKey = new AccountInfoMessageKey(message.accountInfo.id, accountBelongsTo);
                    if (message.alias != null) {
                        aliasToLastTradingAccountsInfoMessage.put(accountInfoMessageKey, message);
                    } else {
                        providerToLastTradingAccountsInfoMessage.put(accountInfoMessageKey, message);
                        programmaticNameToAvailableAccountsSet
                                .computeIfAbsent(accountBelongsTo, k -> ConcurrentHashMap.newKeySet())
                                .add(message.accountInfo.id);
                    }
                    SwingUtilities.invokeLater(() -> {
                        if (settingsPanel.get() != null) {
                            settingsPanel.get().updateIfNeeded();
                        }
                    });
                }
            }

            @Override
            public void setTime(long time) {}
        }, new GeneratedEventInfo[] {new GeneratedEventInfo(MockEvent.class)});
    }
}
