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
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import net.minecraft.server.v1_7_R1.Packet;
import net.minecraft.server.v1_7_R1.PacketPlayOutEntityTeleport;
import net.minecraft.server.v1_7_R1.PacketPlayOutEntityVelocity;
import net.minecraft.server.v1_7_R1.PacketPlayOutNamedEntitySpawn;
import net.minecraft.server.v1_7_R1.PacketPlayOutRelEntityMove;
import net.minecraft.server.v1_7_R1.PacketPlayOutRelEntityMoveLook;

import com.google.common.collect.ImmutableMap;

public enum PacketRegistry {
    SPAWN(PacketPlayOutNamedEntitySpawn.class) {
        {
            this.map("a", PacketFields.ENTITY_ID);
            this.map("c", PacketFields.X);
            this.map("d", PacketFields.Y);
            this.map("e", PacketFields.Z);
        }
    },
    VELOCITY(PacketPlayOutEntityVelocity.class) {
        {
            this.map("a", PacketFields.ENTITY_ID);
            this.map("b", PacketFields.MOT_X);
            this.map("c", PacketFields.MOT_Y);
            this.map("d", PacketFields.MOT_Z);
        }
    },
    MOVE(PacketPlayOutRelEntityMove.class) {
        {
            this.map("a", PacketFields.ENTITY_ID);
            this.map("b", PacketFields.X);
            this.map("c", PacketFields.Y);
            this.map("d", PacketFields.Z);
        }
    },
    MOVELOOK(PacketPlayOutRelEntityMoveLook.class) {
        {
            this.map("a", PacketFields.ENTITY_ID);
            this.map("b", PacketFields.X);
            this.map("c", PacketFields.Y);
            this.map("d", PacketFields.Z);
        }
    },
    TELEPORT(PacketPlayOutEntityTeleport.class) {
        {
            this.map("a", PacketFields.ENTITY_ID);
            this.map("b", PacketFields.X);
            this.map("c", PacketFields.Y);
            this.map("d", PacketFields.Z);
        }
    },
    ;

    private static Map<Class<? extends Packet>, PacketRegistry> byClass;
    private static Set<Integer> trackedEntID = new HashSet<Integer>();

    static {
        final Map<Class<? extends Packet>, PacketRegistry> map = new HashMap<>();
        for (final PacketRegistry registry : PacketRegistry.values()) {
            map.put(registry.clazz, registry);
        }
        PacketRegistry.byClass = ImmutableMap.copyOf(map);
    }

    public static String getOutput(Object packet) {
        if (!PacketRegistry.byClass.containsKey(packet.getClass())) {
            return null;
        }
        final PacketRegistry reg = PacketRegistry.byClass.get(packet.getClass());
        try {
            if (!PacketRegistry.trackedEntID.contains(reg.mapping.get(PacketFields.ENTITY_ID).get(packet))) {
                return null;
            }
        } catch (final Exception e) {
        }
        final StringBuilder builder = new StringBuilder();
        builder.append(reg.name()).append('{');
        for (final Map.Entry<PacketFields, Field> entry : reg.mapping.entrySet()) {
            builder.append('"').append(entry.getKey()).append("\": \"");
            String out;
            try {
                out = entry.getValue().get(packet).toString();
            } catch (final Exception e) {
                out = e.getMessage();
            }
            builder.append(out).append("\", ");
        }
        if (!reg.mapping.isEmpty()) {
            builder.setLength(builder.length() - 2);
        }
        builder.append('}');
        return builder.toString();
    }

    static void track(int ID) {
        PacketRegistry.trackedEntID.add(ID);
    }

    static void untrack(int ID) {
        PacketRegistry.trackedEntID.remove(ID);
    }

    private final Class<? extends Packet> clazz;

    private final Map<PacketFields, Field> mapping = new LinkedHashMap<>();

    private PacketRegistry(Class<? extends Packet> clazz) {
        this.clazz = clazz;
    }

    protected void map(String fieldName, PacketFields track) {
        map(fieldName, track, this.clazz);
    }

    protected void map(String fieldName, PacketFields track, Class<?> clazz) {
        try {
            final Field field = clazz.getDeclaredField(fieldName);
            field.setAccessible(true);
            this.mapping.put(track, field);
        } catch (final NoSuchFieldException e) {
            Class<?> sup = clazz.getSuperclass();
            if (!sup.equals(Object.class)) {
                this.map(fieldName, track, sup);
            }
        }
    }

    Class<? extends Packet> getClazz() {
        return this.clazz;
    }
}