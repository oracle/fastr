pkgname <- "utils"
source(file.path(R.home("share"), "R", "examples-header.R"))
options(warn = 1)
library('utils')

base::assign(".oldSearch", base::search(), pos = 'CheckExEnv')
cleanEx()
nameEx("LINK")
### * LINK

flush(stderr()); flush(stdout())

### Name: LINK
### Title: Create Executable Programs
### Aliases: LINK
### Keywords: utilities

### ** Examples
## Not run: 
##D ## examples of front-ends linked against R.
##D ## First a C program
##D CC=`R CMD config CC`
##D R CMD LINK $CC -o foo foo.o `R CMD config --ldflags`
##D 
##D ## if Fortran code has been compiled into ForFoo.o
##D FLIBS=`R CMD config FLIBS`
##D R CMD LINK $CC -o foo foo.o ForFoo.o `R CMD config --ldflags` $FLIBS
##D 
##D ## And for a C++ front-end
##D CXX=`R CMD config CXX`
##D R CMD COMPILE foo.cc
##D R CMD LINK $CXX -o foo foo.o `R CMD config --ldflags`
## End(Not run)


cleanEx()
nameEx("Question")
### * Question

flush(stderr()); flush(stdout())

### Name: Question
### Title: Documentation Shortcuts
### Aliases: Question ?
### Keywords: documentation

### ** Examples

?lapply

?"for"                  # but quotes/backticks are needed
?`+`

?women                  # information about data set "women"

## Not run: 
##D require(methods)
##D ## define a S4 generic function and some methods
##D combo <- function(x, y) c(x, y)
##D setGeneric("combo")
##D setMethod("combo", c("numeric", "numeric"), function(x, y) x+y)
##D 
##D ## assume we have written some documentation
##D ## for combo, and its methods ....
##D 
##D ?combo  # produces the function documentation
##D 
##D methods?combo  # looks for the overall methods documentation
##D 
##D method?combo("numeric", "numeric")  # documentation for the method above
##D 
##D ?combo(1:10, rnorm(10))  # ... the same method, selected according to
##D                          # the arguments (one integer, the other numeric)
##D 
##D ?combo(1:10, letters)    # documentation for the default method
## End(Not run)


cleanEx()
nameEx("RShowDoc")
### * RShowDoc

flush(stderr()); flush(stdout())

### Name: RShowDoc
### Title: Show R Manuals and Other Documentation
### Aliases: RShowDoc
### Keywords: documentation

### ** Examples


cleanEx()
nameEx("RSiteSearch")
### * RSiteSearch

flush(stderr()); flush(stdout())

### Name: RSiteSearch
### Title: Search for Key Words or Phrases in Documentation
### Aliases: RSiteSearch
### Keywords: utilities documentation

### ** Examples


cleanEx()
nameEx("Rprof")
### * Rprof

flush(stderr()); flush(stdout())

### Name: Rprof
### Title: Enable Profiling of R's Execution
### Aliases: Rprof
### Keywords: utilities

### ** Examples

## Not run: 
##D Rprof()
##D ## some code to be profiled
##D Rprof(NULL)
##D ## some code NOT to be profiled
##D Rprof(append = TRUE)
##D ## some code to be profiled
##D Rprof(NULL)
##D ...
##D ## Now post-process the output as described in Details
## End(Not run)


cleanEx()
nameEx("Rprofmem")
### * Rprofmem

flush(stderr()); flush(stdout())

### Name: Rprofmem
### Title: Enable Profiling of R's Memory Use
### Aliases: Rprofmem
### Keywords: utilities

### ** Examples
## Not run: 
##D ## not supported unless R is compiled to support it.
##D Rprofmem("Rprofmem.out", threshold = 1000)
##D example(glm)
##D Rprofmem(NULL)
##D noquote(readLines("Rprofmem.out", n = 5))
## End(Not run)


cleanEx()
nameEx("Rscript")
### * Rscript

flush(stderr()); flush(stdout())

### Name: Rscript
### Title: Scripting Front-End for R
### Aliases: Rscript
### Keywords: utilities

### ** Examples
## Not run: 
##D Rscript -e 'date()' -e 'format(Sys.time(), "%a %b %d %X %Y")'
##D 
##D # Get the same initial packages in the same order as default R:
##D Rscript --default-packages=methods,datasets,utils,grDevices,graphics,stats -e 'sessionInfo()'
##D 
##D ## example #! script for a Unix-alike
##D 
##D #! /path/to/Rscript --vanilla --default-packages=utils
##D args <- commandArgs(TRUE)
##D res <- try(install.packages(args))
##D if(inherits(res, "try-error")) q(status=1) else q()
##D 
## End(Not run)


cleanEx()
nameEx("Rtangle")
### * Rtangle

flush(stderr()); flush(stdout())

### Name: Rtangle
### Title: R Driver for Stangle
### Aliases: Rtangle RtangleSetup
### Keywords: utilities

### ** Examples

nmRnw <- "example-1.Rnw"
exfile <- system.file("Sweave", nmRnw, package = "utils")
## Create R source file
Stangle(exfile)
nmR <- sub("Rnw$", "R", nmRnw) # the (default) R output file name
if(interactive()) file.show("example-1.R")
## Don't show: 
file.rename("example-1.R", "example-1_def.R")
## End(Don't show)
## Smaller R source file with custom annotation:
my.Ann <- function(options, chunk, output) {
  cat("### chunk #", options$chunknr, ": ",
      if(!is.null(ol <- options$label)) ol else .RtangleCodeLabel(chunk),
      if(!options$eval) " (eval = FALSE)", "\n",
      file = output, sep = "")
}
Stangle(exfile, annotate = my.Ann)
if(interactive()) file.show("example-1.R")
## Don't show: 
file.rename("example-1.R", "example-1_myA.R")
## End(Don't show)
Stangle(exfile, annotate = my.Ann, drop.evalFALSE=TRUE)
if(interactive()) file.show("example-1.R")
## Don't show: 
file.rename("example-1.R", "example-1_myA-noF.R")
## End(Don't show)



cleanEx()
nameEx("SHLIB")
### * SHLIB

flush(stderr()); flush(stdout())

### Name: SHLIB
### Title: Build Shared Object/DLL for Dynamic Loading
### Aliases: SHLIB
### Keywords: utilities

### ** Examples
## Not run: 
##D # To link against a library not on the system library paths:
##D R CMD SHLIB -o mylib.so a.f b.f -L/opt/acml3.5.0/gnu64/lib -lacml
## End(Not run)


cleanEx()
nameEx("Sweave")
### * Sweave

flush(stderr()); flush(stdout())

### Name: Sweave
### Title: Automatic Generation of Reports
### Aliases: Sweave Stangle SweaveSyntaxLatex SweaveSyntaxNoweb
### Keywords: utilities

### ** Examples

testfile <- system.file("Sweave", "Sweave-test-1.Rnw", package = "utils")

## enforce par(ask = FALSE)
options(device.ask.default = FALSE)

## create a LaTeX file
Sweave(testfile)

## This can be compiled to PDF by
## tools::texi2pdf("Sweave-test-1.tex")
## or outside R by
## R CMD texi2pdf Sweave-test-1.tex
## which sets the appropriate TEXINPUTS path.
## create an R source file from the code chunks
Stangle(testfile)
## which can be sourced, e.g.
source("Sweave-test-1.R")

## Don't show: 
if(!interactive()) unlink("Sweave-test-1*")
## End(Don't show)



cleanEx()
nameEx("SweaveSyntConv")
### * SweaveSyntConv

flush(stderr()); flush(stdout())

### Name: SweaveSyntConv
### Title: Convert Sweave Syntax
### Aliases: SweaveSyntConv
### Keywords: utilities

### ** Examples

testfile <- system.file("Sweave", "Sweave-test-1.Rnw", package = "utils")

## convert the file to latex syntax
SweaveSyntConv(testfile, SweaveSyntaxLatex)

## and run it through Sweave
Sweave("Sweave-test-1.Stex")

## Don't show: 
if(!interactive()) unlink("Sweave-test-1*")
## End(Don't show)



cleanEx()
nameEx("URLencode")
### * URLencode

flush(stderr()); flush(stdout())

### Name: URLencode
### Title: Encode or Decode a (partial) URL
### Aliases: URLencode URLdecode
### Keywords: utilities

### ** Examples

(y <- URLencode("a url with spaces and / and @"))
URLdecode(y)
(y <- URLencode("a url with spaces and / and @", reserved = TRUE))
URLdecode(y)

URLdecode(z <- "ab%20cd")
c(URLencode(z), URLencode(z, repeated = TRUE)) # first is usually wanted



cleanEx()
nameEx("adist")
### * adist

flush(stderr()); flush(stdout())

### Name: adist
### Title: Approximate String Distances
### Aliases: adist
### Keywords: character

