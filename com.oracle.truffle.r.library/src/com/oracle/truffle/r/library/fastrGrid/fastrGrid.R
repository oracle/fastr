
# Returns list with elements [[1]] - depth, zero if not found, [[2]] - the viewport, NULL if not found
find.viewport <- function(name, strict, pvp, depth) {
    if (length(ls(env=pvp$children)) == 0) {
        return(list(FALSE, NULL))
    } else if (exists(name, env=pvp$children, inherits=FALSE)) {
        return(list(depth, get(name, env=pvp$children, inherits=FALSE)))
    } else if (strict) {
        return(list(FALSE, NULL))
    } else {
        return(find.in.children(name, pvp$children, depth + 1L))
    }
}

# Note: in GnuR this takes "strict" from find.viewport and forwards it to recursive calls to find.viewport,
# however, strict must be constant FALSE if find.in.children is called, so we leave it out.
find.in.children <- function(name, children, depth) {
  cpvps <- ls(env=children)
  ncpvp <- length(cpvps)
  count <- 0L
  found <- FALSE
  while (count < ncpvp && !found) {
    result <- find.viewport(name, FALSE, get(cpvps[count + 1L], env=children), depth)
    if (result[[1L]]) {
        return(result);
    }
    count <- count + 1L
  }
  list(FALSE, NULL) # not found
}

L_downviewport <- function(name, strict) {
    currVp <- .Call(grid:::L_currentViewport)
    result <- find.viewport(name, strict, currVp, 1L);
    if (result[[1]]) {
        .Internal(.fastr.grid.doSetViewPort(result[[2L]], FALSE, FALSE));
        return(result[[1L]])
    } else {
        stop(paste0("Viewport '", name, "' was not found"));
    }
}

L_setviewport <- function(vp, hasParent) {
    pushedVP <- grid:::pushedvp(vp);
    .Internal(.fastr.grid.doSetViewPort(pushedVP, hasParent, TRUE));
}