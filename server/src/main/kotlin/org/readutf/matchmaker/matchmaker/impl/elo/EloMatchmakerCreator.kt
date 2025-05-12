package org.readutf.matchmaker.matchmaker.impl.elo

import com.fasterxml.jackson.databind.JsonNode
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.runCatching
import org.readutf.matchmaker.Application
import org.readutf.matchmaker.matchmaker.Matchmaker
import org.readutf.matchmaker.matchmaker.MatchmakerCreator

class EloMatchmakerCreator : MatchmakerCreator {
    override fun deserialize(jsonNode: JsonNode): Result<Matchmaker, Throwable> {
        val name = (jsonNode.get("name") ?: return Err(Exception("Missing 'name' field"))).asText()
        val rangeExpansionAmount =
            jsonNode.get("rangeExpansionAmount")?.asInt()
                ?: return Err(Exception("Missing 'rangeExpansionAmount' field"))

        val rangeExpansionTime =
            jsonNode.get("rangeExpansionTime")?.asLong()
                ?: return Err(Exception("Missing 'rangeExpansionTime' field"))

        return Ok(
            EloMatchmaker(
                name = name,
                rangeExpansionAmount = rangeExpansionAmount,
                rangeExpansionTime = rangeExpansionTime,
            ),
        )
    }

    override fun serialize(matchmaker: Matchmaker): Result<JsonNode, Throwable> {
        val converted = matchmaker as? EloMatchmaker ?: return Err(Exception("Matchmaker is not a EloMatchmaker"))

        return runCatching {
            Application.objectMapper.valueToTree(
                mapOf(
                    "name" to converted.name,
                    "rangeExpansionAmount" to converted.rangeExpansionAmount,
                    "rangeExpansionTime" to converted.rangeExpansionTime,
                ),
            )
        }
    }
}
