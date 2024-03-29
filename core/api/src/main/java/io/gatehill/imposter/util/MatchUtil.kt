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
package io.gatehill.imposter.util

import io.gatehill.imposter.plugin.config.security.ConditionalNameValuePair
import io.gatehill.imposter.plugin.config.security.SecurityMatchOperator
import java.util.*

/**
 * @author Pete Cornish
 */
object MatchUtil {
    /**
     * Checks if two objects match, where either input could be null.
     *
     * @param a object to test, possibly `null`
     * @param b object to test, possibly `null`
     * @return `true` if the objects match, otherwise `false`
     */
    fun safeEquals(a: Any?, b: Any?): Boolean {
        return if (Objects.nonNull(a)) {
            a == b
        } else {
            Objects.isNull(b)
        }
    }

    /**
     * Checks if the actual value matches the given regular expression.
     */
    fun safeRegexMatch(actualValue: String?, expression: String?) =
        expression?.toRegex()?.matches(actualValue ?: "") ?: false

    fun conditionMatches(condition: ConditionalNameValuePair, actual: String?): Boolean {
        val matched: Boolean = when (condition.operator) {
            SecurityMatchOperator.EqualTo -> {
                safeEquals(actual, condition.value)
            }

            SecurityMatchOperator.NotEqualTo -> {
                !safeEquals(actual, condition.value)
            }

            SecurityMatchOperator.Matches -> {
                safeRegexMatch(actual, condition.value)
            }

            SecurityMatchOperator.NotMatches -> {
                !safeRegexMatch(actual, condition.value)
            }
        }
        return matched
    }
}
