#  File src/library/base/R/library.R
#  Part of the R package, http://www.R-project.org
#
#  Copyright (C) 1995-2013 The R Core Team
#
#  This program is free software; you can redistribute it and/or modify
#  it under the terms of the GNU General Public License as published by
#  the Free Software Foundation; either version 2 of the License, or
#  (at your option) any later version.
#
#  This program is distributed in the hope that it will be useful,
#  but WITHOUT ANY WARRANTY; without even the implied warranty of
#  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
#  GNU General Public License for more details.
#
#  A copy of the GNU General Public License is available at
#  http://www.r-project.org/Licenses/

# Until problems with numeric version generic ops are fixed.

library <-
    function(package, help, pos = 2, lib.loc = NULL, character.only = FALSE,
        logical.return = FALSE, warn.conflicts = TRUE,
        quietly = FALSE, verbose = getOption("verbose"))
{
  testRversion <- function(pkgInfo, pkgname, pkgpath)
  {
    if(is.null(built <- pkgInfo$Built))
      stop(gettextf("package %s has not been installed properly\n",
              sQuote(pkgname)),
          call. = FALSE, domain = NA)

    ## which version was this package built under?
    R_version_built_under <- as.numeric_version(built$R)
# Suppress check to workaround numeric_version generic comparison
#    if(R_version_built_under < "3.0.0")
#      stop(gettextf("package %s was built before R 3.0.0: please re-install it",
#              sQuote(pkgname)), call. = FALSE, domain = NA)

    current <- getRversion()
    ## depends on R version?
    ## as it was installed >= 2.7.0 it will have Rdepends2
    if(length(Rdeps <- pkgInfo$Rdepends2)) {
      for(dep in Rdeps)
        if(length(dep) > 1L) {
          target <- dep$version
# can't do.call generics
      res <- T
#          res <- if(is.character(target)) {
#                do.call(dep$op, # these are both strings
#                    list(as.numeric(R.version[["svn rev"]]),
#                        as.numeric(sub("^r", "", dep$version))))
#              } else {
#                do.call(dep$op,
#                    list(current, as.numeric_version(target)))
#                ##                        target <- as.numeric_version(dep$version)
#                ##                        eval(parse(text=paste("current", dep$op, "target")))
#              }
          if(!res)
            stop(gettextf("This is R %s, package %s needs %s %s",
                    current, sQuote(pkgname), dep$op, target),
                call. = FALSE, domain = NA)
        }
    }
    ## warn if installed under a later version of R
# Suppress check to workaround numeric_version generic comparison
#    if(R_version_built_under > current)
#      warning(gettextf("package %s was built under R version %s",
#              sQuote(pkgname), as.character(built$R)),
#          call. = FALSE, domain = NA)
    platform <- built$Platform
    r_arch <- .Platform$r_arch
    if(.Platform$OS.type == "unix") {
      ## allow mismatches if r_arch is in use, e.g.
      ## i386-gnu-linux vs x86-gnu-linux depending on
      ## build system.
      if(!nzchar(r_arch) && length(grep("\\w", platform)) &&
          !testPlatformEquivalence(platform, R.version$platform))
        stop(gettextf("package %s was built for %s",
                sQuote(pkgname), platform),
            call. = FALSE, domain = NA)
    } else {  # Windows
      ## a check for 'mingw' suffices, since i386 and x86_64
      ## have DLLs in different places.  This allows binary packages
      ## to be merged.
      if(nzchar(platform) && !grepl("mingw", platform))
        stop(gettextf("package %s was built for %s",
                sQuote(pkgname), platform),
            call. = FALSE, domain = NA)
    }
    ## if using r_arch subdirs, check for presence
    if(nzchar(r_arch)
        && file.exists(file.path(pkgpath, "libs"))
        && !file.exists(file.path(pkgpath, "libs", r_arch)))
      stop(gettextf("package %s is not installed for 'arch = %s'",
              sQuote(pkgname), r_arch),
          call. = FALSE, domain = NA)
  }

  checkLicense <- function(pkg, pkgInfo, pkgPath)
  {
    L <- tools:::analyze_license(pkgInfo$DESCRIPTION["License"])
    if(!L$is_empty && !L$is_verified) {
      site_file <- path.expand(file.path(R.home("etc"), "licensed.site"))
      if(file.exists(site_file) &&
          pkg %in% readLines(site_file)) return()
      personal_file <- path.expand("~/.R/licensed")
      if(file.exists(personal_file)) {
        agreed <- readLines(personal_file)
        if(pkg %in% agreed) return()
      } else agreed <- character()
      if(!interactive())
        stop(gettextf("package %s has a license that you need to accept in an interactive session", sQuote(pkg)), domain = NA)
      lfiles <- file.path(pkgpath, c("LICENSE", "LICENCE"))
      lfiles <- lfiles[file.exists(lfiles)]
      if(length(lfiles)) {
        message(gettextf("package %s has a license that you need to accept after viewing", sQuote(pkg)), domain = NA)
        readline("press RETURN to view license")
        encoding <- pkgInfo$DESCRIPTION["Encoding"]
        if(is.na(encoding)) encoding <- ""
        ## difR and EVER have a Windows' 'smart quote' LICEN[CS]E file
        if(encoding == "latin1") encoding <- "cp1252"
        file.show(lfiles[1L], encoding = encoding)
      } else {
        message(gettextf("package %s has a license that you need to accept:\naccording to the DESCRIPTION file it is", sQuote(pkg)), domain = NA)
        message(pkgInfo$DESCRIPTION["License"], domain = NA)
      }
      choice <- menu(c("accept", "decline"),
          title = paste("License for", sQuote(pkg)))
      if(choice != 1)
        stop(gettextf("license for package %s not accepted",
                sQuote(package)), domain = NA, call. = FALSE)
      dir.create(dirname(personal_file), showWarnings=FALSE)
      writeLines(c(agreed, pkg), personal_file)
    }
  }

  checkNoGenerics <- function(env, pkg)
  {
    nenv <- env
    ns <- .getNamespace(as.name(pkg))
    if(!is.null(ns)) nenv <- asNamespace(ns)
    if (exists(".noGenerics", envir = nenv, inherits = FALSE))
      TRUE
    else {
      ## A package will have created a generic
      ## only if it has created a formal method.
      length(objects(env, pattern="^\\.__[MT]", all.names=TRUE)) == 0L
    }
  }

  ## FIXME: ./attach.R 's attach() has *very* similar checkConflicts(), keep in sync
  checkConflicts <- function(package, pkgname, pkgpath, nogenerics, env)
  {
    dont.mind <- c("last.dump", "last.warning", ".Last.value",
        ".Random.seed", ".Last.lib", ".onDetach",
        ".packageName", ".noGenerics", ".required",
        ".no_S3_generics", ".Depends", ".requireCachedGenerics")
    sp <- search()
    lib.pos <- match(pkgname, sp)
    ## ignore generics not defined for the package
    ob <- objects(lib.pos, all.names = TRUE)
    if(!nogenerics) {
      ##  Exclude generics that are consistent with implicit generic
      ## from another package.  A better test would be to move this
      ## down into the loop and test against specific other package name
      ## but subtle conflicts like that are likely to be found elsewhere
      these <- ob[substr(ob, 1L, 6L) == ".__T__"]
      gen <- gsub(".__T__(.*):([^:]+)", "\\1", these)
      from <- gsub(".__T__(.*):([^:]+)", "\\2", these)
      gen <- gen[from != package]
      ob <- ob[!(ob %in% gen)]
    }
    fst <- TRUE
    ipos <- seq_along(sp)[-c(lib.pos,
            match(c("Autoloads", "CheckExEnv"), sp, 0L))]
    for (i in ipos) {
      obj.same <- match(objects(i, all.names = TRUE), ob, nomatch = 0L)
      if (any(obj.same > 0)) {
        same <- ob[obj.same]
        same <- same[!(same %in% dont.mind)]
        Classobjs <- grep("^\\.__", same)
        if(length(Classobjs)) same <- same[-Classobjs]
        ## report only objects which are both functions or
        ## both non-functions.
        same.isFn <- function(where)
          vapply(same, exists, NA,
              where = where, mode = "function", inherits = FALSE)
        same <- same[same.isFn(i) == same.isFn(lib.pos)]
        ## if a package imports and re-exports, there's no problem
        not.Ident <- function(ch, TRAFO=identity, ...)
          vapply(ch, function(.)
                !identical(TRAFO(get(., i)),
                    TRAFO(get(., lib.pos)), ...),
              NA)
        if(length(same)) same <- same[not.Ident(same)]
        ## if the package is 'base' it cannot be imported and re-exported,
        ## allow a "copy":
        if(length(same) && identical(sp[i], "package:base"))
          same <- same[not.Ident(same, ignore.environment = TRUE)]
        if(length(same)) {
          if (fst) {
            fst <- FALSE
            packageStartupMessage(gettextf("\nAttaching package: %s\n",
                    sQuote(package)),
                domain = NA)
          }

		  msg <- .maskedMsg(same, pkg = sQuote(sp[i]), by = i < lib.pos)
		  packageStartupMessage(msg, domain = NA)
	  }
      }
    }
  }

  if(verbose && quietly)
    message("'verbose' and 'quietly' are both true; being verbose then ..")
  if(!missing(package)) {
    if (is.null(lib.loc)) lib.loc <- .libPaths()
    ## remove any non-existent directories
    lib.loc <- lib.loc[file.info(lib.loc)$isdir %in% TRUE]

    if(!character.only)
      package <- as.character(substitute(package))
    if(length(package) != 1L)
      stop("'package' must be of length 1")
    if(is.na(package) || (package == ""))
      stop("invalid package name")

    pkgname <- paste("package", package, sep = ":")
    newpackage <- is.na(match(pkgname, search()))
    if(newpackage) {
      ## Check for the methods package before attaching this
      ## package.
      ## Only if it is _already_ here do we do cacheMetaData.
      ## The methods package caches all other pkgs when it is
      ## attached.

      pkgpath <- find.package(package, lib.loc, quiet = TRUE,
          verbose = verbose)
      if(length(pkgpath) == 0L) {
        txt <- if(length(lib.loc))
              gettextf("there is no package called %s", sQuote(package))
            else
              gettext("no library trees found in 'lib.loc'")
        if(logical.return) {
          warning(txt, domain = NA)
          return(FALSE)
        } else stop(txt, domain = NA)
      }
      which.lib.loc <- normalizePath(dirname(pkgpath), "/", TRUE)
      pfile <- system.file("Meta", "package.rds", package = package,
          lib.loc = which.lib.loc)
      if(!nzchar(pfile))
        stop(gettextf("%s is not a valid installed package",
                sQuote(package)), domain = NA)
      pkgInfo <- readRDS(pfile)
      testRversion(pkgInfo, package, pkgpath)
      ## avoid any bootstrapping issues by these exemptions
      if(!package %in% c("datasets", "grDevices", "graphics", "methods",
              "splines", "stats", "stats4", "tcltk", "tools",
              "utils") &&
          isTRUE(getOption("checkPackageLicense", FALSE)))
        checkLicense(package, pkgInfo, pkgpath)

      ## The check for inconsistent naming is now in find.package

      if(is.character(pos)) {
        npos <- match(pos, search())
        if(is.na(npos)) {
          warning(gettextf("%s not found on search path, using pos = 2", sQuote(pos)), domain = NA)
          pos <- 2
        } else pos <- npos
      }
      .getRequiredPackages2(pkgInfo, quietly = quietly)
      deps <- unique(names(pkgInfo$Depends))

      ## If the namespace mechanism is available and the package
      ## has a namespace, then the namespace loading mechanism
      ## takes over.
      if (packageHasNamespace(package, which.lib.loc)) {
        tt <- try({
              ns <- loadNamespace(package, c(which.lib.loc, lib.loc))
              env <- attachNamespace(ns, pos = pos, deps)
            })
        if (inherits(tt, "try-error"))
          if (logical.return)
            return(FALSE)
          else stop(gettextf("package or namespace load failed for %s",
                    sQuote(package)),
                call. = FALSE, domain = NA)
        else {
          on.exit(detach(pos = pos))
          ## If there are S4 generics then the package should
          ## depend on methods
          nogenerics <-
              !.isMethodsDispatchOn() || checkNoGenerics(env, package)
          if(warn.conflicts && # never will with a namespace
              !exists(".conflicts.OK", envir = env, inherits = FALSE))
            checkConflicts(package, pkgname, pkgpath,
                nogenerics, ns)
          on.exit()
          if (logical.return)
            return(TRUE)
          else
            return(invisible(.packages()))
        }
      } else
        stop(gettextf("package %s does not have a namespace and should be re-installed",
                sQuote(package)), domain = NA)
    }
    if (verbose && !newpackage)
      warning(gettextf("package %s already present in search()",
              sQuote(package)), domain = NA)

  }
  else if(!missing(help)) {
    if(!character.only)
      help <- as.character(substitute(help))
    pkgName <- help[1L]            # only give help on one package
    pkgPath <- find.package(pkgName, lib.loc, verbose = verbose)
    docFiles <- c(file.path(pkgPath, "Meta", "package.rds"),
        file.path(pkgPath, "INDEX"))
    if(file.exists(vignetteIndexRDS <-
            file.path(pkgPath, "Meta", "vignette.rds")))
      docFiles <- c(docFiles, vignetteIndexRDS)
    pkgInfo <- vector("list", 3L)
    readDocFile <- function(f) {
      if(basename(f) %in% "package.rds") {
        txt <- readRDS(f)$DESCRIPTION
        if("Encoding" %in% names(txt)) {
          to <- if(Sys.getlocale("LC_CTYPE") == "C") "ASCII//TRANSLIT"else ""
          tmp <- try(iconv(txt, from=txt["Encoding"], to=to))
          if(!inherits(tmp, "try-error"))
            txt <- tmp
          else
            warning("'DESCRIPTION' has an 'Encoding' field and re-encoding is not possible", call.=FALSE)
        }
        nm <- paste0(names(txt), ":")
        formatDL(nm, txt, indent = max(nchar(nm, "w")) + 3)
      } else if(basename(f) %in% "vignette.rds") {
        txt <- readRDS(f)
        ## New-style vignette indices are data frames with more
        ## info than just the base name of the PDF file and the
        ## title.  For such an index, we give the names of the
        ## vignettes, their titles, and indicate whether PDFs
        ## are available.
        ## The index might have zero rows.
        if(is.data.frame(txt) && nrow(txt))
          cbind(basename(gsub("\\.[[:alpha:]]+$", "",
                      txt$File)),
              paste(txt$Title,
                  paste0(rep.int("(source", NROW(txt)),
                      ifelse(txt$PDF != "",
                          ", pdf",
                          ""),
                      ")")))
        else NULL
      } else
        readLines(f)
    }
    for(i in which(file.exists(docFiles)))
      pkgInfo[[i]] <- readDocFile(docFiles[i])
    y <- list(name = pkgName, path = pkgPath, info = pkgInfo)
    class(y) <- "packageInfo"
    return(y)
  }
  else {
    ## library():
    if(is.null(lib.loc))
      lib.loc <- .libPaths()
    db <- matrix(character(), nrow = 0L, ncol = 3L)
    nopkgs <- character()

    for(lib in lib.loc) {
      a <- .packages(all.available = TRUE, lib.loc = lib)
      for(i in sort(a)) {
        ## All packages installed under 2.0.0 should have
        ## 'package.rds' but we have not checked.
        file <- system.file("Meta", "package.rds", package = i,
            lib.loc = lib)
        title <- if(file != "") {
              txt <- readRDS(file)
              if(is.list(txt)) txt <- txt$DESCRIPTION
              ## we may need to re-encode here.
              if("Encoding" %in% names(txt)) {
                to <- if(Sys.getlocale("LC_CTYPE") == "C") "ASCII//TRANSLIT" else ""
                tmp <- try(iconv(txt, txt["Encoding"], to, "?"))
                if(!inherits(tmp, "try-error"))
                  txt <- tmp
                else
                  warning("'DESCRIPTION' has an 'Encoding' field and re-encoding is not possible", call.=FALSE)
              }
              txt["Title"]
            } else NA
        if(is.na(title))
          title <- " ** No title available ** "
        db <- rbind(db, cbind(i, lib, title))
      }
      if(length(a) == 0L)
        nopkgs <- c(nopkgs, lib)
    }
    dimnames(db) <- list(NULL, c("Package", "LibPath", "Title"))
    if(length(nopkgs) && !missing(lib.loc)) {
      pkglist <- paste(sQuote(nopkgs), collapse = ", ")
      msg <- sprintf(ngettext(length(nopkgs),
              "library %s contains no packages",
              "libraries %s contain no packages"),
          pkglist)
      warning(msg, domain=NA)
    }

    y <- list(header = NULL, results = db, footer = NULL)
    class(y) <- "libraryIQR"
    return(y)
  }

  if (logical.return)
    TRUE
  else invisible(.packages())
}

