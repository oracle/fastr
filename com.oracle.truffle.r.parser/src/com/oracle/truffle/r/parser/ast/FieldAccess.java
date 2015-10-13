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
import com.oracle.truffle.r.runtime.RRuntime;

public final class FieldAccess extends ASTNode {

    private final ASTNode lhs;
    private final String fieldName;
    private final boolean at;

    private FieldAccess(SourceSection source, ASTNode value, String fieldName, boolean at) {
        super(source);
        this.lhs = value;
        this.fieldName = fieldName;
        this.at = at;
    }

    @Override
    public <R> R accept(Visitor<R> v) {
        return v.visit(this);
    }

    @Override
    public <R> List<R> visitAll(Visitor<R> v) {
        return Arrays.asList(lhs.accept(v));
    }

    public boolean isAt() {
        return at;
    }

    public static ASTNode create(SourceSection src, FieldOperator op, ASTNode value, String fieldName) {
        switch (op) {
            case AT:
                // these two names have special meaning for slot retrieval

                String newName = fieldName.equals(RRuntime.DOT_DATA) ? RRuntime.DOT_DATA : (fieldName.equals(RRuntime.DOT_S3_CLASS) ? RRuntime.DOT_S3_CLASS : fieldName);
                return new FieldAccess(src, value, newName, true);
            case FIELD:
                return new FieldAccess(src, value, fieldName, false);
        }
        throw new Error("No node implemented for: '" + op + "' (" + value + ": " + fieldName + ")");
    }

    public static ASTNode create(SourceSection src, FieldOperator op, ASTNode value, ASTNode fieldName) {
        Constant fnc = (Constant) fieldName;
        assert fnc.getType() == Constant.ConstantType.STRING;
        return create(src, op, value, fnc.getValues()[0]);
    }

    public ASTNode getLhs() {
        return lhs;
    }

    public String getFieldName() {
        return fieldName;
    }
}
