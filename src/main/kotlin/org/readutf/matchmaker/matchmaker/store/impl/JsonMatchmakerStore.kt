package org.readutf.matchmaker.matchmaker.store.impl

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.JsonNode
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.getOrElse
import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.onSuccess
import com.github.michaelbull.result.runCatching
import io.github.oshai.kotlinlogging.KotlinLogging
import org.readutf.matchmaker.Application
import org.readutf.matchmaker.matchmaker.Matchmaker
import org.readutf.matchmaker.matchmaker.MatchmakerCreator
import org.readutf.matchmaker.matchmaker.store.MatchmakerStore
import java.nio.file.Path

class JsonMatchmakerStore(
    jsonPath: Path,
) : MatchmakerStore {
    private val logger = KotlinLogging.logger { }
    private val file = jsonPath.toFile()

    init {
        file.parentFile.mkdirs()
        if (!file.exists()) {
            file.createNewFile()
            file.writeText("[]")
        }
    }

    override fun loadMatchmakers(creators: Map<String, MatchmakerCreator>): Result<Collection<Matchmaker>, Throwable> {
        val nodes =
            runCatching {
                Application.objectMapper.readValue(file, object : TypeReference<List<MatchmakerData>>() {})
            }.getOrElse {
                return Err(it)
            }

        val matchmakers = mutableListOf<Matchmaker>()

        for (jsonNodes in nodes) {
            val creator = creators[jsonNodes.type]
            if (creator == null) {
                logger.error { "Could not find creator for matchmaker type '${jsonNodes.type}'" }
                continue
            }
            creator
                .deserialize(jsonNodes.data)
                .onFailure {
                    logger.error(it) { "Failed to deserialize matchmaker" }
                }.onSuccess {
                    matchmakers.add(it)
                }
        }

        logger.info { "Loaded ${matchmakers.size} matchmakers" }

        return Ok(matchmakers)
    }

    override fun saveMatchmakers(
        creators: Map<String, MatchmakerCreator>,
        matchmakers: Collection<Matchmaker>,
    ): Result<Unit, Throwable> {
        val trees = mutableListOf<MatchmakerData>()

        for (matchmaker in matchmakers) {
            val creator = creators[matchmaker.type]
            if (creator == null) {
                logger.error { "Could not find creator for matchmaker type '${matchmaker.type}'" }
                continue
            }
            creator
                .serialize(matchmaker)
                .onFailure {
                    logger.error(it) { "Failed to serialize matchmaker" }
                }.onSuccess { node ->
                    trees.add(MatchmakerData(matchmaker.type, node))
                }
        }

        logger.info { "Saving ${trees.size} matchmakers" }

        return runCatching {
            Application.objectMapper.writerWithDefaultPrettyPrinter().writeValue(file, trees)
        }
    }

    class MatchmakerData(
        val type: String,
        val data: JsonNode,
    )
}
