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
public class TestBuiltin_min extends TestBase {

    @Test
    public void testmin1() {
        assertEval("argv <- list(c(10L, 1L));min(argv[[1]]);");
    }

    @Test
    public void testmin2() {
        assertEval("argv <- list(structure(c(0.25069599964819, 0.252830784944624), .Dim = 1:2));min(argv[[1]]);");
    }

    @Test
    public void testmin3() {
        assertEval("argv <- list(c(-3.37619548064471, -3.28575139573497, -3.19530731082523, -3.10486322591549, -3.01441914100575, -2.923975056096, -2.83353097118626, -2.74308688627652, -2.65264280136678, -2.56219871645704, -2.4717546315473, -2.38131054663755, -2.29086646172781, -2.20042237681807, -2.10997829190833, -2.01953420699859, -1.92909012208884, -1.8386460371791, -1.74820195226936, -1.65775786735962, -1.56731378244988, -1.47686969754013, -1.38642561263039, -1.29598152772065, -1.20553744281091, -1.11509335790117, -1.02464927299142, -0.934205188081682, -0.84376110317194, -0.753317018262198, -0.662872933352456, -0.572428848442715, -0.481984763532973, -0.39154067862323, -0.301096593713489, -0.210652508803747, -0.120208423894005, -0.029764338984263, 0.0606797459254791, 0.151123830835221, 0.241567915744963, 0.332012000654705, 0.422456085564447, 0.512900170474189, 0.603344255383931, 0.693788340293673, 0.784232425203414, 0.874676510113156, 0.965120595022898, 1.05556467993264, 1.14600876484238, 1.23645284975212, 1.32689693466187, 1.41734101957161, 1.50778510448135, 1.59822918939109, 1.68867327430083, 1.77911735921058, 1.86956144412032, 1.96000552903006, 2.0504496139398, 2.14089369884954, 2.23133778375928, 2.32178186866903, 2.41222595357877, 2.50267003848851, 2.59311412339825, 2.68355820830799, 2.77400229321774, 2.86444637812748, 2.95489046303722, 3.04533454794696, 3.1357786328567, 3.22622271776645, 3.31666680267619, 3.40711088758593, 3.49755497249567, 3.58799905740541, 3.67844314231516, 3.7688872272249, 3.85933131213464, 3.94977539704438, 4.04021948195412, 4.13066356686387, 4.22110765177361, 4.31155173668335, 4.40199582159309, 4.49243990650283, 4.58288399141258, 4.67332807632232, 4.76377216123206, 4.8542162461418, 4.94466033105154, 5.03510441596129, 5.12554850087103, 5.21599258578077, 5.30643667069051, 5.39688075560025, 5.48732484051, 5.57776892541974, 5.66821301032948, 5.75865709523922, 5.84910118014896, 5.9395452650587, 6.02998934996845, 6.12043343487819, 6.21087751978793, 6.30132160469767, 6.39176568960741, 6.48220977451716, 6.5726538594269, 6.66309794433664, 6.75354202924638, 6.84398611415612, 6.93443019906586, 7.02487428397561, 7.11531836888535, 7.20576245379509, 7.29620653870483, 7.38665062361457, 7.47709470852432, 7.56753879343406, 7.6579828783438, 7.74842696325354, 7.83887104816328, 7.92931513307303, 8.01975921798277, 8.11020330289251));min(argv[[1]]);");
    }

    @Test
    public void testmin4() {
        assertEval("argv <- list(c(NA, 1, 2, 3, -Inf, NaN, Inf));min(argv[[1]]);");
    }

