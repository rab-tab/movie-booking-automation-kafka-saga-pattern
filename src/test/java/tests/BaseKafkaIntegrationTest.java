package tests;


import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.time.Duration;
import java.util.List;
import java.util.Properties;
import java.util.function.Function;

public abstract class BaseKafkaIntegrationTest {

    protected static final String BOOTSTRAP_SERVERS =
            System.getProperty("kafka.bootstrap", "localhost:9092");

    protected <T> KafkaProducer<String, T> createProducer() {

        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        props.put(ProducerConfig.ACKS_CONFIG, "all");

        return new KafkaProducer<>(props);
    }

    protected <T> Consumer<String, T> createConsumer(
            String groupId,
            Class<T> valueType,
            String topic
    ) {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);

        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        props.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, valueType.getName());

        KafkaConsumer<String, T> consumer =
                new KafkaConsumer<>(
                        props,
                        new StringDeserializer(),
                        new JsonDeserializer<>(valueType, false)
                );
        consumer.subscribe(List.of(topic));
        return consumer;
    }

    protected <T> T pollSingleRecord(
            Consumer<String, T> consumer,
            String topic,
            Duration timeout
    ) {
        return consumer
                .poll(timeout)
                .records(topic)
                .iterator()
                .next()
                .value();
    }

    // Utility: retry polling for asynchronous events
    /*protected <T> T pollSingleRecordWithRetry(Consumer<String, T> consumer, String topic, Duration timeout) {
        var records = consumer.poll(timeout).records(topic);
        var iterator = records.iterator();
        if (iterator.hasNext()) {
            return iterator.next().value();
        } else {
            throw new RuntimeException("No records found in topic: " + topic);
        }
    }*/

    protected <T> T pollSingleRecordWithRetry(Consumer<String, T> consumer, String topic, Duration timeout) throws InterruptedException {
        int retries = 5;
        while (retries-- > 0) {
            var records = consumer.poll(timeout).records(topic);
            var iterator = records.iterator();
            if (iterator.hasNext()) {
                return iterator.next().value();
            }
            Thread.sleep(1000);
        }
        throw new RuntimeException("No records found in topic: " + topic);
    }

    protected <T> T pollForBookingEvent(Consumer<String, T> consumer, String topic, Duration timeout, String expectedBookingId, Function<T, String> bookingIdExtractor) {
        long end = System.currentTimeMillis() + timeout.toMillis();

        while (System.currentTimeMillis() < end) {
            var records = consumer.poll(Duration.ofMillis(500));
            for (var record : records.records(topic)) {
                T event = record.value();
                if (expectedBookingId.equals(bookingIdExtractor.apply(event))) {
                    return event;
                }
            }
        }
        throw new RuntimeException(
                "No matching event found for bookingId=" + expectedBookingId
        );
    }


}
