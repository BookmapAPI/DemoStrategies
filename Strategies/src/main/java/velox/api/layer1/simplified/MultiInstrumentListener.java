package velox.api.layer1.simplified;

import velox.api.layer1.data.InstrumentInfo;

/**
 * Listen to data from multiple instruments at once. Without this interface you
 * will only get data about the alias passed inside
 * {@link CustomModule#initialize(String, InstrumentInfo, Api)}, when this
 * interface is implemented data for all instruments will be received. You can
 * determine which instrument data belongs to by the alias set with previous
 * {@link #onCurrentInstrument(String)} call. Note, that if alias did not change
 * between two updates {@link #onCurrentInstrument(String)} will not be called.
 */
public interface MultiInstrumentListener {

    void onCurrentInstrument(String alias);

    void onInstrumentAdded(InstrumentInfo info);
}
