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
public class TestBuiltin_sub extends TestBase {

    @Test
    public void testsub1() {
        assertEval("argv <- list('^..dfd.', '', c('aa', '..dfd.row.names'), FALSE, FALSE, FALSE, FALSE); .Internal(sub(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]]))");
    }

    @Test
    public void testsub2() {
        assertEval("argv <- list('[b-e]', '.', c('The', 'licenses', 'for', 'most', 'software', 'are', 'designed', 'to', 'take', 'away', 'your', 'freedom', 'to', 'share', 'and', 'change', 'it.', '', 'By', 'contrast,', 'the', 'GNU', 'General', 'Public', 'License', 'is', 'intended', 'to', 'guarantee', 'your', 'freedom', 'to', 'share', 'and', 'change', 'free', 'software', '--', 'to', 'make', 'sure', 'the', 'software', 'is', 'free', 'for', 'all', 'its', 'users'), FALSE, TRUE, FALSE, FALSE); .Internal(sub(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]]))");
    }

    @Test
    public void testsub3() {
        assertEval("argv <- list('%bm', 'http://www.bioconductor.org', c('@CRAN@', 'http://www.stats.ox.ac.uk/pub/RWin', '%bm/packages/%v/bioc', '%bm/packages/%v/data/annotation', '%bm/packages/%v/data/experiment', '%bm/packages/%v/extra', 'http://www.omegahat.org/R', 'http://R-Forge.R-project.org', 'http://www.rforge.net'), FALSE, FALSE, TRUE, FALSE); .Internal(sub(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]]))");
    }

    @Test
    public void testsub4() {
        assertEval(Ignored.Unknown,
                        "argv <- list('^  \\036 ', '\\036', c('', '  \\036 Merged in a set of Splus code changes that had accumulated at Mayo', '    over the course of a decade. The primary one is a change in how', '    indexing is done in the underlying C code, which leads to a major', '    speed increase for large data sets.  Essentially, for the lower', '    leaves all our time used to be eaten up by bookkeeping, and this', '    was replaced by a different approach.  The primary routine also', '    uses .Call{} so as to be more memory efficient.', '', '  \\036 The other major change was an error for asymmetric loss matrices,', '    prompted by a user query.  With L=loss asymmetric, the altered', '    priors were computed incorrectly - they were using L' instead of L.', '    Upshot - the tree would not not necessarily choose optimal splits', '    for the given loss matrix.  Once chosen, splits were evaluated', '    correctly.  The printed “improvement” values are of course the', '    wrong ones as well.  It is interesting that for my little test', '    case, with L quite asymmetric, the early splits in the tree are', '    unchanged - a good split still looks good.', '', '  \\036 Add the return.all argument to xpred.rpart().', '', '  \\036 Added a set of formal tests, i.e., cases with known answers to', '    which we can compare.', '', '  \\036 Add a usercode vignette, explaining how to add user defined', '    splitting functions.', '', '  \\036 The class method now also returns the node probability.', '', '  \\036 Add the stagec data set, used in some tests.', '', '  \\036 The plot.rpart routine needs to store a value that will be visible', '    to the rpartco routine at a later time.  This is now done in an', '    environment in the namespace.', ''), FALSE, FALSE, FALSE, FALSE); .Internal(sub(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]]))");
    }

    @Test
    public void testsub5() {
        assertEval("argv <- list('./', '', structure(c('./data', './html', './po/en@quot/LC_MESSAGES', './po/en@quot', './po/pl/LC_MESSAGES', './po/pl', './po/de/LC_MESSAGES', './po/de', './po', './doc/SuiteSparse', './doc', './Meta', './include', './R', './help', './libs', './external'), class = 'AsIs'), FALSE, FALSE, FALSE, FALSE); .Internal(sub(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]]))");
    }

    @Test
    public void testsub6() {
        assertEval("argv <- list('\\\'', '\\\\\\\'', '\\\\method{as.dist}{default}', FALSE, FALSE, TRUE, FALSE); .Internal(sub(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]]))");
    }

    @Test
    public void testsub7() {
        assertEval("argv <- list('(\\\\w)(\\\\w*)(\\\\w)', '\\\\U\\\\1\\\\E\\\\2\\\\U\\\\3', 'useRs may fly into JFK or laGuardia', FALSE, TRUE, FALSE, FALSE); .Internal(sub(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]]))");
    }

