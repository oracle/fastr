#ifndef __CALLBACK_H__
#define __CALLBACK_H__

#define RJavaActivity 16

/* all IPC messages are long-alligned */
#define IPCC_LOCK_REQUEST 1
#define IPCC_LOCK_GRANTED 2 /* reponse on IPCC_LOCK_REQUEST */
#define IPCC_CLEAR_LOCK   3
#define IPCC_CALL_REQUEST 4 /* pars: <fn-ptr> <data-ptr> */
#define IPCC_CONTROL_ADDR 5 /* ipc: request, res: <ctrl-ptr> */

int RJava_request_lock();
int RJava_clear_lock();
/* void RJava_request_callback(callbackfn *fn, void *data); */
void RJava_setup(int _in, int _out);
void RJava_init_ctrl();

#endif

