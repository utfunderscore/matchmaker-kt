package org.readutf.matchmaker.matchmaker

import org.readutf.matchmaker.queue.QueueTeam

sealed class MatchMakerResult {
    class MatchMakerSuccess(
        result: List<List<QueueTeam>>,
    ) : MatchMakerResult()

    class MatchMakerFailure(
        throwable: Throwable,
    ) : MatchMakerResult()

    class MatchMakerSkip : MatchMakerResult()
}
