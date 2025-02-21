package org.readutf.matchmaker.matchmaker

import com.fasterxml.jackson.databind.JsonNode
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.map
import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.onSuccess
import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import org.readutf.matchmaker.matchmaker.store.MatchmakerStore

/**
 * This class handles the creation and management of matchmakers.
 * [MatchmakerCreator] is responsible for the creation of matchmakers and can be registered, unregistered, and retrieved.
 * [MatchmakerStore] is responsible for storing and loading matchmakers.
 * [Matchmaker] is responsible for matching teams against each other.
 *
 * @param matchmakerStore The store responsible for storing and loading matchmakers.
 */
class MatchmakerManager(
    private val matchmakerStore: MatchmakerStore,
) {
    private val logger: KLogger = KotlinLogging.logger { }
    private val creators: MutableMap<String, MatchmakerCreator> =
        mutableMapOf<String, MatchmakerCreator>()
    private val matchmakers: MutableMap<String, Matchmaker> = mutableMapOf<String, Matchmaker>()

    fun getMatchmaker(name: String): Matchmaker? = matchmakers[name]

    fun getMatchmakers(): List<Matchmaker> = matchmakers.values.toList()

    fun createMatchmaker(
        creator: String,
        jsonNode: JsonNode,
    ): Result<Unit, Throwable> {
        logger.info { "Creating matchmaker $creator" }
        val creator = creators[creator] ?: return Err(Throwable("Matchmaker not found"))
        return creator
            .createMatchmaker(jsonNode)
            .onSuccess { matchmaker ->
                matchmakers[matchmaker.name] = matchmaker
                matchmakerStore.saveMatchmakers(creators, matchmakers.values).onFailure {
                    logger.error(it) { "Failed to save matchmakers" }
                }
            }.map { }
    }

    fun deleteMatchmaker(name: String): Boolean {
        logger.info { "Deleting matchmaker $name" }
        return matchmakers.remove(name) != null
    }

    fun registerCreator(
        name: String,
        factory: MatchmakerCreator,
    ) {
        logger.info { "Registering creator $name" }
        creators[name] = factory
    }

    fun getCreatorIds(): List<String> = creators.keys.toList()

    fun removeCreator(name: String) {
        creators.remove(name)
    }

    fun shutdown() {
        logger.info { "Shutting down matchmaker manager ${matchmakers.keys}" }
        matchmakerStore.saveMatchmakers(creators, matchmakers.values).onFailure {
            logger.error(it) { "Failed to save matchmakers" }
        }
    }

    fun loadMatchmakers(): Result<Unit, Throwable> =
        matchmakerStore
            .loadMatchmakers(creators)
            .onSuccess {
                matchmakers.putAll(it.associateBy { matchmaker -> matchmaker.name })
            }.map { }
}
