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
package io.gatehill.imposter.plugin.openapi

import io.gatehill.imposter.ImposterConfig
import io.gatehill.imposter.http.HttpExchange
import io.gatehill.imposter.http.HttpExchangeFutureHandler
import io.gatehill.imposter.http.HttpMethod
import io.gatehill.imposter.http.HttpRouter
import io.gatehill.imposter.http.SingletonResourceMatcher
import io.gatehill.imposter.http.StatusCodeFactory
import io.gatehill.imposter.plugin.PluginInfo
import io.gatehill.imposter.plugin.RequireModules
import io.gatehill.imposter.plugin.config.ConfiguredPlugin
import io.gatehill.imposter.plugin.config.resource.BasicResourceConfig
import io.gatehill.imposter.plugin.openapi.config.OpenApiPluginConfig
import io.gatehill.imposter.plugin.openapi.config.Settings
import io.gatehill.imposter.plugin.openapi.http.OpenApiResponseBehaviourFactory
import io.gatehill.imposter.plugin.openapi.model.ParsedSpec
import io.gatehill.imposter.plugin.openapi.service.ExampleService
import io.gatehill.imposter.plugin.openapi.service.SpecificationLoaderService
import io.gatehill.imposter.plugin.openapi.service.SpecificationService
import io.gatehill.imposter.script.ResponseBehaviour
import io.gatehill.imposter.server.ServerFactory
import io.gatehill.imposter.service.DefaultBehaviourHandler
import io.gatehill.imposter.service.ResourceService
import io.gatehill.imposter.service.ResponseRoutingService
import io.gatehill.imposter.service.ResponseService
import io.gatehill.imposter.service.ResponseService.ResponseSender
import io.gatehill.imposter.util.HttpUtil
import io.gatehill.imposter.util.LogUtil.describeRequestShort
import io.gatehill.imposter.util.MapUtil.addJavaTimeSupport
import io.gatehill.imposter.util.ResourceUtil
import io.gatehill.imposter.util.ResourceUtil.convertPathFromOpenApi
import io.gatehill.imposter.util.completedUnitFuture
import io.gatehill.imposter.util.makeFuture
import io.swagger.util.Json
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.PathItem
import io.swagger.v3.oas.models.media.Content
import io.swagger.v3.oas.models.responses.ApiResponse
import io.vertx.core.Vertx
import org.apache.logging.log4j.LogManager
import java.util.concurrent.CompletableFuture
import javax.inject.Inject
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.set

/**
 * Plugin for OpenAPI (OAI; formerly known as 'Swagger').
 *
 * @author Pete Cornish
 */
