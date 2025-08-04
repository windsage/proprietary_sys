/**
 * Copyright (c) 2024 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.wifi.wifilearner;

/**
 * struct ChannelSurveyInfo - Channel survey info
 *
 * @chanMask: bitmask indicating which fields have been reported, see
 *        enum ChannelSurveyInfoMask.
 * @freq: Center of frequency of the surveyed channel.
 * @noise: Channel noise floor in dBm.
 * @channelTime: Amount of time in ms the radio spent on the channel.
 * @channelTimeBusy: Amount of time in ms the radio detected some signal.
 *     that indicated to the radio the channel was not clear.
 * @channelTimeExtBusy: amount of time the extension channel was sensed busy.
 * @channelTimeRx: Amount of time the radio spent receiving data.
 * @channelTimeTx: Amount of time the radio spent transmitting data.
 * @channelTimeScan: Amount of time the raido spent for scan.
 */
@VintfStability
parcelable ChannelSurveyInfo {
    int chanMask;
    int freq;
    byte noise;
    long channelTime;
    long channelTimeBusy;
    long channelTimeExtBusy;
    long channelTimeRx;
    long channelTimeTx;
    long channelTimeScan;
}
