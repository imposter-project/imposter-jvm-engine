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

package io.gatehill.imposter.script

import io.gatehill.imposter.config.util.EnvVars
import io.gatehill.imposter.http.HttpRequest
import io.gatehill.imposter.plugin.config.PluginConfig
import java.nio.file.Path
import java.nio.file.Paths

/**
 * Convenience methods for script execution.
 *
 * @author Pete Cornish
 */
object ScriptUtil {
    /**
     * Environment variable name for configuring the number of script cache entries.
     */
    const val ENV_SCRIPT_CACHE_ENTRIES = "IMPOSTER_SCRIPT_CACHE_ENTRIES"

    /**
     * Default number of script cache entries if not specified by environment variable.
     */
    const val DEFAULT_SCRIPT_CACHE_ENTRIES = 20L

    /**
     * Flag indicating whether scripts should be precompiled.
     * Determined by the `IMPOSTER_SCRIPT_PRECOMPILE` environment variable.
     * Defaults to true if the environment variable is not set.
     */
    val shouldPrecompile = EnvVars.getEnv("IMPOSTER_SCRIPT_PRECOMPILE")?.toBoolean() != false

    private val forceHeaderKeyNormalisation =
        EnvVars.getEnv("IMPOSTER_NORMALISE_HEADER_KEYS")?.toBoolean() != false

    /**
     * Resolves the absolute path to a script file based on the plugin configuration directory.
     *
     * @param pluginConfig The plugin configuration containing the base directory
     * @param scriptFile The relative path or name of the script file
     * @return The absolute Path to the script file
     */
    fun resolveScriptPath(pluginConfig: PluginConfig, scriptFile: String?): Path =
        Paths.get(pluginConfig.dir.absolutePath, scriptFile!!)

    /**
     * Processes HTTP headers from a request, optionally normalising header keys to lowercase.
     * The normalisation behaviour is controlled by the `IMPOSTER_NORMALISE_HEADER_KEYS` environment variable.
     *
     * @param request The HTTP request containing headers to process
     * @return A map of header names to values, with keys potentially normalised to lowercase
     */
    fun caseHeaders(request: HttpRequest): Map<String, String> {
        val entries = request.headers
        return if (forceHeaderKeyNormalisation) {
            LowercaseKeysMap(entries)
        } else {
            entries
        }
    }
}
