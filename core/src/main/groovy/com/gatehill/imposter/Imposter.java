package com.gatehill.imposter;

import com.gatehill.imposter.plugin.Plugin;
import com.gatehill.imposter.plugin.PluginManager;
import com.gatehill.imposter.plugin.PluginProvider;
import com.gatehill.imposter.plugin.RequireModules;
import com.gatehill.imposter.plugin.config.BaseConfig;
import com.gatehill.imposter.plugin.config.ConfigurablePlugin;
import com.gatehill.imposter.util.InjectorUtil;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Injector;
import com.google.inject.Module;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import static com.gatehill.imposter.util.FileUtil.CONFIG_FILE_SUFFIX;
import static com.gatehill.imposter.util.HttpUtil.BIND_ALL_HOSTS;
import static com.gatehill.imposter.util.MapUtil.MAPPER;
import static com.google.common.collect.Lists.newArrayList;
import static java.util.Optional.ofNullable;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class Imposter {
    private static final Logger LOGGER = LogManager.getLogger(Imposter.class);

    @Inject
    private Injector injector;

    @Inject
    private ImposterConfig imposterConfig;

    @Inject
    private PluginManager pluginManager;

    public void start() {
        InjectorUtil.createChildInjector(getModules()).injectMembers(this);

        // server config
        processConfiguration();

        // load and configure plugins
        final Map<String, List<File>> pluginConfigs = loadPluginConfigs(imposterConfig.getConfigDirs());
        instantiatePlugins(imposterConfig.getPluginClassNames(), pluginConfigs);
        configurePlugins(pluginConfigs);
    }

    protected Module[] getModules() {
        return new ImposterModule[]{new ImposterModule()};
    }

    private void processConfiguration() {
        imposterConfig.setServerUrl(buildServerUrl().toString());

        final String[] configDirs = imposterConfig.getConfigDirs();

        // resolve relative config paths
        for (int i = 0; i < configDirs.length; i++) {
            if (configDirs[i].startsWith("./")) {
                configDirs[i] = Paths.get(System.getProperty("user.dir"), configDirs[i].substring(2)).toString();
            }
        }
    }

    private URI buildServerUrl() {
        // might be set explicitly
        final Optional<String> explicitUrl = ofNullable(imposterConfig.getServerUrl());
        if (explicitUrl.isPresent()) {
            return URI.create(explicitUrl.get());
        }

        // build based on configuration
        final String scheme = (imposterConfig.isTlsEnabled() ? "https" : "http") + "://";
        final String host = (BIND_ALL_HOSTS.equals(imposterConfig.getHost()) ? "localhost" : imposterConfig.getHost());

        final String port;
        if ((imposterConfig.isTlsEnabled() && 443 == imposterConfig.getListenPort())
                || (!imposterConfig.isTlsEnabled() && 80 == imposterConfig.getListenPort())) {
            port = "";
        } else {
            port = ":" + String.valueOf(imposterConfig.getListenPort());
        }

        return URI.create(scheme + host + port);
    }

    @SuppressWarnings("unchecked")
    private void instantiatePlugins(String[] plugins, Map<String, List<File>> pluginConfigs) {
        instantiatePluginsFromConfig(plugins, pluginConfigs);

        final int pluginCount = pluginManager.getPlugins().size();
        if (pluginCount > 0) {
            LOGGER.info("Loaded {} plugins", pluginCount);
        } else {
            throw new RuntimeException("No plugins were loaded");
        }
    }

    private void instantiatePluginsFromConfig(String[] pluginClassNames, Map<String, List<File>> pluginConfigs) {
        ofNullable(pluginClassNames).ifPresent(classNames ->
                Arrays.stream(classNames).forEach(this::registerPluginClass));

        pluginManager.getPluginClasses().forEach(this::registerPlugin);

        final List<PluginProvider> newProviders = pluginManager.getPlugins().stream()
                .filter(plugin -> plugin instanceof PluginProvider)
                .map(plugin -> ((PluginProvider) plugin))
                .filter(provider -> !pluginManager.isProviderRegistered(provider.getClass()))
                .collect(Collectors.toList());

        // recurse for any new providers
        newProviders.forEach(provider -> {
            pluginManager.registerProvider(provider.getClass());
            final String[] provided = provider.providePlugins(imposterConfig, pluginConfigs);
            LOGGER.debug("{} plugins provided by {}", provided.length, provider.getClass().getCanonicalName());
            instantiatePluginsFromConfig(provided, pluginConfigs);
        });
    }

    @SuppressWarnings("unchecked")
    private void registerPluginClass(String className) {
        try {
            if (pluginManager.registerClass((Class<? extends Plugin>) Class.forName(className))) {
                LOGGER.debug("Registered plugin {}", className);
            }
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private void registerPlugin(Class<? extends Plugin> pluginClass) {
        final Injector pluginInjector;

        final RequireModules moduleAnnotation = pluginClass.getAnnotation(RequireModules.class);
        if (null != moduleAnnotation && moduleAnnotation.value().length > 0) {
            pluginInjector = injector.createChildInjector(instantiateModules(moduleAnnotation));
        } else {
            pluginInjector = injector;
        }

        pluginManager.registerInstance(pluginInjector.getInstance(pluginClass));
    }

    private List<Module> instantiateModules(RequireModules moduleAnnotation) {
        final List<Module> modules = Lists.newArrayList();

        for (Class<? extends Module> moduleClass : moduleAnnotation.value()) {
            try {
                modules.add(moduleClass.newInstance());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        return modules;
    }

    /**
     * Send config to plugins.
     *
     * @param pluginConfigs configurations keyed by plugin
     */
    private void configurePlugins(Map<String, List<File>> pluginConfigs) {
        pluginManager.getPlugins().stream()
                .filter(plugin -> plugin instanceof ConfigurablePlugin)
                .map(plugin -> (ConfigurablePlugin) plugin)
                .forEach(plugin -> {
                    final List<File> configFiles = ofNullable(pluginConfigs.get(plugin.getClass().getCanonicalName()))
                            .orElse(Collections.emptyList());
                    plugin.loadConfiguration(configFiles);
                });
    }

    private Map<String, List<File>> loadPluginConfigs(String[] configDirs) {
        int configCount = 0;

        // read all config files
        final Map<String, List<File>> allPluginConfigs = Maps.newHashMap();
        for (String configDir : configDirs) {
            try {
                final File[] configFiles = ofNullable(new File(configDir).listFiles((dir, name) -> name.endsWith(CONFIG_FILE_SUFFIX)))
                        .orElse(new File[0]);

                for (File configFile : configFiles) {
                    LOGGER.debug("Loading configuration file: {}", configFile);
                    configCount++;

                    final BaseConfig config = MAPPER.readValue(configFile, BaseConfig.class);
                    config.setParentDir(configFile.getParentFile());

                    List<File> pluginConfigs = allPluginConfigs.get(config.getPluginClass());
                    if (Objects.isNull(pluginConfigs)) {
                        pluginConfigs = newArrayList();
                        allPluginConfigs.put(config.getPluginClass(), pluginConfigs);
                    }

                    pluginConfigs.add(configFile);
                }

            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        LOGGER.info("Loaded {} plugin configuration files from: {}",
                configCount, Arrays.toString(configDirs));

        return allPluginConfigs;
    }
}
