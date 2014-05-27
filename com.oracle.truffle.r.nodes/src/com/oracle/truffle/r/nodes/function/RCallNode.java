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
package com.oracle.truffle.r.nodes.function;

import java.lang.reflect.*;
import java.util.*;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.impl.*;
import com.oracle.truffle.api.nodes.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.access.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;

public abstract class RCallNode extends RNode {

    private static final int INLINE_CACHE_SIZE = 4;

    protected RCallNode() {
    }

    @Override
    public Object execute(VirtualFrame frame) {
        throw new AssertionError();
    }

    protected void onCreate() {
        // intended for subclasses to be used to implement special inlining semantics.
    }

    public abstract Object execute(VirtualFrame frame, RFunction function);

    public int executeInteger(VirtualFrame frame, RFunction function) throws UnexpectedResultException {
        return RTypesGen.RTYPES.expectInteger(execute(frame, function));
    }

    public double executeDouble(VirtualFrame frame, RFunction function) throws UnexpectedResultException {
        return RTypesGen.RTYPES.expectDouble(execute(frame, function));
    }

    public static RCallNode createStaticCall(String function, CallArgumentsNode arguments) {
        return RCallNode.createCall(ReadVariableNode.create(function, RRuntime.TYPE_FUNCTION, false), arguments);
    }

    public static RCallNode createStaticCall(SourceSection src, String function, CallArgumentsNode arguments) {
        RCallNode cn = createStaticCall(function, arguments);
        cn.assignSourceSection(src);
        return cn;
    }

    public static RCallNode createCall(RNode function, CallArgumentsNode arguments) {
        return new UninitializedCallNode(function, arguments);
    }

    public static RCallNode createCall(SourceSection src, RNode function, CallArgumentsNode arguments) {
        RCallNode cn = new UninitializedCallNode(function, arguments);
        cn.assignSourceSection(src);
        return cn;
    }

    private static RBuiltinRootNode findBuiltinRootNode(RootCallTarget callTarget) {
        RootNode root = callTarget.getRootNode();
        if (root instanceof RBuiltinRootNode) {
            return (RBuiltinRootNode) root;
        }
        return null;
    }

    private abstract static class RootCallNode extends RCallNode {

        @Child protected RNode functionNode;

        public RootCallNode(RNode function) {
            this.functionNode = function;
        }

        private RFunction executeFunctionNode(VirtualFrame frame) {
            try {
                return functionNode.executeFunction(frame);
            } catch (UnexpectedResultException e) {
                // TODO unsupported yet
                throw new UnsupportedOperationException();
            }
        }

        @Override
        public final Object execute(VirtualFrame frame) {
            return execute(frame, executeFunctionNode(frame));
        }

        @Override
        public final int executeInteger(VirtualFrame frame) throws UnexpectedResultException {
            return executeInteger(frame, executeFunctionNode(frame));
        }

        @Override
        public final double executeDouble(VirtualFrame frame) throws UnexpectedResultException {
            return executeDouble(frame, executeFunctionNode(frame));
        }

    }

    public static final class CachedCallNode extends RootCallNode {

        @Child protected RootCallNode nextNode;
        @Child protected RCallNode currentNode;
        private final RFunction function;
        private final CallTarget target;

        public CachedCallNode(RNode function, RCallNode current, RootCallNode next, RFunction cachedFunction) {
            super(function);
            this.currentNode = current;
            this.nextNode = next;
            this.function = cachedFunction;
            this.target = cachedFunction.getTarget();
        }

        @Override
        public Object execute(VirtualFrame frame, RFunction f) {
            if (this.function == f) {
                return currentNode.execute(frame, this.function);
            }
            return nextNode.execute(frame, f);
        }

        @Override
        public int executeInteger(VirtualFrame frame, RFunction f) throws UnexpectedResultException {
            if (this.function == f) {
                return currentNode.executeInteger(frame, this.function);
            }
            return nextNode.executeInteger(frame, f);
        }

        @Override
        public double executeDouble(VirtualFrame frame, RFunction f) throws UnexpectedResultException {
            if (this.function == f) {
                return currentNode.executeDouble(frame, this.function);
            }
            return nextNode.executeDouble(frame, f);
        }

