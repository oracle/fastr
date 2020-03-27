/*
 * Copyright (c) 1995-2012, The R Core Team
 * Copyright (c) 2003, The R Foundation
 * Copyright (c) 2013, 2020, Oracle and/or its affiliates
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, a copy is available at
 * https://www.R-project.org/Licenses/
 */
package com.oracle.truffle.r.runtime;

import java.util.Arrays;

import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.dsl.CachedContext;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.api.library.ExportMessage.Ignore;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.context.TruffleRLanguage;
import com.oracle.truffle.r.runtime.data.RBaseObject;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.env.frame.FrameSlotChangeMonitor.MultiSlotData;

@ExportLibrary(InteropLibrary.class)
public enum RType implements TruffleObject {
    Any("any", -1),
    Null("NULL", -1),
    Unbound("UNBOUND", -1),
    Raw("raw", 0),
    Logical("logical", 1),
    Integer("integer", 2),
    Double("double", "numeric", 3),
    Numeric("numeric", 3),
    Complex("complex", 4),
    Character("character", 5),
    List("list", 6),
    Expression("expression", 7),
    Missing("missing", -1),
    Function("function", -1),
    Matrix("matrix", -1),
    Array("array", -1),
    Closure("closure", "function", -1),
    Builtin("builtin", "function", -1),
    Special("special", "function", -1),
    Symbol("symbol", "name", -1),
    Environment("environment", -1),
    PairList("pairlist", -1),
    Language("language", "call", -1),
    Promise("promise", -1),
    DefunctReal("real", -1),
    DefunctSingle("single", -1),
    ExternalPtr("externalptr", -1),
    WeakRef("weakref", -1),
    S4Object("S4", -1),
    Connection("connection", -1),
    Dots("...", -1),
    TruffleObject("polyglot.value", -1),
    RInteropByte("interopt.byte", -1),
    RInteropChar("interopt.char", -1),
    RInteropFloat("interopt.float", -1),
    RInteropLong("interopt.long", -1),
    RInteropShort("interopt.short", -1),
    Char("char", -1);

    public static final int NO_PRECEDENCE = -1;
    public static final int NUMBER_OF_PRECEDENCES = 9;
    public static final RType[] VALUES = values();

    private final String name;
    private final String clazz;
    private final int precedence;

    RType(String name, int precedence) {
        this(name, name, precedence);
    }

    RType(String name, String clazz, int precedence) {
        this.name = name;
        this.clazz = clazz;
        this.precedence = precedence;
    }

    @ExportMessage
    @SuppressWarnings("static-method")
    public boolean isMetaObject() {
        return true;
    }

    @ExportMessage(name = "getMetaQualifiedName")
    @ExportMessage(name = "getMetaSimpleName")
    public String getName() {
        return name;
    }

    @SuppressWarnings("static-method")
    @ExportMessage
    public final boolean hasSourceLocation() {
        return false;
    }

    @SuppressWarnings("static-method")
    @ExportMessage
    public final SourceSection getSourceLocation() {
        return null;
    }

    @ExportMessage
    @SuppressWarnings("static-method")
    public final boolean hasLanguage() {
        return true;
    }

    @ExportMessage
    @SuppressWarnings("static-method")
    public final Class<? extends TruffleLanguage<?>> getLanguage() {
        return TruffleRLanguage.class;
    }

    @ExportMessage
    public String toDisplayString(@SuppressWarnings("unused") boolean allowSideEffects) {
        return getName();
    }

    @ExportMessage
    static class IsMetaInstance {
        @Specialization
        static boolean regularObjects(RType receiver, RBaseObject obj) {
            return obj.getMetaType() == receiver;
        }

        @Specialization
        static boolean multiSlots(RType receiver, MultiSlotData multiSlotData,
                        @CachedLibrary("receiver") InteropLibrary interopLib,
                        @CachedContext(TruffleRLanguage.class) RContext ctx) {
            try {
                return interopLib.isMetaInstance(receiver, multiSlotData.get(ctx.getMultiSlotInd()));
            } catch (UnsupportedMessageException ex) {
                throw RInternalError.shouldNotReachHere();
            }
        }

        @Fallback
        static boolean otherObjects(@SuppressWarnings("unused") RType receiver, @SuppressWarnings("unused") Object obj) {
            return false;
        }
    }

    public String getClazz() {
        return clazz;
    }

    public int getPrecedence() {
        return precedence;
    }

    public boolean isNumeric() {
        switch (this) {
            case Logical:
            case Double:
            case Integer:
            case Complex:
                return true;
            default:
                return false;
        }
    }

