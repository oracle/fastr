#ifndef __CALLBACK_H__
#define __CALLBACK_H__

#ifdef ENABLE_JRICB
extern int RJava_has_control;
#endif

#define RJavaActivity 16

/* all IPC messages are long-alligned */
#define IPCC_LOCK_REQUEST 1
#define IPCC_LOCK_GRANTED 2 /* reponse on IPCC_LOCK_REQUEST */
#define IPCC_CLEAR_LOCK   3
#define IPCC_CALL_REQUEST 4 /* pars: <fn-ptr> <data-ptr> */
#define IPCC_CONTROL_ADDR 5 /* ipc: request, res: <ctrl-ptr> */

#endif

