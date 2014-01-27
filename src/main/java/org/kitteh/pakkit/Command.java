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

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;

import com.google.common.collect.ImmutableList;

final class Command implements TabExecutor {
    final class Args {
        private final String[] args;

        Args(String[] args) {
            this.args = args.length > 1 ? Arrays.copyOfRange(args, 1, args.length) : new String[0];
        }

        String get(int index) {
            if (this.args.length <= index) {
                return null;
            }
            return this.args[index];
        }

        int length() {
            return this.args.length;
        }
    }

    @Retention(value = RetentionPolicy.RUNTIME)
    @interface SubCommand {
        String arg();
    }

    private static final List<String> OPTIONS;
    private static final Map<String, Method> METHODS = new HashMap<>();
    private static final String opt;

    static {
        final List<String> list = new ArrayList<>();
        for (final Method method : Command.class.getDeclaredMethods()) {
            final SubCommand sub = method.getAnnotation(SubCommand.class);
            if (sub == null) {
                continue;
            }
            method.setAccessible(true);
            list.add(sub.arg());
            Command.METHODS.put(sub.arg().toLowerCase(), method);
        }
        OPTIONS = ImmutableList.copyOf(list);
        final StringBuilder builder = new StringBuilder();
        for (final String s : Command.OPTIONS) {
            builder.append(s).append(", ");
        }
        if (builder.length() > 0) {
            builder.setLength(builder.length() - 2);
        }
        opt = builder.toString();
    }

    private final Pakkit pakkit;

    Command(Pakkit pakkit) {
        this.pakkit = pakkit;
    }

    @Override
    public boolean onCommand(CommandSender sender, org.bukkit.command.Command cmd, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("Need moar args");
            return true;
        }
        final Method method = Command.METHODS.get(args[0].toLowerCase());
        if (method != null) {
            try {
                method.invoke(this, sender, new Args(args));
            } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            }
        } else {
            sender.sendMessage(Command.opt);
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, org.bukkit.command.Command cmd, String label, String[] args) {
        switch (args.length) {
            case 1:
                return this.match(Command.OPTIONS, args[0]);
            case 2:
                return this.match(this.pakkit.getPacketNames(), args[1]);
            default:
                return null;
        }
    }

    private List<String> match(List<String> haystack, String needle) {
        if (needle.length() == 0) {
            return haystack;
        }
        final List<String> list = new ArrayList<String>();
        for (final String hay : haystack) {
            if (hay.toLowerCase().startsWith(needle)) {
                list.add(hay);
            }
        }
        return ImmutableList.copyOf(list);
    }

    @SubCommand(arg = "disable")
    void disable(CommandSender sender, Args args) {
        if (args.length() < 1) {
            sender.sendMessage("Need the packet name");
            return;
        }
        final PakkitPacket packet = this.pakkit.getPacket(args.get(0));
        if (packet == null) {
            sender.sendMessage("Invalid packet " + args.get(0));
            return;
        }
        packet.setEnabled(false);
        this.pakkit.save();
        sender.sendMessage("Disabled " + args.get(0));
    }

    @SubCommand(arg = "enable")
    void enable(CommandSender sender, Args args) {
        if (args.length() < 1) {
            sender.sendMessage("Need the packet name");
            return;
        }
        final PakkitPacket packet = this.pakkit.getPacket(args.get(0));
        if (packet == null) {
            sender.sendMessage("Invalid packet " + args.get(0));
            return;
        }
        packet.setEnabled(true);
        this.pakkit.save();
        sender.sendMessage("Enabled " + args.get(0));
    }

    @SubCommand(arg = "full")
    void full(CommandSender sender, Args args) {
        if (args.length() < 2) {
            sender.sendMessage("Need the packet name and true or false");
            return;
        }
        boolean full;
        if (args.get(1).equalsIgnoreCase("true")) {
            full = true;
        } else if (args.get(1).equalsIgnoreCase("false")) {
            full = false;
        } else {
            sender.sendMessage("Must be true or false not " + args.get(1));
            return;
        }
        final PakkitPacket packet = this.pakkit.getPacket(args.get(0));
        if (packet == null) {
            sender.sendMessage("Invalid packet " + args.get(0));
            return;
        }
        packet.setFull(full);
        this.pakkit.save();
        sender.sendMessage("Set full on " + args.get(0) + " to " + full);
    }

    @SubCommand(arg = "map")
    void map(CommandSender sender, Args args) {
        if (args.length() < 3) {
            sender.sendMessage("map PacketName FieldName MappedName");
            return;
        }
        final PakkitPacket packet = this.pakkit.getPacket(args.get(0));
        if (packet == null) {
            sender.sendMessage("Invalid packet " + args.get(0));
            return;
        }
        final String old = packet.map(args.get(1), args.get(2));
        this.pakkit.save();
        sender.sendMessage("Mapped " + args.get(1) + " to " + args.get(2) + (old == null ? "." : "(was " + old + ")."));
    }
}