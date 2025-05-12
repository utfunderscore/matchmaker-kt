package org.readutf.matchmaker.matchmaker

import org.readutf.matchmaker.queue.QueueTeam

sealed class MatchMakerResult {
    class MatchMakerSuccess(
        val teams: List<List<QueueTeam>>,
    ) : MatchMakerResult()

    class MatchMakerFailure(
        val err: Throwable,
    ) : MatchMakerResult()

    data object MatchMakerSkip : MatchMakerResult()

    companion object {
        fun failure(err: Throwable): MatchMakerResult = MatchMakerFailure(err)

        fun skip(): MatchMakerResult = MatchMakerSkip

        fun success(teams: List<List<QueueTeam>>): MatchMakerResult = MatchMakerSuccess(teams)
    }
}
