/*===========================================================================
  Copyright (c) 2023 Qualcomm Technologies, Inc.
  All Rights Reserved.
  Confidential and Proprietary - Qualcomm Technologies, Inc.
===========================================================================*/

package vendor.qti.hardware.data.flowaidlservice;

import vendor.qti.hardware.data.flowaidlservice.Flow;
import vendor.qti.hardware.data.flowaidlservice.FlowInfo;
import vendor.qti.hardware.data.flowaidlservice.IUplinkPriorityIndication;
import vendor.qti.hardware.data.flowaidlservice.StatusCode;
import vendor.qti.hardware.data.flowaidlservice.Flow;
import vendor.qti.hardware.data.flowaidlservice.FlowId;

/**
 * Interface declaring uplink flow priority service.
 */
@VintfStability
interface IUplinkPriorityService {
    /**
     * Adds an priority flow, updated with Direction specifier for Flow.
     * The existing addFlow() will only add the flow for the UPLINK
     * direction.
     *
     * @param flow The Flow to be added.
     *
     * @param out flowId Unique identifier of flow being added.
     * @param out status StatusCode of the call, which may be:
     *   - `OK`               - Flow will attempt to be added. Status after addition will be
     *                          sent via IUplinkPriorityIndication().flowStatus().
     *   - `CALLBACK_NOT_SET` - IUplinkPriorityIndication has not been set.
     *
     */
    StatusCode addFlow(in Flow flow, out FlowId flowId);

    /**
     * Called by the client to deinitialize and free up
     * all the currently allocated resources. It is recommended to close
     * the effect on the client side as soon as it is becomes unused.
     *
     * @return OK in case the success. INVALID_STATE if the effect was already closed.
     *
     */
    StatusCode close();

    /**
     * Deletes all Flows added by the client.
     *
     * Each deleted Flow will have a seperate response from
     * IUplinkPriorityIndication().flowStatus().
     *
     * @return StatusCode of the call, which may be:
     *   - `OK`               - Flows will be deleted. Status after deletion will be
     *                          sent via IUplinkPriorityIndication().flowStatus().
     *   - `CALLBACK_NOT_SET` - IUplinkPriorityIndication has not been set.
     *
     */
    StatusCode deleteAllFlows();

    /**
     * Deletes the specific Flow if added by the client.
     *
     * Response for deleted Flow is IUplinkPriorityIndication().flowStatus().
     *
     * @param flowId ID for the Flow to be deleted.
     *
     * @return StatusCode of the call, which may be:
     *   - `OK`               - Flow will be deleted. Status after deletion will be
     *                          sent via IUplinkPriorityIndication().flowStatus().
     *                          IUplinkPriorityIndication().flowStatus().
     *   - `CALLBACK_NOT_SET` - IUplinkPriorityIndication has not been set.
     *   - `INVALID_ARGUMENTS`- flowId is not valid.
     *
     */
    StatusCode deleteFlow(in int flowId);

    /**
     * Returns a vector of FlowInfo objects corresponding to the Flows added by the client.
     *
     * @param flows Callback returning the vector of Flows which have been added. Includes
     *      the flowId and FlowStatus accociated with each Flow.
     *
     * @return vector of Flow ID's and related Status for every flow added by the client.
     */
    FlowInfo[] getFlows();

    /**
     * Sets the callback for the client.
     *
     * @param callback The async class for sending indications and responses back to client.
     */
    oneway void setCallback(in IUplinkPriorityIndication callback);
}
