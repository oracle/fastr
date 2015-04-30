/*
 * Copyright (c) 2013, 2015, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.access;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.source.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.env.*;

public abstract class ConstantNode extends RNode implements VisibilityController {

    public static boolean isFunction(RNode node) {
        return node instanceof ConstantObjectNode && ((ConstantObjectNode) node).value instanceof RFunction;
    }

    public static boolean isMissing(RNode node) {
        return node instanceof ConstantObjectNode && ((ConstantObjectNode) node).value == RMissing.instance;
    }

    public final Object getValue() {
        return execute(null);
    }

    @Override
    @TruffleBoundary
    public void deparse(RDeparse.State state) {
        RDeparse.deparse2buff(state, getValue());
    }

    @Override
    public RNode substitute(REnvironment env) {
        return this;
    }

    @Override
    public void serialize(RSerialize.State state) {
        state.setCar(getValue());
    }

    public static ConstantNode create(Object value) {
        CompilerAsserts.neverPartOfCompilation();
        if (value instanceof Integer) {
            return new ConstantIntegerScalarNode((Integer) value);
        } else if (value instanceof Double) {
            return new ConstantDoubleScalarNode((Double) value);
        } else if (value instanceof Boolean) {
            return new ConstantLogicalScalarNode((Boolean) value);
        } else if (value instanceof Byte) {
            return new ConstantLogicalScalarNode((Byte) value);
        } else if (value instanceof String) {
            return new ConstantObjectNode(value);
        } else if (value instanceof RSymbol) {
            return new ConstantObjectNode(((RSymbol) value).getName());
        } else if (value instanceof RArgsValuesAndNames) {
            // this can be created during argument matching and "call"
            return new ConstantObjectNode(value);
        } else {
            assert value instanceof RTypedValue && !(value instanceof RPromise) : value;
            return new ConstantObjectNode(value);
        }
    }

    public static ConstantNode create(SourceSection src, Object value) {
        ConstantNode cn = create(value);
        cn.assignSourceSection(src);
        return cn;
    }

    @Override
    public boolean isSyntax() {
        return true;
    }

    public static final class ConstantDoubleScalarNode extends ConstantNode {

        private final Double objectValue;
        private final double doubleValue;

        public ConstantDoubleScalarNode(double value) {
            this.objectValue = value;
            this.doubleValue = value;
        }

        @Override
        public Object execute(VirtualFrame frame) {
            controlVisibility();
            return objectValue;
        }

        @Override
        public double executeDouble(VirtualFrame frame) {
            controlVisibility();
            return doubleValue;
        }
    }

    public static final class ConstantLogicalScalarNode extends ConstantNode {

        private final double doubleValue;
        private final int intValue;
        private final byte logicalValue;

        public ConstantLogicalScalarNode(boolean value) {
            this.logicalValue = RRuntime.asLogical(value);
            this.doubleValue = value ? 1.0d : 0.0d;
            this.intValue = value ? 1 : 0;
        }

        public ConstantLogicalScalarNode(byte value) {
            this.logicalValue = value;
            this.doubleValue = value == RRuntime.LOGICAL_TRUE ? 1.0d : 0.0d;
            this.intValue = value == RRuntime.LOGICAL_TRUE ? 1 : 0;
        }

        @Override
        public Object execute(VirtualFrame frame) {
            controlVisibility();
            return logicalValue;
        }

        @Override
        public int executeInteger(VirtualFrame frame) {
            controlVisibility();
            return intValue;
        }

        @Override
        public byte executeByte(VirtualFrame frame) {
            controlVisibility();
            return logicalValue;
        }

        @Override
        public double executeDouble(VirtualFrame frame) {
            controlVisibility();
            return doubleValue;
        }
    }

    public static final class ConstantIntegerScalarNode extends ConstantNode {

        private final Integer objectValue;
        private final int intValue;
        private final double doubleValue;

        public ConstantIntegerScalarNode(int value) {
            this.objectValue = value;
            this.intValue = value;
            this.doubleValue = value;
        }

        @Override
        public Object execute(VirtualFrame frame) {
            controlVisibility();
            return objectValue;
        }

        @Override
        public int executeInteger(VirtualFrame frame) {
            controlVisibility();
            return intValue;
        }

        @Override
        public double executeDouble(VirtualFrame frame) {
            controlVisibility();
            return doubleValue;
        }
    }

    private static final class ConstantObjectNode extends ConstantNode {

        private final Object value;

        public ConstantObjectNode(Object value) {
            controlVisibility();
            this.value = value;
        }

        @Override
        public Object execute(VirtualFrame frame) {
            controlVisibility();
            return value;
        }

        @Override
        @TruffleBoundary
        public void deparse(RDeparse.State state) {
            if (value == RMissing.instance) {
                // nothing to do
            } else if (value instanceof RArgsValuesAndNames) {
                RArgsValuesAndNames args = (RArgsValuesAndNames) value;
                Object[] values = args.getArguments();
                for (int i = 0; i < values.length; i++) {
                    String name = args.getSignature().getName(i);
                    if (name != null) {
                        state.append(name);
                        state.append(" = ");
                    }
                    Object argValue = values[i];
                    if (argValue instanceof RNode) {
                        ((RNode) argValue).deparse(state);
                    } else if (argValue instanceof RPromise) {
                        ((RNode) RASTUtils.unwrap(((RPromise) argValue).getRep())).deparse(state);
                    } else {
                        RInternalError.shouldNotReachHere();
                    }
                    if (i < values.length - 1) {
                        state.append(", ");
                    }
                }
            } else {
                super.deparse(state);
            }
        }

        @Override
        public void serialize(RSerialize.State state) {
            if (value == RMissing.instance) {
                state.setCar(RMissing.instance);
            } else {
                super.serialize(state);
            }
        }
    }
}
