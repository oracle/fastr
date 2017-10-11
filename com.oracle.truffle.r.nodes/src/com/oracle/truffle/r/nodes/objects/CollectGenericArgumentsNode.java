/*
 * Copyright (c) 2015, 2017, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.objects;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.SlowPathException;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.nodes.function.ClassHierarchyScalarNode;
import com.oracle.truffle.r.nodes.function.ClassHierarchyScalarNodeGen;
import com.oracle.truffle.r.nodes.function.PromiseHelperNode;
import com.oracle.truffle.r.nodes.function.PromiseHelperNode.PromiseCheckHelperNode;
import com.oracle.truffle.r.runtime.RArguments;
import com.oracle.truffle.r.runtime.data.RArgsValuesAndNames;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.REmpty;
import com.oracle.truffle.r.runtime.data.RMissing;
import com.oracle.truffle.r.runtime.data.RPromise;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;

// transcribed from /src/library/methods/src/methods_list_dispatch.c (R_dispatchGeneric function)

/*
 * Used to collect arguments of the generic function for S4 method dispatch. Modeled after {@link CollectArgumentsNode}.
 * The way GnuR determines the classes of the arguments is by looking up the names of the formal arguments in the dispatching function.
 * However, the dispatching function may define default values for arguments that can change the signature of the actual arguments.
 * The function lookup must be done by using the original actual arguments (i.e. actual arguments without default values).
 * Since the arguments have already been matched and are ordered, we can just look at the arguments in the frame.
 * Varargs do not appear in the formal signature, therefore any vararg parameter must be skipped.
 */
public abstract class CollectGenericArgumentsNode extends RBaseNode {

    // TODO: re-do with a multi-element cache? (list comparison will have some cost, though)

    @Children private final ClassHierarchyScalarNode[] classHierarchyNodes;
    @Child private ClassHierarchyScalarNode classHierarchyNodeSlowPath;
    @Child private PromiseCheckHelperNode promiseHelper = new PromiseCheckHelperNode();

    private final int nProvidedArgs;

    private final ConditionProfile valueMissingProfile = ConditionProfile.createBinaryProfile();

    public abstract RStringVector execute(VirtualFrame frame, int argLength);

    protected CollectGenericArgumentsNode(int argLength) {
        ClassHierarchyScalarNode[] hierarchyNodes = new ClassHierarchyScalarNode[argLength];
        for (int i = 0; i < argLength; i++) {
            hierarchyNodes[i] = ClassHierarchyScalarNodeGen.create();
        }
        nProvidedArgs = argLength;
        classHierarchyNodes = hierarchyNodes;
    }

    @ExplodeLoop
    @Specialization(rewriteOn = SlowPathException.class)
    protected RStringVector combineCached(VirtualFrame frame, int argLength) throws SlowPathException {
        int nActualArgs = RArguments.getArgumentsLength(frame);
        if (argLength != nProvidedArgs || !(nActualArgs == nProvidedArgs || nActualArgs == nProvidedArgs + 1)) {
            throw new SlowPathException();
        }
        String[] result = new String[nProvidedArgs];

        // The length of the actual and formal arguments may not be equal because "..." is just
        // ignored in formals (i.e. '.SigArgs').
        assert nActualArgs == result.length || nActualArgs == result.length + 1;

        // Intentionally using 'i' as loop variable since nActualArgs >=
        // signatureArgumentNames.length
        int j = 0;
        for (int i = 0; i < nProvidedArgs; i++) {
            Object value = RArguments.getArgument(frame, j);
            if (value instanceof RArgsValuesAndNames) {
                j++;
                value = RArguments.getArgument(frame, j);
            }
            if (value == REmpty.instance || value == RMissing.instance) {
                value = null;
            }
            Object evaledArg = promiseHelper.checkEvaluate(frame, value);
            assert !(evaledArg instanceof RArgsValuesAndNames);
            result[i] = valueMissingProfile.profile(value == null) ? "missing" : classHierarchyNodes[i].executeString(evaledArg);
            j++;
        }
        return RDataFactory.createStringVector(result, RDataFactory.COMPLETE_VECTOR);
    }

    @Specialization
    protected RStringVector combine(VirtualFrame frame, int argLength) {
        return readFromMaterialized(frame.materialize(), argLength);
    }

    private RStringVector readFromMaterialized(MaterializedFrame frame, int argLength) {
        if (classHierarchyNodeSlowPath == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            classHierarchyNodeSlowPath = insert(ClassHierarchyScalarNodeGen.create());
        }

        int nActualArgs = RArguments.getArgumentsLength(frame);
        assert nActualArgs >= argLength;

        String[] result = new String[argLength];
        for (int j = 0, i = 0; i < argLength && j < nActualArgs; j++) {
            Object value = RArguments.getArgument(frame, j);
            if (value == REmpty.instance || value == RMissing.instance) {
                value = null;
            }
            if (!(value instanceof RArgsValuesAndNames)) {
                if (value instanceof RPromise) {
                    value = PromiseHelperNode.evaluateSlowPath((RPromise) value);
                }
                assert !(value instanceof RArgsValuesAndNames);
                result[i] = value == null ? "missing" : classHierarchyNodeSlowPath.executeString(value);
                i++;
            }
        }
        return RDataFactory.createStringVector(result, RDataFactory.COMPLETE_VECTOR);
    }
}
