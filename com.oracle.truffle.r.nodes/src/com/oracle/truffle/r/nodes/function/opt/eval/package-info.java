/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 3 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 3 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 3 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

/**
 * This package contains classes used for optimization of function calls. The optimization is
 * employed in builtin {@code com.oracle.truffle.r.nodes.builtin.base.Eval} and upcall
 * {@code com.oracle.truffle.r.ffi.impl.nodes.RfEvalNode}. This documentation describes the
 * optimization pipeline that begins at the entry of the {@code RfEvalNode}, resp. {@code Eval}
 * node, and ends at the invocation of the instance of
 * {@code com.oracle.truffle.r.nodes.function.RCallNode}, which actually executes the function call.
 * <p>
 * <h3>{@link com.oracle.truffle.r.nodes.function.opt.eval.CallInfoNode CallInfoNode} and
 * {@link com.oracle.truffle.r.nodes.function.opt.eval.CallInfo CallInfo}</h3> The first node in the
 * optimization pipeline is {@link com.oracle.truffle.r.nodes.function.opt.eval.CallInfoNode
 * CallInfoNode}, which is responsible for determining whether the ongoing function call can be
 * optimized and if so, it creates a representation of the call encapsulated in
 * {@link com.oracle.truffle.r.nodes.function.opt.eval.CallInfo CallInfo}. The
 * {@link com.oracle.truffle.r.nodes.function.opt.eval.CallInfo CallInfo} instance is then passed
 * over to the next node in the pipeline. The
 * {@link com.oracle.truffle.r.nodes.function.opt.eval.CallInfoNode CallInfoNode} node examines the
 * input {@link com.oracle.truffle.r.runtime.data.RPairList language object} and tries to determine
 * if it can be evaluated as a function call. Currently, two situations are recognized: a simple
 * function call and a function call wrapped in a try-catch envelope. The latter case is there to
 * support function calls originating from the {@code Rcpp} package. Other situations may be
 * recognized and handled in the future.
 * <p>
 * {@link com.oracle.truffle.r.nodes.function.opt.eval.CallInfoNode CallInfoNode} creates the
 * {@link com.oracle.truffle.r.nodes.function.opt.eval.CallInfo CallInfo} instance via the helper
 * factory node {@link com.oracle.truffle.r.nodes.function.opt.eval.CallInfoFactoryNode
 * CallInfoFactoryNode}, which either extracts the function object from the input language object or
 * looks it up using the symbol stored as the LHS in the language object. Furthermore,
 * {@link com.oracle.truffle.r.nodes.function.opt.eval.CallInfoFactoryNode CallInfoFactoryNode} uses
 * a PIC for caching call info objects.
 * <h3>CachedCallInfoEvalNode etc.</h3> After a call info is supplied by
 * {@link com.oracle.truffle.r.nodes.function.opt.eval.CallInfoNode CallInfoNode}, it has to be
 * evaluated. Both {@code Eval} and {@code RfEvalNode} nodes implement their own evaluation node
 * named {@code CachedCallInfoEvalNode}. Both evaluation nodes inherit from
 * {@link com.oracle.truffle.r.nodes.function.opt.eval.AbstractCallInfoEvalNode
 * AbstractCallInfoEvalNode} encapsulating common helper methods and constants. (N.B. There are
 * subtle differences in how the nodes evaluate function calls making it difficult to move more code
 * that looks similar to the common parent.)
 * <p>
 * The main goal of {@code CachedCallInfoEvalNode} is to prepare function argument values from the
 * input language object and pass them over to a dedicated function execution node further down in
 * the pipeline. To prepare the arguments, {@code CachedCallInfoEvalNode} uses
 * {@link com.oracle.truffle.r.nodes.function.opt.eval.ArgValueSupplierNode ArgValueSupplierNode}
 * containing a number of specializations, including PIC, for individual argument types. {@code
 * CachedCallInfoEvalNode} also contains a PIC allowing caching
 * {@link com.oracle.truffle.r.nodes.function.opt.eval.ArgValueSupplierNode ArgValueSupplierNode}
 * nodes being specialized for a given argument. Such a caching is guarded by the
 * {@link com.oracle.truffle.r.nodes.function.opt.eval.CallInfo.CachedCallInfo CachedCallInfo}
 * object used to determine if the input call info matches a cache entry (using
 * {@link com.oracle.truffle.r.nodes.function.opt.eval.CallInfo.CachedCallInfo#isCompatible(CallInfo, com.oracle.truffle.api.profiles.ValueProfile)}
 * .
 * <p>
 * As soon as the PIC in {@code CachedCallInfoEvalNode} is full, arguments are prepared by
 * pre-allocated polymorphic
 * {@link com.oracle.truffle.r.nodes.function.opt.eval.ArgValueSupplierNode ArgValueSupplierNode}
 * nodes.
 * <h3>{@link com.oracle.truffle.r.nodes.function.opt.eval.CallInfoEvalRootNode
 * CallInfoEvalRootNode}</h3> As soon as the argument values are prepared,
 * {@code CachedCallInfoEvalNode} delegates the function evaluation to
 * {@link com.oracle.truffle.r.nodes.function.opt.eval.CallInfoEvalRootNode CallInfoEvalRootNode}.
 * The delegation goes through two alternative intermediary auxiliary nodes
 * {@link com.oracle.truffle.r.nodes.function.opt.eval.CallInfoEvalRootNode.FastPathDirectCallerNode
 * FastPathDirectCallerNode} or
 * {@link com.oracle.truffle.r.nodes.function.opt.eval.CallInfoEvalRootNode.SlowPathDirectCallerNode
 * SlowPathDirectCallerNode}, which prepare the frame for the use by the
 * {@link com.oracle.truffle.r.nodes.function.opt.eval.CallInfoEvalRootNode CallInfoEvalRootNode}.
 * {@link com.oracle.truffle.r.nodes.function.opt.eval.CallInfoEvalRootNode CallInfoEvalRootNode}
 * then delegates the execution to its child {@link com.oracle.truffle.r.nodes.function.RCallNode
 * RCallNode} node.
 */
package com.oracle.truffle.r.nodes.function.opt.eval;
