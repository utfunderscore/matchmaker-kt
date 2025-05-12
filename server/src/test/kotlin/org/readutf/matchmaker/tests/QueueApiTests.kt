package org.readutf.matchmaker.tests

import com.fasterxml.jackson.databind.JsonNode
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.getOrThrow
import io.github.oshai.kotlinlogging.KotlinLogging
import io.javalin.http.Context
import io.javalin.http.HttpStatus
import io.javalin.websocket.WsCloseContext
import io.javalin.websocket.WsMessageContext
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import org.readutf.matchmaker.Application
import org.readutf.matchmaker.game.GameResult
import org.readutf.matchmaker.matchmaker.store.impl.JsonMatchmakerStore
import org.readutf.matchmaker.queue.QueueManager
import org.readutf.matchmaker.queue.api.QueueCreateEndpoint
import org.readutf.matchmaker.queue.api.QueueDeleteEndpoint
import org.readutf.matchmaker.queue.api.QueueInfoEndpoint
import org.readutf.matchmaker.queue.api.QueueJoinSocket
import org.readutf.matchmaker.queue.api.QueueListEndpoint
import org.readutf.matchmaker.queue.store.impl.JsonQueueStore
import org.readutf.matchmaker.utils.ApiResult
import org.readutf.matchmaker.utils.failure
import org.readutf.matchmaker.utils.success
import java.nio.file.Files
import java.util.UUID
import java.util.function.Consumer
import kotlin.test.Test

class QueueApiTests {
    private val logger = KotlinLogging.logger { }

    private val workDirTemp =
        Files.createTempDirectory("matchmaker-test")

    private val jsonMatchmakerStore = JsonMatchmakerStore(workDirTemp)
    private val jsonQueueStore = JsonQueueStore(workDirTemp)

    private val application =
        Application(
            matchmakerStore = jsonMatchmakerStore,
            queueStore = jsonQueueStore,
        )

    private val matchmakerManager = application.matchmakerManager
    private val queueManager = application.queueManager

    @Test
    fun `create queue success`() {
        createQueue("test-queue")
    }

    private fun createQueue(queueName: String) {
        val matchmakerName = "create-queue-success"

        matchmakerManager
            .createMatchmaker(
                "flexible",
                Application.objectMapper.valueToTree(
                    mapOf(
                        "name" to matchmakerName,
                        "targetTeamSize" to 1,
                        "minTeamSize" to 1,
                        "maxTeamSize" to 1,
                        "numberOfTeams" to 2,
                    ),
                ),
            ).getOrThrow()

        val endpoint = QueueCreateEndpoint(queueManager, matchmakerManager)

        val ctx = mockk<Context>(relaxed = true)

        every { ctx.queryParam("matchmaker") } returns matchmakerName
        every { ctx.pathParam("name") } returns queueName

        endpoint.handle(ctx)

        verify { ctx.status(HttpStatus.OK) }
    }

    @Test
    fun `create queue failure - matchmaker not found`() {
        val endpoint = QueueCreateEndpoint(queueManager, matchmakerManager)

        val ctx = mockk<Context>(relaxed = true)

        every { ctx.queryParam("matchmaker") } returns "non-existent-matchmaker"
        every { ctx.pathParam("name") } returns "test-queue"

        endpoint.handle(ctx)

        verify { ctx.failure("Matchmaker type not found") }
    }

    @Test
    fun `create queue failure - matchmaker type not specified`() {
        val endpoint = QueueCreateEndpoint(queueManager, matchmakerManager)

        val ctx = mockk<Context>(relaxed = true)

        every { ctx.queryParam("matchmaker") } returns null
        every { ctx.pathParam("name") } returns "test-queue"

        endpoint.handle(ctx)

        verify { ctx.failure("Matchmaker type not specified") }
    }

