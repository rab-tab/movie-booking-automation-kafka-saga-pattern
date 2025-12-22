package com.microservices.api.consumers;

import com.microservices.api.model.events.BookingCreatedEvent;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.utils.KafkaTestUtils;

import java.util.Map;

public class KafkaTestConsumerFactory {

    private final EmbeddedKafkaBroker embeddedKafkaBroker;

    public KafkaTestConsumerFactory(EmbeddedKafkaBroker embeddedKafkaBroker) {
        this.embeddedKafkaBroker = embeddedKafkaBroker;
    }

    public <T> Consumer<String, T> createConsumer(
            String topic,
            Class<T> eventClass,
            String groupId
    ) {

        Map<String, Object> props = KafkaTestUtils.consumerProps(
                groupId,
                "true",
                embeddedKafkaBroker
        );

        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, eventClass.getName());

        ConsumerFactory<String, T> consumerFactory =
                new DefaultKafkaConsumerFactory<>(
                        props,
                        new StringDeserializer(),
                        new JsonDeserializer<>(eventClass)
                );

        Consumer<String, T> consumer = consumerFactory.createConsumer();
        embeddedKafkaBroker.consumeFromAnEmbeddedTopic(consumer, topic);

        return consumer;
    }
}