    @Test
    public void testsub8() {
        assertEval("argv <- list('^(msgstr)\\\\[([[:digit:]]+)\\\\].*$', '\\\\1\\\\\\\\[\\\\2\\\\\\\\]', 'msgstr[0] \\\'%d ligne de poids nul non comptabilis<U+00E9>e\\\'', FALSE, FALSE, FALSE, FALSE); .Internal(sub(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]]))");
    }

    @Test
    public void testsub9() {
        assertEval(Ignored.Unknown,
                        "argv <- list('[[:space:]]*$', '', 'identical() has a new argument, ignore.environment, used when\\ncomparing functions (with default FALSE as before).\\n\\n\\036There is a new option, options(CBoundsCheck=), which controls how\\n.C() and .Fortran() pass arguments to compiled code.  If true\\n(which can be enabled by setting the environment variable\\nR_C_BOUNDS_CHECK to yes), raw, integer, double and complex\\narguments are always copied, and checked for writing off either end\\nof the array on return from the compiled code (when a second copy\\nis made).  This also checks individual elements of character\\nvectors passed to .C().\\n\\nThis is not intended for routine use, but can be very helpful in\\nfinding segfaults in package code.\\n\\n\\036In layout(), the limits on the grid size have been raised (again).\\n\\n\\036New simple provideDimnames() utility function.\\n\\n\\036Where methods for length() return a double value which is\\nrepresentable as an integer (as often happens for package Matrix),\\nthis is converted to an integer.\\n\\n\\036Matrix indexing of dataframes by two-column numeric indices is now\\nsupported for replacement as well as extraction.\\n\\n\\036setNames() now has a default for its object argument, useful for a\\ncharacter result.\\n\\n\\036StructTS() has a revised additive constant in the loglik component\\nof the result: the previous definition is returned as the loglik0\\ncomponent.  However, the help page has always warned of a lack of\\ncomparability of log-likelihoods for non-stationary models.\\n(Suggested by Jouni Helske.)\\n\\n\\036The logic in aggregate.formula() has been revised.  It is now\\npossible to use a formula stored in a variable; previously, it had\\nto be given explicitly in the function call.\\n\\n\\036install.packages() has a new argument quiet to reduce the amount of\\noutput shown.\\n\\n\\036Setting an element of the graphics argument lwd to a negative or\\ninfinite value is now an error.  Lines corresponding to elements\\nwith values NA or NaN are silently omitted.\\n\\nPreviously the behaviour was device-dependent.\\n\\n\\036Setting graphical parameters cex, col, lty, lwd and pch in par()\\nnow requires a length-one argument.  Previously some silently took\\nthe first element of a longer vector, but not always when\\ndocumented to do so.\\n\\n\\036Sys.which() when used with inputs which would be unsafe in a shell\\n(e.g. absolute paths containing spaces) now uses appropriate\\nquoting.\\n\\n\\036as.tclObj() has been extended to handle raw vectors.  Previously,\\nit only worked in the other direction.  (Contributed by Charlie\\nFriedemann, PR#14939.)\\n\\n\\036New functions cite() and citeNatbib() have been added, to allow\\ngeneration of in-text citations from \\\'bibentry\\\' objects.  A cite()\\nfunction may be added to bibstyle() environments.\\n\\n\\036A sort() method has been added for \\\'bibentry\\\' objects.\\n\\n\\036The bibstyle() function now defaults to setting the default\\nbibliography style. The getBibstyle() function has been added to\\nreport the name of the current default style.\\n\\n\\036scatter.smooth() now has an argument lpars to pass arguments to\\nlines().\\n\\n\\036pairs() has a new log argument, to allow some or all variables to\\nbe plotted on logarithmic scale.  (In part, wish of PR#14919.)\\n\\n\\036split() gains a sep argument.\\n\\n\\036termplot() does a better job when given a model with interactions\\n(and no longer attempts to plot interaction terms).\\n\\n\\036The parser now incorporates code from Romain Francois' parser\\npackage, to support more detailed computation on the code, such as\\nsyntax highlighting, comment-based documentation, etc.  Functions\\ngetParseData() and getParseText() access the data.\\n\\n\\036There is a new function rep_len() analogous to rep.int() for when\\nspeed is required (and names are not).\\n\\n\\036The undocumented use rep(NULL, length.out = n) for n > 0 (which\\nreturns NULL) now gives a warning.\\n\\n\\036demo() gains an encoding argument for those packages with non-ASCII\\ndemos: it defaults to the package encoding where there is one.\\n\\n\\036strwrap() converts inputs with a marked encoding to the current\\nlocale: previously it made some attempt to pass through as bytes\\ninputs invalid in the current locale.\\n\\n\\036Specifying both rate and scale to [dpqr]gamma is a warning (if they\\nare essentially the same value) or an error.\\n\\n\\036merge() works in more cases where the data frames include matrices.\\n(Wish of PR#14974.)\\n\\n\\036optimize() and uniroot() no longer use a shared parameter object\\nacross calls.  (nlm(), nlminb() and optim() with numerical\\nderivatives still do, as documented.)\\n\\n\\036The all.equal() method for date-times is now documented: times are\\nregarded as equal (by default) if they differ by up to 1 msec.\\n\\n\\036duplicated() and unique() gain a nmax argument which can be used to\\nmake them much more efficient when it is known that there are only\\na small number of unique entries.  This is done automatically for\\nfactors.\\n\\n\\036Functions rbinom(), rgeom(), rhyper(), rpois(), rnbinom(),\\nrsignrank() and rwilcox() now return integer (not double) vectors.\\nThis halves the storage requirements for large simulations.\\n\\n\\036sort(), sort.int() and sort.list() now use radix sorting for\\nfactors of less than 100,000 levels when method is not supplied.\\nSo does order() if called with a single factor, unless na.last =\\nNA.\\n\\n\\036diag() as used to generate a diagonal matrix has been re-written in\\nC for speed and less memory usage.  It now forces the result to be\\nnumeric in the case diag(x) since it is said to have ‘zero\\noff-diagonal entries’.\\n\\n\\036backsolve() (and forwardsolve()) are now internal functions, for\\nspeed and support for large matrices.\\n\\n\\036More matrix algebra functions (e.g. chol() and solve()) accept\\nlogical matrices (and coerce to numeric).\\n\\n\\036sample.int() has some support for n >= 2^31: see its help for the\\nlimitations.\\n\\nA different algorithm is used for (n, size, replace = FALSE, prob =\\nNULL) for n > 1e7 and size <= n/2.  This is much faster and uses\\nless memory, but does give different results.\\n\\n\\036approxfun() and splinefun() now return a wrapper to an internal\\nfunction in the stats namespace rather than a .C() or .Call() call.\\nThis is more likely to work if the function is saved and used in a\\ndifferent session.\\n\\n\\036The functions .C(), .Call(), .External() and .Fortran() now give an\\nerror (rather than a warning) if called with a named first\\nargument.\\n\\n\\036Sweave() by default now reports the locations in the source file(s)\\nof each chunk.\\n\\n\\036clearPushBack() is now a documented interface to a long-existing\\ninternal call.\\n\\n\\036aspell() gains filters for R code, Debian Control Format and\\nmessage catalog files, and support for R level dictionaries.  In\\naddition, package utils now provides functions\\naspell_package_R_files() and aspell_package_C_files() for spell\\nchecking R and C level message strings in packages.\\n\\n\\036bibentry() gains some support for “incomplete” entries with a\\ncrossref field.\\n\\n\\036gray() and gray.colors() finally allow alpha to be specified.\\n\\n\\036monthplot() gains parameters to control the look of the reference\\nlines.  (Suggestion of Ian McLeod.)\\n\\n\\036Added support for new %~% relation (“is distributed as”) in\\nplotmath.\\n\\n\\036domain = NA is accepted by gettext() and ngettext(), analogously to\\nstop() etc.\\n\\n\\036termplot() gains a new argument plot = FALSE which returns\\ninformation to allow the plots to be modified for use as part of\\nother plots, but does not plot them.  (Contributed by Terry\\nTherneau, PR#15076.)\\n\\n\\036quartz.save(), formerly an undocumented part of R.app, is now\\navailable to copy a device to a quartz() device.  dev.copy2pdf()\\noptionally does this for PDF output: quartz.save() defaults to PNG.\\n\\n\\036The default method of pairs() now allows text.panel = NULL and the\\nuse of <foo>.panel = NULL is now documented.\\n\\n\\036setRefClass() and getRefClass() now return class generator\\nfunctions, similar to setClass(), but still with the reference\\nfields and methods as before (suggestion of Romain Francois).\\n\\n\\036New functions bitwNot(), bitwAnd(), bitwOr() and bitwXor(), using\\nthe internal interfaces previously used for classes \\\'octmode\\\' and\\n\\\'hexmode\\\'.\\n\\nAlso bitwShiftL() and bitwShiftR() for shifting bits in elements of\\ninteger vectors.\\n\\n\\036New option \\\'deparse.cutoff\\\' to control the deparsing of language\\nobjects such as calls and formulae when printing.  (Suggested by a\\ncomment of Sarah Goslee.)\\n\\n\\036colors() gains an argument distinct.\\n\\n\\036New demo(colors) and demo(hclColors), with utility functions.\\n\\n\\036list.files() (aka dir()) gains a new optional argument no.. which\\nallows to exclude \\\'.\\\' and \\\'..\\\' from listings.\\n\\n\\036Multiple time series are also of class \\\'matrix\\\'; consequently,\\nhead(), e.g., is more useful.\\n\\n\\036encodeString() preserves UTF-8 marked encodings.  Thus if factor\\nlevels are marked as UTF-8 an attempt is made to print them in\\nUTF-8 in RGui on Windows.\\n\\n\\036readLines() and scan() (and hence read.table()) in a UTF-8 locale\\nnow discard a UTF-8 byte-order-mark (BOM).  Such BOMs are allowed\\nbut not recommended by the Unicode Standard: however Microsoft\\napplications can produce them and so they are sometimes found on\\nwebsites.\\n\\nThe encoding name \\\'UTF-8-BOM\\\' for a connection will ensure that a\\nUTF-8 BOM is discarded.\\n\\n\\036mapply(FUN, a1, ..) now also works when a1 (or a further such\\nargument) needs a length() method (which the documented arguments\\nnever do).  (Requested by Hervé Pagès; with a patch.)\\n\\n\\036.onDetach() is supported as an alternative to .Last.lib.  Unlike\\n.Last.lib, this does not need to be exported from the package's\\nnamespace.\\n\\n\\036The srcfile argument to parse() may now be a character string, to\\nbe used in error messages.\\n\\n\\036The format() method for ftable objects gains a method argument,\\npropagated to write.ftable() and print(), allowing more compact\\noutput, notably for LaTeX formatting, thanks to Marius Hofert.\\n\\n\\036The utils::process.events() function has been added to trigger\\nimmediate event handling.\\n\\n\\036Sys.which() now returns NA (not \\\'\\\') for NA inputs (related to\\nPR#15147).\\n\\n\\036The print() method for class \\\'htest\\\' gives fewer trailing spaces\\n(wish of PR#15124).\\n\\nAlso print output from HoltWinters(), nls() and others.\\n\\n\\036loadNamespace() allows a version specification to be given, and\\nthis is used to check version specifications given in the Imports\\nfield when a namespace is loaded.\\n\\n\\036setClass() has a new argument, slots, clearer and less ambiguous\\nthan representation.  It is recommended for future code, but should\\nbe back-compatible.  At the same time, the allowed slot\\nspecification is slightly more general.  See the documentation for\\ndetails.\\n\\n\\036mget() now has a default for envir (the frame from which it is\\ncalled), for consistency with get() and assign().\\n\\n\\036close() now returns an integer status where available, invisibly.\\n(Wish of PR#15088.)\\n\\n\\036The internal method of tar() can now store paths too long for the\\nustar format, using the (widely supported) GNU extension.  It can\\nalso store long link names, but these are much less widely\\nsupported.  There is support for larger files, up to the ustar\\nlimit of 8GB.\\n\\n\\036Local reference classes have been added to package methods.  These\\nare a technique for avoiding unneeded copying of large components\\nof objects while retaining standard R functional behavior.  See\\n?LocalReferenceClasses.\\n\\n\\036untar() has a new argument restore_times which if false (not the\\ndefault) discards the times in the tarball.  This is useful if they\\nare incorrect (some tarballs submitted to CRAN have times in a\\nlocal timezone or many years in the past even though the standard\\nrequired them to be in UTC).\\n\\n\\036replayplot() cannot (and will not attempt to) replay plots recorded\\nunder R < 3.0.0.  It may crash the R session if an attempt is made\\nto replay plots created in a different build of R >= 3.0.0.\\n\\n\\036Palette changes get recorded on the display list, so replaying\\nplots (including when resizing screen devices and using dev.copy())\\nwill work better when the palette is changed during a plot.\\n\\n\\036chol(pivot = TRUE) now defaults to LAPACK, not LINPACK.\\n\\n\\036The parse() function has a new parameter keep.source, which\\ndefaults to options(\\\'keep.source\\\').\\n\\n\\036Profiling via Rprof() now optionally records information at the\\nstatement level, not just the function level.\\n\\n\\036The Rprof() function now quotes function names in in its output\\nfile on Windows, to be consistent with the quoting in Unix.\\n\\n\\036Profiling via Rprof() now optionally records information about time\\nspent in GC.\\n\\n\\036The HTML help page for a package now displays non-vignette\\ndocumentation files in a more accessible format.\\n\\n\\036To support options(stringsAsFactors = FALSE), model.frame(),\\nmodel.matrix() and replications() now automatically convert\\ncharacter vectors to factors without a warning.\\n\\n\\036The print method for objects of class \\\'table\\\' now detects tables\\nwith 0-extents and prints the results as, e.g., < table of extent 0\\nx 1 x 2 >. (Wish of PR#15198.)\\n\\n\\036Deparsing involving calls to anonymous functions and has been made\\ncloser to reversible by the addition of extra parentheses.\\n\\n\\036The function utils::packageName() has been added as a lightweight\\nversion of methods::getPackageName().\\n\\n\\036find.package(lib.loc = NULL) now treats loaded namespaces\\npreferentially in the same way as attached packages have been for a\\nlong time.\\n\\n\\036In Windows, the Change Directory dialog now defaults to the current\\nworking directory, rather than to the last directory chosen in that\\ndialog.\\n\\n\\036available.packages() gains a \\\'license/restricts_use\\\' filter which\\nretains only packages for which installation can proceed solely\\nbased on packages which are guaranteed not to restrict use.\\n\\n\\036New check_packages_in_dir() function in package tools for\\nconveniently checking source packages along with their reverse\\ndependencies.\\n\\n\\036R's completion mechanism has been improved to handle help requests\\n(starting with a question mark).  In particular, help prefixes are\\nnow supported, as well as quoted help topics.  To support this,\\ncompletion inside quotes are now handled by R by default on all\\nplatforms.\\n\\n\\036The memory manager now allows the strategy used to balance garbage\\ncollection and memory growth to be controlled by setting the\\nenvironment variable R_GC_MEM_GROW. See ?Memory for more details.\\n\\n\\036(‘For experts only’, as the introductory manual says.)  The use of\\nenvironment variables R_NSIZE and R_VSIZE to control the initial (=\\nminimum) garbage collection trigger for number of cons cels and\\nsize of heap has been restored: they can be overridden by the\\ncommand-line options --min-nsize and --min-vsize; see ?Memory.\\n\\n\\036On Windows, the device name for bitmap devices as reported by\\n.Device and .Devices no longer includes the file name.  This is for\\nconsistency with other platforms and was requested by the lattice\\nmaintainer.\\n\\nwin.metafile() still uses the file name: the exact form is used by\\npackage tkrplot.\\n\\n\\036set.seed(NULL) re-initializes .Random.seed as done at the beginning\\nof the session if not already set.  (Suggestion of Bill Dunlap.)\\n\\n\\036The breaks argument in hist.default() can now be a function that\\nreturns the breakpoints to be used (previously it could only return\\nthe suggested number of breakpoints).\\n\\n\\036File share/licenses/licenses.db has some clarifications, especially\\nas to which variants of ‘BSD’ and ‘MIT’ is intended and how to\\napply them to packages.  The problematic licence ‘Artistic-1.0’ has\\nbeen removed.\\n', FALSE, FALSE, FALSE, FALSE); .Internal(sub(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]]))");
    }

