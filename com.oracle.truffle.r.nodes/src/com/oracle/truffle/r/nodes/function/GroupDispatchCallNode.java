/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 2014, Purdue University
 * Copyright (c) 2015, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.nodes.function;

import java.util.*;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.nodes.*;
import com.oracle.truffle.api.source.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.access.*;
import com.oracle.truffle.r.nodes.access.variables.*;
import com.oracle.truffle.r.nodes.runtime.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.RDeparse.State;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;
import com.oracle.truffle.r.runtime.env.*;
import com.oracle.truffle.r.runtime.env.frame.*;

import edu.umd.cs.findbugs.annotations.*;

public abstract class GroupDispatchCallNode extends RNode {

    private static final int INLINE_CACHE_SIZE = 4;
    @Child protected CallArgumentsNode callArgsNode;

    public static GroupDispatchCallNode create(String aGenericName, String groupName, CallArgumentsNode callArgNode, SourceSection callSrc) {
        GroupDispatchCallNode gdcn = new UninitializedGroupDispatchCallNode(aGenericName, groupName, callArgNode);
        gdcn.assignSourceSection(callSrc);
        return gdcn;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        throw new UnsupportedOperationException();
    }

    public abstract Object execute(VirtualFrame frame, RArgsValuesAndNames argAndNames);

    public abstract String getGenericName();

    public abstract String getGroupName();

    public abstract SourceSection getCallSrc();

    protected RArgsValuesAndNames evalArgs(VirtualFrame frame) {
        UnrolledVariadicArguments unrolledArgs = callArgsNode.executeFlatten(frame);
        RNode[] unevaledArgs = unrolledArgs.getArguments();
        Object[] evaledArgs = new Object[unevaledArgs.length];
        for (int i = 0; i < unevaledArgs.length; ++i) {
            if (unevaledArgs[i] != null) {
                evaledArgs[i] = unevaledArgs[i].execute(frame);
            }
        }
        // Delay assignment to allow recursion
        RArgsValuesAndNames argAndNames = new RArgsValuesAndNames(evaledArgs, unrolledArgs.getNames());
        return argAndNames;
    }

    @Override
    public boolean isSyntax() {
        return true;
    }

    @Override
    public void deparse(State state) {
        String name = this.getGenericName();
        RDeparse.Func func = RDeparse.getFunc(name);
        if (func != null) {
            // infix operator
            RASTDeparse.deparseInfixOperator(state, this, func);
        } else {
            state.append(name);
            this.callArgsNode.deparse(state);
        }
    }

    @Override
    public RNode substitute(REnvironment env) {
        // TODO substitute aDispatchNode
        return RASTUtils.createCall(this, (CallArgumentsNode) callArgsNode.substitute(env));
    }

    private static class UninitializedGroupDispatchCallNode extends GroupDispatchCallNode {

        private final String groupName;
        @CompilationFinal private final String genericName;
        private final int depth;

        public UninitializedGroupDispatchCallNode(final String aGenericName, final String groupName, final CallArgumentsNode callArgNode) {
            this.genericName = aGenericName;
            this.groupName = groupName;
            this.callArgsNode = callArgNode;
            this.depth = 0;
        }

        private UninitializedGroupDispatchCallNode(final UninitializedGroupDispatchCallNode copy, final int depth) {
            this.genericName = copy.genericName;
            this.groupName = copy.groupName;
            this.callArgsNode = copy.callArgsNode;
            this.depth = depth;
            this.assignSourceSection(copy.getSourceSection());
        }

        @Override
        public Object execute(VirtualFrame frame) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            RArgsValuesAndNames argAndNames = evalArgs(frame);
            return specialize(argAndNames).execute(frame, argAndNames);
        }

