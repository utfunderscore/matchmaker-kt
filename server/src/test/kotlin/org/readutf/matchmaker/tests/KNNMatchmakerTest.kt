package org.readutf.matchmaker.tests

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.JsonNode
import org.readutf.matchmaker.Application
import org.readutf.matchmaker.matchmaker.MatchMakerResult
import org.readutf.matchmaker.matchmaker.impl.python.PythonMatchmaker
import org.readutf.matchmaker.matchmaker.impl.python.creators.PythonMatchmakerCreator
import org.readutf.matchmaker.queue.QueueTeam
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertTrue

class KNNMatchmakerTest {
    private val modelMatchmaker =
        PythonMatchmaker(
            "test-knn",
            "knn",
            "knn",
            5,
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
        )

    private val jsonContent =
        RandomForestMatchmakerTest::class.java.getResource("/random_row.json")?.readText()
            ?: throw IllegalArgumentException("File not found: random_row.json")

    private val teamData = Application.objectMapper.readValue(jsonContent, object : TypeReference<List<JsonNode>>() {}).take(2)

    @Test
    fun successful() {
        val teams =
            teamData.map {
                QueueTeam(UUID.randomUUID(), "socketId", listOf(UUID.randomUUID()), it)
            }

        println(Application.objectMapper.writeValueAsString(teams))

        assertTrue { modelMatchmaker.matchmake(teams) is MatchMakerResult.MatchMakerSuccess }
    }

    @Test
    fun testSerialisation() {
        val creator =
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
            )

        val params =
            mapOf(
                "name" to "test",
                "batch_size" to 5,
                "features" to emptyList<String>(),
            )

        creator.deserialize(Application.objectMapper.valueToTree(params))
    }

    fun testAddTeam() {
//        val team = QueueTea
        //        m("team1", mapOf("rank" to 1, "skill" to 1000, "mmr" to 1500))
//        val result = kNearestMatchmaker.addTeam(team)
//        assert(result.isOk) { "Failed to add team: ${result.error}" }
    }
}
