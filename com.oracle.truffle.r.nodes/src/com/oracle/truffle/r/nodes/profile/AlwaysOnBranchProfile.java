/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.profile;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.profiles.BranchProfile;

/**
 * {@link BranchProfile} implementation that is always enabled independent of the runtime. Useful if
 * whether or not a profile is {@link AlwaysOnBranchProfile#isVisited() visited} needs to be
 * queried.
 *
 * @see AlwaysOnBranchProfile#enter()
 */
public final class AlwaysOnBranchProfile {

    @CompilationFinal private boolean visited;

    AlwaysOnBranchProfile() {
    }

    /**
     * Call when an unlikely branch is entered.
     */
    public void enter() {
        if (!visited) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            visited = true;
        }
    }

    public boolean isVisited() {
        return visited;
    }

    /**
     * Call to create a new instance of a branch profile.
     */
    public static AlwaysOnBranchProfile create() {
        return new AlwaysOnBranchProfile();
    }
}
