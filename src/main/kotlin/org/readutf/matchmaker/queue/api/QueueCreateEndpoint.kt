package org.readutf.matchmaker.queue.api

import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.onSuccess
import io.javalin.http.Context
import io.javalin.http.Handler
import org.readutf.matchmaker.matchmaker.MatchmakerManager
import org.readutf.matchmaker.queue.QueueManager
import org.readutf.matchmaker.utils.failure
import org.readutf.matchmaker.utils.success

// /api/queue/{name}?matchmaker={type}
class QueueCreateEndpoint(
    val queueManager: QueueManager,
    val matchmakerManager: MatchmakerManager,
) : Handler {
    override fun handle(ctx: Context) {
        val matchmakerType =
            ctx.queryParam("matchmaker") ?: let {
                ctx.failure("Matchmaker type not specified")
                return
            }
        val queueName = ctx.pathParam("name")

        val matchmaker =
            matchmakerManager.getMatchmaker(matchmakerType) ?: let {
                ctx.failure("Matchmaker type not found")
                return
            }

        queueManager
            .createQueue(queueName, matchmaker)
            .onFailure { err ->
                ctx.failure(err.message ?: "An error occurred")
            }.onSuccess { queue ->
                ctx.success(queue)
            }
    }
}
