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
package io.gatehill.imposter.plugin.config

import io.gatehill.imposter.ImposterConfig
import io.gatehill.imposter.config.LoadedConfig
import io.gatehill.imposter.config.util.ConfigUtil
import io.gatehill.imposter.http.UniqueRoute
import io.gatehill.imposter.plugin.RoutablePlugin
import io.gatehill.imposter.plugin.config.resource.PassthroughResourceConfig
import io.gatehill.imposter.util.ResourceUtil
import io.vertx.core.Vertx
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import java.io.File
import javax.inject.Inject

/**
 * @author Pete Cornish
 */
abstract class ConfiguredPlugin<T : BasicPluginConfig> @Inject constructor(
    protected val vertx: Vertx,
    protected val imposterConfig: ImposterConfig
) : RoutablePlugin, ConfigurablePlugin<T> {

    private val logger: Logger = LogManager.getLogger(ConfiguredPlugin::class.java)
    override var configs: List<T> = emptyList()

    protected abstract val configClass: Class<T>

    override fun loadConfiguration(loadedConfigs: List<LoadedConfig>) {
        configs = loadedConfigs.mapNotNull { loadedConfig ->
            try {
                val config = ConfigUtil.loadPluginConfig(imposterConfig, loadedConfig, configClass)
                validateConfig(loadedConfig.ref.file, config)
                return@mapNotNull config

            } catch (e: Exception) {
                val configEx = RuntimeException("Error loading plugin config: $loadedConfig", e)
                if (ConfigUtil.ignoreConfigErrors) {
                    logger.warn("Skipping plugin configuration with error", e)
                    return@mapNotNull null
                } else {
                    throw configEx
                }
            }
        }
        configurePlugin(configs)
    }

    protected fun validateConfig(file: File, config: T) {
        try {
            if (config is ResourcesHolder<*>) {
                config.resources?.forEach { resource ->
                    if (resource is PassthroughResourceConfig && resource.responseConfig.hasConfiguration() && !resource.passthrough.isNullOrBlank()) {
                        throw IllegalArgumentException("Passthrough and response configuration are mutually exclusive")
                    }
                }
            }
        } catch (e: Exception) {
            throw IllegalArgumentException("Invalid configuration in file: ${file.absolutePath}", e)
        }
    }

    /**
     * Strongly typed configuration objects for this plugin.
     *
     * @param configs
     */
    protected open fun configurePlugin(configs: List<T>) {
        /* no op */
    }

    /**
     * Iterates over [configs] and subresources to find unique route combinations
     * of path and HTTP method. For each combination found, only
     * the _first_ plugin configuration is returned.
     *
     * **Note:** static content routes are not returned.
     */
    protected fun findUniqueRoutes(): Map<UniqueRoute, T> {
        val unique = mutableMapOf<UniqueRoute, T>()
        configs.forEach { config ->
            // root resource
            config.takeUnless { ResourceUtil.isStaticContentRoute(config) }?.path?.let {
                val uniqueRoute = UniqueRoute.fromResourceConfig(config)
                unique[uniqueRoute] = config
            }

            // subresources
            if (config is ResourcesHolder<*>) {
                config.resources?.filterNot { ResourceUtil.isStaticContentRoute(it) }?.forEach { resource ->
                    val uniqueRoute = UniqueRoute.fromResourceConfig(resource)
                    if (!unique.containsKey(uniqueRoute)) {
                        unique[uniqueRoute] = config
                    }
                }
            }
        }
        return unique
    }
}
