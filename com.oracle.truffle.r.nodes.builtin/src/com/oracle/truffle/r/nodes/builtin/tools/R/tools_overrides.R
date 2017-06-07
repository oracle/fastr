#
# This material is distributed under the GNU General Public License
# Version 2. You may review the terms of this license at
# http://www.gnu.org/licenses/gpl-2.0.html
#
# Copyright (c) 1995-2015, The R Core Team
# Copyright (c) 2003, The R Foundation
# Copyright (c) 2017, Oracle and/or its affiliates
#
# All rights reserved.
#



# The modified function ".install_package_code_files" copies the source 
# file of the package to be installed into the installation directory 
# (see comments starting with "FastR extension:").
# The code is then parsed from these files such that the parsed
# elements are associated with the source files in the installation 
# directory.

eval(expression({
.install_package_code_files <-
function(dir, outDir)
{
    if(!dir.exists(dir))
        stop(gettextf("directory '%s' does not exist", dir),
             domain = NA)
    dir <- file_path_as_absolute(dir)

    ## Attempt to set the LC_COLLATE locale to 'C' to turn off locale
    ## specific sorting.
    curLocale <- Sys.getlocale("LC_COLLATE")
    on.exit(Sys.setlocale("LC_COLLATE", curLocale), add = TRUE)
    ## (Guaranteed to work as per the Sys.setlocale() docs.)
    lccollate <- "C"
    if(Sys.setlocale("LC_COLLATE", lccollate) != lccollate) {
        ## <NOTE>
        ## I don't think we can give an error here.
        ## It may be the case that Sys.setlocale() fails because the "OS
        ## reports request cannot be honored" (src/main/platform.c), in
        ## which case we should still proceed ...
        warning("cannot turn off locale-specific sorting via LC_COLLATE")
        ## </NOTE>
    }

    ## We definitely need a valid DESCRIPTION file.
    db <- .read_description(file.path(dir, "DESCRIPTION"))

    codeDir <- file.path(dir, "R")
    if(!dir.exists(codeDir)) return(invisible())

    codeFiles <- list_files_with_type(codeDir, "code", full.names = FALSE)

    collationField <-
        c(paste("Collate", .OStype(), sep = "."), "Collate")
    if(any(i <- collationField %in% names(db))) {
        collationField <- collationField[i][1L]
        codeFilesInCspec <- .read_collate_field(db[collationField])
        ## Duplicated entries in the collation spec?
        badFiles <-
            unique(codeFilesInCspec[duplicated(codeFilesInCspec)])
        if(length(badFiles)) {
            out <- gettextf("\nduplicated files in '%s' field:",
                            collationField)
            out <- paste(out,
                         paste(" ", badFiles, collapse = "\n"),
                         sep = "\n")
            stop(out, domain = NA)
        }
        ## See which files are listed in the collation spec but don't
        ## exist.
        badFiles <- setdiff(codeFilesInCspec, codeFiles)
        if(length(badFiles)) {
            out <- gettextf("\nfiles in '%s' field missing from '%s':",
                            collationField,
                            codeDir)
            out <- paste(out,
                         paste(" ", badFiles, collapse = "\n"),
                         sep = "\n")
            stop(out, domain = NA)
        }
        ## See which files exist but are missing from the collation
        ## spec.  Note that we do not want the collation spec to use
        ## only a subset of the available code files.
        badFiles <- setdiff(codeFiles, codeFilesInCspec)
        if(length(badFiles)) {
            out <- gettextf("\nfiles in '%s' missing from '%s' field:",
                            codeDir,
                            collationField)
            out <- paste(out,
                         paste(" ", badFiles, collapse = "\n"),
                         sep = "\n")
            stop(out, domain = NA)
        }
        ## Everything's groovy ...
        codeFiles <- codeFilesInCspec
    }

    codeFiles <- file.path(codeDir, codeFiles)

    if(!dir.exists(outDir) && !dir.create(outDir))
        stop(gettextf("cannot open directory '%s'", outDir),
             domain = NA)
    outCodeDir <- file.path(outDir, "R")
    if(!dir.exists(outCodeDir) && !dir.create(outCodeDir))
        stop(gettextf("cannot open directory '%s'", outCodeDir),
             domain = NA)
    outFile <- file.path(outCodeDir, db["Package"])
    if(!file.create(outFile))
        stop(gettextf("unable to create '%s'", outFile), domain = NA)
    writeLines(paste0(".packageName <- \"", db["Package"], "\""),
               outFile)
    enc <- as.vector(db["Encoding"])
    need_enc <- !is.na(enc) # Encoding was specified
    ## assume that if locale is 'C' we can used 8-bit encodings unchanged.
    if(need_enc && !(Sys.getlocale("LC_CTYPE") %in% c("C", "POSIX"))) {
        con <- file(outFile, "a")
        on.exit(close(con))  # Windows does not like files left open
        for(f in codeFiles) {
            tmp <- iconv(readLines(f, warn = FALSE), from = enc, to = "")
            if(length(bad <- which(is.na(tmp)))) {
                warning(sprintf(ngettext(length(bad),
                                         "unable to re-encode %s line %s",
                                         "unable to re-encode %s lines %s"),
                                sQuote(basename(f)),
                                paste(bad, collapse = ", ")),
                        domain = NA, call. = FALSE)
                tmp <- iconv(readLines(f, warn = FALSE), from = enc, to = "",
                             sub = "byte")
            }

			# FastR extension: also copy original source file
            singleOutFile <- file.path(outCodeDir, basename(f))
            writeLines(tmp, file(singleOutFile))

            writeLines(paste0("#line 1 \"", singleOutFile, "\""), con)
            writeLines(tmp, con)
        }
	close(con); on.exit()
    } else {
        ## <NOTE>
        ## It may be safer to do
        ##   writeLines(sapply(codeFiles, readLines), outFile)
        ## instead, but this would be much slower ...
        ## use fast version of file.append that ensures LF between files

		# FastR extension: also copy original source file
        singleOutFiles <- file.path(outCodeDir, basename(codeFiles))
        file.copy(codeFiles, singleOutFiles)

        if(!all(.file_append_ensuring_LFs(outFile, singleOutFiles)))
            stop("unable to write code files")
        ## </NOTE>
    }
    ## A syntax check here, so that we do not install a broken package.
    ## FIXME:  this is only needed if we don't lazy load, as the lazy loader
    ## would detect the error.
    op <- options(showErrorCalls=FALSE)
    on.exit(options(op))
    parse(outFile)
    invisible()
}

}), asNamespace("tools"))
