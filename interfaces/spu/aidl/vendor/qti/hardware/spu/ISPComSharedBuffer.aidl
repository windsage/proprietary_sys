/**
*
* Copyright (c) 2023 Qualcomm Technologies, Inc.
* All Rights Reserved.
* Confidential and Proprietary - Qualcomm Technologies, Inc.
*
*/

package vendor.qti.hardware.spu;

@VintfStability
interface ISPComSharedBuffer {

    /**
    * Copy data from DMA buffer shared between SPU HAL server and SPU to
    * hidl_memory shared between SPU HAL client and SPU HAL server
    *
    * @param   in offset  Offset in shared memory to start copy from
    * @param   in size    Number of bytes to copy
    *
    * @return  error   Zero on success or non-zero error code on failure
    */
    int copyFromSpu(in int offset, in int size);

    /**
    * Copy data from hidl_memory shared between SPU HAL client and SPU HAL
    * server to DMA buffer shared between SPU HAL server and SPU
    *
    * @param   in offset  Offset in shared memory to start copy from
    * @param   in size    Number of bytes to copy
    *
    * @return  error  Zero on success or non-zero error code on failure
    */
    int copyToSpu(in int offset, in int size);
}
