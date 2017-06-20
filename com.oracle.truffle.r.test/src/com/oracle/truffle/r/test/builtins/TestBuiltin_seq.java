/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 2012-2014, Purdue University
 * Copyright (c) 2013, 2017, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.test.builtins;

import org.junit.Test;

import com.oracle.truffle.r.test.TestBase;

/**
 * Tests for {@code seq} and {@code seq.int}.
 *
 * For numeric inputs {@ode seq}, aka {@code seq.default} should be identical in behavior to
 * {@code seq.int}; there are a couple of exceptions but these are likely GNU R bugs that will be
 * fixed. FastR defines fast paths for {@ode seq}, {@code seq.default} that redirect to
 * {@code seq.int} so testing both is essentially redundant.
 *
 * The tests are broken into methods that test one of the standard forms, i.e.:
 *
 * <ul>
 * <li>seq(from)</li>
 * <li>seq(from, to)</li>
 * <li>seq(from, to, by= )</li>
 * <li>seq(from, to, length.out= )</li>
 * <li>seq(along.with= )</li>
 * <li>seq(length.out= )</li>
 *
 * </ul>
 *
 * Some pre-existing tests are retained, although some overlap new tests.
 */
public class TestBuiltin_seq extends TestBase {

    private static final String[] BOTH_SEQFUNS = new String[]{"seq", "seq.int"};
    private static final String[] SEQFUNS = new String[]{"seq"};
    private static final String[] INT_VALUES = new String[]{"0L", "1L", "30L", "-1L", "-30L"};
    private static final String[] DOUBLE_VALUES = new String[]{"0.2", "1.1", "2.999999999", "5", "33.3", "-0.2", "-1.1", "-2.999999999", "-5", "-33.3"};
    private static final String[] BY_INT_VALUES = new String[]{"0L", "1L", "2L", "-1L"};
    private static final String[] BY_DOUBLE_VALUES = new String[]{"0", "1.1", "2.3", "-1.1"};

    // No args special case

    @Test
    public void testNoArgs() {
        assertEval(template("%0()", SEQFUNS));
    }

    // One from arg

    @Test
    public void testOneNumericFrom() {
        assertEval(template("%0(%1)", SEQFUNS, new String[]{"integer()", "double()"}));
        assertEval(template("%0(%1)", SEQFUNS, INT_VALUES));
        assertEval(template("%0(%1)", SEQFUNS, DOUBLE_VALUES));
    }

    @Test
    public void testOneNotNumericFrom() {
        assertEval(template("%0(%1)", SEQFUNS, new String[]{"logical()", "character()", "complex()", "raw()", "expression()", "list()", "pairlist()", "function() {}"}));
        assertEval(template("%0(%1)", SEQFUNS, new String[]{"F", "T", "1+1i", "\"abc\"", "as.raw(40L)", "list(2)"}));
    }

    // seq(from, to)

    @Test
    public void testFromToNumeric() {
        assertEval(template("%0(%1, %2)", SEQFUNS, INT_VALUES, INT_VALUES));
        assertEval(template("%0(%1, %2)", SEQFUNS, DOUBLE_VALUES, DOUBLE_VALUES));
        assertEval(template("%0(%1, %2)", SEQFUNS, INT_VALUES, DOUBLE_VALUES));
        assertEval(template("%0(%1, %2)", SEQFUNS, DOUBLE_VALUES, INT_VALUES));
        // behaves differently in seq.default/seq.int
        assertEval(template("%0(\"2\", \"3\")", BOTH_SEQFUNS));
    }

    // seq(from, to, by=)
    @Test
    public void testFromToByNumeric() {
        assertEval(Output.MayIgnoreErrorContext, template("%0(%1, %2, %3)", SEQFUNS, INT_VALUES, INT_VALUES, BY_INT_VALUES));
        assertEval(Output.MayIgnoreErrorContext, template("%0(%1, %2, %3)", SEQFUNS, DOUBLE_VALUES, DOUBLE_VALUES, BY_INT_VALUES));
        assertEval(Output.MayIgnoreErrorContext, template("%0(%1, %2, %3)", SEQFUNS, DOUBLE_VALUES, DOUBLE_VALUES, BY_DOUBLE_VALUES));
        // tests setting of last value to "to"
        assertEval(template("%0(2.3, 7.6, 0.1)", SEQFUNS));
    }

