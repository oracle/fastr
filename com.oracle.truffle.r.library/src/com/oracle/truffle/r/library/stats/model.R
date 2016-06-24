#
# This material is distributed under the GNU General Public License
# Version 2. You may review the terms of this license at
# http://www.gnu.org/licenses/gpl-2.0.html
#
# Copyright (c) 1995, 1996  Robert Gentleman and Ross Ihaka
# Copyright (c) 1997-2013,  The R Core Team
# Copyright (c) 2016, Oracle and/or its affiliates
#
# All rights reserved.
#

#
# This file defines FastR counterparts of C functions defined in model.c in GnuR
# These functions are called using .External, in FastR hand-picked
# functions invoked through .External are routed to R functions defined
# in this file (and possibly others in the future). Public functions intended 
# to be called through external are labeled with comment 'PUBLIC'.
#
# The structure of the implementation somewhat reflects GnuR so that
# it is easier to update this code, should the code in GnuR be changed.
#

# ================================================================
# R reimplementations of C utility functions 
# N.B. some of them work differently than their R counterparts

error <- function(message) {
    stop(simpleError(message))
}

isLanguage <- function(x) {
    is.null(x) || typeof(x) == "language"
}

nrows <- function(x) {
    plain <- unclass(x) # this is necessary because of 'AsIs' class: e.g. I(var+4)
    if (is.factor(x) || is.vector(plain) || is.list(plain) || is.matrix(plain)) {
        dims <- dim(plain);
        if (is.null(dims)) {
            return(length(plain))
        }
        return(dims[[1L]]);
    } else if (is.data.frame(x)) {
        nrows(x[[1L]])
    }
    error("object is not a matrix")
}

ncols <- function(x) {
    plain <- unclass(x)
    if (is.factor(x) || is.vector(plain) || is.list(plain) || is.matrix(plain)) {
        dims <- dim(plain);
        if (is.null(dims)) {
            return(1L);
        }
        if (length(dims) >= 2L) {
            return(dims[[2L]])
        }
        return(1L) # 1D or array
    }  else if (is.data.frame(x)) {
        return(length(x));
    }
    error("object is not a matrix")
}

# ================================================================
# implementation of termsform

# Global variable that are used to transfer state between methods this reflects GnuR implementation
intercept <- FALSE;
parity <- FALSE;
response <- FALSE;
nvar <- 0L;
nterm <- 0L;
varlist <- list()
framenames <- NULL
haveDot = FALSE

isZeroOne <- function(x) {
    if (is.numeric(x)) {
        x == 0 || x == 1
    } else {
        FALSE
    }
}

MatchVar <- function(var1, var2) {
    if (is.vector(var1) && is.vector(var2) && var1 == var2) {
        return(TRUE)
    } else if (is.null(var1) && is.null(var2)) {
        return(TRUE)
    } else if (is.null(var1) || is.null(var2)) {
        return(FALSE)
    } else if (is.symbol(var1) || is.symbol(var2)) {
        return(identical(var1, var2))
    } else if ((is.list(var1) || isLanguage(var1))
               && (is.list(var2) || isLanguage(var2))) {
        if (length(var1) > length(var2)) {
            maxVar <- var1
        } else {
            maxVar <- var2
        }
        
        for (i in seq_along(maxVar)) {
            if (!MatchVar(var1[[i]], var2[[i]])) {
                return(FALSE)
            }
        }
        return(TRUE)
    }  else {
        return(FALSE)
    }
}

#InstallVar locates a ``variable'' in the model variable list;
#adding it to the global varlist if not found. 
InstallVar <- function(var) {
    # Check that variable is legitimate
    if (!is.symbol(var) && !isLanguage(var) && !isZeroOne(var)) {
        error("invalid term in model formula")
    }
    # Lookup/Install it
    index <- 0L
    for (v in varlist) {
        index <- index + 1L
        if (MatchVar(var, v)) {
            return(index)
        }
    }
    varlist <<- c(varlist, var)
    
    return(index + 1L)
}