    @Test
    fun `create queue failure - error creating queue`() {
        val queueManager = mockk<QueueManager>(relaxed = true)

        val matchmakerName = "error-creating-queue"

        matchmakerManager
            .createMatchmaker(
                "flexible",
                Application.objectMapper.valueToTree(
                    mapOf(
                        "name" to matchmakerName,
                        "targetTeamSize" to 1,
                        "minTeamSize" to 1,
                        "maxTeamSize" to 1,
                        "numberOfTeams" to 2,
                    ),
                ),
            ).getOrThrow()

        val endpoint = QueueCreateEndpoint(queueManager, matchmakerManager)

        val ctx = mockk<Context>(relaxed = true)

        every { ctx.queryParam("matchmaker") } returns matchmakerName
        every { ctx.pathParam("name") } returns "error-creating-queue"

        val error = "Error creating queue"

        every { queueManager.createQueue(any(), any()) } returns Err(Exception(error))

        endpoint.handle(ctx)

        verify { ctx.failure(error) }
    }

    @Test
    fun `queue info test`() {
        val matchmakerName = "create-info-test"
        val queueName = "test-info-queue"

        matchmakerManager
            .createMatchmaker(
                "flexible",
                Application.objectMapper.valueToTree(
                    mapOf(
                        "name" to matchmakerName,
                        "targetTeamSize" to 1,
                        "minTeamSize" to 1,
                        "maxTeamSize" to 1,
                        "numberOfTeams" to 2,
                    ),
                ),
            ).getOrThrow()

        val queueCreateEndpoint = QueueCreateEndpoint(queueManager, matchmakerManager)

        val ctx = mockk<Context>(relaxed = true)

        every { ctx.queryParam("matchmaker") } returns matchmakerName
        every { ctx.pathParam("name") } returns queueName

        queueCreateEndpoint.handle(ctx)

        val endpoint = QueueInfoEndpoint(queueManager)

        val ctxInfo = mockk<Context>(relaxed = true)

        every { ctxInfo.pathParam("name") } returns queueName
        endpoint.handle(ctxInfo)
        verify { ctxInfo.status(HttpStatus.OK) }
    }

    @Test
    fun `queue info test - queue not found`() {
        val endpoint = QueueInfoEndpoint(queueManager)

        val ctx = mockk<Context>(relaxed = true)

        every { ctx.pathParam("name") } returns "non-existent-queue"
        endpoint.handle(ctx)
        verify { ctx.failure("Could not find queue with name 'non-existent-queue'") }
    }

    @Test
    fun `queue socket close - team not found`() {
        val endpoint = QueueJoinSocket(queueManager)

        val closeContext = mockk<WsCloseContext>(relaxed = true)

        every { closeContext.pathParam("name") } returns "test"
        every { closeContext.sessionId() } returns "test-session"

        endpoint.onClose(closeContext)
    }

    @Test
    fun `queue list endpoint`() {
        val queueListEndpoint = QueueListEndpoint(queueManager)

        val ctx = mockk<Context>(relaxed = true)
        queueListEndpoint.handle(ctx)
        verify { ctx.status(HttpStatus.OK) }
    }

    @Test
    fun `queue delete endpoint - doesnt exist`() {
        val queueName = "delete-non-existent-queue"
        val queueManager = mockk<QueueManager>(relaxed = true)

        val endpoint = QueueDeleteEndpoint(queueManager)

        val ctx = mockk<Context>(relaxed = true)

        every { ctx.pathParam("name") } returns queueName

        endpoint.handle(ctx)

        verify { ctx.failure("Queue $queueName not found") }
    }

    @Test
    fun `queue delete endpoint - success`() {
        val queueName = "delete-queue-success"

        createQueue(queueName)

        val endpoint = QueueDeleteEndpoint(queueManager)

        val ctx = mockk<Context>(relaxed = true)

        every { ctx.pathParam("name") } returns queueName

        endpoint.handle(ctx)

        verify { ctx.success("Queue $queueName deleted") }
    }

