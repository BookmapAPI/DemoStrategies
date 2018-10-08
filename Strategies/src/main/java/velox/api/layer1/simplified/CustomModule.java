package velox.api.layer1.simplified;

import velox.api.layer1.data.InstrumentInfo;

public interface CustomModule {
    void initialize(String alias, InstrumentInfo info, Api api);

    void stop();
}
