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
package io.gatehill.imposter.service.security

import io.gatehill.imposter.config.ResolvedResourceConfig
import io.gatehill.imposter.http.HttpExchange
import io.gatehill.imposter.lifecycle.SecurityLifecycleListener
import io.gatehill.imposter.plugin.config.resource.BasicResourceConfig
import io.gatehill.imposter.plugin.config.security.SecurityConfig
import io.gatehill.imposter.plugin.config.security.SecurityConfigHolder
import io.gatehill.imposter.service.SecurityService
import io.gatehill.imposter.util.LogUtil
import org.apache.logging.log4j.LogManager
import java.util.*
import javax.inject.Inject

/**
 * @author Pete Cornish
 */
class SecurityLifecycleListenerImpl @Inject constructor(
    private val securityService: SecurityService
) : SecurityLifecycleListener {

    override fun isRequestPermitted(
        rootResourceConfig: BasicResourceConfig,
        resourceConfig: BasicResourceConfig?,
        resolvedResourceConfigs: List<ResolvedResourceConfig?>,
        httpExchange: HttpExchange
    ): Boolean {
        val security = getSecurityConfig(rootResourceConfig, resourceConfig)

        return security?.let {
            if (LOGGER.isTraceEnabled) {
                LOGGER.trace(
                    "Enforcing security policy [{} conditions] for: {}",
                    security.conditions.size,
                    LogUtil.describeRequestShort(httpExchange)
                )
            }
            securityService.enforce(security, httpExchange)
        } ?: run {
            if (LOGGER.isTraceEnabled) {
                LOGGER.trace(
                    "No security policy found for: {}",
                    LogUtil.describeRequestShort(httpExchange)
                )
            }
            true
        }
    }

    private fun getSecurityConfig(
        rootResourceConfig: BasicResourceConfig,
        resourceConfig: BasicResourceConfig?
    ): SecurityConfig? {
        var security = getSecurityConfig(resourceConfig)
        if (Objects.isNull(security)) {
            // IMPORTANT: if no resource security, fall back to root security
            security = getSecurityConfig(rootResourceConfig)
        }
        return security
    }

    private fun getSecurityConfig(resourceConfig: BasicResourceConfig?): SecurityConfig? {
        return if (resourceConfig !is SecurityConfigHolder) {
            null
        } else (resourceConfig as SecurityConfigHolder).securityConfig
    }

    companion object {
        private val LOGGER = LogManager.getLogger(
            SecurityLifecycleListenerImpl::class.java
        )
    }
}