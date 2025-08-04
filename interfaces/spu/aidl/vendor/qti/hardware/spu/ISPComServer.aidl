/**
*
* Copyright (c) 2023 Qualcomm Technologies, Inc.
* All Rights Reserved.
* Confidential and Proprietary - Qualcomm Technologies, Inc.
*
*/

package vendor.qti.hardware.spu;

import vendor.qti.hardware.spu.ISPComSharedBuffer;
import android.hardware.common.Ashmem;

@VintfStability
interface ISPComServer {

    parcelable data_st {
        byte[] data;
        int size;
    }

    /**
    * Get an instance of SPU shared buffer object with direct access to
    * shared buffer
    *
    * @param   in Ashmem   sharedMemory allocated and mapped by the caller
    *
    * @return  buffer      Pointer to a buffer on success and null pointer on
    *                      failure
    */
    ISPComSharedBuffer getSPComSharedBuffer(in Ashmem sharedMemory);

    /**
    * SPCom server register
    *
    * @return  error  Zero on success or non-zero error code on failure
    */
    int registerServer();

    /**
    * Send response to SPU client with shared buffer info
    *
    * @param   in response  Response buffer
    * @param   in buffer    Shared buffer to share with SPCom server
    * @param   in offset    Offset in response buffer to store shared buffer
    *                       info
    *                       Note: Minimum buffer size is 8 bytes
    *                             Max offset cannot exceed shared buffer size
    *                             minus 8 bytes
    *
    * @return  error     Zero on success or non-zero error code on failure
    */
    int sendModifiedResponse(in byte[] response, in ISPComSharedBuffer buffer, in int offset);

    /**
    * Send response to SPU client
    *
    * Must be called after getting a request
    *
    * @param   in response  Response buffer
    *
    * @return  error     Zero on success or non-zero error code on failure
    */
    int sendResponse(in byte[] response);

    /**
    * SPCom server unregister
    *
    * @param out  error  Zero on success or non-zero error code on failure
    */
    int unregisterServer();

    /**
    * Wait for a request and get request buffer from SPU client
    *
    * @param  out  request  Request buffer and status
    *
    * @return int  Number of request bytes on success or non-zero error
    *                   code on failure
    */
    int waitForRequest(out data_st request);
}
