/*
 * Copyright (c) 2023 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.ims.configaidlservice;

/*
 * autoconfiguration related params
 *
 *
 * AutoConfig Definition Type
 */
@VintfStability
@Backing(type="int")
enum AutoConfigRequestType {
    SERVER_UPDATE,
    CLIENT_REQUEST,
}
