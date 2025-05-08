package org.readutf.matchmaker.tests

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.JsonNode
import org.readutf.matchmaker.Application
import org.readutf.matchmaker.matchmaker.impl.python.PythonMatchmaker
import org.readutf.matchmaker.queue.QueueTeam
import java.util.UUID
import kotlin.test.Test

class RandomForestMatchmakerTest {
    private val modelMatchmaker =
        PythonMatchmaker(
            "test-knn",
            "random_forest",
            "random_forest",
            2,
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
        )

    private val jsonContent =
        RandomForestMatchmakerTest::class.java.getResource("/random_row.json")?.readText()
            ?: throw IllegalArgumentException("File not found: random_row.json")

    private val teamData = Application.objectMapper.readValue(jsonContent, object : TypeReference<List<JsonNode>>() {})

    @Test
    fun test() {
        val teams =
            teamData.map {
                QueueTeam(UUID.randomUUID(), "socketId", listOf(UUID.randomUUID()), it)
            }

        println(teams.size)

        println(modelMatchmaker.matchmake(teams))
    }

    fun testAddTeam() {
//        val team = QueueTea
        //        m("team1", mapOf("rank" to 1, "skill" to 1000, "mmr" to 1500))
//        val result = kNearestMatchmaker.addTeam(team)
//        assert(result.isOk) { "Failed to add team: ${result.error}" }
    }
}
