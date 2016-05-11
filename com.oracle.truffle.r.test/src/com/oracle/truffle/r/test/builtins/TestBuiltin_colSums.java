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

import java.util.Arrays;

import org.junit.Test;

import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.r.nodes.access.AccessArgumentNode;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.builtin.base.ColSums;
import com.oracle.truffle.r.nodes.builtin.base.ColSumsNodeGen;
import com.oracle.truffle.r.nodes.unary.CastNode.Samples;
import com.oracle.truffle.r.runtime.ArgumentsSignature;
import com.oracle.truffle.r.runtime.RArguments;
import com.oracle.truffle.r.runtime.RBuiltin;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.nodes.RNode;
import com.oracle.truffle.r.test.TestBase;

// Checkstyle: stop line length check
public class TestBuiltin_colSums extends TestBase {

    @Test
    public void testcolSums1() {
        assertEval("argv <- list(structure(c(365, 365, 365, 366, 1, 0), .Dim = c(3L, 2L)), 3, 2, FALSE); .Internal(colSums(argv[[1]], argv[[2]], argv[[3]], argv[[4]]))");
    }

    @Test
    public void testcolSums2() {
        assertEval("argv <- list(structure(c(1L, 0L, 0L, 0L, 2L, 0L, 0L, 0L, 3L), .Dim = c(3L, 3L)), 3, 3, FALSE); .Internal(colSums(argv[[1]], argv[[2]], argv[[3]], argv[[4]]))");
    }

    @Test
    public void testcolSums3() {
        assertEval("argv <- list(structure(c(5, 29, 14, 16, 15, 54, 14, 10, 20, 84, 17, 94, 68, 119, 26, 7), .Dim = c(4L, 4L), .Dimnames = structure(list(Hair = c('Black', 'Brown', 'Red', 'Blond'), Eye = c('Green', 'Hazel', 'Blue', 'Brown')), .Names = c('Hair', 'Eye'))), 4, 4, FALSE); .Internal(colSums(argv[[1]], argv[[2]], argv[[3]], argv[[4]]))");
    }

    @Test
    public void testcolSums4() {
        assertEval("argv <- list(structure(c(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 7, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, NA, 7, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 49, 7, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 21, 0, 0, 7, 0, 0, 0, 0, 0, 0, 0, 0, 3, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, NA, 3, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 21, 3, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 9, 0, 0, 3, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0), .Dim = c(16L, 16L), .Dimnames = list(NULL, NULL)), 16, 16, TRUE); .Internal(colSums(argv[[1]], argv[[2]], argv[[3]], argv[[4]]))");
    }

    @Test
    public void testcolSums5() {
        assertEval("argv <- list(structure(c(FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, TRUE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, NA, TRUE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, TRUE, TRUE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, TRUE, FALSE, FALSE, TRUE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, NA, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, TRUE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, TRUE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE), .Dim = c(16L, 16L), .Dimnames = list(NULL, NULL)), 16, 16, FALSE); .Internal(colSums(argv[[1]], argv[[2]], argv[[3]], argv[[4]]))");
    }

    @Test
    public void testcolSums6() {
        assertEval("argv <- list(structure(numeric(0), .Dim = c(0L, 0L)), 0, 0, FALSE); .Internal(colSums(argv[[1]], argv[[2]], argv[[3]], argv[[4]]))");
    }

    @Test
    public void testcolSums7() {
        assertEval(Ignored.Unknown,
                        "argv <- list(structure(c(-7.5, -6.5, -5.5, -4.5, -3.5, -2.5, -1.5, -0.5, 0.5, 1.5, 2.5, 3.5, 4.5, 5.5, 6.5, 7.5, -421.875, -274.625, -166.375, -91.125, -42.875, -15.625, -3.375, -0.125, 0.125, 3.375, 15.625, 42.875, 91.125, 166.375, 274.625, 421.875, -9187.5, -2866.5, -445.499999999999, -4.5, -283.5, -562.5, -541.5, -220.5, 220.5, 541.5, 562.5, 283.5, 4.49999999999999, 445.5, 2866.5, 9187.5, -139741.875, -4844.38499999995, -10122.255, -28872.045, -28539.315, -15800.625, -4325.535, -178.605, 178.605, 4325.535, 15800.625, 28539.315, 28872.045, 10122.255, 4844.38500000001, 139741.875), .Dim = c(16L, 4L)), 16, 4, FALSE); .Internal(colSums(argv[[1]], argv[[2]], argv[[3]], argv[[4]]))");
    }

    @Test
    public void testcolSums8() {
        assertEval("argv <- list(structure(0:1, .Dim = 1:2, .Dimnames = list('strata(grp)', c('x', 'strata(grp)'))), 1, 2, FALSE); .Internal(colSums(argv[[1]], argv[[2]], argv[[3]], argv[[4]]))");
    }

