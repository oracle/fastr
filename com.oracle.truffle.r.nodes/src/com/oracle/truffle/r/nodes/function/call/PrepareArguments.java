/*
 * Copyright (c) 2016, 2017, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.function.call;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.r.nodes.RRootNode;
import com.oracle.truffle.r.nodes.function.ArgumentMatcher;
import com.oracle.truffle.r.nodes.function.ArgumentMatcher.MatchPermutation;
import com.oracle.truffle.r.nodes.function.CallArgumentsNode;
import com.oracle.truffle.r.nodes.function.FormalArguments;
import com.oracle.truffle.r.nodes.function.RCallNode;
import com.oracle.truffle.r.nodes.function.call.PrepareArgumentsFactory.PrepareArgumentsDefaultNodeGen;
import com.oracle.truffle.r.nodes.function.call.PrepareArgumentsFactory.PrepareArgumentsExplicitNodeGen;
import com.oracle.truffle.r.nodes.profile.TruffleBoundaryNode;
import com.oracle.truffle.r.runtime.Arguments;
import com.oracle.truffle.r.runtime.ArgumentsSignature;
import com.oracle.truffle.r.runtime.RArguments.S3DefaultArguments;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.SubstituteVirtualFrame;
import com.oracle.truffle.r.runtime.data.RArgsValuesAndNames;
import com.oracle.truffle.r.runtime.nodes.RNode;

/**
 * This class provides the infrastructure to reorder the arguments based on R's argument matching
 * rules. It implements two different paths: one for arguments provided as an
 * {@link CallArgumentsNode}, i.e., unevaluated arguments, and another path for evaluated arguments.
 */
public abstract class PrepareArguments extends Node {

    protected static final int CACHE_SIZE = 8;

    abstract static class PrepareArgumentsDefault extends PrepareArguments {

        protected final RRootNode target;
        protected final CallArgumentsNode sourceArguments; // not used as a node
        protected final boolean noOpt;

        static final class ArgumentsAndSignature extends Node {
            @Children private final RNode[] matchedArguments;
            private final ArgumentsSignature matchedSuppliedSignature;

            protected ArgumentsAndSignature(RNode[] matchedArguments, ArgumentsSignature matchedSuppliedSignature) {
                this.matchedArguments = matchedArguments;
                this.matchedSuppliedSignature = matchedSuppliedSignature;
            }
        }

        protected static ArgumentsSignature getSignatureOrNull(RArgsValuesAndNames args) {
            return args == null ? null : args.getSignature();
        }

        protected PrepareArgumentsDefault(RRootNode target, CallArgumentsNode sourceArguments, boolean noOpt) {
            this.target = target;
            this.sourceArguments = sourceArguments;
            this.noOpt = noOpt;
        }

        protected ArgumentsAndSignature createArguments(RCallNode call, ArgumentsSignature varArgSignature, S3DefaultArguments s3DefaultArguments) {
            Arguments<RNode> matched = ArgumentMatcher.matchArguments(target, sourceArguments, varArgSignature, s3DefaultArguments, call, noOpt);
            return new ArgumentsAndSignature(matched.getArguments(), matched.getSignature());
        }

        @ExplodeLoop
        private static RArgsValuesAndNames executeArgs(RNode[] arguments, ArgumentsSignature suppliedSignature, VirtualFrame frame) {
            Object[] result = new Object[arguments.length];
            for (int i = 0; i < arguments.length; i++) {
                Object value = arguments[i].execute(frame);
                if (CompilerDirectives.inInterpreter()) {
                    if (value == null) {
                        throw RInternalError.shouldNotReachHere("Java 'null' not allowed in arguments");
                    }
                }
                result[i] = value;
            }
            return new RArgsValuesAndNames(result, suppliedSignature);
        }

        @Specialization(limit = "CACHE_SIZE", guards = {"cachedVarArgSignature == null || cachedVarArgSignature == varArgs.getSignature()", "cachedS3DefaultArguments == s3DefaultArguments"})
        public RArgsValuesAndNames prepare(VirtualFrame frame, RArgsValuesAndNames varArgs, @SuppressWarnings("unused") S3DefaultArguments s3DefaultArguments,
                        @SuppressWarnings("unused") RCallNode call,
                        @Cached("getSignatureOrNull(varArgs)") ArgumentsSignature cachedVarArgSignature,
                        @Cached("createArguments(call, cachedVarArgSignature, s3DefaultArguments)") ArgumentsAndSignature arguments,
                        @SuppressWarnings("unused") @Cached("s3DefaultArguments") S3DefaultArguments cachedS3DefaultArguments) {
            assert (cachedVarArgSignature != null) == (varArgs != null);
            return executeArgs(arguments.matchedArguments, arguments.matchedSuppliedSignature, frame);
        }

