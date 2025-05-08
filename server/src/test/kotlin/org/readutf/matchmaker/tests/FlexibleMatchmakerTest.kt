package org.readutf.matchmaker.tests

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.michaelbull.result.Result
import org.readutf.matchmaker.matchmaker.MatchMakerResult
import org.readutf.matchmaker.matchmaker.Matchmaker
import org.readutf.matchmaker.matchmaker.impl.flexible.FlexibleMatchmaker
import org.readutf.matchmaker.matchmaker.impl.flexible.FlexibleMatchmakerCreator
import org.readutf.matchmaker.queue.QueueTeam
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertTrue

class FlexibleMatchmakerTest {
    private val objectMapper = jacksonObjectMapper()

    @Test
    fun `shutdown matchmaker`() {
        FlexibleMatchmaker(
            name = "flexible-test",
            targetTeamSize = 1,
            minTeamSize = 1,
            maxTeamSize = 4,
            numberOfTeams = 2,
        ).shutdown()
    }

    @Test
    fun `create flexible matchmaker all values present`() {
        val creator = FlexibleMatchmakerCreator()

        assertTrue {
            creator
                .deserialize(
                    objectMapper.valueToTree(
                        mapOf(
                            "name" to "test",
                            "targetTeamSize" to 5,
                            "minTeamSize" to 1,
                            "maxTeamSize" to 10,
                            "numberOfTeams" to 2,
                        ),
                    ),
                ).isOk
        }
    }

    @Test
    fun `create flexible matchmaker missing name`() {
        val creator = FlexibleMatchmakerCreator()

        assertTrue {
            creator
                .deserialize(
                    objectMapper.valueToTree<JsonNode>(
                        mapOf(
                            "targetTeamSize" to 5,
                            "minTeamSize" to 1,
                            "maxTeamSize" to 10,
                            "numberOfTeams" to 2,
                        ),
                    ),
                ).isErr
        }
    }

    @Test
    fun `create flexible matchmaker missing targetTeamSize`() {
        val creator = FlexibleMatchmakerCreator()

        assertTrue {
            creator
                .deserialize(
                    objectMapper.valueToTree<JsonNode>(
                        mapOf(
                            "name" to "test",
                            "minTeamSize" to 1,
                            "maxTeamSize" to 10,
                            "numberOfTeams" to 2,
                        ),
                    ),
                ).isErr
        }
    }

    @Test
    fun `create flexible matchmaker missing minTeamSize`() {
        val creator = FlexibleMatchmakerCreator()

        assertTrue {
            creator
                .deserialize(
                    objectMapper.valueToTree<JsonNode>(
                        mapOf(
                            "name" to "test",
                            "targetTeamSize" to 5,
                            "maxTeamSize" to 10,
                            "numberOfTeams" to 2,
                        ),
                    ),
                ).isErr
        }
    }

    @Test
    fun `create flexible matchmaker missing maxTeamSize`() {
        val creator = FlexibleMatchmakerCreator()

        assertTrue {
            creator
                .deserialize(
                    objectMapper.valueToTree(
                        mapOf(
                            "name" to "test",
                            "targetTeamSize" to 5,
                            "minTeamSize" to 1,
                            "numberOfTeams" to 2,
                        ),
                    ),
                ).isErr
        }
    }

    @Test
    fun `create flexible matchmaker missing numberOfTeams`() {
        val creator = FlexibleMatchmakerCreator()

        assertTrue {
            creator
                .deserialize(
                    objectMapper.valueToTree(
                        mapOf(
                            "name" to "test",
                            "targetTeamSize" to 5,
                            "minTeamSize" to 1,
                            "maxTeamSize" to 10,
                        ),
                    ),
                ).isErr
        }
    }

