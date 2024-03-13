package io.confluent.developer;

import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.errors.WakeupException;
import shoes.shoe_customers;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Properties;

public class CustomerConsumer {

    public static void main(String[] args) throws IOException {
        Properties properties = Config.loadEnvProperties();
        properties.put(ConsumerConfig.GROUP_ID_CONFIG,properties.getProperty("prefix.id")+"Customers");
        KafkaConsumer<String, shoe_customers> consumer = new KafkaConsumer<>(properties);
        MongodbUpsert mongodbUpsert = new MongodbUpsert();

        try {
            consumer.subscribe(List.of(properties.getProperty("customers.topic.name")));

            final Thread mainThread = Thread.currentThread();

            // adding the shutdown hook
            Runtime.getRuntime().addShutdownHook(new Thread() {
                public void run() {
                    System.out.println("Detected a shutdown, let's exit by calling consumer.wakeup()...");
                    consumer.wakeup();

                    // join the main thread to allow the execution of the code in the main thread
                    try {
                        mainThread.join();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            });

            while (true) {
                ConsumerRecords<String, shoe_customers> records = consumer.poll(Duration.ofMillis(100));
                for (ConsumerRecord<String, shoe_customers> record : records) {
                    mongodbUpsert.upsertCustomer(record.value());
                }
            }
        } catch (WakeupException e) {
            System.out.println("Wake up exception!");
            // we ignore this as this is an expected exception when closing a consumer
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            System.out.println("The consumer is now gracefully closed.");
            mongodbUpsert.close();
            consumer.close();
        }
    }
}
