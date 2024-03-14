package io.confluent.developer;

import shoes.shoe_customers;

import java.io.IOException;

public class CustomerConsumer {

    public static void main(String[] args) throws IOException {
        GenericConsumerUpdateApp<shoe_customers> consumerUpdateApp = new GenericConsumerUpdateApp<>();
        consumerUpdateApp.execute(App.CUSTOMERS_TOPIC, "kafkaStreamsRefactorCustomers2");
    }
}
