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
package com.oracle.truffle.r.runtime.envframe;

import java.util.*;
import java.util.stream.*;

import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.REnvironment.*;
import com.oracle.truffle.r.runtime.data.*;

/**
 * Variant of {@link REnvFrameAccess} that provides access to an actual Truffle execution frame.
 */
public class REnvTruffleFrameAccess extends REnvFrameAccessBindingsAdapter {

    private MaterializedFrame frame;
    private Object id;

    public REnvTruffleFrameAccess(VirtualFrame frame) {
        this.id = new Object();
        FrameDescriptor fd = frame.getFrameDescriptor();
        FrameSlot idSlot = fd.addFrameSlot(id);
        frame.setObject(idSlot, id);
        this.frame = frame.materialize();
    }

    public REnvTruffleFrameAccess(MaterializedFrame frame) {
        this.frame = frame;
    }

    @Override
    public MaterializedFrame getFrame() {
        return frame;
    }

    @Override
    public Object get(String key) {
        FrameDescriptor fd = frame.getFrameDescriptor();
        FrameSlot slot = fd.findFrameSlot(key);
        if (slot == null) {
            return null;
        } else {
            return frame.getValue(slot);
        }
    }

    @Override
    public void put(String key, Object value) throws PutException {
        // check locking
        super.put(key, value);
        FrameDescriptor fd = frame.getFrameDescriptor();
        FrameSlot slot = fd.findFrameSlot(key);
        if (slot != null) {
            frame.setObject(slot, value);
        } else {
            slot = fd.addFrameSlot(key, FrameSlotKind.Object);
            frame.setObject(slot, value);
        }
    }

    @Override
    public void rm(String key) {
        super.rm(key);
    }

    @Override
    public RStringVector ls(boolean allNames, String pattern) {
        // TODO support pattern
        FrameDescriptor fd = frame.getFrameDescriptor();
        String[] names = getStringIdentifiers(fd);
        int undefinedIdentifiers = 0;
        for (int i = 0; i < names.length; ++i) {
            if (frame.getValue(fd.findFrameSlot(names[i])) == null) {
                names[i] = null;
                ++undefinedIdentifiers;
            }
        }
        String[] definedNames = new String[names.length - undefinedIdentifiers];
        int j = 0;
        for (int i = 0; i < names.length; ++i) {
            if (names[i] != null) {
                definedNames[j++] = names[i];
            }
        }
        if (!allNames) {
            definedNames = REnvironment.removeHiddenNames(definedNames);
        }
        return RDataFactory.createStringVector(definedNames, RDataFactory.COMPLETE_VECTOR);
    }

    @Override
    protected Set<String> getBindingsForLock() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Object id() {
        return id;
    }

    private static String[] getStringIdentifiers(FrameDescriptor fd) {
        return fd.getIdentifiers().stream().filter(e -> (e instanceof String)).collect(Collectors.toSet()).toArray(RRuntime.STRING_ARRAY_SENTINEL);
    }

}
