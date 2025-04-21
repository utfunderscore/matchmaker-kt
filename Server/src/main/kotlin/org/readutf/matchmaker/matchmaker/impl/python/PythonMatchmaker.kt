package org.readutf.matchmaker.matchmaker.impl.python

import com.fasterxml.jackson.core.type.TypeReference
import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.onSuccess
import com.github.michaelbull.result.runCatching
import io.github.oshai.kotlinlogging.KotlinLogging
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.readutf.matchmaker.Application
import org.readutf.matchmaker.matchmaker.MatchMakerResult
import org.readutf.matchmaker.matchmaker.PooledMatchmaker
import org.readutf.matchmaker.queue.QueueTeam
import java.time.Duration
import java.time.temporal.ChronoUnit
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit

open class PythonMatchmaker(
    type: String,
    name: String,
    private val topic: String,
) : PooledMatchmaker(type, name) {
    private val logger = KotlinLogging.logger { }

    private val requestFutures = mutableMapOf<UUID, CompletableFuture<PythonResult>>()

    private val consumerSettings =
        mapOf(
            ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to "localhost:29092",
            ConsumerConfig.GROUP_ID_CONFIG to "test-group",
            ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to "org.apache.kafka.common.serialization.StringDeserializer",
            ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to "org.apache.kafka.common.serialization.StringDeserializer",
            ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to "latest",
        )

    private var producerSettings: Map<String, Any> =
        mapOf(
            ProducerConfig.BOOTSTRAP_SERVERS_CONFIG to "localhost:29092",
            ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to "org.apache.kafka.common.serialization.StringSerializer",
            ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to "org.apache.kafka.common.serialization.StringSerializer",
        )

    private val producer = KafkaProducer<String, String>(producerSettings)
    private val consumer =
        KafkaConsumer<String, String>(consumerSettings).also {
            it.subscribe(listOf("result_channel"))
        }

    init {
        Thread {
            while (true) {
                println("Listening for messages...")

                val records = consumer.poll(Duration.of(1, ChronoUnit.SECONDS))
                for (record in records) {
                    try {
                        val pythonResult = Application.objectMapper.readValue(record.value(), PythonResult::class.java)
                        requestFutures[UUID.fromString(pythonResult.requestId)]?.complete(pythonResult)
                    } catch (e: Exception) {
                        logger.error(e) { "Received an invalid message from kafka channel" }
                    }
                }
            }
        }.start()
    }

    override fun matchmake(teams: List<QueueTeam>): MatchMakerResult {
        val teamMap = teams.associateBy { it.teamId }

        val teamData =
            teams.map { team ->
                val attributes =
                    Application.objectMapper.treeToValue(
                        team.attributes,
                        object : TypeReference<HashMap<String, Any>>() {},
                    )
                attributes["id"] = team.teamId.toString()
                attributes
            }

        val requestId = UUID.randomUUID()

        val request =
            mapOf(
                "requestId" to requestId.toString(),
                "teams" to teamData,
            )

        val json = Application.objectMapper.writeValueAsString(request)

        runCatching {
            producer
                .send(
                    ProducerRecord(topic, "data", json),
                ).get()
        }.onFailure {
            it.printStackTrace()
        }.onSuccess { metadata ->
            println("Sent to topic ${metadata.topic()} partition ${metadata.partition()} offset ${metadata.offset()}%n")
        }

        producer.flush()

        val future = CompletableFuture<PythonResult>()
        requestFutures[requestId] = future

        val result =
            try {
                val result = future.get(3, TimeUnit.SECONDS)
                val queueTeams = result.teams.map { team -> team.map { teamId -> teamMap[teamId]!! } }

                MatchMakerResult.MatchMakerSuccess(queueTeams)
            } catch (e: ExecutionException) {
                MatchMakerResult.MatchMakerFailure(e)
            }

        return result
    }

    data class PythonResult(
        val requestId: String,
        val teams: List<List<UUID>>,
    )

    override fun shutdown() {
        println("Shutting down PythonMatchmaker...")
        producer.close()
    }
}
