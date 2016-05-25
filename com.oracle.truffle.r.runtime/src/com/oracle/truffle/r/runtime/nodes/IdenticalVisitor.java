/*
 * Copyright (c) 2016, 2016, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.runtime.nodes;

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
        return element.getValue().equals(((RSyntaxConstant) arg).getValue());
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
            if (!accept(arg1, arg2)) {
                return false;
            }
        }
        return true;
    }
}
