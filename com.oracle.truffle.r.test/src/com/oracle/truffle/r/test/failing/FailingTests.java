// DO NOT EDIT, update using 'mx rignoredtests'
// This contains a copy of the @Ignore tests one micro-test per method
package com.oracle.truffle.r.test.failing;

import org.junit.Ignore;

import com.oracle.truffle.r.test.*;

//Checkstyle: stop
public class FailingTests extends TestBase {
    @Ignore
    public void TestSimpleArithmetic_testIntegerOverflow_e56646664ed3ccebd0b978a474ccae3c() {
        assertEvalWarning("{ x <- 2147483647L ; x + 1L }");
    }

    @Ignore
    public void TestSimpleArithmetic_testIntegerOverflow_9cc3316d11cb57fdb9d71e833e43dcd6() {
        assertEvalWarning("{ x <- 2147483647L ; x * x }");
    }

    @Ignore
    public void TestSimpleArithmetic_testIntegerOverflow_a5d2e40a03d44363ee0bf4afb8a3a70d() {
        assertEvalWarning("{ x <- -2147483647L ; x - 2L }");
    }

    @Ignore
    public void TestSimpleArithmetic_testIntegerOverflow_52bf15e78c97dfea203e3a3a75c0c096() {
        assertEvalWarning("{ x <- -2147483647L ; x - 1L }");
    }

    @Ignore
    public void TestSimpleArithmetic_testIntegerOverflow_4a27f3f0ef1c0e73ea1ae4a599818778() {
        assertEvalWarning("{ 2147483647L + 1:3 }");
    }

    @Ignore
    public void TestSimpleArithmetic_testIntegerOverflow_d17b51eaa8f9d85088d30f7b59888e01() {
        assertEvalWarning("{ 2147483647L + c(1L,2L,3L) }");
    }

    @Ignore
    public void TestSimpleArithmetic_testIntegerOverflow_a208f558c3d55c2d86aa5cfe699b218a() {
        assertEvalWarning("{ 1:3 + 2147483647L }");
    }

    @Ignore
    public void TestSimpleArithmetic_testIntegerOverflow_1444a6f9919138380d32057ddfa36eec() {
        assertEvalWarning("{ c(1L,2L,3L) + 2147483647L }");
    }

    @Ignore
    public void TestSimpleArithmetic_testIntegerOverflow_dd77dbcef3cf523fc2aa46c4c0deaf5c() {
        assertEvalWarning("{ 1:3 + c(2147483647L,2147483647L,2147483647L) }");
    }

    @Ignore
    public void TestSimpleArithmetic_testIntegerOverflow_72a5d4dd67ed5a21396516c0968edf6e() {
        assertEvalWarning("{ c(2147483647L,2147483647L,2147483647L) + 1:3 }");
    }

    @Ignore
    public void TestSimpleArithmetic_testIntegerOverflow_6fd7e6825d4c56f715061fbb7124628a() {
        assertEvalWarning("{ c(1L,2L,3L) + c(2147483647L,2147483647L,2147483647L) }");
    }

    @Ignore
    public void TestSimpleArithmetic_testIntegerOverflow_6b9734a08caf45fad14bde9d7b10a97c() {
        assertEvalWarning("{ c(2147483647L,2147483647L,2147483647L) + c(1L,2L,3L) }");
    }

    @Ignore
    public void TestSimpleArithmetic_testIntegerOverflow_8b60212b3b68acfddf00f22ea65883db() {
        assertEvalWarning("{ 1:4 + c(2147483647L,2147483647L) }");
    }

    @Ignore
    public void TestSimpleArithmetic_testIntegerOverflow_ffe1faa265bec1af2b8c1f1c4d9fc343() {
        assertEvalWarning("{ c(2147483647L,2147483647L) + 1:4 }");
    }

    @Ignore
    public void TestSimpleArithmetic_testIntegerOverflow_8894beb2d0cbaf7303c2efa930d6684b() {
        assertEvalWarning("{ c(1L,2L,3L,4L) + c(2147483647L,2147483647L) }");
    }

    @Ignore
    public void TestSimpleArithmetic_testIntegerOverflow_68d4c3db613629f473aa7128bff2c5a8() {
        assertEvalWarning("{ c(2147483647L,2147483647L) + c(1L,2L,3L,4L) }");
    }

    @Ignore
    public void TestSimpleArithmetic_testScalarsRealIgnore_706f889093f4841d307059b60cb81c13() {
        assertEval("{ 1000000000*100000000000 }");
    }

    @Ignore
    public void TestSimpleArithmetic_testScalarsRealIgnore_85c78d2d490e3d28bc72254fbec91949() {
        assertEval("{ 1000000000L*1000000000 }");
    }

    @Ignore
    public void TestSimpleArithmetic_testScalarsRealIgnore_846b21508ff7d445e01b13f78cc32dba() {
        assertEval("{ 1000000000L*1000000000L }");
    }

    @Ignore
    public void TestSimpleAssignment_testAssign_283c9530c525c82a5e49b43433fdced9() {
        assertEvalNoOutput("{ a<-1 }");
    }

    @Ignore
    public void TestSimpleAssignment_testAssign_e7172f616e0946e808cf73fe8c5ba64b() {
        assertEvalNoOutput("{ a<-FALSE ; b<-a }");
    }

    @Ignore
    public void TestSimpleAssignment_testAssign_b21774dbc3b823d809cfaf4ee17527de() {
        assertEvalNoOutput("{ x = if (FALSE) 1 }");
    }

    @Ignore
    public void TestSimpleAssignment_testAssignFunctionLookup1_af5ff7016009f392e234cca594160ea3() {
        assertEval("f <- function(b) { c <- 42; c(1,1); }; f(0); f(1)");
    }

    @Ignore
    public void TestSimpleAssignment_testAssignFunctionLookup1_2346e3897adeba694188ec2ab21c1070() {
        assertEval("f <- function(b) { if (b) c <- 42; c(1,1); }; f(0); f(1)");
    }

    @Ignore
    public void TestSimpleAssignment_testAssignPoly1_66cf51299f299d2cd8bfa5c599824623() {
        assertEval("test <- function(b) { if (b) f <- function() { 42 }; g <- function() { if (!b) f <- function() { 43 }; f() }; g() }; c(test(FALSE), test(TRUE))");
    }

    @Ignore
    public void TestSimpleAssignment_testAssignShadowBuiltin1_f2d5da3c45411e2c079849343ea84875() {
        assertEval("f <- function(b) { c <- function(x,y) 42; c(1,1); }; f(0); f(1)");
    }

    @Ignore
    public void TestSimpleAssignment_testAssignShadowBuiltin1_b9c9722029283827d0a91f19bac45918() {
        assertEval("f <- function(b) { if (b) c <- function(x,y) 42; c(1,1); }; f(0); f(1)");
    }

    @Ignore
    public void TestSimpleAssignment_testDynamic_d66d832275659532e17a035d9554c549() {
        assertEval("{ l <- quote(x <- 1) ; f <- function() { eval(l) } ; x <- 10 ; f() ; x }");
    }

    @Ignore
    public void TestSimpleAssignment_testDynamic_e224a6d79056c025f24a2a9d1a73d019() {
        assertEval("{ l <- quote(x <- 1) ; f <- function() { eval(l) ; x <<- 10 ; get(\"x\") } ; f() }");
    }

    @Ignore
    public void TestSimpleAssignment_testSuperAssignIgnore_aa206594ebb10eb912cbc08e7c82e4e3() {
        assertEval("{ a <- c(1,2,3) ; f <- function() { a[2] <- 4 } ; list(f(),a) }");
    }

    @Ignore
    public void TestSimpleAssignment_testSuperAssignIgnore_cfdf1ec04d27a60bdfe3a1bea92933e6() {
        assertEvalNoOutput("{ x <<- 1 }");
    }

    @Ignore
    public void TestSimpleAssignment_testSuperAssignIgnore_437eb5c1cc18125d4b5896cf3d2b5365() {
        assertEvalNoOutput("{ x <<- 1 ; x }");
    }

    @Ignore
    public void TestSimpleAttributes_testBuiltinPropagationIgnore_df9b3724960b222fffd20b6a1ef94ed5() {
        assertEval("{ m <- matrix(c(1,1,1,1), nrow=2) ; attr(m,\"a\") <- 1 ;  r <- eigen(m) ; r$vectors <- round(r$vectors, digits=5) ; r  }");
    }