### ** Examples

## Cf. https://en.wikipedia.org/wiki/Levenshtein_distance
adist("kitten", "sitting")
## To see the transformation counts for the Levenshtein distance:
drop(attr(adist("kitten", "sitting", counts = TRUE), "counts"))
## To see the transformation sequences:
attr(adist(c("kitten", "sitting"), counts = TRUE), "trafos")

## Cf. the examples for agrep:
adist("lasy", "1 lazy 2")
## For a "partial approximate match" (as used for agrep):
adist("lasy", "1 lazy 2", partial = TRUE)



cleanEx()
nameEx("alarm")
### * alarm

flush(stderr()); flush(stdout())

### Name: alarm
### Title: Alert the User
### Aliases: alarm
### Keywords: utilities

### ** Examples

alarm()



cleanEx()
nameEx("apropos")
### * apropos

flush(stderr()); flush(stdout())

### Name: apropos
### Title: Find Objects by (Partial) Name
### Aliases: apropos find
### Keywords: data documentation environment

### ** Examples

require(stats)

## Not run: apropos("lm")
apropos("GLM")                      # several
apropos("GLM", ignore.case = FALSE) # not one
apropos("lq")

cor <- 1:pi
find("cor")                         #> ".GlobalEnv"   "package:stats"
find("cor", numeric = TRUE)                     # numbers with these names
find("cor", numeric = TRUE, mode = "function")  # only the second one
rm(cor)

## Not run: apropos(".", mode="list")  # a long list

# need a DOUBLE backslash '\\' (in case you don't see it anymore)
apropos("\\[")



cleanEx()
nameEx("aregexec")
### * aregexec

flush(stderr()); flush(stdout())

### Name: aregexec
### Title: Approximate String Match Positions
### Aliases: aregexec
### Keywords: character

### ** Examples

## Cf. the examples for agrep.
x <- c("1 lazy", "1", "1 LAZY")
aregexec("laysy", x, max.distance = 2)
aregexec("(lay)(sy)", x, max.distance = 2)
aregexec("(lay)(sy)", x, max.distance = 2, ignore.case = TRUE)
m <- aregexec("(lay)(sy)", x, max.distance = 2)
regmatches(x, m)



cleanEx()
nameEx("aspell")
### * aspell

flush(stderr()); flush(stdout())

### Name: aspell
### Title: Spell Check Interface
### Aliases: aspell
### Keywords: utilities

### ** Examples

## Not run: 
##D ## To check all Rd files in a directory, (additionally) skipping the
##D ## \references sections.
##D files <- Sys.glob("*.Rd")
##D aspell(files, filter = list("Rd", drop = "\\references"))
##D 
##D ## To check all Sweave files
##D files <- Sys.glob(c("*.Rnw", "*.Snw", "*.rnw", "*.snw"))
##D aspell(files, filter = "Sweave", control = "-t")
##D 
##D ## To check all Texinfo files (Aspell only)
##D files <- Sys.glob("*.texi")
##D aspell(files, control = "--mode=texinfo")
## End(Not run)

## List the available R system dictionaries.
Sys.glob(file.path(R.home("share"), "dictionaries", "*.rds"))



cleanEx()
nameEx("available.packages")
### * available.packages

flush(stderr()); flush(stdout())

### Name: available.packages
### Title: List Available Packages at CRAN-like Repositories
### Aliases: available.packages
### Keywords: utilities

### ** Examples
## Not run: 
##D ## Restrict install.packages() (etc) to known-to-be-FOSS packages
##D options(available_packages_filters =
##D   c("R_version", "OS_type", "subarch", "duplicates", "license/FOSS"))
##D ## or
##D options(available_packages_filters = list(add = TRUE, "license/FOSS"))
##D 
##D ## Give priority to released versions on CRAN, rather than development
##D ## versions on Omegahat, R-Forge etc.
##D options(available_packages_filters =
##D      c("R_version", "OS_type", "subarch", "CRAN", "duplicates"))
## End(Not run)


cleanEx()
nameEx("bibentry")
### * bibentry

flush(stderr()); flush(stdout())

### Name: bibentry
### Title: Bibliography Entries
### Aliases: bibentry print.bibentry format.bibentry sort.bibentry
###   print.citation format.citation
### Keywords: utilities documentation

### ** Examples

## R reference
rref <- bibentry(
   bibtype = "Manual",
   title = "R: A Language and Environment for Statistical Computing",
   author = person("R Core Team"),
   organization = "R Foundation for Statistical Computing",
   address = "Vienna, Austria",
   year = 2014,
   url = "https://www.R-project.org/")

## Different printing styles
print(rref)
print(rref, style = "Bibtex")
print(rref, style = "citation")
print(rref, style = "html")
print(rref, style = "latex")
print(rref, style = "R")

## References for boot package and associated book
bref <- c(
   bibentry(
     bibtype = "Manual",
     title = "boot: Bootstrap R (S-PLUS) Functions",
     author = c(
       person("Angelo", "Canty", role = "aut",
         comment = "S original"),
       person(c("Brian", "D."), "Ripley", role = c("aut", "trl", "cre"),
         comment = "R port, author of parallel support",
         email = "ripley@stats.ox.ac.uk")
     ),
     year = "2012",
     note = "R package version 1.3-4",
     url = "https://CRAN.R-project.org/package=boot",
     key = "boot-package"
   ),

   bibentry(
     bibtype = "Book",
     title = "Bootstrap Methods and Their Applications",
     author = as.person("Anthony C. Davison [aut], David V. Hinkley [aut]"),
     year = "1997",
     publisher = "Cambridge University Press",
     address = "Cambridge",
     isbn = "0-521-57391-2",
     url = "http://statwww.epfl.ch/davison/BMA/",
     key = "boot-book"
   )
)

## Combining and subsetting
c(rref, bref)
bref[2]
bref["boot-book"]

## Extracting fields
bref$author
bref[1]$author
bref[1]$author[2]$email

## Convert to BibTeX
toBibtex(bref)

## Format in R style
## One bibentry() call for each bibentry:
writeLines(paste(format(bref, "R"), collapse = "\n\n"))
## One collapsed call:
writeLines(format(bref, "R", collapse = TRUE))



cleanEx()
nameEx("browseEnv")
### * browseEnv

flush(stderr()); flush(stdout())

### Name: browseEnv
### Title: Browse Objects in Environment
### Aliases: browseEnv wsbrowser
### Keywords: interface

### ** Examples

if(interactive()) {
   ## create some interesting objects :
   ofa <- ordered(4:1)
   ex1 <- expression(1+ 0:9)
   ex3 <- expression(u, v, 1+ 0:9)
   example(factor, echo = FALSE)
   example(table, echo = FALSE)
   example(ftable, echo = FALSE)
   example(lm, echo = FALSE, ask = FALSE)
   example(str, echo = FALSE)

   ## and browse them:
   browseEnv()

   ## a (simple) function's environment:
   af12 <- approxfun(1:2, 1:2, method = "const")
   browseEnv(envir = environment(af12))
 }



cleanEx()
nameEx("browseURL")
### * browseURL

flush(stderr()); flush(stdout())

### Name: browseURL
### Title: Load URL into an HTML Browser
### Aliases: browseURL
### Keywords: file

### ** Examples

## Not run: 
##D ## for KDE users who want to open files in a new tab
##D options(browser = "kfmclient newTab")
##D browseURL("https://www.r-project.org")
## End(Not run)


cleanEx()
nameEx("browseVignettes")
### * browseVignettes

flush(stderr()); flush(stdout())

### Name: browseVignettes
### Title: List Vignettes in an HTML Browser
### Aliases: browseVignettes print.browseVignettes
### Keywords: documentation

### ** Examples


cleanEx()
nameEx("capture.output")
### * capture.output

flush(stderr()); flush(stdout())

### Name: capture.output
### Title: Send Output to a Character String or File
### Aliases: capture.output
### Keywords: utilities

### ** Examples

require(stats)
glmout <- capture.output(summary(glm(case ~ spontaneous+induced,
                                     data = infert, family = binomial())))
glmout[1:5]
capture.output(1+1, 2+2)
capture.output({1+1; 2+2})

## Not run: 
##D ## on Unix-alike with a2ps available##D 
##D op <- options(useFancyQuotes=FALSE)
##D pdf <- pipe("a2ps -o - | ps2pdf - tempout.pdf", "w")
##D capture.output(example(glm), file = pdf)
##D close(pdf); options(op) ; system("evince tempout.pdf &")
## End(Not run)



cleanEx()
nameEx("changedFiles")
### * changedFiles

flush(stderr()); flush(stdout())

### Name: changedFiles
### Title: Detect which files have changed
### Aliases: fileSnapshot changedFiles print.changedFiles
###   print.fileSnapshot
### Keywords: utilities file

