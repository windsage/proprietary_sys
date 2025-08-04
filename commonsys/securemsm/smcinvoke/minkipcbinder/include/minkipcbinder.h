/********************************************************************
 * Copyright (c) 2023 Qualcomm Technologies, Inc.
 * All Rights Reserved
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 * *********************************************************************/
#pragma once

#include "object.h"
#if defined (__cplusplus)
extern "C" {
#endif

typedef struct MinkIpcBinder MinkIpcBinder;

/** service : The AIBinder service name to connect to
    opener : An out pointer to a mink object which'll be
             filled on successful connection
**/
MinkIpcBinder* MinkIpcBinder_connect(const char *service, void *aiBinder, Object *opener);


/**
   wait for the service to finish ..
   waits until stopped or the service dies
**/
void MinkIpcBinder_join(MinkIpcBinder *me);

/**
   Increment reference count to keep the object live.
**/
void MinkIpcBinder_retain(MinkIpcBinder *me);

/**
   Decrement reference count.
   When the count goes to 0, *me* is deleted.
**/
void MinkIpcBinder_release(MinkIpcBinder *me);

#if defined (__cplusplus)
}
#endif
