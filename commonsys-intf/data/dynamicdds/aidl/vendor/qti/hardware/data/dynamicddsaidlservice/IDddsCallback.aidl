/*===========================================================================

  Copyright (c) 2023 Qualcomm Technologies, Inc.
  All Rights Reserved.
  Confidential and Proprietary - Qualcomm Technologies, Inc.

===========================================================================*/

package vendor.qti.hardware.data.dynamicddsaidlservice;

@VintfStability
interface IDddsCallback {
    /**
     * Called when dynamic DDS feature is enabled or disabled
     * @param available true if feature is enabled; false if feature is disabled
     */
    oneway void onFeatureAvailable(in boolean available);

    /**
     * Called when modem is recommending a new subscription. Recommendations
     * will only be provided when the feature is disabled. When the feature
     * is enabled, the recommendations will be automatically acted upon and
     * and subscription changes will be provided through onSubChanged()
     *
     * @param slotId    suggested dds subscription sim slot
     */
    oneway void onRecommendedSubChange(in int slotId);

    /**
     * Called when subscription dynamically changed
     * @param slotId    current dds subscription sim slot
     */
    oneway void onSubChanged(in int slotId);
}
