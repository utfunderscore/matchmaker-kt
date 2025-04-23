package org.readutf.matchmaker.tests

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.junit.Test
import org.readutf.matchmaker.matchmaker.MatchMakerResult
import org.readutf.matchmaker.matchmaker.impl.python.impl.KNearestConstrainedMatchmaker
import org.readutf.matchmaker.queue.QueueTeam
import java.util.UUID
import kotlin.test.assertTrue

class KNearestContrainedTests {
    private val objectMapper = jacksonObjectMapper()

    @Test
    fun `invalid team`() {
        val kNearest =
            KNearestConstrainedMatchmaker(
                "test_knearest",
                2,
                listOf(
                    "lifetime_level",
                    "lifetime_kd",
                    "lifetime_kills",
                    "lifetime_deaths",
                    "lifetime_kills_per_match",
                ),
            )

        val team1Player = UUID.randomUUID()
        val teamResult =
            kNearest.addTeam(
                QueueTeam(
                    teamId = UUID.randomUUID(),
                    players = listOf(team1Player),
                    attributes =
                        objectMapper.valueToTree(
                            mapOf(
                                "kdr" to 5,
                            ),
                        ),
                ),
            )

        assertTrue { teamResult.isErr }
        kNearest.shutdown()
    }

    @Test
    fun `add valid team`() {
        val kNearest =
            KNearestConstrainedMatchmaker(
                "test_knearest",
                2,
                listOf(
                    "lifetime_level",
                    "lifetime_kd",
                    "lifetime_kills",
                    "lifetime_deaths",
                    "lifetime_kills_per_match",
                ),
            )

        val team1Player = UUID.randomUUID()
        val teamResult =
            kNearest.addTeam(
                QueueTeam(
                    teamId = UUID.randomUUID(),
                    players = listOf(team1Player),
                    attributes =
                        objectMapper.valueToTree(
                            mapOf(
                                "lifetime_level" to 5,
                                "lifetime_kd" to 5,
                                "lifetime_kills" to 5,
                                "lifetime_deaths" to 5,
                                "lifetime_kills_per_match" to 5,
                            ),
                        ),
                ),
            )

        assertTrue { teamResult.isOk }
        kNearest.shutdown()
    }

    @Test
    fun `test not enough teams`() {
        val kNearest =
            KNearestConstrainedMatchmaker(
                "test_knearest",
                2,
                listOf(
                    "lifetime_level",
                    "lifetime_kd",
                    "lifetime_kills",
                    "lifetime_deaths",
                    "lifetime_kills_per_match",
                ),
            )

        val team1Player = UUID.randomUUID()
        val teamResult =
            kNearest.addTeam(
                QueueTeam(
                    teamId = UUID.randomUUID(),
                    players = listOf(team1Player),
                    attributes =
                        objectMapper.valueToTree(
                            mapOf(
                                "lifetime_level" to 5,
                                "lifetime_kd" to 5,
                                "lifetime_kills" to 5,
                                "lifetime_deaths" to 5,
                                "lifetime_kills_per_match" to 5,
                            ),
                        ),
                ),
            )

        assertTrue { teamResult.isOk }

        val matchmakeResult = kNearest.matchmake()

        assertTrue { matchmakeResult is MatchMakerResult.MatchMakerSkip }
        kNearest.shutdown()
    }

    @Test
    fun `successful matchmake`() {
        val kNearest =
            KNearestConstrainedMatchmaker(
                "test_knearest",
                2,
                listOf(
                    "lifetime_level",
                    "lifetime_kd",
                    "lifetime_kills",
                    "lifetime_deaths",
                    "lifetime_kills_per_match",
                ),
            )

        val team1Player = UUID.randomUUID()
        val team1Result =
            kNearest.addTeam(
                QueueTeam(
                    teamId = UUID.randomUUID(),
                    players = listOf(team1Player),
                    attributes =
                        objectMapper.valueToTree(
                            mapOf(
                                "lifetime_level" to 5,
                                "lifetime_kd" to 5,
                                "lifetime_kills" to 5,
                                "lifetime_deaths" to 5,
                                "lifetime_kills_per_match" to 5,
                            ),
                        ),
                ),
            )

        assertTrue { team1Result.isOk }

        val team2Player = UUID.randomUUID()
        val team2Result =
            kNearest.addTeam(
                QueueTeam(
                    teamId = UUID.randomUUID(),
                    players = listOf(team2Player),
                    attributes =
                        objectMapper.valueToTree(
                            mapOf(
                                "lifetime_level" to 5,
                                "lifetime_kd" to 5,
                                "lifetime_kills" to 5,
                                "lifetime_deaths" to 5,
                                "lifetime_kills_per_match" to 5,
                            ),
                        ),
                ),
            )

        assertTrue { team2Result.isOk }
        val matchmakeResult = kNearest.matchmake()

        if (matchmakeResult is MatchMakerResult.MatchMakerFailure) {
            println("Matchmake failed with error")
            matchmakeResult.err.printStackTrace()
        }
        assertTrue { matchmakeResult is MatchMakerResult.MatchMakerSuccess }

        kNearest.shutdown()
    }
}
