/*****************************************************************************
  Copyright (c) 2020 Qualcomm Technologies, Inc.
  All rights reserved.
  Confidential and Proprietary - Qualcomm Technologies, Inc.
*****************************************************************************/
#ifndef __IOBJFORWARDER_H
#define __IOBJFORWARDER_H

#include <stdint.h>
#include "object.h"

// Generic object forwarder upon a set object

#define IObjForwarder_OP_set ((ObjectOp_METHOD_MASK & ~ObjectOp_LOCAL) - 0)


static inline int32_t
IObjForwarder_release(Object self)
{
  return Object_invoke(self, Object_OP_release, 0, 0);
}

static inline int32_t
IObjForwarder_retain(Object self)
{
  return Object_invoke(self, Object_OP_retain, 0, 0);
}

static inline int32_t
IObjForwarder_set(Object self, Object callee_val)
{
  ObjectArg a[1];
  a[0].o = callee_val;

  return Object_invoke(self, IObjForwarder_OP_set, a, ObjectCounts_pack(0, 0, 1, 0));
}


#endif /* __IOBJFORWARDER_H */
