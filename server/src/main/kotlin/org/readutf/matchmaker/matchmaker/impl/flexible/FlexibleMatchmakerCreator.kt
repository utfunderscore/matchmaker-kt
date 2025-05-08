package org.readutf.matchmaker.matchmaker.impl.flexible

import com.fasterxml.jackson.databind.JsonNode
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.runCatching
import org.readutf.matchmaker.Application
import org.readutf.matchmaker.matchmaker.Matchmaker
import org.readutf.matchmaker.matchmaker.MatchmakerCreator

class FlexibleMatchmakerCreator : MatchmakerCreator {
    override fun deserialize(jsonNode: JsonNode): Result<Matchmaker, Throwable> {
        val name =
            (jsonNode.get("name") ?: return Err(Exception("Missing 'name' field"))).asText()
        val targetTeamSize =
            jsonNode.get("targetTeamSize")?.asInt()
                ?: return Err(Exception("Missing 'targetTeamSize' field"))
        val minTeamSize =
            jsonNode.get("minTeamSize")?.asInt()
                ?: return Err(Exception("Missing 'minTeamSize' field"))
        val maxTeamSize =
            jsonNode.get("maxTeamSize")?.asInt()
                ?: return Err(Exception("Missing 'maxTeamSize' field"))
        val numberOfTeams =
            jsonNode.get("numberOfTeams")?.asInt() ?: return Err(Exception("Missing 'numberOfTeams' field"))

        return Ok(
            FlexibleMatchmaker(
                name,
                targetTeamSize,
                minTeamSize,
                maxTeamSize,
                numberOfTeams,
            ),
        )
    }

    override fun serialize(matchmaker: Matchmaker): Result<JsonNode, Throwable> {
        val converted = matchmaker as? FlexibleMatchmaker ?: return Err(Exception("Matchmaker is not a FlexibleMatchmaker"))

        return runCatching {
            Application.objectMapper.valueToTree(
                mapOf(
                    "name" to converted.name,
                    "targetTeamSize" to converted.targetTeamSize,
                    "minTeamSize" to converted.minTeamSize,
                    "maxTeamSize" to converted.maxTeamSize,
                    "numberOfTeams" to converted.numberOfTeams,
                ),
            )
        }
    }
}
