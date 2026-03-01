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

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.dataformat.xml.XmlMapper

/**
 * XML serialisation utilities for OpenAPI responses.
 */
object XmlMapUtil {
    @JvmField
    val XML_MAPPER: XmlMapper = XmlMapper().apply {
        enable(SerializationFeature.INDENT_OUTPUT)
        setSerializationInclusion(JsonInclude.Include.NON_NULL)
    }

    fun xmlify(
        obj: Any?,
        rootName: String? = null,
        itemName: String? = null
    ): String = obj?.let { serialiseXml(it, rootName, itemName) } ?: ""

    private fun serialiseXml(
        example: Any,
        rootName: String?,
        itemName: String?
    ): String {
        return when (example) {
            is List<*> -> {
                val wrapperName = rootName ?: DEFAULT_LIST_ROOT
                val elementName = itemName ?: DEFAULT_LIST_ITEM
                XML_MAPPER.writer().withRootName(wrapperName)
                    .writeValueAsString(mapOf(elementName to example))
            }
            else -> {
                XML_MAPPER.writer().withRootName(rootName ?: DEFAULT_ROOT)
                    .writeValueAsString(example)
            }
        }
    }

    private const val DEFAULT_ROOT = "root"
    private const val DEFAULT_LIST_ROOT = "items"
    private const val DEFAULT_LIST_ITEM = "item"
}
