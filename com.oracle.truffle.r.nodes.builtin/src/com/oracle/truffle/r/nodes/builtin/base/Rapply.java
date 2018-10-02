/*
 * Copyright (c) 1995-2015, The R Core Team
 * Copyright (c) 2016, 2018, Oracle and/or its affiliates
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, a copy is available at
 * https://www.R-project.org/Licenses/
 */
package com.oracle.truffle.r.nodes.builtin.base;

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.anyValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.constant;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.instanceOf;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.nullConstant;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.stringValue;
import static com.oracle.truffle.r.nodes.builtin.base.Lapply.createCallSourceSection;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.COMPLEX;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.INTERNAL;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.api.frame.FrameSlotTypeException;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.profiles.LoopConditionProfile;
import com.oracle.truffle.r.nodes.access.variables.ReadVariableNode;
import com.oracle.truffle.r.nodes.access.vector.ElementAccessMode;
import com.oracle.truffle.r.nodes.access.vector.ExtractVectorNode;
import com.oracle.truffle.r.nodes.access.vector.ExtractVectorNodeGen;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.builtin.base.RapplyNodeGen.RapplyInternalNodeGen;
import com.oracle.truffle.r.nodes.control.RLengthNode;
import com.oracle.truffle.r.nodes.function.RCallBaseNode;
import com.oracle.truffle.r.nodes.function.RCallNode;
import com.oracle.truffle.r.nodes.helpers.InheritsCheckNode;
import com.oracle.truffle.r.runtime.ArgumentsSignature;
import com.oracle.truffle.r.runtime.RArguments;
import com.oracle.truffle.r.runtime.RError.Message;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RListBase;
import com.oracle.truffle.r.runtime.data.model.RAbstractListVector;
import com.oracle.truffle.r.runtime.env.frame.FrameSlotChangeMonitor;
import com.oracle.truffle.r.runtime.nodes.InternalRSyntaxNodeChildren;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;
import com.oracle.truffle.r.runtime.nodes.RNode;
import com.oracle.truffle.r.runtime.nodes.RSourceSectionNode;
import com.oracle.truffle.r.runtime.nodes.RSyntaxCall;
import com.oracle.truffle.r.runtime.nodes.RSyntaxElement;
import com.oracle.truffle.r.runtime.nodes.RSyntaxLookup;
import com.oracle.truffle.r.runtime.nodes.RSyntaxNode;

@RBuiltin(name = "rapply", kind = INTERNAL, parameterNames = {"object", "f", "classes", "deflt", "how"}, splitCaller = true, behavior = COMPLEX)
public abstract class Rapply extends RBuiltinNode.Arg5 {

    @Child private RapplyInternalNode rapply = RapplyInternalNode.create();

    static {
        Casts casts = new Casts(Rapply.class);
        casts.arg("object").mustBe(RAbstractListVector.class, Message.GENERIC, "'object' must be a list");
        casts.arg("f").mustBe(instanceOf(RFunction.class));
        casts.arg("classes").mapNull(constant("ANY")).mapMissing(constant("ANY")).mustBe(stringValue()).asStringVector().findFirst().mustNotBeNA();
        casts.arg("deflt").allowNull().mapMissing(nullConstant()).mustBe(anyValue());
        casts.arg("how").mapNull(constant("unlist")).mapMissing(constant("unlist")).mustBe(stringValue()).asStringVector().findFirst().mustNotBeNA();
    }

    @Specialization
    protected Object rapply(VirtualFrame frame, RAbstractListVector object, RFunction f, String classes, Object deflt, String how) {

        RList result;
        if (rapply.isReplace(how)) {
            result = (RList) rapply.execute(frame, object, f, classes, deflt, how);
        } else {
            result = RDataFactory.createList((Object[]) rapply.execute(frame, object, f, classes, deflt, how));
            result.copyAttributesFrom(object);
        }
        return result;
    }

    private static final class ExtractElementInternal extends RSourceSectionNode implements RSyntaxCall {

        @Child private ExtractVectorNode extractElementNode = ExtractVectorNodeGen.create(ElementAccessMode.SUBSCRIPT, false);
        private final FrameSlot vectorSlot;
        private final FrameSlot indexSlot;

        protected ExtractElementInternal(FrameSlot vectorSlot, FrameSlot indexSlot) {
            super(RSyntaxNode.LAZY_DEPARSE);
            this.vectorSlot = vectorSlot;
            this.indexSlot = indexSlot;
        }

        @Override
        public Object execute(VirtualFrame frame) {
            RArguments.getCall(frame);
            try {
                return extractElementNode.apply(FrameSlotChangeMonitor.getObject(vectorSlot, frame), new Object[]{frame.getInt(indexSlot)}, RRuntime.LOGICAL_TRUE, RRuntime.LOGICAL_TRUE);
            } catch (FrameSlotTypeException e) {
                CompilerDirectives.transferToInterpreter();
                throw RInternalError.shouldNotReachHere("frame type mismatch in rapply");
            }
        }

        @Override
        public RSyntaxElement getSyntaxLHS() {
            return RSyntaxLookup.createDummyLookup(LAZY_DEPARSE, "list", true);
        }

        @Override
        public ArgumentsSignature getSyntaxSignature() {
            return ArgumentsSignature.empty(2);
        }

        @Override
        public RSyntaxElement[] getSyntaxArguments() {
            return new RSyntaxElement[]{RSyntaxLookup.createDummyLookup(LAZY_DEPARSE, "object", false), RSyntaxLookup.createDummyLookup(LAZY_DEPARSE, "i", false)};
        }
    }

