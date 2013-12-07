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

import java.util.Arrays;

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