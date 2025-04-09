/*
 * Copyright (c) 2023-2023.
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

import io.gatehill.imposter.config.util.EnvVars
import io.gatehill.imposter.plugin.test.TestPluginImpl
import io.gatehill.imposter.util.HttpTestUtil
import io.restassured.RestAssured
import io.restassured.RestAssured.given
import io.vertx.core.Handler
import io.vertx.core.Vertx
import io.vertx.core.http.HttpMethod
import io.vertx.core.http.HttpServerOptions
import io.vertx.core.http.HttpServerRequest
import io.vertx.junit5.VertxTestContext
import org.hamcrest.Matcher
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.concurrent.CountDownLatch
import java.util.function.Consumer

/**
 * Tests for remote steps.
 *
 * @author Pete Cornish
 */
class StepsRemoteTest : BaseVerticleTest() {
    override val pluginClass = TestPluginImpl::class.java

    @BeforeEach
    override fun setUp(vertx: Vertx, testContext: VertxTestContext) {
        super.setUp(vertx, testContext)
        RestAssured.baseURI = "http://$host:$listenPort"
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails()
    }

    override val testConfigDirs = listOf(
        "/steps-remote"
    )

    /**
     * Execute a remote step.
     */
    @Test
    fun `execute remote step`() {
        given().`when`()
            .queryParam("petId", "123")
            .get("/example")
            .then()
            .statusCode(201)
            .body(equalTo("Fluffy"))
    }

    companion object {
        private var vertx: Vertx? = null
        private var remoteServer: io. vertx. core. http.HttpServer? = null

        @JvmStatic
        @BeforeAll
        fun beforeClass() {
            vertx = Vertx.vertx()
            startRemoteServer()
        }

        private fun startRemoteServer() {
            val remoteServerPort = HttpTestUtil.findFreePort()
            EnvVars.populate("REMOTE_SERVER_PORT" to remoteServerPort.toString())

            val httpServer = vertx!!.createHttpServer(HttpServerOptions().setPort(remoteServerPort))
            httpServer.requestHandler { request ->
                println("Received remote request: $request")
                request.failOnAssertionError(request.method(), equalTo(HttpMethod.POST))
                request.failOnAssertionError(request.path(), equalTo("/"))
                request.failOnAssertionError(request.query(), equalTo("petId=123"))
                request.failOnAssertionError(request.getHeader("X-Test-Header"), equalTo("test"))
                request.body { body ->
                    request.failOnAssertionError(
                        body.result().toString(),
                        equalTo("""{ "type": "cat" }"""),
                    )
                    request.response().end("Fluffy")
                }
            }.also { server ->
                blockWait(server::listen)
            }
            remoteServer = httpServer
        }

        @JvmStatic
        @AfterAll
        @Throws(Exception::class)
        fun afterClass() {
            try {
                remoteServer?.close()
            } finally {
                vertx?.close()
            }
        }

        private fun <T> HttpServerRequest.failOnAssertionError(
            actual: T,
            assertion: Matcher<in T>,
        ) {
            try {
                assertThat(actual, assertion)
            } catch (e: AssertionError) {
                response().setStatusCode(400).end(e.message)
                throw e
            }
        }

        /**
         * Block the consumer until the handler is called.
         *
         * @param handlerConsumer the consumer of the handler
         * @param <T>             the type of the async result
         */
        @Throws(Exception::class)
        private fun <T> blockWait(handlerConsumer: Consumer<Handler<T>>) {
            val latch = CountDownLatch(1)
            val handler = Handler { _: T -> latch.countDown() }
            handlerConsumer.accept(handler)
            latch.await()
        }
    }
}
