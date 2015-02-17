#  File src/library/base/R/namespace.R
#  Part of the R package, http://www.R-project.org
#
#  Copyright (C) 1995-2014 The R Core Team
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

loadNamespace <- function (package, lib.loc = NULL,
        keep.source = getOption("keep.source.pkgs"),
        partial = FALSE, versionCheck = NULL)
{
    package <- as.character(package)[[1L]]

    ## check for cycles
    dynGet <- function(name,
            notFound = stop(gettextf("%s not found", name),
                    domain = NA))
    {
        n <- sys.nframe()
        while (n > 1) {
            n <- n - 1
            env <- sys.frame(n)
            if (exists(name, envir = env, inherits = FALSE))
                return(get(name, envir = env, inherits = FALSE))
        }
        notFound
    }
    loading <- dynGet("__NameSpacesLoading__", NULL)
    if (match(package, loading, 0L))
        stop("cyclic namespace dependency detected when loading ",
                sQuote(package), ", already loading ",
                paste(sQuote(loading), collapse = ", "),
                domain = NA)
    "__NameSpacesLoading__" <- c(package, loading)

    ns <- .Internal(getRegisteredNamespace(as.name(package)))
    if (! is.null(ns)) {
        if(length(z <- versionCheck) == 3L) {
            current <- getNamespaceVersion(ns)
            if(!do.call(z$op, list(as.numeric_version(current), z$version)))
                stop(gettextf("namespace %s %s is already loaded, but %s %s is required",
                                sQuote(package), current, z$op, z$version),
                        domain = NA)
        }
        ns
    } else {
        ## only used here for .onLoad
        runHook <- function(hookname, env, libname, pkgname) {
            if (exists(hookname, envir = env, inherits = FALSE)) {
                fun <- get(hookname, envir = env, inherits = FALSE)
                res <- tryCatch(fun(libname, pkgname), error = identity)
                if (inherits(res, "error")) {
                    stop(gettextf("%s failed in %s() for '%s', details:\n  call: %s\n  error: %s",
                                    hookname, "loadNamespace", pkgname,
                                    deparse(conditionCall(res))[1L],
                                    conditionMessage(res)),
                            call. = FALSE, domain = NA)
                }
            }
        }
        runUserHook <- function(pkgname, pkgpath) {
            hooks <- getHook(packageEvent(pkgname, "onLoad")) # might be list()
            for(fun in hooks) try(fun(pkgname, pkgpath))
        }
        makeNamespace <- function(name, version = NULL, lib = NULL) {
            impenv <- new.env(parent = .BaseNamespaceEnv, hash = TRUE)
            attr(impenv, "name") <- paste("imports", name, sep = ":")
            env <- new.env(parent = impenv, hash = TRUE)
            name <- as.character(as.name(name))
            version <- as.character(version)
            info <- new.env(hash = TRUE, parent = baseenv())
            assign(".__NAMESPACE__.", info, envir = env)
            assign("spec", c(name = name, version = version), envir = info)
            setNamespaceInfo(env, "exports", new.env(hash = TRUE, parent = baseenv()))
            dimpenv <- new.env(parent = baseenv(), hash = TRUE)
            attr(dimpenv, "name") <- paste("lazydata", name, sep = ":")
            setNamespaceInfo(env, "lazydata", dimpenv)
            setNamespaceInfo(env, "imports", list("base" = TRUE))
            ## this should be an absolute path
            setNamespaceInfo(env, "path",
                    normalizePath(file.path(lib, name), "/", TRUE))
            setNamespaceInfo(env, "dynlibs", NULL)
            setNamespaceInfo(env, "S3methods", matrix(NA_character_, 0L, 3L))
            assign(".__S3MethodsTable__.",
                    new.env(hash = TRUE, parent = baseenv()),
                    envir = env)
            .Internal(registerNamespace(name, env))
            env
        }
        sealNamespace <- function(ns) {
            namespaceIsSealed <- function(ns)
                environmentIsLocked(ns)
            ns <- asNamespace(ns, base.OK = FALSE)
            if (namespaceIsSealed(ns))
                stop(gettextf("namespace %s is already sealed in 'loadNamespace'",
                                sQuote(getNamespaceName(ns))),
                        call. = FALSE, domain = NA)
            lockEnvironment(ns, TRUE)
            lockEnvironment(parent.env(ns), TRUE)
        }
        addNamespaceDynLibs <- function(ns, newlibs) {
            dynlibs <- getNamespaceInfo(ns, "dynlibs")
            setNamespaceInfo(ns, "dynlibs", c(dynlibs, newlibs))
        }

        bindTranslations <- function(pkgname, pkgpath)
        {
            ## standard packages are treated differently
            std <- c("compiler", "foreign", "grDevices", "graphics", "grid",
                    "methods", "parallel", "splines", "stats", "stats4",
                    "tcltk", "tools", "utils")
            popath <- if (pkgname %in% std) .popath else file.path(pkgpath, "po")
            if(!file.exists(popath)) return()
            bindtextdomain(pkgname, popath)
            bindtextdomain(paste("R", pkgname, sep = "-"), popath)
        }

        assignNativeRoutines <- function(dll, lib, env, nativeRoutines) {
            if(length(nativeRoutines) == 0L) return(NULL)

            if(nativeRoutines$useRegistration) {
                ## Use the registration information to register ALL the symbols
                fixes <- nativeRoutines$registrationFixes
                routines <- getDLLRegisteredRoutines.DLLInfo(dll, addNames = FALSE)
                lapply(routines,
                        function(type) {
                            lapply(type,
                                    function(sym) {
                                        varName <- paste0(fixes[1L], sym$name, fixes[2L])
                                        if(exists(varName, envir = env))
                                            warning(gettextf("failed to assign RegisteredNativeSymbol for %s to %s since %s is already defined in the %s namespace",
                                                            sym$name, varName, varName, sQuote(package)),
                                                    domain = NA)
                                        else
                                            assign(varName, sym, envir = env)
                                    })
                        })

            }

            symNames <- nativeRoutines$symbolNames
            if(length(symNames) == 0L) return(NULL)

            symbols <- getNativeSymbolInfo(symNames, dll, unlist = FALSE,
                    withRegistrationInfo = TRUE)
            lapply(seq_along(symNames),
                    function(i) {
                        ## could vectorize this outside of the loop
                        ## and assign to different variable to
                        ## maintain the original names.
                        varName <- names(symNames)[i]
                        origVarName <- symNames[i]
                        if(exists(varName, envir = env))
                            if(origVarName != varName)
                                warning(gettextf("failed to assign NativeSymbolInfo for %s to %s since %s is already defined in the %s namespace",
                                                origVarName, varName, varName, sQuote(package)),
                                        domain = NA)
                            else
                                warning(gettextf("failed to assign NativeSymbolInfo for %s since %s is already defined in the %s namespace",
                                                origVarName, varName, sQuote(package)),
                                        domain = NA)
                        else
                            assign(varName, symbols[[origVarName]], envir = env)

                    })
            symbols
        }

        ## find package and check it has a namespace
        pkgpath <- find.package(package, lib.loc, quiet = TRUE)
        if (length(pkgpath) == 0L)
            stop(gettextf("there is no package called %s", sQuote(package)),
                    domain = NA)
        bindTranslations(package, pkgpath)
        package.lib <- dirname(pkgpath)
        package <- basename(pkgpath) # need the versioned name
        if (! packageHasNamespace(package, package.lib)) {
            hasNoNamespaceError <-
                    function (package, package.lib, call = NULL) {
                class <- c("hasNoNamespaceError", "error", "condition")
                msg <- gettextf("package %s does not have a namespace",
                        sQuote(package))
                structure(list(message = msg, package = package,
                                package.lib = package.lib, call = call),
                        class = class)
            }
            stop(hasNoNamespaceError(package, package.lib))
        }

        ## create namespace; arrange to unregister on error
        ## Can we rely on the existence of R-ng 'nsInfo.rds' and
        ## 'package.rds'?
        ## No, not during builds of standard packages
        ## stats4 depends on methods, but exports do not matter
        ## whilst it is being built
        nsInfoFilePath <- file.path(pkgpath, "Meta", "nsInfo.rds")
        nsInfo <- if(file.exists(nsInfoFilePath)) readRDS(nsInfoFilePath)
                else parseNamespaceFile(package, package.lib, mustExist = FALSE)

        pkgInfoFP <- file.path(pkgpath, "Meta", "package.rds")
        if(file.exists(pkgInfoFP)) {
            pkgInfo <- readRDS(pkgInfoFP)
            version <- pkgInfo$DESCRIPTION["Version"]
            vI <- pkgInfo$Imports
            if(is.null(built <- pkgInfo$Built))
                stop(gettextf("package %s has not been installed properly\n",
                                sQuote(basename(pkgpath))),
                        call. = FALSE, domain = NA)
            R_version_built_under <- as.numeric_version(built$R)
# can't do.call generics
#            if(R_version_built_under < "3.0.0")
#                stop(gettextf("package %s was built before R 3.0.0: please re-install it",
#                                sQuote(basename(pkgpath))),
#                        call. = FALSE, domain = NA)
            ## we need to ensure that S4 dispatch is on now if the package
            ## will require it, or the exports will be incomplete.
            dependsMethods <- "methods" %in% names(pkgInfo$Depends)
            if(dependsMethods) loadNamespace("methods")
            if(length(z <- versionCheck) == 3L &&
                    !do.call(z$op, list(as.numeric_version(version), z$version)))
                stop(gettextf("namespace %s %s is being loaded, but %s %s is required",
                                sQuote(package), version, z$op, z$version),
                        domain = NA)
        }
        ns <- makeNamespace(package, version = version, lib = package.lib)
        on.exit(.Internal(unregisterNamespace(package)))

        ## process imports
        for (i in nsInfo$imports) {
            if (is.character(i))
                namespaceImport(ns,
                        loadNamespace(i, c(lib.loc, .libPaths()),
                                versionCheck = vI[[i]]),
                        from = package)
            else
                namespaceImportFrom(ns,
                        loadNamespace(j <- i[[1L]],
                                c(lib.loc, .libPaths()),
                                versionCheck = vI[[j]]),
                        i[[2L]], from = package)
        }
        for(imp in nsInfo$importClasses)
            namespaceImportClasses(ns, loadNamespace(j <- imp[[1L]],
                            c(lib.loc, .libPaths()),
                            versionCheck = vI[[j]]),
                    imp[[2L]], from = package)
        for(imp in nsInfo$importMethods)
            namespaceImportMethods(ns, loadNamespace(j <- imp[[1L]],
                            c(lib.loc, .libPaths()),
                            versionCheck = vI[[j]]),
                    imp[[2L]], from = package)

        ## store info for loading namespace for loadingNamespaceInfo to read
        "__LoadingNamespaceInfo__" <- list(libname = package.lib,
                pkgname = package)

        env <- asNamespace(ns)
        ## save the package name in the environment
        assign(".packageName", package, envir = env)

        ## load the code
        codename <- strsplit(package, "_", fixed = TRUE)[[1L]][1L]
        codeFile <- file.path(pkgpath, "R", codename)
        if (file.exists(codeFile)) {
            res <- try(sys.source(codeFile, env, keep.source = keep.source))
            if(inherits(res, "try-error"))
                stop(gettextf("unable to load R code in package %s",
                                sQuote(package)), call. = FALSE, domain = NA)
        }
        # a package without R code currently is required to have a namespace
        # else warning(gettextf("package %s contains no R code",
        #                        sQuote(package)), call. = FALSE, domain = NA)

        ## partial loading stops at this point
        ## -- used in preparing for lazy-loading
        if (partial) return(ns)

        ## lazy-load any sysdata
        dbbase <- file.path(pkgpath, "R", "sysdata")
        if (file.exists(paste0(dbbase, ".rdb"))) lazyLoad(dbbase, env)

        ## load any lazydata into a separate environment
        dbbase <- file.path(pkgpath, "data", "Rdata")
        if(file.exists(paste0(dbbase, ".rdb")))
            lazyLoad(dbbase, getNamespaceInfo(ns, "lazydata"))

        ## register any S3 methods
        registerS3methods(nsInfo$S3methods, package, env)

        ## load any dynamic libraries
        dlls <- list()
        dynLibs <- nsInfo$dynlibs
        for (i in seq_along(dynLibs)) {
            lib <- dynLibs[i]
            dlls[[lib]]  <- library.dynam(lib, package, package.lib)
            assignNativeRoutines(dlls[[lib]], lib, env,
                    nsInfo$nativeRoutines[[lib]])

            ## If the DLL has a name as in useDynLib(alias = foo),
            ## then assign DLL reference to alias.  Check if
            ## names() is NULL to handle case that the nsInfo.rds
            ## file was created before the names were added to the
            ## dynlibs vector.
            if(!is.null(names(nsInfo$dynlibs))
                    && names(nsInfo$dynlibs)[i] != "")
                assign(names(nsInfo$dynlibs)[i], dlls[[lib]], envir = env)
            setNamespaceInfo(env, "DLLs", dlls)
        }
        addNamespaceDynLibs(env, nsInfo$dynlibs)


        ## used in e.g. utils::assignInNamespace
        Sys.setenv("_R_NS_LOAD_" = package)
        on.exit(Sys.unsetenv("_R_NS_LOAD_"), add = TRUE)
        ## run the load hook
        runHook(".onLoad", env, package.lib, package)

        ## process exports, seal, and clear on.exit action
        exports <- nsInfo$exports

        for (p in nsInfo$exportPatterns)
            exports <- c(ls(env, pattern = p, all.names = TRUE), exports)
        ##
        if(.isMethodsDispatchOn() && methods:::.hasS4MetaData(ns) &&
                !identical(package, "methods") ) {
            ## cache generics, classes in this namespace (but not methods itself,
            ## which pre-cached at install time
            methods:::cacheMetaData(ns, TRUE, ns)
            ## load actions may have added objects matching patterns
            for (p in nsInfo$exportPatterns) {
                expp <- ls(ns, pattern = p, all.names = TRUE)
                newEx <- !(expp %in% exports)
                if(any(newEx))
                    exports <- c(expp[newEx], exports)
            }
            ## process class definition objects
            expClasses <- nsInfo$exportClasses
            ##we take any pattern, but check to see if the matches are classes
            pClasses <- character()
            aClasses <- methods:::getClasses(ns)
            classPatterns <- nsInfo$exportClassPatterns
            ## defaults to exportPatterns
            if(!length(classPatterns))
                classPatterns <- nsInfo$exportPatterns
            for (p in classPatterns) {
                pClasses <- c(aClasses[grep(p, aClasses)], pClasses)
            }
            pClasses <- unique(pClasses)
            if( length(pClasses) ) {
                good <- vapply(pClasses, methods:::isClass, NA, where = ns)
                if( !any(good) && length(nsInfo$exportClassPatterns))
                    warning(gettextf("'exportClassPattern' specified in 'NAMESPACE' but no matching classes in package %s", sQuote(package)),
                            call. = FALSE, domain = NA)
                expClasses <- c(expClasses, pClasses[good])
            }
            if(length(expClasses)) {
                missingClasses <-
                        !vapply(expClasses, methods:::isClass, NA, where = ns)
                if(any(missingClasses))
                    stop(gettextf("in package %s classes %s were specified for export but not defined",
                                    sQuote(package),
                                    paste(expClasses[missingClasses],
                                            collapse = ", ")),
                            domain = NA)
                expClasses <- paste0(methods:::classMetaName(""), expClasses)
            }
            ## process methods metadata explicitly exported or
            ## implied by exporting the generic function.
            allGenerics <- unique(c(methods:::.getGenerics(ns),
                            methods:::.getGenerics(parent.env(ns))))
            expMethods <- nsInfo$exportMethods
            ## check for generic functions corresponding to exported methods
            addGenerics <- expMethods[is.na(match(expMethods, exports))]
            if(length(addGenerics)) {
                nowhere <- sapply(addGenerics, function(what) !exists(what, mode = "function", envir = ns))
                if(any(nowhere)) {
                    warning(gettextf("no function found corresponding to methods exports from %s for: %s",
                                    sQuote(package),
                                    paste(sQuote(sort(unique(addGenerics[nowhere]))), collapse = ", ")),
                            domain = NA, call. = FALSE)
                    addGenerics <- addGenerics[!nowhere]
                }
                if(length(addGenerics)) {
                    ## skip primitives
                    addGenerics <- addGenerics[sapply(addGenerics, function(what) ! is.primitive(get(what, mode = "function", envir = ns)))]
                    ## the rest must be generic functions, implicit or local
                    ## or have been cached via a DEPENDS package
                    ok <- sapply(addGenerics, methods:::.findsGeneric, ns)
                    if(!all(ok)) {
                        bad <- sort(unique(addGenerics[!ok]))
                        msg <-
                                ngettext(length(bad),
                                        "Function found when exporting methods from the namespace %s which is not S4 generic: %s",
                                        "Functions found when exporting methods from the namespace %s which are not S4 generic: %s", domain = "R-base")
                        stop(sprintf(msg, sQuote(package),
                                        paste(sQuote(bad), collapse = ", ")),
                                domain = NA, call. = FALSE)
                    }
                    else if(any(ok > 1L))  #from the cache, don't add
                        addGenerics <- addGenerics[ok < 2L]
                }
                ### <note> Uncomment following to report any local generic functions
                ### that should have been exported explicitly.  But would be reported
                ### whenever the package is loaded, which is not when it is relevant.
                ### </note>
                ## local <- sapply(addGenerics, function(what) identical(as.character(get(what, envir = ns)@package), package))
                ## if(any(local))
                ##     message(gettextf("export(%s) from package %s generated by exportMethods()",
                ##        paste(addGenerics[local], collapse = ", ")),
                ##             domain = NA)
                exports <- c(exports, addGenerics)
            }
            expTables <- character()
            if(length(allGenerics)) {
                expMethods <-
                        unique(c(expMethods,
                                        exports[!is.na(match(exports, allGenerics))]))
                missingMethods <- !(expMethods %in% allGenerics)
                if(any(missingMethods))
                    stop(gettextf("in %s methods for export not found: %s",
                                    sQuote(package),
                                    paste(expMethods[missingMethods],
                                            collapse = ", ")),
                            domain = NA)
                tPrefix <- methods:::.TableMetaPrefix()
                allMethodTables <-
                        unique(c(methods:::.getGenerics(ns, tPrefix),
                                        methods:::.getGenerics(parent.env(ns), tPrefix)))
                needMethods <-
                        (exports %in% allGenerics) & !(exports %in% expMethods)
                if(any(needMethods))
                    expMethods <- c(expMethods, exports[needMethods])
                ## Primitives must have their methods exported as long
                ## as a global table is used in the C code to dispatch them:
                ## The following keeps the exported files consistent with
                ## the internal table.
                pm <- allGenerics[!(allGenerics %in% expMethods)]
                if(length(pm)) {
                    prim <- logical(length(pm))
                    for(i in seq_along(prim)) {
                        f <- methods:::getFunction(pm[[i]], FALSE, FALSE, ns)
                        prim[[i]] <- is.primitive(f)
                    }
                    expMethods <- c(expMethods, pm[prim])
                }
                for(i in seq_along(expMethods)) {
                    mi <- expMethods[[i]]
                    if(!(mi %in% exports) &&
                            exists(mi, envir = ns, mode = "function",
                                    inherits = FALSE))
                        exports <- c(exports, mi)
                    pattern <- paste0(tPrefix, mi, ":")
                    ii <- grep(pattern, allMethodTables, fixed = TRUE)
                    if(length(ii)) {
                        if(length(ii) > 1L) {
                            warning(gettextf("multiple methods tables found for %s",
                                            sQuote(mi)), call. = FALSE, domain = NA)
                            ii <- ii[1L]
                        }
                        expTables[[i]] <- allMethodTables[ii]
                    }
                    else { ## but not possible?
                        warning(gettextf("failed to find metadata object for %s",
                                        sQuote(mi)), call. = FALSE, domain = NA)
                    }
                }
            }
            else if(length(expMethods))
                stop(gettextf("in package %s methods %s were specified for export but not defined",
                                sQuote(package),
                                paste(expMethods, collapse = ", ")),
                        domain = NA)
            exports <- unique(c(exports, expClasses,  expTables))
        }
        ## certain things should never be exported.
        if (length(exports)) {
            stoplist <- c(".__NAMESPACE__.", ".__S3MethodsTable__.",
                    ".packageName", ".First.lib", ".onLoad",
                    ".onAttach", ".conflicts.OK", ".noGenerics")
            exports <- exports[! exports %in% stoplist]
        }
        namespaceExport(ns, exports)
        sealNamespace(ns)
        runUserHook(package, pkgpath)
        on.exit()
        Sys.unsetenv("_R_NS_LOAD_")
        ns
    }
}

