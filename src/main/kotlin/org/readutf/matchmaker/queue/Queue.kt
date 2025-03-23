package org.readutf.matchmaker.queue

import com.fasterxml.jackson.annotation.JsonIgnore
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.getOrElse
import com.github.michaelbull.result.onFailure
import io.github.oshai.kotlinlogging.KotlinLogging
import org.readutf.matchmaker.matchmaker.MatchMakerResult
import org.readutf.matchmaker.matchmaker.Matchmaker
import java.util.UUID
import java.util.function.Consumer

/**
 * Represents a queue of teams waiting to be matched
 * @param name The name of the queue
 * @param matchmaker The matchmaker that will match the teams
 */
open class Queue(
    val name: String,
    @JsonIgnore val matchmaker: Matchmaker,
) {
    @JsonIgnore private val logger = KotlinLogging.logger { }

    @JsonIgnore private val listeners = mutableMapOf<UUID, Consumer<List<List<QueueTeam>>>>()

    /**
     * Stores the teams currently waiting to be matched
     */
    private val inQueue: MutableMap<UUID, QueueTeam> = mutableMapOf()

    @Synchronized
    fun addTeam(
        team: QueueTeam,
        callback: Consumer<List<List<QueueTeam>>>,
    ): Result<Unit, Throwable> {
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
        listeners[team.teamId] = callback

        return Ok(Unit)
    }

    @Synchronized
    fun tickQueue() {
        val matchmake = matchmaker.matchmake()
        when (matchmake) {
            is MatchMakerResult.MatchMakerFailure -> {
                logger.error(matchmake.err) { "Matchmaker failure" }
            }

            is MatchMakerResult.MatchMakerSuccess -> {
                val teams = matchmake.teams

                val callback = listeners.values.distinct()

                callback.forEach {
                    it.accept(teams)
                }

                teams.flatten().forEach { team ->
                    removeTeam(team).onFailure { err ->
                        logger.error(err) { "Failed to remove team from queue" }
                    }
                }
            }
            else -> {
                return
            }
        }
    }

    @Synchronized
    fun removeTeam(team: QueueTeam): Result<Unit, Throwable> {
        if (!inQueue.contains(team.teamId)) {
            return Err(Exception("Team is not in queue"))
        }

        inQueue.remove(team.teamId)
        listeners.remove(team.teamId)
        return Ok(Unit)
    }
}
