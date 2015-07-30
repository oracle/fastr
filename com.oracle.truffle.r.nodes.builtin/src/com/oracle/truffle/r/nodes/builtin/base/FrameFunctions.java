/*
 * Copyright (c) 2014, 2015, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.builtin.base;

import static com.oracle.truffle.r.runtime.RBuiltinKind.*;

import java.util.*;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.frame.FrameInstance.FrameAccess;
import com.oracle.truffle.api.source.*;
import com.oracle.truffle.api.utilities.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.access.*;
import com.oracle.truffle.r.nodes.access.variables.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.nodes.function.*;
import com.oracle.truffle.r.nodes.function.PromiseHelperNode.PromiseDeoptimizeFrameNode;
import com.oracle.truffle.r.nodes.function.PromiseNode.VarArgsPromiseNode;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.RContext.Engine.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.RPromise.*;
import com.oracle.truffle.r.runtime.env.*;
import com.oracle.truffle.r.runtime.gnur.*;

/**
 * sys.R. See <a
 * href="https://stat.ethz.ch/R-manual/R-devel/library/base/html/sys.parent.html">here</a>. N.B. The
 * frame for the sys functions themselves is not counted in the R spec. Frames are numbered 0, 1, ..
 * starting from .GlobalEnv. Non-negative arguments are frame numbers, negative arguments are
 * relative to the current frame.
 */
public class FrameFunctions {

    public abstract static class FrameHelper extends RBuiltinNode {

        private final ConditionProfile currentFrameProfile = ConditionProfile.createBinaryProfile();
        protected final BranchProfile errorProfile = BranchProfile.create();

        /**
         * Determine the frame access mode of a subclass. The rule of thumb is that subclasses that
         * only use the frame internally should not materialize it, i.e., they should use
         * {@link FrameAccess#READ_ONLY} or {@link FrameAccess#READ_WRITE}.
         */
        protected abstract FrameAccess frameAccess();

        /**
         * Handles n > 0 and n < 0 and errors relating to stack depth.
         */
        protected Frame getFrame(VirtualFrame frame, int n) {
            int actualFrame;
            int depth = RArguments.getDepth(frame);
            if (n > 0) {
                if (n > depth) {
                    errorProfile.enter();
                    throw RError.error(RError.Message.NOT_THAT_MANY_FRAMES);
                }
                actualFrame = n;
            } else {
                actualFrame = depth + n - 1;
            }
            if (currentFrameProfile.profile(actualFrame == depth)) {
                return frame;
            } else {
                Frame callerFrame = Utils.getStackFrame(frameAccess(), actualFrame);
                if (callerFrame == null) {
                    errorProfile.enter();
                    throw RError.error(RError.Message.NOT_THAT_MANY_FRAMES);
                }
                return callerFrame;
            }
        }
    }

    @RBuiltin(name = "sys.call", kind = INTERNAL, parameterNames = {"which"})
    public abstract static class SysCall extends FrameHelper {

        @Override
        protected final FrameAccess frameAccess() {
            return FrameAccess.READ_ONLY;
        }

        @Specialization
        protected Object sysCall(VirtualFrame frame, int which) {
            /*
             * sys.call preserves provided names but does not create them, unlike match.call.
             */
            controlVisibility();
            Frame cframe = getFrame(frame, which);
            if (RArguments.getFunction(cframe) == null) {
                return RNull.instance;
            }
            return createCall(cframe);
        }

        @Specialization
        protected Object sysCall(VirtualFrame frame, double which) {
            return sysCall(frame, (int) which);
        }

        @TruffleBoundary
        protected RLanguage createCall(Frame cframe) {
            // TODO we really want the AST for the call in RArguments.
            // For now we reparse it.
            SourceSection callSource = RArguments.getCallSourceSection(cframe);
            RLanguage callAST;
            try {
                RExpression call = RContext.getEngine().parse(Source.fromText(callSource.getCode(), "<call source>"));
                callAST = (RLanguage) call.getDataAt(0);
            } catch (ParseException ex) {
                throw RInternalError.shouldNotReachHere("parse call source");
            }
            return callAST;
        }
    }

