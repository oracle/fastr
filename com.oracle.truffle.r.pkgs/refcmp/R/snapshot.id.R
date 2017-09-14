snapshot.id <-
function (id, valueList, env = parent.frame()) {
    if (refcmpEnv$isReferenceRun()) {
        snapshot.record(id, valueList)
    }
    else {
        snapshot.check(id, valueList)
    }
}
