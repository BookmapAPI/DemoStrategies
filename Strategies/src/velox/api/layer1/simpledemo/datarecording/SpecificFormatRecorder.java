package velox.api.layer1.simpledemo.datarecording;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import velox.api.layer1.data.InstrumentInfo;

/**
 * This class performs recording into a specific format. This format can be
 * opened in a text editor and reviewed. It contains some unused fields - the
 * only reason those exist is to make it readable by one of our internal tools
 * for testing purpose.
 */
public class SpecificFormatRecorder {

    private static final String EOL = System.getProperty("line.separator");
    private static final char DELIMITER = ',';

    private FileWriter depthWriter;
    private FileWriter ordersWriter;

    public SpecificFormatRecorder(long time, File depth, File orders, String dataSource) throws IOException {
        depthWriter = new FileWriter(depth);
        ordersWriter = new FileWriter(orders);

        depthWriter
                .append(SpecificFormatTags.ON_FEED_SOURCE)
                .append(DELIMITER).append(Long.toString(time))
                .append(DELIMITER).append(Integer.toString(-1))
                .append(DELIMITER).append(dataSource)
                .append(EOL);
    }

    public void onTrade(int id, double price, int size, int aggressor, int otcCode) throws IOException {
        depthWriter
                .append(SpecificFormatTags.ON_TRADE)
                .append(DELIMITER).append(Long.toString(System.currentTimeMillis()))
                .append(DELIMITER).append(Integer.toString(id))
                .append(DELIMITER).append(Double.toString(price))
                .append(DELIMITER).append(Integer.toString(size))
                .append(DELIMITER).append(Integer.toString(aggressor))
                .append(DELIMITER).append(Integer.toString(otcCode))
                .append(EOL);
        depthWriter.flush();
    }

    public void onDepth(int id, boolean isBid, double price, int size) throws IOException {
        depthWriter
                .append(SpecificFormatTags.ON_BOOK_UPDATE)
                .append(DELIMITER).append(Long.toString(System.currentTimeMillis()))
                .append(DELIMITER).append(Integer.toString(id))
                .append(DELIMITER)
                .append(Integer.toString(isBid ? SpecificFormatTags.BID_SIDE : SpecificFormatTags.ASK_SIDE))
                .append(DELIMITER).append(Double.toString(price))
                .append(DELIMITER).append(Integer.toString(size))
                .append(EOL);
        depthWriter.flush();
    }

    public void onInstrumentAdded(int id, InstrumentInfo instrumentInfo) throws IOException {
        depthWriter
                .append(SpecificFormatTags.ON_CONTRACT_DETAILS)
                .append(DELIMITER).append(Long.toString(System.currentTimeMillis()))
                .append(DELIMITER).append(Integer.toString(id))
                .append(DELIMITER).append(instrumentInfo.exchange)
                .append(DELIMITER).append(instrumentInfo.symbol)
                .append(DELIMITER).append(instrumentInfo.type)
                .append(DELIMITER).append(Double.toString(instrumentInfo.pips))
                .append(DELIMITER).append(Double.toString(instrumentInfo.multiplier))
                .append(DELIMITER).append("0")
                .append(EOL);
        depthWriter.flush();
    }

    public void onOrderData(String data) throws IOException {
        ordersWriter.append(data).append(EOL);
        ordersWriter.flush();
    }
}