# ExtractVars recursively extracts the variables
# in a model formula.  It calls InstallVar to do
# the installation.  The code takes care of unary/
#    + and minus.  No checks are made of the other
# ``binary'' operators.  Maybe there should be some. 
ExtractVars <- function (formula, checkonly=FALSE) {
    if (is.null(formula) || isZeroOne(formula)) { return() }
    
    v <- NULL
    if (is.symbol(formula)) {
        haveDot <- identical(formula, quote(`.`))
        if (!checkonly) {
            if (identical(formula, quote(`.`)) && !is.null(framenames)) {
                for (framename in framenames) {
                    if (!MatchVar(framename, varlist)) {
                        InstallVar(framename)
                    }
                }
            } else  {
                InstallVar(formula)
            }
        }
    } else if (isLanguage(formula)) {
        # note: several malformed cases cannot even make it to terms.formula, 
        # hence do not have to be checked here, e.g. terms.formula(y~z*)
        length <- length(formula)
        op <- formula[[1]]
        if (identical(op, quote(`~`))) { # tilde
            if (response) {
                error("invalid model formula")
            } else if (is.null(formula[[3]])) {
                response <<- FALSE
                ExtractVars(formula[[2]], FALSE)
            } else {
                response <<- TRUE
                InstallVar(formula[[2]])
                ExtractVars(formula[[3]], FALSE)
            }
        } else if (identical(op, quote(`+`))) {
            if (length(formula) > 1L) { 
                ExtractVars(formula[[2]], checkonly) 
            }
            if (length(formula) > 2L) { 
                ExtractVars(formula[[3]], checkonly) 
            }
        } else if (identical(op, quote(`:`)) ||
                    identical(op, quote(`*`)) ||
                    identical(op, quote(`%in%`)) ||
                    identical(op, quote(`/`))) {
            ExtractVars(formula[[2]], checkonly)
            ExtractVars(formula[[3]], checkonly)
        } else if (identical(op, quote(`^`))) {
            if (!is.numeric(formula[[3]])) {
                error("invalid power in formula");
            }
            ExtractVars(formula[[2]], checkonly)
        } else if (identical(op, quote(`-`))) {
            if (length == 2) {
                ExtractVars(formula[[2]], TRUE)
            } else {
                ExtractVars(formula[[2]], checkonly)
                ExtractVars(formula[[3]], TRUE)
            }
        } else if (identical(op, quote(`(`))) {
            ExtractVars(formula[[2]], checkonly)
        } else {
            InstallVar(formula)
        }
    } else {
        error("invalid model formula in ExtractVars")
    }
}

# If there is a dotsxp being expanded then we need to see
# whether any of the variables in the data frame match with
# the variable on the lhs. If so they shouldn't be included
# in the factors 
CheckRHS <- function (v) {
    if (is.list(v) || isLanguage(v)) {
        for (e in v) {
            CheckRHS(e)
        }        
    }
    if (is.symbol(v)) {
        for (i in seq_along(framenames)) {
            framename <- framenames[[i]];
            # TODO is this check good enough?
            # its a raw check in GNUR
            if (identical(framename, v))  {
                framenames <<- framenames[[-i]]
            }
        }
    }
}
AllocTerm <- function()  {
    logical(nvar)
}
OrBits <- function(term1, term2) {
    term1 | term2
}
BitCount <- function(term) {
    sum(term)   
}
TermZero <- function(term) {
    sum(term) == 0L
}
TermEqual <- function(term1, term2) {
    identical(term1, term2)
}
StripTerm <- function(term, list) {
    if (TermZero(term)) {
        intercept <<- FALSE
    }
    filter <- mapply(function(otherTerm) {TermEqual(term, otherTerm)}, list)
    return (list[filter])
}

# TrimRepeats removes duplicates of (bit string) terms 
# in a model formula by repeated use of ``StripTerm''.
# Also drops zero terms. 
    
TrimRepeats <- function(list) {
    if (length(list) == 0) list else unique(list)
}

# ====================================================

# PlusTerms expands ``left'' and ``right'' and 
# concatenates their terms (removing duplicates). 
PlusTerms <- function(left, right) {
    left <- EncodeVars(left)
    right <- EncodeVars(right)
    return (TrimRepeats(c(left, right)))
}

# InteractTerms expands ``left'' and ``right'' 
# and forms a new list of terms containing the bitwise 
# OR of each term in ``left'' with each term in ``right''. 
InteractTerms <- function(left, right) {
    left <- EncodeVars(left)
    right <- EncodeVars(right)
    
    term <- vector("list", length(left) * length(right))
    index <- 1L
    for (leftTerm in left) {
        for (rightTerm in right) {
            term[[index]] <- OrBits(leftTerm, rightTerm)
            index <- index + 1L
        }
    }
    return (TrimRepeats(term))
}
 

# CrossTerms expands ``left'' and ``right'' 
# and forms the ``cross'' of the list of terms.  
# Duplicates are removed. 
CrossTerms <- function(left, right) {
    left <- EncodeVars(left)
    right <- EncodeVars(right)

    term <- vector("list", length(left) * length(right))
    index <- 1L
    for (leftTerm in left) {
        for (rightTerm in right) {
            term[[index]] <- OrBits(leftTerm, rightTerm)
            index <- index + 1L
        }
    }
    
    return (TrimRepeats(c(left, right, term)))
}


# PowerTerms expands the ``left'' form and then 
# raises it to the power specified by the right term. 
# Allocation here is wasteful, but so what ... 
PowerTerms <- function(left, right) {
    
    ip <- as.integer(right)
    if (is.na(ip) || ip <= 1) {
        error("invalid power in formula")
    }
    
    left <- EncodeVars(left)
    term <- NULL
    right <- left
    for (power in 1L:(ip-1L)) {
        term <- vector("list", length(left) * length(right))
        index <- 1L
        for (leftTerm in left) {
            for (rightTerm in right) {
                term[[index]] <- OrBits(leftTerm, rightTerm)
                index <- index + 1L
            }
        }
        # note: TrimRepeats in GnuR alters its argument
        right <- term <- TrimRepeats(term)
    }
    return(term)
}

