/*
 * Copyright (c) 2013, 2016, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.r.nodes.RASTUtils;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.gnur.*;
import com.oracle.truffle.r.runtime.nodes.*;

abstract class WriteVariableNodeSyntaxHelper extends WriteVariableNode {
    @CompilationFinal private SourceSection sourceSectionR;

    protected WriteVariableNodeSyntaxHelper(SourceSection sourceSection) {
        assert sourceSection != null;
        this.sourceSectionR = sourceSection;
    }

    protected void deparseHelper(RDeparse.State state, String op) {
        state.append(getName().toString());
        RNode rhs = getRhs();
        if (rhs != null) {
            state.append(op);
            getRhs().deparse(state);
        }
    }

    protected void serializeHelper(RSerialize.State state, String op) {
        RNode rhs = getRhs();
        if (rhs == null) {
            state.setCarAsSymbol(getName().toString());
        } else {
            state.setAsBuiltin(op);
            state.openPairList(SEXPTYPE.LISTSXP);
            state.setCarAsSymbol(getName().toString());
            state.openPairList(SEXPTYPE.LISTSXP);
            state.serializeNodeSetCar(getRhs());
            state.linkPairList(2);
            state.setCdr(state.closePairList());
        }
    }

    protected Object getRelementHelper(String op, int index) {
        switch (index) {
            case 0:
                assert op == op.intern();
                return RDataFactory.createSymbol(op);
            case 1:
                CompilerAsserts.neverPartOfCompilation();
                return RDataFactory.createSymbolInterned(getName().toString());
            case 2:
                return RASTUtils.createLanguageElement(getRhs());
            default:
                throw RInternalError.shouldNotReachHere();
        }

    }

    protected void allNamesHelper(RAllNames.State state, String op) {
        RNode rhs = getRhs();
        if (rhs != null) {
            state.addName(op);
        }
        state.addName(getName().toString());
        if (rhs != null) {
            getRhs().allNames(state);
        }
    }

    public void setSourceSection(SourceSection sourceSection) {
        this.sourceSectionR = sourceSection;
    }

    @Override
    public SourceSection getSourceSection() {
        return sourceSectionR;
    }

    public void unsetSourceSection() {
        sourceSectionR = null;
    }
}
