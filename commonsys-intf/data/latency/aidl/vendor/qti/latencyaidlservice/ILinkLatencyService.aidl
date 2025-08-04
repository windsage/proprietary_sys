/******************************************************************************  
Copyright (c) 2023 Qualcomm Technologies, Inc.  
All Rights Reserved.  
Confidential and Proprietary - Qualcomm Technologies, Inc.
******************************************************************************/

package vendor.qti.latencyaidlservice;

import vendor.qti.latencyaidlservice.Config;
import vendor.qti.latencyaidlservice.Level;
import vendor.qti.latencyaidlservice.StatusCode;
import vendor.qti.latencyaidlservice.Filter;
import vendor.qti.latencyaidlservice.FilterInfo;
import vendor.qti.latencyaidlservice.ILinkLatencyIndication;
import vendor.qti.latencyaidlservice.OodStatus;
import vendor.qti.latencyaidlservice.SetLevelArguments;
import vendor.qti.latencyaidlservice.AddFilterId;
import vendor.qti.latencyaidlservice.SlotId;

/**
 * This is the root of the HAL module and is the interface returned when
 * loading an implementation of the IFactory HAL.
 */
@VintfStability
interface ILinkLatencyService {
    /**
     * Called by the client to deinitialize the effect and free up
     * all the currently allocated resources. It is recommended to close
     * the effect on the client side as soon as it is becomes unused.
     *
     * @return OK in case of successful Filter addition.
     * filterId is present in Parcelable
     */
    StatusCode addFilter(in Filter filter, out AddFilterId addfilterid);

    /**
     * Called by the client to deinitialize the effect and free up
     * all the currently allocated resources. It is recommended to close
     * the effect on the client side as soon as it is becomes unused.
     *
     * @return OK in case the success. INVALID_STATE if the effect was already closed.
     *
     */
    StatusCode close();

    /**
     * Deletes all Filters added by the client.
     *
     * Each deleted Filter will have a seperate response from
     * ILinkLatencyIndication().filterStatus().
     *
     * @return StatusCode of the call, which may be:
     *   - `OK`               - Flows will be deleted. Status after deletion will be
     *                          sent via ILinkLatencyIndication().flowStatus().
     *   - `CALLBACK_NOT_SET` - ILinkLatencyIndication has not been set.
     *
     */
    StatusCode deleteAllFilters();

    /**
     * Deletes the specific Filter if added by the client.
     *
     * Response for deleted Filter is ILinkLatencyIndication().filterStatus().
     *
     * @param filterId ID for the Filter to be deleted.
     *
     * @return StatusCode of the call, which may be:
     *   - `OK`               - Filter will be deleted. Status after deletion will be
     *                          sent via ILinkLatencyIndication().flowStatus().
     *                          ILinkLatencyIndication().flowStatus().
     *   - `CALLBACK_NOT_SET` - ILinkLatencyIndication has not been set.
     *   - `INVALID_ARGUMENTS`- filterId is not valid.
     *
     */
    StatusCode deleteFilter(in int filterId);

    /**
     * Provides uninterrupted gaming experience. Enables absolute prioritization
     * of default data subscription(by suspending other subscription).
     *
     * @param: enable or disable this feature.
     */
    oneway void enableHighPerformanceGaming(in boolean isEnable);

    /**
     * Returns a vector of FilterInfo objects corresponding to the Filters added by the client.
     *
     * @param out vector of Filter ID's and Status' for every filter added by the client.
     */
    FilterInfo[] getFilters();

    /**
     * Enables prioritization of default data subcription. System will
     * prioritize default data subscription over other subscriptions,
     * as long as there is at least one vote for it.
     *
     * @param isEnabled true enables prioritization of default data
     *    subscription, and false disables it. System will continue
     *    prioritizing default data subscription as long as there is
     *    one client requesting for this to be enabled.
     * @return operation status
     */
    StatusCode prioritizeDefaultDataSubscription(in boolean isEnabled);

    /**
     * Sets the callback for the client. Necessary to add/remove filters and get
     * updates on filter status.
     *
     * @param callback The async class for sending indications and responses back to client.
     */
    oneway void setCallback(in ILinkLatencyIndication callback);

    /**
     * Configures the uplink and downlink link latency for
     * the specified radio link and slot Id. In case of multiple requests,
     * system will pick the lowest latency level amongst outstanding
     * requests. A client is guaranteed a latency level at
     * least as good as what it requested, but the effective level may
     * be better by virtue of other concurrent requests.
     * Enables extension of existing radio connection extension of the
     * specified radio link and slot Id. System will enable radio connection
     * extension as long as there is at least one vote for it.
     *
     * @param params data structure containing latency config parameters.
     * @return OK in case of successful setLevel
     * effectiveUplink and effectiveDownlink are clubbed together in Parcelable
     * @param out effectiveUplink level currently set on the system.
     * @param out effectiveDownlink level currently set on the system.
     */
    StatusCode setLevel(in Config params,
        out SetLevelArguments setlevelarguments);

    /**
     * Called by the client to update OOD status for a filter
     *
     * @return OK in case the success. INVALID_ARGUMENTS if the filterId does not match.
     *
     */
    StatusCode updateOODStatus(in int filterId, in OodStatus ood);

    /**
     * Enables/Disables out of order delivery.
     *
     * @return StatusCode of the call, which may be:
     *   - `OK`               - OOD was enabled/disabled.
     *
     */
    StatusCode updateOodForDDS(in OodStatus ood);

    /**
     * Called by the client to update pdcp discard timer for a filter
     *
     * @return OK in case the success. INVALID_ARGUMENTS if the filterId does not match.
     *
     */
    StatusCode updatePdcpDiscardTimer(in int filterId, in long timer);

    /**
     * Sets the PDCP discard timer value.
     *
     * @return StatusCode of the call, which may be:
     *   - `OK`               - PDCP discard timer value correctly set.
     *
     */
    StatusCode updatePdcpDiscardTimerForDDS(in long timerValue);

    /**
     * Called by the client to update uplink latency level for a filter.
     *
     * @return OK in case the success. INVALID_ARGUMENTS if the filterId does not match.
     *
     */
    StatusCode updatelinkLatencyLevel(in int filterId, in Level uplink_level,
        in Level downlink_level);

    /**
     * Enables selection of data subcription to be prioritized. System will
     * prioritize given data subscription as long as there is at least one vote for it.
     *
     * @param slotId SIM slot ID of subscription to prioritize
     *
     * @param isEnabled Value true enables prioritization of given data
     *    subscription, and false disables it. System will continue
     *    prioritizing data subscription as long as there is
     *    one client requesting for this to be enabled.
     * @return status operation status
     */
    StatusCode prioritizeDataPerSubscription(in SlotId slotId, in boolean isEnabled);
}
