package org.readutf.matchmaker.matchmaker

import com.fasterxml.jackson.databind.JsonNode
import com.github.michaelbull.result.Result

interface MatchmakerCreator {
    fun deserialize(jsonNode: JsonNode): Result<Matchmaker, Throwable>

    fun serialize(matchmaker: Matchmaker): Result<JsonNode, Throwable>
}