    @Test
    public void testsub10() {
        assertEval("argv <- list('^[[:space:]]*([[:alnum:].]+).*$', '\\\\1', structure('MASS', .Names = 'Suggests'), FALSE, FALSE, FALSE, FALSE); .Internal(sub(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]]))");
    }

    @Test
    public void testsub11() {
        assertEval("argv <- list(' +$', '', c(NA, '1', NA, '2', '1', NA, NA, '1', '4', '1', NA, '4', '1', '3', NA, '4', '2', '2', NA, '4', '4', '2', '4', '4', '2', '1', '4', '4', '3', '1', '1', '4', '1', '4', NA, '1', '4', '4', '2', '2', '4', '4', '3', '4', '2', '2', '3', '3', '4', '1', '1', '1', '4', '1', '4', '4', '4', '4', NA, '4', '4', '4', NA, '1', '2', '3', '4', '3', '4', '2', '4', '4', '1', '4', '1', '4', NA, '4', '2', '1', '4', '1', '1', '1', '4', '4', '2', '4', '1', '1', '1', '4', '1', '1', '1', '4', '3', '1', '4', '3', '2', '4', '3', '1', '4', '2', '4', NA, '4', '4', '4', '2', '1', '4', '4', NA, '2', '4', '4', '1', '1', '1', '1', '4', '1', '2', '3', '2', '1', '4', '4', '4', '1', NA, '4', '2', '2', '2', '4', '4', '3', '3', '4', '2', '4', '3', '1', '1', '4', '2', '4', '3', '1', '4', '3', '4', '4', '1', '1', '4', '4', '3', '1', '1', '2', '1', '3', '4', '2', '2', '2', '4', '4', '3', '2', '1', '1', '4', '1', '1', '2', NA, '2', '3', '3', '2', '1', '1', '1', '1', '4', '4', '4', '4', '4', '4', '2', '2', '1', '4', '1', '4', '3', '4', '2', '3', '1', '3', '1', '4', '1', '4', '1', '4', '3', '3', '4', '4', '1', NA, '3', '4', '4', '4', '4', '4', '4', '3', '4', '3', '4', '2', '4', '4', '1', '2', NA, '4', '4', '4', '4', '1', '2', '1', '1', '2', '1', '4', '2', '3', '1', '4', '4', '4', '1', '2', '1', '4', '2', '1', '3', '1', '2', '2', '1', '2', '1', NA, '3', '2', '2', '4', '1', '4', '4', '2', '4', '4', '4', '2', '1', '4', '2', '4', '4', '4', '4', '4', '1', '3', '4', '3', '4', '1', NA, '4', NA, '1', '1', '1', '4', '4', '4', '4', '2', '4', '3', '2', NA, '1', '4', '4', '3', '4', '4', '4', '2', '4', '2', '1', '4', '4', NA, '4', '4', '3', '3', '4', '2', '2', '4', '1', '4', '4', '4', '3', '4', '4', '4', '3', '2', '1', '3', '1', '4', '1', '4', '2', NA, '1', '4', '4', '3', '1', '4', '1', '4', '1', '4', '4', '1', '2', '2', '1', '4', '1', '1', '4', NA, '4', NA, '4', '4', '4', '1', '4', '2', '1', '2', '2', '2', '2', '1', '1', '2', '1', '4', '2', '3', '3', '1', '3', '1', '4', '1', '3', '2', '2', '4', '1', NA, '3', '4', '2', '4', '4', '4', '4', '4', '4', '3', '4', '4', '3', '2', '1', '4', '4', '2', '4', '2', '1', '2', '1', '1', '1', '1', '4', '4', '1', '1', '4', '1', '4', '4', '4', '1', '1', NA, '3', '2', '4', '4', '4', '4', '2', '3', '3', '2', NA, '4', '2', '4', '4', '1', '1', '4', '4', '1', '1', '4', '1', '2', '2', '2', '2', '1', '4', '4', '1', '2', '2', '2', '3', '4', '4', '3', '4', '1', '1', '4', '4', NA, '4', '1', '4', '4', '4', '1', '4', '4', '1', '2', '4', '4', '4', '4', '1', '2', '4', '4', '2', '1', '4', '2', '4', '2', '2', '4', '1', '3', '3', '2', '4', '1', '4', '4', '4', '1', NA, '4', '4', '2', '4', '4', '4', '4', '4', '2', NA, '4', '2', '4', '3', '1', '4', '4', '3', '4', '2', '4', '4', '1', '2', '1', '4', '1', '3', '3', '1', '4', '4', '2', '4', '4', '4', '4', '3', '2', '3', '3', '2', NA, '3', '4', '4', '3', '3', '4', '4', '4', '1', '4', '4', '4', '4', '4', '4', '4', '2', '4', '2', '3', '4', '1', '3', '1', NA, '4', '1', '2', '2', '1', '4', '3', '3', '4', '1', '1', '3'), FALSE, FALSE, FALSE, FALSE); .Internal(sub(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]]))");
    }

