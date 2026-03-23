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
package io.gatehill.imposter.store.dynamodb.support

import io.gatehill.imposter.config.util.EnvVars
import io.gatehill.imposter.store.dynamodb.config.Settings
import org.testcontainers.localstack.LocalStackContainer
import org.testcontainers.utility.DockerImageName
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.dynamodb.model.AttributeDefinition
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement
import software.amazon.awssdk.services.dynamodb.model.KeyType
import software.amazon.awssdk.services.dynamodb.model.ProvisionedThroughput
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType
import java.net.URI

/**
 * Tests for DynamoDB store implementation.
 *
 * @author Pete Cornish
 */
class DynamoDBStoreTestHelper {
    lateinit var ddb: DynamoDbClient

    fun startDynamoDb(additionalEnv: Map<String, String> = emptyMap()): LocalStackContainer {
        val dynamo = LocalStackContainer(DockerImageName.parse("localstack/localstack:3"))
            .withServices("dynamodb")
            .apply { start() }

        val dynamoDbEndpoint = dynamo.endpoint.toString()
        EnvVars.populate(
            mapOf(
                "IMPOSTER_DYNAMODB_ENDPOINT" to dynamoDbEndpoint,
                "AWS_ACCESS_KEY_ID" to "dummy",
                "AWS_SECRET_ACCESS_KEY" to "dummy",
            ) + additionalEnv
        )

        ddb = DynamoDbClient.builder()
            .endpointOverride(URI.create(Settings.dynamoDbApiEndpoint!!))
            .region(Region.of(Settings.dynamoDbRegion))
            .credentialsProvider(
                StaticCredentialsProvider.create(AwsBasicCredentials.create("dummy", "dummy"))
            )
            .build()

        return dynamo
    }

    fun createTable(tableName: String) {
        val keySchema = listOf(
            KeySchemaElement.builder().attributeName("StoreName").keyType(KeyType.HASH).build(),
            KeySchemaElement.builder().attributeName("Key").keyType(KeyType.RANGE).build(),
        )
        val attributeDefs = listOf(
            AttributeDefinition.builder().attributeName("StoreName").attributeType(ScalarAttributeType.S).build(),
            AttributeDefinition.builder().attributeName("Key").attributeType(ScalarAttributeType.S).build(),
        )
        val request = CreateTableRequest.builder()
            .tableName(tableName)
            .keySchema(keySchema)
            .attributeDefinitions(attributeDefs)
            .provisionedThroughput(
                ProvisionedThroughput.builder()
                    .readCapacityUnits(5L)
                    .writeCapacityUnits(6L)
                    .build()
            )
            .build()

        ddb.createTable(request)
    }
}
