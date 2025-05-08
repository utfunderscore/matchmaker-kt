package org.readutf.matchmaker.matchmaker.impl.python

import java.util.UUID

data class PythonResult(
    val requestId: String,
    val teams: List<List<UUID>>?,
    val error: String?,
)
