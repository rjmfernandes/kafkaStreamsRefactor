package io.confluent.developer;

import org.apache.kafka.streams.kstream.ValueJoiner;
import shoes.shoe_customers;
import shoes.shoe_orders;
import shoes.shoe_orders_customers;

public class CustomersOrderJoiner implements ValueJoiner<shoe_customers, shoe_orders, shoe_orders_customers> {

    @Override
    public shoe_orders_customers apply(shoe_customers customer,shoe_orders order) {
        if (order == null) {
            return null;
        }
        return shoe_orders_customers.newBuilder()
                .setCustomer(customer)
                .setOrderId(order.getOrderId())
                .setTs(order.getTs())
                .setProductId(order.getProductId()).build();
    }
}
