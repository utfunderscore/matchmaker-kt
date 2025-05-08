package org.readutf.matchmaker.matchmaker.impl.elo

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import org.readutf.matchmaker.matchmaker.MatchMakerResult
import org.readutf.matchmaker.matchmaker.Matchmaker
import org.readutf.matchmaker.queue.QueueTeam
import org.readutf.matchmaker.utils.containsAllKeys

class EloMatchmaker(
    name: String,
    val rangeExpansionAmount: Int,
    val rangeExpansionTime: Long,
) : Matchmaker("pooled_elo", name) {
    override fun matchmake(teams: Collection<QueueTeam>): MatchMakerResult {
        // Iterate over every team in the queue
        for (team in teams) {
            val range = getRange(team) // Get the elo range for the current team

            for (opponent in teams) { // Iterate over every other team
                if (opponent == team) continue
                val opponentElo = getElo(opponent) // Get the elo of the current opponent

                if (opponentElo in range.min..range.max) { // Check the opponents elo is within the current range
                    return MatchMakerResult.MatchMakerSuccess(listOf(listOf(team), listOf(opponent)))
                }
            }
        }
        return MatchMakerResult.MatchMakerSkip
    }

    override fun validateTeam(team: QueueTeam): Result<Unit, Throwable> {
        if (team.attributes.containsAllKeys("elo")) {
            return Err(Exception("Team must contain a 'elo' attribute"))
        }

        return Ok(Unit)
    }

    private fun getRange(queueTeam: QueueTeam): EloRange {
        val difference = System.nanoTime() - queueTeam.joinedAt
        val elo = getElo(queueTeam)

        val rangeExpansion = (difference / rangeExpansionTime).toInt() * rangeExpansionAmount

        return EloRange(
            queueTeam = queueTeam,
            min = elo - rangeExpansion,
            max = elo + rangeExpansion,
        )
    }

    private fun getElo(queueTeam: QueueTeam): Int = queueTeam.attributes.get("elo").asInt()

    private data class EloRange(
        val queueTeam: QueueTeam,
        val min: Int,
        val max: Int,
    )
}
