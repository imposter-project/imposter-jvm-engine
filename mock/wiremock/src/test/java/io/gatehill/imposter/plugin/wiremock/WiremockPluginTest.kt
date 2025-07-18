/*
 * Copyright (c) 2023-2024.
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

package io.gatehill.imposter.plugin.wiremock

import io.gatehill.imposter.ImposterConfig
import io.gatehill.imposter.config.ConfigReference
import io.gatehill.imposter.config.LoadedConfig
import io.gatehill.imposter.config.util.EnvVars
import io.gatehill.imposter.service.HandlerService
import io.gatehill.imposter.service.InterceptorService
import io.gatehill.imposter.service.ResponseFileService
import io.gatehill.imposter.service.ResponseRoutingService
import io.gatehill.imposter.service.ResponseService
import io.vertx.core.Vertx
import org.hamcrest.CoreMatchers.containsString
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.hasItem
import org.hamcrest.CoreMatchers.not
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.hasSize
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import java.io.File

/**
 * Verifies converting wiremock mappings to Imposter config.
 *
 * @author Pete Cornish
 */
class WiremockPluginTest {
    @Test
    fun `can convert unwrapped wiremock mappings`() {
        val configDir = convert("/wiremock-nowrap")

        val files = configDir.listFiles()?.map { it.name }
        Assertions.assertEquals(2, files?.size)

        assertThat(files, hasItem("wiremock-0-config.json"))
        assertThat(files, hasItem("files"))
    }

    @Test
    fun `can convert simple wiremock mappings to single config file`() {
        val configDir = convert("/wiremock-simple")

        val files = configDir.listFiles()?.map { it.name }
        Assertions.assertEquals(2, files?.size)

        assertThat(files, hasItem("wiremock-0-config.json"))
        assertThat(files, hasItem("files"))

        val responseFileDir = File(configDir, "files")
        val responseFiles = responseFileDir.listFiles()?.map { it.name }
        Assertions.assertEquals(1, responseFiles?.size)
        assertThat(responseFiles, hasItem("response.json"))
    }

    @Test
    fun `can convert simple wiremock mappings to separate config files`() {
        val configDir = convert("/wiremock-simple", 2, "IMPOSTER_WIREMOCK_SEPARATE_CONFIG" to "true")

        val files = configDir.listFiles()?.map { it.name }
        Assertions.assertEquals(3, files?.size)

        assertThat(files, hasItem("wiremock-0-config.json"))
        assertThat(files, hasItem("wiremock-1-config.json"))
        assertThat(files, hasItem("files"))

        val responseFileDir = File(configDir, "files")
        val responseFiles = responseFileDir.listFiles()?.map { it.name }
        Assertions.assertEquals(1, responseFiles?.size)
        assertThat(responseFiles, hasItem("response.json"))
    }

    @Test
    fun `can convert templated wiremock mappings`() {
        val configDir = convert("/wiremock-templated")

        val files = configDir.listFiles()?.map { it.name }
        Assertions.assertEquals(2, files?.size)

        assertThat(files, hasItem("wiremock-0-config.json"))
        assertThat(files, hasItem("files"))

        val responseFileDir = File(configDir, "files")
        val responseFiles = responseFileDir.listFiles()?.map { it.name }
        Assertions.assertEquals(1, responseFiles?.size)
        assertThat(responseFiles, hasItem("response.xml"))

        val responseFile = File(responseFileDir, "response.xml").readText()
        assertThat(responseFile, not(containsString("{{")))
        assertThat(
            responseFile,
            containsString("\${context.request.body://getPetByIdRequest/id}")
        )
        assertThat(
            responseFile,
            containsString("\${random.alphabetic(length=5,uppercase=true)}")
        )
    }

    private fun convert(mappingsPath: String, expectedConfigFiles: Int = 1, vararg env: Pair<String, String>): File {
        val mappingsDir = File(WiremockPluginTest::class.java.getResource(mappingsPath)!!.toURI())

        if (env.isNotEmpty()) {
            EnvVars.populate(*env)
        }

        val wiremock = WiremockPluginImpl(
            mock<Vertx>(),
            ImposterConfig(),
            mock<HandlerService>(),
            mock<InterceptorService>(),
            mock<ResponseFileService>(),
            mock<ResponseService>(),
            mock<ResponseRoutingService>()
        )

        val configRef = ConfigReference(
            file = File(mappingsDir, "imposter-config.yaml"),
            configRoot = mappingsDir
        )
        val loadedConfig = LoadedConfig(configRef, configRef.file.readText(), "wiremock")
        val loadedConfigs = wiremock.convert(loadedConfig)
        assertThat(loadedConfigs, hasSize(expectedConfigFiles))

        val configDir = loadedConfigs.first().ref.file.parentFile

        assertTrue(configDir.exists(), "Config dir should exist")
        assertThat(
            "Config dir should differ from source dir",
            configDir,
            not(equalTo(mappingsDir))
        )
        return configDir
    }
}
