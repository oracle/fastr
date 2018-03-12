#include "rjava.h"
#include <unistd.h>

#ifdef _WIN64
typedef long long ptrlong;
#else
typedef long ptrlong;
#endif

int ipcout;
int resin;
int *rjctrl = 0;

typedef void(callbackfn)(void *);

int RJava_request_lock() {
  ptrlong buf[4];
  int n;
  if (rjctrl && *rjctrl) return 2;

  buf[0] = IPCC_LOCK_REQUEST;
  write(ipcout, buf, sizeof(ptrlong));
  n = read(resin, buf, sizeof(ptrlong));
  return (n > 0 && buf[0] == IPCC_LOCK_GRANTED) ? 1 : 0;
}

int RJava_clear_lock() {
  ptrlong buf[4];
  buf[0] = IPCC_CLEAR_LOCK;
  write(ipcout, buf, sizeof(ptrlong));
  return 1;
}

void RJava_request_callback(callbackfn *fn, void *data) {
  ptrlong buf[4];
  buf[0] = IPCC_CALL_REQUEST;
  buf[1] = (ptrlong) fn;
  buf[2] = (ptrlong) data;
  write(ipcout, buf, sizeof(ptrlong) * 3);
}

void RJava_setup(int _in, int _out) {
  /* ptrlong buf[4]; */
  ipcout = _out;
  resin = _in;
}

void RJava_init_ctrl() {
  ptrlong buf[4];
  buf[0] = IPCC_CONTROL_ADDR;
  write(ipcout, buf, sizeof(ptrlong));
  read(resin, buf, sizeof(ptrlong) * 2);
  if (buf[0] == IPCC_CONTROL_ADDR) {
    rjctrl= (int*) buf[1];
  }
}
