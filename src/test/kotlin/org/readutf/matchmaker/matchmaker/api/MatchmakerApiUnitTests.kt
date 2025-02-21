package org.readutf.matchmaker.matchmaker.api

import io.javalin.json.JavalinJackson
import io.javalin.json.toJsonString
import io.javalin.testtools.JavalinTest
import org.readutf.matchmaker.Application
import org.readutf.matchmaker.utils.ApiResult
import org.testng.annotations.Test
import java.nio.file.Files
import kotlin.test.assertEquals

class MatchmakerApiUnitTests {
    val testingPath = Files.createTempDirectory("matchmaker-test")

    private var javalinJackson: JavalinJackson = JavalinJackson()

    @Test
    fun `create pgvector matchmaker missing property`() =
        JavalinTest.test(Application(testingPath).javalin) { server, client ->
            val response = client.put("/api/matchmaker/pgvector/")
            val expectedBody = javalinJackson.toJsonString(ApiResult.failure("Missing 'name' field"))
            assertEquals(400, response.code)
            assertEquals(expectedBody, response.body?.string())
        }

    @Test
    fun `create pgvector matchmaker invalid body`() =
        JavalinTest.test(Application(testingPath).javalin) { server, client ->
            val response = client.put("/api/matchmaker/pgvector/", "[")
            val expectedBody = javalinJackson.toJsonString(ApiResult.failure("Failed to parse JSON body"))
            assertEquals(400, response.code)
            assertEquals(expectedBody, response.body?.string())
        }

    @Test
    fun `create pgvector matchmaker success`() =
        JavalinTest.test(Application(testingPath).javalin) { server, client ->

            val response =
                client.put(
                    "/api/matchmaker/pgvector/",
                    mapOf(
                        "name" to "pgvector-test",
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
                    ),
                )

            val expectedBody = javalinJackson.toJsonString(ApiResult.success(null))
            assertEquals(200, response.code)
            assertEquals(expectedBody, response.body?.string())
        }

    @Test
    fun `create flexible matchmaker missing name`() =
        JavalinTest.test(Application(testingPath).javalin) { server, client ->
            val response = client.put("/api/matchmaker/flexible/")
            val expectedBody = javalinJackson.toJsonString(ApiResult.failure("Missing 'name' field"))
            assertEquals(400, response.code)
            assertEquals(expectedBody, response.body?.string())
        }

    @Test
    fun `create flexible matchmaker success`() =
        JavalinTest.test(Application(testingPath).javalin) { server, client ->
            val expectedBody = javalinJackson.toJsonString(ApiResult.success(null))
            val response =
                client.put(
                    "/api/matchmaker/flexible/",
                    mapOf(
                        "name" to "flexible-test",
                        "targetTeamSize" to 5,
                        "minTeamSize" to 1,
                        "maxTeamSize" to 10,
                        "numberOfTeams" to 2,
                    ),
                )
            assertEquals(expectedBody, response.body?.string())
        }

    @Test(dependsOnMethods = ["create pgvector matchmaker success"])
    fun `delete matchmaker success`() =
        JavalinTest.test(Application(testingPath).javalin) { server, client ->
            val response =
                client.delete("/api/matchmaker/pgvector-test/")

            val expectedBody = javalinJackson.toJsonString(ApiResult.success("Matchmaker 'pgvector-test' deleted"))
            assertEquals(expectedBody, response.body?.string())
            assertEquals(200, response.code)
        }

    @Test(dependsOnMethods = ["delete matchmaker success"])
    fun `delete matchmaker doesnt exist`() =
        JavalinTest.test(Application(testingPath).javalin) { server, client ->
            val response =
                client.delete("/api/matchmaker/doesnt-exist/")

            val expectedBody = javalinJackson.toJsonString(ApiResult.failure("Could not find matchmaker with name 'doesnt-exist'"))
            assertEquals(expectedBody, response.body?.string())
            assertEquals(400, response.code)
        }

    @Test(dependsOnMethods = ["create pgvector matchmaker success", "create flexible matchmaker success"])
    fun `list matchmakers`() {
        JavalinTest.test(Application(testingPath).javalin) { server, client ->
            val response = client.get("/api/matchmakers")
            val expectedBody = javalinJackson.toJsonString(ApiResult.success(listOf("flexible-test", "pgvector-test")))
            assertEquals(200, response.code)
            assertEquals(expectedBody, response.body?.string())
        }
    }

    @Test
    fun `list creators`() {
        JavalinTest.test(Application(testingPath).javalin) { server, client ->
            val response = client.get("/api/matchmaker/types")
            val expectedBody = javalinJackson.toJsonString(ApiResult.success(listOf("flexible", "pgvector")))
            assertEquals(200, response.code)
            assertEquals(expectedBody, response.body?.string())
        }
    }
}
