/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.runtime.interop;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.Message;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;

@ImportStatic({RRuntime.class})
public abstract class Foreign2R extends RBaseNode {

    @Child private Foreign2R recursive;
    @Child private Node isNull;
    @Child private Node isBoxed;
    @Child private Node unbox;

    public static Foreign2R create() {
        return Foreign2RNodeGen.create();
    }

    public abstract Object execute(Object obj);

    @Specialization
    public byte doBoolean(boolean obj) {
        return RRuntime.asLogical(obj);
    }

    @Specialization
    public int doByte(byte obj) {
        return ((Byte) obj).intValue();
    }

    @Specialization
    public int doShort(short obj) {
        return ((Short) obj).intValue();
    }

    @Specialization
    public double doLong(long obj) {
        return (((Long) obj).doubleValue());
    }

    @Specialization
    public double doFloat(float obj) {
        return (((Float) obj).doubleValue());
    }

    @Specialization
    public String doChar(char obj) {
        return ((Character) obj).toString();
    }

    @Specialization(guards = "isNull(obj)")
    public RNull doNull(@SuppressWarnings("unused") Object obj) {
        return RNull.instance;
    }

    @Specialization(guards = "isForeignObject(obj)")
    public Object doForeignObject(TruffleObject obj) {
        if (isNull == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            isNull = insert(Message.IS_NULL.createNode());
        }
        if (ForeignAccess.sendIsNull(isNull, obj)) {
            return RNull.instance;
        }

        /*
         * For the time being, we have to ask "IS_BOXED" all the time (instead of simply trying
         * UNBOX first), because some TruffleObjects return bogus values from UNBOX when IS_BOXED is
         * false.
         */
        if (isBoxed == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            isBoxed = insert(Message.IS_BOXED.createNode());
        }
        if (ForeignAccess.sendIsBoxed(isBoxed, obj)) {
            if (unbox == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                unbox = insert(Message.UNBOX.createNode());
            }
            try {
                Object unboxed = ForeignAccess.sendUnbox(unbox, obj);
                if (recursive == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    recursive = insert(Foreign2RNodeGen.create());
                }
                return recursive.execute(unboxed);
            } catch (UnsupportedMessageException e) {
                CompilerDirectives.transferToInterpreter();
                throw RInternalError.shouldNotReachHere(e, "object does not support UNBOX message even though IS_BOXED == true: " + obj.getClass().getSimpleName());
            }
        }
        return obj;
    }

    @Fallback
    public Object doObject(Object obj) {
        return obj;
    }

    protected boolean isNull(Object o) {
        return o == null;
    }
}
