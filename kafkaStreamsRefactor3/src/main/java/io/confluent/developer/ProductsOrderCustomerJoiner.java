package io.confluent.developer;

import org.apache.kafka.streams.kstream.ValueJoiner;
import shoes.*;

public class ProductsOrderCustomerJoiner implements ValueJoiner<shoe_orders_products, shoe_customers,
        shoe_orders_customers_products> {

    @Override
    public shoe_orders_customers_products apply(shoe_orders_products order, shoe_customers customer) {
        if(customer==null) {
            customer = shoe_customers.newBuilder().setId(order.getCustomerId()).setCountry("").setEmail("").
                    setFirstName("").setLastName("").setPhone("").setState("").setStreetAddress("").
                    setZipCode("").setCountryCode("").build();
        }
        return shoe_orders_customers_products.newBuilder()
                .setProduct(order.getProduct())
                .setCustomer(customer)
                .setOrderId(order.getOrderId())
                .setTs(order.getTs()).build();
    }
}
