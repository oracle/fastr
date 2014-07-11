/*
 * Copyright (c) 2013, 2014, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.nodes.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.access.*;
import com.oracle.truffle.r.nodes.function.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;

@NodeField(name = "builtin", type = RBuiltinFactory.class)
@NodeChild(value = "arguments", type = RNode[].class)
public abstract class RBuiltinNode extends RCallNode implements VisibilityController {

    public String getSourceCode() {
        return "<builtin>";
    }

    /**
     * Accessor to the Truffle-generated 'arguments' field, used by binary operators and such.<br/>
     * <strong>ATTENTION:</strong> For implementing default values, use
     * {@link #getParameterValues()}!!!
     *
     * @return The arguments this builtin has received
     */
    public abstract RNode[] getArguments();

    public abstract RBuiltinFactory getBuiltin();

    /**
     * @return The names of the builtin's formal arguments
     * @see #getParameterValues()
     */
    public Object[] getParameterNames() {
        return RArguments.EMPTY_OBJECT_ARRAY;
    }

    /**
     * @return The default values of the builin's formal arguments
     * @see #getParameterNames()
     */
    public RNode[] getParameterValues() {
        return RNode.EMTPY_RNODE_ARRAY;
    }

    @Override
    public final Object execute(VirtualFrame frame, RFunction function) {
        return execute(frame);
    }

    @Override
    public final int executeInteger(VirtualFrame frame, RFunction function) throws UnexpectedResultException {
        return executeInteger(frame);
    }

    @Override
    public final double executeDouble(VirtualFrame frame, RFunction function) throws UnexpectedResultException {
        return executeDouble(frame);
    }

    private static RNode[] createAccessArgumentsNodes(RBuiltinFactory builtin) {
        int total = builtin.getFactory().getExecutionSignature().size();
        RNode[] args = new RNode[total];
        for (int i = 0; i < total; i++) {
            args[i] = new AccessArgumentNode(i);
        }
        return args;
    }

    static RootCallTarget createArgumentsCallTarget(RBuiltinFactory builtin) {
        // Create function initialization
        RNode[] argAccessNodes = createAccessArgumentsNodes(builtin);
        RBuiltinNode node = createNode(builtin, argAccessNodes, null);

        // Create formal arguments
        String[] names = new String[node.getParameterNames().length];
        for (int i = 0; i < names.length; i++) {
            names[i] = RRuntime.toString(node.getParameterNames()[i]);
        }
        FormalArguments formals = FormalArguments.create(names, node.getParameterValues());

        // Setup
        RBuiltinRootNode root = new RBuiltinRootNode(node, formals, new FrameDescriptor());
        node.onCreate();
        return Truffle.getRuntime().createCallTarget(root);
    }

    public RCallNode inline(EvaluatedArguments args) {
        // static number of arguments
        RNode[] builtinArguments = inlineStaticArguments(args);
        // TODO Gero: Is use of getNames correct???
        RBuiltinNode builtin = createNode(getBuiltin(), builtinArguments, args.getNameCount() == 0 ? null : args.getNames());
        builtin.onCreate();
        return builtin;
    }

    protected RBuiltin getRBuiltin() {
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

    private static RBuiltinNode createNode(RBuiltinFactory factory, Object[] builtinArguments, String[] argNames) {
        RBuiltin rBuiltin = getRBuiltin(factory.getFactory().getClass());
        boolean isCombine = rBuiltin == null ? false : rBuiltin.isCombine();
        Object[] args = new Object[(isCombine ? 3 : 2) + factory.getConstantArguments().length];
        int index = 0;
        for (; index < factory.getConstantArguments().length; index++) {
            args[index] = factory.getConstantArguments()[index];
        }

        args[index++] = builtinArguments;
        args[index++] = factory;
        if (isCombine) {
            args[index++] = argNames;
        }

        return factory.getFactory().createNode(args);
    }

    protected RNode[] inlineStaticArguments(EvaluatedArguments args) {
        int signatureSize = getBuiltin().getFactory().getExecutionSignature().size();
        RNode[] children = new RNode[signatureSize];

        // Fill with already determined arguments..
        RNode[] evaledArgs = args.getEvaluatedArgs();
        int argsSize = evaledArgs.length;
        int di = Math.min(argsSize, signatureSize);
        System.arraycopy(args.getEvaluatedArgs(), 0, children, 0, di);

        // ...and the rest with RMissing
        for (; di < signatureSize; di++) {
            children[di] = ConstantNode.create(RMissing.instance);
        }

        return children;
    }

    /**
     * A wrapper builtin is a {@link RCustomBuiltinNode} that is able to create any arbitrary node
     * as builtin. It can be used as normal builtin. Implement {@link #createDelegate()} to create
     * that node. Warning: setting argument count is not yet implemented. set
     * {@link RBuiltin#lastParameterKind()} to varargs to get all arguments in a single node in the
     * arguments array.
     */
    // TODO support argument for number of arguments. Currently no arguments are passed
    // or in case of var args exactly one.
    public abstract static class RWrapperBuiltinNode extends RCustomBuiltinNode {

        @Child protected RNode delegate;

        public RWrapperBuiltinNode(RBuiltinNode prev) {
            super(prev);
        }

        protected abstract RNode createDelegate();

        @Override
        protected void onCreate() {
            delegate = insert(createDelegate());
        }

        @Override
        public Object execute(VirtualFrame frame) {
            return delegate.execute(frame);
        }

        @Override
        public Object[] executeArray(VirtualFrame frame) throws UnexpectedResultException {
            return delegate.executeArray(frame);
        }

        @Override
        public byte executeByte(VirtualFrame frame) throws UnexpectedResultException {
            return delegate.executeByte(frame);
        }

        @Override
        public double executeDouble(VirtualFrame frame) throws UnexpectedResultException {
            return delegate.executeDouble(frame);
        }

        @Override
        public RFunction executeFunction(VirtualFrame frame) throws UnexpectedResultException {
            return delegate.executeFunction(frame);
        }

        @Override
        public int executeInteger(VirtualFrame frame) throws UnexpectedResultException {
            return delegate.executeInteger(frame);
        }

        @Override
        public RNull executeNull(VirtualFrame frame) throws UnexpectedResultException {
            return delegate.executeNull(frame);
        }

        @Override
        public RDoubleVector executeRDoubleVector(VirtualFrame frame) throws UnexpectedResultException {
            return delegate.executeRDoubleVector(frame);
        }

        @Override
        public RIntVector executeRIntVector(VirtualFrame frame) throws UnexpectedResultException {
            return delegate.executeRIntVector(frame);
        }

    }

    public static class RCustomBuiltinNode extends RBuiltinNode {

        @Children protected final RNode[] arguments;

        private final RBuiltinFactory builtin;

        public RCustomBuiltinNode(RBuiltinNode prev) {
            this(prev.getArguments(), prev.getBuiltin());
        }

        public RCustomBuiltinNode(RNode[] arguments, RBuiltinFactory builtin) {
            this.arguments = arguments;
            this.builtin = builtin;
        }

        @Override
        public RNode[] getArguments() {
            return arguments;
        }

        @Override
        public RBuiltinFactory getBuiltin() {
            return builtin;
        }

        @Override
        public String getSourceCode() {
            return "<custom builtin>";
        }

    }

}
