package io.confluent.developer;

import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KTable;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.state.KeyValueStore;
import shoes.shoe_customers;
import shoes.shoe_orders;
import shoes.shoe_orders_customers;
import shoes.shoe_product;
import shoes.shoe_orders_customers_products;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
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

    public Topology buildTopology(Properties allProps,
                                  final SpecificAvroSerde<shoe_orders> ordersSerde,
                                  final SpecificAvroSerde<shoe_customers> customersSerde,
                                  final SpecificAvroSerde<shoe_product> productsSerde,
                                  final SpecificAvroSerde<shoe_orders_customers> ordersCustomersSerde) {
        final StreamsBuilder builder = new StreamsBuilder();

        final String ordersTopic = allProps.getProperty("orders.topic.name");
        final String customersTopic = allProps.getProperty("customers.topic.name");
        final String productsTopic = allProps.getProperty("products.topic.name");

        KTable<String, shoe_orders> ordersTable = builder.table(ordersTopic, Consumed.with(Serdes.String(),
                ordersSerde));
        KTable<String, shoe_customers> customersTable = builder.table(customersTopic, Consumed.with(Serdes.String(),
                customersSerde));
        KTable<String, shoe_product> productsTable = builder.table(productsTopic, Consumed.with(Serdes.String(),
                productsSerde));

        OrdersCustomerJoiner ordersCustomerJoiner = new OrdersCustomerJoiner();
        OrdersCustomerProductJoiner ordersCustomerProductJoiner = new OrdersCustomerProductJoiner();
        MongodbUpsert mongodbUpsert = new MongodbUpsert();

        KTable<String,shoe_orders_customers> ordersCustomersTable= ordersTable.leftJoin(customersTable,
                shoe_orders::getCustomerId,ordersCustomerJoiner,
                Materialized.<String, shoe_orders_customers, KeyValueStore<Bytes, byte[]>>as(
                        "orders-customers-store") /* state store name */
                        .withValueSerde(ordersCustomersSerde)
                );

        KTable<String,shoe_orders_customers_products> ordersCustomersProductsTable=
                ordersCustomersTable.leftJoin(productsTable, shoe_orders_customers::getProductId,
                                ordersCustomerProductJoiner);

        ordersCustomersProductsTable.toStream().peek((k,v)->mongodbUpsert.upsert(v));

        return builder.build();
    }


    public static Properties loadEnvProperties(String fileName) throws IOException {
        Properties allProps = new Properties();
        FileInputStream input = new FileInputStream(fileName);
        allProps.load(input);
        input.close();

        return allProps;
    }

    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            args = new String[]{DEV_CONFIG_FILE};
        }

        new App().runRecipe(args[0]);
    }

    private void runRecipe(final String configPath) throws IOException {
        final Properties allProps = new Properties();
        try (InputStream inputStream = new FileInputStream(configPath)) {
            allProps.load(inputStream);
        }
        allProps.put(StreamsConfig.APPLICATION_ID_CONFIG, allProps.getProperty("application.id"));

        final Topology topology = this.buildTopology(allProps, this.buildOrdersSerde(allProps),
                this.buildCustomersSerde(allProps),
                this.buildProductsSerde(allProps),
                this.buildOrdersCustomersSerde(allProps));

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