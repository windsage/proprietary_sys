/*
 * Copyright (c) 2023 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */
package vendor.qti.hardware.bttpi;

@VintfStability
interface IBtTpiStatusRspCb {
  /**
   * set the status of Tpi request
   * @param : reqType, type of request
   * @param : status and cmd response
   * @Note  : None
   * @return: void
   */
  void setStatusRsp(in int reqType, byte status, in byte[] rsp_status);
}