    public boolean isAtomic() {
        switch (this) {
            case Logical:
            case Double:
            case Integer:
            case Complex:
            case Character:
            case Raw:
                return true;
            default:
                return false;
        }
    }

    public boolean isVector() {
        switch (this) {
            case Logical:
            case Double:
            case Integer:
            case Complex:
            case Character:
            case Raw:
            case List:
            case Expression:
                return true;
            default:
                return false;
        }
    }

    public static RType getRType(Object value) {
        if (value instanceof RBaseObject) {
            return ((RBaseObject) value).getRType();
        } else if (value instanceof Integer) {
            return Integer;
        } else if (value instanceof Double) {
            return Double;
        } else if (value instanceof Byte) {
            return Logical;
        } else if (value instanceof String) {
            return Character;
        } else {
            return null;
        }
    }

    public static RType fromMode(String mode) {
        return fromMode(mode, false);
    }

    /**
     * @param includeNumeric if {@code true}, then the method returns {@link #Numeric} for string
     *            "numeric". This may be useful for callers that need to distinguish "numeric" and
     *            "double" modes.
     */
    public static RType fromMode(String mode, boolean includeNumeric) {
        switch (mode) {
            case "any":
                return Any;
            case "NULL":
                return Null;
            case "UNBOUND":
                return Unbound;
            case "raw":
                return Raw;
            case "logical":
                return Logical;
            case "integer":
                return Integer;
            case "numeric":
                return includeNumeric ? Numeric : Double;
            case "double":
                return Double;
            case "complex":
                return Complex;
            case "character":
                return Character;
            case "list":
                return List;
            case "expression":
                return Expression;
            case "missing":
                return Missing;
            case "function":
                return Function;
            case "matrix":
                return Matrix;
            case "array":
                return Array;
            case "closure":
                return Closure;
            case "builtin":
                return Builtin;
            case "special":
                return Special;
            case "name":
            case "symbol":
                return Symbol;
            case "environment":
                return Environment;
            case "pairlist":
                return PairList;
            case "language":
                return Language;
            case "promise":
                return Promise;
            case "real":
                return DefunctReal;
            case "single":
                return DefunctSingle;
            case "externalptr":
                return ExternalPtr;
            case "weakref":
                return WeakRef;
            case "S4":
                return S4Object;
            case "connection":
                return Connection;
            default:
                return null;
        }
    }

    public static RType maxPrecedence(RType t1, RType t2) {
        if (t1 == t2) {
            return t1;
        }
        if (t1.precedence == NO_PRECEDENCE || t2.precedence == NO_PRECEDENCE) {
            throw new IllegalArgumentException("invalid precedence");
        }
        if (t1.precedence >= t2.precedence) {
            return t1;
        } else {
            return t2;
        }
    }

    public RAbstractVector getEmpty() {
        switch (this) {
            case Double:
                return RDataFactory.createEmptyDoubleVector();
            case Integer:
                return RDataFactory.createEmptyIntVector();
            case Complex:
                return RDataFactory.createEmptyComplexVector();
            case Logical:
                return RDataFactory.createEmptyLogicalVector();
            case Character:
                return RDataFactory.createEmptyStringVector();
            case Raw:
                return RDataFactory.createEmptyRawVector();
            case List:
                return RDataFactory.createList();
            default:
                throw RInternalError.shouldNotReachHere();
        }
    }

    public RAbstractVector create(int length, boolean fillNA) {
        switch (this) {
            case Logical:
                return RDataFactory.createLogicalVector(length, fillNA);
            case Integer:
                return RDataFactory.createIntVector(length, fillNA);
            case Double:
                return RDataFactory.createDoubleVector(length, fillNA);
            case Complex:
                return RDataFactory.createComplexVector(length, fillNA);
            case Character:
                return RDataFactory.createStringVector(length, fillNA);
            case Expression: {
                Object[] data = new Object[length];
                Arrays.fill(data, RNull.instance);
                return RDataFactory.createExpression(data);
            }
            case List: {
                Object[] data = new Object[length];
                Arrays.fill(data, RNull.instance);
                return RDataFactory.createList(data);
            }
            case Raw:
                return RDataFactory.createRawVector(length);
            default:
                throw RInternalError.shouldNotReachHere();
        }
    }

    private static final RType[] VECTOR_TYPES = new RType[]{Raw, Logical, Integer, Double, Complex, Character, List};

    public static RType[] getVectorTypes() {
        return VECTOR_TYPES;
    }

    @Ignore
    public boolean isNull() {
        return this == Null;
    }
}
