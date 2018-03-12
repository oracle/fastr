# Process this file with autoconf to produce a configure script.
AC_INIT(rJava, 0.8, Simon.Urbanek@r-project.org)
AC_CONFIG_SRCDIR([src/rJava.c])
AC_CONFIG_HEADER([src/config.h])

# find R home and set CC/CFLAGS
: ${R_HOME=`R RHOME`}
if test -z "${R_HOME}"; then
  echo "could not determine R_HOME"
  exit 1
fi
RBIN="${R_HOME}/bin/R"
CC=`"${RBIN}" CMD config CC`;
CFLAGS=`"${RBIN}" CMD config CFLAGS`
LIBS="${PKG_LIBS}"
AC_SUBST(R_HOME)

RLD=`"${RBIN}" CMD config --ldflags 2>/dev/null`
has_R_shlib=no
if test -n "$RLD"; then
   has_R_shlib=yes
fi

## enable threads, i.e. Java is running is a separate thread
AC_ARG_ENABLE([threads],
[AC_HELP_STRING([--enable-threads],
		[enable the use of threads, i.e. Java is run on a separate thread.
		This is necessary for some implementations of AWT. This feature
		is highly experimental, becasue of synchronization issues, so
		use with care. @<:@no@:>@])],
[want_threads="${enableval}"],
[want_threads=no])

## enable JNI-cache
AC_ARG_ENABLE([jni-cache],
[AC_HELP_STRING([--enable-jni-cache],
	        [enable support for caching of the JNI environment. With this
		 option turned on, the JNI state is stored locally and re-used
		 for subsequent calls. This will work *only* if no threads are
		 used, because each thread has a separate JNI state. Enabling
		 this option can give some performance boost for applications
		 that call JNI very often. If used in a threaded environment,
		 it is bound to crash, so use with care. @<:@no@:>@])],
[want_jni_cache="${enableval}"],
[want_jni_cache=no])

## enable JRI
AC_ARG_ENABLE([jri],
[AC_HELP_STRING([--enable-jri],
		[enable Java to R interface (JRI), which allows Java programs
		to embed R. @<:@auto@:>@])],
[want_jri="${enableval}"],
[want_jri=auto])

## enable headless
AC_ARG_ENABLE([headless],
[AC_HELP_STRING([--enable-headless],
		[enable initialization in headless mode. @<:@auto@:>@])],
[want_headless="${enableval}"],
[want_headless=auto])

## enable -Xrs support
AC_ARG_ENABLE([Xrs],
[AC_HELP_STRING([--enable-Xrs],
		[use -Xrs in Java initialization. @<:@auto@:>@])],
[want_xrs="${enableval}"],
[want_xrs=auto])

## enable debug flags
AC_ARG_ENABLE([debug],
[AC_HELP_STRING([--enable-debug],
		[enable debug flags and output. @<:@no@:>@])],
[want_debug="${enableval}"],
[want_debug=no])

## enable memory profiling
AC_ARG_ENABLE([mem-profile],
[AC_HELP_STRING([--enable-mem-profile],
		[enable memory profiling. @<:@debug@:>@])],
[want_memprof="${enableval}"],
[want_memprof=debug])

## enable callbacks (experimental)
AC_ARG_ENABLE([callbacks],
[AC_HELP_STRING([--enable-callbacks],
		[enable the support for callbacks from Java into R. This requires JRI and is currently experimental/incomplete. @<:@no@:>@])],
[want_callbacks="${enableval}"],
[want_callbacks=no])


# Checks for programs.
AC_PROG_CC

# Checks for libraries.

# Checks for header files.
AC_HEADER_STDC
AC_HEADER_SYS_WAIT
AC_CHECK_HEADERS([string.h sys/time.h unistd.h])

# Checks for typedefs, structures, and compiler characteristics.
AC_C_CONST
AC_HEADER_TIME