    // seq(from, to, by, length.out=)
    @Test
    public void testFromToLengthOutNumeric() {
        assertEval(Output.MayIgnoreErrorContext, template("%0(%1, length.out=%2)", SEQFUNS, INT_VALUES, INT_VALUES));
        assertEval(Output.MayIgnoreErrorContext, template("%0(%1, length.out=%2)", SEQFUNS, DOUBLE_VALUES, DOUBLE_VALUES));
        assertEval(Output.MayIgnoreErrorContext, template("%0(to=%1, length.out=%2)", SEQFUNS, INT_VALUES, INT_VALUES));
        assertEval(Output.MayIgnoreErrorContext, template("%0(to=%1, length.out=%2)", SEQFUNS, DOUBLE_VALUES, DOUBLE_VALUES));
        assertEval(Output.MayIgnoreErrorContext, template("%0(%1, %2, length.out=%3)", SEQFUNS, INT_VALUES, INT_VALUES, INT_VALUES));
        assertEval(Output.MayIgnoreErrorContext, template("%0(%1, %2, length.out=%3)", SEQFUNS, DOUBLE_VALUES, DOUBLE_VALUES, DOUBLE_VALUES));
    }

    // seq(along.with= )
    @Test
    public void testAlongWith() {
        assertEval(Output.MayIgnoreErrorContext, template("%0(along.with=%1)", SEQFUNS, new String[]{"0", "1", "10", "-1", "2.3", "-2.3", "T", "F", "c(1,2)", "list(2,3,4)"}));
    }

    // seq(length.out=)

    @Test
    public void testLengthOutOnly() {
        assertEval(Output.MayIgnoreErrorContext, template("%0(length.out=%1)", SEQFUNS, INT_VALUES));
        assertEval(Output.MayIgnoreErrorContext, template("%0(length.out=%1)", SEQFUNS, DOUBLE_VALUES));
    }

    // missing (aka empty) parameters
    @Test
    public void testEmptyParams() {
        assertEval(template("%0(,5)", BOTH_SEQFUNS));
        assertEval(template("%0(,,5)", BOTH_SEQFUNS));
        assertEval(template("%0(,,,5)", BOTH_SEQFUNS));
    }

    // generic dispatch
    @Test
    public void testSeqDispatch() {
        assertEval(template("{ d <- as.Date(1, origin = \"1970-01-01\"); %0(d, by=1, length.out=4) }", BOTH_SEQFUNS));
    }

    private static final String[] NOT_FINITE = new String[]{"NA_real_", "Inf", "NaN", "NA_integer_"};
    private static final String[] NOT_LENGTH_ONE = new String[]{"c(5,2)"};

    // error conditions
    @Test
    public void testErrors() {
        // seq(from)
        assertEval(Output.MayIgnoreErrorContext, template("%0(%1)", SEQFUNS, NOT_FINITE));
        // seq(from, to)
        assertEval(Output.MayIgnoreErrorContext, template("%0(%1, %2)", SEQFUNS, NOT_LENGTH_ONE, NOT_LENGTH_ONE));
        // seq(from, to, by, length.out =)
        assertEval(Output.MayIgnoreWarningContext, template("%0(1, length.out=%1)", SEQFUNS, NOT_LENGTH_ONE));
        assertEval(Output.MayIgnoreWarningContext, template("%0(1, 20, length.out=%1)", SEQFUNS, NOT_LENGTH_ONE));
        assertEval(Output.MayIgnoreErrorContext, template("%0(1, 20, 3, length.out=10)", SEQFUNS));
    }

    // argument matching corner-case appearing in seq (because it has a fast-path and varargs)
    // taken and adapted from the scales package
    @Test
    public void testSeqArgMatching() {
        assertEval("{ foo <- function(beg, end, by, len) seq(beg, end, by, length.out = len); foo(beg=1, by=1, len=10) }");
    }

    // Old tests, undoubtedly partially overlapping

    @Test
    public void testseq29() {
        assertEval("argv <- structure(list(0, 38431.66015625, by = 1000), .Names = c('',     '', 'by'));do.call('seq', argv)");
    }

    @Test
    public void testseq30() {
        assertEval("argv <- structure(list(18000, 28000, length = 50L), .Names = c('',     '', 'length'));do.call('seq', argv)");
    }

