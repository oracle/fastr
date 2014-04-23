// DO NOT EDIT, update using 'mx rignoredtests'
// This contains a copy of the @Test tests one micro-test per method
package com.oracle.truffle.r.test.all;

import org.junit.Test;

import com.oracle.truffle.r.test.*;

//Checkstyle: stop
public class AllTests extends TestBase {
    @Test
    public void TestSimpleArithmetic_testArithmeticUpdate_53dd62f0f4ee11cdf35cbec8ec41f7c8() {
        assertEval("{ x <- 3 ; f <- function(z) { if (z) { x <- 1 } ; x <- x + 1L ; x } ; f(FALSE) }");
    }

    @Test
    public void TestSimpleArithmetic_testArithmeticUpdate_7c11cc002b58eebf858e56da622b7816() {
        assertEval("{ x <- 3 ; f <- function(z) { if (z) { x <- 1 } ; x <- 1L + x ; x } ; f(FALSE) }");
    }

    @Test
    public void TestSimpleArithmetic_testArithmeticUpdate_e4e9b5e68f5febfd8848a72f10d818be() {
        assertEval("{ x <- 3 ; f <- function(z) { if (z) { x <- 1 } ; x <- x - 1L ; x } ; f(FALSE) }");
    }

    @Test
    public void TestSimpleArithmetic_testComplexNaNInfinity_8124186154226341ba9cef3e8816cbaa() {
        assertEval("{ 0^(-1+1i) }");
    }

    @Test
    public void TestSimpleArithmetic_testComplexNaNInfinity_6c5ffca629fa2d7d2b1412f6621a46a1() {
        assertEval("{ (0+0i)/(0+0i) }");
    }

    @Test
    public void TestSimpleArithmetic_testComplexNaNInfinity_5d534dca1c2d0275dbaa22b12e0b4f2e() {
        assertEval("{ (1+0i)/(0+0i) }");
    }

    @Test
    public void TestSimpleArithmetic_testComplexNaNInfinity_58576b74c634ea951bc7f90f7e2a97f2() {
        assertEval("{ (0+1i)/(0+0i) }");
    }

    @Test
    public void TestSimpleArithmetic_testComplexNaNInfinity_1a10642f746450640a45c01c34a6a318() {
        assertEval("{ (1+1i)/(0+0i) }");
    }

    @Test
    public void TestSimpleArithmetic_testComplexNaNInfinity_efde6bf8de686595a1aa36aea46c5828() {
        assertEval("{ (-1+0i)/(0+0i) }");
    }

    @Test
    public void TestSimpleArithmetic_testComplexNaNInfinity_1c9c59138fc580458ed35848f74c4a78() {
        assertEval("{ (-1-1i)/(0+0i) }");
    }

    @Test
    public void TestSimpleArithmetic_testComplexNaNInfinity_ea15462c9b7c011c1d945736e3a3dcf8() {
        assertEval("{ (1+2i) / ((0-0i)/(0+0i)) }");
    }

    @Test
    public void TestSimpleArithmetic_testComplexNaNInfinity_675d9cf5c53725282f0c294743ce6b39() {
        assertEval("{ ((0+1i)/0) * ((0+1i)/0) }");
    }

    @Test
    public void TestSimpleArithmetic_testComplexNaNInfinity_0ae6d7ac55927a259d69f7f03a4542e3() {
        assertEval("{ ((0-1i)/0) * ((0+1i)/0) }");
    }

    @Test
    public void TestSimpleArithmetic_testComplexNaNInfinity_27f92a8a7da750230f55859308842034() {
        assertEval("{ ((0-1i)/0) * ((0-1i)/0) }");
    }

    @Test
    public void TestSimpleArithmetic_testComplexNaNInfinity_2afee5796d3c017f5bb957d5553f5800() {
        assertEval("{ ((0-1i)/0) * ((1-1i)/0) }");
    }

    @Test
    public void TestSimpleArithmetic_testComplexNaNInfinity_b8ac47096347e664f052f76de05fe994() {
        assertEval("{ ((0-1i)/0) * ((-1-1i)/0) }");
    }

    @Test
    public void TestSimpleArithmetic_testComplexNaNInfinity_3bb94c09c10232c654e3a8d73caa236d() {
        assertEval("{ 0/0 - 4i }");
    }

    @Test
    public void TestSimpleArithmetic_testComplexNaNInfinity_413b4efc28ed020519ac2305176225ff() {
        assertEval("{ 4i + 0/0  }");
    }

    @Test
    public void TestSimpleArithmetic_testComplexNaNInfinity_4bc46d908fc18069dc1921591018a438() {
        assertEval("{ a <- 1 + 2i; b <- 0/0 - 4i; a + b }");
    }

    @Test
    public void TestSimpleArithmetic_testExponentiation_f17675ff294c6d47a0d09cb3db0bdf2a() {
        assertEval("{ 1^(1/0) }");
    }

    @Test
    public void TestSimpleArithmetic_testExponentiation_4710d67f4bd1982f8c40202ed5307dce() {
        assertEval("{ (-2)^(1/0) }");
    }

    @Test
    public void TestSimpleArithmetic_testExponentiation_6d863a43b4b431014a0dcbda198c0af8() {
        assertEval("{ (-2)^(-1/0) }");
    }

    @Test
    public void TestSimpleArithmetic_testExponentiation_504ab1a6fda5fb22ee8cf23fe4624f81() {
        assertEval("{ (1)^(-1/0) }");
    }

    @Test
    public void TestSimpleArithmetic_testExponentiation_1bb4c11ec62cdf615b04b1dcb3185cfb() {
        assertEval("{ 0^(-1/0) }");
    }

    @Test
    public void TestSimpleArithmetic_testExponentiation_428f96d84c85cc65662e08ab2d77d1d1() {
        assertEval("{ 0^(1/0) }");
    }

    @Test
    public void TestSimpleArithmetic_testExponentiation_924b9c012d964293a4383a9399ee6a83() {
        assertEval("{ 0^(0/0) }");
    }

    @Test
    public void TestSimpleArithmetic_testExponentiation_cb129d707887df32a9b72c721dbf5199() {
        assertEval("{ 1^(0/0) }");
    }

    @Test
    public void TestSimpleArithmetic_testExponentiation_f947c668e5820532d6f52ca88e810da3() {
        assertEval("{ (-1)^(0/0) }");
    }

    @Test
    public void TestSimpleArithmetic_testExponentiation_f290cee9e92be8587a7b24f137183d2b() {
        assertEval("{ (-1/0)^(0/0) }");
    }

    @Test
    public void TestSimpleArithmetic_testExponentiation_1d4363eb048e63be2bd9139289c6b738() {
        assertEval("{ (1/0)^(0/0) }");
    }

    @Test
    public void TestSimpleArithmetic_testExponentiation_c3cce60bf627dc8bd18806a13df87aae() {
        assertEval("{ (0/0)^(1/0) }");
    }

    @Test
    public void TestSimpleArithmetic_testExponentiation_7fdec89de71f828679d99068de8a70e8() {
        assertEval("{ (-1/0)^3 }");
    }

    @Test
    public void TestSimpleArithmetic_testExponentiation_3245c9f71d4a8423326955c12aabd2a4() {
        assertEval("{ (1/0)^(-4) }");
    }

    @Test
    public void TestSimpleArithmetic_testExponentiation_611e93b0981944cb54f20b5a79b1bce4() {
        assertEval("{(-1/0)^(-4) }");
    }

    @Test
    public void TestSimpleArithmetic_testIntegerDivision_3b1bbf0ad36002ef273d079f5390abdb() {
        assertEval("{ 3 %/% 2 }");
    }

    @Test
    public void TestSimpleArithmetic_testIntegerDivision_88024b2c4c54a96e1833b57b07471f45() {
        assertEval("{ 3L %/% 2L }");
    }

    @Test
    public void TestSimpleArithmetic_testIntegerDivision_4c13d6cec19707fc289ad9b95ff6d6eb() {
        assertEval("{ 3L %/% -2L }");
    }

    @Test
    public void TestSimpleArithmetic_testIntegerDivision_0e5eed1178d5b020dbbdc5fe357f700f() {
        assertEval("{ 3 %/% -2 }");
    }

    @Test
    public void TestSimpleArithmetic_testIntegerDivision_eaa376f24e148a6538d9df85851782c4() {
        assertEval("{ 3 %/% 0 }");
    }

    @Test
    public void TestSimpleArithmetic_testIntegerOverflow_e56646664ed3ccebd0b978a474ccae3c() {
        assertEvalWarning("{ x <- 2147483647L ; x + 1L }");
    }

    @Test
    public void TestSimpleArithmetic_testIntegerOverflow_9cc3316d11cb57fdb9d71e833e43dcd6() {
        assertEvalWarning("{ x <- 2147483647L ; x * x }");
    }

    @Test
    public void TestSimpleArithmetic_testIntegerOverflow_a5d2e40a03d44363ee0bf4afb8a3a70d() {
        assertEvalWarning("{ x <- -2147483647L ; x - 2L }");
    }

    @Test
    public void TestSimpleArithmetic_testIntegerOverflow_52bf15e78c97dfea203e3a3a75c0c096() {
        assertEvalWarning("{ x <- -2147483647L ; x - 1L }");
    }

    @Test
    public void TestSimpleArithmetic_testIntegerOverflow_4a27f3f0ef1c0e73ea1ae4a599818778() {
        assertEvalWarning("{ 2147483647L + 1:3 }");
    }

    @Test
    public void TestSimpleArithmetic_testIntegerOverflow_d17b51eaa8f9d85088d30f7b59888e01() {
        assertEvalWarning("{ 2147483647L + c(1L,2L,3L) }");
    }

    @Test
    public void TestSimpleArithmetic_testIntegerOverflow_a208f558c3d55c2d86aa5cfe699b218a() {
        assertEvalWarning("{ 1:3 + 2147483647L }");
    }

    @Test
    public void TestSimpleArithmetic_testIntegerOverflow_1444a6f9919138380d32057ddfa36eec() {
        assertEvalWarning("{ c(1L,2L,3L) + 2147483647L }");
    }

    @Test
    public void TestSimpleArithmetic_testIntegerOverflow_dd77dbcef3cf523fc2aa46c4c0deaf5c() {
        assertEvalWarning("{ 1:3 + c(2147483647L,2147483647L,2147483647L) }");
    }

    @Test
    public void TestSimpleArithmetic_testIntegerOverflow_72a5d4dd67ed5a21396516c0968edf6e() {
        assertEvalWarning("{ c(2147483647L,2147483647L,2147483647L) + 1:3 }");
    }

    @Test
    public void TestSimpleArithmetic_testIntegerOverflow_6fd7e6825d4c56f715061fbb7124628a() {
        assertEvalWarning("{ c(1L,2L,3L) + c(2147483647L,2147483647L,2147483647L) }");
    }

    @Test
    public void TestSimpleArithmetic_testIntegerOverflow_6b9734a08caf45fad14bde9d7b10a97c() {
        assertEvalWarning("{ c(2147483647L,2147483647L,2147483647L) + c(1L,2L,3L) }");
    }

    @Test
    public void TestSimpleArithmetic_testIntegerOverflow_8b60212b3b68acfddf00f22ea65883db() {
        assertEvalWarning("{ 1:4 + c(2147483647L,2147483647L) }");
    }

    @Test
    public void TestSimpleArithmetic_testIntegerOverflow_ffe1faa265bec1af2b8c1f1c4d9fc343() {
        assertEvalWarning("{ c(2147483647L,2147483647L) + 1:4 }");
    }

    @Test
    public void TestSimpleArithmetic_testIntegerOverflow_8894beb2d0cbaf7303c2efa930d6684b() {
        assertEvalWarning("{ c(1L,2L,3L,4L) + c(2147483647L,2147483647L) }");
    }

    @Test
    public void TestSimpleArithmetic_testIntegerOverflow_68d4c3db613629f473aa7128bff2c5a8() {
        assertEvalWarning("{ c(2147483647L,2147483647L) + c(1L,2L,3L,4L) }");
    }

    @Test
    public void TestSimpleArithmetic_testIntegerOverflowNoWarning_a3ab4fef386bc64ef22475c67e0ffa13() {
        assertEval("{ 3L %/% 0L }");
    }

    @Test
    public void TestSimpleArithmetic_testIntegerOverflowNoWarning_046ca027eee4285bccdc8a4eebb20ab2() {
        assertEval("{ 3L %% 0L }");
    }

    @Test
    public void TestSimpleArithmetic_testIntegerOverflowNoWarning_7a89533aae8626464e0426511cbfc211() {
        assertEval("{ c(3L,3L) %/% 0L }");
    }

    @Test
    public void TestSimpleArithmetic_testIntegerOverflowNoWarning_d9e8981d1a9430da45fae8c5a53f8184() {
        assertEval("{ c(3L,3L) %% 0L }");
    }

    @Test
    public void TestSimpleArithmetic_testMatrices_1e3c62e0c2fe6df45e9682661c296c9e() {
        assertEval("{ m <- matrix(1:6, nrow=2, ncol=3, byrow=TRUE) ; m-1 }");
    }

    @Test
    public void TestSimpleArithmetic_testMatrices_5e7a942b4dd16d7b452ea0befe6c79b3() {
        assertEval("{ z<-matrix(12)+1 ; z }");
    }

    @Test
    public void TestSimpleArithmetic_testMatricesIgnore_6501297c30346fab1b5e3cb8243d733e() {
        assertEval("{ m <- matrix(1:6, nrow=2, ncol=3, byrow=TRUE) ; m+1L }");
    }

    @Test
    public void TestSimpleArithmetic_testMatricesIgnore_20ecb09faabed8eebd8838ad5e84bd30() {
        assertEval("{ m <- matrix(1:6, nrow=2, ncol=3, byrow=TRUE) ; m+m }");
    }

    @Test
    public void TestSimpleArithmetic_testMatricesOuterProduct_10f665dc31dc8de98c87bb9b2603e3ac() {
        assertEval("{ 1:3 %o% 1:2 }");
    }

    @Test
    public void TestSimpleArithmetic_testMatricesOuterProduct_b4ef8ce1b5d0ec522e1700961204a7fe() {
        assertEval("{ 1:3 %*% c(TRUE,FALSE,TRUE) }");
    }

    @Test
    public void TestSimpleArithmetic_testMatricesOuterProduct_37728a54ca59a17f41ea4c8e909975fa() {
        assertEvalError("{ 1:4 %*% 1:3 }");
    }

    @Test
    public void TestSimpleArithmetic_testMatricesOuterProduct_35e59fddf166a877e6189099ee6fb1fa() {
        assertEvalError("{ 1:3 %*% as.raw(c(1,2,3)) }");
    }

    @Test
    public void TestSimpleArithmetic_testMatricesOuterProduct_a09ecf2d4e23f8197f1193edacadb395() {
        assertEvalError("{ as.raw(1:3) %o% 1:3 }");
    }

    @Test
    public void TestSimpleArithmetic_testMatricesPrecedence_2ffbf2ffcec6699d78e24fb25ef01c31() {
        assertEval("{ 10 / 1:3 %*% 3:1 }");
    }

    @Test
    public void TestSimpleArithmetic_testMatricesPrecedence_535ad26d0ae2d6aebb3f6b8ee7202d6b() {
        assertEval("{ x <- 1:2 ; dim(x) <- c(1,1,2) ; y <- 2:3 ; dim(y) <- c(1,1,2) ; x + y }");
    }

    @Test
    public void TestSimpleArithmetic_testMatricesProduct_8ca5705f5446845cbed42dd449fc7c06() {
        assertEval("{ double() %*% double() }");
    }

    @Test
    public void TestSimpleArithmetic_testMatricesProduct_af557b2850b93c6f969d8bbddeda060f() {
        assertEval("{ m <- double() ; dim(m) <- c(0,4) ; m %*% t(m) }");
    }

    @Test
    public void TestSimpleArithmetic_testMatricesProduct_f85f22d8857748f52317e561e2718ae3() {
        assertEval("{ m <- double() ; dim(m) <- c(0,4) ; t(m) %*% m }");
    }

    @Test
    public void TestSimpleArithmetic_testMatricesProduct_f1ed21950d9811ec9ce279909f8ac20a() {
        assertEval("{ m <- double() ; dim(m) <- c(0,4) ; n <- matrix(1:4,4) ; m %*% n }");
    }

    @Test
    public void TestSimpleArithmetic_testMatricesProduct_a0b87c9b473a105c31dbe9f024f1229f() {
        assertEval("{ m <- double() ; dim(m) <- c(4,0) ; n <- matrix(1:4,ncol=4) ; n %*% m }");
    }

    @Test
    public void TestSimpleArithmetic_testMatricesProductIgnore_2d6353b38e2b880f487de091cada51de() {
        assertEval("{ x <- 1:3 %*% 9:11 ; x[1] }");
    }

    @Test
    public void TestSimpleArithmetic_testMatricesProductIgnore_24bd08e2ca37ab0518a7581f2f50ddee() {
        assertEval("{ m<-matrix(1:3, nrow=1) ; 1:2 %*% m }");
    }

    @Test
    public void TestSimpleArithmetic_testMatricesProductIgnore_5ec0a442914239f245360029a38d268d() {
        assertEval("{ m<-matrix(1:6, nrow=2) ; 1:2 %*% m }");
    }

    @Test
    public void TestSimpleArithmetic_testMatricesProductIgnore_d0711a4730cdf02f5a3b8f72241f1e4b() {
        assertEval("{ m<-matrix(1:6, nrow=2) ; m %*% 1:3 }");
    }

    @Test
    public void TestSimpleArithmetic_testMatricesProductIgnore_863b40edd577576cae70ebf3e7bfced1() {
        assertEval("{ m<-matrix(1:3, ncol=1) ; m %*% 1:2 }");
    }

    @Test
    public void TestSimpleArithmetic_testMatricesProductIgnore_5ff07900e3894985c2c175e2f144e1e5() {
        assertEval("{ a<-matrix(1:6, ncol=2) ; b<-matrix(11:16, nrow=2) ; a %*% b }");
    }

    @Test
    public void TestSimpleArithmetic_testMatricesProductIgnore_fcf07ee50d1cec625af66d876a3ade31() {
        assertEval("{ a <- array(1:9, dim=c(3,1,3)) ;  a %*% 1:9 }");
    }

    @Test
    public void TestSimpleArithmetic_testMatricesProductIgnore_caeff71e032666868b80c5661b0aedbf() {
        assertEval("{ m <- matrix(c(1,2,3,0/0), nrow=4) ; m %*% 1:4 }");
    }

    @Test
    public void TestSimpleArithmetic_testMatricesProductIgnore_70b2522d5c0789b448aaa4e42bcb041d() {
        assertEval("{ m <- matrix(c(NA,1,0/0,2), nrow=2) ; 1:2 %*% m }");
    }

    @Test
    public void TestSimpleArithmetic_testMatricesProductIgnore_54299d1df8e5f86b0085b241c557b2db() {
        assertEval("{ m <- double() ; dim(m) <- c(0,0) ; m %*% m }");
    }

    @Test
    public void TestSimpleArithmetic_testMatricesProductIgnore_1ec6885e784fa47096ed05a88cc80f60() {
        assertEval("{ m <- matrix(c(NA,1,4,2), nrow=2) ; t(m) %*% m }");
    }

    @Test
    public void TestSimpleArithmetic_testMatricesProductIgnore_dcc24f855641426f57aae95814c402ba() {
        assertEval("{ matrix(c(3,1,0/0,2), nrow=2) %*% matrix(1:6,nrow=2) }");
    }

    @Test
    public void TestSimpleArithmetic_testMatricesProductIgnore_6b9454263a04c54bc733dd613376e166() {
        assertEvalError("{ matrix(2,nrow=2,ncol=3) %*% matrix(4,nrow=1,ncol=5) }");
    }

    @Test
    public void TestSimpleArithmetic_testMatricesProductIgnore_ead68010e962c2d7e87bdca6608f0e53() {
        assertEvalError("{ 1:3 %*% matrix(4,nrow=2,ncol=5) }");
    }

    @Test
    public void TestSimpleArithmetic_testMatricesProductIgnore_b920cd3ed3cec15b725979c29716cbaf() {
        assertEvalError("{ matrix(4,nrow=2,ncol=5) %*% 1:4 }");
    }

    @Test
    public void TestSimpleArithmetic_testMatricesProductIgnore_bc12ac5a1ffe6af2ea4fd50c117b9c64() {
        assertEvalError("{ as.raw(1:3) %*% 1:3 }");
    }

    @Test
    public void TestSimpleArithmetic_testModulo_202573a7f4d94dd371f035e6320682ba() {
        assertEval("{ 3 %% 2 }");
    }

    @Test
    public void TestSimpleArithmetic_testModulo_9bb9e1239536cbde22cebb969dcb45cf() {
        assertEval("{ 3L %% 2L }");
    }

    @Test
    public void TestSimpleArithmetic_testModulo_9e38b0a757946bb29595e4d1088a2e66() {
        assertEval("{ 3L %% -2L }");
    }

    @Test
    public void TestSimpleArithmetic_testModulo_06b718e5ce5ae46c4a411a09da74d9f0() {
        assertEval("{ 3 %% -2 }");
    }

    @Test
    public void TestSimpleArithmetic_testModulo_580af7a9bdcb8715462ed7eed121a9ba() {
        assertEval("{ 3 %% 0 }");
    }

    @Test
    public void TestSimpleArithmetic_testNonvectorizedLogicalAnd_cff9c3d9b0fd8cc5fbcf3bc968b56d36() {
        assertEval("{ TRUE && FALSE }");
    }

    @Test
    public void TestSimpleArithmetic_testNonvectorizedLogicalAnd_05930136c0a595320cf48c26498946d7() {
        assertEval("{ FALSE && FALSE }");
    }

    @Test
    public void TestSimpleArithmetic_testNonvectorizedLogicalAnd_b67dff95a7948a3a53e547752dcae356() {
        assertEval("{ FALSE && TRUE }");
    }

    @Test
    public void TestSimpleArithmetic_testNonvectorizedLogicalAnd_c3ee6db57300925d8ce3d59d3099896d() {
        assertEval("{ TRUE && TRUE }");
    }

    @Test
    public void TestSimpleArithmetic_testNonvectorizedLogicalAnd_21c10a7cda34cd431d95210e1ea170ba() {
        assertEval("{ x <- 1 ; f <- function(r) { x <<- 2; r } ; FALSE && f(FALSE) ; x } ");
    }

    @Test
    public void TestSimpleArithmetic_testNonvectorizedLogicalAnd_0b093f90a0e1ef1a86a958007b96db3b() {
        assertEval("{ x <- 1 ; f <- function(r) { x <<- 2; r } ; c(FALSE, TRUE) && f(FALSE) ; x } ");
    }

    @Test
    public void TestSimpleArithmetic_testNonvectorizedLogicalAnd_1f396564af274610e5be09291c5fc95d() {
        assertEval("{ TRUE && NA }");
    }

    @Test
    public void TestSimpleArithmetic_testNonvectorizedLogicalAnd_97ce6d0e8cb12d9a46f7e74cd0752930() {
        assertEval("{ FALSE && NA }");
    }

    @Test
    public void TestSimpleArithmetic_testNonvectorizedLogicalAnd_0b0cb7f79750321198743bd2ccf4fb92() {
        assertEval("{ NA && TRUE }");
    }

    @Test
    public void TestSimpleArithmetic_testNonvectorizedLogicalAnd_93c19a2cb63dfc748a1394a8143eef3b() {
        assertEval("{ NA && FALSE }");
    }

    @Test
    public void TestSimpleArithmetic_testNonvectorizedLogicalAnd_4eaa032d92546e29feb671e9b97b6dae() {
        assertEval("{ NA && NA }");
    }

    @Test
    public void TestSimpleArithmetic_testNonvectorizedLogicalAnd_ebfdf2708abf9adaa3d7a3227bfbe73a() {
        assertEval("{ x <- 1 ; f <- function(r) { x <<- 2; r } ; NA && f(NA) ; x } ");
    }

    @Test
    public void TestSimpleArithmetic_testNonvectorizedLogicalAnd_b01625d80e4379978afb1d75376cb6eb() {
        assertEval("{ TRUE && c(TRUE, FALSE) }");
    }

    @Test
    public void TestSimpleArithmetic_testNonvectorizedLogicalAnd_551e97c314a1a0c046e7633b5cc84a1f() {
        assertEval("{ c(TRUE, FALSE) && c(TRUE, FALSE) }");
    }

    @Test
    public void TestSimpleArithmetic_testNonvectorizedLogicalAnd_eebd41a5410d062a6b2137d0b634c952() {
        assertEval("{ c(TRUE, FALSE) && c(TRUE, FALSE, FALSE) }");
    }

    @Test
    public void TestSimpleArithmetic_testNonvectorizedLogicalAnd_f4537d57cf8d3df0d2fe212a5d0a3d0e() {
        assertEval("{ c(1.0, 0.0) && 1.0 }");
    }

    @Test
    public void TestSimpleArithmetic_testNonvectorizedLogicalAnd_46227f653f6fce0f79e69d6db085bc60() {
        assertEval("{ c(1, 0) && 1 }");
    }

    @Test
    public void TestSimpleArithmetic_testNonvectorizedLogicalAnd_5afad57a0c523b0dcf5649822c4f8a37() {
        assertEval("{ c(1.1, 0.0) && c(TRUE, FALSE) }");
    }

    @Test
    public void TestSimpleArithmetic_testNonvectorizedLogicalAnd_d0c22acdacf31c2a8f13048f02bde419() {
        assertEval("{ c(1, 0) && 1+1i }");
    }

    @Test
    public void TestSimpleArithmetic_testNonvectorizedLogicalAnd_68b11794f64110f23c2d8271dcff5f34() {
        assertEval("{ c(1+1i, 0+0i) && 1 }");
    }

    @Test
    public void TestSimpleArithmetic_testNonvectorizedLogicalAnd_50b0be4def9e64e8876db0de53d0a0f6() {
        assertEval("{ 1.0 && c(1+1i, 0+0i) }");
    }

    @Test
    public void TestSimpleArithmetic_testNonvectorizedLogicalAnd_0445580e385bed757d863aa9838cfb1a() {
        assertEval("{ c(1+1i, 0+0i) && c(1+1i, 0+0i) }");
    }

    @Test
    public void TestSimpleArithmetic_testNonvectorizedLogicalAnd_4b86f5b4342387abd9e8f9bee52ffba0() {
        assertEvalError("{ c(\"1\", \"0\") && TRUE }");
    }

    @Test
    public void TestSimpleArithmetic_testNonvectorizedLogicalAnd_6decce739d81b593c0049822632f9e12() {
        assertEvalError("{ c(1, 0) && \"1\" }");
    }

    @Test
    public void TestSimpleArithmetic_testNonvectorizedLogicalAnd_458c45be2120f0f097d58673e99fc5d0() {
        assertEvalError("{ \"1\" && c(1, 0) }");
    }

    @Test
    public void TestSimpleArithmetic_testNonvectorizedLogicalAnd_6acb641048da6f23855f1f75fb877993() {
        assertEvalError("{ as.raw(c(1, 0)) && TRUE }");
    }

    @Test
    public void TestSimpleArithmetic_testNonvectorizedLogicalAndAsFunction_7f3ae07c12cb772d001e503b34ab3f33() {
        assertEval("{ f <- function(a,b) { a && b } ;  f(c(TRUE, FALSE), TRUE) }");
    }

    @Test
    public void TestSimpleArithmetic_testNonvectorizedLogicalAndAsFunction_af987d86996028033ce13fb70386754f() {
        assertEval("{ f <- function(a,b) { a && b } ;  f(c(TRUE, FALSE), logical()) }");
    }

    @Test
    public void TestSimpleArithmetic_testNonvectorizedLogicalAndAsFunction_88bd252f9707cc0728933d5a01d5ec36() {
        assertEval("{ f <- function(a,b) { a && b } ;  f(c(TRUE, FALSE), logical()) ; f(1:3,4:10) ; f(1,2) }");
    }

    @Test
    public void TestSimpleArithmetic_testNonvectorizedLogicalAndAsFunction_0d6f9f6d6b5bf8c004bd208c8b88a72e() {
        assertEval("{ f <- function(a,b) { a && b } ;  f(c(TRUE, FALSE), logical()) ; f(1:3,4:10) ; f(double(),2) }");
    }

    @Test
    public void TestSimpleArithmetic_testNonvectorizedLogicalAndAsFunction_c924f78b26ae5bf033f4de08b188383f() {
        assertEval("{ f <- function(a,b) { a && b } ;  f(c(TRUE, FALSE), logical()) ; f(1:3,4:10) ; f(integer(),2) }");
    }

    @Test
    public void TestSimpleArithmetic_testNonvectorizedLogicalAndAsFunction_2a6f592a022116d86a5ec89d04304c16() {
        assertEval("{ f <- function(a,b) { a && b } ;  f(c(TRUE, FALSE), logical()) ; f(1:3,4:10) ; f(2+3i,1/0) }");
    }

    @Test
    public void TestSimpleArithmetic_testNonvectorizedLogicalAndAsFunction_72585e59f431501cf5001dd2c3e50d33() {
        assertEval("{ f <- function(a,b) { a && b } ;  f(c(TRUE, FALSE), logical()) ; f(1:3,4:10) ; f(2+3i,logical()) }");
    }

    @Test
    public void TestSimpleArithmetic_testNonvectorizedLogicalAndAsFunction_852d4ff2e6db41111d0f4c780193e927() {
        assertEval("{ f <- function(a,b) { a && b } ;  f(c(TRUE, FALSE), logical()) ; f(1:3,4:10) ; f(1,2) ; f(logical(),4) }");
    }

    @Test
    public void TestSimpleArithmetic_testNonvectorizedLogicalAndAsFunction_127115bfa7022c79f86704e41e945f5c() {
        assertEval("{ f <- function(a,b) { a && b } ;  f(c(TRUE, FALSE), logical()) ; f(TRUE, c(TRUE,TRUE,FALSE)) ; f(1,2) }");
    }

    @Test
    public void TestSimpleArithmetic_testNonvectorizedLogicalLengthChecks_d490718031714d1bdd340f16c17adb30() {
        assertEvalWarning("{ as.raw(c(1,4)) | as.raw(c(1,5,4)) }");
    }

    @Test
    public void TestSimpleArithmetic_testNonvectorizedLogicalLengthChecks_8ffa31733d993b902cfacd5ad43d3287() {
        assertEvalWarning("{ as.raw(c(1,5,4)) | as.raw(c(1,4)) }");
    }

    @Test
    public void TestSimpleArithmetic_testNonvectorizedLogicalLengthChecks_cff0c7f286d7100c893ceaf4dee9a0c2() {
        assertEvalWarning("{ c(TRUE, FALSE, FALSE) & c(TRUE,TRUE) }");
    }

    @Test
    public void TestSimpleArithmetic_testNonvectorizedLogicalLengthChecks_1ea15db9b7685a8714146e29573cabde() {
        assertEvalWarning("{ c(TRUE, TRUE) & c(TRUE, FALSE, FALSE) }");
    }

    @Test
    public void TestSimpleArithmetic_testNonvectorizedLogicalLengthChecks_a1594db2ddfa1d07731f6c5f92cd9833() {
        assertEvalWarning("{ c(a=TRUE, TRUE) | c(TRUE, b=FALSE, FALSE) }");
    }

    @Test
    public void TestSimpleArithmetic_testNonvectorizedLogicalOr_526f9e209761ca3bbd64718e71e97c92() {
        assertEval("{ 1.1 || 3.15 }");
    }

    @Test
    public void TestSimpleArithmetic_testNonvectorizedLogicalOr_45cfdfc8a595a5ca7def354d7302fa2b() {
        assertEval("{ 0 || 0 }");
    }

    @Test
    public void TestSimpleArithmetic_testNonvectorizedLogicalOr_402100c7062bc903b141b0f04e634db8() {
        assertEval("{ 1 || 0 }");
    }

    @Test
    public void TestSimpleArithmetic_testNonvectorizedLogicalOr_f1db670be5140a3855c81cfab1cdce57() {
        assertEval("{ x <- 1 ; f <- function(r) { x <<- 2; r } ; TRUE || f(FALSE) ; x } ");
    }

    @Test
    public void TestSimpleArithmetic_testNonvectorizedLogicalOr_760a93622b583f112db61af5ecb89065() {
        assertEval("{ NA || 1 }");
    }

    @Test
    public void TestSimpleArithmetic_testNonvectorizedLogicalOr_575c3b7057582a5e30afa06c6ad2ad77() {
        assertEval("{ 0 || NA }");
    }

    @Test
    public void TestSimpleArithmetic_testNonvectorizedLogicalOr_242f310f9750193bba40515bb9f20379() {
        assertEval("{ NA || 0 }");
    }

    @Test
    public void TestSimpleArithmetic_testNonvectorizedLogicalOr_9b819df695ba4d3b15034aa187c01bd3() {
        assertEval("{ x <- 1 ; f <- function(r) { x <<- 2; r } ; NA || f(NA) ; x }");
    }

    @Test
    public void TestSimpleArithmetic_testNonvectorizedLogicalSpecialChecks_cfba50a4001725b57220cb4dbe5cfd6a() {
        assertEval("{ FALSE && \"hello\" }");
    }

    @Test
    public void TestSimpleArithmetic_testNonvectorizedLogicalSpecialChecks_fdc8ea59d7d02e9f011cdc1e3fbd6dfb() {
        assertEval("{ TRUE || \"hello\" }");
    }

    @Test
    public void TestSimpleArithmetic_testNonvectorizedLogicalSpecialChecks_cd3b11a8a08ad8e03971429625307fa5() {
        assertEval("{ 0 && \"hello\" }");
    }

    @Test
    public void TestSimpleArithmetic_testNonvectorizedLogicalSpecialChecks_05ec7b8e23727b3a9523c6b3bdcde09f() {
        assertEval("{ 0.0 && \"hello\" }");
    }

    @Test
    public void TestSimpleArithmetic_testNonvectorizedLogicalSpecialChecks_53f44f07da03b9d10ab9dc94241ab996() {
        assertEval("{ 1+2i && 0 }");
    }

    @Test
    public void TestSimpleArithmetic_testNonvectorizedLogicalSpecialChecks_4e6a6d54d19e85dbee52ca1e9d3fbadf() {
        assertEval("{ 1+2i && TRUE }");
    }

    @Test
    public void TestSimpleArithmetic_testNonvectorizedLogicalSpecialChecks_8b8c90899a26f742e85bf8683fd9f1fe() {
        assertEval("{ TRUE && 0+0i}");
    }

    @Test
    public void TestSimpleArithmetic_testNonvectorizedLogicalSpecialChecks_515b2f282e6c851eef0caf046006c490() {
        assertEval("{ 1.0 && 0+0i}");
    }

    @Test
    public void TestSimpleArithmetic_testNonvectorizedLogicalSpecialChecks_3f2ad5582aaf127b3fa1203f59721aa3() {
        assertEval("{ logical(0) && logical(0) }");
    }

    @Test
    public void TestSimpleArithmetic_testNonvectorizedLogicalSpecialChecks_e87b491302f42fd5b9d44498164b954c() {
        assertEval("{ logical(0) && TRUE }");
    }

    @Test
    public void TestSimpleArithmetic_testNonvectorizedLogicalSpecialChecks_85db1f05c7496d81ab9698cda92d281f() {
        assertEval("{ logical(0) && FALSE }");
    }

    @Test
    public void TestSimpleArithmetic_testNonvectorizedLogicalSpecialChecks_b56c8840a38299ad9972cca70c0e981d() {
        assertEval("{ 1 || \"hello\" }");
    }

    @Test
    public void TestSimpleArithmetic_testNonvectorizedLogicalSpecialChecks_e3bebc31704023fade44e00f7abfbe92() {
        assertEval("{ FALSE || 1+2i }");
    }

    @Test
    public void TestSimpleArithmetic_testNonvectorizedLogicalSpecialChecks_91c7d8f56c2020b4f049b8a1f2b4920b() {
        assertEval("{ 0+0i || FALSE}");
    }

    @Test
    public void TestSimpleArithmetic_testNonvectorizedLogicalSpecialChecks_250eb924ad73f0bd5845691cf37bc3e8() {
        assertEval("{ 1.1 || \"hello\" }");
    }

    @Test
    public void TestSimpleArithmetic_testNonvectorizedLogicalSpecialChecks_5dd259f546909e39cf793ce5b00ff554() {
        assertEval("{ 1+2i || 0 }");
    }

    @Test
    public void TestSimpleArithmetic_testNonvectorizedLogicalSpecialChecks_ed2ce66395f43593bd77dfa55bde6bed() {
        assertEval("{ 1+2i || 1.0 }");
    }

    @Test
    public void TestSimpleArithmetic_testNonvectorizedLogicalSpecialChecks_5663699886cbdea8749e9cdc778245c9() {
        assertEval("{ c(TRUE,FALSE) | logical() }");
    }

    @Test
    public void TestSimpleArithmetic_testNonvectorizedLogicalSpecialChecks_077b6a471529efd7c28cf1386adbc0ae() {
        assertEval("{ logical() | c(TRUE,FALSE) }");
    }

    @Test
    public void TestSimpleArithmetic_testNonvectorizedLogicalSpecialChecks_e980b880f67c586ee098681fb19abb1d() {
        assertEval("{ as.raw(c(1,4)) | raw() }");
    }

    @Test
    public void TestSimpleArithmetic_testNonvectorizedLogicalSpecialChecks_5b535f433912e7c82ad33269b3c9c3fb() {
        assertEval("{ raw() | as.raw(c(1,4))}");
    }

    @Test
    public void TestSimpleArithmetic_testNonvectorizedLogicalSpecialChecks_82cfd97f03646f390c31d30590ed9c9f() {
        assertEval("{ logical(0) || logical(0) }");
    }

    @Test
    public void TestSimpleArithmetic_testNonvectorizedLogicalSpecialChecks_58043a4520662956383c9439eda19b3a() {
        assertEval("{ logical(0) || TRUE }");
    }

    @Test
    public void TestSimpleArithmetic_testNonvectorizedLogicalSpecialChecks_e76e51b08dc40a0aaecd51fa1282cb91() {
        assertEval("{ logical(0) || FALSE }");
    }

    @Test
    public void TestSimpleArithmetic_testNonvectorizedLogicalSpecialChecks_cd87ee7ad7211e553313f7da7ed79f00() {
        assertEvalError("{ \"hello\" || TRUE }");
    }

    @Test
    public void TestSimpleArithmetic_testNonvectorizedLogicalSpecialChecks_3d4942d3f510f8b7df72309b09d0f706() {
        assertEvalError("{ FALSE || \"hello\" }");
    }

    @Test
    public void TestSimpleArithmetic_testNonvectorizedLogicalSpecialChecks_a8e04940f4acf7b01d96884ee2e2802e() {
        assertEvalError("{ 1 && \"hello\" }");
    }

    @Test
    public void TestSimpleArithmetic_testNonvectorizedLogicalSpecialChecks_b0d66f451f4dca2a71c2583473ffce37() {
        assertEvalError("{ 0.1 && \"hello\" }");
    }

    @Test
    public void TestSimpleArithmetic_testNonvectorizedLogicalSpecialChecks_f10d753b2bced9e3dffb226ef0873762() {
        assertEvalError("{ TRUE && \"hello\" }");
    }

    @Test
    public void TestSimpleArithmetic_testNonvectorizedLogicalSpecialChecks_7fa0b8add5e6d06679999cb35350915d() {
        assertEvalError("{ \"hello\" && TRUE }");
    }

    @Test
    public void TestSimpleArithmetic_testNonvectorizedLogicalSpecialChecks_3b90ad971292b18114d513b30e69f5fb() {
        assertEvalError("{ \"hello\" && 1 }");
    }

    @Test
    public void TestSimpleArithmetic_testNonvectorizedLogicalSpecialChecks_5838ac95b19505178a85db9c0028cc50() {
        assertEvalError("{ \"hello\" && 1L }");
    }

    @Test
    public void TestSimpleArithmetic_testNonvectorizedLogicalSpecialChecks_caf963ebc8aa302adef10c8d57b0beb5() {
        assertEvalError("{ NULL && 1 }");
    }

    @Test
    public void TestSimpleArithmetic_testNonvectorizedLogicalSpecialChecks_68feed475319e891b10e8af5da92d322() {
        assertEvalError("{ 0.1 && NULL }");
    }

    @Test
    public void TestSimpleArithmetic_testNonvectorizedLogicalSpecialChecks_3d7fba79ef009650011f6e19d9a66f01() {
        assertEvalError("{ as.raw(1) && 1 }");
    }

    @Test
    public void TestSimpleArithmetic_testNonvectorizedLogicalSpecialChecks_df6652528d8be7f3d56f1cfd1327184e() {
        assertEvalError("{ 0.1 && as.raw(1) }");
    }

    @Test
    public void TestSimpleArithmetic_testNonvectorizedLogicalSpecialChecks_86ba711f72f5ac4b0d5c22311f9d4dc4() {
        assertEvalError("{ character(0) && FALSE }");
    }

    @Test
    public void TestSimpleArithmetic_testNonvectorizedLogicalSpecialChecks_101ebf2b12333cafe8aca411da8715b4() {
        assertEvalError("{ character(0) && TRUE }");
    }

    @Test
    public void TestSimpleArithmetic_testNonvectorizedLogicalSpecialChecks_a3e717839989fef0f0f31d9228e40dfc() {
        assertEvalError("{ 0 || \"hello\" }");
    }

    @Test
    public void TestSimpleArithmetic_testNonvectorizedLogicalSpecialChecks_79c0fd44f9ee5f8dad0bc89580c3d5c8() {
        assertEvalError("{ 0L || \"hello\" }");
    }

    @Test
    public void TestSimpleArithmetic_testNonvectorizedLogicalSpecialChecks_ca99587d0dccc88b7e24e4276ca4ad67() {
        assertEvalError("{ \"hello\" || FALSE }");
    }

    @Test
    public void TestSimpleArithmetic_testNonvectorizedLogicalSpecialChecks_0f7a997113480257623eb03f8379e158() {
        assertEvalError("{ \"hello\" || 1 }");
    }

    @Test
    public void TestSimpleArithmetic_testNonvectorizedLogicalSpecialChecks_fde5b40fd6ac3d81925ea9695a5370ab() {
        assertEvalError("{ \"hello\" || 1L }");
    }

    @Test
    public void TestSimpleArithmetic_testNonvectorizedLogicalSpecialChecks_c95444abd5e43a0900d814a17fc28a58() {
        assertEvalError("{ NULL || 1 }");
    }

    @Test
    public void TestSimpleArithmetic_testNonvectorizedLogicalSpecialChecks_f4f1d0076aac93764444979f2ef93750() {
        assertEvalError("{ 0 || NULL }");
    }

    @Test
    public void TestSimpleArithmetic_testNonvectorizedLogicalSpecialChecks_d43c763bc74de86bfca4c261cbc965f0() {
        assertEvalError("{ as.raw(1) || 1 }");
    }

    @Test
    public void TestSimpleArithmetic_testNonvectorizedLogicalSpecialChecks_8e4ec7bd6b3ef0faf637c0b9fa1a3c8b() {
        assertEvalError("{ 0 || as.raw(1) }");
    }

    @Test
    public void TestSimpleArithmetic_testNonvectorizedLogicalSpecialChecks_0f8ddb2f431956495e6d3de5d97793cb() {
        assertEvalError("{ as.raw(10) && \"hi\" }");
    }

    @Test
    public void TestSimpleArithmetic_testNonvectorizedLogicalSpecialChecks_82387ae5f5be52ae3c5771b24adffd18() {
        assertEvalError("{ character(0) || FALSE }");
    }

    @Test
    public void TestSimpleArithmetic_testNonvectorizedLogicalSpecialChecks_2e79166909c81dc7a05657a71ef3b7f2() {
        assertEvalError("{ character(0) || TRUE }");
    }

    @Test
    public void TestSimpleArithmetic_testScalars_186631423c2671219f6f877cecd6813b() {
        assertEval("{ 1L / 2L }");
    }

    @Test
    public void TestSimpleArithmetic_testScalars_69097829d5c88829794b03d29d20b8ce() {
        assertEval("{ f <- function(a, b) { a / b } ; f(1L, 2L) ; f(1, 2) }");
    }

    @Test
    public void TestSimpleArithmetic_testScalars_94287359cfb1c1ddcca7c50d6d1fc934() {
        assertEval("{ (1:2)[3] / 2L }");
    }

    @Test
    public void TestSimpleArithmetic_testScalars_84ddba9eb45e94df82001276c9f1bcdc() {
        assertEval("{ 2L / (1:2)[3] }");
    }

    @Test
    public void TestSimpleArithmetic_testScalars_ef2f16bd42f3d621ddf6f5b85a8c2d07() {
        assertEval("{ a <- (1:2)[3] ; b <- 2L ; a / b }");
    }

    @Test
    public void TestSimpleArithmetic_testScalars_0b07e9009d2b69b0a2d3a3455795f373() {
        assertEval("{ a <- 2L ; b <- (1:2)[3] ; a / b }");
    }

    @Test
    public void TestSimpleArithmetic_testScalars_e75f1cfbb6b0dbba9f5f172556a88c0a() {
        assertEval("{ (1:2)[3] + 2L }");
    }

    @Test
    public void TestSimpleArithmetic_testScalars_4ec2b1c1f69dd9db4c2a00b54718a383() {
        assertEval("{ 2L + (1:2)[3] }");
    }

    @Test
    public void TestSimpleArithmetic_testScalars_5228271a44507bcd597e7d9d62fb3443() {
        assertEval("{ a <- (1:2)[3] ; b <- 2L ; a + b }");
    }

    @Test
    public void TestSimpleArithmetic_testScalars_9ab44a01d786f609320ecbda14f28abf() {
        assertEval("{ a <- 2L ; b <- (1:2)[3] ; a + b }");
    }

    @Test
    public void TestSimpleArithmetic_testScalars_d270e7cc8f6c72a9ce5baeb4debe7dad() {
        assertEval("{ a <- (1:2)[3] ; b <- 2 ; a + b }");
    }

    @Test
    public void TestSimpleArithmetic_testScalars_9ef70bef566326dfc334d1ecdac268cf() {
        assertEval("{ a <- 2 ; b <- (1:2)[3] ; a + b }");
    }

    @Test
    public void TestSimpleArithmetic_testScalars_2ef895188aeec66b46a86a0084b2a897() {
        assertEval("{ f <- function(a, b) { a / b } ; f(1,1) ; f(1,1L) ; f(2L,4) }");
    }

    @Test
    public void TestSimpleArithmetic_testScalars_56777eeff1eb98c086df11debd53a324() {
        assertEval("{ f <- function(a, b) { a / b } ; f(1,1) ; f(1,1L) ; f(2L,4L) }");
    }

    @Test
    public void TestSimpleArithmetic_testScalars_5c748b3d4087d10ba63f6a8c138ae1ed() {
        assertEval("{ f <- function(a, b) { a / b } ; f(1,1) ; f(1,1L) ; f(2L,(1:2)[3]) }");
    }

    @Test
    public void TestSimpleArithmetic_testScalars_84880b284d8e876f1305479838b3b1a9() {
        assertEval("{ f <- function(a, b) { a / b } ; f(1,1) ; f(1,1L) ; f((1:2)[3], 2L) }");
    }

    @Test
    public void TestSimpleArithmetic_testScalars_116703f2c3a220bc9256a6f605642a9a() {
        assertEval("{ f <- function(a, b) { a + b } ; f(1,1) ; f(1,1L) ; f(2L,4) }");
    }

    @Test
    public void TestSimpleArithmetic_testScalars_70e80e6089bb9dbcc90262b7b8ded679() {
        assertEval("{ f <- function(a, b) { a + b } ; f(1,1) ; f(1,1L) ; f(2L,4L) }");
    }

    @Test
    public void TestSimpleArithmetic_testScalars_d6249993511f48359b36daa49673225b() {
        assertEval("{ f <- function(a, b) { a + b } ; f(1,1) ; f(1,1L) ; f(2L,(1:2)[3]) }");
    }

    @Test
    public void TestSimpleArithmetic_testScalars_75a20c7661d5e982b8f2b4bdc2577da8() {
        assertEval("{ f <- function(a, b) { a + b } ; f(1,1) ; f(1,1L) ; f((1:2)[3], 2L) }");
    }

    @Test
    public void TestSimpleArithmetic_testScalars_2f158ef5596f78f866f1de7bce1f7784() {
        assertEval("{ f <- function(a, b) { a / b } ; f(1,1) ; f(1,1L) ; f(2,(1:2)[3]) }");
    }

    @Test
    public void TestSimpleArithmetic_testScalars_77bbb91ce80d2ceb3b1fa96156368f95() {
        assertEval("{ f <- function(a, b) { a / b } ; f(1,1) ; f(1,1L) ; f((1:2)[3],2) }");
    }

    @Test
    public void TestSimpleArithmetic_testScalars_d6e37e585a5786869d75d20335b2b4f6() {
        assertEval("{ f <- function(b) { 1 / b } ; f(1) ; f(1L) ; f(4) }");
    }

    @Test
    public void TestSimpleArithmetic_testScalars_925fb3c91d92ac487d4870c346573acd() {
        assertEval("{ f <- function(b) { 1 / b } ; f(1) ; f(1L) }");
    }

    @Test
    public void TestSimpleArithmetic_testScalars_b9293f4877209fe4c2d1b7934c7557f0() {
        assertEval("{ f <- function(b) { 1 / b } ; f(1L) ; f(1) }");
    }

    @Test
    public void TestSimpleArithmetic_testScalars_f68293e2bd9e5050f7837a7acf4f3a06() {
        assertEval("{ f <- function(b) { 1 / b } ; f(TRUE) ; f(1L) }");
    }

    @Test
    public void TestSimpleArithmetic_testScalars_887ff2a05b1453b98aea46535d185a42() {
        assertEval("{ f <- function(b) { b / 1 } ; f(1) ; f(1L) ; f(4) }");
    }

    @Test
    public void TestSimpleArithmetic_testScalars_acc6834548ad102ce95d9a70ced0de76() {
        assertEval("{ f <- function(b) { b / 2 } ; f(1) ; f(1L) }");
    }

    @Test
    public void TestSimpleArithmetic_testScalars_8c20fe2311e73618347df9436dfccac9() {
        assertEval("{ f <- function(b) { b / 4 } ; f(1L) ; f(1) }");
    }

    @Test
    public void TestSimpleArithmetic_testScalars_13aeae0ea9aeabb6c54503c997181dd0() {
        assertEval("{ f <- function(b) { 4L / b } ; f(1L) ; f(2) }");
    }

    @Test
    public void TestSimpleArithmetic_testScalars_239c1fda07eec2149279fbcbb024e970() {
        assertEval("{ f <- function(b) { 4L + b } ; f(1L) ; f(2) }");
    }

    @Test
    public void TestSimpleArithmetic_testScalars_7e07c33b47ae19bbce6c1a30dd2b4397() {
        assertEval("{ f <- function(b) { b / 2L } ; f(1L) ; f(2) }");
    }

    @Test
    public void TestSimpleArithmetic_testScalars_056fd1401eb9b6d44fdd6b7118adec90() {
        assertEval("{ f <- function(b) { 4L / b } ; f(1L) ; f(2) ; f(TRUE) }");
    }

    @Test
    public void TestSimpleArithmetic_testScalars_e5b5720cd626d122d70a44a63d2e038d() {
        assertEval("{ f <- function(b) { 4L + b } ; f(1L) ; f(2) ; f(TRUE) }");
    }

    @Test
    public void TestSimpleArithmetic_testScalars_82b87d6972e69ef3aa94df96bceb9a13() {
        assertEval("{ f <- function(b) { 4L + b } ; f(1L) ; f(2) ; f((1:2)[3]) }");
    }

    @Test
    public void TestSimpleArithmetic_testScalars_7613b5ee974bcf117b9ad2a1aef4482d() {
        assertEval("{ f <- function(b) { 4L / b } ; f(1L) ; f(2) ; f((1:2)[3]) }");
    }

    @Test
    public void TestSimpleArithmetic_testScalars_fad6b936dbaf7bf25da3ea9d56323264() {
        assertEval("{ f <- function(b) { (1:2)[3] + b } ; f(1L) ; f(2) }");
    }

    @Test
    public void TestSimpleArithmetic_testScalars_bf738ed494d2f6e7c4ad0b264f901dad() {
        assertEval("{ f <- function(b) { (1:2)[3] + b } ; f(1) ; f(2L) }");
    }

    @Test
    public void TestSimpleArithmetic_testScalars_569a75cffcded7df47e97cdef09d39a1() {
        assertEval("{ f <- function(b) { b + 4L } ; f(1L) ; f(2) ; f(TRUE) }");
    }

    @Test
    public void TestSimpleArithmetic_testScalars_501e7431265017eac321df953b5d76a2() {
        assertEval("{ f <- function(b) { b + 4L } ; f(1L) ; f(2) ; f((1:2)[3]) }");
    }

    @Test
    public void TestSimpleArithmetic_testScalars_6410a63578eda77956f1eb6bd7887048() {
        assertEval("{ f <- function(b) { b / 4L } ; f(1L) ; f(2) ; f(TRUE) }");
    }

    @Test
    public void TestSimpleArithmetic_testScalars_5b709806d5a2e78cf7266aee73cb11c7() {
        assertEval("{ f <- function(b) { b / 4L } ; f(1L) ; f(2) ; f((1:2)[3]) }");
    }

    @Test
    public void TestSimpleArithmetic_testScalars_a22bb6e39039a0e2a4ab56be9aa3bdcb() {
        assertEval("{ f <- function(b) { 1 + b } ; f(1L) ; f(TRUE) }");
    }

    @Test
    public void TestSimpleArithmetic_testScalars_d79498270e9d18dfb1a89fce92ab08ac() {
        assertEval("{ f <- function(b) { FALSE + b } ; f(1L) ; f(2) }");
    }

    @Test
    public void TestSimpleArithmetic_testScalars_c077bbafce498e393c8fa79b9551eebc() {
        assertEval("{ f <- function(b) { b + 1 } ; f(1L) ; f(TRUE) }");
    }

    @Test
    public void TestSimpleArithmetic_testScalars_00e340b620c338b55b0ba841e730ce4c() {
        assertEval("{ f <- function(b) { b + FALSE } ; f(1L) ; f(2) }");
    }

    @Test
    public void TestSimpleArithmetic_testScalarsComplex_7a36b65206e2d484390878a9ec86cee3() {
        assertEval("{ (1+2i)*(3+4i) }");
    }

    @Test
    public void TestSimpleArithmetic_testScalarsComplex_262cd3f2bc987a80702aff7b886bdd74() {
        assertEval("{ x <- 1+2i; y <- 3+4i; x*y }");
    }

    @Test
    public void TestSimpleArithmetic_testScalarsComplex_585854327c85383a0189a81ae30d0f04() {
        assertEval("{ x <- 1+2i; y <- 3+4i; x-y }");
    }

    @Test
    public void TestSimpleArithmetic_testScalarsComplex_d38efa5cf6e85de2ce0e840877c6274f() {
        assertEval("{ x <- c(-1.5-1i,-1.3-1i) ; y <- c(0+0i, 0+0i) ; y*y+x }");
    }

    @Test
    public void TestSimpleArithmetic_testScalarsComplex_a7ad6778b6621bb07aaf361afa349c42() {
        assertEval("{ x <- c(-1.5-1i,-1.3-1i) ; y <- c(0+0i, 0+0i) ; y-x }");
    }

    @Test
    public void TestSimpleArithmetic_testScalarsComplex_d52392544a7d42e16166a7e0fb0a5465() {
        assertEval("{ x <- 1+2i; y <- 3+4i; x/y }");
    }

    @Test
    public void TestSimpleArithmetic_testScalarsComplex_80cfe9bbee075e0700b78acc33ea4ad6() {
        assertEval("{ x <- c(-1-2i,3+10i) ; y <- c(3+1i, -4+5i) ; y-x }");
    }

    @Test
    public void TestSimpleArithmetic_testScalarsComplex_e1ae1c2af2f35adba4733bb031c66b19() {
        assertEval("{ (1+2i)^2 }");
    }

    @Test
    public void TestSimpleArithmetic_testScalarsComplex_274e34fe67c827c3a3481eeb929fd942() {
        assertEval("{ (1+2i)^(-2) }");
    }

    @Test
    public void TestSimpleArithmetic_testScalarsComplex_c859ca43706b3a82cad19ecef340e76b() {
        assertEval("{ (1+2i)^0 }");
    }

    @Test
    public void TestSimpleArithmetic_testScalarsComplex_34c8f857664c09e4a590a4fff80f1c83() {
        assertEval("{ 1/((1+0i)/(0+0i)) }");
    }

    @Test
    public void TestSimpleArithmetic_testScalarsComplex_685e18553e73232fd903f34faa58ec02() {
        assertEval("{ ((1+0i)/(0+0i)) ^ (-3) }");
    }

    @Test
    public void TestSimpleArithmetic_testScalarsComplex_5732c8d2f1659e05d68762541d667d29() {
        assertEval("{ f <- function(a, b) { a + b } ; f(1+2i, 3+4i) ; f(1, 2) }");
    }

    @Test
    public void TestSimpleArithmetic_testScalarsComplex_cf9d64b117ad27553a621ed57dea3ac6() {
        assertEval("{ f <- function(a, b) { a + b } ; f(2, 3+4i) ; f(1, 2) }");
    }

    @Test
    public void TestSimpleArithmetic_testScalarsComplex_f66aaa58a9903fa88847396fe7dde40d() {
        assertEval("{ f <- function(a, b) { a + b } ; f(1+2i, 3) ; f(1, 2) }");
    }

    @Test
    public void TestSimpleArithmetic_testScalarsComplex_ae77830355ffa60a341a5af4c66b6bfb() {
        assertEval("{ f <- function(b) { b / 4i } ; f(1) ; f(1L) }");
    }

    @Test
    public void TestSimpleArithmetic_testScalarsComplex_d8d77339fc0de88fac10791e3f577ab1() {
        assertEval("{ f <- function(b) { 1i / b } ; f(1) ; f(1L) ; f(4) }");
    }

    @Test
    public void TestSimpleArithmetic_testScalarsComplex_45f6d0c3b8410f3c35be3890b2c65885() {
        assertEval("{ f <- function(b) { 1i / b } ; f(1+1i) ; f(1L) }");
    }

    @Test
    public void TestSimpleArithmetic_testScalarsComplex_ba49497cffb3fff3b97b40503f565798() {
        assertEval("{ f <- function(b) { 1i / b } ; f(1) ; f(1L) }");
    }

    @Test
    public void TestSimpleArithmetic_testScalarsComplex_a9549fe9b3baad4869a62913144fa5c2() {
        assertEval("{ f <- function(b) { 1i / b } ; f(TRUE) ; f(1L) }");
    }

    @Test
    public void TestSimpleArithmetic_testScalarsComplex_2bae58be02dccb51ae4cad3616f0cdad() {
        assertEval("{ f <- function(a, b) { a + b } ; f(1,1) ; f(1,1+2i) ; f(TRUE, 2)  }");
    }

    @Test
    public void TestSimpleArithmetic_testScalarsComplex_b1b4bb9041840baeab0c2eaa666173ca() {
        assertEval("{ f <- function(b) { 1 / b } ; f(1+1i) ; f(1L)  }");
    }

    @Test
    public void TestSimpleArithmetic_testScalarsComplex_9b515da8059337cca20b191fb590dbc6() {
        assertEval("{ f <- function(b) { b / 2 } ; f(1+1i) ; f(1L)  }");
    }

    @Test
    public void TestSimpleArithmetic_testScalarsComplex_f777aed6636c3d4bfab7026441d24188() {
        assertEval("{ f <- function(a, b) { a / b } ; f(1,1) ; f(1,1L) ; f(2+1i,(1:2)[3]) }");
    }

    @Test
    public void TestSimpleArithmetic_testScalarsComplex_163f2795c697e95e51884f26116db16b() {
        assertEval("{ (0+2i)^0 }");
    }

    @Test
    public void TestSimpleArithmetic_testScalarsComplex_9bd3ca7d6b66f864b101aa1165f16435() {
        assertEval("{ (1+2i) / ((0-1i)/(0+0i)) }");
    }

    @Test
    public void TestSimpleArithmetic_testScalarsComplex_a86889b885428d4aeed5f5abec2fb487() {
        assertEval("{ ((1+1i)/(0+0i)) ^ (-3) }");
    }

    @Test
    public void TestSimpleArithmetic_testScalarsComplex_c65fc676672c24a2284a61ac44346062() {
        assertEval("{ (3+2i)^2 }");
    }

    @Test
    public void TestSimpleArithmetic_testScalarsComplex_ec41b305258209dcd1f01b4c73dfa38c() {
        assertEval("{ x <- 1+2i; y <- 3+4i; round(x*x*y/(x+y), digits=5) }");
    }

    @Test
    public void TestSimpleArithmetic_testScalarsComplex_e04a6b232a786acb98dfb4218c363291() {
        assertEval("{ round( (1+2i)^(3+4i), digits=5 ) }");
    }

    @Test
    public void TestSimpleArithmetic_testScalarsComplex_b962b71eb5d225af35b575db321f93dc() {
        assertEval("{ round( ((1+1i)/(0+1i)) ^ (-3.54), digits=5) }");
    }

    @Test
    public void TestSimpleArithmetic_testScalarsComplex_c4757c37a7ae086ad8969c73a91e9cf2() {
        assertEval("{ c(1+2i,1.1+2.1i) }");
    }

    @Test
    public void TestSimpleArithmetic_testScalarsComplex_78cc1126bf3f8f6e3c709b5beacf95d8() {
        assertEval("{ c(1+2i,11.1+2.1i) }");
    }

    @Test
    public void TestSimpleArithmetic_testScalarsComplex_a2328b24a0a62e3659427bb986873204() {
        assertEval("{ c(1+2i,1.1+12.1i) }");
    }

    @Test
    public void TestSimpleArithmetic_testScalarsComplex_db6b99346a02533a68ac1bdc5d6a1182() {
        assertEval("{ c(11+2i,1.1+2.1i) }");
    }

    @Test
    public void TestSimpleArithmetic_testScalarsComplex_5434efc74a8434ff9b00c74c21af36be() {
        assertEval("{ c(1+12i,1.1+2.1i) }");
    }

    @Test
    public void TestSimpleArithmetic_testScalarsComplex_f8e47fb5c1d2a99c0ceadffef991295e() {
        assertEval("{ c(-1+2i,1.1+2.1i) }");
    }

    @Test
    public void TestSimpleArithmetic_testScalarsComplex_60b9c85b174529d3ae154bf8ae10cd98() {
        assertEval("{ c(1-2i,1+22i) }");
    }

    @Test
    public void TestSimpleArithmetic_testScalarsComplex_3687279e543d6b321de708f1696d22d9() {
        assertEval("{ x <- c(-1-2i,3+10i) ; y <- c(3+1i, -4+5i) ; round(y/x, digits=5) }");
    }

    @Test
    public void TestSimpleArithmetic_testScalarsComplexIgnore_0dc33376658fd492af2c5beb032efdbf() {
        assertEval("{ x <- c(-1-2i,3+10i) ; y <- c(3+1i, -4+5i) ; y+x }");
    }

    @Test
    public void TestSimpleArithmetic_testScalarsComplexIgnore_46cb3430474fb02811820b09b2bcd950() {
        assertEval("{ x <- c(-1-2i,3+10i) ; y <- c(3+1i, -4+5i) ; y*x }");
    }

    @Test
    public void TestSimpleArithmetic_testScalarsRange_dd5eca5b8772a13dc278f20c915b60db() {
        assertEval("{ f <- function(a, b) { a + b } ; f(c(1,2), c(3,4)) ; f(c(1,2), 3:4) }");
    }

    @Test
    public void TestSimpleArithmetic_testScalarsRange_f8875779383c435f06610f698b00e481() {
        assertEval("{ f <- function(a, b) { a + b } ; f(1:2, c(3,4)) ; f(c(1,2), 3:4) }");
    }

    @Test
    public void TestSimpleArithmetic_testScalarsRange_5636fbef654851e28d20d8bbb43fc670() {
        assertEval("{ f <- function(a, b) { a + b } ; f(1:2, 3:4) ; f(c(1,2), 3:4) }");
    }

    @Test
    public void TestSimpleArithmetic_testScalarsReal_5e5988347c2d8cbd4606eee90ce2b335() {
        assertEval("{ 1L+1 }");
    }

    @Test
    public void TestSimpleArithmetic_testScalarsReal_b779c148ae12bf9a4e04f1c519982853() {
        assertEval("{ 1L+1L }");
    }

    @Test
    public void TestSimpleArithmetic_testScalarsReal_a0e88bbb1d756086d455b6d8f465b8dd() {
        assertEval("{ ( 1+1)*(3+2) }");
    }

    @Test
    public void TestSimpleArithmetic_testScalarsReal_89950aaa7904a1aa1f3c6a1cc7fd2518() {
        assertEval("{ 1+TRUE }");
    }

    @Test
    public void TestSimpleArithmetic_testScalarsReal_1ae0f163b8728127078c82e6e565a8d8() {
        assertEval("{ 1L+TRUE }");
    }

    @Test
    public void TestSimpleArithmetic_testScalarsReal_9624d804341a740e3857be00394ae0b8() {
        assertEval("{ 1+FALSE<=0 }");
    }

    @Test
    public void TestSimpleArithmetic_testScalarsReal_51569e83b1718ab59d4fefbd74b7c730() {
        assertEval("{ 1L+FALSE<=0 }");
    }

    @Test
    public void TestSimpleArithmetic_testScalarsReal_c982e8fb2ee87aabd3cbc22df8731f45() {
        assertEval("{ TRUE+TRUE+TRUE*TRUE+FALSE+4 }");
    }

    @Test
    public void TestSimpleArithmetic_testScalarsReal_0e87d87b38b7386f4300ebbffe64a3c6() {
        assertEval("{ 1L*NA }");
    }

    @Test
    public void TestSimpleArithmetic_testScalarsReal_201efe7374a30129f27f930b5d57015f() {
        assertEval("{ 1+NA }");
    }

    @Test
    public void TestSimpleArithmetic_testScalarsReal_6bfe156ff2611a1de13f1001c82b08a4() {
        assertEval("{ 2L^10L }");
    }

    @Test
    public void TestSimpleArithmetic_testScalarsReal_da3edbe7985b8d4c078992481c60d00c() {
        assertEval("{ 0x10 + 0x10L + 1.28 }");
    }

    @Test
    public void TestSimpleArithmetic_testScalarsRealIgnore_706f889093f4841d307059b60cb81c13() {
        assertEval("{ 1000000000*100000000000 }");
    }

    @Test
    public void TestSimpleArithmetic_testScalarsRealIgnore_85c78d2d490e3d28bc72254fbec91949() {
        assertEval("{ 1000000000L*1000000000 }");
    }

    @Test
    public void TestSimpleArithmetic_testScalarsRealIgnore_846b21508ff7d445e01b13f78cc32dba() {
        assertEval("{ 1000000000L*1000000000L }");
    }

    @Test
    public void TestSimpleArithmetic_testUnaryMinus_354d377e6e8a798d6a219fa84614eb79() {
        assertEval("{ -3 }");
    }

    @Test
    public void TestSimpleArithmetic_testUnaryMinus_c0a840f339b3835fd0651357d7854201() {
        assertEval("{ --3 }");
    }

    @Test
    public void TestSimpleArithmetic_testUnaryMinus_7a76b9408e0d879a22542acd68f8e06a() {
        assertEval("{ ---3 }");
    }

    @Test
    public void TestSimpleArithmetic_testUnaryMinus_795eea899e4c26eb99d9ac804c48fbff() {
        assertEval("{ ----3 }");
    }

    @Test
    public void TestSimpleArithmetic_testUnaryMinus_25918da55b4273462e2b83de73d7f553() {
        assertEval("{ -(0/0) }");
    }

    @Test
    public void TestSimpleArithmetic_testUnaryMinus_0ae57acbf3978e9a6e6f362479cef43e() {
        assertEval("{ -(1/0) }");
    }

    @Test
    public void TestSimpleArithmetic_testUnaryMinusAsFunction_8c4038ab2149a1c53e903705f7bbd937() {
        assertEval("{ f <- function(z) { -z } ; f(TRUE) ; f(1L) }");
    }

    @Test
    public void TestSimpleArithmetic_testUnaryMinusAsFunction_9ac6be2b3cd2ec729f2ae9440e929209() {
        assertEval("{ f <- function(z) { -z } ; f(1L) ; f(1) }");
    }

    @Test
    public void TestSimpleArithmetic_testUnaryMinusAsFunction_c33985a5a61dd127b8344a5f54068948() {
        assertEval("{ f <- function(z) { -z } ; f(1) ; f(1L) }");
    }

    @Test
    public void TestSimpleArithmetic_testUnaryMinusAsFunction_ad32eae103dd185d82e6f66f8c88d0cc() {
        assertEval("{ f <- function(z) { -z } ; f(1L) ; f(TRUE) }");
    }

    @Test
    public void TestSimpleArithmetic_testUnaryMinusAsFunction_fb30dde62421f121b9f4464672e44dfb() {
        assertEval("{ z <- logical() ; -z }");
    }

    @Test
    public void TestSimpleArithmetic_testUnaryMinusAsFunction_4d74f70cdc010447dbd665e51e0ca72a() {
        assertEval("{ z <- integer() ; -z }");
    }

    @Test
    public void TestSimpleArithmetic_testUnaryMinusAsFunction_e7a77d18a773ed94576da3005163944a() {
        assertEval("{ z <- double() ; -z }");
    }

    @Test
    public void TestSimpleArithmetic_testUnaryMinusAsFunctionComplex_2ef3e2b434c2df195cd1a5960c38bfa6() {
        assertEval("{ f <- function(z) { -z } ; f(1L) ; f(1+1i) }");
    }

    @Test
    public void TestSimpleArithmetic_testUnaryMinusAsFunctionComplex_ee081ab92d450e411a984c558262615c() {
        assertEval("{ f <- function(z) { -z } ; f(1+1i) ; f(1L) }");
    }

    @Test
    public void TestSimpleArithmetic_testUnaryMinusAsFunctionComplexIgnore_cd4ef6b3e70982a4c95167396730ad4b() {
        assertEval("{ z <- (1+1i)[0] ; -z }");
    }

    @Test
    public void TestSimpleArithmetic_testUnaryMinusAsFunctionComplexIgnore_f8f74002ffea632d51fc3d3665458ddc() {
        assertEval("{ f <- function(z) { -z } ; f(1:3) ; f(c((0+0i)/0,1+1i)) }");
    }

    @Test
    public void TestSimpleArithmetic_testUnaryMinusAsFunctionIgnore_4c11e746d97889722bef95b2bdd24346() {
        assertEval("{ f <- function(z) { -z } ; f(1:3) ; f(1L) }");
    }

    @Test
    public void TestSimpleArithmetic_testUnaryMinusAsFunctionIgnore_f2913439e4ee1afd564679f72b140a69() {
        assertEval("{ f <- function(z) { -z } ; f(1:3) ; f(TRUE) }");
    }

    @Test
    public void TestSimpleArithmetic_testUnaryMinusComplex_dd24e4be6558c646da975eff2f49c134() {
        assertEval("{ -(2+1i)  }");
    }

    @Test
    public void TestSimpleArithmetic_testUnaryMinusComplex_0372ccc0dc005166454e6f60ef472f8a() {
        assertEval("{ -((0+1i)/0)  }");
    }

    @Test
    public void TestSimpleArithmetic_testUnaryMinusComplex_9ff65d7a79a26d4343c979c130cfd45d() {
        assertEval("{ -((1+0i)/0)  }");
    }

    @Test
    public void TestSimpleArithmetic_testUnaryMinusComplexIgnore_b993a07efd537eb7df29f4eb2477101c() {
        assertEval("{ -c((1+0i)/0,2) }");
    }

    @Test
    public void TestSimpleArithmetic_testUnaryMinusErrors_c1f5e118009944a5b67f947745697a4a() {
        assertEvalError("{ z <- \"hello\" ; -z }");
    }

    @Test
    public void TestSimpleArithmetic_testUnaryMinusErrors_3ea860899d34a37019008b913240ce41() {
        assertEvalError("{ z <- c(\"hello\",\"hi\") ; -z }");
    }

    @Test
    public void TestSimpleArithmetic_testUnaryMinusErrors_3a5d9c20857e8cd1fdf6da7e6ba61ed0() {
        assertEvalError("{ f <- function(z) { -z } ; f(1:3) ; f(\"hello\") }");
    }

    @Test
    public void TestSimpleArithmetic_testUnaryMinusVector_8edadc7fccaabe12f07eb8c985aa5b35() {
        assertEval("{ -(1[2]) }");
    }

    @Test
    public void TestSimpleArithmetic_testUnaryNot_611eb958cd71def0fd25c2c1911a775f() {
        assertEval("{ !TRUE }");
    }

    @Test
    public void TestSimpleArithmetic_testUnaryNot_5b5eb15edba8d9739b058b4b3b1f3425() {
        assertEval("{ !FALSE }");
    }

    @Test
    public void TestSimpleArithmetic_testUnaryNot_b79c4e77d1ff433f864e22550d675120() {
        assertEval("{ !NA }");
    }

    @Test
    public void TestSimpleArithmetic_testUnaryNotError_e1de893845dc88297503728fe5d2e03c() {
        assertEval("{ x<-1:4; dim(x)<-c(2, 2); names(x)<-101:104; attr(x, \"dimnames\")<-list(c(\"201\", \"202\"), c(\"203\", \"204\")); attr(x, \"foo\")<-\"foo\"; y<-!x; attributes(y) }");
    }

    @Test
    public void TestSimpleArithmetic_testUnaryNotError_f188fcdda0e80e0093ed4ea740e630cd() {
        assertEvalError("{ l <- c(\"hello\", \"hi\") ; !l }");
    }

    @Test
    public void TestSimpleArithmetic_testUnaryNotError_0a1ca1e4bd2cbc1c6c177fb6648d3db0() {
        assertEvalError("{ l <- function(){1} ; !l }");
    }

    @Test
    public void TestSimpleArithmetic_testUnaryNotError_d2547e3e20e3391361d3ff0603170063() {
        assertEvalError("{ l <- list(1); !l }");
    }

    @Test
    public void TestSimpleArithmetic_testUnaryNotPropagate_44e928a036845cbe587412096f70a046() {
        assertEval("{ x<-1:4; dim(x)<-c(2, 2); names(x)<-101:104; attr(x, \"dimnames\")<-list(201:202, 203:204); attr(x, \"foo\")<-\"foo\"; y<-!x; attributes(y) }");
    }

    @Test
    public void TestSimpleArithmetic_testUnaryNotRaw_5a2b5a8b61128750aca8b43af3c88638() {
        assertEval("{ f <- function(arg) { !arg } ; f(as.raw(10)) ; f(as.raw(1:3)) }");
    }

    @Test
    public void TestSimpleArithmetic_testUnaryNotRaw_f5bef882d05ecad663e020c1bc381359() {
        assertEval("{ a <- as.raw(201) ; !a }");
    }

    @Test
    public void TestSimpleArithmetic_testUnaryNotRaw_842f10509b520c9b3ad087b281d4db17() {
        assertEval("{ a <- as.raw(12) ; !a }");
    }

    @Test
    public void TestSimpleArithmetic_testUnaryNotRaw_49b21b728afb05180fab326c49e9dcfb() {
        assertEval("{ f <- function(arg) { !arg } ; f(as.raw(10)) ; f(as.raw(c(a=1,b=2))) }");
    }

    @Test
    public void TestSimpleArithmetic_testUnaryNotRaw_8801c6cb606818bed4684b55c58bf949() {
        assertEval("{ f <- function(arg) { !arg } ; f(as.raw(10)) ; x <- as.raw(10:11) ; attr(x, \"my\") <- 1 ; f(x) }");
    }

    @Test
    public void TestSimpleArithmetic_testUnaryNotRaw_2f01c418739d7e365f5da87ef163d9ac() {
        assertEval("{ l <- list(); !l }");
    }

    @Test
    public void TestSimpleArithmetic_testUnaryNotRawIgnore_38e2346209ed5b661d4d085d731ec2eb() {
        assertEval("{ f <- function(arg) { !arg } ; f(as.raw(10)) ; f(matrix(as.raw(1:4),nrow=2 )) }");
    }

    @Test
    public void TestSimpleArithmetic_testUnaryNotVector_1cdb793ddf4f8f12d5c502c5f4f260bf() {
        assertEval("{ !c(TRUE,TRUE,FALSE,NA) }");
    }

    @Test
    public void TestSimpleArithmetic_testUnaryNotVector_675094a72491db9301e5e2cf2e0e97a7() {
        assertEval("{ !c(1,2,3,4,0,0,NA) }");
    }

    @Test
    public void TestSimpleArithmetic_testUnaryNotVector_491ec58a4b4f49738b937768d9499719() {
        assertEval("{ !((0-3):3) }");
    }

    @Test
    public void TestSimpleArithmetic_testVectorizedLogicalAnd_fe55393ecc0bd08a36ecd03e59b1f621() {
        assertEval("{ TRUE & FALSE }");
    }

    @Test
    public void TestSimpleArithmetic_testVectorizedLogicalAnd_d6dcc5824f84b2861a706947ee15e273() {
        assertEval("{ FALSE & FALSE }");
    }

    @Test
    public void TestSimpleArithmetic_testVectorizedLogicalAnd_5179740d36cc7cdf30f9329d39e06728() {
        assertEval("{ FALSE & TRUE }");
    }

    @Test
    public void TestSimpleArithmetic_testVectorizedLogicalAnd_ec24d35230cc384a67d6f5846a6b9365() {
        assertEval("{ TRUE & TRUE }");
    }

    @Test
    public void TestSimpleArithmetic_testVectorizedLogicalAnd_0159d0b79cf4e0b6dfa9a534759eec6a() {
        assertEval("{ TRUE & NA }");
    }

    @Test
    public void TestSimpleArithmetic_testVectorizedLogicalAnd_24729b1d3c5d50b859f09a5fa708562a() {
        assertEval("{ FALSE & NA }");
    }

    @Test
    public void TestSimpleArithmetic_testVectorizedLogicalAnd_a823ff4fd47e829c8fcd8b6c2a835ee0() {
        assertEval("{ NA & TRUE }");
    }

    @Test
    public void TestSimpleArithmetic_testVectorizedLogicalAnd_1ebf7c109bd5b25fe4aa9d2b7f4817cd() {
        assertEval("{ NA & FALSE }");
    }

    @Test
    public void TestSimpleArithmetic_testVectorizedLogicalAnd_de032146890677546c77d5a26eadc7cd() {
        assertEval("{ NA & NA }");
    }

    @Test
    public void TestSimpleArithmetic_testVectorizedLogicalAnd_c883245aa7598b61072222b846e7632d() {
        assertEval("{ x <- 1 ; f <- function(r) { x <<- 2; r } ; NA & f(NA) ; x }");
    }

    @Test
    public void TestSimpleArithmetic_testVectorizedLogicalAnd_5606cb8b7aeb5d28f3fc21854d2bc619() {
        assertEval("{ x <- 1 ; f <- function(r) { x <<- 2; r } ; FALSE & f(FALSE) ; x }");
    }

    @Test
    public void TestSimpleArithmetic_testVectorizedLogicalAnd_f48496f675d04b09c3c4b09648ee26f3() {
        assertEval("{ 1:4 & c(FALSE,TRUE) }");
    }

    @Test
    public void TestSimpleArithmetic_testVectorizedLogicalAnd_c184b13bd337f6d163382adb99e68ab0() {
        assertEval("{ a <- as.raw(201) ; b <- as.raw(1) ; a & b }");
    }

    @Test
    public void TestSimpleArithmetic_testVectorizedLogicalAndAsFunction_c6c81ab71937235d9de882c22f3a3450() {
        assertEval("{ f <- function(a,b) { a & b } ; f(TRUE, 1L) ; f(FALSE, FALSE) }");
    }

    @Test
    public void TestSimpleArithmetic_testVectorizedLogicalAndAsFunction_6f8663edc4665b1d776819e2842f1009() {
        assertEval("{ f <- function(a,b) { a & b } ; f(TRUE, 1L) ; f(as.raw(10), as.raw(11)) }");
    }

    @Test
    public void TestSimpleArithmetic_testVectorizedLogicalAndAsFunction_f133e960fcb6e28c776fe3cad1684bd8() {
        assertEval("{ f <- function(a,b) { a & b } ; f(TRUE, 1L) ; f(1L, 0L) }");
    }

    @Test
    public void TestSimpleArithmetic_testVectorizedLogicalAndAsFunction_040014da6d7b67600d0bae0930a7801c() {
        assertEval("{ f <- function(a,b) { a & b } ; f(TRUE, 1L) ; f(1L, 0) }");
    }

    @Test
    public void TestSimpleArithmetic_testVectorizedLogicalAndAsFunction_88eb134600c92118c87b5dc9fbacd8fe() {
        assertEval("{ f <- function(a,b) { a & b } ; f(TRUE, 1L) ; f(1L, TRUE) }");
    }

    @Test
    public void TestSimpleArithmetic_testVectorizedLogicalAndAsFunction_2d3db286f2d7609de564b18b045f20db() {
        assertEval("{ f <- function(a,b) { a & b } ; f(TRUE, 1L) ; f(1L, 3+4i) }");
    }

    @Test
    public void TestSimpleArithmetic_testVectorizedLogicalAndAsFunction_30ea56769075fee8ddcaa21ea0a3a58c() {
        assertEval("{ f <- function(a,b) { a & b } ; f(TRUE, FALSE) ; f(1L, 3+4i) }");
    }

    @Test
    public void TestSimpleArithmetic_testVectorizedLogicalAndAsFunction_d30d7e1d8ce20e93cae5b041cb305155() {
        assertEval("{ f <- function(a,b) { a & b } ; f(TRUE, FALSE) ; f(TRUE, 3+4i) }");
    }

    @Test
    public void TestSimpleArithmetic_testVectorizedLogicalAttributes_3cb323aedddca7aec31453b56560ed29() {
        assertEval("{ x<-1:4; names(x)<-101:104; x | TRUE }");
    }

    @Test
    public void TestSimpleArithmetic_testVectorizedLogicalAttributes_f174457596a64fdff066c6d12afc5086() {
        assertEval("{ x<-1:4; names(x)<-101:104; y<-21:24; names(y)<-121:124; x | y }");
    }

    @Test
    public void TestSimpleArithmetic_testVectorizedLogicalAttributes_6d75db9196a7fc63f58c4bda307050ba() {
        assertEval("{ x<-1:4; names(x)<-101:104; y<-21:28; names(y)<-121:128; x | y }");
    }

    @Test
    public void TestSimpleArithmetic_testVectorizedLogicalAttributes_5ca008442080837840e1e6669a54420f() {
        assertEval("{ x<-1:4; names(x)<-101:104; attr(x, \"foo\")<-\"foo\"; attributes(x | TRUE) }");
    }

    @Test
    public void TestSimpleArithmetic_testVectorizedLogicalAttributes_7d74605bd0681bef4342741f635d1dae() {
        assertEval("{ x<-1:4; names(x)<-101:104; attr(x, \"foo\")<-\"foo\"; y<-21:24; names(y)<-121:124; attributes(x | y) }");
    }

    @Test
    public void TestSimpleArithmetic_testVectorizedLogicalAttributes_e50d47a9c7c9a3c298a5c78d241eba5b() {
        assertEval("{ x<-as.raw(1:4); names(x)<-101:104; y<-as.raw(21:24); names(y)<-121:124; x | y }");
    }

    @Test
    public void TestSimpleArithmetic_testVectorizedLogicalAttributes_55b675c333cf66be393c36db69f4c915() {
        assertEval("{ x<-1:4; y<-21:24; names(y)<-121:124; attributes(x | y) }");
    }

    @Test
    public void TestSimpleArithmetic_testVectorizedLogicalAttributes_afec08a1eb0b9964ecd8b188eecf54ee() {
        assertEval("{ x<-1:4; names(x)<-101:104; y<-21:28; names(y)<-121:128;  attributes(y | x) }");
    }

    @Test
    public void TestSimpleArithmetic_testVectorizedLogicalAttributes_bd3b25b61ec66ebcbb094af2af53c2cc() {
        assertEval("{ x<-1:4; names(x)<-101:104; y<-21:28; attributes(x | y) }");
    }

    @Test
    public void TestSimpleArithmetic_testVectorizedLogicalAttributes_6ca96306ddcf6d79c3645b1dcdb13321() {
        assertEval("{ x<-1:4; dim(x)<-c(2,2); y<-21:28; x | y }");
    }

    @Test
    public void TestSimpleArithmetic_testVectorizedLogicalComplex_db6245e5fbff0772afabbadd064b5217() {
        assertEval("{ 1+2i | 0 }");
    }

    @Test
    public void TestSimpleArithmetic_testVectorizedLogicalComplex_c2118b77e1d74a887aeb51afd958c386() {
        assertEval("{ 1+2i & 0 }");
    }

    @Test
    public void TestSimpleArithmetic_testVectorizedLogicalOr_fcf4cf5d4ee95b7377fd4201abde0f79() {
        assertEval("{ 1.1 | 3.15 }");
    }

    @Test
    public void TestSimpleArithmetic_testVectorizedLogicalOr_c4b174af036113f7ee0427a8108a980b() {
        assertEval("{ 0 | 0 }");
    }

    @Test
    public void TestSimpleArithmetic_testVectorizedLogicalOr_b4ff4fc310d8846d37ac6da649b4d360() {
        assertEval("{ 1 | 0 }");
    }

    @Test
    public void TestSimpleArithmetic_testVectorizedLogicalOr_76f9397ea21963eec828f63f9a933ef4() {
        assertEval("{ NA | 1 }");
    }

    @Test
    public void TestSimpleArithmetic_testVectorizedLogicalOr_57ffdae36cb4b298e36ad31392557867() {
        assertEval("{ NA | 0 }");
    }

    @Test
    public void TestSimpleArithmetic_testVectorizedLogicalOr_e1e601fdcc6d7548353e0dd448895bb1() {
        assertEval("{ 0 | NA }");
    }

    @Test
    public void TestSimpleArithmetic_testVectorizedLogicalOr_47cf0ab59c59bc81609a25d1ec2b89cc() {
        assertEval("{ x <- 1 ; f <- function(r) { x <<- 2; r } ; NA | f(NA) ; x }");
    }

    @Test
    public void TestSimpleArithmetic_testVectorizedLogicalOr_d4c237628c6a4eb2348915e768b49ef5() {
        assertEval("{ x <- 1 ; f <- function(r) { x <<- 2; r } ; TRUE | f(FALSE) ; x }");
    }

    @Test
    public void TestSimpleArithmetic_testVectorizedLogicalOr_69f6945f2d55d3e3e5571e2bfb563409() {
        assertEval("{ a <- as.raw(200) ; b <- as.raw(255) ; a | b }");
    }

    @Test
    public void TestSimpleArithmetic_testVectorizedLogicalOr_b0e626937a889ce9724ab2f04295d8ea() {
        assertEval("{ a <- as.raw(200) ; b <- as.raw(1) ; a | b }");
    }

    @Test
    public void TestSimpleArithmetic_testVectorizedLogicalOrAsFunction_2430569ece928348fbb44fe735610d35() {
        assertEval("{ f <- function(a,b) { a | b } ; f(c(TRUE, FALSE), FALSE) ; f(1L, 3+4i) }");
    }

    @Test
    public void TestSimpleArithmetic_testVectorizedLogicalOrAsFunction_8692d06bcd0e170b9e6ff8a8a28ff292() {
        assertEval("{ f <- function(a,b) { a | b } ; f(c(TRUE, FALSE), FALSE) ; f(c(FALSE,FALSE), 3+4i) }");
    }

    @Test
    public void TestSimpleArithmetic_testVectorizedLogicalOrAsFunction_bef95365abd9cf0ceadca09e5a20a7d4() {
        assertEval("{ f <- function(a,b) { a | b } ; f(as.raw(c(1,4)), as.raw(3)) ; f(4, FALSE) }");
    }

    @Test
    public void TestSimpleArithmetic_testVectorizedLogicalTypeCheck_f868a7be75215431a6b1334cf3089025() {
        assertEvalError("{ TRUE | \"hello\" }");
    }

    @Test
    public void TestSimpleArithmetic_testVectorizedLogicalTypeCheck_887fdea1382cb85634f10bc002f163eb() {
        assertEvalError("{ f <- function(a,b) { a & b } ; f(TRUE, 1L) ; f(as.raw(10), 12) }");
    }

    @Test
    public void TestSimpleArithmetic_testVectorizedLogicalTypeCheck_034e6243fe235667b84b772832030996() {
        assertEvalError("{ f <- function(a,b) { a & b } ; f(TRUE, 1L) ; f(FALSE, as.raw(10)) }");
    }

    @Test
    public void TestSimpleArithmetic_testVectorizedLogicalTypeCheck_9c7b0a4dbe4f23a2cf145fb069db6b35() {
        assertEvalError("{ f <- function(a,b) { a | b } ; f(as.raw(c(1,4)), as.raw(3)) ; f(as.raw(4), FALSE) }");
    }

    @Test
    public void TestSimpleArithmetic_testVectorizedLogicalTypeCheck_d9206d5a6320512214fdd11e28bf5e0c() {
        assertEvalError("{ f <- function(a,b) { a | b } ; f(as.raw(c(1,4)), as.raw(3)) ; f(FALSE, as.raw(4)) }");
    }

    @Test
    public void TestSimpleArithmetic_testVectorizedLogicalTypeCheck_9bfd2b61f4f0894c323c9288e12f46cb() {
        assertEvalError("{ f <- function(a,b) { a | b } ; f(as.raw(c(1,4)), 3) }");
    }

    @Test
    public void TestSimpleArithmetic_testVectorizedLogicalTypeCheck_c4ba4c76b8ad6f97f56be65e5b4d264e() {
        assertEvalError("{ f <- function(a,b) { a | b } ; f(3, as.raw(c(1,4))) }");
    }

    @Test
    public void TestSimpleArithmetic_testVectors_f80723b7de60ac18a6b3b5470731e564() {
        assertEval("{ x<-c(1,2,3);x }");
    }

    @Test
    public void TestSimpleArithmetic_testVectors_34f65f65153f9257170190104729625a() {
        assertEval("{ x<-c(1,2,3);x*2 }");
    }

    @Test
    public void TestSimpleArithmetic_testVectors_be19c53a4eb27216e05eb4cd073d99f3() {
        assertEval("{ x<-c(1,2,3);x+2 }");
    }

    @Test
    public void TestSimpleArithmetic_testVectors_821c50f9c93d4ce0aea9286a77ed2db7() {
        assertEval("{ x<-c(1,2,3);x+FALSE }");
    }

    @Test
    public void TestSimpleArithmetic_testVectors_228f18c5da08d4fd51bbc6c1c69b6713() {
        assertEval("{ x<-c(1,2,3);x+TRUE }");
    }

    @Test
    public void TestSimpleArithmetic_testVectors_48ccb4bb17db71eb78a647a74eeb787b() {
        assertEval("{ x<-c(1,2,3);x*x+x }");
    }

    @Test
    public void TestSimpleArithmetic_testVectors_1b300b87828d2ca05c219061a36b288a() {
        assertEval("{ x<-c(1,2);y<-c(3,4,5,6);x+y }");
    }

    @Test
    public void TestSimpleArithmetic_testVectors_d3bab0ec10d77b6597f006889753b101() {
        assertEval("{ x<-c(1,2);y<-c(3,4,5,6);x*y }");
    }

    @Test
    public void TestSimpleArithmetic_testVectors_b50fea46c43f35e96b3a302121bb91d1() {
        assertEval("{ x<-c(1,2);z<-c();x==z }");
    }

    @Test
    public void TestSimpleArithmetic_testVectors_1b82c40b26b8cf7b0fbcbe3824f88c4c() {
        assertEval("{ x<-1+NA; c(1,2,3,4)+c(x,10) }");
    }

    @Test
    public void TestSimpleArithmetic_testVectors_3af9364ff2e305fca42b6ac0c67b74e2() {
        assertEval("{ c(1L,2L,3L)+TRUE }");
    }

    @Test
    public void TestSimpleArithmetic_testVectors_a7ae4e1fb9e04ff93535700cac1b9626() {
        assertEval("{ c(1L,2L,3L)*c(10L) }");
    }

    @Test
    public void TestSimpleArithmetic_testVectors_7cb740d8b5fcf7c540662f91dad2e91b() {
        assertEval("{ c(1L,2L,3L)*c(10,11,12) }");
    }

    @Test
    public void TestSimpleArithmetic_testVectors_d373d97eedff5f0181b5fe756156b92a() {
        assertEval("{ c(1L,2L,3L,4L)-c(TRUE,FALSE) }");
    }

    @Test
    public void TestSimpleArithmetic_testVectors_afaba6a25ddbe6025cb1c132b4f667a1() {
        assertEval("{ ia<-c(1L,2L);ib<-c(3L,4L);d<-c(5,6);ia+ib+d }");
    }

    @Test
    public void TestSimpleArithmetic_testVectorsComplex_e622aa08e36d1dd2f9cfc2673541bf62() {
        assertEval("{ 1:4+c(1,2+2i) }");
    }

    @Test
    public void TestSimpleArithmetic_testVectorsComplex_495842139f1f043773b2c44f8d1fa6ef() {
        assertEval("{ c(1,2+2i)+1:4 }");
    }

    @Test
    public void TestSimpleArithmetic_testVectorsEmptyResult_38742bfc0f5ea992de288b0a1ed044ad() {
        assertEval("{ integer()+1 }");
    }

    @Test
    public void TestSimpleArithmetic_testVectorsEmptyResult_0d391d4da490d194323b1e179da0f703() {
        assertEval("{ 1+integer() }");
    }

    @Test
    public void TestSimpleArithmetic_testVectorsIntegerDivision_287ca51f5ca9733a25e760245f79bd31() {
        assertEval("{ c(3,4) %/% 2 }");
    }

    @Test
    public void TestSimpleArithmetic_testVectorsLengthWarning_434cf402275d72887df7f3d5075408bc() {
        assertEvalWarning("{ 1:2+1:3 }");
    }

    @Test
    public void TestSimpleArithmetic_testVectorsLengthWarning_fb266e5d477400a227beb2a990776758() {
        assertEvalWarning("{ 1:3*1:2 }");
    }

    @Test
    public void TestSimpleArithmetic_testVectorsLengthWarning_61d2c11726af0b5cec1c3b100370b905() {
        assertEvalWarning("{ 1:3+c(1,2+2i) }");
    }

    @Test
    public void TestSimpleArithmetic_testVectorsLengthWarning_ea160ab7a388f1aea439e3233d7e21eb() {
        assertEvalWarning("{ c(1,2+2i)+1:3 }");
    }

    @Test
    public void TestSimpleArithmetic_testVectorsMatrixDimsDontMatch_4715846ee2436cdade5e4275aac886d9() {
        assertEvalError("{ m <- matrix(nrow=2, ncol=2, 1:4) ; m + 1:16 }");
    }

    @Test
    public void TestSimpleArithmetic_testVectorsModulo_b27fdb1ffd2c7031e99ad3d04668cd7d() {
        assertEval("{ c(3,4) %% 2 }");
    }

    @Test
    public void TestSimpleArithmetic_testVectorsModulo_fb8ce13c65036336eb29ab9d752e11f7() {
        assertEval("{ c(3,4) %% c(2,5) }");
    }

    @Test
    public void TestSimpleArithmetic_testVectorsNA_a5857e53025b324d5f4deb9b4b235ebc() {
        assertEval("{ 1 + c(1L, NA, 3L) }");
    }

    @Test
    public void TestSimpleArithmetic_testVectorsNA_0f8c20fd1e2b4c124458626fc6cd1beb() {
        assertEval("{ NA + c(1, 2, 3) }");
    }

    @Test
    public void TestSimpleArithmetic_testVectorsNA_5d6cc134026a2a400bf806097ac470ac() {
        assertEval("{ c(1, 2, 3) + NA }");
    }

    @Test
    public void TestSimpleArithmetic_testVectorsNA_1c476841e9f55094d52c1f95265cd91d() {
        assertEval("{ NA+1:3 }");
    }

    @Test
    public void TestSimpleArithmetic_testVectorsNA_76d8c8bce4a15fcc8297e780421770a6() {
        assertEval("{ 1:3+NA }");
    }

    @Test
    public void TestSimpleArithmetic_testVectorsNA_604d28e7ede894739a1609570052594c() {
        assertEval("{ NA+c(1L, 2L, 3L) }");
    }

    @Test
    public void TestSimpleArithmetic_testVectorsNA_cb302da14588e4889ab571d621e18e8f() {
        assertEval("{ c(1L, 2L, 3L)+NA }");
    }

    @Test
    public void TestSimpleArithmetic_testVectorsNA_3bb135d75c81a195cf76b5f7a36cc0d3() {
        assertEval("{ c(NA,NA,NA)+1:3 }");
    }

    @Test
    public void TestSimpleArithmetic_testVectorsNA_def2344e6000a7ccd48ad858c2398682() {
        assertEval("{ 1:3+c(NA, NA, NA) }");
    }

    @Test
    public void TestSimpleArithmetic_testVectorsNA_06944cdfa4127476e2371734429c10b0() {
        assertEval("{ c(NA,NA,NA)+c(1L,2L,3L) }");
    }

    @Test
    public void TestSimpleArithmetic_testVectorsNA_c54a2a35a843a4efa33a69c195795da2() {
        assertEval("{ c(1L,2L,3L)+c(NA, NA, NA) }");
    }

    @Test
    public void TestSimpleArithmetic_testVectorsNA_389c85a610a9c0cd0f6007abd550b46a() {
        assertEval("{ c(NA,NA)+1:4 }");
    }

    @Test
    public void TestSimpleArithmetic_testVectorsNA_a5309b597099b264b82547881fc98b17() {
        assertEval("{ 1:4+c(NA, NA) }");
    }

    @Test
    public void TestSimpleArithmetic_testVectorsNA_4131f1be62ff9a0191bc989cb2436885() {
        assertEval("{ c(NA,NA,NA,NA)+1:2 }");
    }

    @Test
    public void TestSimpleArithmetic_testVectorsNA_01c5e2b08650eb230a0648d6a7da8659() {
        assertEval("{ 1:2+c(NA,NA,NA,NA) }");
    }

    @Test
    public void TestSimpleArithmetic_testVectorsNA_abc9da17ffde5a966e4d36f456657e12() {
        assertEval("{ c(NA,NA)+c(1L,2L,3L,4L) }");
    }

    @Test
    public void TestSimpleArithmetic_testVectorsNA_86bcdf8eb2b052700f43be978cb0bb13() {
        assertEval("{ c(1L,2L,3L,4L)+c(NA, NA) }");
    }

    @Test
    public void TestSimpleArithmetic_testVectorsNA_b74304d533411ff2c792d6568ba55548() {
        assertEval("{ c(NA,NA,NA,NA)+c(1L,2L) }");
    }

    @Test
    public void TestSimpleArithmetic_testVectorsNA_904e436f90553249725ca6e3ff9229b9() {
        assertEval("{ c(1L,2L)+c(NA,NA,NA,NA) }");
    }

    @Test
    public void TestSimpleArithmetic_testVectorsNA_fdec335e4c4b3ccf37bdaf8e1b548683() {
        assertEval("{ c(1L,NA)+1 }");
    }

    @Test
    public void TestSimpleArithmetic_testVectorsNA_6e5a44d28f18bd91c2999d3b0bd19848() {
        assertEval("{ c(1L,NA) + c(2,3) }");
    }

    @Test
    public void TestSimpleArithmetic_testVectorsNA_1e9071a9e93ec415aa6f276e3a88b146() {
        assertEval("{ c(2,3) + c(1L,NA)}");
    }

    @Test
    public void TestSimpleArithmetic_testVectorsNonConformable_2b9020c835ed09c8aa45ff026f9859aa() {
        assertEvalError("{ x <- 1:2 ; dim(x) <- 1:2 ; y <- 2:3 ; dim(y) <- 2:1 ; x + y }");
    }

    @Test
    public void TestSimpleArithmetic_testVectorsNonConformable_a631557f258a6d6c95c89ddc20e555f2() {
        assertEvalError("{ x <- 1:2 ; dim(x) <- 1:2 ; y <- 2:3 ; dim(y) <- c(1,1,2) ; x + y }");
    }

    @Test
    public void TestSimpleArithmetic_testVectorsOperations_44d255712be5c567274f75b350d734f8() {
        assertEval("{ a <- c(1,3) ; b <- c(2,4) ; a ^ b }");
    }

    @Test
    public void TestSimpleArithmetic_testVectorsOperations_f31fa3f2579eabcfbcf0418c144a8306() {
        assertEval("{ a <- c(1,3) ; a ^ 3 }");
    }

    @Test
    public void TestSimpleArithmetic_testVectorsOperations_7c0266100b32126e72a4ddf0cd65d4d0() {
        assertEval("{ c(1,3) - 4 }");
    }

    @Test
    public void TestSimpleArithmetic_testVectorsOperations_fddeb727d1c62277c4675da71cb8dba8() {
        assertEval("{ c(1,3) %/% c(2,4) }");
    }

    @Test
    public void TestSimpleArithmetic_testVectorsOperationsComplex_c4f88c1fe9fb8ce16f6132db7a2b6305() {
        assertEval("{ a <- c(1+1i,3+2i) ; a - (4+3i) }");
    }

    @Test
    public void TestSimpleArithmetic_testVectorsOperationsComplex_4060ff48dc1f976360e6a709c8569f77() {
        assertEval("{ c(1+1i,3+2i) * c(1,2) }");
    }

    @Test
    public void TestSimpleArithmetic_testVectorsOperationsComplex_4a0fafc4eb5c9674d9ef06057a41b601() {
        assertEval("{ z <- c(1+1i,3+2i) ; z * c(1,2) }");
    }

    @Test
    public void TestSimpleArithmetic_testVectorsOperationsComplex_26cbb52252ff028ab7bba252147de26c() {
        assertEval("{ round(c(1+1i,2+3i)^c(1+1i,3+4i), digits = 5) }");
    }

    @Test
    public void TestSimpleArithmetic_testVectorsOperationsComplex_a88a08c3bbe4807e9507763726c626e5() {
        assertEval("{ round( 3^c(1,2,3+1i), digits=5 ) }");
    }

    @Test
    public void TestSimpleArithmetic_testVectorsOperationsComplexIgnore_9b81d167391e44e04a528a367013f806() {
        assertEval("{ z <- c(-1.5-1i,10) ; (z * z)[1] }");
    }

    @Test
    public void TestSimpleArithmetic_testVectorsOperationsComplexIgnore_0dae874162cc69c107cdd6f0c5ea334c() {
        assertEval("{ c(1+1i,3+2i) / 2 }");
    }

    @Test
    public void TestSimpleArithmetic_testVectorsOperationsComplexIgnore_15a6502f9ece8e54a080a3e20541165c() {
        assertEval("{ c(1,2,3+1i)^3 }");
    }

    @Test
    public void TestSimpleArithmetic_testVectorsOperationsIgnore_bc2cc92da6012e61c40e913719b41e8a() {
        assertEval("{ c(1,3) / c(2,4) }");
    }

    @Test
    public void TestSimpleArithmetic_testVectorsRanges_27adc38facd51fc9f2e69fe5c3e56d30() {
        assertEval("{ 1L + 1:2 }");
    }

    @Test
    public void TestSimpleArithmetic_testVectorsRanges_2d4bfded3bdfd86958589973e46fd5b2() {
        assertEval("{ 4:3 + 2L }");
    }

    @Test
    public void TestSimpleArithmetic_testVectorsRanges_47de5333c035a98dc383da3e3f2602d5() {
        assertEval("{ 1:2 + 3:4 }");
    }

    @Test
    public void TestSimpleArithmetic_testVectorsRanges_fb366fe79110c95d1316ac6b5bd480b0() {
        assertEval("{ 1:2 + c(1L, 2L) }");
    }

    @Test
    public void TestSimpleArithmetic_testVectorsRanges_29d5158083ef18879fe68fe720a2742e() {
        assertEval("{ c(1L, 2L) + 1:4 }");
    }

    @Test
    public void TestSimpleArithmetic_testVectorsRanges_a84190f22c5befb7c83934e5f2944312() {
        assertEval("{ 1:4 + c(1L, 2L) }");
    }

    @Test
    public void TestSimpleArithmetic_testVectorsRanges_5c4bfbf270c5f6b3c7387ebb96684184() {
        assertEval("{ 2L + 1:2 }");
    }

    @Test
    public void TestSimpleArithmetic_testVectorsRanges_e1699b2805a46685fcd75e71f3ab60b8() {
        assertEval("{ 1:2 + 2L }");
    }

    @Test
    public void TestSimpleArithmetic_testVectorsRanges_7204e59747c1de8e9a67feea744e7ff9() {
        assertEval("{ c(1L, 2L) + 2L }");
    }

    @Test
    public void TestSimpleArithmetic_testVectorsRanges_6a18ba526051281ca2ce4a5a88e2a902() {
        assertEval("{ 2L + c(1L, 2L) }");
    }

    @Test
    public void TestSimpleArithmetic_testVectorsRanges_efe1b59f2dce52ed7f3137c6ab6bdc4c() {
        assertEval("{ 1 + 1:2 }");
    }

    @Test
    public void TestSimpleArithmetic_testVectorsRanges_3e38fa21a22f6dc0a775a50cbc74008c() {
        assertEval("{ c(1,2) + 1:2 }");
    }

    @Test
    public void TestSimpleArithmetic_testVectorsRanges_4c6d9e6202bf256ee14820189453d6cf() {
        assertEval("{ c(1,2,3,4) + 1:2 }");
    }

    @Test
    public void TestSimpleArithmetic_testVectorsRanges_9b2256415b8ffeccb2a14ac443764148() {
        assertEval("{ c(1,2,3,4) + c(1L,2L) }");
    }

    @Test
    public void TestSimpleArithmetic_testVectorsRanges_fa668c992ee5138c354eb51448cd2c3c() {
        assertEval("{ 1:2 + 1 }");
    }

    @Test
    public void TestSimpleArithmetic_testVectorsRanges_34f66de4699725129b1265d8d2cf10ea() {
        assertEval("{ 1:2 + c(1,2) }");
    }

    @Test
    public void TestSimpleArithmetic_testVectorsRanges_9417977c7df5c9385d859ffeb31c707e() {
        assertEval("{ 1:2 + c(1,2,3,4) }");
    }

    @Test
    public void TestSimpleArithmetic_testVectorsRanges_27a8646ab027e9cb9a674cfb97c43dd7() {
        assertEval("{ c(1L,2L) + c(1,2,3,4) }");
    }

    @Test
    public void TestSimpleArithmetic_testVectorsRanges_a9df6de7b9778ea074439b3477024f46() {
        assertEval("{ 1L + c(1,2) }");
    }

    @Test
    public void TestSimpleArithmetic_testVectorsRanges_0b81720c4f35708568cfb4c05e4922a5() {
        assertEval("{ 1:4+c(1,2) }");
    }

    @Test
    public void TestSimpleArithmetic_testVectorsRanges_a73289342c5df58c715caa68dcea4846() {
        assertEval("{ c(1,2)+1:4 }");
    }

    @Test
    public void TestSimpleArithmetic_testXor_79ad34585fbc7363e71021912c02f78c() {
        assertEval(" xor(FALSE, FALSE) ");
    }

    @Test
    public void TestSimpleArithmetic_testXor_ca68839175a6f107f85a28531ce3983e() {
        assertEval(" xor(TRUE, FALSE) ");
    }

    @Test
    public void TestSimpleArithmetic_testXor_e5021d833ae597222abb6a8487a23ddc() {
        assertEval(" xor(FALSE, TRUE) ");
    }

    @Test
    public void TestSimpleArithmetic_testXor_30407db58b1526d76ec4d7ad7c6967c6() {
        assertEval(" xor(TRUE, TRUE) ");
    }

    @Test
    public void TestSimpleArithmetic_testXor_69e3c453ca02fe06d1ad0b5373676747() {
        assertEval("{ xor(7, 42) }");
    }

    @Test
    public void TestSimpleArithmetic_testXor_657e3fa183c042e6e05b5576ce2dd4b2() {
        assertEval("{ xor(0:2, 2:4) }");
    }

    @Test
    public void TestSimpleArithmetic_testXor_55adab1407fcef52ee9e5c927aeca8ab() {
        assertEval("{ xor(0:2, 2:7) }");
    }

    @Test
    public void TestSimpleArrays_testAccess_7fa54d8eeff2e2fcbd82d3b6fb833e1a() {
        assertEval("{ x<-1:8; dim(x)<-c(1,2,4); dim(x[1,0,3]) }");
    }

    @Test
    public void TestSimpleArrays_testAccess_30488b5c545c0e320762798f7305f7fa() {
        assertEval("{ x<-1:8; dim(x)<-c(1,2,4); dim(x[1,0,-1]) }");
    }

    @Test
    public void TestSimpleArrays_testAccess_d117396db8313c46cdbbf79266c28505() {
        assertEval("{ x<-1:8; dim(x)<-c(1,2,4); dim(x[0,1,-1]) }");
    }

    @Test
    public void TestSimpleArrays_testAccess_41d367ba3c4c89c8195385d388de8b6d() {
        assertEval("{ x<-1:16; dim(x)<-c(2,2,4); dim(x[0,-1,-1]) }");
    }

    @Test
    public void TestSimpleArrays_testAccess_7d11b1e88c5059abb23bffd0989aca65() {
        assertEval("{ x<-1:64; dim(x)<-c(4,4,4); dim(x[0,-1,-1]) }");
    }

    @Test
    public void TestSimpleArrays_testAccess_0b1267815c33d30fc5e514e4433e4ac5() {
        assertEval("{ x<-1:64; dim(x)<-c(4,4,4); dim(x[0,1,-1]) }");
    }

    @Test
    public void TestSimpleArrays_testAccess_05c0e7197d7f7b6257e67a59009f33aa() {
        assertEval("{ x<-1:64; dim(x)<-c(4,4,4); dim(x[1,0,-1]) }");
    }

    @Test
    public void TestSimpleArrays_testAccess_ada18605e29ac6095a99084e74ae774d() {
        assertEval("{ x<-1:64; dim(x)<-c(4,4,2,2); dim(x[1,1, 0,-1]) }");
    }

    @Test
    public void TestSimpleArrays_testAccess_f5e7eac90323ccac9d2114dd45e9a1ec() {
        assertEval("{ x<-1:256; dim(x)<-c(4,4,4,4); dim(x[1,1, 0,-1]) }");
    }

    @Test
    public void TestSimpleArrays_testAccess_19bb6fb6d17321586bff51b634724483() {
        assertEval("{ x<-1:64; dim(x)<-c(4,4,2,2); dim(x[1,0, 1,-1]) }");
    }

    @Test
    public void TestSimpleArrays_testAccess_65425321a23596811695f54295559b47() {
        assertEval("{ x<-1:256; dim(x)<-c(4,4,4,4); dim(x[1,0, 1,-1]) }");
    }

    @Test
    public void TestSimpleArrays_testAccess_a51f3c1b971cf35008b9df963a6e492b() {
        assertEval("{ x<-1:8; dim(x)<-c(2,2,2); dim(x[1,0,1]) }");
    }

    @Test
    public void TestSimpleArrays_testAccess_9586dd6fd7d9547734b53c25053b8fb5() {
        assertEval("{ x<-1:8; dim(x)<-c(1,2,4); dim(x[0,,-1]) }");
    }

    @Test
    public void TestSimpleArrays_testAccess_a7c65ae48cd3dd54408819e26912ad78() {
        assertEval("{ x<-1:16; dim(x)<-c(2,2,4); x[,1,1] }");
    }

    @Test
    public void TestSimpleArrays_testAccess_543f9f5c22bc2ff25cefa7f0618e29e1() {
        assertEval("{ x<-1:64; dim(x)<-c(4,4,4); x[-1,-1,1] }");
    }

    @Test
    public void TestSimpleArrays_testAccess_cbd7ca93ab9d577f831eafa6349041d7() {
        assertEval("{ x<-1:64; dim(x)<-c(4,4,4); x[-1,1,3] }");
    }

    @Test
    public void TestSimpleArrays_testAccess_7d00e6dea95ddefdda84abd23fd3d406() {
        assertEval("{ x<-1:64; dim(x)<-c(4,4,4); x[1,1,3] }");
    }

    @Test
    public void TestSimpleArrays_testAccess_34c47ad746fccb44a4f34801d947f650() {
        assertEval("{ x<-1:32; dim(x)<-c(4,2,4); dim(x[-1,1,1]) }");
    }

    @Test
    public void TestSimpleArrays_testAccess_c1ec7dc4d096ca84175f761d8ec651cc() {
        assertEval("{ x<-1:16; dim(x)<-c(4,1,4); dimnames(x)<-list(c(\"a\", \"b\", \"c\", \"d\"), NULL, c(\"e\", \"f\", \"g\", \"h\")); x[-1,1,1] }");
    }

    @Test
    public void TestSimpleArrays_testAccess_63b17d90cab5471a37d0af95ebdf80d8() {
        assertEval("{ x<-1:16; dim(x)<-c(4,1,4); dimnames(x)<-list(c(\"a\", \"b\", \"c\", \"d\"), NULL, c(\"e\", \"f\", \"g\", \"h\")); x[-1,1,-1] }");
    }

    @Test
    public void TestSimpleArrays_testAccess_03c83c893e63d579cad55778f668daf6() {
        assertEval("{ x<-1:16; dim(x)<-c(4,1,4); dimnames(x)<-list(c(\"a\", \"b\", \"c\", \"d\"), \"z\", c(\"e\", \"f\", \"g\", \"h\")); dimnames(x[-1,1,-1]) }");
    }

    @Test
    public void TestSimpleArrays_testAccess_4cad27eceba31418e643ea7cbd473d6e() {
        assertEval("{ x<-1:32; dim(x)<-c(4,2,4); dimnames(x)<-list(c(\"a\", \"b\", \"c\", \"d\"), c(\"x\", \"y\"), c(\"e\", \"f\", \"g\", \"h\")); x[-1,,-1] }");
    }

    @Test
    public void TestSimpleArrays_testAccess_dfc17a2fe684a22920890e218fe959a4() {
        assertEval("{ x<-1:32; dim(x)<-c(4,2,4); dimnames(x)<-list(c(\"a\", \"b\", \"c\", \"d\"), c(\"x\", \"y\"), c(\"e\", \"f\", \"g\", \"h\")); x[1,1,1] }");
    }

    @Test
    public void TestSimpleArrays_testAccess_ee1652f88128ccc0503cd3422496bcb4() {
        assertEval("{ x<-1:32; dim(x)<-c(4,2,4); dimnames(x)<-list(c(\"a\", \"b\", \"c\", \"d\"), NULL, c(\"e\", \"f\", \"g\", \"h\")); x[-1,,-1] }");
    }

    @Test
    public void TestSimpleArrays_testAccess_afa286aa27a17ea8616763b60cbe88ee() {
        assertEval("{ x<-1:32; dim(x)<-c(4,2,4); dimnames(x)<-list(NULL, c(\"x\", \"y\"), c(\"e\", \"f\", \"g\", \"h\")); x[-1,1,1] }");
    }

    @Test
    public void TestSimpleArrays_testAccess_088279681c71efb751e713c6755e57ea() {
        assertEval("{ x<-(1:8); dim(x)<-c(2, 2, 2); dimnames(x)<-list(c(\"a\", \"b\"), c(\"c\", \"d\"), c(\"e\", \"f\")) ;x[1,1,] }");
    }

    @Test
    public void TestSimpleArrays_testAccess_fd05e785deabe1d0f368d9a70c3ac57a() {
        assertEval("{ x<-(1:8); dim(x)<-c(2, 2, 2); dimnames(x)<-list(c(\"a\", \"b\"), c(\"c\", \"d\"), c(\"e\", \"f\")) ;x[,0,] }");
    }

    @Test
    public void TestSimpleArrays_testAccess_773947cda5ed2e582faff115cc4ccf26() {
        assertEval("{ x<-(1:8); dim(x)<-c(2, 2, 2); x[1,1,NA] }");
    }

    @Test
    public void TestSimpleArrays_testAccess_20bd8218f895f981e730a9bec83d0c10() {
        assertEval("{ x<-(1:8); dim(x)<-c(2, 2, 2); x[1,1,c(1,NA,1)] }");
    }

    @Test
    public void TestSimpleArrays_testAccess_56524fd2e88d5f6d8715c4690b9c9396() {
        assertEval("{ x<-(1:8); dim(x)<-c(2, 2, 2); x[NA,1,c(1,NA,1)] }");
    }

    @Test
    public void TestSimpleArrays_testAccess_d56f875384f0b2d413684966b21acaa9() {
        assertEval("{ x<-(1:8); dim(x)<-c(2, 2, 2); dimnames(x)<-list(c(\"a\", \"b\"), c(\"c\", \"d\"), c(\"e\", \"f\")) ;x[1,1,NA] }");
    }

    @Test
    public void TestSimpleArrays_testAccess_5fcb47c406216ea32092925c28dfb4d8() {
        assertEval("{ x<-(1:8); dim(x)<-c(2, 2, 2); dimnames(x)<-list(c(\"a\", \"b\"), c(\"c\", \"d\"), c(\"e\", \"f\")); x[1,1,c(1,NA,1)] }");
    }

    @Test
    public void TestSimpleArrays_testAccess_c4e20adecb559e12c3b7db6be5f3a515() {
        assertEval("{ x<-(1:8); dim(x)<-c(2, 2, 2); dimnames(x)<-list(c(\"a\", \"b\"), c(\"c\", \"d\"), c(\"e\", \"f\")); x[NA,1,c(1,NA,1)] }");
    }

    @Test
    public void TestSimpleArrays_testAccess_587ad0c9609dc4123c9590f037813a27() {
        assertEval("{ x<-(1:8); dim(x)<-c(2, 2, 2); dim(x[0,0,0]) }");
    }

    @Test
    public void TestSimpleArrays_testAccess_23a36b90ec9ac75b7f4740c6bf7247ab() {
        assertEval("{ x<-(1:8); dim(x)<-c(2, 2, 2); x[0,0,1] }");
    }

    @Test
    public void TestSimpleArrays_testAccess_ea7a54f82e5a448af49459e63e2f3a5c() {
        assertEvalError("{ x<-1:8; dim(x)<-c(2,2,2); x[1, 1, 1, 1] }");
    }

    @Test
    public void TestSimpleArrays_testAccess_edb35c675f513b9c006ede06699f3cad() {
        assertEvalError("{ x<-1:8; dim(x)<-c(2,2,2); x[42,1,1] }");
    }

    @Test
    public void TestSimpleArrays_testAccess_19411bf3b70f9001e93b99dcfc55bcad() {
        assertEvalError("{ x<-1:8; dim(x)<-c(2,2,2); x[[, 1, 1]] }");
    }

    @Test
    public void TestSimpleArrays_testArrayBuiltin_9a47dd99312e693ad52405e33fe75b04() {
        assertEval("{ a = array(); length(a) }");
    }

    @Test
    public void TestSimpleArrays_testArrayBuiltin_1a73a6d88a8f538984fb22368c9412bb() {
        assertEval("{ a = array(); is.na(a[1]) }");
    }

    @Test
    public void TestSimpleArrays_testArrayBuiltin_9e9890dca281ce611bb5bd48143b9b09() {
        assertEval("{ a = array(); is.null(dimnames(a)); }");
    }

    @Test
    public void TestSimpleArrays_testArrayBuiltin_3d8969238024eab6bebc08a29ff468e7() {
        assertEval("{ a <- array(); dim(a) }");
    }

    @Test
    public void TestSimpleArrays_testArrayBuiltin_06425e22638ed1a1ea10f1f3a8920693() {
        assertEval("{ a = array(1:10, dim = c(2,6)); length(a) }");
    }

    @Test
    public void TestSimpleArrays_testArrayBuiltin_d0ed6bf6a52a56790dccad57cfecdd08() {
        assertEval("{ length(array(NA, dim=c(1,0,2,3))) }");
    }

    @Test
    public void TestSimpleArrays_testArrayBuiltin_155b552decb85f600b252aa86008ce29() {
        assertEval("{ dim(array(NA, dim=c(2.1,2.9,3.1,4.7))) }");
    }

    @Test
    public void TestSimpleArrays_testArrayBuiltin_e57eb5be20feb32f2d83ebb6866057c9() {
        assertEvalError("{ array(NA, dim=c(-2,2)); }");
    }

    @Test
    public void TestSimpleArrays_testArrayBuiltin_3b72d275119cfc0cbdf526fbd5ecf594() {
        assertEvalError("{ array(NA, dim=c(-2,-2)); }");
    }

    @Test
    public void TestSimpleArrays_testArraySimpleRead_29eb8d92d01f2a70b40ed74e26a1d55a() {
        assertEval("{ a = array(1:27,c(3,3,3)); c(a[1,1,1],a[3,3,3],a[1,2,3],a[3,2,1]) }");
    }

    @Test
    public void TestSimpleArrays_testArraySimpleRead_c3c3aa99fdb0575dba707c054acc7001() {
        assertEval("{ a = array(1:27, c(3,3,3)); b = a[,,]; d = dim(b); c(d[1],d[2],d[3]) }");
    }

    @Test
    public void TestSimpleArrays_testArraySimpleRead_b977c8c8366306c3295e9a41241295aa() {
        assertEval("{ a = array(1,c(3,3,3)); a = dim(a[,1,]); c(length(a),a[1],a[2]) }");
    }

    @Test
    public void TestSimpleArrays_testArraySimpleRead_46223217c55f14bde7338b03ac9a89da() {
        assertEval("{ a = array(1,c(3,3,3)); is.null(dim(a[1,1,1])) }");
    }

    @Test
    public void TestSimpleArrays_testArraySimpleRead_4f8ddffb583fcf44319c51bfa21736f2() {
        assertEval("{ a = array(1,c(3,3,3)); is.null(dim(a[1,1,])) } ");
    }

    @Test
    public void TestSimpleArrays_testArraySimpleRead_1f676b8c4e884a24f7214afb77a23299() {
        assertEval("{ a = array(1,c(3,3,3)); a = dim(a[1,1,1, drop = FALSE]); c(length(a),a[1],a[2],a[3]) }");
    }

    @Test
    public void TestSimpleArrays_testArraySimpleRead_c02732f112fcc9ca168e5026287e8d1c() {
        assertEval("{ m <- array(1:4, dim=c(4,1,1)) ; x <- m[[2,1,1,drop=FALSE]] ; is.null(dim(x)) }");
    }

    @Test
    public void TestSimpleArrays_testArraySimpleRead_324c037a80f2eca786f087bfbbf7eaf6() {
        assertEval("{ a = array(1:27, c(3,3,3)); c(a[1],a[27],a[22],a[6]) }");
    }

    @Test
    public void TestSimpleArrays_testArraySimpleRead_da1b6e2faf04ef67c8c682ff9b3858ff() {
        assertEval("{ m <- array(c(1,2,3), dim=c(3,1,1)) ; x <- m[1:2,1,1] ; c(x[1],x[2]) }");
    }

    @Test
    public void TestSimpleArrays_testArraySimpleRead_07d18e8c39dc1eada48537968597f808() {
        assertEval("{ m <- array(c(1,2,3), dim=c(3,1,1)) ; x <- dim(m[1:2,1,1]) ; is.null(x) }");
    }

    @Test
    public void TestSimpleArrays_testArraySimpleRead_d341018138a0670caa7710486df37f5c() {
        assertEval("{ m <- array(c(1,2,3), dim=c(3,1,1)) ; x <- dim(m[1:2,1,1,drop=FALSE]) ; c(x[1],x[2],x[3]) }");
    }

    @Test
    public void TestSimpleArrays_testArraySimpleRead_18803ee9bbdd977e41ea10fa24f0b161() {
        assertEval("{ m <- array(c(1,2,3), dim=c(3,1,1)) ; x <- m[1:2,1,integer()] ; d <- dim(x) ; length(x) }");
    }

    @Test
    public void TestSimpleArrays_testArraySimpleRead_e71dd62b8bb11ec211d08f4aa3b23015() {
        assertEval("{ m <- array(c(1,2,3), dim=c(3,1,1)) ; x <- m[1:2,1,integer()] ; d <- dim(x) ; c(d[1],d[2]) }");
    }

    @Test
    public void TestSimpleArrays_testArraySimpleRead_ddfb81111b88b8188ceda0aa75c41a4c() {
        assertEval("{ x<-1:64; dim(x)<-c(4,4,2,2); dim(x[1,1, drop=FALSE, 0, -1]) }");
    }

    @Test
    public void TestSimpleArrays_testArraySimpleRead_db3b466dd83c54ea5aa2ce7f2321bf12() {
        assertEval("{ x<-1:64; dim(x)<-c(4,4,2,2); dim(x[1,1, drop=c(0,2), 0, -1]) }");
    }

    @Test
    public void TestSimpleArrays_testArraySimpleRead_0c5b2be5ef11fdfec2097e442f53b637() {
        assertEval("{ x<-1:64; dim(x)<-c(4,4,2,2); dim(x[1,1, drop=0, 0, -1]) }");
    }

    @Test
    public void TestSimpleArrays_testArraySimpleRead_3867951b74ceb63b3cf702022967100f() {
        assertEval("{ x<-1:64; dim(x)<-c(4,4,2,2); dim(x[1,1, drop=integer(), 0, -1]) }");
    }

    @Test
    public void TestSimpleArrays_testArraySimpleRead_899066bd33233e98e571e5908e183adb() {
        assertEval("{ x<-1:64; dim(x)<-c(4,4,2,2); dim(x[1,drop=FALSE, 1, drop=TRUE, -1]) }");
    }

    @Test
    public void TestSimpleArrays_testArraySimpleRead_7b74b2887b86d6dbce9cc6dba1cdfac0() {
        assertEvalError("{ a = array(1,c(3,3,3)); a[2,2]; }");
    }

    @Test
    public void TestSimpleArrays_testArraySimpleRead_7bfdbb54d9a6446b3cc76cc29dfb78f8() {
        assertEvalError("{ x<-1:64; dim(x)<-c(4,4,2,2); dim(x[1,1, drop=FALSE, 0, drop=TRUE, -1]) }");
    }

    @Test
    public void TestSimpleArrays_testArraySubsetAndSelection_68b97da5a27b01312c95f5d56fe9076d() {
        assertEval("{ array(1,c(3,3,3))[1,1,1] }");
    }

    @Test
    public void TestSimpleArrays_testArraySubsetAndSelection_1d37dd887da3ba708333f8487ccd9f8f() {
        assertEval("{ array(1,c(3,3,3))[[1,1,1]] }");
    }

    @Test
    public void TestSimpleArrays_testArraySubsetAndSelection_efa2a1d85fabc79e8fd5db5134d2a3b9() {
        assertEval("{ m <- array(1:24, dim=c(2,3,4)) ; m[,,2] }");
    }

    @Test
    public void TestSimpleArrays_testArraySubsetAndSelection_d6bda98b11042efe7b4f5c90e1a5493e() {
        assertEval("{ m <- array(1:24, dim=c(2,3,4)) ; m[,,2,drop=FALSE] }");
    }

    @Test
    public void TestSimpleArrays_testArraySubsetAndSelection_d475d1d0faa8bd93959318ac5cafc48d() {
        assertEval("{ m <- array(1:24, dim=c(2,3,4)) ; f <- function(i) { m[,,i] } ; f(1) ; f(2) ; dim(f(1:2)) }");
    }

    @Test
    public void TestSimpleArrays_testArraySubsetAndSelection_9f1844b4c9c14d03f5117a7bddac8659() {
        assertEval("{ m <- array(1:24, dim=c(2,3,4)) ; f <- function(i) { m[,,i] } ; f(1[2]) ; f(3) }");
    }

    @Test
    public void TestSimpleArrays_testArraySubsetAndSelection_930c158db699e39727e6645602fde017() {
        assertEvalError("{ array(1,c(3,3,3))[[,,]]; }");
    }

    @Test
    public void TestSimpleArrays_testArraySubsetAndSelection_b7b4f062010fbeb7ef8c64794ad0b0cd() {
        assertEvalError("{ array(1,c(3,3,3))[[c(1,2),1,1]]; }");
    }

    @Test
    public void TestSimpleArrays_testArrayUpdate_b599fd9800450ca5bdaff3779fdc29aa() {
        assertEval("{ a = matrix(1,2,2); a[1,2] = 3; a[1,2] == 3; }");
    }

    @Test
    public void TestSimpleArrays_testArrayUpdateIgnore_b143dfae9c4c5ae3489a82b375ca1361() {
        assertEval("{ a = array(1,c(3,3,3)); c(a[1,2,3],a[1,2,3]) }");
    }

    @Test
    public void TestSimpleArrays_testArrayUpdateIgnore_fa399785b588c0934381a477dc943405() {
        assertEval("{ a = array(1,c(3,3,3)); a[1,2,3] = 3; a }");
    }

    @Test
    public void TestSimpleArrays_testArrayUpdateIgnore_c8ec0a7ad6d6e5de78f3113e7eac4558() {
        assertEval("{ a = array(1,c(3,3,3)); b = a; b[1,2,3] = 3; c(a[1,2,3],b[1,2,3]) }");
    }

    @Test
    public void TestSimpleArrays_testArrayUpdateIgnore_74477ab1a1ca6b600db67b4629a76e8b() {
        assertEval("{ x <- array(c(1,2,3), dim=c(3,1,1)) ; x[1:2,1,1] <- sqrt(x[2:1]) ; c(x[1] == sqrt(2), x[2], x[3]) }");
    }

    @Test
    public void TestSimpleArrays_testBugIfiniteLoopInGeneralizedRewriting_0947755b213127f97cc94793b7086114() {
        assertEval("{ m <- array(1:3, dim=c(3,1,1)) ; f <- function(x,v) { x[1:2,1,1] <- v ; x } ; f(m,10L) ; f(m,10) ; f(m,c(11L,12L)); c(m[1,1,1],m[2,1,1],m[3,1,1]) }");
    }

    @Test
    public void TestSimpleArrays_testDefinitionsIgnore_57b0a537728276c59318eff6c37f368e() {
        assertEval("{ matrix( as.raw(101:106), nrow=2 ) }");
    }

    @Test
    public void TestSimpleArrays_testDefinitionsIgnore_acc2ff6e352fc26dfe6658d5d25a6581() {
        assertEval("{ m <- matrix(1:6, ncol=3, byrow=TRUE) ; m }");
    }

    @Test
    public void TestSimpleArrays_testDefinitionsIgnore_e2e3e47a6687353d12f0cfb25c473e24() {
        assertEval("{ m <- matrix(1:6, nrow=2, byrow=TRUE) ; m }");
    }

    @Test
    public void TestSimpleArrays_testDefinitionsIgnore_3cde0982135f37c027f66e92df1feb34() {
        assertEval("{ m <- matrix() ; m }");
    }

    @Test
    public void TestSimpleArrays_testDefinitionsIgnore_f46772e8eabb18f95c6940feb557cadf() {
        assertEval("{ matrix( (1:6) * (1+3i), nrow=2 ) }");
    }

    @Test
    public void TestSimpleArrays_testDefinitionsIgnore_a738f26a25596a33b702362864391c08() {
        assertEval("{ m <- matrix(1:6, nrow=2, ncol=3, byrow=TRUE) ; m }");
    }

    @Test
    public void TestSimpleArrays_testLhsCopy_b28c0a33cc0f880b94d67b72b8d2c3fe() {
        assertEval("{ a = array(TRUE,c(3,3,3)); a[1,2,3] = 8L; typeof(a[1,2,3]) }");
    }

    @Test
    public void TestSimpleArrays_testLhsCopy_1ca74f727f5e095c80b0c363226c8cad() {
        assertEval("{ a = array(TRUE,c(3,3,3)); a[1,2,3] = 8.1; typeof(a[1,2,3]) }");
    }

    @Test
    public void TestSimpleArrays_testLhsCopy_1188e5d4220f7ee6bf073bfcf6c44bdc() {
        assertEval("{ a = array(1L,c(3,3,3)); a[1,2,3] = 8.1; typeof(a[1,2,3]) }");
    }

    @Test
    public void TestSimpleArrays_testLhsCopy_89ee7a7c1467b421c0cd7aa8b5b0ab1a() {
        assertEval("{ a = array(TRUE,c(3,3,3)); a[1,2,3] = 2+3i; typeof(a[1,2,3]) }");
    }

    @Test
    public void TestSimpleArrays_testLhsCopy_aeb54b7a0776636764ae6fb6c1aa9598() {
        assertEval("{ a = array(1L,c(3,3,3)); a[1,2,3] = 2+3i; typeof(a[1,2,3]) }");
    }

    @Test
    public void TestSimpleArrays_testLhsCopy_1bf88352245f5e663232507ef40ba011() {
        assertEval("{ a = array(1.3,c(3,3,3)); a[1,2,3] = 2+3i; typeof(a[1,2,3]) }");
    }

    @Test
    public void TestSimpleArrays_testLhsCopy_0e017bf3bcf4621c77c8cde8e9285be9() {
        assertEval("{ a = array(TRUE,c(3,3,3)); a[1,2,3] = \"2+3i\"; c(typeof(a[1,2,3]),typeof(a[1,1,1])) }");
    }

    @Test
    public void TestSimpleArrays_testLhsCopy_fe4f3635b552f069567ccbb6cebee7ef() {
        assertEval("{ a = array(1L,c(3,3,3)); a[1,2,3] = \"2+3i\"; c(typeof(a[1,2,3]),typeof(a[1,1,1])) }");
    }

    @Test
    public void TestSimpleArrays_testLhsCopy_7ba7c939dea8cf9dd35a94fc1defe85a() {
        assertEval("{ a = array(1.5,c(3,3,3)); a[1,2,3] = \"2+3i\"; c(typeof(a[1,2,3]),typeof(a[1,1,1])) }");
    }

    @Test
    public void TestSimpleArrays_testMatrixBuiltin_2e31b866518a0babac7185b6a624f910() {
        assertEval("{ length(matrix()) }");
    }

    @Test
    public void TestSimpleArrays_testMatrixSimpleRead_eb16fa2e75406d7cb53d9577dd513282() {
        assertEval("{ a = matrix(1,3,3); is.null(dim(a[1,])); }");
    }

    @Test
    public void TestSimpleArrays_testMatrixSubsetAndSelection_cb63ad38b6dae22340c98cae3e1b7544() {
        assertEval("{ matrix(1,3,3)[1,1] }");
    }

    @Test
    public void TestSimpleArrays_testMatrixSubsetAndSelection_d90eaa7e0462d85ab3a250782ee72171() {
        assertEval("{ matrix(1,3,3)[[1,1]] }");
    }

    @Test
    public void TestSimpleArrays_testMatrixSubsetAndSelectionIgnore_143f74286d3b0c31c553f6eb924f1cf9() {
        assertEval("{  m <- matrix(1:6, nrow=2) ;  m[1,NULL] }");
    }

    @Test
    public void TestSimpleArrays_testMatrixSubsetAndSelectionIgnore_f25c7ad49835429e017a36e857b420f0() {
        assertEvalError("{ matrix(1,3,3)[[,]]; }");
    }

    @Test
    public void TestSimpleArrays_testMatrixSubsetAndSelectionIgnore_ef9e65c0b7ef9fcfb532ed3b89735ebe() {
        assertEvalError("{ matrix(1,3,3)[[c(1,2),1]]; }");
    }

    @Test
    public void TestSimpleArrays_testMultiDimensionalUpdate_28a7a262e528d483c4f8748808fbbf80() {
        assertEval("{ a = matrix(1,3,3); a[1,] = c(3,4,5); c(a[1,1],a[1,2],a[1,3]) }");
    }

    @Test
    public void TestSimpleArrays_testMultiDimensionalUpdate_e8bbbb6f2925b062e169e30ad559b96a() {
        assertEval("{ a = matrix(1,3,3); a[,1] = c(3,4,5); c(a[1,1],a[2,1],a[3,1]) }");
    }

    @Test
    public void TestSimpleArrays_testMultiDimensionalUpdateIgnore_2880884c9d4299c1d6fc09ff8cad6ac1() {
        assertEval("{ a = array(1,c(3,3,3)); a[1,1,] = c(3,4,5); c(a[1,1,1],a[1,1,2],a[1,1,3]) }");
    }

    @Test
    public void TestSimpleArrays_testMultiDimensionalUpdateIgnore_a760b17b27d515ffd03bd8fcb9c9d596() {
        assertEval("{ a = array(1,c(3,3,3)); a[1,,1] = c(3,4,5); c(a[1,1,1],a[1,2,1],a[1,3,1]) }");
    }

    @Test
    public void TestSimpleArrays_testMultiDimensionalUpdateIgnore_4254fad80bad98a95f88e1afedc060c2() {
        assertEval("{ a = array(1,c(3,3,3)); a[,1,1] = c(3,4,5); c(a[1,1,1],a[2,1,1],a[3,1,1]) }");
    }

    @Test
    public void TestSimpleArrays_testMultiDimensionalUpdateIgnore_3bd39d84ee2da13efb7fd25b042003fd() {
        assertEval("{ a = array(1,c(3,3,3)); a[1,,] = matrix(1:9,3,3); c(a[1,1,1],a[1,3,1],a[1,3,3]) }");
    }

    @Test
    public void TestSimpleArrays_testRhsCopy_7a83b076f303201e23fe9f57257228cd() {
        assertEval("{ a = array(7L,c(3,3,3)); b = TRUE; a[1,2,3] = b; c(typeof(a[1,2,3]),typeof(a[1,1,1])) }");
    }

    @Test
    public void TestSimpleArrays_testRhsCopy_ec537db2632e715dbb3696faaa234c7b() {
        assertEval("{ a = array(1.7,c(3,3,3)); b = TRUE; a[1,2,3] = b; c(typeof(a[1,2,3]),typeof(a[1,1,1])) }");
    }

    @Test
    public void TestSimpleArrays_testRhsCopy_86b83677af3adfffd3a7a8f34771a3d1() {
        assertEval("{ a = array(3+2i,c(3,3,3)); b = TRUE; a[1,2,3] = b; c(typeof(a[1,2,3]),typeof(a[1,1,1])) }");
    }

    @Test
    public void TestSimpleArrays_testRhsCopy_c98226511471ad5901b4f1d9a633a5cc() {
        assertEval("{ a = array(\"3+2i\",c(3,3,3)); b = TRUE; a[1,2,3] = b; c(typeof(a[1,2,3]),typeof(a[1,1,1])) }");
    }

    @Test
    public void TestSimpleArrays_testRhsCopy_daac639ee4db005477fbffdb937aa5df() {
        assertEval("{ a = array(1.7,c(3,3,3)); b = 3L; a[1,2,3] = b; c(typeof(a[1,2,3]),typeof(a[1,1,1])) }");
    }

    @Test
    public void TestSimpleArrays_testRhsCopy_30baf7a53dc92b67fafbe6e9256459bf() {
        assertEval("{ a = array(3+2i,c(3,3,3)); b = 4L; a[1,2,3] = b; c(typeof(a[1,2,3]),typeof(a[1,1,1])) }");
    }

    @Test
    public void TestSimpleArrays_testRhsCopy_fb4c6bb0078e0cdd9cd396140f149d28() {
        assertEval("{ m <- array(c(1+1i,2+2i,3+3i), dim=c(3,1,1)) ; m[1:2,1,1] <- c(100L,101L) ; m ; c(typeof(m[1,1,1]),typeof(m[2,1,1])) }");
    }

    @Test
    public void TestSimpleArrays_testRhsCopy_2f054df4a290a4cb0b2a972f3a90a8b3() {
        assertEval("{ a = array(\"3+2i\",c(3,3,3)); b = 7L; a[1,2,3] = b; c(typeof(a[1,2,3]),typeof(a[1,1,1])) }");
    }

    @Test
    public void TestSimpleArrays_testRhsCopy_3a0e1e67b55dda52f9a150d1ea3b18d8() {
        assertEval("{ a = array(3+2i,c(3,3,3)); b = 4.2; a[1,2,3] = b; c(typeof(a[1,2,3]),typeof(a[1,1,1])) }");
    }

    @Test
    public void TestSimpleArrays_testRhsCopy_67cb7cce32247dd7370808c4cb8bd92b() {
        assertEval("{ a = array(\"3+2i\",c(3,3,3)); b = 2+3i; a[1,2,3] = b; c(typeof(a[1,2,3]),typeof(a[1,1,1])) }");
    }

    @Test
    public void TestSimpleArrays_testSelection_23efbfe0cbd87140bd499be6f514f28d() {
        assertEval("{ m <- matrix(c(1,2,3,4,5,6), nrow=3) ; m[0] }");
    }

    @Test
    public void TestSimpleArrays_testSelection_68ba1e0b5773910a3e4c1ff9fde81858() {
        assertEval("{ m <- matrix(1:6, nrow=2) ; m[upper.tri(m)] }");
    }

    @Test
    public void TestSimpleArrays_testSelection_0891abc916693bd9aafef52e327f4085() {
        assertEval("{ m <- matrix(list(1,2,3,4,5,6), nrow=3) ; m[0] }");
    }

    @Test
    public void TestSimpleArrays_testUpdate_38dc5faf9571715adc3bf3201964d97d() {
        assertEval("{ m <- matrix(1,2,2); m[1,1] = 6; m }");
    }

    @Test
    public void TestSimpleArrays_testUpdate_0ba83e01ba52f02d2d92225bbd25285d() {
        assertEval("{ m <- matrix(list(1,2,3,4,5,6), nrow=3) ; m[c(2,3,4,6)] <- NULL ; m }");
    }

    @Test
    public void TestSimpleArrays_testUpdate_ea6193fb770dbb3218ab789a577fae6b() {
        assertEval("{ x<-1:8; dim(x)<-c(2,2,2); y<-c(101:104); dim(y)<-c(2,2); x[1:2,1:2,1]<-y; x }");
    }

    @Test
    public void TestSimpleArrays_testUpdate_9d33d551a0185e7c52110e4def606b7f() {
        assertEval("{ x<-1:8; dim(x)<-c(2,2,2); y<-c(101:102); z<-(x[1:2,c(1,2,0),1]<-y); x }");
    }

    @Test
    public void TestSimpleArrays_testUpdate_7aa4715384dbabbb215c0963183438b8() {
        assertEval("{ x<-1:8; dim(x)<-c(2,2,2); y<-c(101:104); dim(y)<-c(2,2); z<-(x[1:2,c(1,1),1]<-y); x }");
    }

    @Test
    public void TestSimpleArrays_testUpdate_5e6045e0cd5503184ca06025e0964f1e() {
        assertEval("{ x<-1:8; dim(x)<-c(2,2,2); y<-c(101:104); dim(y)<-c(2,2); z<-(x[1:2,c(1,2,0),1]<-y); x }");
    }

    @Test
    public void TestSimpleArrays_testUpdate_b08dfe706b2530f144de0f10eca0c148() {
        assertEval("{ x<-as.double(1:8); dim(x)<-c(2,2,2); x[1,1,1]<-42L; x }");
    }

    @Test
    public void TestSimpleArrays_testUpdate_95413b92272752471ccd772cb002c80b() {
        assertEval("{ x<-1:8; dim(x)<-c(2,2,2); y<-c(101:104); dim(y)<-c(2,2); z<-(x[1:2,1:2,0]<-y); x }");
    }

    @Test
    public void TestSimpleArrays_testUpdate_04f025466c2fd90a5a8e00881bb11787() {
        assertEval("{ x<-1:8; dim(x)<-c(2,2,2); y<-c(101:104); dim(y)<-c(2,2); z<-(x[1:2,1:2,c(0,0)]<-y); x }");
    }

    @Test
    public void TestSimpleArrays_testUpdate_5cc050596c4a8432a51a889789f95621() {
        assertEval("{ x<-1:8; dim(x)<-c(2,2,2); z<-(x[1,1,1]<-42); z }");
    }

    @Test
    public void TestSimpleArrays_testUpdate_35ada927d965af96aede8ad35b8262a3() {
        assertEval("{ m <- matrix(1,2,2) ; m[,1] = 7 ; m }");
    }

    @Test
    public void TestSimpleArrays_testUpdate_1f013c008fd87b22b853ef1747301556() {
        assertEval("{ m <- matrix(1,2,2) ; m[1,] = 7 ; m }");
    }

    @Test
    public void TestSimpleArrays_testUpdate_7394ae5d30001af76fe55a4f83da2e67() {
        assertEval("{ m <- matrix(1,2,2) ; m[,1] = c(10,11) ; m }");
    }

    @Test
    public void TestSimpleArrays_testUpdate_bb1776e50de338c553c22fcd9ac59d32() {
        assertEval("{ m <- matrix(list(1,2,3,4,5,6), nrow=3) ; m[2] <- list(100) ; m }");
    }

    @Test
    public void TestSimpleArrays_testUpdate_09da1d9365bc7d6a74fccd3e0e6091b8() {
        assertEval("{ m <- matrix(1:6, nrow=2) ; m[,2] <- 10:11 ; m }");
    }

    @Test
    public void TestSimpleArrays_testUpdate_bf3975c5c2955ae7d6bba427e5c001c3() {
        assertEval("{ m <- matrix(1:6, nrow=2) ; m[,2:3] <- 10:11 ; m }");
    }

    @Test
    public void TestSimpleArrays_testUpdate_56b2b139446460e5096668823217cec0() {
        assertEval("{ m <- matrix(1:6, nrow=2) ; m[,integer()] <- integer() ; m }");
    }

    @Test
    public void TestSimpleArrays_testUpdate_5b55fb396efed1a468d4a15fbd7bba4a() {
        assertEval("{ m <- matrix(1:100, nrow=10) ; z <- 1; s <- 0 ; for(i in 1:3) { m[z <- z + 1,z <- z + 1] <- z * z * 1000 } ; sum(m) }");
    }

    @Test
    public void TestSimpleArrays_testUpdate_25f8ea9709db55f1e3a5858bfbad09ff() {
        assertEval("{ m <- matrix(1:6, nrow=2) ; f <- function(i,j) { m[i,j] <- 10 ; m } ; m <- f(1,-1) ; m }");
    }

    @Test
    public void TestSimpleArrays_testUpdate_ffe08a1037594e11b7fd8b76e7e9b82d() {
        assertEval("{ m <- matrix(1:6, nrow=2) ; f <- function(i,j) { m[i,j] <- 10 ; m } ; m <- f(1, c(-1,-10)) ; m }");
    }

    @Test
    public void TestSimpleArrays_testUpdate_a789679657679e8da4e45a4b818b645d() {
        assertEval("{ m <- matrix(1:6, nrow=2) ; f <- function(i,j) { m[i,j] <- 10 ; m } ; m <- f(1,c(-1,-10)) ; m <- f(1,-1) ; m }");
    }

    @Test
    public void TestSimpleArrays_testUpdate_fa779fe93ac86707197b170fc4150d1a() {
        assertEval("{ m <- matrix(1:6, nrow=2) ; f <- function(i,j) { m[i,j] <- 10 ; m } ; m <- f(1,c(-1,-10)) ; m <- f(-1,2) ; m }");
    }

    @Test
    public void TestSimpleArrays_testUpdate_517498110d51fa15ee12a1dcb4aac54e() {
        assertEval("{ m <- matrix(1:6, nrow=2) ; f <- function(i,j) { m[i,j] <- 10 ; m } ; m <- f(2,1:3) ; m <- f(1,-2) ; m }");
    }

    @Test
    public void TestSimpleArrays_testUpdate_da3cea51647c18602b50f7a5ec065395() {
        assertEval("{ m <- matrix(1:6, nrow=3) ; m[2] <- list(100) ; m }");
    }

    @Test
    public void TestSimpleArrays_testUpdate_5b6ebd5dfba1698cc17205996366b023() {
        assertEval("{ m <- matrix(list(1,2,3,4,5,6), nrow=3) ; m[[2]] <- list(100) ; m }");
    }

    @Test
    public void TestSimpleArrays_testUpdate_abe9d8737c99da9620fbb24f508ef79d() {
        assertEval("{ m <- array(1:24, dim=c(2,3,4)) ; m[,,4] <- 10:15 ; m[,,4] }");
    }

    @Test
    public void TestSimpleArrays_testUpdate_a64c7ba3d206a06ff99627855de6b058() {
        assertEval("{  m <- array(1:3, dim=c(3,1,1)) ; f <- function(x,v) { x[[2,1,1]] <- v ; x } ; f(m,10L) ; f(m,10) ; x <- f(m,11L) ; c(x[1],x[2],x[3]) }");
    }

    @Test
    public void TestSimpleArrays_testUpdate_4c96865e28716d351527e1b840b21d19() {
        assertEval("{ m <- matrix(as.double(1:6), nrow=2) ; mi <- matrix(1:6, nrow=2) ; f <- function(v,i,j) { v[i,j] <- 100 ; v[i,j] * i * j } ; f(m, 1L, 2L) ; f(m,1L,TRUE)  }");
    }

    @Test
    public void TestSimpleArrays_testUpdate_56ec90a6916b32370da9d365cdd6c805() {
        assertEval("{ m <- matrix(as.double(1:6), nrow=2) ; mi <- matrix(1:6, nrow=2) ; f <- function(v,i,j) { v[i,j] <- 100 ; v[i,j] * i * j } ; f(m, 1L, 2L) ; f(m,1L,-1)  }");
    }

    @Test
    public void TestSimpleArrays_testUpdate_686a578fdef5256103b362a1989d849b() {
        assertEval("{ x <- array(c(1,2,3), dim=c(3,1)) ; x[1:2,1] <- 2:1 ; x }");
    }

    @Test
    public void TestSimpleArrays_testUpdate_7641ceaebe4dbe9ebf51b1268ba21ed0() {
        assertEval("{ x<-1:8; dim(x)<-c(2,2,2); x[] = 42; x }");
    }

    @Test
    public void TestSimpleArrays_testUpdate_cb001350cf8748cfbf79d8fae2f5e3c6() {
        assertEval("{ x<-1:8; dim(x)<-c(2,2,2); x[] = c(42,7); x }");
    }

    @Test
    public void TestSimpleArrays_testUpdate_f576da04d7eb30be786e850c391cb138() {
        assertEval("{ z<-1:4; y<-((z[1]<-42) >  1) }");
    }

    @Test
    public void TestSimpleArrays_testUpdate_2fcbfbd07a71c18eb466aa1a3b354a61() {
        assertEval("{ z<-1:4; y<-((names(z)<-101:104) >  1) }");
    }

    @Test
    public void TestSimpleArrays_testUpdate_b8115d323e5bad7e3768c3cf9086df3d() {
        assertEvalError("({ x<-as.complex(1:8); dim(x)<-c(2,2,2); x[1, 1, 1] = as.raw(42); x })");
    }

    @Test
    public void TestSimpleArrays_testUpdate_8061a26a33e8e671859c0a05f79196bc() {
        assertEvalError("({ x<-as.character(1:8); dim(x)<-c(2,2,2); x[1, 1, 1] = as.raw(42); x })");
    }

    @Test
    public void TestSimpleArrays_testUpdate_4fe3874c527deb18e092824ff58ff9e2() {
        assertEvalError("({ x<-as.logical(1:8); dim(x)<-c(2,2,2); x[1, 1, 1] = as.raw(42); x })");
    }

    @Test
    public void TestSimpleArrays_testUpdate_b94798cc2ae8809273624ecc2d0a8d61() {
        assertEvalError("({ x<-as.double(1:8); dim(x)<-c(2,2,2); x[1, 1, 1] = as.raw(42); x })");
    }

    @Test
    public void TestSimpleArrays_testUpdate_2de4ff58580126cb0c6d42d2ac080976() {
        assertEvalError("({ x<-1:8; dim(x)<-c(2,2,2); x[1, 1, 1] = as.raw(42); x })");
    }

    @Test
    public void TestSimpleArrays_testUpdate_d5792bd2ae11d4500576d3ae43044769() {
        assertEvalError("{ x<-1:8; dim(x)<-c(2,2,2); y<-c(101:104); dim(y)<-c(2,2); x[1, 1] <- y; x }");
    }

    @Test
    public void TestSimpleArrays_testUpdate_6fa91247f0163918c1100999fc026988() {
        assertEvalError("{ x<-1:8; dim(x)<-c(2,2,2); y<-c(101:120); z<-(x[1:2, c(1, 2, 0), 1] <- y); x }");
    }

    @Test
    public void TestSimpleArrays_testUpdate_4983118d6eff6055c9fd4c410281a88a() {
        assertEvalError("{ x<-1:16; dim(x)<-c(2,2,2,2); y<-c(101:108); dim(y)<-c(2,4); x[1:2, 1:2, 1] <- y; x }");
    }

    @Test
    public void TestSimpleArrays_testUpdate_a28829e2b4043c3f83a730ecb79401fb() {
        assertEvalError("{ x<-1:8; dim(x)<-c(2,2,2); y<-c(101:104); dim(y)<-c(2,2); z<-(x[1:2, c(1, 2, 1), 1] <- y); x }");
    }

    @Test
    public void TestSimpleArrays_testUpdate_3b376634dada7194f8c362d8dde2d90a() {
        assertEvalError("{ x<-1:8; dim(x)<-c(2,2,2); y<-c(101:104); dim(y)<-c(2,2); z<-(x[0,5,1] <- y); x }");
    }

    @Test
    public void TestSimpleArrays_testUpdate_3215dc07aee02a17686c865850a7de65() {
        assertEvalError("{ x<-1:8; dim(x)<-c(2,2,2); y<-c(101:104); dim(y)<-c(2,2); z<-(x[1:2, c(1, NA), 1] <- y); x }");
    }

    @Test
    public void TestSimpleArrays_testUpdate_05f70fa9b550a137dc5550050fe9e5bc() {
        assertEvalError("{ x<-1:8; dim(x)<-c(2,2,2); x[1, 1, 1] = as.raw(42); x }");
    }

    @Test
    public void TestSimpleArrays_testUpdate_23ba0d046652a7c50d2974d05e7c0401() {
        assertEvalError("{ x<-1.1:8.8; dim(x)<-c(2,2,2); x[1, 1, 1] = as.raw(42); x }");
    }

    @Test
    public void TestSimpleArrays_testUpdate_f5e13dd11cb17804c5a2299ccb95e939() {
        assertEvalError("{ m <- matrix(1,2,2) ; m[, 1] = c(1, 2, 3, 4) ; m }");
    }

    @Test
    public void TestSimpleArrays_testUpdate_59af2b5d7153279529a31b1fce956192() {
        assertEvalError("{ m <- matrix(1:6, nrow=2) ; m[, 2] <- integer() }");
    }

    @Test
    public void TestSimpleArrays_testUpdate_60087d8626d45b07c9cbff4643e5f354() {
        assertEvalError("{ a <- 1:9 ; a[, , 1] <- 10L }");
    }

    @Test
    public void TestSimpleArrays_testUpdate_5a5c3233d90ba28e45426d21ed213df0() {
        assertEvalError("{ a <- 1:9 ; a[, 1] <- 10L }");
    }

    @Test
    public void TestSimpleArrays_testUpdate_487c5f957e71e496ee525305bcb3b2b3() {
        assertEvalError("{ a <- 1:9 ; a[1, 1] <- 10L }");
    }

    @Test
    public void TestSimpleArrays_testUpdate_df918d2ebef36c23490588e5b7393d8e() {
        assertEvalError("{ a <- 1:9 ; a[1, 1, 1] <- 10L }");
    }

    @Test
    public void TestSimpleArrays_testUpdate_77550087a3175fdf6431513b61a3d2b4() {
        assertEvalError("{ m <- matrix(1:6, nrow=2) ; m[[1, 1]] <- integer() }");
    }

    @Test
    public void TestSimpleArrays_testUpdate_266e6eb5b0b70e87d226096e143850ac() {
        assertEvalError("{ m <- matrix(1:6, nrow=2) ; m[[1:2, 1]] <- integer() }");
    }

    @Test
    public void TestSimpleArrays_testUpdate_713578410e0bb97eccf8372ce663ef5a() {
        assertEvalError("{ m <- matrix(1:6, nrow=2) ; m[1, 2] <- integer() }");
    }

    @Test
    public void TestSimpleArrays_testUpdate_c4058b1d991077ed6f08caa422de78df() {
        assertEvalError("{ m <- matrix(1:6, nrow=2) ; m[1, 2] <- 1:3 }");
    }

    @Test
    public void TestSimpleArrays_testUpdate_13ed7b6494e56b7dc867a982ccf70b44() {
        assertEvalError("{ x<-1:8; dim(x)<-c(2,2,2); y<-c(101:104); dim(y)<-c(2,2); z<-(x[1:2,c(1,2,NA),1]<-y); x }");
    }

    @Test
    public void TestSimpleArrays_testUpdate_916a50ecd7fc7c819f3c19aa10c7bfab() {
        assertEvalError("{ m <- matrix(1:6, nrow=2) ; m[[1:2,1]] <- 1 }");
    }

    @Test
    public void TestSimpleArrays_testUpdate_ef4a6efccd575fddefbfa099ae1f511d() {
        assertEvalError("{ m <- matrix(1:6, nrow=2) ; m[[integer(),1]] <- 1 }");
    }

    @Test
    public void TestSimpleAssignment_testAssign_283c9530c525c82a5e49b43433fdced9() {
        assertEvalNoOutput("{ a<-1 }");
    }

    @Test
    public void TestSimpleAssignment_testAssign_e7172f616e0946e808cf73fe8c5ba64b() {
        assertEvalNoOutput("{ a<-FALSE ; b<-a }");
    }

    @Test
    public void TestSimpleAssignment_testAssign_b21774dbc3b823d809cfaf4ee17527de() {
        assertEvalNoOutput("{ x = if (FALSE) 1 }");
    }

    @Test
    public void TestSimpleAssignment_testAssign1_09e0568d4e14f0cb95fcddf3e8cbb044() {
        assertEval("{ a<-1; a }");
    }

    @Test
    public void TestSimpleAssignment_testAssign1_eb7695b574cd27fa67c81aa87a217908() {
        assertEval("{ a<-1; a<-a+1; a }");
    }

    @Test
    public void TestSimpleAssignment_testAssign2_d140d26b8ba0a45a0383ad91da2047eb() {
        assertEval("a <- 42; f <- function() { a <- 13; a <<- 37; }; f(); a;");
    }

    @Test
    public void TestSimpleAssignment_testAssignBuiltin_e5c95e6f05f39af943fabed80872c648() {
        assertEval("{ x <- 3 ; f <- function() { assign(\"x\", 4) ; h <- function() { assign(\"z\", 5) ; g <- function() { x <<- 10 ; x } ; g() } ; h() } ; f() ; x }");
    }

    @Test
    public void TestSimpleAssignment_testAssignFunctionLookup1_af5ff7016009f392e234cca594160ea3() {
        assertEval("f <- function(b) { c <- 42; c(1,1); }; f(0); f(1)");
    }

    @Test
    public void TestSimpleAssignment_testAssignFunctionLookup1_2346e3897adeba694188ec2ab21c1070() {
        assertEval("f <- function(b) { if (b) c <- 42; c(1,1); }; f(0); f(1)");
    }

    @Test
    public void TestSimpleAssignment_testAssignPoly1_66cf51299f299d2cd8bfa5c599824623() {
        assertEval("test <- function(b) { if (b) f <- function() { 42 }; g <- function() { if (!b) f <- function() { 43 }; f() }; g() }; c(test(FALSE), test(TRUE))");
    }

    @Test
    public void TestSimpleAssignment_testAssignShadowBuiltin1_f2d5da3c45411e2c079849343ea84875() {
        assertEval("f <- function(b) { c <- function(x,y) 42; c(1,1); }; f(0); f(1)");
    }

    @Test
    public void TestSimpleAssignment_testAssignShadowBuiltin1_b9c9722029283827d0a91f19bac45918() {
        assertEval("f <- function(b) { if (b) c <- function(x,y) 42; c(1,1); }; f(0); f(1)");
    }

    @Test
    public void TestSimpleAssignment_testDynamic_d66d832275659532e17a035d9554c549() {
        assertEval("{ l <- quote(x <- 1) ; f <- function() { eval(l) } ; x <- 10 ; f() ; x }");
    }

    @Test
    public void TestSimpleAssignment_testDynamic_e224a6d79056c025f24a2a9d1a73d019() {
        assertEval("{ l <- quote(x <- 1) ; f <- function() { eval(l) ; x <<- 10 ; get(\"x\") } ; f() }");
    }

    @Test
    public void TestSimpleAssignment_testMisc_100afd1332c60dd5c86703f287e3911a() {
        assertEval("{ f <- function(i) { if (i==1) { c <- 1 } ; c } ; f(1) ; typeof(f(2)) }");
    }

    @Test
    public void TestSimpleAssignment_testMisc_11f8577ba30c376fff5b0303083cae96() {
        assertEval("{ f <- function(i) { if (i==1) { c <- 1 ; x <- 1 } ; if (i!=2) { x } else { c }} ; f(1) ; f(1) ; typeof(f(2)) }");
    }

    @Test
    public void TestSimpleAssignment_testMisc_f1cd2d7494f21b68f5cd46b0f9de2ce3() {
        assertEval("{ x <- 3 ; f <- function() { assign(\"x\", 4) ; g <- function() { assign(\"y\", 3) ; hh <- function() { assign(\"z\", 6) ; h <- function(s=1) { if (s==2) { x <- 5 } ; x } ; h() } ; hh() } ; g()  } ; f() }");
    }

    @Test
    public void TestSimpleAssignment_testMisc_2a5d4112a9df61a54de6d7a8a11d8ecf() {
        assertEval("{ f <- function() { if (FALSE) { c <- 1 } ; g <- function() { c } ; g() } ; typeof(f()) }");
    }

    @Test
    public void TestSimpleAssignment_testMisc_3473f21fbb8ff3f4621185b92fa898be() {
        assertEvalError("{ nonexistent }");
    }

    @Test
    public void TestSimpleAssignment_testMisc_a564aa5cff59f3815a991cd97a0065fc() {
        assertEvalError("{ f <- function(i) { if (i==1) { x <- 1 } ; x } ; f(1) ; f(2) }");
    }

    @Test
    public void TestSimpleAssignment_testMisc_6ea5ab2009ad6e4270c312f2694dd77d() {
        assertEvalError("{ f <- function(i) { if (i==1) { x <- 1 } ; x } ; f(1) ; f(1) ; f(2) }");
    }

    @Test
    public void TestSimpleAssignment_testMisc_7dfa89a192c051a9eca7cfb92894ad3f() {
        assertEvalError("{ f <- function() { if (FALSE) { x <- 1 } ; g <- function() { x } ; g() } ; f() }");
    }

    @Test
    public void TestSimpleAssignment_testSuperAssign_f9bb230e23dc5f0ddc569b1426f8c167() {
        assertEval("{ f <- function() { x <<- 2 } ; f() ; x }");
    }

    @Test
    public void TestSimpleAssignment_testSuperAssign_76240cdf26b6b096bdf624153c83c326() {
        assertEval("{ x <- 10 ; f <- function() { x <<- 2 } ; f() ; x }");
    }

    @Test
    public void TestSimpleAssignment_testSuperAssign_c19f2461210bc3e8b07a79c6a89f2199() {
        assertEval("{ x <- 10 ; f <- function() { x <<- 2 ; x } ; c(f(), f()) }");
    }

    @Test
    public void TestSimpleAssignment_testSuperAssign_d2f998d6fc6310797195c8e90d6d96a7() {
        assertEval("{ x <- 10 ; f <- function() { x <- x ; x <<- 2 ; x } ; c(f(), f()) }");
    }

    @Test
    public void TestSimpleAssignment_testSuperAssign_3203bfeb2d4247d4daf6723fed114ec4() {
        assertEval("{ x <- 10 ; g <- function() { f <- function() { x <- x ; x <<- 2 ; x } ; c(f(), f()) } ; g() }");
    }

    @Test
    public void TestSimpleAssignment_testSuperAssign_95bbe7b2e4dc61354b8489dc53bfb86a() {
        assertEval("{ x <- 10 ; g <- function() { x ; f <- function() { x <- x ; x <<- 2 ; x } ; c(f(), f()) } ; g() }");
    }

    @Test
    public void TestSimpleAssignment_testSuperAssign_5377162cb52b696b2bb6254b44b0d603() {
        assertEval("{ x <- 10 ; g <- function() { x <- 100 ; f <- function() { x <- x ; x <<- 2 ; x } ; c(f(), f()) } ; g() }");
    }

    @Test
    public void TestSimpleAssignment_testSuperAssign_8d705fd54a541e676b0f55b79759028e() {
        assertEval("{ h <- function() { x <- 10 ; g <- function() { if (FALSE) { x <- 2 } ; f <- function() { x <<- 3 ; x } ; f() } ; g() } ; h() }");
    }

    @Test
    public void TestSimpleAssignment_testSuperAssign_fd507ff0258d17af4cc20dd9767f93b2() {
        assertEval("{ b <- 2 ; f <- function() { b <- 4 } ; f() ; b }");
    }

    @Test
    public void TestSimpleAssignment_testSuperAssign_a8d79f5d83d13f5f3b59a26cebaaa7de() {
        assertEval("{ b <- 2 ; f <- function() { b <<- 4 } ; f() ; b }");
    }

    @Test
    public void TestSimpleAssignment_testSuperAssign_81cc32028bebd34eb03e1cca48ceda54() {
        assertEval("{ b <- 2 ; f <- function() { b[2] <- 4 } ; f() ; b }");
    }

    @Test
    public void TestSimpleAssignment_testSuperAssign_90949174a47b681a14ad5c5231f682b1() {
        assertEval("{ b <- 2 ; f <- function() { b[2] <<- 4 } ; f() ; b }");
    }

    @Test
    public void TestSimpleAssignment_testSuperAssign_12ecfcc5fc1638a412dde13bb505856a() {
        assertEval("{ a <- c(1,2,3) ; f <- function() { a[2] <<- 4 } ; f() ; a }");
    }

    @Test
    public void TestSimpleAssignment_testSuperAssign_d7ae9d2b149a168ea98b157820d1c1b8() {
        assertEval("{ f <- function(a) { g <- function(x,y) { a[x] <<- y } ; g(2,4) ; a } ; u <- c(1,2,3) ; k <- f(u) ; u <- c(3,2,1) ; l <- f(u) ; list(k,l) }");
    }

    @Test
    public void TestSimpleAssignment_testSuperAssign_368c5b171ee9e8c92a6652038f19c4f9() {
        assertEval("{ b <- c(1,1) ; f <- function(v,x) { g <- function(y) { v[y] <<- 2 } ; g(x) ; v } ; k <- f(b,1) ; l <- f(b,2) ; list(k,l,b) }");
    }

    @Test
    public void TestSimpleAssignment_testSuperAssign_a98ebddfb233e51a7c68c98d2bd39044() {
        assertEval("{ a <- c(0,0,0) ; u <- function() { a <- c(1,1,1) ; f <- function() { g <- function() { a[2] <<- 9 } ; g() } ; f() ; a } ; list(a,u()) }");
    }

    @Test
    public void TestSimpleAssignment_testSuperAssign_dd05d82c1fd098b6a426d63c199276f2() {
        assertEval("{ a <- c(0,0,0) ; f <- function() { g <- function() { a[2] <<- 9 } ; g() } ; u <- function() { a <- c(1,1,1) ; f() ; a } ; r <- a ; s <- u() ; t <- a ; list(r,s,t) }");
    }

    @Test
    public void TestSimpleAssignment_testSuperAssignIgnore_aa206594ebb10eb912cbc08e7c82e4e3() {
        assertEval("{ a <- c(1,2,3) ; f <- function() { a[2] <- 4 } ; list(f(),a) }");
    }

    @Test
    public void TestSimpleAssignment_testSuperAssignIgnore_cfdf1ec04d27a60bdfe3a1bea92933e6() {
        assertEvalNoOutput("{ x <<- 1 }");
    }

    @Test
    public void TestSimpleAssignment_testSuperAssignIgnore_437eb5c1cc18125d4b5896cf3d2b5365() {
        assertEvalNoOutput("{ x <<- 1 ; x }");
    }

    @Test
    public void TestSimpleAttributes_testArithmeticPropagation_7e4a3bc20603e321a27b3bb945396724() {
        assertEval("{ x <- 1:2;  attr(x, \"hi\") <- 2 ;  x+1:4 }");
    }

    @Test
    public void TestSimpleAttributes_testArithmeticPropagation_c7640c5004c57f6c0ba09229e1231252() {
        assertEval("{ x <- 1+1i;  attr(x, \"hi\") <- 1+2 ; y <- 2:3 ;  x+y }");
    }

    @Test
    public void TestSimpleAttributes_testArithmeticPropagation_a195448ef25445661595ab655c313b36() {
        assertEval("{ x <- 1:2 ;  attr(x, \"hi\") <- 2 ;  !x  }");
    }

    @Test
    public void TestSimpleAttributes_testArithmeticPropagation_ac2abd8e31ed3b36b0a27ecdcaaea36d() {
        assertEval("{ x <- 1:2;  attr(x, \"hi\") <- 2 ;  x & x }");
    }

    @Test
    public void TestSimpleAttributes_testArithmeticPropagation_6de4a8f653aba2cd0c9addf89d5e8894() {
        assertEval("{ x <- as.raw(1:2);  attr(x, \"hi\") <- 2 ;  x & x }");
    }

    @Test
    public void TestSimpleAttributes_testArithmeticPropagation_bd8aedf4ca4bd613b8c5ce35463c17a5() {
        assertEval("{ x <- c(a=FALSE,b=TRUE) ;  attr(x, \"hi\") <- 2 ;  !x  }");
    }

    @Test
    public void TestSimpleAttributes_testArithmeticPropagationIgnore_1bda07d542646902be34ad03340e3589() {
        assertEval("{ x <- c(1+1i,2+2i);  attr(x, \"hi\") <- 3 ; y <- 2:3 ; attr(y,\"zz\") <- 2; x+y }");
    }

    @Test
    public void TestSimpleAttributes_testArithmeticPropagationIgnore_cc69f5992ce92e155a3e58de80622625() {
        assertEval("{ x <- 1+1i;  attr(x, \"hi\") <- 1+2 ; y <- 2:3 ; attr(y,\"zz\") <- 2; x+y }");
    }

    @Test
    public void TestSimpleAttributes_testArithmeticPropagationIgnore_7d329add63812fd510617aeb1ca08021() {
        assertEval("{ x <- c(1+1i, 2+2i) ;  attr(x, \"hi\") <- 3 ; attr(x, \"hihi\") <- 10 ; y <- c(2+2i, 3+3i) ; attr(y,\"zz\") <- 2; attr(y,\"hi\") <-3; attr(y,\"bye\") <- 4 ; x+y }");
    }

    @Test
    public void TestSimpleAttributes_testArithmeticPropagationIgnore_7389f0a3eba8a4f98e70eefb81b427d5() {
        assertEval("{ x <- 1 ; attr(x, \"my\") <- 2; 2+x }");
    }

    @Test
    public void TestSimpleAttributes_testArithmeticPropagationIgnore_8b6f5b94450df9d844225e38d17048cb() {
        assertEval("{ x <- c(a=1) ; y <- c(b=2,c=3) ; x + y }");
    }

    @Test
    public void TestSimpleAttributes_testArithmeticPropagationIgnore_f7681448c0e12bcaeda34d2fd8a7ca9e() {
        assertEval("{ x <- c(a=1) ; y <- c(b=2,c=3) ; y + x }");
    }

    @Test
    public void TestSimpleAttributes_testArithmeticPropagationIgnore_a4301aa383a7a82db04455b69087bfdd() {
        assertEval("{ x <- 1:2;  attr(x, \"hi\") <- 2 ;  x+1 }");
    }

    @Test
    public void TestSimpleAttributes_testArithmeticPropagationIgnore_48b0478621d37a6014b0cbfa6773292b() {
        assertEval("{ x <- 1:2;  attr(x, \"hi\") <- 2 ; y <- 2:3 ; attr(y,\"hello\") <- 3; x+y }");
    }

    @Test
    public void TestSimpleAttributes_testArithmeticPropagationIgnore_9189f1ae9d4f71e60b9b9ef99e6a1fea() {
        assertEval("{ x <- 1;  attr(x, \"hi\") <- 1+2 ; y <- 2:3 ; attr(y, \"zz\") <- 2; x+y }");
    }

    @Test
    public void TestSimpleAttributes_testArithmeticPropagationIgnore_de673112886e8ff7e3c0167e9fbe3fc1() {
        assertEval("{ x <- 1:2 ;  attr(x, \"hi\") <- 3 ; attr(x, \"hihi\") <- 10 ; y <- 2:3 ; attr(y,\"zz\") <- 2; attr(y,\"hi\") <-3; attr(y,\"bye\") <- 4 ; x+y }");
    }

    @Test
    public void TestSimpleAttributes_testArithmeticPropagationIgnore_26dafe051e5a4eaed76812ecb0c1d215() {
        assertEval("{ x <- c(a=1,b=2) ;  attr(x, \"hi\") <- 2 ;  -x  }");
    }

    @Test
    public void TestSimpleAttributes_testArithmeticPropagationIgnore_e8d42dc69308c652b3f34cad5a6d4e16() {
        assertEval("{ x <- c(1+1i,2+2i);  names(x)<-c(\"a\", \"b\"); attr(x, \"hi\") <- 3 ; y <- 2:3 ; attr(y,\"zz\") <- 2; attributes(x+y) }");
    }

    @Test
    public void TestSimpleAttributes_testArithmeticPropagationIgnore_28d78b701f690207ab98fb46cacf6ef4() {
        assertEval("{ x <- c(1+1i,2+2i,3+3i,4+4i);  dim(x)<-c(2,2); names(x)<-c(\"a\", \"b\"); attr(x, \"hi\") <- 3 ; y <- 2:5 ; attr(y,\"zz\") <- 2; attributes(x+y) }");
    }

    @Test
    public void TestSimpleAttributes_testArithmeticPropagationIgnore_0152359fb4a6b66eee82170e9358766c() {
        assertEval("{ x <- c(1+1i,2+2i,3+3i,4+4i);  dim(x)<-c(2,2); dimnames(x)<-list(c(\"a\", \"b\"), c(\"c\", \"d\")); attr(x, \"hi\") <- 3 ; y <- 2:5 ; attr(y,\"zz\") <- 2; attributes(x+y) }");
    }

    @Test
    public void TestSimpleAttributes_testArrayPropagation_ff71faa7f9c4a02839d5cb9c6735788f() {
        assertEval("{ x <- c(a=1, b=2) ; attr(x, \"myatt\") <- 1; x[c(1,1)] }");
    }

    @Test
    public void TestSimpleAttributes_testArrayPropagation_67aa8586d6e9a61d530bc718f23a6fbc() {
        assertEval("{ x <- c(a=1, b=2) ; attr(x, \"myatt\") <- 1; x[\"a\"] <- 2 ; x }");
    }

    @Test
    public void TestSimpleAttributes_testArrayPropagation_f3e7bd3afad0d337f715dc9c0654c411() {
        assertEval("{ x <- c(a=TRUE, b=FALSE) ; attr(x, \"myatt\") <- 1; x[2] <- 2 ; x }");
    }

    @Test
    public void TestSimpleAttributes_testArrayPropagation_c9de33e462d40ccb19cfef28e600e93c() {
        assertEval("{ x <- TRUE ; attr(x, \"myatt\") <- 1; x[2] <- 2 ; x }");
    }

    @Test
    public void TestSimpleAttributes_testArrayPropagation_0054a011ab57339f812d2a1bc4006aed() {
        assertEval("{ x <- TRUE ; attr(x, \"myatt\") <- 1; x[1] <- 2 ; x }");
    }

    @Test
    public void TestSimpleAttributes_testArrayPropagation_7941d0b5f46e8abd2458c464c7c0c63f() {
        assertEval("{ m <- matrix(rep(1,4), nrow=2) ; attr(m, \"a\") <- 1 ;  m[2,2] <- 1+1i ; m }");
    }

    @Test
    public void TestSimpleAttributes_testArrayPropagation_69e8824b34256c5d038cd1cf95e24d0c() {
        assertEval("{ a <- array(c(1,1), dim=c(1,2)) ; attr(a, \"a\") <- 1 ;  a[1,1] <- 1+1i ; a }");
    }

    @Test
    public void TestSimpleAttributes_testBuiltinPropagation_51d2d368098c36292dd484b9a67e6025() {
        assertEval("{ x <- c(a=1, b=2) ; attr(x, \"myatt\") <- 1; matrix(x) }");
    }

    @Test
    public void TestSimpleAttributes_testBuiltinPropagation_8f17bb447b60a34ee3b9ef32b4affe5e() {
        assertEval("{ x <- 1 ; attr(x, \"myatt\") <- 1; x:x }");
    }

    @Test
    public void TestSimpleAttributes_testBuiltinPropagation_772cf48140c03fdf6261b417e8bc21a0() {
        assertEval("{ x <- 1 ; attr(x, \"myatt\") <- 1; c(x, x, x) }");
    }

    @Test
    public void TestSimpleAttributes_testBuiltinPropagation_58ea04a98d6a0eb0a8ce5f3d54bcf157() {
        assertEval("{ x <- 1 ; attr(x, \"myatt\") <- 1; cumsum(c(x, x, x)) }");
    }

    @Test
    public void TestSimpleAttributes_testBuiltinPropagation_f153fe3099db5134bf2ca6e53295342f() {
        assertEval("{ x <- 1 ; attr(x, \"myatt\") <- 1; min(x) }");
    }

    @Test
    public void TestSimpleAttributes_testBuiltinPropagation_17cdb1fb3024afacda26d8f703bcd65d() {
        assertEval("{ x <- 1 ; attr(x, \"myatt\") <- 1; x%o%x }");
    }

    @Test
    public void TestSimpleAttributes_testBuiltinPropagation_c5bbaeef48f1d2325c6a5b0a595cb0cc() {
        assertEval("{ x <- 1 ; attr(x, \"myatt\") <- 1; rep(x,2) }");
    }

    @Test
    public void TestSimpleAttributes_testBuiltinPropagation_10293a2fb3ca5e2b36b00f881b4a44a7() {
        assertEval("{ x <- c(a=1, b=2) ; attr(x, \"myatt\") <- 1; order(x) }");
    }

    @Test
    public void TestSimpleAttributes_testBuiltinPropagation_2ef50b87740e6da45357fabcf2e52f8d() {
        assertEval("{ x <- c(a=1, b=2) ; attr(x, \"myatt\") <- 1; sum(x) }");
    }

    @Test
    public void TestSimpleAttributes_testBuiltinPropagation_46530f286a71e41bcc7de14fa7504dad() {
        assertEval("{ x<-1:8; dim(x)<-c(2, 2, 2); names(x)<-101:108; attr(x, \"dimnames\")<-list(c(\"201\", \"202\"), c(\"203\", \"204\"), c(\"205\", \"206\")); attr(x, \"foo\")<-\"foo\"; y<-x; attributes(x>y) }");
    }

    @Test
    public void TestSimpleAttributes_testBuiltinPropagation_74dc4cc35dbb11bd44bd7b1feaf36e6c() {
        assertEval("{ x<-1:8; dim(x)<-c(2, 2, 2); names(x)<-101:108; attr(x, \"dimnames\")<-list(201:202, 203:204, 205:206); attr(x, \"foo\")<-\"foo\"; y<-x; attributes(x>y) }");
    }

    @Test
    public void TestSimpleAttributes_testBuiltinPropagation_8e0e4c508ec0f20a865b0743b9c50074() {
        assertEval("{ m <- 1:3 ; attr(m,\"a\") <- 1 ;  t(m) }");
    }

    @Test
    public void TestSimpleAttributes_testBuiltinPropagation_d0b74ca1e3a968310d26a6eb998b7eed() {
        assertEval("{ x <- c(a=1, b=2) ; attr(x, \"myatt\") <- 1 ; abs(x) }");
    }

    @Test
    public void TestSimpleAttributes_testBuiltinPropagation_fa36677c6a14355f660b5cf2568af617() {
        assertEval("{ x <- c(a=1, b=2) ; attr(x, \"myatt\") <- 1; array(x) }");
    }

    @Test
    public void TestSimpleAttributes_testBuiltinPropagation_f841fa185d5ca4d1e5534001f2a940ed() {
        assertEval("{ x <- \"a\" ; attr(x, \"myatt\") <- 1; toupper(x) }");
    }

    @Test
    public void TestSimpleAttributes_testBuiltinPropagation_2948272b3cd57d3c283b62245eada5c3() {
        assertEval("{ m <- matrix(1:6, nrow=2) ; attr(m,\"a\") <- 1 ;  diag(m) <- c(1,1) ; m }");
    }

    @Test
    public void TestSimpleAttributes_testBuiltinPropagation_f9c5942aae3ff5c8311c10aadbac4c1b() {
        assertEval("{ x <- c(a=1) ; attr(x, \"myatt\") <- 1; log10(x) }");
    }

    @Test
    public void TestSimpleAttributes_testBuiltinPropagation_4a561b926c2901d834996e4dcf07b721() {
        assertEval("{ x <- c(a=1) ; attr(x, \"myatt\") <- 1; nchar(x) }");
    }

    @Test
    public void TestSimpleAttributes_testBuiltinPropagation_938eacb0f06e2c5a1a15e8a0e3de20c8() {
        assertEval("{ m <- matrix(rep(1,4), nrow=2) ; attr(m,\"a\") <- 1 ;  upper.tri(m) }");
    }

    @Test
    public void TestSimpleAttributes_testBuiltinPropagation_4bb5cb517e5163df0d8f61721691bc5d() {
        assertEval("{ x <- c(a=1, b=2) ; attr(x, \"myatt\") <- 1; rev(x) }");
    }

    @Test
    public void TestSimpleAttributes_testBuiltinPropagation_568694034662a8b6af55c318d54ad291() {
        assertEval("{ x <- c(hello=1, hi=9) ; attr(x, \"hi\") <- 2 ;  sqrt(x) }");
    }

    @Test
    public void TestSimpleAttributes_testBuiltinPropagation_889b85feda5669920cfe714405ea72cd() {
        assertEval("{ m <- matrix(1:6, nrow=2) ; attr(m,\"a\") <- 1 ;  t(m) }");
    }

    @Test
    public void TestSimpleAttributes_testBuiltinPropagation_294da5e2033cde503cc35cc77c91a8be() {
        assertEval("{ x <- c(a=1, b=2) ; attr(x, \"myatt\") <- 1; unlist(x) }");
    }

    @Test
    public void TestSimpleAttributes_testBuiltinPropagation_5f3c184dd2fb70f674345e3d0a5ee9ca() {
        assertEval("{ x <- c(a=1, b=2) ; attr(x, \"myatt\") <- 1; unlist(list(x,x)) }");
    }

    @Test
    public void TestSimpleAttributes_testCastsIgnore_7421f56a7aeb2d6ab6fb29c2bdb776f6() {
        assertEval("{ x <- c(a=1, b=2) ; attr(x, \"myatt\") <- 1 ; as.character(x) }");
    }

    @Test
    public void TestSimpleAttributes_testCastsIgnore_c7ac6373611836a463ed1329c7aa7eee() {
        assertEval("{ x <- c(a=1, b=2) ; attr(x, \"myatt\") <- 1 ; as.double(x) }");
    }

    @Test
    public void TestSimpleAttributes_testCastsIgnore_a553b41add3e553324f2a994498662c4() {
        assertEval("{ x <- c(a=1, b=2) ; attr(x, \"myatt\") <- 1 ; as.integer(x) }");
    }

    @Test
    public void TestSimpleAttributes_testDefinition_9bfc4209f222e2d701466ef7d338132a() {
        assertEval("{ x <- as.raw(10) ; attr(x, \"hi\") <- 2 ;  x }");
    }

    @Test
    public void TestSimpleAttributes_testDefinition_80a0a73a4efa096e215a3bf7e5e5ee3e() {
        assertEval("{ x <- TRUE ; attr(x, \"hi\") <- 2 ;  x }");
    }

    @Test
    public void TestSimpleAttributes_testDefinition_59f68b4d9d36df8c78eb72446822f6dd() {
        assertEval("{ x <- 1L ; attr(x, \"hi\") <- 2 ;  x }");
    }

    @Test
    public void TestSimpleAttributes_testDefinition_ea8b6eacdfe0a30063d3a701d8b81f6e() {
        assertEval("{ x <- 1 ; attr(x, \"hi\") <- 2 ;  x }");
    }

    @Test
    public void TestSimpleAttributes_testDefinition_a0ab09aad0b3f396ec58751a4051d177() {
        assertEval("{ x <- 1+1i ; attr(x, \"hi\") <- 2 ;  x }");
    }

    @Test
    public void TestSimpleAttributes_testDefinition_32d40d8d60a044f9fafe367b8ae885fb() {
        assertEval("{ x <- \"s\" ; attr(x, \"hi\") <- 2 ;  x }");
    }

    @Test
    public void TestSimpleAttributes_testDefinition_ce72d4164b0322ade6f4182d9311ebb0() {
        assertEval("{ x <- c(1L, 2L) ; attr(x, \"hi\") <- 2; x }");
    }

    @Test
    public void TestSimpleAttributes_testDefinition_6c9cda38c30d74cc14b3e95de0034c60() {
        assertEval("{ x <- c(1, 2) ; attr(x, \"hi\") <- 2; x }");
    }

    @Test
    public void TestSimpleAttributes_testDefinition_424967deb3f2e2c414ea5fb27c821f3b() {
        assertEval("{ x <- c(1L, 2L) ; attr(x, \"hi\") <- 2; attr(x, \"hello\") <- 1:2 ;  x }");
    }

    @Test
    public void TestSimpleAttributes_testDefinition_af51cfc72a0c2a460dea902e49bac27c() {
        assertEval("{ x <- c(hello=9) ; attr(x, \"hi\") <- 2 ;  y <- x ; y }");
    }

    @Test
    public void TestSimpleAttributes_testDefinition_efd8aa922890186c8ec61d84b7fefd05() {
        assertEval("{ x <- c(hello=1) ; attr(x, \"hi\") <- 2 ;  attr(x,\"names\") <- \"HELLO\" ; x }");
    }

    @Test
    public void TestSimpleAttributes_testDefinition_09cc8ecbb951d42df7ce9a2b83157a71() {
        assertEval("{ x<-1; dim(x)<-1; y<-(attr(x, \"dimnames\")<-list(1)); y }");
    }

    @Test
    public void TestSimpleAttributes_testDefinition_4889b45f46fc504e4461e022620fc5d7() {
        assertEval("{ x<-1; dim(x)<-1; y<-list(a=\"1\"); z<-(attr(x, \"dimnames\")<-y); z }");
    }

    @Test
    public void TestSimpleAttributes_testDefinition_3856e444c64ddd1b3dbeb3529dd0c22f() {
        assertEval("{ x<-1; dim(x)<-1; y<-list(a=\"1\"); attr(y, \"foo\")<-\"foo\"; z<-(attr(x, \"dimnames\")<-y); z }");
    }

    @Test
    public void TestSimpleAttributes_testOtherPropagation_4957f6dceaabc15ff469b4e6e576d6dc() {
        assertEval("{ x <- 1:2;  attr(x, \"hi\") <- 2 ;  x == x }");
    }

    @Test
    public void TestSimpleBuiltins_testAbs_68f096986a0e9ceed28506f20d94f257() {
        assertEval("{ abs(0) }");
    }

    @Test
    public void TestSimpleBuiltins_testAbs_0acc9423c2327a12a6706a9997f0712f() {
        assertEval("{ abs(100) }");
    }

    @Test
    public void TestSimpleBuiltins_testAbs_569eff027a08e9fde94d1b7d3ba01f39() {
        assertEval("{ abs(-100) }");
    }

    @Test
    public void TestSimpleBuiltins_testAbs_1fc0494f8dd251a24b83991491080491() {
        assertEval("{ abs(0/0) }");
    }

    @Test
    public void TestSimpleBuiltins_testAbs_e6d5bf03ff804a0d023af770b585d69c() {
        assertEval("{ abs((1:2)[3]) }");
    }

    @Test
    public void TestSimpleBuiltins_testAbs_88476886e0f40a7639f1996d50fddc07() {
        assertEval("{ abs((1/0)*(1-0i)) }");
    }

    @Test
    public void TestSimpleBuiltins_testAbs_42fe2bd49dfa37377c15aea64f82ee5a() {
        assertEval("{ abs(1) }");
    }

    @Test
    public void TestSimpleBuiltins_testAbs_4456708f0bfcc38934777e67f82a7548() {
        assertEval("{ abs(1L) }");
    }

    @Test
    public void TestSimpleBuiltins_testAbs_49a74a9acf7cf428f70314c68c018c85() {
        assertEval("{ abs(-1) }");
    }

    @Test
    public void TestSimpleBuiltins_testAbs_7d0151c6a4328be98c5bc1e36d7852e5() {
        assertEval("{ abs(-1L) }");
    }

    @Test
    public void TestSimpleBuiltins_testAbs_b5d0668b9136f3c69e0fc9550dbfb522() {
        assertEval("{ abs(NA) }");
    }

    @Test
    public void TestSimpleBuiltins_testAbs_0a7fc2c350e52e9c63daec2feb9c0bed() {
        assertEval("{ abs(c(1, 2, 3)) }");
    }

    @Test
    public void TestSimpleBuiltins_testAbs_6bfa52057bb27680f3f04805b1537720() {
        assertEval("{ abs(c(1L, 2L, 3L)) }");
    }

    @Test
    public void TestSimpleBuiltins_testAbs_effeff677a4a5a8eb358dcdd8b6e4577() {
        assertEval("{ abs(c(1, -2, 3)) }");
    }

    @Test
    public void TestSimpleBuiltins_testAbs_8d1ac4adf1814118b24e3c61913abe06() {
        assertEval("{ abs(c(1L, -2L, 3L)) }");
    }

    @Test
    public void TestSimpleBuiltins_testAbs_a8078003e9fda805136e2483baee5b91() {
        assertEval("{ abs(c(1L, -2L, NA)) }");
    }

    @Test
    public void TestSimpleBuiltins_testAbs_a03de453893b37f67fdd422f9655f627() {
        assertEval("{ abs((-1-0i)/(0+0i)) }");
    }

    @Test
    public void TestSimpleBuiltins_testAbs_9da4d471d0d1d58d830a27ab25dee2e1() {
        assertEval("{ abs((-0-1i)/(0+0i)) }");
    }

    @Test
    public void TestSimpleBuiltins_testAbs_abe899521613e6d073bd9e7bb246c74b() {
        assertEval("{ abs(NA+0.1) }");
    }

    @Test
    public void TestSimpleBuiltins_testAbs_860cb30c6847e251edc865a9c717708a() {
        assertEval("{ abs((0+0i)/0) }");
    }

    @Test
    public void TestSimpleBuiltins_testAbs_a95bf267b6b89c8ae0d816aaa8b2a61e() {
        assertEval("{ abs(c(1, -2, NA)) }");
    }

    @Test
    public void TestSimpleBuiltins_testAbsIgnore_261d7e173c1caffcac87b3030f93a81c() {
        assertEval("{ abs(c(0/0,1i)) }");
    }

    @Test
    public void TestSimpleBuiltins_testAbsIgnore_c0cc055b696d0196df8961748dac97a4() {
        assertEval("{ exp(-abs((0+1i)/(0+0i))) }");
    }

    @Test
    public void TestSimpleBuiltins_testAbsIgnore_0ab2d0f2d7030b273cd0e45daf435b57() {
        assertEval("{ abs(1:3) }");
    }

    @Test
    public void TestSimpleBuiltins_testAbsIgnore_eb0e93fa1cbdf12456e6b7c849b0f670() {
        assertEval("{ abs(-1:-3) }");
    }

    @Test
    public void TestSimpleBuiltins_testAbsIgnore_f9ca5d8354239b619dbd0b67d729e220() {
        assertEvalError("{ abs(NULL) }");
    }

    @Test
    public void TestSimpleBuiltins_testAll_46b44798ef0ef861d7099cb70f6e04fc() {
        assertEval("{ all(TRUE) }");
    }

    @Test
    public void TestSimpleBuiltins_testAll_7eb2579b0011ec9cebb90c3868f102af() {
        assertEval("{ all(TRUE, TRUE, TRUE) }");
    }

    @Test
    public void TestSimpleBuiltins_testAll_63dfd708a8f6ca07c66d9a36ca7c1671() {
        assertEval("{ all() }");
    }

    @Test
    public void TestSimpleBuiltins_testAll_74e62414f00a7a01b7aee87141540d1c() {
        assertEval("{ all(logical(0)) }");
    }

    @Test
    public void TestSimpleBuiltins_testAll_18caf1af80d31568dbcee6231603018b() {
        assertEval("{ all(TRUE, FALSE) }");
    }

    @Test
    public void TestSimpleBuiltins_testAll_e8b74c770784ab3c2c066d80e5fcedbf() {
        assertEval("{ all(FALSE) }");
    }

    @Test
    public void TestSimpleBuiltins_testAll_69042d8d60da68fbd9a94537c7d616e0() {
        assertEval("{ all(TRUE, TRUE, NA) }");
    }

    @Test
    public void TestSimpleBuiltins_testAll_d80184fa93dcecce1606e966853cf051() {
        assertEval("{ v <- c(\"abc\", \"def\") ; w <- c(\"abc\", \"def\") ; all(v == w) }");
    }

    @Test
    public void TestSimpleBuiltins_testAll_fe60ec40e3bf3085eaf425c129a1cbcf() {
        assertEval("{ all(TRUE, FALSE, NA,  na.rm=FALSE) }");
    }

    @Test
    public void TestSimpleBuiltins_testAll_15bb54df265504a467a3ebdb5fcdf7c1() {
        assertEval("{ all(TRUE, FALSE, NA,  na.rm=TRUE) }");
    }

    @Test
    public void TestSimpleBuiltins_testAll_a3aeecdc1931de721a504b853f3fbb34() {
        assertEval("{ all(TRUE, TRUE, NA,  na.rm=FALSE) }");
    }

    @Test
    public void TestSimpleBuiltins_testAllIgnore_1cad38b2b58506e86b3faf337282af34() {
        assertEval("{ all(TRUE, TRUE, NA,  na.rm=TRUE) }");
    }

    @Test
    public void TestSimpleBuiltins_testAllIgnore_2b56cf245fc3518ca8c3daa8c70c7441() {
        assertEval("{ all(1) }");
    }

    @Test
    public void TestSimpleBuiltins_testAllIgnore_91a6f9d5d41dc450755861f6e318c869() {
        assertEval("{ all(0) }");
    }

    @Test
    public void TestSimpleBuiltins_testAllIgnore_b5c51ccc3f58394e01320c7b59736d24() {
        assertEval("{ all(TRUE,c(TRUE,TRUE),1) }");
    }

    @Test
    public void TestSimpleBuiltins_testAllIgnore_e11f439dffd428996b1d680fede13a41() {
        assertEval("{ all(TRUE,c(TRUE,TRUE),1,0) }");
    }

    @Test
    public void TestSimpleBuiltins_testAny_c3ac960fb2d31a1be58f5185a775cfb8() {
        assertEval("{ any(TRUE) }");
    }

    @Test
    public void TestSimpleBuiltins_testAny_fa0f3c9198da2ce2ff160de0f43f69b2() {
        assertEval("{ any(TRUE, TRUE, TRUE) }");
    }

    @Test
    public void TestSimpleBuiltins_testAny_753d837e06d5b62df1a8801fbfdbcfc3() {
        assertEval("{ any(TRUE, FALSE) }");
    }

    @Test
    public void TestSimpleBuiltins_testAny_d3a630f40989cf201e89e2023a1b51b8() {
        assertEval("{ any(TRUE, TRUE, NA) }");
    }

    @Test
    public void TestSimpleBuiltins_testAny_2a881cee5349443e18159f9c3335a358() {
        assertEval("{ any() }");
    }

    @Test
    public void TestSimpleBuiltins_testAny_a2e09a9cb158b5aa0aab3f18d121bd9a() {
        assertEval("{ any(logical(0)) }");
    }

    @Test
    public void TestSimpleBuiltins_testAny_77d354dde38110a48bac331d8fe9e433() {
        assertEval("{ any(FALSE) }");
    }

    @Test
    public void TestSimpleBuiltins_testAny_9a13ea66e606b5a717d8fb608d05a92e() {
        assertEval("{ any(NA, NA, NA) }");
    }

    @Test
    public void TestSimpleBuiltins_testAny_689c35b766fd3c46f31a3258cec01e39() {
        assertEval("{ any(NA) }");
    }

    @Test
    public void TestSimpleBuiltins_testAny_69119abd2f9748e70a20ccdbf38fac18() {
        assertEval("{ any(TRUE, TRUE, NA,  na.rm=TRUE) }");
    }

    @Test
    public void TestSimpleBuiltins_testAny_98b5442ddc22d31ab63d54d6c59a0e72() {
        assertEval("{ any(TRUE, FALSE, NA,  na.rm=TRUE) }");
    }

    @Test
    public void TestSimpleBuiltins_testAny_24c97d9f007981eca6d7905e73991716() {
        assertEval("{ any(FALSE, NA,  na.rm=FALSE) }");
    }

    @Test
    public void TestSimpleBuiltins_testAnyIgnore_a5514afb3c27ad5fad71696cb1db96a9() {
        assertEval("{ any(FALSE, NA,  na.rm=TRUE) }");
    }

    @Test
    public void TestSimpleBuiltins_testAnyIgnore_91043dd22cb7d3aab79a22019a52ea3f() {
        assertEvalWarning("{ any(1) }");
    }

    @Test
    public void TestSimpleBuiltins_testAnyIgnore_b0a96f7fb16a6bf50fba85a11a8da034() {
        assertEvalWarning("{ any(0) }");
    }

    @Test
    public void TestSimpleBuiltins_testAperm_32f22c3030902475114c0fb4882d3ea0() {
        assertEval("{ a = array(1:4,c(2,2)); b = aperm(a); c(a[1,1] == b[1,1], a[1,2] == b[2,1], a[2,1] == b[1,2], a[2,2] == b[2,2]) }");
    }

    @Test
    public void TestSimpleBuiltins_testAperm_b8c345f580afff451e38c41a3a55ff01() {
        assertEval("{ a = array(1:24,c(2,3,4)); b = aperm(a); c(dim(b)[1],dim(b)[2],dim(b)[3]) }");
    }

    @Test
    public void TestSimpleBuiltins_testAperm_f7e5d7608001661c62ccda8a927e658a() {
        assertEval("{ a = array(1:24,c(2,3,4)); b = aperm(a, resize=FALSE); c(dim(b)[1],dim(b)[2],dim(b)[3]) }");
    }

    @Test
    public void TestSimpleBuiltins_testAperm_18e9d3c9755549c9b400b15ab8950c41() {
        assertEval("{ a = array(1:24,c(2,3,4)); b = aperm(a, c(2,3,1)); a[1,2,3] == b[2,3,1] }");
    }

    @Test
    public void TestSimpleBuiltins_testAperm_5c1fb0ab96b21921d05341372ae0aa2c() {
        assertEval("{ a = array(1:24,c(3,3,3)); b = aperm(a, c(2,3,1)); c(a[1,2,3] == b[2,3,1], a[2,3,1] == b[3,1,2], a[3,1,2] == b[1,2,3]) }");
    }

    @Test
    public void TestSimpleBuiltins_testAperm_674dc3e594fa65d6d7e91a9911e91f02() {
        assertEval("{ a = array(1:24,c(3,3,3)); b = aperm(a, c(2,3,1), resize = FALSE); c(a[1,2,3] == b[2,3,1], a[2,3,1] == b[3,1,2], a[3,1,2] == b[1,2,3]) }");
    }

    @Test
    public void TestSimpleBuiltins_testAperm_f0effd761c52fe6bf1a5d5c76ccc721f() {
        assertEval("{ a = array(1:24,c(2,3,4)); b = aperm(a, c(2,3,1), resize = FALSE); a[1,2,3] == b[2,1,2] }");
    }

    @Test
    public void TestSimpleBuiltins_testAperm_fc6d4e2ce3038c9b44e62938ed037b59() {
        assertEval("{ aperm(array(1:27,c(3,3,3)), c(1+1i,3+3i,2+2i))[1,2,3] == array(1:27,c(3,3,3))[1,3,2]; }");
    }

    @Test
    public void TestSimpleBuiltins_testAperm_f663b80fd121c4a4b2fe9d966eb3db55() {
        assertEvalError("{ aperm(c(1,2,3)); }");
    }

    @Test
    public void TestSimpleBuiltins_testAperm_6a90b304900b2f56fb170f26490d9bca() {
        assertEvalError("{ aperm(array(1,c(3,3,3)), c(1,2)); }");
    }

    @Test
    public void TestSimpleBuiltins_testAperm_ad567449416f42ba7d5a044a3ee92935() {
        assertEvalError("{ aperm(array(1,c(3,3,3)), c(1,2,1)); }");
    }

    @Test
    public void TestSimpleBuiltins_testAperm_d452fc9657b296292ea89b31c89a766b() {
        assertEvalError("{ aperm(array(1,c(3,3,3)), c(1,2,0)); }");
    }

    @Test
    public void TestSimpleBuiltins_testApply_6f7db828eb780faac00d9679475a3b45() {
        assertEval("{ m <- matrix(c(1,2,3,4,5,6),2) ; apply(m,1,sum) }");
    }

    @Test
    public void TestSimpleBuiltins_testApply_daacf7caa38c333d8ac62cef2f4274dc() {
        assertEval("{ m <- matrix(c(1,2,3,4,5,6),2) ; apply(m,2,sum) }");
    }

    @Test
    public void TestSimpleBuiltins_testApplyIgnore_797cd316f3f859174c906d613c777e40() {
        assertEval("{ lapply(1:3, function(x) { 2*x }) }");
    }

    @Test
    public void TestSimpleBuiltins_testApplyIgnore_5ed0951d3e7363f21bc554e405102229() {
        assertEval("{ lapply(1:3, function(x,y) { x*y }, 2) }");
    }

    @Test
    public void TestSimpleBuiltins_testApplyIgnore_aad1bc65130fb0c42e2e3d991f1b3391() {
        assertEval("{ f <- function() { lapply(c(X=\"a\",Y=\"b\"), function(x) { c(a=x) })  } ; f() }");
    }

    @Test
    public void TestSimpleBuiltins_testArrayConstructors_a1f8649b7e81e6553a6460a323f03e01() {
        assertEval("{ integer() }");
    }

    @Test
    public void TestSimpleBuiltins_testArrayConstructors_ace5c82d7decc06e96c0369d976451d2() {
        assertEval("{ double() }");
    }

    @Test
    public void TestSimpleBuiltins_testArrayConstructors_ba528faabde4ed03f8cd7e2670afd6e3() {
        assertEval("{ logical() }");
    }

    @Test
    public void TestSimpleBuiltins_testArrayConstructors_7bb1497dca1100dec4040cff37cad092() {
        assertEval("{ double(3) }");
    }

    @Test
    public void TestSimpleBuiltins_testArrayConstructors_2631ad38e678be34280bca77974845e2() {
        assertEval("{ logical(3L) }");
    }

    @Test
    public void TestSimpleBuiltins_testArrayConstructors_78cc4b45f71b765a08755e29487b9532() {
        assertEval("{ character(1L) }");
    }

    @Test
    public void TestSimpleBuiltins_testArrayConstructors_4f70837cd05e5ebf17439d46a9001540() {
        assertEval("{ raw() }");
    }

    @Test
    public void TestSimpleBuiltins_testAsCharacter_9a2f22325dbb9e10761ad0f6022bc8d7() {
        assertEval("{ as.character(1) }");
    }

    @Test
    public void TestSimpleBuiltins_testAsCharacter_25b198b6eccc28cd71e39846217a8d03() {
        assertEval("{ as.character(1L) }");
    }

    @Test
    public void TestSimpleBuiltins_testAsCharacter_0d773d1535b54d9363bb7ba0d5232291() {
        assertEval("{ as.character(TRUE) }");
    }

    @Test
    public void TestSimpleBuiltins_testAsCharacter_e53d031aa88b9012634d2a91d62da9ec() {
        assertEval("{ as.character(1:3) }");
    }

    @Test
    public void TestSimpleBuiltins_testAsCharacter_7d86b0dfc3ab7a8407636a972c50dc97() {
        assertEval("{ as.character(NULL) }");
    }

    @Test
    public void TestSimpleBuiltins_testAsCharacterIgnore_c803fc23a52fdc9950e5603f439b132f() {
        assertEval("{ as.character(list(1,2,3)) }");
    }

    @Test
    public void TestSimpleBuiltins_testAsCharacterIgnore_03efd474c6b2ac63cfa1f6d497c9cf80() {
        assertEval("{ as.character(list(c(\"hello\", \"hi\"))) }");
    }

    @Test
    public void TestSimpleBuiltins_testAsCharacterIgnore_2f45a0dc44e788e9eaea83ed3fc488ad() {
        assertEval("{ as.character(list(list(c(\"hello\", \"hi\")))) }");
    }

    @Test
    public void TestSimpleBuiltins_testAsCharacterIgnore_f0e99f0b6485990390645c5a6f6b13c3() {
        assertEval("{ as.character(list(c(2L, 3L))) }");
    }

    @Test
    public void TestSimpleBuiltins_testAsCharacterIgnore_2ef5afd90d532194c1e0775974b91525() {
        assertEval("{ as.character(list(c(2L, 3L, 5L))) }");
    }

    @Test
    public void TestSimpleBuiltins_testAsComplex_373710d7b4cc13cffadaba377dcce68c() {
        assertEval("{ as.complex(0) }");
    }

    @Test
    public void TestSimpleBuiltins_testAsComplex_72a16c00ee2a54694ebcb7a89b07382a() {
        assertEval("{ as.complex(TRUE) }");
    }

    @Test
    public void TestSimpleBuiltins_testAsComplex_dab932848b730f42b5b3e008d32e8d56() {
        assertEval("{ as.complex(\"1+5i\") }");
    }

    @Test
    public void TestSimpleBuiltins_testAsComplex_640228a8826c3e8c6800e6add93cb103() {
        assertEval("{ as.complex(\"-1+5i\") }");
    }

    @Test
    public void TestSimpleBuiltins_testAsComplex_95c68dbb2a1de795b72eb00c995d0518() {
        assertEval("{ as.complex(\"-1-5i\") }");
    }

    @Test
    public void TestSimpleBuiltins_testAsComplex_3cb823e08080615aaacb8fd6a9706be2() {
        assertEval("{ as.complex(0/0) }");
    }

    @Test
    public void TestSimpleBuiltins_testAsComplex_6f38d3da5ad7372f6ad69d458aed759e() {
        assertEval("{ as.complex(c(0/0, 0/0)) }");
    }

    @Test
    public void TestSimpleBuiltins_testAsComplex_b6b94ce72b18de1ff03ebe7feeb17554() {
        assertEval("{ x<-c(a=1.1, b=2.2); dim(x)<-c(1,2); attr(x, \"foo\")<-\"foo\"; y<-as.complex(x); attributes(y) }");
    }

    @Test
    public void TestSimpleBuiltins_testAsComplex_8ffbf32e73519b2e78880c77bb680ced() {
        assertEval("{ x<-c(a=1L, b=2L); dim(x)<-c(1,2); attr(x, \"foo\")<-\"foo\"; y<-as.complex(x); attributes(y) }");
    }

    @Test
    public void TestSimpleBuiltins_testAsComplex_1c161837613fc8eb032e82d2847d699a() {
        assertEval("{ as.complex(\"Inf\") }");
    }

    @Test
    public void TestSimpleBuiltins_testAsComplex_09a336b246204913af2ac217bb9c8a39() {
        assertEval("{ as.complex(\"NaN\") }");
    }

    @Test
    public void TestSimpleBuiltins_testAsComplex_a8ce6a3e28ddb6e5b3050aa2adb56f87() {
        assertEval("{ as.complex(\"0x42\") }");
    }

    @Test
    public void TestSimpleBuiltins_testAsComplex_a956418be2f92dc147b9880ee38a0baa() {
        assertEvalWarning("{ as.complex(c(\"1\",\"hello\")) }");
    }

    @Test
    public void TestSimpleBuiltins_testAsComplex_0e40e701e12f7aaf40302968a93162c8() {
        assertEvalWarning("{ as.complex(\"TRUE\") }");
    }

    @Test
    public void TestSimpleBuiltins_testAsComplexIgnore_a234b535de865dc1374d86dc2a304cb0() {
        assertEval("{ as.complex(\"1e10+5i\") }");
    }

    @Test
    public void TestSimpleBuiltins_testAsComplexIgnore_f959c2432167aba7516572589c2a297b() {
        assertEval("{ as.complex(\"-.1e10+5i\") }");
    }

    @Test
    public void TestSimpleBuiltins_testAsComplexIgnore_4fff4d142baeef1724a393317f422bfe() {
        assertEval("{ as.complex(\"1e-2+3i\") }");
    }

    @Test
    public void TestSimpleBuiltins_testAsComplexIgnore_ca81945b0033de54e397d1df1719f69a() {
        assertEval("{ as.complex(\"+.1e+2-3i\") }");
    }

    @Test
    public void TestSimpleBuiltins_testAsDouble_63a91893e2c18baa68e6de1977cdefe8() {
        assertEval("{ as.double(\"1.27\") }");
    }

    @Test
    public void TestSimpleBuiltins_testAsDouble_091669465d2b034d5e6a55aa84b6e4b3() {
        assertEval("{ as.double(1L) }");
    }

    @Test
    public void TestSimpleBuiltins_testAsDouble_479fd1639fe2d6e0d67a0e816becabdc() {
        assertEval("{ as.double(as.raw(1)) }");
    }

    @Test
    public void TestSimpleBuiltins_testAsDouble_f8fef2e9f95f5b9cec473c1ae1ce5dc7() {
        assertEval("{ x<-c(a=1.1, b=2.2); dim(x)<-c(1,2); attr(x, \"foo\")<-\"foo\"; y<-as.double(x); attributes(y) }");
    }

    @Test
    public void TestSimpleBuiltins_testAsDouble_bc1da27133d56768b192f56f13fb67a1() {
        assertEval("{ x<-c(a=1L, b=2L); dim(x)<-c(1,2); attr(x, \"foo\")<-\"foo\"; y<-as.double(x); attributes(y) }");
    }

    @Test
    public void TestSimpleBuiltins_testAsDouble_82ae143056b783ea1c485d5aaaf8b2ef() {
        assertEvalWarning("{ as.double(c(\"1\",\"hello\")) }");
    }

    @Test
    public void TestSimpleBuiltins_testAsDouble_6db9f2b7f030c3b545b6fb2f540cc502() {
        assertEvalWarning("{ as.double(\"TRUE\") }");
    }

    @Test
    public void TestSimpleBuiltins_testAsDouble_d5d5b6abc97079dc1d2d996282a4b4a3() {
        assertEvalWarning("{ as.double(10+2i) }");
    }

    @Test
    public void TestSimpleBuiltins_testAsDouble_69e00247e301060254e0797e5f04251b() {
        assertEvalWarning("{ as.double(c(3+3i, 4+4i)) }");
    }

    @Test
    public void TestSimpleBuiltins_testAsInteger_7141b813c63bfa1663f1f54d19e9a25e() {
        assertEval("{ as.integer(\"1\") }");
    }

    @Test
    public void TestSimpleBuiltins_testAsInteger_bf6db303a1a75a748a8b5b72f613bd2e() {
        assertEval("{ as.integer(c(\"1\",\"2\")) }");
    }

    @Test
    public void TestSimpleBuiltins_testAsInteger_15edd2182c0f050e41ac2b7841afd707() {
        assertEval("{ as.integer(c(1,2,3)) }");
    }

    @Test
    public void TestSimpleBuiltins_testAsInteger_5239f66ec282ce2ec574f10eb3d9df81() {
        assertEval("{ as.integer(c(1.0,2.5,3.9)) }");
    }

    @Test
    public void TestSimpleBuiltins_testAsInteger_3275c4211593a568fc0d31983e0e22fc() {
        assertEval("{ as.integer(0/0) }");
    }

    @Test
    public void TestSimpleBuiltins_testAsInteger_359278f853e0c2b6a409d4461d6323cf() {
        assertEval("{ as.integer(-0/0) }");
    }

    @Test
    public void TestSimpleBuiltins_testAsInteger_05effa6e651dbb52c7a57418a138aff6() {
        assertEval("{ as.integer(as.raw(c(1,2,3,4))) }");
    }

    @Test
    public void TestSimpleBuiltins_testAsInteger_eeb551b51fdaffff49e0d2f647a3f7f3() {
        assertEval("{ as.integer(list(c(1),2,3)) }");
    }

    @Test
    public void TestSimpleBuiltins_testAsInteger_2612b980873b522f030d35b28adc86cf() {
        assertEval("{ as.integer(list(integer(),2,3)) }");
    }

    @Test
    public void TestSimpleBuiltins_testAsInteger_16e288aed648e941ebc2de58971828f5() {
        assertEval("{ as.integer(list(list(1),2,3)) }");
    }

    @Test
    public void TestSimpleBuiltins_testAsInteger_efc9ab8511187da18c9d6555306dc68a() {
        assertEval("{ as.integer(list(1,2,3,list())) }");
    }

    @Test
    public void TestSimpleBuiltins_testAsInteger_9e913fca8b29e8038c02e04e69e8db87() {
        assertEval("{ as.integer(as.raw(1)) }");
    }

    @Test
    public void TestSimpleBuiltins_testAsInteger_4ba2c45f945b7d8bfed6f912ba52a340() {
        assertEval("{ x<-c(a=1.1, b=2.2); dim(x)<-c(1,2); attr(x, \"foo\")<-\"foo\"; y<-as.integer(x); attributes(y) }");
    }

    @Test
    public void TestSimpleBuiltins_testAsInteger_9c882e3c87b92d65d6ad743d230ec501() {
        assertEval("{ x<-c(a=1L, b=2L); dim(x)<-c(1,2); attr(x, \"foo\")<-\"foo\"; y<-as.integer(x); attributes(y) }");
    }

    @Test
    public void TestSimpleBuiltins_testAsInteger_27316ea084f9b602e2ca9b7756c0b726() {
        assertEval("{ as.integer(1.1:5.1) }");
    }

    @Test
    public void TestSimpleBuiltins_testAsInteger_bdee86802f4d5a14a501cbdf208e49b7() {
        assertEvalWarning("{ as.integer(10+2i) }");
    }

    @Test
    public void TestSimpleBuiltins_testAsInteger_20c98748532ab2f9fba5f8f6e882a4b8() {
        assertEvalWarning("{ as.integer(c(3+3i, 4+4i)) }");
    }

    @Test
    public void TestSimpleBuiltins_testAsInteger_61d2ee56099d039eff403fe24f12eaa5() {
        assertEvalWarning("{ as.integer(10000000000000) }");
    }

    @Test
    public void TestSimpleBuiltins_testAsInteger_cd4b790d81dbb46f2e4bc1e8874e7f84() {
        assertEvalWarning("{ as.integer(10000000000) }");
    }

    @Test
    public void TestSimpleBuiltins_testAsInteger_10ae2ab3382a5b5a08b3c994ff8af6a4() {
        assertEvalWarning("{ as.integer(-10000000000) }");
    }

    @Test
    public void TestSimpleBuiltins_testAsInteger_3fd0c179095e1763a074173595c24e9b() {
        assertEvalWarning("{ as.integer(c(\"1\",\"hello\")) }");
    }

    @Test
    public void TestSimpleBuiltins_testAsInteger_a6edcce0ad2597fc069f850f20117738() {
        assertEvalWarning("{ as.integer(\"TRUE\") }");
    }

    @Test
    public void TestSimpleBuiltins_testAsLogical_7c48b732e347c6a32529c0fc9828c898() {
        assertEval("{ as.logical(1) }");
    }

    @Test
    public void TestSimpleBuiltins_testAsLogical_59751f85efb4b5185a40ce38344ed83a() {
        assertEval("{ as.logical(\"false\") }");
    }

    @Test
    public void TestSimpleBuiltins_testAsLogical_102dabbf6235c7777b4863d44c531a5c() {
        assertEval("{ as.logical(\"dummy\") }");
    }

    @Test
    public void TestSimpleBuiltins_testAsLogical_9d9f6468397bd0dc22331958628d3fce() {
        assertEval("{ x<-c(a=1.1, b=2.2); dim(x)<-c(1,2); attr(x, \"foo\")<-\"foo\"; y<-as.logical(x); attributes(y) }");
    }

    @Test
    public void TestSimpleBuiltins_testAsLogical_32deb020cd94418c900ab70fb006b75d() {
        assertEval("{ x<-c(a=1L, b=2L); dim(x)<-c(1,2); attr(x, \"foo\")<-\"foo\"; y<-as.logical(x); attributes(y) }");
    }

    @Test
    public void TestSimpleBuiltins_testAsLogical_0566cd14ef0633a31e86fdfec9ee3d68() {
        assertEval("{ as.logical(c(\"1\",\"hello\")) }");
    }

    @Test
    public void TestSimpleBuiltins_testAsLogical_b51b2ababbc6661feaaec2f13a052999() {
        assertEval("{ as.logical(\"TRUE\") }");
    }

    @Test
    public void TestSimpleBuiltins_testAsLogical_0de6fee948230776fad55dbddab7a52f() {
        assertEval("{ as.logical(10+2i) }");
    }

    @Test
    public void TestSimpleBuiltins_testAsLogical_23d0498579b5646b7c8ff983c9f82171() {
        assertEval("{ as.logical(c(3+3i, 4+4i)) }");
    }

    @Test
    public void TestSimpleBuiltins_testAsRaw_45e9a2f9092baa7b059e1d16b0a2dc36() {
        assertEval("{ as.raw(NULL) }");
    }

    @Test
    public void TestSimpleBuiltins_testAsRaw_8d9753300ecdc7e581bc47059d584961() {
        assertEval("{ as.raw(1) }");
    }

    @Test
    public void TestSimpleBuiltins_testAsRaw_7f0a3d068c37ad40d40740f9a712cc01() {
        assertEval("{ as.raw(1L) }");
    }

    @Test
    public void TestSimpleBuiltins_testAsRaw_810e96ba01dde0b9203a17a350b5ca70() {
        assertEval("{ as.raw(1.1) }");
    }

    @Test
    public void TestSimpleBuiltins_testAsRaw_535958221e3ffa3f60a58fb378e2204b() {
        assertEval("{ as.raw(c(1, 2, 3)) }");
    }

    @Test
    public void TestSimpleBuiltins_testAsRaw_38735b88140ffffd266035fe836d4832() {
        assertEval("{ as.raw(c(1L, 2L, 3L)) }");
    }

    @Test
    public void TestSimpleBuiltins_testAsRaw_d2566bc3e1098609dd899369c2ab527d() {
        assertEval("{ as.raw(list(1,2,3)) }");
    }

    @Test
    public void TestSimpleBuiltins_testAsRaw_a8a0e89246d5e8768b3ee2bf0ae77631() {
        assertEval("{ as.raw(list(\"1\", 2L, 3.4)) }");
    }

    @Test
    public void TestSimpleBuiltins_testAsRawIgnore_234cefc5ff95e036f3fa00ff5e0f2088() {
        assertEvalWarning("{ as.raw(1+1i) }");
    }

    @Test
    public void TestSimpleBuiltins_testAsRawIgnore_6cd26c8e6df8ead2edcbb6df45860698() {
        assertEvalWarning("{ as.raw(-1) }");
    }

    @Test
    public void TestSimpleBuiltins_testAsRawIgnore_b2f6c512a8b92fcc2861d16b643c77e8() {
        assertEvalWarning("{ as.raw(-1L) }");
    }

    @Test
    public void TestSimpleBuiltins_testAsRawIgnore_6e315b6de04235063bc8e0be93dc9780() {
        assertEvalWarning("{ as.raw(NA) }");
    }

    @Test
    public void TestSimpleBuiltins_testAsRawIgnore_3dced2b189d4bc08910497a4c84f8eaf() {
        assertEvalWarning("{ as.raw(\"test\") }");
    }

    @Test
    public void TestSimpleBuiltins_testAsRawIgnore_75c975b8a03a598a4af0a0a332d02b71() {
        assertEvalWarning("{ as.raw(c(1+3i, -2-1i, NA)) }");
    }

    @Test
    public void TestSimpleBuiltins_testAsRawIgnore_747e8b9acbda8dd5dc02ce1274604e0c() {
        assertEvalWarning("{ as.raw(c(1, -2, 3)) }");
    }

    @Test
    public void TestSimpleBuiltins_testAsRawIgnore_ad6deee1d16519ee2ce790bb577709d2() {
        assertEvalWarning("{ as.raw(c(1,1000,NA)) }");
    }

    @Test
    public void TestSimpleBuiltins_testAsRawIgnore_d799cae365e5f001ed4de47c2cbbff01() {
        assertEvalWarning("{ as.raw(c(1L, -2L, 3L)) }");
    }

    @Test
    public void TestSimpleBuiltins_testAsRawIgnore_05185c7c12248d279730674e3b527d86() {
        assertEvalWarning("{ as.raw(c(1L, -2L, NA)) }");
    }

    @Test
    public void TestSimpleBuiltins_testAsVector_e84fd1b27fd6570ddd6a53234fde0ba0() {
        assertEval("{ as.vector(\"foo\", \"logical\") }");
    }

    @Test
    public void TestSimpleBuiltins_testAsVector_cb0809ef1d3ab2fc8312585316b3ff3f() {
        assertEval("{ as.vector(\"foo\", \"character\") }");
    }

    @Test
    public void TestSimpleBuiltins_testAsVector_ad178529ec6948277d4535f8ce4d1307() {
        assertEval("{ as.vector(\"foo\", \"list\") }");
    }

    @Test
    public void TestSimpleBuiltins_testAsVector_715c056f5c63c0038099fb48bed34bad() {
        assertEval("{ as.vector(\"foo\") }");
    }

    @Test
    public void TestSimpleBuiltins_testAsVector_7df3d852bd307ec74005874e9e6b860a() {
        assertEval("x<-c(a=1.1, b=2.2); as.vector(x, \"raw\")");
    }

    @Test
    public void TestSimpleBuiltins_testAsVector_68bdfdc99e081735270c25dad5c6438c() {
        assertEval("x<-c(a=1L, b=2L); as.vector(x, \"complex\")");
    }

    @Test
    public void TestSimpleBuiltins_testAsVector_35065e6784253d5d096669f4fee13932() {
        assertEval("{ x<-c(a=FALSE, b=TRUE); attr(x, \"foo\")<-\"foo\"; y<-as.vector(x); attributes(y) }");
    }

    @Test
    public void TestSimpleBuiltins_testAsVector_5b09e05fda45f41d37a149696f95babb() {
        assertEval("{ x<-c(a=1, b=2); as.vector(x, \"list\") }");
    }

    @Test
    public void TestSimpleBuiltins_testAsVector_824741c5ce8c39da5eb71c8a537981b6() {
        assertEval("{ x<-c(a=FALSE, b=TRUE); attr(x, \"foo\")<-\"foo\"; y<-as.vector(x, \"list\"); attributes(y) }");
    }

    @Test
    public void TestSimpleBuiltins_testAsVector_bfb16efc959f2032f3fd07f7282ad495() {
        assertEval("{ x<-1:4; dim(x)<-c(2, 2); dimnames(x)<-list(c(\"a\", \"b\"), c(\"c\", \"d\")); y<-as.vector(x, \"list\"); y }");
    }

    @Test
    public void TestSimpleBuiltins_testAsVector_8d0d1cdaff856d660d11866fa044397e() {
        assertEvalError("{ as.vector(\"foo\", \"bar\") }");
    }

    @Test
    public void TestSimpleBuiltins_testAsVector_e20c6558cca6aea70251347229e52bf5() {
        assertEvalWarning("{ as.vector(\"foo\", \"integer\") }");
    }

    @Test
    public void TestSimpleBuiltins_testAsVector_f66e36fa2296e57c288db82f5f8e9798() {
        assertEvalWarning("{ as.vector(\"foo\", \"double\") }");
    }

    @Test
    public void TestSimpleBuiltins_testAsVector_0a44fb77142709976b0a7a4642abaf1f() {
        assertEvalWarning("{ as.vector(\"foo\", \"numeric\") }");
    }

    @Test
    public void TestSimpleBuiltins_testAsVector_84c625ebcd4124d15ba6dbf9b32d8293() {
        assertEvalWarning("{ as.vector(\"foo\", \"raw\") }");
    }

    @Test
    public void TestSimpleBuiltins_testAsVector_73bc292dddea0adb6832fc9f79b8d507() {
        assertEvalWarning("{ as.vector(c(\"foo\", \"bar\"), \"raw\") }");
    }

    @Test
    public void TestSimpleBuiltins_testAttributes_7a7da0b52cfc6f6dbfdd195db4c141e9() {
        assertEval("{ x <- 1; attributes(x) }");
    }

    @Test
    public void TestSimpleBuiltins_testAttributes_e369cb73967dc353f1e0afd2e44f7f0e() {
        assertEval("{ x <- 1; names(x) <- \"hello\" ; attributes(x) }");
    }

    @Test
    public void TestSimpleBuiltins_testAttributes_d92b47e84d847dd681b283e9260b6526() {
        assertEval("{ x <- 1:3 ; attr(x, \"myatt\") <- 2:4 ; attributes(x) }");
    }

    @Test
    public void TestSimpleBuiltins_testAttributes_61b1d4303e650fa729c3fef02e0ae698() {
        assertEval("{ x <- 1:3 ; attr(x, \"myatt\") <- 2:4 ; attr(x, \"myatt1\") <- \"hello\" ; attributes(x) }");
    }

    @Test
    public void TestSimpleBuiltins_testAttributes_332b1db258467b26ab72c5056a2ca51d() {
        assertEval("{ x <- 1:3 ; attr(x, \"myatt\") <- 2:4 ; y <- x; attr(x, \"myatt1\") <- \"hello\" ; attributes(y) }");
    }

    @Test
    public void TestSimpleBuiltins_testAttributes_bf9508421334e223031d350a516cd544() {
        assertEval("{ x <- c(a=1, b=2) ; attr(x, \"myatt\") <- 2:4 ; y <- x; attr(x, \"myatt1\") <- \"hello\" ; attributes(y) }");
    }

    @Test
    public void TestSimpleBuiltins_testAttributes_3010b4e4b789d1b465016c6f7faaf08d() {
        assertEval("{ x <- c(a=1, b=2) ; attr(x, \"names\") }");
    }

    @Test
    public void TestSimpleBuiltins_testAttributes_440a367f3e9cf09e1c582accf31d2692() {
        assertEval("{ x <- c(a=1, b=2) ; attr(x, \"na\") }");
    }

    @Test
    public void TestSimpleBuiltins_testAttributes_36febdbf7b69f4e073c589f0f023cc4e() {
        assertEval("{ x <- c(a=1, b=2) ; attr(x, \"mya\") <- 1; attr(x, \"b\") <- 2; attr(x, \"m\") }");
    }

    @Test
    public void TestSimpleBuiltins_testAttributes_d58f9574d72e4e4098e214fcc7e3ba00() {
        assertEval("{ x <- 1:2; attr(x, \"aa\") <- 1 ; attr(x, \"ab\") <- 2; attr(x, \"bb\") <- 3; attr(x, \"b\") }");
    }

    @Test
    public void TestSimpleBuiltins_testAttributes_eec7280497511f485c3a5d3aa0430188() {
        assertEval("{ z <- 1; attr(z,\"a\") <- 1; attr(z,\"b\") <- 2; attr(z,\"c\") <- 3 ; attr(z,\"b\") <- NULL ; z }");
    }

    @Test
    public void TestSimpleBuiltins_testAttributes_34c4624d3b3ab86bc215536ac25578a1() {
        assertEval("{ x <- 1 ; attributes(x) <- list(hi=3, hello=2) ; x }");
    }

    @Test
    public void TestSimpleBuiltins_testAttributes_0994903f7e11c4a54f386ac7d9019585() {
        assertEval("{ x <- 1 ; attributes(x) <- list(hi=3, names=\"name\") ; x }");
    }

    @Test
    public void TestSimpleBuiltins_testAttributes_87e2457fb1a9d6a7e176775eb6366980() {
        assertEval("{ x <- c(hello=1) ; attributes(x) <- list(names=NULL) ; x }");
    }

    @Test
    public void TestSimpleBuiltins_testAttributes_43e8ba6d4f12efd38adf6b788556a34e() {
        assertEval("{ x <- 1; attributes(x) <- list(my = 1) ; y <- x; attributes(y) <- list(his = 2) ; x }");
    }

    @Test
    public void TestSimpleBuiltins_testAttributes_43f74452c912404024bee6bbeb50f37d() {
        assertEval("{ x <- c(hello=1) ; attributes(x) <- list(hi=1) ;  attributes(x) <- NULL ; x }");
    }

    @Test
    public void TestSimpleBuiltins_testAttributes_762ced2b79ff52c63ed69b50c19aba14() {
        assertEval("{ x <- c(hello=1) ; attributes(x) <- list(hi=1, names=NULL, hello=3, hi=2, hello=NULL) ; x }");
    }

    @Test
    public void TestSimpleBuiltins_testAttributes_91959c3aba2c22f7ce55fda369b2089e() {
        assertEval("{ x<-1; attributes(x)<-list(names=\"c\", dim=NULL); attributes(x) }");
    }

    @Test
    public void TestSimpleBuiltins_testAttributes_6389f590bfe766984916afeca9781c4a() {
        assertEvalError("{ x <- c(hello=1) ; attributes(x) <- list(hi = 1, 2) ; x }");
    }

    @Test
    public void TestSimpleBuiltins_testAttributes_8c74e3ead6decbba9caf660787bd966b() {
        assertEvalError("{ x <- c(hello=1) ; attributes(x) <- list(1, hi = 2) ; x }");
    }

    @Test
    public void TestSimpleBuiltins_testAttributes_52e1a1b5231a99ede49b8606c1abccb3() {
        assertEvalError("{ x <- c(hello=1) ; attributes(x) <- list(ho = 1, 2, 3) ; x }");
    }

    @Test
    public void TestSimpleBuiltins_testAttributes_cf6f42b2d0b5931f4a325ab22e39f03a() {
        assertEvalError("{ x <- c(hello=1) ; attributes(x) <- list(1, hi = 2, 3) ; x }");
    }

    @Test
    public void TestSimpleBuiltins_testBuiltinPropagationIgnore_815238e2a76d61eb69db36c00e322f34() {
        assertEval("{ m <- matrix(1:6, nrow=2) ; attr(m,\"a\") <- 1 ;  aperm(m) }");
    }

    @Test
    public void TestSimpleBuiltins_testBuiltinPropagationIgnore_d7181010a1cd39e67a56ceb71922fff9() {
        assertEval("{ x <- c(a=1, b=2) ; attr(x, \"myatt\") <- 1 ; sapply(1:2, function(z) {x}) }");
    }

    @Test
    public void TestSimpleBuiltins_testBuiltinPropagationIgnore_d789eedbfc9166e0b7f70ef343f75e96() {
        assertEval("{ x <- c(a=1) ; attr(x, \"myatt\") <- 1 ; lapply(1:2, function(z) {x}) }");
    }

    @Test
    public void TestSimpleBuiltins_testBuiltinPropagationIgnore_df9b3724960b222fffd20b6a1ef94ed5() {
        assertEval("{ m <- matrix(c(1,1,1,1), nrow=2) ; attr(m,\"a\") <- 1 ;  r <- eigen(m) ; r$vectors <- round(r$vectors, digits=5) ; r  }");
    }

    @Test
    public void TestSimpleBuiltins_testBuiltinPropagationIgnore_34276682124e7b74954e779277f54a3f() {
        assertEval("{ x <- 1 ; attr(x, \"myatt\") <- 1; round(exp(x), digits=5) }");
    }

    @Test
    public void TestSimpleBuiltins_testBuiltinPropagationIgnore_dd00df1d23bd40731a3be30ec8fa4cbe() {
        assertEval("{ x <- c(a=TRUE) ; attr(x, \"myatt\") <- 1; rep(x,2) }");
    }

    @Test
    public void TestSimpleBuiltins_testBuiltinPropagationIgnore_1c5a0061ff8753565f24001f9747bc4e() {
        assertEval("{ x <- c(a=1, b=2) ; attr(x, \"myatt\") <- 1; seq(x) }");
    }

    @Test
    public void TestSimpleBuiltins_testCall_7d3147e26292301cfabf8939c17af430() {
        assertEval("{ f <- function(a, b) { a + b } ; l <- call(\"f\", 2, 3) ; eval(l) }");
    }

    @Test
    public void TestSimpleBuiltins_testCall_ac5601b7f27d60cead4d93b849fd38ca() {
        assertEval("{ f <- function(a, b) { a + b } ; x <- 1 ; y <- 2 ; l <- call(\"f\", x, y) ; x <- 10 ; eval(l) }");
    }

    @Test
    public void TestSimpleBuiltins_testCasts_54020b2a34bff2cae05e6014769467c3() {
        assertEval("{ as.complex(as.character(c(1+1i,1+1i))) }");
    }

    @Test
    public void TestSimpleBuiltins_testCasts_a9dc7f92ad5513354d7ffc4a01bf2515() {
        assertEval("{ as.complex(as.integer(c(1+1i,1+1i))) }");
    }

    @Test
    public void TestSimpleBuiltins_testCasts_cf9d84ba001a43404aab67fea70a7b30() {
        assertEval("{ as.complex(as.logical(c(1+1i,1+1i))) }");
    }

    @Test
    public void TestSimpleBuiltins_testCasts_a13729125ad49e4bd3a829df96d7d97d() {
        assertEval("{ as.double(as.logical(c(10,10))) }");
    }

    @Test
    public void TestSimpleBuiltins_testCasts_819f841f683284ba0d258ed83444bf8e() {
        assertEval("{ as.integer(as.logical(-1:1)) }");
    }

    @Test
    public void TestSimpleBuiltins_testCasts_f6420f1f19e0a4c5f8b1f7741a12aece() {
        assertEval("{ as.raw(as.logical(as.raw(c(1,2)))) }");
    }

    @Test
    public void TestSimpleBuiltins_testCasts_f4ba0d0a8fe074cef6aed7850c3a269c() {
        assertEval("{ as.character(as.double(1:5)) }");
    }

    @Test
    public void TestSimpleBuiltins_testCasts_3c5e393480880b499b521f7e64e16ede() {
        assertEval("{ as.character(as.complex(1:2)) }");
    }

    @Test
    public void TestSimpleBuiltins_testCasts_ab34701fbf0da93227aaf2e275e5f53b() {
        assertEval("{ m<-matrix(1:6, nrow=3) ; as.integer(m) }");
    }

    @Test
    public void TestSimpleBuiltins_testCasts_b102360ceadcc9e72d963d3292564c09() {
        assertEval("{ m<-matrix(1:6, nrow=3) ; as.vector(m, \"any\") }");
    }

    @Test
    public void TestSimpleBuiltins_testCasts_6bb5abf859bc98f1c437cd8ac62d8840() {
        assertEval("{ m<-matrix(1:6, nrow=3) ; as.vector(mode = \"integer\", x=m) }");
    }

    @Test
    public void TestSimpleBuiltins_testCasts_871c422239af2faa5f074e5af1bd7d88() {
        assertEval("{ m<-matrix(c(1,2,3,4), nrow=2) ; as.vector(m, mode = \"double\") }");
    }

    @Test
    public void TestSimpleBuiltins_testCasts_dc4e82fd153b9c51b36e38b143336be4() {
        assertEval("{ m<-matrix(c(1,2,3,4), nrow=2) ; as.vector(m, mode = \"numeric\") }");
    }

    @Test
    public void TestSimpleBuiltins_testCasts_1e5d34d601096f21c5ee66cf058475ec() {
        assertEval("{ m<-matrix(c(1,2,3,4), nrow=2) ; as.vector(m) }");
    }

    @Test
    public void TestSimpleBuiltins_testCasts_7cd17efc5cab7272ddef9006faff8304() {
        assertEval("{ m<-matrix(c(TRUE,FALSE,FALSE,TRUE), nrow=2) ; as.vector(m) }");
    }

    @Test
    public void TestSimpleBuiltins_testCasts_35ebe2837247bbbe8a131689ab098393() {
        assertEval("{ m<-matrix(c(1+1i,2+2i,3-3i,4-4i), nrow=2) ; as.vector(m) }");
    }

    @Test
    public void TestSimpleBuiltins_testCasts_aca53f11bd350624e752dc6ef16f988d() {
        assertEval("{ m<-matrix(c(\"a\",\"b\",\"c\",\"d\"), nrow=2) ; as.vector(m) }");
    }

    @Test
    public void TestSimpleBuiltins_testCasts_cd7bcce839bb8157fcde70ac60d41569() {
        assertEval("{ m<-matrix(as.raw(c(1,2,3,4)), nrow=2) ; as.vector(m) }");
    }

    @Test
    public void TestSimpleBuiltins_testCasts_93624a48c554db2f45fa8eab015805f1() {
        assertEval("{ m <- matrix(1:6, nrow=2) ; as.double(m) }");
    }

    @Test
    public void TestSimpleBuiltins_testCasts_ae4b3d8116361a498946ec4840c49f2c() {
        assertEval("{ m <- matrix(c(1,2,3,4,5,6), nrow=2) ; as.integer(m) }");
    }

    @Test
    public void TestSimpleBuiltins_testCasts_25d8dbb8ec109837dd9837a260fd8d63() {
        assertEval("{ m <- matrix(c(1,2,3,4,5,6), nrow=2) ; as.logical(m) }");
    }

    @Test
    public void TestSimpleBuiltins_testCasts_66d97bd9b62a7ceb6e86b15c223e2c0f() {
        assertEval("{ x <- c(0,2); names(x) <- c(\"hello\",\"hi\") ; as.logical(x) }");
    }

    @Test
    public void TestSimpleBuiltins_testCasts_54e8a60a9ca2de801b9a9fabfbe913e6() {
        assertEval("{ x <- 1:2; names(x) <- c(\"hello\",\"hi\") ; as.double(x) }");
    }

    @Test
    public void TestSimpleBuiltins_testCasts_b38b4200a82fdcbfd43868aede5564fb() {
        assertEval("{ x <- c(1,2); names(x) <- c(\"hello\",\"hi\") ; as.integer(x) }");
    }

    @Test
    public void TestSimpleBuiltins_testCasts_97d4b6e51e8e88e14a04e0f452ea5159() {
        assertEval("{ m<-matrix(c(1,0,1,0), nrow=2) ; as.vector(m, mode = \"logical\") }");
    }

    @Test
    public void TestSimpleBuiltins_testCasts_5dd4341a59ca7d0e2ed6b0d084e792db() {
        assertEval("{ m<-matrix(c(1,2,3,4), nrow=2) ; as.vector(m, mode = \"complex\") }");
    }

    @Test
    public void TestSimpleBuiltins_testCasts_2313113d77f87db77406408465c55c7e() {
        assertEval("{ m<-matrix(c(1,2,3,4), nrow=2) ; as.vector(m, mode = \"character\") }");
    }

    @Test
    public void TestSimpleBuiltins_testCasts_b6184a32a36c6baf7d41451b40d049ba() {
        assertEval("{ m<-matrix(c(1,2,3,4), nrow=2) ; as.vector(m, mode = \"raw\") }");
    }

    @Test
    public void TestSimpleBuiltins_testCasts_5ecedc65b348c959e86222b948f34ce8() {
        assertEval("{ as.vector(list(1,2,3), mode=\"integer\") }");
    }

    @Test
    public void TestSimpleBuiltins_testCasts_63f3c5f285a5128c0781e57db7d90f4d() {
        assertEval("{ k <- as.list(3:6) ; l <- as.list(1) ; list(k,l) }");
    }

    @Test
    public void TestSimpleBuiltins_testCasts_769ed9210ad0b045ab34ce853b3b973a() {
        assertEval("{ as.list(list(1,2,\"eep\")) }");
    }

    @Test
    public void TestSimpleBuiltins_testCasts_d68d399b08fbed0e501c66049723e6e3() {
        assertEval("{ as.list(c(1,2,3,2,1)) }");
    }

    @Test
    public void TestSimpleBuiltins_testCasts_99e069c3e73e7d68d3674c7b340148bb() {
        assertEval("{ as.list(3:6) }");
    }

    @Test
    public void TestSimpleBuiltins_testCasts_bf55a9d7ec96d782af8d9529a2f38293() {
        assertEval("{ l <- list(1) ; attr(l, \"my\") <- 1; as.list(l) }");
    }

    @Test
    public void TestSimpleBuiltins_testCasts_3491879b65e4aca7ee4eb07254dd1ba0() {
        assertEval("{ l <- 1 ; attr(l, \"my\") <- 1; as.list(l) }");
    }

    @Test
    public void TestSimpleBuiltins_testCasts_f53133a962e1057652661353bb342c97() {
        assertEval("{ l <- c(x=1) ; as.list(l) }");
    }

    @Test
    public void TestSimpleBuiltins_testCastsIgnore_fd41615e647202e9a7f994c633674ca4() {
        assertEval("{ as.matrix(1) }");
    }

    @Test
    public void TestSimpleBuiltins_testCastsIgnore_c9e133e0d7fd2ee951acf79fd6d3f133() {
        assertEval("{ as.matrix(1:3) }");
    }

    @Test
    public void TestSimpleBuiltins_testCastsIgnore_9887ea3892849f36e6cad0e4fc3793fa() {
        assertEval("{ x <- 1:3; z <- as.matrix(x); x }");
    }

    @Test
    public void TestSimpleBuiltins_testCastsIgnore_e446fc18e1ac80f3580fd22c9214d841() {
        assertEval("{ x <- 1:3 ; attr(x,\"my\") <- 10 ; attributes(as.matrix(x)) }");
    }

    @Test
    public void TestSimpleBuiltins_testCastsIgnore_a695ef4253fbba58b28a3e8cbcfb1987() {
        assertEval("{ as.complex(as.double(c(1+1i,1+1i))) }");
    }

    @Test
    public void TestSimpleBuiltins_testCastsIgnore_1785fd6355c91d5f76f56cd5bd8eac86() {
        assertEval("{ as.complex(as.raw(c(1+1i,1+1i))) }");
    }

    @Test
    public void TestSimpleBuiltins_testCat_75fb049558704ed597a4d49441bae349() {
        assertEvalNoNL("{ cat(1) }");
    }

    @Test
    public void TestSimpleBuiltins_testCat_f48953c23109705840adb64bc147151b() {
        assertEvalNoNL("{ cat(1,2,3) }");
    }

    @Test
    public void TestSimpleBuiltins_testCat_a81cf8070ed30384fb6e475357ba6ef8() {
        assertEvalNoNL("{ cat(\"a\") }");
    }

    @Test
    public void TestSimpleBuiltins_testCat_17b9c8977c744dc3a518927c5e2b3f09() {
        assertEvalNoNL("{ cat(\"a\", \"b\") }");
    }

    @Test
    public void TestSimpleBuiltins_testCat_eeff96fd8a6223eef32597eb5a8d122e() {
        assertEvalNoNL("{ cat(1, \"a\") }");
    }

    @Test
    public void TestSimpleBuiltins_testCat_7a2128db30707429040a36f92034b6cd() {
        assertEvalNoNL("{ cat(c(1,2,3)) }");
    }

    @Test
    public void TestSimpleBuiltins_testCat_bddd79155851c51527a56d721c6a5ca7() {
        assertEvalNoNL("{ cat(c(\"a\",\"b\")) }");
    }

    @Test
    public void TestSimpleBuiltins_testCat_28b3773384f60d21783ce562b6af2054() {
        assertEvalNoNL("{ cat(c(1,2,3),c(\"a\",\"b\")) }");
    }

    @Test
    public void TestSimpleBuiltins_testCat_320c1a9bf7626562dbaa7c8df589911f() {
        assertEvalNoNL("{ cat(TRUE) }");
    }

    @Test
    public void TestSimpleBuiltins_testCat_d0340760d75ec0574dcfb5f2911b8a76() {
        assertEvalNoNL("{ cat(TRUE, c(1,2,3), FALSE, 7, c(\"a\",\"b\"), \"x\") }");
    }

    @Test
    public void TestSimpleBuiltins_testCat_aa0b2066d8aae3f345ec5228155c79c9() {
        assertEvalNoNL("{ cat(1:3) }");
    }

    @Test
    public void TestSimpleBuiltins_testCat_552441fae9eb3e1a0c1a2581f8262af5() {
        assertEvalNoNL("{ cat(\"hi\",1:3,\"hello\") }");
    }

    @Test
    public void TestSimpleBuiltins_testCat_841a466346563a15e8a9bfafef0e2b8e() {
        assertEvalNoNL("{ cat(2.3) }");
    }

    @Test
    public void TestSimpleBuiltins_testCat_2b31ce579fc7c9f6166788e9922374af() {
        assertEvalNoNL("{ cat(1.2,3.4) }");
    }

    @Test
    public void TestSimpleBuiltins_testCat_d2dd302303cc1c2828fdb841e9329b7d() {
        assertEvalNoNL("{ cat(c(1.2,3.4),5.6) }");
    }

    @Test
    public void TestSimpleBuiltins_testCat_ce7de0a3b63f9b62750fcaeb0d4bb324() {
        assertEvalNoNL("{ cat(c(TRUE,FALSE), TRUE) }");
    }

    @Test
    public void TestSimpleBuiltins_testCat_f79a3c333c0aa46279a83870d12d4e38() {
        assertEvalNoNL("{ cat(NULL) }");
    }

    @Test
    public void TestSimpleBuiltins_testCat_d08462e99741a36b3646a6a147d53b1a() {
        assertEvalNoNL("{ cat(1L) }");
    }

    @Test
    public void TestSimpleBuiltins_testCat_c5ec77152f3be07536df7806d49a1b49() {
        assertEvalNoNL("{ cat(1L, 2L, 3L) }");
    }

    @Test
    public void TestSimpleBuiltins_testCat_c46a1d13e87c8a06a425354c68780eae() {
        assertEvalNoNL("{ cat(c(1L, 2L, 3L)) }");
    }

    @Test
    public void TestSimpleBuiltins_testCat_8de12861bed933861dbbda4217d79576() {
        assertEvalNoNL("{ cat(1,2,sep=\".\") }");
    }

    @Test
    public void TestSimpleBuiltins_testCat_8f49d4d29ce2ef92acf8dc9735d6ebde() {
        assertEvalNoNL("{ cat(\"hi\",1[2],\"hello\",sep=\"-\") }");
    }

    @Test
    public void TestSimpleBuiltins_testCat_1dcd33ce696ea60d54dd157e9d0dd76f() {
        assertEvalNoNL("{ m <- matrix(as.character(1:6), nrow=2) ; cat(m) }");
    }

    @Test
    public void TestSimpleBuiltins_testCat_a3f20135d23156229fddeb4a1f3b8cb0() {
        assertEvalNoNL("{ cat(sep=\" \", \"hello\") }");
    }

    @Test
    public void TestSimpleBuiltins_testCat_c991c3c93c858a033c1e8f63f4994441() {
        assertEvalNoOutput("{ cat() }");
    }

    @Test
    public void TestSimpleBuiltins_testCatIgnore_01ac467ff40598b5a055378fc7882537() {
        assertEvalNoNL("{ cat(\"hi\",NULL,\"hello\",sep=\"-\") }");
    }

    @Test
    public void TestSimpleBuiltins_testCatIgnore_4949a7df83738286ea025e86159c9cdc() {
        assertEvalNoNL("{ cat(\"hi\",integer(0),\"hello\",sep=\"-\") }");
    }

    @Test
    public void TestSimpleBuiltins_testCbind_b6d50b783340cfd840b5d0d1c478e85a() {
        assertEval("{ cbind() }");
    }

    @Test
    public void TestSimpleBuiltins_testCbind_9a0a0da1e5502a4f4fae56ffc4825c65() {
        assertEval("{ cbind(1:3,2) }");
    }

    @Test
    public void TestSimpleBuiltins_testCbind_268852e66f83c27413b9dec6a2e20fee() {
        assertEval("{ cbind(1:3,1:3) }");
    }

    @Test
    public void TestSimpleBuiltins_testCbind_e9c27c728aecbd97c38d9fd72b57fe59() {
        assertEval("{ m <- matrix(1:6, nrow=2) ; cbind(11:12, m) }");
    }

    @Test
    public void TestSimpleBuiltins_testCbindIgnore_c292a9a2047519d8fd24923adebb0ad2() {
        assertEval("{ cbind(list(1,2), TRUE, \"a\") }");
    }

    @Test
    public void TestSimpleBuiltins_testCbindIgnore_849d2f7200b6d113f749abbc67d41a7d() {
        assertEval("{ cbind(1:3,1:2) }");
    }

    @Test
    public void TestSimpleBuiltins_testCeiling_7283237f5a0aa7db1d95780ead2af87f() {
        assertEval("{ ceiling(c(0.2,-3.4,NA,0/0,1/0)) }");
    }

    @Test
    public void TestSimpleBuiltins_testCharUtils_38efbb1cdef27318804e0719732d2be0() {
        assertEval("{ toupper(c(\"hello\",\"bye\")) }");
    }

    @Test
    public void TestSimpleBuiltins_testCharUtils_e144f9dec05c311f45516e0c40097270() {
        assertEval("{ tolower(c(\"Hello\",\"ByE\")) }");
    }

    @Test
    public void TestSimpleBuiltins_testCharUtils_b2d1b37847dd587ab6fcb56daf82d532() {
        assertEval("{ toupper(c()) }");
    }

    @Test
    public void TestSimpleBuiltins_testCharUtils_55b97cb36ee6761df82efdad2c963afa() {
        assertEval("{ tolower(c()) }");
    }

    @Test
    public void TestSimpleBuiltins_testCharUtilsIgnore_864e89c688384c8cc67d1b4676ff314d() {
        assertEval("{ tolower(1E100) }");
    }

    @Test
    public void TestSimpleBuiltins_testCharUtilsIgnore_69433b6491feff8204434af6a79f9307() {
        assertEval("{ toupper(1E100) }");
    }

    @Test
    public void TestSimpleBuiltins_testCharUtilsIgnore_8f5f00293e9bfb6ac9aab0e3e6c88cf8() {
        assertEval("{ m <- matrix(\"hi\") ; toupper(m) }");
    }

    @Test
    public void TestSimpleBuiltins_testCharUtilsIgnore_e5ad5f71aaa8302b8bcddddde53fd68e() {
        assertEval("{ toupper(c(a=\"hi\", \"hello\")) }");
    }

    @Test
    public void TestSimpleBuiltins_testCharUtilsIgnore_ec31b79bc63b78f141adde800c2de5ab() {
        assertEval("{ tolower(c(a=\"HI\", \"HELlo\")) }");
    }

    @Test
    public void TestSimpleBuiltins_testCharUtilsIgnore_ddbafed30934d43a3a0e4862fb6bd0db() {
        assertEval("{ tolower(NA) }");
    }

    @Test
    public void TestSimpleBuiltins_testCharUtilsIgnore_73009820a93846c10cb6c65b68e5b7fa() {
        assertEval("{ toupper(NA) }");
    }

    @Test
    public void TestSimpleBuiltins_testChol_0c871276d1ef0a12733f4763eca31305() {
        assertEval("{ chol(1) }");
    }

    @Test
    public void TestSimpleBuiltins_testChol_56c7d9d1a9d02d3730de6ef5e4b085b8() {
        assertEval("{ round( chol(10), digits=5) }");
    }

    @Test
    public void TestSimpleBuiltins_testChol_7b9b9fe7c5e51dfc97d44dd7ce4cc95a() {
        assertEval("{ m <- matrix(c(5,1,1,3),2) ; round( chol(m), digits=5 ) }");
    }

    @Test
    public void TestSimpleBuiltins_testChol_887c0d3033dcb17c875cf7a89313563c() {
        assertEvalError("{ m <- matrix(c(5,-5,-5,3),2,2) ; chol(m) }");
    }

    @Test
    public void TestSimpleBuiltins_testColStatsArray_de0f9a9ff80104c9d0ef40a135515034() {
        assertEval("{ a = colSums(array(1:24,c(2,3,4))); d = dim(a); c(d[1],d[2]) }");
    }

    @Test
    public void TestSimpleBuiltins_testColStatsArray_a30963b6dca5e14240a90e527026ee60() {
        assertEval("{ a = colSums(array(1:24,c(2,3,4))); length(a) }");
    }

    @Test
    public void TestSimpleBuiltins_testColStatsArray_582b93151f22d1875bb3d55b96a98b49() {
        assertEval("{ a = colSums(array(1:24,c(2,3,4))); c(a[1,1],a[2,2],a[3,3],a[3,4]) }");
    }

    @Test
    public void TestSimpleBuiltins_testColStatsMatrix_14e640ab8bfe09b1978ec5e7af8e398a() {
        assertEval("{ a = colSums(matrix(1:12,3,4)); dim(a) }");
    }

    @Test
    public void TestSimpleBuiltins_testColStatsMatrix_ec2ec8152ef861eccf1eac7113f091ae() {
        assertEval("{ a = colSums(matrix(1:12,3,4)); length(a) }");
    }

    @Test
    public void TestSimpleBuiltins_testColStatsMatrix_508a072dbdad283f34eec61f652bfcc1() {
        assertEval("{ colSums(matrix(1:12,3,4)) }");
    }

    @Test
    public void TestSimpleBuiltins_testColumnsRowsStat_95ac91f94526d949beeb5afe58aea573() {
        assertEval("{ m <- matrix(1:6, nrow=2) ; colSums(na.rm = FALSE, x = m) }");
    }

    @Test
    public void TestSimpleBuiltins_testColumnsRowsStatIgnore_8ca9d6c7f776a8e3441d264e1da328a6() {
        assertEval("{ m <- matrix(1:6, nrow=2) ; colMeans(m) }");
    }

    @Test
    public void TestSimpleBuiltins_testColumnsRowsStatIgnore_42f4dcf92af03ea106b9ee137d80a60b() {
        assertEval("{ m <- matrix(1:6, nrow=2) ; rowMeans(x = m, na.rm = TRUE) }");
    }

    @Test
    public void TestSimpleBuiltins_testColumnsRowsStatIgnore_f6e099a48acc0dc03f8df25fddeaa2ac() {
        assertEval("{ m <- matrix(1:6, nrow=2) ; rowSums(x = m) }");
    }

    @Test
    public void TestSimpleBuiltins_testColumnsRowsStatIgnore_5a6a0c0306ea58dc330442a0ee35ac57() {
        assertEval("{ m <- matrix(c(1,2,3,4,5,6), nrow=2) ; colMeans(m) }");
    }

    @Test
    public void TestSimpleBuiltins_testColumnsRowsStatIgnore_9589a0af44218e0e9ffcf6a4ddb95ee3() {
        assertEval("{ m <- matrix(c(1,2,3,4,5,6), nrow=2) ; rowMeans(m) }");
    }

    @Test
    public void TestSimpleBuiltins_testColumnsRowsStatIgnore_27fd0c0df27edacc427f026c6f82c11e() {
        assertEval("{ m <- matrix(c(1,2,3,4,5,6), nrow=2) ; colSums(m) }");
    }

    @Test
    public void TestSimpleBuiltins_testColumnsRowsStatIgnore_7a7b23e72604196aca2933d8326855f8() {
        assertEval("{ m <- matrix(c(1,2,3,4,5,6), nrow=2) ; rowSums(m) }");
    }

    @Test
    public void TestSimpleBuiltins_testColumnsRowsStatIgnore_6e3237bc98188617dc175a91480d9f8a() {
        assertEval("{ m <- matrix(c(NA,2,3,4,NA,6), nrow=2) ; rowSums(m) }");
    }

    @Test
    public void TestSimpleBuiltins_testColumnsRowsStatIgnore_97ed75fc969a4330d9764e77572c5057() {
        assertEval("{ m <- matrix(c(NA,2,3,4,NA,6), nrow=2) ; rowSums(m, na.rm = TRUE) }");
    }

    @Test
    public void TestSimpleBuiltins_testColumnsRowsStatIgnore_cf3ce4597e64537f5ed0c8e1c5bc2649() {
        assertEval("{ m <- matrix(c(NA,2,3,4,NA,6), nrow=2) ; rowMeans(m, na.rm = TRUE) }");
    }

    @Test
    public void TestSimpleBuiltins_testColumnsRowsStatIgnore_9f1ea14f9baa5e49245d4f90538c3b1d() {
        assertEval("{ m <- matrix(c(NA,2,3,4,NA,6), nrow=2) ; colSums(m) }");
    }

    @Test
    public void TestSimpleBuiltins_testColumnsRowsStatIgnore_cd2c57bffaff581a9e8b2107b3148b58() {
        assertEval("{ m <- matrix(c(NA,2,3,4,NA,6), nrow=2) ; colSums(na.rm = TRUE, m) }");
    }

    @Test
    public void TestSimpleBuiltins_testColumnsRowsStatIgnore_2b787e31b5232423c06c52f73e5df1c6() {
        assertEval("{ m <- matrix(c(NA,2,3,4,NA,6), nrow=2) ; colMeans(m) }");
    }

    @Test
    public void TestSimpleBuiltins_testColumnsRowsStatIgnore_2f427a36497e0dc01a2611f5aa23ae7b() {
        assertEval("{ m <- matrix(c(NA,2,3,4,NA,6), nrow=2) ; colMeans(m, na.rm = TRUE) }");
    }

    @Test
    public void TestSimpleBuiltins_testColumnsRowsStatIgnore_245eed182ce6e800317cc04ea2db8076() {
        assertEval("{ colMeans(matrix(as.complex(1:6), nrow=2)) }");
    }

    @Test
    public void TestSimpleBuiltins_testColumnsRowsStatIgnore_c9f4a9f49a298a830f36751055417164() {
        assertEval("{ colMeans(matrix((1:6)*(1+1i), nrow=2)) }");
    }

    @Test
    public void TestSimpleBuiltins_testColumnsRowsStatIgnore_e27a7ec7efc72290832ff500ab7fdbbd() {
        assertEval("{ rowSums(matrix(as.complex(1:6), nrow=2)) }");
    }

    @Test
    public void TestSimpleBuiltins_testColumnsRowsStatIgnore_0222b72afb3af3984c867d68ee9c340f() {
        assertEval("{ rowSums(matrix((1:6)*(1+1i), nrow=2)) }");
    }

    @Test
    public void TestSimpleBuiltins_testColumnsRowsStatIgnore_8214d8fc710c76862499f1c9b1a31121() {
        assertEval("{ rowMeans(matrix(as.complex(1:6), nrow=2)) }");
    }

    @Test
    public void TestSimpleBuiltins_testColumnsRowsStatIgnore_15d9ab20e0f0df878fa345ad14ce4245() {
        assertEval("{ rowMeans(matrix((1:6)*(1+1i), nrow=2)) }");
    }

    @Test
    public void TestSimpleBuiltins_testColumnsRowsStatIgnore_cfbfed37d84d4a557a3944e4001685a4() {
        assertEval("{ colSums(matrix(as.complex(1:6), nrow=2)) }");
    }

    @Test
    public void TestSimpleBuiltins_testColumnsRowsStatIgnore_b62078f5eeef7282e5eff2a59a8d8cd8() {
        assertEval("{ colSums(matrix((1:6)*(1+1i), nrow=2)) }");
    }

    @Test
    public void TestSimpleBuiltins_testColumnsRowsStatIgnore_ef0e18bdd086f0183fcc8fae77cc4d1a() {
        assertEval("{ o <- outer(1:3, 1:4, \"<\") ; colSums(o) }");
    }

    @Test
    public void TestSimpleBuiltins_testCombine_4679dac350852eb34d83569a96012089() {
        assertEval("{ c(\"1.2\",\"3.4\") }");
    }

    @Test
    public void TestSimpleBuiltins_testCombine_c6f7dc76ba5fa1f637eecb11723c13a0() {
        assertEval("{ c(\"a\",\"b\",\"c\") }");
    }

    @Test
    public void TestSimpleBuiltins_testCombine_e06f9273374c0577eab20a6cbd4d5c4f() {
        assertEval("{ c(\"1\",\"b\") }");
    }

    @Test
    public void TestSimpleBuiltins_testCombine_6998254e94d79011615ad531ce0b348b() {
        assertEval("{ c(\"1.00\",\"2.00\") }");
    }

    @Test
    public void TestSimpleBuiltins_testCombine_1d6f227b61dac56df87f3b175086871e() {
        assertEval("{ c(\"1.00\",\"b\") }");
    }

    @Test
    public void TestSimpleBuiltins_testCombine_15dce62c5e8802c5ede59e518fc80c9b() {
        assertEval("{ c(1.0,1L) }");
    }

    @Test
    public void TestSimpleBuiltins_testCombine_b9a129e37f54ab1e02b949c464018edc() {
        assertEval("{ c(1L,1.0) }");
    }

    @Test
    public void TestSimpleBuiltins_testCombine_70904c2def982ddc01f28731f498ac6c() {
        assertEval("{ c( 1:3 ) }");
    }

    @Test
    public void TestSimpleBuiltins_testCombine_4d6030bb18d60319e30fc9978f33d995() {
        assertEval("{ c( 1L:3L ) }");
    }

    @Test
    public void TestSimpleBuiltins_testCombine_8f1b49d9999a5674cfa43db33241eb62() {
        assertEval("{ c( 100, 1:3, 200 ) }");
    }

    @Test
    public void TestSimpleBuiltins_testCombine_44bf27c9efd32e4371deb4cf7486141a() {
        assertEval("{ c( 1:3, 7:9 ) }");
    }

    @Test
    public void TestSimpleBuiltins_testCombine_900b65685fca6e3f75d94f967ac796a8() {
        assertEval("{ c( 1:3, 5, 7:9 ) }");
    }

    @Test
    public void TestSimpleBuiltins_testCombine_2dac339219be7e5c27e52ab7afd2183a() {
        assertEval("{ c() }");
    }

    @Test
    public void TestSimpleBuiltins_testCombine_888cd37fcf41f94ec330c4c6e1437434() {
        assertEval("{ c(NULL) }");
    }

    @Test
    public void TestSimpleBuiltins_testCombine_67e4f7b760f46f95f8cf42736d903a74() {
        assertEval("{ c(NULL,NULL) }");
    }

    @Test
    public void TestSimpleBuiltins_testCombine_407a249940a31d9ee13bb6551e031542() {
        assertEval("{ c(NULL,1,2,3) }");
    }

    @Test
    public void TestSimpleBuiltins_testCombine_510bb72a4eb6a3b8ff4b6dccd90c1b9f() {
        assertEval("{ c(1+1i,2-3i,4+5i) }");
    }

    @Test
    public void TestSimpleBuiltins_testCombine_cf50589d64f2913d49d7d13e24b4d83e() {
        assertEval("{ c(\"hello\", \"hi\") }");
    }

    @Test
    public void TestSimpleBuiltins_testCombine_26f9b7984b2dcdb98f08ecb71761478c() {
        assertEval("{ c(1+1i, as.raw(10)) }");
    }

    @Test
    public void TestSimpleBuiltins_testCombine_646f38533f47d8afa39894bb7343cdb0() {
        assertEval("{ c(as.raw(10), as.raw(20)) }");
    }

    @Test
    public void TestSimpleBuiltins_testCombine_8b8344b2898a55c8f0c4319ba2af7e6b() {
        assertEval("{ c(as.raw(10),  \"test\") }");
    }

    @Test
    public void TestSimpleBuiltins_testCombine_feb230bd981f6ac6443c3beef08747ff() {
        assertEval("{ f <- function(x,y) { c(x,y) } ; f(1,1) ; f(1, TRUE) }");
    }

    @Test
    public void TestSimpleBuiltins_testCombine_3269dfb91b40928bc635cf8a9a1fdae8() {
        assertEval("{ f <- function(x,y) { c(x,y) } ; f(1,1) ; f(1, TRUE) ; f(NULL, NULL) }");
    }

    @Test
    public void TestSimpleBuiltins_testCombine_2bf3d334d5c1ccb0c45b0e70174bd811() {
        assertEval("{ x<-1:2; names(x)<-7:8; y<-3:4; names(y)<-9:10; z<-c(x, y); z }");
    }

    @Test
    public void TestSimpleBuiltins_testCombine_50be7932312ba8ddd9296f052a94a748() {
        assertEval("{ x<-1:2; names(x)<-7:8; y<-3:4; z<-c(x, y); z }");
    }

    @Test
    public void TestSimpleBuiltins_testCombine_42b96645e8ed42b9ae1143b9b2025dbb() {
        assertEval("{ x<-1:2; names(x)<-7:8;  z<-c(x, integer()); z }");
    }

    @Test
    public void TestSimpleBuiltins_testCombine_0d38307277cd787b400b4ba1d3b2b519() {
        assertEval("{ x<-1:2; names(x)<-7:8; z<-c(x, 3L); z }");
    }

    @Test
    public void TestSimpleBuiltins_testCombine_480d88aacc9bed2257b6f79d07e14e97() {
        assertEval("{ x<-1:2; names(x)<-7:8; z<-c(3L, x); z }");
    }

    @Test
    public void TestSimpleBuiltins_testCombine_24f0da921aa223cdbb332eb22fdf4bb5() {
        assertEval("{ x<-1:2; names(x)<-7:8; y<-double(0);  z<-c(x, y); z }");
    }

    @Test
    public void TestSimpleBuiltins_testCombine_ed26f444a6d549bbcc624907d89d818d() {
        assertEval("{ x<-1:2; names(x)<-7:8; z<-c(x, 3); z }");
    }

    @Test
    public void TestSimpleBuiltins_testCombine_646d29144d695579b808223b4e41f947() {
        assertEval("{ x<-1:2; names(x)<-7:8; z<-c(x, 3); attributes(z) }");
    }

    @Test
    public void TestSimpleBuiltins_testCombine_7f52aecc0a51a4da55f41ee55124511e() {
        assertEval("{ c(a=42) }");
    }

    @Test
    public void TestSimpleBuiltins_testCombine_4d602f065b01e6da5073e7d6c689f36f() {
        assertEval("{ c(a=FALSE) }");
    }

    @Test
    public void TestSimpleBuiltins_testCombine_ecb45b5a72f81ae6b0a3efc7cfe1001f() {
        assertEval("{ c(a=as.raw(7)) }");
    }

    @Test
    public void TestSimpleBuiltins_testCombine_7d7e90c63365e635ef78d240f718d9b4() {
        assertEval("{ c(a=\"foo\") }");
    }

    @Test
    public void TestSimpleBuiltins_testCombine_81baea171d91ebdf7b9960d920aed296() {
        assertEval("{ c(a=7i) }");
    }

    @Test
    public void TestSimpleBuiltins_testCombine_0873ce25c656bf498704798b5df469af() {
        assertEval("{ c(a=1, b=2) }");
    }

    @Test
    public void TestSimpleBuiltins_testCombine_50a1711b0000a973e927e0ca379adf98() {
        assertEval("{ c(a=FALSE, b=TRUE) }");
    }

    @Test
    public void TestSimpleBuiltins_testCombine_3f9bbc68eb85eacd4c941c1a5bb19450() {
        assertEval("{ c(a=as.raw(1), b=as.raw(2)) }");
    }

    @Test
    public void TestSimpleBuiltins_testCombine_dd15c8c6fba4c0de9d163da57323871a() {
        assertEval("{ c(a=\"bar\", b=\"baz\") }");
    }

    @Test
    public void TestSimpleBuiltins_testCombine_68296cef278a5edee664382c23786bf5() {
        assertEval("{ c(a=1, 2) }");
    }

    @Test
    public void TestSimpleBuiltins_testCombine_8ede4ba2623d5cac17b9b6a2b140259f() {
        assertEval("{ c(1, b=2) }");
    }

    @Test
    public void TestSimpleBuiltins_testCombine_2261873dd0aeaaabf6c80211f2f4f6c0() {
        assertEval("{ c(a=1i, b=2i) }");
    }

    @Test
    public void TestSimpleBuiltins_testCombine_cce7ad9d0ef24299d2073abf9f03872d() {
        assertEval("{ c(a=7i, a=1:2) }");
    }

    @Test
    public void TestSimpleBuiltins_testCombine_f72aa25c6812d1be9dbf46d114ebcba0() {
        assertEval("{ c(a=1:2, 42) }");
    }

    @Test
    public void TestSimpleBuiltins_testCombine_8203d2c7f110ab1f9005d249baf6e32b() {
        assertEval("{ c(a=1:2, b=c(42)) }");
    }

    @Test
    public void TestSimpleBuiltins_testCombine_dd6aa51d9b53b506e93d13ec70ef6953() {
        assertEval("{ c(a=1:2, b=double()) }");
    }

    @Test
    public void TestSimpleBuiltins_testCombine_1121e53af3f01bdef30c961ff1520873() {
        assertEval("{ c(a=c(z=1), 42) }");
    }

    @Test
    public void TestSimpleBuiltins_testCombine_814a40f41418b22f481e2bc21038ec8b() {
        assertEval("{ x<-c(z=1); names(x)=c(\"\"); c(a=x, 42) }");
    }

    @Test
    public void TestSimpleBuiltins_testCombine_1d410834d640b4e428b91dd3a3c82436() {
        assertEval("{ x<-c(y=1, z=2); names(x)=c(\"\", \"\"); c(a=x, 42) }");
    }

    @Test
    public void TestSimpleBuiltins_testCombine_0693bdd1317eff1a1f98cbec2a3cf001() {
        assertEval("{ x<-c(y=1, z=2);  c(a=x, 42) }");
    }

    @Test
    public void TestSimpleBuiltins_testCombine_40c213a64ad08362b605276223140a6d() {
        assertEval("{ x<-c(y=1);  c(x, 42) }");
    }

    @Test
    public void TestSimpleBuiltins_testCombine_57208318c28a1a11feb2626a14c85bb0() {
        assertEval("{ x<-c(1);  c(z=x, 42) }");
    }

    @Test
    public void TestSimpleBuiltins_testCombine_d54bd6f700b00abf14005fc1f709cc8e() {
        assertEval("{ x<-c(y=1, 2);  c(a=x, 42) }");
    }

    @Test
    public void TestSimpleBuiltins_testCombine_9e952ef7a4db9df7944aa490c4223d4a() {
        assertEval("{ c(TRUE,1L,1.0,list(3,4)) }");
    }

    @Test
    public void TestSimpleBuiltins_testCombine_35615a7ebae2fab48bfd016636b142d0() {
        assertEval("{ c(TRUE,1L,1.0,list(3,list(4,5))) }");
    }

    @Test
    public void TestSimpleBuiltins_testCombine_991b4e290db12070daca4ed96a3ffc63() {
        assertEval("{ c(x=1,y=2) }");
    }

    @Test
    public void TestSimpleBuiltins_testCombine_500795124539a97439309c50c3a36290() {
        assertEval("{ c(x=1,2) }");
    }

    @Test
    public void TestSimpleBuiltins_testCombine_d78532c80490f1bd045d0fbedd9b9c87() {
        assertEval("{ x <- 1:2 ; names(x) <- c(\"A\",NA) ; c(x,test=x) }");
    }

    @Test
    public void TestSimpleBuiltins_testCombine_dea889dee9bbedd803fa8a3c9437c69c() {
        assertEval("{ c(a=1,b=2:3,list(x=FALSE))  }");
    }

    @Test
    public void TestSimpleBuiltins_testCombine_3a952f6809176f161ea61dd876989666() {
        assertEval("{ c(1,z=list(1,b=22,3)) }");
    }

    @Test
    public void TestSimpleBuiltins_testCombineBroken_d365e1ffe5f8c886f6d1911c69b3af00() {
        assertEval("{ c(1i,0/0) }");
    }

    @Test
    public void TestSimpleBuiltins_testComplex_d6b94943d1cf2559663d1f79fc832a8c() {
        assertEval("{ complex(real=1,imaginary=2) }");
    }

    @Test
    public void TestSimpleBuiltins_testComplex_2fc2976e242e2e94a493842481f533d1() {
        assertEval("{ complex(real=1,imag=2) }");
    }

    @Test
    public void TestSimpleBuiltins_testComplex_08afe9365f9ccc2563e2efdda7b69a89() {
        assertEval("{ complex(3) }");
    }

    @Test
    public void TestSimpleBuiltins_testComplexIgnore_6c296b051839b1865e7b24f04e0f89d5() {
        assertEval("{ x <- 1:2 ; attr(x,\"my\") <- 2 ; Im(x) }");
    }

    @Test
    public void TestSimpleBuiltins_testComplexIgnore_ebb6e003d79ccc9419c0bbc4c4601d12() {
        assertEval("{ x <- c(1+2i,3-4i) ; attr(x,\"my\") <- 2 ; Im(x) }");
    }

    @Test
    public void TestSimpleBuiltins_testComplexIgnore_0a640363497df7e0fff841acd48b8679() {
        assertEval("{ x <- 1:2 ; attr(x,\"my\") <- 2 ; Re(x) }");
    }

    @Test
    public void TestSimpleBuiltins_testComplexIgnore_cd6019c801f0cbbb3b00ecbde91958c5() {
        assertEval("{ x <- c(1+2i,3-4i) ; attr(x,\"my\") <- 2 ; Re(x) }");
    }

    @Test
    public void TestSimpleBuiltins_testCor_2e29cd851a9773fb62e305857a008c70() {
        assertEval("{ cor(c(1,2,3),c(1,2,3)) }");
    }

    @Test
    public void TestSimpleBuiltins_testCor_20cf0f6f873bbbda686866856b6118f9() {
        assertEval("{ as.integer(cor(c(1,2,3),c(1,2,5))*10000000) }");
    }

    @Test
    public void TestSimpleBuiltins_testCor_e6e285c7bf8e5a77aaa66ad1ee28352f() {
        assertEval("{ cor(cbind(c(3,2,1), c(1,2,3))) }");
    }

    @Test
    public void TestSimpleBuiltins_testCor_eb299d07a9ea2621996c82b52b16f5e4() {
        assertEval("{ cor(cbind(c(1, 1, 1), c(1, 1, 1))) }");
    }

    @Test
    public void TestSimpleBuiltins_testCor_564c5ee2d2eea4a4b168dca5e6fa9e4f() {
        assertEval("{ cor(cbind(c(1:9,0/0), 101:110)) }");
    }

    @Test
    public void TestSimpleBuiltins_testCorIgnore_13b78c66b0e72ebed23e724262a27546() {
        assertEval("{ round( cor(cbind(c(10,5,4,1), c(2,5,10,5))), digits=5 ) }");
    }

    @Test
    public void TestSimpleBuiltins_testCov_4b96d1c7c503defdec6ebab5b659625c() {
        assertEval("{ cov(c(1,2,3),c(1,2,3)) }");
    }

    @Test
    public void TestSimpleBuiltins_testCov_6367ba5338f445d4661af52ffdf77ae9() {
        assertEval("{ cov(c(1,2,3),c(1,2,4)) }");
    }

    @Test
    public void TestSimpleBuiltins_testCov_eac366afae2ec8d25d978cf83e1549c9() {
        assertEval("{ cov(c(1,2,3),c(1,2,5)) }");
    }

    @Test
    public void TestSimpleBuiltins_testCrossprod_7f9549017d66ad3dd1583536fa7183d7() {
        assertEval("{ x <- 1:6 ; crossprod(x) }");
    }

    @Test
    public void TestSimpleBuiltins_testCrossprod_1c6fdfbd19321f1f57a6f9260789424a() {
        assertEval("{ x <- 1:2 ; crossprod(t(x)) }");
    }

    @Test
    public void TestSimpleBuiltins_testCrossprod_0ceb7477eceaa0684310f07ef6b6865c() {
        assertEval("{ crossprod(1:3, matrix(1:6, ncol=2)) }");
    }

    @Test
    public void TestSimpleBuiltins_testCrossprod_57b1bcccff6a1f41d6a0c82a658a3c52() {
        assertEval("{ crossprod(t(1:2), 5) }");
    }

    @Test
    public void TestSimpleBuiltins_testCrossprod_2770157f2b02bfda92abe04278a245f8() {
        assertEval("{ crossprod(c(1,NA,2), matrix(1:6, ncol=2)) }");
    }

    @Test
    public void TestSimpleBuiltins_testCumulativeSum_6d835b5903b7c57c797726b1610f5359() {
        assertEval("{ cumsum(1:10) }");
    }

    @Test
    public void TestSimpleBuiltins_testCumulativeSum_a28cda3a768d6f05f5d2b6c93446cf98() {
        assertEval("{ cumsum(c(1,2,3)) }");
    }

    @Test
    public void TestSimpleBuiltins_testCumulativeSum_4f9bfd33e4d3808722f87b4a09d674ea() {
        assertEval("{ cumsum(NA) }");
    }

    @Test
    public void TestSimpleBuiltins_testCumulativeSum_b9a240e4d368e0590ef25b3f2c9ca21d() {
        assertEval("{ cumsum(c(2000000000L, NA, 2000000000L)) }");
    }

    @Test
    public void TestSimpleBuiltins_testCumulativeSum_9d140cee42a87f06883cfd5d1df13b45() {
        assertEval("{ cumsum(c(TRUE,FALSE,TRUE)) }");
    }

    @Test
    public void TestSimpleBuiltins_testCumulativeSum_8e8190ab2fbc12acd11d34299ca041fb() {
        assertEval("{ cumsum(c(TRUE,FALSE,NA,TRUE)) }");
    }

    @Test
    public void TestSimpleBuiltins_testCumulativeSum_f255d43a5e7f32ec7fca1e819e91c69c() {
        assertEval("{ cumsum(c(1+1i,2-3i,4+5i)) }");
    }

    @Test
    public void TestSimpleBuiltins_testCumulativeSum_e3c4ed48fe0ed9ddd5e833febf410594() {
        assertEval("{ cumsum(c(1+1i, NA, 2+3i)) }");
    }

    @Test
    public void TestSimpleBuiltins_testCumulativeSum_840a15e453fc2128a838a946e8941be5() {
        assertEval("{ cumsum(as.logical(-2:2)) }");
    }

    @Test
    public void TestSimpleBuiltins_testCumulativeSumBroken_29fdcc5a5db08a57fa538ba6ea36df62() {
        assertEval("{ cumsum(c(1,2,3,0/0,5)) }");
    }

    @Test
    public void TestSimpleBuiltins_testCumulativeSumBroken_c4e74421afc1541ec09c1258dd016111() {
        assertEval("{ cumsum(c(1,0/0,5+1i)) }");
    }

    @Test
    public void TestSimpleBuiltins_testCumulativeSumBroken_c798b06052d4528aca37769d38a0f9af() {
        assertEval("{ cumsum(as.raw(1:6)) }");
    }

    @Test
    public void TestSimpleBuiltins_testCumulativeSumBroken_24579242149f490e91e8b1b7fc76f4e9() {
        assertEval("{ cumsum(rep(1e308, 3) ) }");
    }

    @Test
    public void TestSimpleBuiltins_testCumulativeSumBroken_9e68f6a2cfecca2814fd572d9d3dc519() {
        assertEval("{ cumsum(c(1e308, 1e308, NA, 1, 2)) }");
    }

    @Test
    public void TestSimpleBuiltins_testCumulativeSumBroken_e1af5bf2238f58b9e00ba5f815e46a59() {
        assertEval("{ cumsum(c(2000000000L, 2000000000L)) }");
    }

    @Test
    public void TestSimpleBuiltins_testCumulativeSumBroken_e41ac0de20a9dba0b5c5c897e46d2ddb() {
        assertEval("{ cumsum(c(-2147483647L, -1L)) }");
    }

    @Test
    public void TestSimpleBuiltins_testCumulativeSumBroken_598bb2dd748d2cd878a7312e7a0935c9() {
        assertEval("{ cumsum((1:6)*(1+1i)) }");
    }

    @Test
    public void TestSimpleBuiltins_testDefaultArgsIgnore_da411f3d8d8a722a471e77966e8e1135() {
        assertEval("{ length(array(dim=c(1,0,2,3))) }");
    }

    @Test
    public void TestSimpleBuiltins_testDefaultArgsIgnore_3cc1186607b6ef41bdbc0c66fc278b3a() {
        assertEval("{ dim(array(dim=c(2.1,2.9,3.1,4.7))) }");
    }

    @Test
    public void TestSimpleBuiltins_testDefaultArgsIgnore_62c7f6f4b6bf06a81284d05487afc849() {
        assertEvalError("{ array(dim=c(-2,2)); }");
    }

    @Test
    public void TestSimpleBuiltins_testDefaultArgsIgnore_6298ff4d222c7787e6c111563ac6a26a() {
        assertEvalError("{ array(dim=c(-2,-2)); }");
    }

    @Test
    public void TestSimpleBuiltins_testDelayedAssign_8ec95e38ecb3a999ffba3e7abc6ffb72() {
        assertEval("{ delayedAssign(\"x\", y); y <- 10; x }");
    }

    @Test
    public void TestSimpleBuiltins_testDelayedAssign_e828fbbef10258dab93aa4d7350c38f9() {
        assertEval("{ delayedAssign(\"x\", a+b); a <- 1 ; b <- 3 ; x }");
    }

    @Test
    public void TestSimpleBuiltins_testDelayedAssign_cedc0d1753c9e0fc71d5868f5654e3ef() {
        assertEval("{ f <- function() { delayedAssign(\"x\", y); y <- 10; x  } ; f() }");
    }

    @Test
    public void TestSimpleBuiltins_testDelayedAssign_79fb1d399e2b39a496dac5a9749fb873() {
        assertEval("{ h <- new.env(parent=emptyenv()) ; delayedAssign(\"x\", y, h, h) ; assign(\"y\", 2, h) ; get(\"x\", h) }");
    }

    @Test
    public void TestSimpleBuiltins_testDelayedAssign_af327b1b6a16f6b664839a659452d6ff() {
        assertEval("{ h <- new.env(parent=emptyenv()) ; assign(\"x\", 1, h) ; delayedAssign(\"x\", y, h, h) ; assign(\"y\", 2, h) ; get(\"x\", h) }");
    }

    @Test
    public void TestSimpleBuiltins_testDelayedAssign_b0a8cc01cf8e5fc94f5e4084097107ad() {
        assertEval("{ f <- function(...) { delayedAssign(\"x\", ..1) ; y <<- x } ; f(10) ; y }");
    }

    @Test
    public void TestSimpleBuiltins_testDelayedAssign_2650fc25df477fca9f65b4ae42030ddc() {
        assertEval("{ f <- function() { delayedAssign(\"x\", 3); delayedAssign(\"x\", 2); x } ; f() }");
    }

    @Test
    public void TestSimpleBuiltins_testDelayedAssign_8c59e6c2915b2b15a962ae541292c0db() {
        assertEval("{ f <- function() { x <- 4 ; delayedAssign(\"x\", y); y <- 10; x  } ; f() }");
    }

    @Test
    public void TestSimpleBuiltins_testDelayedAssign_83064c7d347757ad66074441e8cfc90e() {
        assertEvalError("{ f <- function() { delayedAssign(\"x\", y); delayedAssign(\"y\", x) ; x } ; f() }");
    }

    @Test
    public void TestSimpleBuiltins_testDeparse_1dc435ef27d6d10df26ec2271cb67316() {
        assertEval("{ f <- function(x) { deparse(substitute(x)) } ; f(a + b * (c - d)) }");
    }

    @Test
    public void TestSimpleBuiltins_testDet_0119e3eeb33ab4a029ba7826ddc06536() {
        assertEval("{ det(matrix(c(1,2,4,5),nrow=2)) }");
    }

    @Test
    public void TestSimpleBuiltins_testDet_5e1459250de6d93f03e5e5eaaccd1afc() {
        assertEval("{ det(matrix(c(1,-3,4,-5),nrow=2)) }");
    }

    @Test
    public void TestSimpleBuiltins_testDet_9c562345cefeea163f138973f9d0f2a1() {
        assertEval("{ det(matrix(c(1,0,4,NA),nrow=2)) }");
    }

    @Test
    public void TestSimpleBuiltins_testDiagnostics_f20f62c82be750e78cc720a71705d1f4() {
        assertEvalError("{ f <- function() { stop(\"hello\",\"world\") } ; f() }");
    }

    @Test
    public void TestSimpleBuiltins_testDiagonal_6f217e9d92c383bc03ed5ed5cf2dcde8() {
        assertEval("{ m <- matrix(1:6, nrow=3) ; diag(m) }");
    }

    @Test
    public void TestSimpleBuiltins_testDiagonal_fed2e28fa5024509b954be36e263feca() {
        assertEval("{ m <- matrix(1:6, nrow=2) ; diag(m) }");
    }

    @Test
    public void TestSimpleBuiltins_testDiagonal_c5061263e37b75e26943e61e7efe44dd() {
        assertEval("{ m <- matrix(1:9, nrow=3) ; diag(m) }");
    }

    @Test
    public void TestSimpleBuiltins_testDimensions_0c23ecb60a78eddaab4dced71193f975() {
        assertEval("{ dim(1) }");
    }

    @Test
    public void TestSimpleBuiltins_testDimensions_2247aafe5a53d13807d99953aa2e8775() {
        assertEval("{ dim(1:3) }");
    }

    @Test
    public void TestSimpleBuiltins_testDimensions_a496d196f4be0a6b7f284e7431d43aa3() {
        assertEval("{ m <- matrix(1:6, nrow=3) ; dim(m) }");
    }

    @Test
    public void TestSimpleBuiltins_testDimensions_4aaa4f8353c6353b27748bad1e08f52c() {
        assertEval("{ nrow(1) }");
    }

    @Test
    public void TestSimpleBuiltins_testDimensions_76592ac69f51d70b7e6ad799518ac1ac() {
        assertEval("{ nrow(1:3) }");
    }

    @Test
    public void TestSimpleBuiltins_testDimensions_356aaad7eae5dcb2221bf5a5c23af694() {
        assertEval("{ NROW(1) }");
    }

    @Test
    public void TestSimpleBuiltins_testDimensions_87fea101f7b501c07c8067bab685c4f9() {
        assertEval("{ NROW(1:3) }");
    }

    @Test
    public void TestSimpleBuiltins_testDimensions_c918fa702b932ee7113b693471e5979b() {
        assertEval("{ ncol(1) }");
    }

    @Test
    public void TestSimpleBuiltins_testDimensions_fddbb4f43160793483577ef23b57631f() {
        assertEval("{ ncol(1:3) }");
    }

    @Test
    public void TestSimpleBuiltins_testDimensions_11a0cafbcadded1a1ca2d5acad69309f() {
        assertEval("{ NCOL(1) }");
    }

    @Test
    public void TestSimpleBuiltins_testDimensions_205ea4a4a5c3d1a4ffee3c0b76478d4e() {
        assertEval("{ NCOL(1:3) }");
    }

    @Test
    public void TestSimpleBuiltins_testDimensions_6bd405e828cf565728b9c5740e42aad2() {
        assertEval("{ m <- matrix(1:6, nrow=3) ; nrow(m) }");
    }

    @Test
    public void TestSimpleBuiltins_testDimensions_2c126276c7789a1db820e62a53ef26e8() {
        assertEval("{ m <- matrix(1:6, nrow=3) ; ncol(m) }");
    }

    @Test
    public void TestSimpleBuiltins_testDimensions_35f4a03ea02d8d59ff26883dc78874d1() {
        assertEval("{ z <- 1 ; dim(z) <- c(1,1) ; dim(z) <- NULL ; z }");
    }

    @Test
    public void TestSimpleBuiltins_testDimensions_00ec03bd9099b945efb83d5b6cbedbf9() {
        assertEval("{ x <- 1:4 ; f <- function() { x <- 1:4 ; dim(x) <<- c(2,2) } ; f() ; dim(x) }");
    }

    @Test
    public void TestSimpleBuiltins_testDimensions_b402fcc65635b6378b385f8ef0355475() {
        assertEval("{ x<-1:12; dim(x)<-c(12); x }");
    }

    @Test
    public void TestSimpleBuiltins_testDimensions_823a9607b577b042cb434a1431fbd9dd() {
        assertEval("{ x<-1:12; dim(x)<-c(as.raw(12)); x }");
    }

    @Test
    public void TestSimpleBuiltins_testDimensions_6858c29517ef2e49d616e86364b6b1f9() {
        assertEval("{ x<-1:12; dim(x)<-c(\"12\"); x }");
    }

    @Test
    public void TestSimpleBuiltins_testDimensions_a1d80c23ad741e9b5f9d99b3ff58910c() {
        assertEval("{ x<-1:1; dim(x)<-c(TRUE); x }");
    }

    @Test
    public void TestSimpleBuiltins_testDimensions_e773d32308c488c1c74a3c8b8d190c38() {
        assertEval("{ x<-1:12; dim(x)<-c(3, 4); attr(x, \"dim\") }");
    }

    @Test
    public void TestSimpleBuiltins_testDimensions_13776c8dbe738895cbc639890f5d29a5() {
        assertEval("{ x<-1:12; attr(x, \"dim\")<-c(3, 4); dim(x) }");
    }

    @Test
    public void TestSimpleBuiltins_testDimensions_aa9f516580fb8ea481089ee726918a90() {
        assertEval("{ x<-1:4; names(x)<-c(21:24); attr(x, \"foo\")<-\"foo\"; x }");
    }

    @Test
    public void TestSimpleBuiltins_testDimensions_8d7416728846ceb5d0838a27cafcabbd() {
        assertEval("{ x<-list(1,2,3); names(x)<-c(21:23); attr(x, \"foo\")<-\"foo\"; x }");
    }

    @Test
    public void TestSimpleBuiltins_testDimensions_8a0d7f4ac80c83acbb4aed468e5c2685() {
        assertEval("{ b <- c(a=1+2i,b=3+4i) ; attr(b,\"my\") <- 211 ; dim(b) <- c(2,1) ; names(b) }");
    }

    @Test
    public void TestSimpleBuiltins_testDimensions_fdbe4986bac1158671c6f3aacc8d5272() {
        assertEval("{ x<-1:4; dim(x)<-c(4); y<-101:104; dim(y)<-c(4); x > y }");
    }

    @Test
    public void TestSimpleBuiltins_testDimensions_1e40426982ed3cd602392fea84959d07() {
        assertEval("{ x<-1:4; y<-101:104; dim(y)<-c(4); x > y }");
    }

    @Test
    public void TestSimpleBuiltins_testDimensions_37cc6487daa17db41d5379a95c7a0931() {
        assertEval("{ x<-1:4; dim(x)<-c(4); y<-101:104; x > y }");
    }

    @Test
    public void TestSimpleBuiltins_testDimensions_71d211060a30670c2bd2533d384a5b66() {
        assertEval("{ x<-c(1); dim(x)<-1; names(x)<-c(\"b\"); attributes(x) }");
    }

    @Test
    public void TestSimpleBuiltins_testDimensions_b4450bc51a534b462fb59d9b4be773f1() {
        assertEval("{ x<-c(1); dim(x)<-1; attr(x, \"dimnames\")<-list(\"b\"); attributes(x) }");
    }

    @Test
    public void TestSimpleBuiltins_testDimensions_783e8a89bd791ef131594b47abb113b5() {
        assertEval("{ x<-c(1); dim(x)<-1; attr(x, \"dimnames\")<-list(a=\"b\"); attributes(x) }");
    }

    @Test
    public void TestSimpleBuiltins_testDimensions_6593313ac00fa05af5824a01ca99a11a() {
        assertEval("{ x<-c(42); names(x)<-\"a\"; attr(x, \"dim\")<-1; names(x)<-\"z\"; dim(x)<-NULL; attributes(x) }");
    }

    @Test
    public void TestSimpleBuiltins_testDimensions_f8f916213524b6dd75b22b6ec0cd713e() {
        assertEval("{ x<-c(42); names(x)<-\"a\"; attr(x, \"dim\")<-1; names(x)<-\"z\"; attr(x, \"foo\")<-\"foo\"; attr(x, \"dim\")<-NULL; attributes(x) }");
    }

    @Test
    public void TestSimpleBuiltins_testDimensions_43dce999c62da429818bb97ea3ef1042() {
        assertEval("{ x<-c(42); names(x)<-\"a\"; attr(x, \"dim\")<-1; names(x)<-\"z\"; attributes(x) }");
    }

    @Test
    public void TestSimpleBuiltins_testDimensions_a2c91fc7f072d37823a7a9d36c366ee1() {
        assertEval("{ x<-c(42); names(x)<-\"a\"; attr(x, \"dim\")<-1; names(x)<-\"z\"; names(x)<-NULL; attributes(x) }");
    }

    @Test
    public void TestSimpleBuiltins_testDimensions_715d2210067ec1dad08eaaf5c76bfa9d() {
        assertEval("{ x<-c(42); names(x)<-\"a\"; attr(x, \"dim\")<-1; names(x)<-\"z\"; names(x)<-NULL; attr(x, \"dimnames\")<-NULL; attributes(x) }");
    }

    @Test
    public void TestSimpleBuiltins_testDimensions_8f2737bd9fb2a97748df329f43ee564b() {
        assertEval("{ x<-c(1); y<-c(1); dim(x)<-1; dim(y)<-1; attr(x, \"dimnames\")<-(attr(y, \"dimnames\")<-list(\"b\")); attributes(x) }");
    }

    @Test
    public void TestSimpleBuiltins_testDimensions_60ce105fa17b875756e65d14e8dff5c3() {
        assertEval("{ x<-1:4; dim(x)<-c(2,1,2); dimnames(x)<-list(NULL); attributes(x) }");
    }

    @Test
    public void TestSimpleBuiltins_testDimensions_a030ab7282df112d4d7c259076dd0aa6() {
        assertEval("{ x<-1:4; dim(x)<-c(2,1,2); dimnames(x)<-list(c(\"a\", \"b\"), \"c\", c(\"d\", \"e\")); attributes(x) }");
    }

    @Test
    public void TestSimpleBuiltins_testDimensions_87dd54d946d76d249f6f1254077b043c() {
        assertEval("{ x<-1:4; dim(x)<-c(2,1,2); dimnames(x)<-list(c(\"a\", \"b\"), 42, c(\"d\", \"e\")); attributes(x) }");
    }

    @Test
    public void TestSimpleBuiltins_testDimensions_fd5ee9595105a81c94947c2282870d1d() {
        assertEval("{ x<-42; y<-(dim(x)<-1); }");
    }

    @Test
    public void TestSimpleBuiltins_testDimensions_8a4717c07b4ba285a522832a1e003583() {
        assertEval("{ x<-42; y<-(dim(x)<-1); y }");
    }

    @Test
    public void TestSimpleBuiltins_testDimensions_2da0a517db75943da30c4605535fbb2d() {
        assertEval("{ x<-1:4; y<-c(2, 2); dim(x)<-y; y[1]=4; dim(x) }");
    }

    @Test
    public void TestSimpleBuiltins_testDimensions_9052cdec9a7a273338567d2da1661f63() {
        assertEval("{ x<-1; dim(x)=1; attr(x, \"foo\")<-\"foo\"; dim(x)<-NULL; attributes(x) }");
    }

    @Test
    public void TestSimpleBuiltins_testDimensions_854ca3d1bf7f4671087c9d572dbc362b() {
        assertEval("{ x<-1; dim(x)=1; attr(x, \"names\")<-\"a\"; dim(x)<-NULL; attributes(x) }");
    }

    @Test
    public void TestSimpleBuiltins_testDimensions_cd3f901cd902d2b36347911410ff1c10() {
        assertEval("{ x<-1; dim(x)=1; names(x)<-\"a\"; dim(x)<-NULL; attributes(x) }");
    }

    @Test
    public void TestSimpleBuiltins_testDimensions_ee8b09cfabdf83f3172abf96b044cf56() {
        assertEval("{ x<-1:2; dim(x)=c(1,2); names(x)<-c(\"a\", \"b\"); attr(x, \"foo\")<-\"foo\"; dim(x)<-NULL; attributes(x) }");
    }

    @Test
    public void TestSimpleBuiltins_testDimensions_441810163015f31111b53993eeb1438d() {
        assertEval("{ x<-1:4; names(x)<-c(21:24); attr(x, \"dim\")<-c(4); attr(x, \"foo\")<-\"foo\"; x }");
    }

    @Test
    public void TestSimpleBuiltins_testDimensions_199f5567ac0eef76710b54785dc65cc0() {
        assertEval("{ x<-list(1,2,3); names(x)<-c(21:23); attr(x, \"dim\")<-c(3); attr(x, \"foo\")<-\"foo\"; x }");
    }

    @Test
    public void TestSimpleBuiltins_testDimensions_23b7e792972c66f8f798088f2e2db517() {
        assertEval("{ x<-1; dimnames(x) }");
    }

    @Test
    public void TestSimpleBuiltins_testDimensions_25559c72545218e5904f029d31043341() {
        assertEval("{ dimnames(1) }");
    }

    @Test
    public void TestSimpleBuiltins_testDimensions_94c1f6362cb75ccb566992ddbb86b57f() {
        assertEval("{ dimnames(NULL) }");
    }

    @Test
    public void TestSimpleBuiltins_testDimensions_fa944f6fbbd53435e7ee6de00c6e156d() {
        assertEval("{ x<-1; dim(x)<-1; dimnames(x)<-list() }");
    }

    @Test
    public void TestSimpleBuiltins_testDimensions_2979e5a89355ac34e701f6b1d6910468() {
        assertEval("{ x<-1; dim(x)<-1; dimnames(x)<-list(0) }");
    }

    @Test
    public void TestSimpleBuiltins_testDimensions_2dc914e1fb3582f18a062c4d58f30902() {
        assertEval("{ x<-1; dim(x)<-1; dimnames(x)<-list(\"a\"); dimnames(x); dimnames(x)<-list(); dimnames(x) }");
    }

    @Test
    public void TestSimpleBuiltins_testDimensions_e3fb5c3d5b9cce5f04b1cd8a5ce350e3() {
        assertEval("{ x <- 1:2 ; dim(x) <- c(1,2) ; x }");
    }

    @Test
    public void TestSimpleBuiltins_testDimensions_67530941177a750a2f5c26ba31f07c5e() {
        assertEval("{ x <- 1:2 ; attr(x, \"dim\") <- c(2,1) ; x }");
    }

    @Test
    public void TestSimpleBuiltins_testDimensions_999f000321095e06ce4af15a0c503839() {
        assertEvalError("{ x <- 1:2 ; dim(x) <- c(1, 3) ; x }");
    }

    @Test
    public void TestSimpleBuiltins_testDimensions_ee6c362ec748a941bc4907be29b94a57() {
        assertEvalError("{ x <- 1:2 ; dim(x) <- c(1, NA) ; x }");
    }

    @Test
    public void TestSimpleBuiltins_testDimensions_7df10933f49ff5657ce8bb2409bf1b4f() {
        assertEvalError("{ x <- 1:2 ; dim(x) <- c(1, -1) ; x }");
    }

    @Test
    public void TestSimpleBuiltins_testDimensions_05b0bd2fb44532eb640aff25aa284124() {
        assertEvalError("{ x <- 1:2 ; dim(x) <- integer() ; x }");
    }

    @Test
    public void TestSimpleBuiltins_testDimensions_bb1b63d58ea745c7867a83c91240dafc() {
        assertEvalError("{ x<-1:4; dim(x)<-c(4); y<-101:104; dim(y)<-c(2,2); x > y }");
    }

    @Test
    public void TestSimpleBuiltins_testDimensions_998e865c021b666c232e101ec968b9b2() {
        assertEvalError("{ x<-1:4; dim(x)<-c(4); y<-101:108; dim(y)<-c(8); x > y }");
    }

    @Test
    public void TestSimpleBuiltins_testDimensions_6d0ffbecf470d274edf5b4f670120299() {
        assertEvalError("{ x<-1:4; attr(x, \"dimnames\") <- list(101, 102, 103, 104) }");
    }

    @Test
    public void TestSimpleBuiltins_testDimensions_c9bedc4b2a14651bb6845c1a86eab49b() {
        assertEvalError("{ x<-1:4; dim(x)<-c(2,1,2); dimnames(x) <- list(c(\"a\")); x }");
    }

    @Test
    public void TestSimpleBuiltins_testDimensions_77c69bd2baa9a218de392858593f66c3() {
        assertEvalError("{ x<-1:4; dim(x)<-c(2,1,2); dimnames(x) <- list(c(\"a\", \"b\"), NULL, c(\"d\")); x }");
    }

    @Test
    public void TestSimpleBuiltins_testDimensions_3a25606029dde41e4de9340e8fbcd6d4() {
        assertEvalError("{ x<-1:4; dim(x)<-c(2,1,2); dimnames(x) <- list(c(\"a\", \"b\"), 42, c(\"d\", \"e\", \"f\")); attributes(x) }");
    }

    @Test
    public void TestSimpleBuiltins_testDimensions_3861861bd8a1425a307ed3da5dc375c4() {
        assertEvalError("{ x<-1:4; dim(x)<-c(2,1,2); dimnames(x) <- list(c(\"a\", \"b\"), \"c\", c(\"d\", \"e\"), 7); attributes(x) }");
    }

    @Test
    public void TestSimpleBuiltins_testDimensions_f8dd040ae16a25bb3f4113a243fb3869() {
        assertEvalError("{ x<-1; dim(x)<-1; dimnames(x) <- 1; dimnames(x) }");
    }

    @Test
    public void TestSimpleBuiltins_testDimensions_f00b649a080774da3e77fe66eb366f25() {
        assertEvalError("{ x<-1; dim(x)<-1; attr(x, \"dimnames\") <- 1 }");
    }

    @Test
    public void TestSimpleBuiltins_testDimensions_b5baee88565f3b36d5026bad6cc5e60a() {
        assertEvalWarning("{ x<-1:12; dim(x)<-c(12+10i); x }");
    }

    @Test
    public void TestSimpleBuiltins_testEigen_5f76ba83937083ccca6e7d8fca5c8d43() {
        assertEval("{ r <- eigen(matrix(rep(1,4), nrow=2), only.values=FALSE) ; round( r$vectors, digits=5 ) }");
    }

    @Test
    public void TestSimpleBuiltins_testEigen_3a0973319dd9e19b5d218165db6c191e() {
        assertEval("{ r <- eigen(matrix(rep(1,4), nrow=2), only.values=FALSE) ; round( r$values, digits=5 ) }");
    }

    @Test
    public void TestSimpleBuiltins_testEigen_115e6b72c47df2b5d5700b273b70c533() {
        assertEval("{ eigen(10, only.values=FALSE) }");
    }

    @Test
    public void TestSimpleBuiltins_testEigen_0449327e2827cfc14c352f69bb2d6863() {
        assertEval("{ r <- eigen(matrix(c(1,2,2,3), nrow=2), only.values=FALSE); round( r$vectors, digits=5 ) }");
    }

    @Test
    public void TestSimpleBuiltins_testEigen_1807792fd12f35acb23589be46cf6b57() {
        assertEval("{ r <- eigen(matrix(c(1,2,2,3), nrow=2), only.values=FALSE); round( r$values, digits=5 ) }");
    }

    @Test
    public void TestSimpleBuiltins_testEigen_a7d1b10ab33353c276caf5c71013af50() {
        assertEval("{ r <- eigen(matrix(c(1,2,3,4), nrow=2), only.values=FALSE); round( r$vectors, digits=5 ) }");
    }

    @Test
    public void TestSimpleBuiltins_testEigen_83d97801023043df2de8fa2831ea80e5() {
        assertEval("{ r <- eigen(matrix(c(1,2,3,4), nrow=2), only.values=FALSE); round( r$values, digits=5 ) }");
    }

    @Test
    public void TestSimpleBuiltins_testEigen_e1ef8addd5b3fea26321432b42bf54e5() {
        assertEval("{ r <- eigen(matrix(c(3,-2,4,-1), nrow=2), only.values=FALSE); round( r$vectors, digits=5 ) }");
    }

    @Test
    public void TestSimpleBuiltins_testEigen_19ec900b70611f935fb95e980df000f3() {
        assertEval("{ r <- eigen(matrix(c(3,-2,4,-1), nrow=2), only.values=FALSE); round( r$values, digits=5 ) }");
    }

    @Test
    public void TestSimpleBuiltins_testEnvironment_1b8319a6952fc86b59ba9a33e6463213() {
        assertEval("{ h <- new.env() ; assign(\"abc\", \"yes\", h) ; exists(c(\"abc\", \"def\"), h) }");
    }

    @Test
    public void TestSimpleBuiltins_testEnvironment_400ebe1d63776a8123657672755b7146() {
        assertEval("{ h <- new.env() ; assign(\"abc\", \"yes\", h) ; exists(c(\"def\", \"abc\"), h) }");
    }

    @Test
    public void TestSimpleBuiltins_testEnvironment_7deda451aa6989868e186dfcaf61f6d4() {
        assertEval("{ h <- new.env() ; assign(c(\"a\"), 1, h) ; ls(h) }");
    }

    @Test
    public void TestSimpleBuiltins_testEnvironment_e122e6c7bd35b1065ede73e6288fe1cb() {
        assertEval("{ h <- new.env() ; assign(c(\"a\"), 1L, h) ; ls(h) }");
    }

    @Test
    public void TestSimpleBuiltins_testEnvironment_951aba917a8cc5eefa372ff08169c9a9() {
        assertEval("{ h <- new.env(parent=emptyenv()) ; assign(\"x\", 1, h) ; assign(\"y\", 2, h) ; ls(h) }");
    }

    @Test
    public void TestSimpleBuiltins_testEnvironment_b58f915b8ffcad4f39b069e781f79c8b() {
        assertEval("{ hh <- new.env() ; assign(\"z\", 3, hh) ; h <- new.env(parent=hh) ; assign(\"y\", 2, h) ; get(\"z\", h) }");
    }

    @Test
    public void TestSimpleBuiltins_testEnvironment_dc31d1344caca67f4916babcf9dc55de() {
        assertEval("{ e<-new.env() ; ls(e) }");
    }

    @Test
    public void TestSimpleBuiltins_testEnvironment_14e179c30aa8e043ec4eb7e3e6e62da8() {
        assertEval("{ e<-new.env() ; assign(\"x\",1,e) ; ls(e) }");
    }

    @Test
    public void TestSimpleBuiltins_testEnvironment_0552b3dcd8bebf3e5c9d397b809c6625() {
        assertEval("{ e<-new.env() ; assign(\"x\",1,e) ; get(\"x\",e) }");
    }

    @Test
    public void TestSimpleBuiltins_testEnvironment_b6cf99b9c601bf62f2aaede170a5610c() {
        assertEval("{ h <- new.env() ; assign(\"x\", 1, h) ; assign(\"x\", 1, h) ; get(\"x\", h) }");
    }

    @Test
    public void TestSimpleBuiltins_testEnvironment_83f71814b8fed725f660655faa735a9e() {
        assertEval("{ h <- new.env() ; assign(\"x\", 1, h) ; assign(\"x\", 2, h) ; get(\"x\", h) }");
    }

    @Test
    public void TestSimpleBuiltins_testEnvironment_03cb7bc0b0b620df9cedd9aa88c078f3() {
        assertEval("{ h <- new.env() ; u <- 1 ; assign(\"x\", u, h) ; assign(\"x\", u, h) ; get(\"x\", h) }");
    }

    @Test
    public void TestSimpleBuiltins_testEnvironment_cffc3cfc363df31801bc5b6fe5cb0f50() {
        assertEval("{ h <- new.env(parent=emptyenv()) ; assign(\"x\", 1, h) ; exists(\"x\", h) }");
    }

    @Test
    public void TestSimpleBuiltins_testEnvironment_3cc61e453ab189eef7b808e7bc4e4fac() {
        assertEval("{ h <- new.env(parent=emptyenv()) ; assign(\"x\", 1, h) ; exists(\"xx\", h) }");
    }

    @Test
    public void TestSimpleBuiltins_testEnvironment_836f4d03f7c80c2a96ae712476c28062() {
        assertEval("{ hh <- new.env() ; assign(\"z\", 3, hh) ; h <- new.env(parent=hh) ; assign(\"y\", 2, h) ; exists(\"z\", h) }");
    }

    @Test
    public void TestSimpleBuiltins_testEnvironment_bf653a1abe8a78d652f33fa094b8ded8() {
        assertEval("{ ph <- new.env() ; h <- new.env(parent=ph) ; assign(\"x\", 2, ph) ; assign(\"x\", 10, h, inherits=TRUE) ; get(\"x\", ph)}");
    }

    @Test
    public void TestSimpleBuiltins_testEnvironment_9e2a6f71d6db27159d2decade3c9ba3d() {
        assertEval("{ globalenv() }");
    }

    @Test
    public void TestSimpleBuiltins_testEnvironment_f3ba3e5d35d964724cc7d40b83ce40d8() {
        assertEval("{ emptyenv() }");
    }

    @Test
    public void TestSimpleBuiltins_testEnvironment_4209dd6f0954b5bf18cdf4e5ef11ce2b() {
        assertEval("{ baseenv() }");
    }

    @Test
    public void TestSimpleBuiltins_testEnvironment_2ade89a2966be013ed043bfe3f3da7e7() {
        assertEval("{ ls() }");
    }

    @Test
    public void TestSimpleBuiltins_testEnvironment_158d5d1e9a2d6c61d8dac661de0e1a5e() {
        assertEval("{ f <- function(x, y) { ls() }; f(1, 2) }");
    }

    @Test
    public void TestSimpleBuiltins_testEnvironment_e3f6ba9f37472794e38c7ad678f810e3() {
        assertEval("{ x <- 1; ls(globalenv()) }");
    }

    @Test
    public void TestSimpleBuiltins_testEnvironment_35855ecba908e6027f9dc0a2580910bf() {
        assertEval("{ x <- 1; .y <- 2; ls(globalenv()) }");
    }

    @Test
    public void TestSimpleBuiltins_testEnvironment_4adda712c70fefdfd0544421b1063c36() {
        assertEval("{ is.environment(globalenv()) }");
    }

    @Test
    public void TestSimpleBuiltins_testEnvironment_ee00382b21f4a44fba3b55a71a042763() {
        assertEval("{ is.environment(1) }");
    }

    @Test
    public void TestSimpleBuiltins_testEnvironment_8a26b290c7ede675e52cdbe4ac3b88b5() {
        assertEval("{ f <- function()  { as.environment(-1) } ; f() }");
    }

    @Test
    public void TestSimpleBuiltins_testEnvironment_2c96b7a4989b55bba289512b17fa40b4() {
        assertEval("{ as.environment(1) }");
    }

    @Test
    public void TestSimpleBuiltins_testEnvironment_9ef27d8439d61318b0db2162cfc73474() {
        assertEval("{ as.environment(length(search())) }");
    }

    @Test
    public void TestSimpleBuiltins_testEnvironment_fed6dc80140fa4f060c531f74dbbd8a7() {
        assertEval("{ as.environment(length(search()) + 1) }");
    }

    @Test
    public void TestSimpleBuiltins_testEnvironment_d5339cb20f7ff6e01af46dfe67eb60a7() {
        assertEval("{ as.environment(\".GlobalEnv\") }");
    }

    @Test
    public void TestSimpleBuiltins_testEnvironment_285eb0ed975eee2bda3e2fb19417637b() {
        assertEval("{ as.environment(\"package:base\") }");
    }

    @Test
    public void TestSimpleBuiltins_testEnvironment_557a9faf503e7318f90b201e74850df7() {
        assertEval("{ identical(parent.env(baseenv()), emptyenv()) }");
    }

    @Test
    public void TestSimpleBuiltins_testEnvironment_39dce27a0b9a7269596592d1d23c6b20() {
        assertEval("{ e <- new.env(); `parent.env<-`(e, emptyenv()); identical(parent.env(e), emptyenv()) }");
    }

    @Test
    public void TestSimpleBuiltins_testEnvironment_5ae3e8f410ab5a9c4d8d6b31651a8bd2() {
        assertEval("{ environment() }");
    }

    @Test
    public void TestSimpleBuiltins_testEnvironment_dbf0f3e90a92de00cbc4faef09918256() {
        assertEval("{ environment(environment) }");
    }

    @Test
    public void TestSimpleBuiltins_testEnvironment_7394d15ed01a61085d62175d9e69de66() {
        assertEval("{ environmentName(baseenv()) }");
    }

    @Test
    public void TestSimpleBuiltins_testEnvironment_943f70856f148035b3bfbdbdff0c2222() {
        assertEval("{ environmentName(globalenv()) }");
    }

    @Test
    public void TestSimpleBuiltins_testEnvironment_c7d3f6936e31e8863fb6663435e1593b() {
        assertEval("{ environmentName(emptyenv()) }");
    }

    @Test
    public void TestSimpleBuiltins_testEnvironment_4c7392e14b704a6b75055d48d5d5dc5f() {
        assertEval("{ environmentName(1) }");
    }

    @Test
    public void TestSimpleBuiltins_testEnvironment_3602079d5fdc38d030056e67704d6461() {
        assertEval("{ e<-new.env(); environmentIsLocked(e) }");
    }

    @Test
    public void TestSimpleBuiltins_testEnvironment_4b733b3c8920f029f34b48ebacda5606() {
        assertEval("{ e<-new.env(); lockEnvironment(e); environmentIsLocked(e) }");
    }

    @Test
    public void TestSimpleBuiltins_testEnvironment_f3087d3aadebff13ac0e65ae3b77ccec() {
        assertEval("{ e<-new.env(); assign(\"a\", 1, e) ; lockEnvironment(e); assign(\"a\", 2, e) }");
    }

    @Test
    public void TestSimpleBuiltins_testEnvironment_804a9447db172c80fb207ca48b0de263() {
        assertEval("{ e<-new.env(); assign(\"a\", 1, e) ; lockEnvironment(e, TRUE); unlockBinding(\"a\", e); assign(\"a\", 2, e) }");
    }

    @Test
    public void TestSimpleBuiltins_testEnvironment_7e0380fcca98529bec6f3984c0f5f0d1() {
        assertEval("{ e<-new.env(); assign(\"a\", 1, e); bindingIsLocked(\"a\", e) }");
    }

    @Test
    public void TestSimpleBuiltins_testEnvironment_5f8448cc1a1e6ab105de27796ae2269b() {
        assertEval("{ e<-new.env(); assign(\"a\", 1, e); lockBinding(\"a\", e); bindingIsLocked(\"a\", e) }");
    }

    @Test
    public void TestSimpleBuiltins_testEnvironment_55690b9e7007c6c718afe28a4356ee75() {
        assertEval("{ e<-new.env(); assign(\"a\", 1, e) ; lockBinding(\"a\", e); rm(\"a\",envir = e); ls() }");
    }

    @Test
    public void TestSimpleBuiltins_testEnvironment_ecad558961e82bd73a386034c871ed68() {
        assertEval("{ e<-new.env(); assign(\"a\", 1, e) ; rm(\"a\",envir = e); ls() }");
    }

    @Test
    public void TestSimpleBuiltins_testEnvironment_ca04651afc834382c8dd7df7d55d8862() {
        assertEval("{ e<-new.env(); x<-1; get(\"x\", e) }");
    }

    @Test
    public void TestSimpleBuiltins_testEnvironment_1137907b5efe300e13b83d852b13db42() {
        assertEval("{ e<-new.env(); assign(\"x\", 1, e); get(\"x\", e) }");
    }

    @Test
    public void TestSimpleBuiltins_testEnvironment_9da65acccab58ab951f83fcc62eb2a9d() {
        assertEvalAlt("{ h <- new.env(parent=emptyenv()) ; assign(\"y\", 1, h) ; assign(\"x\", 2, h) ; ls(h) }");
    }

    @Test
    public void TestSimpleBuiltins_testEnvironment_aa6736b0cb4026ec2d0e5ccfe392d192() {
        assertEvalError("{ as.environment(-1) }");
    }

    @Test
    public void TestSimpleBuiltins_testEnvironment_7f18e387cfa483bb9e5d1ccf3f036cfc() {
        assertEvalError("{ as.environment(0) }");
    }

    @Test
    public void TestSimpleBuiltins_testEnvironment_6659f77b69ee9ac64f532e27438f6a00() {
        assertEvalError("{ as.environment(length(search()) + 2) }");
    }

    @Test
    public void TestSimpleBuiltins_testEnvironment_14d25c1c38347070f388d2f433245dab() {
        assertEvalError("{ as.environment(as.environment) }");
    }

    @Test
    public void TestSimpleBuiltins_testEnvironment_ce252c65c0262f6fbb8778c80f154db0() {
        assertEvalError("{ e<-new.env(); lockEnvironment(e); assign(\"a\", 1, e) }");
    }

    @Test
    public void TestSimpleBuiltins_testEnvironment_c70e2f98803cf9712a16f3c05963a400() {
        assertEvalError("{ e<-new.env(); assign(\"a\", 1, e) ; lockEnvironment(e, TRUE); assign(\"a\", 2, e) }");
    }

    @Test
    public void TestSimpleBuiltins_testEnvironment_9710d6ff5c3e123234468d4f0455fc7b() {
        assertEvalError("{ e<-new.env(); assign(\"a\", 1, e); lockBinding(\"a\", e); assign(\"a\", 2, e) }");
    }

    @Test
    public void TestSimpleBuiltins_testEnvironment_a066900fcbfdda8bef1990a02d8d21e7() {
        assertEvalError("{ rm(\"foo\", envir = baseenv()) }");
    }

    @Test
    public void TestSimpleBuiltins_testEnvironment_96f7f8ea59f79d0925519d113eed4fee() {
        assertEvalError("{ e<-new.env(); assign(\"a\", 1, e) ; lockEnvironment(e); rm(\"a\",envir = e); }");
    }

    @Test
    public void TestSimpleBuiltins_testEnvironment_7da93587d25bdae28c3a9f5eb37c27a5() {
        assertEvalError("{ e<-new.env(); get(\"x\", e) }");
    }

    @Test
    public void TestSimpleBuiltins_testEnvironment_fc01d2ee012ac0a5e709be2214a69320() {
        assertEvalError("{ e<-new.env(); x<-1; get(\"x\", e, inherits=FALSE) }");
    }

    @Test
    public void TestSimpleBuiltins_testEnvironment_ecd1935ae926eb5338e4af77d14276f3() {
        assertEvalError("{ e<-new.env(parent=emptyenv()); x<-1; get(\"x\", e) }");
    }

    @Test
    public void TestSimpleBuiltins_testEnvironment_c6a4f629877c5c26aa4a01b522eb2649() {
        assertEvalError("{ h <- new.env(parent=emptyenv()) ; assign(\"y\", 2, h) ; get(\"z\", h) }");
    }

    @Test
    public void TestSimpleBuiltins_testEnvironmentIgnore_c29f313075292391e27de42119da385a() {
        assertEval("{ h <- new.env(parent=globalenv()) ; assign(\"x\", 10, h, inherits=TRUE) ; x }");
    }

    @Test
    public void TestSimpleBuiltins_testEnvironmentIgnore_ce30ddfe4bd336aa1ca03e769de77455() {
        assertEval("{ ph <- new.env() ; h <- new.env(parent=ph) ; assign(\"x\", 10, h, inherits=TRUE) ; x }");
    }

    @Test
    public void TestSimpleBuiltins_testEnvironmentIgnore_f9bf2bff62b5ca445def1eed9808f98c() {
        assertEval("{ plus <- function(x) { function(y) x + y } ; plus_one <- plus(1) ; ls(environment(plus_one)) }");
    }

    @Test
    public void TestSimpleBuiltins_testEnvironmentIgnore_0902b89753b80fe43a8612bd6c00d063() {
        assertEval("{ ls(.GlobalEnv) }");
    }

    @Test
    public void TestSimpleBuiltins_testEnvironmentIgnore_5482bc17285fec304815fd90301c9e13() {
        assertEval("{ x <- 1 ; ls(.GlobalEnv) }");
    }

    @Test
    public void TestSimpleBuiltins_testEnvironmentIgnore_067a1395bae2eadd465e38a5799ca76a() {
        assertEvalError("{ ph <- new.env(parent=emptyenv()) ; h <- new.env(parent=ph) ; assign(\"x\", 10, h, inherits=TRUE) ; get(\"x\", ph)}");
    }

    @Test
    public void TestSimpleBuiltins_testEnvironmentIgnore_60bf41382750ac0f4de965f761a2fcf7() {
        assertEvalError("{ ph <- new.env() ; h <- new.env(parent=ph) ; assign(\"x\", 2, h) ; assign(\"x\", 10, h, inherits=TRUE) ; get(\"x\", ph)}");
    }

    @Test
    public void TestSimpleBuiltins_testEval_df5a9c0a0569879276fa81b87dddc5cf() {
        assertEval("{ eval(quote(x+x), list(x=1)) }");
    }

    @Test
    public void TestSimpleBuiltins_testEval_046c5969a889af57d7ea19d1fba119d6() {
        assertEval("{ y <- 2; eval(quote(x+y), list(x=1)) }");
    }

    @Test
    public void TestSimpleBuiltins_testEval_5b956e0508e3402588200db72e33861f() {
        assertEval("{ y <- 2; x <- 4; eval(x + y, list(x=1)) }");
    }

    @Test
    public void TestSimpleBuiltins_testEval_b2e8a12bd61dc527a9bc79b8c43a380f() {
        assertEval("{ y <- 2; x <- 2 ; eval(quote(x+y), -1) }");
    }

    @Test
    public void TestSimpleBuiltins_testExp_604f92586ff1b698d6b752cce3248f1e() {
        assertEval("{ round( exp(c(1+1i,-2-3i)), digits=5 ) }");
    }

    @Test
    public void TestSimpleBuiltins_testExp_615369efc779cc2d92f0f1998762dc35() {
        assertEval("{ round( exp(1+2i), digits=5 ) }");
    }

    @Test
    public void TestSimpleBuiltins_testFFT_7d15c7af36066c109da585156e650924() {
        assertEval("{ fft(1:4) }");
    }

    @Test
    public void TestSimpleBuiltins_testFFT_f1ae7f45f01309beee55de626238e7c3() {
        assertEval("{ fft(1:4, inverse=TRUE) }");
    }

    @Test
    public void TestSimpleBuiltins_testFFT_29d449480364092bd0ea8a833767b31e() {
        assertEval("{ fft(10) }");
    }

    @Test
    public void TestSimpleBuiltins_testFFT_f64cc856ecf7295f8f5b1c98bf346710() {
        assertEval("{ fft(cbind(1:2,3:4)) }");
    }

    @Test
    public void TestSimpleBuiltins_testFileListing_9646bfd3fb553824f1f54cc5d04b8219() {
        assertEval("{ list.files(\"test/r/simple/data/tree1\") }");
    }

    @Test
    public void TestSimpleBuiltins_testFileListing_c3407928ac3dcbd4ed94ca586c8fa3bd() {
        assertEval("{ list.files(\"test/r/simple/data/tree1\", recursive=TRUE) }");
    }

    @Test
    public void TestSimpleBuiltins_testFileListing_b71b321a5f8b4e1665e1e8c55dfc00f5() {
        assertEval("{ list.files(\"test/r/simple/data/tree1\", recursive=TRUE, pattern=\".*dummy.*\") }");
    }

    @Test
    public void TestSimpleBuiltins_testFileListing_b36b504faedcd110cf3480d0627a4990() {
        assertEval("{ list.files(\"test/r/simple/data/tree1\", recursive=TRUE, pattern=\"dummy\") }");
    }

    @Test
    public void TestSimpleBuiltins_testFileListing_de580ef8e4242ba05e2ab96a9e21d936() {
        assertEval("{ list.files(\"test/r/simple/data/tree1\", pattern=\"*.tx\") }");
    }

    @Test
    public void TestSimpleBuiltins_testFloor_f23ad70b9011c6a81c3301a3dfefb542() {
        assertEval("{ floor(c(0.2,-3.4,NA,0/0,1/0)) }");
    }

    @Test
    public void TestSimpleBuiltins_testGet_17b5e1592125ebc43403174fb9611f19() {
        assertEval("{y<-function(){y<-2;get(\"y\",mode=\"integer\")};y();}");
    }

    @Test
    public void TestSimpleBuiltins_testGet_f4c68e22ab10a8f75c50c1b850306a99() {
        assertEval("{y<-function(){y<-2;get(\"y\",mode=\"closure\")};y();}");
    }

    @Test
    public void TestSimpleBuiltins_testGet_ad705be74a5aac60fbc5b60f1428ad54() {
        assertEval("{y<-function(){y<-2;get(\"y\",mode=\"integer\",inherits=FALSE);get(\"y\",mode=\"integer\",inherits=FALSE)};y();}");
    }

    @Test
    public void TestSimpleBuiltins_testGet_597a46246487040b4d1717d18b6dab16() {
        assertEval("{y<-function(){y<-2;get(\"y\",mode=\"double\")};y();}");
    }

    @Test
    public void TestSimpleBuiltins_testGet_374e182f1c02f32eea8630124198890b() {
        assertEval("{y<-function(){y<-2;get(\"y\",mode=\"double\",inherits=FALSE)};y();}");
    }

    @Test
    public void TestSimpleBuiltins_testGet_f2c6b557e11aa719fe83073c7b60a966() {
        assertEval("{ get(\"dummy\") }");
    }

    @Test
    public void TestSimpleBuiltins_testGet_1087991be7b199d7a0645e3ba9553805() {
        assertEval("{ x <- 33 ; f <- function() { if (FALSE) { x <- 22  } ; get(\"x\", inherits = FALSE) } ; f() }");
    }

    @Test
    public void TestSimpleBuiltins_testGet_5159e1fa2f3c3b1e9e2819e18d1aaed4() {
        assertEval("{ x <- 33 ; f <- function() { get(\"x\", inherits = FALSE) } ; f() }");
    }

    @Test
    public void TestSimpleBuiltins_testGet_454f5be352adc770dd02ec79976ed693() {
        assertEval("{ get(\".Platform\", globalenv())$endian }");
    }

    @Test
    public void TestSimpleBuiltins_testGet_1360e7f9c659eacd7ee24ef5ca6b274c() {
        assertEval("{ get(\".Platform\")$endian }");
    }

    @Test
    public void TestSimpleBuiltins_testGetClass_50e9635bfb1e3eeed4dd1a14ca0c6d4f() {
        assertEval("{x<-1L;class(x)}");
    }

    @Test
    public void TestSimpleBuiltins_testGetClass_af81637cccf11bc2161b1d4dedfea724() {
        assertEval("{x<-c(1L,2L,3L);class(x)}");
    }

    @Test
    public void TestSimpleBuiltins_testGetClass_e61d45b963b2c7350460c3b02c35576e() {
        assertEval("{x<-seq(1L,10L);class(x)}");
    }

    @Test
    public void TestSimpleBuiltins_testGetClass_dcaf905a90c1ecb53d1c1841d515e555() {
        assertEval("{x<-seq(1.1,10.1);class(x)}");
    }

    @Test
    public void TestSimpleBuiltins_testGetClass_b4ae30b1b3b31b7f8bf83e725f98bcca() {
        assertEval("{x<-1;class(x)}");
    }

    @Test
    public void TestSimpleBuiltins_testGetClass_a524770d8c4b8a5d06a826167443fed7() {
        assertEval("{x<-c(1,2,3);class(x)}");
    }

    @Test
    public void TestSimpleBuiltins_testGetClassIgnore_04e1bbb35c3306f6feb801b5cce80b88() {
        assertEval("{x<-seq(1,10);class(x)}");
    }

    @Test
    public void TestSimpleBuiltins_testGetIgnore_64afee6cadb778dda13b25a2f3f9ecef() {
        assertEval("{y<-function(){y<-2;get(\"y\",mode=\"closure\",inherits=FALSE);};y();}");
    }

    @Test
    public void TestSimpleBuiltins_testIdentical_c46eaf60fda944bdf1391b5fe9af0427() {
        assertEval("{ identical(1,1) }");
    }

    @Test
    public void TestSimpleBuiltins_testIdentical_53fdb3573317b847b951f1bb6b1d8ea0() {
        assertEval("{ identical(1L,1) }");
    }

    @Test
    public void TestSimpleBuiltins_testIdentical_959b063ad8742ea07448fc45ba8f9851() {
        assertEval("{ identical(1:3, c(1L,2L,3L)) }");
    }

    @Test
    public void TestSimpleBuiltins_testIdentical_0538458b54dce047351d6fe4728461d7() {
        assertEval("{ identical(0/0,1[2]) }");
    }

    @Test
    public void TestSimpleBuiltins_testIdentical_e9828bc94f4f46dc68f975a39942f654() {
        assertEval("{ identical(list(1, list(2)), list(list(1), 1)) }");
    }

    @Test
    public void TestSimpleBuiltins_testIdentical_1bd4e6954bc44911ff58137eb71e3c2c() {
        assertEval("{ identical(list(1, list(2)), list(1, list(2))) }");
    }

    @Test
    public void TestSimpleBuiltins_testIdentical_3dd8c26fc61d38a1308e5199dfaeb876() {
        assertEval("{ x <- 1 ; attr(x, \"my\") <- 10; identical(x, 1) }");
    }

    @Test
    public void TestSimpleBuiltins_testIdentical_329bfbf3d8ef2c8dccff787144ebe4c5() {
        assertEval("{ x <- 1 ; attr(x, \"my\") <- 10; y <- 1 ; attr(y, \"my\") <- 10 ; identical(x,y) }");
    }

    @Test
    public void TestSimpleBuiltins_testIdentical_905c81c2be1d34a4bba411f19c71b4ae() {
        assertEval("{ x <- 1 ; attr(x, \"my\") <- 10; y <- 1 ; attr(y, \"my\") <- 11 ; identical(x,y) }");
    }

    @Test
    public void TestSimpleBuiltins_testIdentical_6c7bc412e522d929c5a5f2071ca26ec9() {
        assertEval("{ x <- 1 ; attr(x, \"hello\") <- 2 ; attr(x, \"my\") <- 10;  attr(x, \"hello\") <- NULL ; y <- 1 ; attr(y, \"my\") <- 10 ; identical(x,y) }");
    }

    @Test
    public void TestSimpleBuiltins_testIfelse_2c1658e39df5fae138d4782ff7283d0e() {
        assertEval("{ ifelse(TRUE,1,0) }");
    }

    @Test
    public void TestSimpleBuiltins_testIfelse_31f374a4ed587c448bc38112b9f44b9f() {
        assertEval("{ ifelse(FALSE,1,0) }");
    }

    @Test
    public void TestSimpleBuiltins_testIfelse_b80411713f72aff02d9beeb4116c484c() {
        assertEval("{ ifelse(NA,1,0) }");
    }

    @Test
    public void TestSimpleBuiltins_testIn_a6548512e6448e6d86b8d072a604a135() {
        assertEval("{ 2 %in% c(1,2,3) }");
    }

    @Test
    public void TestSimpleBuiltins_testIn_ec8e3e43892fdc195b04d491cc3bdcaf() {
        assertEval("{ c(1,2,3,4,5) %in% c(1,2,1,2) }");
    }

    @Test
    public void TestSimpleBuiltins_testIn_b57b1dea7bbfaf83f8385a48613bc45b() {
        assertEval("{ \"hello\" %in% c(\"I\", \"say\", \"hello\", \"world\") }");
    }

    @Test
    public void TestSimpleBuiltins_testIn_382692b7b856b490f8d1c547d331d435() {
        assertEval("{ c(\"hello\", \"say\") %in% c(\"I\", \"say\", \"hello\", \"world\") }");
    }

    @Test
    public void TestSimpleBuiltins_testIn_e380183c1cd9aa0d725987a8c96350e7() {
        assertEval("{ `%in%`(2,c(1,2,3)) }");
    }

    @Test
    public void TestSimpleBuiltins_testInherits_d44abdb71603cf4c3dd66f3e9be929e1() {
        assertEval("{x <- 10; inherits(x, \"a\") ;}");
    }

    @Test
    public void TestSimpleBuiltins_testInherits_dfd666ff99448757c43d859d60e8bfa4() {
        assertEval("{x <- 10;class(x) <- c(\"a\", \"b\"); inherits(x,\"a\") ;}");
    }

    @Test
    public void TestSimpleBuiltins_testInherits_c7ded27e57c1310a0844a7607f124b3f() {
        assertEval("{x <- 10;class(x) <- c(\"a\", \"b\");inherits(x, \"a\", TRUE) ;}");
    }

    @Test
    public void TestSimpleBuiltins_testInherits_1a1360b46e15fc2f2e213641e50470f5() {
        assertEval("{x <- 10;class(x) <- c(\"a\", \"b\");inherits(x, c(\"a\", \"b\", \"c\"), TRUE) ;}");
    }

    @Test
    public void TestSimpleBuiltins_testInherits_6e8f9ef4539db9f9a35e6d2a43fc9fc3() {
        assertEval("{x <- 10;class(x) <- c(\"a\");inherits(x, c(\"a\", \"b\", \"a\"), TRUE) ;}");
    }

    @Test
    public void TestSimpleBuiltins_testInherits_0027ac15d1bcae36eaa7a69454d148c6() {
        assertEval("{x <- 10;class(x) <- c(\"a\", \"b\");inherits(x, c(\"c\", \"q\", \"b\"), TRUE) ;}");
    }

    @Test
    public void TestSimpleBuiltins_testInherits_b4eba1f1190198ceaa262614f59857b1() {
        assertEval("{x <- 10;class(x) <- c(\"a\", \"b\");inherits(x, c(\"c\", \"q\", \"b\")) ;}");
    }

    @Test
    public void TestSimpleBuiltins_testInherits_f90abf6cde0533d9ea8216062edbc944() {
        assertEval("{x <- 10;class(x) <- c(\"a\", \"b\");inherits(x, \"a\", c(TRUE)) ;}");
    }

    @Test
    public void TestSimpleBuiltins_testInheritsIgnore_d0dc6389c924878311546ba61d753a22() {
        assertEval("{x <- 10;class(x) <- c(\"a\", \"b\");inherits(x, 2, c(TRUE)) ;}");
    }

    @Test
    public void TestSimpleBuiltins_testInheritsIgnore_89e7444d88aeaed136ad761742bfd5e4() {
        assertEval("{x <- 10;class(x) <- c(\"a\", \"b\");inherits(x, \"a\", 1) ;}");
    }

    @Test
    public void TestSimpleBuiltins_testInvisible_8469019c606ff78421cea952d395fa6b() {
        assertEval("{ f <- function() { invisible(23) } ; toString(f()) }");
    }

    @Test
    public void TestSimpleBuiltins_testInvisible_45ebb4beda7d951423729ceea489852e() {
        assertEvalNoOutput("{ f <- function() { invisible(23) } ; f() }");
    }

    @Test
    public void TestSimpleBuiltins_testInvocation_b8f6f536144543ff7b2ec4ab5ded07cd() {
        assertEval("{ g <- function(...) { max(...) } ; g(1,2) }");
    }

    @Test
    public void TestSimpleBuiltins_testInvocation_8fe10517039bb11c9728f8bef0db2cf0() {
        assertEval("{ f <- function(a, ...) { list(...) } ; f(1) }");
    }

    @Test
    public void TestSimpleBuiltins_testInvocation_fbbf7efb3099f10d62c7d48ff602ec5d() {
        assertEval("{ matrix(da=1:3,1) }");
    }

    @Test
    public void TestSimpleBuiltins_testInvocation_63ddef3dc00d9e13df700294b80f8e85() {
        assertEvalError("{ rnorm(n = 1, n = 2) }");
    }

    @Test
    public void TestSimpleBuiltins_testInvocation_bad37b65d3db46adac99edf64bcc7f15() {
        assertEvalError("{ rnorm(s = 1, s = 1) }");
    }

    @Test
    public void TestSimpleBuiltins_testInvocation_83cadcf7287a3b26f2cdad74757af875() {
        assertEvalError("{ matrix(1:4, n = 2) }");
    }

    @Test
    public void TestSimpleBuiltins_testInvocationIgnore_6024770f1412c264dd004f2fa8bc6fbf() {
        assertEval("{ round( rnorm(1,), digits = 5 ) }");
    }

    @Test
    public void TestSimpleBuiltins_testInvocationIgnore_dd3e0cc9f1a660be34f8d72900973743() {
        assertEval("{ f <- function(...) { l <- list(...) ; l[[1]] <- 10; ..1 } ; f(11,12,13) }");
    }

    @Test
    public void TestSimpleBuiltins_testInvocationIgnore_52f3eb5641c6781b78df1fadd9026fd4() {
        assertEval("{ g <- function(...) { length(list(...)) } ; f <- function(...) { g(..., ...) } ; f(z = 1, g = 31) }");
    }

    @Test
    public void TestSimpleBuiltins_testInvocationIgnore_1258dbcba01e5d764684be0e540347c1() {
        assertEval("{ g <- function(...) { `-`(...) } ; g(1,2) }");
    }

    @Test
    public void TestSimpleBuiltins_testInvocationIgnore_a3771a811b477ac7315bd35bcf519731() {
        assertEval("{ f <- function(...) { list(a=1,...) } ; f(b=2,3) }");
    }

    @Test
    public void TestSimpleBuiltins_testInvocationIgnore_a8ec33ddd003e2f4f13be2ec0d07b6d3() {
        assertEval("{ f <- function(...) { substitute(...) } ; f(x + z) } ");
    }

    @Test
    public void TestSimpleBuiltins_testInvocationIgnore_c0649b33488ef441844f88cbeb22d470() {
        assertEval("{ p <- function(prefix, ...) { cat(prefix, ..., \"\n\") } ; p(\"INFO\", \"msg:\", \"Hello\", 42) }");
    }

    @Test
    public void TestSimpleBuiltins_testInvocationIgnore_d9a5cb384d79347b34d55b8293316a42() {
        assertEval("{ f <- function(...) { g <- function() { list(...)$a } ; g() } ; f(a=1) }");
    }

    @Test
    public void TestSimpleBuiltins_testInvocationIgnore_aa735a388a824b851e914305a0ee78ec() {
        assertEval("{ f <- function(...) { args <- list(...) ; args$name } ; f(name = 42) }");
    }

    @Test
    public void TestSimpleBuiltins_testInvocationIgnore_e7dd2cd652f2b8c1a31a90832603d4c5() {
        assertEvalError("{ matrix(x=1) }");
    }

    @Test
    public void TestSimpleBuiltins_testInvocationIgnore_53d1bf6a3bf98883a70a360da169055c() {
        assertEvalError("{ max(1,2,) }");
    }

    @Test
    public void TestSimpleBuiltins_testIsAtomic_2dc95edb23c6a5a7cd776acd9d8b0161() {
        assertEval("{ is.atomic(1) }");
    }

    @Test
    public void TestSimpleBuiltins_testIsAtomic_3519e4ab9adfd7f0f31e17cd790d60ef() {
        assertEval("{ is.atomic(1L) }");
    }

    @Test
    public void TestSimpleBuiltins_testIsAtomic_8e32cfd92fe5419280f614af9bd8b7c5() {
        assertEval("{ is.atomic(1:3) }");
    }

    @Test
    public void TestSimpleBuiltins_testIsAtomic_98b4f52e672f1abec09834a0b1d70ef7() {
        assertEval("{ is.atomic(c(1,2,3)) }");
    }

    @Test
    public void TestSimpleBuiltins_testIsAtomic_078ba04ee5202e6b9182e3ca89d99c09() {
        assertEval("{ is.atomic(NA) }");
    }

    @Test
    public void TestSimpleBuiltins_testIsAtomic_c5aa836cf30d2bfbf49845464431e193() {
        assertEval("{ is.atomic(NULL) }");
    }

    @Test
    public void TestSimpleBuiltins_testIsAtomic_e20e6e0a0f334955a28fb3f440bc7100() {
        assertEval("{ is.atomic(TRUE) }");
    }

    @Test
    public void TestSimpleBuiltins_testIsAtomic_a6e57d1b16ffb05d8352862ff5be69ba() {
        assertEval("{ !is.atomic(list()) }");
    }

    @Test
    public void TestSimpleBuiltins_testIsAtomic_c5836be5597617c6d0b6f1ff8b620966() {
        assertEval("{ !is.atomic(function() {}) }");
    }

    @Test
    public void TestSimpleBuiltins_testIsNA_b8cf60154d61f3d4fb5896e670ddc520() {
        assertEval("{ is.na(NA) }");
    }

    @Test
    public void TestSimpleBuiltins_testIsNA_edaacabd596ade1ca61f9c8197938e57() {
        assertEval("{ is.na(c(NA)) }");
    }

    @Test
    public void TestSimpleBuiltins_testIsNA_5024ef11649883961aa523598dad360f() {
        assertEval("{ is.na(c(1,2,3,4)) }");
    }

    @Test
    public void TestSimpleBuiltins_testIsNA_324cc04c8c1b252aa5a25ec87ed592a6() {
        assertEval("{ is.na(c(1,2,NA,4)) }");
    }

    @Test
    public void TestSimpleBuiltins_testIsNA_0cbc25db85ed6a5941455678fdd166c2() {
        assertEval("{ is.na(1[10]) }");
    }

    @Test
    public void TestSimpleBuiltins_testIsNA_19b0c24d7c5d403027bfb91f9e95c742() {
        assertEval("{ is.na(c(1[10],2[10],3)) }");
    }

    @Test
    public void TestSimpleBuiltins_testIsNABroken_7215b0a22d4bed3645f9359539e607c8() {
        assertEval("{ is.na(list(1[10],1L[10],list(),integer())) }");
    }

    @Test
    public void TestSimpleBuiltins_testIsObject_bf8900970d62fc778d5a113513a1f4f8() {
        assertEval("{ is.object(1) }");
    }

    @Test
    public void TestSimpleBuiltins_testIsObject_b326b8b58745eb5babff2b83ac2cf9c5() {
        assertEval("{ is.object(1L) }");
    }

    @Test
    public void TestSimpleBuiltins_testIsObject_a29dac377f658c7f9518428cefd90410() {
        assertEval("{ is.object(1:3) }");
    }

    @Test
    public void TestSimpleBuiltins_testIsObject_743f1640a136cc343f2fde63d5fdfdae() {
        assertEval("{ is.object(c(1,2,3)) }");
    }

    @Test
    public void TestSimpleBuiltins_testIsObject_f016e0d824b333e1fbead8d5a7e7baae() {
        assertEval("{ is.object(NA) }");
    }

    @Test
    public void TestSimpleBuiltins_testIsObject_d734f214980ce3573bfcf525d400afa6() {
        assertEval("{ is.object(NULL) }");
    }

    @Test
    public void TestSimpleBuiltins_testIsTRUE_acbc975cbce8f2d6a8b5422c52a423d3() {
        assertEval("{ isTRUE(TRUE) }");
    }

    @Test
    public void TestSimpleBuiltins_testIsTRUE_914502371c8dda2fce5a0ab871b41cd7() {
        assertEval("{ isTRUE(FALSE) }");
    }

    @Test
    public void TestSimpleBuiltins_testIsTRUE_0c8034c5be2cf8442cbe373537d1f7d0() {
        assertEval("{ isTRUE(NA) }");
    }

    @Test
    public void TestSimpleBuiltins_testIsTRUE_78f604c978973485d42c11bc4d70aaeb() {
        assertEval("{ isTRUE(1) }");
    }

    @Test
    public void TestSimpleBuiltins_testIsTRUE_4510212fe57e0d1dc393efa4c05bfc0f() {
        assertEval("{ isTRUE(as.vector(TRUE)) }");
    }

    @Test
    public void TestSimpleBuiltins_testIsTRUE_a3b6592c79b5852ef853c585e0eff03d() {
        assertEval("{ isTRUE(as.vector(FALSE)) }");
    }

    @Test
    public void TestSimpleBuiltins_testIsTRUE_625a4bacb96d3aa1531e631190657a54() {
        assertEval("{ isTRUE(as.vector(1)) }");
    }

    @Test
    public void TestSimpleBuiltins_testIsUnsorted_3b7257dae08d22cc08ee94a46df33918() {
        assertEval("{ is.unsorted(c(1,2,3,4)) }");
    }

    @Test
    public void TestSimpleBuiltins_testIsUnsorted_82bae88571c23ab3537cf19d60233dde() {
        assertEval("{ is.unsorted(c(1,2,6,4)) }");
    }

    @Test
    public void TestSimpleBuiltins_testLength_31389093e21d303bed26828ab536576b() {
        assertEval("{ x <- 1:4 ; length(x) <- 2 ; x }");
    }

    @Test
    public void TestSimpleBuiltins_testLength_548e298d60c6cf79aede96d767748eea() {
        assertEval("{ x <- 1:2 ; length(x) <- 4 ; x }");
    }

    @Test
    public void TestSimpleBuiltins_testLength_0f085fe508c7e13bd8d44c9c226377fd() {
        assertEval("{ length(c(z=1:4)) }");
    }

    @Test
    public void TestSimpleBuiltins_testLength_fe015fb0ebc736db809aa71ec2873568() {
        assertEval("{ x <- 1 ; f <- function() { length(x) <<- 2 } ; f() ; x }");
    }

    @Test
    public void TestSimpleBuiltins_testLength_de39589213b36483b01fbfd93b30cbe9() {
        assertEval("{ length(1) }");
    }

    @Test
    public void TestSimpleBuiltins_testLength_d58a76dd44eb0ab3495b642b5ac5e52b() {
        assertEval("{ length(NULL) }");
    }

    @Test
    public void TestSimpleBuiltins_testLength_8fe0624350dd66dc914610e9e3e473ff() {
        assertEval("{ length(NA) }");
    }

    @Test
    public void TestSimpleBuiltins_testLength_9e576ee58a7485a487496efb447bc71e() {
        assertEval("{ length(TRUE) }");
    }

    @Test
    public void TestSimpleBuiltins_testLength_15dd7cd7751d7f0ca49a90ffdd7adcd3() {
        assertEval("{ length(1L) }");
    }

    @Test
    public void TestSimpleBuiltins_testLength_9aa44152c0aaa301d4e61860e8e3329e() {
        assertEval("{ length(1+1i) }");
    }

    @Test
    public void TestSimpleBuiltins_testLength_5f87030622796c8145f800bea19c5345() {
        assertEval("{ length(d<-dim(1:3)) }");
    }

    @Test
    public void TestSimpleBuiltins_testLength_269468e63a5cd6592dc8477ae86c43f7() {
        assertEval("{ length(1:3) }");
    }

    @Test
    public void TestSimpleBuiltins_testLength_e5f8864e4d2b6f979fc4290defe16267() {
        assertEval("{ x <- 1:2 ; z <- (length(x) <- 4) ; z }");
    }

    @Test
    public void TestSimpleBuiltins_testLength_d456081defe1bac5ede62fe309160330() {
        assertEval("{ x<-c(a=7, b=42); length(x)<-4; x }");
    }

    @Test
    public void TestSimpleBuiltins_testLength_e220727d80d0083f6b28a575dd62de95() {
        assertEval("{ x<-c(a=7, b=42); length(x)<-1; x }");
    }

    @Test
    public void TestSimpleBuiltins_testList_c74b9ee71e8970c28a28d0daff0eeb0f() {
        assertEval("{ list(a=1, b=2) }");
    }

    @Test
    public void TestSimpleBuiltins_testList_905882aea8d1a0d06c1a7b1fa68efd42() {
        assertEval("{ list(a=1, 2) }");
    }

    @Test
    public void TestSimpleBuiltins_testList_3eccf067711ea3cc347de2d8588564af() {
        assertEval("{ list(1, b=2) }");
    }

    @Test
    public void TestSimpleBuiltins_testList_253f9818561781cfb77cfc14ff3da5ba() {
        assertEval("{ x<-c(y=1, 2);  list(a=x, 42) }");
    }

    @Test
    public void TestSimpleBuiltins_testList_fe6c4308041539f9b779caa3c25fbfc7() {
        assertEval("{ x<-list(y=1, 2);  c(a=x, 42) }");
    }

    @Test
    public void TestSimpleBuiltins_testList_62081abc4c770e1bb51d360d37cadc94() {
        assertEval("{ x<-list(y=1, 2);  c(42, a=x) }");
    }

    @Test
    public void TestSimpleBuiltins_testList_9ac495270f3a395351229eb03a27261c() {
        assertEval("{ x<-list(y=1, 2);  c(a=x, c(z=7,42)) }");
    }

    @Test
    public void TestSimpleBuiltins_testList_7f6e35d21661c8d2607996e34b736525() {
        assertEval("{ x<-list(y=1, 2);  c(a=x, c(y=7,z=42)) }");
    }

    @Test
    public void TestSimpleBuiltins_testLog_fd3ef6f859e8e0d8956abf0e2bec0c13() {
        assertEval("{ log(1) } ");
    }

    @Test
    public void TestSimpleBuiltins_testLog_1b4ff97a47d0ed4a8f74c200941bb51d() {
        assertEval("{ log(0) }");
    }

    @Test
    public void TestSimpleBuiltins_testLog_f5b2053e10cdc3ff7f3bbc2ded231629() {
        assertEval("{ log(c(0,1)) }");
    }

    @Test
    public void TestSimpleBuiltins_testLog_19a1529b84641df8cd280fc3f04fdc83() {
        assertEval("{ round( log(10,), digits = 5 ) }");
    }

    @Test
    public void TestSimpleBuiltins_testLog_11d410bde7960c23039008b176a8c6de() {
        assertEval("{ round( log(10,2), digits = 5 ) }");
    }

    @Test
    public void TestSimpleBuiltins_testLog_55d1aea1ad49cb7388b91157708cc4fd() {
        assertEval("{ round( log(10,10), digits = 5 ) }");
    }

    @Test
    public void TestSimpleBuiltins_testLog10_ecfc4b2a884388b0322382974493d08e() {
        assertEval("{ log10(1) } ");
    }

    @Test
    public void TestSimpleBuiltins_testLog10_b15b2fca363331ef75efe46529aefb72() {
        assertEval("{ log10(0) }");
    }

    @Test
    public void TestSimpleBuiltins_testLog10_7c91cb1360cfaaae3fa54a4bff915fc4() {
        assertEval("{ log10(c(0,1)) }");
    }

    @Test
    public void TestSimpleBuiltins_testLog10_4d940d8c097ab0ad12ab89bcaec28f84() {
        assertEval("{ log10(10) } ");
    }

    @Test
    public void TestSimpleBuiltins_testLog10_24fb260f9773853204641bd5f4dcdafb() {
        assertEval("{ log10(100) } ");
    }

    @Test
    public void TestSimpleBuiltins_testLog10_a8854ea52706d8d97a82cc31318131e3() {
        assertEval("{ as.integer(log10(200)*100000) } ");
    }

    @Test
    public void TestSimpleBuiltins_testLog2_cd2db08763e44c63bd85072193e4a7e5() {
        assertEval("{ log2(1) } ");
    }

    @Test
    public void TestSimpleBuiltins_testLog2_9254be07c6ac1cad9d45feb8ec7459d6() {
        assertEval("{ log2(0) }");
    }

    @Test
    public void TestSimpleBuiltins_testLog2_187e92c1da74959a70ddf63ed571ce27() {
        assertEval("{ log2(c(0,1)) }");
    }

    @Test
    public void TestSimpleBuiltins_testLog2_4ae6cbbe47ecaa53673148a96c985996() {
        assertEval("{ log2(2) } ");
    }

    @Test
    public void TestSimpleBuiltins_testLog2_da7dbf3a647d0ad0dff364d19ed73536() {
        assertEval("{ log2(4) } ");
    }

    @Test
    public void TestSimpleBuiltins_testLog2_1867c20d58519782db9911c7db59004e() {
        assertEval("{ as.integer(log2(6)*1000000) } ");
    }

    @Test
    public void TestSimpleBuiltins_testLogIgnore_052ed04e88403025c80c488866a0f346() {
        assertEval("{ m <- matrix(1:4, nrow=2) ; round( log10(m), digits=5 )  }");
    }

    @Test
    public void TestSimpleBuiltins_testLogIgnore_6568d70e4d076fc4b14b58158162a0ea() {
        assertEval("{ x <- c(a=1, b=10) ; round( c(log(x), log10(x), log2(x)), digits=5 ) }");
    }

    @Test
    public void TestSimpleBuiltins_testLookup_3a5345078701581c660c582e0464b9da() {
        assertEval("{ f <- function() { assign(\"x\", 1) ; x } ; f() }");
    }

    @Test
    public void TestSimpleBuiltins_testLookup_39e78cb4d5d82c567efcfcc580f74363() {
        assertEval("{ f <- function() { x <- 2 ; g <- function() { x <- 3 ; assign(\"x\", 1, inherits=FALSE) ; x } ; g() } ; f() }");
    }

    @Test
    public void TestSimpleBuiltins_testLookup_548434da986ff8aa88f0f6a76e458833() {
        assertEval("{ f <- function() { x <- 2 ; g <- function() { assign(\"x\", 1, inherits=FALSE) } ; g() ; x } ; f() }");
    }

    @Test
    public void TestSimpleBuiltins_testLookup_16e998e313fb51e237cac840df83519f() {
        assertEval("{ f <- function() { x <- 2 ; g <- function() { assign(\"x\", 1, inherits=TRUE) } ; g() ; x } ; f() }");
    }

    @Test
    public void TestSimpleBuiltins_testLookup_9fb00dc9e429b3567af94555695cf89f() {
        assertEval("{ f <- function() {  g <- function() { assign(\"x\", 1, inherits=TRUE) } ; g() } ; f() ; x }");
    }

    @Test
    public void TestSimpleBuiltins_testLookup_0d33a9cf9cdcc65307f78d195cc549ef() {
        assertEval("{ x <- 3 ; g <- function() { x } ; f <- function() { assign(\"x\", 2) ; g() } ; f() }");
    }

    @Test
    public void TestSimpleBuiltins_testLookup_6a2bfceedc36d0b2ff994b5a30df433a() {
        assertEval("{ x <- 3 ; f <- function() { assign(\"x\", 2) ; g <- function() { x } ; g() } ; f() }");
    }

    @Test
    public void TestSimpleBuiltins_testLookup_3a86279a40eaa4f21960a2abfc0764dc() {
        assertEval("{ h <- function() { x <- 3 ; g <- function() { x } ; f <- function() { assign(\"x\", 2) ; g() } ; f() }  ; h() }");
    }

    @Test
    public void TestSimpleBuiltins_testLookup_5b652ae1ae9d60ef8850d5f3b3843f5a() {
        assertEval("{ h <- function() { x <- 3  ; f <- function() { assign(\"x\", 2) ; g <- function() { x } ; g() } ; f() }  ; h() }");
    }

    @Test
    public void TestSimpleBuiltins_testLookup_73733642fbadb8ac6edf05a8acf5d6de() {
        assertEval("{ x <- 3 ; h <- function() { g <- function() { x } ; f <- function() { assign(\"x\", 2, inherits=TRUE) } ; f() ; g() }  ; h() }");
    }

    @Test
    public void TestSimpleBuiltins_testLookup_2d61a18e5af79bced1a351870a9d514e() {
        assertEval("{ x <- 3 ; h <- function(s) { if (s == 2) { assign(\"x\", 2) } ; x }  ; h(1) ; h(2) }");
    }

    @Test
    public void TestSimpleBuiltins_testLookup_492925abedef28fc7b3e0bfae9ba8a52() {
        assertEval("{ x <- 3 ; h <- function(s) { y <- x ; if (s == 2) { assign(\"x\", 2) } ; c(y,x) }  ; c(h(1),h(2)) }");
    }

    @Test
    public void TestSimpleBuiltins_testLookup_0f7e2fdb465f6c47ae4b0a1085eea1b1() {
        assertEval("{ g <- function() { x <- 2 ; f <- function() { x ; exists(\"x\") }  ; f() } ; g() }");
    }

    @Test
    public void TestSimpleBuiltins_testLookup_3780dc57c780fbb8cec9704dc7de17ed() {
        assertEval("{ g <- function() { f <- function() { if (FALSE) { x } ; exists(\"x\") }  ; f() } ; g() }");
    }

    @Test
    public void TestSimpleBuiltins_testLookup_3ae3e1ea3a68f0a98203b1398c63a59c() {
        assertEval("{ g <- function() { f <- function() { if (FALSE) { x } ; assign(\"x\", 1) ; exists(\"x\") }  ; f() } ; g() }");
    }

    @Test
    public void TestSimpleBuiltins_testLookup_1e7c2d8dd4c08474021c9e95ef2232ff() {
        assertEval("{ g <- function() { if (FALSE) { x <- 2 } ; f <- function() { if (FALSE) { x } ; exists(\"x\") }  ; f() } ; g() }");
    }

    @Test
    public void TestSimpleBuiltins_testLookup_1afe1fb2b81d78394742bc213196d303() {
        assertEval("{ g <- function() { if (FALSE) { x <- 2 } ; f <- function() { if (FALSE) { x } ; assign(\"x\", 2) ; exists(\"x\") }  ; f() } ; g() }");
    }

    @Test
    public void TestSimpleBuiltins_testLookup_9bb2b5cc4a3c0540f6289c0c070aa49a() {
        assertEval("{ h <- function() { g <- function() { if (FALSE) { x <- 2 } ; f <- function() { if (FALSE) { x } ; exists(\"x\") }  ; f() } ; g() } ; h() }");
    }

    @Test
    public void TestSimpleBuiltins_testLookup_daf243dde25f3873d1202df260bb3114() {
        assertEval("{ h <- function() { x <- 3 ; g <- function() { if (FALSE) { x <- 2 } ; f <- function() { if (FALSE) { x } ; exists(\"x\") }  ; f() } ; g() } ; h() }");
    }

    @Test
    public void TestSimpleBuiltins_testLookup_5b0f9d198fb49e36c8af3be9834dabd1() {
        assertEval("{ f <- function(z) { exists(\"z\") } ; f() }");
    }

    @Test
    public void TestSimpleBuiltins_testLookup_48b08b9590e75552578d0fb596c87287() {
        assertEval("{ f <- function() { x <- 3 ; exists(\"x\", inherits=FALSE) } ; f() }");
    }

    @Test
    public void TestSimpleBuiltins_testLookup_38aded252ff8f6a6c63dd7e8bbe684b1() {
        assertEval("{ f <- function() { z <- 3 ; exists(\"x\", inherits=FALSE) } ; f() }");
    }

    @Test
    public void TestSimpleBuiltins_testLookup_4ab259967887738519b4bf25c2ae70de() {
        assertEval("{ f <- function() { if (FALSE) { x <- 3 } ; exists(\"x\", inherits=FALSE) } ; f() }");
    }

    @Test
    public void TestSimpleBuiltins_testLookup_6a8e5da083d06d4e72430b9f3870fef2() {
        assertEval("{ f <- function() { assign(\"x\", 2) ; exists(\"x\", inherits=FALSE) } ; f() }");
    }

    @Test
    public void TestSimpleBuiltins_testLookup_1677e8dba1f8ac3830d985614e17644c() {
        assertEval("{ g <- function() { x <- 2 ; f <- function() { if (FALSE) { x <- 3 } ; exists(\"x\") }  ; f() } ; g() }");
    }

    @Test
    public void TestSimpleBuiltins_testLookup_6aec41beabbda969b6aed4c316d4d445() {
        assertEval("{ g <- function() { x <- 2 ; f <- function() { x <- 5 ; exists(\"x\") }  ; f() } ; g() }");
    }

    @Test
    public void TestSimpleBuiltins_testLookup_3a3976ee840d8ae83ac739ddc0f60bf6() {
        assertEval("{ g <- function() { f <- function() { assign(\"x\", 3) ; if (FALSE) { x } ; exists(\"x\") }  ; f() } ; g() }");
    }

    @Test
    public void TestSimpleBuiltins_testLookup_c89981601d0bb40314d41b0df22de379() {
        assertEval("{ g <- function() { f <- function() { assign(\"z\", 3) ; if (FALSE) { x } ; exists(\"x\") }  ; f() } ; g() }");
    }

    @Test
    public void TestSimpleBuiltins_testLookup_615e5eabb7122045650a11c6e8e1b14a() {
        assertEval("{ h <- function() { assign(\"x\", 1) ; g <- function() { if (FALSE) { x <- 2 } ; f <- function() { if (FALSE) { x } ; exists(\"x\") }  ; f() } ; g() } ; h() }");
    }

    @Test
    public void TestSimpleBuiltins_testLookup_4d5704d9f1b90efda2c59502caa0c315() {
        assertEval("{ h <- function() { assign(\"z\", 1) ; g <- function() { if (FALSE) { x <- 2 } ; f <- function() { if (FALSE) { x } ; exists(\"x\") }  ; f() } ; g() } ; h() }");
    }

    @Test
    public void TestSimpleBuiltins_testLookup_a7f5ec2e61d3f25ff85aeb7cf82b97f0() {
        assertEval("{ h <- function() { x <- 3 ; g <- function() { f <- function() { if (FALSE) { x } ; exists(\"x\") }  ; f() } ; g() } ; h() }");
    }

    @Test
    public void TestSimpleBuiltins_testLookup_56f22eb0a023e8f286f44ce532f7dd73() {
        assertEval("{ x <- 3 ; f <- function() { exists(\"x\") } ; f() }");
    }

    @Test
    public void TestSimpleBuiltins_testLookup_6ee73db0e1b11e0b48199609cb7170b1() {
        assertEval("{ x <- 3 ; f <- function() { exists(\"x\", inherits=FALSE) } ; f() }");
    }

    @Test
    public void TestSimpleBuiltins_testLookup_1b704057d0a3b32717c4812143a5f57f() {
        assertEval("{ x <- 2 ; y <- 3 ; rm(\"y\") ; ls() }");
    }

    @Test
    public void TestSimpleBuiltins_testLookup_5f9149f4bb0770f64df8392c8f0f1560() {
        assertEval("{ f <- function() { if (FALSE) { x <- 1 } ; y <- 2 ; ls() } ; f() }");
    }

    @Test
    public void TestSimpleBuiltins_testLookup_9f7ce37cec840951bd4c5c058fa11af0() {
        assertEval("{ f <- function() { for (i in rev(1:10)) { assign(as.character(i), i) } ; ls() } ; length(f()) }");
    }

    @Test
    public void TestSimpleBuiltins_testLookup_6d315a9f02daf4ce58004586f3de768e() {
        assertEval("{ x <- 3 ; f <- function() { assign(\"x\", 4) ; h <- function(s=1) { if (s==2) { x <- 5 } ; x } ; h() } ; f() }");
    }

    @Test
    public void TestSimpleBuiltins_testLookup_f2bb3dcedd62406c5e49354fc09e0aa8() {
        assertEval("{ x <- 3 ; f <- function() { assign(\"x\", 4) ; g <- function() { assign(\"y\", 3) ; h <- function(s=1) { if (s==2) { x <- 5 } ; x } ; h() } ; g()  } ; f() }");
    }

    @Test
    public void TestSimpleBuiltins_testLookup_18db76eaec1c42a5a710f01e49b79444() {
        assertEval("{ f <- function() { assign(\"x\", 2, inherits=TRUE) ; assign(\"x\", 1) ; h <- function() { x } ; h() } ; f() }");
    }

    @Test
    public void TestSimpleBuiltins_testLookup_30309b638e880e9d3cfbde105cc95686() {
        assertEval("{ x <- 3 ; g <- function() { if (FALSE) { x <- 2 } ; f <- function() { h <- function() { x } ; h() } ; f() } ; g() }");
    }

    @Test
    public void TestSimpleBuiltins_testLookup_d83ca8b14b8ce96aab4d3c068cc01d37() {
        assertEval("{ x <- 3 ; gg <- function() {  g <- function() { if (FALSE) { x <- 2 } ; f <- function() { h <- function() { x } ; h() } ; f() } ; g() } ; gg() }");
    }

    @Test
    public void TestSimpleBuiltins_testLookup_78b53abedd804defaf12eb15b203d3c6() {
        assertEval("{ h <- function() { x <- 2 ; f <- function() { if (FALSE) { x <- 1 } ; g <- function() { x } ; g() } ; f() } ; h() }");
    }

    @Test
    public void TestSimpleBuiltins_testLookup_324112969e78fa9f195b1551bbe8e4b2() {
        assertEval("{ f <- function() { assign(\"x\", 3) ; g <- function() { x } ; g() } ; x <- 10 ; f() }");
    }

    @Test
    public void TestSimpleBuiltins_testLookup_ae7740eaeaefe74759fbc829bade39e0() {
        assertEval("{ f <- function() { assign(\"x\", 3) ; h <- function() { assign(\"z\", 4) ; g <- function() { x } ; g() } ; h() } ; x <- 10 ; f() }");
    }

    @Test
    public void TestSimpleBuiltins_testLookup_75c39aaa83d4cde89f3f39d408e4504d() {
        assertEval("{ f <- function() { assign(\"x\", 3) ; h <- function() { g <- function() { x } ; g() } ; h() } ; x <- 10 ; f() }");
    }

    @Test
    public void TestSimpleBuiltins_testLookup_f229f5696856e7d097d3e2b6cd84ad7e() {
        assertEval("{ f <- function() { assign(\"x\", 1) ; g <- function() { assign(\"z\", 2) ; x } ; g() } ; f() }");
    }

    @Test
    public void TestSimpleBuiltins_testLookup_d41ff44d767d83bfd35007621e1eac60() {
        assertEval("{ h <- function() { x <- 3 ; g <- function() { assign(\"z\", 2) ; x } ; f <- function() { assign(\"x\", 2) ; g() } ; f() }  ; h() }");
    }

    @Test
    public void TestSimpleBuiltins_testLookup_d966bf2f7931aa1107f571930d7c5741() {
        assertEval("{ h <- function() { x <- 3 ; g <- function() { assign(\"x\", 5) ; x } ; f <- function() { assign(\"x\", 2) ; g() } ; f() }  ; h() }");
    }

    @Test
    public void TestSimpleBuiltins_testLookup_e602c31474de55d9faf2ff34075b7cfb() {
        assertEval("{ x <- 10 ; g <- function() { x <- 100 ; z <- 2 ; f <- function() { assign(\"z\", 1); x <- x ; x } ; f() } ; g() }");
    }

    @Test
    public void TestSimpleBuiltins_testLookup_f2d9a62ccea6f97320c4b9dcb3db8dcb() {
        assertEval("{ g <- function() { if (FALSE) { x <- 2 ; y <- 3} ; f <- function() { if (FALSE) { x } ; assign(\"y\", 2) ; exists(\"x\") }  ; f() } ; g() }");
    }

    @Test
    public void TestSimpleBuiltins_testLookup_23d1c3aae3a7b25d923ab1a04acdcc5a() {
        assertEval("{ g <- function() { if (FALSE) {y <- 3; x <- 2} ; f <- function() { assign(\"x\", 2) ; exists(\"x\") }  ; f() } ; g() }");
    }

    @Test
    public void TestSimpleBuiltins_testLookup_c6c8e0f9c3bbb80af43935eacb608104() {
        assertEval("{ g <- function() { if (FALSE) {y <- 3; x <- 2} ; f <- function() { assign(\"x\", 2) ; h <- function() { exists(\"x\") } ; h() }  ; f() } ; g() }");
    }

    @Test
    public void TestSimpleBuiltins_testLookup_c5d265d6092f4dd023538cd02e7f1bc9() {
        assertEval("{ g <- function() { if (FALSE) {y <- 3; x <- 2} ; f <- function() { assign(\"y\", 2) ; h <- function() { exists(\"x\") } ; h() }  ; f() } ; g() }");
    }

    @Test
    public void TestSimpleBuiltins_testLookup_c0a2d99911ad04c0e8587f08a546f0fa() {
        assertEval("{ g <- function() { if (FALSE) {y <- 3; x <- 2} ; f <- function() { assign(\"x\", 2) ; gg <- function() { h <- function() { exists(\"x\") } ; h() } ; gg() } ; f() } ; g() }");
    }

    @Test
    public void TestSimpleBuiltins_testLookup_e89d9d3f7ae96ba78c6773593f947412() {
        assertEval("{ x <- 3 ; f <- function(i) { if (i == 1) { assign(\"x\", 4) } ; function() { x } } ; f1 <- f(1) ; f2 <- f(2) ; f1() }");
    }

    @Test
    public void TestSimpleBuiltins_testLookup_e1b77ad90ab35bc1e2c43c6082cc3eab() {
        assertEval("{ x <- 3 ; f <- function(i) { if (i == 1) { assign(\"x\", 4) } ; function() { x } } ; f1 <- f(1) ; f2 <- f(2) ; f2() ; f1() }");
    }

    @Test
    public void TestSimpleBuiltins_testLookup_4e6bde1abd5de65801380cf42006b6cc() {
        assertEval("{ f <- function() { x <- 2 ; g <- function() { if (FALSE) { x <- 2 } ; assign(\"x\", 1, inherits=TRUE) } ; g() ; x } ; f() }");
    }

    @Test
    public void TestSimpleBuiltins_testLookup_e1d9cbe74522cc75d4891616afca4c9d() {
        assertEval("{ h <- function() { if (FALSE) { x <- 2 ; z <- 3 } ; g <- function() { assign(\"z\", 3) ; if (FALSE) { x <- 4 } ;  f <- function() { exists(\"x\") } ; f() } ; g() } ; h() }");
    }

    @Test
    public void TestSimpleBuiltins_testLookup_47089dcda7fca6ff9b671c7e318eb58f() {
        assertEval("{ f <- function(x) { assign(x, 23) ; exists(x) } ; c(f(\"a\"),f(\"b\")) }");
    }

    @Test
    public void TestSimpleBuiltins_testLookup_60546b187f8a5219bf72536c8824dab6() {
        assertEval("{ f <- function() { x <- 2 ; get(\"x\") } ; f() }");
    }

    @Test
    public void TestSimpleBuiltins_testLookup_f6a5f0c10480af1d2547ce0768efd58f() {
        assertEval("{ x <- 3 ; f <- function() { get(\"x\") } ; f() }");
    }

    @Test
    public void TestSimpleBuiltins_testLookup_81780ae1edf74a449351bc1cc7bf5e99() {
        assertEval("{ x <- 3 ; f <- function() { x <- 2 ; get(\"x\") } ; f() }");
    }

    @Test
    public void TestSimpleBuiltins_testLookup_db082090d084c963d4553b4120231e83() {
        assertEval("{ x <- 3 ; f <- function() { x <- 2; h <- function() {  get(\"x\") }  ; h() } ; f() }");
    }

    @Test
    public void TestSimpleBuiltins_testLookup_339a4adbc575c5f1423bd5477c48e02b() {
        assertEval("{ f <- function() { g <- function() { get(\"x\", inherits=TRUE) } ; g() } ; x <- 3 ; f() }");
    }

    @Test
    public void TestSimpleBuiltins_testLookup_aaaf86fd401db53b800026b8eb4724ce() {
        assertEval("{ f <- function() { assign(\"z\", 2) ; g <- function() { get(\"x\", inherits=TRUE) } ; g() } ; x <- 3 ; f() }");
    }

    @Test
    public void TestSimpleBuiltins_testLookup_be81c769a08855b15887cc765467b5e5() {
        assertEval("{ f <- function() { x <- 22 ; get(\"x\", inherits=FALSE) } ; f() }");
    }

    @Test
    public void TestSimpleBuiltins_testLookup_5dd4780ce6e2667b02ccb16a838a7057() {
        assertEval("{ x <- 33 ; f <- function() { assign(\"x\", 44) ; get(\"x\", inherits=FALSE) } ; f() }");
    }

    @Test
    public void TestSimpleBuiltins_testLookup_ddb48b4bb7fa157390e03b1f4822a35b() {
        assertEval("{ g <- function() { if (FALSE) {y <- 3; x <- 2} ; f <- function() { assign(\"x\", 2) ; gg <- function() { h <- function() { get(\"x\") } ; h() } ; gg() } ; f() } ; g() }");
    }

    @Test
    public void TestSimpleBuiltins_testLookup_2d20392fa0238bebf581e87c3ae5e40b() {
        assertEval("{ x <- function(){3} ; f <- function() { assign(\"x\", function(){4}) ; h <- function(s=1) { if (s==2) { x <- 5 } ; x() } ; h() } ; f() }");
    }

    @Test
    public void TestSimpleBuiltins_testLookup_a0d2460a2e8d29a01faad9f15a7300ac() {
        assertEval("{ f <- function() { assign(\"x\", function(){2}, inherits=TRUE) ; assign(\"x\", function(){1}) ; h <- function() { x() } ; h() } ; f() }");
    }

    @Test
    public void TestSimpleBuiltins_testLookup_2909415dd11417ff5c2c2f2d8c31723e() {
        assertEval("{ x <- function(){3} ; g <- function() { if (FALSE) { x <- 2 } ; f <- function() { h <- function() { x() } ; h() } ; f() } ; g() }");
    }

    @Test
    public void TestSimpleBuiltins_testLookup_f7ec9350e3e49b28cda6909084c5230f() {
        assertEval("{ x <- function(){3} ; gg <- function() {  g <- function() { if (FALSE) { x <- 2 } ; f <- function() { h <- function() { x() } ; h() } ; f() } ; g() } ; gg() }");
    }

    @Test
    public void TestSimpleBuiltins_testLookup_45a53720348ebc4d67c11a38c334bfeb() {
        assertEval("{ h <- function() { x <- function(){2} ; f <- function() { if (FALSE) { x <- 1 } ; g <- function() { x } ; g() } ; f() } ; z <- h() ; z() }");
    }

    @Test
    public void TestSimpleBuiltins_testLookup_f47389c544ecea804ef4bbcdbdddd787() {
        assertEval("{ h <- function() { g <- function() {4} ; f <- function() { if (FALSE) { g <- 4 } ; g() } ; f() } ; h() }");
    }

    @Test
    public void TestSimpleBuiltins_testLookup_f1b20896d2c392017c0654d07d5e398b() {
        assertEval("{ h <- function() { assign(\"f\", function() {4}) ; f() } ; h() }");
    }

    @Test
    public void TestSimpleBuiltins_testLookup_4eb6105ac5a6aed8ae0b1b7faf6d66c9() {
        assertEval("{ f <- function() { 4 } ; h <- function() { assign(\"f\", 5) ; f() } ; h() }");
    }

    @Test
    public void TestSimpleBuiltins_testLookup_52bac7c5c3cb46bc9622cbfc09f2bc6b() {
        assertEval("{ f <- function() { 4 } ; h <- function() { assign(\"z\", 5) ; f() } ; h() }");
    }

    @Test
    public void TestSimpleBuiltins_testLookup_f7462e1b64e98cc4342dbd4bdba53a77() {
        assertEval("{ gg <- function() {  assign(\"x\", function(){11}) ; g <- function() { if (FALSE) { x <- 2 } ; f <- function() { h <- function() { x() } ; h() } ; f() } ; g() } ; gg() }");
    }

    @Test
    public void TestSimpleBuiltins_testLookup_7ccbe91c968b460f891aa1e191584ae2() {
        assertEval("{ x <- function(){3} ; gg <- function() { assign(\"x\", 4) ; g <- function() { if (FALSE) { x <- 2 } ; f <- function() { h <- function() { x() } ; h() } ; f() } ; g() } ; gg() }");
    }

    @Test
    public void TestSimpleBuiltins_testLookup_87c7111434f45f87d7319f771a86725e() {
        assertEval("{ h <- function() { x <- function() {3} ; g <- function() { assign(\"z\", 2) ; x } ; f <- function() { assign(\"x\", 2) ; g() } ; f() }  ; z <- h() ; z() }");
    }

    @Test
    public void TestSimpleBuiltins_testLookup_6091ece90f56a846f7bad3990789920d() {
        assertEval("{ h <- function() { x <- function() {3} ; g <- function() { assign(\"x\", function() {5} ) ; x() } ; g() } ; h() }");
    }

    @Test
    public void TestSimpleBuiltins_testLookup_de6ad73e27aa42d2a1c28e095d312b61() {
        assertEval("{ h <- function() { z <- 3 ; x <- function() {3} ; g <- function() { x <- 1 ; assign(\"z\", 5) ; x() } ; g() } ; h() }");
    }

    @Test
    public void TestSimpleBuiltins_testLookup_6ba1690b3bf1582ad21a41b4aa7cade9() {
        assertEval("{ h <- function() { x <- function() {3} ; gg <- function() { assign(\"x\", 5) ; g <- function() { x() } ; g() } ; gg() } ; h() }");
    }

    @Test
    public void TestSimpleBuiltins_testLookup_ac90cce2345fd5bf30230331ed0d7db5() {
        assertEval("{ h <- function() { z <- 2 ; x <- function() {3} ; gg <- function() { assign(\"z\", 5) ; g <- function() { x() } ; g() } ; gg() } ; h() }");
    }

    @Test
    public void TestSimpleBuiltins_testLookup_003c1247fa6a427b0135d9e892c80f28() {
        assertEval("{ h <- function() { x <- function() {3} ; g <- function() { assign(\"x\", function() {4}) ; x() } ; g() } ; h() }");
    }

    @Test
    public void TestSimpleBuiltins_testLookup_111cf7d36f024022a02d162c42fc40bd() {
        assertEval("{ h <- function() { z <- 2 ; x <- function() {3} ; g <- function() { assign(\"z\", 1) ; x() } ; g() } ; h() }");
    }

    @Test
    public void TestSimpleBuiltins_testLookup_53723d83f879f9403ce15de376c091eb() {
        assertEval("{ x <- function() { 3 } ; h <- function() { if (FALSE) { x <- 2 } ;  z <- 2  ; g <- function() { assign(\"z\", 1) ; x() } ; g() } ; h() }");
    }

    @Test
    public void TestSimpleBuiltins_testLookup_d015a488b299b2257ba745357ad94008() {
        assertEval("{ x <- function() { 3 } ; h <- function() { g <- function() { f <- function() { x <- 1 ; x() } ; f() } ; g() } ; h() }");
    }

    @Test
    public void TestSimpleBuiltins_testLookup_d7019c82bbbda92b379335a1d2354896() {
        assertEval("{ h <- function() { myfunc <- function(i) { sum(i) } ; g <- function() { myfunc <- 2 ; f <- function() { myfunc(2) } ; f() } ; g() } ; h() }");
    }

    @Test
    public void TestSimpleBuiltins_testLookup_12172006720445a9282465095e950a98() {
        assertEval("{ x <- function() {11} ; g <- function() { f <- function() { assign(\"x\", 2) ; x() } ; f() } ; g() }");
    }

    @Test
    public void TestSimpleBuiltins_testLookup_a733251bb5b3310685899a2e93b53f4a() {
        assertEval("{ x <- function() {3} ; f <- function(i) { if (i == 1) { assign(\"x\", function() {4}) } ; function() { x() } } ; f1 <- f(1) ; f2 <- f(2) ; f1() }");
    }

    @Test
    public void TestSimpleBuiltins_testLookup_f3882a86c6251ac810356b60b0e51d56() {
        assertEval("{ x <- function() {3} ; f <- function(i) { if (i == 1) { assign(\"x\", function() {4}) } ; function() { x() } } ; f1 <- f(1) ; f2 <- f(2) ; f2() ; f1() }");
    }

    @Test
    public void TestSimpleBuiltins_testLookup_5282ed3905bf098d2a7e10398d20b473() {
        assertEval("{ x <- function() {3} ; f <- function(i) { if (i == 1) { assign(\"x\", function() {4}) } ; function() { x() } } ; f1 <- f(1) ; f2 <- f(2) ; f1() ; f2() }");
    }

    @Test
    public void TestSimpleBuiltins_testLookup_14f99f57844c42586b995c05bb9a1ea3() {
        assertEval("{ x <- 3 ; g <- function() { if (FALSE) { x <- 2 } ; f <- function() { h <- function() { x ; hh <- function() { x <<- 4 } ; hh() } ; h() } ; f() } ; g() ; x }");
    }

    @Test
    public void TestSimpleBuiltins_testLookup_786bcc90eafc890d1a4b6bed053ad73a() {
        assertEval("{ f <- function() { x <- 1 ; g <- function() { h <- function() { x <<- 2 } ; h() } ; g() ; x } ; f() }");
    }

    @Test
    public void TestSimpleBuiltins_testLookup_8b36fb11373ffcb6297193370232ae16() {
        assertEval("{ g <- function() { if (FALSE) { x <- 2 } ; f <- function() { assign(\"x\", 4) ; x <<- 3 } ; f() } ; g() ; x }");
    }

    @Test
    public void TestSimpleBuiltins_testLookup_7e7c1d01be25a13c8d5699c87a856b60() {
        assertEval("{ g <- function() { if (FALSE) { x <- 2 ; z <- 3 } ; h <- function() { if (FALSE) { x <- 1 } ; assign(\"z\", 10) ; f <- function() { assign(\"x\", 4) ; x <<- 3 } ; f() } ; h() } ; g() ; x }");
    }

    @Test
    public void TestSimpleBuiltins_testLookup_f0114d38bafb636ba6a61ca6429641fc() {
        assertEval("{ gg <- function() { assign(\"x\", 100) ; g <- function() { if (FALSE) { x <- 2 ; z <- 3 } ; h <- function() { if (FALSE) { x <- 1 } ; assign(\"z\", 10) ; f <- function() { assign(\"x\", 4) ; x <<- 3 } ; f() } ; h() } ; g() } ; x <- 10 ; gg() ; x }");
    }

    @Test
    public void TestSimpleBuiltins_testLookup_5426ebb4d5aeea5d12cbe2bdcf8e0dbe() {
        assertEval("{ gg <- function() { if (FALSE) { x <- 100 } ; g <- function() { if (FALSE) { x <- 100 } ; h <- function() { f <- function() { x <<- 3 } ; f() } ; h() } ; g() } ; x <- 10 ; gg() ; x }");
    }

    @Test
    public void TestSimpleBuiltins_testLookup_b831c18619a7befa072aec4032bd1dac() {
        assertEval("{ g <- function() { if (FALSE) { x <- 2 ; z <- 3 } ; h <- function() { assign(\"z\", 10) ; f <- function() { x <<- 3 } ; f() } ; h() } ; g() ; x }");
    }

    @Test
    public void TestSimpleBuiltins_testLookup_395e80de24e62b38db1e3d5060112adf() {
        assertEval("{ g <- function() { x <- 2 ; z <- 3 ; hh <- function() { assign(\"z\", 2) ; h <- function() { f <- function() { x <<- 3 } ; f() } ; h() } ; hh() } ; x <- 10 ; g() ; x }");
    }

    @Test
    public void TestSimpleBuiltins_testLookup_ffd862131767a28190fc8acc7a56c8ef() {
        assertEval("{ g <- function() { x <- 2 ; z <- 3 ; hh <- function() { assign(\"z\", 2) ; h <- function() { assign(\"x\", 1); f <- function() { x <<- 3 } ; f() } ; h() } ; hh() ; x } ; x <- 10 ; g() }");
    }

    @Test
    public void TestSimpleBuiltins_testLookup_550606f41244ab5b72ac0d403f76be03() {
        assertEval("{ x <- 3 ; f <- function(i) { if (i == 1) { assign(\"x\", 4) } ; function(v) { x <<- v} } ; f1 <- f(1) ; f2 <- f(2) ; f1(10) ; f2(11) ; x }");
    }

    @Test
    public void TestSimpleBuiltins_testLookup_6a487bd0ec6e3ba9cf00d0bfc3636608() {
        assertEval("{ x <- 3 ; f <- function(i) { if (i == 1) { assign(\"x\", 4) } ; function(v) { x <<- v} } ; f1 <- f(1) ; f2 <- f(2) ; f2(10) ; f1(11) ; x }");
    }

    @Test
    public void TestSimpleBuiltins_testLookup_2865497f22f615139b2590662d15a2cb() {
        assertEval("{ x <- 3 ; f <- function() { assign(\"x\", 4) ; h <- function(s=1) { if (s==2) { x <- 5 } ; x <<- 6 } ; h() ; get(\"x\") } ; f() }");
    }

    @Test
    public void TestSimpleBuiltins_testLookup_25848b87f12bed3b5c48ec094616f48b() {
        assertEval("{ x <- 3 ; f <- function() { assign(\"x\", 4) ; hh <- function() { if (FALSE) { x <- 100 } ; h <- function() { x <<- 6 } ; h() } ; hh() ; get(\"x\") } ; f() }");
    }

    @Test
    public void TestSimpleBuiltins_testLookup_fab0f3eded79eede99bca42c84977de5() {
        assertEval("{ assign(\"z\", 10, inherits=TRUE) ; z }");
    }

    @Test
    public void TestSimpleBuiltins_testLookup_48d8f6c2d2586ee471cfa0421ea5b4ae() {
        assertEval("{ exists(\"sum\") }");
    }

    @Test
    public void TestSimpleBuiltins_testLookup_2b36458da0b28ae1a46b3e3404e77bb2() {
        assertEval("{ exists(\"sum\", inherits = FALSE) }");
    }

    @Test
    public void TestSimpleBuiltins_testLookup_1b9568eed30d48488f00c0eef8b9866f() {
        assertEval("{ x <- 1; exists(\"x\", inherits = FALSE) }");
    }

    @Test
    public void TestSimpleBuiltins_testLookup_bf436a43a6caacd418317cd684d67d8e() {
        assertEvalAlt("{ f <- function() { assign(\"x\", 1) ; y <- 2 ; ls() } ; f() }");
    }

    @Test
    public void TestSimpleBuiltins_testLookup_23454b3e47dfe4aa28afa243683df359() {
        assertEvalAlt("{ f <- function() { x <- 1 ; y <- 2 ; ls() } ; f() }");
    }

    @Test
    public void TestSimpleBuiltins_testLookup_f5b6d5670ee3679c2214a7720ed4f5e9() {
        assertEvalAlt("{ f <- function() { assign(\"x\", 1) ; y <- 2 ; if (FALSE) { z <- 3 } ; ls() } ; f() }");
    }

    @Test
    public void TestSimpleBuiltins_testLookup_3f6070106221293533a2b2c1eab24f36() {
        assertEvalAlt("{ fu <- function() { uu <<- 23 } ; fu() ; ls(globalenv()) }");
    }

    @Test
    public void TestSimpleBuiltins_testLookup_d2b207ebaf757535786aa553fb1fe90a() {
        assertEvalError("{ x <- 2 ; rm(\"x\") ; get(\"x\") }");
    }

    @Test
    public void TestSimpleBuiltins_testLookup_72bcfb2ecee83734cf418b9ff1c34f8c() {
        assertEvalError("{ get(\"x\") }");
    }

    @Test
    public void TestSimpleBuiltins_testLookupIgnore_7e5d40be5a03aac06880b44eefa7d94b() {
        assertEval("{ f <- function(z) { exists(\"z\") } ; f(a) }");
    }

    @Test
    public void TestSimpleBuiltins_testLookupIgnore_7623faf4c356905dacd205a8b10eac15() {
        assertEval("{ g <- function() { assign(\"myfunc\", function(i) { sum(i) });  f <- function() { lapply(2, \"myfunc\") } ; f() } ; g() }");
    }

    @Test
    public void TestSimpleBuiltins_testLookupIgnore_2c371d6a6d4b74a871402788dbf16cf8() {
        assertEval("{ myfunc <- function(i) { sum(i) } ; g <- function() { assign(\"z\", 1);  f <- function() { lapply(2, \"myfunc\") } ; f() } ; g() }");
    }

    @Test
    public void TestSimpleBuiltins_testLookupIgnore_fc0e56627d1b08ab2d6c38875a68a1f0() {
        assertEval("{ g <- function() { f <- function() { assign(\"myfunc\", function(i) { sum(i) }); lapply(2, \"myfunc\") } ; f() } ; g() }");
    }

    @Test
    public void TestSimpleBuiltins_testLookupIgnore_2deae78feff592acd7d61159c8e39ea7() {
        assertEval("{ g <- function() { myfunc <- function(i) { i+i } ; f <- function() { lapply(2, \"myfunc\") } ; f() } ; g() }");
    }

    @Test
    public void TestSimpleBuiltins_testLowerTriangular_d0f253a7c6e1e06bb5cf39dbff9f01da() {
        assertEval("{ m <- matrix(1:6, nrow=2) ;  lower.tri(m, diag=TRUE) }");
    }

    @Test
    public void TestSimpleBuiltins_testLowerTriangular_0ba0d133686dd0481614017fbd5e5b41() {
        assertEval("{ m <- matrix(1:6, nrow=2) ;  lower.tri(m, diag=FALSE) }");
    }

    @Test
    public void TestSimpleBuiltins_testLowerTriangular_44ba325a12bc689011ed5350658dabb6() {
        assertEval("{ lower.tri(1:3, diag=TRUE) }");
    }

    @Test
    public void TestSimpleBuiltins_testLowerTriangular_29e7f74c119f3fd2dff006792c5fa9a1() {
        assertEval("{ lower.tri(1:3, diag=FALSE) }");
    }

    @Test
    public void TestSimpleBuiltins_testMatMult_c8d5c07bcbeefe8fbf58f325e98839ea() {
        assertEval("{ matrix(c(1,2,3,4), 2) %*% matrix(c(5,6,7,8), 2) }");
    }

    @Test
    public void TestSimpleBuiltins_testMatMult_bedda33e4997623d48c6fd5ea86d246b() {
        assertEval("{ matrix(c(3,1,2,0,1,2), 2) %*% matrix(c(1,0,4,2,1,0), 3) }");
    }

    @Test
    public void TestSimpleBuiltins_testMatMult_ec58a2e9592b539ca35489dc82886b98() {
        assertEval("{ c(1,2,3) %*% c(4,5,6) }");
    }

    @Test
    public void TestSimpleBuiltins_testMatMult_72c9ce920307ef19fc90624bd208d362() {
        assertEval("{ matrix(c(3,1,2,0,1,2),2) %*% c(1,0,4) }");
    }

    @Test
    public void TestSimpleBuiltins_testMatMult_65f058b7196ede745a341d316b0097a3() {
        assertEval("{ c(1,0,4) %*% matrix(c(3,1,2,0,1,2),3) }");
    }

    @Test
    public void TestSimpleBuiltins_testMatMult_54cb220edcef0e3e4aaea1e10c57a1e9() {
        assertEval("{ as.vector(c(1,2,3)) %*% t(as.vector(c(1,2))) }");
    }

    @Test
    public void TestSimpleBuiltins_testMatMultIgnore_e9ba9d6fa9abe7cec2ddbabcc73934ca() {
        assertEval("{ matrix(c(1+1i,2-2i,3+3i,4-4i), 2) %*% matrix(c(5+5i,6-6i,7+7i,8-8i), 2) }");
    }

    @Test
    public void TestSimpleBuiltins_testMatMultIgnore_864ab8a27b006789bddc33fbf08a681d() {
        assertEval("{ matrix(c(3+3i,1-1i,2+2i,0-0i,1+1i,2-2i), 2) %*% matrix(c(1+1i,0-0i,4+4i,2-2i,1+1i,0-0i), 3) }");
    }

    @Test
    public void TestSimpleBuiltins_testMatMultIgnore_2a28fb03fdd28d0687ea97640678c7c5() {
        assertEval("{ c(1+1i,2-2i,3+3i) %*% c(4-4i,5+5i,6-6i) }");
    }

    @Test
    public void TestSimpleBuiltins_testMatMultIgnore_500c9fe5e23232d488b23dea0ffe60e6() {
        assertEval("{ matrix(c(3+3i,1-1i,2+2i,0-0i,1+1i,2-2i),2) %*% c(1+1i,0-0i,4+4i) }");
    }

    @Test
    public void TestSimpleBuiltins_testMatMultIgnore_bd7d1161309480785b4881fa0f001408() {
        assertEval("{ c(1+1i,0-0i,4+4i) %*% matrix(c(3+3i,1-1i,2+2i,0-0i,1+1i,2-2i),3) }");
    }

    @Test
    public void TestSimpleBuiltins_testMatchFun_4dc251ff1db19e52f20841e136754b32() {
        assertEval("{ f <- match.fun(length) ; f(c(1,2,3)) }");
    }

    @Test
    public void TestSimpleBuiltins_testMatchFun_7b0c6121c55cb44b95fec61ca90379e6() {
        assertEval("{ f <- match.fun(\"length\") ; f(c(1,2,3)) }");
    }

    @Test
    public void TestSimpleBuiltins_testMatchFun_f0b1d6b8900072300d4205a1af315a5c() {
        assertEval("{ f <- function(x) { y <- match.fun(x) ; y(c(1,2,3)) } ; c(f(\"sum\"),f(\"cumsum\")) }");
    }

    @Test
    public void TestSimpleBuiltins_testMatchFun_b060469f6289b6abdddbe023b5919310() {
        assertEval("{ f <- function(x) { y <- match.fun(x) ; y(3,4) } ; c(f(\"+\"),f(\"*\")) }");
    }

    @Test
    public void TestSimpleBuiltins_testMatchIgnore_048ab83fbf746ab7b0de92f083754c50() {
        assertEval("{ match(2,c(1,2,3)) }");
    }

    @Test
    public void TestSimpleBuiltins_testMatchIgnore_4b9c00763f8d3b8f32effe9cf00561c6() {
        assertEval("{ match(c(1,2,3,4,5),c(1,2,1,2)) }");
    }

    @Test
    public void TestSimpleBuiltins_testMatchIgnore_939487ea836b5aac7a33fa6875c20339() {
        assertEval("{ match(\"hello\",c(\"I\", \"say\", \"hello\", \"world\")) }");
    }

    @Test
    public void TestSimpleBuiltins_testMatchIgnore_354af2561e4e24ce3b2b61b15e126ce8() {
        assertEval("{ match(c(\"hello\", \"say\"),c(\"I\", \"say\", \"hello\", \"world\")) }");
    }

    @Test
    public void TestSimpleBuiltins_testMatrix_161cd1f480d4e35750cd366a097e5dc9() {
        assertEval("{ matrix(c(1,2,3,4),2,2) }");
    }

    @Test
    public void TestSimpleBuiltins_testMatrix_d1e196f6c31ca607820acf1269711ac2() {
        assertEval("{ matrix(as.double(NA),2,2) }");
    }

    @Test
    public void TestSimpleBuiltins_testMatrix_7da256d3445817db926f36c119414bde() {
        assertEval("{ matrix(\"a\",10,10) }");
    }

    @Test
    public void TestSimpleBuiltins_testMatrix_1ce1dab3390cacefa841400b37b0d20c() {
        assertEval("{ matrix(c(\"a\",NA),10,10) }");
    }

    @Test
    public void TestSimpleBuiltins_testMatrix_e23499bab4d503ddb354e5bd66c22e08() {
        assertEval("{ matrix(1:4, nrow=2) }");
    }

    @Test
    public void TestSimpleBuiltins_testMatrix_94213eef23a62612694d36caa8a534f2() {
        assertEval("{ matrix(c(1,2,3,4), nrow=2) }");
    }

    @Test
    public void TestSimpleBuiltins_testMatrix_186bd3d23aff9a5a2f99c0085331c535() {
        assertEval("{ matrix(c(1+1i,2+2i,3+3i,4+4i),2) }");
    }

    @Test
    public void TestSimpleBuiltins_testMatrix_0eae53e6a046f7679e50f8660579fac4() {
        assertEval("{ matrix(nrow=2,ncol=2) }");
    }

    @Test
    public void TestSimpleBuiltins_testMatrix_f7e2a87a6677d6b7b701176c6c9e1036() {
        assertEval("{ matrix(1:4,2,2) }");
    }

    @Test
    public void TestSimpleBuiltins_testMatrix_a7247bc1f1726ae687962cfda709230e() {
        assertEval("{ matrix(1i,10,10) }");
    }

    @Test
    public void TestSimpleBuiltins_testMatrix_fa8d853982879fcc896086fe6addfb0f() {
        assertEval("{ matrix(c(1i,NA),10,10) }");
    }

    @Test
    public void TestSimpleBuiltins_testMatrix_9e26083c44caa7c52f4f651cad7b0af3() {
        assertEval("{ matrix(c(10+10i,5+5i,6+6i,20-20i),2) }");
    }

    @Test
    public void TestSimpleBuiltins_testMatrix_cc0dd296841e5af699ac9efbf0121ed3() {
        assertEval("{ matrix(c(1i,100i),10,10) }");
    }

    @Test
    public void TestSimpleBuiltins_testMatrixIgnore_f5dba0a59ab80b80d211e6e6fee198de() {
        assertEvalWarning("{ matrix(c(1,2,3,4),3,2) }");
    }

    @Test
    public void TestSimpleBuiltins_testMatrixIgnore_8daf811c43e5de9f9027463997632ce6() {
        assertEvalWarning("{ matrix(1:4,3,2) }");
    }

    @Test
    public void TestSimpleBuiltins_testMaximum_6bbb1dbae8964defa9b0e785d0ac9cd5() {
        assertEval("{ max((-1):100) }");
    }

    @Test
    public void TestSimpleBuiltins_testMaximum_d580679bd9053630b2db6a60f993be5e() {
        assertEval("{ max(2L, 4L) }");
    }

    @Test
    public void TestSimpleBuiltins_testMaximum_fad6482964d100a864f0587674fff019() {
        assertEval("{ max(1:10, 100:200, c(4.0, 5.0)) }");
    }

    @Test
    public void TestSimpleBuiltins_testMaximum_bc432220dfa79d23e7855e16bba7c766() {
        assertEvalWarning("{ max() }");
    }

    @Test
    public void TestSimpleBuiltins_testMaximumIgnore_9669c97c2ea4e1d1253ad005c5ca32c9() {
        assertEval("{ max(1:10, 100:200, c(4.0, 5.0), c(TRUE,FALSE,NA)) }");
    }

    @Test
    public void TestSimpleBuiltins_testMaximumIgnore_dc0861ab168a5dfb771bd75705f64484() {
        assertEval("{ max(c(\"hi\",\"abbey\",\"hello\")) }");
    }

    @Test
    public void TestSimpleBuiltins_testMaximumIgnore_05c8d18859b5c967c43445aa5d36985c() {
        assertEval("{ max(\"hi\",\"abbey\",\"hello\") }");
    }

    @Test
    public void TestSimpleBuiltins_testMean_e4e4a8783ee77120a7aa47998e394318() {
        assertEval("{ mean(c(5,5,5,5,5)) }");
    }

    @Test
    public void TestSimpleBuiltins_testMean_6dd208342193df93abc50817a746b98b() {
        assertEval("{ mean(c(1,2,3,4,5)) }");
    }

    @Test
    public void TestSimpleBuiltins_testMinimum_f34671823380d987d92590fce2e8011e() {
        assertEval("{ min((-1):100) }");
    }

    @Test
    public void TestSimpleBuiltins_testMinimum_8e3e0dab2b698155aac30656d1ba17d8() {
        assertEval("{ min(2L, 4L) }");
    }

    @Test
    public void TestSimpleBuiltins_testMinimum_05cb66d8b364489fa06362a12abd9749() {
        assertEval("{ min(c(1,2,0/0)) }");
    }

    @Test
    public void TestSimpleBuiltins_testMinimum_487ff8108e12a356f236d4c3ffedc067() {
        assertEval("{ max(c(1,2,0/0)) }");
    }

    @Test
    public void TestSimpleBuiltins_testMinimum_cafbdda915450ee379f661525d48e422() {
        assertEval("{ min(1:10, 100:200, c(4.0, -5.0)) }");
    }

    @Test
    public void TestSimpleBuiltins_testMinimum_2cc02ae7579e178d45b0fbe3c78005ec() {
        assertEvalWarning("{ min() }");
    }

    @Test
    public void TestSimpleBuiltins_testMinimumIgnore_439b91bb3000e058b9736056b15556a1() {
        assertEval("{ min(1:10, 100:200, c(4.0, 5.0), c(TRUE,FALSE,NA)) }");
    }

    @Test
    public void TestSimpleBuiltins_testMinimumIgnore_6ff5d3958c466ce8176bc44372e64494() {
        assertEval("{ min(c(\"hi\",\"abbey\",\"hello\")) }");
    }

    @Test
    public void TestSimpleBuiltins_testMinimumIgnore_c38784d03f763b9b7ba319a4e709ad53() {
        assertEval("{ min(\"hi\",\"abbey\",\"hello\") }");
    }

    @Test
    public void TestSimpleBuiltins_testMinimumIgnore_e8d15c4a706047697bad794ac2370a27() {
        assertEval("{ min(\"hi\",100) }");
    }

    @Test
    public void TestSimpleBuiltins_testMissing_0d49cd1806a44cd64801786e75fdacdb() {
        assertEval("{ f <- function(a) { g(a) } ;  g <- function(b) { missing(b) } ; f() }");
    }

    @Test
    public void TestSimpleBuiltins_testMissing_b4f6edfaffe32ea586f59a695242264f() {
        assertEval("{ f <- function(a = 2) { g(a) } ; g <- function(b) { missing(b) } ; f() }");
    }

    @Test
    public void TestSimpleBuiltins_testMissing_3d2cb98b5a49a1cd30375bff59a5a970() {
        assertEval("{ f <- function(a,b,c) { missing(b) } ; f(1,,2) }");
    }

    @Test
    public void TestSimpleBuiltins_testMissing_5358c8df917d174350d62aa79b336a60() {
        assertEval("{ g <- function(a, b, c) { b } ; f <- function(a,b,c) { g(a,b=2,c) } ; f(1,,2) }");
    }

    @Test
    public void TestSimpleBuiltins_testMissingIgnore_cf62337ead8caecb2e4db39b971a6823() {
        assertEval("{ f <- function(a = 2 + 3) { missing(a) } ; f() }");
    }

    @Test
    public void TestSimpleBuiltins_testMissingIgnore_e3ec4820900994d734d0199b41a505ab() {
        assertEval("{ f <- function(a = z) { missing(a) } ; f() }");
    }

    @Test
    public void TestSimpleBuiltins_testMissingIgnore_14a03fde115ece14b0e877fd4bf28ad0() {
        assertEval("{ f <- function(a = 2 + 3) { a;  missing(a) } ; f() }");
    }

    @Test
    public void TestSimpleBuiltins_testMissingIgnore_0da52004f0b9453ad6deab5e0b49a111() {
        assertEval("{ f <- function(a = z) {  g(a) } ; g <- function(b) { missing(b) } ; f() }");
    }

    @Test
    public void TestSimpleBuiltins_testMissingIgnore_3b1c18d77df4f57cd223b95c8c205d89() {
        assertEval("{ f <- function(a = z, z) {  g(a) } ; g <- function(b) { missing(b) } ; f() }");
    }

    @Test
    public void TestSimpleBuiltins_testMissingIgnore_12f97d45e336adc392898885f48afa76() {
        assertEval("{ f <- function(a) { g(a) } ; g <- function(b=2) { missing(b) } ; f() }");
    }

    @Test
    public void TestSimpleBuiltins_testMissingIgnore_a96249c626958ddb13c95b4628e7f318() {
        assertEval("{ f <- function(x = y, y = x) { g(x, y) } ; g <- function(x, y) { missing(x) } ; f() }");
    }

    @Test
    public void TestSimpleBuiltins_testMissingIgnore_fc5302d7e40c71c48b09f7e6fcf1df6d() {
        assertEval("{ f <- function(x) { missing(x) } ; f(a) }");
    }

    @Test
    public void TestSimpleBuiltins_testMissingIgnore_20756d2c3aaa3afd4ad6f87416f461ea() {
        assertEval("{ f <- function(a) { g <- function(b) { before <- missing(b) ; a <<- 2 ; after <- missing(b) ; c(before, after) } ; g(a) } ; f() }");
    }

    @Test
    public void TestSimpleBuiltins_testMissingIgnore_2c7389435b7285c22a1e276db60a1c8e() {
        assertEval("{ f <- function(...) { g(...) } ;  g <- function(b=2) { missing(b) } ; f() }");
    }

    @Test
    public void TestSimpleBuiltins_testMissingIgnore_7a7476796aa855e4eef288a9fa74b80f() {
        assertEval("{ f <- function(...) { missing(..2) } ; f(x + z, a * b) }");
    }

    @Test
    public void TestSimpleBuiltins_testMod_9e8ae1303f27834ae87665ed2c4ae12c() {
        assertEval("{ round(Mod(1+1i)*10000) }");
    }

    @Test
    public void TestSimpleBuiltins_testNChar_495a7fd2d648682e65ccfb8cf57e8805() {
        assertEval("{ nchar(c(\"hello\", \"hi\")) }");
    }

    @Test
    public void TestSimpleBuiltins_testNChar_1193ed9b71174082c92be71c70d698e0() {
        assertEval("{ nchar(c(\"hello\", \"hi\", 10, 130)) }");
    }

    @Test
    public void TestSimpleBuiltins_testNChar_2b2b116c81ddc47069b1a784ec550a25() {
        assertEval("{ nchar(c(10,130)) }");
    }

    @Test
    public void TestSimpleBuiltins_testNextMethod_4a05151b190f59933e5693d3e3fba9f1() {
        assertEval("{g<-function(){ x<-1; class(x)<-c(\"a\",\"b\",\"c\"); f<-function(x){UseMethod(\"f\")}; f.a<-function(x){cat(\"a\");NextMethod(\"f\",x)}; f.b<-function(x){cat(\"b\")}; f(x); }; g();}");
    }

    @Test
    public void TestSimpleBuiltins_testOperators_d4e3be6301b8298150f0b9769e6d59f0() {
        assertEval("{ `+`(1,2) }");
    }

    @Test
    public void TestSimpleBuiltins_testOperators_29fad4bdb25ec4ad05904614767be93a() {
        assertEval("{ `-`(1,2) }");
    }

    @Test
    public void TestSimpleBuiltins_testOperators_b7bb9b7e55f90000cde0e368058599c6() {
        assertEval("{ `*`(1,2) }");
    }

    @Test
    public void TestSimpleBuiltins_testOperators_9b2c8c03180166b9b962e8644e793655() {
        assertEval("{ `/`(1,2) }");
    }

    @Test
    public void TestSimpleBuiltins_testOperators_bb752e04b1a583c8690124ab6b074151() {
        assertEval("{ `%/%`(1,2) }");
    }

    @Test
    public void TestSimpleBuiltins_testOperators_f98b8748159a426ff7f60fa6e3e3e0ec() {
        assertEval("{ `%%`(1,2) }");
    }

    @Test
    public void TestSimpleBuiltins_testOperators_c192dfdc78da49170c40df3c430cab0e() {
        assertEval("{ `^`(1,2) }");
    }

    @Test
    public void TestSimpleBuiltins_testOperators_d525ac6f3b3c989891117eb4b1915b5b() {
        assertEval("{ `!`(TRUE) }");
    }

    @Test
    public void TestSimpleBuiltins_testOperators_f8ce660041adff1e80a0dd4f3f04ee25() {
        assertEval("{ `%o%`(3,5) }");
    }

    @Test
    public void TestSimpleBuiltins_testOperators_109142944819af0e25f3edd907f8bfd2() {
        assertEval("{ x <- `+` ; x(2,3) }");
    }

    @Test
    public void TestSimpleBuiltins_testOperators_6fccbf01084abe36fe62eb4661f555f6() {
        assertEval("{ x <- `+` ; f <- function() { x <- 1 ; x(2,3) } ; f() }");
    }

    @Test
    public void TestSimpleBuiltins_testOperators_7780439e622de4f4c21350b3490528f0() {
        assertEval("{ `||`(TRUE, FALSE) }");
    }

    @Test
    public void TestSimpleBuiltins_testOperators_7bcce4c1c2000a4ea4f3d20bd208ef75() {
        assertEval("{ `&&`(TRUE, FALSE) }");
    }

    @Test
    public void TestSimpleBuiltins_testOperators_924edb8accf499458ef16a93acd8ecc6() {
        assertEval("{ `|`(TRUE, FALSE) }");
    }

    @Test
    public void TestSimpleBuiltins_testOperators_852f1f1b4259b28b146c661cadb6dbaf() {
        assertEval("{ `&`(TRUE, FALSE) }");
    }

    @Test
    public void TestSimpleBuiltins_testOperatorsIgnore_dd8820aada824b55da8fce1b2069a4a8() {
        assertEval("{ `%*%`(3,5) }");
    }

    @Test
    public void TestSimpleBuiltins_testOrder_b3e9fa38e57708b98e592be54724d7ef() {
        assertEval("{ order(c(\"a\",\"c\",\"b\",\"d\",\"e\",\"f\")) }");
    }

    @Test
    public void TestSimpleBuiltins_testOrder_862b6133d7b052ce64cfad4a4a67e84e() {
        assertEval("{ order(c(5,2,2,1,7,4)) }");
    }

    @Test
    public void TestSimpleBuiltins_testOrder_f59e8d989847e099cd830b09c1811eb1() {
        assertEval("{ order(c(5,2,2,1,7,4),c(\"a\",\"c\",\"b\",\"d\",\"e\",\"f\")) }");
    }

    @Test
    public void TestSimpleBuiltins_testOrder_852a49414fd348ee2ac43d573ec918d0() {
        assertEval("{ order(c(1,1,1,1),c(4,3,2,1)) }");
    }

    @Test
    public void TestSimpleBuiltins_testOrder_1876c32edccfa36eaa58c7526d847258() {
        assertEval("{ order(c(1,1,1,1),c(\"d\",\"c\",\"b\",\"a\")) }");
    }

    @Test
    public void TestSimpleBuiltins_testOrderIgnore_9d9e462e8a8cc7dbbf92366b9602bf39() {
        assertEval("{ order(1:3) }");
    }

    @Test
    public void TestSimpleBuiltins_testOrderIgnore_ea195becea5e63c0bc6efd17b21ed503() {
        assertEval("{ order(3:1) }");
    }

    @Test
    public void TestSimpleBuiltins_testOrderIgnore_05f671b27b0512bcbf1a2e113be7890a() {
        assertEval("{ order(c(1,1,1), 3:1) }");
    }

    @Test
    public void TestSimpleBuiltins_testOrderIgnore_00ba7b7a2cb7b8ec3054739ef0c56f0e() {
        assertEval("{ order(c(1,1,1), 3:1, decreasing=FALSE) }");
    }

    @Test
    public void TestSimpleBuiltins_testOrderIgnore_0aee4193d1ed56df561c1905296ddca9() {
        assertEval("{ order(c(1,1,1), 3:1, decreasing=TRUE, na.last=TRUE) }");
    }

    @Test
    public void TestSimpleBuiltins_testOrderIgnore_87d0b85ae402b237e6eea7524e6ebfe2() {
        assertEval("{ order(c(1,1,1), 3:1, decreasing=TRUE, na.last=NA) }");
    }

    @Test
    public void TestSimpleBuiltins_testOrderIgnore_63382528759189343899c7eaad048f33() {
        assertEval("{ order(c(1,1,1), 3:1, decreasing=TRUE, na.last=FALSE) }");
    }

    @Test
    public void TestSimpleBuiltins_testOrderIgnore_8b055d570492191af8f8acd6bca6b6ad() {
        assertEval("{ order() }");
    }

    @Test
    public void TestSimpleBuiltins_testOrderIgnore_233b9224709438d6239a02a3cbca1d6f() {
        assertEval("{ order(c(NA,NA,1), c(2,1,3)) }");
    }

    @Test
    public void TestSimpleBuiltins_testOrderIgnore_0d31ec524c63c01a9d36ce580dd87b76() {
        assertEval("{ order(c(NA,NA,1), c(1,2,3)) }");
    }

    @Test
    public void TestSimpleBuiltins_testOrderIgnore_4b6ee44c315ce2abafeeff55be3bda6a() {
        assertEval("{ order(c(1,2,3,NA)) }");
    }

    @Test
    public void TestSimpleBuiltins_testOrderIgnore_e08023e645a2200f800f52383def050b() {
        assertEval("{ order(c(1,2,3,NA), na.last=FALSE) }");
    }

    @Test
    public void TestSimpleBuiltins_testOrderIgnore_5b504932f266176135d80d1de4c180a6() {
        assertEval("{ order(c(1,2,3,NA), na.last=FALSE, decreasing=TRUE) }");
    }

    @Test
    public void TestSimpleBuiltins_testOrderIgnore_1b4cf21da630e25cd59c951bbff7a050() {
        assertEval("{ order(c(0/0, -1/0, 2)) }");
    }

    @Test
    public void TestSimpleBuiltins_testOrderIgnore_e63709ad10dd0c536abd53f59d2cfdf8() {
        assertEval("{ order(c(0/0, -1/0, 2), na.last=NA) }");
    }

    @Test
    public void TestSimpleBuiltins_testOther_9e779b291a3f7b361680c2b1177496d2() {
        assertEval("{ rev.mine <- function(x) { if (length(x)) x[length(x):1L] else x } ; rev.mine(1:3) }");
    }

    @Test
    public void TestSimpleBuiltins_testOuter_f0494514b9b654d66482f6b88889f616() {
        assertEval("{ outer(c(1,2,3),c(1,2),\"-\") }");
    }

    @Test
    public void TestSimpleBuiltins_testOuter_7bf5e09a3cc82b43764d2fb0d857765b() {
        assertEval("{ outer(c(1,2,3),c(1,2),\"*\") }");
    }

    @Test
    public void TestSimpleBuiltins_testOuter_a3cacf25df5ce992f10a406a31e690f2() {
        assertEval("{ outer(1, 3, \"-\") }");
    }

    @Test
    public void TestSimpleBuiltins_testOuterIgnore_da963cbde1784128a50d0bb2220f4a09() {
        assertEval("{ foo <- function (x,y) { x + y * 1i } ; outer(3,3,foo) }");
    }

    @Test
    public void TestSimpleBuiltins_testOuterIgnore_fa7bab756255d002e9b280b544ccabdb() {
        assertEval("{ foo <- function (x,y) { x + y * 1i } ; outer(3,3,\"foo\") }");
    }

    @Test
    public void TestSimpleBuiltins_testOuterIgnore_4a115174070896c785016a9d9d5d665e() {
        assertEval("{ foo <- function (x,y) { x + y * 1i } ; outer(1:3,1:3,foo) }");
    }

    @Test
    public void TestSimpleBuiltins_testOuterIgnore_e5c558a0c7a7981c18d26924fb310194() {
        assertEval("{ outer(c(1,2,3),c(1,2),\"+\") }");
    }

    @Test
    public void TestSimpleBuiltins_testOuterIgnore_5182ad090b959b44000d6c63b2bf223b() {
        assertEval("{ outer(1:3,1:2) }");
    }

    @Test
    public void TestSimpleBuiltins_testOuterIgnore_2d96437a7e8bbf4c84f39c87f3822203() {
        assertEval("{ outer(1:3,1:2,\"*\") }");
    }

    @Test
    public void TestSimpleBuiltins_testOuterIgnore_6dc2cca210d082a9eafba79e161f0d8f() {
        assertEval("{ outer(1:3,1:2, function(x,y,z) { x*y*z }, 10) }");
    }

    @Test
    public void TestSimpleBuiltins_testOuterIgnore_9eece79caddd6ebf500a83a675d56b84() {
        assertEval("{ outer(1:2, 1:3, \"<\") }");
    }

    @Test
    public void TestSimpleBuiltins_testOuterIgnore_9260b477fa0c0eacb1851e4c1227c63d() {
        assertEval("{ outer(1:2, 1:3, '<') }");
    }

    @Test
    public void TestSimpleBuiltins_testOverride_de3b22c9cf36f0c894eb0b6d14a77142() {
        assertEval("{ sub <- function(x,y) { x - y }; sub(10,5) }");
    }

    @Test
    public void TestSimpleBuiltins_testParen_499acebd19ac76555ed92ca7ecc3ec53() {
        assertEval("{ a = array(1,c(3,3,3)); (a[1,2,3] = 3) }");
    }

    @Test
    public void TestSimpleBuiltins_testPaste_23188ccb2641c8c48c9812a88fe004f5() {
        assertEval("{ paste() }");
    }

    @Test
    public void TestSimpleBuiltins_testPaste_c2d17803bc72097f3a44e9d017be92ba() {
        assertEval("{ a <- as.raw(200) ; b <- as.raw(255) ; paste(a, b) }");
    }

    @Test
    public void TestSimpleBuiltins_testPaste_fa650154a20cffdacccbb8ba12d16452() {
        assertEval("{ paste(character(0),31415) }");
    }

    @Test
    public void TestSimpleBuiltins_testPaste_52b59d2a84691d7cad337a574eaf5d67() {
        assertEval("{ paste(1:2, 1:3, FALSE, collapse=NULL) }");
    }

    @Test
    public void TestSimpleBuiltins_testPaste_8aadd2296b0752c162a22f4f975cfafc() {
        assertEval("{ paste(sep=\"\") }");
    }

    @Test
    public void TestSimpleBuiltins_testPaste_b55d294d8dba8cde76019bf8f4828089() {
        assertEval("{ paste(1:2, 1:3, FALSE, collapse=\"-\", sep=\"+\") }");
    }

    @Test
    public void TestSimpleBuiltins_testPasteIgnore_1a3c1e77838670e434c0da99950c8e2c() {
        assertEval("{ file.path(\"a\", \"b\", c(\"d\",\"e\",\"f\")) }");
    }

    @Test
    public void TestSimpleBuiltins_testPasteIgnore_3408303a6c99992f74f43cb72bc7fa75() {
        assertEval("{ file.path() }");
    }

    @Test
    public void TestSimpleBuiltins_testPrint_71b73b9cd97a190f54e9c03ce59b3097() {
        assertEval("{ print(23) }");
    }

    @Test
    public void TestSimpleBuiltins_testPrint_92dcccba34d298ef77db9808594dbd93() {
        assertEval("{ print(1:3) }");
    }

    @Test
    public void TestSimpleBuiltins_testPrint_b08e619361c9310ae1cede898a80271f() {
        assertEval("{ print(list(1,2,3)) }");
    }

    @Test
    public void TestSimpleBuiltins_testPrint_f4c5b564c0eda175d1cd7d16025360ce() {
        assertEval("{ x<-c(1,2); names(x)=c(\"a\", \"b\"); print(x) }");
    }

    @Test
    public void TestSimpleBuiltins_testPrint_b69b07c7129a8041106e16c0e463c6d8() {
        assertEval("{ x<-c(1, 2:20, 21); n<-\"a\"; n[21]=\"b\"; names(x)<-n; print(x) }");
    }

    @Test
    public void TestSimpleBuiltins_testPrint_5456e88b28ca441eb52dcdf4f8e0f874() {
        assertEval("{ x<-c(10000000, 10000:10007, 21000000); n<-\"a\"; n[10]=\"b\"; names(x)<-n; print(x) }");
    }

    @Test
    public void TestSimpleBuiltins_testPrint_1dafd0f376cc69880535873ba3831021() {
        assertEval("{ x<-c(\"11\", \"7\", \"2222\", \"7\", \"33\"); print(x) }");
    }

    @Test
    public void TestSimpleBuiltins_testPrint_f0b5d548698b1fb89e8f57a05648e10c() {
        assertEval("{  x<-c(11, 7, 2222, 7, 33); print(x) }");
    }

    @Test
    public void TestSimpleBuiltins_testPrint_3ccb402bb4f18ea3cb9224769d3f148a() {
        assertEval("{ x<-c(\"11\", \"7\", \"2222\", \"7\", \"33\"); names(x)<-1:5; print(x) }");
    }

    @Test
    public void TestSimpleBuiltins_testPrint_fd0bd86c034af13a512b66088cd5cb7f() {
        assertEval("{ x<-c(11, 7, 2222, 7, 33); names(x)<-1:5; print(x) }");
    }

    @Test
    public void TestSimpleBuiltins_testPrint_cb4a109b4038ad092aed2a2dce1f9da8() {
        assertEval("{ print(list(list(list(1,2),list(3)),list(list(4),list(5,6)))) }");
    }

    @Test
    public void TestSimpleBuiltins_testPrint_6dbe8644c3dd5d7408b5253e71989a62() {
        assertEval("{ print(c(1.1,2.34567)) }");
    }

    @Test
    public void TestSimpleBuiltins_testPrint_7fca30871c0b1c3962edf36aca9a78fe() {
        assertEval("{ print(c(1,2.34567)) }");
    }

    @Test
    public void TestSimpleBuiltins_testPrint_271e323df38614fdda33518f1d19b587() {
        assertEval("{ print(c(11.1,2.34567)) }");
    }

    @Test
    public void TestSimpleBuiltins_testQr_4c61546a62c6441af95effa50e76e062() {
        assertEval(" { x <- qr(cbind(1:10,2:11), LAPACK=TRUE) ; round( qr.coef(x, 1:10), digits=5 ) }");
    }

    @Test
    public void TestSimpleBuiltins_testQr_abe3bd72b9a9dc9279dace1511a3fac8() {
        assertEval("{ qr(10, LAPACK=TRUE) }");
    }

    @Test
    public void TestSimpleBuiltins_testQr_2e522fbe7c114da3cb368c5f7274cf12() {
        assertEval("{ round( qr(matrix(1:6,nrow=2), LAPACK=TRUE)$qr, digits=5) }");
    }

    @Test
    public void TestSimpleBuiltins_testQr_98faff4fc32371fae174496695a3a35b() {
        assertEval("{ qr(matrix(1:6,nrow=2), LAPACK=FALSE)$pivot }");
    }

    @Test
    public void TestSimpleBuiltins_testQr_251a3e0804d5e1ec4ba95cabe5851fea() {
        assertEval("{ qr(matrix(1:6,nrow=2), LAPACK=FALSE)$rank }");
    }

    @Test
    public void TestSimpleBuiltins_testQr_17317e26aa90dc9f21710b9567daa0c0() {
        assertEval("{ round( qr(matrix(1:6,nrow=2), LAPACK=FALSE)$qraux, digits=5 ) }");
    }

    @Test
    public void TestSimpleBuiltins_testQr_637b00e95199b0d802bfd6e4c98184a6() {
        assertEval("{ round( qr(matrix(c(3,2,-3,-4),nrow=2), LAPACK=FALSE)$qr, digits=5 ) }");
    }

    @Test
    public void TestSimpleBuiltins_testQr_0cb51ae181bc178bf223d49001723552() {
        assertEval("{ x <- qr(t(cbind(1:10,2:11)), LAPACK=TRUE) ; qr.coef(x, 1:2) }");
    }

    @Test
    public void TestSimpleBuiltins_testQr_eb7d6b94998592915901ccdc876f3e5e() {
        assertEval("{ x <- qr(c(3,1,2), LAPACK=TRUE) ; round( qr.coef(x, c(1,3,2)), digits=5 ) }");
    }

    @Test
    public void TestSimpleBuiltins_testQr_b833c099f54f44df2488c463c5977c69() {
        assertEval("{ x <- qr(t(cbind(1:10,2:11)), LAPACK=FALSE) ; qr.coef(x, 1:2) }");
    }

    @Test
    public void TestSimpleBuiltins_testQr_faacf8177a822d44074aa43fd81139d5() {
        assertEval("{ x <- qr(c(3,1,2), LAPACK=FALSE) ; round( qr.coef(x, c(1,3,2)), digits=5 ) }");
    }

    @Test
    public void TestSimpleBuiltins_testQr_17cad1cc6779f137acbded3e743990f8() {
        assertEval("{ m <- matrix(c(1,0,0,0,1,0,0,0,1),nrow=3) ; x <- qr(m, LAPACK=FALSE) ; qr.coef(x, 1:3) }");
    }

    @Test
    public void TestSimpleBuiltins_testQr_1cd6ad9cf8d11508e422eae128c0fa58() {
        assertEval("{ x <- qr(cbind(1:3,2:4), LAPACK=FALSE) ; round( qr.coef(x, 1:3), digits=5 ) }");
    }

    @Test
    public void TestSimpleBuiltins_testQr_e2d68b4592f13f68f031a68d95f80d75() {
        assertEval("{ round( qr.solve(qr(c(1,3,4,2)), c(1,2,3,4)), digits=5 ) }");
    }

    @Test
    public void TestSimpleBuiltins_testQr_46728c69e8381944b3e3b0272b971935() {
        assertEval("{ round( qr.solve(c(1,3,4,2), c(1,2,3,4)), digits=5) }");
    }

    @Test
    public void TestSimpleBuiltins_testQr_cb5a4156797fb35468b2a52c03675858() {
        assertEvalError("{ x <- qr(cbind(1:10,2:11), LAPACK=TRUE) ; qr.coef(x, 1:2) }");
    }

    @Test
    public void TestSimpleBuiltins_testQuote_b0c9c56afaa693b70b7fb241f261ccdf() {
        assertEval("{ quote(1:3) }");
    }

    @Test
    public void TestSimpleBuiltins_testQuote_8b82ba407a1eb6062c2565daa9557474() {
        assertEval("{ quote(list(1,2)) }");
    }

    @Test
    public void TestSimpleBuiltins_testQuote_339002b066f4349faeef982ea5860293() {
        assertEval("{ typeof(quote(1)) }");
    }

    @Test
    public void TestSimpleBuiltins_testQuote_d8393f64864243ce76e46a2bb07637b2() {
        assertEval("{ typeof(quote(x + y)) }");
    }

    @Test
    public void TestSimpleBuiltins_testQuote_0b4f01ed9d7275da794434ee3b6f8d45() {
        assertEval("{ quote(x <- x + 1) }");
    }

    @Test
    public void TestSimpleBuiltins_testQuote_0ce6b058e6a459207f7154ded3d856cb() {
        assertEval("{ typeof(quote(x)) }");
    }

    @Test
    public void TestSimpleBuiltins_testQuote_2ce345e0f74c01976ac35948bfab5a71() {
        assertEval("{ l <- quote(x[1,1] <- 10) ; f <- function() { eval(l) } ; x <- matrix(1:4,nrow=2) ; f() ; x }");
    }

    @Test
    public void TestSimpleBuiltins_testQuote_409341bfbb82606d75eb0c1700c98952() {
        assertEval("{ l <- quote(x[1] <- 1) ; f <- function() { eval(l) } ; x <- 10 ; f() ; x }");
    }

    @Test
    public void TestSimpleBuiltins_testQuote_b8cacd46656e5a810809ba31bd8af586() {
        assertEval("{ l <- quote(x[1] <- 1) ; f <- function() { eval(l) ; x <<- 10 ; get(\"x\") } ; x <- 20 ; f() }");
    }

    @Test
    public void TestSimpleBuiltins_testQuote_e61120d917bf2901af3855b76706bcf1() {
        assertEvalError("{ l <- quote(a[3] <- 4) ; f <- function() { eval(l) } ; f() }");
    }

    @Test
    public void TestSimpleBuiltins_testQuote_d285d13bbf9697578c2b60d4e8305cdd() {
        assertEvalError("{ l <- quote(a[3] <- 4) ; eval(l) ; f() }");
    }

    @Test
    public void TestSimpleBuiltins_testRandom_d3441d3fabd779f2fa970e3cd1c9072f() {
        assertEval("{ round( rnorm(3), digits = 5 ) }");
    }

    @Test
    public void TestSimpleBuiltins_testRandom_563ca05e17aa93f60a3c0b558ac50057() {
        assertEval("{ round( rnorm(3,1000,10), digits = 5 ) }");
    }

    @Test
    public void TestSimpleBuiltins_testRandom_784f02d69de0bfc6b26f80cc27b3eaf0() {
        assertEval("{ round( rnorm(3,c(1000,2,3),c(10,11)), digits = 5 ) }");
    }

    @Test
    public void TestSimpleBuiltins_testRandom_b2e35c06b054d504b83a29fdc0f2c77a() {
        assertEval("{ round( runif(3), digits = 5 ) }");
    }

    @Test
    public void TestSimpleBuiltins_testRandom_38f6214fa41def07b060c01b29004277() {
        assertEval("{ round( runif(3,1,10), digits = 5 ) }");
    }

    @Test
    public void TestSimpleBuiltins_testRandom_f1a576fe16d8967d5d94472745eb8757() {
        assertEval("{ round( runif(3,1:3,3:2), digits = 5 ) }");
    }

    @Test
    public void TestSimpleBuiltins_testRandom_b1cb39289a32d016a5e4d8fd0369a06b() {
        assertEval("{ round( rgamma(3,1), digits = 5 ) }");
    }

    @Test
    public void TestSimpleBuiltins_testRandom_98b47b95df69a17bd9bfaf2a24c9cffd() {
        assertEval("{ round( rgamma(3,0.5,scale=1:3), digits = 5 ) }");
    }

    @Test
    public void TestSimpleBuiltins_testRandom_fd28dcd349e0cca475812e380ef658bf() {
        assertEval("{ round( rgamma(3,0.5,rate=1:3), digits = 5 ) }");
    }

    @Test
    public void TestSimpleBuiltins_testRandom_e0ebcb975feabfb978612a64a771116e() {
        assertEval("{ round( rbinom(3,3,0.9), digits = 5 ) }");
    }

    @Test
    public void TestSimpleBuiltins_testRandom_8c7daa50068479e536d478513c940605() {
        assertEval("{ round( rbinom(3,10,(1:5)/5), digits = 5 ) }");
    }

    @Test
    public void TestSimpleBuiltins_testRandom_7d00e32e71b1e734a6bf82d8e5ad1e59() {
        assertEval("{ round( rlnorm(3), digits = 5 ) }");
    }

    @Test
    public void TestSimpleBuiltins_testRandom_b35e5af9e87e8a17b87bad6537a48322() {
        assertEval("{ round( rlnorm(3,sdlog=c(10,3,0.5)), digits = 5 ) }");
    }

    @Test
    public void TestSimpleBuiltins_testRandom_9e1f8a6e4a70c5688947e9205b449a9e() {
        assertEval("{ round( rcauchy(3), digits = 5 ) }");
    }

    @Test
    public void TestSimpleBuiltins_testRandom_df5e70f5779809e68123bd1f1474d2de() {
        assertEval("{ round( rcauchy(3, scale=4, location=1:3), digits = 5 ) }");
    }

    @Test
    public void TestSimpleBuiltins_testRank_ac4677bb60d34cb54b0855ff9af216fe() {
        assertEval("{ rank(c(10,100,100,1000)) }");
    }

    @Test
    public void TestSimpleBuiltins_testRank_b661e996c8bab94a49a1b912170e269c() {
        assertEval("{ rank(c(1000,100,100,100, 10)) }");
    }

    @Test
    public void TestSimpleBuiltins_testRank_8119ffd7c473890dd5a8fb4bb4eb27dd() {
        assertEval("{ rank(c(a=2,b=1,c=3,40)) }");
    }

    @Test
    public void TestSimpleBuiltins_testRank_79652345882c62a61705a5fc72b80f6c() {
        assertEval("{ rank(c(a=2,b=1,c=3,d=NA,e=40), na.last=NA) }");
    }

    @Test
    public void TestSimpleBuiltins_testRank_d933b8b9599a925bdbfc61565085f049() {
        assertEval("{ rank(c(a=2,b=1,c=3,d=NA,e=40), na.last=\"keep\") }");
    }

    @Test
    public void TestSimpleBuiltins_testRank_920ad82c4f789e0f160e9bec2592a796() {
        assertEval("{ rank(c(a=2,b=1,c=3,d=NA,e=40), na.last=TRUE) }");
    }

    @Test
    public void TestSimpleBuiltins_testRank_71d5d62deb1ac8f050666be28cc69770() {
        assertEval("{ rank(c(a=2,b=1,c=3,d=NA,e=40), na.last=FALSE) }");
    }

    @Test
    public void TestSimpleBuiltins_testRank_25cc554304f5043b71318c6e7db78796() {
        assertEval("{ rank(c(a=1,b=1,c=3,d=NA,e=3), na.last=FALSE, ties.method=\"max\") }");
    }

    @Test
    public void TestSimpleBuiltins_testRank_71c5bf762cec2ebaac51f86364fad786() {
        assertEval("{ rank(c(a=1,b=1,c=3,d=NA,e=3), na.last=NA, ties.method=\"min\") }");
    }

    @Test
    public void TestSimpleBuiltins_testRank_4b9cea01de60a8694a6b5606f91cf6e5() {
        assertEval("{ rank(c(1000, 100, 100, NA, 1, 20), ties.method=\"first\") }");
    }

    @Test
    public void TestSimpleBuiltins_testRbind_903e21f7160e1b75b925c546a71f2382() {
        assertEval("{ rbind(1.1:3.3,1.1:3.3) }");
    }

    @Test
    public void TestSimpleBuiltins_testRbind_9db2e0a7ec8f0f38c9fc70e39ba19ea1() {
        assertEval("{ rbind() }");
    }

    @Test
    public void TestSimpleBuiltins_testRbind_877ba8a0c82ea0e1806b1b3a8989a6a6() {
        assertEval("{ rbind(1:3,2) }");
    }

    @Test
    public void TestSimpleBuiltins_testRbind_49055fb7dacb9a5ce0cca004d1b2c7cb() {
        assertEval("{ rbind(1:3,1:3) }");
    }

    @Test
    public void TestSimpleBuiltins_testRbind_638277b823e65af7e856ecf66594c63e() {
        assertEval("{ m <- matrix(1:6, ncol=2) ; rbind(m, 11:12) }");
    }

    @Test
    public void TestSimpleBuiltins_testRbind_ad842ae6e75484f537190b0005164a2c() {
        assertEval("{ m <- matrix(1:6, ncol=2) ; rbind(11:12, m) }");
    }

    @Test
    public void TestSimpleBuiltins_testRbindIgnore_be158803468f8099cec173e61a9c21e2() {
        assertEval("{ m <- matrix(1:6, nrow=2) ; rbind(11:12, m) }");
    }

    @Test
    public void TestSimpleBuiltins_testReIm_95c6a5bfa6a3c21a1c0b96172569dae5() {
        assertEval("{ Re(1+1i) }");
    }

    @Test
    public void TestSimpleBuiltins_testReIm_9ae67abaa867a65f6296a0c6492969f1() {
        assertEval("{ Im(1+1i) }");
    }

    @Test
    public void TestSimpleBuiltins_testReIm_37988dec2f27d6109524ff49342aafb4() {
        assertEval("{ Re(1) }");
    }

    @Test
    public void TestSimpleBuiltins_testReIm_74bde8d3265cea27201f771a32aa6820() {
        assertEval("{ Im(1) }");
    }

    @Test
    public void TestSimpleBuiltins_testReIm_b51e9bf13edb27292ec723545da8014c() {
        assertEval("{ Re(c(1+1i,2-2i)) }");
    }

    @Test
    public void TestSimpleBuiltins_testReIm_695e20e4675aae861985bf62be5c3dc2() {
        assertEval("{ Im(c(1+1i,2-2i)) }");
    }

    @Test
    public void TestSimpleBuiltins_testReIm_710eccf642f88fe5b36e243eb0f50bf7() {
        assertEval("{ Re(c(1,2)) }");
    }

    @Test
    public void TestSimpleBuiltins_testReIm_b4fc1b0b3a0a6825795dc057bdeb88d2() {
        assertEval("{ Im(c(1,2)) }");
    }

    @Test
    public void TestSimpleBuiltins_testReIm_0b578f8709fd8650c76e4e966bfcf875() {
        assertEval("{ Re(as.double(NA)) }");
    }

    @Test
    public void TestSimpleBuiltins_testReIm_c471bf142d67a893551ed73c5005c67e() {
        assertEval("{ Im(as.double(NA)) }");
    }

    @Test
    public void TestSimpleBuiltins_testReIm_52ea9dd247c8e1ffe482ae528e37dc4f() {
        assertEval("{ Re(c(1,NA,2)) }");
    }

    @Test
    public void TestSimpleBuiltins_testReIm_4f1f76d4ead8a97e2f5b391916ae7863() {
        assertEval("{ Im(c(1,NA,2)) }");
    }

    @Test
    public void TestSimpleBuiltins_testReIm_5d8952605ab5cec9b014901812842ee7() {
        assertEval("{ Re(NA+2i) }");
    }

    @Test
    public void TestSimpleBuiltins_testReIm_ae55c54aa1488eae5687b14ae08b2008() {
        assertEval("{ Im(NA+2i) }");
    }

    @Test
    public void TestSimpleBuiltins_testRecall_ab1f19a0b3e459827e17703fdb01ae66() {
        assertEval("{ f<-function(i) { if(i<=1) 1 else i*Recall(i-1) } ; f(10) }");
    }

    @Test
    public void TestSimpleBuiltins_testRecall_0ba837fe54245bf403e44681adc101c6() {
        assertEval("{ f<-function(i) { if(i<=1) 1 else i*Recall(i-1) } ; g <- f ; f <- sum ; g(10) }");
    }

    @Test
    public void TestSimpleBuiltins_testRecall_c03f6fc45c4259fc4e69c35929eaece2() {
        assertEval("{ f<-function(i) { if (i==1) { 1 } else if (i==2) { 1 } else { Recall(i-1) + Recall(i-2) } } ; f(10) }");
    }

    @Test
    public void TestSimpleBuiltins_testRecall_7c29fb4f1a8750978976ebb307ddc9c8() {
        assertEvalError("{ Recall(10) }");
    }

    @Test
    public void TestSimpleBuiltins_testRegExprComplex_86a31eb43c44df7c7453e0bfe0140ded() {
        assertEval("gregexpr(\"(a)[^a]\\1\", c(\"andrea apart\", \"amadeus\", NA))");
    }

    @Test
    public void TestSimpleBuiltins_testRegExprComplex_97c86df70bdfc0c7ad6b46db6019ca17() {
        assertEval("regexpr(\"(a)[^a]\\1\", c(\"andrea apart\", \"amadeus\", NA))");
    }

    @Test
    public void TestSimpleBuiltins_testRegExprIgnore_72408e09ac9c484ede969026b2eec870() {
        assertEval("regexpr(\"e\",c(\"arm\",\"foot\",\"lefroo\", \"bafoobar\"))");
    }

    @Test
    public void TestSimpleBuiltins_testRegExprIgnore_aed64085f066f3404115215e0fded1c4() {
        assertEval("gregexpr(\"e\",c(\"arm\",\"foot\",\"lefroo\", \"bafoobar\"))");
    }

    @Test
    public void TestSimpleBuiltins_testRep_d3b22cd966ba62bdefd363c5ad17c845() {
        assertEval("{ rep(1,3) }");
    }

    @Test
    public void TestSimpleBuiltins_testRep_fa2060c27d1216733fd09d08d594e4f5() {
        assertEval("{ rep(1:3,2) }");
    }

    @Test
    public void TestSimpleBuiltins_testRep_8c65cba8388301ff747dcfc6f6f37c90() {
        assertEval("{ rep(c(1,2),0) }");
    }

    @Test
    public void TestSimpleBuiltins_testRep_5f00249719b216513619a29b3d41fd07() {
        assertEval("{ rep(as.raw(14), 4) }");
    }

    @Test
    public void TestSimpleBuiltins_testRep_ad161db74641fdf279304ce82b348006() {
        assertEval("{ rep(1:3, length.out=4) }");
    }

    @Test
    public void TestSimpleBuiltins_testRep_a4d795c5d590995b4351345df5bcada9() {
        assertEval("{ rep(\"hello\", 3) }");
    }

    @Test
    public void TestSimpleBuiltins_testRep_1304ebded16b81c3bf627d5d6636af76() {
        assertEval("{ rep(c(1,2),c(3,3)) }");
    }

    @Test
    public void TestSimpleBuiltins_testRepIgnore_f5295de8fec47c85c0ebb8273aaffe5e() {
        assertEval("{ rep(1:3, length.out=NA) }");
    }

    @Test
    public void TestSimpleBuiltins_testRepIgnore_37bcda27c57e6918291616b0f69bf3b6() {
        assertEval("{ x <- as.raw(11) ; names(x) <- c(\"X\") ; rep(x, 3) }");
    }

    @Test
    public void TestSimpleBuiltins_testRepIgnore_5b82141af1888c35e442c79c94ee046f() {
        assertEval("{ x <- as.raw(c(11,12)) ; names(x) <- c(\"X\",\"Y\") ; rep(x, 2) }");
    }

    @Test
    public void TestSimpleBuiltins_testRepIgnore_2df9727ae253abdb9c0ea3a4055d1563() {
        assertEval("{ x <- c(TRUE,NA) ; names(x) <- c(\"X\",NA) ; rep(x, length.out=3) }");
    }

    @Test
    public void TestSimpleBuiltins_testRepIgnore_41febe11e7d8ee67ae1a3c96176e6758() {
        assertEval("{ x <- 1L ; names(x) <- c(\"X\") ; rep(x, times=2) } ");
    }

    @Test
    public void TestSimpleBuiltins_testRepIgnore_4d2f602803b6746348def3b076ff4129() {
        assertEval("{ x <- 1 ; names(x) <- c(\"X\") ; rep(x, times=0) }");
    }

    @Test
    public void TestSimpleBuiltins_testRepIgnore_e8422af202641451dc9547b331356e3f() {
        assertEval("{ x <- 1+1i ; names(x) <- c(\"X\") ; rep(x, times=2) }");
    }

    @Test
    public void TestSimpleBuiltins_testRepIgnore_109091b6f9625b204bc0e053084ffef6() {
        assertEval("{ x <- c(1+1i,1+2i) ; names(x) <- c(\"X\") ; rep(x, times=2) }");
    }

    @Test
    public void TestSimpleBuiltins_testRepIgnore_5e2e382a5ebec41881dd1cac8e3dc177() {
        assertEval("{ x <- c(\"A\",\"B\") ; names(x) <- c(\"X\") ; rep(x, length.out=3) }");
    }

    @Test
    public void TestSimpleBuiltins_testRepInt_4ccfb2f0d566a28b506a769bb45eaa31() {
        assertEval("{ rep.int(1,3) }");
    }

    @Test
    public void TestSimpleBuiltins_testRepInt_cfb2bd1a99d10fc97720eb038c036552() {
        assertEval("{ rep.int(1:3,2) }");
    }

    @Test
    public void TestSimpleBuiltins_testRepInt_2b2fb39de1b0018c90020a32fa7ed961() {
        assertEval("{ rep.int(c(1,2),0) }");
    }

    @Test
    public void TestSimpleBuiltins_testRepInt_4cc9c756f99cfd498d9f4c782df45f2b() {
        assertEval("{ rep.int(c(1,2),2) }");
    }

    @Test
    public void TestSimpleBuiltins_testRepInt_48d3cf910b1d3a2a8ce4a2bb4354b19b() {
        assertEval("{ rep.int(as.raw(14), 4) }");
    }

    @Test
    public void TestSimpleBuiltins_testRepInt_85e972ec6a5a837de34d002db49ab2d2() {
        assertEval("{ rep.int(1L,3L) }");
    }

    @Test
    public void TestSimpleBuiltins_testRepInt_25dc3c5afcc02fec6b3cc41d5fbc3e7e() {
        assertEval("{ rep.int(\"a\",3) }");
    }

    @Test
    public void TestSimpleBuiltins_testRev_4d0b89f7d5b9601a90230cf009915fc3() {
        assertEval("{ rev(1:3) }");
    }

    @Test
    public void TestSimpleBuiltins_testRev_fad7bbf7a1b8ce343cbb228baf5ae77a() {
        assertEval("{ rev(c(1+1i, 2+2i)) }");
    }

    @Test
    public void TestSimpleBuiltins_testRound_33dc93cb0d4989bdf0b386cebae13f9d() {
        assertEval("{ round(0.4) }");
    }

    @Test
    public void TestSimpleBuiltins_testRound_d4361bb15b45e1b2f7425b3a26561a87() {
        assertEval("{ round(0.5) }");
    }

    @Test
    public void TestSimpleBuiltins_testRound_06c33514c24463e163687c2382f27715() {
        assertEval("{ round(0.6) }");
    }

    @Test
    public void TestSimpleBuiltins_testRound_f194a1b4b1019dd99327b6cfa143234f() {
        assertEval("{ round(1.5) }");
    }

    @Test
    public void TestSimpleBuiltins_testRound_226302ca4a1143b69b70bf41dc906765() {
        assertEval("{ round(-1.5) }");
    }

    @Test
    public void TestSimpleBuiltins_testRound_0b1e3418feeb75dce8ac55176403caa4() {
        assertEval("{ round(1L) }");
    }

    @Test
    public void TestSimpleBuiltins_testRound_63d9dc3dcde6510b4d0eaa1feb47039b() {
        assertEval("{ round(1/0) }");
    }

    @Test
    public void TestSimpleBuiltins_testRound_c9f80053d1763fbdf554f07bf0d3efbc() {
        assertEval("{ round(c(0,0.2,0.4,0.6,0.8,1)) }");
    }

    @Test
    public void TestSimpleBuiltins_testRoundIgnore_bb594f5dd03efc19fa1dbee51b5324da() {
        assertEval("{ round(1.123456,digit=2.8) }");
    }

    @Test
    public void TestSimpleBuiltins_testRowStats_80140817c8e933718f596cc1e3fbdfd6() {
        assertEval("{ a = rowSums(matrix(1:12,3,4)); is.null(dim(a)) }");
    }

    @Test
    public void TestSimpleBuiltins_testRowStats_4d8e03379ba609c667ba75b44ee74af9() {
        assertEval("{ a = rowSums(matrix(1:12,3,4)); length(a) }");
    }

    @Test
    public void TestSimpleBuiltins_testRowStats_8da03b857598bdb3f6318c67e59d362c() {
        assertEval("{ a = rowSums(matrix(1:12,3,4)); c(a[1],a[2],a[3]) }");
    }

    @Test
    public void TestSimpleBuiltins_testRowStatsArray_c5f4c7d13c735e2fa65c4f607b63518b() {
        assertEval("{ a = rowSums(array(1:24,c(2,3,4))); is.null(dim(a)) }");
    }

    @Test
    public void TestSimpleBuiltins_testRowStatsArray_06049a7dceb10c804f2b283437a7e06a() {
        assertEval("{ a = rowSums(array(1:24,c(2,3,4))); length(a) }");
    }

    @Test
    public void TestSimpleBuiltins_testRowStatsArray_0963abebe9629587b68d742c268c67e5() {
        assertEval("{ a = rowSums(array(1:24,c(2,3,4))); c(a[1],a[2]) }");
    }

    @Test
    public void TestSimpleBuiltins_testSapply_d1488c6ad2fa1184247d106e5336622c() {
        assertEval("{ f <- function() { sapply(1:3,function(x){x*2L}) }; f() + f() }");
    }

    @Test
    public void TestSimpleBuiltins_testSapply_064143bb552ca8892a7483cf88daa85c() {
        assertEval("{ f <- function() { sapply(c(1,2,3),function(x){x*2}) }; f() + f() }");
    }

    @Test
    public void TestSimpleBuiltins_testSapply_caee58efeb733b5717f0de91b4d20756() {
        assertEval("{ h <- new.env() ; assign(\"a\",1,h) ; assign(\"b\",2,h) ; sa <- sapply(ls(h), function(k) get(k,h,inherits=FALSE)) ; names(sa) }");
    }

    @Test
    public void TestSimpleBuiltins_testSapply_bf5deeac7ad8a02e0a8f0670b694c2a5() {
        assertEval("{ sapply(1:3, function(x) { if (x==1) { list(1) } else if (x==2) { list(NULL) } else { list() } }) }");
    }

    @Test
    public void TestSimpleBuiltins_testSapply_6f9651f5a0cd6b702fe7b1d763478038() {
        assertEval("{ f<-function(g) { sapply(1:3, g) } ; f(function(x) { x*2 }) }");
    }

    @Test
    public void TestSimpleBuiltins_testSapply_7a12a0c33bfc6b4de9d80979df389588() {
        assertEval("{ f<-function() { x<-2 ; sapply(1, function(i) { x }) } ; f() }");
    }

    @Test
    public void TestSimpleBuiltins_testSapply_2eba6aa90b9d5b807306d4d68ef8b26d() {
        assertEval("{ sapply(1:3,function(x){x*2L}) }");
    }

    @Test
    public void TestSimpleBuiltins_testSapply_1cbe85169b8580a19e21f8d802e27042() {
        assertEval("{ sapply(c(1,2,3),function(x){x*2}) }");
    }

    @Test
    public void TestSimpleBuiltins_testSapply_2f7f99dcbb19562b3d6f17b94ee73fcb() {
        assertEval("{ sapply(1:3, length) }");
    }

    @Test
    public void TestSimpleBuiltins_testSapply_676fc88e5b4a020d243c4c5db88ae38e() {
        assertEval("{ f<-length; sapply(1:3, f) }");
    }

    @Test
    public void TestSimpleBuiltins_testSapply_2fcfbb48a94218b02477a08b3c2ea9e6() {
        assertEval("{ sapply(list(1,2,3),function(x){x*2}) }");
    }

    @Test
    public void TestSimpleBuiltins_testSapply_2af813149c865c375985a936ebdb0b4a() {
        assertEval("{ sapply(1:3, function(x) { if (x==1) { 1 } else if (x==2) { integer() } else { TRUE } }) }");
    }

    @Test
    public void TestSimpleBuiltins_testSapplyIgnore_e30b8dbeaaac291438d9893765622dcc() {
        assertEval("{ f<-function(g) { sapply(1:3, g) } ; f(function(x) { x*2 }) ; f(function(x) { TRUE }) }");
    }

    @Test
    public void TestSimpleBuiltins_testSapplyIgnore_d1e677fbd4330542e55296a85de7a560() {
        assertEval("{ sapply(1:2, function(i) { if (i==1) { as.raw(0) } else { 5+10i } }) }");
    }

    @Test
    public void TestSimpleBuiltins_testSapplyIgnore_3348bfd05e16974ee51fd002aa21a7c4() {
        assertEval("{ sapply(1:2, function(i) { if (i==1) { as.raw(0) } else { as.raw(10) } }) }");
    }

    @Test
    public void TestSimpleBuiltins_testSapplyIgnore_a1f12546a0709e269e55fdf2ce6796a1() {
        assertEval("{ sapply(1:2, function(i) { if (i==1) { as.raw(0) } else { \"hello\" }} ) } ");
    }

    @Test
    public void TestSimpleBuiltins_testSapplyIgnore_64d9e8edd48f17d106de20e6c9502df6() {
        assertEval("{ sapply(1:3, function(x) { if (x==1) { list(1) } else if (x==2) { list(NULL) } else { list(2) } }) }");
    }

    @Test
    public void TestSimpleBuiltins_testSapplyIgnore_567fb751fa9228a98594254d6b9f8f06() {
        assertEval("{ sapply(1:3, `-`, 2) }");
    }

    @Test
    public void TestSimpleBuiltins_testSapplyIgnore_9ad66c18d0dee6188d50055a969a5721() {
        assertEval("{ sapply(1:3, \"-\", 2) }");
    }

    @Test
    public void TestSimpleBuiltins_testSapplyIgnore_2be6b7c9102a07fc7019e3f281e0ee77() {
        assertEval("{ sapply(1:3, function(i) { list(1,2) }) }");
    }

    @Test
    public void TestSimpleBuiltins_testSapplyIgnore_e47eef80479600898e0643dc204df376() {
        assertEval("{ sapply(1:3, function(i) { if (i < 3) { list(1,2) } else { c(11,12) } }) }");
    }

    @Test
    public void TestSimpleBuiltins_testSapplyIgnore_90910a59f9c7641649fafc606ad82fbe() {
        assertEval("{ sapply(1:3, function(i) { if (i < 3) { c(1+1i,2) } else { c(11,12) } }) }");
    }

    @Test
    public void TestSimpleBuiltins_testSapplyIgnore_2cf7378fd6b712f0b62c0f76b237c08c() {
        assertEval("{ (sapply(1:3, function(i) { if (i < 3) { list(xxx=1) } else {list(zzz=2)} })) }");
    }

    @Test
    public void TestSimpleBuiltins_testSapplyIgnore_91913713ed1196f2c80dc3bdd44320fe() {
        assertEval("{ (sapply(1:3, function(i) { list(xxx=1:i) } )) }");
    }

    @Test
    public void TestSimpleBuiltins_testSapplyIgnore_142908c5c8b7910e9934f2f41b1aa41a() {
        assertEval("{ sapply(1:3, function(i) { if (i < 3) { list(xxx=1) } else {list(2)} }) }");
    }

    @Test
    public void TestSimpleBuiltins_testSapplyIgnore_2304de70341b6e2e641140ace2ce7f15() {
        assertEval("{ (sapply(1:3, function(i) { if (i < 3) { c(xxx=1) } else {c(2)} })) }");
    }

    @Test
    public void TestSimpleBuiltins_testSapplyIgnore_5a1e2c136a6c7890d7d240bbf2b24fd5() {
        assertEval("{ f <- function() { sapply(c(1,2), function(x) { c(a=x) })  } ; f() }");
    }

    @Test
    public void TestSimpleBuiltins_testSapplyIgnore_c113767d2df4d2f9f537c1fcd5cc62c2() {
        assertEval("{ f <- function() { sapply(c(X=1,Y=2), function(x) { c(a=x) })  } ; f() }");
    }

    @Test
    public void TestSimpleBuiltins_testSapplyIgnore_a5b2fb168341e693b49bdbf8260ea50a() {
        assertEval("{ f <- function() { sapply(c(\"a\",\"b\"), function(x) { c(a=x) })  } ; f() }");
    }

    @Test
    public void TestSimpleBuiltins_testSapplyIgnore_5024f503e2bdd48f3b32408e0c8c3e1c() {
        assertEval("{ f <- function() { sapply(c(X=\"a\",Y=\"b\"), function(x) { c(a=x) })  } ; f() }");
    }

    @Test
    public void TestSimpleBuiltins_testSapplyIgnore_c2167cd4565e9253a6b834237b6772a6() {
        assertEval("{ sapply(c(\"a\",\"b\",\"c\"), function(x) { x }) }");
    }

    @Test
    public void TestSimpleBuiltins_testSd_a7e5475bbc1990b7bf61f291042c9dc4() {
        assertEval("{ round(100*sd(c(1,2))^2) }");
    }

    @Test
    public void TestSimpleBuiltins_testSequence_521e4a932cff700b625d2bfa8bbdcf0f() {
        assertEval("{ 5L:10L }");
    }

    @Test
    public void TestSimpleBuiltins_testSequence_d79bd620eece5b7d4d7a6df0156d438f() {
        assertEval("{ 5L:(0L-5L) }");
    }

    @Test
    public void TestSimpleBuiltins_testSequence_e1f9fc3029ece649eda55d230825b611() {
        assertEval("{ 1:10 }");
    }

    @Test
    public void TestSimpleBuiltins_testSequence_82184fedd2ad890a017bfb4d43a592ae() {
        assertEval("{ 1:(0-10) }");
    }

    @Test
    public void TestSimpleBuiltins_testSequence_69c90807f8ff216d25b73a6826054e8b() {
        assertEval("{ 1L:(0-10) }");
    }

    @Test
    public void TestSimpleBuiltins_testSequence_ffc04a18b2cdc358ba766fe986158dd4() {
        assertEval("{ 1:(0L-10L) }");
    }

    @Test
    public void TestSimpleBuiltins_testSequence_f0d36c1d41525fa15e938cbb14e8152a() {
        assertEval("{ (0-12):1.5 }");
    }

    @Test
    public void TestSimpleBuiltins_testSequence_dd29b7a979c708238b10d3bfdadfd1f0() {
        assertEval("{ 1.5:(0-12) }");
    }

    @Test
    public void TestSimpleBuiltins_testSequence_3b4f0d9fdad10f80a40cbf25c5a0c70a() {
        assertEval("{ (0-1.5):(0-12) }");
    }

    @Test
    public void TestSimpleBuiltins_testSequence_739d2e57eb05e3ca6a2c8182f854a3ad() {
        assertEval("{ 10:1 }");
    }

    @Test
    public void TestSimpleBuiltins_testSequence_75ce42236f5c48ebe0a94226935498bc() {
        assertEval("{ (0-5):(0-9) }");
    }

    @Test
    public void TestSimpleBuiltins_testSequence_05358d747ae7f4d160ef82b11ff90f30() {
        assertEval("{ 1.1:5.1 }");
    }

    @Test
    public void TestSimpleBuiltins_testSequenceStatement_f0ced7ecdfec4e76074a7f2580e91928() {
        assertEval("{ seq(1L,10L) }");
    }

    @Test
    public void TestSimpleBuiltins_testSequenceStatement_f4512de25e264ad92db2680e06133608() {
        assertEval("{ seq(10L,1L) }");
    }

    @Test
    public void TestSimpleBuiltins_testSequenceStatement_d6e5e8fee79605724f232197ef045064() {
        assertEval("{ seq(1L,4L,2L) }");
    }

    @Test
    public void TestSimpleBuiltins_testSequenceStatement_7267f495fcb0457ba0bbd81da84d4e56() {
        assertEval("{ seq(1,-4,-2) }");
    }

    @Test
    public void TestSimpleBuiltins_testSequenceStatementIgnore_6e0da5f0115b849917bed14234134dd1() {
        assertEval("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(c(1,3,10), seq(2L,4L,2L),c(TRUE,FALSE)) }");
    }

    @Test
    public void TestSimpleBuiltins_testSequenceStatementIgnore_0eb86a4dc13ce0ad3244974ab0baef64() {
        assertEval("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(as.double(1:5), seq(7L,1L,-3L),c(TRUE,FALSE,NA)) }");
    }

    @Test
    public void TestSimpleBuiltins_testSequenceStatementIgnore_94d11f5198379c03b6e2a20a174ae13b() {
        assertEval("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(as.logical(-3:3),seq(1L,7L,3L),c(TRUE,NA,FALSE)) }");
    }

    @Test
    public void TestSimpleBuiltins_testSequenceStatementIgnore_74f98be1aeeaf4ffef6fb7da1d0df304() {
        assertEval("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(as.character(-3:3),seq(1L,7L,3L),c(\"A\",\"a\",\"XX\")) }");
    }

    @Test
    public void TestSimpleBuiltins_testSequenceStatementIgnore_7689ae968008d949c0ab6cd0ffaff400() {
        assertEval("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(1:2,1:2,3:4); f(1:2,1:2,c(3,4)) ; f(1:8, seq(1L,7L,3L), c(10,100,1000)) }");
    }

    @Test
    public void TestSimpleBuiltins_testSequenceStatementIgnore_62d05e6f950604222ac778f81a04c118() {
        assertEval("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(1:2,1:2,3:4); f(1:2,1:2,c(3,4)) ; z <- f(1:8, seq(1L,7L,3L), list(10,100,1000)) ; sum(as.double(z)) }");
    }

    @Test
    public void TestSimpleBuiltins_testSequenceStatementNamedParams_18694bb5ccdf385e6887a516efe02fe8() {
        assertEval("{ seq(from=1,to=3) }");
    }

    @Test
    public void TestSimpleBuiltins_testSequenceStatementNamedParams_1efbcccae5859c6dec6f4f14658bc0af() {
        assertEval("{ seq(length.out=1) }");
    }

    @Test
    public void TestSimpleBuiltins_testSequenceStatementNamedParams_831618fbb7e8dff236e5ed2dadc222b6() {
        assertEval("{ seq(from=1.4) }");
    }

    @Test
    public void TestSimpleBuiltins_testSequenceStatementNamedParams_602bfdaca1ca57ba5a79e4ad6757b4eb() {
        assertEval("{ seq(from=1.7) }");
    }

    @Test
    public void TestSimpleBuiltins_testSequenceStatementNamedParams_a45c47e7c8d03a35171e9ea681aeb669() {
        assertEval("{ seq(from=1,to=3,by=1) }");
    }

    @Test
    public void TestSimpleBuiltins_testSequenceStatementNamedParams_953b69f5c8ebbd510614bbb5fd97b426() {
        assertEval("{ seq(from=-10,to=-5,by=2) }");
    }

    @Test
    public void TestSimpleBuiltins_testSequenceStatementNamedParamsIgnore_1251c8e4910f8fd3c34d302f4dedd4e3() {
        assertEval("{ seq(to=-1,from=-10) }");
    }

    @Test
    public void TestSimpleBuiltins_testSequenceStatementNamedParamsIgnore_e098431abfadfb039b6df6aff8256b5e() {
        assertEval("{ seq(length.out=13.4) }");
    }

    @Test
    public void TestSimpleBuiltins_testSequenceStatementNamedParamsIgnore_017c83d5285ef470e086c8cdcf688948() {
        assertEval("{ seq(length.out=0) }");
    }

    @Test
    public void TestSimpleBuiltins_testSequenceStatementNamedParamsIgnore_4d45a903e77e66f73e8e8dc46c0f6295() {
        assertEval("{ seq(along.with=10) }");
    }

    @Test
    public void TestSimpleBuiltins_testSequenceStatementNamedParamsIgnore_8f03e56bf879ae85769df875ba64193f() {
        assertEval("{ seq(along.with=NA) }");
    }

    @Test
    public void TestSimpleBuiltins_testSequenceStatementNamedParamsIgnore_260fc64c52f6c6a0f229523992fc18b8() {
        assertEval("{ seq(along.with=1:10) }");
    }

    @Test
    public void TestSimpleBuiltins_testSequenceStatementNamedParamsIgnore_6da980d127281bc30e3ee84c77da9350() {
        assertEval("{ seq(along.with=-3:-5) }");
    }

    @Test
    public void TestSimpleBuiltins_testSequenceStatementNamedParamsIgnore_9e0234d61f5fcd67663f569045ba0f06() {
        assertEval("{ seq(from=10:12) }");
    }

    @Test
    public void TestSimpleBuiltins_testSequenceStatementNamedParamsIgnore_90194674658adf75d59119916718fc06() {
        assertEval("{ seq(from=c(TRUE, FALSE)) }");
    }

    @Test
    public void TestSimpleBuiltins_testSequenceStatementNamedParamsIgnore_06d793d38a0809d898a5bc0678f47dd2() {
        assertEval("{ seq(from=TRUE, to=TRUE, length.out=0) }");
    }

    @Test
    public void TestSimpleBuiltins_testSequenceStatementNamedParamsIgnore_252700236c9eb870fbf263c1aacd182e() {
        assertEval("{ round(seq(from=10.5, to=15.4, length.out=4), digits=5) }");
    }

    @Test
    public void TestSimpleBuiltins_testSequenceStatementNamedParamsIgnore_34a207f39c5269d9972a5c0adda240b1() {
        assertEval("{ seq(from=11, to=12, length.out=2) }");
    }

    @Test
    public void TestSimpleBuiltins_testSequenceStatementNamedParamsIgnore_2b815d4518fb10efc18eb377b3111cbc() {
        assertEval("{ seq(from=-10.4,to=-5.8,by=2.1) }");
    }

    @Test
    public void TestSimpleBuiltins_testSequenceStatementNamedParamsIgnore_57722e8323d481afeb0dc6bc8ef818e9() {
        assertEval("{ round(seq(from=3L,to=-2L,by=-4.2), digits=5) }");
    }

    @Test
    public void TestSimpleBuiltins_testSequenceStatementNamedParamsIgnore_6e790dfb1de4a070282c353b0be255bd() {
        assertEval("{ seq(along=c(10,11,12)) }");
    }

    @Test
    public void TestSimpleBuiltins_testSimpleRm_ca7a9f28edcdd3c7bf66a0b3735a11dc() {
        assertEval("{ x <- 200 ; rm(\"x\") }");
    }

    @Test
    public void TestSimpleBuiltins_testSimpleRm_7c0682fb8c9a86ff5b94ead1d97dbab6() {
        assertEvalError("{ x <- 200 ; rm(\"x\") ; x }");
    }

    @Test
    public void TestSimpleBuiltins_testSimpleRm_638fe08c6d320c8475e37234929ca562() {
        assertEvalWarning("{ rm(\"ieps\") }");
    }

    @Test
    public void TestSimpleBuiltins_testSort_284b7e7d187c6ab2e4fa9e4409153a7b() {
        assertEval("{ sort(c(1L,10L,2L)) }");
    }

    @Test
    public void TestSimpleBuiltins_testSort_1fd4d093837b7d126d0ef7530e43c343() {
        assertEval("{ sort(c(3,10,2)) }");
    }

    @Test
    public void TestSimpleBuiltins_testSort_6a592c6f57c71c5d15a2ca0155fee884() {
        assertEval("{ sort(c(1,2,0/0,NA)) }");
    }

    @Test
    public void TestSimpleBuiltins_testSort_5aa86dc4ae1bb25c682d61e872e9b040() {
        assertEval("{ sort(c(2,1,0/0,NA), na.last=NA) }");
    }

    @Test
    public void TestSimpleBuiltins_testSort_6a7ec5187507fa97abda94b64f5a079d() {
        assertEval("{ sort(c(3,0/0,2,NA), na.last=TRUE) }");
    }

    @Test
    public void TestSimpleBuiltins_testSort_b5d4d0684b5f7ae93abbd963d09e2547() {
        assertEval("{ sort(c(3,NA,0/0,2), na.last=FALSE) }");
    }

    @Test
    public void TestSimpleBuiltins_testSort_ccb733ea6a05ce0344a90278f6b60239() {
        assertEval("{ sort(c(3L,NA,2L)) }");
    }

    @Test
    public void TestSimpleBuiltins_testSort_894104e630b40ec41f7a3242c9dd48bb() {
        assertEval("{ sort(c(3L,NA,-2L), na.last=TRUE) }");
    }

    @Test
    public void TestSimpleBuiltins_testSort_7371476317ce19939f96f4a8546a66ca() {
        assertEval("{ sort(c(3L,NA,-2L), na.last=FALSE) }");
    }

    @Test
    public void TestSimpleBuiltins_testSort_b2088bf4f79792e07aeb1878814c42dd() {
        assertEval("{ sort(c(a=NA,b=NA,c=3,d=1),na.last=TRUE, decreasing=TRUE) }");
    }

    @Test
    public void TestSimpleBuiltins_testSort_7cfdc805071697201c562b5f50ebd539() {
        assertEval("{ sort(c(a=NA,b=NA,c=3,d=1),na.last=FALSE, decreasing=FALSE) }");
    }

    @Test
    public void TestSimpleBuiltins_testSort_ac8a4c1d13606a72e3e1b8c439efda29() {
        assertEval("{ sort(c(a=0/0,b=1/0,c=3,d=NA),na.last=TRUE, decreasing=FALSE) }");
    }

    @Test
    public void TestSimpleBuiltins_testSort_519a0465d477a73e1db30d78e8776c1b() {
        assertEval("{ sort(double()) }");
    }

    @Test
    public void TestSimpleBuiltins_testSort_df4ed76c79e6d77ac09a69738271e1fd() {
        assertEval("{ sort(c(a=NA,b=NA,c=3L,d=-1L),na.last=TRUE, decreasing=FALSE) }");
    }

    @Test
    public void TestSimpleBuiltins_testSort_2ce0809f50d42943354aa60d00cd1a90() {
        assertEval("{ sort(c(3,NA,1,d=10), decreasing=FALSE, index.return=TRUE) }");
    }

    @Test
    public void TestSimpleBuiltins_testSort_9f37df375d06bb45b37c5fe0fb3d1b54() {
        assertEval("{ sort(3:1, index.return=TRUE) }");
    }

    @Test
    public void TestSimpleBuiltins_testSource_5c17b4de1a98b4e6a8cfa7815d97f7e4() {
        assertEval("{ source(\"test/r/simple/data/tree2/setx.r\") ; x }");
    }

    @Test
    public void TestSimpleBuiltins_testSource_d4a38dfd161547e3c0a27bad69e1cbf8() {
        assertEval("{ source(\"test/r/simple/data/tree2/setx.r\", local=TRUE) ; x }");
    }

    @Test
    public void TestSimpleBuiltins_testSource_be101f4a7d5eb393d6100a7da3b04018() {
        assertEval("{ x <- 1; f <- function() { source(\"test/r/simple/data/tree2/setx.r\", local=TRUE) ; x } ; c(f(), x) }");
    }

    @Test
    public void TestSimpleBuiltins_testSource_f8c23fa44e5be57cccce50c2c2c77af6() {
        assertEval("{ x <- 1; f <- function() { source(\"test/r/simple/data/tree2/setx.r\", local=FALSE) ; x } ; c(f(), x) }");
    }

    @Test
    public void TestSimpleBuiltins_testSource_47529aa6f5e299a286137b552e7163dc() {
        assertEval("{ x <- 1; f <- function() { source(\"test/r/simple/data/tree2/incx.r\", local=FALSE) ; x } ; c(f(), x) }");
    }

    @Test
    public void TestSimpleBuiltins_testSource_e52eebdb86410e47576dc1c11b4690b0() {
        assertEval("{ x <- 1; f <- function() { source(\"test/r/simple/data/tree2/incx.r\", local=TRUE) ; x } ; c(f(), x) }");
    }

    @Test
    public void TestSimpleBuiltins_testSprintf_1e77471269b0c978614b8f4d37953714() {
        assertEval("{ sprintf(\"0x%x\",1L) }");
    }

    @Test
    public void TestSimpleBuiltins_testSprintf_0c2d0dec16af0183539be3efb36ecbb5() {
        assertEval("{ sprintf(\"0x%x\",10L) }");
    }

    @Test
    public void TestSimpleBuiltins_testSprintf_19208d4eb50ba8bf7b46fac21b957c8a() {
        assertEval("{ sprintf(\"%d%d\",1L,2L) }");
    }

    @Test
    public void TestSimpleBuiltins_testSprintf_5f5a0f4fe58cd9e38c2a22cc3dca0530() {
        assertEval("{ sprintf(\"0x%x\",1) }");
    }

    @Test
    public void TestSimpleBuiltins_testSprintf_ea84b3ed172129efec318c5ea6363096() {
        assertEval("{ sprintf(\"0x%x\",10) }");
    }

    @Test
    public void TestSimpleBuiltins_testSprintf_321bdd0da74c24ea895c36cb34c2ac92() {
        assertEval("{ sprintf(\"%d\", 10) }");
    }

    @Test
    public void TestSimpleBuiltins_testSprintf_1c0e42acf7e20e55541505d674246fa2() {
        assertEval("{ sprintf(\"%7.3f\", 10.1) }");
    }

    @Test
    public void TestSimpleBuiltins_testSprintf_f8293e298efeca08ef7f2f7b3a347d9b() {
        assertEval("{ sprintf(\"%03d\", 1:3) }");
    }

    @Test
    public void TestSimpleBuiltins_testSprintf_369ea53e7642159df80f8e0496151883() {
        assertEval("{ sprintf(\"%3d\", 1:3) }");
    }

    @Test
    public void TestSimpleBuiltins_testSprintf_712a0a6e450c44801885e6158758ccd0() {
        assertEval("{ sprintf(\"%4X\", 26) }");
    }

    @Test
    public void TestSimpleBuiltins_testSprintf_aed9d9245ae2af5ba420e569133aa594() {
        assertEval("{ sprintf(\"%04X\", 26) }");
    }

    @Test
    public void TestSimpleBuiltins_testSprintf_2f5377dc3237afcaf3f4b0fa24d32d56() {
        assertEval("{ sprintf(\"Hello %*d\", 3, 2) }");
    }

    @Test
    public void TestSimpleBuiltins_testSprintf_2b763de8de8629771e185b2aa8e8c72f() {
        assertEval("{ sprintf(\"Hello %*2$d\", 3, 2) }");
    }

    @Test
    public void TestSimpleBuiltins_testSprintf_632e911896ba628e751fb0a5d3deb81f() {
        assertEval("{ sprintf(\"Hello %2$*2$d\", 3, 2) }");
    }

    @Test
    public void TestSimpleBuiltins_testSqrt_acb3c0dd030fef9fd12d71b3b967f349() {
        assertEval("{ sqrt(9) }");
    }

    @Test
    public void TestSimpleBuiltins_testSqrt_1ff8a1f5462ee5f4aa4288cca6d73598() {
        assertEval("{ sqrt(9L) }");
    }

    @Test
    public void TestSimpleBuiltins_testSqrt_39f0b9fb3b4e0a9c9ec5e8b165ed4ce3() {
        assertEval("{ sqrt(NA) }");
    }

    @Test
    public void TestSimpleBuiltins_testSqrt_28aebbd3eaef748411bfac3b51dc4c13() {
        assertEval("{ sqrt(c(1,4,9,16)) }");
    }

    @Test
    public void TestSimpleBuiltins_testSqrt_ae452a6abb71b18f117df33e4ff8c8ad() {
        assertEval("{ sqrt(c(1,4,NA,16)) }");
    }

    @Test
    public void TestSimpleBuiltins_testSqrtBroken_dda9ccdc11f9f5afbe9854145501c5e5() {
        assertEval("{ sqrt(c(a=9,b=81)) }");
    }

    @Test
    public void TestSimpleBuiltins_testSqrtBroken_a2489c7a22d5ac414a9587cbff9b6c64() {
        assertEval("{ sqrt(1:5) }");
    }

    @Test
    public void TestSimpleBuiltins_testSqrtBroken_cae4a927d1bb1f88b88550ba795899f5() {
        assertEval("{ sqrt(-1L) }");
    }

    @Test
    public void TestSimpleBuiltins_testSqrtBroken_d1949f3b9fbc81f7fe02ad4b8719bcaa() {
        assertEval("{ sqrt(-1) }");
    }

    @Test
    public void TestSimpleBuiltins_testStrSplit_ffcd71cd9d0efa22e370a386fc0fb6ee() {
        assertEval("{ strsplit(\"helloh\", \"h\", fixed=TRUE) }");
    }

    @Test
    public void TestSimpleBuiltins_testStrSplit_e31b13fe2147045f4dec8805d20a3bd7() {
        assertEval("{ strsplit( c(\"helloh\", \"hi\"), c(\"h\",\"\"), fixed=TRUE) }");
    }

    @Test
    public void TestSimpleBuiltins_testStrSplit_b203e36d815b8d814f4a996c64fd7752() {
        assertEval("{ strsplit(\"helloh\", \"\", fixed=TRUE) }");
    }

    @Test
    public void TestSimpleBuiltins_testStrSplit_78caedc9313c6f8b03c8ee7e1711c675() {
        assertEval("{ strsplit(\"helloh\", \"h\") }");
    }

    @Test
    public void TestSimpleBuiltins_testStrSplit_da73161b8b5923748e2f85186e43a9d6() {
        assertEval("{ strsplit( c(\"helloh\", \"hi\"), c(\"h\",\"\")) }");
    }

    @Test
    public void TestSimpleBuiltins_testStrSplitIgnore_46d4b4f12ca8e8fb947be03344b9b554() {
        assertEval("{ strsplit(\"ahoj\", split=\"\") [[c(1,2)]] }");
    }

    @Test
    public void TestSimpleBuiltins_testSub_290740a46fa19cc21606f434466273ad() {
        assertEval("{ gsub(\"a\",\"aa\", \"prague alley\") }");
    }

    @Test
    public void TestSimpleBuiltins_testSub_b5f6b4aa456065a871e9f43cb28752b0() {
        assertEval("{ sub(\"a\",\"aa\", \"prague alley\") }");
    }

    @Test
    public void TestSimpleBuiltins_testSub_fdf4e3e911b000282bd785289762680f() {
        assertEval("{ gsub(\"a\",\"aa\", \"prAgue alley\") }");
    }

    @Test
    public void TestSimpleBuiltins_testSubIgnore_0902579a0dce5fa8d7a808155b8c09b0() {
        assertEval("{ gsub(\"a\",\"aa\", \"prague alley\", fixed=TRUE) }");
    }

    @Test
    public void TestSimpleBuiltins_testSubIgnore_fa9e3d4d6577b70532d26a56fc343b17() {
        assertEval("{ sub(\"a\",\"aa\", \"prague alley\", fixed=TRUE) }");
    }

    @Test
    public void TestSimpleBuiltins_testSubIgnore_dca0ae0449dfa1c58f334818a4b87673() {
        assertEval("{ gsub(\"a\",\"aa\", \"prAgue alley\", fixed=TRUE) }");
    }

    @Test
    public void TestSimpleBuiltins_testSubIgnore_3d79a5bb75bf60e95350618f5485daa6() {
        assertEval("{ gsub(\"a\",\"aa\", \"prAgue alley\", fixed=TRUE, ignore.case=TRUE) }");
    }

    @Test
    public void TestSimpleBuiltins_testSubIgnore_d1977e782dbbd1ca4da912d5f56d63ed() {
        assertEval("{ gsub(\"a\",\"aa\", \"prAgue alley\", ignore.case=TRUE) }");
    }

    @Test
    public void TestSimpleBuiltins_testSubIgnore_7332b217f11d38a5978915cf31eff6f4() {
        assertEval("{ gsub(\"([a-e])\",\"\\1\\1\", \"prague alley\") }");
    }

    @Test
    public void TestSimpleBuiltins_testSubIgnore_8df24d5d1e0149a6b232c373b6057aa7() {
        assertEval("{ gsub(\"h\",\"\", c(\"hello\", \"hi\", \"bye\")) }");
    }

    @Test
    public void TestSimpleBuiltins_testSubIgnore_42529469f0a7019b2a56e1e5312e0577() {
        assertEval("{ gsub(\"h\",\"\", c(\"hello\", \"hi\", \"bye\"), fixed=TRUE) }");
    }

    @Test
    public void TestSimpleBuiltins_testSubstitute_4d6f07ded5992a096c046ebead59dfd0() {
        assertEval("{ substitute(x + y, list(x=1)) }");
    }

    @Test
    public void TestSimpleBuiltins_testSubstitute_69aeec67da0ee58f71a5a4244df69a7c() {
        assertEval("{ f <- function(expr) { substitute(expr) } ; f(a * b) }");
    }

    @Test
    public void TestSimpleBuiltins_testSubstitute_4c1a0e897f6f8dcba279129803430c82() {
        assertEval("{ f <- function() { delayedAssign(\"expr\", a * b) ; substitute(expr) } ; f() }");
    }

    @Test
    public void TestSimpleBuiltins_testSubstitute_f82a54616cf2b4be6f752e5c66c635c9() {
        assertEval("{ f <- function() { delayedAssign(\"expr\", a * b) ; substitute(dummy) } ; f() }");
    }

    @Test
    public void TestSimpleBuiltins_testSubstitute_587cbbd25dcab3e16f1b360e583c7db5() {
        assertEval("{ delayedAssign(\"expr\", a * b) ; substitute(expr) }");
    }

    @Test
    public void TestSimpleBuiltins_testSubstitute_dc45366e3a931d33e1c7ea987435cdd1() {
        assertEval("{ f <- function(expr) { expr ; substitute(expr) } ; a <- 10; b <- 2; f(a * b) }");
    }

    @Test
    public void TestSimpleBuiltins_testSubstitute_61078b0c4da1266fe57918a4361362dd() {
        assertEval("{ f <- function(expra, exprb) { substitute(expra + exprb) } ; f(a * b, a + b) }");
    }

    @Test
    public void TestSimpleBuiltins_testSubstitute_d84d47dddb7bd0bf96bf16437eadd619() {
        assertEval("{ f <- function(y) { substitute(y) } ; f() }");
    }

    @Test
    public void TestSimpleBuiltins_testSubstitute_5f9847b1be03c329f3c41d8883684dc2() {
        assertEval("{ f <- function(y) { substitute(y) } ; typeof(f()) }");
    }

    @Test
    public void TestSimpleBuiltins_testSubstitute_8308ab3830982170f12169a348ea89e8() {
        assertEval("{ f <- function(z) { g <- function(y) { substitute(y)  } ; g(z) } ; f(a + d) }");
    }

    @Test
    public void TestSimpleBuiltins_testSubstitute_a8173ff3145e5caeadfe0a38e28a2a09() {
        assertEval("{ f <- function(x) { g <- function() { substitute(x) } ; g() } ;  f(a * b) }");
    }

    @Test
    public void TestSimpleBuiltins_testSubstitute_89798b3d8963d8d31c6b22ed6bc05491() {
        assertEval("{ substitute(a, list(a = quote(x + y), x = 1)) }");
    }

    @Test
    public void TestSimpleBuiltins_testSubstitute_3e4cc116e9a592c28b2159c6e8365bfa() {
        assertEval("{ f <- function(x = y, y = x) { substitute(x) } ; f() }");
    }

    @Test
    public void TestSimpleBuiltins_testSubstitute_1bcbef75639b8b543cc72a07279a2203() {
        assertEval("{ f <- function(a, b=a, c=b, d=c) { substitute(d) } ; f(x + y) }");
    }

    @Test
    public void TestSimpleBuiltins_testSubstitute_b728de23a3c96c7d1c7e179ba0cf22c8() {
        assertEval("{ substitute(if(a) { x } else { x * a }, list(a = quote(x + y), x = 1)) }");
    }

    @Test
    public void TestSimpleBuiltins_testSubstitute_b9b9e1994091af7e565e035d8c87b9ef() {
        assertEval("{ substitute(function(x, a) { x + a }, list(a = quote(x + y), x = 1)) }");
    }

    @Test
    public void TestSimpleBuiltins_testSubstitute_9d646fcf10648fbae8e8087bb65a9bd6() {
        assertEval("{ substitute(a[x], list(a = quote(x + y), x = 1)) }");
    }

    @Test
    public void TestSimpleBuiltins_testSubstitute_844fb1f54ddd6fb3cb03e5a9d632edda() {
        assertEval("{ f <- function(x) { substitute(x, list(a=1,b=2)) } ; f(a + b) }");
    }

    @Test
    public void TestSimpleBuiltins_testSubstitute_0083a2f370b2d901d6617b52259cd8ef() {
        assertEval("{ f <- function() { substitute(x(1:10), list(x=quote(sum))) } ; f() }");
    }

    @Test
    public void TestSimpleBuiltins_testSubstitute_ba8b61c2d3fa9c76a2c14d5e96138f4b() {
        assertEval("{ env <- new.env() ; z <- 0 ; delayedAssign(\"var\", z+2, assign.env=env) ; substitute(var, env=env) }");
    }

    @Test
    public void TestSimpleBuiltins_testSubstitute_91aaa32f72b8dab4c7856c1e7e89ed54() {
        assertEval("{ env <- new.env() ; z <- 0 ; delayedAssign(\"var\", z+2, assign.env=env) ; z <- 10 ; substitute(var, env=env) }");
    }

    @Test
    public void TestSimpleBuiltins_testSubstitute_8b21e0ecb7d6143dab8b63c68608f906() {
        assertEval("{ f <- function() { substitute(list(a=1,b=2,...,3,...)) } ; f() }");
    }

    @Test
    public void TestSimpleBuiltins_testSubstitute_6da555da9a31bfb212efe33b45c838d7() {
        assertEval("{ f <- function(...) { substitute(list(a=1,b=2,...,3,...)) } ; f() }");
    }

    @Test
    public void TestSimpleBuiltins_testSubstitute_fc2154960706a9f7207993aa89aaca50() {
        assertEval("{ f <- function(...) { substitute(list(a=1,b=2,...,3,...)) } ; f(x + z, a * b) }");
    }

    @Test
    public void TestSimpleBuiltins_testSubstitute_b6449119b833609315c063f2a2c5a363() {
        assertEval("{ f <- function(...) { substitute(list(...)) } ; f(x + z, a * b) }");
    }

    @Test
    public void TestSimpleBuiltins_testSubstring_2d9423471db74f12008f85fe7ea76b68() {
        assertEval("{ substr(\"123456\", 2L, 4L) }");
    }

    @Test
    public void TestSimpleBuiltins_testSubstring_bc36931aed8ceaa1bd23d0e49cd13344() {
        assertEval("{ substr(\"123456\", 2, 4) }");
    }

    @Test
    public void TestSimpleBuiltins_testSubstring_383a1341ed3de2ff3823a5843a8290ac() {
        assertEval("{ substr(\"123456\", 4, 2) }");
    }

    @Test
    public void TestSimpleBuiltins_testSubstring_a0f17645511412460b64ea030631170b() {
        assertEval("{ substr(\"123456\", 7, 8) }");
    }

    @Test
    public void TestSimpleBuiltins_testSubstring_0ce02680f0c46cb48841f9cea3fb3952() {
        assertEval("{ substr(\"123456\", 4, 8) }");
    }

    @Test
    public void TestSimpleBuiltins_testSubstring_b6957e3b9199fc6d1f01f3ac1d04655f() {
        assertEval("{ substr(\"123456\", -1, 3) }");
    }

    @Test
    public void TestSimpleBuiltins_testSubstring_1bc11f33f0e9cd3c08af5a326104b40c() {
        assertEval("{ substr(\"123456\", -5, -1) }");
    }

    @Test
    public void TestSimpleBuiltins_testSubstring_a50efb9f7b7a89f591f5e8475e7fba3c() {
        assertEval("{ substr(\"123456\", -20, -100) }");
    }

    @Test
    public void TestSimpleBuiltins_testSubstring_479502d919102eefedeeec2f2f7c685d() {
        assertEval("{ substr(\"123456\", 2.8, 4) }");
    }

    @Test
    public void TestSimpleBuiltins_testSubstring_730b2eccd2850ac226982a44541673a0() {
        assertEval("{ substr(c(\"hello\", \"bye\"), 1, 2) }");
    }

    @Test
    public void TestSimpleBuiltins_testSubstring_f97efd130a5093d9607458edc51de2df() {
        assertEval("{ substr(c(\"hello\", \"bye\"), c(1,2,3), 4) }");
    }

    @Test
    public void TestSimpleBuiltins_testSubstring_f282f7d91016707e704e961a510a072a() {
        assertEval("{ substr(c(\"hello\", \"bye\"), 1, c(1,2,3)) }");
    }

    @Test
    public void TestSimpleBuiltins_testSubstring_9e86b7cfe9a9503ac28b0cf1c1c2e930() {
        assertEval("{ substr(c(\"hello\", \"bye\"), c(1,2), c(2,3)) }");
    }

    @Test
    public void TestSimpleBuiltins_testSubstring_a4a55f807c6b0f7a2532159e2bade082() {
        assertEval("{ substr(1234L,2,3) }");
    }

    @Test
    public void TestSimpleBuiltins_testSubstring_03162f5f8ad707a1ae6adcef91e208bc() {
        assertEval("{ substr(1234,2,3) }");
    }

    @Test
    public void TestSimpleBuiltins_testSubstring_2765ec88cd5af3461354f18ca811b329() {
        assertEval("{ substr(\"abcdef\",c(1,2),c(3L,5L)) }");
    }

    @Test
    public void TestSimpleBuiltins_testSubstringIgnore_41302c9bd877c3627e699cd303bfef78() {
        assertEval("{ substring(\"123456\", first=2, last=4) }");
    }

    @Test
    public void TestSimpleBuiltins_testSubstringIgnore_747b32e2b791c976fc9b634a5aef6b23() {
        assertEval("{ substring(\"123456\", first=2.8, last=4) }");
    }

    @Test
    public void TestSimpleBuiltins_testSubstringIgnore_8ce15f4973c2ddb4ca609ef2c4836ab5() {
        assertEval("{ substring(c(\"hello\", \"bye\"), first=c(1,2,3), last=4) }");
    }

    @Test
    public void TestSimpleBuiltins_testSubstringIgnore_6dd56114a5d7ba502c449ca3c03308ae() {
        assertEval("{ substring(\"fastr\", first=NA, last=2) }");
    }

    @Test
    public void TestSimpleBuiltins_testSubstringIgnore_221901876479773561663a589e4c633b() {
        assertEval("{ substr(NA,1,2) }");
    }

    @Test
    public void TestSimpleBuiltins_testSubstringIgnore_d6fcea25fcf0ab63be67b287b1d36d91() {
        assertEval("{ substr(\"fastr\", NA, 2) }");
    }

    @Test
    public void TestSimpleBuiltins_testSubstringIgnore_b67af38ded736620a9005880de5731e0() {
        assertEval("{ substr(\"fastr\", 1, NA) }");
    }

    @Test
    public void TestSimpleBuiltins_testSum_6af75be035eaee127d9548c29f6808da() {
        assertEval("{ sum() }");
    }

    @Test
    public void TestSimpleBuiltins_testSum_d31dace7ded3b759ec0f726e7fd05511() {
        assertEval("{ sum(0, 1, 2, 3) }");
    }

    @Test
    public void TestSimpleBuiltins_testSum_0a122e7ae784861891bdedaa8e2715c3() {
        assertEval("{ sum(c(0, 1, 2, 3)) }");
    }

    @Test
    public void TestSimpleBuiltins_testSum_39dd13a4d223573962016c0b73d385b8() {
        assertEval("{ sum(c(0, 1, 2, 3), 4) }");
    }

    @Test
    public void TestSimpleBuiltins_testSum_9b7f57c2bd4927c31c0f8ce1cb161938() {
        assertEval("{ sum(1:6, 3, 4) }");
    }

    @Test
    public void TestSimpleBuiltins_testSum_64559a1ed3a336ad0145b7e61e1c6459() {
        assertEval("{ sum(1:6, 3L, TRUE) }");
    }

    @Test
    public void TestSimpleBuiltins_testSum_7324adc5a5f1b8a978ff297c7cc86279() {
        assertEval("{ `sum`(1:10) }");
    }

    @Test
    public void TestSimpleBuiltins_testSumIgnore_512304594d55f1330efacd6cc594cf7a() {
        assertEval("{ sum(0, 1[3]) }");
    }

    @Test
    public void TestSimpleBuiltins_testSumIgnore_b579f0fccb80261d02dd8e36a1c21977() {
        assertEval("{ sum(na.rm=FALSE, 0, 1[3]) }");
    }

    @Test
    public void TestSimpleBuiltins_testSumIgnore_71b125cd0c9f2fe015befa381709e1a6() {
        assertEval("{ sum(0, na.rm=FALSE, 1[3]) }");
    }

    @Test
    public void TestSimpleBuiltins_testSumIgnore_d6658778aa6ef9490e87eee1748c00b1() {
        assertEval("{ sum(0, 1[3], na.rm=FALSE) }");
    }

    @Test
    public void TestSimpleBuiltins_testSumIgnore_d8048d7927bb3ae55032b224e19caf66() {
        assertEval("{ sum(0, 1[3], na.rm=TRUE) }");
    }

    @Test
    public void TestSimpleBuiltins_testSumIgnore_79d5da5603083c8a7cd4e867a99de305() {
        assertEval("{ sum(1+1i,2,NA, na.rm=TRUE) }");
    }

    @Test
    public void TestSimpleBuiltins_testTranspose_6e1d2dd0ef9ac4c3dbae8a1bc755d657() {
        assertEval("{ m <- matrix(1:49, nrow=7) ; sum(m * t(m)) }");
    }

    @Test
    public void TestSimpleBuiltins_testTranspose_dd94bafdef407bdc3e73264698eb5b5a() {
        assertEval("{ m <- matrix(1:81, nrow=9) ; sum(m * t(m)) }");
    }

    @Test
    public void TestSimpleBuiltins_testTranspose_209de10a7856180c428e9816d06d1a43() {
        assertEval("{ m <- matrix(-5000:4999, nrow=100) ; sum(m * t(m)) }");
    }

    @Test
    public void TestSimpleBuiltins_testTranspose_fa1a12b9e0728d5f5374903976e090f4() {
        assertEval("{ m <- matrix(c(rep(1:10,100200),100L), nrow=1001) ; sum(m * t(m)) }");
    }

    @Test
    public void TestSimpleBuiltins_testTranspose_23b14b1abdbbcc6ee0ba28e66b2dc0b3() {
        assertEval("{ m <- double() ; dim(m) <- c(0,4) ; t(m) }");
    }

    @Test
    public void TestSimpleBuiltins_testTranspose_0f10beb0082312c346b7a524e0232269() {
        assertEval("{ t(1:3) }");
    }

    @Test
    public void TestSimpleBuiltins_testTranspose_8c3760da6589e7f75a2c2e7f69f79d92() {
        assertEval("{ t(t(t(1:3))) }");
    }

    @Test
    public void TestSimpleBuiltins_testTranspose_0282fba864025d02c3c4e8ebd7541e68() {
        assertEval("{ t(matrix(1:6, nrow=2)) }");
    }

    @Test
    public void TestSimpleBuiltins_testTranspose_db2d13e94432cc5797b041c97f0d18a3() {
        assertEval("{ t(t(matrix(1:6, nrow=2))) }");
    }

    @Test
    public void TestSimpleBuiltins_testTranspose_fef54622909a3d0c5407fc40bf43e478() {
        assertEval("{ t(matrix(1:4, nrow=2)) }");
    }

    @Test
    public void TestSimpleBuiltins_testTranspose_2934c6176efb21ea0d9a503c7ec5b175() {
        assertEval("{ t(t(matrix(1:4, nrow=2))) }");
    }

    @Test
    public void TestSimpleBuiltins_testTriangular_41ca685d92138926005a9f7fb6ca8478() {
        assertEval("{ m <- { matrix( as.character(1:6), nrow=2 ) } ; diag(m) <- c(1,2) ; m }");
    }

    @Test
    public void TestSimpleBuiltins_testTriangular_f1776e942214f71194d5c31b1a80996e() {
        assertEval("{ m <- { matrix( (1:6) * (1+3i), nrow=2 ) } ; diag(m) <- c(1,2) ; m }");
    }

    @Test
    public void TestSimpleBuiltins_testTriangular_e3c989be96bfd58a83c33b08e911de62() {
        assertEval("{ m <- { matrix( as.raw(11:16), nrow=2 ) } ; diag(m) <- c(as.raw(1),as.raw(2)) ; m }");
    }

    @Test
    public void TestSimpleBuiltins_testTypeCheck_4ad2fc9014b639c9865b3f87a754d86b() {
        assertEval("{ is.double(10L) }");
    }

    @Test
    public void TestSimpleBuiltins_testTypeCheck_a09943eec69c0f9e6c5108055eec119a() {
        assertEval("{ is.double(10) }");
    }

    @Test
    public void TestSimpleBuiltins_testTypeCheck_8a62928742d9328c0d2b62ebb0aaa3eb() {
        assertEval("{ is.double(\"10\") }");
    }

    @Test
    public void TestSimpleBuiltins_testTypeCheck_73b039879ab6c5b42795c2d117043aa9() {
        assertEval("{ is.numeric(10L) }");
    }

    @Test
    public void TestSimpleBuiltins_testTypeCheck_0b260831340c6db5b75b5343f4d81141() {
        assertEval("{ is.numeric(10) }");
    }

    @Test
    public void TestSimpleBuiltins_testTypeCheck_5fb1f85fd979e797ae381ddc6aa28d81() {
        assertEval("{ is.numeric(TRUE) }");
    }

    @Test
    public void TestSimpleBuiltins_testTypeCheck_918eddac7eccb3a1f6e6db8ef40e5e88() {
        assertEval("{ is.character(\"hi\") }");
    }

    @Test
    public void TestSimpleBuiltins_testTypeCheck_1d50db654ed0e3b39436f75f170da0eb() {
        assertEval("{ is.logical(1L) }");
    }

    @Test
    public void TestSimpleBuiltins_testTypeCheck_8ef21fca19da63ee364eaead6eec8dff() {
        assertEval("{ is.integer(1) }");
    }

    @Test
    public void TestSimpleBuiltins_testTypeCheck_b3240109e17bef22adb2244c9588ecd1() {
        assertEval("{ is.integer(1L) }");
    }

    @Test
    public void TestSimpleBuiltins_testTypeCheck_119fa91e62811491da293d9888681f22() {
        assertEval("{ is.complex(1i) }");
    }

    @Test
    public void TestSimpleBuiltins_testTypeCheck_28a1fd268cf649ed4571a85d1fea88fd() {
        assertEval("{ is.complex(1) }");
    }

    @Test
    public void TestSimpleBuiltins_testTypeCheck_3ab2e42a3bbdf4c825bbb035a00e6a29() {
        assertEval("{ is.raw(raw()) }");
    }

    @Test
    public void TestSimpleBuiltins_testTypeCheck_ed27de3be242606ea27d2246faaf0ae7() {
        assertEval("{ is.logical(NA) }");
    }

    @Test
    public void TestSimpleBuiltins_testTypeCheck_05ed427ea5ed1e2a96e61f007cbd5cd7() {
        assertEval("{ is.matrix(1) }");
    }

    @Test
    public void TestSimpleBuiltins_testTypeCheck_aea25d02a08b3d0682a3cc768137582c() {
        assertEval("{ is.matrix(NULL) }");
    }

    @Test
    public void TestSimpleBuiltins_testTypeCheck_5d58ab660464a607a098cd9272d211a9() {
        assertEval("{ is.matrix(matrix(1:6, nrow=2)) }");
    }

    @Test
    public void TestSimpleBuiltins_testTypeCheck_c5ae72c4471fe269e025837002cf6b4f() {
        assertEval("{ is.array(1) }");
    }

    @Test
    public void TestSimpleBuiltins_testTypeCheck_9fd110693bdcf0f4f6c80bdbb70502cf() {
        assertEval("{ is.array(NULL) }");
    }

    @Test
    public void TestSimpleBuiltins_testTypeCheck_1255c1431b609d712fe80ec07f61dfca() {
        assertEval("{ is.array(matrix(1:6, nrow=2)) }");
    }

    @Test
    public void TestSimpleBuiltins_testTypeCheck_d57496c27c1d770f9553513344aaffe8() {
        assertEval("{ is.array(1:6) }");
    }

    @Test
    public void TestSimpleBuiltins_testTypeCheckIgnore_7f8323b03018432a0d32c10f362ec5d7() {
        assertEval("{ is.list(NULL) }");
    }

    @Test
    public void TestSimpleBuiltins_testTypeOf_f23b57132b2f7a3dd4b957c584b7746b() {
        assertEval("{ typeof(1) }");
    }

    @Test
    public void TestSimpleBuiltins_testTypeOf_1f33f5956306e18e68d3c5cc52303074() {
        assertEval("{ typeof(1L) }");
    }

    @Test
    public void TestSimpleBuiltins_testTypeOf_7169b22f93ac5461a6c1da2aeaf9e372() {
        assertEval("{ typeof(function(){}) }");
    }

    @Test
    public void TestSimpleBuiltins_testTypeOf_0ab75cc04f8de691d716ed2cafebe0a6() {
        assertEval("{ typeof(\"hi\") }");
    }

    @Test
    public void TestSimpleBuiltins_testTypeOf_3d3698b481587c6638dce1b558e3ccaa() {
        assertEval("{ typeof(sum) }");
    }

    @Test
    public void TestSimpleBuiltins_testTypeOf_0d8b1d0664b1b51d65d5f5ef5fd1a2c1() {
        assertEval("{ typeof(NULL) }");
    }

    @Test
    public void TestSimpleBuiltins_testTypeOf_de9301cc97b392acab5934faa8a3298d() {
        assertEval("{ typeof(TRUE) }");
    }

    @Test
    public void TestSimpleBuiltins_testTypeOf_21b618c3d8c7e0223f9ab0489d0abcac() {
        assertEval("{ typeof(\"test\") }");
    }

    @Test
    public void TestSimpleBuiltins_testTypeOf_7375a099f1bea64b22645d6754bed891() {
        assertEval("{ typeof(c(1, 2, 3)) }");
    }

    @Test
    public void TestSimpleBuiltins_testTypeOf_2d0a9dca70c2018ed9d7e4d630e1f63c() {
        assertEval("{ typeof(c(1L, 2L, 3L)) }");
    }

    @Test
    public void TestSimpleBuiltins_testTypeOf_af61fcd2481200b529fd2affd9372344() {
        assertEval("{ typeof(1:3) }");
    }

    @Test
    public void TestSimpleBuiltins_testTypeOf_75ed2f1fd8f51f28e63396c01050a4a1() {
        assertEval("{ typeof(c(TRUE, TRUE, FALSE)) }");
    }

    @Test
    public void TestSimpleBuiltins_testTypeOf_4db9864824bbc26c451ce251404a653f() {
        assertEval("{ typeof(typeof(NULL)) }");
    }

    @Test
    public void TestSimpleBuiltins_testTypeOf_8155b6f4e306fc6ae858a8160e770221() {
        assertEval("{ length(typeof(NULL)) }");
    }

    @Test
    public void TestSimpleBuiltins_testTypeOf_6832941989a6faf780031672b354dfe5() {
        assertEval("{ typeof(length(typeof(NULL))) }");
    }

    @Test
    public void TestSimpleBuiltins_testUnlist_52964c4cb43a47670c1f4d283abd1e1d() {
        assertEval("{ unlist(list(\"hello\", \"hi\")) }");
    }

    @Test
    public void TestSimpleBuiltins_testUnlistIgnore_f35b6e0161ac852251f29fe1bc8a7f0c() {
        assertEval("{ unlist(list(a=\"hello\", b=\"hi\")) }");
    }

    @Test
    public void TestSimpleBuiltins_testUnlistIgnore_0e497d9170f54c56c46d71f9c2a7b065() {
        assertEval("{ x <- list(a=1,b=2:3,list(x=FALSE)) ; unlist(x, recursive=FALSE) }");
    }

    @Test
    public void TestSimpleBuiltins_testUnlistIgnore_053bfeb29189c57f2c388a6015092e27() {
        assertEval("{ x <- list(1,z=list(1,b=22,3)) ; unlist(x, recursive=FALSE) }");
    }

    @Test
    public void TestSimpleBuiltins_testUnlistIgnore_566f28a4c86058a48ce00c31c2d3032c() {
        assertEval("{ x <- list(1,z=list(1,b=22,3)) ; unlist(x, recursive=FALSE, use.names=FALSE) }");
    }

    @Test
    public void TestSimpleBuiltins_testUnlistIgnore_a698318202ba9d48899d816aaf045170() {
        assertEval("{ x <- list(\"a\", c(\"b\", \"c\"), list(\"d\", list(\"e\"))) ; unlist(x) }");
    }

    @Test
    public void TestSimpleBuiltins_testUnlistIgnore_f28bf5269d48ccb8325f37a8fda65a1d() {
        assertEval("{ x <- list(NULL, list(\"d\", list(), character())) ; unlist(x) }");
    }

    @Test
    public void TestSimpleBuiltins_testUnlistIgnore_1d0087eeeb15e56b4081ebf242c3ee4c() {
        assertEval("{ x <- list(a=list(\"1\",\"2\",b=\"3\",\"4\")) ; unlist(x) }");
    }

    @Test
    public void TestSimpleBuiltins_testUnlistIgnore_86b20ffcf8f88b8502d3da0218b3327c() {
        assertEval("{ x <- list(a=list(\"1\",\"2\",b=list(\"3\"))) ; unlist(x) }");
    }

    @Test
    public void TestSimpleBuiltins_testUnlistIgnore_ca79e22b108545ebb9086587d6a71e2f() {
        assertEval("{ x <- list(a=list(1,FALSE,b=list(2:4))) ; unlist(x) }");
    }

    @Test
    public void TestSimpleBuiltins_testUnlistIgnore_7c2d3aec2785e5d94b9cad216a3ba4f9() {
        assertEval("{ x <- list(1,list(2,3),4) ; z <- list(x,x) ; u <- list(z,z) ; u[[c(2,2,3)]] <- 6 ; unlist(u) }");
    }

    @Test
    public void TestSimpleBuiltins_testUpdateClass_b026dd671e5d00aa004905d5c6045e7e() {
        assertEval("{x=1; class(x)<-\"first\"; x;}");
    }

    @Test
    public void TestSimpleBuiltins_testUpdateClass_552769ad36db8589fb70fb1b41993e41() {
        assertEval("{ x=1;class(x)<-\"character\"; x}");
    }

    @Test
    public void TestSimpleBuiltins_testUpdateClass_8a441adef27997e1e1532772e5a9e286() {
        assertEval("{x<-1; class(x)<-\"logical\"; x;  class(x)<-c(1,2,3); x; class(x)<-NULL; x;}");
    }

    @Test
    public void TestSimpleBuiltins_testUpdateClass_526b2561673ee2a69337f4788254a5eb() {
        assertEval("{x<-1;class(x)<-c(1,2,3);class(x)<-c(); x;}");
    }

    @Test
    public void TestSimpleBuiltins_testUpdateClass_d03db72108886a98c46295a164ed4d85() {
        assertEval("{x<-1;class(x)<-c(1,2,3); x;}");
    }

    @Test
    public void TestSimpleBuiltins_testUpdateClass_776a79e57b4a962db7bcd05dfa5ce8d1() {
        assertEval("{x<-1;class(x)<-c(TRUE,FALSE); x;}");
    }

    @Test
    public void TestSimpleBuiltins_testUpdateClass_d038930d8b6c578f3bb46297f75874ca() {
        assertEval("{x<-1;class(x)<-c(2+3i,4+5i); x;}");
    }

    @Test
    public void TestSimpleBuiltins_testUpdateClass_048feec8f0e66e4525a8d8ed08d363c2() {
        assertEval("{x<-1;class(x)<-c(1,2,3);class(x)<-NULL; x;}");
    }

    @Test
    public void TestSimpleBuiltins_testUpdateClass_463192aaee84feb9609362e07c0be76a() {
        assertEval("{x<-c(1,2,3,4); dim(x)<-c(2,2); class(x)<-\"array\"; x; class(x)<-\"matrix\"; x;}");
    }

    @Test
    public void TestSimpleBuiltins_testUpdateClass_e8351001b47001baafd3504bb524c875() {
        assertEval("{x<-c(1,2,3,4); dim(x)<-c(2,2); class(x)}");
    }

    @Test
    public void TestSimpleBuiltins_testUpdateClass_fe6405456a75e70bd76c070552f35d60() {
        assertEval("{x<-c(1,2,3,4); dim(x)<-c(2,2); class(x);dim(x)<-c(2,2,1);class(x)}");
    }

    @Test
    public void TestSimpleBuiltins_testUpdateClass_c9e0b14e171ef8219401c574023d3708() {
        assertEval("{x<-c(1,2,3,4); dim(x)<-c(2,2,1); class(x)}");
    }

    @Test
    public void TestSimpleBuiltins_testUpdateClass_49cf6b6a29e324e3eab3a030e50c26a3() {
        assertEval("{x<-1;class(x)<-c(1,2,3);y<-unclass(x);x;y}");
    }

    @Test
    public void TestSimpleBuiltins_testUpdateClass_ec3513da84f6f670f7d4a57912d6a339() {
        assertEval("{x<-1;class(x)<-\"a\";x}");
    }

    @Test
    public void TestSimpleBuiltins_testUpdateClass_da3101183d77d64cb95eff24c3850c92() {
        assertEval("{x<-1;class(x)<-\"a\";class(x)<-\"numeric\";x;}");
    }

    @Test
    public void TestSimpleBuiltins_testUpdateClass_73236b353b738acb3cdbd9c14f9c65df() {
        assertEval("{x<-TRUE;class(x)<-\"a\";class(x)<-\"logical\";x;}");
    }

    @Test
    public void TestSimpleBuiltins_testUpdateClass_22ecd3611e61cf5e9eaeb1af64df9713() {
        assertEval("{x<-2+3i;class(x)<-\"a\";class(x)<-\"complex\";x;}");
    }

    @Test
    public void TestSimpleBuiltins_testUpdateClass_6baf58e27ad47ba371aca29e93048efd() {
        assertEval("{x<-c(1,2);class(x)<-\"a\";class(x)<-\"list\";x;}");
    }

    @Test
    public void TestSimpleBuiltins_testUpdateClass_2b0b627305ea65b93c2163d63403c0de() {
        assertEval("{x<-\"abc\";class(x)<-\"a\";class(x)<-\"character\";x;}");
    }

    @Test
    public void TestSimpleBuiltins_testUpdateClass_f8c35e459e547ef07df2f112d6ac592d() {
        assertEval("{x<-c(2+3i,4+5i);class(x)<-\"a\";class(x)<-\"complex\";x;}");
    }

    @Test
    public void TestSimpleBuiltins_testUpdateClass_a4af9cf22b0a25e13f9738dcc3fdaa78() {
        assertEval("{x<-1;attr(x,\"class\")<-c(\"a\",\"b\");x;}");
    }

    @Test
    public void TestSimpleBuiltins_testUpdateClass_72ba9bfd00d92c6921a9f9eeb295663b() {
        assertEval("{x<-1;attr(x,\"class\")<-c(\"a\",\"b\");attr(x,\"class\")<-\"numeric\";x}");
    }

    @Test
    public void TestSimpleBuiltins_testUpdateClass_df40771873a47544ab7bbff308f8277f() {
        assertEval("{x<-1;attr(x,\"class\")<-\"b\";x;}");
    }

    @Test
    public void TestSimpleBuiltins_testUpdateClass_d6b803bf6147aa8341ca32569078ae88() {
        assertEval("{x<-1;y<-\"b\";attr(x,\"class\")<-y;x;}");
    }

    @Test
    public void TestSimpleBuiltins_testUpdateClassIgnore_de2b6cfc60c31afa53dbd74ec10d3136() {
        assertEval("{x<-c(1,2,3,4); class(x)<-\"array\"; class(x)<-\"matrix\";}");
    }

    @Test
    public void TestSimpleBuiltins_testUpdateClassIgnore_dfbd07abb7b6feb1f2afd25c4ad019ef() {
        assertEval("{x<-1;attr(x,\"class\")<-c(1,2,3);}");
    }

    @Test
    public void TestSimpleBuiltins_testUpdateDiagonal_22adf63c895e0643c07b11286c2701ff() {
        assertEval("{ m <- matrix(1:6, nrow=3) ; diag(m) <- c(1,2) ; m }");
    }

    @Test
    public void TestSimpleBuiltins_testUpdateDiagonal_927e9b7da432914a51553ea8963fd3b0() {
        assertEval("{ m <- matrix(1:6, nrow=3); y<-m+42; diag(y) <- c(1,2); y }");
    }

    @Test
    public void TestSimpleBuiltins_testUpdateDiagonal_36bfcdec38ec4f6e82eac4495b9b385e() {
        assertEval("{ m <- matrix(1:6, nrow=3) ;  attr(m, \"foo\")<-\"foo\"; diag(m) <- c(1,2); attributes(m) }");
    }

    @Test
    public void TestSimpleBuiltins_testUpdateDiagonal_7c8f4662acfa9913ff8d6b9d25042c7f() {
        assertEval("{ m <- matrix(1:6, nrow=3) ; diag(m) <- c(1.1,2.2); m }");
    }

    @Test
    public void TestSimpleBuiltins_testUpdateDiagonal_f4a7c352af2d5f2b21300e141f70bfa6() {
        assertEval("{ m <- matrix(1:6, nrow=3) ; diag(m) <- c(1.1,2); m }");
    }

    @Test
    public void TestSimpleBuiltins_testUpdateDiagonal_0c9c209deab9bdd4a4dc6d6c967f9050() {
        assertEval("{ m <- matrix(1:6, nrow=3) ; diag(m) <- c(1,2.2); m }");
    }

    @Test
    public void TestSimpleBuiltins_testUpdateDiagonal_73173fb5b0b7dbaec878c9af7ea0272e() {
        assertEval("{ m <- matrix(1:6, nrow=3) ;  attr(m, \"foo\")<-\"foo\"; diag(m) <- c(1.1,2.2); attributes(m) }");
    }

    @Test
    public void TestSimpleBuiltins_testUpdateDiagonal_bc599977512a7c0c3a2e02240001e906() {
        assertEval("{ x <- (m <- matrix(1:6, nrow=3)) ; diag(m) <- c(1,2) ; x }");
    }

    @Test
    public void TestSimpleBuiltins_testUpdateDiagonal_09e39a7080c61e974aa17b123966ca64() {
        assertEval("{ m <- matrix(1:6, nrow=3) ; f <- function() { diag(m) <- c(100,200) } ; f() ; m }");
    }

    @Test
    public void TestSimpleBuiltins_testUpdateNames_e198a69acefc7b109fc57f84d1e4961c() {
        assertEval("{ x <- c(1,2) ; names(x) <- c(\"hello\", \"hi\"); names(x) } ");
    }

    @Test
    public void TestSimpleBuiltins_testUpdateNames_c74908428b530eea5e0fb376164ff012() {
        assertEval("{ x <- 1:2 ; names(x) <- c(\"hello\", \"hi\"); names(x) } ");
    }

    @Test
    public void TestSimpleBuiltins_testUpdateNames_9fc9d2a738d2134caf4cfc5b75e89e92() {
        assertEval("{ x<-c(1, 2); attr(x, \"names\")<-c(\"a\", \"b\"); x }");
    }

    @Test
    public void TestSimpleBuiltins_testUpdateNames_bde2dc504616ae4d0796a69cb583b448() {
        assertEval("{ x<-c(1, 2); attr(x, \"names\")<-c(\"a\", \"b\"); names(x)<-NULL; attributes(x) }");
    }

    @Test
    public void TestSimpleBuiltins_testUpdateNames_443ef98bbe06c2638ba2231400b6a805() {
        assertEval("{ x<-c(1, 2); names(x)<-c(\"a\", \"b\"); attr(x, \"names\")<-NULL; x }");
    }

    @Test
    public void TestSimpleBuiltins_testUpdateNames_45f921bf4e27f1472bf75c92265dc212() {
        assertEval("{ x<-c(1, 2); names(x)<-42; x }");
    }

    @Test
    public void TestSimpleBuiltins_testUpdateNames_e61e3b6ebd70f1ffe0e3f4a16aa19c2c() {
        assertEval("{ x<-c(1, 2); names(x)<-c(TRUE, FALSE); x }");
    }

    @Test
    public void TestSimpleBuiltins_testUpdateNames_2e95d15b0b7d46db216765456c26f277() {
        assertEval("{ x<-list(1,2); names(x)<-c(\"a\",NA); x }");
    }

    @Test
    public void TestSimpleBuiltins_testUpdateNames_7a672acb012dedae00306ed4769b3bfd() {
        assertEval("{ x<-list(1,2); names(x)<-c(\"a\",\"$\"); x }");
    }

    @Test
    public void TestSimpleBuiltins_testUpdateNames_5bcbc88a4622b78bcef8ebb2a020f0ae() {
        assertEval("{ x<-list(1,2); names(x)<-c(\"a\",\"b\"); x }");
    }

    @Test
    public void TestSimpleBuiltins_testUpdateNames_e393ef7225640d92615b82181a5a5ca6() {
        assertEval("{ x<-list(1,2); names(x)<-42:43; x }");
    }

    @Test
    public void TestSimpleBuiltins_testUpdateNames_58dd20f93d768e0d40bd3bef178d405b() {
        assertEval("{ x<-7; attr(x, \"foo\")<-\"a\"; attr(x, \"bar\")<-42; attributes(x) }");
    }

    @Test
    public void TestSimpleBuiltins_testUpdateNames_19d40aeb38c31829ea680c7fd52a2681() {
        assertEval("{ x<-c(\"a\", \"\", \"bbb\", \"\", \"c\"); names(x)<-1:4; x }");
    }

    @Test
    public void TestSimpleBuiltins_testUpdateNames_193ad5f0e7b508124e52bd4a28c93cc8() {
        assertEval("{ x <- c(1,2); names(x) <- c(\"hello\", \"hi\") ; x }");
    }

    @Test
    public void TestSimpleBuiltins_testUpdateNames_3736e14863f8a138edd27c060ef9fb20() {
        assertEval("{ x <- 1:2; names(x) <- c(\"hello\", \"hi\") ; x }");
    }

    @Test
    public void TestSimpleBuiltins_testUpdateNames_82002890e9d84bebe018a87938817c57() {
        assertEval("{ x <- c(1,9); names(x) <- c(\"hello\",\"hi\") ; sqrt(x) }");
    }

    @Test
    public void TestSimpleBuiltins_testUpdateNames_2c7554239072a265b828a13cdc0ae57d() {
        assertEval("{ x <- c(1,9); names(x) <- c(\"hello\",\"hi\") ; is.na(x) }");
    }

    @Test
    public void TestSimpleBuiltins_testUpdateNames_63fdd9e50620f24ff3163d5bfe4f6216() {
        assertEval("{ x <- c(1,NA); names(x) <- c(\"hello\",\"hi\") ; cumsum(x) }");
    }

    @Test
    public void TestSimpleBuiltins_testUpdateNames_0e37a2d5d706cfbb0f2a7b710fc11c0e() {
        assertEval("{ x <- c(1,NA); names(x) <- c(NA,\"hi\") ; cumsum(x) }");
    }

    @Test
    public void TestSimpleBuiltins_testUpdateNames_ae2d3abcd724826738d2516eab9dedd5() {
        assertEval("{ x <- 1:2; names(x) <- c(\"A\", \"B\") ; abs(x) }");
    }

    @Test
    public void TestSimpleBuiltins_testUpdateNames_fd0f00f1f70705698f7d0e7dfa3ab447() {
        assertEval("{ z <- c(a=1, b=2) ; names(z) <- NULL ; z }");
    }

    @Test
    public void TestSimpleBuiltins_testUpdateNames_d946b1680854335d34946599b5e36868() {
        assertEval("{ x <- c(1,2) ; names(x) <- c(\"hello\"); names(x) }");
    }

    @Test
    public void TestSimpleBuiltins_testUpdateNames_5627f064a2be6910a4b02b2972ecdbc0() {
        assertEval("{ x <- 1:2 ; names(x) <- c(\"hello\"); names(x) }");
    }

    @Test
    public void TestSimpleBuiltins_testUpdateNames_a3ee0f7d402245158d533aae17ce0a22() {
        assertEvalError("{ x<-c(1,2); names(x) <- 42:44; x }");
    }

    @Test
    public void TestSimpleBuiltins_testUpdateNames_65bdea328aa309d0db153a1c72cd7e3e() {
        assertEvalError("{ x<-c(1,2); attr(x, \"names\") <- 42:45; x }");
    }

    @Test
    public void TestSimpleBuiltins_testUpdateNamesIgnore_2cffdfa878b18bbad7b6a53d7e4932ae() {
        assertEval("{ x <- c(1,2); names(x) <- c(\"A\", \"B\") ; x + 1 }");
    }

    @Test
    public void TestSimpleBuiltins_testUpdateNamesIgnore_d40e4da2cc65cb7648581165a629d52a() {
        assertEval("{ x <- 1:2; names(x) <- c(\"A\", \"B\") ; y <- c(1,2,3,4) ; names(y) <- c(\"X\", \"Y\", \"Z\") ; x + y }");
    }

    @Test
    public void TestSimpleBuiltins_testUpperTriangular_59ec3ba9a936ceaa71459f89969b9373() {
        assertEval("{ m <- matrix(1:6, nrow=2) ;  upper.tri(m, diag=TRUE) }");
    }

    @Test
    public void TestSimpleBuiltins_testUpperTriangular_7a2a681e328cbddd6fcb0be530c10f59() {
        assertEval("{ m <- matrix(1:6, nrow=2) ;  upper.tri(m, diag=FALSE) }");
    }

    @Test
    public void TestSimpleBuiltins_testUpperTriangular_9f58ca08d0fb67c7c4b4a2fb2dc4770b() {
        assertEval("{ upper.tri(1:3, diag=TRUE) }");
    }

    @Test
    public void TestSimpleBuiltins_testUpperTriangular_c5229e5f6220d8ffaf6059b74988078e() {
        assertEval("{ upper.tri(1:3, diag=FALSE) }");
    }

    @Test
    public void TestSimpleBuiltins_testUseMethodEnclFuncArgs_c699286a5e7dd6ca4c46b1245a1f633e() {
        assertEval("{f <- function(x,y,z){ UseMethod(\"f\"); }; f.second <- function(x,y,z){cat(\"f second\",x,y,z)}; obj <-1; attr(obj,\"class\") <- \"second\"; arg2=2; arg3=3; f(obj,arg2,arg3);}");
    }

    @Test
    public void TestSimpleBuiltins_testUseMethodLocalVars_cd724107886a7c9d25ae3b6aad713cb6() {
        assertEval("{f <- function(x){ y<-2;locFun <- function(){cat(\"local\")}; UseMethod(\"f\"); }; f.second <- function(x){cat(\"f second\",x);locFun();}; obj <-1; attr(obj,\"class\")  <- \"second\"; f(obj);}");
    }

    @Test
    public void TestSimpleBuiltins_testUseMethodNested_d689820491ffcbc9ddb83012801bd243() {
        assertEval("{f <- function(x){g<- function(x){ h<- function(x){ UseMethod(\"f\");}; h(x)}; g(x) }; f.second <- function(x){cat(\"f second\",x);}; obj <-1; attr(obj,\"class\")  <- \"second\"; f(obj);}");
    }

    @Test
    public void TestSimpleBuiltins_testUseMethodOneArg_8bf84b00a22cc5bb15b74ae0b3384ade() {
        assertEval("{f <- function(x){ UseMethod(\"f\"); };f.first <- function(x){cat(\"f first\",x)}; f.second <- function(x){cat(\"f second\",x)}; obj <-1; attr(obj,\"class\")  <- \"first\"; f(obj); attr(obj,\"class\")  <- \"second\";}");
    }

    @Test
    public void TestSimpleBuiltins_testUseMethodReturn_1af23cb23456744d7e6a4cb93888e9a3() {
        assertEval("{f <- function(x){ UseMethod(\"f\");cat(\"This should not be executed\"); }; f.second <- function(x){cat(\"f second\",x);}; obj <-1; attr(obj,\"class\")  <- \"second\"; f(obj);}");
    }

    @Test
    public void TestSimpleBuiltins_testUseMethodSimple_3daab073549d57abf4b3cece0fae9dd2() {
        assertEval("{f <- function(x){ UseMethod(\"f\",x); };f.first <- function(x){cat(\"f first\",x)};f.second <- function(x){cat(\"f second\",x)};obj <-1;attr(obj,\"class\")  <- \"first\";f(obj);attr(obj,\"class\")  <- \"second\";}");
    }

    @Test
    public void TestSimpleBuiltins_testVectorConstructor_629fc5f98d9d6659735740d0b0894210() {
        assertEval("{ vector() }");
    }

    @Test
    public void TestSimpleBuiltins_testVectorConstructor_2e5c4cbba72ce650f6121a120e852297() {
        assertEval("{ vector(\"integer\") }");
    }

    @Test
    public void TestSimpleBuiltins_testVectorConstructor_57cd36b68776561d2902b2e76a15bd6b() {
        assertEval("{ vector(\"numeric\") }");
    }

    @Test
    public void TestSimpleBuiltins_testVectorConstructor_d015a194b16ec70f1861f3e0a5e36ece() {
        assertEval("{ vector(\"numeric\", length=4) }");
    }

    @Test
    public void TestSimpleBuiltins_testVectorConstructorIgnore_4c533a47811eec5d654d8bc9cada841a() {
        assertEval("{ vector(length=3) }");
    }

    @Test
    public void TestSimpleBuiltins_testWhich_abb40fde89cc0dfbb69ec73c399e9ee0() {
        assertEval("{ which(c(TRUE, FALSE, NA, TRUE)) }");
    }

    @Test
    public void TestSimpleBuiltins_testWhich_910e4cd5226a6cd85c417d837cfe28d5() {
        assertEval("{ which(logical()) }");
    }

    @Test
    public void TestSimpleBuiltins_testWhichIgnore_6d01b8ef11e5cdf979ca7122cd3de717() {
        assertEval("{ which(c(a=TRUE,b=FALSE,c=TRUE)) }");
    }

    @Test
    public void TestSimpleBuiltins_testWorkingDirectory_4dea13731bbc2e14f050d3a8c9270396() {
        assertEval("{ cur <- getwd(); cur1 <- setwd(getwd()) ; cur2 <- getwd() ; cur == cur1 && cur == cur2 }");
    }

    @Test
    public void TestSimpleBuiltins_testWorkingDirectory_4158e8f80f9f54af9ceaf07aaacc8395() {
        assertEval("{ cur <- getwd(); cur1 <- setwd(c(cur, \"dummy\")) ; cur2 <- getwd() ; cur == cur1  }");
    }

    @Test
    public void TestSimpleBuiltins_testWorkingDirectory_b06c73943c7300d6a0af95bb6d4140c3() {
        assertEvalError("{ setwd(1) }");
    }

    @Test
    public void TestSimpleBuiltins_testWorkingDirectory_d4bb5261e83943081702a1fb0f063135() {
        assertEvalError("{ setwd(character()) }");
    }

    @Test
    public void TestSimpleComparison_testAttributes_43e8804bc516ad5a6ddc0d408d8c9913() {
        assertEval("{ x<-1:4; names(x)<-101:104; x < 7 }");
    }

    @Test
    public void TestSimpleComparison_testAttributes_55604a8c1b8ed77407219b71acf218a7() {
        assertEval("{ x<-1:4; names(x)<-101:104; y<-21:24; names(y)<-121:124; x < y }");
    }

    @Test
    public void TestSimpleComparison_testAttributes_acc0a10d9af45ef9c8f80f3a8090c44d() {
        assertEval("{ x<-1:4; names(x)<-101:104; y<-21:28; names(y)<-121:128; x < y }");
    }

    @Test
    public void TestSimpleComparison_testAttributes_ec1ec490aaa95ff5d15c4df04eb16052() {
        assertEval("{ x<-1:4; names(x)<-101:104; attr(x, \"foo\")<-\"foo\"; attributes(x < 7) }");
    }

    @Test
    public void TestSimpleComparison_testAttributes_d9a40d1f533bc1a4d492ed3b321e8cc6() {
        assertEval("{ x<-1:4; names(x)<-101:104; attr(x, \"foo\")<-\"foo\"; y<-21:24; names(y)<-121:124; attributes(x < y) }");
    }

    @Test
    public void TestSimpleComparison_testAttributes_86ec5ccaff37802e5e9eecaff9bdccbd() {
        assertEval("{ x<-1:4; y<-21:24; names(y)<-121:124; attributes(x > y) }");
    }

    @Test
    public void TestSimpleComparison_testAttributes_72884c59bcf48691861df94fcde4ede4() {
        assertEval("{ x<-1:4; names(x)<-101:104; y<-21:28; names(y)<-121:128;  attributes(y > x) }");
    }

    @Test
    public void TestSimpleComparison_testAttributes_290da18d0f64bf5efc43d773015bf354() {
        assertEval("{ x<-1:4; names(x)<-101:104; y<-21:28; attributes(x > y) }");
    }

    @Test
    public void TestSimpleComparison_testAttributes_12d97a2c216dbef22bd74a1060756b30() {
        assertEvalError("{ x<-1:4; dim(x)<-c(2,2); y<-21:28; x > y }");
    }

    @Test
    public void TestSimpleComparison_testMatrices_eb091ba085dda60b02299905b6603cba() {
        assertEval("{ matrix(1) > matrix(2) }");
    }

    @Test
    public void TestSimpleComparison_testMatrices_e08838ffe9812e3d1cb041aaddec856a() {
        assertEval("{ matrix(1) > NA }");
    }

    @Test
    public void TestSimpleComparison_testMatrices_6e89d79b793dfb2076088167e168c6e0() {
        assertEval("{ m <- matrix(1:6, nrow=2) ; m > c(1,2,3) }");
    }

    @Test
    public void TestSimpleComparison_testScalars_c57b3960b9bc1110a87a7aefe679dc2e() {
        assertEval("{ 1==1 }");
    }

    @Test
    public void TestSimpleComparison_testScalars_62c8da2c4151930a77bd1c3ae7840070() {
        assertEval("{ 2==1 }");
    }

    @Test
    public void TestSimpleComparison_testScalars_f7d6800fb1216ab82bd54f7711fc12a4() {
        assertEval("{ 1L<=1 }");
    }

    @Test
    public void TestSimpleComparison_testScalars_4580017a02ca6b0682acbb490f1ca329() {
        assertEval("{ 1<=0L }");
    }

    @Test
    public void TestSimpleComparison_testScalars_9361da3b54a706f3099267596285d0d6() {
        assertEval("{ x<-2; f<-function(z=x) { if (z<=x) {z} else {x} } ; f(1.4)}");
    }

    @Test
    public void TestSimpleComparison_testScalars_c3ffcceee1f4c9fdeb43c11560b6369f() {
        assertEval("{ 1L==1 }");
    }

    @Test
    public void TestSimpleComparison_testScalars_baae6eb76e3e6105da77704a11ce7a72() {
        assertEval("{ TRUE==1 }");
    }

    @Test
    public void TestSimpleComparison_testScalars_ec050a6f818956b64a61a809c498549e() {
        assertEval("{ TRUE==1L }");
    }

    @Test
    public void TestSimpleComparison_testScalars_8711a9c57987677525115f4e22798f1f() {
        assertEval("{ 2L==TRUE }");
    }

    @Test
    public void TestSimpleComparison_testScalars_650eda6fa64d3e5c362d4357b26db222() {
        assertEval("{ TRUE==FALSE }");
    }

    @Test
    public void TestSimpleComparison_testScalars_86ad3e49f2dc79c55e14862ac3fd438c() {
        assertEval("{ FALSE<=TRUE }");
    }

    @Test
    public void TestSimpleComparison_testScalars_2ab47f400d51cbbbd10b95e5b7a4dd4b() {
        assertEval("{ FALSE<TRUE }");
    }

    @Test
    public void TestSimpleComparison_testScalars_c6f9be6be9538032c49c5d95d09cbbd8() {
        assertEval("{ TRUE>FALSE }");
    }

    @Test
    public void TestSimpleComparison_testScalars_5edef2d93f42c0909076c653c4ad7eb5() {
        assertEval("{ TRUE>=FALSE }");
    }

    @Test
    public void TestSimpleComparison_testScalars_0c8d72da028d42199fb944c364c553d9() {
        assertEval("{ TRUE!=FALSE }");
    }

    @Test
    public void TestSimpleComparison_testScalars_29907cc9eed21ce410248b8be48dfbd1() {
        assertEval("{ 2L==NA }");
    }

    @Test
    public void TestSimpleComparison_testScalars_cf07dc5b7b65e07deb74eaeb7bb9531f() {
        assertEval("{ NA==2L }");
    }

    @Test
    public void TestSimpleComparison_testScalars_1da0f6d4e09bace87d9266ce02b7ede7() {
        assertEval("{ 1==NULL }");
    }

    @Test
    public void TestSimpleComparison_testScalars_8bb2a07a7b73c039598160a4bd77a9cc() {
        assertEval("{ 2L==as.double(NA) }");
    }

    @Test
    public void TestSimpleComparison_testScalars_bf458c846f319a88d5cc395eab547fb0() {
        assertEval("{ as.double(NA)==2L }");
    }

    @Test
    public void TestSimpleComparison_testScalars_a523d441174f5067f5e7ecc64668761b() {
        assertEval("{ 0/0 <= 2 }");
    }

    @Test
    public void TestSimpleComparison_testScalars_24db53f2006747c190b7ced9648169a5() {
        assertEval("{ 1+1i == TRUE }");
    }

    @Test
    public void TestSimpleComparison_testScalars_a1bed396542abb86df5d5a39a87ce157() {
        assertEval("{ 1+1i == 1 }");
    }

    @Test
    public void TestSimpleComparison_testScalars_95443d797f8d9a84bad1be61f05c9f96() {
        assertEval("{ 1+0i == 1 }");
    }

    @Test
    public void TestSimpleComparison_testScalars_2c52a57bde1b6efe85a533df5821cc9c() {
        assertEval("{ \"-1+1i\" > \"1+1i\" }");
    }

    @Test
    public void TestSimpleComparison_testScalars_68f2b0b612cbf7c62b177793a372d2c3() {
        assertEval("{ \"-1+1i\" > 1+1i }");
    }

    @Test
    public void TestSimpleComparison_testScalars_62c53267190fa608a7b2fa53628ae8bc() {
        assertEval("{ \"+1+1i\" > 1+1i }");
    }

    @Test
    public void TestSimpleComparison_testScalars_d40fbf45aa8c96f39511e95058101a5f() {
        assertEval("{ \"1+2i\" > 1+1i }");
    }

    @Test
    public void TestSimpleComparison_testScalars_6c7471ecde9943bc49336a0aab793962() {
        assertEval("{ \"1+1.1i\" == 1+1.1i }");
    }

    @Test
    public void TestSimpleComparison_testScalars_65b6edd6dbb64a0ba00c85ceae430efe() {
        assertEval("{ \"1+1.100i\" == 1+1.100i }");
    }

    @Test
    public void TestSimpleComparison_testScalars_9c105a4ac408d983b6d0e86e8565fdc0() {
        assertEvalError("{ x<-1+1i; x > FALSE }");
    }

    @Test
    public void TestSimpleComparison_testScalarsAsFunction_ea932904f46d5ab6e8f4c341c9e4900f() {
        assertEval("{ f <- function(a,b) { a > b } ; f(1,2) ; f(1L,2) }");
    }

    @Test
    public void TestSimpleComparison_testScalarsAsFunction_30260787eea963c4b167a97727856b3d() {
        assertEval("{ f <- function(a,b) { a > b } ; f(1,2) ; f(1,2L) }");
    }

    @Test
    public void TestSimpleComparison_testScalarsAsFunction_118cb009842442742a8af01e8efc9cbd() {
        assertEval("{ f <- function(a,b) { a > b } ; f(1L,2L) ; f(1,2) }");
    }

    @Test
    public void TestSimpleComparison_testScalarsAsFunction_c2d59702269fb2dc016be012d4109162() {
        assertEval("{ f <- function(a,b) { a > b } ; f(1L,2L) ; f(1L,2) }");
    }

    @Test
    public void TestSimpleComparison_testScalarsAsFunction_1abe98ee86dfe375a378e6ee87f1c231() {
        assertEval("{ f <- function(a,b) { a > b } ; f(1L,2) ; f(1,2) }");
    }

    @Test
    public void TestSimpleComparison_testScalarsAsFunction_7f348d188fb89cab8e4fcc797ed5ce81() {
        assertEval("{ f <- function(a,b) { a > b } ; f(1L,2) ; f(1L,2L) }");
    }

    @Test
    public void TestSimpleComparison_testScalarsAsFunction_c6b70d6a700575f9f6685a428176393e() {
        assertEval("{ f <- function(a,b) { a > b } ; f(1,2L) ; f(1,2) }");
    }

    @Test
    public void TestSimpleComparison_testScalarsAsFunction_a8c40fd4aa5f122323341a249772d459() {
        assertEval("{ f <- function(a,b) { a > b } ; f(1,2L) ; f(1L,2L) }");
    }

    @Test
    public void TestSimpleComparison_testScalarsAsFunction_70d3713cb0644c59c00e290f9b7610fe() {
        assertEval("{ f <- function(a,b) { a > b } ; f(TRUE,FALSE) ; f(TRUE,2) }");
    }

    @Test
    public void TestSimpleComparison_testScalarsAsFunction_3239e355a8c583acccdad510f5ed99e3() {
        assertEval("{ f <- function(a,b) { a > b } ; f(TRUE,FALSE) ; f(1L,2L) }");
    }

    @Test
    public void TestSimpleComparison_testScalarsAsFunction_88e2bf2a44a83d8b87b9b6d0539f11b8() {
        assertEval("{ f <- function(a,b) { a > b } ; f(0L,TRUE) ; f(FALSE,2) }");
    }

    @Test
    public void TestSimpleComparison_testScalarsAsFunction_fdc0fe6a512c2693d94dbb4660445ceb() {
        assertEval("{ f <- function(a,b) { a > b } ; f(0L,TRUE) ; f(0L,2L) }");
    }

    @Test
    public void TestSimpleComparison_testScalarsAsFunction_d56011fcc535a45680ee58c0443a441b() {
        assertEval("{ f <- function(a,b) { a > b } ; f(0L,TRUE) ; f(2L,TRUE) }");
    }

    @Test
    public void TestSimpleComparison_testScalarsAsFunction_fbcba5d1da9fe3f0451728150a013f62() {
        assertEval("{ f <- function(a,b) { a > b } ; f(TRUE,2L) ; f(FALSE,2) }");
    }

    @Test
    public void TestSimpleComparison_testScalarsAsFunction_a731a517b15410c7d3a548640c6944f5() {
        assertEval("{ f <- function(a,b) { a > b } ; f(TRUE,2L) ; f(0L,2L) }");
    }

    @Test
    public void TestSimpleComparison_testScalarsComplex_460261d3121f9bd5e133e31b24dfcd7c() {
        assertEval("{ 1+1i == 1-1i }");
    }

    @Test
    public void TestSimpleComparison_testScalarsComplex_a5bcb421bda8809d72505b38996276fb() {
        assertEval("{ 1+1i == 1+1i }");
    }

    @Test
    public void TestSimpleComparison_testScalarsComplex_b748817aa0c4dce43cae298e11ab2422() {
        assertEval("{ 1+1i == 2+1i }");
    }

    @Test
    public void TestSimpleComparison_testScalarsComplex_723a7acf9b656144d2ebed9838df2e62() {
        assertEval("{ 1+1i != 1+1i }");
    }

    @Test
    public void TestSimpleComparison_testScalarsComplex_287627257ace98bea5f95da723b85689() {
        assertEval("{ 1+1i != 1-1i }");
    }

    @Test
    public void TestSimpleComparison_testScalarsComplex_432f5a35da7b91d3b17e3c5747664156() {
        assertEval("{ 1+1i != 2+1i }");
    }

    @Test
    public void TestSimpleComparison_testScalarsIgnore_7a6557e91f8f198b6c11c29c4e572f57() {
        assertEval("{ z <- TRUE; dim(z) <- c(1) ; dim(z == TRUE) }");
    }

    @Test
    public void TestSimpleComparison_testScalarsIgnore_6f066c83dbb9ff430a0d5056100fbb50() {
        assertEvalError("{ z <- TRUE; dim(z) <- c(1) ; u <- 1:3 ; dim(u) <- 3 ; u == z }");
    }

    @Test
    public void TestSimpleComparison_testScalarsNA_6f86e642ea4cd41169f92247190536c9() {
        assertEval("{ a <- 1L ; b <- a[2] ; a == b }");
    }

    @Test
    public void TestSimpleComparison_testScalarsNA_b3cd9df88a78f5b775407d666abd4751() {
        assertEval("{ a <- 1L ; b <- a[2] ; b > a }");
    }

    @Test
    public void TestSimpleComparison_testScalarsNA_09b840f7af956531110609efb16473a9() {
        assertEval("{ a <- 1L ; b <- 1[2] ; a == b }");
    }

    @Test
    public void TestSimpleComparison_testScalarsNA_948a2d658fc84c47fac00fa7031bee2a() {
        assertEval("{ a <- 1L[2] ; b <- 1 ; a == b }");
    }

    @Test
    public void TestSimpleComparison_testScalarsNA_f55ceacfd2124bc2037aec01ca352ece() {
        assertEval("{ a <- 1L[2] ; b <- 1 ; b > a }");
    }

    @Test
    public void TestSimpleComparison_testScalarsNA_6fa34b9cf225f42bad64051b48591319() {
        assertEval("{ a <- 1 ; b <- 1L[2] ; a == b }");
    }

    @Test
    public void TestSimpleComparison_testScalarsNA_e1fd2c4d514fe0b6965eb3744a6ccf32() {
        assertEval("{ a <- 1[2] ; b <- 1L ; b > a }");
    }

    @Test
    public void TestSimpleComparison_testScalarsNA_d68d193e8d7def6b0ce9d4eda9bfe2e0() {
        assertEval("{ a <- 1L[2] ; b <- TRUE ; a != b }");
    }

    @Test
    public void TestSimpleComparison_testScalarsNA_d77950c0fdd260db632e7af4972b6419() {
        assertEval("{ a <- TRUE ; b <- 1L[2] ; a > b }");
    }

    @Test
    public void TestSimpleComparison_testScalarsNA_6484d2c61759ff69ec6ec85822ea29c5() {
        assertEval("{ a <- 1 ; b <- a[2] ; a == b }");
    }

    @Test
    public void TestSimpleComparison_testScalarsNA_c0ffd4204ace47b2bee2b81b1460336f() {
        assertEval("{ a <- 1 ; b <- a[2] ; b > a }");
    }

    @Test
    public void TestSimpleComparison_testScalarsNAAsFunction_1a5c9371d7fafab23d6dfd01ba9ddc6e() {
        assertEval("{ f <- function(a,b) { a > b } ; f(1,2) ; f(1L,2) ; f(2, 1L[2]) }");
    }

    @Test
    public void TestSimpleComparison_testScalarsNAAsFunction_39fc333bd0a2a4bf9d7b8ebbade7ce8b() {
        assertEval("{ f <- function(a,b) { a > b } ; f(1,2) ; f(1L,2) ; f(2[2], 1L) }");
    }

    @Test
    public void TestSimpleComparison_testScalarsNAAsFunction_71f97d59e0e728318cfeee50a6547ff3() {
        assertEval("{ f <- function(a,b) { a > b } ; f(1,2) ; f(1L,2) ; f(2L, 1[2]) }");
    }

    @Test
    public void TestSimpleComparison_testScalarsNAAsFunction_613d54c4780f3499cdd6b61409d8dac7() {
        assertEval("{ f <- function(a,b) { a > b } ; f(1,2) ; f(1L,2) ; f(2L[2], 1) }");
    }

    @Test
    public void TestSimpleComparison_testScalarsNAAsFunction_5e0bed2ee593f7831a040ae01a0e5845() {
        assertEval("{ f <- function(a,b) { a > b } ; f(1,2) ; f(1L,2) ; f(2L, 1L[2]) }");
    }

    @Test
    public void TestSimpleComparison_testScalarsNAAsFunction_c51baa904ffc0e6d77744cb835910692() {
        assertEval("{ f <- function(a,b) { a > b } ; f(1,2) ; f(1L,2) ; f(2L[2], 1L) }");
    }

    @Test
    public void TestSimpleComparison_testScalarsNAAsFunction_677dd5f0f60307ba054080d43ad8c070() {
        assertEval("{ f <- function(a,b) { a > b } ; f(1,2) ; f(1L,2) ; f(2, 1[2]) }");
    }

    @Test
    public void TestSimpleComparison_testScalarsNAAsFunction_4a4d07e745eb0f450e82004e044e8708() {
        assertEval("{ f <- function(a,b) { a > b } ; f(1,2) ; f(1L,2) ; f(2[2], 1) }");
    }

    @Test
    public void TestSimpleComparison_testScalarsNAAsFunctionIgnore_0a500b31b16f008e4a1dc5b5630344c8() {
        assertEval("{ f <- function(a,b) { a > b } ; f(1,2) ; f(1L,2) ; f(\"hello\", \"hi\"[2]) }");
    }

    @Test
    public void TestSimpleComparison_testScalarsNAAsFunctionIgnore_c803d5d2a05362ff97b2237e3502ac08() {
        assertEval("{ f <- function(a,b) { a > b } ; f(1,2) ; f(1L,2) ; f(\"hello\"[2], \"hi\") }");
    }

    @Test
    public void TestSimpleComparison_testScalarsNAIgnore_5d82706c2baa41a30419736895aecb0c() {
        assertEval("{ a <- 1L ; b <- TRUE[2] ; a == b }");
    }

    @Test
    public void TestSimpleComparison_testScalarsNAIgnore_6617a42ac54ed9cdf434eee9b0c67e30() {
        assertEval("{ a <- TRUE[2] ; b <- 1L ; a == b }");
    }

    @Test
    public void TestSimpleComparison_testScalarsRaw_745754e97e9a262cbcb578df6ea5a597() {
        assertEval("{ as.raw(15) > as.raw(10) }");
    }

    @Test
    public void TestSimpleComparison_testScalarsRaw_67344179521653c0f000fe3687292695() {
        assertEval("{ as.raw(15) < as.raw(10) }");
    }

    @Test
    public void TestSimpleComparison_testScalarsRaw_70adf7ed9b21dad0f12c1699269f3d14() {
        assertEval("{ as.raw(15) >= as.raw(10) }");
    }

    @Test
    public void TestSimpleComparison_testScalarsRaw_8051d65b72fa2f4f5f9959d8f778b907() {
        assertEval("{ as.raw(15) <= as.raw(10) }");
    }

    @Test
    public void TestSimpleComparison_testScalarsRaw_c5d8b8eaa8f0804851dc74720412a3aa() {
        assertEval("{ as.raw(10) >= as.raw(15) }");
    }

    @Test
    public void TestSimpleComparison_testScalarsRaw_858d4f893aa1f9d75d2d798de4c5d0c8() {
        assertEval("{ as.raw(10) <= as.raw(15) }");
    }

    @Test
    public void TestSimpleComparison_testScalarsRaw_9690af70422eec5d5628a43e0579b499() {
        assertEval("{ as.raw(15) == as.raw(10) }");
    }

    @Test
    public void TestSimpleComparison_testScalarsRaw_185702e534b9cc50a42fd1423fb8731a() {
        assertEval("{ as.raw(15) != as.raw(10) }");
    }

    @Test
    public void TestSimpleComparison_testScalarsRaw_5a43eaa94d30a62670d9c4f9508eda0a() {
        assertEval("{ as.raw(15) == as.raw(15) }");
    }

    @Test
    public void TestSimpleComparison_testScalarsRaw_c9f5c2ed289315961f63160e31ddaee3() {
        assertEval("{ as.raw(15) != as.raw(15) }");
    }

    @Test
    public void TestSimpleComparison_testScalarsRaw_e15accefdfe5ecee7e42890714068769() {
        assertEval("{ a <- as.raw(1) ; b <- as.raw(2) ; a < b }");
    }

    @Test
    public void TestSimpleComparison_testScalarsRaw_2e2b707984fb012a41b81c30e2cb7fdc() {
        assertEval("{ a <- as.raw(1) ; b <- as.raw(2) ; a > b }");
    }

    @Test
    public void TestSimpleComparison_testScalarsRaw_454ac1889ae1618d4a40944efeb90395() {
        assertEval("{ a <- as.raw(1) ; b <- as.raw(2) ; a == b }");
    }

    @Test
    public void TestSimpleComparison_testScalarsRaw_bb5c978a513f7370356299239e540488() {
        assertEval("{ a <- as.raw(1) ; b <- as.raw(200) ; a < b }");
    }

    @Test
    public void TestSimpleComparison_testScalarsRaw_ea123daf7376398e349f8a4f3963c5a0() {
        assertEval("{ a <- as.raw(200) ; b <- as.raw(255) ; a < b }");
    }

    @Test
    public void TestSimpleComparison_testScalarsStrings_660e099f7d35223cab2c47f270f3ca51() {
        assertEval("\"hello\" != \"hello\"");
    }

    @Test
    public void TestSimpleComparison_testScalarsStrings_ab62d59883f3a31429aeff772acabfbb() {
        assertEval("\"hello\" == \"hello\"");
    }

    @Test
    public void TestSimpleComparison_testScalarsStrings_5b44b63b0db8a62cbe38a38028208533() {
        assertEval("\"hello\" >= \"hi\"");
    }

    @Test
    public void TestSimpleComparison_testScalarsStrings_2b9ce8be720704511b1cd5e85417cc6e() {
        assertEval("\"hello\" <= \"hi\"");
    }

    @Test
    public void TestSimpleComparison_testScalarsStrings_f7f5db7faf46a83d61dd071c1d993750() {
        assertEval("\"hi\" != \"hello\"");
    }

    @Test
    public void TestSimpleComparison_testScalarsStrings_e553b723480e788fb4dcf1223fd05135() {
        assertEval("\"hi\" == \"hello\"");
    }

    @Test
    public void TestSimpleComparison_testScalarsStrings_0a4890140fcfc7a6776a53014a11f094() {
        assertEval("\"hi\" > \"hello\"");
    }

    @Test
    public void TestSimpleComparison_testScalarsStrings_49acb7b74f41c6b00fe740305275999e() {
        assertEval("\"hi\" < \"hello\"");
    }

    @Test
    public void TestSimpleComparison_testScalarsStrings_8e0c6a770076a82aec26ca391a6102c7() {
        assertEval("\"hi\" >= \"hello\"");
    }

    @Test
    public void TestSimpleComparison_testScalarsStrings_ae5fdca13d6ab9f3b21b6d019f765fe6() {
        assertEval("\"hi\" <= \"hello\"");
    }

    @Test
    public void TestSimpleComparison_testScalarsStrings_068141bfb17a29f139d636b51c0f04e1() {
        assertEval("\"hello\" > \"hi\"");
    }

    @Test
    public void TestSimpleComparison_testScalarsStrings_2f86f96eeb15e4417370c5adb99bc243() {
        assertEval("\"hello\" < \"hi\"");
    }

    @Test
    public void TestSimpleComparison_testScalarsStrings_84c518c6d75c33f1ade9e8abc02aa89b() {
        assertEval("{ \"a\" <= \"b\" }");
    }

    @Test
    public void TestSimpleComparison_testScalarsStrings_ffa57e69466e00ed8b5d03c71c9c1754() {
        assertEval("{ \"a\" > \"b\" }");
    }

    @Test
    public void TestSimpleComparison_testScalarsStrings_c2f69fa84ec0de7f5027924adc60e333() {
        assertEval("{ \"2.0\" == 2 }");
    }

    @Test
    public void TestSimpleComparison_testVectors_06addc50d40cb6d3f37355ccf3a3308b() {
        assertEval("{ x<-c(1,2,3,4);y<-2.5; x<=y }");
    }

    @Test
    public void TestSimpleComparison_testVectors_b08d83b7ee9e50867f4804b554e0f3b1() {
        assertEval("{ x<-c(1L,2L,3L,4L);y<-1.5; x<=y }");
    }

    @Test
    public void TestSimpleComparison_testVectors_1f559fcd37aff5a2e4a201d844fcad76() {
        assertEval("{ c(1:3,4,5)==1:5 }");
    }

    @Test
    public void TestSimpleComparison_testVectors_bcf7f061ed0734abf33e39700f98e0df() {
        assertEval("{ 3 != 1:2 }");
    }

    @Test
    public void TestSimpleComparison_testVectors_96ea43b843b9051fa554615a1dbaf2cb() {
        assertEval("{ b <- 1:3 ; z <- FALSE ; b[2==2] }");
    }

    @Test
    public void TestSimpleComparison_testVectors_3ab069b54b0715af8f86b7241b226e7d() {
        assertEval("{ c(1,2,NA,4) != 2 }");
    }

    @Test
    public void TestSimpleComparison_testVectors_5a549c10c86b94e63cf90ab2224191d7() {
        assertEval("{ c(1,2,NA,4) == 2 }");
    }

    @Test
    public void TestSimpleComparison_testVectors_37fb84a8772716ea44a0777ddfddb361() {
        assertEval("{ x<-c(FALSE,TRUE);y<-c(TRUE,FALSE); x<y }");
    }

    @Test
    public void TestSimpleComparison_testVectors_aefd0cf2c9c4e4a7573f1c892547923c() {
        assertEval("{ x<-c(FALSE,TRUE, FALSE, FALSE);y<-c(TRUE,FALSE); x<y }");
    }

    @Test
    public void TestSimpleComparison_testVectors_1b694b3d6e6aa70144fed0c9f897552d() {
        assertEval("{ x<-c(\"0\",\"1\");y<-c(\"a\",\"-1\"); x<y }");
    }

    @Test
    public void TestSimpleComparison_testVectors_d6bb13211cfd38d94f46e84ddc403f07() {
        assertEval("{ x<-c(\"0\",\"1\",\"-1\", \"2\");y<-c(\"a\",\"-1\", \"0\", \"2\"); x<y }");
    }

    @Test
    public void TestSimpleComparison_testVectors_80c345536ca058a10cd69852aa174300() {
        assertEval("{ x<-c(10,3);y<-c(10,2); x<=y }");
    }

    @Test
    public void TestSimpleComparison_testVectors_ae14786274e66f83b38b9cb4c4ffdd2d() {
        assertEval("{ x<-c(10L,3L);y<-c(10L,2L); x<=y }");
    }

    @Test
    public void TestSimpleComparison_testVectors_92cb905f8c166a0bed29234ec978a7f9() {
        assertEval("{ x<-c(10L,3L);y<-c(10,2); x<=y }");
    }

    @Test
    public void TestSimpleComparison_testVectors_a2fe3477f345d727ca7098d3afa55c0a() {
        assertEval("{ x<-c(10,3);y<-c(10L,2L); x<=y }");
    }

    @Test
    public void TestSimpleComparison_testVectors_929f2b89efd8cd95c4db8a235fd3cffe() {
        assertEval("{ x<-c(1,2,3,4);y<-c(10,2); x<=y }");
    }

    @Test
    public void TestSimpleComparison_testVectors_ac182f5f2fafd01f7d36101b7f4d3d66() {
        assertEval("{ x<-c(1,2,3,4);y<-c(2.5+NA,2.5); x<=y }");
    }

    @Test
    public void TestSimpleComparison_testVectors_26398ba64d396ff0ade552b9c316f669() {
        assertEval("{ x<-c(1L,2L,3L,4L);y<-c(2.5+NA,2.5); x<=y }");
    }

    @Test
    public void TestSimpleComparison_testVectors_facbf15afbc39c4f340b08afb938e1e2() {
        assertEval("{ x<-c(10,1,3);y<-4:6; x<=y }");
    }

    @Test
    public void TestSimpleComparison_testVectors_d0676decfc816c03f51a10a6530837aa() {
        assertEval("{ x<-5;y<-4:6; x<=y }");
    }

    @Test
    public void TestSimpleComparison_testVectors_d7751c91e8041d0015d2e40e10378aea() {
        assertEval("{ x<-c(1L,2L,3L,4L);y<-c(TRUE,FALSE); x<=y }");
    }

    @Test
    public void TestSimpleComparison_testVectors_d47055aa7714ecd836723624dd73a502() {
        assertEval("{ 0/0 == c(1,2,3,4) }");
    }

    @Test
    public void TestSimpleComparison_testVectors_e8136d0f5638ef6ebfa22f19ed4a1334() {
        assertEval("{ 1:3 == TRUE }");
    }

    @Test
    public void TestSimpleComparison_testVectors_18016242b408fe8ec7beefcee04f5018() {
        assertEval("{ TRUE == 1:3 }");
    }

    @Test
    public void TestSimpleComparison_testVectors_fa567fef5fdaed2dd27153ccf630f33e() {
        assertEval("{ as.raw(c(2,1,4)) < raw() }");
    }

    @Test
    public void TestSimpleComparison_testVectors_11ec07553cf71700cb1a8f707bfddb92() {
        assertEval("{ raw() < as.raw(c(2,1,4)) }");
    }

    @Test
    public void TestSimpleComparison_testVectors_c41baf7b846c132da2591ede447edeae() {
        assertEval("{ 1:3 < integer() }");
    }

    @Test
    public void TestSimpleComparison_testVectors_b95bfd72dda63254f757ebb8492496d1() {
        assertEval("{ integer() < 1:3 }");
    }

    @Test
    public void TestSimpleComparison_testVectors_28f05a590ae0a94a9110a220f3c4ed63() {
        assertEval("{ c(1,2,3) < double() }");
    }

    @Test
    public void TestSimpleComparison_testVectors_fd8355b848459af41a03e5cf89dec4e2() {
        assertEval("{ double() == c(1,2,3) }");
    }

    @Test
    public void TestSimpleComparison_testVectors_5c773c380167f898e4838b1c36605e30() {
        assertEval("{ c(TRUE,FALSE) < logical() }");
    }

    @Test
    public void TestSimpleComparison_testVectors_a22e9601def027ac547c27a444bee966() {
        assertEval("{ logical() == c(FALSE, FALSE) }");
    }

    @Test
    public void TestSimpleComparison_testVectors_c0076b59d10b4a09bc162d10532d0d71() {
        assertEval("{ c(1+2i, 3+4i) == (1+2i)[0] }");
    }

    @Test
    public void TestSimpleComparison_testVectors_075a24f57f0a69aeba347ea5f27703e7() {
        assertEval("{ (1+2i)[0] == c(2+3i, 4+1i) }");
    }

    @Test
    public void TestSimpleComparison_testVectors_0d662e1dcf736eab888ecc52e1cd0823() {
        assertEval("{ c(\"hello\", \"hi\") == character() }");
    }

    @Test
    public void TestSimpleComparison_testVectors_aa19364c3d04eea6d264fb980d5950b5() {
        assertEval("{ character() > c(\"hello\", \"hi\") }");
    }

    @Test
    public void TestSimpleComparison_testVectors_f535cf744b7d506781970ea853b33ee2() {
        assertEval("{ integer() == 2L }");
    }

    @Test
    public void TestSimpleComparison_testVectors_73469418cc9d716326fee1a4b716ba46() {
        assertEval("{ c(1,2,3,4) != c(1,NA) }");
    }

    @Test
    public void TestSimpleComparison_testVectors_519adec753671e6cbe5d3f3c829ec02d() {
        assertEval("{ 2 != c(1,2,NA,4) }");
    }

    @Test
    public void TestSimpleComparison_testVectors_45d3490c976cc101365b950afbf065cd() {
        assertEval("{ 2 == c(1,2,NA,4) }");
    }

    @Test
    public void TestSimpleComparison_testVectors_573719cc9ca5b09f01e86276ea3bd833() {
        assertEval("{ c(\"hello\", NA) < c(\"hi\", NA) }");
    }

    @Test
    public void TestSimpleComparison_testVectors_11c9ba034db1638c86f7a6dd27191408() {
        assertEval("{ c(\"hello\", NA) >= \"hi\" }");
    }

    @Test
    public void TestSimpleComparison_testVectors_d4682649aa1b019290b0395440473db1() {
        assertEval("{ \"hi\" > c(\"hello\", NA)  }");
    }

    @Test
    public void TestSimpleComparison_testVectors_b4d2b07844982ad15ddd24338c044ae0() {
        assertEval("{ c(\"hello\", NA) > c(NA, \"hi\") }");
    }

    @Test
    public void TestSimpleComparison_testVectors_6ac047f42d4e2b7211727c90257e2373() {
        assertEval("{ c(1L, NA) > c(NA, 2L) }");
    }

    @Test
    public void TestSimpleComparison_testVectors_d33264b98a8149fcfd01997e6e956257() {
        assertEval("{ c(TRUE, NA) > c(NA, FALSE) }");
    }

    @Test
    public void TestSimpleComparison_testVectors_4e09b8381d61556f547367705278185f() {
        assertEval("{ \"hi\" > c(\"hello\", \"hi\")  }");
    }

    @Test
    public void TestSimpleComparison_testVectors_af056939709daaaacd6a1decb84f44b6() {
        assertEval("{ NA > c(\"hello\", \"hi\") }");
    }

    @Test
    public void TestSimpleComparison_testVectors_2592ae7a5f7dba232b37a87d9bcb1039() {
        assertEval("{ c(\"hello\", \"hi\") < NA }");
    }

    @Test
    public void TestSimpleComparison_testVectors_04ea379fa8e275629d352b23e9f10eac() {
        assertEval("{ 1:3 < NA }");
    }

    @Test
    public void TestSimpleComparison_testVectors_e1ed5b7a55773862f2bec1cf4250523b() {
        assertEval("{ NA > 1:3 }");
    }

    @Test
    public void TestSimpleComparison_testVectors_0d650e306165b88997184cd42bc774b5() {
        assertEval("{ 2L > c(1L,NA,2L) }");
    }

    @Test
    public void TestSimpleComparison_testVectors_21855af7e2e3cbdb1b4f6fe92c2f12f7() {
        assertEval("{ c(1L,NA,2L) < 2L }");
    }

    @Test
    public void TestSimpleComparison_testVectors_ecd5170aa87a7130b6146c5c2d8052f4() {
        assertEval("{ c(0/0+1i,2+1i) == c(1+1i,2+1i) }");
    }

    @Test
    public void TestSimpleComparison_testVectors_402395e1e90c3ea2885d3ab1963eadfb() {
        assertEval("{ c(1+1i,2+1i) == c(0/0+1i,2+1i) }");
    }

    @Test
    public void TestSimpleComparison_testVectors_b929bf25036bce4293cfa56b6600fc9e() {
        assertEvalError("{ x<-1+1i; y<-2+2i; x > y }");
    }

    @Test
    public void TestSimpleComparison_testVectors_75fcc56085ac7c2bf42f270fcbb6da70() {
        assertEvalError("{ x<-1+1i; y<-2+2i; x < y }");
    }

    @Test
    public void TestSimpleComparison_testVectors_b7c5f6b2b2a224b3f7a4bd82e9dcad9c() {
        assertEvalError("{ x<-1+1i; y<-2+2i; x >= y }");
    }

    @Test
    public void TestSimpleComparison_testVectors_e2dd732681cca7234782698db46a4381() {
        assertEvalError("{ x<-1+1i; y<-2+2i; x <= y }");
    }

    @Test
    public void TestSimpleComparison_testVectors_a8b2d39c5994d50c5b3f2d1cd68d8f00() {
        assertEvalWarning("{ c(1,2) < c(2,1,4) }");
    }

    @Test
    public void TestSimpleComparison_testVectors_d71565099234436b145d5c2f09387ec2() {
        assertEvalWarning("{ c(2,1,4) < c(1,2) }");
    }

    @Test
    public void TestSimpleComparison_testVectors_4a230ac666979515c5cb2ab9b9ecc81e() {
        assertEvalWarning("{ c(1L,2L) < c(2L,1L,4L) }");
    }

    @Test
    public void TestSimpleComparison_testVectors_e3a5091cb1c14ec735a5122c14b23373() {
        assertEvalWarning("{ c(2L,1L,4L) < c(1L,2L) }");
    }

    @Test
    public void TestSimpleComparison_testVectors_a14b99637db3d415c001a1a1d8a01b49() {
        assertEvalWarning("{ c(TRUE,FALSE,FALSE) < c(TRUE,TRUE) }");
    }

    @Test
    public void TestSimpleComparison_testVectors_0cf4453ac9ebe713972ae3540ba0b699() {
        assertEvalWarning("{ c(TRUE,TRUE) == c(TRUE,FALSE,FALSE) }");
    }

    @Test
    public void TestSimpleComparison_testVectors_670388c4d33ea967f5b646ab377d4721() {
        assertEvalWarning("{ as.raw(c(1,2)) < as.raw(c(2,1,4)) }");
    }

    @Test
    public void TestSimpleComparison_testVectors_e04b9cbf42f4808ea119480a4aef6c8f() {
        assertEvalWarning("{ as.raw(c(2,1,4)) < as.raw(c(1,2)) }");
    }

    @Test
    public void TestSimpleComparison_testVectors_6b045b9386eec4c7715468037361cb80() {
        assertEvalWarning("{ c(\"hi\",\"hello\",\"bye\") > c(\"cau\", \"ahoj\") }");
    }

    @Test
    public void TestSimpleComparison_testVectors_8888b6b7fbc963f99246b5d9dc32cb32() {
        assertEvalWarning("{ c(\"cau\", \"ahoj\") != c(\"hi\",\"hello\",\"bye\") }");
    }

    @Test
    public void TestSimpleComparison_testVectors_7464bb8d120f9d68e3c643e1f30c92f0() {
        assertEvalWarning("{ c(1+1i,2+2i) == c(2+1i,1+2i,1+1i) }");
    }

    @Test
    public void TestSimpleComparison_testVectors_2d97edfc3c6f35bfdf51f33bf24e90e4() {
        assertEvalWarning("{ c(2+1i,1+2i,1+1i) == c(1+1i, 2+2i) }");
    }

    @Test
    public void TestSimpleComparison_testVectorsIgnore_9ad8bb825e6c5d11db011ae03b0c67c1() {
        assertEvalError("{ m <- matrix(nrow=2, ncol=2, 1:4) ; m == 1:16 }");
    }

    @Test
    public void TestSimpleFunctions_testBinding_08912db0fc81d6f3582a954d1f9c1fa5() {
        assertEval("{ myapp <- function(f, x, y) { f(x,y) } ; myapp(function(x,y) { x + y }, 1, 2) ; myapp(sum, 1, 2) }");
    }

    @Test
    public void TestSimpleFunctions_testBinding_fc01e818513a47ce3fa39da117b9ac28() {
        assertEval("{ myapp <- function(f, x, y) { f(x,y) } ; myapp(f = function(x,y) { x + y }, y = 1, x = 2) ; myapp(f = sum, x = 1, y = 2) }");
    }

    @Test
    public void TestSimpleFunctions_testBinding_fecf4504d25618313f1c52088b7c198b() {
        assertEval("{ myapp <- function(f, x, y) { f(x,y) } ; myapp(f = function(x,y) { x + y }, y = 1, x = 2) ; myapp(f = sum, x = 1, y = 2) ; myapp(f = c, y = 10, x = 3) }");
    }

    @Test
    public void TestSimpleFunctions_testBinding_90e51346b1d54ff0902be55151d21579() {
        assertEval("{ myapp <- function(f, x, y) { f(x,y) } ; myapp(f = function(x,y) { x + y }, y = 1, x = 2) ; myapp(f = sum, x = 1, y = 2) ; myapp(f = function(x,y) { x - y }, y = 10, x = 3) }");
    }

    @Test
    public void TestSimpleFunctions_testBinding_4be58635cffea4322af94a94aed6a1e2() {
        assertEval("{ myapp <- function(f, x, y) { f(x,y) } ; g <- function(x,y) { x + y } ; myapp(f = g, y = 1, x = 2) ; myapp(f = sum, x = 1, y = 2) ; myapp(f = g, y = 10, x = 3) ;  myapp(f = g, y = 11, x = 2) }");
    }

    @Test
    public void TestSimpleFunctions_testBinding_2e492f8c676e04888266ff99773b4d55() {
        assertEval("{ f <- function(i) { if (i==2) { c <- sum }; c(1,2) } ; f(1) ; f(2) }");
    }

    @Test
    public void TestSimpleFunctions_testBinding_085f773e2b6a619cfea783e918127454() {
        assertEval("{ f <- function(i) { if (i==2) { assign(\"c\", sum) }; c(1,2) } ; f(1) ; f(2) }");
    }

    @Test
    public void TestSimpleFunctions_testBinding_9ac77652c1f4231a6a7f8b32c14b091f() {
        assertEval("{ f <- function(func, arg) { func(arg) } ; f(sum, c(3,2)) ; f(length, 1:4) }");
    }

    @Test
    public void TestSimpleFunctions_testBinding_1f9d9445f99a6c0120c6576de385fc65() {
        assertEval("{ f <- function(func, arg) { func(arg) } ; f(sum, c(3,2)) ; f(length, 1:4) ; f(length,1:3) }");
    }

    @Test
    public void TestSimpleFunctions_testBinding_033ab51f0cc168f7755dcc1684fa33f4() {
        assertEval("{ f <- function(func, arg) { func(arg) } ; f(sum, c(3,2)) ; f(length, 1:4) ; f(function(i) {3}, 1) ; f(length,1:3) }");
    }

    @Test
    public void TestSimpleFunctions_testBinding_62f4a159f7cdfcf03b705626884a0c4e() {
        assertEval("{ f <- function(func, a) { if (func(a)) { 1 } else { 2 } } ; f(function(x) {TRUE}, 5) ; f(is.na, 4) }");
    }

    @Test
    public void TestSimpleFunctions_testBinding_0e3d8f0fa257cca99b618d85b34167e3() {
        assertEval("{ f <- function(func, a) { if (func(a)) { 1 } else { 2 } } ; g <- function(x) {TRUE} ; f(g, 5) ; f(is.na, 4) ; f(g, 3) }");
    }

    @Test
    public void TestSimpleFunctions_testBinding_5c7a9b0c548ba8536d1a8c68ef5825e1() {
        assertEval("{ f <- function(func, a) { if (func(a)) { 1 } else { 2 } } ; g <- function(x) {TRUE} ; f(g, 5) ; f(is.na, 4) ; h <- function(x) { x == x } ; f(h, 3) }");
    }

    @Test
    public void TestSimpleFunctions_testBinding_cdbac40d8e88176d59018301c37f8033() {
        assertEval("{ f <- function(func, a) { if (func(a)) { 1 } else { 2 } } ; g <- function(x) {TRUE} ; f(g, 5) ; f(is.na, 4) ; f(g, 3) ; f(is.na, 10) }");
    }

    @Test
    public void TestSimpleFunctions_testBinding_eca2b3ed40faa194e24d49a5e73091cd() {
        assertEval("{ f <- function(func, a) { if (func(a)) { 1 } else { 2 } } ; g <- function(x) {TRUE} ; f(g, 5) ; f(is.na, 4) ; f(g, 3) ; f(c, 10) }");
    }

    @Test
    public void TestSimpleFunctions_testBinding_ae9fc4bbb36c93e16fd0bf5b3b69c916() {
        assertEval("{ f <- function(func, a) { if (func(a)) { 1 } else { 2 } } ; g <- function(x) {TRUE} ; f(g, 5) ; f(is.na, 4) ; f(g, 3) ; f(function(x) { 3+4i }, 10) }");
    }

    @Test
    public void TestSimpleFunctions_testBinding_846251aa0ea36e2caaa7d914b2992c6b() {
        assertEval("{ f <- function(func, a) { if (func(a)) { 1 } else { 2 } } ; g <- function(x) {TRUE} ; f(g, 5) ; f(is.na, 4) ; f(is.na, 10) }");
    }

    @Test
    public void TestSimpleFunctions_testBinding_c4c514929f311524620fb77a5f5d793f() {
        assertEval("{ f <- function(func, a) { func(a) && TRUE } ; g <- function(x) {TRUE} ; f(g, 5) ; f(is.na, 4)  }");
    }

    @Test
    public void TestSimpleFunctions_testBinding_b8b2c96096e38661cd275762856ada88() {
        assertEval("{ f <- function(func, a) { func(a) && TRUE } ; g <- function(x) {TRUE} ; f(g, 5) ; f(is.na, 4) ; f(is.na, 10) }");
    }

    @Test
    public void TestSimpleFunctions_testBinding_9238eb5438c84da7ae30d5542700fd1f() {
        assertEval("{ f <- function(func, a) { func(a) && TRUE } ; g <- function(x) {TRUE} ; f(g, 5) ; f(is.na, 4) ; f(length, 10) }");
    }

    @Test
    public void TestSimpleFunctions_testBinding_9d33cb3f2800c2e34da552fa52b5e4ac() {
        assertEval("{ f <- function(func, a) { func(a) && TRUE } ; g <- function(x) {TRUE} ; f(g, 5) ; f(is.na, 4) ; f(g, 10) ; f(is.na,5) }");
    }

    @Test
    public void TestSimpleFunctions_testBinding_821e4b79662133a431605809bcd39ab5() {
        assertEval("{ f <- function(func, a) { func(a) && TRUE } ; g <- function(x) {TRUE} ; f(g, 5) ; f(is.na, 4) ; f(function(x) { x + x }, 10) }");
    }

    @Test
    public void TestSimpleFunctions_testBinding_46add31adae2b550c93bf2c7341733e3() {
        assertEval("{ f <- function(i) { c(1,2) } ; f(1) ; c <- sum ; f(2) }");
    }

    @Test
    public void TestSimpleFunctions_testDefinitions_28852cbbe8f6c77e4be232ae01c765cb() {
        assertEval("{ \"%plus%\" <- function(a,b) a+b ; 3 %plus% 4 }");
    }

    @Test
    public void TestSimpleFunctions_testDefinitions_89e6dcd864857f47904a713f9b399e89() {
        assertEval("{ \"-\"(1) }");
    }

    @Test
    public void TestSimpleFunctions_testDefinitions_db43873d538745b7f440bea9a72e5ba8() {
        assertEval("x<-function(){1};x");
    }

    @Test
    public void TestSimpleFunctions_testDefinitions_0b5fd9af93b3e73346e7f4de73d898be() {
        assertEval("{ 'my<-' <- function(x, value) { attr(x, \"myattr\") <- value ; x } ; z <- 1; my(z) <- \"hello\" ; z }");
    }

    @Test
    public void TestSimpleFunctions_testDefinitionsIgnore_b8d75a017c31d73d6dbf7c6a93953d67() {
        assertEval("{ x <- function(a,b) { a^b } ; f <- function() { x <- \"sum\" ; sapply(1, x, 2) } ; f() }");
    }

    @Test
    public void TestSimpleFunctions_testDefinitionsIgnore_dfa24cb65db3a6a592617aa583ec1aaa() {
        assertEval("{ x <- function(a,b) { a^b } ; g <- function() { x <- \"sum\" ; f <- function() { sapply(1, x, 2) } ; f() }  ; g() }");
    }

    @Test
    public void TestSimpleFunctions_testDefinitionsIgnore_6ff99329ff4c5405259dd094d456df82() {
        assertEval("{ x <- function(a,b) { a^b } ; f <- function() { x <- 211 ; sapply(1, x, 2) } ; f() }");
    }

    @Test
    public void TestSimpleFunctions_testDefinitionsIgnore_90214c174a4cd064fcdf43a64bba6f73() {
        assertEval("{ x <- function(a,b) { a^b } ; dummy <- sum ; f <- function() { x <- \"dummy\" ; sapply(1, x, 2) } ; f() }");
    }

    @Test
    public void TestSimpleFunctions_testDefinitionsIgnore_ba4a8d210d2bcdac8ede803b28c13172() {
        assertEval("{ x <- function(a,b) { a^b } ; dummy <- sum ; f <- function() { x <- \"dummy\" ; dummy <- 200 ; sapply(1, x, 2) } ; f() }");
    }

    @Test
    public void TestSimpleFunctions_testDefinitionsIgnore_8ef4913016fe9a78ae79cb9f48e3c5ae() {
        assertEval("{ foo <- function (x) { x } ; foo() }");
    }

    @Test
    public void TestSimpleFunctions_testDefinitionsIgnore_1c3efc0657001d0ce5000a68b2e7b18d() {
        assertEval("{ foo <- function (x) { x } ; foo(1,2,3) }");
    }

    @Test
    public void TestSimpleFunctions_testDefinitionsNamedAndDefault_c3181fa02422dff5e566ca113c4fb5b3() {
        assertEval("{ f<-function(a=1,b=2,c=3) {TRUE} ; f(,,) }");
    }

    @Test
    public void TestSimpleFunctions_testDefinitionsNamedAndDefault_6f9b2854817a6afe9e8484d3fdc1c6b5() {
        assertEval("{ f<-function(x=2) {x} ; f() } ");
    }

    @Test
    public void TestSimpleFunctions_testDefinitionsNamedAndDefault_0a240c8a4628af4cbded7b97a76ef2c1() {
        assertEval("{ f<-function(a,b,c=2,d) {c} ; f(1,2,c=4,d=4) }");
    }

    @Test
    public void TestSimpleFunctions_testDefinitionsNamedAndDefault_6b39acec458c90722f8a5b2d73de7e69() {
        assertEval("{ f<-function(a,b,c=2,d) {c} ; f(1,2,d=8,c=1) }");
    }

    @Test
    public void TestSimpleFunctions_testDefinitionsNamedAndDefault_6fb997cfc0d7415bb13941f5b3325c2d() {
        assertEval("{ f<-function(a,b,c=2,d) {c} ; f(1,d=8,2,c=1) }");
    }

    @Test
    public void TestSimpleFunctions_testDefinitionsNamedAndDefault_db248b3017f463ba0f9184e28f7e909d() {
        assertEval("{ f<-function(a,b,c=2,d) {c} ; f(d=8,1,2,c=1) }");
    }

    @Test
    public void TestSimpleFunctions_testDefinitionsNamedAndDefault_99f69a3d1bd5795378ad4efee45c9e6e() {
        assertEval("{ f<-function(a,b,c=2,d) {c} ; f(d=8,c=1,2,3) }");
    }

    @Test
    public void TestSimpleFunctions_testDefinitionsNamedAndDefault_f687688558cd8e81ce3a0da702a239e6() {
        assertEval("{ f<-function(a=10,b,c=20,d=20) {c} ; f(4,3,5,1) }");
    }

    @Test
    public void TestSimpleFunctions_testDefinitionsNamedAndDefault_196f59348e26e1e26858b778b32590d3() {
        assertEval("{ x<-1 ; z<-TRUE ; f<-function(y=x,a=z,b) { if (z) {y} else {z}} ; f(b=2) }");
    }

    @Test
    public void TestSimpleFunctions_testDefinitionsNamedAndDefault_92eeefbdd71e59f91e6f685e96f800a1() {
        assertEval("{ x<-1 ; z<-TRUE ; f<-function(y=x,a=z,b) { if (z) {y} else {z}} ; f(2) }");
    }

    @Test
    public void TestSimpleFunctions_testDefinitionsNamedAndDefault_79e963e740ed06baa2bf676e9785d34e() {
        assertEval("{ x<-1 ; f<-function(x=x) { x } ; f(x=x) }");
    }

    @Test
    public void TestSimpleFunctions_testDefinitionsNamedAndDefault_c1a3b13a147c1000562a39c3fc4f20cc() {
        assertEval("{ f<-function(z, x=if (z) 2 else 3) {x} ; f(FALSE) }");
    }

    @Test
    public void TestSimpleFunctions_testDefinitionsNamedAndDefault_f36bc89f7c525704b677993e0d982761() {
        assertEval("{f<-function(a,b,c=2,d) {c} ; g <- function() f(d=8,c=1,2,3) ; g() ; g() }");
    }

    @Test
    public void TestSimpleFunctions_testDefinitionsNamedAndDefault_7b2dd380d3089ef7fca14a021ba1dbf8() {
        assertEval("{ x <- function(y) { sum(y) } ; f <- function() { x <- 1 ; x(1:10) } ; f() }");
    }

    @Test
    public void TestSimpleFunctions_testDefinitionsNamedAndDefault_d88ed35c4b95c84585a13f058691c6c2() {
        assertEval("{ f <- sum ; f(1:10) }");
    }

    @Test
    public void TestSimpleFunctions_testDefinitionsWorking_a64a6c5d250c94e619b66abb83c3124f() {
        assertEval("{ x<-function(z){z} ; x(TRUE) }");
    }

    @Test
    public void TestSimpleFunctions_testDefinitionsWorking_a2ddc77ba805631e3bf24e4e39c0e69e() {
        assertEval("{ x<-1 ; f<-function(){x} ; x<-2 ; f() }");
    }

    @Test
    public void TestSimpleFunctions_testDefinitionsWorking_6e9e1b64b6e294b6c70bd8297039f421() {
        assertEval("{ x<-1 ; f<-function(x){x} ; f(TRUE) }");
    }

    @Test
    public void TestSimpleFunctions_testDefinitionsWorking_6c47634aa11865bfb57c4b695c8fe32e() {
        assertEval("{ x<-1 ; f<-function(x){a<-1;b<-2;x} ; f(TRUE) }");
    }

    @Test
    public void TestSimpleFunctions_testDefinitionsWorking_ec536961ada0e354bbb9db3b6a2d7ce4() {
        assertEval("{ f<-function(x){g<-function(x) {x} ; g(x) } ; f(TRUE) }");
    }

    @Test
    public void TestSimpleFunctions_testDefinitionsWorking_6ef962720f2144b54149f7afb2b974da() {
        assertEval("{ x<-1 ; f<-function(x){a<-1; b<-2; g<-function(x) {b<-3;x} ; g(b) } ; f(TRUE) }");
    }

    @Test
    public void TestSimpleFunctions_testDefinitionsWorking_d5d454ec4274116056c0ffc6d7e4a38e() {
        assertEval("{ x<-1 ; f<-function(z) { if (z) { x<-2 } ; x } ; x<-3 ; f(FALSE) }");
    }

    @Test
    public void TestSimpleFunctions_testDefinitionsWorking_cde91319452e62e67af3a1b5538b8a11() {
        assertEval("{ f<-function() {z} ; z<-2 ; f() }");
    }

    @Test
    public void TestSimpleFunctions_testDefinitionsWorking_c287fa0cc254895e34869a17606ea495() {
        assertEval("{ x<-1 ; g<-function() { x<-12 ; f<-function(z) { if (z) { x<-2 } ; x } ; x<-3 ; f(FALSE) } ; g() }");
    }

    @Test
    public void TestSimpleFunctions_testDefinitionsWorking_90da246bb50ef4e1ebdae0087f361a7f() {
        assertEval("{ x<-function() { z<-211 ; function(a) { if (a) { z } else { 200 } } } ; f<-x() ; z<-1000 ; f(TRUE) }");
    }

    @Test
    public void TestSimpleFunctions_testDots_4dcf8d99f0c73ad065c2a74676a7ab2e() {
        assertEval("{ f <- function(a, b) { a - b } ; g <- function(...) { f(1, ...) } ; g(b = 2) }");
    }

    @Test
    public void TestSimpleFunctions_testDots_45ef2f9c49a54fcc7a262d8ccd06f11b() {
        assertEval("{ f <- function(...) { g(...) } ;  g <- function(b=2) { b } ; f() }");
    }

    @Test
    public void TestSimpleFunctions_testDots_8f61493e777085cb94d56683285bf0fe() {
        assertEval("{ f <- function(a, barg) { a + barg } ; g <- function(...) { f(a=1, ...) } ; g(b=2) }");
    }

    @Test
    public void TestSimpleFunctions_testDots_a570f75f485e65c7bd8cb5ba1a42b33e() {
        assertEval("{ f <- function(a, b) { a * b } ; g <- function(...) { f(...,...) } ; g(3) }");
    }

    @Test
    public void TestSimpleFunctions_testDots_cfd4e15d62829d53a1ff023619659bcb() {
        assertEval("{ g <- function(...) { c(...,...) } ; g(3) }");
    }

    @Test
    public void TestSimpleFunctions_testDotsIgnore_e620898284cbe5e1d40bfe326c77804e() {
        assertEval("{ f <- function(...) { ..1 } ;  f(10) }");
    }

    @Test
    public void TestSimpleFunctions_testDotsIgnore_a8e7323fa1a949f877214637cf0a91b1() {
        assertEval("{ f <- function(...) { x <<- 10 ; ..1 } ; x <- 1 ; f(x) }");
    }

    @Test
    public void TestSimpleFunctions_testDotsIgnore_ab19b9b703d36ea0149b6950305344b1() {
        assertEval("{ f <- function(...) { ..1 ; x <<- 10 ; ..1 } ; x <- 1 ; f(x) }");
    }

    @Test
    public void TestSimpleFunctions_testDotsIgnore_a52d7c73079437ca5443652b7f20f2ef() {
        assertEval("{ f <- function(...) { ..1 ; x <<- 10 ; ..2 } ; x <- 1 ; f(100,x) }");
    }

    @Test
    public void TestSimpleFunctions_testDotsIgnore_fc05b96d7c209b4b11d3c1597a4f5d95() {
        assertEval("{ f <- function(...) { ..2 ; x <<- 10 ; ..1 } ; x <- 1 ; f(x,100) }");
    }

    @Test
    public void TestSimpleFunctions_testDotsIgnore_d6e84b6c4d84ca15395f370802824ec0() {
        assertEval("{ g <- function(...) { 0 } ; f <- function(...) { g(...) ; x <<- 10 ; ..1 } ; x <- 1 ; f(x) }");
    }

    @Test
    public void TestSimpleFunctions_testDotsIgnore_581191e3ee585752a4393b1dd5c20af3() {
        assertEval("{ f <- function(...) { substitute(..1) } ;  f(x+y) }");
    }

    @Test
    public void TestSimpleFunctions_testDotsIgnore_46356a32a158c79de398dd64974058fc() {
        assertEval("{ f <- function(...) { g <- function() { ..1 } ; g() } ; f(a=2) }");
    }

    @Test
    public void TestSimpleFunctions_testDotsIgnore_569ec3ad103b4dcd2b7e7af1202dd26f() {
        assertEval("{ f <- function(...) { ..1 <- 2 ; ..1 } ; f(z = 1) }");
    }

    @Test
    public void TestSimpleFunctions_testDotsIgnore_a29c54a3c8cd1ee3e35a2aea432951cb() {
        assertEval("{ g <- function(a,b) { a + b } ; f <- function(...) { g(...) }  ; f(1,2) }");
    }

    @Test
    public void TestSimpleFunctions_testDotsIgnore_82b39f3b671e13554b9f70c67b51d9bc() {
        assertEval("{ g <- function(a,b,x) { a + b * x } ; f <- function(...) { g(...,x=4) }  ; f(b=1,a=2) }");
    }

    @Test
    public void TestSimpleFunctions_testDotsIgnore_44ffe8a1375fa81b1531c8e8a3c876ee() {
        assertEval("{ g <- function(a,b,x) { a + b * x } ; f <- function(...) { g(x=4, ...) }  ; f(b=1,a=2) }");
    }

    @Test
    public void TestSimpleFunctions_testDotsIgnore_1d94abf5afd9989c20c9e7713f15aa3a() {
        assertEval("{ g <- function(a,b,x) { a + b * x } ; f <- function(...) { g(x=4, ..., 10) }  ; f(b=1) }");
    }

    @Test
    public void TestSimpleFunctions_testDotsIgnore_b7c1eb65db6a2cb8b5f3401383477104() {
        assertEval("{ g <- function(a,b,aa,bb) { a ; x <<- 10 ; aa ; c(a, aa) } ; f <- function(...) {  g(..., ...) } ; x <- 1; y <- 2; f(x, y) }");
    }

    @Test
    public void TestSimpleFunctions_testDotsIgnore_f869c81e19bebe1d0b508f3152867860() {
        assertEval("{ f <- function(a, b) { a - b } ; g <- function(...) { f(1, ...) } ; g(a = 2) }");
    }

    @Test
    public void TestSimpleFunctions_testDotsIgnore_168904965e7c99fe53738eba7ef80c6e() {
        assertEval("{ f <- function(a, barg, ...) { a + barg } ; g <- function(...) { f(a=1, ...) } ; g(b=2,3) }");
    }

    @Test
    public void TestSimpleFunctions_testDotsIgnore_446276723386c4e17ee775d34b52759a() {
        assertEval("{ f <- function(a, barg, bextra, dummy) { a + barg } ; g <- function(...) { f(a=1, ...) } ; g(be=2,du=3, 3) }");
    }

    @Test
    public void TestSimpleFunctions_testDotsIgnore_30b478f9a7f62680adb64c9c36c9ab71() {
        assertEval("{ f <- function(a, barg, bextra, dummy) { a + barg } ; g <- function(...) { f(a=1, ...) } ; g(1,2,3) }");
    }

    @Test
    public void TestSimpleFunctions_testDotsIgnore_ba5a64f80ce3db2ca6ec2bc574c2b011() {
        assertEval("{ f <- function(...,d) { ..1 + ..2 } ; f(1,d=4,2) }");
    }

    @Test
    public void TestSimpleFunctions_testDotsIgnore_ccfd3930d86a89add4a6dbc2941c216e() {
        assertEval("{ f <- function(...,d) { ..1 + ..2 } ; f(1,2,d=4) }");
    }

    @Test
    public void TestSimpleFunctions_testDotsIgnore_a3678db1544ef8395deec4ed02acdb3d() {
        assertEvalError("{ g <- function(a,b,x) { a + b * x } ; f <- function(...) { g(x=4, ..., 10) }  ; f(b=1,a=2) }");
    }

    @Test
    public void TestSimpleFunctions_testDotsIgnore_67eac84ba5b2dac0c1bc9214053b228c() {
        assertEvalError("{ f <- function(...) { ..3 } ; f(1,2) }");
    }

    @Test
    public void TestSimpleFunctions_testDotsIgnore_452f05dd561690c47f4f03db94d54b6b() {
        assertEvalError("{ f <- function() { dummy() } ; f() }");
    }

    @Test
    public void TestSimpleFunctions_testDotsIgnore_20c4c3aa63da2253e51ef2c5ba9d4a1b() {
        assertEvalError("{ f <- function() { if (FALSE) { dummy <- 2 } ; dummy() } ; f() }");
    }

    @Test
    public void TestSimpleFunctions_testDotsIgnore_8645241807f4b8810f69603e0858ef16() {
        assertEvalError("{ f <- function() { if (FALSE) { dummy <- 2 } ; g <- function() { dummy() } ; g() } ; f() }");
    }

    @Test
    public void TestSimpleFunctions_testDotsIgnore_edee5dc6f81e51ce659c0f3a2fb21571() {
        assertEvalError("{ f <- function() { dummy <- 2 ; g <- function() { dummy() } ; g() } ; f() }");
    }

    @Test
    public void TestSimpleFunctions_testDotsIgnore_c3c566ad3a1f22872c3f310db5ae8933() {
        assertEvalError("{ f <- function() { dummy() } ; dummy <- 2 ; f() }");
    }

    @Test
    public void TestSimpleFunctions_testDotsIgnore_5606499974e8a959bd2e5a755f7832c8() {
        assertEvalError("{ dummy <- 2 ; dummy() }");
    }

    @Test
    public void TestSimpleFunctions_testDotsIgnore_76837b302e412d60cdec11289bac184b() {
        assertEvalError("{ lapply(1:3, \"dummy\") }");
    }

    @Test
    public void TestSimpleFunctions_testDotsIgnore_27d8843efbecef3fd6ae84611b61cdff() {
        assertEvalError("{ f <- function(a, b) { a + b } ; g <- function(...) { f(a=1, ...) } ; g(a=2) }");
    }

    @Test
    public void TestSimpleFunctions_testDotsIgnore_7f8c80886bf192821872b6edd793baf2() {
        assertEvalError("{ f <- function(a, barg, bextra) { a + barg } ; g <- function(...) { f(a=1, ...) } ; g(b=2,3) }");
    }

    @Test
    public void TestSimpleFunctions_testDotsIgnore_997c167046500987d88720745d0018c2() {
        assertEvalError("{ f <- function(a, barg, bextra, dummy) { a + barg } ; g <- function(...) { f(a=1, ...) } ; g(be=2,bex=3, 3) }");
    }

    @Test
    public void TestSimpleFunctions_testDotsIgnore_601a671e48fcffae9a23e5b3466aa324() {
        assertEvalError("{ f <- function(a, barg, bextra, dummy) { a + barg } ; g <- function(...) { f(a=1, ..., x=2) } ; g(1) }");
    }

    @Test
    public void TestSimpleFunctions_testDotsIgnore_c42cdbf8980cb24618b0e81c71c76f87() {
        assertEvalError("{ f <- function(a, barg, bextra, dummy) { a + barg } ; g <- function(...) { f(a=1, ..., x=2,z=3) } ; g(1) }");
    }

    @Test
    public void TestSimpleFunctions_testDotsIgnore_673e885ab1ad8a737dbc0b05d6a34eed() {
        assertEvalError("{ f <- function(a, barg, bextra, dummy) { a + barg } ; g <- function(...) { f(a=1, ..., xxx=2) } ; g(1) }");
    }

    @Test
    public void TestSimpleFunctions_testDotsIgnore_4ef97fc6760900dfba4abef33ebb3620() {
        assertEvalError("{ f <- function(a, barg, bextra, dummy) { a + barg } ; g <- function(...) { f(a=1, xxx=2, ...) } ; g(1) }");
    }

    @Test
    public void TestSimpleFunctions_testDotsIgnore_3df181a7e78ef23b092f1aba322bbfa1() {
        assertEvalError("{ f <- function(a, barg, bextra, dummy) { a + barg } ; g <- function(...) { f(a=1, ...,,,) } ; g(1) }");
    }

    @Test
    public void TestSimpleFunctions_testDotsIgnore_abcc928e40684f62d0ad26ee2f35b057() {
        assertEvalError("{ f <- function(...) { ..2 + ..2 } ; f(1,,2) }");
    }

    @Test
    public void TestSimpleFunctions_testDotsIgnore_408a647f1319d8f5216323761b223a47() {
        assertEvalError("{ f <- function(...) { ..1 + ..2 } ; f(1,,3) }");
    }

    @Test
    public void TestSimpleFunctions_testErrors_97c1046334e0c7a03ba92803615fccd6() {
        assertEvalError("{ x<-function(){1} ; x(y=1) }");
    }

    @Test
    public void TestSimpleFunctions_testErrors_e45fc91400caff4d8a5596ec8cd2edfc() {
        assertEvalError("{ x<-function(y, b){1} ; x(y=1, 2, 3, z = 5) }");
    }

    @Test
    public void TestSimpleFunctions_testErrors_e8fd77ad56a4fc8e254f827faed5c973() {
        assertEvalError("{ x<-function(){1} ; x(1) }");
    }

    @Test
    public void TestSimpleFunctions_testErrors_9c686da74e6a9bfda861ec6e834613e8() {
        assertEvalError("{ x<-function(a){1} ; x(1,) }");
    }

    @Test
    public void TestSimpleFunctions_testErrors_423440c018b8f580500bc17469c52cb8() {
        assertEvalError("{ x<-function(){1} ; x(y=sum(1:10)) }");
    }

    @Test
    public void TestSimpleFunctions_testErrors_483a6566dbfd75258c3c09b229efb70b() {
        assertEvalError("{ f <- function(x) { x } ; f() }");
    }

    @Test
    public void TestSimpleFunctions_testErrors_da6b1096c4e55e8bb4ac7400d7e63552() {
        assertEvalError("{ x<-function(y,b){1} ; x(y=1,y=3,4) }");
    }

    @Test
    public void TestSimpleFunctions_testErrors_3e920d36beba426178bb6e2c548151b7() {
        assertEvalError("{ x<-function(foo,bar){foo*bar} ; x(fo=10,f=1,2) }");
    }

    @Test
    public void TestSimpleFunctions_testErrors_1f3190100b071debf5b11ed7f2fae959() {
        assertEvalError("{ f <- function(a,a) {1} }");
    }

    @Test
    public void TestSimpleFunctions_testErrors_bf29c1dae99e04f8cd11a340f54e1287() {
        assertEvalError("{ f <- function(a,b,c,d) { a + b } ; f(1,x=1,2,3,4) }");
    }

    @Test
    public void TestSimpleFunctions_testMatching_c272d90b4e2480f9f6fc9b6bfcc79e74() {
        assertEval("{ x<-function(foo,bar){foo*bar} ; x(f=10,2) }");
    }

    @Test
    public void TestSimpleFunctions_testMatching_7945ff12693520f14035e534231ca1a5() {
        assertEval("{ x<-function(foo,bar){foo*bar} ; x(fo=10, bar=2) }");
    }

    @Test
    public void TestSimpleFunctions_testMatchingIgnore_7c113e0683905a2c65072aebc1cf14dc() {
        assertEvalError("{ f <- function(hello, hi) { hello + hi } ; f(h = 1) }");
    }

    @Test
    public void TestSimpleFunctions_testMatchingIgnore_1bd6b789e14102f4d5c84c2e1cd0b3cd() {
        assertEvalError("{ f <- function(hello, hi) { hello + hi } ; f(hello = 1, bye = 3) }");
    }

    @Test
    public void TestSimpleFunctions_testMatchingIgnore_b27e201723ae1ff4db0c5bcbe14b18b6() {
        assertEvalError("{ f <- function(a) { a } ; f(1,2) }");
    }

    @Test
    public void TestSimpleFunctions_testPromises_4a3b20e85c61edf3c36e662bdc63d53d() {
        assertEval("{ z <- 1 ; f <- function(c = z) { c(1,2) ; z <- z + 1 ; c  } ; f() }");
    }

    @Test
    public void TestSimpleFunctions_testPromisesIgnore_c7558b8584a0a8c1dff6c7ee5575ab52() {
        assertEval("{ f <- function(x = z) { z = 1 ; x } ; f() }");
    }

    @Test
    public void TestSimpleFunctions_testPromisesIgnore_b817867bec89270f00c9820b107edd80() {
        assertEval("{ z <- 1 ; f <- function(c = z) {  z <- z + 1 ; c  } ; f() }");
    }

    @Test
    public void TestSimpleFunctions_testPromisesIgnore_0782b9c8b5990e31ca5d45f3d355ad83() {
        assertEval("{ f <- function(a) { g <- function(b) { x <<- 2; b } ; g(a) } ; x <- 1 ; f(x) }");
    }

    @Test
    public void TestSimpleFunctions_testPromisesIgnore_9a98faefce072c525121fc846528b144() {
        assertEval("{ f <- function(a) { g <- function(b) { a <<- 3; b } ; g(a) } ; x <- 1 ; f(x) }");
    }

    @Test
    public void TestSimpleFunctions_testPromisesIgnore_f502212c6a9fc0404104e3f44f29d926() {
        assertEval("{ f <- function(x) { function() {x} } ; a <- 1 ; b <- f(a) ; a <- 10 ; b() }");
    }

    @Test
    public void TestSimpleFunctions_testPromisesIgnore_1d4e596e32ad6ce14263c2861138bb44() {
        assertEvalError("{ f <- function(x = y, y = x) { y } ; f() }");
    }

    @Test
    public void TestSimpleFunctions_testRecursion_788b8f353f8f02e3fa76469c56859e2e() {
        assertEval("{ f<-function(i) { if(i==1) { 1 } else { j<-i-1 ; f(j) } } ; f(10) }");
    }

    @Test
    public void TestSimpleFunctions_testRecursion_53ff93949493b468da6dd721627f97c3() {
        assertEval("{ f<-function(i) { if(i==1) { 1 } else { f(i-1) } } ; f(10) }");
    }

    @Test
    public void TestSimpleFunctions_testRecursion_2327d5e8fe9efc6208aad1417470eebc() {
        assertEval("{ f<-function(i) { if(i<=1) 1 else i*f(i-1) } ; f(10) }");
    }

    @Test
    public void TestSimpleFunctions_testRecursion_b8ff015ccc9e2f9833f93f4683f1e6c1() {
        assertEval("{ f<-function(i) { if(i<=1L) 1L else i*f(i-1L) } ; f(10L) }");
    }

    @Test
    public void TestSimpleFunctions_testRecursion_2fa0d885e0c0c4715e94b7eb7ff0ba08() {
        assertEval("{ f<-function(i) { if(i<=1) 1 else i*f(i-1) } ; g<-function(n, f, a) { if (n==1) { f(a) } else { f(a) ; g(n-1, f, a) } } ; g(100,f,120) }");
    }

    @Test
    public void TestSimpleFunctions_testRecursion_b491d61408b15d700753b3576d392510() {
        assertEval("{ f<-function(i) { if (i==1) { 1 } else if (i==2) { 1 } else { f(i-1) + f(i-2) } } ; f(10) }");
    }

    @Test
    public void TestSimpleFunctions_testRecursion_bafa0822c2c0e4bb58abe1a7501bc9a7() {
        assertEval("{ f<-function(i) { if (i==1L) { 1L } else if (i==2L) { 1L } else { f(i-1L) + f(i-2L) } } ; f(10L) }");
    }

    @Test
    public void TestSimpleFunctions_testReturn_3a43f178980409e80374fdf37840b6cd() {
        assertEval("{ f<-function() { return() } ; f() }");
    }

    @Test
    public void TestSimpleFunctions_testReturn_8d446d0c309684cf52c2cff0d3d9efdd() {
        assertEval("{ f<-function() { return(2) ; 3 } ; f() }");
    }

    @Test
    public void TestSimpleIfEvaluator_testCast_9199af29689a0510d0f2b7657d6f9656() {
        assertEvalError("{ if (integer()) { TRUE } }");
    }

    @Test
    public void TestSimpleIfEvaluator_testCast_9759a28257afd267f562c056ecb21bc3() {
        assertEvalError("{ if (1[2:1]) { TRUE } }");
    }

    @Test
    public void TestSimpleIfEvaluator_testCast_16ad47e3aae858392d62ccd5199242c9() {
        assertEvalError("{ if (c(1L[2],0L,0L)) { TRUE } else { 2 } }");
    }

    @Test
    public void TestSimpleIfEvaluator_testCast_bda065a78031d440e536225f68fb6c2c() {
        assertEvalError("{ f <- function(cond) { if (cond) { TRUE } else { 2 }  } ; f(logical()) }");
    }

    @Test
    public void TestSimpleIfEvaluator_testCast_82982f95ffe974f98ccba036dfa8744e() {
        assertEvalWarning("{ f <- function(a) { if (is.na(a)) { 1 } else { 2 } } ; f(5) ; f(1:3)}");
    }

    @Test
    public void TestSimpleIfEvaluator_testCast_0db47653499ad8ead6375d84cb54b7f9() {
        assertEvalWarning("{ if (1:3) { TRUE } }");
    }

    @Test
    public void TestSimpleIfEvaluator_testCast_099b8bdf35d655c86519abbffda1ce8d() {
        assertEvalWarning("{ if (c(0,0,0)) { TRUE } else { 2 } }");
    }

    @Test
    public void TestSimpleIfEvaluator_testCast_813778d331bc4877ff5907cb5b3c7f3c() {
        assertEvalWarning("{ if (c(1L,0L,0L)) { TRUE } else { 2 } }");
    }

    @Test
    public void TestSimpleIfEvaluator_testCast_3e2b75fc9ef406c71f3e29e6b3d99c78() {
        assertEvalWarning("{ if (c(0L,0L,0L)) { TRUE } else { 2 } }");
    }

    @Test
    public void TestSimpleIfEvaluator_testCast_7fc18aa80c865a84fa5e33de006f8ccd() {
        assertEvalWarning("{ f <- function(cond) { if (cond) { TRUE } else { 2 } } ; f(1:3) ; f(2) }");
    }

    @Test
    public void TestSimpleIfEvaluator_testCast_54e42c6c4429a21b131e545c9dc37dbe() {
        assertEvalWarning("{ f <- function(cond) { if (cond) { TRUE } else { 2 }  } ; f(c(TRUE,FALSE)) ; f(FALSE) }");
    }

    @Test
    public void TestSimpleIfEvaluator_testCast_f221f10e3f4b7d00f239da0a0f88304f() {
        assertEvalWarning("{ f <- function(cond) { if (cond) { TRUE } else { 2 }  } ; f(c(TRUE,FALSE)) ; f(1) }");
    }

    @Test
    public void TestSimpleIfEvaluator_testIf_7b33c311264b2e52d4b7a20a7b757e37() {
        assertEval("{ x <- 2 ; if (1==x) TRUE else 2 }");
    }

    @Test
    public void TestSimpleIfEvaluator_testIf_e966a84324c8008c95cf9b99088ae120() {
        assertEval("{ f <- function(x) { if (x) 1 else 2 } ; f(1) ; f(TRUE) }");
    }

    @Test
    public void TestSimpleIfEvaluator_testIf_678b56ea12921bdd8a913afb486b9867() {
        assertEval("{ f <- function(x) { if (x) 1 else 2 } ; f(1) ; f(FALSE) }");
    }

    @Test
    public void TestSimpleIfEvaluator_testIf_bc3c097b9dd55e8f7434f1a865633ff3() {
        assertEval("{ if (TRUE==FALSE) TRUE else FALSE }");
    }

    @Test
    public void TestSimpleIfEvaluator_testIf_c070b3457c7ce4ac612978aa99bad9e1() {
        assertEval("{ if (FALSE==TRUE) TRUE else FALSE }");
    }

    @Test
    public void TestSimpleIfEvaluator_testIf_368d985d896df8d658b28399a8c3ae8e() {
        assertEval("{ if (FALSE==1) TRUE else FALSE }");
    }

    @Test
    public void TestSimpleIfEvaluator_testIf_342f4f9212dd0d7f6d241317e70c48ff() {
        assertEval("{ f <- function(v) { if (FALSE==v) TRUE else FALSE } ; f(TRUE) ; f(1) }");
    }

    @Test
    public void TestSimpleIfEvaluator_testIf_174fdf04199a4eba4db1c05dbec06db9() {
        assertEvalError("{ x <- 2 ; if (NA) x <- 3 ; x }");
    }

    @Test
    public void TestSimpleIfEvaluator_testIf_45ac51977c099e844e9cb00ebb02366e() {
        assertEvalError("{ f <- function(x) { if (x) 1 else 2 } ; f(NA)  }");
    }

    @Test
    public void TestSimpleIfEvaluator_testIf_c737c8946144235d827f68135ecd67b8() {
        assertEvalError("{ f <- function(x) { if (x) 1 else 2 } ; f(1) ; f(NA) }");
    }

    @Test
    public void TestSimpleIfEvaluator_testIf_d3f8558892544cd36de71a744cf8d3b7() {
        assertEvalError("{ f <- function(x) { if (x) 1 else 2 } ; f(1) ; f(\"hello\") }");
    }

    @Test
    public void TestSimpleIfEvaluator_testIf_a3db4b937859f822db002f66720a2330() {
        assertEvalError("{ f <- function(x) { if (x) 1 else 2 } ; f(1) ; f(logical()) }");
    }

    @Test
    public void TestSimpleIfEvaluator_testIf_1176ca756067ae8389a73912701118f3() {
        assertEvalError("{ f <- function(x) { if (x == 2) 1 else 2 } ; f(1) ; f(NA) }");
    }

    @Test
    public void TestSimpleIfEvaluator_testIf_614b57fd3bef21349d7e3e718d39f8de() {
        assertEvalError("{ if (NA == TRUE) TRUE else FALSE }");
    }

    @Test
    public void TestSimpleIfEvaluator_testIf_bb9be8d2a69b78e30de31042055ecfb5() {
        assertEvalError("{ if (TRUE == NA) TRUE else FALSE }");
    }

    @Test
    public void TestSimpleIfEvaluator_testIf2_5692fd60cc98a5ce0d954993e427bb59() {
        assertEval("if(TRUE) 1 else 2");
    }

    @Test
    public void TestSimpleIfEvaluator_testIf2_77782ac181f98640f0259537469a459f() {
        assertEval("if(FALSE) 1 else 2");
    }

    @Test
    public void TestSimpleIfEvaluator_testIfDanglingElse_7a306dba6d14aca43d5045c2db42e702() {
        assertEval("if(TRUE) if (FALSE) 1 else 2");
    }

    @Test
    public void TestSimpleIfEvaluator_testIfDanglingElseIgnore_d73be7d76c1d5f7720c73594824df7ea() {
        assertEvalNoOutput("if(FALSE) if (FALSE) 1 else 2");
    }

    @Test
    public void TestSimpleIfEvaluator_testIfIgnore_e44614f9767a91b8721567cbaab6aa97() {
        assertEvalWarning("{ f <- function(x) { if (x) 1 else 2 } ; f(1) ; f(1:3) }");
    }

    @Test
    public void TestSimpleIfEvaluator_testIfNot1_a2e2a550ed5c85989fee041136f4cddd() {
        assertEval("if(!FALSE) 1 else 2");
    }

    @Test
    public void TestSimpleIfEvaluator_testIfNot1_51683f5dc6a8c118b35fc5ca9f2c398c() {
        assertEval("if(!TRUE) 1 else 2");
    }

    @Test
    public void TestSimpleIfEvaluator_testIfWithoutElse_fda700832dfd7d9e6c06cb66fd9e25c8() {
        assertEval("if(TRUE) 1");
    }

    @Test
    public void TestSimpleIfEvaluator_testIfWithoutElseIgnore_a1e01cf7b16f44e54f53f0bd7b7d4712() {
        assertEvalNoOutput("if(FALSE) 1");
    }

    @Test
    public void TestSimpleLists_testListAccess_18f9fc63a2e08aff74f0ff462ce83db4() {
        assertEval("{ l <- list(c(1,2,3),\"eep\") ; l[[1]] }");
    }

    @Test
    public void TestSimpleLists_testListAccess_5c6155b65246519eee382c04fd74fc56() {
        assertEval("{ l <- list(c(1,2,3),\"eep\") ; l[[2]] }");
    }

    @Test
    public void TestSimpleLists_testListAccess_53a797f0d6470ad9ec52827e37b2d7c7() {
        assertEval("{ l <- list(1,2,3) ; l[5] }");
    }

    @Test
    public void TestSimpleLists_testListAccess_9d31ef76615e6556bfc4edf6ec869e25() {
        assertEval("{ l <- list(1,2,3) ; typeof(l[5]) }");
    }

    @Test
    public void TestSimpleLists_testListAccess_91fe05887df6c77e161e4527b18cc6a0() {
        assertEval("{ l <- list(1,2,3) ; l[0] }");
    }

    @Test
    public void TestSimpleLists_testListAccess_3a4538aac7e564692ec0865701cf4f38() {
        assertEval("{ l <- list(1,2,3) ; l[[NA]] }");
    }

    @Test
    public void TestSimpleLists_testListAccess_ab60c92a5348c5c3af12f60fda3c2cf9() {
        assertEval("{ l <- list(1,2,3) ; typeof(l[[NA]]) }");
    }

    @Test
    public void TestSimpleLists_testListAccess_9cec59e77b03e6dad551210e799ef068() {
        assertEval("{ l <- list(1,2,3) ; l[NA] }");
    }

    @Test
    public void TestSimpleLists_testListAccess_875d88d54a98324fd9b20ec65f92717c() {
        assertEval("{ l <- list(1,2,3) ; typeof(l[NA]) }");
    }

    @Test
    public void TestSimpleLists_testListAccess_86d81009828e37036c672e2d0bcc92d2() {
        assertEval("{ l <- list(1,2,3) ; l[-2] }");
    }

    @Test
    public void TestSimpleLists_testListAccess_c7b7784cf8c0f1b69f7c1f718456ed58() {
        assertEval("{ l <- list(1,2,3) ; typeof(l[-2]) }");
    }

    @Test
    public void TestSimpleLists_testListAccess_3c3b7a46d2860e3da58a221107bb0847() {
        assertEval("{ l <- list(1,2,3) ; l[-5] }");
    }

    @Test
    public void TestSimpleLists_testListAccess_62a1913ad8084caa2140ffb8faf43b5d() {
        assertEval("{ a <- list(1,NULL,list()) ; a[3] }");
    }

    @Test
    public void TestSimpleLists_testListAccess_ac7d1818b5b74aeec142dec729bf8538() {
        assertEval("{ a <- list(1,NULL,list()) ; a[[3]] }");
    }

    @Test
    public void TestSimpleLists_testListAccess_e812d415728b4d67419d55af93a40176() {
        assertEval("{ a <- list(1,NULL,list()) ; typeof(a[3]) }");
    }

    @Test
    public void TestSimpleLists_testListAccess_a2eb5f7e77e0c397af70c404f3c26162() {
        assertEval("{ a <- list(1,NULL,list()) ; typeof(a[[3]]) }");
    }

    @Test
    public void TestSimpleLists_testListAccess_d96acf7acd87b203fb663abeb53bf98c() {
        assertEval("{ a <- list(1,2,3) ; x <- integer() ; a[x] }");
    }

    @Test
    public void TestSimpleLists_testListAccess_f50556c729cd8f8fb891ec8e2ff7be57() {
        assertEvalError("{ l <- list(1,2,3) ; l[[5]] }");
    }

    @Test
    public void TestSimpleLists_testListAccess_7c18c20f645e4c331fba77e5f4e9d13c() {
        assertEvalError("{ l <- list(1,2,3) ; l[[0]] }");
    }

    @Test
    public void TestSimpleLists_testListAccess_4a1c095b6af64ee84a88ba1a3e3bf6dc() {
        assertEvalError("{ l <- list(1,2,3) ; l[[-2]] }");
    }

    @Test
    public void TestSimpleLists_testListAccess_34a7d57c315887ff314aaf437954874c() {
        assertEvalError("{ l <- list(1,2,3) ; l[[-5]] }");
    }

    @Test
    public void TestSimpleLists_testListAccess_f2688f64a0fd99d51f3d5123888afffb() {
        assertEvalError("{ a <- list(1,2,3) ; x <- integer() ; a[[x]] }");
    }

    @Test
    public void TestSimpleLists_testListArgumentEvaluation_f62339e36ed620e527abf492790cea00() {
        assertEval("{ a <- c(0,0,0) ; f <- function() { g <- function() { a[2] <<- 9 } ; g() } ; u <- function() { a <- c(1,1,1) ; f() ; a } ; list(a,u()) }");
    }

    @Test
    public void TestSimpleLists_testListCombine_315689503dbaa314b803e0b6ced2181f() {
        assertEval("{ a <- c(list(1)) ; typeof(a) }");
    }

    @Test
    public void TestSimpleLists_testListCombine_031786c1ea90d2d2e9ec699f794b4afd() {
        assertEval("{ a <- c(list(1)) ; typeof(a[1]) }");
    }

    @Test
    public void TestSimpleLists_testListCombine_964f72f495ac4c5188cd2fb4bbf6fa55() {
        assertEval("{ a <- c(list(1)) ; typeof(a[[1]]) }");
    }

    @Test
    public void TestSimpleLists_testListCombine_201fcbdb0ecf34cc4ff2f1f723b2a53a() {
        assertEval("{ a <- c(1,2,list(3,4),5) ; a }");
    }

    @Test
    public void TestSimpleLists_testListCombine_467698077b5b2ff1e3e7785bbf9677c7() {
        assertEval("{ a <- c(1,2,list(3,4),5) ; typeof(a) }");
    }

    @Test
    public void TestSimpleLists_testListCombine_c2ea15d71c33bb87608041117c9e2891() {
        assertEval("{ a <- c(1,2,list(3,4),5) ; a[3] }");
    }

    @Test
    public void TestSimpleLists_testListCombine_232d71f34559999310e6a53914651f9d() {
        assertEval("{ a <- c(1,2,list(3,4),5) ; typeof(a[3]) }");
    }

    @Test
    public void TestSimpleLists_testListCombine_4ee7b61f01251cf293c28bb420e4e87c() {
        assertEval("{ a <- c(1,2,list(3,4),5) ; a[[3]] }");
    }

    @Test
    public void TestSimpleLists_testListCombine_be373b0c9592e9c0ec3670a4d7a740d6() {
        assertEval("{ a <- c(1,2,list(3,4),5) ; typeof(a[[3]]) }");
    }

    @Test
    public void TestSimpleLists_testListCreation_c503a581b4392d103b701db574cadae8() {
        assertEval("{ list() }");
    }

    @Test
    public void TestSimpleLists_testListCreation_7931cfc2ce818a71b64fdf1cf5e413ed() {
        assertEval("{ list(list(),list()) }");
    }

    @Test
    public void TestSimpleLists_testListCreation_a90919f9121c46fed48bc8590db8c055() {
        assertEval("{ list(1,NULL,list()) }");
    }

    @Test
    public void TestSimpleLists_testListUpdate_df8998751433c2a2773cbb812fae67f2() {
        assertEval("{ l <- list(c(1,2,3),c(4,5,6)) ; l[[1]] <- c(7,8,9) ; l[[1]] }");
    }

    @Test
    public void TestSimpleLoop_testDynamic_f61782f946510fe4afa8081fcbdd8fb1() {
        assertEval("{ l <- quote({x <- 0 ; for(i in 1:10) { x <- x + i } ; x}) ; f <- function() { eval(l) } ; x <<- 10 ; f() }");
    }

    @Test
    public void TestSimpleLoop_testFactorial_980dc7a40991e6f049592919e6e49549() {
        assertEval("{ f<-function(i) { if (i<=1) {1} else {r<-i; for(j in 2:(i-1)) {r=r*j}; r} }; f(10) }");
    }

    @Test
    public void TestSimpleLoop_testFibonacci_e824e7493537a5d548e4c8cc8b81672e() {
        assertEval("{ f<-function(i) { x<-integer(i); x[1]<-1; x[2]<-1; if (i>2) { for(j in 3:i) { x[j]<-x[j-1]+x[j-2] } }; x[i] } ; f(32) }");
    }

    @Test
    public void TestSimpleLoop_testForSequenceDescending_994e6351e728280d7ba88e09fc7a11d1() {
        assertEval("{ sum <- 0; for (i in 3:1) { sum <- sum + i; }; sum; }");
    }

    @Test
    public void TestSimpleLoop_testLoops1_bce9a3951514a3066661f5ca1a2441bd() {
        assertEval("{ x<-210 ; repeat { x <- x + 1 ; break } ; x }");
    }

    @Test
    public void TestSimpleLoop_testLoops1_ae888ff43ea51dddf513e8b8df7d9d3f() {
        assertEval("{ x<-1 ; repeat { x <- x + 1 ; if (x > 11) { break } } ; x }");
    }

    @Test
    public void TestSimpleLoop_testLoops1_0c89718b6eb747702f0f5d00b8079cc3() {
        assertEval("{ x<-1 ; repeat { x <- x + 1 ; if (x <= 11) { next } else { break } ; x <- 1024 } ; x }");
    }

    @Test
    public void TestSimpleLoop_testLoops1_34699c5e1e3644780a5432cf084c15c2() {
        assertEval("{ x<-1 ; while(TRUE) { x <- x + 1 ; if (x > 11) { break } } ; x }");
    }

    @Test
    public void TestSimpleLoop_testLoops1_f70a46760861e5ab800206007d2255b7() {
        assertEval("{ x<-1 ; while(x <= 10) { x<-x+1 } ; x }");
    }

    @Test
    public void TestSimpleLoop_testLoops1_1d936a3900cebedb6509c9a54f315836() {
        assertEval("{ x<-1 ; for(i in 1:10) { x<-x+1 } ; x }");
    }

    @Test
    public void TestSimpleLoop_testLoops1_57326daec626f7534170f16514601151() {
        assertEval("{ for(i in c(1,2)) { x <- i } ; x }");
    }

    @Test
    public void TestSimpleLoop_testLoops1_b34270c17d2d7bc4cc52ebd278b531c7() {
        assertEval("{ f<-function(r) { x<-0 ; for(i in r) { x<-x+i } ; x } ; f(1:10) ; f(c(1,2,3,4,5)) }");
    }

    @Test
    public void TestSimpleLoop_testLoops1_f6c2d39e2b4af11d41385c29b4e8b362() {
        assertEval("{ f<-function(r) { x<-0 ; for(i in r) { x<-x+i } ; x } ; f(c(1,2,3,4,5)) ; f(1:10) }");
    }

    @Test
    public void TestSimpleLoop_testLoops1_95ad9d671942b4704417971ed44409ba() {
        assertEval("{ r <- \"\" ; for (s in c(\"Hello\", \"world\")) r <- paste(r, s) ; r }");
    }

    @Test
    public void TestSimpleLoop_testLoops3_e52a6f4007d0a090db2f28b255bf413a() {
        assertEval("{ l <- quote({for(i in c(1,2)) { x <- i } ; x }) ; f <- function() { eval(l) } ; f() }");
    }

    @Test
    public void TestSimpleLoop_testLoops3_6548e4ec40613c3fef7af0ad99e9633e() {
        assertEval("{ l <- quote(for(i in s) { x <- i }) ; s <- 1:3 ; eval(l) ; s <- 2:1 ; eval(l) ; x }");
    }

    @Test
    public void TestSimpleLoop_testLoops3_46a097a0af4ffe6e8077dcbe5e4430e0() {
        assertEval("{ l <- quote({for(i in c(2,1)) { x <- i } ; x }) ; f <- function() { if (FALSE) i <- 2 ; eval(l) } ; f() }");
    }

    @Test
    public void TestSimpleLoop_testLoops3_569178ca1ef4a4eb52481f6da3753a5a() {
        assertEval("{ l <- quote(for(i in s) { x <- i }) ; s <- 1:3 ; eval(l) ; s <- NULL ; eval(l) ; x }");
    }

    @Test
    public void TestSimpleLoop_testLoops3_05c2bfcd5008d009fec146738755dac8() {
        assertEval("{ l <- quote({ for(i in c(1,2,3,4)) { if (i == 1) { next } ; if (i==3) { break } ; x <- i ; if (i==4) { x <- 10 } } ; x }) ; f <- function() { eval(l) } ; f()  }");
    }

    @Test
    public void TestSimpleLoop_testLoops3_ea11d8de89669a91c43b8a2985aaf4a0() {
        assertEval("{ l <- quote({ for(i in 1:4) { if (i == 1) { next } ; if (i==3) { break } ; x <- i ; if (i==4) { x <- 10 } } ; x }) ; f <- function() { eval(l) } ; f()  }");
    }

    @Test
    public void TestSimpleLoop_testLoopsBreakNext_91e30f3e9fb9cb4abe44cb85b9b1af13() {
        assertEval("{ for(i in c(1,2,3,4)) { if (i == 1) { next } ; if (i==3) { break } ; x <- i ; if (i==4) { x <- 10 } } ; x }");
    }

    @Test
    public void TestSimpleLoop_testLoopsBreakNext_74ae7e11b70da5938e55907d13cfd552() {
        assertEval("{ f <- function() { for(i in c(1,2,3,4)) { if (i == 1) { next } ; if (i==3) { break } ; x <- i ; if (i==4) { x <- 10 } } ; x } ; f()  }");
    }

    @Test
    public void TestSimpleLoop_testLoopsBreakNext_a91816fd9403d310cc046b69cdc2bf25() {
        assertEval("{ f <- function() { for(i in 1:4) { if (i == 1) { next } ; if (i==3) { break } ; x <- i ; if (i==4) { x <- 10 } } ; x } ; f() }");
    }

    @Test
    public void TestSimpleLoop_testLoopsBreakNext_7d1ff3638747f862f33de8e36ab422f0() {
        assertEval("{ for(i in 1:4) { if (i == 1) { next } ; if (i==3) { break } ; x <- i ; if (i==4) { x <- 10 } } ; x }");
    }

    @Test
    public void TestSimpleLoop_testLoopsBreakNext_c57020b4e0d4bfbdb7cb345d761dc311() {
        assertEval("{ i <- 0L ; while(i < 3L) { i <- i + 1 ; if (i == 1) { next } ; if (i==3) { break } ; x <- i ; if (i==4) { x <- 10 } } ; x }");
    }

    @Test
    public void TestSimpleLoop_testLoopsBreakNext_e6d103c7fe97d1e1aec5f43f1fb495d2() {
        assertEval("{ f <- function(s) { for(i in s) { if (i == 1) { next } ; if (i==3) { break } ; x <- i ; if (i==4) { x <- 10 } } ; x } ; f(2:1) ; f(c(1,2,3,4)) }");
    }

    @Test
    public void TestSimpleLoop_testLoopsErrors_30290bc6eef9629f585deca4eb7fb0a3() {
        assertEvalError("{ while (1 < NA) { 1 } }");
    }

    @Test
    public void TestSimpleLoop_testLoopsErrors_bc6b5c193e92175abc33e62c6b4cb66c() {
        assertEvalError("{ break; }");
    }

    @Test
    public void TestSimpleLoop_testLoopsErrors_4e8d19b7c3269b63639652234d8164f8() {
        assertEvalError("{ next; }");
    }

    @Test
    public void TestSimpleLoop_testLoopsErrorsIgnore_f394e8f19fc73574a5c55ba7f8e03973() {
        assertEvalError("{ l <- quote(for(i in s) { x <- i }) ; s <- 1:3 ; eval(l) ; s <- function(){} ; eval(l) ; x }");
    }

    @Test
    public void TestSimpleLoop_testLoopsErrorsIgnore_2b1a508671083a1b18d0ddb3fe0979c2() {
        assertEvalError("{ l <- function(s) { for(i in s) { x <- i } ; x } ; l(1:3) ; s <- function(){} ; l(s) ; x }");
    }

    @Test
    public void TestSimpleLoop_testLoopsErrorsIgnore_eb72a8fa37e3e5c2ac10481c6173a724() {
        assertEvalError("{ l <- quote({ for(i in s) { x <- i } ; x }) ; f <- function(s) { eval(l) } ; f(1:3) ; s <- function(){} ; f(s) ; x }");
    }

    @Test
    public void TestSimpleLoop_testOneIterationLoops_2b49e8a8d835c688af57e7939698d86a() {
        assertEvalNoNL("{ for (a in 1) cat(a) }");
    }

    @Test
    public void TestSimpleLoop_testOneIterationLoops_d16fcada6748f3bb2cf6eb7647ccd86f() {
        assertEvalNoNL("{ for (a in 1L) cat(a) }");
    }

    @Test
    public void TestSimpleLoop_testOneIterationLoops_133be12813e36ebfe9c2af618ab288c8() {
        assertEvalNoNL("{ for (a in \"xyz\") cat(a) }");
    }

    @Test
    public void TestSimpleMatrix_testAccessDim_6470f97c7d301cf55ea6f422824b2554() {
        assertEval("{ x<-1:10; dim(x) }");
    }

    @Test
    public void TestSimpleMatrix_testAccessDim_8a790ee2171c7ec67230d47fd5a93685() {
        assertEval("{ x<-FALSE; dim(x) }");
    }

    @Test
    public void TestSimpleMatrix_testAccessDim_0fe6a0202e78f96226b6c78220c9019f() {
        assertEval("{ x<-TRUE; dim(x) }");
    }

    @Test
    public void TestSimpleMatrix_testAccessDim_bade8af98bcb15586125f1cf4f6cd17f() {
        assertEval("{ x<-1; dim(x) }");
    }

    @Test
    public void TestSimpleMatrix_testAccessDim_a0e6820bb5b00f15a2e0d421d3328356() {
        assertEval("{ x<-1L; dim(x) }");
    }

    @Test
    public void TestSimpleMatrix_testAccessDim_8a5794c7c3350416ffe145dfd29ebaf2() {
        assertEval("{ x<-c(1L, 2L, 3L); dim(x) }");
    }

    @Test
    public void TestSimpleMatrix_testAccessDim_ca5043dcd46d0a5670096a1c1d54e212() {
        assertEval("{ x<-c(1, 2, 3); dim(x) }");
    }

    @Test
    public void TestSimpleMatrix_testAccessScalarIndex_23871025be785d91a8713ca885390f83() {
        assertEval("{ x<-c(1L,2L,3L,4L,5L,6L); dim(x) <- c(2,3); x[1,2] }");
    }

    @Test
    public void TestSimpleMatrix_testAccessScalarIndex_b970d364e61f5cab20df21ddb613d69c() {
        assertEval("{ x<-c(1L,2L,3L,4L,5L,6L,7L,8L,9L,10L); dim(x) <- c(2,5); x[2,4] }");
    }

    @Test
    public void TestSimpleMatrix_testAccessScalarIndex_94fdc60f4cebbc0434c623cc82fd0f35() {
        assertEval("{ x<-c(1, 2, 3, 4); dim(x)<-c(2, 2); x[1] }");
    }

    @Test
    public void TestSimpleMatrix_testAccessScalarIndex_e4c95f3a7a97a99a6fd32061257e6e0b() {
        assertEval("{ x<-c(1+1i, 2+2i, 3+3i, 4+4i); dim(x)<-c(2, 2); x[1, 1] }");
    }

    @Test
    public void TestSimpleMatrix_testAccessScalarIndex_300ef4163de9cd7969bf3979f4f471f3() {
        assertEval("{ x<-c(FALSE, TRUE, TRUE, FALSE); dim(x)<-c(2, 2); x[1, 1] }");
    }

    @Test
    public void TestSimpleMatrix_testAccessScalarIndex_bc4958b450b2bfdf1383f35fb17cfb3a() {
        assertEval("{ x<-c(\"a\", \"b\", \"c\", \"d\"); dim(x)<-c(2, 2); x[1, 1] }");
    }

    @Test
    public void TestSimpleMatrix_testAccessScalarIndex_38338d5474331aa6eeff230e7b5ba879() {
        assertEval("{ x<-1:8; dim(x)<-c(2,4);  x[1, TRUE] }");
    }

    @Test
    public void TestSimpleMatrix_testAccessScalarIndex_db156d54bea8cb65414cbc93089ecf17() {
        assertEval("{ x<-1:8; dim(x)<-c(2,4);  x[1, FALSE] }");
    }

    @Test
    public void TestSimpleMatrix_testAccessScalarIndex_1c896ed27494abd74b02ac6f757b9ac0() {
        assertEval("{ x<-1:8; dim(x)<-c(2,4);  x[1, c(TRUE, FALSE, TRUE, FALSE)] }");
    }

    @Test
    public void TestSimpleMatrix_testAccessScalarIndex_dd0a5fa1d3a35144101e702bc966c97d() {
        assertEval("{ x<-1:8; dim(x)<-c(2,4);  x[1, c(TRUE, FALSE, TRUE)] }");
    }

    @Test
    public void TestSimpleMatrix_testAccessScalarIndex_0cfe5fa46c26e276668996e57b232f40() {
        assertEval("{ x<-(1:8); dim(x)<-c(2,4); x[1, c(NA, NA)] }");
    }

    @Test
    public void TestSimpleMatrix_testAccessScalarIndex_3c49481cdcb36b7f447b368f80f306fb() {
        assertEval("{ x<-(1:8); dim(x)<-c(2,4); x[1, c(1, NA)] }");
    }

    @Test
    public void TestSimpleMatrix_testAccessScalarIndex_877ed90ef0b07fff954fea3ad5704fc9() {
        assertEval("{ x<-(1:4); dim(x)<-c(2,2); dimnames(x)<-list(c(\"a\", \"b\"), c(\"c\", \"d\")); x[1, NA] }");
    }

    @Test
    public void TestSimpleMatrix_testAccessScalarIndex_6c11754386b4cea4b3b5e02ba04b74c7() {
        assertEval("{ x<-(1:4); dim(x)<-c(2,2); dimnames(x)<-list(c(\"a\", \"b\"), c(\"c\", \"d\")); x[NA, 1] }");
    }

    @Test
    public void TestSimpleMatrix_testAccessScalarIndex_f283c238b6f4173d639057369fa0fd31() {
        assertEval("{ x<-1:16; dim(x)<-c(4,4); x[-1,-2] }");
    }

    @Test
    public void TestSimpleMatrix_testAccessScalarIndex_48402becb93ba8b2f5691f90dc26bd2d() {
        assertEval("{ x<-1:16; dim(x)<-c(4,4); x[-1,c(1,1,2,3)] }");
    }

    @Test
    public void TestSimpleMatrix_testAccessScalarIndex_90132b2a193dad2d92381b594de9b112() {
        assertEval("{ x<-1:4; dim(x)<-c(4,1); dimnames(x)<-list(c(\"a\", \"b\", \"c\", \"d\"), \"z\"); x[, 1] }");
    }

    @Test
    public void TestSimpleMatrix_testAccessScalarIndex_6681aef9db0540fe03ac4a1bad8d0e82() {
        assertEval("{ x<-1:4; dim(x)<-c(4,1); dimnames(x)<-list(c(\"a\", \"b\", \"c\", \"d\"), \"z\"); x[c(2,4), 1] }");
    }

    @Test
    public void TestSimpleMatrix_testAccessScalarIndex_12eb5030bb09607d1eb6a514959ea2a3() {
        assertEval("{ x<-1:4; dim(x)<-c(2,2); dimnames(x)<-list(c(\"a\", \"b\"), c(\"d\", \"e\")); x[1, 1] }");
    }

    @Test
    public void TestSimpleMatrix_testAccessScalarIndex_fd66aadc9f2fd9aa7ca6e3df54185524() {
        assertEval("{ x<-1:4; dim(x)<-c(2,2); dimnames(x)<-list(c(\"a\", \"b\"), c(\"d\", \"e\")); x[c(1,1), 1] }");
    }

    @Test
    public void TestSimpleMatrix_testAccessScalarIndex_4f67e9f39a7503fb10d7a531ae2951f4() {
        assertEval("{ x<-1:4; dim(x)<-c(2,2); dimnames(x)<-list(c(\"a\", \"b\"), c(\"d\", \"e\")); x[c(1,2), 1] }");
    }

    @Test
    public void TestSimpleMatrix_testAccessScalarIndex_6cc4cb472cb00d5ef8a561c3d5637de9() {
        assertEval("{ x<-1:4; dim(x)<-c(2,2); dimnames(x)<-list(c(\"a\", \"b\"), c(\"d\", \"e\")); x[c(1,2,1), 1] }");
    }

    @Test
    public void TestSimpleMatrix_testAccessScalarIndex_b5888200ddf246d4ac8dd86539184029() {
        assertEval("{ x<-1:4; dim(x)<-c(2,2); dimnames(x)<-list(c(\"a\", \"b\"), c(\"d\", \"e\")); x[\"b\", 1] }");
    }

    @Test
    public void TestSimpleMatrix_testAccessScalarIndex_908d675bd2ddd444451a0355ec9bf7d4() {
        assertEval("{ x<-1:4; dim(x)<-c(2,2); dimnames(x)<-list(c(\"a\", \"b\"), c(\"d\", \"e\")); x[c(\"b\"), 1] }");
    }

    @Test
    public void TestSimpleMatrix_testAccessScalarIndex_0f5bc7e7bccfed6ec5321b68c044f1ae() {
        assertEval("{ x<-1:4; dim(x)<-c(2,2); dimnames(x)<-list(c(\"a\", \"b\"), c(\"d\", \"e\")); x[c(\"a\", \"b\"), 1] }");
    }

    @Test
    public void TestSimpleMatrix_testAccessScalarIndex_1ba7fe23ec0a449c904545570200ccb1() {
        assertEval("{ x<-1:4; dim(x)<-c(2,2); dimnames(x)<-list(c(\"a\", \"b\"), c(\"d\", \"e\")); x[c(\"a\", \"a\"), 1] }");
    }

    @Test
    public void TestSimpleMatrix_testAccessScalarIndex_4c376b0ccf37c503c4a44dc5f78132cf() {
        assertEval("{ x<-1:2; dim(x)<-c(1:2); dimnames(x)<-list(\"z\", c(\"a\", \"b\")); x[\"z\", 1] }");
    }

    @Test
    public void TestSimpleMatrix_testAccessScalarIndex_eafc095b1d9c3d14a07a0158b746e3b8() {
        assertEval("{ x<-1:2; dim(x)<-c(1:2); dimnames(x)<-list(\"z\", c(\"a\", \"b\")); x[c(\"z\", \"z\"), 1] }");
    }

    @Test
    public void TestSimpleMatrix_testAccessScalarIndex_736aa1170a184e51af6878c1e0fdf353() {
        assertEvalError("{ x<-1:8; dim(x)<-c(2,2,2); x[1, 2] }");
    }

    @Test
    public void TestSimpleMatrix_testAccessScalarIndex_80e3d4ff9a7a919485b1b1696a77db9a() {
        assertEvalError("{ x<-1:8; x[1, 2] }");
    }

    @Test
    public void TestSimpleMatrix_testAccessScalarIndex_7a671d6881d9d33742413c97ae2a3660() {
        assertEvalError("{ x<-1:4; dim(x)<-c(2,2); x[1,3] }");
    }

    @Test
    public void TestSimpleMatrix_testAccessScalarIndex_64a62a50d06389f6a1a7d3c61caa35c3() {
        assertEvalError("{ x<-1:8; dim(x)<-c(2, 4); x[c(-1, -2),c(5)] }");
    }

    @Test
    public void TestSimpleMatrix_testAccessScalarIndex_112ecfdbb435356eb1b836e9064960f2() {
        assertEvalError("{ x<-1:8; dim(x)<-c(2,4);  x[1, c(TRUE, FALSE, TRUE, TRUE, TRUE)] }");
    }

    @Test
    public void TestSimpleMatrix_testAccessScalarIndex_02ed4a00e7024d8a0dece052620f9251() {
        assertEvalError("{ x<-1:4; dim(x)<-c(2,2); dimnames(x)<-list(c(\"a\", \"b\"), c(\"d\", \"e\")); x[\"d\", 1] }");
    }

    @Test
    public void TestSimpleMatrix_testAccessScalarIndex_901876df0956efc91604b2151ff8de01() {
        assertEvalError("{ x<-1:4; dim(x)<-c(2,2); dimnames(x)<-list(c(\"a\", \"b\"), c(\"d\", \"e\")); x[as.character(NA), 1] }");
    }

    @Test
    public void TestSimpleMatrix_testMatrixAccessWithScalarAndVector_f7e6d694531cccdce652b564fa32a0d5() {
        assertEval("{ i <- c(1L,3L,5L) ; m <- 1:10 ; dim(m) <- c(2,5) ; m[2,i] }");
    }

    @Test
    public void TestSimpleMatrix_testMatrixAccessWithScalarAndVector_5296fa0b525cb2b63cc0e89f623ed76a() {
        assertEval("{ i <- c(1L,3L,5L) ; m <- c(\"a\",\"b\",\"c\",\"d\",\"e\",\"f\",\"g\",\"h\",\"i\",\"j\") ; dim(m) <- c(2,5) ; m[2,i] }");
    }

    @Test
    public void TestSimpleMatrix_testUpdateDim_e8436aea143b529d58eaf56176937b43() {
        assertEval("{ x<-c(1L,2L,3L,4L,5L,6L); dim(x) <- c(2.1,3.9); dim(x) }");
    }

    @Test
    public void TestSimpleMatrix_testUpdateDim_980308d6cdf53f1640b22a8a93311cf9() {
        assertEval("{ x<-c(1L,2L,3L,4L,5L,6L); dim(x) <- c(2L,3L); dim(x) }");
    }

    @Test
    public void TestSimpleMatrix_testUpdateDim_2e069f5ecd8acf85a869c1fc37aa3234() {
        assertEval("{ x<-c(1L,2L,3L,4L,5L,6L); dim(x) <- c(2L,3L); dim(x) <- NULL; dim(x) }");
    }

    @Test
    public void TestSimpleMatrix_testUpdateDim_e8b11f590e8d94f01d81c721517e6853() {
        assertEval("{ x<-c(1,2,3,4,5,6); dim(x) <- c(2L,3L); dim(x) }");
    }

    @Test
    public void TestSimpleMatrix_testUpdateDim_487c47c990819a887cf65b58c290bbb1() {
        assertEval("{ x<-c(1,2,3,4,5,6); dim(x) <- c(2L,3L); dim(x) <- NULL; dim(x) }");
    }

    @Test
    public void TestSimpleMatrix_testUpdateScalarIndex_bd3cb031dca50b8f2f57d0c4933ce350() {
        assertEval("{ x<-c(1L,2L,3L,4L,5L,6L); dim(x) <- c(2,3); x[1,2] <- 100L; x[1,2] }");
    }

    @Test
    public void TestSimpleMatrix_testUpdateScalarIndex_4b8ac2caeaba39b5b1ac28be9587e4ed() {
        assertEval("{ x<-c(1L,2L,3L,4L,5L,6L,7L,8L,9L,10L); dim(x) <- c(2,5); x[2,4] <- 100L; x[2,4] }");
    }

    @Test
    public void TestSimpleMatrix_testUpdateScalarIndex_d9165729b589596d2e2a7dbb2f0d60f5() {
        assertEval("{ x<-c(1.1, 2.2, 3.3, 4.4); dim(x)<-c(2,2); x[, c(1)] }");
    }

    @Test
    public void TestSimpleMatrix_testUpdateScalarIndex_ba4a52b20ed7345ef31cf3e659a87dbc() {
        assertEval("{ x<-c(1.1, 2.2, 3.3, 4.4); dim(x)<-c(2,2); x[c(1), ] }");
    }

    @Test
    public void TestSimpleMatrix_testUpdateScalarIndex_8c47572d02e22a37037f2be7f2be3d98() {
        assertEval("{ x<-c(1.1, 2.2, 3.3, 4.4); dim(x)<-c(2,2); x[, c(1,2)] }");
    }

    @Test
    public void TestSimpleMatrix_testUpdateScalarIndex_ec328e4169bc19f5fa0381aed577f9a4() {
        assertEval("{ x<-c(1.1, 2.2, 3.3, 4.4); dim(x)<-c(2,2); x[c(1,2), ] }");
    }

    @Test
    public void TestSimpleMatrix_testUpdateScalarIndex_31ebd8e266314975219ed84586986401() {
        assertEval("{ x<-c(1,2,3,4); dim(x)<-c(2,2); x[,][1]<-42; x }");
    }

    @Test
    public void TestSimpleMatrix_testUpdateScalarIndex_4967f8b2cc2f04f762ff05e4f74b9bc0() {
        assertEval("{  x<-c(1,2,3,4); dim(x)<-c(2,2); x[3][1]<-42; x }");
    }

    @Test
    public void TestSimpleMatrix_testUpdateScalarIndex_b527449d0f1c203e299497a4644b6af8() {
        assertEval("{  x<-c(1,2,3,4); dim(x)<-c(2,2); x[3][1][1]<-42; x }");
    }

    @Test
    public void TestSimpleMatrix_testUpdateScalarIndex_6c7cbcb2bab50d0736a5bbe09c042585() {
        assertEval("{ x<-c(1L,2L,3L,4L); dim(x)<-c(2,2); f<-function() { x[3][1]<-42; x }; f() }");
    }

    @Test
    public void TestSimpleMatrix_testUpdateScalarIndex_8e4e377c0dcf0307b5bc7a010993e859() {
        assertEval("{ x<-c(1L,2L,3L,4L); dim(x)<-c(2,2); f<-function() { x[3][1]<-42; }; f(); x }");
    }

    @Test
    public void TestSimpleSequences_testSequenceConstruction_a0aafd2380a76f7e9301d729381845b6() {
        assertEval("{ 1:3 }");
    }

    @Test
    public void TestSimpleSequences_testSequenceConstruction_7d1f7dfd0be44d74a8f958186dda4ba0() {
        assertEval("{ 1.1:3.1 }");
    }

    @Test
    public void TestSimpleSequences_testSequenceConstruction_64d3f25fe0104d28e16a65f7d75666ee() {
        assertEval("{ 3:1 }");
    }

    @Test
    public void TestSimpleSequences_testSequenceConstruction_596f23a8951f029774b4cbcc718adb2c() {
        assertEval("{ 3.1:1 }");
    }

    @Test
    public void TestSimpleSequences_testSequenceConstructionIgnore_b9324a4b0cb6cce5fbe2323872e18705() {
        assertEvalWarning("{ (1:3):3 }");
    }

    @Test
    public void TestSimpleTruffle_test1_6aea9de6256435933f7fdbcf714437f6() {
        assertEval("{ f<-function(i) {i} ; f(1) ; f(2) }");
    }

    @Test
    public void TestSimpleTruffle_test1_9c209cc63f0ba403b98a12e725a0840e() {
        assertEval("{ f<-function() { 1:5 } ; f(); f() }");
    }

    @Test
    public void TestSimpleTruffle_test1_1a9c20179d7fdabeca2834e247e89657() {
        assertEval("{ f<-function() { length(c(1,2)) } ; f(); f() }");
    }

    @Test
    public void TestSimpleTruffle_test1_14a85cdf35cdf823f3ef3432e5c514a5() {
        assertEval("{ f<-function() { if (1) TRUE } ; f(); f() }");
    }

    @Test
    public void TestSimpleTruffle_test1_6c330d6ee67a88e8ab3d19a27f7b25c9() {
        assertEval("{ f<-function() { if (if (1) {TRUE} else {FALSE} ) 1 } ; f(); f() }");
    }

    @Test
    public void TestSimpleTruffle_test1_3047b379721539858289b2d629cd80d9() {
        assertEval("{ f<-function() { logical(0) } ; f(); f() }");
    }

    @Test
    public void TestSimpleTruffle_test1_75c1d4c5363750b1faffef11b163de15() {
        assertEval("{ f<-function(i) { if (TRUE) { i } } ; f(2) ; f(1) }");
    }

    @Test
    public void TestSimpleTruffle_test1_577fdaa937e0a2e6b789d4dffdf817a3() {
        assertEval("{ f<-function(i) { i ; if (FALSE) { 1 } else { i } } ; f(2) ; f(1) }");
    }

    @Test
    public void TestSimpleTruffle_test1_695c9df18decfa589f194a19fadf5d2d() {
        assertEval("{ f<-function(i) { i ; if (TRUE) { 1 } else { i } } ; f(2) ; f(1) }");
    }

    @Test
    public void TestSimpleTruffle_test1_e1d337c8b8c2cc8b40b2751269edfeb3() {
        assertEval("{ f<-function(i) { if(i==1) { 1 } else { i } } ; f(2) ; f(2) }");
    }

    @Test
    public void TestSimpleTruffle_test1_5d52ffc510a53dc67eacd6159998f57c() {
        assertEval("{ f<-function(i) { if(i==1) { 1 } else { i } } ; f(2) ; f(1) }");
    }

    @Test
    public void TestSimpleTruffle_test1Ignore_3ec182256a363ba8d70350f6d949593b() {
        assertEvalNoOutput("{ f<-function(i) { if(i==1) { i } } ; f(1) ; f(2) }");
    }

    @Test
    public void TestSimpleTruffle_test1Ignore_6b932d60711336223d0b7667e5e39f6d() {
        assertEvalNoOutput("{ f<-function() { if (!1) TRUE } ; f(); f() }");
    }

    @Test
    public void TestSimpleTruffle_test1Ignore_97edc61479ed325f8e463f75a53a34d4() {
        assertEvalNoOutput("{ f<-function() { if (!TRUE) 1 } ; f(); f() }");
    }

    @Test
    public void TestSimpleTruffle_test1Ignore_71c46963de35ffad054f0a585f749a4f() {
        assertEvalNoOutput("{ f<-function(i) { if (FALSE) { i } } ; f(2) ; f(1) }");
    }

    @Test
    public void TestSimpleTruffle_testLoop_58312576d302ffbb45436397fb9f8815() {
        assertEval("{ f<-function() { x<-210 ; repeat { x <- x + 1 ; break } ; x } ; f() ; f() }");
    }

    @Test
    public void TestSimpleTruffle_testLoop_e4803a48856bde5db424c421caa03d91() {
        assertEval("{ f<-function() { x<-1 ; repeat { x <- x + 1 ; if (x > 11) { break } } ; x } ; f(); f() }");
    }

    @Test
    public void TestSimpleTruffle_testLoop_3fec88aa0477aeda520abced9da93648() {
        assertEval("{ f<-function() { x<-1 ; repeat { x <- x + 1 ; if (x <= 11) { next } else { break } ; x <- 1024 } ; x } ; f() ; f() }");
    }

    @Test
    public void TestSimpleTruffle_testLoop_f02918e1bdf7b940e14719d4952bdeea() {
        assertEval("{ f<-function() { x<-1 ; while(TRUE) { x <- x + 1 ; if (x > 11) { break } } ; x } ; f(); f() }");
    }

    @Test
    public void TestSimpleTruffle_testLoop_d7967d49723aa632df85db9a5820b2cc() {
        assertEval("{ f<-function() { x<-1 ; while(x <= 10) { x<-x+1 } ; x } ; f(); f() }");
    }

    @Test
    public void TestSimpleTruffle_testLoop_d2fd7b9921214141f5bf38d20a6074b4() {
        assertEval("{ f<-function() { x<-1 ; for(i in 1:10) { x<-x+1 } ; x } ; f(); f() }");
    }

    @Test
    public void TestSimpleTruffle_testWarningsAndErrors_3bcccdb6b89f944c76928e4d11276b6e() {
        assertEvalErrorWarning("{ 1i > (c(1, 2) < c(1, 2, 3)) }");
    }

    @Test
    public void TestSimpleTruffle_testWarningsAndErrors_f2097159b99fe87d699d53500008dc0c() {
        assertEvalErrorWarning("{ 1i > ((c(1, 2) < c(1, 2, 3)) ==  (c(1, 2) < c(1, 3, 4))) }");
    }

    @Test
    public void TestSimpleTruffle_testWarningsAndErrors_9eb71b5a95179d81a82d0dda30c88935() {
        assertEvalWarning("{ (c(1, 2) < c(1, 2, 3)) ==  (c(1, 2) < c(1, 3, 4)) }");
    }

    @Test
    public void TestSimpleValues_testBinaryArithmetic_beaa81d2bea3ff575e89078614aadfbd() {
        assertEval("FALSE^(-3)");
    }

    @Test
    public void TestSimpleValues_testComplex_65fd146574282acbbc892c088995981c() {
        assertEval("{ 1i }");
    }

    @Test
    public void TestSimpleValues_testDefaultVariables_71c55934e32576774664d7bc1d063085() {
        assertEval("{ .Platform$endian }");
    }

    @Test
    public void TestSimpleValues_testSpecial_14f67f9b7533cb499c464ac21d9aba7c() {
        assertEval("{ NULL }");
    }

    @Test
    public void TestSimpleValues_testSpecial_656dd8da1a29a8f3fa13b9cab79deafa() {
        assertEval("{ NA }");
    }

    @Test
    public void TestSimpleValues_testSpecial_042432b01c6b0643a6e3dfce4ec8b2e8() {
        assertEval("{ Inf }");
    }

    @Test
    public void TestSimpleValues_testSpecial_bc23ab8b0106790ae54939bd98dceed3() {
        assertEval("{ NaN }");
    }

    @Test
    public void TestSimpleValues_testStrings_0cb9c0de703a15078809ec0dde49d18a() {
        assertEval("{ \"hello\" }");
    }

    @Test
    public void TestSimpleValues_testTranspose_626830f106140278db741cbf7d867b2e() {
        assertEval("x <- c(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L); dim(x) <- c(2L,5L); x <- t(x); dim(x) <- NULL; x");
    }

    @Test
    public void TestSimpleValues_testTranspose_128d8ff03ef139c1383b2606113feae5() {
        assertEval("x <- c(1, 2, 3, 4, 5, 6, 7, 8, 9, 10); dim(x) <- c(2L,5L); x <- t(x); dim(x) <- NULL; x");
    }

    @Test
    public void TestSimpleValues_testUnlist_7910f1cb1969ab32f4c20dc7220459c3() {
        assertEval("x <- list(1, 2, 3); unlist(x);");
    }

    @Test
    public void TestSimpleValues_testUnlist_1a84ff13e0d90d02a6911bebf5de33e5() {
        assertEval("x <- list(1, TRUE, 3); unlist(x);");
    }

    @Test
    public void TestSimpleValues_testUnlist_6c58fdbd0cb3b0b023373dc2bdc09904() {
        assertEval("x <- list(1, 2, NA); unlist(x);");
    }

    @Test
    public void TestSimpleValues_testUnlist_7da334f70d826adc51596947eb731890() {
        assertEval("x <- list(1L, 2L, 3L); unlist(x);");
    }

    @Test
    public void TestSimpleValues_testVectorStringAccess_5f6081387a97951664aa429ac452e60e() {
        assertEval("x <- NULL; x[c(\"A\", \"A\", \"B\")] <- 1; names(x)");
    }

    @Test
    public void TestSimpleValues_testVectorStringAccess_67fc3d00f106a64c9a45ad01471b862d() {
        assertEval("y <- NULL; y[c(\"A\", \"A\", \"B\")] <- 1; y <- NULL; names(y)");
    }

    @Test
    public void TestSimpleValues_testVectorStringAccess_3948ba3c355ea9a2c8f8d9600ebfb821() {
        assertEval("x <- NULL; names(x)");
    }

    @Test
    public void TestSimpleValues_testVectorStringAccess_b25c5965153c0024deca1da999c633ef() {
        assertEval("x <- NULL; x[c(\"A\", \"A\", \"B\")] <- \"x\"; names(x[\"A\"])");
    }

    @Test
    public void TestSimpleValues_testVectorStringAccess_c37b003dc8f16f1f7174a12876d3355c() {
        assertEval("x <- NULL; x[c(\"A\", \"A\", \"B\")] <- \"x\"; as.vector(x[\"A\"])");
    }

    @Test
    public void TestSimpleValues_testVectorStringAccess_9cf80bbd67cd7ea0d0cf7e204b0c8e9c() {
        assertEval("x <- NULL; x[c(\"A\", \"B\", \"C\")] <- c(\"x\", \"y\", \"z\"); as.vector(x[\"B\"])");
    }

    @Test
    public void TestSimpleValues_testVectorStringAccess_c4a83ac0c23d99f51ca7d805b3858b94() {
        assertEval("x <- NULL; x[c(\"A\", \"B\", \"C\")] <- c(\"x\", \"y\", \"z\"); as.vector(x[\"C\"])");
    }

    @Test
    public void TestSimpleValues_testVectorStringAccess_2310c9bf86ab7260cb19f0bf091bc4cb() {
        assertEval("x <- NULL; codes <- c(\"A\", \"C\", \"G\"); complements <- c(\"T\", \"G\", \"C\"); x[codes] <- complements; as.vector(x);");
    }

    @Test
    public void TestSimpleValues_testVectorStringAccess_d7f8eefef5cff7b032219f2be8488e9d() {
        assertEval("x <- NULL; codes <- c(\"A\", \"C\", \"G\"); complements <- c(\"T\", \"G\", \"C\"); x[codes] <- complements; names(x);");
    }

    @Test
    public void TestSimpleValues_testVectorStringAccess_f1417e2cdd7b64b082f9e55141370e04() {
        assertEval("x <- NULL; codes <- c(\"A\", \"C\", \"G\"); complements <- c(\"T\", \"G\", \"C\"); x[codes] <- complements; x[tolower(codes)] <- complements; as.vector(x);");
    }

    @Test
    public void TestSimpleValues_testVectorStringAccess_5ef9b34206654d1379338a048a887bac() {
        assertEval("x <- NULL; codes <- c(\"A\", \"C\", \"G\"); complements <- c(\"T\", \"G\", \"C\"); x[codes] <- complements; x[tolower(codes)] <- complements; names(x);");
    }

    @Test
    public void TestSimpleValues_testVectorUpdate_16fea05cc3e31435537b282fa267f339() {
        assertEval("x <- c(1, 2, 3); y <- x; x[1] <- 100; y;");
    }

    @Test
    public void TestSimpleValues_testVectorUpdate_4b9b71e942c9f564851d13050899c9fc() {
        assertEval("x <- 1:10; for (i in 1:2) { x[[1]] <- x[[1]]; x <- c(1, 2, 3) }; x");
    }

    @Test
    public void TestSimpleValues_testVectorUpdate_fe1341f09775a2455f14c247cbe19c77() {
        assertEval("v <- double(5) ; v[[3]] <- c(1) ; v");
    }

    @Test
    public void TestSimpleValues_testVectorUpdate_1efe5b039f98e22ce9bc3c7d633a13b4() {
        assertEval("v <- double(5) ; v[[3]] <- matrix(c(1)) ; v");
    }

    @Test
    public void TestSimpleVectors_testAccessSequence_b23232af20650b7393e2c15e3ca3b55c() {
        assertEval("{ x<-c(1L,2L,3L,4L,5L); x[1:4][1:3][1:2][1:1] }");
    }

    @Test
    public void TestSimpleVectors_testAccessSequence_0ece2dd5789d068dcec4ff52cad23b72() {
        assertEval("{ x<-c(1L,2L,3L,4L,5L); x[2:5][2:4][2:3][2:2] }");
    }

    @Test
    public void TestSimpleVectors_testAccessSequence_27cfcb44c738956a6c44a21bd39cf11b() {
        assertEval("{ x<-c(1L,2L,3L,4L,5L); x[1:5][2:5][2:4][2:2] }");
    }

    @Test
    public void TestSimpleVectors_testComplexIndex_ee8c1f9af263214be7bfa6591824f335() {
        assertEvalError("{ x<-c(1,2,3,4); x[[1+1i]]<-NULL }");
    }

    @Test
    public void TestSimpleVectors_testComplexIndex_3d4a76fec7a23b7ff8340e0340c2e2bd() {
        assertEvalError("{ x<-c(1,2,3,4); x[[1+1i]]<-integer() }");
    }

    @Test
    public void TestSimpleVectors_testComplexIndex_c5f49b4eae0bcbd5f26fa10250903111() {
        assertEvalError("{ x<-c(1,2,3,4); x[[1+1i]]<-c(1) }");
    }

    @Test
    public void TestSimpleVectors_testComplexIndex_116bc6c97888dd6cdcce82454df4afd7() {
        assertEvalError("{ x<-c(1,2,3,4); x[[1+1i]]<-c(1,2) }");
    }

    @Test
    public void TestSimpleVectors_testComplexIndex_8c381d82f56994f32ee69603b383ee1a() {
        assertEvalError("{ x<-c(1,2,3,4); x[[1+1i]]<-c(1,2,3) }");
    }

    @Test
    public void TestSimpleVectors_testComplexIndex_d0f7d3edc331ccc8abc4d45088abef7b() {
        assertEvalError("{ x<-c(1,2,3,4); x[1+1i]<-NULL }");
    }

    @Test
    public void TestSimpleVectors_testComplexIndex_39bc3029b9c503a6fd96085d1b37eca7() {
        assertEvalError("{ x<-c(1,2,3,4); x[1+1i]<-integer() }");
    }

    @Test
    public void TestSimpleVectors_testComplexIndex_7647bd3b3d337c0002821f3cf1da3aa3() {
        assertEvalError("{ x<-c(1,2,3,4); x[1+1i]<-c(1) }");
    }

    @Test
    public void TestSimpleVectors_testComplexIndex_db0e0410e78c3e7b9ab69e2dad484845() {
        assertEvalError("{ x<-c(1,2,3,4); x[1+1i]<-c(1,2) }");
    }

    @Test
    public void TestSimpleVectors_testComplexIndex_eb3fc341164793178876282a47027609() {
        assertEvalError("{ x<-c(1,2,3,4); x[1+1i]<-c(1,2,3) }");
    }

    @Test
    public void TestSimpleVectors_testComplexIndex_593fc8e31f28867ca49ec0bbe21bbc5f() {
        assertEvalError("{ x<-c(1,2,3,4); dim(x)<-c(2,2); x[[1+1i, 1]]<-NULL }");
    }

    @Test
    public void TestSimpleVectors_testComplexIndex_07d9c32173a18ab35324fa68859484db() {
        assertEvalError("{ x<-c(1,2,3,4); dim(x)<-c(2,2); x[[1+1i, 1]]<-integer() }");
    }

    @Test
    public void TestSimpleVectors_testComplexIndex_1d6beddcd2ffd5a190aa0ce657098c90() {
        assertEvalError("{ x<-c(1,2,3,4); dim(x)<-c(2,2); x[[1+1i, 1]]<-7 }");
    }

    @Test
    public void TestSimpleVectors_testComplexIndex_bc55c375f0b6cd015e4f04b63e384ff6() {
        assertEvalError("{ x<-c(1,2,3,4); dim(x)<-c(2,2); x[[1+1i, 1]]<-c(7,42) }");
    }

    @Test
    public void TestSimpleVectors_testComplexIndex_18b832d8d859441034bd24f9ce629b94() {
        assertEvalError("{ x<-c(1,2,3,4); dim(x)<-c(2,2); x[1+1i, 1]<-NULL }");
    }

    @Test
    public void TestSimpleVectors_testComplexIndex_708e4de14faa63ebd9b07800e95f5fed() {
        assertEvalError("{ x<-c(1,2,3,4); dim(x)<-c(2,2); x[1+1i, 1]<-integer() }");
    }

    @Test
    public void TestSimpleVectors_testComplexIndex_f142c76654eb3c3c8ed6a77b31f4fa04() {
        assertEvalError("{ x<-c(1,2,3,4); dim(x)<-c(2,2); x[1+1i, 1]<-7 }");
    }

    @Test
    public void TestSimpleVectors_testComplexIndex_6d7678ac353d3842368eb9da8e1940c2() {
        assertEvalError("{ x<-c(1,2,3,4); dim(x)<-c(2,2); x[1+1i, 1]<-c(7,42) }");
    }

    @Test
    public void TestSimpleVectors_testComplexIndex_6e542fed54f4bb8212c1b12a7416e3da() {
        assertEvalError("{ x<-list(1,2,3,4); x[[1+1i]]<-NULL }");
    }

    @Test
    public void TestSimpleVectors_testComplexIndex_736a14e9b3876d23852221cf2f6fd1fa() {
        assertEvalError("{ x<-list(1,2,3,4); x[[1+1i]]<-integer() }");
    }

    @Test
    public void TestSimpleVectors_testComplexIndex_5857ee3d94208819ee56416c253cb4f8() {
        assertEvalError("{ x<-list(1,2,3,4); x[[1+1i]]<-c(1) }");
    }

    @Test
    public void TestSimpleVectors_testComplexIndex_cab3a5462c1f4a646d9b05430b647243() {
        assertEvalError("{ x<-list(1,2,3,4); x[[1+1i]]<-c(1,2) }");
    }

    @Test
    public void TestSimpleVectors_testComplexIndex_fece54bdd5b7adf09af707281e962312() {
        assertEvalError("{ x<-list(1,2,3,4); x[[1+1i]]<-c(1,2,3) }");
    }

    @Test
    public void TestSimpleVectors_testComplexIndex_fc831fb8f6046690f77aca8715c2e8a7() {
        assertEvalError("{ x<-list(1,2,3,4); x[1+1i]<-NULL }");
    }

    @Test
    public void TestSimpleVectors_testComplexIndex_e24817ee41f5719318391002ff9d5368() {
        assertEvalError("{ x<-list(1,2,3,4); x[1+1i]<-integer() }");
    }

    @Test
    public void TestSimpleVectors_testComplexIndex_cc035d3220b222138fb5b1c0e4e4db21() {
        assertEvalError("{ x<-list(1,2,3,4); x[1+1i]<-c(1) }");
    }

    @Test
    public void TestSimpleVectors_testComplexIndex_23836013fd6cc50e7d752204c1a53d91() {
        assertEvalError("{ x<-list(1,2,3,4); x[1+1i]<-c(1,2) }");
    }

    @Test
    public void TestSimpleVectors_testComplexIndex_99255e686e151be77725a86bf9bebe27() {
        assertEvalError("{ x<-list(1,2,3,4); x[1+1i]<-c(1,2,3) }");
    }

    @Test
    public void TestSimpleVectors_testComplexIndex_83b47829ea13c5371abf13b44b86e706() {
        assertEvalError("{ x<-list(1,2,3,4); dim(x)<-c(2,2); x[[1+1i, 1]]<-NULL }");
    }

    @Test
    public void TestSimpleVectors_testComplexIndex_6ea65f29dafe6f59c2fa33154520ecae() {
        assertEvalError("{ x<-list(1,2,3,4); dim(x)<-c(2,2); x[[1+1i, 1]]<-integer() }");
    }

    @Test
    public void TestSimpleVectors_testComplexIndex_8a2c948579b6d626c4f66b38e8344fad() {
        assertEvalError("{ x<-list(1,2,3,4); dim(x)<-c(2,2); x[[1+1i, 1]]<-7 }");
    }

    @Test
    public void TestSimpleVectors_testComplexIndex_c2f32e5c2361eab45aa5c20f3bad48c5() {
        assertEvalError("{ x<-list(1,2,3,4); dim(x)<-c(2,2); x[[1+1i, 1]]<-c(7,42) }");
    }

    @Test
    public void TestSimpleVectors_testComplexIndex_3778652c6b249df7e5af933d33ffdd5d() {
        assertEvalError("{ x<-list(1,2,3,4); dim(x)<-c(2,2); x[1+1i, 1]<-NULL }");
    }

    @Test
    public void TestSimpleVectors_testComplexIndex_1ef5a3e53276329564aea8f19ffbef10() {
        assertEvalError("{ x<-list(1,2,3,4); dim(x)<-c(2,2); x[1+1i, 1]<-integer() }");
    }

    @Test
    public void TestSimpleVectors_testComplexIndex_a9c3bcd09ea404fc12d16b441254b3d5() {
        assertEvalError("{ x<-list(1,2,3,4); dim(x)<-c(2,2); x[1+1i, 1]<-7 }");
    }

    @Test
    public void TestSimpleVectors_testComplexIndex_132fc955aa21b60500d415ee91e8b66b() {
        assertEvalError("{ x<-list(1,2,3,4); dim(x)<-c(2,2); x[1+1i, 1]<-c(7,42) }");
    }

    @Test
    public void TestSimpleVectors_testEmptyUpdate_92f308555b0238f4ae92a11deaa25a29() {
        assertEval("{ a <- list(); a$a = 6; a; }");
    }

    @Test
    public void TestSimpleVectors_testEmptyUpdate_13ac19cb49e0756177e01a71af7bf72f() {
        assertEval("{ a <- list(); a[['b']] = 6; a; }");
    }

    @Test
    public void TestSimpleVectors_testFieldAccess_d9784857aabbc17e8cb0685a883e7e4d() {
        assertEval("{ a <- list(a = 1, b = 2); a$a; }");
    }

    @Test
    public void TestSimpleVectors_testFieldAccess_a6caada15cce0052850c3e16ac54a160() {
        assertEval("{ a <- list(a = 1, b = 2); a$b; }");
    }

    @Test
    public void TestSimpleVectors_testFieldAccess_dcfb33dfab6ee6defa00678a6ff24c02() {
        assertEval("{ a <- list(a = 1, b = 2); a$c; }");
    }

    @Test
    public void TestSimpleVectors_testFieldAccess_9909c36bdb9435c94745a47b0cb316fa() {
        assertEval("{ a <- list(a = 1, b = 2); a$a <- 67; a; }");
    }

    @Test
    public void TestSimpleVectors_testFieldAccess_b8cd0394c85574bf293c492bc0d370fc() {
        assertEval("{ a <- list(a = 1, b = 2); a$b <- 67; a; }");
    }

    @Test
    public void TestSimpleVectors_testFieldAccess_6f69ae8c4902d61b3baa35e8fe2bf13b() {
        assertEval("{ a <- list(a = 1, b = 2); a$c <- 67; a; }");
    }

    @Test
    public void TestSimpleVectors_testFieldAccess_8a94c753a099dfe3b5b5e2a65f3ffd69() {
        assertEval("{ v <- list(xb=1, b=2, aa=3, aa=4) ; v$aa }");
    }

    @Test
    public void TestSimpleVectors_testFieldAccess_3c609a8b3cd6dbb3e719975ef0a5a888() {
        assertEval("{ x <- list(1, 2) ; x$b }");
    }

    @Test
    public void TestSimpleVectors_testFieldAccess_466525eabb17d6e7a344f57792ae4ceb() {
        assertEval("{ x <- list(a=1, b=2) ; f <- function(x) { x$b } ; f(x) ; f(x) }");
    }

    @Test
    public void TestSimpleVectors_testFieldAccess_9bb19d29b9ddbc7882974c31817e5826() {
        assertEval("{ x <- list(a=1, b=2) ; f <- function(x) { x$b } ; f(x) ; x <- list(c=2,b=10) ; f(x) }");
    }

    @Test
    public void TestSimpleVectors_testFieldAccess_1370c1274b369491f7dba97204c3b139() {
        assertEval("{ v <- list(xb=1, b=2, aa=3, aa=4) ; v$x }");
    }

    @Test
    public void TestSimpleVectors_testFieldAccess_49275e7a0dd60b3f1a9ab26f6a0e3cbc() {
        assertEval("{ v <- list(xb=1, b=2, aa=3, aa=4) ; v$a }");
    }

    @Test
    public void TestSimpleVectors_testFieldAccess_13f39dfa58ec5e47b5715397b61c8c15() {
        assertEval("{ f <- function(v) { v$x } ; f(list(xa=1, xb=2, hello=3)) ; f(list(y=2,x=3)) }");
    }

    @Test
    public void TestSimpleVectors_testFieldAccess_844d753a53a599ab792b7c02eccfc524() {
        assertEval("{ f <- function(v) { v$x } ; f(list(xa=1, xb=2, hello=3)) ; l <- list(y=2,x=3) ; f(l) ; l[[2]] <- 4 ; f(l) }");
    }

    @Test
    public void TestSimpleVectors_testFieldAccess_5323930bd315d7a9d640f80f09658876() {
        assertEvalError("{ x <- list(a=1, b=2) ; f <- function(x) { x$b } ; f(x) ; f(1:3) }");
    }

    @Test
    public void TestSimpleVectors_testFieldAccess_1bd3fbb975d1db4afe5805404942c275() {
        assertEvalError("{ a <- c(a=1,b=2); a$a; }");
    }

    @Test
    public void TestSimpleVectors_testFieldAccess_904541d16c10de1380f167ea3efaab85() {
        assertEvalWarning("{ a <- c(1,2); a$a = 3; a; }");
    }

    @Test
    public void TestSimpleVectors_testGenericUpdate_c628a46d95cf59bdaa3de23cb6ced1a4() {
        assertEval("{ a <- TRUE; a[[2]] <- FALSE; a; }");
    }

    @Test
    public void TestSimpleVectors_testIn_a575a95504a8b9280fc337e0f735d634() {
        assertEval("{ 1:3 %in% 1:10 }");
    }

    @Test
    public void TestSimpleVectors_testIn_077d02af633cc7d5756753065e754d6d() {
        assertEval("{ 1 %in% 1:10 }");
    }

    @Test
    public void TestSimpleVectors_testIn_b43c35d3772d1b2e31423b82d6bf6e4a() {
        assertEval("{ c(\"1L\",\"hello\") %in% 1:10 }");
    }

    @Test
    public void TestSimpleVectors_testIn_67ef0a883a816cec9a48a28785af9373() {
        assertEval("{ (1 + 2i) %in% c(1+10i, 1+4i, 2+2i, 1+2i) }");
    }

    @Test
    public void TestSimpleVectors_testIn_b0b384b8b31a8578c66c54d6ee04f7fa() {
        assertEval("{ as.logical(-1:1) %in% TRUE }");
    }

    @Test
    public void TestSimpleVectors_testIn_81d66358a8fad9bfb170460b17b75f0a() {
        assertEvalError("{ x <- function(){1} ; x %in% TRUE }");
    }

    @Test
    public void TestSimpleVectors_testLengthUpdate_3d08abbfb62473ff04cb81d987d154fe() {
        assertEval("{ k <- c(1,2,3) ; length(k) <- 5 ; k }");
    }

    @Test
    public void TestSimpleVectors_testLengthUpdate_131bae9878b726d906186a1726be20ff() {
        assertEval("{ k <- c(1,2,3,4,5,6,7,8,9) ; length(k) <- 4 ; k }");
    }

    @Test
    public void TestSimpleVectors_testListAccess_cbc4e659bc8dc93a3030d3ae4c6d73f8() {
        assertEval("{ l<-list(1,2L,TRUE) ; l[[2]] }");
    }

    @Test
    public void TestSimpleVectors_testListAccess_96d469bdfd0291a85e5d3f783e8429b1() {
        assertEval("{ l<-list(1,2L,TRUE) ; l[c(FALSE,FALSE,TRUE)] }");
    }

    @Test
    public void TestSimpleVectors_testListAccess_5af03987ea65d7b69c26634a159af3d9() {
        assertEval("{ l<-list(1,2L,TRUE) ; l[FALSE] }");
    }

    @Test
    public void TestSimpleVectors_testListAccess_b5b5a722bd1d6524a93ce5399df0f76d() {
        assertEval("{ l<-list(1,2L,TRUE) ; l[-2] }");
    }

    @Test
    public void TestSimpleVectors_testListAccess_da17959911ade0de97a32a89b4c80383() {
        assertEval("{ l<-list(1,2L,TRUE) ; l[NA] }");
    }

    @Test
    public void TestSimpleVectors_testListAccess_49bca1ef9300fb52319083ea27bcb8b7() {
        assertEval("{ l<-list(1,2,3) ; l[c(1,2)] }");
    }

    @Test
    public void TestSimpleVectors_testListAccess_d7d2b20a394ec6d75e5c13568e81e100() {
        assertEval("{ l<-list(1,2,3) ; l[c(2)] }");
    }

    @Test
    public void TestSimpleVectors_testListAccess_76681ec4fe36b4775bb4f82a987b495a() {
        assertEval("{ x<-list(1,2L,TRUE,FALSE,5) ; x[2:4] }");
    }

    @Test
    public void TestSimpleVectors_testListAccess_665594f5014067898ca0c7a188c5b6ea() {
        assertEval("{ x<-list(1,2L,TRUE,FALSE,5) ; x[4:2] }");
    }

    @Test
    public void TestSimpleVectors_testListAccess_3e21060c054b07f532a7185e7aba9220() {
        assertEval("{ x<-list(1,2L,TRUE,FALSE,5) ; x[c(-2,-3)] }");
    }

    @Test
    public void TestSimpleVectors_testListAccess_dfb237cb2f4e3c844f1b038b7d650a64() {
        assertEval("{ x<-list(1,2L,TRUE,FALSE,5) ; x[c(-2,-3,-4,0,0,0)] }");
    }

    @Test
    public void TestSimpleVectors_testListAccess_42abb55b72f43f2729de9c5a43d9398f() {
        assertEval("{ x<-list(1,2L,TRUE,FALSE,5) ; x[c(2,5,4,3,3,3,0)] }");
    }

    @Test
    public void TestSimpleVectors_testListAccess_9b84421c057421cfe3c1fb077b5051fc() {
        assertEval("{ x<-list(1,2L,TRUE,FALSE,5) ; x[c(2L,5L,4L,3L,3L,3L,0L)] }");
    }

    @Test
    public void TestSimpleVectors_testListAccess_c838bd31779c5561e124ef5e65c1f324() {
        assertEval("{ m<-list(1,2) ; m[NULL] }");
    }

    @Test
    public void TestSimpleVectors_testListAccess_67e903425a325a03ec6df2ed88371941() {
        assertEval("{ f<-function(x, i) { x[i] } ; f(list(1,2,3),3:1) ; f(list(1L,2L,3L,4L,5L),c(0,0,0,0-2)) }");
    }

    @Test
    public void TestSimpleVectors_testListAccess_abbedda69bca73ce055c366d78adaa64() {
        assertEval("{ x<-list(1,2,3,4,5) ; x[c(TRUE,TRUE,TRUE,FALSE,FALSE,FALSE,FALSE,TRUE,NA)] }");
    }

    @Test
    public void TestSimpleVectors_testListAccess_9ccdc251d03ea549e09a7febf33531d6() {
        assertEval("{ f<-function(i) { x<-list(1,2,3,4,5) ; x[i] } ; f(1) ; f(1L) ; f(TRUE) }");
    }

    @Test
    public void TestSimpleVectors_testListAccess_1735f3742a12bb4ce51cdd3b2dfd83f7() {
        assertEval("{ f<-function(i) { x<-list(1,2,3,4,5) ; x[i] } ; f(1) ; f(TRUE) ; f(1L)  }");
    }

    @Test
    public void TestSimpleVectors_testListAccess_e8af0e4df210e541e1942a6f95547b08() {
        assertEval("{ f<-function(i) { x<-list(1L,2L,3L,4L,5L) ; x[i] } ; f(1) ; f(TRUE) ; f(c(3,2))  }");
    }

    @Test
    public void TestSimpleVectors_testListAccess_87353e70931c518879ba90596f228582() {
        assertEval("{ f<-function(i) { x<-list(1,2,3,4,5) ; x[i] } ; f(1)  ; f(3:4) }");
    }

    @Test
    public void TestSimpleVectors_testListAccess_082588944220a73fd0b7bcf232d59937() {
        assertEval("{ f<-function(i) { x<-list(1,2,3,4,5) ; x[i] } ; f(c(TRUE,FALSE))  ; f(3:4) }");
    }

    @Test
    public void TestSimpleVectors_testListAccess_b8916741c65922be43af18eb013d0b98() {
        assertEval("{ l<-(list(list(1,2),list(3,4))); l[[c(1,2)]] }");
    }

    @Test
    public void TestSimpleVectors_testListAccess_79318e1bccdf5b463fa0c42b5fa4ecde() {
        assertEval("{ l<-(list(list(1,2),list(3,4))); l[[c(1,-2)]] }");
    }

    @Test
    public void TestSimpleVectors_testListAccess_7431245306ae936fe705d6a1b9d20da0() {
        assertEval("{ l<-(list(list(1,2),list(3,4))); l[[c(1,-1)]] }");
    }

    @Test
    public void TestSimpleVectors_testListAccess_afc0def74a7aeecff2f45eacb1cd39ab() {
        assertEval("{ l<-(list(list(1,2),list(3,4))); l[[c(1,TRUE)]] }");
    }

    @Test
    public void TestSimpleVectors_testListAccess_404b3f1ce68370313607ae5cbba2a612() {
        assertEval("{ l<-(list(list(1,2),c(3,4))); l[[c(2,1)]] }");
    }

    @Test
    public void TestSimpleVectors_testListAccess_3658c17582c284601a8e88f5f21fe4db() {
        assertEval("{ l <- list(a=1,b=2,c=list(d=3,e=list(f=4))) ; l[[c(3,2)]] }");
    }

    @Test
    public void TestSimpleVectors_testListAccess_afaab4fa419f9c85c8961fbf3837d893() {
        assertEval("{ l <- list(a=1,b=2,c=list(d=3,e=list(f=4))) ; l[[c(3,1)]] }");
    }

    @Test
    public void TestSimpleVectors_testListAccess_3099f08c81cb2f1f6a81dac123cf7027() {
        assertEval("{ l <- list(c=list(d=3,e=c(f=4)), b=2, a=3) ; l[[c(\"c\",\"e\")]] }");
    }

    @Test
    public void TestSimpleVectors_testListAccess_37ef0e19f732e07940389915d8b28b9b() {
        assertEval("{ l <- list(c=list(d=3,e=c(f=4)), b=2, a=3) ; l[[c(\"c\",\"e\", \"f\")]] }");
    }

    @Test
    public void TestSimpleVectors_testListAccess_584deab8c6d8454c71e7bbf9c35e1715() {
        assertEval("{ l <- list(c=list(d=3,e=c(f=4)), b=2, a=3) ; l[[c(\"c\")]] }");
    }

    @Test
    public void TestSimpleVectors_testListAccess_03080fa7bfc88ddc1cb2919ce4931b68() {
        assertEval("{ f <- function(b, i, v) { b[[i]] <- v ; b } ; f(1:3,2,2) ; f(1:3,\"X\",2) ; f(list(1,list(2)),c(2,1),4) }");
    }

    @Test
    public void TestSimpleVectors_testListDefinitions_a993d22d8e1140932dcc58196f3b02f8() {
        assertEval("{ list(1:4) }");
    }

    @Test
    public void TestSimpleVectors_testListDefinitions_efa5263d92e550f2d29597d1f2a0a9af() {
        assertEval("{ list(1,list(2,list(3,4))) }");
    }

    @Test
    public void TestSimpleVectors_testListDefinitions_acc4be6455c6572947cc9686743e559c() {
        assertEval("{ list(1,b=list(2,3)) }");
    }

    @Test
    public void TestSimpleVectors_testListDefinitions_73d482d5eae6ecb8e2bc332445d7d6e1() {
        assertEval("{ list(1,b=list(c=2,3)) }");
    }

    @Test
    public void TestSimpleVectors_testListDefinitions_76827bae891c4de50e821493d6bfa7b1() {
        assertEval("{ list(list(c=2)) }");
    }

    @Test
    public void TestSimpleVectors_testListIndex_62900648a6ddc74ec71cdffdd941b677() {
        assertEvalError("{ z<-1:4; z[list()]<-42 }");
    }

    @Test
    public void TestSimpleVectors_testListIndex_3c73f537980be0ad1aff4671449bf0ce() {
        assertEvalError("{ z<-1:4; z[list(1)]<-42 }");
    }

    @Test
    public void TestSimpleVectors_testListIndex_5fd6f1292ddf7ecb2e45e293c60dcb35() {
        assertEvalError("{ z<-1:4; z[list(1,2)]<-42 }");
    }

    @Test
    public void TestSimpleVectors_testListIndex_bc82eb8661debda6055f355c630f4a3b() {
        assertEvalError("{ z<-1:4; z[list(1,2,3)]<-42 }");
    }

    @Test
    public void TestSimpleVectors_testListIndex_04eb71b04464e58a6e6194bc118c490f() {
        assertEvalError("{ z<-1:4; z[list()]<-integer() }");
    }

    @Test
    public void TestSimpleVectors_testListIndex_021d3f0a6dc2553e61910554d768d7ce() {
        assertEvalError("{ z<-1:4; z[list(1)]<-integer() }");
    }

    @Test
    public void TestSimpleVectors_testListIndex_f7b28479f4d6c6b052715dcc2c952437() {
        assertEvalError("{ z<-1:4; z[list(1,2)]<-integer() }");
    }

    @Test
    public void TestSimpleVectors_testListIndex_b89087d4148d1b0a133bd3e979ed1a15() {
        assertEvalError("{ z<-1:4; z[list(1,2,3)]<-integer() }");
    }

    @Test
    public void TestSimpleVectors_testListIndex_4cc3fb7172d6eeaa9da9ca63f98877c6() {
        assertEvalError("{ z<-1:4; z[list()]<-NULL }");
    }

    @Test
    public void TestSimpleVectors_testListIndex_78fda1f486dc4793e895fecc117ee366() {
        assertEvalError("{ z<-1:4; z[list(1)]<-NULL }");
    }

    @Test
    public void TestSimpleVectors_testListIndex_60b46cd481fe7a50b6d32a8910984e12() {
        assertEvalError("{ z<-1:4; z[list(1,2)]<-NULL }");
    }

    @Test
    public void TestSimpleVectors_testListIndex_71d805380372eaf80cc5c624cd0f48cc() {
        assertEvalError("{ z<-1:4; z[list(1,2,3)]<-NULL }");
    }

    @Test
    public void TestSimpleVectors_testListIndex_cadd985c22720009d73c4f2888608c3b() {
        assertEvalError("{ z<-1:4; z[[list()]]<-42 }");
    }

    @Test
    public void TestSimpleVectors_testListIndex_84ae4e387e05559c9985148f1b6b59e8() {
        assertEvalError("{ z<-1:4; z[[list(1)]]<-42 }");
    }

    @Test
    public void TestSimpleVectors_testListIndex_abc93d0d72b8d64275944e04a64082ec() {
        assertEvalError("{ z<-1:4; z[[list(1,2)]]<-42 }");
    }

    @Test
    public void TestSimpleVectors_testListIndex_b48fee2960eded7f4c4301f8b10f90a0() {
        assertEvalError("{ z<-1:4; z[[list(1,2,3)]]<-42 }");
    }

    @Test
    public void TestSimpleVectors_testListIndex_d54db43170fb921445c80ea76aa2fb8d() {
        assertEvalError("{ z<-1:4; z[[list()]]<-integer() }");
    }

    @Test
    public void TestSimpleVectors_testListIndex_d931b308c6698bad69395ef90819b352() {
        assertEvalError("{ z<-1:4; z[[list(1)]]<-integer() }");
    }

    @Test
    public void TestSimpleVectors_testListIndex_f6ab26dd7463b89fa4e25f82a1c3192a() {
        assertEvalError("{ z<-1:4; z[[list(1,2)]]<-integer() }");
    }

    @Test
    public void TestSimpleVectors_testListIndex_a9a24148db437e505cd7951b68cd3e6d() {
        assertEvalError("{ z<-1:4; z[[list(1,2,3)]]<-integer() }");
    }

    @Test
    public void TestSimpleVectors_testListIndex_2223add8510c9d9c0c26ffc36fbf858e() {
        assertEvalError("{ z<-1:4; z[[list()]]<-NULL }");
    }

    @Test
    public void TestSimpleVectors_testListIndex_2856013d57376f52f235d42dbf5472cd() {
        assertEvalError("{ z<-1:4; z[[list(1)]]<-NULL }");
    }

    @Test
    public void TestSimpleVectors_testListIndex_33380d6e067ece6c4cfde428eaf27617() {
        assertEvalError("{ z<-1:4; z[[list(1,2)]]<-NULL }");
    }

    @Test
    public void TestSimpleVectors_testListIndex_ca976d07fb8869d83d5ff629eb83705d() {
        assertEvalError("{ z<-1:4; z[[list(1,2,3)]]<-NULL }");
    }

    @Test
    public void TestSimpleVectors_testListIndex_75a9767a9276c0bd7308496a627858d0() {
        assertEvalError("{ z<-list(1,2,3,4); z[list()]<-42 }");
    }

    @Test
    public void TestSimpleVectors_testListIndex_9f8bef0c4de4e2b4d005c770ed8ff575() {
        assertEvalError("{ z<-list(1,2,3,4); z[list(1)]<-42 }");
    }

    @Test
    public void TestSimpleVectors_testListIndex_b084a9315c4a485d178a1b6b234c5925() {
        assertEvalError("{ z<-list(1,2,3,4); z[list(1,2)]<-42 }");
    }

    @Test
    public void TestSimpleVectors_testListIndex_5bea88f27650b4d3739d7e3280ea3787() {
        assertEvalError("{ z<-list(1,2,3,4); z[list(1,2,3)]<-42 }");
    }

    @Test
    public void TestSimpleVectors_testListIndex_288170ea9c554d6f62392228dbfb66d2() {
        assertEvalError("{ z<-list(1,2,3,4); z[list()]<-integer() }");
    }

    @Test
    public void TestSimpleVectors_testListIndex_cdb52695336c3ddd04a1592230c1ecff() {
        assertEvalError("{ z<-list(1,2,3,4); z[list(1)]<-integer() }");
    }

    @Test
    public void TestSimpleVectors_testListIndex_55d80976c411acaa14e05eab43ab7c6d() {
        assertEvalError("{ z<-list(1,2,3,4); z[list(1,2)]<-integer() }");
    }

    @Test
    public void TestSimpleVectors_testListIndex_114975e0c2cb95ca00ce595f65d06b18() {
        assertEvalError("{ z<-list(1,2,3,4); z[list(1,2,3)]<-integer() }");
    }

    @Test
    public void TestSimpleVectors_testListIndex_9672c2cd92039341777e92906a37c716() {
        assertEvalError("{ z<-list(1,2,3,4); z[list()]<-NULL }");
    }

    @Test
    public void TestSimpleVectors_testListIndex_ea7ab1f26bc227dc131656349412a512() {
        assertEvalError("{ z<-list(1,2,3,4); z[list(1)]<-NULL }");
    }

    @Test
    public void TestSimpleVectors_testListIndex_1b64f1e78919e40fb4862e7d5cbb787b() {
        assertEvalError("{ z<-list(1,2,3,4); z[list(1,2)]<-NULL }");
    }

    @Test
    public void TestSimpleVectors_testListIndex_70268df56805a9cce50d64de154372af() {
        assertEvalError("{ z<-list(1,2,3,4); z[list(1,2,3)]<-NULL }");
    }

    @Test
    public void TestSimpleVectors_testListIndex_9840576d5623f1f65f25e0408dc4606e() {
        assertEvalError("{ z<-list(1,2,3,4); z[[list()]]<-42 }");
    }

    @Test
    public void TestSimpleVectors_testListIndex_afc1fb17ccd18e4a0d7723a68ae35f1d() {
        assertEvalError("{ z<-list(1,2,3,4); z[[list(1)]]<-42 }");
    }

    @Test
    public void TestSimpleVectors_testListIndex_e7f9a12b97532a4a8504c876c33663e2() {
        assertEvalError("{ z<-list(1,2,3,4); z[[list(1,2)]]<-42 }");
    }

    @Test
    public void TestSimpleVectors_testListIndex_1b8f01c8c4f6e00f8eb34b3ac3d4839d() {
        assertEvalError("{ z<-list(1,2,3,4); z[[list(1,2,3)]]<-42 }");
    }

    @Test
    public void TestSimpleVectors_testListIndex_642558e8ce2d56930456300bb4defae8() {
        assertEvalError("{ z<-list(1,2,3,4); z[[list()]]<-integer() }");
    }

    @Test
    public void TestSimpleVectors_testListIndex_aeddab98f9743dd8223b126ee07a8f10() {
        assertEvalError("{ z<-list(1,2,3,4); z[[list(1)]]<-integer() }");
    }

    @Test
    public void TestSimpleVectors_testListIndex_feab7717de1ee92b5d29e29220b48f71() {
        assertEvalError("{ z<-list(1,2,3,4); z[[list(1,2)]]<-integer() }");
    }

    @Test
    public void TestSimpleVectors_testListIndex_1c90e53e30bd8adabff955d90ac795c9() {
        assertEvalError("{ z<-list(1,2,3,4); z[[list(1,2,3)]]<-integer() }");
    }

    @Test
    public void TestSimpleVectors_testListIndex_1c58533e0162cfc4a7d0b8213041c84e() {
        assertEvalError("{ z<-list(1,2,3,4); z[[list()]]<-NULL }");
    }

    @Test
    public void TestSimpleVectors_testListIndex_f9635083f8a0b70845e7d644ae6ea282() {
        assertEvalError("{ z<-list(1,2,3,4); z[[list(1)]]<-NULL }");
    }

    @Test
    public void TestSimpleVectors_testListIndex_12867b37eeb74d2d3b8aa762d2db2184() {
        assertEvalError("{ z<-list(1,2,3,4); z[[list(1,2)]]<-NULL }");
    }

    @Test
    public void TestSimpleVectors_testListIndex_bdc30dc8329a493fa36375da2ba069f0() {
        assertEvalError("{ z<-list(1,2,3,4); z[[list(1,2,3)]]<-NULL }");
    }

    @Test
    public void TestSimpleVectors_testListIndex_06ea91f2dcc9297a5e9f0899e28844fa() {
        assertEvalError("{ x<-1:4; dim(x)<-c(2,2); x[list(), 1]<-integer() }");
    }

    @Test
    public void TestSimpleVectors_testListIndex_717ae5ce656be40b9360d35975183518() {
        assertEvalError("{ x<-1:4; dim(x)<-c(2,2); x[list(), 1]<-7 }");
    }

    @Test
    public void TestSimpleVectors_testListIndex_dc39833e520d1e67f8525c090703e740() {
        assertEvalError("{ x<-1:4; dim(x)<-c(2,2); x[list(), 1]<-c(7,42) }");
    }

    @Test
    public void TestSimpleVectors_testListIndex_d6d35c02bf2d9e3769b72fda0f7a2d59() {
        assertEvalError("{ x<-1:4; dim(x)<-c(2,2); x[list(1), 1]<-integer() }");
    }

    @Test
    public void TestSimpleVectors_testListIndex_656a2c14c95543bd63d0bb687a275f67() {
        assertEvalError("{ x<-1:4; dim(x)<-c(2,2); x[list(1), 1]<-7 }");
    }

    @Test
    public void TestSimpleVectors_testListIndex_5375e4b4d49e0b6b10f14ab2f997a4b2() {
        assertEvalError("{ x<-1:4; dim(x)<-c(2,2); x[list(1), 1]<-c(7,42) }");
    }

    @Test
    public void TestSimpleVectors_testListIndex_16625d65019dfc5bde882918b7f218d4() {
        assertEvalError("{ x<-1:4; dim(x)<-c(2,2); x[list(1,2), 1]<-integer() }");
    }

    @Test
    public void TestSimpleVectors_testListIndex_69a4c6fed750ae51a05f5029146a818a() {
        assertEvalError("{ x<-1:4; dim(x)<-c(2,2); x[list(1,2), 1]<-7 }");
    }

    @Test
    public void TestSimpleVectors_testListIndex_0d60d161cc14b344317f40cee89b15fd() {
        assertEvalError("{ x<-1:4; dim(x)<-c(2,2); x[list(1,2), 1]<-c(7,42) }");
    }

    @Test
    public void TestSimpleVectors_testListIndex_5a0d5c845686831e327f9457bdccd3a5() {
        assertEvalError("{ x<-1:4; dim(x)<-c(2,2); x[list(1,2,3), 1]<-integer() }");
    }

    @Test
    public void TestSimpleVectors_testListIndex_e066e9cc1508a26ff15f1c6234e4b925() {
        assertEvalError("{ x<-1:4; dim(x)<-c(2,2); x[list(1,2,3), 1]<-7 }");
    }

    @Test
    public void TestSimpleVectors_testListIndex_9703de6a78dbb2901fc5afb503093bd8() {
        assertEvalError("{ x<-1:4; dim(x)<-c(2,2); x[list(1,2,3), 1]<-c(7,42) }");
    }

    @Test
    public void TestSimpleVectors_testListIndex_8efeca0fade3c53be178130cebe3695a() {
        assertEvalError("{ x<-1:4; dim(x)<-c(2,2); x[[list(), 1]]<-integer() }");
    }

    @Test
    public void TestSimpleVectors_testListIndex_e52ea8359c4ab512f0bfac1ee5c21afb() {
        assertEvalError("{ x<-1:4; dim(x)<-c(2,2); x[[list(), 1]]<-7 }");
    }

    @Test
    public void TestSimpleVectors_testListIndex_5b0d6475d703cabef0b069e254e3c1b6() {
        assertEvalError("{ x<-1:4; dim(x)<-c(2,2); x[[list(), 1]]<-c(7,42) }");
    }

    @Test
    public void TestSimpleVectors_testListIndex_a3d41d98ebf6cd77c41db84e4b3ac36c() {
        assertEvalError("{ x<-1:4; dim(x)<-c(2,2); x[[list(1), 1]]<-integer() }");
    }

    @Test
    public void TestSimpleVectors_testListIndex_78a520793fa40c45fbf86a1939ced7e2() {
        assertEvalError("{ x<-1:4; dim(x)<-c(2,2); x[[list(1), 1]]<-7 }");
    }

    @Test
    public void TestSimpleVectors_testListIndex_f2148f9cec8b5565463ca78f0ae59005() {
        assertEvalError("{ x<-1:4; dim(x)<-c(2,2); x[[list(1), 1]]<-c(7,42) }");
    }

    @Test
    public void TestSimpleVectors_testListIndex_3955adab9e694f8faf61db7ab321be9d() {
        assertEvalError("{ x<-1:4; dim(x)<-c(2,2); x[[list(1,2), 1]]<-integer() }");
    }

    @Test
    public void TestSimpleVectors_testListIndex_d252d7a077ce045da325a5124705738d() {
        assertEvalError("{ x<-1:4; dim(x)<-c(2,2); x[[list(1,2), 1]]<-7 }");
    }

    @Test
    public void TestSimpleVectors_testListIndex_f2de9bcdf61358da78de57b24b845850() {
        assertEvalError("{ x<-1:4; dim(x)<-c(2,2); x[[list(1,2), 1]]<-c(7,42) }");
    }

    @Test
    public void TestSimpleVectors_testListIndex_c672485831450faa2e3f2f9c3808bccd() {
        assertEvalError("{ x<-1:4; dim(x)<-c(2,2); x[[list(1,2,3), 1]]<-integer() }");
    }

    @Test
    public void TestSimpleVectors_testListIndex_1942a5e56ab9f97abd6c4d05d51a1702() {
        assertEvalError("{ x<-1:4; dim(x)<-c(2,2); x[[list(1,2,3), 1]]<-7 }");
    }

    @Test
    public void TestSimpleVectors_testListIndex_ce09bb8b399554dccc9f1e1486c01c52() {
        assertEvalError("{ x<-1:4; dim(x)<-c(2,2); x[[list(1,2,3), 1]]<-c(7,42) }");
    }

    @Test
    public void TestSimpleVectors_testListIndex_dedff0e279d3d0cb9ba10ed582fa3152() {
        assertEvalError("{ x<-list(1,2,3,4); dim(x)<-c(2,2); x[list(), 1]<-NULL }");
    }

    @Test
    public void TestSimpleVectors_testListIndex_fd1d80f7670eef4ac171bfb734a53c1b() {
        assertEvalError("{ x<-list(1,2,3,4); dim(x)<-c(2,2); x[list(), 1]<-integer() }");
    }

    @Test
    public void TestSimpleVectors_testListIndex_d3e219c57e5a07b2f57556e45477097d() {
        assertEvalError("{ x<-list(1,2,3,4); dim(x)<-c(2,2); x[list(), 1]<-7 }");
    }

    @Test
    public void TestSimpleVectors_testListIndex_b7179a3214e97a1f5f3048d3c4ca041d() {
        assertEvalError("{ x<-list(1,2,3,4); dim(x)<-c(2,2); x[list(), 1]<-c(7,42) }");
    }

    @Test
    public void TestSimpleVectors_testListIndex_8d65d2ef25faa6cd217c797139a40601() {
        assertEvalError("{ x<-list(1,2,3,4); dim(x)<-c(2,2); x[list(1), 1]<-NULL }");
    }

    @Test
    public void TestSimpleVectors_testListIndex_74bc6720206f5c4510a967d5869ace25() {
        assertEvalError("{ x<-list(1,2,3,4); dim(x)<-c(2,2); x[list(1), 1]<-integer() }");
    }

    @Test
    public void TestSimpleVectors_testListIndex_b6645ec751feafcc2a0ab6c338018d36() {
        assertEvalError("{ x<-list(1,2,3,4); dim(x)<-c(2,2); x[list(1), 1]<-7 }");
    }

    @Test
    public void TestSimpleVectors_testListIndex_01ab1779e20386bcac2c2f439552aecb() {
        assertEvalError("{ x<-list(1,2,3,4); dim(x)<-c(2,2); x[list(1), 1]<-c(7,42) }");
    }

    @Test
    public void TestSimpleVectors_testListIndex_f37849cda758f0b210634e33940f4250() {
        assertEvalError("{ x<-list(1,2,3,4); dim(x)<-c(2,2); x[list(1,2), 1]<-integer() }");
    }

    @Test
    public void TestSimpleVectors_testListIndex_f9765afaeaff0c1f2d7cf22ee80244a7() {
        assertEvalError("{ x<-list(1,2,3,4); dim(x)<-c(2,2); x[list(1,2), 1]<-NULL }");
    }

    @Test
    public void TestSimpleVectors_testListIndex_308bc4bf8d49f49148cc26ab9b56294e() {
        assertEvalError("{ x<-list(1,2,3,4); dim(x)<-c(2,2); x[list(1,2), 1]<-7 }");
    }

    @Test
    public void TestSimpleVectors_testListIndex_2aa96a5b66f100367be104c1eed1f53d() {
        assertEvalError("{ x<-list(1,2,3,4); dim(x)<-c(2,2); x[list(1,2), 1]<-c(7,42) }");
    }

    @Test
    public void TestSimpleVectors_testListIndex_2c61cae0fce93f129c9d027cb0556f72() {
        assertEvalError("{ x<-list(1,2,3,4); dim(x)<-c(2,2); x[list(1,2,3), 1]<-NULL }");
    }

    @Test
    public void TestSimpleVectors_testListIndex_4e2403844d7481cbf3fab673ea14165b() {
        assertEvalError("{ x<-list(1,2,3,4); dim(x)<-c(2,2); x[list(1,2,3), 1]<-integer() }");
    }

    @Test
    public void TestSimpleVectors_testListIndex_1c372cd74c16d924956b2927d57b8f49() {
        assertEvalError("{ x<-list(1,2,3,4); dim(x)<-c(2,2); x[list(1,2,3), 1]<-7 }");
    }

    @Test
    public void TestSimpleVectors_testListIndex_bdd91872a3af544ed9ffe0761d7ba87e() {
        assertEvalError("{ x<-list(1,2,3,4); dim(x)<-c(2,2); x[list(1,2,3), 1]<-c(7,42) }");
    }

    @Test
    public void TestSimpleVectors_testListIndex_98fec56e4f3b22ff9cf15381e61855c6() {
        assertEvalError("{ x<-list(1,2,3,4); dim(x)<-c(2,2); x[[list(), 1]]<-NULL }");
    }

    @Test
    public void TestSimpleVectors_testListIndex_eec4128492fad3e345b07152a9263c0b() {
        assertEvalError("{ x<-list(1,2,3,4); dim(x)<-c(2,2); x[[list(), 1]]<-integer() }");
    }

    @Test
    public void TestSimpleVectors_testListIndex_f3dd5b77fe0463c4b792435514e3f8a1() {
        assertEvalError("{ x<-list(1,2,3,4); dim(x)<-c(2,2); x[[list(), 1]]<-7 }");
    }

    @Test
    public void TestSimpleVectors_testListIndex_92368132eb8d2aa468c9a26e3b5a860e() {
        assertEvalError("{ x<-list(1,2,3,4); dim(x)<-c(2,2); x[[list(), 1]]<-c(7,42) }");
    }

    @Test
    public void TestSimpleVectors_testListIndex_5be4b07baeef02540de1d78befe86dbb() {
        assertEvalError("{ x<-list(1,2,3,4); dim(x)<-c(2,2); x[[list(1), 1]]<-NULL }");
    }

    @Test
    public void TestSimpleVectors_testListIndex_6b8536e0e74eca54af6618e90d2f3ffa() {
        assertEvalError("{ x<-list(1,2,3,4); dim(x)<-c(2,2); x[[list(1), 1]]<-integer() }");
    }

    @Test
    public void TestSimpleVectors_testListIndex_6323ef4998bcca5a97a4138231693c72() {
        assertEvalError("{ x<-list(1,2,3,4); dim(x)<-c(2,2); x[[list(1), 1]]<-7 }");
    }

    @Test
    public void TestSimpleVectors_testListIndex_9bad4c65246379e164507b496f375613() {
        assertEvalError("{ x<-list(1,2,3,4); dim(x)<-c(2,2); x[[list(1), 1]]<-c(7,42) }");
    }

    @Test
    public void TestSimpleVectors_testListIndex_e26a7e1237100dcedee81b108effc2ec() {
        assertEvalError("{ x<-list(1,2,3,4); dim(x)<-c(2,2); x[[list(1,2), 1]]<-NULL }");
    }

    @Test
    public void TestSimpleVectors_testListIndex_bad7d96c7d63e25aeaa9fab6284afed5() {
        assertEvalError("{ x<-list(1,2,3,4); dim(x)<-c(2,2); x[[list(1,2), 1]]<-integer() }");
    }

    @Test
    public void TestSimpleVectors_testListIndex_65e5557e141c503cbeda6e2560e64a1d() {
        assertEvalError("{ x<-list(1,2,3,4); dim(x)<-c(2,2); x[[list(1,2), 1]]<-7 }");
    }

    @Test
    public void TestSimpleVectors_testListIndex_cd0e33fe6fc95a3eecd46dd8fbb0915a() {
        assertEvalError("{ x<-list(1,2,3,4); dim(x)<-c(2,2); x[[list(1,2), 1]]<-c(7,42) }");
    }

    @Test
    public void TestSimpleVectors_testListIndex_d6ea0ead3cd93cc1d462acee3bed2f99() {
        assertEvalError("{ x<-list(1,2,3,4); dim(x)<-c(2,2); x[[list(1,2,3), 1]]<-NULL }");
    }

    @Test
    public void TestSimpleVectors_testListIndex_febbc7cc54b9dd05cf298c934132348e() {
        assertEvalError("{ x<-list(1,2,3,4); dim(x)<-c(2,2); x[[list(1,2,3), 1]]<-integer() }");
    }

    @Test
    public void TestSimpleVectors_testListIndex_01b8d8931d537d293d55d05f1541d1a2() {
        assertEvalError("{ x<-list(1,2,3,4); dim(x)<-c(2,2); x[[list(1,2,3), 1]]<-7 }");
    }

    @Test
    public void TestSimpleVectors_testListIndex_57b48b6e0fe839b99294fc3914dae7a3() {
        assertEvalError("{ x<-list(1,2,3,4); dim(x)<-c(2,2); x[[list(1,2,3), 1]]<-c(7,42) }");
    }

    @Test
    public void TestSimpleVectors_testListIndex_7619ccc2f2374dec7e6f38f163ca4379() {
        assertEvalError("{ z<-1:4; z[list()] }");
    }

    @Test
    public void TestSimpleVectors_testListIndex_468995f820f92bdd80a41e84055e8bbd() {
        assertEvalError("{ z<-1:4; z[list(1)] }");
    }

    @Test
    public void TestSimpleVectors_testListIndex_e9a2501e8a06a3604ae9f73a25d65002() {
        assertEvalError("{ z<-1:4; z[list(1,2)] }");
    }

    @Test
    public void TestSimpleVectors_testListIndex_a4a62179951720e4620361c1574b84a4() {
        assertEvalError("{ x<-1:4; dim(x)<-c(2,2); x[list(), 1] }");
    }

    @Test
    public void TestSimpleVectors_testListIndex_4af73b0e9ea727493a5eb23b9aa666dc() {
        assertEvalError("{ x<-1:4; dim(x)<-c(2,2); x[list(1), 1] }");
    }

    @Test
    public void TestSimpleVectors_testListIndex_dbea30e80931fe5df84ad6281b20b458() {
        assertEvalError("{ x<-1:4; dim(x)<-c(2,2); x[list(1,2), 1] }");
    }

    @Test
    public void TestSimpleVectors_testListIndex_cb06bc90af8f04b74d74e73bd46a3b01() {
        assertEvalError("{ z<-1:4; z[[list()]] }");
    }

    @Test
    public void TestSimpleVectors_testListIndex_38375c7621db26335021b49684553ea4() {
        assertEvalError("{ z<-1:4; z[[list(1)]] }");
    }

    @Test
    public void TestSimpleVectors_testListIndex_10d56d85f2dff3f66644d5cca5e40ca3() {
        assertEvalError("{ z<-1:4; z[[list(1,2)]] }");
    }

    @Test
    public void TestSimpleVectors_testListIndex_2a15e1846757a931b553c9e2afa084a0() {
        assertEvalError("{ x<-1:4; dim(x)<-c(2,2); x[[list(), 1]] }");
    }

    @Test
    public void TestSimpleVectors_testListIndex_eeea14d73825be5efcfd57af5a3ddd56() {
        assertEvalError("{ x<-1:4; dim(x)<-c(2,2); x[[list(1), 1]] }");
    }

    @Test
    public void TestSimpleVectors_testListIndex_117b4af98479d816d153fe37d818dc22() {
        assertEvalError("{ x<-1:4; dim(x)<-c(2,2); x[[list(1,2), 1]] }");
    }

    @Test
    public void TestSimpleVectors_testListIndex_4120c3bdf8109f8c113a30d29e298e59() {
        assertEvalError("{ z<-list(1,2,3,4); z[list()] }");
    }

    @Test
    public void TestSimpleVectors_testListIndex_518f41095d2dbfff54cd44a0f6d1c550() {
        assertEvalError("{ z<-list(1,2,3,4); z[list(1)] }");
    }

    @Test
    public void TestSimpleVectors_testListIndex_f6197d4249c0fa2b3b0df4ea4d7049aa() {
        assertEvalError("{ z<-list(1,2,3,4); z[list(1,2)] }");
    }

    @Test
    public void TestSimpleVectors_testListIndex_852d62386ef7bf8f4201d24fbeeaae97() {
        assertEvalError("{ x<-list(1,2,3,4); dim(x)<-c(2,2); x[list(), 1] }");
    }

    @Test
    public void TestSimpleVectors_testListIndex_4096a6a10355a1ed51ccb6606a68a209() {
        assertEvalError("{ x<-list(1,2,3,4); dim(x)<-c(2,2); x[list(1), 1] }");
    }

    @Test
    public void TestSimpleVectors_testListIndex_db679cc1376157176eb9355a8b3d15cd() {
        assertEvalError("{ x<-list(1,2,3,4); dim(x)<-c(2,2); x[list(1,2), 1] }");
    }

    @Test
    public void TestSimpleVectors_testListIndex_052cc49770dd619bbf133018ba027ea0() {
        assertEvalError("{ z<-list(1,2,3,4); z[[list()]] }");
    }

    @Test
    public void TestSimpleVectors_testListIndex_df918b754fb1832830148b59037ad491() {
        assertEvalError("{ z<-list(1,2,3,4); z[[list(1)]] }");
    }

    @Test
    public void TestSimpleVectors_testListIndex_5a7eeda0b9fad4b4c28da8f31016622d() {
        assertEvalError("{ z<-list(1,2,3,4); z[[list(1,2)]] }");
    }

    @Test
    public void TestSimpleVectors_testListIndex_9437dbd41533b3e33625d50b26cde216() {
        assertEvalError("{ x<-list(1,2,3,4); dim(x)<-c(2,2); x[[list(), 1]] }");
    }

    @Test
    public void TestSimpleVectors_testListIndex_180591b5c3d9454728d407ac6f7c42ae() {
        assertEvalError("{ x<-list(1,2,3,4); dim(x)<-c(2,2); x[[list(1), 1]] }");
    }

    @Test
    public void TestSimpleVectors_testListIndex_975899f5a12f723c3b87c767c491e6d4() {
        assertEvalError("{ x<-list(1,2,3,4); dim(x)<-c(2,2); x[[list(1,2), 1]] }");
    }

    @Test
    public void TestSimpleVectors_testListUpdate_e4cc13b3d845341d0bbe02830ff3054d() {
        assertEval(" { f <- function(b,i,v) { b[[i]] <- v ; b } ;  f(list(1,2,b=list(a=1)),c(\"b\",\"a\"),10) ; f(list(a=1,b=c(a=2)),c(TRUE,TRUE),3) }");
    }

    @Test
    public void TestSimpleVectors_testListUpdate_6cfdfeec466a18abc1ef39eeb59b3890() {
        assertEval("{ x<-c(1,2,3) ; y<-x ; x[2]<-100 ; y }");
    }

    @Test
    public void TestSimpleVectors_testListUpdate_69e5cb49024ff3c4e1865e0b5aa09827() {
        assertEval("{ x <-2L ; y <- x; x[1] <- 211L ; y }");
    }

    @Test
    public void TestSimpleVectors_testListUpdate_9af53ea24a50f5fc0dfbc17eb78d222f() {
        assertEval("{ l <- matrix(list(1,2)) ; l[3] <- NULL ; l }");
    }

    @Test
    public void TestSimpleVectors_testListUpdate_f651c7af8aa82bc05a51faa6b02f67c9() {
        assertEval("{ l <- matrix(list(1,2)) ; l[4] <- NULL ; l }");
    }

    @Test
    public void TestSimpleVectors_testListUpdate_049e1c6a5d2b23ebf9ac92a6b90755dd() {
        assertEval("{ l<-list(1,2L,TRUE) ; l[[2]]<-100 ; l }");
    }

    @Test
    public void TestSimpleVectors_testListUpdate_2ba4f81ad74701a020200cb052072962() {
        assertEval("{ l<-list(1,2L,TRUE) ; l[[5]]<-100 ; l }");
    }

    @Test
    public void TestSimpleVectors_testListUpdate_0bef32accaeca45783f1d9c6a1934106() {
        assertEval("{ l<-list(1,2L,TRUE) ; l[[3]]<-list(100) ; l }");
    }

    @Test
    public void TestSimpleVectors_testListUpdate_c7b1df58bb3dc64812dac2f228d881a2() {
        assertEval("{ v<-1:3 ; v[2] <- list(100) ; v }");
    }

    @Test
    public void TestSimpleVectors_testListUpdate_f0f773700693ec2ee5168ecab33d279a() {
        assertEval("{ v<-1:3 ; v[[2]] <- list(100) ; v }");
    }

    @Test
    public void TestSimpleVectors_testListUpdate_2e2c96717b4fe7735b6b4ee43ca2740e() {
        assertEval("{ l <- list() ; l[[1]] <-2 ; l}");
    }

    @Test
    public void TestSimpleVectors_testListUpdate_f3504202d22ef90762e108fb17310415() {
        assertEval("{ l<-list() ; x <- 1:3 ; l[[1]] <- x  ; l }");
    }

    @Test
    public void TestSimpleVectors_testListUpdate_ab04e59f8bf6164452564b4df0d4403f() {
        assertEval("{ l <- list(1,2,3) ; l[2] <- list(100) ; l[2] }");
    }

    @Test
    public void TestSimpleVectors_testListUpdate_38cd2f6e7d811f6b5f82ceadb42e2b93() {
        assertEval("{ l <- list(1,2,3) ; l[[2]] <- list(100) ; l[2] }");
    }

    @Test
    public void TestSimpleVectors_testListUpdate_3e76797bb9cc275eb87fdfdfe5d629d7() {
        assertEval("{ m<-list(1,2) ; m[TRUE] <- NULL ; m }");
    }

    @Test
    public void TestSimpleVectors_testListUpdate_5fb3879628fe8aa1c87a0ab7e8de00f3() {
        assertEval("{ m<-list(1,2) ; m[[TRUE]] <- NULL ; m }");
    }

    @Test
    public void TestSimpleVectors_testListUpdate_acd8b867416da2dc3d62e6858ff40cfe() {
        assertEval("{ m<-list(1,2) ; m[[1]] <- NULL ; m }");
    }

    @Test
    public void TestSimpleVectors_testListUpdate_3f35cc1ec75d690a220917b2211ea1ab() {
        assertEval("{ m<-list(1,2) ; m[[-1]] <- NULL ; m }");
    }

    @Test
    public void TestSimpleVectors_testListUpdate_e701b971bdf0c357bfaa570a581a18f3() {
        assertEval("{ m<-list(1,2) ; m[[-2]] <- NULL ; m }");
    }

    @Test
    public void TestSimpleVectors_testListUpdate_29a43d2d262ecff91ad9d6c0c22e1e5d() {
        assertEval("{ l <- list(a=1,b=2,c=3) ; l[1] <- NULL ; l }");
    }

    @Test
    public void TestSimpleVectors_testListUpdate_1dfc27c06619fa9e95132334bd1002f9() {
        assertEval("{ l <- list(a=1,b=2,c=3) ; l[3] <- NULL ; l }");
    }

    @Test
    public void TestSimpleVectors_testListUpdate_2da0dfbc881010d9330ad379fa41589f() {
        assertEval("{ l <- list(a=1,b=2,c=3) ; l[5] <- NULL ; l}");
    }

    @Test
    public void TestSimpleVectors_testListUpdate_fa8ec30358c8fe3e6aba336797890ea7() {
        assertEval("{ l <- list(a=1,b=2,c=3) ; l[4] <- NULL ; l}");
    }

    @Test
    public void TestSimpleVectors_testListUpdate_9d865157f881c2e5e9614fe0dab33070() {
        assertEval("{ l <- list(a=1,b=2,c=3) ; l[[5]] <- NULL ; l}");
    }

    @Test
    public void TestSimpleVectors_testListUpdate_417ed4eedca1ebfc460de6952cb8a02e() {
        assertEval("{ l <- list(a=1,b=2,c=3) ; l[[4]] <- NULL ; l}");
    }

    @Test
    public void TestSimpleVectors_testListUpdate_ebb58f885bceb97268ae070e7564311e() {
        assertEval("{ l <- list(1,2); l[0] <- NULL; l}");
    }

    @Test
    public void TestSimpleVectors_testListUpdate_ab03f072c05c1f8c291eb9fe76e9ec98() {
        assertEval("{ l <- list(1,2,3) ; l[c(2,3)] <- c(20,30) ; l }");
    }

    @Test
    public void TestSimpleVectors_testListUpdate_247ef3f2a76dab27a26505a21c6a9f67() {
        assertEval("{ l <- list(1,2,3) ; l[c(2:3)] <- c(20,30) ; l }");
    }

    @Test
    public void TestSimpleVectors_testListUpdate_7807e8ecc86a34567e8dc7385693bb4e() {
        assertEval("{ l <- list(1,2,3) ; l[-1] <- c(20,30) ; l }");
    }

    @Test
    public void TestSimpleVectors_testListUpdate_7019c7ad5dc70ebf1468c32821e0d9ab() {
        assertEval("{ l <- list(1,2,3) ; l[-1L] <- c(20,30) ; l }");
    }

    @Test
    public void TestSimpleVectors_testListUpdate_d2b9b5ddbcde8a2533abb711ef562ebd() {
        assertEval("{ l <- list(1,2,3) ; l[c(FALSE,TRUE,TRUE)] <- c(20,30) ; l }");
    }

    @Test
    public void TestSimpleVectors_testListUpdate_9d469add0487923fb41250af3826b347() {
        assertEval("{ l <- list() ; l[c(TRUE,TRUE)] <-2 ; l }");
    }

    @Test
    public void TestSimpleVectors_testListUpdate_750d6189787169e361ae35560e0c0327() {
        assertEval("{ x <- 1:3 ; l <- list(1) ; l[[TRUE]] <- x ; l[[1]] } ");
    }

    @Test
    public void TestSimpleVectors_testListUpdate_21abb39393289aed2fd1d7146f916ec7() {
        assertEval("{ x<-list(1,2,3,4,5); x[3:4]<-c(300L,400L); x }");
    }

    @Test
    public void TestSimpleVectors_testListUpdate_2fe869eb0999f96cc326202e725636c6() {
        assertEval("{ x<-list(1,2,3,4,5); x[4:3]<-c(300L,400L); x }");
    }

    @Test
    public void TestSimpleVectors_testListUpdate_7e7d53d2b674c8db4b1ae742f4336eb8() {
        assertEval("{ x<-list(1,2L,TRUE,TRUE,FALSE); x[c(-2,-3,-3,-100,0)]<-256; x }");
    }

    @Test
    public void TestSimpleVectors_testListUpdate_bfcd30bc0ad198478ba4a3aacc3abc50() {
        assertEval("{ x<-list(1,2L,list(3,list(4)),list(5)) ; x[c(4,2,3)]<-list(256L,257L,258L); x }");
    }

    @Test
    public void TestSimpleVectors_testListUpdate_2c1efbaa2d407059a4b07561fa137e27() {
        assertEval("{ x<-list(FALSE,NULL,3L,4L,5.5); x[c(TRUE,FALSE)] <- 1000; x }");
    }

    @Test
    public void TestSimpleVectors_testListUpdate_b0aeed0aaee6e2741579449b582ac68a() {
        assertEval("{ x<-list(11,10,9) ; x[c(TRUE, FALSE, TRUE)] <- c(1000,2000); x }");
    }

    @Test
    public void TestSimpleVectors_testListUpdate_bae116ddbf2ce289006665c06b6d928e() {
        assertEval("{ l <- list(1,2,3) ; x <- list(100) ; y <- x; l[1:1] <- x ; l[[1]] }");
    }

    @Test
    public void TestSimpleVectors_testListUpdate_d72fc9e57496b3b629857408183195d3() {
        assertEval("{ l <- list(1,2,3) ; x <- list(100) ; y <- x; l[[1:1]] <- x ; l[[1]] }");
    }

    @Test
    public void TestSimpleVectors_testListUpdate_f451130c194bdfe116d208d878ee9122() {
        assertEval("{ v<-list(1,2,3) ; v[c(2,3,NA,7,0)] <- NULL ; v }");
    }

    @Test
    public void TestSimpleVectors_testListUpdate_a2fd3c6abfeec863743dd99823e1844e() {
        assertEval("{ v<-list(1,2,3) ; v[c(2,3,4)] <- NULL ; v }");
    }

    @Test
    public void TestSimpleVectors_testListUpdate_c28ffe769650b652389b669bd9f0209e() {
        assertEval("{ v<-list(1,2,3) ; v[c(-1,-2,-6)] <- NULL ; v }");
    }

    @Test
    public void TestSimpleVectors_testListUpdate_248ed5f84bf3b769ac858dd79c96f55b() {
        assertEval("{ v<-list(1,2,3) ; v[c(TRUE,FALSE,TRUE)] <- NULL ; v }");
    }

    @Test
    public void TestSimpleVectors_testListUpdate_0070bdd6671ea7c1b6726a9bb991a567() {
        assertEval("{ v<-list(1,2,3) ; v[c()] <- NULL ; v }");
    }

    @Test
    public void TestSimpleVectors_testListUpdate_2d00753aaaa85580ffbc76afc074f267() {
        assertEval("{ v<-list(1,2,3) ; v[integer()] <- NULL ; v }");
    }

    @Test
    public void TestSimpleVectors_testListUpdate_564c4e02950c33852d1c458d7dba257f() {
        assertEval("{ v<-list(1,2,3) ; v[double()] <- NULL ; v }");
    }

    @Test
    public void TestSimpleVectors_testListUpdate_6ad374dd0da2e0e8c6a15c2a8fe1aeaf() {
        assertEval("{ v<-list(1,2,3) ; v[logical()] <- NULL ; v }");
    }

    @Test
    public void TestSimpleVectors_testListUpdate_0a992a6f686043a23612d0cad7b2f4fe() {
        assertEval("{ v<-list(1,2,3) ; v[c(TRUE,FALSE)] <- NULL ; v }");
    }

    @Test
    public void TestSimpleVectors_testListUpdate_be55228ae4359663671c14daf453e65b() {
        assertEval("{ v<-list(1,2,3) ; v[c(TRUE,FALSE,FALSE,FALSE,FALSE,TRUE)] <- NULL ; v }");
    }

    @Test
    public void TestSimpleVectors_testListUpdate_f6a401e5d08cdb3baadb4b7671f23ea8() {
        assertEval("{ l<-list(a=1,b=2,c=3,d=4); l[c(-1,-3)] <- NULL ; l}");
    }

    @Test
    public void TestSimpleVectors_testListUpdate_d5d91db31eb54707478850e4834df484() {
        assertEval("{ l<-list(a=1,b=2,c=3,d=4); l[c(-1,-10)] <- NULL ; l}");
    }

    @Test
    public void TestSimpleVectors_testListUpdate_883638aa57a8ea03fd0532282765aa32() {
        assertEval("{ l<-list(a=1,b=2,c=3,d=4); l[c(2,3)] <- NULL ; l}");
    }

    @Test
    public void TestSimpleVectors_testListUpdate_b3ea05b8eef0db5165d29658023d0c52() {
        assertEval("{ l<-list(a=1,b=2,c=3,d=4); l[c(2,3,5)] <- NULL ; l}");
    }

    @Test
    public void TestSimpleVectors_testListUpdate_cfe6a90c97052d7bce56b5d13c77e2a8() {
        assertEval("{ l<-list(a=1,b=2,c=3,d=4); l[c(2,3,6)] <- NULL ; l}");
    }

    @Test
    public void TestSimpleVectors_testListUpdate_68d48f797198406b6fb3cafb886ca21c() {
        assertEval("{ l<-list(a=1,b=2,c=3,d=4); l[c(TRUE,TRUE,FALSE,TRUE)] <- NULL ; l}");
    }

    @Test
    public void TestSimpleVectors_testListUpdate_3fb3e75e72acddde0fa4ab047a784692() {
        assertEval("{ l<-list(a=1,b=2,c=3,d=4); l[c(TRUE,FALSE)] <- NULL ; l}");
    }

    @Test
    public void TestSimpleVectors_testListUpdate_5fe9642b771fea0bfd44e30f7e27a400() {
        assertEval("{ l<-list(a=1,b=2,c=3,d=4); l[c(TRUE,FALSE,FALSE,TRUE,FALSE,NA,TRUE,TRUE)] <- NULL ; l}");
    }

    @Test
    public void TestSimpleVectors_testListUpdate_fbdfc169c90e95ee6079a8e3b4165933() {
        assertEval("{ l <- list(a=1,b=2,c=3) ; l[[\"b\"]] <- NULL ; l }");
    }

    @Test
    public void TestSimpleVectors_testListUpdate_f25647486f0eb83581434a82445b053a() {
        assertEval("{ l <- list(1,list(2,c(3))) ; l[[c(2,2)]] <- NULL ; l }");
    }

    @Test
    public void TestSimpleVectors_testListUpdate_7f536d0f039ede0f23f33e1e7762c55b() {
        assertEval("{ l <- list(1,list(2,c(3))) ; l[[c(2,2)]] <- 4 ; l }");
    }

    @Test
    public void TestSimpleVectors_testListUpdate_69d9b9f43fe1e0b54916a9cdbbab8847() {
        assertEval("{ l <- list(1,list(2,list(3))) ; l[[1]] <- NULL ; l }");
    }

    @Test
    public void TestSimpleVectors_testListUpdate_5328d841dfeeb211c1f1e83cc0c42d83() {
        assertEval("{ l <- list(1,list(2,list(3))) ; l[[1]] <- 5 ; l }");
    }

    @Test
    public void TestSimpleVectors_testListUpdate_29f7a4bc377ec163981f54784bba532f() {
        assertEval("{ l<-list(a=1,b=2,list(c=3,d=4,list(e=5:6,f=100))) ; l[[c(3,3,1)]] <- NULL ; l }");
    }

    @Test
    public void TestSimpleVectors_testListUpdate_848be28a30435fce4756c5b62bf78e16() {
        assertEval("{ l<-list(a=1,b=2,c=list(d=1,e=2,f=c(x=1,y=2,z=3))) ; l[[c(\"c\",\"f\",\"zz\")]] <- 100 ; l }");
    }

    @Test
    public void TestSimpleVectors_testListUpdate_20e266b984045b07d64928d07561bfd4() {
        assertEval("{ l<-list(a=1,b=2,c=list(d=1,e=2,f=c(x=1,y=2,z=3))) ; l[[c(\"c\",\"f\",\"z\")]] <- 100 ; l }");
    }

    @Test
    public void TestSimpleVectors_testListUpdate_995668d5dd8a67a9a972f24731eef976() {
        assertEval("{ l<-list(a=1,b=2,c=list(d=1,e=2,f=c(x=1,y=2,z=3))) ; l[[c(\"c\",\"f\")]] <- NULL ; l }");
    }

    @Test
    public void TestSimpleVectors_testListUpdate_d670c6f8714ce5fcacb94ae57562db87() {
        assertEval("{ l<-list(a=1,b=2,c=3) ; l[c(\"a\",\"a\",\"a\",\"c\")] <- NULL ; l }");
    }

    @Test
    public void TestSimpleVectors_testListUpdate_a589b01ba7a4063ae97660096b1ee75f() {
        assertEval("{ l<-list(a=1L,b=2L,c=list(d=1L,e=2L,f=c(x=1L,y=2L,z=3L))) ; l[[c(\"c\",\"f\",\"zz\")]] <- 100L ; l }");
    }

    @Test
    public void TestSimpleVectors_testListUpdate_7763f920c8c2a47663328e24ec8d6ef3() {
        assertEval("{ l<-list(a=TRUE,b=FALSE,c=list(d=TRUE,e=FALSE,f=c(x=TRUE,y=FALSE,z=TRUE))) ; l[[c(\"c\",\"f\",\"zz\")]] <- TRUE ; l }");
    }

    @Test
    public void TestSimpleVectors_testListUpdate_4b1b6ac81cff5bed3cb070cf3523e1ad() {
        assertEval("{ l<-list(a=\"a\",b=\"b\",c=list(d=\"cd\",e=\"ce\",f=c(x=\"cfx\",y=\"cfy\",z=\"cfz\"))) ; l[[c(\"c\",\"f\",\"zz\")]] <- \"cfzz\" ; l }");
    }

    @Test
    public void TestSimpleVectors_testListUpdate_ef789f75ea9b0ed5b35b15360f6e6845() {
        assertEval("{ l<-list(a=1,b=2,c=list(d=1,e=2,f=c(x=1,y=2,z=3))) ; l[[c(\"c\",\"f\",\"zz\")]] <- list(100) ; l }");
    }

    @Test
    public void TestSimpleVectors_testListUpdate_065977b548c71b24999b617291e5fddc() {
        assertEval("{ l<-list(a=1L,b=2L,c=list(d=1L,e=2L,f=c(x=1L,y=2L,z=3L))) ; l[[c(\"c\",\"f\")]] <- 100L ; l }");
    }

    @Test
    public void TestSimpleVectors_testListUpdate_9db6af8d8cb76682b5c762fb7d11243b() {
        assertEval("{ l<-list(a=1L,b=2L,c=list(d=1L,e=2L,f=c(x=1L,y=2L,z=3L))) ; l[[c(\"c\",\"f\")]] <- list(haha=\"gaga\") ; l }");
    }

    @Test
    public void TestSimpleVectors_testListUpdate_29583140ec90135bb63f82ca6ff7f50a() {
        assertEval("{ l<-list() ; x <- 1:3 ; l[[1]] <- x; x[2] <- 100L; l[[1]] }");
    }

    @Test
    public void TestSimpleVectors_testListUpdate_8b4beca3b47cd90ad3878f989f90c27f() {
        assertEval("{ l <- list(1, list(2)) ;  m <- l ; l[[c(2,1)]] <- 3 ; m[[2]][[1]] }");
    }

    @Test
    public void TestSimpleVectors_testListUpdate_717a2af5c5e6ba6eff57dde5e202dd47() {
        assertEval("{ l <- list(1, list(2,3,4)) ;  m <- l ; l[[c(2,1)]] <- 3 ; m[[2]][[1]] }");
    }

    @Test
    public void TestSimpleVectors_testListUpdate_72633999c42a077e4d11682c86a41e05() {
        assertEval("{ x <- c(1L,2L,3L) ; l <- list(1) ; l[[1]] <- x ; x[2] <- 100L ; l[[1]] }");
    }

    @Test
    public void TestSimpleVectors_testListUpdate_7b00a78326db1d40bf645ed57d2305a6() {
        assertEval("{ l <- list(100) ; f <- function() { l[[1]] <- 2 } ; f() ; l }");
    }

    @Test
    public void TestSimpleVectors_testListUpdate_7f36ff7d9774f5db47a11b004f95f31a() {
        assertEval("{ l <- list(100,200,300,400,500) ; f <- function() { l[[3]] <- 2 } ; f() ; l }");
    }

    @Test
    public void TestSimpleVectors_testListUpdate_d21089ac73615bdcbe12c669a94c6c45() {
        assertEval("{ f <- function() { l[1:2] <- x ; x[1] <- 211L  ; l[1] } ; l <- 1:3 ; x <- 10L ; f() }");
    }

    @Test
    public void TestSimpleVectors_testListUpdate_a509d62c5b1008a9154837389c9af6aa() {
        assertEval("{ x <- list(1,list(2,3),4) ; x[[c(2,3)]] <- 3 ; x }");
    }

    @Test
    public void TestSimpleVectors_testListUpdate_e6ded06b4f4788520dc88118b27cfa89() {
        assertEval("{ x <- list(1,list(2,3),4) ; z <- x[[2]] ; x[[c(2,3)]] <- 3 ; z }");
    }

    @Test
    public void TestSimpleVectors_testListUpdate_fa65794f16acb025e1a3662f876ea6a9() {
        assertEval("{ f <- function(b,i,v) { b[[i]] <- v ; b } ; f(list(1,2,list(3)), c(3,1), 4) ; f(list(1,2,3), 2L, 3) }");
    }

    @Test
    public void TestSimpleVectors_testListUpdate_70d9a3d4707057a593a7b7fbf64efb07() {
        assertEval("{ f <- function(b,i,v) { b[[i]] <- v ; b } ; f(list(1,2,list(3)), c(3,1), 4) ; f(list(1,2,3), 2L, NULL) }");
    }

    @Test
    public void TestSimpleVectors_testListUpdate_ba2b0194283c852fe6eba3a4d4c4124a() {
        assertEval("{ f <- function(b,i,v) { b[[i]] <- v ; b } ; f(list(1,2,list(3)), c(3,1), 4) ; f(c(1,2,3), \"hello\", 2) }");
    }

    @Test
    public void TestSimpleVectors_testListUpdate_8804523a64abfe471cb05900a0b642d5() {
        assertEval("{ f <- function(b,i,v) { b[[i]] <- v ; b } ; f(list(1,2,b=list(x=3)),c(\"b\",\"x\"),10) }");
    }

    @Test
    public void TestSimpleVectors_testListUpdate_a8a63ac6c2a9750cc77a0b24a66636b6() {
        assertEval("{ f <- function(b,i,v) { b[[i]] <- v ; b } ; f(list(1,2,b=c(x=3)),c(\"b\",\"x\"),10) }");
    }

    @Test
    public void TestSimpleVectors_testListUpdate_d5a16d4b481e67db350fc3344cb401b2() {
        assertEval("{ f <- function(b,i,v) { b[[i]] <- v ; b } ; f(c(1,2,b=c(x=3)),c(\"b\"),10) }");
    }

    @Test
    public void TestSimpleVectors_testListUpdate_036b36ec3bbc27080a33bedad3745355() {
        assertEval("{ f <- function(b,i,v) { b[[i]] <- v ; b } ; f(list(1,2,b=list(a=list(x=1,y=2),3),4),c(\"b\",\"a\",\"x\"),10) }");
    }

    @Test
    public void TestSimpleVectors_testListUpdate_d4d23ecf1c341646080e677a3605d193() {
        assertEval("{ f <- function(b,i,v) { b[[i]] <- v ; b } ;  f(list(1,2,b=list(a=1)),c(\"b\",\"a\"),10) ; f(list(a=1,b=2),\"b\",NULL) }");
    }

    @Test
    public void TestSimpleVectors_testListUpdate_9f9899b299cc13f4545955a7432a28cb() {
        assertEval("{ f <- function(b,i,v) { b[[i]] <- v ; b } ;  f(list(1,2,b=list(a=1)),c(\"b\",\"a\"),10) ; f(list(a=1,b=list(2)),\"b\",double()) }");
    }

    @Test
    public void TestSimpleVectors_testListUpdate_ce127c26577ca03a4e65e6dda2526e73() {
        assertEval("{ l <- list(a=1,b=2,cd=list(c=3,d=4)) ; x <- list(l,xy=list(x=l,y=l)) ; x[[c(2,2,3,2)]] <- 10 ; l }");
    }

    @Test
    public void TestSimpleVectors_testListUpdate_304e0c334c8d62c7a75d96c112a98bef() {
        assertEval("{ l <- list(a=1,b=2,cd=list(c=3,d=4)) ; x <- list(l,xy=list(x=l,y=l)) ; x[[c(\"xy\",\"y\",\"cd\",\"d\")]] <- 10 ; l }");
    }

    @Test
    public void TestSimpleVectors_testListUpdate_c3be18201db91f403f7708060107126a() {
        assertEval("{ l <- matrix(list(1,2)) ; l[[3]] <- NULL ; l }");
    }

    @Test
    public void TestSimpleVectors_testListUpdate_0b04b217ba7d7f72bfb22fcad66db620() {
        assertEval("{ l <- matrix(list(1,2)) ; l[[4]] <- NULL ; l }");
    }

    @Test
    public void TestSimpleVectors_testListUpdate_ad4f324c267f9fd378c1ebb5db6be131() {
        assertEvalError("{ f <- function(b,i,v) { b[[i]] <- v ; b } ; f(list(1,2,b=list(x=3)),character(),10) }");
    }

    @Test
    public void TestSimpleVectors_testListUpdate_8d60c82c47cee679eaec904ac070b844() {
        assertEvalError("{ l <- list(1,2); l[[0]] }");
    }

    @Test
    public void TestSimpleVectors_testListUpdate_f3069de7f4e2a93ed52633d59eb9a0c0() {
        assertEvalError("{ l <- list(list(1,2),2) ; l[[c(1,1,2,3,4,3)]] <- 10 ; l }");
    }

    @Test
    public void TestSimpleVectors_testListUpdate_e632c1b958f6dfc38b8945d2ac0fefec() {
        assertEvalError("{ l <- list(1,2) ; l[[c(1,1,2,3,4,3)]] <- 10 ; l }");
    }

    @Test
    public void TestSimpleVectors_testListUpdate_58816dd55e5cfa866c3d22cb27e753e6() {
        assertEvalError("{ l <- as.list(1:3) ; l[[0]] <- 2 }");
    }

    @Test
    public void TestSimpleVectors_testListUpdate_21ae327fc9760aa93f399aa2b8a1a064() {
        assertEvalError("{ x <- as.list(1:3) ; x[[integer()]] <- 3 }");
    }

    @Test
    public void TestSimpleVectors_testListUpdate_a65c7eee2c68039154deb31fada0ae4d() {
        assertEvalError("{ f <- function(b,i,v) { b[[i]] <- v ; b } ; f(list(1,2,list(3)), c(3,1), 4) ; f(list(f,f), c(1,1), 3) }");
    }

    @Test
    public void TestSimpleVectors_testListUpdate_942201d73db7111f1ae9c68deaed4964() {
        assertEvalError("{ f <- function(b,i,v) { b[[i]] <- v ; b } ; f(list(1,2,list(3)), c(3,1), 4) ; f(c(1,2,3), 2L, NULL) }");
    }

    @Test
    public void TestSimpleVectors_testListUpdate_3483bab6aa975bf40077db52d4c99fb9() {
        assertEvalError("{ f <- function(b,i,v) { b[[i]] <- v ; b } ; f(list(1,2,list(3)), c(3,1), 4) ; f(c(1,2,3), 2L, 1:2) }");
    }

    @Test
    public void TestSimpleVectors_testListUpdate_46d267d73b5b19292f799c55c5c8b655() {
        assertEvalError("{ f <- function(b,i,v) { b[[i]] <- v ; b } ; f(list(1,2,list(3)), c(3,1), 4) ; f(c(1,2,3), f, 2) }");
    }

    @Test
    public void TestSimpleVectors_testListUpdate_5038483431f78dfe82dd044060a16939() {
        assertEvalError("{ f <- function(b,i,v) { b[[i]] <- v ; b } ; f(c(1,2,b=c(x=3)),c(\"b\",\"x\"),10) }");
    }

    @Test
    public void TestSimpleVectors_testListUpdate_69e9cbe8a47a5eb9ba1011da9572c3fc() {
        assertEvalError("{ f <- function(b,i,v) { b[[i]] <- v ; b } ; f(list(1,2, list(3)),c(\"b\",\"x\"),10) }");
    }

    @Test
    public void TestSimpleVectors_testListUpdate_0366c87bfe791402269374ef9e91eda5() {
        assertEvalError("{ f <- function(b,i,v) { b[[i]] <- v ; b } ; f(list(1,2,b=list(3)),c(\"a\",\"x\"),10) }");
    }

    @Test
    public void TestSimpleVectors_testListUpdate_9a74a47b76d187307b5920f6f5cfa904() {
        assertEvalError("{ f <- function(b,i,v) { b[[i]] <- v ; b } ;  f(list(1,2,b=list(a=1)),c(\"b\",\"a\"),10) ; f(list(a=1,b=f),c(\"b\",\"x\"),3) }");
    }

    @Test
    public void TestSimpleVectors_testListUpdate_b0c34f83e997bfdf47a82b1531934031() {
        assertEvalError("{ f <- function(b,i,v) { b[[i]] <- v ; b } ;  f(list(1,2,b=list(a=1)),c(\"b\",\"a\"),10) ; f(c(a=1,b=2),\"b\",NULL) }");
    }

    @Test
    public void TestSimpleVectors_testListUpdate_c8d3f68defd80945f76c00b35a0084c3() {
        assertEvalError("{ f <- function(b,i,v) { b[[i]] <- v ; b } ;  f(list(1,2,b=list(a=1)),c(\"b\",\"a\"),10) ; f(list(a=1,b=c(a=2)),c(\"b\",\"a\"),1:3) }");
    }

    @Test
    public void TestSimpleVectors_testListUpdate_3d615bb9b102533bc5d3c80d111a2207() {
        assertEvalError("{ f <- function(b,i,v) { b[[i]] <- v ; b } ;  f(list(1,2,b=list(a=1)),c(\"b\",\"a\"),10) ; f(list(a=1,b=c(a=2)),1+2i,1:3) }");
    }

    @Test
    public void TestSimpleVectors_testListUpdate_3dd5886f537002149bc7703a3e52f68f() {
        assertEvalError("{ f <- function(b,i,v) { b[[i]] <- v ; b } ;  f(list(1,2,b=list(a=1)),c(\"b\",\"a\"),10) ; f(f,TRUE,3) }");
    }

    @Test
    public void TestSimpleVectors_testListUpdate_d8dc2d36066fedc78a8e52042a31c205() {
        assertEvalError("{ f <- function(b,i,v) { b[[i]] <- v ; b } ;  f(list(1,2,b=list(a=1)),c(\"b\",\"a\"),10) ; f(c(a=1,b=2),\"b\",as.raw(12)) }");
    }

    @Test
    public void TestSimpleVectors_testListUpdate_2b3c661f87fd977f839d5864781c66d0() {
        assertEvalError("{ f <- function(b,i,v) { b[[i]] <- v ; b } ;  f(list(1,2,b=list(a=1)),c(\"b\",\"a\"),10) ; f(c(a=1,b=2),c(1+2i,3+4i),as.raw(12)) }");
    }

    @Test
    public void TestSimpleVectors_testMatrixIndex_159dc4e42d0e70ec782df4eaf5c147f6() {
        assertEval("{ m <- matrix(1:6, nrow=2) ; m[1,2] }");
    }

    @Test
    public void TestSimpleVectors_testMatrixIndex_96a64263f53176fd81256212c3f59f68() {
        assertEval("{ m <- matrix(1:6, nrow=2) ; m[1,] }");
    }

    @Test
    public void TestSimpleVectors_testMatrixIndex_8e4e1f45ab171ee18506015ebfad7055() {
        assertEval("{ m <- matrix(1:6, nrow=2) ; m[,1] }");
    }

    @Test
    public void TestSimpleVectors_testMatrixIndex_470d31a477bc58d722149f6df9636298() {
        assertEval("{ m <- matrix(1:6, nrow=3) ; f <- function(i,j) { m[i,j] } ; f(1,c(1,2)) }");
    }

    @Test
    public void TestSimpleVectors_testMatrixIndex_5d4eee20540197cba2587dc184ff8343() {
        assertEval("{ m <- matrix(1:6, nrow=2) ; f <- function(i,j) { m[i,j] } ; f(1,1) }");
    }

    @Test
    public void TestSimpleVectors_testMatrixIndex_1a04362748f5618213615f5d3e662ec2() {
        assertEval("{ m <- matrix(1:6, nrow=3) ; f <- function(i,j) { m[i,j] } ; f(1,c(-1,0,-1,-10)) }");
    }

    @Test
    public void TestSimpleVectors_testMatrixIndex_b5e03849227fcc357dc809a521111e19() {
        assertEval("{ m <- matrix(1:6, nrow=2) ; x<-2 ; m[[1,x]] }");
    }

    @Test
    public void TestSimpleVectors_testMatrixIndex_161c14887933887986cd6a867d939312() {
        assertEval("{ m <- matrix(1:6, nrow=2) ; m[[1,2]] }");
    }

    @Test
    public void TestSimpleVectors_testMatrixIndex_dddc9a38ee5fd88c062cf63a1692c162() {
        assertEval("{ m <- matrix(1:6, nrow=2) ; m[1:2,2:3] }");
    }

    @Test
    public void TestSimpleVectors_testMatrixIndex_39da798df6610ce77c1d3ec2b90f8492() {
        assertEval("{ m <- matrix(1:6, nrow=2) ; m[1:2,-1] }");
    }

    @Test
    public void TestSimpleVectors_testMatrixIndex_557cdc287421eba8181ab08c798dcc25() {
        assertEval("{ m <- matrix(1:6, nrow=2) ; m[,c(-1,0,0,-1)] }");
    }

    @Test
    public void TestSimpleVectors_testMatrixIndex_9529f83d6bcc992c61b29fcb2c2e4179() {
        assertEval("{ m <- matrix(1:6, nrow=2) ; m[,c(1,NA,1,NA)] }");
    }

    @Test
    public void TestSimpleVectors_testMatrixIndex_4f1f4879a924745096888234d96426c7() {
        assertEval("{ m <- matrix(1:6, nrow=2) ; m[,c(NA,1,0)] }");
    }

    @Test
    public void TestSimpleVectors_testMatrixIndex_66278cd0804d9e330cfac5d2f26c3cc0() {
        assertEval("{ m <- matrix(1:6, nrow=3) ; f <- function(i,j) { m[i,j] } ; f(c(TRUE),c(FALSE,TRUE)) }");
    }

    @Test
    public void TestSimpleVectors_testMatrixIndex_d441f95e86126fc14455080e15b4caa7() {
        assertEval("{ m <- matrix(1:6, nrow=2) ; f <- function(i,j) { m[i,j] } ; f(1,1:3) }");
    }

    @Test
    public void TestSimpleVectors_testMatrixIndex_33c43f2c126872dd7288128b23496109() {
        assertEval("{ m <- matrix(1:6, nrow=2) ; m[1:2,0:1] }");
    }

    @Test
    public void TestSimpleVectors_testMatrixIndex_061aa796abc51ab677aa26edfa505d3e() {
        assertEval("{ m <- matrix(1:6, nrow=2) ; m[1:2,0:1] ; m[1:2,1:1] }");
    }

    @Test
    public void TestSimpleVectors_testMatrixIndex_a7c369d05a1e240ff0d87267d6ecd712() {
        assertEval("{ m <- matrix(1:6, nrow=2) ; m[,] }");
    }

    @Test
    public void TestSimpleVectors_testMatrixIndex_5b59b84f9c147a0a912e1f5686e3e6ba() {
        assertEval("{ m <- matrix(1:6, nrow=2) ; m[,-1] }");
    }

    @Test
    public void TestSimpleVectors_testMatrixIndex_5a71bcac492a57a25e26b81bbb3ad94b() {
        assertEval("{ m <- matrix(1:6, nrow=2) ; m[1,,drop=FALSE] }");
    }

    @Test
    public void TestSimpleVectors_testMatrixIndex_38b22553c0c24a00f3c4bad9eae849bd() {
        assertEval("{ m <- matrix(1:6, nrow=2) ; m[,1[2],drop=FALSE] }");
    }

    @Test
    public void TestSimpleVectors_testMatrixIndex_4373d3f5d55838252424c60f13dda0ca() {
        assertEval("{ m <- matrix(1:16, nrow=8) ; m[c(TRUE,FALSE,FALSE),c(FALSE,NA), drop=FALSE]}");
    }

    @Test
    public void TestSimpleVectors_testMatrixIndex_ffc5261d4800d15145553df574ee4dbc() {
        assertEval("{ m <- matrix(1:16, nrow=8) ; m[c(TRUE,FALSE),c(FALSE,TRUE), drop=TRUE]}");
    }

    @Test
    public void TestSimpleVectors_testMatrixIndex_bee953bbf9ed7ccb9274ad6c43b8ad50() {
        assertEval("{ m <- matrix(1:16, nrow=8) ; m[c(TRUE,FALSE,FALSE),c(FALSE,TRUE), drop=TRUE]}");
    }

    @Test
    public void TestSimpleVectors_testMatrixIndex_11e269d5bd5b39a80cbdec0fffb111dd() {
        assertEval("{ m <- matrix(1:4, nrow=2) ; m[[2,1,drop=FALSE]] }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectors_560325de4dcc9cbe5871d5282250635b() {
        assertEval("{ x<-NULL; x[1L] }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectors_31f74d7bf828e5ad2abc2dfb151f5ded() {
        assertEval("{ x<-NULL; x[2L] }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectors_7d443bafdcaede0017b46a9668cae8b9() {
        assertEval("{ x<-NULL; x[3L] }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectors_2572360550bcbde36eb5a4c4676bba40() {
        assertEval("{ x<-1.1:3.1; x[1L] }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectors_04e188323e6391e15f534953a76157c7() {
        assertEval("{ x<-1.1:3.1; x[2L] }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectors_e54c41553738cb8ef9a9e59de67e625c() {
        assertEval("{ x<-1.1:3.1; x[3L] }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectors_2f8da7085a857f4a0d35c61ef4f642c0() {
        assertEval("{ x<-3.1:1; x[1L] }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectors_81d208fc210f87ff3417873c997156d1() {
        assertEval("{ x<-3.1:1; x[2L] }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectors_ac44cc6bb4bb06c219c96380f11f10f6() {
        assertEval("{ x<-3.1:1; x[3L] }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_93ba9dd747c7379e9e2c84ec7231640e() {
        assertEval("{ x<-c(TRUE,TRUE,FALSE); x[1L] }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_044b9e85a74f8578c3a768dca065355e() {
        assertEval("{ x<-c(TRUE,TRUE,FALSE); x[2L] }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_8e3f96d4a82935b012bfdb246addabf9() {
        assertEval("{ x<-c(TRUE,TRUE,FALSE); x[3L] }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_c48e6329ed532b7234c8ab581a577f0e() {
        assertEval("{ x<-c(1,2); x[c(\"a\")] }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_e3df6e4c26c764d90548afb7cdbcf7b7() {
        assertEval("{ x<-c(1,2); dim(x)<-c(1,2); x[c(\"a\")] }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_6ff2a31a140c7a0889dc23eeb2d56374() {
        assertEval("{ x<-c(1,2); x[c(\"a\", \"b\")] }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_bff8151fb7d577bb95bfb938a2b8373f() {
        assertEval("{ x<-1:2; x[c(TRUE, TRUE)] }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_e10fbecc1be59e4cd657ede639c7276b() {
        assertEval("{ x<-2; x[NULL] }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_d7af0282e04e5d12217d962be3adf901() {
        assertEval("{ x<-1; x[] }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_1e14968a7fffebe47f5d4396f6de4949() {
        assertEval("{ x<-1:2; dim(x)=c(1,2); x[,] }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_b739daf21732bf5db826f96032d57134() {
        assertEval("{ x<-1:2; dim(x)=c(1,2); x[,1] }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_1751d9cb480d29a8b87c6f1f44bc5194() {
        assertEval("{ x<-c(a=1, b=2); x[c(\"z\", \"x\")] }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_5b3510facf519429db532983c68b755b() {
        assertEval("{ x<-1:4; dim(x)<-c(2,2); x[0,0]<-integer(); x }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_b6d10d849eaa43304b1bb758053cfab9() {
        assertEval("{ x<-1:4; dim(x)<-c(2,2); x[-1,0]<-integer(); x }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_9ba59633f2d06b04c5a0f6ada35c29a8() {
        assertEval("{ x<-1:4; dim(x)<-c(2,2); x[0,-1]<-integer(); x }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_2fbc15e8c5a8f6128faf1456c09093ab() {
        assertEval("{ x<-1:4; dim(x)<-c(2,2); x[-5,0]<-integer(); x }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_281f6f5ad2104c73b3d1f549807aaa26() {
        assertEval("{ x<-1:4; dim(x)<-c(2,2); x[0,-5]<-integer(); x }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_027dfea87b9027c9264e1ef4e5df1177() {
        assertEval("{ x<-1:4; dim(x)<-c(2,2); x[-1] }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_8d129b13dbd02cec4c9ff2a773f68ea2() {
        assertEval("{ x<-list(1); x[[c(1, 1)]] }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_895c174db876e845a32c9b5b395911cf() {
        assertEval("{ x<-list(list(1,42)); x[[c(1, 2)]] }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_194a0110ab2e99fdc8313a47dfea9af1() {
        assertEval("{ x<-list(list(1,list(42))); x[[c(1, 2)]] }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_02f1c2992d279c06774eb0d904d8c6ed() {
        assertEval("{ x<-list(list(1,list(42,list(143)))); x[[c(1, 2, 2)]] }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_abc5322be544b3fcba9dfd4674f304dd() {
        assertEval("{ x<-list(1); x[[c(TRUE, TRUE)]] }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_e035619bcbbd3f9bce8a888843042049() {
        assertEval("{ x<-list(1); x[[NA]] }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_674a5352d4926d08e2f1a47f65d38ab6() {
        assertEval("{ x<-list(1); x[[c(NA)]] }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_3bf516f6868e5f2180d89713d7b92d82() {
        assertEval("{ x<-list(1); x[[as.integer(NA)]] }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_83252c4bc0173a6fe39524ec3c9ffa23() {
        assertEval("{ x<-list(42,2,3); x[[c(NULL,1)]] }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_74ade69deb64efb2234d28178ff2ed2c() {
        assertEval("{ x<-list(42,2,3); x[[c(NULL, NULL,1)]] }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_dd7ef07d8adf80c3e7836f3ff9d81f58() {
        assertEval("{ x<-list(42,2,3); x[[c(NULL, 2,1)]] }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_abf9db339ae4c33fa0df21e6a303d4e3() {
        assertEval("{ x<-c(a=1, b=2); x[1] }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_2a8a12d1a4f78b4aff180eaec1f01555() {
        assertEval("{ x<-c(a=1, b=2); x[[1]] }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_faf449da955be06ba06f19ef29c01f92() {
        assertEval("{ x<-list(1); x[\"y\"] }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_ba5840077277f2e7e5bdcba95eabb985() {
        assertEval("{ x<-list(1); x[[\"y\"]] }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_58c8faa612c00fa5f48f6567d21e52a4() {
        assertEval("{ x<-list(a=42); x[\"b\"] }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_ccb9c77dcb25926986055394dd2f891d() {
        assertEval("{ x<-list(a=42); x[[\"b\"]] }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_4f60ba9ce036cc41dbd660290a7f9d4d() {
        assertEval("{ x<-list(a=42); x[\"a\"] }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_89f8ec5eb3f4840a588ead03b3213ab5() {
        assertEval("{ x<-list(a=42); x[[\"a\"]] }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_ed698d132f7811727d6e0405fc1173a1() {
        assertEval("{ x<-list(a=list(42)); x[[c(\"a\", \"y\")]] }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_eda4ee811f875757d3664d3a22336477() {
        assertEval("{ x<-list(a=list(b=42)); x[[c(\"a\", \"b\")]] }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_8c99590764a83f983d9fd4af25755b15() {
        assertEval("{ l<-list(1,2,3,4); l[[c(2,1)]]<-7; l }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_8bcbe335d7a1878c5771f33eb774d5d6() {
        assertEval("{ l<-list(1,2,3,4); l[c(2,1)]<-7; l }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_88a0ba292059d1acf6986685d3cdf6d7() {
        assertEval("{ x<-1; x[0]<-integer(); x }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_68568fd3ee518dd4c6596a397a2360c5() {
        assertEval("{ x<-1; x[]<-42; x }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_215478542f0b66592dac8d1a21953047() {
        assertEval("{ x<-7; x[NA]<-42; x }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_197b78e693f00688299473daebc73b19() {
        assertEval("{ x<-7; x[NULL]<-42; x }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_6374ecc92aca47b6d9c557df6e75d5af() {
        assertEval("{ x<-7; x[0]<-42; x }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_4fa8baa005d68aba113f16f8600cc456() {
        assertEval("{ x<-1:4;  x[c(1, 0)]<-42; x }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_d7c709fd74b5f6ecbb9dddcce35ca1a7() {
        assertEval("{ x<-1:4;  x[c(0, 1)]<-42; x }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_2e91e5794cc865d99276d98f9ba0eef0() {
        assertEval("{ x<-1:4; dim(x)<-c(2,2); x[NULL]<-42; x }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_5ac2a690b2b91232081e1e0be7270309() {
        assertEval("{ x<-1:4; dim(x)<-c(2,2); x[NA]<-42; x }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_5d2b74ba21d72dd200bb20c841b8cc6f() {
        assertEval("{ x<-1:4; dim(x)<-c(2,2); x[0]<-42; x }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_cc0267fdeb07843c1e62d2ab7e9d75ad() {
        assertEval("{ x<-1:4; dim(x)<-c(2,2); x[c(1,4)]<-c(42, 43); x }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_87592c81cfaba6f155729635583ec95b() {
        assertEval("{ x<-1:4; dim(x)<-c(2,2); x[c(1,1,0)]<-c(42, 43); x }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_b5b1ba9bc43a6c264c0e86d9d9456a42() {
        assertEval("{ x<-1:4; dim(x)<-c(2,2); x[c(0,1,1)]<-c(42, 43); x }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_9ee2e6aa7d9ed695fc792ec5f013e59d() {
        assertEval("{ x<-1:4; dim(x)<-c(2,2); x[c(0,0)]<-c(42, 43); x }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_8ffe36d4de29230e074c24bd49486086() {
        assertEval("{ x<-1:4; dim(x)<-c(2,2); x[c(0,0,0)]<-c(42, 43); x }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_77c736a6c4cd4e7364356c486977dcb4() {
        assertEval("{ x<-1:4; dim(x)<-c(2,2); x[-1]<-42; x }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_23de3ea27d3f2f6bc2798a094cae3390() {
        assertEval("{ x<-1:4; dim(x)<-c(2,2); x[-5]<-42; x }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_73c2a56b1d4e148bc167865ae9a5db4c() {
        assertEval("{ x<-1:4; dim(x)<-c(2,2); x[c(-1, -2)]<-42; x }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_84a5d8fde238008943dedef5bb04a204() {
        assertEval("{ l <- list(1,2,3) ; l[c(1,3)] <- NULL ; l }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_8f173e6c9429270bbf6ce80048a9787a() {
        assertEval("{ l <- list(1,2,3) ; l[c(1,3)] <- c(NULL, 42) ; l }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_cdfe5e94399c2263120560e973463f1e() {
        assertEval("{ l <- list(1,2,3) ; l[c(1,3)] <- c(NULL, NULL) ; l }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_09ced80f6614e27da58ea713313539f5() {
        assertEval("{ x<-list(1,2,3,4); dim(x)<-c(2,2); x[1]<-NULL; x }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_51c02efcd353b844c578a8267190eafb() {
        assertEval("{ x<-list(1,2,3,4); dim(x)<-c(2,2); x[[1]]<-NULL; x }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_437e4d7f2837d403512ddc04b07f95bc() {
        assertEval("{ x<-1:4; x[0]<-NULL; x }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_1e07b5f4a5833ead113bb3a97506dd0d() {
        assertEval("{ n<-1; n[7]<-42; n }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_ba496589b6e2157d6f327a94259a813d() {
        assertEval("{ n<-1; n[[7]]<-42; n }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_c3358f70336e8bedd9884ac14b85155a() {
        assertEval("{ n<-1; n[c(7,8)]<-c(42,43); n }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_66ff3c78e1c0c1bdd02c91a780f1f856() {
        assertEval("{ x<-NULL; x[1]<-42; x }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_1e5241694f6bd4c4ed352fa97484a11d() {
        assertEval("{ x<-NULL; x[1]<-42+7i; x }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_af25ec66ede55ff79eb1cb16be10a388() {
        assertEval("{ x<-NULL; x[7]<-42; x }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_79892af0b66074eae7db278a1944bf0b() {
        assertEval("{ x<-NULL; x[1,1]<-NULL }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_ae929ae5d06a8c37a041f37d39b0beeb() {
        assertEval("{ x<-c(a=1); x[\"b\"]<-2; x }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_01b1453beb8f932c92276a81cff16755() {
        assertEval("{ x<-c(a=1); x[c(\"a\",\"b\")]<-c(7,42); x }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_f05039b2bfc8b504f6930aeba9c35c18() {
        assertEval("{ x<-c(a=1); x[c(\"a\",\"b\",\"b\")]<-c(7,42,100); x }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_17b647743be7e4e02cc7a515c2441ac7() {
        assertEval("{ x<-NULL; x[c(\"a\", \"b\")]<-42L; x }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_75fa7156e5a5c7e311d917a928bfc204() {
        assertEval("{ x<-c(1,2); dim(x)<-2; attr(x, \"foo\")<-\"foo\"; x[\"a\"]<-42; attributes(x) }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_f46c03ce3faf0d1cd2c7b9fbcfe77972() {
        assertEval("{ x<-c(1,2); dim(x)<-2; attr(x, \"foo\")<-\"foo\"; x[1]<-42; attributes(x) }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_72cb8c41c1f91cf5cc5e3399ce4a2bda() {
        assertEval("{ x <- NULL; x[c(\"a\", as.character(NA))] <- 7; x }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_b7089a4d72fdff3453ebf281883962ef() {
        assertEval("{ x <- NULL; x[c(\"a\", as.character(NA), as.character(NA))] <- 7; x }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_ab4d2db7b9105ec951a2dd9302f737cc() {
        assertEval("{ b<-3:5; dim(b) <- c(1,3) ; b[] <- NULL ; b }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_928a7fb171cb379cdf5c1db998f4a841() {
        assertEval("{ b<-3:5; dim(b) <- c(1,3) ; b[c(FALSE, FALSE, FALSE)] <- NULL ; b }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_f1490cc8d0fd15fdbd26c85b3a0847d4() {
        assertEval("{ b<-3:5; dim(b) <- c(1,3) ; b[0] <- NULL ; b }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_5c55e6278eb67cd52fcbebe24a7c28cc() {
        assertEval("{ l<-list(1,2,3,4); l[1]<-NULL; l }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_6e5e00b5f895b5c856d1db0dbb02fd1d() {
        assertEval("{ l<-list(1,2,3,4); l[4]<-NULL; l }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_9d404382f5c38b3e268a152ee71f03e8() {
        assertEval("{ l<-list(1,2,3,4); l[5]<-NULL; l }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_f7109f17d6d2380070cb094490a01206() {
        assertEval("{ l<-list(1,2,3,4); l[7]<-NULL; l }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_999267c3cc2ced88aab05561c153f358() {
        assertEval("{ l<-list(1); l[1]<-NULL; l }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_90db04b7eaf5dac15c1af0e5dcf2241f() {
        assertEval("{ x<-list(list(1,list(42))); x[[c(1, 2)]]<-7; x }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_ba5c0a816efdcb0ebfb07371ae87873e() {
        assertEval("{ x<-list(list(1,list(42,list(143)))); x[[c(1, 2, 2)]]<-7; x }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_d13b161a7c39a96099ab95a178aa030d() {
        assertEval("{ x<-list(list(1,list(42,list(143)))); x[[c(1, 2, 7)]]<-7; x }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_2c24533ee8c4dc58f8d4c115a87f8594() {
        assertEval("{ x<-list(list(1,list(42,list(list(143))))); x[[c(1, 2, 2, 1)]]<-7; x }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_f83813a00871826e6340b24a97e46f56() {
        assertEval("{ x<-list(1, list(42, 1)); x[[c(-1, -2)]]<-7; x }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_654cff711af9f940bc8b3349aafc1866() {
        assertEval("{ x<-list(1, list(42)); x[[c(-1, 1)]]<-7; x }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_6dee791876ffc675e915a26e1d4a5895() {
        assertEval("{ x<-list(1, list(42)); x[[c(2, 5)]]<-7; x }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_12b6b8631f2d98575baab0c016e42323() {
        assertEval("{ x<-list(1, list(42)); x[c(2, 5)]<-7; x }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_2467c2da125cd598c66cfcdd43a52def() {
        assertEval("{ x<-list(1, list(42)); dim(x)<-c(1,2); x[[c(2, 5)]]<-7; x }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_f8eeb432057857da02737da734b4eee0() {
        assertEval("{ x<-list(1, list(42)); dim(x)<-c(1,2); x[c(2, 5)]<-7; x }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_c84608271bf18ed6403361a730ddaaed() {
        assertEval("{ y<-list(42,7); dim(y)<-c(1:2); attr(y, \"foo\")<-\"foo\"; x<-list(1, y); dim(x)<-c(1,2); x[[c(2, 1)]]<-7; x[2] }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_1ab2fc7ae1c1ce3637c7d64994fa6b38() {
        assertEval("{ l<-list(1,2,3,4); l[c(1,3)]<-list(NULL); l }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_29bace176839c7b8128eba298c105af2() {
        assertEval("{ x<-list(1, list(42)); x[[c(2, 1)]]<-NULL; x }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_d4a35c7149f0abdba9f06e9e1ff8ecaa() {
        assertEval("{ x<-list(1, list(42)); x[[c(2, 5)]]<-NULL; x }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_950f207a8c6eb4f4bb6aa6d80db0d99e() {
        assertEval("{ x<-list(1, list(42)); x[[c(-1, 1)]]<-NULL; x }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_ab29b7072a95630f7dd0ba6f2e47d80c() {
        assertEval("{ m <- matrix(list(1,2,3,4,5,6), nrow=3) ; m[c(2,3,6,7)] <- NULL ; m }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_b62c6c7cc5100030efeadf49c991b585() {
        assertEval("{ m <- matrix(list(1,2,3,4,5,6), nrow=3) ; m[c(2,3,6,8)] <- NULL ; m }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_4eed5e15c0cd1f02407d79681aa5dad2() {
        assertEval("{ m <- matrix(list(1,2,3,4,5,6), nrow=3) ; m[c(2,3,7)] <- NULL ; m }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_1ed930df0955322c7a50aa7123397d30() {
        assertEval("{ m <- matrix(list(1,2,3,4,5,6), nrow=3) ; m[c(2,3,8)] <- NULL ; m }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_a1b1dbb99042ce4a6c2e5abfbcca94da() {
        assertEval("{ m <- matrix(list(1,2,3,4,5,6), nrow=3) ; m[c(2,3,6,8,9)] <- NULL ; m }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_0a0c66f7f493e765a508f1481cab7c1a() {
        assertEval("{ b<-as.list(3:5); dim(b) <- c(1,3) ; b[] <- NULL ; b }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_884003e5a5cbcd8c36fece04d00d9eb9() {
        assertEval("{ b <- as.list(3:5) ; dim(b) <- c(1,3) ; b[NULL] <- NULL ; b }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_24b0f34219c8db8be64cc7e478db5793() {
        assertEval("{ b<-as.list(3:5); dim(b) <- c(1,3) ; b[c(1,2)] <- NULL ; b }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_cff260d84aab9ee039a27df2a8f5b0c7() {
        assertEval("{ b<-as.list(3:5); dim(b) <- c(1,3) ; b[c(1)] <- NULL ; b }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_c3bbc73cfffafc3c7f1739b6e6f887e4() {
        assertEval("{ b<-as.list(3:5); dim(b) <- c(1,3) ; b[[c(1)]] <- NULL ; b }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_133c86809f35720b10f52aeab2dcef62() {
        assertEval("{ b<-as.list(3:5); dim(b) <- c(1,3) ; b[0] <- NULL ; b }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_24a56d50cabeb817c4a0b9c887b44909() {
        assertEval("{ l <- list(a=1,b=2) ; attr(l, \"foo\")<-\"foo\"; l[1] <- NULL ; l }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_ddad35e4aa1e259b94b54e0511c155f6() {
        assertEval("{ l <- list(a=1,b=2) ; attr(l, \"foo\")<-\"foo\"; l[[1]] <- NULL ; l }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_eedb0842754877a220476dda916bfa12() {
        assertEval("{ l <- matrix(list(a=1,b=2,c=3,d=4)) ; attr(l, \"foo\")<-\"foo\"; l[1] <- NULL ; attributes(l) }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_d4f881825653fb51729fc65d499b45b2() {
        assertEval("{ l <- matrix(list(a=1,b=2,c=3,d=4)) ; attr(l, \"foo\")<-\"foo\"; l[[1]] <- NULL ; attributes(l) }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_9a9412f98a1c8133fbce96001917936a() {
        assertEval("{ l <- list(1,2) ; names(l)<-c(\"a\", \"b\"); attr(l, \"foo\")<-\"foo\"; l[1] <- NULL ; attributes(l) }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_4e6189023ce3267eb2085d45a0c4607a() {
        assertEval("{ l <- list(1,2) ;  attr(l, \"foo\")<-\"foo\"; names(l)<-c(\"a\", \"b\"); l[1] <- NULL ; attributes(l) }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_1e3c027fef1c3ffe555f75ae1e091490() {
        assertEval("{ l <- c(1,2) ; names(l)<-c(\"a\", \"b\"); attr(l, \"foo\")<-\"foo\"; l[1] <- 7 ; attributes(l) }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_f4668962bb161304f9fabc3b9f909aa1() {
        assertEval("{ l <- c(1,2) ;  attr(l, \"foo\")<-\"foo\"; names(l)<-c(\"a\", \"b\"); l[1] <- 7 ; attributes(l) }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_1e74830104a0415f597dc77fb35eefc8() {
        assertEval("{ x<-1:4; dim(x)<-c(2,2); x[c(1, NA), 2]<-7; x }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_80bbf2457e4503a7e67cdc09e1c24f4c() {
        assertEval("{ x<-1:4; x[c(1, NA)]<-7; x }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_0ba7832d1151c1f13a885edef388f0c5() {
        assertEval("{ x<-1:4; dim(x)<-c(2,2); x[c(1, NA)]<-7; x }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_dfd6482632647b3dd9427a1560bf0d3a() {
        assertEval("{ x<-1:4; dim(x)<-c(2,2);  x[NA, NA]<-7; x }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_8696a766063b6ab8173a6e7efa9e3e5d() {
        assertEval("{ x<-1:4; dim(x)<-c(2,2); x[c(NA, NA),1]<-7; x }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_6195cd6ac53f9f62c9cde2bcbca0cdce() {
        assertEval("{ x<-1:4; dim(x)<-c(2,2); x[c(NA, 1),1]<-7; x }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_23e220f9c43711417c97b6024e96b424() {
        assertEvalError("{ x<-c(1,2); x[[c(\"a\")]] }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_e176ff384390f53eb9bf045388e4f6c0() {
        assertEvalError("{ x<-c(1,2); dim(x)<-c(1,2); x[[c(\"a\")]] }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_217d7ae9da117b0d370b04e663c7590d() {
        assertEvalError("{ x<-c(1,2); dim(x)<-c(1,2); x[\"a\", \"b\"] }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_c6629576c5b02cba4a8abff42f3f6eaf() {
        assertEvalError("{ x<-c(1,2); dim(x)<-c(1,2); x[[\"a\", \"b\"]] }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_018c4131eb9bf538bce5302d577cb481() {
        assertEvalError("{ x<-c(1,2); x[[c(\"a\", \"b\")]] }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_39efab523ad0459683e78ccca8cb8ace() {
        assertEvalError("{ x<-1:2; x[[c(TRUE, TRUE)]] }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_9001b0057551988e2eeaffbd28c61983() {
        assertEvalError("{ x<-1:2; dim(x)<-c(1,2); x[2+2i, 2+2i] }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_6522a2ad1052e08e9af2845bac99d000() {
        assertEvalError("{ x<-1:2; dim(x)<-c(1,2); u<-2+2i; x[[u, u]] }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_f9d1e352fb09ee37390927e2bbc9df13() {
        assertEvalError("{ x<-2; x[[NULL]] }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_3b0d0ebcdd207346be89a5baa67c1f1a() {
        assertEvalError("{ x<-1; x[[]] }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_02f5eed076c04322e6fb328987195cbe() {
        assertEvalError("{ x<-1:2; dim(x)=c(1,2); x[[, ]] }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_6576b9f60b34c296d3d792fdfb47aa07() {
        assertEvalError("{ x<-1:2; dim(x)=c(1,2); x[[, 1]] }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_c1cf11147e5b0f1d3daedeed31fe2b1b() {
        assertEvalError("{ x<-1:2; dim(x)=c(1,2); x[[1, ]] }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_3c02f94bd25c41e872d4fa4f6bab0a9c() {
        assertEvalError("{ x<-1:2; dim(x)=c(1,1,2); x[[1, , 1]] }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_c54b4533430e6e346c3a7c71efeabfa1() {
        assertEvalError("{ x<-1:2; dim(x)=c(1,1,2); x[[, , 1]] }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_8e00cb54b880e1b152cd846a4833354f() {
        assertEvalError("{ x<-1:2; dim(x)=c(1,1,2); x[[1, , ]] }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_6dcc10478f68a120a80bb3d0346ad506() {
        assertEvalError("{ x<-c(1,2); dim(x)<-c(1,2); dimnames(x)<-list(\"a\", c(\"b\", \"c\")); x[\"z\", \"x\"] }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_24793b17b3c0ea9d535fb8354a550aee() {
        assertEvalError("{ x<-c(1,2); dim(x)<-c(1,2); dimnames(x)<-list(\"a\", c(\"b\", \"c\")); x[[\"z\", \"x\"]] }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_0eccf7799fcc08024179091319dc3238() {
        assertEvalError("{ x<-c(a=1, b=2); x[[c(\"z\", \"x\")]] }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_c3caa8615256f828d614b1b121e8d528() {
        assertEvalError("{ x<-as.integer(1:4); x[[as.integer(NA)]] }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_7882cdfb769a9c138fe1b9eb2d740c53() {
        assertEvalError("{ x<-as.integer(1:4); dim(x)<-c(2,2); x[[as.integer(NA)]] }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_d5020554c570ba6ec99bc6041684052c() {
        assertEvalError("{ x<-1:4; dim(x)<-c(2,2); x[[0, 0]] <- integer(); x }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_b872d088f6c3cbf6c8ab57c51df9800c() {
        assertEvalError("{ x<-1:4; dim(x)<-c(2,2); x[[-1, 0]] <- integer(); x }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_c26e1ed227232092068a7745823f1f4b() {
        assertEvalError("{ x<-1:4; dim(x)<-c(2,2); x[[0, -1]] <- integer(); x }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_6c6eac65db6c337372945e0f61d1127d() {
        assertEvalError("{ x<-1:4; dim(x)<-c(2,2); x[[-5, 0]] <- integer(); x }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_6aeb45ed82ab80f912d9d13958b2e442() {
        assertEvalError("{ x<-1:4; dim(x)<-c(2,2); x[[0, -5]] <- integer(); x }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_62dd1389fce7f08bcc30a38040f79087() {
        assertEvalError("{ x<-list(1); x[[c(1, 2)]] }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_29f31365b5fbcc4c9ea1af59336b109d() {
        assertEvalError("{ x<-1; x[[c(1, 1)]] }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_beab803554d0c967967ad76b4426c218() {
        assertEvalError("{ x<-list(1); x[[c(1, 1, 1)]] }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_18dfb18fd0a6724aabf0a4aa54b57358() {
        assertEvalError("{ x<-list(1); x[[c(1, 2, 0)]] }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_66c905d2a2121fba66923e543973102f() {
        assertEvalError("{ x<-list(1); x[[c(1, 0)]] }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_60724dddd2ff1994aaa8268b606f344b() {
        assertEvalError("{ x<-list(1); x[[c(1, NA)]] }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_ca176d2dcd2b1e992f2045f89b99f6ff() {
        assertEvalError("{ x<-list(1); x[[c(TRUE, FALSE)]] }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_23fdd759e65b9a6577800575f0566221() {
        assertEvalError("{ x<-c(1); x[[NA]] }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_0762365d9263410d2140da754b8bf401() {
        assertEvalError("{ x<-list(1); x[[NULL]] }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_bfe22aa12f4a2ec609defc4248d94521() {
        assertEvalError("{ x<-list(1); x[[c(NULL)]] }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_32f8ef4eed2c72eabc2817b0e490e3d7() {
        assertEvalError("{ x<-list(1); x[[0]] }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_8f6f05767e7e126cdebc3289dbce59f1() {
        assertEvalError("{ x<-list(1); x[[c(0)]] }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_ac581ed6e9d9bf2c203dd4de260ed72e() {
        assertEvalError("{ x<-list(1); x[[-1]] }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_45215232ebd54fc2bc10e30252dfb8da() {
        assertEvalError("{ x<-list(1,2,3); x[[-1]] }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_1c1fdeb874dcab7bd9fac094bcfd9c98() {
        assertEvalError("{ x<-list(1,2,3); x[[-5]] }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_3c1a4923d96fea1f421192a99483e461() {
        assertEvalError("{ x<-list(42,2,3); x[[c(NA, 1)]] }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_ebd2fed75eb9213d6bf5b81b2fab2dd1() {
        assertEvalError("{ x<-list(42,2,3); x[[c(0, 1)]] }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_e91389214f815f56315d1d6d3a770121() {
        assertEvalError("{ x<-list(42,2,3); x[[c(1, -1)]] }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_7bdf9c192d7e05d8e91b1a45e8846606() {
        assertEvalError("{ x<-list(42,2,3); x[[c(-1, 1)]] }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_038a34e2644ecc8f403869f290bf58d3() {
        assertEvalError("{ x<-list(42,2,3); x[[c(1, NULL, 2)]] }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_34d72ddc694e32ae6a989ab0e3b83082() {
        assertEvalError("{ x<-list(42,2,3); x[[c(1, 2)]] }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_dce54ff351f23f5b44fea07866a19e28() {
        assertEvalError("{ x<-list(42,2,3); x[[c(NULL, 2, 1, 3)]] }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_4b203c2e2f3deac872700cb5379e37bd() {
        assertEvalError("{ x<-list(42,2,3); x[[c(NULL, 2, NULL, 1, 3)]] }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_070596d797cf4ec75971677dd13a08e3() {
        assertEvalError("{ x<-list(42,2,3); x[[c(2, 1, 3)]] }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_3afe3afeda75dd2fc1484d3bb526e1de() {
        assertEvalError("{ x<-1:8; dim(x)<-c(2,2,2); x[[-3, 1, 1]] }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_641e88efadea1dc614e810e498270dc9() {
        assertEvalError("{ x<-list(a=42); x[[c(\"a\", \"y\")]] }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_db7d6b2348d086b68e1b702eba57c185() {
        assertEvalError("{ l<-list(1,2,3,4); l[[c(NA,1)]]<-c(1); l }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_6bcb89680f1e94b680b25d5a5096b70e() {
        assertEvalError("{ l<-list(1,2,3,4); l[[c(1,NA)]]<-c(1); l }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_a09322b92ec816b47437681de5e70d05() {
        assertEvalError("{ l<-list(1,2,3,4); l[[c(7,1)]]<-c(1); l }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_220b5e0ad05034470735a930bb394211() {
        assertEvalError("{ l<-list(1,2,3,4); l[[c(NA)]]<-c(1); l }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_cbc317a1501e2f0933abad37cdbeb28d() {
        assertEvalError("{ l<-list(1,2,3,4); l[[c(NA,1)]]<-c(-1); l }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_9290ef2d56b023ee73121144e33b3087() {
        assertEvalError("{ l<-list(1,2,3,4); l[[c(-1)]]<-c(1); l }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_11b9d0165c0406999453d86f05274783() {
        assertEvalError("{ l<-list(1,2,3,4); l[[c(-1,1)]]<-c(1); l }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_b0999c4077d6414b1fde9376454b8d47() {
        assertEvalError("{ l<-list(1,2,3,4); l[[c(0)]]<-c(1); l }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_5f51e2624674520079f8b2fabc9cce90() {
        assertEvalError("{ l<-list(1,2,3,4); l[[c(0,1)]]<-c(1); l }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_bbf8d3f63872869e2a919ed15261557b() {
        assertEvalError("{ l<-list(1,2,3,4); l[[c(1,1,1)]]<-c(1); l }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_9e239e75e9c3fcca9c98188411c31971() {
        assertEvalError("{ l<-list(list(1),2,3,4); l[[c(1,1,NA)]]<-c(1); l }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_a8880813618f5e6d2e38495e75f6958f() {
        assertEvalError("{ x<-1:4; x[[1]]<-c(1,1); x }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_a4524653d0e4625f9a320c553ee13f4f() {
        assertEvalError("{ x<-1; x[[0]]<-integer(); x }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_7aacc03e28c232195908535ffc28a37e() {
        assertEvalError("{ x<-1; x[1]<-integer(); x }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_abe91c4902cf1f15eb0d5ba865101fbb() {
        assertEvalError("{ x<-1; x[[]]<-42; x }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_eb4e0875f0c085f6e32467d80ef82ee5() {
        assertEvalError("{ x<-7; x[NA]<-c(42, 7); x }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_187bc52f2a55d71bfb61f62bd1daced8() {
        assertEvalError("{ x<-7; x[[NA]]<-42; x }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_b8a22eab16cc69d4718d97591279cf53() {
        assertEvalError("{ x<-7; x[[NA]]<-c(42, 7); x }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_84ff95b327c70ec303c4b9f3405f0d8a() {
        assertEvalError("{ x<-7; x[[NULL]]<-42; x }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_7eeb9ba184b0c3bab9b9af3a2a212ae6() {
        assertEvalError("{ x<-7; x[[0]]<-42; x }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_742f575fd8be7af0531308e766e83348() {
        assertEvalError("{ x<-1:4; dim(x)<-c(2,2); x[[c(1,4)]]<-c(42, 43); x }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_1b82f4f1751a57f055cebbc4e83f513c() {
        assertEvalError("{ x<-1:4; dim(x)<-c(2,2); x[c(1,NA)]<-c(42, 43); x }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_7fc506271b8fe81fc07dc74a34a5ec61() {
        assertEvalError("{ x<-1:4; dim(x)<-c(2,2); x[c(NA,1)]<-c(42, 43); x }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_9246e0ea5eeaf5137fc28e783e875f6d() {
        assertEvalError("{ x<-1:4; dim(x)<-c(2,2); x[[c(1,0)]]<-c(42, 43); x }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_df586fa72a24f11c1a0096bbd1a56e84() {
        assertEvalError("{ x<-1:4; dim(x)<-c(2,2); x[[c(1,0,0)]]<-c(42, 43); x }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_50122aa69a9095e68504dd5b14fffcaf() {
        assertEvalError("{ x<-1:4; dim(x)<-c(2,2); x[[c(1,1,0)]]<-c(42, 43); x }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_42ede1e07b91b73a80514a29b0bb2f06() {
        assertEvalError("{ x<-1:4; dim(x)<-c(2,2); x[[c(0,1)]]<-c(42, 43); x }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_0c13f7f058762cbcca92f64a7b788c19() {
        assertEvalError("{ x<-1:4; dim(x)<-c(2,2); x[[c(0,0,1)]]<-c(42, 43); x }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_884863e30908d1f87946572407965125() {
        assertEvalError("{ x<-1:4; dim(x)<-c(2,2); x[[c(0,1,1)]]<-c(42, 43); x }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_17c4985e036906526f4c692cf73aa7ca() {
        assertEvalError("{ x<-1:4; dim(x)<-c(2,2); x[[c(0,0)]]<-c(42, 43); x }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_f97a9898cd6695fe05bdf4b0f647e395() {
        assertEvalError("{ x<-1:4; dim(x)<-c(2,2); x[[c(0,0,0)]]<-c(42, 43); x }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_1cb851b978ddad132613a2bb05a063c6() {
        assertEvalError("{ x<-1:4; dim(x)<-c(2,2); x[[c(FALSE,TRUE)]]<-c(42,43); x }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_661021a4962239ad4c19295097119aab() {
        assertEvalError("{ x<-1:4; dim(x)<-c(2,2); x[[c(FALSE,TRUE,TRUE)]]<-c(42,43); x }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_87e16bc4899197eaa5cc40f70838534d() {
        assertEvalError("{ x<-1:4; dim(x)<-c(2,2); x[[complex()]]<-c(42,43); x }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_6b1152d7009932999e9a8dc2dc529f62() {
        assertEvalError("{ x<-1:4; dim(x)<-c(2,2); x[[c(1+1i)]]<-integer(); x }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_02be4c4213a9ec9e14b696616703933e() {
        assertEvalError("{ x<-1:4; dim(x)<-c(2,2); x[[c(1+1i)]]<-c(42); x }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_6e773c8219c54df7a058e2265f008920() {
        assertEvalError("{ x<-1:4; dim(x)<-c(2,2); x[[c(1+1i)]]<-c(42,43); x }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_9b177421dba0231044c9c987b2a905cc() {
        assertEvalError("{ x<-1:4; dim(x)<-c(2,2); x[[c(1+1i,42+7i,3+3i)]]<-c(42,43); x }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_4cadd28e21376a1cb9b8fb943f6c8d3e() {
        assertEvalError("{ x<-1:4; dim(x)<-c(2,2); x[[c(0,42+7i)]]<-c(42,43); x }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_9562e45f72ec5f8431cc4751734ae0c8() {
        assertEvalError("{ x<-1:4; dim(x)<-c(2,2); x[c(1+1i)]<-integer(); x }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_c14298020b2a344b2af9e738646c5352() {
        assertEvalError("{ x<-1:4; dim(x)<-c(2,2); x[c(1+1i)]<-c(42); x }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_8d06122dd1ef3483cad67937e0b6629b() {
        assertEvalError("{ x<-1:4; dim(x)<-c(2,2); x[complex()]<-c(42,43); x }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_18dd63ca47bf47e3f9e6fee0db3f5636() {
        assertEvalError("{ x<-1:4; dim(x)<-c(2,2); x[c(1+1i)]<-c(42,43); x }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_c1611848b539a92a6fd7e36736ba5b32() {
        assertEvalError("{ x<-1:4; dim(x)<-c(2,2); x[c(1+1i,42+7i,3+3i)]<-c(42,43); x }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_94a3483a582c8315956edf7c2050483b() {
        assertEvalError("{ x<-1:4; dim(x)<-c(2,2); x[c(0,42+7i)]<-c(42,43); x }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_4c40449e6fbd94e06857aee1f1a9cb4b() {
        assertEvalError("{ x<-1:4; dim(x)<-c(2,2); x[[c(0,0,42+71)]]<-c(42,43); x }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_952e5ee59ac0b97579eb867f63ee4d45() {
        assertEvalError("{ x<-1:4; dim(x)<-c(2,2); x[[c(as.raw(integer()))]]<-c(42,43); x }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_55c6b6d048655801c1f34b30e90b5c3c() {
        assertEvalError("{ x<-1:4; dim(x)<-c(2,2); x[[c(as.raw(42))]]<-integer(); x }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_f46dc6001bd816937f87d50ed7067887() {
        assertEvalError("{ x<-1:4; dim(x)<-c(2,2); x[[c(as.raw(42))]]<-c(43); x }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_addc8bb3e4c764cfe79e0ed3bbfc436f() {
        assertEvalError("{ x<-1:4; dim(x)<-c(2,2); x[[c(as.raw(42))]]<-c(42,43); x }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_f97f06303d96170386df679074599e41() {
        assertEvalError("{ x<-1:4; dim(x)<-c(2,2); x[[c(as.raw(42), as.raw(7))]]<-c(42,43); x }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_d8d8d99c60ce5b7d2ce505f012e4a8cf() {
        assertEvalError("{ x<-1:4; dim(x)<-c(2,2); x[[c(as.raw(42), as.raw(7), as.raw(1))]]<-c(42,43); x }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_fdbc1b3c1492a9396638a586ba7aabd0() {
        assertEvalError("{ x<-1:4; dim(x)<-c(2,2); x[c(as.raw(integer()))]<-c(42,43); x }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_43133c58860d72478a6821dcc15dadd2() {
        assertEvalError("{ x<-1:4; dim(x)<-c(2,2); x[c(as.raw(42))]<-integer(); x }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_6f041632744bd2d1b114761530d052cb() {
        assertEvalError("{ x<-1:4; dim(x)<-c(2,2); x[c(as.raw(42))]<-c(43); x }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_ab6d9983b3931acafdca21e802becdac() {
        assertEvalError("{ x<-1:4; dim(x)<-c(2,2); x[c(as.raw(42))]<-c(42,43); x }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_eb7323c02302e184ad7b58e3e477baa7() {
        assertEvalError("{ x<-1:4; dim(x)<-c(2,2); x[c(as.raw(42), as.raw(7))]<-c(42,43); x }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_6a67a9c7aac51e68812fc0419f8a0116() {
        assertEvalError("{ x<-1:4; dim(x)<-c(2,2); x[c(as.raw(42), as.raw(7), as.raw(1))]<-c(42,43); x }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_e466f76e85fe42d2a39a8a7d5c47b40c() {
        assertEvalError("{ x<-1:4; dim(x)<-c(2,2); x[[list()]]<-integer(); x }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_7c53695c9c52822d2157515898f815c6() {
        assertEvalError("{ x<-1:4; dim(x)<-c(2,2); x[[list()]]<-c(42); x }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_c4eec1f267974213c4a6d6d9c673f8dd() {
        assertEvalError("{ x<-1:4; dim(x)<-c(2,2); x[[list()]]<-c(42,43); x }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_8aa1ed922f38b7c9e40b2fcd3b5a1946() {
        assertEvalError("{ x<-1:4; dim(x)<-c(2,2); x[[list(1)]]<-integer(); x }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_101a74bb31894c4f42ffa41a3c35873d() {
        assertEvalError("{ x<-1:4; dim(x)<-c(2,2); x[[list(1)]]<-c(42); x }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_e98301a9bc81b7e03fcf9be2b19a4730() {
        assertEvalError("{ x<-1:4; dim(x)<-c(2,2); x[[list(1)]]<-c(42,43); x }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_129276f9c14ce0e386be1e2c849e712e() {
        assertEvalError("{ x<-1:4; dim(x)<-c(2,2); x[[list(1,2)]]<-c(42,43); x }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_625e995d3d9be2f5f97d0d8946c82cd1() {
        assertEvalError("{ x<-1:4; dim(x)<-c(2,2); x[[list(1,2,3)]]<-c(42,43); x }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_18c269d06eedd840cd35b93f6fac08ba() {
        assertEvalError("{ x<-1:4; dim(x)<-c(2,2); x[list()]<-integer(); x }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_9ec5a6a88d3cf47306ce903b11db63a1() {
        assertEvalError("{ x<-1:4; dim(x)<-c(2,2); x[list()]<-c(42); x }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_8a0350fc3cc62848956542b42d97870b() {
        assertEvalError("{ x<-1:4; dim(x)<-c(2,2); x[list()]<-c(42,43); x }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_427595a8f7dbfa52e2c56bbe7b757542() {
        assertEvalError("{ x<-1:4; dim(x)<-c(2,2); x[list(1)]<-integer(); x }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_3887d58f49abca5fe7478a655f1590e1() {
        assertEvalError("{ x<-1:4; dim(x)<-c(2,2); x[list(1)]<-c(42); x }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_32febab0c9929ff347462e30b4eca24f() {
        assertEvalError("{ x<-1:4; dim(x)<-c(2,2); x[list(1)]<-c(42,43); x }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_f52f81c7a4c8c73174ed9247bcb482c2() {
        assertEvalError("{ x<-1:4; dim(x)<-c(2,2); x[list(1,2)]<-c(42,43); x }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_f3ece60814fac7397bf38d4274eb7605() {
        assertEvalError("{ x<-1:4; dim(x)<-c(2,2); x[list(1,2,3)]<-c(42,43); x }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_ed129f03de5dfece4ba8fa2a68a37faa() {
        assertEvalError("{ x<-1:4; dim(x)<-c(2,2); x[[c(-1, -2)]]<-42; x }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_0ca370d3cce01e249f32b9ccdfc2d12a() {
        assertEvalError("{ x<-list(1,2,3,4); dim(x)<-c(2,2); x[1,1]<-NULL; x }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_035f43c2fd1a69e8f3bab5ab54761b64() {
        assertEvalError("{ x<-list(1,2,3,4); dim(x)<-c(2,2); x[[1,1]]<-NULL; x }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_f6f5b126455c0d4ba3e5dee006e01c48() {
        assertEvalError("{ x<-c(1,2,3,4); dim(x)<-c(2,2); x[1,1]<-NULL; x }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_27d35661168c4e6b3bed381c312a2bc7() {
        assertEvalError("{ x<-c(1,2,3,4); dim(x)<-c(2,2); x[[1,1]]<-NULL; x }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_870dedec5f6380a0125faeb675762994() {
        assertEvalError("{ x<-c(1,2,3,4); dim(x)<-c(2,2); x[1]<-NULL; x }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_1acdfdc91d57454ef82b08797212fa73() {
        assertEvalError("{ x<-1:4; x[1]<-NULL; x }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_2188231eaf197b390e1b65567eb6c3da() {
        assertEvalError("{ n<-1; n[[c(7,8)]]<-c(42,43); n }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_98e45afe06f34cb156e3a8c4e470661c() {
        assertEvalError("{ x<-NULL; x[1,1]<-42; x }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_5dd1752daaee791916e2eea59e6b3816() {
        assertEvalError("{ x<-NULL; x[[1,1]]<-42; x }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_811c38fd0ea4276c256f3e2d53a7ee5f() {
        assertEvalError("{ x<-NULL; x[1,1,1]<-42; x }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_139a97034bdefea13a9e5e527f385837() {
        assertEvalError("{ x<-NULL; x[[1,1,1]]<-42; x }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_564f32e683589596f081a21c9134751f() {
        assertEvalError("{ x<-1; x[1,1]<-42; x }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_6db5c49a1df1f6c77bfc6c323469abf2() {
        assertEvalError("{ x<-1; x[[1,1]]<-42; x }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_2ce483fdd6de8250f0923775c7d456cd() {
        assertEvalError("{ x<-1; x[1,1,1]<-42; x }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_4c56d8fdea5af593593cd6d73b25b3b3() {
        assertEvalError("{ x<-1; x[[1,1,1]]<-42; x }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_738deeef72f0e97b329aa4cc97252bab() {
        assertEvalError("{ b<-3:5; dim(b) <- c(1,3) ; b[[c(1,2)]] <- NULL ; b }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_7d0a5295979444e5d678e06f69346920() {
        assertEvalError("{ b<-3:5; dim(b) <- c(1,3) ; b[c(1,2)] <- NULL ; b }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_e5cacb60fe96b1017cc955151435f132() {
        assertEvalError("{ b<-3:5; dim(b) <- c(1,3) ; b[c(1)] <- NULL ; b }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_692f518fc549d8fb0a730108e8bef736() {
        assertEvalError("{ x<-list(list(1,list(42,list(143)))); x[[c(1, 2, 7, 7)]]<-7; x }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_398598eb055d0381910904833fcdc253() {
        assertEvalError("{ x<-list(list(1,list(42,list(list(143))))); x[[c(1, NA, 2, 1)]]<-7; x }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_9b95c8210b23d53828c2bb42cb6d8350() {
        assertEvalError("{ x<-list(list(1,list(42,list(list(143))))); x[[c(1, 2, 2, NA)]]<-7; x }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_8233ee52bbe594da451369ab3d789d39() {
        assertEvalError("{ x<-list(1, list(42)); x[[c(-3, 1)]]<-7; x }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_b463205a19eb54f89e73dbb219aa3359() {
        assertEvalError("{ x<-list(1, 2, list(42)); x[[c(-1, 1)]]<-7; x }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_db6a47288f08e9ad879f15b57ae5ee46() {
        assertEvalError("{ x<-list(1, list(42, 1)); x[[c(-1, -3)]]<-7; x }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_0cb958e4a80f9d06b7dd456d0dc54c1d() {
        assertEvalError("{ x<-list(1, list(42, 1, 2)); x[[c(-1, -2)]]<-7; x }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_9f9196de1851d669b74b2f60a2963de1() {
        assertEvalError("{ x <- list() ; x[[NA]] <- NULL ; x }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_64e2ad04b3bc06d21a804f2d05bc2e77() {
        assertEvalError("{ x <- list(1) ; x[[NA]] <- NULL ; x }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_325ccb7760ed51594ea6bb1880c6063c() {
        assertEvalError("{ x <- list(1,2) ; x[[NA]] <- NULL ; x }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_4fa8f7bd90d598c3ec2bdd45a1da11b7() {
        assertEvalError("{ x <- list(1,2,3) ; x[[NA]] <- NULL ; x }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_f0678151c819bc4f3eb4dae6db644bec() {
        assertEvalError("{ b<-as.list(3:5); dim(b) <- c(1,3) ; b[[c(1,2)]] <- NULL ; b }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_dda366aaec278dbc5223a2f7cc60646d() {
        assertEvalError("{ b<-as.list(3:5); dim(b) <- c(1,3) ; b[[0]] <- NULL ; b }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_ec58c8d651cf401cc31aa8a731080f42() {
        assertEvalError("{ x<-1:4; x[c(1, NA)]<-c(7, 42); x }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_6fdb20b8d158008062e243a4325bd6c5() {
        assertEvalError("{ x<-1:4; dim(x)<-c(2,2); x[c(NA, 1),1]<-c(7, 42); x }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_93c3d8d129ec1960c034ca95932b108d() {
        assertEvalError("{ x<-c(1); x[[-4]]<-7 }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_bfc881280fe371d3c90f164a4eff917b() {
        assertEvalError("{ x<-list(1); x[[-4]]<-7 }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_6367f29b9be5e09105fd47109eb9f25a() {
        assertEvalError("{ x<-c(1,2,3); x[[-4]]<-7 }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_3b2b326bb29b921bac7f0169f8dc2411() {
        assertEvalError("{ x<-list(1,2,3); x[[-4]]<-7 }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_a338599f5990914a216512af38ecff12() {
        assertEvalError("{ x<-c(1,2,3); x[[-1]]<-7 }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_70dcba37b8de50fa12a7de0f4a5a8abb() {
        assertEvalError("{ x<-list(1,2,3); x[[-1]]<-7 }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_35f1e2a24fc60c149435c1fe132a003d() {
        assertEvalError("{ x<-c(1); x[[-4]]<-NULL }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_99fdfa6b74f507f1bee2ca5fb4390b23() {
        assertEvalError("{ x<-list(1); x[[-4]]<-NULL }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_5aef7d4499818ad9eb2d54802279c67c() {
        assertEvalError("{ x<-c(1,2,3); x[[-4]]<-NULL }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_65335c69df951fe1e9433f651b8d9a21() {
        assertEvalError("{ x<-list(1,2,3); x[[-4]]<-NULL }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_d1831b8c7ceedf443ef835f7d74882c1() {
        assertEvalError("{ x<-c(1,2,3); x[[-1]]<-NULL }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_74c9929fc75d004bdc650475dbabe040() {
        assertEvalError("{ x<-list(1,2,3); x[[-1]]<-NULL }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_f40e760b22c190582d7b76bb4cfc1734() {
        assertEvalWarning("{ x<-1:4; x[1]<-c(1,1); x }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_234f3fc09448b5cc0ef68d84f0d0a987() {
        assertEvalWarning("{ x<-1:4;  x[c(1, 0)]<-c(7, 42); x }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_d5b74507ecdf19e11bfd18dae806dc48() {
        assertEvalWarning("{ x<-1:4;  x[c(0, 1)]<-c(7, 42); x }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_d96313c3423d43c62a89814f11ae7370() {
        assertEvalWarning("{ x<-1:4; dim(x)<-c(2,2); x[c(1,0)]<-c(42, 43); x }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_e0bcad7194383e1687078d57a3d125e2() {
        assertEvalWarning("{ x<-1:4; dim(x)<-c(2,2); x[c(1,0,0)]<-c(42, 43); x }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_ec4f591fac449579997fa4b050f9cb45() {
        assertEvalWarning("{ x<-1:4; dim(x)<-c(2,2); x[c(0,1)]<-c(42, 43); x }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOther_887cf433e6cbe81983f8c3c10b81f71c() {
        assertEvalWarning("{ x<-1:4; dim(x)<-c(2,2); x[c(0,0,1)]<-c(42, 43); x }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOtherIgnore_461a050f655ae44ddfc5d11f6a011e93() {
        assertEvalError("{ x<-1:4; x[[1]]<-NULL; x }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOtherIgnore_5a63039e693f0bcdb40f33a133932ebd() {
        assertEvalError("{ x<-1:4; x[[0]]<-NULL; x }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOtherIgnore_3b27f8602ed093e9302f1ed670a155cf() {
        assertEvalError("{ b<-3:5; dim(b) <- c(1,3) ; b[[c(1)]] <- NULL ; b }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOtherIgnore_4b570d690c92236829b8974bae01fe3e() {
        assertEvalError("{ b<-3:5; dim(b) <- c(1,3) ; b[[0]] <- NULL ; b }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOtherIgnore_fe5461bdd4035e24804d4c684b9bb20f() {
        assertEvalError("{ x <- integer() ; x[[NA]] <- NULL ; x }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOtherIgnore_f09061c93f11ca4a2ec5ecd4f85f7548() {
        assertEvalError("{ x <- c(1) ; x[[NA]] <- NULL ; x }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOtherIgnore_4eea10efa7dfc459fce3420e5cf8d9fc() {
        assertEvalError("{ x <- c(1,2) ; x[[NA]] <- NULL ; x }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOtherIgnore_ad453b3eec6a2d91d42ea4c78a0c9356() {
        assertEvalError("{ x <- c(1,2,3) ; x[[NA]] <- NULL ; x }");
    }

    @Test
    public void TestSimpleVectors_testMoreVectorsOtherIgnore_d5bbba1f1bb5b771dbc80175679415c5() {
        assertEvalError("{ x<-c(1,2,3,4); dim(x)<-c(2,2); x[[1]]<-NULL; x }");
    }

    @Test
    public void TestSimpleVectors_testMultiDimScalarIndex_e271a023f22f29d8c4fce3e063eff2ed() {
        assertEval("{ x<-1:8; dim(x)<-c(2,2,2); dim(x[1,0,]) }");
    }

    @Test
    public void TestSimpleVectors_testMultiDimScalarIndex_a2f6edb8e0d5c28047cf3bc9d5248153() {
        assertEval("{ x<-1:8; dim(x)<-c(2,2,2); dim(x[,0,]) }");
    }

    @Test
    public void TestSimpleVectors_testMultiDimScalarIndex_ab373a6624fc24e51f8841e0bb6ad24e() {
        assertEval("{ x<-1:8; dim(x)<-c(2,2,2); x[-1,0,] }");
    }

    @Test
    public void TestSimpleVectors_testMultiDimScalarIndex_3b7826a50f5dbda64f3f83e1ccd997d1() {
        assertEval("{ x<-1:8; dim(x)<-c(2,2,2); x[-1,-1, 0] }");
    }

    @Test
    public void TestSimpleVectors_testMultiDimScalarIndex_a60e4b233bad14985256417c6e26b137() {
        assertEval("{ x<-1:8; dim(x)<-c(2,2,2); dim(x[0,2,0]) }");
    }

    @Test
    public void TestSimpleVectors_testMultiDimScalarIndex_5014b99635cd3154e0582da781d7b010() {
        assertEval("{ x<-1:8; dim(x)<-c(2,2,2); dim(x[0,-1,0]) }");
    }

    @Test
    public void TestSimpleVectors_testMultiDimScalarIndex_116b4c2b304c439efeb7ba43dcc2d63a() {
        assertEvalError("{ x<-1:8; dim(x)<-c(2,2,2); dim(x[0,3,0]) }");
    }

    @Test
    public void TestSimpleVectors_testNAIndex_88bdd0aba70235fdd509aa36d30468a8() {
        assertEval("{ x<-c(1,2,3,4); dim(x)<-c(2,2); x[NA] }");
    }

    @Test
    public void TestSimpleVectors_testNAIndex_4bd040fabab030cabd42d57c3e8596a0() {
        assertEval("{ x<-list(1,2,3,4); dim(x)<-c(2,2); x[NA] }");
    }

    @Test
    public void TestSimpleVectors_testNAIndex_98e484c24bacfdf3962fe27ad98eaeca() {
        assertEval("{ x<-list(1,2,3,4); dim(x)<-c(2,2); x[[NA]] }");
    }

    @Test
    public void TestSimpleVectors_testNAIndex_dc6ff4b4c230e0eca9fec694745c704e() {
        assertEval("{ x<-c(1,2,3,4); dim(x)<-c(2,2); x[NA, NA] }");
    }

    @Test
    public void TestSimpleVectors_testNAIndex_c8bc2526a6b211e4b98a88db1b41c44c() {
        assertEval("{ x<-c(1,2,3,4); dim(x)<-c(2,2); x[1, NA] }");
    }

    @Test
    public void TestSimpleVectors_testNAIndex_be9619338d494975ec0c3c8edff1e639() {
        assertEval("{ x<-c(1,2,3,4); dim(x)<-c(2,2); x[NA, 1] }");
    }

    @Test
    public void TestSimpleVectors_testNAIndex_649f7174126e1d84f6e3c7a130eb84ad() {
        assertEval("{ x<-c(1,2,3,4); dim(x)<-c(2,2); x[[NA, NA]] }");
    }

    @Test
    public void TestSimpleVectors_testNAIndex_bf042c216dafa00a1b00f5dfd15f9440() {
        assertEval("{ x<-c(1,2,3,4); dim(x)<-c(2,2); x[[1, NA]] }");
    }

    @Test
    public void TestSimpleVectors_testNAIndex_8c1e37c45a928881fc33c003d854c59f() {
        assertEval("{ x<-c(1,2,3,4); dim(x)<-c(2,2); x[[NA, 1]] }");
    }

    @Test
    public void TestSimpleVectors_testNAIndex_463aafb1a140d36408d75ed2cb102b27() {
        assertEval("{ x<-list(1,2,3,4); dim(x)<-c(2,2); x[NA, NA] }");
    }

    @Test
    public void TestSimpleVectors_testNAIndex_42a3021c2f2e66e612baceca1683c729() {
        assertEval("{ x<-list(1,2,3,4); dim(x)<-c(2,2); x[1, NA] }");
    }

    @Test
    public void TestSimpleVectors_testNAIndex_4bb849a6b461652f0efbead88ff515c1() {
        assertEval("{ x<-list(1,2,3,4); dim(x)<-c(2,2); x[NA, 1] }");
    }

    @Test
    public void TestSimpleVectors_testNAIndex_5c99ebde54a21079e792820b45033d0f() {
        assertEval("{ x<-list(1,2,3,4); dim(x)<-c(2,2); x[[NA, NA]] }");
    }

    @Test
    public void TestSimpleVectors_testNAIndex_4a7808e00db6310d0e78bd963f064700() {
        assertEval("{ x<-list(1,2,3,4); dim(x)<-c(2,2); x[[1, NA]] }");
    }

    @Test
    public void TestSimpleVectors_testNAIndex_3c57246a343459dfc517cfec1e4101fc() {
        assertEval("{ x<-list(1,2,3,4); dim(x)<-c(2,2); x[[NA, 1]] }");
    }

    @Test
    public void TestSimpleVectors_testNAIndex_88040eeee8e4bee69800daf1a0c740e1() {
        assertEval("{ x<-c(1,2,3,4); dim(x)<-c(2,2); x[NA]<-7; x }");
    }

    @Test
    public void TestSimpleVectors_testNAIndex_5bb56005dc3807414ba4e2c0ff77a1c6() {
        assertEval("{ x<-list(1,2,3,4); dim(x)<-c(2,2); x[NA]<-7; x }");
    }

    @Test
    public void TestSimpleVectors_testNAIndex_f6e69b3a69da199c4199f7a28b5b8ef5() {
        assertEval("{ x<-c(1,2,3,4); dim(x)<-c(2,2); x[NA, NA]<-7; x }");
    }

    @Test
    public void TestSimpleVectors_testNAIndex_44fd0d635d95786c5f413ab73386d718() {
        assertEval("{ x<-c(1,2,3,4); dim(x)<-c(2,2); x[1, NA]<-7; x }");
    }

    @Test
    public void TestSimpleVectors_testNAIndex_94e8c398203a3967c9d5a4a3f13d7125() {
        assertEval("{ x<-c(1,2,3,4); dim(x)<-c(2,2); x[NA, 1]<-7; x }");
    }

    @Test
    public void TestSimpleVectors_testNAIndex_4b43b790bcc7230c2ccfd08e2cf245ab() {
        assertEval("{ x<-list(1,2,3,4); dim(x)<-c(2,2); x[NA, NA]<-7; x }");
    }

    @Test
    public void TestSimpleVectors_testNAIndex_db1a55a57d305b03b4d5266407eeb7df() {
        assertEval("{ x<-list(1,2,3,4); dim(x)<-c(2,2); x[1, NA]<-7; x }");
    }

    @Test
    public void TestSimpleVectors_testNAIndex_355f8f8cfe3e97f12f1af848759f6161() {
        assertEval("{ x<-list(1,2,3,4); dim(x)<-c(2,2); x[NA, 1]<-7; x }");
    }

    @Test
    public void TestSimpleVectors_testNAIndex_af68885e1502ebfbcac7065f69bceedc() {
        assertEvalError("{ x<-c(1,2,3,4); dim(x)<-c(2,2); x[[NA]] }");
    }

    @Test
    public void TestSimpleVectors_testNAIndex_65b4ffa267e203e075e146250466fa85() {
        assertEvalError("{ x<-c(1,2,3,4); dim(x)<-c(2,2); x[[NA]]<-7; x }");
    }

    @Test
    public void TestSimpleVectors_testNAIndex_ae5be5a43350faac41d14329de8bb637() {
        assertEvalError("{ x<-c(1,2,3,4); dim(x)<-c(2,2); x[NA]<-c(7,42); x }");
    }

    @Test
    public void TestSimpleVectors_testNAIndex_99faa119cbbc7399131787b31939b3cd() {
        assertEvalError("{ x<-c(1,2,3,4); dim(x)<-c(2,2); x[[NA]]<-c(7, 42); x }");
    }

    @Test
    public void TestSimpleVectors_testNAIndex_71fc6410bcd22b879035fa6dc99a1ca4() {
        assertEvalError("{ x<-c(1,2,3,4); dim(x)<-c(2,2); x[NA]<-c(7, 42, 1); x }");
    }

    @Test
    public void TestSimpleVectors_testNAIndex_d310fb001637b82d27a15fa53d44cb40() {
        assertEvalError("{ x<-c(1,2,3,4); dim(x)<-c(2,2); x[[NA]]<-c(7, 42, 1); x }");
    }

    @Test
    public void TestSimpleVectors_testNAIndex_420d5a8458aff14012bd745527efb221() {
        assertEvalError("{ x<-list(1,2,3,4); dim(x)<-c(2,2); x[[NA]]<-7; x }");
    }

    @Test
    public void TestSimpleVectors_testNAIndex_a4d4206b87450d745a4aec39fa55f58c() {
        assertEvalError("{ x<-list(1,2,3,4); dim(x)<-c(2,2); x[NA]<-c(7,42); x }");
    }

    @Test
    public void TestSimpleVectors_testNAIndex_669f12e08ce2551a35fe68a07d210d78() {
        assertEvalError("{ x<-list(1,2,3,4); dim(x)<-c(2,2); x[[NA]]<-c(7, 42); x }");
    }

    @Test
    public void TestSimpleVectors_testNAIndex_ab4e4df539b94132b248f86c1804c3cc() {
        assertEvalError("{ x<-list(1,2,3,4); dim(x)<-c(2,2); x[NA]<-c(7, 42, 1); x }");
    }

    @Test
    public void TestSimpleVectors_testNAIndex_009f1c486f3566bac6a793a83a32d3bd() {
        assertEvalError("{ x<-list(1,2,3,4); dim(x)<-c(2,2); x[[NA]]<-c(7, 42, 1); x }");
    }

    @Test
    public void TestSimpleVectors_testNAIndex_a707e8901efdd62ad28ac35f9dbdf900() {
        assertEvalError("{ x<-c(1,2,3,4); dim(x)<-c(2,2); x[NA, NA]<-c(7, 42); x }");
    }

    @Test
    public void TestSimpleVectors_testNAIndex_08b1811917e507fa97b3b0804c5f6fff() {
        assertEvalError("{ x<-c(1,2,3,4); dim(x)<-c(2,2); x[1, NA]<-c(7, 42); x }");
    }

    @Test
    public void TestSimpleVectors_testNAIndex_27c99e25445b65fc972f1747db49b02d() {
        assertEvalError("{ x<-c(1,2,3,4); dim(x)<-c(2,2); x[NA, 1]<-c(7, 42); x }");
    }

    @Test
    public void TestSimpleVectors_testNAIndex_845ed8e3e168f535edd6da1ec7986f9e() {
        assertEvalError("{ x<-c(1,2,3,4); dim(x)<-c(2,2); x[[NA, NA]]<-7; x }");
    }

    @Test
    public void TestSimpleVectors_testNAIndex_35945265760e132e37037a2cf03a6c3c() {
        assertEvalError("{ x<-c(1,2,3,4); dim(x)<-c(2,2); x[[1, NA]]<-7; x }");
    }

    @Test
    public void TestSimpleVectors_testNAIndex_e808a1f52d68577b8b4e1cd1b8c4ca20() {
        assertEvalError("{ x<-c(1,2,3,4); dim(x)<-c(2,2); x[[NA, 1]]<-7; x }");
    }

    @Test
    public void TestSimpleVectors_testNAIndex_10a2e68f826deb0c600e1d9008a2e7af() {
        assertEvalError("{ x<-c(1,2,3,4); dim(x)<-c(2,2); x[[NA, NA]]<-c(7, 42); x }");
    }

    @Test
    public void TestSimpleVectors_testNAIndex_30877ff9ad2e8932a7e9fa0c8bce1bdd() {
        assertEvalError("{ x<-c(1,2,3,4); dim(x)<-c(2,2); x[[1, NA]]<-c(7, 42); x }");
    }

    @Test
    public void TestSimpleVectors_testNAIndex_e5a8fb94d20089a554162e5d81ccf498() {
        assertEvalError("{ x<-c(1,2,3,4); dim(x)<-c(2,2); x[[NA, 1]]<-c(7, 42); x }");
    }

    @Test
    public void TestSimpleVectors_testNAIndex_ed92b3caefbef0f121777794669ae00d() {
        assertEvalError("{ x<-c(1,2,3,4); dim(x)<-c(2,2); x[[NA, NA]]<-c(7, 42, 1); x }");
    }

    @Test
    public void TestSimpleVectors_testNAIndex_d8e39a930b8aa9301fe9666e637d98c7() {
        assertEvalError("{ x<-c(1,2,3,4); dim(x)<-c(2,2); x[[1, NA]]<-c(7, 42, 1); x }");
    }

    @Test
    public void TestSimpleVectors_testNAIndex_4fba73c61f6a5f54eaf026bf14e82e2b() {
        assertEvalError("{ x<-c(1,2,3,4); dim(x)<-c(2,2); x[[NA, 1]]<-c(7, 42, 1); x }");
    }

    @Test
    public void TestSimpleVectors_testNAIndex_4da5935d823c4867eee597685afe49a7() {
        assertEvalError("{ x<-list(1,2,3,4); dim(x)<-c(2,2); x[NA, NA]<-c(7, 42); x }");
    }

    @Test
    public void TestSimpleVectors_testNAIndex_4a85dd2028ad9849d3fe8fe38a6d0431() {
        assertEvalError("{ x<-list(1,2,3,4); dim(x)<-c(2,2); x[1, NA]<-c(7, 42); x }");
    }

    @Test
    public void TestSimpleVectors_testNAIndex_bac9d76d02abd665847316b7784ca495() {
        assertEvalError("{ x<-list(1,2,3,4); dim(x)<-c(2,2); x[NA, 1]<-c(7, 42); x }");
    }

    @Test
    public void TestSimpleVectors_testNAIndex_a159c2c54dda5d1b9392ae6565c396ba() {
        assertEvalError("{ x<-list(1,2,3,4); dim(x)<-c(2,2); x[NA, NA]<-c(7, 42, 1); x }");
    }

    @Test
    public void TestSimpleVectors_testNAIndex_1e931defd7f89cb92497ddd3dab69dbe() {
        assertEvalError("{ x<-list(1,2,3,4); dim(x)<-c(2,2); x[1, NA]<-c(7, 42, 1); x }");
    }

    @Test
    public void TestSimpleVectors_testNAIndex_df0b69a4383c5263ac2aefa0c7548086() {
        assertEvalError("{ x<-list(1,2,3,4); dim(x)<-c(2,2); x[NA, 1]<-c(7, 42, 1); x }");
    }

    @Test
    public void TestSimpleVectors_testNAIndex_183e60afce2f03d6679ed016cd821a12() {
        assertEvalError("{ x<-list(1,2,3,4); dim(x)<-c(2,2); x[[NA, NA]]<-7; x }");
    }

    @Test
    public void TestSimpleVectors_testNAIndex_a75aae871e4d604148627e9af510f148() {
        assertEvalError("{ x<-list(1,2,3,4); dim(x)<-c(2,2); x[[1, NA]]<-7; x }");
    }

    @Test
    public void TestSimpleVectors_testNAIndex_f037c13cd4c259bcaca86ba4b8813961() {
        assertEvalError("{ x<-list(1,2,3,4); dim(x)<-c(2,2); x[[NA, 1]]<-7; x }");
    }

    @Test
    public void TestSimpleVectors_testNAIndex_476d3491607676520619a031d8362964() {
        assertEvalError("{ x<-list(1,2,3,4); dim(x)<-c(2,2); x[[NA, NA]]<-c(7, 42); x }");
    }

    @Test
    public void TestSimpleVectors_testNAIndex_0c2d756e5c21dafb303812fd75c5f762() {
        assertEvalError("{ x<-list(1,2,3,4); dim(x)<-c(2,2); x[[1, NA]]<-c(7, 42); x }");
    }

    @Test
    public void TestSimpleVectors_testNAIndex_762158160086b86cfc2b2b5ec27367df() {
        assertEvalError("{ x<-list(1,2,3,4); dim(x)<-c(2,2); x[[NA, 1]]<-c(7, 42); x }");
    }

    @Test
    public void TestSimpleVectors_testNAIndex_f2dc5f70329a5146cf11d544b31ed567() {
        assertEvalError("{ x<-list(1,2,3,4); dim(x)<-c(2,2); x[[NA, NA]]<-c(7, 42, 1); x }");
    }

    @Test
    public void TestSimpleVectors_testNAIndex_ff27c0f64271de055e0be419558958d4() {
        assertEvalError("{ x<-list(1,2,3,4); dim(x)<-c(2,2); x[[1, NA]]<-c(7, 42, 1); x }");
    }

    @Test
    public void TestSimpleVectors_testNAIndex_f363b0173ad1642aed3ebaa183130944() {
        assertEvalError("{ x<-list(1,2,3,4); dim(x)<-c(2,2); x[[NA, 1]]<-c(7, 42, 1); x }");
    }

    @Test
    public void TestSimpleVectors_testPrint_ac880bff260f234821af9ee036453e82() {
        assertEval("{ x<-1:8; dim(x)<-c(2, 4); x }");
    }

    @Test
    public void TestSimpleVectors_testPrint_2353b26f5025b3b887c97eb5ea65e808() {
        assertEval("{ x<-c(1,2); y<-list(x, 1, 2, 3); dim(y)<-c(2, 2); y }");
    }

    @Test
    public void TestSimpleVectors_testPrint_34a2f0bd885192caa54b4be1b01723ec() {
        assertEval("{ x<-integer(0); y<-list(x, 1, 2, 3); dim(y)<-c(2, 2); y }");
    }

    @Test
    public void TestSimpleVectors_testPrint_bfbd6edbd45d39e41eaf37228d3b3689() {
        assertEval("{ x<-character(0); y<-list(x, 1+1i, 2+2i, 3+3i); dim(y)<-c(2, 2); y }");
    }

    @Test
    public void TestSimpleVectors_testPrint_f2b9166399f84a17b55468507202f20b() {
        assertEval("{ x<-list(1,2,3,4); dim(x)<-c(2, 2); y<-list(x, 1, 2, 3); dim(y)<-c(2, 2); y }");
    }

    @Test
    public void TestSimpleVectors_testPrint_3c7a1a57f283a35aabc3e72823f6d702() {
        assertEval("{ z<-list(1,2,3,4); dim(z)<-c(2,2); x<-list(z,2,3,42); dim(x)<-c(2, 2); y<-list(x, 1, 2, 3); dim(y)<-c(2, 2); y }");
    }

    @Test
    public void TestSimpleVectors_testPrint_9a48692a3c1cb930aaa111a6670344d0() {
        assertEval("{ x<-1:8; dim(x)<-c(2, 4); toString(x) }");
    }

    @Test
    public void TestSimpleVectors_testPrint_9fa337312d75fdd276af0a5c0c591b91() {
        assertEval("{ x<-list(1, 2, 3, 4); dim(x)<-c(2, 2); toString(x) }");
    }

    @Test
    public void TestSimpleVectors_testPrint_c227b9929334bb2fd8e85a7fc27a0a62() {
        assertEval("{ x<-1:8; dim(x)<-c(2,2,2); x }");
    }

    @Test
    public void TestSimpleVectors_testPrint_cbf1decbcd38dea9aaf593c96b7ddec9() {
        assertEval("{ x<-list(1,2,3,4,5,6,7,8); dim(x)<-c(2,2,2); x }");
    }

    @Test
    public void TestSimpleVectors_testPrint_652efb0e3d197644eaf862f8a8358274() {
        assertEval("{ x<-1:16; dim(x)<-c(2,2,2,2); x }");
    }

    @Test
    public void TestSimpleVectors_testPrint_33625f228f215e90dca785f18ae6d1db() {
        assertEval("{ x<-1:32; dim(x)<-c(2,2,2,2,2); x }");
    }

    @Test
    public void TestSimpleVectors_testPrint_17457869fdb86ec6e041e20ba496a7c2() {
        assertEval("{ x<-1:64; dim(x)<-c(2,2,2,2,2,2); x }");
    }

    @Test
    public void TestSimpleVectors_testPrint_c7ff2bd7fba8e216c7350673ace9901a() {
        assertEval("{ x<-1:8; dim(x)<-c(2,2,2); dimnames(x)<-list(c(101, 102), NULL, NULL); x }");
    }

    @Test
    public void TestSimpleVectors_testPrint_0b4cdccccc0a6a09fb8816311a87493b() {
        assertEval("{ x<-1:8; dim(x)<-c(2,2,2); dimnames(x)<-list(c(101, 102), c(103, 104), NULL); x }");
    }

    @Test
    public void TestSimpleVectors_testPrint_88d879760a1d4d6d70c163f6b101551b() {
        assertEval("{ x<-1:8; dim(x)<-c(2,2,2); dimnames(x)<-list(c(101, 102), c(103, 104), c(105, 106)); x }");
    }

    @Test
    public void TestSimpleVectors_testPrint_8e23d61b4a3ecc43d7a3b32a02a365e7() {
        assertEval("{ x<-1:8; dim(x)<-c(2,2,2); dimnames(x)<-list(c(101, 102), c(105, 106)); x }");
    }

    @Test
    public void TestSimpleVectors_testPrint_e5a4975280a8bb37b70d51826cfb83e2() {
        assertEval("{ x<-1:8; dim(x)<-c(2,2,2); dimnames(x)<-list(c(101, 102), NULL, c(105, 106)); x }");
    }

    @Test
    public void TestSimpleVectors_testPrint_01b5961257e84363c7f3e1e197091c4d() {
        assertEval("{ x<-1:4; dim(x)<-c(2,1,2); dimnames(x)<-list(c(\"a\", \"b\"), \"c\", c(\"d\", \"e\")); x }");
    }

    @Test
    public void TestSimpleVectors_testPrint_6c779ed900c8e2abe428d228b96e9319() {
        assertEval("{ x<-101:108; dim(x)<-c(2,2,2); dimnames(x)<-list(c(1, 2), c(3, 4), c(5, 6)); x }");
    }

    @Test
    public void TestSimpleVectors_testPrint_48685b68f49fd142b40c17c34b686a03() {
        assertEval("{ x<-10001:10008; dim(x)<-c(2,2,2); x }");
    }

    @Test
    public void TestSimpleVectors_testPrint_efbf44179f1ba5857a1edbf332e10fb5() {
        assertEval("{ x<-c(1:2, 100003:100004,10005:10008); dim(x)<-c(2,2,2); x }");
    }

    @Test
    public void TestSimpleVectors_testPrint_30107f4e77c1473f22795431639c8c2c() {
        assertEval("{ x<-c(1:4,10005:10008); dim(x)<-c(2,2,2); x }");
    }

    @Test
    public void TestSimpleVectors_testPrint_1b6f7c0c459865d98873e1e045c36e0b() {
        assertEval("{ x<-1:16; dim(x)<-c(2,4,2); x }");
    }

    @Test
    public void TestSimpleVectors_testPrint_c3b8c4e50d796d4d3a3e54bbd92c0b13() {
        assertEval("{ x<-1:32; dim(x)<-c(4,2,2,2); x }");
    }

    @Test
    public void TestSimpleVectors_testPrint_6973675d621590a897682c6943e0e97f() {
        assertEval("{ x<-1:32; dim(x)<-c(2,4,2,2); x }");
    }

    @Test
    public void TestSimpleVectors_testPrint_1ac72e8058e1e4e3cbcc3c819e1cea22() {
        assertEval("{ x<-1:32; dim(x)<-c(2,2,4,2); x }");
    }

    @Test
    public void TestSimpleVectors_testPrint_e6805e12f0418d16fbbacf74b58a4667() {
        assertEval("{ x<-1:32; dim(x)<-c(2,2,2,4); x }");
    }

    @Test
    public void TestSimpleVectors_testPrint_3e24af09eb7d8f00b2e3972fdde9b773() {
        assertEval("{ x<-1:64; dim(x)<-c(4,4,4); x }");
    }

    @Test
    public void TestSimpleVectors_testPrint_f8456a1d3fda495af109147a4dd0cef2() {
        assertEval("{ x<-1:256; dim(x)<-c(4,4,4,4); x }");
    }

    @Test
    public void TestSimpleVectors_testPrint_38265eb3763a6030a7d2c9f0a0b051dd() {
        assertEval("{ x<-1:64; dim(x)<-c(2,2,2,4,2); x }");
    }

    @Test
    public void TestSimpleVectors_testPrint_3bed41bb0850feb512afc7fc8c6005a1() {
        assertEval("{ x<-1:4; dim(x)<-c(2,2,1); x }");
    }

    @Test
    public void TestSimpleVectors_testPrint_cabfc5cdcbfb67d9ec4d395095ee8635() {
        assertEval("{ x<-1:4; dim(x)<-c(2,2,1,1); x }");
    }

    @Test
    public void TestSimpleVectors_testPrint_d171a6dc0219f3a8476f11bc9ec328ec() {
        assertEval("{ x<-1:4; dim(x)<-c(1,2,2); x }");
    }

    @Test
    public void TestSimpleVectors_testPrint_f3603e1eb847493e36900b208b7c1631() {
        assertEval("{ x<-1:4; dim(x)<-c(2,1,2); x }");
    }

    @Test
    public void TestSimpleVectors_testPrint_321ba23e5d16334b4702673e50a96713() {
        assertEval("{ x<-integer(0); dim(x)<-c(1, 0); x }");
    }

    @Test
    public void TestSimpleVectors_testPrint_93df6beaff71e9a0bb81b9e6bcbfb3a7() {
        assertEval("{ x<-integer(0); dim(x)<-c(0, 1); x }");
    }

    @Test
    public void TestSimpleVectors_testPrint_ec822f9f915e8646ffee62f21606488c() {
        assertEval("{ x<-integer(0); dim(x)<-c(0, 3); x }");
    }

    @Test
    public void TestSimpleVectors_testPrint_a80b8568c326ac40a73df657af0f8294() {
        assertEval("{ x<-integer(0); dim(x)<-c(3, 0); x }");
    }

    @Test
    public void TestSimpleVectors_testPrint_0668e8929bc74bb62489658186b50c3d() {
        assertEval("{ x<-integer(0); dim(x)<-c(0, 0); x }");
    }

    @Test
    public void TestSimpleVectors_testPrint_0e433b2de3c0036902a31d07cafbcf7f() {
        assertEval("{ x<-integer(0); dim(x)<-c(1, 0, 2); x }");
    }

    @Test
    public void TestSimpleVectors_testPrint_b1ef0cd726b2e1bf660a0ac0b22d286a() {
        assertEval("{ x<-integer(0); dim(x)<-c(1, 0, 0); x }");
    }

    @Test
    public void TestSimpleVectors_testPrint_f243da63b36970c02751fdd43edddaf2() {
        assertEval("{ x<-integer(0); dim(x)<-c(1, 0, 0, 2); x }");
    }

    @Test
    public void TestSimpleVectors_testPrint_a63d26bd9cd67a7fdda67ceb13c3be23() {
        assertEval("{ x<-integer(0); dim(x)<-c(1, 0, 2, 0, 2); x }");
    }

    @Test
    public void TestSimpleVectors_testPrint_d8ce68a23a7e5c9b8ce56d5d227268c7() {
        assertEval("{ x<-integer(0); dim(x)<-c(0, 0, 2, 2, 2); x }");
    }

    @Test
    public void TestSimpleVectors_testPrint_b2dcfc5e9ef8a00fdec843dcca184100() {
        assertEval("{ x<-integer(0); dim(x)<-c(1, 0); dimnames(x)<-list(\"a\"); x }");
    }

    @Test
    public void TestSimpleVectors_testPrint_ad0b42209217f883b4bb99a32d80291c() {
        assertEval("{ x<-integer(0); dim(x)<-c(0, 1); dimnames(x)<-list(NULL, \"a\"); x }");
    }

    @Test
    public void TestSimpleVectors_testPrint_ad373fb8a0cb534670056e6cfff084c3() {
        assertEval("{ x<-integer(0); dim(x)<-c(1, 0, 2, 2, 2); dimnames(x)<-list(\"a\", NULL, c(\"b\", \"c\"), c(\"d\", \"e\"), c(\"f\", \"g\")); x }");
    }

    @Test
    public void TestSimpleVectors_testPrint_26c4856d8152edfdee732b70242e88a8() {
        assertEval("{ x<-integer(0); dim(x)<-c(0, 4); dimnames(x)<-list(NULL, c(\"a\", \"bbbbbbbbbbbb\", \"c\", \"d\")); x }");
    }

    @Test
    public void TestSimpleVectors_testRawIndex_56015d7e5db9ea79ba00565c74ba9e61() {
        assertEvalError("{ x<-c(1,2,3,4); x[[as.raw(1)]]<-NULL }");
    }

    @Test
    public void TestSimpleVectors_testRawIndex_31a7d00e5fbca894f44096cd2ef1fd7a() {
        assertEvalError("{ x<-c(1,2,3,4); x[[as.raw(1)]]<-integer() }");
    }

    @Test
    public void TestSimpleVectors_testRawIndex_94eca145a76900bfca147708ee56d326() {
        assertEvalError("{ x<-c(1,2,3,4); x[[as.raw(1)]]<-c(1) }");
    }

    @Test
    public void TestSimpleVectors_testRawIndex_6415c49d64ab2b5e5d87106b2ac2012e() {
        assertEvalError("{ x<-c(1,2,3,4); x[[as.raw(1)]]<-c(1,2) }");
    }

    @Test
    public void TestSimpleVectors_testRawIndex_e1c20720f1f67b5d5c8bfe73241b1cc6() {
        assertEvalError("{ x<-c(1,2,3,4); x[[as.raw(1)]]<-c(1,2,3) }");
    }

    @Test
    public void TestSimpleVectors_testRawIndex_09307db9933d177f0d8a6d08963a7eb0() {
        assertEvalError("{ x<-c(1,2,3,4); x[as.raw(1)]<-NULL }");
    }

    @Test
    public void TestSimpleVectors_testRawIndex_cd083dc6b55bdfada462ba12362642af() {
        assertEvalError("{ x<-c(1,2,3,4); x[as.raw(1)]<-integer() }");
    }

    @Test
    public void TestSimpleVectors_testRawIndex_ca020137a9c55adda8557f974a5b7334() {
        assertEvalError("{ x<-c(1,2,3,4); x[as.raw(1)]<-c(1) }");
    }

    @Test
    public void TestSimpleVectors_testRawIndex_6021df17bfe80c01d5bb31c5aae6cdb9() {
        assertEvalError("{ x<-c(1,2,3,4); x[as.raw(1)]<-c(1,2) }");
    }

    @Test
    public void TestSimpleVectors_testRawIndex_edc0145fd6f48d5360c39f7ca6a9ab75() {
        assertEvalError("{ x<-c(1,2,3,4); x[as.raw(1)]<-c(1,2,3) }");
    }

    @Test
    public void TestSimpleVectors_testRawIndex_bca1724c712508f88f83bf6d4187daee() {
        assertEvalError("{ x<-c(1,2,3,4); dim(x)<-c(2,2); x[[as.raw(1), 1]]<-NULL }");
    }

    @Test
    public void TestSimpleVectors_testRawIndex_46dca8a606a60d006561b3be80897666() {
        assertEvalError("{ x<-c(1,2,3,4); dim(x)<-c(2,2); x[[as.raw(1), 1]]<-integer() }");
    }

    @Test
    public void TestSimpleVectors_testRawIndex_b8def2f548261cd8846e5fd0b6f75f64() {
        assertEvalError("{ x<-c(1,2,3,4); dim(x)<-c(2,2); x[[as.raw(1), 1]]<-7 }");
    }

    @Test
    public void TestSimpleVectors_testRawIndex_06b655e90d18fae055bcde9e519ef0d4() {
        assertEvalError("{ x<-c(1,2,3,4); dim(x)<-c(2,2); x[[as.raw(1), 1]]<-c(7,42) }");
    }

    @Test
    public void TestSimpleVectors_testRawIndex_03dcdf09fbb9a072ae8ff13d531bd792() {
        assertEvalError("{ x<-c(1,2,3,4); dim(x)<-c(2,2); x[as.raw(1), 1]<-NULL }");
    }

    @Test
    public void TestSimpleVectors_testRawIndex_cf04268d305540a545c49ecf3dbf9d2b() {
        assertEvalError("{ x<-c(1,2,3,4); dim(x)<-c(2,2); x[as.raw(1), 1]<-integer() }");
    }

    @Test
    public void TestSimpleVectors_testRawIndex_6ec24131ae122c869cc540836c1ae072() {
        assertEvalError("{ x<-c(1,2,3,4); dim(x)<-c(2,2); x[as.raw(1), 1]<-7 }");
    }

    @Test
    public void TestSimpleVectors_testRawIndex_325ef28a15192581f5c07bb95eb4abb8() {
        assertEvalError("{ x<-c(1,2,3,4); dim(x)<-c(2,2); x[as.raw(1), 1]<-c(7,42) }");
    }

    @Test
    public void TestSimpleVectors_testRawIndex_c84bd1ebd989fcb437f5939ed982943a() {
        assertEvalError("{ x<-list(1,2,3,4); x[[as.raw(1)]]<-NULL }");
    }

    @Test
    public void TestSimpleVectors_testRawIndex_a38b27a0e2b411826d35b8f8e6318272() {
        assertEvalError("{ x<-list(1,2,3,4); x[[as.raw(1)]]<-integer() }");
    }

    @Test
    public void TestSimpleVectors_testRawIndex_ba751d66d8c56e02da25ae78b31f9a2e() {
        assertEvalError("{ x<-list(1,2,3,4); x[[as.raw(1)]]<-c(1) }");
    }

    @Test
    public void TestSimpleVectors_testRawIndex_e5b1925a05bdac2865786cff65acaa90() {
        assertEvalError("{ x<-list(1,2,3,4); x[[as.raw(1)]]<-c(1,2) }");
    }

    @Test
    public void TestSimpleVectors_testRawIndex_5af361e92cb132d1a7ed9e21447a2f78() {
        assertEvalError("{ x<-list(1,2,3,4); x[[as.raw(1)]]<-c(1,2,3) }");
    }

    @Test
    public void TestSimpleVectors_testRawIndex_00ba5a51eb97d6fa540d6a8cb60dc66d() {
        assertEvalError("{ x<-list(1,2,3,4); x[as.raw(1)]<-NULL }");
    }

    @Test
    public void TestSimpleVectors_testRawIndex_0e4f83e08191f9c295aeb26359692ad0() {
        assertEvalError("{ x<-list(1,2,3,4); x[as.raw(1)]<-integer() }");
    }

    @Test
    public void TestSimpleVectors_testRawIndex_5e5e7de6a50cd2f733bda827f1226b23() {
        assertEvalError("{ x<-list(1,2,3,4); x[as.raw(1)]<-c(1) }");
    }

    @Test
    public void TestSimpleVectors_testRawIndex_830618155452aa717128938776c1fbf0() {
        assertEvalError("{ x<-list(1,2,3,4); x[as.raw(1)]<-c(1,2) }");
    }

    @Test
    public void TestSimpleVectors_testRawIndex_b9b456f619e8650700bbc4508c448a96() {
        assertEvalError("{ x<-list(1,2,3,4); x[as.raw(1)]<-c(1,2,3) }");
    }

    @Test
    public void TestSimpleVectors_testRawIndex_2030830bd507d4ea57fcfc3e662d10ea() {
        assertEvalError("{ x<-list(1,2,3,4); dim(x)<-c(2,2); x[[as.raw(1), 1]]<-NULL }");
    }

    @Test
    public void TestSimpleVectors_testRawIndex_05f29f0ba9791b744a49f35538f0ba61() {
        assertEvalError("{ x<-list(1,2,3,4); dim(x)<-c(2,2); x[[as.raw(1), 1]]<-integer() }");
    }

    @Test
    public void TestSimpleVectors_testRawIndex_1fedcf7c75a0a1edf85ca9bab4f3b90b() {
        assertEvalError("{ x<-list(1,2,3,4); dim(x)<-c(2,2); x[[as.raw(1), 1]]<-7 }");
    }

    @Test
    public void TestSimpleVectors_testRawIndex_338e6099652d449b58f0aced90c4b717() {
        assertEvalError("{ x<-list(1,2,3,4); dim(x)<-c(2,2); x[[as.raw(1), 1]]<-c(7,42) }");
    }

    @Test
    public void TestSimpleVectors_testRawIndex_9a82fee67cc276e7689ecdf9ad54bca1() {
        assertEvalError("{ x<-list(1,2,3,4); dim(x)<-c(2,2); x[as.raw(1), 1]<-NULL }");
    }

    @Test
    public void TestSimpleVectors_testRawIndex_43702e9cfda16ed6e768aff809c59719() {
        assertEvalError("{ x<-list(1,2,3,4); dim(x)<-c(2,2); x[as.raw(1), 1]<-integer() }");
    }

    @Test
    public void TestSimpleVectors_testRawIndex_665cd227aa7166cea62eabf1a75a0053() {
        assertEvalError("{ x<-list(1,2,3,4); dim(x)<-c(2,2); x[as.raw(1), 1]<-7 }");
    }

    @Test
    public void TestSimpleVectors_testRawIndex_93380e8a169f7145d2f15c685a4d6a92() {
        assertEvalError("{ x<-list(1,2,3,4); dim(x)<-c(2,2); x[as.raw(1), 1]<-c(7,42) }");
    }

    @Test
    public void TestSimpleVectors_testScalarDoubleAsVector_9acec1f4b64be4651d8f6034c161155e() {
        assertEval("{ x<-1; x[1L] }");
    }

    @Test
    public void TestSimpleVectors_testScalarDoubleAsVector_635c5063dc8660d2298231970fbed713() {
        assertEval("{ x<-1; x[0L] }");
    }

    @Test
    public void TestSimpleVectors_testScalarDoubleAsVector_21b031587b7ab0b2ed2b61959d0a1826() {
        assertEval("{ x<-1; x[2L] }");
    }

    @Test
    public void TestSimpleVectors_testScalarDoubleAsVector_ae22a68aa4d57a4bbabd4766671a846c() {
        assertEval("{ x<-1; x[-1L] }");
    }

    @Test
    public void TestSimpleVectors_testScalarDoubleAsVector_39bcf0b5d8f41037a855c8fec44b6b16() {
        assertEval("{ x<-1; x[-2L] }");
    }

    @Test
    public void TestSimpleVectors_testScalarDoubleAsVector_d5e8bebf56bee74425637af69273aab0() {
        assertEval("{ x<-1; x[TRUE] }");
    }

    @Test
    public void TestSimpleVectors_testScalarDoubleAsVector_3e06984f14f8b8f7c14725bee49dff46() {
        assertEval("{ x<-1; x[FALSE] }");
    }

    @Test
    public void TestSimpleVectors_testScalarDoubleAsVector_5061ec4e36179fcff31e62f8392979e9() {
        assertEval("{ x<-1; x[NA] }");
    }

    @Test
    public void TestSimpleVectors_testScalarDoubleIndexOnVector_ecd7c9ce7bd37b9ec57fe278a4650734() {
        assertEval("{ x<-c(1,2,3); x[1.1] }");
    }

    @Test
    public void TestSimpleVectors_testScalarDoubleIndexOnVector_f067fef618f8c78cd46223ffb7d8b153() {
        assertEval("{ x<-c(1,2,3); x[2.1] }");
    }

    @Test
    public void TestSimpleVectors_testScalarDoubleIndexOnVector_1996d36ecb97925e13a09f32c3b2c5b8() {
        assertEval("{ x<-c(1,2,3); x[3.1] }");
    }

    @Test
    public void TestSimpleVectors_testScalarDoubleIndexOnVector_f9ef4c254cf41225b518eb71d95aefce() {
        assertEval("{ x<-c(1L,2L,3L); x[1.1] }");
    }

    @Test
    public void TestSimpleVectors_testScalarDoubleIndexOnVector_1f561260223163d441744d1eca65c707() {
        assertEval("{ x<-c(1L,2L,3L); x[2.1] }");
    }

    @Test
    public void TestSimpleVectors_testScalarDoubleIndexOnVector_29a37c4cd9639a6c4406bd404db1e312() {
        assertEval("{ x<-c(1L,2L,3L); x[3.1] }");
    }

    @Test
    public void TestSimpleVectors_testScalarDoubleIndexOnVector_20bde038708c24440a16b851d1c47519() {
        assertEval("{ x<-1:3; x[1.1] }");
    }

    @Test
    public void TestSimpleVectors_testScalarDoubleIndexOnVector_a86c4a05cfab8b9b22b033e4c34bedb0() {
        assertEval("{ x<-1:3; x[2.1] }");
    }

    @Test
    public void TestSimpleVectors_testScalarDoubleIndexOnVector_49f286dc47ccf90e117bb4ff9adc3113() {
        assertEval("{ x<-1:3; x[3.1] }");
    }

    @Test
    public void TestSimpleVectors_testScalarDoubleIndexOnVector_9feec00670f96e13614acd7fff1c3cdc() {
        assertEval("{ x<-3:1; x[1.1] }");
    }

    @Test
    public void TestSimpleVectors_testScalarDoubleIndexOnVector_719f026d0185bacdf6a860df8d80cf07() {
        assertEval("{ x<-3:1; x[2.1] }");
    }

    @Test
    public void TestSimpleVectors_testScalarDoubleIndexOnVector_391ea7065057fb421adb200ef190bad5() {
        assertEval("{ x<-3:1; x[3.1] }");
    }

    @Test
    public void TestSimpleVectors_testScalarDoubleNegativeIndexOnVector_2832f2f47d06c8d21133ed6586418161() {
        assertEval("{ x<-c(1,2,3); x[-1.1] }");
    }

    @Test
    public void TestSimpleVectors_testScalarDoubleNegativeIndexOnVector_3a99cbd1cb38e509d752835a51a8c990() {
        assertEval("{ x<-c(1,2,3); x[-2.1] }");
    }

    @Test
    public void TestSimpleVectors_testScalarDoubleNegativeIndexOnVector_0dbeca2649fa8c7964b48ad61097bf25() {
        assertEval("{ x<-c(1L,2L,3L); x[-1.1] }");
    }

    @Test
    public void TestSimpleVectors_testScalarDoubleNegativeIndexOnVector_d72c20471a99f88cded2d0cc05461232() {
        assertEval("{ x<-c(1L,2L,3L); x[-2.1] }");
    }

    @Test
    public void TestSimpleVectors_testScalarDoubleNegativeIndexOnVector_a027a32be8ce0360fd9f3c47dbd5a737() {
        assertEval("{ x<-1:3; x[-1.1] }");
    }

    @Test
    public void TestSimpleVectors_testScalarDoubleNegativeIndexOnVector_72fe27930bcb517266ac57b826f39d3f() {
        assertEval("{ x<-1:3; x[-2.1] }");
    }

    @Test
    public void TestSimpleVectors_testScalarDoubleNegativeIndexOnVector_f34402d4b730f3b7deca9d3b511a348f() {
        assertEval("{ x<-c(1,2,3); x[-3.1] }");
    }

    @Test
    public void TestSimpleVectors_testScalarDoubleNegativeIndexOnVector_17da8d08be7a27df72cb116919fadf6c() {
        assertEval("{ x<-c(1L,2L,3L); x[-3.1] }");
    }

    @Test
    public void TestSimpleVectors_testScalarDoubleNegativeIndexOnVector_83e0ed4534446b2dd7c33513d9f3881a() {
        assertEval("{ x<-1:3; x[-3.1] }");
    }

    @Test
    public void TestSimpleVectors_testScalarIndex_43f52e129be75662fc39ffb1722d08fe() {
        assertEval("{ x <- c(a=1, b=2, c=3) ; x[[2]] }");
    }

    @Test
    public void TestSimpleVectors_testScalarIndex_2cc04adcf513ac5c67d258d8ff29be6e() {
        assertEval("{ x<-5:1 ; y <- 6L;  x[y] }");
    }

    @Test
    public void TestSimpleVectors_testScalarIndex_796557b3804f91e5c923ad74d3bac050() {
        assertEval("{ x<-5:1 ; y <- 2L;  x[[y]] }");
    }

    @Test
    public void TestSimpleVectors_testScalarIndex_13ddc436eddecadeccd03456e2fe2b30() {
        assertEval("{ x <- c(1,4) ; y <- -1L ; x[y] }");
    }

    @Test
    public void TestSimpleVectors_testScalarIndex_9174300cde75228773dadbeca24f5afb() {
        assertEval("{ x <- c(1,4) ; y <- 10L ; x[y] }");
    }

    @Test
    public void TestSimpleVectors_testScalarIndex_cab9e8d57efb4307f149584817c9dd2e() {
        assertEval("{ x <- c(1,4) ; y <- -1 ; x[y] }");
    }

    @Test
    public void TestSimpleVectors_testScalarIndex_028b2de6b6ef9736af601517476005ea() {
        assertEval("{ x <- c(1,4) ; y <- 10 ; x[y] }");
    }

    @Test
    public void TestSimpleVectors_testScalarIndex_cf2a63483a6199389e3c1c532f230daf() {
        assertEval("{ x <- 1:4 ; y <- -1 ; x[y] }");
    }

    @Test
    public void TestSimpleVectors_testScalarIndex_a9965767c78568255e78f55a9f5fba9c() {
        assertEval("{ x <- 1:4 ; y <- 10 ; x[y] }");
    }

    @Test
    public void TestSimpleVectors_testScalarIndex_e6079d71ec408f0249cebbc96ce7b8fc() {
        assertEval("{ x <- list(1,2,3,4) ; y <- 3 ; x[[y]] }");
    }

    @Test
    public void TestSimpleVectors_testScalarIndex_2a7e56049ad8a25299c6be4cd3b61b61() {
        assertEval("{ x <- c(as.raw(10), as.raw(11), as.raw(12)) ; x[-2] }");
    }

    @Test
    public void TestSimpleVectors_testScalarIndex_50f8d5d633f8f248cb1651a0304290ba() {
        assertEval("{ x<-as.list(5:1) ; y <- 2L;  x[[y]] }");
    }

    @Test
    public void TestSimpleVectors_testScalarIndex_942a9f166109273f1444551c2ae8ade1() {
        assertEval("{ x <- as.list(1:2) ; f <- function(i) { x[i] <- NULL ; x } ; f(1) ; f(NULL) }");
    }

    @Test
    public void TestSimpleVectors_testScalarIndex_432032e9368dd1cc8df079e75b453970() {
        assertEval("{ x<-c(TRUE,TRUE,FALSE); x[0-2] }");
    }

    @Test
    public void TestSimpleVectors_testScalarIndex_bdfb9982f2125589fde80bb9bf92be8b() {
        assertEval("{ x <- c(a=1, b=2, c=3) ; x[2] }");
    }

    @Test
    public void TestSimpleVectors_testScalarIndex_5a9f024ee5b02de5e8cb27fde97002b3() {
        assertEval("{ x <- c(a=\"A\", b=\"B\", c=\"C\") ; x[-2] }");
    }

    @Test
    public void TestSimpleVectors_testScalarIndex_3aa6b5291198d73ce11db7713ead3af6() {
        assertEval("{ x <- c(a=1+2i, b=2+3i, c=3) ; x[-2] }");
    }

    @Test
    public void TestSimpleVectors_testScalarIndex_8a8d2a503f522b4e5caaaae8f46dfc80() {
        assertEval("{ x <- c(a=1, b=2, c=3) ; x[-2] }");
    }

    @Test
    public void TestSimpleVectors_testScalarIndex_bec8cb40ff51054173db1fd6ac0d9b8f() {
        assertEval("{ x <- c(a=1L, b=2L, c=3L) ; x[-2] }");
    }

    @Test
    public void TestSimpleVectors_testScalarIndex_c66136c48e96dac7d20a2c3321cb1f5c() {
        assertEval("{ x <- c(a=TRUE, b=FALSE, c=NA) ; x[-2] }");
    }

    @Test
    public void TestSimpleVectors_testScalarIndex_d6bd87ca45c836c7514a6ce27e611d84() {
        assertEval("{ x <- c(a=as.raw(10), b=as.raw(11), c=as.raw(12)) ; x[-2] }");
    }

    @Test
    public void TestSimpleVectors_testScalarIndex_f189a29dfb77ee9077e643cc16e61823() {
        assertEval("{ x <- c(a=1L, b=2L, c=3L) ; x[0] }");
    }

    @Test
    public void TestSimpleVectors_testScalarIndex_5058b6240987ebc70b4cbca75923d5c6() {
        assertEval("{ x <- c(a=1L, b=2L, c=3L) ; x[10] }");
    }

    @Test
    public void TestSimpleVectors_testScalarIndex_570d8d05b4cc3299253475df80b8e95c() {
        assertEval("{ x <- c(a=TRUE, b=FALSE, c=NA) ; x[0] }");
    }

    @Test
    public void TestSimpleVectors_testScalarIndex_661f9176f30bb012e4f838c8e6b85af1() {
        assertEval("{ x <- c(TRUE, FALSE, NA) ; x[0] }");
    }

    @Test
    public void TestSimpleVectors_testScalarIndex_08cb14acafbcb589e6182b35681a3933() {
        assertEval("{ x <- list(1L, 2L, 3L) ; x[10] }");
    }

    @Test
    public void TestSimpleVectors_testScalarIndex_699296d14feb7eef24d9250d0ad1b78f() {
        assertEval("{ x <- list(a=1L, b=2L, c=3L) ; x[0] }");
    }

    @Test
    public void TestSimpleVectors_testScalarIndex_5364e294f38fde7c99eabcc1655f8066() {
        assertEval("{ x <- c(a=\"A\", b=\"B\", c=\"C\") ; x[10] }");
    }

    @Test
    public void TestSimpleVectors_testScalarIndex_d99732fcab7d1c4728968ffc7bde82f3() {
        assertEval("{ x <- c(a=\"A\", b=\"B\", c=\"C\") ; x[0] }");
    }

    @Test
    public void TestSimpleVectors_testScalarIndex_55c8d58d0fe2bbc46822b71af31b099b() {
        assertEval("{ x <- c(a=1+1i, b=2+2i, c=3+3i) ; x[10] }");
    }

    @Test
    public void TestSimpleVectors_testScalarIndex_8148cce9d18d5e93bcd2ec3586b0b25a() {
        assertEval("{ x <- c(a=1+1i, b=2+2i, c=3+3i) ; x[0] }");
    }

    @Test
    public void TestSimpleVectors_testScalarIndex_0e8d05b2b61989e0a3145800263b809f() {
        assertEval("{ x <- c(a=as.raw(10), b=as.raw(11), c=as.raw(12)) ; x[10] }");
    }

    @Test
    public void TestSimpleVectors_testScalarIndex_646d60d1e08899b093bf881cb8106719() {
        assertEval("{ x <- c(a=as.raw(10), b=as.raw(11), c=as.raw(12)) ; x[0] }");
    }

    @Test
    public void TestSimpleVectors_testScalarIndex_c5919f4596d35961d03d7da5169f43df() {
        assertEval("{ x <- c(a=1, b=2, c=3) ; x[10] }");
    }

    @Test
    public void TestSimpleVectors_testScalarIndex_f5cc3235b42a4f6759b44430a61bdc48() {
        assertEval("{ x <- c(a=1, b=2, c=3) ; x[0] }");
    }

    @Test
    public void TestSimpleVectors_testScalarIndex_438ac759f0ef9677e4325e53f2f88571() {
        assertEval("{ x <- c(a=1,b=2,c=3,d=4) ; x[\"b\"] }");
    }

    @Test
    public void TestSimpleVectors_testScalarIndex_3edfb967bbf1c6ed47f0470f63740ab0() {
        assertEval("{ x <- c(a=1,b=2,c=3,d=4) ; x[\"d\"] }");
    }

    @Test
    public void TestSimpleVectors_testScalarIndex_f4e366fb9b4d13fc763c576039600e5b() {
        assertEval("{ x <- 1 ; attr(x, \"hi\") <- 2; x[2] <- 2; attr(x, \"hi\") }");
    }

    @Test
    public void TestSimpleVectors_testScalarIndex_e99ef17d48648c7759a934be8425ab7f() {
        assertEval("{ x<-5:1 ; y <- -1L;  x[y] }");
    }

    @Test
    public void TestSimpleVectors_testScalarIndex_f46eea254cc6c35426449d4583dab803() {
        assertEval("{ x <- c(a=1,b=2) ; y <- 2L ; x[y] }");
    }

    @Test
    public void TestSimpleVectors_testScalarIndex_da2f6b7225ab17876651e3fb5da37be2() {
        assertEval("{ x <- c(a=1,b=2) ; y <- 2 ; x[y] }");
    }

    @Test
    public void TestSimpleVectors_testScalarIndex_ec2a04b9eb613d5cb3040e66734d161f() {
        assertEval("{ x <- list(1,2,3,4) ; y <- 3 ; x[y] }");
    }

    @Test
    public void TestSimpleVectors_testScalarIndex_bd897ddbbd064762af377c1bff05745b() {
        assertEval("{ x <- list(1,4) ; y <- -1 ; x[y] }");
    }

    @Test
    public void TestSimpleVectors_testScalarIndex_22dd8c09df9879fbc4d4a3742dd51571() {
        assertEval("{ x <- list(1,4) ; y <- 4 ; x[y] }");
    }

    @Test
    public void TestSimpleVectors_testScalarIndex_bece80ce29b2be5b8d8837537d10d7f5() {
        assertEval("{ x <- list(a=1,b=4) ; y <- 2 ; x[y] }");
    }

    @Test
    public void TestSimpleVectors_testScalarIndex_029ccf40f09e0e332c6db1681c979aae() {
        assertEval("{ f <- function(x,i) { x[i] } ; x <- c(a=1,b=2) ; f(x,\"a\") }");
    }

    @Test
    public void TestSimpleVectors_testScalarIndex_e44f7131ee8502655892ea1ca391f5ef() {
        assertEval("{ f <- function(x,i) { x[i] } ; x <- c(a=1,b=2) ; f(x,\"a\") ; f(x,2) }");
    }

    @Test
    public void TestSimpleVectors_testScalarIndex_84f499fe90cb617221f716b44d9ca0bb() {
        assertEval("{ f <- function(x,i) { x[[i]] } ; f(1:4, 2L) ; f(c(a=1), \"a\") ; f(list(1,2),TRUE) }");
    }

    @Test
    public void TestSimpleVectors_testScalarIndex_e181002aae9746a49ea6f292b0c67e12() {
        assertEval("{ f <- function(x,i) { x[i] } ; f(1:4, 2L) ; f(c(a=1), \"a\") ; f(list(), NA) }");
    }

    @Test
    public void TestSimpleVectors_testScalarIndex_d76f6b9afcf2439ef6c19b0fb89cc081() {
        assertEval("{ f <- function(x,i) { x[i] } ; f(1:4, 2L) ; f(c(a=1), \"a\") ; f(integer(), NA) }");
    }

    @Test
    public void TestSimpleVectors_testScalarIndex_7c8b7fed0e62f126f17bdcabc2ee77ac() {
        assertEval("{ f <- function(x,i) { x[[i]] } ; f(1:4, 2L) ; f(c(a=1), \"a\") ; f(1:2,-1) }");
    }

    @Test
    public void TestSimpleVectors_testScalarIndex_992c15149e4299d850d909f75a53ef80() {
        assertEval("{ f <- function(x,i) { x[[i]] } ; f(1:4, 2L) ; f(c(a=1), \"a\") ; f(1:2,-2) }");
    }

    @Test
    public void TestSimpleVectors_testScalarIndex_6cf2e47e8ec04a371dade56dec783807() {
        assertEval("{ f <- function(x,i) { x[i] } ; f(1:4, 2L) ; f(c(a=1), \"a\") ; f(1:2,NA) }");
    }

    @Test
    public void TestSimpleVectors_testScalarIndex_f64d547e1bfac72b81d6bbc7f6abb7a0() {
        assertEval("{ f <- function(x,i) { x[i] } ; f(1:4, 2L) ; f(c(a=1), \"a\") ; f(1:2,-4) }");
    }

    @Test
    public void TestSimpleVectors_testScalarIndex_6d6385a6ce007265ccf607c07ca538f4() {
        assertEval("{ f <- function(x,i) { x[i] } ; f(1:4, 2L) ; f(c(a=1), \"a\") ; f(c(a=1L,b=2L),0) }");
    }

    @Test
    public void TestSimpleVectors_testScalarIndex_b43e3d67178e70653b93443ea03d88bb() {
        assertEval("{ f <- function(x,i) { x[i] } ; f(1:4, 2L) ; f(c(a=1), \"a\") ; f(1:2,0) }");
    }

    @Test
    public void TestSimpleVectors_testScalarIndex_60424b7883978b896905584f87a372da() {
        assertEval("{ f <- function(x,i) { x[i] } ; f(1:4, 2L) ; f(c(a=1), \"a\") ; f(1:2,-2) }");
    }

    @Test
    public void TestSimpleVectors_testScalarIndex_d476116d6dedebea0e585e316329d53d() {
        assertEval("{ f <- function(x,i) { x[i] } ; f(1:4, 2L) ; f(c(a=1), \"a\") ; f(c(TRUE,FALSE),NA) }");
    }

    @Test
    public void TestSimpleVectors_testScalarIndex_0eddef67dea4fbf5d930ce89934dd3fc() {
        assertEval("{ f <- function(x,i) { x[i] } ; f(1:4, 2L) ; f(c(a=1), \"a\") ; f(c(TRUE,FALSE),-4) }");
    }

    @Test
    public void TestSimpleVectors_testScalarIndex_87978a95ff46685552fdaba914a68865() {
        assertEval("{ f <- function(x,i) { x[i] } ; f(1:4, 2L) ; f(c(a=1), \"a\") ; f(c(TRUE,FALSE),0) }");
    }

    @Test
    public void TestSimpleVectors_testScalarIndex_1f0ca732efd80c0832cd49ac24b579b4() {
        assertEval("{ f <- function(x,i) { x[i] } ; f(1:4, 2L) ; f(c(a=1), \"a\") ; f(c(a=TRUE,b=FALSE),0) }");
    }

    @Test
    public void TestSimpleVectors_testScalarIndex_6f2a0591d57fd7c8c778b4a5bfd74096() {
        assertEval("{ f <- function(x,i) { x[i] } ; f(1:4, 2L) ; f(c(a=1), \"a\") ; f(c(TRUE,FALSE),-2) }");
    }

    @Test
    public void TestSimpleVectors_testScalarIndex_286f7780cb2131f6a5316b45a69b4b72() {
        assertEval("{ f <- function(x,i) { x[i] } ; f(1:4, 2L) ; f(c(a=1), \"a\") ; f(c(TRUE,FALSE),4) }");
    }

    @Test
    public void TestSimpleVectors_testScalarIndex_c67abd9557f37d4a7ce84ba9798f3a56() {
        assertEval("{ f <- function(x,i) { x[i] } ; f(1:4, 2L) ; f(c(a=1), \"a\") ; f(c(a=TRUE,b=FALSE),4) }");
    }

    @Test
    public void TestSimpleVectors_testScalarIndex_0199f93899548b8d24184d366f94e720() {
        assertEval("{ f <- function(x,i) { x[i] } ; f(1:4, 2L) ; f(c(a=1), \"a\") ; f(list(1,2),-4) }");
    }

    @Test
    public void TestSimpleVectors_testScalarIndex_9ebb2291da2c7ae213595b3877227f76() {
        assertEval("{ f <- function(x,i) { x[i] } ; f(1:4, 2L) ; f(c(a=1), \"a\") ; f(list(1,2),4) }");
    }

    @Test
    public void TestSimpleVectors_testScalarIndex_0a70ee0a0323e5c7bc1a4e5247c84aa3() {
        assertEval("{ f <- function(x,i) { x[i] } ; f(1:4, 2L) ; f(c(a=1), \"a\") ; f(list(a=1,b=2),4) }");
    }

    @Test
    public void TestSimpleVectors_testScalarIndex_ee840e859861608e3ee028e844fd6109() {
        assertEval("{ f <- function(x,i) { x[i] } ; f(1:4, 2L) ; f(c(a=1), \"a\") ; f(c(\"a\",\"b\"),4) }");
    }

    @Test
    public void TestSimpleVectors_testScalarIndex_2029832d44f2e40ee352a2c942c79f38() {
        assertEval("{ f <- function(x,i) { x[i] } ; f(1:4, 2L) ; f(c(a=1), \"a\") ; f(c(\"a\",\"b\"),NA) }");
    }

    @Test
    public void TestSimpleVectors_testScalarIndex_3329a999908ae3e3c3f6841ee86a62f5() {
        assertEval("{ f <- function(x,i) { x[i] } ; f(1:4, 2L) ; f(c(a=1), \"a\") ; f(c(\"a\",\"b\"),-4) }");
    }

    @Test
    public void TestSimpleVectors_testScalarIndex_d36c373afe1d7e908ed1ab2c556717a9() {
        assertEval("{ f <- function(x,i) { x[i] } ; f(1:4, 2L) ; f(c(a=1), \"a\") ; f(c(\"a\",\"b\"),0) }");
    }

    @Test
    public void TestSimpleVectors_testScalarIndex_3d964a3f74217999a7ba05a8c6b686be() {
        assertEval("{ f <- function(x,i) { x[i] } ; f(1:4, 2L) ; f(c(a=1), \"a\") ; f(c(a=\"a\",b=\"b\"),0) }");
    }

    @Test
    public void TestSimpleVectors_testScalarIndex_21266169e46726b5c0421f44a4967a51() {
        assertEval("{ f <- function(x,i) { x[i] } ; f(1:4, 2L) ; f(c(a=1), \"a\") ; f(c(1+2i,3+4i),NA) }");
    }

    @Test
    public void TestSimpleVectors_testScalarIndex_479e6f35e1d04cfae75e3690a0fc48c4() {
        assertEval("{ f <- function(x,i) { x[i] } ; f(1:4, 2L) ; f(c(a=1), \"a\") ; f(c(1+2i,3+4i),-4) }");
    }

    @Test
    public void TestSimpleVectors_testScalarIndex_da63e8a9bf21482888f09b934989ab90() {
        assertEval("{ f <- function(x,i) { x[i] } ; f(1:4, 2L) ; f(c(a=1), \"a\") ; f(c(1+2i,3+4i),4) }");
    }

    @Test
    public void TestSimpleVectors_testScalarIndex_8f97852319952dc49dfc6a16bb728403() {
        assertEval("{ f <- function(x,i) { x[i] } ; f(1:4, 2L) ; f(c(a=1), \"a\") ; f(c(a=1+2i,b=3+4i),4) }");
    }

    @Test
    public void TestSimpleVectors_testScalarIndex_a2f2a08fd2b2971138c187d55922002e() {
        assertEval("{ f <- function(x,i) { x[i] } ; f(1:4, 2L) ; f(c(a=1), \"a\") ; f(as.raw(c(10,11)),-4) }");
    }

    @Test
    public void TestSimpleVectors_testScalarIndex_8ced45a533c71d5d820add8c4a108310() {
        assertEval("{ f <- function(x,i) { x[i] } ; f(1:4, 2L) ; f(c(a=1), \"a\") ; f(as.raw(c(10,11)),0) }");
    }

    @Test
    public void TestSimpleVectors_testScalarIndex_cac5b26455790bdac250e8b2929f0b7f() {
        assertEval("{ f <- function(x,i) { x[i] } ; f(1:4, 2L) ; f(c(a=1), \"a\") ; f(as.raw(c(10,11)),4) }");
    }

    @Test
    public void TestSimpleVectors_testScalarIndex_d301f6c139e4f63d8f74382c7cc6482a() {
        assertEval("{ f <- function(x,i) { x[i] } ; f(1:4, 2L) ; f(c(a=1), \"a\") ; z <- c(1+2i,3+4i) ; attr(z, \"my\") <- 1 ; f(z,-10) }");
    }

    @Test
    public void TestSimpleVectors_testScalarIndex_b048e75762e1c565b2617a8c5abf25bf() {
        assertEval("{ f <- function(x,i) { x[i] } ; f(1:4, 2L) ; f(c(a=1), \"a\") ; z <- c(1,3) ; attr(z, \"my\") <- 1 ; f(z,-10) }");
    }

    @Test
    public void TestSimpleVectors_testScalarIndex_a544054267a50bb9db780958f1aa5b7a() {
        assertEval("{ f <- function(x,i) { x[i] } ; f(1:4, 2L) ; f(c(a=1), \"a\") ; z <- c(1L,3L) ; attr(z, \"my\") <- 1 ; f(z,-10) }");
    }

    @Test
    public void TestSimpleVectors_testScalarIndex_5e58f5282047ec68c090ed7822deaa07() {
        assertEval("{ f <- function(x,i) { x[i] } ; f(1:4, 2L) ; f(c(a=1), \"a\") ; z <- c(TRUE,FALSE) ; attr(z, \"my\") <- 1 ; f(z,-10) }");
    }

    @Test
    public void TestSimpleVectors_testScalarIndex_d8db2a2f3f116442da496d752b517bc3() {
        assertEval("{ f <- function(x,i) { x[i] } ; f(1:4, 2L) ; f(c(a=1), \"a\") ; z <- c(a=\"a\",b=\"b\") ; attr(z, \"my\") <- 1 ; f(z,-10) }");
    }

    @Test
    public void TestSimpleVectors_testScalarIndex_415aade7ef2c80206d6b6a4b14520701() {
        assertEval("{ f <- function(x,i) { x[i] } ; f(1:4, 2L) ; f(c(a=1), \"a\") ; z <- c(a=as.raw(10),b=as.raw(11)) ; attr(z, \"my\") <- 1 ; f(z,-10) }");
    }

    @Test
    public void TestSimpleVectors_testScalarIndex_302bb092ce335272b1dcfa49f9a5982e() {
        assertEval("{ f <- function(x,i) { x[i] } ; f(1:4, 2L) ; f(c(a=1), \"a\") ; f(1:3,c(TRUE,FALSE)) }");
    }

    @Test
    public void TestSimpleVectors_testScalarIndex_5f22c8ae5cc529428a95d0f3a7153e95() {
        assertEval("{ f <- function(x,i) { x[i] } ; f(1:4, 2L) ; f(c(a=1), \"a\") ; f(1:3,c(1,2)) }");
    }

    @Test
    public void TestSimpleVectors_testScalarIndex_e2e0914debca71a826c67a51628d953f() {
        assertEval("{ x <- 1:3 ; x[TRUE] <- 10 ; x }");
    }

    @Test
    public void TestSimpleVectors_testScalarIndex_8b2d8fd122c6f4fe211a09d8631a1f47() {
        assertEval("{ x <- 1:3 ; x[[TRUE]] <- 10 ; x }");
    }

    @Test
    public void TestSimpleVectors_testScalarIndex_0ca792ef5f55f83be583edb1f96fa47f() {
        assertEval("{ b <- c(1+2i,3+4i) ; dim(b) <- c(2,1) ; b[1] <- 3+1i ; b }");
    }

    @Test
    public void TestSimpleVectors_testScalarIndex_888cc5df646aba9a374c2b19cae9c037() {
        assertEval("{ b <- list(1+2i,3+4i) ; dim(b) <- c(2,1) ; b[\"hello\"] <- NULL ; b }");
    }

    @Test
    public void TestSimpleVectors_testScalarIndex_be1593d8cc3ee8434648bdcce749f404() {
        assertEval("{ x<-1:4; x[c(-1.5)] }");
    }

    @Test
    public void TestSimpleVectors_testScalarIndex_c5bf8fe63f55705b2cb57df329d3fc2d() {
        assertEval("{ x<-1:4; x[c(-0.5)] }");
    }

    @Test
    public void TestSimpleVectors_testScalarIndex_ef5bc4f2a725e0a67adf0f7152b65e14() {
        assertEval("{ x<-1:4; x[c(1.4,1.8)] }");
    }

    @Test
    public void TestSimpleVectors_testScalarIndex_6c76043f65f6afe7dd9830e157c67576() {
        assertEvalError(" { x <- 1:3 ; x[[NULL]] }");
    }

    @Test
    public void TestSimpleVectors_testScalarIndex_1f6ce2b65db60583a32fce6d86fc7f09() {
        assertEvalError("{ x<-function() {1} ; y <- 2;  x[y] }");
    }

    @Test
    public void TestSimpleVectors_testScalarIndex_1b15b6a7007e3d7c3862c4f21ccc98a2() {
        assertEvalError("{ x<-function() {1} ; y <- 2;  y[x] }");
    }

    @Test
    public void TestSimpleVectors_testScalarIndex_efe356c278dd12174231ff0470cda9d1() {
        assertEvalError("{ x<-as.list(5:1) ; y <- 1:2;  x[[y]] }");
    }

    @Test
    public void TestSimpleVectors_testScalarIndex_3067eaf57f43cdbbd66dd3acf805b418() {
        assertEvalError("{ x<-function() {1} ; x[2L] }");
    }

    @Test
    public void TestSimpleVectors_testScalarIndex_7862196f55b49b9da907b833239b820e() {
        assertEvalError("{ x <- function(){3} ; y <- 3 ; x[[y]] }");
    }

    @Test
    public void TestSimpleVectors_testScalarIndex_83e479d4d7d9677c4e220736c0fc24bc() {
        assertEvalError("{ f <- function(x,i) { x[[i]] } ; f(1:4, 2L) ; f(c(a=1), \"a\") ; f(list(1,2),FALSE) }");
    }

    @Test
    public void TestSimpleVectors_testScalarIndex_8c0cb442854c3a41aa613a71bce4d9f3() {
        assertEvalError("{ f <- function(x,i) { x[[i]] } ; f(1:4, 2L) ; f(c(a=1), \"a\") ; f(list(1,2),1+0i) }");
    }

    @Test
    public void TestSimpleVectors_testScalarIndex_8dd32115d7a04762e93b6d8a96bc74df() {
        assertEvalError("{ f <- function(x,i) { x[[i]] } ; f(1:4, 2L) ; f(c(a=1), \"a\") ; f(1:3,4) }");
    }

    @Test
    public void TestSimpleVectors_testScalarIndex_e08be2ea57354a31be34e233a88794cf() {
        assertEvalError("{ f <- function(x,i) { x[[i]] } ; f(1:4, 2L) ; f(c(a=1), \"a\") ; f(1:3,NA) }");
    }

    @Test
    public void TestSimpleVectors_testScalarIndex_40bec1554fa960e5ebff9975b39be7fa() {
        assertEvalError("{ f <- function(x,i) { x[[i]] } ; f(1:4, 2L) ; f(c(a=1), \"a\") ; f(1:3,-1) }");
    }

    @Test
    public void TestSimpleVectors_testScalarIndex_0d075f0c7cb3b2b12ee909f8a20e36ce() {
        assertEvalError("{ f <- function(x,i) { x[[i]] } ; f(1:4, 2L) ; f(c(a=1), \"a\") ; f(2,-2) }");
    }

    @Test
    public void TestSimpleVectors_testScalarIndex_5e4851b8ee44b0424147f0a84cb0c00d() {
        assertEvalError("{ f <- function(x,i) { x[[i]] } ; f(1:4, 2L) ; f(c(a=1), \"a\") ; f(2,-3) }");
    }

    @Test
    public void TestSimpleVectors_testScalarIndex_fad12749ab58f520b228eca5883a85fd() {
        assertEvalError("{ f <- function(x,i) { x[[i]] } ; f(1:4, 2L) ; f(c(a=1), \"a\") ; f(1:4,-3) }");
    }

    @Test
    public void TestSimpleVectors_testScalarIndex_38022471c8a556dfcd31377bf221d3fd() {
        assertEvalError("{ f <- function(x,i) { x[[i]] } ; f(1:4, 2L) ; f(c(a=1), \"a\") ; f(1:2,-3) }");
    }

    @Test
    public void TestSimpleVectors_testScalarIndex_719828ffe9d432cdd7b936bdef2d4d81() {
        assertEvalError("{ f <- function(x,i) { x[[i]] } ; f(1:4, 2L) ; f(c(a=1), \"a\") ; f(1:3,c(TRUE,FALSE)) }");
    }

    @Test
    public void TestSimpleVectors_testScalarIndex_b7f1b6c224c905e03542543ff6522ddb() {
        assertEvalError("{ f <- function(x,i) { x[[i]] } ; f(1:4, 2L) ; f(c(a=1), \"a\") ; f(1:3,c(3,3)) }");
    }

    @Test
    public void TestSimpleVectors_testScalarIndex_e521b7f64369a52c90a4bf4e1d7a2fde() {
        assertEvalError("{ x <- as.list(1:2) ; f <- function(i) { x[[i]] <- NULL ; x } ; f(1) ; f(as.raw(10)) }");
    }

    @Test
    public void TestSimpleVectors_testScalarIndex_43e4d916c58c831fff9e543d385b70e7() {
        assertEvalError("{ x <- 1:3 ; x[2] <- integer() }");
    }

    @Test
    public void TestSimpleVectors_testScalarIndex_6a2c1f530cc3053597f9bd84d7f93336() {
        assertEvalError("{ x <- 1:3 ; x[[TRUE]] <- 1:2 }");
    }

    @Test
    public void TestSimpleVectors_testScalarIndex_8b4991ee77dc76e754dc6a94b1b02903() {
        assertEvalError("{ x <- 1:3 ; x[[FALSE]] <- 10 ; x }");
    }

    @Test
    public void TestSimpleVectors_testScalarIndex_93f3864842c5903beb5fac9b0e6d1546() {
        assertEvalError("{ x <- 1:3 ; x[[NA]] <- 10 ; x }");
    }

    @Test
    public void TestSimpleVectors_testScalarIndex_b58da1932910c8d41d641a044fcb656a() {
        assertEvalError("{ f <- function(b,i,v) { b[[i]] <- v ; b } ; f(1:3, 1L, 10) ; f(c(1,2), \"hello\", TRUE) ; f(1:2, list(1), 3) }");
    }

    @Test
    public void TestSimpleVectors_testScalarIndex_137a9d4cff52a8deae82bb51fa5be019() {
        assertEvalError("{ f <- function(b,i,v) { b[[i]] <- v ; b } ; f(1:3, 1L, 10) ; f(c(1,2), \"hello\", TRUE) ; f(1:2, list(), 3) }");
    }

    @Test
    public void TestSimpleVectors_testScalarIndex_506b655ea1254a74b6ab8009d14deed3() {
        assertEvalError("{ f <- function(b,i,v) { b[i] <- v ; b } ; f(1:3, 1L, 10) ; f(c(1,2), \"hello\", TRUE) ; f(1:2, 1+2i, 3) }");
    }

    @Test
    public void TestSimpleVectors_testScalarIndex_a65b12faaf8797cc763f2c479d2ed6c3() {
        assertEvalError("{ f <- function(b,i,v) { b[[i]] <- v ; b } ; f(1:3, 1L, 10) ; f(c(1,2), \"hello\", TRUE) ; f(1:2, 1, 3:4) }");
    }

    @Test
    public void TestSimpleVectors_testScalarIndex_f922b6ea0e26cd2c018dcf7fe32f0fff() {
        assertEvalError("{ f <- function(b,i,v) { b[[i]] <- v ; b } ; f(1:3, 1L, 10) ; f(c(1,2), \"hello\", TRUE) ; f(1:2, as.integer(NA), 3:4) }");
    }

    @Test
    public void TestSimpleVectors_testScalarIndex_c907d752426a67f3307085caee70a245() {
        assertEvalError("{ x <- 1:2 ; x[as.integer(NA)] <- 3:4 }");
    }

    @Test
    public void TestSimpleVectors_testScalarIndex_e779aeb90d80a1638b7a5cde7d6df3e8() {
        assertEvalError("{ f <- function(x,i) { x[[i]]} ; f(list(1,2,3,4), 3); f(f,2) }");
    }

    @Test
    public void TestSimpleVectors_testScalarIndex_23d48d08709d0c790853d982cc773a44() {
        assertEvalError("{ f <- function(x,i) { x[i] } ; x <- c(a=1,b=2) ; f(x,\"a\") ; f(function(){3},\"b\") }");
    }

    @Test
    public void TestSimpleVectors_testScalarIntAsVector_c8a9f5db23840e357b39daccd0c8b7ff() {
        assertEval("{ x<-1L; x[1L] }");
    }

    @Test
    public void TestSimpleVectors_testScalarIntAsVector_8690eb653b2a591ae61aa45ebce9207a() {
        assertEval("{ x<-1L; x[0L] }");
    }

    @Test
    public void TestSimpleVectors_testScalarIntAsVector_ef72b2cff00b178752cd9b35411f531a() {
        assertEval("{ x<-1L; x[2L] }");
    }

    @Test
    public void TestSimpleVectors_testScalarIntAsVector_08f9c323b0fc669fbf6504ff8f38ec7d() {
        assertEval("{ x<-1L; x[-1L] }");
    }

    @Test
    public void TestSimpleVectors_testScalarIntAsVector_adf08ecfbee113433ec11f9d7e90f8b1() {
        assertEval("{ x<-1L; x[-2L] }");
    }

    @Test
    public void TestSimpleVectors_testScalarIntAsVector_b2d92b69ed2aaea8e8cb73e0ce65f058() {
        assertEval("{ x<-1L; x[TRUE] }");
    }

    @Test
    public void TestSimpleVectors_testScalarIntAsVector_c14c53842e186837694f03fc36d200f2() {
        assertEval("{ x<-1L; x[FALSE] }");
    }

    @Test
    public void TestSimpleVectors_testScalarIntAsVector_19512e25003805afb8fcdc6959520df6() {
        assertEval("{ x<-1L; x[NA] }");
    }

    @Test
    public void TestSimpleVectors_testScalarIntIndexOnVector_21f34345010dad6027118fe57895dc09() {
        assertEval("{ x<-c(1,2,3); x[1L] }");
    }

    @Test
    public void TestSimpleVectors_testScalarIntIndexOnVector_1a2cc86f6af315c71d5d6933ee26282d() {
        assertEval("{ x<-c(1,2,3); x[2L] }");
    }

    @Test
    public void TestSimpleVectors_testScalarIntIndexOnVector_03da652d4a8bcd8e47ba05a2c9189562() {
        assertEval("{ x<-c(1,2,3); x[3L] }");
    }

    @Test
    public void TestSimpleVectors_testScalarIntIndexOnVector_e856ea222f8154d363ff0ffac44db78f() {
        assertEval("{ x<-c(1L,2L,3L); x[1L] }");
    }

    @Test
    public void TestSimpleVectors_testScalarIntIndexOnVector_2a832aed8b21e3a9e3c4608c8aa4f134() {
        assertEval("{ x<-c(1L,2L,3L); x[2L] }");
    }

    @Test
    public void TestSimpleVectors_testScalarIntIndexOnVector_b312038e39c5d2bf1d3140a34626e123() {
        assertEval("{ x<-c(1L,2L,3L); x[3L] }");
    }

    @Test
    public void TestSimpleVectors_testScalarIntIndexOnVector_071a2cb316dc9ab506271e9856674c18() {
        assertEval("{ x<-1:3; x[1L] }");
    }

    @Test
    public void TestSimpleVectors_testScalarIntIndexOnVector_293c100ffc39c403d5cbfa2f2b4a44c5() {
        assertEval("{ x<-1:3; x[2L] }");
    }

    @Test
    public void TestSimpleVectors_testScalarIntIndexOnVector_42358daa3ccb17d1dc9555fb5d7bbaa5() {
        assertEval("{ x<-1:3; x[3L] }");
    }

    @Test
    public void TestSimpleVectors_testScalarIntIndexOnVector_97fc17be8cc6aa4bb47c2fac12b9899b() {
        assertEval("{ x<-3:1; x[1L] }");
    }

    @Test
    public void TestSimpleVectors_testScalarIntIndexOnVector_c71ae3b6d1c7cfb6e7e6c08846826d3c() {
        assertEval("{ x<-3:1; x[2L] }");
    }

    @Test
    public void TestSimpleVectors_testScalarIntIndexOnVector_ae2e64fea8687ae2cdd2dbf2c12a05b7() {
        assertEval("{ x<-3:1; x[3L] }");
    }

    @Test
    public void TestSimpleVectors_testScalarIntIndexScalarValueUpdateOnVector_24e0839678ff53184f13aac451f05d9b() {
        assertEval("{ x<-c(TRUE,TRUE,FALSE); x[2.3] <- FALSE; x }");
    }

    @Test
    public void TestSimpleVectors_testScalarIntIndexScalarValueUpdateOnVector_a768babe3834eb353f8bdbbc5dcb1853() {
        assertEval("{ x<-c(TRUE,TRUE,FALSE); x[2.3] <- 100L; x }");
    }

    @Test
    public void TestSimpleVectors_testScalarIntIndexScalarValueUpdateOnVector_19b22b1bd2dc875eeb3c45d8ae271ab5() {
        assertEval("{ x<-c(TRUE,TRUE,FALSE); x[2.3] <- 100; x }");
    }

    @Test
    public void TestSimpleVectors_testScalarIntIndexScalarValueUpdateOnVector_8d19ad3ebde707a8a24afea2988831d2() {
        assertEval("{ x<-c(TRUE,TRUE,FALSE); x[2.3] <- \"hello\"; x }");
    }

    @Test
    public void TestSimpleVectors_testScalarIntIndexScalarValueUpdateOnVector_cb49ea237c8dfc5727aa20681a099848() {
        assertEval("{ x<-c(1L,2L,3L); x[2.3] <- FALSE; x }");
    }

    @Test
    public void TestSimpleVectors_testScalarIntIndexScalarValueUpdateOnVector_b0554046fc7b94376cda9158ed1f3ac7() {
        assertEval("{ x<-c(1L,2L,3L); x[2.3] <- 100L; x }");
    }

    @Test
    public void TestSimpleVectors_testScalarIntIndexScalarValueUpdateOnVector_ae075ca47374f0c1291ae79fa63d23af() {
        assertEval("{ x<-c(1L,2L,3L); x[2.3] <- 100; x }");
    }

    @Test
    public void TestSimpleVectors_testScalarIntIndexScalarValueUpdateOnVector_566b518d7467e4e5873b29132e3f0c4c() {
        assertEval("{ x<-c(1L,2L,3L); x[2.3] <- \"hello\"; x }");
    }

    @Test
    public void TestSimpleVectors_testScalarIntIndexScalarValueUpdateOnVector_406a86bbb59de3b836f0e8054a53c744() {
        assertEval("{ x<-c(1,2,3); x[2.3] <- FALSE; x }");
    }

    @Test
    public void TestSimpleVectors_testScalarIntIndexScalarValueUpdateOnVector_9436648794460458f137cbdb901a7364() {
        assertEval("{ x<-c(1,2,3); x[2.3] <- 100; x }");
    }

    @Test
    public void TestSimpleVectors_testScalarIntIndexScalarValueUpdateOnVector_3e1e4804ea0724943d89e1bc342f2533() {
        assertEval("{ x<-c(1,2,3); x[2.3] <- \"hello\"; x }");
    }

    @Test
    public void TestSimpleVectors_testScalarIntIndexScalarValueUpdateOnVector_220169f25a36557f83281221a95ac9d2() {
        assertEval("{ x<-c(TRUE,TRUE,FALSE); x[2.3] <- 100i; x }");
    }

    @Test
    public void TestSimpleVectors_testScalarIntIndexScalarValueUpdateOnVector_2e1228f822c8d764370a029ce6b1a96b() {
        assertEval("{ x<-c(1L,2L,3L); x[2.3] <- 100i; x }");
    }

    @Test
    public void TestSimpleVectors_testScalarIntIndexScalarValueUpdateOnVector_5a2883f80da612378c68a1d156b8d82c() {
        assertEval("{ x<-c(1,2,3); x[2.3] <- 100i; x }");
    }

    @Test
    public void TestSimpleVectors_testScalarIntNegativeIndexOnVector_d987b0bab057b91dc4dbb9d288e29700() {
        assertEval("{ x<-c(1,2,3); x[-1L] }");
    }

    @Test
    public void TestSimpleVectors_testScalarIntNegativeIndexOnVector_f61b0c694f2a1c64952b4805dc619511() {
        assertEval("{ x<-c(1,2,3); x[-2L] }");
    }

    @Test
    public void TestSimpleVectors_testScalarIntNegativeIndexOnVector_06fdfb25cb463a4982ff81b0ddd646bc() {
        assertEval("{ x<-c(1,2,3); x[-3L] }");
    }

    @Test
    public void TestSimpleVectors_testScalarIntNegativeIndexOnVector_bdad54fe3eb57cfce34fcb17a305c38c() {
        assertEval("{ x<-c(1L,2L,3L); x[-1L] }");
    }

    @Test
    public void TestSimpleVectors_testScalarIntNegativeIndexOnVector_31bc86d012c2528420c95c9699c76291() {
        assertEval("{ x<-c(1L,2L,3L); x[-2L] }");
    }

    @Test
    public void TestSimpleVectors_testScalarIntNegativeIndexOnVector_2bb7c3b1c0a2756bfdfb047835cfa4dd() {
        assertEval("{ x<-c(1L,2L,3L); x[-3L] }");
    }

    @Test
    public void TestSimpleVectors_testScalarIntNegativeIndexOnVector_914dbe76b1363b3a653ab03de1b7397e() {
        assertEval("{ x<-1:3; x[-1L] }");
    }

    @Test
    public void TestSimpleVectors_testScalarIntNegativeIndexOnVector_93b2394f16a362022f712ae941a156da() {
        assertEval("{ x<-1:3; x[-2L] }");
    }

    @Test
    public void TestSimpleVectors_testScalarIntNegativeIndexOnVector_46b1fd34b88de39ac3fec36ebc39c502() {
        assertEval("{ x<-1:3; x[-3L] }");
    }

    @Test
    public void TestSimpleVectors_testScalarLogicOnVector_b97bd3a5c660cddec13778b5a553beca() {
        assertEval("{ x<-c(1,2,3); x[FALSE] }");
    }

    @Test
    public void TestSimpleVectors_testScalarLogicOnVector_770e1bec79da857f0818a7365a9e89c6() {
        assertEval("{ x<-c(1L,2L,3L); x[FALSE] }");
    }

    @Test
    public void TestSimpleVectors_testScalarLogicOnVector_e794493dd1e120a2abbf837dd8704647() {
        assertEval("{ x<-1:3; x[FALSE] }");
    }

    @Test
    public void TestSimpleVectors_testScalarLogicOnVector_c06fc37264eb0d1633a026d1d351b23d() {
        assertEval("{ x<-c(1,2,3); x[TRUE] }");
    }

    @Test
    public void TestSimpleVectors_testScalarLogicOnVector_91059a82ec22d29ecd4bb8b4bf08c536() {
        assertEval("{ x<-c(1L,2L,3L); x[TRUE] }");
    }

    @Test
    public void TestSimpleVectors_testScalarLogicOnVector_ce3de4f7efd43fde26962b2a8de9959c() {
        assertEval("{ x<-1:3; x[TRUE] }");
    }

    @Test
    public void TestSimpleVectors_testScalarOutOfBoundsOnVector_4420fc80abd284a5b9cbbad99963af9a() {
        assertEval("{ x<-c(1,2,3); x[4L] }");
    }

    @Test
    public void TestSimpleVectors_testScalarOutOfBoundsOnVector_a093536f6faf2479bf44a7dd699642de() {
        assertEval("{ x<-c(1L,2L,3L); x[4L] }");
    }

    @Test
    public void TestSimpleVectors_testScalarOutOfBoundsOnVector_a7602d9a3f27e02bf044bdba9058003d() {
        assertEval("{ x<-1:3; x[4L] }");
    }

    @Test
    public void TestSimpleVectors_testScalarOutOfBoundsOnVector_eb6224c3036acec85302edeb40886296() {
        assertEval("{ x<-c(1,2,3); x[4.1] }");
    }

    @Test
    public void TestSimpleVectors_testScalarOutOfBoundsOnVector_be48270b11aac6ae4e2bd1232a97ad10() {
        assertEval("{ x<-c(1L,2L,3L); x[4.1] }");
    }

    @Test
    public void TestSimpleVectors_testScalarOutOfBoundsOnVector_9df76bc045f001d3ec10bdce9cbf8cdc() {
        assertEval("{ x<-1:3; x[4.1] }");
    }

    @Test
    public void TestSimpleVectors_testScalarOutOfBoundsOnVector_c9ad4d81c2417019d4e5dd006a72580c() {
        assertEval("{ x<-c(1,2,3); x[-4L] }");
    }

    @Test
    public void TestSimpleVectors_testScalarOutOfBoundsOnVector_795e72c64aead8314a02da48d7807453() {
        assertEval("{ x<-c(1L,2L,3L); x[-4L] }");
    }

    @Test
    public void TestSimpleVectors_testScalarOutOfBoundsOnVector_e4c1ea49d9240af294a5334442d0bd34() {
        assertEval("{ x<-1:3; x[-4L] }");
    }

    @Test
    public void TestSimpleVectors_testScalarOutOfBoundsOnVector_43b94939f91be20839b11997f0dce845() {
        assertEval("{ x<-c(1,2,3); x[-4.1] }");
    }

    @Test
    public void TestSimpleVectors_testScalarOutOfBoundsOnVector_d7557382a0a6347e3a263cea223f7381() {
        assertEval("{ x<-c(1L,2L,3L); x[-4.1] }");
    }

    @Test
    public void TestSimpleVectors_testScalarOutOfBoundsOnVector_efd65d1f3f6d9db2dda269891418e826() {
        assertEval("{ x<-1:3; x[-4.1] }");
    }

    @Test
    public void TestSimpleVectors_testScalarOutOfBoundsOnVector_fde25c81ef6f8bd6861131419fc22203() {
        assertEval("{ x<-c(1,2,3); x[0L] }");
    }

    @Test
    public void TestSimpleVectors_testScalarOutOfBoundsOnVector_54697558d510d2dbb7dca189d04d6f0e() {
        assertEval("{ x<-c(1L,2L,3L); x[0L] }");
    }

    @Test
    public void TestSimpleVectors_testScalarOutOfBoundsOnVector_43fde9d2583dc6645bc9f6c8ac50c64c() {
        assertEval("{ x<-1:3; x[0L] }");
    }

    @Test
    public void TestSimpleVectors_testScalarOutOfBoundsOnVector_328c772c04586c56fc2c68a6e28e5853() {
        assertEval("{ x<-c(1,2,3); x[0] }");
    }

    @Test
    public void TestSimpleVectors_testScalarOutOfBoundsOnVector_a7d0261a56f578ca3d0007440c5ce812() {
        assertEval("{ x<-c(1L,2L,3L); x[0] }");
    }

    @Test
    public void TestSimpleVectors_testScalarOutOfBoundsOnVector_0b40f5c092d26309b1d31f0ca31489d9() {
        assertEval("{ x<-1:3; x[0] }");
    }

    @Test
    public void TestSimpleVectors_testScalarOutOfBoundsOnVector_420f33f679daef81a817ed8717324b1a() {
        assertEval("{ x<-c(1,2,3); x[0.1] }");
    }

    @Test
    public void TestSimpleVectors_testScalarOutOfBoundsOnVector_01272ec62fc2558b3401639bb9d40f14() {
        assertEval("{ x<-c(1L,2L,3L); x[0.1] }");
    }

    @Test
    public void TestSimpleVectors_testScalarOutOfBoundsOnVector_6c8062c1cfbf7385a786c2748bb41163() {
        assertEval("{ x<-1:3; x[0.1] }");
    }

    @Test
    public void TestSimpleVectors_testScalarOutOfBoundsOnVector_56fb30c1f1e1c458babb43d54682aec0() {
        assertEval("{ x<-c(1,2,3); x[-0.1] }");
    }

    @Test
    public void TestSimpleVectors_testScalarOutOfBoundsOnVector_75ebae7c0fbc6f076571ecc1eca34950() {
        assertEval("{ x<-c(1L,2L,3L); x[-0.1] }");
    }

    @Test
    public void TestSimpleVectors_testScalarOutOfBoundsOnVector_824bad7757ef4527e818848c9a152ddb() {
        assertEval("{ x<-1:3; x[-0.1] }");
    }

    @Test
    public void TestSimpleVectors_testScalarOutOfBoundsOnVector_dbe5deb75b10df713e178073410dcabe() {
        assertEval("{ x<-c(1,2,3); x[NA] }");
    }

    @Test
    public void TestSimpleVectors_testScalarOutOfBoundsOnVector_51dbbfadc930910bb0da1fed33478580() {
        assertEval("{ x<-c(1L,2L,3L); x[NA] }");
    }

    @Test
    public void TestSimpleVectors_testScalarOutOfBoundsOnVector_7dbceeef7aee4ecf90cc31c836880f57() {
        assertEval("{ x<-1:3; x[NA] }");
    }

    @Test
    public void TestSimpleVectors_testScalarOutOfBoundsOnVector_6a116b7dade13ccf6643d7f4c625aeb8() {
        assertEval("{ x<-c(1,2,3); typeof(x[NA]) }");
    }

    @Test
    public void TestSimpleVectors_testScalarOutOfBoundsOnVector_92b56baa0bb296f5ff5e37ec79a227dc() {
        assertEval("{ x<-c(1L,2L,3L); typeof(x[NA]) }");
    }

    @Test
    public void TestSimpleVectors_testScalarOutOfBoundsOnVector_22a98a1e98696029f5ca586c5101bb8f() {
        assertEval("{ x<-1:3; typeof(x[NA]) }");
    }

    @Test
    public void TestSimpleVectors_testScalarUpdate_8e6e0905d3f18466c5942fc8ac2ecced() {
        assertEval(" { b <- c(1,2) ; x <- b ; f <- function(b,v) { b[2L] <- v ; b } ; f(b,10) ; f(1:3,13L) }");
    }

    @Test
    public void TestSimpleVectors_testScalarUpdate_8b6fc13522cdf6f4d0cf02d060680d02() {
        assertEval("{ x<-1:3; x[1]<-100L; x }");
    }

    @Test
    public void TestSimpleVectors_testScalarUpdate_5b16e1e09bd196857ca7c2673438f0e0() {
        assertEval("{ x<-c(1,2,3); x[2L]<-100L; x }");
    }

    @Test
    public void TestSimpleVectors_testScalarUpdate_879d3e3ceda731159cee890b49f830e6() {
        assertEval("{ x<-c(1,2,3); x[2L]<-100; x }");
    }

    @Test
    public void TestSimpleVectors_testScalarUpdate_5fdd8f957047c5dacce9b716e9ae18c9() {
        assertEval("{ x<-c(1,2,3); x[2]<-FALSE; x }");
    }

    @Test
    public void TestSimpleVectors_testScalarUpdate_fd9c948d30950bbb61277a17a58c8d83() {
        assertEval("{ x<-1:5; x[2]<-1000; x[3] <- TRUE; x[8]<-3L; x }");
    }

    @Test
    public void TestSimpleVectors_testScalarUpdate_6513522fc24e58e165174df166e98c09() {
        assertEval("{ f<-function(x,i,v) { x<-1:5; x[i]<-v; x} ; f(c(1L,2L),1,3L) ; f(c(1L,2L),2,3) }");
    }

    @Test
    public void TestSimpleVectors_testScalarUpdate_760eb6b0a0b89ef61c253e1533ee7ba6() {
        assertEval("{ f<-function(x,i,v) { x<-1:5; x[i]<-v; x} ; f(c(1L,2L),1,3L) ; f(c(1L,2L),8,3L) }");
    }

    @Test
    public void TestSimpleVectors_testScalarUpdate_68b1c256617f52f412e1466781237d28() {
        assertEval("{ f<-function(x,i,v) { x<-1:5; x[i]<-v; x} ; f(c(1L,2L),1,FALSE) ; f(c(1L,2L),2,3) }");
    }

    @Test
    public void TestSimpleVectors_testScalarUpdate_f7817b4e6d655ebaf416cb9a071a3447() {
        assertEval("{ f<-function(x,i,v) { x<-1:5; x[i]<-v; x} ; f(c(1L,2L),1,FALSE) ; f(c(1L,2L),8,TRUE) }");
    }

    @Test
    public void TestSimpleVectors_testScalarUpdate_7b38ae1d6c4a011824dd9c615bfa1624() {
        assertEval("{ a <- c(1L,2L,3L); a <- 1:5; a[3] <- TRUE; a }");
    }

    @Test
    public void TestSimpleVectors_testScalarUpdate_4397b431ed996a335efba93a5bcea3cf() {
        assertEval("{ x <- 1:3 ; x[2] <- \"hi\"; x }");
    }

    @Test
    public void TestSimpleVectors_testScalarUpdate_fb705c5bb52949c2dfc486179b2a713b() {
        assertEval("{ x <- c(1,2,3) ; x[2] <- \"hi\"; x }");
    }

    @Test
    public void TestSimpleVectors_testScalarUpdate_3d0735e377d3d00945d302612e4f56a5() {
        assertEval("{ x <- c(TRUE,FALSE,FALSE) ; x[2] <- \"hi\"; x }");
    }

    @Test
    public void TestSimpleVectors_testScalarUpdate_b7f6891dc23ee9f7543c6403cf232b2d() {
        assertEval("{ x <- c(2,3,4) ; x[1] <- 3+4i ; x  }");
    }

    @Test
    public void TestSimpleVectors_testScalarUpdate_e5c7558b98bec106ac8ee91a38ae0d84() {
        assertEval("{ b <- c(1,2) ; x <- b ; b[2L] <- 3 ; b }");
    }

    @Test
    public void TestSimpleVectors_testScalarUpdate_e5867bd3dce69dc31863b7187efaf73f() {
        assertEval("{ b <- c(1,2) ; b[0L] <- 3 ; b }");
    }

    @Test
    public void TestSimpleVectors_testScalarUpdate_acef3bdc5a6faf81b8009957db79db47() {
        assertEval("{ b <- c(1,2) ; b[0] <- 1+2i ; b }");
    }

    @Test
    public void TestSimpleVectors_testScalarUpdate_5697bde9aa22e4e870849fff9779ebbd() {
        assertEval("{ b <- c(1,2) ; b[5L] <- 3 ; b }");
    }

    @Test
    public void TestSimpleVectors_testScalarUpdate_89c7c10578d5301a452dc16dfb765f71() {
        assertEval("{ f <- function(b,v) { b[2] <- v ; b } ; f(c(1L,2L),10L) ; f(1,3) }");
    }

    @Test
    public void TestSimpleVectors_testScalarUpdate_f2fbba873f55c58be65c8d58cbee3dec() {
        assertEval("{ f <- function(b,v) { b[2] <- v ; b } ; f(c(1L,2L),10L) ; f(1L,3) }");
    }

    @Test
    public void TestSimpleVectors_testScalarUpdate_c4076ef7a883ee64afbba80254e35651() {
        assertEval("{ b <- c(1L,2L) ; b[3] <- 13L ; b }");
    }

    @Test
    public void TestSimpleVectors_testScalarUpdate_6ce654f0fb037b297ec15fcc196bf6f0() {
        assertEval("{ b <- c(1L,2L) ; b[0] <- 13L ; b }");
    }

    @Test
    public void TestSimpleVectors_testScalarUpdate_5fea1ce98ca0924f92aad90d3fd81802() {
        assertEval("{ f <- function(b,i,v) { b[i] <- v ; b } ; b <- c(10L,2L) ; b[0] <- TRUE ; b }");
    }

    @Test
    public void TestSimpleVectors_testScalarUpdate_35b0cfe096f82dc1fdb1ca2650fbf8d5() {
        assertEval("{ f <- function(b,i,v) { b[i] <- v ; b } ; b <- c(10L,2L) ; b[3] <- TRUE ; b }");
    }

    @Test
    public void TestSimpleVectors_testScalarUpdate_7c686858796637d0a128061ced0c51f1() {
        assertEval("{ b <- c(1L,2L) ; b[2] <- FALSE ; b }");
    }

    @Test
    public void TestSimpleVectors_testScalarUpdate_e230fdc11f0de83c2e748e60d926bdec() {
        assertEval("{ f <- function(b,v) { b[2] <- v ; b } ; f(c(1L,2L),TRUE) ; f(1L,3) }");
    }

    @Test
    public void TestSimpleVectors_testScalarUpdate_07091220ff11d9a7f376b45f537abcb0() {
        assertEval("{ f <- function(b,v) { b[2] <- v ; b } ; f(c(1L,2L),TRUE) ; f(10,3) }");
    }

    @Test
    public void TestSimpleVectors_testScalarUpdate_cacdee90b945c44d9599ab91c38be195() {
        assertEval("{ b <- c(1,2) ; x <- b ; f <- function(b,v) { b[2L] <- v ; b } ; f(b,10) ; f(b,13L) }");
    }

    @Test
    public void TestSimpleVectors_testScalarUpdate_b6611afefa3e055c7dfae7f0348e480b() {
        assertEval("{ b <- c(1,2) ; x <- b ; f <- function(b,v) { b[2L] <- v ; b } ; f(b,10) ; f(c(1,2),10) }");
    }

    @Test
    public void TestSimpleVectors_testScalarUpdate_36d886d2e40df6e6584f35829fdec975() {
        assertEval("{ b <- c(1,2) ; x <- b ; f <- function(b,v) { b[2L] <- v ; b } ; f(b,10L) ; f(1:3,13L) }");
    }

    @Test
    public void TestSimpleVectors_testScalarUpdate_ec59637bcec790f7073d0f30fc78e3bf() {
        assertEval("{ b <- c(1,2) ; x <- b ; f <- function(b,v) { b[2L] <- v ; b } ; f(b,10L) ; f(b,13) }");
    }

    @Test
    public void TestSimpleVectors_testScalarUpdate_ed85c76612c3c039db326f7d457747ff() {
        assertEval("{ b <- c(1,2) ; z <- b ; b[3L] <- 3L ; b }");
    }

    @Test
    public void TestSimpleVectors_testScalarUpdate_cff4502766410612f5722a773f57b7bd() {
        assertEval("{ b <- c(1,2) ; z <- b ; b[3L] <- FALSE ; b }");
    }

    @Test
    public void TestSimpleVectors_testScalarUpdate_e4465b9a2e0c53f4a7e9babcc83520b7() {
        assertEval("{ f <- function(b,v) { b[2] <- v ; b } ; f(c(1,2),FALSE) ; f(10L,3) }");
    }

    @Test
    public void TestSimpleVectors_testScalarUpdate_e6475151cea360d7a874b96ea142dc0a() {
        assertEval("{ f <- function(b,v) { b[2] <- v ; b } ; f(c(1,2),FALSE) ; f(10,3) }");
    }

    @Test
    public void TestSimpleVectors_testScalarUpdate_db28f6d6dc433008570130fc134fcf7f() {
        assertEval("{ f <- function(b,v) { b[2] <- v ; b } ; f(c(TRUE,NA),FALSE) ; f(c(FALSE,TRUE),3) }");
    }

    @Test
    public void TestSimpleVectors_testScalarUpdate_0a3546a3da8a6df496529dc38a8d46e0() {
        assertEval("{ f <- function(b,v) { b[2] <- v ; b } ; f(c(TRUE,NA),FALSE) ; f(3,3) }");
    }

    @Test
    public void TestSimpleVectors_testScalarUpdate_77b051a5b0630662ff0917a0f02b7010() {
        assertEval("{ f <- function(b,v) { b[[2]] <- v ; b } ; f(c(\"a\",\"b\"),\"d\") ; f(1:3,\"x\") }");
    }

    @Test
    public void TestSimpleVectors_testScalarUpdate_c437f31d077e4f162746db775e1eae68() {
        assertEval("{ b <- c(\"a\",\"b\") ; z <- b ; b[[3L]] <- \"xx\" ; b }");
    }

    @Test
    public void TestSimpleVectors_testScalarUpdate_09adda5c97bee3d85cb1bc08ce20a710() {
        assertEval("{ x <- as.list(1:2) ; x[[\"z\"]] <- NULL ; x }");
    }

    @Test
    public void TestSimpleVectors_testScalarUpdate_1516ce41d7ffe7190bcb5b25ead6f4ee() {
        assertEval("{ x<-5:1; x[0-2]<-1000; x }");
    }

    @Test
    public void TestSimpleVectors_testScalarUpdate_fba39d3c63512edc6a7cf0378160d969() {
        assertEval("{ x<-c(); x[[TRUE]] <- 2; x }");
    }

    @Test
    public void TestSimpleVectors_testScalarUpdate_0cd0b0064b782821b8c7c39a236dbc93() {
        assertEval("{ x<-1:2; x[[0-2]]<-100; x }");
    }

    @Test
    public void TestSimpleVectors_testScalarUpdate_bfc98a1a775330f146d70efdf98155e4() {
        assertEval("{ b <- c(1,2) ; z <- b ; b[-2] <- 3L ; b }");
    }

    @Test
    public void TestSimpleVectors_testScalarUpdate_98bbfef0763d8f1cab8e43f53d326c93() {
        assertEval("{ b <- c(1,2) ; z <- b ; b[-10L] <- FALSE ; b }");
    }

    @Test
    public void TestSimpleVectors_testScalarUpdate_95e0f2b02375bec15818d8b352eff125() {
        assertEval("{ b <- c(TRUE,NA) ; z <- b ; b[-10L] <- FALSE ; b }");
    }

    @Test
    public void TestSimpleVectors_testScalarUpdate_d3162fe4be286e1955bafb743e92b7ab() {
        assertEval("{ b <- c(TRUE,NA) ; z <- b ; b[4L] <- FALSE ; b }");
    }

    @Test
    public void TestSimpleVectors_testScalarUpdate_e851812a13785b522177bf6f8eaed4b2() {
        assertEval("{ b <- list(TRUE,NA) ; z <- b ; b[[4L]] <- FALSE ; b }");
    }

    @Test
    public void TestSimpleVectors_testScalarUpdate_7e535f1fe8c0e3025eb48b2d6b3b2166() {
        assertEval("{ b <- list(TRUE,NA) ; z <- b ; b[[-1L]] <- FALSE ; b }");
    }

    @Test
    public void TestSimpleVectors_testScalarUpdate_287f44db28e43d6a19fd4203e8578546() {
        assertEval("{ f <- function(b,v) { b[[2]] <- v ; b } ; f(list(TRUE,NA),FALSE) ; f(3,3) }");
    }

    @Test
    public void TestSimpleVectors_testScalarUpdate_ed0b6d40724bc9d00c060097f577deaa() {
        assertEval("{ f <- function(b,v) { b[[2]] <- v ; b } ; f(list(TRUE,NA),FALSE) ; f(list(3),NULL) }");
    }

    @Test
    public void TestSimpleVectors_testScalarUpdate_d7243a02eb759376db48acb9c7495355() {
        assertEval("{ f <- function(b,v) { b[[2]] <- v ; b } ; f(list(TRUE,NA),FALSE) ; f(list(),NULL) }");
    }

    @Test
    public void TestSimpleVectors_testScalarUpdate_41f085cedcfebff98ca2a5d2bab855f4() {
        assertEval("{ b <- c(\"a\",\"b\") ; z <- b ; b[[-1L]] <- \"xx\" ; b }");
    }

    @Test
    public void TestSimpleVectors_testScalarUpdate_eae903d16764c465fae7c81ca0349474() {
        assertEval("{ b <- c(1,2) ; b[3] <- 2+3i ; b }");
    }

    @Test
    public void TestSimpleVectors_testScalarUpdate_2b6757962f461360d90b42355bab4b21() {
        assertEval("{ b <- c(1+2i,3+4i) ; b[3] <- 2 ; b }");
    }

    @Test
    public void TestSimpleVectors_testScalarUpdate_a9edb417f38dc2568a4fdcce3bfd6ee3() {
        assertEval("{ b <- c(TRUE,NA) ; b[3] <- FALSE ; b }");
    }

    @Test
    public void TestSimpleVectors_testScalarUpdate_068ba6690f99b629bbe682042335907b() {
        assertEval("{ b <- as.raw(c(1,2)) ; b[3] <- as.raw(13) ; b }");
    }

    @Test
    public void TestSimpleVectors_testScalarUpdate_59d1b91c04f4ca4f67683ed08e843ae7() {
        assertEval("{ b <- as.raw(c(1,2)) ; b[as.double(NA)] <- as.raw(13) ; b }");
    }

    @Test
    public void TestSimpleVectors_testScalarUpdate_1ffb8c5e883b8bf91aa538a5f4d220bb() {
        assertEval("{ b <- as.raw(c(1,2)) ; b[[-2]] <- as.raw(13) ; b }");
    }

    @Test
    public void TestSimpleVectors_testScalarUpdate_cd23a14fa39c5a10a6e36a79db1fe61c() {
        assertEval("{ b <- as.raw(c(1,2)) ; b[[-1]] <- as.raw(13) ; b }");
    }

    @Test
    public void TestSimpleVectors_testScalarUpdate_b92849ae98b7565295dea93b8d4793c0() {
        assertEval("{ x <- c(a=1+2i, b=3+4i) ; x[\"a\"] <- 10 ; x }");
    }

    @Test
    public void TestSimpleVectors_testScalarUpdate_9813b046264cd033c8c326524b01e8d2() {
        assertEval("{ x <- as.raw(c(10,11)) ; x[\"a\"] <- as.raw(13) ; x }");
    }

    @Test
    public void TestSimpleVectors_testScalarUpdate_a3706cf0ecf4156cb325186f3ba447b5() {
        assertEval("{ x <- 1:2 ; x[\"a\"] <- 10+3i ; x }");
    }

    @Test
    public void TestSimpleVectors_testScalarUpdate_0916411a0792e5905ebd846ea5400c29() {
        assertEval("{ x <- c(a=1+2i, b=3+4i) ; x[\"a\"] <- \"hi\" ; x }");
    }

    @Test
    public void TestSimpleVectors_testScalarUpdate_2d98f6458019419db8ccdd71ceeab7e1() {
        assertEval("{ x <- 1:2 ; x[\"a\"] <- 10 ; x }");
    }

    @Test
    public void TestSimpleVectors_testScalarUpdate_700d0e26aef40368482e1a14d19b54e0() {
        assertEval("{ x <- c(a=1,a=2) ; x[\"a\"] <- 10L ; x }");
    }

    @Test
    public void TestSimpleVectors_testScalarUpdate_e2ae7fb13556e39f9289216c5fbe79b1() {
        assertEval("{ x <- 1:2 ; x[\"a\"] <- FALSE ; x }");
    }

    @Test
    public void TestSimpleVectors_testScalarUpdate_6fc1510cfd7c006e8a912e1671971dc2() {
        assertEval("{ x <- c(aa=TRUE,b=FALSE) ; x[\"a\"] <- 2L ; x }");
    }

    @Test
    public void TestSimpleVectors_testScalarUpdate_6058b61140ee06ea8ee2227d1624bff0() {
        assertEval("{ x <- c(aa=TRUE) ; x[[\"a\"]] <- list(2L) ; x }");
    }

    @Test
    public void TestSimpleVectors_testScalarUpdate_6381a2f9c4009355ab756e7b6fd00f27() {
        assertEval("{ x <- c(aa=TRUE) ; x[\"a\"] <- list(2L) ; x }");
    }

    @Test
    public void TestSimpleVectors_testScalarUpdate_6fda0f6224a3768ce6c4528a028d876f() {
        assertEval("{ x <- c(b=2,a=3) ; z <- x ; x[\"a\"] <- 1 ; x }");
    }

    @Test
    public void TestSimpleVectors_testScalarUpdate_96747cf328bcda8d480b4933f3fbd06d() {
        assertEval("{ x <- list(1,2) ; dim(x) <- c(2,1) ; x[[3]] <- NULL ; x }");
    }

    @Test
    public void TestSimpleVectors_testScalarUpdate_85c6524b9cd048047d9459b2ee4162a9() {
        assertEval("{ x <- list(1,2) ; dim(x) <- c(2,1) ; x[3] <- NULL ; x }");
    }

    @Test
    public void TestSimpleVectors_testScalarUpdate_fa7d79194cee9353e9a1c5ed0fd99030() {
        assertEval("{ x <- list(1,2) ; dim(x) <- c(2,1) ; x[2] <- NULL ; x }");
    }

    @Test
    public void TestSimpleVectors_testScalarUpdate_e16c530517ace801566b228f09c05555() {
        assertEval("{ x <- list(1,2) ; dim(x) <- c(2,1) ; x[[2]] <- NULL ; x }");
    }

    @Test
    public void TestSimpleVectors_testScalarUpdate_88cd04f30ca7f57580810d3b38e87a56() {
        assertEval("{ x <- list(1,2) ; x[0] <- NULL ; x }");
    }

    @Test
    public void TestSimpleVectors_testScalarUpdate_e326e218c4a0ad4809bbb03d95f99d3f() {
        assertEval("{ x <- list(1,2) ; x[NA] <- NULL ; x }");
    }

    @Test
    public void TestSimpleVectors_testScalarUpdate_c8b80e30ebc723cc1687b326c0dbd714() {
        assertEval("{ x <- list(1,2) ; x[as.integer(NA)] <- NULL ; x }");
    }

    @Test
    public void TestSimpleVectors_testScalarUpdate_07ddaa1b0bef9cb2f000e823cc10cd64() {
        assertEval("{ x <- list(1,2) ; x[-1] <- NULL ; x }");
    }

    @Test
    public void TestSimpleVectors_testScalarUpdate_73b3ef0cadcf0155e0fa48188e105238() {
        assertEval("{ x <- list(3,4) ; x[[-1]] <- NULL ; x }");
    }

    @Test
    public void TestSimpleVectors_testScalarUpdate_5d3b89a0e736ebb6b3ede5606165db2c() {
        assertEval("{ x <- list(3,4) ; x[[-2]] <- NULL ; x }");
    }

    @Test
    public void TestSimpleVectors_testScalarUpdate_f490d19ba1685f69c0f8cf16f5c576cf() {
        assertEval("{ x <- list(a=3,b=4) ; x[[\"a\"]] <- NULL ; x }");
    }

    @Test
    public void TestSimpleVectors_testScalarUpdate_360c2b664487b9e657393bf83af15aab() {
        assertEval("{ x <- list(a=3,b=4) ; x[\"z\"] <- NULL ; x }");
    }

    @Test
    public void TestSimpleVectors_testScalarUpdate_b1fc14cec6fb9378d456bd97fa340ad0() {
        assertEval("{ f <- function(b,i,v) { b[i] <- v ; b } ; f(1:2,\"hi\",3L) ; f(1:2,-2,10) }");
    }

    @Test
    public void TestSimpleVectors_testScalarUpdate_d7c0586ebe0e3e94c6d0a9ab113c2899() {
        assertEval("{ f <- function(b,i,v) { b[i] <- v ; b } ; f(1:2,\"hi\",3L) ; f(1:2,2,10) ; f(1:2,as.integer(NA), 10) }");
    }

    @Test
    public void TestSimpleVectors_testScalarUpdate_01f92837efb935610413a74c701011e7() {
        assertEval("{ f <- function(b,i,v) { b[[i]] <- v ; b } ; f(1:2,\"hi\",3L) ; f(1:2,c(2),10) ; f(1:2,2, 10) }");
    }

    @Test
    public void TestSimpleVectors_testScalarUpdate_69b9e2ceb3a5b23bf18f03992b4e33d8() {
        assertEval("{ b <- list(1+2i,3+4i) ; dim(b) <- c(2,1) ; b[3] <- NULL ; b }");
    }

    @Test
    public void TestSimpleVectors_testScalarUpdate_86242473cde6cae942caf5e96cfdc0ed() {
        assertEvalError(" { x <- as.raw(c(10,11)) ; x[\"a\"] <- NA ; x }");
    }

    @Test
    public void TestSimpleVectors_testScalarUpdate_11bbf13cd150265efe922bf333149e5e() {
        assertEvalError(" { x <- c(a=1+2i, b=3+4i) ; x[\"a\"] <- as.raw(13) ; x }");
    }

    @Test
    public void TestSimpleVectors_testScalarUpdate_e477ca2490aca17f6d679ee10e03ceb4() {
        assertEvalError("{ x[3] <<- 10 }");
    }

    @Test
    public void TestSimpleVectors_testScalarUpdate_d3c3a8bf3f8f61cc3da1a605c1ae2a75() {
        assertEvalError("{ f <- function() { a[3] <- 4 } ; f() }");
    }

    @Test
    public void TestSimpleVectors_testScalarUpdate_f2c88bbd64008e434cf221d0733b708b() {
        assertEvalError("{ b <- as.raw(c(1,2)) ; b[3] <- 3 ; b }");
    }

    @Test
    public void TestSimpleVectors_testScalarUpdate_bc0b56938a5819cdc8609b3c13b03771() {
        assertEvalError("{ b <- c(1,2) ; b[3] <- as.raw(13) ; b }");
    }

    @Test
    public void TestSimpleVectors_testScalarUpdate_72fad591b0bc7a6459c56705eb534968() {
        assertEvalError("{ b <- as.raw(c(1,2)) ; b[[-3]] <- as.raw(13) ; b }");
    }

    @Test
    public void TestSimpleVectors_testScalarUpdate_71d20d0b5ed6ecc4c7593baac455d304() {
        assertEvalError("{ b <- as.raw(1) ; b[[-3]] <- as.raw(13) ; b }");
    }

    @Test
    public void TestSimpleVectors_testScalarUpdate_8a2006e6c8daf17fed16797f2942035e() {
        assertEvalError("{ b <- as.raw(c(1,2,3)) ; b[[-2]] <- as.raw(13) ; b }");
    }

    @Test
    public void TestSimpleVectors_testScalarUpdate_2699c4ed4193ac15a24a972c48bf1ca4() {
        assertEvalError("{ f <- function(b,i) { b[i] <- 1 } ; f(1:3,2) ; f(f, 3) }");
    }

    @Test
    public void TestSimpleVectors_testScalarUpdate_6260b86ea2fc061ba85e805105874a62() {
        assertEvalError("{ f <- function(b,i) { b[i] <- 1 } ; f(1:3,2) ; f(1:2, f) }");
    }

    @Test
    public void TestSimpleVectors_testScalarUpdate_5f45d32b5b8d5cf5e353115ccb5d396c() {
        assertEvalError("{ f <- function(b,v) { b[2] <- v } ; f(1:3,2) ; f(1:2, f) }");
    }

    @Test
    public void TestSimpleVectors_testScalarUpdate_97b15de22e1d258a8ef36b1003b3b24f() {
        assertEvalError("{ x <- list(1,2) ; x[[0]] <- NULL ; x }");
    }

    @Test
    public void TestSimpleVectors_testScalarUpdate_d4d9108a48aa07b8b546b6c17db77782() {
        assertEvalError("{ x <- list(1,2,3) ; x[[-1]] <- NULL ; x }");
    }

    @Test
    public void TestSimpleVectors_testScalarUpdate_fb459b7a5e9ab9b6f13b0d3867ca38a5() {
        assertEvalError("{ x <- list(1,2,3) ; x[[-5]] <- NULL ; x }");
    }

    @Test
    public void TestSimpleVectors_testScalarUpdate_ca9e562efaeaee4ea3ec5bb77022a370() {
        assertEvalError("{ x <- list(1) ; x[[-2]] <- NULL ; x }");
    }

    @Test
    public void TestSimpleVectors_testScalarUpdate_7f268186425145a348dae1bd433fb1f6() {
        assertEvalError("{ x <- list(1) ; x[[-1]] <- NULL ; x }");
    }

    @Test
    public void TestSimpleVectors_testScalarUpdate_2478e3df555252e501a817519339308e() {
        assertEvalError("{ x <- list(3,4) ; x[[-10]] <- NULL ; x }");
    }

    @Test
    public void TestSimpleVectors_testScalarUpdate_1ed0262879c8fed775c33cc17879dfef() {
        assertEvalError("{ x <- 1:2; x[[as.integer(NA)]] <- 10 ; x }");
    }

    @Test
    public void TestSimpleVectors_testScalarUpdate_7646c2822df116ef3b7f44412b9bb139() {
        assertEvalError("{ f <- function(b,i,v) { b[[i]] <- v ; v } ; f(1:2,\"hi\",3L) ; f(1:2,c(2),10) ; f(1:2,as.integer(NA), 10) }");
    }

    @Test
    public void TestSimpleVectors_testScalarUpdate_4e6c90f74e044e44e246aefb64415b9f() {
        assertEvalError("{ f <- function(b,i,v) { b[[i]] <- v ; b } ; f(1:2,\"hi\",3L) ; f(1:2,c(2),10) ; f(1:2,0, 10) }");
    }

    @Test
    public void TestSimpleVectors_testScalarUpdate_47685573f1e5dbe725ec41ce2f33415e() {
        assertEvalError("{ f <- function(b,i,v) { b[[i]] <- v ; b } ; f(1:2,\"hi\",3L) ; f(1:2,2,10) ; f(1:2,1:3, 10) }");
    }

    @Test
    public void TestSimpleVectors_testScalarUpdate_ad5342fcc8975e60a34efea30e8babeb() {
        assertEvalError("{ f <- function(b,i,v) { b[[i]] <- v ; b } ; f(1:2,\"hi\",3L) ; f(1:2,2,10) ; f(as.list(1:2),1:3, 10) }");
    }

    @Test
    public void TestSimpleVectors_testScalarUpdate_800f4af3f0feda75c8b259ad846f4937() {
        assertEvalWarning("{ b <- c(1,2) ; z <- c(10,11) ; attr(z,\"my\") <- 4 ; b[2] <- z ; b }");
    }

    @Test
    public void TestSimpleVectors_testScalarUpdateIgnore_ea7d1aaf03e73608bdd0d9114c96e3a8() {
        assertEvalError("{ f <- function(b,v) { b[[2]] <- v ; b } ; f(c(\"a\",\"b\"),\"d\") ; f(c(\"a\",\"b\"),NULL) }");
    }

    @Test
    public void TestSimpleVectors_testScalarUpdateIgnore_5d198ef5c0421165963dc6da0d622857() {
        assertEvalError("{ x <- 4:10 ; x[[\"z\"]] <- NULL ; x }");
    }

    @Test
    public void TestSimpleVectors_testSequenceIntIndexOnVector_f87f94af38ac07f3ec7eeef63f9e6c5b() {
        assertEval("{ x<-c(1L,2L,3L); x[1:3] }");
    }

    @Test
    public void TestSimpleVectors_testSequenceIntIndexOnVector_72a246974ad03d0631cdfcec7fa15812() {
        assertEval("{ x<-c(1L,2L,3L); x[1:2] }");
    }

    @Test
    public void TestSimpleVectors_testSequenceIntIndexOnVector_a5c7528e0c93b870a0ebf43e500f1f5c() {
        assertEval("{ x<-c(1L,2L,3L); x[2:3] }");
    }

    @Test
    public void TestSimpleVectors_testSequenceIntIndexOnVector_6059427aca51482301c13e3d036252ac() {
        assertEval("{ x<-c(1L,2L,3L); x[1:1] }");
    }

    @Test
    public void TestSimpleVectors_testSequenceIntIndexOnVector_3af98990dca78bc5cbd813c711e3031e() {
        assertEval("{ x<-c(1L,2L,3L); x[0:3] }");
    }

    @Test
    public void TestSimpleVectors_testSequenceIntIndexOnVector_0c72fe749ea5f7a9af012e00b32e92b4() {
        assertEval("{ x<-c(1,2,3); x[1:3] }");
    }

    @Test
    public void TestSimpleVectors_testSequenceIntIndexOnVector_b664ec8d1c5d0b69173e9cd6f31c6d61() {
        assertEval("{ x<-c(1,2,3); x[1:2] }");
    }

    @Test
    public void TestSimpleVectors_testSequenceIntIndexOnVector_cf37af8bafe041e6819b0a77b2858cfb() {
        assertEval("{ x<-c(1,2,3); x[2:3] }");
    }

    @Test
    public void TestSimpleVectors_testSequenceIntIndexOnVector_d36a31b65d4fe2351d9413634e8a68db() {
        assertEval("{ x<-c(1,2,3); x[1:1] }");
    }

    @Test
    public void TestSimpleVectors_testSequenceIntIndexOnVector_542954c947aa234e6f838c1d464d4b2b() {
        assertEval("{ x<-c(1,2,3); x[0:3] }");
    }

    @Test
    public void TestSimpleVectors_testStringUpdate_04dd040eb2169fe380ffccd8c938f936() {
        assertEval("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(1:3,3:1,4:6) ; f(1:3,\"a\",4) }");
    }

    @Test
    public void TestSimpleVectors_testStringUpdate_f21a624532eed1d675234ce25aaa2bd4() {
        assertEval("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(1:3,3:1,4:6) ; f(NULL,\"a\",4) }");
    }

    @Test
    public void TestSimpleVectors_testStringUpdate_bafd686b1681f29b7d44db9d25af274c() {
        assertEval("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(1:3,3:1,4:6) ; f(NULL,c(\"a\",\"X\"),4:5) }");
    }

    @Test
    public void TestSimpleVectors_testStringUpdate_defc66b21c157803ff71db40d58d2d9d() {
        assertEval("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(1:3,3:1,4:6) ; f(double(),c(\"a\",\"X\"),4:5) }");
    }

    @Test
    public void TestSimpleVectors_testStringUpdate_29ce0ae561f19f3d09c4dc651d2970f1() {
        assertEval("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(1:3,3:1,4:6) ; f(double(),c(\"a\",\"X\"),list(3,TRUE)) }");
    }

    @Test
    public void TestSimpleVectors_testStringUpdate_dac9b308751c7c38199bc8bf25ac93b5() {
        assertEval("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(1:3,3:1,4:6) ; f(as.raw(11:13),c(\"a\",\"X\"),list(3,TRUE)) }");
    }

    @Test
    public void TestSimpleVectors_testStringUpdate_f965bd09034ad4389fa52695a15e6b01() {
        assertEval("{ b <- c(11,12) ; b[\"\"] <- 100 ; b }");
    }

    @Test
    public void TestSimpleVectors_testStringUpdate_d9917231155acd7416cc8a85cc717000() {
        assertEval("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(1:3,3:1,4:6) ; f(c(1,a=2),c(\"a\",\"X\",\"a\"),list(3,TRUE,FALSE)) }");
    }

    @Test
    public void TestSimpleVectors_testStringUpdate_f11755d762e9ef159acc635ed47dd77f() {
        assertEval("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(1:3,3:1,4:6) ; f(c(X=1,a=2),c(\"a\",\"X\",\"a\"),list(3,TRUE,FALSE)) }");
    }

    @Test
    public void TestSimpleVectors_testStringUpdate_b83220dcfa819d5242db01ba3fecf03e() {
        assertEval("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(1:3,3:1,4:6) ; f(as.complex(c(13,14)),as.character(NA),as.complex(23)) }");
    }

    @Test
    public void TestSimpleVectors_testStringUpdate_919b1dc44ca6a21451268dfaf228c669() {
        assertEval("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(1:3,3:1,4:6) ; f(as.complex(c(13,14)),character(),as.complex(23)) }");
    }

    @Test
    public void TestSimpleVectors_testStringUpdate_9be8133d1385917a65d1536ed7c04e05() {
        assertEval("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(1:3,3:1,4:6) ; f(as.complex(c(13,14)),c(\"\",\"\",\"\"),as.complex(23)) }");
    }

    @Test
    public void TestSimpleVectors_testStringUpdate_58f1ce6bdefdfa1e82ae52d5251a3642() {
        assertEval("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(1:3,3:1,4:6) ; f(as.complex(c(13,14)),c(\"\",\"\",NA),as.complex(23)) }");
    }

    @Test
    public void TestSimpleVectors_testStringUpdate_726ca56243c5b31dc6353d6283f93dd4() {
        assertEval("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(1:3,3:1,4:6) ; f(as.raw(c(13,14)),c(\"a\",\"X\",\"a\"),as.raw(23)) }");
    }

    @Test
    public void TestSimpleVectors_testStringUpdate_4bdda40e8292b1ec15f0a7ea8da3e1b2() {
        assertEval("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(1:3,3:1,4:6) ; f(c(X=1,a=2),c(\"X\",\"b\",NA),list(3,TRUE,FALSE)) }");
    }

    @Test
    public void TestSimpleVectors_testStringUpdate_b6c9fb4353a2d30421ea074892567475() {
        assertEval("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(1:3,3:1,4:6) ; f(c(X=1,a=2),c(\"X\",\"b\",NA),as.complex(10)) }");
    }

    @Test
    public void TestSimpleVectors_testStringUpdate_67de1a7f667ccbe9df399b861d8a7778() {
        assertEval("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(1:3,3:1,4:6) ; f(c(X=1,a=2),c(\"X\",\"b\",NA),1:3) }");
    }

    @Test
    public void TestSimpleVectors_testStringUpdate_d5f7bf583d66c76ff0eb584d1095b89d() {
        assertEval("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(1+2i,3:1,4:6) ; f(c(X=1L,a=2L),c(\"X\",\"b\",NA),c(TRUE,NA,FALSE)) }");
    }

    @Test
    public void TestSimpleVectors_testStringUpdate_2f20afd1c787c8770cdf17eba8351445() {
        assertEval("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(1+2i,3:1,4:6) ; f(list(X=1L,a=2L),c(\"X\",\"b\",NA),NULL) }");
    }

    @Test
    public void TestSimpleVectors_testStringUpdate_67cbb94b38b7c299bdf566625150d25d() {
        assertEval("{ b <- c(a=1+2i,b=3+4i) ; dim(b) <- c(2,1) ; b[c(\"a\",\"b\")] <- 3+1i ; b }");
    }

    @Test
    public void TestSimpleVectors_testStringUpdate_288c45431beb53217738e85c6810b820() {
        assertEval("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(1+2i,3:1,4:6) ; b <- list(1L,2L) ; attr(b,\"my\") <- 21 ; f(b,c(\"X\",\"b\",NA),NULL) }");
    }

    @Test
    public void TestSimpleVectors_testStringUpdate_b9dd3e19248b0a0f6bd00be0c36a0f61() {
        assertEval("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(1+2i,3:1,4:6) ; b <- list(b=1L,2L) ; attr(b,\"my\") <- 21 ; f(b,c(\"X\",\"b\",NA),NULL) }");
    }

    @Test
    public void TestSimpleVectors_testStringUpdate_759b06ffb7b2d1a031772f2245984863() {
        assertEval("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(1+2i,3:1,4:6) ; b <- list(b=1L,2L) ; attr(b,\"my\") <- 21 ; f(b,c(\"ZZ\",\"ZZ\",NA),NULL) }");
    }

    @Test
    public void TestSimpleVectors_testStringUpdate_2c73425a8453c9bcad083b8f33ae6591() {
        assertEval("{ b <- list(1+2i,3+4i) ; dim(b) <- c(2,1) ; b[c(\"hello\",\"hi\")] <- NULL ; b }");
    }

    @Test
    public void TestSimpleVectors_testStringUpdate_dff56fd25747041574656cf30f3969a0() {
        assertEval("{ a <- 'hello'; a[[5]] <- 'done'; a[[3]] <- 'muhuhu'; a; }");
    }

    @Test
    public void TestSimpleVectors_testStringUpdate_6318de4011edc9ab9ad909c87f47357e() {
        assertEval("{ a <- 'hello'; a[[5]] <- 'done'; b <- a; b[[3]] <- 'muhuhu'; b; }");
    }

    @Test
    public void TestSimpleVectors_testStringUpdate_a3c4465b74a83763136b074639150397() {
        assertEvalError("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(1:3,3:1,4:6) ; f(as.raw(c(13,14)),c(\"a\",\"X\",\"a\"),c(3,TRUE,FALSE)) }");
    }

    @Test
    public void TestSimpleVectors_testStringUpdate_1fc4ff687d23d0ada3a07d3cae6350bd() {
        assertEvalError("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(1:3,3:1,4:6) ; f(c(X=1,a=2),c(\"X\",\"b\",NA),as.raw(10)) }");
    }

    @Test
    public void TestSimpleVectors_testStringUpdate_a0768f399d4477f4a763b9d1273171e2() {
        assertEvalError("{ f <- function(b, i, v) { b[[i]] <- v ; b } ; f(1+2i,3:1,4:6) ; f(c(X=1L,a=2L),c(\"X\",\"b\",NA),NULL) }");
    }

    @Test
    public void TestSimpleVectors_testStringUpdate_5c20aceeb6e5236ec88efed5c1fec386() {
        assertEvalWarning("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(1:3,3:1,4:6) ; f(c(X=1,a=2),c(\"a\",\"X\",\"a\",\"b\"),list(3,TRUE,FALSE)) }");
    }

    @Test
    public void TestSimpleVectors_testStringUpdate_8f118953ceadc684e87681bfe249ef23() {
        assertEvalWarning("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(1+2i,3:1,4:6) ; f(c(X=1,a=2),c(\"X\",\"b\",NA),c(TRUE,NA)) }");
    }

    @Test
    public void TestSimpleVectors_testSuperUpdate_74f7675230b3e29fa70ac5e28d9db039() {
        assertEval("{ x <- 1:3 ; f <- function() { x[2] <<- 100 } ; f() ; x }");
    }

    @Test
    public void TestSimpleVectors_testSuperUpdate_10ce79326af7e07dc8525793ffb4c8bc() {
        assertEval("{ x <- 1:3 ; f <- function() { x[2] <- 10 ; x[2] <<- 100 ; x[2] <- 1000 } ; f() ; x }");
    }

    @Test
    public void TestSimpleVectors_testUpdateOther_eea525ae4479446e708a52622475cd5b() {
        assertEval("{ f<-function() { print(`*tmp*`[2]); `*tmp*`[2]<-7; 1 } ; x<-c(1,2); x[f()]<-42; x }");
    }

    @Test
    public void TestSimpleVectors_testUpdateOther_7994f543433d0239668974a5699941da() {
        assertEval("{ f<-function() { print(`*tmp*`[2]); `*tmp*`[2]<<-7; 1 } ; x<-c(1,2); x[f()]<-42; x }");
    }

    @Test
    public void TestSimpleVectors_testUpdateOther_8f47617a6b12ce7fa5b41d5ce455b89e() {
        assertEval("{ x<-c(1,2); f<-function() { x<-c(100, 200); x[1]<-4; print(x) } ; f(); x }");
    }

    @Test
    public void TestSimpleVectors_testUpdateOther_f6ccb4168af3fd4313e35696afc3f2f5() {
        assertEval("{ x<-c(1,2); x[1]<-42; `*tmp*`[1]<-7; x }");
    }

    @Test
    public void TestSimpleVectors_testUpdateOther_2a527d7409757c6f8ae809606cf60294() {
        assertEval("{ x<-c(1,2); f<-function() { x<-c(100, 200); x[1]<<-4; print(x) } ; f(); x }");
    }

    @Test
    public void TestSimpleVectors_testVectorIndex_1d6d1e8273a075727e7f7ee39bf9060e() {
        assertEval(" { f <- function(b,i) { b[i] } ; f(c(a=1,b=2,c=3), c(TRUE,NA)) }");
    }

    @Test
    public void TestSimpleVectors_testVectorIndex_89bfe7fcf10b4e29a54200e286cfc35b() {
        assertEval("{ x<-1:5 ; x[3:4] }");
    }

    @Test
    public void TestSimpleVectors_testVectorIndex_038f8130e57e9b029bf4b9be8f223309() {
        assertEval("{ x<-1:5 ; x[4:3] }");
    }

    @Test
    public void TestSimpleVectors_testVectorIndex_c5f21663e9b4358771bc856b2e954943() {
        assertEval("{ x<-c(1,2,3,4,5) ; x[4:3] }");
    }

    @Test
    public void TestSimpleVectors_testVectorIndex_6294aa6aa11da33aad4542c1b41e75a8() {
        assertEval("{ (1:5)[3:4] }");
    }

    @Test
    public void TestSimpleVectors_testVectorIndex_e159bad694b11a98bd87ecab70cb33d2() {
        assertEval("{ x<-(1:5)[2:4] ; x[2:1] }");
    }

    @Test
    public void TestSimpleVectors_testVectorIndex_b134fcba1bb5376ea04f0c07c5440557() {
        assertEval("{ x<-1:5;x[c(0-2,0-3)] }");
    }

    @Test
    public void TestSimpleVectors_testVectorIndex_9a24c680cb03f7fc0a65c45c58c35b90() {
        assertEval("{ x<-1:5;x[c(0-2,0-3,0,0,0)] }");
    }

    @Test
    public void TestSimpleVectors_testVectorIndex_69262108a14f4dd23aa3534682e10a09() {
        assertEval("{ x<-1:5;x[c(2,5,4,3,3,3,0)] }");
    }

    @Test
    public void TestSimpleVectors_testVectorIndex_77654a46937a1e3d1c705d9e9f24ad23() {
        assertEval("{ x<-1:5;x[c(2L,5L,4L,3L,3L,3L,0L)] }");
    }

    @Test
    public void TestSimpleVectors_testVectorIndex_0f8f9d829a0a4aa8696219c8234d12ff() {
        assertEval("{ f<-function(x, i) { x[i] } ; f(1:3,3:1) ; f(1:5,c(0,0,0,0-2)) }");
    }

    @Test
    public void TestSimpleVectors_testVectorIndex_6563f81f22a1a2f3a1bcd43790ae540b() {
        assertEval("{ f<-function(x, i) { x[i] } ; f(1:3,0-3) ; f(1:5,c(0,0,0,0-2)) }");
    }

    @Test
    public void TestSimpleVectors_testVectorIndex_7841d50376f3b003ba259a909d9f9d89() {
        assertEval("{ f<-function(x, i) { x[i] } ; f(1:3,0L-3L) ; f(1:5,c(0,0,0,0-2)) }");
    }

    @Test
    public void TestSimpleVectors_testVectorIndex_8f9eb3b677a98fcca4093568fd2f6806() {
        assertEval("{ x<-1:5 ; x[c(TRUE,FALSE)] }");
    }

    @Test
    public void TestSimpleVectors_testVectorIndex_fe16cd33ba64644aa58087446523a555() {
        assertEval("{ f<-function(i) { x<-1:5 ; x[i] } ; f(1) ; f(1L) ; f(TRUE) }");
    }

    @Test
    public void TestSimpleVectors_testVectorIndex_7194d998da3cc9c3b40f1de1bdff1038() {
        assertEval("{ f<-function(i) { x<-1:5 ; x[i] } ; f(1) ; f(TRUE) ; f(1L)  }");
    }

    @Test
    public void TestSimpleVectors_testVectorIndex_59f7f0ba74c9a57502cccbe94a6ae0eb() {
        assertEval("{ f<-function(i) { x<-1:5 ; x[i] } ; f(1) ; f(TRUE) ; f(c(3,2))  }");
    }

    @Test
    public void TestSimpleVectors_testVectorIndex_131179270ce4f6c5bc33e965bc0dd1f8() {
        assertEval("{ f<-function(i) { x<-1:5 ; x[i] } ; f(1)  ; f(3:4) }");
    }

    @Test
    public void TestSimpleVectors_testVectorIndex_fa8b22ee08dda896a304f531518829d5() {
        assertEval("{ f<-function(i) { x<-1:5 ; x[i] } ; f(c(TRUE,FALSE))  ; f(3:4) }");
    }

    @Test
    public void TestSimpleVectors_testVectorIndex_c10170c362d9184b48bcd3d2191d5aa9() {
        assertEval("{ x <- 1;  y<-c(1,1) ; x[y] }");
    }

    @Test
    public void TestSimpleVectors_testVectorIndex_19cbce3cfec49b2bbdeabf71b3e750d8() {
        assertEval("{ x <- 1L;  y<-c(1,1) ; x[y] }");
    }

    @Test
    public void TestSimpleVectors_testVectorIndex_e8b5e6259ff56e2e9090a309cb4c42db() {
        assertEval("{ x <- c(1,2,3,2) ; x[x==2] }");
    }

    @Test
    public void TestSimpleVectors_testVectorIndex_c656fe910ffd3c52bc68e9c72a7f8c34() {
        assertEval("{ x <- c(1,2,3,2) ; x[c(3,4,2)==2] }");
    }

    @Test
    public void TestSimpleVectors_testVectorIndex_0af9433d33250581cf2ced5feb7ed146() {
        assertEval("{ x <- c(as.double(1:2000)) ; x[c(1,3,3,3,1:1996)==3] }");
    }

    @Test
    public void TestSimpleVectors_testVectorIndex_95c002a0ab074a3e60bb9ac1c84d9cef() {
        assertEval("{ x <- c(as.double(1:2000)) ; sum(x[rep(3, 2000)==3]) }");
    }

    @Test
    public void TestSimpleVectors_testVectorIndex_3edc24d04fed3fc7d8f955bc1fc5d003() {
        assertEval("{ x <- c(1,2,3,2) ; x[c(3,4,2,NA)==2] }");
    }

    @Test
    public void TestSimpleVectors_testVectorIndex_9f5ad5b66c3181749fc3d13839ba5b15() {
        assertEval("{ f <- function(b,i) { b[i] } ; f(1:3, c(TRUE,FALSE,TRUE)) ; f(1:3,3:1) }");
    }

    @Test
    public void TestSimpleVectors_testVectorIndex_bb3a6b5ff8f80d80d3c2ce954ecc989b() {
        assertEval("{ f <- function(b,i) { b[i] } ; f(1:3,c(2,1)) ; f(1:3,c(TRUE,FALSE)) }");
    }

    @Test
    public void TestSimpleVectors_testVectorIndex_54bd55084aee7eae8dbb8fddcf0560a6() {
        assertEval("{ f <- function(b,i) { b[i] } ; f(1:3,c(2,1)) ; f(1:3,NULL) }");
    }

    @Test
    public void TestSimpleVectors_testVectorIndex_afb5f2d66fc95a3a31b530cf041c2ed6() {
        assertEval("{ x <- \"hi\";  y<-c(1,1) ; x[y] }");
    }

    @Test
    public void TestSimpleVectors_testVectorIndex_735c444595fe843fd5efb95ee1f0a3bd() {
        assertEval("{ l <- list(1,function(){3}) ; f <- function(i) { l[[i]] } ; f(c(2)) }");
    }

    @Test
    public void TestSimpleVectors_testVectorIndex_7a6d17fe063a5585f7aa59e7f27b18bf() {
        assertEval("{ a <- c(1,2,3) ; x <- integer() ; a[x] }");
    }

    @Test
    public void TestSimpleVectors_testVectorIndex_1f99c587df8881ae8bb4134fbb911f66() {
        assertEval("{ f <- function(i) { l[[i]] } ; l <- list(1, as.list(1:3)) ; f(c(2,NA)) }");
    }

    @Test
    public void TestSimpleVectors_testVectorIndex_643519b0c5955f38e43859fb87074617() {
        assertEval("{ x<-1:5 ; x[c(TRUE,TRUE,TRUE,NA)] }");
    }

    @Test
    public void TestSimpleVectors_testVectorIndex_faa1957919a5c0f09bb51d732dce723a() {
        assertEval("{ x<-1:5 ; x[c(TRUE,TRUE,TRUE,FALSE,FALSE,FALSE,FALSE,TRUE,NA)] }");
    }

    @Test
    public void TestSimpleVectors_testVectorIndex_eb8919c4f7927219c30b474aa7e88581() {
        assertEval("{ x<-as.complex(c(1,2,3,4)) ; x[2:4] }");
    }

    @Test
    public void TestSimpleVectors_testVectorIndex_82df4ecb0270888e2f978c9dd14d807c() {
        assertEval("{ x<-as.raw(c(1,2,3,4)) ; x[2:4] }");
    }

    @Test
    public void TestSimpleVectors_testVectorIndex_c643b834c198e654256294f03e0def64() {
        assertEval("{ x<-c(1,2,3,4) ; names(x) <- c(\"a\",\"b\",\"c\",\"d\") ; x[c(10,2,3,0)] }");
    }

    @Test
    public void TestSimpleVectors_testVectorIndex_0b905d5671476c428b89d1f53a9ff957() {
        assertEval("{ x<-c(1,2,3,4) ; names(x) <- c(\"a\",\"b\",\"c\",\"d\") ; x[c(10,2,3)] }");
    }

    @Test
    public void TestSimpleVectors_testVectorIndex_5893865439ae18e5e702e6c6ee21e280() {
        assertEval("{ x<-c(1,2,3,4) ; names(x) <- c(\"a\",\"b\",\"c\",\"d\") ; x[c(-2,-4,0)] }");
    }

    @Test
    public void TestSimpleVectors_testVectorIndex_4125235d5782f62ad9c991f92f40e9b6() {
        assertEval("{ x<-c(1,2) ; names(x) <- c(\"a\",\"b\") ; x[c(FALSE,TRUE,NA,FALSE)] }");
    }

    @Test
    public void TestSimpleVectors_testVectorIndex_52b22acb2389bb8429f984c35c1909c4() {
        assertEval("{ x<-c(1,2) ; names(x) <- c(\"a\",\"b\") ; x[c(FALSE,TRUE)] }");
    }

    @Test
    public void TestSimpleVectors_testVectorIndex_3f82bbe7484aff9273391843a15be6b0() {
        assertEval("{ x <- c(a=1,b=2,c=3,d=4) ; x[character()] }");
    }

    @Test
    public void TestSimpleVectors_testVectorIndex_0065b5b7400cf013c14e5a52edae7c26() {
        assertEval("{ x <- c(a=1,b=2,c=3,d=4) ; x[c(\"b\",\"b\",\"d\",\"a\",\"a\")] }");
    }

    @Test
    public void TestSimpleVectors_testVectorIndex_31bb531d3fe326a293bcc0d468cddd90() {
        assertEval("{ x <- c(a=as.raw(10),b=as.raw(11),c=as.raw(12),d=as.raw(13)) ; f <- function(s) { x[s] } ; f(TRUE) ; f(1L) ; f(as.character(NA)) }");
    }

    @Test
    public void TestSimpleVectors_testVectorIndex_a27e0679014fc2205b2185697e43c02d() {
        assertEval("{ x <- c(a=1,b=2,c=3,d=4) ; f <- function(s) { x[s] } ; f(TRUE) ; f(1L) ; f(\"b\") }");
    }

    @Test
    public void TestSimpleVectors_testVectorIndex_3241bcf9c83bf8c4462688c1ff8b6612() {
        assertEval("{ x <- c(a=as.raw(10),b=as.raw(11),c=as.raw(12),d=as.raw(13)) ; f <- function(s) { x[c(s,s)] } ; f(TRUE) ; f(1L) ; f(as.character(NA)) }");
    }

    @Test
    public void TestSimpleVectors_testVectorIndex_f0c931241c541153b0b5b028b1fe5e02() {
        assertEval("{ x <- c(a=1,b=2,c=3,d=4) ; f <- function(s) { x[c(s,s)] } ; f(TRUE) ; f(1L) ; f(\"b\") }");
    }

    @Test
    public void TestSimpleVectors_testVectorIndex_b4259fbeeaa0bfc21bc675e1b5cea0bd() {
        assertEval("{ x <- TRUE;  y<-c(1,1) ; x[y] }");
    }

    @Test
    public void TestSimpleVectors_testVectorIndex_14daed4b8b79de4293a49f88a9919501() {
        assertEval("{ x <- 1+2i;  y<-c(1,2) ; x[y] }");
    }

    @Test
    public void TestSimpleVectors_testVectorIndex_cac9c05f0cc2b26b012547de6b90f3b4() {
        assertEval("{ f<-function(x,l) { x[l == 3] } ; f(c(1,2,3), c(1,2,3)) ; f(c(1,2,3), 1:3) ; f(1:3, c(3,3,2)) }");
    }

    @Test
    public void TestSimpleVectors_testVectorIndex_44308cd20aba8b285780210d692923db() {
        assertEval("{ x <- c(TRUE,FALSE,TRUE) ; x[2:3] }");
    }

    @Test
    public void TestSimpleVectors_testVectorIndex_f820e4fafb91b266952b6857e18db781() {
        assertEval("{ x <- c(1+2i,3+4i,5+6i) ; x[2:3] }");
    }

    @Test
    public void TestSimpleVectors_testVectorIndex_2eeb14b57745c41d034b9f732f972faf() {
        assertEval("{ x <- c(1+2i,3+4i,5+6i) ; x[c(2,3,NA)] }");
    }

    @Test
    public void TestSimpleVectors_testVectorIndex_cfc3eda027a4daae92cd3b1403344c9e() {
        assertEval("{ x <- c(1+2i,3+4i,5+6i) ; x[c(-2,-3,-4,-5)] }");
    }

    @Test
    public void TestSimpleVectors_testVectorIndex_54938dbc2f6c8729cd244697eab1c0b5() {
        assertEval("{ x <- c(1+2i,3+4i,5+6i) ; x[c(-2,-3,-4,-5,-5)] }");
    }

    @Test
    public void TestSimpleVectors_testVectorIndex_bd8b72e0a38ef6459ea655c69b7ff87e() {
        assertEval("{ x <- c(1+2i,3+4i,5+6i) ; x[c(-2,-3,-4,-5,-2)] }");
    }

    @Test
    public void TestSimpleVectors_testVectorIndex_8ed99a09915af3df44584ef575c99794() {
        assertEval("{ x <- c(TRUE,FALSE,TRUE) ; x[integer()] }");
    }

    @Test
    public void TestSimpleVectors_testVectorIndex_63e58b086646f4c3a054f7d87a3a616f() {
        assertEval("{ x <- c(a=1,x=2,b=3,y=2) ; x[c(3,4,2)==2] }");
    }

    @Test
    public void TestSimpleVectors_testVectorIndex_65b28532c10612a4bb2f96f94801105c() {
        assertEval("{ x <- c(a=1,x=2,b=3,y=2) ; x[c(3,4,2,1)==2] }");
    }

    @Test
    public void TestSimpleVectors_testVectorIndex_69dac9a3e48d65b697721b9c6bcfb717() {
        assertEval("{ x <- c(as.double(1:2000)) ; x[c(NA,3,3,NA,1:1996)==3] }");
    }

    @Test
    public void TestSimpleVectors_testVectorIndex_1e261da4e69f779d9c82578ed77f39a7() {
        assertEval("{ f <- function(b,i) { b[i] } ; f(1:3, c(TRUE,FALSE,TRUE)) ; f(c(a=1,b=2,c=3),3:1) }");
    }

    @Test
    public void TestSimpleVectors_testVectorIndex_a7b7afc04413ecc554d9183e260e2e41() {
        assertEval("{ f <- function(b,i) { b[i] } ; f(1:3, c(TRUE,FALSE,NA)) }");
    }

    @Test
    public void TestSimpleVectors_testVectorIndex_9d5e7084ede07e2d50f2300a5ccb322c() {
        assertEval("{ f <- function(b,i) { b[i] } ; f(1:3, c(TRUE,FALSE,NA,NA,NA)) }");
    }

    @Test
    public void TestSimpleVectors_testVectorIndex_1b5511da875e829853723e757b2f6da3() {
        assertEval("{ f <- function(b,i) { b[i] } ; f(c(a=1,b=2,c=3), c(TRUE,NA,FALSE,FALSE,TRUE)) }");
    }

    @Test
    public void TestSimpleVectors_testVectorIndex_eb5090ef54b605e24be46e92eef4d3bb() {
        assertEval("{ f <- function(b,i) { b[i] } ; f(1:3, logical()) }");
    }

    @Test
    public void TestSimpleVectors_testVectorIndex_a62770e869d141739eb075b2b6addee1() {
        assertEval("{ f <- function(b,i) { b[i] } ; f(c(a=1L,b=2L,c=3L), logical()) }");
    }

    @Test
    public void TestSimpleVectors_testVectorIndex_9ee97bff55e650fb561b0d22c4b5422f() {
        assertEval("{ f <- function(b,i) { b[i] } ; f(c(a=1,b=2,c=3), character()) }");
    }

    @Test
    public void TestSimpleVectors_testVectorIndex_ee23eb8ee388a34f219353f2c7414939() {
        assertEval("{ f <- function(b,i) { b[i] } ; f(c(1,2,3), character()) }");
    }

    @Test
    public void TestSimpleVectors_testVectorIndex_7fc57744e5dd22496ea5f0e4df11c9e0() {
        assertEval("{ f <- function(b,i) { b[i] } ; f(c(1,2,3), c(\"hello\",\"hi\")) }");
    }

    @Test
    public void TestSimpleVectors_testVectorIndex_bb136bd4d4bf0afb476b89ab443ad8b3() {
        assertEval("{ f <- function(b,i) { b[i] } ; f(1:3, c(\"h\",\"hi\")) ; f(1:3,TRUE) }");
    }

    @Test
    public void TestSimpleVectors_testVectorIndex_c81b30fb22fb65b68927dd94af59416c() {
        assertEval("{ x <- list(1,2,list(3)) ; x[[c(3,1)]] }");
    }

    @Test
    public void TestSimpleVectors_testVectorIndex_bbfbf9712df6115c37def0342db7c966() {
        assertEval("{ x <- list(1,2,list(3)) ; x[[c(3,NA)]] }");
    }

    @Test
    public void TestSimpleVectors_testVectorIndex_17482c2faac21387879ad6743e11ecc6() {
        assertEval("{ x <- list(1,list(3)) ; x[[c(-1,1)]] }");
    }

    @Test
    public void TestSimpleVectors_testVectorIndex_e5cba9fa5b94fb314f5ea0d3270cd1fb() {
        assertEval("{ l <- list(1,list(2)) ; f <- function(i) { l[[i]] } ; f(c(2,1)) ; f(1) }");
    }

    @Test
    public void TestSimpleVectors_testVectorIndex_0d8ef2c07f8ababba9bd39e38e6a6ff2() {
        assertEval("{ f <- function(i) { l[[i]] } ; l <- list(1, c(2,3)) ; f(c(2,-1)) }");
    }

    @Test
    public void TestSimpleVectors_testVectorIndex_e95d94c457262de899bbeb0481bad9dc() {
        assertEval("{ f <- function(i) { l[[i]] } ; l <- list(1, c(2,3)) ; f(c(2,-2)) }");
    }

    @Test
    public void TestSimpleVectors_testVectorIndex_de2c32e732c7d63bb027111d547305f8() {
        assertEval("{ x <- list(a=1,b=2,d=list(x=3)) ; x[[c(\"d\",\"x\")]] }");
    }

    @Test
    public void TestSimpleVectors_testVectorIndex_96b89e028115903f0bc864c81c62ad56() {
        assertEval("{ x <- list(a=1,b=2,d=list(x=3)) ; x[[c(\"d\",NA)]] }");
    }

    @Test
    public void TestSimpleVectors_testVectorIndex_88d69a07809ca2dc7bb20b2f7d783391() {
        assertEval("{ x <- list(a=1,b=2,d=list(x=3)) ; f <- function(i) { x[[i]] } ; f(c(\"d\",\"x\")) ; f(\"b\") }");
    }

    @Test
    public void TestSimpleVectors_testVectorIndex_c2871393cc9204f0b163a30c9426d95a() {
        assertEvalError("{ a <- c(1,2,3) ; x <- integer() ; a[[x]] }");
    }

    @Test
    public void TestSimpleVectors_testVectorIndex_581ec67c581f25855da44378c8c9b077() {
        assertEvalError("{ x <- function(){3} ; x[3:2] }");
    }

    @Test
    public void TestSimpleVectors_testVectorIndex_4bfcc3740bcd67bbb835a0da19a68128() {
        assertEvalError("{ x <- c(1,2,3) ; x[-1:2] }");
    }

    @Test
    public void TestSimpleVectors_testVectorIndex_5ae4c806c752b28de4a87cfb64d2c215() {
        assertEvalError("{ x <- c(1+2i,3+4i,5+6i) ; x[c(-2,3,NA)] }");
    }

    @Test
    public void TestSimpleVectors_testVectorIndex_e85cd675b20437c2b3c42ef5a701c6aa() {
        assertEvalError("{ x <- c(1+2i,3+4i,5+6i) ; x[c(-2,-3,NA)] }");
    }

    @Test
    public void TestSimpleVectors_testVectorIndex_3206fa39bd2d947d5c47594c6ba5230c() {
        assertEvalError("{ f <- function(b,i) { b[i] } ; x <- c(1+2i,3+4i,5+6i) ; f(x,c(1,2)) ; f(x,c(1+2i)) }");
    }

    @Test
    public void TestSimpleVectors_testVectorIndex_937b553693e061e2659bfa8c8cce00f4() {
        assertEvalError("{ x <- list(1,2,list(3)) ; x[[c(4,1)]] }");
    }

    @Test
    public void TestSimpleVectors_testVectorIndex_be992a2209eb4108599af0e01753e57f() {
        assertEvalError("{ x <- list(1,2,list(3)) ; x[[c(NA,1)]] }");
    }

    @Test
    public void TestSimpleVectors_testVectorIndex_14109cd35e7f11e5dadece496ccfe9d2() {
        assertEvalError("{ l <- list(1,list(2)) ; l[[integer()]] }");
    }

    @Test
    public void TestSimpleVectors_testVectorIndex_38d87c248daa2317107b01e6401055b7() {
        assertEvalError("{ l <- list(1,NULL) ; f <- function(i) { l[[i]] } ; f(c(2,1)) }");
    }

    @Test
    public void TestSimpleVectors_testVectorIndex_2f6239337e763cea6b37f55978bcd5b0() {
        assertEvalError("{ f <- function(i) { l[[i]] } ; l <- list(1, 1:3) ; f(c(2,NA)) }");
    }

    @Test
    public void TestSimpleVectors_testVectorIndex_48f81853d07173ea86329d48f6502165() {
        assertEvalError("{ f <- function(i) { l[[i]] } ; l <- list(1, 1:3) ; f(c(2,-4)) }");
    }

    @Test
    public void TestSimpleVectors_testVectorIndex_79a6931134d204e038e8a9165254f361() {
        assertEvalError("{ f <- function(i) { l[[i]] } ; l <- list(1, 2) ; f(c(2,-1)) }");
    }

    @Test
    public void TestSimpleVectors_testVectorIndex_0e83dcb18a9ab495b9068641d3bdac70() {
        assertEvalError("{ f <- function(i) { l[[i]] } ; l <- list(1, c(2,3)) ; f(c(2,-4)) }");
    }

    @Test
    public void TestSimpleVectors_testVectorIndex_0c12312d7bb73f882fa41607af3894dd() {
        assertEvalError("{ f <- function(i) { l[[i]] } ; l <- list(1, c(2,3)) ; f(c(2,0)) }");
    }

    @Test
    public void TestSimpleVectors_testVectorIndex_155a3df70f3fa7baa994ed5d784ff331() {
        assertEvalError("{ x <- list(a=1,b=2,d=list(x=3)) ; x[[c(\"z\",\"x\")]] }");
    }

    @Test
    public void TestSimpleVectors_testVectorIndex_a64710a5ce41a31720020179bd49c369() {
        assertEvalError("{ x <- list(a=1,b=2,d=list(x=3)) ; x[[c(\"z\",NA)]] }");
    }

    @Test
    public void TestSimpleVectors_testVectorIndex_548faeb03e49dca38cd7c1c87b9509d9() {
        assertEvalError("{ x <- list(a=1,b=2,d=list(x=3)) ; x[[c(NA,\"x\")]] }");
    }

    @Test
    public void TestSimpleVectors_testVectorIndex_98751d056799576e09fef87ea4e41612() {
        assertEvalError("{ x <- list(a=1,b=2,d=list(x=3)) ; x[[character()]] }");
    }

    @Test
    public void TestSimpleVectors_testVectorIndex_f47a43ac796ee5aff9bfd960d2752d3e() {
        assertEvalError("{ x <- c(a=1,b=2) ; x[[c(\"a\",\"a\")]] }");
    }

    @Test
    public void TestSimpleVectors_testVectorIndex_1cfa555a6e4539e74b584e0e4a2cc41e() {
        assertEvalError("{ x <- list(1,2) ; x[[c(\"a\",\"a\")]] }");
    }

    @Test
    public void TestSimpleVectors_testVectorIndex_5712dd96758146fe2cea795860a7179c() {
        assertEvalError("{ x <- list(a=1,b=1:3) ; x[[c(\"b\",\"a\")]] }");
    }

    @Test
    public void TestSimpleVectors_testVectorIndex_42764d14cd8f86e1e754496ce6edbce9() {
        assertEvalError("{ x <- list(a=1,b=1:3) ; x[[2+3i]] }");
    }

    @Test
    public void TestSimpleVectors_testVectorIndex_00f7760c3670c4fb3c64fe8037e366fd() {
        assertEvalError("{ x <- list(a=1,b=1:3) ; f <- function(i) { x[[i]] } ; f(c(2,2)) ; f(2+3i) }");
    }

    @Test
    public void TestSimpleVectors_testVectorIndex_72b52a394176ae2a8269b374f9a704ab() {
        assertEvalError("{ x <- 1:3; x[list(2,3)] }");
    }

    @Test
    public void TestSimpleVectors_testVectorIndex_0a19d077b896c90a40db6876574efacd() {
        assertEvalError("{ x <- 1:3; x[function(){3}] }");
    }

    @Test
    public void TestSimpleVectors_testVectorIndex_959c720b9a9af388413220debc100a6e() {
        assertEvalError("{ x <- 1:2; x[[list()]] }");
    }

    @Test
    public void TestSimpleVectors_testVectorIndex_ab60e544ad965d3efeeeb62b50846f55() {
        assertEvalError("{ x <- 1:2; x[[list(-0,-1)]] }");
    }

    @Test
    public void TestSimpleVectors_testVectorIndex_5760451f35992103d657f61892e571b7() {
        assertEvalError("{ x <- 1:2; x[[list(0)]] }");
    }

    @Test
    public void TestSimpleVectors_testVectorIndex_1c06ce49529ccdcf03d647c455cca8c8() {
        assertEvalError("{ f <- function(b,i) { b[[i]] } ; f(list(1,list(2)),c(2,1)) ; f(1:3,list(1)) }");
    }

    @Test
    public void TestSimpleVectors_testVectorIndex_a5e5ece2c7207c07bb19205c9a14d9e4() {
        assertEvalError("{ f <- function(b,i) { b[i] } ; f(1:3,c(2,1)) ; f(1:3,as.raw(c(10,11))) }");
    }

    @Test
    public void TestSimpleVectors_testVectorIndex_51b0ed6271015b73d982d933a45bce21() {
        assertEvalError("{ l <- list(1,2) ; l[[c(1,1,2,3,4,3)]] }");
    }

    @Test
    public void TestSimpleVectors_testVectorIndex_37d698bcf08fa46b31d198a2e2a7f095() {
        assertEvalError("{ l <- list(list(1,2),2) ; l[[c(1,1,2,3,4,3)]] }");
    }

    @Test
    public void TestSimpleVectors_testVectorIndex_3bc7f294454f97d9cb11d6e1b5558c0c() {
        assertEvalError("{ f <- function(b) { b[integer()] } ; f(c(TRUE,FALSE,TRUE)) ; f(f) }");
    }

    @Test
    public void TestSimpleVectors_testVectorIndex_9d704c58dc70d171339e0ed2af90602f() {
        assertEvalError("{ f <- function(b,i) { b[i] } ; f(1:3, c(TRUE,FALSE,TRUE)) ; f(function(){2},3:1) }");
    }

    @Test
    public void TestSimpleVectors_testVectorIndex_6ebbd3a2fad58c0c250e5c792d87ebb8() {
        assertEvalError("{ f <- function(b,i) { b[i] } ; f(1:3, c(TRUE,FALSE)) ; f(f, c(TRUE,NA)) }");
    }

    @Test
    public void TestSimpleVectors_testVectorIndex_94102136bcc491dd4cef2d68b5ab0304() {
        assertEvalError("{ f <- function(b,i) { b[i] } ; f(1:3, c(\"h\",\"hi\")) ; f(function(){3},\"hi\") }");
    }

    @Test
    public void TestSimpleVectors_testVectorIndex_0969e3058322c4ae7f7a03d5dd02e033() {
        assertEvalError("{ f <- function(i) { l[[i]] } ; l <- list(1, f) ; f(c(2,1)) }");
    }

    @Test
    public void TestSimpleVectors_testVectorIndex_2d2bb6829bd147ff70ac0628fe64b80e() {
        assertEvalError("{ x <- list(a=1,b=function(){3},d=list(x=3)) ; x[[c(2,10)]] }");
    }

    @Test
    public void TestSimpleVectors_testVectorIndex_41b05c97e57278ac385535fb4f47222f() {
        assertEvalError("{ x <- list(a=1,b=function(){3},d=list(x=3)) ; x[[c(2,-3)]] }");
    }

    @Test
    public void TestSimpleVectors_testVectorIndex_f18bf8d7ed8664f5b3cd440109c89865() {
        assertEvalError("{ x <- list(a=1,b=function(){3},d=list(x=3)) ; f <- function(i) { x[[i]] } ; f(c(\"d\",\"x\")) ; f(c(\"b\",\"z\")) }");
    }

    @Test
    public void TestSimpleVectors_testVectorIndex_1cad751e84ef65887eda84669bd42192() {
        assertEvalError("{ x <- list(a=1,b=1:3) ; f <- function(i) { x[[i]] } ; f(c(2,2)) ; x <- f ; f(2+3i) }");
    }

    @Test
    public void TestSimpleVectors_testVectorIndex_3a122572c4f5c947c4e9b5e44bd356cc() {
        assertEvalNoOutput("{ f<-function(x,l) { x[l == 3] <- 4 } ; f(c(1,2,3), c(1,2,3)) ; f(c(1,2,3), 1:3) ; f(1:3, c(3,3,2)) }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_bb72cebb9ade33a16898fc1ebad1393e() {
        assertEval(" { f <- function(b, i, v) { b[i] <- v ; b } ; f(c(1,2,3),c(TRUE,FALSE,TRUE),5:6) ; f(3:5, c(FALSE,TRUE,TRUE), c(NA,FALSE)) }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_836c2ee0ab2a0bdce3b0cb4206188056() {
        assertEval("{ a <- c(1,2,3) ; b <- a; a[1] <- 4L; a }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_0c90a0cd3cdbe747fa8e8651d600561e() {
        assertEval("{ a <- c(1,2,3) ; b <- a; a[2] <- 4L; a }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_ca9b64cebab92450c44508734162b2be() {
        assertEval("{ a <- c(1,2,3) ; b <- a; a[3] <- 4L; a }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_11f4c9a22b48ae0883cf48c801875c7f() {
        assertEval("{ a <- c(TRUE,TRUE,TRUE); b <- a; a[[1]] <- FALSE; a }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_ff4f870ac3bb59c34b24f140bc6f36c5() {
        assertEval("{ a <- c(TRUE,TRUE,TRUE); b <- a; a[[2]] <- FALSE; a }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_9926f6f34a94735722c159957ebdd31b() {
        assertEval("{ a <- c(TRUE,TRUE,TRUE); b <- a; a[[3]] <- FALSE; a }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_49d9cc48eeed827ce882b487539908d0() {
        assertEval("{ f<-function(i,v) { x<-1:5 ; x[i]<-v ; x } ; f(1,1) ; f(1L,TRUE) ; f(2,TRUE) }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_049ea7335b1b6af038bc7b10410fcf20() {
        assertEval("{ f<-function(i,v) { x<-1:5 ; x[[i]]<-v ; x } ; f(1,1) ; f(1L,TRUE) ; f(2,TRUE) }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_d9262d7cc29729af3921cc43149f689d() {
        assertEval("{ buf <- integer() ; buf[[1]] <- 4L ; buf }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_335605cb2a0db85fcf9b011b4c73e31d() {
        assertEval("{ buf <- double() ; buf[[1]] <- 23 ; buf }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_474b23368347af90731af13b04ada475() {
        assertEval("{ inds <- 1:4 ; m <- 2:3 ; inds[m] <- inds[m] + 1L ; inds }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_a79a0da05705f4da5811fe8003a50889() {
        assertEval("{ inds <- 1:4 ; m <- c(2L,3L) ; inds[m] <- inds[m] + 1L ; inds }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_8529636fed75da10e0e35990d55d29bf() {
        assertEval("{ inds <- 1:4 ; m <- 2:3 ; inds[m] <- inds[m] + 1L ; m <- 1:2 ; inds[m] <- inds[m] + 1L ; inds }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_f3104bbdcca5088508626c8667827749() {
        assertEval("{ inds <- 1:4 ; m <- 2L ; inds[m] <- inds[m] + 1L ; m <- c(1L,2L) ; inds[m] <- inds[m] + 1L ; inds }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_2db8808221622144ce2ccabdfb674f36() {
        assertEval("{ x <- c(1) ; f <- function() { x[[1]] <<- x[[1]] + 1 ; x } ; a <- f() ; b <- f() ; c(a,b) }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_d25c41a22a663b7717b8ae7a6e516917() {
        assertEval("{ x<-c(1,2,3,4,5); x[3:4]<-c(300,400); x }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_c6a8a884d1152fce0d1c6fac344933cc() {
        assertEval("{ x<-c(1,2,3,4,5); x[4:3]<-c(300L,400L); x }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_ad7386379162230e3c4bc6d0fa4ebc00() {
        assertEval("{ x<-1:5; x[4:3]<-c(300L,400L); x }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_777ed1f3ca38d860b871d26019fc605c() {
        assertEval("{ x<-5:1; x[3:4]<-c(300L,400L); x }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_cbc180f9feb71681af8038f30aaa85e0() {
        assertEval("{ x<-5:1; x[3:4]<-c(300,400); x }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_219f3ab34c2d4e8cea5bb43df1083b5d() {
        assertEval("{ x<-1:5; x[c(4,2,3)]<-c(256L,257L,258L); x }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_de884f4cf8a60e7ff49de5ceb57c4f6c() {
        assertEval("{ f<-function(i,v) { x<-1:5 ; x[i]<-v ; x } ; f(3:2,1) ; f(1L,TRUE) ; f(2:4,4:2) }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_ebde27167622450b586c27d4845f7338() {
        assertEval("{ f<-function(i,v) { x<-1:5 ; x[i]<-v ; x } ; f(c(3,2),1) ; f(1L,TRUE) ; f(2:4,c(4,3,2)) }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_f52178e093d81c4ddbd1260af4542025() {
        assertEval("{ b <- 1:3 ; b[integer()] <- 3:5 ; b }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_68872cf7ac79fb04821c3a961b6aef82() {
        assertEval("{ b <- as.list(3:6) ; dim(b) <- c(4,1) ; b[c(TRUE,FALSE)] <- NULL ; b }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_94da93241a73ec6cec883f3a8a1067f9() {
        assertEval("{ b <- as.list(3:6) ; names(b) <- c(\"X\",\"Y\",\"Z\",\"Q\") ; b[c(TRUE,FALSE)] <- NULL ; b }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_98098d0ab4b0f25322f799edc9ce5bbc() {
        assertEval("{ b <- as.list(3:6) ; names(b) <- c(\"X\",\"Y\",\"Z\",\"Q\") ; b[c(FALSE,FALSE)] <- NULL ; b }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_d4fc37b15d1d32e379d3bf1b64ef139a() {
        assertEval("{ b <- as.list(3:6) ; dim(b) <- c(1,4) ; b[c(FALSE,FALSE)] <- NULL ; b }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_79937a987764abbf160fdfd26173b4cd() {
        assertEval("{ b <- as.list(3:6) ; dim(b) <- c(1,4) ; b[c(FALSE,FALSE,TRUE)] <- NULL ; b }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_ea864b5e5eb945f5125e4d30e33da13b() {
        assertEval("{ b <- as.list(3:5) ; dim(b) <- c(1,3) ; b[c(FALSE,FALSE,FALSE)] <- NULL ; b }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_482cc3f8f70802c19aa313810aa28702() {
        assertEval("{ b <- as.list(3:5) ; dim(b) <- c(1,3) ; b[c(FALSE,TRUE,NA)] <- NULL ; b }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_947dff76330e2ec4dd0880d07524dbf2() {
        assertEval("{ x<-1:5; x[c(0-2,0-3,0-3,0-100,0)]<-256; x }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_0ac3a51afcc0841b4e737788188044e5() {
        assertEval("{ x<-c(1,2,3,4,5); x[c(TRUE,FALSE)] <- 1000; x }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_f5c63c967b015544ab9683ea89fc8298() {
        assertEval("{ x<-c(1,2,3,4,5,6); x[c(TRUE,TRUE,FALSE)] <- c(1000L,2000L) ; x }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_35e12d011e9b1a62307009d31fe66b06() {
        assertEval("{ x<-c(1,2,3,4,5); x[c(TRUE,FALSE,TRUE,TRUE,FALSE)] <- c(1000,2000,3000); x }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_3e3fa83e51c9a8ee6b2a5927885180f0() {
        assertEval("{ x<-c(1,2,3,4,5); x[c(TRUE,FALSE,TRUE,TRUE,0)] <- c(1000,2000,3000); x }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_73303394623b5437766a5c5cf7fad458() {
        assertEval("{ x<-1:3; x[c(TRUE, FALSE, TRUE)] <- c(TRUE,FALSE); x }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_370c386ee46591f450fce2008d0ea8e1() {
        assertEval("{ x<-c(TRUE,TRUE,FALSE); x[c(TRUE, FALSE, TRUE)] <- c(FALSE,TRUE); x }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_7726b39b3fab26722ba47c7ef5031811() {
        assertEval("{ x<-c(TRUE,TRUE,FALSE); x[c(TRUE, FALSE, TRUE)] <- c(1000,2000); x }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_28bf53749d58cbea70887d5df691fe88() {
        assertEval("{ x<-11:9 ; x[c(TRUE, FALSE, TRUE)] <- c(1000,2000); x }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_27964f4fdfc886149a1d0f4836c3653e() {
        assertEval("{ l <- double() ; l[c(TRUE,TRUE)] <-2 ; l}");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_5c7f6472bbb6b5783a68cdfdf29fc7a6() {
        assertEval("{ l <- double() ; l[c(FALSE,TRUE)] <-2 ; l}");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_cc802a4e153da853db95a40945072400() {
        assertEval("{ a<- c('a','b','c','d'); a[3:4] <- c(4,5); a}");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_558b5d2f32bc719b5b978d1c39b16086() {
        assertEval("{ a<- c('a','b','c','d'); a[3:4] <- c(4L,5L); a}");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_cc13f28fd2bc91020c68f04c884947b7() {
        assertEval("{ a<- c('a','b','c','d'); a[3:4] <- c(TRUE,FALSE); a}");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_c3f494c18d43c31e70ce4305a592d09c() {
        assertEval("{ f<-function(b,i,v) { b[i]<-v ; b } ; f(1:4,4:1,TRUE) ; f(c(3,2,1),8,10) }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_f9a3474319d08685e8d866cf757ed3e8() {
        assertEval("{ f<-function(b,i,v) { b[i]<-v ; b } ; f(1:4,4:1,TRUE) ; f(c(3,2,1),8,10) ; f(c(TRUE,FALSE),TRUE,FALSE) }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_4615758516ee6e1475a81a9bda64266e() {
        assertEval("{ x<-c(TRUE,TRUE,FALSE,TRUE) ; x[3:2] <- TRUE; x }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_4045ae3981b8569c73c607e327b2ae8e() {
        assertEval("{ x<-1:3 ; y<-(x[2]<-100) ; y }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_c4aea29b2ed1e37217b54f60760aec97() {
        assertEval("{ x<-1:5 ; x[3] <- (x[4]<-100) ; x }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_7fa208fb556661e762dcbd7c4cd5e3d5() {
        assertEval("{ x<-5:1 ; x[x[2]<-2] <- (x[3]<-50) ; x }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_93ef2e6ce2424349ab27e3e1549d7ac8() {
        assertEval("{ v<-1:3 ; v[TRUE] <- 100 ; v }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_6b617d9cf7f8d096b45b6189e8c6a547() {
        assertEval("{ v<-1:3 ; v[-1] <- c(100,101) ; v }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_ee0f602f9d7da5b108a194b992483f45() {
        assertEval("{ v<-1:3 ; v[TRUE] <- c(100,101,102) ; v }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_0605a930c389eaec5690049f668bb62a() {
        assertEval("{ x <- c(a=1,b=2,c=3) ; x[2]<-10; x }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_2a50638b5648c8fec2f534cf0ed23808() {
        assertEval("{ x <- c(a=1,b=2,c=3) ; x[2:3]<-10; x }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_7600b4a7dd905c83280d8d593a4250f8() {
        assertEval("{ x <- c(a=1,b=2,c=3) ; x[c(2,3)]<-10; x }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_240ff56461bc744a8acc7ceac8bccf7d() {
        assertEval("{ x <- c(a=1,b=2,c=3) ; x[c(TRUE,TRUE,FALSE)]<-10; x }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_48f6fefe507618711482944059f9f4cb() {
        assertEval("{ x <- c(a=1,b=2) ; x[2:3]<-10; x }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_dd1cb95b07cedb656bd32742a492a6c5() {
        assertEval("{ x <- c(a=1,b=2) ; x[c(2,3)]<-10; x }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_ab486e9fc5aa7f34c093a0edc73f4a04() {
        assertEval("{ x <- c(a=1,b=2) ; x[3]<-10; x }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_86c9f1941727b98abf25b554b7bdfe12() {
        assertEval("{ x <- matrix(1:2) ; x[c(FALSE,FALSE,TRUE)]<-10; x }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_12430fe386cd14932232672aeaedd053() {
        assertEval("{ x <- 1:2 ; x[c(FALSE,FALSE,TRUE)]<-10; x }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_d33bf1188541d3d2e1a2ec327bfab3af() {
        assertEval("{ x <- c(a=1,b=2) ; x[c(FALSE,FALSE,TRUE)]<-10; x }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_d96e02fc371cf967564fd6ff0de21f7b() {
        assertEval("{ x<-c(a=1,b=2,c=3) ; x[[\"b\"]]<-200; x }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_206c09ca7dfee016619959d68e46ec41() {
        assertEval("{ x<-c(a=1,b=2,c=3) ; x[[\"d\"]]<-200; x }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_b2f9318c7fa42f64c5cdad19d5f7f14c() {
        assertEval("{ x<-c() ; x[c(\"a\",\"b\",\"c\",\"d\")]<-c(1,2); x }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_17f724769f9da361f4441c34d22aae90() {
        assertEval("{ x<-c(a=1,b=2,c=3) ; x[\"d\"]<-4 ; x }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_de469943f5b34820b838db7480e5cb55() {
        assertEval("{ x<-c(a=1,b=2,c=3) ; x[c(\"d\",\"e\")]<-c(4,5) ; x }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_4761cf2e6a831e4f8b5355b65695846f() {
        assertEval("{ x<-c(a=1,b=2,c=3) ; x[c(\"d\",\"a\",\"d\",\"a\")]<-c(4,5) ; x }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_b275a7e32ba77937cf4ea8e76e015af5() {
        assertEval("{ a = c(1, 2); a[['a']] = 67; a; }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_307fa4d76e5f482fb617038f1e1dbecf() {
        assertEval("{ a = c(a=1,2,3); a[['x']] = 67; a; }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_6a5d180337cb9d51b2da67888c8538e8() {
        assertEval("{ x <- c(TRUE,TRUE,TRUE,TRUE); x[2:3] <- c(FALSE,FALSE); x }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_dfaa32bf58291f9062a35992af2d21d8() {
        assertEval("{ x <- c(TRUE,TRUE,TRUE,TRUE); x[3:2] <- c(FALSE,TRUE); x }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_9aa149d56494fb64808d6c930e2ddf11() {
        assertEval("{ x <- c('a','b','c','d'); x[2:3] <- 'x'; x}");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_4ddf55d30cc51e3380d89ea699843a71() {
        assertEval("{ x <- c('a','b','c','d'); x[2:3] <- c('x','y'); x}");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_ea653ed4f255c92c158447750b21be68() {
        assertEval("{ x <- c('a','b','c','d'); x[3:2] <- c('x','y'); x}");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_4b8cd2b49c4c12124ae1bdceb06feea8() {
        assertEval("{ x <- c('a','b','c','d'); x[c(TRUE,FALSE,TRUE)] <- c('x','y','z'); x }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_f8f2f3e6e86075509decf9ed473a419b() {
        assertEval("{ x <- c(TRUE,TRUE,TRUE,TRUE); x[c(TRUE,TRUE,FALSE)] <- c(10L,20L,30L); x }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_c819f5198a670e81b910231b25b2afa0() {
        assertEval("{ x <- c(1L,1L,1L,1L); x[c(TRUE,TRUE,FALSE)] <- c('a','b','c'); x}");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_2641d7b587fa84d8c3b00cac8ebbf207() {
        assertEval("{ x <- c(TRUE,TRUE,TRUE,TRUE); x[c(TRUE,TRUE,FALSE)] <- list(10L,20L,30L); x }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_9916df8073b6a64a40f09884b8f79c35() {
        assertEval("{ x <- c(); x[c('a','b')] <- c(1L,2L); x }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_801feec5ca948a343f5925349c3c7833() {
        assertEval("{ x <- c(); x[c('a','b')] <- c(TRUE,FALSE); x }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_1b0d29df6cf2e39a9f74fb31cb3bbf8b() {
        assertEval("{ x <- c(); x[c('a','b')] <- c('a','b'); x }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_2e120823aef2745c283d8131c46c5cd7() {
        assertEval("{ x <- list(); x[c('a','b')] <- c('a','b'); x }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_4fb4ee0e2f241b96768679529e0b9a4c() {
        assertEval("{ x <- list(); x[c('a','b')] <- list('a','b'); x }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_e8df833a4c496550621107dc2914c994() {
        assertEval("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(list(1,2), 1:2, 10) ; f(1:2, 1:2, 11) }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_77a8a9b8f093e1344544a94a6a90d992() {
        assertEval("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(list(1,2), 1:2, TRUE) }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_9361c6731e182046b3eff2a1a53d5658() {
        assertEval("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(list(1,2), 1:2, 11L) }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_feee61d15ee2f56858042c74b5a658c3() {
        assertEval("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(list(1,2), 1:2, TRUE) ;  f(list(1,2), 1:2, as.raw(10))}");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_19ed4f74ee8c0230509908ff54e87f2b() {
        assertEval("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(list(1,2), 1:2, c(TRUE,NA)) ;  f(list(1,2), 1:2, c(1+2i,3+4i))}");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_4a719d61e876dab746d1975ef1629e1d() {
        assertEval("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(list(1,2), 1:2, c(TRUE,NA)) ;  f(1:2, 1:2, c(10,5))}");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_7e868e5e6dae7381963f76d52e0bac83() {
        assertEval("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(list(1,2), 1:2, c(TRUE,NA)) ;  f(list(1,2), 1:3, c(2,10,5)) }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_4b2bf42c6fbd7e4da6ef9c74fa4e9a09() {
        assertEval("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(list(1,2,3,4,5), 4:3, c(TRUE,NA)) }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_986c42b1579ac9f899e35c6209375846() {
        assertEval("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(list(1,2,3,4), seq(1L,4L,2L), c(TRUE,NA)) }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_1802b0c50469f3f3b2a28a4de43034e2() {
        assertEval("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(list(1,2),1:2,3:4) }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_905ab5b831e870f8a835f4b03fca0ded() {
        assertEval("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(list(1,2),1:2,c(4,3)) }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_08b000f160654ffaa989fd255a1c641b() {
        assertEval("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(list(1,2),1:2,c(1+2i,3+2i)) }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_beaf000005391ba41cebe98a07e5fb7b() {
        assertEval("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(c(1,3,10),1:2,1+2i) }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_c9029fe04d1a89f4e6cfe48475f5bb6b() {
        assertEval("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(c(1,3,10),1:2,c(3,NA)) }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_8aa1a3436347f6c01a9bcbc77e64aa7d() {
        assertEval("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(c(1,3,10),1:2,c(3L,NA)) }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_c647a26b0d806ce4b76dd661e7915671() {
        assertEval("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(c(1,3,10),1:2,c(TRUE,FALSE)) }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_45ebcdef526fb5ca8a45775cc099884a() {
        assertEval("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(c(1,3,10),1:2,c(TRUE,FALSE)) ; f(c(10L,4L), 2:1, 1+2i) }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_fbbf3fd14f07aa726d92040eb172b6bc() {
        assertEval("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(c(1,3,10),-1:0,c(TRUE,FALSE)) }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_319bb269e7cf04d5cc3d7adaaf17cc57() {
        assertEval("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(c(1,3,10), c(2L,4L),c(TRUE,FALSE)) }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_dba3120ad6625c60f65ac14a9106d1ba() {
        assertEval("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(as.double(1:5), seq(1L,6L,2L),c(TRUE,FALSE,NA)) }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_510c629da6ee43d24fbc8fce94d8882d() {
        assertEval("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(as.double(1:5), c(7L,4L,1L),c(TRUE,FALSE,NA)) }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_f5f3e9548a6093ad0ff76f31db6acb12() {
        assertEval("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(c(1L,3L,10L),2:1,1+2i) }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_e0839946250fe44c56860cd181d71b76() {
        assertEval("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(c(1L,3L,10L),2:1,c(3,NA)) }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_1992456fe5c261ea4d09de3fc7e97859() {
        assertEval("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(c(1L,3L,10L),2:1,c(3L,NA)) }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_a9b6f8d4813c301bae4ddd795d929fc6() {
        assertEval("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(c(1L,3L,10L),1:2,c(TRUE,FALSE)) }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_72d1d07f35dc2bec1cf59eb0b5b33d2f() {
        assertEval("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(c(1L,3L,10L),1:2,c(TRUE,FALSE)) ; f(c(10,4), 2:1, 1+2i) }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_9269ca11e1804ff49bdc1fdcc321a4ca() {
        assertEval("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(1:5, seq(1L,6L,2L),c(TRUE,FALSE,NA)) }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_eb0a02069d44a0a6df0bbc4877108acd() {
        assertEval("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(1:2, seq(1L,6L,2L),c(TRUE,FALSE,NA)) }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_507c63597b251da28999081d44027a76() {
        assertEval("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(c(TRUE,FALSE,NA),2:1,1+2i) }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_d849ff7b1e8afa4c315a33bd7dbde5fa() {
        assertEval("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(c(TRUE,NA,FALSE),2:1,c(TRUE,NA)) }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_a819511fafcc2b50f1c541d94cfb8960() {
        assertEval("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(c(TRUE,NA,FALSE),2:0,c(TRUE,NA)) }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_8ea985ca76f50405d981d36e2c003ffd() {
        assertEval("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(c(TRUE,NA,FALSE),3:4,c(TRUE,NA)) }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_92732392a83d52c15ebd0fb9266a29b4() {
        assertEval("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(as.logical(-3:3),c(1L,4L,7L),c(TRUE,NA,FALSE)) }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_7b45dcae2c1f4a09c0da964e192f493a() {
        assertEval("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(c(TRUE,FALSE),2:1,c(NA,NA)) ; f(c(TRUE,FALSE),1:2,3:4) }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_a6a1ed8957882b01ccbc4e555c6a6a6a() {
        assertEval("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(c(TRUE,FALSE),2:1,c(NA,NA)) ; f(10:11,1:2,c(NA,FALSE)) }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_572a7913eb9c1364a1a59dda9530bece() {
        assertEval("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(c(\"a\",\"b\"),2:1,1+2i) }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_5e995c837c7dec66f0864b6711bf7665() {
        assertEval("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(as.character(-3:3),c(1L,4L,7L),c(\"A\",\"a\",\"XX\")) }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_22db0a920c1a7e3b75d0839cf04b72ad() {
        assertEval("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(c(\"hello\",\"hi\",\"X\"), -1:-2, \"ZZ\") }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_51fa07f2c9cb467127e868ba8248d2c2() {
        assertEval("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(c(\"hello\",\"hi\",\"X\"), 3:4, \"ZZ\") }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_85f898bbc2eed319c2841d61a707f37d() {
        assertEval("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(c(\"hello\",\"hi\",\"X\"), 1:2, c(\"ZZ\",\"xx\")) ; f(1:4,1:2,NA) }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_9a688ce959e6ec2c973c70b044551921() {
        assertEval("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(c(\"hello\",\"hi\",\"X\"), 1:2, c(\"ZZ\",\"xx\")) ; f(as.character(1:2),1:2,NA) }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_b4b48a701ddc299bbd0f12583d1d18ac() {
        assertEval("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(c(1+2i,2+3i), 1:2, c(10+1i,2+4i)) }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_a859f9b5971b1dbd9f3495002dfb9aea() {
        assertEval("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(as.raw(1:3), 1:2, as.raw(40:41)) }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_21b263beb0757b816b6c15ba2b81ff0a() {
        assertEval("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(list(1,2), 1:2, c(TRUE,NA)) ;  f(1:2, c(0,0), c(1+2i,3+4i))}");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_e75179143a7aa1c318c269c65a01d6f1() {
        assertEval("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(1:3, 1:2, 3:4); f(c(TRUE,FALSE), 2:1, 1:2) }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_baa0311b65f430aeaff56cc290d52000() {
        assertEval("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(1:3, 1:2, 3:4); f(3:4, 2:1, c(NA,FALSE)) }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_b2d4e7578bb4a749df4b15e62273e905() {
        assertEval("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(1:2,1:2,3:4); f(1:2,1:2,c(3,4)) ; f(c(TRUE,FALSE,NA), 1:2, c(FALSE,TRUE)) }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_a0c9291997249240a6d4f15f0dea4b87() {
        assertEval("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(1:2,1:2,3:4); f(1:2,1:2,c(3,4)) ; f(c(3,4), 1:2, c(NA,NA)) }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_dac4854c0956b452d591adac1b597a5c() {
        assertEval("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(1:2,1:2,3:4); f(1:2,1:2,c(3,4)) ; f(c(3,4), 1:2, c(\"hello\",\"hi\")) }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_bfdb30c609c9e13b1d54709d78fab3d4() {
        assertEval("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(1:2,1:2,3:4); f(1:2,1:2,c(3,4)) ; f(c(3,4,8), 1:2, list(3,TRUE)) }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_1b27eb090083541df2fc04916f01e661() {
        assertEval("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(1:2,1:2,3:4); l <- list(3,5L) ; dim(l) <- c(2,1) ; f(5:6,1:2,c(3,4)) ; f(l, 1:2, list(3,TRUE)) }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_f8b03e351ea5373cf6cbecb8401437bf() {
        assertEval("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(1:2,1:2,3:4); l <- list(3,5L) ; dim(l) <- c(2,1) ; f(5:6,1:2,c(3,4)) ; f(list(3,TRUE), 1:2, l) }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_18018913b3160fc9ec761cc6b384b590() {
        assertEval("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(1:2,1:2,3:4); l <- c(3,5L) ; dim(l) <- c(2,1) ; f(5:6,1:2,c(3,4)) ; f(l, 1:2, c(3,TRUE)) }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_3ed2d9714ee83cc4e3ca1074ed05524e() {
        assertEval("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(1:2,1:2,3:4); l <- list(3,5L) ; dim(l) <- c(2,1) ; f(5:6,1:2,c(3,4)) ; m <- c(3,TRUE) ; dim(m) <- c(1,2) ; f(m, 1:2, l) }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_721b193a8364200819ba05e585ee7e53() {
        assertEval("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(1:2,1:2,3:4); f(1:2,1:2,c(3,4)) ; f(c(3,4,8), -1:-2, 10) }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_ba134d7acf5eb58ed65fc09ac53d3d08() {
        assertEval("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(1:2,1:2,3:4); f(1:2,1:2,c(3,4)) ; f(c(3,4,8), 3:4, 10) }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_891f7c4d2f3913f51f0f053fc15f666b() {
        assertEval("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(1:2,1:2,3:4); f(1:2,1:2,c(3,4)) ; f(1:8, c(1L,4L,7L), c(10,100,1000)) }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_0e5b200f21a52e167b8cbb6861f449c1() {
        assertEval("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(1:2,1:2,3:4); f(1:2,1:2,c(3,4)) ; z <- f(1:8, c(1L,4L,7L), list(10,100,1000)) ; sum(as.double(z)) }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_2094abcb6c6845d312d6e70f28748492() {
        assertEval("{ f <- function(b,i,v) { b[i] <- v ; b } ; x <- list(1,2) ; attr(x,\"my\") <- 10 ; f(x, 1:2, c(10,11)) }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_0a53538cb9f1207c10b158e583576189() {
        assertEval("{ b <- 1:3 ; b[c(3,2)] <- list(TRUE,10) ; b }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_514979cc585ad79bd98a03ff6a9dde08() {
        assertEval("{ b <- as.raw(11:13) ; b[c(3,2)] <- list(2) ; b }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_62a16cfa81bd89d1988dcdca4986a619() {
        assertEval("{ b <- as.raw(11:13) ; b[c(3,2)] <- as.raw(2) ; b }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_808643e827973d74893756954f54c0b9() {
        assertEval("{ b <- c(TRUE,NA,FALSE) ; b[c(3,2)] <- FALSE ; b }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_d5d58868d5204394a7747432f58a4e06() {
        assertEval("{ b <- 1:4 ; b[c(3,2)] <- c(NA,NA) ; b }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_29588bdf008718726d4db302fef5eb03() {
        assertEval("{ b <- c(TRUE,FALSE) ; b[c(3,2)] <- 5:6 ; b }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_1bbbaf4c6c7a62eb4500e40a589f7d0c() {
        assertEval("{ b <- c(1+2i,3+4i) ; b[c(3,2)] <- 5:6 ; b }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_cd49b4517ef0090a71b58c8d7527e8aa() {
        assertEval("{ b <- 3:4 ; b[c(3,2)] <- c(1+2i,3+4i) ; b }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_4c5655573c140bda8bb7c1503b5ee33d() {
        assertEval("{ b <- c(\"hello\",\"hi\") ; b[c(3,2)] <- c(2,3) ; b }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_4dcb747c4539130f224c7e2c068135f8() {
        assertEval("{ b <- 3:4 ; b[c(3,2)] <- c(\"X\",\"xx\") ; b }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_04b0da6da7c236b1a7934ffdd79a1d12() {
        assertEval("{ f <- function(b,i,v) { b[i] <- v ; b } ; f(3:4, c(1,2), c(10,11)) ; f(4:5, as.integer(NA), 2) }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_ff33f1bfcf09c29ef0a40909e374ab40() {
        assertEval("{ b <- c(1,4,5) ; x <- c(2,8,2) ; b[x==2] <- c(10,11) ; b }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_f7cebd823e4d00669301b23ce179c8ed() {
        assertEval("{ b <- c(1,4,5) ; z <- b ; x <- c(2,8,2) ; b[x==2] <- c(10,11) ; b }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_81d4578e5191d0cc0f08e3513aa80dcd() {
        assertEval("{ b <- c(1,2,5) ;  x <- as.double(NA) ; attr(x,\"my\") <- 2 ; b[c(1,NA,2)==2] <- x ; b }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_01c07d42792435ffedd27bf04768ec7e() {
        assertEval("{ b <- c(1,2,5) ; b[integer()] <- NULL ; b }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_1ccee66027fcbfedebefef451667aa73() {
        assertEval("{ b <- c(1,2,5) ; attr(b,\"my\") <- 10 ; b[integer()] <- NULL ; b }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_0a16096baa322fca5eb39b523692ae18() {
        assertEval("{ b <- list(1,2,5) ; b[c(1,1,5)] <- NULL ; b }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_5cb57134a94edae0f9577f618faec3d0() {
        assertEval("{ b <- list(1,2,5) ; b[c(-1,-4,-5,-1,-5)] <- NULL ; b }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_243ca6174ee1349662605aa677f48f3c() {
        assertEval("{ b <- list(1,2,5) ; b[c(1,1,0,NA,5,5,7)] <- NULL ; b }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_c1bc6afe36b0a1284d861dd8f302c834() {
        assertEval("{ b <- list(1,2,5) ; b[c(0,-1)] <- NULL ; b }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_3110467de84ed8e6d31e68893a986e73() {
        assertEval("{ b <- list(1,2,5) ; b[c(1,NA)] <- NULL ; b }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_0840d0e6a93329b5c2e938b07e82f73d() {
        assertEval("{ b <- list(x=1,y=2,z=5) ; b[c(0,-1)] <- NULL ; b }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_e3a7160879f5a23db6158c61a595605d() {
        assertEval("{ b <- list(1,2,5) ; dim(b) <- c(1,3) ; b[c(0,-1)] <- NULL ; b }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_1cdd2191a3b3aeaa9be625165388ecc7() {
        assertEval("{ b <- list(1,2,5) ; dim(b) <- c(1,3) ; b[c(0,0)] <- NULL ; b }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_749996670b3b45bd325ca2c6b9bfbc01() {
        assertEval("{ b <- list(1,2,5) ; dim(b) <- c(1,3) ; b[c(-10,-20,0)] <- NULL ; b }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_4284cc4632ffb47203c0eb0af9f42ec9() {
        assertEval("{ b <- list(1,2,5) ; dim(b) <- c(1,3) ; b[c(0,0,-1,-2,-3)] <- NULL ; b }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_d6d11cd0277d98539a9c119acc1c6740() {
        assertEval("{ b <- list(1,2,5) ; dim(b) <- c(1,3) ; b[c(0,3,5)] <- NULL ; b }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_7bda41b83cbd0ed9acee5773fc2119f7() {
        assertEval("{ b <- c(1,2,5) ; b[logical()] <- NULL ; b }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_ca7b86168b422f7c3e7e163045bdf8b4() {
        assertEval("{ b <- c(1,2,5) ; b[c(TRUE,FALSE,TRUE)] <- list(TRUE,1+2i) ; b }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_9e71307601e2e4f71969523fe8dfd3f7() {
        assertEval("{ b <- list(1,2,5) ; dim(b) <- c(1,3) ; b[c(TRUE,FALSE,TRUE)] <- list(TRUE,1+2i) ; b }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_561c6b0953e03082995115b0900fda70() {
        assertEval("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(1:3,c(TRUE,FALSE,TRUE),5:6) ; x <- list(1,2,5) ; dim(x) <- c(1,3) ; f(x, c(FALSE,TRUE,TRUE), list(TRUE,1+2i)) }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_18c151c50ac33985757a479842d1c272() {
        assertEval("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(1:3,c(TRUE,FALSE,TRUE),5:6) ; x <- as.raw(10:12) ; dim(x) <- c(1,3) ; f(x, c(FALSE,TRUE,TRUE), as.raw(21:22)) }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_1bcec74e0ab0a4eecaa377ba316aa9f4() {
        assertEval("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(1:3,c(TRUE,FALSE,TRUE),5:6) ; f(c(\"a\",\"XX\",\"b\"), c(FALSE,TRUE,TRUE), 21:22) }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_57e9c7c29969f984085f75b90d864bf1() {
        assertEval("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(1:3,c(TRUE,FALSE,TRUE),5:6) ; f(c(10,12,3), c(FALSE,TRUE,TRUE), c(\"hi\",NA)) }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_6b439574e27b2b9e9935590eccba4001() {
        assertEval("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(1:3,c(TRUE,FALSE,TRUE),5:6) ; f(c(10,12,3), c(FALSE,TRUE,TRUE), c(1+2i,10)) }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_91bdc2a5ea9dc5173d3ba0a18591c93b() {
        assertEval("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(1:3,c(TRUE,FALSE,TRUE),5:6) ; f(c(3+4i,5+6i), c(FALSE,TRUE,TRUE), c(\"hi\",NA)) }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_2b358f4ee693ef2dd14035c1dbb595c2() {
        assertEval("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(1:3,c(TRUE,FALSE,TRUE),5:6) ; f(c(3+4i,5+6i), c(FALSE,TRUE,TRUE), c(NA,1+10i)) }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_a6ee47087c1d0a792517befacb8b0431() {
        assertEval("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(1:3,c(TRUE,FALSE,TRUE),5:6) ; f(c(TRUE,FALSE), c(FALSE,TRUE,TRUE), c(NA,2L)) }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_1a5a53e6b1b89c30db9fbd870639f297() {
        assertEval("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(1:3,c(TRUE,FALSE,TRUE),5:6) ; f(3:5, c(FALSE,TRUE,TRUE), c(NA,FALSE)) }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_c2aad70d9a500f1f7cdd83bcafebcea4() {
        assertEval("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(1:3,c(TRUE,FALSE,TRUE),5:6) ; f(c(TRUE,TRUE,FALSE), c(FALSE,TRUE,TRUE), c(TRUE,NA)) }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_8ae152de2f49dddfbdc77eb3575fc29d() {
        assertEval("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(c(1,2,3),c(TRUE,FALSE,TRUE),5:6) ; f(3:5, c(FALSE,NA), 4) }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_55f7c54b72babf89ed4e4be97e372ee6() {
        assertEval("{ f <- function(b,i,v) { b[i] <- v ; b } ; f(list(1,2), c(TRUE,FALSE), list(1+2i)) }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_0ca13d70dac830967b511a33c0722256() {
        assertEval("{ f <- function(b,i,v) { b[i] <- v ; b } ; f(list(1,2), c(TRUE,FALSE), list(1+2i)) ; f(1:2, c(TRUE,FALSE), list(TRUE)) }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_1820b8463bc95bbba230b771b3c50fe7() {
        assertEval("{ f <- function(b,i,v) { b[i] <- v ; b } ; f(list(1,2), c(TRUE,FALSE), list(1+2i)) ; f(as.list(1:2), c(TRUE,FALSE), TRUE) }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_e2acaf658d9b35311c87164e5836d400() {
        assertEval("{ f <- function(b,i,v) { b[i] <- v ; b } ; f(list(1,2), c(TRUE,FALSE), list(1+2i)) ; f(as.list(1:2), c(TRUE,FALSE), 1+2i) }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_72cf9369679ae9645fef0ab72cc46454() {
        assertEval("{ f <- function(b,i,v) { b[i] <- v ; b } ; f(list(1,2), c(TRUE,FALSE), list(1+2i)) ; f(as.list(1:2), c(TRUE,FALSE), 10) }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_c3cee32a9b398de85865a1699fc72112() {
        assertEval("{ f <- function(b,i,v) { b[i] <- v ; b } ; f(list(1,2), c(TRUE,FALSE), list(1+2i)) ; f(as.list(1:2), c(TRUE,FALSE), 10L) }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_a006c8fcc65ff68707056410ddcf4d68() {
        assertEval("{ f <- function(b,i,v) { b[i] <- v ; b } ; f(list(1,2), c(TRUE,NA), list(1+2i)) }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_efd77557074bdbd7952fbd0ae09af682() {
        assertEval("{ f <- function(b,i,v) { b[i] <- v ; b } ; f(list(1,2), c(TRUE,NA), 10) }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_5086166a34e895ba299d42057b497887() {
        assertEval("{ x <- list(1,0) ; x[is.na(x)] <- c(10,11); x }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_d987862e9f4c682e9554eeebb35ecb6b() {
        assertEval("{ x <- list(1,0) ; x[is.na(x)] <- c(10L,11L); x }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_7ac73694f078007429a70eca10a81593() {
        assertEval("{ x <- list(1,0) ; x[c(TRUE,TRUE)] <- c(TRUE,NA); x }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_1f2b360e7b919ea60d3a023243dec0ed() {
        assertEval("{ x <- list(1,0) ; x[logical()] <- c(TRUE,NA); x }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_736f1dbe7dce28e5769e3f0010047538() {
        assertEval("{ x <- c(1,0) ; x[c(TRUE,TRUE)] <- c(TRUE,NA); x }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_b27ef401104dadce30911a80b69155a3() {
        assertEval("{ x <- c(1,0) ; x[c(TRUE,TRUE)] <- 3:4; x }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_3a56349b3f388b58f85f37eddcba3ee1() {
        assertEval("{ x <- c(1,0) ; x[logical()] <- 3:4; x }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_8dc98d5bb2876b02234752e4e00693a2() {
        assertEval("{ x <- c(1,0) ; attr(x,\"my\") <- 1 ; x[c(TRUE,TRUE)] <- c(NA,TRUE); x }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_da690a387313b4d1eeb994282a552cff() {
        assertEval("{ x <- c(1,0) ; z <- x ; x[c(NA,TRUE)] <- TRUE; x }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_e8e0a9ac960351cccfc65b9b1aa06fa6() {
        assertEval("{ x <- c(1,0)  ; x[is.na(x)] <- TRUE; x }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_99792d88bcb34ba805f9a719adfe909e() {
        assertEval("{ x <- c(1,0)  ; x[c(TRUE,TRUE)] <- rev(x) ; x }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_284dcca2e7561afbc78306b6820c477a() {
        assertEval("{ x <- c(1,0) ; f <- function(v) { x[c(TRUE,TRUE)] <- v ; x } ; f(1:2) ; f(c(1,2)) }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_61ba543b7a27378fb1acab7deee88b5f() {
        assertEval("{ x <- c(1,0) ; f <- function(v) { x[c(TRUE,TRUE)] <- v ; x } ; f(1:2) ; f(1+2i) }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_0eb8e841bca0a35325a966a4df68e9d1() {
        assertEval("{ b <- list(1,2,3) ; attr(b,\"my\") <- 12; b[2] <- NULL ; b }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_fe8087a9570fb2b7bd3347f706a59b02() {
        assertEval("{ b <- list(1,2,3) ; attr(b,\"my\") <- 12; b[2:3] <- NULL ; b }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_349fad6f3191d63d12d86fc6036942b9() {
        assertEval("{ x <- 1:2 ; x[c(TRUE,FALSE,FALSE,TRUE)] <- 3:4 ; x }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_bcf990e6803a080bf59657506961807f() {
        assertEval("{ x <- 1:2 ; x[c(TRUE,FALSE,FALSE,NA)] <- 3L ; x }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_7d5442af0676f3459df6bacf4748f7ec() {
        assertEval("{ x <- 1:2 ; x[c(TRUE,NA)] <- 3L ; x }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_4c3500d7bb1654364dc88b70a09ffec7() {
        assertEval("{ x <- c(1L,2L) ; x[c(TRUE,FALSE)] <- 3L ; x }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_178e213d88d9ecabf5139faf44ce3495() {
        assertEval("{ x <- c(1L,2L) ; x[c(TRUE,NA)] <- 3L ; x }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_077acb1eb2741cbc88f14d9893ebb4d1() {
        assertEval("{ x <- c(1L,2L) ; x[TRUE] <- 3L ; x }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_54a82aca11611038888ee2a4a863e4ff() {
        assertEval("{ x <- c(1L,2L,3L,4L) ; x[c(TRUE,FALSE)] <- 5:6 ; x }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_9cfa99205c05f66dcf1c61b5f5b620f2() {
        assertEval("{ x <- c(1L,2L,3L,4L) ; attr(x,\"my\") <- 0 ;  x[c(TRUE,FALSE)] <- 5:6 ; x }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_49405ab84d73286dbfca2bdae5fd2434() {
        assertEval("{ x <- c(1L,2L,3L,4L) ;  x[is.na(x)] <- 5:6 ; x }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_780927c96ae7b7279affdf82d663a41c() {
        assertEval("{ x <- c(1L,2L) ; x[logical()] <- 3L ; x }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_edb715a1f317bd245f095853fdbbc2f1() {
        assertEval("{ b <- c(TRUE,NA,FALSE,TRUE) ; b[c(TRUE,FALSE)] <- c(FALSE,NA) ; b }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_29b3e3bb8ff95bb465a10e4b9e949206() {
        assertEval("{ b <- c(TRUE,NA,FALSE,TRUE) ; b[c(TRUE,FALSE,FALSE)] <- c(FALSE,NA) ; b }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_1efb93bd2ac08f91a20f86a4f356d8b2() {
        assertEval("{ b <- c(TRUE,NA,FALSE) ; b[c(TRUE,FALSE,TRUE,TRUE)] <- c(FALSE,NA,NA) ; b }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_f59e145c5005d9a9d9914a01da69883a() {
        assertEval("{ b <- c(TRUE,NA,FALSE,TRUE) ; b[c(TRUE,FALSE,TRUE,NA)] <- FALSE ; b }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_03caeed238fa9bee6868d6b4092c0863() {
        assertEval("{ b <- c(TRUE,NA,FALSE,TRUE) ; z <- b ; b[c(TRUE,FALSE,TRUE,NA)] <- FALSE ; b }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_35aaf36af73ee5e0e927843d71041f23() {
        assertEval("{ b <- c(TRUE,NA,FALSE,TRUE) ; attr(b,\"my\") <- 10 ; b[c(TRUE,FALSE,TRUE,NA)] <- FALSE ; b }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_4ee14db6d1ffb63ef753328faed88fcf() {
        assertEval("{ b <- c(TRUE,FALSE,FALSE,TRUE) ; b[b] <- c(TRUE,FALSE) ; b }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_36635424013a8ecc3f29abf529f696c8() {
        assertEval("{ f <- function(b,i,v) { b[i] <- v ; b } ; f(c(TRUE,FALSE,FALSE,TRUE),c(TRUE,FALSE), NA) ; f(1:4, c(TRUE,TRUE), NA) }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_aecdc5097d509904b70ba0f16224b9cc() {
        assertEval("{ f <- function(b,i,v) { b[i] <- v ; b } ; f(c(TRUE,FALSE,FALSE,TRUE),c(TRUE,FALSE), NA) ; f(c(FALSE,FALSE,TRUE), c(TRUE,TRUE), c(1,2,3)) }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_f894c1423eb4fa0dc94d73132242acc4() {
        assertEval("{ b <- c(TRUE,NA,FALSE,TRUE) ; b[logical()] <- c(FALSE,NA) ; b }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_52a1c00a75337a9d79f1f2c7a3bb82ea() {
        assertEval("{ b <- c(\"a\",\"b\",\"c\") ; b[c(TRUE,FALSE)] <- \"X\" ; b }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_c6ac333f104ca780ace60947941054d3() {
        assertEval("{ b <- c(\"a\",\"b\",\"c\") ; b[c(TRUE,FALSE,TRUE,TRUE)] <- \"X\" ; b }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_c90304ceb916a44dcf44273f1af82634() {
        assertEval("{ b <- c(\"a\",\"b\",\"c\") ; b[c(TRUE,FALSE,TRUE,NA)] <- \"X\" ; b }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_8fe9812b56d54ff628a362b4335ba4e9() {
        assertEval("{ b <- c(\"a\",\"b\",\"c\") ; b[c(TRUE,FALSE,NA)] <- \"X\" ; b }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_a7524a2a68d00af71359ca634b039b0c() {
        assertEval("{ b <- c(\"a\",\"b\",\"c\") ; b[logical()] <- \"X\" ; b }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_2d5bef31e49c9c8bcc26712257bea630() {
        assertEval("{ b <- c(\"a\",\"b\",\"c\") ; x <- b ; b[c(FALSE,TRUE,TRUE)] <- c(\"X\",\"z\") ; b } ");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_d68e495685933470f833459c15263bd4() {
        assertEval("{ b <- c(\"a\",\"b\",\"c\") ; b[is.na(b)] <- c(\"X\",\"z\") ; b }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_9dc16b77cca1a509af5604152475f8eb() {
        assertEval("{ b <- c(\"a\",\"b\",\"c\") ; attr(b,\"my\") <- 211 ; b[c(FALSE,TRUE)] <- c(\"X\") ; b }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_8c7ed53b9615065c50648f0811febb7c() {
        assertEval("{ b <- c(\"a\",\"b\",\"c\") ; b[c(TRUE,TRUE,TRUE)] <- rev(as.character(b)) ; b }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_75b0be4ad480d9a3ddc6c51bed4d034e() {
        assertEval("{ f <- function(b,i,v) { b[i] <- v ; b } ; f(c(\"a\",\"b\",\"c\"),c(TRUE,FALSE),c(\"A\",\"X\")) ; f(1:3,c(TRUE,FALSE),4) }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_ddbd80fececb99bdbf65aef32ff7fe08() {
        assertEval("{ f <- function(b,i,v) { b[i] <- v ; b } ; f(c(\"a\",\"b\",\"c\"),c(TRUE,FALSE),c(\"A\",\"X\")) ; f(c(\"A\",\"X\"),c(TRUE,FALSE),4) }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_40f03dd7fe695ec7ed62913af59f2d05() {
        assertEval("{ b <- c(\"a\",\"b\",\"c\") ; b[c(TRUE,FALSE,TRUE)] <- c(1+2i,3+4i) ; b }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_940c463a3edf6a809a6569e9b87b2957() {
        assertEval("{ f <- function(b,i,v) { b[[i]] <- v ; b } ; f(1:2,\"hi\",3L) ; f(1:2,c(2),10) ; f(1:2, -1, 10) }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_acc8c20adb5724497365dcdfece54c4c() {
        assertEval("{ x <- c(); f <- function(i, v) { x[i] <- v ; x } ; f(1:2,3:4); f(c(1,2),c(TRUE,FALSE)) }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_99033dd01b36ab699ee558eb6c25ad95() {
        assertEval("{ x <- c(); f <- function(i, v) { x[i] <- v ; x } ; f(1:2,3:4); f(c(\"a\",\"b\"),c(TRUE,FALSE)) }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_3546a414baf8592d3baa06286a49fe1b() {
        assertEval("{ a <- c(2.1,2.2,2.3); b <- a; a[[2]] <- TRUE; a }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_eaa66a4f1687c7b78b2c424bce826ddd() {
        assertEval("{ a <- c(2.1,2.2,2.3); b <- a; a[[3]] <- TRUE; a }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_1716a6e7a088561f56e2e983c136558b() {
        assertEval("{ buf <- character() ; buf[[1]] <- \"hello\" ; buf[[3]] <- \"world\" ; buf }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_f3cc1a0ddc64b0c6465d81b9276416e5() {
        assertEval("{ b <- 1:3 ; dim(b) <- c(1,3) ;  b[integer()] <- 3:5 ; b }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_235ffdf66bd349381d4161c9dd783c93() {
        assertEvalError(" { f <- function(b, i, v) { b[i] <- v ; b } ; f(1:3, 1:2, f) }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_c02e3c87f352b7aa2279fc7c86d13c48() {
        assertEvalError("{ x <- (0:4); x[c(NA, NA, NA)] <- c(200L, 300L); x }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_ac61aa83946cdf6705c4d3a216f78726() {
        assertEvalError("{ x <- c(1L, 2L, 3L, 4L, 5L); x[c(NA, 2, 10)] <- c(400L, 500L, 600L); x }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_498a118268ef03337afa85005080b622() {
        assertEvalError("{ x <- c(1L, 2L, 3L, 4L, 5L); x[c(NA, 0, NA)] <- c(400L, 500L, 600L); x }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_cd9a123825ac30d925ba4f06786fd330() {
        assertEvalError("{ x <- 1:3 ; x[c(-2, 1)] <- 10 }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_7dc6183c066a3aa5631873b0c3afd6d7() {
        assertEvalError("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(list(1,2), 1:2, c(TRUE,NA)) ;  f(1:2, c(0,0), as.raw(c(11,23)))}");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_34c541197c3a5c6da0f8478529ca7cb4() {
        assertEvalError("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(list(1,2), 1:2, TRUE) ;  f(list(1,2), -1:1, c(2,10,5)) }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_7b6a4b5649f9afce5f484fbc1a2a8532() {
        assertEvalError("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(list(1,2), 1:2, c(TRUE,NA)) ;  f(list(1,2), -10:10, 1:3) }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_b4365954ec04292549c048e9bdd9e0e7() {
        assertEvalError("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(c(1,3,10),1:2,c(TRUE,FALSE)) ; f(c(10,4), 2:1, as.raw(10)) }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_0d2d81d1e208887a264391ce62397203() {
        assertEvalError("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(c(1L,3L,10L),1:2,c(TRUE,FALSE)) ; f(c(10L,4L), 2:1, as.raw(10)) }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_ce22402c878077447e589c74a61cd106() {
        assertEvalError("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(1:2, seq(1L,-8L,-2L),c(TRUE,FALSE,NA)) }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_8802b9ad2b9764a0c8798a929e85439f() {
        assertEvalError("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(c(1+2i,2+3i), 1:2, as.raw(10:11)) }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_af530c3fdcf59601f383aa955d7e000a() {
        assertEvalError("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(as.raw(10:11), 1:2, c(10+1i, 11)) }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_d76af362feb34bdeea2b124e308c6233() {
        assertEvalError("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(1:2,1:2,3:4); f(f, 1:2, 1:3) }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_8e6aa5e5bc9818c1ec743ee3809f6df8() {
        assertEvalError("{ b <- as.raw(11:13) ; b[c(3,2)] <- 2 ; b }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_f756772538e94a8d1283d1bfc0398dc7() {
        assertEvalError("{ b <- 3:4 ; b[c(NA)] <- c(2,7) ; b }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_168097517c6715bd9db20c8470827fe9() {
        assertEvalError("{ b <- 3:4 ; b[c(NA,1)] <- c(2,10) ; b }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_617c920ebea7b4a8ed625e36c7b6214d() {
        assertEvalError("{ b <- 3:4 ; b[[c(NA,1)]] <- c(2,10) ; b }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_a405f9bde73906238394a303daf9c927() {
        assertEvalError("{ f <- function(b,i,v) { b[i] <- v ; b } ; f(3:4, c(1,2), c(10,11)) ; f(4:5, c(1,-1), 2) }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_9475d4acda9c79520d36c472566b071c() {
        assertEvalError("{ f <- function(b,i,v) { b[i] <- v ; b } ; f(3:4, c(1,2), c(10,11)) ; f(4:5, c(NA,-1), 2) }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_876cb9920f5d76cb75f44607f771d629() {
        assertEvalError("{ b <- c(1,2,5) ;  x <- c(2,2,NA) ; b[x==2] <- c(10,11,3) ; b }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_1f8c30b75941df6258075e829cf216da() {
        assertEvalError("{ b <- c(1,2,5) ; b[c(1)] <- NULL ; b }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_e6d9ec36d849ac7cbd743386a60aad28() {
        assertEvalError("{ b <- list(1,2,5) ; b[c(-1,NA)] <- NULL ; b }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_a8334cce07074c5d410ed764b2f30ca4() {
        assertEvalError("{ b <- list(1,2,5) ; b[c(-1,1)] <- NULL ; b }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_4d1fc7122f2e0b401d0183eb6f503081() {
        assertEvalError("{ b <- c(1,2,5) ; b[c(0,3,5)] <- NULL ; b }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_78a0c90c546b3beda12ba9adda7b4fb7() {
        assertEvalError("{ b <- c(1,2,5) ; b[c(TRUE,FALSE,FALSE)] <- NULL ; b }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_6bb70c47dd4d8881d05b964af9a407b8() {
        assertEvalError("{ b <- c(1,2,5) ; b[c(TRUE,NA,TRUE)] <- list(TRUE,1+2i) ; b }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_ce653fc6e2a85e6ba53458269c91b7fa() {
        assertEvalError("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(1:3,c(TRUE,FALSE,TRUE),5:6) ; x <- as.raw(10:12) ; dim(x) <- c(1,3) ; f(x, c(FALSE,TRUE,TRUE), 21:22) }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_db4f9546f2f459cff51de789ad57369f() {
        assertEvalError("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(1:3,c(TRUE,FALSE,TRUE),5:6) ; x <- 10:12 ; dim(x) <- c(1,3) ; f(x, c(FALSE,TRUE,TRUE), as.raw(21:22)) }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_bc58b6771bea567e086c374b44e7450d() {
        assertEvalError("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(c(1,2,3),c(TRUE,FALSE,TRUE),5:6) ; f(3:5, c(FALSE,NA), 4:5) }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_68e654c466179ff78d07f17a1a52c5f6() {
        assertEvalError("{ f <- function(b, i, v) { b[[i]] <- v ; b } ; f(c(1,2,3),c(TRUE,FALSE,TRUE),5:6) ; f(3:5, c(FALSE,NA), 4:5) }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_e1e15ba02ca24634ca6cbfa12d72a278() {
        assertEvalError("{ f <- function(b,i,v) { b[i] <- v ; b } ; f(list(1,2), c(TRUE,NA), c(10,11)) }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_f6ddff82f85b4d4631af50b5ddef1c23() {
        assertEvalError("{ f <- function(b,i,v) { b[i] <- v ; b } ; x <- list(1,2) ; z <- x ; f(x, c(TRUE,NA), c(10,11)) }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_034502c4f8d8021a64f39aca2dfd45ac() {
        assertEvalError("{ x <- c(1,0) ; x[c(NA,TRUE)] <- c(NA,TRUE); x }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_7e84107a03a0010556d6679c74bc22e6() {
        assertEvalError("{ x <- c(1,0) ; z <- x ; x[c(NA,TRUE)] <- c(NA,TRUE); x }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_9416b61cf8e60f8eb859150bf76390c7() {
        assertEvalError("{ x <- 1:2 ; x[c(TRUE,FALSE,FALSE,NA)] <- 3:4 ; x }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_854efa7776408f5045b11cd152e731fc() {
        assertEvalError("{ x <- 1:2 ; x[c(TRUE,NA)] <- 2:3 ; x }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_5f647476b04b5fe89ef534d88732deaf() {
        assertEvalError("{ b <- c(TRUE,NA,FALSE,TRUE) ; b[c(TRUE,NA)] <- c(FALSE,NA) ; b }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_8cf4b3ccbc0930eea19f786724868feb() {
        assertEvalError("{ b <- c(\"a\",\"b\",\"c\") ; b[c(FALSE,NA,NA)] <- c(\"X\",\"y\") ; b }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_3f5eea3da14fd77de9d1f8c79f62867e() {
        assertEvalError("{ b <- c(\"a\",\"b\",\"c\") ; x <- b ; b[c(FALSE,TRUE,NA)] <- c(\"X\",\"z\") ; b }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_f8debc7b4a996e1d0bf11c357424da6a() {
        assertEvalError("{ b <- as.raw(1:5) ; b[c(TRUE,FALSE,TRUE)] <- c(1+2i,3+4i) ; b }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_fa02b626ba765599e92b84e529acc289() {
        assertEvalError("{ f <- function(b,i,v) { b[i] <- v ; b } ; f(c(\"a\",\"b\",\"c\"),c(TRUE,FALSE),c(\"A\",\"X\")) ; f(f,c(TRUE,FALSE),4) }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_102409e6b0727bf9ae3c6b5f85250f15() {
        assertEvalError("{ f <- function(b,i,v) { b[i] <- v ; b } ; f(c(\"a\",\"b\",\"c\"),c(TRUE,FALSE),c(\"A\",\"X\")) ; f(c(\"A\",\"X\"),c(TRUE,FALSE),f) }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_e0700f33df67d725164c5adf2abcf5a2() {
        assertEvalWarning("{ x = c(1,2,3,4); x[x %% 2 == 0] <- c(1,2,3,4); }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_ee68c47a1480b236e9656cf681266a18() {
        assertEvalWarning("{ b <- 3:4 ; b[c(0,1)] <- c(2,10,11) ; b }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_77be9e8866755ba716bf330867f8fcec() {
        assertEvalWarning("{ b <- c(1,4,5) ;  x <- c(2,2) ; b[x==2] <- c(10,11) ; b }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_3bc28b0b8b8d4e40c893e54433158cc1() {
        assertEvalWarning("{ b <- c(1,2,5) ;  x <- c(2,2,-1) ; b[x==2] <- c(10,11,5) ; b }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_5be95e7eb2db99a4d7e364a238cf4f3d() {
        assertEvalWarning("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(1:3,c(TRUE,FALSE,TRUE),5:6) ; f(3:5, c(FALSE,TRUE,TRUE), 4:6) }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_ed4b2d98242785def5e20ed168038dc9() {
        assertEvalWarning("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(c(1,2,3),c(TRUE,FALSE,TRUE),5:6) ; f(3:5, c(FALSE,TRUE,TRUE), 4:6) }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_bd1d82de226b4c57972a852d20945cb7() {
        assertEvalWarning("{ x <- list(1,2) ; attr(x,\"my\") <- 10; x[c(TRUE,TRUE)] <- c(10,11,12); x }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_ca005e60b8c09e5b783617851441f9a4() {
        assertEvalWarning("{ x <- list(1,0) ; x[as.logical(x)] <- c(10,11); x }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_c725f3cf9807665fe40ee54aaf029dad() {
        assertEvalWarning("{ x <- list(1,0) ; x[c(TRUE,FALSE)] <- x[2:1] ; x }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_e028057d1a5d675b0546ccfd95edb159() {
        assertEvalWarning("{ x <- list(1,0) ; attr(x,\"my\") <- 20 ; x[c(TRUE,FALSE)] <- c(11,12) ; x }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_31f762c52dd500a5f5161809ed2c569f() {
        assertEvalWarning("{ x <- c(1L,2L,3L,4L) ; x[c(TRUE,FALSE)] <- rev(x) ; x }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_620dd2f602dbc673070b2083f33a3863() {
        assertEvalWarning("{ b <- c(TRUE,NA,FALSE) ; b[c(TRUE,TRUE)] <- c(FALSE,NA) ; b }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_9da74cec3da34244abc43a491df1fcaf() {
        assertEvalWarning("{ b <- c(TRUE,NA,FALSE,TRUE) ; b[c(TRUE,FALSE,TRUE,FALSE)] <- b ; b }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_4cd33b0b1b1f590d66d75c78f9127a5d() {
        assertEvalWarning("{ f <- function(b,i,v) { b[b] <- b ; b } ; f(c(TRUE,FALSE,FALSE,TRUE)) ; f(1:3) }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdate_15ebd8d51186d749607ef9ee65e27b37() {
        assertEvalWarning("{ b <- c(\"a\",\"b\",\"c\") ; b[c(FALSE,TRUE,TRUE)] <- c(\"X\",\"y\",\"z\") ; b }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdateIgnore_4bb6389721e2adbd8f6b69aa42e80569() {
        assertEval("{ x<-1:5 ; x[x[4]<-2] <- (x[4]<-100) ; x }");
    }

    @Test
    public void TestSimpleVectors_testVectorUpdateIgnore_09e16a78eb04d58e35b4c9045cbc0acb() {
        assertEval("{ x<-5:1 ; x[x[2]<-2] }");
    }

}