        CallTarget getCallTarget() {
            return target;
        }
    }

    private static final class UninitializedCallNode extends RootCallNode {

        @Child protected CallArgumentsNode args;
        protected final int depth;

        protected UninitializedCallNode(RNode function, CallArgumentsNode args) {
            this(function, args, 0);
        }

        private UninitializedCallNode(RNode function, CallArgumentsNode args, int depth) {
            super(function);
            this.args = args;
            this.depth = depth;
        }

        protected UninitializedCallNode(UninitializedCallNode copy) {
            this(copy, copy.depth + 1);
        }

        private UninitializedCallNode(UninitializedCallNode copy, int depth) {
            super(null);
            this.args = copy.args;
            this.depth = depth;
        }

        @Override
        public Object execute(VirtualFrame frame, RFunction function) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            return specialize(function).execute(frame, function);
        }

        private RCallNode specialize(RFunction function) {
            CompilerAsserts.neverPartOfCompilation();

            if (depth < INLINE_CACHE_SIZE) {
                final RCallNode current = createCacheNode(function);
                final RootCallNode cachedNode = new CachedCallNode(this.functionNode, current, new UninitializedCallNode(this), function);
                current.onCreate();
                this.replace(cachedNode);
                return cachedNode;
            } else {
                RootCallNode topMost = (RootCallNode) getTopNode();
                GenericCallNode generic = topMost.replace(new GenericCallNode(topMost.functionNode, args));
                generic.onCreate();
                return generic;
            }
        }

        protected Node getTopNode() {
            Node parentNode = this;
            for (int i = 0; i < depth; i++) {
                parentNode = parentNode.getParent();
            }
            return parentNode;
        }

        protected RCallNode createCacheNode(RFunction function) {
            CallArgumentsNode clonedArgs = NodeUtil.cloneNode(args);
            clonedArgs = permuteArguments(function, clonedArgs, clonedArgs.getNames());

            if (function.isBuiltin()) {
                RootCallTarget callTarget = function.getTarget();
                RBuiltinRootNode root = findBuiltinRootNode(callTarget);
                if (root != null) {
                    return root.inline(clonedArgs);
                }
            }

            return new DispatchedCallNode(function, clonedArgs);
        }

