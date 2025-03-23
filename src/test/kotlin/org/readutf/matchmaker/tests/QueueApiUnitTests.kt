package org.readutf.matchmaker.tests

import io.javalin.json.JavalinJackson
import io.javalin.json.toJsonString
import io.javalin.testtools.DefaultTestConfig.okHttpClient
import okhttp3.Request
import org.readutf.matchmaker.Application
import org.readutf.matchmaker.matchmaker.store.impl.JsonMatchmakerStore
import org.readutf.matchmaker.queue.store.impl.JsonQueueStore
import org.readutf.matchmaker.utils.ApiResult
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName
import org.testng.annotations.AfterTest
import org.testng.annotations.BeforeTest
import org.testng.annotations.Test
import java.nio.file.Files
import kotlin.test.assertEquals

class QueueApiUnitTests {
    private val testingPath = Files.createTempDirectory("matchmaker-test")
    private val javalinJackson: JavalinJackson = JavalinJackson()

    var postgres = PostgreSQLContainer(DockerImageName.parse("pgvector/pgvector:pg17"))

    init {
        postgres.start()

        val jsonMatchmakerStore = JsonMatchmakerStore(testingPath)
        val jsonQueueStore = JsonQueueStore(testingPath)

        Application(
            databaseUrl = postgres.jdbcUrl,
            username = postgres.username,
            password = postgres.password,
            matchmakerStore = jsonMatchmakerStore,
            queueStore = jsonQueueStore,
        ).start("0.0.0.0", 7001)
    }

    @AfterTest
    fun cleanup() {
        postgres.stop()
    }

    @BeforeTest
    fun setup() {
        val request =
            Request
                .Builder()
                .put(
                    mapOf(
                        "name" to "pgvector_test",
                        "minPoolSize" to 20,
                        "teamSize" to 1,
                        "numberOfTeams" to 2,
                        "requiredStatistics" to
                            listOf(
                                "kdr",
                                "winpct",
                                "level",
                                "killsPerMatch",
                                "matchesPlayed",
                            ),
                    ).toRequestBody(),
                ).url("http://localhost:7001/api/matchmaker/pgvector/")

        val response = okHttpClient.newCall(request.build()).execute()
        val expectedBody = javalinJackson.toJsonString(ApiResult.success(null))

        // assert response code
        // assert response body
        assertEquals(
            expectedBody,
            response.body?.string(),
        )
        assertEquals(200, response.code)
    }

    @Test
    fun `create pgvector queue invalid matchmaker`() {
        val request =
            Request
                .Builder()
                .put(javalinJackson.toJsonString(emptyMap<String, String>()).toRequestBody())
                .url("http://localhost:7001/api/queue/test-queue?matchmaker=no-matchmaker")

        val response = okHttpClient.newCall(request.build()).execute()
        val expectedBody = javalinJackson.toJsonString(ApiResult.failure("Matchmaker type not found"))

        // assert response code
        // assert response body
        assertEquals(400, response.code)
        assertEquals(expectedBody, response.body?.string())
    }

    @Test
    fun `create pgvector queue success`() {
        val request =
            Request
                .Builder()
                .put(javalinJackson.toJsonString(emptyMap<String, String>()).toRequestBody())
                .url("http://localhost:7001/api/queue/test-queue?matchmaker=pgvector_test")

        val response = okHttpClient.newCall(request.build()).execute()
        val expectedBody = javalinJackson.toJsonString(ApiResult.failure("Missing 'name' field"))

        // assert response code
        // assert response body
        assertEquals(200, response.code)
        assertEquals(
            true,
            javalinJackson.mapper
                .readTree(response.body?.string())
                .get("success")
                .asBoolean(),
        )
    }
}
