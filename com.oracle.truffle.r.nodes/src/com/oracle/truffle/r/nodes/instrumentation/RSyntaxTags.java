/*
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.instrumentation;

import java.util.ArrayList;

import com.oracle.truffle.api.debug.Debugger;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.r.nodes.control.BlockNode;
import com.oracle.truffle.r.nodes.function.RCallNode;

public class RSyntaxTags {
    /**
     * Applied to all nodes in a {@link BlockNode}.
     */
    public static final String STATEMENT = "r-statement";
    /**
     * Applied to function bodies.
     */
    public static final String START_FUNCTION = "r-start_function";
    /**
     * All {@link RCallNode}s.
     */
    public static final String CALL = "r-call";
    /**
     * Applied to all loop nodes.
     */
    public static final String LOOP = "r-loop";

    /*
     * Hopefully Temporary if can agree on lang-call syntax.
     */
    static final String DEBUG_CALL = Debugger.CALL_TAG;
    static final String DEBUG_HALT = Debugger.HALT_TAG;

    public static final String[] ALL_TAGS = new String[]{CALL, STATEMENT, START_FUNCTION, LOOP, DEBUG_CALL, DEBUG_HALT};

    /**
     * Returns the existing set of tags or {@code null} if none.
     */
    static String[] getTags(SourceSection ss) {
        ArrayList<String> oldTags = new ArrayList<>();
        for (String tag : RSyntaxTags.ALL_TAGS) {
            if (ss.hasTag(tag)) {
                oldTags.add(tag);
            }
        }
        if (oldTags.size() > 0) {
            String[] result = new String[oldTags.size()];
            oldTags.toArray(result);
            return result;
        } else {
            return null;
        }
    }

    static boolean containsTag(String[] tags, String atag) {
        for (String tag : tags) {
            if (tag.equals(atag)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Return a new {@link SourceSection} that adds {@code newTags} checking for duplicates.
     */
    public static SourceSection addTags(SourceSection ss, String... newTags) {
        ArrayList<String> oldTags = new ArrayList<>();
        for (String tag : RSyntaxTags.ALL_TAGS) {
            if (ss.hasTag(tag)) {
                oldTags.add(tag);
            }
        }
        if (oldTags.size() > 0) {
            L: for (String newTag : newTags) {
                for (String oldTag : oldTags) {
                    if (newTag.equals(oldTag)) {
                        break L;
                    }
                }
                oldTags.add(newTag);
            }
            String[] mergedTags = new String[oldTags.size()];
            oldTags.toArray(mergedTags);
            return ss.withTags(mergedTags);
        } else {
            return ss.withTags(newTags);
        }
    }
}