    @Test
    public void testsub12() {
        assertEval(Ignored.Unknown,
                        "argv <- list('.* : ', '', structure('Error in rnorm(2, c(1, NA)) : (converted from warning) NAs produced\\n', class = 'try-error', condition = structure(list(message = '(converted from warning) NAs produced', call = quote(rnorm(2, c(1, NA)))), .Names = c('message', 'call'), class = c('simpleError', 'error', 'condition'))), FALSE, FALSE, FALSE, FALSE); .Internal(sub(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]]))");
    }

    @Test
    public void testsub13() {
        assertEval(Ignored.Unknown,
                        "argv <- list('.* : ', '', structure('Error in rexp(2, numeric()) : (converted from warning) NAs produced\\n', class = 'try-error', condition = structure(list(message = '(converted from warning) NAs produced', call = quote(rexp(2, numeric()))), .Names = c('message', 'call'), class = c('simpleError', 'error', 'condition'))), FALSE, FALSE, FALSE, FALSE); .Internal(sub(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]]))");
    }

    @Test
    public void testsub14() {
        assertEval(Ignored.Unknown,
                        "argv <- list('.* : ', '', structure('Error in rnorm(2, numeric()) : (converted from warning) NAs produced\\n', class = 'try-error', condition = structure(list(message = '(converted from warning) NAs produced', call = quote(rnorm(2, numeric()))), .Names = c('message', 'call'), class = c('simpleError', 'error', 'condition'))), FALSE, FALSE, FALSE, FALSE); .Internal(sub(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]]))");
    }

