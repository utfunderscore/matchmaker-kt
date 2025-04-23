package org.readutf.matchmaker.queue.api

import io.javalin.http.Context
import io.javalin.http.Handler
import org.readutf.matchmaker.queue.QueueManager
import org.readutf.matchmaker.utils.success

class QueueListEndpoint(
    val queueManager: QueueManager,
) : Handler {
    override fun handle(ctx: Context) {
        ctx.success(queueManager.getQueues())
    }
}
