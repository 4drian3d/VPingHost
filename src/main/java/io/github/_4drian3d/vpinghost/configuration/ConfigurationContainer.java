package io.github._4drian3d.vpinghost.configuration;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.hocon.HoconConfigurationLoader;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

public final class ConfigurationContainer {
    private final AtomicReference<Configuration> config;
    private final HoconConfigurationLoader loader;
    private final Logger logger;

    private ConfigurationContainer(
            final Configuration config,
            final HoconConfigurationLoader loader,
            final Logger logger
    ) {
        this.config = new AtomicReference<>(config);
        this.loader = loader;
        this.logger = logger;
    }

    public CompletableFuture<Boolean> reload() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                final CommentedConfigurationNode node = loader.load();
                Configuration newConfig = node.get(Configuration.class);
                node.set(Configuration.class, newConfig);
                loader.save(node);
                config.set(newConfig);
                return true;
            } catch (ConfigurateException exception) {
                logger.error("Could not reload configuration file", exception);
                return false;
            }
        });
    }

    public @NotNull Configuration get() {
        return this.config.get();
    }

    public static ConfigurationContainer load(
            final Logger logger,
            final Path path,
            final String file
    ) {
        final HoconConfigurationLoader loader = HoconConfigurationLoader.builder()
                .defaultOptions(opts -> opts
                        .shouldCopyDefaults(true)
                        .header("VPingHost | by 4drian3d\n")
                )
                .path(path.resolve(file+".conf"))
                .build();


        try {
            final CommentedConfigurationNode node = loader.load();
            final Configuration config = node.get(Configuration.class);
            node.set(Configuration.class, config);
            loader.save(node);
            return new ConfigurationContainer(config, loader, logger);
        } catch (ConfigurateException exception){
            logger.error("Could not load configuration file", exception);
            return null;
        }
    }
}
