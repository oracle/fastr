/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 2012-2014, Purdue University
 * Copyright (c) 2013, 2016, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.parser.ast;

import java.util.Collections;
import java.util.List;

import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.RComplex;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RNull;

public final class Constant extends ASTNode {

    private final Object value;

    private Constant(Object value, SourceSection source) {
        super(source);
        this.value = value;
    }

    public Object getValue() {
        return value;
    }

    @Override
    public int getPrecedence() {
        return Operation.MAX_PRECEDENCE;
    }

    public String prettyValue() {
        return String.valueOf(value);
    }

    public static ASTNode createNull(SourceSection src) {
        return new Constant(RNull.instance, src);
    }

    @Override
    public <R> List<R> visitAll(Visitor<R> v) {
        return Collections.emptyList();
    }

    @Override
    public <R> R accept(Visitor<R> v) {
        return v.visit(this);
    }

    public static Constant createDouble(SourceSection src, String value) {
        return new Constant(RRuntime.string2double(value), src);
    }

    public static Constant createComplex(SourceSection src, String value) {
        if (value.equals("NA_complex_")) {
            return new Constant(RComplex.NA, src);
        } else {
            return new Constant(RDataFactory.createComplex(0, RRuntime.string2double(value)), src);
        }
    }

    public static Constant createInt(SourceSection src, String value) {
        return new Constant(RRuntime.string2int(value), src);
    }

    public static Constant createBool(SourceSection src, byte value) {
        return new Constant(value, src);
    }

    public static Constant createString(SourceSection src, String value) {
        return new Constant(value.intern(), src);
    }

    public static Constant createStringNA(SourceSection src) {
        // don't intern NA value, otherwise each "NA" literal becomes an NA value
        return new Constant(RRuntime.STRING_NA, src);
    }

    public Constant createNegated() {
        if (value instanceof Integer) {
            return new Constant(-(Integer) value, getSource());
        } else if (value instanceof Double) {
            return new Constant(-(Double) value, getSource());
        } else {
            throw RInternalError.shouldNotReachHere();
        }
    }
}
