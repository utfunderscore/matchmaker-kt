package org.readutf.matchmaker.matchmaker

import org.readutf.matchmaker.queue.QueueTeam

sealed class MatchMakerResult {
    class MatchMakerSuccess(
        val teams: List<List<QueueTeam>>,
    ) : MatchMakerResult()

    class MatchMakerFailure(
        val err: Throwable,
    ) : MatchMakerResult()

    class MatchMakerSkip : MatchMakerResult()
}
