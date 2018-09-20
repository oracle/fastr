/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 3 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 3 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 3 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.oracle.truffle.r.runtime.interop;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.InteropException;
import com.oracle.truffle.api.interop.Message;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;

@ImportStatic({RRuntime.class, Message.class})
public abstract class GetForeignKeysNode extends RBaseNode {

    @Child private Node executeNode;
    @Child private Node hasSizeNode;

    public static GetForeignKeysNode create() {
        return GetForeignKeysNodeGen.create();
    }

    public abstract Object execute(Object obj, boolean acceptJavaStatic);

    @Specialization(guards = "isForeignObject(obj)")
    protected Object getKeys(TruffleObject obj, boolean acceptJavaStatic,
                    @Cached("GET_SIZE.createNode()") Node getSizeNode,
                    @Cached("KEYS.createNode()") Node keysNode,
                    @Cached("READ.createNode()") Node readNode) {

        try {
            String[] staticNames = new String[0];
            RContext context = RContext.getInstance();
            TruffleLanguage.Env env = context.getEnv();
            if (acceptJavaStatic && env.isHostObject(obj) && !(env.asHostObject(obj) instanceof Class)) {
                if (hasSizeNode == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    hasSizeNode = insert(Message.HAS_SIZE.createNode());
                }
                if (ConvertForeignObjectNode.isForeignArray(obj, hasSizeNode)) {
                    // got no names for a truffle array
                    return RNull.instance;
                }

                if (executeNode == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    executeNode = insert(Message.EXECUTE.createNode());
                }
                try {
                    TruffleObject clazzStatic = context.toJavaStatic(obj, readNode, executeNode);
                    staticNames = readKeys(keysNode, clazzStatic, getSizeNode, readNode);
                } catch (UnknownIdentifierException | NoSuchFieldError | UnsupportedMessageException e) {
                }
            }

            String[] names;
            try {
                names = readKeys(keysNode, obj, getSizeNode, readNode);
            } catch (UnsupportedMessageException e) {
                // because it is a java function, java.util.Map (has special handling too) ... ?
                return RNull.instance;
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

    private static String[] readKeys(Node keysNode, TruffleObject obj, Node getSizeNode, Node readNode)
                    throws UnknownIdentifierException, InteropException, UnsupportedMessageException {
        TruffleObject keys = ForeignAccess.sendKeys(keysNode, obj);
        int size = (Integer) ForeignAccess.sendGetSize(getSizeNode, keys);
        String[] names = new String[size];
        for (int i = 0; i < size; i++) {
            Object value = ForeignAccess.sendRead(readNode, keys, i);
            names[i] = (String) value;
        }
        return names;
    }

    @Fallback
    public Object doObject(@SuppressWarnings("unused") Object obj, @SuppressWarnings("unused") boolean acceptJavaStatic) {
        return RInternalError.shouldNotReachHere();
    }

}