    @Test
    public void testsub15() {
        assertEval(Ignored.Unknown,
                        "argv <- list('.* : ', '', structure('Error in rnorm(1, sd = Inf) : (converted from warning) NAs produced\\n', class = 'try-error', condition = structure(list(message = '(converted from warning) NAs produced', call = quote(rnorm(1, sd = Inf))), .Names = c('message', 'call'), class = c('simpleError', 'error', 'condition'))), FALSE, FALSE, FALSE, FALSE); .Internal(sub(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]]))");
    }

    @Test
    public void testsub16() {
        assertEval("argv <- list('^ +', '', c('1_', 'Weight', 'Cylinders4', 'Cylinders5', 'Cylinders6', 'Cylinders8', 'Cylindersrotary', 'TypeLarge', 'TypeMidsize', 'TypeSmall', 'TypeSporty', 'TypeVan', 'EngineSize', 'DriveTrainFront', 'DriveTrainRear'), FALSE, FALSE, FALSE, FALSE); .Internal(sub(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]]))");
    }

    @Test
    public void testsub17() {
        assertEval("argv <- list('^msgstr[[:blank:]]+[\\\'](.*)[\\\'][[:blank:]]*$', '\\\\1', 'msgstr \\\'<U+043E><U+0442><U+0440><U+0438><U+0446><U+0430><U+0442><U+0435><U+043B><U+044C><U+043D><U+044B><U+0435> <U+0432><U+0435><U+0441><U+0430> <U+043D><U+0435> <U+0440><U+0430><U+0437><U+0440><U+0435><U+0448><U+0435><U+043D><U+044B>\\\'', FALSE, FALSE, FALSE, FALSE); .Internal(sub(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]]))");
    }

