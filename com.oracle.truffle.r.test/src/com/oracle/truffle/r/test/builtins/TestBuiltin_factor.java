/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 2012-2014, Purdue University
 * Copyright (c) 2013, 2015, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.test.builtins;

import org.junit.*;

import com.oracle.truffle.r.test.*;

// Checkstyle: stop line length check

public class TestBuiltin_factor extends TestBase {

    @Test
    public void testfactor1() {
        assertEval("argv <- structure(list(x = c(1L, 0L, 1L, 1L, 1L, 1L, 1L, 1L,     0L, 0L, 0L, 0L, 1L, 1L, 0L, 1L, 0L, 1L, 1L, 1L, 0L, 1L, 0L,     0L, 1L, 1L, 0L, 0L, 1L, 1L, 0L, 0L, 0L, 0L, 0L, 0L, 1L, 0L,     0L, 0L, 1L, 0L, 0L, 1L, 1L, 1L, 1L, 0L, 0L, 0L, 1L, 1L, 0L,     1L, 1L, 1L, 0L, 1L, 0L, 1L, 1L, 0L, 1L, 1L, 1L, 1L, 1L, 0L,     0L, 1L, 0L, 1L, 1L, 1L, 0L, 0L, 0L, 1L, 0L, 1L, 0L, 0L, 0L,     1L, 0L, 0L, 0L, 0L, 1L, 0L, 1L, 1L, 0L, 0L, 0L, 1L, 0L, 0L,     1L, 0L, 1L, 0L, 1L, 1L, 1L, 0L, 1L, 1L, 1L, 1L, 1L, 1L, 1L,     1L, 1L, 1L, 0L, 0L, 1L, 0L, 0L, 0L, 0L, 1L, 1L, 1L, 0L, 0L,     0L, 0L, 0L, 0L, 1L, 1L, 0L, 1L, 1L, 1L, 0L, 0L, 1L, 1L, 1L,     0L, 0L, 1L, 1L, 1L, 1L, 0L, 1L, 1L, 0L, 0L, 0L, 1L, 0L, 0L,     0L, 1L, 1L, 1L, 0L, 1L, 0L, 0L, 0L, 1L, 1L, 0L, 0L, 0L, 0L,     1L, 0L, 0L, 0L, 0L, 1L, 0L, 0L, 1L, 1L, 1L, 0L, 1L, 0L, 0L,     1L, 1L, 1L, 0L, 1L, 1L, 1L, 0L, 0L, 0L, 1L, 0L, 0L, 1L, 0L,     0L, 0L, 1L, 0L, 0L, 1L, 0L, 1L, 0L, 0L, 1L, 0L, 0L, 1L, 0L,     1L, 1L, 0L, 1L, 1L, 0L, 1L, 0L, 1L, 0L, 1L, 0L, 0L, 1L, 1L,     1L, 0L, 1L, 1L, 1L, 1L, 1L, 0L, 0L, 1L, 0L, 1L, 0L, 0L, 0L,     0L, 1L, 0L, 0L, 0L, 1L, 1L, 1L, 0L, 1L, 0L, 1L, 0L, 1L, 0L,     1L, 1L, 1L, 1L, 0L, 0L, 1L, 0L, 0L, 1L, 0L, 1L, 1L, 1L, 1L,     1L, 1L, 0L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 0L, 1L, 0L, 0L, 0L,     0L, 0L, 1L, 1L, 0L, 0L, 0L, 0L, 1L, 1L, 0L, 1L, 1L, 1L, 1L,     1L, 1L, 1L, 1L, 1L, 1L, 1L, 0L, 0L, 0L, 0L, 1L, 1L, 1L, 0L,     1L, 1L, 1L, 1L, 1L, 1L, 1L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L,     1L, 0L, 1L, 1L, 1L, 0L, 1L, 1L, 0L, 1L, 1L, 0L, 1L, 1L, 1L,     0L, 1L, 1L, 0L, 1L, 1L, 1L, 1L, 0L, 0L, 0L, 0L, 0L, 0L, 0L,     0L, 1L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 1L, 1L, 1L,     1L, 1L, 1L, 1L, 0L, 1L, 1L, 1L, 0L, 0L, 1L, 0L, 1L, 1L, 1L,     1L, 1L, 0L, 0L, 1L, 1L, 1L, 1L, 1L, 0L, 1L, 1L, 1L, 1L, 0L,     0L, 1L, 1L, 1L, 1L, 0L, 0L, 0L, 1L, 1L, 1L, 1L, 0L, 1L, 1L,     0L, 0L, 0L, 1L, 1L, 1L, 0L, 0L, 1L, 1L, 1L, 1L, 1L, 1L, 1L,     0L, 0L, 0L, 1L, 0L, 0L, 0L, 0L, 1L, 0L, 0L, 1L, 0L, 1L, 1L,     1L, 1L, 0L, 1L, 1L, 0L, 1L, 0L, 1L, 0L, 0L, 1L, 1L, 1L, 0L,     1L, 0L, 0L, 1L, 0L, 0L, 1L, 1L, 1L, 0L, 0L, 0L, 0L, 0L, 1L,     0L, 0L, 1L, 1L, 1L, 1L, 1L, 0L, 0L, 0L, 1L, 1L, 0L, 1L, 0L,     0L, 1L, 1L, 0L, 1L, 0L, 1L, 1L, 1L, 1L, 0L, 0L, 0L, 0L, 1L,     1L, 0L, 1L, 0L, 0L, 0L, 1L, 1L, 0L, 1L, 0L, 0L, 0L, 0L, 0L,     1L, 0L, 1L, 0L, 1L, 1L, 0L, 1L, 0L, 0L, 1L, 0L, 0L, 0L, 1L,     0L, 1L, 1L, 1L, 1L, 0L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 0L, 0L,     0L, 1L, 1L, 1L, 1L, 0L, 0L, 0L, 0L, 1L, 1L, 0L, 0L, 1L, 0L,     0L, 0L, 1L, 1L, 0L, 1L, 0L, 1L, 1L, 1L, 0L, 1L, 1L, 1L, 0L,     0L, 1L, 0L, 1L, 0L, 1L, 1L, 0L, 0L, 0L, 0L, 1L, 0L, 1L, 0L,     0L, 1L, 0L, 0L, 1L, 1L, 0L, 1L, 1L, 0L, 0L, 0L, 1L, 0L, 1L,     0L, 1L, 0L, 0L, 1L, 0L, 0L, 0L, 0L, 0L, 1L, 1L, 1L, 1L, 1L,     0L, 1L, 0L, 0L, 1L, 1L, 0L, 0L, 1L, 1L, 0L, 0L, 0L, 0L, 0L,     1L, 1L, 1L, 0L, 1L, 1L, 1L, 1L, 0L, 0L, 1L, 0L, 0L, 1L, 0L,     0L, 0L, 0L, 0L, 0L, 0L, 0L, 1L, 1L, 1L, 0L, 1L, 1L, 1L, 1L,     1L, 0L, 1L, 0L, 0L, 1L, 1L, 0L, 0L, 0L, 0L, 0L, 0L, 1L, 0L,     0L, 1L, 0L, 1L, 0L, 0L, 1L, 1L, 0L, 0L, 1L, 1L, 1L, 0L, 0L,     0L, 0L, 1L, 0L, 0L, 1L, 0L, 0L, 0L, 1L, 1L, 1L, 0L, 0L, 1L,     0L, 1L, 0L, 1L, 0L, 1L, 0L, 1L, 1L, 1L, 0L, 0L, 1L, 0L, 1L,     1L, 0L, 1L, 1L, 0L, 1L, 1L, 0L, 0L, 0L, 1L, 1L, 1L, 1L, 0L,     0L, 0L, 1L, 1L, 0L, 0L, 0L, 0L, 1L, 1L, 1L, 1L, 0L, 1L, 0L,     0L, 0L, 1L, 0L, 1L, 0L, 1L, 1L, 0L, 0L, 0L, 1L, 1L, 0L, 0L,     1L, 0L, 0L, 1L, 0L, 0L, 1L, 0L, 1L, 0L, 0L, 0L, 1L, 1L, 0L,     1L, 0L, 0L, 0L, 0L, 0L, 0L, 1L, 1L, 0L, 1L, 0L, 0L, 1L, 1L,     0L, 1L, 0L, 1L, 0L, 1L, 1L, 1L, 0L, 0L, 0L, 0L, 1L, 0L, 0L,     0L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 0L, 1L, 1L, 0L, 1L, 0L, 0L,     0L, 1L, 1L, 1L, 0L, 1L, 0L, 0L, 0L, 1L, 0L, 1L, 0L, 1L, 0L,     1L, 1L, 1L, 1L, 0L, 1L, 1L, 1L, 1L, 1L, 0L, 1L, 0L, 1L, 1L,     0L, 0L, 1L, 0L, 1L, 0L, 1L, 0L, 0L, 0L, 1L, 0L, 0L, 0L, 1L,     0L, 1L, 1L, 0L, 1L, 1L, 1L, 1L, 0L, 0L, 1L, 1L, 1L, 0L, 0L,     1L, 0L, 0L, 1L, 0L, 1L, 0L, 1L, 0L, 1L, 1L, 0L, 0L, 1L, 0L,     0L, 1L, 0L, 0L, 1L, 0L, 0L, 0L, 1L, 1L, 1L, 0L, 0L, 0L, 0L,     0L, 1L, 1L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 1L, 0L, 1L, 1L, 0L,     0L, 1L, 0L, 0L, 1L, 1L, 0L, 1L, 1L, 0L, 0L, 0L, 1L, 0L, 0L,     0L, 0L, 1L, 1L, 0L, 1L, 0L, 0L, 1L, 1L, 0L, 1L, 1L, 0L, 1L,     1L, 1L, 1L, 1L, 0L, 1L, 1L, 1L, 0L, 1L, 0L, 1L, 1L, 0L, 1L,     0L, 1L)), .Names = 'x');" +
                        "do.call('factor', argv)");
    }

