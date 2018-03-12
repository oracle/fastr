#ifndef __RDECL_H__
#define __RDECL_H__

/* declarations from R internals or other include files */
/* last update: R 2.4.0 */

void run_Rmainloop(void); /* main/main.c */
int  R_ReadConsole(char*, unsigned char*, int, int); /* include/Defn.h */
void Rf_checkArity(SEXP, SEXP); /* include/Defn.h */
int  Rf_initialize_R(int ac, char **av); /* include/Rembedded.h */

#endif
