package org.readutf.matchmaker.matchmaker.impl.pgvector

import com.fasterxml.jackson.databind.JsonNode
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.runCatching
import com.zaxxer.hikari.HikariDataSource
import org.readutf.matchmaker.Application
import org.readutf.matchmaker.matchmaker.Matchmaker
import org.readutf.matchmaker.matchmaker.MatchmakerCreator

class VectorSearchCreator(
    val hikariDataSource: HikariDataSource,
) : MatchmakerCreator {
    override fun createMatchmaker(jsonNode: JsonNode): Result<Matchmaker, Throwable> {
        val name = jsonNode["name"]?.asText() ?: return Err(IllegalArgumentException("Missing 'name' field"))
        val minPoolSize =
            jsonNode["minPoolSize"]?.asInt() ?: return Err(IllegalArgumentException("Missing 'minPoolSize' field"))
        val teamSize =
            jsonNode["teamSize"]?.asInt() ?: return Err(IllegalArgumentException("Missing 'teamSize' field"))
        val numberOfTeams =
            jsonNode["numberOfTeams"]?.asInt()
                ?: return Err(IllegalArgumentException("Missing 'numberOfTeams' field"))
        val requiredStatistics =
            jsonNode["requiredStatistics"]?.map { it.asText() }
                ?: return Err(IllegalArgumentException("Missing 'requiredStatistics' field"))

        return Ok(
            PostgresVectorSearchMatchmaker(
                name = name,
                minPoolSize = minPoolSize,
                teamSize = teamSize,
                numberOfTeams = numberOfTeams,
                hikariDataSource = hikariDataSource,
                requiredStatistics = requiredStatistics,
            ),
        )
    }

    override fun serialize(matchmaker: Matchmaker): Result<JsonNode, Throwable> {
        val matchmaker =
            matchmaker as? PostgresVectorSearchMatchmaker
                ?: return Err(IllegalArgumentException("Matchmaker is not a PostgresVectorSearchMatchmaker"))
        return runCatching {
            Application.objectMapper.valueToTree(
                mapOf(
                    "name" to matchmaker.name,
                    "minPoolSize" to matchmaker.minPoolSize,
                    "teamSize" to matchmaker.teamSize,
                    "numberOfTeams" to matchmaker.numberOfTeams,
                    "requiredStatistics" to matchmaker.requiredStatistics,
                ),
            )
        }
    }

    override fun deserialize(jsonNode: JsonNode): Result<Matchmaker, Throwable> = createMatchmaker(jsonNode)
}
