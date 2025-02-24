package org.readutf.matchmaker.tests

import io.javalin.json.JavalinJackson
import io.javalin.json.toJsonString
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.readutf.matchmaker.Application
import org.readutf.matchmaker.utils.ApiResult
import org.testng.annotations.Test
import java.nio.file.Files
import kotlin.test.assertEquals

class MatchmakerApiUnitTests {
    private val testingPath = Files.createTempDirectory("matchmaker-test")
    private val javalinJackson: JavalinJackson = JavalinJackson()

    init {
        Application(testingPath).start("0.0.0.0", 7000)
    }

    val okHttpClient =
        OkHttpClient
            .Builder()
            .build()

    @Test
    fun `create pgvector matchmaker missing property`() {
        val request =
            Request
                .Builder()
                .put(javalinJackson.toJsonString(emptyMap<String, String>()).toRequestBody())
                .url("http://localhost:7000/api/matchmaker/pgvector/")

        val response = okHttpClient.newCall(request.build()).execute()
        val expectedBody = javalinJackson.toJsonString(ApiResult.failure("Missing 'name' field"))

        // assert response code
        // assert response body
        assertEquals(400, response.code)
        assertEquals(expectedBody, response.body?.string())
    }

    @Test
    fun `create pgvector matchmaker invalid body`() {
        val request =
            Request
                .Builder()
                .put("[".toRequestBody())
                .url("http://localhost:7000/api/matchmaker/pgvector/")

        val response = okHttpClient.newCall(request.build()).execute()
        val expectedBody = javalinJackson.toJsonString(ApiResult.failure("Failed to parse JSON body"))

        // assert response code
        // assert response body
        assertEquals(400, response.code)
        assertEquals(expectedBody, response.body?.string())
    }

    @Test
    fun `create pgvector matchmaker success`() {
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
                ).url("http://localhost:7000/api/matchmaker/pgvector/")

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
    fun `create flexible matchmaker missing name`() {
        val request =
            Request
                .Builder()
                .put(
                    mapOf(
                        "targetTeamSize" to 5,
                        "minTeamSize" to 1,
                        "maxTeamSize" to 10,
                        "numberOfTeams" to 2,
                    ).toRequestBody(),
                ).url("http://localhost:7000/api/matchmaker/flexible/")

        val response = okHttpClient.newCall(request.build()).execute()
        val expectedBody = javalinJackson.toJsonString(ApiResult.failure("Missing 'name' field"))

        // assert response code
        // assert response body
        assertEquals(
            expectedBody,
            response.body?.string(),
        )
        assertEquals(400, response.code)
    }

    @Test
    fun `create flexible matchmaker success`() {
        val request =
            Request
                .Builder()
                .put(
                    mapOf(
                        "name" to "flexible-test",
                        "targetTeamSize" to 5,
                        "minTeamSize" to 1,
                        "maxTeamSize" to 10,
                        "numberOfTeams" to 2,
                    ).toRequestBody(),
                ).url("http://localhost:7000/api/matchmaker/flexible/")

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

    @Test(dependsOnMethods = ["create pgvector matchmaker success"])
    fun `delete matchmaker success`() {
        val request =
            Request
                .Builder()
                .delete()
                .url("http://localhost:7000/api/matchmaker/pgvector_test")

        val response = okHttpClient.newCall(request.build()).execute()
        val expectedBody = javalinJackson.toJsonString(ApiResult.success("Matchmaker 'pgvector_test' deleted"))

        // assert response code
        // assert response body
        assertEquals(
            expectedBody,
            response.body?.string(),
        )
        assertEquals(200, response.code)
    }

    @Test(dependsOnMethods = ["delete matchmaker success"])
    fun `delete matchmaker doesnt exist`() {
        val request =
            Request
                .Builder()
                .delete()
                .url("http://localhost:7000/api/matchmaker/doesnt-exist")

        val response = okHttpClient.newCall(request.build()).execute()
        val expectedBody = javalinJackson.toJsonString(ApiResult.failure("Could not find matchmaker with name 'doesnt-exist'"))

        // assert response code
        // assert response body
        assertEquals(
            expectedBody,
            response.body?.string(),
        )
        assertEquals(400, response.code)
    }

    @Test(dependsOnMethods = ["create pgvector matchmaker success", "create flexible matchmaker success"])
    fun `list matchmakers`() {
        val request =
            Request
                .Builder()
                .get()
                .url("http://localhost:7000/api/matchmakers/")

        val response = okHttpClient.newCall(request.build()).execute()
        val expectedBody =
            javalinJackson.toJsonString(
                ApiResult.success(
                    listOf("flexible-test"),
                ),
            )

        // assert response code
        // assert response body
        assertEquals(
            expectedBody,
            response.body?.string(),
        )
        assertEquals(200, response.code)
    }

    @Test
    fun `list creators`() {
        val request =
            Request
                .Builder()
                .get()
                .url("http://localhost:7000/api/matchmaker/types/")

        val response = okHttpClient.newCall(request.build()).execute()
        val expectedBody =
            javalinJackson.toJsonString(
                ApiResult.success(
                    listOf("flexible", "pgvector"),
                ),
            )

        // assert response code
        // assert response body
        assertEquals(
            expectedBody,
            response.body?.string(),
        )
        assertEquals(200, response.code)
    }
}
