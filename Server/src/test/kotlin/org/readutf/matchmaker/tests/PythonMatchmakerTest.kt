package org.readutf.matchmaker.tests

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import java.util.UUID
import org.readutf.matchmaker.matchmaker.Matchmaker
import org.readutf.matchmaker.matchmaker.impl.python.PythonMatchmaker
import org.readutf.matchmaker.queue.QueueTeam
import org.testng.annotations.AfterTest
import org.testng.annotations.Test

class PythonMatchmakerTest {

    private val objectMapper = ObjectMapper()
    private val matchmaker: Matchmaker = PythonMatchmaker("python", "test", "py-matchmaker-test")


    @Test
    fun pythonMatchmakerTest() {

        matchmaker.addTeam(
            QueueTeam(
                teamId = UUID.randomUUID(),
                socketId = "",
                players = listOf(UUID.randomUUID()),
                attributes = objectMapper.valueToTree(
                    mapOf(
                        "id" to "bd524747-d311-4a2a-b746-7178cc422ff4",
                        "lifetime_level" to 71.0,
                        "lifetime_kd" to 0.82,
                        "lifetime_kills" to 640.0,
                        "lifetime_deaths" to 779.0,
                        "lifetime_kills_per_match" to 3.32,
                        "lifetime_headshot_pct" to 34.91,
                        "lifetime_matches_won" to 104.0,
                        "lifetime_matches_lost" to 89.0,
                        "lifetime_matches_abandoned" to 0.0,
                        "lifetime_matches_played" to 193.0,
                        "lifetime_match_win_pct" to 53.89,
                        "lifetime_time_played" to 303209.0
                    )
                )
            )
        )
        matchmaker.addTeam(
            QueueTeam(
                teamId = UUID.randomUUID(),
                socketId = "",
                players = listOf(UUID.randomUUID()),
                attributes = objectMapper.valueToTree(
                    mapOf(
                        "id" to "0f3f1cf2-4329-49be-9215-10295819ac6c",
                        "lifetime_level" to 319.0,
                        "lifetime_kd" to 1.18,
                        "lifetime_kills" to 30575.0,
                        "lifetime_deaths" to 26014.0,
                        "lifetime_kills_per_match" to 4.19,
                        "lifetime_headshot_pct" to 51.13,
                        "lifetime_matches_won" to 3780.0,
                        "lifetime_matches_lost" to 3430.0,
                        "lifetime_matches_abandoned" to 53.0,
                        "lifetime_matches_played" to 7289.0,
                        "lifetime_match_win_pct" to 51.86,
                        "lifetime_time_played" to 10674049.0
                    )
                )
            )
        )

        for (i in 0 until 5) {
            matchmaker.matchmake()
        }
    }

    @AfterTest
    fun shutdown() {
        matchmaker.shutdown()
    }

}