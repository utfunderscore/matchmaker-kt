package org.readutf.matchmaker.queue

import com.fasterxml.jackson.databind.JsonNode
import java.util.UUID

data class QueueTeam(
    val teamId: UUID,
    val players: List<UUID>,
    val attributes: JsonNode,
)