AC_CHECKING([whether ${CC} supports static inline])
can_inline=no
AC_COMPILE_IFELSE([AC_LANG_SOURCE([
static inline int foo(int a, int b);
static f = 1;
static inline int foo(int a, int b) { return a+b; }
int main(void) {
    return foo(f,-1);
}])],can_inline=yes)
AC_MSG_RESULT(${can_inline})
if test "${can_inline}" = yes; then
  AC_DEFINE(HAVE_STATIC_INLINE, 1, [Define to 1 when static inline works])
fi

### from R m4/R.m4 - needed to hack R 2.9.x
AC_CACHE_CHECK([whether setjmp.h is POSIX.1 compatible],
               [r_cv_header_setjmp_posix],
[AC_COMPILE_IFELSE([AC_LANG_PROGRAM(
[[#include <setjmp.h>]],
[[sigjmp_buf b;
sigsetjmp(b, 0);
siglongjmp(b, 1);]])],
                   [r_cv_header_setjmp_posix=yes],
                   [r_cv_header_setjmp_posix=no])])
AC_CHECK_DECLS([sigsetjmp, siglongjmp], , , [#include <setjmp.h>])
if test "$ac_cv_have_decl_sigsetjmp" = no; then
  r_cv_header_setjmp_posix=no
fi
if test "$ac_cv_have_decl_siglongjmp" = no; then
  r_cv_header_setjmp_posix=no
fi
if test "${r_cv_header_setjmp_posix}" = yes; then
  AC_DEFINE(HAVE_POSIX_SETJMP, 1,
            [Define if you have POSIX.1 compatible sigsetjmp/siglongjmp.])
fi

AC_MSG_CHECKING([Java support in R])
R_JAVA_HOME=`"${RBIN}" CMD config JAVA_HOME`
: ${JAVA_HOME="${R_JAVA_HOME}"}
if test -z "${JAVA_HOME}"; then
   AC_MSG_ERROR([absent
R was configured without Java support. Please run
R CMD javareconf
as root to add Java support to R.

If you do not have root privileges, run
R CMD javareconf -e
to set all Java-related variables and then install rJava.])
fi

: ${JAR=`"${RBIN}" CMD config JAR|sed 's/ERROR:.*//'`}
: ${JAVA=`"${RBIN}" CMD config JAVA|sed 's/ERROR:.*//'`}
: ${JAVAC=`"${RBIN}" CMD config JAVAC|sed 's/ERROR:.*//'`}
: ${JAVAH=`"${RBIN}" CMD config JAVAH|sed 's/ERROR:.*//'`}
: ${JAVA_CPPFLAGS=`"${RBIN}" CMD config JAVA_CPPFLAGS|sed 's/ERROR:.*//'`}
: ${JAVA_LIBS=`"${RBIN}" CMD config JAVA_LIBS|sed 's/ERROR:.*//'`}

AC_MSG_RESULT([present:
interpreter : '${JAVA}'
archiver    : '${JAR}'
compiler    : '${JAVAC}'
header prep.: '${JAVAH}'
cpp flags   : '${JAVA_CPPFLAGS}'
java libs   : '${JAVA_LIBS}'])

java_error='One or more Java configuration variables are not set.'
if test -z "${JAVA}"; then
   java_error='Java interpreter is missing or not registered in R'
fi
if test -z "${JAVAC}"; then
   java_error='Java Development Kit (JDK) is missing or not registered in R'
fi

have_all_flags=no
if test -n "${JAVA}" && test -n "${JAVAC}" && test -n "${JAVAH}" && \
   test -n "${JAVA_CPPFLAGS}" && test -n "${JAVA_LIBS}" && test -n "${JAR}"; then
  have_all_flags=yes;
fi
if test "${have_all_flags}" = no; then
  AC_MSG_ERROR([${java_error}
Make sure R is configured with full Java support (including JDK). Run
R CMD javareconf
as root to add Java support to R.

If you don't have root privileges, run
R CMD javareconf -e
to set all Java-related variables and then install rJava.
])
fi

if test `echo foo | sed -e 's:foo:bar:'` = bar; then
   JAVA_CPPFLAGS0=`echo ${JAVA_CPPFLAGS} | sed -e 's:$(JAVA_HOME):'${JAVA_HOME}':g'`
   JAVA_LIBS0=`echo ${JAVA_LIBS} | sed -e 's:$(JAVA_HOME):'${JAVA_HOME}':g'`
   JAVA_LD_LIBRARY_PATH0=`echo ${JAVA_LD_LIBRARY_PATH} | sed -e 's:$(JAVA_HOME):'${JAVA_HOME}':g'`
else
   AC_MSG_WARN([sed is not working properly - the configuration may fail])
   JAVA_CPPFLAGS0="${JAVA_CPPFLAGS}"
   JAVA_LIBS0="${JAVA_LIBS}"
   JAVA_LD_LIBRARY_PATH0="${JAVA_LD_LIBRARY_PATH}"
fi

OSNAME=`uname -s 2>/dev/null`

LIBS="${LIBS} ${JAVA_LIBS0}"
CFLAGS="${CFLAGS} ${JAVA_CPPFLAGS0}"
LD_LIBRARY_PATH="${LD_LIBRARY_PATH}:${JAVA_LD_LIBRARY_PATH0}"
if test "x$OSNAME" = xDarwin; then
  ## we need to pull that out of R in case re-export fails (which is does on 10.11)
  DYLD_FALLBACK_LIBRARY_PATH=`"${RBIN}" --slave --vanilla -e 'cat(Sys.getenv("DYLD_FALLBACK_LIBRARY_PATH"))'`
  export DYLD_FALLBACK_LIBRARY_PATH
fi

AC_MSG_CHECKING([whether Java run-time works])
if "$JAVA" -classpath . getsp; then
   AC_MSG_RESULT(yes)
else
   AC_MSG_RESULT(no)
   AC_MSG_ERROR([Java interpreter '$JAVA' does not work])
fi

has_xrs="$want_xrs"
if test x"$has_xrs" = xauto; then
   AC_MSG_CHECKING([whether -Xrs is supported])
   if "$JAVA" -Xrs -classpath . getsp; then
      has_xrs=yes
   else
      has_xrs=no
   fi
   AC_MSG_RESULT(${has_xrs})
fi

AC_MSG_CHECKING([whether -Xrs will be used])
AC_MSG_RESULT(${has_xrs})
if test "$has_xrs" = xyes; then
   AC_DEFINE(HAVE_XRS, 1, [Set if the Java parameter -Xrs is supported])
fi

AC_MSG_CHECKING([whether JNI programs can be compiled])
AC_LINK_IFELSE([AC_LANG_SOURCE([
#include <jni.h>
int main(void) {
    jobject o;
    JNI_CreateJavaVM(0, 0, 0);
    return 0;
}
	])],[AC_MSG_RESULT(yes)],
	[AC_MSG_ERROR([Cannot compile a simple JNI program. See config.log for details.

Make sure you have Java Development Kit installed and correctly registered in R.
If in doubt, re-run "R CMD javareconf" as root.
])])

AC_MSG_CHECKING([JNI data types])
AC_TRY_RUN(
	[
#include <jni.h>
int main(void) {
  return (sizeof(int)==sizeof(jint) && sizeof(long)==sizeof(long) && sizeof(jbyte)==sizeof(char) && sizeof(jshort)==sizeof(short) && sizeof(jfloat)==sizeof(float) && sizeof(jdouble)==sizeof(double))?0:1;
}
	],
	[AC_MSG_RESULT([ok])],
	[AC_MSG_ERROR([One or more JNI types differ from the corresponding native type. You may need to use non-standard compiler flags or a different compiler in order to fix this.])],
	[AC_MSG_RESULT([don't know (cross-compiling)])])

if test "${want_jri}" = auto; then
   AC_MSG_CHECKING([whether JRI should be compiled (autodetect)])
   AC_MSG_RESULT([${has_R_shlib}])
   want_jri=${has_R_shlib}
fi
AM_CONDITIONAL(WANT_JRI, [test "x${want_jri}" = xyes])

AC_MSG_CHECKING([whether debugging output should be enabled])
if test "${want_debug}" = yes; then
  JAVA_CPPFLAGS="-g -DRJ_DEBUG ${JAVA_CPPFLAGS}"
  AC_MSG_RESULT([yes])
else
  AC_MSG_RESULT([no])
fi

AC_MSG_CHECKING([whether memory profiling is desired])
if test "${want_memprof}" = debug; then
   want_memprof="${want_debug}"
fi
if test "${want_memprof}" = yes; then
  AC_DEFINE(MEMPROF, 1, [memory profiling is enabled when defined])
  AC_MSG_RESULT([yes])
else
  AC_MSG_RESULT([no])
fi

AC_SUBST(JAVA_LIBS)
AC_SUBST(JAVA_CPPFLAGS)
AC_SUBST(JAVA_HOME)
AC_SUBST(JAVA)
AC_SUBST(JAVAC)
AC_SUBST(JAVAH)

use_threads=no
AC_MSG_CHECKING([whether threads support is requested])
if test "${want_threads}" = yes; then
  AC_MSG_RESULT([yes])
  AC_MSG_CHECKING([whether threads can be enabled])
  # check whether we can add THREADS support
  # we don't want to run full AC_CANONICAL_HOST, all we care about is OS X
  if test "x$OSNAME" = xDarwin; then
    use_threads=yes
    AC_DEFINE(THREADS, 1, [Set if threading support should be enabled.])
  fi
  AC_MSG_RESULT([${use_threads}])
else
  AC_MSG_RESULT([no])
fi

## enable callbacks if desired
AC_MSG_CHECKING([whether callbacks support is requested])
if test "${want_callbacks}" = yes; then
   AC_MSG_RESULT([yes])
   if test "${want_jri}" != yes; then
      AC_MSG_ERROR([Callbacks support can be only enabled if JRI is enabled as well.])
   fi
   AC_DEFINE(ENABLE_JRICB, 1, [define if callbacks support is enabled.])
else
   AC_MSG_RESULT([no])
fi

AC_MSG_CHECKING([whether JNI cache support is requested])
if test "${want_jni_cache}" = yes; then
  AC_MSG_RESULT([yes])
  if test "${use_threads}" = yes; then
    AC_MSG_ERROR([Threads and JNI cache cannot be used at the same time, because JNI cache is by definition not thread-safe. Please disable either option.])
  fi
  AC_DEFINE(JNI_CACHE, 1, [Set if caching JNI environment is enabled.])
else
  AC_MSG_RESULT([no])
fi

AC_MSG_CHECKING([whether headless init is enabled])
if test "${want_headless}" = auto; then
  want_headless=no
  ## only Darwin defaults to headless
  if test "x$OSNAME" = xDarwin; then
    want_headless=yes
  fi
fi
AC_MSG_RESULT([${want_headless}])
if test "${want_headless}" = yes; then
  AC_DEFINE(USE_HEADLESS_INIT, 1, [Set if headless mode is to be used when starting the JVM])
fi

AC_MSG_CHECKING([whether JRI is requested])
if test "${want_jri}" = yes; then
  AC_MSG_RESULT([yes])
  export R_HOME
  export JAVA_HOME JAVA_CPPFLAGS JAVA_LIBS JAVA_LD_LIBRARY_PATH JAVA JAVAC JAVAH JAR
  CONFIGURED=1
  export CONFIGURED
  AC_CONFIG_SUBDIRS(jri)
else
  AC_MSG_RESULT([no])
fi

AC_CONFIG_FILES([src/Makevars])
AC_CONFIG_FILES([R/zzz.R])
AC_OUTPUT
