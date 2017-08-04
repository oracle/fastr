/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 2014, Purdue University
 * Copyright (c) 2014, 2017, Oracle and/or its affiliates
 *
 * All rights reserved.
 */

package com.oracle.truffle.r.nodes.builtin.base;

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.instanceOf;
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
import com.oracle.truffle.api.nodes.LoopNode;
import com.oracle.truffle.api.profiles.LoopConditionProfile;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.r.nodes.access.variables.ReadVariableNode;
import com.oracle.truffle.r.nodes.access.vector.ElementAccessMode;
import com.oracle.truffle.r.nodes.access.vector.ExtractVectorNode;
import com.oracle.truffle.r.nodes.access.vector.ExtractVectorNodeGen;
import com.oracle.truffle.r.nodes.attributes.SpecialAttributesFunctions.GetNamesAttributeNode;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.builtin.base.LapplyNodeGen.LapplyInternalNodeGen;
import com.oracle.truffle.r.nodes.control.RLengthNode;
import com.oracle.truffle.r.nodes.function.RCallBaseNode;
import com.oracle.truffle.r.nodes.function.RCallNode;
import com.oracle.truffle.r.runtime.ArgumentsSignature;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.RSource;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.env.frame.FrameSlotChangeMonitor;
import com.oracle.truffle.r.runtime.nodes.InternalRSyntaxNodeChildren;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;
import com.oracle.truffle.r.runtime.nodes.RNode;
import com.oracle.truffle.r.runtime.nodes.RSourceSectionNode;
import com.oracle.truffle.r.runtime.nodes.RSyntaxCall;
import com.oracle.truffle.r.runtime.nodes.RSyntaxElement;
import com.oracle.truffle.r.runtime.nodes.RSyntaxLookup;
import com.oracle.truffle.r.runtime.nodes.RSyntaxNode;

/**
 * The {@code lapply} builtin. {@code lapply} is an important implicit iterator in R. This
 * implementation handles the general case, but there are opportunities for "specializations" that
 * rewrite simple uses directly down to, e.g. explicit loops using, for example, a {@link LoopNode}.
 *
 * See the comment in {@link VApply} regarding "...".
 */
@RBuiltin(name = "lapply", kind = INTERNAL, parameterNames = {"X", "FUN"}, splitCaller = true, behavior = COMPLEX)
public abstract class Lapply extends RBuiltinNode.Arg2 {

    private static final Source CALL_SOURCE = RSource.fromTextInternal("FUN(X[[i]], ...)", RSource.Internal.LAPPLY);

    @Child private LapplyInternalNode lapply = LapplyInternalNodeGen.create();

    static {
        Casts casts = new Casts(Lapply.class);
        // to make conversion of X parameter 100% correct, we'd need to match semantics of
        // asVector() to whatever GNU R is doing there; still this can be a problem only if the
        // internal is called directly (otherwise, it's guaranteed that it's a vector)
        casts.arg("X").returnIf(instanceOf(RAbstractVector.class).not()).asVector(false);
        casts.arg("FUN").mustBe(instanceOf(RFunction.class), RError.Message.APPLY_NON_FUNCTION);
    }

    @Specialization
    protected Object lapply(VirtualFrame frame, RAbstractVector vec, RFunction fun,
                    @Cached("create()") GetNamesAttributeNode getNamesNode) {
        Object[] result = lapply.execute(frame, vec, fun);
        // set here else it gets overridden by the iterator evaluation
        return RDataFactory.createList(result, getNamesNode.getNames(vec));
    }

    @Specialization
    protected Object lapply(VirtualFrame frame, Object x, RFunction fun) {
        Object[] result = lapply.execute(frame, x, fun);
        return RDataFactory.createList(result);
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
            try {
                return extractElementNode.apply(frame, FrameSlotChangeMonitor.getObject(vectorSlot, frame), new Object[]{frame.getInt(indexSlot)}, RRuntime.LOGICAL_TRUE, RRuntime.LOGICAL_TRUE);
            } catch (FrameSlotTypeException e) {
                CompilerDirectives.transferToInterpreter();
                throw RInternalError.shouldNotReachHere("frame type mismatch in lapply");
            }
        }

