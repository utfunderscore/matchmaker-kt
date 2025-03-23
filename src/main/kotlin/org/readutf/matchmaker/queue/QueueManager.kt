package org.readutf.matchmaker.queue

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.getOrThrow
import org.readutf.matchmaker.matchmaker.Matchmaker
import org.readutf.matchmaker.matchmaker.MatchmakerManager
import org.readutf.matchmaker.queue.store.QueueStore
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import java.util.function.Consumer

class QueueManager(
    val queueStore: QueueStore,
) {
    private val queues: MutableMap<String, Queue> = mutableMapOf<String, Queue>()

    private val queueExecutors = mutableMapOf<String, ScheduledExecutorService>()

    fun getQueue(queueName: String): Queue? = queues[queueName]

    fun createQueue(
        name: String,
        matchmaker: Matchmaker,
    ): Result<Queue, Throwable> {
        if (queues.containsKey(name)) return Err(Exception("Queue already exists"))

        val queue = Queue(name, matchmaker)
        registerQueue(queue)

        return Ok(queue)
    }

    private fun registerQueue(queue: Queue) {
        queues[queue.name] = queue

        val executor = Executors.newSingleThreadScheduledExecutor()
        queueExecutors[queue.name] = executor

        executor.scheduleAtFixedRate({
            println("Ticking queue ${queue.name}")
            queue.tickQueue()
        }, 0, 1, TimeUnit.SECONDS)
    }

    /**
     * Adds the given team to the queue with the given name
     * This can fail if the queue doesn't exist, or if the team is already in the queue
     * or if the matchmaker cannot support the given team
     */
    fun joinQueue(
        queueName: String,
        team: QueueTeam,
        callback: Consumer<List<List<QueueTeam>>>,
    ): Result<Unit, Throwable> {
        val queue = queues[queueName] ?: return Err(Exception("Queue does not exist"))

        return queue.addTeam(team, callback)
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

    fun loadQueues(matchmakerManager: MatchmakerManager) {
        for (queue in queueStore.loadQueues(matchmakerManager).getOrThrow()) {
            registerQueue(queue)
        }
    }

    fun getQueues(): List<Queue> = queues.values.toList()

    fun shutdown() {
        queueStore.saveQueues(queues.values.toList()).getOrThrow()
    }

    fun deleteQueue(queueName: String): Boolean = queues.remove(queueName) != null
}
