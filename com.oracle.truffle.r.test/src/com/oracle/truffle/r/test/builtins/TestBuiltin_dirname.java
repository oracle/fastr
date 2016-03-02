/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 2014, Purdue University
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.test.builtins;

import org.junit.*;

import com.oracle.truffle.r.test.*;

// Checkstyle: stop line length check
public class TestBuiltin_dirname extends TestBase {

    @Test
    public void testdirname1() {
        assertEval("argv <- list('/home/roman/r-instrumented/library/graphics'); .Internal(dirname(argv[[1]]))");
    }

    @Test
    public void testdirname2() {
        assertEval("argv <- list('/home/lzhao/hg/r-instrumented/tests/Packages/survival/inst/CITATION'); .Internal(dirname(argv[[1]]))");
    }

    @Test
    public void testdirname3() {
        assertEval("argv <- list(character(0)); .Internal(dirname(argv[[1]]))");
    }

    @Test
    public void testdirname4() {
        assertEval(Ignored.Unstable,
                        "argv <- list(c('ChangeLog', 'DESCRIPTION', 'INDEX', 'MD5', 'NAMESPACE', 'PORTING', 'R/0aaa.R', 'R/agnes.q', 'R/clara.q', 'R/clusGap.R', 'R/coef.R', 'R/daisy.q', 'R/diana.q', 'R/ellipsoidhull.R', 'R/fanny.q', 'R/internal.R', 'R/mona.q', 'R/pam.q', 'R/plothier.q', 'R/plotpart.q', 'R/silhouette.R', 'R/zzz.R', 'README', 'data/agriculture.tab', 'data/animals.tab', 'data/chorSub.rda', 'data/flower.R', 'data/plantTraits.rda', 'data/pluton.tab', 'data/ruspini.tab', 'data/votes.repub.tab', 'data/xclara.rda', 'inst/CITATION', 'inst/po/de/LC_MESSAGES/R-cluster.mo', 'inst/po/en@quot/LC_MESSAGES/R-cluster.mo', 'inst/po/pl/LC_MESSAGES/R-cluster.mo', 'man/agnes.Rd', 'man/agnes.object.Rd', 'man/agriculture.Rd', 'man/animals.Rd', 'man/bannerplot.Rd', 'man/chorSub.Rd', 'man/clara.Rd', 'man/clara.object.Rd', 'man/clusGap.Rd', 'man/clusplot.default.Rd', 'man/clusplot.partition.Rd', 'man/cluster-internal.Rd', 'man/coef.hclust.Rd', 'man/daisy.Rd', 'man/diana.Rd', 'man/dissimilarity.object.Rd', 'man/ellipsoidhull.Rd', 'man/fanny.Rd', 'man/fanny.object.Rd', 'man/flower.Rd', 'man/lower.to.upper.tri.inds.Rd', 'man/mona.Rd', 'man/mona.object.Rd', 'man/pam.Rd', 'man/pam.object.Rd', 'man/partition.object.Rd', 'man/plantTraits.Rd', 'man/plot.agnes.Rd', 'man/plot.diana.Rd', 'man/plot.mona.Rd', 'man/plot.partition.Rd', 'man/pltree.Rd', 'man/pltree.twins.Rd', 'man/pluton.Rd', 'man/predict.ellipsoid.Rd', 'man/print.agnes.Rd', 'man/print.clara.Rd', 'man/print.diana.Rd', 'man/print.dissimilarity.Rd', 'man/print.fanny.Rd', 'man/print.mona.Rd', 'man/print.pam.Rd', 'man/ruspini.Rd', 'man/silhouette.Rd', 'man/sizeDiss.Rd', 'man/summary.agnes.Rd', 'man/summary.clara.Rd', 'man/summary.diana.Rd', 'man/summary.mona.Rd', 'man/summary.pam.Rd', 'man/twins.object.Rd', 'man/volume.ellipsoid.Rd', 'man/votes.repub.Rd', 'man/xclara.Rd', 'po/R-cluster.pot', 'po/R-de.po', 'po/R-en@quot.po', 'po/R-pl.po', 'po/update-me.sh', 'src/clara.c', 'src/cluster.h', 'src/daisy.f', 'src/dysta.f', 'src/fanny.c', 'src/ind_2.h', 'src/init.c', 'src/mona.f', 'src/pam.c', 'src/sildist.c', 'src/spannel.c', 'src/twins.c', 'tests/agnes-ex.R', 'tests/agnes-ex.Rout.save', 'tests/clara-NAs.R', 'tests/clara-NAs.Rout.save', 'tests/clara-ex.R', 'tests/clara.R', 'tests/clara.Rout.save', 'tests/clusplot-out.R', 'tests/clusplot-out.Rout.save', 'tests/daisy-ex.R', 'tests/daisy-ex.Rout.save', 'tests/diana-boots.R', 'tests/diana-ex.R', 'tests/diana-ex.Rout.save', 'tests/ellipsoid-ex.R', 'tests/ellipsoid-ex.Rout.save', 'tests/fanny-ex.R', 'tests/mona.R', 'tests/mona.Rout.save', 'tests/pam.R', 'tests/pam.Rout.save', 'tests/silhouette-default.R', 'tests/silhouette-default.Rout.save', 'tests/sweep-ex.R')); .Internal(dirname(argv[[1]]))");
    }

    @Test
    public void testdirname5() {
        assertEval("argv <- list(structure('/home/lzhao/hg/r-instrumented/library/utils', .Names = 'Dir')); .Internal(dirname(argv[[1]]))");
    }

    @Test
    public void testdirname7() {
        assertEval("argv <- structure(list(path = character(0)), .Names = 'path');do.call('dirname', argv)");
    }

}
