/*
 * Copyright (c) 2015, 2017, Oracle and/or its affiliates. All rights reserved.
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

import java.util.Arrays;
import java.util.List;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.r.nodes.control.OperatorNode;
import com.oracle.truffle.r.nodes.function.PromiseHelperNode.PromiseCheckHelperNode;
import com.oracle.truffle.r.nodes.function.visibility.SetVisibilityNode;
import com.oracle.truffle.r.nodes.unary.CastNode;
import com.oracle.truffle.r.runtime.ArgumentsSignature;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RError.Message;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.builtins.RBuiltinKind;
import com.oracle.truffle.r.runtime.conn.RConnection;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.RArgsValuesAndNames;
import com.oracle.truffle.r.runtime.data.REmpty;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import com.oracle.truffle.r.runtime.nodes.RNode;
import com.oracle.truffle.r.runtime.nodes.RSyntaxCall;
import com.oracle.truffle.r.runtime.nodes.RSyntaxConstant;
import com.oracle.truffle.r.runtime.nodes.RSyntaxElement;
import com.oracle.truffle.r.runtime.nodes.RSyntaxLookup;
import com.oracle.truffle.r.runtime.nodes.RSyntaxNode;

/**
 * This node is used during AST creation to represent a .Internal call. Upon the first execution, it
 * will examine its arguments, raising errors if necessary, and rewrite itself to a version that
 * fits the arguments. The main difference in how arguments are passed is in whether varargs are
 * introduced or forwarded by the call to the .Internal.
 */
public abstract class InternalNode extends OperatorNode {

    protected final ArgumentsSignature outerSignature;
    protected final RSyntaxNode[] outerArgs;

    public InternalNode(SourceSection src, RSyntaxLookup operator, ArgumentsSignature outerSignature, RSyntaxNode[] outerArgs) {
        super(src, operator);
        this.outerSignature = outerSignature;
        this.outerArgs = outerArgs;
    }

    public static InternalNode create(SourceSection src, RSyntaxLookup operator, ArgumentsSignature outerSignature, RSyntaxNode[] outerArgs) {
        return new InternalUninitializedNode(src, operator, outerSignature, outerArgs);
    }

    /**
     * Represents an uninitialized .Internal call, executing will replace it with a more specific
     * implementation.
     */
    private static final class InternalUninitializedNode extends InternalNode {

        // whether arguments need to be copied
        private boolean needsCopy;

        InternalUninitializedNode(SourceSection src, RSyntaxLookup operator, ArgumentsSignature outerSignature, RSyntaxNode[] outerArgs) {
            super(src, operator, outerSignature, outerArgs);
        }

        @Override
        public Node copy() {
            InternalUninitializedNode copy = (InternalUninitializedNode) super.copy();
            copy.needsCopy = true;
            return copy;
        }

        @Override
        public Object execute(VirtualFrame frame) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            if (outerArgs.length != 1) {
                throw RError.error(RError.SHOW_CALLER, Message.ARGUMENTS_PASSED, outerArgs.length, ".Internal", 1);
            }
            if (!(outerArgs[0] instanceof RSyntaxCall)) {
                throw RError.error(RError.SHOW_CALLER, Message.INVALID_ARG, ".Internal()");
            }

            RSyntaxCall call = (RSyntaxCall) outerArgs[0];
            RSyntaxElement lhs = call.getSyntaxLHS();

            // extract the builtin name
            String name = null;
            if (lhs instanceof RSyntaxLookup) {
                name = ((RSyntaxLookup) lhs).getIdentifier();
            } else if (lhs instanceof RSyntaxConstant) {
                Object constant = ((RSyntaxConstant) lhs).getValue();
                if (constant instanceof String) {
                    name = (String) constant;
                } else if (constant instanceof RAbstractStringVector) {
                    RAbstractStringVector stringVector = (RAbstractStringVector) constant;
                    if (stringVector.getLength() == 1) {
                        name = stringVector.getDataAt(0);
                    }
                }
            }
            if (name == null) {
                throw RError.error(RError.SHOW_CALLER, Message.INVALID_ARG, ".Internal()");
            }