        @Override
        public Object execute(VirtualFrame frame, RArgsValuesAndNames argAndNames) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            return specialize(argAndNames).execute(frame, argAndNames);
        }

        private GroupDispatchCallNode specialize(RArgsValuesAndNames argAndNames) {
            CompilerAsserts.neverPartOfCompilation();
            if (depth < INLINE_CACHE_SIZE) {
                final GroupDispatchNode current = createCurrentNode(argAndNames.getValues());
                final GroupDispatchCallNode cachedNode = new CachedNode(current, new UninitializedGroupDispatchCallNode(this, this.depth + 1), this.callArgsNode);
                this.replace(cachedNode);
                return cachedNode;
            }
            return this.replace(new GenericDispatchNode(createGenericNode(argAndNames.getValues())));
        }

        private GroupDispatchNode createGenericNode(final Object[] evaluatedArgs) {
            if (this.groupName == RGroupGenerics.GROUP_OPS) {
                if (evaluatedArgs.length == 1) {
                    return new GenericUnaryOpsGroupDispatchNode(this.genericName, this.callArgsNode.containsVarArgsSymbol(), this.getSourceSection(), this.callArgsNode.getEncapsulatingSourceSection());
                }
                if (evaluatedArgs.length >= 2) {
                    return new GenericBinarysOpsGroupDispatchNode(this.genericName, this.callArgsNode.containsVarArgsSymbol(), this.getSourceSection(),
                                    this.callArgsNode.getEncapsulatingSourceSection());
                }
            }
            if (evaluatedArgs.length == 0 /*
                                           * TODO add condition for when all the arguments are
                                           * constant
                                           */) {
                return new GroupDispatchNode(this.genericName, this.groupName, this.callArgsNode.containsVarArgsSymbol(), this.getSourceSection(), this.callArgsNode.getEncapsulatingSourceSection());
            }
            return new GenericGroupDispatchNode(this.genericName, this.groupName, this.callArgsNode.containsVarArgsSymbol(), this.getSourceSection(), this.callArgsNode.getEncapsulatingSourceSection());
        }

        @SuppressFBWarnings(value = "ES_COMPARING_STRINGS_WITH_EQ", justification = "GROUP_OPS is intended to be used as an identity")
        protected GroupDispatchNode createCurrentNode(final Object[] evaluatedArgs) {
            if (this.groupName == RGroupGenerics.GROUP_OPS) {
                if (evaluatedArgs.length == 1) {
                    return new UnaryOpsGroupDispatchNode(this.genericName, this.callArgsNode.containsVarArgsSymbol(), this.getSourceSection(), this.callArgsNode.getEncapsulatingSourceSection());
                }
                if (evaluatedArgs.length >= 2) {
                    return new BinaryOpsGroupDispatchNode(this.genericName, this.callArgsNode.containsVarArgsSymbol(), this.getSourceSection(), this.callArgsNode.getEncapsulatingSourceSection());
                }
            }
            return new GroupDispatchNode(this.genericName, this.groupName, this.callArgsNode.containsVarArgsSymbol(), this.getSourceSection(), this.callArgsNode.getEncapsulatingSourceSection());
        }

        @Override
        public String getGenericName() {
            return this.genericName;
        }

        @Override
        public String getGroupName() {
            return this.groupName;
        }

        @Override
        public SourceSection getCallSrc() {
            return this.getSourceSection();
        }
    }

    private static final class CachedNode extends GroupDispatchCallNode {

        @Child private GroupDispatchCallNode nextNode;
        @Child private GroupDispatchNode currentNode;

        CachedNode(final GroupDispatchNode currentNode, final GroupDispatchCallNode nextNode, final CallArgumentsNode callArgsNode) {
            this.nextNode = nextNode;
            this.currentNode = currentNode;
            this.callArgsNode = callArgsNode;
        }

        @Override
        public Object execute(VirtualFrame frame) {
            RArgsValuesAndNames argsAndNames = evalArgs(frame);
            if (currentNode.isSameType(argsAndNames.getValues())) {
                return currentNode.execute(frame, argsAndNames);
            }
            return nextNode.execute(frame, argsAndNames);
        }

        @Override
        public Object execute(VirtualFrame frame, final RArgsValuesAndNames argAndNames) {
            if (currentNode.isSameType(argAndNames.getValues())) {
                return currentNode.execute(frame, argAndNames);
            }
            return nextNode.execute(frame, argAndNames);
        }

        @Override
        public String getGenericName() {
            return currentNode.genericName;
        }

        @Override
        public String getGroupName() {
            return currentNode.groupName;
        }

        @Override
        public SourceSection getCallSrc() {
            return currentNode.callSrc;
        }
    }

    private static final class GenericDispatchNode extends GroupDispatchCallNode {

        @Child private GroupDispatchNode gdn;

        public GenericDispatchNode(GroupDispatchNode gdn) {
            this.gdn = gdn;
        }

        @Override
        public Object execute(VirtualFrame frame, final RArgsValuesAndNames argAndNames) {
            return gdn.execute(frame, argAndNames);
        }

        @Override
        public String getGenericName() {
            return gdn.genericName;
        }

        @Override
        public String getGroupName() {
            return gdn.groupName;
        }

        @Override
        public SourceSection getCallSrc() {
            return gdn.callSrc;
        }
    }
}

