# Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
# DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
#
# This code is free software; you can redistribute it and/or modify it
# under the terms of the GNU General Public License version 2 only, as
# published by the Free Software Foundation.
#
# This code is distributed in the hope that it will be useful, but WITHOUT
# ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
# FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
# version 2 for more details (a copy is included in the LICENSE file that
# accompanied this code).
#
# You should have received a copy of the GNU General Public License version
# 2 along with this work; if not, write to the Free Software Foundation,
# Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
#
# Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
# or visit www.oracle.com if you need additional information or have any
# questions.

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

            writeLines(paste0("#line 1 \"", f, "\""), con)
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
