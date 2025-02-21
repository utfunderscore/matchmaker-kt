package org.readutf.matchmaker

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.github.michaelbull.result.onFailure
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.github.oshai.kotlinlogging.KotlinLogging
import io.javalin.Javalin
import org.readutf.matchmaker.matchmaker.MatchmakerManager
import org.readutf.matchmaker.matchmaker.api.MatchmakerCreateEndpoint
import org.readutf.matchmaker.matchmaker.api.MatchmakerCreatorListEndpoint
import org.readutf.matchmaker.matchmaker.api.MatchmakerDeleteEndpoint
import org.readutf.matchmaker.matchmaker.api.MatchmakerListEndpoint
import org.readutf.matchmaker.matchmaker.impl.flexible.FlexibleMatchmakerCreator
import org.readutf.matchmaker.matchmaker.impl.pgvector.VectorSearchCreator
import org.readutf.matchmaker.matchmaker.store.impl.JsonMatchmakerStore
import org.readutf.matchmaker.queue.QueueManager
import org.readutf.matchmaker.queue.api.QueueCreateEndpoint
import org.readutf.matchmaker.queue.api.QueueDeleteEndpoint
import org.readutf.matchmaker.queue.api.QueueInfoEndpoint
import org.readutf.matchmaker.queue.api.QueueListEndpoint
import org.readutf.matchmaker.queue.store.impl.JsonQueueStore
import java.nio.file.Path
import kotlin.io.path.Path

class Application(
    baseDir: Path,
) {
    private val logger = KotlinLogging.logger { }

    private val matchmakerManager = MatchmakerManager(JsonMatchmakerStore(baseDir.resolve("matchmakers.json")))
    private val queueManager = QueueManager(JsonQueueStore(baseDir.resolve("queues.json"), matchmakerManager))

    val javalin =
        Javalin
            .create {
                it.showJavalinBanner = false
                it.useVirtualThreads = true
            }

    init {
        logger.info { "Starting Matchmaker..." }
        val pgVectorConfig =
            HikariConfig().apply {
                jdbcUrl = "jdbc:postgresql://localhost:5432/example_db"
                username = "postgres"
                password = "password"
            }

        val pgVectorDatasource = HikariDataSource(pgVectorConfig)

        matchmakerManager.registerCreator("flexible", FlexibleMatchmakerCreator())
        matchmakerManager.registerCreator("pgvector", VectorSearchCreator(pgVectorDatasource))
        matchmakerManager.loadMatchmakers().onFailure {
            throw it
        }

        javalin
            .get("/api/matchmakers", MatchmakerListEndpoint(matchmakerManager))
            .get("/api/matchmaker/types", MatchmakerCreatorListEndpoint(matchmakerManager))
            .put("/api/matchmaker/{name}", MatchmakerCreateEndpoint(matchmakerManager))
            .delete("/api/matchmaker/{name}", MatchmakerDeleteEndpoint(matchmakerManager))
            .get("/api/queue/{name}", QueueInfoEndpoint(queueManager))
            .get("/api/queues", QueueListEndpoint(queueManager))
            .put("/api/queue/{name}/create", QueueCreateEndpoint(queueManager, matchmakerManager))
            .delete("/api/queue/{name}", QueueDeleteEndpoint(queueManager))

        Runtime.getRuntime().addShutdownHook(
            Thread {
                queueManager.shutdown()
                matchmakerManager.shutdown()
            },
        )
    }

    fun start() {
        javalin.start(7000)
    }

    companion object {
        val objectMapper = ObjectMapper().registerKotlinModule()
    }
}

fun main() {
    val workDir = Path(System.getProperty("user.dir"))
    Application(workDir).start()
}
