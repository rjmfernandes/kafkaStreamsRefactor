package io.confluent.developer;

import shoes.shoe_product;
import java.io.IOException;

public class ProductConsumer {

    public static void main(String[] args) throws IOException {
        GenericConsumerUpdateApp<shoe_product> consumerUpdateApp = new GenericConsumerUpdateApp<>();
        consumerUpdateApp.execute(App.PRODUCTS_TOPIC, "kafkaStreamsRefactorProducts2");
    }
}
