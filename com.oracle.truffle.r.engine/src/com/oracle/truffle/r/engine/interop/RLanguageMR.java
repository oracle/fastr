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
package com.oracle.truffle.r.engine.interop;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.CanResolve;
import com.oracle.truffle.api.interop.Message;
import com.oracle.truffle.api.interop.MessageResolution;
import com.oracle.truffle.api.interop.Resolve;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.r.engine.TruffleRLanguage;
import com.oracle.truffle.r.nodes.access.vector.ElementAccessMode;
import com.oracle.truffle.r.nodes.access.vector.ExtractVectorNode;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.context.RContext.RCloseable;
import com.oracle.truffle.r.runtime.data.RLanguage;
import com.oracle.truffle.r.runtime.data.RLogical;
import com.oracle.truffle.r.runtime.data.RNull;

@MessageResolution(receiverType = RLanguage.class, language = TruffleRLanguage.class)
public class RLanguageMR {

    @Resolve(message = "IS_BOXED")
    public abstract static class RLanguageIsBoxedNode extends Node {
        protected Object access(@SuppressWarnings("unused") RLanguage receiver) {
            return false;
        }
    }

    @Resolve(message = "HAS_SIZE")
    public abstract static class RLanguageHasSizeNode extends Node {
        protected Object access(@SuppressWarnings("unused") RLanguage receiver) {
            return true;
        }
    }

    @Resolve(message = "IS_NULL")
    public abstract static class RLanguageIsNullNode extends Node {
        protected Object access(@SuppressWarnings("unused") RLanguage receiver) {
            return false;
        }
    }

    @Resolve(message = "READ")
    public abstract static class RLanguageReadNode extends Node {
        @Child private ExtractVectorNode extract = ExtractVectorNode.create(ElementAccessMode.SUBSCRIPT, true);
        @Child private Node findContext = TruffleRLanguage.INSTANCE.actuallyCreateFindContextNode();

        protected Object access(VirtualFrame frame, RLanguage receiver, int label) {
            try (RCloseable c = RContext.withinContext(TruffleRLanguage.INSTANCE.actuallyFindContext0(findContext))) {
                return extract.apply(frame, receiver, new Object[]{label + 1}, RLogical.TRUE, RLogical.TRUE);
            }
        }
    }

    @Resolve(message = "WRITE")
    public abstract static class RLanguageWriteNode extends Node {

        @SuppressWarnings("unused")
        protected Object access(VirtualFrame frame, RLanguage receiver, int label, Object valueObj) {
            throw UnsupportedMessageException.raise(Message.WRITE);
        }
    }

    @Resolve(message = "KEYS")
    public abstract static class RLanguageKeysNode extends Node {

        protected Object access(@SuppressWarnings("unused") RLanguage receiver) {
            return RNull.instance;
        }
    }

    @Resolve(message = "KEY_INFO")
    public abstract static class RLanguageKeyInfoNode extends Node {

        @SuppressWarnings("unused")
        protected Object access(VirtualFrame frame, RLanguage receiver, String identifier) {
            return 0;
        }
    }

    @CanResolve
    public abstract static class RLanguageCheck extends Node {

        protected static boolean test(TruffleObject receiver) {
            return receiver instanceof RLanguage;
        }
    }
}