    @Test
    fun `test flexible matchmaker serialization`() {
        val creator = FlexibleMatchmakerCreator()
        val matchmaker =
            creator.deserialize(
                objectMapper.valueToTree<JsonNode>(
                    mapOf(
                        "name" to "test",
                        "targetTeamSize" to 5,
                        "minTeamSize" to 1,
                        "maxTeamSize" to 10,
                        "numberOfTeams" to 2,
                    ),
                ),
            )

        assertTrue { matchmaker.isOk }

        assertTrue { creator.serialize(matchmaker.value).isOk }
    }

    @Test
    fun `test flexible matchmaker serialization invalid type`() {
        val creator = FlexibleMatchmakerCreator()

        assertTrue {
            creator
                .serialize(
                    object : Matchmaker("test", "test") {
                        override fun matchmake(teams: Collection<QueueTeam>): MatchMakerResult {
                            TODO("Not yet implemented")
                        }

                        override fun validateTeam(team: QueueTeam): Result<Unit, Throwable> {
                            TODO("Not yet implemented")
                        }
                    },
                ).isErr
        }
    }

    fun `test flexible matchmaker deserialization`() {
        val creator = FlexibleMatchmakerCreator()
        val matchmaker =
            creator.deserialize(
                objectMapper.valueToTree(
                    mapOf(
                        "name" to "test",
                        "targetTeamSize" to 5,
                        "minTeamSize" to 1,
                        "maxTeamSize" to 10,
                        "numberOfTeams" to 2,
                    ),
                ),
            )

        assertTrue { matchmaker.isOk }

        assertTrue { creator.deserialize(creator.serialize(matchmaker.value).value).isOk }
    }

    @Test
    fun `test deserialize matches serialized data`() {
        val matchmaker = FlexibleMatchmaker("test", 5, 1, 10, 2)

        val creator = FlexibleMatchmakerCreator()

        val serialized = creator.serialize(matchmaker).value
        val deserialized = creator.deserialize(serialized).value

        assertTrue { matchmaker == deserialized }
    }

    @Test
    fun `flexible matchmake not enough players`() {
        val matchmaker =
            FlexibleMatchmaker(
                name = "flexible-test",
                targetTeamSize = 1,
                minTeamSize = 1,
                maxTeamSize = 1,
                numberOfTeams = 2,
            )

        assertTrue { matchmaker.matchmake(emptyList()) is MatchMakerResult.MatchMakerSkip }
    }

    @Test
    fun `flexible matchmake not invalid composition`() {
        val matchmaker =
            FlexibleMatchmaker(
                name = "flexible-test",
                targetTeamSize = 1,
                minTeamSize = 1,
                maxTeamSize = 4,
                numberOfTeams = 2,
            )

        val jsonNode =
            objectMapper.valueToTree<JsonNode>(emptyMap<String, String>())

        val teams =
            listOf(
                QueueTeam(
                    teamId = UUID.randomUUID(),
                    players = List(4) { UUID.randomUUID() },
                    attributes = jsonNode,
                ),
                QueueTeam(UUID.randomUUID(), "", List(4) { UUID.randomUUID() }, jsonNode),
            )

        assertTrue { matchmaker.matchmake(teams) is MatchMakerResult.MatchMakerSkip }
    }

    @Test
    fun `flexible matchmake success`() {
        val matchmaker =
            FlexibleMatchmaker(
                name = "flexible-test",
                targetTeamSize = 1,
                minTeamSize = 1,
                maxTeamSize = 1,
                numberOfTeams = 2,
            )

        val jsonNode =
            objectMapper.valueToTree<JsonNode>(emptyMap<String, String>())

        val teams =
            listOf(
                QueueTeam(
                    teamId = UUID.randomUUID(),
                    players = List(1) { UUID.randomUUID() },
                    attributes = jsonNode,
                ),
                QueueTeam(
                    teamId = UUID.randomUUID(),
                    players = List(1) { UUID.randomUUID() },
                    attributes = jsonNode,
                ),
            )

        val matchmakeResult = matchmaker.matchmake(teams)

        print(matchmakeResult)

        assertTrue { matchmakeResult is MatchMakerResult.MatchMakerSuccess }
    }
}
