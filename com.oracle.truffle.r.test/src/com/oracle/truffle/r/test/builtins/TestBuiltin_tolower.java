/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 2012-2014, Purdue University
 * Copyright (c) 2013, 2016, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.test.builtins;

import org.junit.Test;

import com.oracle.truffle.r.test.TestBase;

// Checkstyle: stop line length check
public class TestBuiltin_tolower extends TestBase {

    @Test
    public void testtolower1() {
        assertEval("argv <- list('show'); .Internal(tolower(argv[[1]]))");
    }

    @Test
    public void testtolower2() {
        assertEval("argv <- list('TRUE'); .Internal(tolower(argv[[1]]))");
    }

    @Test
    public void testtolower3() {
        assertEval("argv <- list(c('title', 'author', 'year', 'note')); .Internal(tolower(argv[[1]]))");
    }

    @Test
    public void testtolower4() {
        assertEval("argv <- list(c('ChangeLog', 'DESCRIPTION', 'INDEX', 'MD5', 'NAMESPACE', 'PORTING', '0aaa.R', 'agnes.q', 'clara.q', 'clusGap.R', 'coef.R', 'daisy.q', 'diana.q', 'ellipsoidhull.R', 'fanny.q', 'internal.R', 'mona.q', 'pam.q', 'plothier.q', 'plotpart.q', 'silhouette.R', 'zzz.R', 'README', 'agriculture.tab', 'animals.tab', 'chorSub.rda', 'flower.R', 'plantTraits.rda', 'pluton.tab', 'ruspini.tab', 'votes.repub.tab', 'xclara.rda', 'CITATION', 'R-cluster.mo', 'R-cluster.mo', 'R-cluster.mo', 'agnes.Rd', 'agnes.object.Rd', 'agriculture.Rd', 'animals.Rd', 'bannerplot.Rd', 'chorSub.Rd', 'clara.Rd', 'clara.object.Rd', 'clusGap.Rd', 'clusplot.default.Rd', 'clusplot.partition.Rd', 'cluster-internal.Rd', 'coef.hclust.Rd', 'daisy.Rd', 'diana.Rd', 'dissimilarity.object.Rd', 'ellipsoidhull.Rd', 'fanny.Rd', 'fanny.object.Rd', 'flower.Rd', 'lower.to.upper.tri.inds.Rd', 'mona.Rd', 'mona.object.Rd', 'pam.Rd', 'pam.object.Rd', 'partition.object.Rd', 'plantTraits.Rd', 'plot.agnes.Rd', 'plot.diana.Rd', 'plot.mona.Rd', 'plot.partition.Rd', 'pltree.Rd', 'pltree.twins.Rd', 'pluton.Rd', 'predict.ellipsoid.Rd', 'print.agnes.Rd', 'print.clara.Rd', 'print.diana.Rd', 'print.dissimilarity.Rd', 'print.fanny.Rd', 'print.mona.Rd', 'print.pam.Rd', 'ruspini.Rd', 'silhouette.Rd', 'sizeDiss.Rd', 'summary.agnes.Rd', 'summary.clara.Rd', 'summary.diana.Rd', 'summary.mona.Rd', 'summary.pam.Rd', 'twins.object.Rd', 'volume.ellipsoid.Rd', 'votes.repub.Rd', 'xclara.Rd', 'R-cluster.pot', 'R-de.po', 'R-en@quot.po', 'R-pl.po', 'update-me.sh', 'clara.c', 'cluster.h', 'daisy.f', 'dysta.f', 'fanny.c', 'ind_2.h', 'init.c', 'mona.f', 'pam.c', 'sildist.c', 'spannel.c', 'twins.c', 'agnes-ex.R', 'agnes-ex.Rout.save', 'clara-NAs.R', 'clara-NAs.Rout.save', 'clara-ex.R', 'clara.R', 'clara.Rout.save', 'clusplot-out.R', 'clusplot-out.Rout.save', 'daisy-ex.R', 'daisy-ex.Rout.save', 'diana-boots.R', 'diana-ex.R', 'diana-ex.Rout.save', 'ellipsoid-ex.R', 'ellipsoid-ex.Rout.save', 'fanny-ex.R', 'mona.R', 'mona.Rout.save', 'pam.R', 'pam.Rout.save', 'silhouette-default.R', 'silhouette-default.Rout.save', 'sweep-ex.R', '.', 'R', 'data', 'inst', 'LC_MESSAGES', 'LC_MESSAGES', 'LC_MESSAGES', 'man', 'po', 'src', 'tests')); .Internal(tolower(argv[[1]]))");
    }

    @Test
    public void testtolower5() {
        assertEval("argv <- list(structure('base', .Names = 'Priority')); .Internal(tolower(argv[[1]]))");
    }

    @Test
    public void testtolower6() {
        assertEval("argv <- list(character(0)); .Internal(tolower(argv[[1]]))");
    }

    @Test
    public void testtolower8() {
        assertEval("argv <- structure(list(x = c('NA', NA, 'BANANA')), .Names = 'x');do.call('tolower', argv)");
    }

    @Test
    public void testCharUtils() {
        assertEval("{ tolower(c(\"Hello\",\"ByE\")) }");
        assertEval("{ tolower(c()) }");

        // double-to-string conversion problem
        assertEval(Ignored.OutputFormatting, "{ tolower(1E100) }");
        assertEval("{ tolower(c(a=\"HI\", \"HELlo\")) }");
        assertEval("{ tolower(NA) }");

        assertEval("tolower(c('NA', 'na'))");
        assertEval("tolower(NA_integer_)");
        assertEval("tolower(NA_real_)");
    }
}