class GroupDispatchNode extends S3DispatchNode {

    @CompilationFinal protected boolean isExecuted = false;
    @CompilationFinal protected final String groupName;
    @Child protected ReadVariableNode builtInNode;
    @Child private WriteVariableNode wvnGroup;
    protected RFunction builtinFunc;
    protected boolean writeGroup;
    protected RStringVector dotMethod;
    protected boolean hasVararg;
    protected final SourceSection callSrc;
    protected final SourceSection argSrc;

    @Override
    public Object execute(VirtualFrame frame, RStringVector aType) {
        throw new AssertionError();
    }

    protected GroupDispatchNode(final String aGenericName, final String groupName, final boolean hasVarArg, SourceSection callSrc, SourceSection argSrc) {
        this.genericName = aGenericName;
        this.groupName = groupName;
        this.hasVararg = hasVarArg;
        this.callSrc = callSrc;
        this.argSrc = argSrc;
    }

    public boolean isSameType(Object[] args) {
        return !isExecuted || S3DispatchNode.isEqualType(getArgClass(args[0]), this.type);
    }

    protected void initBuiltin(VirtualFrame frame) {
        // assuming builtin functions don't get redefined.
        if (builtInNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            builtInNode = insert(ReadVariableNode.createFunctionLookup(genericName, true));
            try {
                builtinFunc = builtInNode.executeFunction(frame);
            } catch (UnexpectedResultException e) {
                throw new RuntimeException("Builtin " + this.genericName + " not found");
            }
        }
    }

    protected void findTargetFunction(VirtualFrame frame) {
        final String[] prefix = {genericName, groupName};
        for (int i = 0; i < this.type.getLength(); ++i) {
            for (int j = 0; j < prefix.length; ++j) {
                findFunction(prefix[j], this.type.getDataAt(i), frame);
                if (targetFunction != null) {
                    RStringVector classVec = null;
                    if (i > 0) {
                        isFirst = false;
                        classVec = RDataFactory.createStringVector(Arrays.copyOfRange(this.type.getDataWithoutCopying(), i, this.type.getLength()), true);
                    } else {
                        isFirst = true;
                        classVec = this.type.copyResized(this.type.getLength(), false);
                    }
                    klass = classVec;
                    if (j == 1) {
                        writeGroup = true;
                    } else {
                        writeGroup = false;
                    }
                    return;
                }
            }
        }
    }

    public Object execute(VirtualFrame frame, final RArgsValuesAndNames argAndNames) {
        Object[] evaluatedArgs = argAndNames.getValues();
        String[] argNames = argAndNames.getNames();
        if (!isExecuted) {
            isExecuted = true;
            this.type = evaluatedArgs.length > 0 ? getArgClass(evaluatedArgs[0]) : null;
            if (this.type == null) {
                return callBuiltin(frame, evaluatedArgs, argNames);
            }
            findTargetFunction(frame);
            if (targetFunction != null) {
                dotMethod = RDataFactory.createStringVector(new String[]{targetFunctionName, ""}, true);
            }
        }
        if (targetFunction == null) {
            return callBuiltin(frame, evaluatedArgs, argNames);
        }
        return executeHelper(frame, evaluatedArgs, argNames);
    }

    protected Object callBuiltin(VirtualFrame frame, final Object[] evaluatedArgs, final String[] argNames) {
        initBuiltin(frame);
        EvaluatedArguments reorderedArgs = reorderArgs(frame, builtinFunc, evaluatedArgs, argNames, this.hasVararg, this.callSrc);
        Object[] argObject = RArguments.create(builtinFunc, this.callSrc, RArguments.getDepth(frame), reorderedArgs.getEvaluatedArgs(), reorderedArgs.getNames());
        indirectCallNode.assignSourceSection(this.callSrc);
        return indirectCallNode.call(frame, builtinFunc.getTarget(), argObject);
    }

