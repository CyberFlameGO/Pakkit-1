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

import java.util.logging.Level;

import org.bukkit.plugin.java.JavaPlugin;

public final class Plugin extends JavaPlugin {
    @Override
    public void onEnable() {
        try {
            Class.forName("net.minecraft.util.io.netty.channel.Channel");
        } catch (final ClassNotFoundException e) {
            this.getLogger().log(Level.SEVERE, "I cannot has 1.7 or higher", e);
            return;
        }
        new Pakkit(this);
    }
}
