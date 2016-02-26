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

import org.junit.*;

import com.oracle.truffle.r.test.*;

// Checkstyle: stop line length check
public class TestBuiltin_dim extends TestBase {

    @Test
    public void testdim1() {
        assertEval("argv <- list(structure(c(3, 3, 3, 3, 3, 3, 3, 3, 4, 3, 2, 1, 2, 3, 4, 5), .Dim = c(8L, 2L), .Dimnames = list(c('a', 'b', 'c', 'd', 'e', 'f', 'g', 'h'), c('x1', 'x2'))));dim(argv[[1]]);");
    }

    @Test
    public void testdim2() {
        assertEval("argv <- list(structure(list(surname = structure(1:5, .Label = c('McNeil', 'Ripley', 'Tierney', 'Tukey', 'Venables'), class = 'factor'), nationality = structure(c(1L, 2L, 3L, 3L, 1L), .Label = c('Australia', 'UK', 'US'), class = 'factor'), deceased = structure(c(1L, 1L, 1L, 2L, 1L), .Label = c('no', 'yes'), class = 'factor'), title = structure(c(NA_integer_, NA_integer_, NA_integer_, NA_integer_, NA_integer_), .Label = c('An Introduction to R', 'Exploratory Data Analysis', 'Interactive Data Analysis', 'LISP-STAT', 'Modern Applied Statistics ...', 'Spatial Statistics', 'Stochastic Simulation'), class = 'factor'), other.author = structure(c(NA_integer_, NA_integer_, NA_integer_, NA_integer_, NA_integer_), .Label = c('Ripley', 'Venables & Smith'), class = 'factor')), .Names = c('surname', 'nationality', 'deceased', 'title', 'other.author'), row.names = c(5L, 4L, 3L, 1L, 2L), class = 'data.frame'));dim(argv[[1]]);");
    }

    @Test
    public void testdim3() {
        assertEval("argv <- list(structure(c(0.100837939896365, 0.100837939896365, -2.89916206010363, 1.10083793989637, 2.10083793989637, 15.3663689601092, 5.36636896010918, -15.5557367724452, 1.44426322755481, -15.5557367724452, -6.55573677244519, 2.44426322755481, -20.5557367724452, 3.41611550389052, 37.4161155038905, 8.41611550389052, 23.0292477138695, -29.9707522861305, 3.02924771386952, -25.9707522861305, 5.46163181127829, 1.46163181127829, 22.4616318112783, -11.5383681887217, 17.4616318112783, -24.5383681887217, 2.79509291794369, -19.2049070820563, -32.2049070820563, 10.7950929179437, -18.2049070820563, 17.7950929179437, 2.79509291794369, 25.7950929179437, 9.79509291794369, -3.77239539978251, 13.2276046002175, -0.772395399782511, 23.2276046002175, -2.77239539978251, 1.22760460021749, -12.7723953997825, 13.2276046002175, 7.22760460021749, 7.22760460021749, -36.8669529292102, 13.1330470707898, 12.1330470707898, -18.8669529292102, -7.86695292921016, -18.3408059382389, 14.7301967628363), .Dim = c(52L, 1L)));dim(argv[[1]]);");
    }

    @Test
    public void testdim4() {
        assertEval("argv <- list(structure(c(3L, 3L, 3L, 1L, 1L, 1L, 1L, 2L, 2L, 2L, 2L, 3L, 3L, 3L, 3L, 1L, 1L, 1L, 1L, 2L, 2L, 2L, 2L, 3L, 3L, 3L, 3L, 1L, 1L, 1L, 1L, 2L, 2L, 2L, 2L, 3L, 3L, 3L, 3L, 1L, 1L, 1L, 1L, 2L, 2L, 2L, 2L, 3L, 3L, 3L, 3L, 1L, 1L, 1L, 1L, 2L, 2L, 2L, 2L, 3L, 3L, 3L, 3L, 1L, 1L, 1L, 1L, 2L, 2L, 2L, 2L), .Label = c('Golden.rain', 'Marvellous', 'Victory'), class = 'factor', contrasts = 'contr.treatment'));dim(argv[[1]]);");
    }

    @Test
    public void testdim5() {
        assertEval("argv <- list(structure(list(df = 10L, ss = 2.74035772634541, ms = 0.274035772634541, f = NA_real_, P = NA_real_), .Names = c('df', 'ss', 'ms', 'f', 'P'), row.names = c(NA, -1L), class = 'data.frame'));dim(argv[[1]]);");
    }

