/**
*
* Copyright (c) 2023 Qualcomm Technologies, Inc.
* All Rights Reserved.
* Confidential and Proprietary - Qualcomm Technologies, Inc.
*
*/

package vendor.qti.hardware.spu;

import vendor.qti.hardware.spu.ISPComClient;
import vendor.qti.hardware.spu.ISPComServer;
import vendor.qti.hardware.spu.ISPUNotifier;
import android.hardware.common.Ashmem;


@VintfStability
interface ISPUManager {

    parcelable data_st {
        byte[] data;
        int size;
    }

    /**
    * Read SPU health info such as registers and sensors state
    *
    * @param   out  data   Response from SPU
    *
    * @return  error  Zero on success or non-zero error code on failure
    */
    int checkHealth(out data_st data);

    /**
    * Clear preset SPU event notifier of HAL client
    *
    * @return  error   Zero on success or non-zero error code on failure
    */
    int clearEventNotifier();

    /**
    * Get SPU image type: Signature type and HW version
    *
    * @return  Zero if cannot get image type or non-zero image type
    */
    int getImageType();

    /**
    * Get handle to SPCom client interface
    *
    * @param   in name  Channel name as defined in application manifest
    *
    * @return  client   SPCom client handle or null pointer on failure
    */
    ISPComClient getSPComClient(in String name);

    /**
    * Get SPCom max channel name length including null terminator
    *
    * @return  length  SPCom max channel name length
    */
    int getSPComMaxChannelNameLength();

    /**
    * Get SPCom max message size
    *
    * @return  SPCom max message size
    */
    int getSPComMaxMessageSize();

    /**
    * Get handle to SPCom server interface
    *
    * @param   in name  Channel name as defined in application manifest
    *
    * @return  server   SPCom server handle or null pointer on failure
    */
    ISPComServer getSPComServer(in String name);

    /**
    * Check if SPU application is loaded
    *
    * @param   in name  Channel name as defined in application manifest
    *
    * @return  status   True if loaded a false otherwise
    */
    boolean isAppLoaded(in String name);

    /**
    * Load SPU application
    *
    * @param   in name   Channel name as defined in application manifest
    * @param   in data   Shared memory storing application image data
    * @param   in size   Application image byte size
    *
    * @return  error  Zero on success or non-zero error code on failure
    */
    int loadApp(in String name, in Ashmem data, in int size);

    /**
    * Send command to SPU to trigger SPU subsystem reset
    *
    * @return  error  Zero on success or non-zero error code on failure
    */
    int resetSpu();

    /**
    * Set a callable SPU event notifier for HAL client
    *
    * @param   in notifier  Callable notifier object
    *
    * @return  error     Zero on success or non-zero error code on failure
    */
    int setEventNotifier(in ISPUNotifier notifier);

    /**
    * Read SPU system data
    *
    * @param   in   id     Predefined system data id
    * @param   in   arg1   First argument
    * @param   in   arg2   Second argument
    * @param   out  data   Response from SPU
    *
    * @return  error  Zero on success or non-zero error code on failure
    */
    int sysDataRead(in int id, in int arg1, in int arg2, out data_st data);

    /**
    * Read SPU system parameter
    *
    * @param   in  id     Predefined system parameter id
    * @param   in  arg1   First argument
    * @param   in  arg2   Second argument
    * @param   out value  Response from SPU
    *
    * @return  error  Zero on success or non-zero error code on failure
    */
    int sysParamRead(in int id, in int arg1, in int arg2, out data_st value);

    /**
    * Write SPU System parameter
    *
    * @param   in id     Predefined system parameter id
    * @param   in arg1   First argument
    * @param   in arg2   Second argument
    *
    * @return  error  Zero on success or non-zero error code on failure
    */
    int sysParamWrite(in int id, in int arg1, in int arg2);

    /**
    * Wait until HLOS-SPU link up is notified
    *
    * @param   in timeoutMs  Timeout in milliseconds, zero timeout return link
    *                        without waiting
    *
    * @return  status     True for link up or false for link down
    */
    boolean waitForLinkUp(in int timeoutMs);

    /**
    * Wait for SPU to be ready and finish boot applications loading
    *
    * Must be called after system boot and after each SPU subsystem restart to
    * verify the system readiness before using ISPUManager interface
    *
    * @param   in timeoutSec  Timeout in seconds
    *
    * @return  error       Zero on success or non-zero error code on failure
    */
    int waitForSpuReady(in int timeoutSec);

    /**
    * Check if SPU application is loaded
    *
    * @param   in uuid  Application UUID defined in the App manifest
    *
    * @return  status   True if loaded a false otherwise
    */
    boolean isAppLoadedByUUID(in int uuid);
}
