/*
 * Copyright (c) 2016-2021.
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
package io.gatehill.imposter.store.dynamodb

import io.gatehill.imposter.service.DeferredOperationService
import io.gatehill.imposter.store.core.AbstractStore
import io.gatehill.imposter.store.dynamodb.config.Settings
import io.gatehill.imposter.store.dynamodb.model.ResultWrapper
import io.gatehill.imposter.util.MapUtil
import org.apache.logging.log4j.LogManager
import software.amazon.awssdk.core.SdkBytes
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.dynamodb.model.AttributeValue
import software.amazon.awssdk.services.dynamodb.model.DeleteItemRequest
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest
import software.amazon.awssdk.services.dynamodb.model.QueryRequest
import java.text.NumberFormat
import java.time.LocalDateTime
import java.time.ZoneOffset

/**
 * Store implementation using DynamoDB.
 *
 * @author Pete Cornish
 */
class DynamoDBStore(
    deferredOperationService: DeferredOperationService,
    override val storeName: String,
    private val ddb: DynamoDbClient,
    private val tableName: String,
) : AbstractStore(deferredOperationService) {
    override val typeDescription = "dynamodb"
    override val isEphemeral = false
    private val logger = LogManager.getLogger(DynamoDBStore::class.java)

    init {
        logger.debug("Initialised DynamoDB store: $storeName using table: $tableName")
    }

    override fun saveItem(key: String, value: Any?) {
        logger.trace("Saving item with key: {} to store: {}", key, storeName)
        val valueAttribute = convertToAttributeValue(value)

        val itemData = mutableMapOf(
            "StoreName" to AttributeValue.builder().s(storeName).build(),
            "Key" to AttributeValue.builder().s(key).build(),
            "Value" to valueAttribute
        )
        if (Settings.Ttl.enabled) {
            itemData[Settings.Ttl.attributeName] = AttributeValue.builder().n(
                LocalDateTime.now()
                    .plusSeconds(Settings.Ttl.seconds)
                    .toEpochSecond(ZoneOffset.UTC)
                    .toString()
            ).build()
        }
        ddb.putItem(PutItemRequest.builder().tableName(tableName).item(itemData).build())
    }

    private fun convertToAttributeValue(value: Any?): AttributeValue {
        return when (value) {
            null -> AttributeValue.builder().nul(true).build()
            is String -> AttributeValue.builder().s(value.toString()).build()
            is Number -> AttributeValue.builder().n(value.toString()).build()
            is Boolean -> AttributeValue.builder().bool(value.toString().toBoolean()).build()
            is Map<*, *> -> AttributeValue.builder().m(convertToDynamoMap(value)).build()
            else -> {
                when (Settings.objectSerialisation) {
                    Settings.ObjectSerialisation.BINARY -> {
                        AttributeValue.builder().b(SdkBytes.fromByteArray(MapUtil.JSON_MAPPER.writeValueAsBytes(value))).build()
                    }
                    Settings.ObjectSerialisation.MAP -> {
                        AttributeValue.builder().m(convertToDynamoMap(value)).build()
                    }
                }
            }
        }
    }

    override fun <T> load(key: String): T? {
        logger.trace("Loading item with key: {} from store: {}", key, storeName)
        val result = ddb.getItem(
            GetItemRequest.builder().tableName(tableName).key(
                mapOf(
                    "StoreName" to AttributeValue.builder().s(storeName).build(),
                    "Key" to AttributeValue.builder().s(key).build()
                )
            ).build()
        )

        return if (result.hasItem()) {
            val (_, value) = destructure<T>(result.item())
            value
        } else {
            null
        }
    }

    override fun delete(key: String) {
        logger.trace("Deleting item with key: {} from store: {}", key, storeName)
        ddb.deleteItem(
            DeleteItemRequest.builder().tableName(tableName).key(
                mapOf(
                    "StoreName" to AttributeValue.builder().s(storeName).build(),
                    "Key" to AttributeValue.builder().s(key).build()
                )
            ).build()
        )
    }

    override fun loadAll(): Map<String, Any?> {
        logger.trace("Loading all items in store: {}", storeName)
        return listAllInStore().items.associate { destructure<Any>(it) }
    }

    override fun loadByKeyPrefix(keyPrefix: String): Map<String, Any?> {
        logger.trace("Loading items in store: $storeName with key prefix: $keyPrefix")

        val query = QueryRequest.builder().tableName(tableName)
            .keyConditionExpression("StoreName = :storeName AND begins_with(#k, :keyPrefix)")
            .expressionAttributeNames(
                mapOf(
                    "#k" to "Key"
                )
            )
            .expressionAttributeValues(
                mapOf(
                    ":storeName" to AttributeValue.builder().s(storeName).build(),
                    ":keyPrefix" to AttributeValue.builder().s(keyPrefix).build(),
                )
            )
            .build()

        val items = ddb.query(query).items()
        logger.trace("{} items found in store: $storeName with key prefix: $keyPrefix", items.size)
        return items.associate { destructure(it) }
    }

    override fun hasItemWithKey(key: String): Boolean {
        logger.trace("Checking for item with key: {} in store: {}", key, storeName)
        return load<Any>(key) != null
    }

    override fun count(): Int {
        val count = listAllInStore().count
        logger.trace("Returning item count {} from store: {}", count, storeName)
        return count
    }

    private fun listAllInStore(): ResultWrapper {
        logger.trace("Listing items in store: {}", storeName)
        return ResultWrapper.usingQuery(ddb, tableName, storeName)
    }

    private fun <T> destructure(attributeItem: Map<String, AttributeValue>): Pair<String, T?> {
        val attributeKey = attributeItem.getValue("Key").s()
        val attributeValue = attributeItem.getValue("Value")
        return attributeKey to convertFromAttributeValue(attributeKey, attributeValue)
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> convertFromAttributeValue(
        attributeKey: String,
        attributeValue: AttributeValue,
    ): T? = when {
        attributeValue.nul() ?: false -> null
        attributeValue.s() != null -> attributeValue.s() as T?
        attributeValue.bool() != null -> attributeValue.bool() as T?
        attributeValue.n() != null -> NumberFormat.getInstance().parse(attributeValue.n()) as T?
        attributeValue.b() != null -> MapUtil.JSON_MAPPER.readValue(attributeValue.b().asByteArray(), Map::class.java) as T?
        attributeValue.hasM() -> convertFromDynamoMap(attributeKey, attributeValue.m()) as T?
        else -> {
            logger.warn("Unable to read value of item: $attributeKey")
            null
        }
    }

    private fun convertToDynamoMap(value: Any?): Map<String, AttributeValue> {
        val mapValue: Map<out Any?, Any?> = when (value) {
            is Map<*, *> -> value
            else -> MapUtil.JSON_MAPPER.convertValue(value, Map::class.java)
        }
        val dynamoMap: Map<String, AttributeValue> = mapValue.entries.associate { (key, value) ->
            key as String to convertToAttributeValue(value)
        }
        return dynamoMap
    }

    private fun convertFromDynamoMap(attributeKey: String, m: Map<String, AttributeValue>): Map<String, Any?> =
        m.entries.associate { (key, value) ->
            key to convertFromAttributeValue<Any>(attributeKey, value)
        }
}