    @Ignore
    public void TestSimpleBuiltins_testAbsIgnore_261d7e173c1caffcac87b3030f93a81c() {
        assertEval("{ abs(c(0/0,1i)) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testAbsIgnore_c0cc055b696d0196df8961748dac97a4() {
        assertEval("{ exp(-abs((0+1i)/(0+0i))) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testAbsIgnore_0ab2d0f2d7030b273cd0e45daf435b57() {
        assertEval("{ abs(1:3) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testAbsIgnore_eb0e93fa1cbdf12456e6b7c849b0f670() {
        assertEval("{ abs(-1:-3) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testAbsIgnore_f9ca5d8354239b619dbd0b67d729e220() {
        assertEvalError("{ abs(NULL) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testAllIgnore_1cad38b2b58506e86b3faf337282af34() {
        assertEval("{ all(TRUE, TRUE, NA,  na.rm=TRUE) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testAllIgnore_2b56cf245fc3518ca8c3daa8c70c7441() {
        assertEval("{ all(1) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testAllIgnore_91a6f9d5d41dc450755861f6e318c869() {
        assertEval("{ all(0) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testAllIgnore_b5c51ccc3f58394e01320c7b59736d24() {
        assertEval("{ all(TRUE,c(TRUE,TRUE),1) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testAllIgnore_e11f439dffd428996b1d680fede13a41() {
        assertEval("{ all(TRUE,c(TRUE,TRUE),1,0) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testAnyDuplicatedIgnore_dcc2ba95aa8608d62368b2c9886bb0ba() {
        assertEval("{ anyDuplicated(c(1L, 2L, 1L, 1L, 3L, 2L), incomparables = \"cat\") }");
    }

    @Ignore
    public void TestSimpleBuiltins_testAnyDuplicatedIgnore_58cdce8ea781c0cdf349b42069b16727() {
        assertEval("{ anyDuplicated(c(1,2,3,2), incomparables = c(2+6i)) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testAnyIgnore_a5514afb3c27ad5fad71696cb1db96a9() {
        assertEval("{ any(FALSE, NA,  na.rm=TRUE) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testAnyIgnore_91043dd22cb7d3aab79a22019a52ea3f() {
        assertEvalWarning("{ any(1) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testAnyIgnore_b0a96f7fb16a6bf50fba85a11a8da034() {
        assertEvalWarning("{ any(0) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testAsCharacterIgnore_c803fc23a52fdc9950e5603f439b132f() {
        assertEval("{ as.character(list(1,2,3)) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testAsCharacterIgnore_03efd474c6b2ac63cfa1f6d497c9cf80() {
        assertEval("{ as.character(list(c(\"hello\", \"hi\"))) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testAsCharacterIgnore_2f45a0dc44e788e9eaea83ed3fc488ad() {
        assertEval("{ as.character(list(list(c(\"hello\", \"hi\")))) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testAsCharacterIgnore_f0e99f0b6485990390645c5a6f6b13c3() {
        assertEval("{ as.character(list(c(2L, 3L))) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testAsCharacterIgnore_2ef5afd90d532194c1e0775974b91525() {
        assertEval("{ as.character(list(c(2L, 3L, 5L))) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testAsComplexIgnore_a234b535de865dc1374d86dc2a304cb0() {
        assertEval("{ as.complex(\"1e10+5i\") }");
    }

    @Ignore
    public void TestSimpleBuiltins_testAsComplexIgnore_f959c2432167aba7516572589c2a297b() {
        assertEval("{ as.complex(\"-.1e10+5i\") }");
    }

    @Ignore
    public void TestSimpleBuiltins_testAsComplexIgnore_4fff4d142baeef1724a393317f422bfe() {
        assertEval("{ as.complex(\"1e-2+3i\") }");
    }

    @Ignore
    public void TestSimpleBuiltins_testAsComplexIgnore_ca81945b0033de54e397d1df1719f69a() {
        assertEval("{ as.complex(\"+.1e+2-3i\") }");
    }

    @Ignore
    public void TestSimpleBuiltins_testCall_7d3147e26292301cfabf8939c17af430() {
        assertEval("{ f <- function(a, b) { a + b } ; l <- call(\"f\", 2, 3) ; eval(l) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testCall_ac5601b7f27d60cead4d93b849fd38ca() {
        assertEval("{ f <- function(a, b) { a + b } ; x <- 1 ; y <- 2 ; l <- call(\"f\", x, y) ; x <- 10 ; eval(l) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testCall_7c2048e48cfa4b8a27e274503d2d28f2() {
        assertEval("{ anyDuplicated(c(1L, 2L, 3L, 4L, 2L, 3L), fromLast = TRUE) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testCatIgnore_01ac467ff40598b5a055378fc7882537() {
        assertEvalNoNL("{ cat(\"hi\",NULL,\"hello\",sep=\"-\") }");
    }

    @Ignore
    public void TestSimpleBuiltins_testCatIgnore_4949a7df83738286ea025e86159c9cdc() {
        assertEvalNoNL("{ cat(\"hi\",integer(0),\"hello\",sep=\"-\") }");
    }

    @Ignore
    public void TestSimpleBuiltins_testCbindIgnore_c292a9a2047519d8fd24923adebb0ad2() {
        assertEval("{ cbind(list(1,2), TRUE, \"a\") }");
    }

    @Ignore
    public void TestSimpleBuiltins_testCbindIgnore_849d2f7200b6d113f749abbc67d41a7d() {
        assertEval("{ cbind(1:3,1:2) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testCbindIgnore_24a51282927ba915c3ebc8717b71c58a() {
        assertEval("{ cbind(2,3, complex(3,3,2));}");
    }

    @Ignore
    public void TestSimpleBuiltins_testCbindIgnore_ad3549732fce5b2ee1cd8a4b9996d797() {
        assertEval("{ cbind(2,3, c(1,1,1))");
    }

    @Ignore
    public void TestSimpleBuiltins_testCbindIgnore_eac42e3620663dabb16389ef366ddb5e() {
        assertEval("{ cbind(2.1:10,32.2)");
    }

    @Ignore
    public void TestSimpleBuiltins_testCharUtilsIgnore_864e89c688384c8cc67d1b4676ff314d() {
        assertEval("{ tolower(1E100) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testCharUtilsIgnore_69433b6491feff8204434af6a79f9307() {
        assertEval("{ toupper(1E100) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testCharUtilsIgnore_8f5f00293e9bfb6ac9aab0e3e6c88cf8() {
        assertEval("{ m <- matrix(\"hi\") ; toupper(m) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testCharUtilsIgnore_e5ad5f71aaa8302b8bcddddde53fd68e() {
        assertEval("{ toupper(c(a=\"hi\", \"hello\")) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testCharUtilsIgnore_ec31b79bc63b78f141adde800c2de5ab() {
        assertEval("{ tolower(c(a=\"HI\", \"HELlo\")) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testCharUtilsIgnore_ddbafed30934d43a3a0e4862fb6bd0db() {
        assertEval("{ tolower(NA) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testCharUtilsIgnore_73009820a93846c10cb6c65b68e5b7fa() {
        assertEval("{ toupper(NA) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testChol_0c871276d1ef0a12733f4763eca31305() {
        assertEval("{ chol(1) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testChol_56c7d9d1a9d02d3730de6ef5e4b085b8() {
        assertEval("{ round( chol(10), digits=5) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testChol_7b9b9fe7c5e51dfc97d44dd7ce4cc95a() {
        assertEval("{ m <- matrix(c(5,1,1,3),2) ; round( chol(m), digits=5 ) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testChol_887c0d3033dcb17c875cf7a89313563c() {
        assertEvalError("{ m <- matrix(c(5,-5,-5,3),2,2) ; chol(m) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testColMeansIgnore_1e146b3cdde30114bb9bdd12bbfd4a51() {
        assertEval("{colMeans(matrix(c(NaN,4+5i,2+0i,5+10i),nrow=2,ncol=2), na.rm = TRUE)}");
    }

    @Ignore
    public void TestSimpleBuiltins_testColumnsRowsStatIgnore_8ca9d6c7f776a8e3441d264e1da328a6() {
        assertEval("{ m <- matrix(1:6, nrow=2) ; colMeans(m) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testColumnsRowsStatIgnore_42f4dcf92af03ea106b9ee137d80a60b() {
        assertEval("{ m <- matrix(1:6, nrow=2) ; rowMeans(x = m, na.rm = TRUE) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testColumnsRowsStatIgnore_f6e099a48acc0dc03f8df25fddeaa2ac() {
        assertEval("{ m <- matrix(1:6, nrow=2) ; rowSums(x = m) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testColumnsRowsStatIgnore_5a6a0c0306ea58dc330442a0ee35ac57() {
        assertEval("{ m <- matrix(c(1,2,3,4,5,6), nrow=2) ; colMeans(m) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testColumnsRowsStatIgnore_9589a0af44218e0e9ffcf6a4ddb95ee3() {
        assertEval("{ m <- matrix(c(1,2,3,4,5,6), nrow=2) ; rowMeans(m) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testColumnsRowsStatIgnore_27fd0c0df27edacc427f026c6f82c11e() {
        assertEval("{ m <- matrix(c(1,2,3,4,5,6), nrow=2) ; colSums(m) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testColumnsRowsStatIgnore_7a7b23e72604196aca2933d8326855f8() {
        assertEval("{ m <- matrix(c(1,2,3,4,5,6), nrow=2) ; rowSums(m) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testColumnsRowsStatIgnore_6e3237bc98188617dc175a91480d9f8a() {
        assertEval("{ m <- matrix(c(NA,2,3,4,NA,6), nrow=2) ; rowSums(m) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testColumnsRowsStatIgnore_97ed75fc969a4330d9764e77572c5057() {
        assertEval("{ m <- matrix(c(NA,2,3,4,NA,6), nrow=2) ; rowSums(m, na.rm = TRUE) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testColumnsRowsStatIgnore_cf3ce4597e64537f5ed0c8e1c5bc2649() {
        assertEval("{ m <- matrix(c(NA,2,3,4,NA,6), nrow=2) ; rowMeans(m, na.rm = TRUE) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testColumnsRowsStatIgnore_9f1ea14f9baa5e49245d4f90538c3b1d() {
        assertEval("{ m <- matrix(c(NA,2,3,4,NA,6), nrow=2) ; colSums(m) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testColumnsRowsStatIgnore_cd2c57bffaff581a9e8b2107b3148b58() {
        assertEval("{ m <- matrix(c(NA,2,3,4,NA,6), nrow=2) ; colSums(na.rm = TRUE, m) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testColumnsRowsStatIgnore_2b787e31b5232423c06c52f73e5df1c6() {
        assertEval("{ m <- matrix(c(NA,2,3,4,NA,6), nrow=2) ; colMeans(m) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testColumnsRowsStatIgnore_2f427a36497e0dc01a2611f5aa23ae7b() {
        assertEval("{ m <- matrix(c(NA,2,3,4,NA,6), nrow=2) ; colMeans(m, na.rm = TRUE) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testColumnsRowsStatIgnore_245eed182ce6e800317cc04ea2db8076() {
        assertEval("{ colMeans(matrix(as.complex(1:6), nrow=2)) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testColumnsRowsStatIgnore_c9f4a9f49a298a830f36751055417164() {
        assertEval("{ colMeans(matrix((1:6)*(1+1i), nrow=2)) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testColumnsRowsStatIgnore_e27a7ec7efc72290832ff500ab7fdbbd() {
        assertEval("{ rowSums(matrix(as.complex(1:6), nrow=2)) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testColumnsRowsStatIgnore_0222b72afb3af3984c867d68ee9c340f() {
        assertEval("{ rowSums(matrix((1:6)*(1+1i), nrow=2)) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testColumnsRowsStatIgnore_8214d8fc710c76862499f1c9b1a31121() {
        assertEval("{ rowMeans(matrix(as.complex(1:6), nrow=2)) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testColumnsRowsStatIgnore_15d9ab20e0f0df878fa345ad14ce4245() {
        assertEval("{ rowMeans(matrix((1:6)*(1+1i), nrow=2)) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testColumnsRowsStatIgnore_cfbfed37d84d4a557a3944e4001685a4() {
        assertEval("{ colSums(matrix(as.complex(1:6), nrow=2)) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testColumnsRowsStatIgnore_b62078f5eeef7282e5eff2a59a8d8cd8() {
        assertEval("{ colSums(matrix((1:6)*(1+1i), nrow=2)) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testColumnsRowsStatIgnore_ef0e18bdd086f0183fcc8fae77cc4d1a() {
        assertEval("{ o <- outer(1:3, 1:4, \"<\") ; colSums(o) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testCombineBroken_d365e1ffe5f8c886f6d1911c69b3af00() {
        assertEval("{ c(1i,0/0) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testComplexIgnore_6c296b051839b1865e7b24f04e0f89d5() {
        assertEval("{ x <- 1:2 ; attr(x,\"my\") <- 2 ; Im(x) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testComplexIgnore_ebb6e003d79ccc9419c0bbc4c4601d12() {
        assertEval("{ x <- c(1+2i,3-4i) ; attr(x,\"my\") <- 2 ; Im(x) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testComplexIgnore_0a640363497df7e0fff841acd48b8679() {
        assertEval("{ x <- 1:2 ; attr(x,\"my\") <- 2 ; Re(x) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testComplexIgnore_cd6019c801f0cbbb3b00ecbde91958c5() {
        assertEval("{ x <- c(1+2i,3-4i) ; attr(x,\"my\") <- 2 ; Re(x) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testCorIgnore_13b78c66b0e72ebed23e724262a27546() {
        assertEval("{ round( cor(cbind(c(10,5,4,1), c(2,5,10,5))), digits=5 ) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testCrossprod_7f9549017d66ad3dd1583536fa7183d7() {
        assertEval("{ x <- 1:6 ; crossprod(x) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testCrossprod_1c6fdfbd19321f1f57a6f9260789424a() {
        assertEval("{ x <- 1:2 ; crossprod(t(x)) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testCrossprod_0ceb7477eceaa0684310f07ef6b6865c() {
        assertEval("{ crossprod(1:3, matrix(1:6, ncol=2)) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testCrossprod_57b1bcccff6a1f41d6a0c82a658a3c52() {
        assertEval("{ crossprod(t(1:2), 5) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testCrossprod_2770157f2b02bfda92abe04278a245f8() {
        assertEval("{ crossprod(c(1,NA,2), matrix(1:6, ncol=2)) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testCumulativeMaxIgnore_2f6f91ad8c5d7ca01467c196f33b080e() {
        assertEval("{ cummax(c(1+1i,2-3i,4+5i)) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testCumulativeMaxIgnore_80eddc53556e008ace29ab00e165f768() {
        assertEval("{ cummax(c(1+1i, NA, 2+3i)) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testCumulativeMinIgnore_b5b948bfcb858f80485301067b4a3cb5() {
        assertEval("{ cummin(c(1+1i,2-3i,4+5i)) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testCumulativeMinIgnore_9fe29fb5f8789df82d6740ac7b77830f() {
        assertEval("{ cummin(c(1+1i, NA, 2+3i)) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testCumulativeSumBroken_29fdcc5a5db08a57fa538ba6ea36df62() {
        assertEval("{ cumsum(c(1,2,3,0/0,5)) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testCumulativeSumBroken_c4e74421afc1541ec09c1258dd016111() {
        assertEval("{ cumsum(c(1,0/0,5+1i)) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testCumulativeSumBroken_c798b06052d4528aca37769d38a0f9af() {
        assertEval("{ cumsum(as.raw(1:6)) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testCumulativeSumBroken_24579242149f490e91e8b1b7fc76f4e9() {
        assertEval("{ cumsum(rep(1e308, 3) ) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testCumulativeSumBroken_9e68f6a2cfecca2814fd572d9d3dc519() {
        assertEval("{ cumsum(c(1e308, 1e308, NA, 1, 2)) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testCumulativeSumBroken_e1af5bf2238f58b9e00ba5f815e46a59() {
        assertEval("{ cumsum(c(2000000000L, 2000000000L)) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testCumulativeSumBroken_e41ac0de20a9dba0b5c5c897e46d2ddb() {
        assertEval("{ cumsum(c(-2147483647L, -1L)) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testCumulativeSumBroken_598bb2dd748d2cd878a7312e7a0935c9() {
        assertEval("{ cumsum((1:6)*(1+1i)) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testDateIgnore_dc2d15503c397a52d19f8f822448e08d() {
        assertEval("{date()}");
    }

    @Ignore
    public void TestSimpleBuiltins_testDelayedAssignIgnore_79fb1d399e2b39a496dac5a9749fb873() {
        assertEval("{ h <- new.env(parent=emptyenv()) ; delayedAssign(\"x\", y, h, h) ; assign(\"y\", 2, h) ; get(\"x\", h) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testDelayedAssignIgnore_af327b1b6a16f6b664839a659452d6ff() {
        assertEval("{ h <- new.env(parent=emptyenv()) ; assign(\"x\", 1, h) ; delayedAssign(\"x\", y, h, h) ; assign(\"y\", 2, h) ; get(\"x\", h) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testDelayedAssignIgnore_b0a8cc01cf8e5fc94f5e4084097107ad() {
        assertEval("{ f <- function(...) { delayedAssign(\"x\", ..1) ; y <<- x } ; f(10) ; y }");
    }

    @Ignore
    public void TestSimpleBuiltins_testDelayedAssignIgnore_2650fc25df477fca9f65b4ae42030ddc() {
        assertEval("{ f <- function() { delayedAssign(\"x\", 3); delayedAssign(\"x\", 2); x } ; f() }");
    }

    @Ignore
    public void TestSimpleBuiltins_testDelayedAssignIgnore_8c59e6c2915b2b15a962ae541292c0db() {
        assertEval("{ f <- function() { x <- 4 ; delayedAssign(\"x\", y); y <- 10; x  } ; f() }");
    }

    @Ignore
    public void TestSimpleBuiltins_testDelayedAssignIgnore_83064c7d347757ad66074441e8cfc90e() {
        assertEvalError("{ f <- function() { delayedAssign(\"x\", y); delayedAssign(\"y\", x) ; x } ; f() }");
    }

    @Ignore
    public void TestSimpleBuiltins_testDeparseIgnore_1dc435ef27d6d10df26ec2271cb67316() {
        assertEval("{ f <- function(x) { deparse(substitute(x)) } ; f(a + b * (c - d)) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testDet_0119e3eeb33ab4a029ba7826ddc06536() {
        assertEval("{ det(matrix(c(1,2,4,5),nrow=2)) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testDet_5e1459250de6d93f03e5e5eaaccd1afc() {
        assertEval("{ det(matrix(c(1,-3,4,-5),nrow=2)) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testDet_9c562345cefeea163f138973f9d0f2a1() {
        assertEval("{ det(matrix(c(1,0,4,NA),nrow=2)) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testDiagnostics_f20f62c82be750e78cc720a71705d1f4() {
        assertEvalError("{ f <- function() { stop(\"hello\",\"world\") } ; f() }");
    }

    @Ignore
    public void TestSimpleBuiltins_testEigen_5f76ba83937083ccca6e7d8fca5c8d43() {
        assertEval("{ r <- eigen(matrix(rep(1,4), nrow=2), only.values=FALSE) ; round( r$vectors, digits=5 ) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testEigen_3a0973319dd9e19b5d218165db6c191e() {
        assertEval("{ r <- eigen(matrix(rep(1,4), nrow=2), only.values=FALSE) ; round( r$values, digits=5 ) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testEigen_115e6b72c47df2b5d5700b273b70c533() {
        assertEval("{ eigen(10, only.values=FALSE) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testEigen_0449327e2827cfc14c352f69bb2d6863() {
        assertEval("{ r <- eigen(matrix(c(1,2,2,3), nrow=2), only.values=FALSE); round( r$vectors, digits=5 ) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testEigen_1807792fd12f35acb23589be46cf6b57() {
        assertEval("{ r <- eigen(matrix(c(1,2,2,3), nrow=2), only.values=FALSE); round( r$values, digits=5 ) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testEigen_a7d1b10ab33353c276caf5c71013af50() {
        assertEval("{ r <- eigen(matrix(c(1,2,3,4), nrow=2), only.values=FALSE); round( r$vectors, digits=5 ) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testEigen_83d97801023043df2de8fa2831ea80e5() {
        assertEval("{ r <- eigen(matrix(c(1,2,3,4), nrow=2), only.values=FALSE); round( r$values, digits=5 ) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testEigen_e1ef8addd5b3fea26321432b42bf54e5() {
        assertEval("{ r <- eigen(matrix(c(3,-2,4,-1), nrow=2), only.values=FALSE); round( r$vectors, digits=5 ) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testEigen_19ec900b70611f935fb95e980df000f3() {
        assertEval("{ r <- eigen(matrix(c(3,-2,4,-1), nrow=2), only.values=FALSE); round( r$values, digits=5 ) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testEnvironmentIgnore_14d25c1c38347070f388d2f433245dab() {
        assertEvalError("{ as.environment(as.environment) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testEvalIgnore_a2bb4f39d740a0564a45a2fa5a7f8259() {
        assertEval("{ eval({ xx <- pi; xx^2}) ; xx }");
    }

    @Ignore
    public void TestSimpleBuiltins_testExp_604f92586ff1b698d6b752cce3248f1e() {
        assertEval("{ round( exp(c(1+1i,-2-3i)), digits=5 ) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testExp_615369efc779cc2d92f0f1998762dc35() {
        assertEval("{ round( exp(1+2i), digits=5 ) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testFileListing_9646bfd3fb553824f1f54cc5d04b8219() {
        assertEval("{ list.files(\"test/r/simple/data/tree1\") }");
    }

    @Ignore
    public void TestSimpleBuiltins_testFileListing_c3407928ac3dcbd4ed94ca586c8fa3bd() {
        assertEval("{ list.files(\"test/r/simple/data/tree1\", recursive=TRUE) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testFileListing_b71b321a5f8b4e1665e1e8c55dfc00f5() {
        assertEval("{ list.files(\"test/r/simple/data/tree1\", recursive=TRUE, pattern=\".*dummy.*\") }");
    }

    @Ignore
    public void TestSimpleBuiltins_testFileListing_b36b504faedcd110cf3480d0627a4990() {
        assertEval("{ list.files(\"test/r/simple/data/tree1\", recursive=TRUE, pattern=\"dummy\") }");
    }

    @Ignore
    public void TestSimpleBuiltins_testFileListing_de580ef8e4242ba05e2ab96a9e21d936() {
        assertEval("{ list.files(\"test/r/simple/data/tree1\", pattern=\"*.tx\") }");
    }

    @Ignore
    public void TestSimpleBuiltins_testGetClassIgnore_04e1bbb35c3306f6feb801b5cce80b88() {
        assertEval("{x<-seq(1,10);class(x)}");
    }

    @Ignore
    public void TestSimpleBuiltins_testGetIgnore_64afee6cadb778dda13b25a2f3f9ecef() {
        assertEval("{y<-function(){y<-2;get(\"y\",mode=\"closure\",inherits=FALSE);};y();}");
    }

    @Ignore
    public void TestSimpleBuiltins_testIdentical_c46eaf60fda944bdf1391b5fe9af0427() {
        assertEval("{ identical(1,1) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testIdentical_53fdb3573317b847b951f1bb6b1d8ea0() {
        assertEval("{ identical(1L,1) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testIdentical_959b063ad8742ea07448fc45ba8f9851() {
        assertEval("{ identical(1:3, c(1L,2L,3L)) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testIdentical_0538458b54dce047351d6fe4728461d7() {
        assertEval("{ identical(0/0,1[2]) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testIdentical_e9828bc94f4f46dc68f975a39942f654() {
        assertEval("{ identical(list(1, list(2)), list(list(1), 1)) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testIdentical_1bd4e6954bc44911ff58137eb71e3c2c() {
        assertEval("{ identical(list(1, list(2)), list(1, list(2))) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testIdentical_3dd8c26fc61d38a1308e5199dfaeb876() {
        assertEval("{ x <- 1 ; attr(x, \"my\") <- 10; identical(x, 1) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testIdentical_329bfbf3d8ef2c8dccff787144ebe4c5() {
        assertEval("{ x <- 1 ; attr(x, \"my\") <- 10; y <- 1 ; attr(y, \"my\") <- 10 ; identical(x,y) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testIdentical_905c81c2be1d34a4bba411f19c71b4ae() {
        assertEval("{ x <- 1 ; attr(x, \"my\") <- 10; y <- 1 ; attr(y, \"my\") <- 11 ; identical(x,y) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testIdentical_6c7bc412e522d929c5a5f2071ca26ec9() {
        assertEval("{ x <- 1 ; attr(x, \"hello\") <- 2 ; attr(x, \"my\") <- 10;  attr(x, \"hello\") <- NULL ; y <- 1 ; attr(y, \"my\") <- 10 ; identical(x,y) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testInheritsIgnore_d0dc6389c924878311546ba61d753a22() {
        assertEval("{x <- 10;class(x) <- c(\"a\", \"b\");inherits(x, 2, c(TRUE)) ;}");
    }

    @Ignore
    public void TestSimpleBuiltins_testInheritsIgnore_89e7444d88aeaed136ad761742bfd5e4() {
        assertEval("{x <- 10;class(x) <- c(\"a\", \"b\");inherits(x, \"a\", 1) ;}");
    }

    @Ignore
    public void TestSimpleBuiltins_testInvisibleIgnore_d73dc3df8036b77c171c3b1e3e6abe2b() {
        assertEval("{ f <- function(x, r) { if (x) invisible(r) else r }; f(TRUE, 1) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testInvocationIgnore_6024770f1412c264dd004f2fa8bc6fbf() {
        assertEval("{ round( rnorm(1,), digits = 5 ) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testInvocationIgnore_dd3e0cc9f1a660be34f8d72900973743() {
        assertEval("{ f <- function(...) { l <- list(...) ; l[[1]] <- 10; ..1 } ; f(11,12,13) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testInvocationIgnore_52f3eb5641c6781b78df1fadd9026fd4() {
        assertEval("{ g <- function(...) { length(list(...)) } ; f <- function(...) { g(..., ...) } ; f(z = 1, g = 31) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testInvocationIgnore_1258dbcba01e5d764684be0e540347c1() {
        assertEval("{ g <- function(...) { `-`(...) } ; g(1,2) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testInvocationIgnore_a3771a811b477ac7315bd35bcf519731() {
        assertEval("{ f <- function(...) { list(a=1,...) } ; f(b=2,3) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testInvocationIgnore_a8ec33ddd003e2f4f13be2ec0d07b6d3() {
        assertEval("{ f <- function(...) { substitute(...) } ; f(x + z) } ");
    }

    @Ignore
    public void TestSimpleBuiltins_testInvocationIgnore_c0649b33488ef441844f88cbeb22d470() {
        assertEval("{ p <- function(prefix, ...) { cat(prefix, ..., \"\n\") } ; p(\"INFO\", \"msg:\", \"Hello\", 42) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testInvocationIgnore_d9a5cb384d79347b34d55b8293316a42() {
        assertEval("{ f <- function(...) { g <- function() { list(...)$a } ; g() } ; f(a=1) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testInvocationIgnore_aa735a388a824b851e914305a0ee78ec() {
        assertEval("{ f <- function(...) { args <- list(...) ; args$name } ; f(name = 42) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testInvocationIgnore_e7dd2cd652f2b8c1a31a90832603d4c5() {
        assertEvalError("{ matrix(x=1) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testInvocationIgnore_53d1bf6a3bf98883a70a360da169055c() {
        assertEvalError("{ max(1,2,) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testIsFactorIgnore_e43e62dca9f8a682efdd7d472154123e() {
        assertEval("{is.factor(1)}");
    }

    @Ignore
    public void TestSimpleBuiltins_testIsFactorIgnore_9b6189d7740f2b58ed5ac90834facc44() {
        assertEval("{x<-1;class(x)<-\"factor\";is.factor(x)}");
    }

    @Ignore
    public void TestSimpleBuiltins_testIsFactorIgnore_f50a4cd1b0417c209953249fed637957() {
        assertEval("{is.factor(c)}");
    }

    @Ignore
    public void TestSimpleBuiltins_testLapplyIgnore_bb1b1b8299159a83c87fe6dc760e5b8b() {
        assertEval("{ lapply(1:3, function(x,y,z) { as.character(x*y+z) }, 2,7) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testLogIgnore_052ed04e88403025c80c488866a0f346() {
        assertEval("{ m <- matrix(1:4, nrow=2) ; round( log10(m), digits=5 )  }");
    }

    @Ignore
    public void TestSimpleBuiltins_testLogIgnore_6568d70e4d076fc4b14b58158162a0ea() {
        assertEval("{ x <- c(a=1, b=10) ; round( c(log(x), log10(x), log2(x)), digits=5 ) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testLookupIgnore_7e5d40be5a03aac06880b44eefa7d94b() {
        assertEval("{ f <- function(z) { exists(\"z\") } ; f(a) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testLookupIgnore_7623faf4c356905dacd205a8b10eac15() {
        assertEval("{ g <- function() { assign(\"myfunc\", function(i) { sum(i) });  f <- function() { lapply(2, \"myfunc\") } ; f() } ; g() }");
    }

    @Ignore
    public void TestSimpleBuiltins_testLookupIgnore_2c371d6a6d4b74a871402788dbf16cf8() {
        assertEval("{ myfunc <- function(i) { sum(i) } ; g <- function() { assign(\"z\", 1);  f <- function() { lapply(2, \"myfunc\") } ; f() } ; g() }");
    }

    @Ignore
    public void TestSimpleBuiltins_testLookupIgnore_fc0e56627d1b08ab2d6c38875a68a1f0() {
        assertEval("{ g <- function() { f <- function() { assign(\"myfunc\", function(i) { sum(i) }); lapply(2, \"myfunc\") } ; f() } ; g() }");
    }

    @Ignore
    public void TestSimpleBuiltins_testLookupIgnore_2deae78feff592acd7d61159c8e39ea7() {
        assertEval("{ g <- function() { myfunc <- function(i) { i+i } ; f <- function() { lapply(2, \"myfunc\") } ; f() } ; g() }");
    }

    @Ignore
    public void TestSimpleBuiltins_testLowerTriangular_d0f253a7c6e1e06bb5cf39dbff9f01da() {
        assertEval("{ m <- matrix(1:6, nrow=2) ;  lower.tri(m, diag=TRUE) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testLowerTriangular_0ba0d133686dd0481614017fbd5e5b41() {
        assertEval("{ m <- matrix(1:6, nrow=2) ;  lower.tri(m, diag=FALSE) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testLowerTriangular_44ba325a12bc689011ed5350658dabb6() {
        assertEval("{ lower.tri(1:3, diag=TRUE) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testLowerTriangular_29e7f74c119f3fd2dff006792c5fa9a1() {
        assertEval("{ lower.tri(1:3, diag=FALSE) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testMatMultIgnore_e9ba9d6fa9abe7cec2ddbabcc73934ca() {
        assertEval("{ matrix(c(1+1i,2-2i,3+3i,4-4i), 2) %*% matrix(c(5+5i,6-6i,7+7i,8-8i), 2) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testMatMultIgnore_864ab8a27b006789bddc33fbf08a681d() {
        assertEval("{ matrix(c(3+3i,1-1i,2+2i,0-0i,1+1i,2-2i), 2) %*% matrix(c(1+1i,0-0i,4+4i,2-2i,1+1i,0-0i), 3) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testMatMultIgnore_2a28fb03fdd28d0687ea97640678c7c5() {
        assertEval("{ c(1+1i,2-2i,3+3i) %*% c(4-4i,5+5i,6-6i) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testMatMultIgnore_500c9fe5e23232d488b23dea0ffe60e6() {
        assertEval("{ matrix(c(3+3i,1-1i,2+2i,0-0i,1+1i,2-2i),2) %*% c(1+1i,0-0i,4+4i) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testMatMultIgnore_bd7d1161309480785b4881fa0f001408() {
        assertEval("{ c(1+1i,0-0i,4+4i) %*% matrix(c(3+3i,1-1i,2+2i,0-0i,1+1i,2-2i),3) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testMatrixIgnore_b8b7fe21147f05355c681ebdcad2082c() {
        assertEval("{ matrix(c(NaN,4+5i,2+0i,5+10i)} ");
    }

    @Ignore
    public void TestSimpleBuiltins_testMatrixIgnore_1fb1b5745bcee5d420963eac101e5666() {
        assertEval("{ matrix(TRUE,FALSE,FALSE,TRUE)}");
    }

    @Ignore
    public void TestSimpleBuiltins_testMatrixIgnore_f5dba0a59ab80b80d211e6e6fee198de() {
        assertEvalWarning("{ matrix(c(1,2,3,4),3,2) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testMatrixIgnore_8daf811c43e5de9f9027463997632ce6() {
        assertEvalWarning("{ matrix(1:4,3,2) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testMissingIgnore_cf62337ead8caecb2e4db39b971a6823() {
        assertEval("{ f <- function(a = 2 + 3) { missing(a) } ; f() }");
    }

    @Ignore
    public void TestSimpleBuiltins_testMissingIgnore_e3ec4820900994d734d0199b41a505ab() {
        assertEval("{ f <- function(a = z) { missing(a) } ; f() }");
    }

    @Ignore
    public void TestSimpleBuiltins_testMissingIgnore_14a03fde115ece14b0e877fd4bf28ad0() {
        assertEval("{ f <- function(a = 2 + 3) { a;  missing(a) } ; f() }");
    }

    @Ignore
    public void TestSimpleBuiltins_testMissingIgnore_0da52004f0b9453ad6deab5e0b49a111() {
        assertEval("{ f <- function(a = z) {  g(a) } ; g <- function(b) { missing(b) } ; f() }");
    }

    @Ignore
    public void TestSimpleBuiltins_testMissingIgnore_3b1c18d77df4f57cd223b95c8c205d89() {
        assertEval("{ f <- function(a = z, z) {  g(a) } ; g <- function(b) { missing(b) } ; f() }");
    }

    @Ignore
    public void TestSimpleBuiltins_testMissingIgnore_12f97d45e336adc392898885f48afa76() {
        assertEval("{ f <- function(a) { g(a) } ; g <- function(b=2) { missing(b) } ; f() }");
    }

    @Ignore
    public void TestSimpleBuiltins_testMissingIgnore_a96249c626958ddb13c95b4628e7f318() {
        assertEval("{ f <- function(x = y, y = x) { g(x, y) } ; g <- function(x, y) { missing(x) } ; f() }");
    }

    @Ignore
    public void TestSimpleBuiltins_testMissingIgnore_fc5302d7e40c71c48b09f7e6fcf1df6d() {
        assertEval("{ f <- function(x) { missing(x) } ; f(a) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testMissingIgnore_20756d2c3aaa3afd4ad6f87416f461ea() {
        assertEval("{ f <- function(a) { g <- function(b) { before <- missing(b) ; a <<- 2 ; after <- missing(b) ; c(before, after) } ; g(a) } ; f() }");
    }

    @Ignore
    public void TestSimpleBuiltins_testMissingIgnore_2c7389435b7285c22a1e276db60a1c8e() {
        assertEval("{ f <- function(...) { g(...) } ;  g <- function(b=2) { missing(b) } ; f() }");
    }

    @Ignore
    public void TestSimpleBuiltins_testMissingIgnore_7a7476796aa855e4eef288a9fa74b80f() {
        assertEval("{ f <- function(...) { missing(..2) } ; f(x + z, a * b) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testOperatorsIgnore_dd8820aada824b55da8fce1b2069a4a8() {
        assertEval("{ `%*%`(3,5) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testOpsGroupDispatchLs_2b92f252b506f74c3dd61aa019e285ed() {
        assertEval("{x<-1;y<-7;class(x)<-\"foo\";class(y)<-\"foo\";\"*.foo\"<-function(e1,e2){min(e1,e2)}; ls()}");
    }

    @Ignore
    public void TestSimpleBuiltins_testOrderIgnore_9d9e462e8a8cc7dbbf92366b9602bf39() {
        assertEval("{ order(1:3) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testOrderIgnore_ea195becea5e63c0bc6efd17b21ed503() {
        assertEval("{ order(3:1) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testOrderIgnore_05f671b27b0512bcbf1a2e113be7890a() {
        assertEval("{ order(c(1,1,1), 3:1) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testOrderIgnore_00ba7b7a2cb7b8ec3054739ef0c56f0e() {
        assertEval("{ order(c(1,1,1), 3:1, decreasing=FALSE) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testOrderIgnore_0aee4193d1ed56df561c1905296ddca9() {
        assertEval("{ order(c(1,1,1), 3:1, decreasing=TRUE, na.last=TRUE) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testOrderIgnore_87d0b85ae402b237e6eea7524e6ebfe2() {
        assertEval("{ order(c(1,1,1), 3:1, decreasing=TRUE, na.last=NA) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testOrderIgnore_63382528759189343899c7eaad048f33() {
        assertEval("{ order(c(1,1,1), 3:1, decreasing=TRUE, na.last=FALSE) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testOrderIgnore_8b055d570492191af8f8acd6bca6b6ad() {
        assertEval("{ order() }");
    }

    @Ignore
    public void TestSimpleBuiltins_testOrderIgnore_233b9224709438d6239a02a3cbca1d6f() {
        assertEval("{ order(c(NA,NA,1), c(2,1,3)) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testOrderIgnore_0d31ec524c63c01a9d36ce580dd87b76() {
        assertEval("{ order(c(NA,NA,1), c(1,2,3)) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testOrderIgnore_4b6ee44c315ce2abafeeff55be3bda6a() {
        assertEval("{ order(c(1,2,3,NA)) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testOrderIgnore_e08023e645a2200f800f52383def050b() {
        assertEval("{ order(c(1,2,3,NA), na.last=FALSE) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testOrderIgnore_5b504932f266176135d80d1de4c180a6() {
        assertEval("{ order(c(1,2,3,NA), na.last=FALSE, decreasing=TRUE) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testOrderIgnore_1b4cf21da630e25cd59c951bbff7a050() {
        assertEval("{ order(c(0/0, -1/0, 2)) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testOrderIgnore_e63709ad10dd0c536abd53f59d2cfdf8() {
        assertEval("{ order(c(0/0, -1/0, 2), na.last=NA) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testOuterIgnore_da963cbde1784128a50d0bb2220f4a09() {
        assertEval("{ foo <- function (x,y) { x + y * 1i } ; outer(3,3,foo) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testOuterIgnore_fa7bab756255d002e9b280b544ccabdb() {
        assertEval("{ foo <- function (x,y) { x + y * 1i } ; outer(3,3,\"foo\") }");
    }

    @Ignore
    public void TestSimpleBuiltins_testOuterIgnore_4a115174070896c785016a9d9d5d665e() {
        assertEval("{ foo <- function (x,y) { x + y * 1i } ; outer(1:3,1:3,foo) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testOuterIgnore_e5c558a0c7a7981c18d26924fb310194() {
        assertEval("{ outer(c(1,2,3),c(1,2),\"+\") }");
    }

    @Ignore
    public void TestSimpleBuiltins_testOuterIgnore_5182ad090b959b44000d6c63b2bf223b() {
        assertEval("{ outer(1:3,1:2) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testOuterIgnore_2d96437a7e8bbf4c84f39c87f3822203() {
        assertEval("{ outer(1:3,1:2,\"*\") }");
    }

    @Ignore
    public void TestSimpleBuiltins_testOuterIgnore_6dc2cca210d082a9eafba79e161f0d8f() {
        assertEval("{ outer(1:3,1:2, function(x,y,z) { x*y*z }, 10) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testOuterIgnore_9eece79caddd6ebf500a83a675d56b84() {
        assertEval("{ outer(1:2, 1:3, \"<\") }");
    }

    @Ignore
    public void TestSimpleBuiltins_testOuterIgnore_9260b477fa0c0eacb1851e4c1227c63d() {
        assertEval("{ outer(1:2, 1:3, '<') }");
    }

    @Ignore
    public void TestSimpleBuiltins_testParen_499acebd19ac76555ed92ca7ecc3ec53() {
        assertEval("{ a = array(1,c(3,3,3)); (a[1,2,3] = 3) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testPasteIgnore_1a3c1e77838670e434c0da99950c8e2c() {
        assertEval("{ file.path(\"a\", \"b\", c(\"d\",\"e\",\"f\")) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testPasteIgnore_3408303a6c99992f74f43cb72bc7fa75() {
        assertEval("{ file.path() }");
    }

    @Ignore
    public void TestSimpleBuiltins_testProdNa_75349670d382cb12b8cdbbfa32158e8a() {
        assertEval("{prod(c(2,4,NA))}");
    }

    @Ignore
    public void TestSimpleBuiltins_testProdNa_568e8381169ab3f99f187e83583f8455() {
        assertEval("{prod(c(2,4,3,NA),TRUE)}");
    }

    @Ignore
    public void TestSimpleBuiltins_testProdNa_9bbd7d1d1e4ccf2057fefdbf93dd46a4() {
        assertEval("{prod(c(1,2,3,4,5,NA),FALSE)}");
    }

    @Ignore
    public void TestSimpleBuiltins_testQr_4c61546a62c6441af95effa50e76e062() {
        assertEval(" { x <- qr(cbind(1:10,2:11), LAPACK=TRUE) ; round( qr.coef(x, 1:10), digits=5 ) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testQr_abe3bd72b9a9dc9279dace1511a3fac8() {
        assertEval("{ qr(10, LAPACK=TRUE) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testQr_2e522fbe7c114da3cb368c5f7274cf12() {
        assertEval("{ round( qr(matrix(1:6,nrow=2), LAPACK=TRUE)$qr, digits=5) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testQr_98faff4fc32371fae174496695a3a35b() {
        assertEval("{ qr(matrix(1:6,nrow=2), LAPACK=FALSE)$pivot }");
    }

    @Ignore
    public void TestSimpleBuiltins_testQr_251a3e0804d5e1ec4ba95cabe5851fea() {
        assertEval("{ qr(matrix(1:6,nrow=2), LAPACK=FALSE)$rank }");
    }

    @Ignore
    public void TestSimpleBuiltins_testQr_17317e26aa90dc9f21710b9567daa0c0() {
        assertEval("{ round( qr(matrix(1:6,nrow=2), LAPACK=FALSE)$qraux, digits=5 ) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testQr_637b00e95199b0d802bfd6e4c98184a6() {
        assertEval("{ round( qr(matrix(c(3,2,-3,-4),nrow=2), LAPACK=FALSE)$qr, digits=5 ) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testQr_0cb51ae181bc178bf223d49001723552() {
        assertEval("{ x <- qr(t(cbind(1:10,2:11)), LAPACK=TRUE) ; qr.coef(x, 1:2) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testQr_eb7d6b94998592915901ccdc876f3e5e() {
        assertEval("{ x <- qr(c(3,1,2), LAPACK=TRUE) ; round( qr.coef(x, c(1,3,2)), digits=5 ) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testQr_b833c099f54f44df2488c463c5977c69() {
        assertEval("{ x <- qr(t(cbind(1:10,2:11)), LAPACK=FALSE) ; qr.coef(x, 1:2) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testQr_faacf8177a822d44074aa43fd81139d5() {
        assertEval("{ x <- qr(c(3,1,2), LAPACK=FALSE) ; round( qr.coef(x, c(1,3,2)), digits=5 ) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testQr_17cad1cc6779f137acbded3e743990f8() {
        assertEval("{ m <- matrix(c(1,0,0,0,1,0,0,0,1),nrow=3) ; x <- qr(m, LAPACK=FALSE) ; qr.coef(x, 1:3) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testQr_1cd6ad9cf8d11508e422eae128c0fa58() {
        assertEval("{ x <- qr(cbind(1:3,2:4), LAPACK=FALSE) ; round( qr.coef(x, 1:3), digits=5 ) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testQr_e2d68b4592f13f68f031a68d95f80d75() {
        assertEval("{ round( qr.solve(qr(c(1,3,4,2)), c(1,2,3,4)), digits=5 ) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testQr_46728c69e8381944b3e3b0272b971935() {
        assertEval("{ round( qr.solve(c(1,3,4,2), c(1,2,3,4)), digits=5) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testQr_cb5a4156797fb35468b2a52c03675858() {
        assertEvalError("{ x <- qr(cbind(1:10,2:11), LAPACK=TRUE) ; qr.coef(x, 1:2) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testQuoteIgnore_2ce345e0f74c01976ac35948bfab5a71() {
        assertEval("{ l <- quote(x[1,1] <- 10) ; f <- function() { eval(l) } ; x <- matrix(1:4,nrow=2) ; f() ; x }");
    }

    @Ignore
    public void TestSimpleBuiltins_testQuoteIgnore_409341bfbb82606d75eb0c1700c98952() {
        assertEval("{ l <- quote(x[1] <- 1) ; f <- function() { eval(l) } ; x <- 10 ; f() ; x }");
    }

    @Ignore
    public void TestSimpleBuiltins_testQuoteIgnore_b8cacd46656e5a810809ba31bd8af586() {
        assertEval("{ l <- quote(x[1] <- 1) ; f <- function() { eval(l) ; x <<- 10 ; get(\"x\") } ; x <- 20 ; f() }");
    }

    @Ignore
    public void TestSimpleBuiltins_testRandomIgnore_4f7a7feadb0afd594a6252de0817b40f() {
        assertEval("{ set.seed(4357, \"default\"); round( rnorm(3,1000,10), digits = 5 ) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testRandomIgnore_784f02d69de0bfc6b26f80cc27b3eaf0() {
        assertEval("{ round( rnorm(3,c(1000,2,3),c(10,11)), digits = 5 ) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testRandomIgnore_b2e35c06b054d504b83a29fdc0f2c77a() {
        assertEval("{ round( runif(3), digits = 5 ) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testRandomIgnore_38f6214fa41def07b060c01b29004277() {
        assertEval("{ round( runif(3,1,10), digits = 5 ) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testRandomIgnore_f1a576fe16d8967d5d94472745eb8757() {
        assertEval("{ round( runif(3,1:3,3:2), digits = 5 ) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testRandomIgnore_b1cb39289a32d016a5e4d8fd0369a06b() {
        assertEval("{ round( rgamma(3,1), digits = 5 ) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testRandomIgnore_98b47b95df69a17bd9bfaf2a24c9cffd() {
        assertEval("{ round( rgamma(3,0.5,scale=1:3), digits = 5 ) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testRandomIgnore_fd28dcd349e0cca475812e380ef658bf() {
        assertEval("{ round( rgamma(3,0.5,rate=1:3), digits = 5 ) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testRandomIgnore_e0ebcb975feabfb978612a64a771116e() {
        assertEval("{ round( rbinom(3,3,0.9), digits = 5 ) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testRandomIgnore_8c7daa50068479e536d478513c940605() {
        assertEval("{ round( rbinom(3,10,(1:5)/5), digits = 5 ) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testRandomIgnore_7d00e32e71b1e734a6bf82d8e5ad1e59() {
        assertEval("{ round( rlnorm(3), digits = 5 ) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testRandomIgnore_b35e5af9e87e8a17b87bad6537a48322() {
        assertEval("{ round( rlnorm(3,sdlog=c(10,3,0.5)), digits = 5 ) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testRandomIgnore_9e1f8a6e4a70c5688947e9205b449a9e() {
        assertEval("{ round( rcauchy(3), digits = 5 ) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testRandomIgnore_df5e70f5779809e68123bd1f1474d2de() {
        assertEval("{ round( rcauchy(3, scale=4, location=1:3), digits = 5 ) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testRank_ac4677bb60d34cb54b0855ff9af216fe() {
        assertEval("{ rank(c(10,100,100,1000)) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testRank_b661e996c8bab94a49a1b912170e269c() {
        assertEval("{ rank(c(1000,100,100,100, 10)) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testRank_8119ffd7c473890dd5a8fb4bb4eb27dd() {
        assertEval("{ rank(c(a=2,b=1,c=3,40)) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testRank_79652345882c62a61705a5fc72b80f6c() {
        assertEval("{ rank(c(a=2,b=1,c=3,d=NA,e=40), na.last=NA) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testRank_d933b8b9599a925bdbfc61565085f049() {
        assertEval("{ rank(c(a=2,b=1,c=3,d=NA,e=40), na.last=\"keep\") }");
    }

    @Ignore
    public void TestSimpleBuiltins_testRank_920ad82c4f789e0f160e9bec2592a796() {
        assertEval("{ rank(c(a=2,b=1,c=3,d=NA,e=40), na.last=TRUE) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testRank_71d5d62deb1ac8f050666be28cc69770() {
        assertEval("{ rank(c(a=2,b=1,c=3,d=NA,e=40), na.last=FALSE) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testRank_25cc554304f5043b71318c6e7db78796() {
        assertEval("{ rank(c(a=1,b=1,c=3,d=NA,e=3), na.last=FALSE, ties.method=\"max\") }");
    }

    @Ignore
    public void TestSimpleBuiltins_testRank_71c5bf762cec2ebaac51f86364fad786() {
        assertEval("{ rank(c(a=1,b=1,c=3,d=NA,e=3), na.last=NA, ties.method=\"min\") }");
    }

    @Ignore
    public void TestSimpleBuiltins_testRank_4b9cea01de60a8694a6b5606f91cf6e5() {
        assertEval("{ rank(c(1000, 100, 100, NA, 1, 20), ties.method=\"first\") }");
    }

    @Ignore
    public void TestSimpleBuiltins_testRbindIgnore_53509c8f581c1a9947804e87f0a3580f() {
        assertEval("{ info <- c(\"print\", \"AES\", \"print.AES\") ; ns <- integer(0) ; rbind(info, ns) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testRegExprComplex_86a31eb43c44df7c7453e0bfe0140ded() {
        assertEval("gregexpr(\"(a)[^a]\\1\", c(\"andrea apart\", \"amadeus\", NA))");
    }

    @Ignore
    public void TestSimpleBuiltins_testRegExprComplex_97c86df70bdfc0c7ad6b46db6019ca17() {
        assertEval("regexpr(\"(a)[^a]\\1\", c(\"andrea apart\", \"amadeus\", NA))");
    }

    @Ignore
    public void TestSimpleBuiltins_testRegExprIgnore_72408e09ac9c484ede969026b2eec870() {
        assertEval("regexpr(\"e\",c(\"arm\",\"foot\",\"lefroo\", \"bafoobar\"))");
    }

    @Ignore
    public void TestSimpleBuiltins_testRegExprIgnore_aed64085f066f3404115215e0fded1c4() {
        assertEval("gregexpr(\"e\",c(\"arm\",\"foot\",\"lefroo\", \"bafoobar\"))");
    }

    @Ignore
    public void TestSimpleBuiltins_testRoundIgnore_bb594f5dd03efc19fa1dbee51b5324da() {
        assertEval("{ round(1.123456,digit=2.8) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testRowMeansIgnore_ee1961081fcfca53ea506fa81009d5b5() {
        assertEval("{rowMeans(matrix(c(NaN,4+5i,2+0i,5+10i),nrow=2,ncol=2), na.rm = FALSE)}");
    }

    @Ignore
    public void TestSimpleBuiltins_testRowMeansIgnore_1d83557bfc6a9792419b6f87c844133e() {
        assertEval("{rowMeans(matrix(NA,NA,NA),TRUE)}");
    }

    @Ignore
    public void TestSimpleBuiltins_testRowMeansIgnore_80aaaab0a28cbb6472de7b973b40187b() {
        assertEval("{x<-matrix(c(\"1\",\"2\",\"3\",\"4\"),ncol=2);rowMeans(x)}");
    }

    @Ignore
    public void TestSimpleBuiltins_testRowSumsIgnore_fc993b3be8ff0e09bc78bbb22bcf0aec() {
        assertEval("{x<-matrix(c(\"1\",\"2\",\"3\",\"4\"),ncol=2);rowSums(x)}");
    }

    @Ignore
    public void TestSimpleBuiltins_testSampleIgnore_ff42abcbf4f968c27e32a7dd28eda044() {
        assertEval("{ set.seed(9567, \"Marsaglia-Multicarry\");x <- c(5) ; prob <- c(1, 2, 3, 4, 5) ; sample(x, 5, TRUE, prob) ; }");
    }

    @Ignore
    public void TestSimpleBuiltins_testSampleIgnore_c0abb95d78ba54d518dba3716e78f683() {
        assertEval("{ set.seed(9567, \"Marsaglia-Multicarry\");x <- c(5) ; prob <- c(1, 2, 3, 4, 5) ; sample(x, 5, FALSE, prob) ; }");
    }

    @Ignore
    public void TestSimpleBuiltins_testSampleIgnore_b703c1a90d66f9baf7ccbe08919f69d1() {
        assertEval("{ set.seed(9567, \"Marsaglia-Multicarry\");x <- c(\"Heads\", \"Tails\") ; prob <- c(.3, .7) ; sample(x, 10, TRUE, prob) ; }");
    }

    @Ignore
    public void TestSimpleBuiltins_testSampleIgnore_c90feee3f3b3a20606e1b43eab8afb31() {
        assertEval("{ set.seed(4357, \"default\"); x <- c(5) ; prob <- c(1, 2, 3, 4, 5) ; sample(x, 5, TRUE, prob) ; }");
    }

    @Ignore
    public void TestSimpleBuiltins_testSampleIgnore_18482bc15a1e30cd46e5be81317a3374() {
        assertEval("{ set.seed(4357, \"default\"); x <- c(5) ; prob <- c(1, 2, 3, 4, 5) ; sample(x, 5, FALSE, prob) ; }");
    }

    @Ignore
    public void TestSimpleBuiltins_testSampleIgnore_38b963b6f50f4a4d9e1250d1df321b43() {
        assertEval("{ set.seed(4357, \"default\"); x <- 5 ; sample(x, 6, FALSE, NULL) ;}");
    }

    @Ignore
    public void TestSimpleBuiltins_testSampleIgnore_2935bb73d988381d4ae52f265101577a() {
        assertEval("{ set.seed(9567, \"Marsaglia-Multicarry\"); x <- 5 ; sample(x, 6, FALSE, NULL) ;}");
    }

    @Ignore
    public void TestSimpleBuiltins_testSetAttr_4c035922fa30fd65161fe53e1af97368() {
        assertEval("{ x <- NULL; levels(x)<-\"dog\"; levels(x)}");
    }

    @Ignore
    public void TestSimpleBuiltins_testSetAttr_d3a803a8bcf4ca34f3f28cc87c530aef() {
        assertEval("{ x <- 1 ; levels(x)<-NULL; levels(notx)}");
    }

    @Ignore
    public void TestSimpleBuiltins_testSortIgnore_6a592c6f57c71c5d15a2ca0155fee884() {
        assertEval("{ sort(c(1,2,0/0,NA)) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testSortIgnore_5aa86dc4ae1bb25c682d61e872e9b040() {
        assertEval("{ sort(c(2,1,0/0,NA), na.last=NA) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testSortIgnore_6a7ec5187507fa97abda94b64f5a079d() {
        assertEval("{ sort(c(3,0/0,2,NA), na.last=TRUE) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testSortIgnore_b5d4d0684b5f7ae93abbd963d09e2547() {
        assertEval("{ sort(c(3,NA,0/0,2), na.last=FALSE) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testSortIgnore_ccb733ea6a05ce0344a90278f6b60239() {
        assertEval("{ sort(c(3L,NA,2L)) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testSortIgnore_894104e630b40ec41f7a3242c9dd48bb() {
        assertEval("{ sort(c(3L,NA,-2L), na.last=TRUE) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testSortIgnore_7371476317ce19939f96f4a8546a66ca() {
        assertEval("{ sort(c(3L,NA,-2L), na.last=FALSE) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testSortIgnore_b2088bf4f79792e07aeb1878814c42dd() {
        assertEval("{ sort(c(a=NA,b=NA,c=3,d=1),na.last=TRUE, decreasing=TRUE) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testSortIgnore_7cfdc805071697201c562b5f50ebd539() {
        assertEval("{ sort(c(a=NA,b=NA,c=3,d=1),na.last=FALSE, decreasing=FALSE) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testSortIgnore_ac8a4c1d13606a72e3e1b8c439efda29() {
        assertEval("{ sort(c(a=0/0,b=1/0,c=3,d=NA),na.last=TRUE, decreasing=FALSE) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testSortIgnore_519a0465d477a73e1db30d78e8776c1b() {
        assertEval("{ sort(double()) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testSortIgnore_df4ed76c79e6d77ac09a69738271e1fd() {
        assertEval("{ sort(c(a=NA,b=NA,c=3L,d=-1L),na.last=TRUE, decreasing=FALSE) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testSortIgnore_2ce0809f50d42943354aa60d00cd1a90() {
        assertEval("{ sort(c(3,NA,1,d=10), decreasing=FALSE, index.return=TRUE) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testSortIgnore_9f37df375d06bb45b37c5fe0fb3d1b54() {
        assertEval("{ sort(3:1, index.return=TRUE) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testSource_5c17b4de1a98b4e6a8cfa7815d97f7e4() {
        assertEval("{ source(\"test/r/simple/data/tree2/setx.r\") ; x }");
    }

    @Ignore
    public void TestSimpleBuiltins_testSource_d4a38dfd161547e3c0a27bad69e1cbf8() {
        assertEval("{ source(\"test/r/simple/data/tree2/setx.r\", local=TRUE) ; x }");
    }

    @Ignore
    public void TestSimpleBuiltins_testSource_be101f4a7d5eb393d6100a7da3b04018() {
        assertEval("{ x <- 1; f <- function() { source(\"test/r/simple/data/tree2/setx.r\", local=TRUE) ; x } ; c(f(), x) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testSource_f8c23fa44e5be57cccce50c2c2c77af6() {
        assertEval("{ x <- 1; f <- function() { source(\"test/r/simple/data/tree2/setx.r\", local=FALSE) ; x } ; c(f(), x) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testSource_47529aa6f5e299a286137b552e7163dc() {
        assertEval("{ x <- 1; f <- function() { source(\"test/r/simple/data/tree2/incx.r\", local=FALSE) ; x } ; c(f(), x) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testSource_e52eebdb86410e47576dc1c11b4690b0() {
        assertEval("{ x <- 1; f <- function() { source(\"test/r/simple/data/tree2/incx.r\", local=TRUE) ; x } ; c(f(), x) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testSqrtBroken_dda9ccdc11f9f5afbe9854145501c5e5() {
        assertEval("{ sqrt(c(a=9,b=81)) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testSqrtBroken_a2489c7a22d5ac414a9587cbff9b6c64() {
        assertEval("{ sqrt(1:5) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testSqrtBroken_cae4a927d1bb1f88b88550ba795899f5() {
        assertEval("{ sqrt(-1L) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testSqrtBroken_d1949f3b9fbc81f7fe02ad4b8719bcaa() {
        assertEval("{ sqrt(-1) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testSubIgnore_0902579a0dce5fa8d7a808155b8c09b0() {
        assertEval("{ gsub(\"a\",\"aa\", \"prague alley\", fixed=TRUE) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testSubIgnore_fa9e3d4d6577b70532d26a56fc343b17() {
        assertEval("{ sub(\"a\",\"aa\", \"prague alley\", fixed=TRUE) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testSubIgnore_dca0ae0449dfa1c58f334818a4b87673() {
        assertEval("{ gsub(\"a\",\"aa\", \"prAgue alley\", fixed=TRUE) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testSubIgnore_3d79a5bb75bf60e95350618f5485daa6() {
        assertEval("{ gsub(\"a\",\"aa\", \"prAgue alley\", fixed=TRUE, ignore.case=TRUE) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testSubIgnore_d1977e782dbbd1ca4da912d5f56d63ed() {
        assertEval("{ gsub(\"a\",\"aa\", \"prAgue alley\", ignore.case=TRUE) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testSubIgnore_7332b217f11d38a5978915cf31eff6f4() {
        assertEval("{ gsub(\"([a-e])\",\"\\1\\1\", \"prague alley\") }");
    }

    @Ignore
    public void TestSimpleBuiltins_testSubIgnore_8df24d5d1e0149a6b232c373b6057aa7() {
        assertEval("{ gsub(\"h\",\"\", c(\"hello\", \"hi\", \"bye\")) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testSubIgnore_42529469f0a7019b2a56e1e5312e0577() {
        assertEval("{ gsub(\"h\",\"\", c(\"hello\", \"hi\", \"bye\"), fixed=TRUE) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testSubstitute_4d6f07ded5992a096c046ebead59dfd0() {
        assertEval("{ substitute(x + y, list(x=1)) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testSubstitute_69aeec67da0ee58f71a5a4244df69a7c() {
        assertEval("{ f <- function(expr) { substitute(expr) } ; f(a * b) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testSubstitute_4c1a0e897f6f8dcba279129803430c82() {
        assertEval("{ f <- function() { delayedAssign(\"expr\", a * b) ; substitute(expr) } ; f() }");
    }

    @Ignore
    public void TestSimpleBuiltins_testSubstitute_f82a54616cf2b4be6f752e5c66c635c9() {
        assertEval("{ f <- function() { delayedAssign(\"expr\", a * b) ; substitute(dummy) } ; f() }");
    }

    @Ignore
    public void TestSimpleBuiltins_testSubstitute_587cbbd25dcab3e16f1b360e583c7db5() {
        assertEval("{ delayedAssign(\"expr\", a * b) ; substitute(expr) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testSubstitute_dc45366e3a931d33e1c7ea987435cdd1() {
        assertEval("{ f <- function(expr) { expr ; substitute(expr) } ; a <- 10; b <- 2; f(a * b) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testSubstitute_61078b0c4da1266fe57918a4361362dd() {
        assertEval("{ f <- function(expra, exprb) { substitute(expra + exprb) } ; f(a * b, a + b) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testSubstitute_d84d47dddb7bd0bf96bf16437eadd619() {
        assertEval("{ f <- function(y) { substitute(y) } ; f() }");
    }

    @Ignore
    public void TestSimpleBuiltins_testSubstitute_5f9847b1be03c329f3c41d8883684dc2() {
        assertEval("{ f <- function(y) { substitute(y) } ; typeof(f()) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testSubstitute_8308ab3830982170f12169a348ea89e8() {
        assertEval("{ f <- function(z) { g <- function(y) { substitute(y)  } ; g(z) } ; f(a + d) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testSubstitute_a8173ff3145e5caeadfe0a38e28a2a09() {
        assertEval("{ f <- function(x) { g <- function() { substitute(x) } ; g() } ;  f(a * b) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testSubstitute_89798b3d8963d8d31c6b22ed6bc05491() {
        assertEval("{ substitute(a, list(a = quote(x + y), x = 1)) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testSubstitute_3e4cc116e9a592c28b2159c6e8365bfa() {
        assertEval("{ f <- function(x = y, y = x) { substitute(x) } ; f() }");
    }

    @Ignore
    public void TestSimpleBuiltins_testSubstitute_1bcbef75639b8b543cc72a07279a2203() {
        assertEval("{ f <- function(a, b=a, c=b, d=c) { substitute(d) } ; f(x + y) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testSubstitute_b728de23a3c96c7d1c7e179ba0cf22c8() {
        assertEval("{ substitute(if(a) { x } else { x * a }, list(a = quote(x + y), x = 1)) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testSubstitute_b9b9e1994091af7e565e035d8c87b9ef() {
        assertEval("{ substitute(function(x, a) { x + a }, list(a = quote(x + y), x = 1)) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testSubstitute_9d646fcf10648fbae8e8087bb65a9bd6() {
        assertEval("{ substitute(a[x], list(a = quote(x + y), x = 1)) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testSubstitute_844fb1f54ddd6fb3cb03e5a9d632edda() {
        assertEval("{ f <- function(x) { substitute(x, list(a=1,b=2)) } ; f(a + b) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testSubstitute_0083a2f370b2d901d6617b52259cd8ef() {
        assertEval("{ f <- function() { substitute(x(1:10), list(x=quote(sum))) } ; f() }");
    }

    @Ignore
    public void TestSimpleBuiltins_testSubstitute_ba8b61c2d3fa9c76a2c14d5e96138f4b() {
        assertEval("{ env <- new.env() ; z <- 0 ; delayedAssign(\"var\", z+2, assign.env=env) ; substitute(var, env=env) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testSubstitute_91aaa32f72b8dab4c7856c1e7e89ed54() {
        assertEval("{ env <- new.env() ; z <- 0 ; delayedAssign(\"var\", z+2, assign.env=env) ; z <- 10 ; substitute(var, env=env) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testSubstitute_8b21e0ecb7d6143dab8b63c68608f906() {
        assertEval("{ f <- function() { substitute(list(a=1,b=2,...,3,...)) } ; f() }");
    }

    @Ignore
    public void TestSimpleBuiltins_testSubstitute_6da555da9a31bfb212efe33b45c838d7() {
        assertEval("{ f <- function(...) { substitute(list(a=1,b=2,...,3,...)) } ; f() }");
    }

    @Ignore
    public void TestSimpleBuiltins_testSubstitute_fc2154960706a9f7207993aa89aaca50() {
        assertEval("{ f <- function(...) { substitute(list(a=1,b=2,...,3,...)) } ; f(x + z, a * b) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testSubstitute_b6449119b833609315c063f2a2c5a363() {
        assertEval("{ f <- function(...) { substitute(list(...)) } ; f(x + z, a * b) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testSumIgnore_512304594d55f1330efacd6cc594cf7a() {
        assertEval("{ sum(0, 1[3]) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testSumIgnore_b579f0fccb80261d02dd8e36a1c21977() {
        assertEval("{ sum(na.rm=FALSE, 0, 1[3]) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testSumIgnore_71b125cd0c9f2fe015befa381709e1a6() {
        assertEval("{ sum(0, na.rm=FALSE, 1[3]) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testSumIgnore_d6658778aa6ef9490e87eee1748c00b1() {
        assertEval("{ sum(0, 1[3], na.rm=FALSE) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testSumIgnore_d8048d7927bb3ae55032b224e19caf66() {
        assertEval("{ sum(0, 1[3], na.rm=TRUE) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testSumIgnore_79d5da5603083c8a7cd4e867a99de305() {
        assertEval("{ sum(1+1i,2,NA, na.rm=TRUE) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testSweepBroken_922919324a346071a3eb17872bd65bfd() {
        assertEval("{ sweep(array(1:24, dim = 4:2), 1:2, 5) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testSweepBroken_764897cc4d4562a31c107658a96cc3b2() {
        assertEval("{ A <- matrix(1:15, ncol=5); sweep(A, 2, colSums(A), \"/\") }");
    }

    @Ignore
    public void TestSimpleBuiltins_testSweepBroken_403bf44c1ac2aaf3d8cdb91d68b2345d() {
        assertEval("{ A <- matrix(1:50, nrow=4); sweep(A, 1, 5, '-') }");
    }

    @Ignore
    public void TestSimpleBuiltins_testSweepBroken_542d39a4358474b0ed5e7284b7652493() {
        assertEval("{ A <- matrix(7:1, nrow=5); sweep(A, 1, -1, '*') }");
    }

    @Ignore
    public void TestSimpleBuiltins_testSwitchIgnore_85ece8b67b950e9299c9a4d4dcb0b533() {
        assertEval("{answer<-\"no\";switch(as.character(answer), yes=, YES=1, no=, NO=2,3)}");
    }

    @Ignore
    public void TestSimpleBuiltins_testTriangular_41ca685d92138926005a9f7fb6ca8478() {
        assertEval("{ m <- { matrix( as.character(1:6), nrow=2 ) } ; diag(m) <- c(1,2) ; m }");
    }

    @Ignore
    public void TestSimpleBuiltins_testTriangular_f1776e942214f71194d5c31b1a80996e() {
        assertEval("{ m <- { matrix( (1:6) * (1+3i), nrow=2 ) } ; diag(m) <- c(1,2) ; m }");
    }

    @Ignore
    public void TestSimpleBuiltins_testTriangular_e3c989be96bfd58a83c33b08e911de62() {
        assertEval("{ m <- { matrix( as.raw(11:16), nrow=2 ) } ; diag(m) <- c(as.raw(1),as.raw(2)) ; m }");
    }

    @Ignore
    public void TestSimpleBuiltins_testTypeCheckIgnore_7f8323b03018432a0d32c10f362ec5d7() {
        assertEval("{ is.list(NULL) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testUpdateClassIgnore_de2b6cfc60c31afa53dbd74ec10d3136() {
        assertEval("{x<-c(1,2,3,4); class(x)<-\"array\"; class(x)<-\"matrix\";}");
    }

    @Ignore
    public void TestSimpleBuiltins_testUpdateClassIgnore_dfbd07abb7b6feb1f2afd25c4ad019ef() {
        assertEval("{x<-1;attr(x,\"class\")<-c(1,2,3);}");
    }

    @Ignore
    public void TestSimpleBuiltins_testUpdateNamesIgnore_2cffdfa878b18bbad7b6a53d7e4932ae() {
        assertEval("{ x <- c(1,2); names(x) <- c(\"A\", \"B\") ; x + 1 }");
    }

    @Ignore
    public void TestSimpleBuiltins_testUpdateNamesIgnore_d40e4da2cc65cb7648581165a629d52a() {
        assertEval("{ x <- 1:2; names(x) <- c(\"A\", \"B\") ; y <- c(1,2,3,4) ; names(y) <- c(\"X\", \"Y\", \"Z\") ; x + y }");
    }

    @Ignore
    public void TestSimpleBuiltins_testUseMethodLocalVars_cd724107886a7c9d25ae3b6aad713cb6() {
        assertEval("{f <- function(x){ y<-2;locFun <- function(){cat(\"local\")}; UseMethod(\"f\"); }; f.second <- function(x){cat(\"f second\",x);locFun();}; obj <-1; attr(obj,\"class\")  <- \"second\"; f(obj);}");
    }

    @Ignore
    public void TestSimpleBuiltins_testWhichIgnore_6d01b8ef11e5cdf979ca7122cd3de717() {
        assertEval("{ which(c(a=TRUE,b=FALSE,c=TRUE)) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testWorkingDirectory_4dea13731bbc2e14f050d3a8c9270396() {
        assertEval("{ cur <- getwd(); cur1 <- setwd(getwd()) ; cur2 <- getwd() ; cur == cur1 && cur == cur2 }");
    }

    @Ignore
    public void TestSimpleBuiltins_testWorkingDirectory_4158e8f80f9f54af9ceaf07aaacc8395() {
        assertEval("{ cur <- getwd(); cur1 <- setwd(c(cur, \"dummy\")) ; cur2 <- getwd() ; cur == cur1  }");
    }

    @Ignore
    public void TestSimpleBuiltins_testWorkingDirectory_b06c73943c7300d6a0af95bb6d4140c3() {
        assertEvalError("{ setwd(1) }");
    }

    @Ignore
    public void TestSimpleBuiltins_testWorkingDirectory_d4bb5261e83943081702a1fb0f063135() {
        assertEvalError("{ setwd(character()) }");
    }

    @Ignore
    public void TestSimpleComparison_testMatrices_eb091ba085dda60b02299905b6603cba() {
        assertEval("{ matrix(1) > matrix(2) }");
    }

    @Ignore
    public void TestSimpleComparison_testMatrices_e08838ffe9812e3d1cb041aaddec856a() {
        assertEval("{ matrix(1) > NA }");
    }

    @Ignore
    public void TestSimpleComparison_testMatrices_6e89d79b793dfb2076088167e168c6e0() {
        assertEval("{ m <- matrix(1:6, nrow=2) ; m > c(1,2,3) }");
    }

    @Ignore
    public void TestSimpleComparison_testScalarsIgnore_7a6557e91f8f198b6c11c29c4e572f57() {
        assertEval("{ z <- TRUE; dim(z) <- c(1) ; dim(z == TRUE) }");
    }

    @Ignore
    public void TestSimpleComparison_testScalarsIgnore_6f066c83dbb9ff430a0d5056100fbb50() {
        assertEvalError("{ z <- TRUE; dim(z) <- c(1) ; u <- 1:3 ; dim(u) <- 3 ; u == z }");
    }

    @Ignore
    public void TestSimpleComparison_testScalarsNAAsFunctionIgnore_0a500b31b16f008e4a1dc5b5630344c8() {
        assertEval("{ f <- function(a,b) { a > b } ; f(1,2) ; f(1L,2) ; f(\"hello\", \"hi\"[2]) }");
    }

    @Ignore
    public void TestSimpleComparison_testScalarsNAAsFunctionIgnore_c803d5d2a05362ff97b2237e3502ac08() {
        assertEval("{ f <- function(a,b) { a > b } ; f(1,2) ; f(1L,2) ; f(\"hello\"[2], \"hi\") }");
    }

    @Ignore
    public void TestSimpleFunctions_testDefinitionsIgnore_b8d75a017c31d73d6dbf7c6a93953d67() {
        assertEval("{ x <- function(a,b) { a^b } ; f <- function() { x <- \"sum\" ; sapply(1, x, 2) } ; f() }");
    }

    @Ignore
    public void TestSimpleFunctions_testDefinitionsIgnore_dfa24cb65db3a6a592617aa583ec1aaa() {
        assertEval("{ x <- function(a,b) { a^b } ; g <- function() { x <- \"sum\" ; f <- function() { sapply(1, x, 2) } ; f() }  ; g() }");
    }

    @Ignore
    public void TestSimpleFunctions_testDefinitionsIgnore_6ff99329ff4c5405259dd094d456df82() {
        assertEval("{ x <- function(a,b) { a^b } ; f <- function() { x <- 211 ; sapply(1, x, 2) } ; f() }");
    }

    @Ignore
    public void TestSimpleFunctions_testDefinitionsIgnore_90214c174a4cd064fcdf43a64bba6f73() {
        assertEval("{ x <- function(a,b) { a^b } ; dummy <- sum ; f <- function() { x <- \"dummy\" ; sapply(1, x, 2) } ; f() }");
    }

    @Ignore
    public void TestSimpleFunctions_testDefinitionsIgnore_ba4a8d210d2bcdac8ede803b28c13172() {
        assertEval("{ x <- function(a,b) { a^b } ; dummy <- sum ; f <- function() { x <- \"dummy\" ; dummy <- 200 ; sapply(1, x, 2) } ; f() }");
    }

    @Ignore
    public void TestSimpleFunctions_testDefinitionsIgnore_8ef4913016fe9a78ae79cb9f48e3c5ae() {
        assertEval("{ foo <- function (x) { x } ; foo() }");
    }

    @Ignore
    public void TestSimpleFunctions_testDefinitionsIgnore_1c3efc0657001d0ce5000a68b2e7b18d() {
        assertEval("{ foo <- function (x) { x } ; foo(1,2,3) }");
    }

    @Ignore
    public void TestSimpleFunctions_testDotsIgnore_e620898284cbe5e1d40bfe326c77804e() {
        assertEval("{ f <- function(...) { ..1 } ;  f(10) }");
    }

    @Ignore
    public void TestSimpleFunctions_testDotsIgnore_a8e7323fa1a949f877214637cf0a91b1() {
        assertEval("{ f <- function(...) { x <<- 10 ; ..1 } ; x <- 1 ; f(x) }");
    }

    @Ignore
    public void TestSimpleFunctions_testDotsIgnore_ab19b9b703d36ea0149b6950305344b1() {
        assertEval("{ f <- function(...) { ..1 ; x <<- 10 ; ..1 } ; x <- 1 ; f(x) }");
    }

    @Ignore
    public void TestSimpleFunctions_testDotsIgnore_a52d7c73079437ca5443652b7f20f2ef() {
        assertEval("{ f <- function(...) { ..1 ; x <<- 10 ; ..2 } ; x <- 1 ; f(100,x) }");
    }

    @Ignore
    public void TestSimpleFunctions_testDotsIgnore_fc05b96d7c209b4b11d3c1597a4f5d95() {
        assertEval("{ f <- function(...) { ..2 ; x <<- 10 ; ..1 } ; x <- 1 ; f(x,100) }");
    }

    @Ignore
    public void TestSimpleFunctions_testDotsIgnore_d6e84b6c4d84ca15395f370802824ec0() {
        assertEval("{ g <- function(...) { 0 } ; f <- function(...) { g(...) ; x <<- 10 ; ..1 } ; x <- 1 ; f(x) }");
    }

    @Ignore
    public void TestSimpleFunctions_testDotsIgnore_581191e3ee585752a4393b1dd5c20af3() {
        assertEval("{ f <- function(...) { substitute(..1) } ;  f(x+y) }");
    }

    @Ignore
    public void TestSimpleFunctions_testDotsIgnore_46356a32a158c79de398dd64974058fc() {
        assertEval("{ f <- function(...) { g <- function() { ..1 } ; g() } ; f(a=2) }");
    }

    @Ignore
    public void TestSimpleFunctions_testDotsIgnore_569ec3ad103b4dcd2b7e7af1202dd26f() {
        assertEval("{ f <- function(...) { ..1 <- 2 ; ..1 } ; f(z = 1) }");
    }

    @Ignore
    public void TestSimpleFunctions_testDotsIgnore_a29c54a3c8cd1ee3e35a2aea432951cb() {
        assertEval("{ g <- function(a,b) { a + b } ; f <- function(...) { g(...) }  ; f(1,2) }");
    }

    @Ignore
    public void TestSimpleFunctions_testDotsIgnore_82b39f3b671e13554b9f70c67b51d9bc() {
        assertEval("{ g <- function(a,b,x) { a + b * x } ; f <- function(...) { g(...,x=4) }  ; f(b=1,a=2) }");
    }

    @Ignore
    public void TestSimpleFunctions_testDotsIgnore_44ffe8a1375fa81b1531c8e8a3c876ee() {
        assertEval("{ g <- function(a,b,x) { a + b * x } ; f <- function(...) { g(x=4, ...) }  ; f(b=1,a=2) }");
    }

    @Ignore
    public void TestSimpleFunctions_testDotsIgnore_1d94abf5afd9989c20c9e7713f15aa3a() {
        assertEval("{ g <- function(a,b,x) { a + b * x } ; f <- function(...) { g(x=4, ..., 10) }  ; f(b=1) }");
    }

    @Ignore
    public void TestSimpleFunctions_testDotsIgnore_b7c1eb65db6a2cb8b5f3401383477104() {
        assertEval("{ g <- function(a,b,aa,bb) { a ; x <<- 10 ; aa ; c(a, aa) } ; f <- function(...) {  g(..., ...) } ; x <- 1; y <- 2; f(x, y) }");
    }

    @Ignore
    public void TestSimpleFunctions_testDotsIgnore_f869c81e19bebe1d0b508f3152867860() {
        assertEval("{ f <- function(a, b) { a - b } ; g <- function(...) { f(1, ...) } ; g(a = 2) }");
    }

    @Ignore
    public void TestSimpleFunctions_testDotsIgnore_168904965e7c99fe53738eba7ef80c6e() {
        assertEval("{ f <- function(a, barg, ...) { a + barg } ; g <- function(...) { f(a=1, ...) } ; g(b=2,3) }");
    }

    @Ignore
    public void TestSimpleFunctions_testDotsIgnore_446276723386c4e17ee775d34b52759a() {
        assertEval("{ f <- function(a, barg, bextra, dummy) { a + barg } ; g <- function(...) { f(a=1, ...) } ; g(be=2,du=3, 3) }");
    }

    @Ignore
    public void TestSimpleFunctions_testDotsIgnore_30b478f9a7f62680adb64c9c36c9ab71() {
        assertEval("{ f <- function(a, barg, bextra, dummy) { a + barg } ; g <- function(...) { f(a=1, ...) } ; g(1,2,3) }");
    }

    @Ignore
    public void TestSimpleFunctions_testDotsIgnore_ba5a64f80ce3db2ca6ec2bc574c2b011() {
        assertEval("{ f <- function(...,d) { ..1 + ..2 } ; f(1,d=4,2) }");
    }

    @Ignore
    public void TestSimpleFunctions_testDotsIgnore_ccfd3930d86a89add4a6dbc2941c216e() {
        assertEval("{ f <- function(...,d) { ..1 + ..2 } ; f(1,2,d=4) }");
    }

    @Ignore
    public void TestSimpleFunctions_testDotsIgnore_a3678db1544ef8395deec4ed02acdb3d() {
        assertEvalError("{ g <- function(a,b,x) { a + b * x } ; f <- function(...) { g(x=4, ..., 10) }  ; f(b=1,a=2) }");
    }

    @Ignore
    public void TestSimpleFunctions_testDotsIgnore_67eac84ba5b2dac0c1bc9214053b228c() {
        assertEvalError("{ f <- function(...) { ..3 } ; f(1,2) }");
    }

    @Ignore
    public void TestSimpleFunctions_testDotsIgnore_452f05dd561690c47f4f03db94d54b6b() {
        assertEvalError("{ f <- function() { dummy() } ; f() }");
    }

    @Ignore
    public void TestSimpleFunctions_testDotsIgnore_20c4c3aa63da2253e51ef2c5ba9d4a1b() {
        assertEvalError("{ f <- function() { if (FALSE) { dummy <- 2 } ; dummy() } ; f() }");
    }

    @Ignore
    public void TestSimpleFunctions_testDotsIgnore_8645241807f4b8810f69603e0858ef16() {
        assertEvalError("{ f <- function() { if (FALSE) { dummy <- 2 } ; g <- function() { dummy() } ; g() } ; f() }");
    }

    @Ignore
    public void TestSimpleFunctions_testDotsIgnore_edee5dc6f81e51ce659c0f3a2fb21571() {
        assertEvalError("{ f <- function() { dummy <- 2 ; g <- function() { dummy() } ; g() } ; f() }");
    }

    @Ignore
    public void TestSimpleFunctions_testDotsIgnore_c3c566ad3a1f22872c3f310db5ae8933() {
        assertEvalError("{ f <- function() { dummy() } ; dummy <- 2 ; f() }");
    }

    @Ignore
    public void TestSimpleFunctions_testDotsIgnore_5606499974e8a959bd2e5a755f7832c8() {
        assertEvalError("{ dummy <- 2 ; dummy() }");
    }

    @Ignore
    public void TestSimpleFunctions_testDotsIgnore_76837b302e412d60cdec11289bac184b() {
        assertEvalError("{ lapply(1:3, \"dummy\") }");
    }

    @Ignore
    public void TestSimpleFunctions_testDotsIgnore_27d8843efbecef3fd6ae84611b61cdff() {
        assertEvalError("{ f <- function(a, b) { a + b } ; g <- function(...) { f(a=1, ...) } ; g(a=2) }");
    }

    @Ignore
    public void TestSimpleFunctions_testDotsIgnore_7f8c80886bf192821872b6edd793baf2() {
        assertEvalError("{ f <- function(a, barg, bextra) { a + barg } ; g <- function(...) { f(a=1, ...) } ; g(b=2,3) }");
    }

    @Ignore
    public void TestSimpleFunctions_testDotsIgnore_997c167046500987d88720745d0018c2() {
        assertEvalError("{ f <- function(a, barg, bextra, dummy) { a + barg } ; g <- function(...) { f(a=1, ...) } ; g(be=2,bex=3, 3) }");
    }

    @Ignore
    public void TestSimpleFunctions_testDotsIgnore_601a671e48fcffae9a23e5b3466aa324() {
        assertEvalError("{ f <- function(a, barg, bextra, dummy) { a + barg } ; g <- function(...) { f(a=1, ..., x=2) } ; g(1) }");
    }

    @Ignore
    public void TestSimpleFunctions_testDotsIgnore_c42cdbf8980cb24618b0e81c71c76f87() {
        assertEvalError("{ f <- function(a, barg, bextra, dummy) { a + barg } ; g <- function(...) { f(a=1, ..., x=2,z=3) } ; g(1) }");
    }

    @Ignore
    public void TestSimpleFunctions_testDotsIgnore_673e885ab1ad8a737dbc0b05d6a34eed() {
        assertEvalError("{ f <- function(a, barg, bextra, dummy) { a + barg } ; g <- function(...) { f(a=1, ..., xxx=2) } ; g(1) }");
    }

    @Ignore
    public void TestSimpleFunctions_testDotsIgnore_4ef97fc6760900dfba4abef33ebb3620() {
        assertEvalError("{ f <- function(a, barg, bextra, dummy) { a + barg } ; g <- function(...) { f(a=1, xxx=2, ...) } ; g(1) }");
    }

    @Ignore
    public void TestSimpleFunctions_testDotsIgnore_3df181a7e78ef23b092f1aba322bbfa1() {
        assertEvalError("{ f <- function(a, barg, bextra, dummy) { a + barg } ; g <- function(...) { f(a=1, ...,,,) } ; g(1) }");
    }

    @Ignore
    public void TestSimpleFunctions_testDotsIgnore_abcc928e40684f62d0ad26ee2f35b057() {
        assertEvalError("{ f <- function(...) { ..2 + ..2 } ; f(1,,2) }");
    }

    @Ignore
    public void TestSimpleFunctions_testDotsIgnore_408a647f1319d8f5216323761b223a47() {
        assertEvalError("{ f <- function(...) { ..1 + ..2 } ; f(1,,3) }");
    }

    @Ignore
    public void TestSimpleFunctions_testErrors_97c1046334e0c7a03ba92803615fccd6() {
        assertEvalError("{ x<-function(){1} ; x(y=1) }");
    }

    @Ignore
    public void TestSimpleFunctions_testErrors_e45fc91400caff4d8a5596ec8cd2edfc() {
        assertEvalError("{ x<-function(y, b){1} ; x(y=1, 2, 3, z = 5) }");
    }

    @Ignore
    public void TestSimpleFunctions_testErrors_e8fd77ad56a4fc8e254f827faed5c973() {
        assertEvalError("{ x<-function(){1} ; x(1) }");
    }

    @Ignore
    public void TestSimpleFunctions_testErrors_9c686da74e6a9bfda861ec6e834613e8() {
        assertEvalError("{ x<-function(a){1} ; x(1,) }");
    }

    @Ignore
    public void TestSimpleFunctions_testErrors_423440c018b8f580500bc17469c52cb8() {
        assertEvalError("{ x<-function(){1} ; x(y=sum(1:10)) }");
    }

    @Ignore
    public void TestSimpleFunctions_testErrors_483a6566dbfd75258c3c09b229efb70b() {
        assertEvalError("{ f <- function(x) { x } ; f() }");
    }

    @Ignore
    public void TestSimpleFunctions_testErrors_da6b1096c4e55e8bb4ac7400d7e63552() {
        assertEvalError("{ x<-function(y,b){1} ; x(y=1,y=3,4) }");
    }

    @Ignore
    public void TestSimpleFunctions_testErrors_3e920d36beba426178bb6e2c548151b7() {
        assertEvalError("{ x<-function(foo,bar){foo*bar} ; x(fo=10,f=1,2) }");
    }

    @Ignore
    public void TestSimpleFunctions_testErrors_1f3190100b071debf5b11ed7f2fae959() {
        assertEvalError("{ f <- function(a,a) {1} }");
    }

    @Ignore
    public void TestSimpleFunctions_testErrors_bf29c1dae99e04f8cd11a340f54e1287() {
        assertEvalError("{ f <- function(a,b,c,d) { a + b } ; f(1,x=1,2,3,4) }");
    }

    @Ignore
    public void TestSimpleFunctions_testPromisesIgnore_c7558b8584a0a8c1dff6c7ee5575ab52() {
        assertEval("{ f <- function(x = z) { z = 1 ; x } ; f() }");
    }

    @Ignore
    public void TestSimpleFunctions_testPromisesIgnore_b817867bec89270f00c9820b107edd80() {
        assertEval("{ z <- 1 ; f <- function(c = z) {  z <- z + 1 ; c  } ; f() }");
    }

    @Ignore
    public void TestSimpleFunctions_testPromisesIgnore_0782b9c8b5990e31ca5d45f3d355ad83() {
        assertEval("{ f <- function(a) { g <- function(b) { x <<- 2; b } ; g(a) } ; x <- 1 ; f(x) }");
    }

    @Ignore
    public void TestSimpleFunctions_testPromisesIgnore_9a98faefce072c525121fc846528b144() {
        assertEval("{ f <- function(a) { g <- function(b) { a <<- 3; b } ; g(a) } ; x <- 1 ; f(x) }");
    }

    @Ignore
    public void TestSimpleFunctions_testPromisesIgnore_f502212c6a9fc0404104e3f44f29d926() {
        assertEval("{ f <- function(x) { function() {x} } ; a <- 1 ; b <- f(a) ; a <- 10 ; b() }");
    }

    @Ignore
    public void TestSimpleFunctions_testPromisesIgnore_1d4e596e32ad6ce14263c2861138bb44() {
        assertEvalError("{ f <- function(x = y, y = x) { y } ; f() }");
    }

    @Ignore
    public void TestSimpleFunctions_testReturnIgnore_ea86042d5ec0a9de6c14aabc98049cf0() {
        assertEval("{ f<-function() { return(invisible(2)) } ; f() }");
    }

    @Ignore
    public void TestSimpleIfEvaluator_testIfDanglingElseIgnore_d73be7d76c1d5f7720c73594824df7ea() {
        assertEvalNoOutput("if(FALSE) if (FALSE) 1 else 2");
    }

    @Ignore
    public void TestSimpleIfEvaluator_testIfWithoutElseIgnore_a1e01cf7b16f44e54f53f0bd7b7d4712() {
        assertEvalNoOutput("if(FALSE) 1");
    }

    @Ignore
    public void TestSimpleLists_testListArgumentEvaluation_f62339e36ed620e527abf492790cea00() {
        assertEval("{ a <- c(0,0,0) ; f <- function() { g <- function() { a[2] <<- 9 } ; g() } ; u <- function() { a <- c(1,1,1) ; f() ; a } ; list(a,u()) }");
    }

    @Ignore
    public void TestSimpleLoop_testDynamic_f61782f946510fe4afa8081fcbdd8fb1() {
        assertEval("{ l <- quote({x <- 0 ; for(i in 1:10) { x <- x + i } ; x}) ; f <- function() { eval(l) } ; x <<- 10 ; f() }");
    }

    @Ignore
    public void TestSimpleLoop_testLoops3_e52a6f4007d0a090db2f28b255bf413a() {
        assertEval("{ l <- quote({for(i in c(1,2)) { x <- i } ; x }) ; f <- function() { eval(l) } ; f() }");
    }

    @Ignore
    public void TestSimpleLoop_testLoops3_6548e4ec40613c3fef7af0ad99e9633e() {
        assertEval("{ l <- quote(for(i in s) { x <- i }) ; s <- 1:3 ; eval(l) ; s <- 2:1 ; eval(l) ; x }");
    }

    @Ignore
    public void TestSimpleLoop_testLoops3_46a097a0af4ffe6e8077dcbe5e4430e0() {
        assertEval("{ l <- quote({for(i in c(2,1)) { x <- i } ; x }) ; f <- function() { if (FALSE) i <- 2 ; eval(l) } ; f() }");
    }

    @Ignore
    public void TestSimpleLoop_testLoops3_569178ca1ef4a4eb52481f6da3753a5a() {
        assertEval("{ l <- quote(for(i in s) { x <- i }) ; s <- 1:3 ; eval(l) ; s <- NULL ; eval(l) ; x }");
    }

    @Ignore
    public void TestSimpleLoop_testLoops3_05c2bfcd5008d009fec146738755dac8() {
        assertEval("{ l <- quote({ for(i in c(1,2,3,4)) { if (i == 1) { next } ; if (i==3) { break } ; x <- i ; if (i==4) { x <- 10 } } ; x }) ; f <- function() { eval(l) } ; f()  }");
    }

    @Ignore
    public void TestSimpleLoop_testLoops3_ea11d8de89669a91c43b8a2985aaf4a0() {
        assertEval("{ l <- quote({ for(i in 1:4) { if (i == 1) { next } ; if (i==3) { break } ; x <- i ; if (i==4) { x <- 10 } } ; x }) ; f <- function() { eval(l) } ; f()  }");
    }

    @Ignore
    public void TestSimpleLoop_testLoopsErrorsIgnore_f394e8f19fc73574a5c55ba7f8e03973() {
        assertEvalError("{ l <- quote(for(i in s) { x <- i }) ; s <- 1:3 ; eval(l) ; s <- function(){} ; eval(l) ; x }");
    }

    @Ignore
    public void TestSimpleLoop_testLoopsErrorsIgnore_2b1a508671083a1b18d0ddb3fe0979c2() {
        assertEvalError("{ l <- function(s) { for(i in s) { x <- i } ; x } ; l(1:3) ; s <- function(){} ; l(s) ; x }");
    }

    @Ignore
    public void TestSimpleLoop_testLoopsErrorsIgnore_eb72a8fa37e3e5c2ac10481c6173a724() {
        assertEvalError("{ l <- quote({ for(i in s) { x <- i } ; x }) ; f <- function(s) { eval(l) } ; f(1:3) ; s <- function(){} ; f(s) ; x }");
    }

    @Ignore
    public void TestSimpleLoop_testOneIterationLoops_2b49e8a8d835c688af57e7939698d86a() {
        assertEvalNoNL("{ for (a in 1) cat(a) }");
    }

    @Ignore
    public void TestSimpleLoop_testOneIterationLoops_d16fcada6748f3bb2cf6eb7647ccd86f() {
        assertEvalNoNL("{ for (a in 1L) cat(a) }");
    }

    @Ignore
    public void TestSimpleLoop_testOneIterationLoops_133be12813e36ebfe9c2af618ab288c8() {
        assertEvalNoNL("{ for (a in \"xyz\") cat(a) }");
    }

    @Ignore
    public void TestSimpleSequences_testSequenceConstructionIgnore_b9324a4b0cb6cce5fbe2323872e18705() {
        assertEvalWarning("{ (1:3):3 }");
    }

    @Ignore
    public void TestSimpleTruffle_test1Ignore_3ec182256a363ba8d70350f6d949593b() {
        assertEvalNoOutput("{ f<-function(i) { if(i==1) { i } } ; f(1) ; f(2) }");
    }

    @Ignore
    public void TestSimpleTruffle_test1Ignore_6b932d60711336223d0b7667e5e39f6d() {
        assertEvalNoOutput("{ f<-function() { if (!1) TRUE } ; f(); f() }");
    }

    @Ignore
    public void TestSimpleTruffle_test1Ignore_97edc61479ed325f8e463f75a53a34d4() {
        assertEvalNoOutput("{ f<-function() { if (!TRUE) 1 } ; f(); f() }");
    }

    @Ignore
    public void TestSimpleTruffle_test1Ignore_71c46963de35ffad054f0a585f749a4f() {
        assertEvalNoOutput("{ f<-function(i) { if (FALSE) { i } } ; f(2) ; f(1) }");
    }

    @Ignore
    public void TestSimpleVectors_testMoreVectorsOtherIgnore_461a050f655ae44ddfc5d11f6a011e93() {
        assertEvalError("{ x<-1:4; x[[1]]<-NULL; x }");
    }

    @Ignore
    public void TestSimpleVectors_testMoreVectorsOtherIgnore_5a63039e693f0bcdb40f33a133932ebd() {
        assertEvalError("{ x<-1:4; x[[0]]<-NULL; x }");
    }

    @Ignore
    public void TestSimpleVectors_testMoreVectorsOtherIgnore_3b27f8602ed093e9302f1ed670a155cf() {
        assertEvalError("{ b<-3:5; dim(b) <- c(1,3) ; b[[c(1)]] <- NULL ; b }");
    }

    @Ignore
    public void TestSimpleVectors_testMoreVectorsOtherIgnore_4b570d690c92236829b8974bae01fe3e() {
        assertEvalError("{ b<-3:5; dim(b) <- c(1,3) ; b[[0]] <- NULL ; b }");
    }

    @Ignore
    public void TestSimpleVectors_testMoreVectorsOtherIgnore_fe5461bdd4035e24804d4c684b9bb20f() {
        assertEvalError("{ x <- integer() ; x[[NA]] <- NULL ; x }");
    }

    @Ignore
    public void TestSimpleVectors_testMoreVectorsOtherIgnore_f09061c93f11ca4a2ec5ecd4f85f7548() {
        assertEvalError("{ x <- c(1) ; x[[NA]] <- NULL ; x }");
    }

    @Ignore
    public void TestSimpleVectors_testMoreVectorsOtherIgnore_4eea10efa7dfc459fce3420e5cf8d9fc() {
        assertEvalError("{ x <- c(1,2) ; x[[NA]] <- NULL ; x }");
    }

    @Ignore
    public void TestSimpleVectors_testMoreVectorsOtherIgnore_ad453b3eec6a2d91d42ea4c78a0c9356() {
        assertEvalError("{ x <- c(1,2,3) ; x[[NA]] <- NULL ; x }");
    }

    @Ignore
    public void TestSimpleVectors_testMoreVectorsOtherIgnore_d5bbba1f1bb5b771dbc80175679415c5() {
        assertEvalError("{ x<-c(1,2,3,4); dim(x)<-c(2,2); x[[1]]<-NULL; x }");
    }

    @Ignore
    public void TestSimpleVectors_testScalarUpdateIgnore_ea7d1aaf03e73608bdd0d9114c96e3a8() {
        assertEvalError("{ f <- function(b,v) { b[[2]] <- v ; b } ; f(c(\"a\",\"b\"),\"d\") ; f(c(\"a\",\"b\"),NULL) }");
    }

    @Ignore
    public void TestSimpleVectors_testScalarUpdateIgnore_5d198ef5c0421165963dc6da0d622857() {
        assertEvalError("{ x <- 4:10 ; x[[\"z\"]] <- NULL ; x }");
    }

    @Ignore
    public void TestSimpleVectors_testVectorUpdateIgnore_4bb6389721e2adbd8f6b69aa42e80569() {
        assertEval("{ x<-1:5 ; x[x[4]<-2] <- (x[4]<-100) ; x }");
    }

    @Ignore
    public void TestSimpleVectors_testVectorUpdateIgnore_09e16a78eb04d58e35b4c9045cbc0acb() {
        assertEval("{ x<-5:1 ; x[x[2]<-2] }");
    }

}

