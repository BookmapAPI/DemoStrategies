package velox.api.layer1.simplified.demo.model;

import velox.api.layer1.layers.utils.mbo.Order;

import java.util.Objects;

public class OrderAndCount {
    private Order order;
    private Long count;


    public OrderAndCount(Order order, Long count) {
        this.order = order;
        this.count = count;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof OrderAndCount that)) return false;
        return Objects.equals(order, that.order) && Objects.equals(count, that.count);
    }

    @Override
    public int hashCode() {
        return Objects.hash(order, count);
    }

    public Long getCount() {
        return count;
    }

    public void setCount(Long count) {
        this.count = count;
    }

    public Order getOrder() {
        return order;
    }

    public void setOrder(Order order) {
        this.order = order;
    }
}
