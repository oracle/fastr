#include <windows.h>
#include <winreg.h>
#include <stdio.h>

static char RegStrBuf[32768], dbuf[32768];

int main(int argc, char **argv) {
  int i=0, doit=0;
  DWORD t,s=32767;
  HKEY k;
  HKEY root=HKEY_LOCAL_MACHINE;
  char *javakey="Software\\JavaSoft\\Java Runtime Environment";

  /* JAVA_HOME can override our detection - but we still post-process it */
  if (getenv("JAVA_HOME")) {
    strcpy(RegStrBuf,getenv("JAVA_HOME"));
  } else {

#ifdef FINDJRE
    if (RegOpenKeyEx(HKEY_LOCAL_MACHINE,javakey,0,KEY_QUERY_VALUE,&k)!=ERROR_SUCCESS ||
	RegQueryValueEx(k,"CurrentVersion",0,&t,RegStrBuf,&s)!=ERROR_SUCCESS) {
#endif
      javakey="Software\\JavaSoft\\Java Development Kit"; s=32767;
      if (RegOpenKeyEx(HKEY_LOCAL_MACHINE,javakey,0,KEY_QUERY_VALUE,&k)!=ERROR_SUCCESS ||
	  RegQueryValueEx(k,"CurrentVersion",0,&t,RegStrBuf,&s)!=ERROR_SUCCESS) {
	fprintf(stderr, "ERROR*> JavaSoft\\{JRE|JDK} can't open registry keys.\n");
	/* MessageBox(wh, "Can't find Sun's Java runtime.\nPlease install Sun's J2SE JRE or JDK 1.4.2 or later (see http://java.sun.com/).","Can't find Sun's Java",MB_OK|MB_ICONERROR); */
	return -1;
      }
#ifdef FINDJRE
    }
#endif
    RegCloseKey(k); s=32767;

    strcpy(dbuf,javakey);
    strcat(dbuf,"\\");
    strcat(dbuf,RegStrBuf);
    javakey=(char*) malloc(strlen(dbuf)+1);
    strcpy(javakey, dbuf);
    
    if (RegOpenKeyEx(HKEY_LOCAL_MACHINE,javakey,0,KEY_QUERY_VALUE,&k)!=ERROR_SUCCESS ||
	RegQueryValueEx(k,"JavaHome",0,&t,RegStrBuf,&s)!=ERROR_SUCCESS) {
      fprintf(stderr, "There's no JavaHome value in the JDK/JRE registry key.\n");
      /* MessageBox(wh, "Can't find Java home path. Maybe your JRE is too old.\nPlease install Sun's J2SE JRE or SDK 1.4.2 (see http://java.sun.com/).","Can't find Sun's Java",MB_OK|MB_ICONERROR); */
      return -1;
    }
    RegCloseKey(k);
  }
  
  /*--- post-processing according to supplied flags --*/

  /* -a = automagic, i.e. use short name only if the name contains spaces */
  i=1;
  while (i<argc) if (!strcmp(argv[i++],"-a")) { doit=1; break; };
  if (doit) {
    int hasws=0;
    char *c=dbuf;
    while (*c) { if (*c==' '||*c=='\t') { hasws=1; break; } c++; };
    if (!hasws) doit=0;
  }

  /* -s = short name */
  if (!doit) {
    i=1;
    while (i<argc) if (!strcmp(argv[i++],"-s")) { doit=1; break; };
  }
  strcpy(dbuf, RegStrBuf);
  if (doit)
    GetShortPathName(RegStrBuf, dbuf, 32768);

  /* -f = forward slashes */
  doit=0; i=1;
  while (i<argc) if (!strcmp(argv[i++],"-f")) { doit=1; break; };
  if (doit) {
    char *c=dbuf;
    while (*c) { if (*c=='\\') *c='/'; c++; };
  }

  puts(dbuf);
  return 0;
}

