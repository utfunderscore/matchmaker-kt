package org.readutf.matchmaker.matchmaker.impl.python

import io.github.oshai.kotlinlogging.KotlinLogging
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.ConsumerRebalanceListener
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.TopicPartition
import org.readutf.matchmaker.Application
import java.time.Duration
import java.time.temporal.ChronoUnit
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.system.measureTimeMillis

object PythonConsumerTask : Thread() {
    private val logger = KotlinLogging.logger { }
    private val active = AtomicBoolean(true)

    private val connectionFuture = CompletableFuture<Unit>()
    private val requestFutures: MutableMap<UUID, CompletableFuture<PythonResult>> = mutableMapOf()

    private val consumerSettings =
        mapOf(
            ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to "localhost:29092",
            ConsumerConfig.GROUP_ID_CONFIG to "test-group",
            ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to "org.apache.kafka.common.serialization.StringDeserializer",
            ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to "org.apache.kafka.common.serialization.StringDeserializer",
            ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to "latest",
        )

    init {
        start()
    }

    private val consumer: KafkaConsumer<String, String> =
        KafkaConsumer<String, String>(consumerSettings).also {
            it.subscribe(
                listOf("result_channel"),
                object : ConsumerRebalanceListener {
                    override fun onPartitionsRevoked(partitions: MutableCollection<TopicPartition>?) {
                    }

                    override fun onPartitionsAssigned(partitions: MutableCollection<TopicPartition>?) {
                        logger.info { "Partitions assigned: $partitions" }
                        connectionFuture.complete(Unit)
                    }
                },
            )
        }

    init {
        Runtime.getRuntime().addShutdownHook(
            Thread {
                synchronized(consumer) {
                    consumer.close()
                }
            },
        )
    }

    override fun run() {
        while (active.get()) {
            logger.debug { "Consuming messages..." }

            val records = consumer.poll(Duration.of(1, ChronoUnit.SECONDS))
            for (record in records) {
                try {
                    val pythonResult = Application.objectMapper.readValue(record.value(), PythonResult::class.java)

                    logger.info { "Received message from consumer: $pythonResult" }
                    val future = requestFutures.remove(UUID.fromString(pythonResult.requestId))
                    if (future == null) {
                        logger.warn { "Received a result for a request that does not exist: ${pythonResult.requestId}" }
                        continue
                    }

                    future.complete(pythonResult)
                } catch (e: Exception) {
                    logger.error(e) { "Received an invalid message from kafka channel" }
                }
            }
        }
    }

    fun addRequestFuture(
        requestId: UUID,
        future: CompletableFuture<PythonResult>,
    ) {
        if (!connectionFuture.isDone) {
            logger.info { "Waiting to be assigned access to results topic..." }
            val time =
                measureTimeMillis {
                    connectionFuture.join()
                }
            logger.info { "Assigned access to results topic in $time ms" }
        }

        requestFutures[requestId] = future
    }
}
