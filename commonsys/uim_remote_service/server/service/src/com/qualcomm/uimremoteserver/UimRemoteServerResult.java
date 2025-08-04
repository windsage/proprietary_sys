/*
 * Copyright (c) 2023 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.qualcomm.uimremoteserver;

public class UimRemoteServerResult {

    int mSapConnectRsp;

    int mMaxMsgSize;

    int mResultCode;

    int mCardReaderStatus;

    byte[] mApduRsp;

    byte[] mAtr;

    int mDisconnectType;

    int mStatus;

    public UimRemoteServerResult() {
    }

    public void setSapConnectRsp(int mSapConnectRsp) {
        this.mSapConnectRsp = mSapConnectRsp;
    }

    public void setMaxMsgSize(int mMaxMsgSize) {
        this.mMaxMsgSize = mMaxMsgSize;
    }

    public void setResultCode(int mResultCode) {
        this.mResultCode = mResultCode;
    }

    public void setCardReaderStatus(int mCardReaderStatus) {
        this.mCardReaderStatus = mCardReaderStatus;
    }

    public void setDisconnectType(int mDisconnectType) {
        this.mDisconnectType = mDisconnectType;
    }

    public void setStatus(int mStatus) {
        this.mStatus = mStatus;
    }

    public void setApduRsp(byte[] mApduRsp) {
        this.mApduRsp = mApduRsp;
    }

    public void setAtr(byte[] mAtr) {
        this.mAtr = mAtr;
    }

    public int getSapConnectRsp() {
        return mSapConnectRsp;
    }

    public int getMaxMsgSize() {
        return mMaxMsgSize;
    }

    public int getResultCode() {
        return mResultCode;
    }

    public int getCardReaderStatus() {
        return mCardReaderStatus;
    }

    public int getDisconnectType() {
        return mDisconnectType;
    }

    public int getStatus() {
        return mStatus;
    }

    public byte[] getApduRsp() {
        return mApduRsp;
    }

    public byte[] getAtr() {
        return mAtr;
    }
}
