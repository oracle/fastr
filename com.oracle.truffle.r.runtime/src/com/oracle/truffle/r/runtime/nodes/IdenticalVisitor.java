/*
 * Copyright (c) 2016, 2019, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.runtime.nodes;

import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.r.runtime.data.RAttributable;
import com.oracle.truffle.r.runtime.data.RPairList;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.env.REnvironment;

/**
 * Currently, this visitor is only necessary because we don't treat the body of an RFunction as a
 * pairlist. It's slightly inaccurate in how it treats constants, attributes, etc.
 */
public final class IdenticalVisitor extends RSyntaxArgVisitor<Boolean, RSyntaxElement> {

    @Override
    protected Boolean visit(RSyntaxCall element, RSyntaxElement arg) {
        if (!(arg instanceof RSyntaxCall)) {
            return false;
        }
        RSyntaxCall other = (RSyntaxCall) arg;
        if (!element.getSyntaxSignature().equals(other.getSyntaxSignature()) || !accept(element.getSyntaxLHS(), other.getSyntaxLHS())) {
            return false;
        }
        return compareArguments(element.getSyntaxArguments(), other.getSyntaxArguments());
    }

    @Override
    protected Boolean visit(RSyntaxConstant element, RSyntaxElement arg) {
        if (!(arg instanceof RSyntaxConstant)) {
            return false;
        }
        return identicalValue(element.getValue(), ((RSyntaxConstant) arg).getValue());
    }

    private Boolean identicalValue(Object value, Object otherValue) {
        if (value instanceof Number || value instanceof String) {
            return value.equals(otherValue);
        }
        if (value instanceof RAttributable) {
            if (!(otherValue instanceof RAttributable)) {
                return false;
            }
            if (!identicalAttributes((RAttributable) value, (RAttributable) otherValue)) {
                return false;
            }
            if (!identicalAttributes((RAttributable) otherValue, (RAttributable) value)) {
                return false;
            }
        }
        if (value instanceof RAbstractVector) {
            RAbstractVector vector = (RAbstractVector) value;
            if (!(otherValue instanceof RAbstractVector)) {
                return false;
            }
            RAbstractVector otherVector = (RAbstractVector) otherValue;
            if (vector.getLength() != otherVector.getLength() || vector.getRType() != otherVector.getRType()) {
                return false;
            }
            for (int i = 0; i < vector.getLength(); i++) {
                if (!identicalValue(vector.getDataAtAsObject(i), otherVector.getDataAtAsObject(i))) {
                    return false;
                }
            }
            return true;
        }
        if (value instanceof RPairList && ((RPairList) value).isLanguage()) {
            if (!(otherValue instanceof RPairList && ((RPairList) otherValue).isLanguage())) {
                return false;
            }
            return accept(((RPairList) value).getSyntaxElement(), ((RPairList) otherValue).getSyntaxElement());
        }
        if (value instanceof REnvironment) {
            return value == otherValue;
        }
        return value == otherValue;
    }

    private boolean identicalAttributes(RAttributable attributable, RAttributable otherAttributable) {
        DynamicObject attributes = attributable.getAttributes();
        if (attributes != null) {
            DynamicObject otherAttributes = otherAttributable.getAttributes();
            for (Object key : attributes.getShape().getKeys()) {
                Object attributeValue = attributes.get(key);
                Object otherAttributeValue = otherAttributes == null ? null : otherAttributes.get(key);
                if ((attributeValue == null) != (otherAttributeValue == null) || !identicalValue(attributeValue, otherAttributeValue)) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    protected Boolean visit(RSyntaxLookup element, RSyntaxElement arg) {
        if (!(arg instanceof RSyntaxLookup)) {
            return false;
        }
        return element.getIdentifier().equals(((RSyntaxLookup) arg).getIdentifier());
    }

    @Override
    protected Boolean visit(RSyntaxFunction element, RSyntaxElement arg) {
        if (!(arg instanceof RSyntaxFunction)) {
            return false;
        }
        RSyntaxFunction other = (RSyntaxFunction) arg;
        if (element.getSyntaxSignature() != other.getSyntaxSignature() || !accept(element.getSyntaxBody(), other.getSyntaxBody())) {
            return false;
        }
        return compareArguments(element.getSyntaxArgumentDefaults(), other.getSyntaxArgumentDefaults());
    }

    private Boolean compareArguments(RSyntaxElement[] arguments1, RSyntaxElement[] arguments2) {
        assert arguments1.length == arguments2.length;
        for (int i = 0; i < arguments1.length; i++) {
            RSyntaxElement arg1 = arguments1[i];
            RSyntaxElement arg2 = arguments2[i];
            if (arg1 == null && arg2 == null) {
                continue;
            }
            if ((arg1 == null && arg2 != null) || (arg2 == null && arg1 != null)) {
                return false;
            }
            assert arg1 != null;
            assert arg2 != null;
            if (!accept(arg1, arg2)) {
                return false;
            }
        }
        return true;
    }
}
