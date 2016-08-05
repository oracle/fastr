/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 2014, Purdue University
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates
 *
 * All rights reserved.
 */

package com.oracle.truffle.r.nodes.builtin.base;

import static com.oracle.truffle.r.runtime.builtins.RBehavior.COMPLEX;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.INTERNAL;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
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
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.builtin.base.LapplyNodeGen.LapplyInternalNodeGen;
import com.oracle.truffle.r.nodes.control.RLengthNode;
import com.oracle.truffle.r.nodes.function.RCallNode;
import com.oracle.truffle.r.runtime.ArgumentsSignature;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.RSerialize.State;
import com.oracle.truffle.r.runtime.RSource;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RAttributeProfiles;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.nodes.InternalRSyntaxNodeChildren;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;
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
public abstract class Lapply extends RBuiltinNode {

    private static final Source CALL_SOURCE = RSource.fromTextInternal("FUN(X[[i]], ...)", RSource.Internal.LAPPLY);

    private final RAttributeProfiles attrProfiles = RAttributeProfiles.create();

    @Child private LapplyInternalNode lapply = LapplyInternalNodeGen.create();

    @Specialization
    protected Object lapply(VirtualFrame frame, RAbstractVector vec, RFunction fun) {
        Object[] result = lapply.execute(frame, vec, fun);
        // set here else it gets overridden by the iterator evaluation
        return RDataFactory.createList(result, vec.getNames(attrProfiles));
    }

    private static final class ExtractElementInternal extends RSourceSectionNode implements RSyntaxCall {

        protected ExtractElementInternal() {
            super(RSyntaxNode.LAZY_DEPARSE);
        }

        @Child private ExtractVectorNode extractElementNode = ExtractVectorNodeGen.create(ElementAccessMode.SUBSCRIPT, false);
        @CompilationFinal private FrameSlot vectorSlot;
        @CompilationFinal private FrameSlot indexSlot;

        @Override
        public Object execute(VirtualFrame frame) {
            if (vectorSlot == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                vectorSlot = frame.getFrameDescriptor().findFrameSlot("X");
                indexSlot = frame.getFrameDescriptor().findFrameSlot("i");
            }
            try {
                return extractElementNode.apply(frame, frame.getObject(vectorSlot), new Object[]{frame.getInt(indexSlot)}, RRuntime.LOGICAL_TRUE, RRuntime.LOGICAL_TRUE);
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

        @Override
        public void serializeImpl(State state) {
            throw RInternalError.shouldNotReachHere();
        }
    }

    public abstract static class LapplyInternalNode extends RBaseNode implements InternalRSyntaxNodeChildren {

        protected static final String INDEX_NAME = "i";
        protected static final String VECTOR_NAME = "X";

        public abstract Object[] execute(VirtualFrame frame, Object vector, RFunction function);

        protected static FrameSlot createIndexSlot(Frame frame) {
            return frame.getFrameDescriptor().findOrAddFrameSlot(INDEX_NAME, FrameSlotKind.Int);
        }

        protected static FrameSlot createVectorSlot(Frame frame) {
            return frame.getFrameDescriptor().findOrAddFrameSlot(VECTOR_NAME, FrameSlotKind.Object);
        }

        @Specialization
        protected Object[] cachedLApply(VirtualFrame frame, Object vector, RFunction function, //
                        @Cached("createIndexSlot(frame)") FrameSlot indexSlot, //
                        @Cached("createVectorSlot(frame)") FrameSlot vectorSlot, //
                        @Cached("create()") RLengthNode lengthNode, //
                        @Cached("createCountingProfile()") LoopConditionProfile loop, //
                        @Cached("createCallNode()") RCallNode firstCallNode, //
                        @Cached("createCallNode()") RCallNode callNode) {
            // TODO: R switches to double if x.getLength() is greater than 2^31-1
            frame.setObject(vectorSlot, vector);
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
        protected RCallNode createCallNode() {
            CompilerAsserts.neverPartOfCompilation();

            ExtractElementInternal element = new ExtractElementInternal();
            ReadVariableNode readArgs = ReadVariableNode.createSilent(ArgumentsSignature.VARARG_NAME, RType.Any);

            return RCallNode.createCall(createCallSourceSection(), ReadVariableNode.create("FUN"), ArgumentsSignature.get(null, "..."), element, readArgs);
        }
    }

    static SourceSection createCallSourceSection() {
        return CALL_SOURCE.createSection("", 0, CALL_SOURCE.getLength());
    }
}
