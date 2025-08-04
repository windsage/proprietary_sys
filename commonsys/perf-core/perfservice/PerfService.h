/*
 * Copyright (c) 2018 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 *
 */

#ifndef ANDROID_PERFSERVICE_H
#define ANDROID_PERFSERVICE_H

#include "IPerfManager.h"
#include <map>
#include <set>
#include <binder/BinderService.h>
#include <utils/Mutex.h>

using namespace std;

namespace android {

class PerfService :
    public BinderService<PerfService>,
    public BnPerfManager
{
public:
    PerfService();
    virtual int perfLockAcquire(int duration, int len, int* boostsList);
    virtual int perfHint(int hint, String16& userDataStr, int userData1, int userData2, int tid);
    virtual int perfLockRelease();
    virtual int perfLockReleaseHandler(int _handle);
    virtual int perfUXEngine_events(int opcode, int pid, String16& pkg_name, int lat);
    virtual int setClientBinder(const sp<IBinder>& client);
    virtual int perfLockAcqAndRelease(int handle, int duration, int len, int reservelen, int* boostsList);
    virtual int perfGetFeedbackExtn(int req, String16& pkg_name, int tid, int len, int* list);
    virtual void perfEvent(int eventId, String16& pkg_name, int tid, int len, int* list);
    virtual int perfHintAcqRel(int handle, int hint, String16& pkg_name, int duration, int hint_type, int tid, int len, int* list);
    virtual int perfHintRenew(int handle, int hint, String16& pkg_name, int duration, int hint_type, int tid, int len, int* list);
    virtual double getPerfHalVer();
    virtual status_t dump(int fd, const Vector<String16>& args);

    class DeathNotifier : public IBinder::DeathRecipient {
    public:
        DeathNotifier(PerfService *perf) : mPerf(perf) {}
        virtual ~DeathNotifier();
        virtual void binderDied(const wp<IBinder>& who);
    private:
        PerfService *mPerf;
    };
    struct BinderInfo {
        int pid;
        sp<IBinder> binder;
        sp<DeathNotifier> notifier;

        bool operator < (const BinderInfo &bi) const {
            return pid<bi.pid;
        }
    };
    struct HandleInfo {
        int handle;
        int pid;
        int tid;
        int hintId;
        int hintType;

        bool operator < (const HandleInfo &hi) const {
            return handle<hi.handle;
        }
    };
private:
    Mutex mLock;
    set<BinderInfo> mBinderSet;
    set<HandleInfo> mHandleSet;
    int mHandle = 0;
    int (*perf_lock_acq)(int handle, int duration, int list[], int numArgs);
    int (*perf_lock_rel)(int handle);
    int (*perf_hint)(int, const char *, int, int);
    int (*perf_ux_engine_events)(int, int, const char *, int);
    const char* (*perf_sync_request)(int);
    int (*perf_lock_acq_rel)(int handle, int duration, int list[], int numArgs, int rnumArgs);
    int (*perf_get_feedback_extn)(int, const char *, int, int[]);
    void (*perf_event)(int, const char *, int, int[]);
    int (*perf_hint_acq_rel)(int, int, const char *, int, int, int, int[]);
    int (*perf_hint_renew)(int, int, const char *, int, int, int, int[]);
    double (*get_perf_hal_ver)();
};

} // namespace android

#endif // ANDROID_PERFSERVICE_H
