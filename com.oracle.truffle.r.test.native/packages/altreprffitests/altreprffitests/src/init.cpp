#include <R.h>
#include <Rinternals.h>
#include <R_ext/Rdynload.h>
#include "altrep_classes.hpp"

static SEXP is_altrep(SEXP x);
static SEXP my_test();

static const R_CallMethodDef CallEntries[] = {
        {"is_altrep", (DL_FUNC) &is_altrep, 1},
        {"simple_vec_wrapper_create_instance", (DL_FUNC) &VecWrapper::createInstance, 9},
        {"logging_vec_wrapper_create_instance", (DL_FUNC) &LoggingVecWrapper::createInstance, 9},
        {"logging_vec_wrapper_was_method_called", (DL_FUNC) &LoggingVecWrapper::wasMethodCalled, 2},
        {"logging_vec_wrapper_clear_called_methods", (DL_FUNC) &LoggingVecWrapper::clearCalledMethods, 0},
        {"native_mem_vec_create_instance", (DL_FUNC) &NativeMemVec::createInstance, 1},
        {"native_mem_vec_delete_instance", (DL_FUNC) &NativeMemVec::deleteInstance, 1},
        {"my_test", (DL_FUNC) &my_test, 0},
        {NULL, NULL, 0}
};

extern "C" void R_init_altreprffitests(DllInfo *dll)
{
    R_registerRoutines(dll, NULL, CallEntries, NULL, NULL);
}

static SEXP is_altrep(SEXP x)
{
    return ScalarLogical(ALTREP(x));
}

static SEXP my_test()
{
    SEXP data = PROTECT(allocVector(INTSXP, 5));
    int sorted_mode = 0;
    for (int i = 0; i < 2; i++) {
        Args args;
        if (i == 0) {
            // Is_sorted = true
            args = Args{FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, TRUE};
        }
        else {
            // Is_sorted = false
            args = Args{};
        }
        SEXP instance = VecWrapper::createInstanceFromArgs(data, args);
        sorted_mode = INTEGER_IS_SORTED(instance);
    }
    return ScalarInteger(sorted_mode);
}