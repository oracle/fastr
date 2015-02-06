#include <R.h>
#include <Rinternals.h>

#include <R_ext/Rdynload.h>

void
R_init_testrffi(DllInfo *dll)
{
    R_registerRoutines(dll, NULL, NULL, NULL, NULL);
}
