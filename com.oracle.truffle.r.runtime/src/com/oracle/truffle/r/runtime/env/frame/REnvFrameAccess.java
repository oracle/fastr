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
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.env.REnvironment;
import com.oracle.truffle.r.runtime.env.REnvironment.PutException;

/**
 * Access to the frame component, handled by delegation in {@link REnvironment}. The default
 * implementation throws an exception for all calls.
 */
public abstract class REnvFrameAccess {
    /**
     * Return the value of object named {@code name} or {@code null} if not found.
     */
    public abstract Object get(String key);

    /**
     * Set the value of object named {@code name} to {@code value}. if {@code value == null},
     * effectively removes the name.
     *
     * @throws PutException if the binding is locked
     */
    public abstract void put(String key, Object value) throws REnvironment.PutException;

    /**
     * Remove binding.
     */
    public abstract void rm(String key) throws PutException;

    /**
     * Return the names in the environment that match {@code pattern}.
     *
     * @param allNames if {@code false} ignore names beginning with ".".
     * @param pattern if not {@code null} only include names matching {@code pattern}.
     * @param sorted TODO
     */
    public abstract RStringVector ls(boolean allNames, Pattern pattern, boolean sorted);

    public abstract void lockBindings();

    /**
     * Disallow updates to {@code key}.
     */
    public abstract void lockBinding(String key);

    /**
     * Allow updates to (previously locked) {@code key}.
     */
    public abstract void unlockBinding(String key);

    public abstract boolean bindingIsLocked(String key);

    public abstract MaterializedFrame getFrame();

}
