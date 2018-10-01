package velox.api.layer1.layers.tradinghelper;

public class OrderRequest implements Comparable<OrderRequest>{
    public final String orderId;
    public final Integer requestLevel;
    
    public OrderRequest(String orderId, Integer requestLevel) {
        this.orderId = orderId;
        this.requestLevel = requestLevel;
    }
    
    @Override
    public int compareTo(OrderRequest o) {
        if (!this.orderId.equals(o.orderId)) {
            return this.orderId.compareTo(o.orderId);
        }
        
        return this.requestLevel.compareTo(o.requestLevel);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof OrderRequest) {
            OrderRequest orderRequest = (OrderRequest) obj;
            return orderRequest.orderId.equals(this.orderId) && orderRequest.requestLevel == this.requestLevel;
        }
        return false;
    }
    
    @Override
    public String toString() {
        return "(" + orderId + " " + requestLevel + ")";
    }
}
