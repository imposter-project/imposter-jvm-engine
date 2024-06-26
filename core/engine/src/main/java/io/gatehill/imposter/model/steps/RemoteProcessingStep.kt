/*
 * Copyright (c) 2023-2023.
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

package io.gatehill.imposter.model.steps

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import io.gatehill.imposter.http.HttpExchange
import io.gatehill.imposter.http.HttpMethod
import io.gatehill.imposter.http.ResponseBehaviourFactory
import io.gatehill.imposter.placeholder.RemoteEvaluator
import io.gatehill.imposter.plugin.config.capture.ItemCaptureConfig
import io.gatehill.imposter.plugin.config.resource.AbstractResourceConfig
import io.gatehill.imposter.plugin.config.resource.BasicResourceConfig
import io.gatehill.imposter.plugin.config.resource.ResponseConfig
import io.gatehill.imposter.script.ReadWriteResponseBehaviour
import io.gatehill.imposter.service.CaptureService
import io.gatehill.imposter.service.RemoteService
import io.gatehill.imposter.util.HttpUtil
import io.gatehill.imposter.util.PlaceholderUtil
import org.apache.logging.log4j.LogManager

class RemoteProcessingStep(
    private val remoteService: RemoteService,
    private val captureService: CaptureService,
) : ProcessingStep {
    private val logger = LogManager.getLogger(javaClass)

    private val evaluators = PlaceholderUtil.defaultEvaluators.toMutableMap().apply {
        // allows queries of the remote context using '${remote.response...}' etc.
        put("remote", RemoteEvaluator)
    }

    override fun execute(
        context: StepContext,
        httpExchange: HttpExchange,
        statusCode: Int,
        responseBehaviourFactory: ResponseBehaviourFactory,
        additionalContext: Map<String, Any>?,
    ): ReadWriteResponseBehaviour {
        val ctx = context as RemoteStepContext
        return try {
            val remoteExchange = remoteService.sendRequest(
                ctx.config.url,
                ctx.config.method,
                ctx.config.queryParams,
                ctx.config.formParams,
                ctx.config.headers,
                ctx.config.content,
                httpExchange
            )
            ctx.config.capture?.forEach { (key, config) ->
                captureService.captureItem(key, config, remoteExchange, evaluators)
            }
            responseBehaviourFactory.build(statusCode, ctx.resourceConfig)
        } catch (e: Exception) {
            logger.error("Error sending remote request: {} {}", ctx.config.method, ctx.config.url, e)
            val emptyResourceConfig = object : AbstractResourceConfig() {
                override val responseConfig = ResponseConfig()
            }
            responseBehaviourFactory.build(HttpUtil.HTTP_INTERNAL_ERROR, emptyResourceConfig)
        }
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class RemoteStepConfig(
    @JsonProperty("url")
    val url: String,

    @JsonProperty("method")
    val method: HttpMethod,

    @JsonProperty("queryParams")
    val queryParams: Map<String, String>?,

    @JsonProperty("formParams")
    val formParams: Map<String, String>?,

    @JsonProperty("headers")
    val headers: Map<String, String>?,

    @JsonProperty("content")
    val content: String?,

    @JsonProperty("capture")
    val capture: Map<String, ItemCaptureConfig>?,
)

data class RemoteStepContext(
    override val stepId: String,
    override val resourceConfig: BasicResourceConfig,
    val config: RemoteStepConfig,
) : StepContext
