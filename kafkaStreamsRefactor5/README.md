# Custom SMT for using only a single connector with minimal KTables joins refactoring

This project just creates the jar deployed for the custom SMT RemoveNullFields in order to avoid having to use many sink 
connectors (one per table) for the mongodb sink connector in the last refactoring.

If you want to use this example just call the creation of the sink connector as follows 
(after executing the last refactoring for minimal ktables parallel joins): 

```bash
curl -i -X PUT -H "Accept:application/json" \
    -H  "Content-Type:application/json" http://localhost:8083/connectors/my-sink-mongodb5/config \
    -d '{
          "connector.class"    : "com.mongodb.kafka.connect.MongoSinkConnector",
          "connection.uri"     : "mongodb://root:example@mongo:27017",
          "topics"             : "orders-enriched4",
          "tasks.max"          : "1",
          "auto.create"        : "true",
          "auto.evolve"        : "true",
          "database"           : "demo",
          "value.converter.schema.registry.url": "http://schema-registry:8081",
          "value.converter.schemas.enable":"false",
          "key.converter"       : "org.apache.kafka.connect.storage.StringConverter",
          "value.converter"     : "io.confluent.connect.avro.AvroConverter",
          "mongodb.delete.on.null.values": "true",
          "delete.on.null.values": "true",
          "document.id.strategy.overwrite.existing": "true",
          "document.id.strategy": "com.mongodb.kafka.connect.sink.processor.id.strategy.ProvidedInKeyStrategy",
          "transforms":"copyIdToKey,replaceFieldInKey,removeNullFields",
          "transforms.copyIdToKey.type" : "org.apache.kafka.connect.transforms.ValueToKey",
          "transforms.copyIdToKey.fields" : "order_id",
          "transforms.replaceFieldInKey.type": "org.apache.kafka.connect.transforms.ReplaceField$Key",
          "transforms.replaceFieldInKey.renames": "order_id:_id",
          "transforms.removeNullFields.type": "io.confluent.developer.transforms.RemoveNullFields",
          "writemodel.strategy" : "com.mongodb.kafka.connect.sink.writemodel.strategy.UpdateOneDefaultStrategy",
          "collection"          : "orders5"}'
```

Let's check the data in the mongodb database in http://localhost:18081 for the new collection orders5.