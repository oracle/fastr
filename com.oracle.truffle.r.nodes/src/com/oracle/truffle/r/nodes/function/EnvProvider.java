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
package com.oracle.truffle.r.nodes.function;

import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;

/**
 * This class should prevent the unnecessary creation of a new {@link REnvironment} for different
 * {@link RPromise} that are created to be evaluated inside the same environment. An instance of
 * this class is shared between all promise factories and provides them with a - lazy created -
 * instance of the respective environment.<br/>
 */
public class EnvProvider {
    /**
     * The cached {@link REnvironment}.
     */
    private REnvironment env = null;

    /**
     * {@link #getREnvironmentFor(VirtualFrame)} expects to be called for the same frame in a row,
     * and if the {@link #env} is {@code null} or {@link #env} is not for the given frame, a new
     * {@link REnvironment} is created.
     *
     * @param frame The frame the {@link REnvironment} should be retrieved for
     * @return An - maybe cached - instance of {@link REnvironment} for the given frame
     * @see EnvProvider
     */
    public REnvironment getREnvironmentFor(VirtualFrame frame) {
        if (env == null || env.getFrame() != frame) {
            env = REnvironment.frameToEnvironment(frame.materialize());
        }

        return env;
    }
}
