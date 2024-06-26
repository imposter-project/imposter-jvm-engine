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

package io.gatehill.imposter.plugin.soap.parser

import io.gatehill.imposter.plugin.soap.model.WsdlService
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.emptyString
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.notNullValue
import org.jdom2.Document
import org.jdom2.Element
import org.jdom2.Namespace
import org.junit.Test
import org.mockito.Mockito.mock
import org.xml.sax.EntityResolver
import java.io.File
import javax.xml.namespace.QName

/**
 * Tests for [AbstractWsdlParser].
 *
 * @author Pete Cornish
 */
class AbstractWsdlParserTest {
    @Test
    fun `test getAttributeValueAsQName with unqualified attribute and no target namespace`() {
        val parser = TestWsdlParser(File("."), Document(), mock(), targetNamespace = null)
        val element = Element("pet", "pets", "urn:com:example:petstore")
        element.setAttribute("noNsAttribute", "foo")

        val value = parser.getAttributeValueAsQName(element, "noNsAttribute")
        assertThat(value, notNullValue())
        assertThat(value?.prefix, emptyString())
        assertThat(value?.namespaceURI, emptyString())
        assertThat(value?.localPart, equalTo("foo"))
    }

    @Test
    fun `test getAttributeValueAsQName with qualified attribute and no target namespace`() {
        val parser = TestWsdlParser(File("."), Document(), mock(), targetNamespace = null)
        val element = Element("pet", "pets", "urn:com:example:petstore")
        element.setAttribute("noNsAttribute", "pets:foo")

        val value = parser.getAttributeValueAsQName(element, "noNsAttribute")
        assertThat(value, notNullValue())
        assertThat(value?.prefix, equalTo("pets"))
        assertThat(value?.namespaceURI, equalTo("urn:com:example:petstore"))
        assertThat(value?.localPart, equalTo("foo"))
    }

    @Test
    fun `test getAttributeValueAsQName with unqualified attribute and a target namespace`() {
        val parser = TestWsdlParser(File("."), Document(), mock(), targetNamespace = "urn:com:example:petstore")
        val element = Element("pet", "pets", "urn:com:example:petstore")
        element.setAttribute("noNsAttribute", "foo")

        val value = parser.getAttributeValueAsQName(element, "noNsAttribute")
        assertThat(value, notNullValue())
        assertThat(value?.prefix, emptyString())
        assertThat(value?.namespaceURI, equalTo("urn:com:example:petstore"))
        assertThat(value?.localPart, equalTo("foo"))
    }

    @Test
    fun `test getAttributeValueAsQName with qualified attribute and a target namespace`() {
        val parser = TestWsdlParser(File("."), Document(), mock(), targetNamespace = "urn:com:example:petstore")
        val element = Element("pet", "pets", "urn:com:example:petstore")
        element.setAttribute("noNsAttribute", "pets:foo")

        val value = parser.getAttributeValueAsQName(element, "noNsAttribute")
        assertThat(value, notNullValue())
        assertThat(value?.prefix, equalTo("pets"))
        assertThat(value?.namespaceURI, equalTo("urn:com:example:petstore"))
        assertThat(value?.localPart, equalTo("foo"))
    }

    private class TestWsdlParser(
        wsdlFile: File,
        document: Document,
        entityResolver: EntityResolver,
        private val targetNamespace: String?,
    ) : AbstractWsdlParser(
        wsdlFile, document, entityResolver
    ) {
        override fun resolveTargetNamespace() = targetNamespace

        public override fun getAttributeValueAsQName(element: Element, attributeName: String): QName? {
            return super.getAttributeValueAsQName(element, attributeName)
        }

        override fun findEmbeddedTypesSchemaNodes() = TODO("Not yet implemented")
        override val xPathNamespaces: List<Namespace>
            get() = TODO("Not yet implemented")
        override val version: WsdlParser.WsdlVersion
            get() = TODO("Not yet implemented")

        override val services: List<WsdlService>
            get() = TODO("Not yet implemented")

        override fun getBinding(bindingName: String) = TODO("Not yet implemented")

        override fun getInterface(interfaceName: String) = TODO("Not yet implemented")
    }
}
