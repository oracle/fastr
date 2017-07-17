/*
 * Copyright (c) 2017, 2017, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.interop.CanResolve;
import com.oracle.truffle.api.interop.MessageResolution;
import com.oracle.truffle.api.interop.Resolve;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.r.nodes.attributes.ArrayAttributeNode;
import com.oracle.truffle.r.nodes.attributes.GetAttributeNode;
import com.oracle.truffle.r.nodes.attributes.SetAttributeNode;
import com.oracle.truffle.r.runtime.data.RAttributesLayout.RAttribute;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RS4Object;

@MessageResolution(receiverType = RS4Object.class)
public class RS4ObjectMR {

    @Resolve(message = "IS_BOXED")
    public abstract static class RS4ObjectIsBoxedNode extends Node {
        protected Object access(@SuppressWarnings("unused") RS4Object receiver) {
            return false;
        }
    }

    @Resolve(message = "HAS_SIZE")
    public abstract static class RS4ObjectHasSizeNode extends Node {
        protected Object access(@SuppressWarnings("unused") RS4Object receiver) {
            return false;
        }
    }

    @Resolve(message = "IS_NULL")
    public abstract static class RS4ObjectIsNullNode extends Node {
        protected Object access(@SuppressWarnings("unused") RS4Object receiver) {
            return false;
        }
    }

    @Resolve(message = "READ")
    public abstract static class RS4ObjectReadNode extends Node {
        @Child private GetAttributeNode getAttributeNode = GetAttributeNode.create();

        protected Object access(RS4Object receiver, String field) {
            return getAttributeNode.execute(receiver, field);
        }
    }

    @Resolve(message = "WRITE")
    public abstract static class RS4ObjectWriteNode extends Node {
        @Child private SetAttributeNode setAttributeNode = SetAttributeNode.create();

        protected Object access(RS4Object receiver, String field, Object valueObj) {
            Object value = Utils.javaToRPrimitive(valueObj);
            setAttributeNode.execute(receiver, field, value);
            return value;
        }
    }

    @Resolve(message = "KEYS")
    public abstract static class RS4ObjectKeysNode extends Node {
        @Child private ArrayAttributeNode arrayAttrAccess = ArrayAttributeNode.create();

        protected Object access(RS4Object receiver) {
            RAttribute[] attributes = arrayAttrAccess.execute(receiver.getAttributes());
            String[] data = new String[attributes.length];
            for (int i = 0; i < data.length; i++) {
                data[i] = attributes[i].getName();
            }
            return RDataFactory.createStringVector(data, RDataFactory.COMPLETE_VECTOR);
        }
    }

    @CanResolve
    public abstract static class RS4ObjectCheck extends Node {
        protected static boolean test(TruffleObject receiver) {
            return receiver instanceof RS4Object;
        }
    }
}
