package org.readutf.matchmaker.utils

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonInclude.Include
import io.javalin.http.Context
import io.javalin.http.HttpStatus

data class ApiResult<T>(
    val success: Boolean,
    @JsonInclude(Include.NON_NULL) val data: T?,
) {
    companion object {
        fun <T> success(data: T): ApiResult<T> = ApiResult(true, data)

        fun <T> failure(): ApiResult<T> = ApiResult(false, null)

        fun failure(failureReason: String): ApiResult<String> = ApiResult(false, failureReason)
    }
}

fun <T> Context.success(data: T) {
    status(HttpStatus.OK)
    json(ApiResult.success(data))
}

fun Context.failure(errorReason: String? = null) {
    status(HttpStatus.BAD_REQUEST)
    if (errorReason != null) {
        json(ApiResult.failure(errorReason))
    } else {
        json(ApiResult.failure<String>())
    }
}
