#ifndef __JGD_TALK_H__
#define __JGD_TALK_H__

#ifdef HAVE_CONFIG_H
# include <config.h>
#endif

#include "javaGD.h"

extern void setupJavaGDfunctions(NewDevDesc *dd);

Rboolean newJavaGD_Open(NewDevDesc *dd, newJavaGDDesc *xd, const char *dsp, double w, double h);


#endif
