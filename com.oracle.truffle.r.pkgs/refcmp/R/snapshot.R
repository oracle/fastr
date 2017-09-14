snapshot <-
function (...) {
    actParExprs <- as.list(match.call()[-1])
    valueList <- actParsToList(actParExprs, parent.frame())
    try({
        snapshot.id(refcmpEnv$snapshot_id, valueList)
    })
    refcmpEnv$snapshot_id <- refcmpEnv$snapshot_id + 1
}