    /**
     * Generate a call object in which all of the arguments are fully qualified. Unlike
     * {@code sys.call}, named arguments are re-ordered to match the order in the signature. Plus,
     * unlike, {@code sys.call}, the {@code call} argument can be provided by the caller. "..." is a
     * significant complication for two reasons:
     * <ol>
     * <li>If {@code expand.dots} is {@code false} the "..." args are wrapped in a {@code pairlist}</li>
     * <li>One of the args might itself be "..." in which case the values have to be retrieved from
     * the environment associated with caller of the function containing {@code match.call}.</li>
     * </ol>
     * In summary, although the simple cases are indeed simple, there are many possible variants
     * using "..." that make the code a lot more complex that it seems it ought to be.
     */
    @RBuiltin(name = "match.call", kind = INTERNAL, parameterNames = {"definition", "call", "expand.dots"})
    public abstract static class MatchCall extends FrameHelper {

        @Override
        protected final FrameAccess frameAccess() {
            return FrameAccess.READ_ONLY;
        }

        @Specialization
        protected RLanguage matchCall(VirtualFrame frame, @SuppressWarnings("unused") RNull definition, Object callObj, byte expandDots) {
            return matchCall(frame, (RFunction) null, callObj, expandDots);
        }

        @Specialization
        protected RLanguage matchCall(VirtualFrame frame, RFunction definitionArg, Object callObj, byte expandDotsL) {
            /*
             * definition==null in the standard (default) case, in which case we get the RFunction
             * from the calling frame
             */
            controlVisibility();
            RLanguage call = checkCall(callObj);
            if (expandDotsL == RRuntime.LOGICAL_NA) {
                throw RError.error(getEncapsulatingSourceSection(), RError.Message.INVALID_ARGUMENT, "expand.dots");
            }
            boolean expandDots = RRuntime.fromLogical(expandDotsL);

            Frame cframe = Utils.getCallerFrame(frame, FrameAccess.READ_ONLY);
            RFunction definition = definitionArg;
            if (definition == null) {
                definition = RArguments.getFunction(cframe);
                if (definition == null) {
                    throw RError.error(getEncapsulatingSourceSection(), RError.Message.MATCH_CALL_CALLED_OUTSIDE_FUNCTION);
                }
            }
            return doMatchCall(cframe, definition, call, expandDots);
        }