### ** Examples

# Create some files in a temporary directory
dir <- tempfile()
dir.create(dir)
writeBin(1L, file.path(dir, "file1"))
writeBin(2L, file.path(dir, "file2"))
dir.create(file.path(dir, "dir"))

# Take a snapshot
snapshot <- fileSnapshot(dir, timestamp = tempfile("timestamp"), md5sum=TRUE)

# Change one of the files.
writeBin(3L:4L, file.path(dir, "file2"))

# Display the detected changes.  We may or may not see mtime change...
changedFiles(snapshot)
changedFiles(snapshot)$changes



cleanEx()
nameEx("citation")
### * citation

flush(stderr()); flush(stdout())

### Name: citation
### Title: Citing R and R Packages in Publications
### Aliases: CITATION citation readCitationFile
### Keywords: misc

### ** Examples

## the basic R reference
citation()

## references for a package -- might not have these installed
if(nchar(system.file(package = "lattice"))) citation("lattice")
if(nchar(system.file(package = "foreign"))) citation("foreign")

## extract the bibtex entry from the return value
x <- citation()
toBibtex(x)




cleanEx()
nameEx("cite")
### * cite

flush(stderr()); flush(stdout())

### Name: cite
### Title: Cite a bibliography entry.
### Aliases: cite citeNatbib
### Keywords: utilities documentation

### ** Examples

## R reference
rref <- bibentry(
   bibtype = "Manual",
   title = "R: A Language and Environment for Statistical Computing",
   author = person("R Core Team"),
   organization = "R Foundation for Statistical Computing",
   address = "Vienna, Austria",
   year = 2013,
   url = "https://www.R-project.org/",
   key = "R")

## References for boot package and associated book
bref <- c(
   bibentry(
     bibtype = "Manual",
     title = "boot: Bootstrap R (S-PLUS) Functions",
     author = c(
       person("Angelo", "Canty", role = "aut",
         comment = "S original"),
       person(c("Brian", "D."), "Ripley", role = c("aut", "trl", "cre"),
         comment = "R port, author of parallel support",
         email = "ripley@stats.ox.ac.uk")
     ),
     year = "2012",
     note = "R package version 1.3-4",
     url = "https://CRAN.R-project.org/package=boot",
     key = "boot-package"
   ),

   bibentry(
     bibtype = "Book",
     title = "Bootstrap Methods and Their Applications",
     author = as.person("Anthony C. Davison [aut], David V. Hinkley [aut]"),
     year = "1997",
     publisher = "Cambridge University Press",
     address = "Cambridge",
     isbn = "0-521-57391-2",
     url = "http://statwww.epfl.ch/davison/BMA/",
     key = "boot-book"
   )
)

## Combine and cite
refs <- c(rref, bref)
cite("R, boot-package", refs)

## Cite numerically
savestyle <- tools::getBibstyle()
tools::bibstyle("JSSnumbered", .init = TRUE,
         fmtPrefix = function(paper) paste0("[", paper$.index, "]"),
         cite = function(key, bib, ...)
         	citeNatbib(key, bib, mode = "numbers",
         	    bibpunct = c("[", "]", ";", "n", "", ","), ...)
         )
cite("R, boot-package", refs, textual = TRUE)
refs

## restore the old style
tools::bibstyle(savestyle, .default = TRUE)



cleanEx()
nameEx("combn")
### * combn

flush(stderr()); flush(stdout())

### Name: combn
### Title: Generate All Combinations of n Elements, Taken m at a Time
### Aliases: combn
### Keywords: utilities iteration

### ** Examples

combn(letters[1:4], 2)
(m <- combn(10, 5, min))   # minimum value in each combination
mm <- combn(15, 6, function(x) matrix(x, 2, 3))
stopifnot(round(choose(10, 5)) == length(m),
          c(2,3, round(choose(15, 6))) == dim(mm))

## Different way of encoding points:
combn(c(1,1,1,1,2,2,2,3,3,4), 3, tabulate, nbins = 4)

## Compute support points and (scaled) probabilities for a
## Multivariate-Hypergeometric(n = 3, N = c(4,3,2,1)) p.f.:
# table.mat(t(combn(c(1,1,1,1,2,2,2,3,3,4), 3, tabulate, nbins = 4)))

## Assuring the identity
for(n in 1:7)
 for(m in 0:n) stopifnot(is.array(cc <- combn(n, m)),
                         dim(cc) == c(m, choose(n, m)))



cleanEx()
nameEx("compareVersion")
### * compareVersion

flush(stderr()); flush(stdout())

### Name: compareVersion
### Title: Compare Two Package Version Numbers
### Aliases: compareVersion
### Keywords: utilities

### ** Examples

compareVersion("1.0", "1.0-1")
compareVersion("7.2-0","7.1-12")



cleanEx()
nameEx("count.fields")
### * count.fields

flush(stderr()); flush(stdout())

### Name: count.fields
### Title: Count the Number of Fields per Line
### Aliases: count.fields
### Keywords: file

### ** Examples

cat("NAME", "1:John", "2:Paul", file = "foo", sep = "\n")
count.fields("foo", sep = ":")
unlink("foo")



cleanEx()
nameEx("data")
### * data

flush(stderr()); flush(stdout())

### Name: data
### Title: Data Sets
### Aliases: data print.packageIQR
### Keywords: documentation datasets

### ** Examples

require(utils)
data()                         # list all available data sets
try(data(package = "rpart") )  # list the data sets in the rpart package
data(USArrests, "VADeaths")    # load the data sets 'USArrests' and 'VADeaths'
## Not run: 
##D ## Alternatively
##D ds <- c("USArrests", "VADeaths"); data(list = ds)
## End(Not run)
help(USArrests)                # give information on data set 'USArrests'



cleanEx()
nameEx("dataentry")
### * dataentry

flush(stderr()); flush(stdout())

### Name: dataentry
### Title: Spreadsheet Interface for Entering Data
### Aliases: data.entry dataentry de de.ncols de.restore de.setup
### Keywords: utilities file

### ** Examples

# call data entry with variables x and y
## Not run: data.entry(x, y)



cleanEx()
nameEx("debugcall")
### * debugcall

flush(stderr()); flush(stdout())

### Name: debugcall
### Title: Debug a Call
### Aliases: debugcall undebugcall
### Keywords: programming environment utilities

### ** Examples

## Not run: 
##D ## Evaluate call after setting debugging
##D ## 
##D f <- factor(1:10)
##D res <- eval(debugcall(summary(f))) 
## End(Not run)



cleanEx()
nameEx("debugger")
### * debugger

flush(stderr()); flush(stdout())

### Name: debugger
### Title: Post-Mortem Debugging
### Aliases: debugger dump.frames
### Keywords: utilities error

### ** Examples

## Not run: 
##D options(error = quote(dump.frames("testdump", TRUE)))
##D 
##D f <- function() {
##D     g <- function() stop("test dump.frames")
##D     g()
##D }
##D f()   # will generate a dump on file "testdump.rda"
##D options(error = NULL)
##D 
##D ## possibly in another R session
##D load("testdump.rda")
##D debugger(testdump)
##D Available environments had calls:
##D 1: f()
##D 2: g()
##D 3: stop("test dump.frames")
##D 
##D Enter an environment number, or 0 to exit
##D Selection: 1
##D Browsing in the environment with call:
##D f()
##D Called from: debugger.look(ind)
##D Browse[1]> ls()
##D [1] "g"
##D Browse[1]> g
##D function() stop("test dump.frames")
##D <environment: 759818>
##D Browse[1]>
##D Available environments had calls:
##D 1: f()
##D 2: g()
##D 3: stop("test dump.frames")
##D 
##D Enter an environment number, or 0 to exit
##D Selection: 0
##D 
##D ## A possible setting for non-interactive sessions
##D options(error = quote({dump.frames(to.file = TRUE); q(status = 1)}))
## End(Not run)


cleanEx()
nameEx("demo")
### * demo

flush(stderr()); flush(stdout())

### Name: demo
### Title: Demonstrations of R Functionality
### Aliases: demo
### Keywords: documentation utilities

### ** Examples

demo() # for attached packages

## All available demos:
demo(package = .packages(all.available = TRUE))


## Not run: 
##D  ch <- "scoping"
##D  demo(ch, character = TRUE)
## End(Not run)

## Find the location of a demo
system.file("demo", "lm.glm.R", package = "stats")



cleanEx()
nameEx("edit")
### * edit

flush(stderr()); flush(stdout())

### Name: edit
### Title: Invoke a Text Editor
### Aliases: edit edit.default vi emacs pico xemacs xedit
### Keywords: utilities

### ** Examples

## Not run: 
##D # use xedit on the function mean and assign the changes
##D mean <- edit(mean, editor = "xedit")
##D 
##D # use vi on mean and write the result to file mean.out
##D vi(mean, file = "mean.out")
## End(Not run)