    @Test
    public void testsub18() {
        assertEval("argv <- list('.*Content-Type:[^\\\\]*charset=([^\\\\[:space:]]*)[[:space:]]*\\\\\\\\n.*', '\\\\1', 'Project-Id-Version: lattice 0.20-10\\\\nReport-Msgid-Bugs-To: bugs@r-project.org\\\\nPOT-Creation-Date: 2012-03-10 14:42\\\\nPO-Revision-Date: 2012-08-31 16:36+0100\\\\nLast-Translator: \\305\\201ukasz Daniel <lukasz.daniel@gmail.com>\\\\nLanguage-Team: \\305\\201ukasz Daniel <lukasz.daniel@gmail.com>\\\\nLanguage: pl_PL\\\\nMIME-Version: 1.0\\\\nContent-Type: text/plain; charset=UTF-8\\\\nContent-Transfer-Encoding: 8bit\\\\nPlural-Forms: nplurals=3; plural=(n==1 ? 0 : n%10>=2 && n%10<=4 && (n%100<10 || n%100>=20) ? 1 : 2)\\\\nX-Poedit-SourceCharset: iso-8859-1\\\\n', FALSE, FALSE, FALSE, FALSE); .Internal(sub(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]]))");
    }

    @Test
    public void testsub19() {
        assertEval("argv <- list('([^:]*):(.*)', '\\\\2', character(0), FALSE, FALSE, FALSE, FALSE); .Internal(sub(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]]))");
    }

