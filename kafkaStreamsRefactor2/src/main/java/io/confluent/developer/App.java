package io.confluent.developer;

import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.KafkaAdminClient;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.*;
import shoes.shoe_customers;
import shoes.shoe_orders;
import shoes.shoe_orders_customers;
import shoes.shoe_product;
import shoes.shoe_orders_customers_products;

import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;

public class App {


    private final static String DEV_CONFIG_FILE = "configuration/dev.properties";

    private SpecificAvroSerde<shoe_orders> buildOrdersSerde(final Properties allProps) {
        final SpecificAvroSerde<shoe_orders> serde = new SpecificAvroSerde<>();
        final Map<String, String> config = (Map) allProps;
        serde.configure(config, false);
        return serde;
    }

    private SpecificAvroSerde<shoe_customers> buildCustomersSerde(final Properties allProps) {
        final SpecificAvroSerde<shoe_customers> serde = new SpecificAvroSerde<>();
        final Map<String, String> config = (Map) allProps;
        serde.configure(config, false);
        return serde;
    }

    private SpecificAvroSerde<shoe_product> buildProductsSerde(Properties allProps) {
        final SpecificAvroSerde<shoe_product> serde = new SpecificAvroSerde<>();
        final Map<String, String> config = (Map) allProps;
        serde.configure(config, false);
        return serde;
    }

    private SpecificAvroSerde<shoe_orders_customers> buildOrdersCustomersSerde(Properties allProps) {
        final SpecificAvroSerde<shoe_orders_customers> serde = new SpecificAvroSerde<>();
        final Map<String, String> config = (Map) allProps;
        serde.configure(config, false);
        return serde;
    }

    private SpecificAvroSerde<shoe_orders_customers_products> buildOrdersCustomersProductsSerde(Properties allProps) {
        final SpecificAvroSerde<shoe_orders_customers_products> serde = new SpecificAvroSerde<>();
        final Map<String, String> config = (Map) allProps;
        serde.configure(config, false);
        return serde;
    }

    public Topology buildTopology(Properties allProps,
                                  final SpecificAvroSerde<shoe_orders> ordersSerde,
                                  final SpecificAvroSerde<shoe_customers> customersSerde,
                                  final SpecificAvroSerde<shoe_product> productsSerde,
                                  final SpecificAvroSerde<shoe_orders_customers> ordersCustomersSerde,
                                  final SpecificAvroSerde<shoe_orders_customers_products> ordersCustomersProductsSerde)
    {
        final StreamsBuilder builder = new StreamsBuilder();

        final String ordersTopic = allProps.getProperty("orders.topic.name");
        final String customersTopic = allProps.getProperty("customers.topic.name");
        final String productsTopic = allProps.getProperty("products.topic.name");
        final String outputTopic = allProps.getProperty("output.topic.name");

        KStream<String, shoe_orders> ordersStream = builder.stream(ordersTopic, Consumed.with(Serdes.String(),
                ordersSerde));
        KTable<String, shoe_customers> customersTable = builder.table(customersTopic, Consumed.with(Serdes.String(),
                customersSerde));
        KTable<String, shoe_product> productsTable = builder.table(productsTopic, Consumed.with(Serdes.String(),
                productsSerde));

        OrdersCustomerJoiner ordersCustomerJoiner = new OrdersCustomerJoiner();
        OrdersCustomerProductJoiner ordersCustomerProductJoiner = new OrdersCustomerProductJoiner();

        KStream<String,shoe_orders_customers> ordersCustomersStream= ordersStream.
                selectKey( (orderID, order) -> order.getCustomerId() ).
                leftJoin(customersTable,ordersCustomerJoiner,
                        Joined.with(Serdes.String(), ordersSerde,customersSerde));

        KStream<String,shoe_orders_customers_products> ordersCustomersProductsStream= ordersCustomersStream.
                selectKey( (orderID, order) -> order.getProductId() ).
                leftJoin(productsTable,ordersCustomerProductJoiner,
                        Joined.with(Serdes.String(), ordersCustomersSerde,productsSerde));

        ordersCustomersProductsStream.to(outputTopic, Produced.with(Serdes.String(),
                ordersCustomersProductsSerde));

        return builder.build();
    }

    public static void main(String[] args) throws IOException {

        new App().runRecipe(DEV_CONFIG_FILE);
    }

    private void runRecipe(final String configPath) throws IOException {
        final Properties allProps = Config.loadEnvProperties();

        final Topology topology = this.buildTopology(allProps, this.buildOrdersSerde(allProps),
                this.buildCustomersSerde(allProps),
                this.buildProductsSerde(allProps),
                this.buildOrdersCustomersSerde(allProps),
                this.buildOrdersCustomersProductsSerde(allProps));

        System.out.println("Printing the Kafka Streams Topology");
        System.out.println(topology.describe().toString());

        final KafkaStreams streams = new KafkaStreams(topology, allProps);

        final CountDownLatch latch = new CountDownLatch(1);

        // Attach shutdown handler to catch Control-C.
        Runtime.getRuntime().addShutdownHook(new Thread("streams-shutdown-hook") {
            @Override
            public void run() {
                streams.close(Duration.ofSeconds(5));
                latch.countDown();
            }
        });

        try {
            //only for demo purposes
            AdminClient admin = KafkaAdminClient.create(allProps);
            admin.deleteTopics(Arrays.asList(allProps.getProperty("output.topic.name")));
            streams.cleanUp();
            streams.start();
            latch.await();
        } catch (Throwable e) {
            System.exit(1);
        } finally {
            streams.close();
        }

    }
}