    @Test
    fun `queue join - success`() {
        val queueManager = spyk(application.queueManager)
        val server = GameResult("", "", 25, listOf())

        // Mock the join queue state
        every { queueManager.joinQueue(any(), any(), any()) } answers {
            val consumer = thirdArg<Consumer<GameResult>>()
            consumer.accept(server)
            Ok(Unit)
        }
        val endpoint = QueueJoinSocket(queueManager)
        val queueName = "queue-join-success"
        createQueue(queueName)

        val messageContext = mockk<WsMessageContext>(relaxed = true)
        every { messageContext.pathParam("name") } returns queueName
        every { messageContext.messageAsClass<JsonNode>() } returns
            Application.objectMapper.valueToTree(
                mapOf(
                    "players" to listOf(UUID.randomUUID(), UUID.randomUUID()),
                    "attributes" to Application.objectMapper.createObjectNode(),
                ),
            )
        val sessionId = "test-session-id"
        every { messageContext.sessionId() } returns sessionId
        endpoint.onMessage(messageContext)
        Thread.sleep(10)
        verify { messageContext.sendAsClass(ApiResult.success("Successfully joined queue")) }
        verify { messageContext.sendAsClass(ApiResult.success(server)) }
        val closeContext = mockk<WsCloseContext>(relaxed = true)
        every { closeContext.pathParam("name") } returns queueName
        every { closeContext.sessionId() } returns sessionId
        endpoint.onClose(closeContext)
    }

    @Test
    fun `queue join - non existent`() {
        val endpoint = QueueJoinSocket(queueManager)

        val queueName = "queue-join-non-existent"

        val messageContext = mockk<WsMessageContext>(relaxed = true)

        every { messageContext.pathParam("name") } returns queueName
        every { messageContext.messageAsClass<JsonNode>() } returns
            Application.objectMapper.valueToTree(
                mapOf(
                    "players" to listOf(UUID.randomUUID(), UUID.randomUUID()),
                    "attributes" to Application.objectMapper.createObjectNode(),
                ),
            )
        every { messageContext.sessionId() } returns "test-session-id"

        endpoint.onMessage(messageContext)

        verify { messageContext.sendAsClass(ApiResult.failure("Failed to join queue")) }
    }

    @Test
    fun `queue join - already in queue`() {
        val endpoint = QueueJoinSocket(queueManager)

        val queueName = "queue-join-already-in-queue"

        createQueue(queueName)

        val messageContext = mockk<WsMessageContext>(relaxed = true)

        every { messageContext.pathParam("name") } returns queueName
        every { messageContext.messageAsClass<JsonNode>() } returns
            Application.objectMapper.valueToTree(
                mapOf(
                    "players" to listOf(UUID.randomUUID(), UUID.randomUUID()),
                    "attributes" to Application.objectMapper.createObjectNode(),
                ),
            )
        every { messageContext.sessionId() } returns "test-session-id"

        endpoint.onMessage(messageContext)
        endpoint.onMessage(messageContext)

        verify { messageContext.sendAsClass(ApiResult.failure("Already in queue")) }
    }

    @Test
    fun `join queue - invalid team object`() {
        val endpoint = QueueJoinSocket(queueManager)

        val queueName = "join-queue-invalid-team-object"

        createQueue(queueName)

        val messageContext = mockk<WsMessageContext>(relaxed = true)

        every { messageContext.pathParam("name") } returns queueName
        every { messageContext.messageAsClass<JsonNode>() } returns
            Application.objectMapper.valueToTree(
                mapOf(
                    "invalid" to listOf(UUID.randomUUID(), UUID.randomUUID()),
                    "attributes" to Application.objectMapper.createObjectNode(),
                ),
            )
        every { messageContext.sessionId() } returns "test-session-id"

        endpoint.onMessage(messageContext)

        verify { messageContext.sendAsClass(ApiResult.failure("Invalid team object.")) }
    }
}
