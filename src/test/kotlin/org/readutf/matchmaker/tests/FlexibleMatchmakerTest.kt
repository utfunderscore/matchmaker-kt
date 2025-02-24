package org.readutf.matchmaker.tests

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.michaelbull.result.Result
import org.readutf.matchmaker.matchmaker.MatchMakerResult
import org.readutf.matchmaker.matchmaker.Matchmaker
import org.readutf.matchmaker.matchmaker.impl.flexible.FlexibleMatchmaker
import org.readutf.matchmaker.matchmaker.impl.flexible.FlexibleMatchmakerCreator
import org.readutf.matchmaker.queue.QueueTeam
import org.testng.annotations.Test
import java.util.UUID
import kotlin.test.assertTrue

class FlexibleMatchmakerTest {
    val objectMapper = jacksonObjectMapper()

    @Test
    fun `create flexible matchmaker all values present`() {
        val creator = FlexibleMatchmakerCreator()

        assertTrue {
            creator
                .createMatchmaker(
                    objectMapper.valueToTree<JsonNode>(
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
                .createMatchmaker(
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
                .createMatchmaker(
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
                .createMatchmaker(
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
                .createMatchmaker(
                    objectMapper.valueToTree<JsonNode>(
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
                .createMatchmaker(
                    objectMapper.valueToTree<JsonNode>(
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
            creator.createMatchmaker(
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
                        override fun matchmake(): MatchMakerResult {
                            TODO("Not yet implemented")
                        }

                        override fun addTeam(team: QueueTeam): Result<Unit, Throwable> {
                            TODO("Not yet implemented")
                        }

                        override fun removeTeam(teamId: UUID): Result<Unit, Throwable> {
                            TODO("Not yet implemented")
                        }
                    },
                ).isErr
        }
    }

    @Test(dependsOnMethods = ["test flexible matchmaker serialization"])
    fun `test flexible matchmaker deserialization`() {
        val creator = FlexibleMatchmakerCreator()
        val matchmaker =
            creator.createMatchmaker(
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

        assertTrue { matchmaker.matchmake() is MatchMakerResult.MatchMakerSkip }
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

        matchmaker.addTeam(QueueTeam(UUID.randomUUID(), List(4) { UUID.randomUUID() }, jsonNode))

        matchmaker.addTeam(QueueTeam(UUID.randomUUID(), List(4) { UUID.randomUUID() }, jsonNode))

        assertTrue { matchmaker.matchmake() is MatchMakerResult.MatchMakerSkip }
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

        matchmaker.addTeam(QueueTeam(UUID.randomUUID(), listOf(UUID.randomUUID()), jsonNode))

        matchmaker.addTeam(QueueTeam(UUID.randomUUID(), listOf(UUID.randomUUID()), jsonNode))

        val matchmakeResult = matchmaker.matchmake()

        print(matchmakeResult)

        assertTrue { matchmakeResult is MatchMakerResult.MatchMakerSuccess }
    }
}