    protected Object executeHelper(VirtualFrame frame, final Object[] evaluatedArgs, final String[] argNames) {
        EvaluatedArguments reorderedArgs = reorderArgs(frame, targetFunction, evaluatedArgs, argNames, this.hasVararg, this.callSrc);
        Object[] argObject = RArguments.createS3Args(targetFunction, this.callSrc, RArguments.getDepth(frame) + 1, reorderedArgs.getEvaluatedArgs(), reorderedArgs.getNames());
        // todo: cannot create frame descriptors in compiled code
        FrameDescriptor s3VarDefFrameDescriptor = new FrameDescriptor();
        FrameSlotChangeMonitor.initializeFrameDescriptor(s3VarDefFrameDescriptor, true);
        VirtualFrame s3VarDefFrame = Truffle.getRuntime().createVirtualFrame(RArguments.create(null, null, RArguments.getDepth(frame) + 1), s3VarDefFrameDescriptor);
        // todo: cannot create frame descriptors in compiled code
        FrameDescriptor argFrameDescriptor = new FrameDescriptor();
        FrameSlotChangeMonitor.initializeFrameDescriptor(argFrameDescriptor, true);
        VirtualFrame argFrame = Truffle.getRuntime().createVirtualFrame(argObject, argFrameDescriptor);
        genCallEnv = frame;
        defineVarsAsArguments(argFrame);
        defineVarsInFrame(s3VarDefFrame);
        wvnMethod = defineVarInFrame(s3VarDefFrame, wvnMethod, RRuntime.RDotMethod, dotMethod);
        RArguments.setS3Method(argFrame, targetFunctionName);
        if (writeGroup) {
            wvnGroup = defineVarInFrame(s3VarDefFrame, wvnGroup, RRuntime.RDotGroup, groupName);
            RArguments.setS3Group(argFrame, groupName);
        }
        indirectCallNode.assignSourceSection(this.callSrc);
        /*
         * Create a new frame s3VarDefFrame and define s3 generic variables such as .Generic,
         * .Method etc. in it and set this frame as the enclosing frame of the target function
         * ensuring that these generic variables are available to the called function. The real
         * enclosing frame of the target function become enclosing frame of the new frame
         * s3VarDefFrame. After the function returns reset the enclosing frame of the target
         * function.
         */
        MaterializedFrame enclosingFrame = targetFunction.getEnclosingFrame();
        MaterializedFrame mFrame = s3VarDefFrame.materialize();
        RArguments.setEnclosingFrame(mFrame, enclosingFrame);
        targetFunction.setEnclosingFrame(mFrame);
        Object result = indirectCallNode.call(frame, targetFunction.getTarget(), argObject);
        targetFunction.setEnclosingFrame(enclosingFrame);
        RArguments.setEnclosingFrame(mFrame, null);
        removeVars(mFrame);
        removeVar(mFrame.getFrameDescriptor(), RRuntime.RDotGroup);
        return result;
    }

    protected RStringVector getArgClass(Object arg) {
        if (arg instanceof RAbstractContainer && ((RAbstractContainer) arg).isObject()) {
            return ((RAbstractContainer) arg).getClassHierarchy();
        }
        return null;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        throw new AssertionError();
    }
}

class GenericGroupDispatchNode extends GroupDispatchNode {

    protected GenericGroupDispatchNode(String aGenericName, String groupName, final boolean hasVarArg, SourceSection callSrc, SourceSection argSrc) {
        super(aGenericName, groupName, hasVarArg, callSrc, argSrc);
    }

    @Override
    public Object execute(VirtualFrame frame, final RArgsValuesAndNames argAndNames) {
        Object[] evaluatedArgs = argAndNames.getValues();
        String[] argNames = argAndNames.getNames();
        this.type = getArgClass(evaluatedArgs[0]);
        if (this.type == null) {
            return callBuiltin(frame, evaluatedArgs, argNames);
        }
        findTargetFunction(frame);
        if (targetFunction != null) {
            dotMethod = RDataFactory.createStringVector(new String[]{targetFunctionName, ""}, true);
        }
        if (targetFunction == null) {
            callBuiltin(frame, evaluatedArgs, argNames);
        }
        return executeHelper(frame, evaluatedArgs, argNames);
    }
}
