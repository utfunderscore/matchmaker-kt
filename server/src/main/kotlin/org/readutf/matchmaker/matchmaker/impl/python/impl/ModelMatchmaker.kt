package org.readutf.matchmaker.matchmaker.impl.python.impl

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Result
import org.readutf.matchmaker.matchmaker.MatchMakerResult
import org.readutf.matchmaker.matchmaker.impl.python.PythonMatchmaker
import org.readutf.matchmaker.queue.QueueTeam
import org.readutf.matchmaker.utils.containsAllKeys

class ModelMatchmaker(
    name: String,
    mode: String,
    private val batchSize: Int,
) : PythonMatchmaker("knearestneighbor", name, "knn") {
    private val requiredAttributes =
        listOf(
            "lifetimeKdAvg",
            "lifetimeKillsAvg",
            "lifetimeDeathsAvg",
            "lifetimeKillsPerMatchAvg",
            "lifetimeHeadshotPctAvg",
            "lifetimeMatchesWonAvg",
            "lifetimeMatchesLostAvg",
            "lifetimeMatchesAbandonedAvg",
            "lifetimeMatchWinPctAvg",
            "lastSeasonKillsAvg",
            "lastSeasonDeathsAvg",
            "lastSeasonKillsPerMatchAvg",
            "lastSeasonMatchesWonAvg",
            "lastSeasonMatchesLostAvg",
            "lastSeasonMatchesAbandonedAvg",
            "lastSeasonMatchWinPctAvg",
            "lastSeasonBestRankIdAvg",
        )

    override fun addTeam(team: QueueTeam): Result<Unit, Throwable> {
        if (!team.attributes.containsAllKeys(requiredAttributes)) {
            return Err(IllegalArgumentException("Team does not contain all required attributes"))
        }

        return super.addTeam(team)
    }

    override fun matchmake(teams: List<QueueTeam>): MatchMakerResult {
        if (teams.size < batchSize) {
            return MatchMakerResult.skip()
        }

        return super.matchmake(teams)
    }

    override fun createRequest(): MutableMap<String, Any?> = mutableMapOf()
}
