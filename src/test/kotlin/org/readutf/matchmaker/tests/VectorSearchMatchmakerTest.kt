package org.readutf.matchmaker.tests

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.mockk.every
import io.mockk.spyk
import org.readutf.matchmaker.matchmaker.MatchMakerResult
import org.readutf.matchmaker.matchmaker.Matchmaker
import org.readutf.matchmaker.matchmaker.impl.pgvector.PostgresVectorDatabase
import org.readutf.matchmaker.matchmaker.impl.pgvector.PostgresVectorSearchMatchmaker
import org.readutf.matchmaker.matchmaker.impl.pgvector.VectorSearchCreator
import org.readutf.matchmaker.queue.QueueTeam
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName
import org.testng.annotations.BeforeClass
import org.testng.annotations.Test
import java.sql.SQLException
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger
import kotlin.jvm.java
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class VectorSearchMatchmakerTest {
    var postgres = PostgreSQLContainer(DockerImageName.parse("pgvector/pgvector:pg17"))

    init {
        postgres.start()
    }

    lateinit var hikariConfig: HikariConfig
    lateinit var dataSource: HikariDataSource

    val objectMapper = jacksonObjectMapper()

    @BeforeClass
    fun setup() {
        hikariConfig =
            HikariConfig().apply {
                jdbcUrl = postgres.jdbcUrl
                username = postgres.username
                password = postgres.password
            }
        dataSource = HikariDataSource(hikariConfig)
    }

    @Test
    fun `create matchmaker`() {
        createMatchmaker()
    }

    @Test
    fun `create matchmaker invalid name`() {
        try {
            PostgresVectorSearchMatchmaker(
                name = "test-",
                minPoolSize = 2,
                teamSize = 2,
                numberOfTeams = 2,
                hikariDataSource = dataSource,
                requiredStatistics = listOf("kdr", "elo"),
            )
        } catch (e: IllegalArgumentException) {
            assert(e.message == "Name must match regex ^[a-zA-Z0-9_]+$")
        }
    }

    @Test
    fun `add too small team`() {
        val matchmaker = createMatchmaker()

        val result =
            matchmaker.addTeam(
                QueueTeam(
                    teamId = UUID.randomUUID(),
                    players = listOf(UUID.randomUUID()),
                    attributes = objectMapper.valueToTree(mapOf("kdr" to 1.0, "elo" to 1.0)),
                ),
            )

        assertEquals(result.error.message, "Team size must be 2")
    }

    @Test
    fun `add team without required statistics`() {
        val matchmaker = createMatchmaker()

        val result =
            matchmaker.addTeam(
                QueueTeam(
                    teamId = UUID.randomUUID(),
                    players = listOf(UUID.randomUUID(), UUID.randomUUID()),
                    attributes = objectMapper.valueToTree(emptyMap<String, String>()),
                ),
            )

        assertEquals(result.error.message, "Team must contain all required statistics")
    }

    @Test
    fun `add team`() {
        val matchmaker = createMatchmaker()

        val result =
            matchmaker.addTeam(
                QueueTeam(
                    teamId = UUID.randomUUID(),
                    players = listOf(UUID.randomUUID(), UUID.randomUUID()),
                    attributes = objectMapper.valueToTree(mapOf("kdr" to 1.0, "elo" to 1.0)),
                ),
            )

        assertEquals(result.isOk, true)
    }

    @Test
    fun `remove team`() {
        val matchmaker = createMatchmaker()

        val teamId = UUID.randomUUID()

        val addTeamResult =
            matchmaker
                .addTeam(
                    QueueTeam(
                        teamId = teamId,
                        players = listOf(UUID.randomUUID(), UUID.randomUUID()),
                        attributes = objectMapper.valueToTree(mapOf("kdr" to 1.0, "elo" to 1.0)),
                    ),
                )

        assertEquals(addTeamResult.isOk, true)

        val removeResult = matchmaker.removeTeam(teamId)

        assertEquals(removeResult.isOk, true)
    }

    @Test
    fun `matchmake not enough teams`() {
        val matchmaker = createMatchmaker()

        val teamId = UUID.randomUUID()

        val addTeamResult =
            matchmaker
                .addTeam(
                    QueueTeam(
                        teamId = teamId,
                        players = listOf(UUID.randomUUID(), UUID.randomUUID()),
                        attributes = objectMapper.valueToTree(mapOf("kdr" to 1.0, "elo" to 1.0)),
                    ),
                )

        assertEquals(addTeamResult.isOk, true)

        val matchmakeResult = matchmaker.matchmake()

        assert(matchmakeResult is MatchMakerResult.MatchMakerSkip)
    }

    @Test
    fun `matchmake returns invalid size`() {
        val matchmaker = createMatchmaker()
        val database = spyk(matchmaker.postgresVersionDatabase)

        val teamId = UUID.randomUUID()

        val team1Players = listOf(UUID.randomUUID(), UUID.randomUUID())

        every { database.getNearby(any(), any()) } returns Ok(listOf(teamId))

        matchmaker::class.java
            .getDeclaredField("postgresVersionDatabase")
            .apply { isAccessible = true }
            .set(matchmaker, database)

        val team1 =
            QueueTeam(
                teamId = teamId,
                players = team1Players,
                attributes = objectMapper.valueToTree(mapOf("kdr" to 1.0, "elo" to 1.0)),
            )

        val addTeamResult = matchmaker.addTeam(team1)

        assertEquals(addTeamResult.isOk, true)

        val teamId2 = UUID.randomUUID()
        val team2Players = listOf(UUID.randomUUID(), UUID.randomUUID())

        val team2 =
            QueueTeam(
                teamId = teamId2,
                players = team2Players,
                attributes = objectMapper.valueToTree(mapOf("kdr" to 1.0, "elo" to 1.0)),
            )
        val addTeamResult2 = matchmaker.addTeam(team2)

        assertEquals(addTeamResult2.isOk, true)

        val matchmake = matchmaker.matchmake()
        assert(matchmake is MatchMakerResult.MatchMakerFailure)
        assertEquals(
            "Database returned less than invalid number of teams",
            (matchmake as MatchMakerResult.MatchMakerFailure).err.message,
        )
    }

    @Test
    fun `matchmake`() {
        val matchmaker = createMatchmaker()

        val teamId = UUID.randomUUID()

        val team1Players = listOf(UUID.randomUUID(), UUID.randomUUID())

        val team1 =
            QueueTeam(
                teamId = teamId,
                players = team1Players,
                attributes = objectMapper.valueToTree(mapOf("kdr" to 1.0, "elo" to 1.0)),
            )

        val addTeamResult = matchmaker.addTeam(team1)

        assertEquals(addTeamResult.isOk, true)

        val teamId2 = UUID.randomUUID()
        val team2Players = listOf(UUID.randomUUID(), UUID.randomUUID())

        val team2 =
            QueueTeam(
                teamId = teamId2,
                players = team2Players,
                attributes = objectMapper.valueToTree(mapOf("kdr" to 1.0, "elo" to 1.0)),
            )
        val addTeamResult2 = matchmaker.addTeam(team2)

        assertEquals(addTeamResult2.isOk, true)

        val matchmakeResult = matchmaker.matchmake()

        assertTrue { matchmakeResult is MatchMakerResult.MatchMakerSuccess }
        assertEquals(
            listOf(listOf(team1), listOf(team2)),
            (matchmakeResult as MatchMakerResult.MatchMakerSuccess).teams,
        )
    }

    @Test
    fun `test creator`() {
        val creator = VectorSearchCreator(dataSource)

        val matchmaker =
            creator.createMatchmaker(
                objectMapper.valueToTree(
                    mapOf(
                        "name" to "test",
                        "minPoolSize" to 2,
                        "teamSize" to 2,
                        "numberOfTeams" to 2,
                        "requiredStatistics" to listOf("kdr", "elo"),
                    ),
                ),
            )

        assertTrue { matchmaker.isOk }
    }

    @Test
    fun `test creator missing name`() {
        val creator = VectorSearchCreator(dataSource)

        val matchmaker =
            creator.createMatchmaker(
                objectMapper.valueToTree(
                    mapOf(
                        "minPoolSize" to 2,
                        "teamSize" to 2,
                        "numberOfTeams" to 2,
                        "requiredStatistics" to listOf("kdr", "elo"),
                    ),
                ),
            )

        assertEquals(matchmaker.error.message, "Missing 'name' field")
    }

    @Test
    fun `test creator missing minPoolSize`() {
        val creator = VectorSearchCreator(dataSource)

        val matchmaker =
            creator.createMatchmaker(
                objectMapper.valueToTree(
                    mapOf(
                        "name" to "test",
                        "teamSize" to 2,
                        "numberOfTeams" to 2,
                        "requiredStatistics" to listOf("kdr", "elo"),
                    ),
                ),
            )

        assertEquals(matchmaker.error.message, "Missing 'minPoolSize' field")
    }

    @Test
    fun `test creator missing teamSize`() {
        val creator = VectorSearchCreator(dataSource)

        val matchmaker =
            creator.createMatchmaker(
                objectMapper.valueToTree(
                    mapOf(
                        "name" to "test",
                        "minPoolSize" to 2,
                        "numberOfTeams" to 2,
                        "requiredStatistics" to listOf("kdr", "elo"),
                    ),
                ),
            )

        assertEquals(matchmaker.error.message, "Missing 'teamSize' field")
    }

    @Test
    fun `test creator missing numberOfTeams`() {
        val creator = VectorSearchCreator(dataSource)

        val matchmaker =
            creator.createMatchmaker(
                objectMapper.valueToTree(
                    mapOf(
                        "name" to "test",
                        "minPoolSize" to 2,
                        "teamSize" to 2,
                        "requiredStatistics" to listOf("kdr", "elo"),
                    ),
                ),
            )

        assertEquals(matchmaker.error.message, "Missing 'numberOfTeams' field")
    }

    @Test
    fun `test creator missing requiredStatistics`() {
        val creator = VectorSearchCreator(dataSource)

        val matchmaker =
            creator.createMatchmaker(
                objectMapper.valueToTree(
                    mapOf(
                        "name" to "test",
                        "minPoolSize" to 2,
                        "teamSize" to 2,
                        "numberOfTeams" to 2,
                    ),
                ),
            )

        assertEquals(matchmaker.error.message, "Missing 'requiredStatistics' field")
    }

    @Test
    fun `test serialize`() {
        val creator = VectorSearchCreator(dataSource)

        val matchmaker =
            creator.createMatchmaker(
                objectMapper.valueToTree(
                    mapOf(
                        "name" to "test",
                        "minPoolSize" to 2,
                        "teamSize" to 2,
                        "numberOfTeams" to 2,
                        "requiredStatistics" to listOf("kdr", "elo"),
                    ),
                ),
            )

        val serialized = creator.serialize(matchmaker.value)

        assertTrue { serialized.isOk }
    }

    @Test
    fun `test serialize invalid matchmaker`() {
        val creator = VectorSearchCreator(dataSource)

        val serialized =
            creator.serialize(
                object : Matchmaker("pgvector", "test") {
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
            )

        assertEquals(serialized.error.message, "Matchmaker is not a PostgresVectorSearchMatchmaker")
    }

    @Test
    fun `test creator deserialize`() {
        val creator = VectorSearchCreator(dataSource)

        val matchmaker =
            creator.createMatchmaker(
                objectMapper.valueToTree(
                    mapOf(
                        "name" to "test",
                        "minPoolSize" to 2,
                        "teamSize" to 2,
                        "numberOfTeams" to 2,
                        "requiredStatistics" to listOf("kdr", "elo"),
                    ),
                ),
            )

        val serialized = creator.serialize(matchmaker.value)

        val deserialized = creator.deserialize(serialized.value)

        assertTrue { deserialized.isOk }
    }

    @Test
    fun `test database insert missing property`() {
        val database =
            PostgresVectorDatabase(
                name = "test",
                embeddingNames = listOf("kdr", "elo"),
                datasource = dataSource,
            )

        val result = database.insertData(UUID.randomUUID(), mapOf("kdr" to 1.0))

        assertEquals(result.error.message, "Embedding values must contain all embedding names")
    }

    @Test
    fun `test database sql error`() {
        val database =
            spyk(
                PostgresVectorDatabase(
                    name = "test",
                    embeddingNames = listOf("kdr", "elo"),
                    datasource = dataSource,
                ),
            )

        every { database["getConnection"]() }.throws(SQLException())

        assertTrue { database.createExtension().error is SQLException }
    }

    val matchmakerCounter = AtomicInteger(0)

    fun createMatchmaker(): PostgresVectorSearchMatchmaker =
        PostgresVectorSearchMatchmaker(
            name = "test_${matchmakerCounter.andIncrement}",
            minPoolSize = 2,
            teamSize = 2,
            numberOfTeams = 2,
            hikariDataSource = dataSource,
            requiredStatistics = listOf("kdr", "elo"),
        )
}
