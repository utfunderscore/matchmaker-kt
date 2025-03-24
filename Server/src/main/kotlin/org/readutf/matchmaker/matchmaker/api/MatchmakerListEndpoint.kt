package org.readutf.matchmaker.matchmaker.api

import io.javalin.http.Context
import io.javalin.http.Handler
import org.readutf.matchmaker.matchmaker.MatchmakerManager
import org.readutf.matchmaker.utils.success

class MatchmakerListEndpoint(
    val matchmakerManager: MatchmakerManager,
) : Handler {
    override fun handle(ctx: Context) {
        ctx.success(matchmakerManager.getMatchmakers().map { it.name })
    }
}
