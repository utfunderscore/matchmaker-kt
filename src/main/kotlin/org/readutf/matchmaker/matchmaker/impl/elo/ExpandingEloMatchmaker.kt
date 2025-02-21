package org.readutf.matchmaker.matchmaker.impl.elo

import org.readutf.matchmaker.matchmaker.MatchMakerResult
import org.readutf.matchmaker.matchmaker.PooledMatchmaker
import org.readutf.matchmaker.queue.QueueTeam
import java.util.UUID
import kotlin.math.pow

class ExpandingEloMatchmaker(
    name: String,
    val rangeIncrement: Int,
    val rangeIncreaseRate: Int,
    val kFactor: Int,
) : PooledMatchmaker("expanding_elo", name) {
    val eloQueueTeams = mutableMapOf<UUID, QueueTeam>()

    override fun matchmake(teams: List<QueueTeam>): MatchMakerResult {
        TODO()
    }

    data class EloQueueTeam(
        val team: QueueTeam,
        val elo: Int,
    )

    fun calculateNewElo(
        player1Elo: Int,
        player2Elo: Int,
        player1Score: Double,
        kFactor: Int = 32,
    ): Pair<Int, Int> {
        val expectedScore1 = 1 / (1 + 10.0.pow((player2Elo - player1Elo) / 400.0))
        val expectedScore2 = 1 / (1 + 10.0.pow((player1Elo - player2Elo) / 400.0))

        val newPlayer1Elo = player1Elo + (kFactor * (player1Score - expectedScore1)).toInt()
        val newPlayer2Elo = player2Elo + (kFactor * ((1 - player1Score) - expectedScore2)).toInt()

        return Pair(newPlayer1Elo, newPlayer2Elo)
    }
}
