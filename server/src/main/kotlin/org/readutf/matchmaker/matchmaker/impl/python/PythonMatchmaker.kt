package org.readutf.matchmaker.matchmaker.impl.python

import com.fasterxml.jackson.core.type.TypeReference
import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.onSuccess
import com.github.michaelbull.result.runCatching
import io.github.oshai.kotlinlogging.KotlinLogging
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.readutf.matchmaker.Application
import org.readutf.matchmaker.matchmaker.MatchMakerResult
import org.readutf.matchmaker.matchmaker.PooledMatchmaker
import org.readutf.matchmaker.queue.QueueTeam
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

abstract class PythonMatchmaker(
    type: String,
    name: String,
    private val topic: String,
) : PooledMatchmaker(type, name) {
    private val logger = KotlinLogging.logger { }

    private var producerSettings: Map<String, Any> =
        mapOf(
            ProducerConfig.BOOTSTRAP_SERVERS_CONFIG to "localhost:29092",
            ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to "org.apache.kafka.common.serialization.StringSerializer",
            ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to "org.apache.kafka.common.serialization.StringSerializer",
        )

    private val producer = KafkaProducer<String, String>(producerSettings)
    private val pythonConsumerTask = PythonConsumerTask

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
        val future = CompletableFuture<PythonResult>()
        logger.info { "Matchmaking $teamData with id $requestId" }
        pythonConsumerTask.addRequestFuture(requestId, future)

        val request = createRequest()
        request["requestId"] = requestId.toString()
        request["teams"] = teamData

        val json = Application.objectMapper.writeValueAsString(request)

        runCatching {
            producer
                .send(
                    ProducerRecord(topic, "data", json),
                ).get()
        }.onFailure {
            return MatchMakerResult.MatchMakerFailure(it)
        }.onSuccess { metadata ->
            println("Sent to topic ${metadata.topic()} partition ${metadata.partition()} offset ${metadata.offset()}%n")
        }

        producer.flush()

        val result =
            try {
                val result = future.get(15, TimeUnit.SECONDS)
                val queueTeams = result.teams.map { team -> team.map { teamId -> teamMap[teamId]!! } }

                MatchMakerResult.MatchMakerSuccess(queueTeams)
            } catch (e: Exception) {
                MatchMakerResult.MatchMakerFailure(e)
            }

        return result
    }

    abstract fun createRequest(): MutableMap<String, Any?>

    override fun shutdown() {
        println("Shutting down PythonMatchmaker...")
        producer.close()
    }
}