    @Test
    public void testFactor() {

        assertEval("{data = c(1,2,2,3,1,2,3,3,1,2,3,3,1);fdata<-factor(data);fdata}");
        assertEval("{data = c(1,2,2,3,1,2,3,3,1,2,3,3,1);rdata = factor(data,labels=c(\"I\",\"II\",\"III\"));rdata;}");
        assertEval("{data = c(1,2,2,3,1,2,3,3,1,2,3,3,1);fdata<-factor(data);levels(fdata) = c('I','II','III');fdata;}");
        assertEval("{set.seed(124);l1 = factor(sample(letters,size=10,replace=TRUE));set.seed(124);l2 = factor(sample(letters,size=10,replace=TRUE));l12 = factor(c(levels(l1)[l1],levels(l2)[l2]));l12;}");
        assertEval("{set.seed(124); schtyp <- sample(0:1, 20, replace = TRUE);schtyp.f <- factor(schtyp, labels = c(\"private\", \"public\")); schtyp.f;}");
        // Checkstyle: stop line length check
        assertEval("{ses <- c(\"low\", \"middle\", \"low\", \"low\", \"low\", \"low\", \"middle\", \"low\", \"middle\", \"middle\", \"middle\", \"middle\", \"middle\", \"high\", \"high\", \"low\", \"middle\", \"middle\", \"low\", \"high\"); ses.f.bad.order <- factor(ses); is.factor(ses.f.bad.order);levels(ses.f.bad.order);ses.f <- factor(ses, levels = c(\"low\", \"middle\", \"high\"));ses.order <- ordered(ses, levels = c(\"low\", \"middle\", \"high\"));ses.order; } ");
        // Checkstyle: resume line length check

        assertEval("{ x<-factor(c(\"a\", \"b\", \"a\")); attr(x, \"levels\")<-NULL; as.character(x) }");
        assertEval("{ x<-factor(c(\"a\", \"b\", \"a\")); attr(x, \"levels\")<-character(); as.character(x) }");
        assertEval(Output.ContainsError, "{ x<-c(1,2,3); class(x)<-\"factor\"; x }");
        assertEval(Output.ContainsError, "{ x<-c(\"1\",\"2\",\"3\"); class(x)<-\"factor\"; x }");
        assertEval(Output.ContainsError, "{ x<-c(1L,2L,3L); class(x)<-\"factor\"; x }");

        assertEval("{ x<-factor(c(\"a\", \"b\", \"a\")); attr(x, \"levels\")<-c(7L, 42L); x  }");
        assertEval("{ x<-factor(c(\"a\", \"b\", \"a\")); attr(x, \"levels\")<-c(7, 42); x }");
        assertEval("{ x<-factor(c(\"a\", \"b\", \"a\")); attr(x, \"levels\")<-c(FALSE, TRUE); x }");
        assertEval("{ x<-factor(c(\"a\", \"b\", \"a\")); attr(x, \"levels\")<-c(7+7i, 42+42i); x }");
        assertEval(Output.ContainsError, "{ x<-factor(c(\"a\", \"b\", \"a\")); attr(x, \"levels\")<-c(as.raw(7), as.raw(42)); x }");

        assertEval("{ x<-factor(c(\"a\", \"b\", \"a\")); x == \"a\" }");
        // these would fail if comparison was performed on strings
        assertEval("{ x<-factor(c(\"a\", \"b\", \"a\")); attr(x, \"levels\")<-c(+7L, +42L); x == 7 }");
        assertEval("{ x<-factor(c(\"a\", \"b\", \"a\")); attr(x, \"levels\")<-c(+7, +42); x == 7 }");
        assertEval("{ x<-factor(c(\"a\", \"b\", \"a\")); attr(x, \"levels\")<-c(+7+7i, +42+42i); x == 7+7i }");
        assertEval("{ x<-factor(c(\"a\", \"b\", \"a\")); attr(x, \"levels\")<-c(as.raw(7), as.raw(42)); x == as.raw(7) }");

        assertEval(Output.ContainsWarning, "{ x<-factor(c(\"a\", \"b\", \"a\")); x == c(\"a\", \"b\") }");

        assertEval("{ x<-factor(c(\"a\", \"b\", \"a\", \"c\")); x == c(\"a\", \"b\") }");
        assertEval("{ x<-factor(c(\"a\", \"b\", \"a\"), ordered=TRUE); x > \"a\" }");

        assertEval("{ x<-c(1L, 2L, 1L); class(x)<-c(\"ordered\", \"factor\"); levels(x)<-c(\"a\", \"b\"); x > \"a\" }");

        assertEval(Output.ContainsWarning, "{ x<-factor(c(\"c\", \"b\", \"a\", \"c\")); y<-list(1); y[1]<-x; y }");
        assertEval(Output.ContainsWarning, "{ x<-factor(c(\"c\", \"b\", \"a\", \"c\")); y<-c(1); y[1]<-x; y }");
        assertEval("{ x<-factor(c(\"c\", \"b\", \"a\", \"c\")); y<-list(1); y[[1]]<-x; y }");
        assertEval(Output.ContainsError, "{ x<-factor(c(\"c\", \"b\", \"a\", \"c\")); y<-c(1); y[[1]]<-x; y }");

        assertEval("{ x<-factor(c(\"a\", \"b\", \"a\")); x[1] }");
        assertEval("{ x<-factor(c(\"a\", \"b\", \"a\")); x[[1]] }");
        assertEval("{ x<-factor(c(\"a\", \"b\", \"a\")); x[2] }");
        assertEval("{ x<-factor(c(\"a\", \"b\", \"a\")); x[[2]] }");
        assertEval("{ x<-factor(c(\"a\", \"b\", \"a\")); x[c(1,2)] }");
        assertEval("{ x<-factor(c(\"a\", \"b\", \"a\")); x[c(1,2,3,4)] }");

        assertEval("{ x<-factor(c(\"a\", \"b\", \"a\")); is.atomic(x) }");
        assertEval("{ x<-factor(c(\"a\", \"b\", \"a\"), ordered=TRUE); is.atomic(x) }");

        assertEval("{ as.logical(factor(c(\"a\", \"b\", \"a\"))) }");
        assertEval("{ as.logical(factor(integer())) }");

        assertEval("{ x<-structure(c(1.1,2.2,1.1), .Label=c(\"a\", \"b\"), class = c('factor')); attributes(x) }");
        assertEval("{ x<-structure(c(1.1,2.2,1.1), .Label=c(\"a\", \"b\"), class = c('factor')); x }");
        assertEval("{ x<-structure(c(1.2,2.2,1.1), .Label=c(\"a\", \"b\"), class = c('factor')); x }");
        assertEval("{ x<-structure(c(2.2,3.2,2.1), .Label=c(\"a\", \"b\"), class = c('factor')); as.integer(x) }");

        assertEval("{ x<-structure(c(1,2,1), .Label=c(\"a\", \"b\"), class = c('factor'), .Names=c(\"111\",\"112\",\"113\")); y<-structure(c(1,2,1), .Label=c(\"a\", \"b\"), class = c('factor'), .Names=c(\"111\",\"112\",\"113\")); x==y }");

        assertEval("{ x<-structure(factor(c(\"a\",\"b\",\"c\")), class=NULL); x }");
        assertEval("{ x<-factor(c(\"a\", \"b\", \"a\")); levels(x)<-c(7,42); x }");
        assertEval("{ x<-factor(c(\"a\", \"b\", \"a\")); levels(x)<-c(7,42); is.character(levels(x)) }");

        assertEval("{ x <- factor(c(\"a\", \"b\", \"a\")); attr(x, \"levels\")<-c(7L, 42L); is.integer(levels(x)) }");
        assertEval("{ x <- factor(c(\"a\", \"b\", \"a\")); attr(x, \"levels\")<-c(7, 42); is.double(levels(x)) }");
        assertEval("{ x <- factor(c(\"a\", \"b\", \"a\")); attr(x, \"levels\")<-c(FALSE, TRUE); is.logical(levels(x)) }");
        assertEval("{ x <- factor(c(\"a\", \"b\", \"a\")); levels(x)<-c(7, 42); is.character(levels(x)) }");
        assertEval("{ x <- factor(c(\"a\", \"b\", \"a\")); attr(x, \"levels\")<-c(7+7i, 42+42i); is.complex(levels(x)) }");
        assertEval("{ x <- factor(c(\"a\", \"b\", \"a\")); attr(x, \"levels\")<-c(as.raw(7), as.raw(42)); is.raw(levels(x)) }");

        assertEval("{ z=factor(c(\"a\", \"b\", \"a\")); z[1] = \"b\"; z }");

        assertEval("{ x<-structure(c(1,2,1), .Label=c(\"a\", \"b\"), class = c('factor'), .Names=c(\"111\",\"112\",\"113\")); names(x) }");

        assertEval(Output.ContainsWarning, "{ x<-factor(c(\"a\", \"b\", \"a\"), ordered=TRUE); x + \"a\" }");
        assertEval(Output.ContainsWarning, "{ x<-c(1L, 2L, 1L); class(x)<-c(\"ordered\", \"factor\"); levels(x)<-c(\"a\", \"b\"); x + \"a\" }");

        assertEval(Output.ContainsWarning, "{ x<-factor(c(\"a\", \"b\", \"a\")); x > \"a\" }");
        assertEval(Output.ContainsWarning, "{ x<-factor(c(\"a\", \"b\", \"a\")); x + \"a\" }");
        assertEval(Output.ContainsWarning, "{ x<-factor(c(\"a\", \"b\", \"a\")); x > c(\"a\", \"b\") }");
        assertEval(Output.ContainsWarning, "{ x<-factor(c(\"a\", \"b\", \"a\")); x + c(\"a\", \"b\") }");
        assertEval(Output.ContainsWarning, "{ x<-c(1L, 2L, 1L); class(x)<-c(\"factor\", \"ordered\"); levels(x)<-c(\"a\", \"b\"); x > \"a\" }");
        assertEval(Output.ContainsWarning, "{ x<-c(1L, 2L, 1L); class(x)<-c(\"factor\", \"ordered\"); levels(x)<-c(\"a\", \"b\"); x + \"a\" }");
        assertEval(Output.ContainsWarning,
                        "{ x<-structure(c(1,2,1), .Label=c(\"a\", \"b\"), class = c('factor'), .Names=c(\"111\",\"112\",\"113\")); y<-structure(c(1,2,1), .Label=c(\"a\", \"b\"), class = c('factor'), .Names=c(\"111\",\"112\",\"113\")); x+y }");
    }
}
