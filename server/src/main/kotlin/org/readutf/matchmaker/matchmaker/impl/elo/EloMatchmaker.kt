package org.readutf.matchmaker.matchmaker.impl.elo

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Result
import org.readutf.matchmaker.matchmaker.MatchMakerResult
import org.readutf.matchmaker.matchmaker.PooledMatchmaker
import org.readutf.matchmaker.queue.QueueTeam
import org.readutf.matchmaker.utils.containsAllKeys
import kotlin.math.abs

class EloMatchmaker(
    name: String,
    private val rangeExpansionAmount: Int,
    private val rangeExpansionTime: Long,
) : PooledMatchmaker("pooled_elo", name) {
    override fun matchmake(teams: List<QueueTeam>): MatchMakerResult {
        val ranges = teams.map { team -> getRange(team) }

        val intersectingPairs = findIntersectingRanges(ranges)

        val (first, second) =
            intersectingPairs.minByOrNull { pair -> abs(pair.first.original - pair.second.original) }
                ?: return MatchMakerResult.MatchMakerSkip

        return MatchMakerResult.MatchMakerSuccess(listOf(listOf(first.queueTeam), listOf(second.queueTeam)))
    }

    override fun addTeam(team: QueueTeam): Result<Unit, Throwable> {
        if (team.attributes.containsAllKeys("elo")) {
            return Err(Exception("Team must contain a 'elo' attribute"))
        }

        return super.addTeam(team)
    }

    private fun findIntersectingRanges(ranges: List<EloRange>): List<Pair<EloRange, EloRange>> {
        val intersectingPairs = mutableListOf<Pair<EloRange, EloRange>>()
        for (i in ranges.indices) {
            for (j in i + 1 until ranges.size) {
                if (ranges[i].intersects(ranges[j])) {
                    intersectingPairs.add(ranges[i] to ranges[j])
                }
            }
        }
        return intersectingPairs
    }

    private fun getRange(queueTeam: QueueTeam): EloRange {
        val difference = System.nanoTime() - queueTeam.joinedAt
        val elo = queueTeam.attributes.get("elo").asInt()

        val rangeExpansion = (difference / rangeExpansionTime).toInt() * rangeExpansionAmount

        return EloRange(
            queueTeam = queueTeam,
            min = elo - rangeExpansion,
            max = elo + rangeExpansion,
        )
    }

    private data class EloRange(
        val queueTeam: QueueTeam,
        val min: Int,
        val max: Int,
    ) {
        val original = (min + max) / 2

        fun intersects(other: EloRange): Boolean = (min <= other.max && other.min <= max)
    }
}
