# Anti-pattern: Sink from inside Kafka Streams

This is the baseline for the anti-pattern of using Kafka Streams to sink data into a database.
In this case Mongodb.

Once you have the 3 datagen source connectors running as per global README, you can run the Kafka Streams application.

Just run the io.confluent.developer.App class.

You should see on the mongo express interface on http://localhost:18081 with user/password admin/pass for the database 
demo, the new collection order getting populated with the joined data by the streams app.