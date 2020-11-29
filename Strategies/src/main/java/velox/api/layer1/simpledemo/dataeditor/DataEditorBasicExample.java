package velox.api.layer1.simpledemo.dataeditor;

import java.util.HashMap;
import java.util.Map.Entry;

import velox.api.layer1.Layer1ApiFinishable;
import velox.api.layer1.Layer1ApiProvider;
import velox.api.layer1.annotations.Layer1ApiVersion;
import velox.api.layer1.annotations.Layer1ApiVersionValue;
import velox.api.layer1.annotations.Layer1StrategyName;
import velox.api.layer1.annotations.Layer1UpstreamDataEditor;
import velox.api.layer1.data.InstrumentInfo;
import velox.api.layer1.data.TradeInfo;
import velox.api.layer1.layers.Layer1ApiInjectorRelay;
import velox.api.layer1.layers.utils.OrderBook;
import velox.api.layer1.messages.UserMessageLayersChainCreatedTargeted;

/** Offsets depth data and trades 3 levels up and doubles displayed sizes.*/
@Layer1UpstreamDataEditor
@Layer1StrategyName("Data editor")
@Layer1ApiVersion(Layer1ApiVersionValue.VERSION2)
public class DataEditorBasicExample extends Layer1ApiInjectorRelay implements Layer1ApiFinishable {
    
    /** Sizes of depth updates and trades will be multiplied by this */
    private static final int SIZE_MULTIPLIER = 2;
    /** How many levels to offset the price */
    private static final int PRICE_OFFSET = 3;

    private boolean isActive = false;

    HashMap<String, OrderBook> originalDataBooks = new HashMap<>();
    HashMap<String, OrderBook> modifiedDataBooks = new HashMap<>();
    
    public DataEditorBasicExample(Layer1ApiProvider provider) {
        super(provider);
    }
    
    @Override
    public void onInstrumentAdded(String alias, InstrumentInfo instrumentInfo) {
        originalDataBooks.put(alias, new OrderBook());
        if (isActive) {
            modifiedDataBooks.put(alias, new OrderBook());
        }
        super.onInstrumentAdded(alias, instrumentInfo);
    }

    @Override
    public void onDepth(String alias, boolean isBid, int price, int size) {
       
        originalDataBooks.get(alias).onUpdate(isBid, price, size);
        
        // If active - modify data. Otherwise we just get data for initialization
        if (isActive) {
            price = modifyPrice(price);
            size = modifySize(size);
            
            modifiedDataBooks.get(alias).onUpdate(isBid, price, size);
            // This could be outside if, but there is no difference since
            // inactive strategy has nowhere to forward data
            super.onDepth(alias, isBid, price, size);
        }
    }
    
    @Override
    public void onTrade(String alias, double price, int size, TradeInfo tradeInfo) {
        super.onTrade(alias, price + PRICE_OFFSET, size * SIZE_MULTIPLIER, tradeInfo);
    }

    private int modifySize(long size) {
        size *= SIZE_MULTIPLIER;
        return (int)size;
    }

    private int modifyPrice(int price) {
        price += PRICE_OFFSET;
        return price;
    }

    @Override
    public void finish() {
        injectSynchronously(() -> {
            if (isActive) {
                isActive = false;
                deactivate();
            }
        });
    }
    

    @Override
    public void onUserMessage(Object data) {
        if (data instanceof UserMessageLayersChainCreatedTargeted) {
            UserMessageLayersChainCreatedTargeted message = (UserMessageLayersChainCreatedTargeted) data;
            if (message.targetClass == getClass()) {
                isActive = true;
                activate();
            }
        }
        super.onUserMessage(data);
    }

    private void activate() {
        // Currently bookmap shows normal data, let's replace it with modified.
        for (String alias : originalDataBooks.keySet()) {
            OrderBook originalBook = originalDataBooks.get(alias);
            OrderBook modifiedBook = new OrderBook();
            modifiedDataBooks.put(alias, modifiedBook);
            
            for (Entry<Integer, Long> entry : originalBook.getBidMap().entrySet()) {
                modifiedBook.onUpdate(true,
                        modifyPrice(entry.getKey()),
                        modifySize(entry.getValue()));
            }
            for (Entry<Integer, Long> entry : originalBook.getAskMap().entrySet()) {
                modifiedBook.onUpdate(false,
                        modifyPrice(entry.getKey()),
                        modifySize(entry.getValue()));
            }
            
            sendUpdates(alias, originalBook, modifiedBook);
        }
    }
    
    private void deactivate() {
        for (String alias : originalDataBooks.keySet()) {
            OrderBook originalBook = originalDataBooks.get(alias);
            OrderBook modifiedBook = modifiedDataBooks.remove(alias);
            
            sendUpdates(alias, modifiedBook, originalBook);
        }
    }

    private void sendUpdates(String alias, OrderBook currentBook, OrderBook intendedBook) {
        
        // Erasing missing levels
        for (Integer price : currentBook.getBidMap().keySet()) {
            if (!intendedBook.getBidMap().containsKey(price)) {
                super.onDepth(alias, true, price, 0);
            }
        }
        for (Integer price : currentBook.getAskMap().keySet()) {
            if (!intendedBook.getAskMap().containsKey(price)) {
                super.onDepth(alias, false, price, 0);
            }
        }
        
        for (Entry<Integer, Long> entry : intendedBook.getBidMap().entrySet()) {
            super.onDepth(alias, true, entry.getKey(), entry.getValue().intValue());
        }
        for (Entry<Integer, Long> entry : intendedBook.getAskMap().entrySet()) {
            super.onDepth(alias, false, entry.getKey(), entry.getValue().intValue());
        }
    }
}