# InTerms expands ``left'' and ``right'' and 
# forms the ``nest'' of the the left in the 
# interaction of the right 
InTerms <- function(left, right) {
    
    left <- EncodeVars(left)
    right <- EncodeVars(right)
    
    term <- AllocTerm()
    for (rightTerm in right) {
        term <- OrBits(term, rightTerm)
    }
    
    index <- 1L
    for (leftTerm in left) {
        left[[index]] <- OrBits(term, leftTerm)
        index <- index + 1L
    }
    return (TrimRepeats(left))
}

# NestTerms expands ``left'' and ``right'' 
# and forms the ``nest'' of the list of terms.  
# Duplicates are removed. 
NestTerms <- function(left, right) {
    
    left <- EncodeVars(left)
    right <- EncodeVars(right)
    term <- AllocTerm()
    for (i in length(left)) {
        term <- OrBits(term, left[[i]])
    }
    for (i in length(right)) {
        right[[i]] <- OrBits(term, right[[i]])
    }
    return (TrimRepeats(c(left, right)))
}

# DeleteTerms expands ``left'' and ``right'' 
# and then removes any terms which appear in 
# ``right'' from ``left''. 

DeleteTerms <- function(left, right) {
    left <- EncodeVars(left)
    parity <<- !parity
    right <- EncodeVars(right)
    parity <<- !parity

    for (rightTerm in right) {
        left <- StripTerm(rightTerm, left)
    }
    return (left)
}


# EncodeVars performs  model expansion and bit string encoding. 
# This is the real workhorse of model expansion. 
EncodeVars <- function(formula) {
    if (is.null(formula)) {
        return(NULL)
    } else if (is.numeric(formula) && formula == 1L) {
        intercept <<- parity
        return(NULL)
    } else if (is.numeric(formula) && formula == 0L) {
        intercept <<- !parity
        return(NULL)
    } else if (is.symbol(formula)) {
        if (identical(formula, quote(`.`)) && !is.null(framenames)) {
            error("termsform: not implemented when formula='.' and there are framenames")
        } else {
            term <- AllocTerm()
            term[[InstallVar(formula)]] <- TRUE
            return(list(term))
        }
    } else if (isLanguage(formula)) {
        length <- length(formula)
        op <- formula[[1]]
        if (identical(op, quote(`~`))) {
            if (length == 2L) {
                return (EncodeVars(formula[[2]]))
            } else {
                return (EncodeVars(formula[[3]]))
            }
        } else if (identical(op, quote(`+`))) {
            if (length == 2L) {
                return (EncodeVars(formula[[2]]))
            } else {
                return (PlusTerms(formula[[2]], formula[[3]]))
            }
        } else if (identical(op, quote(`:`))) {
            return (InteractTerms(formula[[2]], formula[[3]]))
        } else if (identical(op, quote(`*`))) {
            return (CrossTerms(formula[[2]], formula[[3]]))
        } else if (identical(op, quote(`%in%`))) {
            return (InTerms(formula[[2]], formula[[3]]))
        } else if (identical(op, quote(`/`))) {
            return (NestTerms(formula[[2]], formula[[3]]))
        } else if (identical(op, quote(`^`))) {
            return (PowerTerms(formula[[2]], formula[[3]]))
        } else if (identical(op, quote(`-`))) {
            if (length == 2L) {
                return (DeleteTerms(NULL, formula[[2]]))
            }
            return (DeleteTerms(formula[[2]], formula[[3]]))
        } else if (identical(op, quote(`(`))) {
            return (EncodeVars(formula[[2]]))
        } else {
            term <- AllocTerm()
            term[[InstallVar(formula)]] <- TRUE
            return (list(term))
        }
    } else {
        error("invalid model formula in EncodeVars")
    }
}

# TermCode decides on the encoding of a model term.
# Returns 1 if variable ``whichBit'' in ``thisTerm''
# is to be encoded by contrasts and 2 if it is to be 
# encoded by dummy variables.  This is decided using
# the heuristic described in Statistical Models in S, page 38.
TermCode <- function(formula, callIdx, varIndex) {
    # Note: because of FastR error to be fixed: list items not copied by value
    # should be call <- formula[callIdx], but instead:
    call <- logical(length(formula[[callIdx]]))
    call[which(formula[[callIdx]])] <- TRUE
    
    call[varIndex] <- FALSE
  
    # Search preceding terms for a match - when the 
    # preceeding term does not contain a variable that 
    # our 'call' does contain.
    # Zero is a possibility - it is a special case
    if (!any(call)) { return (1L); }
    
    for (i in seq(1, length = callIdx-1)) {
        if (!any(!formula[[i]] & call)) {
            return (1L)
        }
    }
    
    return(2L);
}


