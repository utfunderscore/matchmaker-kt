package org.readutf.matchmaker

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.github.michaelbull.result.onFailure
import io.github.oshai.kotlinlogging.KotlinLogging
import io.javalin.Javalin
import io.javalin.apibuilder.ApiBuilder.delete
import io.javalin.apibuilder.ApiBuilder.get
import io.javalin.apibuilder.ApiBuilder.path
import io.javalin.apibuilder.ApiBuilder.put
import io.javalin.apibuilder.ApiBuilder.ws
import org.readutf.matchmaker.game.GameProvider
import org.readutf.matchmaker.game.impl.PseudoGameProvider
import org.readutf.matchmaker.matchmaker.MatchmakerManager
import org.readutf.matchmaker.matchmaker.api.MatchmakerCreateEndpoint
import org.readutf.matchmaker.matchmaker.api.MatchmakerCreatorListEndpoint
import org.readutf.matchmaker.matchmaker.api.MatchmakerDeleteEndpoint
import org.readutf.matchmaker.matchmaker.api.MatchmakerListEndpoint
import org.readutf.matchmaker.matchmaker.impl.elo.EloMatchmakerCreator
import org.readutf.matchmaker.matchmaker.impl.flexible.FlexibleMatchmakerCreator
import org.readutf.matchmaker.matchmaker.impl.python.creators.PythonMatchmakerCreator
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
    matchmakerStore: MatchmakerStore,
    queueStore: QueueStore,
) {
    private val logger = KotlinLogging.logger { }

    private var running = true
    private val gameProvider: GameProvider = PseudoGameProvider()
    val matchmakerManager = MatchmakerManager(matchmakerStore)
    val queueManager = QueueManager(gameProvider, queueStore)

    lateinit var javalin: Javalin

    init {
        logger.info { "Starting Matchmaker..." }

        matchmakerManager.registerCreator("flexible", FlexibleMatchmakerCreator())
        matchmakerManager.registerCreator("elo", EloMatchmakerCreator())
        matchmakerManager.registerCreator(
            "knn",
            PythonMatchmakerCreator(
                "knn",
                "knn",
                listOf(
                    "lifetimeKdAvg",
                    "lifetimeKillsAvg",
                    "lifetimeDeathsAvg",
                    "lifetimeKillsPerMatchAvg",
                    "lifetimeHeadshotPctAvg",
                    "lifetimeMatchesWonAvg",
                    "lifetimeMatchesLostAvg",
                    "lifetimeMatchesAbandonedAvg",
                    "lifetimeMatchWinPctAvg",
                    "lastSeasonKillsAvg",
                    "lastSeasonDeathsAvg",
                    "lastSeasonKillsPerMatchAvg",
                    "lastSeasonMatchesWonAvg",
                    "lastSeasonMatchesLostAvg",
                    "lastSeasonMatchesAbandonedAvg",
                    "lastSeasonMatchWinPctAvg",
                    "lastSeasonBestRankIdAvg",
                ),
            ),
        )
        matchmakerManager.registerCreator(
            "random_forest",
            PythonMatchmakerCreator(
                "random_forest",
                "random_forest",
                listOf(
                    "orange.lifetimeKdAvg",
                    "blue.lifetimeKdAvg",
                    "orange.lifetimeKillsAvg",
                    "blue.lifetimeKillsAvg",
                    "orange.lifetimeDeathsAvg",
                    "blue.lifetimeDeathsAvg",
                    "orange.lifetimeKillsPerMatchAvg",
                    "blue.lifetimeKillsPerMatchAvg",
                    "orange.lifetimeHeadshotPctAvg",
                    "blue.lifetimeHeadshotPctAvg",
                    "orange.lifetimeMatchesWonAvg",
                    "blue.lifetimeMatchesWonAvg",
                    "orange.lifetimeMatchesLostAvg",
                    "blue.lifetimeMatchesLostAvg",
                    "orange.lifetimeMatchesAbandonedAvg",
                    "blue.lifetimeMatchesAbandonedAvg",
                    "orange.lifetimeMatchWinPctAvg",
                    "blue.lifetimeMatchWinPctAvg",
                    "orange.lastSeasonKillsAvg",
                    "blue.lastSeasonKillsAvg",
                    "orange.lastSeasonDeathsAvg",
                    "blue.lastSeasonDeathsAvg",
                    "orange.lastSeasonKillsPerMatchAvg",
                    "blue.lastSeasonKillsPerMatchAvg",
                    "orange.lastSeasonMatchesWonAvg",
                    "blue.lastSeasonMatchesWonAvg",
                    "orange.lastSeasonMatchesLostAvg",
                    "blue.lastSeasonMatchesLostAvg",
                    "orange.lastSeasonMatchesAbandonedAvg",
                    "blue.lastSeasonMatchesAbandonedAvg",
                    "orange.lastSeasonMatchWinPctAvg",
                    "blue.lastSeasonMatchWinPctAvg",
                    "orange.lastSeasonBestRankIdAvg",
                    "blue.lastSeasonBestRankIdAvg",
                ),
            ),
        )
        matchmakerManager.registerCreator("k_means", PythonMatchmakerCreator("k_means", "k_means", emptyList()))

        matchmakerManager.loadMatchmakers().onFailure {
            throw it
        }

        queueManager.loadQueues(matchmakerManager)

        javalin =
            Javalin.create {
                it.showJavalinBanner = false
                it.useVirtualThreads = true

                it.router.apiBuilder {
                    path("/api/private/matchmaker") {
                        get(MatchmakerListEndpoint(matchmakerManager))
                        path("/types") {
                            get(MatchmakerCreatorListEndpoint(matchmakerManager))
                        }
                        path("{type}") {
                            put(MatchmakerCreateEndpoint(matchmakerManager))
                            delete(MatchmakerDeleteEndpoint(matchmakerManager))
                        }
                    }
                    path("/api/private/queue") {
                        path("{name}") {
                            get(QueueInfoEndpoint(queueManager))
                            put(QueueCreateEndpoint(queueManager, matchmakerManager))
                            delete(QueueDeleteEndpoint(queueManager))
                        }
                        get(QueueListEndpoint(queueManager))
                    }
                    path("/api/public/queue") {
                        path("{name}") {
                            ws(QueueJoinSocket(queueManager))
                        }
                    }
                }
            }

        Runtime.getRuntime().addShutdownHook(
            Thread {
                shutdown()
            },
        )
    }

    private fun shutdown() {
        if (!running) return
        queueManager.shutdown()
        matchmakerManager.shutdown()
        running = false
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
        matchmakerStore = jsonMatchmakerStore,
        queueStore = jsonQueueStore,
    ).start("0.0.0.0", 7000)
}
