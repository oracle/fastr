#include <R.h>
#include <Rdefines.h>
#include <Rinternals.h>

SEXP addint(SEXP a, SEXP b) {
	int aInt = INTEGER_VALUE(a);
	int bInt = INTEGER_VALUE(b);
	return ScalarInteger(aInt + bInt);
}
