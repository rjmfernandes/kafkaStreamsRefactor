# With Flink in Confluent Cloud

## Setup

Now we try to run the same example from Flink just as demonstration within Confluent Cloud (CC).
We run the instructions for deploying with terraform our setup in CC as here: https://github.com/rjmfernandes/flink (only till environment and cluster, topics and connectors are in place).

We have summarised on the following script:

```shell
git clone https://github.com/rjmfernandes/flink
cd flink
echo "Enter a prefix value as 'rfernandes_':"
read prefix_value
confluent login
CC_API_KEY_SECRET=`confluent api-key create --resource cloud --description "API for terraform"`
CC_API_KEY=`echo "$CC_API_KEY_SECRET"| grep 'API Key'|sed s/'.*| '//g|sed s/' .*'//g`
CC_API_SECRET=`echo "$CC_API_KEY_SECRET"| grep 'API Secret'|sed s/'.*| '//g|sed s/' .*'//g`
cat > $PWD/terraform/terraform.tfvars <<EOF
confluent_cloud_api_key = "$CC_API_KEY"
confluent_cloud_api_secret = "$CC_API_SECRET"
use_prefix = "$prefix_value"
EOF
cd ./terraform
terraform init -upgrade
terraform plan
terraform apply -auto-approve
cc_hands_env=`terraform output -json | jq -r .cc_hands_env.value`
cc_kafka_cluster=`terraform output -json | jq -r .cc_kafka_cluster.value`
CC_SR_KEY=`terraform output -json | jq -r .SRKey.value`
CC_SR_SECRET=`terraform output -json | jq -r .SRSecret.value`
cc_flink_pool=`terraform output -json | jq -r .FlinkComputePool.value`
cd ../..
confluent environment use $cc_hands_env
confluent flink compute-pool use $cc_flink_pool
```

Make sure to have a .env file with format and content only:

```
TAG=7.6.0
SR_URL=<<SR_URL>>
BOOTSTRAP_SERVERS=<<BOOTSTRAP_SERVERS>>
CLOUD_KEY=<<CLOUD_KEY>>
CLOUD_SECRET=<<CLOUD_SECRET>>
```

- You can get the SR_URL looking at the Stream Governance API for your environment.
- You can get the BOOTSTRAP_SERVERS and the API credentials by adding a new java client to your cluster.

Now you can open the sql console:

```shell
confluent flink shell --database $cc_kafka_cluster
```


## Tables with Primary Key

Let's create our tables with primary keys:

```
CREATE TABLE customers (
  customer_id STRING,
  first_name STRING,
  last_name STRING,
  email STRING,
  PRIMARY KEY (customer_id) NOT ENFORCED
  );
```

```
CREATE TABLE products (
  product_id STRING,
  brand STRING,
  name STRING,
  sale_price INT,
  rating DOUBLE,
  PRIMARY KEY (product_id) NOT ENFORCED
  );
```

```
CREATE TABLE orders (
  order_id INT,
  product_id STRING,
  customer_id STRING,
  ts TIMESTAMP(3),
  PRIMARY KEY (order_id) NOT ENFORCED
  );
```

After we create the jobs to populate our primary keyed tables (we will let these jobs running):

```
INSERT INTO customers
  SELECT id, first_name, last_name, email
    FROM shoe_customers;
```

```
INSERT INTO products
  SELECT id, brand, `name`, sale_price, rating 
    FROM shoe_products;
```

```
INSERT INTO orders
  SELECT order_id, product_id, customer_id, ts 
    FROM shoe_orders;
```

## Join Table

Let's create our join table:

```
CREATE TABLE order_customer_product (
  id INT,
  order_id INT,
  product MAP<STRING,STRING>,
  customer MAP<STRING,STRING>,
  ts TIMESTAMP(3),
  PRIMARY KEY (id) NOT ENFORCED
);
```

And for populating it we run our join job:

```
INSERT INTO order_customer_product(
  id, order_id, product, customer, ts
) 
SELECT 
  orders.order_id, 
  orders.order_id, 
  MAP[ 'id', 
  orders.product_id, 
  'brand', 
  products.brand, 
  'name', 
  products.name, 
  'sale_price', 
  CAST(products.sale_price AS varchar), 
  'rating', 
  CAST(products.rating AS varchar) ], 
  MAP[ 'id', 
  orders.customer_id, 
  'first_name', 
  customers.first_name, 
  'last_name', 
  customers.last_name, 
  'email', 
  customers.email], 
  orders.ts 