    @Test
    public void testmin5() {
        assertEval("argv <- list(structure(list(c(1L, 2L, 4L), 1:3, c(2L, 1L)), class = c('package_version', 'numeric_version')), na.rm = FALSE);min(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testmin6() {
        assertEval("argv <- list(structure(c(1338544800L, 1338566400L, 1338588000L, 1338609600L, 1338631200L, 1338652800L, 1338674400L, 1338696000L, 1338717600L, 1338739200L, 1338760800L, 1338782400L, 1338804000L, 1338825600L, 1338847200L, 1338868800L, 1338890400L, 1338912000L, 1338933600L, 1338955200L, 1338976800L, 1338998400L, 1339020000L, 1339041600L), class = c('POSIXct', 'POSIXt'), tzone = ''), na.rm = TRUE);min(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testmin7() {
        assertEval("argv <- list(1.234e+100);min(argv[[1]]);");
    }

    @Test
    public void testmin8() {
        assertEval(Output.IgnoreErrorContext, "argv <- list(structure(c(3L, 2L, 1L), .Label = c('A', 'B', 'C'), class = c('ordered', 'factor')), na.rm = FALSE);min(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testmin9() {
        assertEval("argv <- list(structure(c(11368.8306749654, 11347.7238090355, 11341.9182102121, 11392.4878842821, 11367.3445285107, 11337.9245694652, 11332.0560643654, 11356.4682624019, 11387.6852128883, 11364.9132677, 11391.3319486445, 11374.2254758319, 11347.9708838458, 11353.2031583386, 11333.3748092474, 11323.9154302836, 11373.0896246266, 11330.2228965024, 11354.2399044028, 11367.8070731596, 11392.4287034031, 11357.6915504499, 11356.9044667059, 11335.1409634408, 11375.8374661156, 11354.7726842454, 11358.781884864, 11337.5281579299, 11339.0060699913, 11364.6998397419, 11363.2410538797, 11328.3945066198, 11325.487840571, 11367.9956844538, 11388.0030639744, 11364.8664695648, 11362.2630523606, 11359.821940674, 11391.9566656714, 11358.5349275633, 11370.7951655071, 11365.1078852355, 11339.7208074429, 11341.0716148671, 11374.0516736354, 11354.6799581982, 11335.2588737891, 11375.2688788734, 11330.349134828, 11383.518146432, 11366.0251480173, 11362.0011677193, 11346.0144123337, 11354.7192011815, 11358.0308680837, 11335.6606452791, 11360.0741421962, 11328.2693021996, 11342.4429152855, 11337.8889663466, 11342.9353336683, 11385.6565872063, 11354.2364726327, 11377.5989422849, 11384.6433324409, 11351.9186946652, 11327.4665936357, 11346.4841244179, 11373.6608162634, 11346.6330733448, 11367.1289885738, 11381.8430187805, 11382.9292165297, 11350.3951496719, 11349.6345719923, 11385.6811798196, 11368.1021034038, 11374.8755054101, 11365.3712412571, 11386.2157128048, 11343.5611108569, 11336.3882076922, 11385.0515660313, 11358.2337640012, 11384.3940280117, 11336.2435535709, 11376.0672136671, 11373.7149224868, 11389.0607372806, 11361.3352610911, 11372.8220707406, 11350.2233569878, 11330.0611188328, 11387.9111462012, 11342.8262750218, 11364.340121117, 11330.7252423461, 11381.8354922482, 11345.257457911, 11377.7995935893), class = 'Date'), na.rm = TRUE);min(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testmin10() {
        assertEval("argv <- list(c(2.00256647265648e-308, 2.22284878464869e-308, 2.22507363599982e-308, 2.2250738585072e-308, 2.22507408101459e-308, 2.22729893236571e-308, 2.44758124435792e-308));min(argv[[1]]);");
    }

    @Test
    public void testmin11() {
        assertEval("argv <- list(c(FALSE, FALSE));min(argv[[1]]);");
    }

    @Test
    public void testmin12() {
        assertEval("argv <- list(c(2, 13954490295224484, 9.73638996997572e+31, 6.79331796732739e+47, 4.73986448237219e+63, 3.30711964599708e+79, 2.30745845026066e+95, 1.60997032753976e+111, 1.12331576556267e+127, 7.83764947450857e+142, 5.46852017646992e+158, 3.8155205865895e+174, 2.66218224983966e+190, 1.85746981847535e+206, 1.29600222777925e+222, 9.04252525506755e+237, 6.30919154580821e+253, 4.40207760983472e+269, 3.07143746426322e+285, 2.14301721437253e+301));min(argv[[1]]);");
    }

    @Test
    public void testmin13() {
        assertEval("min( );");
    }

    @Test
    public void testmin14() {
        assertEval("argv <- list(structure(c(13823, NA), class = 'Date'), na.rm = TRUE);min(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testmin15() {
        assertEval("argv <- list(structure(c(1208822400, 1209168000), class = c('POSIXct', 'POSIXt'), tzone = 'GMT'), na.rm = FALSE);min(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testmin16() {
        assertEval("argv <- list(3L, 7);min(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testmin17() {
        assertEval("argv <- list(c(-7, -5.6, -4.2, -2.8, -1.4, 0, 1.4, 2.8, 4.2, 5.6, 7));min(argv[[1]]);");
    }

    @Test
    public void testmin18() {
        assertEval("argv <- list(c(4.5241870901798, 0.211646098116025, 1.86003798801034e-43));min(argv[[1]]);");
    }

    @Test
    public void testmin19() {
        assertEval("argv <- list(structure(c(-0.562441486309934, -0.588967592535822, 0.0277608937997097, 0.568074124752969, 3.89980510825846, -0.428174866497729, -0.343990813420242, -0.260996370058754, -2.31774610938305, 0.314764947225063, -0.455124436264437, -0.0444006414474544, -0.27748974692001, -0.303134023269405, -0.670168347915028, 2.92643313367, -0.749546667806845, -0.410394401887929, -0.203261263063707, 0.1847365997012, 0.128559671155683, 0.313558179929332, -0.0668425264405297, -0.106427678524531, -0.523747793519006, -0.402585404761851, 0.0642079595716389, -0.779859286629166, 0.356484381211739, -0.625053119472271, 1.31547628490512, -0.21959878152752, -0.102402088986461), .Names = c('Craig Dunain', 'Ben Rha', 'Ben Lomond', 'Goatfell', 'Bens of Jura', 'Cairnpapple', 'Scolty', 'Traprain', 'Lairig Ghru', 'Dollar', 'Lomonds', 'Cairn Table', 'Eildon Two', 'Cairngorm', 'Seven Hills', 'Knock Hill', 'Black Hill', 'Creag Beag', 'Kildcon Hill', 'Meall Ant-Suidhe', 'Half Ben Nevis', 'Cow Hill', 'N Berwick Law', 'Creag Dubh', 'Burnswark', 'Largo Law', 'Criffel', 'Acmony', 'Ben Nevis', 'Knockfarrel', 'Two Breweries', 'Cockleroi', 'Moffat Chase')));min(argv[[1]]);");
    }

    @Test
    public void testmin20() {
        assertEval("argv <- list(numeric(0));min(argv[[1]]);");
    }

    @Test
    public void testmin22() {
        assertEval("argv <- list(2, 3, NA);do.call('min', argv)");
    }

    @Test
    public void testMinimum() {
        assertEval("{ min((-1):100) }");
        assertEval("{ min(2L, 4L) }");
        assertEval("{ min() }");
        assertEval("{ min(c(1,2,0/0)) }");
        assertEval("{ max(c(1,2,0/0)) }");
        assertEval("{ min(1:10, 100:200, c(4.0, -5.0)) }");
        assertEval("{ min(NA, 1.1) }");
        assertEval("{ min(0/0, 1.1) }");
        assertEval("{ min(0/0, 1.1, NA) }");
        assertEval("{ min(c(as.character(NA), \"foo\")) }");
        assertEval("{ min(character(0)) }");
        assertEval("{ min(character()) }");
        assertEval("{ min(double(0)) }");
        assertEval("{ min(double()) }");
        assertEval("{ min(NULL) }");

        assertEval("{ min(1:10, 100:200, c(4.0, 5.0), c(TRUE,FALSE,NA)) }");
        assertEval("{ min(c(\"hi\",\"abbey\",\"hello\")) }");
        assertEval("{ min(\"hi\",\"abbey\",\"hello\") }");
        assertEval("{ min(\"hi\",100) }");

        assertEval("{ is.logical(min(TRUE, FALSE)) }");
        assertEval("{ is.logical(min(TRUE)) }");
        assertEval("{ min(as.raw(42), as.raw(7)) }");
        assertEval(Output.IgnoreErrorContext, "{ min(42+42i, 7+7i) }");
        assertEval("{ min(\"42\", \"7\") }");

        assertEval("{ min(as.double(NA), na.rm=FALSE) }");
        assertEval(Output.IgnoreWarningContext, "{ min(as.double(NA), as.double(NA), na.rm=TRUE) }");
        assertEval("{ min(as.double(NA), as.double(NA), na.rm=FALSE) }");
        assertEval("{ min(as.integer(NA), na.rm=FALSE) }");
        assertEval("{ min(as.integer(NA), as.integer(NA), na.rm=FALSE) }");
        assertEval(Output.IgnoreWarningContext, "{ min(as.character(NA), na.rm=TRUE) }");
        assertEval("{ min(as.character(NA), na.rm=FALSE) }");
        assertEval(Output.IgnoreWarningContext, "{ min(as.character(NA), as.character(NA), na.rm=TRUE) }");
        assertEval("{ min(as.character(NA), as.character(NA), na.rm=FALSE) }");
        assertEval("{ min(42L, as.integer(NA), na.rm=TRUE) }");
        assertEval("{ min(42L, as.integer(NA), na.rm=FALSE) }");
        assertEval("{ min(42, as.double(NA), na.rm=TRUE) }");
        assertEval("{ min(42, as.double(NA), na.rm=FALSE) }");
        assertEval("{ min(\"42\", as.character(NA), na.rm=TRUE) }");
        assertEval("{ min(\"42\", as.character(NA), na.rm=FALSE) }");
        assertEval("{ min(42L, as.integer(NA), 7L, na.rm=TRUE) }");
        assertEval("{ min(42L, as.integer(NA), 7L, na.rm=FALSE) }");
        assertEval("{ min(42, as.double(NA), 7, na.rm=TRUE) }");
        assertEval("{ min(42, as.double(NA), 7, na.rm=FALSE) }");
        assertEval("{ min(\"42\", as.character(NA), \"7\", na.rm=TRUE) }");
        assertEval("{ min(\"42\", as.character(NA), \"7\", na.rm=FALSE) }");

        assertEval(Ignored.Unknown, Output.IgnoreWarningContext, "{ min(integer(0)) }");
        assertEval(Ignored.Unknown, Output.IgnoreWarningContext, "{ min(integer()) }");
        assertEval(Ignored.Unknown, Output.IgnoreWarningContext, "{ min(as.double(NA), na.rm=TRUE) }");
        assertEval(Ignored.Unknown, Output.IgnoreWarningContext, "{ min(as.integer(NA), na.rm=TRUE) }");
        assertEval(Ignored.Unknown, Output.IgnoreWarningContext, "{ min(as.integer(NA), as.integer(NA), na.rm=TRUE) }");
    }
}
