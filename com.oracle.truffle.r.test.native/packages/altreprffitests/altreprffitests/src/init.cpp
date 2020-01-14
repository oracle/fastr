#include <R.h>
#include <Rinternals.h>
#include <R_ext/Rdynload.h>
#include "altrep_classes.hpp"

static SEXP is_altrep(SEXP x);
extern "C" SEXP my_test(SEXP vec);

static const R_CallMethodDef CallEntries[] = {
        {"is_altrep", (DL_FUNC) &is_altrep, 1},
        {"simple_vec_wrapper_create_instance", (DL_FUNC) &VecWrapper::createInstance, 9},
        {"logging_vec_wrapper_create_instance", (DL_FUNC) &LoggingVecWrapper::createInstance, 9},
        {"logging_vec_wrapper_was_method_called", (DL_FUNC) &LoggingVecWrapper::wasMethodCalled, 2},
        {"logging_vec_wrapper_clear_called_methods", (DL_FUNC) &LoggingVecWrapper::clearCalledMethods, 0},
        {"my_test", (DL_FUNC) &my_test, 1},
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

/*static int my_elt_method(SEXP instance, R_xlen_t idx)
{
    // instance should be NativeMirror
    SEXP data1 = R_altrep_data1(instance);
    return 42;
}

static void * my_dataptr_method(SEXP instance, Rboolean writeabble) {
    return nullptr;
}

static R_xlen_t my_length_method(SEXP instance) {
    return 42;
}*/

extern "C" SEXP my_test(SEXP vec)
{
    /*R_altrep_class_t descr = R_make_altinteger_class("MyClassName", "MyPackageName", nullptr);
    R_set_altinteger_Elt_method(descr, &my_elt_method);
    R_set_altrep_Length_method(descr, &my_length_method);
    R_set_altvec_Dataptr_method(descr, &my_dataptr_method);
    SEXP instance = R_new_altrep(descr, R_NilValue, R_NilValue);
    // invokeEltMethod --> invokeNativeFunction --> R_altrep_data1Call --> ...
    int first_elt = INTEGER_ELT(instance, 0);*/

    return R_NilValue;
}