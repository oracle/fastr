/*
 * Copyright (c) 2018, 2020, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.builtin.base;

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.constant;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.missingValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.nullValue;
import static com.oracle.truffle.r.runtime.RVisibility.OFF;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.IO;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.INTERNAL;

import java.io.IOException;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.r.runtime.data.nodes.attributes.GetAttributesNode;
import com.oracle.truffle.r.runtime.data.nodes.attributes.SpecialAttributesFunctions;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.builtin.casts.fluent.CastNodeBuilder;
import com.oracle.truffle.r.runtime.nodes.unary.CastNode;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.conn.StdConnections;
import com.oracle.truffle.r.runtime.data.CharSXPWrapper;
import com.oracle.truffle.r.runtime.data.RArgsValuesAndNames;
import com.oracle.truffle.r.runtime.data.RAttributable;
import com.oracle.truffle.r.runtime.data.RBaseObject;
import com.oracle.truffle.r.runtime.data.RPairList;
import com.oracle.truffle.r.runtime.data.RRaw;
import com.oracle.truffle.r.runtime.data.RSharingAttributeStorage;
import com.oracle.truffle.r.runtime.data.model.RAbstractListBaseVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.env.REnvironment;
import com.oracle.truffle.r.runtime.gnur.SEXPTYPE;

/**
 * Internal inspect builtin.
 */
@RBuiltin(name = "inspect", visibility = OFF, kind = INTERNAL, parameterNames = {"obj", "..."}, behavior = IO)
public abstract class Inspect extends RBuiltinNode.Arg2 {

    static {
        Casts.noCasts(Inspect.class);
    }

    private static final int DEEP_DEFAULT = -1;
    private static final int PVEC_DEFAULT = 5;

    @Child private SpecialAttributesFunctions.GetClassAttributeNode getClassNode = SpecialAttributesFunctions.GetClassAttributeNode.create();
    @Child private GetAttributesNode getAttrsNode = GetAttributesNode.create();

    @Child private CastNode castDeepNode;
    @Child private CastNode castPvecNode;

    @Specialization
    @TruffleBoundary
    protected Object inspect(Object obj, RArgsValuesAndNames threeDots) {
        int deep = DEEP_DEFAULT;
        if (threeDots.getLength() > 0) {
            if (castDeepNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                castDeepNode = insert(CastNodeBuilder.newCastBuilder().mapIf(missingValue().or(nullValue()), constant(DEEP_DEFAULT)).asIntegerVector().findFirst().buildCastNode());
            }
            try {
                deep = (int) castDeepNode.doCast(threeDots.getArgument(1));
            } catch (Throwable t) {
                // Leave default value
            }
        }
        int pvec = PVEC_DEFAULT;
        if (threeDots.getLength() > 1) {
            if (castPvecNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                castPvecNode = insert(CastNodeBuilder.newCastBuilder().mapIf(missingValue().or(nullValue()), constant(PVEC_DEFAULT)).asIntegerVector().findFirst().buildCastNode());
            }
            try {
                pvec = (int) castPvecNode.doCast(threeDots.getArgument(1));
            } catch (Throwable t) {
                // Leave default value
            }
        }
        StringBuilder sb = new StringBuilder(64);
        inspectTree(sb, 0, null, obj, deep, pvec);
        echoOutput(sb.toString());
        return obj;
    }

