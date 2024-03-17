package io.confluent.developer;

import org.apache.kafka.streams.kstream.ValueJoiner;
import shoes.shoe_customers;
import shoes.shoe_orders;
import shoes.shoe_orders_customers_products;

public class OrdersCustomerJoiner implements ValueJoiner<shoe_orders, shoe_customers, shoe_orders_customers_products> {

    @Override
    public shoe_orders_customers_products apply(shoe_orders order, shoe_customers customer) {
        return shoe_orders_customers_products.newBuilder()
                .setCustomer(customer)
                .setOrderId(order.getOrderId()).build();
    }
}
