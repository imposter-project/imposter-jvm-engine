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
package io.gatehill.imposter.plugin.rest

import io.gatehill.imposter.server.BaseVerticleTest
import io.gatehill.imposter.util.HttpUtil
import io.gatehill.imposter.util.HttpUtil.CONTENT_TYPE
import io.restassured.RestAssured
import io.restassured.config.RedirectConfig
import io.vertx.core.Vertx
import io.vertx.junit5.VertxTestContext
import org.hamcrest.Matchers
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Tests for [RestPluginImpl].
 *
 * @author Pete Cornish
 */
class RestPluginTest : BaseVerticleTest() {
    override val pluginClass = RestPluginImpl::class.java

    @BeforeEach
    @Throws(Exception::class)
    override fun setUp(vertx: Vertx, testContext: VertxTestContext) {
        super.setUp(vertx, testContext)
        RestAssured.baseURI = "http://$host:$listenPort"
        RestAssured.config().redirect(RedirectConfig.redirectConfig().followRedirects(false))
    }

    @Test
    fun testRequestStaticRootPathSuccess() {
        RestAssured.given().`when`()
            .get("/example")
            .then()
            .log().ifValidationFails()
            .statusCode(Matchers.equalTo(HttpUtil.HTTP_OK))
            .and()
            .contentType(Matchers.equalTo("application/json"))
            .and()
            .body("testKey", Matchers.equalTo("testValue1"))
    }

    @Test
    fun testRequestStaticArrayResourceSuccess() {
        fetchVerifyRow(1)
        fetchVerifyRow(2)
        fetchVerifyRow(3)
    }

    private fun fetchVerifyRow(rowId: Int) {
        RestAssured.given().`when`()
            .get("/example/$rowId")
            .then()
            .log().ifValidationFails()
            .statusCode(Matchers.equalTo(HttpUtil.HTTP_OK))
            .and()
            .contentType(Matchers.equalTo("application/json"))
            .and()
            .body("aKey", Matchers.equalTo("aValue$rowId"))
    }

    @Test
    fun testRequestScriptedResponseFile() {
        // default action should return static data file 1
        RestAssured.given().`when`()
            .get("/scripted")
            .then()
            .log().ifValidationFails()
            .statusCode(Matchers.equalTo(HttpUtil.HTTP_OK))
            .and()
            .contentType(Matchers.equalTo("application/json"))
            .and()
            .body("testKey", Matchers.equalTo("testValue1"))

        // default action should return static data file 2
        RestAssured.given().`when`()
            .get("/scripted?action=fetch")
            .then()
            .log().ifValidationFails()
            .statusCode(Matchers.equalTo(HttpUtil.HTTP_OK))
            .and()
            .contentType(Matchers.equalTo("application/json"))
            .and()
            .body("testKey", Matchers.equalTo("testValue2"))
    }

    @Test
    fun testRequestScriptedStatusCode() {
        // script causes short circuit to 201
        RestAssured.given().`when`()
            .get("/scripted?action=create")
            .then()
            .log().ifValidationFails()
            .statusCode(Matchers.equalTo(HttpUtil.HTTP_CREATED))
            .and()
            .body(Matchers.`is`(Matchers.emptyOrNullString()))

        // script causes short circuit to 204
        RestAssured.given().`when`()
            .get("/scripted?action=delete")
            .then()
            .log().ifValidationFails()
            .statusCode(Matchers.equalTo(HttpUtil.HTTP_NO_CONTENT))
            .and()
            .body(Matchers.`is`(Matchers.emptyOrNullString()))

        // script causes short circuit to 400
        RestAssured.given().`when`()
            .get("/scripted?bad")
            .then()
            .log().ifValidationFails()
            .statusCode(Matchers.equalTo(HttpUtil.HTTP_BAD_REQUEST))
            .and()
            .body(Matchers.`is`(Matchers.emptyOrNullString()))
    }

    @Test
    fun testRequestNotFound() {
        RestAssured.given().`when`()
            .get("/nonExistentEndpoint")
            .then()
            .log().ifValidationFails()
            .statusCode(Matchers.equalTo(HttpUtil.HTTP_NOT_FOUND))
    }

    @Test
    fun testRequestScriptedWithHeaders() {
        RestAssured.given().`when`()
            .header("Authorization", "AUTH_HEADER")["/scripted?with-auth"]
            .then()
            .log().ifValidationFails()
            .statusCode(Matchers.equalTo(HttpUtil.HTTP_NO_CONTENT))
    }

    /**
     * Tests status code, headers, static data and method for a single resource.
     */
    @Test
    fun testRequestStaticSingleFull() {
        RestAssured.given().`when`()
            .get("/static-single")
            .then()
            .log().ifValidationFails()
            .statusCode(Matchers.equalTo(HttpUtil.HTTP_OK))
            .header(CONTENT_TYPE, "text/html")
            .header("X-Example", "foo")
            .body(
                Matchers.allOf(
                    Matchers.containsString("<html>"),
                    Matchers.containsString("Hello, world!")
                )
            )
    }

    /**
     * Tests status code, headers, static data and method for a single resource.
     */
    @Test
    fun testRequestStaticMultiFull() {
        RestAssured.given().`when`()
            .post("/static-multi")
            .then()
            .log().ifValidationFails()
            .statusCode(Matchers.equalTo(HttpUtil.HTTP_MOVED_TEMP))
            .body(Matchers.emptyOrNullString())

        RestAssured.given().`when`().get("/static-multi")
            .then()
            .log().ifValidationFails()
            .statusCode(Matchers.equalTo(HttpUtil.HTTP_OK))
            .header(CONTENT_TYPE, "text/html")
            .header("X-Example", "foo")
            .body(
                Matchers.allOf(
                    Matchers.containsString("<html>"),
                    Matchers.containsString("Hello, world!")
                )
            )
    }
}
