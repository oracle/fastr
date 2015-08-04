/*
 * Copyright (c) 2013, 2015, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.builtin;

import java.util.*;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.source.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.access.*;
import com.oracle.truffle.r.nodes.binary.*;
import com.oracle.truffle.r.nodes.function.*;
import com.oracle.truffle.r.nodes.unary.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.env.*;
import com.oracle.truffle.r.runtime.env.frame.*;

@NodeFields(value = {@NodeField(name = "builtin", type = RBuiltinFactory.class), @NodeField(name = "suppliedSignature", type = ArgumentsSignature.class)})
@NodeChild(value = "arguments", type = RNode[].class)
public abstract class RBuiltinNode extends RNode implements RSyntaxNode, VisibilityController {

    public abstract Object execute(VirtualFrame frame, Object... args);

    private static final CastNode[] EMPTY_CASTS_ARRAY = new CastNode[0];

    public final class CastBuilder {

        private CastNode[] casts = EMPTY_CASTS_ARRAY;

        private CastBuilder insert(int index, CastNode cast) {
            if (index >= casts.length) {
                casts = Arrays.copyOf(casts, index + 1);
            }
            if (casts[index] == null) {
                casts[index] = cast;
            } else {
                casts[index] = new ChainedCastNode(casts[index], cast);
            }
            return this;
        }

        public CastBuilder toVector(int index) {
            return toVector(index, false);
        }

        public CastBuilder toVector(int index, boolean nonVectorPreserved) {
            return insert(index, CastToVectorNodeGen.create(nonVectorPreserved));
        }

        public CastBuilder toInteger(int index) {
            return toInteger(index, false, false, false);
        }

        public CastBuilder toInteger(int index, boolean preserveNames, boolean dimensionsPreservation, boolean attrPreservation) {
            return insert(index, CastIntegerNodeGen.create(preserveNames, dimensionsPreservation, attrPreservation));
        }

        public CastBuilder toDouble(int index) {
            return toDouble(index, false, false, false);
        }

        public CastBuilder toDouble(int index, boolean preserveNames, boolean dimensionsPreservation, boolean attrPreservation) {
            return insert(index, CastDoubleNodeGen.create(preserveNames, dimensionsPreservation, attrPreservation));
        }

        public CastBuilder toLogical(int index) {
            return toLogical(index, false, false, false);
        }

        public CastBuilder toLogical(int index, boolean preserveNames, boolean dimensionsPreservation, boolean attrPreservation) {
            return insert(index, CastLogicalNodeGen.create(preserveNames, dimensionsPreservation, attrPreservation));
        }

        public CastBuilder toCharacter(int index) {
            return toCharacter(index, false, false, false, false);
        }

        public CastBuilder toCharacter(int index, boolean preserveNames, boolean dimensionsPreservation, boolean attrPreservation, boolean emptyVectorConvertedToNull) {
            return insert(index, CastStringNodeGen.create(preserveNames, dimensionsPreservation, attrPreservation, emptyVectorConvertedToNull));
        }

        public CastBuilder boxPrimitive(int index) {
            return insert(index, BoxPrimitiveNodeGen.create());
        }

        public CastBuilder custom(int index, CastNode cast) {
            return insert(index, cast);
        }

        public CastBuilder firstIntegerWithWarning(int index, int intNa, String name) {
            insert(index, CastIntegerNodeGen.create(false, false, false));
            return insert(index, FirstIntNode.createWithWarning(RError.Message.FIRST_ELEMENT_USED, name, intNa));
        }

        public CastBuilder convertToInteger(int index) {
            return insert(index, ConvertIntNodeGen.create());
        }

        public CastBuilder firstIntegerWithError(int index, RError.Message error, String name) {
            insert(index, CastIntegerNodeGen.create(false, false, false));
            return insert(index, FirstIntNode.createWithError(error, name));
        }

        public CastBuilder firstStringWithError(int index, RError.Message error, String name) {
            return insert(index, FirstStringNode.createWithError(error, name));
        }
    }

    protected void createCasts(@SuppressWarnings("unused") CastBuilder casts) {
        // nothing to do
    }

    @CreateCast("arguments")
    protected RNode[] castArguments(RNode[] arguments) {
        CastNode[] casts = getCasts();
        if (casts.length == 0) {
            return arguments;
        }
        RNode[] castArguments = arguments.clone();
        for (int i = 0; i < casts.length; i++) {
            if (casts[i] != null) {
                castArguments[i] = new ApplyCastNode(casts[i], castArguments[i]);
            }
        }
        return castArguments;
    }

    public CastNode[] getCasts() {
        CastBuilder builder = new CastBuilder();
        createCasts(builder);
        return builder.casts;
    }

    /**
     * @return This is the accessor to the 'suppliedArgsNames': The names that have been given to
     *         the arguments supplied to the current function call. These are in the order as they
     *         appear in the source, of course.
     */
    public abstract ArgumentsSignature getSuppliedSignature();

    /**
     * Implementation is generated by Truffle in the "Factory" class.
     */
    public abstract RBuiltinFactory getBuiltin();

    /**
     * Accessor to the Truffle-generated 'arguments' field, used by binary operators and such.<br/>
     * <strong>ATTENTION:</strong> For implementing default values, use
     * {@link #getDefaultParameterValues()}!!!
     *
     * @return The arguments this builtin has received
     */
    public abstract RNode[] getArguments();

    /**
     * Return the default values of the builtin's formal arguments. This is only valid for builtins
     * of {@link RBuiltinKind kind} PRIMITIVE or SUBSTITUTE. Only simple scalar constants and
     * {@link RMissing#instance}, {@link RNull#instance} and {@link RArgsValuesAndNames#EMPTY} are
     * allowed.
     */
    public Object[] getDefaultParameterValues() {
        return EMPTY_OBJECT_ARRAY;
    }

    private static RNode[] createAccessArgumentsNodes(RBuiltinDescriptor builtin) {
        int total = builtin.getSignature().getLength();
        RNode[] args = new RNode[total];
        for (int i = 0; i < total; i++) {
            args[i] = AccessArgumentNode.create(i);
        }
        return args;
    }

    static RootCallTarget createArgumentsCallTarget(RBuiltinFactory builtin) {
        CompilerAsserts.neverPartOfCompilation();

        // Create function initialization
        RNode[] argAccessNodes = createAccessArgumentsNodes(builtin);
        RBuiltinNode node = createNode(builtin, argAccessNodes.clone(), ArgumentsSignature.empty(argAccessNodes.length));

        assert builtin.getKind() != RBuiltinKind.INTERNAL || node.getDefaultParameterValues().length == 0 : "INTERNAL builtins do not need default values";
        FormalArguments formals = FormalArguments.createForBuiltin(node.getDefaultParameterValues(), node.getBuiltin().getSignature());
        for (RNode access : argAccessNodes) {
            ((AccessArgumentNode) access).setFormals(formals);
        }

        // Setup
        FrameDescriptor frameDescriptor = new FrameDescriptor();
        RBuiltinRootNode root = new RBuiltinRootNode(node, formals, frameDescriptor);
        FrameSlotChangeMonitor.initializeFunctionFrameDescriptor(frameDescriptor);
        return Truffle.getRuntime().createCallTarget(root);
    }

    public final RBuiltinNode inline(ArgumentsSignature signature, RNode[] args, SourceSection callSrc) {
        // static number of arguments
        RBuiltinNode node = createNode(getBuiltin(), args, signature);
        // TODO src is (very) occasionally null, which should not happen
        node.assignSourceSection(callSrc);
        return node;
    }

    protected final RBuiltin getRBuiltin() {
        return getRBuiltin(getClass());
    }

    private static RBuiltin getRBuiltin(Class<?> klass) {
        GeneratedBy generatedBy = klass.getAnnotation(GeneratedBy.class);
        if (generatedBy != null) {
            return generatedBy.value().getAnnotation(RBuiltin.class);
        } else {
            return null;
        }
    }

    private static RBuiltinNode createNode(RBuiltinFactory factory, RNode[] arguments, ArgumentsSignature signature) {
        assert signature != null : factory + " " + Arrays.toString(arguments);
        return factory.getConstructor().get(arguments, factory, signature);
    }

    @Override
    public void deparseImpl(RDeparse.State state) {
        assert getBuiltin().getKind() == RBuiltinKind.INTERNAL;
        state.append(".Internal(");
        state.append(getBuiltin().getName());
        // arguments; there is no CallArgumentsNode, so we create one to reuse the deparse code
        RSyntaxNode[] args = Arrays.copyOf(getArguments(), getArguments().length, RSyntaxNode[].class);
        RCallNode.deparseArguments(state, args, getSuppliedSignature());
        state.append(')');
    }

    @Override
    public void serializeImpl(RSerialize.State state) {
        throw RInternalError.unimplemented();
    }

    @Override
    public RSyntaxNode substituteImpl(REnvironment env) {
        throw RInternalError.unimplemented();
    }

    @Override
    public String toString() {
        return (getRBuiltin() == null ? getClass().getSimpleName() : getRBuiltin().name());
    }
}
