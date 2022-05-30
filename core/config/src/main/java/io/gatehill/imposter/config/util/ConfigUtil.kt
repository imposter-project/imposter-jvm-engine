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
package io.gatehill.imposter.config.util

import com.fasterxml.jackson.databind.ObjectMapper
import io.gatehill.imposter.ImposterConfig
import io.gatehill.imposter.config.resolver.ConfigResolver
import io.gatehill.imposter.plugin.PluginManager
import io.gatehill.imposter.plugin.config.PluginConfigImpl
import io.gatehill.imposter.plugin.config.ResourcesHolder
import io.gatehill.imposter.util.MapUtil
import io.gatehill.imposter.util.ResourceUtil
import org.apache.commons.io.FileUtils
import org.apache.commons.text.StringSubstitutor
import org.apache.logging.log4j.LogManager
import java.io.File
import java.io.IOException
import java.nio.charset.StandardCharsets

/**
 * Utility methods for reading configuration.
 *
 * @author Pete Cornish
 */
object ConfigUtil {
    private val LOGGER = LogManager.getLogger(ConfigUtil::class.java)
    const val CURRENT_PACKAGE = "io.gatehill.imposter"
    private const val CONFIG_FILE_SUFFIX = "-config"
    private const val IGNORE_FILE_NAME = ".imposterignore"

    private val CONFIG_FILE_MAPPERS: Map<String, ObjectMapper> = mapOf(
        ".json" to MapUtil.JSON_MAPPER,
        ".yaml" to MapUtil.YAML_MAPPER,
        ".yml" to MapUtil.YAML_MAPPER
    )

    private val scanRecursiveConfig
        get() = EnvVars.getEnv("IMPOSTER_CONFIG_SCAN_RECURSIVE")?.toBoolean() == true

    private var placeholderSubstitutor: StringSubstitutor? = null

    private val defaultIgnoreList: List<String> by lazy {
        ConfigUtil::class.java.getResourceAsStream("/$IGNORE_FILE_NAME")?.use {
            parseExclusionsFile(it.reader().readLines())
        } ?: run {
            LOGGER.warn("Failed to find default Imposter ignore file '$IGNORE_FILE_NAME' on classpath")
            emptyList()
        }
    }

    val resolvers: Set<ConfigResolver>

    init {
        resolvers = registerResolvers()
        initInterpolators(EnvVars.getEnv())
    }

    private fun registerResolvers(): Set<ConfigResolver> {
        val configResolvers = MetaUtil.readConfigResolverMetaFiles()
        LOGGER.trace("Configuration resolvers: {}", configResolvers)

        return configResolvers.distinct().map { resolver ->
            try {
                resolver.getDeclaredConstructor().newInstance()
            } catch (e: Exception) {
                throw RuntimeException("Error instantiating configuration resolver: $resolver", e)
            }
        }.toSet()
    }

    fun initInterpolators(environment: Map<String, String>) {
        // prefix all environment vars with 'env.'
        val environmentVars: Map<String, String> = environment.entries.associate { (key, value) ->
            "env.$key" to value
        }
        placeholderSubstitutor = StringSubstitutor(environmentVars)
    }

    /**
     * Resolves configuration directories to local paths, then
     * discovers configuration files. File discovery is optionally
     * recursive, defaulting to the value of [scanRecursiveConfig].
     */
    fun discoverConfigFiles(rawConfigDirs: Array<String>, scanRecursive: Boolean = scanRecursiveConfig): List<File> {
        val configDirs = resolveToLocal(rawConfigDirs)
        val exclusions = buildExclusions(configDirs)

        val configFiles = mutableListOf<File>()
        for (configDir in configDirs) {
            try {
                configFiles += listConfigFiles(configDir, scanRecursive, exclusions)
            } catch (e: Exception) {
                throw RuntimeException(e)
            }
        }
        return configFiles
    }

    private fun buildExclusions(configDirs: List<File>): List<String> {
        val exclusions = configDirs.flatMap { configDir ->
            val ignoreFile = File(configDir, IGNORE_FILE_NAME)
            if (ignoreFile.exists()) {
                parseExclusionsFile(ignoreFile.readLines())
            } else {
                emptyList()
            }
        }.takeUnless { it.isEmpty() } ?: defaultIgnoreList

        LOGGER.trace("Excluded from config file search: {}", exclusions)
        return exclusions
    }

