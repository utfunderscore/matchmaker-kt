package org.readutf.matchmaker.utils

import com.fasterxml.jackson.databind.JsonNode

fun JsonNode.containsAllKeys(vararg keys: String): Boolean = keys.all { this.has(it) }
