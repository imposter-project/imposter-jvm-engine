/*
 * Copyright (c) 2024-2024.
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

package io.gatehill.imposter.scripting.graalvm.proxy

import io.gatehill.imposter.http.HttpMethod
import io.gatehill.imposter.http.HttpRequest
import org.graalvm.polyglot.proxy.ProxyObject
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.hasItemInArray
import org.hamcrest.Matchers.nullValue
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

/**
 * Tests for the RequestProxy class.
 */
class RequestProxyTest {
    private lateinit var mockRequest: HttpRequest
    private lateinit var requestProxy: RequestProxy

    @BeforeEach
    fun setUp() {
        mockRequest = mock()
        requestProxy = RequestProxy(mockRequest)
    }

    @Test
    fun `getMember returns correct values for each property`() {
        // Setup mock request
        whenever(mockRequest.path).thenReturn("/test/path")
        whenever(mockRequest.method).thenReturn(HttpMethod.GET)
        whenever(mockRequest.absoluteUri).thenReturn("http://localhost:8080/test/path")
        whenever(mockRequest.headers).thenReturn(mapOf("Content-Type" to "application/json"))
        whenever(mockRequest.pathParams).thenReturn(mapOf("id" to "123"))
        whenever(mockRequest.queryParams).thenReturn(mapOf("filter" to "active"))
        whenever(mockRequest.formParams).thenReturn(mapOf("name" to "test"))
        whenever(mockRequest.bodyAsString).thenReturn("{\"key\":\"value\"}")

        // Test each property
        assertThat(requestProxy.getMember("path"), equalTo("/test/path"))
        assertThat(requestProxy.getMember("method"), equalTo("GET"))
        assertThat(requestProxy.getMember("uri"), equalTo("http://localhost:8080/test/path"))

        // For map properties, cast to ProxyObject and use getMember directly
        val headers = requestProxy.getMember("headers") as ProxyObject
        assertThat(headers.getMember("Content-Type"), equalTo("application/json"))

        val pathParams = requestProxy.getMember("pathParams") as ProxyObject
        assertThat(pathParams.getMember("id"), equalTo("123"))

        val queryParams = requestProxy.getMember("queryParams") as ProxyObject
        assertThat(queryParams.getMember("filter"), equalTo("active"))

        val formParams = requestProxy.getMember("formParams") as ProxyObject
        assertThat(formParams.getMember("name"), equalTo("test"))

        val normalisedHeaders = requestProxy.getMember("normalisedHeaders") as ProxyObject
        assertThat(normalisedHeaders.getMember("content-type"), equalTo("application/json"))

        assertThat(requestProxy.getMember("body"), equalTo("{\"key\":\"value\"}"))

        // Test invalid property
        assertThat(requestProxy.getMember("nonExistentProperty"), nullValue())
    }

    @Test
    fun `getMemberKeys returns all expected properties`() {
        val keys = requestProxy.getMemberKeys()

        // Verify all expected properties are in the keys array
        assertThat(keys, hasItemInArray("path"))
        assertThat(keys, hasItemInArray("method"))
        assertThat(keys, hasItemInArray("uri"))
        assertThat(keys, hasItemInArray("headers"))
        assertThat(keys, hasItemInArray("pathParams"))
        assertThat(keys, hasItemInArray("queryParams"))
        assertThat(keys, hasItemInArray("formParams"))
        assertThat(keys, hasItemInArray("body"))
        assertThat(keys, hasItemInArray("normalisedHeaders"))
    }

    @Test
    fun `hasMember correctly identifies valid and invalid properties`() {
        // Test valid properties
        assertThat(requestProxy.hasMember("path"), equalTo(true))
        assertThat(requestProxy.hasMember("method"), equalTo(true))
        assertThat(requestProxy.hasMember("uri"), equalTo(true))
        assertThat(requestProxy.hasMember("headers"), equalTo(true))
        assertThat(requestProxy.hasMember("pathParams"), equalTo(true))
        assertThat(requestProxy.hasMember("queryParams"), equalTo(true))
        assertThat(requestProxy.hasMember("formParams"), equalTo(true))
        assertThat(requestProxy.hasMember("body"), equalTo(true))
        assertThat(requestProxy.hasMember("normalisedHeaders"), equalTo(true))

        // Test invalid properties
        assertThat(requestProxy.hasMember("nonExistentProperty"), equalTo(false))
        assertThat(requestProxy.hasMember(null), equalTo(false))
    }

    @Test
    fun `putMember throws UnsupportedOperationException`() {
        // Value is a final class and cannot be mocked, so we'll use null instead
        // The implementation of putMember doesn't actually use the Value parameter

        // Verify that putMember throws UnsupportedOperationException
        val exception = assertThrows(UnsupportedOperationException::class.java) {
            requestProxy.putMember("path", null)
        }

        // Verify the exception message
        assertThat(exception.message, equalTo("Request cannot be modified"))
    }
}
