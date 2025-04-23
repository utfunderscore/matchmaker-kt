package org.readutf.matchmaker.queue.api

import com.github.michaelbull.result.onFailure
import io.github.oshai.kotlinlogging.KotlinLogging
import io.javalin.websocket.WsCloseContext
import io.javalin.websocket.WsConfig
import io.javalin.websocket.WsConnectContext
import io.javalin.websocket.WsMessageContext
import org.readutf.matchmaker.queue.QueueManager
import org.readutf.matchmaker.queue.QueueTeam
import org.readutf.matchmaker.utils.ApiResult
import java.util.function.Consumer

// /api/queue/{name}
class QueueJoinSocket(
    private val queueManager: QueueManager,
) : Consumer<WsConfig> {
    private val logger = KotlinLogging.logger { }
    private val idToEntry = mutableMapOf<String, QueueTeam>()

    override fun accept(wsConfig: WsConfig) {
        wsConfig.onConnect(::onConnect)
        wsConfig.onMessage(::onMessage)
        wsConfig.onClose(::onClose)
        wsConfig.onError { it.error()?.printStackTrace() }
    }

    fun onConnect(ctx: WsConnectContext) {
        val queue = ctx.pathParam("name")
    }

    fun onMessage(ctx: WsMessageContext) {
        val queue = ctx.pathParam("name")
        val team = ctx.messageAsClass<QueueTeam>()

        team.socketId = ctx.sessionId()

        idToEntry[queue] = team
        queueManager
            .joinQueue(queue, team) {
                ctx.sendAsClass(ApiResult.success(it))
            }.onFailure { err ->
                logger.error(err) { "Failed to join queue" }
                ctx.sendAsClass(ApiResult.failure("Failed to join queue"))
            }
    }

    fun onClose(ctx: WsCloseContext) {
        val queue = ctx.pathParam("name")
        val team = idToEntry[queue]
        if (team != null) {
            queueManager.leaveQueue(queue, team)
        } else {
            ctx.sendAsClass(ApiResult.failure("Failed to leave queue"))
        }
    }
}
