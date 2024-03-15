package io.confluent.developer;

import org.apache.kafka.streams.kstream.ValueJoiner;
import shoes.shoe_orders_customers;
import shoes.shoe_orders_customers_products;
import shoes.shoe_product;

public class OrdersCustomerProductJoiner implements ValueJoiner<shoe_orders_customers, shoe_product,
        shoe_orders_customers_products> {

    @Override
    public shoe_orders_customers_products apply(shoe_orders_customers order, shoe_product product) {
        if(product==null) {
            product=shoe_product.newBuilder().setId(order.getProductId()).setBrand("").setName("").setRating(0).
                    setSalePrice(0).build();
        }
        return shoe_orders_customers_products.newBuilder()
                .setProduct(product)
                .setCustomer(order.getCustomer())
                .setOrderId(order.getOrderId())
                .setTs(order.getTs()).build();
    }
}
