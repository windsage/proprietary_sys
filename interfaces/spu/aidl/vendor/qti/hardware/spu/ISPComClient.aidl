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
interface ISPComClient {

    parcelable data_st {
        byte[] data;
        int size;
    }

    /**
    * Get an instance of SPU shared buffer object with direct access to shared
    * buffer
    *
    * @param in Ashmem sharedMemory  allocated and mapped by the caller
    *
    * @return  buffer  Pointer to a buffer on success and null pointer on failure
    */
    ISPComSharedBuffer getSPComSharedBuffer(in Ashmem sharedMemory);

    /**
    * Check remote edge connectivity
    *
    * @return status  True if connected false otherwise
    */
    boolean isConnected();

    /**
    * SPCom client register
    *
    * @return  error  Zero on success or non-zero error code on failure
    */
    int registerClient();

    /**
    * Send request to SPU server and wait for response
    *
    * @param  in   request    Request buffer
    * @param  in   timeoutMs  Timeout in milliseconds, zero means no timeout
    * @param  out  response   Response buffer
    *
    * @return int Number of response bytes on success or non-zero
    *                     error code on failure
    */
    int sendMessage(in byte[] request, in int timeoutMs, out data_st response);

    /**
    * Send request to SPU server with shared buffer info and wait for response
    *
    * @param  in  request    Request buffer
    * @param  in  buffer     Shared buffer to share with SPCom client
    * @param  in  offset     Offset in response buffer to store shared buffer
    * @param  in  timeoutMs  Timeout in milliseconds, zero means no timeout
    * @param  out response   Response buffer
    *
    * @return int Number of response bytes on success or non-zero
    *                    error code on failure
    */
    int sendModifiedMessage(in byte[] request, in ISPComSharedBuffer buffer, in int offset,
        in int timeoutMs, out data_st response);

    /**
    * SPCom client unregister
    *
    * @return error  Zero on success or non-zero error code on failure
    */
    int unregisterClient();
}
