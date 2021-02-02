/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 3 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 3 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 3 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.oracle.truffle.r.nodes.helpers;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.profiles.ValueProfile;
import com.oracle.truffle.r.nodes.access.variables.LocalReadVariableNode;
import com.oracle.truffle.r.nodes.access.variables.ReadVariableNode;
import com.oracle.truffle.r.nodes.function.PromiseHelperNode;
import com.oracle.truffle.r.runtime.data.RPromise;
import com.oracle.truffle.r.runtime.env.REnvironment;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;

public abstract class GetFromEnvironment extends RBaseNode {

    public static GetFromEnvironment create() {
        return GetFromEnvironmentNodeGen.create();
    }

    protected final ValueProfile frameAccessProfile = ValueProfile.createClassProfile();
    protected final ValueProfile frameProfile = ValueProfile.createClassProfile();

    @Child private PromiseHelperNode promiseHelper;

    @CompilationFinal private boolean firstExecution = true;

    protected Object checkPromise(VirtualFrame frame, Object r, String identifier) {
        if (r instanceof RPromise) {
            if (firstExecution) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                firstExecution = false;
                return ReadVariableNode.evalPromiseSlowPathWithName(identifier, frame, (RPromise) r);
            }
            if (promiseHelper == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                promiseHelper = insert(new PromiseHelperNode());
            }
            return promiseHelper.visibleEvaluate(frame, (RPromise) r);
        } else {
            return r;
        }
    }

    public abstract Object execute(VirtualFrame frame, REnvironment env, String name);

    protected static LocalReadVariableNode createRead(String name) {
        return LocalReadVariableNode.create(name, true);
    }

    protected FrameDescriptor getFrameDescriptor(REnvironment env) {
        return frameProfile.profile(env.getFrame(frameAccessProfile)).getFrameDescriptor();
    }

    @Specialization(guards = {"getFrameDescriptor(env) == envDesc", "name.equals(read.getIdentifier())"})
    protected Object getCached(VirtualFrame frame, REnvironment env, @SuppressWarnings("unused") String name,
                    @Cached("env.getFrame().getFrameDescriptor()") @SuppressWarnings("unused") FrameDescriptor envDesc,
                    @Cached("createRead(name)") LocalReadVariableNode read) {
        return read.execute(frame, frameProfile.profile(env.getFrame(frameAccessProfile)));
    }

    @Specialization(replaces = "getCached")
    protected Object get(VirtualFrame frame, REnvironment env, String name) {
        return checkPromise(frame, env.get(name), name);
    }
}
