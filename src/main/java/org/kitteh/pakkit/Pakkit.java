/*
 * Copyright 2012-2013 Matt Baxter
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http:www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.kitteh.pakkit;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import net.minecraft.util.io.netty.channel.Channel;
import net.minecraft.util.io.netty.channel.ChannelHandlerContext;
import net.minecraft.util.io.netty.channel.ChannelOutboundHandlerAdapter;
import net.minecraft.util.io.netty.channel.ChannelPromise;

/*
 * Because I am only performing inspection and not modifying anything (except
 * for adding a handler for the purpose of listening), I can safely (hah) 
 * screw around with crazy reflection like this. It's generally speaking a
 * HORRIBLE IDEA and you shouldn't use this code as an example.
 */
public class Pakkit implements Listener {
    public class Handler extends ChannelOutboundHandlerAdapter {
        private final Player player;

        private Handler(Player player) {
            this.player = player;
        }

        @Override
        public void write(ChannelHandlerContext ctx, Object packet, ChannelPromise promise) throws Exception {
            Pakkit.this.handlePacket(this.player, packet);
            super.write(ctx, packet, promise);
        }
    }

    private Class<?> craftPlayer;
    private Method getHandle;
    private Class<?> entityPlayerClass;
    private final Plugin plugin;
    private Field playerConnectionField;
    private Class<?> playerConnectionClass;
    private Field networkManagerField;
    private Class<?> networkManagerClass;

    private final Set<Field> channelFields = new HashSet<>();

    private final Map<Class<?>, PakkitPacket> packets = new ConcurrentHashMap<>();

    Pakkit(Plugin plugin) {
        this.plugin = plugin;
        final String serverPackage = this.plugin.getServer().getClass().getPackage().getName();
        final String version = serverPackage.substring(serverPackage.lastIndexOf('.') + 1);
        try {
            this.craftPlayer = Class.forName(serverPackage + ".entity.CraftPlayer");
            this.getHandle = this.craftPlayer.getMethod("getHandle");
            this.entityPlayerClass = this.getHandle.getReturnType();
            this.playerConnectionField = this.entityPlayerClass.getField("playerConnection");
            this.playerConnectionField.setAccessible(true);
            this.playerConnectionClass = this.playerConnectionField.getType();
            this.networkManagerField = this.playerConnectionClass.getField("networkManager");
            this.networkManagerField.setAccessible(true);
            this.networkManagerClass = this.networkManagerField.getType();
            for (final Field field : this.networkManagerClass.getDeclaredFields()) {
                if (field.getType().equals(Channel.class)) {
                    this.channelFields.add(field);
                    field.setAccessible(true);
                }
            }
            if (this.channelFields.isEmpty()) {
                throw new Exception("NO CHANNELS IN NETWORK MANAGER OMG");
            }
        } catch (final Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Could not start, unknown stuffs", e);
            return;
        }
        for (final Map.Entry<String, Object> entry : plugin.getConfig().getValues(false).entrySet()) {
            if (entry.getValue() instanceof ConfigurationSection) {
                Class<?> clazz;
                try {
                    clazz = Class.forName("net.minecraft.server." + version + "." + entry.getKey());
                } catch (final ClassNotFoundException e) {
                    plugin.getLogger().info("Ignoring entry " + entry.getKey());
                    continue;
                }
                final ConfigurationSection pac = (ConfigurationSection) entry.getValue();
                this.packets.put(clazz, new PakkitPacket(clazz, pac.getBoolean("enabled", false), pac.getBoolean("full", true)));
            }
        }
        plugin.getCommand("pakkit").setExecutor(new Command(this));
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        for (final Player player : plugin.getServer().getOnlinePlayers()) {
            this.inject(player);
        }
    }

    @EventHandler
    public void join(PlayerJoinEvent event) {
        this.inject(event.getPlayer());
    }

    private void handlePacket(Player player, Object packet) {
        final Class<?> clazz = packet.getClass();
        PakkitPacket pak = this.packets.get(clazz);
        if (pak == null) {
            pak = new PakkitPacket(clazz);
            this.packets.put(clazz, pak);
        }
        final String output = pak.print(packet);
        if ((output != null) && (output.length() > 1)) {
            if (clazz.getSimpleName().toLowerCase().contains("chat") && output.substring(1).contains(clazz.getSimpleName() + "{")) {
                return;
            }
            this.plugin.getServer().broadcastMessage(output);
        }
    }

    private void inject(Player player) {
        boolean injected = false;
        if (player.getClass().isAssignableFrom(this.craftPlayer)) {
            try {
                final Object entPlayer = this.getHandle.invoke(player);
                if (entPlayer.getClass().isAssignableFrom(this.entityPlayerClass)) {
                    final Object playerConnection = this.playerConnectionField.get(entPlayer);
                    if (playerConnection.getClass().isAssignableFrom(this.playerConnectionClass)) {
                        final Object networkManager = this.networkManagerField.get(playerConnection);
                        if (networkManager.getClass().isAssignableFrom(this.networkManagerClass)) {
                            final Handler handler = new Handler(player);
                            for (final Field field : this.channelFields) {
                                final Channel channel = (Channel) field.get(networkManager);
                                channel.pipeline().addLast(handler);
                                injected = true;
                            }
                        }
                    }
                }
            } catch (final Exception e) {
                this.plugin.getLogger().log(Level.WARNING, "Could not inject player " + player, e);
            }
        }
        if (!injected) {
            player.sendMessage("I could not hax ur pakkits");
        }
    }

    PakkitPacket getPacket(String string) {
        if (!string.startsWith("Packet")) {
            string = "PacketPlayOut" + string;
        }
        for (final Map.Entry<Class<?>, PakkitPacket> entry : this.packets.entrySet()) {
            if (entry.getKey().getSimpleName().equalsIgnoreCase(string)) {
                return entry.getValue();
            }
        }
        return null;
    }

    List<String> getPacketNames() {
        final List<String> names = new ArrayList<>();
        for (final Class<?> clazz : this.packets.keySet()) {
            names.add(clazz.getSimpleName().startsWith("PacketPlayOut") ? clazz.getSimpleName().substring("PacketPlayOut".length()) : clazz.getSimpleName());
        }
        Collections.sort(names);
        return names;
    }

    void save() {
        for (final Map.Entry<Class<?>, PakkitPacket> entry : this.packets.entrySet()) {
            entry.getValue().save(this.plugin.getConfig().createSection(entry.getKey().getSimpleName()));
        }
    }
}