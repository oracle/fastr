/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes;

import java.util.ArrayList;

import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.r.runtime.Collections.ArrayListInt;
import com.oracle.truffle.r.runtime.context.Engine.ParserMetadata;
import com.oracle.truffle.r.runtime.nodes.RCodeBuilder.RCodeToken;

/**
 * Gets called back from {@link RASTBuilder} to create GNU-R compatible parser metadata. Those
 * metadata can be accessed using, e.g., {@code getParseData(parse(text='x+y', keep.source=T))}.
 *
 * The missing functionality:
 * <ul>
 * <li>comments are not processed at all yet</li>
 * <li>RASTBuilder should have: binaryCall, unaryCall and specialCall forwarding this class, so that
 * we can differentiate between {@code 3+4} and {@code `+`(3,4)}. This could also have to save some
 * List allocations in RASTBuilder itself.</li>
 * <li>string constants should contain the quotes and also any escape sequences. The lexer should
 * just return them as-is including the quotes and in the semantic action for string constant we
 * should parse them separately.</li>
 * </ul>
 */
final class ParseDataBuilder {
    /**
     * Metadata built by this class. 6 numbers for each token/terminal:
     * <ul>
     * <li>startLine</li>
     * <li>startColumn</li>
     * <li>endLine</li>
     * <li>endColumn</li>
     * <li>isTerminal (0 or 1)</li>
     * <li>token code</li>
     * <li>token/terminal ID</li>
     * <li>parent terminal ID</li>
     * </ul>
     */
    public ArrayListInt data = new ArrayListInt(256);

    private static final int DATA_ELEM_SIZE = 8;
    private static final int DATA_TOKEN_CODE_OFFSET = 5;

    /**
     * Metadata built by this class: for each token it contains the name of the token type. For
     * expressions it contains just "expr".
     */
    private final ArrayList<String> tokens = new ArrayList<>(32);

    /**
     * Metadata built by this class: for each token contains the actual text.
     */
    public final ArrayList<String> text = new ArrayList<>(32);

    // ----------------------
    // Fields and methods for bookkeeping

    // Running counter to assign unique IDs to tokens/terminals
    private int idCounter;

    // Indexes to 'data'. Marks elements that are pending to be adopted by their parent.
    // The indexes point to the respective "parent ids" slots in "data" that need to be patched.
    private ArrayListInt orphansParentIdIdx = new ArrayListInt(32);
    private ArrayList<SourceSection> orphansSections = new ArrayList<>(32);

    private int removeOrphan(int index) {
        int result = orphansParentIdIdx.get(index);
        orphansParentIdIdx.set(index, orphansParentIdIdx.get(orphansParentIdIdx.size() - 1));
        orphansParentIdIdx.pop();
        orphansSections.set(index, orphansSections.get(orphansSections.size() - 1));
        orphansSections.remove(orphansSections.size() - 1);
        return result;
    }

    private boolean isParent(SourceSection parentSection, int orphanIndex) {
        SourceSection childSec = orphansSections.get(orphanIndex);
        // we are paranoid with the null checks to be extra robust
        return childSec != null && parentSection != null && isSubsection(parentSection, childSec);
    }

    private static boolean isSubsection(SourceSection parentSection, SourceSection childSec) {
        return parentSection.getCharIndex() <= childSec.getCharIndex() &&
                        childSec.getCharEndIndex() <= parentSection.getCharEndIndex();
    }

    private void adopt(int parentId, int orphanId) {
        int idx = removeOrphan(orphanId);
        data.set(idx, parentId);
    }

    /**
     * Puts the given arguments into {@link #data}.
     */
    private int record(SourceSection ss, boolean isTerminal, int tokenId, String token, String txt) {
        data.add(ss.getStartLine());
        data.add(ss.getStartColumn());
        data.add(ss.getEndLine());
        data.add(ss.getEndColumn());
        data.add(isTerminal ? 1 : 0);
        data.add(tokenId);
        int newId = ++idCounter;
        data.add(newId);
        data.add(0); // dummy parent
        tokens.add(token);
        text.add(txt);
        return newId;
    }

    // ----------------------
    // Callbacks from RASTBuilder

    ParserMetadata getParseData() {
        return new ParserMetadata(data.toArray(), tokens.toArray(new String[0]), text.toArray(new String[0]));
    }

    void token(SourceSection source, RCodeToken token, String tokenTextIn) {
        String tokenText = tokenTextIn;
        if (token == RCodeToken.STR_CONST) {
            // TODO: change parser so that it sends us the quotes, escape sequences, etc. see the
            // JavaDoc of this class
            tokenText = '"' + tokenText + '"';
        }
        record(source, true, token.getCode(), token.getTokenName(), tokenText);
        orphansParentIdIdx.add(data.size() - 1);
        orphansSections.add(source);
    }

    void modifyLastTokenIf(RCodeToken oldToken, RCodeToken newToken) {
        int oldCode = data.get(data.size() - DATA_ELEM_SIZE + DATA_TOKEN_CODE_OFFSET);
        if (oldToken.getCode() == oldCode) {
            modifyLastToken(newToken);
        }
    }

    void modifyLastToken(RCodeToken newToken) {
        data.set(data.size() - DATA_ELEM_SIZE + DATA_TOKEN_CODE_OFFSET, newToken.getCode());
        tokens.set(tokens.size() - 1, newToken.getTokenName());
    }

    /**
     * Record the parse data of the expression and adopt orphan tokens/terminals that are inside the
     * given expression.
     */
    void expr(SourceSection ss, String tokenName) {
        int parentId = record(ss, false, 77, tokenName, "");
        int i = 0;
        while (i < orphansParentIdIdx.size()) {
            if (isParent(ss, i)) {
                adopt(parentId, i);
                // no increment: revisit the current index as we moved the last orphan to this index
            } else {
                i++;
            }
        }
        orphansParentIdIdx.add(data.size() - 1);
        orphansSections.add(ss);
    }

    void expr(SourceSection ss) {
        expr(ss, "expr");
    }

    void lookupCall(SourceSection source, String symbol) {
        if (symbol.equals("=")) {
            expr(source, "equal_assign");
        } else {
            expr(source);
        }
    }
}