        private static final class GenericCallEntry extends Node {
            private final ArgumentsSignature cachedVarArgSignature;
            private final S3DefaultArguments cachedS3DefaultArguments;

            @Child private ArgumentsAndSignature arguments;

            GenericCallEntry(ArgumentsSignature cachedVarArgSignature, S3DefaultArguments cachedS3DefaultArguments, ArgumentsAndSignature arguments) {
                this.cachedVarArgSignature = cachedVarArgSignature;
                this.cachedS3DefaultArguments = cachedS3DefaultArguments;
                this.arguments = arguments;
            }
        }

        /*
         * Use a TruffleBoundaryNode to be able to switch child nodes without invalidating the whole
         * method.
         */
        protected final class GenericCall extends TruffleBoundaryNode {

            @Child private GenericCallEntry entry;

            @TruffleBoundary
            public RArgsValuesAndNames execute(MaterializedFrame materializedFrame, S3DefaultArguments s3DefaultArguments, ArgumentsSignature varArgSignature, RCallNode call) {
                GenericCallEntry e = entry;
                if (e == null || e.cachedVarArgSignature != varArgSignature || e.cachedS3DefaultArguments != s3DefaultArguments) {
                    ArgumentsAndSignature arguments = createArguments(call, varArgSignature, s3DefaultArguments);
                    entry = e = insert(new GenericCallEntry(varArgSignature, s3DefaultArguments, arguments));
                }
                VirtualFrame frame = SubstituteVirtualFrame.create(materializedFrame);
                return executeArgs(e.arguments.matchedArguments, e.arguments.matchedSuppliedSignature, frame);
            }
        }

        protected GenericCall createGenericCall() {
            return new GenericCall();
        }

        @Specialization
        public RArgsValuesAndNames prepareGeneric(VirtualFrame frame, RArgsValuesAndNames varArgs, S3DefaultArguments s3DefaultArguments, RCallNode call,
                        @Cached("createGenericCall()") GenericCall generic) {
            ArgumentsSignature varArgSignature = varArgs == null ? null : varArgs.getSignature();
            return generic.execute(frame.materialize(), s3DefaultArguments, varArgSignature, call);
        }
    }

    abstract static class PrepareArgumentsExplicit extends PrepareArguments {

        protected final RRootNode target;
        private final FormalArguments formals;

        protected PrepareArgumentsExplicit(RRootNode target) {
            this.target = target;
            this.formals = target.getFormalArguments();
        }

        protected MatchPermutation createArguments(RCallNode call, ArgumentsSignature explicitArgSignature) {
            return ArgumentMatcher.matchArguments(explicitArgSignature, formals.getSignature(), call, target.getBuiltin());
        }

        @Specialization(limit = "CACHE_SIZE", guards = {"cachedExplicitArgSignature == explicitArgs.getSignature()"})
        public RArgsValuesAndNames prepare(RArgsValuesAndNames explicitArgs, S3DefaultArguments s3DefaultArguments, @SuppressWarnings("unused") RCallNode call,
                        @SuppressWarnings("unused") @Cached("explicitArgs.getSignature()") ArgumentsSignature cachedExplicitArgSignature,
                        @Cached("createArguments(call, cachedExplicitArgSignature)") MatchPermutation permutation) {
            return ArgumentMatcher.matchArgumentsEvaluated(permutation, explicitArgs.getArguments(), s3DefaultArguments, formals);
        }

        @Fallback
        @TruffleBoundary
        public RArgsValuesAndNames prepareGeneric(RArgsValuesAndNames explicitArgs, S3DefaultArguments s3DefaultArguments, @SuppressWarnings("unused") RCallNode call) {
            // Function and arguments may change every call: Flatt'n'Match on SlowPath! :-/
            return ArgumentMatcher.matchArgumentsEvaluated(target, explicitArgs, s3DefaultArguments, RError.ROOTNODE);
        }
    }

    /**
     * Returns the argument values and corresponding signature. The signature represents the
     * original call signature reordered in the same way as the arguments. For s3DefaultArguments
     * motivation see {@link RCallNode#callGroupGeneric}.
     */
    public abstract RArgsValuesAndNames execute(VirtualFrame frame, RArgsValuesAndNames varArgs, S3DefaultArguments s3DefaultArguments, RCallNode call);

    public static PrepareArguments create(RRootNode target, CallArgumentsNode args, boolean noOpt) {
        return PrepareArgumentsDefaultNodeGen.create(target, args, noOpt);
    }

    public static PrepareArguments createExplicit(RRootNode target) {
        return PrepareArgumentsExplicitNodeGen.create(target);
    }
}
