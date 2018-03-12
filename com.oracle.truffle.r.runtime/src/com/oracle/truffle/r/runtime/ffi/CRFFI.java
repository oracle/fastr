/*
 * Copyright (c) 2014, 2018, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.runtime.ffi;

import com.oracle.truffle.r.runtime.ArgumentsSignature;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.RArgsValuesAndNames;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.ffi.UnwrapVectorNode.UnwrapVectorsNode;
import com.oracle.truffle.r.runtime.ffi.UnwrapVectorNodeGen.UnwrapVectorsNodeGen;
import com.oracle.truffle.r.runtime.ffi.WrapVectorNode.WrapVectorsNode;
import com.oracle.truffle.r.runtime.ffi.WrapVectorNodeGen.WrapVectorsNodeGen;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;

/**
 * Support for the {.C} and {.Fortran} calls. Arguments of these calls are only arrays of primitive
 * types, in the case character vectors, only the first string. The vectors coming from the R side
 * are duplicated (if not temporary) with all their attributes and then the pointer to the data of
 * the new fresh vectors is passed to the function. The result is a list of all those new vectors
 * (or the original vectors if they are temporary).
 *
 * Note: seems that symbols in GnuR may declare: expected types of their args (and other types
 * should be coerced), whether an argument is only input (RNull is in its place in the result list)
 * and whether the argument value must always be copied. We do not implement those as they do not
 * seem necessary?
 */
public interface CRFFI {

    abstract class InvokeCNode extends RBaseNode {

        @Child WrapVectorsNode argsWrapperNode = WrapVectorsNodeGen.create();
        @Child UnwrapVectorsNode argsUnwrapperNode = UnwrapVectorsNodeGen.create();

        /**
         * Invoke the native method identified by {@code symbolInfo} passing it the arguments in
         * {@code args}. The values in {@code args} should be support the IS_POINTER/AS_POINTER
         * messages.
         */
        protected abstract void execute(NativeCallInfo nativeCallInfo, Object[] args);

        public final RList dispatch(NativeCallInfo nativeCallInfo, byte naok, byte dup, RArgsValuesAndNames args) {
            @SuppressWarnings("unused")
            boolean dupArgs = RRuntime.fromLogical(dup);
            @SuppressWarnings("unused")
            boolean checkNA = RRuntime.fromLogical(naok);

            VectorRFFIWrapper[] preparedArgs = argsWrapperNode.execute(args.getArguments());

            RFFIContext stateRFFI = RContext.getInstance().getStateRFFI();
            long before = stateRFFI.beforeDowncall();
            try {
                execute(nativeCallInfo, preparedArgs);
                return RDataFactory.createList(argsUnwrapperNode.execute(preparedArgs), validateArgNames(preparedArgs.length, args.getSignature()));
            } finally {
                stateRFFI.afterDowncall(before);
            }
        }

        private static RStringVector validateArgNames(int argsLength, ArgumentsSignature signature) {
            String[] listArgNames = new String[argsLength];
            for (int i = 0; i < argsLength; i++) {
                String name = signature.getName(i);
                if (name == null) {
                    name = RRuntime.NAMES_ATTR_EMPTY_VALUE;
                }
                listArgNames[i] = name;
            }
            return RDataFactory.createStringVector(listArgNames, RDataFactory.COMPLETE_VECTOR);
        }
    }

    InvokeCNode createInvokeCNode();
}
