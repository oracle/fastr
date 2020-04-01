/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.function.opt.eval;

import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.profiles.ValueProfile;
import com.oracle.truffle.r.runtime.DSLConfig;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;

/**
 * Evaluates the function call defined in {@code FunctionInfo} in the fast path.
 */
@ImportStatic(DSLConfig.class)
public abstract class AbstractCallInfoEvalNode extends RBaseNode {
    protected static final int CACHE_SIZE = 10;
    protected static final int MAX_ARITY = 10;

    protected final ValueProfile frameProfile = ValueProfile.createClassProfile();
    protected final ValueProfile frameAccessProfile = ValueProfile.createClassProfile();

    protected static ArgValueSupplierNode[] createArgValueSupplierNodes(int argLength, boolean cached) {
        ArgValueSupplierNode[] argValSupplierNodes = new ArgValueSupplierNode[argLength];
        for (int i = 0; i < argLength; i++) {
            argValSupplierNodes[i] = ArgValueSupplierNodeGen.create(cached);
        }
        return argValSupplierNodes;
    }

    protected static ArgValueSupplierNode[] createGenericArgValueSupplierNodes(int argLength) {
        return createArgValueSupplierNodes(argLength, false);
    }

}
