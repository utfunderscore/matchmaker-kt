package org.readutf.matchmaker.matchmaker

import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import org.readutf.matchmaker.queue.QueueTeam
import java.util.UUID

abstract class PooledMatchmaker(
    type: String,
    name: String,
) : Matchmaker(type, name) {
    private val teams = mutableMapOf<UUID, QueueTeam>()

    abstract fun matchmake(teams: List<QueueTeam>): MatchMakerResult

    override fun addTeam(team: QueueTeam): Result<Unit, Throwable> {
        teams.put(team.teamId, team)
        return Ok(Unit)
    }

    override fun removeTeam(teamId: UUID): Result<Unit, Throwable> {
        teams.remove(teamId)
        return Ok(Unit)
    }

    override fun matchmake(): MatchMakerResult = matchmake(teams.values.toList())
}