cleanEx()
nameEx("edit.data.frame")
### * edit.data.frame

flush(stderr()); flush(stdout())

### Name: edit.data.frame
### Title: Edit Data Frames and Matrices
### Aliases: edit.data.frame edit.matrix
### Keywords: utilities

### ** Examples

## Not run: 
##D edit(InsectSprays)
##D edit(InsectSprays, factor.mode = "numeric")
## End(Not run)



cleanEx()
nameEx("example")
### * example

flush(stderr()); flush(stdout())

### Name: example
### Title: Run an Examples Section from the Online Help
### Aliases: example
### Keywords: documentation utilities

### ** Examples

example(InsectSprays)
## force use of the standard package 'stats':
example("smooth", package = "stats", lib.loc = .Library)

## set RNG *before* example as when R CMD check is run:

r1 <- example(quantile, setRNG = TRUE)
x1 <- rnorm(1)
u <- runif(1)
## identical random numbers
r2 <- example(quantile, setRNG = TRUE)
x2 <- rnorm(1)
stopifnot(identical(r1, r2))
## but x1 and x2 differ since the RNG state from before example()
## differs and is restored!
x1; x2

## Exploring examples code:
## How large are the examples of "lm...()" functions?
lmex <- sapply(apropos("^lm", mode = "function"),
               example, character.only = TRUE, give.lines = TRUE)
sapply(lmex, length)



cleanEx()
nameEx("file.edit")
### * file.edit

flush(stderr()); flush(stdout())

### Name: file.edit
### Title: Edit One or More Files
### Aliases: file.edit
### Keywords: utilities

### ** Examples

## Not run: 
##D # open two R scripts for editing
##D file.edit("script1.R", "script2.R")
## End(Not run)



cleanEx()
nameEx("filetest")
### * filetest

flush(stderr()); flush(stdout())

### Name: file_test
### Title: Shell-style Tests on Files
### Aliases: file_test
### Keywords: file

### ** Examples

dir <- file.path(R.home(), "library", "stats")
file_test("-d", dir)
file_test("-nt", file.path(dir, "R"), file.path(dir, "demo"))



cleanEx()
nameEx("findLineNum")
### * findLineNum

flush(stderr()); flush(stdout())

### Name: findLineNum
### Title: Find the Location of a Line of Source Code, or Set a Breakpoint
###   There.
### Aliases: findLineNum setBreakpoint
### Keywords: debugging

### ** Examples

## Not run: 
##D # Find what function was defined in the file mysource.R at line 100:
##D findLineNum("mysource.R#100")
##D 
##D # Set a breakpoint in both copies of that function, assuming one is in the
##D # same namespace as myfunction and the other is on the search path
##D setBreakpoint("mysource.R#100", envir = myfunction)
## End(Not run)



cleanEx()
nameEx("fix")
### * fix

flush(stderr()); flush(stdout())

### Name: fix
### Title: Fix an Object
### Aliases: fix
### Keywords: utilities

### ** Examples

## Not run: 
##D  ## Assume 'my.fun' is a user defined function :
##D  fix(my.fun)
##D  ## now my.fun is changed
##D  ## Also,
##D  fix(my.data.frame) # calls up data editor
##D  fix(my.data.frame, factor.mode="char") # use of ...
## End(Not run)


cleanEx()
nameEx("format")
### * format

flush(stderr()); flush(stdout())

### Name: format
### Title: Format Unordered and Ordered Lists
### Aliases: formatUL formatOL
### Keywords: print

### ** Examples

## A simpler recipe.
x <- c("Mix dry ingredients thoroughly.",
       "Pour in wet ingredients.",
       "Mix for 10 minutes.",
       "Bake for one hour at 300 degrees.")
## Format and output as an unordered list.
writeLines(formatUL(x))
## Format and output as an ordered list.
writeLines(formatOL(x))
## Ordered list using lower case roman numerals.
writeLines(formatOL(x, type = "i"))
## Ordered list using upper case letters and some offset.
writeLines(formatOL(x, type = "A", offset = 5))



cleanEx()
nameEx("getAnywhere")
### * getAnywhere

flush(stderr()); flush(stdout())

### Name: getAnywhere
### Title: Retrieve an R Object, Including from a Namespace
### Aliases: getAnywhere argsAnywhere [.getAnywhere print.getAnywhere
### Keywords: data

### ** Examples

getAnywhere("format.dist")
getAnywhere("simpleLoess") # not exported from stats
argsAnywhere(format.dist)



cleanEx()
nameEx("getFromNamespace")
### * getFromNamespace

flush(stderr()); flush(stdout())

### Name: getFromNamespace
### Title: Utility functions for Developing Namespaces
### Aliases: assignInNamespace assignInMyNamespace getFromNamespace
###   fixInNamespace
### Keywords: data

### ** Examples

getFromNamespace("findGeneric", "utils")
## Not run: 
##D fixInNamespace("predict.ppr", "stats")
##D stats:::predict.ppr
##D getS3method("predict", "ppr")
##D ## alternatively
##D fixInNamespace("predict.ppr", pos = 3)
##D fixInNamespace("predict.ppr", pos = "package:stats")
## End(Not run)


cleanEx()
nameEx("getParseData")
### * getParseData

flush(stderr()); flush(stdout())

### Name: getParseData
### Title: Get detailed parse information from object.
### Aliases: getParseData getParseText
### Keywords: utilities

### ** Examples

fn <- function(x) {
  x + 1 # A comment, kept as part of the source
}

d <- getParseData(fn)
if (!is.null(d)) {
  plus <- which(d$token == "'+'")
  sum <- d$parent[plus]
  print(d[as.character(sum),])
  print(getParseText(d, sum))
}



cleanEx()
nameEx("getS3method")
### * getS3method

flush(stderr()); flush(stdout())

### Name: getS3method
### Title: Get an S3 Method
### Aliases: getS3method
### Keywords: data methods

### ** Examples

require(stats)
exists("predict.ppr") # false
getS3method("predict", "ppr")



cleanEx()
nameEx("glob2rx")
### * glob2rx

flush(stderr()); flush(stdout())

### Name: glob2rx
### Title: Change Wildcard or Globbing Pattern into Regular Expression
### Aliases: glob2rx
### Keywords: file character utilities

### ** Examples

stopifnot(glob2rx("abc.*") == "^abc\\.",
          glob2rx("a?b.*") == "^a.b\\.",
          glob2rx("a?b.*", trim.tail = FALSE) == "^a.b\\..*$",
          glob2rx("*.doc") == "^.*\\.doc$",
          glob2rx("*.doc", trim.head = TRUE) == "\\.doc$",
          glob2rx("*.t*")  == "^.*\\.t",
          glob2rx("*.t??") == "^.*\\.t..$",
          glob2rx("*[*")  == "^.*\\["
)



cleanEx()
nameEx("globalVariables")
### * globalVariables

flush(stderr()); flush(stdout())

### Name: globalVariables
### Title: Declarations Used in Checking a Package
### Aliases: globalVariables suppressForeignCheck
### Keywords: packages

### ** Examples

## Not run: 
##D ## assume your package has some code that assigns ".obj1" and ".obj2"
##D ## but not in a way that codetools can find.
##D ## In the same source file (to remind you that you did it) add:
##D if(getRversion() >= "2.15.1")  utils::globalVariables(c(".obj1", "obj2"))
##D 
##D ## To suppress messages about a run-time calculated native symbol, 
##D ## save it to a local variable.
##D 
##D ## At top level, put this:
##D if(getRversion() >= "3.1.0") utils::suppressForeignCheck("localvariable")
##D 
##D ## Within your function, do the call like this:
##D localvariable <- if (condition) entry1 else entry2
##D .Call(localvariable, 1, 2, 3)
##D 
##D ## HOWEVER, it is much better practice to write code
##D ## that can be checked thoroughly, e.g.
##D if(condition) .Call(entry1, 1, 2, 3) else .Call(entry2, 1, 2, 3)
## End(Not run)



cleanEx()
nameEx("hasName")
### * hasName

flush(stderr()); flush(stdout())

### Name: hasName
### Title: Check for name
### Aliases: hasName
### Keywords: manip logic

### ** Examples

x <- list(abc = 1, def = 2)
!is.null(x$abc) # correct
!is.null(x$a)   # this is the wrong test!
hasName(x, "abc")
hasName(x, "a")



cleanEx()
nameEx("head")
### * head

flush(stderr()); flush(stdout())

### Name: head
### Title: Return the First or Last Part of an Object
### Aliases: head head.default head.data.frame head.function head.ftable
###   head.table head.matrix tail tail.default tail.data.frame
###   tail.function tail.ftable tail.table tail.matrix
### Keywords: manip

