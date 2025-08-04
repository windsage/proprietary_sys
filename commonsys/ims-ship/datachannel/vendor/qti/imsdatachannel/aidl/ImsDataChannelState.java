/**********************************************************************
 * Copyright (c) 2022-2023 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 **********************************************************************/

package vendor.qti.imsdatachannel.aidl;

import android.os.Parcel;
import android.os.Parcelable;
import vendor.qti.imsdatachannel.aidl.DataChannelState;

public class ImsDataChannelState implements Parcelable {
    private String mDcId;
    private DataChannelState mState;
    private String mDcHandle;

    public ImsDataChannelState(){}

    private ImsDataChannelState(Parcel in) {
        readFromParcel(in);
    }

    public void readFromParcel(Parcel in) {
        mDcId = in.readString();
        mState = in.readParcelable(DataChannelState.class.getClassLoader());
        mDcHandle = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mDcId);
        dest.writeParcelable(mState, flags);
        dest.writeString(mDcHandle);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<ImsDataChannelState> CREATOR = new Creator<ImsDataChannelState>() {
        @Override
        public ImsDataChannelState createFromParcel(Parcel in) {
            return new ImsDataChannelState(in);
        }

        @Override
        public ImsDataChannelState[] newArray(int size) {
            return new ImsDataChannelState[size];
        }
    };

    public String getDcHandle() {
        return mDcHandle;
    }

    public void setDcHandle(String mDcHandle) {
        this.mDcHandle = mDcHandle;
    }


    public String getDcId() {
        return mDcId;
    }

    public void setDcId(String mDcId) {
        this.mDcId = mDcId;
    }

    public DataChannelState getState() {
        return mState;
    }

    public void setState(DataChannelState mState) {
        this.mState = mState;
    }
}