        @TruffleBoundary
        private static RLanguage doMatchCall(Frame cframe, RFunction definition, RLanguage call, boolean expandDots) {
            /*
             * We have to ensure that all parameters are named, in the correct order, and deal with
             * "...". This process has a lot in common with MatchArguments, which we use as a
             * starting point
             */
            RCallNode callNode = (RCallNode) RASTUtils.unwrap(call.getRep());
            CallArgumentsNode callArgs = callNode.createArguments(null, false, false);
            MatchedArguments matchedArgs = ArgumentMatcher.matchArguments(definition, callArgs, null, null, true);
            ArgumentsSignature sig = matchedArgs.getSignature();
            RNode[] matchedArgNodes = matchedArgs.getArguments();
            // expand any varargs
            ExpandedVarArgs expandedVarArgs = processVarArgs(cframe, matchedArgNodes);
            int[] argsLength = newArgsLength(matchedArgNodes, expandDots, expandedVarArgs);
            int newCallArgsLength = argsLength[0];
            int listArgsLength = expandDots ? -1 : argsLength[1];
            String[] listNames = null;
            if (sig.getVarArgCount() > 0) {
                if (!expandDots) {
                    if (sig.getNonNullCount() > 1) {
                        // some named arguments as well as ...
                        String[] names = new String[newCallArgsLength];
                        listNames = new String[listArgsLength];
                        int i = 0;
                        int j = 0;
                        for (RNode node : matchedArgNodes) {
                            if (node instanceof VarArgsPromiseNode) {
                                assignVarArgsNames(expandedVarArgs, listNames, 0);
                                names[i++] = ArgumentsSignature.VARARG_NAME;
                            } else if (!isMissing(node)) {
                                names[i++] = sig.getName(j);
                            }
                            j++;
                        }
                        sig = ArgumentsSignature.get(names);
                    } else {
                        listNames = new String[listArgsLength];
                        if (listArgsLength > 0) {
                            for (RNode node : matchedArgNodes) {
                                if (node instanceof VarArgsPromiseNode) {
                                    assignVarArgsNames(expandedVarArgs, listNames, 0);
                                }
                            }
                            sig = ArgumentsSignature.get(ArgumentsSignature.VARARG_NAME);
                        } else {
                            sig = ArgumentsSignature.empty(0);
                        }
                    }
                } else {
                    String[] names = new String[newCallArgsLength];
                    int i = 0;
                    int j = 0;
                    for (RNode node : matchedArgNodes) {
                        if (node instanceof VarArgsPromiseNode) {
                            assignVarArgsNames(expandedVarArgs, names, i);
                            i += expandedVarArgs.nodes.size();
                        } else if (!isMissing(node)) {
                            names[i++] = sig.getName(j);
                        }
                        j++;
                    }
                    sig = ArgumentsSignature.get(names);
                }
            }

            RSyntaxNode[] newArgs = new RSyntaxNode[newCallArgsLength];
            int newArgsIndex = 0;
            for (RNode node : matchedArgNodes) {
                if (isMissing(node)) {
                    continue;
                } else if (node instanceof VarArgsPromiseNode) {
                    if (expandedVarArgs.nodes.size() > 0) {
                        if (expandDots) {
                            for (int v = 0; v < expandedVarArgs.nodes.size(); v++) {
                                newArgs[newArgsIndex++] = (RSyntaxNode) expandedVarArgs.nodes.get(v);
                            }
                        } else {
                            // GnuR appears to create a pairlist rather than a list
                            RPairList head = RDataFactory.createPairList();
                            head.setType(SEXPTYPE.LISTSXP);
                            RPairList pl = head;
                            RPairList prev = null;
                            int pls = expandedVarArgs.nodes.size();
                            for (int v = 0; v < pls; v++) {
                                RNode n = RASTUtils.unwrap(expandedVarArgs.nodes.get(v));
                                Object listValue;
                                if (n instanceof ConstantNode) {
                                    listValue = ((ConstantNode) n).getValue();
                                } else if (n instanceof ReadVariableNode) {
                                    listValue = RDataFactory.createSymbol(((ReadVariableNode) n).getIdentifier());
                                } else {
                                    throw RInternalError.shouldNotReachHere();
                                }
                                pl.setCar(listValue);
                                if (listNames[v] != null) {
                                    pl.setTag(RDataFactory.createSymbol(listNames[v]));
                                }
                                if (prev != null) {
                                    prev.setCdr(pl);
                                }
                                prev = pl;
                                if (v != pls - 1) {
                                    pl = RDataFactory.createPairList();
                                    pl.setType(SEXPTYPE.LISTSXP);
                                }
                            }
                            newArgs[newArgsIndex++] = ConstantNode.create(head);
                        }
                    }
                } else {
                    PromiseNode pn = (PromiseNode) node;
                    RSyntaxNode pnExpr = pn.getPromiseExpr();
                    newArgs[newArgsIndex++] = pnExpr;
                }
            }

            RNode modCallNode = RASTUtils.createCall(callNode.getFunctionNode(), sig, newArgs);
            return RDataFactory.createLanguage(modCallNode);
        }

        /**
         * Computes the total number of arguments in the resulting call, handling whether "..." is
         * expanded or not. Returns the total in index 0 and the number of varargs args in index 1.
         */
        private static int[] newArgsLength(RNode[] matchedArgNodes, boolean expandDots, ExpandedVarArgs expandedVarArgs) {
            int totalArgs = 0;
            int varArgs = 0;
            for (RNode node : matchedArgNodes) {
                if (ConstantNode.isMissing(node) || isEmptyVarArg(node)) {
                    continue;
                } else if (node instanceof VarArgsPromiseNode) {
                    varArgs = expandedVarArgs.nodes.size();
                    if (expandDots) {
                        totalArgs += varArgs;
                    } else {
                        if (varArgs > 0) {
                            totalArgs++;
                        }
                    }
                } else {
                    totalArgs++;
                }
            }
            return new int[]{totalArgs, varArgs};
        }

        private static boolean isMissing(RNode node) {
            return ConstantNode.isMissing(node) || isEmptyVarArg(node);
        }

        private static boolean isEmptyVarArg(RNode node) {
            if (node instanceof ConstantNode && ((ConstantNode) node).getValue() instanceof RArgsValuesAndNames) {
                RArgsValuesAndNames rvn = (RArgsValuesAndNames) ((ConstantNode) node).getValue();
                return rvn.getLength() == 0;
            }
            return false;
        }