    @Test
    public void testdim6() {
        assertEval("argv <- list(structure(list(File = c('GOusage.Rnw', 'annotate.Rnw', 'chromLoc.Rnw', 'prettyOutput.Rnw', 'query.Rnw', 'useDataPkgs.Rnw', 'useHomology.Rnw', 'useProbeInfo.Rnw'), Title = c('Basic GO Usage', 'Annotation Overview', 'HowTo: use chromosomal information', 'HowTo: Get HTML Output', 'HOWTO: Use the online query tools', 'Using Data Packages', 'Using the homology package', 'Using Affymetrix Probe Level Data'), PDF = c('GOusage.pdf', 'annotate.pdf', 'chromLoc.pdf', 'prettyOutput.pdf', 'query.pdf', 'useDataPkgs.pdf', 'useHomology.pdf', 'useProbeInfo.pdf'), Depends = list(c('GO.db', 'hgu95av2.db', 'Biobase'), c('Biobase', 'genefilter', 'annotate', 'hgu95av2.db'), c('annotate', 'hgu95av2.db'), c('annotate', 'hgu95av2.db'), c('annotate', 'hgu95av2.db'), c('hgu95av2.db', 'GO.db'), 'hom.Hs.inp.db', c('hgu95av2.db', 'rae230a.db', 'rae230aprobe', 'Biostrings')), Keywords = list(c('GO', 'ontology'), c('Expression Analysis', 'Annotation'), c('Expression Analysis', 'Annotation'), c('Expression Analysis', 'Annotation'),     c('Expression Analysis', 'Annotation'), 'Annotation', 'Annotation', 'Annotation'), R = c('GOusage.R', 'annotate.R', 'chromLoc.R', 'prettyOutput.R', 'query.R', 'useDataPkgs.R', 'useHomology.R', 'useProbeInfo.R')), .Names = c('File', 'Title', 'PDF', 'Depends', 'Keywords', 'R'), row.names = c(NA, -8L), class = 'data.frame'));dim(argv[[1]]);");
    }

    @Test
    public void testdim7() {
        assertEval("argv <- list(c(2832L, 2836L, 2836L, 2833L, 2833L));dim(argv[[1]]);");
    }

    @Test
    public void testdim8() {
        assertEval("argv <- list(structure(list(day = structure(1:6, .Label = c('2012-06-01', '2012-06-02', '2012-06-03', '2012-06-04', '2012-06-05', '2012-06-06', '2012-06-07'), class = 'factor')), .Names = 'day', row.names = c(1L, 5L, 9L, 13L, 17L, 21L), class = 'data.frame'));dim(argv[[1]]);");
    }

    @Test
    public void testdim9() {
        assertEval("argv <- list(c(NA, NA, '/home/lzhao/tmp/RtmpGUHe0I/R.INSTALL2aa51f3e9d31/lattice/R/cloud.R', '/home/lzhao/tmp/RtmpGUHe0I/R.INSTALL2aa51f3e9d31/lattice/R/cloud.R', '/home/lzhao/tmp/RtmpGUHe0I/R.INSTALL2aa51f3e9d31/lattice/R/cloud.R', '/home/lzhao/tmp/RtmpGUHe0I/R.INSTALL2aa51f3e9d31/lattice/R/cloud.R', '/home/lzhao/tmp/RtmpGUHe0I/R.INSTALL2aa51f3e9d31/lattice/R/cloud.R'));dim(argv[[1]]);");
    }

    @Test
    public void testdim10() {
        assertEval("argv <- list(structure(c(93.3042409622253, 72.8638419893434, 65.9708818502055, 74.2809886424684, 79.8889070755712, 100.233809580112, 104.965351919781, 83.86798597082, 105.795365211341, 108.580858711588, 95.3646703714076, 98.1558192431132, 5.58314117171466, 29.7542740978848, 32.2224082035474, 27.8665232792916, 25.046508702598, 23.5818201384803, 35.0327999599812, 33.2751275770215, 43.2947043117474, 39.1828378794408, 11.7874053171943, 57.3766532219607), .Dim = c(12L, 2L)));dim(argv[[1]]);");
    }

    @Test
    public void testdim11() {
        assertEval("argv <- list(structure(c(1+1i, 3+1i, 2+1i, 4+1i, 5-1i, 7-1i, 6-1i, 8-1i), .Dim = c(2L, 2L, 2L)));dim(argv[[1]]);");
    }

    @Test
    public void testdim12() {
        assertEval("argv <- list(structure(list(height = numeric(0), weight = numeric(0)), .Names = c('height', 'weight'), class = 'data.frame', row.names = integer(0)));dim(argv[[1]]);");
    }

    @Test
    public void testdim13() {
        assertEval("argv <- list(structure(c(3+2i, 3+2i, NA, 3+2i, 3+2i, 3+2i, 3+2i, 3+2i, 4-5i, 3-5i, NA, NA, 2-5i, 3-5i, 4-5i, 5-5i), .Dim = c(8L, 2L), .Dimnames = list(NULL, c('x1', 'x2'))));dim(argv[[1]]);");
    }

    @Test
    public void testdim14() {
        assertEval("argv <- list(c(99, 1, 2, -3, 4, 3, NA));dim(argv[[1]]);");
    }

    @Test
    public void testdim15() {
        assertEval("argv <- list(structure(c('‘[,1]’', '‘[,2]’', '‘height’', '‘weight’', 'numeric', 'numeric', 'Height (in) ', 'Weight (lbs)'), .Dim = c(2L, 4L)));dim(argv[[1]]);");
    }

