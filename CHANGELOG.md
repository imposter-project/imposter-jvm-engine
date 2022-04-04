# Change Log

All notable changes to this project will be documented in this file.
This project adheres to [Semantic Versioning](http://semver.org/).

## [Unreleased]
### Added
- ...

## [2.12.0] - 2022-04-04
### Added
- build: builds both arm64 and amd64 Docker images.
- fix: strips surrounding quotes from when determining plugin class.

### Changed
- chore: bumps JRE to 8u322.
- chore: bumps log4j2 to 2.17.2.
- chore: bumps vert.x to 3.9.13.
- chore: bumps CLI to 0.12.6.
- chore: bumps jackson-databind to 2.13.2.2.
- chore: bumps JUnit to 2.13.1.
- docs: bumps Imposter version in JUnit sample.

## [2.11.1] - 2022-03-22
### Fixed
- fix: rewrites spec paths with base path in OpenAPI plugin.
- refactor: replaces Vert.x executeBlocking with coroutines at boot. This should reduce the logging noise on slow startup.

### Changed
- chore: bumps CLI to 0.12.3.
- chore: bumps Jackson to 2.13.2.
- docs: switches CLI plugin install guidance to use 'save default' option.
- docs: improves formatting of .imposterignore example.

## [2.11.0] - 2022-03-21
### Added
- feat: improves dynamic Groovy script loading.
- feat: adds graphql query support for stores.
- feat: adds support for '.imposterignore' file when scanning for config files.
- docs: adds future JVM version change to roadmap.
- docs: adds details about using path to set a prefix in the OpenAPI mock.

## [2.10.0] - 2022-03-12
### Added
- feat: allows store queries by key prefix.
- feat: allows use of query operation in place of scan in DynamoDB store driver.
- feat: adds support for JSON parsing in Groovy scripts.

### Changed
- refactor: simplifies plugin discovery and provider logic.

## [2.9.1] - 2022-03-07
### Added
- feat: allows JsonPath queries in expressions.

## [2.9.0] - 2022-02-27
### Added
- feat: allows pathless SOAP operation matches.
- feat: adds support for XPath request body capture.
- feat: adds XPath resource matching.
- feat: adds support for multiple resource configurations with the same path/method in REST plugin.

### Changed
- refactor: moves initialisation logic from verticle to builder and engine.
- refactor: optimises JsonPath and XPath queries.

### Fixed
- fix: improves resource matching with multiple rules for the same path.

## [2.8.3] - 2022-02-25
### Fixed
- fix: caches request and response wrappers in Vert.x adapter.
- fix: moves deferred capture and store cleanup to after response transmission to support delayed responses.

## [2.8.2] - 2022-02-22
### Added
- feat: adds DynamoDB and Redis store drivers to 'all' distribution.

### Fixed
- fix: improves handling of nonexistent response files.
- fix: supports session token in DynamoDB store driver.

## [2.8.1] - 2022-02-07
### Fixed
- fix: uses dedicated XSD parser for WSDL schemas.

## [2.8.0] - 2022-02-07
### Added
- feat: adds SOAP mocks plugin. This is available as a plugin and in the 'all' distro. See the user documentation for plugin reference and examples.
- feat: allows resource matching without specifying an HTTP method.
- feat: allows capture of store items to be deferred to after request processing has completed.
- feat: adds support for capture of response headers and body.
- docs: improves expression description and examples.

### Changed
- feat: response file cache now applies to all files, not just templates.
- docs: moves examples to top level directory.
- chore: bumps sample serverless dependency to 2.72.2.

## [2.7.3] - 2022-01-31
### Added
- feat: allows store name to be set using a capture config.
- feat: allows use of expressions for store name.
- feat: allows constant values for key name.

## [2.7.2] - 2022-01-26
### Changed
- refactor: moves mock implementations to dedicated subdirectory.
- refactor: replaces HBase and SFDC distributions with plugins.
- chore: bumps CLI to 0.10.1.

## [2.7.1] - 2022-01-26
### Added
- feat: adds map data type support to DynamoDB store.

### Changed
- refactor: switches Lambda adapter to static plugin discovery to improve boot time.
- refactor: short-circuits in memory store creation if unmodified.
- refactor: defers in memory store instantiation until first use.

### Fixed
- fix: non-existent stores no longer throw an exception.

## [2.7.0] - 2022-01-20
### Added
- feat: adds expressions for capture keys and values.

### Changed
- refactor: splits embedded distro into submodules.
- refactor: adds Java version guard to standalone script service.

### Fixed
- fix: improves accuracy of Nashorn error line numbers.
- fix: improves error trapping in Vert.x Web error handler.

## [2.6.4] - 2022-01-17
### Added
- feat: allows envfile resolution to be disabled.

## [2.6.3] - 2022-01-17
### Added
- feat: adds envfile resolution relative to configuration files.

## [2.6.2] - 2022-01-17
### Added
- feat: allows OpenAPI path prefix to be configured.
- feat: adds support for envfiles.

## [2.6.1] - 2022-01-17
### Fixed
- fix: stops S3 config downloader from attempting to download directories.
- chore: bumps sample project dependency 'follow-redirects' to 1.14.7.

## [2.6.0] - 2022-01-14
### Added
- feat: supports recursive configuration file discovery.
- feat: adds option to cache remote OpenAPI specifications on local filesystem.

### Fixed
- fix: ensures Redis and DynamoDB stores always indicate they exist.
- ci: removes Graal plugin release asset.

## [2.5.4] - 2022-01-12
### Changed
- chore: bumps CLI to 0.9.4.
- chore: bumps vert.x to 3.9.12.

## [2.5.3] - 2022-01-11
### Fixed
- refactor: uses Nashorn APIs to instantiate script engine to disambiguate implementation.

## [2.5.2] - 2022-01-11
### Added
- feat: adds standalone Nashorn JavaScript implementation.
- feat: allows Graal.js to be used as JavaScript implementation using plugin system.
- feat: automatically detects store driver plugins on the classpath.
- build: adds Java 11 build.

### Changed
- refactor: removes support for legacy plugin package.
- chore: bumps CLI to 0.9.1.

## [2.5.1] - 2022-01-10
### Added
- feat: allows plugin classloader strategy to be configured.

## [2.5.0] - 2022-01-09
### Added
- feat: improves supported data types for DynamoDB store.
- feat: adds support for TTL in DynamoDB store.
- feat: denies recursive templating of untrusted response data by default.
- feat: increases speed of Groovy scripts by two orders of magnitude.
- feat: functionally aligns Graal with Nashorn for JavaScript engine.
- feat: introduces 'core' distribution supporting OpenAPI and REST plugins.

### Changed
- build: replaces server-bundled store implementations with standalone plugins.
- refactor: routes all plugin-like classloading through dedicated ClassLoader.
- chore: bumps CLI to 0.8.2.

## [2.4.14] - 2022-01-08
### Changed
- feat: switches Lambda adapter logging to AWS Lambda appender.
- feat: improves file not found logging.

### Fixed
- fix: skips OpenAPI plugin setup if no compatible configuration files are provided.

## [2.4.13] - 2022-01-05
### Changed
- feat: sets explicit uid/gid for container user.

## [2.4.12] - 2021-12-29
### Changed
- chore: bumps log4j2 to 2.17.1.

## [2.4.11] - 2021-12-28
### Changed
- refactor: removes log4j S3 appender from lambda adapter.

## [2.4.10] - 2021-12-27
### Changed
- chore: bumps Groovy to 2.5.15.
- chore: bumps Guice to 4.2.3.
- refactor: removes unneeded Gson dependency from Lambda adapter.

## [2.4.9] - 2021-12-27
### Changed
- chore: bumps okhttp3 to 4.9.3.

## [2.4.8] - 2021-12-22
### Changed
- build: uses managed dependency versions in lambda adapter.

## [2.4.7] - 2021-12-21
### Changed
- feat: uses application specific user in Docker image.
- feat: hardens container image to remove alpine distro bin.
- chore: bumps CLI to 0.8.0.

## [2.4.6] - 2021-12-20
### Fixed
- fix: bumps log4j2 to 2.17.0 to mitigate CVE-2021-45105.

## [2.4.5] - 2021-12-17
### Changed
- refactor: switches boot to direct invocation of unpacked wrapper script.

## [2.4.4] - 2021-12-17
### Changed
- chore: bumps CLI version to 0.7.7.

### Fixed
- fix: resolves placeholders when preparsing configuration files.

## [2.4.3] - 2021-12-17
### Fixed
- fix: version should be reported by status endpoint.

## [2.4.2] - 2021-12-17
### Added
- feat: prints summary on underlying adapter 404 responses.

## [2.4.1] - 2021-12-16
### Added
- feat: allows individual capture configuration to be enabled/disabled.

## [2.4.0] - 2021-12-16
### Added
- feat: adds structured logging with durations, request and response properties.

## [2.3.2] - 2021-12-16
### Fixed
- fix: bumps log4j2 to 2.16.0 to mitigate CVE-2021-45046.

## [2.3.1] - 2021-12-15
### Changed
- feat: switches boot to CLI unpacked method.
- build: improves JAR naming for non-distribution archives.

## [2.3.0] - 2021-12-13
### Added
- feat: adds DynamoDB store implementation.
- feat: adds DynamoDB store support to Lambda distro.
- feat: implements store-wide deletion in Redis and DynamoDB stores.

### Fixed
- fix: uses host header if available for Lambda adapter base URL.
- fix: captures Lambda path parameters explicitly instead of relying on request event.

## [2.2.3] - 2021-12-12
### Fixed
- fix: disable unneeded file upload directory and interception in Vert.x Web. 

## [2.2.2] - 2021-12-12
### Changed
- chore: bumps JRE base image to 8u312-b07.

## [2.2.1] - 2021-12-11
### Fixed
- fix: bumps log4j2 to 2.15.0 to mitigate CVE-2021-44228.

## [2.2.0] - 2021-12-02
### Added
- feat: adds multiple plugin support to embedded distro.
- feat: allows advanced customisation of engine settings in embedded distro.
- feat: adds Lambda adapter and distro.
- docs: adds Lambda example.

### Changed
- refactor: moves vertx-web adapter implementation and dependency into separate module.
- refactor: moves api, config, engine and S3 config resolver modules under core.

### Fixed
- fix: correctly serialises security schemes in OpenAPI plugin.
- fix: resolves some compiler warnings.

## [2.1.0] - 2021-11-23
### Added
- feat: adds a server entry to the combined OpenAPI spec for the mock endpoint.
- feat: adds support for CLI boot. CLI boot is currently experimental, but supports hot-reload of configuration and other [CLI features](https://github.com/gatehill/imposter-cli).

## [2.0.2] - 2021-11-17
### Fixed
- fix: resolves some compiler warnings.
- fix: initialises scripts once even if referenced multiple times.
- fix: restores behaviour of deprecated params property.

## [2.0.1] - 2021-11-15
### Changed
- feat: precompiles JavaScript scripts on startup to lower initial request latency.

### Fixed
- fix: improves error trapping when dynamic item name cannot be resolved.

## [2.0.0] - 2021-11-12
### Changed
- refactor: ports codebase to Kotlin.

### Fixed
- fix: stops path parameters being treated as query parameters during OpenAPI request validation.
- fix: stops path parameters being treated as query parameters during condition and resource matching.

## [1.24.5] - 2021-11-12
### Added
- (docs) Links to example JUnit project.

### Changed
- (build) Bumps Jackson to 2.12.5.

## [1.24.4] - 2021-10-29
### Added
- (docs) Adds JUnit sample project.
- (ci) Publishes base distro Maven artifacts.

### Changed
- (openapi) Logs full URI to OpenAPI specification UI at startup. 
- (core) Quietens logging of unhandled errors for favicons to TRACE. 
- (build) Upgrades Gradle to 6.8.1.
- (build) Increases granularity of module dependency scopes.

## [1.24.3] - 2021-10-28
### Added
- (ci) Publishes embedded distro Maven artifacts.

## [1.24.2] - 2021-10-26
### Added
- (ci) Uploads JAR release asset without version suffix.

## [1.24.1] - 2021-10-26
### Added
- (core) Adds JS type support for stores.

## [1.24.0] - 2021-10-21
### Added
- (core) Allows header keys to be forced to lowercase.

## [1.23.3] - 2021-10-06
### Changed
- (core) Quietens logs for core engine startup.

## [1.23.2] - 2021-10-01
### Added
- (core) Adds support for scripts using `@imposter-js/types`. See https://github.com/gatehill/imposter-js-types

### Changed
- (core) Caches script logger for improved lookup performance.

## [1.23.1] - 2021-09-28
### Changed
- (ci) Switches CI to GitHub Actions.

## [1.23.0] - 2021-09-18
### Added
- (core) Adds support for dynamic capture names.
- (core) Adds support for constant capture values.
- (docs) Adds preloading documentation.

## [1.22.0] - 2021-09-17
### Added
- (all) Applies license.

## [1.21.1] - 2021-09-16
### Added
- (core) Adds embedded JVM distribution. This allows use of Imposter in JUnit tests. See [JVM bindings](./docs/embed_jvm.md).
- (docs) Adds links to [Imposter CLI](./docs/run_imposter_cli.md) and [JavaScript bindings](https://github.com/gatehill/imposter-js).

## [1.21.0] - 2021-09-13
### Added
- (openapi) Allows OpenAPI specification to be loaded from a URL.
- (openapi) Allows OpenAPI specification to be loaded from an S3 bucket.
- (core) Adds support for preloading stores from file or inline data.
- (core) Adds store item count and allows retrieval of object items from store REST API.
- (core) Allows inline data to be used as a response template.

### Changed
- (openapi) Sets response content type from OpenAPI mime type.

## [1.20.0] - 2021-08-31
### Added
- (core) Allows response file cache size to be configured.
- (core) Improves request ID logging in exception handlers.
- (core) Adds metric gauge for script cache.
- (core) Adds metric gauge for response file cache.
- (core) Adds metric timer for script execution duration.

### Changed
- (core) Disables ordered execution, as responses should not block each other. This provides up to a 2x throughput improvement under load.
- (core) Increases default response file cache size to 20.

## [1.19.0] - 2021-08-31
### Added
- (core) Adds support for OpenAPI style path parameter syntax.
- (rest) Adds REST plugin resource support for matching path parameters, query parameters and request headers.

### Changed
- (core) Bumps base JRE image to 8u302.

## [1.18.1] - 2021-08-23
### Added
- (openapi, rest) Allows root response to be used as defaults for resource responses.

### Changed
- (core) Moves request scoped store cleanup to after routing context handler completes.

### Fixed
- (openapi) Fixes root resource being used instead of active resource when sending OpenAPI plugin response.
- (core) Determines content type for response files in the same way regardless of whether template is used.

## [1.18.0] - 2021-08-19
### Added
- (openapi, rest) Allows resource matching against request body using JsonPath. See [request matching](./docs/request_matching.md).
- (docs) Improves documentation for data capture, response templates and advanced request matching.

### Fixed
- (core) Fixes JSON formatting of status response.

## [1.17.1] - 2021-08-16
### Fixed
- (core) Forces header key insensitivity when evaluating security conditions.

## [1.17.0] - 2021-08-11
### Added
- (core) Improves detection of missing configuration files
- (core) Allows store item lookup in response templates.
- (core) Enables capture of path parameters, query parameters and request headers for resources.
- (core) Enables capture of request body properties using JsonPath.
- (core) Enables response template interpolation using JsonPath.
- (core) Adds request scoped store.
- (core) Adds server response header.
- (core) Enables JVM metrics collection.
- (core) Improves trapping and request ID logging for unhandled errors.

### Fixed
- (openapi) Fixes OpenAPI spec path redirect.

## [1.16.0] - 2021-08-09
### Added
- (openapi) Allows resource matching based on request headers.
- (openapi) Adds log-only option for OpenAPI validation.
- (openapi) Allows configuration of default OpenAPI validation issue behaviour.

## [1.15.1] - 2021-07-30
### Added
- (core) Adds normalised headers map to script context request. This aids script portability, avoiding case-sensitivity for header keys.

### Changed
- (core) Bumps base JRE image to 8u292.

## [1.15.0] - 2021-07-21
### Added
- (core) Graduates Stores to GA.
- (docs) Improves metrics examples.

### Changed
- (openapi) Enables full resolution of OpenAPI references.

### Fixed
- (openapi) Supports example references in OpenAPI specifications.

## [1.14.1] - 2021-07-06
### Changed
- (core) Switches in memory store implementation to concurrent map.

## [1.14.0] - 2021-07-05
### Added
- (core) Adds Redis store implementation.
- (core) Adds support for store key prefixes.

### Changed
- (core) Improves script execution time logging and URI logging.

## [1.13.0] - 2021-06-28
### Added
- (core) Adds experimental Stores support.

## [1.12.0] - 2021-06-23
### Added
- (core) Allows configuration driven and script driven latency simulation.
- (core) Adds environment variables to runtime context.

### Changed
- (core) Deprecates confusing DSL method 'immediately()' in favour of a more descriptive name 'skipDefaultBehaviour()'. This is a non-breaking change, but will be removed in a future major release.

## [1.11.0] - 2021-06-22
### Added
- (core) Allows log level to be set using environment variable.
- (core) Exposes Prometheus metrics endpoint.

### Changed
- (core) Bumps Vert.x version to 3.7.1.
- (core) Compiles and caches scripts in the Nashorn script engine, substantially improving execution speed.

## [1.10.1] - 2021-06-11
### Fixed
- (core) Matches security policy headers in a case-insensitive manner.

## [1.10.0] - 2021-06-09
### Added
- (openapi, rest) Adds security policy support to resources.

### Changed
- (openapi) Bumps Swagger UI version to 3.50.0.

## [1.9.0] - 2021-06-07
### Added
- (core) Adds path parameter support to declarative config and script engines.
- (core) Adds query parameter support to security policy condition.

### Changed
- (core) Renames params to queryParams in configuration and script engine.

## [1.8.0] - 2021-06-05
### Added
- (core) Allows request security to be controlled via plugin configuration. This supports authentication using request header values.
- (core) Allows substitution of environment variables in plugin configuration.

## [1.7.0] - 2021-05-25
### Added
- (openapi) Allows matching of response behaviours based on request parameters in static configuration.
- (openapi) Allows specification of example name in static configuration.
- (core) Exposes request path to scripts for easier conditional logic.

## [1.6.2] - 2021-05-07
### Fixed
- (core) Allows request parameters to be accessed using Nashorn and Graal.js script engines.

## [1.6.1] - 2021-05-06
### Fixed
- (openapi) Correctly validates request query parameters.

## [1.6.0] - 2021-05-05
### Added
- (openapi) Allows OpenAPI validation levels to be configured.
- (openapi) Adds format-aware default value generators.
- (docs) Improves OpenAPI plugin documentation.

### Fixed
- (openapi) Fixes example collection for date and date-time schemas (thanks, zellerr).

## [1.5.0] - 2021-05-03
### Added
- (openapi) Allows request validation to be enabled via OpenAPI plugin configuration.

### Changed
- (openapi) OpenAPI plugin returns first status code for operation if none set explicitly.
- (openapi) Returns the first value of an enum when building an example from an OpenAPI schema.

## [1.4.0] - 2021-04-29
### Added
- (openapi) Enables default status codes to be defined for OpenAPI paths.
- (openapi) Adds path specific response behaviours to OpenAPI plugin.

## [1.3.0] - 2021-04-20
### Added
- (openapi) Enables response examples to be selected by name.
- (core) Adds GraalVM script engine as Nashorn is deprecated from Java 11. Nashorn is still the default.
- (docs) Improves scripting documentation.

## [1.2.0] - 2020-04-30
### Added
- (openapi) Adds support for response refs.
- (openapi) Enables schema model ref lookup by default.
- (openapi) Improves array example serialisation.
- (openapi) Improves test coverage for object and schema examples, including YAML serialisations.

## [1.1.2] - 2020-02-22
### Added
- (openapi) Adds experimental model example generator.
- (openapi) Improves base path handling to remove double slashes at the start of full paths.

## [1.1.1] - 2019-11-25
### Added
- (openapi, rest) Allows static responses to specify headers and inline data.
- (rest) Allows HTTP method to be set on root REST plugin resource.
- (docs) Improves configuration documentation.

## [1.1.0] - 2019-11-25
### Added
- (core) Enables plugins to be specified by their short name.
- (docs) Improves examples and documentation.

### Changed
- (openapi) Sets OpenAPI base path on server URI instead of path.
- (openapi) Bumps Swagger UI to 3.24.3.
- (rest) Makes configuration enum deserialisation case insensitive.
- (rest) Sets default REST resource type to object.

## [1.0.1] - 2019-11-17
### Added
- (core) Adds support for YAML-formatted configuration files.
- (rest) Improves REST plugin examples and enables non-string ID comparision.

### Fixed
- (openapi) Improves OpenAPI path parameter detection.
- (openapi) Improves error trapping when overriding OpenAPI scheme.

## [1.0.0] - 2019-10-20
### Added
- (openapi) Adds OpenAPI v3 support. This means the OpenAPI plugin now supports both Swagger/OpenAPI v2 and OpenAPI v3 files.
- (openapi) Adds support for object response examples in the OpenAPI plugin.

### Changed
- (core) Breaking change: Defaults HTTP listen port to 8080.
- (core) Slims distributions to include only the required dependencies.
- (core) Switches Docker base image to Alpine.
- (core) Enables cgroup-aware heap sizing JVM option.

### Fixed
- (core) Automatic plugin detection now works properly.

## [0.7.0] - 2019-08-03
### Added
- (core) Adds extension point to allow custom HTTP server implementation.
- (core) More modules published for extension developers.

### Fixed
- (core) Verticle startup no longer blocks the main thread.
- (hbase) HBase plugin tests now included in test reports.

### Changed
- (core) Moves core API into Maven-published 'api' module.
- (sfdc) Sets SFDC plugin content type to JSON (thanks, pauturner).

## [0.6.0] - 2017-10-14
### Added
- (core) Exposes request headers to scripts (thanks, kareem-habib).
- (core) Adds withData response behaviour (thanks, yanan-l).
- (core) Adds withHeader response behaviour (thanks, benjvoigt).
- (docs) Adds documentation for new response behaviours.
- (core) Prints version in status response.

### Changed
- (core) Switches to asynchronous request handling, which should improve performance under load.
- (openapi) Updates swagger-ui to version 3.0.21 (thanks, benjvoigt).
- (core) Bumps various dependency versions.

## [0.5.0] - 2016-09-03
### Added
- (core) Adds JavaScript scripting support.

### Changed
- (docs) Refactored documentation.

## [0.4.0] - 2016-08-29
### Added
- (docs) Adds changelog (this document!)
- (core) Adds CLI support for loading multiple plugins.
- (core) Adds CLI and core support for specifying multiple config directories.
- (core) Now detects plugins based on provided configuration files. This means you no longer have to specify the plugin class explicitly.
- (core) Adds core support for specifying plugin class via META-INF properties file.

### Changed
- (core) Breaking change: response files are now resolved relative to the plugin configuration file, not the core configuration directory.

## [0.3.4] - 2016-08-22
### Added
- (hbase) Adds RecordInfo and record ID to context for HBase plugin.

## [0.3.3] - 2016-05-08
### Added
- (openapi) Adds API sandbox using swagger-ui for OpenAPI plugin.

## [0.3.2] - 2016-05-04
### Added
- (hbase) Adds HBase content negotiation. Supports JSON and protobuf serialisation/deserialisation.
- (core) Allows plugins to declare additional dependency modules.

## [0.3.1] - 2016-05-02
### Added
- (openapi) Adds option to serve first example found if no exact match is found in OpenAPI plugin.

## [0.3.0] - 2016-05-01
### Added
- (openapi) Adds OpenAPI (aka Swagger) API specification plugin.
- (core) Adds request method to script context for all plugins.
- (hbase) Adds table name to HBase plugin.

## [0.2.5] - 2016-04-26
### Added
- (rest) Adds REST plugin support for ID field name (like HBase plugin).
- (rest) Adds REST plugin subresources to return objects or arrays of data.

## [0.2.4] - 2016-04-25
### Changed
- (core) Using args4j for command line arguments in place of system properties.

## [0.2.3] - 2016-04-21
### Added
- (hbase) Support named ID field in HBase plugin.

## [0.2.2] - 2016-04-20
### Added
- (hbase) Adds the ability to get a single row from an HBase table.

## [0.2.1] - 2016-04-18
### Added
- (hbase, sfdc) Adds script support to other plugins.

## [0.2.0] - 2016-04-17
### Added
- (rest) Adds support for Groovy scripting of REST response behaviour.

## [0.1.0] - 2016-04-16
### Added
- Initial release.
- REST plugin.
- HBase mock plugin.
- SFDC plugin.
