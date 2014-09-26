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
import java.util.stream.*;

import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.RError.Message;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.env.*;
import com.oracle.truffle.r.runtime.env.REnvironment.*;

/**
 * Variant of {@link REnvFrameAccess} that provides access to an actual Truffle execution frame.
 */
public class REnvTruffleFrameAccess extends REnvFrameAccessBindingsAdapter {

    private final MaterializedFrame frame;

    public REnvTruffleFrameAccess(VirtualFrame frame) {
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
        // check locking, handled in superclass
        super.put(key, value);
        FrameDescriptor fd = frame.getFrameDescriptor();
        FrameSlot slot = fd.findFrameSlot(key);

        // Handle RPromise: It cannot be cast to a int/double/byte!
        if (value instanceof RPromise) {
            if (slot == null) {
                slot = addFrameSlot(fd, key, FrameSlotKind.Object);
            }
            // Overwrites former FrameSlotKind
            frame.setObject(slot, value);
            return;
        }

        // Handle all other values
        FrameSlotKind slotKind = null;
        if (slot == null) {
            slotKind = RRuntime.getSlotKind(value);
            if (slotKind != FrameSlotKind.Illegal) {
                slot = addFrameSlot(fd, key, slotKind);
            }
        } else {
            slotKind = slot.getKind();
        }
        switch (slotKind) {
            case Byte:
                frame.setByte(slot, (byte) value);
                break;
            case Int:
                frame.setInt(slot, (int) value);
                break;
            case Double:
                frame.setDouble(slot, (double) value);
                break;
            case Object:
                frame.setObject(slot, value);
                break;
            case Illegal:
                break;
            default:
                throw new PutException(Message.GENERIC, "frame slot exception");
        }
    }

    private static FrameSlot addFrameSlot(FrameDescriptor fd, String key, FrameSlotKind kind) {
        return fd.addFrameSlot(key, new FrameSlotChangeMonitor(), kind);
    }

    @Override
    public void rm(String key) {
        super.rm(key);
    }

    @Override
    public RStringVector ls(boolean allNames, Pattern pattern) {
        FrameDescriptor fd = frame.getFrameDescriptor();
        String[] names = getStringIdentifiers(fd);
        ArrayList<String> matchedNamesList = new ArrayList<>(names.length);
        for (int i = 0; i < names.length; ++i) {
            String name = names[i];
            if (frame.getValue(fd.findFrameSlot(name)) == null) {
                continue;
            }
            if (REnvironment.includeName(name, allNames, pattern)) {
                matchedNamesList.add(name);
            }
        }
        String[] data = new String[matchedNamesList.size()];
        return RDataFactory.createStringVector(matchedNamesList.toArray(data), RDataFactory.COMPLETE_VECTOR);
    }

    @Override
    protected Set<String> getBindingsForLock() {
        // TODO Auto-generated method stub
        return null;
    }

    private static String[] getStringIdentifiers(FrameDescriptor fd) {
        return fd.getIdentifiers().stream().filter(e -> (e instanceof String)).collect(Collectors.toSet()).toArray(RRuntime.STRING_ARRAY_SENTINEL);
    }

}
