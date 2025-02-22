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
package io.gatehill.imposter.server

import io.gatehill.imposter.plugin.test.TestPluginImpl
import io.gatehill.imposter.util.HttpUtil
import io.restassured.RestAssured
import io.restassured.http.ContentType
import io.vertx.core.Vertx
import io.vertx.junit5.VertxTestContext
import org.hamcrest.Matchers
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File

/**
 * Tests for response templates.
 *
 * @author Pete Cornish
 */
class ResponseTemplateTest : BaseVerticleTest() {
    override val pluginClass = TestPluginImpl::class.java

    @BeforeEach
    @Throws(Exception::class)
    override fun setUp(vertx: Vertx, testContext: VertxTestContext) {
        super.setUp(vertx, testContext)
        RestAssured.baseURI = "http://$host:$listenPort"
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails()
    }

    override val testConfigDirs = listOf(
        "/response-template"
    )

    /**
     * Interpolate a simple template placeholder in a file using a store value.
     */
    @Test
    fun testSimpleInterpolatedTemplateFromFile() {
        // create item
        RestAssured.given().`when`()
            .pathParam("storeId", "templateTest")
            .pathParam("key", "foo")
            .contentType(HttpUtil.CONTENT_TYPE_PLAIN_TEXT)
            .body("bar")
            .put("/system/store/{storeId}/{key}")
            .then()
            .statusCode(Matchers.equalTo(HttpUtil.HTTP_CREATED))

        // read interpolated response
        RestAssured.given().`when`().get("/example")
            .then()
            .statusCode(Matchers.equalTo(HttpUtil.HTTP_OK))
            .body(Matchers.equalTo("Hello bar!")) // content type inferred from response file name
            .contentType(ContentType.TEXT)
    }

    /**
     * Interpolate a simple template placeholder from inline data using a store value.
     */
    @Test
    fun testSimpleInterpolatedTemplateFromInlineData() {
        // create item
        RestAssured.given().`when`()
            .pathParam("storeId", "templateTest")
            .pathParam("key", "foo-inline")
            .contentType(HttpUtil.CONTENT_TYPE_PLAIN_TEXT)
            .body("bar")
            .put("/system/store/{storeId}/{key}")
            .then()
            .statusCode(Matchers.equalTo(HttpUtil.HTTP_CREATED))

        // read interpolated response
        RestAssured.given().`when`().get("/example-inline")
            .then()
            .statusCode(Matchers.equalTo(HttpUtil.HTTP_OK))
            .body(Matchers.equalTo("Inline bar")) // content type inferred from response file name
            .contentType(ContentType.TEXT)
    }

    /**
     * Interpolate a request-scoped template placeholder.
     */
    @Test
    fun testRequestScopedInterpolatedTemplate() {
        RestAssured.given().`when`()
            .contentType(ContentType.JSON)
            .pathParam("petId", 99)
            .put("/pets/{petId}")
            .then()
            .statusCode(Matchers.equalTo(HttpUtil.HTTP_OK))
            .body(Matchers.equalTo("Pet ID: 99")) // content type inferred from response file name
            .contentType(ContentType.TEXT)
    }

    /**
     * Interpolate a JsonPath template placeholder using a store value.
     */
    @Test
    @Throws(Exception::class)
    fun testJsonPathInterpolatedTemplate() {
        val user = File(CaptureTest::class.java.getResource("/response-template/user.json").toURI()).readText()
        RestAssured.given().`when`()
            .body(user)
            .contentType(ContentType.JSON)
            .post("/users")
            .then()
            .statusCode(Matchers.equalTo(HttpUtil.HTTP_OK))
            .body(Matchers.equalTo("Postcode: PO5 7CO")) // content type inferred from response file name
            .contentType(ContentType.TEXT)
    }

    /**
     * Interpolate a simple expression, without any capture.
     */
    @Test
    @Throws(Exception::class)
    fun testExpressionTemplate() {
        RestAssured.given().`when`()
            .get("/greeting/world")
            .then()
            .statusCode(Matchers.equalTo(HttpUtil.HTTP_OK))
            .body(Matchers.equalToCompressingWhiteSpace("Hello world!"))
            .contentType(ContentType.TEXT)
    }

    /**
     * Fall back to a value when an expression resolves to null.
     */
    @Test
    @Throws(Exception::class)
    fun testFallbackExpression() {
        RestAssured.given().`when`()
            .get("/fallback")
            .then()
            .statusCode(Matchers.equalTo(HttpUtil.HTTP_OK))
            .body(Matchers.equalToCompressingWhiteSpace("fallback"))
            .contentType(ContentType.TEXT)
    }
}