    private fun parseExclusionsFile(rawEntries: List<String>) =
        rawEntries.filter { it.isNotBlank() && !it.startsWith("#") }

    fun loadPluginConfigs(
        imposterConfig: ImposterConfig,
        pluginManager: PluginManager,
        configFiles: List<File>
    ): Map<String, List<File>> {
        var configCount = 0

        // read all config files
        val allPluginConfigs = mutableMapOf<String, MutableList<File>>()
        try {
            for (configFile in configFiles) {
                LOGGER.debug("Loading configuration file: {}", configFile)
                configCount++

                // load to determine plugin
                val config = loadPluginConfig(
                    imposterConfig,
                    configFile,
                    PluginConfigImpl::class.java,
                    substitutePlaceholders = true,
                    convertPathParameters = false
                )

                val pluginClass = pluginManager.determinePluginClass(config.plugin!!)
                val pluginConfigs = allPluginConfigs.getOrPut(pluginClass) { mutableListOf() }
                pluginConfigs.add(configFile)
            }
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
        LOGGER.trace("Loaded $configCount plugin configuration file(s): $configFiles")
        return allPluginConfigs
    }

    fun listConfigFiles(configDir: File, scanRecursive: Boolean, exclusions: List<String>): List<File> {
        val configFiles = mutableListOf<File>()

        configDir.listFiles()?.filterNot { exclusions.contains(it.name) }?.forEach { file ->
            if (isConfigFile(file.name)) {
                configFiles += file
            } else {
                if (scanRecursive && file.isDirectory) {
                    configFiles += listConfigFiles(file, scanRecursive, exclusions)
                }
            }
        }

        LOGGER.trace("Configuration files discovered in {}: {}", configDir, configFiles)
        return configFiles
    }

    private fun resolveToLocal(rawConfigDirs: Array<String>): List<File> {
        return rawConfigDirs.map { rawDir ->
            val resolver = resolvers.firstOrNull { it.handles(rawDir) }
                ?: throw IllegalStateException("No config resolver can handle path: $rawDir")

            return@map resolver.resolve(rawDir)
        }
    }

    private fun isConfigFile(name: String): Boolean {
        return CONFIG_FILE_MAPPERS.keys.any { extension -> name.endsWith(CONFIG_FILE_SUFFIX + extension) }
    }

    /**
     * Reads the contents of the configuration file, performing necessary string substitutions.
     *
     * @param configFile             the configuration file
     * @param configClass            the configuration class
     * @param substitutePlaceholders whether to substitute placeholders in the configuration
     * @param convertPathParameters  whether to convert path parameters from OpenAPI format to Vert.x format
     * @return the configuration
     */
    fun <T : PluginConfigImpl> loadPluginConfig(
        imposterConfig: ImposterConfig,
        configFile: File,
        configClass: Class<T>,
        substitutePlaceholders: Boolean,
        convertPathParameters: Boolean
    ): T {
        try {
            val rawContents = FileUtils.readFileToString(configFile, StandardCharsets.UTF_8)
            val parsedContents = if (substitutePlaceholders) {
                placeholderSubstitutor!!.replace(rawContents)
            } else {
                rawContents
            }

            val config = lookupMapper(configFile).readValue(parsedContents, configClass)!!
            config.parentDir = configFile.parentFile

            // convert any OpenAPI format path parameters
            if (convertPathParameters && config is ResourcesHolder<*>) {
                (config as ResourcesHolder<*>).resources?.forEach { resource ->
                    resource.path = ResourceUtil.convertPathFromOpenApi(resource.path)
                }
            }

            if (imposterConfig.useEmbeddedScriptEngine) {
                config.responseConfig.scriptFile = "embedded"
            }

            check(config.plugin != null) { "No plugin specified in configuration file: $configFile" }
            return config

        } catch (e: IOException) {
            throw RuntimeException("Error reading configuration file: " + configFile.absolutePath, e)
        }
    }

    /**
     * Determine the mapper to use based on the filename.
     *
     * @param configFile the configuration file
     * @return the mapper
     */
    private fun lookupMapper(configFile: File?): ObjectMapper {
        val extension = configFile!!.name.substring(configFile.name.lastIndexOf("."))
        return CONFIG_FILE_MAPPERS[extension]
            ?: throw IllegalStateException("Unable to locate mapper for config file: $configFile")
    }
}
