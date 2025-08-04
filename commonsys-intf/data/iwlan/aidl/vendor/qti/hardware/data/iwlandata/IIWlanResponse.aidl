/******************************************************************************
  Copyright (c) 2023 Qualcomm Technologies, Inc.
  All Rights Reserved.
  Confidential and Proprietary - Qualcomm Technologies, Inc.
******************************************************************************/

package vendor.qti.hardware.data.iwlandata;

import vendor.qti.hardware.data.iwlandata.IWlanDataRegStateResult;
import vendor.qti.hardware.data.iwlandata.IWlanResponseInfo;
import vendor.qti.hardware.data.iwlandata.QualifiedNetworks;
import android.hardware.radio.RadioResponseInfo;
import android.hardware.radio.data.SetupDataCallResult;

/**
 * Interface declaring response functions for data service.
 */
@VintfStability
interface IIWlanResponse {

    /**
     * Acknowledge the receipt of radio request sent to the vendor. This must be sent only for
     * radio request which take long time to respond.
     * For more details, refer https://source.android.com/devices/tech/connect/ril.html
     *
     * @param serial Serial no. of the request whose acknowledgement is sent.
     */
    oneway void acknowledgeRequest(in int serial);

    /**
     * @param info Response info struct containing response type, serial no. and error
     *
     * Valid errors returned:
     *   RadioError:NONE
     *   RadioError:RADIO_NOT_AVAILABLE
     *   RadioError:INVALID_CALL_ID
     *   RadioError:INVALID_STATE
     *   RadioError:INVALID_ARGUMENTS
     *   RadioError:REQUEST_NOT_SUPPORTED
     *   RadioError:INTERNAL_ERR
     *   RadioError:NO_MEMORY
     *   RadioError:NO_RESOURCES
     *   RadioError:CANCELLED
     *   RadioError:SIM_ABSENT
     */
    oneway void deactivateDataCallResponse(in IWlanResponseInfo info);

    /**
     * @param info Response info struct containing response type, serial no. and error
     * @param qualified Networks List
     *
     * Valid errors returned:
     *   RadioError:NONE
     *   RadioError:INTERNAL_ERR
     *   RadioError:NO_MEMORY
     *   RadioError:NO_RESOURCES
     *   RadioError:REQUEST_NOT_SUPPORTED
     */
    oneway void getAllQualifiedNetworksResponse(in IWlanResponseInfo info,
        in QualifiedNetworks[] qualifiedNetworksList);

    /**
     * @param info Response info struct containing response type, serial no. and error
     * @param dcResponse List of SetupDataCallResult
     *
     * Valid errors returned:
     *   RadioError:NONE
     *   RadioError:RADIO_NOT_AVAILABLE
     *   RadioError:INTERNAL_ERR
     *   RadioError:SIM_ABSENT
     */
    oneway void getDataCallListResponse(in RadioResponseInfo info, in SetupDataCallResult[] dcResponse);

    /**
     * @param info Response info struct containing response type, serial no. and error
     * @param state Data registration state parameters
     */
    oneway void getDataRegistrationStateResponse(in IWlanResponseInfo info,
        in IWlanDataRegStateResult state);

    /**
     * @param info Response info struct containing response type, serial no. and error
     * @param dcResponse SetupDataCallResult
     *
     * Valid errors returned:
     *   RadioError:NONE must be returned on both success and failure of setup with the
     *              DataCallResponse.status containing the actual status
     *              For all other errors the DataCallResponse is ignored.
     *   RadioError:RADIO_NOT_AVAILABLE
     *   RadioError:OP_NOT_ALLOWED_BEFORE_REG_TO_NW
     *   RadioError:OP_NOT_ALLOWED_DURING_VOICE_CALL
     *   RadioError:INVALID_ARGUMENTS
     *   RadioError:INTERNAL_ERR
     *   RadioError:NO_RESOURCES if the vendor is unable handle due to resources are full.
     *   RadioError:SIM_ABSENT
     */
    oneway void setupDataCallResponse(in RadioResponseInfo info, in SetupDataCallResult dcResponse);
}