    @Test
    public void testdim16() {
        assertEval("argv <- list(structure(list(V1 = c(0.497699242085218, 0.991906094830483), V2 = c(0.668466738192365, 0.107943625887856), V3 = c(0.0994661601725966, 0.518634263193235), V4 = c(0.892198335845023, 0.389989543473348), V5 = c(0.79730882588774, 0.410084082046524)), .Names = c('V1', 'V2', 'V3', 'V4', 'V5'), row.names = c(16L, 18L), class = 'data.frame'));dim(argv[[1]]);");
    }

    @Test
    public void testdim17() {
        assertEval("argv <- list(structure(c(FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE), .Dim = c(5L, 21L), .Dimnames = list(c('#ifdef', '\\\\Sexpr', 'build', 'install', 'render'), NULL)));dim(argv[[1]]);");
    }

    @Test
    public void testdim18() {
        assertEval("argv <- list(structure(c(0, 0, 0, 0, 0, -1.43884556914512e-134, 0, 0, 0, -7.95468296571581e-252, 1.76099882882167e-260, 0, -9.38724727098368e-323, -0.738228974836154, 0, 0, 0, 0, 0, 0, 0, 0, 0, -6.84657791618065e-123, 0, 0, 0, 0, 0, 0, 0, 0, 0, -1.05931985100232e-174, 0, -3.41789378681991e-150, 0, 0, 0, 0, -1.07225492686949e-10, 0, 1.65068934474523e-67, 0, -6.49830035279282e-307, 0, 5.83184963977238e-90, 0, -9.81722610183938e-287, 6.25336419454196e-54, 0, 0, 0, -1.72840591500382e-274, 1.22894687952101e-13, 0.660132850077566, 0, 0, 7.79918925397516e-200, -2.73162827952857e-178, 1.32195942051179e-41, 0, 0, 0, 0, 2.036057023761e-45, -3.40425060445074e-186, 1.59974269220388e-26, 0, 6.67054294775317e-124, 0.158503117506202, 0, 0, 0, 0, 0, 0, 3.42455724859116e-97, 0, 0, -2.70246891320217e-272, 0, 0, -3.50562438899045e-06, 0, 0, 1.35101732326608e-274, 0, 0, 0, 0, 0, 0, 0, 7.24580295957621e-65, 0, -3.54887341172294e-149, 0, 0, 0, 0, 0, 0, 0, 0, 1.77584594753563e-133, 0, 0, 0, 2.88385135688311e-250, 1.44299633616158e-259, 0, 1.56124744085834e-321, 1.63995835868977, 0, 0, 0, 0, 0, 0, 0, 0, 0, 2.01050064173383e-122, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1.64868196850938e-172, 0, 6.28699823828692e-149, 0, 0, 0, 0, 5.0552295590188e-09, 0, 2.30420733561404e-66, 0, 7.0823279075443e-306, 0, 2.05009901740696e-88, 0, 7.41800724282869e-285, 7.18347043784483e-53, 0, 0, 0, 1.04251223075649e-273, 9.75816316577433e-13, 4.29519957592147, 0, 0, 1.33541454912682e-198, 2.34606233784019e-176, 8.38236726536896e-41, 0, 0, 0, 0, 1.35710537434521e-43, 1.15710503176511e-185, 1.25601735272233e-25, 0, 4.46811655846376e-123, 4.4196641795634, 0, 0, 0, 0, 0, 0, 3.74179015251531e-93, 0, 0, 3.62662047836582e-271, 0, 0, 1.26220330674453e-05, 0, 0, 1.72715562657338e-273, 0, 0, 0, 0, 0, 0, 0, 5.46372806810809e-64, 0, 2.47081972486962e-148, 0, 0, 0), .Dim = c(100L, 2L)));dim(argv[[1]]);");
    }

    @Test
    public void testdim19() {
        assertEval("argv <- list(structure(c(0, 87, 82, 75, 63, 50, 43, 32, 35, 60, 54, 55, 36, 39, 0, 0, 69, 57, 57, 51, 45, 37, 46, 39, 36, 24, 32, 23, 25, 32, 0, 32, 59, 74, 75, 60, 71, 61, 71, 57, 71, 68, 79, 73, 76, 71, 67, 75, 79, 62, 63, 57, 60, 49, 48, 52, 57, 62, 61, 66, 71, 62, 61, 57, 72, 83, 71, 78, 79, 71, 62, 74, 76, 64, 62, 57, 80, 73, 69, 69, 71, 64, 69, 62, 63, 46, 56, 44, 44, 52, 38, 46, 36, 49, 35, 44, 59, 65, 65, 56, 66, 53, 61, 52, 51, 48, 54, 49, 49, 61, 0, 0, 68, 44, 40, 27, 28, 25, 24, 24), .Tsp = c(1945, 1974.75, 4), class = 'ts'));dim(argv[[1]]);");
    }

    @Test
    public void testdim20() {
        assertEval("argv <- list(structure(raw(0), .Dim = c(0L, 0L)));dim(argv[[1]]);");
    }

    @Test
    public void testdim21() {
        assertEval("argv <- list(c(0, 1, 131072, 129140163, 17179869184, 762939453125, 16926659444736, 232630513987207, 2251799813685248, 16677181699666568, 1e+17));dim(argv[[1]]);");
    }

