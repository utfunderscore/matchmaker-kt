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

// /api/matchmaker/{type}/
class MatchmakerCreateEndpoint(
    private val matchmakerManager: MatchmakerManager,
) : Handler {
    override fun handle(ctx: Context) {
        val type = ctx.pathParam("type")

        val jsonBody =
            runCatching {
                objectMapper.readTree(ctx.body())
            }.getOrElse {
                ctx.failure("Failed to parse JSON body")
                return
            }

        matchmakerManager
            .createMatchmaker(type, jsonBody)
            .onSuccess {
                ctx.success(null)
            }.onFailure {
                ctx.failure(it.message)
            }
    }
}
