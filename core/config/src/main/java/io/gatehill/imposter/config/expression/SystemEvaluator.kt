/*
 * Copyright (c) 2023.
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

package io.gatehill.imposter.config.expression

import io.gatehill.imposter.ImposterConfig
import io.gatehill.imposter.config.ConfigHolder
import io.gatehill.imposter.expression.eval.ExpressionEvaluator
import org.apache.logging.log4j.LogManager

/**
 * Evaluates a system expression in the form:
 * ```
 * system.a.b
 * ```
 */
object SystemEvaluator : ExpressionEvaluator<String> {
    private val impl by lazy { SystemEvaluatorImpl(ConfigHolder.config) }

    override val name: String
        get() = impl.name

    override fun eval(expression: String, context: Map<String, *>) =
            impl.eval(expression, context)
}

class SystemEvaluatorImpl(
    private val imposterConfig: ImposterConfig,
) : ExpressionEvaluator<String> {
    private val logger = LogManager.getLogger(SystemEvaluatorImpl::class.java)

    override val name = "system"

    override fun eval(expression: String, context: Map<String, *>): String? {
        try {
            val parts = expression.split(
                    delimiters = arrayOf("."),
                    ignoreCase = false,
                    limit = 3,
            )
            if (parts.size < 3) {
                logger.warn("Could not parse system expression: $expression")
                return null
            }
            val parsed = when (parts[0]) {
                "system" -> when (parts[1]) {
                    "server" -> when (parts[2]) {
                        "port" -> imposterConfig.listenPort.toString()
                        "url" -> imposterConfig.serverUrl
                        else -> {
                            logger.warn("Could not parse system expression: $expression")
                            null
                        }
                    }
                    else -> {
                        logger.warn("Could not parse system expression: $expression")
                        null
                    }
                }
                else -> {
                    logger.warn("Could not parse system expression: $expression")
                    null
                }
            }
            return parsed

        } catch (e: Exception) {
            throw RuntimeException("Error evaluating system expression: $expression", e)
        }
    }
}
