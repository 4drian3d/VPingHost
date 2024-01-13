package io.github._4drian3d.vpinghost;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.inject.Inject;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.PingOptions;
import com.velocitypowered.api.proxy.server.ServerInfo;
import io.github._4drian3d.vpinghost.configuration.ConfigurationContainer;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static net.kyori.adventure.text.minimessage.MiniMessage.miniMessage;

@Plugin(
        id = "vpinghost",
        name = "VPingHost",
        description = "A simple Velocity plugin that allows you to get information about other servers instantly",
        version = Constants.VERSION,
        authors = {"4drian3d"}
)
public final class VPingHost {
    private static final List<Integer> SUPPORTED_PROTOCOLS = ProtocolVersion.SUPPORTED_VERSIONS
            .stream().map(ProtocolVersion::getProtocol).toList();
    private final Cache<String, ServerInfo> SERVER_INFO_CACHE = Caffeine.newBuilder()
            .expireAfterWrite(1, TimeUnit.MINUTES)
            .build();
    private final Cache<UUID, Instant> COOLDOWN_CACHE = Caffeine.newBuilder()
            .expireAfterWrite(2, TimeUnit.SECONDS)
            .build();
    @Inject
    private ComponentLogger logger;
    @Inject
    private CommandManager commandManager;
    @Inject
    private ProxyServer proxyServer;
    @Inject
    @DataDirectory
    private Path dataDirectory;

    private ConfigurationContainer configurationContainer;

    @Subscribe
    void onProxyInitialization(final ProxyInitializeEvent ignoredEvent) {
        logger.info("Starting VPingHost");
        configurationContainer = ConfigurationContainer.load(logger, dataDirectory, "config");
        final var node = BrigadierCommand.literalArgumentBuilder("pinghost")
                .then(BrigadierCommand.requiredArgumentBuilder("address", StringArgumentType.string())
					.executes(ctx -> {
                        final CommandSource source = ctx.getSource();
                        if (isInCooldown(source)) {
                            return Command.SINGLE_SUCCESS;
                        }
						pingAddressFromProtocol(source, StringArgumentType.getString(ctx, "address"), ProtocolVersion.MAXIMUM_VERSION);
						return Command.SINGLE_SUCCESS;
					})
                    .then(BrigadierCommand.requiredArgumentBuilder("protocol", IntegerArgumentType.integer())
                        .suggests((ctx, builder) -> {
                            SUPPORTED_PROTOCOLS.forEach(builder::suggest);
                            return builder.buildFuture();
                        })
                        .executes(ctx -> {
                            final CommandSource source = ctx.getSource();
                            if (isInCooldown(source)) {
                                return Command.SINGLE_SUCCESS;
                            }
                            int protocol = IntegerArgumentType.getInteger(ctx, "protocol");
                            final ProtocolVersion version = ProtocolVersion.getProtocolVersion(protocol);
                            pingAddressFromProtocol(source, StringArgumentType.getString(ctx, "address"), version);
                            return Command.SINGLE_SUCCESS;
                        })
                    )
                );
        final BrigadierCommand command = new BrigadierCommand(node);
        final CommandMeta meta = commandManager.metaBuilder(command)
                .plugin(this)
                .build();
        commandManager.register(meta, command);
    }

    private InetSocketAddress addressFromString(final String string) {
        final int charAtSeparator = string.indexOf(':');
        if (charAtSeparator != -1) {
            final String host = string.substring(0, charAtSeparator);
            final String port = string.substring(charAtSeparator+1);
            return new InetSocketAddress(host, Integer.parseInt(port));
        } else {
            return new InetSocketAddress(string, 25565);
        }
    }

    private void pingAddressFromProtocol(final CommandSource source, final String address, final ProtocolVersion protocolVersion) {
        source.sendMessage(Component.text("Pinging server " + address, NamedTextColor.GOLD));
        final PingOptions options = PingOptions.builder()
                .version(protocolVersion)
                .timeout(5, TimeUnit.SECONDS)
                .build();
        ServerInfo serverInfo = SERVER_INFO_CACHE.getIfPresent(address);
        if (serverInfo == null) {
            final InetSocketAddress inetSocketAddress = addressFromString(address);
            serverInfo = new ServerInfo(UUID.randomUUID().toString(), inetSocketAddress);
            SERVER_INFO_CACHE.put(address, serverInfo);
        }
        proxyServer.createRawRegisteredServer(serverInfo)
                .ping(options)
                .orTimeout(10, TimeUnit.SECONDS)
                .handle((ping, throwable) -> {
                    if (throwable != null) {
                        source.sendMessage(Component.text("An error occurred while pinging the server " + address + ", check the console"));
                        logger.error("An error occurred while pinging the server {}", address, throwable);
                    } else {
                        final Component description = ping.getDescriptionComponent().compact();
                        String json = GsonComponentSerializer.gson().serialize(description);
                        if (json.indexOf(LegacyComponentSerializer.SECTION_CHAR) != -1) {
                            json = json.replace(LegacyComponentSerializer.SECTION_CHAR, LegacyComponentSerializer.AMPERSAND_CHAR);
                        }
                        final TagResolver resolver = TagResolver.resolver(
                                Placeholder.component("description", description),
                                Placeholder.parsed("json", json),
                                Placeholder.parsed("protocol", options.getProtocolVersion().toString()),
                                Placeholder.parsed("server", address)
                        );
                        final TextComponent.Builder builder = Component.text();
                        for (final String s : configurationContainer.get().pingFormat) {
                            builder.append(miniMessage().deserialize(s, resolver)).appendNewline();
                        }
                        source.sendMessage(builder);
                    }
                    return null;
                }).exceptionally(ex -> {
                    logger.error("An error has occurred while sending the ping result", ex);
                    return null;
                });
    }

    public boolean isInCooldown(CommandSource source) {
        final Instant now = Instant.now();
        final UUID uuid = source.get(Identity.UUID).orElse(null);
        if (uuid == null) {
            return false;
        }
        final Instant instant = COOLDOWN_CACHE.get(uuid, id -> now);
        if (instant == now) {
            return false;
        }
        final Duration duration = Duration.between(instant, now);
        final Component message = miniMessage().deserialize(configurationContainer.get()
                .cooldown.cooldownMessage, Placeholder.unparsed("time", Long.toString(duration.toMillis())));
        source.sendMessage(message);
        return true;
    }
}