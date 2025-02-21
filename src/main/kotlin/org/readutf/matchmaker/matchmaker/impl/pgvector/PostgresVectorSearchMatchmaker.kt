package org.readutf.matchmaker.matchmaker.impl.pgvector

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.getOrElse
import com.github.michaelbull.result.onSuccess
import com.zaxxer.hikari.HikariDataSource
import org.readutf.matchmaker.matchmaker.MatchMakerResult
import org.readutf.matchmaker.matchmaker.Matchmaker
import org.readutf.matchmaker.queue.QueueTeam
import org.readutf.matchmaker.utils.containsAllKeys
import java.util.UUID

class PostgresVectorSearchMatchmaker(
    name: String,
    val minPoolSize: Int,
    val teamSize: Int,
    val numberOfTeams: Int,
    hikariDataSource: HikariDataSource,
    val requiredStatistics: List<String>,
) : Matchmaker("pgvector", name) {
    val postgresVersionDatabase =
        PostgresVectorDatabase(
            name = name,
            embeddingNames = requiredStatistics,
            datasource = hikariDataSource,
        )

    val joinOrder = LinkedHashMap<UUID, QueueTeam>()

    override fun addTeam(team: QueueTeam): Result<Unit, Throwable> {
        if (team.players.size != teamSize) {
            return Err(IllegalArgumentException("Team size must be $teamSize"))
        }
        if (!team.attributes.containsAllKeys(*requiredStatistics.toTypedArray())) {
            return Err(IllegalArgumentException("Team must contain all required statistics"))
        }

        val embeddings = requiredStatistics.associateWith { team.attributes[it]?.doubleValue() ?: 0.0 }

        return postgresVersionDatabase.insertData(team.teamId, embeddings).onSuccess {
            joinOrder.put(team.teamId, team)
        }
    }

    override fun removeTeam(team: QueueTeam): Result<Unit, Throwable> =
        postgresVersionDatabase.removeData(team.teamId).onSuccess {
            joinOrder.remove(team.teamId)
        }

    override fun matchmake(): MatchMakerResult {
        if (joinOrder.size < numberOfTeams || joinOrder.isEmpty()) {
            return MatchMakerResult.MatchMakerSkip()
        }

        val firstJoined = joinOrder.firstEntry()
        val teams = listOf<List<QueueTeam>>()

        val nearbyPlayers =
            postgresVersionDatabase
                .getNearby(firstJoined.key, numberOfTeams)
                .getOrElse { return MatchMakerResult.MatchMakerFailure(it) }
                .map {
                    joinOrder[it]
                        ?: return MatchMakerResult.MatchMakerFailure(IllegalArgumentException("Player not found"))
                }.map { listOf(it) }

        if (nearbyPlayers.size < numberOfTeams) {
            return MatchMakerResult.MatchMakerSkip()
        }

        return MatchMakerResult.MatchMakerSuccess(teams)
    }
}
