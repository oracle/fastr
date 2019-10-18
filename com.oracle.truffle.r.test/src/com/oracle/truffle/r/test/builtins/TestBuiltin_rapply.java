/*
 * Copyright (c) 2018, 2019, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.test.builtins;

import org.junit.Test;

import com.oracle.truffle.r.test.TestBase;

public class TestBuiltin_rapply extends TestBase {

    @Test
    public void testLapplyList() {
        assertEval("{rapply(list(1:3), function(x) { 2*x })}");
        assertEval("{rapply(list(list(1:3),4,5,6), function(x) { 2*x })}");
        assertEval("{rapply(list(1:3), function(x) { 2*x }, how = \"replace\")}");
        assertEval("{rapply(c(list(1:3),4,5,c(6,7)), function(x) { 2*x }, how = \"list\", deflt = 0)}");
        assertEval("{rapply(list(list(1:3),4,5,6), function(x) { 2*x }, how = \"list\")}");
        assertEval("{rapply(c(c(1:3),4,5,list(6,7)), function(x) { 2*x }, how = \"replace\", deflt =)}");
        assertEval("{rapply(c(c(1:3),4,5,list(6,7)), function(x) { 2*x }, how = \"replace\", deflt =, classes=)}");
        assertEval("{rapply(c(c(1:3),4,5,list(6,7)), function(x) { 2*x }, how=\"unlist\",deflt =, classes=)}");
        assertEval("{X <- list(list(a = pi, b = list(c = 1:1)), d = \"a test\"); rapply(X, function(x) x, how = \"replace\")}");
        assertEval("{X <- list(list(a = pi, b = list(c = 1:1)), d = \"a test\"); rapply(X, sqrt, classes = \"numeric\", how = \"replace\")}");
        assertEval("{X <- list(list(a = pi, b = list(c = 1:1)), d = \"a test\"); rapply(X, nchar, classes = \"character\", deflt = as.integer(NA), how = \"list\")}");
        assertEval("{X <- list(list(a = pi, b = list(c = 1:1)), d = \"a test\"); rapply(X, nchar, classes = \"character\", deflt = as.integer(NA), how = \"unlist\")}");
        assertEval("{X <- list(list(a = pi, b = list(c = 1:1)), d = \"a test\"); rapply(X, nchar, classes = \"character\", how = \"unlist\")}");
        assertEval("{X <- list(list(a = pi, b = list(c = 1:1)), d = \"a test\"); rapply(X, log, classes = \"numeric\", how = \"replace\", base = 2)}");
        assertEval("x=list(1,2,3,4,\"a\"); rapply(x,function(x){x^2},classes=\"numeric\")");
        assertEval("x=list(1,list(2,3),4,list(5,list(6,7))); rapply(x,function(x){x^2},classes=\"numeric\")");
        assertEval("x=list(1,list(2,3),4,list(5,list(6,7))); rapply(x,function(x){x^2},classes=\"numeric\",how=\"unlist\",deflt=\"p\")");
        assertEval("x=list(1,list(2,3),4,list(5,list(6,7))); rapply(x,function(x,y){x^y},classes=\"numeric\",how=\"unlist\",deflt=\"p\",y=3)");
        assertEval("l2 = list(a = 1:10, b = 11:20,c=c('d','a','t','a')); rapply(l2, mean, how = \"list\", classes = \"integer\")");
        assertEval("l2 = list(a = 1:10, b = 11:20,c=c('d','a','t','a')); rapply(l2, mean, how = \"unlist\", classes = \"integer\")");
        assertEval("l2 = list(a = 1:10, b = 11:20,c=c('d','a','t','a')); rapply(l2, mean, how = \"replace\", classes = \"integer\")");
        assertEval("rapply(NULL, function(x) {2*x})");
        assertEval("rapply(NA, function(x) {2*x})");
        assertEval("rapply(list(NULL), function(x) {2*x})");
        assertEval("rapply(list(NA), function(x) {2*x})");
        assertEval("rapply(list(NULL), function(x) {2*x}, how=\"list\")");
        assertEval("rapply(list(NULL), function(x) {2*x}, how=\"replace\")");
    }

    @Test
    public void testLapplyExpression() {
        assertEval("{rapply(expression('a+b'), function(x) { x })}");
        assertEval("{rapply(expression('a+b'), function(x)x)}");
        assertEval("{rapply(expression(list(1,2,3)), function(x)x)}");
        assertEval("{rapply(expression(a=1, 2, b=3), function(x)x)}");
        assertEval("{rapply(expression(a=1, 2, b=list(1,2,3)), function(x)x)}");
        assertEval("{rapply(expression(a=1, 2, b=list(1,2,3), c=expression('a+b')), function(x)x)}");
        assertEval("{rapply(expression(a=1, 2, b=list(1,2,3), list(1,2,3), c=expression('a+b')), function(x)x)}");
        assertEval("{rapply(expression(a=1, 2, b=list(1,2,3), list(1,2,3), c=expression('a+b'), expression('a+b')), function(x)x)}");
    }
}
