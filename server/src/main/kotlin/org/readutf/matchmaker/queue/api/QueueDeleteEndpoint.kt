package org.readutf.matchmaker.queue.api

import io.javalin.http.Context
import io.javalin.http.Handler
import org.readutf.matchmaker.queue.QueueManager
import org.readutf.matchmaker.utils.failure
import org.readutf.matchmaker.utils.success

// /api/queue/{name}
class QueueDeleteEndpoint(
    private val queueManager: QueueManager,
) : Handler {
    override fun handle(ctx: Context) {
        val queueName: String = ctx.pathParam("name")

        if (!queueManager.deleteQueue(queueName)) {
            ctx.failure("Queue $queueName not found")
            return
        }

        ctx.success("Queue $queueName deleted")
    }
}
