/*
 * Copyright (c) 2014, 2021, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.runtime;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

/**
 * Read/write a <a href="http://www.debian.org/doc/debian-policy/ch-controlfields.html">Debian
 * Control File</a>.
 *
 * Note that for the
 * <a href="https://cran.r-project.org/doc/manuals/r-release/R-exts.html#The-DESCRIPTION-file"> R
 * DESCRIPTION file </a> it is not specified whether or not the whitespaces in fields values (for
 * multiline and continuation fields) should be retained. GNU-R 4.0.3 does retain the whitespaces in
 * the fields by default, therefore we also retain them.
 *
 */
public class DCF {

    public static class Fields {
        // A map because field may repeat and last one wins
        private final LinkedHashMap<String, String> fieldMap = new LinkedHashMap<>();

        private void add(String name, String content) {
            fieldMap.put(name, content);
        }

        /**
         * Note that values of fields may contain white spaces at the beginning or at the end.
         */
        public LinkedHashMap<String, String> getFields() {
            return fieldMap;
        }
    }

    private final ArrayList<Fields> paragraphs = new ArrayList<>();

    public static DCF read(String[] lines, Set<String> keepWhiteSet) {
        DCF result = new DCF();
        String fieldName = null;
        StringBuilder fieldContent = new StringBuilder();
        Fields fields = new Fields();
        for (String line : lines) {
            if (line == null) {
                break;
            }
            if (line.startsWith("#")) {
                continue;
            }
            if (isContinuationLine(line)) {
                assert fieldName != null;
                if (shouldStripWhiteSpaceOfField(fieldName, keepWhiteSet)) {
                    fieldContent.append('\n').append(line.trim());
                } else {
                    fieldContent.append('\n').append(line);
                }
            } else {
                // should start a field, finish off any current one
                if (fieldName != null) {
                    fields.add(fieldName, fieldContent.toString());
                    fieldContent = new StringBuilder();
                }
                if (endOfParagraph(line)) {
                    fieldName = null;
                    if (!fields.fieldMap.isEmpty()) {
                        result.paragraphs.add(fields);
                        fields = new Fields();
                    }
                    continue;
                }
                int ix = line.indexOf(':');
                if (ix <= 0) {
                    throw RError.error(RError.SHOW_CALLER, RError.Message.LINE_MALFORMED, line);
                }
                fieldName = line.substring(0, ix);
                // There may be no content on this line, all on a continuation line
                if (ix < line.length() - 1) {
                    if (line.charAt(ix + 1) == ' ') {
                        ix++;
                    }
                    fieldContent.append(line.substring(ix + 1));
                }
            }
        }
        if (fieldName != null) {
            // Finish the last field
            fields.add(fieldName, fieldContent.toString());
            result.paragraphs.add(fields);
        }
        return result;
    }

    public List<Fields> getRecords() {
        return paragraphs;
    }

    private static boolean isContinuationLine(String line) {
        int wsLen = getWhiteSpaceHeadLength(line);
        return wsLen > 0;
    }

    private static boolean shouldStripWhiteSpaceOfField(String fieldName, Set<String> keepWhiteSet) {
        assert fieldName != null;
        return keepWhiteSet == null || !keepWhiteSet.contains(fieldName);
    }

    private static int getWhiteSpaceHeadLength(String line) {
        int i;
        for (i = 0; i < line.length() && (line.charAt(i) == ' ' || line.charAt(i) == '\t'); i++) {
        }
        return i;
    }

    private static boolean endOfParagraph(String line) {
        // TODO just spaces or tabs also legal
        return line.length() == 0;
    }
}
