package org.readutf.matchmaker.queue

import com.fasterxml.jackson.annotation.JsonIgnore
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.getOrElse
import org.readutf.matchmaker.matchmaker.Matchmaker
import java.util.UUID

/**
 * Represents a queue of teams waiting to be matched
 * @param name The name of the queue
 * @param matchmaker The matchmaker that will match the teams
 */
open class Queue(
    val name: String,
    @JsonIgnore val matchmaker: Matchmaker,
) {
    /**
     * Stores the teams currently waiting to be matched
     */
    private val inQueue: MutableMap<UUID, QueueTeam> = mutableMapOf()

    @Synchronized
    fun addTeam(team: QueueTeam): Result<Unit, Throwable> {
        // Check if the team is already in the queue
        if (inQueue.contains(team.teamId)) {
            return Err(Exception("Team is already in queue"))
        }
        // Check if any players in the team are already in the queue
        if (inQueue.values.any { it.players.any { player -> team.players.contains(player) } }) {
            return Err(Exception("One or more players in the team are already in the queue"))
        }

        matchmaker.addTeam(team).getOrElse { return Err(it) }

        inQueue[team.teamId] = team
        return Ok(Unit)
    }

    @Synchronized
    fun tickQueue() {
        matchmaker.matchmake()
    }

    @Synchronized
    fun removeTeam(team: QueueTeam): Result<Unit, Throwable> {
        if (!inQueue.contains(team.teamId)) {
            return Err(Exception("Team is not in queue"))
        }

        inQueue.remove(team.teamId)
        return Ok(Unit)
    }
}