# PUBLIC: termsform
#
# This function adds attributes to the formula x.
#
# Example attributes for termsform(z ~ y, NULL, environment(), FALSE, FALSE)
#
# "variables" - list(z,y) (typeof: language)
# "factors" - data.frame:
#       y
#    z  0
#    y  1
# "term.labels": y
# "order", "intercept", "response": 1
termsform <- function (x, specials, data, keep.order, allowDotAsName) {
    
    if (!isLanguage(x)
        || !identical(x[[1]], quote(`~`))
        || length(x) != 2L && length(x) != 3L) {
        error("argument is not a valid model");
    }
    
    if (length(specials) > 0L && !is.character(specials)) {
        error("'specials' must be NULL or a character vector")
    }
    a <- data;
    if (is.null(data) || is.environment(data)) {
        framenames <<- NULL
    } else if(is.data.frame(data)) {
        framenames <<- names(data)
    } else {
        error("'data' argument is of the wrong type")
    }
    
    hadFrameNames <- FALSE
    if (!is.null(framenames)) {
        if (length(framenames) > 0L) {
            hadFrameNames = TRUE
        }
        if (length(x) == 3L) {
            CheckRHS(x[[2]])
        }
    }
    keep.order <- as.logical(keep.order)
    if (is.na(keep.order)) {
        keep.order = FALSE;
    }
    allowDotAsName <- as.logical(allowDotAsName)
    if (is.na(allowDotAsName)) {
        allowDotAsName = FALSE;
    }
    
    # Step 1: Determine the ``variables'' in the model 
    # Here we create an expression of the form 
    # list(...).  You can evaluate it to get 
    # the model variables or use substitute and then 
    # pull the result apart to get the variable names. 
    intercept <<- TRUE
    parity <<- TRUE
    response <<- FALSE
    varlist <<- list()
    ExtractVars(x)
    vars <- quote(list())
    vars[2:(length(varlist) + 1L)] <- varlist
    attr(x, "variables") <- vars
    
    # Note: GnuR uses bitvector of integers and variable nwords to denote its size, we do not need that
    # EncodeVars may have stretched varlist becuase it is a global variable (to reflect GnuR's implementation) 
    nvar <<- length(varlist) 
    
    formula <- EncodeVars(x)
    
    # EncodeVars may have stretched the varlist global variable
    nvar <<- length(varlist)
    
    # Step 2a: Compute variable names 
    varnames <- vapply(varlist, function(i) { deparse(i, nlines=1) }, "")
    
    # Step 2b: Find and remove any offset(s) 
    
    # first see if any of the variables are offsets
    k <- sum(substr(varnames, 0, 7) == "offset(")
    if (k > 0L) {
        offsets <- integer(k)
        # TODO remove the offset terms from the formula
        error("termsform: not implemented - remove the offset terms from formula")
        attr(x, "offset") <- offsets
    }
    
    nterm <<- length(formula);
    
    # Step 3: Reorder the model terms by BitCount, otherwise
    # preserving their order. 
    sCounts <- vapply(formula, BitCount, 0L)
    bitmax <- max(sCounts)

    if (keep.order) {
        ord <- sCounts;
    } else {
      pattern <- formula # save original formula
      callIdx <- 1L # on the top of the two loop below, we iterate through formula. In GnuR this is done with CDR
      ord <- integer(nterm)
      for (i in 0:bitmax) {
        for (n in 1:nterm) {
          if (sCounts[[n]] == i) {
            formula[[callIdx]] <- pattern[[n]]
            ord[[callIdx]] <- i
            callIdx <- callIdx + 1L
          }
        }
      }
    }
    
    # Step 4: Compute the factor pattern for the model. 
    # 0 - the variable does not appear in this term. 
    # 1 - code the variable by contrasts in this term. 
    # 2 - code the variable by indicators in this term.
    # note: we set the attribute "factors" later on in step 5
    pattern <- integer(0)
    if (nterm > 0L) {
        pattern <- matrix(integer(nvar * nterm), nvar, nterm)
        for (n in 1:nterm) {
            for (i in 1:nvar) {
                if (formula[[n]][[i]]) {
                    pattern[[i, n]] <- TermCode(formula, n, i)
                }
            }
        }
    }
    
    # Step 5: Compute term labels
    termlabs <- vapply(formula, function(the.call) {
      # join all the var names (flag set to TRUE) in given call
      paste(varnames[which(the.call)], collapse = ":")
    }, "");
    
    if (nterm > 0L) {
      dimnames(pattern) <- list(varnames, termlabs)
    }
    attr(x, "term.labels") <- termlabs
    attr(x, "factors") <- pattern
    
    if (!is.null(specials)) {
      # TODO -- if there are specials stick them in here
      error("termsform: not implemented when !is.null(specials)")
    }
    
    # Step 6: Fix up the formula by substituting for dot, which should be
    # the framenames joined by +
    if (haveDot) {
      # TODO
      error("termsform: not implemented when haveDot")
    }
    
    attr(x, "order") <- ord
    attr(x, "intercept") <- as.integer(intercept)
    attr(x, "response") <- as.integer(response)
    class(x) <- c("terms", "formula")
    return(x)
}


