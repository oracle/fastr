/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 2014, Purdue University
 * Copyright (c) 2014, 2015, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.test.builtins;

import org.junit.*;

import com.oracle.truffle.r.test.*;

// Checkstyle: stop line length check
public class TestBuiltin_strptime extends TestBase {

    @Test
    public void teststrptime1() {
        assertEval("argv <- list('2008-04-22 09:45', '%Y-%m-%d', ''); .Internal(strptime(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void teststrptime2() {
        assertEval("argv <- list(character(0), '%X', ''); .Internal(strptime(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void teststrptime3() {
        assertEval("argv <- list('1970-01-01', '%Y-%m-%d %H:%M', 'GMT'); .Internal(strptime(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void teststrptime4() {
        assertEval("argv <- list('2007-11-06', '%Y-%m-%d', 'GMT'); .Internal(strptime(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void teststrptime5() {
        assertEval("argv <- list('1970-01-01', '%Y-%m-%d', 'GMT'); .Internal(strptime(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void teststrptime6() {
        assertEval("argv <- list(c('2007-11-06', NA), '%Y-%m-%d', ''); .Internal(strptime(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void teststrptime7() {
        assertEval("argv <- list(c('20010101', NA, NA, '20041026'), '%Y%m%d', 'GMT'); .Internal(strptime(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void teststrptime8() {
        assertEval("argv <- list('2002-02-02 02:02', '%Y-%m-%d %H:%M', ''); .Internal(strptime(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void teststrptime9() {
        assertEval("argv <- list(c('1890/01/01', '1891/01/01', '1892/01/01', '1893/01/01', '1894/01/01', '1895/01/01'), '%Y/%m/%d', ''); .Internal(strptime(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void teststrptime10() {
        assertEval("argv <- list(c('1937/01/01', '1916/01/01', '1913/01/01', '1927/01/01', '1947/01/01', '1913/01/01', '1917/01/01', '1923/01/01', '1921/01/01', '1926/01/01', '1920/01/01', '1915/01/01', '1914/01/01', '1914/01/01', '1914/01/01', '1919/01/01', '1948/01/01', '1911/01/01', '1909/01/01', '1913/01/01', '1925/01/01', '1926/01/01', '1910/01/01', '1917/01/01', '1936/01/01', '1938/01/01', '1960/01/01', '1915/01/01', '1919/01/01', '1924/01/01', '1914/01/01', '1905/01/01', '1921/01/01', '1929/01/01', '1926/01/01', '1921/01/01', '1908/01/01', '1928/01/01', '1919/01/01', '1921/01/01', '1925/01/01', '1934/01/01', '1927/01/01', '1928/01/01', '1934/01/01', '1922/01/01', '1923/01/01', '1915/01/01', '1934/01/01', '1925/01/01', '1922/01/01', '1930/01/01', '1924/01/01', '1923/01/01', '1919/01/01', '1932/01/01', '1930/01/01', '1923/01/01', '1930/01/01', '1922/01/01', '1919/01/01', '1932/01/01', '1939/01/01', '1923/01/01', '1920/01/01', '1919/01/01', '1952/01/01', '1927/01/01', '1924/01/01', '1919/01/01', '1925/01/01', '1945/01/01', '1916/01/01', '1943/01/01', '1920/01/01', '1920/01/01', '1931/01/01', '1924/01/01', '1919/01/01', '1926/01/01', '1920/01/01', '1952/01/01', '1919/01/01', '1930/01/01', '1925/01/01', '1924/01/01', '1926/01/01', '1918/01/01', '1922/01/01', '1921/01/01', '1925/01/01', '1928/01/01', '1925/01/01', '1929/01/01', '1933/01/01', '1947/01/01', '1950/01/01', '1945/01/01', '1924/01/01', '1939/01/01', '1924/01/01', '1933/01/01', '1928/01/01'), '%Y/%m/%d', ''); .Internal(strptime(argv[[1]], argv[[2]], argv[[3]]))");
    }
}
