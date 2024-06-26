/*
 * Copyright (c) 2021-2024.
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

package io.gatehill.imposter.awslambda

import io.gatehill.imposter.awslambda.config.Settings
import io.gatehill.imposter.awslambda.impl.LambdaServer
import io.gatehill.imposter.awslambda.impl.LambdaServerFactory
import io.gatehill.imposter.awslambda.util.ImposterBuilderKt
import io.gatehill.imposter.awslambda.util.PluginUtil
import io.gatehill.imposter.plugin.internal.MetaInfPluginDetectorImpl
import io.gatehill.imposter.plugin.openapi.OpenApiPluginImpl
import io.gatehill.imposter.plugin.rest.RestPluginImpl
import io.gatehill.imposter.plugin.soap.SoapPluginImpl
import io.gatehill.imposter.util.InjectorUtil
import io.gatehill.imposter.util.LogUtil
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger

/**
 * AWS Lambda handler.
 *
 * @author Pete Cornish
 */
abstract class AbstractHandler<Request, Response>(
    eventType: LambdaServerFactory.EventType,
) {
    protected val logger: Logger = LogManager.getLogger(AbstractHandler::class.java)
    protected val server: LambdaServer<Request, Response>

    init {
        // lambda functions are only allowed write access to /tmp
        System.setProperty("vertx.cacheDirBase", "/tmp/.vertx")
        System.setProperty("java.io.tmpdir", "/tmp")

        LogUtil.configureLoggingFromEnvironment()
        LogUtil.configureVertxLogging()

        LambdaServerFactory.eventType = eventType

        ImposterBuilderKt()
            .withPluginClass(OpenApiPluginImpl::class.java)
            .withPluginClass(RestPluginImpl::class.java)
            .withPluginClass(SoapPluginImpl::class.java)
            .apply {
                if (Settings.metaInfScan) {
                    withPluginClass(MetaInfPluginDetectorImpl::class.java)
                }
                Settings.configDirs.forEach { withConfigurationDir(it) }
            }
            .withEngineOptions { options ->
                options.serverFactory = LambdaServerFactory::class.qualifiedName

                Settings.pluginDiscoveryStrategyClass?.let { discoveryStrategy ->
                    options.pluginDiscoveryStrategyClass = discoveryStrategy
                } ?: run {
                    options.pluginDiscoveryStrategy = PluginUtil.buildStaticDiscoveryStrategy()
                }
            }.startBlocking()

        val serverFactory = InjectorUtil.getInstance<LambdaServerFactory>()

        @Suppress("UNCHECKED_CAST")
        server = serverFactory.activeServer as LambdaServer<Request, Response>

        logger.info("Imposter handler ready")
    }
}
