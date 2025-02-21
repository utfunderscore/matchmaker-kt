package org.readutf.matchmaker.queue

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.getOrThrow
import org.readutf.matchmaker.matchmaker.Matchmaker
import org.readutf.matchmaker.queue.store.QueueStore

class QueueManager(
    val queueStore: QueueStore,
) {
    private val queues =
        queueStore
            .loadQueues()
            .getOrThrow()
            .associateBy { it.name }
            .toMutableMap()

    fun getQueue(queueName: String): Queue? = queues[queueName]

    fun createQueue(
        name: String,
        matchmaker: Matchmaker,
    ): Result<Queue, Throwable> {
        if (queues.containsKey(name)) return Err(Exception("Queue already exists"))

        val queue = Queue(name, matchmaker)
        queues[name] = queue
        return Ok(queue)
    }

    /**
     * Adds the given team to the queue with the given name
     * This can fail if the queue doesn't exist, or if the team is already in the queue
     * or if the matchmaker cannot support the given team
     */
    fun joinQueue(
        queueName: String,
        team: QueueTeam,
    ): Result<Unit, Throwable> {
        val queue = queues[queueName] ?: return Err(Exception("Queue does not exist"))
        return queue.addTeam(team)
    }

    /**
     * Removes the given team from the queue with the given name
     */
    fun leaveQueue(
        queueName: String,
        team: QueueTeam,
    ): Result<Unit, Throwable> {
        val queue = queues[queueName] ?: return Err(Exception("Queue does not exist"))
        return queue.removeTeam(team)
    }

    fun getQueues(): List<Queue> = queues.values.toList()

    fun shutdown() {
        queueStore.saveQueues(queues.values.toList()).getOrThrow()
    }

    fun deleteQueue(queueName: String): Boolean = queues.remove(queueName) != null
}
