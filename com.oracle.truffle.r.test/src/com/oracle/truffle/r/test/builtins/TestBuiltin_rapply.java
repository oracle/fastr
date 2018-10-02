/*
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 51
 * Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 *
 * Copyright (c) 2012-2014, Purdue University
 * Copyright (c) 2013, 2018, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.test.builtins;

import org.junit.Test;

import com.oracle.truffle.r.test.TestBase;

public class TestBuiltin_rapply extends TestBase {

    @Test
    public void testLapply() {
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
}