        private static boolean isDots(Object expr) {
            RSyntaxNode arg = (RSyntaxNode) RASTUtils.unwrap(expr);
            return arg instanceof ReadVariableNode && ((ReadVariableNode) arg).getIdentifier().equals(ArgumentsSignature.VARARG_NAME);
        }

        private static void assignVarArgsNames(ExpandedVarArgs expandedVarArgs, String[] names, int index) {
            for (int c = 0; c < expandedVarArgs.names.size(); c++) {
                if (expandedVarArgs.names.get(c) != null) {
                    names[index + c] = expandedVarArgs.names.get(c);
                }
            }
        }

        private static final class ExpandedVarArgs {
            private final ArrayList<RNode> nodes = new ArrayList<>();
            private final ArrayList<String> names = new ArrayList<>();
        }

        /**
         * Processes any "..." args, retrieving their values using cframe if any of them are
         * themselves "...".
         */
        private static ExpandedVarArgs processVarArgs(Frame cframe, RNode[] matchedArgNodes) {
            ExpandedVarArgs result = new ExpandedVarArgs();
            for (RNode node : matchedArgNodes) {
                if (node instanceof VarArgsPromiseNode) {
                    VarArgsPromiseNode vpn = (VarArgsPromiseNode) node;
                    ArgumentsSignature vpnArgsSig = vpn.getSignature();
                    RPromise.Closure[] closures = ((VarArgsPromiseNode) node).getClosures();
                    for (int i = 0; i < closures.length; i++) {
                        Closure cl = closures[i];
                        if (isDots(cl.getExpr())) {
                            // expand this using cframe
                            RArgsValuesAndNames rvn = (RArgsValuesAndNames) RArguments.getArgument(cframe, 0);
                            int vn = 1;
                            for (int v = i; v < rvn.getLength(); v++) {
                                RNode narg = RASTUtils.createNodeForValue(rvn.getArgument(v));
                                if (narg == null) {
                                    StringBuilder sb = new StringBuilder((vn) < 10 ? ".." : ".");
                                    sb.append(vn);
                                    narg = ConstantNode.create(RDataFactory.createSymbol(sb.toString()));
                                }
                                result.nodes.add(narg);
                                result.names.add(rvn.getSignature().getName(v));
                                vn++;
                            }
                        } else {
                            result.nodes.add((RNode) cl.getExpr());
                            result.names.add(vpnArgsSig.getName(i));
                        }
                    }
                }
            }
            return result;
        }

        @Specialization
        @SuppressWarnings("unused")
        protected RLanguage matchCall(Object definition, Object call, Object expandDots) {
            controlVisibility();
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.INVALID_OR_UNIMPLEMENTED_ARGUMENTS);
        }

