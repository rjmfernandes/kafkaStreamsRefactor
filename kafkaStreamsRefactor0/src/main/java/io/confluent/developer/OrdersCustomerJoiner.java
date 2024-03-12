package io.confluent.developer;

import org.apache.kafka.streams.kstream.ValueJoiner;
import shoes.shoe_customers;
import shoes.shoe_orders;
import shoes.shoe_orders_customers;

public class OrdersCustomerJoiner implements ValueJoiner<shoe_orders, shoe_customers, shoe_orders_customers> {

    @Override
    public shoe_orders_customers apply(shoe_orders order, shoe_customers customer) {
        if (customer == null) {
            customer = shoe_customers.newBuilder().setId(order.getCustomerId()).setCountry("").setEmail("").
                    setFirstName("").setLastName("").setPhone("").setState("").setStreetAddress("").
                    setZipCode("").setCountryCode("").build();
        }
        return shoe_orders_customers.newBuilder()
                .setCustomer(customer)
                .setOrderId(order.getOrderId())
                .setTs(order.getTs())
                .setProductId(order.getProductId()).build();
    }
}