        private CallArgumentsNode permuteArguments(RFunction function, CallArgumentsNode arguments, Object[] actualNames) {
            RRootNode rootNode = (RRootNode) function.getTarget().getRootNode();
            final boolean isBuiltin = rootNode instanceof RBuiltinRootNode;
            final boolean hasVarArgs = Arrays.asList(rootNode.getParameterNames()).contains("...");
            if (!isBuiltin && !hasVarArgs && arguments.getArguments().length > rootNode.getParameterCount()) {
                RNode unusedArgNode = arguments.getArguments()[rootNode.getParameterCount()];
                CompilerDirectives.transferToInterpreter();
                throw RError.getUnusedArgument(getEncapsulatingSourceSection(), unusedArgNode.getSourceSection().getCode());
            }
            if (arguments.getNameCount() != 0 || hasVarArgs) {
                RNode[] permuted = permuteArguments(arguments.getArguments(), rootNode.getParameterNames(), actualNames, new VarArgsAsObjectArrayNodeFactory());
                if (!isBuiltin) {
                    for (int i = 0; i < permuted.length; i++) {
                        if (permuted[i] == null) {
                            permuted[i] = ConstantNode.create(RMissing.instance);
                        }
                    }
                }
                return CallArgumentsNode.create(permuted, arguments.getNames());
            }
            return arguments;
        }
    }

    private static class DispatchedCallNode extends RCallNode {

        @Child protected CallArgumentsNode arguments;
        @Child protected DirectCallNode call;

        protected final RFunction function;

        DispatchedCallNode(RFunction function, CallArgumentsNode arguments) {
            this.arguments = arguments;
            this.function = function;
            this.call = Truffle.getRuntime().createDirectCallNode(function.getTarget());
        }

        @Override
        public Object execute(VirtualFrame frame, RFunction evaluatedFunction) {
            Object[] argsObject = RArguments.create(function, arguments.executeArray(frame));
            return call.call(frame, argsObject);
        }

    }

    private static final class GenericCallNode extends RootCallNode {

        @Child protected CallArgumentsNode arguments;
        @Child protected IndirectCallNode indirectCall = Truffle.getRuntime().createIndirectCallNode();

        GenericCallNode(RNode functionNode, CallArgumentsNode arguments) {
            super(functionNode);
            this.arguments = arguments;
        }

        @Override
        public Object execute(VirtualFrame frame, RFunction function) {
            Object[] argsObject = RArguments.create(function, permuteArguments(function, arguments.executeArray(frame), arguments.getNames()));
            return indirectCall.call(frame, function.getTarget(), argsObject);
        }

        private Object[] permuteArguments(RFunction function, Object[] evaluatedArgs, Object[] actualNames) {
            if (arguments.getNameCount() == 0) {
                return evaluatedArgs;
            }
            return permuteArguments(evaluatedArgs, ((RRootNode) ((DefaultCallTarget) function.getTarget()).getRootNode()).getParameterNames(), actualNames, new VarArgsAsObjectArrayFactory());
        }
    }

    public interface VarArgsFactory<T> {
        T makeList(T[] elements, String[] names);
    }

    public static final class VarArgsAsListFactory implements VarArgsFactory<Object> {
        public Object makeList(final Object[] elements, final String[] names) {
            RList argList = RDataFactory.createList(elements);
            if (names != null) {
                argList.setNames(RDataFactory.createStringVector(names, true));
            }
            return argList;
        }
    }

    public static final class VarArgsAsObjectArrayFactory implements VarArgsFactory<Object> {
        public Object makeList(final Object[] elements, final String[] names) {
            if (elements.length > 1) {
                return elements;
            } else if (elements.length == 1) {
                return elements[0];
            } else {
                return RMissing.instance;
            }
        }
    }

    public static final class VarArgsAsListNodeFactory implements VarArgsFactory<RNode> {
        public RNode makeList(final RNode[] elements, final String[] names) {
            if (elements.length > 1) {
                return new VarArgsAsListNode(elements, names);
            } else if (elements.length == 1) {
                return elements[0];
            } else {
                return ConstantNode.create(RMissing.instance);
            }
        }
    }

    public abstract static class VarArgsNode extends RNode {
        @Children protected final RNode[] elementNodes;

        protected VarArgsNode(RNode[] elements) {
            elementNodes = elements;
        }

        public final RNode[] getArgumentNodes() {
            return elementNodes;
        }
    }

    private static final class VarArgsAsListNode extends VarArgsNode {
        private final String[] names;

        private VarArgsAsListNode(RNode[] elements, String[] names) {
            super(elements);
            this.names = names;
        }

        @Override
        public RList execute(VirtualFrame frame) {
            Object[] evaluatedElements = new Object[elementNodes.length];
            if (elementNodes.length > 0) {
                executeElementNodes(frame, elementNodes, evaluatedElements);
            }
            RList argList = RDataFactory.createList(evaluatedElements);
            if (names != null) {
                argList.setNames(RDataFactory.createStringVector(names, true));
            }
            return argList;
        }
    }

    public static final class VarArgsAsObjectArrayNodeFactory implements VarArgsFactory<RNode> {
        public RNode makeList(final RNode[] elements, final String[] names) {
            if (elements.length > 1) {
                return new VarArgsAsObjectArrayNode(elements);
            } else if (elements.length == 1) {
                return elements[0];
            } else {
                return ConstantNode.create(RMissing.instance);
            }
        }
    }

    private static final class VarArgsAsObjectArrayNode extends VarArgsNode {
        protected VarArgsAsObjectArrayNode(RNode[] elements) {
            super(elements);
        }

        @Override
        public Object[] execute(VirtualFrame frame) {
            Object[] evaluatedElements = new Object[elementNodes.length];
            if (elementNodes.length > 0) {
                executeElementNodes(frame, elementNodes, evaluatedElements);
            }
            return evaluatedElements;
        }
    }

    @ExplodeLoop
    protected static void executeElementNodes(VirtualFrame frame, RNode[] elementNodes, Object[] evaluatedElements) {
        for (int i = 0; i < elementNodes.length; i++) {
            evaluatedElements[i] = elementNodes[i].execute(frame);
        }
    }

    @SuppressWarnings("unchecked")
    protected <T> T[] permuteArguments(T[] arguments, Object[] parameterNames, Object[] actualNames, VarArgsFactory<T> listFactory) {
        int varArgIndex = Arrays.asList(parameterNames).indexOf("...");
        boolean hasVarArgs = varArgIndex != -1;
        boolean hasArgNodes = arguments.getClass() == RNode[].class;
        T[] resultArgs = (T[]) Array.newInstance(arguments.getClass().getComponentType(), hasVarArgs ? parameterNames.length : Math.max(parameterNames.length, arguments.length));
        BitSet matchedNames = new BitSet(actualNames.length);
        int unmatchedNameCount = 0;
        boolean[] matchedArgs = new boolean[parameterNames.length];
        for (int i = 0; i < actualNames.length; i++) {
            if (actualNames[i] != null) {
                RNode argNode = null;
                if (hasArgNodes) {
                    argNode = (RNode) arguments[i];
                }
                int parameterPosition = findParameterPosition(parameterNames, actualNames[i], matchedArgs, i, hasVarArgs, argNode);
                if (parameterPosition >= 0) {
                    resultArgs[parameterPosition] = arguments[i];
                    matchedNames.set(i);
                } else {
                    unmatchedNameCount++;
                }
            }
        }
        int varArgCount = arguments.length - varArgIndex - matchedNames.cardinality();
        if (varArgIndex >= 0 && varArgCount >= 0) {
            T[] varArgsArray = (T[]) Array.newInstance(arguments.getClass().getComponentType(), varArgCount);
            String[] namesArray = null;
            if (unmatchedNameCount != 0) {
                namesArray = new String[varArgCount];
            }
            int pos = 0;
            for (int i = varArgIndex; i < arguments.length; i++) {
                if (i > actualNames.length || !matchedNames.get(i)) {
                    varArgsArray[pos] = arguments[i];
                    if (namesArray != null) {
                        namesArray[pos] = actualNames[i] != null ? String.valueOf(actualNames[i]) : "";
                    }
                    pos++;
                }
            }
            resultArgs[varArgIndex] = listFactory.makeList(varArgsArray, namesArray);
        }
        int cursor = 0;
        for (int i = 0; i < resultArgs.length && (!hasVarArgs || i < varArgIndex); i++) {
            if (resultArgs[i] == null) {
                while (cursor < actualNames.length && matchedNames.get(cursor)) {
                    cursor++;
                }
                if (cursor < arguments.length) {
                    resultArgs[i] = arguments[cursor++];
                }
            }
        }
        return resultArgs;
    }

    private int findParameterPosition(Object[] parameterNames, Object actualName, boolean[] matchedArgs, int argPos, boolean varArgs, RNode argNode) {
        String name = RRuntime.toString(actualName);
        int found = -1;
        for (int i = 0; i < parameterNames.length; i++) {
            if (parameterNames[i] != null) {
                final String pn = RRuntime.toString(parameterNames[i]);
                if (pn.equals(name)) {
                    found = i;
                    if (matchedArgs[found]) {
                        CompilerDirectives.transferToInterpreter();
                        throw RError.getFormalMatchedMultiple(getEncapsulatingSourceSection(), pn);
                    }
                    matchedArgs[found] = true;
                    break;
                } else if (pn.startsWith(name)) {
                    if (found >= 0) {
                        CompilerDirectives.transferToInterpreter();
                        throw RError.getArgumentMatchesMultiple(getEncapsulatingSourceSection(), 1 + argPos);
                    }
                    found = i;
                    if (matchedArgs[found]) {
                        CompilerDirectives.transferToInterpreter();
                        throw RError.getFormalMatchedMultiple(getEncapsulatingSourceSection(), pn);
                    }
                    matchedArgs[found] = true;
                }
            }
        }
        if (found >= 0 || varArgs) {
            return found;
        }
        CompilerDirectives.transferToInterpreter();
        throw RError.getUnusedArgument(getEncapsulatingSourceSection(), argNode != null ? argNode.getSourceSection().getCode() : name);
    }
}
