package org.readutf.matchmaker.game.impl

import org.readutf.matchmaker.game.GameProvider
import org.readutf.matchmaker.game.GameServer
import org.readutf.matchmaker.queue.QueueTeam
import java.util.concurrent.atomic.AtomicInteger

class PseudoGameProvider : GameProvider {
    private val idTracker = AtomicInteger(0)

    override fun getGame(teams: List<List<QueueTeam>>): GameServer =
        GameServer(
            "pseudo-${idTracker.incrementAndGet()}",
            ip = "127.0.0.1",
            port = 2000,
        )
}
