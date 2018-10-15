package velox.api.layer1.simplified;

import velox.api.layer1.annotations.Layer1SimpleAttachable;
import velox.api.layer1.data.InstrumentInfo;
import velox.api.layer1.data.InstrumentInfoCrypto;

/**
 * Must be implemented by modules annotated with {@link Layer1SimpleAttachable}.
 * Provides way for Bookmap to interact with module.
 */
public interface CustomModule {
    /**
     * Called before any other method
     *
     * @param alias
     *            host instrument (the one where checkbox was checked)
     * @param info
     *            instrument info. Might be an instance of a subclass, for example
     *            {@link InstrumentInfoCrypto}. In this case you can try casting to
     *            it to get more information.
     * @param api
     *            object for interacting with Bookmap
     * @param initialState
     *            additional information partially replacing historical data, such
     *            as last trade price
     */
    void initialize(String alias, InstrumentInfo info, Api api, InitialState initialState);

    /**
     * Called before unloading the module. If you have started any thread or
     * allocated any resources - that's a good place to release those.
     */
    void stop();
}