### ** Examples

head(letters)
head(letters, n = -6L)

head(freeny.x, n = 10L)
head(freeny.y)

tail(letters)
tail(letters, n = -6L)

tail(freeny.x)
tail(freeny.y)

tail(library)

head(stats::ftable(Titanic))



cleanEx()
nameEx("help")
### * help

flush(stderr()); flush(stdout())

### Name: help
### Title: Documentation
### Aliases: help
### Keywords: documentation

### ** Examples

help()
help(help)              # the same

help(lapply)

help("for")             # or ?"for", but quotes/backticks are needed


topi <- "women"
help(topi)

try(help("bs", try.all.packages = FALSE)) # reports not found (an error)
help("bs", try.all.packages = TRUE)       # reports can be found
                                          # in package 'splines'



cleanEx()
nameEx("help.search")
### * help.search

flush(stderr()); flush(stdout())

### Name: help.search
### Title: Search the Help System
### Aliases: help.search ?? print.hsearch
### Keywords: documentation

### ** Examples

help.search("linear models")    # In case you forgot how to fit linear
                                # models
help.search("non-existent topic")

??utils::help  # All the topics matching "help" in the utils package



cleanEx()
nameEx("help.start")
### * help.start

flush(stderr()); flush(stdout())

### Name: help.start
### Title: Hypertext Documentation
### Aliases: help.start
### Keywords: documentation

### ** Examples

## Not run: 
##D ## the 'remote' arg can be tested by
##D help.start(remote = paste0("file://", R.home()))
## End(Not run)


cleanEx()
nameEx("hsearch-utils")
### * hsearch-utils

flush(stderr()); flush(stdout())

### Name: hsearch-utils
### Title: Help Search Utilities
### Aliases: hsearch_db hsearch_db_concepts hsearch_db_keywords
### Keywords: documentation

### ** Examples

db <- hsearch_db()
## Total numbers of documentation objects, aliases, keywords and
## concepts (using the current format):
sapply(db, NROW)
## Can also be obtained from print method:
db
## 10 most frequent concepts:
head(hsearch_db_concepts(), 10)
## 10 most frequent keywords:
head(hsearch_db_keywords(), 10)



cleanEx()
nameEx("install.packages")
### * install.packages

flush(stderr()); flush(stdout())

### Name: install.packages
### Title: Install Packages from Repositories or Local Files
### Aliases: install.packages
### Keywords: utilities

### ** Examples
## Not run: 
##D ## A Linux example for Fedora's layout of udunits2 headers.
##D install.packages(c("ncdf4", "RNetCDF"),
##D   configure.args = c(RNetCDF = "--with-netcdf-include=/usr/include/udunits2"))
## End(Not run)


cleanEx()
nameEx("installed.packages")
### * installed.packages

flush(stderr()); flush(stdout())

### Name: installed.packages
### Title: Find Installed Packages
### Aliases: installed.packages
### Keywords: utilities

### ** Examples

## confine search to .Library for speed
str(ip <- installed.packages(.Library, priority = "high"))
ip[, c(1,3:5)]
plic <- installed.packages(.Library, priority = "high", fields = "License")
## what licenses are there:
table( plic[, "License"] )



cleanEx()
nameEx("isS3method")
### * isS3method

flush(stderr()); flush(stdout())

### Name: isS3method
### Title: Is 'method' the Name of an S3 Method?
### Aliases: isS3method
### Keywords: methods

### ** Examples

isS3method("t")           # FALSE - it is an S3 generic
isS3method("t.default")   # TRUE
isS3method("t.ts")        # TRUE
isS3method("t.test")      # FALSE
isS3method("t.data.frame")# TRUE
isS3method("t.lm")        # FALSE - not existing
isS3method("t.foo.bar")   # FALSE - not existing

## S3 methods with "4 parts" in their name:
ff <- c("as.list", "as.matrix", "is.na", "row.names", "row.names<-")
for(m in ff) if(isS3method(m)) stop("wrongly declared an S3 method: ", m)
(m4 <- paste(ff, "data.frame", sep="."))
for(m in m4) if(!isS3method(m)) stop("not an S3 method: ", m)
## Don't show: 
stopifnot(
  !isS3method("t"), !isS3method("t.test"), !isS3method("qr.coef"), !isS3method("sort.list"),
  isS3method("t.default"), isS3method("t.ts"), isS3method("t.data.frame"),
  !isS3method("t.lm"), !isS3method("t.foo.bar"))
## End(Don't show)



cleanEx()
nameEx("localeToCharset")
### * localeToCharset

flush(stderr()); flush(stdout())

### Name: localeToCharset
### Title: Select a Suitable Encoding Name from a Locale Name
### Aliases: localeToCharset
### Keywords: utilities

### ** Examples

localeToCharset()



cleanEx()
nameEx("ls_str")
### * ls_str

flush(stderr()); flush(stdout())

### Name: ls.str
### Title: List Objects and their Structure
### Aliases: ls.str lsf.str print.ls_str
### Keywords: print utilities

### ** Examples

require(stats)

lsf.str()  #- how do the functions look like which I am using?
ls.str(mode = "list")   #- what are the structured objects I have defined?

## create a few objects
example(glm, echo = FALSE)
ll <- as.list(LETTERS)
print(ls.str(), max.level = 0)# don't show details

## which base functions have "file" in their name ?
lsf.str(pos = length(search()), pattern = "file")

## demonstrating that  ls.str() works inside functions
## ["browser/debug mode"]:
tt <- function(x, y = 1) { aa <- 7; r <- x + y; ls.str() }
(nms <- sapply(strsplit(capture.output(tt(2))," *: *"), `[`, 1))
stopifnot(nms == c("aa", "r","x","y"))



cleanEx()
nameEx("maintainer")
### * maintainer

flush(stderr()); flush(stdout())

### Name: maintainer
### Title: Show Package Maintainer
### Aliases: maintainer
### Keywords: utilities

### ** Examples

maintainer("MASS")



cleanEx()
nameEx("make.packages.html")
### * make.packages.html

flush(stderr()); flush(stdout())

### Name: make.packages.html
### Title: Update HTML Package List
### Aliases: make.packages.html
### Keywords: utilities

### ** Examples
## Not run: 
##D make.packages.html()
##D # this can be slow for large numbers of installed packages.
## End(Not run)


cleanEx()
nameEx("make.socket")
### * make.socket

flush(stderr()); flush(stdout())

### Name: make.socket
### Title: Create a Socket Connection
### Aliases: make.socket print.socket
### Keywords: misc

### ** Examples

daytime <- function(host = "localhost"){
    a <- make.socket(host, 13)
    on.exit(close.socket(a))
    read.socket(a)
}
## Official time (UTC) from US Naval Observatory
## Not run: daytime("tick.usno.navy.mil")



cleanEx()
nameEx("menu")
### * menu

flush(stderr()); flush(stdout())

### Name: menu
### Title: Menu Interaction Function
### Aliases: menu
### Keywords: utilities programming

### ** Examples

## Not run: 
##D switch(menu(c("List letters", "List LETTERS")) + 1,
##D        cat("Nothing done\n"), letters, LETTERS)
## End(Not run)



cleanEx()
nameEx("methods")
### * methods

flush(stderr()); flush(stdout())

### Name: methods
### Title: List Methods for S3 Generic Functions or Classes
### Aliases: .S3methods methods print.MethodsFunction
### Keywords: methods

### ** Examples

require(stats)

methods(summary)
methods(class = "aov")    # S3 class
## The same, with more details and more difficult to read:
print(methods(class = "aov"), byclass=FALSE)
methods("[[")             # uses C-internal dispatching
methods("$")
methods("$<-")            # replacement function
methods("+")              # binary operator
methods("Math")           # group generic
require(graphics)
methods("axis")           # looks like a generic, but is not

if(require(Matrix)) {
print(methods(class = "Matrix"))  # S4 class
m <- methods("dim")       # S3 and S4 methods
print(m)
print(attr(m, "info"))    # more extensive information

## --> help(showMethods) for related examples
}



cleanEx()
nameEx("modifyList")
### * modifyList

flush(stderr()); flush(stdout())

### Name: modifyList
### Title: Recursively Modify Elements of a List
### Aliases: modifyList
### Keywords: utilities

### ** Examples

foo <- list(a = 1, b = list(c = "a", d = FALSE))
bar <- modifyList(foo, list(e = 2, b = list(d = TRUE)))
str(foo)
str(bar)



cleanEx()
nameEx("news")
### * news

flush(stderr()); flush(stdout())

### Name: news
### Title: Build and Query R or Package News Information
### Aliases: news print.news_db

### ** Examples

## Build a db of all R news entries.
db <- news()
## Don't show: 
  vv <- capture.output(print(db, doBrowse=FALSE))  # without an error
  stopifnot(is.character(vv), length(vv) >= 3) # was wrong (for weeks during devel.)
