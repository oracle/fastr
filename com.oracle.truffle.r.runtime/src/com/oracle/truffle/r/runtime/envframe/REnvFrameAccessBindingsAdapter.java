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
import java.util.regex.*;

import com.oracle.truffle.api.CompilerDirectives.SlowPath;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.r.runtime.REnvironment.PutException;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;

/**
 * This adapter class handles the locking of bindings, but has null implementations of the basic
 * methods, which must be overridden by a subclass, while calling {@code super.method} appropriately
 * to invoke the locking logic.
 */
public class REnvFrameAccessBindingsAdapter extends REnvFrameAccess {
    /**
     * Records which bindings are locked. In normal use we don't expect any bindings to be locked so
     * this set is allocated lazily.
     */
    protected Set<String> lockedBindings;

    @Override
    public Object get(String key) {
        return null;
    }

    @Override
    public void put(String key, Object value) throws PutException {
        if (lockedBindings != null && lockedBindings.contains(key)) {
            throw createPutException(key);
        }
    }

    @Override
    public void rm(String key) {
        if (lockedBindings != null) {
            lockedBindings.remove(key);
        }
    }

    @Override
    public RStringVector ls(boolean allNames, Pattern pattern) {
        return RDataFactory.createEmptyStringVector();
    }

    @Override
    @SlowPath
    public void lockBindings() {
        Set<String> bindings = getBindingsForLock();
        if (bindings != null) {
            for (String binding : bindings) {
                lockBinding(binding);
            }
        }
    }

    protected Set<String> getBindingsForLock() {
        return null;
    }

    @Override
    @SlowPath
    public void lockBinding(String key) {
        if (lockedBindings == null) {
            lockedBindings = new HashSet<>();
        }
        lockedBindings.add(key);
    }

    @SlowPath
    public PutException createPutException(String key) throws PutException {
        throw new PutException(RError.Message.ENV_CHANGE_BINDING, key);
    }

    @Override
    public void unlockBinding(String key) {
        if (lockedBindings != null) {
            lockedBindings.remove(key);
        }
    }

    @Override
    public boolean bindingIsLocked(String key) {
        return lockedBindings != null && lockedBindings.contains(key);
    }

    @Override
    public MaterializedFrame getFrame() {
        return null;
    }
}
