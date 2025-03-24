package org.readutf.matchmaker

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.github.michaelbull.result.onFailure
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.github.oshai.kotlinlogging.KotlinLogging
import io.javalin.Javalin
import org.readutf.matchmaker.game.GameProvider
import org.readutf.matchmaker.game.impl.PseudoGameProvider
import org.readutf.matchmaker.matchmaker.MatchmakerManager
import org.readutf.matchmaker.matchmaker.api.MatchmakerCreateEndpoint
import org.readutf.matchmaker.matchmaker.api.MatchmakerCreatorListEndpoint
import org.readutf.matchmaker.matchmaker.api.MatchmakerDeleteEndpoint
import org.readutf.matchmaker.matchmaker.api.MatchmakerListEndpoint
import org.readutf.matchmaker.matchmaker.impl.flexible.FlexibleMatchmakerCreator
import org.readutf.matchmaker.matchmaker.impl.pgvector.VectorSearchCreator
import org.readutf.matchmaker.matchmaker.store.MatchmakerStore
import org.readutf.matchmaker.matchmaker.store.impl.JsonMatchmakerStore
import org.readutf.matchmaker.queue.QueueManager
import org.readutf.matchmaker.queue.QueueTeam
import org.readutf.matchmaker.queue.api.QueueCreateEndpoint
import org.readutf.matchmaker.queue.api.QueueDeleteEndpoint
import org.readutf.matchmaker.queue.api.QueueInfoEndpoint
import org.readutf.matchmaker.queue.api.QueueJoinSocket
import org.readutf.matchmaker.queue.api.QueueListEndpoint
import org.readutf.matchmaker.queue.store.QueueStore
import org.readutf.matchmaker.queue.store.impl.JsonQueueStore
import java.util.UUID
import kotlin.io.path.Path

class Application(
    databaseUrl: String = "jdbc:postgresql://localhost:5432/example_db",
    username: String,
    password: String,
    matchmakerStore: MatchmakerStore,
    queueStore: QueueStore,
) {
    private val logger = KotlinLogging.logger { }

    private val gameProvider: GameProvider = PseudoGameProvider()
    private val matchmakerManager = MatchmakerManager(matchmakerStore)
    private val queueManager = QueueManager(queueStore)

    private val javalin =
        Javalin.create {
            it.showJavalinBanner = false
            it.useVirtualThreads = true
            it.bundledPlugins.enableDevLogging()
        }

    init {
        logger.info { "Starting Matchmaker..." }
        val pgVectorConfig = HikariConfig()

        pgVectorConfig.jdbcUrl = databaseUrl
        pgVectorConfig.username = username
        pgVectorConfig.password = password

        val pgVectorDatasource = HikariDataSource(pgVectorConfig)

        matchmakerManager.registerCreator("flexible", FlexibleMatchmakerCreator())
        matchmakerManager.registerCreator("pgvector", VectorSearchCreator(pgVectorDatasource))
        matchmakerManager.loadMatchmakers().onFailure {
            throw it
        }

        queueManager.loadQueues(matchmakerManager)

        javalin
            .get("/api/matchmakers", MatchmakerListEndpoint(matchmakerManager))
            .get("/api/matchmaker/types", MatchmakerCreatorListEndpoint(matchmakerManager))
            .put("/api/matchmaker/{name}", MatchmakerCreateEndpoint(matchmakerManager))
            .delete("/api/matchmaker/{name}", MatchmakerDeleteEndpoint(matchmakerManager))
            .get("/api/queue/{name}", QueueInfoEndpoint(queueManager))
            .get("/api/queues", QueueListEndpoint(queueManager))
            .put("/api/queue/{name}", QueueCreateEndpoint(queueManager, matchmakerManager))
            .delete("/api/queue/{name}", QueueDeleteEndpoint(queueManager))
            .ws("/api/queue/{name}", QueueJoinSocket(queueManager))

        Runtime.getRuntime().addShutdownHook(
            Thread {
                queueManager.shutdown()
                matchmakerManager.shutdown()
            },
        )
    }

    fun start(
        hostname: String,
        port: Int,
    ) {
        javalin.start(hostname, port)
    }

    companion object {
        val objectMapper = ObjectMapper().registerKotlinModule()
    }
}

fun main() {
    val objectMapper = jacksonObjectMapper()
    println(
        objectMapper.writeValueAsString(
            QueueTeam(
                teamId = UUID.randomUUID(),
                socketId = "",
                players = listOf(UUID.randomUUID()),
                attributes =
                    objectMapper.valueToTree<JsonNode>(
                        mapOf(
                            "kdr" to 1.0,
                        ),
                    ),
            ),
        ),
    )
    println(
        objectMapper.writeValueAsString(
            QueueTeam(
                teamId = UUID.randomUUID(),
                socketId = "",
                players = listOf(UUID.randomUUID()),
                attributes =
                    objectMapper.valueToTree<JsonNode>(
                        mapOf(
                            "kdr" to 1.0,
                        ),
                    ),
            ),
        ),
    )

    val workDir = Path(System.getProperty("user.dir"))

    val jsonMatchmakerStore = JsonMatchmakerStore(workDir)
    val jsonQueueStore = JsonQueueStore(workDir)

    Application(
        databaseUrl = "jdbc:postgresql://localhost:5432/example_db",
        username = "postgres",
        password = "password",
        matchmakerStore = jsonMatchmakerStore,
        queueStore = jsonQueueStore,
    ).start("0.0.0.0", 7000)
}
