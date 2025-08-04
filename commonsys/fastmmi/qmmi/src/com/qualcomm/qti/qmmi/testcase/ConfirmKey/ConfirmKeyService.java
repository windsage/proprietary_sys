/*
 * Copyright (c) 2020, 2023 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */
package com.qualcomm.qti.qmmi.testcase.ConfirmKey;

import com.qualcomm.qti.qmmi.bean.TestCase;
import com.qualcomm.qti.qmmi.framework.BaseService;
import com.qualcomm.qti.qmmi.utils.LogUtils;

public class ConfirmKeyService extends BaseService {
    @Override
    public void register() {

    }

    @Override
    public int run(TestCase testCase) {
        LogUtils.logi("ConfirmKeyservice run");
        return 0;
    }

    @Override
    public int stop(TestCase testCase) {
        return 0;
    }

}
