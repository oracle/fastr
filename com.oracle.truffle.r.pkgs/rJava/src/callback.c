#include "rJava.h"

#ifdef ENABLE_JRICB

#include <R_ext/eventloop.h>
#include <unistd.h>
#include "callback.h"

int RJava_has_control = 0;

/* ipcin   ->  receives requests on R thread
   ipcout  <-  write to place requests to main R thread
   resout  <-  written by main thread to respond
   resin   ->  read to get info from main thread */

static int ipcin, ipcout, resin, resout;

static struct rJavaInfoBlock_s {
  int ipcin, ipcout, resin, resout;
  int *has_control;
} rJavaInfoBlock;

struct rJavaInfoBlock_s *RJava_get_info_block() {
  return &rJavaInfoBlock;
}

typedef void(callbackfn)(void*);

static void RJava_ProcessEvents(void *data) {
  long buf[4];
  int n = read(ipcin, buf, sizeof(long));
  if (buf[0] == IPCC_LOCK_REQUEST) {
    RJava_has_control = 1;
    buf[0] = IPCC_LOCK_GRANTED;
    write(resout, buf, sizeof(long));
    n = read(ipcin, buf, sizeof(long));
  }
  if (buf[0] == IPCC_CLEAR_LOCK) {
    RJava_has_control = 0;
  }
  if (buf[0] == IPCC_CONTROL_ADDR) {
    buf[1] = (long) (void*) &RJava_has_control;
    write(resout, buf, sizeof(long)*2);
  }
  if (buf[0] == IPCC_CALL_REQUEST) {
    callbackfn *fn;
    read(ipcin, buf+1, sizeof(long)*2);
    fn = (callbackfn*) buf[1];
    RJava_has_control = 1;
    fn((void*) buf[2]);
    RJava_has_control = 0;
  }
}

int RJava_init_loop() {
  int pfd[2];
  pipe(pfd);
  ipcin = pfd[0];
  ipcout = pfd[1];
  pipe(pfd);
  resin = pfd[0];
  resout = pfd[1];

  rJavaInfoBlock.ipcin = ipcin;
  rJavaInfoBlock.ipcout = ipcout;
  rJavaInfoBlock.resin = resin;
  rJavaInfoBlock.resout = resout;
  rJavaInfoBlock.has_control = &RJava_has_control;

  addInputHandler(R_InputHandlers, ipcin, RJava_ProcessEvents, RJavaActivity);
  return 0;
}

#endif
