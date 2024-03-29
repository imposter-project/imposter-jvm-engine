/*
 * Copyright (c) 2016-2023.
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
package io.gatehill.imposter.service.security

import io.gatehill.imposter.http.HttpExchange
import io.gatehill.imposter.lifecycle.SecurityLifecycleHooks
import io.gatehill.imposter.plugin.config.PluginConfig
import io.gatehill.imposter.plugin.config.security.*
import io.gatehill.imposter.service.SecurityService
import io.gatehill.imposter.util.CollectionUtil.convertKeysToLowerCase
import io.gatehill.imposter.util.HttpUtil
import io.gatehill.imposter.util.LogUtil
import io.gatehill.imposter.util.MatchUtil.conditionMatches
import org.apache.logging.log4j.LogManager
import java.util.*
import javax.inject.Inject

/**
 * @author Pete Cornish
 */
class SecurityServiceImpl @Inject constructor(
    securityLifecycle: SecurityLifecycleHooks,
    securityListener: SecurityLifecycleListenerImpl,
) : SecurityService {

    init {
        securityLifecycle.registerListener(securityListener)
    }

    /**
     * {@inheritDoc}
     */
    override fun findConfigPreferringSecurityPolicy(allPluginConfigs: List<PluginConfig>): PluginConfig {
        // sanity check
        check(allPluginConfigs.isNotEmpty()) { "No plugin configurations" }

        val configsWithSecurity = allPluginConfigs.filter { c ->
            if (c is SecurityConfigHolder) {
                return@filter null != (c as SecurityConfigHolder).securityConfig
            }
            false
        }

        val selectedConfig: PluginConfig = if (configsWithSecurity.isEmpty()) {
            allPluginConfigs[0]
        } else if (configsWithSecurity.size == 1) {
            configsWithSecurity[0]
        } else {
            throw IllegalStateException("Cannot specify root 'security' configuration block more than once. Ensure only one configuration file contains the root 'security' block.")
        }
        return selectedConfig
    }

    /**
     * {@inheritDoc}
     */
    override fun enforce(security: SecurityConfig?, httpExchange: HttpExchange): Boolean {
        val outcome: PolicyOutcome = if (security!!.conditions.isEmpty()) {
            PolicyOutcome(security.defaultEffect, "default effect")
        } else {
            evaluatePolicy(security, httpExchange)
        }
        return enforceEffect(httpExchange, outcome)
    }

    private fun evaluatePolicy(security: SecurityConfig?, httpExchange: HttpExchange): PolicyOutcome {
        val failed = security!!.conditions.filter { c: SecurityCondition -> !checkCondition(c, httpExchange) }

        return if (failed.isEmpty()) {
            PolicyOutcome(SecurityEffect.Permit, "all conditions")
        } else {
            PolicyOutcome(
                SecurityEffect.Deny,
                failed.joinToString(", ") { condition -> describeCondition(condition) }
            )
        }
    }

    /**
     * Determine if the condition permits the request to proceed.
     *
     * @param condition      the security condition
     * @param httpExchange the HTTP exchange
     * @return `true` if the condition permits the request, otherwise `false`
     */
    private fun checkCondition(condition: SecurityCondition, httpExchange: HttpExchange): Boolean {
        val results = mutableListOf<SecurityEffect>()
        val request = httpExchange.request

        // query params
        results.addAll(
            checkCondition(
                condition.queryParams,
                request.queryParams,
                condition.effect,
                caseSensitiveKeyMatch = true
            )
        )

        // form params
        results.addAll(
            checkCondition(
                condition.formParams,
                request.formParams,
                condition.effect,
                caseSensitiveKeyMatch = true
            )
        )

        // headers
        results.addAll(
            checkCondition(
                condition.requestHeaders,
                request.headers,
                condition.effect,
                caseSensitiveKeyMatch = false
            )
        )

        // all must permit
        return results.all { effect -> SecurityEffect.Permit == effect }
    }

    /**
     * Determine the effect of each conditional name/value pair and operator.
     * Keys in the request map may be compared in a case-insensitive manner, based
     * on the value of caseSensitiveKeyMatch.
     *
     * @param conditionMap          the values from the condition
     * @param requestMap            the values from the request
     * @param conditionEffect       the effect of the condition if it is true
     * @param caseSensitiveKeyMatch whether to match the keys case-sensitively
     * @return the actual effect based on the values
     */
    private fun checkCondition(
        conditionMap: Map<String, ConditionalNameValuePair>,
        requestMap: Map<String, String>,
        conditionEffect: SecurityEffect,
        caseSensitiveKeyMatch: Boolean,
    ): List<SecurityEffect> {
        val comparisonMap = if (caseSensitiveKeyMatch) requestMap else convertKeysToLowerCase(requestMap)
        return conditionMap.values.map { conditionValue: ConditionalNameValuePair ->
            val key = if (caseSensitiveKeyMatch) conditionValue.name else conditionValue.name.lowercase(Locale.getDefault())
            val requestConditionValue = comparisonMap[key]

            val matched = conditionMatches(conditionValue, requestConditionValue)
            val finalEffect: SecurityEffect = if (matched) {
                conditionEffect
            } else {
                conditionEffect.invert()
            }

            if (LOGGER.isTraceEnabled) {
                LOGGER.trace(
                    "Condition match for {} {} {}: {}. Request map: {}. Effect: {}",
                    conditionValue.name,
                    conditionValue.operator,
                    conditionValue.value,
                    matched,
                    comparisonMap.entries,
                    finalEffect
                )
            }
            return@map finalEffect
        }
    }

    private fun enforceEffect(httpExchange: HttpExchange, outcome: PolicyOutcome): Boolean {
        return if (SecurityEffect.Permit != outcome.effect) {
            LOGGER.warn(
                "Denying request {} due to security policy - {}",
                LogUtil.describeRequestShort(httpExchange), outcome.policySource
            )
            httpExchange.fail(HttpUtil.HTTP_UNAUTHORIZED)
            false
        } else {
            LOGGER.trace(
                "Permitting request {} due to security policy - {}",
                LogUtil.describeRequestShort(httpExchange), outcome.policySource
            )
            true
        }
    }

    private fun describeCondition(condition: SecurityCondition): String {
        val description = StringBuilder()
        describeConditionPart(description, condition.queryParams, "query conditions")
        describeConditionPart(description, condition.requestHeaders, "header conditions")
        return description.toString()
    }

    private fun describeConditionPart(
        description: StringBuilder,
        part: Map<String, ConditionalNameValuePair>,
        partType: String,
    ) {
        if (part.isNotEmpty()) {
            if (description.isNotEmpty()) {
                description.append(", ")
            }
            description.append(partType).append(": [").append(java.lang.String.join(", ", part.keys)).append("]")
        }
    }

    private class PolicyOutcome(val effect: SecurityEffect, val policySource: String)

    companion object {
        private val LOGGER = LogManager.getLogger(
            SecurityServiceImpl::class.java
        )
    }
}