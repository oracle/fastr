/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 1995-2015, The R Core Team
 * Copyright (c) 2003, The R Foundation
 * Copyright (c) 2015, 2017, Oracle and/or its affiliates
 *
 * All rights reserved.
 */

// These are the definitions (i.e., not extern) that are defined as extern in RInternals.h.
// They are in a a separate header to support a JNI and non-JNI implementation of their values.
// Therefore this file must only be included by the implementation.
// N.B. Some variables become functions in FastR, see RInternals.h

/* Evaluation Environment */
//SEXP R_GlobalEnv;
SEXP R_EmptyEnv;
//SEXP R_BaseEnv;
//SEXP R_BaseNamespace;
//SEXP R_NamespaceRegistry;

//SEXP R_Srcref;

/* Special Values */
SEXP R_NilValue;
SEXP R_UnboundValue;
SEXP R_MissingArg;

/* Symbol Table Shortcuts */
SEXP R_Bracket2Symbol;   /* "[[" */
SEXP R_BracketSymbol;    /* "[" */
SEXP R_BraceSymbol;      /* "{" */
SEXP R_ClassSymbol;     /* "class" */
SEXP R_DeviceSymbol;     /* ".Device" */
SEXP R_DevicesSymbol;     /* ".Devices" */
SEXP R_DimNamesSymbol;   /* "dimnames" */
SEXP R_DimSymbol;     /* "dim" */
SEXP R_DollarSymbol;     /* "$" */
SEXP R_DotsSymbol;     /* "..." */
SEXP R_DropSymbol;     /* "drop" */
SEXP R_LastvalueSymbol;  /* ".Last.value" */
SEXP R_LevelsSymbol;     /* "levels" */
SEXP R_ModeSymbol;     /* "mode" */
SEXP R_NameSymbol;     /* "name" */
SEXP R_NamesSymbol;     /* "names" */
SEXP R_NaRmSymbol;     /* "na.rm" */
SEXP R_PackageSymbol;    /* "package" */
SEXP R_QuoteSymbol;     /* "quote" */
SEXP R_RowNamesSymbol;   /* "row.names" */
SEXP R_SeedsSymbol;     /* ".Random.seed" */
SEXP R_SourceSymbol;     /* "source" */
SEXP R_TspSymbol;     /* "tsp" */

SEXP R_dot_defined;      /* ".defined" */
SEXP R_dot_Method;       /* ".Method" */
SEXP R_dot_target;       /* ".target" */
SEXP R_NaString;	    /* NA_STRING as a CHARSXP */
SEXP R_BlankString;	    /* "" as a CHARSXP */

// Symbols not part of public API but used in FastR tools implementation
SEXP R_SrcrefSymbol;
SEXP R_SrcfileSymbol;

// logical constants
SEXP R_TrueValue;
SEXP R_FalseValue;
SEXP R_LogicalNAValue;

// Arith.h
double R_NaN;		/* IEEE NaN */
double R_PosInf;	/* IEEE Inf */
double R_NegInf;	/* IEEE -Inf */
double R_NaReal;	/* NA_REAL: IEEE */
int R_NaInt;	/* NA_INTEGER:= INT_MIN currently */

// from Defn.h
char* R_Home;
const char* R_TempDir;

// Set by a down call based on the setting in the initial context
Rboolean R_Interactive;

// various ignored flags and variables:
Rboolean R_Visible;
Rboolean R_interrupts_suspended;
int R_interrupts_pending;
Rboolean mbcslocale;
Rboolean useaqua;
char* OutDec = ".";
Rboolean utf8locale = FALSE;
Rboolean mbcslocale = FALSE;
Rboolean latin1locale = FALSE;
int R_dec_min_exponent = -308;
int max_contour_segments = 25000;

// from sys-std.c
#include <R_ext/eventloop.h>

static InputHandler BasicInputHandler = {2, -1, NULL};
InputHandler *R_InputHandlers = &BasicInputHandler;

// ordinal numbers of the RVariables enum
#define R_Home_x 0
#define R_TempDir_x 1
#define R_NilValue_x 2
#define R_UnboundValue_x 3
#define R_MissingArg_x 4
#define R_GlobalEnv_x 5
#define R_EmptyEnv_x 6
#define R_BaseEnv_x 7
#define R_BaseNamespace_x 8
#define R_NamespaceRegistry_x 9
#define R_Srcref_x 10
#define R_Bracket2Symbol_x 11
#define R_BracketSymbol_x 12
#define R_BraceSymbol_x 13
#define R_ClassSymbol_x 14
#define R_DeviceSymbol_x 15
#define R_DevicesSymbol_x 16
#define R_DimNamesSymbol_x 17
#define R_DimSymbol_x 18
#define R_DollarSymbol_x 19
#define R_DotsSymbol_x 20
#define R_DropSymbol_x 21
#define R_LastvalueSymbol_x 22
#define R_LevelsSymbol_x 23
#define R_ModeSymbol_x 24
#define R_NameSymbol_x 25
#define R_NamesSymbol_x 26
#define R_NaRmSymbol_x 27
#define R_PackageSymbol_x 28
#define R_QuoteSymbol_x 29
#define R_RowNamesSymbol_x 30
#define R_SeedsSymbol_x 31
#define R_SourceSymbol_x 32
#define R_TspSymbol_x 33
#define R_dot_defined_x 34
#define R_dot_Method_x 35
#define R_dot_target_x 36
#define R_SrcrefSymbol_x 37
#define R_SrcfileSymbol_x 38
#define R_NaString_x 39
#define R_NaN_x 40
#define R_PosInf_x 41
#define R_NegInf_x 42
#define R_NaReal_x 43
#define R_NaInt_x 44
#define R_BlankString_x 45
#define R_TrueValue_x 46
#define R_FalseValue_x 47
#define R_LogicalNAValue_x 48
#define R_Interactive_x 49
