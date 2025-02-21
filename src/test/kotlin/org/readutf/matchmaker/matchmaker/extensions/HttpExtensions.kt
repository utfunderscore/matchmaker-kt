package org.readutf.matchmaker.matchmaker.extensions

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody

private val objectMapper = jacksonObjectMapper()

fun Any.toRequestBody(): RequestBody = objectMapper.writeValueAsString(this).toRequestBody()
