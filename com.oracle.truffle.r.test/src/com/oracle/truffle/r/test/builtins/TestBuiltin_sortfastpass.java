package com.oracle.truffle.r.test.builtins;

import com.oracle.truffle.r.test.TestBase;
import org.junit.Test;

public class TestBuiltin_sortfastpass extends TestBase {

    @Test
    public void testsortfastpass() {
        assertEval(".Internal(sorted_fpass(NA, FALSE, FALSE))");
        assertEval(".Internal(sorted_fpass(1, FALSE, FALSE))");
        assertEval(".Internal(sorted_fpass(1.5, FALSE, FALSE))");
        assertEval(".Internal(sorted_fpass(c(), FALSE, FALSE))");
        assertEval(".Internal(sorted_fpass(NULL, FALSE, FALSE))");
        assertEval(".Internal(sorted_fpass(1:5, FALSE, FALSE))");
        assertEval(".Internal(sorted_fpass(1:5, TRUE, FALSE))");
        assertEval(".Internal(sorted_fpass(1:5, FALSE, TRUE))");
        assertEval(".Internal(sorted_fpass(1:5, TRUE, TRUE))");
        assertEval(".Internal(sorted_fpass(1.5:5.5, FALSE, FALSE))");
        assertEval(".Internal(sorted_fpass(seq(1.5,5.5,0.5), FALSE, FALSE))");
        assertEval(".Internal(sorted_fpass(paste('hello',1:10,'.'), FALSE, FALSE))");
        assertEval("argv <- list(1:10, FALSE, FALSE); argv2 <- argv[[1]] + 1; .Internal(sorted_fpass(argv2[[1]], argv2[[2]], argv2[[3]]))");
        assertEval("argv <- list(c(1,2,3,4,5,6,7,8), FALSE, FALSE); argv2 <- argv[[1]] + 1; .Internal(sorted_fpass(argv2[[1]], argv2[[2]], argv2[[3]]))");
    }
}
