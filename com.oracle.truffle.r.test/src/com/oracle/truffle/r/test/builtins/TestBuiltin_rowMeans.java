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
public class TestBuiltin_rowMeans extends TestBase {

    @Test
    public void testrowMeans1() {
        assertEval("argv <- list(structure(c(3, 3, NA, 3, 3, 3, 3, 3, 4, 3, NA, NA, 2, 3, 4, 5), .Dim = c(8L, 2L), .Dimnames = list(NULL, c('x1', 'x2'))), 8, 2, TRUE); .Internal(rowMeans(argv[[1]], argv[[2]], argv[[3]], argv[[4]]))");
    }

    @Test
    public void testrowMeans2() {
        assertEval("argv <- list(structure(c(50.7138381326659, 6.51590327164277, 24.9887625571708, 6.50401666172534, 16.6227411608333, 24.2873904534041, 56.036205319809, 9.42637482307856, 6.7207351702689e-16, 6.51590327164276, 106.05353593478, 13.0563348605106, 29.556736958112, 26.535297847233, 83.1597312749807, 86.1180411620546, 4.28836475146602, 3.05748120025494e-16, 24.9887625571708, 13.0563348605107, 382.901882167719, 28.709795659465, 7.19910301202793, 51.849911207061, 76.6652389324741, 13.4232601222667, 9.57039987233639e-16, 6.50401666172536, 29.556736958112, 28.709795659465, 286.290790661071, 29.5533327979648, 105.611010510127, 106.256264404531, 22.4644024278478, 1.60164752950704e-15, 16.6227411608333, 26.535297847233, 7.19910301202793, 29.5533327979648, 611.022025519874, 52.7749434153259, 19.5698513619914, 23.9507376116895, 1.70761896956049e-15, 24.2873904534041, 83.1597312749807, 51.849911207061, 105.611010510127, 52.7749434153259, 736.165134132116, 133.440685552903, 91.9053353168322, 6.55258708668096e-15, 56.036205319809, 86.1180411620546, 76.6652389324741, 106.256264404531, 19.5698513619915, 133.440685552903, 1401.55449200362, 107.582093653927, 7.67029504004995e-15, 9.42637482307856, 4.28836475146602, 13.4232601222667, 22.4644024278478, 23.9507376116895, 91.9053353168321, 107.582093653927, 57.6052682140803, 4.10709057665822e-15, 6.7207351702689e-16, 3.05748120025493e-16, 9.57039987233639e-16, 1.60164752950703e-15, 1.70761896956049e-15, 6.55258708668095e-15, 7.67029504004995e-15, 4.10709057665822e-15, 2.92823790737107e-31), .Dim = c(9L, 9L)), 9, 9, FALSE); .Internal(rowMeans(argv[[1]], argv[[2]], argv[[3]], argv[[4]]))");
    }

    @Test
    public void testrowMeans3() {
        assertEval("argv <- list(structure(c(2, 2, NA, 2, 2, 2, 2, 2, -5, -5, NA, NA, -5, -5, -5, -5), .Dim = c(8L, 2L), .Dimnames = list(NULL, c('x1', 'x2'))), 8, 2, TRUE); .Internal(rowMeans(argv[[1]], argv[[2]], argv[[3]], argv[[4]]))");
    }

    @Test
    public void testRowMeans() {
        assertEval("{ m <- matrix(1:6, nrow=2) ; rowMeans(x = m, na.rm = TRUE) }");
        assertEval("{ m <- matrix(c(1,2,3,4,5,6), nrow=2) ; rowMeans(m) }");
        assertEval("{ m <- matrix(c(NA,2,3,4,NA,6), nrow=2) ; rowMeans(m, na.rm = TRUE) }");
        assertEval("{ rowMeans(matrix(as.complex(1:6), nrow=2)) }");
        assertEval("{ rowMeans(matrix((1:6)*(1+1i), nrow=2)) }");

        assertEval("{rowMeans(matrix(c(3,4,2,5)))}");
        assertEval("{rowMeans(matrix(c(3L,4L,2L,5L)))}");
        assertEval("{rowMeans(matrix(c(TRUE,FALSE,FALSE,TRUE)))}");
        assertEval("{rowMeans(matrix(c(3+2i,4+5i,2+0i,5+10i)))}");
        assertEval("{rowMeans(matrix(c(3,4,NaN,5),ncol=2,nrow=2), na.rm = TRUE)}");
        assertEval("{rowMeans(matrix(c(3,4,NaN,5),ncol=2,nrow=2), na.rm = FALSE)}");
        assertEval("{rowMeans(matrix(c(3L,NaN,2L,5L),ncol=2,nrow=2), na.rm = TRUE)}");
        assertEval("{rowMeans(matrix(c(3L,NA,2L,5L),ncol=2,nrow=2), na.rm = TRUE)}");
        assertEval("{rowMeans(matrix(c(3L,NaN,2L,5L),ncol=2,nrow=2), na.rm = FALSE)}");
        assertEval("{rowMeans(matrix(c(3L,NA,2L,5L),ncol=2,nrow=2), na.rm = FALSE)}");
        assertEval("{rowMeans(matrix(c(TRUE,FALSE,FALSE,NaN),nrow=2,ncol=2), na.rm = TRUE)}");
        assertEval("{rowMeans(matrix(c(TRUE,FALSE,FALSE,NA),nrow=2,ncol=2), na.rm = TRUE)}");
        assertEval("{rowMeans(matrix(c(TRUE,FALSE,FALSE,NaN),nrow=2,ncol=2), na.rm = FALSE)}");
        assertEval("{rowMeans(matrix(c(TRUE,FALSE,FALSE,NA),nrow=2,ncol=2), na.rm = FALSE)}");
        assertEval("{rowMeans(matrix(c(NaN,4+5i,2+0i,5+10i),nrow=2,ncol=2), na.rm = TRUE)}");
        // Whichever value(NA or NaN) is first in the row will be returned for that row.
        assertEval("{rowMeans(matrix(c(NA,NaN,NaN,NA),ncol=2,nrow=2))}");

        // Error message mismatch
        assertEval(Ignored.Unknown, "{rowMeans(matrix(NA,NA,NA),TRUE)}");
        assertEval(Output.ContainsError, "{x<-matrix(c(\"1\",\"2\",\"3\",\"4\"),ncol=2);rowMeans(x)}");
    }
}
