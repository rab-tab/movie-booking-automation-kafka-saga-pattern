package com.microservices.api.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microservices.api.model.events.BookingCreatedEvent;
import com.microservices.api.tests.BaseKafkaIntegrationTest;
import org.apache.kafka.clients.admin.*;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import org.apache.kafka.clients.admin.*;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.Collections;
import java.util.Properties;
import java.util.stream.Collectors;

public class KafkaTestUtils {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static KafkaTemplate<String, Object> kafkaTemplate;

    static {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        props.put(JsonSerializer.ADD_TYPE_INFO_HEADERS, true);

        ProducerFactory<String, Object> producerFactory =
                new DefaultKafkaProducerFactory<>(props);

        kafkaTemplate = new KafkaTemplate<>(producerFactory);
    }


    public static void deleteTopicRecords(String bootstrapServers, String topic) throws Exception {
        Properties props = new Properties();
        props.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);

        try (AdminClient admin = AdminClient.create(props)) {

            // Get partitions for topic
            var partitions = admin.describeTopics(Collections.singletonList(topic))
                    .all()
                    .get()
                    .get(topic)
                    .partitions();

            // Build map of TopicPartition -> RecordsToDelete using actual end offsets
            Map<TopicPartition, RecordsToDelete> deleteMap = partitions.stream()
                    .map(p -> new TopicPartition(topic, p.partition()))
                    .collect(Collectors.toMap(
                            tp -> tp,
                            tp -> {
                                try {
                                    Map<TopicPartition, ListOffsetsResult.ListOffsetsResultInfo> offsets =
                                            admin.listOffsets(Collections.singletonMap(tp, OffsetSpec.latest()))
                                                    .all()
                                                    .get();
                                    long endOffset = offsets.get(tp).offset();
                                    return RecordsToDelete.beforeOffset(endOffset);
                                } catch (Exception e) {
                                    throw new RuntimeException(e);
                                }
                            }
                    ));

            // Delete messages
            admin.deleteRecords(deleteMap).all().get();
        }
    }

    /**
     * Publishes an event to a Kafka topic using a local producer.
     *
     * @param bootstrapServers Kafka bootstrap server (e.g., "localhost:9092")
     * @param topic            Kafka topic to publish to
     * @param key              Kafka key (e.g., bookingId)
     * @param event            Event object to send
     */
   /* public static <T> void publishEvent(String bootstrapServers, String topic, String key, T event) {
        Properties props = new Properties();
        props.put("bootstrap.servers", bootstrapServers);
        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("acks", "all"); // wait for all replicas

        try (KafkaProducer<String, String> producer = new KafkaProducer<>(props)) {
            String value = objectMapper.writeValueAsString(event);
            ProducerRecord<String, String> record = new ProducerRecord<>(topic, key, value);
            producer.send(record).get(); // synchronous send for test
        } catch (Exception e) {
            throw new RuntimeException("Failed to publish Kafka event: " + e.getMessage(), e);
        }
    }*/

   /* public static <T> void publishEvent(
            String bootstrapServers,
            String topic,
            String key,
            T event
    ) {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);

        // 🔑 CRITICAL
        props.put(JsonSerializer.ADD_TYPE_INFO_HEADERS, true);

        ProducerFactory<String, T> producerFactory =
                new DefaultKafkaProducerFactory<>(props);

        KafkaTemplate<String, T> kafkaTemplate =
                new KafkaTemplate<>(producerFactory);

        kafkaTemplate.send(topic, key, event);
        kafkaTemplate.flush();
    }*/

    public static void createTopicIfNotExists(String bootstrapServers, String topicName, int numPartitions, short replicationFactor) throws ExecutionException, InterruptedException, ExecutionException {
        Properties props = new Properties();
        props.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);

        try (AdminClient adminClient = AdminClient.create(props)) {
            // Check if topic exists
            if (!adminClient.listTopics().names().get().contains(topicName)) {
                NewTopic newTopic = new NewTopic(topicName, numPartitions, replicationFactor);
                adminClient.createTopics(Collections.singleton(newTopic)).all().get();
                System.out.println("Topic created: " + topicName);
            } else {
                System.out.println("Topic already exists: " + topicName);
            }
        }
    }

    public static ConsumerRecord<String, BookingCreatedEvent>
    waitForBookingCreatedEventInDLT(String bookingId) {

        Consumer<String, BookingCreatedEvent> consumer =
                BaseKafkaIntegrationTest.createConsumer(
                        "dlt-test-group",
                        BookingCreatedEvent.class,
                        "movie-booking-events-dlt"
                );

        long end = System.currentTimeMillis() + 30_000;

        try {
            while (System.currentTimeMillis() < end) {
                ConsumerRecords<String, BookingCreatedEvent> records =
                        consumer.poll(Duration.ofMillis(1000));

                for (ConsumerRecord<String, BookingCreatedEvent> record : records) {
                    BookingCreatedEvent event = record.value();

                    if (event != null &&
                            bookingId.equals(event.getBookingId())) {
                        return record;
                    }
                }
            }
        } finally {
            consumer.close();
        }

        return null;
    }


    public static void publishEvent(String topic, String key, Object event) {
        kafkaTemplate.send(topic, key, event);
        kafkaTemplate.flush();
    }

    public static void shutdown() {
        kafkaTemplate.destroy();
    }

}

