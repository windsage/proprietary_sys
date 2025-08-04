/*
 * Copyright (c) 2012-2013, 2018-2019, 2022-2023 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

#include <jni.h>

#ifndef SYSHELPER_H
#define SYSHELPER_H

#ifdef __cplusplus
extern "C" {
#endif
/*
 * Class:     com_qualcomm_qti_services_systemhelper
 * Method:    init
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_com_qualcomm_qti_services_systemhelper_SysHelperService_init
  (JNIEnv *, jclass);

/*
 * Class:     com_qualcomm_qti_services_systemhelper
 * Method:    initAIDL
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_com_qualcomm_qti_services_systemhelper_SysHelperService_initAIDL
  (JNIEnv *, jclass);

/*
 * Class:     com_qualcomm_qti_services_systemhelper
 * Method:    sendEventNotification
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL Java_com_qualcomm_qti_services_systemhelper_SysHelperService_sendEventNotification
  (JNIEnv *, jclass, jint);

/*
 * Class:     com_qualcomm_qti_services_systemhelper
 * Method:    sendEventNotificationAIDL
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL Java_com_qualcomm_qti_services_systemhelper_SysHelperService_sendEventNotificationAIDL
  (JNIEnv *, jclass, jint);

/*
 * Class:     com_qualcomm_qti_services_systemhelper
 * Method:    getMessage
 * Signature: ()I
 */
JNIEXPORT jstring JNICALL Java_com_qualcomm_qti_services_systemhelper_SysHelperService_getMessage
  (JNIEnv *, jclass);

/*
 * Class:     com_qualcomm_qti_services_systemhelper
 * Method:    terminate
 * Signature: ()
 */
JNIEXPORT jint JNICALL Java_com_qualcomm_qti_services_systemhelper_SysHelperService_terminate
  (JNIEnv *, jclass);

/*
 * Class:     com_qualcomm_qti_services_systemhelper
 * Method:    setPotraitOrientation
 * Signature: ()
 */
JNIEXPORT void JNICALL Java_com_qualcomm_qti_services_systemhelper_SysHelperService_setOrientationPortrait
  (JNIEnv *, jclass, jboolean);

#ifdef __cplusplus
}
#endif
#endif
