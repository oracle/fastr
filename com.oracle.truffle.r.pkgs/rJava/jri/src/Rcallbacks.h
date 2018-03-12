#ifndef __R_CALLBACKS__H__
#define __R_CALLBACKS__H__

#include <R.h>
#include <Rinternals.h>
#include <Rversion.h>

/* functions provided as R callbacks */

#if R_VERSION < R_Version(2,7,0)
#define RCCONST
#else
#define RCCONST const
#endif

int  Re_ReadConsole(RCCONST char *prompt, unsigned char *buf, int len, int addtohistory);
void Re_Busy(int which);
void Re_WriteConsole(RCCONST char *buf, int len);
void Re_WriteConsoleEx(RCCONST char *buf, int len, int oType);
void Re_ResetConsole();
void Re_FlushConsole();
void Re_ClearerrConsole();
int  Re_ChooseFile(int new, char *buf, int len);
void Re_ShowMessage(RCCONST char *buf);
void Re_read_history(char *buf);
void Re_loadhistory(SEXP call, SEXP op, SEXP args, SEXP env);
void Re_savehistory(SEXP call, SEXP op, SEXP args, SEXP env);
int  Re_ShowFiles(int nfile, RCCONST char **file, RCCONST char **headers, RCCONST char *wtitle, Rboolean del, RCCONST char *pager);

#endif
