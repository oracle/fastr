snapshot.check <-
function (id, valueList) {
    fcon <- file(file.path(refcmpEnv$snapshot_dir, paste0("snapshot", 
        id, ".obj")))
    restoredVars <- readRDS(file = fcon)
    close(fcon)
    if (length(restoredVars) < length(valueList)) {
        stop(paste("recorded snapshot has", length(restoredVars), 
            "recorded variables but expected", length(valueList)))
    }
    var_names <- names(valueList)
    restored_names <- names(restoredVars)
    for (i in seq_along(var_names)) {
        if (var_names[[i]] %in% restored_names) {
            actualVal <- valueList[[var_names[[i]]]]
            expectedVal <- restoredVars[[var_names[[i]]]]
            if (!refcmpEnv$equals(expectedVal, actualVal)) {
                stop(paste0("Value of variable '", var_names[[
                  i]], "' differs. Expected ", expectedVal, " but was ", 
                  actualVal))
            }
        }
        else {
            stop(paste0("Missing variable '", var_names[[i]], 
                "' in recorded variables"))
        }
    }
}
