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

package io.gatehill.imposter.scripting.graalvm.proxy

import org.graalvm.polyglot.Value
import org.graalvm.polyglot.proxy.ProxyArray
import org.graalvm.polyglot.proxy.ProxyObject

/**
 * Wraps a map to intercept access. All retrieved elements are proxied.
 */
class InterceptingMap(private val src: Map<*, *>) : ProxyObject {
    override fun getMember(key: String?): Any? {
        val value = src[key]
        return value?.let(DeepProxy::of)
    }

    override fun getMemberKeys(): Any {
        return ProxyArray.fromList(src.keys.toList())
    }

    override fun hasMember(key: String?): Boolean {
        return key?.let { src.containsKey(key) } ?: false
    }

    override fun putMember(key: String, value: Value?) {
        check(src is MutableMap)
        value?.also {
            @Suppress("UNCHECKED_CAST")
            (src as MutableMap<Any?, Any?>)[key] = if (value.isHostObject) value.asHostObject() else value
        }
    }

    override fun removeMember(key: String?): Boolean {
        check(src is MutableMap)
        if (src.containsKey(key)) {
            @Suppress("UNCHECKED_CAST")
            (src as MutableMap<Any?, Any?>).remove<Any?, Any?>(key)
            return true
        } else {
            return false
        }
    }
}
