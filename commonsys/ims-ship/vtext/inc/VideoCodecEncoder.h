/*****************************************************************************

 ============================
Copyright (c)  2016-2017  Qualcomm Technologies, Inc.
All Rights Reserved.
Confidential and Proprietary - Qualcomm Technologies, Inc.
=============================

 File: VideoCodecEncoder.h
 Description: Implementation of VideoCodecEncoder

 Revision History
 ===============================================================================
 Date    |   Author's Name    |  BugID  |        Change Description
 ===============================================================================
 1-Dec-16   Sanjeev Mittal      First Version
 *****************************************************************************/
#include<MediaCodecBase.h>
#include <utils/StrongPointer.h>

#include<VTRecorder.h>
using namespace android;
#include <pthread.h>
#include <VTAImageReader.h>

class VideoCodecEncoder : public MediaCodecBase, public VTRecorder
{
  public:
    VideoCodecEncoder();

    virtual ~VideoCodecEncoder();
    /** Returns true if the thread started */
    bool StartRecordThread()
    {
      CRITICAL1("Start RecordLoop Thread ");
      return (pthread_create(&recorderthread, NULL, RecorderThreadFunction,
                             this) == 0);
    }

    /** Will not return until the internal thread has exited. */
    void WaitforRecordThread()
    {
      CRITICAL1("Waiting for RecordLoop Thread to exit");
      (void) pthread_join(recorderthread, NULL);
      CRITICAL1("RecordLoop Thread exited");
    }
    virtual QPE_VIDEO_ERROR Init(QP_VIDEO_CALLBACK tVideoCallBack,
                                 void *pUserData, QPE_VIDEO_DEV eVDev,
                                 QP_VIDEO_CONFIG *pCodecConfig);
    virtual QPE_VIDEO_ERROR Configure(QP_VIDEO_CONFIG CodecConfig);
    virtual QPE_VIDEO_ERROR Start();
    virtual QPE_VIDEO_ERROR Stop();
    //Recorder specific functions
    virtual ANativeWindow* getRecordingSurface(JNIEnv * env);
    virtual QPE_VIDEO_ERROR AdaptVideoBitRate(uint32_t iBitRate);
    virtual QPE_VIDEO_ERROR DeInit();
    QPE_VIDEO_ERROR ProcessRecordingFrame(uint64_t timestamp, void *dataPtr,
                                          size_t size);
    virtual QPE_VIDEO_ERROR GenerateH264IDRFrame();
    QPE_VIDEO_ERROR ConfigureProfileAndLevel();
    QPE_VIDEO_ERROR SetRotationAngle(uint16_t rotation);
    virtual QPE_VIDEO_ERROR AdaptVideoFrameRate(uint32_t iFrameRate);
    virtual QPE_VIDEO_ERROR AdaptVideoIdrPeriodicity(int32_t iFrameInt);
    void videoRecordLoop();

    /* This API will be called to mark LTR frame.*/
    void ConfigLTRMark(int32_t slLtrIdx);
    /* This API will be called to use LTR frame.*/
    void ConfigLTRUse(int32_t slLtrIdx);
    void copyNV12BufferToVenusBuffer(uint8_t *venusBuffer, uint8_t *nv12Buffer, size_t ulFrameBufSize);

  private:
    static void * RecorderThreadFunction(void * This)
    {
      ((VideoCodecEncoder *)This)->videoRecordLoop();
      return NULL;
    }
    QPE_VIDEO_ERROR GetEncodedFrame();

    void AImageReaderCameraLoop();
    static void * AImageReaderThreadFunction(void * This)
    {
      ((VideoCodecEncoder *)This)->AImageReaderCameraLoop();
      return NULL;
    }
    bool StartAImageReaderThread()
    {
      CRITICAL1("H263:Start AImageReader Thread ");
      return (pthread_create(&m_pAImageReaderthread, NULL, AImageReaderThreadFunction,
                             this) == 0);
    }

    /** Will not return until the internal thread has exited. */
    void WaitforAImageReaderThread()
    {
      CRITICAL1("Waiting for AImageReader Thread to exit");
      (void) pthread_join(m_pAImageReaderthread, NULL);
      CRITICAL1("AImageReader Thread exited");
    }


    pthread_t recorderthread;
    pthread_t m_pAImageReaderthread;
    enum eVideoState iRecorderState;
    QP_VIDEO_CONFIG m_codecConfig;
    /*used only for static image to align to the recorder input buffer*/
    int32_t m_Stride;
    int32_t m_Scanlines;
    int32_t m_ColorFormat;
    uint32 m_LastIDRtimestamp;
    bool m_IDRRetransmitOn;
    uint8_t m_IDRframeinterval;
    uint8_t videoRecordThreadCreated;
    uint8_t m_iVideoAImageReaderThreadCreated;
    VideoAImageReader *m_pVideoAImageReader;

};
