# Best Practice: Beyond Connect, Leverage Streams

This is the example following now not only the best practice for leveraging Kafka Connect to sink data externally to 
mongodb but also to use Streams as much as possible and not only use Tables so to optimize memory usage (state store 
minimization). 

Once you have the 3 datagen source connectors running as per global README, you can run the Kafka Streams application.

Just run the io.confluent.developer.App class.

You should see the new topic orders-enriched2 with the data from the orders topic enriched with the customer and product
data from the other topics.

Now we can configure the sink connector to sink the data to the mongodb database.

```bash
curl -i -X PUT -H "Accept:application/json" \
    -H  "Content-Type:application/json" http://localhost:8083/connectors/my-sink-mongodb2/config \
    -d '{
          "connector.class"    : "com.mongodb.kafka.connect.MongoSinkConnector",
          "connection.uri"     : "mongodb://root:example@mongo:27017",
          "topics"             : "orders-enriched2",
          "tasks.max"          : "1",
          "auto.create"        : "true",
          "auto.evolve"        : "true",
          "database"           : "demo",
          "value.converter.schema.registry.url": "http://schema-registry:8081",
          "key.converter"       : "org.apache.kafka.connect.storage.StringConverter",
          "value.converter"     : "io.confluent.connect.avro.AvroConverter",
          "mongodb.delete.on.null.values": "true",
          "delete.on.null.values": "true",
          "document.id.strategy.overwrite.existing": "true",
          "document.id.strategy": "com.mongodb.kafka.connect.sink.processor.id.strategy.ProvidedInKeyStrategy",
          "transforms":"copyIdToKey,replaceFieldInKey",
          "transforms.copyIdToKey.type" : "org.apache.kafka.connect.transforms.ValueToKey",
          "transforms.copyIdToKey.fields" : "order_id",
          "transforms.replaceFieldInKey.type": "org.apache.kafka.connect.transforms.ReplaceField$Key",
          "transforms.replaceFieldInKey.renames": "order_id:_id",
          "writemodel.strategy" : "com.mongodb.kafka.connect.sink.writemodel.strategy.ReplaceOneDefaultStrategy",
          "collection"          : "orders2"}'
```

Let's check the data in the mongodb database in http://localhost:18081 for the new collection orders2.

Now run the product and customer consumer apps which will update Mongodb directly in case of updates on the 
corresponding topics.
