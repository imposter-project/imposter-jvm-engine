/*
 * Copyright (c) 2016-2024.
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
import io.gatehill.imposter.util.HttpUtil
import io.restassured.RestAssured
import io.restassured.http.ContentType
import io.vertx.core.Vertx
import io.vertx.junit5.VertxTestContext
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Tests for XML response serialisation in the OpenAPI plugin.
 */
internal class XmlExamplesTest : BaseVerticleTest() {
    override val pluginClass = OpenApiPluginImpl::class.java

    @BeforeEach
    @Throws(Exception::class)
    override fun setUp(vertx: Vertx, testContext: VertxTestContext) {
        super.setUp(vertx, testContext)
        RestAssured.baseURI = "http://$host:$listenPort"
    }

    override val testConfigDirs = listOf(
        "/openapi3/xml-examples"
    )

    @Test
    fun testServeSchemaExamplesAsXml() {
        val body = RestAssured.given()
            .log().ifValidationFails()
            .accept(ContentType.XML)
            .`when`().get("/xml/pets")
            .then()
            .log().ifValidationFails()
            .statusCode(HttpUtil.HTTP_OK)
            .contentType(ContentType.XML)
            .extract().asString()

        // Verify valid XML with expected elements
        assertTrue(body.contains("<root>"), "Response should contain <root> element")
        assertTrue(body.contains("<name>"), "Response should contain <name> element")
        assertTrue(body.contains("<id>"), "Response should contain <id> element")
        assertTrue(body.contains("<breed>Collie</breed>"), "Response should contain breed example value")
        assertTrue(body.contains("</root>"), "Response should contain closing </root> element")
    }

    @Test
    fun testServeSchemaArrayExamplesAsXml() {
        val body = RestAssured.given()
            .log().ifValidationFails()
            .accept(ContentType.XML)
            .`when`().get("/xml/pets/list")
            .then()
            .log().ifValidationFails()
            .statusCode(HttpUtil.HTTP_OK)
            .contentType(ContentType.XML)
            .extract().asString()

        // Verify valid XML with list structure
        assertTrue(body.contains("<items>"), "Response should contain <items> wrapper element")
        assertTrue(body.contains("<item>"), "Response should contain <item> element")
        assertTrue(body.contains("<name>"), "Response should contain <name> element")
        assertTrue(body.contains("<id>"), "Response should contain <id> element")
        assertTrue(body.contains("</items>"), "Response should contain closing </items> element")
    }

    @Test
    fun testServeInlineExampleAsXml() {
        val body = RestAssured.given()
            .log().ifValidationFails()
            .accept(ContentType.XML)
            .`when`().get("/xml/pets/inline")
            .then()
            .log().ifValidationFails()
            .statusCode(HttpUtil.HTTP_OK)
            .contentType(ContentType.XML)
            .extract().asString()

        // Verify inline example values are present
        assertTrue(body.contains("<root>"), "Response should contain <root> element")
        assertTrue(body.contains("<name>Buddy</name>"), "Response should contain inline name value")
        assertTrue(body.contains("<id>1</id>"), "Response should contain inline id value")
        assertTrue(body.contains("<breed>Labrador</breed>"), "Response should contain inline breed value")
    }

    @Test
    fun testServeXmlWhenAcceptingXml() {
        // Request XML from an endpoint that supports both JSON and XML
        val body = RestAssured.given()
            .log().ifValidationFails()
            .accept(ContentType.XML)
            .`when`().get("/xml/pets/both")
            .then()
            .log().ifValidationFails()
            .statusCode(HttpUtil.HTTP_OK)
            .contentType(ContentType.XML)
            .extract().asString()

        // Verify XML format, not JSON
        assertTrue(body.contains("<root>"), "Response should be XML format")
        assertTrue(body.contains("<name>"), "Response should contain <name> element")
        assertFalse(body.contains("{"), "Response should not contain JSON braces")
    }

    @Test
    fun testServeJsonWhenAcceptingJson() {
        // Request JSON from an endpoint that supports both JSON and XML
        val body = RestAssured.given()
            .log().ifValidationFails()
            .accept(ContentType.JSON)
            .`when`().get("/xml/pets/both")
            .then()
            .log().ifValidationFails()
            .statusCode(HttpUtil.HTTP_OK)
            .contentType(ContentType.JSON)
            .extract().asString()

        // Verify JSON format, not XML
        assertTrue(body.contains("{"), "Response should be JSON format")
        assertTrue(body.contains("\"name\""), "Response should contain JSON name key")
        assertFalse(body.contains("<root>"), "Response should not contain XML root element")
    }

    @Test
    fun testServeSchemaWithCustomXmlRootName() {
        val body = RestAssured.given()
            .log().ifValidationFails()
            .accept(ContentType.XML)
            .`when`().get("/xml/pets/named")
            .then()
            .log().ifValidationFails()
            .statusCode(HttpUtil.HTTP_OK)
            .contentType(ContentType.XML)
            .extract().asString()

        // Verify custom root element name from schema xml.name
        assertTrue(body.contains("<Pet>"), "Response should use custom root element 'Pet'")
        assertTrue(body.contains("<name>"), "Response should contain <name> element")
        assertTrue(body.contains("<breed>Collie</breed>"), "Response should contain breed example value")
        assertTrue(body.contains("</Pet>"), "Response should contain closing </Pet>")
        assertFalse(body.contains("<root>"), "Response should not contain default <root>")
    }

    @Test
    fun testServeSchemaArrayWithCustomXmlNames() {
        val body = RestAssured.given()
            .log().ifValidationFails()
            .accept(ContentType.XML)
            .`when`().get("/xml/pets/named-list")
            .then()
            .log().ifValidationFails()
            .statusCode(HttpUtil.HTTP_OK)
            .contentType(ContentType.XML)
            .extract().asString()

        // Verify custom wrapper and item names from schema xml.name
        assertTrue(body.contains("<Pets>"), "Response should use custom wrapper element 'Pets'")
        assertTrue(body.contains("<Pet>"), "Response should use custom item element 'Pet'")
        assertTrue(body.contains("<name>"), "Response should contain <name> element")
        assertTrue(body.contains("</Pets>"), "Response should contain closing </Pets>")
        assertFalse(body.contains("<items>"), "Response should not contain default <items>")
        assertFalse(body.contains("<item>"), "Response should not contain default <item>")
    }

    @Test
    fun testServeInlineExampleWithCustomXmlRootName() {
        val body = RestAssured.given()
            .log().ifValidationFails()
            .accept(ContentType.XML)
            .`when`().get("/xml/pets/named-inline")
            .then()
            .log().ifValidationFails()
            .statusCode(HttpUtil.HTTP_OK)
            .contentType(ContentType.XML)
            .extract().asString()

        // Verify custom root element name with inline example
        assertTrue(body.contains("<Animal>"), "Response should use custom root element 'Animal'")
        assertTrue(body.contains("<name>Buddy</name>"), "Response should contain inline name value")
        assertTrue(body.contains("<id>1</id>"), "Response should contain inline id value")
        assertTrue(body.contains("</Animal>"), "Response should contain closing </Animal>")
        assertFalse(body.contains("<root>"), "Response should not contain default <root>")
    }
}
