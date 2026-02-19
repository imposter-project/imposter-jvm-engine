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
package io.gatehill.imposter.plugin.openapi.util

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * Tests for [XmlMapUtil].
 */
class XmlMapUtilTest {

    @Test
    fun `xmlify should return empty string for null`() {
        assertEquals("", XmlMapUtil.xmlify(null))
    }

    @Test
    fun `xmlify should serialise map as XML with root element`() {
        val data = mapOf("name" to "Buddy", "id" to 1)
        val xml = XmlMapUtil.xmlify(data)

        assertTrue(xml.contains("<root>"), "Should contain <root> element")
        assertTrue(xml.contains("<name>Buddy</name>"), "Should contain name element")
        assertTrue(xml.contains("<id>1</id>"), "Should contain id element")
        assertTrue(xml.contains("</root>"), "Should contain closing </root>")
    }

    @Test
    fun `xmlify should serialise list as XML with items and item elements`() {
        val data = listOf(
            mapOf("id" to 1, "name" to "Buddy"),
            mapOf("id" to 2, "name" to "Max")
        )
        val xml = XmlMapUtil.xmlify(data)

        assertTrue(xml.contains("<items>"), "Should contain <items> wrapper")
        assertTrue(xml.contains("<item>"), "Should contain <item> elements")
        assertTrue(xml.contains("<id>1</id>"), "Should contain first id")
        assertTrue(xml.contains("<name>Buddy</name>"), "Should contain first name")
        assertTrue(xml.contains("<id>2</id>"), "Should contain second id")
        assertTrue(xml.contains("<name>Max</name>"), "Should contain second name")
        assertTrue(xml.contains("</items>"), "Should contain closing </items>")
    }

    @Test
    fun `xmlify should serialise nested map`() {
        val data = mapOf(
            "name" to "Buddy",
            "details" to mapOf("breed" to "Collie", "age" to 5)
        )
        val xml = XmlMapUtil.xmlify(data)

        assertTrue(xml.contains("<root>"), "Should contain <root>")
        assertTrue(xml.contains("<name>Buddy</name>"), "Should contain name")
        assertTrue(xml.contains("<details>"), "Should contain nested element")
        assertTrue(xml.contains("<breed>Collie</breed>"), "Should contain nested breed")
        assertTrue(xml.contains("<age>5</age>"), "Should contain nested age")
    }

    @Test
    fun `xmlify should exclude null values`() {
        val data = mapOf("name" to "Buddy", "breed" to null)
        val xml = XmlMapUtil.xmlify(data)

        assertTrue(xml.contains("<name>Buddy</name>"), "Should contain name")
        assertFalse(xml.contains("<breed"), "Should not contain null breed element")
    }

    @Test
    fun `xmlify should serialise empty map`() {
        val data = emptyMap<String, Any>()
        val xml = XmlMapUtil.xmlify(data)

        assertTrue(xml.contains("<root"), "Should contain root element")
    }

    @Test
    fun `xmlify should serialise empty list`() {
        val data = emptyList<Any>()
        val xml = XmlMapUtil.xmlify(data)

        assertTrue(xml.contains("<items"), "Should contain items wrapper")
    }

    @Test
    fun `xmlify should serialise single-element list`() {
        val data = listOf(mapOf("id" to 42))
        val xml = XmlMapUtil.xmlify(data)

        assertTrue(xml.contains("<items>"), "Should contain <items> wrapper")
        assertTrue(xml.contains("<item>"), "Should contain <item> element")
        assertTrue(xml.contains("<id>42</id>"), "Should contain id")
    }

    @Test
    fun `xmlify should produce pretty-printed output`() {
        val data = mapOf("key" to "value")
        val xml = XmlMapUtil.xmlify(data)

        assertTrue(xml.contains("\n"), "Should contain newlines for pretty printing")
    }

    @Test
    fun `xmlify should handle various value types`() {
        val data = mapOf(
            "string" to "hello",
            "integer" to 42,
            "decimal" to 3.14,
            "boolean" to true
        )
        val xml = XmlMapUtil.xmlify(data)

        assertTrue(xml.contains("<string>hello</string>"), "Should serialise string")
        assertTrue(xml.contains("<integer>42</integer>"), "Should serialise integer")
        assertTrue(xml.contains("<decimal>3.14</decimal>"), "Should serialise decimal")
        assertTrue(xml.contains("<boolean>true</boolean>"), "Should serialise boolean")
    }

    @Test
    fun `XML_MAPPER should be accessible`() {
        assertNotNull(XmlMapUtil.XML_MAPPER, "XML_MAPPER should be non-null")
    }

    @Test
    fun `xmlify should use custom root name for map`() {
        val data = mapOf("name" to "Buddy", "id" to 1)
        val xml = XmlMapUtil.xmlify(data, rootName = "Pet")

        assertTrue(xml.contains("<Pet>"), "Should use custom root element name")
        assertTrue(xml.contains("<name>Buddy</name>"), "Should contain name element")
        assertTrue(xml.contains("</Pet>"), "Should contain closing custom root")
        assertFalse(xml.contains("<root>"), "Should not contain default root")
    }

    @Test
    fun `xmlify should use custom names for list`() {
        val data = listOf(
            mapOf("id" to 1, "name" to "Buddy"),
            mapOf("id" to 2, "name" to "Max")
        )
        val xml = XmlMapUtil.xmlify(data, rootName = "Pets", itemName = "Pet")

        assertTrue(xml.contains("<Pets>"), "Should use custom wrapper name")
        assertTrue(xml.contains("<Pet>"), "Should use custom item name")
        assertTrue(xml.contains("<name>Buddy</name>"), "Should contain first name")
        assertTrue(xml.contains("<name>Max</name>"), "Should contain second name")
        assertTrue(xml.contains("</Pets>"), "Should contain closing custom wrapper")
        assertFalse(xml.contains("<items>"), "Should not contain default wrapper")
        assertFalse(xml.contains("<item>"), "Should not contain default item name")
    }

    @Test
    fun `xmlify should use custom root name only for list`() {
        val data = listOf(mapOf("id" to 1))
        val xml = XmlMapUtil.xmlify(data, rootName = "Users")

        assertTrue(xml.contains("<Users>"), "Should use custom wrapper name")
        assertTrue(xml.contains("<item>"), "Should use default item name when itemName not specified")
        assertTrue(xml.contains("</Users>"), "Should contain closing custom wrapper")
    }

    @Test
    fun `xmlify should use custom item name only for list`() {
        val data = listOf(mapOf("id" to 1))
        val xml = XmlMapUtil.xmlify(data, itemName = "User")

        assertTrue(xml.contains("<items>"), "Should use default wrapper when rootName not specified")
        assertTrue(xml.contains("<User>"), "Should use custom item name")
        assertTrue(xml.contains("</items>"), "Should contain default closing wrapper")
    }

    @Test
    fun `xmlify should ignore xml names for null input`() {
        assertEquals("", XmlMapUtil.xmlify(null, rootName = "Pet", itemName = "Item"))
    }
}
