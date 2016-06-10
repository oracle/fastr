/*
 * Copyright (c) 2016, 2016, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.engine.interop;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.CanResolve;
import com.oracle.truffle.api.interop.MessageResolution;
import com.oracle.truffle.api.interop.Resolve;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.r.engine.TruffleRLanguage;
import com.oracle.truffle.r.nodes.access.vector.ElementAccessMode;
import com.oracle.truffle.r.nodes.access.vector.ExtractVectorNode;
import com.oracle.truffle.r.nodes.control.RLengthNode;
import com.oracle.truffle.r.runtime.data.RLogical;
import com.oracle.truffle.r.runtime.data.RMissing;
import com.oracle.truffle.r.runtime.data.RVector;

@MessageResolution(receiverType = RVector.class, language = TruffleRLanguage.class)
public class RVectorMR {
    @Resolve(message = "IS_BOXED")
    public abstract static class RVectorIsBoxedNode extends Node {
        protected Object access(@SuppressWarnings("unused") RVector receiver) {
            return false;
        }
    }

    @Resolve(message = "HAS_SIZE")
    public abstract static class RVectorHasSizeNode extends Node {
        protected Object access(@SuppressWarnings("unused") RVector receiver) {
            return true;
        }
    }

    @Resolve(message = "GET_SIZE")
    public abstract static class RVectorGetSizeNode extends Node {
        @Child private RLengthNode lengthNode = RLengthNode.create();

        protected Object access(VirtualFrame frame, RVector receiver) {
            return lengthNode.executeInteger(frame, receiver);
        }
    }

    @Resolve(message = "IS_NULL")
    public abstract static class RVectorIsNullNode extends Node {
        protected Object access(@SuppressWarnings("unused") RVector receiver) {
            return false;
        }
    }

    @Resolve(message = "READ")
    public abstract static class RVectorReadNode extends Node {
        @Child private ExtractVectorNode extract = ExtractVectorNode.create(ElementAccessMode.SUBSCRIPT, true);

        protected Object access(VirtualFrame frame, RVector receiver, String field) {
            return extract.applyAccessField(frame, receiver, field);
        }

        protected Object access(VirtualFrame frame, RVector receiver, Integer index) {
            return extract.apply(frame, receiver, new Object[]{index}, RLogical.valueOf(false), RMissing.instance);
        }
    }

    @CanResolve
    public abstract static class RVectorCheck extends Node {

        protected static boolean test(TruffleObject receiver) {
            return receiver instanceof RVector;
        }
    }

}
