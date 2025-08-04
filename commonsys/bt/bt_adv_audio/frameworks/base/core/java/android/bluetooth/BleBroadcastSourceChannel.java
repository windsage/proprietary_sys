/*
 * Copyright (c) 2021 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 *
*/
 /*
 * Copyright (c) 2020, The Linux Foundation. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *    * Redistributions of source code must retain the above copyright
 *      notice, this list of conditions and the following disclaimer.
 *    * Redistributions in binary form must reproduce the above
 *      copyright notice, this list of conditions and the following
 *      disclaimer in the documentation and/or other materials provided
 *      with the distribution.
 *    * Neither the name of The Linux Foundation nor the names of its
 *      contributors may be used to endorse or promote products derived
 *      from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED "AS IS" AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT
 * ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS
 * BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
 * BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
 * IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package android.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.IBluetoothGatt;
import android.bluetooth.IBluetoothManager;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Retention;
import android.annotation.IntDef;
import android.compat.annotation.UnsupportedAppUsage;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.Handler;
import android.os.Looper;
import android.os.RemoteException;
import android.util.Log;
import java.util.Objects;
import android.util.Log;
import java.util.List;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * This class provides Interface to select the Broadcast source channels
 * to be synchronized from the Broadcast source. these Broadcast source channels
 * are mapped to the BIS indicies that given Broadcast source is broadcasting with.
 *
 * <p>This also acts as general data structure for updating the Broadcast
 * source channel information
 *
 * This class is used to input the User provided data for below operations
 * {@link BleBroadcastAudioScanAssistManager#addBroadcastSource},
 * {@link BleBroadcastAudioScanAssistManager#updateBroadcastSource} and
 *
 * mIndex : index is the Identifier for Broadcast channel
 * mDescription: Description describing the type of Broadcast data being broadcasted
 * mStatus: TRUE means broadcast source channel need to be synchronized
 *            FALSE means broadcast source channel need NOT be synchronized
 *
 * @hide
 */
public final class BleBroadcastSourceChannel implements Parcelable {

    private static final String TAG = "BleBroadcastSourceChannel";
    private int mIndex;
    private String mDescription;
    private boolean mStatus;
    private int mSubGroupId;
    private byte[] mMetadata;

    public BleBroadcastSourceChannel (int index,
                            String description,
                            boolean st,
                            int aSubGroupId,
                            byte[] aMetadata) {
            mIndex = index;
            mDescription = description;
            mStatus = st;
            mSubGroupId = aSubGroupId;
            if (aMetadata != null && aMetadata.length != 0) {
                mMetadata = new byte[aMetadata.length];
                System.arraycopy(aMetadata, 0, mMetadata, 0, aMetadata.length);
            }
    }
    @Override
    public boolean equals(Object o) {
        if (o instanceof BleBroadcastSourceChannel) {
            BleBroadcastSourceChannel other = (BleBroadcastSourceChannel) o;
            return (other.mIndex == mIndex
                    && other.mDescription == mDescription
                    && other.mStatus == mStatus
                    );
        }
        return false;
    }
    @Override
    public int hashCode() {
        return Objects.hash(mIndex, mDescription, mStatus);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public String toString() {
        return mDescription;
    }

    /**
     * Gets the Source Id of the BleBroadcastSourceChannel Object
     *
     * @return byte representing the Source Id of the Broadcast Source Info Object
     *          {@link #BROADCAST_ASSIST_INVALID_SOURCE_ID} in case if this field is not valid
     * @hide
     */
    public int getIndex () {
        return mIndex;
    }

    /**
     * Gets the Broadcast source Device object from the BleBroadcastSourceChannel Object
     *
     * @return BluetoothDevice object for Broadcast source device
     * @hide
     */
    public String getDescription () {
        return mDescription;
    }

     /**
     * Gets the status of given BleBroadcastSourceChannel
     *
     * @return true if selected, false otherwise
     * @hide
     *
     * @deprecated
     */

    public boolean getStatus () {
        return mStatus;
    }

     /**
     * Gets the address type of the Broadcast source advertisement for the BleBroadcastSourceChannel Object
     *
     * @return byte addressType, this can be one of {@link #BROADCAST_ASSIST_ADDRESS_TYPE_PUBLIC} OR {@link #BROADCAST_ASSIST_ADDRESS_TYPE_PUBLIC}
     * @hide
     *
     * @deprecated
     */

    public byte[] getMetadata () {
        return mMetadata;
    }

     /**
     * Gets the subgroup Id that broadcast Channel belongs
     * Internal helper function
     *
     * @hide
     * @deprecated
     */
    public int getSubGroupId () {
         return mSubGroupId;
    }

     /**
     * Sets the status of given BleBroadcastSourceChannel
     *
     * @return true if selected, false otherwise
     * @hide
     *
     * @deprecated
     */
    public void setStatus (boolean status) {
        mStatus = status;
    }


    public static final @android.annotation.NonNull Parcelable.Creator<BleBroadcastSourceChannel> CREATOR =
            new Parcelable.Creator<BleBroadcastSourceChannel>() {
                public BleBroadcastSourceChannel createFromParcel(Parcel in) {

                    log(TAG, "createFromParcel>");
                    final int index = in.readInt();
                    final String desc = in.readString();
                    final boolean status = in.readBoolean();
                    final int subGroupId = in.readInt();

                    final int metadataLength = in.readInt();
                    byte[] metadata = null;
                    if (metadataLength > 0) {
                        metadata = new byte[metadataLength];
                        in.readByteArray(metadata);
                    }

                    BleBroadcastSourceChannel srcChannel =
                        new BleBroadcastSourceChannel(index, desc, status, subGroupId, metadata);
                    log(TAG, "createFromParcel:" + srcChannel);
                    return srcChannel;
                }

                public BleBroadcastSourceChannel[] newArray(int size) {
                    return new BleBroadcastSourceChannel[size];
                }
            };



    @Override
    public void writeToParcel(Parcel out, int flags) {
        log(TAG, "writeToParcel>");
        out.writeInt(mIndex);
        out.writeString(mDescription);
        out.writeBoolean(mStatus);
        out.writeInt(mSubGroupId);
        if (mMetadata != null) {
            out.writeInt(mMetadata.length);
            out.writeByteArray(mMetadata);
        } else {
            out.writeInt(0);
        }
        log(TAG, "writeToParcel:" + toString());
    }
    private static void log(String TAG, String msg) {
        BleBroadcastSourceInfo.BASS_Debug(TAG, msg);
    }
};

