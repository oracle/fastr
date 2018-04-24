.onLoad <-
function(libname, pkgname) {
    OPATH <- Sys.getenv("PATH")
    javahome <- if (!is.null(getOption("java.home"))) getOption("java.home") else Sys.getenv("JAVA_HOME")
    if(!nchar(javahome)) { ## JAVA_HOME was not set explicitly
        find.java <- function() {
            for (root in c("HLM", "HCU"))
                for(key in c("Software\\JavaSoft\\Java Runtime Environment",
                             "Software\\JavaSoft\\Java Development Kit")) {
                  hive <- try(utils::readRegistry(key, root, 2), silent=TRUE)
                  if (!inherits(hive, "try-error")) return(hive)
                }
            hive
        }
        hive <- find.java()
        if (inherits(hive, "try-error"))
            stop("JAVA_HOME cannot be determined from the Registry")
        if (!length(hive$CurrentVersion))
            stop("No CurrentVersion entry in Software/JavaSoft registry! Try re-installing Java and make sure R and Java have matching architectures.")
        this <- hive[[hive$CurrentVersion]]
        javahome <- this$JavaHome
        paths <- dirname(this$RuntimeLib) # wrong on 64-bit
    } else paths <- character()
    if(is.null(javahome) || !length(javahome) || !nchar(javahome))
        stop("JAVA_HOME is not set and could not be determined from the registry")
    #else cat("using JAVA_HOME =", javahome, "\n")

    ## we need to add Java-related library paths to PATH
    curPath <- OPATH
    paths <- c(paths,
               file.path(javahome, "bin", "client"), # 32-bit
               file.path(javahome, "bin", "server"), # 64-bit
               file.path(javahome, "bin"), # base (now needed for MSVCRT in recent Sun Java)
               file.path(javahome, "jre", "bin", "server"), # old 64-bit (or manual JAVA_HOME setting to JDK)
               file.path(javahome, "jre", "bin", "client")) # old 32-bit (or manual JAVA_HOME setting to JDK)
    cpc <- strsplit(curPath, ";", fixed=TRUE)[[1]] ## split it up so we can check presence/absence of a path

    ## add paths only if they are not in already and they exist
    for (path in unique(paths))
        if (!path %in% cpc && file.exists(path)) curPath <- paste(path, curPath, sep=";")

    ## set PATH only if it's not correct already (cannot use identical/isTRUE because of PATH name attribute)
    if (curPath != OPATH) {
      Sys.setenv(PATH = curPath)
      # check the resulting PATH - if they don't match then Windows has truncated it
      if (curPath != Sys.getenv("PATH"))
        warning("*** WARNING: your Windows system seems to suffer from truncated PATH bug which will likely prevent rJava from loading.\n      Either reduce your PATH or read http://support.microsoft.com/kb/906469 on how to fix your system.")
    }

    library.dynam("rJava", pkgname, libname)
    Sys.setenv(PATH = OPATH)
    .jfirst(libname, pkgname)
}