@PluginInfo("openapi")
@RequireModules(OpenApiModule::class)
class OpenApiPluginImpl @Inject constructor(
    vertx: Vertx,
    imposterConfig: ImposterConfig,
    private val resourceService: ResourceService,
    private val specificationService: SpecificationService,
    private val exampleService: ExampleService,
    private val responseService: ResponseService,
    private val responseRoutingService: ResponseRoutingService,
    private val openApiResponseBehaviourFactory: OpenApiResponseBehaviourFactory,
    private val serverFactory: ServerFactory,
    private val specificationLoaderService: SpecificationLoaderService,
) : ConfiguredPlugin<OpenApiPluginConfig>(
    vertx, imposterConfig
) {
    override val configClass = OpenApiPluginConfig::class.java
    private lateinit var allSpecs: List<ParsedSpec>

    companion object {
        private val LOGGER = LogManager.getLogger(OpenApiPluginImpl::class.java)
        private const val UI_WEB_ROOT = "swagger-ui"

        /**
         * 'default' is a special case in OpenAPI that does not have a status code.
         */
        private const val DEFAULT_RESPONSE_KEY = "default"

        @JvmStatic
        val specPathPrefix
            get() = Settings.specPathPrefix

        @JvmStatic
        val combinedSpecPath
            get() = "$specPathPrefix/combined.json"

        init {
            addJavaTimeSupport(Json.mapper())
        }
    }

    private val resourceMatcher = SingletonResourceMatcher.instance

    override fun configureRoutes(router: HttpRouter) {
        if (configs.isEmpty()) {
            LOGGER.debug("No OpenAPI configuration files provided - skipping plugin setup")
            return
        }

        parseSpecs(router)
        if (Settings.shouldExposeSpec) {
            exposeSpec(router)
        } else {
            LOGGER.trace("Skipped exposing OpenAPI specification")
        }
    }

    private fun parseSpecs(router: HttpRouter) {
        val parsedSpecs = mutableListOf<ParsedSpec>()

        // specification mock endpoints
        configs.forEach { config: OpenApiPluginConfig ->
            val spec = specificationLoaderService.parseSpecification(config)

            // The *path prefix* includes the plugin configuration root path,
            // but not the OpenAPI server entry 'basePath', as this is
            // required only for the list of server entries added to
            // the combined spec.
            // This, the path prefix, is incorporated into each of the
            // operation paths within the spec.
            val pathPrefix = (config.path ?: "")

            // The *serving prefix* includes the spec's first server entry path.
            // The operation paths in the combined spec document should not
            // include this, as server entries are automatically prefixed by
            // the spec UI.
            //
            // The serving prefix is built from a concatenation of the plugin
            // configuration root path (see path prefix) and, optionally,
            // the path of the first 'server' entry in the spec. This means
            // that other server entries' paths are ignored.
            //
            // If server-agnostic mock paths are required, set the
            // 'stripServerPath=true' configuration option and specify the
            // root path in the configuration.
            val servingPrefix = pathPrefix + if (config.stripServerPath) "" else specificationService.determinePathFromSpec(spec)

            spec.paths.forEach { path: String, pathConfig: PathItem ->
                handlePathOperations(router, config, spec, servingPrefix, path, pathConfig)
            }
            parsedSpecs += ParsedSpec(spec, pathPrefix)
        }

        allSpecs = parsedSpecs
    }

    /**
     * Serve specification and UI.
     */
    private fun exposeSpec(router: HttpRouter) {
        LOGGER.debug("Adding specification UI at: {}{}", imposterConfig.serverUrl, specPathPrefix)
        router.get(combinedSpecPath).handler(
                resourceService.handleRoute(imposterConfig, configs, resourceMatcher) { httpExchange: HttpExchange ->
                    handleCombinedSpec(httpExchange)
                }
        )
        router.getWithRegex("$specPathPrefix$").handler(
                resourceService.handleRouteAndWrap(imposterConfig, configs, resourceMatcher) { httpExchange: HttpExchange ->
                    httpExchange.response
                            .putHeader("Location", "$specPathPrefix/")
                            .setStatusCode(HttpUtil.HTTP_MOVED_PERM)
                            .end()
                }
        )
        router.get("$specPathPrefix/*").handler(serverFactory.createStaticHttpHandler(UI_WEB_ROOT))
        responseService.addNotFoundMessage("""View the <a href="$specPathPrefix/">available OpenAPI resources</a>.""")
    }

    /**
     * Bind a handler to each operation.
     *
     * @param router     the HTTP router
     * @param config     the plugin configuration
     * @param spec       the OpenAPI specification
     * @param pathPrefix
     * @param path       the mock path
     * @param pathConfig the path configuration
     */
    private fun handlePathOperations(
        router: HttpRouter,
        config: OpenApiPluginConfig,
        spec: OpenAPI,
        pathPrefix: String,
        path: String,
        pathConfig: PathItem
    ) {
        pathConfig.readOperationsMap().forEach { (httpMethod: PathItem.HttpMethod, operation: Operation) ->
            val fullPath = buildFullPath(pathPrefix, path)
            LOGGER.debug("Adding mock endpoint: {} -> {}", httpMethod, fullPath)

            // convert an io.swagger.models.HttpMethod to an io.vertx.core.http.HttpMethod
            val method = HttpMethod.valueOf(httpMethod.name)
            router.route(method, fullPath).handler(buildHandler(config, operation, spec))
        }
    }

    /**
     * Construct the full path from the base path and the operation path.
     *
     * @param pathPrefix
     * @param specOperationPath the operation path from the OpenAPI specification
     * @return the full path
     */
    private fun buildFullPath(pathPrefix: String, specOperationPath: String): String {
        val operationPath = convertPathFromOpenApi(specOperationPath)

        return if (pathPrefix.endsWith("/")) {
            if (operationPath!!.startsWith("/")) {
                pathPrefix + operationPath.substring(1)
            } else {
                pathPrefix + operationPath
            }
        } else {
            if (operationPath!!.startsWith("/")) {
                pathPrefix + operationPath
            } else {
                "$pathPrefix/$operationPath"
            }
        }
    }

    /**
     * Returns an OpenAPI specification combining all the given specifications.
     *
     * @param httpExchange the HTTP exchange
     */
    private fun handleCombinedSpec(httpExchange: HttpExchange): CompletableFuture<Unit> = makeFuture {
        try {
            httpExchange.response
                .putHeader(HttpUtil.CONTENT_TYPE, HttpUtil.CONTENT_TYPE_JSON)
                .end(specificationService.getCombinedSpecSerialised(allSpecs))
        } catch (e: Exception) {
            httpExchange.fail(e)
        }
    }

    /**
     * Build a handler for the given operation.
     *
     * @param pluginConfig the plugin configuration
     * @param operation    the specification operation
     * @param spec         the OpenAPI specification
     * @return a route handler
     */
    private fun buildHandler(
        pluginConfig: OpenApiPluginConfig,
        operation: Operation,
        spec: OpenAPI
    ): HttpExchangeFutureHandler {
        // statically calculate as much as possible
        val statusCodeFactory = buildStatusCodeCalculator(operation)
        return resourceService.handleRoute(imposterConfig, pluginConfig, resourceMatcher) { httpExchange: HttpExchange ->
            LOGGER.trace("Operation ${operation.operationId} matched for request: ${describeRequestShort(httpExchange)}")

            if (!specificationService.isValidRequest(pluginConfig, httpExchange, allSpecs)) {
                return@handleRoute completedUnitFuture()
            }

            val context = mutableMapOf<String, Any>()
            context["operation"] = operation

            val resourceConfig = httpExchange.get<BasicResourceConfig>(ResourceUtil.RESOURCE_CONFIG_KEY)

            val defaultBehaviourHandler: DefaultBehaviourHandler = { responseBehaviour: ResponseBehaviour ->
                // set status code regardless of response strategy
                val response = httpExchange.response.setStatusCode(responseBehaviour.statusCode)

                findApiResponse(operation, responseBehaviour.statusCode)?.let { specResponse ->
                    if (!responseBehaviour.responseHeaders.containsKey(HttpUtil.CONTENT_TYPE)) {
                        setContentTypeFromSpec(httpExchange, responseBehaviour, specResponse)
                    }

                    // build a response from the specification
                    val exampleSender =
                        ResponseSender { httpExchange: HttpExchange, responseBehaviour: ResponseBehaviour ->
                            exampleService.serveExample(
                                imposterConfig,
                                pluginConfig,
                                httpExchange,
                                responseBehaviour,
                                specResponse,
                                spec
                            )
                        }

                    // attempt to serve an example from the specification, falling back if not present
                    return@let responseService.sendResponse(
                        pluginConfig,
                        resourceConfig,
                        httpExchange,
                        responseBehaviour,
                        exampleSender,
                        this::fallback
                    )
                } ?: run {
                    LOGGER.warn(
                        "No response found in specification for {} and status code {}",
                        describeRequestShort(httpExchange),
                        responseBehaviour.statusCode
                    )
                    makeFuture { response.end() }
                }
            }

            responseRoutingService.route(
                pluginConfig,
                resourceConfig,
                httpExchange,
                context,
                statusCodeFactory,
                openApiResponseBehaviourFactory,
                defaultBehaviourHandler
            )
        }
    }

    private fun setContentTypeFromSpec(
        httpExchange: HttpExchange,
        responseBehaviour: ResponseBehaviour,
        optionalResponse: ApiResponse
    ) {
        optionalResponse.content?.let { responseContent: Content ->
            responseContent.keys.firstOrNull()?.let { firstContentType ->
                when (responseContent.size) {
                    0 -> return
                    1 -> LOGGER.debug(
                        "Setting content type [{}] from specification for {}",
                        firstContentType,
                        describeRequestShort(httpExchange)
                    )
                    else -> LOGGER.warn(
                        "Multiple content types in specification - selecting first [{}] for {}",
                        firstContentType,
                        describeRequestShort(httpExchange)
                    )
                }
                responseBehaviour.responseHeaders[HttpUtil.CONTENT_TYPE] = firstContentType
            }
        }
    }

    private fun buildStatusCodeCalculator(operation: Operation) = StatusCodeFactory { rc: BasicResourceConfig ->
        rc.responseConfig.statusCode
        // Fall back to the first non-default response for this operation.
        // Note: responses are keyed on their status code and "default" is a
        // special case in OpenAPI that does not have a status code.
            ?: operation.responses?.keys?.firstOrNull { !DEFAULT_RESPONSE_KEY.equals(it, ignoreCase = true) }?.toInt()
            ?: HttpUtil.HTTP_OK
    }

    /**
     * @return an API response for the given [statusCode], or `null`
     */
    private fun findApiResponse(operation: Operation, statusCode: Int): ApiResponse? {
        // openapi statuses are represented as strings
        val status = statusCode.toString()

        // look for a specification response based on the status code
        // allow for absent 'responses' block
        val optionalResponse = operation.responses?.filter { it.key == status }?.values?.firstOrNull()

        return optionalResponse ?: run {
            // fall back to default (might also be null)
            LOGGER.debug(
                "No response found for status code {}; falling back to default response if present",
                statusCode
            )
            // allow for absent 'responses' block
            operation.responses?.default
        }
    }

    /**
     * Handles the scenario when no example is found.
     *
     * @param httpExchange    the HTTP exchange
     * @param responseBehaviour the response behaviour
     */
    private fun fallback(httpExchange: HttpExchange, responseBehaviour: ResponseBehaviour): Boolean {
        LOGGER.warn(
            "No example match found and no response file set for mock response for {} with status code {}" +
                    " - sending empty response", describeRequestShort(httpExchange), responseBehaviour.statusCode
        )
        httpExchange.response.end()
        return true
    }
}