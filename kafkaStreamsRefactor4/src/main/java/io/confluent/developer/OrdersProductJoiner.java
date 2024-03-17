package io.confluent.developer;

import org.apache.kafka.streams.kstream.ValueJoiner;
import shoes.*;

public class OrdersProductJoiner implements ValueJoiner<shoe_orders, shoe_product, shoe_orders_customers_products> {


    @Override
    public shoe_orders_customers_products apply(shoe_orders order, shoe_product product) {
        return shoe_orders_customers_products.newBuilder()
                .setProduct(product)
                .setOrderId(order.getOrderId()).build();
    }
}