    private void inspectTree(StringBuilder sb, int indent, String label, Object obj, int deep, int pvec) {
        indent(sb, indent);
        if (label != null) {
            sb.append(label);
        }
        sb.append('@').append(System.identityHashCode(obj));
        SEXPTYPE type = SEXPTYPE.typeForClass(obj);
        if (obj instanceof String) {
            type = SEXPTYPE.CHARSXP;
        }
        sb.append(' ').append(type.code).append(' ').append(type.name());
        sb.append(" [");
        boolean isObject = (RSharingAttributeStorage.isShareable(obj)) ? getClassNode.isObject((RSharingAttributeStorage) obj) : false;
        if (isObject) {
            sb.append("OBJ");
        }
        sb.append("] ");
        if (obj instanceof String) {
            sb.append('"').append(obj).append("\"\n");
        } else if (obj instanceof CharSXPWrapper) {
            int typeinfo = ((CharSXPWrapper) obj).getTypedValueInfo();
            if (typeinfo == RBaseObject.BYTES_MASK) {
                sb.append("[bytes] ");
            }
            if (typeinfo == RBaseObject.LATIN1_MASK) {
                sb.append("[latin1] ");
            }
            if (typeinfo == RBaseObject.UTF8_MASK) {
                sb.append("[UTF8] ");
            }
            if (typeinfo == RBaseObject.CACHED_MASK) {
                sb.append("[cached] ");
            }
            if (typeinfo == RBaseObject.ASCII_MASK) {
                sb.append("[ASCII] ");
            }
            sb.append('"').append(((CharSXPWrapper) obj).getContents()).append("\"\n");
        } else if (obj instanceof Double) {
            // GNU R uses Rprintf("%g",val) but String.format("%g", val) differs
            sb.append(obj);
            sb.append('\n');
        } else if (obj instanceof Integer || obj instanceof Byte) {
            sb.append(obj);
            sb.append('\n');
        } else if (obj instanceof REnvironment) {
            REnvironment env = (REnvironment) obj;
            sb.append('\n');
            inspectTree(sb, indent + 2, null, env.ls(true, null, true), deep - 1, pvec);
        } else if (obj instanceof RPairList) {
            RPairList pairList = (RPairList) obj;
            sb.append('\n');
            while (true) {
                Object tag = pairList.getTag();
                if (tag != null) {
                    inspectTree(sb, indent + 2, "TAG: ", tag, deep - 1, pvec);
                }
                inspectTree(sb, indent + 2, null, pairList.car(), deep - 1, pvec);
                Object cdr = pairList.cdr();
                if (cdr instanceof RPairList) {
                    pairList = (RPairList) cdr;
                } else {
                    break;
                }
            }
        } else if (obj instanceof RAbstractListBaseVector) {
            sb.append('\n');
            if (deep != 0) {
                RAbstractListBaseVector l = (RAbstractListBaseVector) obj;
                int len = l.getLength();
                if (len > 0) {
                    int i;
                    int pLen = Math.min(len, pvec);
                    for (i = 0; i < pLen; i++) {
                        inspectTree(sb, indent + 2, null, l.getDataAt(i), deep - 1, pvec);
                    }
                    if (i < len) {
                        sb.append(",...");
                    }
                }
            }
        } else if (obj instanceof RAbstractStringVector) {
            sb.append('\n');
            if (deep != 0) {
                RAbstractStringVector v = (RAbstractStringVector) obj;
                int len = v.getLength();
                if (len > 0) {
                    int i;
                    int pLen = Math.min(len, pvec);
                    for (i = 0; i < pLen; i++) {
                        inspectTree(sb, indent + 2, null, v.getDataAt(i), deep - 1, pvec);
                    }
                }
            }
        } else if (obj instanceof RAbstractVector) {
            RAbstractVector v = (RAbstractVector) obj;
            int len = v.getLength();
            sb.append("(len=").append(len).append(",tl=").append(len).append(")");
            if (len > 0) {
                int i;
                int pLen = Math.min(len, pvec);
                for (i = 0; i < pLen; i++) {
                    sb.append((i > 0) ? ',' : ' ');
                    Object val = v.getDataAtAsObject(i);
                    switch (type) {
                        case LGLSXP:
                            if (((Byte) val).byteValue() == -1) {
                                val = RRuntime.INT_NA;
                            }
                            sb.append(val); // NA as -2147483648
                            break;
                        case REALSXP:
                            // GNU R uses Rprintf("%g",val) but String.format("%g", val) differs
                            sb.append(val);
                            break;
                        case INTSXP:
                            sb.append(val); // NA as "nan" or -2147483648
                            break;
                        case RAWSXP:
                            int b = ((RRaw) val).getValue() & 0xFF;
                            sb.append("0123456789abcdef".charAt(b >>> 4));
                            sb.append("0123456789abcdef".charAt(b & 0x0F));
                            break;
                        case CPLXSXP:
                            sb.append(val); // Btw no output for complex values in GNU R
                            break;
                    }
                }
                if (i < len) {
                    sb.append(",...");
                }
            }
            sb.append('\n');
        } else {
            sb.append('\n');
        }
        if (obj instanceof RAttributable) {
            // GetAttributesNode with nested inspectTree would stack overflow for .Names
            DynamicObject attrs = ((RAttributable) obj).getAttributes();
            if (attrs != null) {
                sb.append("ATTRIB:\n");
                for (Object key : attrs.getShape().getKeys()) {
                    inspectTree(sb, indent + 2, "KEY: ", key, deep - 1, pvec);
                    inspectTree(sb, indent + 2, "VALUE: ", attrs.get(key), deep - 1, pvec);
                }
            }
        }
    }

    @TruffleBoundary
    private void echoOutput(String s) {
        try {
            StdConnections.getStdout().writeString(s, false);
        } catch (IOException ex) {
            throw error(RError.Message.GENERIC, ex.getMessage());
        }
    }

    private static void indent(StringBuilder sb, int indent) {
        int i = indent;
        while (i >= 8) {
            sb.append('\t');
            i -= 8;
        }
        while (--i >= 0) {
            sb.append(' ');
        }
    }

}