    @Test
    public void testdim22() {
        assertEval("argv <- list(structure(1:20, .Tsp = c(1960.08333333333, 1961.66666666667, 12), class = 'ts'));dim(argv[[1]]);");
    }

    @Test
    public void testdim23() {
        assertEval("argv <- list(structure(c(1, 0, -1, 0.5, -0.5, NA, NA, NA, 0), .Dim = c(3L, 3L)));dim(argv[[1]]);");
    }

    @Test
    public void testdim24() {
        assertEval("argv <- list(c(NA, NA, NA, NA, NA, 'Ripley', 'Venables & Smith'));dim(argv[[1]]);");
    }

    @Test
    public void testdim25() {
        assertEval("argv <- list(c(FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE));dim(argv[[1]]);");
    }

    @Test
    public void testdim26() {
        assertEval("argv <- list(structure(c(31.9624451639742, -0.0105001367774998, 0.669596455370805, 5.14945152173688, 4.36500574231155, 6.30517873115649, 2.15562516502081, 1.77361974340981, 1.31721464996217, -6.23951992881235, -3.14333173153233, 3.7074414096456, 0.619951366669834, -3.31636282291298, -1.38277940327151, -0.0105001367774998, 4.92314051157491e-06, -0.00208176000671443, -0.00393685976143421, -0.00306114840585819, -0.00304239831702958, -0.00360485387426285, -0.000776863531985863, -0.000930425548039592, 0.00177325947012459, 0.000873478088813267, -0.00222133591240768, -0.0010997058222662, 0.000971542840761233, 0.000408638350886089, 0.669596455370805, -0.00208176000671443, 6.25966104941131, 7.27107350691908, 7.1568051334171, 7.69164996000523, 6.38546846168565, 0.584255359868468, 0.425893553320666, -0.302152926219182, -0.539719353375365, 0.767832198930969, -0.0339253942409593, -0.954875198336619, -0.44454733172958, 5.14945152173688, -0.00393685976143421, 7.27107350691908, 16.9287891082998, 9.68170957680298, 10.1927925852141, 8.31848730773964, 0.0225771728679827, -0.13423885912137, -0.952279072677042, -1.28303151957048, -0.309366054071832, 0.277949783704086, -1.95357808926458, -1.1490330193693, 4.36500574231155, -0.00306114840585819, 7.1568051334171, 9.68170957680298, 10.6200372990959, 12.0025058084698, 7.09661626032293, 0.00831356355794886, -0.18522684338686, -1.13540989802495, -1.33382692805767, 0.308573868840132, -0.809762853334073, -1.09668270906855, -0.213078730283059, 6.30517873115649, -0.00304239831702958, 7.69164996000523, 10.1927925852141, 12.0025058084698, 17.4522685874698, 6.92295996047857, -0.125541819250371, -0.215552520930932, -1.84365094178865, -1.59654434238815, 0.684384781199279, -1.93856751571012, -1.02265709591595, -0.303713451023131, 2.15562516502081, -0.00360485387426285, 6.38546846168565, 8.31848730773964, 7.09661626032293, 6.92295996047857, 23.2114402831465, -0.28786423278137, 0.360665125986693, -0.292516552346824, -1.61368872459996, 0.400045448449001, 1.49305046916227, -1.52595532739395, -2.45569084011985, 1.77361974340981, -0.000776863531985863, 0.584255359868468, 0.0225771728679827, 0.00831356355794886, -0.125541819250371, -0.28786423278137, 3.63382138185307, 1.73220942447866, 0.201523343362283, 0.961824629517822, 2.18512927857691, -0.299291412368627, -0.222951433523371, 0.190518621032026, 1.31721464996217, -0.000930425548039592, 0.425893553320666, -0.13423885912137, -0.18522684338686, -0.215552520930932, 0.360665125986693, 1.73220942447866, 2.07264968016672, 0.409359459121014, 0.882002960805309, 1.87214770160952, 0.189008347036503, -0.266586895155729, -0.112011626327013, -6.23951992881235, 0.00177325947012459, -0.302152926219182, -0.952279072677042, -1.13540989802495, -1.84365094178865, -0.292516552346824, 0.201523343362283, 0.409359459121014, 2.48234483048294, 1.19369459724506, -0.189893084140488, 0.16622651987368, 0.28664359918476, -0.0113387579323685, -3.14333173153233, 0.000873478088813267, -0.539719353375365, -1.28303151957048, -1.33382692805767, -1.59654434238815, -1.61368872459996, 0.961824629517822, 0.882002960805309, 1.19369459724506, 2.16828149626507, 0.76585533428598, 0.0326711935947258, 0.375684864300291, 0.0175473410796721, 3.7074414096456, -0.00222133591240768, 0.767832198930969, -0.309366054071832, 0.308573868840132, 0.684384781199279, 0.400045448449001, 2.18512927857691, 1.87214770160952, -0.189893084140488, 0.76585533428598, 4.87998635701286, 0.240260053826388, 0.639583107689077, 1.24508720406537, 0.619951366669834, -0.0010997058222662, -0.0339253942409593, 0.277949783704086, -0.809762853334073, -1.93856751571012, 1.49305046916227, -0.299291412368627, 0.189008347036503, 0.16622651987368, 0.0326711935947258, 0.240260053826388, 1.27358692244222, -0.271133086074816, -0.61768767314107, -3.31636282291298, 0.000971542840761233, -0.954875198336619, -1.95357808926458, -1.09668270906855, -1.02265709591595, -1.52595532739395, -0.222951433523371, -0.266586895155729, 0.28664359918476, 0.375684864300291, 0.639583107689077, -0.271133086074816, 2.24773578295184, 2.00648977390012, -1.38277940327151, 0.000408638350886089, -0.44454733172958, -1.1490330193693, -0.213078730283059, -0.303713451023131, -2.45569084011985, 0.190518621032026, -0.112011626327013, -0.0113387579323685, 0.0175473410796721, 1.24508720406537, -0.61768767314107, 2.00648977390012, 3.44090885157986), .Dim = c(15L, 15L), .Dimnames = list(c('(Intercept)', 'Weight', 'Cylinders4', 'Cylinders5', 'Cylinders6', 'Cylinders8', 'Cylindersrotary', 'TypeLarge', 'TypeMidsize', 'TypeSmall', 'TypeSporty', 'TypeVan', 'EngineSize', 'DriveTrainFront', 'DriveTrainRear'), c('(Intercept)', 'Weight', 'Cylinders4', 'Cylinders5', 'Cylinders6', 'Cylinders8', 'Cylindersrotary', 'TypeLarge', 'TypeMidsize', 'TypeSmall', 'TypeSporty', 'TypeVan', 'EngineSize', 'DriveTrainFront', 'DriveTrainRear'))));dim(argv[[1]]);");
    }

