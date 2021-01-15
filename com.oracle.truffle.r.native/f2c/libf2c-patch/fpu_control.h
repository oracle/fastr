/* Used to avoid compiler error on musl due to missing header file */

#if !defined(__linux__) || defined(__GLIBC__)
#include_next "fpu_control.h"
#endif