    @Test
    public void testSequenceStatement() {
        assertEval("{ seq(1L,10L) }");
        assertEval("{ seq(10L,1L) }");
        assertEval("{ seq(1L,4L,2L) }");
        assertEval("{ seq(1,-4,-2) }");
        assertEval("{ seq(0,0,0) }");
        assertEval("{ seq(0,0) }");
        assertEval("{ seq(0L,0L,0L) }");
        assertEval("{ seq(0L,0L) }");
        assertEval("{ seq(0,0,1i) }");
        assertEval(Output.IgnoreErrorContext, "{ seq(integer(), 7) }");
        assertEval(Output.MayIgnoreErrorContext, "{ seq(c(1,2), 7) }");
        assertEval(Output.IgnoreErrorContext, "{ seq(7, integer()) }");
        assertEval(Output.MayIgnoreErrorContext, "{ seq(7, c(41,42)) }");
        assertEval("{ seq(integer()) }");
        assertEval("{ seq(double()) }");
        assertEval("{ seq(from=3L, length.out=3L) }");
        assertEval("{ seq(to=10L, by=1) }");
        assertEval("{ seq(to=10L, by=1.1) }");

        assertEval("{ typeof(seq(1L, 3L)) }");
        assertEval("{ typeof(seq(1, 3)) }");
        assertEval("{ typeof(seq(1L, 3L, by=2)) }");
        assertEval("{ typeof(seq(1L, 3L, by=2L)) }");
        assertEval("{ typeof(seq(1L, 3L, length.out=2)) }");
        assertEval("{ typeof(seq(1L, 3L, length.out=2L)) }");
        assertEval("{ typeof(seq(FALSE, TRUE)) }");
        assertEval("{ typeof(seq(TRUE, FALSE, length.out=5)) }");
        assertEval("{ typeof(seq(TRUE, FALSE, length.out=5L)) }");
        assertEval("{ typeof(seq(1L, 3)) }");
        assertEval("{ typeof(seq(1L, 3, by=2)) }");
        assertEval("{ typeof(seq(1L, 3, by=2L)) }");
        assertEval("{ typeof(seq(1L, 3, length.out=5)) }");
        assertEval("{ typeof(seq(1L, 3, length.out=5L)) }");
        assertEval("{ typeof(seq(1, 3L)) }");
        assertEval("{ typeof(seq(1, 3L, by=2)) }");
        assertEval("{ typeof(seq(1, 3L, by=2L)) }");
        assertEval("{ typeof(seq(1, 3L, length.out=5)) }");
        assertEval("{ typeof(seq(1, 3L, length.out=5L)) }");
        assertEval("{ typeof(seq(to=3L, length.out=2)) }");
        assertEval("{ typeof(seq(to=3L, length.out=2L)) }");
        assertEval("{ typeof(seq(to=3L, by=5)) }");
        assertEval("{ typeof(seq(to=3L, by=5L)) }");
        assertEval("{ typeof(seq(along.with=c(1,2))) }");
        assertEval("{ typeof(seq(1, length.out=0)) }");
        assertEval("{ typeof(seq(1, length.out=0L)) }");
        assertEval("{ typeof(seq(1, along.with=double())) }");
        assertEval("{ typeof(seq(1L, along.with=double())) }");
    }

    @Test
    public void testSequenceStatementNamedParams() {
        assertEval("{ seq(from=1,to=3) }");
        assertEval("{ seq(length.out=1) }");
        assertEval("{ seq(from=1.4) }");
        assertEval("{ seq(from=1.7) }");
        assertEval("{ seq(from=1,to=3,by=1) }");
        assertEval("{ seq(from=-10,to=-5,by=2) }");

        assertEval("{ seq(length.out=0) }");

        assertEval("{ seq(to=-1,from=-10) }");
        assertEval("{ seq(length.out=13.4) }");
        assertEval("{ seq(along.with=10) }");
        assertEval("{ seq(along.with=NA) }");
        assertEval("{ seq(along.with=1:10) }");
        assertEval("{ seq(along.with=-3:-5) }");
        assertEval("{ seq(from=10:12) }");
        assertEval("{ seq(from=c(TRUE, FALSE)) }");
        assertEval("{ seq(from=TRUE, to=TRUE, length.out=0) }");
        assertEval("{ round(seq(from=10.5, to=15.4, length.out=4), digits=5) }");
        assertEval("{ seq(from=11, to=12, length.out=2) }");
        assertEval("{ seq(from=-10.4,to=-5.8,by=2.1) }");
        assertEval("{ round(seq(from=3L,to=-2L,by=-4.2), digits=5) }");
        assertEval("{ seq(along=c(10,11,12)) }"); // test partial name match
    }
}
