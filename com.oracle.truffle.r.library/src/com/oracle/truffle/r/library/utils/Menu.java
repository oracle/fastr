/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 1995-2012, The R Core Team
 * Copyright (c) 2003, The R Foundation
 * Copyright (c) 2015, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.library.utils;

import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.RContext.*;

// Translated from GnuR: library/utils/io.c

public class Menu {
    public static int menu(String[] choices) {
        ConsoleHandler ch = RContext.getInstance().getConsoleHandler();
        int first = choices.length + 1;
        ch.print("Selection: ");
        String response = ch.readLine().trim();
        if (response.length() > 0) {
            if (Character.isDigit(response.charAt(0))) {
                try {
                    first = Integer.parseInt(response);
                } catch (NumberFormatException ex) {
                    //
                }
            } else {
                for (int i = 0; i < choices.length; i++) {
                    String entry = choices[i];
                    if (entry.equals(response)) {
                        first = i + 1;
                        break;
                    }
                }
            }
        }
        return first;
    }
}
