package org.readutf.matchmaker.matchmaker.api

import io.javalin.http.Context
import io.javalin.http.Handler
import org.readutf.matchmaker.matchmaker.MatchmakerManager
import org.readutf.matchmaker.utils.failure
import org.readutf.matchmaker.utils.success

// /api/matchmaker/{name}/
class MatchmakerDeleteEndpoint(
    val matchmakerManager: MatchmakerManager,
) : Handler {
    override fun handle(ctx: Context) {
        val name = ctx.pathParam("name")

        if (!matchmakerManager.deleteMatchmaker(name)) {
            ctx.failure("Could not find matchmaker with name '$name'")
            return
        }

        ctx.success("Matchmaker '$name' deleted")
    }
}
