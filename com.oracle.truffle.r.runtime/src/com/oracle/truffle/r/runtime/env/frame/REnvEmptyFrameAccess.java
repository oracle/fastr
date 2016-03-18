/*
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates. All rights reserved.
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

import java.util.regex.Pattern;

import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.env.REnvironment;
import com.oracle.truffle.r.runtime.env.REnvironment.PutException;

public final class REnvEmptyFrameAccess extends REnvFrameAccess {
    private static final RStringVector EMPTY = RDataFactory.createEmptyStringVector();

    @Override
    public MaterializedFrame getFrame() {
        return null;
    }

    @Override
    public Object get(String key) {
        return null;
    }

    @Override
    public void put(String key, Object value) throws REnvironment.PutException {
        throw RInternalError.shouldNotReachHere();
    }

    @Override
    public void rm(String key) throws PutException {
        throw new PutException(RError.Message.UNKNOWN_OBJECT, key);
    }

    @Override
    public RStringVector ls(boolean allNames, Pattern pattern, boolean sorted) {
        return EMPTY;
    }

    @Override
    public void lockBindings() {
        throw RInternalError.shouldNotReachHere();
    }

    @Override
    public void lockBinding(String key) {
        throw RInternalError.shouldNotReachHere();
    }

    @Override
    public void unlockBinding(String key) {
        throw RInternalError.shouldNotReachHere();
    }

    @Override
    public boolean bindingIsLocked(String key) {
        throw RInternalError.shouldNotReachHere();
    }
}
