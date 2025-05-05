package org.readutf.matchmaker.tests

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.JsonNode
import org.readutf.matchmaker.Application
import org.readutf.matchmaker.matchmaker.impl.python.impl.ModelMatchmaker
import org.readutf.matchmaker.queue.QueueTeam
import java.util.UUID
import kotlin.test.Test

class RandomForestMatchmakerTest {
    private val modelMatchmaker = ModelMatchmaker("test-knn", "random_forest", 5)

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

        teams.forEach {
            modelMatchmaker.addTeam(it)
        }

        println(modelMatchmaker.matchmake())
    }

    fun testAddTeam() {
//        val team = QueueTea
        //        m("team1", mapOf("rank" to 1, "skill" to 1000, "mmr" to 1500))
//        val result = kNearestMatchmaker.addTeam(team)
//        assert(result.isOk) { "Failed to add team: ${result.error}" }
    }
}
