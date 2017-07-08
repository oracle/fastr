/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 1995-2015, The R Core Team
 * Copyright (c) 2003, The R Foundation
 * Copyright (c) 2016, 2017, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.runtime.gnur;

public enum SA_TYPE {
    DEFAULT("default"),
    NOSAVE("no"),
    SAVE("yes"),
    SAVEASK("ask");

    private String name;

    SA_TYPE(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
