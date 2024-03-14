package io.confluent.developer;

import org.apache.avro.specific.SpecificRecord;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.List;
import java.util.Properties;

public class GenericConsumerUpdateApp<T extends SpecificRecord> {



    public void execute(String topicName, String appId) throws IOException {
        final Properties properties = new Properties();
        try (InputStream inputStream = new FileInputStream(App.DEV_CONFIG_FILE)) {
            properties.load(inputStream);
        }
        properties.put(ConsumerConfig.GROUP_ID_CONFIG,appId);
        KafkaConsumer<String, T> consumer = new KafkaConsumer<>(properties);
        MongodbUpdater mongodbUpdater = new MongodbUpdater();

        try {
            consumer.subscribe(List.of(topicName));

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
                ConsumerRecords<String, T> records = consumer.poll(Duration.ofMillis(100));
                for (ConsumerRecord<String, T> record : records) {
                    mongodbUpdater.updateField(record.value());
                }
            }
        } catch (WakeupException e) {
            System.out.println("Wake up exception!");
            // we ignore this as this is an expected exception when closing a consumer
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            System.out.println("The consumer is now gracefully closed.");
            mongodbUpdater.close();
            consumer.close();
        }
    }

}
