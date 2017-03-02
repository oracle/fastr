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
package com.oracle.truffle.r.ffi.impl.nodes;

import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.dsl.TypeSystemReference;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.data.RLanguage;
import com.oracle.truffle.r.runtime.data.RPairList;
import com.oracle.truffle.r.runtime.data.RTypes;

/**
 * Nodes that implement {@code CAR}, {@code CDR}, etc. N.B. GNU R does not error check the
 * arguments; it will crash (segv) if given, say, a numeric arg.
 */
public final class ListAccessNodes {

    @TypeSystemReference(RTypes.class)
    public abstract static class CARNode extends FFIUpCallNode.Arg1 {
        @Specialization
        protected Object car(RPairList pl) {
            return pl.car();
        }

        @Specialization
        protected Object car(RLanguage lang) {
            return lang.getDataAtAsObject(0);
        }

        @Fallback
        protected Object car(@SuppressWarnings("unused") Object obj) {
            throw RInternalError.unimplemented("CAR only works on pair lists and language objects");
        }

    }

    @TypeSystemReference(RTypes.class)
    public abstract static class CDRNode extends FFIUpCallNode.Arg1 {
        @Specialization
        protected Object cdr(RPairList pl) {
            return pl.cdr();
        }

        @Specialization
        protected Object cdr(RLanguage lang) {
            RPairList l = lang.getPairList();
            return l.cdr();
        }

        @Fallback
        protected Object cdr(@SuppressWarnings("unused") Object obj) {
            throw RInternalError.unimplemented("CDR only works on pair lists and language objects");

        }
    }

    @TypeSystemReference(RTypes.class)
    public abstract static class CADRNode extends FFIUpCallNode.Arg1 {
        @Specialization
        protected Object cadr(RPairList pl) {
            return pl.cadr();
        }

        @Specialization
        protected Object cadr(RLanguage lang) {
            return lang.getDataAtAsObject(1);
        }

        @Fallback
        protected Object cadr(@SuppressWarnings("unused") Object obj) {
            throw RInternalError.unimplemented("CADR only works on pair lists and language objects");
        }

    }

    @TypeSystemReference(RTypes.class)
    public abstract static class CADDRNode extends FFIUpCallNode.Arg1 {
        @Specialization
        protected Object caddr(RPairList pl) {
            return pl.caddr();
        }

        @Specialization
        protected Object caddr(RLanguage lang) {
            return lang.getDataAtAsObject(2);
        }

        @Fallback
        protected Object caddr(@SuppressWarnings("unused") Object obj) {
            throw RInternalError.unimplemented("CADDR only works on pair lists and language objects");
        }

    }

    @TypeSystemReference(RTypes.class)
    public abstract static class CDDRNode extends FFIUpCallNode.Arg1 {
        @Specialization
        protected Object cddr(RPairList pl) {
            return pl.cddr();
        }

        @Specialization
        protected Object cdr(RLanguage lang) {
            RPairList l = lang.getPairList();
            return l.cddr();
        }

        @Fallback
        protected Object cddr(@SuppressWarnings("unused") Object obj) {
            throw RInternalError.unimplemented("CDDR only works on pair lists and language objects");

        }
    }
}
