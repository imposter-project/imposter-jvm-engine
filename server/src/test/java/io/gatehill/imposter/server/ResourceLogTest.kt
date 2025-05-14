/*
 * Copyright (c) 2024.
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
package io.gatehill.imposter.server

import io.gatehill.imposter.plugin.test.TestPluginImpl
import io.gatehill.imposter.service.ResponseServiceImpl
import io.restassured.RestAssured
import io.vertx.core.Vertx
import io.vertx.junit5.VertxTestContext
import org.apache.logging.log4j.Level
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.core.LogEvent
import org.apache.logging.log4j.core.Logger
import org.apache.logging.log4j.core.appender.AbstractAppender
import org.apache.logging.log4j.core.config.Property
import org.apache.logging.log4j.core.layout.PatternLayout
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Tests for resource logging functionality.
 */
class ResourceLogTest : BaseVerticleTest() {
    override val pluginClass = TestPluginImpl::class.java
    
    private lateinit var memoryAppender: MemoryAppender
    private lateinit var logger: Logger

    @BeforeEach
    override fun setUp(vertx: Vertx, testContext: VertxTestContext) {
        super.setUp(vertx, testContext)
        RestAssured.baseURI = "http://$host:$listenPort"
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails()
        
        // Setup logging capture
        logger = LogManager.getLogger(ResponseServiceImpl::class.java) as Logger
        memoryAppender = MemoryAppender("MemoryAppender-${UUID.randomUUID()}")
        memoryAppender.start()
        logger.addAppender(memoryAppender)
    }
    
    @AfterEach
    fun tearDownLogger() {
        logger.removeAppender(memoryAppender)
        memoryAppender.stop()
    }

    override val testConfigDirs = listOf(
        "/resource-log"
    )

    @Test
    fun `resource should log message with path parameters`() {
        // Clear logs before test
        memoryAppender.clear()
        
        // Make the request
        RestAssured.given().`when`()
            .get("/resource-log/123")
            .then()
            .statusCode(200)
            .body(equalTo("resource_logged"))
        
        // Verify log message was captured
        val logEvents = memoryAppender.getLogEvents()
        assertTrue(logEvents.any { event -> 
            event.level == Level.INFO && 
            event.message.formattedMessage.contains("Resource log: Resource log message for ID: 123") 
        }, "Expected log message with path parameter not found")
    }

    @Test
    fun `interceptor should log message`() {
        // Clear logs before test
        memoryAppender.clear()

        // Make the request
        RestAssured.given().`when`()
            .get("/interceptor-log")
            .then()
            .statusCode(401)
            .body(equalTo("Unauthorized"))

        // Verify log message was captured
        val logEvents = memoryAppender.getLogEvents()
        assertTrue(logEvents.any { event ->
            event.level == Level.INFO &&
            event.message.formattedMessage.contains("Resource log: Interceptor log message")
        }, "Expected interceptor log message not found")
    }

    @Test
    fun `request without log property should not log custom message`() {
        // Clear logs before test
        memoryAppender.clear()
        
        // Make the request to a path without log property
        RestAssured.given().`when`()
            .get("/simple")
            .then()
            .statusCode(200)
            .body(equalTo("simple"))
        
        // Verify no custom log message was captured
        val logEvents = memoryAppender.getLogEvents()
        assertTrue(logEvents.none { event -> 
            event.level == Level.INFO && 
            event.message.formattedMessage.contains("Resource log:") 
        }, "Custom log message found when none was expected")
    }
    
    /**
     * Custom in-memory appender for capturing log events during tests.
     */
    class MemoryAppender(name: String) : AbstractAppender(
        name,
        null,
        PatternLayout.createDefaultLayout(),
        false,
        Property.EMPTY_ARRAY
    ) {
        private val logEvents = CopyOnWriteArrayList<LogEvent>()
        
        override fun append(event: LogEvent) {
            logEvents.add(event.toImmutable())
        }
        
        fun getLogEvents(): List<LogEvent> = logEvents.toList()
        
        fun clear() {
            logEvents.clear()
        }
    }
}