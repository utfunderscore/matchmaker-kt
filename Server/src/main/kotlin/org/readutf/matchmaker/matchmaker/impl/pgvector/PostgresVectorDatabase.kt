package org.readutf.matchmaker.matchmaker.impl.pgvector

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.runCatching
import com.zaxxer.hikari.HikariDataSource
import java.sql.SQLException
import java.util.UUID

class PostgresVectorDatabase(
    val name: String,
    val embeddingNames: List<String>,
    val datasource: HikariDataSource,
) {
    fun init() {
        createExtension()
        createTable()
    }

    fun getNearby(
        playerId: UUID,
        limit: Int,
    ): Result<List<UUID>, Throwable> {
        val result =
            getConnection().use { conn ->
                val sql =
                    "SELECT player_id, embeddings <-> (SELECT embeddings FROM $name WHERE player_id = '$playerId') AS distance FROM $name ORDER BY distance LIMIT $limit;"
                val result = mutableListOf<UUID>()

                conn.prepareStatement(sql).use { statement ->
                    statement.executeQuery().use { resultSet ->
                        while (resultSet.next()) {
                            result.add(UUID.fromString(resultSet.getString("player_id")))
                        }
                    }
                }
                return@use result
            }

        return Ok(result)
    }

    fun insertData(
        playerId: UUID,
        embeddingValues: Map<String, Double>,
    ): Result<Unit, Throwable> {
        if (embeddingNames.any { !embeddingValues.containsKey(it) } || embeddingValues.any { it.key !in embeddingNames }) {
            return Err(IllegalArgumentException("Embedding values must contain all embedding names"))
        }

        return runCatching {
            getConnection().use { conn ->
                val sql = "INSERT INTO $name (player_id, ${embeddingNames.joinToString(
                    ", ",
                )}) VALUES ('$playerId', ${embeddingValues.values.joinToString(", ")})"

                println(sql)

                conn.prepareStatement(sql).use { statement ->
                    statement.executeUpdate()
                }
            }
        }
    }

    fun removeData(playerId: UUID): Result<Unit, Throwable> =
        runCatching {
            getConnection().use { conn ->
                val sql = "DELETE FROM $name WHERE player_id = '$playerId'"

                conn.prepareStatement(sql).use { statement ->
                    statement.executeUpdate()
                }
            }
        }

    fun createExtension(): Result<Unit, Throwable> {
        val sql = "CREATE EXTENSION IF NOT EXISTS vector;"
        try {
            getConnection().use { conn ->
                conn.prepareStatement(sql).use { statement ->
                    statement.executeUpdate()
                }
            }
        } catch (e: SQLException) {
            return Err(e)
        }
        return Ok(Unit)
    }

    fun createTable(): Result<Unit, Throwable> {
        getConnection().use { conn ->
            val sql = createTableStatement()
            try {
                conn.prepareStatement(sql).use { statement ->
                    statement.executeUpdate()
                }
            } catch (e: SQLException) {
                return Err(e)
            }
        }

        return Ok(Unit)
    }

    fun dropTable() {
        getConnection().use { conn ->
            val sql = "DROP TABLE IF EXISTS $name;"
            conn.prepareStatement(sql).use { statement ->
                statement.executeUpdate()
            }
        }
    }

    fun createTableStatement(): String {
        val embeddingPart = embeddingNames.joinToString(", ") { "$it FLOAT NOT NULL" }
        return "CREATE TABLE $name (player_id uuid PRIMARY KEY, $embeddingPart, embeddings vector(${embeddingNames.size}));"
    }

    private fun getConnection() = datasource.connection
}
