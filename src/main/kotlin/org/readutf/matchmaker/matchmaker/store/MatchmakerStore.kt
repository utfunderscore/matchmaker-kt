package org.readutf.matchmaker.matchmaker.store

import com.github.michaelbull.result.Result
import org.readutf.matchmaker.matchmaker.Matchmaker
import org.readutf.matchmaker.matchmaker.MatchmakerCreator

interface MatchmakerStore {
    fun loadMatchmakers(creators: Map<String, MatchmakerCreator>): Result<Collection<Matchmaker>, Throwable>

    fun saveMatchmakers(
        creators: Map<String, MatchmakerCreator>,
        matchmakers: Collection<Matchmaker>,
    ): Result<Unit, Throwable>
}