    public abstract static class RapplyInternalNode extends RBaseNode implements InternalRSyntaxNodeChildren {

        @Child private InheritsCheckNode inhertsCheck;
        @Child private RapplyInternalNode rapply;

        protected static final String VECTOR_NAME = "object";
        protected static final String INDEX_NAME = "i";

        public abstract Object execute(VirtualFrame frame, RAbstractListVector object, RFunction f, String classes, Object deflt, String how);

        protected static FrameSlot createIndexSlot(Frame frame) {
            return FrameSlotChangeMonitor.findOrAddFrameSlot(frame.getFrameDescriptor(), INDEX_NAME, FrameSlotKind.Int);
        }

        protected static FrameSlot createVectorSlot(Frame frame) {
            return FrameSlotChangeMonitor.findOrAddFrameSlot(frame.getFrameDescriptor(), VECTOR_NAME, FrameSlotKind.Object);
        }

        @Specialization(guards = "isReplace(how)")
        protected RListBase cachedLapplyReplace(VirtualFrame frame, RAbstractListVector object, RFunction f, String classes, Object deflt, String how,
                        @Cached("createIndexSlot(frame)") FrameSlot indexSlot,
                        @Cached("createVectorSlot(frame)") FrameSlot vectorSlot,
                        @Cached("create()") RLengthNode lengthNode,
                        @Cached("createCountingProfile()") LoopConditionProfile loop,
                        @Cached("createCallNode(vectorSlot, indexSlot)") RCallBaseNode callNode) {

            int length = lengthNode.executeInteger(object);
            RListBase result = (RListBase) object.copy();
            inhertsCheck = InheritsCheckNode.createWithImplicit(classes);
            FrameSlotChangeMonitor.setObject(frame, vectorSlot, object);

            if (length > 0) {
                reportWork(this, length);
                loop.profileCounted(length);
                for (int i = 0; loop.inject(i < length); i++) {
                    frame.setInt(indexSlot, i + 1);
                    Object element = object.getDataAt(i);
                    if (element instanceof RAbstractListVector) {
                        rapply = RapplyInternalNodeGen.create();
                        result.setDataAt(i, rapply.execute(frame, (RAbstractListVector) element, f, classes, deflt, how));
                        FrameSlotChangeMonitor.setObject(frame, vectorSlot, object);
                    } else if (isRNull(element)) {
                        result.setDataAt(i, element);
                    } else if (classes.equals("ANY") || inhertsCheck.execute(element)) {
                        result.setDataAt(i, callNode.execute(frame, f));
                    } else {
                        result.setDataAt(i, element);
                    }
                }
            }
            return result;
        }

        @Specialization(guards = "!isReplace(how)")
        protected Object[] cachedLapply(VirtualFrame frame, RAbstractListVector object, RFunction f, String classes, Object deflt, String how,
                        @Cached("createIndexSlot(frame)") FrameSlot indexSlot,
                        @Cached("createVectorSlot(frame)") FrameSlot vectorSlot,
                        @Cached("create()") RLengthNode lengthNode,
                        @Cached("createCountingProfile()") LoopConditionProfile loop,
                        @Cached("createCallNode(vectorSlot, indexSlot)") RCallBaseNode callNode) {

            int length = lengthNode.executeInteger(object);
            Object[] result = new Object[length];
            inhertsCheck = InheritsCheckNode.createWithImplicit(classes);
            FrameSlotChangeMonitor.setObject(frame, vectorSlot, object);

            if (length > 0) {
                reportWork(this, length);
                loop.profileCounted(length);
                for (int i = 0; loop.inject(i < length); i++) {
                    frame.setInt(indexSlot, i + 1);
                    Object element = object.getDataAt(i);
                    if (element instanceof RAbstractListVector) {
                        rapply = RapplyInternalNodeGen.create();
                        RList newlist = RDataFactory.createList((Object[]) rapply.execute(frame, (RAbstractListVector) element, f, classes, deflt, how));
                        newlist.copyAttributesFrom((RAbstractListVector) element);
                        result[i] = newlist;
                        FrameSlotChangeMonitor.setObject(frame, vectorSlot, object);
                    } else if (isRNull(element)) {
                        result[i] = RDataFactory.createList();
                    } else if (classes.equals("ANY") || inhertsCheck.execute(element)) {
                        result[i] = callNode.execute(frame, f);
                    } else {
                        result[i] = deflt;
                    }
                }
            }
            return result;
        }

        protected RCallBaseNode createCallNode(FrameSlot vectorSlot, FrameSlot indexSlot) {
            CompilerAsserts.neverPartOfCompilation();

            ExtractElementInternal element = new ExtractElementInternal(vectorSlot, indexSlot);
            RSyntaxNode readArgs = ReadVariableNode.wrap(RSyntaxNode.LAZY_DEPARSE, ReadVariableNode.createSilent(ArgumentsSignature.VARARG_NAME, RType.Any));
            RNode function = RContext.getASTBuilder().lookup(RSyntaxNode.LAZY_DEPARSE, "f", false).asRNode();

            return RCallNode.createCall(createCallSourceSection(), function, ArgumentsSignature.get(null, "..."), element, readArgs);
        }

        protected boolean isReplace(String how) {
            return how.equals("replace");
        }

        public static RapplyInternalNode create() {
            return RapplyInternalNodeGen.create();
        }
    }

    public static Rapply create() {
        return RapplyNodeGen.create();
    }
}
