/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.engine.interop;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.interop.*;
import com.oracle.truffle.api.nodes.*;
import com.oracle.truffle.api.utilities.*;
import com.oracle.truffle.r.engine.*;
import com.oracle.truffle.r.nodes.builtin.base.InfixEmulationFunctions.AccessArraySubscriptBuiltin;
import com.oracle.truffle.r.nodes.builtin.base.InfixEmulationFunctionsFactory.AccessArraySubscriptBuiltinNodeGen;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;

public class VectorReadNode extends RootNode {

    @CompilationFinal private boolean lengthAccess;
    @Child private AccessArraySubscriptBuiltin builtin;

    private final BranchProfile intIndex = BranchProfile.create();
    private final BranchProfile doubleIndex = BranchProfile.create();
    private final BranchProfile longIndex = BranchProfile.create();

    public VectorReadNode() {
        super(TruffleRLanguage.class, null, null);
        this.lengthAccess = false;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        Object label = ForeignAccess.getArguments(frame).get(0);
        if (lengthAccess && !(label instanceof String)) {
            CompilerDirectives.transferToInterpreter();
            lengthAccess = false;
        }
        if (!lengthAccess && (label instanceof String)) {
            CompilerDirectives.transferToInterpreter();
            lengthAccess = true;
        }

        RAbstractVector arg = (RAbstractVector) ForeignAccess.getReceiver(frame);
        if (lengthAccess) {
            if (label.equals("length")) {
                return arg.getLength();
            } else {
                CompilerDirectives.transferToInterpreter();
                throw RInternalError.shouldNotReachHere("unknown message: " + label);
            }
        } else {
            if (builtin == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                builtin = insert(AccessArraySubscriptBuiltinNodeGen.create(null, null, null));
            }
            int index;
            if (label instanceof Integer) {
                intIndex.enter();
                index = (int) label;
            } else if (label instanceof Long) {
                longIndex.enter();
                index = (int) (long) label;
            } else if (label instanceof Double) {
                doubleIndex.enter();
                index = (int) (double) label;
            } else {
                CompilerDirectives.transferToInterpreter();
                throw RInternalError.shouldNotReachHere("invalid index type: " + label);
            }
            return builtin.execute(frame, arg, new RArgsValuesAndNames(new Object[]{index + 1}, ArgumentsSignature.empty(1)), RRuntime.LOGICAL_TRUE, RRuntime.LOGICAL_TRUE);
        }
    }

}
