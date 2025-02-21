package org.readutf.matchmaker.matchmaker.impl.pgvector

import com.github.michaelbull.result.onFailure
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName
import org.testng.Assert
import org.testng.annotations.Test
import java.io.BufferedReader
import java.io.FileReader
import java.util.UUID

class VectorSearchMatchmakerTest {
    var postgres = PostgreSQLContainer(DockerImageName.parse("pgvector/pgvector:pg17"))

    init {
        postgres.start()
    }

    val hikariConfig =
        HikariConfig().apply {
            jdbcUrl = postgres.jdbcUrl
            username = postgres.username
            password = postgres.password
        }

    val dataSource = HikariDataSource(hikariConfig)

    val postgresVectorDatabase =
        PostgresVectorDatabase(
            name = "vectorsearch_test",
            embeddingNames = listOf("embedding1", "embedding2"),
            dataSource,
        )

    @Test
    fun createTableStatement() {
        val expected =
            "CREATE TABLE vectorsearch_test (player_id uuid PRIMARY KEY, embedding1 FLOAT NOT NULL, embedding2 FLOAT NOT NULL, embeddings vector(2));"
        val actual = postgresVectorDatabase.createTableStatement()

        Assert.assertEquals(expected, actual)
    }

    @Test
    fun createExtension() {
        val createExtension = postgresVectorDatabase.createExtension()

        createExtension.onFailure {
            it.printStackTrace()
        }
        Assert.assertTrue(createExtension.isOk)
    }

    @Test
    fun createTable() {
        postgresVectorDatabase.createExtension()

        val createTable = postgresVectorDatabase.createTable()

        createTable.onFailure {
            it.printStackTrace()
        }
        Assert.assertTrue(createTable.isOk)
    }

    @Test(dependsOnMethods = ["createTable", "createExtension"])
    fun insertPlayer() {
        val insertPlayer =
            postgresVectorDatabase.insertData(
                playerId = UUID.randomUUID(),
                embeddingValues = mapOf("embedding1" to 1.0, "embedding2" to 2.0),
            )

        insertPlayer.onFailure {
            it.printStackTrace()
        }
    }

    val populateDatabase =
        PostgresVectorDatabase(
            name = "population_test",
            embeddingNames =
                listOf(
                    "lifetimeLevel",
                    "lifetimeKd",
                    "lifetimeKills",
                    "lifetimeDeaths",
                    "lifetimeKillsPerMatch",
                    "lifetimeHeadshotPct",
                    "lifetimeMatchesWon",
                    "lifetimeMatchesLost",
                    "lifetimeMatchesAbandoned",
                    "lifetimeMatchesPlayed",
                    "lifetimeMatchWinPct",
                    "lifetimeTimePlayed",
                ),
            datasource = dataSource,
        )

    @Test(dependsOnMethods = ["insertPlayer"])
    fun testPopulateDatabase() {
        // read features.csv line by line
        val bufferedReader = BufferedReader(FileReader("src/test/resources/features.csv"))

        populateDatabase.createTable()

        bufferedReader.useLines { lines ->
            lines.forEachIndexed { index, line ->
                if (index != 0 && line.isNotBlank()) {
                    val values = line.split(",")
                    val playerId = UUID.fromString(values[0])
                    val lifetimeLevel = values[1].toDouble()
                    val lifetimeKd = values[2].toDouble()
                    val lifetimeKills = values[3].toDouble()
                    val lifetimeDeaths = values[4].toDouble()
                    val lifetimeKillsPerMatch = values[5].toDouble()
                    val lifetimeHeadshotPct = values[6].toDouble()
                    val lifetimeMatchesWon = values[7].toDouble()
                    val lifetimeMatchesLost = values[8].toDouble()
                    val lifetimeMatchesAbandoned = values[9].toDouble()
                    val lifetimeMatchesPlayed = values[10].toDouble()
                    val lifetimeMatchWinPct = values[11].toDouble()
                    val lifetimeTimePlayed = values[12].toDouble()

                    populateDatabase.insertData(
                        playerId = playerId,
                        mapOf(
                            "lifetimeLevel" to lifetimeLevel,
                            "lifetimeKd" to lifetimeKd,
                            "lifetimeKills" to lifetimeKills,
                            "lifetimeDeaths" to lifetimeDeaths,
                            "lifetimeKillsPerMatch" to lifetimeKillsPerMatch,
                            "lifetimeHeadshotPct" to lifetimeHeadshotPct,
                            "lifetimeMatchesWon" to lifetimeMatchesWon,
                            "lifetimeMatchesLost" to lifetimeMatchesLost,
                            "lifetimeMatchesAbandoned" to lifetimeMatchesAbandoned,
                            "lifetimeMatchesPlayed" to lifetimeMatchesPlayed,
                            "lifetimeMatchWinPct" to lifetimeMatchWinPct,
                            "lifetimeTimePlayed" to lifetimeTimePlayed,
                        ),
                    )
                }
            }
        }
    }

    @Test(dependsOnMethods = ["testPopulateDatabase"])
    fun queryNearbyPlayers() {
        println(populateDatabase.getNearby(UUID.fromString("7453d100-e009-4be1-bbec-a52d1f011dce"), 5))
    }
}
