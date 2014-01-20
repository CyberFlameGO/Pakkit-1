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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
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
            this.map("a", PacketRegistry.ENTITY_ID);
            this.map("c", "X");
            this.map("d", "Y");
            this.map("e", "Z");
        }
    },
    VELOCITY(PacketPlayOutEntityVelocity.class) {
        {
            this.map("a", PacketRegistry.ENTITY_ID);
            this.map("b", "MOT_X");
            this.map("c", "MOT_Y");
            this.map("d", "MOT_Z");
        }
    },
    MOVE(PacketPlayOutRelEntityMove.class) {
        {
            this.map("a", PacketRegistry.ENTITY_ID);
            this.map("b", "X");
            this.map("c", "Y");
            this.map("d", "Z");
        }
    },
    MOVELOOK(PacketPlayOutRelEntityMoveLook.class) {
        {
            this.map("a", PacketRegistry.ENTITY_ID);
            this.map("b", "X");
            this.map("c", "Y");
            this.map("d", "Z");
        }
    },
    TELEPORT(PacketPlayOutEntityTeleport.class) {
        {
            this.map("a", PacketRegistry.ENTITY_ID);
            this.map("b", "X");
            this.map("c", "Y");
            this.map("d", "Z");
        }
    },
    ;

    static class DefaultFieldOutputter implements FieldOutputter {
        @Override
        public String getOutput(String name, Field field, Object packet) {
            final StringBuilder builder = new StringBuilder();
            builder.append('"').append(name).append("\": \"");
            String out;
            try {
                out = field.get(packet).toString();
            } catch (final Exception e) {
                out = e.getMessage();
            }
            builder.append(out).append("\", ");
            return builder.toString();
        }
    }

    interface FieldOutputter {
        String getOutput(String name, Field field, Object packet);
    }

    class PacketInfo {
        private final String name;
        private final Field field;
        private final FieldOutputter output;

        PacketInfo(String name, Field field, FieldOutputter output) {
            this.name = name;
            this.field = field;
            this.output = output;
        }

        Field getField() {
            return this.field;
        }

        String getName() {
            return this.name;
        }

        FieldOutputter getOutputter() {
            return this.output;
        }
    }

    private static final String ENTITY_ID = "EntityID";
    private static Map<Class<? extends Packet>, PacketRegistry> byClass;
    private static Set<Integer> trackedEntID = new HashSet<Integer>();
    private static FieldOutputter DEFAULT_OUTPUT = new DefaultFieldOutputter();

    static {
        final Map<Class<? extends Packet>, PacketRegistry> map = new HashMap<>();
        for (final PacketRegistry registry : PacketRegistry.values()) {
            map.put(registry.clazz, registry);
        }
        PacketRegistry.byClass = ImmutableMap.copyOf(map);
    }

    public static String getOutput(Object packet) {
        final PacketRegistry reg = PacketRegistry.byClass.get(packet.getClass());
        if (reg == null) {
            return null;
        }
        return reg.output(packet);
    }

    static void track(int ID) {
        PacketRegistry.trackedEntID.add(ID);
    }

    static void untrack(int ID) {
        PacketRegistry.trackedEntID.remove(ID);
    }

    private final Class<? extends Packet> clazz;
    private final List<PacketInfo> mapping = new ArrayList<>();

    private PacketRegistry(Class<? extends Packet> clazz) {
        this.clazz = clazz;
    }

    private String output(Object packet) {
        final StringBuilder builder = new StringBuilder();
        builder.append(this.name()).append('{');
        for (final PacketInfo info : this.mapping) {
            builder.append(info.getOutputter().getOutput(info.getName(), info.getField(), packet));
        }
        if (!this.mapping.isEmpty()) {
            builder.setLength(builder.length() - 2);
        }
        builder.append('}');
        return builder.toString();
    }

    protected void map(FieldOutputter output, String fieldName, String name, Class<?> clazz) {
        try {
            final Field field = clazz.getDeclaredField(fieldName);
            field.setAccessible(true);
            this.mapping.add(new PacketInfo(name, field, output));
        } catch (final NoSuchFieldException e) {
            final Class<?> sup = clazz.getSuperclass();
            if (sup == null) {
                throw new AssertionError("Could not find field " + fieldName);
            }
            if (!sup.equals(Object.class)) {
                this.map(output, fieldName, name, sup);
            }
        }
    }

    protected void map(String fieldName, String name) {
        this.map(fieldName, name, PacketRegistry.DEFAULT_OUTPUT);
    }

    protected void map(String fieldName, String name, FieldOutputter customOutput) {
        this.map(customOutput, fieldName, name, this.clazz);
    }

    Class<? extends Packet> getClazz() {
        return this.clazz;
    }
}