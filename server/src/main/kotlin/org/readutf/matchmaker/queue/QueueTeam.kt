package org.readutf.matchmaker.queue

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.databind.JsonNode
import java.util.UUID

data class QueueTeam(
    val teamId: UUID,
    @JsonIgnore val socketId: String = "",
    val players: List<UUID>,
    val attributes: JsonNode,
    @JsonIgnore val joinedAt: Long = System.nanoTime(),
)
