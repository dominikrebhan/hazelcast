/*
 * Copyright 2025 Hazelcast Inc.
 *
 * Licensed under the Hazelcast Community License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://hazelcast.com/hazelcast-community-license
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hazelcast.jet.kafka;

import com.hazelcast.function.FunctionEx;
import com.hazelcast.internal.util.Preconditions;
import com.hazelcast.jet.core.EventTimePolicy;
import com.hazelcast.jet.core.ProcessorMetaSupplier;
import com.hazelcast.jet.kafka.impl.StreamKafkaP;
import com.hazelcast.jet.kafka.impl.WriteKafkaP;
import com.hazelcast.jet.pipeline.DataConnectionRef;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.ProducerRecord;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.Properties;

import static com.hazelcast.jet.kafka.impl.StreamKafkaP.PREFERRED_LOCAL_PARALLELISM;

/**
 * Static utility class with factories of Apache Kafka source and sink
 * processors.
 *
 * @since Jet 3.0
 */
public final class KafkaProcessors {

    private KafkaProcessors() {
    }

    /**
     * Returns a supplier of processors for {@link
     * KafkaSources#kafka(Properties, FunctionEx, String...)}.
     */
    public static <K, V, T> ProcessorMetaSupplier streamKafkaP(
            @Nonnull Properties properties,
            @Nonnull FunctionEx<? super ConsumerRecord<K, V>, ? extends T> projectionFn,
            @Nonnull EventTimePolicy<? super T> eventTimePolicy,
            @Nonnull String... topics
    ) {
        Preconditions.checkPositive(topics.length, "At least one topic must be supplied");
        TopicsConfig topicsConfig = new TopicsConfig().addTopics(Arrays.asList(topics));
        return ProcessorMetaSupplier.of(
                PREFERRED_LOCAL_PARALLELISM,
                StreamKafkaP.processorSupplier(
                        c -> new KafkaConsumer<>(properties),
                        topicsConfig,
                        projectionFn,
                        eventTimePolicy
                )
        );
    }

    /**
     * Returns a supplier of processors for {@link
     * KafkaSources#kafka(Properties, FunctionEx, TopicsConfig)}}.
     *
     * @since 5.3
     */
    public static <K, V, T> ProcessorMetaSupplier streamKafkaP(
            @Nonnull Properties properties,
            @Nonnull FunctionEx<? super ConsumerRecord<K, V>, ? extends T> projectionFn,
            @Nonnull EventTimePolicy<? super T> eventTimePolicy,
            @Nonnull TopicsConfig topicsConfig
    ) {
        Preconditions.checkPositive(topicsConfig.getTopicNames().size(), "At least one topic must be supplied");
        return ProcessorMetaSupplier.of(
                PREFERRED_LOCAL_PARALLELISM,
                StreamKafkaP.processorSupplier(
                        c -> new KafkaConsumer<>(properties),
                        topicsConfig,
                        projectionFn,
                        eventTimePolicy
                )
        );
    }

    /**
     * Returns a supplier of processors for {@link
     * KafkaSources#kafka(DataConnectionRef, FunctionEx, String...)}.
     */
    public static <K, V, T> ProcessorMetaSupplier streamKafkaP(
            @Nonnull DataConnectionRef dataConnectionRef,
            @Nonnull FunctionEx<? super ConsumerRecord<K, V>, ? extends T> projectionFn,
            @Nonnull EventTimePolicy<? super T> eventTimePolicy,
            @Nonnull String... topics
    ) {
        Preconditions.checkPositive(topics.length, "At least one topic must be supplied");
        TopicsConfig topicsConfig = new TopicsConfig().addTopics(Arrays.asList(topics));
        return ProcessorMetaSupplier.of(
                PREFERRED_LOCAL_PARALLELISM,
                StreamKafkaP.processorSupplier(
                        StreamKafkaP.kafkaConsumerFn(dataConnectionRef),
                        topicsConfig,
                        projectionFn,
                        eventTimePolicy
                )
        );
    }