    @Test
    public void testSub() {
        assertEval("{ sub(\"a\",\"aa\", \"prague alley\") }");
        assertEval("{ sub(\"a\",\"aa\", \"prague alley\", fixed=TRUE) }");

        assertEval("{ sub('[[:space:]]+$', '', 'R (>= 3.0.3)  ') }");
        assertEval("{ sub('^[[:space:]]*(.*)', '\\\\1', 'R (>= 3.0.3)') }");
        assertEval("{ sub('^([[:alnum:].]+).*$', '\\\\1', 'R (>= 3.0.3)') }");

        assertEval("{ sub('^([1[:alpha:].]+).*$', '\\\\1', '1R.ff (>= 3.0.3)') }");

        assertEval("{ sub('^([[:alnum:]]*).*$', '\\\\1', 'aZ45j%$  ') }");
        assertEval("{ sub('^([[:alpha:]]*).*$', '\\\\1', 'aZ45j%$  ') }");
        assertEval("{ sub('^([[:blank:]]*).*$', '\\\\1', '  \\ta45j%$  ') }");
        assertEval("{ sub('^([[:cntrl:]]*).*$', '\\\\1', '\\ta45j%$  ') }");
        assertEval("{ sub('^([[:digit:]]*).*$', '\\\\1', '12a45j%$  ') }");
        assertEval("{ sub('^([[:graph:]]*).*$', '\\\\1', 'a45j%$  ') }");
        assertEval("{ sub('^([[:lower:]]*).*$', '\\\\1', 'a45j%$  ') }");
        assertEval("{ sub('^([[:print:]]*).*$', '\\\\1', 'a45j%$  ') }");
        assertEval("{ sub('^([[:punct:]]*).*$', '\\\\1', '.,/a45j%$  ') }");
        assertEval("{ sub('^([[:space:]]*).*$', '\\\\1', '   a45j%$  ') }");
        assertEval("{ sub('^([[:upper:]]*).*$', '\\\\1', 'AASDFAa45j%$  ') }");
        assertEval("{ sub('^([[:xdigit:]]*).*$', '\\\\1', '1234abABhxa45j%$  ') }");

        assertEval("{ .Internal(sub(7, \"42\", \"7\", F, F, F, F)) }");
        assertEval("{ .Internal(sub(character(), \"42\", \"7\", F, F, F, F)) }");
        assertEval("{ .Internal(sub(\"7\", 42, \"7\", F, F, F, F)) }");
        assertEval("{ .Internal(sub(\"7\", character(), \"7\", F, F, F, F)) }");
        assertEval("{ .Internal(sub(\"7\", \"42\", 7, F, F, F, F)) }");

        assertEval("{ sub('\\\\s*$', '', 'Ä', perl=TRUE) }");

        assertEval("{ sub(pattern = 'a*', replacement = 'x', x = 'ÄaÄ', perl = TRUE) }");
        assertEval("{ sub(pattern = 'a*', replacement = 'x', x = 'ÄaaaaÄ', perl = TRUE) }");

        // Expected output: [1] "xaÄÄÄÄÄb"
        // FastR output: [1] "axÄÄÄÄb"
        assertEval(Ignored.Unknown, "{ sub(pattern = 'Ä*', replacement = 'x', x = 'aÄÄÄÄÄb', perl = TRUE) }");
    }
}
