package org.readutf.matchmaker.game

import org.readutf.matchmaker.queue.QueueTeam

interface GameProvider {
    /**
     * Finds a available game server given a specific set of teams
     * @param teams The teams to find a game for
     * @return The game server id to connect to
     */
    fun getGame(teams: List<List<QueueTeam>>): GameServer
}
