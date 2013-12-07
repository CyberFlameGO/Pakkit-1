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
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.bukkit.configuration.ConfigurationSection;

final class PakkitPacket {
    private final Class<?> clazz;
    private final Map<String, Field> mappings = Collections.synchronizedMap(new LinkedHashMap<String, Field>());
    private final Map<String, String> mappingsReversed = Collections.synchronizedMap(new HashMap<String, String>());
    private final Map<String, Field> internalMap = Collections.synchronizedMap(new LinkedHashMap<String, Field>());
    private boolean enabled = false;
    private boolean full = false;

    PakkitPacket(Class<?> clazz) {
        this(clazz, false, true);
    }

    PakkitPacket(Class<?> clazz, boolean enabled, boolean full) {
        this.clazz = clazz;
        for (final Field field : clazz.getDeclaredFields()) {
            field.setAccessible(true);
            this.internalMap.put(field.getName(), field);
        }
        this.enabled = enabled;
        this.full = full;
    }

    boolean isEnabled() {
        return this.enabled;
    }

    String map(String fieldName, String mapping) {
        final Field field = this.internalMap.get(fieldName);
        if (field == null) {
            return null;
        }
        final Field old = this.mappings.put(mapping, field);
        this.mappingsReversed.put(fieldName, mapping);
        return old == null ? null : old.getName();
    }

    String print(Object o) {
        if (!this.enabled) {
            return null;
        }
        final StringBuilder builder = new StringBuilder();
        builder.append(this.clazz.getSimpleName()).append('{');
        for (final Map.Entry<String, Field> entry : (this.full ? this.internalMap : this.mappings).entrySet()) {
            String name;
            if (this.full && this.mappingsReversed.containsKey(entry.getKey())) {
                name = this.mappingsReversed.get(entry.getKey());
            } else {
                name = entry.getKey();
            }

            builder.append('\"').append(name).append("\":\"");
            try {
                builder.append(entry.getValue().get(o).toString());
            } catch (IllegalArgumentException | IllegalAccessException e) {
                builder.append(e.getMessage());
            }
            builder.append("\", ");
        }
        if (!this.mappings.isEmpty()) {
            builder.setLength(builder.length() - 2);
        }
        builder.append('}');
        return builder.toString();
    }

    void save(ConfigurationSection conf) {
        conf.set("enabled", this.enabled);
        conf.set("full", this.full);
        conf.createSection("mappings", this.mappingsReversed);
    }

    void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    void setFull(boolean full) {
        this.full = full;
    }
}