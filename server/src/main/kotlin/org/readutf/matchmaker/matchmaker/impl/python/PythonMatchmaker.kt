package org.readutf.matchmaker.matchmaker.impl.python

import com.fasterxml.jackson.core.type.TypeReference
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.onSuccess
import com.github.michaelbull.result.runCatching
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.readutf.matchmaker.Application
import org.readutf.matchmaker.matchmaker.MatchMakerResult
import org.readutf.matchmaker.matchmaker.Matchmaker
import org.readutf.matchmaker.queue.QueueTeam
import org.readutf.matchmaker.utils.containsAllKeys
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

class PythonMatchmaker(
    name: String,
    type: String,
    val topic: String,
    val batchSize: Int,
    val features: List<String>,
) : Matchmaker(type, name) {
    private var producerSettings: Map<String, Any> =
        mapOf(
            ProducerConfig.BOOTSTRAP_SERVERS_CONFIG to "kafka:29092",
            ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to "org.apache.kafka.common.serialization.StringSerializer",
            ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to "org.apache.kafka.common.serialization.StringSerializer",
        )

    private val producer = KafkaProducer<String, String>(producerSettings)
    private val pythonConsumerTask = PythonConsumerTask

    override fun matchmake(teams: Collection<QueueTeam>): MatchMakerResult {
        if (teams.size < batchSize) {
            return MatchMakerResult.skip()
        }

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
//        logger.info { "Matchmaking $teamData with id $requestId" }
        pythonConsumerTask.addRequestFuture(requestId, future)

        val request = mutableMapOf<String, Any>()
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

                if (result.error != null || result.teams == null) {
                    return MatchMakerResult.MatchMakerFailure(Throwable(result.error ?: "Unknown python error"))
                }
                val queueTeams = result.teams.map { team -> team.map { teamId -> teamMap[teamId]!! } }

                MatchMakerResult.MatchMakerSuccess(queueTeams)
            } catch (e: Exception) {
                MatchMakerResult.MatchMakerFailure(e)
            }

        return result
    }

    override fun validateTeam(team: QueueTeam): Result<Unit, Throwable> {
        if (!team.attributes.containsAllKeys(features)) {
            return Err(IllegalArgumentException("Team does not contain all required attributes"))
        }

        return Ok(Unit)
    }

    override fun shutdown() {
        println("Shutting down PythonMatchmaker...")
        producer.close()
    }
}