# =============================================================
# Implementation of modelframe

isValidFrameType <- function(ans) {
    is.factor(ans) || is.logical(ans) || is.integer(ans) || is.double(ans) || is.complex(ans) || is.character(ans) || is.raw(ans)
}

# PUBLIC: model.frame
#
# The argument "formula" contains the terms object generated from the
# model formula (note: termsform function above).  We first evaluate 
# the "variables" attribute of "formula" in the "data" environment.  
# This gives us a list of basic variables to be in the model frame.  
# We do some basic sanity checks on these to ensure that resulting 
# object make sense.
#
# The argument "dots" gives additional things like "weights", "offsets"
# and "subset" which will also go into the model frame so that they can
# be treated in parallel.
#
# Next we subset the data frame according to "subset" and finally apply
# "na.action" to get the final data frame.
#
# Note that the "terms" argument is glued to the model frame as an
# attribute.  Code downstream appears to need this.
#
modelframe <- function(formula, rownames, variables, varnames, dots, dotnames, subset, na.action) {
    
    # argument sanity checks
    if (!is.list(variables)) {
        error("invalid variables")
    }
    if (!is.character(varnames)) {
        error("invalid variable names");
    }
    nvars <- length(variables)
    if (nvars != length(varnames)) {
        error("number of variables != number of variable names")
    }
    ndots <- length(dots)
    if (ndots != length(dotnames)) {
        error("number of variables != number of variable names")
    }
    if (ndots > 0L && !is.character(dotnames)) {
        error("invalid extra variable names")
    }
    
    # dots may contain NULLs, we ignore those in some cases
    # note: lists and NULLs do not support things like list[!is.na(list)] 
    # so we use low level loops
    nactualdots <- 0L
    for (x in dots) {
        if (!is.null(x)) {
            nactualdots <- nactualdots + 1L
        }
    }
    
    # either branch of the following if sets data and dataNames, the else branch is a 'fast-path'
    # it is also simpler to handle the two separate cases, because 1:0 (ndots) 
    # does not work as one may expect in R...
    if (nactualdots > 0L) {
        data <- vector("list", nvar + nactualdots)
        dataNames <- character(nvar + nactualdots)
        data[1:nvar] <- variables
        dataNames[1:nvar] <- varnames
        j <- nvar + 1L
        for (i in 1:ndots) {
            if (is.null(dots[[i]])) {
                next;
            }
            
            ss <- dotnames[[i]]
            if (nchar(ss) + 3L > 256L) {
                # buffer size is not really issue in our case, but to stay compliant
                error(paste0("overlong names in '", ss, "'"))
            }
            
            data[[j]] <- dots[[i]]
            dataNames[[j]] <- paste("(", ss, ")")
            j <- j + 1L
        }
    } else {
        data <- variables
        dataNames <- varnames
    }
    
    names(data) <- dataNames
    
    # Note, the following steps up to running na.action could be simplified to: 
    # data <- data.frame(data, row.names=rownames, check.rows=TRUE)
    # we do not do that to stay close to the C implementation
    
    # Sanity checks to ensure that the answer can become a data frame
    # Each list item has the same 'nrow' and is of a supported type
    nc <- length(data)
    if (nc > 0L) {
        nr <- nrows(data[[1L]])
        for (i in 1:length(data)) {
            ans <- data[[i]]
            if (!isValidFrameType(ans)) {
                error(paste0("invalid type '", typeof(ans), "' for variable '", dataNames[[i]], "'"))
            }
            if (nr != nrows(ans)) {
                error(paste0("variable lengths differ (found for '", dataNames[[i]], "')"))
            }
        }
    } else {
        nr <- length(rownames)
    }
    
    # Turn the data "list" into a "data.frame"
    # so that subsetting methods will work.
    # We must attach "class" and "row.names"
    class(data) <- "data.frame"
    if (length(rownames) == nr) {
        attr(data, "row.names") <- rownames
    } else {
        attr(data, "row.names") <- 1:nr
    }
    
    # Do the subsetting, if required.
    if (!is.null(subset)) {
        data <- data[subset,,drop=FALSE]
    }
    
    # finally, we run na.action on the data frame
    # usually, this will be na.omit
    attr(data, "terms") <- formula
    if (!is.null(na.action)) {
        ndata <- length(data)
        if (is.character(na.action) && nchar(na.action) > 0) {
            # FastR cannot handle just a symbol in parse, it must be language
            # this does not work: na.action <- eval(parse(text = na.action))
            data <- eval(parse(text = paste0(na.action, "(data)")))
        } else {
            data <- na.action(data)
        }
        if (ndata != length(data) || !is.list(data)) {
            error("invalid result from na.action");
        }
        
        # We do not need to transfer attributes here in R they should be preserved.
        # TODO/Note: we might have to delte dim and tsp...
    }
    
    data
}

