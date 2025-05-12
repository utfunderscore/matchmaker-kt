package org.readutf.matchmaker.queue.api

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.JsonNode
import com.github.michaelbull.result.onFailure
import io.github.oshai.kotlinlogging.KotlinLogging
import io.javalin.websocket.WsCloseContext
import io.javalin.websocket.WsConfig
import io.javalin.websocket.WsMessageContext
import org.readutf.matchmaker.Application
import org.readutf.matchmaker.queue.QueueManager
import org.readutf.matchmaker.queue.QueueTeam
import org.readutf.matchmaker.utils.ApiResult
import org.readutf.matchmaker.utils.containsAllKeys
import java.time.Duration
import java.util.UUID
import java.util.concurrent.TimeUnit
import java.util.function.Consumer

// /api/queue/{name}
class QueueJoinSocket(
    private val queueManager: QueueManager,
) : Consumer<WsConfig> {
    private val logger = KotlinLogging.logger { }
    private val sessionToTeam = mutableMapOf<String, QueueTeam>()

    override fun accept(wsConfig: WsConfig) {
        wsConfig.onMessage(::onMessage)
        wsConfig.onClose(::onClose)
        wsConfig.onConnect { onConnect ->
            onConnect.session.idleTimeout = Duration.ofMillis(TimeUnit.MINUTES.toMillis(2))
        }
    }

    fun onMessage(ctx: WsMessageContext) {
        val queue = ctx.pathParam("name")
        val teamBody = ctx.messageAsClass<JsonNode>()

        if (ctx.sessionId() in sessionToTeam) {
            ctx.sendAsClass(ApiResult.failure("Already in queue"))
            return
        }

        if (!teamBody.containsAllKeys("players", "attributes")) {
            ctx.sendAsClass(ApiResult.failure("Invalid team object."))
            return
        }

        val team =
            QueueTeam(
                teamId = UUID.randomUUID(),
                players =
                    Application.objectMapper.treeToValue(
                        teamBody.get("players"),
                        object : TypeReference<List<UUID>>() {},
                    ),
                socketId = ctx.sessionId(),
                attributes = teamBody.get("attributes"),
            )

        sessionToTeam[ctx.sessionId()] = team

        queueManager
            .joinQueue(queue, team) {
                println("Queue $queue: $it")
                ctx.sendAsClass(ApiResult.success(it))
                ctx.closeSession()
            }.onFailure { err ->
                logger.error(err) { "Failed to join queue" }
                ctx.sendAsClass(ApiResult.failure("Failed to join queue"))
            }
    }

    fun onClose(ctx: WsCloseContext) {
        val queue = ctx.pathParam("name")
        val team = sessionToTeam[ctx.sessionId()]
        if (team != null) {
            queueManager.leaveQueue(queue, team)
        } else {
            ctx.sendAsClass(ApiResult.failure("Failed to leave queue"))
        }
    }
}
