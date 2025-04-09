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

package io.gatehill.imposter.scripting.common.util

import io.gatehill.imposter.service.ScriptSource
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.file.Files
import kotlin.io.path.pathString

/**
 * Tests for [JavaScriptUtil].
 */
class JavaScriptUtilTest {
    @Test
    fun `wrapScript adds CJS require shim when script uses @imposter-js types import`() {
        // Create a script that uses the @imposter-js/types import
        val scriptWithImport = """
            const types = require('@imposter-js/types');
            console.log(types.logger);
        """.trimIndent()
        
        val scriptSource = ScriptSource(
            source = "test-script-with-import",
            code = scriptWithImport
        )
        val wrappedScript = JavaScriptUtil.wrapScript(scriptSource)
        
        // Verify that the CJS require shim is included in the wrapped script
        assertTrue(wrappedScript.code.contains("function require(moduleName)"))
        assertTrue(wrappedScript.code.contains("if (\"@imposter-js/types\" !== moduleName)"))
        assertTrue(wrappedScript.code.contains("var __imposter_types ="))
    }
    
    @Test
    fun `wrapScript adds CJS require shim when script uses __imposter_types direct reference`() {
        // Create a script that directly uses __imposter_types
        val scriptWithDirectReference = """
            console.log(__imposter_types.logger);
        """.trimIndent()
        
        val scriptSource = ScriptSource(
            source = "test-script-with-direct-reference",
            code = scriptWithDirectReference
        )
        val wrappedScript = JavaScriptUtil.wrapScript(scriptSource)
        
        // Verify that the CJS require shim is included in the wrapped script
        assertTrue(wrappedScript.code.contains("var __imposter_types ="))
        assertTrue(wrappedScript.code.contains("function require(moduleName)"))
    }
    
    @Test
    fun `wrapScript doesn't add CJS require shim when script doesn't use @imposter-js types or __imposter_types`() {
        // Create a script that doesn't use @imposter-js/types or __imposter_types
        val scriptWithoutImport = """
            console.log('Hello, world!');
        """.trimIndent()
        
        val scriptSource = ScriptSource(
            source = "test-script-without-import",
            code = scriptWithoutImport
        )
        val wrappedScript = JavaScriptUtil.wrapScript(scriptSource)
        
        // Verify that the CJS require shim is NOT included in the wrapped script
        assertFalse(wrappedScript.code.contains("function require(moduleName)"))
        assertFalse(wrappedScript.code.contains("var __imposter_types ="))
    }
    
    @Test
    fun `wrapScript from file adds CJS require shim when file uses @imposter-js types`() {
        // Create a temporary file with content that uses @imposter-js/types
        val tempFile = Files.createTempFile("test-script-", ".js")
        Files.writeString(tempFile, """
            const types = require('@imposter-js/types');
            console.log(types.logger);
        """.trimIndent())
        
        try {
            val scriptSource = ScriptSource(
                // use path as source to allow reuse of script cache
                source = tempFile.pathString,
                file = tempFile,
            )
            val wrappedScript = JavaScriptUtil.wrapScript(scriptSource)
            
            // Verify that the CJS require shim is included in the wrapped script
            assertTrue(wrappedScript.code.contains("function require(moduleName)"))
            assertTrue(wrappedScript.code.contains("var __imposter_types ="))
        } finally {
            // Clean up
            Files.deleteIfExists(tempFile)
        }
    }
    
    @Test
    fun `wrapScript from file adds CJS require shim when file uses __imposter_types direct reference`() {
        // Create a temporary file with content that uses __imposter_types directly
        val tempFile = Files.createTempFile("test-script-", ".js")
        Files.writeString(tempFile, """
            console.log(__imposter_types.logger);
        """.trimIndent())
        
        try {
            val scriptSource = ScriptSource(
                // use path as source to allow reuse of script cache
                source = tempFile.pathString,
                file = tempFile,
            )
            val wrappedScript = JavaScriptUtil.wrapScript(scriptSource)
            
            // Verify that the CJS require shim is included in the wrapped script
            assertTrue(wrappedScript.code.contains("var __imposter_types ="))
        } finally {
            // Clean up
            Files.deleteIfExists(tempFile)
        }
    }
}