        private RLanguage checkCall(Object callObj) throws RError {
            if (callObj instanceof RExpression) {
                return checkCall(((RExpression) callObj).getDataAt(0));
            }
            if (callObj instanceof RLanguage) {
                RLanguage call = (RLanguage) callObj;
                RNode node = RASTUtils.unwrap(call.getRep());
                if (node instanceof RCallNode || node instanceof GroupDispatchNode) {
                    return call;
                }
            }
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.INVALID_ARGUMENT, "call");
        }

    }

    @RBuiltin(name = "sys.nframe", kind = INTERNAL, parameterNames = {})
    public abstract static class SysNFrame extends RBuiltinNode {
        @Specialization
        protected int sysNFrame(VirtualFrame frame) {
            controlVisibility();
            return RArguments.getDepth(frame) - 1;
        }
    }

    @RBuiltin(name = "sys.frame", kind = INTERNAL, parameterNames = {"which"})
    public abstract static class SysFrame extends FrameHelper {

        private final ConditionProfile zeroProfile = ConditionProfile.createBinaryProfile();
        private final PromiseDeoptimizeFrameNode deoptFrameNode = new PromiseDeoptimizeFrameNode();

        @Override
        protected final FrameAccess frameAccess() {
            return FrameAccess.MATERIALIZE;
        }

        @Specialization
        protected REnvironment sysFrame(VirtualFrame frame, int which) {
            controlVisibility();
            REnvironment result;
            if (zeroProfile.profile(which == 0)) {
                result = REnvironment.globalEnv();
            } else {
                Frame callerFrame = getFrame(frame, which);
                result = REnvironment.frameToEnvironment(callerFrame.materialize());
            }

            // Deoptimize every promise which is now in this frame, as it might leave it's stack
            deoptFrameNode.deoptimizeFrame(result.getFrame());
            return result;
        }

        @Specialization
        protected REnvironment sysFrame(VirtualFrame frame, double which) {
            return sysFrame(frame, (int) which);
        }
    }

    @RBuiltin(name = "sys.parent", kind = INTERNAL, parameterNames = {"n"})
    public abstract static class SysParent extends RBuiltinNode {

        @Specialization
        protected int sysParent(VirtualFrame frame, int n) {
            controlVisibility();
            int p = RArguments.getDepth(frame) - n - 1;
            return p < 0 ? 0 : p;
        }

        @Specialization
        protected int sysParent(VirtualFrame frame, double n) {
            return sysParent(frame, (int) n);
        }

    }

    @RBuiltin(name = "sys.function", kind = INTERNAL, parameterNames = {"which"})
    public abstract static class SysFunction extends FrameHelper {

        @Override
        protected final FrameAccess frameAccess() {
            return FrameAccess.READ_ONLY;
        }

        @Specialization
        protected Object sysFunction(VirtualFrame frame, int which) {
            controlVisibility();
            // N.B. Despite the spec, n==0 is treated as the current function
            Frame callerFrame = getFrame(frame, which);
            RFunction func = RArguments.getFunction(callerFrame);
            if (func == null) {
                return RNull.instance;
            } else {
                return func;
            }
        }

        @Specialization
        protected Object sysFunction(VirtualFrame frame, double which) {
            return sysFunction(frame, (int) which);
        }
    }

    @RBuiltin(name = "sys.parents", kind = INTERNAL, parameterNames = {})
    public abstract static class SysParents extends FrameHelper {

        @Override
        protected final FrameAccess frameAccess() {
            return FrameAccess.READ_ONLY;
        }

        @Specialization
        protected RIntVector sysParents(VirtualFrame frame) {
            controlVisibility();
            int d = RArguments.getDepth(frame) - 1;
            int[] data = new int[d];
            for (int i = 0; i < d; i++) {
                data[i] = i;
            }
            return RDataFactory.createIntVector(data, RDataFactory.COMPLETE_VECTOR);
        }

    }

    @RBuiltin(name = "sys.frames", kind = INTERNAL, parameterNames = {})
    public abstract static class SysFrames extends FrameHelper {

        @Override
        protected final FrameAccess frameAccess() {
            return FrameAccess.READ_ONLY;
        }

        @Specialization
        protected Object sysFrames() {
            errorProfile.enter();
            // TODO DEOPT RPromise.deoptimizeFrame every frame that escapes it's stack here
            throw RError.nyi(null, "sys.frames");
        }
    }

    /**
     * The environment of the caller of the function that called parent.frame.
     */
    @RBuiltin(name = "parent.frame", kind = INTERNAL, parameterNames = {"n"})
    public abstract static class ParentFrame extends FrameHelper {

        private final ConditionProfile nullProfile = ConditionProfile.createBinaryProfile();

        public abstract Object execute(VirtualFrame frame, int n);

        @Override
        protected final FrameAccess frameAccess() {
            return FrameAccess.MATERIALIZE;
        }

        @Specialization
        protected REnvironment parentFrame(VirtualFrame frame, int n) {
            controlVisibility();
            if (n == 0) {
                errorProfile.enter();
                throw RError.error(RError.Message.INVALID_ARGUMENT, RRuntime.intToString(n));
            }
            int p = RArguments.getDepth(frame) - n - 1;
            Frame callerFrame = Utils.getStackFrame(FrameAccess.MATERIALIZE, p);
            if (nullProfile.profile(callerFrame == null)) {
                return REnvironment.globalEnv();
            } else {
                return REnvironment.frameToEnvironment(callerFrame.materialize());
            }
        }

        @Specialization
        protected REnvironment parentFrame(VirtualFrame frame, double n) {
            return parentFrame(frame, (int) n);
        }
    }

}
