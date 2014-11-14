/*
 * Copyright (c) 2014, 2014, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.oracle.truffle.r.runtime;

import java.util.*;

import com.oracle.truffle.r.runtime.data.*;

/**
 * Central location for all R options, that is for the {@code options(...)} and {@code getOption}
 * builtins. When a package is loaded, it must call {@link #registerHandler}, which will immediately
 * callback the {@code addOptions} method that should register the options for that package. The
 * {@code initialize} method will be called (later) which can set values based on the current
 * execution environment.
 *
 * {@code null} is used to designate an unset option.
 *
 */
public class ROptions {

    public interface Handler {
        void initialize();

        void addOptions();
    }

    private static final HashMap<String, Object> map = new HashMap<>();
    private static final ArrayList<Handler> handlers = new ArrayList<>();

    public static void initialize() {
        for (Handler handler : handlers) {
            handler.initialize();
        }
    }

    public static void registerHandler(Handler handler) {
        handlers.add(handler);
        handler.addOptions();
    }

    public static Set<Map.Entry<String, Object>> getValues() {
        Set<Map.Entry<String, Object>> result = new HashSet<>();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (entry.getValue() != null) {
                result.add(entry);
            }
        }
        return result;
    }

    public static Object getValue(String key) {
        return map.get(key);
    }

    public static Object setValue(String key, Object value) {
        Object previous = map.get(key);
        if (value == RNull.instance) {
            map.remove(key);
        } else {
            map.put(key, value);
        }
        return previous;
    }

    public static void addOption(String name, Object value) {
        setValue(name, value);
    }

    public static void addOptions(String[] names, Object[] values) {
        for (int i = 0; i < names.length; i++) {
            map.put(names[i], values == null ? null : values[i]);
        }
    }
}
