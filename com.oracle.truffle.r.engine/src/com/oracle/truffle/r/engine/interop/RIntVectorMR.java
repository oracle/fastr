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
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.RLogical;

@MessageResolution(receiverType = RIntVector.class, language = TruffleRLanguage.class)
public class RIntVectorMR {
    @Resolve(message = "IS_BOXED")
    public abstract static class RIntVectorIsBoxedNode extends Node {
        protected Object access(@SuppressWarnings("unused") RIntVector receiver) {
            return false;
        }
    }

    @Resolve(message = "HAS_SIZE")
    public abstract static class RIntVectorHasSizeNode extends Node {
        protected Object access(@SuppressWarnings("unused") RIntVector receiver) {
            return true;
        }
    }

    @Resolve(message = "GET_SIZE")
    public abstract static class RIntVectorGetSizeNode extends Node {
        @Child private RLengthNode lengthNode = RLengthNode.create();

        protected Object access(VirtualFrame frame, RIntVector receiver) {
            return lengthNode.executeInteger(frame, receiver);
        }
    }

    @Resolve(message = "IS_NULL")
    public abstract static class RIntVectorIsNullNode extends Node {
        protected Object access(@SuppressWarnings("unused") RIntVector receiver) {
            return false;
        }
    }

    @Resolve(message = "READ")
    public abstract static class RIntVectorReadNode extends Node {
        @Child private ExtractVectorNode extract = ExtractVectorNode.create(ElementAccessMode.SUBSCRIPT, true);

        protected Object access(VirtualFrame frame, RIntVector receiver, String field) {
            return extract.applyAccessField(frame, receiver, field);
        }

        protected Object access(VirtualFrame frame, RIntVector receiver, Integer index) {
            return extract.apply(frame, receiver, new Object[]{index}, RLogical.TRUE, RLogical.TRUE);
        }
    }

    @CanResolve
    public abstract static class RIntVectorCheck extends Node {

        protected static boolean test(TruffleObject receiver) {
            return receiver instanceof RIntVector;
        }
    }

}
