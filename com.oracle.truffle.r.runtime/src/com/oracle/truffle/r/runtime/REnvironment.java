/*
 * Copyright (c) 2013, 2014, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.CompilerDirectives.SlowPath;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.r.runtime.data.*;

/**
 * Denotes an R {@code environment}. R environments can be named or unnamed. {@code base} is an
 * example of a named environment. Environments associated with function invocations are unnamed.
 * The {@code environmentName} builtin returns "" for an unnamed environment. However, unnamed
 * environments print using a unique numeric id in the place where the name would appear for a named
 * environment. This is finessed using the {@link #getPrintNameHelper} method.
 */
public class REnvironment {

    public static final String UNNAMED = "";
    private final FrameDescriptor descriptor;
    private final REnvironment parent;
    private final Map<String, Object> map;
    private final String name;

    private static final Map<String, REnvironment> namedEnvironments = new HashMap<>();

    public REnvironment() {
        this(null, UNNAMED, 0);
    }

    public REnvironment(String name) {
        this(null, name, 0);
    }

    public REnvironment(REnvironment parent, String name) {
        this(parent, name, 0);
    }

    public REnvironment(REnvironment parent, String name, int size) {
        this.parent = parent;
        this.name = name;
        this.descriptor = new FrameDescriptor();
        this.map = newHashMap(size);
        if (!name.equals(UNNAMED)) {
            namedEnvironments.put(name, this);
        }
    }

    @SlowPath
    private static LinkedHashMap<String, Object> newHashMap(int size) {
        return size == 0 ? new LinkedHashMap<>() : new LinkedHashMap<>(size);
    }

    public static REnvironment lookup(String name) {
        return namedEnvironments.get(name);
    }

    public FrameDescriptor getDescriptor() {
        return descriptor;
    }

    public REnvironment getParent() {
        return parent;
    }

    public String getName() {
        return name;
    }

    @SlowPath
    public String getPrintName() {
        return new StringBuilder("<environment: ").append(getPrintNameHelper()).append('>').toString();
    }

    protected String getPrintNameHelper() {
        return getName();
    }

    public Object get(String key) {
        return map.get(key);
    }

    public void put(String key, Object value) {
        map.put(key, value);
    }

    public RStringVector ls() {
        return RDataFactory.createStringVector(map.keySet().toArray(RRuntime.STRING_ARRAY_SENTINEL), RDataFactory.COMPLETE_VECTOR);
    }

    @Override
    @SlowPath
    public String toString() {
        return getPrintName();
    }

}