    @Test
    public void testcolSums9() {
        assertEval("argv <- list(structure(c(0, 0, 0, 0, 0, -1.43884556914512e-134, 0, 0, 0, -7.95468296571581e-252, 1.76099882882167e-260, 0, -9.38724727098368e-323, -0.738228974836154, 0, 0, 0, 0, 0, 0, 0, 0, 0, -6.84657791618065e-123, 0, 0, 0, 0, 0, 0, 0, 0, 0, -1.05931985100232e-174, 0, -3.41789378681991e-150, 0, 0, 0, 0, -1.07225492686949e-10, 0, 1.65068934474523e-67, 0, -6.49830035279282e-307, 0, 5.83184963977238e-90, 0, -9.81722610183938e-287, 6.25336419454196e-54, 0, 0, 0, -1.72840591500382e-274, 1.22894687952101e-13, 0.660132850077566, 0, 0, 7.79918925397516e-200, -2.73162827952857e-178, 1.32195942051179e-41, 0, 0, 0, 0, 2.036057023761e-45, -3.40425060445074e-186, 1.59974269220388e-26, 0, 6.67054294775317e-124, 0.158503117506202, 0, 0, 0, 0, 0, 0, 3.42455724859116e-97, 0, 0, -2.70246891320217e-272, 0, 0, -3.50562438899045e-06, 0, 0, 1.35101732326608e-274, 0, 0, 0, 0, 0, 0, 0, 7.24580295957621e-65, 0, -3.54887341172294e-149, 0, 0, 0, 0, 0, 0, 0, 0, 1.77584594753563e-133, 0, 0, 0, 2.88385135688311e-250, 1.44299633616158e-259, 0, 1.56124744085834e-321, 1.63995835868977, 0, 0, 0, 0, 0, 0, 0, 0, 0, 2.01050064173383e-122, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1.64868196850938e-172, 0, 6.28699823828692e-149, 0, 0, 0, 0, 5.0552295590188e-09, 0, 2.30420733561404e-66, 0, 7.0823279075443e-306, 0, 2.05009901740696e-88, 0, 7.41800724282869e-285, 7.18347043784483e-53, 0, 0, 0, 1.04251223075649e-273, 9.75816316577433e-13, 4.29519957592147, 0, 0, 1.33541454912682e-198, 2.34606233784019e-176, 8.38236726536896e-41, 0, 0, 0, 0, 1.35710537434521e-43, 1.15710503176511e-185, 1.25601735272233e-25, 0, 4.46811655846376e-123, 4.4196641795634, 0, 0, 0, 0, 0, 0, 3.74179015251531e-93, 0, 0, 3.62662047836582e-271, 0, 0, 1.26220330674453e-05, 0, 0, 1.72715562657338e-273, 0, 0, 0, 0, 0, 0, 0, 5.46372806810809e-64, 0, 2.47081972486962e-148, 0, 0, 0), .Dim = c(100L, 2L)), 100, 2, FALSE); .Internal(colSums(argv[[1]], argv[[2]], argv[[3]], argv[[4]]))");
    }

    @Test
    public void testColSums() {
        assertEval("{ m <- matrix(1:6, nrow=2) ; colSums(na.rm = FALSE, x = m) }");
        assertEval("{ m <- matrix(c(1,2,3,4,5,6), nrow=2) ; colSums(m) }");
        assertEval("{ m <- matrix(c(NA,2,3,4,NA,6), nrow=2) ; colSums(m) }");
        assertEval("{ m <- matrix(c(NA,2,3,4,NA,6), nrow=2) ; colSums(na.rm = TRUE, m) }");
        assertEval("{ colSums(matrix(as.complex(1:6), nrow=2)) }");
        assertEval("{ colSums(matrix((1:6)*(1+1i), nrow=2)) }");
        assertEval("{ o <- outer(1:3, 1:4, \"<\") ; colSums(o) }");

        // colSums on matrix drop dimension
        assertEval("{ a = colSums(matrix(1:12,3,4)); dim(a) }");

        // colSums on matrix have correct length
        assertEval("{ a = colSums(matrix(1:12,3,4)); length(a) }");

        // colSums on matrix have correct values
        assertEval("{ colSums(matrix(1:12,3,4)) }");

        // colSums on array have correct dimension
        assertEval("{ a = colSums(array(1:24,c(2,3,4))); d = dim(a); c(d[1],d[2]) }");

        // colSums on array have correct length
        assertEval("{ a = colSums(array(1:24,c(2,3,4))); length(a) }");

        // colSums on array have correct values
        assertEval("{ a = colSums(array(1:24,c(2,3,4))); c(a[1,1],a[2,2],a[3,3],a[3,4]) }");
    }

    class RBuiltinRootNode extends RootNode {

        @Child private RBuiltinNode builtinNode;

        RBuiltinRootNode(RBuiltinNode builtinNode) {
            super(TruffleLanguage.class, null, null);
            this.builtinNode = builtinNode;
        }

        @Override
        public Object execute(VirtualFrame frame) {
            return builtinNode.execute(frame);
        }
    }

    @Test
    public void autoTestColSums() {
        RBuiltin annotation = ColSums.class.getAnnotation(RBuiltin.class);
        String[] parameterNames = annotation.parameterNames();
        parameterNames = Arrays.stream(parameterNames).map(n -> n.isEmpty() ? null : n).toArray(String[]::new);
        ArgumentsSignature signature = ArgumentsSignature.get(parameterNames);

        int total = signature.getLength();
        RNode[] args = new RNode[total];
        for (int i = 0; i < total; i++) {
            args[i] = AccessArgumentNode.create(i);
        }
        ColSums builtinNode = ColSumsNodeGen.create(args.clone());

        Samples<?> s0 = builtinNode.getCasts()[0].collectSamples();
        System.out.println("Samples:\n" + s0);
        Samples<?> s1 = builtinNode.getCasts()[1].collectSamples();
        System.out.println("Samples:\n" + s1);
        Samples<?> s2 = builtinNode.getCasts()[2].collectSamples();
        System.out.println("Samples:\n" + s2);
        Samples<?> s3 = builtinNode.getCasts()[3].collectSamples();
        System.out.println("Samples:\n" + s3);

        RootCallTarget builtinNodeCallTarget = Truffle.getRuntime().createCallTarget(new RBuiltinRootNode(builtinNode));
        builtinNodeCallTarget.call(RArguments.createUnitialized(1.0, 3, 1, 1));
    }

}
