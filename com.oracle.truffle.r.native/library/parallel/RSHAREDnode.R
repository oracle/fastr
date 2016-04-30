makeSHAREDmaster <- function(key) {
    channel <- .fastr.channel.get(as.integer(key))
    structure(list(channel=channel), class = "SHAREDnode")
}

local({
    port <- ""
    outfile <- Sys.getenv("R_SNOW_OUTFILE") ##**** defaults to ""; document

    args <- commandArgs()
    pos <- match("--args", args)
    args <- args[-(1 : pos)]
    for (a in args) {
        pos <- regexpr("=", a)
        name <- substr(a, 1, pos - 1)
        value <- substr(a,pos + 1, nchar(a))
        switch(name,
               PORT = port <- value,
               OUT = outfile <- value)
    }

    if (port == "") port <- getClusterOption("port")

    parallel:::sinkWorkerOutput(outfile)
    parallel:::slaveLoop(makeSHAREDmaster(port))
})