## End(Don't show)
## Bug fixes with PR number in 3.0.1.
db3 <- news(Version == "3.0.1" & grepl("^BUG", Category) & grepl("PR#", Text),
            db = db)
## Don't show: 
stopifnot( !any(attr(db3,"bad")) && nrow(db3) == 12 )
## End(Don't show)

## News from a date range ('Matrix' is there in a regular R installation):
if(length(iM <- find.package("Matrix", quiet=TRUE)) && nzchar(iM)) {
   dM <- news(package="Matrix")
   stopifnot(identical(dM, news(db=dM)))
   dM2014 <- news("2014-01-01" <= Date & Date <= "2014-12-31", db = dM)
   stopifnot(paste0("1.1-", 2:4) %in% dM2014[,"Version"])
}


cleanEx()
nameEx("nsl")
### * nsl

flush(stderr()); flush(stdout())

### Name: nsl
### Title: Look up the IP Address by Hostname
### Aliases: nsl
### Keywords: utilities

### ** Examples

## Not run: nsl("www.r-project.org")



cleanEx()
nameEx("object.size")
### * object.size

flush(stderr()); flush(stdout())

### Name: object.size
### Title: Report the Space Allocated for an Object
### Aliases: object.size format.object_size print.object_size
### Keywords: utilities

### ** Examples

object.size(letters)
object.size(ls)
format(object.size(library), units = "auto")

sl <- object.size(rep(letters, 1000))

print(sl)                                    ## 209288 bytes
print(sl, units = "auto")                    ## 204.4 Kb
print(sl, units = "auto", standard = "IEC")  ## 204.4 KiB
print(sl, units = "auto", standard = "SI")   ## 209.3 kB

(fsl <- sapply(c("Kb", "KB", "KiB"),
               function(u) format(sl, units = u)))
stopifnot(identical( ## assert that all three are the same :
             unique(substr(as.vector(fsl), 1,5)),
             format(round(as.vector(sl)/1024, 1))))

## find the 10 largest objects in the base package
z <- sapply(ls("package:base"), function(x)
            object.size(get(x, envir = baseenv())))
if(interactive()) {
as.matrix(rev(sort(z))[1:10])
} else # (more constant over time):
    names(rev(sort(z))[1:10])



cleanEx()
nameEx("package.skeleton")
### * package.skeleton

flush(stderr()); flush(stdout())

### Name: package.skeleton
### Title: Create a Skeleton for a New Source Package
### Aliases: package.skeleton
### Keywords: file utilities

### ** Examples

require(stats)
## two functions and two "data sets" :
f <- function(x, y) x+y
g <- function(x, y) x-y
d <- data.frame(a = 1, b = 2)
e <- rnorm(1000)
## Don't show: 
  owd <- getwd()
  setwd(tempdir())
## End(Don't show)
package.skeleton(list = c("f","g","d","e"), name = "mypkg")
## Don't show: 
 setwd(owd) 
## End(Don't show)



cleanEx()
nameEx("packageDescription")
### * packageDescription

flush(stderr()); flush(stdout())

### Name: packageDescription
### Title: Package Description
### Aliases: packageDescription packageVersion print.packageDescription
### Keywords: utilities

### ** Examples


cleanEx()
nameEx("packageName")
### * packageName

flush(stderr()); flush(stdout())

### Name: packageName
### Title: Find package associated with an environment.
### Aliases: packageName
### Keywords: utilities

### ** Examples

packageName()
packageName(environment(mean))



cleanEx()
nameEx("packageStatus")
### * packageStatus

flush(stderr()); flush(stdout())

### Name: packageStatus
### Title: Package Management Tools
### Aliases: packageStatus print.packageStatus summary.packageStatus
###   update.packageStatus upgrade.packageStatus upgrade
### Keywords: utilities

### ** Examples

## Not run: 
##D x <- packageStatus()
##D print(x)
##D summary(x)
##D upgrade(x)
##D x <- update(x)
##D print(x)
## End(Not run)


cleanEx()
nameEx("page")
### * page

flush(stderr()); flush(stdout())

### Name: page
### Title: Invoke a Pager on an R Object
### Aliases: page
### Keywords: utilities

### ** Examples
## Not run: 
##D ## four ways to look at the code of 'page'
##D page(page)             # as an object
##D page("page")           # a character string
##D v <- "page"; page(v)   # a length-one character vector
##D page(utils::page)      # a call
## End(Not run)


cleanEx()
nameEx("person")
### * person

flush(stderr()); flush(stdout())

### Name: person
### Title: Persons
### Aliases: person as.person as.person.default [.person $.person
###   as.character.person c.person format.person print.person
###   toBibtex.person personList as.personList as.personList.person
###   as.personList.default
### Keywords: misc

### ** Examples

## Create a person object directly ...
p1 <- person("Karl", "Pearson", email = "pearson@stats.heaven")

## ... or convert a string.
p2 <- as.person("Ronald Aylmer Fisher")

## Combining and subsetting.
p <- c(p1, p2)
p[1]
p[-1]

## Extracting fields.
p$family
p$email
p[1]$email

## Specifying package authors, example from "boot":
## AC is the first author [aut] who wrote the S original.
## BR is the second author [aut], who translated the code to R [trl],
## and maintains the package [cre].
b <- c(person("Angelo", "Canty", role = "aut", comment =
         "S original, http://statwww.epfl.ch/davison/BMA/library.html"),
       person(c("Brian", "D."), "Ripley", role = c("aut", "trl", "cre"),
              comment = "R port", email = "ripley@stats.ox.ac.uk")
     )
b

## Formatting.
format(b)
format(b, include = c("family", "given", "role"),
   braces = list(family = c("", ","), role = c("(Role(s): ", ")")))

## Conversion to BibTeX author field.
paste(format(b, include = c("given", "family")), collapse = " and ")
toBibtex(b)



cleanEx()
nameEx("prompt")
### * prompt

flush(stderr()); flush(stdout())

### Name: prompt
### Title: Produce Prototype of an R Documentation File
### Aliases: prompt prompt.default prompt.data.frame promptImport
### Keywords: documentation

### ** Examples

require(graphics)
prompt(plot.default)
prompt(interactive, force.function = TRUE)
unlink("plot.default.Rd")
unlink("interactive.Rd")

prompt(women) # data.frame
unlink("women.Rd")

prompt(sunspots) # non-data.frame data
unlink("sunspots.Rd")

## Not run: 
##D ## Create a help file for each function in the .GlobalEnv:
##D for(f in ls()) if(is.function(get(f))) prompt(name = f)
## End(Not run)




cleanEx()
nameEx("promptData")
### * promptData

flush(stderr()); flush(stdout())

### Name: promptData
### Title: Generate Outline Documentation for a Data Set
### Aliases: promptData
### Keywords: documentation

### ** Examples

promptData(sunspots)
unlink("sunspots.Rd")



cleanEx()
nameEx("promptPackage")
### * promptPackage

flush(stderr()); flush(stdout())

### Name: promptPackage
### Title: Generate a Shell for Documentation of a Package
### Aliases: promptPackage
### Keywords: documentation

### ** Examples


cleanEx()
nameEx("read.DIF")
### * read.DIF

flush(stderr()); flush(stdout())

### Name: read.DIF
### Title: Data Input from Spreadsheet
### Aliases: read.DIF
### Keywords: file connection

### ** Examples

## read.DIF() may need transpose = TRUE for a file exported from Excel
udir <- system.file("misc", package = "utils")
dd <- read.DIF(file.path(udir, "exDIF.dif"), header = TRUE, transpose = TRUE)
dc <- read.csv(file.path(udir, "exDIF.csv"), header = TRUE)
stopifnot(identical(dd, dc), dim(dd) == c(4,2))



cleanEx()
nameEx("read.fortran")
### * read.fortran

flush(stderr()); flush(stdout())

### Name: read.fortran
### Title: Read Fixed-Format Data in a Fortran-like Style
### Aliases: read.fortran
### Keywords: file connection

### ** Examples

ff <- tempfile()
cat(file = ff, "123456", "987654", sep = "\n")
read.fortran(ff, c("F2.1","F2.0","I2"))
read.fortran(ff, c("2F1.0","2X","2A1"))
unlink(ff)
cat(file = ff, "123456AB", "987654CD", sep = "\n")
read.fortran(ff, list(c("2F3.1","A2"), c("3I2","2X")))
unlink(ff)
# Note that the first number is read differently than Fortran would
# read it:
cat(file = ff, "12.3456", "1234567", sep = "\n")
read.fortran(ff, "F7.4")
unlink(ff)



cleanEx()
nameEx("read.fwf")
### * read.fwf

flush(stderr()); flush(stdout())

