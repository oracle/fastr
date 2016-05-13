/*
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates. All rights reserved.
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

import static com.oracle.truffle.r.runtime.RBuiltinKind.PRIMITIVE;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.NodeCost;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.r.nodes.RASTUtils;
import com.oracle.truffle.r.nodes.access.variables.ReadVariableNode;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.function.RCallNode;
import com.oracle.truffle.r.runtime.RBuiltin;
import com.oracle.truffle.r.runtime.RBuiltinKind;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RVisibility;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.data.RMissing;
import com.oracle.truffle.r.runtime.data.RPromise;
import com.oracle.truffle.r.runtime.nodes.RInstrumentableNode;
import com.oracle.truffle.r.runtime.nodes.RNode;

/**
 * The {@code .Internal} builtin. In {@code .Internal(func(args))} we have an AST where the
 * RCallNode.Uninitialized and the function child should be a {@link ReadVariableNode} node with
 * symbol {@code func}. We want to rewrite the AST as if the {@code func} had been called directly.
 * However, we must do this in a non-destructive way otherwise deparsing and serialization will
 * fail.
 *
 * A note on {@link RInstrumentableNode}. Since both the {@code .Internal} and the argument are
 * {@link RCallNode}s both may have been wrapped. The call to {@link RASTUtils#unwrap} will go
 * through any {@code RNodeWrapper} and the rewrite will remove one level of wrapping. However the
 * parent of the the {@code .Internal}, which will be an {@code RNodeWrapper}, will remain so any
 * instrumentation at that level will remain in place.
 */
@NodeInfo(cost = NodeCost.NONE)
@RBuiltin(name = ".Internal", visibility = RVisibility.CUSTOM, kind = PRIMITIVE, parameterNames = {"call"}, nonEvalArgs = 0)
public abstract class Internal extends RBuiltinNode {

    private final BranchProfile errorProfile = BranchProfile.create();

    @Child private RNode builtinCallNode;

    @Specialization
    protected Object doInternal(@SuppressWarnings("unused") RMissing x) {
        errorProfile.enter();
        throw RError.error(this, RError.Message.ARGUMENTS_PASSED_0_1, getRBuiltin().name());
    }

    @Specialization
    protected Object doInternal(VirtualFrame frame, RPromise x) {
        if (builtinCallNode == null) {
            RNode call = (RNode) x.getRep();
            RNode operand = (RNode) RASTUtils.unwrap(call);

            if (!(operand instanceof RCallNode)) {
                errorProfile.enter();
                throw RError.error(this, RError.Message.INVALID_INTERNAL);
            }

            RCallNode callNode = (RCallNode) operand;
            RNode func = callNode.getFunctionNode();
            String name = ((ReadVariableNode) func).getIdentifier();
            RFunction function = RContext.lookupBuiltin(name);
            if (function == null || function.isBuiltin() && function.getRBuiltin().getKind() != RBuiltinKind.INTERNAL) {
                errorProfile.enter();
                if (function == null && notImplemented(name)) {
                    throw RInternalError.unimplemented(".Internal " + name);
                }
                throw RError.error(this, RError.Message.NO_SUCH_INTERNAL, name);
            }

            // .Internal function is validated
            CompilerDirectives.transferToInterpreterAndInvalidate();
            builtinCallNode = insert(RCallNode.createInternalCall(callNode, function));
        }
        return builtinCallNode.execute(frame);
    }

    private static boolean notImplemented(String name) {
        for (String internal : NOT_IMPLEMENTED) {
            if (internal.equals(name)) {
                return true;
            }
        }
        return false;
    }

    private static final String[] NOT_IMPLEMENTED = new String[]{
                    ".addTryHandlers", "interruptsSuspended", "restart", "backsolve", "max.col", "row", "all.names", "comment", "`comment<-`", "list2env", "tcrossprod", "setFileTime", "lbeta",
                    "beta", "lchoose", "choose", "dchisq", "pchisq", "qchisq", "dexp", "pexp", "qexp", "dgeom", "pgeom", "qgeom", "dpois", "ppois", "qpois", "dt", "pt", "qt", "dsignrank",
                    "psignrank", "qsignrank", "besselJ", "besselY", "psigamma", "dbeta", "pbeta", "qbeta", "dbinom", "pbinom", "qbinom", "dcauchy", "pcauchy", "qcauchy", "df", "pf", "qf", "dgamma",
                    "pgamma", "qgamma", "dlnorm", "plnorm", "qlnorm", "dlogis", "plogis", "qlogis", "dnbinom", "pnbinom", "qnbinom", "dnorm", "pnorm", "qnorm", "dunif", "punif", "qunif", "dweibull",
                    "pweibull", "qweibull", "dnchisq", "pnchisq", "qnchisq", "dnt", "pnt", "qnt", "dwilcox", "pwilcox", "qwilcox", "besselI", "besselK", "dnbinom_mu", "pnbinom_mu", "qnbinom_mu",
                    "dhyper", "phyper", "qhyper", "dnbeta", "pnbeta", "qnbeta", "dnf", "pnf", "qnf", "dtukey", "ptukey", "qtukey", "rchisq", "rexp", "rgeom", "rpois", "rt", "rsignrank", "rbeta",
                    "rbinom", "rcauchy", "rf", "rgamma", "rlnorm", "rlogis", "rnbinom", "rnbinom_mu", "rnchisq", "rnorm", "runif", "rweibull", "rwilcox", "rhyper", "sample2", "format.info",
                    "abbreviate", "grepRaw", "regexec", "adist", "aregexec", "chartr", "intToBits", "rawToBits", "packBits", "utf8ToInt", "intToUtf8", "strtrim", "eapply", "machine", "save",
                    "saveToConn", "dput", "dump", "prmatrix", "gcinfo", "gctorture", "gctorture2", "memory.profile", "recordGraphics", "sys.calls", "sys.on.exit", "rank", "builtins", "bodyCode",
                    "rapply", "islistfactor", "inspect", "mem.limits", "merge", "capabilitiesX11", "Cstack_info", "file.show", "file.choose", "polyroot", "mkCode", "bcClose", "is.builtin.internal",
                    "disassemble", "bcVersion", "load.from.file", "save.to.file", "growconst", "putconst", "getconst", "enableJIT", "setNumMathThreads", "setMaxNumMathThreads", "isatty",
                    "isIncomplete", "pipe", "fifo", "bzfile", "xzfile", "unz", "truncate", "rawConnection", "rawConnectionValue", "sockSelect", "gzcon", "memCompress", "memDecompress", "mkUnbound",
                    "env.profile", "setTimeLimit", "setSessionTimeLimit", "icuSetCollate", "lazyLoadDBflush", "findInterval", "pretty", "crc64", "rowsum_matrix", "rowsum_df", "setS4Object",
                    "traceOnOff", "La_qr_cmplx", "La_rs", "La_rs_cmplx", "La_rg_cmplx", "La_rs", "La_rs_cmplx", "La_dlange", "La_dgecon", "La_dtrcon", "La_zgecon", "La_ztrcon", "La_solve_cmplx",
                    "La_chol2inv", "qr_qy_real", "qr_coef_cmplx", "qr_qy_cmpl", "La_svd", "La_svd_cmplx"};

}
