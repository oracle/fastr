/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.runtime.data.altrep;

import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RPairList;
import com.oracle.truffle.r.runtime.data.RPairListLibrary;

/**
 * This class represent an instance data of an ALTREP. In GNU-R these data are represented as pairlist,
 * where CAR is data1 and CDR is data2.
 */
public class RAltRepData {
    private RPairList data;

    public RAltRepData(Object data1, Object data2) {
        this.data = RDataFactory.createPairList(data1, data2);
    }

    public RPairList getDataPairList() {
        return data;
    }

    public Object getData1() {
        return data.car();
    }

    public Object getData1(RPairListLibrary pairListLibrary) {
        return pairListLibrary.car(data);
    }

    public Object getData2() {
        return data.cdr();
    }

    public void setData1(Object data1) {
        data.setCar(data1);
    }

    public void setData2(Object data2) {
        data.setCdr(data2);
    }

    @Override
    public String toString() {
        return "data1=" + (data.car() == null ? "null" : data.car()) +
                        ", data2=" + (data.cdr() == null ? "null" : data.cdr());
    }
}
