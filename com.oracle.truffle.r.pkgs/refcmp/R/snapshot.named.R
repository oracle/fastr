snapshot.named <-
function (...) {
    args <- list(...)
    valueList <- args[names(args) != ""]
    try({
        snapshot.id(refcmpEnv$snapshot_id, valueList)
        refcmpEnv$snapshot_id <- refcmpEnv$snapshot_id + 1
    })
}
