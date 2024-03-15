package io.confluent.developer;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.KafkaAdminClient;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.*;
import shoes.*;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.Arrays;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;

public class App {


    public final static String DEV_CONFIG_FILE = "configuration/dev.properties";
    private static final String ORDERS_TOPIC = "orders";
    public static final String CUSTOMERS_TOPIC = "customers";
    public static final String PRODUCTS_TOPIC = "products";
    private static final String OUTPUT_TOPIC = "orders-enriched3";

    public Topology buildTopology(Properties allProps) {
        final StreamsBuilder builder = new StreamsBuilder();

        KTable<String, shoe_orders> ordersTable = builder.table(ORDERS_TOPIC,
                (new SpecificAvroSerdeBuilder<shoe_orders>()).buildConsumed(allProps));
        KTable<String, shoe_customers> customersTable = builder.table(CUSTOMERS_TOPIC,
                (new SpecificAvroSerdeBuilder<shoe_customers>()).buildConsumed(allProps));
        KTable<String, shoe_product> productsTable = builder.table(PRODUCTS_TOPIC,
                (new SpecificAvroSerdeBuilder<shoe_product>()).buildConsumed(allProps));

        OrdersCustomerJoiner ordersCustomerJoiner = new OrdersCustomerJoiner();
        CustomersOrderJoiner customersOrderJoiner = new CustomersOrderJoiner();
        OrdersCustomerProductJoiner ordersCustomerProductJoiner = new OrdersCustomerProductJoiner();
        ProductsOrderJoiner productsOrderJoiner = new ProductsOrderJoiner();
        ProductsOrderCustomerJoiner productsOrderCustomerJoiner = new ProductsOrderCustomerJoiner();

        /*orders stream leading*/

        KStream<String, shoe_orders_customers> ordersCustomersStream = ordersTable.toStream().
                selectKey((orderID, order) -> order.getCustomerId()).
                leftJoin(customersTable, ordersCustomerJoiner,
                        Joined.with(Serdes.String(),
                                (new SpecificAvroSerdeBuilder<shoe_orders>()).buildSerde(allProps),
                                (new SpecificAvroSerdeBuilder<shoe_customers>()).buildSerde(allProps)
                        )
                );

        KStream<String, shoe_orders_customers_products> ordersCustomersProductsStream = ordersCustomersStream.
                selectKey((orderID, order) -> order.getProductId()).
                leftJoin(productsTable, ordersCustomerProductJoiner,
                        Joined.with(Serdes.String(),
                                (new SpecificAvroSerdeBuilder<shoe_orders_customers>()).buildSerde(allProps),
                                (new SpecificAvroSerdeBuilder<shoe_product>()).buildSerde(allProps)
                        )
                );

        ordersCustomersProductsStream.to(OUTPUT_TOPIC,
                Produced.with(Serdes.String(),
                        (new SpecificAvroSerdeBuilder<shoe_orders_customers_products>()).buildSerde(allProps)
                )
        );

        /*customers stream leading*/
        KTable<String, shoe_orders> ordersTableKeyCustomerId = ordersTable.groupBy(
                        (key, value) -> KeyValue.pair(value.getCustomerId(), value),
                        Grouped.with("orderByCustomerId", Serdes.String(),
                                (new SpecificAvroSerdeBuilder<shoe_orders>()).buildSerde(allProps))).
                reduce(
                        (aggValue, newValue) -> newValue,
                        (aggValue, oldValue) -> oldValue
                );
        KStream<String, shoe_orders_customers> customersOrdersStream = customersTable.toStream().join(
                ordersTableKeyCustomerId,
                customersOrderJoiner,
                Joined.with(Serdes.String(),
                        (new SpecificAvroSerdeBuilder<shoe_customers>()).buildSerde(allProps),
                        (new SpecificAvroSerdeBuilder<shoe_orders>()).buildSerde(allProps)));

        KStream<String, shoe_orders_customers_products> customersOrdersProductsStream = customersOrdersStream.
                selectKey((orderID, order) -> order.getProductId()).
                leftJoin(productsTable, ordersCustomerProductJoiner,
                        Joined.with(Serdes.String(),
                                (new SpecificAvroSerdeBuilder<shoe_orders_customers>()).buildSerde(allProps),
                                (new SpecificAvroSerdeBuilder<shoe_product>()).buildSerde(allProps)
                        )
                );
        customersOrdersProductsStream.to(OUTPUT_TOPIC,
                Produced.with(Serdes.String(),
                        (new SpecificAvroSerdeBuilder<shoe_orders_customers_products>()).buildSerde(allProps)
                )
        );

        /* products stream leading */
        KTable<String, shoe_orders> ordersTableKeyProductId = ordersTable.groupBy(
                        (key, value) -> KeyValue.pair(value.getProductId(), value),
                        Grouped.with("orderByProductId", Serdes.String(),
                                (new SpecificAvroSerdeBuilder<shoe_orders>()).buildSerde(allProps))).
                reduce(
                        (aggValue, newValue) -> newValue,
                        (aggValue, oldValue) -> oldValue
                );
        KStream<String, shoe_orders_products> productsOrdersStream = productsTable.toStream().join(
                ordersTableKeyProductId,
                productsOrderJoiner,
                Joined.with(Serdes.String(),
                        (new SpecificAvroSerdeBuilder<shoe_product>()).buildSerde(allProps),
                        (new SpecificAvroSerdeBuilder<shoe_orders>()).buildSerde(allProps)));
        KStream<String, shoe_orders_customers_products> productsOrdersCustomersStream = productsOrdersStream.
                selectKey((orderID, order) -> order.getProduct().getId()).
                leftJoin(customersTable, productsOrderCustomerJoiner,
                        Joined.with(Serdes.String(),
                                (new SpecificAvroSerdeBuilder<shoe_orders_products>()).buildSerde(allProps),
                                (new SpecificAvroSerdeBuilder<shoe_customers>()).buildSerde(allProps)
                        )
                );
        productsOrdersCustomersStream.to(OUTPUT_TOPIC,
                Produced.with(Serdes.String(),
                        (new SpecificAvroSerdeBuilder<shoe_orders_customers_products>()).buildSerde(allProps)
                )
        );

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