        @Override
        public RSyntaxElement getSyntaxLHS() {
            return RSyntaxLookup.createDummyLookup(LAZY_DEPARSE, "[[", true);
        }

        @Override
        public ArgumentsSignature getSyntaxSignature() {
            return ArgumentsSignature.empty(2);
        }

        @Override
        public RSyntaxElement[] getSyntaxArguments() {
            return new RSyntaxElement[]{RSyntaxLookup.createDummyLookup(LAZY_DEPARSE, "X", false), RSyntaxLookup.createDummyLookup(LAZY_DEPARSE, "i", false)};
        }
    }

    public abstract static class LapplyInternalNode extends RBaseNode implements InternalRSyntaxNodeChildren {

        protected static final String INDEX_NAME = "i";
        protected static final String VECTOR_NAME = "X";

        public abstract Object[] execute(VirtualFrame frame, Object vector, RFunction function);

        protected static FrameSlot createIndexSlot(Frame frame) {
            return FrameSlotChangeMonitor.findOrAddFrameSlot(frame.getFrameDescriptor(), INDEX_NAME, FrameSlotKind.Int);
        }

        protected static FrameSlot createVectorSlot(Frame frame) {
            return FrameSlotChangeMonitor.findOrAddFrameSlot(frame.getFrameDescriptor(), VECTOR_NAME, FrameSlotKind.Object);
        }

        @Specialization
        protected Object[] cachedLApply(VirtualFrame frame, Object vector, RFunction function,
                        @Cached("createIndexSlot(frame)") FrameSlot indexSlot,
                        @Cached("createVectorSlot(frame)") FrameSlot vectorSlot,
                        @Cached("create()") RLengthNode lengthNode,
                        @Cached("createCountingProfile()") LoopConditionProfile loop,
                        @Cached("createCallNode(vectorSlot, indexSlot)") RCallBaseNode firstCallNode,
                        @Cached("createCallNode(vectorSlot, indexSlot)") RCallBaseNode callNode) {
            // TODO: R switches to double if x.getLength() is greater than 2^31-1
            FrameSlotChangeMonitor.setObject(frame, vectorSlot, vector);
            int length = lengthNode.executeInteger(frame, vector);
            Object[] result = new Object[length];
            if (length > 0) {
                reportWork(this, length);
                loop.profileCounted(length);
                frame.setInt(indexSlot, 1);
                result[0] = firstCallNode.execute(frame, function);
                for (int i = 2; loop.inject(i <= length); i++) {
                    frame.setInt(indexSlot, i);
                    result[i - 1] = callNode.execute(frame, function);
                }
            }
            return result;
        }

        /**
         * Creates the {@link RCallNode} for this target and {@code varArgs}.
         */
        protected RCallBaseNode createCallNode(FrameSlot vectorSlot, FrameSlot indexSlot) {
            CompilerAsserts.neverPartOfCompilation();

            ExtractElementInternal element = new ExtractElementInternal(vectorSlot, indexSlot);
            RSyntaxNode readArgs = ReadVariableNode.wrap(RSyntaxNode.LAZY_DEPARSE, ReadVariableNode.createSilent(ArgumentsSignature.VARARG_NAME, RType.Any));
            RNode function = RContext.getASTBuilder().lookup(RSyntaxNode.LAZY_DEPARSE, "FUN", false).asRNode();

            return RCallNode.createCall(createCallSourceSection(), function, ArgumentsSignature.get(null, "..."), element, readArgs);
        }
    }

    static SourceSection createCallSourceSection() {
        return CALL_SOURCE.createSection(0, CALL_SOURCE.getLength());
    }
}
