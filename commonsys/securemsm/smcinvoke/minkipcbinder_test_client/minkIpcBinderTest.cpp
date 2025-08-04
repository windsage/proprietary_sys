/**
  * Copyright (c) 2024 Qualcomm Technologies, Inc.
  * All Rights Reserved.
  * Confidential and Proprietary - Qualcomm Technologies, Inc.
  */
#include <string>
#include <unistd.h>
#include <errno.h>
#include "object.h"
#include "client_logging.h"
#include "minkipcbinder.h"
#include "IOpener.h"
#include "CDiagnostics.h"
#include "IDiagnostics.h"

#define VENDOR_MINKIPCBINDER_SERVER "vendor.qti.hardware.minkipcbinder.IMinkServer/default"


int main(int argc, char **argv) {

  int32_t count;
  Object minkBinderOpener = Object_NULL;
  IDiagnostics_HeapInfo heapInfo;
  Object appObject = Object_NULL;
  MinkIpcBinder *clientMinkBinderConn;
  memset((void *)&heapInfo, 0, sizeof(IDiagnostics_HeapInfo));

  size_t iterations = atoi(argv[1]);
  int ret           = 0;
  size_t size       = 0;
  uint8_t * buffer  = NULL;

  LOGE("Begin minkipcbinder client test");

  clientMinkBinderConn =  MinkIpcBinder_connect(VENDOR_MINKIPCBINDER_SERVER, NULL, &minkBinderOpener);
  if (clientMinkBinderConn == NULL) {
      LOGE("Connect to IMinkServer failed");
      return -1;
  }
  LOGE("Connection to minkipcbinder Server is sucessfull");

  IOpener_open(minkBinderOpener, CDiagnostics_UID, &appObject);
  if (Object_isNull(appObject)) {
      LOGE("Failed to acquire CDiagnostics service opener");
      goto exit;
  }

  LOGE("Start CDiagnostics heap test");

  for(int i=0; i <iterations; i++) {
      LOGE("Retrieve TZ heap info Iteration = %d", i);
      IDiagnostics_queryHeapInfo(appObject, &heapInfo);
      LOGE("%d = Total bytes as heap\n", heapInfo.totalSize);
      LOGE("%d = Total bytes allocated from heap\n", heapInfo.usedSize);
  }

  LOGE("End CDiagnostics heap test");

exit:
  Object_ASSIGN_NULL(minkBinderOpener);
  Object_ASSIGN_NULL(appObject);
  MinkIpcBinder_release(clientMinkBinderConn);
  return 0;
}
