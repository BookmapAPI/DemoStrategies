package velox.api.layer1.simplified;

import velox.api.layer1.data.InstrumentInfo;

public interface MultiInstrumentListener {

    void onCurrentInstrument(String alias);

    default void onInstrumentAdded(InstrumentInfo info) {};
    default void onInstrumentRemoved() {};
}
