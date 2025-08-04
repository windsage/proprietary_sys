/*****************************************************************
 * Copyright (c) 2022 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 ******************************************************************/

package vendor.qti.bluetooth.xpan;

public class TwtWakeParams {

    private int usecase;
    private int si;
    private int sp;
    private int rightoffset;

    int getUsecase() {
        return usecase;
    }
    void setUsecase(int usecase) {
        this.usecase = usecase;
    }
    int getSi() {
        return si;
    }
    void setSi(int si) {
        this.si = si;
    }
    int getSp() {
        return sp;
    }
    void setSp(int sp) {
        this.sp = sp;
    }
    int getRightoffset() {
        return rightoffset;
    }
    void setRightoffset(int rightoffset) {
        this.rightoffset = rightoffset;
    }
}