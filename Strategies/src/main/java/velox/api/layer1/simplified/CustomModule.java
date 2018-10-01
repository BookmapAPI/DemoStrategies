package velox.api.layer1.simplified;

import velox.api.layer1.data.InstrumentInfo;

public interface CustomModule {
    void initialize(InstrumentInfo info, Api api);

    default void stop() {};
}