    @Test
    public void testdim27() {
        assertEval("argv <- list(structure(c(-3.001e+155, -1.067e+107, -1.976e+62, -9.961e+152, -2.059e+23, 1), .Names = c('Min.', '1st Qu.', 'Median', 'Mean', '3rd Qu.', 'Max.'), class = 'table'));dim(argv[[1]]);");
    }

    @Test
    public void testdim28() {
        assertEval("argv <- list(structure(1395082220.91387, class = c('POSIXct', 'POSIXt')));dim(argv[[1]]);");
    }

    @Test
    public void testdim30() {
        assertEval("argv <- list(structure(list(Sepal.Length = c(5.1, 4.9, 4.7, 4.6,     5, 5.4, 4.6, 5, 4.4, 4.9, 5.4, 4.8, 4.8, 4.3, 5.8, 5.7, 5.4,     5.1, 5.7, 5.1, 5.4, 5.1, 4.6, 5.1, 4.8, 5, 5, 5.2, 5.2, 4.7,     4.8, 5.4, 5.2, 5.5, 4.9, 5, 5.5, 4.9, 4.4, 5.1, 5, 4.5, 4.4,     5, 5.1, 4.8, 5.1, 4.6, 5.3, 5, 7, 6.4, 6.9, 5.5, 6.5, 5.7,     6.3, 4.9, 6.6, 5.2, 5, 5.9, 6, 6.1, 5.6, 6.7, 5.6, 5.8, 6.2,     5.6, 5.9, 6.1, 6.3, 6.1, 6.4, 6.6, 6.8, 6.7, 6, 5.7, 5.5,     5.5, 5.8, 6, 5.4, 6, 6.7, 6.3, 5.6, 5.5, 5.5, 6.1, 5.8, 5,     5.6, 5.7, 5.7, 6.2, 5.1, 5.7, 6.3, 5.8, 7.1, 6.3, 6.5, 7.6,     4.9, 7.3, 6.7, 7.2, 6.5, 6.4, 6.8, 5.7, 5.8, 6.4, 6.5, 7.7,     7.7, 6, 6.9, 5.6, 7.7, 6.3, 6.7, 7.2, 6.2, 6.1, 6.4, 7.2,     7.4, 7.9, 6.4, 6.3, 6.1, 7.7, 6.3, 6.4, 6, 6.9, 6.7, 6.9,     6.8, 6.7, 6.7, 6.3, 6.5, 6.2, 5.9), Sepal.Width = c(3.5,     3, 3.2, 3.1, 3.6, 3.9, 3.4, 3.4, 2.9, 3.1, 3.7, 3.4, 3, 3,     4, 4.4, 3.9, 3.5, 3.8, 3.8, 3.4, 3.7, 3.6, 3.3, 3.4, 3, 3.4,     3.5, 3.4, 3.2, 3.1, 3.4, 4.1, 4.2, 3.1, 3.2, 3.5, 3.6, 3,     3.4, 3.5, 2.3, 3.2, 3.5, 3.8, 3, 3.8, 3.2, 3.7, 3.3, 3.2,     3.2, 3.1, 2.3, 2.8, 2.8, 3.3, 2.4, 2.9, 2.7, 2, 3, 2.2, 2.9,     2.9, 3.1, 3, 2.7, 2.2, 2.5, 3.2, 2.8, 2.5, 2.8, 2.9, 3, 2.8,     3, 2.9, 2.6, 2.4, 2.4, 2.7, 2.7, 3, 3.4, 3.1, 2.3, 3, 2.5,     2.6, 3, 2.6, 2.3, 2.7, 3, 2.9, 2.9, 2.5, 2.8, 3.3, 2.7, 3,     2.9, 3, 3, 2.5, 2.9, 2.5, 3.6, 3.2, 2.7, 3, 2.5, 2.8, 3.2,     3, 3.8, 2.6, 2.2, 3.2, 2.8, 2.8, 2.7, 3.3, 3.2, 2.8, 3, 2.8,     3, 2.8, 3.8, 2.8, 2.8, 2.6, 3, 3.4, 3.1, 3, 3.1, 3.1, 3.1,     3.2, 3.3, 3, 2.5, 3, 3.4, 3), Petal.Length = c(1.4, 1.4,     1.3, 1.5, 1.4, 1.7, 1.4, 1.5, 1.4, 1.5, 1.5, 1.6, 1.4, 1.1,     1.2, 1.5, 1.3, 1.4, 1.7, 1.5, 1.7, 1.5, 1, 1.7, 1.9, 1.6,     1.6, 1.5, 1.4, 1.6, 1.6, 1.5, 1.5, 1.4, 1.5, 1.2, 1.3, 1.4,     1.3, 1.5, 1.3, 1.3, 1.3, 1.6, 1.9, 1.4, 1.6, 1.4, 1.5, 1.4,     4.7, 4.5, 4.9, 4, 4.6, 4.5, 4.7, 3.3, 4.6, 3.9, 3.5, 4.2,     4, 4.7, 3.6, 4.4, 4.5, 4.1, 4.5, 3.9, 4.8, 4, 4.9, 4.7, 4.3,     4.4, 4.8, 5, 4.5, 3.5, 3.8, 3.7, 3.9, 5.1, 4.5, 4.5, 4.7,     4.4, 4.1, 4, 4.4, 4.6, 4, 3.3, 4.2, 4.2, 4.2, 4.3, 3, 4.1,     6, 5.1, 5.9, 5.6, 5.8, 6.6, 4.5, 6.3, 5.8, 6.1, 5.1, 5.3,     5.5, 5, 5.1, 5.3, 5.5, 6.7, 6.9, 5, 5.7, 4.9, 6.7, 4.9, 5.7,     6, 4.8, 4.9, 5.6, 5.8, 6.1, 6.4, 5.6, 5.1, 5.6, 6.1, 5.6,     5.5, 4.8, 5.4, 5.6, 5.1, 5.9, 5.7, 5.2, 5, 5.2, 5.4, 5.1),     Petal.Width = c(0.2, 0.2, 0.2, 0.2, 0.2, 0.4, 0.3, 0.2, 0.2,         0.1, 0.2, 0.2, 0.1, 0.1, 0.2, 0.4, 0.4, 0.3, 0.3, 0.3,         0.2, 0.4, 0.2, 0.5, 0.2, 0.2, 0.4, 0.2, 0.2, 0.2, 0.2,         0.4, 0.1, 0.2, 0.2, 0.2, 0.2, 0.1, 0.2, 0.2, 0.3, 0.3,         0.2, 0.6, 0.4, 0.3, 0.2, 0.2, 0.2, 0.2, 1.4, 1.5, 1.5,         1.3, 1.5, 1.3, 1.6, 1, 1.3, 1.4, 1, 1.5, 1, 1.4, 1.3,         1.4, 1.5, 1, 1.5, 1.1, 1.8, 1.3, 1.5, 1.2, 1.3, 1.4,         1.4, 1.7, 1.5, 1, 1.1, 1, 1.2, 1.6, 1.5, 1.6, 1.5, 1.3,         1.3, 1.3, 1.2, 1.4, 1.2, 1, 1.3, 1.2, 1.3, 1.3, 1.1,         1.3, 2.5, 1.9, 2.1, 1.8, 2.2, 2.1, 1.7, 1.8, 1.8, 2.5,         2, 1.9, 2.1, 2, 2.4, 2.3, 1.8, 2.2, 2.3, 1.5, 2.3, 2,         2, 1.8, 2.1, 1.8, 1.8, 1.8, 2.1, 1.6, 1.9, 2, 2.2, 1.5,         1.4, 2.3, 2.4, 1.8, 1.8, 2.1, 2.4, 2.3, 2.3, 2.5, 2.3,         1.9, 2, 2.3, 1.8), Species = structure(c(1L, 1L, 1L,         1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L,         1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L,         1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L,         1L, 1L, 1L, 1L, 1L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L,         2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L,         2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L,         2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 3L,         3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L,         3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L,         3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L,         3L, 3L, 3L, 3L, 3L, 3L), .Label = c('setosa', 'versicolor',         'virginica'), class = 'factor')), .Names = c('Sepal.Length',     'Sepal.Width', 'Petal.Length', 'Petal.Width', 'Species'),     row.names = c(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L, 11L,         12L, 13L, 14L, 15L, 16L, 17L, 18L, 19L, 20L, 21L, 22L,         23L, 24L, 25L, 26L, 27L, 28L, 29L, 30L, 31L, 32L, 33L,         34L, 35L, 36L, 37L, 38L, 39L, 40L, 41L, 42L, 43L, 44L,         45L, 46L, 47L, 48L, 49L, 50L, 51L, 52L, 53L, 54L, 55L,         56L, 57L, 58L, 59L, 60L, 61L, 62L, 63L, 64L, 65L, 66L,         67L, 68L, 69L, 70L, 71L, 72L, 73L, 74L, 75L, 76L, 77L,         78L, 79L, 80L, 81L, 82L, 83L, 84L, 85L, 86L, 87L, 88L,         89L, 90L, 91L, 92L, 93L, 94L, 95L, 96L, 97L, 98L, 99L,         100L, 101L, 102L, 103L, 104L, 105L, 106L, 107L, 108L,         109L, 110L, 111L, 112L, 113L, 114L, 115L, 116L, 117L,         118L, 119L, 120L, 121L, 122L, 123L, 124L, 125L, 126L,         127L, 128L, 129L, 130L, 131L, 132L, 133L, 134L, 135L,         136L, 137L, 138L, 139L, 140L, 141L, 142L, 144L, 145L,         146L, 147L, 148L, 149L, 150L), class = 'data.frame'));" +
                        "do.call('dim', argv)");
    }