            RFunction function = RContext.lookupBuiltin(name);
            if (function == null || function.isBuiltin() && function.getRBuiltin().getKind() != RBuiltinKind.INTERNAL) {
                // determine whether we're supposed to implement this builtin
                if (function == null && NOT_IMPLEMENTED.contains(name)) {
                    throw RInternalError.unimplemented(".Internal " + name);
                }
                throw RError.error(RError.SHOW_CALLER, RError.Message.NO_SUCH_INTERNAL, name);
            }

            RBuiltinFactory factory = (RBuiltinFactory) function.getRBuiltin();
            RSyntaxElement[] callArgs = call.getSyntaxArguments();
            if (needsCopy) {
                callArgs = callArgs.clone();
                for (int i = 0; i < callArgs.length; i++) {
                    callArgs[i] = RContext.getASTBuilder().process(callArgs[i]);
                }
            }

            // verify the number of arguments
            if (factory.getSignature().getVarArgCount() == 0) {
                if (callArgs.length != factory.getSignature().getLength()) {
                    throw RError.error(RError.SHOW_CALLER, Message.ARGUMENTS_PASSED, callArgs.length, ".Internal(" + name + ")", factory.getSignature().getLength());
                }
                for (int i = 0; i < callArgs.length; i++) {
                    if (callArgs[i] instanceof RSyntaxConstant && ((RSyntaxConstant) callArgs[i]).getValue() == REmpty.instance) {
                        throw RError.error(RError.SHOW_CALLER, Message.ARGUMENT_EMPTY, i + 1);
                    }
                }
            }
            // look for "..." argument and verify that only one "..." is given (FastR restriction)
            int varArgForward = ArgumentsSignature.NO_VARARG;
            for (int i = 0; i < callArgs.length; i++) {
                RSyntaxElement arg = callArgs[i];
                if (arg instanceof RSyntaxLookup && ((RSyntaxLookup) arg).getIdentifier().equals(ArgumentsSignature.VARARG_NAME)) {
                    RInternalError.guarantee(varArgForward == ArgumentsSignature.NO_VARARG, "more than one '...' in .Internal arguments is not supported");
                    varArgForward = i;
                }
            }
            int varArgIndex = factory.getSignature().getVarArgIndex();
            if (varArgForward == varArgIndex) {
                if (varArgIndex == ArgumentsSignature.NO_VARARG) {
                    return replace(new InternalCallDefaultNode(getLazySourceSection(), operator, outerSignature, outerArgs, factory, callArgs)).execute(frame);
                } else {
                    return replace(new InternalCallVarArgForwardNode(getLazySourceSection(), operator, outerSignature, outerArgs, factory, callArgs)).execute(frame);
                }
            } else if (varArgForward == ArgumentsSignature.NO_VARARG) {
                return replace(new InternalCallWrapNode(getLazySourceSection(), operator, outerSignature, outerArgs, factory, callArgs)).execute(frame);
            } else {
                /*
                 * So far, refusing to handle .Internal calls that require complex argument
                 * rematching seems reasonable.
                 */
                throw RInternalError.shouldNotReachHere("complex argument matching in .Internal");
            }
        }
    }

    /**
     * Base class for all actual implementations of .Internal, handles everything but preparing the
     * argument array.
     */
    private abstract static class InternalCallNode extends InternalNode {

        protected final RBuiltinFactory factory;
        protected final int varArgIndex;

        @Children protected final RNode[] arguments;
        @Children protected final CastNode[] casts;
        @Child private RBuiltinNode builtin;
        @Child private SetVisibilityNode visibility = SetVisibilityNode.create();

        InternalCallNode(SourceSection src, RSyntaxLookup operator, ArgumentsSignature outerSignature, RSyntaxNode[] outerArgs, RBuiltinFactory factory, RSyntaxElement[] args) {
            super(src, operator, outerSignature, outerArgs);
            this.factory = factory;
            this.builtin = factory.getConstructor().get();
            this.casts = builtin.getCasts();
            this.arguments = new RNode[args.length];
            for (int i = 0; i < args.length; i++) {
                arguments[i] = ((RSyntaxNode) args[i]).asRNode();
            }
            this.varArgIndex = factory.getSignature().getVarArgIndex();
            if (varArgIndex != ArgumentsSignature.NO_VARARG) {
                assert casts.length <= varArgIndex || casts[varArgIndex] == null : "no casts on '...' arguments to .Internals";
            }
        }

        protected abstract Object[] prepareArgs(VirtualFrame frame);

        @Override
        public Object execute(VirtualFrame frame) {
            Object result = builtin.executeBuiltin(frame, prepareArgs(frame));
            assert result != null : "builtins cannot return 'null': " + factory.getName();
            assert !(result instanceof RConnection) : "builtins cannot return connection': " + factory.getName();
            visibility.execute(frame, factory.getVisibility());
            return result;
        }

        @Override
        public void voidExecute(VirtualFrame frame) {
            builtin.executeBuiltin(frame, prepareArgs(frame));
        }
    }

    /**
     * Handling the case where there are no varargs.
     */
    private static final class InternalCallDefaultNode extends InternalCallNode {

        InternalCallDefaultNode(SourceSection src, RSyntaxLookup operator, ArgumentsSignature outerSignature, RSyntaxNode[] outerArgs, RBuiltinFactory factory, RSyntaxElement[] args) {
            super(src, operator, outerSignature, outerArgs, factory, args);
            assert args.length == factory.getSignature().getLength();
        }

        @Override
        @ExplodeLoop
        protected Object[] prepareArgs(VirtualFrame frame) {
            Object[] args = new Object[arguments.length];
            for (int i = 0; i < args.length; i++) {
                Object value = arguments[i].execute(frame);
                if (i < casts.length && casts[i] != null) {
                    value = casts[i].execute(value);
                }
                args[i] = value;
            }
            return args;
        }
    }

    /**
     * Handling the case where there is a single vararg parameter that is forwarded to the builtin.
     * Any promises in the vararg need to be forced.
     */
    private static final class InternalCallVarArgForwardNode extends InternalCallNode {

        @Child private PromiseCheckHelperNode promiseHelper = new PromiseCheckHelperNode();

        InternalCallVarArgForwardNode(SourceSection src, RSyntaxLookup operator, ArgumentsSignature outerSignature, RSyntaxNode[] outerArgs, RBuiltinFactory factory, RSyntaxElement[] args) {
            super(src, operator, outerSignature, outerArgs, factory, args);
            assert args.length == factory.getSignature().getLength();
        }

        @Override
        @ExplodeLoop
        protected Object[] prepareArgs(VirtualFrame frame) {
            Object[] args = new Object[arguments.length];
            for (int i = 0; i < args.length; i++) {
                Object value = arguments[i].execute(frame);
                if (i == varArgIndex) {
                    value = forcePromises(frame, (RArgsValuesAndNames) value);
                } else {
                    if (i < casts.length && casts[i] != null) {
                        value = casts[i].execute(value);
                    }
                }
                args[i] = value;
            }
            return args;
        }

        private RArgsValuesAndNames forcePromises(VirtualFrame frame, RArgsValuesAndNames varArgs) {
            Object[] array = new Object[varArgs.getLength()];
            for (int i = 0; i < array.length; i++) {
                array[i] = promiseHelper.checkEvaluate(frame, varArgs.getArgument(i));
            }
            return new RArgsValuesAndNames(array, varArgs.getSignature());
        }
    }

    /**
     * Handles the case where the .Internal call specifies multiple arguments that need to be
     * wrapped in a vararg.
     */
    private static final class InternalCallWrapNode extends InternalCallNode {

        InternalCallWrapNode(SourceSection src, RSyntaxLookup operator, ArgumentsSignature outerSignature, RSyntaxNode[] outerArgs, RBuiltinFactory factory, RSyntaxElement[] args) {
            super(src, operator, outerSignature, outerArgs, factory, args);
        }

        @Override
        @ExplodeLoop
        protected Object[] prepareArgs(VirtualFrame frame) {
            Object[] args = new Object[factory.getSignature().getLength()];

            for (int i = 0; i < args.length - 1; i++) {
                Object value = arguments[i].execute(frame);
                if (i < casts.length && casts[i] != null) {
                    value = casts[i].execute(value);
                }
                args[i] = value;
            }
            Object[] varArgs = new Object[arguments.length - (factory.getSignature().getLength() - 1)];
            for (int i = 0; i < varArgs.length; i++) {
                varArgs[i] = arguments[args.length - 1 + i].execute(frame);
            }
            args[args.length - 1] = new RArgsValuesAndNames(varArgs, ArgumentsSignature.empty(varArgs.length));
            return args;
        }
    }

    @Override
    public RSyntaxNode[] getSyntaxArguments() {
        return outerArgs;
    }

    @Override
    public ArgumentsSignature getSyntaxSignature() {
        return outerSignature;
    }

    /*
     * This is a list of .Internal functions that should be available but may not be implemented
     * yet. When an unknown .Internal is encountered, this list is queried to see whether it's a
     * non-existing or an unimplemented builtin. For builtins that are implemented, it makes no
     * difference if they are in the list or not.
     *
     * Some builtins, e.g., the distribution functions, are in this list because GNUR lists them as
     * .Internal although they are never used in this way. Therefore, this list is not an accurate
     * listing of missing builtins.
     */
    private static final List<String> NOT_IMPLEMENTED = Arrays.asList(
                    ".addTryHandlers", "interruptsSuspended", "restart", "backsolve", "max.col", "comment", "`comment<-`", "list2env", "tcrossprod",
                    "lbeta", "beta", "lchoose", "dchisq", "pchisq", "qchisq", "dexp", "pexp", "qexp", "dgeom", "pgeom", "qgeom", "dpois", "ppois", "qpois", "dt", "pt", "qt", "dsignrank", "psignrank",
                    "qsignrank", "besselJ", "besselY", "psigamma", "dbeta", "pbeta", "qbeta", "dbinom", "pbinom", "qbinom", "dcauchy", "pcauchy", "qcauchy", "df", "pf", "qf", "dgamma", "pgamma",
                    "qgamma", "dlnorm", "plnorm", "qlnorm", "dlogis", "plogis", "qlogis", "dnbinom", "pnbinom", "qnbinom", "dnorm", "pnorm", "qnorm", "dunif", "punif", "qunif", "dweibull", "pweibull",
                    "qweibull", "dnchisq", "pnchisq", "qnchisq", "dnt", "pnt", "qnt", "dwilcox", "pwilcox", "qwilcox", "besselI", "besselK", "dnbinom_mu", "pnbinom_mu", "qnbinom_mu", "dhyper",
                    "phyper", "qhyper", "dnbeta", "pnbeta", "qnbeta", "dnf", "pnf", "qnf", "dtukey", "ptukey", "qtukey", "rchisq", "rexp", "rgeom", "rpois", "rt", "rsignrank", "rbeta", "rbinom",
                    "rcauchy", "rf", "rgamma", "rlnorm", "rlogis", "rnbinom", "rnbinom_mu", "rnchisq", "rnorm", "runif", "rweibull", "rwilcox", "rhyper",
                    "format.info", "grepRaw", "regexec", "adist", "aregexec", "chartr", "packBits", "strtrim", "eapply", "machine", "save", "dump", "prmatrix", "gcinfo", "gctorture", "gctorture2",
                    "memory.profile", "sys.on.exit", "builtins", "bodyCode", "rapply", "inspect",
                    "mem.limits", "capabilitiesX11", "Cstack_info", "file.choose", "polyroot", "mkCode", "bcClose", "is.builtin.internal", "disassemble", "bcVersion", "load.from.file", "save.to.file",
                    "growconst", "putconst", "getconst", "setNumMathThreads", "setMaxNumMathThreads", "isatty", "isIncomplete", "pipe", "fifo", "unz", "truncate", "rawConnection",
                    "rawConnectionValue", "sockSelect", "gzcon", "memCompress", "memDecompress", "mkUnbound", "env.profile", "setSessionTimeLimit", "icuSetCollate", "findInterval", "rowsum_df",
                    "La_qr_cmplx", "La_rs_cmplx", "La_rg_cmplx", "La_rs_cmplx", "La_dlange", "La_dgecon", "La_dtrcon", "La_zgecon", "La_ztrcon", "La_solve_cmplx", "La_chol2inv", "qr_qy_real",
                    "qr_coef_cmplx", "qr_qy_cmpl", "La_svd", "La_svd_cmplx");
}
