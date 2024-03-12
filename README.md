# Kafka Streams Reffactor Demo

Kafka Streams Reffactor demo including Mongodb.

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
    "class": "io.confluent.kafka.connect.datagen.DatagenConnector",
    "type": "source",
    "version": "null"
  },
  {
    "class": "org.apache.kafka.connect.mirror.MirrorCheckpointConnector",
    "type": "source",
    "version": "7.5.0-ce"
  },
  {
    "class": "org.apache.kafka.connect.mirror.MirrorHeartbeatConnector",
    "type": "source",
    "version": "7.5.0-ce"
  },
  {
    "class": "org.apache.kafka.connect.mirror.MirrorSourceConnector",
    "type": "source",
    "version": "7.5.0-ce"
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
confluent-hub install mongodb/kafka-connect-mongodb:latest
```

(Choose option 2 and after say yes to everything when prompted.)

Now we need to restart our connect:

```bash
docker compose restart connect
```

Now if we list our plugins again we should see two new ones corresponding to the Mongo connector.

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

## Cleanup

```bash
docker compose down -v
```