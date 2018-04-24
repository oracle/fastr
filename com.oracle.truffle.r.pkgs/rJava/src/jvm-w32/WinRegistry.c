#include <Rinternals.h>

#ifdef WIN32
#include <windows.h>
#include <winreg.h>

char RegStrBuf[32768];

SEXP RegGetStrValue(SEXP par) {
  SEXP res=R_NilValue;
  DWORD t,s=32767;
  HKEY k;
  char *key=CHAR(STRING_ELT(par, 0));
  char *val=CHAR(STRING_ELT(par, 1));

  RegStrBuf[32767]=*RegStrBuf=0;
  /* printf("RegGetStrValue(\"%s\",\"%s\")\n",key,val); */

  if (RegOpenKeyEx(HKEY_LOCAL_MACHINE,key,0,KEY_QUERY_VALUE,&k)!=ERROR_SUCCESS ||
      RegQueryValueEx(k,val,0,&t,RegStrBuf,&s)!=ERROR_SUCCESS)
    return res;
  
  PROTECT(res = allocVector(STRSXP, 1));
  SET_STRING_ELT(res, 0, mkChar(RegStrBuf));
  UNPROTECT(1);

  return res;
};

#else
/* all functions return NULL since they are not supported on non-Win32 platforms */
SEXP RegGetStrValue(SEXP par) { return R_NilValue; };

#endif
