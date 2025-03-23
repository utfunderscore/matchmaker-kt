package org.readutf.matchmaker.queue.store.impl

import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import io.github.oshai.kotlinlogging.KotlinLogging
import org.readutf.matchmaker.Application.Companion.objectMapper
import org.readutf.matchmaker.matchmaker.MatchmakerManager
import org.readutf.matchmaker.queue.Queue
import org.readutf.matchmaker.queue.store.QueueStore
import java.nio.file.Path

class JsonQueueStore(
    private val filePath: Path,
) : QueueStore {
    private val logger = KotlinLogging.logger { }

    private val file = filePath.toFile().resolve("queues.json")

    init {
        if (!(file.exists())) file.createNewFile()
    }

    override fun saveQueues(queues: List<Queue>): Result<Unit, Throwable> {
        objectMapper.writeValue(file.bufferedWriter(), queues.map { QueueData(it.name, it.matchmaker.name) })
        return Ok(Unit)
    }

    override fun loadQueues(matchmakerManager: MatchmakerManager): Result<List<Queue>, Throwable> {
        val queues = mutableListOf<Queue>()

        objectMapper.readTree(file).forEach {
            try {
                print(it)

                val queueData = objectMapper.readValue(it.toString(), QueueData::class.java)
                val matchmaker =
                    matchmakerManager.getMatchmaker(queueData.matchmaker) ?: let {
                        logger.error { "Matchmaker ${queueData.matchmaker} not found" }
                        return@forEach
                    }
                queues.add(Queue(queueData.name, matchmaker))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        logger.info { "Loaded ${queues.size} queues" }
        return Ok(queues)
    }

    private class QueueData(
        val name: String,
        val matchmaker: String,
    )
}
