/*
 * Copyright (c) 2013, 2017, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.builtin.base;

import static com.oracle.truffle.r.runtime.RDispatch.INTERNAL_GENERIC;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.PURE;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.PRIMITIVE;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.InteropException;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.interop.java.JavaInterop;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.nodes.attributes.SpecialAttributesFunctions.GetNamesAttributeNode;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractContainer;
import com.oracle.truffle.r.runtime.env.REnvironment;

@ImportStatic({RRuntime.class, com.oracle.truffle.api.interop.Message.class})
@RBuiltin(name = "names", kind = PRIMITIVE, parameterNames = {"x"}, dispatch = INTERNAL_GENERIC, behavior = PURE)
public abstract class Names extends RBuiltinNode.Arg1 {

    private final ConditionProfile hasNames = ConditionProfile.createBinaryProfile();
    @Child private GetNamesAttributeNode getNames = GetNamesAttributeNode.create();

    static {
        Casts.noCasts(Names.class);
    }

    @Specialization
    protected Object getNames(RAbstractContainer container) {
        RStringVector names = getNames.getNames(container);
        if (hasNames.profile(names != null)) {
            return names;
        } else {
            return RNull.instance;
        }
    }

    @Specialization
    @TruffleBoundary
    protected Object getNames(REnvironment env) {
        return env.ls(true, null, false);
    }

    @Specialization(guards = "isForeignObject(obj)")
    protected Object getNames(TruffleObject obj,
                    @Cached("GET_SIZE.createNode()") Node getSizeNode,
                    @Cached("KEYS.createNode()") Node keysNode,
                    @Cached("READ.createNode()") Node readNode,
                    @Cached("IS_BOXED.createNode()") Node isBoxedNode,
                    @Cached("UNBOX.createNode()") Node unboxNode) {

        try {
            String[] names;
            try {
                names = readKeys(keysNode, obj, getSizeNode, readNode, isBoxedNode, unboxNode);
            } catch (UnsupportedMessageException e) {
                // because it is a java function, java.util.Map (has special handling too) ... ?
                return RNull.instance;
            }
            String[] staticNames = new String[0];
            try {
                if (JavaInterop.isJavaObject(Object.class, obj)) {
                    staticNames = readKeys(keysNode, toJavaClass(obj), getSizeNode, readNode, isBoxedNode, unboxNode);
                }
            } catch (UnknownIdentifierException | NoSuchFieldError | UnsupportedMessageException e) {
                // because it is a class ... ?
            }
            if (names.length == 0 && staticNames.length == 0) {
                return RNull.instance;
            }
            String[] result = new String[names.length + staticNames.length];
            System.arraycopy(names, 0, result, 0, names.length);
            System.arraycopy(staticNames, 0, result, names.length, staticNames.length);
            return RDataFactory.createStringVector(result, true);
        } catch (InteropException e) {
            throw RInternalError.shouldNotReachHere(e);
        }
    }

    @TruffleBoundary
    private static TruffleObject toJavaClass(TruffleObject obj) {
        return JavaInterop.toJavaClass(obj);
    }

    private static String[] readKeys(Node keysNode, TruffleObject obj, Node getSizeNode, Node readNode, Node isBoxedNode, Node unboxNode)
                    throws UnknownIdentifierException, InteropException, UnsupportedMessageException {
        TruffleObject keys = (TruffleObject) ForeignAccess.send(keysNode, obj);
        if (keys != null) {
            int size = (Integer) ForeignAccess.sendGetSize(getSizeNode, keys);
            String[] names = new String[size];
            for (int i = 0; i < size; i++) {
                Object value;
                value = ForeignAccess.sendRead(readNode, keys, i);
                if (value instanceof TruffleObject && ForeignAccess.sendIsBoxed(isBoxedNode, (TruffleObject) value)) {
                    value = ForeignAccess.sendUnbox(unboxNode, (TruffleObject) value);
                }
                names[i] = (String) value;
            }
            return names;
        }
        return new String[0];
    }

    @Fallback
    protected RNull getNames(@SuppressWarnings("unused") Object operand) {
        return RNull.instance;
    }
}
