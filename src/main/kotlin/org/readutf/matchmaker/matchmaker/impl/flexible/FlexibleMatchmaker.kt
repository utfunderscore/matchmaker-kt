package org.readutf.matchmaker.matchmaker.impl.flexible

import org.readutf.matchmaker.matchmaker.MatchMakerResult
import org.readutf.matchmaker.matchmaker.PooledMatchmaker
import org.readutf.matchmaker.queue.QueueTeam
import org.readutf.matchmaker.utils.AddendFinder

/**
 * Takes a set of teams of varying sizes and attempts to match them into teams of the target size.
 * The original teams are kept intact and are not modified.
 */
class FlexibleMatchmaker(
    name: String,
    val targetTeamSize: Int,
    val minTeamSize: Int,
    val maxTeamSize: Int,
    val numberOfTeams: Int,
) : PooledMatchmaker("flexible", name) {
    /**
     * A set of all valid team sizes for the target team size.
     */
    private val validTeamComposition: Set<List<Int>> = AddendFinder.findUniqueAddends(targetTeamSize)

    override fun matchmake(teams: List<QueueTeam>): MatchMakerResult {
        if (teams.sumOf { it.players.size } != targetTeamSize * numberOfTeams) {
            return MatchMakerResult.MatchMakerSkip()
        }
        val teamsBySize = teams.groupBy { it.players.size }.map { (size, teams) -> size to ArrayDeque(teams) }.toMap()

        val results = mutableListOf<List<QueueTeam>>()

        for (i in 0 until numberOfTeams) {
            val teamComposition =
                validTeamComposition.firstOrNull { teamSizes ->
                    teamSizes.all { teamSize -> teamsBySize.contains(teamSize) }
                }

            if (teamComposition == null) {
                return MatchMakerResult.MatchMakerSkip()
            }

            results.add(teamComposition.map { teamSize -> teamsBySize[teamSize]!!.first() })
        }
        return MatchMakerResult.MatchMakerSuccess(results)
    }
}
