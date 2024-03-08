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
package io.gatehill.imposter.server.vertxweb

import com.google.inject.Injector
import io.gatehill.imposter.ImposterConfig
import io.gatehill.imposter.http.HttpExchangeFutureHandler
import io.gatehill.imposter.http.HttpRouter
import io.gatehill.imposter.server.HttpServer
import io.gatehill.imposter.server.ServerFactory
import io.gatehill.imposter.server.vertxweb.impl.VertxHttpExchange
import io.gatehill.imposter.server.vertxweb.impl.VertxHttpServer
import io.gatehill.imposter.server.vertxweb.util.VertxResourceUtil.convertMethodToVertx
import io.gatehill.imposter.util.FileUtil
import io.gatehill.imposter.util.makeFuture
import io.vertx.core.Vertx
import io.vertx.core.http.HttpServerOptions
import io.vertx.core.net.JksOptions
import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.FileSystemAccess
import io.vertx.ext.web.handler.StaticHandler
import io.vertx.ext.web.handler.impl.BodyHandlerImpl
import io.vertx.micrometer.PrometheusScrapingHandler
import org.apache.logging.log4j.LogManager
import java.net.URISyntaxException
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.CompletableFuture

/**
 * @author Pete Cornish
 */
class VertxWebServerFactoryImpl : ServerFactory {
    override fun provide(
        injector: Injector,
        imposterConfig: ImposterConfig,
        vertx: Vertx,
        router: HttpRouter,
    ): CompletableFuture<HttpServer> {
        LOGGER.trace("Starting mock server on {}:{}", imposterConfig.host, imposterConfig.listenPort)
        val serverOptions = HttpServerOptions()

        // configure keystore and enable HTTPS
        if (imposterConfig.isTlsEnabled) {
            LOGGER.trace("TLS is enabled")
            configureTls(imposterConfig, serverOptions)
        } else {
            LOGGER.trace("TLS is disabled")
        }

        val vertxRouter = convertRouterToVertx(router)
        val vertxServer = vertx.createHttpServer(serverOptions).requestHandler(vertxRouter)

        LOGGER.trace("Listening on {}", imposterConfig.serverUrl)
        val serverFuture = CompletableFuture<HttpServer>()
        vertxServer.listen(imposterConfig.listenPort, imposterConfig.host) { listenResult ->
            if (listenResult.succeeded()) {
                serverFuture.complete(VertxHttpServer(vertxServer))
            } else {
                serverFuture.completeExceptionally(
                    RuntimeException(
                        "Failed to listen on ${imposterConfig.host}:${imposterConfig.listenPort}",
                        listenResult.cause()
                    )
                )
            }
        }

        return serverFuture
    }

    private fun configureTls(
        imposterConfig: ImposterConfig,
        serverOptions: HttpServerOptions,
    ) {
        // locate keystore
        val keystorePath: Path = if (imposterConfig.keystorePath!!.startsWith(FileUtil.CLASSPATH_PREFIX)) {
            try {
                val kp = imposterConfig.keystorePath!!.substring(FileUtil.CLASSPATH_PREFIX.length)
                Paths.get(VertxWebServerFactoryImpl::class.java.getResource(kp).toURI())
            } catch (e: URISyntaxException) {
                throw RuntimeException("Error locating keystore", e)
            }
        } else {
            Paths.get(imposterConfig.keystorePath!!)
        }

        val jksOptions = JksOptions()
        jksOptions.path = keystorePath.toString()
        jksOptions.password = imposterConfig.keystorePassword
        serverOptions.keyStoreOptions = jksOptions
        serverOptions.isSsl = true
    }

    private fun convertRouterToVertx(router: HttpRouter) = Router.router(router.vertx).also { vertxRouter ->
        router.routes.forEach { httpRoute ->
            val vertxRoute = httpRoute.regex?.let { regex ->
                httpRoute.method?.let { method -> vertxRouter.routeWithRegex(convertMethodToVertx(method), regex) }
                    ?: vertxRouter.routeWithRegex(regex)

            } ?: httpRoute.path?.let { path ->
                httpRoute.method?.let { method -> vertxRouter.route(convertMethodToVertx(method), path) }
                    ?: vertxRouter.route(path)

            } ?: vertxRouter.route()

            val handler = httpRoute.handler ?: throw IllegalStateException("No route handler set for: $httpRoute")
            vertxRoute.handler { rc -> handler(VertxHttpExchange(router, rc, httpRoute)).get() }
        }

        router.errorHandlers.forEach { (statusCode, errorHandler) ->
            vertxRouter.errorHandler(statusCode) { rc ->
                errorHandler(VertxHttpExchange(router, rc, null))
            }
        }
    }

    override fun createBodyHttpHandler(): HttpExchangeFutureHandler {
        val handler = BodyHandlerImpl(false)
        return { he -> makeFuture { handler.handle((he as VertxHttpExchange).routingContext) } }
    }

    override fun createStaticHttpHandler(root: String, relative: Boolean): HttpExchangeFutureHandler {
        val handlerVisibility = if (relative) FileSystemAccess.RELATIVE else FileSystemAccess.ROOT
        val handler = StaticHandler.create(handlerVisibility, root)
        return { exchange ->
            makeFuture {
                LOGGER.debug("Serving static resource: ${exchange.request.path}")
                handler.handle((exchange as VertxHttpExchange).routingContext)
            }
        }
    }

    override fun createMetricsHandler(): HttpExchangeFutureHandler {
        val handler = PrometheusScrapingHandler.create()
        return { he -> makeFuture { handler.handle((he as VertxHttpExchange).routingContext) } }
    }

    companion object {
        private val LOGGER = LogManager.getLogger(VertxWebServerFactoryImpl::class.java)
    }
}
