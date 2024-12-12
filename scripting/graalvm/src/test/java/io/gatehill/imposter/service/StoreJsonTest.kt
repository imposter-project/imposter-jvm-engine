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
package io.gatehill.imposter.service

import com.google.inject.AbstractModule
import io.gatehill.imposter.config.util.EnvVars
import io.gatehill.imposter.plugin.config.resource.BasicResourceConfig
import io.gatehill.imposter.scripting.AbstractBaseScriptTest
import io.gatehill.imposter.scripting.graalvm.proxy.ObjectProxyingStore
import io.gatehill.imposter.scripting.graalvm.service.GraalvmScriptServiceImpl
import io.gatehill.imposter.store.factory.StoreFactory
import io.gatehill.imposter.store.inmem.InMemoryStoreFactoryImpl
import io.gatehill.imposter.store.model.StoreProvider
import io.gatehill.imposter.store.service.StoreService
import io.gatehill.imposter.store.service.StoreServiceImpl
import io.gatehill.imposter.util.asSingleton
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.Test
import javax.inject.Inject

/**
 * Verify that store items can interoperate with JavaScript.
 *
 * @author Pete Cornish
 */
class StoreJsonTest : AbstractBaseScriptTest() {
    @Inject
    private lateinit var service: GraalvmScriptServiceImpl

    @Inject
    private lateinit var storeFactory: StoreFactory

    override fun getService() = service

    override fun getScriptName() = "store_json.js"

    override fun onBeforeInject() {
        EnvVars.populate(GraalvmScriptServiceImpl.ENV_IMPOSTER_GRAAL_STORE_PROXY to "true")
    }

    override val modules = arrayOf(
        TestStoreFactoryModule()
    )

    @Test
    fun `stringify to JSON`() {
        // init the store proxy
        getService().checkEnableStoreProxy()

        val pluginConfig = configureScript()
        val resourceConfig = pluginConfig as BasicResourceConfig

        with(storeFactory) {
            getStoreByName("test", false).save("example", listOf(
                mapOf("name" to "foo"),
                mapOf("name" to "bar"),
            ))
        }

        val scriptBindings = buildScriptBindings(mapOf(
            "stores" to StoreProvider(storeFactory, { ObjectProxyingStore(it) }, "1")
        ))
        val script = resolveScriptFile(pluginConfig, resourceConfig)
        val actual = getService().executeScript(script, scriptBindings)

        val itemHeader = actual.responseHeaders["items"]
        assertThat(itemHeader, equalTo("foo,bar,baz,"))

        assertThat(actual.content, equalTo("""[{"name":"foo"},{"name":"bar"},{"name":"baz"}]"""))
    }

    class TestStoreFactoryModule: AbstractModule() {
        override fun configure() {
            bind(StoreFactory::class.java).to(InMemoryStoreFactoryImpl::class.java).asSingleton()
            bind(StoreService::class.java).to(StoreServiceImpl::class.java).asSingleton()
        }
    }
}
