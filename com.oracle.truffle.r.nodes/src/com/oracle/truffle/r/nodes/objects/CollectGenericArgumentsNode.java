/*
 * Copyright (c) 2015, 2016, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.SlowPathException;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.nodes.access.variables.LocalReadVariableNode;
import com.oracle.truffle.r.nodes.function.ClassHierarchyScalarNode;
import com.oracle.truffle.r.nodes.function.ClassHierarchyScalarNodeGen;
import com.oracle.truffle.r.nodes.function.PromiseHelperNode;
import com.oracle.truffle.r.nodes.function.PromiseHelperNode.PromiseCheckHelperNode;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RPromise;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.RSymbol;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;

/*
 * Used to collect arguments of the generic function for S4 method dispatch. Modeled after {@link CollectArgumentsNode}.
 */
public abstract class CollectGenericArgumentsNode extends RBaseNode {

    // TODO: re-do with a multi-element cache? (list comparison will have some cost, though)

    @Children private final LocalReadVariableNode[] argReads;
    @Children private final ClassHierarchyScalarNode[] classHierarchyNodes;
    @Child private ClassHierarchyScalarNode classHierarchyNodeSlowPath;
    @Child private PromiseCheckHelperNode promiseHelper = new PromiseCheckHelperNode();

    private final ConditionProfile valueMissingProfile = ConditionProfile.createBinaryProfile();

    public abstract RStringVector execute(VirtualFrame frame, RList arguments, int argLength);

    protected CollectGenericArgumentsNode(Object[] arguments, int argLength) {
        LocalReadVariableNode[] reads = new LocalReadVariableNode[argLength];
        ClassHierarchyScalarNode[] hierarchyNodes = new ClassHierarchyScalarNode[argLength];
        for (int i = 0; i < argLength; i++) {
            RSymbol s = (RSymbol) arguments[i];
            reads[i] = LocalReadVariableNode.create(s.getName(), true);
            hierarchyNodes[i] = ClassHierarchyScalarNodeGen.create();
        }
        argReads = insert(reads);
        classHierarchyNodes = insert(hierarchyNodes);
    }

    @ExplodeLoop
    @Specialization(rewriteOn = SlowPathException.class)
    protected RStringVector combineCached(VirtualFrame frame, RList arguments, int argLength) throws SlowPathException {
        if (argLength != argReads.length) {
            throw new SlowPathException();
        }
        String[] result = new String[argReads.length];
        for (int i = 0; i < argReads.length; i++) {
            Object cachedId = argReads[i].getIdentifier();
            String id = ((RSymbol) (arguments.getDataAt(i))).getName();
            assert cachedId instanceof String && cachedId == ((String) cachedId).intern() && id == id.intern();
            if (cachedId != id) {
                throw new SlowPathException();
            }
            Object value = argReads[i].execute(frame);
            result[i] = valueMissingProfile.profile(value == null) ? "missing" : classHierarchyNodes[i].executeString(promiseHelper.checkEvaluate(frame, value));
        }
        return RDataFactory.createStringVector(result, RDataFactory.COMPLETE_VECTOR);
    }

    @Specialization
    protected RStringVector combine(VirtualFrame frame, RList arguments, int argLength) {
        return readFromMaterialized(frame.materialize(), arguments, argLength);
    }

    @TruffleBoundary
    private RStringVector readFromMaterialized(MaterializedFrame frame, RList arguments, int argLength) {
        CompilerAsserts.neverPartOfCompilation();
        classHierarchyNodeSlowPath = insert(ClassHierarchyScalarNodeGen.create());
        String[] result = new String[argLength];
        FrameDescriptor desc = frame.getFrameDescriptor();
        for (int i = 0; i < argLength; i++) {
            RSymbol s = (RSymbol) arguments.getDataAt(i);
            FrameSlot slot = desc.findFrameSlot(s.getName());
            if (slot == null) {
                result[i] = "missing";
            } else {
                Object value = frame.getValue(slot);
                if (value instanceof RPromise) {
                    value = PromiseHelperNode.evaluateSlowPath(null, (RPromise) value);
                }
                result[i] = classHierarchyNodeSlowPath.executeString(value);
            }
        }
        return RDataFactory.createStringVector(result, RDataFactory.COMPLETE_VECTOR);
    }
}
