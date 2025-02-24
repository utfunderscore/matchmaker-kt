package org.readutf.matchmaker.matchmaker

import com.github.michaelbull.result.Result
import org.readutf.matchmaker.queue.QueueTeam
import java.util.UUID

/**
 * Implementations of the matchmaker class are responsible for taking the given set of teams,
 * and producing a valid set of teams to be matched against each other. The number of teams is decided by
 * the matchmaker implementation. The matchmaker should also validate the teams to ensure they are valid.
 *
 * If an error occurs during the matchmaking process, the matchmaker should return a [MatchMakerResult.MatchMakerFailure] result.
 * If the matchmaker is unable to produce a valid set of teams, it should return a [MatchMakerResult.MatchMakerSkip] result.
 * Otherwise, the matchmaker should return a [MatchMakerResult.MatchMakerFailure] result containing the matched teams.
 */
abstract class Matchmaker(
    val type: String,
    val name: String,
) {
    /**
     * Produces a set of teams to be matched against each other.
     */
    abstract fun matchmake(): MatchMakerResult

    abstract fun addTeam(team: QueueTeam): Result<Unit, Throwable>

    abstract fun removeTeam(teamId: UUID): Result<Unit, Throwable>
}
