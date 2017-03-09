
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

###################################################
# Helper functions to deal with null and grob units
# these functions are invoked from Java directly

# Should be in sync with constants in Unit java class
L_GROBX <- 19
L_GROBY <- 20
L_GROBWIDTH <- 21
L_GROBHEIGHT <- 22
L_GROBASCENT <- 23
L_GROBDESCENT <- 24

indexMod <- function(i, mod) ((i - 1) %% mod) + 1

# if the grob is gPath, use it to find an actual grob
# savedgrob - the grob from grid context
findGrob <- function(grob, savedgrob) {
    if (inherits(grob, "gPath")) {
        if (is.null(savedgrob)) {
            return(grid:::findGrobinDL(grob$name))
        } else {
            return(grid:::findGrobinChildren(grob$name, savedgrob$children))
        }
    }
    grob
}

# this is called from FastR, it is simpler to implement this whole function in R.
# GnuR uses series of install -> lang2 -> eval calls to achieve this from C.
isPureNullUnit <- function(unit, index) {
    if (inherits(unit, "unit.arithmetic")) {
        return(isPureNullUnitArithmetic(unit, index));
    } else if (inherits(unit, "unit.list")) {
        return(isPureNullUnit(unit[[indexMod(index, length(unit))]], 1))
    }
    unitId <- attr(unit, "valid.unit")
    if (unitId == L_GROBWIDTH) {
        return(isPureNullUnitGrobDim(unit, index, grid:::width))
    } else if (unitId == L_GROBHEIGHT) {
        return(isPureNullUnitGrobDim(unit, index, grid:::height))
    }
    unitId == 5 # L_NULL
}

getUnitData <- function(unit, index) {
    result <- attr(unit, "data")
    if (!is.list(result)) {
        return(result)
    }
    result[[indexMod(index, length(result))]]
}

isPureNullUnitGrobDim <- function(unit, index, dimFunction) {
    # Can a grob have "null" width/height?
    # to be sure we cover everything, we keep the check here (like in GnuR)
    savedgpar <- .Call(grid:::L_getGPar)
    savedgrob <- .Call(grid:::L_getCurrentGrob)

    grob <- findGrob(getUnitData(unit, index), savedgrob)

    updatedgrob <- grid:::preDraw(grob)
    result <- isPureNullUnit(dimFunction(updatedgrob), 1)
    grid:::postDraw(updatedgrob)

    .Call(grid:::L_setGPar, savedgpar)
    .Call(grid:::L_setCurrentGrob, savedgrob)
    result
}

isPureNullUnitArithmetic <- function(x, index) {
    if (x$fname %in% c('+', '-')) {
        # can this ever happen when Ops.unit raises error for two null units added/subtracted?
        # to be sure we cover everything, we keep the check here (like in GnuR)
        return(isPureNullUnit(x$arg1, index) && isPureNullUnit(x$arg2, index))
    } else if (x$fname == '*') {
        return(isPureNullUnit(x$arg2, index))
    } else if (x$fname %in% c('min', 'max', 'sum')) {
        return(all(sapply(seq_along(x$arg1), function(i) isPureNullUnit(x$arg1, i))))
    } else {
        error("unimplemented unit function");
    }
}

# tests:
# isPureNullUnit(grid:::unit.list(unit(c(1,2,3),c('mm', 'cm', 'null'))), 1) == FALSE
# isPureNullUnit(grid:::unit.list(unit(c(1,2,3),c('mm', 'cm', 'null'))), 3) == TRUE
# isPureNullUnit(3*unit(1,'mm'), 2) == FALSE
# isPureNullUnit(3*unit(1,'null'), 2) == TRUE
# isPureNullUnit(min(unit(1,'null')), 1) == TRUE
# { gt <- grid.text("Hi there"); isPureNullUnit(unit(1, "grobwidth", gt), 1) } == FALSE
# { gt <- grid.text("Hi there"); isPureNullUnit(unit(1, "grobheight", gt), 1) } == FALSE

grobConversionPreDraw <- function(grobIn) {
    grob <- findGrob(grobIn, .Call(grid:::L_getCurrentGrob))
    grid:::preDraw(grob)
}

grobConversionGetUnitXY <- function(grob, unitId) {
    if (unitId == L_GROBX || unitId == L_GROBY) {
        return(list(grid:::xDetails(grob), grid:::yDetails(grob)))
    } else if (unitId == L_GROBWIDTH) {
        return(list(grid:::width(grob)))
    } else if (unitId == L_GROBHEIGHT) {
        return(list(grid:::height(grob)))
    } else if (unitId == L_GROBDESCENT) {
        return(list(grid:::descentDetails(grob)))
    } else if (unitId == L_GROBASCENT) {
        return(list(grid:::ascentDetails(grob)))
    }
    error("grobConversionGetUnitXY: not a grob unit.")
}

grobConversionPostDraw <- function(grob) {
    grid:::postDraw(grob)
}
