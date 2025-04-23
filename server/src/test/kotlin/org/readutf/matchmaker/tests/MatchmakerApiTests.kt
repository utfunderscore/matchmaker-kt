package org.readutf.matchmaker.tests

import com.github.michaelbull.result.getOrThrow
import io.github.oshai.kotlinlogging.KotlinLogging
import io.javalin.http.Context
import io.javalin.http.HttpStatus
import io.javalin.testtools.JavalinTest
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.readutf.matchmaker.Application
import org.readutf.matchmaker.matchmaker.api.MatchmakerCreatorListEndpoint
import org.readutf.matchmaker.matchmaker.api.MatchmakerDeleteEndpoint
import org.readutf.matchmaker.matchmaker.api.MatchmakerListEndpoint
import org.readutf.matchmaker.matchmaker.store.impl.JsonMatchmakerStore
import org.readutf.matchmaker.queue.store.impl.JsonQueueStore
import org.readutf.matchmaker.utils.ApiResult
import org.readutf.matchmaker.utils.failure
import org.readutf.matchmaker.utils.success
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

class MatchmakerApiTests {
    private val logger = KotlinLogging.logger { }

    private val workDirTemp =
        Files.createTempDirectory("matchmaker-test")

    private val jsonMatchmakerStore = JsonMatchmakerStore(workDirTemp)
    private val jsonQueueStore = JsonQueueStore(workDirTemp)

    private val application =
        Application(
            databaseUrl = "jdbc:postgresql://localhost:5432/example_db",
            username = "postgres",
            password = "password",
            matchmakerStore = jsonMatchmakerStore,
            queueStore = jsonQueueStore,
        )

    @Test
    fun `create flexible matchmaker`() {
        JavalinTest.test(application.javalin) { server, client ->

            logger.info { "Starting server" }

            val body: Map<String, Any> =
                mapOf(
                    "name" to "test",
                    "targetTeamSize" to 1,
                    "minTeamSize" to 1,
                    "maxTeamSize" to 1,
                    "numberOfTeams" to 2,
                )

            val result =
                client
                    .put("/api/private/matchmaker/flexible", body)

            val expectedResult = Application.objectMapper.writeValueAsString(ApiResult.success(null))

            assertEquals(200, result.code)
            assertEquals(expectedResult, result.body?.string())
        }
    }

    @Test
    fun `create flexible matchmaker gradually`() {
        JavalinTest.test(application.javalin) { server, client ->

            logger.info { "Starting server" }

            val body = mutableMapOf<String, Any>()

            // List of key-value pairs to add one by one
            val keyValuePairs =
                listOf(
                    "name" to "test",
                    "targetTeamSize" to 1,
                    "minTeamSize" to 1,
                    "maxTeamSize" to 1,
                    "numberOfTeams" to 2,
                )

            keyValuePairs.forEach { (key, value) ->
                body[key] = value

                // Perform a request after adding each key-value pair
                val result = client.put("/api/private/matchmaker/flexible", body)

                // Log intermediate result
                logger.info { "Request with $key=$value: response code ${result.code}" }

                assertEquals(
                    if (body.size == keyValuePairs.size) {
                        200
                    } else {
                        400
                    },
                    result.code,
                )
            }

            // Final check for the complete map
            val finalResult = client.put("/api/private/matchmaker/flexible", body)
            val expectedResult = Application.objectMapper.writeValueAsString(ApiResult.success(null))

            assertEquals(200, finalResult.code)
            assertEquals(expectedResult, finalResult.body?.string())
        }
    }

    @Test
    fun `create flexible matchmaker invalid json body`() {
        JavalinTest.test(application.javalin) { server, client ->

            logger.info { "Starting server" }

            val result =
                client
                    .put("/api/private/matchmaker/flexible", "{'tes")

            val expectedResult = Application.objectMapper.writeValueAsString(ApiResult.success(null))

            assertEquals(400, result.code)
        }
    }

    @Test
    fun `list creators test`() {
        val matchmakerManager = application.matchmakerManager

        val endpoint = MatchmakerCreatorListEndpoint(matchmakerManager)

        val context = mockk<Context>(relaxed = true)

        endpoint.handle(context)

        verify { context.status(HttpStatus.OK) }
        verify { context.json(ApiResult.success(matchmakerManager.getCreatorIds())) }
    }

    @Test
    fun `delete endpoint success`() {
        val matchmakerManager = application.matchmakerManager

        val name = "delete-endpoint-test"

        matchmakerManager
            .createMatchmaker(
                "flexible",
                Application.objectMapper.valueToTree(
                    mapOf(
                        "name" to name,
                        "targetTeamSize" to 1,
                        "minTeamSize" to 1,
                        "maxTeamSize" to 1,
                        "numberOfTeams" to 2,
                    ),
                ),
            ).getOrThrow()

        val endpoint = MatchmakerDeleteEndpoint(application.matchmakerManager)

        val ctx = mockk<Context>(relaxed = true)

        every { ctx.pathParam("name") } returns name

        endpoint.handle(ctx)

        verify { ctx.success("Matchmaker '$name' deleted") }
    }

    @Test
    fun `delete endpoint doesnt exist`() {
        val name = "delete-endpoint-doesnt-exist"

        val endpoint = MatchmakerDeleteEndpoint(application.matchmakerManager)

        val ctx = mockk<Context>(relaxed = true)

        every { ctx.pathParam("name") } returns name

        endpoint.handle(ctx)

        verify { ctx.failure("Could not find matchmaker with name '$name'") }
    }

    @Test
    fun `list matchmakers success`() {
        val endpoint = MatchmakerListEndpoint(application.matchmakerManager)

        val ctx = mockk<Context>(relaxed = true)

        endpoint.handle(ctx)

        verify { ctx.status(HttpStatus.OK) }
    }

    @AfterTest
    fun tearDown() {
        Files
            .walk(workDirTemp)
            .sorted(Comparator.reverseOrder())
            .forEach { Files.delete(it) }
    }
}
