makeLazyLoading <-
		function(package, lib.loc = NULL, compress = TRUE,
				keep.source = getOption("keep.source.pkgs"))
{
	if(!is.logical(compress) && ! compress %in% c(2,3))
		stop("invalid value for 'compress': should be FALSE, TRUE, 2 or 3")
	options(warn = 1L)
	findpack <- function(package, lib.loc) {
		pkgpath <- find.package(package, lib.loc, quiet = TRUE)
		if(!length(pkgpath))
			stop(gettextf("there is no package called '%s'", package),
					domain = NA)
		pkgpath
	}

	if (package == "base")
		stop("this cannot be used for package 'base'")

	loaderFile <- file.path(R.home("share"), "R", "nspackloader.R")
	pkgpath <- findpack(package, lib.loc)
	codeFile <- file.path(pkgpath, "R", package)

	if (!file.exists(codeFile)) {
		warning("package contains no R code")
		return(invisible())
	}
#	if (file.info(codeFile)["size"] == file.info(loaderFile)["size"])
	if (fastr.comparefilesizes(codeFile, loaderFile))
		warning("package seems to be using lazy loading already")
	else {
		code2LazyLoadDB(package, lib.loc = lib.loc,
				keep.source = keep.source, compress = compress)
		file.copy(loaderFile, codeFile, TRUE)
	}

	invisible()
}
