/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 1995-2012, The R Core Team
 * Copyright (c) 2003, The R Foundation
 * Copyright (c) 2013, 2015, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.runtime;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;

public enum RType {

    Any("any", -1),
    Null("NULL", -1),
    Raw("raw", 0),
    Logical("logical", 1),
    Integer("integer", 2),
    Numeric("numeric", 3),
    Double("double", 3),
    Complex("complex", 4),
    Character("character", 5),
    List("list", 6),
    Expression("expression", 7),
    Missing("missing", -1),
    Formula("formula", -1),
    Function("function", -1),
    Matrix("matrix", -1),
    Array("array", -1),
    Closure("closure", -1),
    Builtin("builtin", -1),
    Special("special", -1),
    DataFrame("data.frame", -1),
    Factor("factor", -1),
    Symbol("symbol", -1),
    Environment("environment", -1),
    PairList("pairlist", -1),
    Language("language", -1),
    Promise("promise", -1),
    DefunctReal("real", -1),
    DefunctSingle("single", -1),
    ExternalPtr("externalptr", -1),
    S4Object("s4object", -1);

    public static final int NO_PRECEDENCE = -1;
    public static final int NUMBER_OF_PRECEDENCES = 9;

    private final String name;

    private RType(String name, @SuppressWarnings("unused") int precedence) {
        this.name = name;
        // TODO: do we need precedence for anything herw
    }

    public String getName() {
        return name;
    }

    public boolean isNumeric() {
        switch (this) {
            case Logical:
            case Numeric:
            case Double:
            case Integer:
            case Complex:
                return true;
            default:
                return false;
        }
    }

    public static RType fromString(String mode) {
        if (mode == Any.getName()) {
            return Any;
        } else if (mode == Function.getName()) {
            return Function;
        } else {
            return lookup(mode);
        }
    }

    @TruffleBoundary
    private static RType lookup(String mode) {
        for (RType type : values()) {
            if (type.getName().equals(mode)) {
                return type;
            }
        }
        return null;
    }

}
