package org.readutf.matchmaker.queue.store

import com.github.michaelbull.result.Result
import org.readutf.matchmaker.matchmaker.MatchmakerManager
import org.readutf.matchmaker.queue.Queue

interface QueueStore {
    fun saveQueues(queues: List<Queue>): Result<Unit, Throwable>

    fun loadQueues(matchmakerManager: MatchmakerManager): Result<List<Queue>, Throwable>
}