# =============================================================
# Implementation of modelmatrix

isUnorderedInt <- function(x) {
    is.integer(x) && inherits(x, "factor") && !inherits(x, "ordered")
}

isOrderedInt <- function(x) {
    is.integer(x) && inherits(x, "factor") && inherits(x, "ordered")
}

ColumnNames <- function (x) {
    dn <- dimnames(x)
    if (is.null(dn) || length(dn) < 2L) {
        return(NULL);
    }
    return(dn[[2L]]);
}

firstfactor <- function(x, startIdx, nrx, ncx, c, nrc, ncc, v) {
    for (j in 1:ncc) {
        idx <- startIdx + (j-1) * nrx
        for (i in 1:nrx) {
            if (is.na(v[[i]])) {
                x[[idx]] <- as.double(NA)
            } else {
                x[[idx]] <- c[j * nrc  + (v[[i]] - 1)]
            }
        }
    }
    x
}

# generalized for vectors and matrices, makes sure that all the columns
# are of certain length. see in modelmatrix why this is useful.
normalize.len <- function (var.row, len, default) {
    if (nrows(var.row) == len) { return(var.row) }
    
    handle.vector <- function (vec) {
        take <- min(length(vec), len)
        c(vec[1:take], rep(default, len - length(vec)))    
    }
    
    plain <- unclass(var.row)
    if (is.vector(plain) || is.list(plain)) {
        return(handle.vector(vec, len, default))
    }
    if (is.matrix(plain) || is.data.frame(var.row)) {
        for (j in ncols(var.row)) {
            var.row[,j] <- handle.vector(var.row[,j], len, default)
        }
        return(var.row)
    }
    error("normalize.len unimplemented for type " + typeof(plain))
}

# generalized version of matrix[,j] that works for vectors
get.col <- function(x, j) {
    plain <- unclass(x)
    if (is.vector(plain) || (is.list(plain) && !is.data.frame(x))) x else x[,j]
}

addvar <- function(x, jstart, ncx, var.row) {
    # jstart is index of the first column, but we want to use it as a base offset, 
    # hence we sub 1, so that all indexing is more natural in 1-based world of R
    jstart <- jstart - 1
    for (k in ncols(var.row):1) {
        for (j in 1:ncx) {
            x[,jstart + (k-1)*ncx + j] <- get.col(var.row, k) * x[,jstart + j]
        }
    }
    x
}

