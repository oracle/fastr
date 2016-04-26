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
public class TestBuiltin_max extends TestBase {

    @Test
    public void testmax1() {
        assertEval("argv <- list(10L, 1L);max(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testmax2() {
        assertEval("argv <- list(structure(c(1208822400, 1209168000, 1208822400, 1209168000), class = c('POSIXct', 'POSIXt')), na.rm = TRUE);max(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testmax3() {
        assertEval("argv <- list(5, 1, 0);max(argv[[1]],argv[[2]],argv[[3]]);");
    }

    @Test
    public void testmax4() {
        assertEval("argv <- list(c(NA, 1, 2, 3, -Inf, NaN, Inf));max(argv[[1]]);");
    }

    @Test
    public void testmax5() {
        assertEval("max( );");
    }

    @Test
    public void testmax6() {
        assertEval("argv <- list(1L, structure(1:10, .Label = c('a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j'), class = 'factor'));max(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testmax7() {
        assertEval("argv <- list(1573.05073007216, 1000);max(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testmax8() {
        assertEval("argv <- list(structure(c(6L, 3L, 8L, 4L, 4L, 5L, 7L, 8L, 5L), .Dim = 9L, .Dimnames = structure(list(state.division = c('New England', 'Middle Atlantic', 'South Atlantic', 'East South Central', 'West South Central', 'East North Central', 'West North Central', 'Mountain', 'Pacific')), .Names = 'state.division'), class = 'table'));max(argv[[1]]);");
    }

    @Test
    public void testmax9() {
        assertEval(Output.ContainsError,
                        "argv <- list(structure(list(x = c(-1, 1, 1, -1, -1, 1, 1, -1), y = c(-0.701149425287356, -0.701149425287356, -0.701149425287356, -0.701149425287356, 0.701149425287356, 0.701149425287356, 0.701149425287356, 0.701149425287356), z = c(-0.4, -0.4, 0.4, 0.4, -0.4, -0.4, 0.4, 0.4)), .Names = c('x', 'y', 'z'), row.names = c(NA, -8L), class = 'data.frame'), na.rm = FALSE);max(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testmax10() {
        assertEval("argv <- list(c(FALSE, FALSE));max(argv[[1]]);");
    }

    @Test
    public void testmax11() {
        assertEval("argv <- list(numeric(0));max(argv[[1]]);");
    }

    @Test
    public void testmax12() {
        assertEval("argv <- list(4L, numeric(0));max(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testmax13() {
        assertEval("argv <- list(6L, numeric(0));max(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testmax14() {
        assertEval("argv <- list(c(0, 0, 0, 0, 0, 1.75368801162502e-134, 0, 0, 0, 2.60477585273833e-251, 1.16485035372295e-260, 0, 1.53160350210786e-322, 0.333331382328728, 0, 0, 0, 0, 0, 0, 0, 0, 0, 3.44161262707711e-123, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1.968811545398e-173, 0, 8.2359965384697e-150, 0, 0, 0, 0, 6.51733217171341e-10, 0, 2.36840184577368e-67, 0, 9.4348408357524e-307, 0, 1.59959906013771e-89, 0, 8.73836857865034e-286, 7.09716190970992e-54, 0, 0, 0, 1.530425353017e-274, 8.57590058044551e-14, 0.333333106397154, 0, 0, 1.36895217898448e-199, 2.0226102635783e-177, 5.50445388209462e-42, 0, 0, 0, 0, 1.07846402051283e-44, 1.88605464411243e-186, 1.09156111051203e-26, 0, 3.0702877273237e-124, 0.333333209689785, 0, 0, 0, 0, 0, 0, 3.09816093866831e-94, 0, 0, 4.7522727332095e-272, 0, 0, 2.30093251441394e-06, 0, 0, 1.27082826644707e-274, 0, 0, 0, 0, 0, 0, 0, 4.5662025456054e-65, 0, 2.77995853978268e-149, 0, 0, 0));max(argv[[1]]);");
    }

    @Test
    public void testmax15() {
        assertEval("argv <- list(c(1.2e+100, 1.3e+100));max(argv[[1]]);");
    }

    @Test
    public void testmax16() {
        assertEval("argv <- list(structure(c(11368.8306749654, 11347.7238090355, 11341.9182102121, 11392.4878842821, 11367.3445285107, 11337.9245694652, 11332.0560643654, 11356.4682624019, 11387.6852128883, 11364.9132677, 11391.3319486445, 11374.2254758319, 11347.9708838458, 11353.2031583386, 11333.3748092474, 11323.9154302836, 11373.0896246266, 11330.2228965024, 11354.2399044028, 11367.8070731596, 11392.4287034031, 11357.6915504499, 11356.9044667059, 11335.1409634408, 11375.8374661156, 11354.7726842454, 11358.781884864, 11337.5281579299, 11339.0060699913, 11364.6998397419, 11363.2410538797, 11328.3945066198, 11325.487840571, 11367.9956844538, 11388.0030639744, 11364.8664695648, 11362.2630523606, 11359.821940674, 11391.9566656714, 11358.5349275633, 11370.7951655071, 11365.1078852355, 11339.7208074429, 11341.0716148671, 11374.0516736354, 11354.6799581982, 11335.2588737891, 11375.2688788734, 11330.349134828, 11383.518146432, 11366.0251480173, 11362.0011677193, 11346.0144123337, 11354.7192011815, 11358.0308680837, 11335.6606452791, 11360.0741421962, 11328.2693021996, 11342.4429152855, 11337.8889663466, 11342.9353336683, 11385.6565872063, 11354.2364726327, 11377.5989422849, 11384.6433324409, 11351.9186946652, 11327.4665936357, 11346.4841244179, 11373.6608162634, 11346.6330733448, 11367.1289885738, 11381.8430187805, 11382.9292165297, 11350.3951496719, 11349.6345719923, 11385.6811798196, 11368.1021034038, 11374.8755054101, 11365.3712412571, 11386.2157128048, 11343.5611108569, 11336.3882076922, 11385.0515660313, 11358.2337640012, 11384.3940280117, 11336.2435535709, 11376.0672136671, 11373.7149224868, 11389.0607372806, 11361.3352610911, 11372.8220707406, 11350.2233569878, 11330.0611188328, 11387.9111462012, 11342.8262750218, 11364.340121117, 11330.7252423461, 11381.8354922482, 11345.257457911, 11377.7995935893), class = 'Date'), na.rm = TRUE);max(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testmax17() {
        assertEval(Output.ContainsError, "argv <- list(structure(c(3L, 2L, 1L), .Label = c('A', 'B', 'C'), class = c('ordered', 'factor')), na.rm = FALSE);max(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testmax18() {
        assertEval("argv <- list(structure(c(1338544800L, 1338566400L, 1338588000L, 1338609600L, 1338631200L, 1338652800L, 1338674400L, 1338696000L, 1338717600L, 1338739200L, 1338760800L, 1338782400L, 1338804000L, 1338825600L, 1338847200L, 1338868800L, 1338890400L, 1338912000L, 1338933600L, 1338955200L, 1338976800L, 1338998400L, 1339020000L, 1339041600L), class = c('POSIXct', 'POSIXt'), tzone = ''), na.rm = TRUE);max(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testmax19() {
        assertEval("argv <- list(structure(list(c(1L, 2L, 4L), 1:3, c(2L, 1L)), class = c('package_version', 'numeric_version')), na.rm = FALSE);max(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testmax20() {
        assertEval("argv <- list(structure(c(1208822400, 1209168000), class = c('POSIXct', 'POSIXt'), tzone = 'GMT'), na.rm = FALSE);max(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testmax21() {
        assertEval("argv <- list(structure(c(13823, NA), class = 'Date'), na.rm = TRUE);max(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testmax22() {
        assertEval("argv <- list(structure(c(7L, 4L, 3L), .Dim = 3L, .Dimnames = structure(list(c('0', '1', '5')), .Names = ''), class = 'table'));max(argv[[1]]);");
    }

    @Test
    public void testmax23() {
        assertEval("argv <- list(structure(c(0, 1, 1, 1, 1), .Names = c('Hair', 'Eye', 'Sex', 'Hair:Eye', 'Hair:Sex')));max(argv[[1]]);");
    }

    @Test
    public void testmax24() {
        assertEval("argv <- list(structure(c(Inf, Inf, 2.248e+263, Inf, 3.777e+116, 1.128), .Names = c('Min.', '1st Qu.', 'Median', 'Mean', '3rd Qu.', 'Max.')));max(argv[[1]]);");
    }

    @Test
    public void testmax25() {
        assertEval("argv <- list(structure(c(-11.3814849918875, -11.9361690778798, 0.562602893455921, 11.5126028934559, 76.2209544348296, -8.66448499188751, -6.94502893455923, -5.28148499188751, -35.7665182531098, 6.35497106544077, -9.20908119253651, -0.898484991887508, -5.59380090589508, -6.12730922120065, -13.3061334505138, 58.6278831800973, -15.1098009058951, -8.29625696322337, -4.07211681990265, 3.7096551514332, 2.60151500811249, 6.24733923742563, -1.33911681990266, -2.14157287723094, -10.5984849918875, -8.12802893455923, 1.30028697944835, -15.7450289345592, 7.20569077879935, -12.6484849918875, 25.1810423201731, -4.42680090589508, -1.90886979448351), .Names = c('Craig Dunain', 'Ben Rha', 'Ben Lomond', 'Goatfell', 'Bens of Jura', 'Cairnpapple', 'Scolty', 'Traprain', 'Lairig Ghru', 'Dollar', 'Lomonds', 'Cairn Table', 'Eildon Two', 'Cairngorm', 'Seven Hills', 'Knock Hill', 'Black Hill', 'Creag Beag', 'Kildcon Hill', 'Meall Ant-Suidhe', 'Half Ben Nevis', 'Cow Hill', 'N Berwick Law', 'Creag Dubh', 'Burnswark', 'Largo Law', 'Criffel', 'Acmony', 'Ben Nevis', 'Knockfarrel', 'Two Breweries', 'Cockleroi', 'Moffat Chase')));max(argv[[1]]);");
    }

    @Test
    public void testmax26() {
        assertEval("argv <- list(c(1, 0.987688340595138, 0.951056516295154, 0.891006524188368, 0.809016994374947, 0.707106781186547, 0.587785252292473, 0.453990499739547, 0.309016994374947, 0.156434465040231, -1.83697019872103e-16, -0.156434465040231, -0.309016994374948, -0.453990499739547, -0.587785252292473, -0.707106781186548, -0.809016994374948, -0.891006524188368, -0.951056516295154, -0.987688340595138, -1, -0.987688340595138, -0.951056516295154, -0.891006524188368, -0.809016994374947, -0.707106781186547, -0.587785252292473, -0.453990499739547, -0.309016994374947, -0.156434465040231, 6.12323399573677e-17, 0.156434465040231, 0.309016994374947, 0.453990499739547, 0.587785252292473, 0.707106781186548, 0.809016994374947, 0.891006524188368, 0.951056516295154, 0.987688340595138, 1, 0.987688340595138, 0.951056516295154, 0.891006524188368, 0.809016994374947, 0.707106781186548, 0.587785252292473, 0.453990499739547, 0.309016994374947, 0.156434465040231, 6.12323399573677e-17, -0.15643446504023, -0.309016994374947, -0.453990499739548, -0.587785252292473, -0.707106781186547, -0.809016994374947, -0.891006524188368, -0.951056516295154, -0.987688340595138, -1, -0.987688340595138, -0.951056516295154, -0.891006524188368, -0.809016994374948, -0.707106781186547, -0.587785252292473, -0.453990499739548, -0.309016994374948, -0.15643446504023, -1.83697019872103e-16, 0.15643446504023, 0.309016994374947, 0.453990499739547, 0.587785252292473, 0.707106781186547, 0.809016994374947, 0.891006524188368, 0.951056516295154, 0.987688340595138, 1, 0.987688340595138, 0.951056516295154, 0.891006524188368, 0.809016994374948, 0.707106781186547, 0.587785252292473, 0.453990499739548, 0.309016994374948, 0.15643446504023, 3.06161699786838e-16, -0.15643446504023, -0.309016994374947, -0.453990499739547, -0.587785252292473, -0.707106781186547, -0.809016994374947, -0.891006524188368, -0.951056516295153, -0.987688340595138, -1));max(argv[[1]]);");
    }

    @Test
    public void testmax28() {
        assertEval("argv <- structure(list(2, 3, NA, na.rm = TRUE), .Names = c('',     '', '', 'na.rm'));do.call('max', argv)");
    }

    @Test
    public void testmax29() {
        assertEval("argv <- list(2, 3, NA);do.call('max', argv)");
    }

    @Test
    public void testMaximum() {
        assertEval("{ max((-1):100) }");
        assertEval("{ max(2L, 4L) }");
        assertEval("{ max() }");
        assertEval("{ max(1:10, 100:200, c(4.0, 5.0)) }");
        assertEval("{ max(NA, 1.1) }");
        assertEval("{ max(0/0, 1.1) }");
        assertEval("{ max(0/0, 1.1, NA) }");
        assertEval("{ max(c(as.character(NA), \"foo\")) }");
        assertEval("{ max(character(0)) }");
        assertEval("{ max(character()) }");
        assertEval("{ max(double(0)) }");
        assertEval("{ max(double()) }");
        assertEval("{ max(NULL) }");

        assertEval("{ max(1:10, 100:200, c(4.0, 5.0), c(TRUE,FALSE,NA)) }");
        assertEval("{ max(c(\"hi\",\"abbey\",\"hello\")) }");
        assertEval("{ max(\"hi\",\"abbey\",\"hello\") }");

        assertEval("{ is.logical(max(TRUE, FALSE)) }");
        assertEval("{ is.logical(max(TRUE)) }");
        assertEval("{ max(as.raw(42), as.raw(7)) }");
        assertEval(Output.ContainsError, "{ max(42+42i, 7+7i) }");
        assertEval("{ max(\"42\", \"7\") }");

        assertEval("{ max(as.double(NA), na.rm=FALSE) }");
        assertEval(Output.ContainsWarning, "{ max(as.double(NA), as.double(NA), na.rm=TRUE) }");
        assertEval("{ max(as.double(NA), as.double(NA), na.rm=FALSE) }");
        assertEval("{ max(as.integer(NA), na.rm=FALSE) }");
        assertEval("{ max(as.integer(NA), as.integer(NA), na.rm=FALSE) }");
        assertEval(Output.ContainsWarning, "{ max(as.character(NA), na.rm=TRUE) }");
        assertEval("{ max(as.character(NA), na.rm=FALSE) }");
        assertEval(Output.ContainsWarning, "{ max(as.character(NA), as.character(NA), na.rm=TRUE) }");
        assertEval("{ max(as.character(NA), as.character(NA), na.rm=FALSE) }");
        assertEval("{ max(42L, as.integer(NA), na.rm=TRUE) }");
        assertEval("{ max(42L, as.integer(NA), na.rm=FALSE) }");
        assertEval("{ max(42, as.double(NA), na.rm=TRUE) }");
        assertEval("{ max(42, as.double(NA), na.rm=FALSE) }");
        assertEval("{ max(\"42\", as.character(NA), na.rm=TRUE) }");
        assertEval("{ max(\"42\", as.character(NA), na.rm=FALSE) }");
        assertEval("{ max(42L, as.integer(NA), 7L, na.rm=TRUE) }");
        assertEval("{ max(42L, as.integer(NA), 7L, na.rm=FALSE) }");
        assertEval("{ max(42, as.double(NA), 7, na.rm=TRUE) }");
        assertEval("{ max(42, as.double(NA), 7, na.rm=FALSE) }");
        assertEval("{ max(\"42\", as.character(NA), \"7\", na.rm=TRUE) }");
        assertEval("{ max(\"42\", as.character(NA), \"7\", na.rm=FALSE) }");

        assertEval("{ max(as.character(NA), as.character(NA), \"42\", na.rm=TRUE) }");
        assertEval("{ max(as.character(NA), as.character(NA), \"42\", \"7\", na.rm=TRUE) }");

        assertEval("{ max(123, NA, TRUE, 12, FALSE, na.rm=TRUE) }");
        assertEval("{ max(123, NA, TRUE, 12, FALSE, na.rm=FALSE) }");
        assertEval("{ max(123, NA, TRUE, 12, FALSE) }");

        assertEval(Ignored.Unknown, Output.ContainsWarning, "{ max(integer(0)) }");
        assertEval(Ignored.Unknown, Output.ContainsWarning, "{ max(integer()) }");
        assertEval(Ignored.Unknown, Output.ContainsWarning, "{ max(as.double(NA), na.rm=TRUE) }");
        assertEval(Ignored.Unknown, Output.ContainsWarning, "{ max(as.integer(NA), na.rm=TRUE) }");
        assertEval(Ignored.Unknown, Output.ContainsWarning, "{ max(as.integer(NA), as.integer(NA), na.rm=TRUE) }");
    }
}
