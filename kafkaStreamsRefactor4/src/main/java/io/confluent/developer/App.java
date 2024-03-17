package io.confluent.developer;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.KafkaAdminClient;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.KTable;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.kstream.Produced;
import org.apache.kafka.streams.state.KeyValueStore;
import shoes.*;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.Arrays;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;

public class App {


    private final static String DEV_CONFIG_FILE = "configuration/dev.properties";
    private static final String ORDERS_TOPIC = "orders";
    private static final String CUSTOMERS_TOPIC = "customers";
    private static final String PRODUCTS_TOPIC = "products";
    private static final String OUTPUT_TOPIC = "orders-enriched4";

    public Topology buildTopology(Properties allProps) {
        final StreamsBuilder builder = new StreamsBuilder();

        KTable<String, shoe_orders> ordersTable = builder.table(ORDERS_TOPIC,
                (new SpecificAvroSerdeBuilder<shoe_orders>()).buildConsumed(allProps));
        KTable<String, shoe_customers> customersTable = builder.table(CUSTOMERS_TOPIC,
                (new SpecificAvroSerdeBuilder<shoe_customers>()).buildConsumed(allProps));
        KTable<String, shoe_product> productsTable = builder.table(PRODUCTS_TOPIC,
                (new SpecificAvroSerdeBuilder<shoe_product>()).buildConsumed(allProps));

        OrdersCustomerJoiner ordersCustomerJoiner = new OrdersCustomerJoiner();
        OrdersProductJoiner ordersProductJoiner = new OrdersProductJoiner();

        KTable<String, shoe_orders_customers_products> ordersCustomersTable = ordersTable.leftJoin(customersTable,
                shoe_orders::getCustomerId, ordersCustomerJoiner,
                Materialized.<String, shoe_orders_customers_products, KeyValueStore<Bytes, byte[]>>as(
                                "orders-customers-store") /* state store name */
                        .withValueSerde((new SpecificAvroSerdeBuilder<shoe_orders_customers_products>()).buildSerde(allProps))
        );

        KTable<String, shoe_orders_customers_products> ordersProductsTable = ordersTable.leftJoin(productsTable,
                shoe_orders::getProductId, ordersProductJoiner,
                Materialized.<String, shoe_orders_customers_products, KeyValueStore<Bytes, byte[]>>as(
                                "orders-product-store") /* state store name */
                        .withValueSerde(new SpecificAvroSerdeBuilder<shoe_orders_customers_products>().buildSerde(allProps))
        );

        ordersCustomersTable.toStream().to(OUTPUT_TOPIC, Produced.with(Serdes.String(),
                (new SpecificAvroSerdeBuilder<shoe_orders_customers_products>()).buildSerde(allProps)));

        ordersProductsTable.toStream().to(OUTPUT_TOPIC, Produced.with(Serdes.String(),
                (new SpecificAvroSerdeBuilder<shoe_orders_customers_products>()).buildSerde(allProps)));

        ordersTable.mapValues(order -> shoe_orders_customers_products.newBuilder().setOrderId(order.getOrderId()).
                setTs(order.getTs()).build()).toStream().to(OUTPUT_TOPIC, Produced.with(Serdes.String(),
                (new SpecificAvroSerdeBuilder<shoe_orders_customers_products>()).buildSerde(allProps)));

        return builder.build();
    }

    public static void main(String[] args) throws IOException {
        new App().runRecipe(DEV_CONFIG_FILE);
    }

    private void runRecipe(final String configPath) throws IOException {
        final Properties allProps = new Properties();
        try (InputStream inputStream = new FileInputStream(configPath)) {
            allProps.load(inputStream);
        }

        final Topology topology = this.buildTopology(allProps);

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
            admin.deleteTopics(Arrays.asList(OUTPUT_TOPIC));
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