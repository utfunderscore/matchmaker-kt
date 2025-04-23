package org.readutf.matchmaker.queue.api

import io.javalin.http.Context
import io.javalin.http.Handler
import org.readutf.matchmaker.queue.QueueManager
import org.readutf.matchmaker.utils.failure
import org.readutf.matchmaker.utils.success

// /api/queue/{name}/
class QueueInfoEndpoint(
    val queueManager: QueueManager,
) : Handler {
    override fun handle(ctx: Context) {
        val queueName = ctx.pathParam("name")

        val queue =
            queueManager.getQueue(queueName) ?: let {
                ctx.failure("Could not find queue with name '$queueName'")
                return
            }

        ctx.success(queue)
    }
}
