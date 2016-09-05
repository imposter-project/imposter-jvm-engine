# Imposter: A scriptable, multipurpose mock server [![Build Status](https://travis-ci.org/outofcoffee/imposter.svg?branch=master)](https://travis-ci.org/outofcoffee/imposter)

Reliable, scriptable and extensible mock server for general REST APIs,
[OpenAPI](https://github.com/OAI/OpenAPI-Specification) (aka Swagger) specifications,
Salesforce and HBase APIs.

Scripting support for both [JavaScript](https://www.javascript.com/) or [Groovy/Java](http://www.groovy-lang.org/).

## What's it for?

Use Imposter to:

* run standalone mocks in place of real systems
* turn a Swagger file into a mock API for testing or QA
* quickly set up a temporary API for your mobile/web client teams whilst the real API is being built
* decouple your integration tests from the cloud/various back-end systems and take control of your dependencies

Provide mock responses using static files or customise behaviour using JavaScript or Java/Groovy. Power users can write their own plugins in a JVM language of their choice.

*****
# Documentation

**[Read the documentation here.](http://outofcoffee.viewdocs.io/imposter/)**
*****

# Plugins

Imposter uses plugins to control its behaviour and provide specialised mocks:


* **[REST](docs/rest_plugin.md)** - Mocks RESTful or plain HTTP APIs.
* **[OpenAPI (aka Swagger)](docs/openapi_plugin.md)** - Support for [OpenAPI](https://github.com/OAI/OpenAPI-Specification) (aka Swagger) API specifications.
* **[HBase](docs/hbase_plugin.md)** - Basic HBase mock implementation.
* **[SFDC (Salesforce) plugin](docs/sfdc_plugin.md)** - Basic Salesforce mock implementation.

You can also create your own plugins, using a JVM language of your choice.

# Example

Let's assume your [configuration](docs/configuration.md) is in a folder named `config`.

Docker example:

    docker run -ti -p 8443:8443 \
        -v $(pwd)/config:/opt/imposter/config \
        outofcoffee/imposter-rest

Standalone Java example:

    java -jar distro/build/libs/imposter.jar \
        --plugin com.gatehill.imposter.plugin.rest.RestPluginImpl \
        --configDir ./config

Your mock server is now running!

This example starts a mock server using the simple
[REST plugin](docs/rest_plugin.md), serving responses based on the configuration files
inside the `config` folder. You can hit the URL
[http://localhost:8443/example](http://localhost:8443/example) to see the mock response.

# Recent changes and Roadmap
  
For recent changes see the [Changelog](CHANGELOG.md), or view the [Roadmap](docs/roadmap.md).

# Contributing

Pull requests are welcome.

# Author

Pete Cornish (outofcoffee@gmail.com)
