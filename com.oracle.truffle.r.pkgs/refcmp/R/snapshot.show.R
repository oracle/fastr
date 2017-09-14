snapshot.show <-
function (id = refcmpEnv$snapshot_id) {
    snapshot_filename <- file.path(refcmpEnv$snapshot_dir, paste0(
        "snapshot", id, ".obj"))
    if (!file.exists(snapshot_filename)) {
        stop(paste0("Snapshot with ID=", id, " does not exist"))
    }
    fcon <- file(snapshot_filename)
    restoredVars <- readRDS(file = fcon)
    close(fcon)
    return(restoredVars)
}
