/*
 * Copyright (c) 2016-2021.
 *
 * This file is part of Imposter.
 *
 * "Commons Clause" License Condition v1.0
 *
 * The Software is provided to you by the Licensor under the License, as
 * defined below, subject to the following condition.
 *
 * Without limiting other conditions in the License, the grant of rights
 * under the License will not include, and the License does not grant to
 * you, the right to Sell the Software.
 *
 * For purposes of the foregoing, "Sell" means practicing any or all of
 * the rights granted to you under the License to provide to third parties,
 * for a fee or other consideration (including without limitation fees for
 * hosting or consulting/support services related to the Software), a
 * product or service whose value derives, entirely or substantially, from
 * the functionality of the Software. Any license notice or attribution
 * required by the License must also include this Commons Clause License
 * Condition notice.
 *
 * Software: Imposter
 *
 * License: GNU Lesser General Public License version 3
 *
 * Licensor: Peter Cornish
 *
 * Imposter is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Imposter is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Imposter.  If not, see <https://www.gnu.org/licenses/>.
 */
package io.gatehill.imposter.http

import com.fasterxml.jackson.annotation.JsonAlias
import io.gatehill.imposter.plugin.config.resource.ResourceMethod
import io.vertx.core.MultiMap
import io.vertx.core.buffer.Buffer
import io.vertx.core.json.JsonObject

/**
 * @author Pete Cornish
 */
interface HttpExchange {
    var phase: ExchangePhase
    fun request(): HttpRequest
    fun response(): HttpResponse

    @Deprecated("Use request.pathParams", replaceWith = ReplaceWith("request().pathParams()"))
    fun pathParams(): Map<String, String>

    @Deprecated("Use request.queryParams", replaceWith = ReplaceWith("request().queryParams()"))
    fun queryParams(): Map<String, String>

    @Deprecated("Use request.pathParam", replaceWith = ReplaceWith("request().pathParam()"))
    fun pathParam(paramName: String): String?

    @Deprecated("Use request.queryParam", replaceWith = ReplaceWith("request().queryParam()"))
    fun queryParam(queryParam: String): String?

    fun isAcceptHeaderEmpty(): Boolean
    fun acceptsMimeType(mimeType: String): Boolean

    /**
     * Note: not all routes have a path.
     */
    val currentRoutePath: String?

    @Deprecated("Use request.body", replaceWith = ReplaceWith("request().body"))
    val body: Buffer?

    @Deprecated("Use request.bodyAsString", replaceWith = ReplaceWith("request().bodyAsString"))
    val bodyAsString: String?

    @Deprecated("Use request.bodyAsJson", replaceWith = ReplaceWith("request().bodyAsJson"))
    val bodyAsJson: JsonObject?

    fun fail(cause: Throwable?)
    fun fail(statusCode: Int)
    fun fail(statusCode: Int, cause: Throwable?)
    fun failure(): Throwable?

    fun <T> get(key: String): T?
    fun put(key: String, value: Any)

    fun <T : Any> getOrPut(key: String, defaultSupplier: () -> T): T {
        return get(key) ?: run {
            val value = defaultSupplier()
            put(key, value)
            return@run value
        }
    }
}

/**
 * @author Pete Cornish
 */
enum class ExchangePhase {
    @JsonAlias("request-received")
    REQUEST_RECEIVED,

    /**
     * After the HTTP exchange has been dispatched, but the response may not yet have
     * been sent. Note: response may not have been transmitted if delay set.
     */
    @JsonAlias("request-dispatched")
    REQUEST_DISPATCHED,

    /**
     * After the HTTP response has been sent. This is after delayed responses, but assumes
     * that any fallback handlers have blocked until the response has been completely passed
     * to the underlying adapter, and therefore it is safe to perform cleanup activities.
     */
    @JsonAlias("response-sent")
    RESPONSE_SENT,
}

/**
 * @author Pete Cornish
 */
interface HttpRequest {
    fun path(): String
    fun method(): ResourceMethod
    fun absoluteURI(): String
    fun headers(): Map<String, String>
    fun getHeader(headerKey: String): String?
    fun pathParams(): Map<String, String>
    fun queryParams(): Map<String, String>
    fun pathParam(paramName: String): String?
    fun queryParam(queryParam: String): String?

    val body: Buffer?
    val bodyAsString: String?
    val bodyAsJson: JsonObject?
}

/**
 * @author Pete Cornish
 */
interface HttpResponse {
    fun setStatusCode(statusCode: Int): HttpResponse
    fun getStatusCode(): Int
    fun putHeader(headerKey: String, headerValue: String): HttpResponse
    fun headers(): MultiMap
    fun end()
    fun end(body: Buffer)
    fun end(body: String?) {
        body?.let { end(Buffer.buffer(body)) } ?: end()
    }

    val bodyBuffer: Buffer?
}
