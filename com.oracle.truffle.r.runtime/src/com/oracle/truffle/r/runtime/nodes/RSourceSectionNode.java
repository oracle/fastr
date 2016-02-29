/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.source.SourceSection;

/**
 * A subclass of {@link RNode} that carries {@link SourceSection}. Wherever possible, a node that
 * implements {@link RSyntaxNode} should subclass this class. If that is not possible, due to
 * inheritance restrictions, just cut and paste the class body into the class, as it's not worth a
 * level of indirection for such a trivial piece of code.
 */
public abstract class RSourceSectionNode extends RNode implements RSyntaxNode {
    /**
     * temp disambiguate for debugging until sourceSection removed from Node.
     */
    @CompilationFinal private SourceSection sourceSectionR;

    protected RSourceSectionNode(SourceSection sourceSection) {
        assert sourceSection != null;
        this.sourceSectionR = sourceSection;
    }

    public void setSourceSection(SourceSection sourceSection) {
        assert sourceSection != null;
        this.sourceSectionR = sourceSection;
    }

    @Override
    public SourceSection getSourceSection() {
        return sourceSectionR;
    }

}
