package org.readutf.matchmaker.matchmaker.api

import com.github.michaelbull.result.getOrElse
import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.onSuccess
import com.github.michaelbull.result.runCatching
import io.javalin.http.Context
import io.javalin.http.Handler
import org.readutf.matchmaker.Application.Companion.objectMapper
import org.readutf.matchmaker.matchmaker.MatchmakerManager
import org.readutf.matchmaker.utils.failure
import org.readutf.matchmaker.utils.success

// /api/matchmaker/{name}/
class MatchmakerCreateEndpoint(
    val matchmakerManager: MatchmakerManager,
) : Handler {
    override fun handle(ctx: Context) {
        val type = ctx.pathParam("name")

        val jsonBody =
            runCatching {
                objectMapper.readTree(ctx.body())
            }.getOrElse { err ->
                ctx.failure("Failed to parse JSON body")
                return
            }

        matchmakerManager
            .createMatchmaker(type, jsonBody)
            .onSuccess { success ->
                ctx.success(null)
            }.onFailure {
                ctx.failure(it.message ?: "Failed to create matchmaker")
            }
    }
}