# PUBLIC modelmatrix
#
# 'modelframe' is a list with values of the variables that appear in the 'formula'.
# The task of this function is to create a matrix of values of terms that appear 
# in the formula on the RHS. E.g. formula y~x+q+x:q has 2 variables on RHS and 3 terms: 
# 'x', 'q', and 'x:q'. In the world of formulae ':' is multiplication, so the resulting 
# matrix should look like this:
#
# 'x'   'q'     'x:q'
#  2     3       6
#  4     2       8
# etc.
#
# where in the environment there is variable x=c(2,4,...) and q=c(3,2,...)
#
modelmatrix <- function(formula, modelframe) {
    intercept <- as.logical(attr(formula, "intercept"))
    response <- as.integer(attr(formula, "response"))

    # Get the factor pattern matrix.  We duplicate this because.
    # we may want to alter it if we are in the no-intercept case.
    # Note: the values of "nVar" and "nterms" are the REAL number of
    # variables in the model data frame and the number of model terms.
    
    factors <- attr(formula, "factors")
    if (length(factors) == 0L) {
        nvar <- 1L
        nterms <- 0L
    } else if (is.integer(factors) && is.matrix(factors)) {
        nvar <- nrows(factors)
        nterms <- ncols(factors)
    } else {
        error("invalid 'terms' argument")
    }
    
    # get the variable names from the factor matrix - these are name of the rows
    vnames <- dimnames(factors)
    if (length(factors) > 0L) {
        if (length(vnames) < 1L || (nvar - intercept > 0 && !is.character(vnames[[1L]]))) {
            error("invalid 'terms' argument")    
        }
        vnames = vnames[[1L]]
    }
    
    # Get the variables from the model frame.  First perform
    # elementary sanity checks.  Notes:  1) We need at least
    # one variable (lhs or rhs) to compute the number of cases.
    # 2) We don't type-check the response.

    if (!is.list(modelframe) || length(modelframe) < nvar) {
        error("invalid model frame")
    } else if (length(modelframe) == 0L) {
        error("do not know how many cases")
    }
    
    # This section of the code checks the types of the variables
    # in the model frame.  Note that it should really only check
    # the variables if they appear in a term in the model.
    # Because it does not, we need to allow other types here, as they
    # might well occur on the LHS.
    # The R code converts all character variables in the model frame to
    # factors, so the only types that ought to be here are logical,
    # integer (including factor), numeric and complex.
    variable <- modelframe
    columns <- vapply(modelframe, ncols, 0L)
    ordered <- vapply(modelframe, isOrderedInt, FALSE)
    nlevs <- integer(nvar);
    n <- nrows(modelframe[[1L]])
    for (i in 1:nvar) {
        var_i <- variable[[i]]
        if (nrows(var_i) != n) {
            error(paste0("variable lengths differ (found for variable ", i, ")"))
        }
        if (is.factor(var_i)) {
            if ((nlevs[[i]] <- nlevels(var_i)) < 1) {
                error(paste0("variable has no levels"));
            }
        } else if (is.logical(var_i)) {
            nlevs[[i]] <- 2L
        } else {
            nlevs[[i]] <- 0L
        }
    }
    
    # If there is no intercept we look through the factor pattern
    # matrix and adjust the code for the first factor found so that
    # it will be coded by dummy variables rather than contrasts.
    if (!intercept) {
        # Note/TODO: in GnuR response is retrieved using asLogical, 
        # but here it is used as an index, is this intended?
        done <- FALSE
        for (j in 1:nterms) {
            for (i in (response+1):nvar) {
                if (nlevs[[i]] > 1L && factors[i,j] > 0L) {
                    factors[i, j] <- 2
                    done <- TRUE
                    break
                }
            }
            if (done) { break }
        }
    }
    
    # Compute the required contrast or dummy variable matrices. 
    # We do not have to set up symbolic expression, but simply
    # evaluate contrasts for given variable, only for variables 
    # that are factors.
    
    contr1 <- vector("list", nvar);
    contr2 <- vector("list", nvar);
    for (i in 1:nvar) {
        if (nlevs[[i]] == 0L) {
            next
        }
        if (1 %in% factors[i,]) {
            contr1[[i]] = contrasts(variable[[i]], TRUE)
        }
        if (2 %in% factors[i,]) {
            contr2[[i]] = contrasts(variable[[i]], FALSE)
        }
    }
    
    # By convention, an rhs term identical to the response generates nothing
    # in the model matrix (but interactions involving the response do).
    rhs_response = -1;
    if (response > 0) {
        for (j in 1:nterms) {
            if (factors[response, j] != 0L && sum(factors[,j] > 0L) == 1L) {
                rhs_response = j
                break
            }
        }
    }
    
    # We now have everything needed to build the design matrix.
    # The first step is to compute the matrix size and to allocate it.
    # Note that "count" holds a count of how many columns there are
    # for each term in the model and "nc" gives the total column count.
    count <- integer(nterms)
    dnc <- 0
    if (intercept) {
        dnc <- 1
    }
    for (j in 1:nterms) {
        if (j == rhs_response) {
            warning("the response appeared on the right-hand side and was dropped")
            count[[j]] <- 0L
        }
        
        dk <- 1L
        for (i in 1:nvar) {
            factors_ij <- factors[i,j]
            if (factors_ij == 0L) {
                next
            }
            
            if (nlevs[[i]] != 0L) {
                if (factors_ij == 1L) {
                    dk <- dk * ncols(contr1[[i]])
                } else if (factors_ij == 2L) {
                    dk <- dk * ncols(contr2[[i]])
                }
            } else {
                dk <- dk * columns[[i]]
            }
        }
        
        if (typeof(dk) == "double") {
            error(paste0("term ", j, " would require ", dk, " columns"))
        }
        count[[j]] <- dk
        dnc <- dnc + dk
    }    
    
    # Record which columns of the design matrix are associated with which model terms
    assign <- integer(dnc)
    k <- 1L
    if (intercept) {
        assign[[k]] <- 0L
        k <- k + 1
    }
    for (j in 1:nterms) {
        if (count[[j]] <= 0L) {
            warning(paste0("problem with term ", j, " in model.matrix: no columns are assigned"))
        }
        
        # idx.seq covers columns that are associated with term 'j'
        idx.seq <- seq(k, length.out = count[[j]])
        assign[idx.seq] <- j
        k <- k + count[[j]]
    }
    
    # Create column labels for the matrix columns.
    # Here we loop over the terms in the model and, within each
    # term, loop over the corresponding columns of the design
    # matrix, assembling the names.
    xnames <- character(dnc)
    k <- 1
    if (intercept) {
        xnames[[k]] <- "(Intercept)"
        k <- k + 1
    }
    
    # Example: the factor may look like:
    #   x q  x:q
    # y 0 0   0
    # x 1 0   1
    # q 0 1   1
    #
    # for each column we take names of variables that have '1' or '2' and 
    # append them together with ':', this gives us 'x', 'q', 'x:q'. Plus 
    # some special handling that makes it less straightforward
    
    for (j in 1:nterms) {
        if (j == rhs_response) {
            next
        }
        for (kk in 1:count[j]) {
            first <- TRUE
            indx <- kk - 1 # zero base like in GnuR C code
            buffer <- ""
            for (i in 1:nvar) {
                ll <- factors[i,j]
                if (ll != 0L) {
                    var_i <- variable[[i]]
                    if (!first) {
                        buffer  <- paste0(buffer, ":")
                    }
                    first <- FALSE
                    if (is.factor(var_i) || is.logical(var_i)) {
                        if (ll == 1) {
                            x = ColumnNames(contr1[[i]])
                            ll <- ncols(contr1[[i]])
                        } else {
                            x = ColumnNames(contr2[[i]])
                            ll <- ncols(contr2[[i]])
                        }
                        buffer <- paste0(buffer, vnames[[i]])
                        if (is.null(x)) {
                            buffer <- paste0(buffer, indx %% ll + 1)
                        } else {
                            buffer <- paste0(buffer, x[[indx %% ll + 1]])
                        }
                    } else if (is.complex(var_i)) {
                        error("complex variables are not currently allowed in model matrices");
                    } else if (is.numeric(var_i)) {
                        x = ColumnNames(var_i)
                        ll = ncols(var_i)
                        buffer = paste0(buffer, vnames[[i]])
                        if (ll > 1L) {
                            if (is.null(x)) {
                                buffer <- paste0(buffer, indx %% ll + 1)
                            } else {
                                buffer <- paste0(buffer, x[[indx %% ll + 1]])
                            }
                        }
                    } else {
                        error(paste0("variables of type '", typeof(var_i), "' are not allowed in model matrices"))
                    }
                    indx <- indx %/% ll;
                }
            }
            
            xnames[[k]] <- buffer
            k <- k + 1
        }
    }
    
    # ----------------------------
    # Compute the design matrix
    #
    # note: design matrix contains the values of the terms, here we mean explicit values 
    # provided from the environment variables that match the variables in the formula
    # We go through each column of the factor and from it we construct one or more columns 
    # in the design matrix.
    x <- matrix(NA, nrow = n, ncol = dnc)
    
    # begin with a column of 1s for the intercept
    if (intercept != 0L) {
        x[,1] <- 1
    }
    
    # FastR specific: we normalize lengths of variables, so that we can 
    # then use vectorized operations. In most of the typical cases, normalize.len
    # should only forward its parameter
    for (i in 1:nvar) {
        variable[[i]] <- normalize.len(variable[[i]], n, default=NA)
    }
    
    # jnext tells us the next column in the result 'x' that we will fill in with data.
    jnext <- as.integer(intercept) + 1
    jstart <- jnext
    contrast <- NULL
    for (k in 1:nterms) {
        if (k == rhs_response) { next }
        # for each term we go through the rows in corresponding column in 'factor'
        for (i in 1:nvar) {
            if (columns[[i]] == 0L) { next } # num of cols == 0
            var_i <- variable[[i]]
            factor_ik <- factors[i,k]
            
            # if factor for this variable is != 0 we do some action with it, resulting 
            # into putting new columns into the result 'x'. This moves jnext by the 
            # number of new columns, jstart tells us the first column that was copied 
            # within this innermost loop
            if (factor_ik == 0L) { next }
            if (factor_ik == 1L) {
                contrast <- contr1[[i]]
            } else {
                contrast <- contr2[[i]]
            }
            
            # is this the first non-zero factor in this factor column?
            if (jnext == jstart) {
                if (nlevs[[i]] > 0L) {
                    for (j in 1:ncols(contrast)) {
                        x[,jstart + j - 1] = get.col(contrast,j)[var_i]
                    }
                    jnext = jnext + ncols(contrast)
                } else {
                    # first variable in this term is simply copied, note that it can 
                    # be a matrix or a vector, this assignment handles both: 
                    # vector is treated as a single column matrix
                    x[, seq(jstart, length.out = ncols(var_i))] <- var_i
                    jnext = jnext + ncols(var_i)
                }
            } else {
                if (nlevs[[i]] > 0L) {
                    cont.var = matrix(0L, nrows(var_i), ncols(contrast))
                    for (j in 1:ncols(contrast)) {
                        cont.var[,j] = get.col(contrast,j)[var_i]
                    }                    
                    x <- addvar(x, jstart, jnext - jstart, cont.var)
                    jnext <- jnext + (jnext - jstart) * (ncols(contrast) - 1);
                } else {
                    x <- addvar(x, jstart, jnext - jstart, var_i)
                    jnext <- jnext + (jnext - jstart) * (ncols(var_i) - 1);
                }
            }
        }
        jstart <- jnext
    }
    
    dimnames(x) <- list(row.names(modelframe), xnames)
    attr(x, "assign") <- assign
    x
}




