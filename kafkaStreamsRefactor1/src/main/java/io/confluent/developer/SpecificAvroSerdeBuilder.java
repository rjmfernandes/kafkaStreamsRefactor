package io.confluent.developer;

import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde;
import org.apache.avro.specific.SpecificRecord;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.kstream.Consumed;

import java.util.Map;
import java.util.Properties;

public class SpecificAvroSerdeBuilder<T extends SpecificRecord> {

    public SpecificAvroSerde<T> buildSerde(Properties allProps) {
        final SpecificAvroSerde<T> serde = new SpecificAvroSerde<>();
        final Map<String, String> config = (Map) allProps;
        serde.configure(config, false);
        return serde;
    }

    public Consumed<String, T> buildConsumed(Properties allProps) {
        return Consumed.with(Serdes.String(), buildSerde(allProps));
    }
}