FROM 
  orders 
  INNER JOIN customers ON orders.customer_id = customers.customer_id 
  INNER JOIN products ON orders.product_id = products.product_id;
```

## Run Local Connect Instance Connected to CC

Run:

```bash
SR_KEY="${CC_SR_KEY}" SR_SECRET="${CC_SR_SECRET}" docker compose up -d
```

You can check the connector plugins available by executing:

```bash
curl localhost:8086/connector-plugins | jq
```

If you see you only have the 3 default source connectors run (if you have already the mongodb sink connector skip to **Configure Sink Connector to Mongodb**):

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
SR_KEY="${CC_SR_KEY}" SR_SECRET="${CC_SR_SECRET}" docker compose exec -it connect bash
```

Once inside the container we can install a new connector from confluent-hub:

```bash
confluent-hub install mongodb/kafka-connect-mongodb:latest
```
(Choose option 2 and after say yes to everything when prompted.)

Do the same for:

```bash
confluent-hub install confluentinc/connect-transforms:latest
```

Now we need to restart our connect:

```bash
SR_KEY="${CC_SR_KEY}" SR_SECRET="${CC_SR_SECRET}" docker compose restart connect
```

Now if we list our plugins again we should see two new ones corresponding to the Mongo connector.

## Configure Sink Connector to Mongodb

Make sure you have the main docker compose of the project with the mongodb running and database demo created.

Run (**replace SR_UL both times it appears**):

```bash
curl -i -X PUT -H "Accept:application/json" \
    -H  "Content-Type:application/json" http://localhost:8086/connectors/my-sink-mongodb/config \
    -d '{
          "connector.class"    : "com.mongodb.kafka.connect.MongoSinkConnector",
          "connection.uri"     : "mongodb://root:example@docker.for.mac.host.internal:27017",
          "topics"             : "order_customer_product",
          "tasks.max"          : "1",
          "auto.create"        : "true",
          "auto.evolve"        : "true",
          "database"           : "demo",
          "key.converter.schema.registry.url": "<SR_URL>",
          "key.converter.basic.auth.credentials.source": "USER_INFO",
          "key.converter.schema.registry.basic.auth.user.info": "'"$CC_SR_KEY"':'"$CC_SR_SECRET"'",
          "value.converter.schema.registry.url": "<SR_URL>",
          "value.converter.basic.auth.credentials.source": "USER_INFO",
          "value.converter.schema.registry.basic.auth.user.info": "'"$CC_SR_KEY"':'"$CC_SR_SECRET"'",
          "key.converter"       : "io.confluent.connect.avro.AvroConverter",
          "value.converter"     : "io.confluent.connect.avro.AvroConverter",
          "mongodb.delete.on.null.values": "true",
          "delete.on.null.values": "true",
          "document.id.strategy.overwrite.existing": "true",
          "document.id.strategy": "com.mongodb.kafka.connect.sink.processor.id.strategy.ProvidedInKeyStrategy",
          "transforms":"replaceFieldInKey",
          "transforms.replaceFieldInKey.type": "org.apache.kafka.connect.transforms.ReplaceField$Key",
          "transforms.replaceFieldInKey.renames": "id:_id",
          "writemodel.strategy" : "com.mongodb.kafka.connect.sink.writemodel.strategy.ReplaceOneDefaultStrategy",
          "collection"          : "orders6"}'
```

Let's check the data in the mongodb database in http://localhost:18081 for the new collection orders6.

By the way our connector could also be a fully managed connector running in Confluent Cloud.

Not sure if you notice but there is no code (except for SQL) if we implement with CC Flink and Kafka Connect...

## Stop Local Connect

```shell
SR_KEY="${CC_SR_KEY}" SR_SECRET="${CC_SR_SECRET}" docker compose down -v
```

## Destroy CC environment

You may need to execute more than once to fully clean up in case of errors clenaing some of the resources:

```bash
cd flink/terraform
terraform destroy -auto-approve
cd ../..
```
