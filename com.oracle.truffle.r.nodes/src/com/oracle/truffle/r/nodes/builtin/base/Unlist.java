/*
 * Copyright (c) 2013, 2014, Oracle and/or its affiliates. All rights reserved.
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

import static com.oracle.truffle.r.runtime.RBuiltinKind.*;

import com.oracle.truffle.api.CompilerDirectives.SlowPath;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.access.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.nodes.unary.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;

@RBuiltin(name = "unlist", kind = SUBSTITUTE)
// TODO INTERNAL
public abstract class Unlist extends RBuiltinNode {

    private static final Object[] PARAMETER_NAMES = new Object[]{"x", "recursive", "use.names"};

    @Override
    public Object[] getParameterNames() {
        return PARAMETER_NAMES;
    }

    @Override
    public RNode[] getParameterValues() {
        return new RNode[]{ConstantNode.create(RMissing.instance), ConstantNode.create(RRuntime.LOGICAL_TRUE), ConstantNode.create(RRuntime.LOGICAL_TRUE)};
    }

    @Child private PrecedenceNode precedenceNode;

    protected Unlist() {
        this.precedenceNode = PrecedenceNodeFactory.create(null);
    }

    @SuppressWarnings("unused")
    @Specialization(guards = "!isNonEmptyList")
    public Object unlistNoop(Object object, byte recursive, byte useNames) {
        controlVisibility();
        return object;
    }

    @SuppressWarnings("unused")
    @Specialization(guards = "isNonEmptyList")
    public RAbstractVector unlist(VirtualFrame frame, Object object, byte recursive, byte useNames) {
        controlVisibility();
        RList list = (RList) object;

        int precedence = -1;
        int totalSize = 0;
        for (int i = 0; i < list.getLength(); ++i) {
            Object data = list.getDataAt(i);
            precedence = Math.max(precedence, precedenceNode.executeInteger(frame, data));
            if (data instanceof RAbstractVector) {
                RAbstractVector rAbstractVector = (RAbstractVector) data;
                totalSize += rAbstractVector.getLength();
            } else if (data != RNull.instance) {
                totalSize++;
            }
        }
        return unlistHelper(precedence, totalSize, list, useNames == RRuntime.LOGICAL_TRUE);
    }

    @SlowPath
    private static RAbstractVector unlistHelper(int precedence, int totalSize, RList list, boolean useNames) {
        if (precedence == PrecedenceNode.RAW_PRECEDENCE) {
            byte[] result = new byte[totalSize];
            int position = 0;
            RStringVector names = null;
            String[] namesData = null;
            for (int i = 0; i < list.getLength(); ++i) {
                Object cur = list.getDataAt(i);
                if (cur instanceof RAbstractVector) {
                    RAbstractVector rAbstractVector = (RAbstractVector) cur;
                    RStringVector orgNames = null;
                    if (rAbstractVector.getNames() != RNull.instance && useNames) {
                        if (namesData == null) {
                            namesData = new String[totalSize];
                        }
                        orgNames = (RStringVector) rAbstractVector.getNames();
                    }
                    for (int j = 0; j < rAbstractVector.getLength(); ++j) {
                        result[position] = unlistValueRaw(rAbstractVector.getDataAtAsObject(j));
                        if (namesData != null) {
                            namesData[position] = orgNames.getDataAt(j);
                        }
                        position++;
                    }
                    if (namesData != null) {
                        names = RDataFactory.createStringVector(namesData, orgNames.isComplete());
                    }
                } else if (cur != RNull.instance) {
                    result[position++] = unlistValueRaw(cur);
                }
            }
            return RDataFactory.createRawVector(result, names);
        } else if (precedence == PrecedenceNode.LOGICAL_PRECEDENCE) {
            byte[] result = new byte[totalSize];
            int position = 0;
            RStringVector names = null;
            String[] namesData = null;
            for (int i = 0; i < list.getLength(); ++i) {
                Object cur = list.getDataAt(i);
                if (cur instanceof RAbstractVector) {
                    RAbstractVector rAbstractVector = (RAbstractVector) cur;
                    RStringVector orgNames = null;
                    if (rAbstractVector.getNames() != RNull.instance && useNames) {
                        if (namesData == null) {
                            namesData = new String[totalSize];
                        }
                        orgNames = (RStringVector) rAbstractVector.getNames();
                    }
                    for (int j = 0; j < rAbstractVector.getLength(); ++j) {
                        result[position] = unlistValueLogical(rAbstractVector.getDataAtAsObject(j));
                        if (namesData != null) {
                            namesData[position] = orgNames.getDataAt(j);
                        }
                        position++;
                    }
                    if (namesData != null) {
                        names = RDataFactory.createStringVector(namesData, orgNames.isComplete());
                    }
                } else if (cur != RNull.instance) {
                    result[position++] = unlistValueLogical(cur);
                }
            }
            return RDataFactory.createLogicalVector(result, false, names);
        } else if (precedence == PrecedenceNode.INT_PRECEDENCE) {
            int[] result = new int[totalSize];
            int position = 0;
            RStringVector names = null;
            String[] namesData = null;
            for (int i = 0; i < list.getLength(); ++i) {
                Object cur = list.getDataAt(i);
                if (cur instanceof RAbstractVector) {
                    RAbstractVector rAbstractVector = (RAbstractVector) cur;
                    RStringVector orgNames = null;
                    if (rAbstractVector.getNames() != RNull.instance && useNames) {
                        if (namesData == null) {
                            namesData = new String[totalSize];
                        }
                        orgNames = (RStringVector) rAbstractVector.getNames();
                    }
                    for (int j = 0; j < rAbstractVector.getLength(); ++j) {
                        result[position] = unlistValueInt(rAbstractVector.getDataAtAsObject(j));
                        if (namesData != null) {
                            namesData[position] = orgNames.getDataAt(j);
                        }
                        position++;
                    }
                    if (namesData != null) {
                        names = RDataFactory.createStringVector(namesData, orgNames.isComplete());
                    }
                } else if (cur != RNull.instance) {
                    result[position++] = unlistValueInt(cur);
                }
            }
            return RDataFactory.createIntVector(result, false, names);

        } else if (precedence == PrecedenceNode.DOUBLE_PRECEDENCE) {
            double[] result = new double[totalSize];
            int position = 0;
            RStringVector names = null;
            String[] namesData = null;
            for (int i = 0; i < list.getLength(); ++i) {
                Object cur = list.getDataAt(i);
                if (cur instanceof RAbstractVector) {
                    RAbstractVector rAbstractVector = (RAbstractVector) cur;
                    RStringVector orgNames = null;
                    if (rAbstractVector.getNames() != RNull.instance && useNames) {
                        if (namesData == null) {
                            namesData = new String[totalSize];
                        }
                        orgNames = (RStringVector) rAbstractVector.getNames();
                    }
                    for (int j = 0; j < rAbstractVector.getLength(); ++j) {
                        result[position] = unlistValueDouble(rAbstractVector.getDataAtAsObject(j));
                        if (namesData != null) {
                            namesData[position] = orgNames.getDataAt(j);
                        }
                        position++;
                    }
                    if (namesData != null) {
                        names = RDataFactory.createStringVector(namesData, orgNames.isComplete());
                    }
                } else if (cur != RNull.instance) {
                    result[position++] = unlistValueDouble(cur);
                }
            }
            return RDataFactory.createDoubleVector(result, false, names);

        } else if (precedence == PrecedenceNode.COMPLEX_PRECEDENCE) {
            throw Utils.nyi();
        } else if (precedence == PrecedenceNode.STRING_PRECEDENCE) {
            String[] result = new String[totalSize];
            int position = 0;
            RStringVector names = null;
            String[] namesData = null;
            for (int i = 0; i < list.getLength(); ++i) {
                Object cur = list.getDataAt(i);
                if (cur instanceof RAbstractVector) {
                    RAbstractVector rAbstractVector = (RAbstractVector) cur;
                    RStringVector orgNames = null;
                    if (rAbstractVector.getNames() != RNull.instance && useNames) {
                        if (namesData == null) {
                            namesData = new String[totalSize];
                        }
                        orgNames = (RStringVector) rAbstractVector.getNames();
                    }
                    for (int j = 0; j < rAbstractVector.getLength(); ++j) {
                        result[position] = unlistValueString(rAbstractVector.getDataAtAsObject(j));
                        if (namesData != null) {
                            namesData[position] = orgNames.getDataAt(j);
                        }
                        position++;
                    }
                    if (namesData != null) {
                        names = RDataFactory.createStringVector(namesData, orgNames.isComplete());
                    }
                } else if (cur != RNull.instance) {
                    result[position++] = unlistValueString(cur);
                }
            }
            return RDataFactory.createStringVector(result, false, names);
        } else if (precedence == PrecedenceNode.LIST_PRECEDENCE) {
            Object[] result = new Object[totalSize];
            int position = 0;
            RStringVector names = null;
            String[] namesData = null;
            for (int i = 0; i < list.getLength(); ++i) {
                Object cur = list.getDataAt(i);
                if (cur instanceof RAbstractContainer) {
                    RAbstractContainer rAbstractContainer = (RAbstractContainer) cur;
                    RStringVector orgNames = null;
                    if (rAbstractContainer.getNames() != RNull.instance && useNames) {
                        if (namesData == null) {
                            namesData = new String[totalSize];
                        }
                        orgNames = (RStringVector) rAbstractContainer.getNames();
                    }
                    for (int j = 0; j < rAbstractContainer.getLength(); ++j) {
                        result[position] = rAbstractContainer.getDataAtAsObject(j);
                        if (namesData != null) {
                            namesData[position] = orgNames.getDataAt(j);
                        }
                        position++;
                    }
                    if (namesData != null) {
                        names = RDataFactory.createStringVector(namesData, orgNames.isComplete());
                    }
                } else if (cur != RNull.instance) {
                    result[position++] = unlistValueString(cur);
                }
            }
            return RDataFactory.createList(result, names);
        } else {
            throw Utils.nyi();
        }
    }

    private static String unlistValueString(Object cur) {
        return RRuntime.toString(cur);
    }

    private static double unlistValueDouble(Object dataAtAsObject) {
        if (dataAtAsObject instanceof Double) {
            return (double) dataAtAsObject;
        } else {
            int result = unlistValueInt(dataAtAsObject);
            if (RRuntime.isNA(result)) {
                return RRuntime.DOUBLE_NA;
            } else {
                return result;
            }
        }
    }

    private static int unlistValueInt(Object dataAtAsObject) {
        if (dataAtAsObject instanceof RRaw) {
            RRaw rRaw = (RRaw) dataAtAsObject;
            return RRuntime.raw2int(rRaw);
        } else if (dataAtAsObject instanceof Byte) {
            return RRuntime.logical2int((byte) dataAtAsObject);
        } else {
            return (int) dataAtAsObject;
        }
    }

    private static byte unlistValueLogical(Object dataAtAsObject) {
        if (dataAtAsObject instanceof RRaw) {
            RRaw rRaw = (RRaw) dataAtAsObject;
            return RRuntime.raw2logical(rRaw);
        } else {
            return (byte) dataAtAsObject;
        }
    }

    private static byte unlistValueRaw(Object dataAtAsObject) {
        return ((RRaw) dataAtAsObject).getValue();
    }

    public static boolean isNonEmptyList(Object object) {
        return object instanceof RList && ((RList) object).getLength() != 0;
    }
}
