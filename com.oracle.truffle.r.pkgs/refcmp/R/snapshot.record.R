snapshot.record <-
function (id, valueList) {
    dumpDir <- file.path(refcmpEnv$snapshot_dir)
    if (!dir.exists(dumpDir)) {
        dir.create(dumpDir)
    }
    fcon <- file(file.path(refcmpEnv$snapshot_dir, paste0("snapshot", 
        id, ".obj")))
    saveRDS(valueList, file = fcon)
    close(fcon)
}
