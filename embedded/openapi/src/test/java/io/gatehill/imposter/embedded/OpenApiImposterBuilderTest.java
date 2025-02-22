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

package io.gatehill.imposter.embedded;

import io.gatehill.imposter.openapi.embedded.OpenApiImposterBuilder;
import io.gatehill.imposter.plugin.openapi.OpenApiPluginImpl;
import io.gatehill.imposter.util.HttpUtil;
import io.restassured.RestAssured;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

/**
 * @author Pete Cornish
 */
public class OpenApiImposterBuilderTest {
    @Test
    public void testFullConfig() throws Exception {
        final Path configDir = Paths.get(OpenApiImposterBuilderTest.class.getResource("/config").toURI());

        final MockEngine imposter = new OpenApiImposterBuilder<>()
                .withPluginClass(OpenApiPluginImpl.class)
                .withConfigurationDir(configDir)
                .startBlocking();

        invokeMockEndpoint(imposter);
    }

    @Test
    public void testStandaloneSpec() throws Exception {
        final Path specFile = Paths.get(OpenApiImposterBuilderTest.class.getResource("/config/petstore-simple.yaml").toURI());

        final MockEngine imposter = new OpenApiImposterBuilder<>()
                .withSpecificationFile(specFile)
                .startBlocking();

        invokeMockEndpoint(imposter);
    }

    private void invokeMockEndpoint(MockEngine imposter) {
        RestAssured.baseURI = String.valueOf(imposter.getBaseUrl());
        given().when()
                .log().ifValidationFails()
                .get("/v1/pets")
                .then()
                .log().ifValidationFails()
                .statusCode(equalTo(HttpUtil.HTTP_OK));
    }
}
