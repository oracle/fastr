/*
 * Copyright (c) 2014, 2014, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.runtime.data;

import java.util.*;

/**
 * Denotes the (rarely seen) {@code pairlist} type in R.
 */
public class RPairList {
    private Object car;
    private Object cdr;
    private String tag;
    private HashMap<String, Object> attributes;

    public RPairList(Object car, Object cdr, String tag) {
        this.car = car;
        this.cdr = cdr;
        this.tag = tag;
    }

    public Object getCar() {
        return car;
    }

    public Object getCdr() {
        return cdr;
    }

    public String getTag() {
        return tag;
    }

    public Object getAttr(String name) {
        if (attributes == null) {
            return null;
        }
        return attributes.get(name);
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public void setAttr(String name, Object value) {
        if (attributes == null) {
            attributes = new HashMap<>();
        }
        attributes.put(name, value);
    }

}
