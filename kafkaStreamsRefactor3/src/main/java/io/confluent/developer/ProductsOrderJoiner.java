package io.confluent.developer;

import org.apache.kafka.streams.kstream.ValueJoiner;
import shoes.*;

public class ProductsOrderJoiner implements ValueJoiner<shoe_product, shoe_orders,
        shoe_orders_products> {

    @Override
    public shoe_orders_products apply(shoe_product product, shoe_orders order) {
        if(order==null) {
            return null;
        }
        return shoe_orders_products.newBuilder()
                .setProduct(product)
                .setCustomerId(order.getCustomerId())
                .setOrderId(order.getOrderId())
                .setTs(order.getTs()).build();
    }
}
