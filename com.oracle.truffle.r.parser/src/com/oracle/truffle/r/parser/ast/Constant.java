/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 2012-2014, Purdue University
 * Copyright (c) 2013, 2015, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.parser.ast;

import java.util.*;

import com.oracle.truffle.api.source.*;
import com.oracle.truffle.r.runtime.*;

public final class Constant extends ASTNode {

    public enum ConstantType {
        DOUBLE,
        COMPLEX,
        INT,
        BOOL,
        STRING,
        NULL
    }

    private String value;
    private final ConstantType type;

    private Constant(String value, ConstantType type, SourceSection source) {
        super(source);
        this.value = value;
        this.type = type;
    }

    public String getValue() {
        return value;
    }

    public ConstantType getType() {
        return type;
    }

    @Override
    public int getPrecedence() {
        return Operation.MAX_PRECEDENCE;
    }

    public String prettyValue() {
        return value;
    }

    public static ASTNode getNull(SourceSection src) {
        return new Constant(null, ConstantType.NULL, src);
    }

    @Override
    public <R> List<R> visitAll(Visitor<R> v) {
        return Collections.emptyList();
    }

    @Override
    public <R> R accept(Visitor<R> v) {
        return v.visit(this);
    }

    public static Constant createDoubleConstant(SourceSection src, String value) {
        return new Constant(value, ConstantType.DOUBLE, src);
    }

    public static Constant createComplexConstant(SourceSection src, String value) {
        return new Constant(value, ConstantType.COMPLEX, src);
    }

    public static Constant createIntConstant(SourceSection src, String value) {
        return new Constant(value, ConstantType.INT, src);
    }

    public static Constant createBoolConstant(SourceSection src, int value) {
        String strValue;
        if (value == RRuntime.LOGICAL_NA) {
            strValue = "NA";
        } else {
            strValue = String.valueOf(value);
        }

        return new Constant(strValue, ConstantType.BOOL, src);
    }

    public static Constant createStringConstant(SourceSection src, String value) {
        return new Constant(value.intern(), ConstantType.STRING, src);
    }

    public static Constant createStringNA(SourceSection src) {
        // don't intern NA value, otherwise each "NA" literal becomes an NA value
        return new Constant(RRuntime.STRING_NA, ConstantType.STRING, src);
    }

    public void addNegativeSign() {
        value = "-" + value;
    }
}
