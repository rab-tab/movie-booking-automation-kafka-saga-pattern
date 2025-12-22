package com.microservices.api.util;

import org.apache.kafka.clients.admin.*;
import org.apache.kafka.common.TopicPartition;

import java.util.Collections;
import java.util.Properties;
import java.util.stream.Collectors;

import org.apache.kafka.clients.admin.*;
import org.apache.kafka.common.TopicPartition;

import java.util.Collections;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

public class KafkaTestUtils {

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
}

