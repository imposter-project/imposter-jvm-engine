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
package io.gatehill.imposter.plugin.openapi

import io.gatehill.imposter.server.BaseVerticleTest
import io.restassured.RestAssured
import io.restassured.http.ContentType
import io.vertx.core.Vertx
import io.vertx.junit5.VertxTestContext
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Tests for returning specific status codes from OpenAPI mocks.
 *
 * @author Pete Cornish
 */
class OverrideStatusCodeTest : BaseVerticleTest() {
    override val pluginClass = OpenApiPluginImpl::class.java

    @BeforeEach
    @Throws(Exception::class)
    override fun setUp(vertx: Vertx, testContext: VertxTestContext) {
        super.setUp(vertx, testContext)
        RestAssured.baseURI = "http://$host:$listenPort"
    }

    override val testConfigDirs = listOf(
        "/openapi3/override-status-code"
    )

    /**
     * Should return a specific status code for a simple request path.
     */
    @Test
    fun testSetStatusCodeForSimplePath() {
        RestAssured.given()
            .log().ifValidationFails()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .`when`()
            .body("{ \"id\": 1, \"name\": \"Cat\" }")
            .post("/pets")
            .then()
            .log().ifValidationFails()
            .statusCode(201)
    }

    /**
     * Should return a specific status code for a path parameter.
     */
    @Test
    fun testSetStatusCodeForPathParam() {
        RestAssured.given()
            .log().ifValidationFails()
            .accept(ContentType.JSON)
            .`when`().get("/pets/{petId}", "99")
            .then()
            .log().ifValidationFails()
            .statusCode(203)
    }

    /**
     * Should return a specific status code for a query parameter.
     */
    @Test
    fun testSetStatusCodeForQueryParam() {
        RestAssured.given()
            .log().ifValidationFails()
            .accept(ContentType.JSON)
            .`when`().get("/pets?foo=bar")
            .then()
            .log().ifValidationFails()
            .statusCode(204)
    }

    /**
     * Should return a specific status code for a request header.
     */
    @Test
    fun testSetStatusCodeForRequestHeader() {
        RestAssured.given()
            .log().ifValidationFails()
            .`when`()
            .header("X-Foo", "bar")["/pets"]
            .then()
            .log().ifValidationFails()
            .statusCode(205)
    }

    /**
     * Should return a specific status code for a request header, where
     * the case of the request header key differs from that of the configuration.
     */
    @Test
    fun testSetStatusCodeForRequestHeaderCaseInsensitive() {
        RestAssured.given()
            .log().ifValidationFails()
            .`when`() // header key deliberately uppercase in request, but lowercase in config
            .header("X-LOWERCASE-TEST", "baz")
            .body("{ \"id\": 1, \"name\": \"Cat\" }")
            .put("/pets/{petId}", "1")
            .then()
            .log().ifValidationFails()
            .statusCode(409)
    }

    /**
     * Should return a specific status code for a path with a placeholder.
     */
    @Test
    fun testSetStatusCodeForPathWithPlaceholder() {
        RestAssured.given()
            .log().ifValidationFails()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .`when`()
            .body("{ \"id\": 1, \"name\": \"Cat\" }")
            .put("/pets/{petId}", "1")
            .then()
            .log().ifValidationFails()
            .statusCode(202)
    }
}