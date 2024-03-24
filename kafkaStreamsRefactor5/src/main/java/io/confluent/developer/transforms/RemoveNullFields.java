package io.confluent.developer.transforms;

import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.connect.connector.ConnectRecord;
import org.apache.kafka.connect.data.Field;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.transforms.Transformation;
import org.apache.kafka.connect.transforms.util.SimpleConfig;

import java.util.HashMap;
import java.util.Map;

import static org.apache.kafka.connect.transforms.util.Requirements.requireMap;

/********************************************************************************
 * Based on original code https://github.com/jobteaser-oss/kafka-connect-transforms/blob/master/src/main/java/com/jobteaser/kafka/connect/transforms/RemoveNulls.java
 * Which was problematic because it was not removing nulls, but 0x00 bytes, and it was not working with nested structures and schemas.
 *
 * The original code was licensed under the Apache 2.0 License.
 * @param <R>
 */
public class RemoveNullFields<R extends ConnectRecord<R>> implements Transformation<R> {

    public static final String OVERVIEW_DOC = "Remove nulls (0x00).";

    interface ConfigName {}

    public static final ConfigDef CONFIG_DEF = new ConfigDef();

    private static final String PURPOSE = "nulls removal";

    @Override
    public void configure(Map<String, ?> configs) {
        final SimpleConfig config = new SimpleConfig(CONFIG_DEF, configs);
    }

    @Override
    public ConfigDef config() {
        return CONFIG_DEF;
    }

    @Override
    public R apply(R record) {
        final Object updatedValue = updateDependendingOnSchema(record.valueSchema(), record.value());

        return record.newRecord(record.topic(), record.kafkaPartition(), record.keySchema(), record.key(), record.valueSchema(), updatedValue, record.timestamp());
    }

    private Object updateDependendingOnSchema(Schema schema, Object recordPart) {
        if (schema == null) {
            return update(recordPart);
        } else {
            return updateStruct((Struct) recordPart);
        }
    }

    private Map<String, Object> update(Object recordPart) {
        final Map<String, Object> value = requireMap(recordPart, PURPOSE);

        final Map<String, Object> updatedValue = new HashMap<>(value.size());

        for (Map.Entry<String, Object> e : value.entrySet()) {
            final String fieldName = e.getKey();
            final Object fieldContent = e.getValue();
            if(fieldContent!=null){
                updatedValue.put(fieldName, fieldContent);
            }
        }

        return updatedValue;
    }

    private Map updateStruct(Struct value) {
        Map<String, Object> updatedValue = new HashMap<>(value.schema().fields().size());
        for (Field field : value.schema().fields()) {
            final String fieldName = field.name();
            Object fieldContent = value.get(fieldName);
            if(fieldContent instanceof Struct) {
                fieldContent = updateStruct((Struct)fieldContent);
            }
            if(fieldContent!=null) {
                updatedValue.put(fieldName, fieldContent);
            }
        }
        return updatedValue;
    }

    @Override
    public void close() {}
}
