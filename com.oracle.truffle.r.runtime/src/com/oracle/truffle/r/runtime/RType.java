/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 1995-2012, The R Core Team
 * Copyright (c) 2003, The R Foundation
 * Copyright (c) 2013, 2014, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.runtime;

import com.oracle.truffle.api.CompilerDirectives.*;

public enum RType {
    Any("any"),
    Null("NULL"),
    Numeric("numeric"),
    Double("double"),
    Integer("integer"),
    Complex("complex"),
    Character("character"),
    Logical("logical"),
    Raw("raw"),
    List("list"),
    Formula("formula"),
    Function("function"),
    Matrix("matrix"),
    Array("array"),
    Closure("closure"),
    Builtin("builtin"),
    Special("special"),
    DataFrame("data.frame"),
    Factor("factor"),
    Symbol("symbol"),
    Environment("environment"),
    PairList("pairlist"),
    Language("language"),
    Promise("promise"),
    Expression("expression"),
    DefunctReal("real"),
    DefunctSingle("single"),
    ExternalPtr("externalptr");

    private final String name;

    private RType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
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
