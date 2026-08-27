package velox.api.layer1.simpledemo.tradingmessages;

import velox.api.layer1.Layer1ApiAdminAdapter;
import velox.api.layer1.Layer1ApiFinishable;
import velox.api.layer1.Layer1ApiInstrumentAdapter;
import velox.api.layer1.Layer1ApiProvider;
import velox.api.layer1.Layer1CustomPanelsGetter;
import velox.api.layer1.annotations.Layer1ApiVersion;
import velox.api.layer1.annotations.Layer1ApiVersionValue;
import velox.api.layer1.annotations.Layer1Attachable;
import velox.api.layer1.annotations.Layer1DuplicateInstrumentsAware;
import velox.api.layer1.annotations.Layer1StrategyName;
import velox.api.layer1.annotations.Layer1TradingStrategy;
import velox.api.layer1.common.ListenableHelper;
import velox.api.layer1.common.Log;
import velox.api.layer1.common.helper.AccountListManager;
import velox.api.layer1.common.helper.instrumentmanager.InstrumentsManager;
import velox.api.layer1.common.helper.instrumentmanager.InstrumentsManagerFactory;
import velox.api.layer1.data.InstrumentInfo;
import velox.api.layer1.messages.Layer1ApiUiValueChangedMessage;
import velox.api.layer1.messages.Layer1ApiUserMessageReloadStrategyGui;
import velox.api.layer1.messages.UserMessageLayersChainCreatedTargeted;
import velox.gui.StrategyPanel;

import javax.swing.SwingUtilities;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@Layer1Attachable
@Layer1StrategyName(TradingMessagesSender.NAME)
@Layer1ApiVersion(Layer1ApiVersionValue.VERSION2)
@Layer1TradingStrategy
@Layer1DuplicateInstrumentsAware()
public class TradingMessagesSender implements
        Layer1ApiAdminAdapter,
        Layer1ApiFinishable,
        Layer1ApiInstrumentAdapter,
        Layer1CustomPanelsGetter {
    public static final String NAME = "TradingMessagesDemo";
    private final Layer1ApiProvider provider;
    private final AtomicReference<TradingMessagesSenderSettingsPanel> settingsPanel = new AtomicReference<>();
    private final Map<String, InstrumentInfo> instruments = new ConcurrentHashMap<>();
    private final AtomicBoolean isWorking = new AtomicBoolean(false);
    private final ExecutorService asyncExecutor = Executors.newSingleThreadExecutor();
    private final AccountListManager accountListManager = new AccountListManager();
    private final InstrumentsManager instrumentsManager = InstrumentsManagerFactory.getInstrumentsManager();

    public TradingMessagesSender(final Layer1ApiProvider provider) {
        ListenableHelper.addListeners(provider, this);
        this.provider = provider;
    }
    
    @Override
    public void finish() {
        this.isWorking.set(false);
        instruments.clear();
        settingsPanel.set(null);
        asyncExecutor.shutdown();
        ListenableHelper.removeListeners(provider, this);
    }
    
    @Override
    public void onInstrumentAdded(final String alias, final InstrumentInfo info) {
        instrumentsManager.onInstrumentAdded(alias, info);
        if (instrumentsManager.canHasChart(alias)) {
            // we want to have ability to send messages only for the instruments that can have charts
            // you don't need to have this check if your addon doesn't support duplicated instruments mode
            // (in non-duplicated instruments mode each instrument can have a chart)
            SwingUtilities.invokeLater(() -> {
                instruments.put(alias, info);
                updateInstruments();
            });
        }
    }

    @Override
    public void onInstrumentRemoved(final String alias) {
        if (instrumentsManager.canHasChart(alias)) {
            SwingUtilities.invokeLater(() -> {
                instruments.remove(alias);
                updateInstruments();
            });
        }
        instrumentsManager.onInstrumentRemoved(alias);
    }

    @Override
    public StrategyPanel[] getCustomGuiFor(final String alias, final String indicatorName) {
        final TradingMessagesSenderSettingsPanel panel = settingsPanel.updateAndGet(s -> (s != null) ? s
                : TradingMessagesSenderSettingsPanel.newPanel(NAME, this::sendUserMessage, accountListManager::getAccountsByAlias, isWorking));
        settingsPanel.get().updateIfNeeded();
        updateInstruments();
        return new StrategyPanel[]{panel};
    }

    @Override
    public void onUserMessage(Object data) {
        if (accountListManager.onUserMessage(data)) {
            if (settingsPanel.get() != null) {
                settingsPanel.get().updateAccounts();
            }
        }

        if (data instanceof Layer1ApiUiValueChangedMessage valueChanged) {
            // Demonstrates reacting to any Trading Control Panel value change that Bookmap broadcasts to add-ons.
            Log.info(NAME, "Trading Control Panel value changed: " + valueChanged);
            final TradingMessagesSenderSettingsPanel panel = settingsPanel.get();
            if (panel != null) {
                panel.appendBroadcast(valueChanged.toString());
            }
        }

        if (data.getClass() == UserMessageLayersChainCreatedTargeted.class) {
            final Class<?> targetClass = ((UserMessageLayersChainCreatedTargeted) data).targetClass;
            if (!targetClass.equals(TradingMessagesSender.class)) {
                return;
            }
            this.isWorking.set(true);
            asyncExecutor.execute(() -> provider.sendUserMessage(new Layer1ApiUserMessageReloadStrategyGui()));
        }

    }

    private void sendUserMessage(Object data) {
        asyncExecutor.execute(() -> provider.sendUserMessage(data));
    }

    private void updateInstruments() {
        if (settingsPanel.get() != null) {
            settingsPanel.get().updateInstruments(instruments.keySet().stream().toList());
        }
    }
}
