package org.readutf.matchmaker.game

import org.readutf.matchmaker.queue.QueueTeam

data class GameResult(
    val id: String,
    val ip: String,
    val port: Int,
    val teams: List<List<QueueTeam>>,
)
