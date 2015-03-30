/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 1995-2015, The R Core Team
 * Copyright (c) 2003, The R Foundation
 * Copyright (c) 2015, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.library.tools;

import com.oracle.truffle.r.runtime.data.*;

public class Text {
    public static RStringVector doTabExpand(RStringVector strings, RIntVector starts) {
        String[] data = new String[strings.getLength()];
        for (int i = 0; i < data.length; i++) {
            String input = strings.getDataAt(i);
            if (input.indexOf('\t') >= 0) {
                StringBuffer sb = new StringBuffer();
                int b = 0;
                int start = starts.getDataAt(i % data.length);
                for (int sx = 0; sx < input.length(); sx++) {
                    char ch = input.charAt(sx);
                    if (ch == '\n') {
                        start = -b - 1;
                    }
                    if (ch == '\t') {
                        do {
                            sb.append(' ');
                            b++;
                        } while (((b + start) & 7) != 0);
                    } else {
                        sb.append(ch);
                        b++;
                    }
                }
                data[i] = sb.toString();
            } else {
                data[i] = input;
            }
        }
        return RDataFactory.createStringVector(data, RDataFactory.COMPLETE_VECTOR);
    }

}
