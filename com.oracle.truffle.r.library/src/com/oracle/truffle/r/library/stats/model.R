
error <- function(message) {
    stop(simpleError(message))
}

isLanguage <- function(x) {
    is.null(x) || typeof(x) == "language"
}

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
    if (is.null(formula) || isZeroOne(formula)) {
        return
    } 
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
        return
    } else if (isLanguage(formula)) {
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
        } else if (identical(op, quote(`+`))
            || identical(op, quote(`:`))
            || identical(op, quote(`-`))
            || identical(op, quote(`*`))
            || identical(op, quote(`/`))
            || identical(op, quote(`:`))
            || identical(op, quote(`^`))
            || identical(op, quote(`.`))
            || identical(op, quote(`(`))) {
            ExtractVars(formula[[2]], checkonly)
            ExtractVars(formula[[3]], checkonly)
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
SetBit <- function(term, whichBit, value) {
    term[[whichBit]] <- value
    term
}
GetBit <- function(term, whichBit) {
    term[[whichBit]]
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
    
    term <- vector("list",length(left), length(right))
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
    
    term <- vector("list",length(left), length(right))
    index <- 1L
    for (leftTerm in left) {
        for (rightTerm in right) {
            term[[index]] <- OrBits(leftTerm, rightTerm)
            index <- index + 1L
        }
    }
    return (TrimRepeats(c(left, term, right)))
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
    term <- list()
    right <- left
    for (power in 1L:(ip-1L)) {
        term <- vector("list",length(left), length(right))
        index <- 1L
        for (leftTerm in left) {
            for (rightTerm in right) {
                term[[index]] <- OrBits(leftTerm, rightTerm)
                index <- index + 1L
            }
        }
        right <- TrimRepeats(term)
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
    
    for (leftTerm in left) {
        term <- OrBits(term, rightTerm)
    }
    
    index <- 1L
    for (rightTerm in right) {
        right[[index]] <- OrBits(term, rightTerm)
        index <- index + 1L
    }
    return (TrimRepeats(c(right, left)))
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
            # TODO recast
        } else {
            term <- AllocTerm()
            term <- SetBit(term, InstallVar(formula), TRUE)
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
            return (DeleteTerms(formula[[2]], formula[[3]]))
        } else if (identical(op, quote(`(`))) {
            return (EncodeVars(formula[[2]]))
        } else {
            term <- AllocTerm()
            term <- SetBit(term, InstallVar(formula), TRUE)
            return (list(term))
        }
    } else {
        error("invalid model formula in EncodeVars")
    }
}



termsform <- function (x, specials, data, keep.order, allowDotAsName) {
	
    if (!isLanguage(x)
		|| !identical(x[[1]], quote(`~`))
		|| length(x) != 2 && length(x) != 3) {
	    error("argument is not a valid model");
	}
    
	if (length(specials) > 0 && !is.character(specials)) {
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
	    if (length(framenames) > 0) {
	        hadFrameNames = TRUE
	    }
	    if (length(x) == 3) {
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
	
	# we don't need to preallcoate attributes
	if (is.null(specials)) {
	    #a = allocList(8);
	    #SET_ATTRIB(ans, a);
	} else {
	    #a = allocList(9);
	    #SET_ATTRIB(ans, a);
	}
	
	# Step 1: Determine the ``variables'' in the model 
	# Here we create an expression of the form 
	# list(...).  You can evaluate it to get 
	# the model variables or use substitute and then 
	# pull the result apart to get the variable names. 
	
	intercept <<- FALSE
	parity <<- FALSE
	response <<- FALSE
	varlist <<- list()
	ExtractVars(x)
	vars <- quote(list())
	vars[2:(length(varlist) + 1)] <- varlist
	attr(x, "variables") <- vars
	
	nvar <<- length(varlist) 
	formula <- EncodeVars(x)
	
	# need to recompute, in case
	# EncodeVars stretched it 
	nvar <<- length(varlist) 

	
	# Step 2a: Compute variable names 
	k <- 0L
	varnames <- character(nvar)
	index <- 1L
	for (var in varlist) {
	    varnames[[index]] <- deparse(var, nlines=1)
	    index = index + 1L
	}
	
	# Step 2b: Find and remove any offset(s) 
	
	#first see if any of the variables are offsets
	k <- sum(substr(varnames, 0, 7) == "offset(")
	
	if (k > 0L) {
	    offsets <- integer(k)
	    #TODO
	    attr(x, "offset") <- offsets
	}
	
	nterm <<- length(formula);
	
	# Step 3: Reorder the model terms by BitCount, otherwise
	# preserving their order. 
	ord <- integer(nterm)
	
	pattern <- formula
	sCounts <- integer(nterm)
	
	index <- 1L
	for (call in formula) {
	    sCounts[[index]] <- BitCount(call)
	}
	bitmax <- max(sCounts)
	if (keep.order) {
	    # TODO
	} else {
	    call <- formula
	    for (i in 0:bitmax) {
	        for (n in 1:nterm) {
	            # TODO   
	        }
	    }
	}
	
	# Step 4: Compute the factor pattern for the model. 
	# 0 - the variable does not appear in this term. 
	# 1 - code the variable by contrasts in this term. 
	# 2 - code the variable by indicators in this term. 
	
	if (nterm > 0) {
	    pattern <- matrix(integer(nvar * nterm), nvar, nterm)
	    term <- AllocTerm()
	    
	    n <- 0
	    for (call in formula) {
	        for (i in 1:nvar) {
	            if (GetBit(call, i)) {
	                pattern[[i-1+n]] <- TermCode(formula, call, i, term)
	            }
	        }
	        
	        n <- n + 1
	    }
	    
	    #TODO
	    
	    attr(x, "factors") <- pattern
	} else {
	    attr(x, "factors") <- integer(0)
	}
	
	
	# Step 5: Compute term labels
	termlabs <- character(nterm)
	index <- 1L
	for (call in formula) {
	    first <- TRUE
	    sep <- ""
	    cbuf <- ""
	    for (i in seq(nvar)) {
	        if (GetBit(call, i)) {
	           cbuf <- paste(cbuf, varnames[[i]], sep = sep)
	           sep <- ":"
	        }
	    }
	    termlabs[[index]] <- cbuf
	    index <- index + 1L
	}
	v <- list(varnames, termlabs)
	if (nterm > 0) {
	    # TODO
	}
	attr(x, "term.labels") <- termlabs
	
	if (!is.null(specials)) {
	    #TODO
	}
	
	# Step 6: Fix up the formula by substituting for dot, which should be
	# the framenames joined by + 
	
	
	attr(x, "order") <- integer(nterm)
	
	attr(x, "intercept") <- as.integer(intercept)
	attr(x, "response") <- as.integer(response)
	class(x) <- c("terms", "formula")
    return(x)
}



    
