snapshot.init <-
function (dir, referenceRunPredicate, equalityFunction) {
    if (!missing(dir)) {
        refcmpEnv$snaphost_dir <- dir
    }
    if (!missing(referenceRunPredicate)) {
        refcmpEnv$isReferenceRun <- referenceRunPredicate
    }
    if (!missing(equalityFunction)) {
        refcmpEnv$equals <- equalityFunction
    }
    refcmpEnv$snapshot_id <- 0
}
