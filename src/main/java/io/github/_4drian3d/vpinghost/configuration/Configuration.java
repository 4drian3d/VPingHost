package io.github._4drian3d.vpinghost.configuration;

import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;

import java.util.List;

@ConfigSerializable
public class Configuration {
    @Comment("""
        Ping Format
        Placeholders:
        -| <description>
          Shows you the description of the motd
        -| <json>
          It shows you the motd description directly in json format
        -| <protocol>
          It shows you the protocol with which the server was pinged
        """)
    public List<String> pingFormat = List.of(
            "<gray>Server:</gray> <white><server>",
            "<gray>Component Description:</gray>",
            "<description>",
            "<gray>JSON Description:</gray>",
            "<json>",
            "<gray>Protocol: <white><protocol>"
    );

    public Cooldown cooldown = new Cooldown();

    @ConfigSerializable
    public static class Cooldown {
        @Comment("Cooldown message")
        public String cooldownMessage = "You have to wait <time>ms to ping to another server.";
    }
}