    /**
     * Returns a supplier of processors for
     * {@link KafkaSinks#kafka(Properties, String, FunctionEx, FunctionEx)}.
     */
    public static <T, K, V> ProcessorMetaSupplier writeKafkaP(
            @Nonnull Properties properties,
            @Nonnull String topic,
            @Nonnull FunctionEx<? super T, ? extends K> extractKeyFn,
            @Nonnull FunctionEx<? super T, ? extends V> extractValueFn,
            boolean exactlyOnce
    ) {
        return writeKafkaP(properties,
                (T t) -> new ProducerRecord<>(topic, extractKeyFn.apply(t), extractValueFn.apply(t)),
                exactlyOnce
        );
    }

    /**
     * Returns a supplier of processors for
     * {@link KafkaSinks#kafka(DataConnectionRef, String, FunctionEx, FunctionEx)}.
     */
    public static <T, K, V> ProcessorMetaSupplier writeKafkaP(
            @Nonnull DataConnectionRef dataConnectionRef,
            @Nonnull String topic,
            @Nonnull FunctionEx<? super T, ? extends K> extractKeyFn,
            @Nonnull FunctionEx<? super T, ? extends V> extractValueFn,
            boolean exactlyOnce
    ) {
        return writeKafkaP(dataConnectionRef,
                (T t) -> new ProducerRecord<>(topic, extractKeyFn.apply(t), extractValueFn.apply(t)),
                exactlyOnce
        );
    }

    /**
     * Returns a supplier of processors for
     * {@link KafkaSinks#kafka(DataConnectionRef, Properties, String, FunctionEx, FunctionEx)}.
     */
    public static <T, K, V> ProcessorMetaSupplier writeKafkaP(
            @Nonnull DataConnectionRef dataConnectionRef,
            @Nonnull Properties properties,
            @Nonnull String topic,
            @Nonnull FunctionEx<? super T, ? extends K> extractKeyFn,
            @Nonnull FunctionEx<? super T, ? extends V> extractValueFn,
            boolean exactlyOnce
    ) {
        return writeKafkaP(dataConnectionRef,
                properties,
                (T t) -> new ProducerRecord<>(topic, extractKeyFn.apply(t), extractValueFn.apply(t)),
                exactlyOnce
        );
    }

    /**
     * Returns a supplier of processors for
     * {@link KafkaSinks#kafka(Properties, FunctionEx)}.
     */
    public static <T, K, V> ProcessorMetaSupplier writeKafkaP(
            @Nonnull Properties properties,
            @Nonnull FunctionEx<? super T, ? extends ProducerRecord<K, V>> toRecordFn,
            boolean exactlyOnce
    ) {
        return ProcessorMetaSupplier.of(1, WriteKafkaP.supplier(properties, toRecordFn, exactlyOnce));
    }

    /**
     * Returns a supplier of processors for
     * {@link KafkaSinks#kafka(DataConnectionRef, FunctionEx)}.
     */
    public static <T, K, V> ProcessorMetaSupplier writeKafkaP(
            @Nonnull DataConnectionRef dataConnectionRef,
            @Nonnull FunctionEx<? super T, ? extends ProducerRecord<K, V>> toRecordFn,
            boolean exactlyOnce
    ) {
        return ProcessorMetaSupplier.of(1, WriteKafkaP.supplier(dataConnectionRef, toRecordFn, exactlyOnce));
    }

    /**
     * Returns a supplier of processors for
     * {@link KafkaSinks#kafka(Properties, FunctionEx)}.
     */
    public static <T, K, V> ProcessorMetaSupplier writeKafkaP(
            @Nonnull DataConnectionRef dataConnectionRef,
            @Nonnull Properties properties,
            @Nonnull FunctionEx<? super T, ? extends ProducerRecord<K, V>> toRecordFn,
            boolean exactlyOnce
    ) {
        return ProcessorMetaSupplier.of(1, WriteKafkaP.supplier(dataConnectionRef, properties, toRecordFn, exactlyOnce));
    }

}
