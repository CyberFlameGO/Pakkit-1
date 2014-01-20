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
import net.minecraft.server.v1_7_R1.PacketPlayOutAbilities;
import net.minecraft.server.v1_7_R1.PacketPlayOutAnimation;
import net.minecraft.server.v1_7_R1.PacketPlayOutAttachEntity;
import net.minecraft.server.v1_7_R1.PacketPlayOutBed;
import net.minecraft.server.v1_7_R1.PacketPlayOutEntityTeleport;
import net.minecraft.server.v1_7_R1.PacketPlayOutEntityVelocity;
import net.minecraft.server.v1_7_R1.PacketPlayOutNamedEntitySpawn;
import net.minecraft.server.v1_7_R1.PacketPlayOutRelEntityMove;
import net.minecraft.server.v1_7_R1.PacketPlayOutRelEntityMoveLook;

import com.google.common.collect.ImmutableMap;

public enum PacketRegistry {
    ABILITIES(PacketPlayOutAbilities.class) {
        {
            this.map("a", "isInvulnerable");
            this.map("b", "isFlying");
            this.map("c", "canFly");
            this.map("d", "canInstantlyBuild");
            this.map("e", "flyspeed");
            this.map("f", "walkspeed");
        }
    },
    ANIMATION(PacketPlayOutAnimation.class) {
        {
            this.map("a", PacketRegistry.ENTITY_ID);
            this.map("b", "animation", new OutputSingleItem() {
                @Override
                String getOutput(Object o) {
                    if (!(o instanceof Integer)) {
                        return "NULL, ERROR";
                    }
                    final int value = ((Integer) o).intValue();
                    switch (value) {
                        case 0:
                            return "None";
                        case 1:
                            return "Swing arm";
                        case 2:
                            return "Damaged";
                        case 3:
                            return "Leave bed";
                        case 5:
                            return "Eat food";
                        case 6:
                            return "Critical!";
                        case 7:
                            return "Magical Critical!";
                        case 104:
                            return "Crouch";
                        case 105:
                            return "Stop crouching";
                        default:
                            return "Unknown(" + value + ")";
                    }
                }
            });
        }
    },
    ATTACH_ENTITY(PacketPlayOutAttachEntity.class) {
        {
            this.map("a", "attachType", new OutputSingleItem() {
                @Override
                String getOutput(Object o) {
                    if (!(o instanceof Integer)) {
                        return "NULL, ERROR";
                    }
                    final int value = ((Integer) o).intValue();
                    switch (value) {
                        case 0:
                            return "Vehicle";
                        case 1:
                            return "Leash";
                        default:
                            return "Unknown(" + value + ")";
                    }
                }
            });
            this.map("b", PacketRegistry.ENTITY_ID + "_rider/leashed");
            this.map("c", PacketRegistry.ENTITY_ID + "_vehicle/holder");
        }
    },
    BED(PacketPlayOutBed.class) {
        {
            this.map("a", PacketRegistry.ENTITY_ID);
            this.map("b", "X");
            this.map("c", "Y");
            this.map("d", "Z");
        }
    },
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

    abstract class Output {
        final String getOutput(String name, Field field, Object packet) {
            Object o;
            try {
                o = field.get(packet);
            } catch (final Exception e) {
                o = null;
            }
            return this.getOutput(name, o);
        }

        abstract String getOutput(String name, Object o);
    }

    class OutputDefault extends OutputSingleItem {
        @Override
        public String getOutput(Object o) {
            return o.toString();
        }
    }

    abstract class OutputMultiItem extends Output {
        @Override
        public String getOutput(String name, Object o) {
            final StringBuilder builder = new StringBuilder();
            for (final Map.Entry<String, String> entry : this.getItems(o).entrySet()) {
                builder.append('"').append(entry.getKey()).append("\": \"");
                builder.append(entry.getValue()).append("\", ");
            }
            return builder.toString();
        }

        abstract Map<String, String> getItems(Object o);
    }

    abstract class OutputSingleItem extends Output {
        @Override
        final String getOutput(String name, Object o) {
            final StringBuilder builder = new StringBuilder();
            builder.append('"').append(name).append("\": \"");
            builder.append(this.getOutput(o)).append("\", ");
            return builder.toString();
        }

        abstract String getOutput(Object o);
    }

    class PacketInfo {
        private final String name;
        private final Field field;
        private final Output output;

        PacketInfo(String name, Field field, Output output) {
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

        Output getOutputter() {
            return this.output;
        }
    }

    private static final String ENTITY_ID = "EntityID";
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
    private final Output DEFAULT_OUTPUT = new OutputDefault();

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

    protected void map(Output output, String fieldName, String name, Class<?> clazz) {
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
        this.map(fieldName, name, this.DEFAULT_OUTPUT);
    }

    protected void map(String fieldName, String name, Output customOutput) {
        this.map(customOutput, fieldName, name, this.clazz);
    }

    Class<? extends Packet> getClazz() {
        return this.clazz;
    }
}