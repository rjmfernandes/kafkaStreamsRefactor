# Kafka Streams Reffactor Demo

Kafka Streams Reffactor demo including Mongodb.

The use case here is a join between multiple reference tables to be consolidated in an external database (mongodb). We are using for example purposes here 3 data sources (topics): orders, customers, products. But in a real scenario this would be potentially many more. Also the choice of orders here doesn't really match a real case scenario of a reference table but is used only for demonstration purposes and meaning only the most frequently updated table driving in general the joins for consolidation. 

## Setup

### Start Docker Compose

```bash
docker compose up -d
```

### Mongodb

You can access the mongo express interface on http://localhost:18081 with user/password admin/pass. Create database named demo.

### Connect

You can check the connector plugins available by executing:

```bash
curl localhost:8083/connector-plugins | jq
```

As you see we only have source connectors:

```text
[
  {
    "class": "org.apache.kafka.connect.mirror.MirrorCheckpointConnector",
    "type": "source",
    "version": "7.6.0-ce"
  },
  {
    "class": "org.apache.kafka.connect.mirror.MirrorHeartbeatConnector",
    "type": "source",
    "version": "7.6.0-ce"
  },
  {
    "class": "org.apache.kafka.connect.mirror.MirrorSourceConnector",
    "type": "source",
    "version": "7.6.0-ce"
  }
]
```

Let's install mongodb/kafka-connect-mongodb connector plugin for sink.

For that we will open a shell into our connect container:

```bash
docker compose exec -it connect bash
```

Once inside the container we can install a new connector from confluent-hub:

```bash
confluent-hub install confluentinc/kafka-connect-datagen:latest
```

(Choose option 2 and after say yes to everything when prompted.)
Do the same for:

```bash
confluent-hub install mongodb/kafka-connect-mongodb:latest
```

And for:

```bash
confluent-hub install confluentinc/connect-transforms:latest
```

Now we need to restart our connect:

```bash
docker compose restart connect
```

Now if we list our plugins again we should see three new ones corresponding to the Mongo connector and Datagen.

Let's create our source connector using datagen:

```bash
curl -i -X PUT -H "Accept:application/json" -H  "Content-Type:application/json" http://localhost:8083/connectors/my-datagen-source1/config -d '{
    "name" : "my-datagen-source1",
    "connector.class": "io.confluent.kafka.connect.datagen.DatagenConnector",
    "kafka.topic" : "products",
    "output.data.format" : "AVRO",
    "quickstart" : "SHOES",
    "tasks.max" : "1"
}'
curl -i -X PUT -H "Accept:application/json" -H  "Content-Type:application/json" http://localhost:8083/connectors/my-datagen-source2/config -d '{
    "name" : "my-datagen-source2",
    "connector.class": "io.confluent.kafka.connect.datagen.DatagenConnector",
    "kafka.topic" : "customers",
    "output.data.format" : "AVRO",
    "quickstart" : "SHOE_CUSTOMERS",
    "tasks.max" : "1"
}'
curl -i -X PUT -H "Accept:application/json" -H  "Content-Type:application/json" http://localhost:8083/connectors/my-datagen-source3/config -d '{
    "name" : "my-datagen-source3",
    "connector.class": "io.confluent.kafka.connect.datagen.DatagenConnector",
    "kafka.topic" : "orders",
    "output.data.format" : "AVRO",
    "quickstart" : "SHOE_ORDERS",
    "tasks.max" : "1"
}'
```

### Check Control Center

Open http://localhost:9021 and check cluster is healthy including Kafka Connect.

## Execute the examples

You can go to [Anti-pattern example](./kafkaStreamsRefactor0/README.md) and the [Best-practice example](./kafkaStreamsRefactor1/README.md) and execute both in parallel.

Both are capable to send data to Mongodb but one is more resilient than the other. If we stop the mongo database:

```bash
docker compose stop mongo
```

And wait a minute we will see the [Anti-pattern example](./kafkaStreamsRefactor0/README.md) entering into error while our other app [Best-practice example](./kafkaStreamsRefactor1/README.md) keeps executing with no issues. We can see though that the corresponding connector entered into error state. If we now restart the mongo database:

```bash
docker compose start mongo
```

And restart our connector it will pick from the point it left and continue the sink to mongo database (while meanwhile our kafka streams continued processing with no pause). Our other app would need to be restarted or some handling added to the app to be able to restart processing.

Also if we compare the codebase of both apps the one following best practices is smaller since it doesnt have to handle any of the external communication to mongodb. Encapsulated in the configuration driven connector. Which as we saw is much easier to manage and isolate in case of errors communicating to mongo.

Also take into account the latency added by communicating to the external system is minimised since we are testing on localhost in a real scenario would be much bigger. And the optimization of this comunnication for the upserts is already optimised within our sink connector if we follow best practices.

Also managing and monitoring our system and isolating issues is much easier when leveraging Kafka Connect besides also giving us the chance for configuring important points as dead letter queues, etc.

## Second Refactor Example - leverages KStream and Consumer Apps 

The second refactored example [KStreams example](./kafkaStreamsRefactor2/README.md) optimizes state storage usage by leveraging KStreams for orders and tables for reference tables only. It also runs consumers for products and customers topics to update all joined elements in case of updates in those topics. Adding on top of the benefits before the optimization on resources usage for implementing our solution and avoid state store memory explosion.

## Third Refactor Example - leverages many KStreams (but no consumer apps) 

The third example [Only KStreams example](./kafkaStreamsRefactor3/README.md) avoids any extra consumer apps and solves the problem with the Kafka Streams app only but leveraging KStreams for the joins and avoid the state store memory explosion. The major issue is that the code starts to become harder to maintain.

## Fourth Refactor - leverages only KTables but in parallel and not sequential with minimal partial sinks in parallel connectors

The fourth example [Optimize to minimal table joins](./kafkaStreamsRefactor4/README.md) does everything with parallel KTables and non need for consumer apps again and using parallel connectors for sinking the minimal joins.

## Fifth Refactor - leverage custom SMT

The fifth example [Custom SMT for using only a single connector with minimal KTables joins refactoring](./kafkaStreamsRefactor5/README.md) is basically the same as the one before but now in place of using many (3) connectors we use just one leveraging a custom SMT to remove nulls so that the partial joins sinks don't overwrite each other.

## Final and Extra - With Flink in Confluent Cloud

Finally we show how to implement the same with no code just SQL leveraging Confluent Cloud Flink [Extra - With Flink in Confluent Cloud](https://github.com/rjmfernandes/kafkaStreamsRefactor/tree/main/Flink6).

## Cleanup

```bash
docker compose down -v
```