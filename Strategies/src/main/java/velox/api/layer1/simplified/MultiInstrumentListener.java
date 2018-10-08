package velox.api.layer1.simplified;

import velox.api.layer1.data.InstrumentInfo;

public interface MultiInstrumentListener {

    void onCurrentInstrument(String alias);

    void onInstrumentAdded(InstrumentInfo info);
    void onInstrumentRemoved();
}
