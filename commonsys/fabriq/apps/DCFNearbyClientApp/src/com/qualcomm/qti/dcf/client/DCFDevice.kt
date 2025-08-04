/**
 * Copyright (c) 2022 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */
package com.qualcomm.qti.dcf.client

import android.util.Log
import java.util.*

data class DCFDevice(
    val deviceId: String,
    val dataElements: List<DCFDataElement>,
    var discoveryTimestampMillis: Long
) {

    companion object {
        const val LIFE_TIME_THRESHOLD = 12 * 1000
    }

    fun isAlive(): Boolean {
        return (System.currentTimeMillis() - discoveryTimestampMillis) <= LIFE_TIME_THRESHOLD
    }

    override fun equals(other: Any?): Boolean {
        if (other !is DCFDevice) return false

        val desMap = dataElements.associateBy { it.tag }
        val otherDEsMap = other.dataElements.associateBy { it.tag }

        if (desMap.size != otherDEsMap.size) return false

        desMap.forEach {
            // Determine whether the data of each pair of DE are equal
            val otherDe = otherDEsMap[it.key]
            if (otherDe == null || !it.value.data.contentEquals(otherDe.data)) {
                return false
            }
        }

        return true
    }
}
