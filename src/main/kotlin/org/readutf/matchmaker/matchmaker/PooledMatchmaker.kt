package org.readutf.matchmaker.matchmaker

import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import org.readutf.matchmaker.queue.QueueTeam

abstract class PooledMatchmaker(
    type: String,
    name: String,
) : Matchmaker(type, name) {
    private val teams = mutableListOf<QueueTeam>()

    abstract fun matchmake(teams: List<QueueTeam>): MatchMakerResult

    override fun addTeam(team: QueueTeam): Result<Unit, Throwable> {
        teams.add(team)
        return Ok(Unit)
    }

    override fun removeTeam(team: QueueTeam): Result<Unit, Throwable> {
        teams.remove(team)
        return Ok(Unit)
    }

    override fun matchmake(): MatchMakerResult = matchmake()
}
