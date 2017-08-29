/*
 * Copyright (c) 2014, 2017, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.ffi.impl.nodes;

import java.util.function.Supplier;

import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.r.ffi.impl.nodes.ListAccessNodesFactory.CADDRNodeGen;
import com.oracle.truffle.r.ffi.impl.nodes.ListAccessNodesFactory.CADRNodeGen;
import com.oracle.truffle.r.ffi.impl.nodes.ListAccessNodesFactory.CARNodeGen;
import com.oracle.truffle.r.ffi.impl.nodes.ListAccessNodesFactory.CDDRNodeGen;
import com.oracle.truffle.r.ffi.impl.nodes.ListAccessNodesFactory.CDRNodeGen;
import com.oracle.truffle.r.ffi.impl.nodes.MiscNodesFactory.LENGTHNodeGen;
import com.oracle.truffle.r.ffi.impl.nodes.MiscNodesFactory.RDoNewObjectNodeGen;
import com.oracle.truffle.r.ffi.impl.nodes.MiscNodesFactory.RDoSlotAssignNodeGen;
import com.oracle.truffle.r.ffi.impl.nodes.MiscNodesFactory.RDoSlotNodeGen;
import com.oracle.truffle.r.ffi.impl.nodes.RandFunctionsNodesFactory.RandFunction2NodeGen;
import com.oracle.truffle.r.ffi.impl.nodes.RandFunctionsNodesFactory.RandFunction3_1NodeGen;
import com.oracle.truffle.r.ffi.impl.nodes.RandFunctionsNodesFactory.RandFunction3_2NodeGen;
import com.oracle.truffle.r.ffi.impl.upcalls.RFFIUpCallTable;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.nmath.distr.Unif;
import com.oracle.truffle.r.runtime.nodes.RSyntaxNode;

public final class FFIUpCallRootNode extends RootNode {
    private static final RootCallTarget[] rootCallTargets = new RootCallTarget[RFFIUpCallTable.values().length];

    @Child private FFIUpCallNode theFFIUpCallNode;
    private final int numArgs;

    private FFIUpCallRootNode(FFIUpCallNode child) {
        super(RContext.getInstance().getLanguage());
        theFFIUpCallNode = child;
        this.numArgs = child.numArgs();
    }

    @Override
    public SourceSection getSourceSection() {
        return RSyntaxNode.INTERNAL;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        Object[] args = frame.getArguments();
        switch (numArgs) {
            case 0:
                return ((FFIUpCallNode.Arg0) theFFIUpCallNode).executeObject();
            case 1:
                return ((FFIUpCallNode.Arg1) theFFIUpCallNode).executeObject(args[0]);
            case 2:
                return ((FFIUpCallNode.Arg2) theFFIUpCallNode).executeObject(args[0], args[1]);
            case 3:
                return ((FFIUpCallNode.Arg3) theFFIUpCallNode).executeObject(args[0], args[1], args[2]);
            default:
                throw RInternalError.shouldNotReachHere();
        }
    }

    static void add(RFFIUpCallTable upCallMethod, Supplier<FFIUpCallNode> constructor) {

        FFIUpCallRootNode rootNode = new FFIUpCallRootNode(constructor.get());
        rootCallTargets[upCallMethod.ordinal()] = Truffle.getRuntime().createCallTarget(rootNode);
    }

    public static RootCallTarget getCallTarget(RFFIUpCallTable upCallMethod) {
        RootCallTarget target = rootCallTargets[upCallMethod.ordinal()];
        assert target != null;
        return target;
    }

    public static void register() {
        FFIUpCallRootNode.add(RFFIUpCallTable.ATTRIB, AttributesAccessNodesFactory.ATTRIBNodeGen::create);
        FFIUpCallRootNode.add(RFFIUpCallTable.Rf_asReal, AsRealNodeGen::create);
        FFIUpCallRootNode.add(RFFIUpCallTable.Rf_asLogical, AsLogicalNodeGen::create);
        FFIUpCallRootNode.add(RFFIUpCallTable.Rf_asInteger, AsIntegerNodeGen::create);
        FFIUpCallRootNode.add(RFFIUpCallTable.Rf_asChar, AsCharNodeGen::create);
        FFIUpCallRootNode.add(RFFIUpCallTable.Rf_coerceVector, CoerceVectorNode::create);
        FFIUpCallRootNode.add(RFFIUpCallTable.CAR, CARNodeGen::create);
        FFIUpCallRootNode.add(RFFIUpCallTable.CDR, CDRNodeGen::create);
        FFIUpCallRootNode.add(RFFIUpCallTable.CADR, CADRNodeGen::create);
        FFIUpCallRootNode.add(RFFIUpCallTable.CADDR, CADDRNodeGen::create);
        FFIUpCallRootNode.add(RFFIUpCallTable.CDDR, CDDRNodeGen::create);
        FFIUpCallRootNode.add(RFFIUpCallTable.LENGTH, LENGTHNodeGen::create);
        FFIUpCallRootNode.add(RFFIUpCallTable.R_do_new_object, RDoNewObjectNodeGen::create);
        FFIUpCallRootNode.add(RFFIUpCallTable.R_do_slot, RDoSlotNodeGen::create);
        FFIUpCallRootNode.add(RFFIUpCallTable.R_do_slot_assign, RDoSlotAssignNodeGen::create);
        FFIUpCallRootNode.add(RFFIUpCallTable.Rf_runif, () -> RandFunction2NodeGen.create(new Unif.Runif()));
        FFIUpCallRootNode.add(RFFIUpCallTable.Rf_dunif, () -> RandFunction3_1NodeGen.create(new Unif.DUnif()));
        FFIUpCallRootNode.add(RFFIUpCallTable.Rf_qunif, () -> RandFunction3_2NodeGen.create(new Unif.QUnif()));
        FFIUpCallRootNode.add(RFFIUpCallTable.Rf_punif, () -> RandFunction3_2NodeGen.create(new Unif.PUnif()));
        FFIUpCallRootNode.add(RFFIUpCallTable.Rf_namesgets, MiscNodesFactory.NamesGetsNodeGen::create);
        FFIUpCallRootNode.add(RFFIUpCallTable.TAG, AttributesAccessNodesFactory.TAGNodeGen::create);
        FFIUpCallRootNode.add(RFFIUpCallTable.Rf_copyMostAttrib, AttributesAccessNodesFactory.CopyMostAttribNodeGen::create);
    }
}
