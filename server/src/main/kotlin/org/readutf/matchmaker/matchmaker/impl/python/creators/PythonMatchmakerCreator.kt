package org.readutf.matchmaker.matchmaker.impl.python.creators

import com.fasterxml.jackson.databind.JsonNode
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import org.readutf.matchmaker.Application
import org.readutf.matchmaker.matchmaker.Matchmaker
import org.readutf.matchmaker.matchmaker.MatchmakerCreator
import org.readutf.matchmaker.matchmaker.impl.python.PythonMatchmaker

class PythonMatchmakerCreator(
    val type: String,
    val topic: String,
) : MatchmakerCreator {
    override fun deserialize(jsonNode: JsonNode): Result<Matchmaker, Throwable> {
        val name = jsonNode.get("name")?.asText() ?: return Err(Throwable("Invalid matchmaker name"))
        val batchSize = jsonNode.get("batch_size")?.asInt() ?: return Err(Throwable("Invalid matchmaker batch size"))
        val features = jsonNode.get("features")?.map { it.asText() } ?: return Err(Throwable("Invalid matchmaker features"))

        return Ok(
            PythonMatchmaker(
                name = name,
                type = type,
                batchSize = batchSize,
                features = features,
                topic = topic,
            ),
        )
    }

    override fun serialize(matchmaker: Matchmaker): Result<JsonNode, Throwable> {
        val matchmaker = matchmaker as? PythonMatchmaker ?: return Err(Throwable("Invalid matchmaker type"))

        return Ok(
            Application.objectMapper.valueToTree(
                mapOf(
                    "name" to matchmaker.name,
                    "batch_size" to matchmaker.batchSize,
                    "features" to matchmaker.features,
                ),
            ),
        )
    }
}
