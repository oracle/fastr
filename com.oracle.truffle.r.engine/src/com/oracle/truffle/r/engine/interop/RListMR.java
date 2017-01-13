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
import com.oracle.truffle.r.nodes.access.vector.ReplaceVectorNode;
import com.oracle.truffle.r.nodes.attributes.SpecialAttributesFunctions.GetNamesAttributeNode;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.context.RContext.RCloseable;
import com.oracle.truffle.r.runtime.data.RList;

@MessageResolution(receiverType = RList.class, language = TruffleRLanguage.class)
public class RListMR {

    @Resolve(message = "IS_BOXED")
    public abstract static class RListIsBoxedNode extends Node {
        protected Object access(@SuppressWarnings("unused") RList receiver) {
            return false;
        }
    }

    @Resolve(message = "HAS_SIZE")
    public abstract static class RListHasSizeNode extends Node {
        protected Object access(@SuppressWarnings("unused") RList receiver) {
            return true;
        }
    }

    @Resolve(message = "IS_NULL")
    public abstract static class RListIsNullNode extends Node {
        protected Object access(@SuppressWarnings("unused") RList receiver) {
            return false;
        }
    }

    @Resolve(message = "READ")
    public abstract static class RListReadNode extends Node {
        @Child private ExtractVectorNode extract = ExtractVectorNode.create(ElementAccessMode.SUBSCRIPT, true);
        @Child private Node findContext = TruffleRLanguage.INSTANCE.actuallyCreateFindContextNode();

        @SuppressWarnings("try")
        protected Object access(VirtualFrame frame, RList receiver, String field) {
            try (RCloseable c = RContext.withinContext(TruffleRLanguage.INSTANCE.actuallyFindContext0(findContext))) {
                return extract.applyAccessField(frame, receiver, field);
            }
        }
    }

    @Resolve(message = "WRITE")
    public abstract static class RListWriteNode extends Node {
        @Child private ReplaceVectorNode extract = ReplaceVectorNode.create(ElementAccessMode.SUBSCRIPT, true);
        @Child private Node findContext = TruffleRLanguage.INSTANCE.actuallyCreateFindContextNode();

        @SuppressWarnings("try")
        protected Object access(VirtualFrame frame, RList receiver, String field, Object valueObj) {
            try (RCloseable c = RContext.withinContext(TruffleRLanguage.INSTANCE.actuallyFindContext0(findContext))) {
                Object value = valueObj;
                if (value instanceof Short) {
                    value = (int) ((Short) value).shortValue();
                } else if (value instanceof Float) {
                    float floatValue = ((Float) value).floatValue();
                    value = new Double(floatValue);
                } else if (value instanceof Boolean) {
                    boolean booleanValue = ((Boolean) value).booleanValue();
                    value = booleanValue ? RRuntime.LOGICAL_TRUE : RRuntime.LOGICAL_FALSE;
                } else if (value instanceof Character) {
                    value = (int) ((Character) value).charValue();
                } else if (value instanceof Byte) {
                    value = (int) ((Byte) value).byteValue();
                }
                Object x = extract.apply(frame, receiver, new Object[]{field}, value);
                return x;
            }
        }
    }

    @Resolve(message = "KEYS")
    public abstract static class RListKeysNode extends Node {
        @Child private Node findContext = TruffleRLanguage.INSTANCE.actuallyCreateFindContextNode();
        @Child private GetNamesAttributeNode getNamesNode = GetNamesAttributeNode.create();

        @SuppressWarnings("try")
        protected Object access(RList receiver) {
            try (RCloseable c = RContext.withinContext(TruffleRLanguage.INSTANCE.actuallyFindContext0(findContext))) {
                return getNamesNode.getNames(receiver);
            }
        }
    }

    @CanResolve
    public abstract static class RListCheck extends Node {

        protected static boolean test(TruffleObject receiver) {
            return receiver instanceof RList;
        }
    }

}