### Name: read.fwf
### Title: Read Fixed Width Format Files
### Aliases: read.fwf
### Keywords: file connection

### ** Examples

ff <- tempfile()
cat(file = ff, "123456", "987654", sep = "\n")
read.fwf(ff, widths = c(1,2,3))    #> 1 23 456 \ 9 87 654
read.fwf(ff, widths = c(1,-2,3))   #> 1 456 \ 9 654
unlink(ff)
cat(file = ff, "123", "987654", sep = "\n")
read.fwf(ff, widths = c(1,0, 2,3))    #> 1 NA 23 NA \ 9 NA 87 654
unlink(ff)
cat(file = ff, "123456", "987654", sep = "\n")
read.fwf(ff, widths = list(c(1,0, 2,3), c(2,2,2))) #> 1 NA 23 456 98 76 54
unlink(ff)



cleanEx()
nameEx("read.socket")
### * read.socket

flush(stderr()); flush(stdout())

### Name: read.socket
### Title: Read from or Write to a Socket
### Aliases: read.socket write.socket
### Keywords: misc

### ** Examples

finger <- function(user, host = "localhost", port = 79, print = TRUE)
{
    if (!is.character(user))
        stop("user name must be a string")
    user <- paste(user,"\r\n")
    socket <- make.socket(host, port)
    on.exit(close.socket(socket))
    write.socket(socket, user)
    output <- character(0)
    repeat{
        ss <- read.socket(socket)
        if (ss == "") break
        output <- paste(output, ss)
    }
    close.socket(socket)
    if (print) cat(output)
    invisible(output)
}
## Not run: 
##D finger("root")  ## only works if your site provides a finger daemon
## End(Not run)



cleanEx()
nameEx("read.table")
### * read.table

flush(stderr()); flush(stdout())

### Name: read.table
### Title: Data Input
### Aliases: read.table read.csv read.csv2 read.delim read.delim2
### Keywords: file connection

### ** Examples

## using count.fields to handle unknown maximum number of fields
## when fill = TRUE
test1 <- c(1:5, "6,7", "8,9,10")
tf <- tempfile()
writeLines(test1, tf)

read.csv(tf, fill = TRUE) # 1 column
ncol <- max(count.fields(tf, sep = ","))
read.csv(tf, fill = TRUE, header = FALSE,
         col.names = paste0("V", seq_len(ncol)))
unlink(tf)

## "Inline" data set, using text=
## Notice that leading and trailing empty lines are auto-trimmed

