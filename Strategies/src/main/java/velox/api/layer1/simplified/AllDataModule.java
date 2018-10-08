package velox.api.layer1.simplified;

/**
 * Interface implementing all simplified module interfaces including historical
 * data. For those who just want all types of data. Helpful to get started, not
 * recommended for use in production, unless you really need ALL the data
 * (because otherwise you are getting data you don't need). Especially think if
 * you need {@link HistoricalDataListener} and {@link HistoricalModeListener} -
 * those come with significant added cost.
 */
public interface AllDataModule extends
    CustomModule,
    BalanceListener,
    BarDataListener,
    BboListener,
    DepthDataListener,
    MultiInstrumentListener,
    OrdersListener,
    PositionListener,
    TimeListener,
    TradeDataListener,
    HistoricalModeListener {

}
