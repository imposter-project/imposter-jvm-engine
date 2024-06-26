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

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import io.gatehill.imposter.http.HttpExchange
import io.gatehill.imposter.http.ResponseBehaviourFactory
import io.gatehill.imposter.plugin.config.PluginConfig
import io.gatehill.imposter.plugin.config.resource.BasicResourceConfig
import io.gatehill.imposter.script.ReadWriteResponseBehaviour
import io.gatehill.imposter.script.ResponseBehaviourType
import io.gatehill.imposter.script.ScriptUtil
import io.gatehill.imposter.service.ScriptSource
import io.gatehill.imposter.service.ScriptedResponseService
import kotlin.io.path.pathString

class ScriptProcessingStep(
    private val scriptedResponseService: ScriptedResponseService,
) : ProcessingStep {
    override fun execute(
        context: StepContext,
        httpExchange: HttpExchange,
        statusCode: Int,
        responseBehaviourFactory: ResponseBehaviourFactory,
        additionalContext: Map<String, Any>?,
    ): ReadWriteResponseBehaviour {
        val ctx = context as ScriptStepContext
        val script = parseScriptSource(ctx)

        val responseBehaviour = scriptedResponseService.determineResponseFromScript(
            httpExchange,
            ctx.pluginConfig,
            script,
            additionalContext,
        )

        // use defaults if not set
        if (null == responseBehaviour.behaviourType || ResponseBehaviourType.DEFAULT_BEHAVIOUR == responseBehaviour.behaviourType) {
            responseBehaviourFactory.populate(statusCode, ctx.resourceConfig, responseBehaviour)
        }

        return responseBehaviour
    }

    companion object {
        fun parseScriptSource(ctx: ScriptStepContext) = ctx.config.scriptCode?.let {
            val ext = when (val lang = ctx.config.language) {
                "groovy" -> "groovy"
                null, "js", "javascript" -> "js"
                else -> throw IllegalStateException("Unsupported script language: $lang")
            }
            ScriptSource(
                // stable ID for script cache key
                source = "${ctx.stepId}_inline.$ext",
                code = ctx.config.scriptCode
            )
        } ?: ctx.config.scriptFile?.let {
            val resolvedPath = ScriptUtil.resolveScriptPath(ctx.pluginConfig, ctx.config.scriptFile)
            ScriptSource(
                // use path as source to allow reuse of script cache
                source = resolvedPath.pathString,
                file = resolvedPath,
            )
        } ?: throw IllegalStateException("Script file or code not set")
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class ScriptStepConfig(
    @JsonProperty("code")
    val scriptCode: String? = null,

    @JsonProperty("file")
    val scriptFile: String? = null,

    @JsonProperty("language")
    @JsonAlias("lang")
    val language: String? = null,
)

data class ScriptStepContext(
    override val stepId: String,
    override val resourceConfig: BasicResourceConfig,
    val config: ScriptStepConfig,
    val pluginConfig: PluginConfig,
) : StepContext