read.table(header = TRUE, text = "
a b
1 2
3 4
")



cleanEx()
nameEx("recover")
### * recover

flush(stderr()); flush(stdout())

### Name: recover
### Title: Browsing after an Error
### Aliases: recover limitedLabels
### Keywords: programming debugging

### ** Examples

## Not run: 
##D 
##D options(error = recover) # setting the error option
##D 
##D ### Example of interaction
##D 
##D > myFit <- lm(y ~ x, data = xy, weights = w)
##D Error in lm.wfit(x, y, w, offset = offset, ...) :
##D         missing or negative weights not allowed
##D 
##D Enter a frame number, or 0 to exit
##D 1:lm(y ~ x, data = xy, weights = w)
##D 2:lm.wfit(x, y, w, offset = offset, ...)
##D Selection: 2
##D Called from: eval(expr, envir, enclos)
##D Browse[1]> objects() # all the objects in this frame
##D [1] "method" "n"      "ny"     "offset" "tol"    "w"
##D [7] "x"      "y"
##D Browse[1]> w
##D [1] -0.5013844  1.3112515  0.2939348 -0.8983705 -0.1538642
##D [6] -0.9772989  0.7888790 -0.1919154 -0.3026882
##D Browse[1]> dump.frames() # save for offline debugging
##D Browse[1]> c # exit the browser
##D 
##D Enter a frame number, or 0 to exit
##D 1:lm(y ~ x, data = xy, weights = w)
##D 2:lm.wfit(x, y, w, offset = offset, ...)
##D Selection: 0 # exit recover
##D >
##D 
## End(Not run)



cleanEx()
nameEx("relist")
### * relist

flush(stderr()); flush(stdout())

### Name: relist
### Title: Allow Re-Listing an unlist()ed Object
### Aliases: relist relist.default relist.list relist.factor relist.matrix
###   as.relistable is.relistable unlist.relistable
### Keywords: list manip

### ** Examples

 ipar <- list(mean = c(0, 1), vcov = cbind(c(1, 1), c(1, 0)))
 initial.param <- as.relistable(ipar)
 ul <- unlist(initial.param)
 relist(ul)
 stopifnot(identical(relist(ul), initial.param))



cleanEx()
nameEx("removeSource")
### * removeSource

flush(stderr()); flush(stdout())

### Name: removeSource
### Title: Remove Stored Source from a Function.
### Aliases: removeSource
### Keywords: utility

### ** Examples

fn <- function(x) {
  x + 1 # A comment, kept as part of the source
}
fn
fn <- removeSource(fn)
fn



cleanEx()
nameEx("roman")
### * roman

flush(stderr()); flush(stdout())

### Name: roman
### Title: Roman Numerals
### Aliases: as.roman .romans
### Keywords: arith

### ** Examples

## First five roman 'numbers'.
(y <- as.roman(1 : 5))
## Middle one.
y[3]
## Current year as a roman number.
(y <- as.roman(format(Sys.Date(), "%Y")))
## Today, and  10, 20, 30, and 100 years ago ...
y - 10*c(0:3,10)
## Don't show: 
stopifnot(identical(as.character(as.roman("2016") - 10*c(0:3,10)),
             c("MMXVI", "MMVI", "MCMXCVI", "MCMLXXXVI", "MCMXVI"))) 
## End(Don't show)
## mixture of arabic and roman numbers :
as.roman(c(NA, 1:3, "", strrep("I", 1:6))) # + NA with a warning for "IIIIII"
cc <- c(NA, 1:3, strrep("I", 0:5))
(rc <- as.roman(cc)) # two NAs: 0 is not "roman"
(ic <- as.integer(rc)) # works automitcally [without an explicit method]
## simple consistency checks -- arithmetic when result is in  {1,2,..,3899} :
stopifnot(identical(rc, as.roman(rc)), # as.roman(.) is "idempotent"
          identical(rc + rc + (3*rc), rc*5),
          identical(ic, c(NA, 1:3, NA, 1:5)),
          identical(as.integer(5*rc), 5L*ic),
          identical(as.numeric(rc), as.numeric(ic)),
          identical(as.list(rc), as.list(ic)))



cleanEx()
nameEx("rtags")
### * rtags

flush(stderr()); flush(stdout())

### Name: rtags
### Title: An Etags-like Tagging Utility for R
### Aliases: rtags
### Keywords: programming utilities

### ** Examples


## Not run: 
##D rtags("/path/to/src/repository",
##D       pattern = "[.]*\\.[RrSs]$",
##D       keep.re = "/R/",
##D       verbose = TRUE,
##D       ofile = "TAGS",
##D       append = FALSE,
##D       recursive = TRUE)
## End(Not run)




cleanEx()
nameEx("savehistory")
### * savehistory

flush(stderr()); flush(stdout())

### Name: savehistory
### Title: Load or Save or Display the Commands History
### Aliases: loadhistory savehistory history timestamp
### Keywords: utilities

### ** Examples
## Not run: 
##D ## Save the history in the home directory: note that it is not
##D ## (by default) read from there but from the current directory
##D .Last <- function()
##D     if(interactive()) try(savehistory("~/.Rhistory"))
## End(Not run)


cleanEx()
nameEx("select.list")
### * select.list

flush(stderr()); flush(stdout())

### Name: select.list
### Title: Select Items from a List
### Aliases: select.list
### Keywords: utilities

### ** Examples
## Not run: 
##D select.list(sort(.packages(all.available = TRUE)))
## End(Not run)


cleanEx()
nameEx("sessionInfo")
### * sessionInfo

flush(stderr()); flush(stdout())

### Name: sessionInfo
### Title: Collect Information About the Current R Session
### Aliases: sessionInfo toLatex.sessionInfo print.sessionInfo
### Keywords: misc

### ** Examples


cleanEx()
nameEx("setRepositories")
### * setRepositories

flush(stderr()); flush(stdout())

### Name: setRepositories
### Title: Select Package Repositories
### Aliases: setRepositories
### Keywords: utilities

### ** Examples
## Not run: 
##D setRepositories(addURLs =
##D                 c(CRANxtras = "http://www.stats.ox.ac.uk/pub/RWin"))
## End(Not run)


cleanEx()
nameEx("sourceutils")
### * sourceutils

flush(stderr()); flush(stdout())

### Name: sourceutils
### Title: Source Reference Utilities
### Aliases: getSrcFilename getSrcDirectory getSrcref getSrcLocation
### Keywords: utilities

### ** Examples

fn <- function(x) {
  x + 1 # A comment, kept as part of the source
}			

# Show the temporary file directory
# where the example was saved

getSrcDirectory(fn)
getSrcLocation(fn, "line")



cleanEx()
nameEx("stack")
### * stack

flush(stderr()); flush(stdout())

### Name: stack
### Title: Stack or Unstack Vectors from a Data Frame or List
### Aliases: stack stack.default stack.data.frame unstack unstack.default
###   unstack.data.frame
### Keywords: manip

### ** Examples

require(stats)
formula(PlantGrowth)         # check the default formula
pg <- unstack(PlantGrowth)   # unstack according to this formula
pg
stack(pg)                    # now put it back together
stack(pg, select = -ctrl)    # omitting one vector



cleanEx()
nameEx("str")
### * str

flush(stderr()); flush(stdout())

### Name: str
### Title: Compactly Display the Structure of an Arbitrary R Object
### Aliases: str str.default str.data.frame strOptions
### Keywords: print documentation utilities

### ** Examples

require(stats); require(grDevices); require(graphics)
## The following examples show some of 'str' capabilities
str(1:12)
str(ls)
str(args) #- more useful than  args(args) !
str(freeny)
str(str)
str(.Machine, digits.d = 20) # extra digits for identification of binary numbers
str( lsfit(1:9, 1:9))
str( lsfit(1:9, 1:9), max.level = 1)
str( lsfit(1:9, 1:9), width = 60, strict.width = "cut")
str( lsfit(1:9, 1:9), width = 60, strict.width = "wrap")
op <- options(); str(op)   # save first;
                           # otherwise internal options() is used.
need.dev <-
  !exists(".Device") || is.null(.Device) || .Device == "null device"
{ if(need.dev) postscript()
  str(par())
  if(need.dev) graphics.off()
}
ch <- letters[1:12]; is.na(ch) <- 3:5
str(ch) # character NA's

str(list(a = "A", L = as.list(1:100)), list.len = 9)
##                                     ------------
## " .. [list output truncated] "

## Long strings,   'nchar.max'; 'strict.width' :
nchar(longch <- paste(rep(letters,100), collapse = ""))
str(longch)
str(longch, nchar.max = 52)
str(longch, strict.width = "wrap")

## Multibyte characters in strings (in multibyte locales):
oloc <- Sys.getlocale("LC_CTYPE")
mbyte.lc <- if(.Platform$OS.type == "windows")
 "English_United States.28605" else "en_GB.UTF-8"
try(Sys.setlocale("LC_CTYPE", mbyte.lc))
## Truncation behavior (<-> correct width measurement) for "long" non-ASCII:
idx <- c(65313:65338, 65345:65350)
fwch <- intToUtf8(idx) # full width character string: each has width 2
ch <- strtrim(paste(LETTERS, collapse="._"), 64)
(ncc <- c(c.ch = nchar(ch),   w.ch = nchar(ch,   "w"),
          c.fw = nchar(fwch), w.fw = nchar(fwch, "w")))
stopifnot(unname(ncc) == c(64,64, 32, 64))
## nchar.max: 1st line needs an increase of  2  in order to see  1  (in UTF-8!):
invisible(lapply(60:66, function(N) str(fwch, nchar.max = N)))
invisible(lapply(60:66, function(N) str( ch , nchar.max = N))) # "1 is 1" here
## revert locale to previous:
Sys.setlocale("LC_CTYPE", oloc)


## Settings for narrow transcript :
op <- options(width = 60,
              str = strOptions(strict.width = "wrap"))
str(lsfit(1:9,1:9))
str(options())
## reset to previous:
options(op)


## Don't show: 
 ##-- Some "crazy" objects
 str(array(1:5, dim = 20))
 str(factor(character(0)))
 str(as.data.frame(NULL))
## End(Don't show)
str(quote( { A+B; list(C, D) } ))

## Don't show: 
had.stats4 <- "package:stats4" %in% search()
if(!had.stats4)
   rs <- 
## End(Don't show)
## S4 classes :
require(stats4)
x <- 0:10; y <- c(26, 17, 13, 12, 20, 5, 9, 8, 5, 4, 8)
ll <- function(ymax = 15, xh = 6)
      -sum(dpois(y, lambda=ymax/(1+x/xh), log=TRUE))
fit <- mle(ll)
str(fit)
## Don't show: 
if(!had.stats4 && rs) detach("package:stats4")
## End(Don't show)



graphics::par(get("par.postscript", pos = 'CheckExEnv'))
cleanEx()
nameEx("strcapture")
### * strcapture

flush(stderr()); flush(stdout())

### Name: strcapture
### Title: Capture string tokens into a data.frame
### Aliases: strcapture
### Keywords: utilities

### ** Examples

x <- "chr1:1-1000"
pattern <- "(.*?):([[:digit:]]+)-([[:digit:]]+)"
proto <- data.frame(chr=character(), start=integer(), end=integer())
strcapture(pattern, x, proto)



cleanEx()
nameEx("summaryRprof")
### * summaryRprof

flush(stderr()); flush(stdout())

### Name: summaryRprof
### Title: Summarise Output of R Sampling Profiler
### Aliases: summaryRprof
### Keywords: utilities

### ** Examples

## Not run: 
##D ## Rprof() is not available on all platforms
##D Rprof(tmp <- tempfile())
##D example(glm)
##D Rprof()
##D summaryRprof(tmp)
##D unlink(tmp)
## End(Not run)



cleanEx()
nameEx("txtProgressBar")
### * txtProgressBar

flush(stderr()); flush(stdout())

### Name: txtProgressBar
### Title: Text Progress Bar
### Aliases: txtProgressBar getTxtProgressBar setTxtProgressBar
###   close.txtProgressBar
### Keywords: utilities

### ** Examples


cleanEx()
nameEx("url.show")
### * url.show

flush(stderr()); flush(stdout())

### Name: url.show
### Title: Display a text URL
### Aliases: url.show
### Keywords: file misc

### ** Examples

## Not run: url.show("http://www.stats.ox.ac.uk/pub/datasets/csb/ch3a.txt")



cleanEx()
nameEx("vignette")
### * vignette

flush(stderr()); flush(stdout())

### Name: vignette
### Title: View, List or Get R Source of Package Vignettes
### Aliases: vignette edit.vignette print.vignette vignettes
### Keywords: documentation

### ** Examples

## List vignettes from all *attached* packages
vignette(all = FALSE)

## List vignettes from all *installed* packages (can take a long time!):
vignette(all = TRUE)

## The grid intro vignette -- open it
## Not run: vignette("grid") # calling print()
## The same (conditional on existence of the vignettte).
## Note that 'package = *' is much faster in the case of many installed packages:
if(!is.null(v1 <- vignette("grid", package="grid"))) {
## Not run: v1 # calling print(.)
  str(v1)
  ## Now let us have a closer look at the code
  
  
  
  
## Not run: edit(v1) # e.g., to send lines ...
}# if( has vignette "installed")
## A package can have more than one vignette (package grid has several):
vignette(package = "grid")
if(interactive()) {
   ## vignette("rotated")
   ## The same, but without searching for it:
   vignette("rotated", package = "grid")
}



cleanEx()
nameEx("write.table")
### * write.table

flush(stderr()); flush(stdout())

### Name: write.table
### Title: Data Output
### Aliases: write.table write.csv write.csv2
### Keywords: print file

### ** Examples

## Not run: 
##D ## To write a CSV file for input to Excel one might use
##D x <- data.frame(a = I("a \" quote"), b = pi)
##D write.table(x, file = "foo.csv", sep = ",", col.names = NA,
##D             qmethod = "double")
##D ## and to read this file back into R one needs
##D read.table("foo.csv", header = TRUE, sep = ",", row.names = 1)
##D ## NB: you do need to specify a separator if qmethod = "double".
##D 
##D ### Alternatively
##D write.csv(x, file = "foo.csv")
##D read.csv("foo.csv", row.names = 1)
##D ## or without row names
##D write.csv(x, file = "foo.csv", row.names = FALSE)
##D read.csv("foo.csv")
##D 
##D ## To write a file in Mac Roman for simple use in Mac Excel 2004/8
##D write.csv(x, file = "foo.csv", fileEncoding = "macroman")
##D ## or for Windows Excel 2007/10
##D write.csv(x, file = "foo.csv", fileEncoding = "UTF-16LE")
## End(Not run)


### * <FOOTER>
###
options(digits = 7L)
base::cat("Time elapsed: ", proc.time() - base::get("ptime", pos = 'CheckExEnv'),"\n")
#grDevices::dev.off()
###
### Local variables: ***
### mode: outline-minor ***
### outline-regexp: "\\(> \\)?### [*]+" ***
### End: ***
quit('no')
