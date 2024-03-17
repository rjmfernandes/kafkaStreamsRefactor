# Optimize to minimal table joins

This follows from the first refactoring, but we avoid the state memory explosion by using partial joins.

So now each reference table join happens in parallel and not sequentially and only involves the minimal part of the main 
leading table that includes the refence for the join. We also generate partial sinks from the main topic table without 
reference table pointers to trigger update on main table fields. The connector now needs to be configured to leverage 
partial updates on the mongodb database.  

Once you have the 3 datagen source connectors running as per global README, you can run the Kafka Streams application.

Just run the io.confluent.developer.App class.

You should see the new topic orders-enriched4 with the data from the orders topic enriched with the customer 
and product data but each time separately with null values for the orther fields.

Now we can configure the sink connectors to sink the data to the mongodb database.

```bash
curl -i -X PUT -H "Accept:application/json" \
    -H  "Content-Type:application/json" http://localhost:8083/connectors/my-sink-mongodb41/config \
    -d '{
          "connector.class"    : "com.mongodb.kafka.connect.MongoSinkConnector",
          "connection.uri"     : "mongodb://root:example@mongo:27017",
          "topics"             : "orders-enriched4",
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
          "transforms":"filterOutEmptyTS,RemoveProduct,RemoveCustomer,copyIdToKey,replaceFieldInKey",
          "transforms.filterOutEmptyTS.type": "io.confluent.connect.transforms.Filter$Value",
          "transforms.filterOutEmptyTS.filter.condition": "$.[?(@.ts)]",
          "transforms.filterOutEmptyTS.filter.type": "include",
          "transforms.filterOutEmptyTS.missing.or.null.behavior": "exclude",
          "transforms.RemoveProduct.type": "org.apache.kafka.connect.transforms.ReplaceField$Value",
          "transforms.RemoveProduct.exclude": "product",
          "transforms.RemoveCustomer.type": "org.apache.kafka.connect.transforms.ReplaceField$Value",
          "transforms.RemoveCustomer.exclude": "customer",
          "transforms.copyIdToKey.type" : "org.apache.kafka.connect.transforms.ValueToKey",
          "transforms.copyIdToKey.fields" : "order_id",
          "transforms.replaceFieldInKey.type": "org.apache.kafka.connect.transforms.ReplaceField$Key",
          "transforms.replaceFieldInKey.renames": "order_id:_id",
          "writemodel.strategy" : "com.mongodb.kafka.connect.sink.writemodel.strategy.UpdateOneDefaultStrategy",
          "collection"          : "orders4"}'
curl -i -X PUT -H "Accept:application/json" \
    -H  "Content-Type:application/json" http://localhost:8083/connectors/my-sink-mongodb42/config \
    -d '{
          "connector.class"    : "com.mongodb.kafka.connect.MongoSinkConnector",
          "connection.uri"     : "mongodb://root:example@mongo:27017",
          "topics"             : "orders-enriched4",
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
          "transforms":"filterOutEmptyProduct,RemoveTS,RemoveCustomer,copyIdToKey,replaceFieldInKey",
          "transforms.filterOutEmptyProduct.type": "io.confluent.connect.transforms.Filter$Value",
          "transforms.filterOutEmptyProduct.filter.condition": "$.[?(@.product)]",
          "transforms.filterOutEmptyProduct.filter.type": "include",
          "transforms.filterOutEmptyProduct.missing.or.null.behavior": "exclude",
          "transforms.RemoveTS.type": "org.apache.kafka.connect.transforms.ReplaceField$Value",
          "transforms.RemoveTS.exclude": "ts",
          "transforms.RemoveCustomer.type": "org.apache.kafka.connect.transforms.ReplaceField$Value",
          "transforms.RemoveCustomer.exclude": "customer",
          "transforms.copyIdToKey.type" : "org.apache.kafka.connect.transforms.ValueToKey",
          "transforms.copyIdToKey.fields" : "order_id",
          "transforms.replaceFieldInKey.type": "org.apache.kafka.connect.transforms.ReplaceField$Key",
          "transforms.replaceFieldInKey.renames": "order_id:_id",
          "writemodel.strategy" : "com.mongodb.kafka.connect.sink.writemodel.strategy.UpdateOneDefaultStrategy",
          "collection"          : "orders4"}'
curl -i -X PUT -H "Accept:application/json" \
    -H  "Content-Type:application/json" http://localhost:8083/connectors/my-sink-mongodb43/config \
    -d '{
          "connector.class"    : "com.mongodb.kafka.connect.MongoSinkConnector",
          "connection.uri"     : "mongodb://root:example@mongo:27017",
          "topics"             : "orders-enriched4",
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
          "transforms":"filterOutEmptyCustomer,RemoveTS,RemoveProduct,copyIdToKey,replaceFieldInKey",
          "transforms.filterOutEmptyCustomer.type": "io.confluent.connect.transforms.Filter$Value",
          "transforms.filterOutEmptyCustomer.filter.condition": "$.[?(@.customer)]",
          "transforms.filterOutEmptyCustomer.filter.type": "include",
          "transforms.filterOutEmptyCustomer.missing.or.null.behavior": "exclude",
          "transforms.RemoveTS.type": "org.apache.kafka.connect.transforms.ReplaceField$Value",
          "transforms.RemoveTS.exclude": "ts",
          "transforms.RemoveProduct.type": "org.apache.kafka.connect.transforms.ReplaceField$Value",
          "transforms.RemoveProduct.exclude": "product",
          "transforms.copyIdToKey.type" : "org.apache.kafka.connect.transforms.ValueToKey",
          "transforms.copyIdToKey.fields" : "order_id",
          "transforms.replaceFieldInKey.type": "org.apache.kafka.connect.transforms.ReplaceField$Key",
          "transforms.replaceFieldInKey.renames": "order_id:_id",
          "writemodel.strategy" : "com.mongodb.kafka.connect.sink.writemodel.strategy.UpdateOneDefaultStrategy",
          "collection"          : "orders4"}'
```

Let's check the data in the mongodb database in http://localhost:18081 for the new collection orders4.