    @Test
    public void testDimensions() {
        assertEval("{ dim(1) }");
        assertEval("{ dim(1:3) }");
        assertEval("{ m <- matrix(1:6, nrow=3) ; dim(m) }");

        assertEval("{ nrow(1) }");
        assertEval("{ nrow(1:3) }");
        assertEval("{ NROW(1) }");
        assertEval("{ NROW(1:3) }");
        assertEval("{ ncol(1) }");
        assertEval("{ ncol(1:3) }");
        assertEval("{ NCOL(1) }");
        assertEval("{ NCOL(1:3) }");

        assertEval("{ m <- matrix(1:6, nrow=3) ; nrow(m) }");
        assertEval("{ m <- matrix(1:6, nrow=3) ; ncol(m) }");

        assertEval("{ z <- 1 ; dim(z) <- c(1,1) ; dim(z) <- NULL ; z }");

        assertEval("{ x <- 1:4 ; f <- function() { x <- 1:4 ; dim(x) <<- c(2,2) } ; f() ; dim(x) }");

        assertEval("{ x<-1:12; dim(x)<-c(12); x }");
        assertEval(Output.ContainsWarning, "{ x<-1:12; dim(x)<-c(12+10i); x }");
        assertEval("{ x<-1:12; dim(x)<-c(as.raw(12)); x }");
        assertEval("{ x<-1:12; dim(x)<-c(\"12\"); x }");
        assertEval("{ x<-1:1; dim(x)<-c(TRUE); x }");

        assertEval("{ x<-1:12; dim(x)<-c(3, 4); attr(x, \"dim\") }");
        assertEval("{ x<-1:12; attr(x, \"dim\")<-c(3, 4); dim(x) }");
        assertEval("{ x<-1:4; names(x)<-c(21:24); attr(x, \"foo\")<-\"foo\"; x }");
        assertEval("{ x<-list(1,2,3); names(x)<-c(21:23); attr(x, \"foo\")<-\"foo\"; x }");

        assertEval("{ x <- 1:2 ; dim(x) <- c(1, 3) ; x }");
        assertEval("{ x <- 1:2 ; dim(x) <- c(1, NA) ; x }");
        assertEval("{ x <- 1:2 ; dim(x) <- c(1, -1) ; x }");
        assertEval("{ x <- 1:2 ; dim(x) <- integer() ; x }");
        assertEval("{ b <- c(a=1+2i,b=3+4i) ; attr(b,\"my\") <- 211 ; dim(b) <- c(2,1) ; names(b) }");

        assertEval("{ x<-1:4; dim(x)<-c(4); y<-101:104; dim(y)<-c(4); x > y }");
        assertEval("{ x<-1:4; y<-101:104; dim(y)<-c(4); x > y }");
        assertEval("{ x<-1:4; dim(x)<-c(4); y<-101:104; x > y }");
        assertEval("{ x<-1:4; dim(x)<-c(4); y<-101:104; dim(y)<-c(2,2); x > y }");
        assertEval("{ x<-1:4; dim(x)<-c(4); y<-101:108; dim(y)<-c(8); x > y }");

        assertEval("{ x<-c(1); dim(x)<-1; names(x)<-c(\"b\"); attributes(x) }");
        assertEval("{ x<-c(1); dim(x)<-1; attr(x, \"dimnames\")<-list(\"b\"); attributes(x) }");
        assertEval("{ x<-c(1); dim(x)<-1; attr(x, \"dimnames\")<-list(a=\"b\"); attributes(x) }");
        // reset all attributes
        assertEval("{ x<-c(42); names(x)<-\"a\"; attr(x, \"dim\")<-1; names(x)<-\"z\"; dim(x)<-NULL; attributes(x) }");
        // reset dimensions and dimnames
        assertEval("{ x<-c(42); names(x)<-\"a\"; attr(x, \"dim\")<-1; names(x)<-\"z\"; attr(x, \"foo\")<-\"foo\"; attr(x, \"dim\")<-NULL; attributes(x) }");
        // second names() invocation sets "dimnames"
        assertEval("{ x<-c(42); names(x)<-\"a\"; attr(x, \"dim\")<-1; names(x)<-\"z\"; attributes(x) }");
        // third names() invocation resets "names" (and not "dimnames")
        assertEval("{ x<-c(42); names(x)<-\"a\"; attr(x, \"dim\")<-1; names(x)<-\"z\"; names(x)<-NULL; attributes(x) }");
        // both names and dimnames are set and then re-set
        assertEval("{ x<-c(42); names(x)<-\"a\"; attr(x, \"dim\")<-1; names(x)<-\"z\"; names(x)<-NULL; attr(x, \"dimnames\")<-NULL; attributes(x) }");
        assertEval("{ x<-1:4; attr(x, \"dimnames\") <- list(101, 102, 103, 104) }");
        // assigning an "invisible" list returned by "attr(y, dimnames)<-" as dimnames attribute for
        // x
        assertEval("{ x<-c(1); y<-c(1); dim(x)<-1; dim(y)<-1; attr(x, \"dimnames\")<-(attr(y, \"dimnames\")<-list(\"b\")); attributes(x) }");
        assertEval("{ x<-1:4; dim(x)<-c(2,1,2); dimnames(x)<-list(NULL); attributes(x) }");
        assertEval("{ x<-1:4; dim(x)<-c(2,1,2); dimnames(x) <- list(c(\"a\")); x }");
        assertEval("{ x<-1:4; dim(x)<-c(2,1,2); dimnames(x) <- list(c(\"a\", \"b\"), NULL, c(\"d\")); x }");
        assertEval("{ x<-1:4; dim(x)<-c(2,1,2); dimnames(x) <- list(c(\"a\", \"b\"), 42, c(\"d\", \"e\", \"f\")); attributes(x) }");
        assertEval("{ x<-1:4; dim(x)<-c(2,1,2); dimnames(x) <- list(c(\"a\", \"b\"), \"c\", c(\"d\", \"e\"), 7); attributes(x) }");
        assertEval("{ x<-1:4; dim(x)<-c(2,1,2); dimnames(x)<-list(c(\"a\", \"b\"), \"c\", c(\"d\", \"e\")); attributes(x) }");
        assertEval("{ x<-1:4; dim(x)<-c(2,1,2); dimnames(x)<-list(c(\"a\", \"b\"), 42, c(\"d\", \"e\")); attributes(x) }");

        // there should be no output
        assertEval("{ x<-42; y<-(dim(x)<-1); }");
        assertEval("{ x<-42; y<-(dim(x)<-1); y }");
        // dim vector should not be shared
        assertEval("{ x<-1:4; y<-c(2, 2); dim(x)<-y; y[1]=4; dim(x) }");
        assertEval("{ x<-1; dim(x)=1; attr(x, \"foo\")<-\"foo\"; dim(x)<-NULL; attributes(x) }");
        assertEval("{ x<-1; dim(x)=1; attr(x, \"names\")<-\"a\"; dim(x)<-NULL; attributes(x) }");
        assertEval("{ x<-1; dim(x)=1; names(x)<-\"a\"; dim(x)<-NULL; attributes(x) }");
        assertEval("{ x<-1:2; dim(x)=c(1,2); names(x)<-c(\"a\", \"b\"); attr(x, \"foo\")<-\"foo\"; dim(x)<-NULL; attributes(x) }");
        assertEval("{ x<-1:4; names(x)<-c(21:24); attr(x, \"dim\")<-c(4); attr(x, \"foo\")<-\"foo\"; x }");
        assertEval("{ x<-list(1,2,3); names(x)<-c(21:23); attr(x, \"dim\")<-c(3); attr(x, \"foo\")<-\"foo\"; x }");

        assertEval("{ x<-1; dimnames(x) }");
        assertEval("{ dimnames(1) }");
        assertEval("{ dimnames(NULL) }");
        assertEval("{ x<-1; dim(x)<-1; dimnames(x) <- 1; dimnames(x) }");
        assertEval("{ x<-1; dim(x)<-1; attr(x, \"dimnames\") <- 1 }");
        assertEval("{ x<-1; dim(x)<-1; dimnames(x)<-list() }");
        assertEval("{ x<-1; dim(x)<-1; dimnames(x)<-list(0) }");
        assertEval("{ x<-1; dim(x)<-1; dimnames(x)<-list(\"a\"); dimnames(x); dimnames(x)<-list(); dimnames(x) }");

        assertEval("{ x <- 1:2 ; dim(x) <- c(1,2) ; x }");
        assertEval("{ x <- 1:2 ; attr(x, \"dim\") <- c(2,1) ; x }");

        assertEval("{ n <- 17 ; fac <- factor(rep(1:3, length = n), levels = 1:5) ; y<-tapply(1:n, fac, sum); attributes(y) }");
    }
}
