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

import net.minecraft.server.v1_7_R1.Block;
import net.minecraft.server.v1_7_R1.Blocks;
import net.minecraft.server.v1_7_R1.Packet;
import net.minecraft.server.v1_7_R1.PacketPlayOutAbilities;
import net.minecraft.server.v1_7_R1.PacketPlayOutAnimation;
import net.minecraft.server.v1_7_R1.PacketPlayOutAttachEntity;
import net.minecraft.server.v1_7_R1.PacketPlayOutBed;
import net.minecraft.server.v1_7_R1.PacketPlayOutBlockAction;
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
                String getOutput(Object packet, Object extractedObject) {
                    if (!(extractedObject instanceof Integer)) {
                        return "NULL, ERROR";
                    }
                    final int value = ((Integer) extractedObject).intValue();
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
                String getOutput(Object packet, Object extractedObject) {
                    if (!(extractedObject instanceof Integer)) {
                        return "NULL, ERROR";
                    }
                    final int value = ((Integer) extractedObject).intValue();
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
    BLOCK_ACTION(PacketPlayOutBlockAction.class) {
        {
            final Field block = this.map("f", "block");
            this.map("a", "X");
            this.map("b", "Y");
            this.map("c", "Z");
            this.map("d", "byte1", new OutputMultiItem() {
                @Override
                Map<String, String> getItems(Object packet, Object extractedObject) {
                    Map<String, String> map = new HashMap<>();
                    Object blockO = null;
                    try {
                        blockO = block.get(packet);
                    } catch (Exception e) {
                    }
                    if (blockO == null || extractedObject == null) {
                        map.put("error", "null values");
                        return map;
                    }
                    if (blockO instanceof Block) {
                        if (blockO == Blocks.NOTE_BLOCK) {
                            String instrument;
                            int type = ((Byte) extractedObject).intValue();
                            switch (type) {
                                case 0:
                                    instrument = "harp";
                                    break;
                                case 1:
                                    instrument = "double bass";
                                    break;
                                case 2:
                                    instrument = "snare drum";
                                    break;
                                case 3:
                                    instrument = "click";
                                    break;
                                case 4:
                                    instrument = "bass drum";
                                    break;
                                default:
                                    instrument = "unknown";
                            }
                            map.put("instrument", instrument);
                        } else if (blockO == Blocks.PISTON) {
                            int type = ((Byte) extractedObject).intValue();
                            String movement;
                            switch (type) {
                                case 0:
                                    movement = "pushing";
                                    break;
                                case 1:
                                    movement = "pulling";
                                    break;
                                default:
                                    movement = "unknown";
                            }
                            map.put("movement", movement);
                        } else if (blockO == Blocks.CHEST || blockO == Blocks.TRAPPED_CHEST) {
                            int type = ((Byte) extractedObject).intValue();
                            map.put("value-always-1", String.valueOf(type));
                        }
                    }
                    return map;
                }
            });
            this.map("e", "byte2", new OutputMultiItem() {
                @Override
                Map<String, String> getItems(Object packet, Object extractedObject) {
                    Map<String, String> map = new HashMap<>();
                    Object blockO = null;
                    try {
                        blockO = block.get(packet);
                    } catch (Exception e) {
                    }
                    if (blockO == null || extractedObject == null) {
                        map.put("error", "null values");
                        return map;
                    }
                    if (blockO instanceof Block) {
                        if (blockO == Blocks.NOTE_BLOCK) {
                            map.put("pitch", String.valueOf(((Byte) extractedObject).intValue()));
                        } else if (blockO == Blocks.PISTON) {
                            int type = ((Byte) extractedObject).intValue();
                            String movement;
                            switch (type) {
                                case 0:
                                    movement = "down";
                                    break;
                                case 1:
                                    movement = "up";
                                    break;
                                case 2:
                                    movement = "south";
                                    break;
                                case 3:
                                    movement = "west";
                                    break;
                                case 4:
                                    movement = "north";
                                    break;
                                case 5:
                                    movement = "east";
                                    break;
                                default:
                                    movement = "unknown";
                            }
                            map.put("movement", movement);
                        } else if (blockO == Blocks.CHEST || blockO == Blocks.TRAPPED_CHEST) {
                            int type = ((Byte) extractedObject).intValue();
                            String state;
                            switch (type) {
                                case 0:
                                    state = "closed";
                                    break;
                                case 1:
                                    state = "open";
                                    break;
                                default:
                                    state = "unknown";
                            }
                            map.put("chest state", state);
                        }
                    }
                    return map;
                }
            });
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
            Object extractedObject;
            try {
                extractedObject = field.get(packet);
            } catch (final Exception e) {
                extractedObject = null;
            }
            return this.getOutput(packet, name, extractedObject);
        }

        abstract String getOutput(Object packet, String name, Object extractedObject);
    }

    class OutputDefault extends OutputSingleItem {
        @Override
        public String getOutput(Object packet, Object extractedObject) {
            return extractedObject.toString();
        }
    }

    abstract class OutputMultiItem extends Output {
        @Override
        public String getOutput(Object packet, String name, Object extractedObject) {
            final StringBuilder builder = new StringBuilder();
            for (final Map.Entry<String, String> entry : this.getItems(packet, extractedObject).entrySet()) {
                builder.append('"').append(entry.getKey()).append("\": \"");
                builder.append(entry.getValue()).append("\", ");
            }
            return builder.toString();
        }

        abstract Map<String, String> getItems(Object packet, Object extractedObject);
    }

    abstract class OutputSingleItem extends Output {
        @Override
        final String getOutput(Object packet, String name, Object extractedObject) {
            final StringBuilder builder = new StringBuilder();
            builder.append('"').append(name).append("\": \"");
            builder.append(this.getOutput(packet, extractedObject)).append("\", ");
            return builder.toString();
        }

        abstract String getOutput(Object packet, Object extractedObject);
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
            return "";
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

    protected Field map(Output output, String fieldName, String name, Class<?> clazz) {
        try {
            final Field field = clazz.getDeclaredField(fieldName);
            field.setAccessible(true);
            this.mapping.add(new PacketInfo(name, field, output));
            return field;
        } catch (final NoSuchFieldException e) {
            final Class<?> sup = clazz.getSuperclass();
            if ((sup == null) || sup.equals(Object.class)) {
                throw new AssertionError("Could not find field " + fieldName);
            }
            return this.map(output, fieldName, name, sup);
        }
    }

    protected Field map(String fieldName, String name) {
        return this.map(fieldName, name, this.DEFAULT_OUTPUT);
    }

    protected Field map(String fieldName, String name, Output customOutput) {
        return this.map(customOutput, fieldName, name, this.clazz);
    }

    Class<? extends Packet> getClazz() {
        return this.clazz;
    }
}