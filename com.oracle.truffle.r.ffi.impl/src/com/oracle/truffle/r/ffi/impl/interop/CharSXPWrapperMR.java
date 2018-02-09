/*
 * Copyright (c) 2017, 2018, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.ffi.impl.interop;

import com.oracle.truffle.api.interop.CanResolve;
import com.oracle.truffle.api.interop.MessageResolution;
import com.oracle.truffle.api.interop.Resolve;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.data.CharSXPWrapper;
import com.oracle.truffle.r.runtime.data.RObject;
import com.oracle.truffle.r.runtime.interop.RObjectNativeWrapper;

@MessageResolution(receiverType = CharSXPWrapper.class)
public class CharSXPWrapperMR {

    @Resolve(message = "IS_POINTER")
    public abstract static class IsPointerNode extends Node {
        protected boolean access(@SuppressWarnings("unused") Object receiver) {
            return false;
        }
    }

    @Resolve(message = "TO_NATIVE")
    public abstract static class ToNativeNode extends Node {
        protected Object access(RObject receiver) {
            return new RObjectNativeWrapper(receiver);
        }
    }

    @Resolve(message = "HAS_SIZE")
    public abstract static class NCAHasSizeNode extends Node {
        protected boolean access(@SuppressWarnings("unused") NativeCharArray receiver) {
            return true;
        }
    }

    @Resolve(message = "GET_SIZE")
    public abstract static class GetSizeNode extends Node {
        protected Object access(CharSXPWrapper receiver) {
            return receiver.getLength();
        }
    }

    @Resolve(message = "READ")
    public abstract static class ReadNode extends Node {
        private final ConditionProfile prof1 = ConditionProfile.createBinaryProfile();
        private final ConditionProfile prof2 = ConditionProfile.createBinaryProfile();

        protected Object access(CharSXPWrapper receiver, Number indexNum) {
            int index = indexNum.intValue();
            int len = receiver.getLength();
            if (prof1.profile(index < len)) {
                return receiver.getByteAt(index);
            } else if (prof2.profile(index == len)) {
                return 0;
            } else {
                throw RInternalError.shouldNotReachHere();
            }
        }
    }

    /**
     * The <code>EXECUTABLE</code> message is used to extract the string wrapped in
     * {@link CharSXPWrapper}. It is called only from the LLVM version of the
     * <code>ensure_string</code> function in
     * <code>com.oracle.truffle.r.native/fficall/src/truffle_llvm/Rinternals.c</code>.
     */
    @Resolve(message = "EXECUTE")
    public abstract static class NCAToStringNode extends Node {

        protected java.lang.Object access(CharSXPWrapper receiver, @SuppressWarnings("unused") Object[] arguments) {
            return receiver.getContents();
        }
    }

    @Resolve(message = "IS_EXECUTABLE")
    public abstract static class NCAToStringIsExecutableNode extends Node {
        protected Object access(@SuppressWarnings("unused") CharSXPWrapper receiver) {
            return true;
        }
    }

    @CanResolve
    public abstract static class CharSXPWrapperCheck extends Node {

        protected static boolean test(TruffleObject receiver) {
            return receiver instanceof CharSXPWrapper;
        }
    }
}
