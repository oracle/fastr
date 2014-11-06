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
package com.oracle.truffle.r.runtime.env.frame;

import java.util.*;
import java.util.regex.*;

import com.oracle.truffle.api.CompilerDirectives.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.env.*;
import com.oracle.truffle.r.runtime.env.REnvironment.*;

/**
 * Variant of {@link REnvFrameAccess} environments where the "frame" is a {@link LinkedHashMap},
 * e.g, for {link NewEnv}. By default there is no Truffle connection, i.e. {@link #getFrame()}
 * returns null. However, if the owning environment is "attach"ed, or used in an "eval", then an
 * {@link REnvMaterializedFrame} is created.
 */
public class REnvMapFrameAccess extends REnvFrameAccessBindingsAdapter {
    private final Map<String, Object> map;
    private REnvMaterializedFrame frame;

    public REnvMapFrameAccess(int size) {
        this.map = newHashMap(size);
    }

    public void setMaterializedFrame(REnvMaterializedFrame frame) {
        this.frame = frame;
    }

    @TruffleBoundary
    private static LinkedHashMap<String, Object> newHashMap(int size) {
        return size == 0 ? new LinkedHashMap<>() : new LinkedHashMap<>(size);
    }

    @Override
    public Object get(String key) {
        return map.get(key);
    }

    @Override
    public void rm(String key) {
        super.rm(key);
        map.remove(key);
        if (frame != null) {
            frame.rm(key);
        }
    }

    @TruffleBoundary
    @Override
    public void put(String key, Object value) throws PutException {
        super.put(key, value);
        map.put(key, value);
        if (frame != null) {
            frame.put(key, value);
        }
    }

    @Override
    public RStringVector ls(boolean allNames, Pattern pattern) {
        if (allNames && pattern == null) {
            return RDataFactory.createStringVector(map.keySet().toArray(RRuntime.STRING_ARRAY_SENTINEL), RDataFactory.COMPLETE_VECTOR);
        } else {
            ArrayList<String> matchedNamesList = new ArrayList<>(map.size());
            for (String name : map.keySet()) {
                if (REnvironment.includeName(name, allNames, pattern)) {
                    matchedNamesList.add(name);
                }
            }
            String[] names = new String[matchedNamesList.size()];
            return RDataFactory.createStringVector(matchedNamesList.toArray(names), RDataFactory.COMPLETE_VECTOR);
        }
    }

    @Override
    protected Set<String> getBindingsForLock() {
        return map.keySet();
    }

    @Override
    public MaterializedFrame getFrame() {
        return frame;
    }

    public void detach() {
        frame = null;
    }

    public Map<String, Object> getMap() {
        return map;
    }

}
